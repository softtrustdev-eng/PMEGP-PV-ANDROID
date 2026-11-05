# S02 — Select Verification Type

| | |
|---|---|
| **MAUI source** | `PrePostVerificationPage.xaml.cs` (398), `.xaml` (145) |
| **Java target** | `ui/dashboard/VerificationTypeActivity` |
| **Review state** | **R1 PASSED** |
| **Rules** | 8 (2 HIGH) |

---

## 1. Purpose

The screen immediately after a PV login. Shows the user ID and offers the verification types
this account may perform.

---

## 2. Rules

### S02-R01 — Shows the logged-in user ID
**Rule.** Header area reads `UserID : <id>`, the id in orange.
**Source.** `.xaml`
**Risk.** LOW

### S02-R02 — Options are data-driven, not hardcoded ⚠️
**Rule.** The list of verification types comes from the server. Observed in this session: only
**Post-Verification**. `Pre-Verification` exists in the codebase.
**Source.** `:280` builds a request carrying `PhysicalVeriID`
**Java.** Render from the response. Do **not** hardcode a single card because only one appeared
during testing — an account with both would silently lose one.
**Risk.** HIGH

### S02-R03 — Selecting a type loads that dashboard
**Rule.** Tapping a card navigates to the dashboard for that type.
**Source.** `:280-300`
**Risk.** MED

### S02-R04 — Logout in the header
**Rule.** A logout icon sits top-right. Tapping asks *"Are you sure you want to logout?"* /
*"Do you want to logout?"*, then ends the session.
**Source.** `:` logout handlers
**See.** [C01-R11](../cross-cutting/C01-session-and-auth.md) — the server call is
fire-and-forget and must never block.
**Risk.** MED

### S02-R05 — Error messages are developer-facing ⚠️
**Rule.** Failures surface raw strings: `HTTP Error: {StatusCode}`,
`Empty response from server`, `API returned unsuccessful response`,
`Failed to navigate: {message}`.
**Source.** throughout
**Note.** These are debug strings shown to a field officer. A status code or an exception
message is not actionable by them.
**Action.** Replace with officer-facing wording in the Java port — but **confirm with the
tester**, since they may be using these to report issues.
**Risk.** MED

### S02-R06 — A resumed session lands here
**Rule.** Resuming a killed PV session restores to this screen, not login.
**See.** [C01-R09](../cross-cutting/C01-session-and-auth.md)
**Verified.** Observed on device twice this session.
**Risk.** HIGH

### S02-R07 — Takes the login response as a constructor argument
**Rule.** The screen is constructed from the login response — which is why the session must
persist and restore that whole object.
**Source.** `:280`
**See.** [C01-R07 / C01-R09](../cross-cutting/C01-session-and-auth.md)
**Risk.** MED

### S02-R08 — Standard header
**See.** [C07](../cross-cutting/C07-header-and-theme.md)
**Risk.** LOW

---

## 3. R2 checklist

| # | Step | Expected | State |
|---|---|---|---|
| 1 | Log in as PV | this screen, correct user ID | ✅ |
| 2 | Tap Post-Verification | PV dashboard loads | ✅ |
| 3 | Kill the process, relaunch | **returns here**, not login | ✅ |
| 4 | Tap logout, confirm | returns to login, session cleared | ⬜ |
| 5 | Airplane mode, tap a type | graceful error, not a raw status code | ⬜ |
| 6 | **An account with both types** | **both cards render** (R02) | ⬜ |

---

## 4. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 8 rules traced |
| R2 | 2026-11-05 | ⚠️ 3 of 6 confirmed on device |
| R3 | — | pending |
