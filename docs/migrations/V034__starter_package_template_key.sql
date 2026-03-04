-- V034: Add template_key for starter package duplicate detection
-- Prerequisite: V003 (applicationsection table exists)

ALTER TABLE applicationsection ADD COLUMN template_key VARCHAR(50) NULL;
ALTER TABLE applicationsection ADD UNIQUE INDEX uq_section_template_psp (template_key, psp_id);

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V034', 'Add template_key to applicationsection for starter packages', 'V034__starter_package_template_key.sql', NOW());
