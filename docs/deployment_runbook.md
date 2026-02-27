# PSP Deployment Runbook

**Last Updated:** February 23, 2026 (Session 2)
**Reference:** `docs/deployment_strategy.md` for full architecture context
**SSL Reference:** `docs/tomcat_ssl_setup.md` for detailed SSL instructions

---

## Pre-Deployment Prerequisites

Before deploying any PSP, ensure:

- [ ] Master VPS snapshot is current (`SSA-Master-Base-v4-2026-02-23` or newer)
- [ ] Latest release published to GitHub Releases (WAR + any migration SQL)
- [ ] `schema_version` table is up to date on the master image
- [ ] Blank schema dump exists at `/opt/ssa/schema/beta_ssa_blank.sql` on master image
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
7. [ ] Edit `/var/lib/tomcat10/conf/ssa.properties`:
   ```properties
   PSP_ID=<short_identifier>          # e.g., acme_benefits (used for backups, logs, health emails)
   DEPLOYMENT_KEY=<unique_strong_key> # e.g., UUID or random passphrase
   SAVE_PATH=/var/lib/tomcat10/data/
   LOG_PATH=/var/lib/tomcat10/logs/
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
   # If missing:
   sudo mkdir -p /var/lib/tomcat10/branding
   sudo chown tomcat:tomcat /var/lib/tomcat10/branding
   ```
Verify systemd override allows writes (should be baked into master image):
   ```bash
   systemctl cat tomcat10 | grep branding
   # Should show: ReadWritePaths=/var/lib/tomcat10/branding/
   # If missing:
   sudo systemctl edit tomcat10
   # Add:
   # [Service]
   # ReadWritePaths=/var/lib/tomcat10/branding/
   # Then: sudo systemctl daemon-reload
   ```

9. [ ] Verify Wasabi credentials are configured in AWS CLI:
   ```bash
   aws s3 ls --profile wasabi --endpoint-url https://s3.us-east-1.wasabisys.com s3://ssa-backups/
   ```
10. [ ] Start Tomcat:
    ```bash
    sudo systemctl start tomcat10
    ```
11. [ ] Verify Tomcat is running:
    ```bash
    sudo systemctl status tomcat10
    curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/
    # Should return 200
    ```
12. [ ] Check logs for clean startup:
    ```bash
    tail -50 /var/lib/tomcat10/logs/catalina.out
    # Look for: ✅ ssa.properties loaded from: ... (PSP_ID=<your_id>)
    # Look for: 🔁 Skipping global data load – database not initialized
    ```

---

## Phase 3: DNS & SSL (PSP's Side + Your Side)

13. [ ] Communicate VPS static IP address to PSP contact
14. [ ] PSP creates A record: `their.domain.com` → VPS IP
15. [ ] Wait for DNS propagation (verify with `dig their.domain.com` or `nslookup`)
16. [ ] Follow the detailed steps in `docs/tomcat_ssl_setup.md`:
    - Stop Tomcat
    - Run Certbot to generate certificate
    - Copy PEM files to Tomcat conf directory
    - Configure `server.xml` with HTTPS connector on port 443
    - Configure HTTP→HTTPS redirect
    - Grant Tomcat permission to bind privileged ports
    - Start Tomcat
    - Set up auto-renewal with deploy hooks
17. [ ] Verify HTTPS access: `https://their.domain.com` should show the uninitialized app
18. [ ] Verify HTTP redirect: `http://their.domain.com` should redirect to HTTPS

---

## Phase 4: Communicate Deployment Key (Your Side)

19. [ ] Send the deployment key to the PSP contact via a **separate secure channel**
    - Do NOT include in the same email as the domain/IP
    - Options: phone call, separate encrypted email, secure messaging
20. [ ] Provide PSP with initialization URL: `https://their.domain.com/initialize.jsp`

---

## Phase 5: Initialize Database (PSP's Side)

21. [ ] PSP navigates to `https://their.domain.com/initialize.jsp`
22. [ ] PSP fills out the initialization form:
    - Deployment Key
    - PSP Name, Contact Name, Email, Password
    - Address, Phone, Tax ID
    - Domain, SMTP settings, Summit path
23. [ ] PSP submits the form
24. [ ] Verify initialization succeeded:
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
- Health check email constants (`SYS_HEALTH_*`) for daily status reports
- Initialization checklist (4 tasks for data import)

---

## Phase 6: Post-Initialization Verification (Your Side)

