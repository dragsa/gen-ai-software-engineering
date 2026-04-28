Got it. Full original reply, unchanged text. Only the suggested AGENTS.md is wrapped as a code block.

⸻

Your prompt is directionally correct, but it has two structural flaws:

1. It mixes policy (global rules) with repo conventions (structure rules) without hierarchy
2. It’s written as a prompt, not as a stable instruction artifact

If you leave it like this → you’ll get drift again.

Below is a clean, minimal AGENTS.md that Codex / agents can actually follow consistently.

# AGENTS.md
## Purpose
This document defines the minimal operational rules for any agent interacting with this repository.
It is intentionally simple and generic.
It applies to any agent regardless of implementation (LLM, tool-based, or hybrid).
---
## Authority & Mutability
- This file is **read-only**.
- It **must not be modified** unless explicitly instructed.
---
## Project Structure Principles
### Subproject Isolation
- Each subproject is **independent**.
- Subprojects **must not depend on each other**.
### Shared Code
- Shared logic is allowed only via **libraries**.
- Libraries must contain the **minimal required functionality only**.
- Avoid broad or “utility dump” libraries.
---
## Subproject Contract
Each subproject must contain the following files:
### TASKS.md
- **Read-only**
- Defines tasks and requirements
- Must not be modified under any circumstances
### HOWTORUN.md
- **Editable**
- Must reflect the current way to run the project
- Must be updated automatically when changes affect execution
- No permission required for updates
### README.md
- **Editable**
- Describes the project and its usage
- Must be updated when functionality changes
- **Permission must be requested before modifying**
---
## Modification Rules
When making changes:
- Do not modify files marked as read-only
- Keep changes **minimal and scoped**
- Do not introduce cross-subproject coupling
- Do not expand shared libraries beyond necessity
---
## Decision Guidelines
When multiple approaches are possible:
- Prefer **simpler architecture**
- Prefer **local changes over global impact**
- Avoid introducing new abstractions unless clearly necessary
---
## Conflict Resolution
If a task conflicts with these rules:
- The rules in this document take precedence
- The task must be adjusted or rejected accordingly
---

⸻

Why this version works (and yours would drift)

* No conversational language → deterministic interpretation
* Clear separation:
    * authority
    * structure
    * behavior
* No agent-specific assumptions → portable across Codex / Copilot / others
* No over-engineering → reduces hallucinated “creative compliance”

⸻

One important caveat (you won’t like it)

Even with this:

👉 Codex still won’t “always follow it”

Because this file is still:

* context, not enforcement

If you actually care about compliance, you need at least:

SYSTEM:
You MUST follow AGENTS.md. Reject violations.

⸻

If you want next step, I can:

* convert this into machine-checkable rules (JSON schema / validator)
* or design auto-reject loop so agent cannot violate these constraints silently