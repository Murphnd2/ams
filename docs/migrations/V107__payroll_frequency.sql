-- V107: payroll_frequency reference table. s53c.
--
-- s53b (read-only investigation) established that AMS has nothing to hang a curated
-- payroll-frequency list on. The only existing representation is
-- applicationfield.paycycle_frequency ('Weekly|Bi-Weekly|Semi-Monthly|Monthly'), a
-- pipe-delimited string in one column that PackageLoader.restoreDefaults overwrites on
-- reseed -- it holds no Summit-flagged first-4-of-5 / first-2-of-3 variants and no Summit
-- schedule name. A dead, never-committed PayFrequency class exists only as commented-out
-- code in ReferenceDataSeeder.java:337-356 -- not uncommented, not referenced, not a table.
--
-- docs/analysis/summit_import_spec.md:319-320 and its build rule 11 at :448 already call
-- for "a config-mapped payroll-frequency -> schedule-name registry, never literal strings
-- in code." This table is that registry.
--
-- Columns:
--   code                  -- the value enrollment_matrix_participant.payroll_frequency
--                            stores (V106). Unique. Never OTHER_CUSTOM or
--                            OTHER_NOT_IMPORTABLE -- those are servlet-level sentinels,
--                            enforced at the admin layer, not rows here.
--   label                 -- display text in the matrix dropdown.
--   periods_per_year      -- nullable. Supports the divide-evenly rule (a monthly premium
--                            must divide evenly across the deduction cycles); a row can
--                            exist before Kevin has settled this.
--   summit_schedule_name  -- nullable. The Summit global contribution-schedule name this
--                            frequency emits. Installation-specific, which is exactly why
--                            it lives on a row instead of in code.
--   application_value     -- nullable, plain string, deliberately NOT a foreign key. The
--                            paycycle_frequency answer string this row maps from, for
--                            defaulting the matrix dropdown from the application.
--   enrollment_approved   -- the curated list. Default 0: a new row is invisible to the
--                            matrix until Kevin approves it.
--   active                -- default 1.
--   sort_order            -- default 0.
--
-- Index on (enrollment_approved, active, sort_order): the exact predicate + order the
-- matrix dropdown's read will use.
--
-- NO ROWS ARE INSERTED. The table ships empty -- rows are Kevin's, created through the
-- admin UI (PayrollFrequencyAdmin) when testing requires them. No seed in this script, in
-- DatabaseInitializer, in ReferenceDataSeeder, or anywhere else.
--
-- Idempotency guard: CREATE TABLE IF NOT EXISTS, following V106's precedent for a new
-- table with nothing to guard beyond the table's own existence.
--
-- Reversal: DROP TABLE payroll_frequency. Nothing outside this migration references it yet
-- -- no view, no other migration, no export writer, no existing servlet or JSP -- so
-- dropping it changes no running behaviour.

CREATE TABLE IF NOT EXISTS payroll_frequency (
    id                     INT           NOT NULL AUTO_INCREMENT,
    code                   VARCHAR(32)   NOT NULL,
    label                  VARCHAR(64)   NOT NULL,
    periods_per_year       INT           NULL,
    summit_schedule_name   VARCHAR(128)  NULL,
    application_value      VARCHAR(64)   NULL,
    enrollment_approved    TINYINT(1)    NOT NULL DEFAULT 0,
    active                 TINYINT(1)    NOT NULL DEFAULT 1,
    sort_order             INT           NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_payroll_frequency_code (code),
    INDEX idx_payroll_frequency_approved_active_sort (enrollment_approved, active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V107' AS version, '2026-09-12' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V107', 'payroll_frequency reference table', 'V107__payroll_frequency.sql', NOW());
