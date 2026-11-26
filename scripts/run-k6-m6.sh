#!/usr/bin/env bash
# Run M6 portability k6 against a local API (Compose or IntelliJ Debug).
# Usage:
#   ./scripts/run-k6-m6.sh           # 3 VUs / 45s smoke
#   ./scripts/run-k6-m6.sh debug     # 1 VU / 60s, soft latency thresholds
#   K6_VUS=5 K6_DURATION=30s ./scripts/run-k6-m6.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

MODE="${1:-smoke}"
API_BASE="${API_BASE:-http://host.docker.internal:8080}"

case "$MODE" in
  debug|ide|intellij)
    export K6_VUS="${K6_VUS:-1}"
    export K6_DURATION="${K6_DURATION:-60s}"
    export K6_SKIP_THRESHOLDS="${K6_SKIP_THRESHOLDS:-1}"
    echo "k6 M6 debug companion → API_BASE=$API_BASE VUs=$K6_VUS duration=$K6_DURATION (thresholds relaxed)"
    ;;
  smoke|*)
    export K6_VUS="${K6_VUS:-3}"
    export K6_DURATION="${K6_DURATION:-45s}"
    export K6_SKIP_THRESHOLDS="${K6_SKIP_THRESHOLDS:-0}"
    echo "k6 M6 smoke → API_BASE=$API_BASE VUs=$K6_VUS duration=$K6_DURATION"
    ;;
esac

missing=0
if ! curl -sf "http://localhost:8080/actuator/health" >/dev/null 2>&1; then
  echo "ERROR: portability-api not on localhost:8080 — Debug/Compose API first." >&2
  missing=1
fi
if ! curl -sf "http://localhost:8081/actuator/health" >/dev/null 2>&1; then
  echo "ERROR: central-simulator not on localhost:8081 — Debug Central first (API alone returns 503)." >&2
  missing=1
fi
if [[ "$missing" -ne 0 ]]; then
  echo "See docs/runbooks/load-test-intellij-debug.md" >&2
  exit 1
fi

# Pass VAR=value to k6 -e. Bare `-e API_BASE` clears the var and falls back to
# localhost inside the container (connection refused against Compose-published ports).
docker run --rm -i \
  grafana/k6:0.54.0 run \
  -e "API_BASE=${API_BASE}" \
  -e "K6_VUS=${K6_VUS}" \
  -e "K6_DURATION=${K6_DURATION}" \
  -e "K6_SKIP_THRESHOLDS=${K6_SKIP_THRESHOLDS}" \
  - < tests/performance/k6-m6-portability.js
