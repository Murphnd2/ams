-- V029: Add is_active column to user table for user deactivation
-- Defaults to TRUE so all existing users remain active.

ALTER TABLE user ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V029', 'Add is_active column to user table', 'V029__user_is_active.sql', NOW());
