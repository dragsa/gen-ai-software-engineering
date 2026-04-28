### Currency + Money Type Refactor (`homework-1`)

#### Summary
Refactor monetary and currency modeling to remove Java `Currency` runtime validation, use explicit Kotlin domain types, and make financial math precision-safe:
- Replace string-based currency with a dedicated `CurrencyCode` enum (full ISO-4217 list).
- Replace `Double` amounts with `BigDecimal`.
- Replace `Map<String, String>` balances with `Map<CurrencyCode, BigDecimal>`.
- Keep request currency parsing case-insensitive for backward compatibility.
- Represent money in JSON as decimal strings (output), with tolerant input parsing for migration safety.

#### Public API / Model Changes
- `CreateTransactionRequest.amount`: `Double` -> `BigDecimal` (serialized as decimal string).
- `CreateTransactionRequest.currency`: `String` -> `CurrencyCode` (case-insensitive parsing).
- `Transaction.amount`: `Double` -> `BigDecimal`.
- `Transaction.currency`: `String` -> `CurrencyCode`.
- `CreateTransactionCommand` and all command variants: `amount: BigDecimal`, `currency: CurrencyCode`.
- `BalanceResponse.balances`: `Map<String, String>` -> `Map<CurrencyCode, BigDecimal>`.
- Add serializers:
    - `BigDecimalAsStringSerializer` for stable precision-preserving JSON.
    - `CurrencyCodeSerializer` for case-insensitive enum decoding (`usd`, `Usd`, `USD` all accepted; normalized to enum).

#### Implementation Changes
- **Models and serialization**
    - Introduce `CurrencyCode` enum with full ISO-4217 codes.
    - Annotate money and currency fields to use custom serializers where needed.
    - Keep route contracts unchanged functionally (same endpoints/status semantics), only payload/response typing changes.

- **Validation**
    - Remove `java.util.Currency` usage entirely from validator.
    - Currency validity becomes enum decoding validity.
    - Amount validation switches to `BigDecimal` checks:
        - `> 0`
        - scale max `2` (after trailing-zero normalization policy defined in code).

- **Service logic**
    - Eliminate all `BigDecimal.valueOf(Double)` conversions.
    - Perform all arithmetic directly on `BigDecimal`.
    - Keep failed-transaction exclusion and status derivation behavior unchanged.

- **Docs/spec**
    - Update README architecture notes with:
        - why enum over Java `Currency`,
        - why `BigDecimal` for monetary correctness,
        - case-insensitive currency input policy.
    - Update OpenAPI (`openapi.yaml`) schemas:
        - money fields as string-decimal,
        - currency fields as enum.
    - Keep HOWTORUN unchanged except if examples need new payload formatting.

#### Test Plan
- **Unit (validator + serializers)**
    - Case-insensitive currency decode success (`usd`, `USD`) and invalid code failure.
    - Amount validation split by rule:
        - non-positive rejected,
        - >2 decimals rejected,
        - valid positive 2-decimal accepted.
    - BigDecimal serializer round-trip:
        - preserves exact decimal text (no floating artifacts),
        - accepts migration-friendly numeric input if enabled.

- **Service**
    - Re-run existing balance/status scenarios with `BigDecimal` values.
    - Add precision-focused case (`0.10 + 0.20 - 0.30 == 0.00`) to prove no binary-float drift.

- **Integration**
    - `POST /transactions` with string-decimal amount and case-insensitive currency.
    - `GET /accounts/{id}/balance` returns enum-keyed currency map and decimal-string amounts in JSON.
    - Existing failed/completed status behavior remains identical.
    - Swagger/OpenAPI endpoint tests remain green and assert updated schema fragments.

- **Fixtures/demo compatibility**
    - Update test fixtures to new amount format.
    - Update demo payloads only if needed by new request schema to keep e2e runnable.

#### Assumptions
- Full ISO enum is committed statically in repo; no runtime Java `Currency` dependency for validation.
- Monetary JSON output is canonical decimal string; internal arithmetic is `BigDecimal` only.
- Currency input remains backward-compatible via case-insensitive decoding.
- This is a contract change at type level; behavior and endpoint set remain unchanged.
