-- =============================================================================
-- Service Manager — Combined Production Migration
-- Date: February 20, 2026
-- Target: production beta_ssa schema
--
-- This combines three migrations applied to beta during development:
--   1. Enhancement table + join tables + ServiceModule FKs
--   2. LOS sort_order column
--   3. LOS suppressed column
--
-- RUN THIS AS A SINGLE SCRIPT ON PRODUCTION
-- =============================================================================

-- ─── 1. Enhancement table ───────────────────────────────────────────────────

CREATE TABLE enhancement (
    enhancement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(200) NOT NULL,
    short_text VARCHAR(20) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    suppressed TINYINT(1) NOT NULL DEFAULT 0,
    psp_id INT NOT NULL,
    FOREIGN KEY (psp_id) REFERENCES psp(psp_id)
);

-- ─── 2. Enhancement ↔ LOS join table ────────────────────────────────────────

CREATE TABLE enhancement_los (
    enhancement_id BIGINT NOT NULL,
    los_id BIGINT NOT NULL,
    PRIMARY KEY (enhancement_id, los_id),
    FOREIGN KEY (enhancement_id) REFERENCES enhancement(enhancement_id),
    FOREIGN KEY (los_id) REFERENCES los(los_id)
);

-- ─── 3. ApplicationSection ↔ Enhancement join table ─────────────────────────

CREATE TABLE applicationsectionenhancement (
    section_id BIGINT NOT NULL,
    enhancement_id BIGINT NOT NULL,
    PRIMARY KEY (section_id, enhancement_id),
    FOREIGN KEY (section_id) REFERENCES applicationsection(section_id),
    FOREIGN KEY (enhancement_id) REFERENCES enhancement(enhancement_id)
);

-- ─── 4. ServiceModule FKs ───────────────────────────────────────────────────

ALTER TABLE servicemodule ADD COLUMN los_id BIGINT NULL;
ALTER TABLE servicemodule ADD COLUMN enhancement_id BIGINT NULL;
ALTER TABLE servicemodule ADD CONSTRAINT fk_sm_los FOREIGN KEY (los_id) REFERENCES los(los_id);
ALTER TABLE servicemodule ADD CONSTRAINT fk_sm_enhancement FOREIGN KEY (enhancement_id) REFERENCES enhancement(enhancement_id);

-- ─── 5. LOS new columns ────────────────────────────────────────────────────

ALTER TABLE los ADD COLUMN sort_order INT NOT NULL DEFAULT 0;
ALTER TABLE los ADD COLUMN suppressed TINYINT(1) NOT NULL DEFAULT 0;

-- ─── 6. Seed enhancements ──────────────────────────────────────────────────

INSERT INTO enhancement (enhancement_id, description, short_text, sort_order, suppressed, psp_id) VALUES
    (1, 'Debit Card Services', 'Cards', 100, 0, 1),
    (2, 'Payment Services', 'Payment', 200, 0, 1),
    (3, 'Document Services', 'Docs', 300, 0, 1),
    (4, 'Multi-Plan Discounts', 'Discounts', 400, 0, 1);

-- ─── 7. Enhancement ↔ LOS associations ─────────────────────────────────────
-- Cards: FSA(6), HRA(11), HSA(9), COBRA(8), Transit(10), MERP(12), ICHRA(13), LSA(18)
-- Payment: FSA(6), HRA(11), HSA(9), COBRA(8), Transit(10), MERP(12), ICHRA(13), LSA(18)
-- Docs: FSA(6), HRA(11), HSA(9), COBRA(8), MERP(12)
-- Discounts: FSA(6), HRA(11), HSA(9), COBRA(8), Transit(10)

INSERT INTO enhancement_los (enhancement_id, los_id) VALUES
    (1,6),(1,11),(1,9),(1,8),(1,10),(1,12),(1,13),(1,18),
    (2,6),(2,11),(2,9),(2,8),(2,10),(2,12),(2,13),(2,18),
    (3,6),(3,11),(3,9),(3,8),(3,12),
    (4,6),(4,11),(4,9),(4,8),(4,10);

-- ─── 8. Backfill ServiceModule FKs ─────────────────────────────────────────
-- LOS-linked modules (matched by short_text)

UPDATE servicemodule SET los_id = 5 WHERE short_text = 'POP' AND los_id IS NULL;
UPDATE servicemodule SET los_id = 6 WHERE short_text = 'FSA' AND los_id IS NULL;
UPDATE servicemodule SET los_id = 11 WHERE short_text = 'HRA' AND los_id IS NULL;
UPDATE servicemodule SET los_id = 9 WHERE short_text = 'HSA' AND los_id IS NULL;
UPDATE servicemodule SET los_id = 8 WHERE short_text = 'COBRA' AND los_id IS NULL;
UPDATE servicemodule SET los_id = 10 WHERE short_text = 'Transit' AND los_id IS NULL;

-- Enhancement-linked modules
UPDATE servicemodule SET enhancement_id = 1 WHERE short_text = 'Cards' AND enhancement_id IS NULL;
UPDATE servicemodule SET enhancement_id = 2 WHERE short_text = 'Payment' AND enhancement_id IS NULL;
UPDATE servicemodule SET enhancement_id = 3 WHERE short_text = 'Docs' AND enhancement_id IS NULL;
UPDATE servicemodule SET enhancement_id = 4 WHERE short_text = 'Discounts' AND enhancement_id IS NULL;

-- ─── 9. ApplicationSection ↔ Enhancement links ─────────────────────────────
-- Payment section (16) → Payment enhancement (2)
-- Debit Cards section (17) → Cards enhancement (1)

INSERT INTO applicationsectionenhancement (section_id, enhancement_id) VALUES (16, 2), (17, 1);

-- ─── 10. LOS sort_order backfill ────────────────────────────────────────────

UPDATE los SET sort_order = 100 WHERE los_id = 5;
UPDATE los SET sort_order = 200 WHERE los_id = 6;
UPDATE los SET sort_order = 300 WHERE los_id = 11;
UPDATE los SET sort_order = 400 WHERE los_id = 12;
UPDATE los SET sort_order = 500 WHERE los_id = 13;
UPDATE los SET sort_order = 600 WHERE los_id = 14;
UPDATE los SET sort_order = 700 WHERE los_id = 15;
UPDATE los SET sort_order = 800 WHERE los_id = 9;
UPDATE los SET sort_order = 900 WHERE los_id = 8;
UPDATE los SET sort_order = 1000 WHERE los_id = 10;
UPDATE los SET sort_order = 1100 WHERE los_id = 16;
UPDATE los SET sort_order = 1200 WHERE los_id = 17;
UPDATE los SET sort_order = 1300 WHERE los_id = 18;
UPDATE los SET sort_order = 1400 WHERE los_id = 19;

-- ─── VERIFICATION ───────────────────────────────────────────────────────────

SELECT 'Enhancements' as what, COUNT(*) as cnt FROM enhancement
UNION ALL
SELECT 'Enhancement-LOS links', COUNT(*) FROM enhancement_los
UNION ALL
SELECT 'AppSection-Enhancement links', COUNT(*) FROM applicationsectionenhancement
UNION ALL
SELECT 'ServiceModules with LOS FK', COUNT(*) FROM servicemodule WHERE los_id IS NOT NULL
UNION ALL
SELECT 'ServiceModules with Enh FK', COUNT(*) FROM servicemodule WHERE enhancement_id IS NOT NULL
UNION ALL
SELECT 'LOS with sort_order > 0', COUNT(*) FROM los WHERE sort_order > 0;
