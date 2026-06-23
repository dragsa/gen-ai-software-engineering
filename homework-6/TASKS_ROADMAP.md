# Homework-6 — Implementation Roadmap (TASKS_ROADMAP)

A phased, checklist-driven plan to deliver the **Capstone: AI-Powered Multi-Agent Banking Pipeline**.

The assignment has two layers that must not be confused:

- **Meta-agents (the deliverable workflows)** — four AI/automation workflows (Spec, Code-gen, Tests, Docs) that *build* the system.
- **Runtime agents (the produced system)** — the cooperating pipeline components (Validator, Fraud Detector, third agent) that *process transactions*.

> **Source of truth:** `AGENTS.MD` (rules) and `homework-6/TASKS.md` (requirements, read-only). This roadmap is the Plan-phase artifact required by `CLAUDE.MD`. **No code is written until this plan is approved.**

---

## 0. Key design decisions (resolve before Phase 1)

These four decisions drive the whole build and map directly to the points flagged for this homework.

### 0.1 Meta-agents vs. runtime agents (do not conflate)

| Layer | What it is | Where it lives | Deliverable role |
|-------|-----------|----------------|------------------|
| **Meta-agent 1 — Spec** | Slash command that emits `specification.md` from the template | `.claude/commands/write-spec.md` | Produces the spec |
| **Meta-agent 2 — Code-gen** | Workflow that generates the pipeline, using **context7** to look up the framework | prompt + `research-notes.md` | Produces the code |
| **Meta-agent 3 — Tests** | Workflow that writes the test suite + wires the **coverage gate** | tests + `settings.json` hook | Produces tests |
| **Meta-agent 4 — Docs** | Workflow that generates `README.md` / `HOWTORUN.md` (with student name) | docs | Produces documentation |
| **Runtime agent — Validator** | Field/amount/ISO-4217 checks | `agents/transaction_validator.*` | Part of the system |
| **Runtime agent — Fraud Detector** | Risk scoring (high-value, odd hours, cross-border) | `agents/fraud_detector.*` | Part of the system |
| **Runtime agent — 3rd (Compliance / Settlement / Reporting)** | pick one | `agents/<third>.*` | Part of the system |

Rule of thumb: **meta-agents are Claude Code constructs (commands/hooks/MCP); runtime agents are program code.** The grade depends on *both* existing.

### 0.2 Runtime agents = callable Kotlin functions **with** standalone execution

`TASKS.md` requires (a) **file-based JSON message passing** through `shared/{input,processing,output,results}` and (b) agents that can be **run separately** (e.g. `transaction_validator --dry-run`). The preferred end-state is also that each agent be **callable as a function from a Kotlin `main` class** (the integrator).

Resolution — each runtime agent is implemented **once** as a Kotlin class exposing a pure function, and wrapped so it can also run as its own process:

```
class TransactionValidator {
    fun process(message: AgentMessage): AgentMessage   // pure, unit-testable, callable from integrator
}
fun main(args: Array<String>) {                        // standalone entrypoint (own mainClass)
    // reads shared/processing/*.json -> process() -> writes shared/output/*.json
    // supports --dry-run (validate-only, no file moves)
}
```

This satisfies all three constraints at once:
- **Callable from the integrator's `main`** → the orchestrator invokes `validator.process(msg)` directly (in-process), or
- **Separate execution** → each agent has its own `mainClass`, runnable via `./gradlew :homework-6:runValidator` (separate JVM process), communicating only through `shared/` JSON files.
- The integrator chooses the mode; the **business logic is identical** because both paths call the same `process()`.

> **Approved:** default the demo to **in-process orchestration** (fast, deterministic, easy to screenshot) while keeping **per-agent `main()` + Gradle run tasks** to demonstrate true separate execution. Both are wired; README documents both.

### 0.3 Stack: Kotlin for the pipeline, Python only for the FastMCP server

