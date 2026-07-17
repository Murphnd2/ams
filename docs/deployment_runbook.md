# PSP Deployment Runbook

**Last Updated:** March 2, 2026
**Reference:** `docs/deployment_strategy.md` for full architecture context
**SSL Reference:** `docs/tomcat_ssl_setup.md` for nginx + Let's Encrypt SSL setup

---

## Pre-Deployment Prerequisites

Before deploying any PSP, ensure:

- [ ] Master VM snapshot is current (`SSA-Master-Base-v9-2026-03-20` or newer)
- [ ] Latest release published to GitHub Releases (WAR + any migration SQL)
- [ ] `schema_version` table is up to date on the master image (currently V057)
- [ ] Health check script installed at `/opt/ssa/scripts/healthcheck.sh` on master image
- [ ] PSP has provided: company name, contact info, email, domain name, SMTP credentials, Summit path, tax ID

---

## Phase 1: Provision VM (Your Side — IONOS DCD)

1. [ ] Clone master snapshot in IONOS DCD (SSA-PSP VDC, US-Las Vegas)
2. [ ] Assign a name to the new VM (e.g., `SSA-AcmeBenefits`)
3. [ ] **Reserve a static IP block** in DCD (Menu → Network Services → IP Management → + Reserve IP) and assign to this VM's NIC
   - **Note:** If you receive "error occurred while reserving ip block", this may be an account-level limit. Contact IONOS support or use the DHCP-assigned IP for demo/test machines (stable unless VM is deallocated).
4. [ ] Start the VM
5. [ ] Note the static IP address: `_______________`

---

## Phase 2: Configure VM (Your Side — SSH)

6. [ ] SSH into the new VM
7. [ ] Edit `/var/lib/tomcat10/conf/ssa.properties` — set PSP-specific values:
   ```properties
   PSP_ID=<short_identifier>          # e.g., acme_benefits (used for backups, logs, health emails)
   SYSTEM_URL=https://<domain>        # e.g., https://acme.superiorstate.biz (used by VendorManager)
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
   DB_PASSWORD=<set per docs/runbooks/ams_app_credential_rotation.md>
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
   # Should exist and be owned by tomcat:tomcat (baked into master image)
   # If missing:
   sudo mkdir -p /var/lib/tomcat10/branding
   sudo chown tomcat:tomcat /var/lib/tomcat10/branding
   ```
   Verify systemd override allows writes (baked into master image):
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

SSL is handled by nginx (reverse proxy), not Tomcat directly. See `docs/tomcat_ssl_setup.md` for full details.

15. [ ] Communicate VM static IP address to PSP contact
16. [ ] PSP creates A record: `their.domain.com` → VM IP
17. [ ] Wait for DNS propagation (verify with `dig their.domain.com` or `nslookup`)
18. [ ] Create nginx site config:
    ```bash
    sudo cp /etc/nginx/sites-available/template.conf /etc/nginx/sites-available/theirdomain.conf
    # Edit to replace yourdomain.com with their.domain.com
    sudo ln -s /etc/nginx/sites-available/theirdomain.conf /etc/nginx/sites-enabled/
    ```
19. [ ] Generate SSL certificate (uses nginx plugin — no downtime):
    ```bash
    sudo certbot certonly --nginx -d their.domain.com -d www.their.domain.com
    ```
20. [ ] Reload nginx to pick up the new cert:
    ```bash
    sudo systemctl reload nginx
    ```
21. [ ] Start Tomcat if not already running:
    ```bash
    sudo systemctl start tomcat10
    ```
22. [ ] Verify HTTPS access: `https://their.domain.com/`

---

## Phase 4: Initialize Application (PSP's Side)

23. [ ] PSP navigates to `https://their.domain.com/initialize.jsp`
24. [ ] PSP fills out initialization form (company info, contact, address, SMTP, tax ID)
25. [ ] PSP enters deployment key (communicated separately from IP)
26. [ ] PSP submits form → database seeded, application ready
27. [ ] Verify: PSP can log in with the credentials they entered during initialization

---

## Phase 5: Post-Initialization (PSP's Side)

28. [ ] PSP follows the initialization checklist displayed on their dashboard
29. [ ] PSP uploads Summit exports (CSV files)
30. [ ] PSP runs import process
31. [ ] PSP verifies data in the application

