# AMS Opportunity Register

Business relationships and opportunities that **influence the AMS roadmap** or are **solved by AMS**.
Each entry links to the concrete AMS work it drives, so opportunity size can be weighed against build
cost when prioritizing.

## Register

| Opportunity | Type | Status | Size / stakes | AMS work it drives |
|---|---|---|---|---|
| [DataPath](datapath.md) | Platform partnership | Active | Ecosystem-wide licensing (AMS across DataPath's PSP network) | Multi-PSP / distributed architecture, BPO federation, managed installations (V054), super-user dashboard |
| [SWBD / PremiumPath](swbd_premiumpath.md) | Agency/GA demand + benefits-admin partnership | **Active** — proposal delivered 2026-07-15; pilot-first | Small-employer program + 2 streams (TPA ICHRA under-25/50; <10-life groups w/ rev-share) | White-label delivery (V068–V071, markup V066/V067, GA hierarchy V070/V071, #39) + **PremiumPath Program**: QSEHRA-Lite admin (Summit-administered; AMS role = attestation-gathering, ties #38), post-tax PremiumPath Card (**DataPath Summit config in progress**, not an AMS build), list billing/remittance, rev-share markup, HSA+; wrapper/stack-wrap = leading hypothesis, gated on Presidio filed form; carrier quoting via HealthSherpa EDE integration — under eval (see healthsherpa.md); ICHRA+/QSEHRA+ tier design complete — see plus_tier.md |
| [HealthSherpa](healthsherpa.md) | Integration partner | **Evaluation — quoting + off-ex enrollability proven** | ICHRA quote+enroll rails (free CMS-approved EDE API, no licensing cost); could make AMS a quote-to-admin ICHRA platform and displace zizzl for SWBD's book | ICHRA quoting + **off-exchange direct enrollment lifecycle** (`POST /v1/quotes`; `POST /v1/enrollments` plus submit / cancel / terminate / payment-redirect / SEP-doc upload) and on-exchange deeplinks (`POST /v1/enrollment-sessions`). **#38 attestation unblock is off-exchange `GET /v1/enrollments`** — employer-scoped via `employer_external_id` + `updated_since` polling; the on-exchange `/v1/policy-status/*` endpoints are agent-scoped and **alpha**, so status coverage is not uniform across exchanges. Webhooks are **unconfirmed** (absent from both the OpenAPI contract and the current docs) — do not plan around them. Carrier payment is a browser form POST, so enrollment **cannot be fully headless**. PHI in the enrollment payload makes a **BAA mandatory** (counterparty: Geozoning, Inc. DBA HealthSherpa). Eval: quoting + `api_enrollable` **DONE 2026-07-28**; remaining step is a portal access request, with the **AOR / TPA account model** the critical open question. Backlog #42; see `healthsherpa.md` |
| [ICHRA+/QSEHRA+](plus_tier.md) | Product | **Design complete, not built** | New LOS bundling HealthSherpa services + automated coverage verification | Rating-area cache, quote snapshot, census staging, participant verification ledger, notice obligations, per-employee questionnaire attachment, "+" pricing proposal section |

## Types

- **Platform partnership** — AMS becomes a distributed / licensed platform *for* the partner
  (DataPath). Drives multi-tenancy, federation, installation management.
- **Agency / GA demand** — a customer / general agency whose needs justify **custom AMS work** when
  the opportunity is large enough (SWBD). Drives feature customization.
- **Integration partner** — external infrastructure AMS integrates *with* (e.g. HealthSherpa EDE rails
  for ICHRA quote/enroll). Drives API-integration work, not platform/multi-tenancy work.

*(Add new types as they appear: integration partner, channel / referral, etc.)*

## Conventions

- One file per opportunity (`<name>.md`): summary · people · status/history · **AMS work this drives**
  (link backlog #, migrations, features) · open questions.
- Keep the **"AMS work it drives"** column current — it's what turns an opportunity into roadmap input.
- Sensitive CRM detail may also live in per-machine memory; this register is the version-controlled,
  roadmap-facing view.
