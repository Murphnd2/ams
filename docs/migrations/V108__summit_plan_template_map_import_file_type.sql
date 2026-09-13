-- V108: summit_plan_template_map.import_file_type -- which Summit import file a leg's
-- enrollment rows belong in. s56b.
--
-- s56a (read-only investigation) found that no column on this table distinguishes a leg whose
-- enrollment row imports through the HRA Enrollment template (ZZ_TEST_HRA_ENROLL: ICHRA, HRA,
-- MERP) from one that imports through 125 PI Elections (ZZ_TEST_125_ELECTIONS: PremiumPath,
-- FSA, DCA). template_id is the Summit CDH *plan* template file 2 creates, key_segment is the
-- Import Plan ID fragment, is_card_issuer marks one row, and the import-template *names* live
-- in ssa.properties SUMMIT_IMPORT_TEMPLATES keyed by file type -- none of them is per leg.
--
-- Deriving the split from enrollment_amount_mode (V105) was considered and rejected: that
-- column says what input a matrix cell accepts (amount vs tier), not which Summit template the
-- row imports into, and an amount-based HRA or MERP leg would be routed into the wrong file
-- silently. This column records the routing explicitly, per leg, as an operator decision.
--
-- Values are AMS-owned file-type strings of the same kind as SummitExportServlet's TYPE_
-- constants -- 'enrollment' (HRA Enrollment) and 'elections' (125 PI Elections) -- code-validated
-- by the admin screen, not a MySQL ENUM, on enrollment_amount_mode's own precedent on this
-- table. They are not PSP-scoped reference rows, so a fixed option list is correct here.
--
-- NULL is deliberate and load-bearing. There is no default -- not 'enrollment', not '' --
-- because an unassigned leg must be distinguishable from an assigned one: the enrollment
-- export will refuse and name an unassigned leg rather than route it somewhere plausible.
-- Every existing row is therefore NULL after this migration, and stays NULL until Kevin
-- assigns it on the Summit Plan Templates admin screen.
--
-- No index: no reader filters or joins on this column. Every current and planned consumer
-- reads it off a leg it has already loaded by id or by (psp, service item).
--
-- Checked against every existing column on summit_plan_template_map (id, psp_id,
-- service_item_id, seq, effective_date_rule, offset_months, plan_year_offset_years,
-- template_id, key_segment, label, sort_order, is_active, is_card_issuer,
-- enrollment_amount_mode, affects_payroll, tax_treatment, created_at, created_by) -- no
-- collision.
--
-- NO ROWS ARE INSERTED, NO EXISTING ROW IS UPDATED (rule 5). No INSERT INTO constant.
--
-- Idempotency guard follows V104's (and V101/V103/V105's) information_schema +
-- PREPARE/EXECUTE pattern, so a re-run against a table that already has the column is a no-op.
--
-- Reversal: ALTER TABLE summit_plan_template_map DROP COLUMN import_file_type;
-- Nothing else references the column -- no view, no other migration, no inbound FK.

SET @db = DATABASE();

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_plan_template_map' AND COLUMN_NAME = 'import_file_type');
SET @sql = IF(@col = 0,
    'ALTER TABLE summit_plan_template_map ADD COLUMN import_file_type VARCHAR(20) NULL COMMENT ''Which Summit import file this leg''''s enrollment rows belong in: enrollment (HRA Enrollment) | elections (125 PI Elections). NULL = unassigned; the export refuses rather than defaults. Code-validated, not an ENUM.'' AFTER tax_treatment',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V108' AS version, '2026-09-12' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V108', 'summit_plan_template_map.import_file_type enrollment|elections, NULL=unassigned', 'V108__summit_plan_template_map_import_file_type.sql', NOW());
