# 🏦 Homework 1: Banking Transactions API

> **Student Name**: Andrii Gnatiuk

> **Date Submitted**: fill in

> **AI Tools Used**:
> - Codex (actual code generation)
> - ChatGPT for brainstorming and cross-validation

> **Usage approach**:
> - docs/screenshots usage is structured as follows:
>  - step_number-prompt-tool_used-what_was_asked.xxx
>  - step_number-reply-tool_used-result.xxx
> - an iterative path was followed:
>   - no God-like prompts were used to implement the whole thing
>   - each prompt attempts to target a specific part of the implementation
>   - the next step is not necessarily built on top of the previous one, but rather targets to improve some aspect
> and get change which can be validated with ease (lack of a task list, yeah)
> - each commit contains screenshots of tool(s) in action (prompt and reply) and actual changes
 
> **Feedback**:
> - at some point it became obvious that there is a need for planning and correction ;)
> - on steps 6–10 it was found that the Codex tool was not able to generate the API and models according to the requirements
>and it took a lot of back and forth to get it to work

---

## 📋 Project Overview

The main goal of this project is to create a REST API that will allow users to perform CRUD operations on banking transactions.
One of the side quests is to not touch the itself code at all, but rather use the Codex tool to generate the API and do the validation.

## 🧱 Architecture Notes

### API and model choice

The implementation uses a clear separation between transport (HTTP API) and domain behavior:

- API create payload uses a single request model:
  - `fromAccount` and `toAccount` are optional at API level.
  - `amount`, `currency`, and `type` are mandatory.
  - optional client `status` is accepted but ignored.
- Domain create model is the source of truth and is sealed by transaction type:
  - `DepositCommand(toAccount, amount, currency)`
  - `WithdrawalCommand(fromAccount, amount, currency)`
  - `TransferCommand(fromAccount, toAccount, amount, currency)`
- Routing layer responsibilities:
  - decode request body,
  - run validation,
  - map API request to sealed domain command,
  - delegate to service.
- Concern splits validation:
  - structural validity from deserialization and typed mapping,
  - business validity from validator (amount rules, currency code, account format, account presence by `type`).
- Service layer responsibilities:
  - derive transaction `status` from business state (not from API input),
  - persist transactions in memory,
  - provide filtering, balance, and summary reads.
- The response model is consistent across read endpoints:
  - `Transaction` includes `id`, accounts, `amount`, `currency`, `type`, `timestamp`, `status`.
  - Task 4A summary endpoint (`GET /accounts/{accountId}/summary`) reuses the same `Transaction` response model and returns a list.

Why this model was chosen:

- It keeps the external API compact (single create schema and single create endpoint).
- It keeps domain rules strict and explicit using sealed command types.
- It avoids pushing business state ownership (`status`) to the client.
- It keeps endpoint behavior testable and deterministic via service-level logic.

### API behavior under concurrent access

The API uses in-memory process-local storage (`MutableList<Transaction>`) with synchronization. Behavior under concurrent requests is:

- Writes (`POST /transactions`) are synchronized:
  - status derivation and append happen inside one lock.
  - a transaction is never observed in a partially created state.
- Reads use immutable snapshots:
  - each read endpoint first captures a synchronized `toList()` snapshot,
  - filtering/aggregation/sorting then runs on that immutable copy.
  - each response is internally consistent for a single point-in-time view.
- Status derivation under concurrency:
  - available funds for withdrawal/transfer are computed from completed transactions visible inside the same synchronized write section.
  - this avoids race conditions where two concurrent writes could derive status from stale partially-updated state.
- Balance endpoint semantics:
  - failed transactions are excluded from balance calculations.
  - completed transactions are included.
- Summary endpoint semantics:
  - includes both completed and failed transactions for the requested account,
  - sorted by timestamp descending (newest first),
  - returns `200 []` for valid accounts with no transactions.

Important limits of this concurrency model:

- It is safe only within a single JVM process.
- There is no cross-instance coordination (no distributed lock / no shared database).
- Data is volatile and resets on application restart.
- It provides thread safety and per-request snapshot consistency, not database-grade transactional guarantees across multiple requests.

<div align="center">

*This project was completed as part of the AI-Assisted Development course.*

</div>
