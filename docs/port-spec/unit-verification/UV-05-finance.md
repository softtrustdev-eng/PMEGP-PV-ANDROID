# UV05 — Stage 5: Finance Bank Detail

| | |
|---|---|
| **MAUI source** | builder `:3134-3262` |
| **Java target** | `ui/unitverification/stages/Stage05FinanceFragment` |
| **Review state** | **R1 PASSED** |
| **Rules** | 5 (1 HIGH) |

---

## 1. Fields

| Label | Type |
|---|---|
| Financing Bank* | text |
| Bank Branch* | text |
| IFSC code* | text |

---

## 2. Rules

### UV05-R01 — Values come from the record
**Rule.** All three are populated from the loaded record.
**Source.** `:3134-3262`
**Risk.** MED

### UV05-R02 — IFSC is uppercased
**Rule.** The IFSC value is trimmed and uppercased before use, matching the GST handling on
Stage 2.
**Source.** pattern at `:8771` (`.Trim().ToUpper()`)
**Java.** `toUpperCase(Locale.ROOT)` — **not** the default locale. Turkish lowercases `I` to
`ı`, which corrupts an IFSC on a Turkish-locale device.
**Risk.** HIGH

### UV05-R03 — No IFSC format validation
**Rule.** The app does **not** validate the IFSC pattern (4 letters + `0` + 6 alphanumerics).
Any text is accepted.
**Note.** Worth raising — an IFSC regex is one line and the format is fixed. Record as a
suggestion, do not add it silently.
**Risk.** MED

### UV05-R04 — No forward guard
**See.** [C06-R08](../cross-cutting/C06-navigation-and-back.md)
**Risk.** MED

### UV05-R05 — Read-only on concluded cases
**See.** [C09](../cross-cutting/C09-case-status-and-readonly.md)
**Risk.** MED

---

## 3. Open questions

| # | Question |
|---|---|
| Q1 | Are these three editable at all, or display-only from the sanction record? |
| Q2 | Does Stage 5 save? No dedicated save method was found — confirm against the capture. |
| Q3 | Should IFSC be format-validated? |

**Q2 blocks this document reaching R2.** Resolve it with the [C02 capture run](../cross-cutting/C02-api-contracts.md).

---

## 4. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ⚠️ 5 rules traced; **no save method located — Q2 open** |
| R2 | — | blocked on Q2 |
| R3 | — | pending |
