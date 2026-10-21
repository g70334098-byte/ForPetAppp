$keystore = "$env:USERPROFILE\.android\debug.keystore"
$keytool = "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"

$process = Start-Process -FilePath $keytool -ArgumentList "-list -v -keystore `"$keystore`" -alias androiddebugkey -storepass android -keypass android" -NoNewWindow -PassThru -RedirectStandardOutput "sha1_out.txt"
$process.WaitForExit()

Get-Content "sha1_out.txt" | Select-String "SHA1:"
