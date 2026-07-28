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

## Eval results (2026-07-28) — quoting proven at rate-parity with zizzl

Ran a read-only quoting test via **HealthSherpa One** (the free self-serve developer tier) against the
Sandoval group's market (Hopkins County, TX — ZIP 75482, FIPS 48223, age 40, plan year 2026).

**Corrected API facts (the docs.ichra.* partner endpoint is a different, gated product):**
- Base URL: **`https://api.one.healthsherpa.com`** · auth header `x-api-key`.
- Quote endpoint: **`POST /v1/quotes`** with a **nested** body: `context` (product `aca`, `exchange`
  = `on_exchange` | `off_exchange`, coverage_family/type `medical`, plan_year), `location`
  (`zip_code`, `fips_code`, `state`), `household` (`household_size`, `effective_date`, `applicants[]`
  with `member_id`, `age`, `relationship`, `uses_tobacco`), `sort`, `page`. One request per exchange.
  Note field names: `fips_code` (not `fip_code`), `uses_tobacco` (not `smoker`); premiums come back as
  **strings** under `pricing.gross_premium`; metal under `details.metal_level`; carrier under `issuer.name`.
- Useful reference endpoints: `GET /v1/ping` (key check), `GET /v1/reference/counties?zip_code=`,
  `GET /v1/reference/issuers?state=TX&plan_year=2026`.

**Findings:**
- **Forrest's gated carriers are freely quotable.** The TX issuer list and the Hopkins quote both return
  **CHRISTUS Health Plan**, **Ambetter (Superior HealthPlan)**, and **Blue Cross and Blue Shield of TX** —
  i.e. exactly the carriers zizzl gated ("turned off Christus"), with no vendor gate, on the free tier.
- **Rate parity confirmed to the penny.** The zizzl CSA baseline — **BCBS Blue Advantage Silver HMO 306
  @ $582.78** — is an **off-exchange** plan (it does not appear on-exchange for rating area 20), and the
  off-exchange quote returned it at **exactly $582.78**. HealthSherpa is quoting the same source-of-truth
  rate data zizzl was.
- **Both exchanges quote freely** on the self-serve tier (on-ex: Christus/Ambetter/BCBS present; off-ex:
  65 plans incl. the 306 baseline). Full plan detail returns (deductible, MOOP, SBC/formulary/brochure
  URLs) — enough to assemble a CSA.
- **Name-matching caveat:** BCBS plan names carry a `℠` service-mark glyph (`HMO℠ 306`), not `HMO SM 306`
  — match on plan numbers, not the "SM".

**Verdict:** At the **quoting** layer, HealthSherpa fully replaces what zizzl gated — Forrest's carriers,
both exchanges, matching rates, free and un-gated. Open items are unchanged and both are the
**rep conversation, not code**: (1) off-exchange **enrollment** approval — plans carry `api_enrollable`
(the sample Christus plan was `false`), and direct off-ex enrollment is approval-gated; (2) **AOR/BAA** —
agent-of-record is derived **server-side from the linked HealthSherpa agent account** (`agent_of_record`,
`_agent_id`, `tpa_slug` are all rejected if caller-supplied), so how SWBD's agents map to that account is
the attribution question to settle.

## Recommended next step (before any build)

Read-only evaluation, **no AMS integration commitment yet**:
1. ~~Quote sanity-test~~ **DONE 2026-07-28** — quoting proven at rate-parity (see Eval results above).
2. Onboarding-rep conversation: **AOR attribution**, **BAA / PHI**, off-exchange carrier network,
   production-access requirements.
3. Only then scope an AMS integration (a Phase-A read-only investigation).

## Related

- `docs/business/swbd_premiumpath.md` — the SWBD ICHRA-admin thread that surfaced this.
- `docs/analysis/project_backlog.md` #42 — HealthSherpa integration evaluation.
- Backlog **#38** — QSEHRA/ICHRA attestation engine (Policy Status API is a candidate unblock).
- `docs/business/datapath.md` — parallel integration-partner pattern.
