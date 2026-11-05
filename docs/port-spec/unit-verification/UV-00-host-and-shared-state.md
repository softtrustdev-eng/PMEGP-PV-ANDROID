# UV00 — Host, stepper & shared state

| | |
|---|---|
| **MAUI source** | `UnitVerificationPage.xaml.cs` — construction `:318-500`, state `:835-1130`, stepper `:681-830`, dispatch `:1458-1500` |
| **Java target** | `ui/unitverification/UnitVerificationActivity` + `UnitVerificationViewModel` |
| **Review state** | **R1 PASSED** |
| **Rules** | 18 (7 HIGH) |

> The frame around all ten stages. Read before any `UV01`–`UV10`.

---

## 1. Construction

### UV00-R01 — Inputs
**Rule.** The page is constructed with: `applId` (int), `badgeStatus` (string),
`isUnitVerDone` (bool), `detailsPageStatus` (string).
**Source.** `:318-400`
**Java.** Navigation arguments → ViewModel `SavedStateHandle`.
**Risk.** MED

### UV00-R02 — Editability is decided twice
**Rule.** Flags are set from the constructor arguments, then **re-derived** from the server's
`VeriStatus` once the record loads.
**See.** [C09-R03 / C09-R04](../cross-cutting/C09-case-status-and-readonly.md) — this is the
fixed defect where reaching a Defunct case via Applicant Details unlocked every field.
**Risk.** HIGH

### UV00-R03 — Load order
**Rule.** On open: fetch the applicant record → fetch saved PV data → fetch the saved document
list → re-derive editability → `LoadStepContent(1)`.
**Source.** `:400-490`
**Risk.** MED

### UV00-R04 — A failed document fetch degrades, it does not abort
**Rule.** If the dedicated saved-documents endpoint returns nothing, the applicant record's own
document collection is used instead.
**Source.** `:457`, `:477`
**Why.** A failure there only costs Stage 8 its saved-document list; it must not block the
whole verification.
**Risk.** MED

---

## 2. Ten stages

### UV00-R05 — Dispatch table
**Rule.** `TotalSteps = 10`, starting at 1.

| # | Stage | Document |
|---|---|---|
| 1 | Beneficiary | [UV01](UV-01-beneficiary.md) |
| 2 | Unit Detail | [UV02](UV-02-unit-detail.md) |
| 3 | EDP Training | [UV03](UV-03-edp-training.md) |
| 4 | Project Detail | [UV04](UV-04-project-detail.md) |
| 5 | Finance | [UV05](UV-05-finance.md) |
| 6 | Product & Sales | [UV06](UV-06-product-and-sales.md) |
| 7 | Employment | [UV07](UV-07-employment-detail.md) |
| 8 | Document Upload | [UV08](UV-08-document-upload.md) |
| 9 | Verification | [UV09](UV-09-verification.md) |
| 10 | Summary & Final Submit | [UV10](UV-10-summary-otp.md) |

**Source.** `:1464-1497`
**Risk.** LOW

### UV00-R06 — Every stage change rebuilds the stage from scratch
**Rule.** The content container is cleared and the stage rebuilt on every change. There is no
caching of built stages.
**Source.** `:1462`
**Consequence.** All per-stage view state is lost on every switch, which is why UV00-R08 and
[C05-R18](../cross-cutting/C05-form-field-behaviour.md) exist.
**Java.** `FragmentTransaction.replace()`. The ViewModel survives, so the save/restore dance
below becomes "the data was never lost" — a genuine simplification, not a shortcut.
**Risk.** HIGH

### UV00-R07 — Field errors are re-applied after every rebuild
**Rule.** `ReapplyFieldErrors(step)` runs immediately after the stage is built.
**See.** [C05-R18 / C05-R19](../cross-cutting/C05-form-field-behaviour.md)
**Source.** `:1498`
**Risk.** HIGH

---

## 3. Form state

### UV00-R08 — State is saved before leaving and restored after arriving ⚠️
**Rule.** On a stage change: the current stage's control values are harvested into a dictionary
**before** the switch; after the new stage is built, that stage's previously saved values are
written back into its controls.
**Source.** `:835-850` (save), `:1104-1125` (restore), sequenced at `:796-813`
**Why.** UV00-R06 destroys the views. Without this, moving away from a stage and back loses
everything typed since the last API save.
**Java.** State lives in the ViewModel as `LiveData`/`StateFlow`. Fragments bind to it. The
harvest-and-restore disappears entirely.
**Risk.** HIGH

### UV00-R09 — Restore happens after a delay
**Rule.** Restore runs ~50 ms after the stage is built.
**Source.** `:810-812`
**Why.** The controls are not in the view tree at the moment the build call returns; restoring
into unattached controls silently does nothing.
**Java.** `onViewCreated` — the delay disappears. **Reproduce the effect, not the delay.**
**Risk.** MED

