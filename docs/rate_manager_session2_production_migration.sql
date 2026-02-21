-- =============================================================================
-- Rate Manager Session 2 — Production Migration
-- Date: February 20, 2026
-- Target: production beta_ssa schema
--
-- Adds per-rate sort_order to ratetable and backfills from servicemodule
--
-- RUN THIS AS A SINGLE SCRIPT ON PRODUCTION
-- =============================================================================

-- ─── 1. Add sort_order column to ratetable ──────────────────────────────────

ALTER TABLE ratetable ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

-- ─── 2. Backfill from servicemodule sort_order ──────────────────────────────

SET SQL_SAFE_UPDATES = 0;

UPDATE ratetable rt
    INNER JOIN servicemodule sm ON rt.module_id = sm.module_id
SET rt.sort_order = sm.sort_order;

SET SQL_SAFE_UPDATES = 1;

-- ─── VERIFICATION ───────────────────────────────────────────────────────────

SELECT rt.rate_id, sm.description, rt.sort_order, COUNT(*) as row_count
FROM ratetable rt
    INNER JOIN servicemodule sm ON rt.module_id = sm.module_id
GROUP BY rt.rate_id, sm.description, rt.sort_order
ORDER BY rt.rate_id, rt.sort_order;
