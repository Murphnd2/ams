-- V109: coverage_tier reference table. S57-P1.
--
-- SSA has adopted DataPath's "Tier Structure 3" as the standard tier set for all HRA-type
-- benefit plans. Summit stamps these four rows into the benefit plan's Coverage Levels/Tiers
-- grid from a dropdown, so the Summit-side Tier ID strings are never typed by hand there.
-- Today the AMS enrollment matrix renders the Summit Tier ID as a free-text input -- an
-- operator-typed string that lands verbatim in column E of the ZZ_TEST_HRA_ENROLL import
-- file with nothing validating it against Summit, and under Contribution Schedule funding
-- the tier determines the dollar amount, so a wrong tier is wrong money with no error. This
-- table is the AMS-side mirror of Summit's four-row tier set (TA-14).
--
-- NO CODE READS THIS TABLE YET. The enrollment matrix tier picker that will replace the
-- free-text input is a separate, later task. This migration is schema, seed data, and the
-- entity/DAO layer only.
--
-- Columns:
--   code             -- AMS identifier. Unique. Never emitted to Summit.
--   label            -- operator-facing text shown in the future AMS dropdown.
--   summit_tier_id   -- the exact string emitted into column E of the HRA Enrollment file.
--                       Must match the Tier ID configured on the Summit benefit plan
--                       character for character. Nothing validates the match.
--   census_code      -- nullable. The value the census upload's coverage_tier column carries
--                       for this tier.
--   sort_order       -- display order in the future picker.
--   active           -- soft-disable flag, defaulting to enabled.
--
-- Idempotency guard: CREATE TABLE IF NOT EXISTS + INSERT IGNORE for the four seed rows,
-- following V107's precedent.
--
-- NO INSERT INTO constant anywhere in this migration.
--
-- Reversal: DROP TABLE coverage_tier. Nothing outside this migration references it yet -- no
-- view, no other migration, no export writer, no existing servlet or JSP -- so dropping it
-- changes no running behaviour.

CREATE TABLE IF NOT EXISTS coverage_tier (
    id                INT           NOT NULL AUTO_INCREMENT,
    code              VARCHAR(32)   NOT NULL,
    label             VARCHAR(64)   NOT NULL,
    summit_tier_id    VARCHAR(64)   NOT NULL COMMENT 'Must match the Tier ID configured on the Summit benefit plan exactly, character for character. Nothing validates the match.',
    census_code       VARCHAR(32)   NULL,
    sort_order        INT           NOT NULL DEFAULT 0,
    active            TINYINT(1)    NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_coverage_tier_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='DataPath Tier Structure 3 reference set (TA-14). summit_tier_id must match the Tier ID configured on the Summit benefit plan exactly; nothing validates the match. No code reads this table yet -- the enrollment matrix tier picker is a separate task.';

INSERT IGNORE INTO coverage_tier (code, label, summit_tier_id, census_code, sort_order, active) VALUES
('EE_ONLY',     'Employee Only',         'EE/Only', 'EE',    10, 1),
('EE_SPOUSE',   'Employee + Spouse',     'EE/SP',   'EE+SP', 20, 1),
('EE_CHILDREN', 'Employee + Children',   'EE/CN',   'EE+CH', 30, 1),
('EE_FAMILY',   'Employee + Family',     'EE/FAM',  'FAM',   40, 1);

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V109' AS version, '2026-09-13' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V109', 'coverage_tier reference table (DataPath Tier Structure 3)', 'V109__coverage_tier_reference.sql', NOW());
