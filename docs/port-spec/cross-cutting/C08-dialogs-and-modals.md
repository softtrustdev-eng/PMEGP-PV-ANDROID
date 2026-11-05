# C08 — Dialogs & modals

| | |
|---|---|
| **MAUI source** | `Util/ValidationPopup.cs`; per-page copies in `BankPhysicalVerificationPage.xaml.cs:306-310, 1147-1160, 1986-2060` and `UnitVerificationPage.xaml.cs`; remarks modal `DetailsPage.xaml.cs:1017-1150` |
| **Java target** | `ui/common/dialog/` — `ValidationDialog`, `RemarksDialog`, `ConfirmExitDialog`, `LoadingOverlay` |
| **Review state** | **R1 PASSED** — R2 partially done during the document-upload ticket |
| **Rules** | 16 (5 HIGH) |

---

## 1. Three layers, deliberately independent

The app stacks three overlay kinds, each tracked by its own reference:

| Layer | Field | Purpose |
|---|---|---|
| Form modal | `_currentModal` | an editable sub-form (add product, add document) |
| Validation popup | `_validationModal` | "Required Fields Missing", over a form modal |
| Loading overlay | `_uploadLoaderOverlay` | blocking spinner during a network call |

### C08-R01 — The validation popup must not close the form modal ⚠️⚠️
**Rule.** Showing a validation popup **never** closes the form modal underneath. The two are
tracked separately and dismissed separately.
**Source.** `BankPhysicalVerificationPage.xaml.cs:306-310` and its comment
**Why — this is a fixed defect.** The validation popup used to call the generic
`CloseCurrentModal()` as part of showing itself. On the Document Upload screen the officer
would hit a validation error and **the whole upload form vanished**, discarding everything
typed. The reported symptom was *"when a validation error occurs, the popup gets closed
automatically"* — actually the form being destroyed behind it.
**Java.** Separate `DialogFragment`s, or separate `Dialog` instances. **Never** route dismissal
through a single "close the current dialog" helper — that helper is exactly what caused this.
**Risk.** HIGH

### C08-R02 — Dismissal order is validation, then form
**Rule.** When back is pressed with both showing, the validation popup closes first and the
press is consumed. A second press closes the form modal. A third runs the leave confirmation
([C06-R03](C06-navigation-and-back.md)).
**Source.** `BankPhysicalVerificationPage.xaml.cs:1147-1160`
**Risk.** MED

### C08-R03 — The loader is independent of both
**Rule.** The loading overlay has its own reference and its own show/hide, so it can appear
over a form modal without disturbing it.
**Source.** `:1986`, `:2052`
**Risk.** MED

---

## 2. The validation popup

### C08-R04 — Appearance
**Rule.** Warning icon, red heading, grey subtitle, optional bulleted list of missing field
names, orange `OK`. White card, border `#E0E0E0`, corner radius 15, padding 25, width **340**,
with shadow. Scrim `#AA000000`, filling the page.
**Source.** `Util/ValidationPopup.cs:30-50`
**Risk.** LOW

### C08-R05 — Default wording
**Rule.** Title `Required Fields Missing`; subtitle *"Please fill in the following fields
before saving:"*. Both overridable — Stage 8 passes `Document Required` /
*"Please upload at least one image before proceeding."* with an empty list.
**Source.** `Util/ValidationPopup.cs:21-23`, used at `UnitVerificationPage.xaml.cs:789-792`
**Risk.** LOW

### C08-R06 — Taps on the scrim are swallowed, not passed through
**Rule.** The overlay registers an empty tap recogniser so taps on the dimmed area do nothing.
**Source.** `Util/ValidationPopup.cs:36`, `DetailsPage.xaml.cs:1030`
**Why.** The scrim is visual only by default — taps fall straight through to the content
behind it, letting the officer open an applicant while a modal is still open.
**Java.** `Dialog` blocks touches by default. Also `setCanceledOnTouchOutside(false)` so a
scrim tap does not dismiss.
**Risk.** MED

### C08-R07 — The list behind a modal is disabled outright
**Rule.** While the remarks modal is open the applicant list control is **disabled**, not just
covered.
**Source.** `DetailsPage.xaml.cs:1143-1145`
**Why.** A tap-swallowing overlay alone does not reliably stop a virtualised list's own
selection and scroll handling.
**Java.** A `DialogFragment` handles this; if a custom overlay is used instead, disable the
`RecyclerView`.
**Risk.** MED

### C08-R08 — Three implementations exist, one shared
**Rule.** `Util/ValidationPopup` is the standalone version for pages without modal plumbing.
`BankPhysicalVerificationPage` and `UnitVerificationPage` each carry their **own copy** built
around their modal stacks.
**Source.** `Util/ValidationPopup.cs:8-11`
**Java.** Build **one** `ValidationDialog`. The three copies exist only because the MAUI
versions are coupled to page-local modal state; a `DialogFragment` has no such coupling. Verify
all three produce identical wording before unifying — the copies may have drifted.
**Risk.** MED

---

## 3. The loading overlay

