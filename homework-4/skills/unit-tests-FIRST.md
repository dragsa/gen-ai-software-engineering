---
name: unit-tests-FIRST
description: >
  Defines the FIRST principles for unit tests (Fast, Independent, Repeatable, Self-validating,
  Timely). Use this skill when generating unit tests for changed code so that every test
  satisfies FIRST. Provides per-principle rules and a pre-submission checklist.
---

# Unit Tests — FIRST

This skill is used by the **Unit Test Generator** to ensure every generated test meets the
**FIRST** quality bar before it is written and recorded in `test-report.md`.

## The FIRST principles

### F — Fast
Tests run quickly so the suite is run often.
- No real network, disk, sleeps, or fixed waits.
- Exercise code in-process; use Ktor `testApplication { application { module(...) } }` rather
  than booting a real server on a port.
- Prefer the smallest unit that proves the behavior (validator/service over full HTTP when
  the logic lives below the route).

### I — Independent
Tests do not depend on each other or on execution order.
- Construct fresh collaborators per test (e.g. a new `InMemorySnippetService()`), never shared
  mutable state across tests.
- No reliance on data created by another test.
- Each test sets up exactly what it needs.

### R — Repeatable
Same result every run, on any machine.
- No dependence on current time, randomness, locale, or environment-specific values; inject
  them if needed.
- No external services. Deterministic inputs and assertions only.

### S — Self-validating
Each test asserts a clear pass/fail with no human inspection.
- Assert concrete outcomes (status codes, returned values, error fields) with `assertEquals`,
  `assertTrue`, etc.
- No `println` debugging in place of assertions; no "eyeball the output".
- Exactly one behavior under test per test method; name states the expected behavior.

### T — Timely
Tests are written alongside the change they cover.
- Generated immediately for the **changed/new code** in `fix-summary.md` — not deferred.
- Cover the corrected behavior (the bug must now be impossible) and at least one happy path.
- Include the boundary/edge that the fix addresses (e.g. the exact off-by-one boundary).

## Scope rule

Generate tests **only** for code identified as changed in `fix-summary.md`. Do not add tests
for unrelated, unchanged modules.

## Pre-submission checklist

Before recording a test, confirm all of the following:

- [ ] **Fast:** no network/disk/sleep; runs in-process.
- [ ] **Independent:** fresh fixtures; passes when run alone and in any order.
- [ ] **Repeatable:** no time/random/locale/env dependence.
- [ ] **Self-validating:** explicit assertions; one behavior per test; descriptive name.
- [ ] **Timely:** targets changed code; covers the fixed boundary + a happy path.
- [ ] **In scope:** only changed/new code from `fix-summary.md`.

## Reporting

For each test, `test-report.md` should record: the test name, which FIRST principles it
demonstrates, the changed code it covers, and the run result (pass/fail).
