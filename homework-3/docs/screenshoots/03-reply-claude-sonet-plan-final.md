# Plan: RTAF Specification Package — homework-3

## Goal

Produce the full specification package (four deliverable files) for the **Real-Time Antifraud (RTAF)** system as required by `TASKS.md`. No code is produced; only documents.

---

## Agreed Design Decisions

| Dimension | Decision |
|---|---|
| Decision model | **3-tier hybrid**: hard-block (vendor lists) → fraud-engine score → human review queue |
| Hard-block source | External vendor lists, ingested via **periodic batch sync** (cache-first, TTL-governed) |
| Gateway verdict states | `ALLOW` / `DENY` / `PENDING` |
| Denial reason | RTAF returns an internal enumerated `denial_reason` with every `DENY` |
| User-facing message | GW maps `denial_reason` → `user_facing_category` → generic string; never exposes internal reason to user |
| Human review triggers | **Combination**: mid-band score AND/OR high-value transaction AND/OR specific rule flags |
| Human review path | **Async** — GW receives `PENDING` + opaque `case_id` immediately; human resolution posted back later |
| Learning events emitted | `transaction_requested`, `decision_made`, `transaction_settled`, `chargeback_raised` |
| Latency SLO | Defined inside spec as assumed FinTech targets with justification |
| Scale target | Medium — 100–1 000 TPS |
| Compliance scope | PCI-DSS, GDPR, AML/KYC (minimal signals only), immutable audit trail |

---

## Files to Produce

```
homework-3/
├── specification.md
├── agents.md
├── .claude/project.md
└── README.md
```

---

## Phase 1 — `specification.md`

### Section 1.1 — High-Level Objective
One sentence: RTAF's north star and scope boundary (what it does and explicitly what it does not).

---

### Section 1.2 — Out of Scope

Explicit block to prevent scope creep and aid grading clarity. Will state that the following are **not** in scope for this specification:

- Implementation of the Payment Gateway itself (GW is an existing system; spec only covers RTAF-facing contract changes required of GW)
- Implementation of the Self-Learning Fraud Engine (treated as a black-box dependency with a defined API contract)
- Full AML/KYC compliance programme (only velocity-check and watchlist-lookup signals are included as lightweight inputs to the scoring tier)
- Human review tooling UI (spec covers the queue producer contract and SLA; the ops UI is out of scope)
- Card scheme rules and dispute resolution workflows beyond chargeback event forwarding

---

### Section 1.3 — Stakeholders

Four stakeholders, each with what they need to **observe** and what they need to **control**:

| Stakeholder | Needs to observe | Needs to control |
|---|---|---|
| **End User** | Generic decline message (no fraud signal detail) | Nothing — read-only consumer of GW response |
| **Ops** | Real-time decision throughput, queue depth, vendor list staleness, error rates | Human review queue drain; vendor sync schedule; tier threshold config |
| **Compliance** | Full immutable audit log per transaction (verdict, `denial_reason`, rule path, score, timestamps); GDPR retention state | Retention policy parameters; right-to-explanation response templates |
| **Support** | Internal `denial_reason` for a specific transaction (lookup by transaction ID); case status for `PENDING` decisions | Read-only lookup; cannot modify decisions or reason codes |

---

### Section 1.4 — System Context (textual)

Four actors and their interaction surfaces:

- **Payment Gateway** → synchronous `POST /analyze` → receives `ALLOW / DENY(denial_reason, user_facing_category) / PENDING(case_id)`; maps to generic user message; stores `denial_reason` in internal audit log only
- **End User** → receives only generic decline message from GW; never sees `denial_reason` or fraud signals
- **Self-Learning Fraud Engine** → queried by RTAF for risk score; receives all four lifecycle events
- **External Vendor Feed** → provides known-bad entity lists via scheduled batch sync (justified in scope: hard-block tier depends on it; without it the spec cannot describe the first decision tier)

---

### Section 1.5 — Mid-Level Objectives (9, observable and testable)

