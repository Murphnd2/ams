-- V068: Host-header custom agency landing pages (agency.landing_host, agency.landing_html)
--
-- Extends the V043 PSP-wide custom-landing feature to a per-agency branded front door.
-- When AMS is reached over a non-PSP host (anything other than the PSP hosts configured
-- in ssa.properties PSP_HOSTS, e.g. swbd.superiorstate.net), the login servlet looks up
-- an agency by that Host header and serves that agency's landing_html instead of the
-- default landing/login page. PSP hosts, hosts with no matching agency, and matched
-- agencies with blank landing_html all fall through to today's behavior, unchanged.
--
-- Storage (one host per agency to start):
--   landing_host  VARCHAR(255) NULL, UNIQUE  — the vanity host, stored lowercase.
--                 NULL for agencies with no branded front door. MySQL treats multiple
--                 NULLs as distinct, so non-opted-in agencies never collide on the index.
--   landing_html  MEDIUMTEXT  NULL          — sanitized landing-page HTML (sanitized on
--                 save via net.superiorstate.ams.data.util.LandingSafe; ~16MB ceiling
--                 comfortably exceeds TEXT for pages with inline base64 images / <style>).
--
-- The white-label switch is the PRESENCE of non-blank landing_html — there is no separate
-- enable flag (unlike V067 markup_enabled).
--
-- Cache: AmsDataGlobal builds a host -> agency_id map from the already-cached agency list
-- (skipping suppressed agencies, V057), rebuilt after every AgencyAction edit via the
-- existing refreshSalesData() reload. No new invalidation plumbing.
--
-- Prerequisites: V057 (agency.suppressed), V067 (agency.markup_enabled).

-- ----------------------------------------------------------------------
-- 1. Add landing_host + landing_html to agency
-- ----------------------------------------------------------------------
ALTER TABLE agency ADD COLUMN landing_host VARCHAR(255) NULL;
ALTER TABLE agency ADD COLUMN landing_html MEDIUMTEXT NULL;

-- Fast, unique lookup by host. NULLs are distinct in MySQL, so agencies without a
-- branded front door do not conflict.
ALTER TABLE agency ADD UNIQUE INDEX uq_agency_landing_host (landing_host);

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V068' AS version, '2026-07-09' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V068', 'Host-header custom agency landing pages (agency.landing_host unique + landing_html)', 'V068__agency_landing_host.sql', NOW());
