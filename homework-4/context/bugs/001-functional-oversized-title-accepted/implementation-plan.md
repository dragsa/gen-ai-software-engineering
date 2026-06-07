# Implementation Plan — Bug 001: Oversized Title Accepted

## Gate Check
- Verifier verdict: **PASS**
- Research quality: **L3 — Solid** (overall score 0.92)
- Proceeding with plan.

---

## Goal

Fix the off-by-one error in `SnippetValidator.validate` so that a title of exactly 51 characters is rejected with `400 Bad Request`, matching the documented contract (`1–50 characters`) and the OpenAPI `maxLength: 50` constraint.

---

## Root Cause (from verified research)

`SnippetValidator.kt:23` uses `> MAX_TITLE_LENGTH + 1` (i.e. `> 51`) as the upper-bound check.  
A 51-character title evaluates `51 > 51 == false`, bypassing the error and allowing the snippet to be stored.  
The correct operator is `> MAX_TITLE_LENGTH` (i.e. `> 50`), so `51 > 50 == true` triggers the error.

---

## Target Files

### `src/main/kotlin/homework4/validation/SnippetValidator.kt`

**Location:** line 23

**Before:**
```kotlin
        if (request.title.isEmpty() || request.title.length > MAX_TITLE_LENGTH + 1) {
```

**After:**
```kotlin
        if (request.title.isEmpty() || request.title.length > MAX_TITLE_LENGTH) {
```

**Change summary:** Remove `+ 1` from the length comparison so the guard correctly rejects any title longer than `MAX_TITLE_LENGTH` (50).

---

## No Other Files Require Changes

- `SnippetRoutes.kt` — calls `SnippetValidator.validate` correctly; no change needed.
- `openapi.yaml` — already declares `maxLength: 50`; no change needed.
- No new constants, no new files, no schema migrations.

---

## Test Command

```
./gradlew :homework-4:test --rerun-tasks --console=plain
```

Run from the repository root. All existing tests must pass. The existing test suite is expected to include (or should be verified to include) a case for a 51-character title returning `400`; if absent, the fixer should note it but the single-line source fix is independent of test authorship.

---

## Edge Cases / Risks

| Case | Expected behaviour after fix | Notes |
|------|------------------------------|-------|
| Title = 50 chars | `201 Created` | Boundary value; must still pass |
| Title = 51 chars | `400 Bad Request`, `title` error | The reported bug; must now fail |
| Title = 52+ chars | `400 Bad Request`, `title` error | Was already rejected before fix |
| Title = 1 char | `201 Created` | Minimum valid length; no change |
| Title = "" (empty) | `400 Bad Request`, `title` error | `isEmpty()` branch; unaffected |
| Title = null / missing | Handled upstream by JSON deserialisation; unaffected |  |

**Risk level:** Minimal. The change is a single token removal (`+ 1`) in one condition. It tightens validation; it cannot widen acceptance or break any previously-passing request.

---

## References

- `src/main/kotlin/homework4/validation/SnippetValidator.kt:16` — `MAX_TITLE_LENGTH = 50`
- `src/main/kotlin/homework4/validation/SnippetValidator.kt:23` — buggy condition (fix target)
- `src/main/kotlin/homework4/routing/SnippetRoutes.kt:31` — validator invocation
- `src/main/kotlin/homework4/routing/SnippetRoutes.kt:37` — snippet creation (happy path, unchanged)
- `src/main/resources/openapi.yaml:84` — `maxLength: 50` (authoritative contract)
- `context/bugs/001-functional-oversized-title-accepted/research/verified-research.md` — verified root-cause analysis
