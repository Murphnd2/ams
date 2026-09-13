-- V105: summit_plan_template_map enrollment-matrix columns -- three columns describing how each
-- leg (mapping row) behaves in the enrollment matrix (s52g, amended s52i)
--
-- s52e/s52f explored a separate enrollment_benefit table for this (never applied to any database,
-- reverted s52h). s52g then put seven columns on this table instead, once Kevin settled that each
-- summit_plan_template_map row under a service item is one enrollment leg -- exactly the matrix's
-- tab grain. s52i cut that seven down to three after Kevin found four of the seven wrong:
-- show_monthly_premium/show_annual_election/show_tier were never independent (a leg takes an
-- annual election OR a monthly premium OR a tier, never two -- three booleans made invalid
-- combinations representable), show_opt_out was dropped (every tab carries decline tracking, so
-- it has no false case), and is_importable was dropped (not-importable turned out to be a
-- property of the EMPLOYEE row -- a non-standard payroll cycle -- not of the benefit; it will
-- live on the matrix header row when that is built). V105 was unapplied everywhere when this
-- amendment landed, so this rewrites the unshipped script rather than adding a V106.
--
-- enrollment_amount_mode replaces the three collapsed booleans: VARCHAR(16) NOT NULL DEFAULT
-- 'NONE', members NONE | ANNUAL_ELECTION | MONTHLY_PREMIUM | TIER, code-validated (not a MySQL
-- ENUM), on effective_date_rule's own precedent on this same table. NONE is load-bearing and
-- deliberately the default: not every summit_plan_template_map row is an enrollment leg -- the
-- card-issuer row (is_card_issuer, V104) is one that is not -- so NONE means "this row gets no
-- tab in the enrollment matrix," and a new row is inert until Kevin says otherwise (rule-2
-- default applied to reference data). TIER keys no dollar figure (the HRA setup already carries
-- the amount against the tier; the enrollment file resolves it by tier-name match) -- only
-- ANNUAL_ELECTION and MONTHLY_PREMIUM take a typed amount, which this migration does not add a
-- column for (no consumer yet). A flat ICHRA is still TIER, not a fourth member.
--
-- affects_payroll and tax_treatment are unchanged from s52g's set and from
-- spec_enrollment_matrix_flags.md Step 4: affects_payroll TINYINT(1) NOT NULL DEFAULT 0;
-- tax_treatment VARCHAR(8) NOT NULL DEFAULT 'POST' (PRE|POST, code-validated, not an ENUM, only
-- meaningful when affects_payroll is true -- affects_payroll is the flag that guards the read).
--
-- Checked against every existing column on summit_plan_template_map (id, psp_id,
-- service_item_id, seq, effective_date_rule, offset_months, plan_year_offset_years, template_id,
-- key_segment, label, sort_order, is_active, is_card_issuer, created_at, created_by) -- no
-- collision with any of the three names.
--
-- ⚠️ SCHEMA ONLY. No emitter, matrix UI, or report reads these columns yet -- all later, separate
-- builds. The admin screen (SummitPlanTemplateAdmin/summitPlanTemplateAdmin25.jsp, also this
-- commit) is the only writer. Every existing row defaults to NONE / not-affects-payroll / 'POST',
-- so an installation that takes this migration without touching any row behaves exactly as
-- before -- NONE in particular means the row is inert in the matrix until Kevin sets it.
--
-- NO ROWS ARE INSERTED, NO EXISTING ROW IS UPDATED (rule 5). Kevin sets enrollment_amount_mode on
-- each of PremiumPath's four legs (and any other product's single leg) himself once this
-- migration is applied.
--
-- Idempotency guard follows V104's (and V101/V103's) information_schema + PREPARE/EXECUTE
-- pattern, one column at a time so a partial prior run cannot re-fail on an already-added column.
--
-- Reversal: ALTER TABLE summit_plan_template_map DROP COLUMN enrollment_amount_mode,
-- DROP COLUMN affects_payroll, DROP COLUMN tax_treatment;
-- Nothing else references these columns -- no view, no other migration, no inbound FK, no
-- export writer -- so dropping them changes no running behaviour.

SET @db = DATABASE();

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_plan_template_map' AND COLUMN_NAME = 'enrollment_amount_mode');
SET @sql = IF(@col = 0,
    'ALTER TABLE summit_plan_template_map ADD COLUMN enrollment_amount_mode VARCHAR(16) NOT NULL DEFAULT ''NONE'' COMMENT ''Enrollment matrix: NONE (no tab -- e.g. the card-issuer row) | ANNUAL_ELECTION | MONTHLY_PREMIUM | TIER. Code-validated, not an ENUM.'' AFTER is_card_issuer',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_plan_template_map' AND COLUMN_NAME = 'affects_payroll');
SET @sql = IF(@col = 0,
    'ALTER TABLE summit_plan_template_map ADD COLUMN affects_payroll TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Enrollment matrix / payroll deduction report: this leg affects payroll.'' AFTER enrollment_amount_mode',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_plan_template_map' AND COLUMN_NAME = 'tax_treatment');
SET @sql = IF(@col = 0,
    'ALTER TABLE summit_plan_template_map ADD COLUMN tax_treatment VARCHAR(8) NOT NULL DEFAULT ''POST'' COMMENT ''Payroll deduction report grouping: PRE or POST. Only meaningful when affects_payroll is true. Code-validated string, not an ENUM.'' AFTER affects_payroll',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V105' AS version, '2026-09-12' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V105', 'summit_plan_template_map: enrollment_amount_mode, affects_payroll, tax_treatment', 'V105__plan_template_map_enrollment_fields.sql', NOW());
