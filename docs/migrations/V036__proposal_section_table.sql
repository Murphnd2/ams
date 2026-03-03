-- =============================================================================
-- V036 — Proposal section table for composable proposal content
-- Creates the proposal_section table to define ordered, configurable sections
-- per PSP: TITLE, PRICING, FEATURES, CLOSING, and CUSTOM HTML pages.
-- =============================================================================

CREATE TABLE proposal_section (
    section_id      BIGINT AUTO_INCREMENT PRIMARY KEY,
    psp_id          BIGINT NOT NULL,
    section_type    VARCHAR(20) NOT NULL,
    title           VARCHAR(200) DEFAULT NULL,
    html_content    TEXT DEFAULT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    date_created    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modified   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ps_psp FOREIGN KEY (psp_id) REFERENCES assignee(id),
    INDEX idx_ps_psp_order (psp_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Self-register migration
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V036', 'Proposal section table for composable proposal content', 'V036__proposal_section_table.sql', NOW());
