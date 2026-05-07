Good. Here is the plan for Step 6:

---

## Step 6 — Integration Tests, Performance Tests & Demo

**Goal**: complete the TASKS.md deliverables — integration + performance tests, demo data, HTTP runner file, demo script, and a final HOWTORUN update.

---

### Phase A — Integration & Performance tests

**File: `src/test/kotlin/homework2/integration/IntegrationTest.kt`**

Five integration scenarios using `testApplication` (same harness as existing tests):

1. **Full lifecycle** — POST → GET by id → PUT (status change) → DELETE → GET returns 404
2. **Bulk import → auto-classify → filter** — import three tickets via CSV, run `auto-classify` on one, then `GET /tickets?category=<classified>` and assert it appears
3. **Combined filter** — create tickets with different category and priority combinations, assert `?category=billing_question&priority=high` returns exactly the right subset
4. **Malformed import pathway** — import a JSON array where one element is invalid, assert `successful=2 failed=1` and the failure row is reported
5. **Concurrent writes** — launch 20 coroutines each doing `POST /tickets`, join all, then `GET /tickets` and assert count = 20 (no lost writes)

**File: `src/test/kotlin/homework2/performance/PerformanceTest.kt`**

Five benchmark tests using wall-clock timing with generous budgets (CI-safe):

1. **CSV import throughput** — import 200-row CSV in under 3 s
2. **JSON import throughput** — import 200-element JSON array in under 3 s
3. **Classifier latency** — run `classifyTicket` 100 times, assert p95 < 50 ms
4. **List at 1k tickets** — seed 1 000 tickets, `GET /tickets` in under 500 ms
5. **Combined-filter latency** — seed 1 000 tickets, `GET /tickets?category=billing_question&priority=high` in under 200 ms

---

### Phase B — Demo data

**`demo/sample_tickets.csv`** — 50 rows, varied category / priority / status, realistic subjects and descriptions

**`demo/sample_tickets.json`** — 20 objects, JSON array

**`demo/sample_tickets.xml`** — 30 `<ticket>` elements under `<tickets>`

**`demo/invalid/`** — three negative samples:
- `invalid_email.csv` — one row with a malformed email
- `malformed.json` — truncated JSON
- `wrong_root.xml` — XML with an unrecognised root element

**`demo/sample-requests.http`** — IntelliJ HTTP runner file covering every endpoint (create, list, get, update, delete, auto-classify, import CSV/JSON/XML)

**`demo/demo.sh`** — shell script that starts the server in the background, sleeps until port 8080 is ready, runs a representative set of `curl` calls against every endpoint, prints results, and exits cleanly

---

### Phase C — HOWTORUN update

Add a **Demo** section covering `demo/demo.sh`, `demo/sample-requests.http`, and how to open the Swagger UI after starting the server.

---

### Files to create / modify

| File | Action |
|---|---|
| `src/test/kotlin/homework2/integration/IntegrationTest.kt` | new |
| `src/test/kotlin/homework2/performance/PerformanceTest.kt` | new |
| `demo/sample_tickets.csv` | new (50 rows) |
| `demo/sample_tickets.json` | new (20 objects) |
| `demo/sample_tickets.xml` | new (30 elements) |
| `demo/invalid/invalid_email.csv` | new |
| `demo/invalid/malformed.json` | new |
| `demo/invalid/wrong_root.xml` | new |
| `demo/sample-requests.http` | new |
| `demo/demo.sh` | new |
| `HOWTORUN.md` | update — add Demo section |

---

### Risks / edge cases

- Performance tests use wall-clock time and will be sensitive to the JVM warm-up in the test JVM. All budgets are set conservatively (3 s import, 500 ms list) and should pass even on CI without JVM pre-warming.
- The concurrency test inside `testApplication` shares one in-memory `ConcurrentHashMap` per test run, which is correct — `InMemoryTicketRepository` already uses `ConcurrentHashMap`.
- `demo.sh` must gracefully handle the case where the server takes a moment to start; it will poll port 8080 with a retry loop before firing curl calls.
- The JaCoCo gate should not be affected by the new tests since `integration/` and `performance/` packages contain no new production code.

---

Proceed?