# C02 — API contracts

| | |
|---|---|
| **MAUI source** | `Util/Appconstants.cs` + 26 separate `new HttpClient` sites across 11 page files |
| **Java target** | `core/network/ApiService` (one Retrofit interface), `core/network/RetrofitClient` |
| **Review state** | **R1 PARTIAL** — inventory complete and traced; request/response bodies **not yet captured** |
| **Rules** | 18 (8 HIGH) |

> ✅ **Capture run 2026-11-05.** Three core read endpoints captured in full off the wire;
> `GetPVList` captured from the app's own logging. Schemas (types only, no values) are in
> [`captures/`](../captures/). The remaining endpoints are writes — not exercised, because
> they modify real government records. Section 7 says what is still open.

---

## 1. Base URL

### C02-R01 — Host is build-configuration dependent
**Rule.** `API_BASE = <host>/MobileApp`.

| Build | Host |
|---|---|
| Debug | `https://59.95.101.93:8054` |
| Release | `https://59.95.101.93:8054` — **currently also UAT** |
| Production (intended) | `https://pmegp.msme.gov.in` |

**Source.** `Util/Appconstants.cs:10-24`
**Status.** Release pointing at UAT is **deliberate and current** — it is there for on-device
testing. It is not a bug to fix. It must be switched to the live domain before any Play Store
AAB is built.
**Java.** Use Gradle `buildConfigField`, one value per build type, so the production URL
cannot be reached by editing a constant. Add a release-build assertion that the host is not
an IP address.
**Risk.** HIGH

### C02-R02 — Privacy policy is a web URL on the same host
**Rule.** `<host>/Home/PrivacyPolicy`, opened in a WebView.
**Source.** `Util/Appconstants.cs:26`
**Risk.** LOW

---

## 2. Transport

### C02-R03 — TLS validation is bypassed
**Rule.** The app accepts the UAT host's certificate without validation; a custom WebView
client does the same for embedded pages.
**Source.** `Platforms/Android/SslBypassWebViewClient.cs`
**Why it exists.** `59.95.101.93` serves a certificate that does not match an IP SAN.
**Java — this is the highest-risk item in the document.** A bypassing `TrustManager` or
`WebViewClient.onReceivedSslError` that reaches a release build is an automatic Play Store
rejection and a critical VAPT finding.

Required in the port:
- the bypass lives in `src/debug/`, never in `src/main/`
- release uses `network_security_config.xml` with default pinning-free validation
- a unit test asserts the release `OkHttpClient` has no custom trust manager

**Risk.** HIGH

### C02-R04 — One HttpClient per call today
**Rule.** The MAUI app constructs **26 separate `HttpClient` instances**.
**Source.** 26 sites, `grep -rn "new HttpClient"`
**Java.** One singleton `OkHttpClient` + one Retrofit instance. This is a straight
improvement — connection pooling, one timeout policy, one interceptor chain — and changes no
observable behaviour.
**Risk.** MED

### C02-R05 — Headers
**Rule.** `Accept: application/json` on login. Bodies are `application/json; charset=utf-8`.
`Authorization: Bearer <token>` is sent **only by logout** today.
**Source.** `LoginPage.xaml.cs:548-549`, `Util/SessionManager.cs:131-132`
**⚠️ CORRECTED 2026-11-05 by capture — this document previously assumed every other endpoint
was unauthenticated. That is wrong.**

`GetPrePostalVerificationDashboardData`, called with no `Authorization` header, returns:

```json
{"success": false, "message": "Authorization token missing"}
```

HTTP **200** with a failure body — not a 401. So auth enforcement is **per-endpoint and
inconsistent**: `GetApplDataForPV`, `GetSavedPVData`, `GetBankVerApplData`, `GetPVList`,
`GetPVDashboardData`, `DigiPinEncodeAsync` and even the **write** `Insert_UVData_PV` all
succeeded with no token, while the pre/post dashboard refused.

**Java — attach the Bearer token to EVERY request via an `AuthInterceptor`.** It is not
optional insurance; at least one endpoint already requires it, and a client that attaches it
selectively will fail on exactly the endpoints nobody tested.

