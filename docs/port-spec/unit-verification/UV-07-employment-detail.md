# UV07 — Unit Verification, Stage 7: Employment Detail

| | |
|---|---|
| **MAUI source** | `UnitVerificationPage.xaml.cs` — builder `LoadEmploymentContent()` @3988, save `SaveEmploymentData()` @9479, guard `ValidateEmploymentDetail()` @9562 |
| **Java target** | `ui/unitverification/stages/Stage07EmploymentFragment` + `validation/EmploymentValidator` |
| **Depends on** | [C05 form fields](../cross-cutting/C05-form-field-behaviour.md), [C06 navigation](../cross-cutting/C06-navigation-and-back.md), [C09 read-only](../cross-cutting/C09-case-status-and-readonly.md) |
| **Feeds** | [UV10 final submit](UV-10-summary-otp.md) — Stage 10 re-validates this stage |
| **Review state** | **R0** — written, not yet traced |
| **Rules** | 22 (6 HIGH) |

---

## 1. Purpose

Records the unit's workforce across three independent groupings, plus average monthly
wages. The three groupings must each total the same number — that number *is* the unit's
employee count.

---

## 2. Screen layout

Top to bottom:

```
Section title: "Employment Detail"

GENDER*                              ← group heading, red when the group fails
  Male                   [   ]
  Female                 [   ]
  Transgender            [   ]

SOCIAL CATEGORY*
  General                [   ]
  SC                     [   ]
  ST                     [   ]
  OBC                    [   ]
  Minority               [   ]

EMPLOYMENT TYPE*
  Full-Time Employees    [   ]
  Part-Time Employees    [   ]
  Seasonal Employees     [   ]

Total Number Of Employees*           ← always read-only, auto-calculated
Average Wages paid per employee per month*
  <amount in words>                  ← italic grey, hidden when blank or zero

[ PREVIOUS ]  [ SAVE & NEXT ]
```

---

## 3. Fields

| Label (exact) | Type | Max | Hint | Editable | API field |
|---|---|:--:|:--:|:--:|---|
| Male | integer | 3 | `0` | yes | `EmpMale` |
| Female | integer | 3 | `0` | yes | `EmpFemale` |
| Transgender | integer | 3 | `0` | yes | `EmpTransgender` |
| General | integer | 3 | `0` | yes | `EmpGEN` |
| SC | integer | 3 | `0` | yes | `EmpSC` |
| ST | integer | 3 | `0` | yes | `EmpST` |
| OBC | integer | 3 | `0` | yes | `EmpOBC` |
| Minority | integer | 3 | `0` | yes | `EmpMinority` |
| Full-Time Employees | integer | 3 | `0` | yes | `FullTime_Emp` |
| Part-Time Employees | integer | 3 | `0` | yes | `PartTime_Emp` |
| Seasonal Employees | integer | 3 | `0` | yes | `Seasonal_Emp` |
| Total Number Of Employees* | integer | — | `0` | **never** | `TotalEMP` |
| Average Wages paid per employee per month* | decimal | 7 | `0.00` | yes | `AvgWgPaidPerMonth` |

The label text is the field's identity — it is normalised and used as the lookup key.
Changing a label in the Java app without changing the lookup breaks save and validation
silently. Keep the labels verbatim.

---

## 4. Derived values

### UV07-R01 — Total is the gender sum, never typed
**Rule.** `Total Number Of Employees` equals `Male + Female + Transgender`. It is read-only
in every state, and recomputes on every keystroke in those three fields — not on focus loss,
not on save.
**Source.** `UnitVerificationPage.xaml.cs:4037`
**Why.** The three groupings are three *views* of the same workforce. Gender is treated as
the authoritative one.
**Test.** Type 2 / 3 / 0 → Total shows `5` immediately. Attempt to focus Total → refused.
**Risk.** MED

