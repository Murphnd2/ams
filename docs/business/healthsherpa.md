# HealthSherpa — ICHRA quote/enroll integration partner

**Type:** Integration partner (infrastructure / EDE rails) · **Status:** Evaluation (not yet a project)
**Surfaced:** 2026-07-28, via the SWBD ICHRA-admin thread (zizzl / Sandoval — see `swbd_premiumpath.md`).
**Last updated:** 2026-07-28 (contract + `api_enrollable` session)

## Summary

HealthSherpa is a CMS-approved Enhanced Direct Enrollment (EDE) provider and the leading **connectivity
layer** for the ICHRA market — API-first infrastructure connecting carriers with ICHRA platforms for
quoting, enrollment, and compliance.

> **Note:** the product SSA is integrating is the **off-exchange ICHRA Partner API**
> (`docs.ichra.healthsherpa.com`), **not** EDE and **not** HSOne. See the 2026-07-29 product
> correction section below before relying on any endpoint detail in this document.

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
