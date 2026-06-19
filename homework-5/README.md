# Homework 5: Configure MCP Servers (GitHub, Filesystem, Jira, Custom)

- **Student Name**: Andrii Gnatiuk
- **Date Submitted**: 19.06.2026
- **AI Tools Used**: Claude (Anthropic) via Cowork, test via Claude CLI

---

Configure three external MCP servers and build one custom MCP server, all registered in a
**project-local** `.mcp.json`, demonstrated against Claude Code with screenshot evidence per task.
See `HOWTORUN.md` for setup/run instructions and `TASKS_ROADMAP.MD` for the phased plan and
per-task checklists. Audit logs for how each server was added are in `docs/logs/`.

## 1. What was done

- Registered all servers in a **project-scoped** `homework-5/.mcp.json` (committed to the repo),
  not the global/user config. Secrets are referenced via environment variables (documented in
  `.env.example`) and never committed.
- **GitHub MCP** — connected to the hosted GitHub MCP and listed recent pull requests of this
  repository.
- **Filesystem MCP** — scoped to a local project directory (`homework-5/`) and produced a
  recursive directory listing.
- **Jira MCP** — connected to the official Atlassian Remote MCP (OAuth) and ran the required
  "last 5 bugs" query against a real project.
- **Custom MCP (FastMCP)** — built `custom-mcp-server/server.py` exposing a `lorem-ipsum`
  **resource** and a `read` **tool**, then called it from the client.
- Captured **screenshots** of each interaction in `docs/screenshots/` and recorded the exact
  commands/tools used to add each server in `docs/logs/`.

## 2. MCP servers added

All four are registered in `homework-5/.mcp.json`:

| Server | Type / transport | Endpoint or command | Auth | Demonstrated interaction |
|--------|------------------|---------------------|------|--------------------------|
| `github` | hosted, HTTP | `https://api.githubcopilot.com/mcp/` | PAT (`GITHUB_PERSONAL_ACCESS_TOKEN`) | Listed recent closed PRs of `dragsa/gen-ai-software-engineering` |
| `filesystem` | local, stdio | `npx -y @modelcontextprotocol/server-filesystem ${FILESYSTEM_MCP_PATH}` | local path scope | Recursive listing of `homework-5/` as a table |
| `jira` | hosted, HTTP (Streamable) | `https://mcp.atlassian.com/v1/mcp` | OAuth (in-client) | "Last 5 bugs" query on project `SCRUM` |
| `custom-lorem` | local, stdio | `custom-mcp-server/.venv/bin/python3 custom-mcp-server/server.py` | none (local) | `read` tool → first N words of `lorem-ipsum.md` |

Notes:

- GitHub uses a PAT in the `Authorization` header; the hosted endpoint does not support the
  client's OAuth (no dynamic client registration), so a valid PAT is required.
- Jira uses the **Streamable HTTP** endpoint (`/v1/mcp`); the older SSE endpoint (`/v1/sse`)
  deprecates after 2026-06-30.
- The `SCRUM` project had no Bug-type issues (only Stories `SCRUM-1`…`SCRUM-5`), so the "last 5
  bugs" query correctly returned no bugs — recorded as the result.

## 3. Resources vs Tools

- **Resources** are URIs Claude can **read from** — passive data sources like files or API
  endpoints (e.g., `lorem://words`). The client reads a resource; it does not cause side effects.
- **Tools** are actions Claude can **call to perform operations** — reading a file, querying an
  API, creating an issue. A tool is invoked with arguments and returns a result.

In this project the custom server exposes both over the same content: a **resource**
(`lorem://words`) the client can read, and a **tool** (`read`) the client can call with a
`word_count` argument.

## 4. Custom MCP server (FastMCP) and how it was used

Location: `custom-mcp-server/` — `server.py`, `lorem-ipsum.md`, `requirements.txt` (includes
`fastmcp`).

It exposes:

- **Resource** `lorem://words` — returns the first **30** words (default) of `lorem-ipsum.md`.
- **Resource template** `lorem://words/{word_count}` — returns the first `word_count` words.
- **Tool** `read(word_count=30)` (`mcp__custom-lorem__read`) — returns the first `word_count`
  words; `word_count` is optional and defaults to 30.

`server.py` resolves `lorem-ipsum.md` relative to its own location, so it works regardless of the
client's working directory. The server runs under a Python **3.10+** virtual environment
(`.venv`) because FastMCP 2.x requires ≥ 3.10; `.mcp.json` launches it via the venv interpreter
directly, so no global Python change or venv activation is needed.

How it was used (verified end-to-end in Claude Code):

- `read` with no arguments → returned **30** words
  ("Lorem ipsum dolor sit amet, … ullamco laboris nisi").
- `read` with `word_count = 5` → returned exactly **5** words ("Lorem ipsum dolor sit amet,").
- `listMcpResources` showed the `lorem://words` resource alongside the server's tool.

See `docs/screenshots/04-*` for evidence and `docs/logs/task-4-custom.md` for commands.

## 5. Final folder structure

```
homework-5/
├── README.md                    (this file — description + author)
├── HOWTORUN.md                  (install / run / connect / usage per server)
├── TASKS.md                     (assignment, read-only)
├── TASKS_ROADMAP.MD             (phased plan + per-task checklists)
├── .mcp.json                    (project-local config: github, filesystem, jira, custom-lorem)
├── .env.example                 (env vars: GITHUB_PERSONAL_ACCESS_TOKEN, FILESYSTEM_MCP_PATH)
├── .gitignore                   (secrets + Python artifacts)
├── custom-mcp-server/
│   ├── server.py                (FastMCP server: lorem resource + read tool)
│   ├── lorem-ipsum.md           (69-word source text)
│   ├── requirements.txt         (fastmcp>=2.0)
│   └── .venv/                   (Python 3.10+ venv — gitignored)
└── docs/
    ├── logs/                    (per-task command/tool audit logs)
    │   ├── task-0-scaffolding.md
    │   ├── task-1-github.md
    │   ├── task-2-filesystem.md
    │   ├── task-3-jira.md
    │   └── task-4-custom.md
    └── screenshots/             (MCP result evidence)
        ├── 01-…-git-closed-PRs.png         (GitHub)
        ├── 02-…-filesystem-dir-structure.png (Filesystem)
        ├── 03-…-jira-dir-structure.png      (Jira)
        └── 04-…-custom-interaction.png      (Custom read tool)
```

## Stack deviation

`.agents/docs/STACK.MD` mandates Kotlin/Gradle, but `TASKS.md` explicitly mandates a Python
**FastMCP** custom server, so `custom-mcp-server/` uses **Python + FastMCP**. This is an allowed
deviation because a subproject's `TASKS.md` may mandate a different runtime; the deviation is
scoped to the custom server only and introduces no cross-subproject coupling.
