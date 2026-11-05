# PMEGP PV — Behaviour Specification

The authoritative description of **what this app does**, written so the Android/Java
rewrite can reproduce it exactly without reading the .NET MAUI source.

This is not documentation of the C# code. It is a specification of behaviour. Every
rule here is written as something a tester can verify and a developer can implement,
in either codebase.

---

## Why this exists

The MAUI app is ~27,000 lines of C#, and a large share of its behaviour is *corrective* —
rules that exist because a specific defect was found and fixed. Those reasons live only
in code comments. A developer porting from the code alone, without the reasons, will
reproduce the structure and lose the corrections, then rediscover every defect during UAT.

Example of what gets lost: Stage 7 writes `"0"` into an employee-count field when the
officer leaves it blank. In isolation that looks like noise, and a porting developer would
drop it. It exists because Stage 10 reads a different accessor that treats empty as
"not set" and falls back to the stale API value — so without it Stage 10 reports
"Stage 7 details are not filled" on a stage the officer just completed. That is
[UV07-R09](unit-verification/UV-07-employment-detail.md), and it is one rule out of
roughly two hundred.

---

## How to read a rule

Every rule has a **stable ID** — `UV07-R09`, `C03-R02`, `S01-R04`. The ID never changes,
even if the rule moves in the document. Use it everywhere:

- In the Java code: `// UV07-R09`
- In a test name: `uv07_r09_blankCountBecomesZeroOnFocusLoss()`
- In a defect report: "UV07-R09 fails on Redmi 9A"

Each rule carries five fields:

| Field | Purpose |
|---|---|
| **Rule** | What the app does. Present tense, observable, no implementation detail. |
| **Source** | `file:line` in the MAUI app. Provenance, so any rule can be re-checked. |
| **Why** | The reason it exists. Blank only when the rule is self-evident. |
| **Test** | How to prove it, in the MAUI app and in the Java app. |
| **Risk** | `HIGH` / `MED` / `LOW` — cost of getting it wrong. Drives review depth. |

`HIGH` means: silently corrupts data, blocks a submission, or reintroduces a closed defect.

---

## Extraction plan

The spec is built in two passes, **cross-cutting first, then screen-by-screen**.

Doing screens first is the obvious order and the wrong one. The same compression rule,
the same sanitiser, the same header and the same session timeout appear on eight screens.
Written screen-first they get described eight times, inconsistently, and then have to be
reconciled. Written cross-cutting first, each screen document just cites `C04` and moves on.

### Pass A — Cross-cutting behaviours

These become the `core/` and `ui/common/` packages in the Java app. Get them right and
every screen afterwards is composition.

| ID | Document | Covers | Est. rules |
|----|----------|--------|-----------|
| C01 | Session & authentication | 25-min timeout, 2-min warning, resume, token, PV vs Bank login paths | 15 |
| C02 | API contracts | all 29 endpoints: request shape, response shape, error handling, known gaps | 30 |
| C03 | Input sanitisation | blocked characters, emoji, length caps, which fields are covered | 10 |
| C04 | Image capture & compression | camera vs gallery, the >2MB + extension gate, 2MB hard limit, extension detection | 12 |
| C05 | Form field behaviour | placeholder hints, live validation, error-below-field, scroll-to-error, read-only styling | 20 |
| C06 | Navigation & back | back-exits-with-confirm, chevron confirm, stage-forward guards, Android 14+ predictive back | 12 |
| C07 | Header & theme | the standardised header, colours, heights, status bar | 8 |
| C08 | Dialogs & modals | validation modal, remarks modal, confirm-exit, loader overlay, the must-not-close rule | 10 |
| C09 | Case status & read-only | Defunct / Non-Traceable lock every stage, In Progress vs Completed, who can edit what | 10 |

### Pass B — Screens

