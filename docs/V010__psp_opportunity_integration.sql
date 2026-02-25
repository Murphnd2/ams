-- =============================================================================
-- V010: PSP Opportunity Integration
-- Date: February 22, 2026
-- Prerequisites: V004 (V008__opportunity_system.sql)
--
-- Changes:
--   1. Add PSP Sales role (UserRole ID 9) — gates opportunity visibility
--   2. Add managed_by_id column to assignee — allows PSP to manage agent opps
--      without changing assigned_to ownership
--
-- Run BEFORE deploying opportunity-in-activity-list code.
-- =============================================================================

-- ─── 1. PSP Sales Role ──────────────────────────────────────────────────────

INSERT IGNORE INTO userrole (role_id, description) VALUES (9, 'PSP Sales');

-- ─── 2. Managed-by FK on Assignee ───────────────────────────────────────────

ALTER TABLE assignee ADD COLUMN managed_by_id BIGINT NULL;

ALTER TABLE assignee ADD CONSTRAINT fk_opp_managed_by
  FOREIGN KEY (managed_by_id) REFERENCES assignee(id);

-- ─── 3. Record this migration ───────────────────────────────────────────────

INSERT IGNORE INTO schema_version (version, description, script_name) VALUES
('V010', 'PSP opportunity integration - sales role and managed_by', 'V010__psp_opportunity_integration.sql');
