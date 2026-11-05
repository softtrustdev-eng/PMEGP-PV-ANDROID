# Captured API schemas

**Types only. No values.** The live responses contain real Aadhaar numbers, PAN numbers,
encrypted names and addresses, so nothing here carries data — every leaf is a type name.

Captured 2026-11-05 from the UAT host for `applID=2885245` /
`ApplCode=DIOR215640-10180466`, by direct `curl` (most endpoints are unauthenticated —
see [C02-R05](../cross-cutting/C02-api-contracts.md)) and from the app's own
`Debug.WriteLine` output in logcat under the `app_process64` tag.

| File | Endpoint | Size |
|---|---|---|
| `GetApplDataForPV.schema.json` | `GET /MobileApp/GetApplDataForPV?applID=` | 13.7 KB raw |
| `GetSavedPVData.schema.json` | `GET /MobileApp/GetSavedPVData?applID=` | 11.7 KB raw |
| `GetBankVerApplData.schema.json` | `GET /MobileApp/GetBankVerApplData?ApplCode=` | 3.3 KB raw |

To regenerate, re-run the curl commands and the redaction script; do **not** commit raw
responses.
