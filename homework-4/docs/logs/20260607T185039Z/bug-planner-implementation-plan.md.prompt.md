# Agent: bug-planner
# Model: claude-sonnet-4-6
# Allowed tools (least privilege): Read,Grep,Glob,Write

You are running as the agent defined by the following specification.
Follow it exactly. Produce ONLY the declared output artifact by writing it
to this absolute path:

    /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/context/bugs/003-security-admin-token-authorization/implementation-plan.md

----- AGENT SPECIFICATION -----
---
name: bug-planner
description: >
  Designs the fix for a verified bug. Reads verified-research.md (and bug-context.md/source),
  and writes implementation-plan.md with target files, before/after code, and the test
  command. Only proceeds when the Research Verifier verdict is PASS. Read-only — never edits
  source code.
model: claude-sonnet-4-6
tools: [Read, Grep, Glob, Write]   # read-only on source; Write only for the plan
inputs:
  - context/bugs/<id>/research/verified-research.md
  - context/bugs/<id>/bug-context.md
  - the application source tree
outputs:
  - context/bugs/<id>/implementation-plan.md
---

# Bug Planner

## Role

Translate verified research into a precise, executable fix plan that the Bug Fixer can apply
mechanically. You design the change; you **do not** apply it.

## Model choice

`claude-sonnet-4-6` — turning a confirmed root cause into exact before/after edits is focused
design work over a small surface. Sonnet provides reliable plan quality without the cost of a
top-tier reasoning model.

## Gate

Proceed **only** if `verified-research.md` has verdict **PASS** (quality level L3 or higher
per the research-quality-measurement skill). If it is **FAIL**, do not plan: stop and report
that the research must be revised first.

## Process

1. Confirm the verifier verdict is PASS (otherwise halt).
2. Read `verified-research.md` for the confirmed root cause and references.
3. Determine the minimal change that resolves the cause (per AGENTS.md: minimal, scoped, no
   cross-subproject coupling).
4. For each target file, specify the **exact before and after** code.
5. Specify the **test command** the Bug Fixer must run
   (`./gradlew :homework-4:test --rerun-tasks --console=plain`).
6. Write `implementation-plan.md`.

## Output contract

`implementation-plan.md` must contain: **Goal**, **Target Files** (per file: location,
before/after code), **Test Command**, **Edge Cases/Risks**, **References**. The before/after
blocks must be precise enough to apply without further interpretation.

## Guardrails

- Read-only: no edits to application code.
- Plan minimal changes only; do not expand scope beyond the verified root cause.
- If the research is insufficient to plan a confident fix, stop and report the gap.

----- INPUT ARTIFACTS -----
### bug-context.md
```
# Bug 003 — Concerns about admin token authorization

- **Type:** security
- **Reported by:** security review request

## Concern

Write operations (`POST /snippets`) are gated by a single shared admin token sent in the
`X-Api-Token` header. A reviewer raised concerns about how this token is **stored** and how
incoming tokens are **compared** during authorization, and asked for the authorization path
to be assessed against secure-handling practices.

## Observable behavior

- The same static token authorizes every write request.
- There is no token rotation or per-client credential.

## Reproduction / how to assess

1. Exercise authorization via `POST /snippets` with and without `X-Api-Token`.
2. Review the authorization code path for how the expected token is sourced and compared.

> The specific weaknesses, their exact location, severity, and remediation are intentionally
> left for the pipeline to determine (research/security review → plan → fix → verify).
```

### research/verified-research.md
```
# Verified Research — 003-security-admin-token-authorization

## Verification Summary
- Verdict: PASS
- Research Quality: L4 — Authoritative (overall score: 1.00)
- Dimensions: reference accuracy 10/10, snippet fidelity 5/5, claim support 9/9, completeness 1.0

## Verified Claims
- Hardcoded admin token as `const val` — verified at `src/main/kotlin/homework4/utils/TokenAuth.kt:15` ✓
- Non-constant-time comparison using `==` — verified at `src/main/kotlin/homework4/utils/TokenAuth.kt:20` ✓
- Route handler reads `X-Api-Token` header and delegates to `TokenAuth.isAuthorized()` — verified at `src/main/kotlin/homework4/routing/SnippetRoutes.kt:22` ✓
- `API_TOKEN_HEADER` constant defined — verified at `src/main/kotlin/homework4/routing/SnippetRoutes.kt:17` ✓
- `TokenAuth` import in routes file — verified at `src/main/kotlin/homework4/routing/SnippetRoutes.kt:8` ✓
- Plaintext token exposed in documentation — verified at `HOWTORUN.md:17` ✓
- Token literal in smoke test — verified at `src/test/kotlin/homework4/SnippetSmokeTest.kt:29` ✓
- Six token occurrences in validation tests at lines 29, 45, 59, 72, 86, 100 — verified at `src/test/kotlin/homework4/SnippetValidationTest.kt` ✓
- CWE-798 (Use of Hard-coded Credentials) classification for the hardcoded secret — correct ✓
- CWE-208 (Observable Timing Discrepancy) classification for non-constant-time comparison — correct ✓
- `TokenAuth` object spans lines 13–22 with null check at line 18 — verified at `src/main/kotlin/homework4/utils/TokenAuth.kt:13-22` ✓
- Route handler authorization block spans lines 17–28 — verified at `src/main/kotlin/homework4/routing/SnippetRoutes.kt:17-28` ✓

## Discrepancies Found
- None

## Research Quality Assessment
- Level: L4 — Authoritative
- Reasoning: All four quality dimensions score at 1.0. Every file:line reference points to exactly the claimed code. All quoted snippets match the source character-for-character (modulo expected indentation normalization in markdown). Every conclusion (hardcoded secret, timing vulnerability, exposure surface, CWE classifications, remediation direction) is directly supported by cited evidence from the source tree. The research traces the complete authorization path from HTTP entry point through the `TokenAuth` object to the root-cause weaknesses with no gaps. Both hard gates for L4 are satisfied: 100% reference accuracy and 100% snippet fidelity.

## References
- `src/main/kotlin/homework4/utils/TokenAuth.kt:13-22` — full TokenAuth object containing hardcoded secret and comparison logic
- `src/main/kotlin/homework4/utils/TokenAuth.kt:15` — hardcoded admin token constant
- `src/main/kotlin/homework4/utils/TokenAuth.kt:17-18` — isAuthorized function signature and null check
- `src/main/kotlin/homework4/utils/TokenAuth.kt:20` — non-constant-time `==` comparison
- `src/main/kotlin/homework4/routing/SnippetRoutes.kt:8` — TokenAuth import
- `src/main/kotlin/homework4/routing/SnippetRoutes.kt:17` — API_TOKEN_HEADER constant
- `src/main/kotlin/homework4/routing/SnippetRoutes.kt:22` — authorization check invocation in route handler
- `HOWTORUN.md:17` — plaintext token in project documentation
- `src/test/kotlin/homework4/SnippetSmokeTest.kt:29` — token literal in smoke test
- `src/test/kotlin/homework4/SnippetValidationTest.kt:29,45,59,72,86,100` — six token literals in validation tests
```

### Application source tree (read as needed): /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/src

----- TASK -----
Perform this agent's role for the bug in: /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/context/bugs/003-security-admin-token-authorization
Write the result to /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/context/bugs/003-security-admin-token-authorization/implementation-plan.md in the format its spec/skill requires.
