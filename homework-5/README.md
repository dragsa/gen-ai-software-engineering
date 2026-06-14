# Homework 5 — MCP Servers (GitHub, Filesystem, Jira/Notion, Custom)

> **Status:** Phase 0 scaffolding. This is a placeholder seeded during setup; the final
> description is completed in Phase 5 (and any substantive edit is gated on explicit approval
> per `AGENTS.MD`).

**Author:** dragsa (to.gnatuk@gmail.com)

## Overview

Configure three external MCP servers (GitHub, Filesystem, Jira **or** Notion) using a
**project-local** `.mcp.json`, and build one **custom MCP server in Kotlin** exposing a
`lorem-ipsum.md` resource and a `read` tool. See `TASKS_ROADMAP.MD` for the phased plan and
`HOWTORUN.md` for setup/run instructions.

## Stack override

`TASKS.md` specifies a Python **FastMCP** custom server. Per explicit instruction, the custom
server is implemented in **Kotlin** using the official **MCP Kotlin SDK**
(`io.modelcontextprotocol:kotlin-sdk`), aligning with the repo's mandated stack
(`.agents/docs/STACK.MD`). Rationale and details to be finalized in Phase 5.

## Deliverables (tracked in TASKS_ROADMAP.MD)

- Project-local `.mcp.json` with all four servers
- Kotlin custom MCP server (resource + `read` tool)
- `docs/screenshots/` — MCP result evidence (quality gate per task)
- `docs/logs/` — command/tool audit trail per task
