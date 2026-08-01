-- =====================================================================
-- V084__zip_county_crosswalk.sql
-- =====================================================================
-- Purpose : ZIP -> county crosswalk, so an agent can type the ZIP they
--           actually have instead of picking a county FIPS they do not.
--           Structure only; V085 carries the data.
--
-- Notes   : Many-to-many BY CONSTRUCTION. A ZIP can straddle a county
--           line, and in Texas 686 of 1,992 ZCTAs (34%) do. That is not
--           an edge case to round off -- it is a third of the state, and
--           it is why the primary key is composite rather than `zip`
--           alone.
--
--           ** The consumer must never auto-select on a multi-row ZIP. **
--           A wrong county returns the wrong rates, which puts a wrong
--           number in front of a client, and nothing downstream detects
--           it. ZipCountyResolver returns every match and chooses none;
--           the UI asks the agent. See
--           docs/analysis/ichra_flow_and_handoffs.md S5.
--
--           `zip` is CHAR(5), not an integer. An integer column silently
--           destroys every leading-zero ZIP -- the whole of New England,
--           New Jersey and Puerto Rico. Texas ZIPs happen to start at 7
--           so this would not have bitten today; it would have bitten on
--           the first expansion migration, in production, quietly.
--
--           `county_fips` is CHAR(5) to match county_reference.county_fips
--           (V076) exactly, so the two join without collation or width
--           surprises.
--
--           NO FOREIGN KEY to county_reference -- deliberate. That table
--           is seeded for TEXAS ONLY (V076, 254 rows, verified 2026-08-01);
--           it is not nationally complete. An FK would reject every valid
--           crosswalk row for a state whose counties are not yet seeded,
--           turning a later national expansion into a migration ordering
--           puzzle. The two tables are independent reference data cut
--           from the same Census vintage.
--
--           `land_area_ratio` is the share of the ZCTA's LAND AREA inside
--           the county. ** It is not a population or address share. **
--           HUD's crosswalk carries a residential (address-count) ratio;
--           this is not that, and is deliberately not named res_ratio. It
--           exists ONLY to order the choices a crossing ZIP presents to
--           the agent, most-land-first. It must never auto-select, and no
--           figure shown to a user may be derived from it. NULL where the
--           source reports zero/absent land area -- an unknown ratio is
--           not a zero one, and ORDER BY must not rank it least by
--           accident.
--
--           No secondary index. The composite PK (zip, county_fips) is
--           itself the index for the only lookup the resolver performs --
--           equality on `zip`, leftmost column. A separate index on `zip`
--           would be redundant. There is no county -> ZIP direction: that
--           is county_reference.representative_zip's job (V076).
--
-- Feature : T74 ZIP intake, part 1 (data layer). Spec:
--           docs/analysis/ichra_flow_and_handoffs.md S5.
--
-- Prereqs : Base schema (V024 baseline). Independent of V076 -- related
--           by data, not by constraint.
--
-- Scope   : All instances -- national reference data, inert where unused.
--           Not PSP-scoped, matching the V074/V075/V076 precedent.
--
-- Rollback: DROP TABLE zip_county;
-- =====================================================================

CREATE TABLE zip_county (
    zip             CHAR(5)       NOT NULL,
    county_fips     CHAR(5)       NOT NULL,
    land_area_ratio DECIMAL(7,6)  NULL,      -- land share, NOT population; ordering only
    PRIMARY KEY (zip, county_fips)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V084' AS version, '2026-08-01' AS updated;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V084', 'ZIP to county crosswalk table for T74 ZIP intake', 'V084__zip_county_crosswalk.sql', NOW());
