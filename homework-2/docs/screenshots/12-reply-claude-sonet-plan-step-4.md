## Step 4 — Detailed Plan: Test Suite (>85% Coverage)

### Context
Exists: all main sources (1,679 lines across 18 files). Test directory is empty except `.gitkeep` placeholders. JaCoCo plugin is applied but no coverage gate or report task is configured yet.

---

### Files to modify (1 file)

#### `homework-2/build.gradle.kts`

Add three things:

**1. Finalize `test` with `jacocoTestReport`** so a report is always generated after tests run:
```kotlin
tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}
```

**2. `jacocoTestReport`** with exclusions for untestable entrypoint code:
```kotlin
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) { exclude("homework2/entrypoint/MainKt*") }
    }))
    reports { xml.required = true; html.required = true }
}
```

**3. `jacocoTestCoverageVerification`** gate at 85%, wired into `check`:
```kotlin
tasks.jacocoTestCoverageVerification {
    classDirectories.setFrom(tasks.jacocoTestReport.get().classDirectories)
    violationRules {
        rule { limit { minimum = "0.85".toBigDecimal() } }
    }
}
tasks.check { dependsOn(tasks.jacocoTestCoverageVerification) }
```

---

### Files to create (11 files)

---

#### `src/test/resources/fixtures/` — 9 fixture files

**`valid_tickets.csv`** — 3 valid rows with all columns including tags and metadata.

**`tickets_header_only.csv`** — header row only, no data rows.

**`tickets_missing_column.csv`** — CSV without the `description` column (a required column).

**`valid_tickets.json`** — JSON array with 3 valid ticket objects.

**`single_ticket.json`** — single JSON object (not an array).

**`invalid_tickets.json`** — JSON array where one element is an integer, not an object.

**`valid_tickets.xml`** — `<tickets>` root with 3 `<ticket>` children including `<tags>` and `<metadata>`.

**`single_ticket.xml`** — `<ticket>` root element (no wrapper).

**`missing_field.xml`** — `<tickets>` root where one `<ticket>` is missing `<description>`.

---

#### `src/test/kotlin/homework2/testsupport/Fixtures.kt`

Loads fixture files from the classpath. Provides helpers for inline JSON payloads so tests can build requests without string literals scattered across files.

```kotlin
object Fixtures {
    fun text(name: String): String          // load as UTF-8 string
    fun bytes(name: String): ByteArray      // load as raw bytes

    // Inline payload builders for API tests
    fun validCreateRequest(...): String     // returns JSON string
    fun minimalCreateRequest(): String      // bare minimum fields
}
```

---

#### `src/test/kotlin/homework2/testsupport/TestApplicationHelper.kt`

A single helper function that creates a `testApplication` with injected collaborators. Every API test calls this instead of duplicating setup:

```kotlin
fun testApp(
    repository: TicketRepository  = InMemoryTicketRepository(),
    validator:  TicketValidator   = TicketValidator(),
    classifier: TicketClassifier  = TicketClassifier()
): ApplicationTestBuilder.() -> Unit = {
    application {
        module(TicketServiceImpl(repository, validator, classifier), validator)
    }
}
```

---

#### `src/test/kotlin/homework2/routing/TicketApiTest.kt` — 11 tests

Uses `testApplication(testApp()) { ... }` pattern. All tests hit real HTTP endpoints against an in-memory stack.

| # | Test name | What it verifies |
|---|---|---|
| 1 | `POST returns 201 for valid request` | Happy path create; response has id, defaults applied |
| 2 | `POST returns 400 for blank customer_id` | Validator fires on missing required string |
| 3 | `POST returns 400 for invalid email` | Email regex validation |
| 4 | `POST returns 400 for subject over 200 chars` | Length upper bound |
| 5 | `POST returns 400 for description under 10 chars` | Length lower bound |
| 6 | `GET returns 200 with all tickets` | List endpoint, no filter |
| 7 | `GET with category filter returns only matching tickets` | Filter applied correctly |
| 8 | `GET by id returns 200 for existing ticket` | Single fetch happy path |
| 9 | `GET by id returns 404 for unknown id` | Not-found response |
| 10 | `PUT returns 200 and applies partial update` | Only supplied fields change; others preserved |
| 11 | `DELETE returns 204 and ticket is gone` | Delete then GET returns 404 |

---