### UV07-R02 — Total shows its hint when the gender sum is zero
**Rule.** When the gender sum is 0, Total is **empty** (showing the grey `0` hint), not the
character `0`.
**Source.** `UnitVerificationPage.xaml.cs:4040` — `genderTotal > 0 ? genderTotal.ToString() : ""`
**Why.** A literal `0` is indistinguishable from a real value and would be submitted as one.
See [C05](../cross-cutting/C05-form-field-behaviour.md) on hint-versus-value.
**Test.** Open an unfilled record → Total is grey. Enter then clear all three gender fields →
Total returns to grey, not black `0`.
**Risk.** MED

### UV07-R03 — Wages renders as words live
**Rule.** Below the wages field, an italic grey label shows the amount in Indian words
(lakh/crore), updating on every keystroke. Hidden when the field is blank, unparseable,
or ≤ 0.
**Source.** `UnitVerificationPage.xaml.cs:4190-4214`
**Test.** Type `125000` → "One Lakh Twenty Five Thousand". Clear → label disappears.
**Risk.** LOW

---

## 5. Validation — live

These run on every keystroke in any field on this stage, and once when the stage opens.

### UV07-R04 — SAVE & NEXT is disabled until the stage is valid
**Rule.** The button starts disabled at 60% opacity and only enables when every rule in
this section passes. On a read-only case the button reads `NEXT` and is always enabled.
**Source.** `UnitVerificationPage.xaml.cs:4240-4245`
**Risk.** HIGH

### UV07-R05 — Validation reads the live control, never the saved state
**Rule.** Every live check reads the on-screen field text, not the cached form state.
**Source.** `UnitVerificationPage.xaml.cs:4019-4021` and its comment
**Why.** The cached accessor falls back to the previously saved value when a field is
cleared. Reading it would let a just-emptied field still satisfy validation and leave
SAVE & NEXT wrongly enabled.
**Test.** Fill the stage until SAVE & NEXT enables, then clear `Male` → button disables
immediately.
**Risk.** HIGH

### UV07-R06 — Gender total must be greater than zero
**Rule.** `Male + Female + Transgender > 0`, else the **GENDER** group heading turns red and
shows: `Enter employees in this section — Total Number Of Employees cannot be 0.`
**Source.** `UnitVerificationPage.xaml.cs:4071`
**Why.** The message used to sit under the read-only Total field, where the officer could
not act on it. It belongs on the group the officer must fix.
**Risk.** MED

### UV07-R07 — Social Category total must equal the gender total
**Rule.** When the gender total > 0 and the category sum differs, the **SOCIAL CATEGORY**
heading turns red and shows:
`Sum of Social Category must be equal to Total Number Of Employees (N). Currently M.`
where `N` is the gender total and `M` the category sum.
**Source.** `UnitVerificationPage.xaml.cs:4075-4077`
**Test.** Gender 5, category 0 → the message with `(5)` and `0`.
**Risk.** HIGH

### UV07-R08 — Employment Type total must equal the gender total
**Rule.** Same as R07 for the **EMPLOYMENT TYPE** group:
`Sum of Employment Type must be equal to Total Number Of Employees (N). Currently M.`
**Source.** `UnitVerificationPage.xaml.cs:4079-4081`
**Risk.** HIGH

### UV07-R09 — A blank count becomes `0` when the officer leaves the field
**Rule.** When an employee-count field loses focus while blank, it is set to `0` and
validation re-runs. Applies to all eleven counts, not the Total, not wages.
**Source.** `UnitVerificationPage.xaml.cs:4135-4142`
**Why.** **This is the rule most likely to be dropped in the port.** A blank count is
ambiguous: this stage reads the live control and sees blank as 0, while Stage 10 reads the
saved-value accessor, which treats empty as "not set" and falls back to the stale API
figure. The two then disagree and Stage 10 reports *"Stage 7 details are not filled"* on a
stage the officer just completed. Normalising on focus loss removes the ambiguous state so
both validators always see the same number.
**Test.** Type `3` in `SC`, clear it, tap another field → `SC` reads `0`, black, not a grey
hint. Then open Stage 10 → it must not list Employment Detail as missing.
**Risk.** HIGH

