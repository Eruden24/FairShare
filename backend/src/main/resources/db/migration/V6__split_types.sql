-- How an expense is split. Existing expenses were all split equally.
ALTER TABLE expenses
    ADD COLUMN split_type VARCHAR(20) NOT NULL DEFAULT 'EQUAL'
        CHECK (split_type IN ('EQUAL', 'EXACT', 'PERCENTAGE', 'SHARES'));
ALTER TABLE expenses ALTER COLUMN split_type DROP DEFAULT;

-- What the participant entered for PERCENTAGE (33.33) and SHARES (2) splits, so the expense can be
-- shown and edited the way it was created. NULL for EQUAL and EXACT, where amount_minor says it all.
ALTER TABLE expense_splits ADD COLUMN split_value NUMERIC(15, 3);
