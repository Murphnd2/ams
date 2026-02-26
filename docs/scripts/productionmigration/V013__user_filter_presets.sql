-- =============================================================================
-- V013 — User Filter Presets
-- Date: February 25, 2026
-- Prerequisites: None (references user table which has always existed)
--
-- Adds a user_filter_preset table with exactly 3 slots per user.
-- Each slot stores a saved activity filter configuration with a custom label.
-- Slots are seeded for existing users by DatabaseInitializer or on first login.
-- =============================================================================

-- 1. Create table
CREATE TABLE IF NOT EXISTS user_filter_preset (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    slot_number     TINYINT NOT NULL COMMENT '1, 2, or 3',
    label           VARCHAR(16) NOT NULL,
    view_renewal    TINYINT(1) NOT NULL DEFAULT 1,
    view_setup      TINYINT(1) NOT NULL DEFAULT 1,
    view_ticket     TINYINT(1) NOT NULL DEFAULT 1,
    view_opportunity TINYINT(1) NOT NULL DEFAULT 1,
    ownership_filter TINYINT NOT NULL DEFAULT 1 COMMENT '0=All, 1=My+Delegated, 2=Assigned, 3=Delegated',
    attention_filter TINYINT NOT NULL DEFAULT 0 COMMENT '0=ShowAll, 1=NeedsAttention, 2=WaitingOnUs, 3=NeedsContact',
    sort_alphabetically TINYINT(1) NOT NULL DEFAULT 0,
    CONSTRAINT fk_preset_user FOREIGN KEY (user_id) REFERENCES user(person_id) ON DELETE CASCADE,
    CONSTRAINT uq_user_slot UNIQUE (user_id, slot_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Seed 3 default presets for all existing users
-- Slot 1: "My Actionable" (ownership=mine+delegated, attention=needs attention, sort=due date)
-- Slot 2: "All Open" (ownership=all, attention=show all, sort=alpha)
-- Slot 3: "My Renewals" (ownership=mine+delegated, renewals only, attention=show all, sort=alpha)

INSERT IGNORE INTO user_filter_preset (user_id, slot_number, label, view_renewal, view_setup, view_ticket, view_opportunity, ownership_filter, attention_filter, sort_alphabetically)
SELECT u.person_id, 1, 'My Actionable', 1, 1, 1, 1, 1, 1, 0
FROM user u;

INSERT IGNORE INTO user_filter_preset (user_id, slot_number, label, view_renewal, view_setup, view_ticket, view_opportunity, ownership_filter, attention_filter, sort_alphabetically)
SELECT u.person_id, 2, 'All Open', 1, 1, 1, 1, 0, 0, 1
FROM user u;

INSERT IGNORE INTO user_filter_preset (user_id, slot_number, label, view_renewal, view_setup, view_ticket, view_opportunity, ownership_filter, attention_filter, sort_alphabetically)
SELECT u.person_id, 3, 'My Renewals', 1, 0, 0, 0, 1, 0, 1
FROM user u;

-- 3. Self-register in schema_version
INSERT IGNORE INTO schema_version (version, description, script_name)
VALUES ('V013', 'User filter presets - 3 configurable slots per user', 'V013__user_filter_presets.sql');

-- ─── VERIFICATION ────────────────────────────────────────────────────────────
-- SELECT * FROM user_filter_preset ORDER BY user_id, slot_number;
-- SELECT * FROM schema_version WHERE version = 'V013';

-- ─── ROLLBACK (if needed) ────────────────────────────────────────────────────
-- DROP TABLE IF EXISTS user_filter_preset;
-- DELETE FROM schema_version WHERE version = 'V013';
