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
('V040', 'Add recurring_series_id to delegated_todo for BPO recurring history', 'V040__delegated_todo_recurring_series.sql'),
('V041', 'Add reviewer tracking fields to application table', 'V041__application_reviewer_fields.sql'),
('V042', 'BPO pending approval workflow - status PENDING support', 'V042__delegated_todo_pending_status.sql'),
('V043', 'Add text_value column to constant for custom landing page HTML', 'V043__custom_landing_page.sql'),
('V044', 'Add agency scoping to proposal_section for TITLE/CLOSING overrides', 'V044__proposal_section_agency_scoping.sql'),
('V045', 'Application selected LOS and Enhancement IDs', 'V045__application_selected_services.sql'),
('V046', 'Chatbot skill table for extensible AI assistant capabilities', 'V046__chatbot_skill_table.sql'),
('V047', 'Composite task order table for cross-sequence ordering', 'V047__composite_task_order.sql'),
('V048', 'Universal import system tables and seed data', 'V048__universal_import_system.sql'),
('V049', 'Add source_task_id to delegated_todo for required-sequence auto-approval', 'V049__delegated_todo_source_task_id.sql'),
('V050', 'BPO default assignee per PSP client', 'V050__bpo_default_assignee.sql'),
('V051', 'Import ID mapping cross-reference table', 'V051__import_id_mapping.sql'),
('V052', 'Import run log cross-reference tracking columns', 'V052__import_run_log_xref_tracking.sql'),
('V053', 'Interactive import enhancements: update mode, mapping status, FK flags', 'V053__interactive_import_enhancements.sql'),
('V054', 'Super User Dashboard — managed_installation table', 'V054__managed_installation.sql'),
('V055', 'Schema info view for structural version identification', 'V055__schema_info_view.sql'),
('V056', 'Training video and single-use token tables', 'V056__training_video_tokens.sql'),
('V057', 'Add suppressed flag to agency table', 'V057__agency_suppressed.sql'),
('V058', 'Add renderer column to questionnaire', 'V058__questionnaire_renderer.sql'),
('V059', 'NDT census-based testing tables', 'V059__ndt_census_tables.sql'),
('V060', 'Outlook add-in user link + weblink.note_id', 'V060__outlook_user_link.sql'),
('V061', 'ToDo-level ownership override for agent delegation', 'V061__todo_ownership_override.sql'),
('V062', 'Per-note agent visibility override', 'V062__note_agent_visibility.sql'),
('V063', 'Knowledge Base tables (knowledge_base, knowledge_chunk, knowledge_chunk_history) + 5 KB registry rows', 'V063__knowledge_base_tables.sql'),
('V064', 'Platform JSON registry seed (proposal_page_builder, automation_email_builder)', 'V064__platform_json_registry_seed.sql'),
('V065', 'Email Draft Assistant skill seed: unique index on chatbot_skill(psp_id,skill_name) + EMAIL_DRAFT_ASSISTANT row', 'V065__email_draft_assistant_skill.sql'),
('V066', 'Per-proposal, per-line agent markup on pricing (proposal_price_adjustment)', 'V066__proposal_price_adjustment.sql'),
('V067', 'Per-agency enable flag for proposal markup (agency.markup_enabled, default OFF)', 'V067__agency_markup_enabled.sql'),
('V068', 'Host-header custom agency landing pages (agency.landing_host unique + landing_html)', 'V068__agency_landing_host.sql'),
('V069', 'Per-agency white-label email sending (agency.email_domain unique + email_verified)', 'V069__agency_email_sending.sql');
