Add-Type -AssemblyName System.Drawing

$width = 512
$height = 512
$bmp = New-Object System.Drawing.Bitmap($width, $height)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

# Cream Textured Background (#F4F1EA)
$bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 244, 241, 234))
$g.FillRectangle($bgBrush, 0, 0, $width, $height)

# Circle Borders (Brown/Orange top arch & Dark Navy bottom arch)
$orangePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 194, 76, 12), 14) # Deep Brown-Orange
$navyPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 15, 42, 86), 14)     # Dark Navy Blue

# Top Deep Orange Arc
$g.DrawArc($orangePen, 35, 35, 442, 442, 200, 140)

# Bottom Dark Navy Arc
$g.DrawArc($navyPen, 35, 35, 442, 442, 20, 150)

# Deep Orange Heart & Paw Print Symbol
$orangeBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 194, 76, 12))

# Deep Orange Paw Main Pad
$g.FillEllipse($orangeBrush, 201, 185, 110, 80)

# Deep Orange Paw Toes
$g.FillEllipse($orangeBrush, 191, 145, 32, 40)
$g.FillEllipse($orangeBrush, 231, 125, 32, 40)
$g.FillEllipse($orangeBrush, 271, 125, 32, 40)
$g.FillEllipse($orangeBrush, 301, 145, 32, 40)

# Dark Navy Footprints & Paw Prints
$navyBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 15, 42, 86))
$g.FillEllipse($navyBrush, 170, 270, 50, 26) # Shoe footprint 1
$g.FillEllipse($navyBrush, 220, 285, 50, 26) # Shoe footprint 2

# Small navy paw prints
$g.FillEllipse($navyBrush, 310, 250, 20, 15)
$g.FillEllipse($navyBrush, 335, 265, 20, 15)
$g.FillEllipse($navyBrush, 315, 280, 20, 15)

# Text: "Baljaguk Donghaeng"
try {
    $fontTitle = New-Object System.Drawing.Font('Arial', 28, [System.Drawing.FontStyle]::Bold)
    $fontSub = New-Object System.Drawing.Font('Arial', 14, [System.Drawing.FontStyle]::Bold)
    $textBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 15, 23, 42))
    $subBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 51, 65, 85))

    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringFormatAlignment]::Center

    $g.DrawString('Baljaguk Donghaeng', $fontTitle, $textBrush, 256, 345, $sf)
    $g.DrawString('Pet Companion Transport Service', $fontSub, $subBrush, 256, 400, $sf)
} catch {}

# Save
$out1 = 'C:\Users\g7033\AndroidStudioProjects\Forpetapp2\baljaguk_app_icon.png'
$out2 = 'C:\Users\g7033\AndroidStudioProjects\Forpetapp2\발자국동행_앱아이콘.png'

$bmp.Save($out1, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Save($out2, [System.Drawing.Imaging.ImageFormat]::Png)

$g.Dispose()
$bmp.Dispose()
Write-Host "New Official Emblem Icon Generated!"
