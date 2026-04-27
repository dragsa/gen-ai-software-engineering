## Keep One API Model, Derive Sealed Domain Commands

### Summary
Refactor to a hybrid contract:
- Keep a **single flat API create model**.
- Make **domain create models sealed** and map from API model into domain variants.
- Keep `fromAccount`/`toAccount` optional in API but enforce requiredness by `type`.
- Keep `status` out of business logic; it is always server-derived.
- Per your choice: client-sent `status` is **ignored silently**.

### Key Changes
- **API create model (single)**
    - `fromAccount: String?`, `toAccount: String?`
    - `amount`, `currency`, `type` become mandatory (non-null, no defaults).
    - Keep a compatibility field for `status` (or tolerate it in decoding) but never use it in command mapping.

- **Domain source of truth (sealed)**
    - Introduce sealed `CreateTransactionCommand`:
        - `Deposit(toAccount, amount, currency)`
        - `Withdrawal(fromAccount, amount, currency)`
        - `Transfer(fromAccount, toAccount, amount, currency)`
    - Add mapper `API model -> sealed command` after validation.
    - Service accepts only sealed command; status derivation remains in service.

- **Validation simplification**
    - Remove nullable checks for `amount/currency/type` (handled by deserialization model shape).
    - Keep only business validations:
        - amount positive, max 2 decimals
        - account format
        - type-based account presence (from/to by transaction type)
        - currency validity
    - Remove validation rule that rejected `status`.

- **Routing/API behavior**
    - Route still `POST /transactions` with same endpoint.
    - Decode flat API model, validate, map to sealed domain command, call service.
    - Client `status` does not affect outcome; response `status` always computed by service.

### Test Plan
- **Validator tests**
    - Rewrite to use mandatory API model fields.
    - Keep type-based account validation tests.
    - Remove tests expecting `status` rejection.
- **Service tests**
    - Keep/adjust tests for derived `COMPLETED`/`FAILED` status and failed-not-in-balance.
    - Ensure sealed command variants are directly tested.
- **API integration tests**
    - Sending `status` in create payload still succeeds and is ignored.
    - Derived status assertions (including insufficient funds -> `FAILED`).
    - Existing list/balance behavior remains unchanged.

### Assumptions
- Backward compatibility is desired for clients still sending `status` on create.
- Response model remains as-is (includes derived `status`).
- Single `/transactions` endpoint and flat request shape remain unchanged externally.
