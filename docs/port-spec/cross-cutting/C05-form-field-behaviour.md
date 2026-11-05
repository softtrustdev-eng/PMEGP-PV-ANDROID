# C05 — Form field behaviour

| | |
|---|---|
| **MAUI source** | `UnitVerificationPage.xaml.cs` — builders `:3490-3700` / `:4270-4450` / `:7000-7440`, error helpers `:8507-8630`, key handling `:1385-1437`; `Util/FieldStyling.cs` |
| **Java target** | `ui/common/form/` — `FormFieldFactory`, `FieldErrorView`, `FieldValidator`, `FieldKey` |
| **Review state** | **R1 PASSED** |
| **Rules** | 24 (7 HIGH) |

> This is the highest-leverage document in Pass A. Stages 1–10 and all three verification
> screens are built almost entirely from these components. Get this right and the stages are
> composition; get it wrong and the error is repeated 200 times.

---

## 1. Anatomy of a field

Every field is a container holding four things:

```
┌─ Grid  [ClassId = "field_" + key]  ─────────────────┐
│  Caption          "Annual Production-Value*"        │  ← reddens on error
│  ┌─ Border ───────────────────────────────────────┐ │  ← outline reddens + thickens
│  │  Entry / Editor / Picker        [icon]         │ │
│  └────────────────────────────────────────────────┘ │
│  Error label  [ClassId = "err_" + key]              │  ← hidden until invalid
└─────────────────────────────────────────────────────┘
```

### C05-R01 — The field's identity is its caption, normalised
**Rule.** The lookup key is the caption with `*`, spaces, `:` and `-` removed, then trimmed.
`"Annual Production-Value*"` → `annualproductionvalue`.
**Source.** `UnitVerificationPage.xaml.cs:1385-1389`
**Why it matters for the port.** Save, validation, error marking and state restore **all**
find the control by this derived key. Changing a caption changes the key and silently breaks
all four. In Java, do not derive keys from display text — give each field an explicit `String`
or enum ID and keep the caption as a separate `@string` resource. Record the mapping so the
old keys remain greppable.
**Risk.** HIGH

### C05-R02 — Two parallel ClassId namespaces
**Rule.** The container carries `field_<key>`; the error label carries `err_<key>`. Lookup
walks the view tree matching these.
**Source.** `:8509`, `:8521`
**Java.** `View.setTag()` or a `Map<String, FieldHolder>`. A tree walk per lookup is O(n) and
runs on every keystroke — build the map once at inflate time.
**Risk.** MED

---

## 2. Visual states

### C05-R03 — Editable vs read-only styling
**Rule.**

| | Editable | Read-only |
|---|---|---|
| Background | `#FFFFFF` | `#F5F5F5` |
| Text | `#1C1B1F` | `#79747E` |
| Outline | `#1976D2` | `#BDBDBD` |
| Outline thickness | 1.5 | 1.5 |
| Shadow | yes, `#40000000` @ 0.15, radius 4, offset (0,2) | none |
| Corner radius | 5 | 5 |
| Padding | `(16, 4, 12, 4)` | same |

**Source.** `UnitVerificationPage.xaml.cs:4271-4296`
**Risk.** MED

### C05-R04 — Error styling
**Rule.** On error: outline → `#D32F2F` and thickness → **2**; the caption's text colour →
`#D32F2F`; the error label becomes visible with the message; the input itself is restyled red.
**Source.** `:8513-8538`
**Risk.** MED

### C05-R05 — Only the field's own Border reddens ⚠️
**Rule.** Exactly **one** Border is restyled — the first descendant. Borders nested deeper
inside the field are left alone.
**Source.** `:8512-8517` and its comment
**Why.** The edit and calendar icons have their own Borders, and they were turning red along
with the field.
**Java.** The field layout has one `MaterialCardView`/background owner. Do not iterate all
child backgrounds.
**Risk.** MED

### C05-R06 — The caption's original colour is remembered
**Rule.** Before reddening a caption, its current colour is stored. Clearing the error restores
the stored colour, not a hardcoded default.
**Source.** `:8532-8535`, restore at `:8595`
**Why.** Captions are not all the same colour — group headings, first-field captions and
ordinary captions differ. A hardcoded restore flattens them.
**Risk.** MED

### C05-R07 — Clearing restores 1.5 / `#1976D2`
**Rule.** Clearing an error sets outline back to `#1976D2` at thickness 1.5 and hides the
error label.
**Source.** `:8556-8560`
**Risk.** LOW

---

## 3. Placeholders

