# S05 — Applicant list (Bank)

| | |
|---|---|
| **MAUI source** | `DetailsPageBL.xaml.cs` (831), `.xaml` (91) |
| **Java target** | same `ApplicantListActivity`, bank data source |
| **Review state** | **R1 PASSED** |
| **Rules** | 7 (2 HIGH) |

> Structurally [S04](S04-applicant-list-pv.md). Only the differences are recorded here;
> **everything in S04 applies unless contradicted below.**

---

## 1. Differences from S04

### S05-R01 — Endpoint
**Rule.** `GetBankApplicationData` instead of `GetPVList`.
**Open.** Not captured.
**Risk.** MED

### S05-R02 — Scoped by IFSC
**Rule.** The list is scoped to the logged-in bank's IFSC.
**See.** [S03-R05](S03-dashboard.md)
**Risk.** MED

### S05-R03 — Search placeholder
**Rule.** `DetailsPageBL.xaml:75` carries its own placeholder, updated alongside S04's.
**Note.** **Verify all three list placeholders read identically** — they are separate strings
and have drifted before.
**Risk.** LOW

### S05-R04 — Rows open the bank verification flow
**Rule.** A row opens `BankVerificationPage` / `BankPhysicalVerificationPage`, not Unit
Verification.
**See.** [S08](S08-bank-verification.md), [S09](S09-bank-physical-verification.md)
**Risk.** HIGH

### S05-R05 — No traceability button
**Rule.** The bank row's action set differs — traceability is a PV concept.
**Open.** Exact action set not fully traced. **Confirm on device.**
**Risk.** MED

### S05-R06 — Bank sessions have no token
**See.** [C01-R17](../cross-cutting/C01-session-and-auth.md) — and therefore no server logout
([C01-R13](../cross-cutting/C01-session-and-auth.md)).
**Risk.** HIGH

### S05-R07 — Standard header
**See.** [C07](../cross-cutting/C07-header-and-theme.md)
**Risk.** LOW

---

## 2. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Bank login → application list | rows scoped to that IFSC |
| 2 | Search by name and by id | both filter |
| 3 | Compare the placeholder with S04 and S06 | identical wording (R03) |
| 4 | Tap a row | bank verification, not unit verification |
| 5 | Note the action buttons | record the actual set (R05) |

---

## 3. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ⚠️ 7 rules; R05 incomplete |
| R2 | — | pending — **no bank credentials used this session by policy** |
| R3 | — | pending |
