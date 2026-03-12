-- V050: Add default_assignee_id to psp_clients for auto-assignment of incoming tasks
-- When auto-accept is enabled, new tasks from this PSP will be assigned to the default assignee
-- unless a prior instance of a recurring/required-sequence task was assigned to someone else.

ALTER TABLE psp_clients ADD COLUMN default_assignee_id BIGINT NULL;
ALTER TABLE psp_clients ADD CONSTRAINT fk_psp_client_default_assignee
    FOREIGN KEY (default_assignee_id) REFERENCES person(id);

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V050', 'BPO default assignee per PSP client', 'V050__bpo_default_assignee.sql', NOW());
