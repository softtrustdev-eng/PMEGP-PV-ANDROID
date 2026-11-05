# C03 — Input sanitisation

| | |
|---|---|
| **MAUI source** | `Util/InputSanitizer.cs` |
| **Java target** | `core/security/InputSanitizer` |
| **Origin** | VAPT finding — *"Text fields accept any characters and lengthy inputs without restrictions"* |
| **Review state** | **R2 PASSED** — 17 assertions executed against the real implementation |
| **Rules** | 12 (3 HIGH) |

---

## 1. What this is and is not

This strips characters that only appear in injection payloads, plus emoji and invisible
formatting, from free-text fields as they are typed or pasted.

**It is defence in depth, not the fix.** The app is not the trust boundary — the API is
reachable without it. Output encoding wherever this text is later rendered, and server-side
validation, remain the controls that count. That half of the VAPT finding is still open and
belongs to the backend team. Do not let the Java port's existence be read as closing it.

---

## 2. The filter

A character is **removed** when any rule below matches. Everything else is kept.

### C03-R01 — Injection characters
**Rule.** Remove `<` `>` `"` `\`.
**Source.** `Util/InputSanitizer.cs:22`
**Why.** `<` and `>` open and close an HTML tag, so removing them makes `<script>`
unenterable. `"` closes an HTML attribute. `\` escapes in most injection grammars and
appears in no Indian address.
**Risk.** HIGH

### C03-R02 — Control characters, except tab
**Rule.** Remove characters where `Character.isISOControl` is true, **except** `\t`, and
except `\n`/`\r` when the field is multi-line.
**Source.** `Util/InputSanitizer.cs:44-49`
**Why.** Invisible, meaningless in these fields, and only ever arrive by paste. Tab is kept —
harmless, and some pasted addresses contain one.
**Risk.** MED

### C03-R03 — Non-BMP emoji, via surrogates
**Rule.** Remove any UTF-16 surrogate code unit.
**Source.** `Util/InputSanitizer.cs:55-56`
**Why.** Every emoji outside the Basic Multilingual Plane — 😀 🏭 flags, skin-tone variants —
is stored as a surrogate pair. No script used in an Indian name, address or remark needs one,
so dropping surrogates removes the whole non-BMP range in a single test.
**Java.** `Character.isSurrogate(c)` while iterating `char`, exactly as C# does. Because
*both* halves are surrogates, both are dropped and no orphan remains. **Do not** iterate by
code point here — that would require re-joining and gains nothing.
**Risk.** MED

### C03-R04 — Variation selectors, by range ⚠️
**Rule.** Remove `U+FE00`..`U+FE0F`.
**Source.** `Util/InputSanitizer.cs:58-64`
**Why this is a range and not a category.** Variation selectors are Unicode category
**`Mn` (NonspacingMark)** — the *same category as every Devanagari matra*. Filtering by
category would delete the vowel signs out of Hindi text, turning `श्री` into `शरी`. The range
is surgical: `U+FE00..FE0F` is the "render the previous character as emoji" switch and
nothing else.
**Note.** This was caught by the test suite, not by reading. Keep the Devanagari assertion.
**Risk.** HIGH

### C03-R05 — BMP pictographs
**Rule.** Remove characters of category `OTHER_SYMBOL` (`So`) — ☺ ✈ ❤ ☢ ★ and the dingbats.
**Source.** `Util/InputSanitizer.cs:60-63`
**Safe because.** Letters in every Indian script are `Lo` (OtherLetter) and the rupee sign ₹
is `Sc` (CurrencySymbol). Neither is touched.
**Known side effect.** `U+00B0` DEGREE SIGN is also `So` and is removed. No PMEGP field needs it.
**Java.** `Character.getType(c) == Character.OTHER_SYMBOL`
**Risk.** MED

### C03-R06 — Format characters
**Rule.** Remove category `FORMAT` (`Cf`).
**Source.** `Util/InputSanitizer.cs:65-68`
**Why.** Covers the zero-width joiner that glues emoji sequences, and the bidi overrides used
to disguise pasted text. All invisible; none belongs in these fields.
**Java.** `Character.getType(c) == Character.FORMAT`
**Risk.** MED

### C03-R07 — Enclosing marks
**Rule.** Remove category `ENCLOSING_MARK` (`Me`) — the keycap combiner that turns `1` into 1️⃣.
**Source.** `Util/InputSanitizer.cs:65-69`
**Safe because.** No Indian script uses an enclosing mark.
**Risk.** LOW

### C03-R08 — What must NOT be removed
**Rule.** `& / - . ' ( ) #`, digits, spaces, commas, and all Indian-script letters are kept.
**Source.** `Util/InputSanitizer.cs:7-12`
**Why.** An Indian unit name or postal address legitimately contains these —
*"Shah & Sons"*, *"Plot 12/4"*, *"D'Souza"*. Blocking that punctuation stops officers entering
real data, which is a worse outcome than the risk it removes.
**Risk.** HIGH — over-blocking here is a field-usability failure, and it is the failure mode
a porting developer is most likely to introduce by "tightening" the filter.

### C03-R09 — Null and empty pass through
**Rule.** `null` returns `null`; `""` returns `""`. Callers need no guard.
**Source.** `Util/InputSanitizer.cs:34-35`
**Risk.** LOW

