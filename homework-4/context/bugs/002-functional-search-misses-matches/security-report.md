# Security Report — 002-functional-search-misses-matches

## Scope

Reviewed the following files in connection with the fix:

| File | Reason |
|------|--------|
| `src/main/kotlin/homework4/service/SnippetService.kt` | Changed file (the fix) |
| `src/main/kotlin/homework4/routing/SnippetRoutes.kt` | Route handler that invokes `service.search()` |
| `src/main/kotlin/homework4/utils/TokenAuth.kt` | Auth path adjacent to the changed route |
| `src/main/kotlin/homework4/validation/SnippetValidator.kt` | Validation path for snippet creation |
| `src/main/kotlin/homework4/models/ApiModels.kt` | DTOs used in request/response |

---

## Findings

### No Issues Introduced by This Fix

The change (adding `ignoreCase = true` to `String.contains`) is a purely algorithmic fix
with no security implications:

- **No injection risk:** The `query` parameter is used only as a substring-match argument
  to Kotlin's `String.contains`. It is never interpolated into a command, SQL query, or
  template. There is no path traversal, command injection, or expression injection vector.
- **No XSS:** The search endpoint returns serialized JSON via Ktor's `respond`. Content
  is not rendered as HTML. The fix does not alter output encoding.
- **No CSRF:** The search endpoint is a read-only GET. The fix does not change
  authentication or state-mutation behavior.
- **No secrets exposed:** The change does not touch credential handling or logging.
- **No denial-of-service amplification:** `String.contains` with `ignoreCase = true` uses
  standard locale-independent comparison (`Char.equals` with `ignoreCase`). It does not
  introduce regex or backtracking-based matching. The search iterates an in-memory map,
  which is bounded by the number of stored snippets — no new amplification vector.

---

### Pre-existing Issues (Not Introduced by This Fix)

The following pre-existing issues were noted in code adjacent to the change. They are
**not caused by this fix** but are reported for completeness since they sit on the same
request path.

#### 1. Missing query-length limit on search endpoint

| Field | Value |
|-------|-------|
| **Severity** | LOW |
| **File:Line** | `src/main/kotlin/homework4/routing/SnippetRoutes.kt:66` |

**Description:** The `q` query parameter is accepted without any length constraint.
An attacker could supply an extremely long query string. While `String.contains` is O(n*m)
in the worst case, the practical impact is limited because the store is in-memory and
bounded. In a production system with a persistent store this could become a concern.

**Remediation:** Consider capping `q` length (e.g., 200 characters) and returning 400
if exceeded.

---

#### 2. Unauthenticated search endpoint (information disclosure)

| Field | Value |
|-------|-------|
| **Severity** | INFO |
| **File:Line** | `src/main/kotlin/homework4/routing/SnippetRoutes.kt:65-68` |

**Description:** The search endpoint requires no authentication. Any caller can enumerate
snippet titles. Whether this is intended depends on the application's access model. If
snippets are meant to be private, this is an information-disclosure issue.

**Remediation:** Clarify the intended access model. If snippets should be private, require
the same `X-Api-Token` header as the create endpoint.

---

#### 3. Hardcoded secret and non-constant-time comparison in TokenAuth

| Field | Value |
|-------|-------|
| **Severity** | HIGH (pre-existing, seeded) |
| **File:Line** | `src/main/kotlin/homework4/utils/TokenAuth.kt:15-20` |

**Description:** The admin token is hardcoded in source (`"s3cr3t-admin-token"`), and
comparison uses `==` which is non-constant-time. This is documented as a seeded
vulnerability in the source comments. It is unrelated to the current fix but sits on
the same request pipeline (POST /snippets).

**Remediation:**
- Move the secret to an environment variable or external configuration.
- Use `MessageDigest.isEqual` or a constant-time comparison function for token validation.

---

## Overall Assessment

**The fix itself introduces no security vulnerabilities.** It is a minimal, safe change
that adds a boolean flag to an existing stdlib function call. No new attack surface,
no altered trust boundaries, no credential handling changes.

Pre-existing issues (hardcoded token, non-constant-time comparison) are noted but are
outside the scope of this bug fix.
