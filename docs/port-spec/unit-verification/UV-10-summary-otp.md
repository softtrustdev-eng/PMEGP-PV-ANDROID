# UV10 — Stage 10: Summary, OTP & Final Submit

| | |
|---|---|
| **MAUI source** | builder `:6363-6900`, prerequisites `:1976-2100`, OTP `:1700-1975`, submit `PhyVerFinalSub_TP` |
| **Java target** | `ui/unitverification/stages/Stage10SummaryOtpFragment` + `data/repository/OtpRepository` |
| **Review state** | **R1 PASSED** |
| **Rules** | 21 (9 HIGH) |

> The gate. Everything the other nine stages did is re-checked here, then locked behind an OTP
> sent to the applicant.

---

## 1. Summary

### UV10-R01 — Read-only recap of every stage
**Rule.** Shows ~25 fields drawn from stages 1–9. All read-only.
**Source.** `:6363-6900`
**Risk.** MED

### UV10-R02 — Stage 9 values must be available without visiting Stage 9
**See.** [UV09-R06](UV-09-verification.md) — an earlier version left this stage blank until
Stage 9 had been opened.
**Risk.** HIGH

---

## 2. Prerequisites

### UV10-R03 — Final Submit is gated on a checklist ⚠️
**Rule.** Before OTP can be generated, every item below must pass. Each failure contributes a
named line naming its stage.

| # | Requirement | Stage |
|---|---|---|
| **0** | **Bank Verification fully completed** | **—** |
| 1 | Unit Name | 2 |
| 2 | Updated Unit Address | 2 |
| 3 | Verification Status | 2 |
| 4 | Unit Establishment Date | 2 |
| 5 | Udyam Registration Number | 2 |
| 6 | Location Co-ordinates | 2 |
| 7 | Digi PIN | 2 |
| 8 | Annual Production-Value **≥ 1** | 6 |
| 9 | Annual Sales-Value **≥ 1** | 6 |
| 10 | Product Details | 6 |
| 11 | Employment Detail — the three-way total rule | 7 |
| 12 | Document Upload — at least one image | 8 |
| 13 | Verification Status `Completed` **and** Enumerator Remark | 9 |

**Source.** `:1987-2090` — item 0 at `:1987-1991`

> **Added 2026-11-05 after device verification (F-016).** Item 0 was missing from the first
> draft of this document. It is the *first* check in the code, reads
> `applicantData.IsBankVerDone || phyVerificationModel.IsBankVerDone`, and was only found by
> opening Stage 10 on a real pending case and reading the blocker the app displayed:
> *"Complete the following before generating OTP: • Bank Verification"*.
>
> A port built from the draft would have let an officer generate an OTP on a case whose bank
> verification was not done. **Unit and bank completion are independent flags on one record
> ([S07-R05](../screens/S07-applicant-details.md)) — neither implies the other.**
**Note.** Stage 10 depends on a *sibling flow*, not just its own stages. Stages 1, 3, 4 and 5 are **not** checked. EDP, Project and Finance can be blank at
Final Submit — see [UV03-R06](UV-03-edp-training.md).
**Risk.** HIGH

### UV10-R04 — Messages name the stage
**Rule.** Each line reads `<field> (Stage N)`, e.g. `Digi PIN (Stage 2)`.
**Source.** `:1996-2085`
**Risk.** LOW

### UV10-R05 — Stage 6 amounts use an at-least-1 helper
**Rule.** The check is `value != null && value >= 1`, matching Stage 6's own rule rather than
"is non-zero".
**Source.** final-submit helper `AtLeastOne`
**Why.** Without it `0.0001` passed the Stage 10 gate even though Stage 6 rejects it.
**Risk.** HIGH

### UV10-R06 — Stage 7 is re-checked with the stage's own rule
**Rule.** Stage 10 mirrors Stage 7's live cross-field rule rather than testing a stored total.
**See.** [UV07-R09](UV-07-employment-detail.md) — the blank-to-`0` guard exists because these
two validators read through different accessors and disagreed.
**Risk.** HIGH

### UV10-R07 — Prerequisites read saved values, not live controls
**Rule.** The checklist reads through the saved-value accessor, because the other stages'
controls are not in the tree.
**Source.** `:2031`
**Why it matters.** This is the asymmetry behind UV07-R09: the accessor treats empty as "not
set" and falls back to the stale API value.
**Risk.** HIGH

---

## 3. OTP

### UV10-R03b — Bank Verification is prerequisite zero
**Rule.** The gate's **first** check is that bank verification is complete, satisfied when
*either* `applicantData.IsBankVerDone` *or* `phyVerificationModel.IsBankVerDone` is true.
Failing it adds the bare line `Bank Verification` — with no stage suffix, because it is not a
stage of this form.
**Source.** `:1987-1991`
**Verified on device** 2026-11-05: a pending case showed exactly
*"Complete the following before generating OTP: • Bank Verification"* with the button disabled.
**Java.** Two nullable booleans OR'd — a single field will be wrong for half the records.
**Risk.** HIGH

### UV10-R08 — OTP is mandatory
**Rule.** Final Submit refuses to proceed until the OTP is verified.
**Source.** `:350`
**Risk.** HIGH

