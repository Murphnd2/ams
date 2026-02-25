-- =============================================================================
-- Schema Version Table
-- Required for automated update script (D-09)
-- Date: February 23, 2026
--
-- Run on: local dev, production, and any existing PSP databases
-- (Already applied to master VPS image as of SSA-Master-Base-v3-2026-02-23)
-- =============================================================================

CREATE TABLE IF NOT EXISTS schema_version (
    version VARCHAR(10) NOT NULL,
    description VARCHAR(200),
    script_name VARCHAR(200),
    applied_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (version)
);

-- =============================================================================
-- Retroactively record existing migrations (per migration_tracker.md)
-- Only insert if not already present
-- =============================================================================

INSERT IGNORE INTO schema_version (version, description, script_name) VALUES
('V001', 'sales pipeline', 'sales_pipeline_migration.sql'),
('V002', 'sales pipeline 2', 'sales_pipeline_migration_2.sql'),
('V003', 'sales pipeline 3', 'sales_pipeline_migration_3.sql'),
('V004', 'opportunity migration', 'opportunity_migration_production.sql'),
('V005', 'service manager migration', 'service_manager_migration.sql'),
('V006', 'agency manager migration', 'agency_manager_migration.sql'),
('V007', 'invitation migration', 'invitation_migration.sql'),
('V008', 'sales pipeline 4', 'sales_pipeline_migration_4.sql'),
('V009', 'timeclock correction', 'timeclock_correction_migration.sql'),
('V010', 'PSP opportunity integration - sales role and managed_by', 'V010__psp_opportunity_integration.sql'),
('V011', 'BPO delegation feature - todo BPO columns and todo_note table', 'V011__bpo_delegation_feature.sql'),
('V012', 'Role cleanup and PSP branding constants', 'V012__role_cleanup_psp_branding_constants.sql'),
('V013', 'User filter presets - 3 configurable slots per user', 'V013__user_filter_presets.sql');
