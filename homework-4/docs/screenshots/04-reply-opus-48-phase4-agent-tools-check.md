You're right — the adapter hardcodes `--allowedTools "Read,Write,Edit,Bash,Grep,Glob"` for every agent, so a read-only reviewer currently has the same power to edit source and run shell as the fixer. That violates least-privilege and the AGENTS.md "minimal, scoped" principle. Here's the plan (no changes yet).

Proposed minimal tool set per agent, driven by role:

| Agent | Read | Grep/Glob | Write | Edit | Bash | Rationale |
|---|---|---|---|---|---|---|
| `bug-researcher` | ✓ | ✓ | ✓ (report only) | — | — | Investigates source, writes `codebase-research.md`. No source edits, no shell. |
| `research-verifier` | ✓ | ✓ | ✓ (report only) | — | — | Fact-checks refs, writes `verified-research.md`. Read-only on code. |
| `bug-planner` | ✓ | ✓ | ✓ (plan only) | — | — | Designs the plan, writes `implementation-plan.md`. No edits. |
| `bug-fixer` | ✓ | ✓ | ✓ | ✓ | ✓ | The only agent that mutates source and runs tests. Full set justified. |
| `security-verifier` | ✓ | ✓ | ✓ (report only) | — | — | Review only, writes `security-report.md`. Explicitly no Edit/Bash. |
| `unit-test-generator` | ✓ | ✓ | ✓ (test files) | — | ✓ | Writes new test files, runs the test command. No source Edit. |

Note: the three read-only reviewers still need `Write` for their single output report — least-privilege narrows away `Edit` (source mutation) and `Bash` (arbitrary shell), which is the real risk. Path-scoping of `Write` isn't expressible via `--allowedTools`, so the agent-spec guardrails remain the backstop for "report only."

Wiring changes:

1. Add a `tools:` field to each `*.agent.md` frontmatter with its minimal set — the agent spec becomes the single source of truth.
2. Rewrite `agents/run-agent.sh` to parse `tools:` from `AGENT_SPEC` and pass that to `--allowedTools`, replacing the hardcoded list (with a safe read-only fallback — `Read,Grep,Glob` — if the field is missing, so a misconfigured agent can't accidentally get write/edit/bash).
3. `run-pipeline.sh` reads the tool set and logs it per step (e.g. `tools=[Read,Grep,Glob,Write]`), so the minimal grant is visible in the run log/evidence.
4. Update the README/HOWTORUN to document the per-agent tool policy.

Files touched: six `agents/*.agent.md`, `agents/run-agent.sh`, `run-pipeline.sh`, `README.md`, `HOWTORUN.md`.

Want me to execute this? And do you agree with giving the three reviewers `Write` (for their report) but withholding `Edit`/`Bash` — or would you prefer an even tighter design where a separate mechanism writes the report and reviewers get truly read-only `Read,Grep,Glob`?