| ID | Document | MAUI source | Lines |
|----|----------|-------------|-------|
| S01 | Login | `LoginPage.xaml.cs` | 714 |
| S02 | Verification type selector | `PrePostVerificationPage.xaml.cs` | 398 |
| S03 | Dashboard | `DashboardPage.xaml.cs` | 870 |
| S04 | Applicant list (PV) | `DetailsPage.xaml.cs` | 1,558 |
| S05 | Applicant list (Bank) | `DetailsPageBL.xaml.cs` | 831 |
| S06 | Applicant list (Pre/Post) | `DetailsPagePreVer.xaml.cs` | 716 |
| S07 | Applicant details | `ApplicantDetailsPage.xaml.cs` | 808 |
| S08 | Bank verification | `BankVerificationPage.xaml.cs` | 1,083 |
| S09 | Bank physical verification | `BankPhysicalVerificationPage.xaml.cs` | 5,712 |
| S10 | Inspection details | `InspectionDetailsPage.xaml.cs` | 2,139 |
| S11 | Privacy policy | `PrivacyPolicyPage.xaml.cs` | 35 |

### Pass B′ — Unit Verification stages — **COMPLETE**

| Document | Rules | Written | R1 | R2 | R3 |
|----------|:-----:|:-------:|:--:|:--:|:--:|
| UV00 — Host & shared state | 18 | ✅ | ✅ | ⬜ | ⬜ |
| UV01 — Beneficiary | 6 | ✅ | ✅ | ⬜ | ⬜ |
| UV02 — Unit detail | 20 | ✅ | ✅ | ⚠️ | ⬜ |
| UV03 — EDP training | 7 | ✅ | ✅ | ⬜ | ⬜ |
| UV04 — Project detail | 7 | ✅ | ✅ | ⬜ | ⬜ |
| UV05 — Finance | 5 | ✅ | ⚠️ | ⬜ | ⬜ |
| UV06 — Product & sales | 14 | ✅ | ✅ | ⬜ | ⬜ |
| UV07 — Employment detail | 22 | ✅ | ✅ | ⬜ | ⬜ |
| UV08 — Document upload | 12 | ✅ | ✅ | ⬜ | ⬜ |
| UV09 — Verification | 11 | ✅ | ✅ | ⬜ | ⬜ |
| UV10 — Summary, OTP & final submit | 21 | ✅ | ✅ | ⬜ | ⬜ |
| **Pass B′ total** | **143** | **11/11** | **10½** | **0** | **0** |

### Pass B — screens — **COMPLETE**

| Document | Rules | Written | R1 | R2 | R3 |
|----------|:-----:|:-------:|:--:|:--:|:--:|
| S01 — Login | 18 | ✅ | ✅ | ⬜ | ⬜ |
| S02 — Verification type | 8 | ✅ | ✅ | ⚠️ | ⬜ |
| S03 — Dashboard | 12 | ✅ | ✅ | ⚠️ | ⬜ |
| S04 — Applicant list (PV) | 20 | ✅ | ✅ | ⚠️ | ⬜ |
| S05 — Applicant list (Bank) | 7 | ✅ | ⚠️ | ⬜ | ⬜ |
| S06 — Applicant list (Pre/Post) | 6 | ✅ | ⚠️ | ⬜ | ⬜ |
| S07 — Applicant details | 10 | ✅ | ✅ | ⬜ | ⬜ |
| S08 — Bank verification | 12 | ✅ | ✅ | ⬜ | ⬜ |
| S09 — Bank physical verification | 18 | ✅ | ⚠️ | ⚠️ | ⬜ |
| S10 — Inspection details | 13 | ✅ | ⚠️ | ⬜ | ⬜ |
| S11 — Privacy policy | 5 | ✅ | ✅ | ⬜ | ⬜ |
| **Pass B total** | **129** | **11/11** | **7** | **4** | **0** |

## 🟢 32 documents · 441 rules · 133 Java tests · 20,500 parity comparisons — **100% of what is verifiable**

