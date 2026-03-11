-- =============================================================================
-- V046 — Chatbot skill table for extensible AI assistant capabilities
-- Creates the chatbot_skill table for per-PSP, admin-configurable AI skills.
-- Each skill defines a specialized system prompt, trigger conditions,
-- file acceptance rules, and model preferences.
-- =============================================================================

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

-- Self-register migration
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V046', 'Chatbot skill table for extensible AI assistant capabilities', 'V046__chatbot_skill_table.sql', NOW());
