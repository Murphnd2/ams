# Agency White-Label Custom Domain — Onboarding Runbook

**Status:** v3 (2026-07-15). Authoritative — reconciled with the **live `premiumpath.net`
(Southwestern Benefit Designers / "PremiumPath") apex onboarding**. Covers **both** an apex/root
domain and an `admin.` subdomain, so a new agency can be onboarded either way.

This is the **SSA-internal** procedure. The **agency-facing** DNS instructions are the companion
one-pager: `docs/runbooks/agency_dns_setup_onepager.md`.

**Related:** `docs/infrastructure/cloudflare_setup.md` · `docs/infrastructure/production_architecture.md` ·
`docs/analysis/email_identity_current_state.md` · migrations `V068__agency_landing_host.sql`, `V069__agency_email_sending.sql`.

**First live onboarding:** `premiumpath.net` (apex) for the SWBD agency. No code or migration is
needed to onboard a domain — it's Cloudflare + SMTP2GO + DNS config plus the Agency Manager fields
(shipped in V068/V069).

---

## Choose the path

| | **Apex / root** (`agency.com`) | **Subdomain** (`admin.agency.com`) |
|---|---|---|
| Branding | Cleanest (`agency.com`) | Longer URL |
| Root impact | **Occupies the root** — can't coexist with an existing root website/mail | Leaves existing site + primary mail **untouched** |
| Best for | A **dedicated program/marketing domain** with a free root (the premiumpath case) | An **established agency** with a live site/mail |
| DNS requirement | Apex CNAME needs **Cloudflare zone (flattening)** — or an **A record → `66.179.248.171`** | Subdomain CNAME is **always legal** (any provider); DCV delegation if not on Cloudflare |

**Recommendation:** default to the **subdomain** for any agency with a live root website/mail;
use **apex** only for a dedicated program domain with a free root.

---

## Shared one-time infra (`superiorstate.net` zone — set once, reused by every agency)

| Type | Name | Target | Proxy |
|---|---|---|---|
| A | `superiorstate.net` (`@`) | `66.179.248.171` | Proxied |
| CNAME | `origin` | `superiorstate.net` | **Proxied** |

- **Fallback Origin** (SSL/TLS → Custom Hostnames) = **`origin.superiorstate.net`** — must be a
  **proxied subdomain**, never the apex. Every agency custom hostname routes through it.
- **SSL/TLS mode = Full (NOT strict)** on `superiorstate.net`. The origin LE cert SAN is only
  `superiorstate.net` + `www` (no wildcard); a custom hostname pulls the origin with **SNI = the
  agency domain**, so **Full (strict) → error 526**. Full (not strict) is required. Low risk: the
  origin is **Cloudflare-only ingress** (not reachable on public 443). Per-host **Origin SNI** (the
  strict-preserving fix) is **Enterprise-only** — Cloudflare API error 1456.

---

## Path A — Apex / root domain (the `premiumpath.net` pattern)

**Prereq:** dedicated program domain, free root; agency domain on **its own Cloudflare zone** (for
apex CNAME-flattening). If not on Cloudflare, replace the apex CNAME with an **A record → `66.179.248.171`**.

