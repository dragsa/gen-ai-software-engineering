# Task 3 — Jira MCP (command & tool audit log)

Date: 2026-06-14
Platform chosen: **Jira** (official Atlassian Remote MCP).
Tool/agent: Claude (Cowork) prepared config; user performs OAuth + query step.

## Configuration (done)

- Server: `jira` (official Atlassian Remote MCP)
- Transport: remote `http` (Streamable HTTP) → `https://mcp.atlassian.com/v1/mcp`
  - NOTE: original SSE endpoint `…/v1/sse` deprecates after 2026-06-30; switched to streamable HTTP.
  - Capture screenshots were taken while still on the SSE endpoint (jira connected, 31 tools).
- Auth: OAuth (in-client via `/mcp`) — no token committed
- Registered in project-local `homework-5/.mcp.json` (replaced the earlier `notion` placeholder)

## Steps to run (user)

```text
/mcp -> jira -> authenticate (Atlassian OAuth) -> verify connected
prompt: "Give me the tickets of the last 5 bugs on a project."
# server JQL ~ project = <KEY> AND issuetype = Bug ORDER BY created DESC (limit 5)
```

## Interaction performed

- [x] OAuth completed; `jira` connected (31 tools)
- [x] Project used (key): SCRUM ("My Scrum Space"), site andrii-gnatyuk.atlassian.net
- [~] Last 5 bug tickets returned — query ran OK but project has **0 Bug-type** issues
- [x] Request returned valid (empty-of-bugs) results; only 5 Stories exist (SCRUM-1…SCRUM-5)

## Result / output

Prompt: "using jira mcp Give me the tickets/pages of the last 5 bugs on a project".
Client: Claude Code CLI → `jira` MCP (multiple tool calls: list sites/projects, JQL search).
Site: andrii-gnatyuk.atlassian.net — one project: **SCRUM** ("My Scrum Space").
JQL `project = SCRUM AND issuetype = Bug ORDER BY created DESC` → **0 results** (no bugs yet).
Project contains 5 issues, all type **Story**, status **To Do**: SCRUM-1 … SCRUM-5.

=> The required request was executed correctly and returned valid results; the project simply
has no Bug-type issues. Follow-up options: create sample Bug issues in SCRUM and re-query, or
record "no bugs exist yet" as the result.

## Evidence

- Screenshot: `docs/screenshots/03-prompt-claude-cli-mcp-00.png` — `jira` connected (mcp list)
- Screenshot: `docs/screenshots/03-prompt-claude-cli-mcp-jira-dir-structure.png` — query result (SCRUM issues)

## Resolution (decision)

Recorded as: **no Bug-type issues in SCRUM, but 5 Stories present** (SCRUM-1…SCRUM-5).
The required "last 5 bugs" request was executed correctly and returned valid results (empty for
bugs). Evidence screenshots retained; no sample bugs created. Task 3 gate satisfied.

## How this server was added (commands & actions)

Method: **declarative** — edited project-scoped `homework-5/.mcp.json` directly (no `claude mcp add`).

Equivalent imperative command (for reference):

```bash
claude mcp add --scope project --transport http jira https://mcp.atlassian.com/v1/mcp
```

Client actions actually performed:
```text
claude                                  # reload so it reads the jira block
/mcp  -> jira -> Authenticate           # Atlassian OAuth in browser -> connected (31 tools)
# interaction prompt:
"using jira mcp Give me the tickets/pages of the last 5 bugs on a project"
```
