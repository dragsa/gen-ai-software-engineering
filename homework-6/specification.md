# Multi-Agent Banking Transaction Pipeline Specification

> Ingest the information from this file, implement the Low-Level Tasks, and generate the code that will satisfy the High and Mid-Level Objectives.

## High-Level Objective

Build a Kotlin multi-agent pipeline that ingests raw banking transactions from `sample-transactions.json`, validates them, scores them for fraud risk, and produces an aggregated run summary, with every agent communicating exclusively through JSON message files in `shared/`.

## Mid-Level Objectives

- `TransactionValidator` rejects any transaction with a missing required field, a non-positive amount, or a currency outside the ISO 4217 allow-list, and marks all others `validated` (verified against `sample-transactions.json`: TXN006 rejected for currency `XYZ`, TXN007 rejected for amount `-100.00`).
- `FraudDetector` assigns a risk outcome to every `validated` transaction: `flagged_for_review` for amount > $10,000 (TXN002, TXN005) or odd-hour (00:00–04:59) cross-border activity (TXN004), and records a near-threshold note for amounts within $0.01–$1,000 of the $10,000 limit (TXN003); all other validated transactions reach `settled`.
- `ReportingAgent` aggregates every terminal message in `shared/results/` into one pipeline summary report containing total/valid/rejected/flagged counts, per-currency settled totals, a CTR-style list of transactions ≥ $10,000, and a cross-border list — and marks the run `summarized`.
- Every runtime agent uses all four `shared/` directories as a strict claim/release lifecycle: it atomically moves a message from its inbox into `shared/processing/` before working on it, then writes the result to the next stage and deletes the claimed file from `shared/processing/` — so `shared/processing/` only ever holds in-flight messages, never a backlog or a leftover.
- The `Integrator` orchestrates the full run end-to-end (seed `shared/input/` from `sample-transactions.json`, drive each agent in order through the `input/processing/output/results` lifecycle) such that every transaction in `sample-transactions.json` has exactly one terminal record in `shared/results/` and `shared/processing/` is empty after a single run.
- Every agent decision is recorded as an audit log line (ISO-8601 timestamp, agent name, transaction ID, outcome) with no plaintext account numbers or names, and the full test suite reaches ≥ 90% line coverage.

## Implementation Notes

- Monetary values use `java.math.BigDecimal` with `RoundingMode.HALF_UP` exclusively — `Double`/`Float` are forbidden anywhere amounts are parsed, compared, or summed. Amounts are read from JSON as strings and converted via `BigDecimal(String)`.
- Currency codes are validated against a fixed ISO 4217 allow-list (USD, EUR, GBP, JPY, CHF, CAD, AUD, plus any others needed by the sample data); anything outside the allow-list is rejected, not silently coerced.
- **Message lifecycle (all four `shared/` directories are mandatory, not just `input`/`output`/`results`):** an agent claims a message by atomically moving it from its inbox (`shared/input/` for `TransactionValidator`, `shared/output/` for `FraudDetector`) into `shared/processing/` before doing any work. Once a decision is made, the agent writes the produced message to the next stage (`shared/output/` or `shared/results/`) and then removes the claimed file from `shared/processing/`. `shared/processing/` must contain only messages currently being worked on — never a queue, never an orphaned file after a run completes. The move-in and move-out are each a single atomic file operation (e.g. `Files.move`) so a message is never visible in two directories at once.
- Every agent operation appends one audit log line per decision in the format `timestamp(ISO-8601) | agent | transaction_id | outcome`. Account numbers are masked (e.g. last 4 digits only) and customer names are never logged — only `transaction_id` and structural fields appear in logs.
- All Kotlin source lives in a single flat `homework6` package under `src/main/kotlin/homework6/` (per `agents.md`); no wildcard imports (per `CODESTYLE.MD`); each runtime agent exposes a pure `fun process(message: AgentMessage): AgentMessage` plus a standalone `main()` for running as an independent process.
- Messages are `kotlinx.serialization`-annotated data classes serialized as JSON, matching the standard envelope (`message_id`, `timestamp`, `source_agent`, `target_agent`, `message_type`, `data`) defined in `agents.md`.
- Cross-border is defined as `metadata.country != "US"`; odd-hour is defined as the UTC hour of `timestamp` falling in `[0, 5)`.
- Build verification after each implementation task: `./gradlew :homework-6:build` must pass with zero errors (per `AGENTS.MD`).

