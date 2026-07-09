# Cloudflare Account & Zone Configuration

**Last Updated:** 2026-05-01 (Phase 3c — proxy enabled, DNS-01 certs, registrar transfer)  
**Related:** `docs/infrastructure/production_architecture.md`, `docs/infrastructure/letsencrypt_renewal.md`

---

## Account

- **Account holder:** ekevinmurphy@gmail.com
- **Plan:** Free (both zones)
- **Nameservers:** `dylan.ns.cloudflare.com`, `isabel.ns.cloudflare.com`
- **Registrar:** Cloudflare Registrar (both domains transferred 2026-05-01 from GoDaddy)

### Registrar renewal costs (at-cost pricing)

| Domain | Renewal price |
|--------|--------------|
| superiorstate.biz | ~$16.20/yr |
| superiorstate.net | ~$11.86/yr |

---

## Zones

Both zones point to the same production VPS (66.179.248.171). See
`docs/infrastructure/production_architecture.md` for the full DNS record table.

### SSL/TLS Mode

**Both zones: Full (strict)**

This mode requires a valid, CA-trusted certificate on the origin (nginx). The
Let's Encrypt cert satisfies this. Do not change to "Flexible" — that would
allow Cloudflare to accept an invalid origin cert and create a false sense of
security.

| Mode | Cloudflare→Client | Cloudflare→Origin | Origin cert required |
|------|------------------|-------------------|---------------------|
| Off | HTTP only | HTTP | No |
| Flexible | HTTPS | HTTP | No |
| Full | HTTPS | HTTPS | Any cert (self-signed OK) |
| **Full (strict)** | **HTTPS** | **HTTPS** | **Valid CA cert** ← current |

---

## Proxy State by Record Type

### Why email records are DNS-only

Cloudflare proxy only intercepts HTTP/HTTPS on ports 80/443. Mail servers (SMTP, IMAP)
connect on ports 25/587/465 which bypass Cloudflare's proxy entirely regardless of
whether a record is orange-cloud or gray-cloud.

However, records that mail senders query by DNS (MX, SPF TXT, DKIM TXT, DMARC TXT,
autodiscover CNAME) must be DNS-only. If they were set to orange-cloud, the
Cloudflare proxy would intercept the IP lookup — there's no meaningful proxying
for non-HTTP records, but setting them to orange-cloud would cause unexpected
behavior and is not supported by Cloudflare for these record types anyway.

### Summary

| Record | Proxied? | Reason |
|--------|----------|--------|
| A @ (apex) | ✅ Yes | Main app traffic — DDoS protection, IP hiding |
| A www | ✅ Yes | www redirect through Cloudflare |
| MX | ❌ No | Mail delivery — must be direct |
| TXT SPF / DMARC | ❌ No | DNS lookup by mail senders |
| TXT DKIM | ❌ No | DNS lookup by mail senders |
| CNAME autodiscover | ❌ No | M365 Outlook autodiscovery |
| CNAME em102001 | ❌ No | SMTP2GO sending domain verification |

---

## API Token for certbot DNS-01

- **Token name:** `certbot-dns-letsencrypt`
- **Scope:** Zone → DNS → Edit, on zones `superiorstate.biz` and `superiorstate.net`
- **Used by:** certbot `python3-certbot-dns-cloudflare` plugin on the production VPS
- **Stored at:** `/etc/letsencrypt/cloudflare.ini` (mode 600, root-owned, on VPS)
- **NEVER commit the token value to the repo**

If the token is lost or expired:
1. Log into Cloudflare dashboard → My Profile → API Tokens
2. Create a new token with the same scope
3. Update `/etc/letsencrypt/cloudflare.ini` on the VPS with the new value
4. Run `sudo certbot renew --dry-run` to confirm the new token works

---

## What to Do if Cloudflare is Unavailable or Needs to Be Bypassed

### Temporarily disable proxy (go gray-cloud)

