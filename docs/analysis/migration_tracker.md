# Database Migration Tracker

Tracks which migration scripts have been applied to each environment.

## Environments

| Environment | Host | Schema | Notes |
|-------------|------|--------|-------|
| Local | 127.0.0.1:3306 | beta_ssa | Dev workstation |
| Production | superiorstate.biz | beta_ssa | Live server |

## Migration Log

| Script | Description | Local | Production | Notes |
|--------|-------------|-------|------------|-------|
| `sales_pipeline_migration.sql` | Sales Pipeline — new tables, column adds, renames | ✅ 2026-02-19 | ❌ NOT RUN | Run BEFORE deploying sales pipeline code. Must run Steps 1-4 in order. Step 5 (drop old tables) is optional cleanup — hold until verified. |

## How to Use

1. Before deploying code changes to production, check this file for any pending migrations
2. Run scripts in the order listed
3. Update the Production column with date after running
4. Commit this file back to GitHub

## Rules
- Never deploy code that depends on schema changes without running the migration first
- Always test the migration on local before running on production
- Keep old migration scripts in `docs/` for history — don't delete them