| # | Objective | Verification signal |
|---|---|---|
| O1 | Transaction analysis pipeline processes a request end-to-end within SLO | GW receives verdict within latency budget |
| O2 | Vendor hard-block tier intercepts known-bad transactions before fraud engine is called | Blocked transactions never appear in fraud-engine query logs |
| O3 | Score-based auto-decision operates without human involvement for clear cases | Decision log records rule path and score for every auto-decision |
| O4 | Human review queue receives mid-confidence cases and drains within SLA | Queue depth and time-to-resolution are tracked |
| O5 | All four lifecycle events are emitted to the fraud engine reliably | Event audit log entries match transaction lifecycle 1:1 |
| O6 | Vendor lists are refreshed on schedule with staleness protection | Cache age metric stays within configured TTL bounds |
| O7 | System operates within stated compliance boundaries at all times | No PCI-sensitive data appears in logs, queues, or event payloads |
| O8 | Every `DENY` verdict carries an internal enumerated reason traceable by ops and support | Each denied transaction has a `denial_reason` in the internal audit log |
| O9 | GW surfaces only generic, non-revealing decline messages to the end user | User-facing message contains no signal that could inform fraud pattern adaptation |

---

### Section 1.6 — Permission Boundaries

Explicit access control table for sensitive internals, required by the regulated-environment constraint in `TASKS.md`:

| Resource | Who can read | Who can write / modify | Notes |
|---|---|---|---|
| `denial_reason` field | GW internal systems, Ops, Support (by txn ID lookup), Compliance | RTAF only (set at decision time, immutable after) | Must never appear in client-facing API responses or externally shipped logs |
| `user_facing_category` → message mapping | GW application code, Compliance | GW owners + Compliance sign-off | Mapping changes require compliance review; unknown enum values fall back to `SECURITY_DECLINE` |
| Human review queue | Ops (read + drain), RTAF (enqueue) | RTAF (produce), Ops (resolve) | Support has no queue access; decisions posted back via internal webhook only |
| Audit log | Compliance (full), Ops (operational fields), Support (per-txn lookup with redaction) | Append-only by RTAF; no modification permitted by any role | Retention governed by GDPR policy; deletion only via automated compliance job |
| Fraud engine event stream | Fraud Engine (consumer), RTAF (producer) | RTAF only | `denial_reason` must be scrubbed from any event payload before forwarding; only verdict category permitted |
| Vendor list cache | RTAF read-only at runtime | Vendor sync job only | No manual override permitted; stale list triggers alert, not emergency write |

---

### Section 1.7 — Non-Functional Requirements & Policy

- **Latency** — p50/p99 targets per decision tier; labeled as assumed targets with FinTech justification (ISO 8583 auth timeout, e-commerce CNP budget)
- **Throughput** — steady-state and burst capacity for 100–1 000 TPS
- **Availability** — payment-grade uptime; degraded-mode behaviour when fraud engine is unreachable (fail-closed: `PENDING`)
- **PCI-DSS** — data handling zones; no PAN/CVV outside encrypted stores; masking rules for all log output
- **GDPR** — data minimisation on event payloads; retention policy; right-to-explanation obligation satisfied via compliance-reviewed generic templates
- **AML/KYC (minimal)** — velocity-check signals and watchlist-match flags fed as inputs to the score tier only; no full SAR filing workflow in scope
- **Audit trail** — append-only, tamper-evident log of every decision with full context snapshot

---

### Section 1.8 — Implementation Notes (guardrails for builders)

- Idempotency key on `POST /analyze` — gateway retries must not produce duplicate events
- All monetary amounts as `Decimal` / fixed-point; floating point is prohibited
- Error semantics — fraud engine timeout or 5xx: fail-closed, return `PENDING`, enqueue for human review
- Vendor list cache TTL and stale-list fallback behaviour (serve stale + alert; do not block on staleness alone)
- Event delivery: at-least-once guarantee; consumer-side dedup key on fraud engine
- No correlation ID may leak PAN across services; internal transaction reference only
- `PENDING` response includes opaque `case_id`; GW polls or receives webhook on resolution
- **Denial reason contract**: `AnalyzeResponse` carries:
  - `denial_reason` — internal enum (e.g. `VENDOR_BLOCKLIST`, `HIGH_FRAUD_SCORE`, `VELOCITY_LIMIT_EXCEEDED`, `AML_FLAG`, `PENDING_REVIEW`) — for GW internal use and audit only
  - `user_facing_category` — coarse enum (e.g. `SECURITY_DECLINE`, `TEMPORARY_UNAVAILABLE`) — GW maps to a single static generic string per category
