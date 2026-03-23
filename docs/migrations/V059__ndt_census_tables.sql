-- V059: NDT Census-Based Testing Tables
-- Creates the foundation for the NDT Section 125 census-based testing system.
-- Three tables: ndt_test_run (main), ndt_document_upload (files), ndt_access_log (audit).

-- =============================================================================
-- Table 1: ndt_test_run
-- One row per NDT test engagement, linked to a Renewal activity.
-- JSON data stored as LONGTEXT (not MySQL JSON type) for EclipseLink compatibility.
-- =============================================================================
CREATE TABLE IF NOT EXISTS ndt_test_run (
    test_run_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id       BIGINT NOT NULL,
    psp_id            BIGINT NOT NULL,
    employer_name     VARCHAR(200) NOT NULL,
    plan_year_end     DATE NOT NULL,
    status            VARCHAR(30) NOT NULL DEFAULT 'data_collection',

    -- JSON data columns (LONGTEXT for EclipseLink compatibility)
    plan_data         LONGTEXT DEFAULT NULL,
    census_data       LONGTEXT DEFAULT NULL,
    test_results      LONGTEXT DEFAULT NULL,
    gap_analysis      LONGTEXT DEFAULT NULL,
    parse_log         LONGTEXT DEFAULT NULL,

    -- Metadata
    employee_count    INT NOT NULL DEFAULT 0,
    document_count    INT NOT NULL DEFAULT 0,
    data_quality_score DECIMAL(5,2) DEFAULT NULL,

    -- Audit
    created_by        BIGINT NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    submitted_at      TIMESTAMP NULL DEFAULT NULL,
    submitted_by      BIGINT DEFAULT NULL,

    -- Foreign keys (all reference assignee table due to single-table inheritance)
    CONSTRAINT fk_ndt_run_activity   FOREIGN KEY (activity_id) REFERENCES assignee(id),
    CONSTRAINT fk_ndt_run_psp        FOREIGN KEY (psp_id) REFERENCES assignee(id),
    CONSTRAINT fk_ndt_run_created_by FOREIGN KEY (created_by) REFERENCES assignee(id),
    CONSTRAINT fk_ndt_run_submitted  FOREIGN KEY (submitted_by) REFERENCES assignee(id),

    -- Indexes
    INDEX idx_ndt_activity (activity_id),
    INDEX idx_ndt_psp_status (psp_id, status),
    INDEX idx_ndt_plan_year (plan_year_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================================
-- Table 2: ndt_document_upload
-- Tracks each file uploaded to a test run.
-- =============================================================================
CREATE TABLE IF NOT EXISTS ndt_document_upload (
    upload_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_run_id       BIGINT NOT NULL,
    document_type     VARCHAR(30) NOT NULL DEFAULT 'other',
    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(255) NOT NULL,
    file_size_bytes   BIGINT NOT NULL DEFAULT 0,
    mime_type         VARCHAR(100) DEFAULT NULL,

    -- Parse tracking
    parse_status      VARCHAR(20) NOT NULL DEFAULT 'pending',
    parse_confidence  DECIMAL(5,2) DEFAULT NULL,
    column_mapping    LONGTEXT DEFAULT NULL,
    parse_errors      LONGTEXT DEFAULT NULL,
    records_extracted INT DEFAULT NULL,

    -- Security
    virus_scan_status VARCHAR(20) NOT NULL DEFAULT 'skipped',
    ssn_detected      TINYINT NOT NULL DEFAULT 0,
    ssn_scrubbed      TINYINT NOT NULL DEFAULT 0,
    pii_scrub_log     LONGTEXT DEFAULT NULL,

    -- Audit
    uploaded_by       BIGINT DEFAULT NULL,
    uploaded_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_ndt_upload_run       FOREIGN KEY (test_run_id) REFERENCES ndt_test_run(test_run_id) ON DELETE CASCADE,
    CONSTRAINT fk_ndt_upload_person    FOREIGN KEY (uploaded_by) REFERENCES assignee(id),

    -- Indexes
    INDEX idx_ndt_upload_run (test_run_id),
    INDEX idx_ndt_upload_status (parse_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================================
-- Table 3: ndt_access_log
-- Audit trail for all actions on a test run.
-- =============================================================================
CREATE TABLE IF NOT EXISTS ndt_access_log (
    log_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_run_id       BIGINT NOT NULL,
    person_id         BIGINT DEFAULT NULL,
    action            VARCHAR(50) NOT NULL,
    detail            VARCHAR(500) DEFAULT NULL,
    ip_address        VARCHAR(45) DEFAULT NULL,
    accessed_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Foreign keys
    CONSTRAINT fk_ndt_log_run    FOREIGN KEY (test_run_id) REFERENCES ndt_test_run(test_run_id) ON DELETE CASCADE,
    CONSTRAINT fk_ndt_log_person FOREIGN KEY (person_id) REFERENCES assignee(id),

    -- Index
    INDEX idx_ndt_log_run (test_run_id, accessed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =============================================================================
-- Schema version registration
-- =============================================================================
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V059', 'NDT census-based testing tables', 'V059__ndt_census_tables.sql', NOW());
