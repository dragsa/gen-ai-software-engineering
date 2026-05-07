#!/usr/bin/env bash
# demo.sh — starts the Ticket System server, exercises every endpoint,
# and logs all output to demo/demo.log and demo/app.log.
#
# Usage (from the homework-2 directory):
#   chmod +x demo/demo.sh
#   ./demo/demo.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
REPO_ROOT="$(cd "$PROJECT_DIR/.." && pwd)"

DEMO_LOG="$SCRIPT_DIR/demo.log"
APP_LOG="$SCRIPT_DIR/app.log"
BASE_URL="http://localhost:8080"
SERVER_PID=""

# ── helpers ──────────────────────────────────────────────────────────────────

log()  { echo "[$(date '+%H:%M:%S')] $*" | tee -a "$DEMO_LOG"; }
head_log() { echo "" | tee -a "$DEMO_LOG"; echo "══════════════════════════════════════════" | tee -a "$DEMO_LOG"; log "$*"; echo "══════════════════════════════════════════" | tee -a "$DEMO_LOG"; }

call() {
  local label="$1"; shift
  log "▶ $label"
  local response
  response=$(curl -s --max-time 30 "$@")
  local status=$?
  if [ $status -ne 0 ]; then
    log "  curl error (code $status) — server may be unresponsive"
  fi
  echo "$response" | tee -a "$DEMO_LOG"
  echo ""
  echo "$response"
}

wait_for_server() {
  log "Waiting for server on $BASE_URL ..."
  local attempts=0
  until curl -sf --max-time 5 "$BASE_URL/openapi.yaml" > /dev/null 2>&1; do
    attempts=$((attempts + 1))
    if [ "$attempts" -ge 30 ]; then
      log "ERROR: server did not start within 30 seconds"
      exit 1
    fi
    sleep 1
  done
  log "Server is up (after ${attempts}s)"
}

cleanup() {
  if [ -n "$SERVER_PID" ] && kill -0 "$SERVER_PID" 2>/dev/null; then
    log "Stopping server (PID $SERVER_PID)"
    kill "$SERVER_PID"
  fi
}
trap cleanup EXIT

# ── initialise log files ──────────────────────────────────────────────────────

: > "$DEMO_LOG"
: > "$APP_LOG"

log "Demo script started"
log "Project: $PROJECT_DIR"

# ── build (skip if already built) ────────────────────────────────────────────

head_log "Building project"
cd "$REPO_ROOT"
./gradlew :homework-2:installDist --quiet 2>&1 | tee -a "$APP_LOG"
log "Build complete"

# ── start server ─────────────────────────────────────────────────────────────

head_log "Starting server"
"$PROJECT_DIR/build/install/homework-2/bin/homework-2" >> "$APP_LOG" 2>&1 &
SERVER_PID=$!
log "Server started with PID $SERVER_PID"

wait_for_server

# ── exercise endpoints ────────────────────────────────────────────────────────

head_log "POST /tickets — create three tickets"

TICKET_1=$(call "Create account_access ticket" \
  -X POST "$BASE_URL/tickets" \
  -H 'Content-Type: application/json' \
  -d '{
    "customer_id": "cust-demo-001",
    "customer_email": "alice@example.com",
    "customer_name": "Alice Smith",
    "subject": "Cannot login after password reset",
    "description": "I have been unable to login since yesterday morning after being forced to reset my password. The reset link worked but new credentials are rejected.",
    "category": "account_access",
    "priority": "high",
    "metadata": { "source": "web_form" }
  }')

