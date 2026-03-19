-- =============================================================================
-- Combined Update: V039 through V057
-- Target: Demo PSP (demo.superiorstate.biz) and BPO (bpo.superiorstate.biz)
-- Both currently at V038
-- Generated: 2026-03-19
-- =============================================================================

-- ─── V039: Questionnaire system ───────────────────────────────────────────────

CREATE TABLE questionnaire (
    questionnaire_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    psp_id              BIGINT NOT NULL,
    name                VARCHAR(100) NOT NULL,
    description         VARCHAR(500) DEFAULT NULL,
    activity_type       VARCHAR(20) NOT NULL DEFAULT 'ALL',
    external_url        VARCHAR(500) DEFAULT NULL,
    sort_order          INT NOT NULL DEFAULT 0,
    suppressed          TINYINT NOT NULL DEFAULT 0,
    template_key        VARCHAR(50) DEFAULT NULL,
    CONSTRAINT fk_q_psp FOREIGN KEY (psp_id) REFERENCES assignee(id),
    UNIQUE INDEX idx_q_template_psp (template_key, psp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE questionnaire_field (
    field_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    questionnaire_id    BIGINT NOT NULL,
    field_key           VARCHAR(100) NOT NULL,
    label               VARCHAR(200) DEFAULT NULL,
    field_type          VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    select_options      VARCHAR(500) DEFAULT NULL,
    help_text           VARCHAR(500) DEFAULT NULL,
    section_name        VARCHAR(100) DEFAULT NULL,
    is_required         TINYINT NOT NULL DEFAULT 0,
    sort_order          INT NOT NULL DEFAULT 0,
    suppressed          TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_qf_questionnaire FOREIGN KEY (questionnaire_id)
        REFERENCES questionnaire(questionnaire_id) ON DELETE CASCADE,
    UNIQUE INDEX idx_qf_key (questionnaire_id, field_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE questionnaire_instance (
    instance_id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    questionnaire_id    BIGINT NOT NULL,
    activity_id         BIGINT NOT NULL,
    todo_id             BIGINT DEFAULT NULL,
    instance_guid       VARCHAR(36) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'NOT_STARTED',
    submitted_by_name   VARCHAR(100) DEFAULT NULL,
    submitted_by_email  VARCHAR(200) DEFAULT NULL,
    date_created        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_submitted      TIMESTAMP NULL DEFAULT NULL,
    date_reviewed       TIMESTAMP NULL DEFAULT NULL,
    reviewed_by_id      BIGINT DEFAULT NULL,
    date_reopened       TIMESTAMP NULL DEFAULT NULL,
    reopened_by_id      BIGINT DEFAULT NULL,
    CONSTRAINT fk_qi_questionnaire FOREIGN KEY (questionnaire_id)
        REFERENCES questionnaire(questionnaire_id),
    CONSTRAINT fk_qi_activity FOREIGN KEY (activity_id)
        REFERENCES assignee(id) ON DELETE CASCADE,
    CONSTRAINT fk_qi_todo FOREIGN KEY (todo_id)
        REFERENCES todo(todo_id) ON DELETE SET NULL,
    CONSTRAINT fk_qi_reviewed_by FOREIGN KEY (reviewed_by_id)
        REFERENCES assignee(id),
    CONSTRAINT fk_qi_reopened_by FOREIGN KEY (reopened_by_id)
        REFERENCES assignee(id),
    UNIQUE INDEX idx_qi_guid (instance_guid),
    UNIQUE INDEX idx_qi_q_activity (questionnaire_id, activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE questionnaire_field_value (
    field_value_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    instance_id         BIGINT NOT NULL,
    field_id            BIGINT NOT NULL,
    field_value         TEXT DEFAULT NULL,
    CONSTRAINT fk_qfv_instance FOREIGN KEY (instance_id)
        REFERENCES questionnaire_instance(instance_id) ON DELETE CASCADE,
    CONSTRAINT fk_qfv_field FOREIGN KEY (field_id)
        REFERENCES questionnaire_field(field_id),
    UNIQUE INDEX idx_qfv_instance_field (instance_id, field_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE questionnaire_los (
    questionnaire_id    BIGINT NOT NULL,
    los_id              BIGINT NOT NULL,
    PRIMARY KEY (questionnaire_id, los_id),
    CONSTRAINT fk_ql_questionnaire FOREIGN KEY (questionnaire_id)
        REFERENCES questionnaire(questionnaire_id) ON DELETE CASCADE,
    CONSTRAINT fk_ql_los FOREIGN KEY (los_id)
        REFERENCES los(los_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE questionnaire_enhancement (
    questionnaire_id    BIGINT NOT NULL,
    enhancement_id      BIGINT NOT NULL,
    PRIMARY KEY (questionnaire_id, enhancement_id),
    CONSTRAINT fk_qe_questionnaire FOREIGN KEY (questionnaire_id)
        REFERENCES questionnaire(questionnaire_id) ON DELETE CASCADE,
    CONSTRAINT fk_qe_enhancement FOREIGN KEY (enhancement_id)
        REFERENCES enhancement(enhancement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE questionnaire_serviceitem (
    questionnaire_id    BIGINT NOT NULL,
    purpose_id          INT NOT NULL,
    PRIMARY KEY (questionnaire_id, purpose_id),
    CONSTRAINT fk_qs_questionnaire FOREIGN KEY (questionnaire_id)
        REFERENCES questionnaire(questionnaire_id) ON DELETE CASCADE,
    CONSTRAINT fk_qs_serviceitem FOREIGN KEY (purpose_id)
        REFERENCES templatepurpose(purpose_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V039', 'Questionnaire system: templates, fields, instances, values, scoping', 'V039__questionnaire_system.sql', NOW());

-- ─── V040: Delegated todo recurring series ────────────────────────────────────

ALTER TABLE delegated_todo
    ADD COLUMN recurring_series_id VARCHAR(50) NULL COMMENT 'PSP RecurringTaskList ID; NULL = non-recurring';

CREATE INDEX idx_delegated_recurring_series ON delegated_todo (recurring_series_id);

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V040', 'Add recurring_series_id to delegated_todo for BPO recurring history', 'V040__delegated_todo_recurring_series.sql', NOW());

-- ─── V041: Application reviewer fields ────────────────────────────────────────

SET @db = DATABASE();

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'application' AND COLUMN_NAME = 'reviewed_by');
SET @sql = IF(@col = 0, 'ALTER TABLE application ADD COLUMN reviewed_by BIGINT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'application' AND COLUMN_NAME = 'review_notes');
SET @sql = IF(@col = 0, 'ALTER TABLE application ADD COLUMN review_notes TEXT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'application' AND COLUMN_NAME = 'date_reviewed');
SET @sql = IF(@col = 0, 'ALTER TABLE application ADD COLUMN date_reviewed TIMESTAMP NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
           WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'application' AND CONSTRAINT_NAME = 'fk_application_reviewed_by');
SET @sql = IF(@fk = 0, 'ALTER TABLE application ADD CONSTRAINT fk_application_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES assignee(id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V041', 'Add reviewer tracking fields to application table', 'V041__application_reviewer_fields.sql', NOW());

-- ─── V042: BPO pending approval (docs-only) ──────────────────────────────────

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V042', 'BPO pending approval workflow - status PENDING support', 'V042__delegated_todo_pending_status.sql', NOW());

-- ─── V043: Custom landing page ────────────────────────────────────────────────

ALTER TABLE constant ADD COLUMN text_value TEXT DEFAULT NULL;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V043', 'Add text_value column to constant for custom landing page HTML', 'V043__custom_landing_page.sql', NOW());

-- ─── V044: Proposal section agency scoping ────────────────────────────────────

ALTER TABLE proposal_section ADD COLUMN agency_id BIGINT DEFAULT NULL AFTER psp_id;

ALTER TABLE proposal_section ADD CONSTRAINT fk_ps_agency
    FOREIGN KEY (agency_id) REFERENCES agency(agency_id) ON DELETE SET NULL;

ALTER TABLE proposal_section ADD UNIQUE INDEX uq_ps_psp_agency_type (psp_id, agency_id, section_type);

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V044', 'Add agency scoping to proposal_section for TITLE/CLOSING overrides', 'V044__proposal_section_agency_scoping.sql', NOW());

-- ─── V045: Application selected services ──────────────────────────────────────

ALTER TABLE application ADD COLUMN selected_los_ids VARCHAR(500) NULL;
ALTER TABLE application ADD COLUMN selected_enhancement_ids VARCHAR(500) NULL;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V045', 'Application selected LOS and Enhancement IDs', 'V045__application_selected_services.sql', NOW());

-- ─── V046: Chatbot skill table ────────────────────────────────────────────────

CREATE TABLE chatbot_skill (
    skill_id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    psp_id              BIGINT NOT NULL,
    skill_name          VARCHAR(100) NOT NULL,
    description         VARCHAR(500) DEFAULT NULL,
    system_prompt       TEXT NOT NULL,
    trigger_keywords    VARCHAR(500) DEFAULT NULL,
    accepts_file_upload BOOLEAN NOT NULL DEFAULT FALSE,
    accepted_mime_types VARCHAR(200) DEFAULT NULL,
    model               VARCHAR(100) NOT NULL DEFAULT 'claude-haiku-4-5-20251001',
    max_tokens          INT NOT NULL DEFAULT 1024,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    is_admin_only       BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order          INT NOT NULL DEFAULT 100,
    date_created        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modified       TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_cs_psp FOREIGN KEY (psp_id) REFERENCES assignee(id),
    INDEX idx_cs_psp_active (psp_id, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V046', 'Chatbot skill table for extensible AI assistant capabilities', 'V046__chatbot_skill_table.sql', NOW());

-- ─── V047: Composite task order ───────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS composite_task_order (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    psp_id      BIGINT NOT NULL,
    group_id    INT NOT NULL,
    task_id     BIGINT NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cto_psp FOREIGN KEY (psp_id) REFERENCES assignee(person_id),
    CONSTRAINT fk_cto_group FOREIGN KEY (group_id) REFERENCES templategroup(group_id),
    CONSTRAINT fk_cto_task FOREIGN KEY (task_id) REFERENCES task(task_id),
    UNIQUE INDEX uq_cto_psp_group_task (psp_id, group_id, task_id)
);

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V047', 'Composite task order table for cross-sequence ordering', 'V047__composite_task_order.sql', NOW());

-- ─── V048: Universal import system ────────────────────────────────────────────

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

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V048', 'Universal import system tables and seed data', 'V048__universal_import_system.sql', NOW());

-- ─── V049: Delegated todo source_task_id ──────────────────────────────────────

ALTER TABLE delegated_todo
    ADD COLUMN source_task_id VARCHAR(20) NULL AFTER recurring_series_id;

CREATE INDEX idx_delegated_todo_source_task
    ON delegated_todo (source_task_id, psp_client_id);

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V049', 'Add source_task_id to delegated_todo for required-sequence auto-approval', 'V049__delegated_todo_source_task_id.sql', NOW());

-- ─── V050: BPO default assignee ───────────────────────────────────────────────

ALTER TABLE psp_clients ADD COLUMN default_assignee_id BIGINT NULL;
ALTER TABLE psp_clients ADD CONSTRAINT fk_psp_client_default_assignee
    FOREIGN KEY (default_assignee_id) REFERENCES assignee(id);

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V050', 'BPO default assignee per PSP client', 'V050__bpo_default_assignee.sql', NOW());

-- ─── V051: Import ID mapping ──────────────────────────────────────────────────

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

-- Back-fill (safe no-op if no SUMMIT provider)
INSERT IGNORE INTO import_id_mapping (provider_id, entity_type, external_id, internal_id, is_primary)
SELECT p.provider_id, 'PLAN_TYPE', CAST(pt.PlanType_ID AS CHAR), pt.PlanType_ID, 1
FROM plantype pt
CROSS JOIN (SELECT provider_id FROM import_provider WHERE provider_code = 'SUMMIT' LIMIT 1) p;

INSERT IGNORE INTO import_id_mapping (provider_id, entity_type, external_id, internal_id, is_primary)
SELECT p.provider_id, 'EMPLOYER', CAST(e.organization_id AS CHAR), e.organization_id, 1
FROM employer e
CROSS JOIN (SELECT provider_id FROM import_provider WHERE provider_code = 'SUMMIT' LIMIT 1) p;

INSERT IGNORE INTO import_id_mapping (provider_id, entity_type, external_id, internal_id, is_primary)
SELECT p.provider_id, 'EMPLOYEE', CAST(ee.employee_id AS CHAR), ee.employee_id, 1
FROM employee ee
CROSS JOIN (SELECT provider_id FROM import_provider WHERE provider_code = 'SUMMIT' LIMIT 1) p;

INSERT IGNORE INTO import_id_mapping (provider_id, entity_type, external_id, internal_id, is_primary)
SELECT p.provider_id, 'BENEFIT', CAST(b.summit_id AS CHAR), b.benefit_id, 1
FROM benefit b
CROSS JOIN (SELECT provider_id FROM import_provider WHERE provider_code = 'SUMMIT' LIMIT 1) p
WHERE b.source_type = 'CDH';

INSERT IGNORE INTO import_id_mapping (provider_id, entity_type, external_id, internal_id, is_primary)
SELECT p.provider_id, 'BENEFIT', CONCAT('COBRA-', CAST(b.summit_id AS CHAR)), b.benefit_id, 1
FROM benefit b
CROSS JOIN (SELECT provider_id FROM import_provider WHERE provider_code = 'SUMMIT' LIMIT 1) p
WHERE b.source_type = 'COBRA';

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V051', 'Import ID mapping cross-reference table', 'V051__import_id_mapping.sql', NOW());

-- ─── V052: Import run log xref tracking ───────────────────────────────────────

ALTER TABLE import_run_log
    ADD COLUMN xref_resolved INT NOT NULL DEFAULT 0 COMMENT 'Records found via import_id_mapping cross-reference',
    ADD COLUMN pk_allocated INT NOT NULL DEFAULT 0 COMMENT 'Records that needed new PK due to conflict',
    ADD COLUMN mappings_recorded INT NOT NULL DEFAULT 0 COMMENT 'New cross-reference mappings recorded';

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V052', 'Import run log cross-reference tracking columns', 'V052__import_run_log_xref_tracking.sql', NOW());

-- ─── V053: Interactive import enhancements ────────────────────────────────────

ALTER TABLE import_file_type
    ADD COLUMN update_mode VARCHAR(20) NOT NULL DEFAULT 'CREATE_AND_UPDATE',
    ADD COLUMN mapping_status VARCHAR(10) NOT NULL DEFAULT 'PENDING';

ALTER TABLE import_field_mapping
    ADD COLUMN is_fk TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN fk_entity_type VARCHAR(20) NULL;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V053', 'Interactive import enhancements: update mode, mapping status, FK flags', 'V053__interactive_import_enhancements.sql', NOW());

-- ─── V054: Super User Dashboard — managed_installation ────────────────────────

CREATE TABLE IF NOT EXISTS managed_installation (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    installation_name     VARCHAR(100) NOT NULL,
    installation_url      VARCHAR(255) NOT NULL,
    system_type           VARCHAR(10)  NOT NULL COMMENT 'PSP or BPO',
    api_token_outbound    VARCHAR(64)            COMMENT 'Token master sends TO installation',
    api_token_inbound     VARCHAR(64)            COMMENT 'Token installation sends TO master',
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, ACTIVE, DISCONNECTED',
    last_heartbeat        DATETIME,
    last_schema_version   VARCHAR(20),
    last_app_version      VARCHAR(20),
    last_user_count       INT,
    date_registered       DATE         NOT NULL,
    date_approved         DATE,
    date_disconnected     DATE,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    notes                 TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V054', 'Super User Dashboard — managed_installation table', 'V054__managed_installation.sql', NOW());

-- ─── V055: Schema info view ───────────────────────────────────────────────────

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V055' AS version, '2026-03-15' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V055', 'Schema info view for structural version identification', 'V055__schema_info_view.sql', NOW());

-- ─── V056: Training video and token tables ────────────────────────────────────

CREATE TABLE training_video (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename         VARCHAR(255) NOT NULL,
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    duration_seconds INT,
    active           TINYINT(1) NOT NULL DEFAULT 1,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_training_video_filename (filename)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE video_token (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    token           VARCHAR(36) NOT NULL,
    video_id        BIGINT NOT NULL,
    recipient_name  VARCHAR(200),
    recipient_email VARCHAR(255),
    created_by      BIGINT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    first_viewed_at DATETIME,
    last_viewed_at  DATETIME,
    view_count      INT NOT NULL DEFAULT 0,
    max_views       INT NOT NULL DEFAULT 1,
    expires_at      DATETIME,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    UNIQUE KEY uq_video_token (token),
    CONSTRAINT fk_vt_video   FOREIGN KEY (video_id)   REFERENCES training_video(id),
    CONSTRAINT fk_vt_creator FOREIGN KEY (created_by)  REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V056' AS version, '2026-03-16' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V056', 'Training video and single-use token tables', 'V056__training_video_tokens.sql', NOW());

-- ─── V057: Agency suppressed flag ─────────────────────────────────────────────

ALTER TABLE agency ADD COLUMN suppressed TINYINT(1) NOT NULL DEFAULT 0;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V057' AS version, '2026-03-19' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V057', 'Add suppressed flag to agency table', 'V057__agency_suppressed.sql', NOW());

-- =============================================================================
-- Done. Verify: SELECT * FROM schema_version ORDER BY version;
-- =============================================================================
