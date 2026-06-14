# Verified Research — Bug 001: Oversized title accepted

## Verification Summary
- Verdict: FAIL
- Research Quality: L2 — Adequate (overall score: 0.94)
- Dimensions: reference accuracy 7/9, snippet fidelity 4/4, claim support 8/8, completeness 1.0

## Verified Claims
- `MAX_TITLE_LENGTH` is defined as `50` — verified at `SnippetValidator.kt:16` ✓
- The faulty condition `request.title.length > MAX_TITLE_LENGTH + 1` is at line 23 — verified at `SnippetValidator.kt:23` ✓
- The BUG A comment acknowledging the seeded defect spans lines 21–22 — verified at `SnippetValidator.kt:21-22` ✓
- The off-by-one logic: `51 > 51 == false` allows a 51-character title through — verified by code inspection of `SnippetValidator.kt:23` ✓
- The correct fix is `request.title.length > MAX_TITLE_LENGTH` (i.e., `> 50`) — verified by logical derivation ✓
- `post("/snippets")` handler is at line 21 — verified at `SnippetRoutes.kt:21` ✓
- `call.receive<CreateSnippetRequest>()` is at line 30 — verified at `SnippetRoutes.kt:30` ✓
- `SnippetValidator.validate(request)` is called at line 31 — verified at `SnippetRoutes.kt:31` ✓
- When validation returns empty errors, route proceeds to `service.create(...)` and responds `201 Created` — verified at `SnippetRoutes.kt:30-38` ✓
- OpenAPI spec defines `title` with `maxLength: 50` — verified at `openapi.yaml:81-84` ✓
- The full validator code block at lines 21–30 matches the quoted snippet exactly — verified at `SnippetValidator.kt:21-30` ✓
- The full route code block at lines 30–38 matches the quoted snippet exactly — verified at `SnippetRoutes.kt:30-38` ✓

## Discrepancies Found
- **R1 line numbers**: Research cites `openapi.yaml:83–85` for the `title` property snippet, but the `title:` key is at line 81, `type: string` at line 82, `minLength: 1` at line 83, and `maxLength: 50` at line 84. The correct range is **lines 81–84**, not 83–85. The snippet content itself is accurate; only the line numbers are wrong (off by 2).
- **Compact call path line attribution**: The compact call path at the top of the "Call Path" section states `SnippetRoutes.kt:31 — call.receive<CreateSnippetRequest>()`, but `call.receive` is at **line 30**, not line 31. Line 31 is `val errors = SnippetValidator.validate(request)`. The numbered list below the compact path correctly assigns `call.receive` to line 30, creating an internal inconsistency within the research document.

## Research Quality Assessment
- Level: L2 — Adequate
- Reasoning: The overall score of 0.94 (mean of reference accuracy 0.78, snippet fidelity 1.00, claim support 1.00, completeness 1.00) falls in the L3 (Solid) score band (0.85–0.94). However, the L3 hard gate requires ≥ 90% reference accuracy, and the measured reference accuracy is 7/9 (77.8%), which fails that gate. The two inaccurate references are line-number errors rather than fabricated references (the cited code exists in the correct files, just at different line numbers), so the "no fabricated references" gate for L3 does pass. Nevertheless, the reference accuracy gate failure caps the result at L2 — Adequate. The research is substantively correct — root cause identification, code snippets, and logical analysis are all accurate — but the line-number discrepancies prevent it from reaching the Solid threshold. To reach L3 or higher, the researcher should correct R1's line range to `openapi.yaml:81–84` and fix the compact call path to assign `call.receive` to line 30 instead of line 31.

## References
- `SnippetValidator.kt:16` — defines `MAX_TITLE_LENGTH = 50`
- `SnippetValidator.kt:21-22` — BUG A comment acknowledging the seeded off-by-one defect
- `SnippetValidator.kt:23` — faulty condition `request.title.length > MAX_TITLE_LENGTH + 1`
- `SnippetValidator.kt:21-30` — full validation block for the title length check
- `SnippetRoutes.kt:21` — `post("/snippets")` route handler entry point
- `SnippetRoutes.kt:30` — `call.receive<CreateSnippetRequest>()` deserialization
- `SnippetRoutes.kt:31` — `SnippetValidator.validate(request)` call
- `SnippetRoutes.kt:30-38` — full route logic from request parsing through `201 Created` response
- `openapi.yaml:81-84` — `title` property definition with `minLength: 1` and `maxLength: 50`
