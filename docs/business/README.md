# AMS Opportunity Register

Business relationships and opportunities that **influence the AMS roadmap** or are **solved by AMS**.
Each entry links to the concrete AMS work it drives, so opportunity size can be weighed against build
cost when prioritizing.

## Register

| Opportunity | Type | Status | Size / stakes | AMS work it drives |
|---|---|---|---|---|
| [DataPath](datapath.md) | Platform partnership | Active | Ecosystem-wide licensing (AMS across DataPath's PSP network) | Multi-PSP / distributed architecture, BPO federation, managed installations (V054), super-user dashboard |
| [SWBD / PremiumPath](swbd_premiumpath.md) | Agency/GA demand + benefits-admin partnership | **Active** — proposal delivered 2026-07-15; pilot-first | Small-employer program + 2 streams (TPA ICHRA under-25/50; <10-life groups w/ rev-share) | White-label delivery (V068–V071, markup V066/V067, GA hierarchy V070/V071, #39) + **PremiumPath Program**: QSEHRA-Lite admin (Summit-administered; AMS role = attestation-gathering, ties #38), post-tax PremiumPath Card (**DataPath Summit config in progress**, not an AMS build), list billing/remittance, rev-share markup, HSA+; wrapper/stack-wrap = leading hypothesis, gated on Presidio filed form; carrier quoting via the HealthSherpa off-exchange ICHRA Partner API — under eval (see healthsherpa.md); ICHRA+/QSEHRA+ tier design complete — see plus_tier.md |
| [HealthSherpa](healthsherpa.md) | Integration partner | **Evaluation — quoting + off-ex enrollability proven** | ICHRA quote+enroll rails (free off-exchange ICHRA Partner API, no licensing cost); could make AMS a quote-to-admin ICHRA platform and displace zizzl for SWBD's book | ICHRA quoting + **off-exchange direct enrollment lifecycle** (`POST /v1/quotes`; `POST /v1/enrollments` plus submit / cancel / terminate / payment-redirect / SEP-doc upload) and on-exchange deeplinks (`POST /v1/enrollment-sessions`). **#38 attestation unblock is off-exchange `GET /v1/enrollments`** — employer-scoped via `employer_external_id` + `updated_since` polling; the on-exchange `/v1/policy-status/*` endpoints are agent-scoped and **alpha**, so status coverage is not uniform across exchanges. Webhooks are **confirmed** — Submission Confirmation and Policy Status, sharing a schema, carrying `paid_through_date` and `grace_period_start_date`. Carrier payment is a browser form POST, so enrollment **cannot be fully headless**. PHI in the enrollment payload makes a **BAA mandatory** (counterparty: Geozoning, Inc. DBA HealthSherpa). Eval: quoting + `api_enrollable` **DONE 2026-07-28**; remaining step is a portal access request, with the **AOR / TPA account model** the critical open question. Backlog #42; see `healthsherpa.md` |
| [ICHRA+/QSEHRA+](plus_tier.md) | Product | **Design complete, not built** | New LOS bundling HealthSherpa services + automated coverage verification | Rating-area cache, quote snapshot, census staging, participant verification ledger, notice obligations, per-employee questionnaire attachment, "+" pricing proposal section |
| [ICHRA+/QSEHRA+ build plan](../analysis/plus_tier_build_plan.md) | Planning doc | **Rev 5 — canonical** | Decisions D1–D37, open items O1–O38, phased build list, prioritised question list | Supersedes `plus_tier.md`'s original open-item list; governs sequencing of #43 |
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
