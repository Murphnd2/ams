-- V055: Create schema_info view for structural version identification
-- This view IS the version — it travels with any DB clone/backup automatically.
-- Each future migration should include:
--   CREATE OR REPLACE VIEW schema_info AS SELECT 'V0XX' AS version, 'YYYY-MM-DD' AS updated;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V055' AS version, '2026-03-15' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V055', 'Schema info view for structural version identification', 'V055__schema_info_view.sql', NOW());
