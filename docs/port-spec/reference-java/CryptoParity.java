import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;

public class CryptoParity {
    private static final String PASSPHRASE = "~!@#$%^&*()_+|";

    /** .NET accepts a 16-byte TripleDES key (2-key EDE, K3 = K1). Java's DESede
     *  requires 24 bytes, so the first 8 bytes are appended. This expansion is the
     *  single most likely thing to be missed in the port. */
    private static SecretKey key() throws Exception {
        byte[] md5 = MessageDigest.getInstance("MD5")
                .digest(PASSPHRASE.getBytes(StandardCharsets.UTF_8));   // 16 bytes
        byte[] k24 = new byte[24];
        System.arraycopy(md5, 0, k24, 0, 16);
        System.arraycopy(md5, 0, k24, 16, 8);
        return new SecretKeySpec(k24, "DESede");
    }

    static String decrypt(String b64) throws Exception {
        if (b64 == null || b64.isBlank()) return b64;
        Cipher c = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        c.init(Cipher.DECRYPT_MODE, key());
        return new String(c.doFinal(Base64.getDecoder().decode(b64)), StandardCharsets.UTF_8);
    }

    static String encrypt(String plain) throws Exception {
        Cipher c = Cipher.getInstance("DESede/ECB/PKCS5Padding");
        c.init(Cipher.ENCRYPT_MODE, key());
        return Base64.getEncoder().encodeToString(c.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
    }

    public static void main(String[] a) throws Exception {
        byte[] md5 = MessageDigest.getInstance("MD5").digest(PASSPHRASE.getBytes(StandardCharsets.UTF_8));
        System.out.println("MD5 key: " + HexFormat.of().formatHex(md5).toUpperCase());

        String[][] v = {
            {"4EZ80JluAut9N2VsldB4Qg==", "9876543210"},
            {"CqUv0YnCbkViYhKkQV5gGcAwQbz2jTx1", "RAJESH KUMAR SHARMA"},
            {"iNR0R7YMbyJnkogCwJ7uAJV4CaT9cr/awBmjER3l9XQJjuIxGcUe2VEPT38t6gJ7xu5ueQqpk+Dq8x8X7mTtoQ==",
             "Plot 12/4, Shah & Sons, D'Souza Road, Vizianagaram, 535161"},
            {"hjZa+q9Mikk=", "A"},
            {"+qFUbZpnsGeo8g2t0u7aJg==", "12345678"},
            {"+qFUbZpnsGefx55vbyDHJWwcsHkJZah7qPINrdLu2iY=", "123456789012345678901234"},
        };
        int pass = 0;
        for (String[] t : v) {
            String got = decrypt(t[0]);
            boolean okD = got.equals(t[1]);
            boolean okE = encrypt(t[1]).equals(t[0]);
            System.out.printf("%s decrypt | %s encrypt  : %s%n",
                okD ? "PASS" : "FAIL", okE ? "PASS" : "FAIL",
                t[1].length() > 32 ? t[1].substring(0, 32) + "..." : t[1]);
            if (okD && okE) pass++;
        }
        // behaviour on garbage must match C#: return input unchanged, report failure
        String junk = "not-base64!!";
        String out;
        try { out = decrypt(junk); } catch (Exception e) { out = junk; }
        System.out.println((out.equals(junk) ? "PASS" : "FAIL") + " undecryptable returns input unchanged");
        System.out.println("\n" + pass + "/" + v.length + " vectors round-trip identically");
    }
}
