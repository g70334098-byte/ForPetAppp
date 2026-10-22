Add-Type -AssemblyName System.Drawing

$width = 512
$height = 512
$bmp = New-Object System.Drawing.Bitmap($width, $height)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

# Cream/Light Background (#FAF9F6)
$bgBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 250, 249, 246))
$g.FillRectangle($bgBrush, 0, 0, $width, $height)

# Circle Borders (Orange top arch & Blue bottom arch)
$orangePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 249, 115, 22), 12)
$bluePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 56, 189, 248), 12)

# Top Orange Arc
$g.DrawArc($orangePen, 35, 35, 442, 442, 200, 140)

# Bottom Blue Arc
$g.DrawArc($bluePen, 35, 35, 442, 442, 20, 150)

# Orange Heart & Paw Print Symbol
$orangeBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 249, 115, 22))

# Orange Paw Main Pad
$g.FillEllipse($orangeBrush, 201, 185, 110, 80)

# Orange Paw Toes
$g.FillEllipse($orangeBrush, 191, 145, 32, 40)
$g.FillEllipse($orangeBrush, 231, 125, 32, 40)
$g.FillEllipse($orangeBrush, 271, 125, 32, 40)
$g.FillEllipse($orangeBrush, 301, 145, 32, 40)

# Blue Footprints & Paw Prints
$blueBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 56, 189, 248))
$g.FillEllipse($blueBrush, 170, 270, 50, 26) # Shoe footprint 1
$g.FillEllipse($blueBrush, 220, 285, 50, 26) # Shoe footprint 2

# Small blue paw prints
$g.FillEllipse($blueBrush, 310, 250, 20, 15)
$g.FillEllipse($blueBrush, 335, 265, 20, 15)
$g.FillEllipse($blueBrush, 315, 280, 20, 15)

# Text: "Baljaguk Donghaeng"
try {
    $fontTitle = New-Object System.Drawing.Font('Arial', 28, [System.Drawing.FontStyle]::Bold)
    $fontSub = New-Object System.Drawing.Font('Arial', 14, [System.Drawing.FontStyle]::Bold)
    $textBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 30, 41, 59))
    $subBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 100, 116, 139))

    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringFormatAlignment]::Center

    $g.DrawString('Baljaguk Donghaeng', $fontTitle, $textBrush, 256, 345, $sf)
    $g.DrawString('Pet Companion Transport Service', $fontSub, $subBrush, 256, 400, $sf)
} catch {}

# Save
$out1 = 'C:\Users\g7033\AndroidStudioProjects\Forpetapp2\baljaguk_app_icon.png'

$bmp.Save($out1, [System.Drawing.Imaging.ImageFormat]::Png)

$g.Dispose()
$bmp.Dispose()
Write-Host "Baljaguk Donghaeng Official Emblem Generated Successfully!"
