-- Extra OPEN demo parcels for Playwright a11y (desktop + mobile) without
-- reopening PORTED financial history. Disposable-lab seeds only.
-- Does not rewrite V8; V8 remains a historical hazard on clean migrate.

INSERT INTO agenda_installments (
    id, external_installment_id, beneficiary_id, agreement_id, amount, discount_date, status
) VALUES
    ('11111111-1111-1111-1111-111111111403', 'parcela-ui-3', 'ben-ui', 'conv-1', 170.00, DATE '2027-03-03', 'OPEN'),
    ('11111111-1111-1111-1111-111111111404', 'parcela-ui-4', 'ben-ui', 'conv-1', 180.00, DATE '2027-03-04', 'OPEN')
ON CONFLICT (external_installment_id) DO NOTHING;
