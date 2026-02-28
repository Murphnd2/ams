# PSP Deployment Runbook

**Last Updated:** February 27, 2026 (Session 7)
**Reference:** `docs/deployment_strategy.md` for full architecture context
**SSL Reference:** `docs/tomcat_ssl_setup.md` for detailed SSL instructions

---

## Pre-Deployment Prerequisites

Before deploying any PSP, ensure:

- [ ] Master VPS snapshot is current (`SSA-Master-Base-v5-2026-02-27` or newer)
- [ ] Latest release published to GitHub Releases (WAR + any migration SQL)
- [ ] `schema_version` table is up to date on the master image (currently V024)
- [ ] Blank schema dump exists at `/opt/ssa/schema/beta_ssa_blank.sql` on master image (currently V024)
- [ ] Health check script installed at `/opt/ssa/scripts/healthcheck.sh` on master image
- [ ] PSP has provided: company name, contact info, email, domain name, SMTP credentials, Summit path, tax ID

---

## Phase 1: Provision VPS (Your Side — IONOS Console)

1. [ ] Clone master snapshot in IONOS (SSA-PSP VDC, US-Las Vegas)
2. [ ] Assign a name to the new VPS (e.g., `SSA-AcmeBenefits`)
3. [ ] **Reserve a static IP** in IONOS (Network → IP Management) and assign to this VPS
4. [ ] Start the VPS
5. [ ] Note the static IP address: `_______________`

---

## Phase 2: Configure VPS (Your Side — SSH)

6. [ ] SSH into the new VPS
7. [ ] Edit `/var/lib/tomcat10/conf/ssa.properties` — set PSP-specific values:
   ```properties
   PSP_ID=<short_identifier>          # e.g., acme_benefits (used for backups, logs, health emails)
   DEPLOYMENT_KEY=<unique_strong_key> # e.g., UUID or random passphrase
   S3_ACCESS_KEY=<wasabi_key>         # For file storage
   S3_SECRET_KEY=<wasabi_secret>
   ```
   The following are pre-configured on the master image and typically don't need changes:
   ```properties
   # Pre-configured (verify, don't change unless needed)
   DB_HOST=localhost
   DB_PORT=3306
   DB_NAME=beta_ssa
   DB_USER=ams_app
   DB_PASSWORD=ams_app_2026
   SAVE_PATH=/var/lib/tomcat10/data/
   LOG_PATH=/var/lib/tomcat10/logs/
   BRANDING_PATH=/var/lib/tomcat10/branding/
   CHATBOT_ENABLED=false
   ANTHROPIC_API_KEY=
   RELEASE_REPO=https://api.github.com/repos/Murphnd2/ams/releases
   RELEASE_TOKEN=
   WASABI_BUCKET=ssa-backups
   WASABI_ENDPOINT=https://s3.us-east-1.wasabisys.com
   WASABI_REGION=us-east-1
   SYS_HEALTH_EMAIL_TO=kevin@superiorstate.net
   SYS_HEALTH_SMTP_SERVER=mail.smtp2go.com
   SYS_HEALTH_SMTP_PORT=2525
   SYS_HEALTH_SMTP_USER=amsSystemHealth
   SYS_HEALTH_SMTP_PASSWORD=<set on master>
   SYS_HEALTH_ENABLED=true
   SYS_HEALTH_EMAIL_FROM=health@superiorstate.net
   ```

8. [ ] Verify data directory exists and has correct ownership:
   ```bash
   ls -la /var/lib/tomcat10/data/
   # If missing:
   sudo mkdir -p /var/lib/tomcat10/data
   sudo chown tomcat:tomcat /var/lib/tomcat10/data
   ```

8.5. [ ] Verify branding directory exists and has correct ownership + systemd write permission:
   ```bash
   ls -la /var/lib/tomcat10/branding/
   # Should exist and be owned by tomcat:tomcat (baked into v5 master image)
   # If missing:
   sudo mkdir -p /var/lib/tomcat10/branding
   sudo chown tomcat:tomcat /var/lib/tomcat10/branding
   ```
   Verify systemd override allows writes (baked into v5 master image):
   ```bash
   systemctl cat tomcat10 | grep branding
   # Should show: ReadWritePaths=/var/lib/tomcat10/branding/
   # If missing:
   sudo mkdir -p /etc/systemd/system/tomcat10.service.d
   cat > /etc/systemd/system/tomcat10.service.d/override.conf << 'EOF'
   [Service]
   ReadWritePaths=/var/lib/tomcat10/branding/
   EOF
   sudo systemctl daemon-reload
   ```