### UV00-R10 — Values are keyed by normalised caption
**Rule.** Harvest and restore both key on the normalised caption.
**See.** [C05-R01](../cross-cutting/C05-form-field-behaviour.md)
**Source.** `:910`, `:969`, `:1026`, `:1065`, and the restore mirrors at `:1153`–`:1346`
**Note.** The harvest walks the tree handling Entry, Editor, Picker and Label separately, in
two near-identical ~200-line blocks (one to save, one to restore). In Java this is **one**
`Map<FieldId, String>` in the ViewModel. One of the largest simplifications in the port.
**Risk.** HIGH

### UV00-R11 — Live controls beat saved state during validation
**Rule.** Validation reads the on-screen control, never the saved dictionary.
**See.** [UV07-R05](UV-07-employment-detail.md)
**Why.** The saved dictionary falls back to the previously stored value when a field is
cleared, so a just-emptied field would still pass.
**Risk.** HIGH

---

## 4. Stepper

### UV00-R12 — Horizontal strip, three states per step
**Rule.** Each step renders as current / past / future, with a connector between consecutive
steps that fills when the following step has been reached.
**Source.** `:681-725`
**Risk.** LOW

### UV00-R13 — Tapping a step is a guarded navigation request
**See.** [C06-R08 / C06-R14](../cross-cutting/C06-navigation-and-back.md)
**Risk.** HIGH

### UV00-R14 — The strip scrolls to the current step
**Rule.** After a change, scroll so the current step is visible, clamped at 0, **animation off**.
**Source.** `:819-828`
**Risk.** LOW

---

## 5. Saving

### UV00-R15 — Stages save individually, on SAVE & NEXT
**Rule.** Each stage posts its own payload when SAVE & NEXT is pressed, then advances. Nine of
ten stages use `Insert_UVData_PV` with a stage-specific body.

| Stage | Saves | Endpoint |
|---|:---:|---|
| 1 Beneficiary | **no** | — |
| 2 Unit Detail | yes | `Insert_UVData_PV` |
| 3 EDP Training | yes | `Insert_UVData_PV` |
| 4 Project Detail | yes | `Insert_UVData_PV` |
| 5 Finance | yes | `Insert_UVData_PV` |
| 6 Product & Sales | yes | `Insert_UVData_PV` |
| 7 Employment | yes | `Insert_UVData_PV` |
| 8 Document Upload | per file | `UploadAllDocumentsUV` |
| 9 Verification | yes | `Insert_UVData_PV` |
| 10 Final Submit | yes | `PhyVerFinalSub_TP` |

**See.** [C02-R08](../cross-cutting/C02-api-contracts.md) — **one endpoint, eight payload
shapes. Use one DTO per stage, never a shared one.**
**Risk.** HIGH

### UV00-R16 — Stage 1 does not call the API
**Rule.** Beneficiary save is a no-op — the data is read-only applicant data.
**Source.** `:8760-8765`
**Risk.** LOW

### UV00-R17 — The local model is updated after each save
**Rule.** On success, the in-memory record is updated so later stages read the new values
without a refetch.
**Source.** `:9539+` and equivalents
**Risk.** MED

### UV00-R18 — Save is wrapped in a blocking loader, cleared in a finally
**See.** [C08-R10](../cross-cutting/C08-dialogs-and-modals.md)
**Risk.** HIGH

---

## 6. Java structure

```
UnitVerificationActivity          host: header, stepper, fragment container, back callback
UnitVerificationViewModel         record, editability, per-stage form state, guards
  ├─ StageValidators              one object per stage, pure, unit-testable
  └─ UnitVerificationRepository   one request DTO per stage
stages/Stage01BeneficiaryFragment … Stage10SummaryOtpFragment
```

Three things that must stay true:

1. **Validators are pure functions** — input values in, result out. No view access. That is
   what makes 176 rules testable without a device.
2. **One request DTO per stage** (C02-R08).
3. **Guards live in the ViewModel**, so the stepper and the SAVE & NEXT button run identical
   code (C06-R08/R14).

---

## 7. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Type in stage 3, go to stage 5, return | typed values still there (R08) |
| 2 | Type in stage 3, go to 5, return, go to 7, return | still there — survives repeat switches |
| 3 | Open stage 2, capture location | stage rebuilds, Digi PIN error survives (R07) |
| 4 | Save stage 4, go to stage 10 | stage 10 shows the new values without a refetch (R17) |
| 5 | Open a Completed case | all stages open, read-only (R02) |
| 6 | Airplane mode, SAVE & NEXT on stage 5 | loader clears, error alert, **stays on stage 5** |

---

## 8. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 18 rules traced |
| R2 | — | pending |
| R3 | — | pending |
