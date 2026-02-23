# Deployment Backlog

**Last Updated:** February 23, 2026 (Session 2)
**Reference:** See `docs/deployment_strategy.md` for full context on each item.

Items are ordered by dependency (earlier items unblock later ones).

---

## Completed Items

### D-01: Create `ssa.properties` Loader ✅

**Completed:** February 23, 2026
**File:** `src/main/java/net/superiorstate/ams/AppConfig.java` (new)

Created `AppConfig` utility class that loads `/var/lib/tomcat10/conf/ssa.properties` at startup. Lookup order: (1) system property `-Dssa.config=/path/to/file` for dev machines, (2) fallback `{catalina.base}/conf/ssa.properties` for Linux VPSes. Called from `EmfListener.contextInitialized()` before EMF creation. Values accessible anywhere via `AppConfig.get(key, default)`.

---

### D-02: Fix Hardcoded Windows Path in `EmfListener` ✅

**Completed:** February 23, 2026
**File:** `src/main/java/net/superiorstate/ams/EmfListener.java`

`logStartupError()` now reads `LOG_PATH` from `AppConfig` with fallback to `catalina.base/logs`. No more hardcoded Windows path.

---

### D-03: Fix Hardcoded `SAVE_PATH` Default in `DatabaseInitializer` ✅

**Completed:** February 23, 2026
**Files changed:**
- `DatabaseInitializer.java` — Removed `SAVE_PATH` from `addPspConstants()` (no longer a DB constant)
- `AppConstantDAO.java` — Replaced two `getSavePath(em)` / `getSavePath(request)` methods with single `getSavePath()` that reads from `AppConfig`
- `AmsDataGlobal.java` — `setConstants()` now reads `SAVE_PATH` from `AppConfig` instead of DB
- Deleted dead servlets: `AddDocumentToActivity.java`, `AddFileToTask.java` (zero usages, used old local file save)

`SAVE_PATH` is now exclusively an infrastructure config value in `ssa.properties`.

---

### D-04: Remove Test Data from `DatabaseInitializer` ✅

**Completed:** February 23, 2026
**File:** `src/main/java/net/superiorstate/ams/data/service/DatabaseInitializer.java`

Removed:
- Fred Flintstone person (ID 50), Slate Rock and Gravel employer (ID -2), associated employee (ID -2), and person↔employee links
- Accelergent BPO persons (IDs 101, 102), their user accounts, and role assignments

Kept:
- User roles 101-103 (Accelergent BPO/Admin/User) — still seeded, just no users assigned. BPO dropdown will be empty until vendors are configured via admin UI.
- Demo benefits (HRA, FSA, COBRA) and ticket categories — to be moved to a demo seeder servlet in the future (see D-24, D-25)

---

### D-09: Create `schema_version` Table ✅

**Completed:** February 23, 2026 (Session 1)

Applied to master VPS image and local dev. Production pending.

---

### D-11: Build Backup Script ✅

**Completed:** February 23, 2026 (Session 1)

`/opt/ssa/scripts/backup.sh` — mysqldump → gzip → Wasabi upload, 7-day local retention. Cron at 2:00 AM UTC.

---

### D-12: Build Update Script ✅

**Completed:** February 23, 2026 (Session 1)

`/opt/ssa/scripts/update.sh` — GitHub Releases API → SQL migrations → WAR deploy → Tomcat restart. Cron at 2:30 AM UTC.

---

### D-15 through D-19: Master VPS Configuration ✅

**Completed:** February 23, 2026 (Session 1)

Data dir, MySQL connector, remove default ROOT, confirm awscli, open 8080.

---

### D-21: GitHub PAT ✅

**Completed:** February 23, 2026 (Session 1)

No expiration, read-only, scoped to Murphnd2/ams.

---

### D-23: Publish First Release ✅

**Completed:** February 23, 2026 (Session 1)

---

## Open Items

### D-05: Validate Deployment Key in `InitializeDataBase`

**Priority:** HIGH
**Status:** Needs investigation
**File:** Servlet that handles `/InitializeDataBase` POST

The `initialize.jsp` form collects a "Deployment Key" field. Need to verify:

