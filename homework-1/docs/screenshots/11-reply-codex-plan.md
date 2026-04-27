## Task 4A: Account Summary as Transaction List

### Summary
Implement Task 4 summary as:
- `GET /accounts/{accountId}/summary`
- Response: `200` with `Transaction[]` (reuse existing transaction API model)
- Include all statuses (`COMPLETED`, `FAILED`)
- Sort by `timestamp` descending (newest first)
- Valid account with no transactions returns `200` + `[]`

### Key Changes
- Add route `GET /accounts/{accountId}/summary` with existing account ID format validation.
- Extend service with `getAccountSummary(accountId: String): List<Transaction>`.
- Service filters by `fromAccount == accountId || toAccount == accountId`, then sorts newest-first.
- No new DTOs; reuse existing `Transaction`.
- Update demo requests/data to include summary endpoint call and assertions.

### Test Plan
- **API integration**
    - Returns `200` with account-scoped transaction list.
    - Includes failed transactions.
    - Returns newest-first ordering.
    - Invalid account format returns `400`.
    - Valid account with no transactions returns `200` + `[]`.
    - **No account exists case:** since there is no account registry, “non-existent account” is treated as “no transactions”; if format is valid, return `200` + `[]` (not `404`).

- **Service**
    - Filters both sender and receiver matches.
    - Sorting by timestamp descending is correct.
    - Empty summary returns empty list.

- **Demo/e2e**
    - Summary call added to demo flow and passes with existing checks.

### Assumptions
- Summary is list-based (not totals object), per your requirement.
- No account registry exists in this homework; existence is inferred only from transaction history.
