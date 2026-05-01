# AMS Production Architecture — superiorstate.biz

**Last Updated:** 2026-05-01 (Phase 3 — Cloudflare proxy + RemoteIpValve + DNS-01 certs)  
**Status:** Active — reflects post-Phase-3 production state  
**Related:** `docs/infrastructure/cloudflare_setup.md`, `docs/infrastructure/letsencrypt_renewal.md`  
**Audit reference:** `docs/analysis/proxy_readiness_audit.md` (pre-Phase-3 gap analysis)

---

## Overview

```
Browser
  │
  │ HTTPS :443
  ▼
Cloudflare Edge  (Universal SSL cert presented to client; Full (strict) mode)
  │
  │ HTTPS :443  (Cloudflare verifies origin cert; CF-Connecting-IP header added)
  ▼
nginx on VPS public IP  (Let's Encrypt ECDSA cert; port 443 + 80 redirect)
  │
  │ HTTP :8080  (loopback only; sets X-Forwarded-* headers)
  ▼
Tomcat 10 on 127.0.0.1  (RemoteIpValve restores real scheme/IP/port from headers)
  │
  ▼
AMS WAR (ROOT.war)
```

No traffic reaches Tomcat directly from the internet. Tomcat is bound to `127.0.0.1:8080` and is not reachable from outside the VPS.

---

## VPS Specifications

| Property | Value |
|----------|-------|
| Hostname | `ssa-production` |
| Public IP | 66.179.248.171 |
| Provider | IONOS Cloud DCD — US-Las Vegas VDC |
| OS | Ubuntu 24.04 LTS |
| Java | OpenJDK 17 (`/usr/lib/jvm/java-17-openjdk-amd64`) |
| Tomcat | 10.x (`/var/lib/tomcat10`, `/usr/share/tomcat10`) |
| MySQL | 8.x — requires `LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu` workaround (Acronis library conflict at startup) |
| Nginx | Latest stable (apt) |
| Certbot | Latest stable (apt) with `python3-certbot-dns-cloudflare` plugin |

**MySQL startup workaround:** The Acronis agent installs a library that conflicts with MySQL 8 client detection on Ubuntu 24. Tomcat's `context.xml` JDBC connection string uses `LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu` in the systemd environment. If MySQL appears unreachable after a reboot, verify this path is still set. See `docs/deployment_runbook.md` for details.

---

## DNS

DNS is managed at Cloudflare (nameservers `dylan.ns.cloudflare.com`, `isabel.ns.cloudflare.com`).  
Both domains are registered at **Cloudflare Registrar** (transferred 2026-05-01).

### superiorstate.biz

| Type | Name | Value | Proxied? | Notes |
|------|------|-------|----------|-------|
| A | `@` (apex) | 66.179.248.171 | ✅ Orange cloud | Main application |
| A | `www` | 66.179.248.171 | ✅ Orange cloud | www redirect |

### superiorstate.net

| Type | Name | Value | Proxied? | Notes |
|------|------|-------|----------|-------|
| A | `@` (apex) | 66.179.248.171 | ✅ Orange cloud | Master registry endpoint |
| A | `www` | 66.179.248.171 | ✅ Orange cloud | |
| MX | `@` | (M365 MX record) | ❌ DNS only | Required for M365 mail flow |
| TXT | `@` | `v=spf1 ...` | ❌ DNS only | SPF record |
| TXT | `_dmarc` | `v=DMARC1 ...` | ❌ DNS only | DMARC policy |
| TXT | `s102001._domainkey` | (DKIM key) | ❌ DNS only | SMTP2GO DKIM |
| CNAME | `autodiscover` | (M365 value) | ❌ DNS only | Outlook autodiscovery |
| CNAME | `em102001` | (SMTP2GO value) | ❌ DNS only | SMTP2GO sending domain |

**Why email records are not proxied:** Cloudflare proxy only applies to HTTP/HTTPS traffic on ports 80/443. Mail delivery uses ports 25/587/465 which bypass Cloudflare regardless of proxy state. However, MX and SPF/DKIM/DMARC TXT records must be DNS-only so mail senders can resolve them directly. Setting these to orange-cloud would cause lookup failures.

---

## SSL / TLS

### Three-layer cert chain

```
Client ←→ Cloudflare:   Cloudflare Universal SSL (Cloudflare-managed, auto-renewed)
Cloudflare ←→ nginx:    Let's Encrypt ECDSA cert at /etc/letsencrypt/live/{domain}/
```

Cloudflare's **Full (strict)** mode validates the origin cert (requires a valid, trusted cert on nginx — not a self-signed cert). Let's Encrypt satisfies this requirement.

### Origin certificate locations (on VPS)

| Domain | Cert path | Key path |
|--------|-----------|----------|
| superiorstate.biz | `/etc/letsencrypt/live/superiorstate.biz/fullchain.pem` | `/etc/letsencrypt/live/superiorstate.biz/privkey.pem` |
| superiorstate.net | `/etc/letsencrypt/live/superiorstate.net/fullchain.pem` | `/etc/letsencrypt/live/superiorstate.net/privkey.pem` |

### Cert renewal

- **Plugin:** `python3-certbot-dns-cloudflare` (DNS-01 challenge)
- **Credentials:** `/etc/letsencrypt/cloudflare.ini` (mode 600, root-owned; **not tracked in repo**)
- **Renewal trigger:** certbot systemd timer (Ubuntu default — runs twice daily)
- **Renewal logs:** `/var/log/letsencrypt/letsencrypt.log`
- See `docs/infrastructure/letsencrypt_renewal.md` for full renewal reference.

