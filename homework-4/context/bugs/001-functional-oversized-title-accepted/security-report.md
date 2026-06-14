# Security Report — Bug 001: Oversized Title Accepted

## Scope

**Changed file reviewed:**
- `src/main/kotlin/homework4/validation/SnippetValidator.kt` (line 23 — off-by-one fix)

**Related files reviewed (directly in the request/validation/auth path):**
- `src/main/kotlin/homework4/routing/SnippetRoutes.kt`
- `src/main/kotlin/homework4/utils/TokenAuth.kt`
- `src/main/kotlin/homework4/service/SnippetService.kt`
- `src/main/kotlin/homework4/models/ApiModels.kt`
- `src/main/kotlin/homework4/models/Snippet.kt`
- `src/main/kotlin/homework4/entrypoint/Module.kt`
- `src/main/kotlin/homework4/routing/DocumentationRoutes.kt`
- `src/main/resources/openapi.yaml`

---

## Assessment of the Fix

The change on `SnippetValidator.kt:23` replaces `> MAX_TITLE_LENGTH + 1` with
`> MAX_TITLE_LENGTH`. This is a pure validation tightening — it reduces the accepted boundary
from 51 to 50 characters, aligning runtime behavior with the OpenAPI contract
(`maxLength: 50`).

**The fix introduces no new security vulnerabilities.** It does not alter control flow, does
not touch authentication or response handling, and does not introduce new inputs or outputs.

---

## Findings

### Finding 1 — Hardcoded Admin Secret

| Field       | Value |
|-------------|-------|
| **Severity**  | **CRITICAL** |
| **Location**  | `src/main/kotlin/homework4/utils/TokenAuth.kt:15` |

**Description:**
The admin API token (`s3cr3t-admin-token`) is hardcoded as a string constant in source code.
Anyone with access to the repository, compiled bytecode, or a decompiler can extract the
secret and authenticate as an administrator to create arbitrary snippets.

**Remediation:**
Load the token from an environment variable or a secrets-management system (e.g.,
`System.getenv("ADMIN_TOKEN")`). Fail fast at startup if the variable is unset. Ensure the
secret is never committed to version control.

---

### Finding 2 — Non-Constant-Time Token Comparison

| Field       | Value |
|-------------|-------|
| **Severity**  | **MEDIUM** |
| **Location**  | `src/main/kotlin/homework4/utils/TokenAuth.kt:20` |

**Description:**
`providedToken == ADMIN_TOKEN` uses Kotlin's standard `String.equals`, which short-circuits
on the first mismatched character. An attacker can measure response-time differences across
many requests to deduce the correct token one character at a time (timing side-channel).

**Remediation:**
Use `java.security.MessageDigest.isEqual(a.toByteArray(), b.toByteArray())` or an equivalent
constant-time comparison. This eliminates timing variance regardless of where the strings
diverge.

---

### Finding 3 — No Upper Bound on Content Size

| Field       | Value |
|-------------|-------|
| **Severity**  | **LOW** |
| **Location**  | `src/main/kotlin/homework4/validation/SnippetValidator.kt:32` |

**Description:**
The validator checks that `content` is non-empty but enforces no maximum length. Because the
backing store is in-memory (`ConcurrentHashMap` in `InMemorySnippetService`), an authenticated
caller can submit arbitrarily large payloads, exhausting heap memory and causing an
`OutOfMemoryError` that crashes the server.

**Remediation:**
Add a `MAX_CONTENT_LENGTH` constant and validate `request.content.length <= MAX_CONTENT_LENGTH`
alongside the existing emptiness check. Additionally, configure Ktor's
`ContentNegotiation` or install a request-size-limiting plugin to reject oversized HTTP bodies
before deserialization.

---

### Finding 4 — No Rate Limiting on Write Endpoint

| Field       | Value |
|-------------|-------|
| **Severity**  | **LOW** |
| **Location**  | `src/main/kotlin/homework4/routing/SnippetRoutes.kt:21` |

**Description:**
`POST /snippets` has no rate limiting. Even with a valid token, an attacker (or a compromised
client) can issue rapid requests to fill the in-memory store, degrading performance or
exhausting memory.

**Remediation:**
Install Ktor's `RateLimit` plugin or an equivalent middleware to cap requests per token per
time window (e.g., 60 requests/minute).

---

### Finding 5 — Unbounded Search Results

| Field       | Value |
|-------------|-------|
| **Severity**  | **INFO** |
| **Location**  | `src/main/kotlin/homework4/service/SnippetService.kt:29` |

**Description:**
`GET /snippets?q=` returns all matching snippets with no pagination or result-count limit.
If the store grows large, a broad query (e.g., `q=`) could return an unbounded response,
causing high memory allocation and slow responses.

**Remediation:**
Add `limit` and `offset` query parameters and cap the default page size (e.g., 100). Update
the OpenAPI spec to document pagination.

---

### Finding 6 — Read Endpoints Require No Authentication

| Field       | Value |
|-------------|-------|
| **Severity**  | **INFO** |
| **Location**  | `src/main/kotlin/homework4/routing/SnippetRoutes.kt:42–69` |

**Description:**
`GET /snippets/{id}` and `GET /snippets?q=` are unauthenticated. Any caller can enumerate
and read all stored snippets, including their full content. This may be intentional (public
read, authenticated write) but is not documented in the OpenAPI spec's security scheme.

**Remediation:**
If public read access is intended, document it explicitly in the OpenAPI spec with a security
scheme (e.g., `security: []` on GET operations). If not intended, apply the same
`X-Api-Token` check used on the POST endpoint.

---

## Summary

| Severity | Count | Introduced by Fix? |
|----------|-------|---------------------|
| CRITICAL | 1     | No (pre-existing)   |
| MEDIUM   | 1     | No (pre-existing)   |
| LOW      | 2     | No (pre-existing)   |
| INFO     | 2     | No (pre-existing)   |

**The fix itself is clean and introduces no security regressions.** It correctly tightens
input validation to match the documented contract. All findings above are pre-existing
conditions in the surrounding authentication, validation, and routing code. The two
highest-severity findings (hardcoded secret, timing-vulnerable comparison) are in `TokenAuth.kt`,
which is on the direct request path of the changed endpoint but was not modified by this fix.
