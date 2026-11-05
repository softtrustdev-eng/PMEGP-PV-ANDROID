# Build order

Nine phases. Each has an **exit test** — a thing that must demonstrably work before the next
phase starts. No phase is done because the code is written; it is done when its exit test passes.

**Phase 0 is already complete.** Start at Phase 1.

---

## ✅ Phase 0 — Specification *(done)*

441 rules across 34 documents, verified three ways: 246 source citations resolved, 87 literals
matched, 39 structural claims checked; 11 API endpoints captured live; 19 rules confirmed on a
physical device.

Found two defects in the shipping MAUI app — [F-001](docs/port-spec/README.md) (`pvid: 0` after
session resume) and [F-016](docs/port-spec/README.md) (a missing Final Submit prerequisite) —
plus five API behaviours invisible in the C#.

---

## ✅ Phase 1a — Pure logic *(done, 133 tests)*

Crypto, sanitiser, session clock, upload gates, date parsing, number-to-words, case status,
validators, stage guards. See [CLAUDE.md §4](CLAUDE.md).

Verified against the real MAUI implementation: the crypto decrypts live UAT data, and 20,500
differential comparisons against .NET-generated vectors pass.

---

## Phase 1b — Network spine  ·  ~1 week

Everything talks to the server through one place.

**Build**
- `core/network/RetrofitClient` — **one** OkHttp + **one** Retrofit (replaces 26 `HttpClient`s)
- `AuthInterceptor` — Bearer token on **every** request (C02-R05: auth is per-endpoint and
  inconsistent; one endpoint already requires it)
- `ApiResult<T>` sealed result — **must** inspect the body's `success` flag, not just the HTTP
  status (C02-R28)
- `ApiDates` registered as a Gson adapter for **both** formats (C02-R19)
- `SslTrustConfig` in **`src/debug/` only** (C02-R03)
- Timeouts: connect 15 s, read 30 s, write 30 s (C02-R06)
- `ApiService` — all 29 endpoints declared

**Read first:** [C02](docs/port-spec/cross-cutting/C02-api-contracts.md),
[`captures/`](docs/port-spec/captures/)

**Exit test**
1. Unit test asserts the **release** OkHttp client has **no** custom trust manager
2. `GetApplDataForPV?applID=2885245` parses into DTOs with no nulls where the captured schema
   shows values
3. Both date formats parse — already covered by `ApiDatesTest`
4. A `success:false` body is surfaced as a failure, not a success

---

## Phase 1c — DTOs  ·  ~1 week

**Build** — generate from [`captures/`](docs/port-spec/captures/), **not** from the C# classes.

- `applicantData` (148), `bankProcess` (86), `phyVerificationModel` (78), `edpTrainingModel`
  (17), `loanAmountDisbursedModel`, `phyVerificationDocs`, `AgencyOffDetailModel`,
  `schemeDetDataModel`
- `@SerializedName` on **every** field — Gson is case-sensitive and the API is not consistent
  (C02-R30: `isPendingCount` has a lower-case `i`)
- Nullable boxed types throughout — live data has nulls in most fields
- **One request DTO per stage** (C02-R08) — a shared one overwrites saved data

**Exit test**
- Round-trip each captured schema: parse → serialise → compare field sets
- `GetApplicantImage`'s empty 200 does not throw (C02-R31)

---

## Phase 2 — Shell & auth  ·  ~2 weeks

**Build:** `LoginActivity` (RSA credentials, captcha, two endpoints) · verification-type
selector · `DashboardActivity` · applicant list with search and `RecyclerView` + `DiffUtil` ·
`SessionManager` wired to `IdleSession` with a `BaseActivity.dispatchTouchEvent` · the
`PageHeaderView` compound view · `PmegpApplication` sets `MODE_NIGHT_NO` (C07-R12)

**Read first:** [S01](docs/port-spec/screens/S01-login.md),
[S02](docs/port-spec/screens/S02-verification-type.md),
[S03](docs/port-spec/screens/S03-dashboard.md),
[S04](docs/port-spec/screens/S04-applicant-list-pv.md),
[C01](docs/port-spec/cross-cutting/C01-session-and-auth.md),
[C07](docs/port-spec/cross-cutting/C07-header-and-theme.md)

