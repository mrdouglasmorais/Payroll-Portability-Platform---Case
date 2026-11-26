#!/usr/bin/env bash
# Laboratory helper — insert ADDITIONAL synthetic OPEN parcels for ben-ui.
#
# Does NOT reopen PORTED/RESERVED rows (forbidden financial mutation).
# Use when demo parcels were already ported and you need more OPEN inventory
# without wiping volumes (prefer docker compose down -v for a full clean lab).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if ! docker compose ps --status running --services 2>/dev/null | grep -qx platform-db; then
  echo "ERROR: platform-db is not running" >&2
  exit 1
fi
if ! docker compose ps --status running --services 2>/dev/null | grep -qx central-db; then
  echo "ERROR: central-db is not running" >&2
  exit 1
fi

SUFFIX="${1:-$(date +%s)}"
P1="parcela-ui-extra-${SUFFIX}-a"
P2="parcela-ui-extra-${SUFFIX}-b"

echo "Seeding Central OPEN parcels ${P1}, ${P2} for ben-ui/conv-1"
docker compose exec -T central-db psql -U central -d central <<SQL
INSERT INTO agenda_installments (
  id, external_installment_id, beneficiary_id, agreement_id, amount, discount_date, status
) VALUES
  (gen_random_uuid(), '${P1}', 'ben-ui', 'conv-1', 190.00, DATE '2027-05-01', 'OPEN'),
  (gen_random_uuid(), '${P2}', 'ben-ui', 'conv-1', 195.00, DATE '2027-05-02', 'OPEN')
ON CONFLICT (external_installment_id) DO NOTHING;
SQL

echo "Ensuring Central authorization for ben-ui/conv-1"
docker compose exec -T central-db psql -U central -d central <<SQL
INSERT INTO authorizations (id, beneficiary_id, agreement_id, authorization_id, status, created_at)
VALUES (gen_random_uuid(), 'ben-ui', 'conv-1', 'auth-lab-extra-${SUFFIX}', 'ACCEPTED', now())
ON CONFLICT (beneficiary_id, agreement_id)
DO UPDATE SET status = 'ACCEPTED';
SQL

# Heal dual-DB drift: platform CONSUMED reservations must not remain OPEN on Central.
CONSUMED=$(docker compose exec -T platform-db psql -U portability -d portability -Atc \
  "SELECT string_agg(quote_literal(external_installment_id), ',') FROM installment_reservations WHERE status='CONSUMED';" || true)
if [[ -n "${CONSUMED}" ]]; then
  echo "Healing Central OPEN→PORTED for platform-CONSUMED parcels"
  docker compose exec -T central-db psql -U central -d central -c \
    "UPDATE agenda_installments SET status='PORTED' WHERE status='OPEN' AND external_installment_id IN (${CONSUMED});"
fi

echo "LAB_SEED_OK parcels=${P1},${P2}"
echo "Re-authorize in the console (ben-ui / conv-1) to sync snapshots."