---

## Phase 6: Verify Automated Systems (Your Side)

32. [ ] Wait for 2:00 AM UTC — verify backup script ran:
    ```bash
    ls -la /opt/ssa/backups/
    aws s3 ls --profile wasabi --endpoint-url https://s3.us-east-1.wasabisys.com s3://ssa-backups/<PSP_ID>/db/
    ```
33. [ ] Wait for 2:30 AM UTC — verify update script ran (should be no-op if already current):
    ```bash
    tail -20 /opt/ssa/logs/update.log
    ```
34. [ ] Next morning: verify health check email arrived
    - Subject: `[SSA Health] <PSP_ID> — <timestamp>`
    - Services should show Tomcat ✅, MySQL ✅, DB Initialized ✅

---

## Rollback Procedure

### Quick Rollback (Re-initialize)

If initialization fails or data is corrupted before the PSP has imported real data:

1. Stop Tomcat: `sudo systemctl stop tomcat10`
2. Drop and recreate the database from the blank schema:
   ```bash
   LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql --socket=/var/run/mysqld/mysqld.sock -u root -p -e "DROP DATABASE beta_ssa; CREATE DATABASE beta_ssa;"
   LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql --socket=/var/run/mysqld/mysqld.sock -u root -p beta_ssa < /opt/ssa/schema/beta_ssa_blank.sql
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
   LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql --socket=/var/run/mysqld/mysqld.sock -u root -p -e "DROP DATABASE beta_ssa; CREATE DATABASE beta_ssa;"
   LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql --socket=/var/run/mysqld/mysqld.sock -u root -p beta_ssa < /opt/ssa/backups/<latest_backup>.sql
   ```
3. Start Tomcat: `sudo systemctl start tomcat10`

### WAR Rollback

If a code update causes issues:

1. Stop Tomcat: `sudo systemctl stop tomcat10`
2. Swap to previous WAR:
   ```bash
   cp /var/lib/tomcat10/webapps/ROOT.war /var/lib/tomcat10/webapps/ROOT.war.broken
   cp /opt/ssa/backups/ROOT-previous.war /var/lib/tomcat10/webapps/ROOT.war
   ```
3. Start Tomcat: `sudo systemctl start tomcat10`
4. If the new WAR included DB migrations, also restore the DB from the pre-update backup

---

## Health Check Email Management

Each PSP VM sends a daily health report to `kevin@superiorstate.net` at 6:00 AM UTC.

**SMTP config is stored in `ssa.properties`** (`SYS_HEALTH_*` keys), pre-configured on the master image. The health check shell script reads these values directly from the properties file.

**To change SMTP provider across all PSPs**, update `ssa.properties` on each VM:
```properties
SYS_HEALTH_SMTP_SERVER=new.smtp.server
SYS_HEALTH_SMTP_USER=new_user
SYS_HEALTH_SMTP_PASSWORD=new_password
```

**To disable health emails for a single PSP**, edit its `ssa.properties`:
```properties
SYS_HEALTH_ENABLED=false
```

**To disable across all PSPs** (e.g., after master dashboard is built), update `ssa.properties` on each VM or bake `SYS_HEALTH_ENABLED=false` into the next master snapshot.

---

## Master Image Maintenance

When the master image needs updating (new schema baseline, script changes, infrastructure updates):

1. SSH into the master VM: `ssh root@208.94.39.77` (or `ssh root@master.superiorstate.biz`)
2. Make changes (update schema dump, scripts, properties, etc.)
3. Ensure `PSP_ID=UNINITIALIZED` in `ssa.properties`
4. Ensure `SYSTEM_URL=` is blank in `ssa.properties`
5. Ensure webapps directory is empty (no WAR — clones pull via update script)
6. Clean logs: `rm -f /var/lib/tomcat10/logs/*.log /var/lib/tomcat10/logs/*.txt`
7. Stop Tomcat: `sudo systemctl stop tomcat10`
8. Shut down the VM in IONOS DCD
9. Take a new snapshot with naming convention: `SSA-Master-Base-v{N}-{YYYY-MM-DD}`
10. Update this runbook's Pre-Deployment Prerequisites with the new snapshot name
11. Update `docs/deployment_strategy.md` §2.2 with the new image version

### Current Master Image: `SSA-Master-Base-v9-2026-03-20`

