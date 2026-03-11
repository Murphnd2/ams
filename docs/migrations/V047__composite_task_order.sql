-- V047: Composite task order table for cross-sequence ordering
-- Stores a PSP-level master ordering of tasks per activity category.
-- When multiple sequences of the same type (Setup, Renewal, Ticket) merge
-- for a new activity, this ordering is used instead of per-sequence sort_order values.

CREATE TABLE IF NOT EXISTS composite_task_order (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    psp_id      BIGINT NOT NULL,
    group_id    INT NOT NULL,
    task_id     BIGINT NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_cto_psp FOREIGN KEY (psp_id) REFERENCES assignee(person_id),
    CONSTRAINT fk_cto_group FOREIGN KEY (group_id) REFERENCES templategroup(group_id),
    CONSTRAINT fk_cto_task FOREIGN KEY (task_id) REFERENCES task(task_id),
    UNIQUE INDEX uq_cto_psp_group_task (psp_id, group_id, task_id)
);

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V047', 'Composite task order table for cross-sequence ordering', 'V047__composite_task_order.sql', NOW());
