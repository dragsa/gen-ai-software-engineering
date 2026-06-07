# Security Report — 003-security-admin-token-authorization

## Scope

Files reviewed:
- `src/main/kotlin/homework4/utils/TokenAuth.kt` (full file, 16 lines)
- `build.gradle.kts` (full file, 30 lines)
- `src/main/kotlin/homework4/routing/SnippetRoutes.kt` (auth call-site, 70 lines)
- Test files referencing the token (`SnippetValidationTest.kt`, `SnippetSmokeTest.kt`)
- `.gitignore` for secret-leakage posture

Review categories: injection, hardcoded secrets, insecure comparisons, missing/weak input
validation, unsafe dependencies, XSS/CSRF.

---

## Findings

### Finding 1 — Token literal in `build.gradle.kts` committed to VCS

| Field       | Value |
|-------------|-------|
| **Severity**  | MEDIUM |
| **File:Line** | `build.gradle.kts:29` |
| **CWE**       | CWE-798 (Use of Hard-coded Credentials) |

**Description:**
The test environment block injects `ADMIN_TOKEN` with the literal value `"s3cr3t-admin-token"`.
While this is scoped to test execution only and does not ship in the production artifact, the
value is committed to version control. If this same token value is reused in staging or
production environments (which is common in practice), it is effectively leaked. An attacker
with read access to the repository obtains a valid token.

**Remediation:**
- Use a CI-managed secret (e.g., GitHub Actions secret, Vault) injected at test time, or
- Use a randomly generated per-run token via a Gradle property with a dev-only default
  (`project.findProperty("testAdminToken") ?: UUID.randomUUID().toString()`), and ensure
  `TokenAuth` reads the same value.
- At minimum, add a comment that this value MUST NOT match any real deployed token.

---

### Finding 2 — `adminToken` read once at class-load; no rotation support

| Field       | Value |
|-------------|-------|
| **Severity**  | LOW |
| **File:Line** | `src/main/kotlin/homework4/utils/TokenAuth.kt:6` |

**Description:**
`adminToken` is assigned at `object` initialization via `System.getenv("ADMIN_TOKEN")`. Because
Kotlin `object` is a singleton loaded once per JVM, changing the environment variable at runtime
(e.g., via a secret-rotation mechanism) has no effect until the process restarts. For a
homework-scale app this is negligible, but in production it prevents zero-downtime token
rotation.

**Remediation:**
For production use, read the environment variable (or better, a secrets-manager reference) on
each call, or implement a periodic refresh mechanism with a short TTL cache.

---

### Finding 3 — No rate-limiting or lockout on auth endpoint

| Field       | Value |
|-------------|-------|
| **Severity**  | LOW |
| **File:Line** | `src/main/kotlin/homework4/routing/SnippetRoutes.kt:22` |

**Description:**
The `POST /snippets` endpoint rejects invalid tokens with HTTP 401 but does not apply any
rate-limiting. An attacker can brute-force tokens at line-rate. While the constant-time
comparison prevents timing-based narrowing of the search space, the absence of rate-limiting
still allows volume-based guessing attacks if the token has low entropy.

**Remediation:**
Add per-IP rate-limiting (e.g., via Ktor rate-limit plugin or a middleware) with exponential
backoff on repeated 401 responses, or use a token format with sufficient entropy (≥128 bits /
22+ alphanumeric characters) so brute-force is computationally infeasible regardless.

---

### Finding 4 — Token echoed in test source

| Field       | Value |
|-------------|-------|
| **Severity**  | INFO |
| **File:Line** | `src/test/kotlin/homework4/SnippetValidationTest.kt:29` (and 6 additional locations) |

**Description:**
The literal `"s3cr3t-admin-token"` appears in multiple test files as an `X-Api-Token` header
value. This is functionally correct (it matches `build.gradle.kts` test environment), but if
the project evolves to use a distinct secret per environment, these scattered literals become
maintenance hazards and information-leak vectors if test artifacts are published.

**Remediation:**
Extract the test token into a shared constant (e.g., `TestConstants.ADMIN_TOKEN`) so it is
defined in exactly one place and can be rotated independently of test logic.

---

## Positive Observations

1. **Constant-time comparison (CWE-208 remediated):** `MessageDigest.isEqual` over SHA-256
   digests provides constant-time behavior that eliminates timing side-channels. SHA-256
   hashing first normalizes input length, further hardening the comparison.

2. **Fail-closed behavior:** When `ADMIN_TOKEN` is not set (`null`), `isAuthorized` returns
   `false`. This is the correct security posture — the system denies all writes if
   misconfigured, rather than failing open.

3. **No hardcoded secret in application source:** The production code path contains zero
   embedded credential literals.

4. **Token read from environment:** Using `System.getenv` is an appropriate mechanism for
   12-factor apps and integrates well with container orchestration and CI/CD secret injection.

---

## Overall Assessment

**PASS with observations.**

The fix correctly addresses both original vulnerabilities (CWE-798 hardcoded credential in
source, CWE-208 timing-attack-vulnerable comparison). The application source is clean of
secrets and uses cryptographically sound constant-time comparison.

The remaining findings are MEDIUM/LOW/INFO severity and relate to operational hardening
(test-environment token in VCS, lack of rotation, lack of rate-limiting) rather than exploitable
vulnerabilities in the fix itself. None represent regressions introduced by the change.

No CRITICAL or HIGH findings.
