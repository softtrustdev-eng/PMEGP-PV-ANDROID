package in.gov.pmegp.pv.data.model;

import java.util.Locale;

/**
 * Whether a record may be edited.
 *
 * <p>Spec: docs/port-spec/cross-cutting/C09-case-status-and-readonly.md
 *
 * <p><b>Two flags, deliberately not one (C09-R01).</b> Several Stage 2 fields pass
 * forceEditable, which overrides {@code recordEditable} on a Completed case. A
 * concluded (Defunct / Non-Traceable) case must NOT be overridable that way.
 * Collapsing these into one boolean unlocks every field on a Defunct record — it is
 * the single most likely simplification a porting developer will make.
 */
public final class CaseStatus {

    /** C09-R05: VeriStatus codes. PD appears only on Stage 9 (F-011, unconfirmed). */
    public static final String WORKING        = "WR";
    public static final String DEFUNCT        = "DF";
    public static final String NON_TRACEABLE  = "NT";

    /** C09-R03: list status keys. Compared literally — see S03-R02. */
    public static final String LIST_PENDING   = "PendingApplications";
    public static final String LIST_WORKING   = "WorkingApplications";
    public static final String LIST_DEFUNCT   = "DefunctApplications";
    public static final String LIST_NT        = "NonTraceable";

    private final boolean concluded;
    private final boolean recordEditable;

    private CaseStatus(boolean concluded, boolean recordEditable) {
        this.concluded = concluded;
        this.recordEditable = recordEditable;
    }

    /**
     * C09-R03 — initial state, from navigation arguments only.
     *
     * <p>A null badge status defaults to "Completed", i.e. NOT editable. The default is
     * the safe one: unknown status means read-only.
     */
    public static CaseStatus fromNavigation(String listStatus, String badgeStatus) {
        boolean concluded = LIST_DEFUNCT.equals(listStatus) || LIST_NT.equals(listStatus);
        String badge = badgeStatus == null ? "Completed" : badgeStatus.trim();
        // C09-R06: Locale.ROOT — Turkish lowercases I to ı and breaks the comparison.
        boolean editable = !"completed".equals(badge.toLowerCase(Locale.ROOT)) && !concluded;
        return new CaseStatus(concluded, editable);
    }

    /**
     * C09-R04 — the server's VeriStatus is authoritative and overrides the navigation
     * arguments.
     *
     * <p>Whether a case is concluded is a property of the RECORD, not of the screen the
     * officer arrived from. ApplicantDetailsPage hardcodes the list status to "Pending"
     * (S07-R03) because it is handed only an applicant id — so without this, reaching a
     * Defunct case that way unlocked every field. The save is rejected server-side, but
     * the officer only finds out after typing the whole form.
     */
    public CaseStatus withServerStatus(String veriStatus) {
        if (DEFUNCT.equals(veriStatus) || NON_TRACEABLE.equals(veriStatus)) {
            return new CaseStatus(true, false);
        }
        return this;
    }

    public boolean isConcluded() { return concluded; }

    /** C09-R02 — the single rule every field builder must route through. */
    public boolean fieldEditable(boolean forceEditable, boolean forceReadOnly) {
        if (forceReadOnly) return false;
        if (concluded) return false;                 // outranks forceEditable
        return recordEditable || forceEditable;
    }

    /** C09-R08 — SAVE & NEXT vs NEXT. */
    public boolean savesOnAdvance() { return recordEditable && !concluded; }
}