9. [ ] Verify Wasabi credentials are configured in AWS CLI:
   ```bash
   aws s3 ls --profile wasabi --endpoint-url https://s3.us-east-1.wasabisys.com s3://ssa-backups/
   ```

---

## Phase 2.5: Deploy WAR (Your Side — SSH)

The master image does not include a WAR file — it is pulled from GitHub Releases.

10. [ ] Trigger the update script to pull the latest release:
    ```bash
    # Temporarily set PSP_ID if still UNINITIALIZED (update.sh skips UNINITIALIZED)
    # PSP_ID should already be set from Phase 2 step 7
    sudo /opt/ssa/scripts/update.sh
    ```
11. [ ] Verify the WAR was deployed:
    ```bash
    ls -la /var/lib/tomcat10/webapps/ROOT.war
    cat /opt/ssa/current_version.txt
    ```
    If `update.sh` reports no release found, deploy manually:
    ```bash
    # Download the latest release WAR from GitHub
    # Place it at /var/lib/tomcat10/webapps/ROOT.war
    sudo chown tomcat:tomcat /var/lib/tomcat10/webapps/ROOT.war
    ```
12. [ ] Start Tomcat:
    ```bash
    sudo systemctl start tomcat10
    ```
13. [ ] Verify Tomcat is running:
    ```bash
    sudo systemctl status tomcat10
    curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/
    # Should return 200
    ```
14. [ ] Check logs for clean startup:
    ```bash
    tail -50 /var/lib/tomcat10/logs/catalina.out
    # Look for: ✅ ssa.properties loaded from: ... (PSP_ID=<your_id>)
    # Look for: 🔁 Skipping global data load – database not initialized
    ```

---

## Phase 3: DNS & SSL (PSP's Side + Your Side)

15. [ ] Communicate VPS static IP address to PSP contact
16. [ ] PSP creates A record: `their.domain.com` → VPS IP
17. [ ] Wait for DNS propagation (verify with `dig their.domain.com` or `nslookup`)
18. [ ] Follow the detailed steps in `docs/tomcat_ssl_setup.md`:
    - Stop Tomcat
    - Run Certbot to generate certificate
    - Copy PEM files to Tomcat conf directory
    - Configure `server.xml` with HTTPS connector on port 443
    - Configure HTTP→HTTPS redirect
    - Grant Tomcat permission to bind privileged ports
    - Start Tomcat
    - Set up auto-renewal with deploy hooks
19. [ ] Verify HTTPS access: `https://their.domain.com` should show the uninitialized app
20. [ ] Verify HTTP redirect: `http://their.domain.com` should redirect to HTTPS

---

## Phase 4: Communicate Deployment Key (Your Side)

21. [ ] Send the deployment key to the PSP contact via a **separate secure channel**
    - Do NOT include in the same email as the domain/IP
    - Options: phone call, separate encrypted email, secure messaging
22. [ ] Provide PSP with initialization URL: `https://their.domain.com/initialize.jsp`

---

## Phase 5: Initialize Database (PSP's Side)

23. [ ] PSP navigates to `https://their.domain.com/initialize.jsp`
24. [ ] PSP fills out the initialization form:
    - Deployment Key
    - PSP Name, Contact Name, Email, Password
    - Address, Phone, Tax ID
    - Domain, SMTP settings, Summit path
25. [ ] PSP submits the form
26. [ ] Verify initialization succeeded:
    ```bash
    # SSH check:
    tail -20 /var/lib/tomcat10/logs/catalina.out
    # Look for: 🚀 InitializeDataBase — key validated, starting initialization...
    # Look for: ✅ InitializeDataBase — initialization complete
    ```

**What initialization creates:**
- PSP entity, primary person, user account with admin role
- Agency record, employer record, employee record
- Reference data: billing groups, plan types, template groups, user roles, activity statuses
- `EMAIL_FOOTER_TEXT` constant (PSP name for email branding)
- Initialization checklist (4 tasks for data import)

---

## Phase 6: Post-Initialization Verification (Your Side)

27. [ ] Restart Tomcat to trigger full global data load:
    ```bash
    sudo systemctl restart tomcat10
    ```
28. [ ] Check logs for successful startup:
    ```bash
    tail -20 /var/lib/tomcat10/logs/catalina.out
    # Look for: ✅ Global data loaded
    ```
