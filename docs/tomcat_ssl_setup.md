# Tomcat 10 SSL Setup with Let's Encrypt

**Last Updated:** February 23, 2026
**Referenced from:** `docs/deployment_runbook.md` Phase 3

---

## Prerequisites

- VPS running Ubuntu 24 with Tomcat 10 installed
- Domain name with A record pointing to the VPS IP address
- DNS propagation complete (verify with `dig yourdomain.com` or `nslookup yourdomain.com`)
- Port 80 open (Certbot uses it for domain verification)
- Port 443 open (HTTPS traffic)

---

## Step 1: Install Certbot

Certbot should already be on the master image. If not:

```bash
sudo apt update
sudo apt install certbot -y
```

---

## Step 2: Stop Tomcat

Certbot standalone mode needs port 80 free. Stop Tomcat first:

```bash
sudo systemctl stop tomcat10
```

---

## Step 3: Generate Certificate

Replace `yourdomain.com` with the PSP's actual domain:

```bash
sudo certbot certonly --standalone -d yourdomain.com
```

Certbot will:
- Verify you own the domain (via port 80)
- Generate certificate files
- Store them in `/etc/letsencrypt/live/yourdomain.com/`

Verify the files exist:

```bash
sudo ls /etc/letsencrypt/live/yourdomain.com/
# Should show: cert.pem  chain.pem  fullchain.pem  privkey.pem  README
```

---

## Step 4: Copy Certificates to Tomcat

```bash
sudo cp /etc/letsencrypt/live/yourdomain.com/cert.pem /var/lib/tomcat10/conf/
sudo cp /etc/letsencrypt/live/yourdomain.com/chain.pem /var/lib/tomcat10/conf/
sudo cp /etc/letsencrypt/live/yourdomain.com/privkey.pem /var/lib/tomcat10/conf/
sudo chown tomcat:tomcat /var/lib/tomcat10/conf/*.pem
```

---

## Step 5: Configure Tomcat server.xml

Edit the Tomcat configuration:

```bash
sudo nano /var/lib/tomcat10/conf/server.xml
```

### 5a: Add HTTPS Connector

Find the commented-out SSL connector block (search for `8443`). Replace the entire commented block with:

```xml
<Connector port="443"
           protocol="org.apache.coyote.http11.Http11NioProtocol"
           maxThreads="150"
           SSLEnabled="true">
   <SSLHostConfig>
      <Certificate certificateFile="conf/cert.pem"
                   certificateKeyFile="conf/privkey.pem"
                   certificateChainFile="conf/chain.pem" />
   </SSLHostConfig>
</Connector>
```

**Note:** We use port 443 directly instead of 8443 so users don't need to type a port number.

### 5b: Redirect HTTP to HTTPS

Find the existing HTTP connector (port 8080) and change it to redirect:

```xml
<Connector port="80"
           protocol="HTTP/1.1"
           connectionTimeout="20000"
           redirectPort="443" />
```

### 5c: Add Redirect Valve

Inside the `<Host>` block (look for `<Host name="localhost" ...>`), add this valve just before the closing `</Host>` tag:

```xml
<Valve className="org.apache.catalina.valves.rewrite.RewriteValve" />
```

Then create the rewrite config:

```bash
sudo mkdir -p /var/lib/tomcat10/webapps/ROOT/WEB-INF
sudo nano /var/lib/tomcat10/webapps/ROOT/WEB-INF/rewrite.config
```

Add this content:

```
RewriteCond %{HTTPS} !on
RewriteRule ^(.*)$ https://%{HTTP_HOST}%{REQUEST_URI} [L,R=301]
```

### 5d: Allow Tomcat to Bind to Privileged Ports (80, 443)

By default, non-root processes can't bind to ports below 1024. Grant Tomcat permission:

```bash
sudo setcap cap_net_bind_service+ep $(readlink -f /usr/lib/jvm/java-17-openjdk-amd64/bin/java)
```

**Note:** If your Java path differs, find it with: `sudo readlink -f $(which java)`

---

## Step 6: Start Tomcat

```bash
sudo systemctl start tomcat10
```

Verify it's running:

```bash
sudo systemctl status tomcat10
```

Check the logs if there are issues:

```bash
tail -50 /var/lib/tomcat10/logs/catalina.out
```

---

## Step 7: Test

1. Open a browser and go to `https://yourdomain.com`
   - Should show the application with a valid SSL padlock
2. Go to `http://yourdomain.com`
   - Should redirect to HTTPS automatically
3. Verify certificate details by clicking the padlock icon in the browser

---

## Step 8: Set Up Auto-Renewal

Let's Encrypt certificates expire after 90 days. Set up automatic renewal.

### 8a: Create the Renewal Script

```bash
sudo nano /opt/ssa/scripts/renew-ssl.sh
```

Paste this content (replace `yourdomain.com`):

```bash
#!/bin/bash
# SSA SSL Certificate Renewal Script
# Called by certbot deploy hook after successful renewal

DOMAIN="yourdomain.com"
TOMCAT_CONF="/var/lib/tomcat10/conf"

# Copy renewed certs to Tomcat
cp /etc/letsencrypt/live/$DOMAIN/cert.pem $TOMCAT_CONF/
cp /etc/letsencrypt/live/$DOMAIN/chain.pem $TOMCAT_CONF/
cp /etc/letsencrypt/live/$DOMAIN/privkey.pem $TOMCAT_CONF/
chown tomcat:tomcat $TOMCAT_CONF/*.pem

# Restart Tomcat to pick up new certs
systemctl restart tomcat10

echo "$(date) — SSL renewed and Tomcat restarted for $DOMAIN" >> /opt/ssa/logs/ssl-renewal.log
```

