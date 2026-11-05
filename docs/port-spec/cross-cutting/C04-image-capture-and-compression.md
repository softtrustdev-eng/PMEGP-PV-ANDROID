# C04 — Image capture, compression & upload

| | |
|---|---|
| **MAUI source** | `Util/ImageCompressor.cs`; upload paths in `UnitVerificationPage.xaml.cs:4790-5030`, `BankPhysicalVerificationPage.xaml.cs:2740-3020`, `InspectionDetailsPage.xaml.cs:1160-1290` |
| **Java target** | `core/util/ImageCompressor`, `data/repository/DocumentRepository` |
| **Review state** | **R1 PASSED** — all rules traced; R2 pending (needs a >2 MB gallery image on device) |
| **Rules** | 16 (6 HIGH) |

---

## 1. The shape of it

Two ways a file enters the app, one way it leaves.

```
CAMERA                              GALLERY / FILE PICKER
  capture                             pick
  compress ALWAYS  (C04-R05)          pick-time gate  (C04-R03)
  write .jpg to cache                 keep original path
        │                                   │
        └──────────────┬────────────────────┘
                       ▼
             UPLOAD-TIME GATE  (C04-R06)
             compress only if  > 2 MB  AND  ext ∈ {jpg, jpeg, png}
                       ▼
             HARD LIMIT  (C04-R08)
             still > 2 MB → reject, abort
                       ▼
             base64 → POST
```

The two compression points are deliberately different. Getting this wrong in either
direction produces a real, already-reported defect — see R05 and R06.

---

## 2. Compression

### C04-R01 — Algorithm
**Rule.** Re-encode as **JPEG, quality 80**, scaled down so the long edge is at most
**1920 px**. Aspect ratio preserved. An image already within 1920×1920 is re-encoded at
quality 80 **without** resizing.
**Source.** `Util/ImageCompressor.cs:9-39`
**Java.** `BitmapFactory` + `Bitmap.compress(JPEG, 80, out)`. Use `inSampleSize` on decode
for large files — decoding a 48 MP photo at full size before scaling will OOM a field phone,
which SkiaSharp avoids internally and `BitmapFactory` will not.
**Risk.** HIGH

### C04-R02 — Compression never fails loudly
**Rule.** If decoding or encoding throws, **return the original bytes unchanged**. No
exception escapes, no empty result.
**Source.** `Util/ImageCompressor.cs:20` (undecodable → original), `:40-44` (catch → original)
**Why.** The size gate downstream (R08) is the real guard. A failed compression degrades to
"file unchanged", which the gate then handles. An exception here would abort an upload the
officer could otherwise complete.
**Risk.** MED

> **Output is always JPEG**, whatever went in. A PNG that gets compressed comes out as JPEG
> bytes. This matters — see R12.

---

## 3. Gallery / file-picker path

### C04-R03 — Pick-time gate rejects only what cannot be compressed ⚠️
**Rule.** At pick time, read the file size. Reject with a dialog **only when** the file is
over 2 MB **and** its extension is not `.jpg` / `.jpeg` / `.png`.

An oversized JPEG or PNG is **accepted** at pick time and compressed later.

**Source.** `UnitVerificationPage.xaml.cs:4810-4831`
**Why — this is a fixed defect.** The gate originally rejected *any* oversized pick. An
ordinary 3–6 MB phone photo was refused outright with no way for the officer to shrink it,
while the identical photo taken through the in-app camera went through fine. Rejecting before
compression ever runs *was* the defect.
**Test.** Pick a 5 MB JPEG → accepted. Pick a 5 MB PDF → rejected with "File Too Large".
**Risk.** HIGH

### C04-R04 — Unreadable size falls through
**Rule.** If the file's size cannot be read, skip the gate and let the upload proceed. The
upload-time limit (R08) catches it.
**Source.** `UnitVerificationPage.xaml.cs:4817-4818`
**Risk.** LOW

---

## 4. Camera path