**Why DNS-01 (not HTTP-01):** HTTP-01 requires port 80 to reach the origin. With Cloudflare proxy enabled (orange cloud), Cloudflare intercepts port 80 before reaching nginx/certbot. DNS-01 uses the Cloudflare API to place a TXT challenge record, so cert renewal is entirely independent of HTTP traffic and proxy state.

---

## Reverse Proxy Chain Detail

### nginx role

nginx is the only process listening on the public IP's ports 80 and 443.

- **Port 80:** returns `301 https://$host$request_uri` (no content served)
- **Port 443:** terminates TLS; reverse-proxies to Tomcat at `http://127.0.0.1:8080/`

nginx sets these headers on every proxied request to Tomcat:

```nginx
proxy_set_header Host              $host;
proxy_set_header X-Real-IP         $remote_addr;
proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto https;
```

Additionally, `/etc/nginx/conf.d/cloudflare-real-ip.conf` rewrites nginx's `$remote_addr` using the `CF-Connecting-IP` header (which Cloudflare sets to the true client IP). This means `X-Real-IP` and `X-Forwarded-For` that nginx sends downstream already contain the real client IP, not the Cloudflare edge IP.

### Tomcat RemoteIpValve role

Tomcat's `RemoteIpValve` (configured in `server.xml` inside the `<Host>` block) reads the `x-forwarded-for` and `x-forwarded-proto` headers set by nginx and rewrites the request's internal properties:

| Property | Without valve | With valve |
|----------|--------------|------------|
| `request.getScheme()` | `"http"` | `"https"` |
| `request.getServerPort()` | `8080` | `443` |
| `request.getRemoteAddr()` | `127.0.0.1` | real client IP |
| `request.isSecure()` | `false` | `true` |
| Session `Secure` cookie flag | not set | set |

The valve trusts only `127.0.0.1` / `::1` (the nginx loopback) as the immediate upstream proxy. External `X-Forwarded-For` values that arrive from Cloudflare are preserved but not trusted as internal proxy IPs.

---

## Config File Locations on VPS

| File | Purpose |
|------|---------|
| `/var/lib/tomcat10/conf/server.xml` | Tomcat connector + valves (canonical) |
| `/var/lib/tomcat10/conf/context.xml` | JNDI datasource, connection pool |
| `/var/lib/tomcat10/conf/ssa.properties` | Infrastructure config (PSP_ID, SYSTEM_URL, S3 creds, etc.) |
| `/var/lib/tomcat10/webapps/ROOT.war` | Deployed application WAR |
| `/var/lib/tomcat10/logs/catalina.out` | Tomcat stdout + startup log |
| `/var/lib/tomcat10/data/` | File upload storage (SAVE_PATH) |
| `/var/lib/tomcat10/branding/` | PSP logo/branding uploads |
| `/etc/nginx/sites-available/superiorstate.conf` | nginx config for superiorstate.biz |
| `/etc/nginx/sites-available/superiorstate-net.conf` | nginx config for superiorstate.net |
| `/etc/nginx/sites-enabled/` | Symlinks to enabled site configs |
| `/etc/nginx/conf.d/cloudflare-real-ip.conf` | Cloudflare IP trust + CF-Connecting-IP rewrite |
| `/etc/nginx/conf.d/upload-size.conf` | `client_max_body_size 200M;` global setting |
| `/etc/letsencrypt/cloudflare.ini` | Cloudflare API token for certbot DNS-01 (**not in repo — secret**) |
| `/etc/letsencrypt/live/superiorstate.biz/` | Active cert symlinks |
| `/etc/letsencrypt/live/superiorstate.net/` | Active cert symlinks |
| `/etc/letsencrypt/renewal/superiorstate.biz.conf` | Certbot renewal config (`authenticator = dns-cloudflare`) |
| `/etc/letsencrypt/renewal/superiorstate.net.conf` | Certbot renewal config (`authenticator = dns-cloudflare`) |
| `/opt/ssa/scripts/backup.sh` | Nightly database backup to Wasabi |
| `/opt/ssa/scripts/healthcheck.sh` | Nightly health report email |
| `/opt/ssa/scripts/update.sh` | Nightly release check + migration apply |
| `/opt/ssa/current_version.txt` | Currently deployed WAR git tag |
| `/opt/ssa/backups/ssa-previous.war` | Previous WAR (rollback target) |

Sanitized copies of the Tomcat and nginx config files that changed in Phase 3 are tracked in `docs/infrastructure/configs/`.

---

## What Is Intentionally NOT in This Architecture Yet

The following items were identified in `docs/analysis/proxy_readiness_audit.md` and are deferred to future phases. Each has a backlog item.

| Missing | Risk | Backlog |
|---------|------|---------|
| **Origin firewall** — no UFW/iptables rule restricting port 443 to Cloudflare IP ranges. The origin IP (66.179.248.171) can be connected to directly, bypassing Cloudflare DDoS protection and IP hiding. | Medium — not exploited yet, but discoverable | D-73 |
| **HSTS headers** — nginx does not set `Strict-Transport-Security`. Cloudflare can serve HSTS at the edge, but origin responses don't include it. | Low | D-73 (can bundle) |
| **SameSite=Strict on session cookie** — `web.xml` has no `<session-config>`. Tomcat defaults apply. | Low | D-75 |
| **Demo/BPO/master VPS standardization** — demo and BPO VPSes predate the nginx+RemoteIpValve architecture and use different SSL setups | Medium | D-74 |
| **URL-generation servlet cleanup** — 5 servlets still build URLs from raw `request.getScheme()` etc. Now produce correct URLs via RemoteIpValve but are fragile if the valve is removed | Low (RemoteIpValve now in place) | D-71 |
