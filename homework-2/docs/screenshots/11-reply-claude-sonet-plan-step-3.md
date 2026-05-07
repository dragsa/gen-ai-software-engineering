## Step 3 — Detailed Plan: Auto-Classification

### Context
Exists from Step 2: all routes, service, parsers. `openapi.yaml` already has the `ClassificationResponse` schema and `/tickets/{id}/auto-classify` path stub. `TicketRoutes.kt` has a comment noting `?auto_classify=true` is not yet wired.

---

### Files to create (2 new files)

---

#### `models/ClassificationModels.kt`

`@Serializable` response DTO returned by the classify endpoint. Also contains the internal `ClassificationDecision` domain type stored in the decision log.

```kotlin
// HTTP response — serializable
@Serializable
data class ClassificationResponse(
    @SerialName("ticket_id")      val ticketId: String,
    val category:                 String,           // enum wire value
    val priority:                 String,           // enum wire value
    val confidence:               Double,           // 0.0 – 1.0
    val reasoning:                String,
    @SerialName("keywords_found") val keywordsFound: List<String>
)

// Internal decision log entry — not serializable
data class ClassificationDecision(
    val ticketId:     String,
    val category:     Category,
    val priority:     Priority,
    val confidence:   Double,
    val reasoning:    String,
    val keywordsFound: List<String>,
    val decidedAt:    String           // ISO-8601 instant
)
```

---

#### `service/TicketClassifier.kt`

Contains both the classifier logic and the in-memory decision log. No public endpoint for the log — internal only.

**Keyword maps:**

Category keywords (checked against lowercased `subject + " " + description`):

| Category | Keywords |
|---|---|
| `ACCOUNT_ACCESS` | login, password, 2fa, two-factor, sign in, sign-in, locked out, account access, authentication, forgot password, reset password |
| `TECHNICAL_ISSUE` | error, crash, bug, not working, broken, fails, failure, exception, timeout, slow, performance, down, outage |
| `BILLING_QUESTION` | invoice, billing, charge, payment, refund, subscription, price, cost, receipt, overcharged, discount |
| `FEATURE_REQUEST` | feature, request, suggestion, enhancement, improve, would like, wish, add support, could you, please add |
| `BUG_REPORT` | bug, reproduce, steps to reproduce, expected behavior, actual behavior, defect, regression, version |

Priority keywords (checked independently of category):

| Priority | Keywords |
|---|---|
| `URGENT` | can't access, cannot access, critical, production down, prod down, security, data loss, urgent, emergency, immediately |
| `HIGH` | important, blocking, blocked, asap, as soon as possible, high priority |
| `LOW` | minor, cosmetic, suggestion, nice to have, low priority, whenever |

**Confidence formula** — transparent and documented:
```
categorySignal = (matched category keywords) / (total keywords in winning category map)
prioritySignal = 1.0 if any priority keyword matched, else 0.5 (default MEDIUM gets 0.5)
confidence     = (categorySignal * 0.7) + (prioritySignal * 0.3)
confidence     = confidence.coerceIn(0.0, 1.0).roundTo2Decimal()
```

Rationale: category match drives 70% of confidence (it requires matching specific vocabulary); priority match drives 30% (binary — either a keyword fires or we fall back to MEDIUM).

**Category resolution:**
- Count keyword hits per category across the full text.
- Winning category = highest hit count.
- If all counts are zero → `OTHER`.

**Priority resolution (independent of category):**
- Check URGENT keywords first — if any match → `URGENT`.
- Then HIGH — if any match → `HIGH`.
- Then LOW — if any match → `LOW`.
- Otherwise → `MEDIUM`.

**`classify(ticket: Ticket): ClassificationDecision`** — main method:
1. Build the search text: `"${ticket.subject} ${ticket.description}".lowercase()`.
2. Count category hits, determine winner.
3. Determine priority.
4. Collect all matched keywords into `keywordsFound` (deduped, sorted).
5. Build `reasoning` string: e.g. `"Matched category 'technical_issue' (3 keywords: error, crash, timeout). Priority set to 'urgent' (keyword: production down)."`.
6. Compute confidence.
7. Create `ClassificationDecision`, append to internal `decisions` list (synchronized).
8. Return decision.

**`getDecisions(): List<ClassificationDecision>`** — package-internal accessor for tests. Not exposed via HTTP.

---

### Files to modify (3 files)

---

#### `service/TicketService.kt`

Add one method to the interface:

```kotlin
fun classifyTicket(id: String): ClassificationDecision?
// Returns null if ticket not found
```

---

#### `service/TicketServiceImpl.kt`

Add `TicketClassifier` as a constructor parameter (defaulting to `TicketClassifier()`):

```kotlin
class TicketServiceImpl(
    private val repository: TicketRepository,
    private val validator: TicketValidator,
    private val classifier: TicketClassifier = TicketClassifier(),
    ...
)
```

Implement `classifyTicket`:
1. `repository.findById(id)` — return null if not found.
2. `classifier.classify(ticket)` — get decision.
3. `repository.update(id)` — persist `category` and `priority` from the decision onto the ticket (classification overrides the existing values).
4. Return the decision.

---

#### `routing/TicketRoutes.kt`

Two additions:

**`POST /tickets/{id}/auto-classify`** — new route inside `route("/tickets") { ... }`:
```
1. service.classifyTicket(id) — null → 404
2. Map ClassificationDecision → ClassificationResponse → 200
```

**Wire `?auto_classify=true` on `POST /tickets`** — after the ticket is created:
```kotlin
val autoClassify = call.request.queryParameters["auto_classify"]?.toBooleanStrictOrNull() ?: false
if (autoClassify) service.classifyTicket(ticket.id)
```
The `POST /tickets` response always returns the ticket as-is (without the classification payload) — the classification is persisted silently and the updated category/priority are reflected in the returned `TicketResponse`.

---

#### `entrypoint/ApplicationModule.kt`

Wire `TicketClassifier` into `TicketServiceImpl`:

```kotlin
val classifier = TicketClassifier()
val service    = TicketServiceImpl(repository, validator, classifier)
```

Update the parameterised `module()` overload to accept `TicketClassifier` so tests can inject a fake:

```kotlin
fun Application.module(
    service: TicketService,
    validator: TicketValidator
)
```
No change needed here — `TicketClassifier` is encapsulated inside `TicketServiceImpl`; tests that need to control classification will inject a custom `TicketServiceImpl` with a seeded classifier.

---

### `API_NOTES.md` update

Add a section documenting the confidence formula and keyword maps, since this feeds into `API_REFERENCE.md` in Step 5.

---

### Verification
```bash
./gradlew :homework-2:build
```

Manual smoke test:
```bash
# Create a ticket
curl -s -X POST http://localhost:8080/tickets \
  -H "Content-Type: application/json" \
  -d '{"customer_id":"c1","customer_email":"a@b.com","customer_name":"Alice",
       "subject":"Production is down","description":"Critical error, cannot access the system"}'

# Auto-classify it (replace UUID with id from above response)
curl -s -X POST http://localhost:8080/tickets/{id}/auto-classify
```

---

### Risks
- Keyword overlap between `TECHNICAL_ISSUE` and `BUG_REPORT` (both include "bug") — the count-based approach handles this naturally since `BUG_REPORT` has more specific keywords that will score higher when the full vocabulary is present.
- `?auto_classify=true` on `POST /tickets`: the classification result is not returned in the create response (the ticket body is). This is intentional and documented in `API_NOTES.md`.

---

**Awaiting your approval to execute.**