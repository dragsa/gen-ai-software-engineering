# HOWTORUN — Homework 5

> **Status:** Phase 0 placeholder. Sections are filled in as each phase is executed.

## 1. Prerequisites

- Node.js (for `npx`-based servers: Filesystem)
- Python **3.10+** and `pip` (FastMCP 2.x requires ≥ 3.10). Note: macOS's built-in `python3` is
  often 3.9 — check with `python3 --version`; if older, install a newer one (e.g.
  `brew install python@3.12`) and use `python3.12` explicitly below.
- An MCP client (Claude Code) that reads project-scoped `.mcp.json`

## 2. Configuration (project-local)

All servers are registered in `homework-5/.mcp.json` (project scope — **not** global/user
config). Secrets are supplied via environment variables referenced in that file:

| Variable | Used by | Notes |
|----------|---------|-------|
| `GITHUB_PERSONAL_ACCESS_TOKEN` | GitHub MCP | Phase 1 — fill before use |
| `FILESYSTEM_MCP_PATH` | Filesystem MCP | Phase 2 — absolute path to a local project dir |
| _(none)_ | Jira MCP | Phase 3 — official Atlassian Remote MCP uses OAuth, no env var |

Set them in your shell (do **not** commit real values), e.g.:

```bash
export GITHUB_PERSONAL_ACCESS_TOKEN=***
export FILESYSTEM_MCP_PATH="$(pwd)"   # run from homework-5/ or repo root
export NOTION_MCP_TOKEN=***
```

## 3. Connect / reload

Open the project in your MCP client and reload so it picks up `homework-5/.mcp.json`, then
confirm each server registers without errors.

### Enabling project-scoped servers (Claude Code)

Servers in a project `.mcp.json` are **not active until approved**, and `${VARS}` are expanded
from the environment Claude Code was launched in. If `/doctor` detects the file but the servers
aren't listed under "Manage MCP servers":

1. **Export env vars first, then launch from the same shell** (Claude Code inherits them):
   ```bash
   export GITHUB_PERSONAL_ACCESS_TOKEN=***
   # (Phase 2/3) export FILESYSTEM_MCP_PATH=...  ;  export NOTION_MCP_TOKEN=...
   cd /Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering
   claude          # start (or fully restart) Claude Code here
   ```
2. **Approve the project servers.** On first load Claude Code prompts to trust the project's
   MCP servers — approve it. If you dismissed it earlier, reset and restart:
   ```bash
   claude mcp reset-project-choices
   ```
3. **Verify:** `claude mcp list` (or `/mcp` in the TUI) should show `github` connected.

> Note: `/doctor` only warns about missing `${VARS}` found in `args`/`env`, **not** in `headers`.
> So the GitHub block's token won't show as a warning — but if it's unset the server fails auth
> silently. Confirm `github` shows **connected** in `/mcp`, not just present in the file.

### Task 1 — GitHub MCP (runbook)

The `github` block uses the **official hosted GitHub MCP** (`https://api.githubcopilot.com/mcp/`,
remote `http` transport) authenticated with a GitHub PAT via `GITHUB_PERSONAL_ACCESS_TOKEN`.

1. **Create a token** — GitHub → Settings → Developer settings → Personal access tokens.
   Scopes: `repo` (read), plus `pull_requests` / `issues` for the interaction you pick.
2. **Export it** (do not commit):
   ```bash
   cp .env.example .env   # then edit .env, or just export in your shell
   export GITHUB_PERSONAL_ACCESS_TOKEN=***
   ```
3. **Reload the client** so it reads `homework-5/.mcp.json`. In Claude Code: `/mcp` should list
   `github` as connected. Verify it registers with **no errors**.
4. **Run one interaction** (pick one — alternatives accepted):
   - *List recent pull requests* of your repo, or
   - *Summarize the latest commits* on a branch, or
   - *Create an issue* and confirm it appears on GitHub.
5. **Capture the screenshot** of the prompt + result →
   `docs/screenshots/01-reply-github-mcp-result.png`.
6. **Log it** — append the commands/tools you used and the outcome to
   `docs/logs/task-1-github.md`.

> Alternative (local) approach: run the official server locally via Docker
> `ghcr.io/github/github-mcp-server` with `GITHUB_PERSONAL_ACCESS_TOKEN`, and point a `command`
> block at it instead of the hosted `http` block. Either satisfies Task 1.

### Task 2 — Filesystem MCP (runbook)

The `filesystem` block runs the official `@modelcontextprotocol/server-filesystem` via `npx`
(stdio), scoped to **a local project directory** passed as `${FILESYSTEM_MCP_PATH}`. Requires
Node.js (`npx`).

1. **Point it at a local dir** (the repo or `homework-5/` — keep it project-local):
   ```bash
   export FILESYSTEM_MCP_PATH="/Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-5"
   ```
   Export in the shell **before** launching Claude Code (it expands `${VARS}` at launch and the
   path appears in `args`, so `/doctor` validates it).