29. [ ] Verify PSP can log in with the credentials from the initialization form
30. [ ] Verify the initialization checklist appears (4 tasks: Get Exports, Upload, Import, Update)
31. [ ] Verify re-initialization is blocked:
    - Navigate to `https://their.domain.com/initialize.jsp` — should not show the form
    - Even if accessed directly, POST to `/InitializeDataBase` returns redirect to login
32. [ ] Run a manual backup test:
    ```bash
    sudo /opt/ssa/scripts/backup.sh
    # Verify backup appears in Wasabi: ssa-backups/<PSP_ID>/db/
    ```
33. [ ] Run a manual update check:
    ```bash
    sudo /opt/ssa/scripts/update.sh
    # Should report "already up to date" if WAR was just deployed
    ```
34. [ ] Run a manual health check test:
    ```bash
    sudo /opt/ssa/scripts/healthcheck.sh 2>&1
    # Should print: ✅ Health check email sent to kevin@superiorstate.net
    # Check your email for the health report
    ```

---

## Phase 7: PSP Data Import (PSP's Side)

_Note: This phase will be simplified once the PSP admin dashboard is built (see D-14 in deployment backlog). Currently uses the initialization checklist workflow._

The PSP follows the initialization checklist in the application:

35. [ ] Get Summit Exports (PSP downloads from their Summit instance)
36. [ ] Upload Exports from Summit (via the upload page in AMS)
37. [ ] Import Data from Uploads (CSV import processing)
38. [ ] Update Working Tables from Imports (refresh working data)

---

## Phase 8: Confirm Operational (Your Side)

39. [ ] Verify cron jobs are active:
    ```bash
    sudo crontab -l
    # Should show:
    #   0 2 * * *  /opt/ssa/scripts/backup.sh       (2:00 AM UTC)
    #   30 2 * * * /opt/ssa/scripts/update.sh        (2:30 AM UTC)
    #   0 6 * * *  /opt/ssa/scripts/healthcheck.sh   (6:00 AM UTC)
    ```
40. [ ] Verify certbot auto-renewal timer:
    ```bash
    sudo systemctl list-timers | grep certbot
    ```
41. [ ] Next morning: verify automated backup ran successfully:
    ```bash
    ls -la /opt/ssa/backups/
    aws s3 ls --profile wasabi --endpoint-url https://s3.us-east-1.wasabisys.com s3://ssa-backups/<PSP_ID>/db/
    ```
42. [ ] Next morning: verify health check email arrived
    - Subject: `[SSA Health] <PSP_ID> — <timestamp>`
    - Services should show Tomcat ✅, MySQL ✅, DB Initialized ✅

---

## Rollback Procedure

### Quick Rollback (Re-initialize)

If initialization fails or data is corrupted before the PSP has imported real data:

1. Stop Tomcat: `sudo systemctl stop tomcat10`
2. Drop and recreate the database from the blank schema:
   ```bash
   mysql -u root -p -e "DROP DATABASE beta_ssa; CREATE DATABASE beta_ssa;"
   mysql -u root -p beta_ssa < /opt/ssa/schema/beta_ssa_blank.sql
   ```
3. Start Tomcat: `sudo systemctl start tomcat10`
4. Re-attempt initialization via `https://their.domain.com/initialize.jsp`

### Full Recovery (From Backup)

If a deployed PSP with real data needs recovery:

1. Stop Tomcat: `sudo systemctl stop tomcat10`
2. Download and restore the latest backup from Wasabi:
   ```bash
   aws s3 cp --profile wasabi --endpoint-url https://s3.us-east-1.wasabisys.com \
     s3://ssa-backups/<PSP_ID>/db/<latest_backup>.sql.gz /opt/ssa/backups/
   gunzip /opt/ssa/backups/<latest_backup>.sql.gz
   mysql -u root -p -e "DROP DATABASE beta_ssa; CREATE DATABASE beta_ssa;"
   mysql -u root -p beta_ssa < /opt/ssa/backups/<latest_backup>.sql
   ```
3. Start Tomcat: `sudo systemctl start tomcat10`

### WAR Rollback

If a code update causes issues:

1. Stop Tomcat: `sudo systemctl stop tomcat10`
2. Swap to previous WAR:
   ```bash
   cp /var/lib/tomcat10/webapps/ROOT.war /var/lib/tomcat10/webapps/ROOT.war.broken
   cp /opt/ssa/backups/ssa-previous.war /var/lib/tomcat10/webapps/ROOT.war
   ```
3. Start Tomcat: `sudo systemctl start tomcat10`
4. If the new WAR included DB migrations, also restore the DB from the pre-update backup

