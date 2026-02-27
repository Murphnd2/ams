-- =====================================================================
-- V020 — ServiceItem Unification: Schema Additions & Data Backfill
-- =====================================================================
-- Prerequisite: V019
--
-- This migration evolves the templatepurpose table into the ServiceItem
-- concept by adding columns for PSP ownership, suppression, provider
-- tracking, renewal frequency, and ticket category grouping. It also
-- adds direct ServiceItem FK columns to los and enhancement tables,
-- adds configurable renewal_months to the benefit table, and backfills
-- existing data.
--
-- Phase 1: Schema additions (all additive, nullable or defaulted)
-- Phase 2: Data backfill
-- Phase 3: Ticket FK migration on assignee table
--
-- NOTE: TicketSubCategory table is NOT dropped in this migration.
--       It will be dropped in a future migration after all Java code
--       references have been updated.
-- =====================================================================

-- ─── PHASE 1: Schema Additions ──────────────────────────────────────

-- 1a. New columns on templatepurpose (ServiceItem)
ALTER TABLE templatepurpose
  ADD COLUMN psp_id BIGINT NULL,
  ADD COLUMN is_suppressed TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN provider_ref VARCHAR(100) NULL,
  ADD COLUMN code VARCHAR(20) NULL,
  ADD COLUMN source_type VARCHAR(20) NULL,
  ADD COLUMN default_renewal_months INT NULL,
  ADD COLUMN has_required_tasks TINYINT(1) NOT NULL DEFAULT 1,
  ADD COLUMN category_id BIGINT NULL;

ALTER TABLE templatepurpose
  ADD CONSTRAINT FK_TEMPLATEPURPOSE_psp_id
  FOREIGN KEY (psp_id) REFERENCES assignee(id);

ALTER TABLE templatepurpose
  ADD CONSTRAINT FK_TEMPLATEPURPOSE_category_id
  FOREIGN KEY (category_id) REFERENCES ticketcategory(category_id);

-- 1b. Direct ServiceItem FK on los
ALTER TABLE los
  ADD COLUMN service_item_id INT NULL;

ALTER TABLE los
  ADD CONSTRAINT FK_LOS_service_item_id
  FOREIGN KEY (service_item_id) REFERENCES templatepurpose(purpose_id);

-- 1c. Direct ServiceItem FK on enhancement
ALTER TABLE enhancement
  ADD COLUMN service_item_id INT NULL;

ALTER TABLE enhancement
  ADD CONSTRAINT FK_ENHANCEMENT_service_item_id
  FOREIGN KEY (service_item_id) REFERENCES templatepurpose(purpose_id);

-- 1d. Configurable renewal frequency on benefit
ALTER TABLE benefit
  ADD COLUMN renewal_months INT NOT NULL DEFAULT 12;


-- ─── PHASE 2: Data Backfill ─────────────────────────────────────────

-- 2a. PSP ownership — all existing TemplatePurposes belong to SSA (id=4)
UPDATE templatepurpose SET psp_id = 4 WHERE psp_id IS NULL;

-- 2b. Source type by group
UPDATE templatepurpose SET source_type = 'DATAPATH' WHERE group_id = 1;
UPDATE templatepurpose SET source_type = 'MANUAL'   WHERE group_id IN (2, 3, 4, 5);

-- 2c. Default renewal months for renewal group
UPDATE templatepurpose SET default_renewal_months = 12 WHERE group_id = 1;

-- 2d. Ticket items: migrate category_id and suppression from ticketsubcategory
UPDATE templatepurpose tp
  INNER JOIN ticketsubcategory tsc ON tsc.temp_purpose_id = tp.purpose_id
  SET tp.category_id = tsc.category_id,
      tp.is_suppressed = CASE WHEN tsc.is_active = 1 THEN 0 ELSE 1 END
  WHERE tp.group_id = 3;

-- 2e. Ticket items: flag those without active RequiredTaskLists
UPDATE templatepurpose tp
  SET tp.has_required_tasks = 0
  WHERE tp.group_id = 3
    AND NOT EXISTS (
      SELECT 1 FROM tasksequence ts
      WHERE ts.purpose_id = tp.purpose_id
        AND ts.DTYPE = 'RequiredTaskList'
        AND ts.is_inactive = 0
    );

-- 2f. All existing benefits are annual
--     (Column was added with DEFAULT 12, so existing rows already have 12.
--      This UPDATE is a safety net.)
UPDATE benefit SET renewal_months = 12 WHERE renewal_months IS NULL;


-- ─── PHASE 3: Ticket FK Migration ──────────────────────────────────
--
-- The assignee table has ticket_category → ticketsubcategory(subcategory_id)
-- We need to add a new FK pointing to templatepurpose(purpose_id) for
-- the ServiceItem reference, then populate it from the existing
-- ticketsubcategory link.
-- ─────────────────────────────────────────────────────────────────────

-- 3a. Add new column for ServiceItem reference on tickets
ALTER TABLE assignee
  ADD COLUMN ticket_service_item_id INT NULL;

ALTER TABLE assignee
  ADD CONSTRAINT FK_ASSIGNEE_ticket_service_item_id
  FOREIGN KEY (ticket_service_item_id) REFERENCES templatepurpose(purpose_id);

-- 3b. Populate from existing ticketsubcategory link
UPDATE assignee a
  INNER JOIN ticketsubcategory tsc ON a.ticket_category = tsc.subcategory_id
  SET a.ticket_service_item_id = tsc.temp_purpose_id
  WHERE a.DTYPE = 'Ticket'
    AND a.ticket_category IS NOT NULL;


-- ─── Self-Registration ──────────────────────────────────────────────

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V020', 'ServiceItem unification - schema additions and data backfill',
        'V020__service_item_unification.sql', NOW());
