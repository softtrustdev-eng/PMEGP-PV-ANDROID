package in.gov.pmegp.pv.validation;

import static org.junit.Assert.*;
import java.math.BigDecimal;
import org.junit.Test;
import in.gov.pmegp.pv.ui.unitverification.validation.ProductSalesValidator;
import in.gov.pmegp.pv.ui.unitverification.validation.ProductSalesValidator.Input;

/** UV06 — the 0.0001 defect and its boundary. */
public class ProductSalesValidatorTest {

    /** UV06-R01 — the reported defect: 0.0001 was accepted. */
    @Test public void uv06_r01_pointZeroZeroZeroOneIsRejected() {
        assertNotNull(ProductSalesValidator.liveMinimumError("0.0001"));
    }

    @Test public void uv06_r01_zeroPointFiveIsAlsoRejected() {
        assertNotNull(ProductSalesValidator.liveMinimumError("0.5"));
    }

    @Test public void uv06_r01_exactlyOneIsAccepted() {
        assertNull(ProductSalesValidator.liveMinimumError("1"));
    }

    /** UV06-R04 — blank raises nothing while the officer is still typing. */
    @Test public void uv06_r04_blankDoesNotFlag() {
        assertNull(ProductSalesValidator.liveMinimumError(""));
        assertNull(ProductSalesValidator.liveMinimumError("   "));
        assertNull(ProductSalesValidator.liveMinimumError(null));
    }

    @Test public void uv06_r01_messageWording() {
        assertEquals("Amount should be at least 1",
                ProductSalesValidator.liveMinimumError("0.9"));
    }

    /** C05-R15 — exact decimal, not double. 0.9999999999 must not reach 1. */
    @Test public void c05_r15_boundaryIsExact() {
        assertFalse(ProductSalesValidator.atLeastMinimum(new BigDecimal("0.9999999999")));
        assertTrue(ProductSalesValidator.atLeastMinimum(new BigDecimal("1.0000000001")));
        assertTrue(ProductSalesValidator.atLeastMinimum(new BigDecimal("1.00")));
    }

    @Test public void uv06_unparseableIsTreatedAsMissing() {
        assertNull(ProductSalesValidator.parse("abc"));
        assertFalse(ProductSalesValidator.atLeastMinimum(ProductSalesValidator.parse("abc")));
    }

    @Test public void uv06_r05_mainProductIsRequired() {
        Input i = new Input();
        i.annualProduction = new BigDecimal("500000");
        i.annualSales = new BigDecimal("450000");
        assertFalse(ProductSalesValidator.validate(i).isValid());
        i.mainProductCode = "ACT123";
        assertTrue(ProductSalesValidator.validate(i).isValid());
    }

    /** C05-R20 — the first invalid field is the one to scroll to. */
    @Test public void c05_r20_firstInvalidFieldIsProduction() {
        Input i = new Input();
        assertEquals("annualproductionvalue",
                ProductSalesValidator.validate(i).firstInvalidField());
    }
}
