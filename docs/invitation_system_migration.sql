-- =============================================================================
-- Invitation System — Production Migration
-- Date: February 21, 2026
-- Target: production beta_ssa schema
--
-- Prerequisites: None (standalone migration)
--
-- Changes:
--   1. Add manager_id column to agency table
--   2. Create invitation table
--   3. Add person_id column to invitation table
--
-- RUN THIS AS A SINGLE SCRIPT ON PRODUCTION
-- =============================================================================

-- ─── 1. Agency manager FK ───────────────────────────────────────────────────

ALTER TABLE agency ADD COLUMN manager_id BIGINT NULL;
ALTER TABLE agency ADD CONSTRAINT fk_agency_manager
  FOREIGN KEY (manager_id) REFERENCES assignee(id);

-- ─── 2. Invitation table ────────────────────────────────────────────────────

CREATE TABLE invitation (
  invitation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  guid VARCHAR(36) NOT NULL UNIQUE,
  email VARCHAR(200) NOT NULL,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  agency_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  invited_by BIGINT NOT NULL,
  person_id BIGINT NULL,
  date_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  date_expires TIMESTAMP NOT NULL,
  date_accepted TIMESTAMP NULL,
  is_used BOOLEAN DEFAULT FALSE,
  CONSTRAINT fk_invitation_agency FOREIGN KEY (agency_id) REFERENCES agency(agency_id),
  CONSTRAINT fk_invitation_invited_by FOREIGN KEY (invited_by) REFERENCES assignee(id),
  CONSTRAINT fk_invitation_person FOREIGN KEY (person_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- =============================================================================
-- Standard UserRole seed data (reference for new PSP installations)
-- These should already exist. INSERT IGNORE ensures no duplicates.
-- =============================================================================

INSERT IGNORE INTO userrole (id, description) VALUES
  (1, 'PSP User'),
  (2, 'Agent'),
  (3, 'Client'),
  (4, 'Applicant'),
  (5, 'PSP Admin'),
  (6, 'Pending Agent'),
  (7, 'Anonymous'),
  (8, 'Agency Admin'),
  (9, 'PSP Super User');
