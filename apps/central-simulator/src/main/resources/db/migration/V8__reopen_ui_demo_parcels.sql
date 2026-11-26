-- Keep operator-console parcels OPEN for local demos after prior PORTED runs.
-- Idempotent upsert-style reopen (does not touch k6 load parcels).

UPDATE agenda_installments
SET status = 'OPEN',
    effective_bank = COALESCE(effective_bank, '000'),
    effective_agency = COALESCE(effective_agency, '0001'),
    effective_account = COALESCE(effective_account, '00000-0')
WHERE external_installment_id IN ('parcela-ui-1', 'parcela-ui-2', 'parcela-seed-1', 'parcela-seed-2');

DELETE FROM installment_reservations
WHERE external_installment_id IN ('parcela-ui-1', 'parcela-ui-2', 'parcela-seed-1', 'parcela-seed-2')
  AND status = 'ACTIVE';
