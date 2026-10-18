-- A payment between two members to settle up, e.g. Carol pays Bob back 30.00.
-- Amounts are in minor units of the group's currency.
CREATE TABLE settlements (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    from_user UUID NOT NULL REFERENCES users (id),
    to_user UUID NOT NULL REFERENCES users (id),
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    settled_on DATE NOT NULL,
    created_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_settlements_different_users CHECK (from_user <> to_user)
);

CREATE INDEX ix_settlements_group_date ON settlements (group_id, settled_on DESC, created_at DESC);
