-- =====================================================================
-- V022 — Orphaned Ticket ServiceItem Backfill
-- =====================================================================
-- Prerequisite: V020
--
-- V020 backfilled ticket_service_item_id by joining through
-- ticketsubcategory.temp_purpose_id → templatepurpose. However,
-- production has ~730 TSC records where temp_purpose_id IS NULL
-- (custom/ad-hoc ticket reasons created by users). These tickets
-- were left with ticket_service_item_id = NULL.
--
-- This migration:
--   1. Creates catch-all ServiceItems for 6 ticket categories that
--      had no existing ServiceItem (GEN, QUOTE, LAW, WHY, NEED, GET)
--   2. Backfills ticket_service_item_id for ALL orphaned tickets by
--      mapping through TSC → category → default ServiceItem
--
-- ID assignments use the 36-41 range (low seed IDs), avoiding
-- collision with DataPath-imported IDs (87000+ range) and V021
-- ServiceItems (25-35).
-- =====================================================================

-- ─── PHASE 1: Create Missing Category ServiceItems ──────────────────

INSERT INTO templatepurpose (purpose_id, description, group_id, psp_id, source_type, is_suppressed, has_required_tasks, category_id)
VALUES
  (36, 'Uncategorized',        3, 4, 'MANUAL', 0, 0, 21),
  (37, 'Sales Item',           3, 4, 'MANUAL', 0, 0, 17),
  (38, 'Law / Compliance',     3, 4, 'MANUAL', 0, 0, 4),
  (39, 'Why Did This Happen',  3, 4, 'MANUAL', 0, 0, 2),
  (40, 'I Need Something',     3, 4, 'MANUAL', 0, 0, 6),
  (41, 'Did SSA Receive',      3, 4, 'MANUAL', 0, 0, 3);


-- ─── PHASE 2: Backfill Orphaned Tickets ─────────────────────────────
--
-- Map each orphaned ticket through its TSC's category_id to a default
-- ServiceItem. Categories that already had ServiceItems use the
-- lowest existing purpose_id as default.
--
-- Category → Default ServiceItem:
--   HOW     (1)  → 21  (Ticket General)
--   WHY     (2)  → 39  (Why Did This Happen)       [new]
--   GET     (3)  → 41  (Did SSA Receive)            [new]
--   LAW     (4)  → 38  (Law / Compliance)           [new]
--   NEED    (6)  → 40  (I Need Something)           [new]
--   CLAIM   (11) → 101 (File Claim)
--   ACCESS  (12) → 102 (Get Online)
--   ENROLL  (16) → 113 (Term EE)
--   QUOTE   (17) → 37  (Sales Item)                 [new]
--   GEN     (21) → 36  (Uncategorized)              [new]
-- ─────────────────────────────────────────────────────────────────────

UPDATE assignee a
  INNER JOIN ticketsubcategory tsc ON a.ticket_category = tsc.subcategory_id
  SET a.ticket_service_item_id = CASE tsc.category_id
    WHEN 1  THEN 21
    WHEN 2  THEN 39
    WHEN 3  THEN 41
    WHEN 4  THEN 38
    WHEN 6  THEN 40
    WHEN 11 THEN 101
    WHEN 12 THEN 102
    WHEN 16 THEN 113
    WHEN 17 THEN 37
    WHEN 21 THEN 36
  END
  WHERE a.DTYPE = 'Ticket'
    AND a.ticket_category IS NOT NULL
    AND a.ticket_service_item_id IS NULL
    AND tsc.category_id IN (1, 2, 3, 4, 6, 11, 12, 16, 17, 21);


-- ─── Self-Registration ──────────────────────────────────────────────

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V022', 'Orphaned ticket ServiceItem backfill',
        'V022__orphaned_ticket_serviceitem_backfill.sql', NOW());
