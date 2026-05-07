# API Reference — Customer Support Ticket System

Base URL: `http://localhost:8080`

---

## Tickets

### POST /tickets

Create a new support ticket.

**Request body** (`application/json`)

| Field | Type | Required | Constraints |
|---|---|---|---|
| `customer_id` | string | yes | non-blank |
| `customer_email` | string | yes | valid email format |
| `customer_name` | string | yes | non-blank |
| `subject` | string | yes | 1–200 characters |
| `description` | string | yes | 10–2000 characters |
| `category` | string | no | see [Enum values](#enum-values); defaults to `other` |
| `priority` | string | no | see [Enum values](#enum-values); defaults to `medium` |
| `status` | string | no | see [Enum values](#enum-values); defaults to `new` |
| `assigned_to` | string | no | free text |
| `tags` | string[] | no | defaults to `[]` |
| `metadata.source` | string | no (required if metadata present) | see [Enum values](#enum-values); defaults to `api` |
| `metadata.browser` | string | no | free text |
| `metadata.device_type` | string | no | see [Enum values](#enum-values) |

**Responses**

`201 Created` — ticket created successfully.

```json
{
  "id": "a1b2c3d4",
  "customer_id": "cust-001",
  "customer_email": "alice@example.com",
  "customer_name": "Alice Smith",
  "subject": "Cannot login to my account",
  "description": "I have been unable to login since yesterday morning",
  "category": "other",
  "priority": "medium",
  "status": "new",
  "assigned_to": null,
  "tags": [],
  "metadata": { "source": "api", "browser": null, "device_type": null },
  "created_at": "2026-01-01T00:00:00Z",
  "updated_at": "2026-01-01T00:00:00Z",
  "resolved_at": null
}
```

`400 Bad Request` — validation failed.

```json
{
  "error": "Validation failed",
  "details": [
    { "field": "customer_email", "message": "must be a valid email address" },
    { "field": "description",    "message": "must be between 10 and 2000 characters" }
  ]
}
```

**cURL**

```bash
curl -X POST http://localhost:8080/tickets \
  -H 'Content-Type: application/json' \
  -d '{
    "customer_id": "cust-001",
    "customer_email": "alice@example.com",
    "customer_name": "Alice Smith",
    "subject": "Cannot login to my account",
    "description": "I have been unable to login since yesterday morning after a password reset"
  }'
```

---

### GET /tickets

List tickets with optional filters.

**Query parameters**

| Parameter | Type | Description |
|---|---|---|
| `status` | string | filter by status enum value |
| `category` | string | filter by category enum value |
| `priority` | string | filter by priority enum value |
| `customer_id` | string | exact match on customer ID |
| `assigned_to` | string | exact match on assigned agent |
| `search` | string | case-insensitive substring match on subject and description |

**Responses**

`200 OK` — array of ticket objects (same shape as POST response). Empty array when no matches.

`400 Bad Request` — invalid enum value supplied for `status`, `category`, or `priority`.

```json
{
  "error": "Validation failed",
  "details": [
    { "field": "category", "message": "invalid value 'not_a_category'" }
  ]
}
```

**cURL examples**

```bash
# all tickets
curl http://localhost:8080/tickets

# filter by status
curl 'http://localhost:8080/tickets?status=in_progress'

# full-text search
curl 'http://localhost:8080/tickets?search=login'

# combined filters
curl 'http://localhost:8080/tickets?category=billing_question&priority=high'
```

---

### GET /tickets/{id}

Retrieve a single ticket by ID.

**Responses**

`200 OK` — ticket object.

`404 Not Found`

```json
{ "error": "Ticket not found" }
```

**cURL**

```bash
curl http://localhost:8080/tickets/a1b2c3d4
```

---

### PUT /tickets/{id}

Partially update a ticket. Only fields present in the request body are changed; omitted fields keep their current values.

**Request body** (`application/json`) — all fields optional, same types as POST.

**Responses**

`200 OK` — updated ticket object.

`400 Bad Request` — JSON parse failure or validation error on a supplied field.

`404 Not Found` — ticket does not exist.

**cURL**

```bash
curl -X PUT http://localhost:8080/tickets/a1b2c3d4 \
  -H 'Content-Type: application/json' \
  -d '{"status": "in_progress", "assigned_to": "agent-007"}'
```

---

### DELETE /tickets/{id}

Delete a ticket permanently.

**Responses**

`204 No Content` — deleted successfully, no body.

`404 Not Found` — ticket does not exist.

**cURL**

```bash
curl -X DELETE http://localhost:8080/tickets/a1b2c3d4
```

---

### POST /tickets/{id}/auto-classify

Run the rule-based classifier on a ticket's subject and description. Updates the ticket's `category` and `priority` in place and returns the classification decision.

**Responses**

`200 OK`

```json
{
  "ticket_id": "a1b2c3d4",
  "category": "account_access",
  "priority": "high",
  "confidence": 0.73,
  "reasoning": "Matched category 'account_access' (2 keyword(s): login, password). Priority set to 'high' (keyword(s): blocking).",
  "keywords_found": ["blocking", "login", "password"]
}
```

`404 Not Found` — ticket does not exist.

**cURL**

```bash
curl -X POST http://localhost:8080/tickets/a1b2c3d4/auto-classify
```

---

### POST /tickets/import

Bulk-import tickets from a file upload. Accepts CSV, JSON, or XML. Returns a per-row summary; the request itself always returns `200` — per-row failures are reported inside the body, not as HTTP errors.

**Request** — `multipart/form-data` with a single `file` part.

The format is detected in this order:
1. `Content-Type` of the file part (`text/csv`, `application/json`, `application/xml`)
2. Filename extension (`.csv`, `.json`, `.xml`)

**CSV format** — first row must be the header. Required columns: `customer_id`, `customer_email`, `customer_name`, `subject`, `description`. Optional columns: `category`, `priority`, `status`, `assigned_to`, `tags` (`;`-separated).

**JSON format** — either a JSON array of objects, or a single JSON object.

**XML format** — root element must be `<tickets>` (multiple) or `<ticket>` (single). Field names match the JSON keys.

**Response** `200 OK`

```json
{
  "total": 3,
  "successful": 2,
  "failed": 1,
  "failures": [
    {
      "row": 2,
      "reason": "customer_email: must be a valid email address",
      "raw": "{\"customer_id\": \"cust-bad\", ...}"
    }
  ]
}
```

`400 Bad Request` — no file part found, or format cannot be determined.

**cURL examples**

```bash
# CSV import
curl -X POST http://localhost:8080/tickets/import \
  -F 'file=@demo/sample_tickets.csv'

# JSON import
curl -X POST http://localhost:8080/tickets/import \
  -F 'file=@demo/sample_tickets.json'

# XML import
curl -X POST http://localhost:8080/tickets/import \
  -F 'file=@demo/sample_tickets.xml'
```

---

## Documentation endpoints

### GET /openapi.yaml

Returns the OpenAPI 3.1 specification for this API in YAML format.

### GET /swagger

Returns the Swagger UI HTML page.

---

## Enum values

**category**: `account_access` · `technical_issue` · `billing_question` · `feature_request` · `bug_report` · `other`

**priority**: `urgent` · `high` · `medium` · `low`

**status**: `new` · `in_progress` · `waiting_customer` · `resolved` · `closed`

**metadata.source**: `web_form` · `email` · `api` · `chat` · `phone`

**metadata.device_type**: `desktop` · `mobile` · `tablet`

---

## Error shapes

All error responses use `application/json`.

| Shape | When used |
|---|---|
| `{"error": "...", "details": [...]}` | validation failures (400) |
| `{"error": "..."}` | all other errors (400, 404) |

`resolved_at` is set automatically when `status` is changed to `resolved` or `closed`. It is never overwritten once set.
