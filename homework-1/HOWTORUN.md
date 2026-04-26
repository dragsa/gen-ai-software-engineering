# ▶️ How to Run the application

## Prerequisites

- JDK 21+ installed
- `curl` available at `/usr/bin/curl`
- `python3` installed (used by demo scripts for reading `demo/sample-data.json`)

## Run the server

From the repository root:

```bash
./gradlew :homework-1:run
```

Server starts on `http://localhost:8080`.

## Verify "Hello World"

```bash
curl http://localhost:8080/
```

Expected response:

```text
Hello World
```

## Run end-to-end API demo (Tasks 1-3)

From the repository root:

```bash
zsh homework-1/demo/demo.sh
```

This script:

- starts the app
- waits until the server is ready
- executes all API endpoint calls using `curl`
- uses payloads and filters from `homework-1/demo/sample-data.json`
- logs every request and response to stdout
- fails immediately on any non-expected HTTP status

The script also writes logs to:

- `homework-1/demo/demo.log` — full demo flow output (including request/response logs)
- `homework-1/demo/app.log` — application startup/runtime output

## Demo artifacts

- `homework-1/demo/sample-requests.http` — executable request flow for all endpoints
- `homework-1/demo/sample-data.json` — reusable payload and filter data
- `homework-1/demo/demo.sh` — full end-to-end automated demo script
