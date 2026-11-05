# C01 — Session & authentication

| | |
|---|---|
| **MAUI source** | `Util/SessionManager.cs` (226 lines), `Platforms/Android/MainActivity.cs`, `App.xaml.cs` |
| **Java target** | `core/session/SessionManager`, `core/session/SessionTimer`, `core/storage/PreferenceStore` |
| **Review state** | **R1 PASSED** — all rules traced |
| **Rules** | 19 (5 HIGH) |

---

## 1. What this is honestly

> The server issues a token but **the API does not authenticate on any endpoint except
> logout**. There is nothing to keep alive server-side. This mechanism only decides whether
> the app should still be showing a logged-in screen.

A user who clears app data bypasses it entirely. Real expiry needs server-side token
validation. Stated here so nobody reading the Java port mistakes it for a security boundary —
and so nobody "hardens" it with client-side tricks that add complexity and no safety.

See [C02-R05](C02-api-contracts.md) — attaching the Bearer token to every request in the Java
port is cheap insurance against the server starting to enforce auth.

---

## 2. Timing

### C01-R01 — 25-minute idle timeout, 2-minute warning
**Rule.** The session ends after **25 minutes** without user activity. A warning is raised
when **2 minutes** remain.
**Source.** `Util/SessionManager.cs:17-20`
**Risk.** MED

### C01-R02 — Activity means a touch, nothing else
**Rule.** The idle clock resets on the officer touching the screen — taps, scrolls, keystrokes.
**A background upload finishing is not activity and deliberately does not reset the clock.**
**Source.** `Util/SessionManager.cs:83-92`, driven from `MainActivity.DispatchTouchEvent`
**Java.** Override `Activity.dispatchTouchEvent` in a `BaseActivity`, call `touch()`, return
`super`. Do **not** reset from network callbacks, timers, or lifecycle events — an app left
open on a desk must still expire.
**Risk.** MED

### C01-R03 — Remaining time is computed, never counted down
**Rule.** Time remaining is derived as `Timeout - (now - lastActive)` on each read.
**Source.** `Util/SessionManager.cs:44-45`
**Why.** The warning dialog counts down from this rather than from its own counter, so a tap
elsewhere that resets the clock is reflected immediately. A dialog with its own ticker would
keep counting toward an expiry that is no longer coming.
**Risk.** MED

### C01-R04 — The warning fires once per idle period
**Rule.** A flag suppresses repeat warnings. It is cleared on every touch and on session start,
so a fresh idle period warns again.
**Source.** `Util/SessionManager.cs:56`, `:78`, `:89`
**Risk.** LOW

### C01-R05 — Checked on a foreground timer and on resume
**Rule.** The expiry check runs on a periodic foreground timer **and** whenever the app returns
to the foreground.
**Source.** `Util/SessionManager.cs:205-226`, driven from `App.xaml.cs`
**Why resume matters.** A timer does not run while the process is backgrounded. Without the
resume check, an app backgrounded for an hour would appear logged in until the next tick.
**Java.** `ProcessLifecycleOwner` `ON_RESUME` + a `Handler` or coroutine tick while foregrounded.
**Risk.** HIGH

### C01-R06 — Events are raised on the main thread
**Rule.** Both `Warning` and `Expired` are dispatched to the main thread so handlers can touch
the UI directly.
**Source.** `Util/SessionManager.cs:213`, `:221`
**Risk.** MED

---

## 3. Persistence

### C01-R07 — Three keys, persisted not in-memory
**Rule.** Stored in shared preferences:

| Key | Holds |
|---|---|
| `session_last_active_utc` | last activity, **ISO-8601 round-trip (`"O"`) UTC** |
| `session_is_bank_login` | which login page to return to |
| `session_login_json` | the serialised login response |

**Source.** `Util/SessionManager.cs:23-28`
**Why persisted.** The OS can kill the app in the background; on relaunch there would
otherwise be nothing to compare against.
**Java.** `SharedPreferences`. Store the timestamp as **epoch millis**, not a formatted string —
`DateTime.Parse` with round-trip style has no exact Java equivalent and locale-formatted dates
are a classic cross-platform parse failure. Behaviour is identical; the representation is
safer.
**Risk.** MED

