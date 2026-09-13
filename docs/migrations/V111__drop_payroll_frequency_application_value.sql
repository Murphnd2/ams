-- V111: drop payroll_frequency.application_value. S57-P4.
--
-- Superseded by paycycle_frequency_alias (V110). application_value (V107) mapped a
-- paycycle_frequency answer to a schedule at per-schedule grain, and had exactly one runtime
-- reader -- a preselection path (EnrollmentMatrixServlet) that returned the first match by
-- sort order, which is wrong for three of every four bi-weekly employers (one answer
-- legitimately matches Thursday/Friday x cycle A/B, and a single string can't discriminate
-- between them). paycycle_frequency_alias maps answer text to a recurrence token at per-answer
-- grain, and V110's pay_dow / anchor_date / semimonthly_variant columns on payroll_frequency
-- itself carry the discrimination application_value could not express. Everything the column
-- could say is already said by the alias table plus those columns; the reverse is not true.
--
-- The column was never emitted to Summit and never read by any exporter, importer, or report
-- -- display-only on the PayrollFrequencyAdmin screen plus the one preselection reader above,
-- both removed in this run's companion Java/JSP edits.
--
-- Payroll-frequency preselection is intentionally absent after this migration until the
-- TA-15 matrix dropdown filter ships. This is a deliberate, accepted regression, not an
-- oversight.
--
-- NO INSERT INTO constant anywhere in this migration.
--
-- Idempotency guard: information_schema + PREPARE/EXECUTE (V108's pattern), inverted for a
-- DROP -- only runs if the column is still there.
--
-- Reversal: one guarded ADD COLUMN application_value VARCHAR(64) NULL. No data to restore --
-- the column held preselection hints only, never a source of truth.

SET @db = DATABASE();

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'payroll_frequency' AND COLUMN_NAME = 'application_value');
SET @sql = IF(@col = 1,
    'ALTER TABLE payroll_frequency DROP COLUMN application_value',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V111' AS version, '2026-09-13' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V111', 'Drop payroll_frequency.application_value, superseded by paycycle_frequency_alias (TA-16)', 'V111__drop_payroll_frequency_application_value.sql', NOW());
