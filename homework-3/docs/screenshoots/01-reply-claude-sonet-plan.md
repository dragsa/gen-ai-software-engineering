# Plan: RTAF Specification Package — homework-3

## Goal

Produce the full specification package (four deliverable files) for the **Real-Time Antifraud (RTAF)** system as required by `TASKS.md`. No code is produced; only documents.

---

## Agreed Design Decisions (from requirements clarification)

| Dimension | Decision |
|---|---|
| Decision model | **3-tier hybrid**: hard-block (vendor lists) → fraud-engine score → human review queue |
| Hard-block source | External vendor lists, ingested via **periodic batch sync** (cache-first) |
| Gateway verdict states | `ALLOW` / `DENY` / `PENDING` |
| Denial reason | RTAF returns an **internal enumerated reason** with every `DENY`; GW stores and logs it internally |
| User-facing denial message | GW maps internal reason to a **generic, non-revealing message** shown to the end user |
| Human review triggers | **Combination**: score in mid-band AND/OR high-value transaction AND/OR specific rule flags |
| Learning events emitted | All 4: `transaction_requested`, `decision_made`, `transaction_settled`, `chargeback_raised` |
| Latency SLO | To be **defined and justified** inside the spec as assumed FinTech targets |
| Scale target | **Medium** — 100–1 000 TPS |
| Compliance scope | PCI-DSS, GDPR, AML/KYC, immutable audit trail |

---

## Files to Produce

```
homework-3/
├── specification.md          ← primary graded artifact
├── agents.md                 ← AI agent guidelines
├── .claude/project.md        ← editor/AI rules (Claude Code format)
└── README.md                 ← student summary + rationale + best practices
```

---

## Phase 1 — `specification.md`

### 1.1 High-Level Objective
One sentence: RTAF's north star and scope boundary (what it is, what it is not).

### 1.2 System Context Diagram (textual)
Describe the four actors and their interaction surfaces:
- **Payment Gateway** → synchronous `POST /analyze` call → receives `ALLOW / DENY(reason) / PENDING`; maps internal reason to generic user message; surfaces that message to end user
- **End User** → receives only generic decline message from GW; never sees internal fraud signal
- **Self-Learning Fraud Engine** → queried by RTAF for score; receives lifecycle events
- **External Vendor Feed** → provides known-bad entity lists via batch sync

### 1.3 Mid-Level Objectives (observable, testable)
Seven objectives, each phrased as "what changes in the world when this succeeds":

| # | Objective | Verification signal |
|---|---|---|
| O1 | Transaction analysis pipeline processes a request end-to-end within SLO | Gateway receives verdict within latency budget |
| O2 | Vendor hard-block tier intercepts known-bad transactions before fraud engine is called | Blocked transactions never appear in fraud-engine query logs |
| O3 | Score-based auto-decision operates without human involvement for clear cases | Decision log records rule path and score for every auto-decision |
| O4 | Human review queue receives mid-confidence cases and drains within SLA | Queue depth and time-to-resolution are tracked |
| O5 | All four lifecycle events are emitted to the fraud engine reliably | Event audit log entries match transaction lifecycle 1:1 |
| O6 | Vendor lists are refreshed on schedule with staleness protection | Cache age metric stays within configured TTL bounds |
| O7 | System operates within stated compliance boundaries at all times | No PCI-sensitive data appears in logs, queues, or event payloads |
| O8 | Every `DENY` verdict carries an internal enumerated reason traceable by ops and audit | Each denied transaction has a reason code in the internal audit log |
| O9 | GW surfaces only generic, non-revealing decline messages to the end user | User-facing message contains no signal that could inform fraud pattern adaptation |

### 1.4 Non-Functional Requirements & Policy

Topics to specify:
- **Latency** — p50/p99 per decision tier (hard-block fastest, human-review excluded from SLO); label as assumed targets with FinTech justification
- **Throughput** — steady-state and burst capacity for 100–1 000 TPS
- **Availability** — payment-grade uptime; degraded-mode behaviour when fraud engine is unreachable
- **PCI-DSS** — data handling zones; no PAN/CVV outside encrypted stores; masking rules for logs
- **GDPR** — data minimisation on event payloads; retention policy; right-to-explanation obligation
- **AML/KYC** — velocity checks, watchlist screening, suspicious-activity escalation path
- **Audit trail** — append-only, tamper-evident log of every decision with full context snapshot

