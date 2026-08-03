-- V087: Plus-tier intake captured in the Proposal Builder (proposal_ichra_intake)
--
-- The consumer V086 was written for. When an ICHRA-entitled agent selects an LOS carrying
-- los.is_plus_tier, the builder interjects a ZIP + county + headcount panel before Create
-- Proposal; this table records what was collected. One row per proposal, or none.
--
-- ⚠️ NOT the same thing as proposal_ichra_snapshot (V079). That table records COMPUTED RATE
-- OUTPUT and is legitimately NOT NULL on county_fips/state/county_name/plan_year/source_env,
-- because ViewProposal fails closed unless source_env='PRODUCTION'. Intake is INPUT: it must
-- persist for a county with no warmed rates at all, which is the majority of Texas counties
-- today. Widening V079's NOT NULLs to hold intake would destroy the compliance invariant
-- ViewProposal depends on. Two tables, deliberately. A proposal may carry either, both, or
-- neither, and they are written by independent best-effort paths.
--
-- Scope of the data: employer ZIP, resolved county, and eligible-employee headcount, entered
-- by an AGENT about a PROSPECT EMPLOYER. No employee-level data, no PHI, no SSN, no
-- individual identifiers of any kind. Nothing a natural person enters about themselves.
--
-- zip is stored alongside county_fips rather than discarded because a ZIP resolving to
-- several counties is the common case (34% of TX ZCTAs) -- keeping the ZIP the agent actually
-- typed makes a later wrong-county diagnosis possible. county_name/state are denormalized for
-- the same reason V079 denormalizes them: the render path must never join county_reference.
--
-- ⚠️ plan_year is a DEVIATION FROM THE S10-A SPEC (docs/analysis/phase_a_builder_interjection.md),
-- added by the S10-B build prompt's HS-1 decision: the agent is asked for ZIP and headcount
-- only, never plan year -- a third field on an agent's first contact with this feature was
-- judged a worse trade than the precision it buys. plan_year is DERIVED server-side (the
-- first entry of RATE_CACHE_PLAN_YEARS, the same source IllustrationServlet.resolvePlanYear()
-- reads) and stored so a later reader (T126) knows what year the ZIP/county were interpreted
-- against -- storing a derived value is not collecting it. If RATE_CACHE_PLAN_YEARS is
-- unconfigured at write time, ProposalBuilder writes no intake row at all (fails closed,
-- matching every other validation on this path) rather than a placeholder year, so this
-- column is never actually NULL in a written row despite carrying no default.
--
-- Reversal: DROP TABLE. The proposal table is untouched, so nothing on the hot core entity
-- has to be rolled back and no existing query plan changes.
--
-- Prerequisites: V079 (sibling shape reference only, not a hard dependency), V084/V085
-- (zip_county crosswalk -- without it the panel's county chooser returns empty, which is a
-- handled state, not a failure), V086 (los.is_plus_tier -- the trigger).

-- ----------------------------------------------------------------------
-- 1. proposal_ichra_intake -- one row per proposal, or none
-- ----------------------------------------------------------------------
CREATE TABLE proposal_ichra_intake (
    intake_id     BIGINT        NOT NULL AUTO_INCREMENT,
    proposal_id   BIGINT        NOT NULL,
    zip           CHAR(5)       NOT NULL,                 -- as typed by the agent, five digits
    county_fips   CHAR(5)       NOT NULL,                 -- the county the agent CHOSE, never auto-picked
    county_name   VARCHAR(100)  NOT NULL,                 -- denormalized; render never joins county_reference
    state         CHAR(2)       NOT NULL,
    headcount     SMALLINT      NOT NULL,                 -- eligible employees, 1..10000
    plan_year     SMALLINT      NOT NULL,                 -- DERIVED, not agent-asserted -- see note above (S10-B HS-1)
    collected_at  DATETIME      NOT NULL,
    created_by    BIGINT        NULL,                     -- FK -> assignee(id); Person is SINGLE_TABLE under assignee
    PRIMARY KEY (intake_id),
    UNIQUE KEY uq_pii_proposal (proposal_id),
    CONSTRAINT fk_pii_proposal FOREIGN KEY (proposal_id) REFERENCES proposal(proposal_id) ON DELETE CASCADE,
    CONSTRAINT fk_pii_created_by FOREIGN KEY (created_by) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V087' AS version, '2026-08-03' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V087', 'Plus-tier ZIP/county/headcount intake captured in the Proposal Builder (proposal_ichra_intake, plan_year derived not agent-asserted)', 'V087__proposal_ichra_intake.sql', NOW());
