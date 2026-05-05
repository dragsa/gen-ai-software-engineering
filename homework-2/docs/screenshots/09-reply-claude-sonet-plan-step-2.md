## Step 2 — Detailed Plan: Parsers, Service & Routes

### Context
Exists from Step 1: all models, `TicketRepository` interface, `InMemoryTicketRepository`, `TicketValidator`. Empty: `utils/`, `service/` (repository only), `routing/` (docs only). `ApplicationModule.kt` has no collaborators wired yet.

---

### Files to create (7 new files)

---

#### `utils/parsers/ParsedRow.kt`

A sealed class representing the outcome of parsing a single row/element. Used by all three parsers and consumed by the service's bulk import method. Keeps parsing errors and successes in one typed structure.

```kotlin
sealed class ParsedRow {
    data class Success(val row: Int, val request: CreateTicketRequest) : ParsedRow()
    data class Failure(val row: Int, val error: String, val rawData: String?) : ParsedRow()
}
```

---

#### `utils/parsers/CsvTicketParser.kt`

Uses Apache Commons CSV (`CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)`).

- Accepts a `String` (file content decoded as UTF-8).
- Iterates records; row number is 1-based (header = row 0, first data row = row 1).
- Expected columns: `customer_id`, `customer_email`, `customer_name`, `subject`, `description`, `category`, `priority`, `status`, `assigned_to`, `tags`, `metadata_source`, `metadata_browser`, `metadata_device_type`.
- `tags` column: comma-separated values within the cell, split and trimmed.
- `metadata_*` columns: mapped into `CreateMetadataRequest` only when `metadata_source` is present and non-blank.
- If a required CSV column is missing entirely from the header → `ParsedRow.Failure` for every row with a descriptive message.
- If a specific record throws during mapping → `ParsedRow.Failure` for that row, continues to next.
- Returns `List<ParsedRow>`.

---

#### `utils/parsers/JsonTicketParser.kt`

Uses `kotlinx.serialization` JSON.

- Accepts a `String`.
- Parses the root as a `JsonElement`. If it is a `JsonArray`, each element is decoded individually as `CreateTicketRequest`. If it is a `JsonObject`, it is treated as a single-element import.
- Each element is decoded inside a `try/catch(SerializationException)` so one bad element does not abort the whole file.
- Row numbers are 1-based index into the array (or `1` for a single object).
- Returns `List<ParsedRow>`.

---

#### `utils/parsers/XmlTicketParser.kt`

Uses JDK `javax.xml.parsers.DocumentBuilderFactory` only — no third-party XML library.

**Security hardening** applied on the `DocumentBuilder` before parsing (XXE/DTD disabled):
```
FEATURE "http://apache.org/xml/features/disallow-doctype-decl" → true
FEATURE "http://xml.org/sax/features/external-general-entities" → false
FEATURE "http://xml.org/sax/features/external-parameter-entities" → false
setExpandEntityReferences(false)
```

- Expected structure: `<tickets><ticket>...</ticket></tickets>`.
- Each `<ticket>` child element maps to `CreateTicketRequest` by reading named child text nodes.
- `<tags>` child: contains `<tag>` children, collected into a list.
- `<metadata>` child: contains `<source>`, `<browser>`, `<device_type>` children.
- If the top-level element is a single `<ticket>` (not wrapped in `<tickets>`), it is treated as a single-element import.
- Each element mapping is wrapped in `try/catch` so one bad element does not abort the whole file.
- Row numbers are 1-based index of the `<ticket>` element.
- Returns `List<ParsedRow>`.

---

#### `service/TicketService.kt`

Interface. Routes depend on this — never on the implementation.

```kotlin
interface TicketService {
    fun createTicket(req: CreateTicketRequest): Ticket
    fun getTicket(id: String): Ticket?
    fun listTickets(filter: TicketFilter): List<Ticket>
    fun updateTicket(id: String, req: UpdateTicketRequest): Ticket?
    fun deleteTicket(id: String): Boolean
    fun bulkImport(parsedRows: List<ParsedRow>): ImportSummaryResponse
}
```

---

#### `service/TicketServiceImpl.kt`

```kotlin
class TicketServiceImpl(
    private val repository: TicketRepository,
    private val validator: TicketValidator,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val timestampProvider: () -> String = { Instant.now().toString() }
) : TicketService
```

**`createTicket`** — maps the validated request to a domain `Ticket` via `validator.toTicket(req, id, now)`, then calls `repository.create(ticket)`. Assumes the route has already validated; no re-validation here.

**`getTicket`** — delegates to `repository.findById(id)`.

**`listTickets`** — delegates to `repository.findAll(filter)`.

**`updateTicket`** — calls `repository.update(id) { existing -> applyUpdate(existing, req) }`. Private `applyUpdate` applies each non-null field from `UpdateTicketRequest` to the existing `Ticket`, leaving null fields unchanged.

**`deleteTicket`** — delegates to `repository.delete(id)`.

