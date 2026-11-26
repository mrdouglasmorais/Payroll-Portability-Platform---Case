#!/usr/bin/env bash
# Laboratory helper — sync Central authorizations from platform ACCEPTED consents.
#
# Use when platform-db kept consents but central-db was recreated (agenda 403 while
# POST /autorizacoes still returns 201 from the local short-circuit path before heal).
# Prefer: POST /api/v1/autorizacoes again (API now re-asserts on Central) or
#   docker compose down -v && docker compose up --build -d
#
# NOT operational recovery and NOT a Flyway migration.
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

CONSENTS=$(
  docker compose exec -T platform-db psql -U portability -d portability -At -c "
select b.external_ref || E'\t' || c.agreement_id || E'\t' || c.authorization_id
from consents c
join beneficiaries b on b.id = c.beneficiary_id
where c.status = 'ACCEPTED';
"
)

if [[ -z "${CONSENTS}" ]]; then
  echo "No ACCEPTED platform consents to sync."
  exit 0
fi

SQL=""
while IFS=$'\t' read -r ben agr auth; do
  [[ -z "${ben:-}" ]] && continue
  SQL+="INSERT INTO authorizations (id, beneficiary_id, agreement_id, authorization_id, status, created_at)
VALUES (gen_random_uuid(), '${ben}', '${agr}', '${auth}', 'ACCEPTED', now())
ON CONFLICT (beneficiary_id, agreement_id)
DO UPDATE SET authorization_id = EXCLUDED.authorization_id, status = 'ACCEPTED';
"
  echo "queue ${ben}/${agr}"
done <<< "${CONSENTS}"

echo "${SQL}" | docker compose exec -T central-db psql -U central -d central
echo "Synced Central authorizations from platform ACCEPTED consents."
docker compose exec -T central-db psql -U central -d central -c \
  "select beneficiary_id, agreement_id, status from authorizations order by 1,2;"
