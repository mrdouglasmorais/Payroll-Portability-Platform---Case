-- M1 bootstrap schema placeholder. Domain tables arrive in M2.
CREATE TABLE IF NOT EXISTS schema_bootstrap (
    id SMALLINT PRIMARY KEY DEFAULT 1,
    initialized_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT schema_bootstrap_singleton CHECK (id = 1)
);

INSERT INTO schema_bootstrap (id)
VALUES (1)
ON CONFLICT (id) DO NOTHING;
