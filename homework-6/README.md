# Homework 6: Final Capstone — AI-Powered Multi-Agent Banking Pipeline

- **Student Name**: Andrii Gnatiuk
- **Date Submitted**:
- **AI Tools Used**: Claude (Anthropic) via Cowork

---

> 🚧 **Placeholder (Phase 0 scaffolding).** Full content is produced by Agent 4 (Documentation) in Phase 5.

A multi-agent transaction-processing pipeline built by four meta-agents (Specification, Code-generation, Tests, Documentation). The runtime system cooperates through file-based JSON messages in `shared/` and consists of a Transaction Validator, a Fraud Detector, and a Reporting Agent, orchestrated by an integrator.

See `TASKS_ROADMAP.md` for the phased implementation plan and `HOWTORUN.md` for run instructions.

## Stack notes / deviations

- **Pipeline:** Kotlin 2.3.20 / JVM 21 / Gradle Kotlin DSL (per `.agents/docs/STACK.MD`).
- **Single flat package** `homework6` (no `entrypoint/service/...` split) — a deliberate simplification for a small CLI pipeline, per `AGENTS.MD` ("prefer simpler architecture").
- **MCP server** (`mcp/server.py`) deviates to **Python + FastMCP**, as mandated by `TASKS.md` Task 4 (allowed under STACK.MD's Deviation Policy).
