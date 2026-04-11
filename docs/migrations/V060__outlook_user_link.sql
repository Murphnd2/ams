-- V060: Outlook Web Add-in — user link + note attachments
--
-- Adds the outlook_user_link cross-reference table mapping AMS Person
-- records to Microsoft 365 identities for the "Log to AMS" Outlook add-in.
-- Each link carries a per-user API token that the add-in stores and sends
-- on every request as `Authorization: Bearer {token}`.
--
-- Also adds a nullable note_id FK to weblink so that plain Notes (not just
-- Email subclasses) can own attached files — used when the add-in logs an
-- inbound email with attachments as a Note on an activity.

-- ----------------------------------------------------------------------
-- 1. outlook_user_link table
-- ----------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS outlook_user_link (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    person_id BIGINT NOT NULL,
    m365_email VARCHAR(255) NOT NULL,
    api_token VARCHAR(64) NOT NULL,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_oul_person FOREIGN KEY (person_id) REFERENCES person(person_id),
    CONSTRAINT uq_oul_m365_email UNIQUE (m365_email),
    CONSTRAINT uq_oul_api_token UNIQUE (api_token),
    INDEX idx_oul_person (person_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------
-- 2. weblink.note_id — allow attachments to hang off a plain Note
--    (same pattern as existing email_id and todo_note_id FKs)
-- ----------------------------------------------------------------------
ALTER TABLE weblink ADD COLUMN note_id BIGINT NULL;
ALTER TABLE weblink ADD CONSTRAINT fk_weblink_note
    FOREIGN KEY (note_id) REFERENCES note(note_id);
CREATE INDEX idx_weblink_note ON weblink(note_id);

-- ----------------------------------------------------------------------
-- 3. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V060' AS version, '2026-04-10' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V060', 'Outlook add-in user link + weblink.note_id', 'V060__outlook_user_link.sql', NOW());
