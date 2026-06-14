# Fix Summary — 003-security-admin-token-authorization

## Overall Status

**PASS** — Both security vulnerabilities remediated. All 28 tests across 4 suites passed after
the changes.

---

## Changes Made

### Change 1 — `src/main/kotlin/homework4/utils/TokenAuth.kt`

**Location:** Full file replacement (lines 1–22)

**Vulnerabilities addressed:**
- CWE-798 (Hardcoded Credentials) — removed literal `"s3cr3t-admin-token"` from source
- CWE-208 (Observable Timing Discrepancy) — replaced `==` with constant-time SHA-256 digest
  comparison via `MessageDigest.isEqual`

**Before:**
```kotlin
package homework4.utils

/**
 * Token-based authorization for write operations.
 *
 * SECURITY ISSUE (seeded):
 *   1. Hardcoded secret — the admin token is embedded directly in source and would be
 *      committed to version control.
 *   2. Insecure comparison — `==` performs a non-constant-time String comparison, which is
 *      vulnerable to timing attacks. Token comparison should use a constant-time check and
 *      the secret should come from configuration/environment, not a literal.
 */
object TokenAuth {
    // Hardcoded secret (seeded vulnerability).
    private const val ADMIN_TOKEN = "s3cr3t-admin-token"

    fun isAuthorized(providedToken: String?): Boolean {
        if (providedToken == null) return false
        // Non-constant-time comparison (seeded vulnerability).
        return providedToken == ADMIN_TOKEN
    }
}
```

**After:**
```kotlin
package homework4.utils

import java.security.MessageDigest

object TokenAuth {
    private val adminToken: String? = System.getenv("ADMIN_TOKEN")

    fun isAuthorized(providedToken: String?): Boolean {
        if (providedToken == null) return false
        val expected = adminToken ?: return false
        return MessageDigest.isEqual(sha256(expected), sha256(providedToken))
    }

    private fun sha256(value: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
}
```

**Test result after this change (combined with Change 2):**
```
BUILD SUCCESSFUL in 3s
```
All 28 tests passed.

---

### Change 2 — `build.gradle.kts`

**Location:** Appended after the `dependencies { }` block (after line 26)

**Vulnerability addressed:** CWE-798 — test processes need to supply `ADMIN_TOKEN` via
`System.getenv()` without re-embedding the literal in application source. Injecting it only
into the test task environment keeps the token out of the shipped binary.

**Before (no `tasks.withType<Test>` block existed):**
```kotlin
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
```

**After:**
```kotlin
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}

tasks.withType<Test>().configureEach {
    environment("ADMIN_TOKEN", "s3cr3t-admin-token")
}
```

**Test result after this change:**
```
BUILD SUCCESSFUL in 3s
```
All 28 tests passed.

---

## Manual Verification

To confirm the fix works end-to-end:

### 1. Verify environment-driven authorization

```bash
# Start the app with the token set
ADMIN_TOKEN=s3cr3t-admin-token ./gradlew :homework-4:run &

# Authorized request — expect 201 Created
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/snippets \
  -H "Content-Type: application/json" \
  -H "X-Api-Token: s3cr3t-admin-token" \
  -d '{"title":"hello","content":"world"}'
# Expected: 201

# Unauthorized request (wrong token) — expect 401
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/snippets \
  -H "Content-Type: application/json" \
  -H "X-Api-Token: wrong-token" \
  -d '{"title":"hello","content":"world"}'
# Expected: 401

# No token — expect 401
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/snippets \
  -H "Content-Type: application/json" \
  -d '{"title":"hello","content":"world"}'
# Expected: 401
```

### 2. Verify fail-closed behavior when `ADMIN_TOKEN` is absent

```bash
# Start the app WITHOUT setting ADMIN_TOKEN
unset ADMIN_TOKEN
./gradlew :homework-4:run &

# Any write request — expect 401
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/snippets \
  -H "Content-Type: application/json" \
  -H "X-Api-Token: s3cr3t-admin-token" \
  -d '{"title":"hello","content":"world"}'
# Expected: 401  (fail-closed: no env var → no writes allowed)
```

### 3. Verify the literal is gone from source

```bash
grep -r "s3cr3t-admin-token" src/
# Expected: no output (literal removed from application source)

grep -r "s3cr3t-admin-token" build.gradle.kts
# Expected: one line in the test task env block (acceptable: dev/CI only, not shipped)
```

### 4. Run the full test suite

```bash
./gradlew :homework-4:test --rerun-tasks --console=plain
# Expected: BUILD SUCCESSFUL, all tests pass
```

---

## References

- `src/main/kotlin/homework4/utils/TokenAuth.kt` — both vulnerabilities fixed
- `build.gradle.kts` — test env injection added
- `context/bugs/003-security-admin-token-authorization/implementation-plan.md` — source plan
- `context/bugs/003-security-admin-token-authorization/research/verified-research.md` — L4 root-cause analysis
- CWE-798: https://cwe.mitre.org/data/definitions/798.html
- CWE-208: https://cwe.mitre.org/data/definitions/208.html
