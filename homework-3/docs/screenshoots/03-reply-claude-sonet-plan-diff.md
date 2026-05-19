Here is the revised plan. I'll summarise every change made:

---

## What changed

### Added: Section 1.2 — Out of Scope

Explicit block listing what is intentionally excluded: GW implementation, fraud engine implementation, full AML/KYC programme, human review UI, card scheme dispute workflows. Keeps grading scope clear.

---

### Added: Section 1.3 — Stakeholders

Four stakeholders with separate observe/control columns:

| Stakeholder | Observes | Controls |
|---|---|---|
| End User | Generic decline message only | Nothing |
| Ops | Throughput, queue depth, vendor staleness, error rates | Queue drain, vendor sync schedule, tier thresholds |
| Compliance | Full immutable audit log, GDPR retention state | Retention policy, right-to-explanation templates |
| Support | `denial_reason` by transaction ID lookup (read-only, redacted) | Nothing — read-only |

---

### Added: Section 1.6 — Permission Boundaries

Explicit access control table covering six sensitive resources: `denial_reason` field, `user_facing_category` mapping, human review queue, audit log, fraud engine event stream, vendor list cache. Each row states who can read, who can write/modify, and a key constraint.

---

### Added: Section 1.12 — Objective-to-Verification Matrix

O1–O9 each mapped to four columns: unit checks, integration checks, fixtures/reconciliation, and manual compliance step. No longer a generic paragraph — every objective has concrete, checkable verification criteria.

---

### Added: Low-level task shape reminder (Section 1.10)

Every task in the final `specification.md` will follow the template exactly: **Prompt / Target file / Details / Acceptance criteria**. Stated explicitly in the plan so execution cannot deviate from the template format.

---

### Removed: T16 (AML/KYC expansion)

Dropped entirely. AML is now scoped to velocity-check signals and watchlist-match flags only, fed as lightweight inputs to the scoring tier. Reflected in the risks section with an explicit scope guard.

---

### Merged tasks — 18 → 14

- **T2 + T3 + T4** → single **T2** "Decision contract + GW mapping group"
- **T14 + T15** → single **T12** "Audit logging + PCI sanitization group"
- **T17** → **T13** reframed as an NFR/compliance task (latency instrumentation)
- **T18** → **T14** reframed as an NFR/compliance task (GDPR retention policy)

---