`.agents/docs/STACK.MD` mandates **Kotlin 2.3.20 / JVM 21 / Gradle Kotlin DSL**. The pipeline (integrator + runtime agents + tests) is therefore **Kotlin**.

**Structure — two packages (approved):** Kotlin classes are split into **`homework6.common`** (shared models + utils: `AgentMessage`, `TransactionData`, `BigDecimalSerializer`, `Iso4217`, `AuditLogger`, `SharedDirs`, `MessageIo`, `Samples`, `AgentRunner`) and **`homework6.agent`** (the runtime agents + their model: `TransactionValidator`, `FraudDetector`, `ReportingAgent`, `PipelineSummary`, `Integrator`). This is a focused middle ground under `AGENTS.MD` ("prefer simpler architecture") rather than STACK.MD's full `entrypoint/service/models/validation/utils` layout, which is intended for the Ktor web subprojects. The structure is recorded in `README.md`.

`TASKS.md` Task 4 explicitly mandates a **FastMCP** server (`mcp/server.py`, Python). Per STACK.MD's **Deviation Policy**, a subproject may deviate *only where its `TASKS.md` mandates it* — so the deviation is confined to `mcp/` (Python), exactly as `homework-5` deviated. Both the Python deviation and the single-package simplification are recorded in `README.md`. No cross-subproject coupling.

- Monetary amounts: Kotlin `BigDecimal` with explicit `RoundingMode` (the JVM equivalent of "never use `float`"). Currency: ISO 4217 allow-list.
- Coverage tooling: **Kover** (`./gradlew :homework-6:koverXmlReport` / `koverVerify`).

### 0.4 The 80% push gate (mandatory, blocking)

A **`pre-push` git hook** runs the test suite + Kover, parses line coverage, and **exits non-zero (blocks the push) when coverage < 80%**.

- Hook script committed at `homework-6/.githooks/pre-push` (repo-tracked, not the un-committable `.git/hooks/`).
- Activated via `git config core.hooksPath homework-6/.githooks` (documented in `HOWTORUN.md`; can be set in a Gradle/init step).
- Also surfaced as a Claude Code hook entry in `.claude/settings.json` so the gate also fires on a `git push` issued from inside a Claude session.
- Evidence: `docs/screenshots/03-hook-trigger.png` shows the push **blocked** at <80%, then passing at ≥80%.

---

## Global conventions (apply to every phase)

- **Plan → Approve → Execute** (`CLAUDE.MD`): this file is the plan; **stop for approval before Phase 0 execution**. Deviations require an updated plan.
- **Build verification** (`AGENTS.MD`): after every implementation phase, run `./gradlew :homework-6:build` and report the result; zero errors, warnings investigated.
- **File contract** (`AGENTS.MD`): `TASKS.md` is read-only; `HOWTORUN.md` auto-updated on any change affecting execution; `README.md` edited **only after asking permission**.
- **Code style** (`.agents/docs/CODESTYLE.MD`): no wildcard imports; one class/top-level function per file where practical; package names mirror directories.
- **Register the subproject:** add `include("homework-6")` to the root `settings.gradle.kts` (currently missing).
- **Screenshot gate:** a task is not "done" until its required screenshot exists in `docs/screenshots/` **and** is referenced in the PR description.
- **Screenshot naming convention:** files use a phase-numbered prefix `NN-<description>.png` (matching homework-4/-5), not the bare names in `TASKS.md`. Canonical set:
  - Phase 1 — `01-write-spec.png`
  - Phase 2 — `02-run-pipeline-log.png`, `02-run-pipeline-result.png` (≙ `pipeline-run.png`)
  - Phase 3 — `03-run-pipeline-skill.png` (≙ `skill-run-pipeline.png`), `03-validate-transactions-skill.png`, `03-hook-trigger.png` (≙ `hook-trigger.png`)
  - Phase 4 — `04-mcp-context7.png`, `04-mcp-custom-tool.png` (together ≙ `mcp-interaction.png`)
  - Phase 5 — `05-test-coverage.png` (≙ `test-coverage.png`)