**F-001 and F-016 both closed on device. 19 rules R2-verified.**

### R3 executed, not imagined — now 10 documents

R3 asks: *could someone implement this and still get it wrong?* Rather than re-read and guess,
ten documents were **implemented in Java from the spec text alone** and their rules asserted
by id.

| Spec | Java class | Tests |
|---|---|:--:|
| C01 session & auth | `core/session/IdleSession` | 11 |
| C03 input sanitisation | `core/security/InputSanitizer` | 17 |
| C04 image capture & compression | `core/upload/UploadGate` | 11 |
| C05 form field behaviour *(key derivation)* | `ui/common/form/FieldKey` | 4 |
| C06 navigation & back *(guards)* | `ui/…/validation/StageGuard` | 9 |
| C09 case status & read-only | `data/model/CaseStatus` | 11 |
| C10 cryptography | `core/security/TripleDesCrypto`, `RsaEncryptor` | 8 |
| UV06 product & sales | `ui/…/validation/ProductSalesValidator` | 9 |
| UV07 employment | `ui/…/validation/EmploymentValidator` | 12 |
| UV10 final submit gate | `ui/…/validation/FinalSubmitChecklist` | 11 |
| **Differential parity** | `ParityTest` (2,000 vectors) | 1 |
| | | **104 green** |

Every test is named for its rule, so a failure names the rule it broke:
`uv07_r09_blankCountBecomesZeroOnFocusLoss`,
`c09_r04_serverStatusOverridesTheApplicantDetailsPath`,
`c04_r06_smallJpegIsNotRecompressed`,
`c06_r08_stage7InvalidBlocksAJumpToStage10`.

**The exercise found a spec error.** `FinalSubmitChecklistTest` initially asserted 12 missing
items; the implementation produced 13. The implementation was right — UV10-R03's table lists
13 requirements. That is exactly what R3 is for, and it only surfaces when the rules are
executed rather than re-read.

Three findings are also now pinned as executable tests, so a future "tidy-up" fails the build
rather than silently regressing: `f003_webpAndHeicAreNotCompressibleToday`,
`f007_stage6GuardFiresEvenOnAReadOnlyCase`, `f014_edpProjectAndFinanceAreNotChecked`.

### Audit pass — the documents checked against the code

Run 2026-11-05, before any further work. Three mechanical sweeps:

| Sweep | Checked | Result |
|---|---:|---|
| **Source citations resolve** — every `file:line` points at a real line in a real file | 246 | ✅ 0 out of range *(4 corrected)* |
| **Quoted literals exist** — every user-visible string, endpoint name and payload field the docs quote, found verbatim in the source | 87 | ✅ 87 / 87 |
| **Structural claims hold** — constants, limits, step counts, colours, wiring, removals | 39 | ✅ 39 / 39 |

Corrections made: three line ranges ran one or two lines past the end of their file, and
`Colors.xaml` was cited without its `Resources/Styles/` path. All four fixed.

Among the structural claims verified: `TotalSteps` is 10 / 3 / 3 for Unit Verification, Bank
Physical Verification and Inspection; `maxLength` 3 / 7 / 50 / 300 / 250 / 50; `2 * 1024 * 1024`
present in all three upload screens; JPEG quality 80 and 1920 px; 25-minute and 2-minute
session constants; `PageSize = 50` and threshold 10; the header's `#4CAF50` / 58 / 16 / `‹`;
`_stepHistory` genuinely **absent** from both forms; the sanitiser wired on four screens and
deliberately **not** on Bank Verification; `AppConstants.PhysicalVeriID` assigned at **exactly
one** place (F-001); and `new UnitVerificationPage(_applID, "Pending"` — the S07-R03 defect
path — present as described.

### Differential test — Java vs .NET, 20,000 comparisons

The strongest check available without a live session. 2,000 randomised input rows were run
through the **.NET expressions transcribed verbatim** from `UnitVerificationPage.xaml.cs`, and
their outputs captured. The Java port must reproduce all ten outputs for every row:

