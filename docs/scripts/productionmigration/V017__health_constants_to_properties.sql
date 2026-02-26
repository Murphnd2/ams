-- =============================================================================
-- V017 — Move SYS_HEALTH constants to ssa.properties, add EMAIL_FOOTER_TEXT
-- Date: February 26, 2026
-- Prerequisites: V016
--
-- These constants are now read from ssa.properties by healthcheck.sh instead
-- of the database constant table.
--
-- Constants removed:
--   SYS_HEALTH_EMAIL_TO      — now in ssa.properties
--   SYS_HEALTH_SMTP_SERVER   — now in ssa.properties
--   SYS_HEALTH_SMTP_PORT     — now in ssa.properties
--   SYS_HEALTH_SMTP_USER     — now in ssa.properties
--   SYS_HEALTH_SMTP_PASSWORD — now in ssa.properties (secret)
--   SYS_HEALTH_ENABLED       — now in ssa.properties
--   SYS_HEALTH_EMAIL_FROM    — now in ssa.properties
--
-- IMPORTANT: Before running this script, ensure your ssa.properties contains
-- the SYS_HEALTH_* keys (see deployment docs).
--
-- Also seeds EMAIL_FOOTER_TEXT constant for per-PSP email footer customization.
-- =============================================================================

SET SQL_SAFE_UPDATES = 0;

-- ─── Delete health constants now managed in ssa.properties ──────────────────

DELETE FROM constant WHERE name IN (
    'SYS_HEALTH_EMAIL_TO',
    'SYS_HEALTH_SMTP_SERVER',
    'SYS_HEALTH_SMTP_PORT',
    'SYS_HEALTH_SMTP_USER',
    'SYS_HEALTH_SMTP_PASSWORD',
    'SYS_HEALTH_ENABLED',
    'SYS_HEALTH_EMAIL_FROM'
);

-- ─── Seed EMAIL_FOOTER_TEXT if not present ───────────────────────────────────
-- PSP entity maps to assignee table (PSP extends Assignee, dtype='PSP')

INSERT IGNORE INTO constant (name, value)
VALUES ('EMAIL_FOOTER_TEXT', (SELECT full_name FROM assignee WHERE dtype = 'PSP' LIMIT 1));

-- ─── Self-register in schema_version ─────────────────────────────────────────

INSERT IGNORE INTO schema_version (version, description, script_name)
VALUES ('V017', 'Move SYS_HEALTH constants to ssa.properties, add EMAIL_FOOTER_TEXT', 'V017__health_constants_to_properties.sql');

SET SQL_SAFE_UPDATES = 1;

-- ─── VERIFICATION ────────────────────────────────────────────────────────────
-- SELECT * FROM constant WHERE name LIKE 'SYS_HEALTH%';
-- Should return 0 rows
-- SELECT * FROM constant WHERE name = 'EMAIL_FOOTER_TEXT';
-- Should return 1 row with PSP name as value
-- SELECT * FROM schema_version WHERE version = 'V017';

-- ─── ROLLBACK (if needed) ────────────────────────────────────────────────────
-- INSERT INTO constant (name, value) VALUES ('SYS_HEALTH_EMAIL_TO', 'kevin@superiorstate.net');
-- INSERT INTO constant (name, value) VALUES ('SYS_HEALTH_SMTP_SERVER', 'mail.smtp2go.com');
-- INSERT INTO constant (name, value) VALUES ('SYS_HEALTH_SMTP_PORT', '2525');
-- INSERT INTO constant (name, value) VALUES ('SYS_HEALTH_SMTP_USER', '<user>');
-- INSERT INTO constant (name, value) VALUES ('SYS_HEALTH_SMTP_PASSWORD', '<password>');
-- INSERT INTO constant (name, value) VALUES ('SYS_HEALTH_ENABLED', 'true');
-- INSERT INTO constant (name, value) VALUES ('SYS_HEALTH_EMAIL_FROM', 'health@superiorstate.net');
-- DELETE FROM constant WHERE name = 'EMAIL_FOOTER_TEXT';
-- DELETE FROM schema_version WHERE version = 'V017';
