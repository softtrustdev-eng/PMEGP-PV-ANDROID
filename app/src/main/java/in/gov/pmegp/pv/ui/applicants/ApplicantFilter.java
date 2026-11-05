package in.gov.pmegp.pv.ui.applicants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Spec: docs/port-spec/screens/S04-applicant-list-pv.md (S04-R01, S04-R11)
 */
public final class ApplicantFilter {

    public interface Row {
        String name();
        String id();
    }

    private ApplicantFilter() { }

    /** S04-R01 — matches name OR id, case-insensitive substring. Blank shows everything. */
    public static <T extends Row> List<T> filter(List<T> all, String query) {
        if (query == null || query.trim().isEmpty()) return new ArrayList<>(all);
        String q = query.toLowerCase(Locale.ROOT);
        List<T> out = new ArrayList<>();
        for (T r : all) {
            String n = r.name() == null ? "" : r.name().toLowerCase(Locale.ROOT);
            String i = r.id()   == null ? "" : r.id().toLowerCase(Locale.ROOT);
            if (n.contains(q) || i.contains(q)) out.add(r);
        }
        return out;
    }

    /**
     * S04-R11 — join the address parts, skipping any part already present
     * (case-insensitive). The record repeats village/mandal/district, producing
     * "BADANGI VILLAGE BADANGI MANDAL, Vizianagaram" style duplication.
     */
    public static String joinAddress(String... parts) {
        List<String> kept = new ArrayList<>();
        for (String p : parts) {
            if (p == null || p.trim().isEmpty()) continue;
            String v = p.trim();
            boolean dupe = false;
            for (String k : kept) if (k.equalsIgnoreCase(v)) { dupe = true; break; }
            if (!dupe) kept.add(v);
        }
        return String.join(", ", kept);
    }
}