ID_1=$(echo "$TICKET_1" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
log "Created ticket id=$ID_1"

TICKET_2=$(call "Create billing_question ticket" \
  -X POST "$BASE_URL/tickets" \
  -H 'Content-Type: application/json' \
  -d '{
    "customer_id": "cust-demo-002",
    "customer_email": "bob@example.com",
    "customer_name": "Bob Jones",
    "subject": "Invoice shows wrong amount for March",
    "description": "My invoice for March shows a charge of $299 but I am on the $199 plan. Please review and issue a corrected invoice or refund.",
    "category": "billing_question",
    "priority": "high",
    "metadata": { "source": "email" }
  }')

ID_2=$(echo "$TICKET_2" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
log "Created ticket id=$ID_2"

TICKET_3=$(call "Create technical_issue ticket" \
  -X POST "$BASE_URL/tickets" \
  -H 'Content-Type: application/json' \
  -d '{
    "customer_id": "cust-demo-003",
    "customer_email": "carol@example.com",
    "customer_name": "Carol White",
    "subject": "App crashes on startup after latest update",
    "description": "The mobile app crashes immediately on launch after the latest update. It worked fine on version 3.1.2. Device is iPhone 15 running iOS 17.4.",
    "category": "technical_issue",
    "priority": "urgent",
    "metadata": { "source": "api", "device_type": "mobile" }
  }')

ID_3=$(echo "$TICKET_3" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
log "Created ticket id=$ID_3"

head_log "POST /tickets — validation failure (invalid email)"
call "Validation failure — bad email" \
  -X POST "$BASE_URL/tickets" \
  -H 'Content-Type: application/json' \
  -d '{
    "customer_id": "cust-bad",
    "customer_email": "not-an-email",
    "customer_name": "Bad User",
    "subject": "Subject is fine",
    "description": "Description is long enough to pass but the email is invalid."
  }'

head_log "GET /tickets — list and filter"
call "List all tickets" -X GET "$BASE_URL/tickets"
call "Filter by category=account_access" -X GET "$BASE_URL/tickets?category=account_access"
call "Filter by priority=high" -X GET "$BASE_URL/tickets?priority=high"
call "Search for login" -X GET "$BASE_URL/tickets?search=login"

head_log "GET /tickets/:id"
call "Get ticket $ID_1" -X GET "$BASE_URL/tickets/$ID_1"
call "Get non-existent ticket (404)" -X GET "$BASE_URL/tickets/does-not-exist"

head_log "PUT /tickets/:id — update"
call "Assign ticket $ID_1 to agent and set in_progress" \
  -X PUT "$BASE_URL/tickets/$ID_1" \
  -H 'Content-Type: application/json' \
  -d '{"status": "in_progress", "assigned_to": "agent-demo-001"}'

call "Resolve ticket $ID_2" \
  -X PUT "$BASE_URL/tickets/$ID_2" \
  -H 'Content-Type: application/json' \
  -d '{"status": "resolved"}'

head_log "POST /tickets/:id/auto-classify"
call "Auto-classify ticket $ID_3" -X POST "$BASE_URL/tickets/$ID_3/auto-classify"
call "Auto-classify non-existent (404)" -X POST "$BASE_URL/tickets/does-not-exist/auto-classify"

head_log "POST /tickets/import — CSV (50 rows)"
call "Import sample_tickets.csv" \
  -X POST "$BASE_URL/tickets/import" \
  -F "file=@$SCRIPT_DIR/sample_tickets.csv"

head_log "POST /tickets/import — JSON (20 objects)"
call "Import sample_tickets.json" \
  -X POST "$BASE_URL/tickets/import" \
  -F "file=@$SCRIPT_DIR/sample_tickets.json"

head_log "POST /tickets/import — XML (30 elements)"
call "Import sample_tickets.xml" \
  -X POST "$BASE_URL/tickets/import" \
  -F "file=@$SCRIPT_DIR/sample_tickets.xml"

head_log "POST /tickets/import — invalid CSV (partial failure)"
call "Import invalid/invalid_email.csv" \
  -X POST "$BASE_URL/tickets/import" \
  -F "file=@$SCRIPT_DIR/invalid/invalid_email.csv"

head_log "POST /tickets/import — malformed JSON (parse failure)"
call "Import invalid/malformed.json" \
  -X POST "$BASE_URL/tickets/import" \
  -F "file=@$SCRIPT_DIR/invalid/malformed.json"

head_log "DELETE /tickets/:id"
call "Delete ticket $ID_1 (204)" -X DELETE "$BASE_URL/tickets/$ID_1"
call "Confirm deleted (404)" -X GET "$BASE_URL/tickets/$ID_1"

head_log "Documentation endpoints"
call "GET /openapi.yaml" -X GET "$BASE_URL/openapi.yaml" | head -5
log "GET /swagger → open http://localhost:8080/swagger in your browser"

head_log "Demo complete"
log "All output written to $DEMO_LOG"
log "Server log written to $APP_LOG"
log "Server still running — press Ctrl+C or let the script exit to stop it"
