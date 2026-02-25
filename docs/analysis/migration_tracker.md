# Database Migration Tracker

Tracks which migration scripts have been applied to each environment.

**Last Updated:** February 25, 2026

## Environments

| Environment | Host | Schema | Notes |
|-------------|------|--------|-------|
| Local (Work) | 127.0.0.1:3306 | beta_ssa | Dev workstation |
| Local (Home) | 127.0.0.1:3306 | beta_ssa | Home workstation |
| Local (either) | 127.0.0.1:3306 | dev_ssa | Initialization testing (wiped regularly) |
| Production | superiorstate.biz | beta_ssa | Live server |

## Current Highest Version: V013

## Dev Baseline

A validated V013 schema baseline is available at `docs/importscript/beta_ssa_dev_baseline_thru_V013.sql`. This was created by:

1. Dumping production structure (V000 + partial V001)
2. Running the validated upgrade script (`production_upgrade_V001_to_V013.sql`) against it
3. Exporting the resulting structure

**To reset a dev database:** Import the baseline, then run `DatabaseInitializer` (start the app). For `beta_ssa`, also re-import Datapath exports. Future migrations (V014+) are applied incrementally on top of this baseline.

## Production Upgrade

Production has a partially-applied V001 (table rename + proposal columns done, application columns + new tables missing). A validated combined upgrade script exists at `docs/importscript/production_upgrade_V001_to_V013.sql` that handles this partial state and applies all remaining changes through V013.

**Bugs found and corrected in the upgrade script (not present in original per-version scripts):**

| Original Script | Bug | Fix |
|----------------|-----|-----|
| V004 (`service_manager_production_migration.sql`) | `REFERENCES psp(psp_id)` — no `psp` table exists | Changed to `REFERENCES assignee(id)` |
| V004 | `psp_id INT NOT NULL` — wrong type | Changed to `BIGINT` |
| V004 | Enhancement seed data used `psp_id=1` | Changed to `psp_id=4` (production PSP ID) |
| V006 (`invitation_system_migration.sql`) | `INSERT INTO userrole (id, ...)` | Column is `role_id`, not `id` |
| V008 (`opportunity_migration_production.sql`) | `INSERT INTO templategroup (id, ...)` | Column is `group_id` |
| V008 | `INSERT INTO templatepurpose (id, ..., template_group)` | Columns are `purpose_id` and `group_id` |
| V008 | `INSERT INTO task (id, ...)` | Column is `task_id` |
| V008 | `INSERT INTO tasksequence (id, ...)` | Column is `sequence_id` |
| V011 (`V011__bpo_delegation_feature.sql`) | `INSERT INTO userrole (id, ...)` | Column is `role_id` |
| V011 | `todo_note` FK `REFERENCES person(person_id)` | Changed to `REFERENCES assignee(id)` |
| V011 | `todo` BPO FKs `REFERENCES person(person_id)` | Changed to `REFERENCES assignee(id)` |
| V013 (`V013__user_filter_presets.sql`) | `REFERENCES user(user_id)` | Column is `person_id`, changed to `REFERENCES user(person_id)` |
| V013 | `SELECT u.user_id` in seed INSERTs | Changed to `u.person_id` |
| V013 | Slots 2 and 3 missing `FROM user u` clause | Added |

**These bugs exist in the original per-version `.sql` files in `docs/` but are corrected in the combined upgrade script.** The per-version files should be considered historical — the upgrade script is the validated source of truth.

## Migration Log

