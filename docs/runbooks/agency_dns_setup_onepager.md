# Branded Portal & Email — DNS Setup

> **Agency-facing template.** This is the one-pager Superior State sends to an agency's IT/DNS
> administrator when onboarding a white-label custom domain. Fill the `‹…›` placeholders per agency
> before sending. The SSA-internal procedure that pairs with this is
> `docs/runbooks/agency_white_label_domain_onboarding.md`.

**For:** ‹AGENCY NAME› IT / DNS administrator
**Prepared by:** Superior State Administrators
**What this does:** points a subdomain of your domain (e.g. `admin.‹yourdomain›.com`) at your branded client portal, and lets proposal/notification emails send **as your domain** so they land in inboxes, not spam.

**Time required:** ~10 minutes, one time. **Records to add:** 3 (plus 1 optional).
**Does NOT affect:** your main website, your existing email, or your MX/mail flow. This only adds a subdomain and mail-authentication records — nothing that touches your primary domain's mail.

---

## Before you start

- Add these at whatever DNS provider hosts **‹yourdomain›.com** (GoDaddy, Cloudflare, your registrar, etc.).
- **If your DNS is on Cloudflare:** set every record below to **DNS only (gray cloud)**, not proxied (orange). These won't verify while proxied.
- Some providers auto-append your domain to the "Host/Name" field — enter only the part shown (e.g. `admin`, not `admin.yourdomain.com`). If unsure, check one existing record to see how your provider formats it.

---

## The records to add

### 1. Branded portal (website)

| Field | Value |
|---|---|
| **Type** | CNAME |
| **Host / Name** | `admin` |
| **Value / Target** | `‹SSA PROVIDES — portal target›` |
| **Proxy / Cloud** | DNS only (gray) |
| **TTL** | Auto / default |

### 2. Email — sender authentication (2 records)

These let email send as `‹yourdomain›.com` with a valid signature, so it passes spam/authentication checks.

| # | Type | Host / Name | Value / Target | Proxy |
|---|---|---|---|---|
| 2a | CNAME | `‹SSA PROVIDES — e.g. em1234.admin›` | `‹SSA PROVIDES›` | DNS only |
| 2b | CNAME | `‹SSA PROVIDES — e.g. s1234._domainkey.admin›` | `‹SSA PROVIDES›` | DNS only |

*(These two authenticate mail for the `admin.‹yourdomain›.com` sending subdomain. They sit at deeper names than the portal record in step 1, so they don't conflict with it.)*

### 3. (Optional) DMARC — only if you don't already have one

If `‹yourdomain›.com` has **no** DMARC record yet, adding this improves trust. If you already have one, **leave it as is** — don't add a second.

| Field | Value |
|---|---|
| **Type** | TXT |
| **Host / Name** | `_dmarc` |
| **Value** | `v=DMARC1; p=none; rua=mailto:‹SSA PROVIDES or your address›` |

---

## What Superior State handles (you don't need to)

- **SSL/HTTPS certificate** for the portal — issued and renewed automatically on our side. You do not provision or manage any certificate.
- **SPF/DKIM signing** — handled by our email platform once records 2a/2b are live; you don't edit your existing SPF record.
- **No mailbox needed** at the subdomain — the portal emails send *outbound* only; replies route to the sending agent's real inbox automatically. You don't need to create `admin@‹yourdomain›.com` or any mailbox, and you don't need an MX record for the subdomain.

---

## After you've added the records

Reply to let us know they're in place. We'll confirm on our end (usually within a few minutes to a couple of hours for DNS to propagate) that:
- the portal certificate has issued, and
- the sending domain shows **Verified**.

Once both are green, we activate the branded portal and switch email over. Nothing goes live on your end until we confirm — so there's no window where your subdomain shows an error.

---

**Questions?** Contact ‹SSA CONTACT NAME / EMAIL / PHONE›.

*One-time setup. These records can be removed later if you ever discontinue the branded portal, with no effect on your main domain.*
