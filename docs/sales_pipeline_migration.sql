-- =============================================================================
-- Sales Pipeline Migration Script
-- Project: AMS Sales Portal (#2)
-- Created: February 19, 2026
-- Target: beta_ssa schema (run on both local and production)
-- =============================================================================
-- IMPORTANT: Run these in order. Some statements depend on prior ones.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- STEP 1: Rename datakey table to applicationfield
-- -----------------------------------------------------------------------------
-- Drop FK from datapair that references datakey first
ALTER TABLE datapair DROP FOREIGN KEY FK_DATAPAIR_key_name;

RENAME TABLE datakey TO applicationfield;

-- Rename columns and add new metadata fields
ALTER TABLE applicationfield
  CHANGE COLUMN key_name field_key VARCHAR(100),
  CHANGE COLUMN easy_name label VARCHAR(200),
  ADD COLUMN field_type VARCHAR(20) DEFAULT 'TEXT',
  ADD COLUMN is_required TINYINT DEFAULT 0,
  ADD COLUMN sort_order INT DEFAULT 0,
  ADD COLUMN select_options VARCHAR(500);

-- -----------------------------------------------------------------------------
-- STEP 2: Add new columns to proposal
-- -----------------------------------------------------------------------------
ALTER TABLE proposal
  ADD COLUMN status VARCHAR(20) DEFAULT 'CREATED',
  ADD COLUMN created_by BIGINT,
  ADD COLUMN date_sent TIMESTAMP NULL,
  ADD COLUMN date_viewed TIMESTAMP NULL,
  ADD COLUMN date_applied TIMESTAMP NULL;

-- -----------------------------------------------------------------------------
-- STEP 3: Add new columns to application
-- -----------------------------------------------------------------------------
ALTER TABLE application
  ADD COLUMN status VARCHAR(20) DEFAULT 'IN_PROGRESS',
  ADD COLUMN date_started TIMESTAMP NULL,
  ADD COLUMN date_submitted TIMESTAMP NULL,
  ADD COLUMN date_reviewed TIMESTAMP NULL,
  ADD COLUMN reviewed_by BIGINT,
  ADD COLUMN review_notes TEXT;

-- -----------------------------------------------------------------------------
-- STEP 4: Create new tables
-- -----------------------------------------------------------------------------

CREATE TABLE feature (
    feature_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(500) NOT NULL,
    sort_order INT,
    module_id BIGINT NOT NULL,
    psp_id BIGINT NOT NULL,
    FOREIGN KEY (module_id) REFERENCES servicemodule(module_id),
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ratediscount (
    ratediscount_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(200) NOT NULL,
    discount_amount DOUBLE NOT NULL,
    rate_id BIGINT NOT NULL,
    price_item_id BIGINT NOT NULL,
    FOREIGN KEY (rate_id) REFERENCES rate(rate_id),
    FOREIGN KEY (price_item_id) REFERENCES priceitem(price_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ratediscountlos (
    ratediscount_id BIGINT NOT NULL,
    los_id BIGINT NOT NULL,
    PRIMARY KEY (ratediscount_id, los_id),
    FOREIGN KEY (ratediscount_id) REFERENCES ratediscount(ratediscount_id),
    FOREIGN KEY (los_id) REFERENCES los(los_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE marketingmaterial (
    material_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    material_type VARCHAR(20) NOT NULL,
    url VARCHAR(500),
    storage_guid VARCHAR(36),
    audience VARCHAR(20),
    sort_order INT,
    psp_id BIGINT NOT NULL,
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE materialmodule (
    material_id BIGINT NOT NULL,
    module_id BIGINT NOT NULL,
    PRIMARY KEY (material_id, module_id),
    FOREIGN KEY (material_id) REFERENCES marketingmaterial(material_id),
    FOREIGN KEY (module_id) REFERENCES servicemodule(module_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE applicationfieldvalue (
    field_value_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    field_key VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    field_value TEXT,
    FOREIGN KEY (application_id) REFERENCES application(proposal_id),
    FOREIGN KEY (field_key) REFERENCES applicationfield(field_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- -----------------------------------------------------------------------------
-- STEP 5 (OPTIONAL CLEANUP): Drop old tables after confirming new structure works
-- Run these only after verifying the application works correctly.
-- -----------------------------------------------------------------------------
-- DROP TABLE IF EXISTS applicationdata;
-- DROP TABLE IF EXISTS datapair;
