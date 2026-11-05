package in.gov.pmegp.pv.model;

import static org.junit.Assert.*;
import org.junit.Test;
import in.gov.pmegp.pv.data.model.CaseStatus;

/** C09 — the two-flag rule and the ApplicantDetails defect path. */
public class CaseStatusTest {

    @Test public void c09_r03_pendingCaseIsEditable() {
        CaseStatus s = CaseStatus.fromNavigation(CaseStatus.LIST_PENDING, "Pending");
        assertTrue(s.fieldEditable(false, false));
        assertTrue(s.savesOnAdvance());
    }

    @Test public void c09_r03_completedCaseIsNotEditable() {
        CaseStatus s = CaseStatus.fromNavigation(CaseStatus.LIST_PENDING, "Completed");
        assertFalse(s.fieldEditable(false, false));
    }

    @Test public void c09_r03_nullBadgeDefaultsToNotEditable() {
        assertFalse(CaseStatus.fromNavigation(CaseStatus.LIST_PENDING, null)
                .fieldEditable(false, false));
    }

    @Test public void c09_r06_badgeIsTrimmedAndCaseInsensitive() {
        assertFalse(CaseStatus.fromNavigation(CaseStatus.LIST_PENDING, "  COMPLETED  ")
                .fieldEditable(false, false));
    }

    /** C09-R01 — forceEditable overrides a Completed case (Stage 2 address / est. date). */
    @Test public void c09_r01_forceEditableWorksOnACompletedCase() {
        CaseStatus s = CaseStatus.fromNavigation(CaseStatus.LIST_PENDING, "Completed");
        assertTrue(s.fieldEditable(true, false));
    }

    /** C09-R01 — but NOT on a concluded one. Collapsing the flags breaks exactly this. */
    @Test public void c09_r01_forceEditableDoesNotOverrideAConcludedCase() {
        CaseStatus s = CaseStatus.fromNavigation(CaseStatus.LIST_DEFUNCT, "Pending");
        assertFalse(s.fieldEditable(true, false));
        assertTrue(s.isConcluded());
    }

    @Test public void c09_r02_forceReadOnlyWinsOverEverything() {
        CaseStatus s = CaseStatus.fromNavigation(CaseStatus.LIST_PENDING, "Pending");
        assertFalse(s.fieldEditable(true, true));
    }

    @Test public void c09_r03_nonTraceableListIsConcluded() {
        assertTrue(CaseStatus.fromNavigation(CaseStatus.LIST_NT, "Pending").isConcluded());
    }

    /**
     * C09-R04 / S07-R03 — THE DEFECT PATH.
     *
     * ApplicantDetailsPage hardcodes the list status to "Pending" because it is handed
     * only an applicant id. Before the server status is applied the case looks editable;
     * applying VeriStatus must lock it.
     */
    @Test public void c09_r04_serverStatusOverridesTheApplicantDetailsPath() {
        CaseStatus viaApplicantDetails =
                CaseStatus.fromNavigation(CaseStatus.LIST_PENDING, "Pending");
        assertTrue("navigation alone looks editable — this is the defect",
                viaApplicantDetails.fieldEditable(false, false));

        CaseStatus corrected = viaApplicantDetails.withServerStatus(CaseStatus.DEFUNCT);
        assertFalse(corrected.fieldEditable(false, false));
        assertFalse(corrected.fieldEditable(true, false));   // even forceEditable
        assertTrue(corrected.isConcluded());
        assertFalse(corrected.savesOnAdvance());
    }

    @Test public void c09_r04_nonTraceableFromServerAlsoLocks() {
        assertTrue(CaseStatus.fromNavigation(CaseStatus.LIST_PENDING, "Pending")
                .withServerStatus(CaseStatus.NON_TRACEABLE).isConcluded());
    }

    @Test public void c09_r04_workingFromServerLeavesItAlone() {
        assertTrue(CaseStatus.fromNavigation(CaseStatus.LIST_PENDING, "Pending")
                .withServerStatus(CaseStatus.WORKING).fieldEditable(false, false));
    }
}
