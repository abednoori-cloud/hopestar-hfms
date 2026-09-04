<#
    Builds the HFMS Windows installer end to end:
      1. mvn clean package -Pinstaller  (fat jar + baked HTTPS keystore)
      2. Convert the HopeStar logo to a multi-res .ico
      3. jpackage: jlinked custom JRE 21 + fat jar -> a single .exe installer

    Output: target/installer/HopeStar HFMS-<version>.exe

    Requirements on THIS build machine only (never on the end user's
    machine): JDK 21 with jpackage, and classic WiX Toolset v3
    (candle.exe/light.exe) -- JDK 21's jpackage does NOT support the
    newer unified WiX v4/v5/v7 "wix.exe" CLI (confirmed by testing: it
    errors "Can not find WiX tools (light.exe, candle.exe)" even with a
    v7 wix.exe on PATH). Install via
    `winget install --id WiXToolset.WiXToolset` (needs admin rights for
    the NetFx3 Windows feature). The end user needs nothing pre-installed
    on their machine either way; the installer bundles its own JRE.
#>
[CmdletBinding()]
param(
    [ValidateSet('exe', 'msi', 'app-image')]
    [string]$Type = 'exe',
    [switch]$SkipMavenBuild
)

$ErrorActionPreference = 'Stop'
$root = Resolve-Path "$PSScriptRoot\.."
Set-Location $root

function Write-Step($msg) { Write-Host "[build-installer] $msg" -ForegroundColor Cyan }

# WiX v3's installer registers itself in the registry/machine PATH, which
# a process started before that change (or a fresh shell that hasn't
# re-read it yet) won't see -- fall back to the well-known install
# location so this script doesn't depend on shell restart timing.
if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue)) {
    $wixBin = Get-ChildItem 'C:\Program Files (x86)\WiX Toolset v3*\bin' -ErrorAction SilentlyContinue |
        Select-Object -First 1 -ExpandProperty FullName
    if ($wixBin) {
        Write-Step "candle.exe not on PATH; adding $wixBin for this run."
        $env:PATH = "$wixBin;$env:PATH"
    }
}

# ---------------------------------------------------------------------
# 1. Maven build (produces target/hfms.jar with the HTTPS keystore baked
#    in via the -Pinstaller profile -- see pom.xml's <profile id="installer">).
# ---------------------------------------------------------------------
if (-not $SkipMavenBuild) {
    Write-Step "Running mvn clean package -Pinstaller..."
    & mvn clean package -Pinstaller -DskipTests
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed with exit code $LASTEXITCODE." }
}

$jarPath = Join-Path $root 'target\hfms.jar'
if (-not (Test-Path $jarPath)) { throw "Expected $jarPath after the Maven build but it's missing." }

# spring-boot:repackage renames the pre-repackage plain jar (classes at
# jar root) to hfms.jar.original before replacing hfms.jar with the fat,
# BOOT-INF/-nested repackaged one -- documented, long-standing behavior
# of that plugin goal, not an implementation detail we're relying on by
# luck. We need that plain jar separately: jpackage's native launcher
# resolves --main-class via a flat classpath scan of --main-jar, which
# can't see classes nested under hfms.jar's BOOT-INF/classes/ (confirmed
# by testing -- `javap -cp hfms.jar com.hopestar.hfms.launcher.Launcher`
# fails with "class not found", while the same call against
# hfms.jar.original succeeds). Launcher itself has zero external
# dependencies (JDK classes only), so this plain jar is all it needs to
# resolve and run -- it never touches the Spring-annotated classes that
# jar also happens to contain, which do need BOOT-INF/lib and would fail
# to link if actually invoked from this classpath.
$originalJarPath = Join-Path $root 'target\hfms.jar.original'
if (-not (Test-Path $originalJarPath)) { throw "Expected $originalJarPath (pre-repackage jar) after the Maven build but it's missing." }

# ---------------------------------------------------------------------
# 2. Icon: jpackage needs a real .ico on Windows (not .jpeg/.png).
# ---------------------------------------------------------------------
Write-Step "Converting logo to .ico..."
$icoPath = Join-Path $root 'target\generated-resources\icon\hopestar-hfms.ico'
& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $root 'scripts\convert-logo-to-ico.ps1') `
    -SourcePath (Join-Path $root 'src\main\resources\static\images\hopestar-logo.jpeg') `
    -OutputPath $icoPath
if ($LASTEXITCODE -ne 0) { throw "Icon conversion failed." }

# ---------------------------------------------------------------------
# 3. Stage a clean input directory containing only the jar -- jpackage
#    copies its whole --input directory into the app image, and target\
#    is full of unrelated build artifacts we don't want bundled.
# ---------------------------------------------------------------------
$stageDir = Join-Path $root 'target\jpackage-input'
Remove-Item $stageDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $stageDir -Force | Out-Null
Copy-Item $jarPath (Join-Path $stageDir 'hfms.jar')
Copy-Item $originalJarPath (Join-Path $stageDir 'launcher.jar')

