-- =============================================================================
-- Schema Version Table
-- Required for automated update script (D-09)
-- Date: February 23, 2026
-- Updated: February 26, 2026 — V016 added, V011 description updated
--
-- Run on: local dev, production, and any existing PSP databases
-- (Already applied to master VPS image as of SSA-Master-Base-v3-2026-02-23)
--
-- NOTE: For production, use the validated combined upgrade script
-- (docs/importscript/production_upgrade_V001_to_V016.sql) instead of this file.
-- This file is for reference and for seeding schema_version on fresh databases
-- that were created from the V016 baseline dump.
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
('V001', 'Sales pipeline - tables, columns, entity renames', 'V001__sales_pipeline.sql'),
('V002', 'Sales pipeline 2 - proposal source_activity_id', 'V002__sales_pipeline_2.sql'),
('V003', 'Sales pipeline 3 - LOS expansion, app sections, IRS limits', 'V003__sales_pipeline_3.sql'),
('V004', 'Service manager - enhancement, join tables, SM FKs, LOS columns', 'V004__service_manager.sql'),
('V005', 'Rate manager - ratetable sort_order', 'V005__rate_manager.sql'),
('V006', 'Invitation system - invitation table, agency manager_id', 'V006__invitation_system.sql'),
('V007', 'Resource library - category, material FK, feature FK', 'V007__resource_library.sql'),
('V008', 'Opportunity system - assignee columns, sales tasks', 'V008__opportunity_system.sql'),
('V009', 'Timeclock correction - request table', 'V009__timeclock_correction.sql'),
('V010', 'PSP opportunity integration - sales role and managed_by', 'V010__psp_opportunity_integration.sql'),
('V011', 'BPO delegation - todo BPO columns, todo_guid, is_reverted, task_guid, todo_note table', 'V011__bpo_delegation_feature.sql'),
('V012', 'Role cleanup and PSP branding constants', 'V012__role_cleanup_psp_branding_constants.sql'),
('V013', 'User filter presets - 3 configurable slots per user', 'V013__user_filter_presets.sql'),
('V014', 'Chatbot deployment - note.is_resolution, API key, ticket categories', 'V014__chatbot_deployment.sql'),
('V015', 'Move S3 and API key constants to ssa.properties, delete dead SAVE_PATH', 'V015__constants_to_properties.sql'),
('V016', 'BPO registration and PSP assignment tables', 'V016__bpo_registration_tables.sql');
