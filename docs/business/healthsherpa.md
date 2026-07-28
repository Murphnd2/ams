# HealthSherpa — ICHRA quote/enroll integration partner

**Type:** Integration partner (infrastructure / EDE rails) · **Status:** Evaluation (not yet a project)
**Surfaced:** 2026-07-28, via the SWBD ICHRA-admin thread (zizzl / Sandoval — see `swbd_premiumpath.md`).

## Summary

HealthSherpa is a CMS-approved Enhanced Direct Enrollment (EDE) provider and the leading **connectivity
layer** for the ICHRA market — API-first infrastructure connecting carriers with ICHRA platforms for
quoting, enrollment, and compliance. It powers 40+ ICHRA platforms behind the scenes and is integrated by
admin platforms (e.g. Alegeus/WealthCare) as the shop-and-enroll layer under their ICHRA administration.

Crucially for AMS: **HealthSherpa is a supplier/enabler, not a competing administrator.** The fit is —
**SSA is the ICHRA admin platform; HealthSherpa is the carrier-integrated quote-and-enroll rail underneath.**

## Why it matters to AMS

Surfaced from the SWBD ICHRA-admin unbundle: SWBD had been quoting ICHRA through **zizzl health**, which
gated carriers (turned off Christus, wouldn't put Forrest's carriers on the quote) and charged a punishing
small-group admin fee (~$660/mo minimum on a 3-employee group). Forrest's logic: if he's doing the carrier
legwork anyway, he'd rather give the *admin* to someone he trusts (SSA). HealthSherpa closes the gap that
opened — carrier-integrated quote+enroll as **open API rails** instead of a gated vendor — which would let
**AMS be a quote-to-admin ICHRA platform** (not just admin), fully displacing zizzl for SWBD's book.

## The Connect API suite ("ICHRA: Powered by HealthSherpa")

- **QuoteConnect / Quoting API** — on- and off-exchange ACA plan data + premiums, all 50 states (FFM +
  SBM), with APTC/subsidy estimation. Produces the plan/rate data behind a CSA. Filters by metal level,
  network, issuer, etc. (On-exchange quoting is free/self-serve via **HealthSherpa One**,
  `one.healthsherpa.com` — instant API key.)
- **Enrollment Deeplink (EnrollConnect)** — prefills an enrollment application and returns a redirect URL
  into HealthSherpa's EDE enrollment flow. Carrier-integrated. Agent attribution via `_agent_id`
  (passing a valid `_agent_id` with an API key auto-whitelists it).
- **Policy Status API** (launched Aug 2025) — real-time effectuation / cancellation / termination
  monitoring. Explicitly eliminates the need for monthly employee attestations → direct candidate to
  unblock backlog **#38** (QSEHRA/ICHRA attestation engine), instead of waiting on the Summit/COMPASS feed.
- **Webhooks** — Submission Confirmation + Policy Status, real-time.
- **Auth / mechanics** — `x-api-key`; staging + production; `_agent_id` allow-listing for production; all
  calls backend-only (keys private; avoid CORS / auto-redirect-follow pitfalls).

Docs: `docs.ichra.healthsherpa.com` (ICHRA Partner API), `one.healthsherpa.com` (self-serve quote API),
`info.healthsherpa.com/ichra` (overview).

## Cost / access model

- On-exchange **quoting API is free and self-serve** (instant key). Fuller enrollment/status capabilities
  are "available upon review" and may require additional permissions for compliance reasons (onboard with
  a rep → staging creds → production allow-listing).
- The Connect suite is marketed **at no cost** — HealthSherpa monetizes on the carrier/enrollment side as
  the EDE, not by charging platforms for the API.
- **SSA's cost is integration engineering, not licensing** — a very different picture than Ideon-style
  rate-data licensing or standing up an EDE.

## Benefits

- Solves Forrest's actual problem: carrier-integrated quote+enroll **without** a vendor gating which
  carriers he can put in front of clients.
- Lets SSA credibly be a **quote-to-admin ICHRA platform** — the thing that fully displaces zizzl.
- Free API access; **HealthSherpa carries the CMS/EDE audit burden**, not SSA (SSA integrates the EDE, it
  doesn't become one).
- **Policy Status API doubles as the attestation solution** (#38).
- Market leader (millions enrolled, 40+ platforms, Alegeus partnership) — a defensible dependency.

## Costs / watch-items / open questions

- **This IS the "shopping/enrollment layer" the SWBD brief said don't build** — but that guidance assumed
  building from scratch (Ideon data + EDE audit + steering risk). Integrating an existing free EDE API is a
  different calculus. Still: revisit the steering/endorsement posture so it's the *agent's/platform's*
  enrollment rail, not SSA steering.
- **PHI / HIPAA** — ICHRA is a group health plan; this integration touches PHI → need a **BAA with
  HealthSherpa** + PHI-safe handling (consistent with the Bedrock rule, `domain_and_compliance_rules.md` §3).
- **Agent-of-record / commission attribution** — confirm the `_agent_id` model keeps SWBD's agents as AOR
  and routes no commission to HealthSherpa.
- **Off-exchange carrier coverage** — confirm HealthSherpa's off-exchange network covers the carriers/
  markets SWBD sells (e.g. Christus / Ambetter in TX).
- **Engineering scope** — backend quote calls, deeplink redirect handling, webhook receiver, PHI storage.
  Medium build; well-documented, API-first.
- **Dependency** on HealthSherpa as rails (like the Summit dependency) — a conscious strategic bet.

## Recommended next step (before any build)

Read-only evaluation, **no AMS integration commitment yet**:
1. Grab the free QuoteConnect key; sanity-test plans/rates for a TX county like **Hopkins** (Sandoval's) —
   confirm sane output and off-exchange carrier coverage.
2. Onboarding-rep conversation: **AOR attribution**, **BAA / PHI**, off-exchange carrier network,
   production-access requirements.
3. Only then scope an AMS integration (a Phase-A read-only investigation).

## Related

- `docs/business/swbd_premiumpath.md` — the SWBD ICHRA-admin thread that surfaced this.
- `docs/analysis/project_backlog.md` #42 — HealthSherpa integration evaluation.
- Backlog **#38** — QSEHRA/ICHRA attestation engine (Policy Status API is a candidate unblock).
- `docs/business/datapath.md` — parallel integration-partner pattern.
