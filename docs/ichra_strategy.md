# ICHRA at SSA — Strategy, State, and Sequence

**Status:** Active — the strategy entry point for all ICHRA/HealthSherpa work
**Created:** 2026-07-31
**Owner:** Kevin
**Baseline:** branch `refactor/modernize-architecture`, HEAD `935c31e`, migration **V076**, release **v0.76.00**
**Related:** `docs/business/healthsherpa.md` · `docs/analysis/plus_tier_build_plan.md` · `docs/business/plus_tier.md` · `docs/business/swbd_premiumpath.md` · `docs/business/ichra_administration_scope.md` · `docs/business/ichra_platform_capability_map.md`

---

## How to use this document

**Read this first.** Before the build plan, before any tactical handoff, before the backlogs, before any code.

- **It orients; it does not specify.** Every section ends by naming where the detail lives. If you need field lists, migration bodies, or per-phase scope, follow the pointer — do not expect to find them here.
- **It is a synthesis of twelve documents**, several of which supersede each other in non-obvious ways. §15 is the provenance key. Read it before trusting any dated claim in the sources.
- **It carries dates because it will go stale.** Anything undated in a strategy document reads as timeless and is wrong within a month. Where a claim can rot, it says when it was true.
- **Verified and assumed are marked separately, everywhere.** The single worst failure mode in this document set has been assumptions that read as findings.
- **It is short on purpose.** Fifteen minutes end to end. A sixth document duplicating the other five drifts from them within weeks; a short one that points at them does not.

Sections 1, 4, 5 and 16 are the ones that change how a session behaves. If you read nothing else, read those.

---

## 1. The thesis

PremiumPath was built for SWBD, and Forrest Huggins is interested — but it does not do much that is *new* for him. It is a post-tax payroll mechanism for paying individual premiums while keeping employer money out of the flow: elegant compliance plumbing, and plumbing is not leverage. His recent discussion indirectly asked for more, and **he named HealthSherpa himself**.

**HealthSherpa data has exposed better ways to provide utility to agents** — SWBD's, and others working the individual market. **If AMS provides that utility, SSA gets the ICHRA administration business.**

**Agent utility is the wedge. Administration revenue is what it earns.**

The corollary governs sequencing: build the things that help an agent *win a case* before the things that help SSA *administer one*. Sales tools win the large cases; automation wins the small ones; Forrest's book has both. Lead with the sales layer to win the partnership and let the admin layer land underneath while the relationship forms.

### What would falsify it

- Agents use the tools and it changes nothing about who wins the case — utility that does not convert is a cost centre.
- Forrest wanted administration all along and the quoting remark was conversational — see §2, because this is a live risk, not a hypothetical.
- A competitor bundles equivalent tooling free with administration, collapsing the wedge into table stakes.
- The economics do not clear at the small end. Setup labour is a hard floor: Summit provides no import template for creating Premium Billing benefit plans, so **every group needs at least two benefits hand-created**, and no AMS work removes that. The pitch is *ongoing administration is automated*, never *setup is cheap*.

**Detail:** `docs/business/ichra_platform_capability_map.md` (the five-layer framing and why Layers 1 and 5 are the defensible ones); `docs/analysis/plus_tier_build_plan.md` Part 3, "On durability."

---

## 2. The evidence base, graded

The thesis rests on a small number of calls and emails. Grade before you build.

