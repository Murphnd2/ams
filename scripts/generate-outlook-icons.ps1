# Generate the three PNG icons for the Outlook add-in.
# Brand:
#   Navy primary   = #0d5681
#   Accent green   = #87a948
#
# Usage (from repo root):
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts/generate-outlook-icons.ps1

Add-Type -AssemblyName System.Drawing

function New-AmsIcon {
    param(
        [int]    $Size,
        [string] $OutPath
    )

    $bmp = New-Object System.Drawing.Bitmap($Size, $Size)
    $g   = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.Clear([System.Drawing.Color]::Transparent)

    # Brand colors
    $navy   = [System.Drawing.Color]::FromArgb(255, 13, 86, 129)    # #0d5681
    $accent = [System.Drawing.Color]::FromArgb(255, 135, 169, 72)   # #87a948

    # Rounded-rect background (no rounding at 16px — cleaner)
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    if ($Size -le 16) {
        $rect = New-Object System.Drawing.Rectangle -ArgumentList 0, 0, $Size, $Size
        $path.AddRectangle($rect)
    } else {
        $radius = [int]([Math]::Max(2, $Size / 6))
        $d = [int]($radius * 2)
        $w = [int]$Size
        $h = [int]$Size
        $wd = [int]($w - $d)
        $hd = [int]($h - $d)
        $path.AddArc(0,   0,   $d, $d, 180, 90)
        $path.AddArc($wd, 0,   $d, $d, 270, 90)
        $path.AddArc($wd, $hd, $d, $d, 0,   90)
        $path.AddArc(0,   $hd, $d, $d, 90,  90)
        $path.CloseFigure()
    }

    $bgBrush = New-Object System.Drawing.SolidBrush -ArgumentList $navy
    $g.FillPath($bgBrush, $path)
    $bgBrush.Dispose()

    # Accent stripe across the bottom, clipped to the rounded path — 32 and 80 only
    if ($Size -ge 32) {
        $stripeH = [int]([Math]::Max(2, $Size * 0.08))
        $stripeY = [int]($Size - $stripeH)
        $stripeRect   = New-Object System.Drawing.Rectangle -ArgumentList 0, $stripeY, ([int]$Size), $stripeH
        $stripeRegion = New-Object System.Drawing.Region -ArgumentList $path
        $stripeRegion.Intersect($stripeRect)
        $stripeBrush  = New-Object System.Drawing.SolidBrush -ArgumentList $accent
        $g.FillRegion($stripeBrush, $stripeRegion)
        $stripeBrush.Dispose()
        $stripeRegion.Dispose()
    }

    # White bold "A" centered, nudged up so it sits above the stripe
    $family = New-Object System.Drawing.FontFamily -ArgumentList 'Segoe UI'
    $style  = [System.Drawing.FontStyle]::Bold
    if ($Size -le 16) {
        [float]$fontSize = 11.0
    } else {
        [float]$fontSize = [float]($Size * 0.62)
    }
    $pixelUnit = [System.Drawing.GraphicsUnit]::Pixel
    $font = New-Object System.Drawing.Font -ArgumentList $family, $fontSize, $style, $pixelUnit

    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment     = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Center

    [float]$yOffset = 0.0
    if ($Size -ge 32) { [float]$yOffset = -1.0 * [float]($Size * 0.04) }
    [float]$rx = 0.0
    [float]$ry = $yOffset
    [float]$rw = [float]$Size
    [float]$rh = [float]$Size
    $textRect = New-Object System.Drawing.RectangleF -ArgumentList $rx, $ry, $rw, $rh

    $whiteBrush = New-Object System.Drawing.SolidBrush -ArgumentList ([System.Drawing.Color]::White)
    $g.DrawString('A', $font, $whiteBrush, $textRect, $sf)

    $whiteBrush.Dispose()
    $font.Dispose()
    $sf.Dispose()
    $path.Dispose()
    $g.Dispose()

    $bmp.Save($OutPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "Created: $OutPath ($Size x $Size)"
}

$outDir = Join-Path $PSScriptRoot '..\src\main\webapp\outlook'
$outDir = [System.IO.Path]::GetFullPath($outDir)

New-AmsIcon 16 (Join-Path $outDir 'icon-16.png')
New-AmsIcon 32 (Join-Path $outDir 'icon-32.png')
New-AmsIcon 80 (Join-Path $outDir 'icon-80.png')
Write-Host "Done."