**Also:** a failure arrives as HTTP 200 with `success: false`. An `isSuccessful()` check on the
HTTP status is **not** enough — every response must be inspected for `success`.
**Risk.** HIGH

### C02-R06 — No timeout is configured
**Rule.** MAUI's default `HttpClient` timeout (100 s) applies everywhere.
**Java.** 100 s is far too long on a field phone with poor signal — the officer sees a frozen
loader. Set connect 15 s / read 30 s / write 30 s and surface a retry. Record as an
intentional change.
**Risk.** MED

---

## 3. Endpoint inventory

29 endpoints. `?` marks a shape inferred from the C# and **not yet captured**.

### Authentication

| # | Endpoint | Method | Request | Used by |
|---|---|---|---|---|
| 1 | `AuthenticatePVLogin` | POST | `{ "data": "<RSA+Base64>" }` wrapping `{ "UserID", "Password" }` | `LoginPage:532-545` |
| 2 | `AuthenticateBankLogin` | POST | same envelope ? | `LoginPage` |
| 3 | `LogoutPVUser` | POST | empty body, **Bearer token** | `SessionManager:131` |

**C02-R07 — Login is double-wrapped.** The credentials are serialised to JSON, that JSON is
RSA-encrypted ([C10-R08](C10-cryptography.md)), and the Base64 result is placed in a `data`
field of a second JSON object. **Risk.** HIGH

### Dashboard & lists

| # | Endpoint | Method | Request | Used by |
|---|---|---|---|---|
| 4 | `GetPVDashboardData` | POST ? | ? | `DashboardPage` |
| 5 | `GetPrePostalVerificationDashboardData` | POST ? | ? | `DashboardPage`, `PrePostVerificationPage` |
| 6 | `GetBankApplicationCount` | GET | `?bankIfsc=<ifsc>` | `DashboardPage` |
| 7 | `GetPVList` | POST ? | ? | `DetailsPage` |
| 8 | `GetPrePostalVerificationList` | POST ? | ? | `DetailsPagePreVer` |
| 9 | `GetBankApplicationData` | POST ? | ? | `DetailsPageBL` |

### Applicant data

| # | Endpoint | Method | Request | Used by |
|---|---|---|---|---|
| 10 | `GetApplDataForPV` | GET | `?applID=<int>` | `UnitVerificationPage`, +3 others |
| 11 | `GetSavedPVData` | GET | `?applID=<int>` | `UnitVerificationPage` |
| 12 | `GetApplicantImage` | POST ? | ? | `ApplicantDetailsPage` |
| 13 | `GetPrePostalVerificationDetailsByApplID` | POST | ? | `PrePostVerificationPage` |
| 14 | `GetBankVerApplData` | GET | `?ApplCode=<code>` | `BankPhysicalVerificationPage` |
| 15 | `GetSaveBankVerData` | POST | ? | `BankVerificationPage`, `BankPhysicalVerificationPage` |

### Unit Verification writes

| # | Endpoint | Method | Request | Used by |
|---|---|---|---|---|
| 16 | `Insert_UVData_PV` | POST | **stage-dependent** — see R08 | 6 call sites, stages 1–7, 9 |
| 17 | `UploadAllDocumentsUV` | POST | base64 file + extension | stage 8 |
| 18 | `DeleteDocument` | POST | ? | stage 8 |
| 19 | `InsertDFOrNT_PV` | POST | ? | Defunct / Non-Traceable conclusion |
| 20 | `PhyVerFinalSub_TP` | POST | ? | stage 10 final submit |

**C02-R08 — One endpoint, eight different payloads.** `Insert_UVData_PV` is called from six
sites with a different anonymous object each time — Stage 7's is in
[UV07-R14](../unit-verification/UV-07-employment-detail.md). The server presumably discriminates
on which fields are present.

**This is the single most dangerous contract in the app for the port.** In C# an anonymous
object serialises exactly the fields written. In Java, a shared DTO class serialises **every**
field, sending nulls or zeros for the stages that were not filled — which may overwrite saved
data.