| Conclusion | Source | Weight |
|---|---|---|
| SSA can administer QSEHRA + individual list billing for this model | Documented call **2026-07-13** (with Annette Bechtold) | **Solid** |
| Management-Presidio-post-tax + staff-minimal-QSEHRA + card model | Documented call **2026-07-14** (Forrest, Tracy, Kevin; transcript referenced) | **Solid** |
| Package delivered; Forrest holds a scoped agent login on `premiumpath.net` | Send record **2026-07-15** | **Solid** |
| zizzl gated carriers (turned off Christus, refused Forrest's carriers); ~**$660/mo minimum** on a **3-employee** group (Sandoval Process Solutions, Hopkins TX, 9/1/26) | Documented call **2026-07-28** | **Solid** |
| HealthSherpa quotes the same source-of-truth rates zizzl did — BCBS Blue Advantage Silver HMO 306, HIOS `33602TX0460776`, **$582.78 to the penny** | Live API test **2026-07-28**, re-confirmed on the correct product **2026-07-30** | **Solid — verified twice** |
| AOR travels **per application, keyed on NPN** — SSA is structurally not competing for the policy | Julian Ferdman (HealthSherpa), quoted directly, **2026-07-29**, plus docs confirmation | **Solid** |
| ⚠️ **"Forrest is focused on integration and raised HealthSherpa"** | **One sentence, one call (2026-07-28).** No written ask, no requirements, no scope | ⚠️ **Thin — and load-bearing for everything in §3** |
| SWBD will share its group book (A4b's gate, O24) | Nothing. An assumption in the plan | ⚠️ **Untested** |
| Forrest expected a "setup video" | Recorded and correctly discounted: *"an offhand remark in his closing recap, not a firm request"* | Discounted |
| The "minor §125 opportunity" from 2026-07-28 | *"details TBD"* — recorded, never elaborated | Unsized |

### The uncomfortable part

**The entire agent-utility thesis descends from one sentence in one call.** That does not make it wrong — the two zizzl failure modes (carrier gating, small-group cost) independently corroborate that *quoting* is where his pain lives, and he called about quoting rather than about the card. But:

- **Nobody has asked Forrest what he would want a quoting tool to do.**
- **O22** — book profile: counties, group-size distribution, carriers, renewal-date distribution, and **producing-agent count** — has been open since the partnership reframe.

Both are one email. **That email is the cheapest de-risking available anywhere in this plan**, and it also unblocks the county list in §5. It has not been sent as of 2026-07-31.

**Detail:** `docs/business/swbd_premiumpath.md` (relationship, history, pricing, compliance guardrails, the stack-wrap track).

---

## 3. What an agent can do that they cannot today

Ranked by **agent utility per unit of build effort**. "Credential" means a HealthSherpa API key must reach a live call; the illustration read path never calls the API, so cache warming is the entire credential exposure for row 1.

| # | Capability | What the agent says to a prospect | Data | Credential | Build |
|---|---|---|---|---|---|
| **1** | **Rating-area illustration** — *built, deployed, inert* | *"Hopkins County, 14 lives: the practical floor is $412 at 21, $588 at 40, $1,236 at 64. Sixty-five plans, three carriers. Rates as of this morning."* | ✅ Verified, cached | Warm only | ✅ **Shipped** |
| **2** | **Age-band net-cost table** (AGE_BAND mode) | *"Your four under-30s, seven forties, three over-55 — here's each band's cost and your monthly total at a $400 contribution."* | ✅ Verified — **every age 21–64 is already cached**; the servlet reads three of them | **None** | **Small** — servlet + JSP branch |
| **3** | **Affordability threshold per employee** | *"At $350 your offer is unaffordable for Maria, she keeps her subsidy and comes out ahead. At $450 it's affordable, she loses the credit, she's worse off. Here's the exact flip point — for each of your fourteen."* | ⚠️ **Cached LCSP is off-exchange only (T44); ICHRA affordability needs the on-exchange LCSP** | 2 calls to fix T44 | **Small–Medium** |
| **4** | **Group-to-ICHRA conversion analysis** (A4a) | *"Your renewal is $9,840/month, up 14%. ICHRA at $430/head is $6,020, and eleven of your fourteen come out ahead."* | ✅ Verified — cache + census + the group premium the agent already knows | **None** | **Small–Medium** |
| **5** | **Subsidy segmentation** | *"Nine of your fourteen are subsidy-eligible — QSEHRA preserves their credit. Five aren't — ICHRA serves them fully. That's which product you're buying, before you commit."* | ⚠️ `POST /api/v1/aptc_estimates` **documented, never called**; the income request field on this product is **unverified** | **Yes** | **Medium** |
| **6** | **Design advisor** (chatbot skill) | *"Does my client's dental plan kill the QSEHRA?"* — answered with citations, never plan selection | ✅ Content and infrastructure both exist (V046/V063/V065) | **None** | **Very small** |
| **7** | **Class optimization** | *"Split salaried from hourly. $520 and $310. That's $1,140/month less than a flat $450, everyone lands within $40 of where they are, and both classes clear the minimum-size rule."* **ICHRA permits classes; QSEHRA forbids them** — this is an ICHRA-only lever and one of the few places ICHRA is structurally better than what they have | ✅ Cache + design census. O38 suggests Summit supports division-scoped contributions natively | **None** | **Medium** |
| **8** | **Provider check** — *"will I lose my doctor?"* | *"Give me your three doctors. Thirty-one of the 65 plans cover all three; here's the cheapest."* The first question every ICHRA employee asks, and a common reason employers decline | ✅ O23 resolved favorably — request takes `providers[]`, plans return `covered` + `covered_addresses`. ⚠️ **But the API takes NPIs, not names — see the gap below** | **Yes** (uncacheable) | **Medium** + gap |
| **9** | **Renewal-defense radar** (A4b) | *"Eleven groups in your book renew in the next 120 days. Here are the four where ICHRA beats the renewal, numbers already run."* Before the conversation, not during it | ✅ AMS already does this shape (`Benefit.nextRenewalDue`, `RENEWAL_DAYS_OUT`, `fillRenewalEmployers()`) | **None** | **Medium–Large** |
| **10** | **Pipeline console** (A5) | *"Here's your whole network's pipeline, by sub-agency and agent."* | ✅ `Opportunity` + V070 hierarchy + V071 tokens + `illustration_log` | **None** | **Medium** (mostly views) |
| **11** | **Enrollment lifecycle** (B5/B7) | *"And when they say yes, they enroll from here and I watch the policy."* | ✅ Deeplink + webhooks confirmed; ⚠️ policy status is **carrier-gated** | Yes + BAA + rep | **Large** |
| **12** | **Book console + alert engine** (C2) | *"SEP window closing on two. One submitted and never effectuated. One terminated mid-year."* *The* differentiator per the capability map | Needs live policies | Yes | **Large** |

**Rows 1–4 need no HealthSherpa approval, no BAA, no counsel, and no Gate 0. Rows 1–2 need no new API call at all.**

### ⚠️ One gap with no owner

A2 is specified as *"enter physicians, see which plans include them."* The ICHRA Partner API accepts **NPIs**. The provider *name-search* endpoint (`GET /v1/reference/providers?query=`) is **HSOne's** — it appears in `healthsherpa.md`'s 2026-07-28 endpoint inventory, which the 2026-07-31 section supersedes, and this product's documented endpoint set is quoting, APTC estimation, enrollment deeplink and webhooks only. Between *"agent types a doctor's name"* and *"API wants an NPI"* sits net-new work that A2's sizing does not contain. Solvable — the NPPES registry is public — but **currently owned by nobody and estimated by nobody** (identified 2026-07-31).

**Detail:** `docs/business/ichra_platform_capability_map.md`; `docs/analysis/plus_tier_build_plan.md` Part 3 (A1–A6) and Part 6 (the A4a/A4b split).

---

## 4. What is actually built, and what a user can do with it right now

**Built and deployed to production** (release `v0.76.00`, 2026-07-31 11:37; `update.sh` applied V074/V075/V076 in order, verified against the production database afterward — `county_reference` = 254 rows, `48223` → Hopkins County, `schema_info` reports V076):

| Piece | Path | Exercised against real data? |
|---|---|---|
| **V074** `rating_area_rate_cache` | `docs/migrations/V074__rating_area_rate_cache.sql` | Table live in production. **Zero rows.** |
| **V075** `illustration_log` (no PII) | `docs/migrations/V075__illustration_log.sql` | Live. **Zero rows.** |
| **V076** `county_reference` — 254 Texas counties | `docs/migrations/V076__county_reference.sql` | ✅ **Yes** — verified in production |
| `HealthSherpaService` | `data/service/HealthSherpaService.java` | ❌ **Never** |
| `RateCacheWarmService` | `data/service/RateCacheWarmService.java` | ❌ **Never registers** — its constant is unseeded |
| `AgeCurve` | `data/util/AgeCurve.java` | ❌ Never run in AMS |
| `RateCacheAdmin` + `rateCacheAdmin25.jsp` | `controller/admin/`, `view/a/admin/` | Reachable; shows an empty cache |
| `IllustrationServlet` + `illustration25.jsp` | `controller/market/`, `view/market/` | ❌ Cannot reach a result state |
| `RateCacheDAO`, `CountyReferenceDAO`, 3 entities | `data/dao/`, `model/market/` | Partially — county reads work |

### What a user can do today: **nothing**

An agent logs into production, opens **Sales ▸ ICHRA Illustration**, and sees:

> *"Rate cache is not configured on this installation."*

`RATE_CACHE_PLAN_YEARS` is unseeded, so the servlet forwards straight to the empty state. Even if it were seeded, the cache is empty: the warm job never registers, and could not call out if it did. **No installation has ever authenticated to HealthSherpa** — verified 2026-07-31 against local dev's `constant` table and `ssa.properties`. Every HealthSherpa result on record was obtained out-of-band with a manually supplied key.

**They also cannot:** run an age-band or per-employee illustration, compute affordability, segment by subsidy, design classes, check providers, run a conversion analysis, upload a census, enroll anyone, or verify a month of coverage.

**Detail:** `docs/analysis/migration_tracker.md` (V074–V076 rows and the 2026-07-31 deployment note); `docs/deployment_backlog.md` D-78/D-79 verification notes.

---

## 5. The five constants — the config-to-capability gap

**This is the single most important operational fact in this document.**

Roughly **1,500 lines of shipped code, three migrations applied to production, and one deployed release** currently produce a page that says *"not configured."* The distance to a working agent tool is **five `constant` rows and one warm cycle**. No build. No migration. No deployment. No external approval.

| Constant | Backlog | Value |
|---|---|---|
| `HEALTHSHERPA_BASE_URL` | **D-79 (HIGH)** | `https://api.ichra-staging.healthsherpa.com` |
| `HEALTHSHERPA_API_KEY` | D-78 | Key issued **2026-07-30** — authenticates against staging; **production 403s pending allow-listing** |
| `RATE_CACHE_WARM_ENABLED` | D-82 | `true` on production; `false` on master and BPO |
| `RATE_CACHE_COUNTIES` | D-83 | Bare FIPS preferred, e.g. `48223`. ⚠️ **The county list is an unmade business decision** |
| `RATE_CACHE_PLAN_YEARS` | **D-84 (HIGH)** | `2026`; **`2026,2027` from 1 November** |

As of T43 (2026-07-31), `AppConfig.getHealthSherpaBaseUrl()` has **no default** — absent or blank means HealthSherpa is not configured on that installation, `HealthSherpaService` refuses the call, and the warm job logs and skips. The operator chooses staging or production deliberately, per installation. This is deliberate fail-closed behaviour, not a bug.

### Four things to settle before an agent shows a client a number

1. **Which counties.** D-83 records it plainly: *"the actual county list is a business decision, not yet made."* It is gated on O22 (§2) — or on a deliberate guess.
2. **Staging provenance is invisible to the agent.** `source_env` is displayed on the admin page and **nowhere on the illustration page**. Staging returns *real* rate data, not synthetic, so the numbers are right — the disclosure is what is missing. One line of JSP. **T48** goes further: `source_env` is excluded from both the delete scope and the unique key, so staging and production rows for the same county silently overwrite each other. **It is advisory provenance, not a partition and not a safety mechanism.**
3. **T47 — 29 of 254 Texas counties have a representative ZIP whose ZCTA crosses a county line** (lowest containment 0.6301, Reagan County). Benign failure: HealthSherpa rejects the pair and the county caches nothing. **Bad failure: HealthSherpa silently returns the ZIP's actual county's rates under our requested FIPS, and wrong rates cache as authoritative.** Verifiable the moment a credential exists; blocked on D-78/D-79 today.
4. **⚖️ O25** — carrier names on SSA-built displays (§7). The current page names no carriers, only counts, so it is probably on the safe side; that has not been confirmed.

**Detail:** `docs/deployment_backlog.md` D-78, D-79, D-82, D-83, D-84; `docs/analysis/project_backlog.md` T43, T47, T48.

---

## 6. The verified HealthSherpa data surface

⚠️ **The product being integrated is the ICHRA Partner API** (`docs.ichra.healthsherpa.com`). **Not HSOne. Not EDE.** This distinction reversed twice in the record; the current answer is settled and confirmed by HealthSherpa's own product management (Julian Ferdman, 2026-07-29). Everything HSOne is fenced at the bottom of this section.

**Verified against live staging 2026-07-30 and 2026-07-31.**

- **Endpoint:** `POST {baseUrl}/api/v1/quotes` — **note the `/api` segment.**
- **Auth:** `x-api-key` header. Backend only; never in browser code, frontend env vars, or logs.
- **Environments:** production `https://api.ichra.healthsherpa.com` · staging `https://api.ichra-staging.healthsherpa.com`.
- **Body is flat.** Required: `zip_code`, `fip_code`, `applicants[]` (each `age`, `relationship`, `smoker`).
- **Response is flat**, enveloped as `{ "plans": [...], "meta": { "result_count": N } }`. Top level: `hios_id`, `name`, `metal_level`, `plan_type`, `hsa_eligible`, `state`, `year`, `csr_level`, `gross_premium`, `ehb_premium`, `subsidy_applied`, `premium`, `ichra_only`, `deeplink_enrollment`, `api_enrollment`. Nested: `issuer`, `networks[]`, `cost_sharing`, `benefits`, `urls`, `gross_premium_per_applicant`, `providers[]`, `drugs[]`.

### ⚠️ Field-naming traps

**`fip_code` — NOT `fips_code`. `smoker` — NOT `uses_tobacco`.**

Older sections of `healthsherpa.md` state the **opposite** for both. Those sections are correct for HSOne and wrong for this product. **Do not "correct" working code to match them.** `HealthSherpaService.java` is right as written and carries a Javadoc note saying so.

### Three gotchas, each of which cost real work to find

1. **The age curve is statutory and exact.** Premiums follow the federal uniform age rating curve; carriers cannot deviate. Confirmed across two carriers, five ages, three separate calls: one base rate at age 21 reproduces every observed premium **to the cent**, and **age 64 ÷ age 21 = 3.000 exactly**. Plan *rankings* are age-invariant too, so LCSP, benchmark silver and lowest bronze computed once at 21 hold at every age. **Forty-five calls per county collapse to one.** Caveats: tobacco is a separate plan-specific load (capped 1.5×), not part of the curve; the curve is plan-year scoped; state-specific curves exist and Texas uses the federal default.
2. **The plan *set* is not age-invariant.** ACA catastrophic plans are restricted to enrollees under 30, so aggregates for ages 30+ must exclude them. **`market_low_premium` legitimately steps up between the age-29 and age-30 rows. That discontinuity is correct — do not "fix" it.**
3. **Pagination silently truncates.** Default `per_page: 20`, and `meta` carries **only** `result_count` — no total, no page count, at any page size. **A truncated response is indistinguishable from a small market.** In the reference county that meant 20 of 65 plans and **zero Gold**. Completeness can only be established by paginating until a short page returns.

**Never send a `metal_levels` filter.** The request enum omits **"Expanded Bronze"** — 15 of 65 plans in the reference county, including some of the cheapest. Pull unfiltered and classify client-side.

### Reference county

Hopkins TX, ZIP **75482**, FIPS **48223**, plan year 2026, off-exchange, age 40 → **65 plans**. BCBS 24 / CHRISTUS 18 / UnitedHealthcare 23. Silver 27, Gold 19, Expanded Bronze 15, Bronze 4.

### Also true, and structural

- **No ZIP→FIPS endpoint exists on this product.** The reference/lookup endpoints were HSOne's. AMS carries its own county data — that is why V076 exists.
- **Public schemas are intentionally open and evolve additively without version bumps.** Lenient deserialization is a **hard requirement**, not a preference: ignore unknown properties, assume new optional fields, never treat a response shape as closed.
- **Two error envelopes.** Branch on body shape and `code`, **not** on HTTP status.
- **Enrollment cannot be fully headless** — carrier payment is a browser form POST. Any enrollment flow ends by handing the member off, and some will never pay.
- **AOR travels per application, keyed on NPN.** Not fixed per API key. This maps cleanly onto SWBD's downline and means **SSA is structurally not competing with an agency's agents for the policy** — say that out loud in the pitch.

### 🚧 HSOne — fenced

`api.one.healthsherpa.com`, `POST /v1/quotes` with a nested `context`/`location`/`household` body, `fips_code`, `uses_tobacco`, `api_enrollable`, `pricing.gross_premium`, `plan_id`, `GET /v1/reference/*`, the 42-of-65 Hopkins enrollability count, and the `employer_external_id` + `updated_since` polling design are **all HSOne**. They are accurate about HSOne and do not describe this product. The HSOne portal request submitted 2026-07-28 was left in place deliberately as a costless fallback.

**Detail:** `docs/business/healthsherpa.md` — read its **2026-07-31**, **2026-07-30** and **2026-07-29** sections, in that order, before any earlier text.

---

## 7. Compliance boundaries that constrain design

### Settled — treat as rules

- **HIPAA / PHI routing.** Any feature touching PHI routes AI calls through **AWS Bedrock** (BAA-covered), never the standard Anthropic key. Non-PHI features may use the standard key.
- **No plan-selection advice.** Guidance on which plan to choose is agent territory; route those questions to the licensed agent. Educational content about *how ICHRA works* is SSA's to write.
- **ERISA safe harbor — presentation rules.** Individual policies stay outside ERISA only if enrollment is voluntary and the employer does not select or endorse any particular issuer or plan. Any plan display SSA builds must therefore be **complete, neutrally ordered, with employee-controlled sort and filter — no "recommended" badge, no default selection, no curation, no hidden carriers.**
- **Substantiation must verify coverage *type*, not just coverage *presence*.** Short-term plans, fixed indemnity and health care sharing ministries are all sold off-exchange and **none of them are MEC**. Reimbursements paid against non-MEC coverage are taxable and the employer carries the exposure.
- **ICHRA and QSEHRA are mutually exclusive at the employer level.** A QSEHRA requires the employer offer no other group health plan, and an ICHRA **is** a group health plan. Not a per-class menu.
- **MEC narrowing.** QSEHRA accepts any-source MEC (spouse, parent, Medicaid, Medicare); ICHRA requires individual-market coverage or Medicare. **One employee on a spouse's group plan can invalidate an ICHRA design that would have worked as a QSEHRA.**
- **`CoverageStatus` is not a valid compliance data source** — billing-driven, no backfill, no run log. Compliance reporting sources from Summit exports.
- **Agent markup confidentiality.** Base price and markup never appear in public proposal HTML; sell price only.

### ⚠️ Pending counsel — do not build against these as settled

| # | Question | Gates |
|---|---|---|
| **O25** | **Carrier names.** The endorsement rule says keep carrier names off all SSA-drafted paper; the ERISA safe harbor *requires* a complete, neutral list, which necessarily names carriers. Proposed reconciliation — names may appear in neutral employee-facing market displays but not in SSA-drafted documents, notices or marketing — is **recorded in the rules document itself as not yet confirmed** | **Every display SSA builds, including the one in production today** |
| **O18** | Does a **signed employee attestation** suffice as a reimbursement-release record where no carrier status feed exists? | **B3, terminally** — see §14 |
| **O17** | The 90-day notice and its newly-established-plan exception | Whether *any* short-runway effective date is achievable |
| **O19** | Is the ICHRA affordability LCSP **tobacco-loaded**? | Whether V074's unused `uses_tobacco` column is ever populated, and whether a person-level tobacco field is needed at all |
| **O20** | Is SSA building a plan display **on the employer's behalf** itself an endorsement problem? | B5's UI |
| **O21** | The standing docket — QSEHRA SEP window, §213(d) filed-form test, MEC floor, PCORI, 1095-B/1094-B, §105(h) NDT, state continuation on ICHRA loss, W-2 (**not** Box 12 Code FF), Tex. Ins. Code ch. 4151 TPA certificate | Annual-compliance phases, mostly downstream |

**O18, O25 and O17 should go to counsel as one package.** They gate work three phases out and counsel is the longest lead in the plan.

**Detail:** `docs/analysis/domain_and_compliance_rules.md` (the standing rules, including O25's self-declared tension); `docs/business/ichra_administration_scope.md` (the ⚖️-marked end-to-end service scope).

---

## 8. Out of scope — by decision, not oversight

These are **settled and closed**. They are not gaps, they are not backlog, and they should not be re-analyzed or "improved." The developer owns them operationally and they work.

| Area | Why it is closed |
|---|---|
| **DataPath Summit configuration** — plan setup, benefit setup, notice settings, import conventions | Deep operational expertise, exercised daily on live clients. Questions *about* existing Summit capability (does J7 carry X, do imported status changes trigger letters) are in scope; proposals to change the configuration are not |
| **The AMS billing pipeline** — `MonthlyBiller`, `BillingGrid`, `billing_summary`, headcount-to-Wave, invoicing | D5 stands: "+" bills through the existing headcount path like every other LOS. AMS produces counts; dollars happen in Wave. **Do not let any open item pull dollars back into the billing model** |
| **PremiumPath Card mechanics** — funding, MCC restrictions, settlement, issuing bank | A DataPath Summit configuration in flight, not an AMS build |
| **Anything about how money moves** | SSA never holds participant funds. Carrier list billing is a counsel question before it is ever a feature question |

**The asymmetry is deliberate.** Summit and billing are known territory and off-limits. **HealthSherpa data is new to everyone, and that is exactly where analysis and proposals are wanted.**

Where the analysis legitimately *touches* these and stops: **D25** records that "+" billing depends on a full Summit round trip, so the first "+" invoice can lag setup by up to one import cycle — a fact about sequencing, not a request to change the pipeline. **D13/O9/O10** record an unresolved internal conflict over which MCCs are loaded (6300 versus 6300 + 5960) — noted, not proposed on.

---

## 9. The sequence

Ordered **buildable-soonest**, not by label. Gate types: **nothing** · **credential** · **vendor** (HealthSherpa) · **partner** (SWBD) · **counsel** · **developer**.

| Step | What | Gate | Agent-visible outcome |
|---|---|---|---|
| **0** | **Seed the five constants; run one warm cycle; verify via `/RateCacheAdmin`** | **Nothing** (staging key in hand) — plus a **developer** call on the county list | The illustration stops saying "not configured" and starts returning real market data |
| **1** | **Give ICHRA its own front door** — a top-level nav item and a hub page, instead of one `<li>` in the Sales dropdown | **Nothing** | ICHRA reads as a product area, not a calculator hidden under Sales |
| **2** | **Send the long-lead asks** (§10) — HealthSherpa rep + BAA; the O18/O25/O17 counsel package; the MOVEit folder; O22 and "send me three renewing groups" to Forrest | **Nothing** — an afternoon | None directly. This is the schedule |
| **3** | **AGE_BAND illustration mode** | **Nothing** | A per-band cost table instead of a range — the first output that looks like a *design* |
| **4** | **T44 — on-exchange LCSP** | **Credential** (2 calls) + **counsel** (O18/O25 on whether an indicative figure is held to the affordability standard) | Affordability numbers that are actually right — fix the input before building outputs on it |
| **5** | **A3 design deliverables** — subsidy segmentation, affordability thresholds, class structure, from a **design census** (age, ZIP, family tier, income band; **no SSN**, per D21, which keeps the whole sales stage outside the BAA question) | **Credential** for segmentation; O19 soft | A priced ICHRA design with per-employee impact — **and a design fee** |
| **6** | **A4a sample conversion analysis** | **Partner** — three to five renewing groups, an easy yes | Renewal versus ICHRA side by side. A case-winning conversation |
| **7** | **A6 design advisor** | **Nothing** | Agent self-service on ICHRA rules. Highest perceived value per unit of work |
| **8** | **T39 + A2 provider check** — resolve the NPI-lookup gap first | **Credential** + the §3 gap | The objection that kills cases, answered on screen |
| **9** | **A5 pipeline console** | **Nothing** | Downline pipeline by sub-agency and agent |
| **10** | **P1 SFTP + B4a notice register** | **Vendor/partner** (MOVEit lead time); B4a itself gates on **nothing** | None agent-facing — but it fixes a problem that exists **today**, for **existing** clients |
| **11** | **A4b book radar** | **Partner** — O24, earned by A4a | The pre-analyzed renewal queue |
| **12** | **Gate 0 → B1 catalog → B2 census → B3 ledger → B5 handoff → B7 webhooks → C1/C2** | **Developer**, then **counsel** (O18), then **vendor** | Administration, sold on the back of the utility above |

### Why Step 0 is first, and why it is not a build

The whole thesis is that agent utility earns the administration business. Right now the agent-visible utility is a page reading *"not configured."* Roughly 1,500 lines of production-deployed code are inert behind five configuration rows. **Nothing else on this list has anything close to that ratio of value to effort.**

### What is deliberately *not* next

- **Not B1 (the "+" catalog).** Gate-0-blocked, realistically weeks rather than days, and it sells *administration* — the thing the thesis says utility must earn first.
- **Not B2/B3.** B2 touches `SummitImportService` on the live production import path and **needs its own Phase A**. B3 is O18-terminal.
- **Not the enrollment rail.** Every gate is external and none has moved since 2026-07-29.

**Detail:** `docs/analysis/plus_tier_build_plan.md` Parts 3–8 for full per-phase scope, schema and deferrals.

---

## 10. The long-lead register

Everything with human lead time. **Its absence is the most visible gap in the current document set** — items were sent and no reply is recorded anywhere.

| Item | Owner | Asked | Reply |
|---|---|---|---|
| Onboarding representative assignment (**O12**) — routes staging credentials **and** the webhook configuration form. *"Nothing is testable until one is assigned"* | HealthSherpa | 2026-07-29 | ❌ **None recorded** |
| **BAA** — counterparty **Geozoning, Inc. DBA HealthSherpa** (**O13**). *"Unaddressed by anyone so far"* | HealthSherpa + counsel | 2026-07-28 (flagged), 2026-07-29 (sent) | ❌ **None recorded** |
| Production **allow-listing** — production returns 403 today | HealthSherpa | 2026-07-30 (follow-up open) | ❌ **None recorded** |
| BCBS TX policy status: **when** in 2026? (**O16**) — *"largely determines whether SSA builds for a 2026 or 2027 launch"* | HealthSherpa | 2026-07-29 | ❌ **None recorded** |
| CHRISTUS policy status — planned at all? Cell is blank, not ☑️ | HealthSherpa | 2026-07-29 | ❌ **None recorded** |
| Deeplink: **employee self-service or agent-completed?** (**O14**) | HealthSherpa | 2026-07-29 | ❌ **None recorded** |
| Deeplink versus EnrollConnect — which does HealthSherpa steer a TPA toward? | HealthSherpa | 2026-07-29 | ❌ **None recorded** |
| Webhook authentication methods supported (**O15**) | HealthSherpa | 2026-07-29 | ❌ **None recorded** |
| On-exchange account model for an unlicensed administrator; and the off-ex/on-ex contradiction | HealthSherpa | Drafted, **not confirmed sent** | — |
| **MOVEit folder creation** — ⚠️ *"requires a MOVEit administrator to set up: a lead-time item."* **No estimate exists anywhere** | DataPath | ❌ **Not asked** | — |
| Encryption key exchange (TPA key / DP key) for Data Exchange | DataPath | ❌ **Not asked** | — |
| Counsel package: **O18** (attestation sufficiency), **O25** (carrier names), **O17** (90-day notice exception) | Counsel | ❌ **Not sent** | — |
| **O22** book profile + producing-agent count | SWBD | ❌ **Not sent** | — |
| "Send me three groups renewing next quarter" (A4a's gate) | SWBD | ❌ **Not sent** | — |
| Program **name** direction; pilot case; the workflow flowchart Forrest offered; the §125 opportunity detail | SWBD | 2026-07-15 (name); rest open | Partial |
| **O24** — the full group book | SWBD | Deliberately deferred until A4a earns it | — |

**Six items sent to HealthSherpa on 2026-07-29 have no recorded response as of 2026-07-31. Four asks that gate later phases have never been sent at all.** Chasing these costs an afternoon and is the difference between waiting three weeks in August and waiting three weeks in October.

**Detail:** `docs/business/healthsherpa.md` "Open items — 2026-07-29" and "Outreach log"; `docs/analysis/plus_tier_build_plan.md` Part 2 (O12–O21) and Part 7's tiered question list.

---

## 11. Decisions settled — D1–D37

The **only place all 37 appear together.** They live in two files: **D1–D17** in `plus_tier.md`, **D15–D37** in `plus_tier_build_plan.md` (D15–D17 appear in both). One line each; the sources carry the rationale.

| # | Decision | Status |
|---|---|---|
| D1 | LOS granularity is `ICHRA+` / `QSEHRA+` only; add-ons are enhancements | ✅ conditional on Gate 0 |
| D2 | Single bundled PEPM; card included, no per-proposal add-on election | ✅ independently confirmed by C2 |
| D3 | No per-employee pricing at any stage | ✅ |
| D4 | Enhancement visibility controlled by rate-table pricing, not a flag | ✅ |
| D5 | Billing via the existing headcount export to Wave; both enrolled and eligible counts | ⚠️ **conditional** — see D25 |
| D6 | Quote inputs: ZIP + headcount mandatory, age bands optional | ✅ shipped as built |
| D7 | Full census collected post-sale at setup | ✅ |
| D8 | Census minimum: SSN, first name, last name | ⚠️ **may be void** — see D32 |
| D9 | Verification state lives in its own table, outside the billing pipeline | ✅ |
| D10 | Participant correlation via HMAC-SHA256 of normalized SSN | ✅ **survives** (Part 8, O34 negative) — but narrowed by Part 8's own finding |
| D11 | Raw SSN lifetime: intake → Summit export → discarded | ⚠️ may be moot (D32) |
| D12 | Employer correlation via `EmployerCustomID`, typed in at setup | ✅ |
| D13 | Card MCC-restricted to insurance codes; in flight, not pending | ⚠️ **internal conflict** — 6300 vs 6300 + 5960 (O9). Out of scope; recorded only |
| D14 | A "+" pricing proposal section with a flag selecting the render variant | ✅ |
| D15 | **ICHRA+ and QSEHRA+ are not symmetric on the enrollment rail** — ICHRA+ gets an SSA-mediated leg, QSEHRA+ does not | ✅ |
| D16 | **Off-exchange only at launch** | ✅ |
| D17 | Verification ladder: `ATTESTATION` → `CARD_TRANSACTION` (on O10) → `HS_POLICY_STATUS` (per carrier) | ✅ subject to ⚖️ **O18** |
| D18 | Reference-row delivery: migration `INSERT` vs `D-NN` items | ⏸ **deferred until Gate 0 runs** |
| D19 | Month-attribution rule for card verification | ⏸ dormant (O6/O10) |
| D20 | Outbound correlation key is a separate opaque UUID; **`ssn_hash` never leaves AMS** | ✅ |
| D21 | The pre-sale **design census** is a separate object from the post-sale administrative census | ✅ **what lets A3 ship years before the BAA resolves** |
| D22 | Illustration access is authenticated agent-facing first | ✅ **shipped** — slightly broader than specified |
| D23 | The group book lives in a new `agency_book_group`, not on `Prospect` | ✅ (O24) |
| D24 | Renewal-defense output goes to the **agent only**, never the employer | ✅ non-negotiable |
| D25 | "+" billing basis is a full Summit round trip; the first invoice can lag setup by one import cycle | ✅ (O29) |
| D26 | "+" participant ID convention and merge-sweep exemption | ❌ **resolved by construction, Part 5** — every "+" participant arrives on a positive Summit ID |
| D27 | Card reversals un-verify a month | ✅ dormant |
| D28 | **A2 is stateless** — physician names plus ZIP, live lookup, nothing persisted | ✅ |
| D29 | Census intake: PSP-staff upload **and** a one-time GUID drop, with PSP-side column mapping | ✅ settled Part 6 |
| D30 | The census file never goes through the standard upload path — parse in memory, never persist | ✅ softened by D32 |
| D31 | Intake utility permanent, with a single-participant mode | ⚠️ **REVERSED, Part 5 → Part 6.** Ongoing adds are **Summit-first**; only the setup census is AMS-first |
| D32 | SSN is sourced from Summit feeds, not from the census | ✅ gated on O31 |
| D33 | The notional COBRA-type benefit is a documented mechanism — and it is the **eligibility marker**, not merely a letter vehicle | ✅ |
| D34 | Any letter-triggering generated import requires preview-and-confirm | ⚠️ **REVISED, Part 6 → Part 7** — Summit's Process Approvals may supply it natively, making this configuration rather than AMS code |
| D35 | Participant Custom ID may retire the SSN hash entirely | ❌ **DOES NOT FIRE, Part 8** — `ParticipantCustomID` is in J2 but **not** in the mailing export (O34 resolved negatively) |
| D36 | Read benefit plan IDs from J7; do not assert them | ✅ (⚠️ O40 — likely needs a new column) |
| D37 | Summit Data Exchange: scheduled SFTP, both directions | ✅ designated **P1** |

**Reversal discipline:** within `plus_tier_build_plan.md`, **later Parts govern earlier ones**. Part 8 is current. Four decisions above are not what an unwary reader of Parts 1–5 would conclude — **D26, D31, D34, D35** — and all four are marked.

**Detail:** `docs/business/plus_tier.md` (D1–D17 with rationale); `docs/analysis/plus_tier_build_plan.md` Parts 2, 3, 4, 5, 6, 7, 8.

---

## 12. Open items by owner

Four buckets. **Self-answerable first, because that is the bucket that gets skipped.**

### ⭐ Bucket 1 — answerable today: no credential, no external party

| # | Item | Cost | Blocks |
|---|---|---|---|
| **O2** | Re-verify the enrollment/status API surface against the ICHRA Partner API. Public docs, `?ask=`, **no account needed** | **~1 hour** | The correlation map, the poll design, and **every HSOne-inherited assumption** |
| **O1** | **Gate 0 probe** — do live databases hold ICHRA/QSEHRA `LOS`, `ServiceItem`, `PlanType`, a *priced* `ServiceModule`→`RateTable` path, and a task sequence? Per environment. Read-only SQL, **script already written** | ~1 hour | **The size of B1** — days versus weeks |
| **O4** | Does the AMS import promote `DivisionName`? (staged and discarded today) | Code read | The ICHRA class model |
| **O32** | Does `SummitImportService` parse J3's SSN at read, or skip it? | Code read | Whether D32's hash-at-parse is free or new code |
| **O40** | Is `ImportPlanID` persisted anywhere in AMS today? | Code read | D36 |
| **T9** | Confirmed: `GenerateProp25` would silently drop a "+" selection in the internal Manual Setup path | Decision only | B1 — fix it, or forbid Manual Setup for "+" |
| — | How does AMS identify "+" employers before B1 exists — flag, manual list, or J7-derived? | Decision | B4a |
| — | **D18** — reference-row delivery mechanism | Decision | B1, partly gated on O1 |

*Adjacent — the staging key is in hand, but the call is made out-of-band, not through AMS:* **O3** (which carriers are actually in Hopkins off-exchange, settling the Ambetter discrepancy), **T42** (`include_non_enrollable_offex` semantics), **T44**'s empirical half, **T47** (the 29 low-containment counties).

**As of 2026-07-31 none of these has been done.** O2 in particular has been open since 2026-07-29 and is the umbrella over five separate stale assumptions.

### Bucket 2 — blocked on HealthSherpa (credential, allow-listing, or the company)

O12 (rep — **the tightest bottleneck**), O13 (BAA), O14 (deeplink self-service vs agent-driven), O15 (webhook auth), O16 (policy-status timing), production allow-listing, the developer-preview/SLA posture, and the on-exchange account model. **See §10 — six were sent 2026-07-29 with no recorded reply.**

### Bucket 3 — blocked on an external party ⚠️ long lead, start early

**SWBD:** O22, three renewing groups, O24, program name, pilot case, workflow flowchart, §125 detail. **All are emails; none have been sent.**

**DataPath / Summit:** MOVEit folder (**lead-time item, no estimate**), encryption key exchange, O26 (participant *update* imports; coverage-add and status-change imports for custom COBRA-type benefits; **do imported status changes trigger letters?** — *"everything downstream rests on it"*), O31, O29, O33, O37, O38, O7, O8, O11, O27, O28. **Several are observable rather than askable** — O5, O37, O38 — via `summit_notice_automation_discovery.md`, which is **written and has never been run**.

**Counsel:** O18 (terminal for B3), O25 (gates every display), O17, O19, O20, O21.

### Bucket 4 — stale, answered, or no longer relevant

O1-original (EDE consent — **dissolved as malformed**; there is no EDE transaction on the off-exchange rail) · O4-original (policy status off-exchange — **resolved unfavourably**: exists, carrier-gated) · **O23 resolved favorably**, A2 stays · O26/O31 largely resolved from documentation · O30 settled (all eligibles) · **O34 resolved negatively** · O35/O36 resolved and narrowed · **the AOR/TPA account model — resolved favorably 2026-07-29**, though `README.md` still names it the critical open question · the "webhooks unconfirmed" framing — corrected · the entire 2026-07-28 HSOne endpoint inventory, request shape, response field map, `api_enrollable` semantics and cost/access model — superseded, retained for provenance · **phase B0 — deleted by Part 5**; T32/T33/T34 revert to ordinary tech debt · backlog **#16** — no longer an epic, a pointer to #43 Phase B5.

**Detail:** `docs/analysis/plus_tier_build_plan.md` Part 2 (the O1–O21 replacement list and crosswalk), Parts 4–8 (O22–O40).

---

## 13. Additive discipline

**Standing rule: ICHRA work adds to AMS without modifying what exists.**

Audit across all seven ICHRA commits (`848a046`, `3b21439`, `d715384`, `eba17ad`, `b593045`, `fe6fd11`, `42caf4e`), performed 2026-07-31:

**Purely additive** — ten new Java classes, two new JSPs, three new migrations, two new URLs (`/RateCacheAdmin`, `/Illustration`), three new tables, one generator script. No existing table altered; `illustration_log`'s FKs to `assignee(id)` and `agency(agency_id)` are outbound references only. **This is the large majority of the work and it is clean.**

**Five pre-existing files touched:**

| File | Change | Verdict |
|---|---|---|
| `AppConfig.java` | Statics and methods **appended**; nothing existing changed | ✅ Fine — and T43 later removed a hardcoded production default in favour of fail-closed |
| `AmsDataGlobal.java` | Two independently-caught blocks in the startup path | ✅ Justified — matches the `ANTHROPIC_API_KEY` precedent rather than inventing a **fourth** credential-storage pattern in a codebase that already has three |
| `EmfListener.java` | Field + registration + shutdown, structurally identical to the existing scheduler blocks, **constant-guarded** | ✅ Fine — no other lifecycle hook exists |
| `navbar25.jsp` | Two single lines | ✅ Fine, though the Sales-dropdown placement is being revisited (§9 Step 1) |
| `InstallationHealthScheduler.java` | `stop()` now awaits termination | ⚠️ **The one genuine slip** — an unrelated, master-only subsystem changed inside a HealthSherpa commit. Good fix, wrong bundle |

**Explicitly verified untouched: every billing, import, proposal and sales DAO and service.** Also untouched: `LoginFilter`, `css-js.jsp`, `AmsDataLocal`, `ApiTokenFilter`, every existing migration.

**Two habits to carry forward:**

1. **Ship unrelated fixes alone.** The plan already applies this rule to T35; it applies here too.
2. **Prefer a new file to an edited one.** When a shared file must be edited, append rather than weave, and guard the addition so its absence is inert.

---

## 14. What would change this plan

| Trigger | Consequence |
|---|---|
| **O18 comes back negative** — a signed attestation is *not* a sufficient reimbursement-release record | **B3 fails and needs replacing before it is built.** The verification ledger's launch configuration has no alternative designed |
| **O14: the deeplink is agent-driven, not employee self-service** | **B5 becomes an agent workstation, not an employee portal.** An architectural fork, not a detail — it changes who the user is |
| **Gate 0 returns Branch B** (or A-minus, which Part 4 calls the realistic best case) | **B1 becomes weeks rather than days** — catalog creation plus ServiceItem, PlanType (QSEHRA is net-new code), task sequence, and the ICHRA compliance checklist |
| **O24 refused** — SWBD will not share its book | **A4b dies**, and the partnership question is answered cheaply. That is a useful outcome, not a failure |
| **BCBS TX policy status slips to 2027** | **Largely determines a 2026 versus 2027 launch.** Rural Texas off-exchange has no automated coverage verification without it; metro Texas and on-exchange do |

One more worth watching, from 2026-07-31: **if production allow-listing lands while any installation holds a key and no base URL**, the fail-closed behaviour introduced by T43 is what prevents a silent production call. Do not reintroduce a default.

---

## 15. Provenance index

### Which document supersedes which

- **`docs/business/healthsherpa.md` is authoritative on the API** — but read it **backwards**. Its **2026-07-31** section supersedes the 2026-07-28 response field map in full; **2026-07-30** corrects the request shape and the access model; **2026-07-29** corrects the product itself (ICHRA Partner API, not HSOne). Superseded text is retained deliberately — the framing reversed twice and the trail is worth keeping. **Any endpoint or field detail above those sections describes HSOne.**
- **`docs/analysis/plus_tier_build_plan.md` is canonical for build mechanics** — D18–D37, O1–O40, the phased build list. **Later Parts govern earlier ones. Part 8 is current.** Its own header states the precedence: Part 8 > Part 7 > Part 6 > Part 5 > Part 4 > Parts 1–3.
- **`docs/business/plus_tier.md` holds product intent and D1–D17.** It carries a provenance warning on itself: it was written without `healthsherpa.md` in context and several assumptions descend from HSOne-era findings. **Where they conflict, `healthsherpa.md` and the build plan are later.**
- **`docs/analysis/phase_a_ichra_enrollment_portal.md`** — every factual finding remains accurate; its *stakes* dropped on 2026-07-29 when the deeplink model replaced the AMS-collects-PHI premise.
- **Live state always wins over any document:** migrations → `ls docs/migrations/`; branches → `git branch -a`; per-environment migration state → `docs/analysis/migration_tracker.md`.

### ⚠️ The phase-numbering key — read this before touching any phase label

**Two systems, both using the letter "B," and they mean nearly opposite things.**

| System | Where it appears | Meaning |
|---|---|---|
| **A1–A6 / B1–B7 / C1–C2** | `plus_tier_build_plan.md` | **Product phases.** Track A = partnership evidence; Track B = administration; Track C = durability |
| **B-1a, B-1b, B-2a, B-2b** | Implementation prompts — visible in `RateCacheWarmService`'s Javadoc, `IllustrationServlet`'s Javadoc, and **V074's migration header** | **"Phase B (build) of the A1 work."** Phase A = investigation, Phase B = build |

**"B-1b" and "B1" are unrelated.** `B-1b` is the rate-cache build inside A1 — shipped. `B1` is the "+" catalog and proposal integration — Gate-0-blocked and not started. A session reading V074's header as *"this is phase B1"* will draw exactly the wrong conclusion about what is built.

### ⚠️ Known-stale items in `docs/business/README.md` (as of 2026-07-31)

Recorded here rather than fixed, because that file gets its own doc pass:

1. Its HealthSherpa row describes the integration in **HSOne paths** — `POST /v1/quotes`, `POST /v1/enrollments`, `GET /v1/enrollments` with `employer_external_id` + `updated_since` — all of which `healthsherpa.md` disclaims as not describing the target product.
2. It names *"the **AOR / TPA account model** the critical open question"* — **resolved favorably 2026-07-29**: AOR travels per application, keyed on NPN.
3. It calls the build plan *"Rev 5 — canonical"*; the file is **Revision 6, with Part 8 governing**.
4. It cites *"open items O1–O38"*; **Part 8 added O39–O40**.

One further inconsistency, noted 2026-07-31: **V076's header cites a ZIP-invariance check — "two ZIPs in Hopkins County TX returned identical plan sets across all 20 plans."** Twenty is the un-paginated default; the true Hopkins market is 65 plans. The conclusion is probably still right (rating does not vary by ZIP within a county), but **the evidence is weaker than the header reads**, and it interacts with T47's 29 low-containment counties. Re-verify when a credential is configured.

---

## 16. How this project's knowledge is organized

The operating rule for future sessions. **This section exists because the failure it prevents already happened:** a session opened, read an accurate tactical handoff, executed several individually-defensible pieces of work, and drifted off-strategy with nothing catching it.

| Source | Role | Currency |
|---|---|---|
| **Claude Code** | **Source of truth for every repo fact** — what is built, migration numbers, branch state, what a file actually contains | **Always current.** Read the repo, do not recall it |
| **This document** (`docs/ichra_strategy.md`) | **Source of truth for strategy.** Read it first, **via Claude Code** — not via the project-knowledge connector | Dated throughout; expected to go stale, which is why §1–§5 carry dates |
| **Project knowledge** | **A cache that cannot evict.** Stable reference material only | Whatever was synced, whenever. **Never assume it is current** |
| **State-carrying documents** — `claude_memory.md`, `migration_tracker.md`, `project_backlog.md`, `deployment_backlog.md` | Live repo state | **Should not be synced to project knowledge.** A cached copy of a state document is worse than no copy, because it reads as authoritative and is silently wrong |

### Session-open order

**Strategy first. Tactical state second.**

1. **This document** — what we are doing and why.
2. `docs/analysis/plus_tier_build_plan.md` (Part 8 first, then backwards as needed) — how.
3. `docs/business/healthsherpa.md` (2026-07-31 section first, then backwards) — the data surface.
4. Live state — `ls docs/migrations/`, `git branch -a`, `migration_tracker.md`, the backlogs.

**A session with state and no strategy executes well and drifts.** That is not a hypothetical — it is the reason this file exists.

---

## Related

- **Strategy and product:** `docs/business/plus_tier.md` · `docs/business/ichra_platform_capability_map.md` · `docs/business/ichra_administration_scope.md`
- **The build plan (canonical for mechanics):** `docs/analysis/plus_tier_build_plan.md`
- **The API:** `docs/business/healthsherpa.md`
- **The relationship:** `docs/business/swbd_premiumpath.md` · `docs/business/README.md` (opportunity register) · `docs/business/datapath.md`
- **Summit-side discovery:** `docs/analysis/summit_plus_tier_discovery.md` · `docs/analysis/summit_notice_automation_discovery.md` (written, not yet run)
- **Prior investigations:** `docs/analysis/phase_a_ichra_enrollment_portal.md` · `docs/analysis/qsehra_attestation_claims_engine.md`
- **Compliance:** `docs/analysis/domain_and_compliance_rules.md`
- **State:** `docs/analysis/migration_tracker.md` · `docs/deployment_backlog.md` · `docs/analysis/project_backlog.md`
