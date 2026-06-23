# Research Notes — context7 queries (Agent 2: Code generation)

During development, **context7** (configured in `.mcp.json`, see Task 4) was used to look up the
build/framework docs for decisions that shaped the pipeline — the Kover coverage plugin, Kotlin
`BigDecimal`, and `kotlinx.serialization`.

> Query 1 (Kover) is the one captured in `docs/screenshots/04-mcp-context7.png` — context7 resolved
> the library ID `/kotlin/kotlinx-kover`. The other two are reproducible the same way (run the search
> in Claude Code with context7 enabled; confirm the exact ID shown in your run).

## Query 1: Kover coverage plugin (the 80% gate) — verified

- **Search:** "how to use kover plugin / library" → resolve-library-id "kover"
- **context7 library ID:** `/kotlin/kotlinx-kover`
- **Applied:**
  - Applied `id("org.jetbrains.kotlinx.kover")` in `homework-6/build.gradle.kts` (via the version catalog).
  - Used the `kover { reports { verify { rule { minBound(80) } } } }` DSL for the coverage gate, run as
    `koverVerify` — wired into the pre-push hook with `-PenforceCoverage`.
  - Noted `koverHtmlReport` / `koverXmlReport` for local coverage inspection.

## Query 2: precise monetary arithmetic in Kotlin (BigDecimal)

- **Search:** "Kotlin BigDecimal money handling rounding" / resolve-library-id "kotlin"
- **context7 library ID:** `/jetbrains/kotlin` (stdlib + `java.math.BigDecimal` interop)
- **Applied:**
  - Amounts are parsed from JSON **strings** with `BigDecimal(String)` (never `Double`/`Float`),
    so `"9999.99"` keeps exact precision — see `BigDecimalSerializer.kt`.
  - Comparisons use Kotlin's operator mapping to `BigDecimal.compareTo` (e.g. `amount > HIGH_VALUE`,
    `amount <= BigDecimal.ZERO`) rather than `equals`, which is scale-sensitive.
  - Per-currency settlement totals accumulate with `BigDecimal.add` in `ReportingAgent.summarize`,
    and are serialized back as strings via `toPlainString()`.

## Query 3: custom serializer for a non-primitive type in kotlinx.serialization

- **Search:** "kotlinx.serialization custom KSerializer for BigDecimal" / resolve-library-id "kotlinx.serialization"
- **context7 library ID:** `/kotlin/kotlinx.serialization`
- **Applied:**
  - Implemented `object BigDecimalSerializer : KSerializer<BigDecimal>` with a
    `PrimitiveSerialDescriptor("BigDecimal", PrimitiveKind.STRING)`, encoding via `encodeString` and
    decoding via `decodeString` — so `BigDecimal` round-trips as a JSON string.
  - Wired it onto the model field with `@Serializable(with = BigDecimalSerializer::class)` on the
    nullable `amount: BigDecimal?`, which kotlinx wraps as a nullable serializer automatically.
  - Configured the shared `Json { prettyPrint = true; encodeDefaults = true; ignoreUnknownKeys = true }`
    instance (`MessageIo.json`) so accumulated fields (`status`, `risk_score`, …) are emitted and
    unknown keys in raw input are tolerated.

## Outcome

The Kover lookup shaped the coverage gate (`build.gradle.kts` + pre-push hook); the BigDecimal and
serialization lookups informed `BigDecimalSerializer.kt`, `TransactionData.kt`, and
`ReportingAgent.kt`, keeping all monetary logic on `BigDecimal` and all JSON on a single configured
`Json` instance.