#### `src/test/kotlin/homework2/models/TicketModelTest.kt` — 9 tests

Pure unit tests — no HTTP, no Ktor. Tests `TicketValidator` and `TicketResponse.from()` directly.

| # | Test name |
|---|---|
| 1 | `validateCreate returns empty list for fully valid request` |
| 2 | `validateCreate returns error for blank customer_id` |
| 3 | `validateCreate returns error for invalid email format` |
| 4 | `validateCreate returns error for subject exceeding 200 chars` |
| 5 | `validateCreate returns error for description shorter than 10 chars` |
| 6 | `validateCreate returns error for unknown category value` |
| 7 | `validateCreate returns error for unknown priority value` |
| 8 | `validateUpdate only checks non-null fields` |
| 9 | `toTicket applies defaults: category=OTHER, priority=MEDIUM, status=NEW` |

---

#### `src/test/kotlin/homework2/utils/parsers/CsvImportTest.kt` — 6 tests

Pure unit tests against `CsvTicketParser.parse()`.

| # | Test name |
|---|---|
| 1 | `parse returns Success rows for valid CSV` |
| 2 | `parse splits comma-separated tags within a cell` |
| 3 | `parse maps metadata_source and metadata_browser columns` |
| 4 | `parse returns single Failure for missing required column` |
| 5 | `parse returns Failure row for record with blank required field` |
| 6 | `parse returns empty list for header-only CSV` |

---

#### `src/test/kotlin/homework2/utils/parsers/JsonImportTest.kt` — 5 tests

Pure unit tests against `JsonTicketParser.parse()`.

| # | Test name |
|---|---|
| 1 | `parse returns Success rows for valid JSON array` |
| 2 | `parse wraps single JSON object as one Success row` |
| 3 | `parse returns Failure for completely malformed JSON` |
| 4 | `parse returns Failure only for the malformed element, succeeds on others` |
| 5 | `parse returns Failure for JSON root that is neither object nor array` |

---

#### `src/test/kotlin/homework2/utils/parsers/XmlImportTest.kt` — 5 tests

Pure unit tests against `XmlTicketParser.parse()`.

| # | Test name |
|---|---|
| 1 | `parse returns Success rows for valid XML with tickets wrapper` |
| 2 | `parse handles single ticket root element` |
| 3 | `parse returns Failure for malformed XML` |
| 4 | `parse rejects XML containing DOCTYPE declaration (XXE protection)` |
| 5 | `parse returns Failure for ticket missing required element` |

---

#### `src/test/kotlin/homework2/service/CategorizationTest.kt` — 10 tests

Pure unit tests against `TicketClassifier.classify()` using a pre-built `Ticket` stub.

| # | Test name |
|---|---|
| 1 | `classify returns ACCOUNT_ACCESS for login-related text` |
| 2 | `classify returns TECHNICAL_ISSUE for error/crash text` |
| 3 | `classify returns BILLING_QUESTION for payment-related text` |
| 4 | `classify returns FEATURE_REQUEST for enhancement text` |
| 5 | `classify returns BUG_REPORT for reproduction-step text` |
| 6 | `classify returns OTHER when no keywords match` |
| 7 | `classify sets URGENT priority for critical/production-down text` |
| 8 | `classify sets HIGH priority for blocking text` |
| 9 | `classify sets LOW priority for minor/cosmetic text` |
| 10 | `classify confidence is in range 0..1 and reasoning is non-empty` |

---

### Verification
```bash
./gradlew :homework-2:test jacocoTestReport jacocoTestCoverageVerification --console=plain
```
Expected: all 51 tests pass, coverage ≥ 85%, report at `build/reports/jacoco/test/html/index.html`.

---

### Risks
- JaCoCo exclusions must match the compiled class name exactly (`MainKt*` not `Main*`) — will verify after first run.
- `TicketApiTest` uses `testApplication` which starts a real embedded server; each test gets a fresh `InMemoryTicketRepository` via `testApp()` so tests are isolated.
- XML DOCTYPE test: the `disallow-doctype-decl` feature throws a `SAXParseException` — the parser catches this and returns a `ParsedRow.Failure`, so the test asserts on the failure list rather than an exception.
- Coverage on `TicketClassifier.classify()` requires exercising many keyword branches — the 10 categorization tests are designed to hit every branch.

---

**Awaiting your approval to execute.**