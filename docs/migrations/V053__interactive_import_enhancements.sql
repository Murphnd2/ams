-- =============================================================================
-- V053: Interactive import enhancements
-- Adds update_mode and mapping_status to import_file_type.
-- Adds is_fk and fk_entity_type to import_field_mapping.
-- =============================================================================

-- ImportFileType enhancements
ALTER TABLE import_file_type
    ADD COLUMN update_mode VARCHAR(20) NOT NULL DEFAULT 'CREATE_AND_UPDATE',
    ADD COLUMN mapping_status VARCHAR(10) NOT NULL DEFAULT 'PENDING';

-- ImportFieldMapping FK awareness
ALTER TABLE import_field_mapping
    ADD COLUMN is_fk TINYINT(1) NOT NULL DEFAULT 0,
    ADD COLUMN fk_entity_type VARCHAR(20) NULL;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V053', 'Interactive import enhancements: update mode, mapping status, FK flags',
        'V053__interactive_import_enhancements.sql', NOW());