**Exit test**
1. Log in on a real device, land on the correct dashboard, counts match the MAUI app
2. Search by name **and** by id (S04-R01)
3. Scroll ~600 rows fast — **no wrong images on recycled rows** (S04-R06)
4. Kill the process, relaunch within 25 min → resumes, no re-login (C01-R09)
5. **`pvid` survives the resume** — F-001 must not be reproducible here

---

## Phase 3 — Form engine  ·  ~2–3 weeks

**The leverage phase.** Get it right and stages 1–10 become composition.

**Build:** `FormFieldFactory` (text / numeric / decimal / dropdown / date / read-only) ·
`FieldHolder` remembering the caption's original colour (C05-R06) · `FieldErrorController`
owning the flagged set, mark/clear and re-application (C05-R18/R19) · `FieldId` **enum**, never
derived from text (C05-R01) · one `TextWatcher` per field evaluating all rules in a defined
order (C05-R13) · scroll-to-first-error then focus (C05-R20) · `ValidationDialog`,
`ConfirmExitDialog`, `LoadingOverlay`, `RemarksDialog` as **separate** `DialogFragment`s
(C08-R01)

**Read first:** [C05](docs/port-spec/cross-cutting/C05-form-field-behaviour.md),
[C08](docs/port-spec/cross-cutting/C08-dialogs-and-modals.md)

**Exit test** — a throwaway demo screen exercising every field type and error state:
1. Error clears on the keystroke that fixes it
2. First invalid field is centred and focused
3. Flag a field, rebuild the screen → **error survives** (C05-R18)
4. Fill it, rebuild → **error does not return** (C05-R19)
5. Date field flagged → **calendar icon does not redden** (C05-R05)
6. Validation dialog over a form dialog → **form survives** (C08-R01)

---

## Phase 4 — Unit Verification, host + stages 1–5  ·  ~3 weeks

**Build:** `UnitVerificationActivity` (header, stepper, fragment container,
`OnBackPressedCallback`) · `UnitVerificationViewModel` holding record, editability and per-stage
state · `StageGuard` wired so the stepper **and** the button use one path (C06-R08/R14) ·
Fragments 1–5 · `LocationProvider` + DIGIPIN + the WebView map with the `pmegp://openmaps`
bridge (UV02-R08/R09)

**Read first:** [UV00](docs/port-spec/unit-verification/UV-00-host-and-shared-state.md)
→ [UV01](docs/port-spec/unit-verification/UV-01-beneficiary.md)
… [UV05](docs/port-spec/unit-verification/UV-05-finance.md),
[C06](docs/port-spec/cross-cutting/C06-navigation-and-back.md),
[C09](docs/port-spec/cross-cutting/C09-case-status-and-readonly.md)

**Exit test**
1. Type in stage 3, go to 5, return → **values still there** (UV00-R08)
2. Back from stage 4 → confirmation, then the list — **not** stage 3 (C06-R01)
3. Open a **Defunct** case → every stage read-only, including stage 2's `forceEditable` fields
   (C09-R01) — *this is already proven on the MAUI app; it must hold here*
4. Same case via Applicant Details → **still read-only** (C09-R04)
5. Stage 1 never calls the API (UV01-R02)
6. Stage 3 save preserves `EDPTCName` / `EDPAddress` (UV03-R02)

---

## Phase 5 — Unit Verification, stages 6–10  ·  ~4–5 weeks

The hard half: cross-field validation, documents, OTP, final submit.

**Build:** Fragments 6–10 · `DocumentRepository` on `UploadGate` · Photo Picker instead of
storage permissions (C04-R16) · OTP generate/validate with the resend cooldown and
`viewLifecycleOwner`-bound timer (UV10-R12/R13) · `FinalSubmitChecklist` wired — **all 14
prerequisites including Bank Verification** (UV10-R03b / F-016)

**Read first:** [UV06](docs/port-spec/unit-verification/UV-06-product-and-sales.md),
[UV07](docs/port-spec/unit-verification/UV-07-employment-detail.md),
[UV08](docs/port-spec/unit-verification/UV-08-document-upload.md),
[UV09](docs/port-spec/unit-verification/UV-09-verification.md),
[UV10](docs/port-spec/unit-verification/UV-10-summary-otp.md),
[C04](docs/port-spec/cross-cutting/C04-image-capture-and-compression.md)

