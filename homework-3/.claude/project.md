# RTAF — Claude Code Project Rules

This file configures Claude Code's behaviour for the Real-Time Antifraud (RTAF) project.
All rules apply to every file in `homework-3/` unless a task instruction explicitly overrides a specific rule and states the reason.

---

## Project Context

This is a **FinTech / payment security** project. The system processes live payment transactions and makes real-time fraud decisions. Mistakes in this domain can cause financial harm, regulatory breaches, or fraud signal leakage. Apply the highest level of care to correctness, security, and auditability.

The authoritative domain rules are in `homework-3/agents.md`. This file provides the editor-level subset that Claude Code should apply automatically during code generation, review, and refactoring.

---

## Naming Conventions

### Events
- Event type names use **snake_case** and are **past-tense** nouns: `transaction_requested`, `decision_made`, `transaction_settled`, `chargeback_raised`
- Never abbreviate: `txnReq`, `decMade`, `txn_settled` are rejected

### Services and Components
- Service class names are **PascalCase** nouns describing their responsibility: `DecisionRouter`, `HardBlockChecker`, `FraudEngineScoringClient`, `AuditLogWriter`
- No generic suffixes like `Manager`, `Handler`, `Helper`, `Util`

### Fields and Variables
- Internal fraud signal: `denial_reason` (snake_case in JSON/Kotlin data class), never `declineCode`, `rejectReason`, `blockReason`
- User-facing category: `user_facing_category`, never `userMessage`, `displayReason`
- Money amounts: `amountMinorUnits: Long` in contracts; `amount: BigDecimal` in domain logic
- Identifiers: `transactionId`, `caseId`, `idempotencyKey` (camelCase in Kotlin)

### Files
- One top-level concern per file
- File name matches primary class name exactly

---

## Patterns to Prefer

### Fail-Closed Default
Always write the failure branch before the success branch when handling upstream calls.
```kotlin
// Preferred
return try {
    fraudEngineClient.score(request)
} catch (e: TimeoutException) {
    // fail closed
    PendingDecision(denialReason = DenialReason.FRAUD_ENGINE_UNAVAILABLE)
}
```

### Idempotency at Every Write Site
Every method that produces a side effect (queue publish, event emit, audit write) must accept and check an idempotency key or dedup ID before executing.

### Contract-First API Design
Define the request/response data classes (`AnalyzeRequest`, `AnalyzeResponse`) before implementing any business logic. Business logic must depend on the contract types, not the other way around.

### Structured Errors with Machine-Readable Codes
Return errors as structured objects: `{ "error_code": "IDEMPOTENCY_KEY_MISSING", "message": "..." }`. Never return unstructured exception stack traces to API callers.

### Dependency Injection Over Static Calls
All external dependencies (fraud engine client, vendor cache, audit log writer) must be injected. No `object` singletons or static method calls in business logic.

---

## What to Avoid

### Monetary Types
```kotlin
// NEVER — will cause rounding errors
val amount: Double = 19.99
val amount: Float = 19.99f

// CORRECT
val amountMinorUnits: Long = 1999      // in API contracts
val amount: BigDecimal = BigDecimal("19.99")  // in domain logic
```

### Raw PAN in Any Output
```kotlin
// NEVER — PCI violation
log.info("Processing card: ${request.pan}")
logger.debug("Transaction for card $cardNumber")

// CORRECT
log.info("Processing transaction: ${request.transactionId}")
// If last-4 is genuinely needed:
log.info("Card ending ${PciSanitizationLayer.mask(pan)}")
```

### `denial_reason` in Client-Facing Responses
```kotlin
// NEVER
data class PublicErrorResponse(
    val error: String,
    val denialReason: DenialReason  // leaks internal fraud signal
)

// CORRECT
data class PublicErrorResponse(
    val userFacingCategory: UserFacingCategory,
    val message: String
)
```

### Hardcoded Thresholds
```kotlin
// NEVER
if (score >= 70) deny()

// CORRECT
if (score >= config.getInt("rtaf.thresholds.autoDeny")) deny()
```

### Optimistic Fallback on Failure
```kotlin
// NEVER — auto-allow on upstream failure
fun score(request: ScoreRequest): Decision {
    return try { callFraudEngine(request) }
    catch (e: Exception) { Decision.ALLOW }  // prohibited
}

// CORRECT — fail closed
fun score(request: ScoreRequest): Decision {
    return try { callFraudEngine(request) }
    catch (e: Exception) { Decision.PENDING(DenialReason.FRAUD_ENGINE_UNAVAILABLE) }
}
```

---

## FinTech-Sensitive Defaults

When generating code for this project, Claude Code must apply the following defaults **without being asked**:

| Concern | Default behaviour |
|---|---|
| Upstream failure | Fail closed (`PENDING`) — never fail open (`ALLOW`) |
| Logging | Always pass data through `PciSanitizationLayer` before any log statement; never log raw request objects |
| Retries | Max 3 retries with exponential backoff; never unbounded retry loops |
| Timeouts | Always set an explicit timeout on every external HTTP call; never rely on default (infinite) timeouts |
| Audit | Write audit record before returning response; failure to audit = do not return `ALLOW` |
| Money | `BigDecimal` or `Long` (minor units); never `Double`/`Float` |
| Idempotency | Every write operation that is triggered externally must check idempotency key before executing |
| Error responses | Structured, machine-readable; no stack traces; no `denial_reason` in public surfaces |

---

## Testing Defaults

When generating tests, Claude Code must include the following **without being asked**:

- **Boundary-value cases** for every numeric threshold: `threshold - 1`, `threshold`, `threshold + 1`
- **Adversarial PAN fixture**: at least one test per inbound-data component where a PAN-like string appears in a non-card field; assert it is masked in all downstream output
- **Unknown-enum fallback**: at least one test per GW-facing component where an unknown `denial_reason` is received; assert fallback to `SECURITY_DECLINE` without exception
- **Idempotency test**: at least one test per write component verifying that a duplicate call produces the same result and no additional side effects
- **Fail-closed test**: at least one test per upstream-dependent component simulating timeout; assert `PENDING` verdict, never `ALLOW`
- **Acceptance criteria block**: every test class must have a top-level comment referencing the task (`T1`–`T14`) and listing which acceptance criteria it covers

---

## What Claude Code Must Not Do in This Project

- Generate code that writes raw PAN to any output stream
- Generate code that returns `denial_reason` in any HTTP response accessible to end users
- Generate code that auto-allows a transaction when an upstream dependency fails
- Generate code that uses `Double` or `Float` for monetary values
- Generate code that hardcodes score thresholds, vendor feed URLs, or API credentials
- Modify `TASKS.md` or `agents.md` (both are read-only per `AGENTS.md`)
- Add cross-subproject dependencies (RTAF is an isolated subproject)
- Generate code that allows an application role to update or delete audit log records
