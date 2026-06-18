# Task 2 — Filesystem MCP (command & tool audit log)

Date: 2026-06-14
Tool/agent: Claude (Cowork) prepared config; user performs path export + interaction step.

## Configuration (done)

- Server: `filesystem` (official `@modelcontextprotocol/server-filesystem`)
- Transport: stdio via `npx -y @modelcontextprotocol/server-filesystem ${FILESYSTEM_MCP_PATH}`
- Scope: local project directory (env var, not committed)
- Registered in project-local `homework-5/.mcp.json`
- `.env.example` documents `FILESYSTEM_MCP_PATH`

## Steps to run (user)

```bash
export FILESYSTEM_MCP_PATH="/Users/dragsa/IdeaProjects/gnatiuk/gen-ai-software-engineering/homework-5"
# (re)launch Claude Code from this shell; approve project servers; verify `filesystem` in /mcp
```

## Interaction performed

- [x] Chosen interaction: list all files in homework-5 + summarize as table (Option A + C)
- [x] Target path: …/gen-ai-software-engineering/homework-5
- [x] Result captured

## Result / output

Prompt: "using mcp filesystem, list all files in homework-5 dir and give summary of entities in table form".
Client: Claude Code CLI → called `filesystem` MCP, tool `mcp__filesystem__directory_tree`.
Response: full recursive listing of `homework-5/` rendered as a Path/Type/Notes table
(.claude/, .env.example, .gitignore, .mcp.json, HOWTORUN.md, README.md, TASKS.md,
TASKS_ROADMAP.MD, custom-mcp-server/, docs/logs/*, docs/screenshots/*). `list_allowed_directories`
confirmed the server is scoped to the project path.

## Evidence

- Screenshot: `docs/screenshots/02-prompt-claude-cli-mcp-00.png` — `filesystem` listed/connected (listMcpResources)
- Screenshot: `docs/screenshots/02-prompt-claude-cli-mcp-filesystem-dir-structure.png` — dir listing table result