- **Secrets:** never commit tokens; context7 config via `.mcp.json`, env-vars documented in `HOWTORUN.md`.

---

## Phase 0 — Scaffolding & conventions

**Goal:** subproject skeleton, folder contract, build wiring — no business logic yet.

- Register `homework-6` in root `settings.gradle.kts`.
- `homework-6/build.gradle.kts` from the version catalog: `application` plugin, Kover plugin, per-agent run tasks, `mainClass` = integrator.
- Folder tree:
  ```
  homework-6/
  ├── .claude/{commands/, settings.json}
  ├── .githooks/pre-push
  ├── agents/                      (runtime agent Kotlin sources may live under src/, see note)
  ├── mcp/server.py
  ├── shared/{input,processing,output,results}/.gitkeep
  ├── src/main/kotlin/homework6/{common,agent}/   (common = models+utils, agent = agents+model)
  ├── src/test/kotlin/homework6/
  ├── docs/{screenshots,logs}/
  ├── specification.md  agents.md  research-notes.md
  ├── .mcp.json  README.md  HOWTORUN.md
  └── (sample-transactions.json, TASKS.md — already present)
  ```
- Seed `README.md` / `HOWTORUN.md` placeholders; `.gitignore` for `.venv/`, `__pycache__/`, `build/`, `shared/**` runtime output, secrets.

**Files:** `settings.gradle.kts`, `homework-6/build.gradle.kts`, folder tree, placeholders.
**Risks:** version-catalog drift; `FAIL_ON_PROJECT_REPOS` compliance; Kover added to `libs.versions.toml`.

**Checklist:**

- [x] `include("homework-6")` added to root `settings.gradle.kts`
- [x] `homework-6/build.gradle.kts` (application + Kover plugins, per-agent run tasks, `mainClass`)
- [x] Kover added to `gradle/libs.versions.toml` (`0.9.1`) + root `build.gradle.kts` (`apply false`)
- [x] Folder tree created (`homework6.common` + `homework6.agent` packages)
- [x] `shared/{input,processing,output,results}/.gitkeep` present
- [x] `README.md` / `HOWTORUN.md` placeholders + `.gitignore`
- [x] **Gate:** `./gradlew :homework-6:build` succeeds on the empty skeleton — ✅ verified locally (BUILD SUCCESSFUL, 9 tasks; Kover `minBound(80)` DSL accepted).

---

## Phase 1 — Meta-agent 1: Specification (Task 1) ⭐⭐

**Goal:** produce the spec and the skill that generates it.

- **`.claude/commands/write-spec.md`** — slash command that emits a `specification.md` following `specification-TEMPLATE-example.md` when invoked.
- Run it to produce **`specification.md`** with all 5 required sections:
  1. High-Level Objective (one sentence).
  2. Mid-Level Objectives (4–5 testable items — e.g. >$10k flagged with risk score; rejects written to `shared/results/` with reason; ISO-8601 audit logs; coverage ≥ 90%).
  3. Implementation Notes (`BigDecimal` not float; ISO 4217; audit trail w/ timestamp, agent, txn id, outcome; PII — no plaintext account/name logging).
  4. Context (begin: `sample-transactions.json`; end: results in `shared/results/`, summary report, coverage ≥ 90%).
  5. Low-Level Tasks — **one entry per runtime agent** in the required `Task/Prompt/File/Function/Details` format.
- **`agents.md`** — extend with project-specific context (the runtime agents, message format, shared-dir protocol, decision rules below).

**Decision rules to encode in the spec** (derived from the 8 sample transactions):
- Validator rejects **TXN006** (currency `XYZ` ∉ ISO 4217) and **TXN007** (amount `-100.00` ≤ 0).
- Fraud flags **TXN002 / TXN005** (> $10k), notes **TXN003** ($9 999.99 near-threshold structuring), **TXN004** (02:47 odd-hour + cross-border DE).
- Third agent (if Reporting/Compliance): CTR-style report for ≥ $10k; cross-border list (DE, GB).

