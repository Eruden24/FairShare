-- Groups: expense_groups -> groups
ALTER TABLE expense_groups RENAME TO groups;
ALTER TABLE groups RENAME CONSTRAINT expense_groups_pkey TO groups_pkey;
ALTER TABLE groups RENAME CONSTRAINT expense_groups_created_by_fkey TO groups_created_by_fkey;
ALTER INDEX ix_expense_groups_created_by RENAME TO ix_groups_created_by;

-- Membership: group_members -> user_groups
ALTER TABLE group_members RENAME TO user_groups;
ALTER TABLE user_groups RENAME CONSTRAINT group_members_pkey TO user_groups_pkey;
ALTER TABLE user_groups RENAME CONSTRAINT group_members_group_id_fkey TO user_groups_group_id_fkey;
ALTER TABLE user_groups RENAME CONSTRAINT group_members_user_id_fkey TO user_groups_user_id_fkey;
ALTER TABLE user_groups RENAME CONSTRAINT group_members_role_check TO user_groups_role_check;
ALTER TABLE user_groups RENAME CONSTRAINT ux_group_members_group_user TO ux_user_groups_group_user;
ALTER INDEX ix_group_members_user RENAME TO ix_user_groups_user;
