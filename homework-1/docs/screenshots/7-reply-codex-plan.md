## Align Transaction Contract With Business Semantics

### Summary
Implement your clarified rules as the source of truth:
- `fromAccount` / `toAccount` stay optional and are validated by transaction type.
- `amount`, `currency`, `type` are mandatory on create.
- `status` is server-managed only and derived from account state.

This keeps Findings 1 and 2 as intentional design, and resolves Finding 3.

### Implementation Changes
- **Request/Domain contract**
  - Keep account fields optional in create request and command.
  - Enforce required `amount`, `currency`, `type` in validation with explicit field errors.
  - Make `status` non-client-settable: reject create requests that include `status` with `400` + validation detail (`status is managed by server`).
  - Keep `Transaction.status` in response model (read-only output).

- **Status derivation in service**
  - Add status computation in service (not routing/validator):
    - `deposit` => `COMPLETED`
    - `withdrawal` / `transfer` => `COMPLETED` only if source account has sufficient balance in transaction currency, otherwise `FAILED`
  - Persist failed transactions (audit/history) and return `201`.
  - Balance calculations continue to ignore `FAILED` transactions (already consistent with current balance endpoint behavior).

- **Routing and payload handling**
  - Keep routes thin: parse, validate, delegate to service.
  - Ensure `POST /transactions` creates transaction regardless of `COMPLETED`/`FAILED` outcome (validation errors still return `400`).

- **Demo updates**
  - Update demo payloads to remove `status` from normal create requests.
  - Add a sample request that intentionally sends `status` and expects `400`.
  - Keep existing end-to-end flow plus insufficient-funds scenario demonstrating server-derived `FAILED`.

### Test Plan
- **Validator tests**
  - Missing `amount`, `currency`, `type` => `400` validation errors.
  - `status` provided by client => `400` validation error.
  - Type-dependent account requirements:
    - deposit requires `toAccount`
    - withdrawal requires `fromAccount`
    - transfer requires both.
- **Service tests**
  - Deposit increases balance and is `COMPLETED`.
  - Withdrawal/transfer with sufficient funds => `COMPLETED`.
  - Withdrawal/transfer with insufficient funds => persisted `FAILED`, no balance impact.
- **API integration/e2e**
  - `POST /transactions` success and failed-by-business-rule cases.
  - `GET /transactions` includes failed records.
  - `GET /accounts/{id}/balance` excludes failed effects.
  - Demo script passes with full request/response logging.

### Assumptions
- Initial account balance is `0`.
- No separate account registry: accounts are validated by format only.
- Sufficiency check is currency-specific and based on current completed transaction history.
- Failed transactions are part of transaction history (auditable) but not balance-affecting.
