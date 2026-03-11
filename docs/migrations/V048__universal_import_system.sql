-- =============================================================================
-- V048: Universal Import System
-- Creates provider registry, file type definitions, column mappings,
-- plan type mappings, and import run log tables.
-- Seeds universal plan type codes recognized across all TPA platforms.
-- =============================================================================

-- ─── Provider Registry ──────────────────────────────────────────────────────
CREATE TABLE import_provider (
    provider_id     INT AUTO_INCREMENT PRIMARY KEY,
    provider_name   VARCHAR(100) NOT NULL,
    provider_code   VARCHAR(30) NOT NULL UNIQUE,
    description     VARCHAR(500) NULL,
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    psp_id          BIGINT NOT NULL,
    created_on      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_provider_psp FOREIGN KEY (psp_id) REFERENCES assignee(id)
);

-- ─── File Type Definitions ──────────────────────────────────────────────────
CREATE TABLE import_file_type (
    file_type_id    INT AUTO_INCREMENT PRIMARY KEY,
    provider_id     INT NOT NULL,
    file_label      VARCHAR(100) NOT NULL,
    target_entity   VARCHAR(20) NOT NULL,
    file_format     VARCHAR(10) NOT NULL DEFAULT 'CSV',
    sort_order      INT NOT NULL DEFAULT 0,
    is_required     TINYINT(1) NOT NULL DEFAULT 1,
    description     VARCHAR(500) NULL,
    CONSTRAINT fk_file_type_provider FOREIGN KEY (provider_id) REFERENCES import_provider(provider_id)
);

-- ─── Column Mappings ────────────────────────────────────────────────────────
CREATE TABLE import_field_mapping (
    mapping_id          INT AUTO_INCREMENT PRIMARY KEY,
    file_type_id        INT NOT NULL,
    source_column       VARCHAR(100) NOT NULL,
    canonical_field     VARCHAR(50) NOT NULL,
    is_required         TINYINT(1) NOT NULL DEFAULT 0,
    is_key              TINYINT(1) NOT NULL DEFAULT 0,
    transform_rule      VARCHAR(200) NULL,
    CONSTRAINT fk_field_mapping_file_type FOREIGN KEY (file_type_id) REFERENCES import_file_type(file_type_id)
);

-- ─── Plan Type Mappings ─────────────────────────────────────────────────────
-- provider_id is nullable: NULL = universal system default mapping
CREATE TABLE import_plan_type_mapping (
    mapping_id          INT AUTO_INCREMENT PRIMARY KEY,
    provider_id         INT NULL,
    source_plan_code    VARCHAR(50) NOT NULL,
    source_plan_name    VARCHAR(200) NULL,
    target_plan_type_id INT NULL,
    is_system_default   TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_plan_type_mapping_provider FOREIGN KEY (provider_id) REFERENCES import_provider(provider_id),
    CONSTRAINT fk_plan_type_mapping_target FOREIGN KEY (target_plan_type_id) REFERENCES plantype(PlanType_ID)
);

-- ─── Import Run Log ─────────────────────────────────────────────────────────
CREATE TABLE import_run_log (
    run_id          INT AUTO_INCREMENT PRIMARY KEY,
    provider_id     INT NOT NULL,
    run_by          BIGINT NOT NULL,
    started_on      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_on    TIMESTAMP NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    plan_types_inserted   INT DEFAULT 0,
    plan_types_updated    INT DEFAULT 0,
    plan_types_skipped    INT DEFAULT 0,
    employers_inserted    INT DEFAULT 0,
    employers_updated     INT DEFAULT 0,
    employers_skipped     INT DEFAULT 0,
    employees_inserted    INT DEFAULT 0,
    employees_updated     INT DEFAULT 0,
    employees_skipped     INT DEFAULT 0,
    benefits_inserted     INT DEFAULT 0,
    benefits_updated      INT DEFAULT 0,
    benefits_skipped      INT DEFAULT 0,
    service_items_created INT DEFAULT 0,
    warnings        TEXT NULL,
    errors          TEXT NULL,
    CONSTRAINT fk_import_run_provider FOREIGN KEY (provider_id) REFERENCES import_provider(provider_id),
    CONSTRAINT fk_import_run_user FOREIGN KEY (run_by) REFERENCES assignee(id)
);

-- ─── Seed Universal Plan Type Codes ─────────────────────────────────────────
-- These are benefit type codes recognized across all TPA platforms.
-- Seeded as system defaults (provider_id = NULL, is_system_default = 1).
-- target_plan_type_id is NULL — mapped to actual plan types per-provider.
INSERT IGNORE INTO import_plan_type_mapping (provider_id, source_plan_code, source_plan_name, target_plan_type_id, is_system_default) VALUES
(NULL, 'FSA',     'Flexible Spending Account',                  NULL, 1),
(NULL, 'HRA',     'Health Reimbursement Arrangement',            NULL, 1),
(NULL, 'HSA',     'Health Savings Account',                      NULL, 1),
(NULL, 'DCA',     'Dependent Care Account',                      NULL, 1),
(NULL, 'COBRA',   'COBRA Administration',                        NULL, 1),
(NULL, 'LFSA',    'Limited-Purpose FSA',                         NULL, 1),
(NULL, 'MERP',    'Medical Expense Reimbursement Plan',          NULL, 1),
(NULL, 'ICHRA',   'Individual Coverage HRA',                     NULL, 1),
(NULL, 'EBHRA',   'Excepted Benefit HRA',                        NULL, 1),
(NULL, 'DENTAL',  'Dental',                                      NULL, 1),
(NULL, 'VISION',  'Vision',                                      NULL, 1),
(NULL, 'MEDICAL', 'Medical',                                     NULL, 1),
(NULL, 'LIFE',    'Life Insurance',                              NULL, 1),
(NULL, 'EAP',     'Employee Assistance Program',                 NULL, 1),
(NULL, 'PHARMACY','Pharmacy',                                    NULL, 1),
(NULL, 'TRANSIT', 'Transit/Commuter',                            NULL, 1),
(NULL, 'PARKING', 'Parking',                                     NULL, 1);

-- ─── Self-register migration ────────────────────────────────────────────────
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V048', 'Universal import system tables and seed data', 'V048__universal_import_system.sql', NOW());