**Files:** `.claude/commands/write-spec.md`, `specification.md`, `agents.md`.

**Checklist:**

- [x] `.claude/commands/write-spec.md` skill authored
- [x] `specification.md` — all 5 sections present
- [x] Low-Level Tasks: one entry per runtime agent (`Task/Prompt/File/Function/Details`)
- [x] Decision rules for TXN001–TXN008 encoded in the spec
- [x] `agents.md` extended with project context (agents, message format, shared-dir protocol)
- [x] **Gate:** `write-spec` runs and regenerates a template-conformant spec — ✅ verified via `/write-spec` in Claude Code (regenerated `specification.md`; all 5 sections + per-agent Low-Level Tasks present)

---

## Phase 2 — Meta-agent 2: Build the runtime pipeline (Task 2) ⭐⭐⭐

**Goal:** ≥ 3 cooperating runtime agents + integrator, communicating via `shared/` JSON, built with **context7** assistance.

- **Models** (package `homework6`): `AgentMessage` (`message_id`, `timestamp`, `source_agent`, `target_agent`, `message_type`, `data`), `Transaction`, enums, `BigDecimal` serializer — all `@Serializable` (kotlinx.serialization).
- **Runtime agents** (each = callable class + standalone `main`, per decision 0.2):
  - `TransactionValidator` — required fields, amount > 0, ISO-4217 currency; `--dry-run` mode.
  - `FraudDetector` — risk score from high-value / odd-hour / cross-border signals; threshold → `flagged_for_review`.
  - **`ReportingAgent` (approved)** — aggregates processed results into the pipeline summary: counts (valid/flagged/rejected), total settled value per currency, CTR-style list of ≥ $10k txns, cross-border list (DE, GB). Writes the summary consumed by the MCP `pipeline://summary` resource.
- **Integrator** (the `main` class, package `homework6`): sets up `shared/` dirs, loads `sample-transactions.json`, runs agents **in order** (in-process by default; can dispatch to separate processes), monitors `shared/results/`. Every transaction ends in `shared/results/`.
- **context7 usage (mandatory):** during code-gen, run **≥ 2 context7 queries** (e.g. "Kotlin BigDecimal rounding", "kotlinx.serialization custom serializer") and document each in **`research-notes.md`** (search term, returned library ID, applied insight).

**Files:** `src/main/kotlin/homework6/common/` (models + utils), `src/main/kotlin/homework6/agent/` (agents, integrator, summary model), `research-notes.md`.
**Risks:** float creep (enforce `BigDecimal`); message-schema drift between agents; file-lock/ordering races in separate-process mode (mitigate: atomic move `processing`→`output`).

**Checklist:**

- [x] `AgentMessage` + `Transaction` models (`@Serializable`, `BigDecimal` serializer)
- [x] `TransactionValidator` (callable object + `main`, `--dry-run`)
- [x] `FraudDetector` (callable object + `main`)
- [x] `ReportingAgent` (callable object + `main`, `summarize`)
- [x] Integrator `Integrator.run(base, sample)` + `main` — seeds `shared/`, runs agents in order through `processing/`
- [x] ≥ 2 context7 queries documented in `research-notes.md`
- [x] Logic verified against the 8-txn ground-truth table (Python simulation matches spec exactly)
- [x] **Gate:** `./gradlew :homework-6:run` lands all 8 txns in `shared/results/` as valid JSON — ✅ verified (TXN001–008 + `pipeline-summary.json`/`.txt`); captured `02-run-pipeline-log.png`, `02-run-pipeline-result.png`

---

## Phase 3 — Meta-agent 3: Skills & the coverage gate (Task 3) ⭐⭐

**Goal:** first-class commands + the blocking 80% gate.

