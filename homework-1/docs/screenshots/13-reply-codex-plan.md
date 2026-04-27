## Coverage Verification and Gap-Closing Plan

### Summary
Verified coverage using a one-off Jacoco report (generated via temporary Gradle init script, no repo config changes).

Current project coverage (`homework-1`):
- Line: **84.35%** (`264/313`)
- Branch: **59.20%** (`103/174`)
- Method: **86.00%** (`86/100`)
- Class: **88.46%** (`23/26`)

Package line coverage:
- `homework1/models`: **96.77%**
- `homework1/service`: **97.44%**
- `homework1/routing`: **72.06%**
- `homework1/validation`: **75.31%**
- `homework1/utils`: **66.67%**
- `homework1/entrypoint`: **80.00%**

### Uncovered Parts to Add
- **Routing gaps** (`TransactionRoutes.kt`, `HealthRoutes.kt`)
    - Add API tests for:
        - `POST /transactions` validation-error branch (successful JSON parse + validator errors)
        - `GET /transactions` invalid filter branch (bad `type`, bad dates, `from > to`)
        - `GET /transactions/{id}` not-found and happy path
        - `GET /accounts/{id}/balance` invalid account format branch
        - `GET /` health route response (`Hello World`)
    - Note: null path-param guards for routes like `/transactions/{id}` and `/accounts/{accountId}/...` are effectively unreachable; either keep as defensive code and accept lower branch coverage, or remove these guards to avoid untestable branches.

- **Validation gaps** (`TransactionValidator.kt`)
    - Add unit tests for:
        - invalid account format on `fromAccount` and `toAccount`
        - filter validation failures (`type`, `from`, `to`, range order)
        - explicit valid-path checks for `validateFilters` return model

- **Utils gaps** (`ParsingUtils.kt`)
    - Add direct unit tests for:
        - `parseIsoDate` invalid date path
        - `parseTransactionType` invalid/non-null path
        - `parseTransactionStatus` (currently unused but uncovered)
    - If keeping `status` parsing out of API behavior, consider removing `parseTransactionStatus` as dead code instead of testing it.

- **Entrypoint gap** (`Main.kt`)
    - Decide one of:
        - exclude `Main.kt` from coverage target (recommended), or
        - add a lightweight smoke test pattern for launcher wiring.
    - Excluding launcher code is common and prevents artificial test complexity.

### Test Plan Additions
- Extend integration test class with new negative and edge route scenarios (focus on `routing` package).
- Extend validator test class with all filter/account error branches.
- Add a small `ParsingUtilsTest` for parse helpers.
- Keep service tests mostly as-is (already high coverage), only add if new route/validator scenarios expose service edge paths.
- Re-run full build with coverage report and track improved metrics, targeting:
    - `routing` >= 85% line
    - `validation` >= 90% line
    - `utils` >= 90% line (or remove dead util and keep >90% on remaining code)

### Assumptions
- “Verify percentage” is satisfied by one-off Jacoco run, not permanent build plugin wiring.
- No production behavior changes are required for this task unless you choose to remove unreachable guards/dead util methods.
- Coverage target emphasis is meaningful business paths, not synthetic Kotlin-generated accessors.
