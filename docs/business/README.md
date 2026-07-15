# AMS Opportunity Register

Business relationships and opportunities that **influence the AMS roadmap** or are **solved by AMS**.
Each entry links to the concrete AMS work it drives, so opportunity size can be weighed against build
cost when prioritizing.

## Register

| Opportunity | Type | Status | Size / stakes | AMS work it drives |
|---|---|---|---|---|
| [DataPath](datapath.md) | Platform partnership | Active | Ecosystem-wide licensing (AMS across DataPath's PSP network) | Multi-PSP / distributed architecture, BPO federation, managed installations (V054), super-user dashboard |
| [SWBD / PremiumPath](swbd_premiumpath.md) | Agency/GA demand + benefits-admin partnership | **Active** — proposal delivered 2026-07-15; pilot-first | Small-employer program + 2 streams (TPA ICHRA under-25/50; <10-life groups w/ rev-share) | White-label delivery (V068–V071, markup V066/V067, GA hierarchy V070/V071, #39) + **PremiumPath Program**: QSEHRA-Lite admin, post-tax PremiumPath Card, list billing/remittance, rev-share markup, HSA+; future: carrier quoting (Sherpa) + QSEHRA stack-wrap |

## Types

- **Platform partnership** — AMS becomes a distributed / licensed platform *for* the partner
  (DataPath). Drives multi-tenancy, federation, installation management.
- **Agency / GA demand** — a customer / general agency whose needs justify **custom AMS work** when
  the opportunity is large enough (SWBD). Drives feature customization.

*(Add new types as they appear: integration partner, channel / referral, etc.)*

## Conventions

- One file per opportunity (`<name>.md`): summary · people · status/history · **AMS work this drives**
  (link backlog #, migrations, features) · open questions.
- Keep the **"AMS work it drives"** column current — it's what turns an opportunity into roadmap input.
- Sensitive CRM detail may also live in per-machine memory; this register is the version-controlled,
  roadmap-facing view.
