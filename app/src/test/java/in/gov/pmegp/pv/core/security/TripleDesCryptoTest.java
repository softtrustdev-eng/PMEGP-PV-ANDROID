package in.gov.pmegp.pv.core.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * C10 §2 — vectors generated from the live .NET CryptoHelper.
 * If any of these fail, every encrypted field in the app is being read wrong.
 */
public class TripleDesCryptoTest {

    private static final String[][] VECTORS = {
        {"4EZ80JluAut9N2VsldB4Qg==", "9876543210"},
        {"CqUv0YnCbkViYhKkQV5gGcAwQbz2jTx1", "RAJESH KUMAR SHARMA"},
        {"iNR0R7YMbyJnkogCwJ7uAJV4CaT9cr/awBmjER3l9XQJjuIxGcUe2VEPT38t6gJ7xu5ueQqpk+Dq8x8X7mTtoQ==",
         "Plot 12/4, Shah & Sons, D'Souza Road, Vizianagaram, 535161"},
        {"hjZa+q9Mikk=", "A"},
        {"+qFUbZpnsGeo8g2t0u7aJg==", "12345678"},
        {"+qFUbZpnsGefx55vbyDHJWwcsHkJZah7qPINrdLu2iY=", "123456789012345678901234"},
    };

    @Test public void c10_r01_decryptsEveryReferenceVector() {
        for (String[] v : VECTORS) assertEquals(v[1], TripleDesCrypto.decrypt(v[0]));
    }

    /** C10-R03: proves the 16 -> 24 byte key expansion matches .NET's 2-key EDE. */
    @Test public void c10_r03_reEncryptsIdentically() throws Exception {
        for (String[] v : VECTORS) assertEquals(v[0], TripleDesCrypto.encrypt(v[1]));
    }

    @Test public void c10_r04_blankPassesThrough() {
        assertEquals("", TripleDesCrypto.decrypt(""));
        assertEquals(null, TripleDesCrypto.decrypt(null));
        assertEquals("   ", TripleDesCrypto.decrypt("   "));
    }

    /** C10-R05: not every API value is encrypted; plaintext must survive intact. */
    @Test public void c10_r05_undecryptableReturnsInputUnchanged() {
        assertEquals("not-base64!!", TripleDesCrypto.decrypt("not-base64!!"));
        assertEquals("RAJESH KUMAR", TripleDesCrypto.decrypt("RAJESH KUMAR"));
    }

    @Test public void c10_r06_reportsWhetherItActuallyDecrypted() {
        assertTrue(TripleDesCrypto.tryDecrypt(VECTORS[0][0]).decrypted);
        assertEquals(false, TripleDesCrypto.tryDecrypt("not-base64!!").decrypted);
    }
}
