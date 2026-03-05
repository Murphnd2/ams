-- V041: Add reviewer tracking fields to application table
-- Prerequisite: V040
-- Note: Columns may already exist on some environments (added pre-migration).
-- Uses conditional ADD COLUMN to avoid errors on duplicate columns.

SET @db = DATABASE();

-- reviewed_by
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'application' AND COLUMN_NAME = 'reviewed_by');
SET @sql = IF(@col = 0, 'ALTER TABLE application ADD COLUMN reviewed_by BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- review_notes
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'application' AND COLUMN_NAME = 'review_notes');
SET @sql = IF(@col = 0, 'ALTER TABLE application ADD COLUMN review_notes TEXT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- date_reviewed
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'application' AND COLUMN_NAME = 'date_reviewed');
SET @sql = IF(@col = 0, 'ALTER TABLE application ADD COLUMN date_reviewed TIMESTAMP NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- FK constraint (only if not already present)
SET @fk = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
           WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'application' AND CONSTRAINT_NAME = 'fk_application_reviewed_by');
SET @sql = IF(@fk = 0, 'ALTER TABLE application ADD CONSTRAINT fk_application_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES assignee(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V041', 'Add reviewer tracking fields to application table',
        'V041__application_reviewer_fields.sql', NOW());
