#!/usr/bin/env bash
# Laboratory helper — NOT operational recovery and NOT a Flyway migration.
#
# Partial reopening of PORTED parcels / deleting ACTIVE reservations while
# confirmed operations remain is forbidden: it creates a false financial state.
#
# Canonical clean demo reset:
#   docker compose down -v && docker compose up --build -d
#
# Prefer exclusive synthetic beneficiaries/parcels per run (ben-ui, ben-recover,
# ben-001) instead of mutating shared financial history.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

cat <<'EOF'
LAB_RESET_REFUSED

This script no longer reopens PORTED/RESERVED parcels or clears reservations
that would desynchronize confirmed operations.

For a disposable lab, recreate volumes:

  docker compose down -v
  docker compose up --build -d

For repeated demos without wiping volumes, use unused synthetic parcel IDs /
beneficiaries instead of mutating processed financial state.
EOF

exit 2