### C01-R08 — The login response is persisted, never the password
**Rule.** The stored blob holds identifiers and the token. No password is written anywhere.
**Source.** `Util/SessionManager.cs:25-28`, `:66-71`
**Java.** The token is a credential. Use `EncryptedSharedPreferences` for this key. The MAUI
app does not — that is an improvement worth making and worth recording.
**Risk.** HIGH

### C01-R09 — Resume requires both the timestamp and the login blob
**Rule.** The session resumes only when the stored timestamp parses, is within the window,
**and** the login blob deserialises to a non-null object. Any failure ends the session and
returns to login.
**Source.** `Util/SessionManager.cs:154-203`
**Why.** Without the login response there is nothing to rebuild the dashboard from — both the
dashboard and the pre/post screen take it as a constructor argument. A half-restored state is
worse than no session.
**Test.** Corrupt `session_login_json` by hand, relaunch → login screen, no crash.
**Risk.** HIGH

### C01-R10 — An expired stored session is cleared, not just ignored
**Rule.** When the stored timestamp is older than the timeout, the full end-of-session cleanup
runs before returning "no session".
**Source.** `Util/SessionManager.cs:160-164`
**Risk.** LOW

---

## 4. Logout

### C01-R11 — The server is told, fire-and-forget
**Rule.** `POST {API_BASE}/LogoutPVUser`, `Authorization: Bearer <token>`, body `{}`,
10-second timeout. The result is logged and otherwise ignored.
**Source.** `Util/SessionManager.cs:112-151`
**Why not awaited.** An officer with no signal, or the idle timer firing with nobody watching,
must still be logged out locally. A stale flag on the server is the lesser problem, and the
token's own expiry clears it anyway.
**Java.** Fire the call, do not join it, and swallow every exception. Do not use a
`lifecycleScope` — the screen is being torn down.
**Risk.** MED

### C01-R12 — Body is `{}`, not null
**Rule.** An empty JSON object, not an empty body.
**Source.** `Util/SessionManager.cs:136-137`
**Why.** The token identifies the session so the body carries nothing, but posting null is not
reliable.
**Risk.** LOW

### C01-R13 — Bank logout is not called
**Rule.** The server call is skipped when there is no token **or** the session was a bank login.
**Source.** `Util/SessionManager.cs:117-118`
**Why.** The bank login does not issue a token and has no confirmed logout endpoint.
**Open.** [C02 Q3](C02-api-contracts.md) — is there one?
**Risk.** LOW

### C01-R14 — Token is read before state is cleared
**Rule.** The logout call captures the token **before** the local teardown nulls it.
**Source.** `Util/SessionManager.cs:95-99`
**Why.** Ordering bug waiting to happen — clearing first sends a logout with no credential.
**Risk.** MED

### C01-R15 — Teardown clears everything
**Rule.** On end: login response nulled, `session_login_json` removed, active flag false,
warned flag false, last-active reset, `session_last_active_utc` removed.
**Source.** `Util/SessionManager.cs:101-110`
**Note.** `session_is_bank_login` is **deliberately kept** — it decides which login page to
show next.
**Risk.** MED

---

## 5. Two login paths

### C01-R16 — PV and Bank are separate flows
**Rule.** Two endpoints, two login screens, two sets of downstream screens. The session records
which one was used and returns there on expiry.
**Source.** `Util/SessionManager.cs:38`, `:73-81`
**Risk.** MED

### C01-R17 — Only PV issues a token
**Rule.** `AuthenticatePVLogin` returns a Bearer token. The bank login does not.
**Source.** `Util/SessionManager.cs:117`
**Risk.** MED

### C01-R18 — `PhysicalVeriID` is captured at login into a global
**Rule.** `PhysicalVeriID` is read from the login response and stored in a static, because
`UnitVerificationPage` needs it as `pvid` for the OTP endpoints but is not constructed from the
login response.
**Source.** `Util/Appconstants.cs:33`, consumed by [C02-R10](C02-api-contracts.md)
**Java.** Put it on the session object and inject the session. A static that must be populated
before another screen works is a null-pointer waiting for a process restart — exactly the case
C01-R09 handles for everything else, and this field is **not** covered by it.
**Risk.** HIGH

