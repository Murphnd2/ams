-- V066: Agent markup on proposal pricing (proposal_price_adjustment)
--
-- Adds per-proposal, per-line agent markup on top of the shared RateTable
-- price. Sell = base (RateTable.price) + markup. Markup is upward-only
-- (markup_amount >= 0, enforced here and in application code).
--
-- Scope is deliberately per-proposal, not agency-wide: RateTable rows are
-- shared across every proposal built on that Rate, so markups must live on
-- a separate table keyed by (proposal_id, module_id, price_item_id) rather
-- than on RateTable itself. An agency-wide default markup is a possible
-- future layer — SalesDAO.getPricingWithAdjustments() is written so a
-- fall-through resolution step (proposal override -> agency default -> 0)
-- can be inserted later without changing the RateTable/adjustment shapes.
--
-- Table names verified against docs/importscript/beta_ssa_baseline_v031.sql:
--   proposal(proposal_id), servicemodule(module_id), priceitem(price_item_id).
-- Person rows live in `assignee` (SINGLE_TABLE inheritance), so created_by
-- references assignee(id) — same pattern as V050/V060/V061.
--
-- Prerequisites: proposal, servicemodule, priceitem, assignee tables (all
-- present since the base schema) and schema_version table.

-- ----------------------------------------------------------------------
-- 1. proposal_price_adjustment
-- ----------------------------------------------------------------------
CREATE TABLE proposal_price_adjustment (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    proposal_id   BIGINT         NOT NULL,
    module_id     BIGINT         NOT NULL,
    price_item_id BIGINT         NOT NULL,
    markup_amount DECIMAL(10,2)  NOT NULL DEFAULT 0,
    created_by    BIGINT         DEFAULT NULL,
    date_created  TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ppa (proposal_id, module_id, price_item_id),
    INDEX idx_ppa_proposal (proposal_id),
    CONSTRAINT fk_ppa_proposal FOREIGN KEY (proposal_id) REFERENCES proposal(proposal_id),
    CONSTRAINT fk_ppa_module FOREIGN KEY (module_id) REFERENCES servicemodule(module_id),
    CONSTRAINT fk_ppa_price_item FOREIGN KEY (price_item_id) REFERENCES priceitem(price_item_id),
    CONSTRAINT fk_ppa_created_by FOREIGN KEY (created_by) REFERENCES assignee(id),
    CONSTRAINT chk_ppa_markup_nonneg CHECK (markup_amount >= 0)
);

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V066' AS version, '2026-07-09' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V066', 'Per-proposal, per-line agent markup on pricing (proposal_price_adjustment)', 'V066__proposal_price_adjustment.sql', NOW());
