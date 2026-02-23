# Deployment Backlog

**Last Updated:** February 23, 2026
**Reference:** See `docs/deployment_strategy.md` for full context on each item.

Items are ordered by dependency (earlier items unblock later ones).

---

## Open Items

### D-01: Create `ssa.properties` Loader

**Priority:** HIGH — Blocks most other items
**Status:** Not started

Create a mechanism for the application to read `/var/lib/tomcat10/conf/ssa.properties` at startup. Options:

- Extend `EmfListener` to read the file and store values in `ServletContext`
- Or create a dedicated `ConfigListener` that runs before `EmfListener`

The application currently has no externalized config — everything is either in `persistence.xml` (inside the WAR) or in the database `constant` table.

**Acceptance:** Application reads `PSP_ID`, `SAVE_PATH`, and `LOG_PATH` from the properties file at startup.

---

### D-02: Fix Hardcoded Windows Path in `EmfListener`

**Priority:** HIGH
**Status:** Not started
**File:** `src/main/java/net/superiorstate/ams/EmfListener.java`

The error log path is hardcoded to `C:/Program Files/Apache Software Foundation/Tomcat 10.1/logs/emf_error.log`. On Linux this path doesn't exist.

**Fix:** Read `LOG_PATH` from `ssa.properties` (depends on D-01), or use a relative path that works on both OS. Fallback: use `System.getProperty("catalina.base") + "/logs/emf_error.log"`.

**Note:** Production already has an `emf_error.log` in `/var/lib/tomcat10/logs/`, so at some point this was working — verify whether the current deployed code differs from the repo.

---

### D-03: Fix Hardcoded `SAVE_PATH` Default in `DatabaseInitializer`

**Priority:** HIGH
**Status:** Not started
**File:** `src/main/java/net/superiorstate/ams/data/service/DatabaseInitializer.java`

`addPspConstants()` sets `SAVE_PATH` to `C:\\data\\`. This should either:

- Be removed from the DB constants entirely (read from `ssa.properties` instead — preferred), or
- Default to `/var/lib/tomcat10/data/` for Linux

**Decision needed:** If `SAVE_PATH` moves entirely to `ssa.properties`, any code that reads it from the `constant` table needs to be updated to read from the properties source instead.

**Depends on:** D-01

---

### D-04: Remove Test Data from `DatabaseInitializer`

**Priority:** MEDIUM
**Status:** Not started
**File:** `src/main/java/net/superiorstate/ams/data/service/DatabaseInitializer.java`

The initializer creates a "Fred Flintstone" person record (ID 50) with email `fred.flintstone@srag.com`. This is a development artifact and should not exist in production PSP databases.

**Fix:** Remove the `createMainContact(em, 50L, "Fred", "Flintstone", ...)` line and any references to `p1`.

---

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

The initializer uses hardcoded IDs (PSP=4, person=50, agency sequences starting at 200). Verify that the blank schema has no conflicting IDs. Document the reserved ID ranges.

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

Lives in the same codebase, deployed on your master/control VPS. Other PSP VPSes don't see it (role-gated).

---

### D-22: Investigate IONOS vCPU Core Quota

**Priority:** MEDIUM — Affects how many PSP VPSes can run simultaneously
**Status:** Not started

During demo setup, IONOS blocked provisioning due to an 8-core personal limit. Need to understand:

- What is the default core quota for the account?
- How do you request an increase? (Support ticket? Self-service?)
- Is the quota per-VDC or per-account?
- What is the cost model — pay-per-use for cores, or fixed allocation?
- Can stopped VPSes release their cores back to the quota? (Yes — confirmed during Feb 22 session)
- What quota do we need for N PSP clients? (2 cores × N servers + 2 for master when running)

Planning note: At 2 cores per PSP VPS, an 8-core quota supports 4 simultaneous PSPs (with master stopped). For 10+ PSPs, a quota increase will be required.

---

### D-24: Seed PSP House Agency with User Links

**Priority:** HIGH — Required for opportunity/sales features to work out of the box
**Status:** Not started
**Files:** `DatabaseInitializer.java`, `CreatePspUser` (if exists)

