-- M9 residual review: V8__reopen_ui_demo_parcels.sql reopened demo parcels and deleted
-- active reservations. That is unsafe as a structural migration on persistent lab volumes
-- that already contain confirmed operations.
--
-- Policy going forward:
--   * Do NOT silently rewrite V8 checksum/content (already applied environments).
--   * Demo parcel preparation lives in scripts/lab-reset-demo-parcels.sh (explicit, operator-run).
--   * Prefer exclusive beneficiaries per run (ben-ui, ben-recover) over mutating shared seeds.
--
-- This migration is intentionally a no-op marker for Flyway history.

SELECT 1;
