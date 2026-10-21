Add-Type -TypeDefinition @"
using System;
using System.Security.Cryptography.X509Certificates;
using System.Security.Cryptography;

public class KeystoreCert {
    public static string GetSHA1(string keystorePath) {
        try {
            X509Certificate2Collection store = new X509Certificate2Collection();
            store.Import(keystorePath, "android", X509KeyStorageFlags.DefaultKeySet);
            foreach (X509Certificate2 cert in store) {
                using (SHA1 sha1 = SHA1.Create()) {
                    byte[] hash = sha1.ComputeHash(cert.RawData);
                    return BitConverter.ToString(hash).Replace("-", ":");
                }
            }
        } catch (Exception ex) {
            return ex.Message;
        }
        return "No cert found";
    }
}
"@
$keystore = "$env:USERPROFILE\.android\debug.keystore"
$sha1 = [KeystoreCert]::GetSHA1($keystore)
Write-Host "SHA1_FINGERPRINT: $sha1"
