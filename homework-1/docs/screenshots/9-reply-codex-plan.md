## Make Domain Models the Source of Truth (Type-Safe Transaction Contract)

### Summary
Refactor transaction contracts so API and domain share one sealed, type-specific model hierarchy.  
This removes nullable/optional create fields from API payloads, simplifies validation, and rewrites tests around the new contract.

### Key Changes
- **Unify API + domain create model**
    - Replace flat `CreateTransactionRequest` with sealed `CreateTransactionCommand` hierarchy:
        - `DepositCreate(toAccount, amount, currency)`
        - `WithdrawalCreate(fromAccount, amount, currency)`
        - `TransferCreate(fromAccount, toAccount, amount, currency)`
    - Keep single `POST /transactions` endpoint using JSON discriminator `type` (values: `deposit`, `withdrawal`, `transfer`).
    - Remove `status` from create schema entirely (server-owned).

- **Unify API + domain transaction response model**
    - Replace flat `Transaction` with sealed typed transaction variants (same type split as above), each including common server fields (`id`, `timestamp`, `status`) and type-required account fields only.
    - Keep `status` read-only in responses.

- **Validation simplification**
    - Structural required-field checks come from deserialization into typed models (no nullable create fields).
    - Validator now handles only business rules:
        - amount positive and max 2 decimals
        - account format `ACC-XXXXX`
        - valid ISO currency code
    - `status` in request is rejected as invalid payload (unknown field / schema violation).

- **Service + routing adjustments**
    - Routing receives typed command directly and delegates.
    - Service derives status from account state:
        - deposit => `COMPLETED`
        - withdrawal/transfer => `COMPLETED` if source has enough completed balance in same currency, else `FAILED`
    - Failed transactions are persisted and returned by history; balance computation excludes failed.
    - Filtering by `type` continues to work against the typed model discriminator.
    - Update demo payloads/scripts to new request shapes (no `status`), keep insufficient-funds flow.

### Test Plan
- **Validator tests (rewritten)**
    - Validate each typed command for amount/account/currency constraints.
    - Remove nullable-field tests tied to old flat request model.
- **Service tests (rewritten for typed commands)**
    - Deposit/withdrawal/transfer completion and insufficient-funds failure.
    - Failed persisted in history and excluded from balances.
- **API integration tests (rewritten)**
    - `POST /transactions` accepts valid typed payloads by discriminator.
    - Missing type-required fields (e.g., transfer without `toAccount`) return `400`.
    - Request containing `status` returns `400`.
    - `GET /transactions` includes failed items; balance excludes failed effects.
- **Demo/e2e verification**
    - `demo/sample-data.json` and `demo/sample-requests.http` use typed payloads without status.
    - `demo.sh` still passes full flow with request/response logs.

### Assumptions
- JSON contract remains a **single endpoint with `type` discriminator**.
- Domain model hierarchy is the canonical contract for create + response (no parallel flat API DTOs).
- Initial balance is `0`; no external account registry.
- Backward compatibility is not required for old flat payloads that included nullable fields or `status`.
