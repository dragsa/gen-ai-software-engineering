# Homework-5 — Implementation Roadmap (TASKS_ROADMAP)

A phased, checklist-driven plan to configure **three external MCP servers** (GitHub, Filesystem, Jira **or** Notion) and build **one custom MCP server in Kotlin**, with **screenshot evidence as the quality gate** between every task.

> **Stack note / explicit override:** `homework-5/TASKS.md` (read-only) literally specifies a Python **FastMCP** server (`server.py`, `requirements.txt`/`fastmcp`). Per explicit user instruction, the custom server is implemented in **Kotlin** instead, aligning with the mandated stack in `.agents/docs/STACK.MD` (Kotlin 2.3.20 / JVM 21, Gradle Kotlin DSL, version catalog). FastMCP has no Kotlin equivalent, so the **official MCP Kotlin SDK** (`io.modelcontextprotocol:kotlin-sdk`) is used to expose the Resource and `read` Tool. This override (Kotlin instead of FastMCP) must be noted in `README.md`. No cross-subproject coupling is introduced.

---

## Global Conventions (apply to every phase)

### MCP configuration is PROJECT-LOCAL (mandatory)

- All MCP servers are registered in a **project-scoped** config committed to the repo: `homework-5/.mcp.json` (Claude Code project scope) — **not** the user/global config (`~/.claude.json` / `~/.config`).
- The **Filesystem** MCP path points at a **local project directory** (e.g. this repo or `homework-5/`), using a path relative to the project where the client allows it.
- The **custom** MCP server is launched from inside the project (`homework-5/custom-mcp-server/`) using a project-relative command.
- Secrets/tokens are **never** committed: reference them via environment variables in `.mcp.json` and document the variables in `HOWTORUN.md`. Add any real env file to `.gitignore`.

Checklist:

- [ ] `homework-5/.mcp.json` exists and is the single source of truth for all four servers
- [ ] No server is configured only in global/user scope
- [ ] Filesystem server path resolves to a local project directory
- [ ] No tokens or secrets are hard-coded in committed files
- [ ] `.gitignore` excludes any local secret/env files

### Screenshot quality gate (mandatory between tasks)

Each task is **not complete** — and the next task must **not start** — until its screenshot evidence exists in `homework-5/docs/screenshots/` and clearly shows a successful MCP interaction (request + result). This is the gate enforced throughout this roadmap.

Checklist (repeated per task below):

- [ ] Required screenshot(s) saved to `docs/screenshots/` with the exact expected filename
- [ ] Screenshot shows both the prompt/request and the successful result
- [ ] Sensitive data redacted (IDs/numbers only where required, e.g. Task 3)
- [ ] Gate confirmed before moving to the next task

### Command & tool logging (mandatory)

Every command run and tool used to **add/configure each MCP server** is recorded under `homework-5/docs/logs/`. This is an audit trail of *how* each server was wired up, separate from the screenshot evidence of results.

- One log file per task: `docs/logs/task-1-github.md`, `task-2-filesystem.md`, `task-3-jira-or-notion.md`, `task-4-custom.md`.
- Each entry captures: the **command(s)** run (install, `claude mcp add ...`, config edits, server start), the **client/tool** used (e.g. Claude Code CLI, MCP Inspector), the working directory, and the **result/output** (success or error + fix).
- Keep secrets/tokens redacted in logs (use `***` or env-var names).
- Logs are committed to the repo as part of the deliverables.

Checklist (repeated per task below):

- [ ] Per-task log file created in `docs/logs/` capturing commands + tools used to add the MCP
- [ ] Log includes command output / result (success or error+resolution)
- [ ] Secrets redacted in the log

---

## Phase 0 — Scaffolding & Project-Local Config

**Goal:** Establish the folder contract and the project-local MCP config shell.

- Create the `homework-5/` structure: `custom-mcp-server/`, `docs/screenshots/`, `docs/logs/`.
- Seed `homework-5/.mcp.json` with empty/placeholder server blocks (filled per phase).
- Seed `README.md` (description + author name placeholder) and `HOWTORUN.md` (install/run/connect/usage placeholders).
- Add `.gitignore` entries for secrets and build artifacts (`.gradle/`, `build/`, `*.env`).