Required in the port: **one request DTO per stage**, not one shared `UvDataRequest`. Plus
`@JsonInclude(NON_NULL)` or Moshi's default null-omission as a second line of defence.
**Risk.** HIGH

### OTP

| # | Endpoint | Method | Request | Response |
|---|---|---|---|---|
| 21 | `SendPVOTPtoApplicant` | POST | `{ "applID": int, "pvid": int }` | `{ "success": bool, "message": str, "maskedMobile": str }` |
| 22 | `ValidateApplicantPVOTP` | POST | `{ "applID": int, "pvid": int, "otp": str }` | `{ "success": bool, "message": str }` |

**C02-R09 — These two are confirmed.** Verified against Postman and documented in
`Modal/SharedModels.cs:17-24` and `:37-45`. The only two endpoints in the app with a
written-down contract. **Risk.** LOW

**C02-R10 — `pvid` comes from login, not from the applicant.** `PhysicalVeriID` is set once at
login and held in a static. `UnitVerificationPage` is not constructed from the login response,
so it is otherwise unreachable there.
**Source.** `Util/Appconstants.cs:33`
**Java.** Put it on the session object, not a static. **Risk.** MED

### Bank verification writes

| # | Endpoint | Method | Used by |
|---|---|---|---|
| 23 | `InsertBVData_PV` | POST | `BankVerificationPage` |
| 24 | `UpdateBankVerData` | POST | `BankPhysicalVerificationPage` (via `{saveEndpoint}`) |
| 25 | `UploadAllDocBankVer` | POST | BankPV documents |
| 26 | `DeleteDocumentBankVer` | POST | BankPV documents |
| 27 | `FinalSubBankVerData` | POST | BankPV final submit |

**C02-R11 — `UpdateBankVerData` silently drops three fields.** `GeoTag_ID`, `DIGIPIN` and
`VarStatus` are collected by the app and not persisted by this endpoint.
**Status.** Open backend defect, reported, unresolved.
**Action.** Fix server-side **during Pass A**, so the Java client is written against a
corrected contract rather than inheriting the bug and a workaround for it.
**Risk.** HIGH

### Other

| # | Endpoint | Method | Used by |
|---|---|---|---|
| 28 | `DigiPinEncodeAsync` | POST | 3 screens — lat/lng → DIGIPIN |
| 29 | `UpdatePostalPreVarDetails` | POST | `PrePostVerificationPage` |

**C02-R12 — DigiPIN is server-side.** The app does not compute it; it posts coordinates and
receives `{ digipin, message, status }`.
**Source.** `Modal/SharedModels.cs:5-16` — one of three documented response shapes.
**Risk.** LOW

---

## 3b. What the capture established

### C02-R19 — Dates arrive in TWO different formats, from the same server ⚠️⚠️
**Rule.** The same field, same applicant, same server, different endpoint:

| Endpoint | `DateofBirth` |
|---|---|
| `GetApplDataForPV` | `"/Date(941481000000)/"` |
| `GetSavedPVData` | `"/Date(941481000000)/"` |
| **`GetBankVerApplData`** | **`"1999-11-02T00:00:00"`** |

The first is the legacy ASP.NET `/Date(epoch-milliseconds)/` form; the second is ISO-8601.
**Milliseconds, not seconds** — a seconds-based parse puts every date in 1970.

**Java.** A Gson/Moshi date adapter must accept **both** and be registered per endpoint or
sniff the string. A single adapter for one format silently nulls every date from the other.
There is no way to discover this from the C#, which deserialises into `DateTime` and lets the
framework cope.
**Risk.** HIGH

### C02-R20 — `/Date(-62135596800000)/` means null, not year 1 ⚠️
**Rule.** That constant is .NET's `DateTime.MinValue` (0001-01-01). It appears in live data —
`PhysicalVerificationCreatedOn` and `DateofBirth` on the saved-PV response both carry it.
**Java.** Map it to null. Rendering it literally shows the officer `01-01-0001`; arithmetic on
it overflows.
**Risk.** HIGH