The initializer creates an Agency record during PSP setup but doesn't link PSP users to it. Without this link, PSP users can't create proposals or opportunities tied to their own agency.

**Required changes:**

1. **DatabaseInitializer:** After creating the house agency, set `manager_id` to the primary contact person, and insert the primary contact into the `agents` join table
2. **DatabaseInitializer:** Grant the initialization user roles 1 (PSP User) + 5 (PSP Admin) + 9 (PSP Sales) — currently only grants 1 + 5
3. **CreatePspUser (future):** When creating new PSP users, auto-add them to the house agency's `agents` join table. They do NOT get Agent role (ID 2) — the agency link is data-only for proposal/opportunity scoping
4. **House agency naming:** Consider naming it `"{PSP Name} Direct Sales"` to distinguish from external agent agencies

**Two proposal-building contexts to support:**

- **PSP Admin:** Sees ALL rates across all agencies. Can build proposals on behalf of any agent. Full pipeline visibility. ProposalBuilder needs a future "build on behalf" mode for this.
- **PSP Sales:** Scoped like an external agent. Sees only own prospects, only house agency rates. Creates and manages own direct-sale opportunities.

**Depends on:** V010 migration (PSP Sales role must exist)

---
## Completed Items

### Session: February 23, 2026

- ✅ **D-09:** Created `schema_version` table on master (also needs to be run on local dev and production)
- ✅ **D-11:** Built and tested `backup.sh` — mysqldump → gzip → Wasabi upload, 7-day local retention
- ✅ **D-12:** Built and tested `update.sh` — GitHub Releases API → SQL migrations → WAR deploy → Tomcat restart
- ✅ **D-15:** Created `/var/lib/tomcat10/data` directory on master
- ✅ **D-16:** Installed `mysql-connector-j-8.4.0.jar` in `/var/lib/tomcat10/lib/`
- ✅ **D-17:** Removed default ROOT webapps directory
- ✅ **D-18:** Confirmed awscli already working on Ubuntu 24.04 (no fix needed)
- ✅ **D-19:** Opened port 8080 in UFW
- ✅ **D-20:** Snapshotted master (superseded by v3 snapshot)
- ✅ **D-21:** GitHub PAT regenerated — no expiration, read-only, scoped to `Murphnd2/ams`
- ✅ **D-23:** Published GitHub release (removed draft status)
- ✅ Wasabi setup: created `ssa-backups` bucket, `ssa-backup-service` IAM user (WasabiFullAccess), configured AWS CLI `wasabi` profile on master
- ✅ Cron jobs: backup at 2:00 AM UTC, update at 2:30 AM UTC
- ✅ Fixed `ams_app` DB user/password in `ssa.properties`
- ✅ Updated `RELEASE_REPO` to actual GitHub repo URL in `ssa.properties`
- ✅ Deleted SSA-Demo VPS (freed cores)
- ✅ Final snapshot: `SSA-Master-Base-v3-2026-02-23` (old snapshots deleted)

### Session: February 22, 2026

- ✅ Created `SSA-PSP` Virtual Data Center on IONOS (US - Las Vegas)
- ✅ Provisioned SSA-Master vCPU server (2 cores, 4GB RAM, 80GB SSD, Ubuntu 24.04)
- ✅ Ran provisioning script — installed Java 17, MySQL 8.0, Tomcat 10, Certbot, AWS CLI
- ✅ Imported blank beta_ssa schema (186 tables/views, exported from local dev with all 9 migrations applied)
- ✅ Created `ams_app` MySQL user with grants on beta_ssa
- ✅ Configured Tomcat JNDI datasource in context.xml
- ✅ Created snapshot: `SSA-Master-Base-2026-02-22`
- ✅ Cloned snapshot to SSA-Demo server (162.254.26.75)
- ✅ Set PSP_ID=ssa_demo on clone
- ✅ Created first GitHub Release (v0.1.0-beta) with WAR attached
- ✅ Successfully pulled WAR from GitHub Releases API onto demo server
- ✅ Deployed WAR — app serving initialization page at http://162.254.26.75:8080
- ✅ Confirmed full deployment pipeline works end-to-end