1. Log into Cloudflare dashboard → DNS → select the A record → click the orange cloud to toggle to gray cloud
2. DNS propagates within seconds (Cloudflare's TTL is 5 minutes for proxied records, configurable for gray-cloud)
3. Clients connect directly to the origin (66.179.248.171) without going through Cloudflare
4. The Let's Encrypt origin cert is valid and trusted by browsers — traffic still works over HTTPS

This is the fastest recovery path if Cloudflare has an outage or if you need to
troubleshoot the origin directly.

### nginx `cloudflare-real-ip.conf` behavior when gray-cloud

If the proxy is disabled, nginx no longer receives `CF-Connecting-IP`. The
`real_ip_header CF-Connecting-IP` directive falls back gracefully — `$remote_addr`
remains the actual connecting client IP (since there's no Cloudflare edge
between client and nginx). The file does not need to be removed when going gray-cloud.

### Tomcat RemoteIpValve behavior when gray-cloud

`X-Forwarded-For` and `X-Forwarded-Proto` are still set by nginx regardless of
Cloudflare proxy state. The valve continues to work correctly.

---

## Operational Notes

- **Origin IP exposure:** The production IP (66.179.248.171) is not yet blocked at the
  firewall for non-Cloudflare connections. See D-73 in the deployment backlog.
  Until D-73 is implemented, a determined attacker can bypass Cloudflare by connecting
  directly to the origin IP.

- **Cloudflare Analytics:** With proxy enabled, Cloudflare's dashboard shows traffic
  statistics. Bot traffic and DDoS attempts are visible there.

- **Audit trail:** The proxy was enabled 2026-05-01. The pre-enablement analysis is
  in `docs/analysis/proxy_readiness_audit.md`.

---

## Per-Agency White-Label Email Sending (V069)

AMS can send an agency's outbound mail `From:` the agency's own subdomain
(e.g. `admin.swbd.com`) instead of a Superior State address, without landing in spam.
This requires the agency's sending domain to be **verified in SMTP2GO** *before* the
per-agency `email_verified` flag is turned on in AMS. The application code path is
`EmailIdentityResolver` (Tier 1); see `docs/analysis/email_identity_current_state.md`
and the V069 migration header for the tier model.

### Onboarding an agency sending domain (per agency)

1. **Pick the sending subdomain.** Typically the same subdomain as the agency's web
   landing host (V068 `landing_host`, e.g. `admin.swbd.com`). The Agency Manager UI
   pre-fills the Sending Domain field from the landing host, but the two are stored and
   verified **independently** — web presence (A/CNAME) is not relay verification.

2. **Add the domain in SMTP2GO** (Sender Domains → Add Sending Domain →
   `admin.swbd.com`). SMTP2GO issues the DNS records to publish **on the agency-owned
   zone** (the agency's DNS, not ours):
   - **SPF return-path CNAME** — `em<id>.admin.swbd.com` → SMTP2GO target
     (same mechanism as our own `em102001` record, table above). This is what makes
     SMTP2GO's VERP return-path align for the subdomain.
   - **DKIM CNAME(s)** — `s<id>._domainkey.admin.swbd.com` → SMTP2GO DKIM target
     (same mechanism as our own `s102001._domainkey`).
   - These are **DNS-only** (never proxied) — mail senders must resolve them directly,
     exactly like our apex mail records (see the Summary table: MX / SPF / DKIM = ❌ No).

3. **Wait for SMTP2GO to show the domain "Verified."** SMTP2GO polls the DNS records;
   propagation can take up to a few hours.

4. **Only then flip `email_verified`** in the AMS Agency Manager (Edit Agency →
   Verified toggle, PSP-admin only). **Strict sequencing gate:** never enable
   `email_verified` before SMTP2GO reports Verified. Turning it on early makes AMS send
   `From: <localpart>@admin.swbd.com` with no aligned SPF/DKIM → DMARC failure / spam,
   which is worse than the Tier-2 fallback (`notifications@superiorstate.net`).

### Notes

- **DMARC on the agency zone:** if the agency publishes a strict `p=reject`/`p=quarantine`
  DMARC policy on `swbd.com`, the subdomain `admin.swbd.com` inherits it unless it has its
  own `_dmarc.admin.swbd.com` record. SMTP2GO DKIM/SPF alignment on the subdomain satisfies
  DMARC once verified — hence the gate.
- **Envelope / return-path:** AMS does **not** set an explicit envelope sender (the V069
  build removed the `mail.smtp.from` override); SMTP2GO's own return-path (the `em<id>`
  CNAME) owns SPF alignment per verified domain.
- **Fallback is safe by default:** until an agency is verified, its agents send Tier-2
  (`From: notifications@superiorstate.net`, `Reply-To:` the agent's real address) — aligned
  and deliverable. No agency change is required for that fallback to work.
