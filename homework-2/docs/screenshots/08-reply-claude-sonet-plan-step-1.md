## Step 1 — Detailed Plan: Models, Repository & Validation

### Context
Currently exists: `entrypoint/`, `routing/DocumentationRoutes.kt`. Empty: `models/`, `service/`, `validation/`. No routes, no parsers yet — those are Step 2.

---

### Files to create (9 files)

---

#### `models/Enums.kt`

Five enumerations covering all domain-constrained string fields. Each entry carries a `value: String` property holding the exact JSON wire name, and a companion `fromValue(s: String): EnumType?` that returns `null` on unknown input — never throws. `@SerialName` is placed on each entry so `kotlinx.serialization` writes the wire name, not the Kotlin constant name.

```
Category:   ACCOUNT_ACCESS    → "account_access"
            TECHNICAL_ISSUE   → "technical_issue"
            BILLING_QUESTION  → "billing_question"
            FEATURE_REQUEST   → "feature_request"
            BUG_REPORT        → "bug_report"
            OTHER             → "other"

Priority:   URGENT → "urgent"
            HIGH   → "high"
            MEDIUM → "medium"
            LOW    → "low"

Status:     NEW              → "new"
            IN_PROGRESS      → "in_progress"
            WAITING_CUSTOMER → "waiting_customer"
            RESOLVED         → "resolved"
            CLOSED           → "closed"

Source:     WEB_FORM → "web_form"
            EMAIL    → "email"
            API      → "api"
            CHAT     → "chat"
            PHONE    → "phone"

DeviceType: DESKTOP → "desktop"
            MOBILE  → "mobile"
            TABLET  → "tablet"
```

---

#### `models/Ticket.kt`

Internal domain entity. Not `@Serializable` — never exposed directly to the HTTP layer. Routes and services work with this type internally; a mapping step converts it to `TicketResponse` before sending to clients.

```
data class Ticket(
    id:            String,
    customerId:    String,
    customerEmail: String,
    customerName:  String,
    subject:       String,
    description:   String,
    category:      Category,
    priority:      Priority,
    status:        Status,
    createdAt:     String,      // ISO-8601 instant string
    updatedAt:     String,      // ISO-8601 instant string
    resolvedAt:    String?,     // null until status becomes RESOLVED or CLOSED
    assignedTo:    String?,
    tags:          List<String>,
    metadata:      Metadata
)

data class Metadata(
    source:     Source,
    browser:    String?,
    deviceType: DeviceType?
)
```

---

#### `models/TicketRequests.kt`

`@Serializable` input DTOs. All field names use `@SerialName` for `snake_case` wire names. Fields that are optional on creation carry default values.

```
@Serializable
data class CreateTicketRequest(
    customer_id:    String,
    customer_email: String,
    customer_name:  String,
    subject:        String,
    description:    String,
    category:       String?         = null,
    priority:       String?         = null,
    status:         String?         = null,
    assigned_to:    String?         = null,
    tags:           List<String>    = emptyList(),
    metadata:       MetadataRequest? = null
)

@Serializable
data class UpdateTicketRequest(
    // all fields nullable — only provided fields are applied (partial update)
    customer_id:    String?      = null,
    customer_email: String?      = null,
    customer_name:  String?      = null,
    subject:        String?      = null,
    description:    String?      = null,
    category:       String?      = null,
    priority:       String?      = null,
    status:         String?      = null,
    assigned_to:    String?      = null,
    tags:           List<String>? = null,
    metadata:       MetadataRequest? = null
)

@Serializable
data class MetadataRequest(
    source:      String? = null,
    browser:     String? = null,
    device_type: String? = null
)
```

---

#### `models/TicketResponse.kt`

`@Serializable` output DTOs. Enum fields are serialized as their string `value` via `@SerialName` on the enum entries. A static `from(ticket: Ticket)` companion function maps the domain entity to this response type — the only place this conversion happens.

```
@Serializable
data class TicketResponse(
    id, customer_id, customer_email, customer_name,
    subject, description,
    category: String, priority: String, status: String,
    created_at, updated_at, resolved_at?,
    assigned_to?, tags: List<String>,
    metadata: MetadataResponse
)

@Serializable
data class MetadataResponse(source: String, browser: String?, device_type: String?)

@Serializable
data class ImportSummaryResponse(
    total:      Int,
    successful: Int,
    failed:     Int,
    failures:   List<ImportFailure>
)

@Serializable
data class ImportFailure(
    row:     Int,
    reason:  String,
    rawData: String?  // the original row/element text for diagnostics
)
```

