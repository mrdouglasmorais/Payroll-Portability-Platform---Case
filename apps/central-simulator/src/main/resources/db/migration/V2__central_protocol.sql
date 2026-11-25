-- M3: Central simulator protocol persistence

CREATE TABLE authorizations (
    id                  UUID PRIMARY KEY,
    beneficiary_id      TEXT NOT NULL,
    agreement_id        TEXT NOT NULL,
    authorization_id    TEXT NOT NULL UNIQUE,
    status              TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT authorizations_status_chk CHECK (status IN ('ACCEPTED', 'REJECTED')),
    CONSTRAINT uq_authorization_beneficiary_agreement UNIQUE (beneficiary_id, agreement_id)
);

CREATE TABLE agenda_installments (
    id                      UUID PRIMARY KEY,
    external_installment_id TEXT NOT NULL UNIQUE,
    beneficiary_id          TEXT NOT NULL,
    agreement_id            TEXT NOT NULL,
    amount                  NUMERIC(19, 2) NOT NULL,
    discount_date           DATE NOT NULL,
    status                  TEXT NOT NULL,
    CONSTRAINT agenda_amount_chk CHECK (amount >= 0),
    CONSTRAINT agenda_status_chk CHECK (status IN ('OPEN', 'RESERVED', 'PORTED', 'CLOSED'))
);

CREATE TABLE portability_protocols (
    id                      UUID PRIMARY KEY,
    protocol                TEXT NOT NULL UNIQUE,
    beneficiary_id          TEXT NOT NULL,
    assignee_creditor       TEXT NOT NULL,
    receiving_bank          TEXT NOT NULL,
    receiving_agency        TEXT NOT NULL,
    receiving_account       TEXT NOT NULL,
    business_status         TEXT NOT NULL,
    rejection_reason        TEXT,
    fault_mode              TEXT NOT NULL DEFAULT 'NONE',
    confirm_after           TIMESTAMPTZ,
    correlation_id          TEXT NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT protocol_status_chk CHECK (
        business_status IN ('PENDENTE', 'CONFIRMADA', 'REJEITADA')
    )
);

CREATE TABLE protocol_installments (
    protocol_id             UUID NOT NULL REFERENCES portability_protocols (id),
    external_installment_id TEXT NOT NULL,
    PRIMARY KEY (protocol_id, external_installment_id)
);

CREATE TABLE installment_reservations (
    id                      UUID PRIMARY KEY,
    external_installment_id TEXT NOT NULL,
    protocol_id             UUID NOT NULL REFERENCES portability_protocols (id),
    status                  TEXT NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT central_reservation_status_chk CHECK (status IN ('ACTIVE', 'RELEASED', 'CONSUMED'))
);

CREATE UNIQUE INDEX uq_central_active_reservation
    ON installment_reservations (external_installment_id)
    WHERE status = 'ACTIVE';

CREATE TABLE idempotency_keys (
    id               UUID PRIMARY KEY,
    scope            TEXT NOT NULL,
    idempotency_key  TEXT NOT NULL,
    request_hash     TEXT NOT NULL,
    protocol_id      UUID REFERENCES portability_protocols (id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_central_idempotency UNIQUE (scope, idempotency_key)
);

CREATE TABLE seen_nonces (
    nonce        TEXT PRIMARY KEY,
    seen_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_seen_nonces_expires ON seen_nonces (expires_at);

-- Synthetic seed data for local demos (no real PII)
INSERT INTO agenda_installments (
    id, external_installment_id, beneficiary_id, agreement_id, amount, discount_date, status
) VALUES
    ('11111111-1111-1111-1111-111111111101', 'parcela-seed-1', 'ben-001', 'conv-1', 150.00, DATE '2026-12-10', 'OPEN'),
    ('11111111-1111-1111-1111-111111111102', 'parcela-seed-2', 'ben-001', 'conv-1', 175.50, DATE '2027-01-10', 'OPEN'),
    ('11111111-1111-1111-1111-111111111103', 'parcela-seed-3', 'ben-002', 'conv-2', 90.00,  DATE '2026-12-15', 'OPEN');