**Files:** `homework-5/.mcp.json`, `README.md`, `HOWTORUN.md`, `.gitignore`, `docs/logs/`, folder tree.

Checklist:

- [ ] Folder tree matches the Expected Project Structure in `TASKS.md` (plus `docs/logs/`)
- [ ] `docs/logs/` folder created for command/tool audit trail
- [ ] `.mcp.json` is valid JSON and parses in the target client
- [ ] `README.md` and `HOWTORUN.md` placeholders created
- [ ] `.gitignore` covers secrets + build artifacts

**Risks:** Committing the wrong (global) config; invalid JSON breaking client startup.

---

## Phase 1 — Task 1: GitHub MCP ⭐

**Goal:** Connect the client to GitHub via the official GitHub MCP server, registered project-locally.

- Add the GitHub MCP server block to `homework-5/.mcp.json`.
- Provide a GitHub token via env var (`GITHUB_PERSONAL_ACCESS_TOKEN` or equivalent), referenced in config — not hard-coded.
- Restart/reload the client and confirm the server registers without errors.
- Perform **at least one** successful interaction against your repository.

**Verification — choose at least one (alternative approaches accepted):**

- [ ] **Option A (read):** Prompt the client to *list recent pull requests* of the repo and capture the response.
- [ ] **Option B (read):** Prompt it to *summarize the latest commits* on a branch.
- [ ] **Option C (write):** Ask it to *create an issue* and confirm it appears in GitHub.

Checklist:

