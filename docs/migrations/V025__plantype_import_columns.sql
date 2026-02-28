-- =============================================================================
-- V025: Add level, los, employer_name columns to plantype table
--
-- Supports Summit Plan Type import. These columns capture metadata from
-- Summit's pre-defined Plan Type export that isn't currently stored:
--   level        — "System Default", "TPA Custom", "Employer Custom"
--   los          — Line of Service ("CDH", "COBRA", "CDH, COBRA")
--   employer_name — only populated for Employer Custom level types
-- =============================================================================

ALTER TABLE plantype
    ADD COLUMN level VARCHAR(20) NULL AFTER PlanTypeName,
    ADD COLUMN los VARCHAR(50) NULL AFTER level,
    ADD COLUMN employer_name VARCHAR(255) NULL AFTER los;

-- Self-register this migration
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V025', 'Add level, los, employer_name to plantype for Summit import', 'V025__plantype_import_columns.sql', NOW());
