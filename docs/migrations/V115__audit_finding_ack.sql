-- V115: Generic audit-finding acknowledgment (T237, audit_finding_ack)
--
-- A PSP-scoped Handled/Ignored acknowledgment for one finding of one audit check, keyed by the
-- check's own key() and a check-defined natural finding key -- generic across every check in the
-- framework (AuditCheck.key(), matching audit_run.check_key), not just the one this run wires it
-- to. Wired to CardDeclineCheck only in this build (finding_key = Participant System ID);
-- FundedPurseNoDisbursementCheck and IchraUncodedParticipantsCheck are deliberately not touched --
-- the table exists so they can be added later without a second migration, not because this run
-- acknowledges anything of theirs.
--
-- Two states, not one, because they mean different things and must be evaluated differently:
--   HANDLED -- this occurrence was dealt with (participant called, carrier confirmed, card
--     reissued). State-scoped, not permanent: it suppresses only while the finding's current
--     activity has not moved past what was observed at acknowledgment time (observed_count,
--     observed_through). New activity past that point re-surfaces the finding. A Handled
--     acknowledgment that swallowed a fresh decline on a premium-bearing card would be exactly the
--     silent-failure shape this check exists to prevent.
--   IGNORED -- a known ongoing condition not worth seeing. Suppressed unconditionally until the
--     row is removed, regardless of new activity. observed_count/observed_through are meaningless
--     for this state and stay NULL.
--
-- Scoped by `psp_id BIGINT` FK to `assignee(id)`, matching `audit_decline_employer.psp_id` and
-- `summit_service_item_flags.psp_id` exactly (PSP extends Assignee under single-table inheritance,
-- so every FK to a PSP targets `assignee(id)`).
--
-- `check_key VARCHAR(50)` matches `audit_run.check_key`'s own width (V099) -- both hold the same
-- values, `AuditCheck.key()`. `finding_key VARCHAR(255)` is deliberately wide and untyped: it is
-- whatever natural key the owning check defines for one finding (here, a Summit
-- Participant System ID, but a future check's key need not be numeric or even short). No FK on
-- either column -- `check_key` names no table, and `finding_key`'s meaning is check-specific, so
-- referential integrity against a specific identity table is neither possible nor desired here.
--
-- `observed_count INT NULL` / `observed_through DATE NULL` -- set together for HANDLED (the
-- finding's decline count and most-recent-decline date at acknowledgment time), both NULL for
-- IGNORED. No CHECK constraint enforcing that pairing -- MySQL 8's CHECK support is inconsistent
-- across the installations this schema runs on, and the application (CardDeclineCheck's save path)
-- is the only writer, so it is the only place that needs to enforce it; a DB-level omission is not
-- a code-level oversight.
--
-- `UNIQUE (psp_id, check_key, finding_key)` is where the "one acknowledgment per finding" rule
-- lives, exactly as V101/V114's own notes explain for themselves: named rather than folded into
-- the primary key, so a future change is an index drop against live rows rather than a table
-- rebuild. Re-acknowledging (changing state, or refreshing observed_count/observed_through)
-- updates this row rather than inserting a second -- the unique key is what makes that an upsert
-- rather than a duplicate.
--
-- No separate index on (psp_id, check_key) alone: the unique key above already serves that lookup
-- as a leftmost-column prefix (InnoDB), and `findAllByPspAndCheck` is the only read pattern beside
-- the exact-key lookup the unique key exists for.
--
-- NO ROWS ARE INSERTED. An acknowledgment is a PSP admin's action taken through the UI, never
-- seeded. No `INSERT INTO constant` -- this is data, not configuration, the same reasoning
-- `audit_decline_employer` (V114) gives for itself.
--
-- Reversal: DROP TABLE. Nothing references it -- no view, no inbound FK, no other migration. After
-- the drop, CardDeclineCheck's acknowledgment read returns nothing found (fails open to "surface
-- everything," never throws) and the detail page's Handled/Ignore/Un-acknowledge actions 500 on
-- POST; both are loud, safe outcomes for a dropped table, and nothing else in AMS reads it.

CREATE TABLE audit_finding_ack (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    psp_id             BIGINT        NOT NULL,
    check_key          VARCHAR(50)   NOT NULL,
    finding_key        VARCHAR(255)  NOT NULL,
    ack_state          VARCHAR(16)   NOT NULL,
    observed_count     INT           NULL,
    observed_through   DATE          NULL,
    note               VARCHAR(500)  NULL,
    created_at         DATETIME      NOT NULL,
    created_by         VARCHAR(100)  NULL,
    updated_at         DATETIME      NULL,
    updated_by         VARCHAR(100)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_audit_finding_ack_psp_check_finding (psp_id, check_key, finding_key),
    CONSTRAINT fk_audit_finding_ack_psp
        FOREIGN KEY (psp_id) REFERENCES assignee (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V115' AS version, '2026-09-15' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V115', 'audit_finding_ack: generic PSP-scoped Handled/Ignored finding acknowledgment, wired to card_declines only (T237)', 'V115__audit_finding_ack.sql', NOW());
