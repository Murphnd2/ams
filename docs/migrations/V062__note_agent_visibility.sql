-- V062: Per-note agent visibility override
--
-- Adds a single nullable flag to the `note` table that controls whether
-- an individual note is visible to agents in the agent portal.
--
-- Resolution semantics (implemented in Java):
--   agent_visible IS NULL  → fall back to PSP default
--                            (constant key NOTES_AGENT_VISIBLE_DEFAULT, default 0)
--   agent_visible = 0      → hidden from agent portal
--   agent_visible = 1      → visible to agent portal
--
-- Default PSP policy is hidden: PSP explicitly opts notes in to agent visibility.
-- Agents creating their own notes (nudges, follow-ups) always write agent_visible=1
-- and those notes are always visible to the authoring agent regardless of setting.
--
-- No FK to constant table — the PSP default is read from the existing
-- per-PSP constant key/value store via AppConstantDAO, consistent with
-- other feature toggles (USE_TIMECLOCK, USE_FRIENDLY_NAMES, etc.).

-- ----------------------------------------------------------------------
-- 1. Add nullable override column to note
-- ----------------------------------------------------------------------
ALTER TABLE note ADD COLUMN agent_visible TINYINT(1) NULL;

-- ----------------------------------------------------------------------
-- 2. Index to support the agent-portal note query
--    (WHERE activity_id = ? AND (agent_visible = 1 OR (agent_visible IS NULL AND <default>)))
-- ----------------------------------------------------------------------
CREATE INDEX idx_note_agent_visible ON note(agent_visible);

-- ----------------------------------------------------------------------
-- 3. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V062' AS version, '2026-04-23' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V062', 'Per-note agent visibility override', 'V062__note_agent_visibility.sql', NOW());
