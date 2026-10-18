-- Expenses: amounts are in minor units (cents) of the group's currency
CREATE TABLE expenses (
    id UUID PRIMARY KEY,
    group_id UUID NOT NULL REFERENCES groups (id) ON DELETE CASCADE,
    description VARCHAR(200) NOT NULL,
    amount_minor BIGINT NOT NULL CHECK (amount_minor > 0),
    paid_by UUID NOT NULL REFERENCES users (id),
    expense_date DATE NOT NULL,
    created_by UUID NOT NULL REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_expenses_group_date ON expenses (group_id, expense_date DESC, created_at DESC);

-- Each participant's share of an expense. A share can be 0 (2 cents split between 3 people).
CREATE TABLE expense_splits (
    id UUID PRIMARY KEY,
    expense_id UUID NOT NULL REFERENCES expenses (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users (id),
    amount_minor BIGINT NOT NULL CHECK (amount_minor >= 0),
    CONSTRAINT ux_expense_splits_expense_user UNIQUE (expense_id, user_id)
);

CREATE INDEX ix_expense_splits_user ON expense_splits (user_id);
