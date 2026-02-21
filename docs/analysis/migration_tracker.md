# Database Migration Tracker

Tracks which migration scripts have been applied to each environment.

## Environments

| Environment | Host | Schema | Notes |
|-------------|------|--------|-------|
| Local (Work) | 127.0.0.1:3306 | beta_ssa | Dev workstation |
| Local (Home) | 127.0.0.1:3306 | beta_ssa | Home workstation |
| Production | superiorstate.biz | beta_ssa | Live server |

## Migration Log

| Script | Description | Local | Production | Notes |
|--------|-------------|-------|------------|-------|
| `sales_pipeline_migration.sql` | Session 1 — new tables, column adds, renames | ✅ 2026-02-19 | ❌ NOT RUN | Run BEFORE deploying sales pipeline code. |
| `sales_pipeline_migration_2.sql` | Session 2 — Proposal source_activity_id column | ✅ 2026-02-19 | ❌ NOT RUN | Optional nullable FK. Run before deploying SendProposal. |
| `sales_pipeline_migration_3.sql` | Session 3 — Application form, IRS limits, benefit/billing types | ✅ 2026-02-20 | ❌ NOT RUN | Largest migration. Includes LOS expansion, 20 sections, ~95 fields, IRS limits, benefit/billing type tables. **Must fill in S3 constants on production.** |
| `service_manager_production_migration.sql` | Service Manager — Enhancement table, join tables, SM FKs, LOS columns, seed data | ✅ 2026-02-20 | ❌ NOT RUN | Combined script. Includes enhancement seed data and LOS sort_order backfill. |
| `invitation_system_migration.sql` | Invitation system — invitation table, agency.manager_id, UserRole seed | ✅ 2026-02-21 | ❌ NOT RUN | Run BEFORE deploying invitation code. Includes standard UserRole seed (INSERT IGNORE). |

## How to Use

1. Before deploying code changes to production, check this file for any pending migrations
2. Run scripts in the order listed (1 → 2 → 3 → service_manager → invitation_system)
3. Update the Production column with date after running
4. Commit this file back to GitHub

## Rules
- Never deploy code that depends on schema changes without running the migration first
- Always test the migration on local before running on production
- Keep old migration scripts in `docs/` for history — don't delete them
