# AMS Opportunity Register

Business relationships and opportunities that **influence the AMS roadmap** or are **solved by AMS**.
Each entry links to the concrete AMS work it drives, so opportunity size can be weighed against build
cost when prioritizing.

> **ICHRA / HealthSherpa work starts at [`docs/ichra_strategy.md`](../ichra_strategy.md)**, not here — it is the strategy entry point, and it flags the rows below that are known stale.

## Register

| Opportunity | Type | Status | Size / stakes | AMS work it drives |
|---|---|---|---|---|
| [DataPath](datapath.md) | Platform partnership | Active | Ecosystem-wide licensing (AMS across DataPath's PSP network) | Multi-PSP / distributed architecture, BPO federation, managed installations (V054), super-user dashboard |
| [SWBD / PremiumPath](swbd_premiumpath.md) | Agency/GA demand + benefits-admin partnership | **Active** — proposal delivered 2026-07-15; pilot-first | Small-employer program + 2 streams (TPA ICHRA under-25/50; <10-life groups w/ rev-share) | White-label delivery (V068–V071, markup V066/V067, GA hierarchy V070/V071, #39) + **PremiumPath Program**: QSEHRA-Lite admin (Summit-administered; AMS role = attestation-gathering, ties #38), post-tax PremiumPath Card (**DataPath Summit config in progress**, not an AMS build), list billing/remittance, rev-share markup, HSA+; wrapper/stack-wrap = leading hypothesis, gated on Presidio filed form; carrier quoting via the HealthSherpa off-exchange ICHRA Partner API — under eval (see healthsherpa.md); ICHRA+/QSEHRA+ tier design complete — see plus_tier.md |
| [HealthSherpa](healthsherpa.md) | Integration partner | **Evaluation — quoting proven at rate parity; full enrollment/status surface documented (O2 closed 2026-08-04)** | ICHRA quote+enroll rails (free ICHRA Partner API, no licensing cost); could make AMS a quote-to-admin ICHRA platform and displace zizzl for SWBD's book | ⚠️ **This row was rewritten 2026-08-04 — the previous version described HSOne paths, which do not describe the target product.** The product is the **ICHRA Partner API** (`docs.ichra.healthsherpa.com`), not HSOne and not EDE. Quoting `POST /api/v1/quotes` (verified live, rate parity to the cent); `POST /api/v1/aptc_estimates`; `GET /api/v1/plans` (state-level catalog, built for caching — backlog T146); `GET /api/v1/plans/{hios_id}`. **Enrollment is a documented three-way route** on flags already present in every quote response: `api_enrollment` → **EnrollConnect** (`POST /api/v1/applications` draft/update/submit/cancel/terminate + ACH payment method + payment redirect + SEP-doc upload, live since 2026-04-13); else `deeplink_enrollment` → **Application Deeplink** (`POST /public/ichra/off_ex`, 302 to a HealthSherpa-hosted flow); else quote-only. **Both accept the same canonical request schema.** ⭐ **`plan_hios_id` is required on every enrollment route** — there is no hand-off to a HealthSherpa shopping experience, so the **ERISA neutral-presentation burden sits with SSA** (gates O20). Webhooks **confirmed** — Submission Confirmation and Policy Status, sharing a schema, carrying `paid_through_date` and `grace_period_start_date`; setup process is public, **delivery semantics (auth, retry, ordering, idempotency, signature verification) are not** (O15). Payment is **carrier-dependent, not universally browser-bound** — ACH can be set server-side. PHI in the enrollment payload makes a **BAA mandatory** (counterparty: Geozoning, Inc. DBA HealthSherpa). **The AOR / TPA account model is RESOLVED (2026-07-29), not open** — AOR travels per application keyed on NPN. Remaining external gates: production allow-listing (T136), staging deeplink Basic Auth, webhook delivery semantics, the BAA, and BCBS TX / CHRISTUS policy-status timing. Backlog #42, T144–T149; see `healthsherpa.md`, **2026-08-04 section first** |
| [ICHRA+/QSEHRA+](plus_tier.md) | Product | **Design complete, not built** | New LOS bundling HealthSherpa services + automated coverage verification | Rating-area cache, quote snapshot, census staging, participant verification ledger, notice obligations, per-employee questionnaire attachment, "+" pricing proposal section |
| [ICHRA+/QSEHRA+ build plan](../analysis/plus_tier_build_plan.md) | Planning doc | **Revision 6 — canonical; Part 8 governs** | Decisions D1–D39, open items O1–O40, phased build list, prioritised question list. **Later Parts govern earlier ones** | Supersedes `plus_tier.md`'s original open-item list; governs sequencing of #43. *(Row corrected 2026-08-04 — previously read "Rev 5" and "O1–O38".)* |
| [Summit notice automation discovery](../analysis/summit_notice_automation_discovery.md) | Planning doc | **Test protocol — not yet run** | Import-driven notice/status-change automation for the "+" tier notice engine | Feeds `notice_event_map` design and B4a (notice obligation register) |

## Types

- **Platform partnership** — AMS becomes a distributed / licensed platform *for* the partner
  (DataPath). Drives multi-tenancy, federation, installation management.
- **Agency / GA demand** — a customer / general agency whose needs justify **custom AMS work** when
  the opportunity is large enough (SWBD). Drives feature customization.
- **Integration partner** — external infrastructure AMS integrates *with* (e.g. the HealthSherpa
  off-exchange ICHRA Partner API for ICHRA quote/enroll). Drives API-integration work, not
  platform/multi-tenancy work.

*(Add new types as they appear: integration partner, channel / referral, etc.)*

## Conventions

- One file per opportunity (`<name>.md`): summary · people · status/history · **AMS work this drives**
  (link backlog #, migrations, features) · open questions.
- Keep the **"AMS work it drives"** column current — it's what turns an opportunity into roadmap input.
- Sensitive CRM detail may also live in per-machine memory; this register is the version-controlled,
  roadmap-facing view.