> ### 🐞 Finding F-001 — `pvid` is lost when a killed session resumes
>
> **Confirmed by code trace, 2026-11-05.** `AppConstants.PhysicalVeriID` is assigned at
> exactly one place in the entire codebase — `LoginPage.xaml.cs:366`, inside the login flow.
> `SessionManager.TryResume()` (`:154-203`) restores `Current` from `session_login_json` and
> sets `_active`, `_lastActive` and `WasBankLogin` — but **never repopulates
> `AppConstants.PhysicalVeriID`**.
>
> So after the OS kills the process and the officer reopens the app within the 25-minute
> window, the session resumes to the dashboard looking perfectly normal, but
> `AppConstants.PhysicalVeriID` holds the `int` default of **0**. Stage 10 then sends
> `pvid: 0` to `SendPVOTPtoApplicant` (`UnitVerificationPage.xaml.cs:1798`) and
> `ValidateApplicantPVOTP` (`:1907`).
>
> **Expected symptom:** OTP generation fails, or silently sends to the wrong record, only
> after a background process kill — which is why it would be near-impossible to reproduce
> deliberately and easy to dismiss as a network blip.
>
> **The value is sitting right there.** `Current.PhysicalVeriID` is restored correctly; only
> the static copy is stale. The MAUI fix is one line in `TryResume`. The Java port avoids it
> structurally by reading `pvid` off the session object rather than a global.
>
> **Status: PROVEN ON DEVICE, 2026-11-05.** A temporary diagnostic was added to the resume
> path, the app force-stopped and relaunched, and the log read:
>
> ```
> [F001] static AppConstants.PhysicalVeriID=0 | restored.PhysicalVeriID=9674
> [SESSION] resumed — bank=False, 24.1 min left
> ```
>
> The session resumed correctly with 24.1 minutes left and the login response restored with
> `PhysicalVeriID=9674`. **The static that Stage 10 actually reads was `0`.** The diagnostic
> was reverted and a clean build reinstalled.
>
> Confirmed by exhaustive grep: `AppConstants.PhysicalVeriID` is assigned at **exactly one
> line in the entire codebase** — `LoginPage.xaml.cs:366`, inside the login flow. Neither
> `TryResume` nor `MainPage.NavigateToLogin` nor the landing-page constructors touch it.
>
> **The MAUI fix is one line** in `SessionManager.TryResume()`, after `Current` is restored:
>
> ```csharp
> AppConstants.PhysicalVeriID = Current.PhysicalVeriID;
> ```
>
> **Affects the APK currently going to the Play Store, not just the port.** Raise with the
> tester.

---

## 6. Credentials

### C01-R19 — Credentials are RSA-encrypted before leaving the device
**Rule.** `{ "UserID", "Password" }` → JSON → RSA-2048 PKCS#1 v1.5 → Base64 → wrapped as
`{ "data": "<base64>" }`.
**Source.** `LoginPage.xaml.cs:532-545`, [C10-R08](C10-cryptography.md)
**Why.** Resolves VAPT #2 (CWE-311, weak/no encryption of credentials in transit).
**Risk.** HIGH

---

## 7. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Log in, idle 23 min untouched | warning dialog appears |
| 2 | Tap during the warning | dialog dismisses, full 25 min restored |
| 3 | Idle a further 25 min | logged out, correct login page for the path used |
| 4 | Log in, kill the process, relaunch within 25 min | resumes to dashboard, no re-login |
| 5 | Log in, kill, wait past 25 min, relaunch | login screen |
| 6 | Corrupt `session_login_json`, relaunch | login screen, no crash |
| 7 | **Resume a killed session, go to Stage 10, Generate OTP** | **does `pvid` survive? — C01-R18** |
| 8 | Start a long upload, do not touch the screen, wait 25 min | still expires — upload is not activity |

Step 7 is the one that may find a live bug. Run it first.

---

## 8. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 19 rules traced |
| R2 | — | pending — checklist above, step 7 prioritised |
| R3 | — | pending |
