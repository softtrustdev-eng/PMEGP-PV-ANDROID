package in.gov.pmegp.pv.core.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

/**
 * Encrypts the login credential blob with the server's public key.
 *
 * <p>Spec: docs/port-spec/cross-cutting/C10-cryptography.md (C10-R08..R11),
 * C01-R19. Resolves VAPT #2 (CWE-311).
 *
 * <p>Server decrypts via {@code RSAHelper.DecryptWithXmlPrivateKey}, which expects
 * a 256-byte block = 344 Base64 characters.
 */
public final class RsaEncryptor {

    // C10-R09: same modulus/exponent as the .NET <RSAKeyValue> XML. A public key is
    // not a secret. This is NOT certificate pinning — it does not authenticate the server.
    private static final String MODULUS =
        "u2wovmBo6SjrgoigtHoXHFAOW8ALujQbrLdc97F/8ev75YjX3tE1Nv4rewWI9Nb/CgvwkNlj75gp3b7Nq"
      + "OmdnLnvoW7FcOvm0PNiSfoZ9+qvOYJD8C1emLZvlEGP5z94EGwSzP4Unqh9F9/FxQD17CfBYfqyf2QZ9k"
      + "cDTjoy1CpE2cozbX3cqW4COADOdvqyxdccAH2Udo0hmgnw50ZF5N1urpeleoqgghxSj/fLqN71HWFJtLv"
      + "rvQskuHOtnVHG5rhrUqbyPwq5bWEb7gPSJrY/JlXmAyFFJPXvjn83J/0oVdnQtVlY/Mgt3uy5oOW+X20f"
      + "prhb/obMqbRKBkElWQ==";
    private static final String EXPONENT = "AQAB";

    // C10-R08: PKCS#1 v1.5, not OAEP. The server will reject OAEP.
    private static final String TRANSFORM = "RSA/ECB/PKCS1Padding";

    private RsaEncryptor() { }

    private static PublicKey key() throws Exception {
        // The signum-1 form is required: .NET writes a raw big-endian magnitude, so a
        // modulus whose top bit is set would otherwise be read as a negative number.
        BigInteger n = new BigInteger(1, Base64.getDecoder().decode(MODULUS));
        BigInteger e = new BigInteger(1, Base64.getDecoder().decode(EXPONENT));
        return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
    }

    /**
     * C10-R10: plaintext must be <= 245 bytes; output is always 344 Base64 characters.
     * C10-R11: empty or null plaintext returns "".
     */
    public static String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.isEmpty()) return "";
        Cipher c = Cipher.getInstance(TRANSFORM);
        c.init(Cipher.ENCRYPT_MODE, key());
        return Base64.getEncoder()
                .encodeToString(c.doFinal(plainText.getBytes(StandardCharsets.UTF_8)));
    }
}