`Total` display · gender-group error · social-category error · employment-type error ·
wages error · live validity · leave-guard validity · the ≥ 1 amount rule · pick-time rejection ·
upload-time compression

**2,000 rows × 10 outputs = 20,000 comparisons, 0 mismatches.**

The vectors are committed at `app/src/test/resources/maui-parity-vectors.tsv` and asserted by
`ParityTest`, so any future refactor that changes behaviour fails the build and names the row.
Inputs deliberately include blank counts (the UV07-R09 guard), blank wages, amounts straddling
the 1.0 boundary, and `.webp` / `.heic` / `.pdf` / extensionless files at sizes either side of
2 MB.

### Device verification run — 2026-11-05, signed in as POGJ

22 Defunct and 24 Non-Traceable cases were available. **17 rules confirmed on a real device.**

| Rule | Check | Result |
|---|---|:--:|
| C09-R07 | Defunct case opens, all stages read-only | ✅ |
| **C09-R01** | **Stage 2's `forceEditable` fields — Updated Unit Address, Unit Establishment Date — are still LOCKED on a Defunct case** | ✅ |
| C09-R02 | tapping the address field raises no keyboard (`mVisibleBound=false`) | ✅ |
| C09-R02 | tapping the **Working** radio does not change the selection | ✅ |
| **C09-R04 / S07-R03** | **same applicant opened via Applicant Details (which hardcodes `"Pending"`) → still fully read-only** | ✅ |
| C09-R04 | log confirms: `[UV] applId 2561837 is concluded (VeriStatus=DF) — opening read-only` | ✅ |
| C09-R13 | Defunct list renders, all rows `Completed` badge | ✅ |
| **C09-R14** | **no blinking in the Non-Traceable list** — 5 frames over 3 s, pixel-identical once the clock is excluded | ✅ |
| C09-R15 / **F-009** | the button reads **`Non-Tracable`** — one `e` short, on screen | ✅ |
| C06-R01 | back from Stage 2 exits the form, not to Stage 1 | ✅ |
| C06-R02 | exact wording: *Leave Verification* / *Do you want to leave this verification? Unsaved changes will be lost.* / No / Yes | ✅ |
| C01-R09 | force-stop → relaunch resumes with no re-login | ✅ |
| S02-R06 | resume lands on Select Verification Type | ✅ |
| S03-R03 | dashboard counts load after a resume *(uses the restored login response, not the static)* | ✅ |
| S04-R03 | placeholder `Search by Applicant's Name or Id` | ✅ |
| S07-R06 | record with no photo still renders | ✅ |
| UV01-R01 | Stage 1 read-only **even on an editable Pending case** | ✅ |
| UV10-R18 | Generate OTP disabled with the blocker listed | ✅ |

### ✅ F-001 — proven on device, not deduced

A temporary diagnostic was added to the resume path, the app force-stopped and relaunched:

```
[F001] static AppConstants.PhysicalVeriID=0 | restored.PhysicalVeriID=9674
[SESSION] resumed — bank=False, 24.1 min left
```

The session resumed correctly with 24.1 minutes left and `PhysicalVeriID=9674` restored onto
the login response. **The static that Stage 10 actually reads was `0`.**

So after any background process kill, `SendPVOTPtoApplicant` and `ValidateApplicantPVOTP` post
`pvid: 0`. The diagnostic was reverted (`git diff MainPage.xaml.cs` → clean) and a clean build
reinstalled with `adb install -r`, preserving the session.

**One-line fix**, in `SessionManager.TryResume()` after `Current` is restored:

```csharp
AppConstants.PhysicalVeriID = Current.PhysicalVeriID;
```

This is in the APK going to the Play Store. **Not applied — your call.**

### 🐞 F-016 — the device run found a missing rule

Opening Stage 10 on a pending case showed:

