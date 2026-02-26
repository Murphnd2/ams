-- =============================================================================
-- V019 — Add suppressed column to applicationfield
-- =============================================================================
-- Prerequisite: V003 (applicationfield table created)
-- Purpose: Enable soft-hide for application fields in Service Manager
-- =============================================================================

ALTER TABLE applicationfield
    ADD COLUMN suppressed TINYINT(1) NOT NULL DEFAULT 0 AFTER select_options;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name)
VALUES ('V019', 'Application field suppressed column', 'V019__application_field_suppressed.sql');
