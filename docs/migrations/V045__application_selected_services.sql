-- V045: Track which LOS and Enhancements the applicant selected for their application
-- Prerequisite: V044

-- Store selected LOS IDs as comma-separated string (simple, no join table needed for public form)
ALTER TABLE application ADD COLUMN selected_los_ids VARCHAR(500) NULL;

-- Store selected Enhancement IDs as comma-separated string
ALTER TABLE application ADD COLUMN selected_enhancement_ids VARCHAR(500) NULL;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V045', 'Application selected LOS and Enhancement IDs',
        'V045__application_selected_services.sql', NOW());