### UV10-R09 — Generate
**Rule.** `POST {API_BASE}/SendPVOTPtoApplicant` → `{ "applID": int, "pvid": int }`;
response `{ "success", "message", "maskedMobile" }`. The masked number is shown.
**See.** [C02-R09](../cross-cutting/C02-api-contracts.md) — one of only two confirmed contracts.
**Risk.** HIGH

### UV10-R10 — `pvid` comes from the login response ⚠️
**Rule.** `pvid` is the login response's `PhysicalVeriID`, cached in a global.
**Source.** `:1798`, `:1907`
**🐞 See [F-001](../README.md).** The global is populated **only** during login. A session
resumed after a process kill leaves it `0`, and this stage sends `pvid: 0`.
**Java.** Read it from the session object. The bug disappears structurally.
**Risk.** HIGH

### UV10-R11 — Validate
**Rule.** `POST {API_BASE}/ValidateApplicantPVOTP` →
`{ "applID": int, "pvid": int, "otp": string }`; response `{ "success", "message" }`.
**Risk.** HIGH

### UV10-R12 — Resend cooldown
**Rule.** After generating, the button becomes `RESEND OTP IN {n}s`, counting down. Only when
it reaches zero does `RESEND OTP` become available.
**Source.** `:1753-1765`
**Risk.** MED

### UV10-R13 — The countdown stops if the stage is left
**Rule.** The timer aborts when the OTP button is detached or the current stage is no longer 10.
**Source.** `:1756`
**Java.** Tie to the fragment's `viewLifecycleOwner`, or the timer leaks.
**Risk.** MED

### UV10-R14 — Only the waiting state is recoloured
**Rule.** The restyling applies to the waiting state only; the live RESEND button keeps its own
colours.
**Source.** `:1749-1751`
**Risk.** LOW

### UV10-R15 — Leaving stage 10 resets the whole OTP flow ⚠️
**Rule.** Navigating away resets: generated flag, verified flag, verified snapshot, resend
timer, and the entered code is cleared. Returning shows `GENERATE OTP`, not `RESEND`.
**Source.** `:803-804`, `:1716-1728`
**Why.** A code generated or validated in one visit should not still stand on the next. Final
Submit is the only thing it was for, and it pops the page on success — so reaching the reset
means submit did not happen.
**See.** [C06-R12](../cross-cutting/C06-navigation-and-back.md)
**Risk.** HIGH

### UV10-R16 — Reset is a no-op when nothing was generated
**Rule.** Returns immediately if neither generated nor verified.
**Source.** `:1718-1719`
**Risk.** LOW

### UV10-R17 — A verified snapshot is held
**Rule.** The app keeps a snapshot taken at verification time, cleared on reset.
**Source.** `:1723`
**Open question.** Is the snapshot compared against the data at submit time — i.e. does editing
after verification invalidate the OTP? **Confirm during R2.**
**Risk.** HIGH

### UV10-R18 — Generate is gated on the prerequisites
**Rule.** The Generate OTP button is only enabled once UV10-R03 passes in full.
**Source.** `:1976-1990`
**Why.** No point sending a code to the applicant for a submission that cannot complete.
**Risk.** HIGH

---

## 4. Final Submit

### UV10-R19 — Endpoint
**Rule.** `POST {API_BASE}/PhyVerFinalSub_TP`.
**Open.** Payload not captured. **Blocks R2.**
**Risk.** HIGH

### UV10-R20 — Success pops the page
**Rule.** A successful submit navigates away without the leave confirmation.
**See.** [C06-R17](../cross-cutting/C06-navigation-and-back.md)
**Risk.** MED

### UV10-R21 — Read-only on concluded cases
**Rule.** A Defunct / Non-Traceable case shows the summary; no OTP, no submit.
**See.** [C09](../cross-cutting/C09-case-status-and-readonly.md)
**Risk.** MED

---

## 5. Open questions

| # | Question | Blocks |
|---|---|---|
| Q1 | `PhyVerFinalSub_TP` payload? | R19 |
| Q2 | Does editing after OTP verification invalidate it? | R17 |
| Q3 | Resend cooldown length — client-only or server-enforced? | R12 |
| Q4 | Is there a maximum OTP attempt count? | — |
| Q5 | Should stages 3, 4, 5 be in the checklist? | R03 |

---

## 6. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Fresh record, open stage 10 | checklist lists every missing item with its stage | ✅ done — surfaced F-016 |
| 2 | Annual Production `0.0001` | listed as missing (R05) |
| 3 | Complete stages 2, 6, 7, 8, 9 | Generate OTP enables (R18) |
| 4 | **Open a fresh record, go straight to stage 10** | summary populated without visiting stage 9 (R02) |
| 5 | Generate OTP | masked mobile shown, countdown starts |
| 6 | Wait for the countdown | `RESEND OTP` becomes available |
| 7 | Go to stage 9, return | **`GENERATE OTP`, not `RESEND`** (R15) |
| 8 | **Kill the process, resume, generate OTP** | **does it work, or send `pvid: 0`? (F-001)** |
| 9 | Verify OTP, go edit stage 7, return, submit | accepted or re-verified? (Q2) |
| 10 | Stage 7 blank-count path, then stage 10 | **must not** report "Stage 7 not filled" (R06) |

Steps 8 and 10 are the two that matter most.

---

## 7. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 21 rules traced |
| R2 | — | pending |
| R3 | — | pending |
