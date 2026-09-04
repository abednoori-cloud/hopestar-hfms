<#
    One-off generator for the launcher's bundled system-tray icon
    (src/main/resources/launcher/tray-icon.png). Unlike the installer's
    .ico (regenerated every build in build-installer.ps1), this PNG is a
    committed source resource -- the launcher loads it from the classpath
    at runtime via getResourceAsStream, so it needs to exist in the repo,
    not be generated fresh per-build.

    Re-run this manually only if the source logo changes.
#>
[CmdletBinding()]
param(
    [string]$SourcePath = "C:\Users\abidn\Desktop\hopestar-hfms\src\main\resources\static\images\hopestar-logo.jpeg",
    [string]$OutputPath = "C:\Users\abidn\Desktop\hopestar-hfms\src\main\resources\launcher\tray-icon.png",
    [int]$Size = 32
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$outDir = Split-Path $OutputPath -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

$source = [System.Drawing.Image]::FromFile((Resolve-Path $SourcePath))
try {
    $bmp = New-Object System.Drawing.Bitmap $Size, $Size
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    try {
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
        $g.DrawImage($source, 0, 0, $Size, $Size)
    } finally { $g.Dispose() }
    $bmp.Save($OutputPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
} finally {
    $source.Dispose()
}

Write-Host "Wrote $OutputPath ($([Math]::Round((Get-Item $OutputPath).Length / 1KB, 1)) KB, ${Size}x${Size})"
