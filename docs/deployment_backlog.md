# Deployment Backlog

**Last Updated:** February 22, 2026
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
2. What is it validated against? (A hardcoded value? A value in `ssa.properties`?)
3. If validation is missing, add it — this is the only thing preventing unauthorized database initialization on a fresh VPS.

**Recommended approach:** Store the expected deployment key in `ssa.properties`. The servlet reads it from there and compares to the submitted value.

**Depends on:** D-01

---

### D-06: Prevent Re-Initialization

**Priority:** HIGH
**Status:** Needs investigation

After the database is initialized, the `/InitializeDataBase` endpoint should refuse to run again. The current `EmfListener` checks for `SSL_PORT=443` to determine if the DB is initialized, but does the initialization servlet itself check this?

**Fix:** At the top of `DatabaseInitializer.initializeDataBase()`, check if `SSL_PORT` constant already exists in the database. If yes, abort and return an error.

---

### D-07: Externalize Database Connection from `persistence.xml`

**Priority:** HIGH — Required for identical WARs across PSPs
**Status:** Not started
**File:** `src/main/resources/META-INF/persistence.xml`

Currently the database URL, username, and password are in `persistence.xml` inside the WAR. This means the WAR is environment-specific and can't be deployed identically to every VPS.

**Fix:** Use EclipseLink's ability to override persistence properties programmatically. The `EmfListener` (or new config listener) reads DB credentials from `ssa.properties` and passes them as a properties map to `Persistence.createEntityManagerFactory("ssaPU", propertiesMap)`.

**Depends on:** D-01

---

### D-08: Audit Hardcoded Entity IDs in `DatabaseInitializer`

**Priority:** MEDIUM
**Status:** Not started
**File:** `src/main/java/net/superiorstate/ams/data/service/DatabaseInitializer.java`

The initializer uses hardcoded IDs: Person 104, Person 50, PSP 4, Agency 14, Employer -1, CheckList 29, various Task IDs (28, 30, 32, 34). The sequence seed starts at 200.

**Risk:** If the blank schema ever includes auto-increment tables or pre-seeded rows that conflict with these IDs, initialization would fail.

**Action:** Verify that the blank schema has no conflicting IDs. Document the reserved ID ranges. Consider whether any of these can use auto-generated IDs instead.

---

### D-09: Create `schema_version` Table

**Priority:** MEDIUM — Required before automated updates
**Status:** Not started

Add a `schema_version` table to the blank database schema:

```sql
CREATE TABLE schema_version (
    version VARCHAR(10) NOT NULL,
    description VARCHAR(200),
    script_name VARCHAR(200),
    applied_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (version)
);
```

Retroactively insert rows for all existing migrations (scripts 1-9 in `migration_tracker.md`) on production and dev environments.

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

### D-11: Build Backup Script

**Priority:** MEDIUM — Required before first PSP deployment
**Status:** Not started

Shell script installed on the master image at `/opt/ssa/scripts/backup.sh`:

- Reads `PSP_ID` from `ssa.properties`
- Skips if `PSP_ID=UNINITIALIZED`
- Runs `mysqldump` on `beta_ssa`
- Compresses output
- Uploads to Wasabi bucket under `PSP_ID/db/` folder
- Logs results
- Cron: nightly, before update script runs

---

### D-12: Build Update Script

**Priority:** MEDIUM — Required before first PSP deployment
**Status:** Not started

Shell script installed on the master image at `/opt/ssa/scripts/update.sh`:

- Reads `PSP_ID` from `ssa.properties`
- Skips if `UNINITIALIZED`
- Checks GitHub Releases API for latest release
- Compares to `/opt/ssa/current_version.txt`
- Downloads and applies new SQL migrations (checking `schema_version`)
- Downloads and deploys new WAR (with backup of previous)
- Restarts Tomcat
- Logs results
- Cron: nightly, after backup completes

**Depends on:** D-09, D-10

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

### D-15: Create Standard Data Directory on Master Image

**Priority:** HIGH — Simple, do during master image setup
**Status:** Not started

```bash
sudo mkdir -p /var/lib/tomcat10/data
sudo chown tomcat:tomcat /var/lib/tomcat10/data
```