- **`.claude/commands/run-pipeline.md`** — checks `sample-transactions.json`, clears `shared/`, runs the pipeline, summarizes `shared/results/`, reports rejects + reasons.
- **`.claude/commands/validate-transactions.md`** — runs validator in `--dry-run`; reports total/valid/invalid + reasons as a table.
- **Coverage gate** (decision 0.4): `.githooks/pre-push` runs `koverVerify -PenforceCoverage`, blocks push < 80%; mirrored in `.claude/settings.json` (Claude-session guard).

> **Sequencing note:** the test suite is written in **Phase 5** (Task 5 / Agent 4), so coverage is still 0% here. That is fine for Task 3 — `hook-trigger.png` only needs to show the hook **firing / blocking the push**, which a 0%-coverage repo does. After Phase 5 the same hook passes; re-run then if you also want to show the green state.

**Files:** the two command files, `.githooks/pre-push`, `.githooks/claude-pre-push-guard.sh`, `.githooks/README.md`, `.claude/settings.json`.

**Checklist:**

- [x] `.claude/commands/run-pipeline.md`
- [x] `.claude/commands/validate-transactions.md`
- [x] `.githooks/pre-push` runs `koverVerify -PenforceCoverage` and blocks push when coverage < 80%
- [x] Gate mirrored in `.claude/settings.json` (PreToolUse guard blocks `git push` in a Claude session)
- [x] `git config core.hooksPath homework-6/.githooks` documented in `HOWTORUN.md`
- [x] **Gate:** ✅ verified on machine — `/run-pipeline` (correct summary, `processing/` empty), `/validate-transactions` (8/6/2), and `git push` **blocked** by the pre-push hook at 0% coverage (`koverVerify FAILED`). Captured `03-run-pipeline-skill.png`, `03-validate-transactions-skill.png`, `03-hook-trigger.png`.

---

## Phase 4 — MCP integration (Task 4) ⭐⭐

**Goal:** two MCP servers — context7 (already used in Phase 2) + a custom FastMCP server.

- **`mcp/server.py`** (FastMCP, Python deviation): `get_transaction_status(transaction_id)` → status from `shared/results/`; `list_pipeline_results()` → summary of all processed txns; resource `pipeline://summary` → latest run summary text.
- **`.mcp.json`** — both `context7` (`npx @upstash/context7-mcp`) and `pipeline-status` (`python mcp/server.py`).
- `mcp/requirements.txt` (fastmcp); `.gitignore` covers `.venv/`.

**Files:** `mcp/server.py`, `mcp/requirements.txt`, `.mcp.json`.
**Risks:** server reads the same `shared/results/` schema the pipeline writes — keep one shared JSON contract; path resolution must be project-relative.

**Checklist:**

- [x] `mcp/server.py` — `get_transaction_status`, `list_pipeline_results`, resource `pipeline://summary` (logic verified against real `shared/results/`)
- [x] `mcp/requirements.txt` (fastmcp); `.gitignore` covers `.venv/` + `__pycache__/`
- [x] `.mcp.json` — both `context7` and `pipeline-status` configured
- [x] **Gate:** ✅ verified on machine — context7 resolved `/kotlin/kotlinx-kover` (`04-mcp-context7.png`) and the custom `pipeline-status` server answered `get_transaction_status(TXN006)` → rejected/currency reason (`04-mcp-custom-tool.png`).

---

## Phase 5 — Meta-agent 4: Tests & documentation (Task 5) ⭐⭐

**Goal:** test suite at ≥ 80% (aim ≥ 90%) + complete docs.

- **Tests** (`src/test/kotlin/homework6/`, kotlin-test-junit): unit tests per runtime agent (happy path + each reject reason from the 8 samples) + **1 integration test** of the full pipeline. Isolate from real `shared/` using a temp dir per test (no shared mutable state — FIRST). Once tests pass ≥ 80%, the Phase 3 pre-push hook flips from blocking to passing.
- **`README.md`** (ask permission before writing, per AGENTS.MD): **student name** (author / "Created by"), what the system does, one bullet per agent, **ASCII architecture diagram** of the pipeline flow, tech-stack table, Kotlin/Python deviation note.
- **`HOWTORUN.md`**: numbered setup → run → test → MCP → demo steps (incl. `git config core.hooksPath`).

