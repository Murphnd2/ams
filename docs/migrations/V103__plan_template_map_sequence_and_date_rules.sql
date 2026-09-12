-- V103: summit_plan_template_map -- sequence discriminator and date-rule columns (W3)
--
-- Lets one elected ServiceItem map to SEVERAL Summit plans, each to be created on its own
-- computed effective date and plan year. PremiumPath needs five plans from one service item;
-- every existing product keeps exactly one. V095 anticipated this: its comment names the unique
-- key uq_summit_plan_template_map_psp_service as "where the 1:1 rule lives" and says the
-- reversal is against live rows rather than a table rebuild. This migration is that reversal,
-- plus the columns W4 (the file 2 fan-out emitter) will evaluate.
--
-- ⚠️ SCHEMA ONLY. No emitter reads these columns yet (W4), no admin screen writes them (W5), and
-- no date arithmetic exists anywhere (W4). SummitPlanTemplateResolver still takes the first
-- active row per service item; with every existing row backfilled to seq 0 and the defaults
-- below, every existing installation behaves byte-for-byte as before.
--
-- Columns:
--   seq                     SMALLINT NOT NULL DEFAULT 0
--       Ordinal within one (psp_id, service_item_id). NOT a display order (sort_order is) and
--       NOT an identifier -- it exists only so the unique key can hold several rows per service
--       item. ⚠️ Named `seq`, not `sequence`: SEQUENCE is a reserved word on MariaDB and a keyword
--       in several SQL dialects, and a column that needs backticks in every query is a defect
--       waiting to happen. Same reasoning, no quoting anywhere.
--   effective_date_rule     VARCHAR(32) NOT NULL DEFAULT 'PLAN_YEAR_START'
--       How W4 computes this plan's Effective Date from the sale's plan-year start (D):
--       PLAN_YEAR_START | MOST_RECENT_PAST_MONTHDAY. A string, not an ENUM type and not an FK:
--       two values today, a third is a code change either way, and a string is cheaper to
--       reverse. AMS validates the value; the database does not.
--   offset_months           INT NOT NULL DEFAULT 0
--       Signed month offset applied to D BEFORE the rule is evaluated (e.g. -3 for the generic
--       card-enabled benefit at implementation-minus-three-months).
--   plan_year_offset_years  INT NOT NULL DEFAULT 0
--       Signed year offset for the plan year this row's plan is created in (e.g. -1 for the
--       prior-year §125 leg). 0 = the sale's own plan year.
--
-- Unique key: (psp_id, service_item_id) -> (psp_id, service_item_id, seq). Re-created under a
-- new name, then the old one dropped, so a re-run is idempotent and the old name never lingers.
-- ⚠️ ADD BEFORE DROP, not the other way around: `psp_id` carries an FK to `assignee(id)`
-- (fk_summit_plan_template_map_psp), and the old two-column key is the only index with `psp_id`
-- leftmost. MySQL refuses to drop an index a foreign key still depends on for its lookup, so a
-- drop-then-add ordering here fails the DROP silently mid-script (the guarded statement just
-- doesn't execute) while the later ADD still succeeds -- leaving BOTH unique keys in place, with
-- the old two-column one still enforcing one row per service item and defeating this migration's
-- whole point. Adding the new (3-column) key first gives the FK a second index with `psp_id`
-- leftmost, so the old one is then free to drop. Confirmed on `beta_ssa` 2026-09-12: the
-- drop-then-add ordering left both indexes in `information_schema.STATISTICS`. Any future
-- migration that swaps a unique key on an FK column needs this same add-then-drop order.
-- `is_active` still does not participate: an inactive row still holds its (psp, item, seq) slot,
-- exactly as V095 intended for the 1:1 key.
--
-- Backfill: every existing row gets seq 0 and the column defaults, which the DEFAULT clauses
-- apply on ADD COLUMN. Existing rows are semantically unchanged: seq 0 + PLAN_YEAR_START +
-- 0 + 0 is "one plan, on the plan-year start, in the sale's own plan year" -- what file 2 emits
-- today. The explicit UPDATE below is belt-and-braces for a server whose ADD COLUMN default
-- application differs; it is a no-op where the defaults already landed.
--
-- Idempotency guard follows V097/V102's information_schema + PREPARE/EXECUTE pattern, per column
-- and per index. No rows inserted, no INSERT INTO constant, no DatabaseInitializer change.
--
-- Reversal (same add-before-drop ordering, same FK reason -- the mirror-image swap has the
-- identical failure mode: dropping `..._seq` first would leave `psp_id`'s FK momentarily
-- uncovered by a leftmost index if `..._psp_service` is added after rather than before):
--   ALTER TABLE summit_plan_template_map ADD UNIQUE KEY uq_summit_plan_template_map_psp_service (psp_id, service_item_id);
--   -- fails if any service item holds more than one row; delete the seq > 0 rows first
--   ALTER TABLE summit_plan_template_map DROP INDEX uq_summit_plan_template_map_psp_service_seq;
--   ALTER TABLE summit_plan_template_map DROP COLUMN seq, DROP COLUMN effective_date_rule,
--       DROP COLUMN offset_months, DROP COLUMN plan_year_offset_years;

SET @db = DATABASE();

-- seq
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_plan_template_map' AND COLUMN_NAME = 'seq');
SET @sql = IF(@col = 0,
    'ALTER TABLE summit_plan_template_map ADD COLUMN seq SMALLINT NOT NULL DEFAULT 0 COMMENT ''Ordinal within (psp_id, service_item_id) so one service item can map to several Summit plans. Not a display order (sort_order is), not an identifier -- exists only to make the unique key unique.'' AFTER service_item_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- effective_date_rule
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_plan_template_map' AND COLUMN_NAME = 'effective_date_rule');
SET @sql = IF(@col = 0,
    'ALTER TABLE summit_plan_template_map ADD COLUMN effective_date_rule VARCHAR(32) NOT NULL DEFAULT ''PLAN_YEAR_START'' COMMENT ''PLAN_YEAR_START | MOST_RECENT_PAST_MONTHDAY -- how W4 derives this plan''''s Effective Date from the sale''''s plan-year start. Validated by AMS, not the database.'' AFTER seq',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- offset_months
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_plan_template_map' AND COLUMN_NAME = 'offset_months');
SET @sql = IF(@col = 0,
    'ALTER TABLE summit_plan_template_map ADD COLUMN offset_months INT NOT NULL DEFAULT 0 COMMENT ''Signed month offset applied to the plan-year start before effective_date_rule is evaluated.'' AFTER effective_date_rule',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- plan_year_offset_years
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_plan_template_map' AND COLUMN_NAME = 'plan_year_offset_years');
SET @sql = IF(@col = 0,
    'ALTER TABLE summit_plan_template_map ADD COLUMN plan_year_offset_years INT NOT NULL DEFAULT 0 COMMENT ''Signed year offset for the plan year this plan is created in; 0 = the sale''''s own plan year.'' AFTER offset_months',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Backfill (no-op where the DEFAULT clauses already applied on ADD COLUMN).
UPDATE summit_plan_template_map
   SET seq = 0
 WHERE seq IS NULL;
UPDATE summit_plan_template_map
   SET effective_date_rule = 'PLAN_YEAR_START'
 WHERE effective_date_rule IS NULL OR effective_date_rule = '';
UPDATE summit_plan_template_map
   SET offset_months = 0
 WHERE offset_months IS NULL;
UPDATE summit_plan_template_map
   SET plan_year_offset_years = 0
 WHERE plan_year_offset_years IS NULL;

-- Replace the unique key: (psp_id, service_item_id) -> (psp_id, service_item_id, seq).
-- ⚠️ ADD BEFORE DROP -- psp_id carries an FK to assignee(id), and the old two-column key is the
-- only index with psp_id leftmost. Dropping it first fails (MySQL won't drop an index an FK
-- depends on) while the later ADD still succeeds, leaving both indexes in place and the old
-- 1:1 constraint still enforced. Adding the new key first gives the FK a second covering index,
-- so the old one is then free to drop. See the header comment for the ordering rule.
SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_plan_template_map'
              AND INDEX_NAME = 'uq_summit_plan_template_map_psp_service_seq');
SET @sql = IF(@idx = 0,
    'ALTER TABLE summit_plan_template_map ADD UNIQUE KEY uq_summit_plan_template_map_psp_service_seq (psp_id, service_item_id, seq)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_plan_template_map'
              AND INDEX_NAME = 'uq_summit_plan_template_map_psp_service');
SET @sql = IF(@idx > 0,
    'ALTER TABLE summit_plan_template_map DROP INDEX uq_summit_plan_template_map_psp_service',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V103' AS version, '2026-09-12' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V103', 'summit_plan_template_map: seq discriminator + effective_date_rule/offset_months/plan_year_offset_years; unique key widened to (psp, service item, seq) for plan fan-out (W3)', 'V103__plan_template_map_sequence_and_date_rules.sql', NOW());
