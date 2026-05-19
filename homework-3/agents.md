# RTAF Agent Guidelines

This file defines the rules, constraints, and conventions that govern any AI agent working on the Real-Time Antifraud (RTAF) system. All rules in this file are non-negotiable unless explicitly overridden by a task instruction that references this file and states the override reason.

---

## 1. Tech Stack Assumptions

| Layer | Technology |
|---|---|
| Primary language | Kotlin (JVM 17+) |
| Build tool | Gradle with Kotlin DSL |
| HTTP framework | Ktor or Spring Boot (whichever is established in the project) |
| Message broker | Kafka (or RabbitMQ if specified in project config) |
| Audit / structured logging | Logback with JSON encoder; log sink is Elasticsearch or equivalent |
| Secrets | HashiCorp Vault or equivalent; never hardcoded, never in environment variables in plain text |
| Metrics | Micrometer with Prometheus scrape endpoint |
| Testing | JUnit 5, MockK for mocking, Testcontainers for integration |
| Money types | `java.math.BigDecimal` or a dedicated `Money` value class; never `Double` or `Float` |

If a task `TASKS.md` mandates a different language or runtime, that mandate takes precedence over this file for that specific subproject.

---

## 2. Domain Rules

These rules apply to all code, configuration, documentation, and test artifacts produced for RTAF.

### 2.1 PCI-DSS Data Handling

- **Never log, print, emit, or store a raw PAN, CVV, full magnetic stripe data, or PIN** in any form — log lines, event payloads, queue messages, API responses, test fixtures, or inline comments.
- Truncated PAN (last 4 digits) is permitted only where explicitly required for deduplication or display. Use the format `****-****-****-XXXX`.
- Always apply the `PciSanitizationLayer` before any data leaves the RTAF trust boundary (log emission, event publishing, queue production, API response serialisation).
- If a task requires a test fixture that resembles a PAN, use a clearly fake value (e.g. `4111-1111-1111-1111` — a well-known test PAN) and annotate it with `// TEST_PAN — not real`.

### 2.2 Monetary Amounts

- All monetary amounts are represented as **long integers in minor currency units** in API contracts and as `BigDecimal` internally.
- `Double`, `Float`, and `float` are prohibited for any monetary field at every layer.
- Currency is always an ISO 4217 three-letter code.

### 2.3 Idempotency

- Every write operation that can be triggered by an external call must be idempotent.
- `POST /analyze` uses the `idempotency_key` header. If the same key is received again, return the cached response without re-running analysis or emitting events.
- Queue producers must deduplicate on `transaction_id`. Enqueueing the same `transaction_id` twice must not create two queue entries.
- Event emitters must deduplicate on `transaction_id` + `event_type`. The same event type for the same transaction must be emitted exactly once.

### 2.4 Fail-Closed

- When an upstream dependency is unavailable (fraud engine timeout, message broker down, vendor list unreachable), **RTAF must fail closed** — the default response is `PENDING`, not `ALLOW`.
- Auto-allow on upstream failure is prohibited.
- Any fail-closed decision must be recorded in the audit log with the failure reason.

### 2.5 `denial_reason` Isolation

- `denial_reason` is an internal enum. It is permitted in:
  - `AnalyzeResponse` to the GW (the GW must not forward it to end users)
  - Audit log records
  - `decision_made` event to the fraud engine
  - Human review queue messages
- `denial_reason` is **prohibited** in:
  - Any HTTP response body, header, or error payload returned to end users or public API consumers
  - Any externally shipped log (log aggregation pipelines to external vendors)
  - Any field name or value accessible via a public API endpoint other than the internal support lookup

### 2.6 Audit Before All Else

- Write the audit record before returning the response to the GW. If the audit write fails, treat it as a critical error: do not return `ALLOW` without an audit record. Return `PENDING` and raise a severity-1 alert.
- The audit log is append-only. No update or delete operation is permitted from application code.

---

## 3. Testing and Verification Expectations

### 3.1 Required test coverage per component

