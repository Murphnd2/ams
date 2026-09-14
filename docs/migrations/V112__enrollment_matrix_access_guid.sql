    -- V112: enrollment_matrix.access_guid -- GUID-addressed, authenticated agent access to one
    -- setup's enrollment matrix. S58-P3.
    --
    -- A new, separate identifier. The proposal GUID (proposal.application_guid) is deliberately NOT
    -- reused: it is served unauthenticated at /proposal/{guid} and circulates outside SSA, so it
    -- cannot double as the address of a page carrying named employee data. This one is an
    -- identifier, not a credential -- /matrix/{guid} is behind LoginFilter and every request is
    -- authorised on the requester's relationship to the setup (MatrixAccessResolver); the GUID
    -- only says which matrix.
    --
    -- Nullable, generated lazily by EnrollmentMatrixServlet (action=issueLink) the first time a PSP
    -- admin asks for the link. NO backfill: a matrix nobody has asked to share has no GUID, and a
    -- NULL never matches a lookup. Unique so one GUID resolves to at most one matrix; MySQL permits
    -- any number of NULLs under a UNIQUE key, so the nullable + unique combination is sound.
    --
    -- NO INSERT INTO constant anywhere in this migration.
    --
    -- Idempotency guard: information_schema.COLUMNS + PREPARE/EXECUTE for the column (V108/V110's
    -- pattern), information_schema.STATISTICS + PREPARE/EXECUTE for the unique key (V103's pattern).
    --
    -- Reversal: ALTER TABLE enrollment_matrix DROP INDEX uq_enrollment_matrix_access_guid, then
    -- DROP COLUMN access_guid. Any issued link stops resolving; nothing else reads the column.

    SET @db = DATABASE();

    SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'enrollment_matrix' AND COLUMN_NAME = 'access_guid');
    SET @sql = IF(@col = 0,
        'ALTER TABLE enrollment_matrix ADD COLUMN access_guid VARCHAR(36) NULL COMMENT ''Lazily issued by a PSP admin for /matrix/{guid}. Identifier only, never a credential: the page is authenticated and authorised per request. NULL until issued.'' AFTER setup_id',
        'SELECT 1');
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

    SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
                WHERE TABLE_SCHEMA = @db AND TABLE_NAME = 'enrollment_matrix'
                  AND INDEX_NAME = 'uq_enrollment_matrix_access_guid');
    SET @sql = IF(@idx = 0,
        'ALTER TABLE enrollment_matrix ADD UNIQUE KEY uq_enrollment_matrix_access_guid (access_guid)',
        'SELECT 1');
    PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

    CREATE OR REPLACE VIEW schema_info AS
    SELECT 'V112' AS version, '2026-09-13' AS updated;

    INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
    VALUES ('V112', 'enrollment_matrix.access_guid, nullable unique, lazily issued for /matrix/{guid} agent access (S58-P3)', 'V112__enrollment_matrix_access_guid.sql', NOW());
