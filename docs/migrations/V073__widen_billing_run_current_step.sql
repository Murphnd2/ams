-- =====================================================================
-- V073__widen_billing_run_current_step.sql
-- =====================================================================
-- Purpose : billing_run.current_step was VARCHAR(40), too short for the
--           Monthly Billing Launcher worker's CREATE_BILLING sub-step labels
--           (e.g. "CREATE_BILLING: Fill billing coverage table (7/14)"),
--           causing MySQL error 1406 (Data too long) that failed a billing run
--           mid-CREATE_BILLING. Widen to VARCHAR(255).
--
-- Feature : Monthly Billing Launcher (SSA-internal). Fixes a V072 sizing bug.
--
-- Prereqs : V072 (creates billing_run).
--
-- Scope   : SSA production instance only.
--
-- Rollback: ALTER TABLE billing_run MODIFY current_step VARCHAR(40) NULL;
-- =====================================================================

ALTER TABLE billing_run MODIFY current_step VARCHAR(255) NULL;

-- Refresh schema_info (match V072's placement/format)
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V073' AS version, '2026-07-17' AS updated;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V073', 'Widen billing_run.current_step to VARCHAR(255)', 'V073__widen_billing_run_current_step.sql', NOW());
