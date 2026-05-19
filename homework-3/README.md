# Homework 3 — Real-Time Antifraud (RTAF) System Specification

## Author

**Andrii Gnatiuk**
Email: to.gnatuk@gmail.com

---

## Task Summary

This submission designs a specification package for a **Real-Time Antifraud (RTAF)** system — a decision and event-routing layer that sits between an existing Payment Gateway and a Self-Learning Fraud Engine. The system intercepts incoming transactions, delivers a low-latency `ALLOW / DENY / PENDING` verdict through a three-tier hybrid decision model, and feeds lifecycle events back to the fraud engine for continuous learning.

No code was written. The deliverables are:

| File | Purpose |
|---|---|
| `specification.md` | Full layered spec: objectives, stakeholders, permission boundaries, 14 low-level tasks, edge cases, verification matrix, performance targets |
| `agents.md` | AI agent guidelines: domain rules, testing expectations, security constraints, prohibited patterns |
| `.claude/project.md` | Claude Code editor rules: naming conventions, FinTech-sensitive defaults, testing defaults, prohibited codegen patterns |
| `README.md` | This file — student info, rationale, and industry practice references |

---

## Rationale

### Why RTAF as the chosen domain

RTAF sits at the intersection of the three compliance concerns the homework requires as first-class concerns — PCI-DSS (card data), GDPR (right-to-explanation, data minimisation), and AML/KYC (velocity and watchlist signals). A virtual card lifecycle spec would have addressed PCI well but would have required artificial scope to introduce the other two. RTAF makes all three necessary by design.

### Why the three-tier hybrid decision model

A single-threshold score model cannot satisfy the dual constraint of maximising conversion (the business goal stated in requirements) and minimising fraud. The three-tier structure — hard-block for known-bad entities, score-based auto-decision for high-confidence cases, and human review for the uncertain middle — reflects standard industry practice at mid-tier processors. It also gives the spec natural layers to decompose into low-level tasks with clear acceptance criteria per tier.

### Why `DENY` carries both an internal `denial_reason` and a `user_facing_category`

A single denial code that is both operationally useful and user-safe is not achievable. Ops and compliance need precise signal; end users (and therefore fraudsters) must not receive it. The two-field design enforces the boundary in the contract itself rather than relying on runtime filtering. The unknown-value fallback rule (`SECURITY_DECLINE` on unknown enum) makes the contract forward-compatible without breaking the boundary.

### How performance targets were chosen

All targets are labeled as assumed. They are justified against two industry reference points:
- **ISO 8583** auth response timeout (commonly 500 ms end-to-end): RTAF's full-pipeline p99 budget (300 ms) leaves headroom for GW network latency.
- **E-commerce CNP norms**: fraud checks for card-not-present transactions are expected to complete in under 300 ms to avoid cart abandonment.
The hard-block tier (< 20 ms p99) reflects in-memory lookup characteristics; the PENDING enqueue is fire-and-forget and is explicitly excluded from the gateway SLO.

### How verification depth was decided

The homework requires verification to be first-class, not a footnote. Each of the nine mid-level objectives has a distinct verification signal in the objectives table (Section 5), a dedicated row in the objective-to-verification matrix (Section 12), and every low-level task has a checkable acceptance-criteria block. The matrix explicitly separates unit, integration, fixture/reconciliation, and manual compliance steps because these have different owners (developer, QA, and compliance officer respectively) and different execution cadences.

### Why AML is kept minimal

Full AML — SAR filing, entity resolution, ongoing monitoring — is a programme-level concern that would dwarf the rest of the spec and introduce regulatory specificity (jurisdiction-dependent thresholds, reporting timelines) that is out of scope for a homework specification. Two lightweight signals (velocity check, watchlist match) are sufficient to make AML a first-class input to the scoring tier and demonstrate the compliance concern without expanding the scope to a full AML platform.

### Why the External Vendor Feed is in scope

The hard-block tier — which is the first and fastest decision path — cannot be specified without describing where the block lists come from and how they are kept current. The vendor feed is therefore a necessary dependency of the specification, even though RTAF does not implement the vendor's side. The out-of-scope block (Section 2) makes this boundary explicit.

---

## Industry Best Practices

The following industry practices are encoded in the specification. Each entry identifies where it appears.

| Practice | Where it appears |
|---|---|
| **Fail-closed on upstream failure** — in payment systems, an unknown risk is treated as high risk; auto-allow on dependency failure is prohibited | `specification.md` §8 Implementation Notes; `agents.md` §2.4; `.claude/project.md` FinTech-Sensitive Defaults |
| **PCI-DSS data zone separation** — PAN/CVV never leave the encrypted zone; masking applied as a cross-cutting concern before every outbound emission | `specification.md` §7.4, T12; `agents.md` §2.1, §4.1; `.claude/project.md` "What to Avoid" |
| **Idempotency keys on all externally-triggered writes** — prevents duplicate decisions, double-counted events, and duplicate queue entries from GW retries | `specification.md` §8, T1, T7–T11; `agents.md` §2.3; `.claude/project.md` FinTech-Sensitive Defaults |
| **Audit-before-response** — audit record is written before returning the verdict; failure to audit blocks `ALLOW` | `specification.md` §8; `agents.md` §2.6 |
| **Internal reason / generic user message separation** — ops gets precise signal; end users and fraudsters get a static generic message; two-field contract enforces the boundary | `specification.md` §8 denial reason contract, T2, §11 edge cases; `agents.md` §2.5; `.claude/project.md` "What to Avoid" |
| **Append-only tamper-evident audit log** — financial services requirement; no application role may modify or delete records | `specification.md` §7.7, T12, §6 Permission Boundaries; `agents.md` §2.6, §4.4 |
| **GDPR data minimisation on event payloads** — fraud engine receives only the minimum fields needed for scoring; no name, address, or PAN | `specification.md` §7.5; `agents.md` §4.2 |
| **Right-to-explanation via static compliance-approved template** — GDPR Art. 22 obligation satisfied without revealing `denial_reason` or score to the data subject | `specification.md` §7.5, T14; `agents.md` §4.2 |
| **Enum versioning with graceful unknown-value fallback** — new `denial_reason` values added by RTAF do not break GW; fallback to `SECURITY_DECLINE` is the default | `specification.md` §8, T2, §11 edge cases; `agents.md` §5; `.claude/project.md` Testing Defaults |
| **Circuit breaker on external scoring dependency** — prevents cascading failure when fraud engine degrades; state (open/half-open/closed) is observable | `specification.md` T5; `agents.md` §2.4 |
| **At-least-once event delivery with consumer-side deduplication** — reliable event pipeline without requiring exactly-once semantics from the broker | `specification.md` §8, T8–T11; `agents.md` §2.3 |
| **Boundary-value testing as a mandatory test category** — threshold-dependent systems are high-risk at boundary values; testing at `n−1`, `n`, `n+1` is required for all numeric thresholds | `specification.md` §12 O3 verification; `agents.md` §3.3; `.claude/project.md` Testing Defaults |
| **Explicit permission boundaries for sensitive internals** — regulated systems must state who can read and write each sensitive resource; prevents privilege escalation and accidental leakage | `specification.md` §6 Permission Boundaries |
| **Fixed-point arithmetic for monetary values** — floating-point is prohibited for all monetary fields to prevent rounding errors in financial calculations | `agents.md` §2.2; `.claude/project.md` "What to Avoid" |
| **Configurable thresholds with no hardcoded literals** — score tiers, TTLs, and SLA values are config-driven to allow ops tuning without code deployment | `specification.md` T6; `agents.md` §6; `.claude/project.md` "What to Avoid" |
