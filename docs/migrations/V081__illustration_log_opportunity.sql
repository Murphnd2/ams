-- V081: Optional opportunity attribution on illustration_log (illustration_log.opportunity_id)
--
-- Build-plan item 13 (S3 resolved: yes, nullable, opportunity-level only). Lets an
-- ICHRA illustration or a group-to-ICHRA conversion analysis record which opportunity
-- it was run for, so the pipeline console can show what analysis work has been done on
-- a deal. Deliberately opportunity-level ONLY -- not prospect, not employer, not
-- anything person-shaped. illustration_log holds no PII (see V075's header) and this
-- column does not change that: an opportunity id is a pipeline record, not a person.
--
-- NULLABLE, and null is the normal case. An illustration works with or without an
-- opportunity: /Illustration and /GroupConversion are reachable from the ICHRA hub with
-- no opportunity in hand, and the servlets persist null whenever the parameter is
-- absent, unparseable, resolves to nothing, or falls outside the caller's agency scope
-- (OpportunityAuthz.canAccessOpportunity). The link runs from an opportunity into the
-- tool, never the other way round -- there is no picker and no UI for choosing one.
--
-- BIGINT REFERENCES assignee(id) -- NOT an "opportunity" table. Opportunity extends
-- Activity extends Assignee, and Assignee is @Inheritance(SINGLE_TABLE) with
-- @Id @Column(name = "id") Long, so every Opportunity row lives in `assignee` and any
-- FK to one must target assignee(id). This has bitten prior migrations (V050, V060);
-- V039's fk_qi_activity and V059's fk_ndt_run_activity both get it right and are the
-- precedent followed here.
--
-- FOREIGN KEY constraint declared WITH ON DELETE SET NULL -- V075's three existing
-- nullable FK columns (agent_person_id, agency_id, parent_agency_id) are not comparable
-- precedents here: agencies and people are heavyweight records that are not casually
-- deleted, but opportunities are lightweight pipeline records deleted routinely --
-- duplicates, mis-entries, test data. Default RESTRICT would mean an agent who creates
-- an opportunity, runs an illustration against it, and then deletes the opportunity hits
-- a foreign-key error -- an ICHRA feature changing how the existing sales pipeline
-- behaves for users who never asked for ICHRA, which build rule 1 forbids outright.
-- SET NULL is also the semantically correct choice on its own terms: the column is
-- nullable with null as the normal case, so losing attribution when the deal record is
-- gone is the right outcome, and the telemetry row survives.
--
-- No index is declared. InnoDB creates one implicitly for the foreign key, and V075's
-- own nullable FK columns (agent_person_id, parent_agency_id) carry no explicit index.
--
-- No constant row. Constant rows are D-NN deployment-backlog items, not migration work.
--
-- Prerequisites: V075 (illustration_log).
--
-- Rollback:
--   ALTER TABLE illustration_log DROP FOREIGN KEY fk_illustration_log_opportunity;
--   ALTER TABLE illustration_log DROP COLUMN opportunity_id;

-- ----------------------------------------------------------------------
-- 1. Add opportunity_id to illustration_log
-- ----------------------------------------------------------------------
ALTER TABLE illustration_log
    ADD COLUMN opportunity_id BIGINT NULL,
    ADD CONSTRAINT fk_illustration_log_opportunity
        FOREIGN KEY (opportunity_id) REFERENCES assignee(id) ON DELETE SET NULL;

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V081' AS version, '2026-07-31' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V081',
        'Optional opportunity attribution on illustration_log (illustration_log.opportunity_id, nullable, FK to assignee(id))',
        'V081__illustration_log_opportunity.sql',
        NOW());