### C02-R21 — The PV response is eight sibling objects, not one record
**Rule.** `GetApplDataForPV` and `GetSavedPVData` both return:

| Object | Fields | Holds |
|---|---:|---|
| `applicantData` | **148** | the applicant; stages 1 and parts of 2 |
| `bankProcess` | **86** | sanction, loan, EDP dates, TDR |
| `loanAmountDisbursedModel` | 7 | disbursement |
| `edpTrainingModel` | 17 | stage 3 |
| `phyVerificationModel` | **78** | stages 2, 6, 7, 9 — everything the officer edits |
| `phyVerificationDocs` | array | stage 8 |
| `AgencyOffDetailModel` | 12 | agency office |
| `schemeDetDataModel` | 4 | subsidy rates |

**Source.** [`captures/GetApplDataForPV.schema.json`](../captures/GetApplDataForPV.schema.json)
**Risk.** HIGH

### C02-R22 — `isBankVerDone` lives on two objects and only one is populated ⚠️
**Rule.** On the captured record: `applicantData.isBankVerDone = null`, while
`phyVerificationModel.isBankVerDone = true`.
**This is why [UV10-R03b](../unit-verification/UV-10-summary-otp.md) ORs the two.** A Java DTO
reading only `applicantData` would report Bank Verification incomplete on a record where it is
done, and block Final Submit forever.
**Verified** against live data, and consistent with the logcat line
`[isBankVerDone] ApplCode=… isBankVerDone= isUnitVerDone=True` — the flag renders **empty**
for a nullable bool.
**Risk.** HIGH

### C02-R23 — Encrypted fields confirmed in live data
**Rule.** `ApplName` and `ComnAddress` arrive TripleDES-encrypted. Everything else on the
captured record is plaintext.
**Verified end-to-end:** the Java `TripleDesCrypto` decrypted a live `ApplName` and
`ComnAddress` from this server correctly — including an address containing `/` and `,`, the
punctuation [C03-R08](C03-input-sanitisation.md) preserves.
**Note.** `AadharNo` is **not** encrypted; `MaskAadharNo` carries the masked form separately.
**Risk.** HIGH

### C02-R24 — Employment fields are mixed-typed in the response too
**Rule.** On `phyVerificationModel`: `EmpMale` is `int`, `EmpFemale`/`EmpGEN`/… are `null`,
and `FullTime_Emp`/`PartTime_Emp`/`Seasonal_Emp` are **strings**. `TotalEMP` and
`AvgWgPaidPerMonth` are `int`.
**Java.** Nullable boxed types throughout, and `String` where the capture says string — see
[UV07-R14](../unit-verification/UV-07-employment-detail.md) for the request side, which
differs again.
**Risk.** HIGH

### C02-R25 — Pre/Post verification has its own geo fields
**Rule.** `phyVerificationModel` carries `DIGIPIN`, `PPV_DIGIPIN`, `PPV_GeoLatitude`,
`PPV_GeoLongitude` — the `PPV_` set belongs to the pre/post flow, not Unit Verification.
**Risk.** MED

### C02-R26 — `GetBankVerApplData` returns a different envelope and 11 extra tables
**Rule.** `{ success: bool, applicantData: {...134 fields} }` — a **different** shape from the
PV endpoints, with 11 empty `t_*` arrays: `t_BuildingDetails`, `t_DetailsOfSales`,
`t_DPRDetail`, `t_MachineryDetails`, `t_MeansOfFinancing`, `t_PowerEstimateExpenditure`,
`t_RawMaterials`, `t_SalaryDetails`, `t_WagesDetails`, `t_WorkingCapitalDetails`,
`t_WorkingCapitalEstimate` (DPR detail tables).
**Risk.** MED

