-- M6: synthetic beneficiaries for deterministic reject/unavailable E2E paths

INSERT INTO agenda_installments (
    id, external_installment_id, beneficiary_id, agreement_id, amount, discount_date, status
) VALUES
    ('11111111-1111-1111-1111-111111111201', 'parcela-reject-1', 'ben-001-reject', 'conv-1', 120.00, DATE '2026-12-20', 'OPEN'),
    ('11111111-1111-1111-1111-111111111202', 'parcela-503-1', 'ben-001-503', 'conv-1', 80.00, DATE '2026-12-22', 'OPEN')
ON CONFLICT (external_installment_id) DO NOTHING;