25. [ ] Restart Tomcat to trigger full global data load:
    ```bash
    sudo systemctl restart tomcat10
    ```
26. [ ] Check logs for successful startup:
    ```bash
    tail -20 /var/lib/tomcat10/logs/catalina.out
    # Look for: ✅ Global data loaded
    ```
27. [ ] Verify PSP can log in with the credentials from the initialization form
28. [ ] Verify the initialization checklist appears (4 tasks: Get Exports, Upload, Import, Update)
29. [ ] Verify re-initialization is blocked:
    - Navigate to `https://their.domain.com/initialize.jsp` — should not show the form
    - Even if accessed directly, POST to `/InitializeDataBase` returns redirect to login
30. [ ] Run a manual backup test:
    ```bash
    sudo /opt/ssa/scripts/backup.sh
    # Verify backup appears in Wasabi: ssa-backups/<PSP_ID>/db/
    ```
31. [ ] Run a manual update check:
    ```bash
    sudo /opt/ssa/scripts/update.sh
    # Should report "already up to date" if master image had latest release
    ```
32. [ ] Run a manual health check test:
    ```bash
    sudo /opt/ssa/scripts/healthcheck.sh 2>&1
    # Should print: ✅ Health check email sent to kevin@superiorstate.net (via DB)
    # Check your email for the health report
    ```

---

## Phase 7: PSP Data Import (PSP's Side)

_Note: This phase will be simplified once the PSP admin dashboard is built (see D-14 in deployment backlog). Currently uses the initialization checklist workflow._

The PSP follows the initialization checklist in the application:

33. [ ] Get Summit Exports (PSP downloads from their Summit instance)
34. [ ] Upload Exports from Summit (via the upload page in AMS)
35. [ ] Import Data from Uploads (CSV import processing)
36. [ ] Update Working Tables from Imports (refresh working data)

---

## Phase 8: Confirm Operational (Your Side)

37. [ ] Verify cron jobs are active:
    ```bash
    sudo crontab -l
    # Should show:
    #   0 2 * * *  /opt/ssa/scripts/backup.sh       (2:00 AM UTC)
    #   30 2 * * * /opt/ssa/scripts/update.sh        (2:30 AM UTC)
    #   0 6 * * *  /opt/ssa/scripts/healthcheck.sh   (6:00 AM UTC)
    ```
38. [ ] Verify certbot auto-renewal timer:
    ```bash
    sudo systemctl list-timers | grep certbot
    ```
39. [ ] Next morning: verify automated backup ran successfully:
    ```bash
    ls -la /opt/ssa/backups/
    aws s3 ls --profile wasabi --endpoint-url https://s3.us-east-1.wasabisys.com s3://ssa-backups/<PSP_ID>/db/
    ```
40. [ ] Next morning: verify health check email arrived
    - Subject: `[SSA Health] <PSP_ID> — <timestamp>`
    - Services should show Tomcat ✅, MySQL ✅, DB Initialized ✅
    - SMTP Source should show "DB"

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

**SMTP config is stored in the database** (`SYS_HEALTH_*` constants), updatable via SQL migrations to all deployments simultaneously. Hardcoded fallback values in the shell script are used only if the DB is unreachable.

**To change SMTP provider across all PSPs**, push a migration:
```sql
UPDATE constant SET value = 'new.smtp.server' WHERE name = 'SYS_HEALTH_SMTP_SERVER';
UPDATE constant SET value = 'new_user' WHERE name = 'SYS_HEALTH_SMTP_USER';
UPDATE constant SET value = 'new_password' WHERE name = 'SYS_HEALTH_SMTP_PASSWORD';
```

**To disable health emails** (e.g., after master dashboard is built):
```sql
UPDATE constant SET value = 'false' WHERE name = 'SYS_HEALTH_ENABLED';
```

**To disable for a single PSP**, run the same UPDATE on that PSP's database only.

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
| Master snapshot | `SSA-Master-Base-v4-2026-02-23` |

---

## Related Documents

| Document | Purpose |
|----------|---------|
| `docs/deployment_strategy.md` | Full architecture and design decisions |
| `docs/deployment_backlog.md` | Tracked work items and completion status |
| `docs/tomcat_ssl_setup.md` | Detailed SSL/HTTPS setup instructions |
| `docs/analysis/migration_tracker.md` | Database migration version tracking |
| `docs/schema_version_migration.sql` | Schema version table + retroactive inserts |
