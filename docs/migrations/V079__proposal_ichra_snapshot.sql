-- V079: ICHRA illustration snapshot on a proposal (build-plan item 6)
--
-- S2 overturned from re-derive to snapshot (Phase A finding, 2026-07-31): there is no
-- per-proposal section content table anywhere in this schema -- every existing
-- ProposalSection is either static template HTML with merge tokens, or reads data the
-- Proposal graph already owns (SalesDAO.getPricingWithAdjustments). Re-derive would
-- have needed a parameters table too, so it was never the cheaper path. A snapshot
-- additionally buys:
--   1. Provenance that belongs to the document. Item 7 gates proposal CREATION on
--      source_env = 'PRODUCTION'; under re-derive, a later re-warm against staging
--      would silently rewrite every already-sent proposal on a public page with no
--      banner. A snapshot cannot be rewritten.
--   2. No rate-cache read on an unauthenticated endpoint (/proposal/* is public per
--      LoginFilter, and nothing in the proposal render path touches the cache today).
--   3. The client and the agent look at the same numbers.
--
-- NOT a scenario system. One design per proposal, enforced by proposal_id UNIQUE.
-- N scenarios later costs dropping that index and adding is_selected -- headroom, not
-- built now.
--
-- No affordability column of any kind, deliberately -- no flip contribution, no
-- income, no PTC status, no onex_lcsp_premium. The proposal URL is public,
-- unauthenticated and forwardable to an employee (LA-12); this schema is structurally
-- incapable of carrying an affordability figure onto that surface.
--
-- county_name is denormalized on purpose -- the public render must never join
-- county_reference.
--
-- DECIMAL precision on every premium/dollar column copied exactly from
-- rating_area_rate_cache (V074): DECIMAL(8,2) for per-employee-scale figures,
-- DECIMAL(10,2) for group-scale totals -- matching V074's own market_low_premium /
-- market_high_premium distinction in spirit (per-employee vs. group-scaled amounts).
--
-- Prerequisites: V074 (rating_area_rate_cache), proposal table (base schema).

-- ----------------------------------------------------------------------
-- 1. proposal_ichra_snapshot -- one row per proposal
-- ----------------------------------------------------------------------
CREATE TABLE proposal_ichra_snapshot (
    snapshot_id         BIGINT        NOT NULL AUTO_INCREMENT,
    proposal_id         BIGINT        NOT NULL,
    mode                VARCHAR(16)   NOT NULL,               -- RANGE / AGE_BAND
    county_fips         CHAR(5)       NOT NULL,
    state               CHAR(2)       NOT NULL,
    county_name         VARCHAR(100)  NOT NULL,                -- denormalized; public render never joins county_reference
    plan_year           SMALLINT      NOT NULL,
    contribution        DECIMAL(8,2)  NULL,                    -- AGE_BAND employer monthly contribution
    headcount           SMALLINT      NULL,                    -- RANGE eligible-employee count
    group_monthly_low   DECIMAL(10,2) NULL,                    -- RANGE output
    group_monthly_high  DECIMAL(10,2) NULL,                    -- RANGE output
    group_net_total     DECIMAL(10,2) NULL,                    -- AGE_BAND output
    employer_outlay     DECIMAL(10,2) NULL,                    -- AGE_BAND output
    source_env          VARCHAR(16)   NOT NULL,                -- STAGING / PRODUCTION -- ViewProposal fails closed unless this is PRODUCTION
    rates_fetched_at    DATETIME      NULL,
    snapshot_at         DATETIME      NOT NULL,
    created_by          BIGINT        NULL,                    -- FK -> assignee(id); Person is SINGLE_TABLE under assignee, not a person table
    PRIMARY KEY (snapshot_id),
    UNIQUE KEY uq_pis_proposal (proposal_id),
    CONSTRAINT fk_pis_proposal FOREIGN KEY (proposal_id) REFERENCES proposal(proposal_id) ON DELETE CASCADE,
    CONSTRAINT fk_pis_created_by FOREIGN KEY (created_by) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------
-- 2. proposal_ichra_snapshot_band -- AGE_BAND only; zero rows for a RANGE snapshot
-- ----------------------------------------------------------------------
CREATE TABLE proposal_ichra_snapshot_band (
    band_id           BIGINT        NOT NULL AUTO_INCREMENT,
    snapshot_id       BIGINT        NOT NULL,
    age               TINYINT       NOT NULL,
    lives             SMALLINT      NOT NULL,
    floor_premium     DECIMAL(8,2)  NOT NULL,
    net_per_employee  DECIMAL(8,2)  NOT NULL,
    band_net          DECIMAL(10,2) NOT NULL,
    sort_order        SMALLINT      NOT NULL,
    PRIMARY KEY (band_id),
    CONSTRAINT fk_pisb_snapshot FOREIGN KEY (snapshot_id) REFERENCES proposal_ichra_snapshot(snapshot_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------
-- 3. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V079' AS version, '2026-07-31' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V079', 'ICHRA illustration snapshot on a proposal (proposal_ichra_snapshot + proposal_ichra_snapshot_band)', 'V079__proposal_ichra_snapshot.sql', NOW());
