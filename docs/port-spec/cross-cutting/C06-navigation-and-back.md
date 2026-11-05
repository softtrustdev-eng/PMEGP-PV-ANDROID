# C06 — Navigation & back

| | |
|---|---|
| **MAUI source** | `UnitVerificationPage.xaml.cs:754-830` (guards), `:1443-1456` (back), `:10399-10410` (confirm); same shape in `BankPhysicalVerificationPage` and `InspectionDetailsPage`; `Platforms/Android/MainActivity.cs` |
| **Java target** | `ui/unitverification/UnitVerificationActivity`, `ui/common/dialog/ConfirmExitDialog` |
| **Review state** | **R1 PASSED** — R2 partially done on device during the back-stack ticket |
| **Rules** | 17 (6 HIGH) |

---

## 1. Back behaviour

### C06-R01 — Back leaves the form, from any stage ⚠️
**Rule.** Pressing the system back button on a multi-stage form shows a confirmation and, on
`Yes`, **leaves the form entirely** — returning to the applicant list. It does **not** step
back one stage, and does **not** retrace the stages the officer visited.
**Source.** `UnitVerificationPage.xaml.cs:1450-1455`
**Why.** Walking back through every visited stage to get out *was* the reported defect. The
step bar is how you move between stages; back is how you leave.
**Applies to.** Unit Verification, Bank Physical Verification, Inspection Details — all three.
**Risk.** HIGH

### C06-R02 — Confirmation wording
**Rule.** Title `Leave Verification`, body *"Do you want to leave this verification? Unsaved
changes will be lost."*, buttons `Yes` / `No`. `Yes` pops the page; `No` does nothing.
**Source.** `:10403-10408`
**Risk.** LOW

### C06-R03 — An open modal closes first
**Rule.** When a modal is open, back closes the modal and consumes the press. The leave
confirmation is **not** shown.
**Source.** `:1445-1449`
**Why.** Back on a dialog means "dismiss the dialog", not "abandon the form".
**Java.** Handle in the `OnBackPressedCallback` chain: the dialog registers its own callback
with higher priority, or the activity checks for a showing dialog first.
**Risk.** MED

### C06-R04 — The header chevron behaves identically to back
**Rule.** The `‹` chevron in the page header runs the same confirmation.
**Source.** `OnBackButtonClicked` → `ConfirmAndLeave()`
**Why.** Two controls that look like "go back" must not differ — one confirming and the other
silently discarding is a trap.
**Risk.** MED

### C06-R05 — The step-history stack was removed
**Rule.** There is **no** per-stage history. Navigation state is a single current-stage integer.
**Source.** `_stepHistory`, its push, and the `recordHistory` parameter were deleted — a
project-wide grep returns nothing.
**Why it is recorded.** Anyone reading the old MAUI code in git history will find a history
stack and may reimplement it. It was removed deliberately; R01 replaced it.
**Java.** One `int currentStage` in the ViewModel. Do **not** put the stages on the Fragment
back stack — that reintroduces exactly the retrace behaviour R01 removes.
**Risk.** HIGH

### C06-R06 — Android 14+ predictive back
**Rule.** The app declares `enableOnBackInvokedCallback="true"` and bridges the platform
dispatcher to the legacy back handler.
**Source.** `Platforms/Android/AndroidManifest.xml`, `MainActivity.cs`
**Why.** On Android 13+ with predictive back enabled, the legacy `onBackPressed` path is not
called and the app closed outright instead of running the confirmation.
**Java.** `OnBackPressedDispatcher` + `OnBackPressedCallback` throughout. This is the standard
AndroidX path and the MAUI bridging workaround disappears.
**Risk.** HIGH

---

## 2. Stage navigation

### C06-R07 — Stage range
**Rule.** Stage changes are accepted only for `1..TotalSteps` and only when the target differs
from the current stage.
**Source.** `:754`
**Risk.** LOW

### C06-R08 — Forward guards on stages 6, 7 and 8 ⚠️
**Rule.** Moving **forward** past these stages is blocked while they are invalid and the case
is editable. Moving **backward** is always allowed.

| From | Blocked when | Reaction |
|---|---|---|
| 6 | Product & Sales invalid | clear field errors, show validation modal, mark each invalid field, focus the first |
| 7 | Employment invalid | `Employment Detail Incomplete` dialog ([UV07-R20](../unit-verification/UV-07-employment-detail.md)) |
| 8 | no document uploaded | modal `Document Required` / *"Please upload at least one image before proceeding."* |

**Source.** `:757-795`
**Why.** Disabling the SAVE & NEXT button alone left the **step indicator** as an open route
past an invalid stage — which is how Stage 10 ended up reporting "Stage 7 details are not
filled" on a stage the officer had apparently completed. The button and the indicator must be
guarded together.
**Risk.** HIGH

### C06-R09 — Guards do not apply to read-only cases
**Rule.** Stages 7 and 8 check `_isEditable` first; a concluded case navigates freely.
**Source.** `:777`, `:789`
**Note.** Stage 6's guard does **not** test `_isEditable`. Either an oversight or it relies on
a read-only stage validating clean. **Confirm during R2** — open a Defunct case and try to
move forward from Stage 6.
**Risk.** MED

