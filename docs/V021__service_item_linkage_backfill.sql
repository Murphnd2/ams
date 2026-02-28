-- =====================================================================
-- V021 — ServiceItem Linkage: LOS/Enhancement Backfill
-- =====================================================================
-- Prerequisite: V020
--
-- This migration:
--   1. Creates new group 2 ServiceItems for LOS/Enhancement types that
--      don't yet have one (MERP, ICHRA, EBHRA, QSEHRA, etc.)
--   2. Renames SI 17 from "Payments / Check" to "Payment Services"
--   3. Suppresses SI 18 (Payments / EFT) and SI 20 (Card Co-pays)
--      — covered by SI 17 and SI 19 respectively
--   4. Links all LOS records to their ServiceItem via service_item_id
--   5. Links all Enhancement records to their ServiceItem via service_item_id
--
-- ID assignments for new ServiceItems use the 25-35 range (low seed IDs),
-- avoiding collision with DataPath-imported IDs (87000+ range).
-- =====================================================================

-- ─── PHASE 1: New Group 2 ServiceItems ──────────────────────────────

INSERT INTO templatepurpose (purpose_id, description, group_id, psp_id, source_type, is_suppressed, has_required_tasks)
VALUES
  (25, 'MERP',                2, 4, 'MANUAL', 0, 1),
  (26, 'ICHRA',               2, 4, 'MANUAL', 0, 1),
  (27, 'EBHRA',               2, 4, 'MANUAL', 0, 1),
  (28, 'QSEHRA',              2, 4, 'MANUAL', 0, 1),
  (29, 'Retiree Billing',     2, 4, 'MANUAL', 0, 1),
  (31, 'Direct Billing',      2, 4, 'MANUAL', 0, 1),
  (32, 'LSA',                 2, 4, 'MANUAL', 0, 1),
  (33, 'Adoption Assistance', 2, 4, 'MANUAL', 0, 1),
  (34, 'Document Services',   2, 4, 'MANUAL', 0, 1),
  (35, 'Multi-Plan Discounts',2, 4, 'MANUAL', 0, 1);


-- ─── PHASE 2: Rename and Suppress ───────────────────────────────────

-- SI 17: broaden from "Payments / Check" to "Payment Services"
UPDATE templatepurpose SET description = 'Payment Services' WHERE purpose_id = 17;

-- SI 18: suppress — covered by SI 17 (Payment Services)
UPDATE templatepurpose SET is_suppressed = 1 WHERE purpose_id = 18;

-- SI 20: suppress — covered by SI 19 (Debit Cards)
UPDATE templatepurpose SET is_suppressed = 1 WHERE purpose_id = 20;


-- ─── PHASE 3: Link LOS → ServiceItem ────────────────────────────────

UPDATE los SET service_item_id =  11 WHERE los_id =  5;  -- POP → POP
UPDATE los SET service_item_id =  12 WHERE los_id =  6;  -- FSA → FSA
UPDATE los SET service_item_id =  14 WHERE los_id =  8;  -- COBRA → COBRA
UPDATE los SET service_item_id =  16 WHERE los_id =  9;  -- HSA → HSA
UPDATE los SET service_item_id =  15 WHERE los_id = 10;  -- Transit → Transit
UPDATE los SET service_item_id =  13 WHERE los_id = 11;  -- HRA → HRA
UPDATE los SET service_item_id =  25 WHERE los_id = 12;  -- MERP → MERP
UPDATE los SET service_item_id =  26 WHERE los_id = 13;  -- ICHRA → ICHRA
UPDATE los SET service_item_id =  27 WHERE los_id = 14;  -- EBHRA → EBHRA
UPDATE los SET service_item_id =  28 WHERE los_id = 15;  -- QSEHRA → QSEHRA
UPDATE los SET service_item_id =  29 WHERE los_id = 16;  -- Retiree Billing → Retiree Billing
UPDATE los SET service_item_id =  31 WHERE los_id = 17;  -- Direct Billing → Direct Billing
UPDATE los SET service_item_id =  32 WHERE los_id = 18;  -- LSA → LSA
UPDATE los SET service_item_id =  33 WHERE los_id = 19;  -- Adoption Assistance → Adoption Assistance


-- ─── PHASE 4: Link Enhancement → ServiceItem ────────────────────────

UPDATE enhancement SET service_item_id = 19 WHERE enhancement_id = 1;  -- Debit Card Services → Debit Cards
UPDATE enhancement SET service_item_id = 17 WHERE enhancement_id = 2;  -- Payment Services → Payment Services
UPDATE enhancement SET service_item_id = 34 WHERE enhancement_id = 3;  -- Document Services → Document Services
UPDATE enhancement SET service_item_id = 35 WHERE enhancement_id = 4;  -- Multi-Plan Discounts → Multi-Plan Discounts


-- ─── Self-Registration ──────────────────────────────────────────────

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V021', 'ServiceItem linkage - LOS/Enhancement backfill, Payment Services rename, suppress duplicates',
        'V021__service_item_linkage_backfill.sql', NOW());
