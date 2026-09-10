-- V097: Delivery tracking columns on summit_file_export
--
-- V096's summit_file_export row records that a file was GENERATED; this migration adds four
-- nullable columns so the same row can also record whether it was PUSHED over SFTP into
-- Summit's ImportFiles directory (T229), and the outcome of that push.
--
-- Status vocabulary, held in delivery_status:
--   NULL      -- download only. This row was never pushed; it exists purely as a generated-file
--                record exactly as it did before this migration. Every pre-existing row reads
--                as NULL and needs no backfill.
--   PUSHING   -- the row was inserted for a push, before the SFTP upload was attempted. Set at
--                the same moment delivered_at and delivery_dir are set, before any network call.
--   PUSHED    -- the upload to delivery_dir succeeded.
--   PUSH_FAILED -- the upload was attempted and failed; delivery_error carries the (scrubbed)
--                failure message. A failed row is never retried in place -- a retry is a new row.
--
-- delivered_at is the time the push was attempted (set alongside PUSHING), not necessarily the
-- time Summit received or processed the file -- this table has no visibility into Summit's own
-- processing state. delivery_dir is the exact remote directory the file was (or would be)
-- uploaded to, i.e. ImportFiles. delivery_error is NULL unless delivery_status is PUSH_FAILED.
--
-- ⚠️ T230 (poll ResponseFiles for a response, and per-step "Mark done" state) is deliberately
-- NOT part of this migration. These four columns answer "was this file pushed, and did the
-- push itself succeed" -- nothing here tracks Summit's response or setup-panel step completion.
-- That is separate schema, filed as its own backlog row.
--
-- All four columns are nullable with no default: every existing row is a download-only record
-- and must remain valid with no backfill, exactly as V093 did for its own optional columns.
--
-- Re-run safety: this installation's most recent column-adding migration (V093) used a plain
-- ALTER TABLE with no guard. Since that migration cannot be safely re-run, this one instead
-- mirrors the information_schema.COLUMNS-guarded pattern from V041, so a repeated run is a
-- no-op rather than a duplicate-column error.
--
-- Reversal: DROP COLUMN on each of the four, independently. Nothing else references them.

SET @db = DATABASE();

-- delivery_status
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_file_export' AND COLUMN_NAME = 'delivery_status');
SET @sql = IF(@col = 0,
    'ALTER TABLE summit_file_export ADD COLUMN delivery_status VARCHAR(20) NULL AFTER content_sha256',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- delivered_at
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_file_export' AND COLUMN_NAME = 'delivered_at');
SET @sql = IF(@col = 0,
    'ALTER TABLE summit_file_export ADD COLUMN delivered_at DATETIME NULL AFTER delivery_status',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- delivery_dir
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_file_export' AND COLUMN_NAME = 'delivery_dir');
SET @sql = IF(@col = 0,
    'ALTER TABLE summit_file_export ADD COLUMN delivery_dir VARCHAR(255) NULL AFTER delivered_at',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- delivery_error
SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'summit_file_export' AND COLUMN_NAME = 'delivery_error');
SET @sql = IF(@col = 0,
    'ALTER TABLE summit_file_export ADD COLUMN delivery_error VARCHAR(500) NULL AFTER delivery_dir',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V097' AS version, '2026-09-10' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V097', 'summit_file_export delivery columns (T229)', 'V097__summit_file_export_delivery.sql', NOW());
