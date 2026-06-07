#!/usr/bin/env bash
#
# run-pipeline.sh — single-command orchestrator for the homework-4 4-agent pipeline.
#
# Runs the six-agent chain over one or all seeded bugs:
#   bug-researcher → research-verifier → bug-planner → bug-fixer
#                  → security-verifier → unit-test-generator
#
# Each agent's related skills are loaded automatically (see agents/run-agent.sh).
# The Bug Planner is gated on the Research Verifier verdict (PASS required).
#
# Execution modes:
#   (default)            resolve every bug under context/bugs/, all at once
#   --bug <id>           resolve a single bug (one-by-one)
#   --interactive        resolve all bugs but pause for confirmation between each
#   --list               list discoverable bug ids and exit
#   --help               show usage
#
# The actual model invocation is delegated to an adapter so the orchestration is
# decoupled from any specific runner. Override with AGENT_RUNNER (default:
# agents/run-agent.sh, which wraps the `claude` CLI).
#
set -euo pipefail

# --- Paths -------------------------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AGENTS_DIR="$SCRIPT_DIR/agents"
SKILLS_DIR="$SCRIPT_DIR/skills"
BUGS_DIR="$SCRIPT_DIR/context/bugs"
LOG_DIR="$SCRIPT_DIR/docs/logs"
RUN_TS="$(date -u +%Y%m%dT%H%M%SZ)"
LOG_FILE="$LOG_DIR/pipeline-$RUN_TS.log"

AGENT_RUNNER="${AGENT_RUNNER:-$AGENTS_DIR/run-agent.sh}"

# --- The agent chain (ordered) ----------------------------------------------
# Format: "agent-name|relative-output-path|space-separated-relative-input-paths"
# Inputs/outputs are relative to a bug folder; "SRC" means the app source tree.
CHAIN=(
  "bug-researcher|research/codebase-research.md|bug-context.md SRC"
  "research-verifier|research/verified-research.md|bug-context.md research/codebase-research.md SRC"
  "bug-planner|implementation-plan.md|bug-context.md research/verified-research.md SRC"
  "bug-fixer|fix-summary.md|implementation-plan.md SRC"
  "security-verifier|security-report.md|fix-summary.md SRC"
  "unit-test-generator|test-report.md|fix-summary.md SRC"
)

# --- Logging -----------------------------------------------------------------
# Consistent, greppable format:
#   [<ts>] [<bug-id>] [<agent>:<model>] <PHASE> — <message>
_log() {
  local bug="$1" who="$2" phase="$3" msg="$4"
  local ts; ts="$(date -u +%Y-%m-%dT%H:%M:%SZ)"
  printf '[%s] [%s] [%s] %-6s — %s\n' "$ts" "$bug" "$who" "$phase" "$msg" | tee -a "$LOG_FILE"
}
log_info() { _log "$1" "$2" "INFO" "$3"; }
log_run()  { _log "$1" "$2" "RUN"  "$3"; }
log_ok()   { _log "$1" "$2" "OK"   "$3"; }
log_warn() { _log "$1" "$2" "WARN" "$3"; }
log_err()  { _log "$1" "$2" "ERROR" "$3"; }

banner() {
  local bug="$1"
  { echo ""
    echo "================================================================"
    echo "  BUG: $bug"
    echo "================================================================"; } | tee -a "$LOG_FILE"
}

# --- Helpers -----------------------------------------------------------------
model_of() {
  # Read the model from an agent spec's frontmatter.
  local name="$1"
  grep -m1 '^model:' "$AGENTS_DIR/$name.agent.md" | awk '{print $2}'
}

tools_of() {
  # Read the least-privilege tool set from an agent spec's `tools:` frontmatter.
  local spec="$AGENTS_DIR/$1.agent.md"
  grep -m1 '^tools:' "$spec" 2>/dev/null \
    | sed -E 's/^tools:[[:space:]]*//; s/#.*$//' \
    | tr -d '[]' | tr -s ' ' \
    | sed -E 's/^[[:space:]]+//; s/[[:space:]]+$//'
}

