-- V093: Section 125 structure intake inputs on the plus-tier intake
--
-- Adds two agent-entered figures to proposal_ichra_intake (V087) for the prospective
-- Section 125 / micro-ICHRA structure: the employer's flat monthly taxable stipend per
-- employee, and the monthly cost of the alternative coverage an employee could buy
-- instead of an ACA plan. Both are INPUT the agent collects, exactly like
-- monthly_contribution_per_employee (V088) on this same table -- not computed or
-- market-derived figures, and neither carries any relationship to
-- proposal_ichra_snapshot (V079) or rating_area_rate_cache (V074) whatsoever. This run
-- captures the two inputs only; the section that renders them is a separate build item.
--
-- monthly_stipend_per_employee is unconditional taxable wages -- not an ICHRA
-- allowance, and not conditioned on any coverage election. It is not a contribution
-- and must not be summed with monthly_contribution_per_employee anywhere.
--
-- alternative_coverage_monthly_cost carries no carrier identity and no product
-- identity -- it is a number the agent types, nothing more.
--
-- Nullable, deliberately: both fields are optional at intake, and every existing
-- proposal_ichra_intake row (written before these columns existed) must remain valid
-- with no backfill. An unanswered value produces an omitted figure, not an error.
--
-- Reversal: DROP COLUMN, either one independently. No other table or view depends on
-- either column.

ALTER TABLE proposal_ichra_intake
    ADD COLUMN monthly_stipend_per_employee DECIMAL(10,2) NULL
        AFTER monthly_contribution_per_employee;

ALTER TABLE proposal_ichra_intake
    ADD COLUMN alternative_coverage_monthly_cost DECIMAL(10,2) NULL
        AFTER monthly_stipend_per_employee;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V093' AS version, '2026-09-05' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V093', 'Section 125 structure intake inputs on proposal_ichra_intake: monthly_stipend_per_employee and alternative_coverage_monthly_cost (both nullable, no backfill)', 'V093__proposal_ichra_intake_section125.sql', NOW());
