-- V11 is a documentation marker only (same policy as V9).
-- There is NO executable "clean seed" / DELETE available here.
--
-- Canonical synthetic inventory (demos / E2E / k6) is documented in:
--   docs/architecture/seed-inventory.md
--
-- Legacy rows from V2 (parcela-seed-3 / ben-002) may still exist on fresh installs.
-- Do not rewrite historical migrations. Lab reset = docker compose down -v
-- or scripts/lab-reset-demo-parcels.sh for demo parcels.
--
-- If an older checksum of this file was applied on a disposable volume and
-- Flyway validate fails after this no-op content change: recreate volumes
-- (docker compose down -v) — expected for lab DBs.

SELECT 1;
