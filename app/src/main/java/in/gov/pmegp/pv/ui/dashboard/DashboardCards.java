package in.gov.pmegp.pv.ui.dashboard;

import java.util.Locale;
import in.gov.pmegp.pv.data.model.CaseStatus;

/**
 * Spec: docs/port-spec/screens/S03-dashboard.md (S03-R01, S03-R02)
 *
 * <p>Maps a dashboard card to the applicant-list status key. The aliases are carried
 * verbatim: the server has changed these identifiers at least once, and both singular
 * and plural forms are accepted.
 */
public final class DashboardCards {

    private DashboardCards() { }

    /**
     * S03-R01 — the fallback is PendingApplications: an unrecognised card silently opens
     * the pending list rather than failing.
     *
     * <p>S03-R02: the returned strings flow into the applicant list and decide whether a
     * case is concluded (C09-R03). A typo here unlocks editing on a Defunct case.
     */
    public static String listStatusFor(String cardId) {
        if (cardId == null) return CaseStatus.LIST_PENDING;
        switch (cardId.toLowerCase(Locale.ROOT)) {
            case "reports_pending":                           return CaseStatus.LIST_PENDING;
            case "working_unit":  case "working_units":       return CaseStatus.LIST_WORKING;
            case "defunct_unit":  case "defunct_units":       return CaseStatus.LIST_DEFUNCT;
            case "non_traceable_unit": case "non_traceable_units": return CaseStatus.LIST_NT;
            default:                                          return CaseStatus.LIST_PENDING;
        }
    }
}
