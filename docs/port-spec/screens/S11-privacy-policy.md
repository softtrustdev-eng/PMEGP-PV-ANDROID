# S11 — Privacy Policy

| | |
|---|---|
| **MAUI source** | `PrivacyPolicyPage.xaml.cs` (35), `PrivacyPolicyPage.xaml` (33) |
| **Java target** | `ui/common/WebContentActivity` |
| **Review state** | **R1 PASSED** |
| **Rules** | 5 (1 HIGH) |

---

## 1. Purpose

Displays the hosted privacy policy in a WebView. The smallest screen in the app.

---

## 2. Rules

### S11-R01 — Loads a remote URL
**Rule.** `{BASE_HOST}/Home/PrivacyPolicy` — the same host as the API.
**Source.** `Util/Appconstants.cs:26`
**Note.** It follows `BASE_HOST`, so a debug build shows the **UAT** policy.
**See.** [C02-R02](../cross-cutting/C02-api-contracts.md)
**Risk.** MED

### S11-R02 — TLS validation is bypassed ⚠️
**Rule.** The WebView uses the bypassing client, because the UAT host's certificate does not
match an IP SAN.
**Source.** `Platforms/Android/SslBypassWebViewClient.cs`
**Java.** **Debug source set only.** A `WebViewClient` overriding `onReceivedSslError` to
proceed is an automatic Play Store rejection. Release must use the default.
**See.** [C02-R03](../cross-cutting/C02-api-contracts.md)
**Risk.** HIGH

### S11-R03 — The logo was removed from the header
**Rule.** The header carries no logo image — removed deliberately.
**Source.** commit `dfc096d`
**Why recorded.** A developer porting from an older screenshot may add it back.
**Risk.** LOW

### S11-R04 — Standard header
**Rule.** Uses the shared header: `#4CAF50`, 58 dp, chevron, start-aligned title.
**See.** [C07](../cross-cutting/C07-header-and-theme.md)
**Risk.** LOW

### S11-R05 — Back returns, with no confirmation
**Rule.** Back and the chevron both return immediately. There is no leave confirmation — this
is not a form.
**See.** [C06-R01](../cross-cutting/C06-navigation-and-back.md) — the confirmation applies only
to the three verification forms.
**Risk.** MED

---

## 3. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Open from the login screen | policy renders |
| 2 | Back | returns immediately, no dialog |
| 3 | Header | matches every other page |
| 4 | **Release build, offline** | fails gracefully, no TLS bypass (R02) |

---

## 4. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 5 rules traced |
| R2 | — | pending — screen not reached this session |
| R3 | — | pending |
