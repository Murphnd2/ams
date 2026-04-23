-- V061: ToDo-level ownership override (agent delegation on Setup ToDos)
--
-- Adds per-ToDo ownership fields that mirror Task's (has_owner, owner,
-- allow_non_owner) plus an override flag that says "use these instead
-- of the Task defaults for this specific ToDo."
--
-- Primary use case: delegating individual Setup ToDos to agents of the
-- originating selling agency, without affecting Task templates used
-- by other Setups. Also supports per-Setup overrides to PSP users.
--
-- Resolution semantics (implemented in ToDoOut25.java):
--   override_ownership = 0 → inherit Task.has_owner / Task.owner / Task.allow_non_owner
--   override_ownership = 1 → use ToDo.has_owner / ToDo.owner_id / ToDo.allow_non_owner
--
-- Person rows live in `assignee` (SINGLE_TABLE inheritance), so owner_id
-- references assignee(id) — same pattern as V030/V036/V039/V046/V060.

-- ----------------------------------------------------------------------
-- 1. Add override columns to todo
-- ----------------------------------------------------------------------
ALTER TABLE todo ADD COLUMN override_ownership TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE todo ADD COLUMN has_owner         TINYINT(1) NOT NULL DEFAULT 0;
ALTER TABLE todo ADD COLUMN owner_id          BIGINT NULL;
ALTER TABLE todo ADD COLUMN allow_non_owner   TINYINT(1) NOT NULL DEFAULT 0;

-- ----------------------------------------------------------------------
-- 2. FK on owner_id → assignee(id)  (Person lives in assignee table)
-- ----------------------------------------------------------------------
ALTER TABLE todo ADD CONSTRAINT fk_todo_owner
    FOREIGN KEY (owner_id) REFERENCES assignee(id);

-- ----------------------------------------------------------------------
-- 3. Index for the delegated_to_me EXISTS subquery in ActivityLandingDao
--    (checks todo.owner_id = :me for ownership filter on ViewHome25)
-- ----------------------------------------------------------------------
CREATE INDEX idx_todo_owner ON todo(owner_id);

-- ----------------------------------------------------------------------
-- 4. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V061' AS version, '2026-04-23' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V061', 'ToDo-level ownership override for agent delegation', 'V061__todo_ownership_override.sql', NOW());
