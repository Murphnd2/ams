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
- User roles 101-103 (Accelergent BPO/Admin/User) — still seeded, just no users assigned
- Demo benefits (HRA, FSA, COBRA) and ticket categories — to be moved to demo seeder servlet (see D-24)

---

### D-05: Validate Deployment Key in `InitializeDataBase` ✅

**Completed:** February 23, 2026
**File:** `src/main/java/net/superiorstate/ams/controller/authentication/InitializeDataBase.java` (new)

Created the `InitializeDataBase` servlet (was previously missing entirely — form posted to a non-existent URL). Validates `deploymentKey` from form against `DEPLOYMENT_KEY` in `ssa.properties`. Rejects if key is missing, blank, or doesn't match.

---

### D-06: Prevent Re-Initialization After Setup ✅

**Completed:** February 23, 2026
**File:** `src/main/java/net/superiorstate/ams/controller/authentication/InitializeDataBase.java`

Built into the new servlet. Checks for `SSL_PORT=443` constant in DB before allowing initialization. If found, redirects to login page.

---

### D-08: Verify Reserved ID Ranges in DatabaseInitializer ✅

**Completed:** February 23, 2026

Verified no conflicts on a fresh blank schema. Reserved ranges documented:
- IDs 1-103: seed data (statuses, roles, persons, tasks, etc.)
- Negative IDs: PSP employer/employee placeholders
- 1000+: application-generated IDs via SEQ_GEN (bumped from 200 to 1000 for breathing room)

---

### D-09: Create `schema_version` Table ✅

**Completed:** February 23, 2026 (Session 1)

Applied to master VPS image and local dev. Production pending.

---

### D-10: Renumber Existing Migrations ✅

**Completed:** February 23, 2026 — Resolved by convention.

Old scripts keep their descriptive names (already tracked in `schema_version`). All new scripts follow `V{NNN}__description.sql` convention (V010 already does). No renames needed.

---

### D-11: Build Backup Script ✅

**Completed:** February 23, 2026 (Session 1)

`/opt/ssa/scripts/backup.sh` — mysqldump → gzip → Wasabi upload, 7-day local retention. Cron at 2:00 AM UTC.

---

### D-12: Build Update Script ✅

**Completed:** February 23, 2026 (Session 1)

`/opt/ssa/scripts/update.sh` — GitHub Releases API → SQL migrations → WAR deploy → Tomcat restart. Cron at 2:30 AM UTC.

---

### D-13: Build Health Check Script ✅

**Completed:** February 23, 2026 (Session 2)

`/opt/ssa/scripts/healthcheck.sh` — Collects system status (Tomcat, MySQL, disk, backups, versions, errors) and emails daily report. Cron at 6:00 AM UTC.

**SMTP config:** Reads from DB constants (`SYS_HEALTH_*`) with hardcoded fallback if DB unreachable. Updatable across all deployments via SQL migrations.

**Kill switch:** `SYS_HEALTH_ENABLED` constant — set to `false` to disable emails (e.g., when master dashboard replaces email reports).

**DB constants seeded during initialization:**
- `SYS_HEALTH_ENABLED` = `true`
- `SYS_HEALTH_EMAIL_TO` = `kevin@superiorstate.net`
- `SYS_HEALTH_EMAIL_FROM` = `health@superiorstate.net`
- `SYS_HEALTH_SMTP_SERVER` = `mail.smtp2go.com`
- `SYS_HEALTH_SMTP_PORT` = `2525`
- `SYS_HEALTH_SMTP_USER` = `jspSmtpSender`
- `SYS_HEALTH_SMTP_PASSWORD` = (smtp2go credential)

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

### D-27: Create Tomcat SSL Reference Doc ✅

**Completed:** February 23, 2026 (Session 2)
**File:** `docs/tomcat_ssl_setup.md`

Step-by-step guide covering Certbot certificate generation, Tomcat `server.xml` HTTPS connector configuration, HTTP→HTTPS redirect, privileged port binding, and auto-renewal with deploy hooks. Referenced from `docs/deployment_runbook.md` Phase 3.

---

### D-28: Save Blank Schema Dump on Master Image ✅

**Completed:** February 23, 2026 (Session 2)
**Location:** `/opt/ssa/schema/beta_ssa_blank.sql` on master VPS

Enables quick rollback: drop database, re-import blank schema, re-initialize. Referenced in deployment runbook rollback procedure.

---

## Open Items

### D-07: Externalize Database Connection

**Priority:** MEDIUM — Currently working via JNDI in context.xml
**Status:** Deferred

The database connection is currently configured via Tomcat JNDI datasource in `context.xml`, which is outside the WAR. This works for multi-PSP deployment. Moving it to `ssa.properties` is a future nice-to-have but not blocking.

---

### D-14: Master Admin Dashboard & PSP Instance Management

**Priority:** LOW — Phase 2 (after health check emails are operational)
**Status:** Not started

Build a dashboard view in the AMS application, accessible only to "Master Admin" role. This serves as the central management console for all PSP instances.

**Features:**
- Receives health check data from all PSP VPSes and displays status grid
- PSP instance registry: list of all deployed PSPs with status, domain, version, last backup
- Deployment feedback: surface errors, version drift, failed updates
- "Master Admin" concept that manages PSP installs

**Architecture decision:** Build within the AMS app (role-gated) rather than a separate website. The infrastructure and auth system already exist.

**When built:** Push migration to set `SYS_HEALTH_ENABLED=false` across all PSPs to stop email reports. Update deployment runbook Phase 7 to reference the new dashboard workflow.

---

### D-22: Investigate IONOS vCPU Core Quota

**Priority:** MEDIUM
**Status:** Not started

Determine if IONOS has a per-account limit on vCPU cores and whether additional quota needs to be requested before spinning up multiple PSP VPSes.

---

### D-24: Create Demo Seeder Servlet

**Priority:** MEDIUM
**Status:** Not started

Create a protected servlet (master admin only) that seeds demo data onto a fresh initialized database for demonstration purposes. Should include:

- Demo benefits (HRA, FSA, COBRA) with realistic dates
- Sample employers and employees
- Sample activities (tickets, renewals, setups) to showcase the UI

This replaces the test data previously hardcoded in `DatabaseInitializer`. Demo data should be clearly identifiable and removable.

---

### D-25: Manual Benefit Creation UI

**Priority:** MEDIUM
**Status:** Not started

Build an admin page to manually create Benefit records without requiring a Summit import. Currently benefits can only enter the system through the Summit CSV import pipeline or the demo data in the initializer.

---

### D-26: Vendor Management Admin Page

**Priority:** MEDIUM
**Status:** Not started

Build an admin page to register third-party vendor contacts (persons + users with BPO roles). Replaces the hardcoded Accelergent persons previously in `DatabaseInitializer`. Should allow PSP admins to:

- Create vendor person records
- Create vendor user accounts with appropriate roles (101/102/103)
- View/edit/deactivate existing vendors

The BPO source dropdown in task manager will populate from these registered vendors.

---

## Remaining TODOs

- Run `schema_version_migration.sql` on production database (holding until further testing)
- Update master VPS snapshot version in runbook after future image updates
- Update deployment runbook Phase 7 when PSP admin dashboard is built (D-14)
- Reserve static IPs in IONOS for each PSP deployment
