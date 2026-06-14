# Verified Research — 002-functional-search-misses-matches

## Verification Summary
- Verdict: PASS
- Research Quality: L4 — Authoritative (overall score: 0.99)
- Dimensions: reference accuracy 4/4, snippet fidelity 4/4, claim support 9/9, completeness 0.95

## Verified Claims
- The search route is registered at `GET /snippets` in `SnippetRoutes.kt:65` — verified at `src/main/kotlin/homework4/routing/SnippetRoutes.kt:65` ✓
- The handler extracts the `q` query parameter and delegates to `service.search(query)` — verified at `src/main/kotlin/homework4/routing/SnippetRoutes.kt:66-67` ✓
- The `search` method is declared on the `SnippetService` interface at `SnippetService.kt:10` — verified at `src/main/kotlin/homework4/service/SnippetService.kt:10` ✓
- The implementation in `InMemorySnippetService` is at `SnippetService.kt:29–33` — verified at `src/main/kotlin/homework4/service/SnippetService.kt:29-33` ✓
- The filtering logic uses `it.title.contains(query)` on line 32 — verified at `src/main/kotlin/homework4/service/SnippetService.kt:32` ✓
- Kotlin's `String.contains(other: String)` without `ignoreCase` defaults to case-sensitive matching — verified (Kotlin standard library fact) ✓
- Kotlin's `String.contains` has an overload accepting `ignoreCase: Boolean` — verified (Kotlin standard library fact) ✓
- The `title` field in the `Snippet` data class is stored as-is with no normalization at creation time — verified at `src/main/kotlin/homework4/models/Snippet.kt:7-11` and `src/main/kotlin/homework4/service/SnippetService.kt:20-24` ✓
- Root cause: case-sensitive `String.contains` causes search misses when the query differs in letter case from the stored title — verified, fully supported by traced call path and source code ✓

## Discrepancies Found
- Reference 1 cites `SnippetRoutes.kt:65–69` but the quoted snippet includes the comment `// Search snippets by title.` from line 64, making the actual span lines 64–69. The route handler code itself (`get("/snippets") { ... }`) is correctly located at lines 65–69. This is a cosmetic line-range discrepancy; the reference still points to the correct code and the claim is accurate.

## Research Quality Assessment
- Level: L4 — Authoritative
- Reasoning: All four references point to the correct files and lines in the source tree. All four quoted snippets match the actual source exactly in content, whitespace, and identifiers (4/4 snippet fidelity). Every claim made in the research is directly supported by cited evidence — the call path from route handler through service interface to the filtering implementation is traced completely and accurately (9/9 claim support). The research covers the full scope of the reported symptom: entry point (`GET /snippets`), query extraction, service dispatch, filtering logic, the defective `contains` call, and the data model confirmation that no normalization occurs at creation time. The only minor issue is a one-line offset in Reference 1's cited range (65–69 vs actual 64–69), which does not affect the accuracy of the underlying claim. Hard gates for L4 are met: 100% reference accuracy (all references resolve to supporting code) and 100% snippet fidelity (all snippets are verbatim matches). Completeness is rated 0.95 rather than 1.0 because the research does not mention whether the `content` field should also be searchable (the bug report only mentions title search, but a thorough analysis might note the scope limitation). Overall score: mean(1.0, 1.0, 1.0, 0.95) = 0.99.

## References
- `src/main/kotlin/homework4/routing/SnippetRoutes.kt:65-69` — search route handler extracting `q` parameter and calling `service.search(query)`
- `src/main/kotlin/homework4/service/SnippetService.kt:10` — `search` method declaration on the `SnippetService` interface
- `src/main/kotlin/homework4/service/SnippetService.kt:29-33` — `InMemorySnippetService.search` implementation with case-sensitive `it.title.contains(query)` (root cause)
- `src/main/kotlin/homework4/service/SnippetService.kt:20-24` — `create` method confirming title is stored without normalization
- `src/main/kotlin/homework4/models/Snippet.kt:7-11` — `Snippet` data class definition with `title: String` field
