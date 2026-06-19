# Task 4 — Custom MCP Server (FastMCP) — command & tool audit log

Date: 2026-06-18
Tool/agent: Claude (Cowork) implemented + tested the server; user runs the client end-to-end + screenshot.

## What was built

- `custom-mcp-server/server.py` — FastMCP server `custom-lorem`
  - Resource `lorem://words` → first 30 words of `lorem-ipsum.md`
  - Resource `lorem://words/{word_count}` → first N words
  - Tool `read(word_count=30)` → first `word_count` words
  - `lorem-ipsum.md` resolved relative to `__file__` (cwd-independent)
- `custom-mcp-server/lorem-ipsum.md` — 69-word source (> 30)
- `custom-mcp-server/requirements.txt` — `fastmcp>=2.0`
- `.mcp.json` `custom-lorem` → `custom-mcp-server/.venv/bin/python3 custom-mcp-server/server.py` (venv interpreter; no PATH/activation needed)

## Commands used (build/install/run)

```bash
# deps
cd custom-mcp-server
python3 -m venv .venv && source .venv/bin/activate     # optional
pip install -r requirements.txt                         # fastmcp

# run standalone (stdio) or via inspector
python3 custom-mcp-server/server.py
fastmcp dev custom-mcp-server/server.py                 # MCP Inspector

# client: reload -> /mcp -> custom-lorem connected -> call `read`
```

## Verification (in sandbox, fastmcp stubbed — PyPI blocked here)

Logic checked by importing server.py with a stubbed `fastmcp` and calling the underlying funcs:

```text
total words in lorem-ipsum.md: 69 (>30: True)
get_words() default -> 30 words
get_words(5)        ->  5 words
get_words(30)       -> 30 words
get_words(1000)     -> 69 words (clamped to all)
read() default      -> 30 ; read(7) -> 7 ; read(0) -> 0
ASSERTIONS PASSED
```

## Interaction performed (user — client end-to-end)

- [x] `custom-lorem` connected in `/mcp` (resource `lorem://words` listed; tool `read` = `mcp__custom-lorem__read`)
- [x] `read` (default) returns 30 words — "Lorem ipsum … ullamco laboris nisi"
- [x] `read` with custom `word_count` (5) returns exactly 5 — "Lorem ipsum dolor sit amet,"
- [x] Result captured (client end-to-end)

## Evidence

- Screenshot: `docs/screenshots/04-prompt-claude-cli-mcp-00.png` — listMcpResources (custom-lorem resource present)
- Screenshot: `docs/screenshots/04-prompt-claude-cli-mcp-custom-tools.png` — `read` tool details (`mcp__custom-lorem__read`)
- Screenshot: `docs/screenshots/04-prompt-claude-cli-mcp-custom-interaction.png` — `read` default=30 words and read 5 words

## Resolution

custom-lorem connected and verified end-to-end in Claude Code: `read` returns 30 words by
default and exactly N for a custom `word_count` (tested 5). Task 4 gate satisfied.