Make it executable:

```bash
sudo chmod +x /opt/ssa/scripts/renew-ssl.sh
```

### 8b: Register the Deploy Hook with Certbot

```bash
sudo certbot certonly --standalone -d yourdomain.com \
    --deploy-hook "/opt/ssa/scripts/renew-ssl.sh" \
    --pre-hook "systemctl stop tomcat10" \
    --post-hook "systemctl start tomcat10" \
    --force-renewal
```

This does three things:
- `--pre-hook`: Stops Tomcat to free port 80 for Certbot
- Renews the certificate
- `--deploy-hook`: Copies new certs and restarts Tomcat (only runs on successful renewal)
- `--post-hook`: Starts Tomcat back up (runs even if renewal is skipped)

These hooks are saved in `/etc/letsencrypt/renewal/yourdomain.com.conf` and will be used automatically on future renewals.

### 8c: Verify Auto-Renewal is Configured

Certbot on Ubuntu installs a systemd timer that runs twice daily. Verify it:

```bash
sudo systemctl list-timers | grep certbot
```

You should see `snap.certbot.renew.timer` or similar. If not, add a cron job:

```bash
sudo crontab -e
# Add:
0 3,15 * * * certbot renew --quiet
```

### 8d: Test Renewal (Dry Run)

```bash
sudo certbot renew --dry-run
```

This simulates renewal without actually changing anything. Should report success.

---

## Troubleshooting

### "Permission denied" binding to port 443
Re-run the setcap command from Step 5d. This may need to be re-applied after Java updates.

### "Address already in use" on port 80 or 443
Check what's using the port: `sudo ss -tlnp | grep :443`
Stop the conflicting process before starting Tomcat.

### Certificate not trusted in browser
Verify `chain.pem` is included in the connector config. Without the chain file, some browsers won't trust the certificate.

### Certbot fails domain verification
- Verify DNS A record points to this VPS IP: `dig yourdomain.com`
- Verify port 80 is open: `sudo ufw status` or check IONOS firewall rules
- Make sure nothing else is using port 80 when running Certbot standalone

### Tomcat won't start after SSL config
Check `catalina.out` for specific errors. Common issues:
- PEM file permissions (must be readable by tomcat user)
- Typo in `server.xml` (XML is strict about syntax)
- Port conflict (another process on 443)

---

## Quick Reference

| Item | Location |
|------|----------|
| Certbot certificates | `/etc/letsencrypt/live/yourdomain.com/` |
| Tomcat cert copies | `/var/lib/tomcat10/conf/*.pem` |
| Tomcat config | `/var/lib/tomcat10/conf/server.xml` |
| Renewal script | `/opt/ssa/scripts/renew-ssl.sh` |
| Renewal log | `/opt/ssa/logs/ssl-renewal.log` |
| Certbot renewal config | `/etc/letsencrypt/renewal/yourdomain.com.conf` |

# tomcat_ssl_setup.md — Append This Section

Add this section after the existing "Troubleshooting" section in `docs/tomcat_ssl_setup.md`:

---

## Known Issues (Discovered March 2026)

### APR/OpenSSL Native Library Conflict

**Symptom:** Tomcat fails to start SSL with errors like `cannot create new ssl` or `OpenSSL library mismatch`.

**Cause:** The `AprLifecycleListener` in `server.xml` tries to load the native APR/OpenSSL library. On some Ubuntu 24 installs, the system OpenSSL version is incompatible with the APR native library bundled or expected by Tomcat.

**Fix:** Comment out or remove the `AprLifecycleListener` in `server.xml`:

```xml
<!-- Commented out: causes SSL failures on Ubuntu 24 with APR/OpenSSL mismatch
<Listener className="org.apache.catalina.core.AprLifecycleListener" SSLEngine="on" />
-->
```

This forces Tomcat to use the pure Java NIO SSL implementation (configured via the `Http11NioProtocol` connector in Step 5a), which is fully functional and does not depend on native libraries.

**Impact:** None in practice. The NIO SSL connector handles all HTTPS traffic correctly. APR can provide marginal performance improvements at very high connection counts, but this is not relevant for AMS deployments.

### Private Key File Permissions

**Symptom:** Tomcat starts but SSL fails with `PEMFile` permission denied errors referencing `privkey.pem`.

**Cause:** Let's Encrypt stores private keys in `/etc/letsencrypt/archive/` with restrictive permissions (readable only by root). The symlinks in `/etc/letsencrypt/live/` don't help if the underlying archive directory blocks access. Tomcat (running as the `tomcat` user) cannot read the key.

**Fix:** Open permissions on the Let's Encrypt directories and key file:

```bash
sudo chmod 755 /etc/letsencrypt/live
sudo chmod 755 /etc/letsencrypt/archive
sudo chmod 644 /etc/letsencrypt/archive/yourdomain.com/privkey1.pem
```

**Note:** This fix must be re-applied after certificate renewals, as Certbot may create new key files with default restrictive permissions. Consider adding these `chmod` commands to the renewal script (`/opt/ssa/scripts/renew-ssl.sh`).

**Alternative:** Instead of loosening permissions, copy the certs to Tomcat's conf directory (as described in Step 4) and ensure the copies are owned by the `tomcat` user. The renewal script already handles this.