### C05-R08 — Zero means "not filled", and shows a hint
**Rule.** A numeric value that is `null` or `0` renders as an **empty** field showing a grey
placeholder — not the character `0`.
**Source.** `UnitVerificationPage.xaml.cs:6503` (`ValueOrHint` ×3 overloads)
**Why.** A literal `0` is indistinguishable from officer input and would be submitted as one.
This is the "values instead of hints" ticket.
**Risk.** HIGH

### C05-R09 — Placeholder colour and texts
**Rule.** Placeholder colour is `#9E9E9E`. Confirmed hint texts:

| Field class | Hint |
|---|---|
| Employee counts | `0` |
| Total (read-only, derived) | `0` |
| Wages / amounts | `0.00` |

**Source.** `:4121`, `:4165`, `:4172`
**Note.** `Total Number Of Employees` previously hinted `Auto-calculated`; changed to `0` so
the numeric column reads uniformly.
**Risk.** LOW

### C05-R10 — A derived field writes empty, not zero
**Rule.** A recomputed read-only total writes `""` when the computed value is 0, so the hint
shows instead of a fabricated `0`.
**Source.** `:4040`
**Risk.** MED

---

## 4. Live validation

### C05-R11 — Errors clear the moment a value arrives
**Rule.** A field's outline and message are removed as soon as a value is entered, not at the
next full validation run.
**Source.** `:8545-8560`
**Risk.** MED

### C05-R12 — Clearing is a no-op unless the field is flagged
**Rule.** The clear path returns immediately when the field has no value, or is not currently
in the flagged set.
**Source.** `:8550-8551`
**Why.** Prevents a keystroke on an already-valid field from doing a tree walk.
**Risk.** LOW

### C05-R13 — Handlers registered later run later ⚠️
**Rule.** Where a field has both a clear-on-type handler and a rule-specific handler (e.g. the
minimum check), the rule handler is registered **after**, so it runs last and wins.
**Source.** `:3973-3990` — the `minimum` block is deliberately placed after the clear handler
**Why.** Both fire on the same keystroke. If the clear handler ran last it would wipe an error
the rule handler had just set, and a `0.0001` entry would look valid.
**Java.** `TextWatcher`s fire in registration order — same constraint, same ordering. Better:
collapse into one watcher that evaluates all rules and sets the final state once.
**Risk.** HIGH

### C05-R14 — A live minimum check
**Rule.** For fields with a minimum, on each keystroke: blank → do nothing; parses and
≥ minimum → clear; otherwise → mark invalid with `Amount should be at least {min}`.
**Source.** `:3973-3990`
**Note.** Blank does nothing deliberately — flagging an empty field the officer is still
typing into is hostile.
**Risk.** MED

### C05-R15 — Parse amounts as decimal, not double
**Rule.** Amount comparisons use exact decimal parsing.
**Java.** `BigDecimal`, **not** `Double.parseDouble`. `0.1 + 0.2 > 0.3` is true in binary
floating point; a `>= 1` boundary check on a value like `0.9999999999` must not pass.
**Risk.** MED

---

## 5. Group errors

### C05-R16 — Group headings carry their own error line
**Rule.** A rule spanning several fields reports on the group heading, which turns red and
shows the message. Only the failing group reddens.
**Source.** `:6839` (`SetGroupError`), used at `:4071-4081`
**Why.** The message used to sit under a read-only derived field where the officer could not
act on it.
**Risk.** MED

### C05-R17 — Field errors and group errors do not mix
**Rule.** A rule about one field reports under that field. A rule spanning a group reports on
the group. The shared summary label at the bottom of a stage renders nothing.
**Source.** `:4064-4089`
**Risk.** MED

---

## 6. Error persistence across rebuilds

### C05-R18 — Flagged fields survive a stage rebuild ⚠️
**Rule.** Flagged field keys are held in a page-level set. After a stage is rebuilt, every key
still in the set is re-marked.
**Source.** `:1397-1437` (`ReapplyFieldErrors`), called from `LoadStepContent` `:1498`
**Why.** Rebuilding a stage from scratch is how `GET LOCATION` and `GENERATE DIGIPIN` deliver
their values. Without re-application the red highlight was wiped from fields that were *still*
empty — Digi PIN above all.
**Risk.** HIGH

### C05-R19 — Stale flags are pruned before repainting ⚠️
**Rule.** Before re-marking, the stage re-runs its own validation and removes keys that are no
longer missing — but **only keys that stage owns**. Keys belonging to other stages survive.

