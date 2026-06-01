# Homework-4 — Implementation Roadmap

A phased plan to deliver the **4-agent pipeline** (Bug Research Verifier → Bug Fixer → Security Verifier → Unit Test Generator) plus the sample app it operates on, runnable via a single command.

Stack per `.agents/docs/STACK.MD`: Kotlin 2.3.20 / JVM 21, Gradle Kotlin DSL, Ktor 3.4.3 (Netty), kotlinx.serialization, kotlin-test-junit.

---

## Phase 0 — Scaffolding & Conventions

**Goal:** Establish the subproject skeleton and folder contract.

- Create `homework-4/` Gradle subproject; register in root `settings.gradle.kts`.
- `build.gradle.kts` via version catalog (`application` plugin, `mainClass`).
- Folder layout: `agents/`, `skills/`, `context/bugs/XXX/`, `src/`, `tests/`, `demo/`, `docs/screenshots/`.
- Seed `README.md` (author info) and `HOWTORUN.md` placeholders.

**Files:** `settings.gradle.kts`, `homework-4/build.gradle.kts`, folder tree, `README.md`, `HOWTORUN.md`.
**Risks:** Version-catalog drift; ensure `FAIL_ON_PROJECT_REPOS` compliance.

---

## Phase 1 — Sample Mini Application (Task 5)

**Goal:** A small runnable Ktor app with **≥2 intentional bugs** and **≥1 intentional security issue**.

- Implement minimal REST endpoints under `homework4` package (entrypoint, routing, service, models, validation).
- Seed bugs (e.g. off-by-one, wrong comparison) and one security flaw (e.g. injection or hardcoded secret).
- Document each seed in `context/bugs/XXX/bug-context.md`.
- Ensure `./gradlew :homework-4:run` and `:homework-4:test` work (failing/incomplete tests acceptable pre-fix).

**Files:** `src/main/kotlin/...`, `tests/`, `context/bugs/XXX/bug-context.md`.
**Risks:** Keep scope fixable in one pipeline run; bugs must be discoverable, not obscure.

---

## Phase 2 — Skills (Tasks 1.2 & 4.2)

**Goal:** Author the two required skills the agents depend on.

- `skills/research-quality-measurement.md` — defines research-quality levels/labels used in `verified-research.md`.
- `skills/unit-tests-FIRST.md` — defines FIRST (Fast, Independent, Repeatable, Self-validating, Timely).

**Files:** the two skill files.
**Risks:** Skill output format must match required result-file sections exactly.

---

## Phase 3 — Agent Definitions (Tasks 1–4)

**Goal:** Author the four `*.agent.md` files with explicit, justified model selection.

- `research-verifier.agent.md` — strong reasoning model; verifies refs, emits `verified-research.md` using the quality skill.
- `bug-fixer.agent.md` — faster/cheaper model; applies `implementation-plan.md`, runs tests, emits `fix-summary.md`.
- `security-verifier.agent.md` — strong reasoning model; reviews changed code, emits `security-report.md` (no edits).
- `unit-test-generator.agent.md` — mid model; generates FIRST tests for changed code, emits `test-report.md`.
- Record model + justification in each frontmatter and in README.

**Files:** four `agents/*.agent.md`.
**Risks:** Each agent must read/write the correct artifacts and load the right skill automatically.

---

## Phase 4 — Single-Command Pipeline Orchestration

**Goal:** One command runs all agents in order with auto skill loading.

- `run-pipeline.sh` (or `npm run pipeline` equivalent) executing:
  Bug Researcher → Research Verifier → Bug Planner → Bug Fixer → Security Verifier → Unit Test Generator.
- No manual per-agent invocation between steps.
- Update `HOWTORUN.md` with the command.

**Files:** `run-pipeline.sh`, `HOWTORUN.md`.
**Risks:** Ordering and artifact hand-off between stages; idempotent reruns.

---

## Phase 5 — Pipeline Execution & Artifacts

**Goal:** Run the pipeline against the seeded app and produce real outputs.

- Generate `codebase-research.md`, `verified-research.md`, `implementation-plan.md`, `fix-summary.md`, `security-report.md`, `test-report.md`.
- Apply fixes so seeded bugs + security issue are resolved.
- Verify `./gradlew :homework-4:build` passes with zero errors (per AGENTS.md).

**Files:** `context/bugs/XXX/*.md`, fixed `src/`, generated `tests/`.
**Risks:** Tests must pass post-fix; build verification mandatory.

---

## Phase 6 — Evidence, Docs & Submission

**Goal:** Package deliverables for review.

- Capture screenshots → `docs/screenshots/`: pipeline run, fixes, security scan, unit tests.
- Finalize `README.md` (overview, run instructions, model justifications, author info) — **request permission before modifying README per AGENTS.md**.
- Open PR with summary and screenshots; commit `agents/`, `skills/`, `context/`, artifacts.

**Files:** `docs/screenshots/*`, `README.md`, PR.
**Risks:** README edits gated on user approval; ensure agentic folders committed.

---

## Cross-Cutting Constraints

- Build verification after every implementation task: `./gradlew :homework-4:build` (zero errors).
- No cross-subproject coupling; minimal, scoped changes.
- `TASKS.MD` is read-only; `README.md` edits require permission.
- Per project CLAUDE.md: Plan → **Approve** → Execute. This roadmap is the plan; execution awaits approval.
