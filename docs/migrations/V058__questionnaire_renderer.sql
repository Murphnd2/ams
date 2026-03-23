-- V058: Add renderer column to questionnaire table
-- Allows questionnaires to specify a custom rendering JSP instead of the standard flat form.
-- NULL or 'standard' = fillQuestionnaire.jsp (current behavior)
-- 'ndt_125' = fillQuestionnaire_ndt125.jsp (multi-page wizard with conditional logic)
-- Future custom renderers follow the same pattern.

ALTER TABLE questionnaire
    ADD COLUMN renderer VARCHAR(30) DEFAULT NULL AFTER external_url;

-- Widen label column to support long compliance question text
ALTER TABLE questionnaire_field
    MODIFY COLUMN label VARCHAR(500);

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V058', 'Add renderer column to questionnaire', 'V058__questionnaire_renderer.sql', NOW());
