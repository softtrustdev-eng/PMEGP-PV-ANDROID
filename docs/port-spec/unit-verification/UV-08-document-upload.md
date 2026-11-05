# UV08 — Stage 8: Document Upload

| | |
|---|---|
| **MAUI source** | builder `:4480-5723`, upload `:4790-5030`, save `:9605-9619` |
| **Java target** | `ui/unitverification/stages/Stage08DocumentUploadFragment` + `data/repository/DocumentRepository` |
| **Depends on** | [C04 image capture & compression](../cross-cutting/C04-image-capture-and-compression.md) — **read that first** |
| **Review state** | **R1 PASSED** |
| **Rules** | 12 (5 HIGH) |

---

## 1. Purpose

Captures or picks verification photographs and uploads them. **Mandatory** — at least one
image is required to proceed.

---

## 2. Upload mechanics

### UV08-R01 — All upload behaviour is specified in C04
**Rule.** Pick-time gate, camera auto-compression, the `> 2 MB` **and** compressible-extension
condition, the hard 2 MB limit, base64 encoding, the size hint label, error wording.
**See.** [C04](../cross-cutting/C04-image-capture-and-compression.md) — 16 rules.
**Risk.** HIGH

### UV08-R02 — Endpoint
**Rule.** `POST {API_BASE}/UploadAllDocumentsUV` with the base64 file, its extension, the
applicant id, and the state name.
**Source.** `:5015-5022`
**Risk.** HIGH

### UV08-R03 — State name has a hardcoded fallback ⚠️
**Rule.** The state name is read from the record, defaulting to `"Maharashtra"` when absent.
**Source.** `:5021`
**Note.** A silent wrong-state fallback. Carry it for compatibility, but flag it — a record
missing its state is filed under Maharashtra.
**Risk.** MED

### UV08-R04 — Extension comes from the filename, not the content
**Rule.** The extension sent is the original filename's.
**See.** [C04-R12](../cross-cutting/C04-image-capture-and-compression.md) — compression always
outputs JPEG, so a PNG is uploaded labelled `.png` with JPEG bytes.
**Status.** Open defect ([F-002](../README.md)).
**Note.** Bank Physical Verification sniffs magic bytes instead
([C04-R13](../cross-cutting/C04-image-capture-and-compression.md)). Two screens, two
behaviours. Unifying changes UV behaviour — needs a tester sign-off.
**Risk.** HIGH

---

## 3. Mandatory

### UV08-R05 — At least one image blocks forward navigation ⚠️
**Rule.** Moving past stage 8 while editable with no uploaded document is blocked with the
modal `Document Required` / *"Please upload at least one image before proceeding."*. The guard
covers the step indicator as well as the button.
**Source.** `:787-794`
**Risk.** HIGH

### UV08-R06 — Stage 10 re-checks the same source
**Rule.** Final Submit's prerequisite check tests the same uploaded-documents source, so the
two can never disagree.
**Source.** `:2075`
**Risk.** HIGH

### UV08-R07 — The saved list degrades to the record's own collection
**Rule.** If the dedicated saved-documents endpoint returns nothing, the applicant record's
document collection is used.
**See.** [UV00-R04](UV-00-host-and-shared-state.md)
**Source.** `:457`, `:477`
**Why.** A failure there costs Stage 8 its list, not the whole verification.
**Risk.** MED

---

## 4. Deletion

### UV08-R08 — Endpoint
**Rule.** `POST {API_BASE}/DeleteDocument`.
**Source.** endpoint inventory
**Open.** Request shape not captured. **Blocks R2** — see
[C02 §7](../cross-cutting/C02-api-contracts.md).
**Risk.** MED

### UV08-R09 — Deleting the last document re-blocks navigation
**Rule.** The mandatory check (R05) is live, so removing the only uploaded document blocks
forward navigation again.
**Risk.** MED

---

## 5. Other

### UV08-R10 — The validation popup must not destroy the form ⚠️
**Rule.** A validation error on this stage shows the popup **without** closing the upload form
behind it.
**See.** [C08-R01](../cross-cutting/C08-dialogs-and-modals.md) — a fixed defect. The reported
symptom was *"the popup gets closed automatically"*; it was the form being destroyed.
**Risk.** HIGH

### UV08-R11 — The stage save method is a no-op
**Rule.** Uploads happen per file as they are added. `SAVE & NEXT` does not post a stage
payload.
**Source.** `:9605-9619`
**Java.** Do not route this stage through the stage-save path — see
[UV01-R02](UV-01-beneficiary.md) for the same trap.
**Risk.** MED

### UV08-R12 — Read-only on concluded cases
**Rule.** No upload, no delete. The saved documents remain viewable.
**See.** [C09](../cross-cutting/C09-case-status-and-readonly.md)
**Risk.** MED

---

## 6. R2 checklist

| # | Step | Expected |
|---|---|---|
| 1 | Stage 8 with nothing uploaded, tap step 9 | blocked, `Document Required` (R05) |
| 2 | Upload one image, tap step 9 | allowed |
| 3 | Delete it, tap step 9 | blocked again (R09) |
| 4 | Trigger a validation error mid-upload | **form survives behind the popup** (R10) |
| 5 | Upload a 5 MB JPEG | accepted, compressed, stored ≈300 KB |
| 6 | Upload a 5 MB PDF | rejected at pick time |
| 7 | Upload a 3 MB PNG | check the stored file's extension vs its bytes (R04) |
| 8 | Record with no state name | which state is sent? (R03) |

---

## 7. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ 12 rules traced |
| R2 | — | pending — needs prepared test files; R08 blocked on the capture |
| R3 | — | pending |
