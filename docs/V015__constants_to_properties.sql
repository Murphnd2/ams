-- =============================================================================
-- V015: Move infrastructure secrets from DB to ssa.properties
-- Date: February 25, 2026
-- Prerequisites: V014
--
-- These constants are now read from ssa.properties via AppConfig instead of
-- the database constant table. The code changes (StorageDAO, ClaudeApiService)
-- must be deployed BEFORE running this migration.
--
-- Constants removed:
--   S3_ENDPOINT      — now in ssa.properties
--   S3_BUCKET        — now in ssa.properties
--   S3_ACCESS_KEY    — now in ssa.properties (secret)
--   S3_SECRET_KEY    — now in ssa.properties (secret)
--   ANTHROPIC_API_KEY — now in ssa.properties (secret, SSA-only feature)
--   SAVE_PATH        — moved to ssa.properties in D-03 (Feb 23), DB row is dead
--
-- IMPORTANT: Before running this script, ensure your ssa.properties contains:
--   S3_ENDPOINT=https://s3.us-east-1.wasabisys.com
--   S3_BUCKET=ams-file-storage
--   S3_ACCESS_KEY=<your-access-key>
--   S3_SECRET_KEY=<your-secret-key>
--   ANTHROPIC_API_KEY=<your-api-key>   (SSA installs only)
--
-- After running: Verify file uploads and downloads still work.
-- =============================================================================

SET SQL_SAFE_UPDATES = 0;

-- ─── Delete constants now managed in ssa.properties ─────────────────────────

DELETE FROM constant WHERE name IN (
    'S3_ENDPOINT',
    'S3_BUCKET',
    'S3_ACCESS_KEY',
    'S3_SECRET_KEY',
    'ANTHROPIC_API_KEY',
    'SAVE_PATH'
);

-- ─── Self-register in schema_version ─────────────────────────────────────────

INSERT IGNORE INTO schema_version (version, description, script_name)
VALUES ('V015', 'Move S3 and API key constants to ssa.properties, delete dead SAVE_PATH', 'V015__constants_to_properties.sql');

SET SQL_SAFE_UPDATES = 1;

-- ─── VERIFICATION ────────────────────────────────────────────────────────────
-- SELECT * FROM constant ORDER BY name;
-- Should no longer contain: S3_ENDPOINT, S3_BUCKET, S3_ACCESS_KEY, S3_SECRET_KEY, ANTHROPIC_API_KEY, SAVE_PATH
-- SELECT * FROM schema_version WHERE version = 'V015';

-- ─── ROLLBACK (if needed) ────────────────────────────────────────────────────
-- INSERT INTO constant (name, value, note) VALUES ('S3_ENDPOINT', 'https://s3.us-east-1.wasabisys.com', 'Wasabi S3 endpoint');
-- INSERT INTO constant (name, value, note) VALUES ('S3_BUCKET', 'ams-file-storage', 'Wasabi bucket name');
-- INSERT INTO constant (name, value, note) VALUES ('S3_ACCESS_KEY', '', 'Wasabi access key — fill in');
-- INSERT INTO constant (name, value, note) VALUES ('S3_SECRET_KEY', '', 'Wasabi secret key — fill in');
-- INSERT INTO constant (name, value, note) VALUES ('ANTHROPIC_API_KEY', '', 'Claude API key');
-- INSERT INTO constant (name, value, note) VALUES ('SAVE_PATH', 'C:\\data\\', NULL);
-- DELETE FROM schema_version WHERE version = 'V015';
