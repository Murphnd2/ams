-- ======================================================================
-- V070__agency_parent.sql
-- Self-referential parent link on agency (GA -> sub-agency hierarchy).
--
-- Adds a nullable parent_agency_id to `agency`. An agency with a non-null
-- parent_agency_id is a "sub-agency" downstream of a general agency (GA);
-- an agency with NULL parent (all existing rows) is top-level and behaves
-- exactly as before. The relationship is managed at the application layer
-- via the JPA @ManyToOne Agency.parentAgency mapping.
--
-- No hard FOREIGN KEY constraint is added here, matching the established
-- idiom of prior agency migrations (V067/V068/V069 are column/index-only).
-- A non-unique index supports child-lookup queries (sub-agencies of a GA).
--
-- Prerequisites: agency table exists (base schema). No dependency on other
-- V06x migrations.
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. Add nullable self-referential parent pointer + child-lookup index
-- ----------------------------------------------------------------------
ALTER TABLE agency ADD COLUMN parent_agency_id BIGINT NULL;
ALTER TABLE agency ADD INDEX ix_agency_parent (parent_agency_id);

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V070' AS version, '2026-07-10' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V070', 'Self-referential agency parent link (GA -> sub-agency hierarchy): agency.parent_agency_id nullable + child-lookup index', 'V070__agency_parent.sql', NOW());
