-- V049: Add source_task_id to delegated_todo for required-sequence auto-approval
--
-- Enables BPO to auto-accept future occurrences of required-sequence tasks
-- (SETUP/RENEWAL) once the same task from the same PSP has been approved once.

ALTER TABLE delegated_todo
    ADD COLUMN source_task_id VARCHAR(20) NULL AFTER recurring_series_id;

-- Index for the auto-approval lookup: sourceTaskId + pspClient
CREATE INDEX idx_delegated_todo_source_task
    ON delegated_todo (source_task_id, psp_client_id);

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V049', 'Add source_task_id to delegated_todo for required-sequence auto-approval',
        'V049__delegated_todo_source_task_id.sql', NOW());
