# Branded Portal & Email — DNS Setup (Root Domain)

> **Agency-facing template — ROOT/APEX version (`‹yourdomain›.com` itself).** Superior State sends
> this when the branded portal runs on a **dedicated program domain's root** (e.g. `premiumpath.net`)
> rather than a subdomain. Fill the `‹…›` placeholders per agency before sending. For the more common
> subdomain setup, use `agency_dns_setup_onepager.md`. Internal procedure:
> `agency_white_label_domain_onboarding.md`.
>
> **Use this only for a dedicated domain whose root is free** (no existing website or email at the
> root). If the agency's root already runs their website/mail, use the subdomain version instead.

**For:** ‹AGENCY NAME› IT / DNS administrator
**Prepared by:** Superior State Administrators
**What this does:** points your domain **`‹yourdomain›.com`** (the root) at your branded client portal, and lets proposal/notification emails send **as your domain** so they land in inboxes, not spam.

**Time required:** ~10–15 minutes, one time. **Records to add:** 6 (plus 1 optional).
**Requires:** your domain's DNS on **Cloudflare** (so the root can use a CNAME) — or, if not on Cloudflare, we provide an **A record** to use at the root instead.

---

## Before you start

- Add these at whatever hosts DNS for **‹yourdomain›.com**.
- **Proxy setting:** the **email and validation records are always DNS only (gray cloud)**. The **root portal record (#1)** is **Proxied (orange)** on Cloudflare — we'll confirm. *(If your DNS is not on Cloudflare, we'll give you an **A record** for the root instead, set DNS only.)*
- For the root, the "Host/Name" is the domain itself — most providers show this as `@` or blank.

---

## The records to add

### 1. Branded portal (website) — at the root

| Field | Value |
|---|---|
| **Type** | CNAME *(root CNAME; works on Cloudflare via CNAME-flattening — otherwise use the A record we provide)* |
| **Host / Name** | `@` (the root / apex) |
| **Value / Target** | `‹SSA PROVIDES — portal target›` |
| **Proxy / Cloud** | Proxied (orange) on Cloudflare *(DNS only if using the A-record alternative)* |
| **TTL** | Auto / default |

*(Optional `www`: add a CNAME `www` → `@`, proxied, plus a redirect — we can help set this up.)*

### 2. Certificate validation (2 records)

These prove you control the domain so the HTTPS certificate can be issued and auto-renewed. The certificate covers the root **and** `www`, so there are **two** values — add both exactly as we provide.

| # | Type | Host / Name | Value / Target | Proxy |
|---|---|---|---|---|
| 2a | TXT | `_acme-challenge` | `‹SSA PROVIDES — value #1›` | DNS only |
| 2b | TXT | `_acme-challenge` | `‹SSA PROVIDES — value #2›` | DNS only |

### 3. Email — sender authentication (3 records)

These let email send as `‹yourdomain›.com` with a valid signature. All **DNS only**.

| # | Type | Host / Name | Value / Target | Purpose |
|---|---|---|---|---|
| 3a | CNAME | `‹SSA PROVIDES — e.g. em1234›` | `‹SSA PROVIDES›` | Return-path / SPF |
| 3b | CNAME | `‹SSA PROVIDES — e.g. s1234._domainkey›` | `‹SSA PROVIDES›` | DKIM signature |
| 3c | CNAME | `‹SSA PROVIDES — e.g. link›` | `‹SSA PROVIDES›` | Click tracking |

### 4. (Optional) DMARC — only if you don't already have one

| Field | Value |
|---|---|
| **Type** | TXT |
| **Host / Name** | `_dmarc` |
| **Value** | `v=DMARC1; p=none; rua=mailto:‹SSA PROVIDES or your address›` |

---

## What Superior State handles (you don't need to)

- **SSL/HTTPS certificate** for the portal — issued and renewed automatically on our side once the validation records (2a/2b) are live. You do not provision or manage any certificate.
- **SPF/DKIM signing** — handled by our email platform once records 3a/3b are live; you **don't edit any existing SPF record**. Mail authenticates and aligns to your domain.
- **No mailbox and no MX change** — the portal emails send *outbound* only; replies route to the sending agent's real inbox automatically.

---

## After you've added the records

Reply to let us know they're in place. We'll confirm (usually within minutes, up to a couple of hours for DNS to propagate) that the portal certificate has issued and the sending domain shows **Verified**, then activate the branded portal and switch email over. Nothing goes live on your end until we confirm.

---

**Questions?** Contact ‹SSA CONTACT NAME / EMAIL / PHONE›.

*One-time setup. These records can be removed later if you ever discontinue the branded portal.*
