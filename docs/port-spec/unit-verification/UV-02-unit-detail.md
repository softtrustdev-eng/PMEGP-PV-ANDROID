# UV02 — Stage 2: Unit Detail

| | |
|---|---|
| **MAUI source** | builder `:2231-2971`, save `:8767-9053` |
| **Java target** | `ui/unitverification/stages/Stage02UnitDetailFragment` + `core/util/LocationProvider` |
| **Review state** | **R1 PASSED** |
| **Rules** | 20 (8 HIGH) |

> The most complex stage after 10: geolocation, a server-side DIGIPIN call, an embedded map,
> and the Defunct / Non-Traceable conclusion path.

---

## 1. Fields

| Label | Type | Editable | API field |
|---|---|---|---|
| Unit Name* | text, max **50** | **conditional** — see R01 | `UnitName` |
| Updated Unit Address* | multiline, max **300** | always (`forceEditable`) | — |
| Verification Status* | status section | if editable | `VeriStatus` |
| Unit Establishment Date* | date | always (`forceEditable`) | `UnitEstDate` |
| GST Registration Number | text | yes | `UnitGSTNo` |
| Udyam Registration Number | text | yes | — |
| Unit Location* / Sponsored By* / Agency Office* / Taluka* / District* / State* / Pincode* | text | read-only | from record |
| Industry Type* / Industry Activity* | text | read-only | from record |
| Longitude* / Latitude* | text | read-only, filled by GET LOCATION | `Longitude` / `Latitude` |
| Geo Tagging ID* | text | read-only | `GeoTagID` |
| Digi PIN* | text | read-only, filled by GENERATE DIGIPIN | `DigiPIN` |

---

## 2. Conditional editability

### UV02-R01 — Unit Name is editable only when the server has none ⚠️
**Rule.** `forceEditable: !unitNameHasValue`, `forceReadOnly: unitNameHasValue`. If the server
already holds a unit name it is locked; if blank, the officer may enter one (max 50).
**Source.** `:2253`
**Java.** Decide this at bind time from the loaded value, not from a static flag.
**Risk.** HIGH

### UV02-R02 — Address and establishment date override case editability
**Rule.** Both pass `forceEditable: true`, so they are editable even on a **Completed** case.
**Source.** `:2264`, `:2276`
**Why.** These are the two things an officer must be able to correct after completion.
**Critical.** `forceEditable` does **not** override a concluded case — see
[C09-R01](../cross-cutting/C09-case-status-and-readonly.md). Collapsing the two flags unlocks
these fields on a Defunct record.
**Risk.** HIGH

---

## 3. Location

### UV02-R03 — GET LOCATION fills Longitude and Latitude
**Rule.** Requests location permission, acquires a fix behind a blocking loader
(`Getting location...`), writes both fields, then **rebuilds the stage**.
**Source.** `:2324-2340`
**Risk.** HIGH

### UV02-R04 — The rebuild is why error re-application exists ⚠️
**Rule.** GET LOCATION and GENERATE DIGIPIN deliver values by rebuilding the stage. The new
controls are populated **without raising a text-change event**.
**Consequence.** Field errors must be re-applied after the rebuild, and stale ones pruned —
otherwise either the red vanishes from fields that are still empty, or it returns on fields
just filled.
**See.** [C05-R18 / C05-R19](../cross-cutting/C05-form-field-behaviour.md). Stage 2 owns:
`unitname`, `updatedunitaddress`, `unitestablishmentdate`, `udyamregistrationnumber`,
`longitude`, `latitude`, `geotaggingid`, `digipin`, `verificationstatus`.
**Java.** Binding from a ViewModel removes the rebuild entirely. **Reproduce the outcome, not
the mechanism.**
**Risk.** HIGH

### UV02-R05 — Location services off redirects to settings
**Rule.** If location services are disabled, the officer is sent to the system location
settings page rather than shown a dead error.
**Source.** `Util/LocationSettings.cs`
**Risk.** MED

### UV02-R06 — GENERATE DIGIPIN is gated on a location
**Rule.** The button is enabled only once a location exists — freshly acquired **or**
previously saved.
**Source.** `:2348-2352`
**Risk.** MED

### UV02-R07 — DIGIPIN is computed server-side
**Rule.** `POST {API_BASE}/DigiPinEncodeAsync` with the coordinates; the response carries
`{ digipin, message, status }`. The app does **not** compute it.
**See.** [C02-R12](../cross-cutting/C02-api-contracts.md)
**Java.** Do not look for a DIGIPIN algorithm to port. There isn't one.
**Risk.** HIGH

---

## 4. The map

### UV02-R08 — Embedded WebView map with an HTML overlay link ⚠️
**Rule.** The map is a `WebView` holding an HTML document with an embedded map and a styled
`#openInMaps` anchor. Height **380**.
**Source.** `:2768`
**Why an HTML anchor and not a native button.** A native button overlaid on the WebView
**renders but never receives taps** — the native WebView consumes every touch across its
surface. The link must live inside the page.
**Risk.** HIGH

### UV02-R09 — The link is a custom-scheme bridge
**Rule.** The anchor navigates to `pmegp://openmaps`. A navigation handler intercepts URLs with
that prefix, cancels the navigation, and launches the external maps app with the coordinates
and the unit name.

