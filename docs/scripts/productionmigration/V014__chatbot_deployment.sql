-- =============================================================================
-- V014: Chatbot Deployment — note.is_resolution, API key, ticket categories
-- Date: February 25, 2026
-- Prerequisites: V013 (or production_upgrade_V001_to_V013.sql)
--
-- Changes:
--   1. Add is_resolution column to note table (chatbot resolution flag)
--   2. Add ANTHROPIC_API_KEY constant (placeholder — fill in real value)
--   3. Deactivate old ticket categories
--   4. Insert new service-oriented ticket categories
--   5. Insert starter subcategories for new categories
--
-- After running:
--   - Fill in actual ANTHROPIC_API_KEY value
--   - Deploy WAR with chatbot code
--   - Verify knowledge base JSON files in src/main/resources/knowledge/
-- =============================================================================

-- ─── 1. Note resolution flag ────────────────────────────────────────────────

SET SQL_SAFE_UPDATES = 0;

-- Used by AI chatbot to identify resolution notes for ticket knowledge base

ALTER TABLE note ADD COLUMN is_resolution TINYINT(1) NOT NULL DEFAULT 0;

-- ─── 2. Anthropic API key constant ──────────────────────────────────────────
-- INSERT IGNORE so existing value (if any) is preserved

INSERT IGNORE INTO constant (name, value, note)
VALUES ('ANTHROPIC_API_KEY', 'FILL_ME_IN', 'Claude API key for AI chatbot assistant');

-- ─── 3. Deactivate old ticket categories ────────────────────────────────────
-- Old categories (IDs vary by installation) are deactivated, not deleted.
-- Existing tickets still reference their subcategories via FK — data is preserved.
-- The active=0 flag hides them from the Create Ticket dropdown.

UPDATE ticketcategory SET active = 0;

-- ─── 4. Insert new service-oriented ticket categories ───────────────────────
-- IDs match ReferenceDataSeeder.loadDefaultTicketCategories() in DatabaseInitializer
-- INSERT IGNORE in case any already exist from dev testing

INSERT IGNORE INTO ticketcategory (category_id, DESCRIPTION, short_text, active) VALUES
(11, 'Claims',            'Claims',  1),
(12, 'Access / Online',   'Access',  1),
(13, 'Debit Card',        'Debit',   1),
(14, 'COBRA',             'COBRA',   1),
(15, 'HSA',               'HSA',     1),
(16, 'Enrollment',        'Enroll',  1),
(17, 'Plan Services',     'Plans',   1),
(18, 'Billing',           'Billing', 1),
(21, 'General',           'General', 1);

-- ─── 5. Insert starter subcategories ────────────────────────────────────────
-- These give immediate categorization options in the Create Ticket dropdown.
-- IDs match ReferenceDataSeeder.loadDefaultTicketCategories().
-- No TemplatePurpose links — sequences can be built later via Sequence Builder.

INSERT IGNORE INTO ticketsubcategory (subcategory_id, DESCRIPTION, category_id, is_active) VALUES
(101, 'Claim not paid',               11, 1),
(102, 'Claim paid incorrectly',       11, 1),
(103, 'Can''t log in to portal',      12, 1),
(104, 'Need online access',           12, 1),
(105, 'Debit card not working',       13, 1),
(106, 'Debit card replacement',       13, 1),
(107, 'COBRA enrollment',             14, 1),
(108, 'COBRA payment issue',          14, 1),
(109, 'HSA contribution question',    15, 1),
(110, 'HSA eligible expense question', 15, 1),
(111, 'New hire enrollment',          16, 1),
(112, 'Open enrollment',              16, 1),
(113, 'Qualifying life event',        16, 1),
(114, 'FSA question',                 17, 1),
(115, 'HRA question',                 17, 1),
(116, 'Plan quote request',           17, 1),
(117, 'Billing discrepancy',          18, 1),
(118, 'Invoice request',              18, 1),
(119, 'General inquiry',              21, 1),
(120, 'Other',                        21, 1);

-- ─── Self-register in schema_version ─────────────────────────────────────────

INSERT IGNORE INTO schema_version (version, description, script_name)
VALUES ('V014', 'Chatbot deployment - note.is_resolution, API key, ticket categories', 'V014__chatbot_deployment.sql');

SET SQL_SAFE_UPDATES = 1;

-- ─── VERIFICATION ────────────────────────────────────────────────────────────
-- DESCRIBE note;  -- should show is_resolution column
-- SELECT * FROM constant WHERE name = 'ANTHROPIC_API_KEY';
-- SELECT * FROM ticketcategory WHERE active = 1 ORDER BY category_id;
-- SELECT * FROM ticketsubcategory WHERE category_id >= 11 ORDER BY subcategory_id;
-- SELECT * FROM schema_version WHERE version = 'V014';

-- ─── ROLLBACK (if needed) ────────────────────────────────────────────────────
-- ALTER TABLE note DROP COLUMN is_resolution;
-- DELETE FROM constant WHERE name = 'ANTHROPIC_API_KEY';
-- DELETE FROM ticketsubcategory WHERE subcategory_id BETWEEN 101 AND 120;
-- DELETE FROM ticketcategory WHERE category_id IN (11,12,13,14,15,16,17,18,21);
-- UPDATE ticketcategory SET active = 1;  -- re-activate old categories
-- DELETE FROM schema_version WHERE version = 'V014';
