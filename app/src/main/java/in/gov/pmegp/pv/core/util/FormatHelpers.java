package in.gov.pmegp.pv.core.util;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Spec: UV04-R03, UV06-R13, UV07-R03 — the amount-in-words label shown live under every
 * money field, and the gender display mapping.
 *
 * <p>Uses the INDIAN grouping — thousand, lakh, crore — not Western thousands. A
 * generic number-to-words library will render 100000 as "One Hundred Thousand", which is
 * wrong for every officer using this app.
 */
public final class FormatHelpers {

    private static final String[] UNITS = {"", "One", "Two", "Three", "Four", "Five",
            "Six", "Seven", "Eight", "Nine"};
    private static final String[] TEENS = {"Ten", "Eleven", "Twelve", "Thirteen", "Fourteen",
            "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"};
    private static final String[] TENS = {"", "", "Twenty", "Thirty", "Forty", "Fifty",
            "Sixty", "Seventy", "Eighty", "Ninety"};

    private FormatHelpers() { }

    private static String hundreds(int n) {
        StringBuilder r = new StringBuilder();
        if (n >= 100) { r.append(UNITS[n / 100]).append(" Hundred "); n %= 100; }
        if (n >= 20)      { r.append(TENS[n / 10]).append(' '); n %= 10; }
        else if (n >= 10) { r.append(TEENS[n - 10]).append(' '); n = 0; }
        if (n > 0) r.append(UNITS[n]).append(' ');
        return r.toString();
    }

    /** Fractional paise are DROPPED — the C# casts to long before converting. */
    public static String convertNumberToWords(BigDecimal number) {
        if (number == null || number.signum() == 0) return "Zero";

        long n = number.longValue();
        StringBuilder r = new StringBuilder();

        if (n >= 10_000_000L) { r.append(hundreds((int) (n / 10_000_000L))).append("Crore "); n %= 10_000_000L; }
        if (n >= 100_000L)    { r.append(hundreds((int) (n / 100_000L))).append("Lakh ");  n %= 100_000L; }
        if (n >= 1_000L)      { r.append(hundreds((int) (n / 1_000L))).append("Thousand "); n %= 1_000L; }
        if (n > 0)            { r.append(hundreds((int) n)); }

        return "Rupees " + r.toString().trim() + " Only";
    }

    /** Gender codes as the API returns them — see the live capture, {@code Gender: "M"}. */
    public static String genderDisplay(String gender) {
        if (gender == null) return "Not Specified";
        switch (gender.toUpperCase(Locale.ROOT)) {
            case "M": return "Male";
            case "F": return "Female";
            case "T": return "Transgender";
            default:  return gender;
        }
    }
}
