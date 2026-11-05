# C10 — Cryptography & key material

| | |
|---|---|
| **MAUI source** | `Util/CryptoHelper.cs`, `Util/RsaHelper.cs` |
| **Java target** | `core/security/TripleDesCrypto`, `core/security/RsaEncryptor` |
| **Reference impl** | [`reference-java/CryptoHelper.java`](../reference-java/CryptoHelper.java), [`reference-java/RsaHelper.java`](../reference-java/RsaHelper.java) |
| **Review state** | **R2 PASSED** — vectors verified in Java against C#-generated reference output |
| **Rules** | 11 (5 HIGH) |

> **Gate 6 of the readiness gate is closed by this document.** Both algorithms reproduce
> byte-for-byte in Java 17. The port is not blocked on cryptography.

---

## 1. Two independent mechanisms

| | TripleDES (C10-R01..R07) | RSA (C10-R08..R11) |
|---|---|---|
| Direction | **decrypt only** in the app | **encrypt only** in the app |
| Used for | field values returned by the API | the login credential blob |
| Key | derived from a hardcoded passphrase | server's public key, embedded |
| Server counterpart | `Tssipl.SecurityHelper.Decrypt(s, useHashing: true)` | `PMEGP_Portal.Utility.RSAHelper.DecryptWithXmlPrivateKey` |

---

## 2. TripleDES — API field decryption

### C10-R01 — Algorithm
**Rule.** TripleDES, **ECB** mode, **PKCS7** padding. Input is Base64, output is UTF-8 text.
**Source.** `Util/CryptoHelper.cs:30-40`
**Java.** `Cipher.getInstance("DESede/ECB/PKCS5Padding")` — PKCS5 and PKCS7 are identical for
8-byte blocks, which is what DES uses.
**Risk.** HIGH

### C10-R02 — Key derivation
**Rule.** Key = `MD5(UTF8("~!@#$%^&*()_+|"))`.
**Source.** `Util/CryptoHelper.cs:12` (passphrase), `:30` (hash)
**Verified value.** `EB65122B092B0051F54D8F3E77F55507` (16 bytes)
**Risk.** HIGH

### C10-R03 — The 16 → 24 byte key expansion ⚠️
**Rule.** MD5 yields **16 bytes**. .NET's `TripleDES.Key` accepts that directly as a 2-key
EDE key (K3 = K1). **Java's `DESede` requires 24 bytes and throws on 16.** The Java
implementation must append the first 8 bytes of the MD5 to reach 24.

```java
byte[] md5 = MessageDigest.getInstance("MD5").digest(PASSPHRASE.getBytes(UTF_8)); // 16
byte[] k24 = new byte[24];
System.arraycopy(md5, 0, k24,  0, 16);
System.arraycopy(md5, 0, k24, 16,  8);   // K3 = K1
```

**Why this rule exists.** This is the single most likely thing to be got wrong in the whole
port. The failure is not subtle at the API — `DESede` throws `InvalidKeyException` — but a
developer who "fixes" it by truncating, zero-padding, or switching to plain `DES` will
produce a cipher that runs and silently returns garbage for every encrypted field in the app.
**Verified.** Both halves of the expansion confirmed against C#-generated ciphertext.
**Risk.** HIGH

### C10-R04 — Empty and blank input passes through
**Rule.** `null`, empty, or whitespace-only input is returned unchanged and counts as success.
**Source.** `Util/CryptoHelper.cs:25-26`
**Risk.** MED

### C10-R05 — Undecryptable input returns the input, not an error
**Rule.** Any failure — bad Base64, wrong padding, non-UTF-8 result — returns the **original
string unchanged**. No exception escapes, no empty string, no placeholder.
**Source.** `Util/CryptoHelper.cs:47-51`
**Why.** Not every value from the API is encrypted. A plaintext field passed through the
decryptor must survive intact.
**Java.** Wrap in try/catch and return the input in the catch. Do **not** let
`BadPaddingException` or `IllegalArgumentException` propagate.
**Risk.** HIGH

### C10-R06 — Callers that need to distinguish failure use the two-result form
**Rule.** A second entry point returns both the result and a success flag, for callers that
must tell "genuinely plain/undecryptable" from "a real decrypted value" — for example when
deciding whether a field should be editable.
**Source.** `Util/CryptoHelper.cs:20-51`
**Java.** Return a small result type or an `Optional<String>`; do not reduce this to the
single-value form, because at least one caller branches on it.
**Risk.** MED

### C10-R07 — ECB leaks repeated blocks (known, documented, not fixed here)
**Rule.** ECB encrypts identical 8-byte plaintext blocks to identical ciphertext blocks.
**Evidence.** `"12345678"` → `+qFUbZpnsGeo8g2t0u7aJg==`;
`"123456789012345678901234"` → `+qFUbZpnsGefx55vbyDHJWwcsHkJZah7qPINrdLu2iY=`.
The first ciphertext block is identical in both.
**Why it is listed.** The Java app must keep ECB to stay compatible with the server. This is
a **server-side finding**, not a port decision — record it, carry the behaviour, and raise it
separately. Changing it unilaterally breaks every existing record.
**Risk.** LOW *(for the port)* / **HIGH** *(as a security finding to route to the backend team)*

### Verified vectors

Generated from the live C# helper, confirmed to decrypt **and** re-encrypt identically in Java.
Use these as the Java unit test's fixtures.

