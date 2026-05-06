-- V064: Platform JSON registry seed (proposal_page_builder, automation_email_builder)
--
-- Seeds two knowledge_base registry rows for the platform-JSON knowledge bases
-- that were missed by V063.
--
-- Why this migration is needed:
--   V063 seeded 5 registry rows for the new email-assistant domains (style_voice,
--   federal_rules, ssa_business, summit_supplemental, summit_official). The two
--   platform-JSON rows (proposal_page_builder, automation_email_builder) were
--   intended to be seeded by DatabaseInitializer.seedKnowledgeBaseRegistry(),
--   which only runs during first-time PSP setup on a fresh install. Existing
--   production databases never trigger that code path, so those two rows are
--   absent, causing ProposalAiBuilder and AutomationAiBuilder to find no KB
--   content at runtime.
--
-- Idempotent via INSERT IGNORE: safe to run on any environment, including fresh
-- installs that already have these rows from DatabaseInitializer. The unique
-- index on kb_key (uq_kb_key) causes INSERT IGNORE to silently skip existing rows.
--
-- Prerequisite: V063 must be applied first (knowledge_base table must exist).
-- Values are identical to those in DatabaseInitializer.seedKnowledgeBaseRegistry()
-- so both seeding paths produce the same data.

INSERT IGNORE INTO knowledge_base
    (kb_key, label, description, source, json_filename, reload_strategy, sort_order, is_active)
VALUES
    ('proposal_page_builder',
     'Proposal Page Builder Knowledge',
     'Knowledge base for the AI Proposal Page Builder feature.',
     'JSON',
     'proposal-page-builder.json',
     'SEARCH',
     60,
     1);

INSERT IGNORE INTO knowledge_base
    (kb_key, label, description, source, json_filename, reload_strategy, sort_order, is_active)
VALUES
    ('automation_email_builder',
     'Automation Email Builder Knowledge',
     'Knowledge base for the AI Automation Email Builder feature.',
     'JSON',
     'automation-email-builder.json',
     'SEARCH',
     70,
     1);

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V064' AS version, '2026-05-04' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V064',
        'Platform JSON registry seed (proposal_page_builder, automation_email_builder)',
        'V064__platform_json_registry_seed.sql',
        NOW());
