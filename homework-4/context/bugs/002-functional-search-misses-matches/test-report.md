# Test Report — 002-functional-search-misses-matches

## Summary

Generated 10 unit tests for `InMemorySnippetService.search()` covering the case-insensitive search fix. All tests pass.

**Test Suite:** `homework4.service.SnippetServiceSearchTest`  
**File:** `src/test/kotlin/homework4/service/SnippetServiceSearchTest.kt`  
**Run Result:** ✅ BUILD SUCCESSFUL

---

## Changed Code Covered

- **File:** `src/main/kotlin/homework4/service/SnippetService.kt`
- **Method:** `InMemorySnippetService.search(query: String): List<Snippet>` (lines 29–31)
- **Change:** Added `ignoreCase = true` to `String.contains()` call to enable case-insensitive substring matching in snippet titles.

---

## Test Cases

### Test 1: `search returns results matching query in lowercase when title is mixed case`

**FIRST Principles Demonstrated:**
- ✅ **Fast:** In-memory service; no I/O or sleeps.
- ✅ **Independent:** Fresh service instance per test; no shared state.
- ✅ **Repeatable:** Deterministic inputs and assertions; no time/randomness.
- ✅ **Self-validating:** Explicit assertion on result size and title content.
- ✅ **Timely:** Directly covers the bug boundary — lowercase query against mixed-case title must match.

**Changed Code Covered:** `it.title.contains(query, ignoreCase = true)`

**Expected Behavior:** Snippet with title "Hello World" is found when searching for "hello".

**Run Result:** ✅ PASS

---

### Test 2: `search returns results matching query in uppercase when title is mixed case`

**FIRST Principles Demonstrated:**
- ✅ **Fast:** In-memory service; no I/O or sleeps.
- ✅ **Independent:** Fresh service instance per test; no shared state.
- ✅ **Repeatable:** Deterministic inputs and assertions; no time/randomness.
- ✅ **Self-validating:** Explicit assertion on result size and exact title match.
- ✅ **Timely:** Covers case-insensitive boundary — uppercase query against mixed-case title must match.

**Changed Code Covered:** `it.title.contains(query, ignoreCase = true)`

**Expected Behavior:** Snippet with title "Hello World" is found when searching for "HELLO".

**Run Result:** ✅ PASS

---

### Test 3: `search returns results matching query with mixed case when title is lowercase`

**FIRST Principles Demonstrated:**
- ✅ **Fast:** In-memory service; no I/O or sleeps.
- ✅ **Independent:** Fresh service instance per test; no shared state.
- ✅ **Repeatable:** Deterministic inputs and assertions; no time/randomness.
- ✅ **Self-validating:** Explicit assertion on result size and title match.
- ✅ **Timely:** Covers the symmetric case — mixed-case query against lowercase title must match.

**Changed Code Covered:** `it.title.contains(query, ignoreCase = true)`

**Expected Behavior:** Snippet with title "hello world" is found when searching for "HeLLo".

**Run Result:** ✅ PASS

---

### Test 4: `search returns empty list when no snippets match`

**FIRST Principles Demonstrated:**
- ✅ **Fast:** In-memory service; no I/O or sleeps.
- ✅ **Independent:** Fresh service instance per test; no shared state.
- ✅ **Repeatable:** Deterministic inputs and assertions; no time/randomness.
- ✅ **Self-validating:** Explicit assertion on empty result list.
- ✅ **Timely:** Happy path for mismatch case; confirms search returns empty results when no title contains the query.

**Changed Code Covered:** `store.values.filter { it.title.contains(query, ignoreCase = true) }`

**Expected Behavior:** Search for "goodbye" in a store containing "Hello World" returns empty list.

**Run Result:** ✅ PASS

---

### Test 5: `search filters multiple snippets and returns only matching ones`

**FIRST Principles Demonstrated:**
- ✅ **Fast:** In-memory service; no I/O or sleeps.
- ✅ **Independent:** Fresh service instance per test; no shared state.
- ✅ **Repeatable:** Deterministic inputs and assertions; no time/randomness.
- ✅ **Self-validating:** Explicit assertions on result count and content filtering.
- ✅ **Timely:** Covers filtering behavior — case-insensitive search correctly filters among multiple snippets.

**Changed Code Covered:** `store.values.filter { it.title.contains(query, ignoreCase = true) }`

**Expected Behavior:** Searching for "hello" in a store with 3 snippets returns 2 matches: "Hello World" and "HELLO AGAIN".

**Run Result:** ✅ PASS

