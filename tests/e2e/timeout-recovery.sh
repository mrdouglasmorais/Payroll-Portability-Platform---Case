#!/usr/bin/env bash
# Lost-response + restart recovery against a healthy Compose stack.
# Simulates platform losing the Central protocol after acceptance, then verifies
# retry with the same idempotency key restores the original protocol; also
# restarts the API mid-flight and expects terminal confirmation.
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
POLL_ATTEMPTS="${POLL_ATTEMPTS:-60}"
POLL_SLEEP_SEC="${POLL_SLEEP_SEC:-1}"
COMPOSE_DIR="${COMPOSE_DIR:-$(cd "$(dirname "$0")/../.." && pwd)}"

json_field() {
  python3 -c 'import json,sys; print(json.load(sys.stdin).get(sys.argv[1], ""))' "$1"
}

post_json() {
  local path="$1"
  local body="$2"
  shift 2
  curl -sS -w "\n%{http_code}" -X POST "${API_BASE}${path}" \
    -H "Content-Type: application/json" \
    -H "X-Correlation-Id: recover-$(uuidgen | tr '[:upper:]' '[:lower:]')" \
    "$@" \
    -d "$body"
}

get_json() {
  curl -sS -w "\n%{http_code}" "${API_BASE}$1" \
    -H "X-Correlation-Id: recover-$(uuidgen | tr '[:upper:]' '[:lower:]')"
}

split_body_code() {
  local raw="$1"
  BODY="$(printf '%s' "$raw" | sed '$d')"
  CODE="$(printf '%s' "$raw" | tail -n1)"
}

wait_status() {
  local operation_id="$1"
  local expected="$2"
  local status=""
  for _ in $(seq 1 "$POLL_ATTEMPTS"); do
    raw="$(get_json "/api/v1/operacoes-portabilidade/${operation_id}")"
    split_body_code "$raw"
    if [[ "$CODE" == "200" ]]; then
      status="$(json_field status <<<"$BODY")"
      if [[ "$status" == "$expected" ]]; then
        echo "OK $operation_id -> $status"
        return 0
      fi
    fi
    sleep "$POLL_SLEEP_SEC"
  done
  echo "Timed out waiting for $expected on $operation_id (last=$status)" >&2
  exit 1
}

wait_protocol() {
  local operation_id="$1"
  local protocol=""
  for _ in $(seq 1 "$POLL_ATTEMPTS"); do
    protocol="$(
      docker compose -f "$COMPOSE_DIR/docker-compose.yml" exec -T platform-db \
        psql -U portability -d portability -Atc \
        "SELECT COALESCE(central_protocol,'') FROM portability_operations WHERE id = '$operation_id'"
    )"
    if [[ -n "$protocol" ]]; then
      echo "$protocol"
      return 0
    fi
    sleep "$POLL_SLEEP_SEC"
  done
  echo "Timed out waiting for central_protocol on $operation_id" >&2
  exit 1
}

echo "== authorize + create (ben-recover) =="
raw="$(post_json /api/v1/autorizacoes '{"beneficiarioId":"ben-recover","convenioId":"conv-1"}')"
split_body_code "$raw"
[[ "$CODE" == "201" ]] || { echo "authorize failed: $CODE $BODY" >&2; exit 1; }

raw="$(get_json /api/v1/beneficiarios/ben-recover/agenda)"
split_body_code "$raw"
[[ "$CODE" == "200" ]] || { echo "agenda failed: $CODE $BODY" >&2; exit 1; }
PARCELA="$(python3 -c 'import json,sys; a=json.load(sys.stdin); print(next(i["parcelaId"] for i in a if i["parcelaId"].startswith("parcela-recover-")))' <<<"$BODY")"

IDEM="recover-$(uuidgen | tr '[:upper:]' '[:lower:]')"
CREATE_BODY=$(cat <<EOF
{"beneficiarioId":"ben-recover","parcelas":["${PARCELA}"],"credorCessionario":"creditor-recover","contaRecebimento":{"banco":"001","agencia":"1234","conta":"55555-0"}}
EOF
)
raw="$(post_json /api/v1/operacoes-portabilidade "$CREATE_BODY" -H "Idempotency-Key: ${IDEM}")"
split_body_code "$raw"
[[ "$CODE" == "202" ]] || { echo "create failed: $CODE $BODY" >&2; exit 1; }
OP="$(json_field operationId <<<"$BODY")"

echo "== wait original protocol =="
ORIGINAL_PROTOCOL="$(wait_protocol "$OP")"
echo "original protocol=$ORIGINAL_PROTOCOL"

echo "== simulate lost local protocol (platform forgets Central acceptance) =="
docker compose -f "$COMPOSE_DIR/docker-compose.yml" exec -T platform-db \
  psql -U portability -d portability -v ON_ERROR_STOP=1 <<SQL
UPDATE portability_operations
SET central_protocol = NULL, business_status = 'PENDENTE', updated_at = NOW()
WHERE id = '$OP';
UPDATE processing_jobs
SET job_type = 'SUBMIT_TO_CENTRAL',
    technical_status = 'PENDING',
    lease_owner = NULL,
    lease_until = NULL,
    next_run_at = NOW() - INTERVAL '1 second',
    attempt_count = 0,
    last_error = 'simulated lost response',
    updated_at = NOW()
WHERE operation_id = '$OP';
SQL

echo "== wait recovered protocol (same identity) =="
RECOVERED=""
for _ in $(seq 1 "$POLL_ATTEMPTS"); do
  RECOVERED="$(
    docker compose -f "$COMPOSE_DIR/docker-compose.yml" exec -T platform-db \
      psql -U portability -d portability -Atc \
      "SELECT COALESCE(central_protocol,'') FROM portability_operations WHERE id = '$OP'"
  )"
  if [[ "$RECOVERED" == "$ORIGINAL_PROTOCOL" ]]; then
    break
  fi
  sleep "$POLL_SLEEP_SEC"
done
[[ "$RECOVERED" == "$ORIGINAL_PROTOCOL" ]] || {
  echo "Protocol mismatch after recovery: original=$ORIGINAL_PROTOCOL recovered=$RECOVERED" >&2
  exit 1
}
echo "recovered same protocol=$RECOVERED"

echo "== restart API while operation still pending/polling =="
docker compose -f "$COMPOSE_DIR/docker-compose.yml" restart portability-api >/dev/null
# wait healthy
for _ in $(seq 1 60); do
  if curl -sf "${API_BASE}/actuator/health" | grep -q UP; then
    break
  fi
  sleep 1
done
curl -sf "${API_BASE}/actuator/health" | grep -q UP

wait_status "$OP" "CONFIRMADA"

echo "== second create with same key stays same operation =="
raw="$(post_json /api/v1/operacoes-portabilidade "$CREATE_BODY" -H "Idempotency-Key: ${IDEM}")"
split_body_code "$raw"
[[ "$CODE" == "202" ]] || { echo "idempotent replay failed: $CODE $BODY" >&2; exit 1; }
OP2="$(json_field operationId <<<"$BODY")"
[[ "$OP" == "$OP2" ]] || { echo "operation identity changed after recovery" >&2; exit 1; }

echo "TIMEOUT_RECOVERY_E2E_PASS"
