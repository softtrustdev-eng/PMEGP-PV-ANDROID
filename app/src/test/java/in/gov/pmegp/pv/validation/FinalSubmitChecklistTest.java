package in.gov.pmegp.pv.validation;

import static org.junit.Assert.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.Test;
import in.gov.pmegp.pv.ui.unitverification.validation.EmploymentValidator;
import in.gov.pmegp.pv.ui.unitverification.validation.FinalSubmitChecklist;
import in.gov.pmegp.pv.ui.unitverification.validation.FinalSubmitChecklist.Record;

/** UV10-R03..R07 — the Final Submit gate. */
public class FinalSubmitChecklistTest {

    private Record ready() {
        Record r = new Record();
        r.unitName = "ACME UNIT"; r.updatedUnitAddress = "Plot 12/4";
        r.verificationStatus = "Working"; r.unitEstablishmentDate = "01/01/2023";
        r.udyamRegistrationNumber = "UDYAM-XX-00-0000000"; r.digiPin = "ABC-123-4567";
        r.hasCoordinates = true;
        r.annualProduction = new BigDecimal("500000");
        r.annualSales = new BigDecimal("450000");
        r.productDetails = "ACT123";
        EmploymentValidator.Input e = new EmploymentValidator.Input();
        e.male = 3; e.general = 3; e.fullTime = 3; e.wages = new BigDecimal("12000");
        r.employment = e;
        r.hasAnyDocument = true;
        r.verStatus = "Completed"; r.enumeratorRemark = "Verified on site.";
        r.applicantBankVerDone = Boolean.TRUE;
        return r;
    }

    @Test public void uv10_r03_aCompleteRecordPasses() {
        assertTrue(FinalSubmitChecklist.missing(ready()).isEmpty());
        assertTrue(FinalSubmitChecklist.canGenerateOtp(ready()));
    }

    @Test public void uv10_r04_messagesNameTheirStage() {
        Record r = ready(); r.digiPin = null;
        assertEquals(List.of("Digi PIN (Stage 2)"), FinalSubmitChecklist.missing(r));
    }

    /** UV10-R05 — testing "is non-zero" instead of ">= 1" let 0.0001 through this gate. */
    @Test public void uv10_r05_pointZeroZeroZeroOneIsStillMissing() {
        Record r = ready(); r.annualProduction = new BigDecimal("0.0001");
        assertTrue(FinalSubmitChecklist.missing(r).contains("Annual Production-Value (Stage 6)"));
    }

    @Test public void uv10_r05_exactlyOnePasses() {
        Record r = ready(); r.annualProduction = BigDecimal.ONE;
        assertTrue(FinalSubmitChecklist.missing(r).isEmpty());
    }

    /** UV10-R06 — mirrors Stage 7's own rule, not a stored total. */
    @Test public void uv10_r06_employmentMismatchIsCaught() {
        Record r = ready(); r.employment.general = 0;      // category != gender
        assertTrue(FinalSubmitChecklist.missing(r).contains("Employment Detail (Stage 7)"));
    }

    /**
     * UV07-R09 + UV10-R06 — the whole reason the blank-to-"0" guard exists.
     * A stage where the officer entered only gender counts, with the other groups
     * normalised to 0, must still be reported as incomplete by BOTH validators.
     */
    @Test public void uv07_r09_bothValidatorsAgreeOnNormalisedCounts() {
        Record r = ready();
        r.employment.general = 0; r.employment.fullTime = 0;   // what "0" normalisation gives
        assertFalse(EmploymentValidator.validateLive(r.employment).isValid());
        assertTrue(FinalSubmitChecklist.missing(r).contains("Employment Detail (Stage 7)"));
    }

    @Test public void uv10_r03_noDocumentBlocks() {
        Record r = ready(); r.hasAnyDocument = false;
        assertTrue(FinalSubmitChecklist.missing(r).contains("Document Upload (Stage 8)"));
    }

    /** UV09-R05 — Completed AND a remark. */
    @Test public void uv09_r05_pendingStatusBlocks() {
        Record r = ready(); r.verStatus = "Pending";
        assertTrue(FinalSubmitChecklist.missing(r).contains("Verification Details (Stage 9)"));
    }

    @Test public void uv09_r05_blankRemarkBlocks() {
        Record r = ready(); r.enumeratorRemark = "   ";
        assertTrue(FinalSubmitChecklist.missing(r).contains("Verification Details (Stage 9)"));
    }

    /** F-014 — stages 3, 4 and 5 are deliberately NOT checked. */
    @Test public void f014_edpProjectAndFinanceAreNotChecked() {
        assertTrue(FinalSubmitChecklist.missing(ready()).isEmpty());
    }

    /** UV10-R03 lists 14 requirements: Bank Verification, 7 from stage 2, 3 from stage 6,
     *  and one each from stages 7, 8 and 9. */
    @Test public void uv10_r03_anEmptyRecordListsAllFourteen() {
        assertEquals(14, FinalSubmitChecklist.missing(new Record()).size());
    }

    /**
     * UV10-R03b / F-016 — found by device verification, not by reading the source.
     * A pending case showed "Complete the following before generating OTP: Bank Verification"
     * with the button disabled, while the first spec draft had no such rule.
     */
    @Test public void uv10_r03b_bankVerificationIsPrerequisiteZero() {
        Record r = ready();
        r.applicantBankVerDone = null; r.phyModelBankVerDone = null;
        assertEquals(List.of("Bank Verification"), FinalSubmitChecklist.missing(r));
        assertFalse(FinalSubmitChecklist.canGenerateOtp(r));
    }

    /** Either flag satisfies it — they live on different objects. */
    @Test public void uv10_r03b_eitherFlagSatisfiesIt() {
        Record a = ready(); a.applicantBankVerDone = Boolean.TRUE; a.phyModelBankVerDone = null;
        assertTrue(FinalSubmitChecklist.missing(a).isEmpty());
        Record b = ready(); b.applicantBankVerDone = null; b.phyModelBankVerDone = Boolean.TRUE;
        assertTrue(FinalSubmitChecklist.missing(b).isEmpty());
        Record c = ready(); c.applicantBankVerDone = Boolean.FALSE; c.phyModelBankVerDone = Boolean.FALSE;
        assertFalse(FinalSubmitChecklist.missing(c).isEmpty());
    }

    /** UV10-R03b — it is listed FIRST, with no stage suffix. */
    @Test public void uv10_r03b_listedFirstWithoutAStageSuffix() {
        List<String> m = FinalSubmitChecklist.missing(new Record());
        assertEquals("Bank Verification", m.get(0));
        assertFalse(m.get(0).contains("Stage"));
    }
}
