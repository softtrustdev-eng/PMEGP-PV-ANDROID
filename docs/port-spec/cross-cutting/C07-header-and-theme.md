# C07 — Header & theme

| | |
|---|---|
| **MAUI source** | `Resources/Styles/Colors.xaml:44-133`, applied in all 11 page XAMLs |
| **Java target** | `ui/common/header/PageHeaderView`, `res/values/colors.xml`, `res/values/dimens.xml`, `themes.xml` |
| **Review state** | **R1 PASSED** — R2 done on 8 of 11 pages |
| **Rules** | 12 (2 HIGH) |

---

## 1. There is no toolbar

### C07-R01 — Every page hides the navigation bar and builds its own header ⚠️
**Rule.** All 11 pages set `NavigationPage.HasNavigationBar="False"` and hand-build a header
`Grid` as the first row of their layout.
**Source.** every page XAML
**Why it is recorded.** This was the root cause of the "toolbar size and colour inconsistency"
ticket — there was no toolbar to be inconsistent. Eleven hand-built headers had drifted into
four different greens and three different height strategies.
**Java.** Use **one** `PageHeaderView` compound view, or a shared `<include>`. Do **not** use
`ActionBar`/`Toolbar` with per-activity theming — that is how the drift happened. One view,
one layout file, one set of dimensions.
**Risk.** HIGH

### C07-R02 — The header is a shared style, not copied markup
**Rule.** Since the standardisation, every page references the same three keyed styles:
`PageHeaderStyle` (the Grid), `PageHeaderBackButtonStyle` (the chevron),
`PageHeaderTitleStyle` (the title).
**Source.** `Resources/Styles/Colors.xaml:105-133`
**Confirmed users.** All 11: BankPhysicalVerification, ApplicantDetails, DetailsPageBL,
DetailsPage, Dashboard, DetailsPagePreVer, InspectionDetails, PrivacyPolicy, BankVerification,
PrePostVerification, UnitVerification.
**Risk.** MED

### C07-R03 — Action buttons stay on their own pages
**Rule.** The shared styles cover the background, height, chevron and title **only**. Page-
specific actions — logout, search, refresh — are added by each page into its own header Grid.
**Why.** A universal header owning every possible action becomes a switch statement over page
identity. The *frame* is shared; the *contents* are not.
**Java.** `PageHeaderView` exposes `setTitle()`, `setOnBackClick()`, and an action slot
(`addAction(icon, listener)` or a `ViewGroup` the page populates).
**Risk.** MED

---

## 2. Exact values

### C07-R04 — Header container
| Property | Value |
|---|---|
| Background | `#4CAF50` |
| Height | **58 dp** — fixed `HeightRequest`, not padding-plus-content |
| Padding | `12, 0` |
| Column spacing | 4 |

**Source.** `Resources/Styles/Colors.xaml:46-48`, `:105-110`
**Why a fixed height.** So a page **without** a back button is exactly as tall as one with it.
Padding-plus-content makes height depend on the tallest child, which is what produced three
different header heights.
**Java.** `android:layout_height="@dimen/page_header_height"`, never `wrap_content`.
**Risk.** MED

### C07-R05 — Back chevron
| Property | Value |
|---|---|
| Glyph | `‹` (U+2039) — carried as part of the style |
| Colour | white |
| Background | transparent |
| Font size | 34 |
| Weight | bold |
| Size | 46 × 46 |
| Padding / Margin | 0 / 0 |
| Alignment | start, vertically centred |

**Source.** `Resources/Styles/Colors.xaml:112-124`
**Java note.** 46 dp is **below the 48 dp minimum touch target**. Keep the 46 dp visual box
but expand the touch area with `TouchDelegate` or a transparent 48 dp parent. Do not simply
grow it to 48 — that changes the layout the tester signed off.
**Risk.** MED

### C07-R06 — Title
| Property | Value |
|---|---|
| Colour | white |
| Font size | **16 sp** |
| Weight | bold |
| Alignment | start, vertically centred |
| Margin | `6, 0, 0, 0` |

