# Architecture — Customer Support Ticket System

## Component overview

```mermaid
graph TD
    Client([HTTP Client])

    subgraph Ktor["Ktor / Netty"]
        TR[TicketRoutes]
        DR[DocumentationRoutes]
    end

    subgraph Service["Service layer"]
        TS[TicketServiceImpl]
        TC[TicketClassifier]
        TV[TicketValidator]
    end

    subgraph Parsers["Import parsers"]
        CP[CsvTicketParser]
        JP[JsonTicketParser]
        XP[XmlTicketParser]
    end

    subgraph Repository["Repository layer"]
        REPO[InMemoryTicketRepository]
        STORE[(ConcurrentHashMap)]
    end

    Client -->|HTTP| TR
    Client -->|HTTP| DR
    TR --> TS
    TS --> TV
    TS --> TC
    TS --> REPO
    TR --> CP
    TR --> JP
    TR --> XP
    CP --> TS
    JP --> TS
    XP --> TS
    REPO --> STORE
```

### Package map

| Package | Responsibility |
|---|---|
| `homework2.entrypoint` | Ktor server bootstrap and application module wiring |
| `homework2.routing` | Route handlers — HTTP in, HTTP out; no business logic |
| `homework2.service` | `TicketServiceImpl`, `TicketClassifier`, `InMemoryTicketRepository` |
| `homework2.models` | Data classes, enums, request/response shapes |
| `homework2.validation` | `TicketValidator` — pure validation, no I/O |
| `homework2.utils.parsers` | Format-specific parsers returning `ParsedRow` results |

---

## Bulk import sequence

The sequence below shows what happens from the moment `POST /tickets/import` arrives until the response is sent.

```mermaid
sequenceDiagram
    actor Client
    participant Route as TicketRoutes
    participant Parser as Parser (CSV/JSON/XML)
    participant Service as TicketServiceImpl
    participant Validator as TicketValidator
    participant Classifier as TicketClassifier
    participant Repo as InMemoryTicketRepository

    Client->>Route: POST /tickets/import (multipart)
    Route->>Route: detect format from Content-Type / filename
    Route->>Parser: parse(fileContent)
    Parser-->>Route: List<ParsedRow> (Success | Failure per row)

    Route->>Service: bulkImport(rows)

    loop for each ParsedRow
        alt ParsedRow.Failure
            Service->>Service: record failure in summary
        else ParsedRow.Success
            Service->>Validator: validateCreate(request)
            alt validation errors
                Service->>Service: record validation failure in summary
            else valid
                Service->>Validator: toTicket(request, id, now)
                Service->>Repo: save(ticket)
                Service->>Service: record success in summary
            end
        end
    end

    Service-->>Route: ImportSummaryResponse
    Route-->>Client: 200 OK { total, successful, failed, failures[] }
```

---

## Auto-classify sequence

```mermaid
sequenceDiagram
    actor Client
    participant Route as TicketRoutes
    participant Service as TicketServiceImpl
    participant Repo as InMemoryTicketRepository
    participant Classifier as TicketClassifier

    Client->>Route: POST /tickets/{id}/auto-classify
    Route->>Service: classifyTicket(id)
    Service->>Repo: findById(id)

    alt not found
        Repo-->>Service: null
        Service-->>Route: null
        Route-->>Client: 404 Not Found
    else found
        Repo-->>Service: Ticket
        Service->>Classifier: classify(ticket)
        Note over Classifier: keyword scan on subject+description<br/>confidence = (categorySignal×0.7) + (prioritySignal×0.3)
        Classifier-->>Service: ClassificationDecision
        Service->>Repo: save(ticket.copy(category, priority))
        Service-->>Route: ClassificationDecision
        Route-->>Client: 200 OK { ticket_id, category, priority, confidence, reasoning, keywords_found }
    end
```

---

## Key design decisions

**In-memory repository with `ConcurrentHashMap`** — sufficient for a coursework API. All reads and writes are thread-safe without external locking. Persistence across restarts is out of scope; the design can be replaced with a database-backed implementation by swapping the `TicketRepository` binding in `ApplicationModule`.

**`ParsedRow` sealed class** — parsers never throw; every row produces either a `ParsedRow.Success` (holds a `CreateTicketRequest`) or a `ParsedRow.Failure` (holds a row number, error message, and optional raw string). This allows partial import success and clean summary reporting without exception-based control flow.

**`TicketValidator` as a pure function object** — no state, no I/O. It accepts a request and returns a list of `ValidationError`. The same validator is used by the direct-create endpoint and by `bulkImport`, ensuring consistent rules across both paths.

**`TicketClassifier` with an internal decision log** — the log is in-memory only and has no public HTTP endpoint. It exists to support internal inspection during testing and could be exposed as an admin endpoint without changing the classifier itself.

**Format detection order** — the import route prefers the `Content-Type` header of the file part; if absent, it falls back to the filename extension. This means a file named `export.csv` sent without an explicit part content-type still works correctly, which matches real-world client behaviour (e.g. `curl -F 'file=@export.csv'`).
