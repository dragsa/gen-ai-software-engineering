#!/usr/bin/env bash
#
# run-agent.sh — agent runner adapter used by run-pipeline.sh.
#
# Composes a single prompt from the agent spec + auto-loaded skills + the agent's
# input artifacts, then invokes a model to produce the output artifact. This keeps
# the orchestrator (run-pipeline.sh) decoupled from any specific CLI.
#
# The orchestrator passes everything via environment variables:
#   AGENT_NAME         agent id (e.g. bug-researcher)
#   AGENT_MODEL        model string from the agent frontmatter
#   AGENT_SPEC         path to the *.agent.md spec
#   AGENT_SKILLS       space-separated skill file paths (may be empty)
#   AGENT_BUG_DIR      path to the current bug folder
#   AGENT_INPUTS       space-separated input paths (relative to bug dir; "SRC" = source tree)
#   AGENT_SRC_DIR      path to the application source tree
#   AGENT_OUTPUT_FILE  absolute path the agent must write
#   AGENT_LOG_DIR      directory to drop the composed prompt for inspection
#
# Least privilege: the set of tools granted to the model is read from the agent
# spec's `tools:` frontmatter field — each agent gets only what its role needs.
# If the field is missing, a safe read-only fallback (Read,Grep,Glob) is used so a
# misconfigured agent can never accidentally gain Edit/Write/Bash.
#
# Override the actual model call with CLAUDE_CMD (default: "claude"). The default
# invocation runs Claude Code headless with file tools so the agent writes its own
# output file. On a machine without the CLI, set CLAUDE_CMD to your own runner.
#
set -euo pipefail

CLAUDE_CMD="${CLAUDE_CMD:-claude}"
READ_ONLY_FALLBACK="Read,Grep,Glob"

# Parse the `tools:` inline array from an agent spec, e.g.
#   tools: [Read, Grep, Glob, Write]   # comment
# -> "Read,Grep,Glob,Write"
parse_tools() {
  local spec="$1" line tools
  line="$(grep -m1 '^tools:' "$spec" || true)"
  [ -n "$line" ] || { echo ""; return; }
  tools="$(printf '%s' "$line" \
    | sed -E 's/^tools:[[:space:]]*//; s/#.*$//' \
    | tr -d '[]' \
    | tr ',' '\n' \
    | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//' \
    | grep -v '^$' \
    | paste -sd ',' -)"
  echo "$tools"
}

ALLOWED_TOOLS="$(parse_tools "$AGENT_SPEC")"
if [ -z "$ALLOWED_TOOLS" ]; then
  echo "run-agent.sh: no tools: field in $AGENT_SPEC — falling back to read-only ($READ_ONLY_FALLBACK)" >&2
  ALLOWED_TOOLS="$READ_ONLY_FALLBACK"
fi
mkdir -p "$AGENT_LOG_DIR"
PROMPT_FILE="$AGENT_LOG_DIR/${AGENT_NAME}-$(basename "$AGENT_OUTPUT_FILE").prompt.md"

# --- Compose the prompt ------------------------------------------------------
{
  echo "# Agent: $AGENT_NAME"
  echo "# Model: $AGENT_MODEL"
  echo "# Allowed tools (least privilege): $ALLOWED_TOOLS"
  echo ""
  echo "You are running as the agent defined by the following specification."
  echo "Follow it exactly. Produce ONLY the declared output artifact by writing it"
  echo "to this absolute path:"
  echo ""
  echo "    $AGENT_OUTPUT_FILE"
  echo ""
  echo "----- AGENT SPECIFICATION -----"
  cat "$AGENT_SPEC"
  echo ""

  if [ -n "${AGENT_SKILLS:-}" ]; then
    echo "----- LOADED SKILLS (apply these) -----"
    for s in $AGENT_SKILLS; do
      [ -f "$s" ] || continue
      echo "### Skill: $(basename "$s")"
      cat "$s"
      echo ""
    done
  fi

  echo "----- INPUT ARTIFACTS -----"
  for in_rel in $AGENT_INPUTS; do
    if [ "$in_rel" = "SRC" ]; then
      echo "### Application source tree (read as needed): $AGENT_SRC_DIR"
      echo ""
      continue
    fi
    in_path="$AGENT_BUG_DIR/$in_rel"
    echo "### $in_rel"
    if [ -f "$in_path" ]; then
      echo '```'
      cat "$in_path"
      echo '```'
    else
      echo "(missing: $in_path)"
    fi
    echo ""
  done

  echo "----- TASK -----"
  echo "Perform this agent's role for the bug in: $AGENT_BUG_DIR"
  echo "Write the result to $AGENT_OUTPUT_FILE in the format its spec/skill requires."
} > "$PROMPT_FILE"

# --- Invoke the model --------------------------------------------------------
# Default path: Claude Code headless, allowed to read/edit/write files so the
# agent can inspect source and write its output artifact.
if command -v "$CLAUDE_CMD" >/dev/null 2>&1; then
  "$CLAUDE_CMD" \
    --model "$AGENT_MODEL" \
    --permission-mode acceptEdits \
    --allowedTools "$ALLOWED_TOOLS" \
    -p "$(cat "$PROMPT_FILE")"
else
  echo "run-agent.sh: '$CLAUDE_CMD' not found on PATH." >&2
  echo "Set CLAUDE_CMD to a runner that reads the prompt and writes $AGENT_OUTPUT_FILE." >&2
  echo "Composed prompt is available at: $PROMPT_FILE" >&2
  exit 127
fi
