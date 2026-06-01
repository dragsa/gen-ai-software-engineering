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
