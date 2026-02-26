# Session Summary — February 26, 2026

## Production Upgrade Validation & Entity-Schema Alignment

### Objective
Validate the production upgrade script by importing a fresh production dump locally, running the upgrade, testing backward compatibility with `main` branch, then testing forward compatibility with `refactor/modernize-architecture` branch. Fix all entity-vs-schema mismatches discovered.

### Production Dump Acquisition
- SSH dump via `mysqldump` initially corrupted by PowerShell output redirection (UTF-16 BOM)
- Fixed by dumping on server first (`mysqldump > /tmp/file`), then `scp` to download clean file
- Imported into local `beta_ssa` via command line (Workbench had encoding issues with large files)

### Upgrade Script Execution
- Ran `production_upgrade_V001_to_V013.sql` (pre-existing) against local production copy
- Required `SET sql_log_bin = 0;` to prevent replication warnings on `DEFAULT (UUID())`
- All versions V001–V013 registered successfully

### Backward Compatibility Testing (main branch)
Switched IntelliJ to `main` branch and tested against upgraded schema.

**Bug found:** `CloseActivity25.java` uses `.toList()` (Java 16+) which returns an immutable list. This caused `UnsupportedOperationException` when `ADD_TICKET` or `ADD_RENEWAL` operations tried to modify the list after any activity was closed in the session.

**Fix:** Changed `.toList()` to `.collect(Collectors.toList())` in two places. Also applied defensive pattern to `ADD_TICKET` and `ADD_RENEWAL` cases in `AmsDataLocal`.

**Results:**
- ✅ Activity list loads
- ✅ Ticket creation works (after fix)
- ✅ Renewals, setups, checklists functional
- ⚠️ Email sending fails — missing Azure env vars (not migration-related)
- ⚠️ Ticket categories empty — expected (V014 deactivated old, new not wired in old code)

### Entity-Schema Mismatch Discovery & Resolution
After switching to current `refactor/modernize-architecture` branch, discovered 8 tables where entities had been refactored AFTER their migration scripts were written:

| Table | Issue | Fix Applied |
|-------|-------|-------------|
| `applicationfield` | Missing `section_id` FK + `help_text`, has obsolete `template_purpose_id` | Added ALTER for `section_id` + `help_text` in V003 section |
| `irslimit` | Wrong PK structure (auto-increment vs composite) | Changed to composite PK `(limit_key, plan_year)` |
| `benefittype` | Missing `default_billingtype_id` FK, wrong `name` width | Added FK, widened to VARCHAR(100) |
| `resourcecategory` | Missing `icon_class`, wrong `name` width | Added column, narrowed to VARCHAR(50) |
| `feature` | Has `material_id`, entity expects `library_resource_id` | Renamed column |
| `time_correction_request` | `correction_note` vs `request_note`, nullable issues | Renamed column, fixed nullability |
| `todo_note` | `created_at`/TIMESTAMP vs `created_date`/DATETIME, `source_type` width | Fixed column name, type, and width |
| `user_filter_preset` | Completely redesigned entity (new column names, types, structure) | Full rewrite of CREATE TABLE |

**Root cause:** Entities were refactored during the `refactor/modernize-architecture` branch work (Feb 19–26), but migration scripts were written against the pre-refactor entity definitions.

**Verification method:** Compared office working database schemas (beta_ssa and dev_ssa from office PC) against the upgrade script output. Office databases represent the ground truth that current code runs against.

### Extended Upgrade Script (V001→V016)
Extended the upgrade script from V013 to V016, incorporating:
- V014: Chatbot deployment (note.is_resolution, API key, ticket categories)
- V015: Constants-to-properties migration
- V016: BPO registration tables
- V011 additions: `todo_guid` (DEFAULT UUID()), `is_reverted`, `task_guid` (DEFAULT UUID())
- `SET sql_log_bin = 0` header for MySQL binary logging compatibility

### Deliverables

| File | Location | Purpose |
|------|----------|---------|
| `production_upgrade_V001_to_V016.sql` | `docs/importscript/` | Validated upgrade script with all entity fixes |
| `migration_tracker.md` | `docs/analysis/` | Updated through V016 with validation notes |
| `schema_version_migration.sql` | `docs/` | Updated with V016 entry |
| `CloseActivity25_main_branch.java` | For `main` branch | Fixed immutable list bug |

### SQL Audit
- **No new migration versions created** — all fixes were applied within the existing V001–V016 upgrade script
- **No ad-hoc SQL run on any environment** — all testing was against a local copy of the production dump
- **Current highest version: V016**
- **Production status: Still at partial V001** — upgrade not yet executed on production
- **All SQL changes tracked** in `production_upgrade_V001_to_V016.sql` and `migration_tracker.md`

### Remaining Items
1. **SendAutoEmail recovery** — File deleted during cleanup, found in git history at commit `6aa6711`. Recovery deferred to next session. Command: `git show 6aa6711:src/main/java/.../SendAutoEmail.java`
2. **Smoke testing** — Current branch against upgraded schema needs completion
3. **Production migration** — Validated script ready, backup-first procedure documented
4. **Dev baseline update** — `beta_ssa_dev_baseline_thru_V016.sql` should be regenerated from a database that has all entity fixes applied (current baseline was taken before this session's fixes)

### Key Technical Learnings
- PowerShell `>` redirection corrupts binary SQL dumps with UTF-16 BOM — use server-side dump + `scp`
- `SET sql_log_bin = 0` required for `DEFAULT (UUID())` columns under MySQL strict/binary-log mode
- `.toList()` (Java 16+) returns immutable list — use `.collect(Collectors.toList())` for mutable lists
- Entity refactoring after migration scripts creates silent drift — always validate by comparing actual DB structure against entity annotations before production deployment