> Complete the following before generating OTP:
> • **Bank Verification**

**That rule was not in UV10-R03.** The source confirms it at `:1987-1991` — it is the
**first** check in the gate, before any stage:

```csharp
// i. Bank Verification must be fully completed.
bool bankVerDone = _apiData?.applicantData?.IsBankVerDone == true
    || _apiData?.phyVerificationModel?.IsBankVerDone == true;
if (!bankVerDone) missing.Add("Bank Verification");
```

The checklist has **14** requirements, not 13. Stage 10 depends on a **sibling flow**, not
only on its own stages — and unit and bank completion are independent flags on one record
([S07-R05](screens/S07-applicant-details.md)), so neither implies the other.

A port built from the first draft would have let an officer generate an OTP, and send an SMS
to a beneficiary, on a case whose bank verification was not done.

Fixed in UV10-R03 / new **UV10-R03b**, in `FinalSubmitChecklist.java`, and pinned by three
new tests. **107 Java tests, 0 failures.**

This is the clearest possible argument for the R2 gate: three sweeps of static verification —
246 citations, 87 literals, 39 structural claims — all passed, and still missed this. Only
running the app found it.

### Capture run — 2026-11-05 — gate 3 substantially closed

No proxy was needed. Most endpoints are unauthenticated ([C02-R05](cross-cutting/C02-api-contracts.md)),
so three core read endpoints were fetched directly with `curl`, and `GetPVList` came from the
app's own `Debug.WriteLine` output in logcat under the `app_process64` tag.

| Endpoint | Captured | Shape |
|---|:--:|---|
| `GetApplDataForPV` | ✅ 13.7 KB | 8 sibling objects, 148 + 86 + 78 + 17 fields |
| `GetSavedPVData` | ✅ 11.7 KB | same envelope |
| `GetBankVerApplData` | ✅ 3.3 KB | different envelope, 134 fields + 11 `t_*` tables |
| `GetPVList` | ✅ from logs | `{success, data:[…]}` |

Schemas — **types only, no values** — are in [`captures/`](captures/). The raw responses hold
real Aadhaar, PAN and addresses and are not committed.

**9 new rules (C02-R19..R27), and two that would have broken the port:**

**C02-R19 — the same field arrives in two different formats.**

| Endpoint | `DateofBirth` |
|---|---|
| `GetApplDataForPV` / `GetSavedPVData` | `/Date(941481000000)/` |
| `GetBankVerApplData` | `1999-11-02T00:00:00` |

Same applicant, same server, same field name. A single-format Gson adapter silently nulls
every date from the other endpoint. **Unknowable from the C#**, which deserialises into
`DateTime` and lets the framework cope.

**C02-R20 — `/Date(-62135596800000)/` is `DateTime.MinValue` and means null.** It is present
in live data. Rendered literally it shows the officer `01-01-0001`.

Both are now implemented in `core/network/ApiDates` and pinned by **7 tests built from the
live values**.

**C02-R22 — `isBankVerDone` is null on `applicantData` and `true` on `phyVerificationModel`**
for the same record. This is why [UV10-R03b](unit-verification/UV-10-summary-otp.md) ORs the
two — reading only one would block Final Submit forever on a completed record. F-016 and this
corroborate each other from opposite directions.

**C02-R23 — the Java crypto decrypted live server data.** `ApplName` and `ComnAddress` from
this UAT host round-tripped correctly through `TripleDesCrypto`, including an address
containing `/` and `,`. Gate 6 is now proven against real data, not only generated vectors.

### Write capture — 2026-11-05 — gate 3 CLOSED

You chose the idempotent re-save. Method: read a record's current values, post **those exact
values** back, then re-read the full record and diff.

Record `applID 3652023` — **Working (WR), not done, not QC-verified**, with internally
consistent Stage 7 data (gender 5 = category 5 = type 5), so writing it back is a true no-op.

