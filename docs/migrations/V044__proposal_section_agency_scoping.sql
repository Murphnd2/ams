-- V044: Add agency scoping to proposal_section for TITLE/CLOSING overrides
-- Allows PSP Admins to create agency-specific Title and Closing pages.
-- When agency_id IS NULL, the section is the default.
-- When agency_id is set, the section overrides the default for that agency's proposals.

ALTER TABLE proposal_section ADD COLUMN agency_id BIGINT DEFAULT NULL AFTER psp_id;

ALTER TABLE proposal_section ADD CONSTRAINT fk_ps_agency
    FOREIGN KEY (agency_id) REFERENCES agency(agency_id) ON DELETE SET NULL;

-- Unique constraint: only one TITLE or CLOSING per (psp_id, agency_id) combination
-- NULL agency_id = default, non-NULL = agency override
-- Note: MySQL treats NULL as distinct in unique indexes, so (psp_id, NULL) won't conflict
-- with (psp_id, NULL) — we rely on application logic to enforce one default per type.
-- The unique index covers the agency-scoped case: one TITLE per agency per PSP.
ALTER TABLE proposal_section ADD UNIQUE INDEX uq_ps_psp_agency_type (psp_id, agency_id, section_type);

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V044', 'Add agency scoping to proposal_section for TITLE/CLOSING overrides', 'V044__proposal_section_agency_scoping.sql', NOW());
