-- M7: parcels reserved for lost-response / restart recovery harness

INSERT INTO agenda_installments (
    id, external_installment_id, beneficiary_id, agreement_id, amount, discount_date, status
) VALUES
    ('11111111-1111-1111-1111-111111111501', 'parcela-recover-1', 'ben-recover', 'conv-1', 130.00, DATE '2027-04-01', 'OPEN'),
    ('11111111-1111-1111-1111-111111111502', 'parcela-recover-2', 'ben-recover', 'conv-1', 140.00, DATE '2027-04-02', 'OPEN')
ON CONFLICT (external_installment_id) DO NOTHING;
