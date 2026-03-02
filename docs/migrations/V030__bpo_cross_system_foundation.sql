-- =============================================================================
-- V030: BPO Cross-System Foundation
--
-- 1a. Alter bpo_registration — add API token and partnership columns (PSP side)
-- 1b. Create psp_clients table (BPO side — tracks PSP clients)
-- 1c. Create delegated_todo table (BPO side — local copy of tasks from PSPs)
-- 1d. Add todo_guid to todo_note
-- =============================================================================

-- 1a. bpo_registration: API tokens, partner URL, date columns
ALTER TABLE bpo_registration ADD COLUMN api_token_outbound VARCHAR(64) NULL;
ALTER TABLE bpo_registration ADD COLUMN api_token_inbound VARCHAR(64) NULL;
ALTER TABLE bpo_registration ADD COLUMN partner_url VARCHAR(255) NULL;
ALTER TABLE bpo_registration ADD COLUMN date_requested DATE NULL;
ALTER TABLE bpo_registration ADD COLUMN date_approved DATE NULL;
ALTER TABLE bpo_registration ADD COLUMN date_disconnected DATE NULL;

-- 1b. psp_clients (BPO side)
CREATE TABLE psp_clients (
    client_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    psp_name VARCHAR(100) NOT NULL,
    psp_url VARCHAR(255) NOT NULL,
    api_token_outbound VARCHAR(64) NULL,
    api_token_inbound VARCHAR(64) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    auto_accept_tasks BOOLEAN NOT NULL DEFAULT FALSE,
    date_requested DATE NULL,
    date_approved DATE NULL,
    date_disconnected DATE NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- 1c. delegated_todo (BPO side)
CREATE TABLE delegated_todo (
    delegated_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    todo_guid VARCHAR(36) NOT NULL,
    psp_client_id BIGINT NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    task_description TEXT NULL,
    due_date DATE NULL,
    goto_link VARCHAR(500) NULL,
    info_link VARCHAR(500) NULL,
    activity_type VARCHAR(30) NULL,
    activity_name VARCHAR(255) NULL,
    employer_name VARCHAR(255) NULL,
    assigned_to_id BIGINT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_by_id BIGINT NULL,
    completed_date DATE NULL,
    is_reverted BOOLEAN NOT NULL DEFAULT FALSE,
    last_sync_timestamp DATETIME NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    date_received DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_delegated_todo_guid (todo_guid),
    CONSTRAINT fk_delegated_psp_client FOREIGN KEY (psp_client_id) REFERENCES psp_clients(client_id),
    CONSTRAINT fk_delegated_assigned_to FOREIGN KEY (assigned_to_id) REFERENCES assignee(id),
    CONSTRAINT fk_delegated_completed_by FOREIGN KEY (completed_by_id) REFERENCES assignee(id)
);

-- 1d. todo_note: add GUID for cross-system note sync
ALTER TABLE todo_note ADD COLUMN todo_guid VARCHAR(36) NULL;
CREATE INDEX idx_todo_note_guid ON todo_note(todo_guid);

-- 1e. Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V030', 'BPO cross-system foundation: psp_clients, delegated_todo, bpo_registration API columns, todo_note GUID', 'V030__bpo_cross_system_foundation.sql', NOW());
