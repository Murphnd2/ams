-- ======================================================================
-- V071__agency_quote_token.sql
-- Per-agency public quote token for RequestQuote sub-agency attribution.
--
-- Adds a nullable, unique quote_token to `agency`. A non-null token lets a
-- RequestQuote link (https://<host>/RequestQuote?k=<token>) attribute the
-- resulting Opportunity (and drive the services/LOS list) to that specific
-- agency, independent of the white-label host the page is served on --
-- e.g. a GA sub-agency riding the GA's branded site. Branding stays
-- host-driven. Tokens are minted on demand (UUID) and revocable by
-- regeneration. Existing rows have NULL (no link issued) and are unaffected.
--
-- MySQL permits multiple NULLs under a UNIQUE index, so all existing NULL
-- rows coexist; only non-null tokens must be unique.
--
-- Prerequisites: agency table exists (base schema). No dependency on other
-- V07x migrations.
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. Add nullable-unique public quote token
-- ----------------------------------------------------------------------
ALTER TABLE agency ADD COLUMN quote_token VARCHAR(64) NULL;
ALTER TABLE agency ADD UNIQUE INDEX uq_agency_quote_token (quote_token);

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V071' AS version, '2026-07-10' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V071', 'Per-agency public quote token (agency.quote_token unique) for RequestQuote sub-agency attribution', 'V071__agency_quote_token.sql', NOW());
