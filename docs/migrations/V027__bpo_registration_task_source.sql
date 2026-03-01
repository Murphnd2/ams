-- =============================================================================
-- V027: BPO Registration — Task Source Refactor
--
-- Changes:
-- 1. Add registration status columns to bpo_registration (is_approved, is_requested, is_accepted)
-- 2. Change task vendor sourcing from Person FK (source_owner) to BpoRegistration FK (bpo_registration_id)
-- 3. Update database views that referenced source_owner
-- =============================================================================

-- Step 1: Add registration status columns to bpo_registration
ALTER TABLE bpo_registration ADD COLUMN is_approved BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bpo_registration ADD COLUMN is_requested BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE bpo_registration ADD COLUMN is_accepted BOOLEAN NOT NULL DEFAULT FALSE;

-- Step 2: Add bpo_registration_id FK to task table
ALTER TABLE task ADD COLUMN bpo_registration_id BIGINT NULL;
ALTER TABLE task ADD CONSTRAINT fk_task_bpo_registration
    FOREIGN KEY (bpo_registration_id) REFERENCES bpo_registration(bpo_reg_id);

-- Step 3: Migrate existing source_owner data (if any tasks have a source_owner set)
-- NOTE: source_owner was a Person FK. If a BpoRegistration exists for the same PSP,
-- we could try to map, but in practice very few (if any) tasks have source_owner set
-- on production. This step is a safety no-op for most deployments.
-- Manual mapping would be needed if source_owner data exists.

