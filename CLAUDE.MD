# CLAUDE.md

## Source of Truth
All project-wide instructions, conventions, and constraints are defined in:

→ AGENTS.md

This file MUST be read before performing any task.  
If there is any conflict, AGENTS.md takes precedence.

---

## Operating Mode (Mandatory)

This project follows a strict Plan → Approve → Execute workflow.

### 1. Plan Phase (Required)
- ALWAYS start in planning mode
- Use /plan (or /ultraplan for complex tasks)
- Analyze:
    - relevant files
    - dependencies
    - architecture impact
- Produce a structured plan

The plan MUST include:
- goal
- steps (ordered)
- files to modify
- risks / edge cases

DO NOT write code in this phase.

---

### 2. Approval Gate (Hard Stop)
- After producing the plan:
    - STOP
    - WAIT for explicit user approval

DO NOT:
- write code
- modify files
- run mutating commands

---

### 3. Execution Phase
Only after approval:

- Execute step-by-step
- Follow the approved plan strictly
- If deviation is needed:
    - STOP
    - propose updated plan

---

## Persistence Rules

For non-trivial tasks:

- Write plan to plan.md
- If large:
    - split into phase-01.md, phase-02.md, etc.

Execution should follow these files, not implicit context.

---

## Safety Constraints

- No implicit assumptions beyond AGENTS.md
- No hidden side effects
- Prefer minimal, maintainable changes
- Avoid overengineering

---

## Interaction Rules

- Be concise and structured
- Surface uncertainties explicitly
- Ask for clarification instead of guessing

---

## Quick Commands Reference

- /plan <task> → standard planning
- /ultraplan <task> → deep multi-step planning

---

## Summary

- AGENTS.md = rules
- CLAUDE.md = workflow enforcement

Always:
1. Plan
2. Wait
3. Execute