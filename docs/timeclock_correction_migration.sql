-- =============================================================================
-- Timeclock Correction Request — Production Migration
-- Date: February 22, 2026
-- Target: beta_ssa schema (local and production)
--
-- Creates the time_correction_request table for employee time correction
-- request workflow. Employees submit correction requests; PSP admins
-- approve or deny them.
--
-- No prerequisites beyond the existing timelog and assignee tables.
-- =============================================================================

-- ─── 1. Create time_correction_request table ────────────────────────────────

CREATE TABLE time_correction_request (
    request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requestor_id BIGINT NOT NULL,
    in_log_id BIGINT NOT NULL,
    out_log_id BIGINT NOT NULL,
    original_date DATE NOT NULL,
    original_in_time TIME NOT NULL,
    original_out_time TIME NOT NULL,
    requested_in_time TIME NULL,
    requested_out_time TIME NULL,
    correction_note VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    date_requested TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewer_id BIGINT NULL,
    review_comment VARCHAR(500) NULL,
    date_reviewed TIMESTAMP NULL,
    FOREIGN KEY (requestor_id) REFERENCES assignee(id),
    FOREIGN KEY (reviewer_id) REFERENCES assignee(id),
    FOREIGN KEY (in_log_id) REFERENCES timelog(log_id),
    FOREIGN KEY (out_log_id) REFERENCES timelog(log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ─── 2. Indexes for common queries ─────────────────────────────────────────

CREATE INDEX idx_tcr_status ON time_correction_request (status);
CREATE INDEX idx_tcr_requestor ON time_correction_request (requestor_id);
CREATE INDEX idx_tcr_date ON time_correction_request (original_date);

-- ─── VERIFICATION ───────────────────────────────────────────────────────────

DESCRIBE time_correction_request;
SELECT COUNT(*) AS row_count FROM time_correction_request;
