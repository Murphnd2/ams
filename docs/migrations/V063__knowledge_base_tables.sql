-- V063: Knowledge Base tables for AI-grounded knowledge system
--
-- Creates the foundational schema for AMS's DB-backed knowledge system.
-- Three tables are introduced:
--   knowledge_base       — registry of named knowledge bases (DB or JSON-backed)
--   knowledge_chunk      — individual content units within a DB-backed knowledge base
--   knowledge_chunk_history — audit log; chunk_id carries no FK so history
--                             survives hard-deletes of chunks
--
-- Prerequisites:
--   assignee table (SINGLE_TABLE JPA base for Person/PSP/Agency — all FKs here
--   target assignee(id) per the V050 FK-fix convention)
--   schema_version table (present in all environments via baseline dump or
--   schema_version_migration.sql)
--
-- Schema-only — no chunk content is seeded in this migration.
-- Five KB registry rows are seeded (one per logical domain + one JSON pointer):
--   style_voice          — communication style rules, ALWAYS_LOAD into every prompt
--   federal_rules        — Section 125 / FSA / HSA / COBRA / DCAP / ICHRA / HRA / transit
--   ssa_business         — SSA offerings, procedures, fees, escalation chains
--   summit_supplemental  — SSA tribal Summit gotchas and workarounds
--   summit_official      — official DataPath Summit guide (JSON, existing classpath file)
--
-- Content for style_voice and the other DB-backed KBs arrives in subsequent
-- migrations or via the future knowledge-base admin UI.
-- The summit_official row points at the existing summit_guide_indexed.json already
-- served by KnowledgeSearchService; the registry is now the single enumeration
-- point for all knowledge bases (DB and JSON alike).
--
-- No DB triggers — audit logging for knowledge_chunk_history is application-side.
-- The DAO writes a history row before any chunk UPDATE / DELETE / DEACTIVATE.

-- ----------------------------------------------------------------------
-- 1. knowledge_base — KB registry
-- ----------------------------------------------------------------------
CREATE TABLE knowledge_base (
    kb_id           BIGINT          NOT NULL AUTO_INCREMENT,
    kb_key          VARCHAR(50)     NOT NULL,
    label           VARCHAR(100)    NOT NULL,
    description     VARCHAR(500)    DEFAULT NULL,
    source          ENUM('DB','JSON') NOT NULL DEFAULT 'DB',
    json_filename   VARCHAR(100)    DEFAULT NULL,
    reload_strategy ENUM('ALWAYS_LOAD','SEARCH') NOT NULL DEFAULT 'SEARCH',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    sort_order      INT             NOT NULL DEFAULT 100,
    date_created    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    date_modified   TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (kb_id),
    UNIQUE KEY uq_kb_key (kb_key)
);

-- ----------------------------------------------------------------------
-- 2. knowledge_chunk — content units within a DB-backed KB
-- ----------------------------------------------------------------------
CREATE TABLE knowledge_chunk (
    chunk_id        BIGINT          NOT NULL AUTO_INCREMENT,
    kb_id           BIGINT          NOT NULL,
    title           VARCHAR(200)    NOT NULL,
    section         VARCHAR(100)    DEFAULT NULL,
    content         TEXT            NOT NULL,
    keywords        VARCHAR(500)    DEFAULT NULL,
    account_type    VARCHAR(50)     DEFAULT NULL,
    chunk_type      ENUM(
                        'STYLE_RULE',
                        'STYLE_EXAMPLE_GOOD',
                        'STYLE_EXAMPLE_BAD',
                        'FEDERAL_RULE',
                        'FEDERAL_LIMIT',
                        'FEDERAL_DEADLINE',
                        'SSA_OFFERING',
                        'SSA_PROCEDURE',
                        'SSA_PRICING',
                        'SSA_CONTACT',
                        'SUMMIT_HOWTO',
                        'SUMMIT_GOTCHA',
                        'SCENARIO_PLAYBOOK',
                        'ESCALATION_TRIGGER'
                    ) NOT NULL,
    effective_start DATE            DEFAULT NULL,
    effective_end   DATE            DEFAULT NULL,
    source_citation VARCHAR(500)    DEFAULT NULL,
    visibility      ENUM('PUBLIC','INTERNAL','ADMIN_ONLY') NOT NULL DEFAULT 'INTERNAL',
    is_active       TINYINT(1)      NOT NULL DEFAULT 1,
    date_created    TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    date_modified   TIMESTAMP       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    modified_by     BIGINT          DEFAULT NULL,
    PRIMARY KEY (chunk_id),
    INDEX idx_kc_kb_active (kb_id, is_active),
    INDEX idx_kc_type (chunk_type),
    INDEX idx_kc_account_type (account_type),
    INDEX idx_kc_effective (effective_start, effective_end),
    CONSTRAINT fk_kc_kb
        FOREIGN KEY (kb_id) REFERENCES knowledge_base (kb_id),
    CONSTRAINT fk_kc_modified_by
        FOREIGN KEY (modified_by) REFERENCES assignee (id)
);

