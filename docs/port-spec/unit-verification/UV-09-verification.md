# UV09 — Stage 9: Verification Details

| | |
|---|---|
| **MAUI source** | builder `:5724-6362`, save `:9620-9700` |
| **Java target** | `ui/unitverification/stages/Stage09VerificationFragment` |
| **Review state** | **R1 PASSED** |
| **Rules** | 11 (4 HIGH) |

---

## 1. Fields

| Label | Type | API field |
|---|---|---|
| Verification Status* | dropdown — `Completed` / `Pending` | `VerStatus` |
| Verification Date* | date | `VerDate` |
| Verification Agency Name* | text | `VerAgencyName` |
| Enumerator Remark* | multiline | `EnumRem` |
| Prominent Sign Board installed | checkbox | `IsSignBoardIns` |

---

## 2. Two different status fields ⚠️

### UV09-R01 — `VeriStatus` and `VerStatus` are not the same thing
**Rule.** The payload carries **both**:

| Field | Holds | Values |
|---|---|---|
| `VeriStatus` | the **unit's** condition | `WR` / `DF` / `NT` |
| `VerStatus` | the **verification's** progress | `Completed` / `Pending` |

**Source.** `:9655-9660`
**Why it matters.** One letter apart, different meanings, both in the same JSON object. Swapping
them corrupts the record in a way no client-side check would catch.
**Java.** Name them unmistakably in the DTO — `unitCondition` and `verificationProgress` — with
`@SerializedName` carrying the wire names.
**Risk.** HIGH

### UV09-R02 — The dropdown maps to a two-letter code
**Rule.** `Working` → `WR`, `Pending` → `PD`; otherwise the derived unit status code.
**Source.** `:9640-9650`
**Note.** `PD` appears **only** here. The code table in
[C09-R05](../cross-cutting/C09-case-status-and-readonly.md) lists `WR`/`DF`/`NT` — `PD` is a
fourth. **Confirm from the capture** whether the server accepts it in `VeriStatus`.
**Risk.** HIGH

---

## 3. Rules

### UV09-R03 — Checkbox sends 1 or 0, not a boolean
**Rule.** `IsSignBoardIns` is the **integer** `1` or `0`.
**Source.** `:9661`
**Java.** `int`, not `boolean`. Gson would serialise a boolean as `true`/`false`.
**Risk.** MED

### UV09-R04 — Payload
```json
{
  "ApplID": 12345,
  "VeriStatus": "WR",
  "VerStatus": "Completed",
  "VerDate": "dd/MM/yyyy",
  "VerAgencyName": "...",
  "EnumRem": "...",
  "IsSignBoardIns": 1
}
```
**Source.** `:9654-9662`
**Risk.** HIGH

### UV09-R05 — Stage 10 requires Completed **and** a remark ⚠️
**Rule.** Final Submit requires `Verification Status == "Completed"` **and** a non-empty
Enumerator Remark. Either missing blocks submission.
**Source.** `:2077-2085`
**Risk.** HIGH

### UV09-R06 — Stage 9 values are resolved at Stage 9 render time
**Rule.** These values do not exist in the loaded record until the server returns them, so they
are resolved when Stage 9 is built.
**Source.** `:535-537`
**Why.** An earlier version resolved them later, which left **Stage 10 blank until Stage 9 had
been visited**.
**Java.** Resolve into the ViewModel on load so Stage 10 never depends on Stage 9 having been
opened.
**Risk.** HIGH

### UV09-R07 — Enumerator Remark is sanitised, newlines allowed
**See.** [C03-R11](../cross-cutting/C03-input-sanitisation.md)
**Risk.** MED

### UV09-R08 — Date conversion on save
**See.** [UV02-R16](UV-02-unit-detail.md)
**Risk.** MED

### UV09-R09 — No forward guard on stage 9
**Rule.** Stage 9 does not block forward navigation. Stage 10's prerequisite check catches it.
**See.** [C06-R08](../cross-cutting/C06-navigation-and-back.md)
**Risk.** MED

### UV09-R10 — Read-only on concluded cases
**See.** [C09](../cross-cutting/C09-case-status-and-readonly.md)
**Risk.** MED

### UV09-R11 — The sign-board checkbox is optional
**Rule.** Not starred, not required. Unchecked sends `0`.
**Risk.** LOW

---

## 4. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Set status Completed, fill remark, save, inspect payload | `VerStatus: "Completed"`, `VeriStatus` separate (R01) |
| 2 | Set Pending, save | `VeriStatus: "PD"` — does the server accept it? (R02) |
| 3 | Tick the sign board, save | `"IsSignBoardIns": 1`, not `true` (R03) |
| 4 | **Open a fresh record, go straight to stage 10 without visiting 9** | stage 10 is **populated** (R06) |
| 5 | Status Pending, try Final Submit | blocked (R05) |
| 6 | Status Completed, remark blank, try Final Submit | blocked (R05) |

Step 4 is the regression this stage's design exists to prevent.

---

## 5. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 11 rules traced |
| R2 | — | pending |
| R3 | — | pending |
