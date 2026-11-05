# PMEGP PV — Android (Java) port

This project is a **1:1 behavioural port** of a .NET MAUI app to native Android/Java.
The MAUI original stays frozen at `~/dev/pmegp-unit-verification` for reference.

---

## 1. The spec is the source of truth, not the C#

`docs/port-spec/` holds **441 numbered rules** across 34 documents, extracted from the MAUI
source, verified against the live UAT API, and confirmed on a physical device.

**Read the relevant spec document before writing any code for a screen or feature.**
Start at [`docs/port-spec/README.md`](docs/port-spec/README.md).

Rules have stable IDs — `UV07-R09`, `C04-R06`, `S03-R01`. Use them:

```java
// UV07-R09
countField.setOnFocusChangeListener(...)
```
```java
@Test void uv07_r09_blankCountBecomesZeroOnFocusLoss() { ... }
```

A failing test should name the rule it broke. `grep -rn "C10-R03"` must find spec,
implementation and test together.

### Why the spec exists

Much of this app's behaviour is *corrective* — rules that exist because a specific defect was
found and fixed. Those reasons lived only in C# comments. Porting from the code alone
reproduces the structure and loses the corrections, then rediscovers every defect in UAT.

**If the C# and the spec disagree, stop and ask.** Do not silently follow either.

---

## 2. The eight traps

Eight places where the obvious Java approach is wrong and fails **silently**. Full list and
reasoning in [`docs/port-spec/README.md`](docs/port-spec/README.md).

| The obvious thing | What breaks | Rule |
|---|---|---|
| Pass the 16-byte MD5 to `DESede` | throws; "fixing" by truncating corrupts every encrypted field | C10-R03 |
| One shared `UvDataRequest` DTO | serialises all fields, **overwrites saved stage data** | C02-R08 |
| One `boolean editable` | unlocks every field on a Defunct case | C09-R01 |
| Stages on the fragment back stack | reintroduces the back-retrace defect | C06-R05 |
| Filter variation selectors by category | **deletes every Devanagari matra** | C03-R04 |
| One "close current dialog" helper | destroys the form behind a validation popup | C08-R01 |
| Derive field keys from caption text | renaming a label breaks save and validation | C05-R01 |
| "Fix" `"Non-Tracable"` to `"Non-Traceable"` | breaks every status comparison | C09-R15 |

Plus, from the live API capture:

- **Dates arrive in two formats** from the same server (C02-R19) — use `core/network/ApiDates`
- **Failures are HTTP 200 with `success:false`** (C02-R28) — `isSuccessful()` is not enough
- **Auth is per-endpoint and inconsistent** (C02-R05) — attach the Bearer token to *everything*
- **`isPendingCount` has a lower-case `i`** (C02-R30) — every DTO field needs `@SerializedName`
- **`GetApplicantImage` returns an empty 200** (C02-R31) — handle a zero-length body

---

## 3. Rules for this project

1. **Spec first.** Read the rule before implementing it. Cite its ID in a comment.
2. **One request DTO per stage.** Never a shared one (C02-R08).
3. **Validators are pure functions.** Values in, result out, no view access — that is what
   makes 441 rules testable without a device.
4. **Every rule gets a test named for it.** No rule is "done" without one.
5. **Don't fix the MAUI app's quirks silently.** Several are pinned by tests on purpose
   (`f003_…`, `f007_…`, `f014_…`). Changing one needs a tester sign-off.
6. **Java 17, XML layouts, ViewBinding.** No Kotlin, no Compose — decided.
7. **`applicationId` is `com.pmegp.pv` and must not change.** Same keystore as MAUI or Play
   rejects the upload. Fingerprint in [C10 §5](docs/port-spec/cross-cutting/C10-cryptography.md).
8. **The TLS bypass belongs in `src/debug/` only.** In a release build it is an automatic Play
   Store rejection (C02-R03).
9. **Never commit raw API responses.** They hold real Aadhaar, PAN and addresses. `captures/`
   holds types only.
10. **Ask before any destructive API call.** See §6.

---

## 4. What already exists

**17 classes, 133 tests, 0 failures.** Run `./gradlew :app:testDebugUnitTest`.

| Area | Class | Spec | Tests |
|---|---|---|---:|
| Crypto | `core/security/TripleDesCrypto`, `RsaEncryptor` | C10 | 8 |
| Sanitiser | `core/security/InputSanitizer` | C03 | 17 |
| Session clock | `core/session/IdleSession` | C01 | 11 |
| Upload gates | `core/upload/UploadGate` | C04 | 11 |
| Date parsing | `core/network/ApiDates` | C02-R19/R20 | 7 |
| Words/gender | `core/util/FormatHelpers` | UV04-R03 | 9 + 532 parity |
| Case status | `data/model/CaseStatus` | C09 | 11 |
| Field keys | `ui/common/form/FieldKey` | C05-R01 | 4 |
| Dashboard map | `ui/dashboard/DashboardCards` | S03 | 3 |
| Search/address | `ui/applicants/ApplicantFilter` | S04 | 6 |
| Stage 6 / 7 / 10 / guards | `ui/unitverification/validation/*` | UV06, UV07, UV10, C06 | 41 |
| **Differential parity** | `ParityTest`, `WordsParityTest` | — | 20,500 comparisons |

These are **verified against the real MAUI implementation**, not just written. The crypto
decrypts live UAT data; the validators match 2,000 randomised .NET-generated vectors.

**Do not rewrite them without a failing test to justify it.**

---

## 5. What does not exist yet

No `Activity`, no `Fragment`, no layout, no Retrofit client, no DTOs. The engine is built;
the car is not. [`PHASES.md`](PHASES.md) is the build order.

---

## 6. Safety

- The UAT host is `59.95.101.93:8054` — a **real government server** with real beneficiary data.
- **Reads are fine.** `GetApplDataForPV`, `GetSavedPVData`, `GetPVList`, `GetPVDashboardData`,
  `GetBankVerApplData`, `DigiPinEncodeAsync` were all captured safely.
- **`Insert_UVData_PV` is idempotent** — verified byte-for-byte. Re-saving identical values
  changes nothing.
- **Never call** `UploadAllDocumentsUV`, `DeleteDocument`, `PhyVerFinalSub_TP`,
  `InsertDFOrNT_PV`, `SendPVOTPtoApplicant` or the bank writes without explicit permission.
  They add files, delete rows, submit records, conclude cases, or **send an SMS to a real
  beneficiary**.
- Decrypted names and addresses are PII. Keep them out of logs, docs and commits.

---

## 7. Git

**Do not commit, push, branch, stash or reset without asking.** Read-only git — `status`,
`diff`, `log`, `show` — is fine. The user owns every commit.