-- ----------------------------------------------------------------------
-- 3. knowledge_chunk_history — audit log
--    chunk_id intentionally carries no FK — history rows survive
--    hard-deletion of the source chunk.
-- ----------------------------------------------------------------------
CREATE TABLE knowledge_chunk_history (
    history_id      BIGINT          NOT NULL AUTO_INCREMENT,
    chunk_id        BIGINT          NOT NULL,
    title_before    VARCHAR(200)    DEFAULT NULL,
    content_before  TEXT,
    keywords_before VARCHAR(500)    DEFAULT NULL,
    is_active_before TINYINT(1)     DEFAULT NULL,
    change_type     ENUM('UPDATE','DELETE','DEACTIVATE') NOT NULL,
    change_note     VARCHAR(500)    DEFAULT NULL,
    modified_by     BIGINT          DEFAULT NULL,
    modified_on     TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (history_id),
    INDEX idx_kch_chunk (chunk_id, modified_on),
    CONSTRAINT fk_kch_modified_by
        FOREIGN KEY (modified_by) REFERENCES assignee (id)
);

-- ----------------------------------------------------------------------
-- 4. Seed 5 KB registry rows
--    INSERT IGNORE so the migration is idempotent and DatabaseInitializer
--    can seed the same rows on fresh installs without conflict.
-- ----------------------------------------------------------------------
INSERT IGNORE INTO knowledge_base
    (kb_key, label, description, source, json_filename, reload_strategy, is_active, sort_order)
VALUES
    (
        'style_voice',
        'Communication Style & Voice',
        'SSA tone, structure, length, and accuracy rules for all outbound email. Always loaded into every email-drafter prompt.',
        'DB', NULL, 'ALWAYS_LOAD', 1, 10
    ),
    (
        'federal_rules',
        'Federal Benefits Rules',
        'Section 125, FSA, HSA, COBRA, DCAP, transit, ICHRA, HRA rules and limits. Effective-dated where applicable.',
        'DB', NULL, 'SEARCH', 1, 20
    ),
    (
        'ssa_business',
        'SSA Business Knowledge',
        'SSA service offerings, internal procedures, fees, escalation chains, and operational rules.',
        'DB', NULL, 'SEARCH', 1, 30
    ),
    (
        'summit_supplemental',
        'Summit Tribal Knowledge',
        'SSA-discovered Summit gotchas, workarounds, and how-tos not in the official Summit guide.',
        'DB', NULL, 'SEARCH', 1, 40
    ),
    (
        'summit_official',
        'Summit User Guide',
        'Official DataPath Summit user guide. Loaded from JSON; updated by re-crawl.',
        'JSON', 'summit_guide_indexed.json', 'SEARCH', 1, 50
    );

-- ----------------------------------------------------------------------
-- 5. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V063' AS version, '2026-05-04' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V063',
        'Knowledge Base tables (knowledge_base, knowledge_chunk, knowledge_chunk_history) + 5 KB registry rows',
        'V063__knowledge_base_tables.sql',
        NOW());
