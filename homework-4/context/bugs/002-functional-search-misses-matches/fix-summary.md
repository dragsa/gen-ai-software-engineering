# Fix Summary — 002-functional-search-misses-matches

## Overall Status

**COMPLETE — all tests pass.**

---

## Changes Made

### Change 1

**File:** `src/main/kotlin/homework4/service/SnippetService.kt`  
**Location:** lines 29-33, `InMemorySnippetService.search`

**Before:**
```kotlin
override fun search(query: String): List<Snippet> {
    // BUG B (logic): search is meant to be case-insensitive, but `contains` defaults to
    // case-sensitive matching, so e.g. searching "hello" misses a title "Hello World".
    return store.values.filter { it.title.contains(query) }
}
```

**After:**
```kotlin
override fun search(query: String): List<Snippet> {
    return store.values.filter { it.title.contains(query, ignoreCase = true) }
}
```

**What changed:** Added `ignoreCase = true` to the `String.contains` call and removed the
bug-documentation comment that described the intentional defect.

**Test result after this change:**
```
BUILD SUCCESSFUL in 1s
6 actionable tasks: 6 executed
```
All tests passed immediately after the edit.

---

## Manual Verification

To confirm the fix by hand:

1. Start the application:
   ```
   ./gradlew :homework-4:run
   ```
2. Create a snippet with a mixed-case title:
   ```
   curl -X POST http://localhost:8080/snippets \
        -H 'Content-Type: application/json' \
        -d '{"title":"Hello World","content":"greetings"}'
   ```
3. Search with a lower-case query that previously returned no results:
   ```
   curl "http://localhost:8080/snippets/search?q=hello"
   ```
4. Expected response: a JSON array containing the snippet created in step 2.
5. Also verify the inverse — searching with `HELLO` or `HeLLo` should return the same result.

---

## References

- `src/main/kotlin/homework4/service/SnippetService.kt:29-33` — root-cause location (now fixed)
- `src/main/kotlin/homework4/routing/SnippetRoutes.kt:65-69` — route handler that calls `service.search(query)`
- Kotlin stdlib: `String.contains(other: String, ignoreCase: Boolean = false)` — `ignoreCase` overload used in the fix
- Implementation plan: `context/bugs/002-functional-search-misses-matches/implementation-plan.md`