## Context

### Beginning context
- `sample-transactions.json` — 8 raw transaction records (`TXN001`–`TXN008`) covering valid transfers, two high-value wires (`TXN002`, `TXN005`), a near-threshold transfer (`TXN003`), an odd-hour cross-border transfer (`TXN004`), an invalid-currency transaction (`TXN006`), and a negative-amount transaction (`TXN007`).
- `agents.md` — runtime agent responsibilities, decision rules, and the `shared/` file-based communication protocol.
- No existing pipeline code; `shared/` directories (`input/`, `processing/`, `output/`, `results/`) do not yet exist.

### Ending context
- `src/main/kotlin/homework6/` containing `AgentMessage` (and related models), `TransactionValidator`, `FraudDetector`, `ReportingAgent`, and `Integrator`.
- `shared/results/` populated with one terminal JSON message per input transaction, plus a pipeline summary report (consumed later by the `pipeline://summary` MCP resource); `shared/processing/` empty at rest.
- `src/test/kotlin/homework6/` with unit tests per agent and one integration test for the full pipeline, reaching ≥ 90% line coverage (gate enforced at ≥ 80% by the coverage hook).
- `research-notes.md` documenting ≥ 2 context7 queries used during code generation.

## Low-Level Tasks

### 1. TransactionValidator

Task: TransactionValidator
Prompt: "Create `TransactionValidator` in the `homework6` package implementing `fun process(message: AgentMessage): AgentMessage`. It must check that `transaction_id`, `amount`, `currency`, `source_account`, and `destination_account` are present and non-blank, that `amount` parses as a positive `BigDecimal` (never `Double`), and that `currency` is in the ISO 4217 allow-list. On success set status `validated` and `target_agent` to `fraud_detector`; on failure set status `rejected` with a `data.reason` field describing the first failing check. Append one audit log line per decision (`timestamp | TransactionValidator | transaction_id | outcome`) without logging account numbers or names. Add a `main()` that, for each message in `shared/input/`: atomically moves it into `shared/processing/` (claim), calls `process`, writes the result to `shared/output/` (validated) or `shared/results/` (rejected), then deletes the claimed copy from `shared/processing/` (release)."
File to CREATE: src/main/kotlin/homework6/TransactionValidator.kt
Function to CREATE: fun process(message: AgentMessage): AgentMessage
Details: Reject TXN006 (currency `XYZ` not in the ISO 4217 allow-list) and TXN007 (amount `-100.00` ≤ 0); validate the remaining six sample transactions. Use `BigDecimal(String)` for amount parsing and comparison; mask the last 4 digits of account numbers only when an account number must appear in a log line. `shared/processing/` must never retain the file once `main()` returns for that message.

### 2. FraudDetector

