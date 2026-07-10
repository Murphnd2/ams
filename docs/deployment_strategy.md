# SSA/AMS Deployment Strategy

**Last Updated:** March 2, 2026
**Status:** Active — BPO deployed, Demo PSP next

---

## 1. Overview

This document defines the long-term strategy for deploying the AMS web application to multiple PSP (Professional Service Provider) clients. Each PSP operates as an independent instance — its own VM, its own database, its own domain — all running the same application codebase.

### Core Principle

**One codebase, many instances.** Every PSP gets a cloned VM running an identical application stack. PSP-specific data lives in the database (seeded at initialization). Infrastructure-level configuration lives in a properties file outside the WAR. The WAR itself is identical across all PSPs.

---

## 2. Infrastructure

### 2.1 Hosting

- **Provider:** IONOS Cloud — **Data Center Designer (DCD)** product (not VPS Cloud Panel)
- **Management console:** DCD at https://dcd.ionos.com
- **Per-PSP setup:** Each PSP gets its own Linux VM (Ubuntu), cloned from a master snapshot in the DCD
- **Each VM gets:** its own external IP address (reserved via DCD IP Manager), its own domain (managed by the PSP's DNS)
- **VDC location:** US-Las Vegas (SSA-PSP VDC)

#### 2.1.1 IONOS DCD IP Management

Static IPs are reserved via **Menu → Network Services → IP Management** in the DCD (not the Cloud Panel). IPs are allocated as consecutive IP blocks. Key notes:

- You cannot reserve a specific IPv4 address — IONOS assigns a random address from the pool
- If you return a static IP, you cannot reserve it again afterward
- DHCP-assigned IPs are dynamic and may change if the VM is deallocated — use reserved static IPs for production
- DHCP-assigned IPs are stable enough for demo/test VMs as long as the VM is not deallocated
- **Known issue (2026-03-02):** Received "error occurred while reserving ip block" when attempting to reserve a second IP block. Root cause unknown — may be an account-level limit or transient DCD error. Investigate with IONOS support if this recurs. See D-22 in `docs/deployment_backlog.md`.

### 2.2 Master VM Image

A "gold master" VM snapshot is maintained with the full stack pre-installed and ready to clone:

| Component | Version | Location |
|-----------|---------|----------|
| Ubuntu | 24.x LTS | — |
| Java | OpenJDK 17 | `/usr/lib/jvm/java-17-openjdk-amd64` |
| Tomcat | 10.x | Base: `/var/lib/tomcat10`, Home: `/usr/share/tomcat10` |
| MySQL | 8.x | Standard package install, `lower_case_table_names = 1` |
| Nginx | Latest | SSL termination, reverse proxy to Tomcat 8080 |
| Certbot | Latest | Let's Encrypt SSL (nginx plugin for zero-downtime renewal) |

**Pre-installed on the master image:**

- Empty beta_ssa database schema at V057 (all tables, views, stored procedures — no PSP data)
- `schema_version` table populated with V001–V057 tracking records
- MySQL configured with `lower_case_table_names = 1` (required for EclipseLink compatibility on Linux)
- `ams_app` MySQL user created with password matching `context.xml`
- No WAR deployed (clones pull via GitHub Releases update script)
- Config file template at `/var/lib/tomcat10/conf/ssa.properties` with `PSP_ID=UNINITIALIZED` and `SYSTEM_URL=` (blank)
- Backup script (cron job) — see §5
- Update script (cron job) — see §6
- Health check script (cron job) — see §7
- Nginx installed with `python3-certbot-nginx` plugin (configured per-domain after DNS is pointed)
- Certbot installed (not yet configured — requires DNS to be pointed first)

**Current snapshot:** `SSA-Master-Base-v9-2026-03-20` (V057 schema, nginx SSL, `lower_case_table_names=1`, hostname `ssa-master`, no WAR)

**Master VPS:** 208.94.39.77 (`master.superiorstate.biz`)

### 2.3 Standard Directory Layout (All PSP VMs)

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

### 3.1 Infrastructure Config (`ssa.properties`)

Lives at `/var/lib/tomcat10/conf/ssa.properties` on every VM. Set once at provisioning time. Contains:

- `PSP_ID` — human-readable identifier (used for backups, logging, health reports)
- `SYSTEM_URL` — the full URL for this instance (set during provisioning, used by VendorManager for partnership requests)
- `DEPLOYMENT_KEY` — secret key for initialization endpoint
- Database credentials (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`)
- Wasabi S3 credentials (`S3_ENDPOINT`, `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`)
- File paths (`SAVE_PATH`, `LOG_PATH`, `BRANDING_PATH`)
- Chatbot settings (`CHATBOT_ENABLED`, `ANTHROPIC_API_KEY`)
- Release management (`RELEASE_REPO`, `RELEASE_TOKEN`)
- Backup target (`WASABI_BUCKET`, `WASABI_ENDPOINT`, `WASABI_REGION`)
- Health monitoring (`HEALTH_EMAIL_TO`, `HEALTH_EMAIL_FROM`)
- System health email (`SYS_HEALTH_*` keys — SMTP config for health check script)

### 3.2 Application Config (Database Constants)

Stored in the `constant` table, seeded during initialization. Contains PSP-specific business configuration:

- SMTP settings (email host, port, user, password, from address)
- Domain/web path
- Summit path (Datapath integration)
- Tax ID
- SSL port
- Feature flags

---

## 4. Database Initialization

### 4.1 Current Flow

1. Fresh VM has empty `beta_ssa` schema (tables/views only, no data)
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
- Pre-release versions: `0.x.y` (e.g., `v0.31.0` — "BPO cross-system architecture, V031 schema")
- Conference-ready: `1.0.0`
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

### 6.4 Update Script Behavior (Per-VM Cron Job)

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
- Manual intervention: SSH into the VM, swap WAR files, restore DB dump, restart Tomcat
- Automated rollback is a future enhancement — manual is acceptable for the first 5-10 PSPs

---

## 7. Health Monitoring

### 7.1 Phase 1: Email Digest

Each VM runs a nightly health check script that collects:

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

- Receive health check data from all PSP VMs (each VM POSTs its health report to a central endpoint)
- Display a summary grid: one row per PSP, showing status indicators
- Alert on failures (red indicators for down services, failed backups, failed updates)
- Show version drift (which PSPs are behind on WAR or migration versions)

The dashboard lives in the same codebase (not a separate site) and is deployed on a "master" VM that you control. Other PSP VMs would not see this view — it's role-gated.

---

## 8. New PSP Onboarding Procedure

### 8.1 Pre-Onboarding (Your Side)

1. Clone the master VM snapshot in IONOS DCD
2. Reserve a static IP in DCD IP Manager and assign to the new VM's NIC
3. SSH into the new VM
4. Set `PSP_ID` in `/var/lib/tomcat10/conf/ssa.properties` (e.g., `PSP_ID=acme_benefits`)
5. Set `SYSTEM_URL` (e.g., `https://acme.superiorstate.biz`)
6. Set database credentials in `ssa.properties`
7. Set Wasabi credentials in `ssa.properties`
8. Create the data directory: `sudo mkdir -p /var/lib/tomcat10/data && sudo chown tomcat:tomcat /var/lib/tomcat10/data`
9. Verify Tomcat starts and serves the uninitialized application
10. Note the VM external IP address
11. Communicate IP to the PSP with DNS instructions

### 8.2 DNS Setup (PSP's Side)

The PSP creates an A record pointing their chosen domain to the VM IP address.

### 8.3 SSL Provisioning (Your Side, After DNS)

Nginx handles SSL termination and reverse-proxies to Tomcat on port 8080. Tomcat does not handle HTTPS directly.

Once the PSP confirms DNS is pointed:

1. Create an nginx site config (see `docs/tomcat_ssl_setup.md` for template)
2. Generate the certificate using the nginx plugin (zero-downtime renewal):
   ```bash
   sudo certbot certonly --nginx -d their.domain.com -d www.their.domain.com
   ```
3. Reload nginx: `sudo systemctl reload nginx`

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

All VM administration (config changes, manual DB access, troubleshooting) is done via SSH. Each VM should use key-based authentication. Password-based SSH login should be disabled on the master image.

### 9.3 Initialize Endpoint Protection

After initialization completes, the `/InitializeDataBase` endpoint should be effectively disabled. The current mechanism for this is the `SSL_PORT` constant check in `EmfListener` — once initialization sets `SSL_PORT=443`, the application knows it's initialized. The initialize endpoint itself should also check this and refuse to run a second time.

### 9.4 MySQL Password Handling

MySQL passwords containing special characters (`!`, `$`, `\`, `` ` ``) must be set using the **interactive MySQL shell**, not via the `-e` flag. Bash interprets these characters before passing them to MySQL, causing CREATE/ALTER USER failures. Always use:

```bash
LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql --socket=/var/run/mysqld/mysqld.sock -u root -p
```

Then paste the password directly at the `mysql>` prompt.

---

## 10. Branching & Release Workflow

### 10.1 Branch Strategy

| Branch | Purpose | Status |
|--------|---------|--------|
| `main` | Legacy. Frozen at 2026-02-12 (`390ad7f`); no longer the default branch and no longer part of the release flow. | Retired — historical reference only |
| `refactor/modernize-architecture` | Active development **and release branch**. Repo default branch since 2026-07-09. Release tags are created directly on this branch. | Current working branch |
| `beta` | Legacy. Represents the pre-cleanup codebase. | Retired — keep for historical reference only |
| Feature branches | Short-lived branches off the working branch for specific features | Create as needed |

### 10.2 Release Process (Tag-First)

> **History (2026-07-09):** ~85 release tags (`v0.31.0`–`v0.69.02`) were created through the GitHub Releases web UI by typing a new tag name, which creates the tag on the **default branch HEAD** — at the time, a `main` frozen since 2026-02-12. All of those tags point at the same stale commit, so per-version source traceability for that range is lost. Deploys were never affected (`update.sh` ships release *assets* — a locally built WAR + SQL — not source built from tags). Two fixes are in effect: the default branch is now `refactor/modernize-architecture`, and every release follows the tag-first procedure below.

When ready to deploy a new version:

1. **Commit first.** Everything going into the release must be committed on `refactor/modernize-architecture` **before** the WAR is built. Never build a release WAR from a dirty working tree — a tag is only trustworthy if the tree it points at is the tree that was built. (The v0.69.0x releases shipped uncommitted white-label code for exactly this reason.)

2. **Build the WAR:**

```powershell
   mvn clean package -P server
```

   Stage the WAR and any new `V0NN__*.sql` migration scripts in the local `release/` folder (untracked; never committed).

3. **Pick the version number** (`v0.NN.PP`):
   - Bump **PP** (patch) for WAR-only releases with no new migration (e.g., `v0.69.02` → `v0.69.03`)
   - Bump **NN** and reset patch (e.g., → `v0.70.00`) when the release carries a new migration script
   - The new tag must sort **above** production's `/opt/ssa/current_version.txt` under `sort -V`, or `update.sh` will not apply it

4. **Tag the release commit and push the tag:**

```powershell
   git tag v0.NN.PP
   git push origin v0.NN.PP
```

5. **Create the GitHub Release:**
   - Repo → Releases → Draft a new release
   - **Select the existing tag `v0.NN.PP` from the dropdown — never type a new tag name.** Typing a new name creates the tag on the default branch HEAD instead of the release commit.
   - Notes: what changed and which migrations are required
   - Attach: `ROOT.war` plus any new `V0NN__*.sql` from `release/`
   - Publish

6. **Deploy:** production applies it at the 02:30 cron, or trigger manually:

```bash
   sudo /opt/ssa/scripts/update.sh
```

   `update.sh` applies any missed migrations oldest-first, then swaps the WAR, then records the version in `current_version.txt`.

### 10.3 Migration Script Naming Convention

SQL migration files attached to releases follow this pattern:

```
V001__initial_schema.sql
V002__sales_pipeline.sql
V003__sales_pipeline_2.sql
...
```

Each release's notes specify which migrations are required. The update script on each VM checks the `schema_version` table and applies any new ones in order.

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

---

# deployment_strategy.md — Section 12 Replacement

Replace the entire §12 block in `docs/deployment_strategy.md` with:

---

## 12. Current Deployed Instances

| Instance | URL | IP | Type | Schema | Status |
|----------|-----|----|------|--------|--------|
| Production PSP | https://superiorstate.biz | (production IP) | PSP | V024 | Running |
| Demo PSP | https://demo.superiorstate.biz | 192.152.28.73 (static) | PSP | V037 | Running, seeded with demo data, release V0.37.0 |
| BPO | https://bpo.superiorstate.biz | 158.222.102.168 (DHCP) | BPO | V037 | Running, initialized, partnered with Demo PSP, release V0.37.0 |
| Master | master.superiorstate.biz | 208.94.39.77 | Master image | V037 | Snapshot v8 (`SSA-Master-Base-v9-2026-03-20`), stopped |

**Notes:**
- Demo PSP and BPO are partnered — cross-system BPO task delegation is functional between the two instances.
- BPO IP is DHCP-assigned (stable as long as VM is not deallocated). Static IP reservation was not needed for a demo/test instance.
- Production remains intentionally isolated at V024 until conference demo infrastructure is proven.
- Demo PSP was provisioned March 2–3, 2026 from master snapshot v7. See `docs/analysis/session_summary_2026-03-03_demo_standup.md` for full standup details and lessons learned.
