-- V086: Plus-tier classification flag on a line of service (los.is_plus_tier)
--
-- Marks an LOS as requiring additional intake — ZIP and headcount — before a proposal
-- carrying it can be created. Kevin's design, settled 2026-08-02: the flag is a WORKFLOW
-- TRIGGER on the LOS, not a visibility marker on a proposal section.
--
-- ⚠️ NOTHING READS THIS COLUMN. This migration and its accompanying build make the flag
-- exist and do nothing with it. No rendering changes, no proposal-builder changes, no
-- resolver changes. It is a gated capability with no consumer, deliberately: the builder
-- interjection that will read it is a separate piece of work with its own Phase A.
-- Reversal today is a display edit plus DROP COLUMN, because no behaviour depends on it.
--
-- NOT NULL DEFAULT 0 — mirrors the shape of los.suppressed, agency.markup_enabled (V067)
-- and agency.ichra_enabled (V077). No backfill: every existing row comes out unflagged, so
-- nothing anywhere changes when this is applied. Marking an LOS plus-tier is a deliberate
-- per-row admin action through the Service Manager, not something this migration decides.
--
-- Naming: is_plus_tier rather than plus_tier_enabled. This is a CLASSIFICATION of what a
-- line of service IS, not an enablement toggle for a capability — the *_enabled suffix on
-- agency belongs to flags that switch a feature on for a tenant. proposal_section.is_active
-- and proposal.is_inactive establish the is_* form in the same domain area. This also matches
-- the column name already recorded against T122 in the backlog.
--
-- Why a column on los rather than a constant row holding LOS ids (the alternative weighed in
-- the S9-D probe): the constant table has PRIMARY KEY (name) and NO psp_id, so it is global
-- per installation, whereas los.psp_id is per-PSP. One constant cannot name the correct LOS
-- rows for every PSP on a multi-PSP installation, and AMS is multi-PSP by design. A column on
-- los is PSP-scoped by construction because each los row already belongs to a PSP.
--
-- Seeds nothing. No INSERT INTO constant — constant rows are D-NN deployment items or go
-- through DatabaseInitializer, never a migration. No index: neither los.suppressed nor
-- agency.ichra_enabled carries one, and nothing queries on this column at all yet.
--
-- Prerequisites: none.

-- ----------------------------------------------------------------------
-- 1. Add is_plus_tier to los
-- ----------------------------------------------------------------------
ALTER TABLE los
    ADD COLUMN is_plus_tier TINYINT(1) NOT NULL DEFAULT 0;

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V086' AS version, '2026-08-02' AS updated;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V086', 'Plus-tier classification flag on line of service (los.is_plus_tier, default OFF, no consumer yet)', 'V086__los_plus_tier.sql', NOW());
