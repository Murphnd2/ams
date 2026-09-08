-- V095: Summit plan template mapping — the PSP-manageable form of SUMMIT_PLAN_TEMPLATES
--
-- Moves the elected-ServiceItem-to-Summit-plan-template mapping out of an `ssa.properties`
-- string and into a PSP-scoped table. The property (S28-B) works, but changing it needs
-- server filesystem access and a Tomcat restart, which puts a routine sales-catalogue
-- decision in the deployment path. This table is the same mapping, editable by a PSP admin
-- once S31-E builds the screen (T202).
--
-- ⚠️ THE PROPERTY IS NOT RETIRED BY THIS MIGRATION. SummitPlanTemplateResolver reads this
-- table first and falls back to SUMMIT_PLAN_TEMPLATES when it returns no active row for the
-- PSP. That fallback is what makes this table safe to create empty: an installation that
-- takes this migration and enters no rows keeps exactly the behaviour it had, byte for byte.
-- Until T202 ships there is no way to populate this table from the UI, so the property is
-- still the only working path on every installation.
--
-- Scoped by `psp_id BIGINT` FK to `assignee(id)`, mirroring `templatepurpose.psp_id` exactly
-- — PSP extends Assignee under single-table inheritance, so every FK to a PSP targets
-- `assignee(id)` and not a `psp` table. ⚠️ Unlike `templatepurpose.psp_id`, this column is
-- NOT NULL: the resolver reads by PSP, so a row with a null PSP would belong to no
-- installation and could never be read back. There is no global/shared mapping tier and none
-- is implied by a nullable column here.
--
-- `service_item_id` is INT, not BIGINT, because `templatepurpose.purpose_id` is `int`.
--
-- ⚠️ ONE SUMMIT PLAN PER ELECTED ServiceItem, AND THE CONSTRAINT IS THE DECISION.
-- `uq_summit_plan_template_map_psp_service` on (psp_id, service_item_id) is where the 1:1
-- rule lives. It is deliberately NOT the primary key: the PK is a surrogate, so if a future
-- decision lets one elected item fan out to several Summit plans, the reversal is
--     ALTER TABLE summit_plan_template_map DROP INDEX uq_summit_plan_template_map_psp_service;
-- against live rows, rather than rebuilding a table whose identity changed. S31-C established
-- that the two Ins125 PremiumPath rails will be two separate ServiceItem rows in the sales
-- catalogue rather than one item fanning out, which is what makes 1:1 correct today.
--
-- `sort_order` is emit order, matching the config-order-is-emit-order rule the property
-- already carries. `label` is nullable and falls back to `key_segment` at read time, which is
-- the property's existing rule for an absent or blank fourth field — the fallback is not
-- materialised into this column.
--
-- `is_active` retires a mapping without deleting it. ⚠️ The unique constraint does NOT
-- include `is_active`, so deactivating a row does not free its (psp_id, service_item_id) for
-- a second row — re-point the existing row instead. That is intentional: two rows for one
-- service item, one active, is the fan-out shape this constraint exists to prevent.
--
-- NO ROWS ARE INSERTED. Mapping rows are per-installation configuration carrying
-- Summit-assigned template ids, which differ per tenant and are never written into source or
-- seeded (build rules 4 and 5). The table arrives empty on every installation.
--
-- Reversal: DROP TABLE. Nothing references it — no view, no inbound FK, no other migration —
-- and the resolver's property fallback resumes on the next request with no code change.

CREATE TABLE summit_plan_template_map (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    psp_id            BIGINT        NOT NULL,
    service_item_id   INT           NOT NULL,
    template_id       INT           NOT NULL,
    key_segment       VARCHAR(50)   NOT NULL,
    label             VARCHAR(100)  NULL,
    sort_order        INT           NOT NULL DEFAULT 0,
    is_active         TINYINT(1)    NOT NULL DEFAULT 1,
    created_at        DATETIME      NOT NULL,
    created_by        VARCHAR(100)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_summit_plan_template_map_psp_service (psp_id, service_item_id),
    INDEX idx_summit_plan_template_map_service_item (service_item_id),
    CONSTRAINT fk_summit_plan_template_map_psp
        FOREIGN KEY (psp_id) REFERENCES assignee (id),
    CONSTRAINT fk_summit_plan_template_map_service_item
        FOREIGN KEY (service_item_id) REFERENCES templatepurpose (purpose_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V095' AS version, '2026-09-08' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V095', 'Summit plan template mapping (summit_plan_template_map): PSP-scoped ServiceItem-to-Summit-template map, read ahead of the SUMMIT_PLAN_TEMPLATES property with fallback, created empty, 1:1 enforced by a named unique constraint rather than the PK', 'V095__summit_plan_template_map.sql', NOW());