**Files:** `src/test/kotlin/homework6/...`, `README.md`, `HOWTORUN.md`.

**Checklist:**

- [ ] Unit tests per runtime agent (happy path + each reject reason)
- [ ] 1 integration test of the full pipeline (isolated temp `shared/`)
- [ ] `README.md` — student name, agent bullets, ASCII diagram, tech-stack table, deviation note
- [ ] `HOWTORUN.md` — numbered setup → run → test → MCP → demo steps
- [ ] **Gate:** `./gradlew :homework-6:test` green; Kover ≥ 80% (aim ≥ 90%); `05-test-coverage.png` captured

---

## Phase 6 — Verification & submission

**Goal:** prove every success-criterion before opening the PR.

- Run full build + tests + pipeline + both MCP servers end-to-end; confirm the gate **blocks** a sub-80% push (temporarily drop a test to demonstrate) then passes.
- Collect all required screenshots in `docs/screenshots/` (numbered convention): `01-write-spec.png`, `02-run-pipeline-log.png`, `02-run-pipeline-result.png`, `03-run-pipeline-skill.png`, `03-validate-transactions-skill.png`, `03-hook-trigger.png`, `04-mcp-context7.png`, `04-mcp-custom-tool.png`, `05-test-coverage.png`.
- Walk the **Success Criteria** and **Deliverables Checklist** tables in `TASKS.md`; tick each.
- Write the **detailed PR description** (summary, AI tools used, how to verify, embedded screenshots) on `claude/homework-6-submission` → fork `main`; reviewer `Alexey-Popov`. (Bare PRs are rejected.)

**Verification step (per workflow rules):** re-run `./gradlew :homework-6:build` clean; diff-review all generated files; confirm no wildcard imports, no `float`/`Double` for money, no plaintext PII in logs, no secrets committed.

**Checklist:**

- [ ] Full build + tests + pipeline + both MCP servers pass end-to-end
- [ ] Coverage gate demonstrated blocking < 80%, then passing
- [ ] All 5 screenshots in `docs/screenshots/`
- [ ] `TASKS.md` Success Criteria + Deliverables Checklist all ticked
- [ ] No wildcard imports / no `float`/`Double` for money / no plaintext PII / no secrets
- [ ] Detailed PR description with embedded screenshots; reviewer `Alexey-Popov`

---

## Traceability — task → phase → evidence

| TASKS.md item | Phase | Screenshot / evidence |
|---|---|---|
| Task 1 — spec + `write-spec` skill | 1 | `specification.md`, `agents.md` |
| Task 2 — pipeline (≥3 agents) + context7 | 2 | `02-run-pipeline-log.png`, `02-run-pipeline-result.png`, `research-notes.md` |
| Task 3 — 2 skills + coverage gate hook | 3 | `03-run-pipeline-skill.png`, `03-validate-transactions-skill.png`, `03-hook-trigger.png` |
| Task 4 — context7 + custom FastMCP | 4 | `04-mcp-context7.png`, `04-mcp-custom-tool.png`, `.mcp.json`, `mcp/server.py` |
| Task 5 — tests + README (name) + HOWTORUN | 5 | `05-test-coverage.png`, README w/ ASCII diagram |
| Submission — 5 shots in PR | 6 | PR description |

---

## Decisions (approved)

1. **Third runtime agent:** ✅ **Reporting Agent**.
2. **Default demo mode:** ✅ **in-process orchestration**, with per-agent `main()` + Gradle run tasks kept for the separate-execution demo.
3. **Coverage:** ✅ gate at **80%** (mandatory), aim **≥ 90%**.
4. **Structure:** ✅ **two packages** — `homework6.common` (models + utils) and `homework6.agent` (agents + model); noted in `README.md`.

*Plan approved — ready to execute from Phase 0 on the next go-ahead.*