| Component | Unit tests | Integration tests | Contract tests |
|---|---|---|---|
| `HardBlockChecker` | All entity-type lookups; stale cache path | Cache loader + checker end-to-end | — |
| `FraudEngineScoringClient` | Timeout, retry, circuit breaker, fallback | Mock fraud engine (Testcontainers or WireMock) | Fraud engine API contract (consumer-driven) |
| `DecisionRouter` | All six `denial_reason` paths; boundary values at both thresholds | End-to-end `POST /analyze` for all verdict types | — |
| `HumanReviewQueueProducer` | Idempotency; SLA timer; message schema | Queue consumer mock | — |
| Event emitters (T8–T11) | Correct hook triggers; dedup | Fraud engine mock captures all events | Fraud engine event ingestion contract |
| `AuditLogWriter` | Record schema; no PAN in output | Append-only guarantee; no update path exposed | — |
| `PciSanitizationLayer` | PAN pattern detection and masking; CVV stripping; all outbound surfaces | Applied in pipeline before every outbound emission | — |
| GW contract (T2) | Fallback to `SECURITY_DECLINE` for unknown `denial_reason` | GW mock renders correct user message per category | RTAF ↔ GW contract (provider-driven) |

### 3.2 Acceptance criteria are required

Every low-level task must end with a checkable acceptance-criteria block. An agent must not mark a task complete unless all acceptance criteria are verifiably satisfied.

### 3.3 Boundary-value testing is mandatory

For any component that applies a numeric threshold (score tiers, velocity windows, retry counts, TTL values), tests must cover: value = threshold − 1, value = threshold, value = threshold + 1.

### 3.4 Adversarial fixture requirement

For every component that handles inbound data from the GW, at least one test fixture must contain a PAN-like value in a non-card field (e.g. a merchant name that happens to be a 16-digit string). The test must assert the value is masked in all downstream records.

---

## 4. Security and Compliance Constraints

### 4.1 PCI zone boundary

- RTAF must never call any external endpoint with a raw PAN in the request body or URL.
- The `PciSanitizationLayer` must be the last step before serialisation; no component may bypass it.
- Secrets (vendor feed credentials, fraud engine API keys) are fetched from Vault at startup and rotated without service restart.

### 4.2 GDPR data minimisation

- Event payloads to the fraud engine contain only: `transaction_id`, `amount`, `currency`, `merchant_id`, `account_id` (hashed/tokenised), `device_fingerprint` (opaque token), `ip_address`. No name, address, date of birth, or email.
- The right-to-explanation response template is static and Compliance-approved. Agents must not generate dynamic explanations that reference `denial_reason` or score values.

### 4.3 AML signal scope

- AML contribution to RTAF is limited to two signals: velocity check (transaction count and volume over rolling windows) and watchlist match (boolean).
- An agent must not expand AML scope to SAR generation, entity resolution, network analysis, or ongoing monitoring without explicit task instruction referencing this file.

### 4.4 Permission boundaries

- Agents must not generate code that allows any application role to modify or delete audit log records.
- Agents must not generate a public API endpoint that returns `denial_reason` to any caller other than the internal support lookup (which must be authenticated and return a redacted view).
- The human review queue must not be readable by end users or GW-facing services.

---

## 5. Edge-Case Handling Rules

| Situation | Required behaviour |
|---|---|
| Upstream slow or unavailable | Fail closed → `PENDING`; never `ALLOW` on failure |
| Vendor list stale beyond TTL | Serve last known list; set `cache_stale = true`; alert ops; never skip the hard-block check |
| Score exactly on threshold boundary | Lower boundary is inclusive of the allow tier (score = `AUTO_ALLOW_THRESHOLD` → `ALLOW`); this rule is deterministic and must not be changed without a plan update |
| Unknown `denial_reason` value received by GW | Silent fallback to `SECURITY_DECLINE`; raise `UNKNOWN_DENIAL_REASON` ops alert; no exception to user |
| Duplicate inbound request (same idempotency key) | Return cached response; emit no new events; do not re-run analysis |
| Chargeback for reversed/voided transaction | Forward with `is_duplicate = true`; do not double-count |
| Audit write fails | Do not return `ALLOW`; return `PENDING`; raise severity-1 alert |
| PAN detected in inbound payload | Strip and mask; log a sanitisation event; do not propagate |

---

## 6. Prohibited Patterns

The following patterns must never appear in any RTAF code, configuration, or generated output:

- `Double` or `Float` for any monetary field
- Hardcoded score threshold values (use config)
- Hardcoded vendor feed URL or credentials (use secrets store)
- `denial_reason` in any HTTP response accessible by end users or public API consumers
- Auto-allow as a fallback for any upstream failure
- Synchronous human-review resolution blocking the `POST /analyze` response
- Any log statement that could emit a raw PAN (use `PciSanitizationLayer` or log only masked/tokenised references)
- Unbounded retry loops (all retries must have a max count and backoff)
- Shared mutable state in event emitters (emitters must be stateless and thread-safe)
