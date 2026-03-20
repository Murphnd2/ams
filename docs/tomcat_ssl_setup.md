# SSL Setup with Nginx + Let's Encrypt

**Last Updated:** March 20, 2026
**Referenced from:** `docs/deployment_runbook.md` Phase 3

---

## Architecture

Nginx serves as the SSL termination point, reverse-proxying to Tomcat on port 8080. Tomcat itself does not handle HTTPS — it receives plain HTTP from nginx on localhost.

```
Client → :443 (nginx, SSL) → :8080 (Tomcat, HTTP)
Client → :80  (nginx) → 301 redirect to HTTPS
```

---

## Prerequisites

- VPS running Ubuntu 24 with Tomcat 10 and Nginx installed
- Domain name with A record pointing to the VPS IP address
- DNS propagation complete (verify with `dig yourdomain.com` or `nslookup yourdomain.com`)
- Ports 80 and 443 open

---

## Step 1: Install Certbot with Nginx Plugin

Certbot and the nginx plugin should already be on the master image. If not:

```bash
sudo apt update
sudo apt install certbot python3-certbot-nginx -y
```

---

## Step 2: Create Nginx Site Configuration

Create a config file for the domain:

```bash
sudo nano /etc/nginx/sites-available/yourdomain.conf
```

Paste this content (replace `yourdomain.com` with the actual domain):

```nginx
# 80: redirect to HTTPS
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    return 301 https://$host$request_uri;
}

# 443: terminate TLS and reverse-proxy to Tomcat
server {
    listen 443 ssl http2;
    server_name yourdomain.com www.yourdomain.com;

    ssl_certificate     /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    client_max_body_size 200M;

    location / {
        proxy_pass http://127.0.0.1:8080/;
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_read_timeout 300;
    }
}
```

Enable the site:

```bash
sudo ln -s /etc/nginx/sites-available/yourdomain.conf /etc/nginx/sites-enabled/
```

**Note:** Don't start nginx yet — the SSL cert doesn't exist until Step 3.

---

## Step 3: Generate Certificate

```bash
sudo certbot certonly --nginx -d yourdomain.com -d www.yourdomain.com
```

Certbot will:
- Verify domain ownership via the running nginx instance
- Generate certificate files in `/etc/letsencrypt/live/yourdomain.com/`
- Configure automatic renewal using the nginx authenticator (no downtime required)

If nginx is not yet running (first-time setup), use standalone mode temporarily:

```bash
sudo certbot certonly --standalone -d yourdomain.com -d www.yourdomain.com
```

Then switch to the nginx authenticator for future renewals:

```bash
sudo certbot certonly --nginx -d yourdomain.com -d www.yourdomain.com --force-renewal
```

---

## Step 4: Start Services

```bash
sudo systemctl start nginx
sudo systemctl start tomcat10
```

Verify:

```bash
sudo systemctl status nginx
sudo systemctl status tomcat10
```

---

## Step 5: Test

1. Open a browser and go to `https://yourdomain.com`
   - Should show the application with a valid SSL padlock
2. Go to `http://yourdomain.com`
   - Should redirect to HTTPS automatically
3. Verify certificate details by clicking the padlock icon in the browser

---

## Auto-Renewal

Let's Encrypt certificates expire after 90 days. Certbot installs a systemd timer that runs twice daily and renews any certificate within 30 days of expiry.

Verify the timer is active:

```bash
sudo systemctl list-timers | grep certbot
```

Test renewal (dry run):

```bash
sudo certbot renew --dry-run
```

Verify the renewal config uses the nginx authenticator (no downtime during renewal):

```bash
cat /etc/letsencrypt/renewal/yourdomain.com.conf
```

Should show `authenticator = nginx`. If it shows `authenticator = standalone`, switch it:

```bash
sudo certbot certonly --nginx -d yourdomain.com -d www.yourdomain.com --force-renewal
```

---

## Tomcat Configuration

Tomcat listens on port 8080 (localhost only) and does **not** handle SSL. The relevant connector in `server.xml`:

```xml
<Connector address="127.0.0.1" port="8080" protocol="HTTP/1.1"
           connectionTimeout="20000" />
```

No SSL connector, no keystore, no PEM files in Tomcat conf. Nginx handles all of that.

---

## Troubleshooting

### Certbot fails domain verification
- Verify DNS A record points to this VPS IP: `dig yourdomain.com`
- Verify port 80 is open: `sudo ufw status` or check IONOS firewall rules
- If using standalone mode, ensure nothing else is using port 80: `sudo lsof -i :80`

### Certificate not trusted in browser
- Verify the nginx config points to `fullchain.pem` (not `cert.pem`) — the full chain includes intermediate certificates

### 502 Bad Gateway
- Tomcat isn't running or isn't listening on 8080
- Check: `curl http://127.0.0.1:8080/` from the server
- Check Tomcat logs: `tail -50 /var/lib/tomcat10/logs/catalina.out`

### Large file upload fails
- The `client_max_body_size` in the nginx config limits upload size (set to 200M)
- Increase if needed for large Summit imports

---

## Quick Reference

| Item | Location |
|------|----------|
| Nginx site configs | `/etc/nginx/sites-enabled/` |
| Certbot certificates | `/etc/letsencrypt/live/yourdomain.com/` |
| Certbot renewal config | `/etc/letsencrypt/renewal/yourdomain.com.conf` |
| Tomcat config | `/var/lib/tomcat10/conf/server.xml` |
| Nginx logs | `/var/log/nginx/` |

---

## Production Reference (superiorstate.biz)

| Item | Value |
|------|-------|
| Nginx config | `/etc/nginx/sites-enabled/superiorstate.conf` |
| Certificate | `/etc/letsencrypt/live/superiorstate.biz/fullchain.pem` |
| Private key | `/etc/letsencrypt/live/superiorstate.biz/privkey.pem` |
| Authenticator | nginx (zero-downtime renewal) |
| Cert expiry | 2026-06-18 (auto-renews) |
| Domains | `superiorstate.biz`, `www.superiorstate.biz` |
