## Purpose
This document defines the **mandatory technology stack and project conventions** for any new subproject in this repository.

It complements `AGENTS.MD` (project rules and file contract). Where this document and `AGENTS.MD` overlap, `AGENTS.MD` takes precedence.

---

## Mandatory Stack

All new subprojects **must** use the following.

### Language & runtime
- Kotlin **2.3.20**
- JVM toolchain **21**

### Build
- **Gradle** with **Kotlin DSL** (`*.gradle.kts`)
- **Multi-project** layout: subprojects are included from the root `settings.gradle.kts`
- **Version catalog** at `gradle/libs.versions.toml` is the single source of truth for dependency and plugin versions
- Root `build.gradle.kts` declares plugins with `apply false`; subprojects opt in via `alias(libs.plugins.<name>)`
- Repository configuration is centralized in `settings.gradle.kts` with `repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)`
- Subprojects apply the `application` plugin and set `mainClass`

### Server
- **Ktor 3.4.3** with the **Netty** engine (embedded server)
- `ktor-server-content-negotiation` with `kotlinx-serialization-json`
- `ktor-server-swagger` for hosted API documentation

### Serialization
- **`kotlinx.serialization` 1.9.0** (JSON)

### Testing
- **`kotlin-test-junit`** (matched to the Kotlin version)
- **`ktor-server-test-host`** with `testApplication` for HTTP-level integration tests

---

## Repository Layout

```
<root>/
├── settings.gradle.kts          # includes each subproject; centralizes repositories
├── build.gradle.kts             # plugin declarations with apply false
├── gradle.properties
├── gradle/
│   └── libs.versions.toml       # version catalog
├── gradlew, gradlew.bat
└── <subproject>/
    ├── build.gradle.kts         # subproject build
    ├── src/main/kotlin/...
    ├── src/main/resources/...
    ├── src/test/kotlin/...
    └── src/test/resources/...
```

Subproject isolation and shared-code rules are governed by `AGENTS.MD` and are not restated here.

---

## Package Layout

Each subproject must organize source code under a single root package matching the subproject name (e.g. `homework1`, `homework2`) with the following sub-packages:

- **`entrypoint`** — `main()` and the Ktor `Application.module()` extension function. The module function must expose a parameterized overload that accepts collaborators (services, validators, etc.), so tests can substitute implementations without rewiring.
- **`routing`** — per-resource route registration as `Route.register<Resource>Routes(...)` extension functions, wired in `Application.module()`.
- **`service`** — service interfaces and their implementations. Routes depend on the interface, not the implementation.
- **`models`** — data models, enums, and any custom serializers.
- **`validation`** — validators that return structured error lists.
- **`utils`** — narrowly scoped helpers used within the subproject only. Utility-dump packages are not allowed.

Tests mirror the same package structure under `src/test/kotlin/`. Test fixtures live under `src/test/resources/fixtures/` and are loaded through a dedicated `testsupport` package within tests.

---

## Modeling Conventions

### Separation of API and domain models
- API models (request/response DTOs) and domain models **must be separate types**.
- API models live at the transport boundary, are shaped to the HTTP contract, and are `@Serializable`.
- Domain models represent internal business state and are not directly exposed to the HTTP layer.
- A mapping step (typically inside the routing or validation layer) converts validated API requests into domain commands or entities before the service is invoked.
- Direct exposure of domain types as request or response payloads is not allowed.

---

## Validation Conventions

- Validators return a structured list of validation errors. They do **not** throw for business validation failures.
- A single, consistent error response shape is used across the subproject for all validation failures.
- Deserialization (structural) failures and business-validation failures are both surfaced through the same error response shape, so clients have one contract to handle.

---

## HTTP & Documentation Conventions

- The OpenAPI specification is checked in as a static YAML file under `src/main/resources/openapi.yaml`.
- The spec is served raw at `/openapi.yaml` and rendered via Swagger UI at `/swagger` using `ktor-server-swagger`.
- The OpenAPI spec **must be kept in sync** with implemented endpoints. Spec drift is treated as a defect.

---

## Testing Conventions

- HTTP integration tests use Ktor `testApplication { application { module(...) } }` against the parameterized `module()` overload.
- Unit tests cover services, validators, and parsing helpers.
- Test fixtures live in `src/test/resources/fixtures/` and are loaded through a single `testsupport` helper.
- Tests are runnable via `./gradlew :<subproject>:test --rerun-tasks --console=plain`.

---

## Run & Demo Conventions

- The application is started via `./gradlew :<subproject>:run` (Gradle `application` plugin, `mainClass` set to the subproject's `Main` entrypoint).
- Each subproject provides a `demo/` folder containing:
  - a runnable end-to-end script (`demo.sh` or equivalent),
  - an executable HTTP request file (`sample-requests.http`),
  - reusable payload data (`sample-data.json` or equivalent).
- `HOWTORUN.md` (per the file contract in `AGENTS.MD`) must reflect these commands.

---

## Subproject File Contract

Governed by `AGENTS.MD`. Not restated here.

---

## Deviation Policy

- The mandatory stack and conventions in this document apply to all new subprojects.
- A subproject may deviate **only** if its `TASKS.md` explicitly mandates a different language, runtime, or framework.
- Any deviation must be documented in that subproject's `README.md`, including the rationale and which conventions still apply.
