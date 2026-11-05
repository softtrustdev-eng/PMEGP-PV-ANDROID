package in.gov.pmegp.pv.ui.unitverification.validation;

import java.math.BigDecimal;

/**
 * Stage 7 — Employment Detail.
 *
 * <p>Spec: docs/port-spec/unit-verification/UV-07-employment-detail.md
 *
 * <p>A pure function: values in, result out. No view access, so every rule is testable
 * without a device (UV00 §6).
 */
public final class EmploymentValidator {

    /** The live control values this stage reads. UV07-R05: live text, never saved state. */
    public static final class Input {
        public int male, female, transgender;
        public int general, sc, st, obc, minority;
        public int fullTime, partTime, seasonal;
        public BigDecimal wages;

        public int genderTotal()       { return male + female + transgender; }
        public int categoryTotal()     { return general + sc + st + obc + minority; }
        public int employmentTypeTotal(){ return fullTime + partTime + seasonal; }
    }

    private EmploymentValidator() { }

    /** UV07-R01 / UV07-R02: Total is the gender sum; empty when that sum is 0. */
    public static String totalDisplay(Input in) {
        int t = in.genderTotal();
        return t > 0 ? String.valueOf(t) : "";
    }

    /** Live validation — drives SAVE &amp; NEXT and the inline errors (UV07-R06..R11). */
    public static ValidationResult validateLive(Input in) {
        ValidationResult r = new ValidationResult();
        int gender = in.genderTotal();

        // UV07-R06 — the message sits on the GENDER group, not under the read-only Total,
        // where the officer could not act on it.
        if (gender <= 0) {
            r.group("gender",
                "Enter employees in this section — Total Number Of Employees cannot be 0.");
        } else {
            // UV07-R07 / UV07-R08 — only checked once a gender total exists.
            int cat = in.categoryTotal();
            if (cat != gender) {
                r.group("socialcategory", String.format(
                    "Sum of Social Category must be equal to Total Number Of Employees (%d). Currently %d.",
                    gender, cat));
            }
            int type = in.employmentTypeTotal();
            if (type != gender) {
                r.group("employmenttype", String.format(
                    "Sum of Employment Type must be equal to Total Number Of Employees (%d). Currently %d.",
                    gender, type));
            }
        }

        // UV07-R10 — the bound is 1, so 0.5 is rejected although the message says
        // "greater than 0". Rule and wording disagree; the rule is carried as-is and the
        // wording is raised separately (UV07-R10 note, Q1).
        if (in.wages == null || in.wages.compareTo(BigDecimal.ONE) < 0) {
            r.field("averagewagespaidperemployeepermonth",
                "Average wages paid per employee per month amount must be greater than 0");
        }
        return r;
    }

    /**
     * The navigation guard (UV07-R18..R20) — called when the officer tries to LEAVE.
     *
     * <p>UV07-R19: an untouched stage passes. If all three totals are 0 and wages is blank
     * the officer never opened this stage, and blocking would trap them on a stage they
     * never visited. Stage 10's prerequisite check catches the genuinely-unfilled case,
     * because it reads the saved record instead.
     */
    public static ValidationResult validateForLeaving(Input in) {
        ValidationResult r = new ValidationResult();
        boolean untouched = in.genderTotal() == 0
                         && in.categoryTotal() == 0
                         && in.employmentTypeTotal() == 0
                         && (in.wages == null || in.wages.signum() == 0);
        if (untouched) return r;

        int gender = in.genderTotal();
        if (gender <= 0) {
            r.dialog("Gender: enter employees — Total Number Of Employees cannot be 0.");
            r.group("gender", "Enter employees in this section — Total Number Of Employees cannot be 0.");
        }
        if (gender > 0 && in.categoryTotal() != gender) {
            r.dialog(String.format(
                "Social Category: sum must be equal to Total Number Of Employees (%d). Currently %d.",
                gender, in.categoryTotal()));
            r.group("socialcategory", "");
        }
        if (gender > 0 && in.employmentTypeTotal() != gender) {
            r.dialog(String.format(
                "Employment Type: sum must be equal to Total Number Of Employees (%d). Currently %d.",
                gender, in.employmentTypeTotal()));
            r.group("employmenttype", "");
        }
        if (in.wages == null || in.wages.compareTo(BigDecimal.ONE) < 0) {
            r.dialog("Average Wages: amount paid per employee per month must be greater than 0.");
            r.field("averagewagespaidperemployeepermonth", "");
        }
        return r;
    }

    /**
     * UV07-R09 — a blank count becomes "0" when the field loses focus.
     *
     * <p>THE RULE MOST LIKELY TO BE DROPPED IN THE PORT. A blank count is ambiguous: this
     * stage reads the live control (blank = 0) while Stage 10 reads the saved-value
     * accessor, which treats empty as "not set" and falls back to the stale API figure.
     * The two then disagree and Stage 10 reports "Stage 7 details are not filled" on a
     * stage the officer just completed.
     */
    public static String normaliseCountOnFocusLoss(String text) {
        return (text == null || text.trim().isEmpty()) ? "0" : text;
    }
}
