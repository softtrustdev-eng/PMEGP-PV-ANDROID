# S07 — Applicant details

| | |
|---|---|
| **MAUI source** | `ApplicantDetailsPage.xaml.cs` (808), `.xaml` (465) |
| **Java target** | `ui/applicants/ApplicantDetailActivity` |
| **Review state** | **R1 PASSED** |
| **Rules** | 10 (4 HIGH) |

---

## 1. Purpose

A read-only record view, reached from an applicant row, with entry points into verification.

---

## 2. Rules

### S07-R01 — Two endpoints
**Rule.** `GetApplDataForPV` for the record; `GetApplicantImage` for the photograph.
**Source.** `:15-16`
**Risk.** MED

### S07-R02 — Constructed from an `applId` alone ⚠️
**Rule.** The page receives only the applicant id. It has **no** list context and no idea which
category the officer came from.
**Source.** `:678`
**Risk.** HIGH

### S07-R03 — It hardcodes `"Pending"` when opening Unit Verification ⚠️⚠️
**Rule.**

```csharp
var unitVerificationPage = new UnitVerificationPage(_applID, "Pending", badgeStatus, isUnitVerDone);
//                                                            ^^^^^^^^^ detailsPageStatus
```

**Source.** `ApplicantDetailsPage.xaml.cs:678`
**This is the exact defect path behind [C09-R04](../cross-cutting/C09-case-status-and-readonly.md).**
Because S07-R02 leaves it no category, it passes a literal `"Pending"`. The verification page's
constructor then computes `concluded = false` — so **opening a Defunct or Non-Traceable case
this way unlocked every field.** The save is rejected server-side, but the officer only
discovers that after typing the whole form.

The fix is not here. The verification page re-derives both flags from the server's `VeriStatus`
once the record loads, and that is authoritative however the page was opened.

**Java.** Do not pass editability as a navigation argument. Derive it in the ViewModel after
the record loads. Passing an id and letting the ViewModel decide makes this bug
unrepresentable.
**Risk.** HIGH

### S07-R04 — Badge status is derived from the record
**Rule.** `badgeStatus = isUnitVerDone ? "Completed" : "Pending"`.
**Source.** `:677`
**Risk.** MED

### S07-R05 — Bank status is derived separately
**Rule.** `IsBankVerDone == true ? "Completed" : "Pending"`.
**Source.** `:656`
**Note.** Unit and bank verification have **independent** completion flags on one record.
**Risk.** HIGH

### S07-R06 — The photograph loads separately and may be absent
**Rule.** The image comes from its own endpoint; a failure leaves the rest of the screen intact.
**Source.** `:16`
**Java.** Load asynchronously with a placeholder. **Cancel on detach** — see
[S04-R06](S04-applicant-list-pv.md) for the recycling hazard this mirrors.
**Risk.** MED

### S07-R07 — Entry point into Unit Verification
**Rule.** The screen navigates into the 10-stage form.
**See.** [UV00](../unit-verification/UV-00-host-and-shared-state.md)
**Risk.** MED

### S07-R08 — Fields may be encrypted
**See.** [C10-R05](../cross-cutting/C10-cryptography.md)
**Risk.** MED

### S07-R09 — Read-only
**Rule.** No field on this screen is editable.
**Risk.** MED

### S07-R10 — Standard header
**See.** [C07](../cross-cutting/C07-header-and-theme.md)
**Risk.** LOW

---

## 3. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Open from a pending row | record and photo render |
| 2 | Record with no photo | screen still renders |
| 3 | Open Unit Verification from here | opens correctly |
| 4 | **Open a Defunct case via this screen, then Unit Verification** | **read-only** — the S07-R03 defect path |
| 5 | A record with unit done but bank not | badges differ (R05) |

Step 4 is the single most important check in this document.

---

## 4. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 10 rules traced; R03 confirmed at source |
| R2 | — | pending — needs a Defunct case |
| R3 | — | pending |
