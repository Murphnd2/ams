# Session Prompt — February 26, 2026

## Context

Backward compatibility testing was performed on February 25 by running the main branch code against the dev database (which has all migrations through V016 applied). Two breaking issues were found and temporarily fixed on the dev database.

## What Was Found

**Problem:** The main branch code doesn't know about `todo_guid` or `task_guid` columns, but both are `NOT NULL` with no default. Any INSERT from old code fails with `Field 'xxx_guid' doesn't have a default value`.

**Temporary dev fix applied (not yet in any migration script):**
```sql
ALTER TABLE todo MODIFY COLUMN todo_guid VARCHAR(36) NOT NULL DEFAULT (UUID());
ALTER TABLE task MODIFY COLUMN task_guid VARCHAR(36) NOT NULL DEFAULT (UUID());
```

After this fix, the main branch worked fully against the dev schema.

## What Needs To Happen

### 1. Untracked columns need migration scripts

Three columns were added ad-hoc to dev during BPO development but never made it into any migration script:

| Table | Column | Type | Notes |
|-------|--------|------|-------|
| `todo` | `todo_guid` | VARCHAR(36) NOT NULL DEFAULT (UUID()), UNIQUE | Cross-system BPO sync identity |
| `todo` | `is_reverted` | TINYINT(1) NOT NULL DEFAULT 0 | PSP sent task back to BPO |
| `task` | `task_guid` | VARCHAR(36) NOT NULL DEFAULT (UUID()), UNIQUE | Cross-system BPO sync identity |

These logically belong with V011 (BPO delegation feature) — they were part of the BPO build plan but got missed when V011 was written.

### 2. Production upgrade script needs updating

The current `docs/importscript/production_upgrade_V001_to_V013.sql` needs:

- **Splice the 3 missing columns into the V011 section** (with `DEFAULT (UUID())` and backfill existing rows)
- **Extend the script through V016** by appending V014, V015, and V016 sections
- **Update the schema_version INSERT block** to include V014-V016
- **Update the script filename** to `production_upgrade_V001_to_V016.sql`

### 3. Dev baseline dump needs refreshing

The current `docs/beta_ssa_dev_baseline_thru_V016.sql` doesn't include the guid columns or their defaults. Needs a fresh dump after the migration script is finalized.

### 4. Migration tracker update

`docs/analysis/migration_tracker.md` needs:
- V011 description updated to mention guid columns and is_reverted
- Note that production upgrade script now covers V001-V016
- Note the `DEFAULT (UUID())` backward compatibility requirement

## Production vs Dev Schema Comparison (verified Feb 25)

Production was confirmed via SSH to be at **partial V001 only**:
- No `schema_version` table
- No `todo_guid`, `task_guid`, BPO columns, opportunity columns, etc.
- Only the `datakey→applicationfield` rename and proposal columns from V001 are present

The earlier comparison that showed them identical was done against the wrong database (dev, not production).

## Files to Reference

- `docs/importscript/production_upgrade_V001_to_V013.sql` — current upgrade script (source of truth, needs extending)
- `docs/V014__chatbot_deployment.sql` — needs appending to upgrade script
- `docs/V015__constants_to_properties.sql` — needs appending
- `docs/V016__bpo_registration_tables.sql` — needs appending
- `docs/analysis/migration_tracker.md` — needs updating
- `docs/analysis/bpo_build_plan.md` — reference for guid column purpose
