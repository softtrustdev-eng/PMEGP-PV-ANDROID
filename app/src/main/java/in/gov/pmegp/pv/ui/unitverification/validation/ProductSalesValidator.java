package in.gov.pmegp.pv.ui.unitverification.validation;

import java.math.BigDecimal;

/**
 * Stage 6 — Product &amp; Sales.
 * Spec: docs/port-spec/unit-verification/UV-06-product-and-sales.md
 */
public final class ProductSalesValidator {

    /** UV06-R01 / UV06-R02: the bound is 1, not 0 — 0.0001 was being accepted. */
    public static final BigDecimal MINIMUM = BigDecimal.ONE;

    public static final class Input {
        public BigDecimal annualProduction;
        public BigDecimal annualSales;
        public String mainProductCode;
    }

    private ProductSalesValidator() { }

    public static ValidationResult validate(Input in) {
        ValidationResult r = new ValidationResult();
        if (!atLeastMinimum(in.annualProduction)) {
            r.field("annualproductionvalue", minimumMessage());
        }
        if (!atLeastMinimum(in.annualSales)) {
            r.field("annualsalesvalue", minimumMessage());
        }
        if (in.mainProductCode == null || in.mainProductCode.trim().isEmpty()) {
            r.field("productdetailsmainproduct", "Product Details is required");
        }
        return r;
    }

    /**
     * UV06-R03 — checked ON TYPING, not on save.
     *
     * <p>C05-R13: this handler must be registered AFTER the clear-on-type handler so it
     * runs last and wins. If the order is reversed the clear wipes the error and 0.0001
     * looks valid again.
     *
     * <p>Returns null when the field should show no error.
     */
    public static String liveMinimumError(String typed) {
        // UV06-R04: blank raises nothing while the officer is still typing.
        if (typed == null || typed.trim().isEmpty()) return null;
        BigDecimal v = parse(typed);
        return atLeastMinimum(v) ? null : minimumMessage();
    }

    /** UV10-R05: Stage 10's prerequisite check uses the same bound as this stage. */
    public static boolean atLeastMinimum(BigDecimal v) {
        return v != null && v.compareTo(MINIMUM) >= 0;
    }

    private static String minimumMessage() {
        return "Amount should be at least " + MINIMUM.stripTrailingZeros().toPlainString();
    }

    /**
     * C05-R15: exact decimal parsing, never Double.parseDouble. A >= 1 boundary check on
     * a value like 0.9999999999 must not pass.
     */
    public static BigDecimal parse(String s) {
        if (s == null || s.trim().isEmpty()) return null;
        try { return new BigDecimal(s.trim()); } catch (NumberFormatException e) { return null; }
    }
}
