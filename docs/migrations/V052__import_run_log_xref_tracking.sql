-- =============================================================================
-- V052: Add cross-reference tracking columns to import_run_log
-- Tracks how many records were resolved via cross-reference vs direct PK,
-- and how many new PKs were allocated due to conflicts.
-- =============================================================================

ALTER TABLE import_run_log
    ADD COLUMN xref_resolved INT NOT NULL DEFAULT 0 COMMENT 'Records found via import_id_mapping cross-reference',
    ADD COLUMN pk_allocated INT NOT NULL DEFAULT 0 COMMENT 'Records that needed new PK due to conflict',
    ADD COLUMN mappings_recorded INT NOT NULL DEFAULT 0 COMMENT 'New cross-reference mappings recorded';

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V052', 'Import run log cross-reference tracking columns', 'V052__import_run_log_xref_tracking.sql', NOW());
