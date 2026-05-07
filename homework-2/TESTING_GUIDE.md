# Testing Guide — Customer Support Ticket System

## Test pyramid

```mermaid
graph BT
    U["Unit tests<br/>(TicketModelTest · CategorizationTest · CsvImportTest · JsonImportTest · XmlImportTest · TicketServiceTest)<br/>~60 tests — fast, no network, no server"]
    I["Integration tests<br/>(TicketApiTest · ImportApiTest · DocumentationApiTest)<br/>~35 tests — Ktor testApplication, in-memory server"]
    E["End-to-end / performance<br/>(IntegrationTest · PerformanceTest)<br/>lifecycle flows, concurrency, latency budgets"]

    U --> I --> E

    style U fill:#4caf50,color:#fff
    style I fill:#2196f3,color:#fff
    style E fill:#ff9800,color:#fff
```

---

## How to run

### All tests + coverage report

```bash
./gradlew :homework-2:test jacocoTestReport
```

### Coverage gate (must be ≥ 85%)

```bash
./gradlew :homework-2:check
```

`check` depends on `jacocoTestCoverageVerification`, which fails the build if instruction coverage drops below 85%. See [Coverage notes](#coverage-notes) below for how Kotlin coroutine bytecode is handled.

### Full build (compile + test + coverage gate + JAR)

```bash
./gradlew :homework-2:build
```

### Run a single test class

```bash
./gradlew :homework-2:test --tests "homework2.routing.TicketApiTest"
```

### Run a single test method

```bash
./gradlew :homework-2:test --tests "homework2.routing.TicketApiTest.POST tickets creates ticket and returns 201"
```

### Run by layer

```bash
# Unit tests only (models, service, parsers, validation)
./gradlew :homework-2:test \
  --tests "homework2.models.*" \
  --tests "homework2.service.*" \
  --tests "homework2.utils.*" \
  --tests "homework2.validation.*"

# HTTP-layer integration tests only (routing)
./gradlew :homework-2:test --tests "homework2.routing.*"

# End-to-end workflow tests only
./gradlew :homework-2:test --tests "homework2.integration.*"

# Performance / benchmark tests only
./gradlew :homework-2:test --tests "homework2.performance.*"
```

---

## Reading test results

### Terminal summary

By default Gradle prints a one-line summary per test class and a final count:

```
> Task :homework-2:test
homework2.routing.TicketApiTest > POST tickets creates ticket and returns 201 PASSED
...
BUILD SUCCESSFUL in 12s
42 tests completed, 0 failed
```

For a failing test the summary shows `FAILED` and the stack trace is printed inline.

To see every test name as it runs (useful for spotting slow tests):

```bash
./gradlew :homework-2:test --console=plain --info
```

### HTML test report

Gradle writes a full JUnit results report — separate from the JaCoCo coverage report — after every test run:

```
homework-2/build/reports/tests/test/index.html
```

Open it in a browser:

```bash
open homework-2/build/reports/tests/test/index.html        # macOS
xdg-open homework-2/build/reports/tests/test/index.html   # Linux
```

The report shows pass/fail per class and per method, duration of each test, and the full failure message and stack trace for any failures.

### JaCoCo coverage report

```
homework-2/build/reports/jacoco/test/html/index.html
```

Shows instruction, branch, and line coverage per package and class. See [Coverage notes](#coverage-notes) for why `homework2.routing` shows ~50% in the report despite the gate passing at ~94%.

### IntelliJ IDEA

- Click the green gutter icon next to any test method or class to run it.
- Right-click a package → **Run 'Tests in homework2…'** to run all tests in that package.
- The **Run** tool window shows a pass/fail tree; failed tests display the assertion message and stack trace inline. Click **Rerun Failed Tests** to re-run only the failures.

---

## Performance tests

### What they are

`PerformanceTest.kt` contains five micro-benchmarks that run as ordinary JUnit tests through the same Gradle task — no separate server start is required. Each test exercises the **service layer directly** (no HTTP stack) and asserts that the operation completes within a generous wall-clock budget.

| Test | What it measures | Budget |
|---|---|---|
| CSV import 200 rows | Parse + validate + persist | < 3 000 ms |
| JSON import 200 rows | Parse + validate + persist | < 3 000 ms |
| Classifier p95 (100 calls) | Rule-based keyword scan | < 50 ms per call |
| List 1 000 tickets | In-memory filter + return | < 500 ms |
| Combined filter 1 000 tickets | Category + priority filter | < 200 ms |

Run them in isolation:

```bash
./gradlew :homework-2:test --tests "homework2.performance.*" --console=plain
```

Results appear in the terminal summary and in the HTML test report at `build/reports/tests/test/index.html`. A budget breach fails the test with a message such as:

```
CSV import took 4 123ms — expected < 3 000ms
```

### What they are not

These are not load tests. They do not:
- send concurrent HTTP traffic or ramp up virtual users
- measure throughput (requests/second) under sustained pressure
- produce a latency histogram or p99 under real load
- simulate multiple clients hitting the server simultaneously

For genuine load testing, start the server (`./gradlew :homework-2:run`) and point a dedicated tool at it:

| Tool | Language | Notes |
|---|---|---|
| [Gatling](https://gatling.io) | Kotlin / Scala DSL | Gradle plugin available; integrates naturally with JVM projects |
| [k6](https://k6.io) | JavaScript | Lightweight CLI; good for quick HTTP load scripts |
| [wrk](https://github.com/wg/wrk) | CLI | Minimal setup; useful for raw throughput baselines |

A Gatling scenario for this API would start the server as a separate process, ramp to N virtual users over a defined duration, and produce an HTML report with throughput, latency percentiles, and error rates — none of which the current `PerformanceTest.kt` provides.

---

## Test organisation

| File | Layer | What it covers |
|---|---|---|
| `routing/TicketApiTest.kt` | Integration | All ticket CRUD endpoints — create, list, list with filters, get-by-id, update, delete, auto-classify |
| `routing/ImportApiTest.kt` | Integration | `POST /tickets/import` — CSV/JSON/XML happy paths, partial failures, format detection from filename, error paths |
| `routing/DocumentationApiTest.kt` | Integration | `/openapi.yaml` returns YAML, `/swagger` returns non-5xx |
| `models/TicketModelTest.kt` | Unit | Enum `fromValue` factories, full `TicketValidator.validateCreate`, full `TicketValidator.validateUpdate`, `toTicket` field mapping |
| `utils/parsers/CsvImportTest.kt` | Unit | `CsvTicketParser` — 3-row parse, field mapping, optional fields, header-only, missing column, 1-based row numbers |
| `utils/parsers/JsonImportTest.kt` | Unit | `JsonTicketParser` — array, single object, invalid element, invalid JSON, empty array |
| `utils/parsers/XmlImportTest.kt` | Unit | `XmlTicketParser` — `<tickets>` root, field mapping, nested `<tags>`, `<ticket>` root, missing field, wrong root |
| `service/TicketServiceTest.kt` | Unit | `bulkImport` summary counts, `listTickets` with all filter combinations, `resolvedAt` lifecycle, metadata partial merge |
| `service/CategorizationTest.kt` | Unit | `TicketClassifier` keyword matching per category, priority precedence, confidence range, decision log, service-level classify |

---

## Test infrastructure

### `testApp()` helper

Every integration test boots a fresh, isolated in-memory application:

```kotlin
fun testApp(
    repository: InMemoryTicketRepository = InMemoryTicketRepository(),
    validator:  TicketValidator          = TicketValidator(),
    classifier: TicketClassifier         = TicketClassifier()
): ApplicationTestBuilder.() -> Unit
```

Each `testApplication { testApp()(this) }` block gets its own repository instance, so tests are fully independent — no shared mutable state between runs.

### `Fixtures` helper

Fixture files are loaded from the classpath:

```kotlin
Fixtures.text("csv/valid_tickets.csv")   // String
Fixtures.bytes("json/valid_tickets.json") // ByteArray
Fixtures.validCreateRequest(...)          // inline JSON string builder
```

---

## Fixture files

```
src/test/resources/fixtures/
├── csv/
│   ├── valid_tickets.csv       3 fully-valid rows (all optional fields populated)
│   ├── header_only.csv         header row only, zero data rows
│   └── missing_column.csv      required column absent — causes parser Failure
├── json/
│   ├── valid_tickets.json      array of 3 valid ticket objects
│   ├── invalid_tickets.json    3-element array; element 2 has an invalid email
│   └── single_ticket.json      single object (not wrapped in array)
└── xml/
    ├── valid_tickets.xml       <tickets> root, 3 <ticket> children
    ├── missing_field.xml       2 tickets; second missing <description>
    └── single_ticket.xml       <ticket> root (single-ticket shorthand)
```

---

## Coverage notes

The 85% instruction-coverage gate is measured on non-generated bytecode only. Kotlin compiles every `suspend` route-handler lambda into a coroutine continuation class (`TicketRoutesKt$registerTicketRoutes$1$3`, etc.) that contains state-machine instructions for suspension points, resumption paths, and cancellation handlers. These are structurally unreachable through HTTP integration tests and are excluded from `jacocoTestCoverageVerification`.

The HTML report still includes these classes (so the `homework2.routing` package shows ~50% there), while the gate measures ~94% on the remaining code.

Classes excluded from the gate:

```
homework2/entrypoint/MainKt*
homework2/routing/TicketRoutesKt$*
homework2/routing/DocumentationRoutesKt$*
```

See [README.md — Challenges faced](README.md#challenges-faced) for the full root-cause analysis.
