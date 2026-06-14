# Agent: research-verifier
# Model: claude-opus-4-6
# Allowed tools (least privilege): Read,Grep,Glob,Write

You are running as the agent defined by the following specification.
Follow it exactly. Produce ONLY the declared output artifact by writing it
to this absolute path:

    /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/context/bugs/003-security-admin-token-authorization/research/verified-research.md

----- AGENT SPECIFICATION -----
---
name: research-verifier
description: >
  Fact-checks the Bug Researcher's output. Verifies every file:line reference and snippet
  against the source, grades research quality using the research-quality-measurement skill,
  and writes verified-research.md. Read-only — never edits source code.
model: claude-opus-4-6
tools: [Read, Grep, Glob, Write]   # read-only on source; Write only for its report
skills:
  - research-quality-measurement
inputs:
  - context/bugs/<id>/research/codebase-research.md
  - context/bugs/<id>/bug-context.md
  - the application source tree
outputs:
  - context/bugs/<id>/research/verified-research.md
---

# Bug Research Verifier

## Role

Independent fact-checker for the Bug Researcher. You confirm that the research is accurate
and verifiable before the Bug Planner relies on it. You **do not** edit source code.

## Model choice

`claude-opus-4-6` — verification is the accuracy-critical gate of the pipeline. Catching a
fabricated reference or a mismatched snippet here prevents a wrong fix downstream, so this
agent uses the strongest reasoning model.

## Skill

Use the **research-quality-measurement** skill (`skills/research-quality-measurement.md`) to
score quality and to format `verified-research.md`. Follow its levels, gates, and required
section layout exactly.

## Process

1. Read `bug-context.md` (the reported symptom) and `research/codebase-research.md` for the
   bug under review; use the symptom to judge the **completeness** dimension.
2. For every cited `file:line`, open the source and confirm it supports the claim.
3. Diff every quoted snippet against the source (whitespace and identifiers must match).
4. Record each mismatch as a discrepancy.
5. Apply the skill: compute the four dimension ratios, apply the hard gates, assign a quality
   level, and set the PASS/FAIL verdict.
6. Write `research/verified-research.md` in the skill's required format.

## Output contract

`verified-research.md` must contain, in order: **Verification Summary** (verdict + Research
Quality per skill), **Verified Claims**, **Discrepancies Found**, **Research Quality
Assessment** (level + reasoning), **References**.

## Guardrails

- Read-only: no edits to application code or to `codebase-research.md`.
- Never invent a verification; if a reference cannot be confirmed, mark it a discrepancy.
- A FAIL verdict must list concrete reasons the Bug Planner can act on.

----- LOADED SKILLS (apply these) -----
### Skill: research-quality-measurement.md
---
name: research-quality-measurement
description: >
  Defines how to measure and label the quality of codebase research. Use this skill when
  verifying a Bug Researcher's output and writing `verified-research.md`. It provides the
  quality levels, the scoring criteria, and the required result-file format.
---

# Research Quality Measurement

This skill is used by the **Bug Research Verifier** to assess the quality of
`research/codebase-research.md` and to produce `research/verified-research.md` in a
consistent, gradeable format.

## Purpose

Research is only useful to downstream agents (Bug Planner, Bug Fixer) if its claims are
**accurate, precise, and verifiable**. This skill turns "does the research hold up?" into a
repeatable measurement with explicit levels.

## Quality dimensions

Assess research against four dimensions:

1. **Reference accuracy** — every `file:line` citation points to the claimed code.
2. **Snippet fidelity** — quoted snippets match the source exactly (whitespace/identifiers).
3. **Claim support** — each conclusion is backed by cited evidence, not assumption.
4. **Completeness** — the research covers the reported symptom end to end (entry point →
   root cause) with no unexplained gaps.

## Scoring

For each dimension, count verified vs total items and compute a ratio:

- Reference accuracy = verified references / total references
- Snippet fidelity = matching snippets / total snippets
- Claim support = supported claims / total claims
- Completeness = judged 0.0–1.0 against the symptom's expected scope

Overall score = the **mean** of the four dimension ratios (0.0–1.0).

## Quality levels (labels)

Map the overall score to exactly one level. The level **must** also be capped by hard gates.

