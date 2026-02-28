-- =====================================================================
-- V023 — Drop TicketSubCategory Table
-- =====================================================================
-- Prerequisite: V022 (all tickets backfilled to ticket_service_item_id)
--
-- With all tickets now linked directly to ServiceItems via
-- ticket_service_item_id, the ticketsubcategory table and its FK
-- on assignee are no longer needed.
--
-- This migration:
--   1. Drops the FK constraint from assignee → ticketsubcategory
--   2. Drops the ticket_category column from assignee
--   3. Drops the ticketsubcategory table
-- =====================================================================

-- Step 1: Drop FK constraint
ALTER TABLE assignee DROP FOREIGN KEY FK_ASSIGNEE_ticket_category;

-- Step 2: Drop the old column
ALTER TABLE assignee DROP COLUMN ticket_category;

-- Step 3: Drop the table
DROP TABLE ticketsubcategory;

-- Self-Registration
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V023', 'Drop ticketsubcategory table and FK',
        'V023__drop_ticketsubcategory.sql', NOW());
