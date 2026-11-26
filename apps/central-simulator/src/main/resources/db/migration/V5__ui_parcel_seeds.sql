-- M6: dedicated OPEN parcels for Playwright operator UI (isolated from k6 contention)

INSERT INTO agenda_installments (
    id, external_installment_id, beneficiary_id, agreement_id, amount, discount_date, status
) VALUES
    ('11111111-1111-1111-1111-111111111401', 'parcela-ui-1', 'ben-ui', 'conv-1', 150.00, DATE '2027-03-01', 'OPEN'),
    ('11111111-1111-1111-1111-111111111402', 'parcela-ui-2', 'ben-ui', 'conv-1', 160.00, DATE '2027-03-02', 'OPEN')
ON CONFLICT (external_installment_id) DO NOTHING;
