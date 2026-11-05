package in.gov.pmegp.pv.ui.unitverification.validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Stage 10 — the Final Submit prerequisite gate.
 *
 * <p>Spec: docs/port-spec/unit-verification/UV-10-summary-otp.md (UV10-R03..R07)
 *
 * <p>Stages 1, 3, 4 and 5 are deliberately NOT checked — EDP, Project and Finance can be
 * blank through Final Submit (F-014, confirm intended).
 */
public final class FinalSubmitChecklist {

    public static final class Record {
        /**
         * UV10-R03b — prerequisite ZERO, checked before any stage.
         * Satisfied when EITHER flag is true; they live on different objects and a single
         * field will be wrong for half the records.
         */
        public Boolean applicantBankVerDone;
        public Boolean phyModelBankVerDone;
        // Stage 2
        public String unitName, updatedUnitAddress, verificationStatus,
                      unitEstablishmentDate, udyamRegistrationNumber, digiPin;
        public boolean hasCoordinates;
        // Stage 6
        public BigDecimal annualProduction, annualSales;
        public String productDetails;
        // Stage 7
        public EmploymentValidator.Input employment;
        // Stage 8
        public boolean hasAnyDocument;
        // Stage 9
        public String verStatus, enumeratorRemark;
    }

    private FinalSubmitChecklist() { }

    /** @return the missing items, each naming its stage (UV10-R04). Empty means ready. */
    public static List<String> missing(Record r) {
        List<String> m = new ArrayList<>();

        // UV10-R03b — Bank Verification first. Found only by device verification (F-016):
        // a port built from the first spec draft would have let an officer generate an OTP
        // on a case whose bank verification was not done. No stage suffix — it is not a
        // stage of this form.
        if (!Boolean.TRUE.equals(r.applicantBankVerDone)
                && !Boolean.TRUE.equals(r.phyModelBankVerDone)) {
            m.add("Bank Verification");
        }

        req(m, r.unitName,                "Unit Name (Stage 2)");
        req(m, r.updatedUnitAddress,      "Updated Unit Address (Stage 2)");
        req(m, r.verificationStatus,      "Verification Status (Stage 2)");
        req(m, r.unitEstablishmentDate,   "Unit Establishment Date (Stage 2)");
        req(m, r.udyamRegistrationNumber, "Udyam Registration Number (Stage 2)");
        if (!r.hasCoordinates) m.add("Location Co-ordinates (Stage 2)");
        req(m, r.digiPin,                 "Digi PIN (Stage 2)");

        // UV10-R05 — the bound is 1, matching Stage 6's own rule. Testing "is non-zero"
        // instead let 0.0001 pass this gate although Stage 6 rejects it.
        if (!ProductSalesValidator.atLeastMinimum(r.annualProduction))
            m.add("Annual Production-Value (Stage 6)");
        if (!ProductSalesValidator.atLeastMinimum(r.annualSales))
            m.add("Annual Sales-Value (Stage 6)");
        req(m, r.productDetails, "Product Details (Stage 6)");

        // UV10-R06 — mirror Stage 7's own cross-field rule, not a stored total.
        if (r.employment == null || !EmploymentValidator.validateLive(r.employment).isValid())
            m.add("Employment Detail (Stage 7)");

        // UV08-R06 — the same source Stage 8's own guard uses, so they cannot disagree.
        if (!r.hasAnyDocument) m.add("Document Upload (Stage 8)");

        // UV09-R05 — Completed AND a remark. Either missing blocks.
        if (!"Completed".equals(r.verStatus)
                || r.enumeratorRemark == null || r.enumeratorRemark.trim().isEmpty())
            m.add("Verification Details (Stage 9)");

        return m;
    }

    /** UV10-R18 — Generate OTP is enabled only once the whole checklist passes. */
    public static boolean canGenerateOtp(Record r) { return missing(r).isEmpty(); }

    private static void req(List<String> out, String value, String label) {
        if (value == null || value.trim().isEmpty()) out.add(label);
    }
}