2. **Reload/approve** the project servers (see "Enabling project-scoped servers" above); confirm
   `filesystem` shows **connected** in `/mcp`.
3. **Run one interaction** (pick one — alternatives accepted):
   - *List files* in the configured directory, or
   - *Read a file* (e.g. `TASKS.md`) and show its contents, or
   - *Summarize the directory structure*.
4. **Capture the screenshot** of prompt + result →
   `docs/screenshots/02-…-filesystem-….png` (use your `NN-prompt/reply-…` naming).
5. **Log it** in `docs/logs/task-2-filesystem.md`.

> Keep the path inside the project so access is scoped locally — do not point it at `$HOME` or
> the filesystem root.

### Task 3 — Jira MCP (runbook)

The `jira` block uses the **official Atlassian Remote MCP** at
`https://mcp.atlassian.com/v1/mcp` (Streamable HTTP). Auth is **OAuth** — no API token in the
repo. (The older `…/v1/sse` SSE endpoint deprecates after **2026-06-30**; this config uses the
streamable-HTTP endpoint. After switching, reconnect via `/mcp` — re-auth may be required.)

1. **Reload** the client so it sees the `jira` block, then **authenticate**:
   in Claude Code run `/mcp`, select `jira`, and complete the Atlassian OAuth login in the
   browser (authorize your Cloud site). Confirm `jira` shows **connected**.
2. **Make the required request** against a real project:
   > "Give me the tickets of the last 5 bugs on a project."
   (Equivalent JQL the server may run: `project = <KEY> AND issuetype = Bug ORDER BY created DESC`
   limited to 5.)
3. **Redact before screenshotting** — show only ticket **keys/numbers** (e.g. `PROJ-123`), not
   summaries/descriptions or any sensitive content.
4. **Capture the screenshot** of prompt + redacted result →
   `docs/screenshots/03-…-jira-….png`.
5. **Log it** in `docs/logs/task-3-jira.md`.

> Alternative: community `mcp-atlassian` (Docker or `uvx`) with `JIRA_URL` + `JIRA_USERNAME` +
> `JIRA_API_TOKEN` env vars. Either satisfies Task 3; the hosted OAuth server avoids committing
> credentials.

### Task 4 — Custom MCP Server (FastMCP) (runbook)

The `custom-lorem` block runs `custom-mcp-server/server.py` (FastMCP) over `lorem-ipsum.md`.

- **Resources** are URIs Claude reads from: `lorem://words` (first 30 words) and
  `lorem://words/{word_count}` (first N words).
- **Tools** are actions Claude calls: `read` with an optional `word_count` (default `30`).

1. **Create the venv and install dependencies** — **required**, because `.mcp.json` launches the
   server via `custom-mcp-server/.venv/bin/python3`, so the venv must exist at that path with
   `fastmcp` installed. Build it with a **3.10+** interpreter (use `python3.12` if your default
   `python3` is older — see Prerequisites):
   ```bash
   cd custom-mcp-server
   python3.12 -m venv .venv                 # or: python3 -m venv .venv  (only if python3 is >=3.10)
   .venv/bin/python -m pip install --upgrade pip
   .venv/bin/pip install -r requirements.txt
   .venv/bin/python --version               # verify 3.10+ ; should NOT be 3.9.x
   ```
   (Calling `.venv/bin/...` directly means you don't need to `activate` the venv. If you prefer,
   `source .venv/bin/activate` first and drop the `.venv/bin/` prefixes.)
2. **Reload/connect** — the client launches it via `.mcp.json`
   (`custom-lorem` → `custom-mcp-server/.venv/bin/python3 custom-mcp-server/server.py`), which
   uses the **venv interpreter directly**, so no PATH/activation is needed before starting Claude
   Code. Confirm `custom-lorem` shows **connected** in `/mcp`. To run standalone:
   `.venv/bin/python3 server.py` (stdio) or `fastmcp dev server.py` (MCP Inspector).
3. **Call the `read` tool** — once with no args (returns 30 words) and once with `word_count`
   (e.g. 5) → exactly that many words.
4. **Capture the screenshot** of prompt + result →
   `docs/screenshots/04-…-custom-mcp-read-tool-….png`.
5. **Log it** in `docs/logs/task-4-custom.md`.

> The `.mcp.json` command points at `custom-mcp-server/.venv/bin/python3`, so the server always
> runs under the 3.12 venv where `fastmcp` is installed — no global `python3` change or venv
> activation required. (Paths are relative to the project dir where Claude Code is launched.)
> `server.py` resolves `lorem-ipsum.md` relative to itself, so the working directory doesn't
> affect file reads.

## 4. Evidence

- Result screenshots → `docs/screenshots/`
- Command/tool audit logs → `docs/logs/`