**A1. Agency DNS (agency's Cloudflare zone):**
| Type | Name | Target | Proxy |
|---|---|---|---|
| CNAME | `@` (apex) | `origin.superiorstate.net` | **Proxied** (relies on CNAME-flattening) |
| TXT | `_acme-challenge` | *(cert value #1 — from the Custom Hostname row)* | DNS only |
| TXT | `_acme-challenge` | *(cert value #2 — present because the cert covers apex **+ www**)* | DNS only |
| *(opt)* CNAME | `www` | `@` | Proxied (+ a redirect rule) |

**A2. Cloudflare for SaaS Custom Hostname (`superiorstate.net` zone):**
- Custom Hostname = `agency.com`; Certificate = **Cloudflare-provided** (CA = SSL.com);
  Validation = **TXT**; Origin = **Default origin server** (→ Fallback Origin); **Origin SNI = none**
  (defaults to Host header).
- **DCV:** the two `_acme-challenge` **TXT** records above (read the exact values from the expanded
  hostname row — **two** because the cert covers apex + www). This is **not** DCV-delegation — ignore
  the `…dcv.cloudflare.com` box (that's for unproxied/off-Cloudflare DNS or wildcards).
- Goes **Active within a few minutes** of TXT propagation; auto-renews.

**A3. Email (SMTP2GO), sender domain = apex.** Add `agency.com` in SMTP2GO → Sender Domains, then
publish its **3 CNAMEs on the apex zone (all DNS only)**:
| Type | Name | Target | Purpose |
|---|---|---|---|
| CNAME | `em<id>` | `return.smtp2go.net` | Return-path / SPF (bounce) |
| CNAME | `s<id>._domainkey` | `dkim.smtp2go.net` | DKIM |
| CNAME | `link` | `track.smtp2go.net` | Click tracking |

- The `em<id>` / `s<id>` prefixes are **assigned per-domain by SMTP2GO** (premiumpath used
  `em102001` / `s102001._domainkey`).
- **No need to edit the agency's existing apex SPF**, even when one exists: SPF is evaluated on the
  **return-path subdomain** (`em….agency.com`, SPF via the CNAME) and **aligns** to the org domain
  under relaxed DMARC; DKIM signs `d=agency.com`. **MX is never touched** (SMTP2GO is send-only).

**A4. AMS Agency Manager (Edit Agency):** Landing Host = `agency.com`, Sending Domain =
`agency.com`, flip **Verified** only after SMTP2GO shows Verified. (premiumpath also enabled agent
markup — optional.)

---

## Path B — Subdomain (`admin.agency.com`)

**B1. Agency DNS (agency's zone — Cloudflare or any provider):**
| Type | Name | Target | Proxy |
|---|---|---|---|
| CNAME | `admin` | `origin.superiorstate.net` | Proxied if on Cloudflare; **DNS-only** if not (subdomain CNAME is always legal — no flattening needed) |
| TXT | `_acme-challenge.admin` | *(cert value — usually **one** for a single hostname)* | DNS only |

- **If the agency's DNS is NOT on Cloudflare** (can't proxy), use **DCV delegation** for
  auto-renewing certs instead of the TXT: `_acme-challenge.admin` **CNAME** →
  `<hostname>.<id>.dcv.cloudflare.com` (the value Cloudflare shows in the DCV-delegation box).

**B2. Cloudflare SaaS:** Custom Hostname = `admin.agency.com`; Default origin. Same SSL-mode
requirement as Path A (Full, not strict).

**B3. Email (SMTP2GO), sender domain = `admin.agency.com`:** the same 3 CNAMEs, **nested under the
subdomain** (`em….admin`, `s…._domainkey.admin`, `link.admin`), DNS only — the agency's **apex mail
stays 100% untouched**.

**B4. Agency Manager:** Landing Host / Sending Domain = `admin.agency.com`; Verified after SMTP2GO.

---

## Part C — AMS Agency Manager fields (Edit Agency, PSP-admin only)

| Field (UI) | Column (V0##) | Purpose |
|---|---|---|
| **Landing Host** | `landing_host` (V068) | Agency host serving the branded landing (must be unique). |
| **Landing HTML** | `landing_html` (V068) | Branded landing markup (sanitized by `LandingSafe` on save — see below). |
| **Sending Domain** | `email_domain` (V069) | Agency sending domain/subdomain. Blank → falls back to `SMTP_FROM`. |
| **Verified in SMTP2GO** | `email_verified` (V069) | Manual gate — flip **only after** SMTP2GO shows Verified. |
| **Enable agent markup** | `markup_enabled` (V067) | Optional (premiumpath enabled it). |

---

## Gotchas (from the premiumpath onboarding)

| Problem | Cause | Fix |
|---|---|---|
| Custom hostname won't route | Zone Fallback Origin empty | Create proxied `origin.superiorstate.net`, set as Fallback Origin (proxied **subdomain**, not apex) |
| **Error 526** on the hostname | Origin cert SAN = `superiorstate.net`+`www` only; custom-hostname pull sends **SNI = agency domain** → strict rejects | Set `superiorstate.net` SSL mode to **Full (not strict)** — low risk (origin is CF-only ingress) |
| Per-host Origin SNI unavailable | Custom Origin SNI is **Enterprise-only** (API 1456) | Use Full (not strict). Long-term re-harden: paid SSL-for-SaaS + Origin SNI, or Authenticated Origin Pulls |
| Apex can't CNAME | DNS forbids CNAME at a zone apex | Works via Cloudflare **flattening** (domain on Cloudflare); else **A record → `66.179.248.171`** |
| Landing logo vanished | Landing HTML field **strips raw `<svg>`** | Use `<img src="data:image/svg+xml,…">`; if `data:` also stripped, host the SVG on `superiorstate.biz` and use an `https://` `src` |
| Can't read origin cert from workstation | `openssl` absent + origin not on public 443 | On the origin box: `openssl s_client -connect 127.0.0.1:443 -servername superiorstate.net` |
| Cloudflare API "Invalid Authorization" | Used a 32-char **Zone/Account ID** as the token | Create a real API token (~40 chars): **Zone:SSL and Certificates:Edit** + **Zone:Read**, scoped to the zone; verify via `/user/tokens/verify` |
| PowerShell parse errors | Pasted multi-line/backtick text | Paste commands as **single lines**; cancel a `>>` prompt with Enter/Ctrl+C |

---

## Verification checklist

- [ ] Custom Hostname shows **Active** (edge cert issued) in Cloudflare.
- [ ] Browsing the agency host loads the branded landing (PSP chrome suppressed).
- [ ] SMTP2GO shows the sending domain **Verified**.
- [ ] A test send from an agency agent arrives `From: <agent>@<sending-domain>`, passing SPF/DKIM/DMARC (check headers).
- [ ] `email_verified` was flipped **after** SMTP2GO verification, not before.

---

## Landing HTML sanitizer (verified from `LandingSafe.java`, V068)

`LandingSafe` (`data/util/LandingSafe.java`) cleans landing HTML on save (Jsoup `Safelist.relaxed()`
+ structural tags, `class`/`id`/inline `style`, `https` font `<link>`s, `data:` images).

- **Inline `<svg>` is stripped** (not in the safelist) — deliver graphics as a **`data:` URI `<img>`**
  (`<img src="data:image/svg+xml,…">`). If `data:` is also stripped, host the SVG on
  `superiorstate.biz` and reference by `https://`. Verify survival on the live page with Ctrl+U →
  search for the tag.
- **Blocked:** `<script>`/`<iframe>`/`<object>`/`<embed>`/`<form>`, `on*` handlers, and dangerous CSS
  (`expression()`, `javascript:`/`vbscript:` URLs, non-https `@import`, `behavior`, `-moz-binding`).

---

## Practical learnings

- **Fallback Origin is one-time and zone-wide** on `superiorstate.net`; every agency reuses it.
- **Free plan ⇒ SaaS zone must be Full (not strict).** No per-host Origin SNI without Enterprise.
- **TXT count differs by scope:** an apex issuance covers apex + www → often **two** `_acme-challenge`
  values; a single subdomain hostname → usually **one**. Read the exact set from the expanded Custom
  Hostname row rather than assuming.
- **Email aligns without editing the agency's apex SPF** in either path (SMTP2GO uses a return-path
  subdomain + DKIM selector). Still prefer the **subdomain** for sending when the root runs the
  agency's real mail, to keep concerns separated. **MX is never touched.**
- **Read the origin cert on the origin box**, not your workstation (origin isn't on public 443).
- **Timing:** with TXT validation, cert + hostname go **Active within minutes** of propagation;
  SMTP2GO verification is similarly quick (Cloudflare warns "up to an hour" but it's usually minutes).
- **Cloudflare token sanity:** a real API token is ~40 chars; a 32-char hex string is a Zone/Account
  ID. Verify via `/client/v4/user/tokens/verify` before use.
