# HealthSherpa — ICHRA quote/enroll integration partner

**Type:** Integration partner (infrastructure / EDE rails) · **Status:** Evaluation (not yet a project)
**Surfaced:** 2026-07-28, via the SWBD ICHRA-admin thread (zizzl / Sandoval — see `swbd_premiumpath.md`).
**Last updated:** 2026-07-31 (pagination truncation, response shape, and FIPS-lookup findings)

## Summary

HealthSherpa is a CMS-approved Enhanced Direct Enrollment (EDE) provider and the leading **connectivity
layer** for the ICHRA market — API-first infrastructure connecting carriers with ICHRA platforms for
quoting, enrollment, and compliance.

> **Note:** the product SSA is integrating is the **off-exchange ICHRA Partner API**
> (`docs.ichra.healthsherpa.com`), **not** EDE and **not** HSOne. See the 2026-07-29 product
> correction, the 2026-07-30 verified-request-shape section, and the 2026-07-31 section (which
> supersedes the 2026-07-28 response field map in full) before relying on any endpoint or field detail
> in this document — request/response field names above predate all three and describe HSOne, not
> this product.

It powers 40+ ICHRA platforms behind the scenes and is integrated by
admin platforms (e.g. Alegeus/WealthCare) as the shop-and-enroll layer under their ICHRA administration.

Crucially for AMS: **HealthSherpa is a supplier/enabler, not a competing administrator.** The fit is —
**SSA is the ICHRA admin platform; HealthSherpa is the carrier-integrated quote-and-enroll rail underneath.**

Legal entity (for paper — BAA, agreements): **Geozoning, Inc. DBA HealthSherpa.**

## Why it matters to AMS

Surfaced from the SWBD ICHRA-admin unbundle: SWBD had been quoting ICHRA through **zizzl health**, which
gated carriers (turned off Christus, wouldn't put Forrest's carriers on the quote) and charged a punishing
small-group admin fee (~$660/mo minimum on a 3-employee group). Forrest's logic: if he's doing the carrier
legwork anyway, he'd rather give the *admin* to someone he trusts (SSA). HealthSherpa closes the gap that
opened — carrier-integrated quote+enroll as **open API rails** instead of a gated vendor — which would let
**AMS be a quote-to-admin ICHRA platform** (not just admin), fully displacing zizzl for SWBD's book.

---

## Product model (CORRECTED 2026-07-28)

> **Supersedes** the earlier "the `docs.ichra.*` partner endpoint is a different, gated product" framing.
> That was wrong. Verified against `https://one.healthsherpa.com/openapi.json` and the developer docs.

There is **one API surface**: `https://api.one.healthsherpa.com`, one key, `x-api-key` on every call.
Quoting, on-exchange enrollment deeplinks, and off-exchange direct enrollment all live there. **The gate
is per-workflow account approval, not a different product, base URL, or tier.**

Onboarding docs: *"Public quoting APIs are available after signup. Enrollment workflows require a linked
HealthSherpa agent account and operator approval."*

### The two enrollment paths are genuinely different products in practice

| | **Off-exchange** | **On-exchange** |
|---|---|---|
| Enrollment mechanism | `POST /v1/enrollments` — full API lifecycle | `POST /v1/enrollment-sessions` — hosted deeplink |
| AMS control | Create / read / update / submit / cancel / terminate | Hand off to HealthSherpa-hosted browser flow |
| `product` accepted | `ichra` **only** | `aca` |
| AOR attribution | Server-derived from the approved account setup | OAuth-linked HealthSherpa Marketplace agent |
| Status monitoring | `GET /v1/enrollments` — **employer-scoped**, `updated_since` | `/v1/policy-status/*` — **agent-scoped**, *alpha* |
| Subsidy | None (net = gross) | APTC applies |
| Plans at Hopkins TX | 65 | 45 |

**Strategic read:** the off-exchange path is the strong one for SSA — full lifecycle control, ICHRA is its
*only* supported product, and status is employer-scoped (exactly what #38 needs). The on-exchange path is
weaker on every axis SSA cares about **except** per-agent AOR — which is the axis SWBD cares about most.
That tension is the single sharpest item for the HealthSherpa conversation.

## Endpoint inventory (from the OpenAPI contract, 2026-07-28)

**Open on signup:**
- `GET /v1/ping` — key/reachability check.
- `GET /v1/reference/counties?zip_code=` — ZIP → county + `fips_code` (a ZIP can cross state lines).
- `GET /v1/reference/issuers?state=XX&plan_year=` — statewide QHP issuers + HIOS issuer ID. **State code
  must be uppercase** or it returns `400 invalid_request`. Defaults to current plan year.
- `GET /v1/reference/providers?query=&zip_code=&exchange=` — provider/facility name search near a ZIP,
  returns NPI, specialty, address. Paginated via `page[number]` / `page[size]` (1–50, default 50).
  Search-only; no "list all" mode. Useful for a "is my doctor covered" step in a CSA.
- `POST /v1/quotes` — one request per exchange per coverage_type.

**Approval-gated:**
- `POST /v1/enrollment-sessions` — on-exchange deeplinks. `context.flow` = `agent_assisted` |
  `self_service`. Optional `Idempotency-Key`.
- `POST /v1/enrollments` — create off-ex ICHRA application. **`Idempotency-Key` header required.**
- `GET /v1/enrollments` — list; filters `employer_external_id`, `policy_status`, `plan_year`,
  `issuer_hios_id`, `plan_hios_id`, `external_id`, `updated_since`; `limit`/`offset` paging.
- `GET` / `PUT /v1/enrollments/{id}` — read / full-replacement update.
- `POST /v1/enrollments/{id}/submissions` — submit to carrier; `202 Accepted`, async, poll for outcome.
- `POST /v1/enrollments/{id}/cancellations` · `/terminations` — carrier-dependent; check capability flags
  (`supports_changes`, `can_change_plan`, `can_report_change`) on the read response first.
- `GET /v1/enrollments/{id}/payment_redirect` — carrier payment as a **browser form POST** with opaque
  fields that must be passed through verbatim. Enrollment therefore **cannot be fully headless.**
- `POST /v1/enrollments/{id}/supporting_documentation` — SEP proof upload (`document_type: "sep"`;
  PDF/JPEG/PNG, multipart or base64). HealthSherpa does not persist the file after processing.
- `GET /v1/policy-status/applications` and `/{confirmation_id}` — on-exchange Marketplace application +
  policy status for the OAuth-linked agent. **Alpha**: not every policy has status data yet.

> **Note:** the policy-status endpoints are documented on the site but **absent from `openapi.json`**.
> The OpenAPI file is not exhaustive — absence there is not evidence of absence in the product. The same
> caveat applies to webhooks (see Open questions).

---

## Eval results (2026-07-28) — quoting proven at rate-parity with zizzl

Read-only quoting tests against the Sandoval group's market (Hopkins County, TX — ZIP 75482, FIPS 48223,
age 40, plan year 2026, effective 2026-09-01).

### Confirmed request shape

`POST /v1/quotes`, nested body: `context` (`product` `aca`, `exchange` `on_exchange` | `off_exchange`,
`coverage_family`/`coverage_type` `medical`, `plan_year`), `location` (`zip_code`, `fips_code`, `state`),
`household` (`household_size`, `annual_income`, `effective_date`, `applicants[]` with `member_id`, `age`,
`relationship`, `uses_tobacco`; optional `date_of_birth`, `pregnant`, `blind_or_disabled`,
`native_american`), `sort`, `page`. **One request per exchange.**

Field-name traps: `fips_code` (not `fip_code`), `uses_tobacco` (not `smoker`).

### Confirmed response field map (live, 2026-07-28)

> ⚠️ **SUPERSEDED 2026-07-31 — this map is HSOne's, not this product's.** `plan_id` is actually
> `hios_id`; `gross_premium` is top level, not under `pricing`; `api_enrollable` does not exist.
> See the 2026-07-31 section at the end of this document.

- **Top level:** `plan_id`, `variant_id`, `external_plan_id`, `coverage_family`, `coverage_type`, `name`,
  `display_name`, **`api_enrollable`**, `context`, `issuer`, `network`, `pricing`, `documents`,
  `availability`, `details`, `release`
- **`pricing.*`:** `gross_premium`, `ehb_premium`, `max_aptc`, `subsidy_applied`, `net_premium`,
  `currency`, `billing_period`
- **`details.*`:** `type`, `metal_level`, `plan_type`, `hsa_eligible`, `deductible_individual`,
  `deductible_family`, `moop_individual`, `moop_family`, `csr_level`, `adult_dental`, `child_dental`,
  `is_standardized`, `primary_care_summary`, `specialist_summary`, `urgent_care_summary`,
  `generic_rx_summary`

`api_enrollable` is **top level**, not nested under `details` or `pricing`. `plan_id` carries the
14-character HIOS plan ID.

Two fields matter beyond the CSA: **`details.hsa_eligible`** feeds the HSA+ piece of the PremiumPath stack
(§125 payroll HSA for bronze enrollees) straight off the quote, and **`pricing.ehb_premium`** is the
EHB-only portion, relevant to HRA substantiation.

### `api_enrollable` — semantics settled

The flag means **"this plan can be enrolled through `POST /v1/enrollments`."** Since that endpoint accepts
only `product=ichra` + `exchange=off_exchange`, **every on-exchange plan is `false` by definition.** It is
*not* a carrier contract flag and *not* an account-permission flag.

Hopkins County, plan year 2026:

| Exchange | Issuer | Plans | `api_enrollable` |
|---|---|---|---|
| off_exchange | Blue Cross and Blue Shield of Texas | 24 | **True** |
| off_exchange | CHRISTUS Health Plan | 18 | **True** |
| off_exchange | UnitedHealthcare | 23 | False |
| off_exchange | **total** | **65** | **42 enrollable (65%)** |
| on_exchange | BCBS TX / CHRISTUS / UnitedHealthcare | 18 / 13 / 14 | **False (all 45)** |

**This corrects the prior note that "the sample Christus plan was `false`."** That observation came from an
on-exchange plan. Off-exchange, **all 18 CHRISTUS plans are enrollable** — including the carrier zizzl
specifically turned off.

Within off-exchange, **UnitedHealthcare is a genuine carrier-level gap** — 23 plans quotable but not
API-enrollable. The split is perfectly carrier-clean; no partial coverage inside an issuer.

### Baseline plan — rate parity to the penny, and enrollable

| Plan | HIOS | Gross | Net | `api_enrollable` | `hsa_eligible` |
|---|---|---|---|---|---|
| BCBS Blue Advantage Silver HMO℠ 306 | `33602TX0460776` | 582.78 | 582.78 | **True** | False |
| BCBS Blue Advantage Plus Silver℠ 306 | `33602TX0870212` | 633.50 | 633.50 | **True** | False |

The zizzl CSA baseline (**Blue Advantage Silver HMO 306 @ $582.78**) is **off-exchange only** (absent from
the on-exchange set), returned at **exactly $582.78**, and is **directly enrollable via the API**.
HealthSherpa is quoting the same source-of-truth rate data zizzl was.

