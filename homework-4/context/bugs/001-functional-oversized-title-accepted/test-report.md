# Test Report — Bug 001: Oversized Title Accepted

## Summary

**All tests PASS.** Two new test suites were generated to cover the off-by-one fix in `SnippetValidator.kt`:

- **SnippetValidatorTest** (unit tests): 10 tests, all passing ✓
- **SnippetValidationTest** (HTTP integration tests): 6 tests, all passing ✓
- **SnippetSmokeTest** (existing smoke tests): 2 tests, all passing ✓

**Total: 18 tests passing**

---

## Test Suites Generated

### 1. SnippetValidatorTest
**File:** `src/test/kotlin/homework4/validation/SnippetValidatorTest.kt`
**Execution time:** 0.001s
**Purpose:** Direct unit tests of the `SnippetValidator.validate()` method, covering the fixed boundary condition.

| Test Name | FIRST Principles Demonstrated | Changed Code Covered | Result |
|-----------|-------|-----------|--------|
| `accept title of exactly 50 characters (boundary, valid)` | F, I, R, S, T | Line 23: `> MAX_TITLE_LENGTH` (50-char case) | ✓ PASS |
| `reject title of exactly 51 characters (boundary, invalid — the fixed bug)` | F, I, R, S, T | Line 23: `> MAX_TITLE_LENGTH` (51-char case, the bug) | ✓ PASS |
| `reject title of 52 characters (beyond boundary)` | F, I, R, S, T | Line 23: `> MAX_TITLE_LENGTH` (52-char case) | ✓ PASS |
| `accept title of 1 character (minimum boundary, valid)` | F, I, R, S, T | Line 23: `isEmpty()` check (1-char case) | ✓ PASS |
| `reject empty title (minimum boundary, invalid)` | F, I, R, S, T | Line 23: `isEmpty()` check (empty case) | ✓ PASS |
| `reject empty content` | F, I, R, S, T | Line 32: content validation (unchanged) | ✓ PASS |
| `accept valid content (non-empty)` | F, I, R, S, T | Line 32: content validation (unchanged) | ✓ PASS |
| `reject both empty title and empty content` | F, I, R, S, T | Lines 23, 32: combined error reporting | ✓ PASS |
| `reject oversized title and empty content together` | F, I, R, S, T | Lines 23, 32: combined validation | ✓ PASS |
| `accept valid snippet request` | F, I, R, S, T | Lines 23, 32: happy path (no errors) | ✓ PASS |

**FIRST Compliance:**
- **F (Fast):** All tests run in-process, no network/disk/sleep. Execution time <1ms.
- **I (Independent):** Each test constructs a fresh `CreateSnippetRequest` and calls `validate()` with no shared state.
- **R (Repeatable):** No time/random/locale/env dependencies; pure function calls with deterministic inputs.
- **S (Self-validating):** Explicit `assertEquals()` and `assertTrue()` assertions; one behavior per test; descriptive names.
- **T (Timely):** Generated immediately for the fixed line (Line 23); covers the corrected boundary (51-char title now rejects) and happy path (50-char title accepts).

---

### 2. SnippetValidationTest
**File:** `src/test/kotlin/homework4/SnippetValidationTest.kt`
**Execution time:** 0.020s
**Purpose:** HTTP-level integration tests using Ktor's `testApplication { }` to verify end-to-end behavior with the validator applied in the route handler.

| Test Name | FIRST Principles Demonstrated | Changed Code Covered | Result |
|-----------|--------|-----------|--------|
| `reject title of exactly 51 characters (boundary, fixed bug)` | F, I, R, S, T | Line 23 via `/snippets` POST endpoint | ✓ PASS |
| `accept title of exactly 50 characters (boundary, valid)` | F, I, R, S, T | Line 23 via `/snippets` POST endpoint | ✓ PASS |
| `reject title of 52 characters (beyond boundary)` | F, I, R, S, T | Line 23 via `/snippets` POST endpoint | ✓ PASS |
| `reject empty title` | F, I, R, S, T | Line 23 via `/snippets` POST endpoint | ✓ PASS |
| `reject empty content` | F, I, R, S, T | Line 32 via `/snippets` POST endpoint | ✓ PASS |
| `reject both empty title and empty content` | F, I, R, S, T | Lines 23, 32 via `/snippets` POST endpoint | ✓ PASS |

