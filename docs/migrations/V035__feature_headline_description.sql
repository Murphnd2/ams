-- =============================================================================
-- V035 — Feature headline column + description widening
-- Adds a headline column for short punchy summary text (like ModuleDetail.bulletPoint)
-- Widens description from varchar(500) to varchar(2000) for paragraph content
-- =============================================================================

-- Add headline column (short punchy text shown as a bullet point)
ALTER TABLE feature ADD COLUMN headline VARCHAR(200) DEFAULT NULL AFTER description;

-- Widen description from varchar(500) to varchar(2000) for paragraph content
ALTER TABLE feature MODIFY COLUMN description VARCHAR(2000) NOT NULL;

-- Self-register migration
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V035', 'Feature headline column and description widening', 'V035__feature_headline_description.sql', NOW());
