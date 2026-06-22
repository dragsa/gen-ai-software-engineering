#!/usr/bin/env bash
#
# Claude Code PreToolUse(Bash) guard — referenced from .claude/settings.json.
# Runs the SAME pre-push gate before a `git push` typed inside a Claude session.
# Does nothing for any other command.
set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Extract the Bash command Claude is about to run (from the tool-call JSON on stdin).
cmd="$(cat | python3 -c 'import json,sys
try: print(json.load(sys.stdin).get("tool_input", {}).get("command", ""))
except Exception: print("")' 2>/dev/null || true)"

case "$cmd" in
  *"git push"*)
    if ! "$DIR/pre-push" >&2; then
      echo "Coverage gate failed — blocking git push." >&2
      exit 2   # exit 2 tells Claude Code to block the tool call
    fi
    ;;
esac
