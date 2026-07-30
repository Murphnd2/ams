-- =====================================================================
-- V074__rating_area_rate_cache.sql
-- =====================================================================
-- Purpose : Per-county-per-age premium cache for the A1 ICHRA rating
--           illustration. Warmed by a scheduled background job (one
--           HealthSherpa API call per county per plan year, not per age
--           -- see AgeCurve), read by the illustration servlet. No live
--           HealthSherpa API call happens on an agent's request path.
--
-- Notes   : uses_tobacco is present but UNUSED pending open item O19
--           (whether the ICHRA affordability LCSP premium is tobacco-
--           loaded) -- adding one column now avoids a second migration
--           later if the answer is yes. All rows currently write 0.
--           lcsp_premium is the LOWEST-cost silver plan's premium (drives
--           ICHRA affordability); benchmark_silver_premium is the
--           SECOND-lowest-cost silver plan's premium (drives APTC) --
--           these are two different plans and both are needed.
--           source_env records whether the row was fetched against the
--           HealthSherpa staging or production environment.
--
-- Feature : A1 ICHRA rating illustration (Phase B-1b).
--
-- Prereqs : Base schema (V024 baseline). No prior migration required.
--
-- Scope   : SSA production instance only.
--
-- Rollback: DROP TABLE rating_area_rate_cache;
-- =====================================================================

CREATE TABLE rating_area_rate_cache (
    id                        BIGINT       NOT NULL AUTO_INCREMENT,
    plan_year                 SMALLINT     NOT NULL,
    county_fips                CHAR(5)      NOT NULL,
    state                     CHAR(2)      NOT NULL,
    age                       TINYINT      NOT NULL,             -- 21-64
    uses_tobacco              TINYINT(1)   NOT NULL DEFAULT 0,   -- unused pending O19, see header
    market_low_premium        DECIMAL(8,2) NULL,
    market_high_premium       DECIMAL(8,2) NULL,
    lcsp_premium               DECIMAL(8,2) NULL,                -- lowest-cost silver plan (ICHRA affordability)
    benchmark_silver_premium  DECIMAL(8,2) NULL,                 -- 2nd-lowest-cost silver plan (APTC)
    lowest_bronze_premium     DECIMAL(8,2) NULL,
    carrier_count              SMALLINT     NULL,
    plan_count                SMALLINT     NULL,
    fetched_at                DATETIME     NOT NULL,
    source_env                 VARCHAR(16)  NOT NULL,             -- STAGING / PRODUCTION
    PRIMARY KEY (id),
    UNIQUE KEY uq_rarc_year_county_age_tobacco (plan_year, county_fips, age, uses_tobacco),
    KEY idx_rarc_county_year (county_fips, plan_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V074' AS version, '2026-07-30' AS updated;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V074', 'Rating-area rate cache for A1 ICHRA illustration', 'V074__rating_area_rate_cache.sql', NOW());