Off-exchange has no APTC: `net_premium` equals `gross_premium` and `max_aptc` is empty. Useful consequence
for CSA rendering — **prefer `net_premium`, fall back to `gross_premium`, and the same logic renders
correctly on both exchanges without branching.**

### Ambetter — CORRECTION

The prior version of this doc stated the TX issuer list *and* the Hopkins quote both returned CHRISTUS,
Ambetter, and BCBS. **The Hopkins quotes return no Ambetter on either exchange** — off-ex and on-ex both
come back BCBS / CHRISTUS / UnitedHealthcare only.

Most likely explanation: `GET /v1/reference/issuers?state=TX` is a **statewide** list, and Ambetter /
Superior HealthPlan does not participate in Hopkins County's rating area. The earlier note collapsed the
statewide issuer list and the county-level quote into a single claim.

**This is a market fact, not a HealthSherpa limitation** — but "Forrest's carriers" was recorded as
Christus / Ambetter / BCBS, and for Sandoval specifically Ambetter is simply not available. Worth knowing
before Forrest asks.

### Other observations

- **Name-matching caveat:** BCBS plan names carry a `℠` service-mark glyph (`HMO℠ 306`), not `HMO SM 306`
  — match on `plan_id` / plan numbers, never on the "SM".
- `metal_level` includes **`expanded_bronze`** as a live value — label-mapping gotcha.
- Premium numeric type is **unconfirmed**: the earlier note said premiums return as strings; the live run
  rendered `633.5` rather than `633.50`, which is ambiguous after JSON parsing. Confirm from a raw
  response dump before writing a Java deserializer.

**Verdict:** HealthSherpa replaces what zizzl gated at **both** the quoting *and* the off-exchange
enrollment layer for the carriers that matter to Sandoval. Remaining gates are **administrative
(account approval + AOR attribution), not capability.**

---

## ICHRA is modelled natively

The application schema carries a first-class `hra` object — this is not a generic ACA API with ICHRA
bolted on:

- `offered_hra` (true / false / null), `type` (**`ichra` | `qsehra`**), `amount` (monthly USD),
  `contribution_covers` (`premium` | `premium_oop`), `used_for_spousal_or_family_premiums`
  (feeds QSEHRA 834 indicators), `start`, `premium_payer`, `household_size`,
  `annual_household_income`, `annual_household_income_determination`
- nested `employer`: `name`, **`external_id`** (partner-supplied key), `phone`, `fein`, `address`

`type: "qsehra"` is supported, so this touches **PremiumPath**, not only the ICHRA thread.
`employer.external_id` is the join key back to AMS `Employer` — and it is also a first-class filter on
`GET /v1/enrollments`.

### SEP handling lines up with a service SSA already sells

`special_enrollment_period.event_type` includes **`offered_ichra`** and **`offered_qsehra`** as standard
values (docs note `offered_ichra` is the most common ICHRA reason); HealthSherpa maps them to
carrier-specific values automatically.

Some carriers require SEP documentation before effectuation, uploaded via
`POST /v1/enrollments/{id}/supporting_documentation`. **SSA already produces exactly that artifact** — the
dated per-employee eligibility letters in the PremiumPath service list. Direct line from an existing
service to an API field that consumes it.

**Effective-date trap:** `desired_effective_date` is optional; when supplied it must be valid for the SEP
reason and event date, and returns `422` if not. **There is no endpoint to query valid dates ahead of
time.** Real UX constraint for any AMS-driven flow.

## Backlog #38 (attestation engine) — concrete unblock

**Off-exchange:** incremental poll of
`GET /v1/enrollments?product=ichra&exchange=off_exchange&employer_external_id={X}&updated_since={T}`.
`policy_status` enum: `draft`, `pending_effectuation`, `effectuated`, `submission_failed`, `cancelled`,
`terminated`. Coverage verification per employee per month, from the source, joined on SSA's own employer
key. **Does not wait on the Summit/COMPASS transaction feed.**

**On-exchange:** `/v1/policy-status/*` is **agent-scoped, not employer-scoped, and alpha**. So an ICHRA
population split across both exchanges would **not** have uniform status coverage. Do not oversell this
internally — the attestation-killer is clean for off-ex enrollees and partial for on-ex ones.

## PHI scope — BAA is mandatory, not prudent

If AMS ever calls `POST /v1/enrollments` it handles substantial PHI/PII. The applicant schema includes
`ssn` (9 digits, noted encrypted at rest), `itin`, date of birth, `race_ethnicity`, `hispanic_origin`,
`has_disability` (+ temporary / end-date follow-ups), `medicare_medicaid_eligible`,
`enrolled_in_medicaid_chip_or_other_gov_program`, `currently_incarcerated`,
`has_eligible_immigration_status`, `veteran_or_active_duty_military`, and `existing_coverage` with carrier
policy IDs.

This must sit **inside** the existing HIPAA tiering (`domain_and_compliance_rules.md` §3 / Bedrock rule),
not beside it.

## Client engineering constraints (binding, from Compatibility Rules)

Public object schemas are **intentionally open** and evolve additively without version bumps. Clients:

- **must** ignore unrecognized response properties
- **must** assume objects gain new optional fields over time
- **must not** use strict serialization/deserialization that fails on unknown fields
- **must not** assume property order is meaningful or treat response shapes as closed

For a Java client this means lenient deserialization (`FAIL_ON_UNKNOWN_PROPERTIES = false` or equivalent)
as a hard requirement, not a preference. Cheap to write down now, expensive to discover later.

Additional mechanics:

- **Two error envelopes.** Unified `{ "error": { code, message, details? } }` for edge/gateway failures
  (incl. missing/invalid key → `403 forbidden`, `413`, `415`, edge `429`, `502`/`504`); top-level
  `{ "errors": [ { code, message, field? } ] }` for enrollment *service* failures. Docs are explicit:
  **branch on body shape and `code`, not on HTTP status.**
- **Idempotency.** Required on `POST /v1/enrollments`, optional on enrollment-sessions. 1–255 chars,
  scoped by API key, 24-hour retention. Replay with same key + same body returns the original response
  with `Idempotent-Replay: true`; same key + different body → `422`. **`5xx` responses are stored** — a
  retry after a suspected-transient 5xx needs a **fresh** key.
- All calls backend-only; key never in browser code, frontend env vars, or logs.

## Cost / access model

- **Quoting: free, self-serve, instant key.** Both exchanges. No rep required. Proven.
- **Enrollment workflows: portal request + operator approval.** Registration → confirm email → sign in to
  the developer portal → generate key → *"request access for enrollment workflows you need in the portal."*
  Their team reviews each request and enables access during onboarding; production access and
  environment-specific credentials are shared then.
- **The way in is a portal request, not a cold sales call** — the request itself triggers their follow-up.
- Each developer receives **a single unique key** (see AOR question below — this phrasing matters).
- The Connect suite is marketed **at no cost**; HealthSherpa monetizes on the carrier/enrollment side as
  the EDE, not by charging platforms for the API.
- **SSA's cost is integration engineering, not licensing** — a very different picture than Ideon-style
  rate-data licensing or standing up an EDE.

## Benefits

- Solves Forrest's actual problem: carrier-integrated quote+enroll **without** a vendor gating which
  carriers he can put in front of clients — now proven at the enrollment layer, not just quoting.
