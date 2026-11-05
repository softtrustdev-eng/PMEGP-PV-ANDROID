# S08 — Bank verification

| | |
|---|---|
| **MAUI source** | `BankVerificationPage.xaml.cs` (1,083), `.xaml` (619) |
| **Java target** | `ui/bankverification/BankVerificationActivity` |
| **Review state** | **R1 PASSED** |
| **Rules** | 12 (4 HIGH) |

---

## 1. Fields

| Label | Type | Validation message |
|---|---|---|
| TDR/SRF number | text | `Enter TDR/SRF number` |
| TDR/SRF amount | amount | `Enter TDR/SRF amount` |
| Margin money release amount | amount | `Enter margin money release amount` |
| Balance Loan to be Released | amount | `Enter balance loan to be released` |
| NPA period | text | `Enter NPA period` |
| *(amount fields, shared)* | | `Enter a valid amount (not negative)` |

API fields seen: `ApplCode`, `ApplTDRActNo`, `BalLnReleased`, `BalLoanAmt`.

---

## 2. Rules

### S08-R01 — Numeric fields use an allowlist, not the sanitiser ⚠️
**Rule.** Six fields are digits-only via `AllowPositiveNumbersOnly`. The
[C03](../cross-cutting/C03-input-sanitisation.md) blocklist is **not** wired here, and
correctly so — an allowlist is strictly stronger.
**Source.** `AllowPositiveNumbersOnly` throughout
**Java.** **Do not replace this with the C03 sanitiser.** That would weaken the screen. Keep
the digits-only filter.
**See.** [C03 §4](../cross-cutting/C03-input-sanitisation.md), [C05-R23](../cross-cutting/C05-form-field-behaviour.md)
**Risk.** HIGH

### S08-R02 — Four hidden mirror entries
**Rule.** Four entries are hidden and mirror visible values.
**Source.** screen audit
**Java.** Model as ViewModel state, not hidden views.
**Risk.** MED

### S08-R03 — Two read-only date fields
**Rule.** Two dates are display-only.
**Risk.** LOW

### S08-R04 — Negative amounts are rejected
**Rule.** `Enter a valid amount (not negative)`.
**Risk.** MED

### S08-R05 — Save endpoint
**Rule.** `InsertBVData_PV`.
**Open.** Payload not captured.
**Risk.** HIGH

### S08-R06 — Loads via `GetSaveBankVerData`
**Rule.** Saved bank verification data is fetched with `GetSaveBankVerData` — shared with
[S09](S09-bank-physical-verification.md).
**Risk.** MED

### S08-R07 — CGTMSE toggle is sent in the payload
**Rule.** The CGTMSE toggle is included in the save payload.
**Source.** commit `0e11b82`
**Why recorded.** It was once omitted. A port rebuilding the payload from the visible form may
drop it again.
**Risk.** HIGH

### S08-R08 — Success message
**Rule.** `Bank verification data saved successfully`.
**Risk.** LOW

### S08-R09 — Errors are developer-facing
**Rule.** `Bank verification network error: {ex}`, `Bank verification save error: {ex}`,
`Empty response from server`.
**See.** [S02-R05](S02-verification-type.md) — same pattern, same recommendation.
**Risk.** MED

### S08-R10 — Completion is independent of unit verification
**See.** [S07-R05](S07-applicant-details.md)
**Risk.** MED

### S08-R11 — Bank sessions carry no token
**See.** [C01-R17](../cross-cutting/C01-session-and-auth.md)
**Risk.** MED

### S08-R12 — Standard header
**Rule.** Uses `PageHeaderStyle`.
**Note.** Header **not yet verified on device** — see [C07 §6](../cross-cutting/C07-header-and-theme.md).
**Risk.** LOW

---

## 3. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Type letters into an amount field | refused — allowlist (R01) |
| 2 | Paste `<script>` into an amount field | refused |
| 3 | Type a negative amount | `Enter a valid amount (not negative)` |
| 4 | Save, inspect the payload | **CGTMSE toggle present** (R07) |
| 5 | Save, reload | all 12 fields round-trip |
| 6 | Header | matches every other page (R12) |

---

## 4. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 12 rules traced |
| R2 | — | pending — no bank credentials used this session by policy |
| R3 | — | pending |
