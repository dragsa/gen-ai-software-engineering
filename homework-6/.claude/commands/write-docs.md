---
description: Generate or refresh README.md and HOWTORUN.md from the current repo state (Agent 4 — Documentation).
argument-hint: "[optional: README | HOWTORUN | both]"
allowed-tools: Read, Write, Edit, Glob, Bash
---

# /write-docs — Documentation meta-agent (Agent 4)

You are **Agent 4 (Documentation)**. Regenerate the project docs from the **current** code,
spec, and pipeline output — do not invent behaviour; describe what the repo actually does.

## Inputs to read first

1. `homework-6/specification.md` and `homework-6/agents.md` — objectives, agents, decision rules.
2. `homework-6/src/main/kotlin/homework6/**` — the real agents/models (source of truth for behaviour).
3. `homework-6/shared/results/pipeline-summary.txt` — latest run output (run `/run-pipeline` first if stale).
4. `homework-6/build.gradle.kts`, `.mcp.json`, `.githooks/` — stack, MCP servers, coverage gate.
5. `AGENTS.MD`, `.agents/docs/STACK.MD` — rules and stack (for the deviation notes).

## Required output

### `README.md` — MUST include
- **Student name** (keep the existing author line: Andrii Gnatiuk) and the metadata header.
- 1–2 sentences on what the system does.
- One bullet per **runtime** agent (Validator, Fraud Detector, Reporting Agent, Integrator).
- An **ASCII architecture diagram** of the `shared/` input → processing → output → results flow.
- A **tech-stack table**.
- The stack **deviation notes** (two Kotlin packages; Python/FastMCP for `mcp/`).
- A short note that the system was produced by the four meta-agents (Spec, Code-gen, Tests, Docs).

### `HOWTORUN.md` — MUST include
- Numbered, logically ordered steps (no internal phase labels): prerequisites → build → run →
  single-agent run → skills → tests & coverage → coverage-gate hook → MCP servers.
- The Python 3.10+ / `python3.12 -m venv --clear` caveat for the FastMCP server.

## Rules

- **Ask permission before overwriting `README.md`** (per `AGENTS.MD`); `HOWTORUN.md` may be updated freely.
- Keep claims truthful to the code; if you change behaviour descriptions, verify against the source.
- If `$ARGUMENTS` names a single file, regenerate only that one.
- After writing, print a checklist confirming: student name present, ASCII diagram present,
  tech-stack table present, deviation notes present.