| Stage | Owned keys |
|---|---|
| 2 | `unitname`, `updatedunitaddress`, `unitestablishmentdate`, `udyamregistrationnumber`, `longitude`, `latitude`, `geotaggingid`, `digipin`, `verificationstatus` |
| 6 | `annualproductionvalue`, `annualsalesvalue`, `productdetailsmainproduct` |

**Source.** `:1405-1432`
**Why.** `GET LOCATION` and `GENERATE DIGIPIN` write onto freshly built controls **without
raising a text-change event** — so without pruning, the red came straight back on fields the
officer had just filled. The ownership restriction stops one stage clearing another's errors.
**Java.** Keep the owned-key lists explicit and adjacent to the stage, not in a shared map.
**Risk.** HIGH

---

## 7. Focus and scrolling

### C05-R20 — Scroll the first invalid field into view and focus it
**Rule.** On failed validation, the first invalid field is scrolled to **centre** with
animation, then after ~150 ms the input inside it takes focus.
**Source.** `:8607-8630`
**Why the delay.** Focusing during the scroll animation fights it.
**Why "centre".** An earlier version scrolled the horizontal step strip instead of the form, so
nothing appeared to happen. **Scroll the form container, not the stepper.**
**Java.** `NestedScrollView.smoothScrollTo` then `view.requestFocus()` in a `postDelayed`, or
`requestChildRectangleOnScreen`. Keep the two steps separate.
**Risk.** MED

### C05-R21 — Focus failures are swallowed
**Rule.** Any exception during scroll/focus is logged and ignored. Validation still blocks.
**Source.** `:8625-8628`
**Risk.** LOW

---

## 8. Input constraints

### C05-R22 — Length caps are enforced by the platform
**Rule.** Where a maximum exists it is set on the control itself, so the soft keyboard refuses
the keystroke and a paste is truncated **before** the value is assigned.
**Java.** `android:maxLength` / `InputFilter.LengthFilter`.
**See.** [C03 §5](C03-input-sanitisation.md) for the cap table and the no-global-cap rule.
**Risk.** MED

### C05-R23 — Numeric fields are digits-only by allowlist
**Rule.** Numeric fields permit only digits (and a decimal point where applicable). This is an
**allowlist** and is strictly stronger than the C03 blocklist.
**Source.** numeric builders, and `AllowPositiveNumbersOnly` in `BankVerificationPage`
**Java.** `android:inputType="number"` / `numberDecimal` plus a `DigitsKeyListener`. **Do not
replace an allowlist with the C03 blocklist** — that weakens the field.
**Risk.** MED

### C05-R24 — Read-only fields are never rewritten
**Rule.** Non-editable fields are excluded from sanitising and from any text rewriting.
**Source.** `:7420-7422`
**Why.** Their text is server data being displayed, not officer input.
**Risk.** MED

---

## 9. Java component design

What this document implies for `ui/common/form/`:

```java
FormFieldFactory.text(parent, FieldId.UNIT_NAME, R.string.unit_name, opts)
FormFieldFactory.numeric(parent, FieldId.EMP_MALE, R.string.emp_male,
        opts().maxLength(3).hint("0").editable(isEditable))
FormFieldFactory.decimal(...)  .dropdown(...)  .date(...)  .readOnly(...)
```

- one `FieldHolder` per field: caption, input, error label, original caption colour (C05-R06)
- `FieldErrorController` owns the flagged set, mark/clear, and re-application (C05-R18/R19)
- `FieldId` is an **enum**, never derived from display text (C05-R01)
- one `TextWatcher` per field evaluating all rules in a defined order (C05-R13)
- a `FieldMap` built at inflate time, not a tree walk (C05-R02)

---

## 10. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Submit an empty stage | first invalid field centred and focused (R20) |
| 2 | Type into a flagged field | red clears immediately (R11) |
| 3 | Stage 2: flag Digi PIN, then tap GET LOCATION | Longitude/Latitude clear, **Digi PIN stays red** (R18/R19) |
| 4 | Stage 2: flag fields, tap GENERATE DIGIPIN | Digi PIN clears, others keep their state |
| 5 | Stage 6: enter `0.0001` in Annual Production | error appears **on typing** (R13/R14) |
| 6 | Stage 6: correct it to `1` | error clears on the same keystroke |
| 7 | Open any stage with a date field and flag it | calendar icon does **not** redden (R05) |
| 8 | Flag a group-heading rule | only that group reddens (R16) |

Steps 3 and 4 are the ones that catch a wrong port. Run them first.

---

## 11. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 24 rules traced |
| R2 | — | pending — checklist above |
| R3 | — | pending |
