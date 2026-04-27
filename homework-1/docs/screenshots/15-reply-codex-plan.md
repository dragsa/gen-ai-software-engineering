### Add Local Swagger/OpenAPI for `homework-1`

#### Summary
Add a fully local, runnable API documentation experience for manual testing:
- Swagger UI at `http://localhost:8080/swagger`
- Raw OpenAPI spec at `http://localhost:8080/openapi.yaml`
- Keep implementation simple with a static YAML spec in app resources.

#### Implementation Changes
- **OpenAPI source of truth**
    - Add `homework-1/src/main/resources/openapi.yaml`.
    - Define all current endpoints and schemas:
        - `GET /`
        - `POST /transactions`
        - `GET /transactions`
        - `GET /transactions/{id}`
        - `GET /accounts/{accountId}/balance`
        - `GET /accounts/{accountId}/summary`
    - Reuse current API models in schema definitions (`CreateTransactionRequest`, `Transaction`, `BalanceResponse`, `ErrorResponse`, `ValidationError`, enums).

- **Ktor wiring**
    - Add Swagger dependency via TOML + module build config (compatible with existing Ktor `3.4.3` stack).
    - In application routing setup, expose:
        - Swagger UI route mapped to `/swagger` reading `openapi.yaml`.
        - Raw spec route `/openapi.yaml` served directly from resources.
    - Keep all existing business routes unchanged.

- **Docs updates**
    - Update `homework-1/HOWTORUN.md` with Swagger/OpenAPI local URLs and quick manual testing steps.
    - Update `homework-1/README.md` (permission granted) with:
        - API documentation availability
        - local manual testing flow via Swagger UI
        - note that OpenAPI YAML must stay in sync with API behavior.

#### Test Plan
- **Integration tests**
    - Add assertions for docs endpoints:
        - `GET /openapi.yaml` returns `200` and contains `openapi:` + expected path fragments.
        - `GET /swagger` returns `200` and HTML response.
- **Regression**
    - Run `:homework-1:test` full suite to ensure no API regressions.
- **Manual acceptance**
    - Start app locally and open `/swagger` in browser.
    - Verify operations are visible and executable against localhost.
    - Confirm raw spec is downloadable at `/openapi.yaml`.

#### Assumptions and Defaults
- Static YAML is intentionally chosen for simplicity and reviewability.
- Swagger/OpenAPI endpoints are public in local runtime (no auth layer).
- No changes to read-only files (`AGENTS.MD`, `TASKS.md`).
- No cross-subproject coupling introduced; all changes stay in `homework-1`.
