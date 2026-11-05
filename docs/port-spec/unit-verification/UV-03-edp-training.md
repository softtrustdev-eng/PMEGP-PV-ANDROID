# UV03 — Stage 3: EDP Training Detail

| | |
|---|---|
| **MAUI source** | builder `UnitVerificationPage.xaml.cs:2972-3072`, save `:9054-9117` |
| **Java target** | `ui/unitverification/stages/Stage03EdpTrainingFragment` |
| **Review state** | **R1 PASSED** |
| **Rules** | 7 (2 HIGH) |

---

## 1. Fields

| Label | Type | API field |
|---|---|---|
| EDP Completed* | date | `TrDateTo` |
| EDP Training Period* | text | `EDPPeriod` |

Plus three values carried from the record and re-sent unchanged: `NoOfTrDays`, `EDPTCName`
(institute name), `EDPAddress` (institute address).

---

## 2. Rules

### UV03-R01 — Endpoint and payload
**Rule.** `POST {API_BASE}/Insert_UVData_PV`

```json
{
  "ApplID": 12345,
  "EDPPeriod": "...",
  "NoOfTrDays": "...",
  "TrDateTo": "dd/MM/yyyy",
  "EDPTCName": "...",
  "EDPAddress": "..."
}
```

**Source.** `:9090-9105`
**Risk.** HIGH

### UV03-R02 — Three fields are echoed, not entered ⚠️
**Rule.** `NoOfTrDays`, `EDPTCName` and `EDPAddress` are read from the loaded record and sent
back unchanged. The officer never sees or edits them on this stage.
**Source.** `:9054-9060`
**Why it matters.** They are part of the payload but not part of the form. A Java DTO built
only from the visible fields would send them as null and **blank them server-side**. This is
[C02-R08](../cross-cutting/C02-api-contracts.md) in miniature.
**Risk.** HIGH

### UV03-R03 — Missing carried values become empty string
**Rule.** A null carried value is sent as `""`, not omitted and not null.
**Source.** `:9056`
**Risk.** MED

### UV03-R04 — Dates are converted to API format before sending
**Rule.** Date fields are converted from the display format to the API format on save.
**See.** [UV02-R08](UV-02-unit-detail.md)
**Risk.** MED

### UV03-R05 — Read-only on concluded cases
**See.** [C09](../cross-cutting/C09-case-status-and-readonly.md)
**Risk.** MED

### UV03-R06 — No forward guard
**Rule.** Stage 3 has no navigation guard; the officer may move forward with it incomplete.
Only stages 6, 7 and 8 guard.
**See.** [C06-R08](../cross-cutting/C06-navigation-and-back.md)
**Note.** Stage 10's prerequisite check does **not** list Stage 3 either — so EDP data can be
left blank through to Final Submit. **Confirm with the tester whether this is intended.**
**Risk.** MED

### UV03-R07 — Both fields are starred but not enforced
**Rule.** Both captions carry `*`, implying mandatory, but nothing blocks on them.
**Open question.** Is the asterisk wrong, or is the validation missing?
**Risk.** MED

---

## 3. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Save stage 3, reload the record | **`EDPTCName` / `EDPAddress` unchanged** (R02) |
| 2 | Leave both fields blank, advance | allowed (R06) — confirm intended |
| 3 | Reach stage 10 with stage 3 blank | not listed as missing — confirm intended |
| 4 | Enter a date, save, reopen | round-trips in the same format |

Step 1 is the one that catches a wrong port.

---

## 4. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 7 rules traced |
| R2 | — | pending |
| R3 | — | pending |