### C02-R27 — `GetPVList` envelope
**Rule.** `{ success: bool, data: [ … ], recordCount: int }` — request
`{ "UserName": "<user>", "Status": "<list status key>" }`. **148 fields per row**, with
`ApplName` and `ComnAddress` encrypted.
**Captured.** 24 rows for `Status: "PendingApplications"`.
**Note.** `recordCount` is a third key — a DTO with only `success` and `data` ignores it, which
is harmless, but it is there.
**Risk.** MED

### C02-R28 — Failures arrive as HTTP 200 with `success: false` ⚠️
**Rule.** Confirmed on two endpoints:

| Endpoint | HTTP | Body |
|---|:--:|---|
| `GetPrePostalVerificationDashboardData` *(no token)* | **200** | `{"success":false,"message":"Authorization token missing"}` |
| `GetSaveBankVerData` *(no such record)* | **200** | `{"success":false,"message":"No Bank Pre-Verification data found."}` |

**Java.** `response.isSuccessful()` is **not** a success check here. Every call must read the
body's `success` flag. Model as a sealed `ApiResult<T>` that inspects both.
**Risk.** HIGH

### C02-R29 — `DigiPinEncodeAsync` returns ONE field, not three ⚠️
**Rule.** Request `{ "lati": <double, 6dp>, "longti": <double, 6dp> }`.
**Live response:** `{"digipin":"JT7-4CC-J232"}` — **only** `digipin`.
**But** `Modal/SharedModels.cs:5-16` declares `DigiPinResponse` with `digipin`, `message`
**and** `status`. Those two are absent from the wire, so `Status` deserialises to `false`.
**Any code branching on `status` would always take the failure path.**
**Java.** Declare only `digipin`, or make the others nullable and never branch on them.
**Risk.** HIGH

### C02-R30 — Dashboard counts, and a casing inconsistency ⚠️
**Rule.** `GetPVDashboardData` → `{ success, data: { TotalCount, IsWorkingCount,
IsDefunctCount, IsNonTracableCount, isPendingCount } }`.
**Note the casing:** four fields are `Is…`, one is **`isPendingCount`** — lower-case `i`. Also
`IsNonTracable` keeps the [F-009](../README.md) spelling.
**Java.** Gson is case-sensitive. Every one of these needs an explicit `@SerializedName`, or
`isPendingCount` silently reads as 0 and the Pending card shows empty.
**Risk.** HIGH

### C02-R31 — `GetApplicantImage` can return an empty 200
**Rule.** For a record with no photograph: HTTP **200**, **zero bytes**.
**Java.** An empty body is not valid JSON — Retrofit throws on parse. Declare the response as
nullable and handle a zero-length body, or the applicant-details screen crashes on every
record without a photo. ([S07-R06](../screens/S07-applicant-details.md) says the screen must
still render.)
**Risk.** HIGH

### C02-R32 — `Insert_UVData_PV` response, and it is idempotent
**Rule.** `{"success": true, "message": "Data saved successfully."}` — 53 bytes, HTTP 200.
**Verified idempotent 2026-11-05:** a record's Stage 7 values were read, the identical values
posted back, and the full 259 KB record re-read. **Byte-for-byte identical** — no `ModifyOn`
bump, no workflow change.
**Risk.** MED

---

## 4. Serialisation

### C02-R13 — Default .NET JSON escaping
**Rule.** `JsonSerializer.Serialize` with default options escapes `<`, `>` and `&` as `<`,
`>`, `&` in string values.
**Java.** Gson and Moshi do **not** do this. A unit address containing `Shah & Sons` is sent
as `Shah & Sons` by the MAUI app and as `Shah & Sons` by a Java client. Both are valid
JSON and decode identically — **but confirm the server decodes properly rather than storing
the literal escape.** Cheap to check, expensive to discover in production.
**Risk.** MED

### C02-R14 — Numeric vs string typing is inconsistent per endpoint
**Rule.** Some fields are JSON numbers, most are strings, and it varies within one payload —
Stage 7 sends `ApplID` and `TotalEMP` as numbers and eleven counts as strings.
**Java.** Declare each DTO field to match the captured wire format exactly. Do not normalise.
**Risk.** HIGH

