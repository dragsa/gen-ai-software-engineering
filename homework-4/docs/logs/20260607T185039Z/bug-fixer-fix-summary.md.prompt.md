# Agent: bug-fixer
# Model: claude-sonnet-4-6
# Allowed tools (least privilege): Read,Grep,Glob,Edit,Write,Bash

You are running as the agent defined by the following specification.
Follow it exactly. Produce ONLY the declared output artifact by writing it
to this absolute path:

    /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/context/bugs/003-security-admin-token-authorization/fix-summary.md

----- AGENT SPECIFICATION -----
---
name: bug-fixer
description: >
  Executes an approved implementation plan: applies the specified code changes, runs the test
  command after each change, and documents the result in fix-summary.md. Follows the plan
  exactly; stops and reports if tests fail or the plan is ambiguous.
model: claude-sonnet-4-6
tools: [Read, Grep, Glob, Edit, Write, Bash]   # only agent that mutates source and runs tests
inputs:
  - context/bugs/<id>/implementation-plan.md
  - the application source tree
outputs:
  - context/bugs/<id>/fix-summary.md
---

# Bug Fixer

## Role

Apply the changes described in `implementation-plan.md` and document exactly what changed.
You execute a plan that was already researched, verified, and designed — you do not redesign.

## Model choice

`claude-sonnet-4-6` — by this stage the analysis is done and the work is mechanical: apply
precise, pre-specified edits and run tests. A balanced model gives reliable code edits at
lower cost than the reasoning-heavy verifier/security agents, matching the task's guidance to
use a faster/cheaper model for routine fixes.

## Process

1. Read the plan fully: target files, before/after code, and the test command.
2. Apply the changes file by file, matching the plan's before/after exactly.
3. After each change, run the test command (`./gradlew :homework-4:test --rerun-tasks
   --console=plain`).
4. If a test fails, **stop**, capture the failure, and document it — do not improvise a fix
   outside the plan.
5. Write `fix-summary.md`.

## Output contract

`fix-summary.md` must contain: **Changes Made** (per change: file, location, before/after,
test result), **Overall Status**, **Manual Verification** (clear steps to confirm by hand),
**References**.

## Guardrails

- Follow the plan strictly. If a deviation seems necessary, stop and report instead of
  silently changing scope.
- Keep changes minimal and scoped to the plan (per AGENTS.md).
- Do not modify read-only files (`TASKS.md`, `AGENTS.md`).

----- INPUT ARTIFACTS -----
### implementation-plan.md
```
# Implementation Plan — 003-security-admin-token-authorization

## Gate

Verified-research verdict: **PASS** (L4 — Authoritative, score 1.00). Proceeding.

---

## Goal

Remediate two security vulnerabilities in the authorization path:

1. **CWE-798 (Hardcoded Credentials)** — Remove the admin token literal from source by reading
   it from the `ADMIN_TOKEN` environment variable at runtime.
2. **CWE-208 (Observable Timing Discrepancy)** — Replace the non-constant-time `==` comparison
   with a constant-time comparison using SHA-256 digests and `MessageDigest.isEqual`.

No functional behavior changes — authorized requests must still be accepted and unauthorized
requests must still be rejected.

---

## Target Files

### 1. `src/main/kotlin/homework4/utils/TokenAuth.kt`

**Root cause addressed:** Both CWE-798 (hardcoded constant at line 15) and CWE-208
(non-constant-time `==` at line 20).

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

**Why this works:**
- `System.getenv("ADMIN_TOKEN")` removes the literal from source entirely. No default fallback
  is provided; an absent env var causes `isAuthorized` to return `false` (no silent access).
- Both the expected and provided tokens are hashed to a fixed-length (32-byte) SHA-256 digest
  before comparison. `MessageDigest.isEqual` compares two equal-length byte arrays in
  constant time, eliminating timing leakage.

---

### 2. `build.gradle.kts`

**Root cause addressed:** CWE-798 — tests need to supply the token they send in
`X-Api-Token`. Rather than re-hardcoding the secret in source, the test task is configured
to inject `ADMIN_TOKEN` via the process environment. The existing test files do not change.

**Before (relevant excerpt — no `tasks.test` block exists):**
```kotlin
dependencies {
    implementation(libs.ktor.server.core)
    // ...
    testImplementation(libs.kotlin.test.junit)
}
```

**After (append after the `dependencies` block):**
```kotlin
tasks.withType<Test>().configureEach {
    environment("ADMIN_TOKEN", "s3cr3t-admin-token")
}
```

**Why this is the right scope:** The test process is a child process launched by Gradle; env
vars set here are visible to `System.getenv()` in test code without requiring runtime
`System.setProperty` workarounds. The token value lives only in the build script (dev/CI
context), not in deployed application source.

---

## Test Command

```bash
./gradlew :homework-4:test --rerun-tasks --console=plain
```

All 8 existing tests across `SnippetSmokeTest` and `SnippetValidationTest` must pass without
modification.

---

## Edge Cases / Risks

| Concern | Analysis |
|---------|----------|
| `ADMIN_TOKEN` not set in production | `adminToken` is `null` → `isAuthorized` returns `false` → all writes rejected with 401. Fail-closed is the correct behavior. |
| SHA-256 for token hashing | SHA-256 is a standard fixed-length digest. Both digests are always 32 bytes, guaranteeing `MessageDigest.isEqual` runs in constant time. No HMAC is needed because the comparison is symmetric (same token on both sides). |
| `MessageDigest` thread safety | `MessageDigest.getInstance()` returns a new instance per call, so there are no concurrency issues. |
| Test token value in build script | `"s3cr3t-admin-token"` in `build.gradle.kts` is a dev/CI-only secret never shipped with the application binary. Acceptable residual risk. |
| No change to routes | `SnippetRoutes.kt` delegates entirely to `TokenAuth.isAuthorized(String?)`. The signature is unchanged; the route file needs no edit. |
| `import java.security.MessageDigest` | Standard JVM library; no new dependency. |

---

## References

- `src/main/kotlin/homework4/utils/TokenAuth.kt:13-22` — full object, both vulnerabilities
- `src/main/kotlin/homework4/utils/TokenAuth.kt:15` — hardcoded constant (CWE-798)
- `src/main/kotlin/homework4/utils/TokenAuth.kt:20` — non-constant-time `==` (CWE-208)
- `src/main/kotlin/homework4/routing/SnippetRoutes.kt:22` — call site (unchanged)
- `build.gradle.kts` — test task environment injection (new)
- `context/bugs/003-security-admin-token-authorization/research/verified-research.md` — L4 root-cause analysis
```

### Application source tree (read as needed): /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/src

----- TASK -----
Perform this agent's role for the bug in: /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/context/bugs/003-security-admin-token-authorization
Write the result to /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-4/context/bugs/003-security-admin-token-authorization/fix-summary.md in the format its spec/skill requires.
