-- Optimistic locking: every change to an expense increases its version. A client that sends an
-- old version when editing gets 409 instead of silently overwriting someone else's change.
ALTER TABLE expenses ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
