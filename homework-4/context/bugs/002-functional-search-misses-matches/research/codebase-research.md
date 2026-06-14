# Codebase Research — Bug 002: Search misses matching snippets

## Reproduced Symptom

A snippet created with title `Hello World` is not returned when searching via
`GET /snippets?q=hello`. The search only matches when the query's letter casing exactly
matches the stored title (e.g., `q=Hello` works, `q=hello` does not).

## Traced Call Path

1. **Entry point** — the search route is registered at `GET /snippets` in
   `SnippetRoutes.kt:65`. The handler extracts the query parameter `q` and delegates to
   `service.search(query)`.

2. **Service dispatch** — the `search` method is defined on the `SnippetService` interface at
   `SnippetService.kt:10` and implemented in `InMemorySnippetService` at
   `SnippetService.kt:29–33`.

3. **Filtering logic** — the implementation filters stored snippets using
   `it.title.contains(query)` (line 32). Kotlin's `String.contains(other: String)` without an
   `ignoreCase` parameter defaults to **case-sensitive** matching.

## Root-Cause Claim

The search miss occurs because `InMemorySnippetService.search` performs a case-sensitive
`String.contains` comparison between the stored title and the user-supplied query. When the
query differs in letter case from the stored title (e.g., `"hello"` vs `"Hello World"`),
`contains` returns `false` and the snippet is excluded from results.

Kotlin's `String.contains` has an overload that accepts `ignoreCase: Boolean`, but it is not
used here. The current code calls the single-argument form which delegates to the platform's
case-sensitive `indexOf`.

## References

### Reference 1 — Search route handler

**File:** `src/main/kotlin/homework4/routing/SnippetRoutes.kt:65–69`

```kotlin
    // Search snippets by title.
    get("/snippets") {
        val query = call.request.queryParameters["q"].orEmpty()
        val results = service.search(query).map { it.toResponse() }
        call.respond(HttpStatusCode.OK, results)
    }
```

### Reference 2 — SnippetService interface declaration

**File:** `src/main/kotlin/homework4/service/SnippetService.kt:10`

```kotlin
    fun search(query: String): List<Snippet>
```

### Reference 3 — InMemorySnippetService.search implementation (root cause)

**File:** `src/main/kotlin/homework4/service/SnippetService.kt:29–33`

```kotlin
    override fun search(query: String): List<Snippet> {
        // BUG B (logic): search is meant to be case-insensitive, but `contains` defaults to
        // case-sensitive matching, so e.g. searching "hello" misses a title "Hello World".
        return store.values.filter { it.title.contains(query) }
    }
```

The defective expression is `it.title.contains(query)` on line 32. The `contains` call uses
the default `ignoreCase = false`, making the search case-sensitive.

### Reference 4 — Snippet data class (title field)

**File:** `src/main/kotlin/homework4/models/Snippet.kt:7–11`

```kotlin
data class Snippet(
    val id: Int,
    val title: String,
    val content: String,
)
```

The `title` field is stored as-is (no normalization at creation time), confirming that
case-insensitive matching must happen at query time.
