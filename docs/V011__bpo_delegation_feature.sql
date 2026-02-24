-- =============================================================================
-- V011: BPO Delegation Feature
-- Date: February 24, 2026
-- Prerequisites: V010 (PSP opportunity integration)
--
-- Adds BPO delegation fields to the todo table and creates the todo_note
-- table for PSP↔BPO communication on individual tasks.
--
-- Also adds BPO user roles (101, 102, 103) if not already present.
-- =============================================================================

-- ============================================================================
-- STEP 1: Add BPO delegation columns to todo table
-- ============================================================================

ALTER TABLE todo
    ADD COLUMN bpo_completed TINYINT(1) NOT NULL DEFAULT 0
        AFTER is_complete,
    ADD COLUMN bpo_completed_date DATE DEFAULT NULL
        AFTER bpo_completed,
    ADD COLUMN bpo_completed_by_id BIGINT DEFAULT NULL
        AFTER bpo_completed_date,
    ADD COLUMN bpo_assigned_to_id BIGINT DEFAULT NULL
        AFTER bpo_completed_by_id;

-- Foreign keys
ALTER TABLE todo
    ADD CONSTRAINT fk_todo_bpo_completed_by
        FOREIGN KEY (bpo_completed_by_id) REFERENCES person(person_id),
    ADD CONSTRAINT fk_todo_bpo_assigned_to
        FOREIGN KEY (bpo_assigned_to_id) REFERENCES person(person_id);

-- ============================================================================
-- STEP 2: Create todo_note table for task-level notes
-- ============================================================================

CREATE TABLE IF NOT EXISTS todo_note (
    note_id BIGINT NOT NULL AUTO_INCREMENT,
    todo_id BIGINT NOT NULL,
    created_by_id BIGINT NOT NULL,
    note_text TEXT NOT NULL,
    source_type VARCHAR(20) DEFAULT 'PSP',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (note_id),
    CONSTRAINT fk_todonote_todo FOREIGN KEY (todo_id) REFERENCES todo(todo_id),
    CONSTRAINT fk_todonote_person FOREIGN KEY (created_by_id) REFERENCES person(person_id)
);

CREATE INDEX idx_todonote_todo ON todo_note(todo_id);
CREATE INDEX idx_todonote_created ON todo_note(created_at);

-- ============================================================================
-- STEP 3: Ensure BPO user roles exist
-- ============================================================================

INSERT IGNORE INTO userrole (id, description) VALUES (101, 'Accelergent BPO');
INSERT IGNORE INTO userrole (id, description) VALUES (102, 'Accelergent BPO Admin');
INSERT IGNORE INTO userrole (id, description) VALUES (103, 'Accelergent BPO User');

-- ============================================================================
-- STEP 4: Register this migration
-- ============================================================================

INSERT IGNORE INTO schema_version (version, description, script_name)
VALUES ('V011', 'BPO delegation feature - todo BPO columns and todo_note table', 'V011__bpo_delegation_feature.sql');

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

-- DESCRIBE todo;
-- DESCRIBE todo_note;
-- SELECT * FROM userrole WHERE id IN (101, 102, 103);
-- SELECT * FROM schema_version WHERE version = 'V011';

-- ============================================================================
-- ROLLBACK (if needed)
-- ============================================================================

-- ALTER TABLE todo DROP FOREIGN KEY fk_todo_bpo_completed_by;
-- ALTER TABLE todo DROP FOREIGN KEY fk_todo_bpo_assigned_to;
-- ALTER TABLE todo DROP COLUMN bpo_completed;
-- ALTER TABLE todo DROP COLUMN bpo_completed_date;
-- ALTER TABLE todo DROP COLUMN bpo_completed_by_id;
-- ALTER TABLE todo DROP COLUMN bpo_assigned_to_id;
-- DROP TABLE IF EXISTS todo_note;
-- DELETE FROM schema_version WHERE version = 'V011';
