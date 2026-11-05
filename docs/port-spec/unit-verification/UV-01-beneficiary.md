# UV01 — Stage 1: Beneficiary

| | |
|---|---|
| **MAUI source** | `UnitVerificationPage.xaml.cs:1503-2230` |
| **Java target** | `ui/unitverification/stages/Stage01BeneficiaryFragment` |
| **Review state** | **R1 PASSED** |
| **Rules** | 6 (1 HIGH) |

---

## 1. Purpose

Displays the applicant's identity as held by the server. **Entirely read-only.** The officer
confirms it is the right person and moves on.

---

## 2. Fields

All read-only, all from `applicantData`.

| Label | Source |
|---|---|
| Beneficiary ID | applicant record |
| Beneficiary Name | applicant record |
| Gender | applicant record |
| Social Category | applicant record |
| Special Category | applicant record |
| Email ID | applicant record |
| Mobile Number | applicant record |

---

## 3. Rules

### UV01-R01 — No field is editable, on any case
**Rule.** Every field is read-only regardless of `isEditable` or `isConcludedCase`.
**Source.** `:1503-1700` — all builders pass `forceReadOnly`
**Risk.** MED

### UV01-R02 — Stage 1 never calls the API ⚠️
**Rule.** `SAVE & NEXT` advances without any network call. The save method is an empty
awaitable.
**Source.** `:8760-8765`
**Why.** Nothing on this stage is officer input, so there is nothing to persist.
**Java.** Do not route this stage through the repository. A developer following the pattern of
stages 2–7 will add a save call that posts an empty body to `Insert_UVData_PV` — with
[C02-R08](../cross-cutting/C02-api-contracts.md)'s shared-DTO risk, that could **blank the
record**.
**Risk.** HIGH

### UV01-R03 — Values show a loading placeholder until the record arrives
**Rule.** Fields render `Loading...` until the applicant record resolves.
**Source.** `:1503-1700`
**Risk.** LOW

### UV01-R04 — Fields may be encrypted
**Rule.** Values pass through the TripleDES decryptor; anything that is not valid ciphertext is
displayed unchanged.
**See.** [C10-R05](../cross-cutting/C10-cryptography.md)
**Risk.** MED

### UV01-R05 — No PREVIOUS button
**Rule.** Stage 1 is the first stage; only `NEXT` / `SAVE & NEXT` is shown.
**Risk.** LOW

### UV01-R06 — Stage 10 repeats these fields
**Rule.** All seven reappear on the summary.
**See.** [UV10](UV-10-summary-otp.md)
**Risk.** LOW

---

## 4. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Open any case at stage 1 | all seven fields grey/read-only |
| 2 | Tap a field | no keyboard |
| 3 | Airplane mode, tap SAVE & NEXT | **advances to stage 2** — no network call (R02) |
| 4 | Compare values against stage 10 | identical |

---

## 5. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 6 rules traced |
| R2 | — | pending |
| R3 | — | pending |