```java
// UV02-R09
webView.setWebViewClient(new WebViewClient() {
    @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
        if (!r.getUrl().toString().startsWith("pmegp://openmaps")) return false;
        openMapsApp(lat, lng, unitName);
        return true;                      // cancel the navigation
    }
});
```

**Why not the map provider's own link.** `target="_blank"` from a cross-origin iframe routes to
`onCreateWindow`, **not** `shouldOverrideUrlLoading`, and is dropped by default. The provider's
own "Open in Maps" link does nothing.
**Note.** The provider's dead link still sits top-left under ours. Only removable by dropping
the embed for a static map.
**Risk.** HIGH

### UV02-R10 — Package visibility is not the problem
**Rule.** The manifest's `<queries>` declares only `IMAGE_CAPTURE` and needs no maps entry.
**Why recorded.** Verified empirically — the applicant card's `geo:` icon launches Google Maps
successfully from the same app. A porting developer debugging R09 will suspect Android 11+
package visibility; it is already ruled out.
**Risk.** MED

---

## 5. Verification status & conclusion

### UV02-R11 — Status section with three values
**Rule.** `Working` (`WR`), `Defunct` (`DF`), `Non-Traceable` (`NT`). The previous status is
tracked so a change can be detected.
**Source.** `:2266-2275`
**Risk.** MED

### UV02-R12 — DF / NT is finalised straight from Stage 2 ⚠️
**Rule.** Selecting Defunct or Non-Traceable concludes the case **from this stage**, via
`InsertDFOrNT_PV` with a remarks value. The officer does not walk the remaining stages.
**Source.** `:2375-2395`
**Risk.** HIGH

### UV02-R13 — Conclusion payload
**Rule.** `{ ApplID, isUnitVerDone: true, VeriStatus, RemarkForDefunctNonTracable }`.
**Source.** `:8880-8890`
**Note.** `RemarkForDefunctNonTracable` — spelled exactly so, matching
[F-009](../README.md)'s `"Non-Tracable"`. Carry it verbatim.
**Risk.** HIGH

---

## 6. Save

### UV02-R14 — Values are read with a fallback accessor
**Rule.** Fields read through an accessor that prefers the live control, then saved state, then
the API value.
**Source.** `:8769-8780`
**Risk.** MED

### UV02-R15 — GST is trimmed and uppercased
**Rule.** `.Trim().ToUpper()`, defaulting to `""`.
**Source.** `:8771`
**Java.** `Locale.ROOT`.
**Risk.** MED

### UV02-R16 — Dates convert to the API format on save
**Rule.** Display format converts to API format before sending.
**Source.** `:8770`
**Java.** `DateTimeFormatter` with `Locale.ROOT` — a Hindi or Arabic locale renders
non-ASCII digits.
**Risk.** HIGH

### UV02-R17 — Establishment date arrives as a Unix timestamp
**Rule.** The record holds the date as a Unix value, converted for display.
**Source.** `:2276`
**ANSWERED by the 2026-11-05 capture: MILLISECONDS.** Live data shows
`DateofBirth: "/Date(941481000000)/"` = 1999-11-01, and `GetBankVerApplData` returns the same
field as ISO-8601 `"1999-11-02T00:00:00"`. A seconds-based parse puts every date in 1970.
Also: `/Date(-62135596800000)/` is `DateTime.MinValue` and means **null**, not year 1.
**See.** [C02-R19 / C02-R20](../cross-cutting/C02-api-contracts.md)
**Risk.** HIGH

### UV02-R18 — DIGIPIN is saved from a page-level variable
**Rule.** On save, DIGIPIN is taken from a page-level field, not re-read from the control.
**Source.** `:8775`
**Risk.** MED

### UV02-R19 — Stage 10 re-validates seven of these fields
**Rule.** Final Submit requires: Unit Name, Updated Unit Address, Verification Status, Unit
Establishment Date, Udyam Registration Number, Location Co-ordinates, Digi PIN.
**Source.** `:1993-2029`
**Risk.** HIGH

### UV02-R20 — Read-only on concluded cases
**See.** [C09](../cross-cutting/C09-case-status-and-readonly.md) — including the
`forceEditable` fields in R02.
**Risk.** HIGH

---

## 7. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Record with a unit name | Unit Name locked (R01) |
| 2 | Record without one | editable, capped at 50 |
| 3 | **Completed case** | Address and Est. Date **still editable** (R02) |
| 4 | **Defunct case** | both **locked** (R02 + C09-R01) |
| 5 | Flag Digi PIN, tap GET LOCATION | Lon/Lat clear, **Digi PIN stays red** (R04) |
| 6 | Tap GENERATE DIGIPIN | Digi PIN clears, others keep their state |
| 7 | Tap the arrow "Open in Maps" in the map | external maps opens at the unit (R09) |
| 8 | Select Defunct, enter a remark, confirm | case concludes from stage 2 (R12) |
| 9 | Save, reload | establishment date identical (R17) |
| 10 | Disable location services, tap GET LOCATION | system settings page opens (R05) |

---

## 8. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 20 rules traced |
| R2 | 2026-11-05 | ⚠️ partial — step 7 confirmed during the maps ticket |
| R3 | — | pending |
