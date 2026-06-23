# How to Run — Homework 6

## Prerequisites

- JDK 21 (the Gradle toolchain resolves it automatically)
- Python **3.10+** (only for the custom FastMCP server under `mcp/`; FastMCP 2.x requires ≥ 3.10).
  macOS's built-in `python3` is often 3.9 — check `python3 --version`; if older, install a newer one
  (`brew install python@3.12`) and use `python3.12` explicitly in the venv step below.

All Gradle commands are run from the **repository root**.

## Build

```bash
./gradlew :homework-6:build
```

## Run the full pipeline

In-process orchestration (default demo): the integrator seeds `shared/`, runs the agents in order, and writes results.

```bash
./gradlew :homework-6:run
```

All 8 transactions land in `shared/results/` (one JSON each) plus `pipeline-summary.json` / `pipeline-summary.txt`.

## Run a single agent as a separate process

Each runtime agent also has its own entry point and communicates only through the `shared/` JSON directories.

```bash
./gradlew :homework-6:runValidator -Pargs="--dry-run"   # validate sample-transactions.json, no file moves
./gradlew :homework-6:runFraudDetector                  # drains shared/output/ -> shared/results/
./gradlew :homework-6:runReporting                       # summarizes shared/results/
```

## Slash-command skills (Claude Code)

```text
/run-pipeline            # clears shared/, runs the pipeline, summarizes results, reports rejects
/validate-transactions   # runs the validator in dry-run and prints a valid/invalid table
/write-spec              # (re)generates specification.md from the project template
/write-docs              # regenerates README.md / HOWTORUN.md from the current code, spec, and pipeline output
```

## Tests & coverage

```bash
./gradlew :homework-6:test
./gradlew :homework-6:koverVerify -PenforceCoverage   # fails if line coverage < 80%
```

Without `-PenforceCoverage`, `koverVerify` is skipped, so a plain `build` / `run` stays green.

## Coverage gate hook (pre-push)

Enable the gate once per clone:

```bash
git config core.hooksPath homework-6/.githooks
```

On every `git push`, `.githooks/pre-push` runs `koverVerify -PenforceCoverage` and **blocks the push if line coverage < 80%**. Bypass deliberately with `git push --no-verify`.

The same gate is mirrored for Claude sessions: `.claude/settings.json` registers a `PreToolUse` guard (`.githooks/claude-pre-push-guard.sh`) that blocks a `git push` issued from inside Claude Code when coverage is below 80%.

## MCP servers

Both servers are configured in `.mcp.json`:
- **`context7`** — framework docs lookup (`npx -y @upstash/context7-mcp@latest`).
- **`pipeline-status`** — the custom FastMCP server (`mcp/server.py`) exposing the tools
  `get_transaction_status` and `list_pipeline_results`, and the `pipeline://summary` resource.

Set up the custom server's virtualenv once:

```bash
cd homework-6
python3.12 -m venv --clear mcp/.venv              # --clear rebuilds; python3 only if it's >= 3.10
mcp/.venv/bin/python --version                    # verify >= 3.10, not 3.9.x — check this FIRST
mcp/.venv/bin/python -m pip install --upgrade pip
mcp/.venv/bin/pip install -r mcp/requirements.txt
```

One-liner (from `homework-6/`). The `--clear` flag rebuilds the venv from scratch so a stale 3.9
venv can't linger:

```bash
python3.12 -m venv --clear mcp/.venv && mcp/.venv/bin/python -m pip install -qU pip && mcp/.venv/bin/pip install -r mcp/requirements.txt
```

If the install still reports `No matching distribution found for fastmcp` (or "Ignored ... Requires-Python
>=3.10"), the venv's interpreter is 3.9. Confirm with `mcp/.venv/bin/python --version`; it must be
≥ 3.10. A plain `python3.12 -m venv` over an existing `mcp/.venv` **reuses the old interpreter** — always
use `--clear` (or `rm -rf mcp/.venv` first). `.mcp.json` launches the server via `mcp/.venv/bin/python3`,
so it always runs under this venv regardless of your global `python3`.

Run the pipeline first (`./gradlew :homework-6:run`) so `shared/results/` is populated, then the
server can answer queries.

> **Note:** `.mcp.json` is the project MCP config Claude Code loads automatically — it is the combined
> `mcp.json` Task 4 asks for (Claude Code uses the dotfile name, as in homework-5). See
> `research-notes.md` for the context7 queries.
