# S10 — Inspection details

| | |
|---|---|
| **MAUI source** | `InspectionDetailsPage.xaml.cs` (2,139), `.xaml` (42) |
| **Java target** | `ui/inspection/InspectionDetailsActivity` |
| **Review state** | **R1 PASSED** |
| **Rules** | 13 (4 HIGH) |

---

## 1. Fields

A **three**-stage form — `TotalSteps = 3`, same stepper pattern as Unit Verification
(`:150-151`, `:262`, `:407`).

**Stage 1 — Inspection Details** (11 fields):

| Label |
|---|
| Name of the Applicant* |
| Contact Number of Applicant* |
| Unit Address* |
| Digi PIN* |
| Area (in sq. ft) of the proposed unit* |
| Ownership Status* |
| Type of Industry (Manufacturing/Service)* |
| Activity Name (as per NIC code)* |
| Whether the unit is New or Existing* |
| Date and Time of Visit* |
| Observation |

**Stage 2** — document upload ([C04](../cross-cutting/C04-image-capture-and-compression.md)).

**Stage 3 — Summary** — repeats all 11 read-only, plus **Verification Status\***.

API fields seen: `ActivityName`, `ApplName`, `Base64File`.

---

## 2. Rules

### S10-R01 — The third form with exit confirmation
**Rule.** Back exits with the confirmation. `OnBackButtonPressed` was **added** to this page —
it previously had none, so back closed the app outright.
**Source.** new `OnBackButtonPressed` + `ConfirmAndLeave()`
**See.** [C06-R01](../cross-cutting/C06-navigation-and-back.md)
**Risk.** HIGH

### S10-R02 — Explicit submit confirmation
**Rule.** `Confirm Submission` / *"Are you sure you want to submit?"* before submitting.
**Source.** `:` submit handler
**Note.** Unit Verification gates on OTP instead
([UV10-R08](../unit-verification/UV-10-summary-otp.md)). Different mechanism, same intent.
**Risk.** MED

### S10-R03 — Document upload, same pipeline
**Rule.** Camera / gallery, the 2 MB gate, compression, base64 as `Base64File`.
**Source.** `:1164-1290`
**See.** [C04](../cross-cutting/C04-image-capture-and-compression.md)
**Risk.** HIGH

### S10-R04 — Gallery gate at `:1164`
**Rule.** 2 MB limit enforced on gallery picks.
**Source.** `:1164-1186`
**Risk.** MED

### S10-R05 — Upload gate at `:1261`
**Rule.** `maxUploadBytes` declared at `:1261`; compress when over and compressible (`:1274`);
reject if still over (`:1280`).
**See.** [C04-R06 / C04-R08](../cross-cutting/C04-image-capture-and-compression.md)
**Risk.** HIGH

### S10-R06 — Size hint label
**Rule.** `File size should be less than 2 MB.` at `:1363-1369`.
**Risk.** LOW

### S10-R07 — Extension from the filename
**Rule.** Follows the Unit Verification approach, not the magic-byte detector.
**See.** [S09-R08](S09-bank-physical-verification.md), [F-004](../README.md) — **three upload
screens, two strategies.**
**Risk.** MED

### S10-R08 — Camera-unavailable message
**Rule.** `Camera is not available`.
**Risk.** LOW

### S10-R09 — Picker options
**Rule.** `Choose File`, `Choose from Gallery`.
**Risk.** LOW

### S10-R10 — Input sanitising
**Rule.** Hooks at `:1855` (entry) and `:1938` (editor).
**See.** [C03](../cross-cutting/C03-input-sanitisation.md)
**Risk.** MED

### S10-R11 — Errors are developer-facing
**Rule.** `API returned error: {response.StatusCode}`.
**See.** [S02-R05](S02-verification-type.md)
**Risk.** MED

### S10-R12 — An Actions column
**Rule.** The form includes an `Actions` grouping.
**Open.** Contents not traced — Q1.
**Risk.** MED

### S10-R13 — Standard header
**Rule.** Uses `PageHeaderStyle`.
**Note.** **Not verified on device** — see [C07 §6](../cross-cutting/C07-header-and-theme.md).
**Risk.** LOW

---

## 3. Open questions

| # | Question | Blocks |
|---|---|---|
| Q2 | Submit endpoint and payload | R02 |
| Q3 | How is this screen reached? | navigation graph |

**Q1 and Q4 closed** — three stages, 11 fields on stage 1, enumerated above. The `Actions`
grouping (S10-R12) is the per-document action column on stage 2, not a form section.

---

## 4. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Reach the screen | record the entry point (Q3) |
| 2 | Confirm the 11 stage-1 fields | matches the table above |
| 3 | Back mid-form | confirmation, app does not close (R01) |
| 4 | Upload a 5 MB JPEG | accepted and compressed |
| 5 | Upload a 5 MB PDF | rejected |
| 6 | Submit | confirmation first (R02) |
| 7 | Header | matches every other page (R13) |

---

## 5. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 13 rules traced; 3 stages and stage-1 fields enumerated |
| R2 | — | pending — screen not reached this session |
| R3 | — | pending |
