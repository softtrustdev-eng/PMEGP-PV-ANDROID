package in.gov.pmegp.pv.core;

import static org.junit.Assert.*;
import java.util.Date;
import org.junit.Test;
import in.gov.pmegp.pv.core.network.ApiDates;

/** C02-R19 / C02-R20 — values taken from the live UAT capture, 2026-11-05. */
public class ApiDatesTest {

    /** C02-R19 — the legacy form, as GetApplDataForPV returns it. */
    @Test public void c02_r19_parsesDotNetEpochMillis() {
        Date d = ApiDates.parse("/Date(941481000000)/");
        assertNotNull(d);
        assertEquals(941481000000L, d.getTime());
    }

    /** C02-R19 — the ISO form, as GetBankVerApplData returns THE SAME FIELD. */
    @Test public void c02_r19_parsesIso8601() {
        Date d = ApiDates.parse("1999-11-02T00:00:00");
        assertNotNull(d);
        assertEquals(941500800000L, d.getTime());
    }

    /** Both formats, same applicant, ~1 day apart in the live data. Both must parse. */
    @Test public void c02_r19_bothFormatsFromTheSameRecord() {
        assertNotNull(ApiDates.parse("/Date(941481000000)/"));
        assertNotNull(ApiDates.parse("1999-11-02T00:00:00"));
    }

    /** C02-R20 — DateTime.MinValue means null, not year 1. */
    @Test public void c02_r20_minValueIsNull() {
        assertNull(ApiDates.parse("/Date(-62135596800000)/"));
    }

    @Test public void c02_r20_otherLiveValuesParse() {
        assertNotNull(ApiDates.parse("/Date(1774242962000)/"));   // VerDate
        assertNotNull(ApiDates.parse("/Date(1643049000000)/"));   // SancDate
        assertNotNull(ApiDates.parse("/Date(1640889000000)/"));   // TrDateTo
        assertNotNull(ApiDates.parse("2021-10-10T14:32:24"));     // FinalSubDate
    }

    @Test public void c02_r19_nullAndBlankAndGarbage() {
        assertNull(ApiDates.parse(null));
        assertNull(ApiDates.parse(""));
        assertNull(ApiDates.parse("   "));
        assertNull(ApiDates.parse("not a date"));
        assertNull(ApiDates.parse("/Date(abc)/"));
    }

    /** Milliseconds, not seconds — a seconds parse would land in 1970. */
    @Test public void c02_r19_millisNotSeconds() {
        Date d = ApiDates.parse("/Date(941481000000)/");
        assertTrue("must be 1999, not 1970", d.getTime() > 900_000_000_000L);
    }
}