### C02-R15 — Missing values are sent as `"0"`, not omitted
**Rule.** At least on Stage 7, a null value is coerced to the string `"0"` before sending.
**Source.** `UnitVerificationPage.xaml.cs:9503-9513`
**Java.** Coerce in the mapper, not the DTO, so the rule stays visible and testable.
**Risk.** MED

---

## 5. Errors

### C02-R16 — Network failure shows a generic alert, state is preserved
**Rule.** A failed call shows a title/message/`OK` dialog, the screen is left exactly as it
was, nothing is cleared, no navigation occurs. Stage 7's is `Error` /
*"Failed to save Employment"*.
**Java.** Model as a sealed `ApiResult<T>` — `Success` / `NetworkError` / `ServerError` —
rather than exceptions, so no call site can accidentally swallow a failure.
**Risk.** MED

### C02-R17 — Loader must clear on the failure path
**Rule.** The blocking loader is removed in a `finally`, **before** any error dialog, so the
dialog is never stacked on the scrim and a failed call never leaves the form locked behind a
permanent spinner.
**Source.** `UnitVerificationPage.xaml.cs:9525-9535`
**Risk.** HIGH

### C02-R18 — Session expiry
**Rule.** 25-minute inactivity timeout with a 2-minute warning, enforced client-side. See
[C01](C01-session-and-auth.md).
**Open questions for the backend team:**
- does the token carry an `expiresIn`?
- what is the server-side token lifetime?
- is there a bank-side logout endpoint? (only `LogoutPVUser` exists)
- can logout return a `reason` code rather than English prose?

**Risk.** MED

---

## 6. Open questions — backend team

Blocking for the port. Raise all seven now; the answers shape the Java `ApiService`.

| # | Question | Blocks |
|---|---|---|
| ~~Q1~~ | ~~Is the Bearer token validated on any endpoint besides logout?~~ **ANSWERED: yes — `GetPrePostalVerificationDashboardData` requires it. Attach it everywhere.** | ✅ closed |
| Q2 | Token `expiresIn` / server lifetime? | C01 |
| Q3 | Bank logout endpoint? | C01 |
| Q4 | Will `UpdateBankVerData` be fixed to persist `GeoTag_ID` / `DIGIPIN` / `VarStatus`? | C02-R11 |
| Q5 | How does `Insert_UVData_PV` discriminate between stages — presence of fields, or something else? | C02-R08 |
| Q6 | Does the server treat `&` and `&` identically? | C02-R13 |
| Q7 | Is the UAT certificate fixable (IP SAN) so the TLS bypass can be deleted? | C02-R03 |

---

## 7. Capture plan — closes gate 3

The C# tells us what is *sent*. It tells us almost nothing reliable about what comes *back*,
because responses are parsed into 69 anonymous and partial model classes scattered across page
files. Guessing those shapes is how a port fails in month four.

**Method.** Run the MAUI debug build through a proxy and perform one complete journey of each
flow, saving every request and response verbatim.

| Step | Detail |
|---|---|
| Tool | mitmproxy or Charles, device proxied to the laptop |
| TLS | the app already bypasses validation, so no extra CA install is needed |
| Flow 1 | PV login → dashboard → applicant list → full 10-stage verification → final submit |
| Flow 2 | Bank login → application list → bank verification → documents → final submit |
| Flow 3 | Pre/Post verification |
| Flow 4 | Defunct and Non-Traceable conclusions |
| Flow 5 | Error paths — wrong OTP, oversized file, expired session |
| Output | `docs/port-spec/captures/<endpoint>.{req,res}.json`, one pair per endpoint |

Then update every `?` in section 3 from the capture, and generate the Java DTOs from the real
JSON rather than from the C# classes.

**Estimated effort: 1 day.** It is the single highest-value day in Pass A — it converts 29
inferred contracts into 29 verified ones and gives the contract-replay test suite
(parity layer 1) its fixtures.

---

## 8. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ⚠️ partial — 29 endpoints inventoried and traced to call sites; bodies not captured |
| R2 | — | **blocked on the capture run** |
| R3 | — | pending |
