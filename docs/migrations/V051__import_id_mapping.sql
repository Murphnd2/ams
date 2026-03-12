-- =============================================================================
-- V051: Import ID Mapping cross-reference table
-- Provides a provider-aware identity mapping layer so that external IDs from
-- multiple providers (Summit, DataPath, competitors) can coexist without
-- primary key collisions in the AMS archive tables.
-- =============================================================================

-- Cross-reference table: maps (provider, entity_type, external_id) → internal AMS PK
CREATE TABLE IF NOT EXISTS import_id_mapping (
    mapping_id      INT AUTO_INCREMENT PRIMARY KEY,
    provider_id     INT NOT NULL,
    entity_type     VARCHAR(20) NOT NULL COMMENT 'PLAN_TYPE, EMPLOYER, EMPLOYEE, BENEFIT',
    external_id     VARCHAR(50) NOT NULL COMMENT 'ID from the source system',
    internal_id     INT NOT NULL COMMENT 'PK in the AMS archive table',
    is_primary      TINYINT(1) NOT NULL DEFAULT 1 COMMENT 'Active mapping for updates?',
    notes           VARCHAR(200) DEFAULT NULL,
    created_on      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_on      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uq_provider_entity_external (provider_id, entity_type, external_id),
    KEY idx_entity_internal (entity_type, internal_id),
    CONSTRAINT fk_idmap_provider FOREIGN KEY (provider_id) REFERENCES import_provider(provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================================
-- Back-fill existing data into import_id_mapping.
-- This assumes that the first import_provider row with provider_code = 'SUMMIT'
-- is the source of all existing archive records. If no SUMMIT provider exists,
-- these INSERTs will produce 0 rows (safe no-op).
-- =============================================================================

-- Back-fill plan types
INSERT IGNORE INTO import_id_mapping (provider_id, entity_type, external_id, internal_id, is_primary)
SELECT
    p.provider_id, 'PLAN_TYPE', CAST(pt.PlanType_ID AS CHAR), pt.PlanType_ID, 1
FROM plantype pt
CROSS JOIN (SELECT provider_id FROM import_provider WHERE provider_code = 'SUMMIT' LIMIT 1) p;

-- Back-fill employers
INSERT IGNORE INTO import_id_mapping (provider_id, entity_type, external_id, internal_id, is_primary)
SELECT
    p.provider_id, 'EMPLOYER', CAST(e.organization_id AS CHAR), e.organization_id, 1
FROM employer e
CROSS JOIN (SELECT provider_id FROM import_provider WHERE provider_code = 'SUMMIT' LIMIT 1) p;

-- Back-fill employees
INSERT IGNORE INTO import_id_mapping (provider_id, entity_type, external_id, internal_id, is_primary)
SELECT
    p.provider_id, 'EMPLOYEE', CAST(ee.employee_id AS CHAR), ee.employee_id, 1
FROM employee ee
CROSS JOIN (SELECT provider_id FROM import_provider WHERE provider_code = 'SUMMIT' LIMIT 1) p;

-- Back-fill benefits (CDH) — external_id = summit_id
INSERT IGNORE INTO import_id_mapping (provider_id, entity_type, external_id, internal_id, is_primary)
SELECT
    p.provider_id, 'BENEFIT', CAST(b.summit_id AS CHAR), b.benefit_id, 1
FROM benefit b
CROSS JOIN (SELECT provider_id FROM import_provider WHERE provider_code = 'SUMMIT' LIMIT 1) p
WHERE b.source_type = 'CDH';

-- Back-fill benefits (COBRA) — prefix external_id to distinguish from CDH
INSERT IGNORE INTO import_id_mapping (provider_id, entity_type, external_id, internal_id, is_primary)
SELECT
    p.provider_id, 'BENEFIT', CONCAT('COBRA-', CAST(b.summit_id AS CHAR)), b.benefit_id, 1
FROM benefit b
CROSS JOIN (SELECT provider_id FROM import_provider WHERE provider_code = 'SUMMIT' LIMIT 1) p
WHERE b.source_type = 'COBRA';

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V051', 'Import ID mapping cross-reference table', 'V051__import_id_mapping.sql', NOW());
