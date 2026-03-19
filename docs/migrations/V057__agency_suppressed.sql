-- V057: Add suppressed flag to agency table
-- Allows PSP admins to suppress agencies, removing them from active workflows
-- while preserving historical data.

ALTER TABLE agency ADD COLUMN suppressed TINYINT(1) NOT NULL DEFAULT 0;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V057' AS version, '2026-03-19' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V057', 'Add suppressed flag to agency table', 'V057__agency_suppressed.sql', NOW());
