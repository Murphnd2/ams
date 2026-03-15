-- V054: Super User Dashboard — managed_installation table for centralized deployment management
-- Tracks remote PSP/BPO installations that this master node manages

CREATE TABLE IF NOT EXISTS managed_installation (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    installation_name     VARCHAR(100) NOT NULL,
    installation_url      VARCHAR(255) NOT NULL,
    system_type           VARCHAR(10)  NOT NULL COMMENT 'PSP or BPO',
    api_token_outbound    VARCHAR(64)            COMMENT 'Token master sends TO installation',
    api_token_inbound     VARCHAR(64)            COMMENT 'Token installation sends TO master',
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, ACTIVE, DISCONNECTED',
    last_heartbeat        DATETIME,
    last_schema_version   VARCHAR(20),
    last_app_version      VARCHAR(20),
    last_user_count       INT,
    date_registered       DATE         NOT NULL,
    date_approved         DATE,
    date_disconnected     DATE,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    notes                 TEXT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V054', 'Super User Dashboard — managed_installation table', 'V054__managed_installation.sql', NOW());
