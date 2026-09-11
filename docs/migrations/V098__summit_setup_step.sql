-- V098: Summit setup step state (T230 phase 1 — employer, cdhplan, schedules, demographics)
--
-- V096/V097's summit_file_export rows record what was generated and (if pushed) delivered. This
-- table is different in kind: it records per-proposal, per-step SETUP-PANEL COMPLETION, decided by
-- an explicit PSP-admin "Mark done" -- not by anything Summit says. It is a separate entity rather
-- than an extension of summit_file_export (T230's S42 decision, reversing that row's earlier
-- "likely an extension" clause) because Mark done must work for a step with NO export at all: the
-- manual override for a group already set up in Summit directly, or entered by hand.
--
-- Step vocabulary, held in step_key: employer | cdhplan | schedules | demographics. (enrollment is
-- priority 2 and is not in this table's whitelist as of this migration -- SummitResponseServlet
-- enforces the whitelist, not this schema.)
--
-- state is DONE or OPEN. basis is REVIEWED (marked done from a response check, against a specific
-- pushed file) or MANUAL (marked done with no response review -- the override), and is NULL
-- whenever state = 'OPEN'. export_id is set only for a REVIEWED row and points at the
-- summit_file_export row whose response was reviewed; NULL for MANUAL and for every OPEN row.
--
-- ⚠️ NO RESPONSE CONTENT LIVES HERE OR ANYWHERE ELSE. Response lines echo personal data --
-- Demographics echoes participant names, and V096's own class note records that a rejection
-- comment once echoed a full street address. The response is fetched over SFTP on demand, parsed,
-- rendered in-request by SummitResponseServlet, and discarded. This table stores only the fact and
-- basis of completion.
--
-- One row per (proposal, step): reopening or re-marking a step updates the existing row rather
-- than inserting a new one, enforced by uq_summit_setup_step. psp_id is nullable for the same
-- reason summit_file_export.psp_id is (V096's class note) -- a recording failure to resolve a PSP
-- must not block Mark done. proposal_id is NOT NULL: unlike summit_file_export's audit rows, a
-- step-state row has no meaning without the proposal it tracks, and step state is read back by
-- (proposal_id, step_key), never by proposal_id alone across a deleted proposal.
--
-- export_id carries a real foreign key to summit_file_export, unlike summit_file_export's own
-- proposal_id/prospect_id scalars -- those are deliberately unconstrained so a deleted proposal
-- cannot take export history down with it (V096's class note). export_id has the opposite
-- lifecycle: a REVIEWED row's whole reason for existing is "this specific export's response was
-- reviewed", so if that export row is ever removed, the review claim should be visibly broken
-- (FK violation) rather than silently pointing at nothing.
--
-- updated_at/updated_by mirror summit_file_export's generated_at/generated_by shape -- display-only,
-- null updated_by is a supported state (session could not name anyone), not an error.
--
-- NO ROWS ARE INSERTED. Rows are written at runtime by SummitResponseServlet. The table arrives
-- empty on every installation.
--
-- Reversal: DROP TABLE. Nothing references it -- no view, no inbound FK -- and the setup panel's
-- status fragment (SummitSetupStatusServlet) fails silent on any read error, so an installation
-- that drops this table shows no step-state badges and otherwise keeps working unchanged.

CREATE TABLE IF NOT EXISTS summit_setup_step (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    psp_id        BIGINT        NULL,
    proposal_id   BIGINT        NOT NULL,
    step_key      VARCHAR(20)   NOT NULL,
    state         VARCHAR(10)   NOT NULL,
    basis         VARCHAR(10)   NULL,
    export_id     BIGINT        NULL,
    updated_at    DATETIME      NOT NULL,
    updated_by    VARCHAR(100)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_summit_setup_step (proposal_id, step_key),
    CONSTRAINT fk_summit_setup_step_psp
        FOREIGN KEY (psp_id) REFERENCES assignee (id),
    CONSTRAINT fk_summit_setup_step_export
        FOREIGN KEY (export_id) REFERENCES summit_file_export (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V098' AS version, '2026-09-10' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V098', 'Summit setup step state (T230 phase 1): Mark done per proposal/step, no response content stored', 'V098__summit_setup_step.sql', NOW());