# The launcher shells out to this script on first run -- it needs to
# exist as a real file next to the jars in the installed app, not just
# in the source tree, since PowerShell -File needs a filesystem path.
Copy-Item (Join-Path $root 'scripts\install-mysql-headless.ps1') (Join-Path $stageDir 'install-mysql-headless.ps1')

# ---------------------------------------------------------------------
# 4. Module list for the jlinked runtime -- computed via jdeps against
#    the actual jar (see BUILD_NOTES below), then widened with modules
#    jdeps' static analysis is known to miss for reflection/SPI-heavy
#    frameworks (Spring/Hibernate/Jackson/JDBC), most importantly
#    jdk.crypto.ec: without it, the HTTPS listener's TLS handshake
#    fails, since cipher-suite/provider selection is dynamic and
#    invisible to jdeps' bytecode analysis. Verified by an actual
#    HTTPS smoke test against this exact module list (see chat), not
#    just by inspection.
# ---------------------------------------------------------------------
$modules = @(
    'java.base', 'java.compiler', 'java.desktop', 'java.instrument',
    'java.logging', 'java.management', 'java.naming', 'java.net.http',
    'java.prefs', 'java.rmi', 'java.scripting', 'java.security.jgss',
    'java.sql', 'java.sql.rowset', 'java.xml',
    'jdk.crypto.ec', 'jdk.jfr', 'jdk.management', 'jdk.unsupported'
) -join ','

# ---------------------------------------------------------------------
# 5. jpackage.
#
#    --win-per-user-install: no admin rights needed, defaults the
#    install location under the current user's profile rather than
#    Program Files.
#    --install-dir "HopeStarHFMS": pins the exact folder name under that
#    per-user default location, matching the app-data location decision
#    (%LOCALAPPDATA%\HopeStarHFMS) -- verified against the actual built
#    output, not assumed (see chat).
#    --main-class com.hopestar.hfms.launcher.Launcher: overrides the
#    jar's own manifest (Main-Class=Spring Boot's JarLauncher) so
#    jpackage's native launcher runs OUR supervisor first -- it handles
#    first-run MySQL setup, reads the DB credentials file, and starts
#    the real Spring Boot app as a child process with that environment
#    (setting HFMS_PROFILE=prod itself, which is why the old
#    --java-options -Dspring.profiles.active=prod baked into the outer
#    launcher's .cfg is gone -- it would only apply to the supervisor's
#    own JVM, not the child process it spawns).
# ---------------------------------------------------------------------
$version = '1.0.0'  # jpackage requires Major[.Minor[.Patch]] -- pom's "-SNAPSHOT" suffix isn't valid here
$upgradeUuid = '6F912B06-0A9B-4D36-B002-A38B939DB9B6'  # fixed forever -- changing this breaks in-place upgrades

$destDir = Join-Path $root 'target\installer'
Remove-Item $destDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Path $destDir -Force | Out-Null

Write-Step "Running jpackage (--type $Type)..."
$jpackageArgs = @(
    '--type', $Type
    '--input', $stageDir
    '--main-jar', 'launcher.jar'
    '--main-class', 'com.hopestar.hfms.launcher.Launcher'
    '--dest', $destDir
    '--name', 'HopeStar HFMS'
    '--app-version', $version
    '--vendor', 'HopeStar Education Consultancy'
    '--description', 'Finance and student management system for HopeStar Education Consultancy'
    '--icon', $icoPath
    '--add-modules', $modules
    # jpackage's default --jlink-options includes --strip-native-commands,
    # which removes java.exe/javaw.exe from the bundled runtime (its own
    # native launcher doesn't need them -- it links the JVM directly).
    # Our Launcher needs javaw.exe to spawn the real Spring Boot app as a
    # child process, so every other jpackage default is kept except that
    # one (confirmed missing at runtime/bin -- see chat).
    '--jlink-options', '--strip-debug --no-header-files --no-man-pages'
)
if ($Type -ne 'app-image') {
    $jpackageArgs += @(
        '--win-per-user-install'
        '--win-shortcut'
        '--win-menu'
        '--win-menu-group', 'HopeStar HFMS'
        '--win-upgrade-uuid', $upgradeUuid
        '--install-dir', 'HopeStarHFMS'
    )
}

& jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { throw "jpackage failed with exit code $LASTEXITCODE." }

Write-Step "Done. Output in $destDir"
Get-ChildItem $destDir | ForEach-Object {
    Write-Step "  $($_.Name) -- $([Math]::Round($_.Length / 1MB, 1)) MB"
}
