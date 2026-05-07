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
./gradlew :homework-2:test --rerun-tasks --console=plain --info
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

---

## Demo

### Automated demo script

The demo script builds the project, starts the server, exercises every endpoint with `curl`, and writes a full transcript to `demo/demo.log`.

```bash
cd homework-2
chmod +x demo/demo.sh
./demo/demo.sh
```

| File | Contents |
|---|---|
| `demo/demo.log` | All curl requests and responses |
| `demo/app.log` | Server stdout / stderr |

### IntelliJ HTTP runner

Open `demo/sample-requests.http` in IntelliJ IDEA. Start the server first (`./gradlew :homework-2:run`), then run individual requests or the whole file from the IDE.

### Sample data files

| File | Format | Rows | Notes |
|---|---|---|---|
| `demo/sample_tickets.csv` | CSV | 50 | Varied category, priority, and status |
| `demo/sample_tickets.json` | JSON | 20 | Varied scenarios including urgent/security |
| `demo/sample_tickets.xml` | XML | 30 | Full `<tickets>` root with nested `<tags>` |
| `demo/invalid/invalid_email.csv` | CSV | 3 | 2 rows with bad emails, 1 valid |
| `demo/invalid/malformed.json` | JSON | — | Truncated JSON — triggers parse error |
| `demo/invalid/wrong_root.xml` | XML | — | Unrecognised root element — triggers 400 |

### Swagger UI

With the server running, open [http://localhost:8080/swagger](http://localhost:8080/swagger) to explore and try all endpoints interactively.
