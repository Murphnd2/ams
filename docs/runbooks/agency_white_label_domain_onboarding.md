# Agency White-Label Custom Domain — Onboarding Runbook

**Status:** v2 (2026-07-15). Reconciled with the agency-facing template
`docs/runbooks/agency_dns_setup_onepager.md`; landing-HTML sanitizer behavior verified from
`LandingSafe.java`. All onboarding items resolved.

This is the **SSA-internal** procedure. The **agency-facing** DNS instructions (what you send the
agency's IT/DNS admin) are the companion one-pager: `docs/runbooks/agency_dns_setup_onepager.md`.

**Related:** `docs/infrastructure/cloudflare_setup.md` · `docs/infrastructure/production_architecture.md` ·
`docs/analysis/email_identity_current_state.md` · migrations `V068__agency_landing_host.sql`,
`V069__agency_email_sending.sql`.

---

## What this achieves

Point an agency's own **subdomain** (e.g. `admin.premiumpath.net`) at AMS so that:
- **Web:** the branded client portal / landing page (V068 `landing_host` / `landing_html`), PSP chrome suppressed.
- **Email:** the agency's agents send outbound mail `From:` the agency subdomain (V069 `email_domain` / `email_verified`, Tier 1 of `EmailIdentityResolver`), SPF/DKIM-aligned so it doesn't land in spam.

**No code or migration is required to onboard a new agency domain** — it is pure Cloudflare + SMTP2GO + DNS configuration, plus filling the Agency Manager fields. The application support shipped in V068/V069.

**First domain onboarded:** `premiumpath.net` (SWBD / PremiumPath).

---

## The agency only adds 3 DNS records (all DNS-only / gray-cloud)

Everything the agency does is on the one-pager. Summary:

| # | Record | Host | Target | Purpose |
|---|---|---|---|---|
| 1 | CNAME | `admin` | SSA-provided portal target (the Cloudflare-for-SaaS Fallback Origin, `origin.superiorstate.net`) | Branded portal |
| 2a | CNAME | `em<id>.admin` | SMTP2GO-provided | SPF return-path (VERP alignment) |
| 2b | CNAME | `s<id>._domainkey.admin` | SMTP2GO-provided | DKIM signature |
| 3 | TXT (optional) | `_dmarc` | `v=DMARC1; p=none; rua=…` | Only if the domain has no DMARC yet |

There are **no `_acme-challenge` / DCV records for the agency to add** — SSA handles the certificate (below). All records are **DNS-only** (mail + custom-hostname validation both break if proxied).

---

## Part A — Web landing (Cloudflare for SaaS Custom Hostname)

Served via a **Custom Hostname** on the `superiorstate.net` zone, routed to the shared **Fallback Origin**.

1. **One-time (already set): Fallback Origin.** `superiorstate.net` → SSL/TLS → Custom Hostnames → Fallback Origin = **`origin.superiorstate.net`** (A record → production VPS `66.179.248.171`, Cloudflare-only ingress). All agency custom hostnames route through this.
2. **SSL/TLS mode = Full (not strict) on `superiorstate.net`.** A custom hostname reaches the origin with the *agency* subdomain as SNI; the origin LE cert only covers `superiorstate.net` + `www`, so **Full (strict) → error 526**. Full (not strict) is required (Custom Origin SNI, the strict-preserving fix, is Enterprise-only — API error 1456). See `cloudflare_setup.md` → SSL/TLS Mode. **Do not "fix" this back to strict.**
3. **Add the Custom Hostname** for the agency subdomain (e.g. `admin.premiumpath.net`): `superiorstate.net` → SSL/TLS → Custom Hostnames → Add.
4. **Give the agency the portal CNAME target** to publish (record 1 above): `admin` → the SSA-provided target. Once that CNAME resolves, **Cloudflare for SaaS issues & renews the edge certificate automatically** (HTTP DCV — no agency-side `_acme-challenge` record). If a specific hostname ever needs DCV delegation, SSA supplies that one record; the standard flow does not.
5. **Wait for the custom hostname to show "Active"** (edge cert issued).
6. **Set the Agency Manager fields** (Part C): Landing Host + Landing HTML.

---

## Part B — Email sending (SMTP2GO)

Authoritative steps: `cloudflare_setup.md` → "Per-Agency White-Label Email Sending (V069)". In brief:

1. **Add the agency sending subdomain** (e.g. `admin.premiumpath.net`) in SMTP2GO (Sender Domains → Add).
2. SMTP2GO issues **2 CNAMEs** — give these to the agency as records 2a/2b:
   - **SPF return-path:** `em<id>.admin` → SMTP2GO target (VERP return-path alignment).
   - **DKIM:** `s<id>._domainkey.admin` → SMTP2GO DKIM target. (SMTP2GO occasionally issues a second DKIM record — pass along whatever it shows.)
   - Both **DNS-only**.
3. **Wait for SMTP2GO to report the domain "Verified."**
4. **Only then flip `email_verified`** in Agency Manager. Strict gate — enabling early sends `From:` an unaligned domain → DMARC failure / spam. The Tier-2 fallback (`From: notifications@superiorstate.net`, `Reply-To:` the agent's real address) is safe and deliverable until verification.

---

## Part C — AMS Agency Manager fields (Edit Agency)

PSP-admin only. No code/migration — per-agency config:

| Field | Migration | Purpose |
|---|---|---|
| **Landing Host** | V068 `landing_host` | The agency subdomain that serves the branded landing (must be unique). |
| **Landing HTML** | V068 `landing_html` | The branded landing markup (sanitized by `LandingSafe` on save). |
| **Sending Domain** | V069 `email_domain` | The agency sending subdomain. Blank → falls back to `SMTP_FROM`. |
| **Verified** | V069 `email_verified` | Manual gate — flip **only after** SMTP2GO shows Verified. |

---

## Verification checklist

- [ ] Custom hostname shows **Active** in Cloudflare (edge cert issued).
- [ ] Browsing `admin.‹agency-domain›` loads the branded landing (PSP chrome suppressed).
- [ ] SMTP2GO shows the sending domain **Verified**.
- [ ] A test send from an agency agent arrives `From: <agent>@admin.‹agency-domain›`, passes SPF/DKIM/DMARC (check headers).
- [ ] `email_verified` was flipped **after** SMTP2GO verification, not before.

---

## Landing HTML sanitizer (verified from `LandingSafe.java`, V068)

`LandingSafe` (`data/util/LandingSafe.java`) cleans landing HTML on save with a hardened Jsoup
`Safelist.relaxed()` plus structural tags (`style`, `link`, `section`, `header`, `footer`, `nav`,
`main`, `article`, `aside`, `figure`, `figcaption`, `button`, `span`, `div`, …), `class`/`id`/inline
`style` on all elements, `https` font `<link>`s, and `data:` images.

- **Inline `<svg>` is NOT permitted** — it is not in the safelist, so Jsoup strips it on save. To
  include SVG graphics in a landing page, embed them as a **`data:` URI image**:
  `<img src="data:image/svg+xml,…">` (img + `data:` protocol are allowed). This is what the project
  memory's "landing-page SVG sanitizer" note refers to — SVG goes through a data-URI `<img>`, not inline.
- **Blocked:** `<script>` / `<iframe>` / `<object>` / `<embed>` / `<form>`, `on*` handlers, and
  dangerous CSS (`expression()`, `javascript:`/`vbscript:` URLs, non-https `@import`, `behavior`,
  `-moz-binding`) — removed from `<style>` blocks and inline `style` alike.

*(All DNS/Cloudflare/SMTP2GO items are resolved against the agency one-pager: portal is a `admin.`
subdomain via CNAME, no agency `_acme-challenge` records, 2 email CNAMEs + optional DMARC, origin
verified Cloudflare-only.)*