skills_of() {
  # Emit skill file paths declared in an agent spec's frontmatter `skills:` block.
  local name="$1" spec="$AGENTS_DIR/$1.agent.md"
  awk '
    /^skills:/ {ins=1; next}
    ins && /^[A-Za-z_]+:/ {ins=0}
    ins && /^[[:space:]]*-[[:space:]]*/ {gsub(/^[[:space:]]*-[[:space:]]*/,""); print}
  ' "$spec" | while read -r s; do
    [ -n "$s" ] && echo "$SKILLS_DIR/$s.md"
  done
}

list_bugs() {
  find "$BUGS_DIR" -mindepth 1 -maxdepth 1 -type d -printf '%f\n' 2>/dev/null | sort
}

verdict_pass() {
  # True if a verified-research.md reports a PASS verdict.
  local f="$1"
  [ -f "$f" ] && grep -qiE '^[-* ]*Verdict:[[:space:]]*PASS' "$f"
}

# --- Run a single agent step -------------------------------------------------
run_step() {
  local bug="$1" entry="$2"
  local name out_rel inputs
  name="${entry%%|*}"
  local rest="${entry#*|}"
  out_rel="${rest%%|*}"
  inputs="${rest#*|}"

  local model; model="$(model_of "$name")"
  local who="$name:$model"
  local bug_dir="$BUGS_DIR/$bug"
  local out_path="$bug_dir/$out_rel"

  local tools; tools="$(tools_of "$name")"
  log_run "$bug" "$who" "operation=produce output=$out_rel inputs=[$inputs] tools=[$tools]"

  # Auto-load related skills.
  local skill_files=(); local sf
  while IFS= read -r sf; do [ -n "$sf" ] && skill_files+=("$sf"); done < <(skills_of "$name")
  if [ "${#skill_files[@]}" -gt 0 ]; then
    log_info "$bug" "$who" "skills loaded: ${skill_files[*]##*/}"
  fi

  mkdir -p "$(dirname "$out_path")"

  # Delegate to the runner adapter. The adapter is responsible for composing the
  # prompt (agent spec + skills + inputs) and writing $AGENT_OUTPUT_FILE.
  if AGENT_NAME="$name" \
     AGENT_MODEL="$model" \
     AGENT_SPEC="$AGENTS_DIR/$name.agent.md" \
     AGENT_SKILLS="${skill_files[*]:-}" \
     AGENT_BUG_DIR="$bug_dir" \
     AGENT_INPUTS="$inputs" \
     AGENT_SRC_DIR="$SCRIPT_DIR/src" \
     AGENT_OUTPUT_FILE="$out_path" \
     AGENT_LOG_DIR="$LOG_DIR/$RUN_TS" \
     "$AGENT_RUNNER"; then
    if [ -f "$out_path" ]; then
      log_ok "$bug" "$who" "wrote $out_rel"
      return 0
    fi
    log_err "$bug" "$who" "runner returned success but $out_rel is missing"
    return 1
  else
    log_err "$bug" "$who" "runner failed for $out_rel"
    return 1
  fi
}

# --- Run the full chain for one bug -----------------------------------------
run_bug() {
  local bug="$1"
  local bug_dir="$BUGS_DIR/$bug"
  banner "$bug"

  if [ ! -f "$bug_dir/bug-context.md" ]; then
    log_err "$bug" "pipeline" "missing bug-context.md — skipping"
    FAILED+=("$bug (no bug-context.md)")
    return
  fi

  for entry in "${CHAIN[@]}"; do
    local name="${entry%%|*}"

    # Gate: do not plan/fix/etc. unless research verdict is PASS.
    if [ "$name" = "bug-planner" ]; then
      if ! verdict_pass "$bug_dir/research/verified-research.md"; then
        log_warn "$bug" "pipeline" "research verdict is not PASS — halting chain before bug-planner"
        FAILED+=("$bug (research FAIL)")
        return
      fi
      log_info "$bug" "pipeline" "research verdict PASS — proceeding to fix"
    fi

    if ! run_step "$bug" "$entry"; then
      log_err "$bug" "pipeline" "chain aborted at $name"
      FAILED+=("$bug (failed at $name)")
      return
    fi
  done

  SUCCEEDED+=("$bug")
}

