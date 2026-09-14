-- V113: enrollment_matrix_participant.agent_schedule_note -- the agent's free-text note about a
-- participant's payroll schedule. S58-P7.
--
-- The agent view (/matrix/{guid}) is now an editing surface, and the agent does most of the
-- enrollment data entry. custom_schedule_name is the Summit-bound schedule name
-- (SummitExportServlet.resolveScheduleName emits it verbatim into HRA column F / 125 column I),
-- so an agent must never write it. When the agent picks OTHER_CUSTOM / OTHER_NOT_IMPORTABLE they
-- record what they learned HERE; a PSP staffer reads it on the PSP matrix page and sets the real
-- schedule. PSP can read and edit this column; the exporter never reads it.
--
-- "Needs PSP confirmation" is DERIVED from payroll_frequency being one of the OTHER_* sentinels
-- (MatrixCompletenessService), not stored -- selecting a real schedule clears it by itself. No
-- flag column here.
--
-- Sized for a sentence or two. Nullable; NO backfill.
--
-- NO INSERT INTO constant anywhere in this migration.
--
-- Idempotency guard: information_schema.COLUMNS + PREPARE/EXECUTE (V108/V110's pattern).
--
-- Reversal: ALTER TABLE enrollment_matrix_participant DROP COLUMN agent_schedule_note. Notes are
-- lost; nothing else reads the column.

SET @db = DATABASE();

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'enrollment_matrix_participant' AND COLUMN_NAME = 'agent_schedule_note');
SET @sql = IF(@col = 0,
    'ALTER TABLE enrollment_matrix_participant ADD COLUMN agent_schedule_note VARCHAR(500) NULL COMMENT ''Agent free text about this participant''''s payroll schedule, written from /matrix/{guid}. Read by PSP to set custom_schedule_name. Never exported.'' AFTER custom_schedule_name',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V113' AS version, '2026-09-13' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V113', 'enrollment_matrix_participant.agent_schedule_note, nullable free text from the agent matrix view (S58-P7)', 'V113__enrollment_matrix_participant_agent_note.sql', NOW());
