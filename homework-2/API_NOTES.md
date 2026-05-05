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

## resolved_at lifecycle

`resolved_at` is set automatically by the server — it cannot be set by the client directly.

| Transition | Effect on resolved_at |
|---|---|
| Status changes to `resolved` or `closed` and `resolved_at` was null | Set to current timestamp |
| Any other status change | `resolved_at` unchanged |
| Ticket created | Always null |
