-- V099: Audit framework run log (T237) — counts-only, no personal data
--
-- One row per (check, run). Findings are self-clearing: a run records only a count, a status
-- and a scrubbed one-line summary/error -- never the rows a check found. Detail pages
-- (e.g. AuditIchraUncoded) read their source live, in-request, and store nothing (LA-40).
--
-- Reversal: DROP TABLE. Nothing references it -- no view, no inbound FK -- and AuditService's
-- constructor never throws on a missing table (logs WARN, starts with an empty in-memory map).

CREATE TABLE IF NOT EXISTS audit_run (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    psp_id         BIGINT        NULL,
    check_key      VARCHAR(50)   NOT NULL,
    run_at         DATETIME      NOT NULL,
    run_trigger    VARCHAR(10)   NOT NULL,
    status         VARCHAR(16)   NOT NULL,
    finding_count  INT           NOT NULL,
    summary        VARCHAR(500)  NULL,
    error          VARCHAR(500)  NULL,
    duration_ms    INT           NULL,
    PRIMARY KEY (id),
    INDEX idx_audit_run_latest (psp_id, check_key, run_at),
    CONSTRAINT fk_audit_run_psp FOREIGN KEY (psp_id) REFERENCES assignee (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V099' AS version, '2026-09-10' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V099', 'Audit framework run log (audit_run), counts only, no personal data (T237)', 'V099__audit_run.sql', NOW());
