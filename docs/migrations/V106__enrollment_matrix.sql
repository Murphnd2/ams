-- V106: enrollment matrix storage -- schema and entity layer only (s52j). No UI, no exporter, no
-- report; those are later builds. Year-1 onboarding of a new client only -- no proration, no
-- mid-year effective dates, no delta handling; none of that is represented here.
--
-- Three tables, one matrix per setup activity (Kevin, this session):
--
--   enrollment_matrix -- one row per setup. Holds the push lock (is_pushed/pushed_by/pushed_at)
--   so the table-wide lock has a single owner rather than a column repeated on every participant
--   row. FK to `assignee(id)`: Setup extends Activity extends Assignee under single-table
--   inheritance (no @Table override on any of the three), the same pattern
--   summit_plan_template_map.psp_id and summit_service_item_flags.psp_id already use for a PSP
--   row -- there is no `setup` table to reference. UNIQUE on setup_id enforces one matrix per
--   setup at the database, not just in application code.
--
--   enrollment_matrix_participant -- the header row, one per participant per matrix. FK to
--   enrollment_matrix and to employer_participant (V094). payroll_frequency is a plain
--   VARCHAR with no default, no CHECK, no ENUM: which global payroll-frequency values are
--   enrollment-approved is Kevin's to designate later and is not decided this run, and building
--   or guessing that list now is explicitly out of scope -- the column is code-validated later,
--   the same way tax_treatment and enrollment_amount_mode are code-validated on
--   summit_plan_template_map rather than being a database ENUM. Two values are already named by
--   the design and are stored the same way, as plain strings, not specially declared here:
--   OTHER_CUSTOM (custom_schedule_name carries a Summit-side schedule name) and
--   OTHER_NOT_IMPORTABLE (none of that participant's entries go in the FTP export).
--   is_entry_locked is a soft, confirm-to-unlock lock, separate from enrollment_matrix's
--   table-wide push lock -- a participant can be locked (entry finished) while the matrix as a
--   whole is still open, and the matrix push lock can close the whole table independent of any
--   one participant's lock state.
--
--   enrollment_matrix_entry -- the detail row, one per participant per enrollment leg. FK to
--   enrollment_matrix_participant and to summit_plan_template_map (the leg; V103's fan-out is
--   exactly what lets one participant hold several entries under one matrix). One `amount`
--   column, not two: monthly premium and annual election are mutually exclusive and both money
--   -- which one it is comes from the leg's own enrollment_amount_mode (V105), not from a second
--   column here. tier_name is separate because a tier is a selection, not a figure, and under
--   TIER mode no amount is keyed at all (the HRA setup already carries the amount against the
--   tier; the enrollment file resolves it by tier-name match). is_declined/declined_at/
--   recorded_by are decline tracking, independent of amount/tier -- a declined entry has neither.
--
-- Every FK-bearing table gets its own index on the FK, per the convention on
-- summit_service_item_flags (idx_..._service_item) and summit_plan_template_map
-- (idx_..._service_item) -- a FK constraint does not itself guarantee a usable index on every
-- MySQL storage engine's join path, and every existing sibling table in this package indexes
-- explicitly rather than relying on it.
--
-- UNIQUE (matrix, participant) and UNIQUE (participant row, leg) are the identity rules: one
-- header row per participant per matrix, one detail row per participant per leg. Neither is the
-- primary key, following summit_plan_template_map's own precedent (a surrogate PK, the business
-- key named separately) so a future decision does not require rebuilding the table.
--
-- Audit columns (created_at DATETIME NOT NULL, created_by VARCHAR(100) NULL) follow
-- summit_plan_template_map's own convention exactly -- no updated_at/updated_by, unlike
-- summit_service_item_flags, because nothing here is expected to need a second-actor edit trail
-- beyond what the lock/push columns already record (pushed_by/pushed_at, locked_by/locked_at,
-- recorded_by/declined_at).
--
-- NO ROWS ARE INSERTED (rule 5). Nothing reads or writes these tables yet -- the matrix UI,
-- the payroll deduction report, the manual-entry report, and the push mechanism are all later,
-- separate builds.
--
-- Idempotency guard: CREATE TABLE IF NOT EXISTS, following V098/V100's precedent for new tables
-- (rather than V101/V103/V104/V105's information_schema + PREPARE/EXECUTE pattern, which guards
-- ADD COLUMN against a table that may already exist -- there is nothing to guard here beyond the
-- table's own existence).
--
-- Reversal: DROP TABLE enrollment_matrix_entry, enrollment_matrix_participant, enrollment_matrix
-- (child tables first, for the FKs). Nothing outside this migration references any of the three
-- -- no view, no other migration, no export writer -- so dropping them changes no running
-- behaviour.

CREATE TABLE IF NOT EXISTS enrollment_matrix (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    setup_id      BIGINT        NOT NULL,
    is_pushed     TINYINT(1)    NOT NULL DEFAULT 0,
    pushed_by     VARCHAR(100)  NULL,
    pushed_at     DATETIME      NULL,
    created_at    DATETIME      NOT NULL,
    created_by    VARCHAR(100)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_enrollment_matrix_setup (setup_id),
    CONSTRAINT fk_enrollment_matrix_setup
        FOREIGN KEY (setup_id) REFERENCES assignee (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS enrollment_matrix_participant (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    matrix_id              BIGINT        NOT NULL,
    participant_id         BIGINT        NOT NULL,
    payroll_frequency      VARCHAR(32)   NULL,
    custom_schedule_name   VARCHAR(100)  NULL,
    is_entry_locked        TINYINT(1)    NOT NULL DEFAULT 0,
    locked_by              VARCHAR(100)  NULL,
    locked_at              DATETIME      NULL,
    created_at             DATETIME      NOT NULL,
    created_by             VARCHAR(100)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_enrollment_matrix_participant_matrix_participant (matrix_id, participant_id),
    INDEX idx_enrollment_matrix_participant_matrix (matrix_id),
    INDEX idx_enrollment_matrix_participant_participant (participant_id),
    CONSTRAINT fk_enrollment_matrix_participant_matrix
        FOREIGN KEY (matrix_id) REFERENCES enrollment_matrix (id),
    CONSTRAINT fk_enrollment_matrix_participant_participant
        FOREIGN KEY (participant_id) REFERENCES employer_participant (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS enrollment_matrix_entry (
    id                       BIGINT         NOT NULL AUTO_INCREMENT,
    matrix_participant_id    BIGINT         NOT NULL,
    plan_template_map_id     BIGINT         NOT NULL,
    amount                   DECIMAL(10,2)  NULL,
    tier_name                VARCHAR(100)   NULL,
    is_declined              TINYINT(1)     NOT NULL DEFAULT 0,
    declined_at              DATETIME       NULL,
    recorded_by              VARCHAR(100)   NULL,
    created_at               DATETIME       NOT NULL,
    created_by               VARCHAR(100)   NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_enrollment_matrix_entry_participant_leg (matrix_participant_id, plan_template_map_id),
    INDEX idx_enrollment_matrix_entry_participant (matrix_participant_id),
    INDEX idx_enrollment_matrix_entry_leg (plan_template_map_id),
    CONSTRAINT fk_enrollment_matrix_entry_participant
        FOREIGN KEY (matrix_participant_id) REFERENCES enrollment_matrix_participant (id),
    CONSTRAINT fk_enrollment_matrix_entry_leg
        FOREIGN KEY (plan_template_map_id) REFERENCES summit_plan_template_map (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V106' AS version, '2026-09-12' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V106', 'Enrollment matrix storage: matrix/participant/entry tables', 'V106__enrollment_matrix.sql', NOW());