**What's on the v9 image:**
- Ubuntu 24.x LTS, Java 17, Tomcat 10, MySQL 8, Nginx, Certbot + python3-certbot-nginx
- MySQL configured with `lower_case_table_names = 1` (required — EclipseLink generates uppercase table names, Linux MySQL defaults to case-sensitive)
- `ams_app` MySQL user created with password matching `context.xml`
- Schema at V057 with `schema_version` table populated (57 versions tracked)
- Full `ssa.properties` template with all keys including `SYSTEM_URL=` (PSP_ID=UNINITIALIZED)
- Chatbot/Anthropic keys removed from ssa.properties (now self-service via UI — D-58)
- Branding directory (`/var/lib/tomcat10/branding/`) with systemd write override
- Data directory (`/var/lib/tomcat10/data/`) with systemd ReadWritePaths override (D-66)
- Scripts: backup.sh, update.sh, healthcheck.sh (reads from ssa.properties)
- `update.sh` synced from repo (version-sort fix, INSERT IGNORE fix)
- Cron jobs: backup 2:00 AM, update 2:30 AM, health check 6:00 AM UTC
- Hostname: `ssa-master`
- No WAR deployed (pulled via update.sh after cloning)

### MySQL Notes for All VMs

All MySQL commands on IONOS VMs require the Acronis library workaround:
```bash
LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql --socket=/var/run/mysqld/mysqld.sock -u root -p
```

**Password operations** (CREATE USER, ALTER USER) must be done in the **interactive MySQL shell**, not via the `-e` flag. Bash interprets special characters (`!`, `$`, `\`, `` ` ``) in passwords. Always enter the interactive shell and paste the password directly.

**Rotating the `ams_app` password:** the user exists as TWO account rows (`'ams_app'@'localhost'` used by
socket CLI, `'ams_app'@'127.0.0.1'` used by the Tomcat datasource over TCP) — **both must be rotated together**
or Tomcat fails auth while manual socket tests still pass. The password must also match in
`/etc/tomcat10/context.xml` and `/etc/tomcat10/ssa.properties`. Using an **alphanumeric-only** password lets
you use the `-e` flag safely (it also avoids breaking the XML attribute and properties file, not just the
shell). Full step-by-step procedure, character rules, and verification: `docs/runbooks/ams_app_credential_rotation.md`.

---

## Quick Reference

| Item | Location |
|------|----------|
| SSA properties | `/var/lib/tomcat10/conf/ssa.properties` |
| Tomcat config | `/var/lib/tomcat10/conf/server.xml` |
| Tomcat logs | `/var/lib/tomcat10/logs/catalina.out` |
| EMF error log | `/var/lib/tomcat10/logs/emf_error.log` |
| Nginx site configs | `/etc/nginx/sites-enabled/` |
| Certbot live certs | `/etc/letsencrypt/live/<domain>/` |
| Application data | `/var/lib/tomcat10/data/` |
| Branding files | `/var/lib/tomcat10/branding/` |
| WAR file | `/var/lib/tomcat10/webapps/ROOT.war` |
| Backup script | `/opt/ssa/scripts/backup.sh` |
| Update script | `/opt/ssa/scripts/update.sh` |
| Health check script | `/opt/ssa/scripts/healthcheck.sh` |
| Local backups | `/opt/ssa/backups/` |
| Update log | `/opt/ssa/logs/update.log` |
| Health check log | `/opt/ssa/logs/healthcheck.log` |
| Nginx logs | `/var/log/nginx/` |
| Current version | `/opt/ssa/current_version.txt` |
| Wasabi bucket | `ssa-backups/<PSP_ID>/db/` |
| Master snapshot | `SSA-Master-Base-v9-2026-03-20` |
| Master VM SSH | `ssh root@master.superiorstate.biz` (208.94.39.77) |

---

## Related Documents

| Document | Purpose |
|----------|---------|
| `docs/deployment_strategy.md` | Full architecture and design decisions |
| `docs/deployment_backlog.md` | Tracked work items and completion status |
| `docs/tomcat_ssl_setup.md` | Nginx + Let's Encrypt SSL setup |
| `docs/analysis/migration_tracker.md` | Database migration version tracking |
| `docs/schema_version_migration.sql` | Schema version table + retroactive inserts |
