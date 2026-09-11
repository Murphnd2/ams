-- V100: Census intake (T231 build 1 / D45) — request link, client drop, staged rows
--
-- Two tables. census_request is the one-time link a PSP admin sends the employer's contact: a
-- UUID token, a 30-day expiry, and a state (OPEN | LOADED | REVOKED). census_submission is what
-- the employer's upload became once parsed: whitelisted, parsed ROWS -- never the file. The raw
-- upload is read from the multipart stream and discarded, exactly as CensusUploadServlet does
-- (S26); only name, address and email fields ever reach rows_json (LA-35, LA-41). mapping_json
-- holds header names and field matches only, never a cell value.
--
-- State vocabulary for census_submission: PENDING (readable, awaiting PSP review) | UNREADABLE
-- (required columns missing; header names kept, no rows) | SUPERSEDED (a newer upload arrived) |
-- LOADED | REJECTED (both written by build 2's review page). rows_json is set to NULL whenever a
-- row leaves PENDING, so staged personal data lives only as long as a review is actually pending.
-- reviewed_by / reviewed_at / review_note are created now, written by build 2 -- no second
-- migration is needed for the review page.
--
-- proposal_id follows V098's summit_setup_step exactly: BIGINT NOT NULL, scalar, no foreign key
-- to proposal (V098 declares none; V096's class note explains why a deleted proposal must not
-- take these rows down with it). requested_by / closed_by / reviewed_by are BIGINT to match the
-- user table's primary key (assignee.id) and are nullable -- a session that cannot name anyone
-- is a supported state, not an error (V098's updated_by convention). Timestamps are DATETIME,
-- as in V098/V099.
--
-- NO ROWS ARE INSERTED. Rows are written at runtime by CensusRequestServlet (requests) and
-- CensusDropServlet (submissions). Both tables arrive empty on every installation.
--
-- Reversal: DROP TABLE census_submission, then census_request. Nothing else references either --
-- no view, no inbound FK -- and the setup panel's status fragment (CensusRequestStatusServlet)
-- fails silent on any read error, so an installation that drops them shows no step-3 status line
-- and otherwise keeps working unchanged.

CREATE TABLE IF NOT EXISTS census_request (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    proposal_id   BIGINT        NOT NULL,
    token         CHAR(36)      NOT NULL,
    state         VARCHAR(16)   NOT NULL,
    sent_to       VARCHAR(320)  NULL,
    requested_by  BIGINT        NULL,
    requested_at  DATETIME      NOT NULL,
    expires_at    DATETIME      NOT NULL,
    closed_at     DATETIME      NULL,
    closed_by     BIGINT        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_census_request_token (token),
    KEY ix_census_request_proposal (proposal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS census_submission (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    request_id        BIGINT        NOT NULL,
    submitted_at      DATETIME      NOT NULL,
    original_filename VARCHAR(255)  NULL,
    state             VARCHAR(16)   NOT NULL,
    row_count         INT           NOT NULL DEFAULT 0,
    issue_count       INT           NOT NULL DEFAULT 0,
    mapping_json      TEXT          NULL,
    rows_json         MEDIUMTEXT    NULL,
    reviewed_by       BIGINT        NULL,
    reviewed_at       DATETIME      NULL,
    review_note       VARCHAR(1000) NULL,
    PRIMARY KEY (id),
    KEY ix_census_submission_request (request_id),
    CONSTRAINT fk_census_submission_request
        FOREIGN KEY (request_id) REFERENCES census_request (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V100' AS version, '2026-09-11' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V100', 'Census intake (census_request, census_submission): one-time client upload link with 30-day expiry, parsed whitelisted rows staged for PSP review, raw file never stored (T231 build 1, D45, LA-41)', 'V100__census_intake.sql', NOW());
