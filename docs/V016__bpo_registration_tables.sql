-- =============================================================================
-- V016: BPO Registration & Assignment Tables
-- Date: February 25, 2026
-- Prerequisites: V011 (BPO delegation feature)
--
-- These tables were created during BPO concept development to support a
-- registration/approval workflow between PSPs and BPO organizations:
--   - bpo_registration: BPO companies that register to offer services to PSPs
--   - bpo_psp_assignment: Links BPO users to specific PSPs they are approved to work with
--
-- Tables already exist on dev beta_ssa (created ad-hoc during development).
-- CREATE TABLE IF NOT EXISTS ensures this script is safe on all environments.
-- =============================================================================

-- ─── 1. BPO Registration table ─────────────────────────────────────────────
-- Tracks BPO organizations that register to provide services

CREATE TABLE IF NOT EXISTS bpo_registration (
    bpo_reg_id BIGINT NOT NULL AUTO_INCREMENT,
    psp_id BIGINT NOT NULL,
    bpo_name VARCHAR(100) NOT NULL,
    bpo_url VARCHAR(255) DEFAULT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    date_registered DATE NOT NULL,
    PRIMARY KEY (bpo_reg_id),
    KEY FK_BPOREG_psp (psp_id),
    CONSTRAINT FK_BPOREG_psp FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ─── 2. BPO-PSP Assignment table ───────────────────────────────────────────
-- Links individual BPO users to the PSPs they are approved to work with

CREATE TABLE IF NOT EXISTS bpo_psp_assignment (
    assignment_id BIGINT NOT NULL AUTO_INCREMENT,
    bpo_user_id BIGINT NOT NULL,
    psp_id BIGINT NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    date_assigned DATE NOT NULL,
    PRIMARY KEY (assignment_id),
    UNIQUE KEY uq_bpo_psp_user (bpo_user_id, psp_id),
    KEY FK_BPOPSP_psp (psp_id),
    CONSTRAINT FK_BPOPSP_psp FOREIGN KEY (psp_id) REFERENCES assignee(id),
    CONSTRAINT FK_BPOPSP_user FOREIGN KEY (bpo_user_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ─── Self-register in schema_version ─────────────────────────────────────────

INSERT IGNORE INTO schema_version (version, description, script_name)
VALUES ('V016', 'BPO registration and PSP assignment tables', 'V016__bpo_registration_tables.sql');

-- ─── VERIFICATION ────────────────────────────────────────────────────────────
-- DESCRIBE bpo_registration;
-- DESCRIBE bpo_psp_assignment;
-- SELECT * FROM schema_version WHERE version = 'V016';
