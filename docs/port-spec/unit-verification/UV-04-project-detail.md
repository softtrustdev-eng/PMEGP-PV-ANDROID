# UV04 — Stage 4: Project Detail

| | |
|---|---|
| **MAUI source** | builder `:3073-3133`, save `:9118-9344` |
| **Java target** | `ui/unitverification/stages/Stage04ProjectDetailFragment` |
| **Review state** | **R1 PASSED** |
| **Rules** | 7 (2 HIGH) |

---

## 1. Fields

| Label | Type | Editable |
|---|---|---|
| Project Sanctioned Date* | date | from record |
| Scheme under which project got sanctioned* | text | from record |
| Capital Expenditure* | amount | yes |
| Working Capital* | amount | yes |
| Own Contribution* | amount | yes |
| **Total*** | amount | **never — derived** |

---

## 2. Rules

### UV04-R01 — Total is the sum of the three amounts
**Rule.** `Total = Capital Expenditure + Working Capital + Own Contribution`, read-only,
recomputed live on every keystroke in the three inputs.
**Source.** `:3073-3133`
**See.** [C05-R10](../cross-cutting/C05-form-field-behaviour.md) — writes empty, not `0`, when
the sum is zero.
**Risk.** MED

### UV04-R02 — Amounts use exact decimal arithmetic
**Rule.** Parsing and summing are decimal, not floating point.
**Java.** `BigDecimal`. Three currency values summed in `double` accumulate visible error.
**See.** [C05-R15](../cross-cutting/C05-form-field-behaviour.md)
**Risk.** HIGH

### UV04-R03 — Amount-in-words below each amount
**Rule.** Each amount field shows its value in Indian words (lakh/crore) beneath it, italic
grey, updating live, hidden when blank or zero.
**See.** [UV07-R03](UV-07-employment-detail.md)
**Java.** Port the number-to-words helper and unit-test it at 1 lakh, 1 crore, and the
99,999 → 1,00,000 boundary. Indian grouping is **not** the Western thousands grouping.
**Risk.** MED

### UV04-R04 — Endpoint
**Rule.** `POST {API_BASE}/Insert_UVData_PV` with a Stage 4 payload.
**Source.** `:9118-9344`
**Risk.** HIGH

### UV04-R05 — Date conversion on save
**See.** [UV02-R08](UV-02-unit-detail.md)
**Risk.** MED

### UV04-R06 — No forward guard
**Rule.** Stage 4 does not block forward navigation.
**See.** [C06-R08](../cross-cutting/C06-navigation-and-back.md)
**Risk.** MED

### UV04-R07 — Read-only on concluded cases
**See.** [C09](../cross-cutting/C09-case-status-and-readonly.md)
**Risk.** MED

---

## 3. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Enter 100000 / 50000 / 25000 | Total shows 175000 immediately |
| 2 | Clear all three | Total shows its grey hint, not `0` |
| 3 | Enter 0.1, 0.2 | Total is exactly 0.3 (R02) |
| 4 | Enter 100000 | words read "One Lakh", not "One Hundred Thousand" |
| 5 | Try to focus Total | refused |

---

## 4. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 7 rules traced |
| R2 | — | pending |
| R3 | — | pending |