**FIRST Compliance:**
- **F (Fast):** In-process via `testApplication { application { module(...) } }`; no real server or network. Avg 3ms per test.
- **I (Independent):** Each test creates a fresh `InMemorySnippetService()` in a new test context; no cross-test state.
- **R (Repeatable):** Deterministic HTTP requests and responses; no external service calls; no time/randomness dependencies.
- **S (Self-validating):** Assert HTTP status codes (`HttpStatusCode.Created`, `HttpStatusCode.BadRequest`) and response body contents; one behavior per test.
- **T (Timely):** Generated immediately for the changed validator; covers the corrected boundary (51-char now returns 400) and happy path (50-char returns 201).

---

## Existing Tests (Pre-existing)

### SnippetSmokeTest
**File:** `src/test/kotlin/homework4/SnippetSmokeTest.kt`
**Execution time:** Not separately measurable (lumped into overall build)
**Status:** Both tests passing ✓

These pre-existing smoke tests verify the happy path and auth guard and remain fully functional:
- `create and retrieve a snippet` — 201 Created + retrieval ✓
- `reject creation without a valid token` — 401 Unauthorized ✓

---

## Coverage Analysis

### Fixed Bug (Line 23)

**Before fix:**
```kotlin
if (request.title.isEmpty() || request.title.length > MAX_TITLE_LENGTH + 1) {
```

**After fix:**
```kotlin
if (request.title.isEmpty() || request.title.length > MAX_TITLE_LENGTH) {
```

**Test coverage:**

| Case | Test | Expected Behavior | Result |
|------|------|---|---|
| 50-char title (MAX_TITLE_LENGTH) | `accept title of exactly 50 characters` | Accept (no error) | ✓ PASS |
| **51-char title (MAX_TITLE_LENGTH + 1, the bug)** | `reject title of exactly 51 characters (boundary, invalid — the fixed bug)` | **Reject with 400/error** | ✓ PASS |
| 52-char title (MAX_TITLE_LENGTH + 2) | `reject title of 52 characters` | Reject (still rejected before fix, now confirmed) | ✓ PASS |
| Empty title (edge) | `reject empty title` | Reject (covered by `isEmpty()`) | ✓ PASS |
| 1-char title (minimum) | `accept title of 1 character` | Accept (within bounds) | ✓ PASS |

**The fix is verified:** A 51-character title (the off-by-one case) now correctly triggers validation error, returning `400 Bad Request` at the HTTP level.

### Unchanged Code

Line 32 (content validation) remains unchanged but is covered by tests:
- `reject empty content` ✓
- `accept valid content (non-empty)` ✓

---

## Build Output

```
> Task :homework-4:test

BUILD SUCCESSFUL in 1s
6 actionable tasks: 6 executed
```

**Test Results XML Summary:**
- `TEST-homework4.validation.SnippetValidatorTest.xml`: 10 tests, 0 failures, 0 errors ✓
- `TEST-homework4.SnippetValidationTest.xml`: 6 tests, 0 failures, 0 errors ✓
- `TEST-homework4.SnippetSmokeTest.xml`: 2 tests, 0 failures, 0 errors ✓

---

## Scope & Guardrails

✓ **Tests target changed code only:** All generated tests exercise Line 23 (the fixed boundary) in `SnippetValidator.kt`.

✓ **No flaky constructs:** No network, disk, sleep, time/random/locale dependencies.

✓ **Tests actually ran:** Gradle test task executed; XML results verify all tests executed and passed.

✓ **In scope:** No unrelated modules tested; focus is solely on title validation and the fixed boundary condition.

---

## Recommendations

All critical test coverage has been provided. The 51-character title boundary (the fixed bug) is now thoroughly tested both at the unit (SnippetValidatorTest) and HTTP (SnippetValidationTest) levels. Future regression is unlikely.

**Follow-up (optional, outside this scope):** Consider adding parameterized tests for title lengths in the range [1..100] if exhaustive boundary coverage is desired, but the current suite covers the critical bug surface.
