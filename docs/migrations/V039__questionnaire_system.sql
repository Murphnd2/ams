-- V039__questionnaire_system.sql
-- Questionnaire system: templates (native + external), fields, instances, values, scoping
-- Prerequisite: V038

-- 1. questionnaire (template definition)
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

-- 2. questionnaire_field (individual questions, native mode only)
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

-- 3. questionnaire_instance (one filling per activity)
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

-- 4. questionnaire_field_value (answers)
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

-- 5. Scoping join tables
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

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V039', 'Questionnaire system: templates, fields, instances, values, scoping', 'V039__questionnaire_system.sql', NOW());
