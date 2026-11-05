# UV06 — Stage 6: Product & Sales

| | |
|---|---|
| **MAUI source** | builder `:3263-3987`, save `:9345-9478`, validation `ValidateProductAndSalesStep` |
| **Java target** | `ui/unitverification/stages/Stage06ProductSalesFragment` + `ProductSalesValidator` |
| **Review state** | **R1 PASSED** |
| **Rules** | 14 (5 HIGH) |

---

## 1. Fields

| Label | Type | Max | Hint | API field |
|---|---|:--:|:--:|---|
| Annual Production-Value* | decimal | — | `0.00` | `AnlProdVal` |
| Annual Sales-Value* | decimal | — | `0.00` | `AnlSaleVal` |
| Product Details - Main Product* | dropdown | — | — | `ProdDetails` |
| Export Detail - Country of Export | text | — | — | `ExportDetCount` |
| Export Detail - value | decimal | — | `0.00` | `ExportDetails` |

---

## 2. Validation

### UV06-R01 — Annual Production must be at least 1 ⚠️
**Rule.** The value must be **≥ 1**. Below that: `Amount should be at least 1`, shown **under
the field**.
**Source.** `ValidateProductAndSalesStep` — the test is `< 1`, changed from `<= 0`
**Why.** `0.0001` was being accepted as a valid annual production value.
**Risk.** HIGH

### UV06-R02 — Annual Sales must be at least 1
**Rule.** Same rule, same message.
**Risk.** HIGH

### UV06-R03 — The minimum is checked **on typing**, not on save ⚠️
**Rule.** The error appears as the officer types, not when SAVE & NEXT is pressed.
**Source.** `:3973-3990`
**Implementation note.** The minimum handler is registered **after** the clear-on-type handler
so it runs last and wins — see [C05-R13](../cross-cutting/C05-form-field-behaviour.md). If the
order is reversed, the clear wipes the error and `0.0001` looks valid again.
**Risk.** HIGH

### UV06-R04 — Blank does not flag
**Rule.** An empty field raises nothing while the officer is still typing.
**See.** [C05-R14](../cross-cutting/C05-form-field-behaviour.md)
**Risk.** MED

### UV06-R05 — Main Product is required
**Rule.** `Product Details - Main Product` must be selected.
**Risk.** MED

### UV06-R06 — Export fields are optional
**Rule.** Neither export field is starred and neither is required.
**Risk.** LOW

### UV06-R07 — Forward navigation is guarded ⚠️
**Rule.** Moving past stage 6 while invalid: clear all field errors, show the validation modal
listing the missing labels, mark each invalid field, then scroll to and focus the first.
**Source.** `:757-774`
**Note.** This guard does **not** test `isEditable` — stages 7 and 8 do. See
[F-007](../README.md).
**Risk.** HIGH

### UV06-R08 — Stale errors are pruned on rebuild
**Rule.** Owned keys: `annualproductionvalue`, `annualsalesvalue`, `productdetailsmainproduct`.
**See.** [C05-R19](../cross-cutting/C05-form-field-behaviour.md)
**Risk.** HIGH

---

## 3. Save

### UV06-R09 — Payload
**Rule.** `POST {API_BASE}/Insert_UVData_PV`

```json
{
  "ApplID": 12345,
  "AnlProdVal": 500000,
  "AnlSaleVal": 450000,
  "ProdDetails": "<activity code>",
  "ExportDetails": 0,
  "ExportDetCount": ""
}
```

**Source.** `:9400-9412`
**Note the typing.** `AnlProdVal`, `AnlSaleVal` and `ExportDetails` are JSON **numbers**;
`ProdDetails` and `ExportDetCount` are **strings**. Different from Stage 7, where the counts are
strings. See [C02-R14](../cross-cutting/C02-api-contracts.md).
**Risk.** HIGH

### UV06-R10 — Empty amounts become the number 0
**Rule.** An empty amount is sent as `0` (number), not `""` and not null.
**Source.** `:9403-9405`
**Risk.** MED

### UV06-R11 — Main Product sends an activity code, not the label ⚠️
**Rule.** The payload carries the **activity code**, resolved from a product→activity map, not
the text the officer selected.
**Source.** `:9385-9395`
**Risk.** HIGH

### UV06-R12 — Fallback when no product is selected
**Rule.** If no activity code resolves, the **first entry** in the product→activity map is used.
**Source.** `:9395`
**Note.** A silent fallback that submits an arbitrary activity code. Carry it to stay
compatible, but flag it — it looks like a defect.
**Risk.** MED

---

## 4. Other

### UV06-R13 — Amount-in-words under each amount
**See.** [UV07-R03](UV-07-employment-detail.md)
**Risk.** LOW

### UV06-R14 — Stage 10 re-validates this stage
**Rule.** The Final Submit prerequisite check re-tests Annual Production, Annual Sales and
Product Details, reading them through the saved-value accessor.
**Source.** `:2031-2047`
**Risk.** HIGH

---

## 5. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Type `0.0001` in Annual Production | error **as you type** (R01/R03) |
| 2 | Correct to `1` | clears on the same keystroke |
| 3 | Type `0.5` | still rejected — the bound is 1, not 0 |
| 4 | Leave Production blank, tap step 9 | blocked, modal, first field focused (R07) |
| 5 | Select a product, save, inspect the payload | **activity code, not label** (R11) |
| 6 | Save with no product selected | which code is sent? (R12) |
| 7 | Flag Production, then fix it and rebuild the stage | error does not return (R08) |
| 8 | **Defunct case, stage 6, tap step 7** | does the guard fire? (F-007) |

---

## 6. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 14 rules traced |
| R2 | — | pending |
| R3 | — | pending |