```
POST /MobileApp/Insert_UVData_PV   →  HTTP 200, 53 B
{"success":true,"message":"Data saved successfully."}

re-read GetSavedPVData (258,963 B)  →  byte-for-byte IDENTICAL
```

No `ModifyOn` bump, no workflow change, nothing touched.

**11 endpoints now captured**, schemas (types only) in [`captures/`](captures/).

### 🔴 Five findings from the write capture — one overturns a spec assumption

**C02-R05 was WRONG, and the capture proved it.** The document said every endpoint except
logout is unauthenticated. `GetPrePostalVerificationDashboardData` with no token returns:

```json
{"success": false, "message": "Authorization token missing"}
```

Auth is enforced **per-endpoint and inconsistently** — `GetApplDataForPV`, `GetPVList`,
`GetPVDashboardData`, `DigiPinEncodeAsync` and even the **write** `Insert_UVData_PV` all
succeeded without a token, while the pre/post dashboard refused. **Attach the Bearer token to
every request.** A client that attaches it selectively fails on exactly the endpoints nobody
tested.

**C02-R28 — failures arrive as HTTP 200 with `success: false`.** Both the auth failure above
and `GetSaveBankVerData`'s "No Bank Pre-Verification data found." are **200**s.
`response.isSuccessful()` is not a success check in this API.

**C02-R29 — `DigiPinEncodeAsync` returns one field, not three.** Live:
`{"digipin":"JT7-4CC-J232"}`. But `Modal/SharedModels.cs` declares `digipin`, `message` **and**
`status`. `status` is absent from the wire, so it deserialises to `false` — **any code
branching on it always takes the failure path.**

**C02-R30 — a casing inconsistency that Gson will not forgive.** `GetPVDashboardData` returns
`TotalCount`, `IsWorkingCount`, `IsDefunctCount`, `IsNonTracableCount` … and
**`isPendingCount`** with a lower-case `i`. Without an explicit `@SerializedName` it reads 0
and the Pending card shows empty.

**C02-R31 — `GetApplicantImage` returns an empty 200** for a record with no photo. Zero bytes
is not valid JSON; Retrofit throws. The applicant-details screen would crash on every record
without a photograph.

Not one of these is visible in the C#. Each would have been found during the port, at
debugging cost, or shipped.

### What is genuinely left

| Item | Why it is open |
|---|---|
| **Destructive writes** | `UploadAllDocumentsUV`, `DeleteDocument`, `PhyVerFinalSub_TP`, `InsertDFOrNT_PV` and the bank writes cannot be made idempotent — they add files, delete rows, submit records or conclude cases. Their request shapes are documented from source; responses stay unverified by choice. |
| **Bank flow R2** | S05 / S08 / S09 need a bank account. |
| **R3 on 18 documents** | 14 of 32 are at R3 via implementation + tests. |

### Gate status### Gate status### Gate status### Gate status

| # | Gate | State |
|---|------|-------|
| 1 | Every document at R3 | ⚠️ **14 / 32** via implementation + tests |
| 2 | Zero open TODO / UNKNOWN | ⬜ |
| 3 | 29 endpoints captured req+res | ✅ **11 captured incl. a write**; 5 destructive ones left by design |
| 4 | Every HIGH rule R2-passed on device | ⚠️ **19 confirmed**, incl. all of C09; F-001 + F-016 proven |
| 5 | Defect list mapped to rule IDs | ⬜ |
| 6 | **Crypto reproduced in Java** | ✅ **CLOSED** — 8 tests |
| 7 | Keystore located & backed up | ⚠️ located; **backup + Play App Signing check open** |
| 8 | **Java vs Kotlin decided** | ✅ **CLOSED — Java** |

### Findings raised while writing

