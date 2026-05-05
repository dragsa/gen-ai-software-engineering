# ▶️ How to Run — Homework 2: Customer Support Ticket System

## Prerequisites

- JDK 21+
- No other local dependencies required (Gradle wrapper is included)

## Run the application

From the repository root:

```bash
./gradlew :homework-2:run
```

The server starts on **http://localhost:8080**.

| Endpoint | Description |
|---|---|
| `GET /swagger` | Swagger UI |
| `GET /openapi.yaml` | Raw OpenAPI spec |

## Build

```bash
./gradlew :homework-2:build
```

## Run tests

```bash
./gradlew :homework-2:test --rerun-tasks --console=plain
```

## Test coverage report (JaCoCo)

```bash
./gradlew :homework-2:test jacocoTestReport
```

Report is generated at: `homework-2/build/reports/jacoco/test/html/index.html`

## Verify the module is recognized by Gradle

```bash
./gradlew projects
```

`homework-2` must appear in the project list.
