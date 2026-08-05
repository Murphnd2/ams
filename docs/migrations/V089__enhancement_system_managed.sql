-- V089: System-managed classification flag on an enhancement (enhancement.system_managed)
--
-- Marks an enhancement whose proposal-section visibility is decided per-proposal by
-- FlaggedEnhancementResolver rather than by scope membership alone. See
-- docs/analysis/S18C_proposal_detail_render_spec.md §2.
--
-- NOT NULL DEFAULT 0 — mirrors los.is_plus_tier (V086), agency.markup_enabled (V067),
-- agency.ichra_enabled (V077) and enhancement.suppressed itself. No backfill: every
-- existing row comes out UNFLAGGED, so nothing anywhere changes when this is applied.
-- FlaggedEnhancementResolver returns true immediately for an unflagged enhancement, so
-- the ViewProposal scope filter reduces to its current condition exactly.
--
-- Naming: system_managed rather than is_system_managed or system_managed_enabled. This
-- is a classification of what an enhancement IS (cf. V086's reasoning), but the
-- enhancement table's existing boolean is bare `suppressed`, not `is_suppressed` — this
-- column matches its own table's established form rather than another table's.
--
-- Why a column on enhancement rather than a constant row holding enhancement ids: the
-- constant table has PRIMARY KEY (name) and NO psp_id, so it is global per installation,
-- whereas enhancement.psp_id is per-PSP. One constant cannot name the correct rows for
-- every PSP on a multi-PSP installation. Same reasoning recorded in V086 lines 24-28.
--
-- Seeds nothing. No INSERT INTO constant. No index: neither enhancement.suppressed nor
-- los.is_plus_tier carries one, and this column is read per already-loaded entity, never
-- queried on.
--
-- Prerequisites: none.

-- ----------------------------------------------------------------------
-- 1. Add system_managed to enhancement
-- ----------------------------------------------------------------------
ALTER TABLE enhancement
    ADD COLUMN system_managed TINYINT(1) NOT NULL DEFAULT 0;

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V089' AS version, '2026-08-05' AS updated;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V089', 'System-managed classification flag on enhancement (enhancement.system_managed, default OFF)', 'V089__enhancement_system_managed.sql', NOW());