- Lets SSA credibly be a **quote-to-admin ICHRA platform** — the thing that fully displaces zizzl.
- Free API access; **HealthSherpa carries the CMS/EDE audit burden**, not SSA (SSA integrates the EDE, it
  doesn't become one).
- **Employer-scoped off-exchange status doubles as the attestation solution** (#38).
- ICHRA/QSEHRA modelled natively, including the employer — maps onto existing AMS entities.
- Market leader (millions enrolled, 40+ platforms, Alegeus partnership) — a defensible dependency.

## Costs / watch-items / open questions

**Open — needs HealthSherpa (the real list, sharpened 2026-07-28):**

1. **AOR / TPA account model — the critical one.** `_agent_id`, `tpa_slug`, `actor.agent_id`, and
   `agent_of_record` are **all rejected if caller-supplied** (`400 invalid_request`, enforced in the JSON
   schema via `not`/`anyOf`); attribution derives from "your approved account setup." Onboarding says each
   developer gets **a single unique key** and enrollment requires **a linked HealthSherpa agent account** —
   both singular. **If one key = one linked agent = one AOR for everything submitted through it, SSA
   submitting for SWBD's whole downline would attribute every enrollment to a single agent** — breaking
   per-agent commission and mapping onto nothing in the GA → sub-agency model (V070/V071).
   The counter-signal is that **`tpa_slug` exists as a distinct server-owned identifier**, implying a TPA
   account shape with agents underneath. **Ask precisely:** *does the TPA account model support multiple
   linked agents with per-application attribution, or is attribution fixed per key?*
   Note the asymmetry: on-exchange `agent_assisted` uses a **per-agent OAuth link**, so per-agent
   attribution demonstrably exists on that path — but that path can't do off-exchange, which is where
   Sandoval's plan lives.
2. **BAA / PHI.** Does Geozoning/HealthSherpa execute BAAs with integrating platforms? Standard paper?
   What is stored by SSA vs. passed through?
3. **Enrollment approval mechanics.** What the portal request requires, what "linked HealthSherpa agent
   account" means concretely for a TPA, and lead time.
4. **UnitedHealthcare off-exchange.** Quotable but not API-enrollable in TX — carrier-contract driven,
   roadmapped, or permanent? Affects how a CSA presents UHC plans.
5. **Webhooks.** This doc previously claimed Submission Confirmation + Policy Status webhooks. They appear
   in **neither** `openapi.json` **nor** the current developer docs — polling is what's documented.
   Confirm whether webhooks exist; **do not plan around them** until confirmed.
6. **Developer-preview status.** HealthSherpa One is marketed "as is," with plan data, premiums,
   eligibility, and workflow details "subject to change." Generating **client-facing CSAs for real
   employers** off a preview product is a different risk posture than prototyping. Ask what the supported
   production posture / SLA is.

**Standing watch-items:**

- **This IS the "shopping/enrollment layer" the SWBD brief said don't build** — but that guidance assumed
  building from scratch (Ideon data + EDE audit + steering risk). Integrating an existing free EDE API is a
  different calculus. Still: revisit the steering/endorsement posture so it's the *agent's/platform's*
  enrollment rail, not SSA steering.
- **Engineering scope** — backend quote calls, off-ex application lifecycle, browser-mediated payment
  redirect, status polling, PHI storage. Medium build; well-documented, API-first. Payment redirect means
  **AMS cannot run enrollment fully server-side.**
- **Dependency** on HealthSherpa as rails (like the Summit dependency) — a conscious strategic bet.

## Recommended next step (before any build)

Read-only evaluation, **no AMS integration commitment yet**:
1. ~~Quote sanity-test~~ **DONE 2026-07-28** — quoting proven at rate-parity.
2. ~~`api_enrollable` carrier/plan coverage~~ **DONE 2026-07-28** — off-ex BCBS + CHRISTUS enrollable
   incl. the Sandoval baseline plan; UHC not; flag is exchange-scoped.
3. **Submit the portal access request** for off-exchange enrollment (and on-exchange sessions if the
   Ambetter/on-ex path matters), carrying the AOR/TPA-account and BAA questions into their follow-up.
4. Only then scope an AMS integration (a Phase-A read-only investigation).

## Related

- `docs/business/swbd_premiumpath.md` — the SWBD ICHRA-admin thread that surfaced this.
- `docs/analysis/project_backlog.md` #42 — HealthSherpa integration evaluation.
- Backlog **#38** — QSEHRA/ICHRA attestation engine (off-exchange `GET /v1/enrollments` is the unblock).
- `docs/analysis/domain_and_compliance_rules.md` §3 — PHI / HIPAA tiering.
- `docs/business/datapath.md` — parallel integration-partner pattern.
- Contract: `https://one.healthsherpa.com/openapi.json` · Docs: `https://one.healthsherpa.com/docs.html`

---

# 2026-07-29 — PRODUCT CORRECTION: the ICHRA Partner API is a separate product

> **This section supersedes earlier conclusions in this document.** Superseded text is retained above
> deliberately. The framing reversed twice; the trail is worth keeping.

## What was wrong

This document previously recorded that the "`docs.ichra.*` is a different product" framing was
mistaken, and retracted it — concluding there was **one** API surface (`api.one.healthsherpa.com`)
with per-workflow account approval as the only gate.

**That retraction was wrong.** Confirmed 2026-07-29 by **Julian Ferdman**, who oversees product
management for ICHRA and off-exchange at HealthSherpa, and by **KJ Sherman** (technical product team),
whose framing was: *"For HSOne we leverage simplified versions of the ICHRA APIs. For your use case,
integrating with those will provide a much better experience."*

There are two products. HSOne exposes a simplified subset. **The dedicated ICHRA Partner API is the
correct target for SSA's use case.**

## The correct product

**Docs:** `https://docs.ichra.healthsherpa.com/`
**Full doc index:** `https://docs.ichra.healthsherpa.com/llms.txt`
**Per-page markdown:** append `.md` to any page URL.
**Documentation query endpoint:** `GET <page-url>.md?ask=<natural-language-question>` — returns a
direct answer with sourced excerpts. Useful for Claude Code work; there is also an MCP integration
documented at `/getting-started/ai-agents-and-mcp`.

**Positioning (HealthSherpa's own):** the ICHRA Partner API is built for ICHRA
platforms/administrators to power quoting, enrollment, payment, member management, and policy updates.
40+ ICHRA platforms are on it. **Free to use** — confirmed directly by Julian, no contract or pricing
gate.

**Environments:**

| | URL |
|---|---|
| Quoting/APTC staging | `https://api.ichra-staging.healthsherpa.com` |
| Quoting/APTC production | `https://api.ichra.healthsherpa.com` |
| Deeplink staging | `https://staging.healthsherpa.com` |
| Deeplink production | `https://www.healthsherpa.com` |

Staging deeplink requires **Basic Auth credentials from an onboarding representative**. Production
deeplink: API key optional but recommended — passing it auto-whitelists any valid `_agent_id` in the
request.

## Corrections to specific prior conclusions

### 1. Webhooks EXIST — prior note said "UNCONFIRMED, don't plan around them"

**Two webhook APIs**, sharing a schema, independently subscribable:

- **Submission Confirmation** — fires when an application is submitted through HealthSherpa.
- **Policy Status** — fires when a policy is effectuated, cancelled (never took effect), or terminated
  (ended after being in force).

`event_type` is `submission` or `sync`. `policy_status` values: `pending_effectuation`, `effectuated`,
`cancelled`, `terminated`, plus `unknown` and `blank` (on-exchange only).

**Not enabled by default.** Setup is a manual, coordinated step: send webhook URL, chosen
authentication method, exchange scope (on/off/both), which webhooks, and environment (sandbox or
production) to an account manager or implementation specialist, who provides a form. They confirm when
the webhook is live in staging for testing.

**Architectural consequence:** this is inbound **push**, not polling. AMS needs a public authenticated
HTTPS endpoint. The existing `/api/*` prefix with `ApiTokenFilter` is the natural home.

### 2. The attestation primitive exists in the payload

The Policy Status payload carries a per-policy `payment` object:

`payment_status`, `payment_status_updated_date`, **`grace_period_start_date`**,
**`paid_through_date`**, `past_due_member_responsibility_balance_due`,
`current_member_responsibility_balance_due`, `autopay_indicator`.

**`paid_through_date` is exactly what the monthly reimbursement obligation needs** — not "is this
person currently active" but "coverage was in force through this date," which is a defensible record
for releasing a given month's reimbursement.

Also in the payload: `external_id` (SSA's correlation key), `application_id`, `transaction_id`,
`issuer_hios_id`, `members[]` (with `member_id`, `effective_date`, `removed_date` — so dependents are
tracked individually), and `policies[]` with `effective_date`, `expiration_date`, `status`,
`plan_hios_id`, `gross_premium`.

**⚠️ Availability is carrier-gated. See the carrier matrix below — this is where it breaks for rural
Texas.**

### 3. AOR is per-application, by NPN — the go/no-go question resolved favorably

Julian, directly: *"Each application can have it's own agent AOR attribution, and if they have
HealthSherpa accounts you can actually assign the enrollment to their individual HealthSherpa
account if you want."*

Mechanically confirmed in the docs:

- The Application Deeplink **requires `_agent_id`** — the opposite of HSOne, which rejected
  caller-supplied `_agent_id`. Passing an API key auto-whitelists any valid `_agent_id`.
- The webhook payload carries three distinct NPN fields — `policy_aor_npn`, `submitter_npn`,
  `npn_used_at_submission` — plus a per-policy `agent_of_record` object (NPN, name, state license
  number, email).

**AOR travels per application, keyed on NPN. It is not fixed per API key.** This maps cleanly onto
SWBD's downline and resolves the question that could have killed the design. It also means SSA is
structurally *not* competing with an agency's agents for the policy.

### 4. QSEHRA is supported on this rail — but only off-exchange

Julian: *"These APIs are labeled as ICHRA, but they are really for all off-exchange enrollments,
including QSEHRA. That said, it is entirely off-exchange."*

See the MEC/subsidy segmentation section in `docs/business/ichra_administration_scope.md` for what
this does and does not mean for PremiumPath. **Short version: it serves the non-subsidy-eligible
population fully, and does not serve the subsidy-eligible population at all.**

### 5. Enrollment architecture — two paths, with opposite PHI consequences

**This is the most consequential finding for the AMS build.**

| | Deeplink (`POST /ichra/off_ex`) | EnrollConnect API |
|---|---|---|
| Model | Returns HTTP 302 with `Location`; SSA redirects the browser. Applicant completes the application **on HealthSherpa**. | SSA collects everything and submits via API. |
| PHI | **HealthSherpa collects SSN, immigration status, incarceration status, attestations.** SSA transmits prefill demographics only. | **SSA collects and stores all of it**, including carrier-specific attestation content returned via `include=enrollment_requirements`, passed back in the `attestations`/`signatures` objects. |
| Availability | Live for every carrier in the matrix. | Per-carrier; state exclusions NJ and NY only. |

**The deeplink path substantially dissolves the hardest problem identified in Phase A** — the
authenticated PHI-bearing employee portal. It shrinks the BAA surface to prefill demographics plus
inbound webhook member/policy data.

**Do not treat the choice as a detail.** EnrollConnect reintroduces the entire PHI-collection problem,
including presenting carrier attestation text and capturing electronic signature consent.

**Unresolved:** whether the deeplink supports **employee self-service** or assumes an **agent**
completes it on the employee's behalf. The Use Cases documentation describes *"redirect agents to
complete each employee's enrollment."* This determines whether AMS builds an employee portal or an
agent workstation. **Asked, not yet answered.**

## Carrier support matrix — off-exchange

Source: `docs.ichra.healthsherpa.com/integration-guide/supported-carriers`, last updated 2026-05-19.
✅ = live, ☑️ = coming in 2026, blank = not listed.

**Hopkins County TX (the carriers actually available there):**

| | BCBS TX | CHRISTUS | UHC |
|---|---|---|---|
| QuoteConnect | ✅ | ✅ | ✅ |
| Deeplink enrollment | ✅ | ✅ | ✅ |
| EnrollConnect API | ☑️ 2026 | ✅ | ✅ |
| Submission confirmation | ✅ | ✅ | ✅ |
| **Policy status updates** | **☑️ 2026** | **blank** | ✅ |

Payment webhook table: **HCSC** (the Blue Cross entity covering IL/MT/NM/OK/**TX**) is *In Progress*
for both markets. **CHRISTUS Health Plan is live on-exchange only.**

**Consequence for a rural Texas group:** enrollment works today; **automated coverage verification
does not.** Attestation falls back to manual — the exact labor that forces competitors into per-group
minimums.

**Consequence for metro Texas:** Ambetter, Cigna, Molina, Oscar, and UHC are all in Texas and all have
Policy Status live. **The automated model works in metro Texas today.** Hopkins County is a worst case,
not a representative one — do not scope the platform against the thinnest rural county.

Also corrected: an older cached version of the carriers page listed EnrollConnect state exclusions as
"NJ, CO, UT, NY and TX." **The live page lists only NJ and NY.** Texas is not excluded.

## On-exchange — worth pursuing, and for a non-obvious reason

Off-exchange policy status is granted **carrier by carrier** (hence BCBS TX at 2026 and CHRISTUS
blank). On-exchange, the docs state submission confirmation and policy status are supported **for FFM
states plus Georgia** — a **state-level** grant. **Texas is an FFM state.**

**So the automation gap that breaks rural Texas off-exchange does not exist on-exchange in Texas.** The
payment webhook table points the same way: CHRISTUS is live on-exchange, absent off-exchange.
Counterintuitively, **on-exchange is currently the more mature rail for coverage-status automation in
Texas.**

On-exchange is also the only path for the subsidy-eligible population — which is PremiumPath's entire
premise, and also covers ICHRA employees whose offer is unaffordable and who opt out for a credit.

**Two blockers, neither technical:**

1. ⚖️ **Licensure.** On-exchange enrollment assistance is regulated; FFM web-broker rules govern who
   may present plans and assist enrollment, with registration, training, and agreement requirements.
   SSA holds no licensure and no carrier appointments. Consumer self-enrollment and agent-assisted
   models both exist, but **this is a counsel question, not a HealthSherpa question**, and it must be
   answered before building.
2. **The on-exchange access request is gated behind linking a HealthSherpa Marketplace agent account,
   which SSA does not have.** The request cannot be submitted. So the live question is not "should we
   request access" but **"what is the account model for an administrator without agent licensure."**

**Contradiction to resolve:** Julian said these APIs are *"entirely off-exchange."* The docs describe
on-exchange quoting **and** enrollment for FFM carriers, on-exchange webhook subscription options,
on-exchange-only `policy_status` values, and an on-exchange webhook payload example. Either he meant
the enrollment path he was steering toward, or on-exchange sits behind a separate permission. **Ask;
do not infer.**

## Contacts

| Name | Role | Email |
|---|---|---|
| Julian Ferdman | Product management, ICHRA and off-exchange | `julian.ferdman@healthsherpa.com` |
| KJ Sherman | Technical product team | `kj.sherman@healthsherpa.com` |
| Michael Levin | **Unknown** — CC'd 2026-07-29 without introduction | `michael.levin@healthsherpa.com` |

**No onboarding representative or account manager has been assigned.** The docs route staging
credentials and the webhook configuration form through that person. **Nothing is testable until one is
assigned.** Asked, not yet answered.

## Status of the HSOne work

The HSOne evaluation remains **accurate about HSOne** and largely **irrelevant** if the ICHRA Partner
API is the target. Specifically: the `api_enrollable` semantics, the 42-of-65 Hopkins enrollability
count, the `POST /v1/enrollments` payload shape, and the `employer_external_id` + `updated_since`
polling design all describe HSOne, not this product.

**The HSOne off-exchange portal access request submitted 2026-07-28 was left in place deliberately** —
it costs nothing and remains a fallback.

## Open items — 2026-07-29

1. **BCBS TX policy status: when in 2026?** This single date largely determines whether SSA builds for
   a 2026 or 2027 launch.
2. **CHRISTUS policy status: planned at all?** (Cell is blank, not ☑️.)
3. **HCSC payment webhook timing** (currently *In Progress*).
4. **Deeplink: employee self-service, or agent-completed?**
5. **Deeplink vs EnrollConnect** — which does HealthSherpa steer a TPA toward, given the PHI tradeoff?
6. **Onboarding contact assignment** — blocks staging credentials and the webhook form.
7. **BAA path** — unaddressed by anyone so far. Counterparty: Geozoning, Inc. DBA HealthSherpa.
8. **On-exchange account model** for an unlicensed administrator; and the off-ex/on-ex contradiction.

Items 1–7 sent to Julian 2026-07-29 (item 8 to be added before sending).

---

## Outreach log

> **Note added 2026-07-29:** this log records the **HSOne-era** evaluation. The product correction
> section above it (dated 2026-07-29) supersedes the interaction model and API-surface conclusions
> here. The outreach record itself — what was submitted, what was asked, and the Marketplace-link
> decision — remains accurate.

### 2026-07-28 — Off-exchange enrollment access requested

**Submitted** the off-exchange enrollment workflow access request via the developer portal.

**Marketplace account deliberately NOT linked.** The portal offers two independent requests;
on-exchange is gated behind linking a HealthSherpa Marketplace account, off-exchange is not. The
linking copy reads: *"connect your agent profile, so access is tied to the right account"* —
**singular agent profile**. Linking would answer the open AOR/attribution question by default, in the
direction SSA does not want, and SSA holds no agent licensure of its own. **Decision: ask first, link
only if the answer supports it.** Nothing needed for an off-exchange ICHRA group sits behind that
link.

**Request text submitted (~105 words):**

> Superior State Administrators is a third-party administrator providing administrative services to
> employer-sponsored ICHRA plans. We're already using the quoting API and want to add enrollment.
>
> Workflow: employees shop plans in our portal, we create and submit off-exchange ICHRA applications,
> upload SEP documentation, and hand off to carrier payment. We then poll enrollment status by
> employer to verify coverage monthly, which is what our reimbursement obligation runs on.
>
> Users: our staff, plus employees of our employer clients enrolling through our portal.
>
> To launch: off-exchange enrollment access, clarity on the TPA account and attribution model, and a
> BAA — the application payload carries PHI.

**QSEHRA deliberately omitted** from the request: off-exchange `POST /v1/enrollments` accepts
`product=ichra` only. QSEHRA participants generally belong on-exchange to preserve the premium tax
credit, which is the whole design premise of PremiumPath. Naming QSEHRA in an off-exchange request
would confuse the reviewer or secure approval for something that does not exist.

### 2026-07-28 — Contact established: KJ Sherman

**KJ Sherman**, HealthSherpa Technical Product Team, `kj.sherman@healthsherpa.com`. Sent a signup
welcome email confirming quoting is self-serve and that enrollment APIs require the portal request
(so the email does **not** substitute for the request). Offered to answer questions or take a call.

**Response sent — written questions, call declined for now.** Rationale: written answers on record
before committing engineering time, rather than improvising in a live conversation. Call offer kept
open.

**Questions pending answers — update this section when they arrive:**

1. **Account and attribution model.** Does the TPA account model support multiple linked agents with
   per-application attribution, or is agent of record fixed per key? *(The go/no-go question — maps
   onto nothing in the GA/sub-agency model, V070/V071.)*
2. **Status data.** Sample `GET /v1/enrollments` response for an off-exchange ICHRA enrollment —
   specifically whether it surfaces lapse, termination, and grace-period states, or only current
   status. *(The single most important answer: it determines whether attestation is automatable or
   whether a human chases coverage confirmations monthly — which is the labor that forces
   competitors into high minimums.)*
3. **Webhooks.** Absent from `openapi.json` and current docs. Do they exist, or is polling the
   intended mechanism?
4. **Test environment.** Is there a sandbox for enrollment workflows, or would the first real
   `POST /v1/enrollments` necessarily create a live policy?
5. **QSEHRA.** Off-exchange appears ICHRA-only. Is on-exchange the only route for QSEHRA participants?

**BAA** flagged in both the request and the email as a launch requirement. Counterparty: Geozoning,
Inc. DBA HealthSherpa.

### Still self-answerable without approval

**Quoting is free and live** — every question about the shopping experience is testable today with the
existing key. Highest-value untested item: **whether plan objects include provider networks, drug
formularies, and benefit summaries.** If they do not, the shopping UI is a price list with phone
calls behind it. Test with a real Hopkins County household including dependents.

## Interaction model (as understood 2026-07-28)

**Per employer at setup:** N affordability quotes, one per employee (lowest-cost silver plan for age
and rating area).

**Per employee at enrollment:** 1+ shopping quotes (repeat on household or filter change) → `POST
/v1/enrollments` → `/submissions` → `/supporting_documentation` → **`/payment_redirect`, which sends
the member off SSA's site to the carrier's own browser payment form.**

**Per employer ongoing:** one `GET /v1/enrollments` poll per cycle, `employer_external_id` +
`updated_since` — a single call covering everyone. **The cheapest and highest-value call in the whole
integration.**

### Two corrections worth keeping visible

1. **Quoting is per-household, not per-employer.** The census cannot produce plan lists: dependents,
   dependent DOBs, and tobacco status are not census fields and are collected at enrollment.
   **The census gates who may enroll; it does not produce the plan list.**
2. **Enrollment cannot be fully headless.** Carrier payment is a browser form. SSA's flow ends by
   handing the member off, and members will complete the flow and never pay. A return path, status
   page, and chase process are required, and `GET /v1/enrollments` is the only way to know who fell
   off. **There is no carrier feed — SSA has no carrier relationship. All post-enrollment visibility
   depends on HealthSherpa's status data**, which makes question 2 above load-bearing for the entire
   design.

---

# 2026-07-30 — VERIFIED REQUEST SHAPE, ACCESS MODEL CORRECTION, AND THE AGE-CURVE FINDING

> **This section adds to, and in two places corrects, earlier conclusions in this document.** The
> 2026-07-29 product-correction section (above) focused on webhooks, the attestation primitive, AOR,
> and QSEHRA — it never revisited the request/response shape or the cost/access model, both recorded
> in the original 2026-07-28 sections and both HSOne's, not this product's. Every item below was
> verified against the live OpenAPI schema for the ICHRA Partner API and against successful staging
> API calls made 2026-07-30.

## Verified request shape (ICHRA Partner API)

- `POST {baseUrl}/api/v1/quotes` — note the `/api` path segment. This document has so far recorded the
  endpoint as `/v1/quotes` (see "Confirmed request shape" and the endpoint inventory, both 2026-07-28)
  — that is the HSOne path, not this product's.
- **Environments:** production `https://api.ichra.healthsherpa.com`; staging
  `https://api.ichra-staging.healthsherpa.com`.
- **Auth:** `x-api-key` header.
- **Body is flat** — not the nested `context` / `location` / `household` shape recorded in the
  2026-07-28 section (that shape is HSOne's).
- **Required fields:** `zip_code`, `fip_code`, `applicants[]` (each entry: `age`, `relationship`,
  `smoker`).
- ⚠️ **`fip_code`, NOT `fips_code`. `smoker`, NOT `uses_tobacco`.** This document's existing
  "Field-name traps" note (2026-07-28 section, "Confirmed request shape") states the **opposite** for
  both — that note is correct for HSOne and wrong for this product. State this explicitly so nobody
  "fixes" working code (`HealthSherpaService.java`, Phase B-1a/B-1b, verified against this section) to
  match the old note.
- `off_ex: true` returns off-exchange plans only — **not** one request per exchange, contrary to the
  2026-07-28 "one request per exchange" note.
- The `metal_levels` request filter enum accepts only `Bronze | Silver | Gold | Platinum |
  Catastrophic` — but **"Expanded Bronze" is a real returned value** (`metal_level` in the response),
  so server-side metal filtering silently drops plans. Pull the full plan set unfiltered and classify
  client-side. (`RateCacheWarmService`, Phase B-1b, does this deliberately — see its class Javadoc.)

## Access model corrected

**Quoting is not free and self-serve on this product.** The "Cost / access model" section above
(2026-07-28) states it is — that is true of HSOne only. ICHRA Partner API keys are **issued**
("generated as needed and shared with you"), not self-service-generated.

The key issued 2026-07-30 works against **staging**; production returns `403`. Consistent with the
documented onboarding flow, where production allow-listing is a follow-up step after staging access.

## Rate parity confirmed at the correct product

Hopkins County TX (ZIP 75482, FIPS 48223), age 40, plan year 2026, off-exchange: **65 plans** — BCBS
24, CHRISTUS 18, UnitedHealthcare 23. Matches the plan-count/carrier split already recorded for HSOne
(2026-07-28) — the two products return the same underlying market data for this county.

**Blue Advantage Silver HMO 306** (HIOS `33602TX0460776`) at **$582.78** — matching both the zizzl CSA
baseline and the earlier HSOne result exactly.

**Staging returns real rate data, not synthetic** — this is not a sandbox with placeholder numbers.

## ⭐ Age curve — the load-bearing finding

Premiums follow the **statutory uniform age rating curve**; carriers cannot deviate from it.

Confirmed across **two carriers, five ages, three separate calls**: one base rate at age 21
reproduces every observed premium **to the cent**. Age 64 ÷ age 21 = **exactly 3.000**.

**Consequence:** one quote at age 21 derives the whole 21–64 curve for every plan in a county — no
need to call the API once per age. Plan **rankings** are age-invariant too, so LCSP, benchmark silver,
and lowest bronze (computed once at age 21) hold at every other age.

**Caveats:**
- Tobacco use is a **separate, plan-specific load** (capped at 1.5×), not part of the age curve — the
  two must not be conflated.
- The curve is **plan-year scoped** — CMS may revise it for a new plan year.
- **State-specific curves exist.** Texas uses the federal default, but this does not generalize to
  every state without a state dimension.
- **The plan *set* is not age-invariant, though the curve and the rankings are.** Catastrophic plans
  are restricted to under-30, so a quote at age 21 returns plans unavailable at 40. Deriving every
  age from one age-21 call requires filtering, not just scaling. See the 2026-07-31 section.

See `net.superiorstate.ams.data.util.AgeCurve` (Phase B-1b) for the implementation — its own Javadoc
carries the same caveats and flags the interior (non-empirically-confirmed) factors as unverified.

## O23 resolved — favorably

The quote request accepts a `providers` array of NPIs; each returned plan carries `covered` (`true` /
`false` / `null`) plus `covered_addresses`.

**A2's provider check is viable.** This document's own earlier warning (2026-07-28, "Still
self-answerable without approval": *"If they do not [include provider networks], the shopping UI is a
price list with phone calls behind it"*) does not apply to this product — network data is present.

## Additional endpoints

- `POST /api/v1/aptc_estimates` — standalone subsidy estimate. Useful for the ICHRA affordability
  threshold independent of a full quote call.
- `GET /api/v1/plans?state=&plan_year=` — state-level plan listing. A non-per-household warming path
  worth evaluating against the per-county `quoteSingleApplicant` approach `RateCacheWarmService`
  currently uses.
- `GET /api/v1/plans/{hios_id}` — single plan lookup, carrying `deeplink_enrollment` /
  `api_enrollment` capability flags and, on request, carrier attestation content.
- `ichra_only: true` flags plans available **only** to applicants with an ICHRA offer.

## Related to this section

- `net.superiorstate.ams.data.service.HealthSherpaService` (Phase B-1a/B-1b) — implements the request
  shape verified above.
- `net.superiorstate.ams.data.util.AgeCurve` (Phase B-1b) — implements the age-curve finding above.
- `docs/analysis/project_backlog.md` T39 — HealthSherpaService needs multi-applicant support (A2, A3,
  and folding the rate-cache canary into the primary call).

---

# 2026-07-31 — PAGINATION TRUNCATION, RESPONSE SHAPE, AND THE FIPS LOOKUP QUESTION

> **Supersedes the 2026-07-28 response field map in full**, and adds three findings not previously
> visible: an undisclosed pagination cap, the absence of any ZIP→FIPS endpoint, and the fact that no
> AMS installation has ever authenticated to this API. All verified against live staging 2026-07-31.

## ⭐ The pagination cap — every cached aggregate was computed on a truncated set

`POST /api/v1/quotes` **defaults to `per_page: 20`**, and `meta` carries **only `result_count`** — no
total, no page count, at any `per_page` value. **A truncated response is therefore indistinguishable
from a small market.**

Hopkins County TX (75482 / 48223), plan year 2026, off-exchange, age 40:

| Metal | True (`per_page: 100`) | Received at default |
|---|---|---|
| Silver | 27 | 5 |
| Gold | 19 | **0** |
| Expanded Bronze | 15 | 11 |
| Bronze | 4 | 4 |
| **Total** | **65** | **20** |

Carrier split at 65 plans: BCBS 24 / CHRISTUS 18 / UHC 23 — matching the 2026-07-30 baseline exactly.

**How it was caught:** two calls at different ages both returned exactly 20. Age 21 included 2
catastrophic plans; age 40 included none. A genuine 20-plan market would have returned 18 at age 40.
It returned 20, backfilling with two more Expanded Bronze and two more UnitedHealthcare — proving
more plans existed than were being returned.

`RateCacheWarmService` derived market low/high, LCSP, benchmark silver, lowest bronze, carrier count
and plan count from that 20-plan subset. **LCSP computed off 5 of 27 silver plans is the ICHRA
affordability threshold.** Fixed in commit `eba17ad`: the service now paginates at `per_page: 100`
until a short page, and fails the whole quote on any page error rather than returning partial results.

**Standing consequence for any future integration:** completeness cannot be verified from a single
response. It can only be established by paginating until a short page returns.

## Request parameters confirmed present

Beyond the required fields recorded 2026-07-30 (`zip_code`, `fip_code`, `applicants[]`):

- **`per_page`** / **`current_page`** — pagination. Default 20.
- **`sort`** — e.g. `premium_asc`. Not load-bearing for aggregates (min/max are order-independent),
  but makes ordering deterministic across runs. Adopted in `eba17ad`.
- **`fields`** — restricts which plan fields are returned; `name`, `year`, `hios_id` and
  `gross_premium` are always included regardless. Not adopted — see project backlog **T41**.
- **`include_non_enrollable_offex`** — documented as causing non-enrollable off-exchange plans to be
  returned. **Actual effect unclear:** all 23 UnitedHealthcare plans — the known non-API-enrollable
  carrier in this county — were returned *without* passing it. See backlog **T42**.

## Verified response shape — the 2026-07-28 field map is HSOne's

The 2026-07-29 and 2026-07-30 corrections fixed the *request* shape and never revisited the
*response*. The 2026-07-28 map is wrong for this product in the same way and for the same reason.

| 2026-07-28 section says | Actually returned |
|---|---|
| `plan_id` | **`hios_id`** |
| `pricing.gross_premium` | **`gross_premium`** — top level, no `pricing` object exists |
| `api_enrollable` | **`deeplink_enrollment`** and **`api_enrollment`** — two separate booleans |
| `details.metal_level` | **`metal_level`** — top level |
| `details.hsa_eligible` | **`hsa_eligible`** — top level |

**The body is flat.** Confirmed at top level: `hios_id`, `name`, `metal_level`, `plan_type`,
`hsa_eligible`, `state`, `year`, `csr_level`, `gross_premium`, `ehb_premium`, `subsidy_applied`,
`premium`, `ichra_only`, `deeplink_enrollment`, `api_enrollment`. Nested: `issuer` (carrying `name`,
`hios_id`, `state`), `networks[]`, `cost_sharing`, `benefits`, `urls`,
`gross_premium_per_applicant`, `providers[]`, `drugs[]`.

Envelope is `{ "plans": [...], "meta": { "result_count": N } }`.

## ⭐ The plan set is NOT age-invariant — amends the age-curve finding

The 2026-07-30 finding established that premiums follow the statutory curve and that plan
**rankings** are age-invariant. Both hold. **It did not establish that the plan *population* is
age-invariant, and it is not.**

ACA catastrophic plans are restricted to enrollees under 30. In the reference county, age 21 returns
2 catastrophic plans; age 40 returns none.

`RateCacheWarmService` makes one call at age 21 and derives ages 21–64 from the curve — so rows for
ages 30+ were computed over a set containing plans nobody that age can buy. Catastrophic plans are
typically the cheapest thing in a market, so `market_low_premium` was the exposed figure.

Fixed in `eba17ad`: aggregates are computed per age from a catastrophic-filtered list for ages 30+.
**`market_low_premium` legitimately steps up between the age-29 and age-30 rows.** That
discontinuity is correct, not a defect.

LCSP and benchmark silver are silver-only by definition and were unaffected.

## No ZIP → county FIPS endpoint exists on this product

The Quoting API documentation carries a **FIPS Code Lookup** note pointing to a "FIPS County Codes"
page for how to look a FIPS up from a ZIP. **That is a documentation page, not an endpoint.** The
documented endpoint set is quoting, APTC estimation, enrollment deeplink, and webhooks — no
reference or lookup endpoint appears anywhere.

The earlier claim that `GET /v1/reference/counties?zip_code=` provides this crosswalk came from the
**HSOne** OpenAPI contract and does not apply here. **The ZIP→FIPS route via this API is closed.**
AMS must carry its own county reference data.

## No AMS installation has ever authenticated to this API

Every verification recorded in this document — 2026-07-28, 07-30 and 07-31 — was made with an
out-of-band HTTP client and a manually supplied key. **None went through AMS.**

Checked on local dev 2026-07-31: `HEALTHSHERPA_API_KEY` is absent from **both** the `constant` table
and `ssa.properties`. **D-78 and D-79 have never been applied to any environment.**
`AppConfig.getHealthSherpaApiKey()` has always returned null in the running application, so the path
`AppConfig` → `AmsDataGlobal` startup population → `HealthSherpaService` is code-complete and
entirely unexercised.

**`AppConfig.getHealthSherpaBaseUrl()`'s hardcoded default is the production endpoint**
(`https://api.ichra.healthsherpa.com`). Harmless today, because production returns 403 pending
allow-listing. It stops being harmless the moment allow-listing lands: any installation holding a
key but no configured base URL will silently target production. D-82 makes this concrete — the warm
job is optional on demo, and a demo installation with warming enabled and no base URL would warm its
cache against production, consuming production quota. **Open decision:** default to staging, or
return null and have `HealthSherpaService` refuse to call. Not yet made.

## Open — LCSP exchange scope

`RateCacheWarmService` quotes with `off_ex: true`, so `lcsp_premium` and `benchmark_silver_premium`
are computed from **off-exchange silver plans only.**

For ICHRA affordability the applicable LCSP is the lowest-cost silver plan offered **on the
Exchange**; the APTC benchmark is on-exchange by definition. The two sets differ here — 2026-07-28
recorded 45 plans on-exchange against 65 off-exchange. If an off-exchange-only silver plan undercuts
the true on-exchange LCSP, the cache understates LCSP, making an ICHRA offer appear affordable when
it is not.

**Unresolved.** The empirical half is two staging calls differing only in `off_ex`; it could not be
run 2026-07-31 because no key is configured anywhere. The compliance half belongs with **O18** and
**O25**.

## Related to this section

- `net.superiorstate.ams.data.service.HealthSherpaService` — pagination, commit `eba17ad`
- `net.superiorstate.ams.data.service.RateCacheWarmService` — catastrophic age filter, same commit
- `docs/analysis/project_backlog.md` — **T41** (`fields` parameter), **T42**
  (`include_non_enrollable_offex` semantics)
- `docs/deployment_backlog.md` — **D-78** / **D-79**, unapplied everywhere

---

# 2026-08-04 — O2 CLOSED: THE ENROLLMENT/STATUS SURFACE, READ FROM PUBLIC DOCS

> **This section closes O2**, open since 2026-07-29 at an estimated one hour: *"re-verify the
> enrollment/status API surface against the ICHRA Partner API — public docs, no account needed."*
> It **corrects six standing claims** in this document, resolves several open items, and needed
> no credential, no representative, and no BAA.
>
> **Method:** `https://docs.ichra.healthsherpa.com/llms.txt` gives a 25-page index. Nine pages were
> read on 2026-08-04: Integration Setup, Endpoints, QuoteConnect, Application Deeplink, Submission
> Confirmation, Policy Status, Supported Carriers, Enrollment Decision Path, Deeplinks V2. Then four
> more, after a first pass under-read the enrollment side: **EnrollConnect**, Carrier Effective Date
> Logic, Webhooks API (parent), API Changelog. **Thirteen of 25 read; twelve unread** — listed near
> the end of this section.
>
> ⚠️ **Pages were fetched and summarised, not parsed from the schema.** Since 2026-07-09 every API
> reference page links its **OpenAPI YAML**. Given this document's history with `fip_code`, `/api`
> and now `/public`, **nothing below should be coded against without checking the YAML first** —
> filed as **T145**.

## ⚠️ Corrections to claims recorded in this document

| This document says | Documentation says | Consequence |
|---|---|---|
| Deeplink is `POST /ichra/off_ex` (2026-07-29) | **`POST /public/ichra/off_ex`** | Missing path segment — the **third** instance of this error class after `/v1/quotes` to `/api/v1/quotes` |
| *"UnitedHealthcare — quotable but not API-enrollable in TX"* (open question #4, 2026-07-28); *"the known non-API-enrollable carrier in this county"* (2026-07-31) | **UHC: quote, deeplink, EnrollConnect, submission confirmation AND policy status all live** | See "The UHC finding" below. **This was wrong when written, not superseded** |
| `tpa_slug` *"rejected if caller-supplied (400 invalid_request, enforced via `not`/`anyOf`)"* (2026-07-28) | **`tpa_slug` is an accepted request field** inside the HRA object — *"auto-fill TPA from database"* | That constraint is HSOne's. Combined with `_agent_id` and nine `agent_of_record_*` fields, the AOR/TPA model is resolved at contract level |
| Deeplink *"substantially dissolves the hardest problem... SSA transmits prefill demographics only"* (2026-07-29) | The deeplink API **accepts `ssn`** (9 digits, no dashes), race/ethnicity, hispanic origin, DOB, gender, existing coverage, household income | **Half wrong.** PHI minimisation on the deeplink path is a **design choice AMS makes**, not a structural property of the rail. Material to the BAA scope (O13) |
| Policy Status values include `pending_effectuation` (2026-07-29) | Policy Status carries **`effectuated` / `cancelled` / `terminated`**; `pending_effectuation` arrives on the **Submission Confirmation** webhook | Two webhooks, two state vocabularies. Do not build one state machine from the wrong list |
| *"Enrollment cannot be fully headless — carrier payment is a browser form POST"* (2026-07-29, repeated since) | True for the `payment_redirect` path only. **`PUT /applications/:id/payment_method` takes ACH server-side**, and the 2026-07-29 changelog added in-flow ACH for Anthem/Wellpoint | **Carrier-dependent, not universal.** Some carriers can be fully headless |

## The UHC finding — and why it matters beyond UHC

**API Changelog, 2026-06-09:** *"UnitedHealthcare Expansion: Added across 25 states (AL, AZ, CO, FL,
GA, IA, IL, IN, KS, LA, MA, MI, MO, MS, NC, NE, NM, OH, OK, SC, TN, TX, VA, WA, WI, WY). NJ and NY
still in testing."*

UHC EnrollConnect went live in Texas **seven weeks before** this document recorded UHC as the known
non-API-enrollable carrier in Hopkins County. **That claim was false at the time it was written** —
not correct-then-superseded.

**Standing consequence:** the 2026-07-28 HSOne-era findings need to be distrusted harder than
"superseded on request/response shape." At least one was substantively wrong about the market on the
day it was recorded.

**Partial mitigation, and worth knowing:** the carrier matrix was genuinely moving underneath that
evaluation. **BCBS TX (HCSC) EnrollConnect only went live 2026-07-16**, twelve days before it.

## The enrollment decision path — a documented three-way tree

Both flags are already present on every plan in the quote response AMS receives today.

1. `api_enrollment = true` then **EnrollConnect API**
2. else `deeplink_enrollment = true` then **Application Deeplink**
3. else **quote-only**; an enrollment attempt returns `422` *"Plan is not available for enrollment"*

*"Both endpoints accept the same canonical request schema, so you can build once and route
accordingly."* One payload builder, two routes — simpler than the plan assumed.

Pre-enrolment validation: `GET /api/v1/plans/{hios_id}?include=enrollment_requirements` returns
per-plan attestations, SEP reasons, HRA fields, applicant questions and proof-of-residency
instructions.

## `plan_hios_id` is REQUIRED — the architectural question, settled

Required on the current deeplink **and** on Deeplinks V2, whose specification states plainly:
*"A specific plan HIOS ID remains required; no shopping/browse experience is supported."*

**There is no version of this integration where AMS hands off to a HealthSherpa shopping experience.**
AMS must present plans and pass a chosen one.

**Therefore the ERISA neutral-presentation burden lands on SSA, not on HealthSherpa.** Two consequences:

- **O20** (*"is SSA building a plan display on the employer's behalf itself an endorsement problem?"*)
  moves from theoretical to load-bearing. It gates the only input the enrollment API requires.
- `ichra_platform_capability_map.md` Layer 3's design constraint — complete list, neutral ordering,
  employee-controlled sort/filter, no "recommended" badge, no default selection, no hidden carriers —
  is now **mandatory rather than a stated preference**. It is the only compliant route to a
  `plan_hios_id`.

## EnrollConnect API — full application lifecycle, live since 2026-04-13

Same base URLs and `x-api-key` as quoting (`api.ichra-staging.healthsherpa.com` /
`api.ichra.healthsherpa.com`).

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/applications` | Create draft |
| PUT | `/api/v1/applications/:id` | Update |
| GET | `/api/v1/applications/:id` and `/api/v1/applications` | Retrieve, list (paginated) |
| POST | `/api/v1/applications/:id/submit` | Submit to carrier |
| POST | `/api/v1/applications/:id/cancel` and `/terminate` | Pre- / post-effectuation |
| PUT | `/api/v1/applications/:id/payment_method` | Set ACH payment method |
| GET | `/api/v1/applications/:id/payment_redirect` | Carrier payment form |
| POST | `/api/v1/applications/:id/supporting_documentation` | Upload SEP docs |

**Not a single monolithic submit.** A draft/validate/submit lifecycle with inline validation
(`errors` array — **empty means ready to submit**), hypermedia `next_actions`, and an `events` audit
timeline carrying field-level from/to diffs with **automatic PII redaction** (newest-first, max 50).

`policy_status`: `draft` to `pending_effectuation` to `effectuated` / `submission_failed` /
`cancelled` / `terminated`. `document_status`: `none_needed` / `required` / `uploaded` / `verified` /
`denied`.

⚠️ **Full-replacement model:** *"Send the complete application on every call. Any field you omit may
be cleared."*

⚠️ **Neither create nor submit is idempotent.** `external_id` is enforced **unique per platform**;
a duplicate returns `422 duplicate_external_id`. **This makes D20's opaque correlation UUID a
requirement, not a tidiness preference.**

**Post-enrolment constraints:** cannot change `first_name` and `last_name` simultaneously after
submission; `date_of_birth` cannot change alongside a name change; plan changes need OEP or a
qualifying SEP; check `supports_changes` / `can_change_plan` / `can_report_change` first.

## PHI, attestations and signatures — larger than recorded

**EnrollConnect collects:** SSN (encrypted at rest) or ITIN, DOB, gender, Medicare/Medicaid enrolment
status, disability status with end date, incarceration status, veteran/active-duty status,
immigration eligibility indicator, race/ethnicity, hispanic origin, spoken and written language,
addresses with FIPS, tobacco use in last 6 months, communication impairment type, existing coverage
(insurer, policy id, term date), full-time student status, marital status, guardian information for
minors, responsible-party details.

**The deeplink-vs-EnrollConnect PHI asymmetry recorded 2026-07-29 holds** — if anything it
understated EnrollConnect's surface.

**Attestations** (state- and carrier-specific): `agrees_issuer_attestations`,
`electronic_signature_consent`, `broker_signature_attestation`,
`coverage_replacement_attestation_accepted`, `pediatric_dental`, `consumer_working_with_agent`,
`spouse_or_dependent_authorization`, plus four agent attestations —
`agent_submitted_application`, `agent_provided_consumer_marketing_materials`,
`agent_advised_consumer_of_product_features`, `agent_retained_signed_application_copy`.

**`disclosure_statement_accepted` is required for Texas HMO plans** — and the Hopkins reference plan
is **Blue Advantage Silver HMO 306**. The demo county's flagship plan hits this requirement.

**Signatures:** primary applicant's typed full legal name in `applicants.primary.signature`, plus
`signatures.signature_date`. Supplemental state signatures for **CO, UT, NJ**.

⚖️ **The four `agent_*` attestations model an agent in the loop and record what that agent did.**
That is evidence for the **agent-workstation** reading of **O14**, at least on the EnrollConnect
path — the API expects a licensed agent, not an unattended employee.

## Payment — a decision tree, not a single redirect

`payment_instructions` carries three booleans: `payment_required_with_submission`,
`payment_redirect_supported`, `pay_by_phone_supported`.

1. **`payment_required_with_submission = true`** then `PUT /payment_method` with ACH details
   **before** submit; requires 200 to proceed.
2. **Otherwise** submit first, poll until status leaves `draft`, then evaluate post-submit options.
3. **Post-submit:** if `next_actions` contains `rel: "payment_redirect"` then `GET /payment_redirect`
   returns carrier destination, `POST` method and form fields — *"submit all returned fields in the
   member's browser without interpreting them."* Else if `pay_by_phone_supported`, show
   `payment_phone_number`. Else the carrier handles payment outside the integration.

⚠️ **The redirect does not confirm payment.** Continue polling or rely on webhooks for reconciliation.

## Polling and rate limits — the design O2 existed to unblock

- Wait a **minimum of 1 hour** after submission before the first poll.
- Then poll every **4-8 hours**; most carriers report effectuation within **1-3 business days**.
- **Never more than once per minute per application**, or `429`.
- `429` carries **`Retry-After`**. `5xx` is retryable with exponential backoff, **max 3 attempts**.
- `include_events=false` for a lightweight poll that skips the audit timeline.
- **Webhooks are the documented primary mechanism; polling is the fallback.**

`422` error codes: `missing_required_field`, `invalid_field_value`, `invalid_field_format`,
`plan_not_found`, `plan_not_available`, `duplicate_external_id`, `ineligible_for_enrollment`,
`supporting_documentation_required`, `latest_submission_failed`, `payload_too_large`.

## Effective dates — this document's existing claim CONFIRMED

**Confirmed:** *"No endpoint to query valid dates ahead of time"* — the carrier validates and returns
either a list of valid dates or a message indicating selection is unavailable.
`ichra_administration_scope.md`'s *"no API endpoint pre-validates effective dates for a SEP reason;
a human step"* holds exactly as written.

`desired_effective_date` (ISO 8601) is optional. *"When omitted, the carrier auto-determines the
effective date based on the SEP type and event date (recommended)."*

**Carrier logic varies.** The majority pattern — *"first of next month from today"* — covers
**Ambetter, BCBS entities, Cigna, Molina, Oscar and UHC**. The 15th-of-month cutoff applies to
CareSource and MedMutual, **not** the Texas carriers. Anthem/Wellpoint/Sanford use *"depends on QLE
1st-or-later."*

⚠️ **Consequence for Sandoval (9/1/26 effective, `sep_reason: offered_ichra`):** on a
first-of-next-month carrier, the submission must land in **August**.

## Webhooks — setup process public, delivery semantics not

**Documented setup (four steps):** supply your HTTPS endpoint URL, authentication preference,
**exchange scope (on / off / both)**, which APIs (Submission Confirmation / Policy Status / both) and
environment (sandbox / production) to your account manager; they register the endpoint and enable
delivery; verify in staging; go live. Delivery is `HTTPS POST`, `Content-Type: application/json`.

⚠️ **Still not public, and this sharpens O15 considerably** — it is not merely "which auth methods":

- Authentication methods (*"a variety"*, unspecified)
- **Retry policy and limits**
- **Delivery guarantees** (at-least-once vs exactly-once)
- **Ordering guarantees**
- **Idempotency / `transaction_id` handling**
- **Signature verification**
- **IP allow-listing**

**Those five operational items decide whether AMS's receiver needs dedup and reordering logic.**
Ask for them by name.

**Payload note:** the **off-exchange payload is a superset of on-exchange**. Off-ex adds
`application_id`, `issuer_hios_id`, `members[]` and `policies[]`; on-exchange webhooks carry
materially less. This **strengthens D16 (off-exchange first)** — the data coverage verification needs
only exists on the off-ex rail. `payment` object: `payment_status` (`unpaid_binder` / `paid_binder` /
`paid` / `past_due`), `payment_status_updated_date`, `grace_period_start_date`, `paid_through_date`,
balances, `autopay_indicator`.

⚠️ **Payment dates are `MM/DD/YYYY` strings, not ISO.** Relevant to backlog #38 / phase B3.

## Carrier matrix — off-exchange, as published 2026-08-04

The full matrix covers 26 carriers. Texas-relevant rows, and what they mean for Hopkins' 65 plans:

| Carrier | Hopkins plans | Quote | Deeplink | EnrollConnect | Submission | **Policy status** |
|---|---|---|---|---|---|---|
| BCBS TX | 24 | live | live | live | live | **Coming in 2026** |
| UHC | 23 | live | live | live | live | **Live now** |
| CHRISTUS | 18 | live | live | live | live | **Not listed** |
| Ambetter | (25 states incl. TX) | live | live | live | live | live |

**O16 is half-answered without a representative.** BCBS TX policy status is publicly marked "coming
in 2026." **The year is confirmed; the month is not** — and the month is what the 2026-vs-2027 launch
decision turns on.

**The "rural Texas has no automated coverage verification" framing is too pessimistic.**
**UHC policy status is live today**, covering 23 of Hopkins' 65 plans. Automated verification is
available now for roughly a third of that market — the difference between *"impossible"* and
*"depends which plan the member picked."* The manual fallback stays first-class, but it is not the
only path from day one.

⚠️ **CHRISTUS has no policy-status roadmap at all** — not "coming," simply absent. That is the
carrier zizzl switched off for Forrest, and 18 of Hopkins' plans. **Ask about it by name.**

Also published: *"Support for all carriers and all states is expected prior to OEP PY2027"*
(EnrollConnect gaps are NJ and NY).

## Quoting — parameters this document had not recorded

`POST /api/v1/quotes` accepts more than previously catalogued: **`household_income`**,
`dental_search`, `add_attributes`, `utilization` (low/medium/high), `all_benefits`, `all_details`,
`networks` (HMO/PPO/EPO/POS/Indemnity), `issuer_hios_ids`, `providers` (NPIs), `filter`, and
`per_page` **max 500** (not 100).

**`household_income` returns subsidy information inline.** Subsidy segmentation — capability #5 in
`ichra_strategy.md` §3, recorded as *"documented, never called"* — is **a request field on a call AMS
already makes**, not a new integration. Filed as **T147**.

**`GET /api/v1/plans?state=&plan_year=&off_ex=` was added 2026-07-09** and the changelog states its
purpose explicitly: *"enables plan catalog caching with pagination support."* **HealthSherpa built the
state-level endpoint for the caching use case.** It is the intended path for rate-cache warming, not
a workaround. Filed as **T146**.

`POST /api/v1/aptc_estimates` requires `zip_code`, `fip_code`, `household_income`, `applicants[]`;
returns `estimated_aptc` and `csr_level` (enum `00`-`06`).

## SEP reasons — `offered_ichra` and `offered_qsehra` are first-class

The deeplink `sep_reason` enum includes **`offered_ichra`** and **`offered_qsehra`** alongside
`birth`, `adoption`, `death`, `divorce`, `marriage`, `loss_of_mec`, `relocation`, and roughly twenty
others.

⚖️ **Operational evidence — not proof — for the counsel question** in
`ichra_administration_scope.md` about whether the QSEHRA/ICHRA triggering event reliably compels
off-exchange issuers. HealthSherpa's own off-exchange rail treats both as valid SEP reasons across
its carrier matrix. **Still a counsel question; the evidence is now better than it was.**

## HRA object — native, and it maps onto AMS entities

Required: `type` (**`ichra` | `qsehra`**) and `offered_hra`. Also carried: `amount` (monthly employer
contribution), `contribution_covers` (**`premium` | `premium_oop`**),
`hra_used_for_spousal_or_family_premiums`, `start`, employer `name` / `phone` / address / `fein`,
`premium_payer`, `household_size`, `annual_household_income`,
`annual_household_income_determination`, `offered_hra_unknown`, **`tpa_slug`**.

## ⚠️ Open contradiction — resolve before building the AOR path

**Changelog 2026-04-09:** *"`_agent_id` no longer required."*
**Current Application Deeplink page:** `_agent_id` is listed as **required**.

Most likely the change applied to EnrollConnect (where `agent_of_record` is optional) while the
deeplink retained it — **but that is inference.** `_agent_id` is the AOR mechanism and the whole SWBD
downline attribution model rests on it. Filed as **T144**.

## Pages not read (12 of 25)

Introduction, Onboarding, AI Agents & MCP, Supporting Material (parent), FIPS County Codes,
Carrier-Specific Info, Use Cases, Integration Scenarios, FAQs, Coming Soon Overview, Expanded
Deeplink API (parent), Deeplink Mapping.

**Highest value among them:** Carrier-Specific Info (CHRISTUS and BCBS TX quirks), Integration
Scenarios and Use Cases (HealthSherpa's own framing of the TPA/administrator account shape), FAQs
(often where rate limits and SLA posture live), Onboarding (the approval process).

## What is still genuinely human-gated after this section

Everything else on the old blocker list was answerable from a URL. What remains:

1. **Webhook authentication method plus the five delivery semantics** above (O15)
2. **Staging deeplink Basic Auth** credentials
3. **Production allow-listing** (T136)
4. **BAA** — counterparty Geozoning, Inc. DBA HealthSherpa (O13)
5. **BCBS TX policy status — which month in 2026?** (O16)
6. **CHRISTUS policy status — planned at all?**
7. **Published rate limits for quoting** (a `429` exists; no values documented)

## Related to this section

- `docs/analysis/project_backlog.md` — **T144** (`_agent_id` contradiction), **T145** (verify against
  the OpenAPI YAML), **T146** (`GET /api/v1/plans` as the warm path), **T147** (`household_income`
  subsidy inline), **T148** (`AgeCurve` has no 2027 curve), **T149** (`per_page` 500)
- `docs/ichra_strategy.md` §3 (capability ranking), §6 (data surface), §10 (long-lead register)
- `docs/business/ichra_platform_capability_map.md` — Layer 3
- `docs/business/ichra_administration_scope.md` — effective-date claim, now confirmed

---

# 2026-08-04 (b) — RATE STABILITY, SILVER LOADING, AND WHAT STAGING DATA ACTUALLY IS

> **Companion to the O2 section above.** That one recorded what the API *does*; this one records what
> the *data* is — how often it changes, why the on- and off-exchange figures diverge, and what can and
> cannot be claimed about staging. **Produced from a design discussion on 2026-08-04**, reasoning over
> data already in the repository. It introduces no new API calls.
>
> ⚠️ **Provenance discipline.** Claims below are marked **[verified]** (checked against this
> repository or the API documentation), **[domain]** (ACA regulatory mechanics — settled law, but not
> verified here and worth a counsel or carrier confirmation before it reaches client-facing material),
> or **[inference]**. **The silver-loading explanation is [inference] over [verified] numbers** — it
> fits the data and nothing else proposed fits as well, but it has not been tested. The test is
> specified at the end and is two staging calls.

## How often off-exchange rates change: annually, and not otherwise

**[domain]** ACA-compliant individual-market plans are the **same filed products** whether sold on or
off exchange. Three mechanics lock this:

- **Single risk pool** (ACA §1312(c)(1)) — an issuer must pool all its individual-market enrollees,
  on *and* off exchange, and derive one index rate for that pool.
- **Rating factors are exhaustive** — age (the statutory curve `AgeCurve` implements), geographic
  rating area, tobacco (capped 1.5×), family composition. Nothing varies by sales channel.
- **Rates are filed and approved annually** with the state DOI and are **fixed for the plan year.**

**Consequence: an off-exchange premium for plan X, rating area Y, age Z, plan year 2026 does not
change between January and December.** Off-exchange is not a less stable data source than
on-exchange — it is the same filed rate, a different catalog.

⚠️ **"Off-exchange" is not a synonym for ACA-compliant, and only one meaning is stable.** Short-term
limited duration, fixed indemnity and health care sharing ministries are all sold off-exchange, none
are MEC, and none are subject to any of the above — they can reprice whenever the carrier likes.
`ichra_administration_scope.md` already carries this as a substantiation guardrail; it applies to the
**data** conversation equally. **Everything in `rating_area_rate_cache` is ACA individual market**,
because it comes from this rail. Nothing cached here is exposed to that volatility.

### What does change inside a plan year: the catalog, not the rates

**[verified/domain]** This matters more for AMS than rate churn would, because **the cache stores
aggregates over a catalog, not individual plan rates.** `market_low_premium`, `lcsp_premium`,
`benchmark_silver_premium`, `lowest_bronze_premium`, `plan_count` and `carrier_count` are all
functions of *which plans were in the response*. **One plan added, corrected or withdrawn moves every
one of those figures even though no rate moved at all.**

Realistic sources: issuer service-area corrections, carrier data corrections flowing through to
HealthSherpa, and product discontinuations (rare mid-year — guaranteed renewability generally
requires advance notice and takes effect at renewal).

⭐ **Design consequence: the refresh AMS needs is change *detection*, not re-fetch.** One age-21 call
per county, compare `plan_count` / `carrier_count` / `market_low_premium` against what is stored,
rewrite only on drift. That is the same shape as the existing age-curve **canary**, applied to the
catalog instead of the curve — and it turns the cache into its own drift alarm. Tracked as **T152**.

### The cadence mismatch this exposes

**[verified]** `RateCacheWarmService.INTERVAL_HOURS = 24`, with a full delete-and-replace per county
(`RateCacheDAO.replaceCountyRates`). **The job re-fetches, roughly 365 times a year, data that is
filed once a year.** The cadence was a generic scheduled-job default, not a reading of the data's
volatility. See **T152**; it is also why "which counties can we afford to warm" was never the
question it appeared to be — see the cost model below.

## What warming actually costs — the numbers, so nobody re-derives them

**[verified]** against `RateCacheWarmService.warmCounty` and `AgeCurve`:

**Per county-year: two API calls.** One off-exchange quote at age 21, one on-exchange quote at age 21
(T44/V078). Plus **one canary per plan year in total**, not per county. Each call paginates at
`per_page: 100` until a short page, so a 65-plan market is a single round trip.

**Output: 44 rows** (ages 21–64). **Every row above age 21 is arithmetic** — `AgeCurve.scale()`
multiplying the age-21 premium by a statutory factor. No API call learns anything about ages 22–64.

| Scope | Rows |
|---|---|
| 1 county, 1 plan year | 44 |
| 4 counties, 1 plan year (state as of 2026-07-31) | 176 |
| **All 254 Texas counties, 2 plan years** | **22,352** |
| Every US county (~3,143), 2 plan years | ~276,600 |

⭐ **Storage is not a constraint and will not become one**, even nationally — the row is ~15 small
numeric columns. **Any framing of the county list as "how much data can we afford to store" is
answering a question that does not exist.**

**The real cost is API calls, and it is entirely a function of cadence.** All 254 Texas counties ×
2 plan years = ~1,016 calls **per cycle**. At 24-hour cadence that is ~370,000 calls a year. At
plan-year cadence plus a rollover re-warm it is ~1,016 calls, **once** — less than three days of the
current four-county schedule.

⭐ **Warm the whole state once and the "which counties" question dissolves.** It was never a storage
decision and never really a business decision; it was an artifact of refreshing annual data daily.
**This is the substantive update to D-83.**

## ⭐ Silver loading — what the V078 gap almost certainly is

**[verified]** V078's header records a live staging probe, Hopkins TX, plan year 2026:

| | Off-exchange | On-exchange | Ratio |
|---|---|---|---|
| Age 40 lowest silver | $489.38 | $705.37 | 1.4414 |
| Age 21 lowest silver | $382.93 | $551.93 | 1.4413 |

**[domain]** For a genuinely identical plan — same HIOS id — the premium **must** be the same on and
off exchange. The index rate is set for the whole single risk pool; the only market-wide adjustments
are risk adjustment, reinsurance and **Exchange user fees**, and the user fee is applied *market-wide*
rather than loaded onto exchange plans. Plan-level adjustments (AV, network, benefits beyond EHB,
admin costs) **explicitly exclude Exchange user fees.** The regulation deliberately prevents charging
more for the same plan on-exchange.

**[inference]** So a 44% gap cannot be the same plan priced twice. It is almost certainly **silver
loading**, which works by creating **different plans**, not different prices for one plan:

When CSR reimbursements were defunded in 2017, issuers recovered the cost by loading it into silver
premiums. Because the load only needs to cover CSR-eligible enrollees — who are on-exchange by
definition — issuers in many states file **off-exchange-only silver "mirror" plans without the
load**: separate HIOS ids, near-identical coverage, materially cheaper. That produces exactly the
observed shape, including the **identical ratio at both ages** (a uniform load, with the age curve
holding on both rails as `RateCacheWarmService` already notes).

### Three consequences

1. ⭐ **T44/V078 is systematic, not defensive.** If this is silver loading, the off-exchange LCSP
   understates the true ICHRA-affordability LCSP in **every silver-loaded market, always**, and always
   in the dangerous direction (a too-low LCSP makes an unaffordable offer look affordable). V078 is
   load-bearing infrastructure, not a belt-and-braces column.
2. ⭐ **It is a genuine selling point for the off-exchange rail, and nothing in the pitch says so.**
   For an **unsubsidized** employee the off-exchange silver mirror really is ~44% cheaper than its
   on-exchange twin. That is real money, and it is an argument for the off-exchange ICHRA model that
   `swbd_premiumpath.md` and the capability map do not currently make.
3. ⭐ **It explains *why* the population splits so hard.** The same mechanism that makes off-exchange
   cheaper for the unsubsidized **inflates the on-exchange benchmark silver plan, which inflates
   APTC** — making on-exchange more valuable for the subsidized. `ichra_administration_scope.md`
   records *that* the population splits; this is the quantitative reason.

### The test, and it is two calls

**Unrun.** Quote Hopkins at age 21 with `off_ex: true` and again with `off_ex: false`, intersect the
plan lists on `hios_id`, compare `gross_premium` for matched plans:

- **Matched plans price identically** → the single-risk-pool rule holds, the whole gap is catalog
  difference (off-exchange-only silver mirrors), and the on/off overlap percentage — never measured —
  falls out of the same call.
- **Matched plans price differently** → the reasoning above is wrong and needs investigating before
  any affordability figure ships.

`RateCacheWarmService.canaryCheck` (`:452-491`) already does exactly this shape — builds a
`Map<hiosId, premium>` from a second response and compares plan-by-plan. Tracked as **T151**.

⚠️ **Also unmeasured: how much the two catalogs overlap at all.** The only figures on record are
**65 off-exchange vs 45 on-exchange** in Hopkins — and the 45 dates from 2026-07-28, before the
pagination finding. Counts do not give overlap; those numbers are consistent with near-total overlap
or with substantial disjointness in both directions.

## What staging data is, and what may not be claimed about it

**[verified] Staging returns real rate data, not synthetic.** Corroborated twice against independent
external baselines — `$582.78` to the penny against both the zizzl CSA figure and HSOne, and the
Hopkins 65-plan / BCBS 24 / CHRISTUS 18 / UHC 23 split reproduced exactly.

**Not established, and worth stating precisely because the inference is easy to make:**

1. **Staging has never been compared to production**, because production 403s. Every parity check is
   staging-versus-*third party*. **"Staging equals production" is a well-supported inference, not a
   measurement.**
2. **Staging's refresh cadence is unknown.** It could be a periodic snapshot rather than a live
   mirror. Within a plan year that is nearly harmless — annual rates do not move. **At plan-year
   rollover it matters**, since PY2027 could appear in one environment before the other. Never asked.
3. **Catalog completeness on staging rests on one county.**
4. **Non-quoting surfaces genuinely differ** — the staging deeplink is a separate host behind Basic
   Auth, webhooks have their own sandbox, and staging enrollments produce no real policies.

### ⭐ The provenance gate is an accountability gate, not a data-quality gate

**This reframe matters and is not recorded anywhere else.** The red banner and the
`source_env = 'PRODUCTION'` checks are **not** protecting anyone from wrong numbers — on the evidence
the staging numbers are very likely right. They protect the **accountability trail**: if SSA puts a
premium in front of an employer it must be able to say where it came from and stand behind it, and
*"fetched from a staging environment we were told is real"* is not that.

**Two things follow.** The fix is **production allow-listing** (T136), an accountability change — not
a data-quality investigation, and not a loosened check. And it is an *additional*, independent reason
not to relax the gate, alongside the ones already settled in session 10.

## HSOne and the ICHRA Partner API are two APIs over one data set

**[verified]** The 2026-07-30 rate-parity test recorded it directly: *"the two products return the
same underlying market data for this county."* Same plan counts, same carrier split, same
`$582.78`. What differs is the surface — host, path, request nesting, field spellings, response
shape, and feature scope (HSOne being, per KJ Sherman, *"simplified versions of the ICHRA APIs"*).

**[domain] Neither product is the source of this data.** ACA individual-market rates are carrier
filings, submitted to state DOIs and CMS, approved and published. HealthSherpa reads them. So does
zizzl. So does Ideon. **That is why three independent vendors agree to the cent.**

⭐ **Strategic consequence, worth stating plainly because it changes how the dependency should be
weighed:** SSA is **not** buying access to proprietary data.

- **No vendor can out-quote another on accuracy.** The numbers are the numbers.
- **The differentiation is catalog completeness and who controls it** — which is precisely where
  zizzl failed Forrest. They did not lack Christus data; they chose not to show it.
- **It lowers the risk half of the HealthSherpa dependency.** `healthsherpa.md` calls the dependency
  *"a conscious strategic bet"* — fair, but the **rate-data** half of that bet is low-risk, because
  the underlying filings are public and another vendor reads the same ones. What would genuinely have
  to be re-integrated if HealthSherpa vanished is the **enrollment rail**, not the rate data.

## Related to this section

- `docs/analysis/project_backlog.md` — **T151** (on/off overlap and same-plan price probe),
  **T152** (refresh cadence and full-state warm), **T148** (`AgeCurve` 2027), **T146**
  (`GET /api/v1/plans`)
- `docs/deployment_backlog.md` — **D-83** (county list, now not cost-constrained), **D-84**
- `docs/migrations/V078__rate_cache_onex_lcsp.sql` — the probe numbers reproduced above
- `docs/business/ichra_administration_scope.md` — MEC/subsidy segmentation

---

# 2026-09-10 — Public docs recheck and correspondence status

**Source:** public docs at `docs.ichra.healthsherpa.com`, read 2026-09-10. Everything here is
**docs-stated**, not runtime-verified.

**Supported Carriers (page last updated 2026-08-24).**
- **BCBS TX:** QuoteConnect, Deeplink, EnrollConnect, and Submission Confirmation live. Policy Status
  is still marked coming in 2026. O16 is open and has been reframed to 2027-01-01 effective dates.
- **CHRISTUS (LA, TX):** QuoteConnect, Deeplink, EnrollConnect, and Submission Confirmation live.
  **No Policy Status entry**, which means not listed rather than marked as coming.
- **Cigna, Oscar:** Policy Status live **without payment info**.
- **Texas off-exchange carriers listed:** Ambetter, BCBS TX, CHRISTUS, Cigna, Molina, Oscar, UHC,
  Wellpoint.
- **EnrollConnect:** the docs state support for all carriers and states is expected before PY2027
  open enrollment, with NJ and NY excluded from the initial launch.

**Webhooks API.**
- Configuration is still routed through the account manager or onboarding contact via a form.
  **O12 gates this.**
- Authentication is documented only as a variety of supported methods. **No documented** retry
  policy, delivery guarantee, ordering, `transaction_id` idempotency, signature verification, or
  source IP range. **O15 stays open.**
- The off-exchange payload example carries a `payment` block that includes `payment_status`,
  `grace_period_start_date`, and `paid_through_date`. This is relevant to monthly coverage
  verification. **Docs-stated only; carrier-dependent.**

**Contacts.**
- ⭐ **Michael Levin** (`michael.levin@healthsherpa.com`): **SVP & General Manager, ICHRA**, per his
  2026-09-10 email signature. Julian cc'd him on 2026-07-29. He is SSA's most senior HealthSherpa
  contact.
- ⚠️ **Dead addresses; do not use.** `ichra@healthsherpa.com` bounced on 2026-09-10 (550 5.1.1). It is
  named in HealthSherpa's 2025-08-05 Policy Status API press release. `ichra_support@healthsherpa.com`
  also bounced on 2026-09-10 (550 5.1.1). It is named in an undated agent help-center article.

**Correspondence.** The last written HealthSherpa reply was Julian Ferdman on 2026-07-29. Unanswered
SSA sends: 2026-07-29 (×2), 2026-07-30, 2026-08-05, 2026-09-10. The 2026-09-10 escalation delivered
to Julian, KJ, and Michael. The only response was Michael's auto-reply (at ACA Summit, slow through
2026-09-11). See `swbd_ichra_build_plan.md` §6.
