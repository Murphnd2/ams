-- =============================================================================
-- schema_version_migration.sql
-- =============================================================================
--
-- The V017 baseline dump (beta_ssa_dev_baseline_thru_V017.sql) includes the
-- schema_version table structure but no data rows.
--
-- Run this after importing the baseline to register all applied versions.
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
('V016', 'BPO registration and PSP assignment tables', 'V016__bpo_registration_tables.sql'),
('V017', 'Move SYS_HEALTH constants to ssa.properties, add EMAIL_FOOTER_TEXT', 'V017__health_constants_to_properties.sql'),
('V018', 'Application section suppressed column', 'V018__application_section_suppressed.sql'),
('V019', 'Application field suppressed column', 'V019__application_field_suppressed.sql'),
('V020', 'ServiceItem unification - schema additions and data backfill', 'V020__service_item_unification.sql');
('V021', 'ServiceItem linkage - LOS/Enhancement backfill, Payment Services rename, suppress duplicates', 'V021__service_item_linkage_backfill.sql'),