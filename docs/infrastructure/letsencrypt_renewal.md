# Let's Encrypt Certificate Renewal Reference

**Last Updated:** 2026-05-01 (Phase 3b — switched from HTTP-01 to DNS-01 via Cloudflare API)  
**Related:** `docs/infrastructure/cloudflare_setup.md`, `docs/infrastructure/production_architecture.md`

---

## Current Setup

| Property | Value |
|----------|-------|
| Plugin | `python3-certbot-dns-cloudflare` |
| Challenge type | DNS-01 (Cloudflare API) |
| Credentials file | `/etc/letsencrypt/cloudflare.ini` (mode 600, root-owned, **not in repo**) |
| Renewal trigger | certbot systemd timer (Ubuntu default) |
| Renewal window | Auto-renews when cert has ≤ 30 days remaining |
| Timer frequency | Twice daily |
| Cert expiry | 90 days from issuance |

### Certs managed

| Domain (+ www) | Renewal config |
|----------------|---------------|
| superiorstate.biz | `/etc/letsencrypt/renewal/superiorstate.biz.conf` |
| superiorstate.net | `/etc/letsencrypt/renewal/superiorstate.net.conf` |

Both renewal configs show `authenticator = dns-cloudflare`.

---

## Why DNS-01 (Not HTTP-01)

The previous setup used HTTP-01 with the nginx authenticator. That worked when:
- DNS was gray-cloud (direct-to-origin)
- certbot could serve the `.well-known/acme-challenge/` token via nginx on port 80

With Cloudflare proxy enabled (orange cloud), Cloudflare intercepts port 80 before
reaching nginx/certbot. HTTP-01 challenge responses may be cached, transformed, or
blocked by Cloudflare, making renewal unreliable.

DNS-01 places a `_acme-challenge` TXT record via the Cloudflare DNS API. Let's Encrypt
verifies it by querying DNS directly — completely independent of HTTP traffic and proxy state.

---

## Verifying Renewal Configuration

```bash
# Check the systemd timer is active
sudo systemctl list-timers | grep certbot

# Verify both certs and their authenticators
sudo certbot certificates

# Check renewal config for a domain
cat /etc/letsencrypt/renewal/superiorstate.biz.conf
# Should contain: authenticator = dns-cloudflare

# Dry-run renewal (no cert changes, just validates)
sudo certbot renew --dry-run
```

---

## Manual Renewal

If auto-renewal fails or you need to renew immediately:

```bash
sudo certbot renew --force-renewal
sudo systemctl reload nginx
```

---

## Re-issuing a Certificate from Scratch

If the renewal config or cert files are lost, re-issue:

```bash
sudo certbot certonly \
  --dns-cloudflare \
  --dns-cloudflare-credentials /etc/letsencrypt/cloudflare.ini \
  -d superiorstate.biz \
  -d www.superiorstate.biz
```

Reload nginx afterward:

```bash
sudo systemctl reload nginx
```

Repeat for `superiorstate.net`.

---

## What Breaks Renewal

| Cause | Symptom | Fix |
|-------|---------|-----|
| Cloudflare API token expired or deleted | Renewal fails with auth error | Re-create token in Cloudflare dashboard; update `/etc/letsencrypt/cloudflare.ini` |
| `/etc/letsencrypt/cloudflare.ini` deleted or permissions changed | Renewal fails — certbot cannot read credentials | Recreate file (mode 600, root-owned) with token value |
| Renewal `.conf` files manually edited | Wrong authenticator; renewal fails | Re-run `certbot certonly --dns-cloudflare ...` for the affected domain |
| Cert files deleted | nginx fails to start | Re-issue from scratch (see above) |

---

## Monitoring

Certbot logs renewal outcomes to `/var/log/letsencrypt/letsencrypt.log`.

The healthcheck script (`/opt/ssa/scripts/healthcheck.sh`) reports cert expiry status
in its nightly email. If a cert is within 14 days of expiry, treat this as an alert —
auto-renewal should have run by then.

---

## Relationship to Cloudflare Universal SSL

Cloudflare presents its own Universal SSL cert to browsers (client ↔ Cloudflare leg).
The Let's Encrypt cert is only used for the Cloudflare ↔ origin (nginx) leg.

Cloudflare validates the origin cert in **Full (strict)** mode. If the Let's Encrypt
cert expires without renewal, Cloudflare will refuse to connect to the origin and
users will see a 502 error. The Cloudflare Universal SSL (client-facing) would still
be valid — but it could not reach the origin.

Cert expiry on the origin is therefore still critical even though clients never
see the Let's Encrypt cert directly.
