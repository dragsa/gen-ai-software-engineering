# Plan — Homework 2: Customer Support Ticket System

> Approved by user on 2026-05-05.
> Stack-bound to `.agents/docs/STACK.MD`. Package root: `homework2`. No dependency on `homework-1`.

## Confirmed defaults

1. README.md rewrite — permission granted (will be re-confirmed when Phase 4 hits).
2. CSV parsing — Apache Commons CSV.
3. XML parsing — JDK stdlib `DocumentBuilderFactory` with secure features (XXE/DTD disabled).
4. JaCoCo — added to `gradle/libs.versions.toml` and applied in `homework-2/build.gradle.kts`.
5. Documentation count — 4 docs (TASKS.md inconsistency between "5" and listed 4 noted; deferred).
6. Auto-classify trigger — `?auto_classify=true` query flag on `POST /tickets`.
7. Decision log — internal-only (no public endpoint).
8. Plan persistence — this file.
9. Phase order — sequential 0 → 6.

---

## Phase 0 — Bootstrap

**Goal:** lay down the package layout and entrypoint per STACK.MD.

**Steps:** create `entrypoint`, `routing`, `service`, `models`, `validation`, `utils` packages under `src/main/kotlin/homework2/`; mirror under `src/test/kotlin/homework2/` plus `testsupport`. Add `Main.kt` and a parameterized `Application.module(...)` overload. Empty `src/main/resources/openapi.yaml`. Update `HOWTORUN.md`.

**Files (new):** `src/main/kotlin/homework2/entrypoint/{Main.kt, ApplicationModule.kt}`, `src/main/resources/openapi.yaml` (stub), `src/test/resources/fixtures/.gitkeep`.

**Risk:** HW2 needs CSV (Apache Commons CSV) + JaCoCo (added in Phase 3). Adding entries to `libs.versions.toml` is not a stack deviation per STACK.MD's deviation policy.

## Phase 1 — Task 1: Multi-Format Ticket Import API

**Goal:** CRUD + bulk import (CSV/JSON/XML) with validation and structured error reporting.

**Steps:**
1. **Models** (`models/`): API DTOs (`CreateTicketRequest`, `UpdateTicketRequest`, `TicketResponse`, `ImportSummaryResponse`, `ImportFailure`, plus the standard `ErrorResponse`/`ValidationError` shape per STACK.MD); domain `Ticket` entity; enums (`Category`, `Priority`, `Status`, `Source`, `DeviceType`); nested `Metadata` type. Strict separation per STACK.MD — domain types never serialized directly.
2. **Repository** (`service/TicketRepository.kt` interface + `InMemoryTicketRepository.kt`): UUID id generation, synchronized list-backed store, `created_at`/`updated_at`/`resolved_at` lifecycle.
3. **Service** (`service/TicketService.kt` + impl): CRUD + bulk-import orchestration (parse → validate per row → persist successes → collect failures → return summary).
4. **Validation** (`validation/TicketValidator.kt`): email regex, subject 1–200, description 10–2000, enum coercion, returning `List<ValidationError>` (no throwing).
5. **Parsers** (`utils/parsers/`): `CsvTicketParser`, `JsonTicketParser`, `XmlTicketParser`, each producing canonical `CreateTicketRequest`. Hardened XML (XXE/DTD disabled).
6. **Routes** (`routing/TicketRoutes.kt`): `POST/GET /tickets`, `GET/PUT/DELETE /tickets/:id`, `POST /tickets/import` (multipart upload, format inferred from Content-Type then filename extension), `GET /tickets` filters (category, priority, status, assigned_to, customer_id, free-text search).
7. **OpenAPI YAML** updated to reflect endpoints.

**Files (new):** ~15 Kotlin files + `openapi.yaml` filled out.

**Risks:** bulk-import format detection (Content-Type primary, extension fallback). Multipart upload size cap at 10 MB.

## Phase 2 — Task 2: Auto-Classification

**Goal:** rule-based category + priority + confidence + reasoning.

**Steps:**
1. **Classifier service** (`service/TicketClassifier.kt`): keyword maps per category + per priority; confidence = matched-weight / total-signal (transparent formula documented in `ARCHITECTURE.md`); returns `(category, priority, confidence, reasoning, keywordsFound)`.
2. **Route** `POST /tickets/:id/auto-classify` → run classifier, persist decision on the ticket, return classification payload.
3. **Optional auto-run** on `POST /tickets` via `?auto_classify=true` query flag.
4. **Manual override** preserved by leaving classifier off the `PUT /tickets/:id` path.
5. **Decision log** kept in-memory inside the classifier service (no public endpoint).

