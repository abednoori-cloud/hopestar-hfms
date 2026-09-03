# Generates the self-signed PKCS12 keystore used by application-prod.yml's
# server.ssl.* config, plus a properties file holding its random password.
#
# Invoked by the pom.xml "installer" Maven profile (exec-maven-plugin, bound
# to the generate-resources phase) -- NOT part of the default build, so
# `mvn clean package` / `mvn spring-boot:run` without -Pinstaller never runs
# this and never needs keytool. See pom.xml for the profile wiring.
#
# Idempotent: if both output files already exist (e.g. a `package` re-run
# without `clean`), generation is skipped so the password/cert don't churn.

param(
    [Parameter(Mandatory = $true)][string]$KeystorePath,
    [Parameter(Mandatory = $true)][string]$PropertiesPath
)

$ErrorActionPreference = "Stop"

if ((Test-Path $KeystorePath) -and (Test-Path $PropertiesPath)) {
    Write-Host "generate-keystore: $KeystorePath already exists, skipping."
    exit 0
}

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $KeystorePath) | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path -Parent $PropertiesPath) | Out-Null

if (Test-Path $KeystorePath) {
    Remove-Item $KeystorePath -Force
}

$keytool = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME "bin\keytool.exe" } else { "keytool" }

$passwordBytes = New-Object byte[] 24
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($passwordBytes)
$rng.Dispose()
$password = [Convert]::ToBase64String($passwordBytes) -replace '[+/=]', ''

& $keytool -genkeypair -alias hfms -keyalg RSA -keysize 2048 -validity 3650 `
    -storetype PKCS12 -keystore $KeystorePath -storepass $password `
    -dname "CN=localhost, OU=HopeStar, O=HopeStar, C=CA" `
    -ext "SAN=dns:localhost,ip:127.0.0.1"

if ($LASTEXITCODE -ne 0) {
    throw "keytool failed with exit code $LASTEXITCODE"
}

"hfms.ssl.keystore.password=$password" | Out-File -FilePath $PropertiesPath -Encoding ascii -NoNewline

Write-Host "generate-keystore: wrote $KeystorePath"
