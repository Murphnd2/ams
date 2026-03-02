-- =============================================================================
-- V031: ToDoNote Cross-System Nullable Columns
--
-- Makes todo_id and created_by_id nullable for cross-system note storage.
-- Adds author_name for display when created_by_id is null (cross-system notes).
-- =============================================================================

ALTER TABLE todo_note MODIFY todo_id BIGINT NULL;
ALTER TABLE todo_note MODIFY created_by_id BIGINT NULL;
ALTER TABLE todo_note ADD COLUMN author_name VARCHAR(100) NULL;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V031', 'ToDoNote cross-system: nullable todo_id/created_by_id, author_name column', 'V031__todo_note_cross_system_nullable.sql', NOW());
