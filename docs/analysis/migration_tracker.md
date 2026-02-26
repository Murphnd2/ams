# Database Migration Tracker

Tracks database schema versions across environments.

**Last Updated:** February 26, 2026

## Environments

| Environment | Host | Schema | Notes |
|-------------|------|--------|-------|
| Local (Work) | 127.0.0.1:3306 | beta_ssa | Dev workstation |
| Local (Home) | 127.0.0.1:3306 | beta_ssa | Home workstation |
| Local (either) | 127.0.0.1:3306 | dev_ssa | Initialization testing (wiped regularly) |
| Production | superiorstate.biz | beta_ssa | Live server |

## Current Highest Version: V017

## All Environments Synced

As of February 26, 2026, production and local dev are both at **V017**. The full V001–V017 upgrade was applied to production and validated.

## Dev Baseline

The current baseline is `docs/importscript/beta_ssa_dev_baseline_thru_V017.sql` — a structure-only dump from production after all migrations were applied.

**To reset a dev database:**
1. Import the baseline: `mysql -u root -p beta_ssa < beta_ssa_dev_baseline_thru_V017.sql`
2. Run `DatabaseInitializer` (start the app with empty DB)
3. For `beta_ssa`, also re-import Datapath exports

Future migrations (V018+) are applied incrementally on top of this baseline.

## Schema Version Reference

The `schema_version` table is seeded by the baseline dump. For fresh databases created outside the baseline (e.g., `dev_ssa` after initialization), run `docs/schema_version_migration.sql` to register all versions.

## Going Forward

All new schema changes must follow these rules:
1. Create a versioned script: `V{NNN}__{description}.sql`
2. Script must self-register via `INSERT IGNORE INTO schema_version`
3. Update this tracker with the new version
4. Update `docs/schema_version_migration.sql` with the new INSERT row

Individual migration scripts are no longer stored in the repo. The baseline dump + `schema_version_migration.sql` are the source of truth. Session summaries document what each version changed.

## Migration Log

| # | Version | Description | Applied |
|---|---------|-------------|---------|
| 1 | V001 | Sales pipeline — tables, columns, entity renames | ✅ All |
| 2 | V002 | Proposal source_activity_id FK | ✅ All |
| 3 | V003 | LOS expansion, app sections, IRS limits, S3 constants | ✅ All |
| 4 | V004 | Service manager — enhancement, join tables, SM FKs | ✅ All |
| 5 | V005 | Rate manager — ratetable sort_order | ✅ All |
| 6 | V006 | Invitation system — invitation table, agency manager_id | ✅ All |
| 7 | V007 | Resource library — category, material FK, feature FK | ✅ All |
| 8 | V008 | Opportunity system — assignee columns, sales tasks | ✅ All |
| 9 | V009 | Timeclock correction — request table | ✅ All |
| 10 | V010 | PSP opportunity integration — sales role, managed_by | ✅ All |
| 11 | V011 | BPO delegation — todo BPO columns, todo_guid, task_guid, todo_note | ✅ All |
| 12 | V012 | Role cleanup and PSP branding constants | ✅ All |
| 13 | V013 | User filter presets — 3 slots per user | ✅ All |
| 14 | V014 | Chatbot deployment — note.is_resolution, ticket categories | ✅ All |
| 15 | V015 | Move S3 and API key constants to ssa.properties | ✅ All |
| 16 | V016 | BPO registration and PSP assignment tables | ✅ All |
| 17 | V017 | Move SYS_HEALTH constants to ssa.properties, seed EMAIL_FOOTER_TEXT | ✅ All |

## Production Upgrade History

**February 26, 2026:** Full V001–V017 upgrade applied to production.
- Backup taken via `mysqldump` before upgrade
- `production_upgrade_V001_to_V016.sql` run first (combined script handling partial V001 state)
- Two `DEFAULT (UUID())` columns required manual workaround (MySQL replication mode blocked non-deterministic defaults — split into ALTER + UPDATE + MODIFY)
- `V017__health_constants_to_properties.sql` run separately
- `ssa.properties` updated with `SYS_HEALTH_*` keys
- Backward compatibility confirmed: `main` branch (old code) runs cleanly against V017 schema
- Old upgrade scripts and per-version migration files deleted from repo after successful upgrade
