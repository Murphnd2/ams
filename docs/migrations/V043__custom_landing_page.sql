-- V043: Add text_value column to constant table for large text content (custom landing page HTML)
-- The existing 'value' column remains unchanged for all other constants.
-- The new 'text_value' column supports TEXT-sized content for HTML storage.

ALTER TABLE constant ADD COLUMN text_value TEXT DEFAULT NULL;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V043', 'Add text_value column to constant for custom landing page HTML', 'V043__custom_landing_page.sql', NOW());
