## Step 0 — Detailed Plan: Bootstrap

Here is the full plan before any code is written. Please review and approve.

---

### Context

- `build.gradle.kts` — exists, has Ktor + serialization, **missing** Commons CSV dep and JaCoCo plugin
- `libs.versions.toml` — exists, **missing** `commons-csv` version and `jacoco` plugin entry
- `src/` — has only a `.gitkeep`, completely empty
- `HOWTORUN.md` — placeholder, needs updating after the skeleton is runnable

---

### Files to create (8 new files)

| File | Purpose |
|---|---|
| `src/main/kotlin/homework2/entrypoint/Main.kt` | `main()` — starts embedded Netty server |
| `src/main/kotlin/homework2/entrypoint/ApplicationModule.kt` | `Application.module()` + parameterized overload for tests |
| `src/main/resources/openapi.yaml` | Stub spec (info block only, no paths yet) |
| `src/test/kotlin/homework2/testsupport/.gitkeep` | Reserves the testsupport package |
| `src/test/resources/fixtures/.gitkeep` | Reserves the fixtures directory |

Package directories created (no `.kt` files yet, just structure):
- `src/main/kotlin/homework2/{routing, service, models, validation, utils}`
- `src/test/kotlin/homework2/{routing, service, models, validation, utils}`

---

### Files to modify (2 files)

**`gradle/libs.versions.toml`** — add 2 entries:
```toml
[versions]
# existing entries unchanged ...
commonsCsv = "1.12.0"
jacoco    = "0.8.12"      # used for toolVersion in build.gradle.kts

[libraries]
# existing entries unchanged ...
commons-csv = { module = "org.apache.commons:commons-csv", version.ref = "commonsCsv" }

[plugins]
# existing entries unchanged ...
jacoco = { id = "jacoco" }   # built-in Gradle plugin, no external coordinate needed
```

**`homework-2/build.gradle.kts`** — add 3 things:
1. `alias(libs.plugins.jacoco)` in the `plugins {}` block (note: built-in so no version needed — just `jacoco` plugin id)
2. `implementation(libs.commons.csv)` in `dependencies {}`
3. `jacoco { toolVersion = "0.8.12" }` configuration block

**`homework-2/HOWTORUN.md`** — rewrite to reflect that the skeleton is now runnable:
```
./gradlew :homework-2:run          # starts server on port 8080
./gradlew :homework-2:build        # compile + test
./gradlew :homework-2:test         # tests only
```

---

### `Main.kt` design
```kotlin
package homework2.entrypoint

import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(Netty, port = 8080, module = { module() }).start(wait = true)
}
```

### `ApplicationModule.kt` design
```kotlin
package homework2.entrypoint

import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.swagger.*
import kotlinx.serialization.json.Json

// Default wiring — used at runtime
fun Application.module() = module(/* no collaborators yet */)

// Parameterized overload — used in tests to inject fakes
fun Application.module(
    // collaborators will be added here in Step 1+
) {
    install(ContentNegotiation) { json(Json { prettyPrint = true }) }
    swaggerUI(path = "swagger", swaggerFile = "openapi.yaml")
}
```

---

### Verification (what I'll run after execution)
```bash
./gradlew :homework-2:build --console=plain
```
Expected: `BUILD SUCCESSFUL` with 0 test failures (no tests yet).

---

### Risks
- JaCoCo's built-in Gradle plugin doesn't need a version in `libs.versions.toml` — I'll use `toolVersion` in the `jacoco {}` block instead. If the `alias(libs.plugins.jacoco)` approach causes issues with the version catalog, I'll fall back to `apply plugin: 'jacoco'` style.

---

**Awaiting your approval to execute.**