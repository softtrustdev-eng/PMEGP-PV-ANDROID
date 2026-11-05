package in.gov.pmegp.pv.validation;

import static org.junit.Assert.*;
import java.math.BigDecimal;
import org.junit.Test;
import in.gov.pmegp.pv.ui.unitverification.validation.EmploymentValidator;
import in.gov.pmegp.pv.ui.unitverification.validation.EmploymentValidator.Input;
import in.gov.pmegp.pv.ui.unitverification.validation.ValidationResult;

/** UV07 — every rule, by id. */
public class EmploymentValidatorTest {

    private Input filled() {
        Input i = new Input();
        i.male = 2; i.female = 3;                 // gender  = 5
        i.general = 5;                            // category= 5
        i.fullTime = 5;                           // type    = 5
        i.wages = new BigDecimal("12000");
        return i;
    }

    @Test public void uv07_r01_totalIsTheGenderSum() {
        assertEquals("5", EmploymentValidator.totalDisplay(filled()));
    }

    @Test public void uv07_r02_totalIsEmptyWhenGenderSumIsZero() {
        assertEquals("", EmploymentValidator.totalDisplay(new Input()));
    }

    @Test public void uv07_r06_genderTotalZeroReportsOnTheGenderGroup() {
        Input i = new Input(); i.wages = new BigDecimal("1");
        ValidationResult r = EmploymentValidator.validateLive(i);
        assertTrue(r.groupErrors().containsKey("gender"));
        assertEquals("Enter employees in this section — Total Number Of Employees cannot be 0.",
                r.groupErrors().get("gender"));
    }

    @Test public void uv07_r07_categoryMismatchNamesBothNumbers() {
        Input i = filled(); i.general = 0;
        ValidationResult r = EmploymentValidator.validateLive(i);
        assertEquals("Sum of Social Category must be equal to Total Number Of Employees (5). Currently 0.",
                r.groupErrors().get("socialcategory"));
    }

    @Test public void uv07_r08_employmentTypeMismatchNamesBothNumbers() {
        Input i = filled(); i.fullTime = 0;
        ValidationResult r = EmploymentValidator.validateLive(i);
        assertEquals("Sum of Employment Type must be equal to Total Number Of Employees (5). Currently 0.",
                r.groupErrors().get("employmenttype"));
    }

    /** UV07-R09 — the rule most likely to be dropped in the port. */
    @Test public void uv07_r09_blankCountBecomesZeroOnFocusLoss() {
        assertEquals("0", EmploymentValidator.normaliseCountOnFocusLoss(""));
        assertEquals("0", EmploymentValidator.normaliseCountOnFocusLoss("   "));
        assertEquals("0", EmploymentValidator.normaliseCountOnFocusLoss(null));
        assertEquals("3", EmploymentValidator.normaliseCountOnFocusLoss("3"));
    }

    /** UV07-R10 — the bound is 1, so 0.5 is rejected despite being greater than zero. */
    @Test public void uv07_r10_wagesBelowOneIsRejected() {
        Input i = filled(); i.wages = new BigDecimal("0.5");
        assertFalse(EmploymentValidator.validateLive(i).isValid());
        i.wages = BigDecimal.ONE;
        assertTrue(EmploymentValidator.validateLive(i).isValid());
    }

    @Test public void uv07_r11_groupRulesDoNotLeakIntoFieldErrors() {
        Input i = filled(); i.general = 0;
        ValidationResult r = EmploymentValidator.validateLive(i);
        assertTrue(r.groupErrors().containsKey("socialcategory"));
        assertFalse(r.fieldErrors().containsKey("socialcategory"));
    }

    /** UV07-R19 — an untouched stage must not trap the officer. */
    @Test public void uv07_r19_untouchedStagePassesTheLeaveGuard() {
        assertTrue(EmploymentValidator.validateForLeaving(new Input()).isValid());
    }

    @Test public void uv07_r19_touchedButInvalidStageDoesNotPass() {
        Input i = new Input(); i.male = 5;
        assertFalse(EmploymentValidator.validateForLeaving(i).isValid());
    }

    /** UV07-R20 — section-named lines, blank line between them. */
    @Test public void uv07_r20_dialogNamesEachSection() {
        Input i = new Input(); i.male = 3; i.wages = new BigDecimal("100");
        String msg = EmploymentValidator.validateForLeaving(i).dialogMessage();
        assertEquals(
            "Social Category: sum must be equal to Total Number Of Employees (3). Currently 0.\n\n"
          + "Employment Type: sum must be equal to Total Number Of Employees (3). Currently 0.",
            msg);
    }

    @Test public void uv07_validStagePasses() {
        assertTrue(EmploymentValidator.validateLive(filled()).isValid());
        assertTrue(EmploymentValidator.validateForLeaving(filled()).isValid());
    }
}
