#!/usr/bin/env bash
# Explicit laboratory reset for demo/UI parcels. NOT a Flyway migration.
# Safe to run only against local Compose DBs — never against shared/prod-like volumes.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "WARNING: This reopens selected Central agenda parcels and clears ACTIVE reservations."
echo "It does not reverse platform CONFIRMADA operations. Prefer docker compose down -v for a clean lab."
echo

docker compose exec -T central-db psql -U central -d central <<'SQL'
UPDATE agenda_installments
SET status = 'OPEN',
    effective_bank = COALESCE(effective_bank, '000'),
    effective_agency = COALESCE(effective_agency, '0001'),
    effective_account = COALESCE(effective_account, '00000-0')
WHERE external_installment_id IN (
  'parcela-ui-1', 'parcela-ui-2', 'parcela-seed-1', 'parcela-seed-2'
);

DELETE FROM installment_reservations
WHERE external_installment_id IN (
  'parcela-ui-1', 'parcela-ui-2', 'parcela-seed-1', 'parcela-seed-2'
)
  AND status = 'ACTIVE';

SELECT external_installment_id, beneficiary_id, status
FROM agenda_installments
WHERE external_installment_id IN (
  'parcela-ui-1', 'parcela-ui-2', 'parcela-seed-1', 'parcela-seed-2'
)
ORDER BY 1;
SQL

docker compose exec -T platform-db psql -U portability -d portability <<'SQL'
-- Align local OPEN snapshots for the same demo ids when still non-terminal financially.
UPDATE installment_snapshots
SET status = 'OPEN'
WHERE external_installment_id IN (
  'parcela-ui-1', 'parcela-ui-2', 'parcela-seed-1', 'parcela-seed-2'
)
  AND status IN ('OPEN', 'RESERVED', 'PORTED');

DELETE FROM installment_reservations
WHERE external_installment_id IN (
  'parcela-ui-1', 'parcela-ui-2', 'parcela-seed-1', 'parcela-seed-2'
)
  AND status = 'ACTIVE';
SQL

echo "LAB_RESET_DEMO_PARCELS_DONE"
echo "Note: previously CONFIRMADA platform operations are not rewritten; recreate volumes for a clean slate."
