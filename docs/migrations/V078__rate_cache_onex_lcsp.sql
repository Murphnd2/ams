-- V078: On-exchange LCSP and benchmark-silver columns (rating_area_rate_cache)
--
-- T44. ICHRA affordability keys on the lowest-cost silver plan offered ON the Exchange,
-- but lcsp_premium/benchmark_silver_premium are derived from HealthSherpa's off_ex:true
-- response (see V074 header) -- a different, cheaper plan set. Confirmed via a live
-- staging probe against Hopkins TX (FIPS 48223, plan year 2026, 2026-07-31):
--
--     Age 40 lowest silver:  off-exchange $489.38  vs.  on-exchange $705.37
--     Age 21 lowest silver:  off-exchange $382.93  vs.  on-exchange $551.93
--
-- The off-exchange figure UNDERSTATES the true LCSP by ~44% at age 40 -- the dangerous
-- direction, since a too-low LCSP lowers the computed affordability threshold and can
-- make an unaffordable offer look affordable. This adds ON-EXCHANGE counterparts so
-- affordability (build item 9) can key on the correct pair. lcsp_premium and
-- benchmark_silver_premium are UNCHANGED -- they remain the off-exchange figures the
-- illustration already displays, with their existing meaning, name and data intact.
--
-- NULLABLE, NO DEFAULT, NO BACKFILL -- mirrors V077's shape/pattern. Existing rows
-- keep their off-exchange values and carry NULL on-exchange values until the next warm
-- run populates them; the warm service's on-exchange derivation is added separately
-- (RateCacheWarmService), not by this script. A failed on-exchange call at warm time
-- leaves these two columns NULL for the affected county-year without failing the
-- off-exchange warm.
--
-- Type/precision copied exactly from lcsp_premium/benchmark_silver_premium (V074):
-- DECIMAL(8,2) NULL.
--
-- Prerequisites: V074 (rating_area_rate_cache).

-- ----------------------------------------------------------------------
-- 1. Add onex_lcsp_premium and onex_benchmark_silver_premium to rating_area_rate_cache
-- ----------------------------------------------------------------------
ALTER TABLE rating_area_rate_cache
    ADD COLUMN onex_lcsp_premium              DECIMAL(8,2) NULL,
    ADD COLUMN onex_benchmark_silver_premium  DECIMAL(8,2) NULL;

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V078' AS version, '2026-07-31' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V078', 'On-exchange LCSP and benchmark-silver columns for T44 (rating_area_rate_cache.onex_lcsp_premium/onex_benchmark_silver_premium, nullable, no backfill)', 'V078__rate_cache_onex_lcsp.sql', NOW());
