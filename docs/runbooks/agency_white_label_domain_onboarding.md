# Agency White-Label Custom Domain — Onboarding Runbook

**Status:** v1 (2026-07-15). Assembled from project memory + existing infra docs
(`cloudflare_setup.md`, `production_architecture.md`) and the V068/V069 migrations.
Items marked **⚠️ CONFIRM** need verification against the project-only
`AGENCY_DNS_SETUP_ONEPAGER.md` before this is treated as authoritative.

**Related:** `docs/infrastructure/cloudflare_setup.md` · `docs/infrastructure/production_architecture.md` ·
`docs/analysis/email_identity_current_state.md` · migrations `V068__agency_landing_host.sql`,
`V069__agency_email_sending.sql`.

---

## What this achieves

Point an agency's own domain (e.g. `premiumpath.net`) at AMS so that:
- **Web:** visitors to the agency host see the agency's branded landing page (V068 `landing_host` / `landing_html`), PSP chrome suppressed.
- **Email:** the agency's agents send outbound mail `From:` the agency's own (sub)domain (V069 `email_domain` / `email_verified`, Tier 1 of `EmailIdentityResolver`), SPF/DKIM-aligned so it doesn't land in spam.

**No code or migration is required to onboard a new agency domain** — it is pure Cloudflare + SMTP2GO + DNS configuration, plus filling the Agency Manager fields. The application support already shipped in V068/V069.

**First domain onboarded:** `premiumpath.net` (SWBD / PremiumPath).

---

## Prerequisites

- The agency **owns the domain** and can publish DNS records on its zone.
- An AMS **Agency record** exists (Agency Manager) for this agency, not suppressed (V057).
- You have access to the Cloudflare dashboard (`superiorstate.net` zone) and the SMTP2GO account.

---

## Part A — Web landing (Cloudflare for SaaS Custom Hostname)

The agency host is served through a **Custom Hostname** on the `superiorstate.net` zone, routed to a shared **Fallback Origin**.

1. **One-time (already done): shared Fallback Origin.** `superiorstate.net` → SSL/TLS → Custom Hostnames → Fallback Origin = **`origin.superiorstate.net`** (an A record on our zone pointing at the production VPS, `66.179.248.171`). All agency custom hostnames route through this. ⚠️ CONFIRM exact Fallback Origin hostname/record.

2. **SSL/TLS mode must be Full (not strict) on `superiorstate.net`.** A custom hostname reaches the origin with the *agency* domain as SNI; the origin LE cert only covers `superiorstate.net` + `www`, so **Full (strict) → error 526**. Full (not strict) is required (Custom Origin SNI, the strict-preserving fix, is Enterprise-only — API error 1456). See `cloudflare_setup.md` → SSL/TLS Mode. **Do not "fix" this back to strict.**

3. **Add the Custom Hostname.** `superiorstate.net` → SSL/TLS → Custom Hostnames → Add. Enter the agency host (⚠️ CONFIRM apex `premiumpath.net` vs `www.` — memory indicates apex).

4. **Agency publishes on its own DNS:**
   - **Apex/host CNAME → `origin.superiorstate.net`** (the Fallback Origin). ⚠️ CONFIRM target.
   - **Two `_acme-challenge` TXT records** issued by Cloudflare's custom-hostname UI, for DCV (edge-cert issuance). ⚠️ CONFIRM exact record names/values come from the Cloudflare UI per hostname.

5. **Wait for the custom hostname to go "Active"** (Cloudflare validates DCV and issues the edge certificate).

6. **Landing HTML sanitizer.** Landing HTML saved in Agency Manager is sanitized by `LandingSafe` (Jsoup safelist; V068). ⚠️ CONFIRM the "landing-page SVG sanitizer fix" recorded in memory — what it changed (e.g. permitting inline `<svg>` while still blocking script vectors) — and whether it is committed.

7. **Set the Agency Manager fields** (Part C): Landing Host + Landing HTML.

---

## Part B — Email sending (SMTP2GO)

The SMTP2GO onboarding is already documented — see **`cloudflare_setup.md` → "Per-Agency White-Label Email Sending (V069)"** for the authoritative steps. In brief:

1. Add the agency **sending (sub)domain** in SMTP2GO (Sender Domains → Add).
2. Agency publishes the SMTP2GO-issued **DNS-only CNAMEs on its own zone** (⚠️ memory says **3 CNAMEs**; `cloudflare_setup.md` lists the SPF return-path `em<id>` CNAME + DKIM `s<id>._domainkey` CNAME(s) — CONFIRM the exact count/targets).
3. **Wait for SMTP2GO to report the domain "Verified."**
4. **Only then flip `email_verified`** in Agency Manager (strict gate — enabling early causes DMARC failure / spam; Tier-2 fallback `notifications@superiorstate.net` is safe until then).

---

## Part C — AMS Agency Manager fields (Edit Agency)

PSP-admin only. No code/migration — these are per-agency config:

| Field | Migration | Purpose |
|---|---|---|
| **Landing Host** | V068 `landing_host` | The agency host that serves the branded landing (must be unique). |
| **Landing HTML** | V068 `landing_html` | The branded landing markup (sanitized by `LandingSafe` on save). |
| **Sending Domain** | V069 `email_domain` | The agency sending (sub)domain. Blank → falls back to `SMTP_FROM`. |
| **Verified** | V069 `email_verified` | Manual gate — flip **only after** SMTP2GO shows Verified. |

---

## Verification checklist

- [ ] Custom hostname shows **Active** in Cloudflare (edge cert issued).
- [ ] Browsing the agency host loads the branded landing (PSP chrome suppressed).
- [ ] SMTP2GO shows the sending domain **Verified**.
- [ ] A test send from an agency agent arrives `From: <agent>@<agency-domain>`, passes SPF/DKIM/DMARC (check headers).
- [ ] `email_verified` was flipped **after** SMTP2GO verification, not before.

---

## ⚠️ Gaps to confirm from `AGENCY_DNS_SETUP_ONEPAGER.md`

1. Exact Fallback Origin record + whether custom hostname is apex or `www`.
2. Exact `_acme-challenge` TXT record names/values (per-hostname from Cloudflare UI).
3. The SMTP2GO CNAME count/targets (memory says 3).
4. The "landing-page SVG sanitizer fix" specifics — what changed and whether committed.
5. ~~Whether the origin is firewalled to Cloudflare-only (D-73)~~ — **Resolved 2026-07-15:** external probe confirms direct port-443 to the origin is blocked (Cloudflare-only ingress), so the Full-not-strict risk is minimal. Repo D-73 / `cloudflare_setup.md` corrected to match.
