# C09 — Case status & read-only

| | |
|---|---|
| **MAUI source** | `UnitVerificationPage.xaml.cs:333-340, 390-400, 430-452, 2445-2455`; `DetailsPage.xaml.cs:543-546, 1454-1480` |
| **Java target** | `data/model/CaseStatus`, `ui/unitverification/UnitVerificationViewModel` |
| **Review state** | **R1 PASSED** |
| **Rules** | 15 (6 HIGH) |

---

## 1. Two independent flags

The MAUI app carries **two** booleans, deliberately not one.

| Flag | Meaning | Overridable |
|---|---|---|
| `_isEditable` | this record is open for editing | yes — individual fields pass `forceEditable: true` |
| `_isConcludedCase` | Defunct / Non-Traceable: view-only, no exceptions | **no** — outranks everything |

### C09-R01 — Why they are separate ⚠️
**Rule.** `_isConcludedCase` is tracked apart from `_isEditable` because several Stage 2 fields
pass `forceEditable: true`, which deliberately overrides `_isEditable` on a Completed case. A
concluded case must **not** be overridable that way.
**Source.** `UnitVerificationPage.xaml.cs:335-339`
**Java.** Do not collapse into one `boolean editable`. Model as:

```java
boolean fieldEditable(boolean forceEditable) {
    if (concluded) return false;              // outranks everything
    return recordEditable || forceEditable;
}
```

One method, used by every field builder. Collapsing the two is the most likely simplification
a porting developer will make, and it unlocks every field on a Defunct case.
**Risk.** HIGH

### C09-R02 — How the field builders combine them
**Rule.** A field is read-only when:
`forceReadOnly || isConcludedCase || !(isEditable || forceEditable)`
**Source.** `:4274`
**Risk.** HIGH

---

## 2. Determining the status

### C09-R03 — Initial values, from the constructor
**Rule.** At construction:
- `concluded = detailsPageStatus is "DefunctApplications" or "NonTraceable"`
- `editable = badgeStatus is not "Completed" (case-insensitive) && !concluded`
- `badgeStatus` defaults to `"Completed"` when null — i.e. **not editable**

**Source.** `:391-397`
**Note.** The default is the safe one: unknown status means read-only.
**Risk.** MED

### C09-R04 — The server's `VeriStatus` is authoritative and overrides the constructor ⚠️⚠️
**Rule.** After the record loads, if `VeriStatus` is `"DF"` or `"NT"`, both flags are forced:
`concluded = true`, `editable = false` — regardless of what the constructor decided.
**Source.** `:446-452`
**Why — this is a fixed defect.** Whether a case is concluded is a property of the **record**,
not of the screen the officer arrived from. The constructor can only read `detailsPageStatus`,
and `ApplicantDetailsPage` hardcodes it to `"Pending"` — it is handed only an `applId` and has
no idea the case is DF/NT. So reaching a Defunct or Non-Traceable case that way **unlocked
every field**. The save is rejected server-side, but the officer only discovers that after
typing the whole form.

`VeriStatus` comes from the server and is correct however the page was opened: from the DF/NT
cards, from Applicant Details, or from a resumed session.

**Java.** Derive the status in the ViewModel **after** the record loads, and expose it as
observable state. Do not pass editability in as a constructor argument — that is the shape
that caused this. A navigation argument may *hint* at it; only the loaded record decides.
**Risk.** HIGH

### C09-R05 — Status codes
**Rule.**

| `VeriStatus` | Display | Editable |
|---|---|---|
| `WR` | Working | yes, if not Completed |
| `DF` | Defunct | **never** |
| `NT` | Non-Traceable | **never** |
| anything else | `""` | per badge status |

**Source.** `:2447-2453`
**Risk.** MED

### C09-R06 — Badge status is trimmed
**Rule.** The badge status is trimmed before comparison, and compared case-insensitively.
**Source.** `:391`, `:395`
**Java.** `"Completed".equalsIgnoreCase(badge.trim())`. Use `Locale.ROOT` — Turkish locale
lowercases `I` to `ı` and breaks the comparison on a Turkish-locale device.
**Risk.** MED

---

## 3. What read-only changes

