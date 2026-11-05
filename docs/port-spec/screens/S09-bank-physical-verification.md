# S09 — Bank physical verification

| | |
|---|---|
| **MAUI source** | `BankPhysicalVerificationPage.xaml.cs` (5,712), `.xaml` (61) |
| **Java target** | `ui/bankverification/BankPhysicalVerificationActivity` + stage fragments |
| **Review state** | **R1 PASSED** |
| **Rules** | 18 (7 HIGH) |

> The second-largest file in the app. Like [UV00](../unit-verification/UV-00-host-and-shared-state.md),
> its XAML is 61 lines against 5,712 of code-behind — the UI is built imperatively.

---

## 1. Shape

### S09-R01 — A **three**-stage form, same pattern as Unit Verification
**Rule.** `TotalSteps = 3`, starting at 1. Stepper, per-stage builders, per-stage saves, a
shared modal stack.

| # | Stage | Builder |
|---|---|---|
| 1 | Applicant Details | `LoadApplicantDetailsContent()` `:1423` |
| 2 | Document Upload | `LoadDocumentUploadContent()` `:2065` |
| 3 | Summary | `LoadSummaryContent()` `:3536` |

**Stage 1 fields** (17):

| Label |
|---|
| Whether unit is a new/existing unit?* |
| Applicant Name* |
| Geo-Tag-ID* |
| Digi PIN* |
| Site Address* |
| Pin code* |
| Date of Visit* |
| Premises* |
| Contact Number* |
| Type of Industry* |
| Activity Name* |
| Capital Expenditure* |
| Working Capital* |
| Infrastructure Requirements - Power* |
| Infrastructure Requirements - Space* |
| Observation of premises* |

**Source.** `:301-303`, `:1172-1180`, `:1423-2064`
**Java.** `Stage1ApplicantDetailsFragment`, `Stage2DocumentUploadFragment`,
`Stage3SummaryFragment` under `ui/bankverification/stages/`.
**Java.** Reuse `ui/common/form/`, `ui/common/stepper/` and the dialog components wholesale.
This screen is ~80% composition over Pass A.
**Risk.** HIGH

### S09-R02 — Loads by `ApplCode`, not `applId` ⚠️
**Rule.** `GetBankVerApplData?ApplCode=<code>` — a **string code**, where Unit Verification
uses an integer id.
**Source.** endpoint inventory
**Java.** Do not share one identifier type across both flows.
**Risk.** HIGH

### S09-R03 — Save endpoint is chosen at runtime
**Rule.** The save URL is interpolated — `{saveEndpoint}` — rather than fixed.
**Source.** `BankPhysicalVerificationPage.xaml.cs` save path
**Open.** Which endpoints, and on what condition. **Blocks R2.**
**Risk.** HIGH

### S09-R04 — `UpdateBankVerData` drops three fields ⚠️
**Rule.** `GeoTag_ID`, `DIGIPIN` and `VarStatus` are collected by the app and **not persisted**.
**Status.** Open backend defect, reported, unresolved.
**See.** [C02-R11](../cross-cutting/C02-api-contracts.md)
**Action.** Fix server-side **during Pass A** so the Java client is written against a corrected
contract instead of inheriting the bug plus a workaround.
**Risk.** HIGH

### S09-R05 — Final submit
**Rule.** `FinalSubBankVerData`.
**Open.** Payload not captured.
**Risk.** HIGH

---

## 2. Documents

### S09-R06 — Same upload pipeline as Stage 8
**See.** [C04](../cross-cutting/C04-image-capture-and-compression.md) — all 16 rules.
**Source.** `:2740-3020`
**Risk.** HIGH

### S09-R07 — Camera path avoids double compression
**Rule.** Compress at capture; at upload compress **only** when over 2 MB **and** the extension
is jpg/jpeg/png.
**Source.** `:2828` (capture), `:2966-2968` (upload gate)
**See.** [C04-R06](../cross-cutting/C04-image-capture-and-compression.md)
**Risk.** HIGH

### S09-R08 — Extension comes from magic bytes here ⚠️
**Rule.** This screen sniffs content: `%PDF` → `.pdf`, `\x89PNG` → `.png`, `FF D8` → `.jpg`,
default `.jpg`.
**Source.** `:3517-3534`
**Note.** **Different from Unit Verification**, which uses the filename
([C04-R12](../cross-cutting/C04-image-capture-and-compression.md),
[F-002](../README.md)/[F-004](../README.md)). This is the **correct** approach. Unify in
`core/util` — but unifying changes UV behaviour, so it needs a tester sign-off.
**Risk.** HIGH

