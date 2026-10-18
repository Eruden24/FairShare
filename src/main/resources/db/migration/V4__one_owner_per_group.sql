-- A group has exactly one OWNER at a time
CREATE UNIQUE INDEX ux_user_groups_one_owner ON user_groups (group_id) WHERE role = 'OWNER';
