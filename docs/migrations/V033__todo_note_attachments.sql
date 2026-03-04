-- =============================================================================
-- V033: ToDoNote Attachments
-- Adds todo_note_id FK column to weblink table, enabling file attachments on
-- BPO task notes. Follows the same pattern as email_id FK for email attachments.
--
-- Prerequisites: V032
-- =============================================================================

ALTER TABLE weblink ADD COLUMN todo_note_id BIGINT NULL;

ALTER TABLE weblink ADD CONSTRAINT fk_weblink_todo_note
    FOREIGN KEY (todo_note_id) REFERENCES todo_note(note_id);

CREATE INDEX idx_weblink_todo_note ON weblink(todo_note_id);

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V033', 'ToDoNote attachments: todo_note_id FK on weblink', 'V033__todo_note_attachments.sql', NOW());
