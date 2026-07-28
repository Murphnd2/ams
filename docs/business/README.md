# AMS Opportunity Register

Business relationships and opportunities that **influence the AMS roadmap** or are **solved by AMS**.
Each entry links to the concrete AMS work it drives, so opportunity size can be weighed against build
cost when prioritizing.

## Register

| Opportunity | Type | Status | Size / stakes | AMS work it drives |
|---|---|---|---|---|
| [DataPath](datapath.md) | Platform partnership | Active | Ecosystem-wide licensing (AMS across DataPath's PSP network) | Multi-PSP / distributed architecture, BPO federation, managed installations (V054), super-user dashboard |
| [SWBD / PremiumPath](swbd_premiumpath.md) | Agency/GA demand + benefits-admin partnership | **Active** — proposal delivered 2026-07-15; pilot-first | Small-employer program + 2 streams (TPA ICHRA under-25/50; <10-life groups w/ rev-share) | White-label delivery (V068–V071, markup V066/V067, GA hierarchy V070/V071, #39) + **PremiumPath Program**: QSEHRA-Lite admin (Summit-administered; AMS role = attestation-gathering, ties #38), post-tax PremiumPath Card (**DataPath Summit config in progress**, not an AMS build), list billing/remittance, rev-share markup, HSA+; wrapper/stack-wrap = leading hypothesis, gated on Presidio filed form; carrier quoting via HealthSherpa EDE integration — under eval (see healthsherpa.md) |
| [HealthSherpa](healthsherpa.md) | Integration partner | **Evaluation** | ICHRA quote+enroll rails (free CMS-approved EDE API); could make AMS a quote-to-admin ICHRA platform | ICHRA quoting/enrollment integration (QuoteConnect / enrollment deeplink / Policy Status / webhooks); **Policy Status API → #38 attestation unblock**; read-only eval first (backlog #42) |

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
