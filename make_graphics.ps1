Add-Type -AssemblyName System.Drawing

$srcPath = "C:\Users\g7033\AndroidStudioProjects\Forpetapp2\real_app_icon.png"
$iconPath = "C:\Users\g7033\AndroidStudioProjects\Forpetapp2\playstore_icon_512.png"
$featurePath = "C:\Users\g7033\AndroidStudioProjects\Forpetapp2\playstore_feature_1024x500.png"

$src = [System.Drawing.Image]::FromFile($srcPath)

# 1. 512x512 앱 아이콘
$icon = New-Object System.Drawing.Bitmap 512, 512
$g = [System.Drawing.Graphics]::FromImage($icon)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$g.Clear([System.Drawing.Color]::White)
$g.DrawImage($src, 0, 0, 512, 512)
$g.Dispose()
$icon.Save($iconPath, [System.Drawing.Imaging.ImageFormat]::Png)
$icon.Dispose()

# 2. 1024x500 그래픽 이미지
$feat = New-Object System.Drawing.Bitmap 1024, 500
$gf = [System.Drawing.Graphics]::FromImage($feat)
$gf.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$gf.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
$gf.Clear([System.Drawing.Color]::FromArgb(248, 250, 252))

# 아이콘 로고 좌측 배치
$gf.DrawImage($src, 50, 50, 400, 400)

$font = New-Object System.Drawing.Font('Arial', 46, [System.Drawing.FontStyle]::Bold)
$brush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(15, 23, 42))
$gf.DrawString('BALDONG', $font, $brush, 480, 150)

$font2 = New-Object System.Drawing.Font('Arial', 20, [System.Drawing.FontStyle]::Bold)
$brush2 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(217, 119, 6))
$gf.DrawString('WithPaw Pet Care Service', $font2, $brush2, 485, 240)

$font3 = New-Object System.Drawing.Font('Arial', 15, [System.Drawing.FontStyle]::Regular)
$brush3 = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(100, 116, 139))
$gf.DrawString('24H Emergency: 010-3786-5394', $font3, $brush3, 485, 295)

$gf.Dispose()
$feat.Save($featurePath, [System.Drawing.Imaging.ImageFormat]::Png)
$feat.Dispose()

$src.Dispose()
Write-Host "REAL_ICON_SUCCESS"