- **User-facing message rule**: one static string per `user_facing_category`; must not vary by sub-reason; unknown `denial_reason` values fall back to `SECURITY_DECLINE` without exception propagation
- **Reason taxonomy ownership**: RTAF owns `denial_reason` enum; GW owns the `user_facing_category` → message mapping; neither mapping is exposed via any public API
- **Enum versioning**: GW must handle unknown `denial_reason` values gracefully; ops alerted to update mapping

---

### Section 1.9 — Context

**Beginning context** (what exists before RTAF is built):
- Payment gateway with outbound API/webhook capability
- Fraud engine with scoring API and event ingestion endpoint
- External vendor with HTTP or SFTP batch export
- Infrastructure: message broker, secrets store, structured log sink

**Ending context** (what exists after RTAF is operational):
- RTAF service with `/analyze` endpoint live
- Vendor sync job running on schedule
- Human review queue populated and drained via ops tooling
- Event stream flowing to fraud engine on all four lifecycle hooks
- Audit log persisted and queryable
- GW updated to consume new response fields and render generic user messages

---

### Section 1.10 — Low-Level Tasks

**Task shape** (every task in the final spec follows this template exactly):

```
### T[N] — [Task name]

Prompt: [What to instruct an AI agent to do]
Target file / component: [Specific file path or service component]
Details: [Requirements, constraints, data shapes, rules]
Acceptance criteria: [Checkable definition of done]
```

**Task list (14 tasks):**

| # | Task | Ties to |
|---|---|---|
| T1 | Define `AnalyzeRequest` / `AnalyzeResponse` data contracts (verdict states, `denial_reason` enum, `user_facing_category` enum, `case_id`) | O1, O8, O9 |
| T2 | Decision contract + GW mapping group: define full `denial_reason` taxonomy; define `user_facing_category` → generic message mapping with leakage-prevention rules; specify GW contract changes (new fields consumed, rendering rules, GW audit log extension) | O8, O9 |
| T3 | Implement vendor list cache loader (batch sync, TTL, staleness check, atomic swap, fallback) | O6 |
| T4 | Implement hard-block lookup against vendor list cache → produces `VENDOR_BLOCKLIST` reason | O2, O8 |
| T5 | Implement fraud-engine scoring client (timeout, retry, circuit breaker, fail-closed fallback) | O3 |
| T6 | Implement 3-tier decision router (hard-block → score thresholds → human queue) with `denial_reason` assigned per path | O3, O4, O8 |
| T7 | Implement human review queue producer (enqueue with enriched context, `case_id`, reason = `PENDING_REVIEW`) | O4, O8 |
| T8 | Implement `transaction_requested` event emitter | O5 |
| T9 | Implement `decision_made` event emitter (verdict, score, `denial_reason`, rule path — internal channel only; `denial_reason` scrubbed from fraud-engine payload) | O5, O8 |
| T10 | Implement `transaction_settled` event consumer + forwarder to fraud engine | O5 |
| T11 | Implement `chargeback_raised` event consumer + forwarder to fraud engine | O5 |
| T12 | Audit logging + PCI sanitization group: implement append-only structured audit log writer (includes `denial_reason`); implement PCI data masking / stripping layer for all outbound payloads; verify `denial_reason` never leaks PAN-derived signals | O7, O8 |
| T13 | Latency instrumentation and SLO alerting *(NFR/compliance task)* — define per-tier timing hooks, SLO breach alerts, p50/p99 dashboards | O1 |
| T14 | GDPR retention cleanup policy *(NFR/compliance task)* — define what data is retained, for how long, automated deletion job spec, right-to-explanation template | O7 |

