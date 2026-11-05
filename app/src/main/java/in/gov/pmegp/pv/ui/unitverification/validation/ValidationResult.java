package in.gov.pmegp.pv.ui.unitverification.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The outcome of one stage's validation.
 *
 * <p>Carries two kinds of message, because the app renders them differently
 * (C05-R16 / C05-R17 / UV07-R11):
 * <ul>
 *   <li><b>field errors</b> — shown under the field, with its outline and caption reddened
 *   <li><b>group errors</b> — shown on the group heading; only the failing group reddens
 * </ul>
 * plus the dialog lines the navigation guard shows (UV07-R20).
 */
public final class ValidationResult {

    private final Map<String, String> fieldErrors = new LinkedHashMap<>();
    private final Map<String, String> groupErrors = new LinkedHashMap<>();
    private final List<String> dialogLines = new ArrayList<>();

    public void field(String key, String message) { fieldErrors.put(key, message); }
    public void group(String key, String message) { groupErrors.put(key, message); }
    public void dialog(String line)               { dialogLines.add(line); }

    public boolean isValid() { return fieldErrors.isEmpty() && groupErrors.isEmpty(); }

    public Map<String, String> fieldErrors() { return Collections.unmodifiableMap(fieldErrors); }
    public Map<String, String> groupErrors() { return Collections.unmodifiableMap(groupErrors); }

    /** UV07-R20: one line per failing rule, separated by a BLANK line. */
    public String dialogMessage() { return String.join("\n\n", dialogLines); }

    /** C05-R20: the first invalid field, to scroll to and focus. */
    public String firstInvalidField() {
        return fieldErrors.isEmpty() ? null : fieldErrors.keySet().iterator().next();
    }
}