Task: FraudDetector
Prompt: "Create `FraudDetector` in the `homework6` package implementing `fun process(message: AgentMessage): AgentMessage`. For every `validated` message, compute: high-value (amount > 10000.00), odd-hour (UTC hour of timestamp in [0,5)), cross-border (`metadata.country != \"US\"`), and near-threshold (10000.00 - amount is between 0.01 and 1000.00 inclusive). Set status `flagged_for_review` with a `data.risk_reasons` list when high-value, or when odd-hour AND cross-border are both true; otherwise set status `settled`. Always include a `data.near_threshold` boolean. Append one audit log line per decision in the same masked format as the validator. Add a `main()` that, for each `validated` message in `shared/output/`: atomically moves it into `shared/processing/` (claim), calls `process`, writes the result to `shared/results/`, then deletes the claimed copy from `shared/processing/` (release)."
File to CREATE: src/main/kotlin/homework6/FraudDetector.kt
Function to CREATE: fun process(message: AgentMessage): AgentMessage
Details: TXN002 ($25,000.00) and TXN005 ($75,000.00) are flagged as high-value; TXN004 (02:47 UTC, country `DE`) is flagged as odd-hour cross-border; TXN003 ($9,999.99) is `settled` but marked `near_threshold = true`; TXN001 and TXN008 settle with no flags. `shared/output/` must no longer contain the message once it has been claimed into `shared/processing/`.

### 3. ReportingAgent

Task: ReportingAgent
Prompt: "Create `ReportingAgent` in the `homework6` package implementing `fun process(message: AgentMessage): AgentMessage` that takes a synthetic `report_request` message, reads every terminal JSON message in `shared/results/`, and produces a pipeline summary: total count, counts by status (`validated`/`rejected`/`settled`/`flagged_for_review`), per-currency sum of settled amounts (as `BigDecimal`, formatted as strings), a CTR-style list of transaction IDs with amount ≥ 10000.00, and a list of cross-border transaction IDs. Return a message with status `summarized` and the report under `data.summary`, and also write the same report as a standalone JSON/text file under `shared/results/pipeline-summary.json` for the MCP `pipeline://summary` resource. Append an audit log line for the report generation itself. The report request message itself follows the same claim lifecycle: stage it through `shared/processing/` while the report is being generated, then remove it once `shared/results/pipeline-summary.json` is written. Add a `main()` that runs this after the other two agents complete."
File to CREATE: src/main/kotlin/homework6/ReportingAgent.kt
Function to CREATE: fun process(message: AgentMessage): AgentMessage
Details: Summary must be derivable purely from `shared/results/` contents so it can be regenerated independently of the live run; per-currency totals only include `settled` transactions; CTR list and cross-border list are derived from `data` fields already present on terminal messages (no re-computation of fraud rules); `ReportingAgent` reads `shared/results/` but does not consume or remove the terminal transaction records themselves, only its own transient `report_request` passes through `shared/processing/`.

### 4. Integrator (orchestrator)

Task: Integrator
Prompt: "Create `Integrator` in the `homework6` package with a `main()` that: (1) creates `shared/input/`, `shared/processing/`, `shared/output/`, `shared/results/` if absent, (2) loads every record from `sample-transactions.json`, wraps each as an `AgentMessage` with `message_type = \"transaction\"` and `target_agent = \"transaction_validator\"`, and writes it to `shared/input/`, (3) invokes `TransactionValidator.process` on each message, observing its claim into `shared/processing/` and release into `shared/output/` or `shared/results/`, (4) invokes `FraudDetector.process` on each `validated` message from `shared/output/`, observing the same claim/release through `shared/processing/`, with all terminal results (`settled`/`flagged_for_review`) landing in `shared/results/`, (5) invokes `ReportingAgent.process` to generate the final summary, and (6) prints a run summary to stdout, asserting `shared/processing/` is empty at the end of the run. Expose the orchestration as a plain function (not only inside `main()`) so the integration test can call it against a temp `shared/` directory."
File to CREATE: src/main/kotlin/homework6/Integrator.kt
Function to CREATE: fun process(message: AgentMessage): AgentMessage
Details: The `Integrator` does not implement business rules itself — it only sequences `TransactionValidator`, `FraudDetector`, and `ReportingAgent` and manages file movement between `shared/` subdirectories, including verifying `shared/processing/` is drained (empty) after each agent's pass and at the end of the run. After a single run, every one of the 8 sample transactions must have exactly one record in `shared/results/`, and `shared/results/pipeline-summary.json` must exist.