| Level | Label | Score band | Hard gates |
|-------|-------|-----------|------------|
| L4 | **Authoritative** | ≥ 0.95 | 100% reference accuracy AND 100% snippet fidelity |
| L3 | **Solid** | 0.85–0.94 | ≥ 90% reference accuracy; no fabricated references |
| L2 | **Adequate** | 0.70–0.84 | majority of references verify |
| L1 | **Weak** | 0.50–0.69 | some references verify |
| L0 | **Unreliable** | < 0.50 | — |

**Gate rule:** if any hard gate for a level fails, the result drops to the highest level whose
gates pass. Any **fabricated reference** (cites a file:line that does not support the claim)
caps the result at **L1 (Weak)** regardless of score.

## Verdict

- **PASS** — level is **L3 (Solid)** or higher. Research is safe for the Bug Planner to use.
- **FAIL** — level is **L2 (Adequate)** or lower. Research must be revised before planning.

## Required result-file format

`verified-research.md` **must** contain these sections, in order:

```markdown
# Verified Research — <bug id>

## Verification Summary
- Verdict: PASS | FAIL
- Research Quality: <Lx — Label> (overall score: 0.00)
- Dimensions: reference accuracy X/Y, snippet fidelity X/Y, claim support X/Y, completeness 0.0

## Verified Claims
- <claim> — verified at `path:line` ✓

## Discrepancies Found
- <claim> — <what was wrong> (expected … / found …) at `path:line`
- (state "None" if there are no discrepancies)

## Research Quality Assessment
- Level: <Lx — Label>
- Reasoning: <why this level, referencing dimensions and any gate caps>

## References
- `path:line` — <what it shows>
```

## Usage checklist for the verifier

- Open each cited `file:line` and confirm it supports the claim.
- Diff each quoted snippet against the source.
- Record every mismatch under **Discrepancies Found**.
- Compute dimension ratios, apply gates, assign the level, set the verdict.
- Write `verified-research.md` exactly in the format above.

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

### research/codebase-research.md
```
# Codebase Research — Bug 003: Admin Token Authorization

## Reproduced Symptom

Write operations (`POST /snippets`) are protected by a single shared admin token transmitted
via the `X-Api-Token` request header. The authorization code path exhibits two security
weaknesses:

1. **Hardcoded secret** — the expected admin token is embedded as a string literal directly
   in source code, which means it is committed to version control and visible to anyone with
   repository access.
2. **Non-constant-time comparison** — the incoming token is compared against the expected
   value using Kotlin's `==` operator (which delegates to `String.equals`), a comparison that
   short-circuits on the first differing character. This is susceptible to timing side-channel
   attacks that can leak the token value one character at a time.

Additionally, the plaintext token value is exposed in the project's `HOWTORUN.md`
documentation (line 17), the OpenAPI spec header description, and across multiple test files,
broadening the exposure surface.

## Traced Call Path

### 1. Entry point — route handler

The `POST /snippets` route is registered in `SnippetRoutes.kt`. The handler reads the
`X-Api-Token` header and delegates to `TokenAuth.isAuthorized()`:

**`src/main/kotlin/homework4/routing/SnippetRoutes.kt:17-28`**

```kotlin
private const val API_TOKEN_HEADER = "X-Api-Token"