### C04-R05 — A captured photo is always compressed, at capture
**Rule.** Compress immediately after capture, unconditionally — no size check. Write the
result to a cache file named `uv_photo_<guid>.jpg` and hand that path to the normal upload
flow, exactly as a picked file.
**Source.** `UnitVerificationPage.xaml.cs:4865-4880`
**Why unconditional.** A camera photo is always large. Checking first costs a stat call and
saves nothing.
**Java.** `context.getCacheDir()`. Note the file is **not** deleted after upload in the MAUI
app — cache growth is bounded only by Android's own cache eviction. Worth fixing in the port;
record it rather than silently changing behaviour.
**Risk.** MED

---

## 5. Upload-time gate

### C04-R06 — Compress only when oversized AND compressible ⚠️⚠️
**Rule.** Immediately before upload:

```java
// C04-R06
String ext = extensionOf(path).toLowerCase(Locale.ROOT);
boolean compressible = ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png");
if (bytes.length > TWO_MB && compressible) {
    bytes = ImageCompressor.compress(bytes);
}
```

Both conditions are load-bearing, for different reasons:

| Condition | Dropping it causes |
|---|---|
| `> 2 MB` | A camera photo — already compressed at capture — is re-encoded a **second** time. Two JPEG quantisation passes, visible generation loss, for no size benefit. This was a reported defect. |
| extension check | A PDF is handed to a JPEG encoder. It fails, R02 returns the original, and the only effect is wasted work. Harmless but pointless. |

**Source.** `UnitVerificationPage.xaml.cs:5003-5007` (and the equivalent in BankPV `:2966-2968`,
Inspection `:1274-1277`)
**Risk.** HIGH

### C04-R07 — The limit is 2 MB binary
**Rule.** `2 * 1024 * 1024` = 2,097,152 bytes. Not 2,000,000.
**Source.** `UnitVerificationPage.xaml.cs:4990`
**Risk.** MED

### C04-R08 — Hard limit after compression
**Rule.** After the optional compression, if the file is **still** over 2 MB: show
`File Too Large` / *"The selected file exceeds the 2 MB limit. Please choose a smaller file."*
/ `OK`, and **abort the upload**. Nothing is sent.
**Source.** `UnitVerificationPage.xaml.cs:5009-5013`
**Why the limit exists at all.** The payload is base64, which inflates by ~33%, and the byte
array and the encoded string are held in memory at the same time. An oversized file OOMs a
field phone instead of failing cleanly.
**Risk.** HIGH

### C04-R09 — Upload encoding
**Rule.** The file is sent as a base64 string in a JSON body, together with its extension.
Not multipart.
**Source.** `UnitVerificationPage.xaml.cs:5015-5017`
**Java.** `Base64.encodeToString(bytes, Base64.NO_WRAP)` — **`NO_WRAP` is required**. Android's
default inserts newlines every 76 characters, which the server will not accept.
**Risk.** HIGH

---

## 6. Accepted types

### C04-R10 — Picker filter
**Rule.** Android picker accepts `image/*` and `application/pdf`.
**Source.** `UnitVerificationPage.xaml.cs:4800-4805`
**Risk.** LOW

### C04-R11 — Compressible set is narrower than the accepted set
**Rule.** Only `.jpg`, `.jpeg`, `.png` are compressed. Everything else — PDF, `.webp`,
`.heic` — passes through untouched and is subject to the hard 2 MB limit.
**Source.** `UnitVerificationPage.xaml.cs:5004`
**Known gap.** The picker accepts `image/*`, so a `.webp` or `.heic` over 2 MB is **rejected
despite being perfectly compressible**. Modern Android cameras produce `.heic` by default on
some devices. This is an open defect, not a design decision.
**Recommendation for the port.** Widen the compressible set to `.webp` and `.heic` — the
Android decoder handles both natively, unlike SkiaSharp. Flag it to the tester rather than
shipping it silently.
**Risk.** MED

---

## 7. Extension handling

