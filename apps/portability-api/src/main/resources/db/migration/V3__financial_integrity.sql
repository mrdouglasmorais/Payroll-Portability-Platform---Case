-- M8: effective receiving account on installments (vigente → efetivada only on confirm)
-- and job claim index covering expired RUNNING leases.

ALTER TABLE installment_snapshots
    ADD COLUMN IF NOT EXISTS effective_bank TEXT,
    ADD COLUMN IF NOT EXISTS effective_agency TEXT,
    ADD COLUMN IF NOT EXISTS effective_account TEXT;

-- Synthetic originals for demos (not real PII)
UPDATE installment_snapshots
SET effective_bank = COALESCE(effective_bank, '000'),
    effective_agency = COALESCE(effective_agency, '0001'),
    effective_account = COALESCE(effective_account, '00000-0')
WHERE effective_bank IS NULL;

DROP INDEX IF EXISTS idx_jobs_due;

CREATE INDEX idx_jobs_claimable ON processing_jobs (next_run_at, id)
    WHERE technical_status IN ('PENDING', 'FAILED', 'RUNNING');