fun Route.registerSnippetRoutes(service: SnippetService) {
    // Create a snippet (requires authorization).
    post("/snippets") {
        if (!TokenAuth.isAuthorized(call.request.headers[API_TOKEN_HEADER])) {
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(listOf(ValidationError("authorization", "invalid or missing API token"))),
            )
            return@post
        }
```

The `TokenAuth` import is at line 8:

```kotlin
import homework4.utils.TokenAuth
```

### 2. Authorization logic — `TokenAuth` object

The `TokenAuth` singleton contains both the stored secret and the comparison logic:

**`src/main/kotlin/homework4/utils/TokenAuth.kt:13-22`**

```kotlin
object TokenAuth {
    // Hardcoded secret (seeded vulnerability).
    private const val ADMIN_TOKEN = "s3cr3t-admin-token"

    fun isAuthorized(providedToken: String?): Boolean {
        if (providedToken == null) return false
        // Non-constant-time comparison (seeded vulnerability).
        return providedToken == ADMIN_TOKEN
    }
}
```

### 3. Token exposure in documentation and tests

The plaintext token value `s3cr3t-admin-token` also appears in:

- **`HOWTORUN.md:17`** — `- \`POST /snippets\` — create (header \`X-Api-Token: s3cr3t-admin-token\`)`
- **`src/test/kotlin/homework4/SnippetSmokeTest.kt:29`** — `header("X-Api-Token", "s3cr3t-admin-token")`
- **`src/test/kotlin/homework4/SnippetValidationTest.kt:29,45,59,72,86,100`** — six occurrences of the same literal in test requests.

## Root-Cause Claim

The authorization path has two distinct security weaknesses, both located in
`src/main/kotlin/homework4/utils/TokenAuth.kt`:

### Weakness 1: Hardcoded secret (CWE-798)

**Location:** `src/main/kotlin/homework4/utils/TokenAuth.kt:15`

```kotlin
    private const val ADMIN_TOKEN = "s3cr3t-admin-token"
```

The admin token is a compile-time constant embedded in the source file. Because it is a
`const val`, the literal string is inlined at every usage site by the Kotlin compiler.
This means:

- The secret is visible in the repository to all contributors and anyone with read access.
- It cannot be rotated without a code change, rebuild, and redeployment.
- It is not loaded from an environment variable, secrets manager, or external configuration.
- The same value is also hardcoded in documentation (`HOWTORUN.md:17`) and test files,
  further increasing exposure.

### Weakness 2: Non-constant-time string comparison (CWE-208)

**Location:** `src/main/kotlin/homework4/utils/TokenAuth.kt:20`

```kotlin
        return providedToken == ADMIN_TOKEN
```

Kotlin's `==` on `String` delegates to `java.lang.String.equals()`, which compares
character-by-character and returns `false` as soon as a mismatch is found. The time taken
by the comparison therefore varies depending on how many leading characters of the provided
token match the expected token. An attacker can exploit this timing difference to deduce the
secret token one character at a time by measuring response latencies (a timing side-channel
attack).

The secure alternative is a constant-time comparison function (e.g.,
`java.security.MessageDigest.isEqual()` or an HMAC-based comparison) that always examines
every byte regardless of match position.

## References

| # | File | Line(s) | Snippet |
|---|------|---------|---------|
| 1 | `src/main/kotlin/homework4/utils/TokenAuth.kt` | 15 | `private const val ADMIN_TOKEN = "s3cr3t-admin-token"` |
| 2 | `src/main/kotlin/homework4/utils/TokenAuth.kt` | 17 | `fun isAuthorized(providedToken: String?): Boolean {` |
| 3 | `src/main/kotlin/homework4/utils/TokenAuth.kt` | 18 | `if (providedToken == null) return false` |
| 4 | `src/main/kotlin/homework4/utils/TokenAuth.kt` | 20 | `return providedToken == ADMIN_TOKEN` |
| 5 | `src/main/kotlin/homework4/routing/SnippetRoutes.kt` | 17 | `private const val API_TOKEN_HEADER = "X-Api-Token"` |
| 6 | `src/main/kotlin/homework4/routing/SnippetRoutes.kt` | 22 | `if (!TokenAuth.isAuthorized(call.request.headers[API_TOKEN_HEADER])) {` |
| 7 | `src/main/kotlin/homework4/routing/SnippetRoutes.kt` | 8 | `import homework4.utils.TokenAuth` |
| 8 | `HOWTORUN.md` | 17 | `` - `POST /snippets` — create (header `X-Api-Token: s3cr3t-admin-token`) `` |
| 9 | `src/test/kotlin/homework4/SnippetSmokeTest.kt` | 29 | `header("X-Api-Token", "s3cr3t-admin-token")` |
| 10 | `src/test/kotlin/homework4/SnippetValidationTest.kt` | 29 | `header("X-Api-Token", "s3cr3t-admin-token")` |
```

### Application source tree (read as needed): /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/src

----- TASK -----
Perform this agent's role for the bug in: /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/context/bugs/003-security-admin-token-authorization
Write the result to /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/context/bugs/003-security-admin-token-authorization/research/verified-research.md in the format its spec/skill requires.
