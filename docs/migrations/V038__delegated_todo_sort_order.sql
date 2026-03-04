-- V038: Add sort_order column to delegated_todo for BPO dashboard task ordering
-- Stores the task's position within its checklist, pushed from PSP side.
-- Used to sort delegated tasks by: due date → activity name → sort order.

ALTER TABLE delegated_todo
    ADD COLUMN sort_order INT DEFAULT 0 AFTER info_link;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V038', 'Add sort_order to delegated_todo for BPO dashboard ordering', 'V038__delegated_todo_sort_order.sql', NOW());
