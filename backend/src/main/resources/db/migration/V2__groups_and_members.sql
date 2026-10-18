-- Groups
CREATE TABLE expense_groups (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    currency CHAR(3) NOT NULL,
    created_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_expense_groups_created_by ON expense_groups (created_by);

-- Membership
CREATE TABLE group_members (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES expense_groups (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id),
    role VARCHAR(20) NOT NULL CHECK (role IN ('OWNER', 'MEMBER')),
    joined_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_group_members_group_user UNIQUE (group_id, user_id)
);

CREATE INDEX ix_group_members_user ON group_members (user_id);