---

### Test 6: `search is case-insensitive for partial substring matches`

**FIRST Principles Demonstrated:**
- ✅ **Fast:** In-memory service; no I/O or sleeps.
- ✅ **Independent:** Fresh service instance per test; no shared state.
- ✅ **Repeatable:** Deterministic inputs and assertions; no time/randomness.
- ✅ **Self-validating:** Explicit assertion on substring match across case boundary.
- ✅ **Timely:** Covers partial substring matching — "script" (lowercase) must match within "JavaScript" (mixed case).

**Changed Code Covered:** `it.title.contains(query, ignoreCase = true)`

**Expected Behavior:** Searching for "script" finds "JavaScript Code" and "Python Guide" is excluded.

**Run Result:** ✅ PASS

---

### Test 7: `search with exact title match case-insensitive`

**FIRST Principles Demonstrated:**
- ✅ **Fast:** In-memory service; no I/O or sleeps.
- ✅ **Independent:** Fresh service instance per test; no shared state.
- ✅ **Repeatable:** Deterministic inputs and assertions; no time/randomness.
- ✅ **Self-validating:** Explicit assertion on exact title match across case boundary.
- ✅ **Timely:** Happy path — case-insensitive exact title match.

**Changed Code Covered:** `it.title.contains(query, ignoreCase = true)`

**Expected Behavior:** Searching for "testsnippet" finds "TestSnippet".

**Run Result:** ✅ PASS

---

### Test 8: `search returns empty list when store is empty`

**FIRST Principles Demonstrated:**
- ✅ **Fast:** In-memory service; no I/O or sleeps.
- ✅ **Independent:** Fresh service instance per test; no shared state.
- ✅ **Repeatable:** Deterministic inputs and assertions; no time/randomness.
- ✅ **Self-validating:** Explicit assertion on empty result list.
- ✅ **Timely:** Edge case — searching an empty store returns empty results.

**Changed Code Covered:** `store.values.filter { ... }`

**Expected Behavior:** Search in an empty service returns empty list regardless of query.

**Run Result:** ✅ PASS

---

### Test 9: `search on empty query returns all snippets`

**FIRST Principles Demonstrated:**
- ✅ **Fast:** In-memory service; no I/O or sleeps.
- ✅ **Independent:** Fresh service instance per test; no shared state.
- ✅ **Repeatable:** Deterministic inputs and assertions; no time/randomness.
- ✅ **Self-validating:** Explicit assertion on result count and filtering logic.
- ✅ **Timely:** Edge case — empty query matches all snippets (contains "" is always true).

**Changed Code Covered:** `store.values.filter { it.title.contains(query, ignoreCase = true) }`

**Expected Behavior:** Searching with empty string "" returns all snippets in store.

**Run Result:** ✅ PASS

---

### Test 10: `search preserves snippet data in results`

**FIRST Principles Demonstrated:**
- ✅ **Fast:** In-memory service; no I/O or sleeps.
- ✅ **Independent:** Fresh service instance per test; no shared state.
- ✅ **Repeatable:** Deterministic inputs and assertions; no time/randomness.
- ✅ **Self-validating:** Explicit assertions on returned snippet id, title, and content.
- ✅ **Timely:** Happy path — verifies returned snippets preserve all fields, not just title.

**Changed Code Covered:** `store.values.filter { ... }` (ensures full Snippet objects returned)

**Expected Behavior:** Returned snippet from search contains correct id, title, and content.

**Run Result:** ✅ PASS

---

## Test Execution Summary

```
BUILD SUCCESSFUL in 1s
6 actionable tasks: 6 executed
```

All 10 unit tests compiled and executed successfully. No flaky tests, no external dependencies, no time/random/locale sensitivity.

---

## Coverage Assessment

✅ **Corrected Behavior Covered:** Case-insensitive search now properly matches titles regardless of case.  
✅ **Bug Boundary Covered:** Fixed boundary — lowercase query finds mixed-case title; uppercase query finds lowercase title.  
✅ **Happy Paths Covered:** Positive match, partial substring match, multiple snippet filtering.  
✅ **Edge Cases Covered:** Empty store, empty query, no matches.  
✅ **Data Integrity:** Returned snippets preserve all fields.

---

## References

- Implementation: `src/main/kotlin/homework4/service/SnippetService.kt:29–31`
- Tests: `src/test/kotlin/homework4/service/SnippetServiceSearchTest.kt`
- Fix summary: `context/bugs/002-functional-search-misses-matches/fix-summary.md`