- [ ] GitHub MCP block added to project-local `.mcp.json`
- [ ] Token supplied via env var (not committed)
- [ ] Server registers/starts with no errors (verify via client's MCP/server status)
- [ ] At least one verification option above succeeds
- [ ] **Log:** `docs/logs/task-1-github.md` records commands + tools used to add the server (secrets redacted)
- [ ] **Quality gate:** `docs/screenshots/01-reply-github-mcp-result.png` saved showing request + result
- [ ] Gate confirmed before starting Phase 2

**Risks:** Token scope too narrow (read vs. write); rate limits; org SSO authorization required.

---

## Phase 2 — Task 2: Filesystem MCP ⭐

**Goal:** Connect the client to a **local project directory** via the Filesystem MCP server.

- Add the Filesystem MCP server block to `homework-5/.mcp.json`, with the allowed path set to a **local** directory (e.g. the repo root or `homework-5/`).
- Restart/reload and confirm registration.
- Perform **at least one** successful interaction.

**Verification — choose at least one (alternatives accepted):**

- [ ] **Option A:** Prompt the client to *list files* in the configured directory.
- [ ] **Option B:** Prompt it to *read a specific file* (e.g. `homework-5/TASKS.md`) and show contents.
- [ ] **Option C:** Prompt it to *summarize the directory structure*.

Checklist:

- [ ] Filesystem MCP block added with a **local** path (requirement)
- [ ] Path is project-scoped, not an unrelated global location
- [ ] Server registers/starts with no errors
- [ ] At least one verification option above succeeds
- [ ] **Log:** `docs/logs/task-2-filesystem.md` records commands + tools used to add the server
- [ ] **Quality gate:** `docs/screenshots/02-reply-filesystem-mcp-result.png` saved showing request + result
- [ ] Gate confirmed before starting Phase 3

**Risks:** Over-broad path (granting access beyond the project); path separators on Windows vs. Unix; permission denials.

---

## Phase 3 — Task 3: Jira **or** Notion MCP ⭐⭐

**Goal:** Connect the client to Jira **or** Notion and run the required "last 5 bugs" query.

- Decide **Jira or Notion** (pick the one you have a real project/workspace in).
- Add the chosen MCP server block to `homework-5/.mcp.json` with credentials via env vars.
- Restart/reload and confirm registration.
- **Make the required request:** *"Give me the tickets/pages of the last 5 bugs on a project"* against a real project.
- Capture the full response; **redact** sensitive content — show only ticket keys / page numbers.

**Verification — alternative approaches by platform:**

- [ ] **Jira path:** request returns the last 5 bug **tickets** (e.g. `PROJ-123`), optionally cross-checked via a JQL-style query (`issuetype = Bug ORDER BY created DESC`).
- [ ] **Notion path:** request returns the last 5 bug **pages** (page IDs/titles), optionally cross-checked against the bug database/view.

Checklist:

- [ ] Platform chosen (Jira or Notion) and noted in README/HOWTORUN
- [ ] MCP block added to project-local `.mcp.json`; credentials via env vars
- [ ] Server registers/starts with no errors
- [ ] The exact "last 5 bugs" request returns valid results
- [ ] Response redacted to IDs/numbers only (no sensitive info)
- [ ] **Log:** `docs/logs/task-3-jira-or-notion.md` records commands + tools used to add the server (secrets redacted)
- [ ] **Quality gate:** `docs/screenshots/03-reply-jira-or-notion-mcp-result.png` saved showing request + redacted response
- [ ] Gate confirmed before starting Phase 4

**Risks:** API token/permission scope; project key/database ID discovery; accidental exposure of sensitive ticket content in the screenshot.

---

## Phase 4 — Task 4: Custom MCP Server in Kotlin (MCP Kotlin SDK) ⭐⭐⭐

**Goal:** Build a custom MCP server in **Kotlin** exposing a **Resource** and a `read` **Tool** over `lorem-ipsum.md`, wired into the project-local config. (FastMCP equivalent: official **MCP Kotlin SDK**, `io.modelcontextprotocol:kotlin-sdk`.)

### 4.1 — Implement the server

- Register `homework-5` as a Gradle subproject in the root `settings.gradle.kts`; add `homework-5/build.gradle.kts` via the version catalog (`application` plugin, `mainClass` set to the server entrypoint).
- Add the MCP Kotlin SDK dependency to `gradle/libs.versions.toml` and reference it from the subproject build.
- Create the server under `custom-mcp-server/` (or `src/main/kotlin/homework5/...` per STACK.MD package layout) — e.g. an `entrypoint` `main()` that starts an MCP server over **stdio**.
- Create `custom-mcp-server/lorem-ipsum.md` with enough text (well over 30 words).
- Add a **Resource** URI that reads `lorem-ipsum.md`, accepts `word_count` (**default 30**), and returns exactly that many words.
- Add a **Tool** named `read` with an optional `word_count` param that returns the resource content.

Checklist:

- [ ] `homework-5` registered in root `settings.gradle.kts`; `build.gradle.kts` uses the version catalog
- [ ] MCP Kotlin SDK (`io.modelcontextprotocol:kotlin-sdk`) added to `gradle/libs.versions.toml` and the subproject
- [ ] Kotlin server entrypoint (`main()`) starts an MCP server (stdio transport)
- [ ] No wildcard imports (CODESTYLE.MD); package matches directory structure
- [ ] `lorem-ipsum.md` present with > 30 words of source text
- [ ] Resource reads the file, honors `word_count` (default 30), returns exactly N words
- [ ] `read` tool present with optional `word_count`, returns resource content

### 4.2 — Wire into project-local config & document

- Add the custom server block to `homework-5/.mcp.json` using a **project-relative** launch command (e.g. `./gradlew :homework-5:run -q` from the repo root, or a built jar invoked with `java -jar`).
- Write `HOWTORUN.md`: build the server (`./gradlew :homework-5:build`), run it, connect MCP config, and use/test the `read` tool.
- Add the short doc explanation: **Resources** = URIs Claude can read from (files, APIs); **Tools** = actions Claude can call to perform operations.

Checklist:

- [ ] Custom server registered in project-local `.mcp.json` with a project-relative command
- [ ] `HOWTORUN.md` covers build / run / connect / use-and-test
- [ ] Resources-vs-Tools explanation included in docs

### 4.3 — Verify (alternative approaches accepted)

- [ ] **Option A (build + direct run):** `./gradlew :homework-5:build` passes (zero errors per AGENTS.md), then start the server and confirm it boots without errors.
- [ ] **Option B (MCP Inspector):** Launch the server under the MCP Inspector, list resources/tools, and call `read`; confirm word-limited output.
- [ ] **Option C (client end-to-end):** From the client, call the `read` tool (default 30 and a custom `word_count`) and confirm exactly N words returned.

Checklist (all must hold):

- [ ] `./gradlew :homework-5:build` passes with zero errors (warnings investigated)
- [ ] Startup command works and is verified
- [ ] `.mcp.json` is valid and points at the custom server
- [ ] MCP Kotlin SDK confirmed present in the version catalog + subproject dependencies
- [ ] `read` with default returns 30 words; with custom value returns exactly that many
- [ ] **Log:** `docs/logs/task-4-custom.md` records build/run commands + tools used to add the server
- [ ] **Quality gate:** `docs/screenshots/04-reply-custom-mcp-read-tool-result.png` saved showing a successful `read` call/result
- [ ] Gate confirmed before starting Phase 5

**Risks:** Word-count off-by-one (tokenization/whitespace edge cases); MCP Kotlin SDK version/API drift; stdio transport must match the client config; default not applied when param omitted; Gradle `mainClass`/run wiring for a long-lived stdio process.

---

## Phase 5 — Evidence, Docs & Submission

**Goal:** Package all deliverables for review.

- Confirm all four screenshots exist in `docs/screenshots/` with the expected filenames.
- Confirm all four command/tool logs exist in `docs/logs/` and are secret-free.
- Finalize `README.md`: description, author name, platform chosen for Task 3, and the **Kotlin-instead-of-FastMCP override rationale** — **request permission before modifying `README.md` per AGENTS.md.**
- Ensure `HOWTORUN.md` reflects the actual commands and env vars.
- Open a PR with a summary and the screenshots; commit `.mcp.json`, `custom-mcp-server/`, dependencies, and `docs/`.

Checklist:

- [ ] All 4 screenshots present and correctly named:
  - [ ] `01-reply-github-mcp-result.png`
  - [ ] `02-reply-filesystem-mcp-result.png`
  - [ ] `03-reply-jira-or-notion-mcp-result.png`
  - [ ] `04-reply-custom-mcp-read-tool-result.png`
- [ ] All 4 command/tool logs present in `docs/logs/`:
  - [ ] `task-1-github.md`
  - [ ] `task-2-filesystem.md`
  - [ ] `task-3-jira-or-notion.md`
  - [ ] `task-4-custom.md`
- [ ] `README.md` finalized (author + deviation note) — **after explicit user approval**
- [ ] `HOWTORUN.md` accurate and reproducible
- [ ] All configs/servers committed; secrets excluded
- [ ] PR opened with summary + screenshots

**Risks:** README edits gated on user approval; forgetting to commit `.mcp.json` or the custom server; leaking secrets in the PR diff.

---

## Final Deliverables Checklist (maps to TASKS.md)

- [ ] Project-local `.mcp.json` registers all **four** servers
- [ ] Kotlin custom MCP server with Resource + `read` Tool (MCP Kotlin SDK)
- [ ] MCP Kotlin SDK declared in `gradle/libs.versions.toml` + `homework-5/build.gradle.kts`
- [ ] `custom-mcp-server/lorem-ipsum.md` source text
- [ ] `docs/screenshots/` with all four result screenshots
- [ ] `docs/logs/` with all four command/tool audit logs (secrets redacted)
- [ ] `README.md` (description + author) and `HOWTORUN.md` (install/run/connect/usage)

---

## Cross-Cutting Constraints

- MCP configuration **project-local**, never global; Filesystem path **local**; secrets via env vars only.
- **Screenshot evidence is the quality gate** — no task is done, and no next task starts, without it.
- **Command/tool logging is mandatory** — every step to add an MCP is recorded in `docs/logs/` (secrets redacted).
- `TASKS.md` is read-only; `README.md` edits require user permission (AGENTS.md).
- Subproject isolation: no cross-subproject coupling; minimal, scoped changes.
- Per project CLAUDE.md: Plan → **Approve** → Execute. This roadmap is the plan; execution awaits approval.