### C09-R07 — Every stage opens, read-only
**Rule.** A concluded case can navigate all ten stages. Nothing is hidden; everything is
locked.
**Source.** `:395-396`
**Why.** The officer still needs to *see* the record.
**Risk.** MED

### C09-R08 — Buttons relabel
**Rule.** `SAVE & NEXT` becomes `NEXT` and is always enabled; no save occurs on advance.
**Source.** `:4226`, `:4236-4237`
**Risk.** MED

### C09-R09 — Live validation does not run
**Rule.** The validation routine returns immediately when the case is not editable.
**Source.** `:4013`
**Why.** Red outlines on a record the officer cannot change are noise.
**Risk.** MED

### C09-R10 — Forward guards are skipped
**Rule.** Stage 7 and 8 forward guards test `isEditable` first.
**See.** [C06-R09](C06-navigation-and-back.md) — Stage 6's guard does **not**, which may be an
oversight. Flagged for R2.
**Risk.** MED

### C09-R11 — Read-only fields are never sanitised or rewritten
**Rule.** Non-editable fields are excluded from the sanitiser and from any programmatic text
rewriting.
**Source.** `:7420-7422`
**Why.** Their text is server data being displayed, not officer input.
**See.** [C03-R12](C03-input-sanitisation.md), [C05-R24](C05-form-field-behaviour.md)
**Risk.** MED

### C09-R12 — Read-only styling
**Rule.** Background `#F5F5F5`, text `#79747E`, outline `#BDBDBD`, no shadow.
**See.** [C05-R03](C05-form-field-behaviour.md)
**Risk.** LOW

---

## 4. The applicant lists

### C09-R13 — Concluded lists are view-only
**Rule.** The Defunct and Non-Traceable lists are concluded cases: viewable, never editable.
**Source.** `DetailsPage.xaml.cs:545-546`
**Risk.** MED

### C09-R14 — Non-Traceable rows blink, except in the Non-Traceable list
**Rule.** A Non-Traceable row pulses its card outline to stand out among ordinary rows. The
blink is **suppressed** when the current list is already the Non-Traceable list.
**Source.** `DetailsPage.xaml.cs:1490-1496`
**Why.** In that list every row would blink, which is noise and a measurable performance cost —
each blinking card invalidates continuously. One shared page timer drives every blink rather
than one animation per card.
**Java.** `RecyclerView` + a single `ValueAnimator` on the fragment, applied to bound holders.
Never one animator per `ViewHolder` — they leak on recycle.
**Risk.** MED

### C09-R15 — Traceability status drives the row's action button
**Rule.** The row's middle button renders per status:

| Key | Behaviour |
|---|---|
| `Working` | normal |
| `Defunct` | concluded styling |
| `Non-Tracable` *(sic — spelling as stored)* | concluded styling + blink |
| `Unknown` | tapping opens the remarks modal ([C08 §4](C08-dialogs-and-modals.md)) |

**Source.** `DetailsPage.xaml.cs:1454-1480`, `:1441-1448`
**Note.** The stored value is `"Non-Tracable"`, one `e` short of "Non-Traceable". Carry the
**exact** string — comparisons depend on it. Do not fix the spelling in the Java port without
confirming every comparison site and the server's value.
**Risk.** HIGH — a "helpful" spelling correction silently breaks every status comparison.

---

## 5. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Open a Defunct case from the Defunct card | every stage read-only, `NEXT` not `SAVE & NEXT` |
| 2 | **Open the same case via Applicant Details** | **still read-only** — C09-R04, the defect path |
| 3 | Open a Non-Traceable case, stage 2 | even `forceEditable` fields locked — C09-R01 |
| 4 | Defunct case, navigate all 10 stages | all open, none editable, no red outlines |
| 5 | PV list, a Non-Traceable row | card outline pulses |
| 6 | Non-Traceable list | **no** blinking — C09-R14 |
| 7 | A row with Unknown status, tap it | remarks modal opens |
| 8 | Defunct case, stage 6, tap step 7 | does the guard fire? — C06-R09 |

Step 2 is the one that matters. It is the exact path that produced the defect C09-R04 fixes.

---

## 6. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 15 rules traced |
| R2 | — | pending — **no concluded case was available during this session** |
| R3 | — | pending |
