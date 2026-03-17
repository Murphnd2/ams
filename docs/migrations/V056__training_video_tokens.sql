-- V056: Training video and single-use token tables
-- Supports token-gated video streaming for training content distribution.

CREATE TABLE training_video (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename         VARCHAR(255) NOT NULL,
    title            VARCHAR(255) NOT NULL,
    description      TEXT,
    duration_seconds INT,
    active           TINYINT(1) NOT NULL DEFAULT 1,
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_training_video_filename (filename)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE video_token (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    token           VARCHAR(36) NOT NULL,
    video_id        BIGINT NOT NULL,
    recipient_name  VARCHAR(200),
    recipient_email VARCHAR(255),
    created_by      BIGINT,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    first_viewed_at DATETIME,
    last_viewed_at  DATETIME,
    view_count      INT NOT NULL DEFAULT 0,
    max_views       INT NOT NULL DEFAULT 1,
    expires_at      DATETIME,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    UNIQUE KEY uq_video_token (token),
    CONSTRAINT fk_vt_video   FOREIGN KEY (video_id)   REFERENCES training_video(id),
    CONSTRAINT fk_vt_creator FOREIGN KEY (created_by)  REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V056' AS version, '2026-03-16' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V056', 'Training video and single-use token tables', 'V056__training_video_tokens.sql', NOW());
