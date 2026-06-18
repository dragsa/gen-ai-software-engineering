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
