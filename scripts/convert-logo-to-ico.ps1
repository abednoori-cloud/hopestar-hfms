<#
    Converts the HopeStar logo (JPEG, no alpha channel) into a proper
    multi-resolution Windows .ico for jpackage's --icon on Windows, which
    requires .ico specifically (unlike macOS/.icns or Linux/.png).

    Builds a real ICO container with PNG-compressed frames at the
    standard Windows icon sizes (16/32/48/256), rather than a single-size
    fake .ico -- Windows picks the frame it needs per context (taskbar,
    Start menu tile, installer wizard, etc).

    Note: the source JPEG has no transparency, so the icon renders as a
    solid square, not a shaped cutout -- flagged as a known limitation,
    not silently "fixed" by guessing at a mask.
#>
[CmdletBinding()]
param(
    [string]$SourcePath = "$PSScriptRoot\..\src\main\resources\static\images\hopestar-logo.jpeg",
    [string]$OutputPath = "$PSScriptRoot\..\target\generated-resources\icon\hopestar-hfms.ico",
    [int[]]$Sizes = @(16, 32, 48, 256)
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

$outDir = Split-Path $OutputPath -Parent
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir -Force | Out-Null }

$source = [System.Drawing.Image]::FromFile((Resolve-Path $SourcePath))
try {
    $frames = foreach ($size in $Sizes) {
        $bmp = New-Object System.Drawing.Bitmap $size, $size
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        try {
            $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
            $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
            $g.DrawImage($source, 0, 0, $size, $size)
        } finally { $g.Dispose() }

        $ms = New-Object System.IO.MemoryStream
        $bmp.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()
        [PSCustomObject]@{ Size = $size; Bytes = $ms.ToArray() }
    }
} finally {
    $source.Dispose()
}

$fs = [System.IO.File]::Open($OutputPath, [System.IO.FileMode]::Create)
$bw = New-Object System.IO.BinaryWriter $fs
try {
    # ICONDIR header
    $bw.Write([UInt16]0)          # reserved
    $bw.Write([UInt16]1)          # type: 1 = icon
    $bw.Write([UInt16]$frames.Count)

    $dataOffset = 6 + (16 * $frames.Count)
    foreach ($f in $frames) {
        $dim = if ($f.Size -ge 256) { 0 } else { $f.Size }  # 0 means 256 in ICO format
        $bw.Write([Byte]$dim)               # width
        $bw.Write([Byte]$dim)               # height
        $bw.Write([Byte]0)                  # color palette
        $bw.Write([Byte]0)                  # reserved
        $bw.Write([UInt16]1)                # color planes
        $bw.Write([UInt16]32)               # bits per pixel
        $bw.Write([UInt32]$f.Bytes.Length)  # size of PNG data
        $bw.Write([UInt32]$dataOffset)      # offset of PNG data
        $dataOffset += $f.Bytes.Length
    }
    foreach ($f in $frames) { $bw.Write($f.Bytes) }
} finally {
    $bw.Close()
    $fs.Close()
}

Write-Host "Wrote $OutputPath ($([Math]::Round((Get-Item $OutputPath).Length / 1KB, 1)) KB, sizes: $($Sizes -join ', '))"
