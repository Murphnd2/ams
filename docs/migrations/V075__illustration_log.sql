-- =====================================================================
-- V075__illustration_log.sql
-- =====================================================================
-- Purpose : Audit/analytics log for the A1 ICHRA rating illustration --
--           one row per illustration run, attributing it to an agent and
--           agency. No Employee, Prospect, or Employer row is created by
--           an illustration.
--
-- Notes   : This table holds NO PII. result_summary is a short computed
--           descriptor such as "range 421.57-1368.03, 65 plans, 3
--           carriers" -- never a name, never an employer identifier.
--           agent_person_id references assignee(id), NOT a "person"
--           table -- Person is SINGLE_TABLE inheritance rooted at
--           Assignee, so any FK to a Person must target assignee(id).
--           This has bitten prior migrations (V060, V061).
--           agency_id / parent_agency_id reference agency(agency_id) --
--           the agency table's PK column is agency_id, not id (see
--           Agency.java's @Column(name = "agency_id") on the @Id field).
--           parent_agency_id is deliberately denormalized (no GA
--           hierarchy-walk helper exists in the codebase; a future
--           pipeline console will want direct rollup access without a
--           self-join).
--
-- Feature : A1 ICHRA rating illustration (Phase B-1b).
--
-- Prereqs : Base schema (V024 baseline), agency table (present since
--           baseline). No prior migration beyond baseline required.
--
-- Scope   : SSA production instance only.
--
-- Rollback: DROP TABLE illustration_log;
-- =====================================================================

CREATE TABLE illustration_log (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    created_at          DATETIME     NOT NULL,
    agent_person_id     BIGINT       NULL,
    agency_id           BIGINT       NULL,
    parent_agency_id    BIGINT       NULL,
    zip_code            VARCHAR(10)  NULL,
    county_fips         CHAR(5)      NULL,
    state               CHAR(2)      NULL,
    plan_year           SMALLINT     NULL,
    eligible_headcount  INT          NULL,
    mode                VARCHAR(16)  NULL,
    cache_hit           TINYINT(1)   NULL,
    result_summary      VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_illustration_log_agency_created (agency_id, created_at),
    KEY idx_illustration_log_created (created_at),
    CONSTRAINT fk_illustration_log_agent
        FOREIGN KEY (agent_person_id) REFERENCES assignee(id),
    CONSTRAINT fk_illustration_log_agency
        FOREIGN KEY (agency_id) REFERENCES agency(agency_id),
    CONSTRAINT fk_illustration_log_parent_agency
        FOREIGN KEY (parent_agency_id) REFERENCES agency(agency_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V075' AS version, '2026-07-30' AS updated;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V075', 'Illustration log for A1 ICHRA rating illustration (no PII)', 'V075__illustration_log.sql', NOW());
