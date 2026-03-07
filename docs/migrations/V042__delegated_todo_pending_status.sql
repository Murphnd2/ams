-- V042: BPO pending approval workflow - status PENDING support
-- No DDL changes needed — status VARCHAR(30) column already exists on delegated_todo.
-- This migration documents the introduction of the PENDING status value
-- used by TaskReceiveApi when autoAcceptTasks is OFF for a PSP client.
--
-- Status values:
--   PENDING = received from PSP, awaiting BPO Admin approval
--   ACTIVE  = accepted, visible to all BPO users (existing behavior)

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V042', 'BPO pending approval workflow - status PENDING support', 'V042__delegated_todo_pending_status.sql', NOW());
