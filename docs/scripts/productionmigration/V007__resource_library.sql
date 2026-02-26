-- =============================================================================
-- Resource Library + Feature Linking — Production Migration
-- Date: February 21, 2026
-- Target: production beta_ssa schema
--
-- Prerequisites: V001__sales_pipeline.sql must have already been applied
-- (creates feature and marketingmaterial tables)
--
-- This script adds:
--   1. ResourceCategory table for organizing library resources
--   2. Category FK on marketingmaterial
--   3. Widen storage_guid to accommodate UUID.extension
--   4. LibraryResource FK on feature for linked resource icon
-- =============================================================================

-- ─── 1. ResourceCategory table ──────────────────────────────────────────────

CREATE TABLE resourcecategory (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    psp_id BIGINT NOT NULL,
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ─── 2. Category FK on marketingmaterial ────────────────────────────────────

ALTER TABLE marketingmaterial ADD COLUMN category_id BIGINT NULL;
ALTER TABLE marketingmaterial ADD CONSTRAINT fk_mm_category 
    FOREIGN KEY (category_id) REFERENCES resourcecategory(category_id);

-- ─── 3. Widen storage_guid for UUID.extension pattern ───────────────────────
-- Original: VARCHAR(36) sized for bare UUID
-- New: VARCHAR(50) accommodates UUID + "." + extension (e.g., 40 chars for .xlsx)

ALTER TABLE marketingmaterial MODIFY COLUMN storage_guid VARCHAR(50);

-- ─── 4. LibraryResource FK on feature ───────────────────────────────────────
-- Optional link from a feature to a library resource (renders as icon at end of text)

ALTER TABLE feature ADD COLUMN material_id BIGINT NULL;
ALTER TABLE feature ADD CONSTRAINT fk_feature_material 
    FOREIGN KEY (material_id) REFERENCES marketingmaterial(material_id);
