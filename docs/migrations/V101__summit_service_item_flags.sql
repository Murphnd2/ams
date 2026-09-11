-- V101: Summit employer administration flags per elected ServiceItem (T238 part 1, D46)
--
-- A PSP-scoped mapping from an elected ServiceItem to the four Summit Employer Demographic
-- Boolean elements: Enable CDH Administration, Enable COBRA Administration, Enable Retiree
-- Billing Administration, Enable Direct Bill Administration. D46 (2026-09-11) settled that file 1
-- emits all four as explicit true/false, derived as the union (OR) across every elected
-- ServiceItem that has a row here. s47b Q10 established that no ServiceItem kind marker exists
-- (ServiceItem.code is null on every Setup-category item, T189) -- this table is that marker,
-- following V095's precedent exactly rather than inventing a second mechanism.
--
-- ⚠️ NOTHING READS THIS TABLE YET. SummitEmployerFlagResolver (this build) computes the union;
-- SummitExportServlet's file 1 emitter is a later, separate build, blocked on SDX-27 (does Summit
-- honor an explicit `false`, and what does `false` do to a flag already on). Until that emitter
-- ships, this table and its admin screen change no export behaviour at all.
--
-- Scoped by `psp_id BIGINT` FK to `assignee(id)`, matching `summit_plan_template_map.psp_id`
-- exactly (PSP extends Assignee under single-table inheritance, so every FK to a PSP targets
-- `assignee(id)`). NOT NULL for the same reason: a row with no PSP could never be read back by
-- the PSP-scoped resolver.
--
-- `service_item_id` is INT, not BIGINT, matching `templatepurpose.purpose_id` (`ServiceItem`'s own
-- primary key is `int`).
--
-- ⚠️ A ROW'S PRESENCE IS THE MAPPING -- THERE IS NO `is_active` COLUMN, unlike V095. V095's
-- `is_active` exists to retire a plan-template mapping without breaking `Import Plan ID`'s upsert
-- identity for plans already created in Summit; a flag mapping creates nothing in Summit on its
-- own; a PSP admin who no longer wants an item's flags simply removes the row (Step 5's Remove),
-- the same way `SummitPlanTemplateAdmin` deletes rather than deactivates its own rows once a
-- mapping is truly unwanted. An elected ServiceItem with no row here contributes no flags -- all
-- four booleans default false at the column level, but "no row" and "a row with all four false"
-- are different states the resolver can tell apart (`mappedCount`).
--
-- The unique constraint is where the 1:1 rule lives, exactly as V095's own note explains for
-- itself: named rather than the primary key, so a future fan-out decision is an index drop against
-- live rows rather than a table rebuild.
--
-- NO ROWS ARE INSERTED. Flag choices are per-installation configuration and are never written into
-- source or seeded (build rules 4 and 5). The table arrives empty on every installation.
--
-- Reversal: DROP TABLE. Nothing references it -- no view, no inbound FK, no other migration -- and
-- SummitEmployerFlagResolver's only caller (nothing, as of this migration) means dropping this
-- table changes no running behaviour at all.

CREATE TABLE summit_service_item_flags (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    psp_id                  BIGINT        NOT NULL,
    service_item_id         INT           NOT NULL,
    enable_cdh              TINYINT(1)    NOT NULL DEFAULT 0,
    enable_cobra            TINYINT(1)    NOT NULL DEFAULT 0,
    enable_retiree_billing  TINYINT(1)    NOT NULL DEFAULT 0,
    enable_direct_bill      TINYINT(1)    NOT NULL DEFAULT 0,
    created_at              DATETIME      NOT NULL,
    created_by              VARCHAR(100)  NULL,
    updated_at              DATETIME      NULL,
    updated_by              VARCHAR(100)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_summit_service_item_flags_psp_service (psp_id, service_item_id),
    INDEX idx_summit_service_item_flags_service_item (service_item_id),
    CONSTRAINT fk_summit_service_item_flags_psp
        FOREIGN KEY (psp_id) REFERENCES assignee (id),
    CONSTRAINT fk_summit_service_item_flags_service_item
        FOREIGN KEY (service_item_id) REFERENCES templatepurpose (purpose_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V101' AS version, '2026-09-11' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V101', 'Summit employer administration flags per elected ServiceItem (summit_service_item_flags): PSP-scoped CDH/COBRA/Retiree/Direct Bill flag mapping, derivation for file 1 (D46), not yet read by any export writer, blocked on SDX-27 (T238 part 1)', 'V101__summit_service_item_flags.sql', NOW());
