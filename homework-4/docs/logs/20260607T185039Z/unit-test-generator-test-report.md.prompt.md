# Agent: unit-test-generator
# Model: claude-haiku-4-5
# Allowed tools (least privilege): Read,Grep,Glob,Write,Bash

You are running as the agent defined by the following specification.
Follow it exactly. Produce ONLY the declared output artifact by writing it
to this absolute path:

    /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/context/bugs/003-security-admin-token-authorization/test-report.md

----- AGENT SPECIFICATION -----
---
name: unit-test-generator
description: >
  Generates and runs unit tests for the code changed by the Bug Fixer. Reads fix-summary.md
  and the changed files, writes tests for new/changed code only following the project's test
  framework and the unit-tests-FIRST skill, runs them, and writes test-report.md.
model: claude-haiku-4-5
tools: [Read, Grep, Glob, Write, Bash]   # writes new test files and runs tests; no source Edit
skills:
  - unit-tests-FIRST
inputs:
  - context/bugs/<id>/fix-summary.md
  - the files listed as changed in fix-summary.md
outputs:
  - context/bugs/<id>/test-report.md
  - test files under src/test/kotlin/homework4/
---

# Unit Test Generator

## Role

Generate FIRST-compliant unit tests for the changed code and run them.

## Model choice

`claude-haiku-4-5` — test scaffolding against an explicit, well-specified target (the changed
code plus the FIRST checklist) is the most routine, highest-throughput step in the pipeline.
A fast, low-cost model is appropriate; the **unit-tests-FIRST** skill supplies the quality
constraints that keep the output rigorous despite the cheaper model.

## Skill

Use the **unit-tests-FIRST** skill (`skills/unit-tests-FIRST.md`). Every generated test must
pass its pre-submission checklist (Fast, Independent, Repeatable, Self-validating, Timely).

## Process

1. Read `fix-summary.md` to identify the changed/new code.
2. For each change, write tests covering the corrected behavior (the fixed boundary must now
   be impossible to violate) plus at least one happy path.
3. Use the project framework: `kotlin-test-junit`, and `testApplication { application {
   module(...) } }` for HTTP-level behavior; construct fresh collaborators per test.
4. Run the tests: `./gradlew :homework-4:test --rerun-tasks --console=plain`.
5. Write `test-report.md`.

## Output contract

`test-report.md` records, per test: name, the FIRST principles demonstrated, the changed code
covered, and the run result (pass/fail). Test files are committed under
`src/test/kotlin/homework4/`.

## Guardrails

- Scope: tests for **changed/new code only** — do not test unrelated modules.
- No flaky constructs (network, sleeps, time/random/locale dependence).
- Tests must actually run and their results recorded; do not report untested code as covered.

----- LOADED SKILLS (apply these) -----
### Skill: unit-tests-FIRST.md
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

----- INPUT ARTIFACTS -----
### fix-summary.md
```
# Fix Summary — 003-security-admin-token-authorization

## Overall Status

**PASS** — Both security vulnerabilities remediated. All 28 tests across 4 suites passed after
the changes.

---

## Changes Made

### Change 1 — `src/main/kotlin/homework4/utils/TokenAuth.kt`

**Location:** Full file replacement (lines 1–22)

**Vulnerabilities addressed:**
- CWE-798 (Hardcoded Credentials) — removed literal `"s3cr3t-admin-token"` from source
- CWE-208 (Observable Timing Discrepancy) — replaced `==` with constant-time SHA-256 digest
  comparison via `MessageDigest.isEqual`

**Before:**
```kotlin
package homework4.utils

/**
 * Token-based authorization for write operations.
 *
 * SECURITY ISSUE (seeded):
 *   1. Hardcoded secret — the admin token is embedded directly in source and would be
 *      committed to version control.
 *   2. Insecure comparison — `==` performs a non-constant-time String comparison, which is
 *      vulnerable to timing attacks. Token comparison should use a constant-time check and
 *      the secret should come from configuration/environment, not a literal.
 */
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

