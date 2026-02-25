# SSA/AMS Deployment Strategy

**Last Updated:** February 22, 2026
**Status:** Planning / Pre-Implementation

---

## 1. Overview

This document defines the long-term strategy for deploying the AMS web application to multiple PSP (Professional Service Provider) clients. Each PSP operates as an independent instance — its own VPS, its own database, its own domain — all running the same application codebase.

### Core Principle

**One codebase, many instances.** Every PSP gets a cloned VPS running an identical application stack. PSP-specific data lives in the database (seeded at initialization). Infrastructure-level configuration lives in a properties file outside the WAR. The WAR itself is identical across all PSPs.

---

## 2. Infrastructure

### 2.1 Hosting

- **Provider:** IONOS Cloud
- **Per-PSP setup:** Each PSP gets its own Linux VPS (Ubuntu), cloned from a master image
- **Each VPS gets:** its own external IP address, its own domain (managed by the PSP's DNS)

### 2.2 Master VPS Image

A "gold master" VPS image is maintained with the full stack pre-installed and ready to clone:

| Component | Version | Location |
|-----------|---------|----------|
| Ubuntu | 24.x LTS | — |
| Java | OpenJDK 17 | `/usr/lib/jvm/java-17-openjdk-amd64` |
| Tomcat | 10.x | Base: `/var/lib/tomcat10`, Home: `/usr/share/tomcat10` |
| MySQL | 8.x | Standard package install |
| Certbot | Latest | For Let's Encrypt SSL |

**Pre-installed on the master image:**

- Empty `beta_ssa` database schema (all tables, views, stored procedures — no PSP data)
- Latest WAR file deployed to `/var/lib/tomcat10/webapps/`
- Config file template at `/var/lib/tomcat10/conf/ssa.properties` with `PSP_ID=UNINITIALIZED`
- Backup script (cron job) — see §5
- Update script (cron job) — see §6
- Health check script (cron job) — see §7
- Certbot installed (but not yet configured — requires DNS to be pointed first)

### 2.3 Standard Directory Layout (All PSP VPSes)

| Purpose | Path |
|---------|------|
| Tomcat base | `/var/lib/tomcat10` |
| Tomcat home | `/usr/share/tomcat10` |
| Application WAR | `/var/lib/tomcat10/webapps/ROOT.war` |
| Logs | `/var/lib/tomcat10/logs/` |
| Application data/file storage | `/var/lib/tomcat10/data/` |
| Application config | `/var/lib/tomcat10/conf/ssa.properties` |
| Backup scripts | `/opt/ssa/scripts/` |
| Local backup staging | `/opt/ssa/backups/` |

---

## 3. Configuration Strategy

Configuration is split into two layers, each serving a distinct purpose.

### 3.1 Infrastructure Config — `ssa.properties`

Located at `/var/lib/tomcat10/conf/ssa.properties`, this file holds values that are:

- Set once when the VPS is provisioned
- Never changed through the application UI
- Specific to the server environment, not the PSP's business data

**Contents:**

```properties
# PSP Identifier — human-readable, used for backup labels, logging, health reports
PSP_ID=UNINITIALIZED

# Database connection (currently in persistence.xml inside the WAR — to be externalized)
DB_HOST=localhost
DB_PORT=3306
DB_NAME=beta_ssa
DB_USER=ssa_app
DB_PASSWORD=<set during provisioning>

# File system paths
SAVE_PATH=/var/lib/tomcat10/data/
LOG_PATH=/var/lib/tomcat10/logs/

# Release management
RELEASE_REPO=https://api.github.com/repos/<org>/<repo>/releases
RELEASE_TOKEN=<read-only GitHub PAT>

# Backup target
WASABI_BUCKET=ssa-backups
WASABI_ACCESS_KEY=<set during provisioning>
WASABI_SECRET_KEY=<set during provisioning>
WASABI_REGION=us-east-1
```

**How the application reads this:** The `EmfListener` (or a new startup listener) reads this file on context initialization and makes values available to the application. The WAR never contains environment-specific values.

### 3.2 PSP Business Config — Database Constants Table

The existing `constant` table holds values that are:

- PSP-specific business configuration
- Potentially editable through the application (now or in the future)
- Seeded during database initialization via `initialize.jsp`

**Current constants seeded by `DatabaseInitializer`:**

| Constant | Purpose | Source |
|----------|---------|--------|
| `SMTP_SERVER` | Email sending | initialize.jsp form |
| `SMTP_PORT` | Email sending | initialize.jsp form |
| `SMTP_USER` | Email sending | initialize.jsp form |
| `SMTP_PASSWORD` | Email sending | initialize.jsp form |
| `WEB_PATH` | Domain for email links | initialize.jsp form |
| `SUMMIT_PATH` | Path to Summit data | initialize.jsp form |
| `SSL_PORT` | HTTPS port (always "443") | Hardcoded |
| `FALSE_CLOSE` | Business logic date | Hardcoded |
| `LOGO_NAVBAR` | Navbar logo path (PSP-customizable) | Hardcoded default, updatable via Branding page |
| `LOGO_LOGIN` | Login page logo path (PSP-customizable) | Hardcoded default, updatable via Branding page |
| `FAVICON` | Browser tab icon path (PSP-customizable) | Hardcoded default, updatable via Branding page |
| `SYS_HEALTH_EMAIL_TO` | Health check recipient | Hardcoded |
| `SYS_HEALTH_SMTP_SERVER` | Health check SMTP server | Hardcoded |
| `SYS_HEALTH_SMTP_PORT` | Health check SMTP port | Hardcoded |
| `SYS_HEALTH_SMTP_USER` | Health check SMTP user | Hardcoded |
| `SYS_HEALTH_SMTP_PASSWORD` | Health check SMTP password | Hardcoded |
| `SYS_HEALTH_ENABLED` | Health check on/off switch | Hardcoded (true) |
| `SYS_HEALTH_EMAIL_FROM` | Health check sender address | Hardcoded |

### 3.3 Migration Plan: What Moves to `ssa.properties`

| Value | Currently | Should Be | Reason |
|-------|-----------|-----------|--------|
| `SAVE_PATH` | DB constant (hardcoded `C:\\data\\`) | `ssa.properties` | Infrastructure path, same on all Linux VPSes |
| `LOG_PATH` | Hardcoded in `EmfListener` (Windows path) | `ssa.properties` | Infrastructure path |
| DB connection | `persistence.xml` inside WAR | `ssa.properties` | Must vary per VPS without rebuilding WAR |
| `SSL_PORT` | DB constant | Can stay | Rarely changes, fine in DB |
| SMTP settings | DB constants via initialize.jsp | Stay in DB | PSP-specific, may be editable later |
| `WEB_PATH` | DB constant via initialize.jsp | Stay in DB | PSP-specific |
| `SUMMIT_PATH` | DB constant via initialize.jsp | Stay in DB | PSP-specific |

---

## 4. Database Initialization

### 4.1 Current Flow

1. Fresh VPS has empty `beta_ssa` schema (tables/views only, no data)
2. Tomcat starts, `EmfListener` checks for `SSL_PORT` constant — finds none, skips global data load
3. PSP navigates to `/initialize.jsp` (public, no auth required)
4. PSP fills out form: company name, contact info, address, domain, SMTP settings, tax ID, deployment key
5. Form POSTs to `/InitializeDataBase` → calls `DatabaseInitializer.initializeDataBase()`
6. Initializer seeds: sequence tracker, activity statuses, PSP entity, persons, users/roles, agency, employers, constants, initialization checklist, completion note
7. On next restart, `EmfListener` finds `SSL_PORT=443`, loads global data normally

### 4.2 What the Initializer Creates (Seed Data)

**Universal (same for every PSP):**

- Sequence tracker (`SEQ_GEN` starting at 200)
- Activity statuses (3: Waiting on Them, No Change, Waiting on Us)
- User roles (admin, basic, master admin)
- Template purposes and required task lists
- The initialization checklist (guides PSP through next steps)
- Constants (`FALSE_CLOSE`, `SSL_PORT`)

**PSP-specific (from the form):**

- PSP entity with address
- Primary contact person + user account
- Agency record
- SMTP constants
- Domain/web path constant
- Summit path constant

### 4.3 Post-Initialization Guided Workflow

After initialization completes, the PSP sees a checklist that walks them through:

1. Get Summit Exports (with link to export instructions doc)
2. Upload Exports from Summit
3. Import Data from Uploads
4. Update Working Tables from Imports

This checklist is created as an actual `CheckList` entity with `ToDo` items, so progress is tracked in the application itself.

---

## 5. Backup Strategy

### 5.1 Database Backups

- **Frequency:** Nightly (cron job in a low-traffic window)
- **Method:** `mysqldump` of the `beta_ssa` schema
- **Local staging:** Compressed dump saved to `/opt/ssa/backups/`
- **Remote storage:** Uploaded to Wasabi bucket, organized by PSP

**Wasabi bucket structure:**

```
ssa-backups/
├── acme_benefits/
│   ├── db/
│   │   ├── beta_ssa_2026-02-22.sql.gz
│   │   ├── beta_ssa_2026-02-21.sql.gz
│   │   └── ...
│   └── logs/
│       └── (optional: periodic log archives)
├── midwest_admin/
│   ├── db/
│   └── logs/
└── ...
```

- **Folder name = `PSP_ID`** from `ssa.properties`
- **Retention:** TBD (recommend: 30 days daily, then weekly for 6 months)

### 5.2 Backup Script Behavior

The backup script reads `PSP_ID` from `/var/lib/tomcat10/conf/ssa.properties` and uses it as the Wasabi folder name. If `PSP_ID=UNINITIALIZED`, the script does not run (master image safety).

---

## 6. Release Management

### 6.1 Release Artifacts

Releases are published to **GitHub Releases** on the project repository. Each release includes:

- The WAR file (e.g., `ssa-1.2.0.war`)
- Any new database migration SQL files (e.g., `V010__add_opportunity_stage.sql`)
- Release notes describing changes

### 6.2 Versioning Convention

- WAR files: semantic versioning (`ssa-MAJOR.MINOR.PATCH.war`)
- Database migrations: numbered sequentially (`V001__initial.sql`, `V002__sales_pipeline.sql`, etc.)
- A WAR version and a database migration version must always be compatible — release notes specify which migrations are required for each WAR version

### 6.3 Database Migration Tracking

Each PSP database contains a `schema_version` table:

```sql
CREATE TABLE schema_version (
    version VARCHAR(10) NOT NULL,
    description VARCHAR(200),
    script_name VARCHAR(200),
    applied_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (version)
);
```

The update script checks this table to determine which migrations have been applied and runs any new ones in order.

### 6.4 Update Script Behavior (Per-VPS Cron Job)

Runs nightly (after backup completes):

1. Read `PSP_ID` from `ssa.properties` — skip if `UNINITIALIZED`
2. Check GitHub Releases API for the latest release tag
3. Compare to the currently deployed WAR version (stored in `/opt/ssa/current_version.txt`)
4. If a new version is available:
   a. Download new migration SQL files
   b. Apply migrations in order (checking `schema_version` table)
   c. If migrations succeed: download new WAR, back up current WAR, deploy new WAR, restart Tomcat
   d. If migrations fail: stop, do not deploy WAR, alert via health check (see §7)
5. Log all actions to `/opt/ssa/logs/update.log`

### 6.5 Rollback

- Previous WAR is kept at `/opt/ssa/backups/ssa-previous.war`
- Database rollback = restore from the nightly backup taken before the update ran
- Manual intervention: SSH into the VPS, swap WAR files, restore DB dump, restart Tomcat
- Automated rollback is a future enhancement — manual is acceptable for the first 5-10 PSPs

---

## 7. Health Monitoring

### 7.1 Phase 1: Email Digest

Each VPS runs a nightly health check script that collects:

- Tomcat status (running/stopped)
- MySQL status (running/stopped)
- Disk usage (total, used, available)
- Last successful backup timestamp
- Currently deployed WAR version
- Current database migration version
- Last update script result (success/failure/skipped)
- Any errors in the last 24 hours of Tomcat logs

The script emails a summary to a designated address (e.g., `health@monitor.superiorstate.net`). The email subject includes the `PSP_ID` for easy filtering/sorting.

### 7.2 Phase 2: Dashboard (Future)

A master admin dashboard built into the AMS application, accessible only to users with the "Master Admin" role. This would:

- Receive health check data from all PSP VPSes (each VPS POSTs its health report to a central endpoint)
- Display a summary grid: one row per PSP, showing status indicators
- Alert on failures (red indicators for down services, failed backups, failed updates)
- Show version drift (which PSPs are behind on WAR or migration versions)

The dashboard lives in the same codebase (not a separate site) and is deployed on a "master" VPS that you control. Other PSP VPSes would not see this view — it's role-gated.

---

## 8. New PSP Onboarding Procedure

### 8.1 Pre-Onboarding (Your Side)

1. Clone the master VPS image in IONOS
2. SSH into the new VPS
3. Set `PSP_ID` in `/var/lib/tomcat10/conf/ssa.properties` (e.g., `PSP_ID=acme_benefits`)
4. Set database credentials in `ssa.properties`
5. Set Wasabi credentials in `ssa.properties`
6. Create the data directory: `sudo mkdir -p /var/lib/tomcat10/data && sudo chown tomcat:tomcat /var/lib/tomcat10/data`
7. Verify Tomcat starts and serves the uninitialized application
8. Note the VPS external IP address
9. Communicate IP to the PSP with DNS instructions

### 8.2 DNS Setup (PSP's Side)

The PSP creates an A record pointing their chosen domain to the VPS IP address.

### 8.3 SSL Provisioning (Your Side, After DNS)

Once the PSP confirms DNS is pointed:

```bash
sudo certbot --standalone -d their.domain.com
```

Configure Tomcat to use the certificate (or use a reverse proxy like Nginx).

### 8.4 Application Initialization (PSP's Side)

1. PSP navigates to `https://their.domain.com/initialize.jsp`
2. Fills out the initialization form (company info, contact, SMTP, etc.)
3. Enters the deployment key
4. Submits → database is seeded with their PSP data
5. Application restarts or re-initializes → full functionality available
6. PSP follows the initialization checklist to upload and import their data from Summit

---

## 9. Security Considerations

### 9.1 Deployment Key

The `/InitializeDataBase` endpoint is publicly accessible (no auth required — the database has no users yet). The deployment key in the form is the only guard against unauthorized initialization. This key must be:

- Validated server-side before any database writes occur
- Strong and unique (not a default or guessable value)
- Communicated to the PSP securely (not in the same email as the IP address)

### 9.2 SSH Access

All VPS administration (config changes, manual DB access, troubleshooting) is done via SSH. Each VPS should use key-based authentication. Password-based SSH login should be disabled on the master image.

### 9.3 Initialize Endpoint Protection

After initialization completes, the `/InitializeDataBase` endpoint should be effectively disabled. The current mechanism for this is the `SSL_PORT` constant check in `EmfListener` — once initialization sets `SSL_PORT=443`, the application knows it's initialized. The initialize endpoint itself should also check this and refuse to run a second time.

---

## 10. Branching & Release Workflow

### 10.1 Branch Strategy

| Branch | Purpose | Status |
|--------|---------|--------|
| `main` | **Production releases.** Tagged, released, deployed to PSP servers. | Active — merge here when ready to release |
| `refactor/modernize-architecture` | Active development. All new features and cleanup work. | Current working branch |
| `beta` | Legacy. Represents the pre-cleanup codebase. | Retired — keep for historical reference only |
| Feature branches | Short-lived branches off the working branch for specific features | Create as needed |

### 10.2 Release Process

When ready to deploy a new version to PSP servers:

1. **Merge working branch into main:**
   ```bash
   git checkout main
   git merge refactor/modernize-architecture
   ```

2. **Tag the release:**
   ```bash
   git tag v1.0.0
   ```
   Use semantic versioning: `vMAJOR.MINOR.PATCH`
   - MAJOR = breaking changes or major milestones
   - MINOR = new features
   - PATCH = bug fixes

3. **Push to GitHub:**
   ```bash
   git push origin main --tags
   ```

4. **Build the WAR:**
   ```bash
   mvn clean package -P server
   ```
   The WAR is built with the server profile (JNDI datasource). Output is in `target/`.

5. **Create GitHub Release:**
   - Go to the repo on GitHub → Releases → Draft a new release
   - Choose the tag you just pushed
   - Title: `v1.0.0` (or descriptive name)
   - Description: list what changed (features, fixes, migration scripts required)
   - Attach: the WAR file from `target/`
   - Attach: any new SQL migration scripts
   - Publish

6. **PSP VPSes pick up the release** via their nightly update script (or manual trigger).

### 10.3 Migration Script Naming Convention

SQL migration files attached to releases follow this pattern:

```
V001__initial_schema.sql
V002__sales_pipeline.sql
V003__sales_pipeline_2.sql
...
```

Each release's notes specify which migrations are required. The update script on each VPS checks the `schema_version` table and applies any new ones in order.

---

## 11. Code Changes Required for Multi-PSP Readiness

See `docs/deployment_backlog.md` for the tracked list of specific changes needed.

High-level categories:

1. **Externalize infrastructure config** — create `ssa.properties` loader, remove hardcoded paths
2. **Fix OS-specific paths** — `SAVE_PATH` default, `EmfListener` error log path
3. **Clean up test data** — remove dev artifacts from initializer
4. **Harden initialization endpoint** — deployment key validation, prevent re-initialization
5. **Add `schema_version` table** — for migration tracking
6. **Build the update and backup scripts** — shell scripts for the master image
7. **Build the health check script** — email-based reporting
8. **Externalize database connection** — move from `persistence.xml` to `ssa.properties`