---

### Section 1.11 — Edge Cases & Failure Modes

| Scenario | Expected behaviour | Compliance implication |
|---|---|---|
| Fraud engine unavailable at check time | Fail-closed: return `PENDING` + `case_id`, enqueue for human review, emit `decision_made` with reason = `PENDING_REVIEW` | Audit log must record fallback path and timestamp |
| Vendor list not refreshed within TTL | Serve stale list with staleness flag; alert ops; do not block transactions on staleness alone | Log staleness duration; compliance alert if beyond policy threshold |
| Duplicate `POST /analyze` for same transaction ID | Idempotent: return cached decision; emit no additional events | Prevents double-counting in fraud engine learning |
| Human review queue depth breaches SLA | Alert ops; auto-escalate oldest cases; do not downgrade to auto-allow | AML: unreviewed cases must not silently expire |
| Score exactly on tier boundary | Deterministic rule: lower boundary inclusive of allow tier; recorded in audit log | Rule path logged; no ambiguity permitted |
| Concurrent vendor list refresh races with active lookup | Atomic swap; no transaction evaluated against partial list | No compliance gap from partial hard-block state |
| PAN inadvertently included in event payload by upstream GW | Strip and mask before any processing; log sanitisation event; do not forward | PCI-DSS: must never propagate downstream |
| AML watchlist lookup timeout | Soft escalation to human review; reason = `AML_FLAG` | Escalation and timeout duration recorded in audit |
| Chargeback received for already reversed/voided transaction | Idempotent accept; emit with `duplicate_chargeback` flag | Prevents double-counting in fraud model training |
| GW retry after RTAF issued a decision | Return original decision from idempotency cache; suppress event re-emission | Audit log preserves original decision timestamp |
| `denial_reason` accidentally exposed in GW error response body | GW must never forward raw `denial_reason`; only `user_facing_category` message permitted | If leaked, constitutes a compliance incident; must be logged and reported |
| New `denial_reason` value added by RTAF, not yet mapped in GW | GW falls back to `SECURITY_DECLINE`; no exception to user; ops alerted to update mapping | No service disruption; mapping gap tracked |
| User contacts support claiming unfair decline | Ops looks up `denial_reason` in internal audit log; user receives only GDPR-compliant generic explanation template | Right-to-explanation satisfied without revealing fraud signal |

---

### Section 1.12 — Objective-to-Verification Matrix

Full matrix mapping each objective to exact verification approach:

| Obj | Unit checks | Integration checks | Fixtures / reconciliation | Manual compliance step |
|---|---|---|---|---|
| O1 | Decision router returns verdict for all input combinations | End-to-end `POST /analyze` → GW response under load | Synthetic transactions covering all three tiers | Latency report reviewed against assumed SLO targets |
| O2 | Hard-block lookup returns `DENY` for all vendor-listed entities | Blocked transaction does not appear in fraud-engine request log | Vendor list fixture with known-bad entries | Confirm no fraud-engine call logged for blocked txn |
| O3 | Score thresholds produce correct auto-allow / auto-deny for boundary values | Fraud-engine client integration: score response mapped to correct decision | Fixture: scores at boundary -1, boundary, boundary +1 | Spot-check decision log for rule path accuracy |
| O4 | Queue producer enqueues correct metadata for mid-confidence transactions | Queue depth observable; resolution webhook received by GW | Fixture: transactions that hit all three review triggers simultaneously | Ops confirms queue drains within SLA in staging |
| O5 | Each event emitter fires on correct lifecycle hook | Four event types visible in fraud-engine ingestion log | Full transaction lifecycle fixture (requested → decided → settled → chargeback) | Reconciliation: event count per txn ID = 4 in audit log |
| O6 | Cache loader respects TTL; staleness flag set correctly | Sync job runs on schedule; cache age metric updated | Fixture: expired list triggers staleness alert | Ops confirms staleness alert fires within 1 cycle of TTL breach |
| O7 | PCI masking layer strips PAN/CVV from all outbound payloads | No PAN present in audit log, event stream, or GW response payload | Fixture: request with PAN present; assert masked in all downstream records | Compliance officer reviews audit log sample; confirms no raw PAN |
| O8 | Every `DENY` response carries a non-null `denial_reason` matching defined enum | `denial_reason` present in GW internal audit log for all denied transactions | Fixture: one transaction triggering each known `denial_reason` value | Support team lookup test: retrieve `denial_reason` by transaction ID |
| O9 | `user_facing_category` maps to a static generic string; same string for all reasons in category | GW response body contains only `user_facing_category` message; no `denial_reason` field present | Fixture: two different `denial_reason` values in same category → same user message | Compliance review: confirm message wording reveals no actionable fraud signal |

