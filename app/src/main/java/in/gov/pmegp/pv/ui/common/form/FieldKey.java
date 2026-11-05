package in.gov.pmegp.pv.ui.common.form;

import java.util.Locale;

/**
 * Spec: docs/port-spec/cross-cutting/C05-form-field-behaviour.md (C05-R01)
 *
 * <p>In the MAUI app a field's identity is its CAPTION, normalised — strip '*', spaces,
 * ':' and '-', then trim. Save, validation, error marking and state restore all find the
 * control by that derived key, so renaming a caption silently breaks all four.
 *
 * <p>The Java app uses explicit ids instead. This class exists only to translate the
 * legacy keys, so the rule ids in the spec stay greppable and payload keys keep matching.
 * <b>Do not use it to derive keys from display text at runtime.</b>
 */
public final class FieldKey {

    private FieldKey() { }

    public static String of(String caption) {
        if (caption == null || caption.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(caption.length());
        for (int i = 0; i < caption.length(); i++) {
            char c = caption.charAt(i);
            if (c == '*' || c == ' ' || c == ':' || c == '-') continue;
            sb.append(c);
        }
        return sb.toString().trim();
    }

    /** The lowercase form used by the error-flag set (C05-R19's owned-key lists). */
    public static String errorKey(String caption) {
        return of(caption).toLowerCase(Locale.ROOT);
    }
}
