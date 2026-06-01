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

> Added in Phase 4. Single command runs all four agents in order:
> Bug Researcher → Research Verifier → Bug Planner → Bug Fixer → Security Verifier → Unit Test Generator.

```bash
# ./run-pipeline.sh   (not yet available)
```
