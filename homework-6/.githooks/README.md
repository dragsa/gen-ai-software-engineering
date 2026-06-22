# homework-6 git hooks

Two scripts, one gate. The gate = `./gradlew :homework-6:koverVerify -PenforceCoverage`
(line coverage must be ≥ 80%).

| File | What it is | When it runs |
|------|-----------|--------------|
| `pre-push` | The actual git pre-push hook. Runs the coverage gate; exits non-zero to block the push. | On `git push`, after `git config core.hooksPath homework-6/.githooks`. Also runnable directly: `bash homework-6/.githooks/pre-push`. |
| `claude-pre-push-guard.sh` | Adapter for Claude Code, referenced from `.claude/settings.json`. Detects a `git push` command and reuses `pre-push`. | On every Bash tool call inside a Claude session, but only acts when the command is a `git push`. |

`claude-pre-push-guard.sh` contains no gate logic of its own — it just calls `pre-push`, so there is a single source of truth.

Enable the git hook:

```bash
git config core.hooksPath homework-6/.githooks
```
