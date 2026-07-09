-- V067: Per-agency enable flag for proposal markup (agency.markup_enabled)
--
-- The V066 agent-markup feature (proposal_price_adjustment) is currently available to
-- any agent/agency-admin/PSP-admin on every proposal, with no per-agency restriction.
-- This adds a capability flag on the agency itself: markup is hidden and disabled by
-- default for every agency (existing and future) and must be explicitly turned on.
--
-- NOT NULL DEFAULT 0 — mirrors agency.suppressed's shape/pattern. No backfill needed;
-- every existing row and every future INSERT defaults OFF.
--
-- Scope: this column only gates the EDIT control (proposalDetail.jsp markup inputs +
-- Save Markup button) and the ProposalDetail "saveMarkup" save endpoint. It does NOT
-- touch SalesDAO.getPricingWithAdjustments() or the sell-price math — a proposal with
-- markups already saved under V066 continues to compute sellPrice = base + markup even
-- if its agency is later disabled (or was never enabled). Only the ability to add/change
-- a markup is gated, not the pricing already on record.
--
-- Prerequisites: V066 (proposal_price_adjustment table + markup feature).

-- ----------------------------------------------------------------------
-- 1. Add markup_enabled to agency
-- ----------------------------------------------------------------------
ALTER TABLE agency ADD COLUMN markup_enabled TINYINT(1) NOT NULL DEFAULT 0;

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V067' AS version, '2026-07-09' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V067', 'Per-agency enable flag for proposal markup (agency.markup_enabled, default OFF)', 'V067__agency_markup_enabled.sql', NOW());
