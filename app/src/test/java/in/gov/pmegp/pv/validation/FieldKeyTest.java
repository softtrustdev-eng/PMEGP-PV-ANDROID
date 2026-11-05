package in.gov.pmegp.pv.validation;

import static org.junit.Assert.*;
import org.junit.Test;
import in.gov.pmegp.pv.ui.common.form.FieldKey;

/** C05-R01 — the legacy key derivation, kept only for translation. */
public class FieldKeyTest {

    @Test public void c05_r01_stripsStarsSpacesColonsAndHyphens() {
        assertEquals("AnnualProductionValue", FieldKey.of("Annual Production-Value*"));
        assertEquals("TotalNumberOfEmployees", FieldKey.of("Total Number Of Employees*"));
        assertEquals("FullTimeEmployees", FieldKey.of("Full-Time Employees"));
        assertEquals("AverageWagespaidperemployeepermonth",
                FieldKey.of("Average Wages paid per employee per month*"));
    }

    @Test public void c05_r01_lowercaseErrorKeys() {
        assertEquals("annualproductionvalue", FieldKey.errorKey("Annual Production-Value*"));
        assertEquals("digipin", FieldKey.errorKey("Digi PIN*"));
        assertEquals("updatedunitaddress", FieldKey.errorKey("Updated Unit Address*"));
    }

    @Test public void c05_r01_emptyAndNull() {
        assertEquals("", FieldKey.of(null));
        assertEquals("", FieldKey.of(""));
    }

    /** The hazard C05-R01 warns about: two captions collapsing to one key. */
    @Test public void c05_r01_differentCaptionsCanCollide() {
        assertEquals(FieldKey.of("Unit-Name"), FieldKey.of("Unit Name"));
    }
}
