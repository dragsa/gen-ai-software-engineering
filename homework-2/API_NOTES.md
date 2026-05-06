# API Notes

> Temporary working document — content to be merged into `API_REFERENCE.md` in Step 5.

---

## POST /tickets — Optional fields and server-side defaults

The following fields are optional in the create request. When omitted, the server applies the listed defaults:

| Field | Default | Notes |
|---|---|---|
| `category` | `other` | Applied when field is absent or null |
| `priority` | `medium` | Applied when field is absent or null |
| `status` | `new` | Applied when field is absent or null |
| `metadata` | `{ "source": "api", "browser": null, "device_type": null }` | Applied when the entire metadata object is absent |
| `tags` | `[]` | Empty array when absent |
| `assigned_to` | `null` | Unassigned when absent |

When `metadata` **is** provided, `source` is **required** inside it. `browser` and `device_type` remain optional within a provided metadata block.

---

## Metadata — why browser and device_type are always nullable

`browser` and `device_type` are genuinely optional in the domain — a ticket created via API may have no browser or device context. They are nullable in both `CreateMetadataRequest` and `UpdateMetadataRequest`, and in the `Metadata` domain entity.

`source` is non-nullable in the domain. In `CreateMetadataRequest` it is required. In `UpdateMetadataRequest` it is nullable because updates are partial (absent = leave unchanged).

---

## PUT /tickets/:id — Partial update semantics

All fields in `UpdateTicketRequest` are nullable. The rule is:
- **Field absent / null in request** → existing value is preserved unchanged.
- **Field present with a value** → existing value is replaced.

This means a client can update only `status` without resending the entire ticket body.

---

## POST /tickets/{id}/auto-classify — Classification behaviour

### Confidence formula

```
categorySignal = matchedCategoryKeywords / totalKeywordsInWinningCategory
prioritySignal = 1.0 if any priority keyword matched, else 0.5 (MEDIUM default)
confidence     = (categorySignal × 0.7) + (prioritySignal × 0.3)  — clamped to [0.0, 1.0]
```

Category drives 70 % of confidence (requires matching specific vocabulary).
Priority drives 30 % (binary — keyword fires or falls back to MEDIUM).

### Category keyword maps

| Category | Keywords |
|---|---|
| `account_access` | login, password, 2fa, two-factor, sign in, sign-in, locked out, account access, authentication, forgot password, reset password |
| `technical_issue` | error, crash, not working, broken, fails, failure, exception, timeout, slow, performance, down, outage |
| `billing_question` | invoice, billing, charge, payment, refund, subscription, price, cost, receipt, overcharged, discount |
| `feature_request` | feature, request, suggestion, enhancement, improve, would like, wish, add support, could you, please add |
| `bug_report` | bug, reproduce, steps to reproduce, expected behavior, actual behavior, defect, regression, version |

If no keywords match → `other`.

### Priority keyword maps (first-match precedence: URGENT → HIGH → LOW → MEDIUM)

| Priority | Keywords |
|---|---|
| `urgent` | can't access, cannot access, critical, production down, prod down, security, data loss, urgent, emergency, immediately |
| `high` | important, blocking, blocked, asap, as soon as possible, high priority |
| `low` | minor, cosmetic, suggestion, nice to have, low priority, whenever |

### Side effects

- The classifier **persists** the resulting `category` and `priority` back onto the ticket.
- All decisions are recorded in an internal in-memory log (no public endpoint).

### ?auto_classify=true on POST /tickets

When `?auto_classify=true` is passed to `POST /tickets`, classification runs immediately after creation. The `POST /tickets` response still returns the standard `TicketResponse` (not the classification payload). The updated category and priority from classification are reflected in subsequent `GET /tickets/{id}` calls.

---

## resolved_at lifecycle

`resolved_at` is set automatically by the server — it cannot be set by the client directly.

| Transition | Effect on resolved_at |
|---|---|
| Status changes to `resolved` or `closed` and `resolved_at` was null | Set to current timestamp |
| Any other status change | `resolved_at` unchanged |
| Ticket created | Always null |
