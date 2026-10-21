Add-Type -AssemblyName System.Drawing

$width = 512
$height = 512
$bmp = New-Object System.Drawing.Bitmap($width, $height)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

# Background Sky-Blue #0284C7
$skyBlue = [System.Drawing.Color]::FromArgb(255, 2, 132, 199)
$bgBrush = New-Object System.Drawing.SolidBrush($skyBlue)
$g.FillRectangle($bgBrush, 0, 0, $width, $height)

# White Paw Brush
$whiteBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::White)

# Main Pad
$g.FillEllipse($whiteBrush, 176, 200, 160, 120)

# 4 Toe Pads
$g.FillEllipse($whiteBrush, 166, 145, 55, 65)
$g.FillEllipse($whiteBrush, 228, 115, 55, 65)
$g.FillEllipse($whiteBrush, 290, 145, 55, 65)
$g.FillEllipse($whiteBrush, 116, 205, 50, 60)
$g.FillEllipse($whiteBrush, 346, 205, 50, 60)

# Text 'ForPet'
try {
    $font = New-Object System.Drawing.Font('Arial', 32, [System.Drawing.FontStyle]::Bold)
    $sf = New-Object System.Drawing.StringFormat
    $sf.Alignment = [System.Drawing.StringFormatAlignment]::Center
    $g.DrawString('ForPet', $font, $whiteBrush, 256, 360, $sf)
} catch {
    # Ignore font error if any
}

# Save
$out1 = 'C:\Users\g7033\AndroidStudioProjects\Forpetapp2\forpet_app_icon.png'
$out2 = 'C:\Users\g7033\AppData\Local\Google\AndroidStudio2026.2.1\projects\forpetapp2.65492385\.artifacts\931392ae-5d6f-4451-a46c-50ce266309ad\forpet_app_icon.png'

$bmp.Save($out1, [System.Drawing.Imaging.ImageFormat]::Png)
$bmp.Save($out2, [System.Drawing.Imaging.ImageFormat]::Png)

$g.Dispose()
$bmp.Dispose()
Write-Host 'PNG App Icon generated successfully!'
