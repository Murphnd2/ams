-- V094: Employer participant roster — the AMS-owned pre-Summit census
--
-- Creates the first AMS table that enumerates an employer's workforce under AMS-generated
-- keys. Before this, no such table existed: S26-A established that `employee` is a
-- Summit-keyed archive table (assigned `int` PK, no hire or effective date), and that the
-- only AMS-generated named persons -- `assignee`/Person rows -- attach to a prospect as
-- one-or-a-few decision-maker contacts, never as a workforce. This table fills that gap so
-- Summit files 4 (Demographics) and 5 (HRA Enrollment) have a source; neither file is
-- emitted by the run that creates this table.
--
-- Keyed to `prospect`, not `employer`. S26-A established that nothing in the codebase links
-- a Prospect to an Employer -- the two employer identities never join -- and that Prospect
-- is the only employer-shaped entity with an AMS-generated PK (`prospect_id BIGINT`,
-- `@GeneratedValue`). The roster is loaded at sales/setup time, before any Summit import
-- exists to produce an `employer` row, so `prospect` is the only FK target available.
--
-- `id` is the opaque immutable participant key. The Summit `Participant TPA Custom ID` is
-- DERIVED from it at emit time as `{SUMMIT_TPA_ID_PREFIX}-P-{id}` and is deliberately NOT
-- stored -- the same rule the employer key already follows (LA-29/LA-32), extended to
-- participants by LA-33. Do not add a column for it: a stored copy is a second source of
-- truth that can drift from the derivation.
--
-- NO unique constraint on any name or address combination, deliberately. Two employees can
-- legitimately share a name, and a household shares an address -- spouses working for the
-- same employer are the common case, not the edge case. A uniqueness constraint here would
-- reject valid rosters.
--
-- Columns deliberately ABSENT: SSN, date of birth, and compensation. Summit's Demographics
-- import requires none of them, and they are excluded at the parser as well as here, so an
-- employer file carrying them drops them silently (LA-35). `email` is nullable and collected
-- only when the employer supplies it.
--
-- `effective_date` is NOT NULL and comes from a single form field applied to every row, not
-- from a spreadsheet column -- which is what lets it be the guaranteed-populated trailing
-- column in file 4's emitted order (see the trailing-column rule in
-- docs/business/summit_data_exchange.md).
--
-- Reversal: DROP TABLE. Nothing references it -- no view, no FK inbound, no other migration.

CREATE TABLE employer_participant (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    prospect_id       BIGINT        NOT NULL,
    first_name        VARCHAR(100)  NOT NULL,
    last_name         VARCHAR(100)  NOT NULL,
    address_line1     VARCHAR(200)  NOT NULL,
    address_line2     VARCHAR(200)  NULL,
    city              VARCHAR(100)  NOT NULL,
    state             VARCHAR(2)    NOT NULL,
    postal_code       VARCHAR(10)   NOT NULL,
    email             VARCHAR(255)  NULL,
    effective_date    DATE          NOT NULL,
    created_at        DATETIME      NOT NULL,
    created_by        VARCHAR(100)  NULL,
    PRIMARY KEY (id),
    INDEX idx_employer_participant_prospect (prospect_id),
    CONSTRAINT fk_employer_participant_prospect
        FOREIGN KEY (prospect_id) REFERENCES prospect (prospect_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V094' AS version, '2026-09-07' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V094', 'Employer participant roster (employer_participant): AMS-owned pre-Summit census keyed to prospect, no name/address uniqueness, no SSN/DOB/compensation columns', 'V094__employer_participant.sql', NOW());