### 1.5 Implementation Notes (guardrails for builders)
- Idempotency key on `POST /analyze` (gateway retries must not produce duplicate events)
- All monetary amounts as `Decimal` / fixed-point; no floating point
- Error semantics: what RTAF returns when the fraud engine times out or is unavailable (fail-closed: return `PENDING`, enqueue for human review)
- Vendor list cache TTL, stale-list fallback behaviour
- Event ordering: at-least-once delivery guarantee; consumer-side dedup key on fraud engine
- No correlation ID that could leak PAN across services; use internal txn reference only
- `PENDING` response must include an opaque case ID the GW can poll or receive a webhook on
- **Denial reason contract**: `AnalyzeResponse` carries two denial fields:
  - `denial_reason` — internal enum (e.g. `VENDOR_BLOCKLIST`, `HIGH_FRAUD_SCORE`, `VELOCITY_LIMIT_EXCEEDED`, `AML_FLAG`, `PENDING_REVIEW`) — visible to GW internally, stored in GW audit log, never forwarded to user
  - `user_facing_category` — coarse enum (e.g. `SECURITY_DECLINE`, `TEMPORARY_UNAVAILABLE`) — what GW maps to a human-readable generic message
- **User-facing message rules (GW responsibility)**: messages must not name the specific fraud signal, must not vary in a way that lets a fraudster binary-search for the triggering condition; a single generic string per `user_facing_category` is the safe default
- **Reason taxonomy ownership**: RTAF owns the `denial_reason` enum; GW owns the `user_facing_category` → message mapping; neither mapping is exposed via any public API

### 1.6 Context

**Beginning context** (what exists before RTAF is built):
- Payment gateway with outbound webhook/API capability
- Fraud engine with scoring API and event ingestion endpoint
- External vendor with HTTP feed or SFTP batch export
- Infrastructure: message broker, secrets store, structured log sink

**Ending context** (what exists after RTAF is operational):
- RTAF service deployed with `/analyze` endpoint
- Vendor sync job running on schedule
- Human review queue populated and drained by ops tooling
- Event stream flowing to fraud engine on all four lifecycle hooks
- Audit log persisted and queryable

### 1.7 Low-Level Tasks (decomposed, with acceptance criteria)

Planned tasks (each will include: prompt, file/component, details, acceptance criteria):

| # | Task | Ties to |
|---|---|---|
| T1 | Define `AnalyzeRequest` / `AnalyzeResponse` data contracts (incl. `denial_reason` enum + `user_facing_category` enum + opaque `case_id` for PENDING) | O1, O8, O9 |
| T2 | Define internal `denial_reason` taxonomy (full enum, trigger condition, which tier produces it) | O8 |
| T3 | Define `user_facing_category` → generic message mapping owned by GW (rules: no signal leakage, single string per category) | O9 |
| T4 | Specify GW contract changes: new response fields consumed, message rendering rules, audit log extension for `denial_reason` | O8, O9 |
| T5 | Implement vendor list cache loader (batch sync, TTL, staleness check, fallback) | O6 |
| T6 | Implement hard-block lookup against vendor list cache → produces `VENDOR_BLOCKLIST` reason | O2, O8 |
| T7 | Implement fraud-engine scoring client (timeout, retry, circuit breaker, fallback) | O3 |
| T8 | Implement 3-tier decision router (hard-block → score thresholds → human queue) with reason assignment per path | O3, O4, O8 |
| T9 | Implement human review queue producer (enqueue with enriched context + metadata + reason = `PENDING_REVIEW`) | O4, O8 |
| T10 | Implement `transaction_requested` event emitter | O5 |
| T11 | Implement `decision_made` event emitter (verdict, score, `denial_reason`, rule path — internal only) | O5, O8 |
| T12 | Implement `transaction_settled` event consumer + forwarder | O5 |
| T13 | Implement `chargeback_raised` event consumer + forwarder | O5 |
| T14 | Implement audit log writer (append-only, structured, PAN-masked, includes `denial_reason`) | O7, O8 |
| T15 | Implement PCI data masking / stripping layer — ensure `denial_reason` never leaks PAN or card-derived signals | O7 |
| T16 | Implement AML velocity-check signal collector → produces `VELOCITY_LIMIT_EXCEEDED` / `AML_FLAG` reasons | O3, O4, O8 |
| T17 | Define latency instrumentation and SLO alerting hooks | O1 |
| T18 | Define GDPR retention cleanup job spec | O7 |

### 1.8 Edge Cases & Failure Modes (table)

Planned scenarios to cover:

