-- M2: platform domain persistence. Monetary amounts use NUMERIC, never floating point.

CREATE TABLE beneficiaries (
    id              UUID PRIMARY KEY,
    external_ref    TEXT NOT NULL UNIQUE,
    display_name    TEXT NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE consents (
    id                  UUID PRIMARY KEY,
    beneficiary_id      UUID NOT NULL REFERENCES beneficiaries (id),
    agreement_id        TEXT NOT NULL,
    authorization_id    TEXT UNIQUE,
    status              TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT consents_status_chk CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    CONSTRAINT consents_auth_when_accepted_chk CHECK (
        (status = 'ACCEPTED' AND authorization_id IS NOT NULL)
        OR (status <> 'ACCEPTED')
    )
);

CREATE UNIQUE INDEX uq_consents_active_agreement
    ON consents (beneficiary_id, agreement_id)
    WHERE status IN ('PENDING', 'ACCEPTED');

CREATE TABLE installment_snapshots (
    id                      UUID PRIMARY KEY,
    external_installment_id TEXT NOT NULL UNIQUE,
    beneficiary_id          UUID NOT NULL REFERENCES beneficiaries (id),
    agreement_id            TEXT NOT NULL,
    amount                  NUMERIC(19, 2) NOT NULL,
    discount_date           DATE NOT NULL,
    status                  TEXT NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT installment_amount_chk CHECK (amount >= 0),
    CONSTRAINT installment_status_chk CHECK (
        status IN ('OPEN', 'RESERVED', 'PORTED', 'CLOSED')
    )
);

CREATE TABLE portability_operations (
    id                  UUID PRIMARY KEY,
    beneficiary_id      UUID NOT NULL REFERENCES beneficiaries (id),
    assignee_creditor   TEXT NOT NULL,
    receiving_bank      TEXT NOT NULL,
    receiving_agency    TEXT NOT NULL,
    receiving_account   TEXT NOT NULL,
    business_status     TEXT NOT NULL,
    rejection_reason    TEXT,
    central_protocol    TEXT UNIQUE,
    correlation_id      TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT operations_status_chk CHECK (
        business_status IN ('PENDENTE', 'CONFIRMADA', 'REJEITADA')
    ),
    CONSTRAINT operations_terminal_reason_chk CHECK (
        (business_status = 'REJEITADA' AND rejection_reason IS NOT NULL)
        OR (business_status <> 'REJEITADA')
    )
);

CREATE TABLE operation_installments (
    operation_id            UUID NOT NULL REFERENCES portability_operations (id),
    installment_id          UUID NOT NULL REFERENCES installment_snapshots (id),
    external_installment_id TEXT NOT NULL,
    PRIMARY KEY (operation_id, installment_id)
);

CREATE TABLE installment_reservations (
    id                      UUID PRIMARY KEY,
    external_installment_id TEXT NOT NULL,
    operation_id            UUID NOT NULL REFERENCES portability_operations (id),
    status                  TEXT NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    released_at             TIMESTAMPTZ,
    CONSTRAINT reservation_status_chk CHECK (status IN ('ACTIVE', 'RELEASED', 'CONSUMED'))
);

-- Concurrency invariant: one active reservation per installment.
CREATE UNIQUE INDEX uq_active_installment_reservation
    ON installment_reservations (external_installment_id)
    WHERE status = 'ACTIVE';

CREATE TABLE operation_events (
    id               UUID PRIMARY KEY,
    operation_id     UUID NOT NULL REFERENCES portability_operations (id),
    sequence_no      INTEGER NOT NULL,
    event_type       TEXT NOT NULL,
    description      TEXT NOT NULL,
    correlation_id   TEXT NOT NULL,
    payload_redacted JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT operation_events_sequence_chk CHECK (sequence_no >= 1),
    CONSTRAINT uq_operation_event_sequence UNIQUE (operation_id, sequence_no)
);

CREATE TABLE idempotency_keys (
    id               UUID PRIMARY KEY,
    scope            TEXT NOT NULL,
    idempotency_key  TEXT NOT NULL,
    request_hash     TEXT NOT NULL,
    operation_id     UUID REFERENCES portability_operations (id),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_idempotency_scope_key UNIQUE (scope, idempotency_key)
);

CREATE TABLE processing_jobs (
    id               UUID PRIMARY KEY,
    operation_id     UUID NOT NULL REFERENCES portability_operations (id),
    job_type         TEXT NOT NULL,
    technical_status TEXT NOT NULL,
    attempt_count    INTEGER NOT NULL DEFAULT 0,
    lease_owner      TEXT,
    lease_until      TIMESTAMPTZ,
    fence_token      BIGINT NOT NULL DEFAULT 0,
    next_run_at      TIMESTAMPTZ NOT NULL,
    last_error       TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT jobs_status_chk CHECK (
        technical_status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'EXHAUSTED')
    ),
    CONSTRAINT jobs_attempts_chk CHECK (attempt_count >= 0)
);

CREATE INDEX idx_jobs_due ON processing_jobs (next_run_at)
    WHERE technical_status IN ('PENDING', 'FAILED');

CREATE INDEX idx_operation_events_operation ON operation_events (operation_id, sequence_no);
CREATE INDEX idx_installments_beneficiary ON installment_snapshots (beneficiary_id, agreement_id);