---

### Section 1.13 — Performance Targets (assumed, labeled)

| Tier | p50 | p99 | Justification |
|---|---|---|---|
| Hard-block (cache hit) | < 5 ms | < 20 ms | In-memory lookup only |
| Score-based (fraud engine call) | < 80 ms | < 250 ms | E-commerce CNP latency budget |
| Full pipeline (all checks) | < 120 ms | < 300 ms | ISO 8583 auth timeout window |
| PENDING enqueue (async) | < 10 ms | < 50 ms | Fire-and-forget; excluded from gateway SLO |
| Vendor list refresh (end-to-end) | — | < 60 s | Staleness SLA for known-bad list currency |

---

## Phase 2 — `agents.md`

- Tech stack assumptions
- Domain rules (never log PAN/CVV, always idempotent writes, fail-closed on unknown errors, `denial_reason` never in external payloads)
- Testing expectations (unit per component, integration per tier, contract test for fraud engine API, GW contract regression test)
- Security/compliance constraints (PCI zone boundaries, GDPR minimisation, permission boundaries per Section 1.6)
- Edge-case handling rules (slow upstream → `PENDING`; stale list → serve + alert; boundary score → deterministic lower-inclusive rule)
- Prohibited patterns (float money, synchronous human-review blocking gateway response, `denial_reason` in any client-visible field)

---

## Phase 3 — `.claude/project.md`

- Project context and domain sensitivity statement
- Naming conventions (`transaction_requested` not `txnReq`; `denial_reason` not `declineCode`)
- What to avoid (raw PAN in any generated output, float for money, optimistic defaults in failure paths, `denial_reason` in external payloads)
- FinTech-sensitive defaults (fail-closed, audit-first, masked logging)
- Patterns to prefer (idempotency keys, structured errors with machine-readable codes, contract-first API design)
- Testing defaults (always generate acceptance criteria; always consider boundary-value case; always include a fixture for the unknown-enum fallback path)

---

## Phase 4 — `README.md`

- Student name + task summary
- Rationale: scope choices, performance target justification, verification depth, vendor feed inclusion justification
- Industry best practices: each practice named with file + section reference

---

## Execution Order

1. `specification.md` — sections 1.1 → 1.13
2. `agents.md` — derived from spec decisions
3. `.claude/project.md` — derived from agents.md and spec patterns
4. `README.md` — written last; rationale references completed spec

---

## Risks

- **Latency numbers** — labeled as assumed throughout; no false precision
- **Template trap** — provided template is the floor; spec must substantially exceed it
- **Human review async model** — must be unambiguous: GW receives `PENDING` + `case_id` synchronously; human resolution is a separate async callback
- **Denial reason leakage** — `denial_reason` / `user_facing_category` contract boundary enforced at every layer; spec must state this explicitly for each outbound surface
- **Enum versioning** — GW must handle unknown `denial_reason` values gracefully; spec must state the fallback rule to prevent breaking changes when RTAF adds new reason codes
- **AML scope** — kept to velocity-check signals and watchlist-match flags only; no SAR filing, no full KYC programme; any expansion requires explicit approval

---

*Awaiting approval before writing any specification content.*
