# S06 — Applicant list (Pre/Post)

| | |
|---|---|
| **MAUI source** | `DetailsPagePreVer.xaml.cs` (716), `.xaml` (134) |
| **Java target** | same `ApplicantListActivity`, pre/post data source |
| **Review state** | **R1 PASSED** |
| **Rules** | 6 (1 HIGH) |

> Structurally [S04](S04-applicant-list-pv.md). Only differences are recorded.

---

## 1. Differences from S04

### S06-R01 — Endpoint
**Rule.** `GetPrePostalVerificationList`.
**Open.** Not captured.
**Risk.** MED

### S06-R02 — Detail endpoint
**Rule.** `GetPrePostalVerificationDetailsByApplID` for a single record.
**Risk.** MED

### S06-R03 — Save endpoint
**Rule.** `UpdatePostalPreVarDetails`.
**Risk.** HIGH

### S06-R04 — Search placeholder
**Rule.** `DetailsPagePreVer.xaml:72`.
**See.** [S05-R03](S05-applicant-list-bank.md) — verify all three match.
**Risk.** LOW

### S06-R05 — DigiPIN is available here too
**Rule.** This flow also calls `DigiPinEncodeAsync` — one of three screens that do.
**See.** [C02-R12](../cross-cutting/C02-api-contracts.md)
**Risk.** MED

### S06-R06 — Standard header
**See.** [C07](../cross-cutting/C07-header-and-theme.md)
**Risk.** LOW

---

## 2. Open questions

| # | Question |
|---|---|
| Q1 | Is Pre-Verification reachable with the current test account? Only Post-Verification appeared on [S02](S02-verification-type.md). |
| Q2 | Does the pre/post flow have its own multi-stage form, or a single detail screen? |

**Both block R2.**

---

## 3. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ⚠️ 6 rules; flow not fully traced |
| R2 | — | blocked on Q1 |
| R3 | — | pending |
