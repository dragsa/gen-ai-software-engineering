# Homework 6: Final Capstone — AI-Powered Multi-Agent Banking Pipeline

- **Student Name**: Andrii Gnatiuk
- **Date Submitted**: 23.06.2026
- **AI Tools Used**: Claude (Anthropic) via Cowork

---

## What this is

An AI-powered, multi-agent banking transaction pipeline. It ingests raw transactions from
`sample-transactions.json`, validates them, scores them for fraud risk, and produces per-transaction
results plus a run summary. The agents are decoupled: they communicate only by passing JSON
messages through the `shared/` directories, so each can run in-process (default) or as its own
standalone process.

The project is itself the output of **four meta-agents** (Claude Code workflows): Agent 1 wrote the
specification, Agent 2 generated the pipeline code (using the **context7** MCP for framework lookups),
Agent 3 added the skills and the coverage-gate hook, and Agent 4 produced the tests and this
documentation. The meta-agents are the workflow; the runtime agents below are the running system.

## Runtime agents

- **Transaction Validator** — checks required fields, a positive `BigDecimal` amount, and an
  ISO 4217 currency; marks each message `validated` or `rejected` (with a reason).
- **Fraud Detector** — scores each validated transaction 0–100 from weighted signals (high-value
  > $10,000, odd-hour 00:00–04:59, cross-border, near-threshold) and sets `flagged_for_review`
  (high-value, or odd-hour **and** cross-border) or `settled`.
- **Reporting Agent** — aggregates the terminal results into a summary: counts by status,
  per-currency settled totals, a CTR-style list of transactions ≥ $10,000, and a cross-border list;
  writes `pipeline-summary.json` / `.txt` (the latter served by the MCP `pipeline://summary` resource).
- **Integrator** — the orchestrator: seeds `shared/input/`, drives the agents in order through the
  claim-and-clear lifecycle, and reports. Implements no business rules itself.

## Architecture

```
 sample-transactions.json
          │
          ▼
   ┌──────────────┐   seeds          ┌───────────────────────────────────────────┐
   │  Integrator  │ ───────────────► │  shared/   input/  processing/  output/  results/
   └──────────────┘                  └───────────────────────────────────────────┘
          │  runs agents in order (each: claim → processing/ → write → clear)
          ▼
   input/ ──► [ Transaction Validator ] ──► output/        (validated)
                                        └──► results/       (rejected)
   output/ ─► [ Fraud Detector ]        ──► results/        (settled / flagged_for_review)
   results/ ─► [ Reporting Agent ]      ──► results/pipeline-summary.json + .txt
                                                   │
                                                   ▼
                              MCP  pipeline-status  (get_transaction_status,
                                                     list_pipeline_results, pipeline://summary)
```

## Tech stack

| Concern | Choice |
|---------|--------|
| Language / runtime | Kotlin 2.3.20 / JVM 21 |
| Build | Gradle (Kotlin DSL), version catalog |
| Serialization | kotlinx.serialization (JSON) |
| Money | `java.math.BigDecimal` (`RoundingMode.HALF_UP`) — never `Double`/`Float` |
| Tests | kotlin-test-junit |
| Coverage | Kover (gate ≥ 80%, target ≥ 90%) |
| Coverage gate | `pre-push` git hook + Claude `PreToolUse` guard |
| Skills | `/write-spec`, `/run-pipeline`, `/validate-transactions`, `/write-docs` |
| MCP | context7 + custom FastMCP `pipeline-status` (`mcp/server.py`, Python) |

See `TASKS_ROADMAP.md` for the phased implementation plan and `HOWTORUN.md` for run instructions.

## Stack notes / deviations

- **Pipeline:** Kotlin 2.3.20 / JVM 21 / Gradle Kotlin DSL (per `.agents/docs/STACK.MD`).
- **Two packages** under `homework6`: `homework6.common` (shared models + utils) and `homework6.agent` (the runtime agents + their model) — a focused split rather than STACK.MD's full `entrypoint/service/...` layout, appropriate for a small CLI pipeline per `AGENTS.MD` ("prefer simpler architecture").
- **MCP server** (`mcp/server.py`) deviates to **Python + FastMCP**, as mandated by `TASKS.md` Task 4 (allowed under STACK.MD's Deviation Policy).
