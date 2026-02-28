# Database Migration Tracker

Tracks database schema versions across environments.

**Last Updated:** February 27, 2026

## Environments

| Environment | Host | Schema | Notes |
|-------------|------|--------|-------|
| Local (Work) | 127.0.0.1:3306 | beta_ssa | Dev workstation |
| Local (Home) | 127.0.0.1:3306 | beta_ssa | Home workstation |
| Local (either) | 127.0.0.1:3306 | dev_ssa | Initialization testing (wiped regularly) |
| Production | superiorstate.biz | beta_ssa | Live server |

## Current Highest Version: V024

## Dev Baseline

The current baseline is `docs/importscript/beta_ssa_dev_baseline_thru_V024.sql` — a structure-only dump from a V024 database.

**To reset a dev database:**
1. Reset target: `DROP DATABASE IF EXISTS dev_ssa; CREATE DATABASE dev_ssa;`
2. Import baseline: Workbench → Server → Data Import → select file → target `dev_ssa`
3. Start app against `dev_ssa` → `DatabaseInitializer` seeds data
4. For `beta_ssa`, also re-import Datapath exports after baseline import

Future migrations (V025+) are applied incrementally on top of the baseline.

## Schema Version Table

The `schema_version` table columns are: `version` (PK), `description`, `script_name`, `applied_on` (timestamp, auto-default).

The table is seeded by the baseline dump. For fresh databases created outside the baseline (e.g., `dev_ssa` after initialization), run `docs/schema_version_migration.sql` to register all versions.

## Going Forward

All new schema changes must follow these rules:
1. Create a versioned script: `V{NNN}__{description}.sql`
2. Script must self-register via `INSERT IGNORE INTO schema_version (version, description, script_name, applied_on) VALUES (..., NOW());`
3. Update this tracker with the new version
4. Update `docs/schema_version_migration.sql` with the new INSERT row

Individual migration scripts are no longer stored in the repo. The baseline dump + `schema_version_migration.sql` are the source of truth. Session summaries document what each version changed.

## Version History

| Version | Description | beta_ssa (work) | beta_ssa (home) | dev_ssa | Production |
|---------|-------------|-----------------|-----------------|---------|------------|
| V001–V019 | Sales pipeline through application field suppressed | ✅ | ✅ | ✅ | ✅ |
| V020 | ServiceItem unification - schema + backfill | ✅ | ✅ | ✅ | ✅ |
| V021 | ServiceItem linkage - LOS/Enhancement backfill | ✅ | ✅ | ✅ | ✅ |
| V022 | Orphaned ticket ServiceItem backfill | ✅ | ✅ | ✅ | ✅ |
| V023 | Drop ticketsubcategory table and FK | ✅ | ✅ | ✅ | ✅ |
| V024 | Fix views referencing dropped ticket_category column | ✅ | ✅ | ✅ | ✅ |

## Notes

- V020 requires corresponding Java code changes (TemplatePurpose → ServiceItem, TemplateGroup → ActivityCategory class/field renames). The code branch with renames must be deployed alongside the schema change.
- V021 creates new group 2 ServiceItems (IDs 25-35), renames SI 17 to "Payment Services", suppresses SI 18 and SI 20, and links all LOS and Enhancement records to their ServiceItems.
- V022 creates 6 catch-all ServiceItems (IDs 36-41) for ticket categories that had no ServiceItem, and backfills 730 orphaned tickets.
- V023 drops the ticketsubcategory table and its FK column (ticket_category) from assignee. Requires updated WAR with TicketSubCategory.java deleted.
- V024 rebuilds the a_base_01 through a_base_05 view chain to remove references to the dropped ticket_category column.
- dev_ssa can be reset from the V024 baseline at any time.
