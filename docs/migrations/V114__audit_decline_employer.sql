-- V114: Card-decline audit employer designation (T237 check #3, audit_decline_employer)
--
-- A PSP-scoped list of the AMS employers whose card declines CardDeclineCheck evaluates. Card-decline
-- monitoring is opt-in per employer, set during plan setup through AuditDeclineEmployerAdmin; the
-- check keeps a decline row only when its `Employer SystemID` matches the Summit `EmployerID`
-- (`employer.employer_id`, `Employer.altId`) of a row here. This replaces reliance on the Summit
-- export template's own Employer selector as the check's only narrowing, following V101's precedent
-- (a PSP-scoped mapping table with a small admin screen) rather than inventing a second mechanism.
--
-- Why employer designation is the only available narrowing: `Plan Type Code` is blank on every
-- decline row in the Transaction export (0 of 958 in the 2026-09-14 verification sample), so a
-- decline cannot be scoped by purse -- a premium decline is indistinguishable from an FSA one in the
-- file. The employer is the only dimension both the export and AMS can agree on.
--
-- Identity, settled 2026-09-15 by comparing real exports: `Employer SystemID` (Transaction export)
-- = `Employer_ID` (Plan History) = Summit `EmployerID` = `employer.employer_id` -- NOT
-- `employer.organization_id`. The Plan History sample carries `Employer_ID` 1100 and
-- `Organization_ID` 1102 as different values; the Transaction export's `Employer SystemID` set
-- contains 1100 and not 1102.
--
-- Scoped by `psp_id BIGINT` FK to `assignee(id)`, matching `summit_service_item_flags.psp_id` and
-- `summit_plan_template_map.psp_id` exactly (PSP extends Assignee under single-table inheritance, so
-- every FK to a PSP targets `assignee(id)`). NOT NULL for the same reason: a row with no PSP could
-- never be read back by the PSP-scoped check.
--
-- `employer_id` is INT and FKs to `employer(organization_id)` -- the AMS employer primary key
-- (`Employer.id`), NOT the Summit `EmployerID` the check matches on. Storing the FK means the
-- employer name resolves for free on the admin screen and the detail page, and referential
-- integrity holds (an employer cannot be deleted out from under a designation); `Employer.altId`
-- is read off the joined row when the check needs the Summit-side value. An employer imported
-- with `altId = 0` (a J1 file without the `EmployerID` column, T265) can be designated but will
-- never match a decline row -- the admin screen flags it.
--
-- ⚠️ A ROW'S PRESENCE IS THE DESIGNATION -- THERE IS NO `is_active` COLUMN, exactly V101's
-- reasoning. A designation creates nothing in Summit and is not an upsert identity for anything
-- Summit-side; a PSP admin who no longer wants an employer monitored removes the row, the same way
-- SummitEmployerFlagAdmin deletes rather than deactivates. "No row" is the only off state.
--
-- The unique constraint is where the 1:1 rule lives, as V095/V101's own notes explain for
-- themselves: named rather than the primary key, so a future fan-out decision is an index drop
-- against live rows rather than a table rebuild.
--
-- NO ROWS ARE INSERTED. Designation is per-installation data and is never written into source or
-- seeded. The table arrives empty on every installation; CardDeclineCheck reports NOT_CONFIGURED
-- (not OK) until at least one employer is designated, because an opt-in feature with nothing
-- opted in has not been configured and "OK" would claim all-clear on an unmonitored book.
--
-- No `INSERT INTO constant`. No config key -- designation is data, not configuration.
--
-- Reversal: DROP TABLE. Nothing references it -- no view, no inbound FK, no other migration. After
-- the drop, CardDeclineCheck's designated-set read fails and the check reports ERROR on every run
-- (its evaluate never throws); AuditDeclineEmployerAdmin 500s on load. Both are the correct
-- loud outcomes for a dropped table; nothing else in AMS reads it.

CREATE TABLE audit_decline_employer (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    psp_id        BIGINT        NOT NULL,
    employer_id   INT           NOT NULL,
    created_at    DATETIME      NOT NULL,
    created_by    VARCHAR(100)  NULL,
    updated_at    DATETIME      NULL,
    updated_by    VARCHAR(100)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_audit_decline_employer_psp_employer (psp_id, employer_id),
    INDEX idx_audit_decline_employer_employer (employer_id),
    CONSTRAINT fk_audit_decline_employer_psp
        FOREIGN KEY (psp_id) REFERENCES assignee (id),
    CONSTRAINT fk_audit_decline_employer_employer
        FOREIGN KEY (employer_id) REFERENCES employer (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V114' AS version, '2026-09-15' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V114', 'Card-decline audit employer designation (audit_decline_employer): PSP-scoped opt-in list of AMS employers whose declines CardDeclineCheck evaluates, matched on employer.employer_id (Summit EmployerID); row presence is the designation, no seed rows, no constant (T237 check #3)', 'V114__audit_decline_employer.sql', NOW());
