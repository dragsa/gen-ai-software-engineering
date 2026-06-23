# agents.md — Multi-Agent Banking Pipeline

Project-specific agent context for Homework 6. Complements the repo-wide `AGENTS.MD`
(operational rules) and `.agents/docs/STACK.MD` (stack). Where they overlap, `AGENTS.MD` wins.

## Two layers of "agent"

- **Meta-agents (deliverable workflows)** — Claude Code constructs that *build* the system:
  - Agent 1 Specification (`/write-spec`)
  - Agent 2 Code-generation (uses context7)
  - Agent 3 Tests (+ coverage gate hook)
  - Agent 4 Documentation
  They are not part of the running system.
- **Runtime agents (the produced system)** — program code that *processes transactions*:
  `TransactionValidator`, `FraudDetector`, `ReportingAgent`, wired by the `Integrator`.

## Runtime agents

| Agent | Responsibility | Input status | Output status |
|-------|---------------|--------------|---------------|
| `TransactionValidator` | required fields, amount > 0, ISO 4217 currency | `received` | `validated` or `rejected` |
| `FraudDetector` | risk score from high-value / near-threshold / odd-hour / cross-border | `validated` | `settled` or `flagged_for_review` |
| `ReportingAgent` | aggregate run summary (counts, per-currency totals, CTR list, cross-border list) | terminal results | `summarized` |
| `Integrator` (orchestrator) | set up `shared/`, load samples, run agents in order, monitor results | — | — |

Each runtime agent is a Kotlin class exposing `fun process(message: AgentMessage): AgentMessage`
(pure, unit-testable, callable from the integrator) **and** a standalone `main()` so it can run
as a separate process communicating only through `shared/` JSON files.

## File-based communication protocol

```
shared/
├── input/       integrator drops initial messages here
├── processing/  agent moves a message here while working
├── output/      agent writes its result for the next agent
└── results/     final, terminal outcomes land here
```

Standard message envelope:

```json
{
  "message_id": "uuid4-string",
  "timestamp": "2026-03-16T10:00:00Z",
  "source_agent": "transaction_validator",
  "target_agent": "fraud_detector",
  "message_type": "transaction",
  "data": {
    "transaction_id": "TXN001",
    "amount": "1500.00",
    "currency": "USD",
    "status": "validated"
  }
}
```

## Decision rules (ground truth from `sample-transactions.json`)

- **Validator rejects:** TXN006 (currency `XYZ` ∉ ISO 4217), TXN007 (amount `-100.00` ≤ 0).
- **Fraud flags:** TXN002 & TXN005 (> $10,000 high-value), TXN004 (02:47 odd-hour + cross-border DE).
- **Fraud notes:** TXN003 ($9,999.99 just under $10k → near-threshold / structuring signal).
- **Cross-border** = `metadata.country` ≠ home country `US`. **Odd-hour** = hour ∈ [0, 5).

## Constraints

- Money: `BigDecimal` + `RoundingMode.HALF_UP`, never `Double`/`Float`. Amounts parse from JSON strings.
- Currency: ISO 4217 allow-list (USD, EUR, GBP, JPY, CHF, CAD, AUD, …).
- Audit log line: `timestamp(ISO-8601) | agent | transaction_id | outcome`; mask account numbers, never log names.
- Kotlin, two packages: `homework6.common` (shared models + utils) and `homework6.agent` (agents + their model); no wildcard imports.
- Build verification after each implementation task: `./gradlew :homework-6:build`.

## Stack deviations

- Pipeline is Kotlin per STACK.MD; the MCP server (`mcp/server.py`, Task 4) deviates to
  **Python + FastMCP** as mandated by `TASKS.md`. Both deviations are recorded in `README.md`.
