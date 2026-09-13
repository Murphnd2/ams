-- V110: payroll_frequency seed (closes TA-10) + filter metadata columns, and a new
-- paycycle_frequency_alias table. S57-P3.
--
-- payroll_frequency (V107) has never held a row. Every Summit export to date has taken the
-- empty-string fallback for the Contribution Schedule columns (TA-10). Fifteen -- actually
-- fourteen, see the discrepancy note below -- Contribution Schedules now exist in Summit
-- under a PP- naming convention. This migration mirrors them into payroll_frequency and adds
-- the metadata columns a future matrix-dropdown filter will read.
--
-- ⚠️ THE FILTER METADATA COLUMNS AND THE ALIAS TABLE HAVE NO READER YET. The matrix dropdown
-- filter and preselection are a separate task. This migration is schema, seed data, and the
-- entity/DAO layer only.
--
-- ⚠️ Row-count discrepancy from the authoring prompt: the prompt's own header text says "15
-- rows" but its own data table lists 14 distinct codes (PP-SM-01-15 through PP-MO-1ST, no
-- 15th row present). Fourteen rows are seeded here -- exactly what the table specifies. No
-- 15th row was invented to reconcile the count.
--
-- New columns on payroll_frequency (idempotent ADD COLUMN IF NOT EXISTS, V108's
-- information_schema + PREPARE/EXECUTE pattern, one guard per column):
--   recurrence            -- WEEKLY | BIWEEKLY | SEMIMONTHLY | MONTHLY. Code-validated, not an
--                            ENUM, on this table's own established precedent.
--   semimonthly_variant   -- DAY_01_15 | DAY_15_EOM. NULL for every other recurrence.
--   pay_dow               -- THURSDAY | FRIDAY. NULL for semi-monthly and monthly.
--   anchor_date           -- a real pay date on this schedule. Bi-weekly A-vs-B parity is
--                            computed as daysBetween(employer_first_paydate, anchor_date) mod
--                            14 and is NEVER stored as a separate parity flag.
--   deduction_count       -- 24, 26, 48, 52, or 12 deductions per year on this schedule.
--   preferred             -- 1 = monthly-aligned, offered first. Discouraged every-check
--                            variants and plan-level (non-selectable) schedules are 0.
--
-- Seed: code and summit_schedule_name are IDENTICAL in every row, deliberately, so a
-- divergence between the two is visible on sight rather than hidden behind a mapping. These
-- strings must match Summit's Contribution Schedule names character for character; nothing
-- validates the match. PP-MO-1ST is the employer stipend / monthly-premium posting schedule
-- set on the benefit plan -- enrollment_approved = 0 so it can never appear in the matrix
-- dropdown; every other row is enrollment_approved = 1. The two OTHER_* sentinel rows are
-- code constants (PayrollFrequency.OTHER_CUSTOM / OTHER_NOT_IMPORTABLE), never rows in this
-- table, and are not touched here.
--
-- New table paycycle_frequency_alias maps the free-text paycycle_frequency application answer
-- to a recurrence token. alias_key = alias_text uppercased with all non-alphanumeric
-- characters stripped -- the same rule PaycycleFrequencyAliasDAO.normalize() applies, so seed
-- and lookup always agree. An application answer with no matching active alias must result in
-- NO FILTERING AT ALL when the (separate, later) filter is built -- never a partial or wrong
-- filter.
--
-- NO INSERT INTO constant anywhere in this migration.
--
-- Idempotency guard: per-column information_schema + PREPARE/EXECUTE (V108's pattern) for the
-- ALTER TABLE additions; CREATE TABLE IF NOT EXISTS + INSERT IGNORE for the new table and both
-- seeds (V107/V109's pattern).
--
-- Reversal: six ALTER TABLE ... DROP COLUMN statements on payroll_frequency, and
-- DROP TABLE paycycle_frequency_alias. Nothing outside this migration reads any of it yet, so
-- reversal changes no running behaviour.

SET @db = DATABASE();

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'payroll_frequency' AND COLUMN_NAME = 'recurrence');
SET @sql = IF(@col = 0,
    'ALTER TABLE payroll_frequency ADD COLUMN recurrence VARCHAR(20) NULL COMMENT ''WEEKLY, BIWEEKLY, SEMIMONTHLY, or MONTHLY. Code-validated, not an ENUM. No reader yet.'' AFTER sort_order',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'payroll_frequency' AND COLUMN_NAME = 'semimonthly_variant');
SET @sql = IF(@col = 0,
    'ALTER TABLE payroll_frequency ADD COLUMN semimonthly_variant VARCHAR(20) NULL COMMENT ''DAY_01_15 or DAY_15_EOM. NULL for every recurrence other than SEMIMONTHLY. No reader yet.'' AFTER recurrence',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'payroll_frequency' AND COLUMN_NAME = 'pay_dow');
SET @sql = IF(@col = 0,
    'ALTER TABLE payroll_frequency ADD COLUMN pay_dow VARCHAR(10) NULL COMMENT ''THURSDAY or FRIDAY. NULL for semi-monthly and monthly schedules. No reader yet.'' AFTER semimonthly_variant',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'payroll_frequency' AND COLUMN_NAME = 'anchor_date');
SET @sql = IF(@col = 0,
    'ALTER TABLE payroll_frequency ADD COLUMN anchor_date DATE NULL COMMENT ''A real pay date on this schedule. Bi-weekly A-vs-B parity is computed as daysBetween(employer_first_paydate, anchor_date) mod 14 and is never stored. No reader yet.'' AFTER pay_dow',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'payroll_frequency' AND COLUMN_NAME = 'deduction_count');
SET @sql = IF(@col = 0,
    'ALTER TABLE payroll_frequency ADD COLUMN deduction_count INT NULL COMMENT ''Deductions per year on this schedule: 24, 26, 48, 52, or 12. No reader yet.'' AFTER anchor_date',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'payroll_frequency' AND COLUMN_NAME = 'preferred');
SET @sql = IF(@col = 0,
    'ALTER TABLE payroll_frequency ADD COLUMN preferred TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''1 = monthly-aligned, offered first. Discouraged every-check variants and plan-level schedules are 0. No reader yet.'' AFTER deduction_count',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Seed: 14 rows (see the row-count discrepancy note above). code = summit_schedule_name in
-- every row, deliberately.
INSERT IGNORE INTO payroll_frequency
    (code, label, summit_schedule_name, recurrence, semimonthly_variant, pay_dow, anchor_date, deduction_count, preferred, sort_order, enrollment_approved, active) VALUES
('PP-SM-01-15',     'Semi-Monthly — 1st & 15th',                                    'PP-SM-01-15',     'SEMIMONTHLY', 'DAY_01_15', NULL,        NULL,         24, 1,  10, 1, 1),
('PP-SM-15-EOM',    'Semi-Monthly — 15th & last day',                              'PP-SM-15-EOM',    'SEMIMONTHLY', 'DAY_15_EOM', NULL,       NULL,         24, 1,  20, 1, 1),
('PP-BW-THU-A-24',  'Bi-Weekly Thursday, cycle A — 24 of 26',                      'PP-BW-THU-A-24',  'BIWEEKLY',    NULL,        'THURSDAY', '2026-09-17', 24, 1,  30, 1, 1),
('PP-BW-THU-B-24',  'Bi-Weekly Thursday, cycle B — 24 of 26',                      'PP-BW-THU-B-24',  'BIWEEKLY',    NULL,        'THURSDAY', '2026-09-24', 24, 1,  40, 1, 1),
('PP-BW-FRI-A-24',  'Bi-Weekly Friday, cycle A — 24 of 26',                        'PP-BW-FRI-A-24',  'BIWEEKLY',    NULL,        'FRIDAY',   '2026-09-18', 24, 1,  50, 1, 1),
('PP-BW-FRI-B-24',  'Bi-Weekly Friday, cycle B — 24 of 26',                        'PP-BW-FRI-B-24',  'BIWEEKLY',    NULL,        'FRIDAY',   '2026-09-25', 24, 1,  60, 1, 1),
('PP-WK-THU-48',    'Weekly Thursday — 48 of 52',                                  'PP-WK-THU-48',    'WEEKLY',      NULL,        'THURSDAY', '2026-09-17', 48, 1,  70, 1, 1),
('PP-WK-FRI-48',    'Weekly Friday — 48 of 52',                                    'PP-WK-FRI-48',    'WEEKLY',      NULL,        'FRIDAY',   '2026-09-18', 48, 1,  80, 1, 1),
('PP-MO-LAST',      'Monthly — last day',                                          'PP-MO-LAST',      'MONTHLY',     NULL,        NULL,       NULL,         12, 1,  90, 1, 1),
('PP-BW-THU-A-26',  'Bi-Weekly Thursday, cycle A — every check (not recommended)', 'PP-BW-THU-A-26',  'BIWEEKLY',    NULL,        'THURSDAY', '2026-09-17', 26, 0, 900, 1, 1),
('PP-BW-FRI-A-26',  'Bi-Weekly Friday, cycle A — every check (not recommended)',   'PP-BW-FRI-A-26',  'BIWEEKLY',    NULL,        'FRIDAY',   '2026-09-18', 26, 0, 910, 1, 1),
('PP-WK-THU-52',    'Weekly Thursday — every check (not recommended)',             'PP-WK-THU-52',    'WEEKLY',      NULL,        'THURSDAY', '2026-09-17', 52, 0, 920, 1, 1),
('PP-WK-FRI-52',    'Weekly Friday — every check (not recommended)',               'PP-WK-FRI-52',    'WEEKLY',      NULL,        'FRIDAY',   '2026-09-18', 52, 0, 930, 1, 1),
('PP-MO-1ST',       'Monthly, posts on the 1st — plan-level, not selectable',      'PP-MO-1ST',       'MONTHLY',     NULL,        NULL,       NULL,         12, 0, 990, 0, 1);

-- paycycle_frequency_alias: maps a paycycle_frequency application answer to a recurrence
-- token. No FK to payroll_frequency -- a recurrence/variant pair, not a specific row, since
-- several payroll_frequency rows can share one recurrence.
CREATE TABLE IF NOT EXISTS paycycle_frequency_alias (
    id                    INT           NOT NULL AUTO_INCREMENT,
    alias_text            VARCHAR(100)  NOT NULL,
    alias_key             VARCHAR(100)  NOT NULL,
    recurrence            VARCHAR(20)   NOT NULL,
    semimonthly_variant   VARCHAR(20)   NULL,
    active                TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_paycycle_frequency_alias_key (alias_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='Maps a paycycle_frequency application answer to a recurrence token. alias_key = alias_text uppercased with all non-alphanumeric characters stripped. No reader yet -- the matrix dropdown filter and preselection are a separate task. An answer with no matching active alias must result in no filtering at all, never a partial or wrong filter.';

-- S57-P3b: the paycycle_frequency application field's Select Options are now confirmed as
-- exactly Weekly | Bi-Weekly | Semi-Monthly 1st/15th | Semi-Monthly 15th/Last | Monthly |
-- Other. S57-P3 seeded two extra "Semi-Monthy" (missing the "l") rows as a hedge against a
-- possible misspelling in the configured dropdown text; that hedge is now dead data and both
-- rows are removed, leaving exactly the five rows below.
--
-- alias_key = alias_text uppercased with all non-alphanumeric characters stripped, recomputed
-- and re-verified against this rule for all five remaining rows -- each value below is correct
-- per the rule as written; none needed correction.
--
-- "Other" intentionally gets no alias row here. An application answer of "Other" -- or any
-- answer with no matching active alias -- must result in no filtering at all when the later
-- matrix dropdown filter is built, never a partial or wrong filter (this table's own comment
-- above states the same rule).
INSERT IGNORE INTO paycycle_frequency_alias (alias_text, alias_key, recurrence, semimonthly_variant) VALUES
('Weekly',                  'WEEKLY',              'WEEKLY',      NULL),
('Bi-Weekly',               'BIWEEKLY',            'BIWEEKLY',    NULL),
('Semi-Monthly 1st/15th',   'SEMIMONTHLY1ST15TH',  'SEMIMONTHLY', 'DAY_01_15'),
('Semi-Monthly 15th/Last',  'SEMIMONTHLY15THLAST', 'SEMIMONTHLY', 'DAY_15_EOM'),
('Monthly',                 'MONTHLY',             'MONTHLY',     NULL);

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V110' AS version, '2026-09-13' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V110', 'payroll_frequency seed (14 PP- rows, closes TA-10) + filter metadata columns; new paycycle_frequency_alias table', 'V110__payroll_frequency_seed_and_filter_metadata.sql', NOW());