### UV07-R10 — Wages must be at least 1
**Rule.** `Average Wages paid per employee per month` must parse to ≥ 1. Below that, the
field border and caption turn red and the message appears beneath the field:
`Average wages paid per employee per month amount must be greater than 0`
**Source.** `UnitVerificationPage.xaml.cs:4058-4062`
**Note.** The threshold is `< 1`, so `0.5` is rejected although it is greater than zero.
The message says "greater than 0". **Wording and rule disagree.** Carry the rule, and
raise the wording with the tester — do not silently change either.
**Risk.** MED

### UV07-R11 — Group errors appear on the group, field errors under the field
**Rule.** A rule spanning a whole group (R06, R07, R08) reports on that group's heading line
and nowhere else, and only the failing group turns red. A rule about one field (R10) reports
under that field with the field's border and caption reddened. The shared summary label at
the bottom of the stage renders nothing.
**Source.** `UnitVerificationPage.xaml.cs:4064-4089`
**Risk.** MED

### UV07-R12 — Validation runs twice at stage open
**Rule.** Validation runs once immediately after the form is attached, and again on the next
UI pass.
**Source.** `UnitVerificationPage.xaml.cs:4258-4262`
**Why.** The first call happens while the form is still detached, so the error-marking
cannot find the controls to outline and the messages never appeared until the officer typed
something.
**Note for Java.** This is a MAUI layout-timing workaround. The equivalent is running
validation in `onViewCreated` *after* the views are attached. Reproduce the *effect* —
errors visible on open — not the double call.
**Risk.** LOW

---

## 6. Save

### UV07-R13 — Save then advance
**Rule.** `SAVE & NEXT` saves, then navigates to Stage 8. On a read-only case it navigates
without saving. `PREVIOUS` always returns to Stage 6 without saving.
**Source.** `UnitVerificationPage.xaml.cs:4230-4238`
**Risk.** MED

### UV07-R14 — Endpoint and payload
**Rule.** `POST {API_BASE}/Insert_UVData_PV`

```json
{
  "ApplID": 12345,
  "EmpMale": "2", "EmpFemale": "3", "EmpTransgender": "0",
  "EmpGEN": "5", "EmpSC": "0", "EmpST": "0", "EmpOBC": "0", "EmpMinority": "0",
  "FullTime_Emp": "5", "PartTime_Emp": "0", "Seasonal_Emp": "0",
  "TotalEMP": 5,
  "AvgWgPaidPerMonth": "12000"
}
```

`ApplID` and `TotalEMP` are **numbers**; every other value is a **string**. A missing field
is sent as the string `"0"`; a blank Total is sent as the number `0`.
**Source.** `UnitVerificationPage.xaml.cs:9500-9516`
**Note for Java.** Retrofit with Gson/Moshi will serialise an `int` field as a number. The
string-typed counts must be declared `String` in the DTO or the backend receives a
different JSON shape. **Verify against a captured MAUI request before trusting this table.**
**Risk.** HIGH

### UV07-R15 — Blocking loader during save
**Rule.** A full-screen loader reading `Saving details...` covers the form for the duration
of the call, and is removed even when the call throws — before any error dialog, so the
dialog is never stacked on the scrim.
**Source.** `UnitVerificationPage.xaml.cs:9525-9535`
**Why.** The save is a network round-trip with no other feedback; without the loader the
officer taps Save again thinking nothing happened.
**Risk.** MED

### UV07-R16 — Local model updated after save
**Rule.** On success the in-memory record is updated with the saved values, so later stages
read the new numbers without a refetch.
**Source.** `UnitVerificationPage.xaml.cs:9539+`
**Risk.** MED