### C06-R10 — Stage 6 clears field errors before re-validating
**Rule.** The Stage 6 guard clears all field errors first, then re-marks only what is still
invalid.
**Source.** `:759`
**Why.** Without it, errors from a previous attempt accumulate.
**Risk.** MED

### C06-R11 — Order of operations on a successful change
**Rule.** Exactly:
1. run the guards (abort on failure)
2. **save current stage state**
3. if leaving stage 10, reset the OTP flow
4. set the current stage
5. update the step indicator visuals
6. rebuild the stage content
7. wait ~50 ms
8. restore the stage state
9. scroll the step strip to the new stage

**Source.** `:796-815`
**Why the ordering matters.** Save-before-switch (2 before 4) is what preserves unsaved typing.
The delay at 7 exists because the controls at 6 are not in the tree yet — restoring into
unattached controls silently does nothing.
**Java.** Fragment transaction committed, then restore in `onViewCreated`. The arbitrary delay
disappears; reproduce the *effect*, not the `Task.Delay`.
**Risk.** HIGH

### C06-R12 — Leaving stage 10 resets the OTP flow ⚠️
**Rule.** Navigating away from stage 10 to any other stage resets the OTP cycle. Returning
starts at `GENERATE OTP`, not `RESEND`.
**Source.** `:803-804`
**Why.** A code generated or validated in one visit should not still stand on the next. Final
Submit is the only thing it was for, and it pops the page on success — so reaching this code
means submit did not happen.
**Risk.** HIGH

---

## 3. Step indicator

### C06-R13 — Three visual states
**Rule.** Each step renders as current, past (completed), or future. The connector between two
steps is "filled" when the step after it has been reached.
**Source.** `:681-725`
**Risk.** LOW

### C06-R14 — Tapping a step is a navigation request, not a jump
**Rule.** A tap routes through the same guarded path as any other stage change — the guards in
R08 apply.
**Source.** `:754`
**Risk.** HIGH

### C06-R15 — The strip scrolls to keep the current step visible
**Rule.** After a stage change the horizontal strip scrolls so the current step is in view.
Scroll position is clamped at 0 and animation is **off**.
**Source.** `:819-828`
**Note.** Animation was deliberately changed from on to off.
**Risk.** LOW

---

## 4. Entry and exit

### C06-R16 — Resumed sessions can make the form the navigation root
**Rule.** After a session resume the verification page can be the root of the navigation stack,
with nothing behind it.
**Source.** `:1443-1447` comment
**Why.** Without the back override the app closed outright instead of returning to the list.
**Java.** The activity must handle "nothing to pop" — finish to the applicant list explicitly
rather than relying on the task stack.
**Risk.** HIGH

### C06-R17 — Final Submit pops the page
**Rule.** A successful Final Submit navigates away. The leave confirmation is not shown.
**Source.** implied by `:803` comment
**Risk.** MED

---

## 5. Java design notes

- **One activity, ten fragments, one shared ViewModel.** Stage state lives in the ViewModel, so
  C06-R11's save/restore dance becomes "the data was never lost" — a genuine simplification.
- **Do not use the fragment back stack for stages.** `replace()` without `addToBackStack()`.
  Adding to the back stack reintroduces the retrace defect (C06-R05).
- **One `OnBackPressedCallback` on the activity** running the confirmation; dialogs register
  their own at higher priority (C06-R03).
- **Guards belong in the ViewModel**, not the fragment, so the step indicator and the
  SAVE & NEXT button call the identical code path (C06-R08/R14).

---

## 6. R2 checklist

| # | Step | Expected | State |
|---|---|---|---|
| 1 | Back from stage 1 | confirmation, then applicant list | ✅ done |
| 2 | Back from stage 7 | **same** — not stage 6 | ✅ done |
| 3 | Header chevron from stage 5 | identical confirmation | ✅ done |
| 4 | Open a modal, press back | modal closes, form stays | ⬜ |
| 5 | Stage 6 invalid → tap step 9 | blocked, modal, first field focused | ⬜ |
| 6 | Stage 7 invalid → tap step 10 | blocked, `Employment Detail Incomplete` | ✅ done |
| 7 | Stage 8 with no upload → tap step 9 | blocked, `Document Required` | ⬜ |
| 8 | Stage 10, generate OTP, go to stage 9, return | shows `GENERATE OTP`, not `RESEND` | ⬜ |
| 9 | Resume a killed session into the form, press back | applicant list, app does not close | ⬜ |
| 10 | **Defunct case, stage 6, tap step 7** | does the guard fire? (C06-R09) | ⬜ |
| 11 | Android 14 device, predictive back gesture | confirmation appears | ✅ done |

---

## 7. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 17 rules traced |
| R2 | 2026-11-05 | ⚠️ partial — 4 of 11 confirmed during the back-stack ticket |
| R3 | — | pending |
