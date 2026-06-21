# How to Run — Homework 6

> 🚧 **Placeholder (Phase 0 scaffolding).** Steps are completed as each phase lands; finalized in Phase 5.

## Prerequisites

- JDK 21 (Gradle toolchain resolves it)
- Python 3.10+ (for the `mcp/` FastMCP server — Phase 4)

## Build

```bash
./gradlew :homework-6:build
```

## Run the pipeline (in-process default — Phase 2)

```bash
./gradlew :homework-6:run
```

## Run a single agent as a separate process (Phase 2)

```bash
./gradlew :homework-6:runValidator -Pargs="--dry-run"
./gradlew :homework-6:runFraudDetector
./gradlew :homework-6:runReporting
```

## Tests & coverage (Phase 5)

```bash
./gradlew :homework-6:test
./gradlew :homework-6:koverVerify   # fails if line coverage < 80%
```

## Coverage gate hook (Phase 3)

```bash
git config core.hooksPath homework-6/.githooks   # enables the pre-push 80% gate
```

## MCP servers (Phase 4)

Configured in `mcp.json` (context7 + custom `pipeline-status`). See that file and `research-notes.md`.
