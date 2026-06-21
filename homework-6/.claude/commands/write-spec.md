---
description: Generate or regenerate specification.md for the banking pipeline from the project template.
argument-hint: "[optional: feature/section to focus on]"
allowed-tools: Read, Write, Edit, Glob
---

# /write-spec — Specification meta-agent (Agent 1)

You are **Agent 1 (Specification)**. Produce a complete, template-conformant
`homework-6/specification.md` for the AI-Powered Multi-Agent Banking Pipeline.

## Inputs to read first

1. `homework-6/TASKS.md` — the authoritative requirements (read-only).
2. `homework-6/specification-TEMPLATE-example.md` — the structure to follow (use the
   **Banking-Specific Specification Template**).
3. `homework-6/agents.md` — project-specific agent context and decision rules.
4. `homework-6/sample-transactions.json` — the input data the agents must handle.
5. `AGENTS.MD`, `.agents/docs/STACK.MD`, `.agents/docs/CODESTYLE.MD` — repo rules and stack.

## Message lifecycle (all four shared/ dirs must be used)

The spec MUST require every runtime agent to use `shared/processing/`, not just `input`/`output`/`results`:
an agent **claims** a message by atomically moving it from its inbox (`shared/input/` for the
validator, `shared/output/` for the fraud detector) into `shared/processing/` while it works, then
writes the produced message to the next stage (`shared/output/` or `shared/results/`) and **removes
the claimed file from `shared/processing/`**. State this in Implementation Notes and in each agent's
Low-Level Task prompt. `shared/processing/` only ever holds in-flight messages.

## Required output: `homework-6/specification.md`

Follow the template exactly. The document MUST contain all five sections:

1. **High-Level Objective** — one sentence describing what the pipeline does.
2. **Mid-Level Objectives** — 4-5 concrete, testable requirements.
3. **Implementation Notes** — `BigDecimal` for money (never `Double`/`float`); ISO 4217
   currency codes; audit trail with ISO-8601 timestamp, agent name, transaction id, outcome;
   PII (account numbers, names) never logged in plaintext; Kotlin single flat `homework6`
   package; kotlinx.serialization for JSON.
4. **Context** — beginning state (`sample-transactions.json`) and ending state (results in
   `shared/results/`, a pipeline summary, test coverage >= 90%).
5. **Low-Level Tasks** — **one entry per agent**, each in this exact format:
   ```
   Task: [Agent Name]
   Prompt: "[Exact prompt to give Claude Code]"
   File to CREATE: src/main/kotlin/homework6/[Name].kt
   Function to CREATE: fun process(message: AgentMessage): AgentMessage
   Details: [What the agent checks, transforms, or decides]
   ```

## Rules

- Do not invent transactions; derive decision rules from the 8 records in
  `sample-transactions.json` (e.g. invalid currency, negative amount, > $10k, odd-hour,
  cross-border).
- Keep monetary and currency rules consistent with `agents.md`.
- Write the file; then print a short checklist confirming each of the 5 sections is present
  and that every runtime agent has a Low-Level Task entry.
- If `$ARGUMENTS` is provided, focus edits on that section while keeping the rest intact.