This is a one-time command on the master image. All clones inherit it.

---

### D-16: Update Master Image — MySQL Driver in Tomcat Lib

**Priority:** HIGH — Without this, every clone fails on first WAR deploy
**Status:** Not started (needs master VPS restarted)

Copy `mysql-connector-j-8.4.0.jar` into `/var/lib/tomcat10/lib/` on the master image. The JNDI connection pool needs the driver in Tomcat's classloader, not just inside the WAR.

Command: `cp mysql-connector-j-8.4.0.jar /var/lib/tomcat10/lib/`

Source: Extract from WAR or download directly from Maven Central.

---

### D-17: Update Master Image — Remove Default ROOT Directory

**Priority:** HIGH — Without this, Tomcat serves its default page instead of the WAR
**Status:** Not started (needs master VPS restarted)

The Ubuntu `tomcat10` package installs a default `ROOT/` directory in webapps. This takes precedence over `ROOT.war`. Delete it on the master so clones don't have this problem.

Command: `rm -rf /var/lib/tomcat10/webapps/ROOT`

---

### D-18: Update Master Image — Fix awscli Install in Provisioning Script

**Priority:** LOW — Only matters if script is re-run on a fresh VPS
**Status:** Not started

The `apt install awscli` command fails on Ubuntu 24.04. Replace with the official AWS installer:

```bash
curl -s "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "/tmp/awscliv2.zip"
unzip -q /tmp/awscliv2.zip -d /tmp
/tmp/aws/install
rm -rf /tmp/awscliv2.zip /tmp/aws
```

---

### D-19: Update Master Image — Open Port 8080 in UFW

**Priority:** MEDIUM — Needed until reverse proxy (Nginx) is set up
**Status:** Not started

Tomcat listens on 8080. The provisioning script's firewall only opens 22, 80, 443. Either:

- Option A: Add `ufw allow 8080/tcp` to the master (simple, works now)
- Option B: Set up Nginx as a reverse proxy from 80/443 → 8080 (proper, do later)

Decision: Use Option A for now, plan Option B for when SSL/Certbot is configured.

---

### D-20: Re-Snapshot Master Image After Updates

**Priority:** HIGH — Blocked by D-16, D-17, D-18, D-19
**Status:** Not started

After applying D-16 through D-19 on the master VPS:

1. Start SSA-Master in IONOS DCD
2. SSH in and apply the fixes
3. Stop Tomcat, verify clean state (no WAR deployed, no data in DB)
4. Create new snapshot: `SSA-Master-Base-v2-2026-MM-DD`
5. Stop SSA-Master

---

### D-21: Regenerate GitHub Personal Access Token

**Priority:** HIGH — Security: token was exposed during setup session
**Status:** Not started

Go to https://github.com/settings/tokens and regenerate the `SSA-VPS-Deploy` token. Update the token value in `ssa.properties` on any VPS that uses it.

---

### D-22: Investigate IONOS vCPU Core Quota

**Priority:** MEDIUM — Affects how many PSP VPSes can run simultaneously
**Status:** Not started

During demo setup, IONOS blocked provisioning due to an 8-core personal limit. Need to understand:

- What is the default core quota for the account?
- How do you request an increase? (Support ticket? Self-service?)
- Is the quota per-VDC or per-account?
- What is the cost model — pay-per-use for cores, or fixed allocation?
- Can stopped VPSes release their cores back to the quota? (Yes — confirmed during this session)
- What quota do we need for N PSP clients? (2 cores × N servers + 2 for master when running)

Planning note: At 2 cores per PSP VPS, an 8-core quota supports 4 simultaneous PSPs (with master stopped). For 10+ PSPs, a quota increase will be required.

---

### D-23: Publish GitHub Release (Remove Draft Status)

**Priority:** MEDIUM
**Status:** Not started

The v0.1.0-beta release was created as a draft. For the automated update scripts to find it via the API, it needs to be published (not draft). Go to GitHub → Releases → edit → uncheck draft → publish.

Note: Draft releases don't appear in the standard releases API endpoint, which is why the initial `curl` download returned "Not Found".

---

## Completed Items

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