### C08-R09 — Blocking, with a message
**Rule.** A full-screen overlay with a spinner and a caller-supplied message covers the page
during a network call. Known messages: `Saving details...`, `Loading...`,
`Uploading documents...`, `Getting location...`, `Generating DigiPIN...`.
**Source.** `:1986`, `:441`, `:1856`, `:1942`
**Risk.** MED

### C08-R10 — Always hidden in a finally, before any error dialog ⚠️
**Rule.** The hide runs in a `finally` so it clears even when the call throws — and **before**
the catch shows its alert.
**Source.** `UnitVerificationPage.xaml.cs:9525-9535`
**Why.** Otherwise a failed save leaves the form locked behind a permanent spinner, and the
error alert stacks on top of the scrim.
**Java.** `try { … } finally { hideLoader(); }` — never hide only on the success path.
**Risk.** HIGH

### C08-R11 — The loader exists because the officer re-taps
**Rule.** Saving is a network round-trip with no other feedback; without the overlay the
officer taps Save again thinking nothing happened.
**Source.** `:9521-9524` comment
**Risk.** MED

---

## 4. The remarks modal

### C08-R12 — Layout
**Rule.** Centred white card, width 340, corner radius 15, padding 25, border `#E0E0E0`,
shadow; spacing 20. Contents top to bottom: emoji icon (font size 50, centred), bold title
(18, `#333333`, centred), multi-line editor, then `CANCEL` / `YES` side by side in equal
columns with spacing 15, height 45, corner radius 10.
**Source.** `DetailsPage.xaml.cs:1033-1120`
**Risk.** LOW

### C08-R13 — Editor styling
**Rule.** Placeholder `Enter your remarks here...`, font 16, height 120, background `#F5F5F5`,
**text colour `#333333`**, placeholder colour `#999999`.
**Source.** `DetailsPage.xaml.cs:1063-1075`
**Why the explicit text colour.** Without it the editor took the platform default, which
rendered near-white on the `#F5F5F5` background — the officer's remarks were invisible as they
typed.
**Java.** Set `android:textColor` explicitly on every input with a custom background. Do not
rely on the theme.
**Risk.** MED

### C08-R14 — Length cap and sanitising
**Rule.** `MaxLength = 250`, plus the C03 sanitiser with newlines allowed.
**Source.** `DetailsPage.xaml.cs` remarks editor
**See.** [C03-R11](C03-input-sanitisation.md)
**Risk.** MED

### C08-R15 — Result contract
**Rule.** `YES` returns the editor's text; `CANCEL` returns **null**. The caller distinguishes
them — null means cancelled, empty string means "confirmed with no remark".
**Source.** `DetailsPage.xaml.cs:1096-1118`
**Java.** A nullable result in the callback, or a sealed `Confirmed(text)` / `Cancelled`.
**Risk.** MED

### C08-R16 — Remarks are not yet persisted
**Rule.** The returned text is logged and discarded — `// TODO: Save remarks to API if needed`.
**Source.** `DetailsPage.xaml.cs:998-1001`, `:1445-1447`
**Status.** Open. The modal is fully built and wired but no endpoint consumes it.
**Action.** Confirm with the backend team whether an endpoint is planned before the port, so
the Java version either wires it or omits the modal — rather than shipping a dialog whose
output goes nowhere.
**Risk.** MED

---

## 5. Plain alerts

Native alerts (no custom styling) are used for simple messages. Confirmed set:

| Title | Body | Buttons | Rule |
|---|---|---|---|
| `Leave Verification` | Do you want to leave this verification? Unsaved changes will be lost. | Yes / No | [C06-R02](C06-navigation-and-back.md) |
| `Employment Detail Incomplete` | per-rule lines, blank line between | OK | [UV07-R20](../unit-verification/UV-07-employment-detail.md) |
| `File Too Large` | The selected file exceeds the 2 MB limit. Please choose a smaller file. | OK | [C04-R15](C04-image-capture-and-compression.md) |
| `Error` | Failed to save Employment | OK | [UV07-R17](../unit-verification/UV-07-employment-detail.md) |

**Java.** `MaterialAlertDialogBuilder`. Two-button dialogs put the affirmative on the right —
MAUI's `Yes`/`No` ordering already matches.

---

## 6. R2 checklist

| # | Step | Expected | State |
|---|---|---|---|
| 1 | Bank login → Pending → Document Upload → save with a missing field | validation popup shows, **form still there behind it** | ✅ done |
| 2 | Dismiss that popup | form modal still open, values intact | ✅ done |
| 3 | Back with both showing | validation closes first, form stays | ⬜ |
| 4 | Trigger a save failure (airplane mode) | loader clears, error alert not on the scrim | ⬜ |
| 5 | Open remarks modal, type | text is **visible**, not white-on-grey | ✅ done |
| 6 | Remarks modal, tap the dimmed area | nothing happens, list not reachable | ⬜ |
| 7 | Remarks modal, CANCEL | caller sees null, not "" | ⬜ |
| 8 | Compare the three validation popups' wording | identical | ⬜ |

---

## 7. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 16 rules traced |
| R2 | 2026-11-05 | ⚠️ 3 of 8 confirmed during the document-upload ticket |
| R3 | — | pending |
