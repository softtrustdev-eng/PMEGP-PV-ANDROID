import javax.crypto.Cipher;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

public class RsaParity {
    // Same modulus/exponent as RsaHelper.PublicKeyXml, Base64 as .NET writes them.
    static final String MOD = "u2wovmBo6SjrgoigtHoXHFAOW8ALujQbrLdc97F/8ev75YjX3tE1Nv4rewWI9Nb/CgvwkNlj75gp3b7NqOmdnLnvoW7FcOvm0PNiSfoZ9+qvOYJD8C1emLZvlEGP5z94EGwSzP4Unqh9F9/FxQD17CfBYfqyf2QZ9kcDTjoy1CpE2cozbX3cqW4COADOdvqyxdccAH2Udo0hmgnw50ZF5N1urpeleoqgghxSj/fLqN71HWFJtLvrvQskuHOtnVHG5rhrUqbyPwq5bWEb7gPSJrY/JlXmAyFFJPXvjn83J/0oVdnQtVlY/Mgt3uy5oOW+X20fprhb/obMqbRKBkElWQ==";
    static final String EXP = "AQAB";

    static PublicKey key() throws Exception {
        // .NET XML holds raw big-endian magnitude; prepend 0x00 so BigInteger reads it unsigned.
        BigInteger n = new BigInteger(1, Base64.getDecoder().decode(MOD));
        BigInteger e = new BigInteger(1, Base64.getDecoder().decode(EXP));
        System.out.println("modulus bits: " + n.bitLength() + "   exponent: " + e);
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
    }

    public static void main(String[] a) throws Exception {
        PublicKey k = key();
        String payload = "{\"userName\":\"appmegp00000\",\"password\":\"example\"}";
        Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        c.init(Cipher.ENCRYPT_MODE, k);
        String b64 = Base64.getEncoder().encodeToString(c.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        System.out.println((b64.length() == 344 ? "PASS" : "FAIL")
            + " Base64 length " + b64.length() + " (server expects 344 = 256-byte block)");
        System.out.println((payload.getBytes(StandardCharsets.UTF_8).length <= 245 ? "PASS" : "FAIL")
            + " payload " + payload.getBytes(StandardCharsets.UTF_8).length + " bytes <= 245 limit");
        // PKCS#1 v1.5 is randomised: two encryptions of the same text must differ.
        c.init(Cipher.ENCRYPT_MODE, k);
        String b64b = Base64.getEncoder().encodeToString(c.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        System.out.println((!b64.equals(b64b) ? "PASS" : "FAIL") + " PKCS#1 v1.5 randomised padding (ciphertexts differ)");
    }
}
