#!/usr/bin/env bash
# Dual-stack API E2E against a running docker compose (API + Central + DBs).
# Usage: ./tests/e2e/dual-stack.sh
set -euo pipefail

API_BASE="${API_BASE:-http://localhost:8080}"
POLL_ATTEMPTS="${POLL_ATTEMPTS:-40}"
POLL_SLEEP_SEC="${POLL_SLEEP_SEC:-1}"

json_field() {
  local json="$1"
  local field="$2"
  python3 -c 'import json,sys; print(json.load(sys.stdin).get(sys.argv[1], ""))' "$field" <<<"$json"
}

post_json() {
  local path="$1"
  local body="$2"
  shift 2
  curl -sS -w "\n%{http_code}" -X POST "${API_BASE}${path}" \
    -H "Content-Type: application/json" \
    -H "X-Correlation-Id: e2e-$(uuidgen | tr '[:upper:]' '[:lower:]')" \
    "$@" \
    -d "$body"
}

get_json() {
  local path="$1"
  curl -sS -w "\n%{http_code}" "${API_BASE}${path}" \
    -H "X-Correlation-Id: e2e-$(uuidgen | tr '[:upper:]' '[:lower:]')"
}

split_body_code() {
  local raw="$1"
  BODY="$(printf '%s' "$raw" | sed '$d')"
  CODE="$(printf '%s' "$raw" | tail -n1)"
}

wait_terminal() {
  local operation_id="$1"
  local expected="$2"
  local status=""
  for _ in $(seq 1 "$POLL_ATTEMPTS"); do
    raw="$(get_json "/api/v1/operacoes-portabilidade/${operation_id}")"
    split_body_code "$raw"
    if [[ "$CODE" != "200" ]]; then
      echo "GET operation HTTP $CODE: $BODY" >&2
      sleep "$POLL_SLEEP_SEC"
      continue
    fi
    status="$(json_field "$BODY" status)"
    if [[ "$status" == "CONFIRMADA" || "$status" == "REJEITADA" ]]; then
      break
    fi
    sleep "$POLL_SLEEP_SEC"
  done
  if [[ "$status" != "$expected" ]]; then
    echo "Expected terminal status $expected, got '${status:-empty}' for $operation_id" >&2
    echo "$BODY" >&2
    exit 1
  fi
  echo "OK operation $operation_id -> $status"
}

echo "== health =="
curl -sf "${API_BASE}/actuator/health" | grep -q UP
curl -sf "${API_BASE}/api/v1/status" | grep -q portability-api

echo "== happy path (confirm) =="
raw="$(post_json /api/v1/autorizacoes '{"beneficiarioId":"ben-001","convenioId":"conv-1"}')"
split_body_code "$raw"
[[ "$CODE" == "201" ]] || { echo "authorize failed: $CODE $BODY" >&2; exit 1; }
AUTH_ID="$(json_field "$BODY" autorizacaoId)"
[[ -n "$AUTH_ID" ]]

raw="$(get_json /api/v1/beneficiarios/ben-001/agenda)"
split_body_code "$raw"
[[ "$CODE" == "200" ]] || { echo "agenda failed: $CODE $BODY" >&2; exit 1; }
PARCELA="$(python3 -c 'import json,sys; a=json.load(sys.stdin); print(a[0]["parcelaId"])' <<<"$BODY")"

IDEM="e2e-confirm-$(uuidgen | tr '[:upper:]' '[:lower:]')"
CREATE_BODY=$(cat <<EOF
{"beneficiarioId":"ben-001","parcelas":["${PARCELA}"],"credorCessionario":"creditor-e2e","contaRecebimento":{"banco":"001","agencia":"1234","conta":"99999-0"}}
EOF
)
raw="$(post_json /api/v1/operacoes-portabilidade "$CREATE_BODY" -H "Idempotency-Key: ${IDEM}")"
split_body_code "$raw"
[[ "$CODE" == "202" ]] || { echo "create failed: $CODE $BODY" >&2; exit 1; }
OP="$(json_field "$BODY" operationId)"

# Same key + same payload returns same operation
raw="$(post_json /api/v1/operacoes-portabilidade "$CREATE_BODY" -H "Idempotency-Key: ${IDEM}")"
split_body_code "$raw"
[[ "$CODE" == "202" ]] || { echo "idempotent replay failed: $CODE $BODY" >&2; exit 1; }
OP2="$(json_field "$BODY" operationId)"
[[ "$OP" == "$OP2" ]] || { echo "idempotency identity mismatch" >&2; exit 1; }

# Mismatch payload -> 409
BAD_BODY=$(cat <<EOF
{"beneficiarioId":"ben-001","parcelas":["${PARCELA}"],"credorCessionario":"other-creditor","contaRecebimento":{"banco":"001","agencia":"1234","conta":"99999-0"}}
EOF
)
raw="$(post_json /api/v1/operacoes-portabilidade "$BAD_BODY" -H "Idempotency-Key: ${IDEM}")"
split_body_code "$raw"
[[ "$CODE" == "409" ]] || { echo "expected 409, got $CODE $BODY" >&2; exit 1; }

wait_terminal "$OP" "CONFIRMADA"

events_raw="$(get_json "/api/v1/operacoes-portabilidade/${OP}/eventos")"
split_body_code "$events_raw"
[[ "$CODE" == "200" ]] || { echo "events failed: $CODE $BODY" >&2; exit 1; }
python3 -c 'import json,sys; ev=json.load(sys.stdin); assert len(ev)>=1; assert ev[0]["sequence"]==1' <<<"$BODY"

echo "== rejection path =="
raw="$(post_json /api/v1/autorizacoes '{"beneficiarioId":"ben-001-reject","convenioId":"conv-1"}')"
split_body_code "$raw"
[[ "$CODE" == "201" ]] || { echo "reject authorize failed: $CODE $BODY" >&2; exit 1; }

raw="$(get_json /api/v1/beneficiarios/ben-001-reject/agenda)"
split_body_code "$raw"
[[ "$CODE" == "200" ]] || { echo "reject agenda failed: $CODE $BODY" >&2; exit 1; }
PARCELA_R="$(python3 -c 'import json,sys; a=json.load(sys.stdin); print(a[0]["parcelaId"])' <<<"$BODY")"

IDEM_R="e2e-reject-$(uuidgen | tr '[:upper:]' '[:lower:]')"
CREATE_R=$(cat <<EOF
{"beneficiarioId":"ben-001-reject","parcelas":["${PARCELA_R}"],"credorCessionario":"creditor-e2e","contaRecebimento":{"banco":"001","agencia":"1234","conta":"88888-0"}}
EOF
)
raw="$(post_json /api/v1/operacoes-portabilidade "$CREATE_R" -H "Idempotency-Key: ${IDEM_R}")"
split_body_code "$raw"
[[ "$CODE" == "202" ]] || { echo "reject create failed: $CODE $BODY" >&2; exit 1; }
OP_R="$(json_field "$BODY" operationId)"
wait_terminal "$OP_R" "REJEITADA"

echo "DUAL_STACK_E2E_PASS"
