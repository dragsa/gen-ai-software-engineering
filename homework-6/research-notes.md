# Research Notes — context7 queries (Agent 2: Code generation)

During code generation, **context7** (configured in `mcp.json`, see Task 4) was used to look up the
chosen framework — Kotlin + `kotlinx.serialization` — for two decisions that shaped the pipeline.

> Reproduce these in Claude Code with the context7 MCP enabled and screenshot the results for
> `docs/screenshots/mcp-interaction.png`. The library IDs below are what context7 resolves for these
> libraries; confirm the exact ID shown in your run.

## Query 1: precise monetary arithmetic in Kotlin (BigDecimal)

- **Search:** "Kotlin BigDecimal money handling rounding" / resolve-library-id "kotlin"
- **context7 library ID:** `/jetbrains/kotlin` (stdlib + `java.math.BigDecimal` interop)
- **Applied:**
  - Amounts are parsed from JSON **strings** with `BigDecimal(String)` (never `Double`/`Float`),
    so `"9999.99"` keeps exact precision — see `BigDecimalSerializer.kt`.
  - Comparisons use Kotlin's operator mapping to `BigDecimal.compareTo` (e.g. `amount > HIGH_VALUE`,
    `amount <= BigDecimal.ZERO`) rather than `equals`, which is scale-sensitive.
  - Per-currency settlement totals accumulate with `BigDecimal.add` in `ReportingAgent.summarize`,
    and are serialized back as strings via `toPlainString()`.

## Query 2: custom serializer for a non-primitive type in kotlinx.serialization

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

Both lookups directly informed `BigDecimalSerializer.kt`, `TransactionData.kt`, and
`ReportingAgent.kt`, keeping all monetary logic on `BigDecimal` and all JSON on a single configured
`Json` instance.