### C04-R12 — Extension is taken from the filename, not the content ⚠️
**Rule.** In the Unit Verification upload path, the extension sent to the server is
`Path.GetExtension(filePath)` — the **original filename's** extension.
**Source.** `UnitVerificationPage.xaml.cs:5018`
**The bug.** C04-R01 always outputs JPEG. So a 5 MB `.png` is compressed into JPEG bytes and
uploaded **labelled `.png`**. The stored file has a PNG extension and JPEG content.
**Status.** Open defect, reported, not fixed. Carry the behaviour into the Java app only if
the server depends on it; otherwise fix it and tell the tester. **Do not fix silently** —
existing records already have mislabelled files.
**Risk.** HIGH

### C04-R13 — Bank Physical Verification detects the extension from magic bytes
**Rule.** BankPV uses content sniffing instead of the filename:

| Bytes | Extension |
|---|---|
| `25 50 44 46` (`%PDF`) | `.pdf` |
| `89 50 4E 47` (`\x89PNG`) | `.png` |
| `FF D8` | `.jpg` |
| anything else, or fewer than 4 bytes | `.jpg` |

**Source.** `BankPhysicalVerificationPage.xaml.cs:3517-3534`
**Note.** This is the correct approach and it is **not** what Unit Verification does. Two
screens, two behaviours, same underlying job. In the Java port put the magic-byte detector in
`core/util` and use it everywhere — but record that this changes Unit Verification's
behaviour, so it needs a tester sign-off, not a quiet unification.
**Risk.** MED

---

## 8. User-facing hint

### C04-R14 — The 2 MB limit is stated on the form
**Rule.** A label reading exactly `File size should be less than 2 MB.` sits directly below
the Choose File row. 12 pt, `#666666`, margin `(4, 0, 4, 0)`.
**Source.** `UnitVerificationPage.xaml.cs:5149-5155`, BankPV `:3139-3145`, Inspection `:1363-1369`
**Why.** The ceiling was previously discoverable only by hitting it.
**Present on.** All three upload screens.
**Risk.** LOW

### C04-R15 — Error dialog wording
**Rule.** Title `File Too Large`, body *"The selected file exceeds the 2 MB limit. Please
choose a smaller file."*, button `OK`. Identical on all three screens and at both gates.
**Risk.** LOW

---

## 9. Permissions

### C04-R16 — Required permissions
**Rule.** `CAMERA`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`.
**Source.** `Platforms/Android/AndroidManifest.xml`
**Java note.** The manifest declares the legacy storage permissions, but the app targets
API 36 where they are ignored for media. On API 33+ the correct permission is
`READ_MEDIA_IMAGES`, and the **Photo Picker** needs no permission at all. In the Java port,
prefer `ActivityResultContracts.PickVisualMedia` — it removes the permission entirely and is
Google's required path for new submissions. Record this as an intentional improvement so the
tester knows the permission prompt will disappear.
**Risk.** MED

---

## 10. Known-defect trace

Every one of these was reported and fixed. The Java port must not reintroduce them.

| Defect | Rule that prevents it |
|---|---|
| Gallery images rejected instead of compressed | C04-R03 |
| Camera photos compressed twice, quality loss | C04-R06 |
| 2 MB ceiling undiscoverable until hit | C04-R14 |
| Oversized file still uploaded, OOM | C04-R08 |

And two still open:

| Open issue | Rule |
|---|---|
| PNG compressed to JPEG but labelled `.png` | C04-R12 |
| `.webp` / `.heic` over 2 MB rejected despite being compressible | C04-R11 |

---

## 11. R2 checklist

Needs a device and prepared files. Not yet executed.

| # | Step | Expected |
|---|---|---|
| 1 | Pick a 5 MB JPEG from gallery | accepted, uploads, stored ≈300 KB |
| 2 | Pick a 5 MB PDF | rejected at pick time |
| 3 | Capture a photo | compressed once; compare against a second upload of the same file for double-encoding artefacts |
| 4 | Pick a 3 MB PNG | uploads; **check the stored file's extension vs its magic bytes** (R12) |
| 5 | Pick a 3 MB `.webp` | currently rejected — confirm, then decide (R11) |
| 6 | Pick a 1 MB JPEG | **not** recompressed — byte size unchanged end to end |

---

## 12. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 16 rules traced across all three upload screens |
| R2 | — | pending — needs prepared test files on device |
| R3 | — | pending |