1. Is the key actually validated before initialization runs?
2. Where is the expected key stored/compared?
3. What happens if someone submits an invalid key?

If not validated, add validation against `DEPLOYMENT_KEY` in `ssa.properties`.

---

### D-06: Prevent Re-Initialization After Setup

**Priority:** HIGH
**Status:** Needs investigation

After `DatabaseInitializer` runs, the `/InitializeDataBase` endpoint should refuse to run again. Current mechanism relies on `SSL_PORT` constant existing in the DB. Verify this is bulletproof.

---

### D-07: Externalize Database Connection

**Priority:** MEDIUM — Currently working via JNDI in context.xml
**Status:** Deferred

The database connection is currently configured via Tomcat JNDI datasource in `context.xml`, which is outside the WAR. This works for multi-PSP deployment. Moving it to `ssa.properties` is a future nice-to-have but not blocking.

---

### D-08: Verify Reserved ID Ranges in DatabaseInitializer

**Priority:** MEDIUM
**Status:** Not started

The initializer uses hardcoded IDs (PSP=4, person=104, agency=14, sequences starting at 200). Verify that the blank schema has no conflicting IDs. Document the reserved ID ranges.

---

### D-10: Renumber Existing Migrations to Standard Convention

**Priority:** MEDIUM
**Status:** Not started

Current migration scripts have descriptive names (`sales_pipeline_migration.sql`, `opportunity_migration_production.sql`, etc.). For the automated update system, adopt a sequential numbering convention:

```
V001__initial_schema.sql
V002__sales_pipeline.sql
V003__sales_pipeline_2.sql
...
V009__timeclock_correction.sql
```

This is a one-time rename + documentation task. The actual SQL content doesn't change.

---

### D-13: Build Health Check Script

**Priority:** LOW — Nice to have for first deployment, required by PSP #3-4
**Status:** Not started

Shell script installed on the master image at `/opt/ssa/scripts/healthcheck.sh`:

- Collects: Tomcat status, MySQL status, disk usage, last backup time, WAR version, DB migration version, recent errors
- Emails summary to `health@monitor.superiorstate.net` (or configurable address)
- Subject line includes `PSP_ID`
- Cron: nightly or twice daily

---

### D-14: Master Admin Dashboard

**Priority:** LOW — Phase 2 (after email monitoring is working)
**Status:** Not started

Build a dashboard view in the AMS application, accessible only to "Master Admin" role. Receives health check POSTs from all PSP VPSes and displays status grid.

---

### D-22: Investigate IONOS vCPU Core Quota

**Priority:** MEDIUM
**Status:** Not started

Determine if IONOS has a per-account limit on vCPU cores and whether additional quota needs to be requested before spinning up multiple PSP VPSes.

---

### D-24: Create Demo Seeder Servlet

**Priority:** MEDIUM
**Status:** Not started (new)

Create a protected servlet (master admin only) that seeds demo data onto a fresh initialized database for demonstration purposes. Should include:

- Demo benefits (HRA, FSA, COBRA) with realistic dates
- Sample employers and employees
- Sample activities (tickets, renewals, setups) to showcase the UI

This replaces the test data previously hardcoded in `DatabaseInitializer`. Demo data should be clearly identifiable and removable.

---

### D-25: Manual Benefit Creation UI

**Priority:** MEDIUM
**Status:** Not started (new)

Build an admin page to manually create Benefit records without requiring a Summit import. Currently benefits can only enter the system through the Summit CSV import pipeline or the demo data in the initializer.

---

### D-26: Vendor Management Admin Page

**Priority:** MEDIUM
**Status:** Not started (new)

Build an admin page to register third-party vendor contacts (persons + users with BPO roles). Replaces the hardcoded Accelergent persons previously in `DatabaseInitializer`. Should allow PSP admins to:

- Create vendor person records
- Create vendor user accounts with appropriate roles (101/102/103)
- View/edit/deactivate existing vendors

The BPO source dropdown in task manager will populate from these registered vendors.

---

## Remaining TODOs (from Session 1)

- Run `schema_version_migration.sql` on production database (holding until further testing)
- Commit updated `deployment_backlog.md` to repo
- Create `ssa.properties` file on each dev machine with local paths