**Exit test**
1. `0.0001` in Annual Production → error **on typing** (UV06-R03)
2. Stage 7: clear a count, tab out → becomes `0` (UV07-R09); then stage 10 does **not** say
   "Stage 7 not filled"
3. 5 MB JPEG accepted and compressed; 5 MB PDF rejected (C04-R03)
4. 1 MB JPEG **not** recompressed (C04-R06)
5. Stage 10 lists Bank Verification when it is not done (F-016)
6. Leave stage 10 and return → `GENERATE OTP`, not `RESEND` (UV10-R15)
7. `VeriStatus` and `VerStatus` both sent, not swapped (UV09-R01)

---

## Phase 6 — Bank Verification, Inspection, Pre/Post  ·  ~4 weeks

**Build:** `BankPhysicalVerificationActivity` (3 stages) · `BankVerificationActivity` (keep the
digits-only **allowlist**, do not substitute the C03 blocklist — S08-R01) ·
`InspectionDetailsActivity` (3 stages) · pre/post flow

**Read first:** [S05](docs/port-spec/screens/S05-applicant-list-bank.md),
[S08](docs/port-spec/screens/S08-bank-verification.md),
[S09](docs/port-spec/screens/S09-bank-physical-verification.md),
[S10](docs/port-spec/screens/S10-inspection-details.md)

**Blocked on:** a bank test account. S05/S08/S09 were never R2-verified — expect open questions.

**Exit test:** bank verification saves and round-trips all 12 fields **including the CGTMSE
toggle** (S08-R07); the pre/post dashboard works **with** the Bearer token (C02-R05).

---

## Phase 7 — Parity hardening  ·  ~3 weeks

1. **Rule sweep** — every one of the 441 rules has a test or a signed-off manual check
2. **Side-by-side** — both APKs, same applicant, same stage, screenshots compared
3. **Server-side truth** — run the same script on both apps, diff the **saved record**. This is
   the only comparison that cannot lie.
4. **Tester's defect list** re-run against the Java build
5. **VAPT re-scan**
6. **Low-end device** — a 600-row list, a 10-stage form, a 5 MB upload

**Exit test:** 5 applicants covering fresh / part-saved / Completed / Defunct / Non-Traceable
produce **byte-identical server records** from both apps.

---

## Phase 8 — Release  ·  ~1–2 weeks

1. `BASE_HOST` → the live domain. **Assert in a test that the release host is not an IP.**
2. Sign with the **same keystore**; verify with
   `keytool -printcert -jarfile app-release.aab` against the fingerprint in
   [C10 §5](docs/port-spec/cross-cutting/C10-cryptography.md)
3. Check Play Console → Setup → App integrity for Play App Signing
4. `versionCode` clear of the MAUI app's (currently 100 vs 8)
5. **Handle leftover MAUI `Preferences`** — a user updating from MAUI 1.2 gets this app on the
   same `applicationId`; decide read-or-clear (C01-R07)
6. Internal testing → closed track → **5% staged rollout**

---

## Timeline

| Phase | Duration |
|---|---|
| 1b + 1c network & DTOs | 2 weeks |
| 2 shell & auth | 2 weeks |
| 3 form engine | 2–3 weeks |
| 4 UV 1–5 | 3 weeks |
| 5 UV 6–10 | 4–5 weeks |
| 6 bank / inspection / pre-post | 4 weeks |
| 7 hardening | 3 weeks |
| 8 release | 1–2 weeks |
| **Total** | **21–24 weeks solo · 13–16 weeks with two developers** |

Phase 0 and 1a are done, which is roughly 5 weeks already banked.

---

## Still open — carry into the work

| # | Item | Phase |
|---|---|---|
| 1 | **F-001** — one-line fix in the MAUI app's `TryResume`; not applied | now |
| 2 | **Keystore backup outside the repo** | before Phase 8 |
| 3 | Play App Signing enrolled? | before Phase 8 |
| 4 | `UpdateBankVerData` drops `GeoTag_ID` / `DIGIPIN` / `VarStatus` — fix server-side | before Phase 6 |
| 5 | 5 destructive endpoints never captured | Phase 5–6 |
| 6 | Bank test account | Phase 6 |
| 7 | F-014: stages 3, 4, 5 never validated — intended? | ask the tester |
| 8 | F-008: remarks modal output goes nowhere | ask the backend team |