- Fraud engine unavailable at check time (timeout / 5xx) → fail-closed to `PENDING`
- Vendor list expired / not refreshed within TTL → serve with staleness flag, alert ops
- Duplicate `POST /analyze` for same transaction ID → idempotent return, no new events
- Human reviewer queue depth breaches SLA threshold → auto-escalate, alert ops
- Score exactly on tier boundary → deterministic rule (lower boundary inclusive of allow tier), recorded in audit
- Concurrent vendor list refresh races with active lookup → atomic swap, no partial list evaluation
- PAN inadvertently included in event payload by upstream gateway → strip before processing, log sanitisation event
- AML watchlist lookup timeout → soft escalation to human review, reason = `AML_FLAG`
- Chargeback received for already reversed/voided transaction → idempotent accept, `duplicate_chargeback` flag
- Gateway retries after RTAF issued a decision → return original decision from idempotency cache, suppress re-emission
- **`denial_reason` accidentally exposed in GW error response body** → GW must never forward raw `denial_reason` to client-facing API; only `user_facing_category` message is permitted
- **New `denial_reason` enum value added by RTAF but not yet mapped in GW** → GW falls back to generic `SECURITY_DECLINE` message; no exception propagated to user; alert raised for ops to update mapping
- **User contacts support claiming unfair decline** → ops can look up `denial_reason` in internal audit log; user is given only the generic message; GDPR right-to-explanation is satisfied via a compliance-reviewed template, not raw reason codes

### 1.9 Verification Section
Per mid-level objective: unit / integration / e2e check categories, data fixtures needed, reconciliation checks, manual compliance review steps.

### 1.10 Performance Targets
Assumed targets with FinTech justification, per tier:

| Tier | p50 target | p99 target | Justification |
|---|---|---|---|
| Hard-block (cache hit) | < 5 ms | < 20 ms | In-memory lookup only |
| Score-based (engine call) | < 80 ms | < 250 ms | E-commerce CNP budget |
| Full pipeline (all checks) | < 120 ms | < 300 ms | ISO 8583 auth timeout window |
| Human review enqueue | < 10 ms | < 50 ms | Fire-and-forget, async |

---

## Phase 2 — `agents.md`

Sections to include:
- Tech stack assumptions (JVM / Kotlin or Python service; message broker; structured logging)
- Domain rules (never log PAN/CVV, always idempotent writes, fail-closed default for unknown errors)
- Testing expectations (unit per component, integration per tier, contract test for fraud engine API)
- Security/compliance constraints (PCI zone boundaries, GDPR minimisation, AML escalation path)
- Edge-case handling rules (explicit: what to do when upstream is slow, list is stale, score is on boundary)
- Prohibited patterns (floating-point money, synchronous human-review blocking the gateway response)

---

## Phase 3 — `.claude/project.md` (Editor/AI Rules)

Sections to include:
- Project context and domain sensitivity
- Naming conventions (events, services, fields)
- What to avoid (no raw PAN in any generated output, no float for money)
- FinTech-sensitive defaults (fail-closed, audit-first, masked logging)
- Patterns to prefer (idempotency keys, structured errors, contract-first API)
- Testing defaults (always generate acceptance criteria, always consider the boundary-value case)

---

## Phase 4 — `README.md`

Sections:
- Student name + task summary
- Rationale: why this scope, how performance targets were chosen, how verification depth was decided
- Industry best practices: list each practice and where it appears (file + section reference)

---

## Execution Order

1. `specification.md` — write section by section (1.1 → 1.10)
2. `agents.md` — derive rules from spec decisions already made
3. `.claude/project.md` — derive editor rules from agents.md and spec patterns
4. `README.md` — write last; rationale section references completed spec

---

## Risks & Edge Cases for the Plan Itself

- **Scope creep on low-level tasks**: keep each task to a single component / concern; resist bundling
- **Latency numbers**: label all as "assumed targets" — do not present as benchmarked
- **Compliance depth**: PCI-DSS, GDPR, AML are first-class sections, not footnotes
- **Template trap**: the provided template is the minimum bar; spec must exceed it meaningfully
- **Human review workflow**: this is a side-channel, not a synchronous part of the gateway flow — must be clearly spec'd as async
- **Denial reason leakage**: the two-field design (`denial_reason` internal + `user_facing_category` external) must be enforced as a contract boundary — spec must state explicitly that `denial_reason` must never appear in any client-facing payload, log shipped externally, or event forwarded to the fraud engine without scrubbing
- **Enum versioning**: `denial_reason` will grow over time; spec must state that GW must handle unknown values gracefully (fallback mapping) to avoid breaking changes

---

*Awaiting approval before writing any specification content.*
