# Fix Summary — Bug 001: Oversized Title Accepted

## Overall Status

**FIXED — All tests passed.**

Single-token removal (`+ 1`) in `SnippetValidator.kt` corrects the off-by-one validation
error. The change was applied exactly as specified in the implementation plan.

---

## Changes Made

### Change 1

| Field | Value |
|-------|-------|
| **File** | `src/main/kotlin/homework4/validation/SnippetValidator.kt` |
| **Line** | 23 |
| **Type** | Condition tightened (off-by-one fix) |

**Before:**
```kotlin
        if (request.title.isEmpty() || request.title.length > MAX_TITLE_LENGTH + 1) {
```

**After:**
```kotlin
        if (request.title.isEmpty() || request.title.length > MAX_TITLE_LENGTH) {
```

**Rationale:** With `> MAX_TITLE_LENGTH + 1` (i.e. `> 51`), a 51-character title evaluates
`51 > 51 == false` and bypasses validation. Removing `+ 1` makes the guard `> 50`, so
`51 > 50 == true` correctly triggers a `400 Bad Request`.

**Test result after this change:**

```
> Task :homework-4:test

BUILD SUCCESSFUL in 1s
6 actionable tasks: 6 executed
```

All 2 existing tests passed:
- `create and retrieve a snippet` — happy path, `201 Created` ✓
- `reject creation without a valid token` — auth guard, `401 Unauthorized` ✓

---

## Test Coverage Gap (noted, not fixed)

The existing test suite (`SnippetSmokeTest.kt`) contains **no test for a 51-character title**.
The plan noted this possibility and confirmed the source fix is independent of test authorship.

The gap means the bug could regress undetected. Adding the following test is recommended as
a follow-up (outside this plan's scope):

```kotlin
@Test
fun `reject title of exactly 51 characters`() = testApplication {
    application { module(InMemorySnippetService()) }
    val title = "A".repeat(51)
    val response = client.post("/snippets") {
        header("X-Api-Token", "s3cr3t-admin-token")
        contentType(ContentType.Application.Json)
        setBody("""{"title":"$title","content":"some content"}""")
    }
    assertEquals(HttpStatusCode.BadRequest, response.status)
    assertTrue(response.bodyAsText().contains("title"))
}
```

---

## Manual Verification

To confirm the fix behaves correctly end-to-end:

1. Start the server:
   ```
   ./gradlew :homework-4:run
   ```

2. **51-character title — must return 400 (the bug):**
   ```bash
   curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/snippets \
     -H "Content-Type: application/json" \
     -H "X-Api-Token: s3cr3t-admin-token" \
     -d '{"title":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA","content":"x"}'
   # title is 51 A's — expected: 400
   ```
   *(Note: the shell string above contains exactly 51 `A` characters.)*

3. **50-character title — must return 201 (boundary, still valid):**
   ```bash
   curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/snippets \
     -H "Content-Type: application/json" \
     -H "X-Api-Token: s3cr3t-admin-token" \
     -d '{"title":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA","content":"x"}'
   # title is 50 A's — expected: 201
   ```

4. **52-character title — must return 400 (was already rejected before fix):**
   ```bash
   curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/snippets \
     -H "Content-Type: application/json" \
     -H "X-Api-Token: s3cr3t-admin-token" \
     -d '{"title":"AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA","content":"x"}'
   # title is 52 A's — expected: 400
   ```

---

## References

- `src/main/kotlin/homework4/validation/SnippetValidator.kt:16` — `MAX_TITLE_LENGTH = 50`
- `src/main/kotlin/homework4/validation/SnippetValidator.kt:23` — fix applied here
- `src/main/kotlin/homework4/routing/SnippetRoutes.kt:31` — validator invocation (unchanged)
- `src/main/resources/openapi.yaml:84` — `maxLength: 50` (authoritative contract)
- `context/bugs/001-functional-oversized-title-accepted/implementation-plan.md` — approved plan followed
- `context/bugs/001-functional-oversized-title-accepted/research/verified-research.md` — root-cause analysis
