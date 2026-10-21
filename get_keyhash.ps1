$keystore = "$env:USERPROFILE\.android\debug.keystore"
$keytool = "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe"

if (Test-Path $keystore) {
    Add-Type -TypeDefinition @"
    using System;
    using System.Security.Cryptography;
    using System.IO;

    public class KakaoKeyHash {
        public static string GetHash(string keytoolPath, string keystorePath) {
            System.Diagnostics.Process p = new System.Diagnostics.Process();
            p.StartInfo.FileName = keytoolPath;
            p.StartInfo.Arguments = "-exportcert -alias androiddebugkey -keystore \"" + keystorePath + "\" -storepass android -keypass android";
            p.StartInfo.UseShellExecute = false;
            p.StartInfo.RedirectStandardOutput = true;
            p.StartInfo.CreateNoWindow = true;
            p.Start();

            MemoryStream ms = new MemoryStream();
            p.StandardOutput.BaseStream.CopyTo(ms);
            p.WaitForExit();

            byte[] certBytes = ms.ToArray();
            SHA1 sha1 = SHA1.Create();
            byte[] hash = sha1.ComputeHash(certBytes);
            return Convert.ToBase64String(hash);
        }
    }
"@
    $result = [KakaoKeyHash]::GetHash($keytool, $keystore)
    Write-Host "KAKAO_KEY_HASH: $result"
} else {
    Write-Host "Keystore not found at $keystore"
}
