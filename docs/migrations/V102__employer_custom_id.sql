-- V102: employer.custom_id -- string-faithful storage of Summit's CustomID (T241)
--
-- The J1 employer import (SummitImportService.importEmployers) has always stored Summit's
-- CustomID only as `er_key` (int), via parseIntSafe, and only on insert. An AMS-composed
-- Employer TPA Custom ID such as `158E140952` (SummitExportServlet.resolveEmployerTpaCustomId,
-- separator `E`, S29-G2) is not an integer: parseIntSafe falls through to Double.parseDouble,
-- which reads it as scientific notation, overflows on the (int) cast, and saturates every such
-- value to 2147483647 (T242 -- recorded, not fixed here; er_key and every reader of it are
-- untouched by this migration).
--
-- `custom_id` stores the CustomID column verbatim (trimmed, "n/a" normalized to NULL), updated
-- on both insert and update, so the Summit setup panel's employer-lookup-by-custom-id (T241) can
-- match the exact string the export composed rather than a lossy integer.
--
-- Idempotency guard follows V097's ADD COLUMN pattern. No data changes, no INSERT INTO constant.
--
-- Reversal: DROP INDEX idx_employer_custom_id ON employer; ALTER TABLE employer DROP COLUMN
-- custom_id; -- nothing else references this column as of this migration.

SET @db = DATABASE();

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'employer' AND COLUMN_NAME = 'custom_id');
SET @sql = IF(@col = 0,
    'ALTER TABLE employer ADD COLUMN custom_id VARCHAR(64) NULL AFTER er_key',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE INDEX idx_employer_custom_id ON employer (custom_id);

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V102' AS version, '2026-09-11' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V102', 'employer.custom_id: string-faithful CustomID storage from J1 import, set on insert and update, distinct from lossy int er_key (T241/T242)', 'V102__employer_custom_id.sql', NOW());
