$sourceDir = "C:\Users\g7033\AndroidStudioProjects\Forpetapp2"
$zipPath = "C:\Users\g7033\Desktop\ForPetApp_HomeSync.zip"

if (Test-Path $zipPath) { Remove-Item $zipPath -Force }

$itemsToZip = Get-ChildItem $sourceDir | Where-Object { $_.Name -ne "build" -and $_.Name -ne ".gradle" } | Select-Object -ExpandProperty FullName

Compress-Archive -Path $itemsToZip -DestinationPath $zipPath -Force
Write-Host "ZIP_SUCCESS: $zipPath"