**After:**
```kotlin
package homework4.utils

import java.security.MessageDigest

object TokenAuth {
    private val adminToken: String? = System.getenv("ADMIN_TOKEN")

    fun isAuthorized(providedToken: String?): Boolean {
        if (providedToken == null) return false
        val expected = adminToken ?: return false
        return MessageDigest.isEqual(sha256(expected), sha256(providedToken))
    }

    private fun sha256(value: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
}
```

**Test result after this change (combined with Change 2):**
```
BUILD SUCCESSFUL in 3s
```
All 28 tests passed.

---

### Change 2 — `build.gradle.kts`

**Location:** Appended after the `dependencies { }` block (after line 26)

**Vulnerability addressed:** CWE-798 — test processes need to supply `ADMIN_TOKEN` via
`System.getenv()` without re-embedding the literal in application source. Injecting it only
into the test task environment keeps the token out of the shipped binary.

**Before (no `tasks.withType<Test>` block existed):**
```kotlin
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}
```

**After:**
```kotlin
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}

tasks.withType<Test>().configureEach {
    environment("ADMIN_TOKEN", "s3cr3t-admin-token")
}
```

**Test result after this change:**
```
BUILD SUCCESSFUL in 3s
```
All 28 tests passed.

---

## Manual Verification

To confirm the fix works end-to-end:

### 1. Verify environment-driven authorization

```bash
# Start the app with the token set
ADMIN_TOKEN=s3cr3t-admin-token ./gradlew :homework-4:run &

# Authorized request — expect 201 Created
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/snippets \
  -H "Content-Type: application/json" \
  -H "X-Api-Token: s3cr3t-admin-token" \
  -d '{"title":"hello","content":"world"}'
# Expected: 201

# Unauthorized request (wrong token) — expect 401
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/snippets \
  -H "Content-Type: application/json" \
  -H "X-Api-Token: wrong-token" \
  -d '{"title":"hello","content":"world"}'
# Expected: 401

# No token — expect 401
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/snippets \
  -H "Content-Type: application/json" \
  -d '{"title":"hello","content":"world"}'
# Expected: 401
```

### 2. Verify fail-closed behavior when `ADMIN_TOKEN` is absent

```bash
# Start the app WITHOUT setting ADMIN_TOKEN
unset ADMIN_TOKEN
./gradlew :homework-4:run &

# Any write request — expect 401
curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/snippets \
  -H "Content-Type: application/json" \
  -H "X-Api-Token: s3cr3t-admin-token" \
  -d '{"title":"hello","content":"world"}'
# Expected: 401  (fail-closed: no env var → no writes allowed)
```

### 3. Verify the literal is gone from source

```bash
grep -r "s3cr3t-admin-token" src/
# Expected: no output (literal removed from application source)

grep -r "s3cr3t-admin-token" build.gradle.kts
# Expected: one line in the test task env block (acceptable: dev/CI only, not shipped)
```

### 4. Run the full test suite

```bash
./gradlew :homework-4:test --rerun-tasks --console=plain
# Expected: BUILD SUCCESSFUL, all tests pass
```

---

## References

- `src/main/kotlin/homework4/utils/TokenAuth.kt` — both vulnerabilities fixed
- `build.gradle.kts` — test env injection added
- `context/bugs/003-security-admin-token-authorization/implementation-plan.md` — source plan
- `context/bugs/003-security-admin-token-authorization/research/verified-research.md` — L4 root-cause analysis
- CWE-798: https://cwe.mitre.org/data/definitions/798.html
- CWE-208: https://cwe.mitre.org/data/definitions/208.html
```

### Application source tree (read as needed): /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/src

----- TASK -----
Perform this agent's role for the bug in: /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/context/bugs/003-security-admin-token-authorization
Write the result to /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/context/bugs/003-security-admin-token-authorization/test-report.md in the format its spec/skill requires.
