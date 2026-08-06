-- V091: The four ICHRA proposal sections -- selection, the section discriminator, #3's
-- employer-supplied comparison inputs, and the band NOT NULL correction.
--
-- S20-B. Three unrelated-looking changes in one script because they are one feature: the
-- agent's selection of which ICHRA content a proposal carries. See
-- docs/analysis/S20A_ichra_sections_spec.md.
--
-- ----------------------------------------------------------------------
-- 1. enhancement.system_section_key -- the Rule-4-compliant discriminator
-- ----------------------------------------------------------------------
-- V089 gave FlaggedEnhancementResolver a flag (system_managed) but no way to tell WHICH
-- of the four sections a flagged enhancement is. This column is that answer, and it is a
-- column on enhancement for exactly the reason V089's own header gives for system_managed:
-- the constant table has PRIMARY KEY (name) and no psp_id, so it is global per
-- installation, whereas enhancement.psp_id is per-PSP. One constant cannot name the
-- correct rows for every PSP. A hardcoded enhancement id in Java would not survive a
-- second installation at all.
--
-- NULLABLE, no default, no backfill, no CHECK constraint and no enum: every existing row
-- reads NULL, and the resolver treats a flagged-but-unkeyed enhancement as withheld
-- (half-configured is not configured). The admissible values are the four literals below;
-- they are enforced by the admin UI's fixed dropdown, not by the column, matching this
-- schema's established practice (proposal_section.scope and section_type are both bare
-- VARCHARs with no CHECK).
--
--   ICHRA_MARKET         -- market illustration data (the base layer)
--   ICHRA_CONTRIBUTION   -- contribution scenarios
--   ICHRA_COMPARISON     -- comparison against their current group plan
--   ICHRA_AFFORDABILITY  -- affordability comparison
--
-- Seeds nothing. No INSERT INTO constant. No enhancement row is created or flagged by this
-- script -- a PSP admin flags and keys its own four in the Service Manager.
--
-- No index: mirrors V089's own reasoning -- read per already-loaded entity, never queried on.
-- (systemManagedIdsForProposal filters on system_managed, not on this column.)

ALTER TABLE enhancement
    ADD COLUMN system_section_key VARCHAR(32) NULL AFTER system_managed;

-- ----------------------------------------------------------------------
-- 2. proposal_ichra_intake -- the four selections
-- ----------------------------------------------------------------------
-- INPUT, so it belongs here rather than only in payload_json, for the reason V087's own
-- header states: intake must persist for a county with no warmed rates at all, which is
-- the majority of Texas counties today. proposal_ichra_snapshot is written only when
-- PRODUCTION-sourced rates exist, so a selection recorded only in the payload is lost
-- entirely for that county -- with no record of what the agent asked for.
--
-- Four named booleans rather than one CSV column: matches los.is_plus_tier (V086),
-- agency.markup_enabled (V067) and enhancement.system_managed (V089), and avoids the
-- substring-matching trap V045's comma-separated selected_enhancement_ids created on
-- application (where "5" matches inside "15" without comma-padding).
--
-- TINYINT(1) NOT NULL DEFAULT 0, no backfill: every existing intake row comes out with all
-- four unselected, which is the truth -- those proposals were built before any of this
-- existed and carry no ICHRA section.
--
-- section_market is stored as submitted AND as derived: the servlet sets it true whenever
-- any of the other three is true (spec sec 2 -- #1 is the base layer, not a peer), so this
-- column always reflects what was actually required, never only what was ticked.

ALTER TABLE proposal_ichra_intake
    ADD COLUMN section_market        TINYINT(1) NOT NULL DEFAULT 0 AFTER monthly_contribution_per_employee,
    ADD COLUMN section_contribution  TINYINT(1) NOT NULL DEFAULT 0 AFTER section_market,
    ADD COLUMN section_comparison    TINYINT(1) NOT NULL DEFAULT 0 AFTER section_contribution,
    ADD COLUMN section_affordability TINYINT(1) NOT NULL DEFAULT 0 AFTER section_comparison;

-- ----------------------------------------------------------------------
-- 3. proposal_ichra_intake -- #3's employer-supplied comparison inputs
-- ----------------------------------------------------------------------
-- The employer's CURRENT group plan cost, as the agent reports it. Neither figure has a
-- home anywhere in the schema today (verified against every column of both ICHRA tables,
-- spec sec 1.5). Both are employer-reported inputs, exactly like
-- monthly_contribution_per_employee (V088) -- not computed, not market-derived, and
-- carrying no relationship to rating_area_rate_cache or proposal_ichra_snapshot.
--
-- The third input section 3 needs -- the planned ICHRA/QSEHRA contribution -- is
-- monthly_contribution_per_employee (V088), already here. One field, not two: the figure
-- the employer plans to contribute is the same figure section 2's scenarios use, and
-- storing it twice is how two copies disagree.
--
-- Nullable, no backfill: both are unanswered until the agent selects section 3, and an
-- unanswered value produces sections.ICHRA_COMPARISON.complete = false -- an omitted
-- section, never an error and never a zero standing in for an unknown.
--
-- DECIMAL(10,2) matches monthly_contribution_per_employee (V088). These are whole-group
-- monthly figures rather than per-employee ones, so the wider scale matters: 99,999,999.99
-- covers any group AMS will quote.
--
-- No CHECK that the employer share <= the total. An employer whose figures are unusual
-- would get content silently withheld with no explanation; record what was collected, do
-- not adjudicate it.

ALTER TABLE proposal_ichra_intake
    ADD COLUMN current_total_monthly_premium  DECIMAL(10,2) NULL AFTER section_affordability,
    ADD COLUMN current_employer_monthly_share DECIMAL(10,2) NULL AFTER current_total_monthly_premium;

-- ----------------------------------------------------------------------
-- 4. proposal_ichra_snapshot_band -- the NOT NULL correction
-- ----------------------------------------------------------------------
-- V079 made net_per_employee and band_net NOT NULL. Both are net-of-contribution figures
-- by definition (ProposalBuilder:897-898), so without an employer contribution there is
-- nothing honest to put in either -- and section 1 and section 3 are both available with no
-- contribution at all.
--
-- The live consequence today: attachAgeBandSnapshot:942 writes
--   ProposalIchraSnapshotDAO.save(em, snapshot, contribution != null ? bands : null)
-- so when no contribution was entered the band rows are SILENTLY NOT PERSISTED. Nothing
-- renders wrong (payload_json.ageBands still carries age/lives/premium), but the
-- structured table has a hole in it for exactly the case this feature must support.
--
-- Widening to NULL is strictly permissive: every existing row has a value and keeps it, no
-- backfill is possible or needed, and no reader anywhere assumes non-null (grep: the only
-- consumers are the writer above and ProposalIchraSnapshotDAO's own flat find).
--
-- Reversal: re-tightening to NOT NULL after contribution-less band rows exist would require
-- deleting them or inventing values, so this is one-directional in practice. Accepted --
-- the alternative is a permanent hole in the structured data.

ALTER TABLE proposal_ichra_snapshot_band
    MODIFY COLUMN net_per_employee DECIMAL(8,2)  NULL,
    MODIFY COLUMN band_net         DECIMAL(10,2) NULL;

-- ----------------------------------------------------------------------
-- 5. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V091' AS version, '2026-08-06' AS updated;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V091', 'ICHRA section selection: enhancement.system_section_key, four selection flags and section 3 comparison inputs on proposal_ichra_intake, band net NOT NULL widened', 'V091__ichra_section_selection.sql', NOW());
