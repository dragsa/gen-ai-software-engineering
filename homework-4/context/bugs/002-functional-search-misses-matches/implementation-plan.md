# Implementation Plan — 002-functional-search-misses-matches

## Gate Check
- Verdict: **PASS** (L4 — Authoritative, overall score 0.99)
- Proceeding with plan.

---

## Goal

Make snippet title search case-insensitive so that a query such as `hello` returns a snippet
stored with title `Hello World`.

---

## Root Cause (confirmed)

`InMemorySnippetService.search` at
`src/main/kotlin/homework4/service/SnippetService.kt:32` calls
`it.title.contains(query)` without `ignoreCase = true`. Kotlin's `String.contains(String)`
defaults to case-sensitive matching, so any case mismatch produces an empty result.

---

## Target Files

### `src/main/kotlin/homework4/service/SnippetService.kt`

**Location:** line 32, inside `override fun search(query: String): List<Snippet>`

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

**Change summary:** add `ignoreCase = true` to the `contains` call and remove the bug
comment that documented the intentional defect.

---

## Test Command

```
./gradlew :homework-4:test --rerun-tasks --console=plain
```

---

## Edge Cases / Risks

| Scenario | Impact | Mitigation |
|---|---|---|
| Query is empty string (`""`) | `"".contains("", ignoreCase = true)` is `true` for every entry — same behaviour as the original case-sensitive path, which also returns all snippets for an empty query. No regression. | None needed. |
| Query contains regex-special or Unicode characters | `String.contains` does literal substring matching; no regex interpretation. Unicode case folding is handled by `java.lang.String.regionMatches` internally, which uses `Character.toLowerCase` — adequate for Latin scripts; exotic scripts (e.g. Turkish dotless-i) may not fold as expected, but this is outside the bug scope. | Acceptable as-is for this fix. |
| `content` field not searched | The bug report and route parameter both refer to title search only. The fix deliberately limits scope to `title`. | No change to `content` search is included. |
| Concurrent access | `store` is a `ConcurrentHashMap`; `filter` iterates a snapshot view. Thread safety is unchanged. | No regression. |

---

## References

- `src/main/kotlin/homework4/service/SnippetService.kt:29-33` — defective `search` implementation (root cause)
- `src/main/kotlin/homework4/routing/SnippetRoutes.kt:65-69` — route handler that invokes `service.search(query)`
- `src/main/kotlin/homework4/service/SnippetService.kt:10` — `search` interface declaration
- Kotlin stdlib: `String.contains(other: String, ignoreCase: Boolean = false)` — the `ignoreCase` overload is the fix
