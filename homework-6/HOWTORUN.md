# How to Run — Homework 6

> Note: the test suite and the custom MCP server are still being finalized; the commands below reflect the intended workflow and the parts already in place.

## Prerequisites

- JDK 21 (the Gradle toolchain resolves it automatically)
- Python 3.10+ (only for the custom FastMCP server under `mcp/`)

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
./gradlew :homework-6:runReporting                      # summarizes shared/results/
```

## Slash-command skills (Claude Code)

```text
/run-pipeline            # clears shared/, runs the pipeline, summarizes results, reports rejects
/validate-transactions   # runs the validator in dry-run and prints a valid/invalid table
```

## Tests & coverage

```bash
./gradlew :homework-6:test
./gradlew :homework-6:koverVerify -PenforceCoverage   # fails if line coverage < 80%
```

Without `-PenforceCoverage`, `koverVerify` is skipped, so a plain `build` / `run` stays green while the test suite is still being written.

## Coverage gate hook (pre-push)

Enable the gate once per clone:

```bash
git config core.hooksPath homework-6/.githooks
```

On every `git push`, `.githooks/pre-push` runs `koverVerify -PenforceCoverage` and **blocks the push if line coverage < 80%**. Bypass deliberately with `git push --no-verify`.

The same gate is mirrored for Claude sessions: `.claude/settings.json` registers a `PreToolUse` guard (`.githooks/claude-pre-push-guard.sh`) that blocks a `git push` issued from inside Claude Code when coverage is below 80%.

## MCP servers

Both servers are configured in `mcp.json`: `context7` (framework docs lookup) and the custom `pipeline-status` server (`mcp/server.py`) exposing `get_transaction_status`, `list_pipeline_results`, and the `pipeline://summary` resource. See `research-notes.md` for the context7 queries used during development.
