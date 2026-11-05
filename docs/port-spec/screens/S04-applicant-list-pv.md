# S04 — Applicant list (PV)

| | |
|---|---|
| **MAUI source** | `DetailsPage.xaml.cs` (1,558), `.xaml` (134) |
| **Java target** | `ui/applicants/ApplicantListActivity` + `ApplicantAdapter` |
| **Review state** | **R1 PASSED** |
| **Rules** | 20 (6 HIGH) |

> The template for S05 (Bank) and S06 (Pre/Post), which are the same screen against different
> endpoints.

---

## 1. Search

### S04-R01 — Matches name OR id, case-insensitive substring
**Rule.** A row matches when `Name` **or** `Id` contains the search text, ignoring case. Blank
search shows everything.
**Source.** `:491-505`
**Risk.** MED

### S04-R02 — Filters on every keystroke, client-side
**Rule.** Setting the search text re-filters immediately. No debounce, no server call — the
whole list is already in memory.
**Source.** `:213-219`
**Java.** `Filterable` adapter or a filtered `LiveData`. At ~600 rows a `TextWatcher` filter is
fine; keep it off the main thread if lists grow.
**Risk.** MED

### S04-R03 — Placeholder
**Rule.** `Search by Applicant's Name or Id`, colour `#999999`.
**Source.** `DetailsPage.xaml:52-53`
**Note.** The ticket asked for `Search by Applicant's Name/Id`. The shipped string uses " or ".
**Confirm the intended wording with the tester** — do not change it unilaterally.
**Risk.** LOW

### S04-R04 — Search is not re-applied after a refresh
**Rule.** Both the initial load and the refresh path explicitly re-run the filter.
**Source.** `:301`, `:321`
**Why.** Otherwise a refresh would show the full list while the search box still held text.
**Risk.** MED

---

## 2. List rendering

### S04-R05 — Virtualised, 50 rows per page ⚠️
**Rule.** Rows are added a page at a time — `PageSize = 50` — rather than all ~600 at once. The
list realises only visible rows plus a small buffer, and appends the next page when 10 rows
remain.
**Source.** `:816-845`
**Why.** Replacing a ScrollView of pre-built cards with a virtualised list was a deliberate
performance fix.
**Java.** `RecyclerView` + `DiffUtil` is virtualised by default. Keep the paging only if the
list grows beyond a few thousand; a `RecyclerView` handles 600 rows without it.
**Risk.** HIGH

### S04-R06 — One reusable card, re-pointed at different data ⚠️
**Rule.** A single card template is built once and the list keeps a small pool, re-pointing each
at a different row.
**Source.** `:859-862`
**Consequence.** Anything stashed on a card instance **is wrong after recycling**. The code
carries an explicit warning that a stashed `Image` ends up on the wrong row.
**Java.** This is exactly `ViewHolder` recycling. Never hold per-row state on the holder — bind
everything in `onBindViewHolder`, and **cancel any in-flight image load** for the recycled row.
**Risk.** HIGH

### S04-R07 — One timer drives every blink
**Rule.** A single page-level timer animates all Non-Traceable outlines, not one animation per
card.
**Source.** `:551-562`
**Why.** Per-card animations ticked on **every frame**; an atrace capture showed each blinking
card invalidating continuously, and in the Non-Traceable list that is every row.
**Java.** One `ValueAnimator` on the fragment applied to bound holders. **Never** one animator
per `ViewHolder` — they leak on recycle.
**Risk.** HIGH

### S04-R08 — Blink is suppressed in the Non-Traceable list
**See.** [C09-R14](../cross-cutting/C09-case-status-and-readonly.md)
**Risk.** MED

### S04-R09 — Card stroke starts transparent
**Rule.** A row's outline brush is transparent until a Non-Traceable row actually needs it.
**Source.** `:874`
**Risk.** LOW

---

## 3. Row contents

### S04-R10 — Fields shown
**Rule.** Applicant Id, Name, Address, Date Of Submission, a status badge (e.g. `In Progress`),
and a five-button action row.
**Source.** `:1207`, `.xaml`
**Risk.** LOW

