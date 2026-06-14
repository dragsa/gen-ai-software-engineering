# Codebase Research — Bug 003: Admin Token Authorization

## Reproduced Symptom

Write operations (`POST /snippets`) are protected by a single shared admin token transmitted
via the `X-Api-Token` request header. The authorization code path exhibits two security
weaknesses:

1. **Hardcoded secret** — the expected admin token is embedded as a string literal directly
   in source code, which means it is committed to version control and visible to anyone with
   repository access.
2. **Non-constant-time comparison** — the incoming token is compared against the expected
   value using Kotlin's `==` operator (which delegates to `String.equals`), a comparison that
   short-circuits on the first differing character. This is susceptible to timing side-channel
   attacks that can leak the token value one character at a time.

Additionally, the plaintext token value is exposed in the project's `HOWTORUN.md`
documentation (line 17), the OpenAPI spec header description, and across multiple test files,
broadening the exposure surface.

## Traced Call Path

### 1. Entry point — route handler

The `POST /snippets` route is registered in `SnippetRoutes.kt`. The handler reads the
`X-Api-Token` header and delegates to `TokenAuth.isAuthorized()`:

**`src/main/kotlin/homework4/routing/SnippetRoutes.kt:17-28`**

```kotlin
private const val API_TOKEN_HEADER = "X-Api-Token"

fun Route.registerSnippetRoutes(service: SnippetService) {
    // Create a snippet (requires authorization).
    post("/snippets") {
        if (!TokenAuth.isAuthorized(call.request.headers[API_TOKEN_HEADER])) {
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(listOf(ValidationError("authorization", "invalid or missing API token"))),
            )
            return@post
        }
```

The `TokenAuth` import is at line 8:

```kotlin
import homework4.utils.TokenAuth
```

### 2. Authorization logic — `TokenAuth` object

The `TokenAuth` singleton contains both the stored secret and the comparison logic:

**`src/main/kotlin/homework4/utils/TokenAuth.kt:13-22`**

```kotlin
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

### 3. Token exposure in documentation and tests

The plaintext token value `s3cr3t-admin-token` also appears in:

- **`HOWTORUN.md:17`** — `- \`POST /snippets\` — create (header \`X-Api-Token: s3cr3t-admin-token\`)`
- **`src/test/kotlin/homework4/SnippetSmokeTest.kt:29`** — `header("X-Api-Token", "s3cr3t-admin-token")`
- **`src/test/kotlin/homework4/SnippetValidationTest.kt:29,45,59,72,86,100`** — six occurrences of the same literal in test requests.

## Root-Cause Claim

The authorization path has two distinct security weaknesses, both located in
`src/main/kotlin/homework4/utils/TokenAuth.kt`:

### Weakness 1: Hardcoded secret (CWE-798)

**Location:** `src/main/kotlin/homework4/utils/TokenAuth.kt:15`

```kotlin
    private const val ADMIN_TOKEN = "s3cr3t-admin-token"
```

The admin token is a compile-time constant embedded in the source file. Because it is a
`const val`, the literal string is inlined at every usage site by the Kotlin compiler.
This means:

- The secret is visible in the repository to all contributors and anyone with read access.
- It cannot be rotated without a code change, rebuild, and redeployment.
- It is not loaded from an environment variable, secrets manager, or external configuration.
- The same value is also hardcoded in documentation (`HOWTORUN.md:17`) and test files,
  further increasing exposure.

### Weakness 2: Non-constant-time string comparison (CWE-208)

**Location:** `src/main/kotlin/homework4/utils/TokenAuth.kt:20`

```kotlin
        return providedToken == ADMIN_TOKEN
```

Kotlin's `==` on `String` delegates to `java.lang.String.equals()`, which compares
character-by-character and returns `false` as soon as a mismatch is found. The time taken
by the comparison therefore varies depending on how many leading characters of the provided
token match the expected token. An attacker can exploit this timing difference to deduce the
secret token one character at a time by measuring response latencies (a timing side-channel
attack).

The secure alternative is a constant-time comparison function (e.g.,
`java.security.MessageDigest.isEqual()` or an HMAC-based comparison) that always examines
every byte regardless of match position.

## References

| # | File | Line(s) | Snippet |
|---|------|---------|---------|
| 1 | `src/main/kotlin/homework4/utils/TokenAuth.kt` | 15 | `private const val ADMIN_TOKEN = "s3cr3t-admin-token"` |
| 2 | `src/main/kotlin/homework4/utils/TokenAuth.kt` | 17 | `fun isAuthorized(providedToken: String?): Boolean {` |
| 3 | `src/main/kotlin/homework4/utils/TokenAuth.kt` | 18 | `if (providedToken == null) return false` |
| 4 | `src/main/kotlin/homework4/utils/TokenAuth.kt` | 20 | `return providedToken == ADMIN_TOKEN` |
| 5 | `src/main/kotlin/homework4/routing/SnippetRoutes.kt` | 17 | `private const val API_TOKEN_HEADER = "X-Api-Token"` |
| 6 | `src/main/kotlin/homework4/routing/SnippetRoutes.kt` | 22 | `if (!TokenAuth.isAuthorized(call.request.headers[API_TOKEN_HEADER])) {` |
| 7 | `src/main/kotlin/homework4/routing/SnippetRoutes.kt` | 8 | `import homework4.utils.TokenAuth` |
| 8 | `HOWTORUN.md` | 17 | `` - `POST /snippets` — create (header `X-Api-Token: s3cr3t-admin-token`) `` |
| 9 | `src/test/kotlin/homework4/SnippetSmokeTest.kt` | 29 | `header("X-Api-Token", "s3cr3t-admin-token")` |
| 10 | `src/test/kotlin/homework4/SnippetValidationTest.kt` | 29 | `header("X-Api-Token", "s3cr3t-admin-token")` |
