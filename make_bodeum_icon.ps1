Add-Type -AssemblyName System.Drawing

$width = 512
$height = 512
$bmp = New-Object System.Drawing.Bitmap($width, $height)
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias

# Pink to Purple Gradient Background filling full 512x512 canvas (No outer white border!)
$rect = New-Object System.Drawing.Rectangle(0, 0, $width, $height)
$color1 = [System.Drawing.Color]::FromArgb(255, 244, 114, 182) # Soft Pink (#F472B6)
$color2 = [System.Drawing.Color]::FromArgb(255, 192, 132, 252) # Light Purple (#C084FC)
$linBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush($rect, $color1, $color2, 45)

$g.FillRectangle($linBrush, 0, 0, $width, $height)

# Soft Cream Paw
$creamColor = [System.Drawing.Color]::FromArgb(245, 255, 241, 242)
$creamBrush = New-Object System.Drawing.SolidBrush($creamColor)

# Main Pad (Center Heart-Shaped Pad)
$g.FillEllipse($creamBrush, 140, 190, 230, 170)

# Heart Symbol inside Main Pad
$pinkDark = [System.Drawing.Color]::FromArgb(220, 236, 72, 153)
$pinkBrush = New-Object System.Drawing.SolidBrush($pinkDark)
$g.FillEllipse($pinkBrush, 210, 235, 50, 50)
$g.FillEllipse($pinkBrush, 250, 235, 50, 50)

$p1 = New-Object System.Drawing.Point 210, 260
$p2 = New-Object System.Drawing.Point 300, 260
$p3 = New-Object System.Drawing.Point 255, 305
$pArray = [System.Drawing.Point[]]@($p1, $p2, $p3)
$g.FillPolygon($pinkBrush, $pArray)

# 4 Toe Pads
$g.FillEllipse($creamBrush, 130, 120, 68, 85)
$g.FillEllipse($creamBrush, 220, 80, 70, 88)
$g.FillEllipse($creamBrush, 312, 120, 68, 85)
$g.FillEllipse($creamBrush, 380, 195, 62, 75)

# Plus Cross at bottom right
$g.FillRectangle($creamBrush, 315, 315, 55, 18)
$g.FillRectangle($creamBrush, 333, 297, 18, 55)

# Save
$out1 = 'C:\Users\g7033\AndroidStudioProjects\Forpetapp2\bodeumpet_app_icon.png'

$bmp.Save($out1, [System.Drawing.Imaging.ImageFormat]::Png)

$g.Dispose()
$bmp.Dispose()
Write-Host "Full Bleed BodeumPet App Icon Generated!"
