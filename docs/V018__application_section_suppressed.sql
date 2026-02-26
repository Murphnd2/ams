-- =============================================================================
-- V018 — Add suppressed column to applicationsection
-- =============================================================================
-- Prerequisite: V003 (applicationsection table created)
-- Purpose: Enable soft-delete/hide for application sections in Service Manager
-- =============================================================================

ALTER TABLE applicationsection
    ADD COLUMN suppressed TINYINT(1) NOT NULL DEFAULT 0 AFTER sort_order;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script, installed_on)
VALUES ('V018', 'Application section suppressed column', 'V018__application_section_suppressed.sql', NOW());