---

#### `models/ErrorModels.kt`

Shared `@Serializable` types used for all error responses across every endpoint. One consistent shape for both deserialization failures and business validation failures so clients have a single contract to handle.

```
@Serializable
data class ValidationError(field: String, message: String)

@Serializable
data class ErrorResponse(error: String, details: List<ValidationError> = emptyList())
```

Also contains `TicketFilter` — a plain (non-serializable) data class used internally to carry filter parameters from the routing layer to the repository:

```
data class TicketFilter(
    category:   Category?  = null,
    priority:   Priority?  = null,
    status:     Status?    = null,
    assignedTo: String?    = null,
    customerId: String?    = null,
    search:     String?    = null   // case-insensitive match on subject + description
)
```

---

#### `service/TicketRepository.kt`

Interface defining the persistence contract. Routes and services depend on this interface, never the implementation — allowing tests to substitute a fake.

```kotlin
interface TicketRepository {
    fun create(ticket: Ticket): Ticket
    fun findById(id: String): Ticket?
    fun findAll(filter: TicketFilter): List<Ticket>
    fun update(id: String, updater: (Ticket) -> Ticket): Ticket?
    fun delete(id: String): Boolean
}
```

---

#### `service/InMemoryTicketRepository.kt`

Thread-safe in-memory implementation backed by a `mutableListOf<Ticket>()`. All reads and writes are wrapped in `synchronized(store) { ... }`. A private `snapshot()` method returns a safe copy before filtering, preventing concurrent modification during iteration.

Constructor accepts two injectable lambdas for deterministic testing:
```kotlin
class InMemoryTicketRepository(
    private val idGenerator:       () -> String = { UUID.randomUUID().toString() },
    private val timestampProvider: () -> String = { Instant.now().toString() }
)
```

Lifecycle rules:
- `create` — sets `createdAt = updatedAt = timestampProvider()`, `resolvedAt = null`
- `update` — sets `updatedAt = timestampProvider()`; if the new status is `RESOLVED` or `CLOSED` and `resolvedAt` is currently `null`, sets `resolvedAt = timestampProvider()`
- `findAll` — applies `TicketFilter` fields in sequence on the snapshot:
    - `category`, `priority`, `status`, `assignedTo`, `customerId` → exact match (null means skip)
    - `search` → case-insensitive `contains` check across both `subject` and `description`
- `delete` — removes by id, returns `true` if found and removed, `false` if not found

---

#### `validation/TicketValidator.kt`

Returns `List<ValidationError>` from every method. Never throws for business-rule violations. Two public methods:

**`validateCreate(req: CreateTicketRequest): List<ValidationError>`**

| Field | Rule |
|---|---|
| `customer_id` | non-blank |
| `customer_name` | non-blank |
| `customer_email` | matches `^[^@\s]+@[^@\s]+\.[^@\s]+$` |
| `subject` | 1–200 chars |
| `description` | 10–2000 chars |
| `category` | if non-null, must parse via `Category.fromValue()` |
| `priority` | if non-null, must parse via `Priority.fromValue()` |
| `status` | if non-null, must parse via `Status.fromValue()` |
| `metadata.source` | if non-null, must parse via `Source.fromValue()` |
| `metadata.device_type` | if non-null, must parse via `DeviceType.fromValue()` |

**`validateUpdate(req: UpdateTicketRequest): List<ValidationError>`** — same rules, but only for non-null fields (partial update semantics — absent fields are not validated).

**`toTicket(req: CreateTicketRequest, id: String, now: String): Ticket`** — maps a validated request to the domain entity. Defaults applied: `category → OTHER`, `priority → MEDIUM`, `status → NEW`.

---

### No changes to existing files
`ApplicationModule.kt`, `Main.kt`, `DocumentationRoutes.kt` — untouched this step.

---

### Verification
```bash
./gradlew :homework-2:build --console=plain
```
Expected: `BUILD SUCCESSFUL` — all new types compile cleanly, no tests yet.

---

### Risks
- `kotlinx.serialization` serializes Kotlin enums by constant name by default. Using `@SerialName` on each enum entry overrides this to the wire value — no custom serializer needed.
- `UpdateTicketRequest` all-nullable means the routing layer (Step 2) must distinguish "field absent" from "field explicitly set to null". This is intentional and will be handled there.

---

**Awaiting your approval to execute.**