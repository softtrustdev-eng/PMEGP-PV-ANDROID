package in.gov.pmegp.pv.core.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;

/** C10 §3. PKCS#1 v1.5 padding is randomised, so no fixed ciphertext can be asserted. */
public class RsaEncryptorTest {

    private static final String LOGIN_JSON = "{\"UserID\":\"appmegp00000\",\"Password\":\"example\"}";

    /** C10-R10: the server expects a 256-byte block = 344 Base64 chars. This single
     *  assertion catches a wrong key size or wrong padding without needing the server. */
    @Test public void c10_r10_outputIsExactly344Chars() throws Exception {
        assertEquals(344, RsaEncryptor.encrypt(LOGIN_JSON).length());
    }

    @Test public void c10_r08_paddingIsRandomised() throws Exception {
        assertNotEquals(RsaEncryptor.encrypt(LOGIN_JSON), RsaEncryptor.encrypt(LOGIN_JSON));
    }

    @Test public void c10_r11_emptyReturnsEmpty() throws Exception {
        assertEquals("", RsaEncryptor.encrypt(""));
        assertEquals("", RsaEncryptor.encrypt(null));
    }
}