# --- End-of-run summary ------------------------------------------------------
summary() {
  { echo ""
    echo "================================================================"
    echo "  RUN SUMMARY  ($RUN_TS)"
    echo "================================================================"; } | tee -a "$LOG_FILE"

  local bug bug_dir verdict sec tests
  for bug in "${SELECTED[@]}"; do
    bug_dir="$BUGS_DIR/$bug"
    verdict="$(grep -m1 -iE 'Verdict:' "$bug_dir/research/verified-research.md" 2>/dev/null | sed 's/^[-* ]*//' || true)"
    sec="$(grep -ciE 'CRITICAL|HIGH|MEDIUM|LOW' "$bug_dir/security-report.md" 2>/dev/null || echo 0)"
    tests="$(grep -m1 -iE 'pass|fail' "$bug_dir/test-report.md" 2>/dev/null || echo 'n/a')"
    printf '  %-45s research=[%s] security-findings=%s tests=[%s]\n' \
      "$bug" "${verdict:-n/a}" "$sec" "${tests:-n/a}" | tee -a "$LOG_FILE"
  done

  echo "" | tee -a "$LOG_FILE"
  log_info "ALL" "pipeline" "succeeded: ${#SUCCEEDED[@]}  failed/halted: ${#FAILED[@]}"
  if [ "${#FAILED[@]}" -gt 0 ]; then
    printf '  FAILED/HALTED: %s\n' "${FAILED[*]}" | tee -a "$LOG_FILE"
  fi
  log_info "ALL" "pipeline" "log saved to ${LOG_FILE#$SCRIPT_DIR/}"
}

usage() {
  sed -n '2,30p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

# --- Main --------------------------------------------------------------------
main() {
  local mode="all" target="" interactive="false"
  while [ $# -gt 0 ]; do
    case "$1" in
      --bug) mode="single"; target="${2:-}"; shift 2 ;;
      --interactive) interactive="true"; shift ;;
      --list) mkdir -p "$LOG_DIR"; echo "Discoverable bugs:"; list_bugs; exit 0 ;;
      -h|--help) usage; exit 0 ;;
      *) echo "Unknown argument: $1" >&2; usage; exit 2 ;;
    esac
  done

  mkdir -p "$LOG_DIR"

  SELECTED=()
  if [ "$mode" = "single" ]; then
    [ -n "$target" ] || { echo "--bug requires a bug id (see --list)" >&2; exit 2; }
    [ -d "$BUGS_DIR/$target" ] || { echo "No such bug: $target (see --list)" >&2; exit 2; }
    SELECTED=("$target")
  else
    while IFS= read -r b; do SELECTED+=("$b"); done < <(list_bugs)
  fi

  [ "${#SELECTED[@]}" -gt 0 ] || { echo "No bugs found under $BUGS_DIR" >&2; exit 1; }

  SUCCEEDED=(); FAILED=()
  log_info "ALL" "pipeline" "mode=$mode interactive=$interactive bugs=${#SELECTED[@]} runner=${AGENT_RUNNER#$SCRIPT_DIR/}"

  local first="true"
  for bug in "${SELECTED[@]}"; do
    if [ "$interactive" = "true" ] && [ "$first" = "false" ]; then
      read -r -p "Proceed to next bug ($bug)? [y/N] " ans
      case "$ans" in y|Y) ;; *) log_info "$bug" "pipeline" "skipped by user"; continue ;; esac
    fi
    run_bug "$bug"
    first="false"
  done

  summary
  [ "${#FAILED[@]}" -eq 0 ]
}

main "$@"
