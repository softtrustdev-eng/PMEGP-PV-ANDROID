# S01 — Login

| | |
|---|---|
| **MAUI source** | `LoginPage.xaml.cs` (714), `LoginPage.xaml` (434) |
| **Java target** | `ui/login/LoginActivity` + `LoginViewModel` + `data/repository/AuthRepository` |
| **Depends on** | [C10 cryptography](../cross-cutting/C10-cryptography.md), [C01 session](../cross-cutting/C01-session-and-auth.md) |
| **Review state** | **R1 PASSED** |
| **Rules** | 18 (6 HIGH) |

---

## 1. Fields

| Field | Type | Cap | Notes |
|---|---|:--:|---|
| User ID | text | **50** | trimmed before use |
| Password | password | — | masked, show/hide toggle (`eye_open` / `eye_closed`) |
| Captcha | text | — | compared case-insensitively |

---

## 2. Captcha

### S01-R01 — Generated client-side, 5 characters
**Rule.** A 5-character captcha is generated **in the app** and displayed as a label.
**Source.** `:250-256`
**Why recorded.** A client-generated captcha is not a security control — it stops accidental
double-submits, not automation, since the expected value is in memory. Carry it for behavioural
parity and record it as a finding, **not** as a defence to strengthen.
**Risk.** MED

### S01-R02 — Reload button regenerates and clears
**Rule.** Tapping reload generates a fresh captcha and clears the entry.
**Source.** `:259-265`
**Risk.** LOW

### S01-R03 — Comparison is case-insensitive and trimmed
**Rule.** `captcha.Trim()` vs the expected value, ignoring case.
**Source.** `:351`
**Java.** `equalsIgnoreCase` with `Locale.ROOT`.
**Risk.** MED

### S01-R04 — Regenerated on every failed attempt
**Rule.** Any login failure regenerates the captcha and clears the field.
**Source.** `:313-314`
**Risk.** LOW

### S01-R05 — Captcha box is styled differently
**Rule.** The captcha **display** border uses `#B8D8C4` stroke on `#EFF7F2`; every other field
uses `#E0E0D8` on `#F6F6F4`.
**Source.** `:189-192`
**Risk.** LOW

---

## 3. Validation order

### S01-R06 — Checked in sequence, first failure wins
**Rule.** In order: User ID empty → Password empty → Captcha empty → Captcha mismatch. Each
shows its own alert and stops.
**Source.** `:336-352`
**Risk.** MED

### S01-R07 — Re-entrancy guard
**Rule.** The handler returns immediately if the Login button is already disabled.
**Source.** `:330`
**Why.** Prevents a double-tap firing two login calls.
**Java.** Disable in the ViewModel and expose as state; do not rely on the view's enabled flag.
**Risk.** MED

### S01-R08 — Captcha is unfocused before submitting
**Rule.** The captcha field is explicitly unfocused, dismissing the keyboard.
**Source.** `:333`
**Risk.** LOW

---

## 4. Authentication

### S01-R09 — Credentials are RSA-encrypted ⚠️
**Rule.** `{ "UserID", "Password" }` → JSON → RSA-2048 PKCS#1 v1.5 → Base64 → wrapped as
`{ "data": "<base64>" }`.
**Source.** `:532-545`
**See.** [C10-R08..R11](../cross-cutting/C10-cryptography.md). Resolves VAPT #2 (CWE-311).
**Risk.** HIGH

### S01-R10 — Two endpoints, one form ⚠️
**Rule.** The same form serves both PV and Bank login, posting to `AuthenticatePVLogin` or
`AuthenticateBankLogin`.
**Source.** `:316` comment
**Open.** How the app chooses is not fully traced. **Confirm from the capture.**
**Risk.** HIGH

### S01-R11 — Bank vs PV is derived from the response
**Rule.** `IsBankLogin` is a computed property: true when `IFSC_Code` is non-empty.
**Source.** `:713`
**Java.** Keep it derived. A stored boolean can disagree with the IFSC.
**Risk.** MED

### S01-R12 — Response shape
```java
class LoginResponse {
    boolean Status;  String Message;
    int PhysicalVeriID;   // -> pvid for OTP (C02-R10)
    int BankLoginID;
    String IFSC_Code;     // non-empty => bank login
    String UserID;  String UserName;
    String Token;         // Bearer; today only logout uses it
}
```
**Source.** `:697-715`
**Note.** Deserialised **case-insensitively** — the server's `"token"` maps to `Token` with no
annotation. Gson is case-**sensitive**: add `@SerializedName` to every field or the whole
response parses as nulls.
**Risk.** HIGH

### S01-R13 — User ID is written back onto the response
**Rule.** After a successful login the trimmed User ID is assigned onto the response object
before it is stored.
**Source.** `:366`
**Why.** The server's response may not echo it, and the session needs it.
**Risk.** MED

### S01-R14 — `PhysicalVeriID` is cached into a global here ⚠️
**Rule.** `AppConstants.PhysicalVeriID = response.PhysicalVeriID` — **the only assignment in
the codebase.**
**Source.** `:367`
**🐞 [F-001](../README.md).** Session resume does not repopulate it, so Stage 10 sends
`pvid: 0` after a process kill.
**Java.** Put it on the session object. The bug disappears structurally.
**Risk.** HIGH

### S01-R15 — Routing after login
**Rule.** Bank login → bank flow; PV login → PV dashboard.
**Source.** `:375-380`
**Risk.** MED

---

## 5. Errors

### S01-R16 — Three distinct failures
**Rule.**

| Condition | Handling |
|---|---|
| null response (network/parse) | its own alert |
| `Status == false` | the server's `Message` |
| already logged in elsewhere | a dedicated alert, detected by inspecting the message text |

**Source.** `:380-404`
**Note.** "Already logged in elsewhere" is detected by **string-matching the server's English
prose**. Fragile — any server-side wording change breaks it silently.
**Action.** This is the `reason`-code question already open in
[C02-R18](../cross-cutting/C02-api-contracts.md). Raise it with the backend team.
**Risk.** HIGH

---

## 6. Keyboard

### S01-R17 — The form scrolls to the focused field
**Rule.** The page subscribes to keyboard show/hide and scrolls the focused field into view.
**Source.** `:146-200`
**Java.** `android:windowSoftInputMode="adjustResize"` + a `ScrollView`. The manual tracking
disappears.
**Risk.** MED

### S01-R18 — Focus styling
**Rule.** Field borders change on focus; the captcha display keeps its own colours.
**Source.** `:172-195`
**Risk.** LOW

---

## 7. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Empty User ID, tap Login | "enter user id" alert; captcha unchanged |
| 2 | Wrong captcha | mismatch alert, captcha **regenerates**, field clears |
| 3 | Captcha in the wrong case | **accepted** (R03) |
| 4 | Double-tap Login | one request only (R07) |
| 5 | Wrong password | server message shown, captcha regenerates |
| 6 | Airplane mode | network alert, not a crash |
| 7 | PV login | lands on the PV dashboard |
| 8 | Bank login | lands on the bank flow; `IFSC_Code` populated |
| 9 | User ID over 50 chars | truncated at 50 |
| 10 | Tap show-password | plaintext, icon toggles |

---

## 8. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 18 rules traced; R10 partial |
| R2 | — | pending — **no credentials used in this session by policy** |
| R3 | — | pending |
