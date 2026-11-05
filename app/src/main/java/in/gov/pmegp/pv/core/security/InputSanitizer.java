package in.gov.pmegp.pv.core.security;

/**
 * Removes characters that only ever appear in injection payloads, plus emoji and
 * invisible formatting, from the app's free-text fields.
 *
 * <p>Spec: docs/port-spec/cross-cutting/C03-input-sanitisation.md.
 * Verified against 17 assertions ported from the .NET implementation.
 *
 * <p><b>Deliberately narrow.</b> An Indian unit name or postal address legitimately
 * contains &amp; / - . ' ( ) # and digits — "Shah &amp; Sons", "Plot 12/4", "D'Souza" —
 * so blocking that punctuation would stop officers entering real data, which is a
 * worse outcome than the risk it removes (C03-R08). Five of the 17 tests exist purely
 * to prove this filter does not over-block. A port that passes only the blocking tests
 * has broken data entry for every officer in the field.
 *
 * <p><b>This is defence in depth, not the fix.</b> The app is not the trust boundary:
 * the API is reachable without it. Output encoding wherever this text is later
 * rendered, and server-side validation, remain the controls that count.
 */
public final class InputSanitizer {

    //  <  >   open and close an HTML tag, so removing them makes <script> unenterable.
    //  "      closes an HTML attribute.
    //  \      escapes in most injection grammars and appears in no Indian address.
    private static final String BLOCKED = "<>\"\\";

    private InputSanitizer() { }

    public static String clean(String value) {
        return clean(value, false);
    }

    /**
     * @param allowNewLines true for multi-line fields, where Enter is a legitimate
     *                      keystroke. Single-line fields drop newlines, which
     *                      otherwise only arrive by paste (C03-R10).
     */
    public static String clean(String value, boolean allowNewLines) {
        // C03-R09: null and empty pass through so callers need no guard.
        if (value == null || value.isEmpty()) return value;

        StringBuilder sb = new StringBuilder(value.length());

        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);

            // C03-R01
            if (BLOCKED.indexOf(c) >= 0) continue;

            // C03-R02: control characters are invisible, carry no meaning in these
            // fields and only ever arrive by paste. Tab is kept — harmless, and some
            // pasted addresses contain one.
            if (Character.isISOControl(c) && c != '\t'
                    && !(allowNewLines && (c == '\n' || c == '\r'))) continue;

            // C03-R03: every emoji outside the BMP is a surrogate pair. Both halves
            // are surrogates, so both are dropped and no orphan remains.
            if (Character.isSurrogate(c)) continue;

            // C03-R04: variation selectors are category Mn — the SAME category as
            // every Devanagari matra — so they cannot be filtered by category without
            // mangling Hindi ("श्री" would become "शरी"). Drop the block by range.
            if (c >= '︀' && c <= '️') continue;

            switch (Character.getType(c)) {
                // C03-R05: BMP pictographs — ☺ ✈ ❤ ★ and the dingbats. Indian-script
                // letters are OTHER_LETTER and ₹ is CURRENCY_SYMBOL, so neither is hit.
                case Character.OTHER_SYMBOL:
                // C03-R06: zero-width joiner, variation glue, bidi overrides.
                case Character.FORMAT:
                // C03-R07: keycap combiner that turns 1 into 1️⃣.
                case Character.ENCLOSING_MARK:
                    continue;
                default:
                    break;
            }

            sb.append(c);
        }

        return sb.toString();
    }
}
