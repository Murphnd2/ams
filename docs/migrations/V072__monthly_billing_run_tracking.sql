-- =====================================================================
-- V072__monthly_billing_run_tracking.sql
-- =====================================================================
-- Purpose : Status tracking for the Monthly Billing Launcher (one-click
--           import + billing pipeline). Persists one row per launch plus
--           one child row per pipeline step, so the launcher page can poll
--           progress and a final verification summary after the long-running
--           CreateBilling step outlives its HTTP request.
--
-- Feature : Monthly Billing Launcher (SSA-internal; legacy Importer path).
--           See DESIGN_monthly_billing_launcher.md sections 5, 8, 10.
--
-- Prereqs : Base schema (assignee table, present since the V024 baseline).
--           No prior migration beyond baseline is required.
--
-- Scope   : SSA production instance only (Demo/BPO/Master decommissioned).
--
-- Rollback: DROP TABLE billing_run_step; DROP TABLE billing_run;
-- =====================================================================

CREATE TABLE billing_run (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    started_at          DATETIME     NOT NULL,
    completed_at        DATETIME     NULL,
    status              VARCHAR(16)  NOT NULL,            -- RUNNING / COMPLETED / FAILED
    current_step        VARCHAR(40)  NULL,               -- live step label (incl. billing sub-step)
    mode                VARCHAR(16)  NOT NULL,            -- FULL / BILLING_ONLY
    plan_type_supplied  TINYINT(1)   NOT NULL DEFAULT 0, -- whether a Plan Type file was included
    renewals_refreshed  TINYINT(1)   NOT NULL DEFAULT 0, -- drives the exactly-once renewal refresh
    error_text          TEXT         NULL,
    launched_by         BIGINT       NULL,               -- operator (Person -> assignee.id)
    PRIMARY KEY (id),
    KEY idx_billing_run_started (started_at),
    CONSTRAINT fk_billing_run_launched_by
        FOREIGN KEY (launched_by) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE billing_run_step (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    run_id        BIGINT       NOT NULL,
    step_name     VARCHAR(40)  NOT NULL,   -- WIPE / IMPORT / PROMOTE / CLEAR_BILLING / CREATE_BILLING
    status        VARCHAR(16)  NOT NULL,   -- PENDING / RUNNING / COMPLETED / FAILED / SKIPPED
    started_at    DATETIME     NULL,
    completed_at  DATETIME     NULL,
    detail        VARCHAR(255) NULL,       -- row counts / files matched / benefits skipped
    error_text    TEXT         NULL,
    PRIMARY KEY (id),
    KEY idx_billing_run_step_run (run_id),
    CONSTRAINT fk_billing_run_step_run
        FOREIGN KEY (run_id) REFERENCES billing_run (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V072' AS version, '2026-07-16' AS updated;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V072', 'Monthly billing run tracking: billing_run + billing_run_step', 'V072__monthly_billing_run_tracking.sql', NOW());
