-- V104: summit_plan_template_map.is_card_issuer -- marks the card-issuer placeholder plan for the
-- $1 seed election export (type=cardseed)
--
-- Several PremiumPath mapping rows can share one service_item_id (W3/W4, V103's fan-out), and
-- nothing already on the row carries "this is the card-issuer placeholder plan" as a meaning:
-- template_id is per tenant (build rule 4), effective_date_rule is a date rule not a marker, and
-- label/key_segment are both admin-editable free text a rename would silently break. This column
-- is that marker -- the smallest new flag, on the row Kevin already administers
-- (SummitPlanTemplateAdmin, W5), following V095's own precedent of using a named column rather
-- than inventing a second table or a config property (spec_card_issuer_seed_election.md §3).
--
-- ⚠️ SCHEMA ONLY. No emitter reads this column until the type=cardseed writer ships in this same
-- commit; the admin screen (SummitPlanTemplateAdmin/summitPlanTemplateAdmin25.jsp, also this
-- commit) is the only writer. Every existing row defaults to 0 (not the card issuer), so an
-- installation that takes this migration without checking the box on any row behaves exactly as
-- before -- no export type is affected by the column's mere existence.
--
-- No unique index. "At most one flagged row per PSP" is wrong (a PSP can run several sales, each
-- with its own PremiumPath fan-out and its own card-issuer row); "at most one per service item"
-- would need is_active semantics V095 deliberately rejected for this table's other uniqueness
-- rules. The real constraint -- exactly one flagged row among a SALE's ELECTED service items --
-- can only be evaluated at export time, against one proposal's elections, so the type=cardseed
-- writer enforces it (refusing on zero or more than one match), not the database.
--
-- NO ROWS ARE INSERTED (rule 5). Kevin ticks the box on the PremiumPath Card Issuer row himself
-- once this migration is applied.
--
-- Idempotency guard follows V101/V103's information_schema + PREPARE/EXECUTE pattern.
--
-- Reversal: ALTER TABLE summit_plan_template_map DROP COLUMN is_card_issuer;
-- Nothing else references it -- no view, no other migration, no inbound FK -- and the
-- type=cardseed writer's "no Summit Plan Templates configured" refusal (spec §3, legacy path) is
-- exactly what every PSP sees once the column and its only reader are both gone.

SET @db = DATABASE();

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_plan_template_map' AND COLUMN_NAME = 'is_card_issuer');
SET @sql = IF(@col = 0,
    'ALTER TABLE summit_plan_template_map ADD COLUMN is_card_issuer TINYINT(1) NOT NULL DEFAULT 0 COMMENT ''Marks the one mapping row per sale that is the card-issuer placeholder plan the $1 seed election (type=cardseed) enrols into. Exactly one active flagged row among a sale''''s elected service items, enforced by the emitter, not the database.'' AFTER plan_year_offset_years',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V104' AS version, '2026-09-12' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V104', 'summit_plan_template_map.is_card_issuer: marks the card-issuer placeholder plan for the $1 seed election export (type=cardseed)', 'V104__plan_template_map_card_issuer.sql', NOW());