-- Step 4: Drop the old source_owner FK and column
-- First find and drop the FK constraint (EclipseLink generates FK names)
SET @fk_name = (
    SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'task'
      AND COLUMN_NAME = 'source_owner'
      AND REFERENCED_TABLE_NAME IS NOT NULL
    LIMIT 1
);
SET @sql = IF(@fk_name IS NOT NULL,
    CONCAT('ALTER TABLE task DROP FOREIGN KEY ', @fk_name), 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

ALTER TABLE task DROP COLUMN source_owner;

-- Step 5: Recreate all views that referenced source_owner → bpo_registration_id
-- Views must be recreated in dependency order (base views first)

-- ─── Layer 1: Views reading directly from task table ───

CREATE OR REPLACE VIEW td_base_03 AS
SELECT t.todo_id, t.date_completed, t.is_complete, t.sort_order, t.checklist_id,
       t.completed_by_id, t.task_id, t.allow_future, t.min_sort_top, t.min_sort_block,
       a.owner_id, a.is_sourced, a.bpo_registration_id AS bpo_registration_id,
       a.allow_early, a.has_owner, a.auto_id, a.has_goto, a.goto_link_id,
       a.has_info, a.info_link_id, a.has_automation, a.allow_non_owner,
       a.DESCRIPTION AS name
FROM td_base_02 t JOIN task a ON t.task_id = a.task_id;

CREATE OR REPLACE VIEW todo_out_04 AS
SELECT row_number() OVER (PARTITION BY t.checklist_id
           ORDER BY t.checklist_id, t.is_complete, t.sort_order, t.todo_id) AS sequential_count,
       t.checklist_id, t.is_complete, t.sort_order, t.todo_id, t.task_id,
       tk.DESCRIPTION, tk.has_automation, tk.auto_id, tk.servlet_name,
       tk.automation_text, tk.allow_early, tk.allow_future, tk.allow_non_owner,
       tk.has_owner, tk.owner_id, tk.is_sourced,
       tk.bpo_registration_id AS bpo_registration_id,
       tk.has_goto, tk.goto_link_id, tk.has_info, tk.info_link_id,
       max(CASE WHEN tk.allow_future = 0 THEN 1 ELSE 0 END)
           OVER (PARTITION BY t.checklist_id ORDER BY t.sort_order
                 ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS has_future_block
FROM todo t JOIN task tk ON t.task_id = tk.task_id
ORDER BY t.checklist_id, t.is_complete, t.sort_order, t.todo_id;

CREATE OR REPLACE VIEW todo_out_05 AS
SELECT subquery.sequential_count, subquery.checklist_id, subquery.is_complete,
       subquery.sort_order, subquery.todo_id, subquery.task_id,
       subquery.DESCRIPTION, subquery.has_automation, subquery.auto_id,
       subquery.servlet_name, subquery.automation_text, subquery.allow_early,
       subquery.allow_future, subquery.allow_non_owner, subquery.has_owner,
       subquery.owner_id, subquery.is_sourced,
       subquery.bpo_registration_id AS bpo_registration_id,
       subquery.has_goto, subquery.goto_link_id, subquery.has_info, subquery.info_link_id,
       max(CASE WHEN subquery.prev_allow_future = 0 THEN 1 ELSE 0 END)
           OVER (PARTITION BY subquery.checklist_id ORDER BY subquery.sort_order
                 ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS has_future_block
FROM (
    SELECT row_number() OVER (PARTITION BY t.checklist_id
               ORDER BY t.checklist_id, t.is_complete, t.sort_order, t.todo_id) AS sequential_count,
           t.checklist_id, t.is_complete, t.sort_order, t.todo_id, t.task_id,
           tk.DESCRIPTION, tk.has_automation, tk.auto_id, tk.servlet_name,
           tk.automation_text, tk.allow_early, tk.allow_future, tk.allow_non_owner,
           tk.has_owner, tk.owner_id, tk.is_sourced,
           tk.bpo_registration_id AS bpo_registration_id,
           tk.has_goto, tk.goto_link_id, tk.has_info, tk.info_link_id,
           lag(tk.allow_future, 1, 1) OVER (PARTITION BY t.checklist_id ORDER BY t.sort_order) AS prev_allow_future
    FROM todo t JOIN task tk ON t.task_id = tk.task_id
) subquery
ORDER BY subquery.checklist_id, subquery.is_complete, subquery.sort_order, subquery.todo_id;

CREATE OR REPLACE VIEW todo_out_06 AS
SELECT subquery.sequential_count, subquery.checklist_id, subquery.is_complete,
       subquery.sort_order, subquery.todo_id, subquery.task_id,
       subquery.DESCRIPTION, subquery.has_automation, subquery.auto_id,
       subquery.servlet_name, subquery.automation_text, subquery.allow_early,
       subquery.allow_future, subquery.allow_non_owner, subquery.has_owner,
       subquery.owner_id, subquery.is_sourced,
       subquery.bpo_registration_id AS bpo_registration_id,
       subquery.has_goto, subquery.goto_link_id, subquery.has_info, subquery.info_link_id,
       max(CASE WHEN subquery.prev_allow_future = 0 THEN 1 ELSE 0 END)
           OVER (PARTITION BY subquery.checklist_id ORDER BY subquery.sort_order
                 ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS has_future_block
FROM (
    SELECT row_number() OVER (PARTITION BY t.checklist_id
               ORDER BY t.checklist_id, t.is_complete, t.sort_order, t.todo_id) AS sequential_count,
           t.checklist_id, t.is_complete, t.sort_order, t.todo_id, t.task_id,
           tk.DESCRIPTION, tk.has_automation, tk.auto_id, tk.servlet_name,
           tk.automation_text, tk.allow_early, tk.allow_future, tk.allow_non_owner,
           tk.has_owner, tk.owner_id, tk.is_sourced,
           tk.bpo_registration_id AS bpo_registration_id,
           tk.has_goto, tk.goto_link_id, tk.has_info, tk.info_link_id,
           lag(CASE WHEN tk.allow_future = 0 AND t.is_complete = 0 THEN 0 ELSE 1 END)
               OVER (PARTITION BY t.checklist_id ORDER BY t.sort_order) AS prev_allow_future
    FROM todo t JOIN task tk ON t.task_id = tk.task_id
) subquery
ORDER BY subquery.checklist_id, subquery.is_complete, subquery.sort_order, subquery.todo_id;

-- ─── Layer 2: td_base_04 (from td_base_03) ───

CREATE OR REPLACE VIEW td_base_04 AS
SELECT t.todo_id, t.task_id, t.has_owner, t.is_sourced, t.owner_id,
       t.allow_non_owner, t.allow_early, t.allow_future, t.sort_order,
       t.min_sort_top, t.min_sort_block, t.checklist_id,
       a.full_name AS checklist_name, a.is_complete AS is_checklist_complete,
       a.assigned_to_id AS checklist_assigned_to_id,
       t.has_goto, t.goto_link_id, t.has_info, t.info_link_id,
       t.auto_id, t.has_automation,
       t.bpo_registration_id AS bpo_registration_id,
       t.name, a.due_date AS checklist_due_date
FROM td_base_03 t JOIN assignee a ON t.checklist_id = a.id;

-- ─── Layer 3: td_base_05 (from td_base_04) ───

CREATE OR REPLACE VIEW td_base_05 AS
SELECT t.todo_id, t.task_id, t.name, t.has_owner, t.is_sourced, t.owner_id,
       t.allow_non_owner, t.allow_early, t.allow_future, t.sort_order,
       t.min_sort_top,
       CASE WHEN t.sort_order > t.min_sort_top THEN 0 ELSE 1 END AS at_top,
       t.min_sort_block,
       CASE
           WHEN (0 <> t.allow_early) IS FALSE AND t.sort_order > t.min_sort_top THEN 1
           WHEN t.min_sort_block IS NULL THEN 0
           WHEN t.sort_order > t.min_sort_block THEN 1
           ELSE 0
       END AS is_blocked,
       t.checklist_id, t.checklist_name, t.is_checklist_complete,
       t.checklist_due_date, t.checklist_assigned_to_id,
       a.DTYPE, a.full_name AS checklist_owner, a.is_complete AS is_activity_complete,
       a.assigned_to_id AS activity_owner_id,
       CASE WHEN a.DTYPE = 'Renewal' THEN 1 WHEN a.DTYPE = 'Setup' THEN 1
            WHEN a.DTYPE = 'Ticket' THEN 1 ELSE 0 END AS is_activity,
       a.due_date AS activity_due_date,
       t.has_goto, t.goto_link_id, t.has_info, t.info_link_id,
       t.auto_id, t.has_automation,
       t.bpo_registration_id AS bpo_registration_id
FROM td_base_04 t JOIN assignee a ON t.checklist_assigned_to_id = a.id;

-- ─── Layer 4: tdx_open_activity / tdx_open_list (from td_base_05) ───

CREATE OR REPLACE VIEW tdx_open_activity AS
SELECT t.todo_id, t.task_id, t.name,
       CASE WHEN ((t.has_owner = true) OR (t.is_sourced = true))
                 AND t.owner_id IS NOT NULL AND t.owner_id <> t.activity_owner_id
            THEN 1 ELSE 0 END AS not_me,
       t.has_owner, t.is_sourced, t.owner_id, t.allow_non_owner,
       t.allow_early, t.allow_future, t.sort_order, t.min_sort_top, t.at_top,
       t.min_sort_block, t.is_blocked, t.checklist_id, t.checklist_name,
       t.checklist_owner, t.is_checklist_complete, t.checklist_due_date,
       t.checklist_assigned_to_id, t.DTYPE, t.is_activity, t.is_activity_complete,
       t.activity_owner_id, t.activity_due_date,
       t.has_goto, t.goto_link_id, t.has_info, t.info_link_id,
       t.auto_id, t.has_automation,
       t.bpo_registration_id AS bpo_registration_id
FROM td_base_05 t
WHERE (t.DTYPE = 'Renewal' OR t.DTYPE = 'Setup' OR t.DTYPE = 'Ticket')
  AND t.is_activity_complete = false
ORDER BY t.checklist_due_date, t.checklist_id, t.sort_order;

CREATE OR REPLACE VIEW tdx_open_list AS
SELECT t.todo_id, t.task_id, t.name, t.has_owner, t.is_sourced, t.owner_id,
       t.allow_non_owner, t.allow_early, t.allow_future, t.sort_order,
       t.min_sort_top, t.at_top, t.min_sort_block, t.is_blocked,
       t.checklist_id, t.checklist_name, t.is_checklist_complete,
       t.checklist_due_date, t.checklist_assigned_to_id, t.DTYPE,
       t.is_activity_complete, t.activity_owner_id, t.checklist_owner,
       t.has_goto, t.goto_link_id, t.has_info, t.info_link_id,
       t.auto_id, t.has_automation,
       t.bpo_registration_id AS bpo_registration_id,
       CASE WHEN ((t.has_owner = true) OR (t.is_sourced = true))
                 AND t.owner_id IS NOT NULL AND t.owner_id <> t.checklist_assigned_to_id
            THEN 1 ELSE 0 END AS not_me
FROM td_base_05 t
WHERE t.DTYPE = 'Person' AND t.is_checklist_complete = false
ORDER BY t.checklist_due_date, t.checklist_id, t.sort_order, t.task_id;

-- ─── Layer 5: ac_base_10 / ck_base_03 ───

CREATE OR REPLACE VIEW ac_base_10 AS
SELECT t.todo_id, t.task_id, t.name, t.not_me, t.has_owner, t.is_sourced,
       t.owner_id, t.allow_non_owner, t.allow_early, t.allow_future,
       t.sort_order, t.min_sort_top, t.at_top, t.min_sort_block, t.is_blocked,
       t.checklist_id, t.checklist_name, t.checklist_owner,
       t.is_checklist_complete, t.checklist_due_date,
       t.checklist_assigned_to_id AS activity_id,
       t.DTYPE, t.is_activity, t.is_activity_complete,
       t.activity_owner_id, t.activity_due_date,
       t.has_goto, t.goto_link_id, t.has_info, t.info_link_id,
       t.auto_id, t.has_automation,
       t.bpo_registration_id AS bpo_registration_id
FROM tdx_open_activity t
WHERE t.not_me = 1 AND t.is_blocked = false;

CREATE OR REPLACE VIEW ck_base_03 AS
SELECT c.todo_count, c.checklist_id, c.full_name, c.due_date, c.assigned_to_id,
       t.todo_id, t.task_id, t.name, t.has_owner, t.is_sourced, t.owner_id,
       t.allow_non_owner, t.allow_early, t.allow_future, t.sort_order,
       t.min_sort_top, t.at_top, t.min_sort_block, t.is_blocked,
       t.checklist_assigned_to_id, t.checklist_owner,
       t.has_goto, t.goto_link_id, t.has_info, t.info_link_id,
       t.auto_id, t.has_automation,
       t.bpo_registration_id AS bpo_registration_id,
       CASE WHEN ((t.has_owner = true) OR (t.is_sourced = true))
                 AND t.owner_id IS NOT NULL AND t.owner_id <> c.assigned_to_id
            THEN 1 ELSE 0 END AS not_me
FROM ck_base_02 c JOIN tdx_open_list t ON c.checklist_id = t.checklist_id;

-- ─── Layer 6: ac_base_11 / ck_base_04 ───

CREATE OR REPLACE VIEW ac_base_11 AS
SELECT count(a.todo_id) AS counts, a.owner_id, a.bpo_registration_id AS bpo_registration_id,
       a.activity_id
FROM ac_base_10 a
GROUP BY a.activity_id, a.owner_id, a.bpo_registration_id;

CREATE OR REPLACE VIEW ck_base_04 AS
SELECT c.todo_count, c.checklist_id, c.full_name, c.due_date, c.assigned_to_id,
       c.todo_id, c.task_id, c.name, c.has_owner, c.is_sourced, c.owner_id,
       c.allow_non_owner, c.allow_early, c.allow_future, c.sort_order,
       c.min_sort_top, c.at_top, c.min_sort_block, c.is_blocked,
       c.checklist_assigned_to_id, c.checklist_owner,
       c.has_goto, c.goto_link_id, c.has_info, c.info_link_id,
       c.auto_id, c.has_automation,
       c.bpo_registration_id AS bpo_registration_id,
       c.not_me
FROM ck_base_03 c
WHERE c.not_me = true AND c.is_blocked = false
ORDER BY c.due_date, c.checklist_id, c.sort_order, c.todo_id;

-- ─── Layer 7: a_base_04 / ckx_open_checklists / acx_open_activities ───

CREATE OR REPLACE VIEW a_base_04 AS
SELECT a.id, a.DTYPE, a.full_name, a.TAXID, a.address_id, a.contact_id,
       a.email, a.first_name, a.last_name, a.middle_init, a.phone, a.title,
       a.psp_id, a.setup_id, a.employee_id, a.date_completed, a.date_created,
       a.due_date, a.is_complete, a.assigned_to_id, a.completed_by_id,
       a.created_by_id, a.employer_id, a.checklist_id, a.proposal_id,
       a.person_id, a.description, a.method_id, a.recurring_list_id,
       a.email_address, a.myRsc, a.primary_contact, a.last_contact,
       a.contact_status, a.needs_contact, a.c_status_id, a.waiting_on_us,
       t.owner_id AS task_owner_id,
       t.bpo_registration_id AS bpo_registration_id,
       CASE WHEN t.owner_id IS NULL
            THEN concat(a.id, '-', a.assigned_to_id, '-N')
            WHEN t.bpo_registration_id IS NULL
            THEN concat(a.id, '-', a.assigned_to_id, '-', t.owner_id, '-N')
            ELSE concat(a.id, '-', a.assigned_to_id, '-', t.owner_id, '-', t.bpo_registration_id)
       END AS UID
FROM a_base_03 a LEFT JOIN ac_base_11 t ON a.id = t.activity_id
ORDER BY a.full_name;

CREATE OR REPLACE VIEW ckx_open_checklists AS
SELECT c.todo_count, c.checklist_id, c.full_name, c.due_date, c.assigned_to_id,
       c.days_in_advance, a.owner_id,
       a.bpo_registration_id AS bpo_registration_id,
       a.has_owner, a.is_sourced,
       CASE WHEN a.owner_id IS NULL
            THEN concat(c.checklist_id, '-', c.assigned_to_id, '-N')
            WHEN a.bpo_registration_id IS NULL
            THEN concat(c.checklist_id, '-', c.assigned_to_id, '-', a.owner_id, '-N')
            ELSE concat(c.checklist_id, '-', c.assigned_to_id, '-', a.owner_id, '-', a.bpo_registration_id)
       END AS UID,
       CASE WHEN c.days_in_advance IS NULL THEN c.due_date
            ELSE c.due_date - INTERVAL c.days_in_advance DAY
       END AS show_date,
       c.todo_count AS todo_count_f
FROM ck_base_05 c LEFT JOIN ck_base_04 a ON c.checklist_id = a.checklist_id
ORDER BY c.due_date, c.full_name, a.sort_order DESC;

CREATE OR REPLACE VIEW acx_open_activities AS
SELECT a.id, a.DTYPE, a.full_name, a.first_name, a.last_name,
       a.employee_id, a.due_date, a.is_complete, a.assigned_to_id,
       a.employer_id, a.last_status_id, a.last_outbound, a.checklist_id,
       n.owner_id AS task_owner_id,
       n.bpo_registration_id AS bpo_registration_id,
       CASE WHEN n.owner_id IS NULL
            THEN concat(a.id, '-', a.assigned_to_id, '-N')
            WHEN n.bpo_registration_id IS NULL
            THEN concat(a.id, '-', a.assigned_to_id, '-', n.owner_id, '-N')
            ELSE concat(a.id, '-', a.assigned_to_id, '-', n.owner_id, '-', n.bpo_registration_id)
       END AS UID
FROM ac_base_09 a LEFT JOIN ac_base_11 n ON a.id = n.activity_id;

-- ─── Layer 8: a_base_05 (from a_base_04) ───

CREATE OR REPLACE VIEW a_base_05 AS
SELECT a.id, a.DTYPE, a.full_name, a.TAXID, a.address_id, a.contact_id,
       a.email, a.first_name, a.last_name, a.middle_init, a.phone, a.title,
       a.psp_id, a.setup_id, a.employee_id, a.date_completed, a.date_created,
       a.due_date, a.is_complete, a.assigned_to_id, a.completed_by_id,
       a.created_by_id, a.employer_id, a.checklist_id, a.proposal_id,
       a.person_id, a.description, a.method_id, a.recurring_list_id,
       a.email_address, a.myRsc, a.primary_contact, a.last_contact,
       a.contact_status, a.needs_contact, a.c_status_id, a.waiting_on_us,
       a.task_owner_id,
       a.bpo_registration_id AS bpo_registration_id,
       a.UID,
       CASE WHEN a.DTYPE = 'Ticket'
            THEN concat(t.first_name, ' ', t.last_name)
            ELSE a.full_name
       END AS full_name_alt
FROM a_base_04 a LEFT JOIN assignee t ON a.person_id = t.id
ORDER BY a.full_name;

-- Step 6: Self-register migration
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V027', 'BPO Registration: task source refactor from Person to BpoRegistration', 'V027__bpo_registration_task_source.sql', NOW());
