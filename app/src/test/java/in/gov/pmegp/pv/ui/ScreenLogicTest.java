package in.gov.pmegp.pv.ui;

import static org.junit.Assert.*;
import java.util.List;
import org.junit.Test;
import in.gov.pmegp.pv.data.model.CaseStatus;
import in.gov.pmegp.pv.ui.applicants.ApplicantFilter;
import in.gov.pmegp.pv.ui.dashboard.DashboardCards;

/** S03 + S04 screen logic. */
public class ScreenLogicTest {

    @Test public void s03_r01_cardMappingIncludingAliases() {
        assertEquals(CaseStatus.LIST_PENDING, DashboardCards.listStatusFor("reports_pending"));
        assertEquals(CaseStatus.LIST_WORKING, DashboardCards.listStatusFor("working_unit"));
        assertEquals(CaseStatus.LIST_WORKING, DashboardCards.listStatusFor("working_units"));
        assertEquals(CaseStatus.LIST_DEFUNCT, DashboardCards.listStatusFor("defunct_unit"));
        assertEquals(CaseStatus.LIST_DEFUNCT, DashboardCards.listStatusFor("defunct_units"));
        assertEquals(CaseStatus.LIST_NT, DashboardCards.listStatusFor("non_traceable_unit"));
        assertEquals(CaseStatus.LIST_NT, DashboardCards.listStatusFor("non_traceable_units"));
    }

    /** S03-R01 — unrecognised falls back to pending, it does not throw. */
    @Test public void s03_r01_unknownFallsBackToPending() {
        assertEquals(CaseStatus.LIST_PENDING, DashboardCards.listStatusFor("total_reports"));
        assertEquals(CaseStatus.LIST_PENDING, DashboardCards.listStatusFor(null));
        assertEquals(CaseStatus.LIST_PENDING, DashboardCards.listStatusFor(""));
    }

    /** S03-R02 — these strings decide whether a case is concluded. */
    @Test public void s03_r02_keysFeedTheConcludedCheck() {
        assertTrue(CaseStatus.fromNavigation(
                DashboardCards.listStatusFor("defunct_units"), "Pending").isConcluded());
        assertTrue(CaseStatus.fromNavigation(
                DashboardCards.listStatusFor("non_traceable_units"), "Pending").isConcluded());
        assertFalse(CaseStatus.fromNavigation(
                DashboardCards.listStatusFor("reports_pending"), "Pending").isConcluded());
    }

    private static ApplicantFilter.Row row(final String n, final String i) {
        return new ApplicantFilter.Row() {
            public String name() { return n; }
            public String id()   { return i; }
        };
    }

    private final List<ApplicantFilter.Row> rows = List.of(
            row("TALAMALA RAJYALAKSHMI", "KBAP227965-11359473"),
            row("Kakinada Durga Rao",    "DIAP232085-11472569"),
            row("CHINTALA MADHAVI",      "KBAP222108-11316362"));

    @Test public void s04_r01_matchesName() {
        assertEquals(1, ApplicantFilter.filter(rows, "durga").size());
    }

    @Test public void s04_r01_matchesIdToo() {
        assertEquals(1, ApplicantFilter.filter(rows, "11472569").size());
        assertEquals(2, ApplicantFilter.filter(rows, "KBAP22").size());
    }

    @Test public void s04_r01_caseInsensitive() {
        assertEquals(1, ApplicantFilter.filter(rows, "MADHAVI").size());
        assertEquals(1, ApplicantFilter.filter(rows, "madhavi").size());
        assertEquals(1, ApplicantFilter.filter(rows, "diap").size());
    }

    @Test public void s04_r01_blankShowsEverything() {
        assertEquals(3, ApplicantFilter.filter(rows, "").size());
        assertEquals(3, ApplicantFilter.filter(rows, "   ").size());
        assertEquals(3, ApplicantFilter.filter(rows, null).size());
    }

    /** S04-R11 — the real duplication seen on device. */
    @Test public void s04_r11_deduplicatesAddressParts() {
        assertEquals("BADANGI VILLAGE BADANGI MANDAL, Vizianagaram, 535578",
                ApplicantFilter.joinAddress(
                        "BADANGI VILLAGE BADANGI MANDAL", "Vizianagaram", "535578"));
        assertEquals("Bhavnagar, 364004",
                ApplicantFilter.joinAddress("Bhavnagar", "bhavnagar", "364004"));
        assertEquals("STATION ROAD,, JAJPUR ROAD, Jajapur, 755019",
                ApplicantFilter.joinAddress("STATION ROAD,", "JAJPUR ROAD", "Jajapur", "755019"));
    }

    @Test public void s04_r11_skipsNullAndBlank() {
        assertEquals("A, B", ApplicantFilter.joinAddress("A", null, "  ", "B"));
    }
}