**`bulkImport`** — iterates `parsedRows`:
- `ParsedRow.Failure` → immediately recorded as `ImportFailure(row, error, rawData)`.
- `ParsedRow.Success` → runs `validator.validateCreate(req)`:
    - If errors → recorded as `ImportFailure(row, errors joined by "; ", rawData=null)`.
    - If valid → calls `createTicket(req)`, counts as success.
- Returns `ImportSummaryResponse(total, successful, failed, failures)`.

`applyUpdate` private method — exhaustively maps all nullable fields:

```
category  → if non-null, coerce via Category.fromValue() (already validated by route)
priority  → same pattern
status    → same pattern
metadata  → merge: only non-null fields within UpdateMetadataRequest replace existing values
all other String/List fields → replace if non-null
```

---

#### `routing/TicketRoutes.kt`

One `Route` extension function `registerTicketRoutes(service, validator)` wrapping all endpoints inside `route("/tickets") { ... }`.

**Error helper** (private extension on `ApplicationCall`):
```kotlin
private suspend fun ApplicationCall.respondValidation(errors: List<ValidationError>) =
    respond(HttpStatusCode.BadRequest, ErrorResponse("Validation failed", errors))
```

**`POST /tickets`**
1. `receive<CreateTicketRequest>()` — if deserialization fails → 400.
2. `validator.validateCreate(req)` — if errors → 400.
3. `service.createTicket(req)` → `TicketResponse.from(ticket)` → 201.
4. `?auto_classify=true` query param noted but not wired until Step 3.

**`GET /tickets`**
1. Read query params: `category`, `priority`, `status`, `assigned_to`, `customer_id`, `search`.
2. Validate enum params via `fromValue()` — collect any invalid values as errors → 400.
3. Build `TicketFilter` → `service.listTickets(filter)` → `List<TicketResponse>` → 200.

**`GET /tickets/{id}`**
1. `service.getTicket(id)` → `TicketResponse.from(ticket)` → 200, or 404.

**`PUT /tickets/{id}`**
1. `receive<UpdateTicketRequest>()` — if deserialization fails → 400.
2. `validator.validateUpdate(req)` — if errors → 400.
3. `service.updateTicket(id, req)` → `TicketResponse.from(ticket)` → 200, or 404.

**`DELETE /tickets/{id}`**
1. `service.deleteTicket(id)` → 204 if deleted, 404 if not found.

**`POST /tickets/import`**
1. `call.receiveMultipart()` — iterate parts, extract the first `PartData.FileItem`.
2. If no file part found → 400.
3. Format detection — in order:
    - Part `Content-Type` header: `text/csv` or `application/csv` → CSV; `application/json` → JSON; `application/xml` or `text/xml` → XML.
    - Fallback: filename extension `.csv` / `.json` / `.xml`.
    - If neither resolves format → 400 with message `"Cannot determine file format. Provide Content-Type or use a .csv/.json/.xml filename"`.
4. Call appropriate parser → `List<ParsedRow>`.
5. If parser itself throws (e.g. XML parse error at document level) → 400 with parse error message.
6. `service.bulkImport(parsedRows)` → `ImportSummaryResponse` → 200.

---

### Files to modify (2 files)

**`entrypoint/ApplicationModule.kt`** — wire production collaborators:
```kotlin
fun Application.module() {
    val repository = InMemoryTicketRepository()
    val validator  = TicketValidator()
    val service    = TicketServiceImpl(repository, validator)
    module(service, validator)
}

fun Application.module(service: TicketService, validator: TicketValidator) {
    install(ContentNegotiation) { json() }
    routing {
        registerDocumentationRoutes()
        registerTicketRoutes(service, validator)
    }
}
```

**`src/main/resources/openapi.yaml`** — filled out with all 6 paths, request/response schemas for `Ticket`, `CreateTicketRequest`, `UpdateTicketRequest`, `ImportSummaryResponse`, `ErrorResponse`.

---

### Verification
```bash
./gradlew :homework-2:build --console=plain
```
Expected: `BUILD SUCCESSFUL`.

Manual smoke test:
```bash
# Start server
./gradlew :homework-2:run

# Create a ticket
curl -X POST http://localhost:8080/tickets \
  -H "Content-Type: application/json" \
  -d '{"customer_id":"c1","customer_email":"a@b.com","customer_name":"Alice","subject":"Login broken","description":"Cannot log in since this morning"}'

# List tickets
curl http://localhost:8080/tickets
```

---

### Risks
- Ktor 3.x multipart: `call.receiveMultipart()` is in `ktor-server-core`; `PartData.FileItem.streamProvider()` returns a standard `InputStream` — no additional dependency needed.
- `kotlinx.serialization` JSON parsing per-element: each array element decoded inside `try/catch(SerializationException)` — partial import survives malformed elements.
- XML: if the document itself is not well-formed XML, `DocumentBuilder.parse()` throws `SAXParseException` — caught at the route level and returned as 400.
- `applyUpdate` must not overwrite `resolvedAt` or `createdAt` — those are server-managed fields only.

---

**Awaiting your approval to execute.**