#!/usr/bin/env bash
# Real process interruption: SIGKILL portability-api after Central acceptance
# (central_protocol persisted), then restart and expect CONFIRMADA without
# duplicate operations. Distinct from graceful `restart` in timeout-recovery.sh.
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
    -H "X-Correlation-Id: kill-$(uuidgen | tr '[:upper:]' '[:lower:]')" \
    "$@" \
    -d "$body"
}

get_json() {
  curl -sS -w "\n%{http_code}" "${API_BASE}$1" \
    -H "X-Correlation-Id: kill-$(uuidgen | tr '[:upper:]' '[:lower:]')"
}

split_body_code() {
  local raw="$1"
  BODY="$(printf '%s' "$raw" | sed '$d')"
  CODE="$(printf '%s' "$raw" | tail -n1)"
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

echo "== authorize + create (ben-recover-2 parcel) =="
# Use recover-timeout parcel family if present; fall back to first recover parcel.
raw="$(post_json /api/v1/autorizacoes '{"beneficiarioId":"ben-recover","convenioId":"conv-1"}')"
split_body_code "$raw"
[[ "$CODE" == "201" ]] || { echo "authorize failed: $CODE $BODY" >&2; exit 1; }

raw="$(get_json /api/v1/beneficiarios/ben-recover/agenda)"
split_body_code "$raw"
[[ "$CODE" == "200" ]] || { echo "agenda failed: $CODE $BODY" >&2; exit 1; }
PARCELA="$(python3 -c 'import json,sys; a=json.load(sys.stdin); open_ids=[i["parcelaId"] for i in a if str(i.get("status","")).upper() in ("OPEN","ABERTA")]; print(open_ids[0] if open_ids else "")' <<<"$BODY")"
[[ -n "$PARCELA" ]] || { echo "no OPEN recover parcel available; recreate volumes" >&2; exit 1; }

IDEM="kill-$(uuidgen | tr '[:upper:]' '[:lower:]')"
CREATE_BODY=$(cat <<EOF
{"beneficiarioId":"ben-recover","parcelas":["${PARCELA}"],"credorCessionario":"creditor-kill","contaRecebimento":{"banco":"001","agencia":"1234","conta":"66666-0"}}
EOF
)
raw="$(post_json /api/v1/operacoes-portabilidade "$CREATE_BODY" -H "Idempotency-Key: ${IDEM}")"
split_body_code "$raw"
[[ "$CODE" == "202" ]] || { echo "create failed: $CODE $BODY" >&2; exit 1; }
OP="$(json_field operationId <<<"$BODY")"

echo "== wait Central acceptance protocol =="
PROTOCOL="$(wait_protocol "$OP")"
echo "protocol=$PROTOCOL"

echo "== SIGKILL API process (hard interrupt after external accept) =="
docker compose -f "$COMPOSE_DIR/docker-compose.yml" kill -s SIGKILL portability-api >/dev/null
docker compose -f "$COMPOSE_DIR/docker-compose.yml" start portability-api >/dev/null
for _ in $(seq 1 60); do
  if curl -sf "${API_BASE}/actuator/health" | grep -q UP; then
    break
  fi
  sleep 1
done
curl -sf "${API_BASE}/actuator/health" | grep -q UP

echo "== resume to confirmation =="
wait_status "$OP" "CONFIRMADA"

echo "== same key still same operation =="
raw="$(post_json /api/v1/operacoes-portabilidade "$CREATE_BODY" -H "Idempotency-Key: ${IDEM}")"
split_body_code "$raw"
[[ "$CODE" == "202" ]] || { echo "idempotent replay failed: $CODE $BODY" >&2; exit 1; }
OP2="$(json_field operationId <<<"$BODY")"
[[ "$OP" == "$OP2" ]] || { echo "operation identity changed after kill" >&2; exit 1; }

echo "PROCESS_KILL_RECOVERY_E2E_PASS"
