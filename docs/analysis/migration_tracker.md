# Database Migration Tracker

Tracks which migration scripts have been applied to each environment.

## Environments

| Environment | Host | Schema | Notes |
|-------------|------|--------|-------|
| Local (Work) | 127.0.0.1:3306 | beta_ssa | Dev workstation |
| Local (Home) | 127.0.0.1:3306 | beta_ssa | Home workstation |
| Production | superiorstate.biz | beta_ssa | Live server |

## Migration Log

Run scripts in the order listed. Some depend on prior ones.

| # | Script | Description | Local | Production | Notes |
|---|--------|-------------|-------|------------|-------|
| 1 | `sales_pipeline_migration.sql` | New tables (feature, ratediscount, marketingmaterial, applicationfield/value), column adds to proposal + application, entity renames | ✅ 2026-02-19 | ❌ NOT RUN | Run BEFORE deploying sales pipeline code. |
| 2 | `sales_pipeline_migration_2.sql` | Proposal `source_activity_id` nullable FK | ✅ 2026-02-19 | ❌ NOT RUN | Run before deploying SendProposal. |
| 3 | `sales_pipeline_migration_3.sql` | LOS expansion (IDs 11–19), applicationsection + applicationsectionlos, ~95 applicationfield seeds, irslimit, billingtype, benefittype, constant inserts for S3 | ✅ 2026-02-20 | ❌ NOT RUN | Largest migration. **Must fill in S3 constants on production.** |
| 4 | `service_manager_production_migration.sql` | Enhancement table, join tables, servicemodule FKs, LOS columns (sort_order, suppressed), seed data | ✅ 2026-02-20 | ❌ NOT RUN | Includes enhancement seed data and LOS sort_order backfill. |
| 5 | `rate_manager_session2_production_migration.sql` | Per-rate `sort_order` column on ratetable, backfill from servicemodule | ✅ 2026-02-20 | ❌ NOT RUN | Prereq: script 4 (LOS sort_order must exist for backfill). |
| 6 | `invitation_system_migration.sql` | Invitation table, agency.manager_id FK, UserRole seed (INSERT IGNORE) | ✅ 2026-02-21 | ❌ NOT RUN | Run BEFORE deploying invitation code. |
| 7 | `resource_library_production_migration.sql` | ResourceCategory table, marketingmaterial.category_id FK, widen storage_guid to VARCHAR(50), feature.material_id FK | ✅ 2026-02-21 | ❌ NOT RUN | Prereq: script 1 (creates feature and marketingmaterial tables). |
| 8 | `opportunity_migration_production.sql` | Assignee columns for Opportunity (prospect_id, agency_id_opp, opportunity_stage, etc.), sales TemplateGroup/TemplatePurpose/Task seed data | ✅ 2026-02-21 | ❌ NOT RUN | Do NOT add DEFAULT to opportunity_stage. Prereq: scripts 1–3. |
| 9 | `timeclock_correction_migration.sql` | time_correction_request table with FKs to assignee and timelog, indexes on status/requestor/date | ❌ NOT RUN | ❌ NOT RUN | No prerequisites. Run before deploying timeclock correction code. |
| 10 | `V010__psp_opportunity_integration.sql` | PSP Sales role (ID 9), managed_by_id on assignee for PSP-managed opportunities | ✅ 2026-02-22 | ❌ NOT RUN | Prereq: script 8 (opportunity columns must exist). Run BEFORE deploying activity list opportunity code. |

## Chatbot Migration (Standalone)

Not a numbered script file — these are individual SQL statements. Run on production before deploying chatbot code.

```sql
ALTER TABLE note ADD COLUMN is_resolution TINYINT(1) NOT NULL DEFAULT 0;
INSERT INTO constant (name, value, note) VALUES ('ANTHROPIC_API_KEY', '<key>', 'Claude API key for chatbot');
-- Ticket category updates: see chatbot_session_summary.md for full SQL
```

## How to Use

1. Before deploying code changes to production, check this file for any pending migrations
2. Run scripts in the order listed (1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9 → 10)
3. Update the Production column with date after running
4. Commit this file back to GitHub

## Rules
- Never deploy code that depends on schema changes without running the migration first
- Always test the migration on local before running on production
- Keep old migration scripts in `docs/` for history — don't delete them
