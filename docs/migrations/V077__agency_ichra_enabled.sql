-- V077: Per-agency enable flag for ICHRA capability access (agency.ichra_enabled)
--
-- Backs IchraAccessResolver's entitlement check: PSP admin is always entitled; any
-- other caller is entitled only if their resolved agency has this flag set. Adds a
-- capability flag on the agency itself, mirroring V067's markup_enabled precedent —
-- ICHRA is hidden and disabled by default for every agency (existing and future) and
-- must be explicitly turned on.
--
-- NOT NULL DEFAULT 0 — mirrors agency.suppressed/agency.markup_enabled's shape/pattern.
-- No backfill. No agency is entitled by this migration; entitlement is a deployment-time
-- decision made per agency (e.g. UPDATE agency SET ichra_enabled = 1 WHERE agency_id = ?).
--
-- Scope: this column only gates IchraAccessResolver's answer, which in turn gates
-- IllustrationServlet and IchraHome. It does not touch any catalog data (LOS,
-- ServiceItem, PlanType, ServiceModule, RateTable) — those are configured separately
-- through the admin UI and are independent of this flag.
--
-- Prerequisites: none.

-- ----------------------------------------------------------------------
-- 1. Add ichra_enabled to agency
-- ----------------------------------------------------------------------
ALTER TABLE agency
    ADD COLUMN ichra_enabled TINYINT(1) NOT NULL DEFAULT 0;

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V077' AS version, '2026-07-31' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V077', 'Per-agency enable flag for ICHRA capability access (agency.ichra_enabled, default OFF)', 'V077__agency_ichra_enabled.sql', NOW());
