-- =============================================================================
-- Schema Version Table
-- Required for automated update script (D-09)
-- Date: February 23, 2026
-- Updated: February 25, 2026 — corrected V004-V008 descriptions and script names
--
-- Run on: local dev, production, and any existing PSP databases
-- (Already applied to master VPS image as of SSA-Master-Base-v3-2026-02-23)
--
-- NOTE: For production, use the validated combined upgrade script
-- (docs/importscript/production_upgrade_V001_to_V013.sql) instead of this file.
-- This file is for reference and for seeding schema_version on fresh databases
-- that were created from the V013 baseline dump.
-- =============================================================================

CREATE TABLE IF NOT EXISTS schema_version (
    version VARCHAR(10) NOT NULL,
    description VARCHAR(200),
    script_name VARCHAR(200),
    applied_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (version)
);

-- =============================================================================
-- Record all migrations (per migration_tracker.md)
-- Only insert if not already present
-- =============================================================================

INSERT IGNORE INTO schema_version (version, description, script_name) VALUES
('V001', 'Sales pipeline - tables, columns, entity renames', 'sales_pipeline_migration.sql'),
('V002', 'Sales pipeline 2 - proposal source_activity_id', 'sales_pipeline_migration_2.sql'),
('V003', 'Sales pipeline 3 - LOS expansion, app sections, IRS limits', 'sales_pipeline_migration_3.sql'),
('V004', 'Service manager - enhancement, join tables, SM FKs, LOS columns', 'service_manager_production_migration.sql'),
('V005', 'Rate manager - ratetable sort_order', 'rate_manager_session2_production_migration.sql'),
('V006', 'Invitation system - invitation table, agency manager_id', 'invitation_system_migration.sql'),
('V007', 'Resource library - category, material FK, feature FK', 'resource_library_production_migration.sql'),
('V008', 'Opportunity system - assignee columns, sales tasks', 'opportunity_migration_production.sql'),
('V009', 'Timeclock correction - request table', 'timeclock_correction_migration.sql'),
('V010', 'PSP opportunity integration - sales role and managed_by', 'V010__psp_opportunity_integration.sql'),
('V011', 'BPO delegation feature - todo BPO columns and todo_note table', 'V011__bpo_delegation_feature.sql'),
('V012', 'Role cleanup and PSP branding constants', 'V012__role_cleanup_psp_branding_constants.sql'),
('V013', 'User filter presets - 3 configurable slots per user', 'V013__user_filter_presets.sql');