### UV07-R17 — Save failure message
**Rule.** On exception: alert titled `Error`, body `Failed to save Employment`, button `OK`.
The stage stays as it was; nothing is cleared and no navigation occurs.
**Source.** `UnitVerificationPage.xaml.cs:9555`
**Risk.** MED

---

## 7. The navigation guard

Separate from live validation. Called when the officer tries to *leave* this stage —
by the step indicator or from Stage 10's prerequisite check.

### UV07-R18 — The guard re-reads live controls
**Rule.** `ValidateEmploymentDetail()` recomputes the three totals and the wages value from
the live controls and returns a pass/fail plus a message.
**Source.** `UnitVerificationPage.xaml.cs:9562-9593`
**Risk.** HIGH

### UV07-R19 — An untouched stage passes the guard
**Rule.** When all three totals are 0 **and** wages is blank, the guard returns *valid*.
**Source.** `UnitVerificationPage.xaml.cs:9578-9580`
**Why.** Stage 7's controls exist only while the stage is on screen. If they are absent,
every total reads 0 and the guard cannot tell "never opened" from "opened and left empty".
Blocking would trap the officer on a stage they never visited. The genuinely-unfilled case
is caught by Stage 10's prerequisite check, which reads the saved record instead.
**Note for Java.** Fragment lifecycle differs — views may be absent for different reasons.
Reproduce the *intent*: never block on a stage the officer has not opened.
**Risk.** HIGH

### UV07-R20 — Guard dialog wording
**Rule.** The dialog is titled `Employment Detail Incomplete`, with one line per failing
rule separated by a blank line, and a single `OK` button:

| Rule | Line |
|---|---|
| gender total = 0 | `Gender: enter employees — Total Number Of Employees cannot be 0.` |
| category ≠ gender | `Social Category: sum must be equal to Total Number Of Employees (N). Currently M.` |
| type ≠ gender | `Employment Type: sum must be equal to Total Number Of Employees (N). Currently M.` |
| wages < 1 | `Average Wages: amount paid per employee per month must be greater than 0.` |

**Source.** `UnitVerificationPage.xaml.cs:9583-9594`, dialog at `:782`
**Why.** Each line names the on-screen group heading it refers to. The earlier wording
("Category-wise total must equal Gender-wise total") named nothing the officer could see —
there is no field called "Gender-wise total"; the thing it meant is labelled
**Total Number Of Employees**.
**Risk.** LOW

### UV07-R21 — Forward navigation is blocked, not just the Save button
**Rule.** While this stage is invalid and the case is editable, the step indicator cannot be
used to jump to any stage after 7. Stages 6 and 8 have the same guard.
**Source.** `UnitVerificationPage.xaml.cs:777`
**Why.** Disabling SAVE & NEXT alone left the step indicator as an open route past an
invalid stage, which is how Stage 10 ended up reporting "Stage 7 details are not filled".
**Test.** Leave the stage invalid, tap step 9 in the indicator → refused, dialog shown.
**Risk.** HIGH

---

## 8. Read-only behaviour

### UV07-R22 — Concluded cases are view-only
**Rule.** On a Defunct or Non-Traceable case, every field is read-only, no validation runs,
the button reads `NEXT`, and the stage is purely a viewer. See
[C09](../cross-cutting/C09-case-status-and-readonly.md).
**Source.** `UnitVerificationPage.xaml.cs:396`, `:4271` (`_isConcludedCase` in the field builder)
**Risk.** MED

---

## 9. Open questions

| # | Question | Blocks |
|---|---|---|
| Q1 | R10 rejects `0.5` but says "greater than 0". Which is correct? | wording only, not the rule |
| Q2 | Is a maximum wage enforced server-side? Client caps the field at 7 digits. | C02 |
| Q3 | What does the backend do when the three totals disagree? Is client validation the only guard? | R07, R08 |

---

## 10. Review log

| Pass | Date | By | Result |
|---|---|---|---|
| R1 | — | — | not started |
| R2 | — | — | not started |
| R3 | — | — | not started |
