-- V096: Summit file export record — one row per generated export file
--
-- `SummitExportServlet` streams a generated file straight to the browser and keeps no trace
-- that it was ever produced. This table is that trace. It is a RECORD, not a transport: no
-- FTP, SFTP, HTTP client, scheduled job or outbound connection of any kind is created by this
-- migration or the code that writes it, and the record is needed identically whichever
-- transport is eventually chosen (client push to DataPath's site, or AMS hosting — undecided;
-- see the HIGH row in docs/analysis/project_backlog.md).
--
-- Two things depend on it, and neither can be built without it:
--
--   1. RESPONSE-FILE VERIFICATION. Summit returns a results file per import. File 2's results
--      template carries NO row number, so correlation falls back to `Plan Name`; Demographics
--      correlates on row number. Both need the bytes that were ACTUALLY SENT. A regeneration
--      is not a substitute — plan-template mapping rows (V095), `SUMMIT_BRANCH_CODE`,
--      `SUMMIT_TPA_ID_PREFIX` and the participant roster can all change underneath an export,
--      so re-running the servlet can legitimately produce different bytes for the same
--      proposal. `content` is therefore stored, not recomputed.
--
--   2. T194 — SUMMIT DEDUPES A RE-SENT FILE ON CONTENT. Re-sending unchanged bytes is a
--      silent no-op: no error, no results row, nothing. `content_sha256` makes that detectable
--      BEFORE sending rather than after a confusing non-result, which is why it is indexed
--      rather than left as an incidental column.
--
-- ⚠️ `content` HOLDS PII. Demographics (file 3) rows carry participant first name, last name,
-- street address, city, state, postal code and email; Enrollment (file 4) carries the
-- participant key and a contribution amount. This column therefore belongs in the same
-- conversation as any other PII column in this schema — retention, access and export.
-- It carries NO SSN: the project's no-SSN boundary (LA-35) holds through the whole export
-- set, because `employer_participant` (V094) has no SSN, DOB or compensation column and
-- `CensusParseService` drops those headers as unrecognised, so no SSN ever enters the
-- process, let alone a generated file.
--
-- SCOPING. `psp_id BIGINT` FK to `assignee(id)`, the same scoping V095 uses (PSP extends
-- Assignee under single-table inheritance, so every FK to a PSP targets `assignee(id)` and
-- not a `psp` table). ⚠️ UNLIKE V095's, THIS COLUMN IS NULLABLE, and the difference is
-- deliberate. V095's rows are read BY PSP, so a null-PSP row could never be read back and
-- NOT NULL is correct there. Here the row is written from a live request whose session may
-- not resolve a PSP, and the governing rule is that A RECORDING FAILURE MUST NOT FAIL THE
-- EXPORT — a NOT NULL column would turn an unresolvable session into a rejected insert and,
-- worse, invite the writer to block the download over it. A null-PSP row is still a complete
-- record of what was sent and still answers the T194 hash question, which needs no PSP.
--
-- `proposal_id` and `prospect_id` are plain scalars with NO foreign keys, also deliberately.
-- An audit row must outlive the record it describes: a proposal deleted a year after its
-- files were sent to Summit must not take the evidence of that send with it, nor make the
-- deletion fail. Both are nullable for the same reason `psp_id` is.
--
-- `file_type` holds the servlet's own `type` discriminator verbatim — `employer`, `cdhplan`,
-- `demographics`, `enrollment` — so the record and the request are readable against each
-- other with no translation table.
--
-- `row_count` is the number of emitted lines. ZERO IS A LEGITIMATE VALUE AND IS RECORDED AS
-- SUCH: S31-J's empty-file guard covers `cdhplan` only, so a prospect with an empty
-- participant roster still produces a zero-row `demographics` or `enrollment` file, and that
-- is exactly the kind of send worth having a record of.
--
-- `content` is MEDIUMTEXT rather than TEXT. Files are kilobytes today, but TEXT's 64KB
-- ceiling is a truncation-on-large-roster failure waiting to happen and MEDIUMTEXT costs one
-- extra length byte per row.
--
-- NO ROWS ARE INSERTED. Rows are written at runtime by `SummitExportServlet`. The table
-- arrives empty on every installation.
--
-- Reversal: DROP TABLE. Nothing references it — no view, no inbound FK, no other migration —
-- and the servlet's recording is best-effort, so an installation that drops it logs an ERROR
-- per export and keeps delivering files unchanged.

CREATE TABLE summit_file_export (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    psp_id            BIGINT        NULL,
    file_type         VARCHAR(20)   NOT NULL,
    proposal_id       BIGINT        NULL,
    prospect_id       BIGINT        NULL,
    file_name         VARCHAR(255)  NOT NULL,
    generated_at      DATETIME      NOT NULL,
    generated_by      VARCHAR(100)  NULL,
    row_count         INT           NOT NULL,
    byte_count        INT           NOT NULL,
    content_sha256    CHAR(64)      NOT NULL,
    content           MEDIUMTEXT    NULL,
    PRIMARY KEY (id),
    INDEX idx_summit_file_export_listing (psp_id, file_type, generated_at),
    INDEX idx_summit_file_export_sha256 (content_sha256),
    CONSTRAINT fk_summit_file_export_psp
        FOREIGN KEY (psp_id) REFERENCES assignee (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V096' AS version, '2026-09-08' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V096', 'Summit file export record (summit_file_export): one row per generated Summit export file with the exact bytes sent, an indexed content SHA-256 for T194 dedupe detection, row/byte counts and acting user; no transport, no rows inserted, content holds PII but no SSN', 'V096__summit_file_export.sql', NOW());
