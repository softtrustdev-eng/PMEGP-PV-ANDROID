# S03 — Dashboard

| | |
|---|---|
| **MAUI source** | `DashboardPage.xaml.cs` (870), `.xaml` (205) |
| **Java target** | `ui/dashboard/DashboardActivity` + `DashboardViewModel` |
| **Review state** | **R1 PASSED** |
| **Rules** | 12 (3 HIGH) |

---

## 1. Purpose

Counts by category, each card opening the matching applicant list.

---

## 2. Cards

| Card | List status key |
|---|---|
| Total Reports | — |
| Working Units | `WorkingApplications` |
| Defunct Units | `DefunctApplications` |
| Non-Traceable Units | `NonTraceable` |
| Reports Pending | `PendingApplications` |
| New Loan | — |

Observed on device: 223 / 136 / 9 / 18 / 60.

---

## 3. Rules

### S03-R01 — Card → status mapping, with aliases ⚠️
**Rule.** Card identifiers map to list status keys, accepting both singular and plural forms:

```
reports_pending       -> PendingApplications
working_unit(s)       -> WorkingApplications
defunct_unit(s)       -> DefunctApplications
non_traceable_unit(s) -> NonTraceable
anything else         -> PendingApplications
```

**Source.** `:590-598`
**Note.** The fallback is `PendingApplications`. An unrecognised card silently opens the
pending list rather than failing. Carry it, but the aliases suggest the server has changed the
identifiers at least once.
**Risk.** HIGH

### S03-R02 — Status keys must match verbatim ⚠️
**Rule.** These exact strings flow into `DetailsPage` and determine whether a case is concluded.
**See.** [C09-R03](../cross-cutting/C09-case-status-and-readonly.md) —
`"DefunctApplications"` and `"NonTraceable"` are compared literally.
**Java.** An **enum** with `@SerializedName`, not loose strings. A typo here unlocks editing on
a concluded case.
**Risk.** HIGH

### S03-R03 — PV counts require a usable `PhysicalVeriID`
**Rule.** The PV counts endpoint is called only when `PhysicalVeriID > 0`.
**Source.** `:292-299`
**Why.** No usable id means nothing to show. This used to fall through to an empty dashboard
with no explanation.
**Relates to.** [F-001](../README.md) — if the global is 0 after a resume, does this screen
also degrade? **Check during R2.**
**Risk.** HIGH

### S03-R04 — Three different count endpoints
**Rule.**

| Flow | Endpoint |
|---|---|
| PV | `GetPVDashboardData` |
| Pre/Post | `GetPrePostalVerificationDashboardData` |
| Bank | `GetBankApplicationCount?bankIfsc=<ifsc>` |

**Source.** `:` dashboard loaders
**Note.** The bank one is a **GET with a query parameter**; the other two are POSTs.
**Risk.** MED

### S03-R05 — Bank counts key on IFSC
**Rule.** The bank count request uses `IFSC_Code` from the login response.
**Source.** `:` bank path
**See.** [S01-R11](S01-login.md) — IFSC presence is also what marks a bank login.
**Risk.** MED

### S03-R06 — An empty list navigates back to the dashboard
**Rule.** Opening a category with no applications returns the officer to the dashboard rather
than showing a blank list.
**Source.** commit `a359c07`
**Risk.** MED

### S03-R07 — User ID in the header
**Rule.** Shows the logged-in user id, falling back to `PhysicalVeriID` when the name is absent.
**Source.** `:634`
**Risk.** LOW

### S03-R08 — Logout with confirmation
**See.** [S02-R04](S02-verification-type.md)
**Risk.** MED

### S03-R09 — Standard header
**See.** [C07](../cross-cutting/C07-header-and-theme.md)
**Risk.** LOW

### S03-R10 — Counts refresh on return
**Rule.** Returning from a list reloads the counts.
**Risk.** MED

### S03-R11 — `PhysicalVeriID` is passed onward
**Rule.** It is included in requests built here.
**Source.** `:447`, `:725`
**Risk.** MED

### S03-R12 — Session touch on interaction
**Rule.** Taps reset the idle clock like anywhere else.
**See.** [C01-R02](../cross-cutting/C01-session-and-auth.md)
**Risk.** LOW

---

## 4. R2 checklist

| # | Step | Expected | State |
|---|---|---|---|
| 1 | PV dashboard | five counts render | ✅ |
| 2 | Tap Reports Pending | pending list | ✅ |
| 3 | Tap Defunct Units | list opens; cases **read-only** (R02 + C09) | ⬜ |
| 4 | Tap Non-Traceable Units | list opens; **no blinking** (C09-R14) | ⬜ |
| 5 | Tap a zero-count card | returns to dashboard (R06) | ⬜ |
| 6 | **Resume a killed session, open the dashboard** | **counts load, or are they 0?** (R03 / F-001) | ⬜ |
| 7 | Bank login dashboard | counts keyed by IFSC | ⬜ |

Step 6 may widen F-001 beyond the OTP path.

---

## 5. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 12 rules traced |
| R2 | 2026-11-05 | ⚠️ 2 of 7 confirmed on device |
| R3 | — | pending |
