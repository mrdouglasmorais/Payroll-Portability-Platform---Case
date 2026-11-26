-- M9: vigente → efetivada only on confirm (parity with platform)
ALTER TABLE agenda_installments
    ADD COLUMN IF NOT EXISTS effective_bank TEXT,
    ADD COLUMN IF NOT EXISTS effective_agency TEXT,
    ADD COLUMN IF NOT EXISTS effective_account TEXT;

UPDATE agenda_installments
SET effective_bank = COALESCE(effective_bank, '000'),
    effective_agency = COALESCE(effective_agency, '0001'),
    effective_account = COALESCE(effective_account, '00000-0')
WHERE effective_bank IS NULL;
