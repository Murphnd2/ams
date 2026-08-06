-- V090: ICHRA payload JSON column on proposal_ichra_snapshot (proposal_ichra_snapshot.payload_json)
--
-- T165. Carries the JSON payload FlaggedEnhancementResolver needs to render a system_managed
-- enhancement's scoped section, plus the affordability, age-band, and plan-landscape data neither
-- proposal_ichra_snapshot's nor proposal_ichra_snapshot_band's existing columns carry.
-- See docs/analysis/S19D_ichra_payload_spec.md.
--
-- NULLABLE, NO DEFAULT, NO BACKFILL -- every row written before this column existed reads NULL;
-- FlaggedEnhancementResolver's null-check (unchanged fail-closed behaviour) treats that identically
-- to "no ICHRA hand-off at all". Every snapshot row written by the two existing fail-closed writers
-- (attachRangeSnapshot / attachAgeBandSnapshot) after this ships always populates at least
-- {"schemaVersion":1,"provenance":{...}} -- provenance is derivable from inputs the writers already
-- require to reach ProposalIchraSnapshotDAO.save() at all, so an empty-but-non-null payload is the
-- floor, not NULL.
--
-- Sized at MEDIUMTEXT against the 2,000-plan worst case (PER_PAGE=100 x MAX_PAGES=20,
-- HealthSherpaService.java:47/52), not the 65-plan reference county -- see
-- docs/analysis/S19D_ichra_payload_spec.md sec 2 for the arithmetic. TEXT's 64KB ceiling is
-- undersized for that worst case by roughly 9x.
--
-- Prerequisites: V079 (proposal_ichra_snapshot).

-- ----------------------------------------------------------------------
-- 1. Add payload_json to proposal_ichra_snapshot
-- ----------------------------------------------------------------------
ALTER TABLE proposal_ichra_snapshot
    ADD COLUMN payload_json MEDIUMTEXT NULL;

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V090' AS version, '2026-08-05' AS updated;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V090', 'ICHRA JSON payload column on proposal_ichra_snapshot (payload_json, MEDIUMTEXT, nullable, no backfill)', 'V090__proposal_ichra_payload.sql', NOW());