### S04-R11 — Address is assembled, de-duplicated
**Rule.** Address parts are joined, skipping any part already present (case-insensitive).
**Source.** `:395`
**Why.** The record repeats village/mandal/district, producing
*"BADANGI VILLAGE BADANGI MANDAL, Vizianagaram"* style duplication.
**Risk.** MED

### S04-R12 — Five action buttons
**Rule.** Documents · Briefcase (verification) · **Traceability status** · Navigate (`geo:`) ·
Call.
**Source.** `:1207-1250`
**Risk.** MED

### S04-R13 — The `geo:` icon launches the maps app directly
**Rule.** Tapping it opens the external maps app at the applicant's coordinates.
**Source.** `:1207-1250`
**Why recorded.** This is the **proof** that Android 11+ package visibility is not what breaks
the Stage 2 in-map link — the same app launches maps successfully from here.
**See.** [UV02-R10](../unit-verification/UV-02-unit-detail.md)
**Risk.** MED

### S04-R14 — Call uses the dialer
**Rule.** The call button opens the phone dialer. `CALL_PHONE` is declared.
**Java.** Prefer `ACTION_DIAL`, which needs **no** permission, over `ACTION_CALL`. Record the
permission change for the tester.
**Risk.** MED

### S04-R15 — Traceability button renders per status
**See.** [C09-R15](../cross-cutting/C09-case-status-and-readonly.md) — including the
`"Non-Tracable"` spelling ([F-009](../README.md)).
**Risk.** HIGH

### S04-R16 — Unknown status opens the remarks modal
**See.** [C08 §4](../cross-cutting/C08-dialogs-and-modals.md), [F-008](../README.md) — the
remarks go nowhere.
**Risk.** MED

---

## 4. Loading

### S04-R17 — Endpoint
**Rule.** `GetPVList`.
**Open.** Request/response not captured.
**Risk.** MED

### S04-R18 — "No Data" is detected from the message text
**Rule.** An empty result is recognised by the response message **containing** `"No Data"`,
case-insensitively.
**Source.** `:291`
**Note.** Same fragility as [S01-R16](S01-login.md) — behaviour driven by English prose. Part of
the `reason`-code question in [C02-R18](../cross-cutting/C02-api-contracts.md).
**Risk.** HIGH

### S04-R19 — The empty-list alert is gated, not cleared ⚠️
**Rule.** The call that shows the empty alert is itself gated, not the state afterwards.
**Source.** `:547-550`
**Why.** The constructor's load **and** the on-appearing refresh both run and both reach the
alert, so a second popup was already queued before the first was dismissed. Clearing state
afterwards could not stop it — the call has to be gated.
**Java.** Single-shot `LiveData`/`Event`, or a `showing` flag checked before showing.
**Risk.** HIGH

### S04-R20 — The list is disabled while a modal is open
**See.** [C08-R07](../cross-cutting/C08-dialogs-and-modals.md)
**Risk.** MED

---

## 5. R2 checklist

| # | Step | Expected | State |
|---|---|---|---|
| 1 | Open Reports Pending | rows render, badges correct | ✅ |
| 2 | Type part of a name | filters live | ⬜ |
| 3 | Type part of an id | **also** filters (R01) | ⬜ |
| 4 | Mixed case search | matches | ⬜ |
| 5 | Scroll ~600 rows | smooth; next page appends | ⬜ |
| 6 | Scroll fast, then back | **no wrong images on rows** (R06) | ⬜ |
| 7 | PV list with an NT row | one row pulses | ⬜ |
| 8 | Non-Traceable list | **no blinking** (R08) | ⬜ |
| 9 | Tap the geo icon | maps opens at the applicant | ✅ |
| 10 | Open a zero-count category | **one** alert, not two (R19) | ⬜ |
| 11 | Tap `? Status` | remarks modal | ✅ |

---

## 6. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 20 rules traced |
| R2 | 2026-11-05 | ⚠️ 3 of 11 confirmed on device |
| R3 | — | pending |
