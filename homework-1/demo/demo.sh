#!/usr/bin/env zsh
set -euo pipefail

SCRIPT_DIR=${0:A:h}
PROJECT_ROOT=${SCRIPT_DIR:h:h}
APP_URL="${APP_URL:-http://127.0.0.1:8080}"
SAMPLE_DATA_FILE="$SCRIPT_DIR/sample-data.json"
APP_LOG_FILE="${APP_LOG_FILE:-$SCRIPT_DIR/app.log}"
DEMO_LOG_FILE="${DEMO_LOG_FILE:-$SCRIPT_DIR/demo.log}"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-/tmp/gradle-home}"
CURL_BIN="${CURL_BIN:-/usr/bin/curl}"
TAIL_BIN="${TAIL_BIN:-/usr/bin/tail}"

: >"$APP_LOG_FILE"
: >"$DEMO_LOG_FILE"
exec > >(tee -a "$DEMO_LOG_FILE") 2>&1

cleanup() {
  if [[ -n "${APP_PID:-}" ]] && kill -0 "$APP_PID" 2>/dev/null; then
    kill "$APP_PID" 2>/dev/null || true
    wait "$APP_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

echo "Starting application..."
(
  cd "$PROJECT_ROOT"
  GRADLE_USER_HOME="$GRADLE_USER_HOME" ./gradlew :homework-1:run >"$APP_LOG_FILE" 2>&1
) &
APP_PID=$!

echo "Waiting for $APP_URL to become ready..."
for _ in {1..90}; do
  if "$CURL_BIN" -fsS "$APP_URL/" >/dev/null 2>&1; then
    echo "Application is ready."
    break
  fi

  if ! kill -0 "$APP_PID" 2>/dev/null; then
    echo "Application exited before becoming ready." >&2
    "$TAIL_BIN" -n 80 "$APP_LOG_FILE" >&2 || true
    exit 1
  fi

  sleep 1
done

if ! "$CURL_BIN" -fsS "$APP_URL/" >/dev/null 2>&1; then
  echo "Timed out waiting for application startup." >&2
  "$TAIL_BIN" -n 80 "$APP_LOG_FILE" >&2 || true
  exit 1
fi

echo "Executing sample API requests..."
APP_URL="$APP_URL" SAMPLE_DATA_FILE="$SAMPLE_DATA_FILE" CURL_BIN="$CURL_BIN" zsh "$SCRIPT_DIR/sample-requests.http"
echo "Demo flow finished successfully."
echo "Demo log: $DEMO_LOG_FILE"
echo "App log: $APP_LOG_FILE"
