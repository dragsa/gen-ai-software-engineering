# HOWTORUN — Homework 5

> **Status:** Phase 0 placeholder. Sections are filled in as each phase is executed.

## 1. Prerequisites

- Node.js (for `npx`-based servers: Filesystem)
- JDK 21 + Gradle wrapper (for the custom Kotlin server)
- An MCP client (Claude Code) that reads project-scoped `.mcp.json`

## 2. Configuration (project-local)

All servers are registered in `homework-5/.mcp.json` (project scope — **not** global/user
config). Secrets are supplied via environment variables referenced in that file:

| Variable | Used by | Notes |
|----------|---------|-------|
| `GITHUB_PERSONAL_ACCESS_TOKEN` | GitHub MCP | Phase 1 — fill before use |
| `FILESYSTEM_MCP_PATH` | Filesystem MCP | Phase 2 — absolute path to a local project dir |
| `NOTION_MCP_TOKEN` | Notion MCP | Phase 3 — or swap block for Jira |

Set them in your shell (do **not** commit real values), e.g.:

```bash
export GITHUB_PERSONAL_ACCESS_TOKEN=***
export FILESYSTEM_MCP_PATH="$(pwd)"   # run from homework-5/ or repo root
export NOTION_MCP_TOKEN=***
```

## 3. Connect / reload

Open the project in your MCP client and reload so it picks up `homework-5/.mcp.json`, then
confirm each server registers without errors.

## 4. Custom server (Phase 4)

```bash
# build
./gradlew :homework-5:build
# run (stdio MCP server; launched by the client via .mcp.json)
./gradlew :homework-5:run -q --console=plain
```

Test the `read` tool (default `word_count` = 30, and a custom value) — see Phase 4 of
`TASKS_ROADMAP.MD`.

## 5. Evidence

- Result screenshots → `docs/screenshots/`
- Command/tool audit logs → `docs/logs/`