### C03-R10 — Newline handling is per-field
**Rule.** A flag selects whether `\n`/`\r` survive. Single-line fields drop them (they only
arrive by paste); multi-line fields keep them.
**Source.** `Util/InputSanitizer.cs:28-32`
**Risk.** LOW

---

## 3. How it is wired to fields

### C03-R11 — Strip on typing, with a re-entrancy guard
**Rule.** Sanitising runs on every text change — so a paste is cleaned too, not just
keystrokes. The cleaned value is written back **only when it differs** from what was typed.

```java
// C03-R11
field.addTextChangedListener(new TextWatcher() {
    public void afterTextChanged(Editable s) {
        String cleaned = InputSanitizer.clean(s.toString(), allowNewLines);
        if (!cleaned.equals(s.toString())) field.setText(cleaned);  // guard is load-bearing
    }
});
```

**Why the guard matters.** Writing the text inside the change callback fires the callback
again. Without the inequality check it is an infinite loop; with it, the second pass finds
nothing to change and stops.
**Known wart.** Rewriting the text moves the caret to the end. Only happens when a character
was actually rejected, which is rare and is itself the signal that something was blocked.
In Java, prefer an `InputFilter` where possible — it rejects the character without rewriting
the field and keeps the caret.
**Source.** `UnitVerificationPage.xaml.cs:7114`, `:7428`; `DetailsPage.xaml.cs` remarks modal
**Risk.** HIGH

### C03-R12 — Read-only fields are never sanitised
**Rule.** Fields that are not editable are skipped entirely.
**Source.** `UnitVerificationPage.xaml.cs:7420-7422`
**Why.** Their text is server data being displayed, not officer input. Rewriting it would
alter what the record actually says.
**Risk.** MED

---

## 4. Coverage

| Screen | Covered | Note |
|---|---|---|
| Unit Verification — text entries | ✅ | `:7114` |
| Unit Verification — editors | ✅ | `:7428` |
| Bank Physical Verification | ✅ | `:4992`, `:5163` |
| Inspection Details | ✅ | `:1855`, `:1938` |
| Traceability remarks modal | ✅ | plus a 250-character cap |
| Bank Verification | **not wired — correct** | 4 hidden mirror entries, 6 already digits-only via a numeric allowlist (strictly stronger than this blocklist), 2 read-only date fields. Adding the sanitiser there would be dead code. |

**Java note.** Keep this table accurate. A developer who sees "BankVerification has no
sanitiser" and adds it is adding dead code; a developer who removes the numeric allowlist and
substitutes this blocklist is **weakening** that screen.

---

## 5. Length caps

Length is capped per field, not centrally — see each screen document. Two confirmed:

| Field | Cap | Source |
|---|---|---|
| Employee counts (Stage 7, ×11) | 3 | `UnitVerificationPage.xaml.cs:4121` |
| Traceability remarks | 250 | `DetailsPage.xaml.cs` remarks modal |

**Rule.** Caps are enforced by the platform control, not by a text-change handler. Android:
`InputFilter.LengthFilter` / `android:maxLength`. The platform refuses the keystroke and
truncates a paste *before* the value is assigned; a handler can only react afterwards, which
lets a 10,000-character paste land and then rewrites it, with a visible flash.

**There is no project-wide maximum length instruction.** The VAPT finding mentions "lengthy
inputs" but no cap was specified by the client. Do not invent one during the port — carry the
caps listed in the screen documents and no others.

---

## 6. Verified test vectors

Executed against the real `InputSanitizer.cs` compiled into a standalone harness.
**17/17 pass.** Port these verbatim as the Java unit test.

| Input | Expected output | Proves |
|---|---|---|
| `<script>alert(1)</script>` | `scriptalert(1)/script` | R01 |
| `" onload="x` | ` onload=x` | R01 |
| `a\b` | `ab` | R01 |
| `Unit 😀 OK` | `Unit  OK` | R03 |
| `Good ❤️ job` | `Good  job` | R04 + R05 |
| `☀✈★` | `` | R05 |
| `a‍b` | `ab` | R06 |
| `x‮y` | `xy` | R06 |
| `1⃣2⃣` | `12` | R07 |
| `a\u0000\u0007b` | `ab` | R02 |
| `a\nb` *(single-line)* | `ab` | R10 |
| `a\nb` *(multi-line)* | `a\nb` | R10 |
| `Shah & Sons, Plot 12/4, D'Souza (Unit-2) #3` | unchanged | **R08** |
| `₹1,20,000` | unchanged | **R08** |
| `श्री अक्षर` | unchanged | **R08 / R04** |
| `null` | `null` | R09 |
| `""` | `""` | R09 |

The last five are the important ones. The first twelve prove the filter blocks; those five
prove it does not over-block. A port that passes only the first twelve has broken data entry
for every officer in the field.

---

## 7. Device verification

Confirmed on a physical device (Android, Traceability remarks field):

- 372 characters typed → field stopped at exactly 250
- `Unit` + 😀 + `closed` typed via the system emoji keyboard → field reads `Unitclosed`

`adb shell input text` cannot transmit `<` or `>` — it converts them to `!`. The injection
half of R01 is therefore proven by the harness, not on device. A manual paste test is the
only way to confirm it end-to-end; it is listed in the R2 checklist for each screen.

---

## 8. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ all 12 rules traced |
| R2 | 2026-11-05 | ✅ 17/17 assertions; length + emoji confirmed on device; `<`/`>` harness-only |
| R3 | — | pending |