**Source.** `Resources/Styles/Colors.xaml:126-133`
**Note.** Start-aligned, **not** centred.
**Risk.** LOW

### C07-R07 — Status bar
**Rule.** `#2D5F3F` — a darker green than the header.
**Source.** `BankPhysicalVerificationPage.xaml.cs` status-bar colour, changed from `#4CAF50`
**Risk.** LOW

---

## 3. Palette

### C07-R08 — Colours in use
| Token | Hex | Used for |
|---|---|---|
| `header_background` | `#4CAF50` | header, primary buttons |
| `status_bar` | `#2D5F3F` | system status bar |
| `primary` | `#1976D2` | field outlines, captions |
| `error` | `#D32F2F` | error text, invalid outlines |
| `hint` | `#9E9E9E` | placeholder text |
| `hint_text` | `#666666` | helper text (e.g. the 2 MB hint) |
| `on_surface` | `#1C1B1F` | editable field text |
| `on_surface_disabled` | `#79747E` | read-only field text |
| `surface` | `#FFFFFF` | editable field background |
| `surface_disabled` | `#F5F5F5` | read-only field background |
| `outline_disabled` | `#BDBDBD` | read-only field outline |
| `nav_secondary` | `#6C757D` | PREVIOUS / CANCEL buttons |
| `scrim` | `#AA000000` | modal overlay |

**Carried to.** `app/src/main/res/values/colors.xml` in the Java project.
**Risk.** LOW

### C07-R09 — Navigation buttons
**Rule.** Primary (`SAVE & NEXT`, `YES`) `#4CAF50`; secondary (`PREVIOUS`, `CANCEL`) `#6C757D`.
White bold text, corner radius 8–10, height 45–48.
**Source.** `Resources/Styles/Colors.xaml:135-143` (`NavButtonStyle`), `UnitVerificationPage.xaml.cs:4230-4236`
**Risk.** LOW

### C07-R10 — Disabled buttons dim, they do not hide
**Rule.** A disabled action button stays visible at **0.6 opacity**.
**Source.** `UnitVerificationPage.xaml.cs:4243-4244`
**Why.** The officer must see that SAVE & NEXT exists and is currently unavailable.
**Java.** `setEnabled(false)` + `setAlpha(0.6f)`. Material's default disabled tint is lighter —
set alpha explicitly to match.
**Risk.** LOW

---

## 4. Typography

### C07-R11 — Fonts
**Rule.** OpenSans Regular and OpenSans Semibold.
**Source.** `Resources/Fonts/`
**Carried to.** `app/src/main/res/font/` (lowercased filenames — Android resource names allow
only `a-z0-9_`).
**Risk.** LOW

---

## 5. Light only

### C07-R12 — No dark mode
**Rule.** The app renders light in all conditions. There is no dark theme and no
`prefers-color-scheme` handling.
**Java.** Base the theme on `Theme.Material3.Light.NoActionBar` and set
`AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_NO)` in the Application class — otherwise a
device in dark mode will recolour Material components and break the palette above.
**Risk.** MED — easy to miss, immediately visible to any tester with dark mode on.

---

## 6. R2 checklist

| Page | Header verified |
|---|:---:|
| Login | ✅ |
| Verification type selector | ✅ |
| Dashboard | ✅ |
| Applicant list (PV) | ✅ |
| Applicant list (Bank) | ✅ |
| Applicant list (Pre/Post) | ✅ |
| Applicant details | ✅ |
| Unit Verification | ✅ |
| Bank Physical Verification | ✅ |
| **Bank Verification** | ⬜ not reached |
| **Inspection Details** | ⬜ not reached |
| **Privacy Policy** | ⬜ not reached |

Plus: ⬜ confirm the app renders light on a device with dark mode enabled (C07-R12).

---

## 7. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 12 rules traced; all 11 pages confirmed on the shared style |
| R2 | 2026-11-05 | ⚠️ 9 of 12 checks done |
| R3 | — | pending |
