Add-Type -AssemblyName System.Drawing

$width = 512
$height = 512
$bmp = New-Object System.Drawing.Bitmap($width, $height)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

# Transparent / White Base
$g.Clear([System.Drawing.Color]::Transparent)

# Cream Textured Inner Fill (#F4F1EA)
$bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 244, 241, 234))
$g.FillEllipse($bgBrush, 12, 12, 488, 488)

# Tight Outer Circle Borders (Brown/Orange top arch & Dark Navy bottom arch)
$orangePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 194, 76, 12), 16) # Deep Brown-Orange
$navyPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 15, 42, 86), 16)     # Dark Navy Blue

# Top Deep Orange Arc (Tight fit to edge)
$g.DrawArc($orangePen, 20, 20, 472, 472, 200, 140)

# Bottom Dark Navy Arc (Tight fit to edge)
$g.DrawArc($navyPen, 20, 20, 472, 472, 20, 150)

# Deep Orange Heart & Paw Print Symbol
$orangeBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 194, 76, 12))

# Deep Orange Paw Main Pad
$g.FillEllipse($orangeBrush, 201, 180, 110, 80)

# Deep Orange Paw Toes
$g.FillEllipse($orangeBrush, 191, 140, 32, 40)
$g.FillEllipse($orangeBrush, 231, 120, 32, 40)
$g.FillEllipse($orangeBrush, 271, 120, 32, 40)
$g.FillEllipse($orangeBrush, 301, 140, 32, 40)

# Dark Navy Footprints & Paw Prints
$navyBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 15, 42, 86))
$g.FillEllipse($navyBrush, 170, 265, 50, 26) # Shoe footprint 1
$g.FillEllipse($navyBrush, 220, 280, 50, 26) # Shoe footprint 2

# Small navy paw prints
$g.FillEllipse($navyBrush, 310, 245, 20, 15)
$g.FillEllipse($navyBrush, 335, 260, 20, 15)
$g.FillEllipse($navyBrush, 315, 275, 20, 15)

# Text: "Baljaguk Donghaeng"
try {
    $fontTitle = New-Object System.Drawing.Font('Arial', 29, [System.Drawing.FontStyle]::Bold)
    $fontSub = New-Object System.Drawing.Font('Arial', 14, [System.Drawing.FontStyle]::Bold)
    $textBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 15, 23, 42))
    $subBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 51, 65, 85))

    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringFormatAlignment]::Center

    $g.DrawString('Baljaguk Donghaeng', $fontTitle, $textBrush, 256, 340, $sf)
    $g.DrawString('Pet Companion Transport Service', $fontSub, $subBrush, 256, 395, $sf)
} catch {}

# Save
$out1 = 'C:\Users\g7033\AndroidStudioProjects\Forpetapp2\baljaguk_app_icon.png'

$bmp.Save($out1, [System.Drawing.Imaging.ImageFormat]::Png)

$g.Dispose()
$bmp.Dispose()
Write-Host "Tight Cropped Official Emblem Icon Generated!"