| # | Version | Script | Description | Local | Production | Notes |
|---|---------|--------|-------------|-------|------------|-------|
| 1 | V001 | `sales_pipeline_migration.sql` | New tables (feature, ratediscount, marketingmaterial, applicationfield/value), column adds to proposal + application, entity renames | ✅ 2026-02-19 | ⚠️ PARTIAL | Rename + proposal cols done. Application cols + new tables NOT done. |
| 2 | V002 | `sales_pipeline_migration_2.sql` | Proposal `source_activity_id` nullable FK | ✅ 2026-02-19 | ❌ NOT RUN | |
| 3 | V003 | `sales_pipeline_migration_3.sql` | LOS expansion (IDs 11–19), applicationsection + applicationsectionlos, ~95 applicationfield seeds, irslimit, billingtype, benefittype, S3 constants | ✅ 2026-02-20 | ❌ NOT RUN | **Must fill in S3 constants on production.** |
| 4 | V004 | `service_manager_production_migration.sql` | Enhancement table, join tables, servicemodule FKs, LOS columns (sort_order, suppressed), seed data | ✅ 2026-02-20 | ❌ NOT RUN | ⚠️ Original has FK bug (see above) |
| 5 | V005 | `rate_manager_session2_production_migration.sql` | Per-rate `sort_order` column on ratetable, backfill from servicemodule | ✅ 2026-02-20 | ❌ NOT RUN | Prereq: V004 |
| 6 | V006 | `invitation_system_migration.sql` | Invitation table, agency.manager_id FK, UserRole seed | ✅ 2026-02-21 | ❌ NOT RUN | ⚠️ Original has column name bug |
| 7 | V007 | `resource_library_production_migration.sql` | ResourceCategory table, marketingmaterial.category_id FK, widen storage_guid, feature.material_id FK | ✅ 2026-02-21 | ❌ NOT RUN | Prereq: V001 |
| 8 | V008 | `opportunity_migration_production.sql` | Assignee columns for Opportunity, sales TemplateGroup/TemplatePurpose/Task seed data | ✅ 2026-02-21 | ❌ NOT RUN | ⚠️ Original has 4 column name bugs. Prereq: V001–V003 |
| 9 | V009 | `timeclock_correction_migration.sql` | time_correction_request table with FKs and indexes | ✅ 2026-02-25 | ❌ NOT RUN | No prerequisites |
| 10 | V010 | `V010__psp_opportunity_integration.sql` | PSP Sales role (ID 9), managed_by_id on assignee | ✅ 2026-02-22 | ❌ NOT RUN | Prereq: V008 |
| 11 | V011 | `V011__bpo_delegation_feature.sql` | BPO columns on todo, todo_note table, BPO user roles (101-103) | ✅ 2026-02-24 | ❌ NOT RUN | ⚠️ Original has FK + column bugs. Prereq: V010 |
| 12 | V012 | `V012__role_cleanup_psp_branding_constants.sql` | Delete unused roles (6,7,10), rename BPO roles, seed branding constants | ✅ 2026-02-25 | ❌ NOT RUN | Prereq: V011 |
| 13 | V013 | `V013__user_filter_presets.sql` | user_filter_preset table, 3 configurable slots per user | ✅ 2026-02-25 | ❌ NOT RUN | ⚠️ Original has FK + column bugs |

## Production Deployment Instructions

**Do NOT run the individual per-version scripts on production.** They contain bugs that will cause failures.

Instead, use the combined, validated upgrade script:

```bash
# SSH into production
# Take backup first!
LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysqldump -u root -p \
  --socket=/var/run/mysqld/mysqld.sock beta_ssa > /opt/ssa/backups/pre_upgrade_backup.sql

# Run the upgrade
LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql -u root -p \
  --socket=/var/run/mysqld/mysqld.sock beta_ssa < production_upgrade_V001_to_V013.sql

# Verify
LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql -u root -p \
  --socket=/var/run/mysqld/mysqld.sock -e "SELECT * FROM beta_ssa.schema_version ORDER BY version;"
```

After running, fill in the S3/Wasabi constants:
```sql
UPDATE constant SET value = '...' WHERE name = 'S3_ENDPOINT';
UPDATE constant SET value = '...' WHERE name = 'S3_BUCKET';
UPDATE constant SET value = '...' WHERE name = 'S3_ACCESS_KEY';
UPDATE constant SET value = '...' WHERE name = 'S3_SECRET_KEY';
```

## schema_version_migration.sql Status

The `docs/schema_version_migration.sql` file contains incorrect descriptions and script names for V004–V008 (they were assigned wrong during retroactive numbering). The validated upgrade script and the baseline dump both contain the correct `schema_version` registrations. The standalone `schema_version_migration.sql` file should be updated or deprecated in favor of the upgrade script.