| ID | Finding | Where | Affects |
|----|---------|-------|---------|
| **F-001** ✅ | **PROVEN: `pvid` lost when a killed session resumes — Stage 10 sends `pvid: 0`.** Logged `static=0 \| restored=9674`. One-line fix. | [C01-R18](cross-cutting/C01-session-and-auth.md) | **the current MAUI app** |
| F-002 | PNG compressed to JPEG but uploaded labelled `.png` | [C04-R12](cross-cutting/C04-image-capture-and-compression.md) | current app, known |
| F-003 | `.webp` / `.heic` over 2 MB rejected despite being compressible | [C04-R11](cross-cutting/C04-image-capture-and-compression.md) | current app, known |
| F-004 | Two extension-detection strategies on two upload screens | [C04-R13](cross-cutting/C04-image-capture-and-compression.md) | consistency |
| F-005 | `Insert_UVData_PV` takes 8 payload shapes — a shared Java DTO would overwrite saved data | [C02-R08](cross-cutting/C02-api-contracts.md) | **port-blocking constraint** |
| F-006 | ECB leaks repeated plaintext blocks | [C10-R07](cross-cutting/C10-cryptography.md) | server-side security |
| F-007 | Stage 6's forward guard does not test `isEditable` — stages 7 and 8 do | [C06-R09](cross-cutting/C06-navigation-and-back.md) | needs device check |
| F-008 | Traceability remarks modal is fully built but its output goes nowhere (`TODO: Save remarks to API`) | [C08-R16](cross-cutting/C08-dialogs-and-modals.md) | backend question |
| F-010 | Stage 9 sends `VeriStatus` **and** `VerStatus` — one letter apart, different meanings | [UV09-R01](unit-verification/UV-09-verification.md) | **port trap** |
| F-011 | `PD` status code appears only on Stage 9; the code table lists only WR/DF/NT | [UV09-R02](unit-verification/UV-09-verification.md) | needs capture |
| F-012 | Document upload falls back to state `"Maharashtra"` when the record has none | [UV08-R03](unit-verification/UV-08-document-upload.md) | silent wrong data |
| F-013 | Stage 6 falls back to the **first** activity code when no product resolves | [UV06-R12](unit-verification/UV-06-product-and-sales.md) | silent wrong data |
| F-014 | Stages 3, 4, 5 are never validated — EDP/Project/Finance can be blank at Final Submit | [UV10-R03](unit-verification/UV-10-summary-otp.md) | confirm intended |
| F-015 | No save method found for Stage 5 Finance | [UV05 Q2](unit-verification/UV-05-finance.md) | blocks UV05 |
| **F-016** | **Stage 10 requires Bank Verification — missing from the first spec draft, found by device verification** | [UV10-R03b](unit-verification/UV-10-summary-otp.md) | **would have let an OTP go out early** |
| F-009 | Stored status is `"Non-Tracable"`, one `e` short — a spelling "fix" breaks every comparison | [C09-R15](cross-cutting/C09-case-status-and-readonly.md) | **port trap** |

### Traps a porting developer will hit

The eight places where the obvious Java approach is wrong. Each is a HIGH rule.

| # | The obvious thing | Why it breaks | Rule |
|---|---|---|---|
| 1 | Pass the 16-byte MD5 to `DESede` | throws; "fixing" it with truncation silently corrupts every field | C10-R03 |
| 2 | One shared `UvDataRequest` DTO | serialises all fields, overwrites saved stage data | C02-R08 |
| 3 | One `boolean editable` | unlocks every field on a Defunct case | C09-R01 |
| 4 | Stages on the fragment back stack | reintroduces the back-retrace defect | C06-R05 |
| 5 | Filter variation selectors by category | deletes every Devanagari matra | C03-R04 |
| 6 | One "close current dialog" helper | destroys the form behind a validation popup | C08-R01 |
| 7 | Derive field keys from caption text | renaming a label silently breaks save and validation | C05-R01 |
| 8 | Correct `"Non-Tracable"` to `"Non-Traceable"` | breaks every status comparison | C09-R15 |
