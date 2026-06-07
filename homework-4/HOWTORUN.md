# HOWTORUN — Homework-4

## Build

```bash
./gradlew :homework-4:build
```

## Run the application

```bash
./gradlew :homework-4:run
```

Serves on `http://localhost:8080`. Endpoints:

- `POST /snippets` — create (header `X-Api-Token: s3cr3t-admin-token`)
- `GET /snippets/{id}` — retrieve by id
- `GET /snippets?q=` — search by title
- `GET /swagger` — API docs, `GET /openapi.yaml` — raw spec

## Run tests

```bash
./gradlew :homework-4:test --rerun-tasks --console=plain
```

## Run the agent pipeline

A single command runs the six-agent chain per bug, in order, loading each agent's skills
automatically:

```
bug-researcher → research-verifier → bug-planner → bug-fixer → security-verifier → unit-test-generator
```

The Bug Planner only proceeds when the Research Verifier verdict is **PASS**.

### Commands

```bash
# All bugs, all at once (default)
./run-pipeline.sh

# A single bug (one-by-one) — id is a folder under context/bugs/
./run-pipeline.sh --bug 002-functional-search-misses-matches

# All bugs, pausing for confirmation between each
./run-pipeline.sh --interactive

# List discoverable bug ids
./run-pipeline.sh --list

# Usage
./run-pipeline.sh --help
```

### Tool policy (least privilege)

Each agent is granted only the tools its role needs, declared in its `tools:` frontmatter and
enforced via `--allowedTools`:

| Agent | Tools |
|-------|-------|
| `bug-researcher` | Read, Grep, Glob, Write |
| `research-verifier` | Read, Grep, Glob, Write |
| `bug-planner` | Read, Grep, Glob, Write |
| `bug-fixer` | Read, Grep, Glob, Edit, Write, Bash |
| `security-verifier` | Read, Grep, Glob, Write |
| `unit-test-generator` | Read, Grep, Glob, Write, Bash |

Only `bug-fixer` can `Edit` source; only `bug-fixer`/`unit-test-generator` get `Bash`. The
reviewers get `Write` solely to emit their report. A missing `tools:` field falls back to
read-only (`Read,Grep,Glob`). The granted set is logged per step as `tools=[...]`.

### Model invocation

Each agent is run by the adapter `agents/run-agent.sh`, which composes the prompt
(agent spec + loaded skills + input artifacts) and calls the `claude` CLI with the model and
tool set declared in the agent's frontmatter. Overrides:

- `CLAUDE_CMD` — the model CLI to call (default `claude`).
- `AGENT_RUNNER` — swap the whole adapter for your own runner.

### Logs

Every run is logged to stdout and persisted to `docs/logs/pipeline-<timestamp>.log`, with the
format `[<ts>] [<bug-id>] [<agent>:<model>] <PHASE> — <message>`, per-bug banners, and an
end-of-run summary. Composed prompts for each step are saved under `docs/logs/<timestamp>/`.
