package in.gov.pmegp.pv.core.network;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the two date formats this API returns.
 *
 * <p>Spec: docs/port-spec/cross-cutting/C02-api-contracts.md (C02-R19, C02-R20).
 *
 * <p><b>The same field, same applicant, same server, arrives in two formats.</b>
 * Verified against live UAT data on 2026-11-05:
 * <ul>
 *   <li>{@code GetApplDataForPV} / {@code GetSavedPVData} → {@code "/Date(941481000000)/"}
 *   <li>{@code GetBankVerApplData} → {@code "1999-11-02T00:00:00"}
 * </ul>
 *
 * A single-format adapter silently nulls every date from the other endpoint. There is no
 * way to discover this from the C#, which deserialises into DateTime and lets the
 * framework cope.
 */
public final class ApiDates {

    /** C02-R19: the value is MILLISECONDS. Seconds puts every date in 1970. */
    private static final Pattern DOT_NET =
            Pattern.compile("^/Date\\((-?\\d+)([+-]\\d{4})?\\)/$");

    /**
     * C02-R20: .NET DateTime.MinValue (0001-01-01) as epoch millis. It appears in live
     * data — PhysicalVerificationCreatedOn and DateofBirth both carry it — and means
     * "not set". Rendering it literally shows the officer 01-01-0001.
     */
    public static final long DOTNET_MIN_VALUE_MS = -62135596800000L;

    private ApiDates() { }

    /** @return the instant, or null for null/blank/unparseable/MinValue. */
    public static Date parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        String s = raw.trim();

        Matcher m = DOT_NET.matcher(s);
        if (m.matches()) {
            long ms = Long.parseLong(m.group(1));
            return ms == DOTNET_MIN_VALUE_MS ? null : new Date(ms);
        }

        // ISO-8601 without a zone, as GetBankVerApplData returns it.
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        f.setLenient(false);
        Date d = f.parse(s, new ParsePosition(0));
        return (d != null && d.getTime() == DOTNET_MIN_VALUE_MS) ? null : d;
    }
}
