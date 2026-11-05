package in.gov.pmegp.pv.core;

import static org.junit.Assert.*;
import java.math.BigDecimal;
import org.junit.Test;
import in.gov.pmegp.pv.core.util.FormatHelpers;

/** UV04-R03 — Indian grouping, not Western thousands. */
public class FormatHelpersTest {

    private static String w(String n) {
        return FormatHelpers.convertNumberToWords(new BigDecimal(n));
    }

    @Test public void uv04_r03_zero() { assertEquals("Zero", w("0")); }

    @Test public void uv04_r03_nullIsZero() {
        assertEquals("Zero", FormatHelpers.convertNumberToWords(null));
    }

    /** The boundary that separates Indian from Western grouping. */
    @Test public void uv04_r03_lakhNotHundredThousand() {
        assertEquals("Rupees One Lakh Only", w("100000"));
        assertEquals("Rupees Ninety Nine Thousand Nine Hundred Ninety Nine Only", w("99999"));
    }

    @Test public void uv04_r03_crore() {
        assertEquals("Rupees One Crore Only", w("10000000"));
        assertEquals("Rupees Ninety Nine Lakh Ninety Nine Thousand Nine Hundred Ninety Nine Only",
                w("9999999"));
    }

    /** Live capture value: TotalProjectCost = 900000. */
    @Test public void uv04_r03_liveCaptureValues() {
        assertEquals("Rupees Nine Lakh Only", w("900000"));
        assertEquals("Rupees Eight Lakh Only", w("800000"));
        assertEquals("Rupees Two Lakh Only", w("200000"));
        assertEquals("Rupees One Lakh Fifty Thousand Only", w("150000"));
    }

    @Test public void uv04_r03_teensAndTens() {
        assertEquals("Rupees Eleven Only", w("11"));
        assertEquals("Rupees Nineteen Only", w("19"));
        assertEquals("Rupees Twenty Only", w("20"));
        assertEquals("Rupees Twenty One Only", w("21"));
        assertEquals("Rupees One Hundred Only", w("100"));
    }

    /** The C# casts to long first, so paise are dropped. */
    @Test public void uv04_r03_fractionsAreDropped() {
        assertEquals("Rupees Twelve Thousand Only", w("12000.99"));
    }

    @Test public void uv04_r03_wagesExample() {
        assertEquals("Rupees Twelve Thousand Only", w("12000"));
        assertEquals("Rupees One Lakh Twenty Five Thousand Only", w("125000"));
    }

    /** Live capture: Gender = "M". */
    @Test public void genderDisplayMapping() {
        assertEquals("Male", FormatHelpers.genderDisplay("M"));
        assertEquals("Female", FormatHelpers.genderDisplay("F"));
        assertEquals("Transgender", FormatHelpers.genderDisplay("T"));
        assertEquals("Not Specified", FormatHelpers.genderDisplay(null));
        assertEquals("X", FormatHelpers.genderDisplay("X"));
    }
}
