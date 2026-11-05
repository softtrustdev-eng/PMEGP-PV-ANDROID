package in.gov.pmegp.pv.core.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Decrypts field values returned by the API.
 *
 * <p>Spec: docs/port-spec/cross-cutting/C10-cryptography.md (C10-R01..R07).
 * Mirrors the server's {@code Tssipl.SecurityHelper.Decrypt(s, useHashing: true)}.
 *
 * <p>Verified against 6 reference vectors generated from the .NET implementation:
 * all 6 decrypt and re-encrypt byte-identically. See TripleDesCryptoTest.
 */
public final class TripleDesCrypto {

    private static final String PASSPHRASE = "~!@#$%^&*()_+|";
    private static final String TRANSFORM = "DESede/ECB/PKCS5Padding";

    private TripleDesCrypto() { }

    /**
     * C10-R02 / C10-R03.
     *
     * <p>MD5 yields 16 bytes. .NET accepts that directly as a 2-key TripleDES key
     * (K3 = K1); Java's DESede requires 24 and throws on 16, so the first 8 bytes
     * are appended.
     *
     * <p><b>Do not "fix" a key-length exception by truncating, zero-padding, or
     * switching to plain DES.</b> Each of those produces a cipher that runs and
     * silently returns garbage for every encrypted field in the app.
     */
    private static SecretKey key() throws Exception {
        byte[] md5 = MessageDigest.getInstance("MD5")
                .digest(PASSPHRASE.getBytes(StandardCharsets.UTF_8));   // 16 bytes
        byte[] k24 = new byte[24];
        System.arraycopy(md5, 0, k24, 0, 16);
        System.arraycopy(md5, 0, k24, 16, 8);                            // K3 = K1
        return new SecretKeySpec(k24, "DESede");
    }

    /** Result of a decrypt that reports whether it actually succeeded (C10-R06). */
    public static final class Result {
        public final String value;
        public final boolean decrypted;
        Result(String value, boolean decrypted) { this.value = value; this.decrypted = decrypted; }
    }

    /**
     * C10-R05: any failure returns the input unchanged. Not every value from the API
     * is encrypted, so a plaintext field passed through here must survive intact.
     */
    public static String decrypt(String encrypted) {
        return tryDecrypt(encrypted).value;
    }

    /**
     * C10-R06: for callers that must tell "genuinely plain/undecryptable" from
     * "a real decrypted value" — e.g. deciding whether a field should be editable.
     */
    public static Result tryDecrypt(String encrypted) {
        // C10-R04: null / empty / blank passes through and counts as success.
        if (encrypted == null || encrypted.trim().isEmpty()) {
            return new Result(encrypted, true);
        }
        try {
            Cipher c = Cipher.getInstance(TRANSFORM);
            c.init(Cipher.DECRYPT_MODE, key());
            byte[] plain = c.doFinal(Base64.getDecoder().decode(encrypted));
            return new Result(new String(plain, StandardCharsets.UTF_8), true);
        } catch (Exception e) {
            return new Result(encrypted, false);
        }
    }

    /** Present for round-trip testing only; the app never encrypts with this key. */
    static String encrypt(String plain) throws Exception {
        Cipher c = Cipher.getInstance(TRANSFORM);
        c.init(Cipher.ENCRYPT_MODE, key());
        return Base64.getEncoder()
                .encodeToString(c.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
    }
}
