-- V088: Employer contribution capture on the plus-tier intake (T80 half 1)
--
-- Adds the employer's own stated monthly-per-employee ICHRA contribution to
-- proposal_ichra_intake (V087). This is INPUT the agent collects, exactly like
-- zip/county_fips/headcount on that table -- not a computed or market-derived
-- figure, and it carries no relationship to proposal_ichra_snapshot (V079) or
-- rating_area_rate_cache (V074) whatsoever. T80 half 2 (all-in cost including
-- SSA administration fees) is a separate, unscoped follow-on that will read
-- pricing/fee data this column does not touch.
--
-- Nullable, deliberately: the field is optional at intake, and every existing
-- proposal_ichra_intake row (written before this column existed) must remain
-- valid with no backfill. An unanswered value produces an omitted cost block
-- on the proposal, not an error -- see ViewProposal.buildTokenMap's ICHRA_
-- CONTRIBUTION_* tokens, which resolve to empty string as a group when this
-- column is null.
--
-- Reversal: DROP COLUMN. No other table or view depends on this column.

ALTER TABLE proposal_ichra_intake
    ADD COLUMN monthly_contribution_per_employee DECIMAL(10,2) NULL
        AFTER headcount;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V088' AS version, '2026-08-03' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V088', 'Employer monthly contribution per employee on proposal_ichra_intake (T80 half 1, nullable, no backfill)', 'V088__proposal_ichra_intake_contribution.sql', NOW());
