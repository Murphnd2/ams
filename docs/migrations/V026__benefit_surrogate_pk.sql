-- ============================================================================
-- V026: Benefit table — surrogate auto-increment PK with source tracking
-- ============================================================================
--
-- Problem:  benefit.benefit_id was set directly from Summit's EmployerPlan_ID.
--           J4 (CDH) uses positive IDs; J7 (COBRA/PB) uses negated IDs to
--           avoid collision. This is fragile and prevents AUTO_INCREMENT.
--
-- Solution: Add summit_id + source_type columns for external key tracking.
--           Renumber negative PKs to positive. Switch to AUTO_INCREMENT.
--           Lookup by (source_type, summit_id) instead of PK for imports.
--
-- Handles both fresh databases (empty) and production (with negated IDs).
-- ============================================================================

-- Step 1: Add new source-tracking columns
ALTER TABLE benefit
    ADD COLUMN summit_id INT NULL AFTER benefit_id,
    ADD COLUMN source_type VARCHAR(10) NOT NULL DEFAULT 'CDH' AFTER summit_id;

-- Step 2: Backfill from existing data
--   Positive IDs = CDH (J4), summit_id matches benefit_id
--   Negative IDs = COBRA (J7), summit_id = ABS(benefit_id)
UPDATE benefit SET summit_id = benefit_id, source_type = 'CDH' WHERE benefit_id > 0;
UPDATE benefit SET summit_id = ABS(benefit_id), source_type = 'COBRA' WHERE benefit_id < 0;
UPDATE benefit SET summit_id = 0 WHERE summit_id IS NULL;  -- safety for id=0

-- Step 3: Make summit_id NOT NULL now that it's populated
ALTER TABLE benefit MODIFY COLUMN summit_id INT NOT NULL;

-- Step 4: Renumber negative benefit_ids to positive values
--   AUTO_INCREMENT requires all-positive PKs. Assign new IDs starting from MAX+1.
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS _benefit_id_remap;
CREATE TABLE _benefit_id_remap (
    old_id INT NOT NULL PRIMARY KEY,
    new_id INT NOT NULL
);

SET @next = (SELECT COALESCE(MAX(benefit_id), 0) FROM benefit WHERE benefit_id > 0);

INSERT INTO _benefit_id_remap (old_id, new_id)
SELECT benefit_id, @next := @next + 1
FROM benefit WHERE benefit_id < 0 ORDER BY benefit_id;

-- Update FK tables before changing benefit PK
UPDATE benefittier bt
    INNER JOIN _benefit_id_remap r ON bt.benefit_id = r.old_id
    SET bt.benefit_id = r.new_id;

UPDATE coveragestatus cs
    INNER JOIN _benefit_id_remap r ON cs.benefit_id = r.old_id
    SET cs.benefit_id = r.new_id;

UPDATE renewalitem ri
    INNER JOIN _benefit_id_remap r ON ri.benefit_id = r.old_id
    SET ri.benefit_id = r.new_id;

-- Update benefit table itself
UPDATE benefit b
    INNER JOIN _benefit_id_remap r ON b.benefit_id = r.old_id
    SET b.benefit_id = r.new_id;

DROP TABLE _benefit_id_remap;
SET FOREIGN_KEY_CHECKS = 1;

-- Step 5: Drop FK constraints, convert to AUTO_INCREMENT, re-add FKs
ALTER TABLE benefittier DROP FOREIGN KEY FK_BENEFITTIER_benefit_id;
ALTER TABLE coveragestatus DROP FOREIGN KEY FK_COVERAGESTATUS_benefit_id;
ALTER TABLE renewalitem DROP FOREIGN KEY FK_RENEWALITEM_benefit_id;

ALTER TABLE benefit MODIFY COLUMN benefit_id INT NOT NULL AUTO_INCREMENT;

ALTER TABLE benefittier ADD CONSTRAINT FK_BENEFITTIER_benefit_id
    FOREIGN KEY (benefit_id) REFERENCES benefit(benefit_id);
ALTER TABLE coveragestatus ADD CONSTRAINT FK_COVERAGESTATUS_benefit_id
    FOREIGN KEY (benefit_id) REFERENCES benefit(benefit_id);
ALTER TABLE renewalitem ADD CONSTRAINT FK_RENEWALITEM_benefit_id
    FOREIGN KEY (benefit_id) REFERENCES benefit(benefit_id);

-- Step 6: Add unique constraint for source-discriminated lookups
ALTER TABLE benefit ADD UNIQUE INDEX uq_benefit_source_summit (source_type, summit_id);

-- Step 7: Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V026', 'Benefit table: surrogate auto-increment PK with source tracking', 'V026__benefit_surrogate_pk.sql', NOW());