---

## Health Check Email Management

Each PSP VPS sends a daily health report to `kevin@superiorstate.net` at 6:00 AM UTC.

**SMTP config is stored in `ssa.properties`** (`SYS_HEALTH_*` keys), pre-configured on the master image. The health check shell script reads these values directly from the properties file.

**To change SMTP provider across all PSPs**, update `ssa.properties` on each VPS:
```properties
SYS_HEALTH_SMTP_SERVER=new.smtp.server
SYS_HEALTH_SMTP_USER=new_user
SYS_HEALTH_SMTP_PASSWORD=new_password
```

**To disable health emails for a single PSP**, edit its `ssa.properties`:
```properties
SYS_HEALTH_ENABLED=false
```

**To disable across all PSPs** (e.g., after master dashboard is built), update `ssa.properties` on each VPS or bake `SYS_HEALTH_ENABLED=false` into the next master snapshot.

---

## Master Image Maintenance

When the master image needs updating (new schema baseline, script changes, infrastructure updates):

1. SSH into the master VPS: `ssh root@208.94.39.77`
2. Make changes (update schema dump, scripts, properties, etc.)
3. Ensure `PSP_ID=UNINITIALIZED` in `ssa.properties`
4. Ensure webapps directory is empty (no WAR — clones pull via update script)
5. Shut down the VPS in IONOS
6. Take a new snapshot with naming convention: `SSA-Master-Base-v{N}-{YYYY-MM-DD}`
7. Update this runbook's Pre-Deployment Prerequisites with the new snapshot name
8. Update `docs/deployment_strategy.md` §2.2 with the new image version

### Current Master Image: `SSA-Master-Base-v5-2026-02-27`

**What's on the v5 image:**
- Ubuntu 24.x LTS, Java 17, Tomcat 10, MySQL 8, Certbot
- Blank schema at V024 (`/opt/ssa/schema/beta_ssa_blank.sql`)
- Full `ssa.properties` template with all keys (PSP_ID=UNINITIALIZED)
- Branding directory (`/var/lib/tomcat10/branding/`) with systemd write override
- Scripts: backup.sh, update.sh, healthcheck.sh (reads from ssa.properties)
- Cron jobs: backup 2:00 AM, update 2:30 AM, health check 6:00 AM UTC
- No WAR deployed (pulled via update.sh after cloning)

---

## Quick Reference

| Item | Location |
|------|----------|
| SSA properties | `/var/lib/tomcat10/conf/ssa.properties` |
| Tomcat config | `/var/lib/tomcat10/conf/server.xml` |
| Tomcat logs | `/var/lib/tomcat10/logs/catalina.out` |
| EMF error log | `/var/lib/tomcat10/logs/emf_error.log` |
| SSL certificates | `/var/lib/tomcat10/conf/*.pem` |
| Certbot live certs | `/etc/letsencrypt/live/<domain>/` |
| Application data | `/var/lib/tomcat10/data/` |
| Branding files | `/var/lib/tomcat10/branding/` |
| WAR file | `/var/lib/tomcat10/webapps/ROOT.war` |
| Blank schema | `/opt/ssa/schema/beta_ssa_blank.sql` |
| Backup script | `/opt/ssa/scripts/backup.sh` |
| Update script | `/opt/ssa/scripts/update.sh` |
| Health check script | `/opt/ssa/scripts/healthcheck.sh` |
| SSL renewal script | `/opt/ssa/scripts/renew-ssl.sh` |
| Local backups | `/opt/ssa/backups/` |
| Update log | `/opt/ssa/logs/update.log` |
| Health check log | `/opt/ssa/logs/healthcheck.log` |
| SSL renewal log | `/opt/ssa/logs/ssl-renewal.log` |
| Current version | `/opt/ssa/current_version.txt` |
| Wasabi bucket | `ssa-backups/<PSP_ID>/db/` |
| Master snapshot | `SSA-Master-Base-v5-2026-02-27` |
| Master VPS SSH | `ssh root@208.94.39.77` |

---

## Related Documents

| Document | Purpose |
|----------|---------|
| `docs/deployment_strategy.md` | Full architecture and design decisions |
| `docs/deployment_backlog.md` | Tracked work items and completion status |
| `docs/tomcat_ssl_setup.md` | Detailed SSL/HTTPS setup instructions |
| `docs/analysis/migration_tracker.md` | Database migration version tracking |
| `docs/schema_version_migration.sql` | Schema version table + retroactive inserts |