**Files (new):** `TicketClassifier.kt`, `ClassificationModels.kt`; updates to `Ticket` model (add `classification` field) and `TicketRoutes.kt`.

**Risk:** confidence is heuristic — must be documented openly. Priority urgent-keywords overlap with category keywords; priority calc kept independent of category calc.

## Phase 3 — Task 3: Test Suite (>85% coverage)

**Goal:** comprehensive JUnit suite via Ktor `testApplication`, JaCoCo-measured.

**Steps:**
1. Add **JaCoCo** plugin to `libs.versions.toml` and apply in `homework-2/build.gradle.kts` with 85% gate.
2. **Test classes** (Kotlin/JUnit naming, counts preserved from spec):
   - `TicketApiTest` — 11 HTTP tests
   - `TicketModelTest` — 9 model/validator tests
   - `CsvImportTest` — 6
   - `JsonImportTest` — 5
   - `XmlImportTest` — 5
   - `CategorizationTest` — 10
   - `IntegrationTest` — 5 (Phase 5)
   - `PerformanceTest` — 5 (Phase 5)
3. **Fixtures** (`src/test/resources/fixtures/`): valid CSV/JSON/XML samples + malformed variants. `testsupport/Fixtures.kt` loader.

**Files (new):** ~8 test classes + fixtures + `testsupport/Fixtures.kt`.

**Risks:** coverage exclusions (entrypoint/`Main.kt`, generated kotlinx-serialization companions). XML parser tests must hit the XXE-rejection path explicitly.

## Phase 4 — Task 4: Multi-Level Documentation

**Goal:** 4 docs with ≥3 Mermaid diagrams.

**Steps:**
1. Write `API_REFERENCE.md` — endpoints, request/response, error shapes, cURL examples (no permission needed; new file).
2. Write `ARCHITECTURE.md` — Mermaid component diagram, sequence diagram for "bulk import → classify → store", design notes.
3. Write `TESTING_GUIDE.md` — Mermaid test pyramid, how to run, fixtures map.
4. Re-confirm permission and rewrite stale `README.md`.

**Files (new/modified):** 3 new docs + 1 README rewrite (after permission).

## Phase 5 — Task 5: Integration & Performance Tests

**Steps:**
1. `IntegrationTest` (5): full ticket lifecycle, bulk import → auto-classify → query, combined filter by category + priority, malformed-import error pathways.
2. Concurrency (subset of integration or `ConcurrencyTest`): 20+ parallel `POST /tickets` via coroutines, assert no lost writes / consistent count.
3. `PerformanceTest` (5): import throughput, classifier latency p95, combined-filter latency, list endpoint at 1k tickets, with generous tolerances.

**Files (new):** `IntegrationTest.kt`, `PerformanceTest.kt`.

## Phase 6 — Demo & finalization

**Steps:**
1. `demo/sample_tickets.csv` (50 rows), `demo/sample_tickets.json` (20), `demo/sample_tickets.xml` (30), `demo/invalid/` (negative samples).
2. `demo/sample-requests.http` for IntelliJ HTTP runner.
3. `demo/demo.sh` — start app, exercise endpoints, log to `demo/demo.log` and `demo/app.log` (mirroring HW1's pattern).
4. Final pass on `HOWTORUN.md`.
5. Coverage screenshot at `docs/screenshots/test_coverage.png` — captured by user; agent generates the JaCoCo HTML report.

---

## Cumulative file changes (anticipated)

- ~15 Kotlin sources under `src/main/kotlin/homework2/...`
- ~10 Kotlin tests under `src/test/kotlin/homework2/...`
- `src/main/resources/openapi.yaml`
- 3 fixture files (CSV/JSON/XML) + invalid variants
- `gradle/libs.versions.toml` (CSV lib, JaCoCo plugin)
- `homework-2/build.gradle.kts` (apply jacoco, add CSV dep)
- `homework-2/HOWTORUN.md` (auto-update at each phase)
- 3 new docs: `API_REFERENCE.md`, `ARCHITECTURE.md`, `TESTING_GUIDE.md`
- `homework-2/README.md` rewrite (permission re-confirmed at Phase 4)
- `demo/*` set