### S09-R09 — Upload and delete endpoints
**Rule.** `UploadAllDocBankVer`, `DeleteDocumentBankVer`.
**Risk.** MED

### S09-R10 — Size hint label
**Rule.** `File size should be less than 2 MB.` below the Choose File row.
**Source.** `:3139-3145`
**See.** [C04-R14](../cross-cutting/C04-image-capture-and-compression.md)
**Risk.** LOW

### S09-R11 — Two stale debug strings
**Rule.** Two debug messages near `:2985` and `:3042` still say "compressed" on a path where
compression may not have run.
**Status.** Cosmetic, logging only. Recorded so the port does not copy a misleading message.
**Risk.** LOW

---

## 3. Modals

### S09-R12 — Validation popup must not close the form modal ⚠️
**Rule.** Tracked separately from the form modal, so a validation error does not destroy the
upload form behind it.
**Source.** `:306-310`
**See.** [C08-R01](../cross-cutting/C08-dialogs-and-modals.md) — the fixed defect. **This is
the screen where it was reported.**
**Risk.** HIGH

### S09-R13 — Back closes validation, then form, then confirms
**Source.** `:1147-1160`
**See.** [C08-R02](../cross-cutting/C08-dialogs-and-modals.md)
**Risk.** MED

### S09-R14 — Loader is independent, cleared in a finally
**Source.** `:1986`, `:2052`, use at `:441`, `:506`, `:566`, `:614`, `:623`
**See.** [C08-R10](../cross-cutting/C08-dialogs-and-modals.md)
**Risk.** HIGH

---

## 4. Other

### S09-R15 — Location and DIGIPIN
**Rule.** Has its own GET LOCATION / GENERATE DIGIPIN, with the loader messages
`Getting location...` and `Generating DigiPIN...`.
**Source.** `:1856`, `:1942`
**See.** [UV02-R03..R07](../unit-verification/UV-02-unit-detail.md)
**Note.** `:1949`'s `usesPageOverlay` branch means two different overlay owners exist here.
Trace before porting.
**Risk.** MED

### S09-R16 — Input sanitising is wired
**Rule.** Hooks at `:4992` (entry) and `:5163` (editor).
**See.** [C03](../cross-cutting/C03-input-sanitisation.md)
**Risk.** MED

### S09-R17 — Back exits the form with confirmation
**See.** [C06-R01](../cross-cutting/C06-navigation-and-back.md) — one of the three forms this
applies to. `_stepHistory` was removed here too.
**Risk.** HIGH

### S09-R18 — Status bar colour
**Rule.** `#2D5F3F`, changed from `#4CAF50`.
**See.** [C07-R07](../cross-cutting/C07-header-and-theme.md)
**Risk.** LOW

---

## 5. Open questions

| # | Question | Blocks |
|---|---|---|
| Q2 | Which endpoints does `{saveEndpoint}` resolve to? | R03 |
| Q3 | `FinalSubBankVerData` payload? | R05 |
| Q4 | Will `UpdateBankVerData` be fixed? | R04 |
| Q5 | What is `usesPageOverlay` distinguishing? | R15 |

**Q1 closed** — three stages, enumerated in S09-R01. Stage 1's 17 fields are listed; stages 2
and 3 are Document Upload ([C04](../cross-cutting/C04-image-capture-and-compression.md)) and a
read-only Summary, both already specified by the cross-cutting documents.

---

## 6. R2 checklist

| # | Step | Expected | State |
|---|---|---|---|
| 1 | Bank login → Pending → Document Upload → save with a field missing | **form survives behind the popup** (R12) | ✅ |
| 2 | Dismiss the popup | form still open, values intact | ✅ |
| 3 | Capture a photo, upload | compressed once (R07) | ✅ logic |
| 4 | Upload a PNG | stored extension matches content (R08) | ⬜ |
| 5 | Save, check `GeoTag_ID` / `DIGIPIN` / `VarStatus` | **currently dropped** (R04) | ⬜ |
| 6 | Back from mid-form | confirmation, then list (R17) | ⬜ |
| 7 | Confirm the 3 stages and 17 stage-1 fields on device | — | ⬜ |

---

## 7. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 18 rules traced; stage structure decomposed |
| R2 | 2026-11-05 | ⚠️ 3 of 7 confirmed during the document-upload ticket |
| R3 | — | pending |
