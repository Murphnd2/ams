-- =============================================================================
-- schema_version_migration.sql
-- Creates the schema_version table and registers all known migrations.
-- Run this on any database that needs the version history populated
-- (e.g., dev_ssa after DatabaseInitializer, or a manually created schema).
-- The baseline dump already includes this table with data; this script
-- is for cases where the table is empty or missing.
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
('V020', 'ServiceItem unification - schema additions and data backfill', 'V020__service_item_unification.sql'),
('V021', 'ServiceItem linkage - LOS/Enhancement backfill, Payment Services rename, suppress duplicates', 'V021__service_item_linkage_backfill.sql'),
('V022', 'Orphaned ticket ServiceItem backfill', 'V022__orphaned_ticket_serviceitem_backfill.sql'),
('V023', 'Drop ticketsubcategory table and FK', 'V023__drop_ticketsubcategory.sql'),
('V024', 'Fix views referencing dropped ticket_category column', 'V024__fix_views_drop_ticket_category.sql'),
('V025', 'Add level, los, employer_name to plantype for Summit import', 'V025__plantype_import_columns.sql'),
('V026', 'Benefit table: surrogate auto-increment PK with source tracking', 'V026__benefit_surrogate_pk.sql'),
('V027', 'BPO Registration: task source refactor from Person to BpoRegistration', 'V027__bpo_registration_task_source.sql'),
('V028', 'Benefit plan year start/end columns for renewal date correction', 'V028__benefit_plan_year_columns.sql'),
('V029', 'Add is_active column to user table', 'V029__user_is_active.sql'),
('V030', 'BPO cross-system foundation: psp_clients, delegated_todo, API columns, todo_note GUID', 'V030__bpo_cross_system_foundation.sql'),
('V031', 'ToDoNote cross-system: nullable todo_id/created_by_id, author_name column', 'V031__todo_note_cross_system_nullable.sql'),
('V032', 'Approved vendors registry table', 'V032__approved_vendors_registry.sql'),
('V033', 'ToDoNote attachments: todo_note_id FK on weblink', 'V033__todo_note_attachments.sql'),
('V034', 'Add template_key to applicationsection for starter packages', 'V034__starter_package_template_key.sql'),
('V035', 'Feature headline column and description widening', 'V035__feature_headline_description.sql'),
('V036', 'Proposal section table for composable proposal content', 'V036__proposal_section_table.sql'),
('V037', 'Add LOS/Enhancement scoping to proposal_section', 'V037__proposal_section_scoping.sql'),
('V038', 'Add sort_order to delegated_todo for BPO ordering', 'V038__delegated_todo_sort_order.sql'),
('V039', 'Questionnaire system: templates, fields, instances, values, scoping', 'V039__questionnaire_system.sql'),
('V040', 'Add recurring_series_id to delegated_todo for BPO recurring history', 'V040__delegated_todo_recurring_series.sql');
