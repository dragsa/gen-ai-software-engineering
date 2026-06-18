# Task 1 — GitHub MCP (command & tool audit log)

Date: 2026-06-14
Tool/agent: Claude (Cowork) prepared config; user performs token + interaction step.

## Configuration (done)

- Server: `github` (official hosted GitHub MCP)
- Transport: remote `http` → `https://api.githubcopilot.com/mcp/`
- Auth: `Authorization: Bearer ${GITHUB_PERSONAL_ACCESS_TOKEN}` (env var, not committed)
- Registered in project-local `homework-5/.mcp.json`
- `.env.example` documents the token variable

## Steps to run (user)

```bash
export GITHUB_PERSONAL_ACCESS_TOKEN=***        # PAT: repo + pull_requests/issues
# reload client; verify `github` connects (e.g. /mcp in Claude Code)
```

## Interaction performed

- [x] Chosen interaction: list recent closed PRs (Option A — read)
- [x] Target repo: dragsa/gen-ai-software-engineering
- [x] Result captured

## Result / output

Prompt: "List recent closed PRs of this repo".
Client: Claude Code CLI → called `github` MCP (ran 2 shell commands).
Response: 9 closed PRs in `dragsa/gen-ai-software-engineering` (#1–#9), all by `dragsa`;
PR #3 ("Sync main upstream") closed without merging; others merged 2026-05-03 … 2026-06-14.

## Evidence

- Screenshot: `docs/screenshots/01-prompt-claude-cli-mcp-00.png` — `github` connected in /mcp
- Screenshot: `docs/screenshots/01-prompt-claude-cli-mcp-git-closed-PRs.png` — PR list result

## Troubleshooting note (2026-06-14)

`/doctor` showed the project `.mcp.json` detected (warnings for filesystem/notion missing env),
but no project servers were active under "Manage MCP servers" (only user `context7` + built-in
`computer-use`). Root cause: project-scoped servers require **approval**, and `${VARS}` are
expanded from the launch environment.

Resolution steps:
- Token exported to the shell session (GITHUB_PERSONAL_ACCESS_TOKEN).
- (Re)launch Claude Code from that shell so it inherits the env and re-reads `.mcp.json`.
- Approve project MCP servers (or `claude mcp reset-project-choices` then restart).
- Verify with `claude mcp list` / `/mcp` → `github` should be **connected**.

Note: `/doctor` does not scan `headers` for `${VARS}`, so the GitHub token never appears as a
missing-env warning — connection status must be checked in `/mcp`.
