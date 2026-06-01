Good catch — let me trace the actual data flow against the four agent specs.

Per-bug artifacts and who touches them:

| Artifact | Produced by | Consumed by | Status |
|---|---|---|---|
| `bug-context.md` | pre-created (done) | Bug Researcher | ✅ has a source |
| `research/codebase-research.md` | **Bug Researcher** | research-verifier | ❌ **no agent produces it** |
| `research/verified-research.md` | research-verifier | Bug Planner | ✅ |
| `implementation-plan.md` | **Bug Planner** | bug-fixer | ❌ **no agent produces it** |
| `fix-summary.md` | bug-fixer | security-verifier + unit-test-generator | ✅ |
| `security-report.md` | security-verifier | (terminal) | ✅ |
| `test-report.md` | unit-test-generator | (terminal) | ✅ |

So the run order in TASKS names **six** roles — Bug Researcher → Research Verifier → Bug Planner → Bug Fixer → Security Verifier → Unit Test Generator — but only four exist as `*.agent.md`. Two inputs are dangling:

- `codebase-research.md` (research-verifier's input) has no producer → needs a **Bug Researcher** agent.
- `implementation-plan.md` (bug-fixer's input) has no producer → needs a **Bug Planner** agent.

TASKS marks only the four as graded "Required" tasks, but the pipeline literally cannot run end-to-end without these two feeder agents. The expected-structure tree in TASKS confirms it (it lists `codebase-research.md` and `implementation-plan.md`).

Proposed additions (no changes yet):

**`agents/bug-researcher.agent.md`** — reads `bug-context.md` + source tree; locates the root cause; writes `research/codebase-research.md` with file:line references and verified snippets. Suggested model `claude-opus-4-6` (root-cause discovery is the most reasoning-heavy step) — though `sonnet` is defensible if we want the opus verifier to be the independent strong check.

**`agents/bug-planner.agent.md`** — reads `verified-research.md` (+ bug-context, source); designs the fix as `implementation-plan.md` with target files, before/after code, and the test command. Suggested model `claude-sonnet-4-6` (translating verified findings into a precise edit plan).

Two smaller alignment fixes:

- `research-verifier` should also list `bug-context.md` as an input (it needs the symptom to judge completeness), not just `codebase-research.md`.
- Gate the chain on the verifier's verdict: if `verified-research.md` is **FAIL**, the Planner shouldn't proceed — worth stating explicitly in the orchestration (Phase 4) and in the planner spec.

If you agree, I'd update the roadmap (Phase 3 scope → six agents) and add the two specs before Phase 4. Want me to also record a model choice for each new agent in the README table?

Which model would you prefer for the Bug Researcher — `opus` (strongest discovery) or `sonnet` (reserve opus for the independent verify)?