-- =============================================================================
-- V040: Add recurring_series_id to delegated_todo
--
-- Enables BPO to identify when a delegated task belongs to a recurring series.
-- The recurring_series_id is the PSP-side RecurringTaskList.id, pushed at
-- delegation time by BpoTaskPushService. NULL = non-recurring task.
-- Prerequisite: V030 (delegated_todo table exists)
-- =============================================================================

ALTER TABLE delegated_todo
    ADD COLUMN recurring_series_id VARCHAR(50) NULL COMMENT 'PSP RecurringTaskList ID; NULL = non-recurring';

CREATE INDEX idx_delegated_recurring_series ON delegated_todo (recurring_series_id);

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V040', 'Add recurring_series_id to delegated_todo for BPO recurring history', 'V040__delegated_todo_recurring_series.sql', NOW());