| Base64 ciphertext | Plaintext |
|---|---|
| `4EZ80JluAut9N2VsldB4Qg==` | `9876543210` |
| `CqUv0YnCbkViYhKkQV5gGcAwQbz2jTx1` | `RAJESH KUMAR SHARMA` |
| `iNR0R7YMbyJnkogCwJ7uAJV4CaT9cr/awBmjER3l9XQJjuIxGcUe2VEPT38t6gJ7xu5ueQqpk+Dq8x8X7mTtoQ==` | `Plot 12/4, Shah & Sons, D'Souza Road, Vizianagaram, 535161` |
| `hjZa+q9Mikk=` | `A` |
| `+qFUbZpnsGeo8g2t0u7aJg==` | `12345678` |
| `+qFUbZpnsGefx55vbyDHJWwcsHkJZah7qPINrdLu2iY=` | `123456789012345678901234` |

Result: **6/6 decrypt, 6/6 re-encrypt, 1/1 garbage-passthrough.**

The third vector matters most — it contains `&`, `/`, `'` and a comma, the punctuation that
real Indian addresses carry and that [C03](C03-input-sanitisation.md) deliberately preserves.

---

## 3. RSA — login credential encryption

### C10-R08 — Algorithm
**Rule.** RSA-2048, **PKCS#1 v1.5** padding, Base64-encoded output. Plaintext is the UTF-8
login JSON.
**Source.** `Util/RsaHelper.cs:36`
**Java.** `Cipher.getInstance("RSA/ECB/PKCS1Padding")`. Do **not** use OAEP — the server's
`DecryptWithXmlPrivateKey` expects v1.5 and will fail.
**Risk.** HIGH

### C10-R09 — Public key is embedded, in .NET XML format
**Rule.** The server's RSA-2048 public key ships in the app as .NET `<RSAKeyValue>` XML.
Modulus and exponent are Base64 big-endian magnitudes.
**Source.** `Util/RsaHelper.cs:21`
**Java.** There is no `fromXmlString` in the JDK. Parse out the two Base64 values and build
the key directly — and construct each `BigInteger` with the **signum-1 form**, or a modulus
whose top bit is set is read as negative:

```java
BigInteger n = new BigInteger(1, Base64.getDecoder().decode(MODULUS)); // the 1 is required
BigInteger e = new BigInteger(1, Base64.getDecoder().decode(EXPONENT));
KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
```

**Verified.** Parses to a 2048-bit modulus, exponent 65537.
**Note.** A public key is not a secret; embedding it is fine. It is **not** a pin — it does
not authenticate the server.
**Risk.** HIGH

### C10-R10 — Output must be exactly 344 Base64 characters
**Rule.** The server expects a 256-byte block, i.e. 344 Base64 characters. Plaintext must be
≤ 245 bytes.
**Source.** `Util/RsaHelper.cs:24-32`
**Verified.** A 48-byte login payload produced exactly 344 characters.
**Test.** Assert the length in the Java unit test — it catches a wrong key size or wrong
padding immediately, without needing the server.
**Risk.** MED

### C10-R11 — Empty plaintext returns empty string
**Rule.** Empty or null plaintext returns `""`, not an encrypted empty block.
**Source.** `Util/RsaHelper.cs:26-27`
**Risk.** LOW

> **Note on testing RSA.** PKCS#1 v1.5 padding is randomised, so two encryptions of the same
> plaintext differ. You cannot assert a fixed ciphertext. Assert instead: output length is
> 344, two encryptions differ, and — once the server team supplies a test private key — that
> it decrypts back. Verified: consecutive encryptions do differ.

---

## 4. What is **not** encrypted

Worth stating so nobody adds it during the port:

- The session token is sent as-is; it is not RSA-wrapped.
- Request bodies other than the login blob are plain JSON.
- There is no certificate pinning. The UAT host is an IP with a bypassing TLS client —
  see [C02](C02-api-contracts.md).

---

## 5. Signing key — not cryptography, but key material

### Play Store identity

| | |
|---|---|
| Keystore | `PMEGP_Physical_V.keystore` (repo root, **tracked in git**) |
| Alias | `pmegpkey` |
| Store password | `123456` (in `PMEGP-Physical V.csproj`, in git) |
| Signature algorithm | SHA384withRSA |
| Valid until | 12 July 2053 |
| Certificate SHA-256 | `9C:02:3C:7F:1B:2B:2A:41:D4:CC:5C:99:1E:E6:2F:BC:5C:88:9E:C0:13:47:15:D8:9C:7E:88:E0:98:64:34:00` |

**The Java app must be signed with this exact key** or Play rejects the upload to
`com.pmegp.pv`. There is no recovery path short of a new listing.

Record the SHA-256 above. After the first Java release build, run
`keytool -printcert -jarfile app-release.aab` and confirm the fingerprint matches. That one
check prevents the only unrecoverable mistake in the whole port.

| # | Action | State |
|---|---|---|
| 1 | Keystore located | ✅ repo root |
| 2 | Alias and password known | ✅ `pmegpkey` / `123456` |
| 3 | Fingerprint recorded here | ✅ |
| 4 | **Backed up outside the repo** | ⬜ **open — user action** |
| 5 | Confirm whether Play App Signing is enrolled | ⬜ open |

Items 4 and 5 are the only parts of gate 7 still open, and neither is a code task.
Item 5 matters: if Play App Signing is enrolled, Google holds the real signing key and this
keystore is only the *upload* key — which changes what "same key" means for the Java build.
Check the Play Console → Setup → App integrity page before the first upload.

---

## 6. Review log

| Pass | Date | Result |
|---|---|---|
| R1 | 2026-11-05 | ✅ all 11 rules traced to source |
| R2 | 2026-11-05 | ✅ 6/6 TripleDES vectors + 3/3 RSA assertions pass in Java 17 |
| R3 | — | pending |
