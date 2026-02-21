-- ============================================================================
-- Opportunity System — Production Migration Script
-- Date: February 21, 2026
-- Run against: beta_ssa (production)
-- Prerequisites: Sales pipeline migrations 1-3 already applied
-- ============================================================================

-- ============================================================================
-- STEP 1: Schema Changes — Assignee table (Opportunity columns)
-- ============================================================================

ALTER TABLE assignee ADD COLUMN prospect_id BIGINT NULL;
ALTER TABLE assignee ADD COLUMN agency_id_opp BIGINT NULL;
ALTER TABLE assignee ADD COLUMN opportunity_stage VARCHAR(30) NULL;
ALTER TABLE assignee ADD COLUMN estimated_employees INT NULL;
ALTER TABLE assignee ADD COLUMN estimated_value DOUBLE NULL;
ALTER TABLE assignee ADD COLUMN expected_close_date DATE NULL;

-- Foreign keys
ALTER TABLE assignee ADD CONSTRAINT fk_opp_prospect
  FOREIGN KEY (prospect_id) REFERENCES prospect(prospect_id);
ALTER TABLE assignee ADD CONSTRAINT fk_opp_agency
  FOREIGN KEY (agency_id_opp) REFERENCES agency(agency_id);

-- NOTE: Do NOT add DEFAULT 'NEW' to opportunity_stage — it backfills all
-- existing rows and causes EclipseLink type resolution issues.

-- ============================================================================
-- STEP 2: Task Seed Data — Sales template group + opportunity tasks
-- ============================================================================

-- Sales template group (may already exist from dev)
INSERT IGNORE INTO templategroup (id, description) VALUES (5, 'Sales');

-- New Opportunity template purpose
INSERT IGNORE INTO templatepurpose (id, description, sort_order, template_group)
  VALUES (30, 'New Opportunity', 100, 5);

-- Sales tasks (900000+ range to avoid ID collisions)
INSERT INTO task (id, description, allow_early, allow_future) VALUES
  (900001, 'Initial contact with prospect', 1, 1),
  (900002, 'Qualify prospect needs', 1, 1),
  (900003, 'Send proposal', 1, 1),
  (900004, 'Follow up on proposal', 1, 1),
  (900005, 'Close deal', 1, 1);

-- Required task list sequence for opportunities
INSERT INTO tasksequence (id, description, DTYPE, purpose_id)
  VALUES (900001, 'New Opportunity Tasks', 'RequiredTaskList', 30);

-- Link tasks to sequence
INSERT INTO tasksequencetable (sequence_id, task_id, sort_order) VALUES
  (900001, 900001, 100),
  (900001, 900002, 200),
  (900001, 900003, 300),
  (900001, 900004, 400),
  (900001, 900005, 500);

-- ============================================================================
-- VERIFICATION QUERIES (run after migration to confirm)
-- ============================================================================

-- Verify columns added
-- DESCRIBE assignee;

-- Verify task data
-- SELECT * FROM templategroup WHERE id = 5;
-- SELECT * FROM templatepurpose WHERE id = 30;
-- SELECT * FROM task WHERE id BETWEEN 900001 AND 900005;
-- SELECT * FROM tasksequence WHERE id = 900001;
-- SELECT * FROM tasksequencetable WHERE sequence_id = 900001;

-- ============================================================================
-- ROLLBACK (if needed)
-- ============================================================================

-- DELETE FROM tasksequencetable WHERE sequence_id = 900001;
-- DELETE FROM tasksequence WHERE id = 900001;
-- DELETE FROM task WHERE id BETWEEN 900001 AND 900005;
-- DELETE FROM templatepurpose WHERE id = 30;
-- DELETE FROM templategroup WHERE id = 5;
-- ALTER TABLE assignee DROP FOREIGN KEY fk_opp_agency;
-- ALTER TABLE assignee DROP FOREIGN KEY fk_opp_prospect;
-- ALTER TABLE assignee DROP COLUMN prospect_id;
-- ALTER TABLE assignee DROP COLUMN agency_id_opp;
-- ALTER TABLE assignee DROP COLUMN opportunity_stage;
-- ALTER TABLE assignee DROP COLUMN estimated_employees;
-- ALTER TABLE assignee DROP COLUMN estimated_value;
-- ALTER TABLE assignee DROP COLUMN expected_close_date;
