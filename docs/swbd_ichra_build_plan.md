# SWBD / ICHRA — Build Plan

**Status:** Active — the execution sequence for ICHRA/QSEHRA work
**Created:** 2026-07-31
**Owner:** Kevin
**Baseline:** branch `refactor/modernize-architecture`, HEAD `26e5c42`, migration **V076**
**Strategy:** `docs/ichra_strategy.md` — read that first
**Design detail:** `docs/analysis/plus_tier_build_plan.md` (D1–D37, O1–O40) · `docs/analysis/legal_assumptions.md` (LA-01–LA-12)

---

## How to use this document

**This sequences work. It does not specify it, and it does not re-argue strategy.**

- **The strategy lives in `docs/ichra_strategy.md`.** Why ICHRA, what the wedge is, what would falsify
  it, the graded evidence base. Not repeated here.
- **The design detail lives in `docs/analysis/plus_tier_build_plan.md`.** Field lists, per-phase scope,
  the D-series and O-series. **Later Parts govern earlier ones; Part 8 is current.** Not repeated here.
- **The compliance assumptions live in `docs/analysis/legal_assumptions.md`.** Every LA reference below
  points there rather than restating the reasoning.
- **This document answers one question:** *what is the next work item, and why that one.*
- **Its readers are two:** a developer picking up the next item, and the Claude session writing the
  prompt for it.

**A fourth document that restates the other three drifts from all of them.** Every section here ends by
naming where the detail lives. If a section starts explaining *why* rather than *when*, it has gone
wrong.

**Dates are load-bearing.** Anything undated reads as timeless and is wrong within a month. **Verified
and assumed are marked separately, every time** — that distinction is the doc set's characteristic
failure mode.

§1 and §3 are the sections that change what a session does. If you read nothing else, read those.

---

## 1. The demo target

**Everything in §3 is sequenced against this section. It is the most important judgment in the
document.**

### The demo

> **Sandoval Process Solutions, Hopkins County, three lives — from market to signed setup, on
> `premiumpath.net`, in about ten minutes.**

Not a capability tour. **One real case Forrest already has**, run end to end in front of him. Sandoval
is his own live ICHRA case — 3 employees, Hopkins TX, 9/1/26 effective — and it is the case zizzl
quoted at a **~$660/mo minimum** with his carriers gated off (documented call, 2026-07-28). Running
his own losing case and winning it is a different event from a feature walkthrough.

### The walkthrough — what he sees and clicks

| # | What Forrest does | What he sees | Status |
|---|---|---|---|
| 1 | Logs into `premiumpath.net` with the agent login he already holds (`fhuggins@swbdmg.com`, issued 2026-07-15) | SWBD branding, his own agency scope | ✅ **Built** — white-label portal, V068–V071 |
| 2 | Clicks **ICHRA** in the top nav | A product area, not a calculator buried in a dropdown | Item 3 |
| 3 | Picks Hopkins County, enters 3 lives | *"65 plans, three carriers. The practical floor is $412 at 21, $588 at 40, $1,236 at 64. Rates as of this morning."* | ✅ **Built** — RANGE illustration |
| 4 | Enters the three actual ages | A per-band net-cost table at a $400 contribution, and the group monthly total | Item 5 |
| 5 | Moves the contribution slider | **The flip point per employee.** *"At $350 Maria keeps her subsidy and comes out ahead. At $450 she loses it and is worse off. Here is the exact number, for each of your three."* | Items 8 + 9 |
| 6 | Clicks **Use this in a proposal** | The existing ProposalBuilder, prospect pre-filled, the illustration already attached as a proposal section | Items 6 + 7 — ⚠️ **BUT the control is `disabled` on staging rates; see the 2026-08-04 correction below. T150** |
| 7 | Sends it; opens the public link | An SWBD-branded proposal. **No SSA chrome. Sell price only — his markup applied and invisible** | ✅ **Built** — white-label + V066/V067 markup |
| 8 | Clicks **Apply**, submits | An application, then a **Setup activity carrying an ICHRA task sequence** — 90-day notice, ERISA safe-harbor notice, affordability determination, substantiation, PCORI | Items 4 + 10 |

**Steps 1, 3 and 7 are already in production.** Steps 6 and 8 run on rails that are already built and
already dynamic — `ApplyForProposal` and `CreateSetup25` are confirmed **fully per-LOS driven**
(`plus_tier_build_plan.md` Part 4), so the customer-facing sales-to-setup path carries a new LOS with no
work. **The demo is mostly assembly, not construction.** That is why this target is reachable and a
larger one is not.

> ⚠️ **Correction, session 27 (S27-D), 2026-09-07.** The setup checklist keys on `ServiceItem`, not
> LOS. `CreateSetup25` populates `ApplicationModule` from `LOS.serviceItem` **and**
> `Enhancement.serviceItem` (`ApplyForProposal.java:451-457` is an Enhancement loop immediately below
> the LOS loop this claim cites), and `AddSetupModule25` is a third door where a PSP user attaches a
> `ServiceItem` to a live Setup by hand. Tasks resolve via
> `RequiredTaskList WHERE serviceItem.id = :id`. A new LOS does flow through with no code change; LOS
> is one input to a ServiceItem-keyed mechanism, not the mechanism.

### Why it ends at setup, not at a number

**A competitor can show a quote engine.** zizzl did, and Forrest called anyway. Take Command, Thatch,
SureCo, Venteur and PeopleKeep all quote.

**Nobody shows him quote → proposal → application → setup, on his own brand, with his own margin,
administered underneath.** That is the whole pitch of `ichra_strategy.md` §1 — agent utility is the
wedge, administration revenue is what it earns — and it only lands if the last click produces a *case*
rather than a *PDF*. The pipeline is the differentiator (`ichra_platform_capability_map.md`: Layers 1
and 5 are the defensible ones), and carrying through to setup costs items 4, 7 and 10 rather than a new
subsystem.

**Stopping at analysis would leave the differentiator on the table for the price of three small items.**

### What is deliberately absent — and say so out loud

Naming the boundary is part of the demo. An honest "not yet, here is when" beats a demo that implies
capability it does not have.

| Absent | Why |
|---|---|
| **Enrollment** | Every gate is external and none has moved since 2026-07-29 (O12 rep, O13 BAA, O14 deeplink model). Not a build decision |
| **Coverage verification / the ledger** | B3, and LA-01-terminal. Correctly behind the wedge |
| **Provider check ("will I lose my doctor")** | The highest-emotion output in the plan, and blocked on an unowned gap — the API takes **NPIs**, the name-search endpoint is HSOne's. See §5 |
| **Subsidy segmentation** | Needs `POST /api/v1/aptc_estimates`, documented but never called. A live-credential item, not a demo blocker |
| **Book / renewal radar** | Gated on O24, which A4a is designed to earn. Deliberately not asked for yet |
| **Anything employee-facing** | The whole build-now scope sits on the agent side of `legal_assumptions.md`'s scope line. Crossing it is a decision, not an increment (LA-09) |
| **The card, PremiumPath mechanics** | A DataPath Summit configuration in flight. Out of scope by decision — `ichra_strategy.md` §8 |

### ⚠️ CORRECTION 2026-08-04 — the demo *does* break at step 6, and the claim below is wrong as written

**The heading below says the demo has no external gate. Step 6 has one, and it shipped the same day
this section was written.**

`illustration25.jsp:991-1003` renders *"Use This in a Proposal"* as a **disabled button** with the
note *"Available once production rates are configured"* whenever `sourceEnv != 'PRODUCTION'`. It was
gated that way from item 7's first commit (`e5b2009`, 2026-07-31) — the same date this section was
settled. **So the demo does not degrade gracefully at a banner, as §1.5 anticipated; it stops
mid-pipeline on a greyed-out control**, which is a worse demo moment than the banner ever was,
because the quote→proposal→application→setup pipeline *is* the pitch (§1.2).

⭐ **And the button's gate is redundant with respect to employer-facing exposure.** `ViewProposal`
independently refuses staging data at three further points — the illustration snapshot section
(`:142`), the conditional MARKET page (`resolveMarketPage`, `:467`) and the seven market tokens
(`:796`). **Enabling the hand-off on staging data puts no staging figure in front of an employer**;
the public proposal renders exactly the tier-1 administration content it renders today. The gate
costs the demo's central claim and buys nothing the three downstream gates do not already enforce.

**This is a distinct question from the one session 10 settled.** That decision — *"the provenance
gate is not to be relaxed to make the market-data demo work"* — concerned **employer-facing market
figures on an unauthenticated public link**. This is an **agent-facing control on an authenticated
page** with three independent gates still standing behind it.

**Kevin reopened this deliberately on 2026-08-04** and asked for a time-boxed, agency-scoped
PSP-admin override. **Requirement and design sketch are filed as T150** — not yet specced, not yet
built. Until it exists, **the demo stops at step 5** and steps 6–8 have to be narrated.

The rest of this section — that the banner is correct, stays, and does not itself block a *partner*
demo — is unaffected and still stands.

### ⭐ The demo has no external gate — settled 2026-07-31 (S6) — ⚠️ **superseded in part, see the correction directly above**

**Today every cached rate row is stamped `source_env = STAGING`, and the illustration page says so in
a red banner reading *"Do not present this to a client"*** (verified 2026-07-31 in
`illustration25.jsp:148-152`).

**That banner is correct, it stays, and it does not block this demo. Forrest is a partner, not a
client.** He is being shown the machine, not sold a number, and the banner is doing exactly its job
when he reads it — a system that refuses to launder staging data as production data is evidence of the
discipline SSA is selling.

**What allow-listing actually gates is the client-facing demo** — the moment one of Forrest's agents
puts a number in front of a real prospect. That is downstream of every item in §3.

Staging returns **real** rate data, not synthetic: verified twice, `$582.78` to the penny against the
zizzl CSA baseline. Same source of truth, same market.

⚠️ **Two things follow, and both matter for sequencing.**

1. ⭐ **Items 1–10 have no external dependency of any kind.** No credential beyond the staging key
   already in hand, no BAA, no counsel, no vendor reply. **Nothing on the path to §1 is waiting on
   anybody.** The only externally-gated item in the sequence is 11, and its gate is one email.
2. **What is not acceptable is removing the banner to make the demo look better**, or re-warming
   against production without allow-listing to make it go away.

**Detail:** `docs/ichra_strategy.md` §3 (the ranked capability list this target selects from) ·
`docs/business/swbd_premiumpath.md` (the relationship and the Sandoval case).

---

## 2. Current state

**As of 2026-07-31, verified against the repo at HEAD `26e5c42`.** Short by design — the narrative
version is `docs/ichra_strategy.md` §4 and §5.

**Shipped and deployed:** V074 `rating_area_rate_cache`, V075 `illustration_log`, V076
`county_reference` (254 Texas counties — row count verified). `HealthSherpaService` (paginating,
fail-closed), `RateCacheWarmService` (24h interval, catastrophic filter for 30+, age-45 canary),
`AgeCurve`, `IllustrationServlet` + `illustration25.jsp` (RANGE mode, ages 21/40/64, provenance
banner), `RateCacheAdmin` + `rateCacheAdmin25.jsp`, `RateCacheDAO`, `CountyReferenceDAO`.

**Live configuration:** five constants seeded on production; **HealthSherpa staging authenticated for
the first time 2026-07-31 14:04**. Cache warmed for **Bexar 48029, Dallas 48113, Harris 48201, Hopkins
48223** — 44 age rows each, `source_env = STAGING`. **Production access still 403s** pending
allow-listing.

**Known defects:** T44 (LCSP from off-exchange silver only, while ICHRA affordability keys on the
on-exchange LCSP — the dangerous direction is understating it), T47 (29 counties with sub-1.0 ZIP
containment), T48 (`source_env` excluded from delete scope and unique key).

### ⚠️ Corrections to the state as previously recorded

Each verified against the repo 2026-07-31. **Several documents are stale on these; correct them when
next touching those files, not now.**

| Claim | Actual | Where the stale text is |
|---|---|---|
| *"`ProposalSection` has no LOS scoping — `ApplicationSection` has it, `ProposalSection` does not"* | ❌ **Wrong.** `proposalsectionlos` **and** `proposalsectionenhancement` were created by **V037** and are mapped on `ProposalSection.losList`. `proposal_section.scope` VARCHAR(10) DEFAULT 'ALL' exists too | `plus_tier.md` "Existing-model additions"; `plus_tier_build_plan.md` B1 (**schedules V081 to build what already exists**) |
| *"`source_env` … `IllustrationServlet` neither filters nor displays it"* | ⚠️ **Half fixed** by `26e5c42`. The servlet reads `sourceEnv` off the loaded rows and the JSP renders a hard warning banner. **The delete-scope and unique-key half of T48 stands** | `ichra_strategy.md` §5.2; `project_backlog.md` T48 |
| *"`AppConfig.getHealthSherpaBaseUrl()`'s hardcoded default is the production endpoint"* | ❌ **Superseded** by T43 (`42caf4e`). No default; absent or blank means not configured and the service refuses to call | `healthsherpa.md` 2026-07-31 section |
| *Tarrant is a T47 case* | ❌ **Distinct problem.** Tarrant 48439's representative ZIP is **75261 — DFW Airport**, a ZCTA with almost no residential population. T47 is about *containment*; this is a **containment-1.0 ZIP that is simply unusable**. A separate gap in V076's selection rule | New — logged here, not yet in `project_backlog.md` |
| *"the `/Illustration` nav link is visible to every agent"* | ✅ **True, and worse.** `navbar25.jsp:196` is unguarded inside the Sales dropdown — but `IllustrationServlet.isAuthorized()` **also** admits `isAgent`, `isAgencyAdmin`, `isPspAdmin`, `isPspUser` and `isPspSales`. **Hiding the link does not close the URL** | — |
| Release **v0.76.02** | ⚠️ **Unconfirmed in-repo.** The only release recorded anywhere in `docs/` is **v0.76.00** (`migration_tracker.md:130`, 2026-07-31 11:37). Four commits sit after that record. Local tags are stale by design — confirm against the GitHub Releases page | `ichra_strategy.md:6` still cites HEAD `935c31e` / v0.76.00 |

**Detail:** `docs/ichra_strategy.md` §4–§5 · `docs/analysis/migration_tracker.md` ·
`docs/deployment_backlog.md` D-78/D-79/D-82/D-83/D-84.

---

## 3. The build sequence

**The core of this document.** Ordered by **fastest path to §1**, not by conceptual tidiness.

**How to read a row.** *Phase A?* means: does this touch existing files, and therefore need a
read-only investigation pass before an implementation prompt is written? *Reversal* is what undoing it
costs — the field that decides whether rule 3 ("build it anyway unless it's hard to reverse") applies.

**Migration numbers are placeholders from a V076 baseline. Allocate in commit order, not plan order.**

---

### 1 — ~~Close the rule-2 violation on `/Illustration`~~ ✅ Done 2026-07-31, `0b4711b`

| | |
|---|---|
| **What** | Move `navbar25.jsp:196` inside the empty `<c:if test="${sessionScope.isPspAdmin}">` block that already sits at lines 197–198, **and** narrow `IllustrationServlet.isAuthorized()` from five session roles to `isPspAdmin` only. |
| **Agent-visible outcome** | **Negative, deliberately.** Agents and agency admins lose a link they should never have had. Nobody outside SSA has used it — the cache was empty until 2026-07-31 |
| **Attaches at** | `navbar25.jsp` Sales dropdown; the servlet's own guard |
| **Gate** | PSP-admin-only. This item *is* the gate |
| **Phase A?** | **Not required.** Two files, both read in full 2026-07-31. `RateCacheAdmin.isAuthorized()` is the exact pattern to copy |
| **Schema** | None |
| **Depends on** | Nothing |
| **Size** | **~1 hour** |
| **Reversal** | Two lines |

**Why first.** It is a live violation of rule 2 in production, it is the cheapest item on the list, and
every subsequent item adds surface behind the same gate. Fixing it after building three more pages
means fixing it in four places. **The empty `isPspAdmin` block already sitting at line 197 is the
intended home** — this was an oversight, not a decision.

---

### 2 — ~~The ICHRA availability resolver and the per-agency entitlement flag~~ ✅ Done 2026-07-31, `0b4711b`

| | |
|---|---|
| **What** | One resolver answering *is ICHRA available in this context* — PSP admin always; a designated agency via a new `agency.ichra_enabled` flag; nobody else. Callers never learn how the answer was reached and **never see a reference-row ID** |
| **Agent-visible outcome** | None. Infrastructure |
| **Attaches at** | `data/resolver/` — joins `AgencyScopeResolver` and `OriginatingAgencyResolver`, an established package with an established shape |
| **Gate** | This item *is* the gating mechanism for items 3–13 |
| **Phase A?** | **Not required** — a new class plus one column. `AgencyScopeResolver` is the structural template |
| **Schema** | **V0NN** — `agency.ichra_enabled TINYINT(1) NOT NULL DEFAULT 0`. **Copy V067 exactly**: default OFF for every existing and future row, no backfill |
| **Depends on** | Item 1 |
| **Size** | **~0.5 day** + migration |
| **Reversal** | Config — set the flag to 0. The column stays harmlessly |

**Rule 4 lives here.** LOS, `ServiceItem`, `PlanType`, `ServiceModule` and `RateTable` are all
PSP-scoped (`LOS.psp_id` verified) and differ per installation, so **no ID may be hardcoded anywhere**.
The resolver looks rows up by PSP plus a stable natural key and **returns "unavailable" cleanly when
they do not exist** — which is what makes item 4 a configuration step rather than a precondition.

---

### 3 — ~~The ICHRA front door~~ ✅ Done 2026-07-31, `0b4711b`

| | |
|---|---|
| **What** | A top-level nav entry and a hub page — the illustration, the rate-cache admin link, and space for items 5/9/11 to land — instead of one `<li>` in the Sales dropdown |
| **Agent-visible outcome** | For an entitled agency: ICHRA reads as a product area. For everyone else: unchanged, because item 2 hides it |
| **Attaches at** | `navbar25.jsp` top level. **Precedent verified:** `SuperDashboard` (line 260) is a top-level `nav-ghost` gated on an installation condition plus a role — exactly this shape |
| **Gate** | Item 2's resolver |
| **Phase A?** | **Required, lightly.** `navbar25.jsp` is a heavily-shared file. Append; never weave |
| **Schema** | None |
| **Depends on** | Item 2. ✅ **S4 settled 2026-07-31 — top-level nav, `SuperDashboard` pattern** |
| **Size** | **~0.5 day** |
| **Reversal** | Delete the nav block; the hub page becomes unreachable and inert |

---

> **4 — ~~(deleted 2026-07-31)~~ Not a build item.** Kevin creates the ICHRA/QSEHRA `LOS`, `ServiceItem`,
> `PlanType` and priced `ServiceModule` → `RateTable` rows through the admin UI when he is ready to
> test. Nothing is sequenced, sized or scheduled around it. The Gate 0 probe is cancelled.

---

### 5 — ~~AGE_BAND illustration mode~~ ✅ Done 2026-07-31, `0b4711b`

| | |
|---|---|
| **What** | A second illustration mode taking age-band counts and rendering per-band net cost against a chosen contribution, plus a group monthly total. `mode` is already written as the literal `"RANGE"` so the column is correct from the first row |
| **Agent-visible outcome** | **The first output that looks like a design rather than a range.** *"Your four under-30s, seven forties, three over-55 — here is each band's cost and your monthly total at $400."* |
| **Attaches at** | `IllustrationServlet` + `illustration25.jsp` — a mode branch, not a new surface |
| **Gate** | Inherits items 2–3 |
| **Phase A?** | **Not required** — both files are new, ICHRA-only, and were read in full |
| **Schema** | **None.** ⭐ **Every age 21–64 is already cached**; the servlet reads three of them |
| **Depends on** | Items 2, 3 |
| **Size** | **~1 day** |
| **Reversal** | Delete the branch |

**Zero new API calls, zero new schema, zero external gates** — the cache already holds 44 age rows per
county. **The best value-to-effort ratio remaining on the list** now that the five constants are seeded.

---

### 6 — ~~Illustration → LOS-scoped proposal section~~ ✅ Done 2026-07-31, `b0e524b`

| | |
|---|---|
| **What** | A proposal section rendering **one ICHRA design, snapshotted onto the proposal at creation** (S2 — revised from parameters-and-recompute to snapshot, 2026-07-31), scoped to the ICHRA LOS so it appears on ICHRA proposals and nowhere else |
| **Agent-visible outcome** | ⭐ **The illustration stops being a calculator and becomes part of a sellable document.** Demo step 6→7 |
| **Attaches at** | **`proposalsectionlos`** — ✅ **verified to exist**, created by **V037**, mapped on `ProposalSection.losList`. Plus `proposal_section.scope`, plus the new **V079** `proposal_ichra_snapshot` / `proposal_ichra_snapshot_band` |
| **Gate** | ⚠️ **LOS-scoped from the first commit. `scope='ALL'` is never acceptable** (S5) |
| **Phase A?** | **Required.** `ProposalSection.sectionType` is a free `VARCHAR(20)` with JSP `<c:choose>` dispatch and **no default branch — an unmatched type renders silently.** `ViewProposal` is live customer-facing code |
| **Schema** | **V079** — `proposal_ichra_snapshot` (one row per proposal, `proposal_id` UNIQUE) + `proposal_ichra_snapshot_band` (AGE_BAND rows). S2 revised to this from the parameters-only, no-new-table plan recorded in S2's superseded history |
| **Depends on** | ⚠️ **The ICHRA reference data existing — Kevin creates it via the admin UI (item 4), not a scheduled build gate** (S5). Plus item 5 |
| **Size** | **1–2 days** |
| **Reversal** | Delete the section row and its snapshot. The JSP branch goes unmatched and renders nothing |

⚠️ **Shipped as a snapshot, not recompute-at-render — S2 was revised 2026-07-31 before this item was
built.** The plan this row originally carried (parameters held on the proposal, re-evaluated against
the cache at every render) would have meant a proposal sent in July and opened in September rendered
September's rates. **That plan is superseded** — see S2 (§4) for the finding that decided it. The
shipped section instead reads a fixed `proposal_ichra_snapshot` row: what the client was shown is what
stays on the page.

**What is *not* indicative and does *not* move is the price.** The PEPM comes from the `RateTable` path
(item 4's reference data) and is fixed by the proposal in the normal way, independent of the ICHRA
snapshot. **The market illustration is now fixed at snapshot time too; the quoted fee never moved.**
Keeping those two visually distinct on the section remains a design requirement.

⭐ **This is the single biggest correction to the received plan.** `plus_tier_build_plan.md` B1
schedules **V081 — `proposalsectionlos` + LOS scoping** as net-new work and `plus_tier.md` states
plainly that `ProposalSection` has no LOS scoping. **Both are wrong: V037 shipped it.** The proposal
attach is a JSP branch and a section row, not a schema project — which is why it sits at item 6 rather
than late in Track B.

⚖️ **Carrier names stay off this section** (LA-04 — the design declines to rely on the permissive
reading), and it inherits the **completeness disclosure** (LA-05: the display is genuinely partial,
off-exchange only, and the disclosure is a statement of fact rather than a hedge).

---

### 7 — ~~"Use this in a proposal" hand-off~~ ✅ Done 2026-07-31, `e5b2009`

| | |
|---|---|
| **What** | A button on the illustration result deep-linking into `ProposalBuilder` with prospect and illustration context attached |
| **Agent-visible outcome** | The pipeline becomes one motion instead of two screens and a re-key |
| **Attaches at** | `ProposalBuilder` query parameters. **Precedent:** `CreateOpportunity.doPost` already deep-links `ProposalBuilder?prospectId=…&sourceActivityId=…` (v0.71.08) |
| **Gate** | Inherits |
| **Phase A?** | **Required, lightly** — `ProposalBuilder` is live sales code. Follow the v0.71.08 pattern rather than inventing one |
| **Schema** | None, **unless S3 (§4) resolves toward persisting the link** |
| **Depends on** | Item 6 |
| **Size** | **~0.5 day** |
| **Reversal** | Remove the button |

---

### 8 — ~~T44: on-exchange LCSP~~ ✅ Done 2026-07-31, `ba023bd`

| | |
|---|---|
| **What** | Derive `lcsp_premium` and `benchmark_silver_premium` from **on-exchange** silver plans. `RateCacheWarmService` quotes `off_ex: true`, so both are currently off-exchange-only |
| **Agent-visible outcome** | **None directly** — and that is the point. This fixes the input before anything is built on it |
| **Attaches at** | `RateCacheWarmService` + `rating_area_rate_cache` |
| **Gate** | Inherits |
| **Phase A?** | **Not required** — both files are new and ICHRA-only. The empirical half is **two staging calls differing only in `off_ex`**, and staging is authenticated as of 2026-07-31 |
| **Schema** | **V0NN** — likely separate on-exchange columns rather than overwriting, so the off-exchange market figures the illustration displays stay available |
| **Depends on** | Staging credential only. ⭐ **Independent of items 3–7 — workable in parallel from today** |
| **Reversal** | Code change plus a cache re-warm. ⚠️ **But every affordability figure already issued on a wrong LCSP is irreversible in the sense that matters** — the offer was made and the plan year ran |
| **Size** | **1–2 days** |

⚖️ **Non-negotiable ordering: this precedes item 9.** LA-12 is explicit — *"fix T44 before any
affordability output is shown to anyone at all, including the agent."* **The dangerous direction is
understating the LCSP**, which lowers the affordability threshold and makes an unaffordable offer look
affordable. The employee then loses subsidy eligibility they were entitled to, and the determination
that said it was fine came from SSA.

**Detail:** `project_backlog.md` T44 · `legal_assumptions.md` LA-12.

**Shipped note, 2026-07-31, `2d3c17c`:** a read-only rate-cache diagnostic panel landed on
`/RateCacheAdmin` alongside this item — two live quotes differing only in `off_ex`, side by side,
persisting nothing, PSP-admin only. Built to unblock the T44 probe from a workstation with no
HealthSherpa constants configured. **Not a numbered sequence item.**

---

### 9 — ~~Affordability threshold per employee~~ ✅ Done 2026-07-31, `4556ecd`

| | |
|---|---|
| **What** | Per employee, the contribution level at which the offer flips from unaffordable (PTC preserved) to affordable (PTC lost). A curve per person |
| **Agent-visible outcome** | ⭐ **The demo's emotional peak, and the most misunderstood mechanic in ICHRA design.** *"Here is the exact flip point — for each of your fourteen"* |
| **Attaches at** | The illustration hub (item 3). Consumes item 5's age bands |
| **Gate** | Inherits. ⚖️ **Employer- and agent-facing only** — employee-facing affordability is on the defer side of the scope line (LA-12) |
| **Phase A?** | **Not required** — new surfaces only |
| **Schema** | ⭐ **None.** Unlike item 6, this output stays computed on demand — V079's snapshot deliberately excludes every affordability figure (no flip contribution, no income, no PTC status), so this path is unaffected by S2's revision to snapshot. No design-census table here |
| **Depends on** | Items 5, 8. **Item 8 is a hard correctness gate, not a preference** |
| **Size** | **~2 days** |
| **Reversal** | Display edit to withdraw. ⚠️ Not reversible for determinations already acted on |

---

### 10 — ~~ICHRA task sequence and setup checklist~~ ✅ Content done 2026-07-31, `4775252`

| | |
|---|---|
| **What** | The `RequiredTaskList` / `TaskSequenceTable` / `Task` content an ICHRA Setup inherits: ⚖️ 90-day notice, ⚖️ ERISA safe-harbor notice, affordability determination, initial substantiation, ⚖️ 1095-B, ⚖️ PCORI, §105(h) |
| **Agent-visible outcome** | **A sold ICHRA case produces a real ICHRA checklist.** Today it would inherit the generic HRA checklist with **zero ICHRA compliance steps** — verified in `phase_a_ichra_enrollment_portal.md` Q4. Demo step 8 |
| **Attaches at** | The existing task-sequence machinery, via item 4's `ServiceItem` |
| **Gate** | LOS-scoped |
| **Phase A?** | **Not required** structurally — but the *content* is compliance work, not code |
| **Schema** | **None — and no migration.** Verified 2026-07-31 that **no migration in the repo (V025–V080) has ever created a task sequence** — zero inserts into or mentions of `task`/`tasksequence`/`tasksequencetable`. Sequences come from the Sequence Builder admin UI, and rule 5 names them as Kevin's reference data. Content delivered as `docs/business/ichra_setup_checklist.md`, ready to type |
| **Depends on** | The ICHRA reference data existing — Kevin creates it via the admin UI (item 4), not a scheduled build item |
| **Size** | **2–3 days**, mostly content |
| **Reversal** | Editing a checklist on a case already in flight is disruptive but not destructive |

⚠️ **Two LA entries constrain the content directly.** **LA-07**: the QSEHRA notice runway is ~45 days
for a non-January effective date, and **January 1 is structurally the hardest first-year date, not the
easiest** — the checklist's due-date logic must not assume otherwise. **LA-08**: the ICHRA notice
analysis **has not been done and does not transfer from QSEHRA.** Until it is, **no ICHRA sale should
be quoted on a short runway**, and the checklist should say so rather than compute a date it cannot
justify.

⭐ **The LA-08 problem turned out to be structural, not a matter of discipline.** Verified 2026-07-31:
**there is no due-date or offset field anywhere in the checklist mechanism** — not on `Task`, not on
`TaskSequenceTable` (which carries only `sort_order`), not on `TaskSequence`, and not on the per-case
`ToDo` (which has `date_completed` only). The ICHRA notice step therefore **cannot** carry a computed,
defaulted or offset date, because no field can hold one. No placeholder is possible, so none can leak.
Timing says "not determined by SSA" in the description text, which is where it belongs.

⚠️ **A trap found in SSA's own documents while sourcing this.** `ichra_administration_scope.md`
Phase 2 asserts *"Required 90 days before the plan year. For a newly established ICHRA the notice is
due by the date coverage begins."* **LA-08 names that exact assertion as uncited and says it must not
be relied on** — it is O17's claim, restated in the scope doc as though settled. The checklist
deliberately does **not** carry either figure.

**Delivered:** `docs/business/ichra_setup_checklist.md` — 19 tasks across five groups, every
description length-checked against `Task.description`'s `varchar(200)`, every admin-UI field filled
in, six named compliance gaps recorded rather than invented.

**Detail:** `plus_tier_build_plan.md` Part 7, "The ICHRA setup checklist now has real content" ·
`ichra_administration_scope.md` Phases 2–5 · `legal_assumptions.md` LA-07, LA-08.

---

### 11 — ~~A4a: sample group-to-ICHRA conversion analysis~~ ✅ Done 2026-07-31, `7db188d`

| | |
|---|---|
| **What** | Three to five renewing groups entered by hand. Current group premium in; ICHRA comparison and per-employee net position out |
| **Agent-visible outcome** | ⭐ *"Your renewal is $9,840/month, up 14%. ICHRA at $430/head is $6,020, and eleven of your fourteen come out ahead."* A case-winning conversation, not a feature |
| **Attaches at** | The illustration hub, via a new `GroupConversionServlet` (`/GroupConversion`) — a separate servlet by deliberate choice (POST-computed, unlike item 9's GET-only `/Illustration`, because a current-premium/payroll-deduction input has no business in a URL). A thin increment over item 9 — current group cost is the only new input. Structural note found during the build: **item 9 is not a separate servlet or JSP** — it is an opt-in `affordabilityBasis` sub-mode inside `IllustrationServlet`'s `AGE_BAND` path, rendered in `illustration25.jsp` |
| **Gate** | Inherits. ⚖️ **D24, non-negotiable: output goes to the agent, never to the employer** |
| **Phase A?** | **Not required** |
| **Schema** | Likely none. Hand-entered, not imported — **`agency_book_group` is A4b and is not in this sequence** |
| **Depends on** | Item 9. **The three-to-five-renewing-groups email from Forrest gates having a compelling case to *demo* this, not the build — the page ships with hand-entered input regardless of whether that email is ever sent** |
| **Size** | **~2 days** |
| **Reversal** | Delete the page |

**A4a exists to earn A4b.** *"Send me your book"* (O24) is a large request to make of a partner nothing
has been proven to. *"Send me three groups renewing next quarter"* is an easy yes and self-selects for
cases where the analysis matters.

---

### 12 — ~~A6: design advisor~~ ✅ Done 2026-07-31, `e25ee4e`

| | |
|---|---|
| **What** | A `ChatbotSkill` row plus KB content answering agents' ICHRA/QSEHRA design questions from rules already written |
| **Agent-visible outcome** | *"Does my client's dental plan kill the QSEHRA?"* — answered with citations, in seconds |
| **Attaches at** | The V046 `chatbot_skill` mechanism. **Precedent:** V065 seeded `EMAIL_DRAFT_ASSISTANT` the same way |
| **Gate** | `is_admin_only = 0` since **V082** (2026-07-31) — reachable by any caller item 2's `IchraAccessResolver` already grants ICHRA access to; `navbar25.jsp`'s chat-assistant include was widened to match (**T58**). A UI entry point now exists: the hub's Design Advisor card |
| **Phase A?** | **Not required** — folded into the build run instead |
| **Schema** | **V080** — `ICHRA_DESIGN_ADVISOR` skill row + `ichra_design` KB registry row + 18 `knowledge_chunk` rows. No new tables, and **no Java or JSP change** |
| **Depends on** | ⭐ **Nothing. Fully independent — workable at any point** |
| **Size** | **~0.5 day** |
| **Reversal** | Delete the row |

⚖️ **Hard boundary: education with citations. Never plan selection, never anything requiring
licensure** — those route to the licensed agent. Non-PHI, so the standard Anthropic key is correct;
Bedrock routing is not required here. `legal_assumptions.md` considered and **declined** to give this
an LA number, because the boundary is a settled rule rather than an assumption.

⚠️ **The boundaries are encoded as refusal behaviour in the skill's `system_prompt`, not merely
documented.** The load-bearing one is **LA-08**: asked when an ICHRA notice is due, the advisor states
that the analysis has not been done, declines to compute or estimate a date, and refuses to reason by
analogy from LA-07's QSEHRA runway. That is the single most likely wrong answer the feature could give,
so it is written as an explicit hard boundary with its own worked example rather than left to inference.

⚠️ **Two mechanism findings from the build, both worth knowing before touching this again.**
**(1)** `ChatAssistant.executeSkill` injects **no** KB content — a matched skill's `system_prompt` is its
entire context. The rules therefore live in *both* the prompt and the chunks by design (prompt = the
matched path, chunks = the no-match fallback and the Knowledge Manager edit UI); the migration carries a
`SYNC-GUARD` comment saying so. **(2)** The skill's `model`/`max_tokens` columns were **ignored** on the
text-only path — it hardcoded Haiku 4.5 / 1024 tokens. **Fixed 2026-07-31 (T52)**: `ChatAssistant
.executeSkill` now honours a matched skill's configured `model`/`max_tokens` when both are set, so this
skill runs on Sonnet / 2048 tokens as V080 configured.

---

### 13 — ~~Link illustrations and designs to an opportunity~~ ✅ Done 2026-07-31, `a71b79d` + `619461f`

| | |
|---|---|
| **What** | An optional opportunity (or prospect) reference on `illustration_log` and on whatever item 9 persists |
| **Agent-visible outcome** | *"Three illustrations were run against this prospect, here they are"* — and it is what makes A5's pipeline console possible later |
| **Attaches at** | `Opportunity extends Activity`, already carrying stage, prospect, agency and value |
| **Gate** | Inherits |
| **Phase A?** | **Required, lightly** — adding a column to a shipped table with rows in it |
| **Schema** | **V081** — nullable `opportunity_id` FK to `assignee(id)`, `ON DELETE SET NULL` (precautionary: no delete path for an Opportunity exists in AMS today, verified by grep 2026-07-31, so nothing would fail under RESTRICT right now — but SET NULL is free, semantically correct for a nullable attribution column, and right if a delete path or manual cleanup appears later; corrected in `cf15ff8`). ⚠️ **`illustration_log` records agent and agency and nothing else** (verified against V075) |
| **Depends on** | ⚠️ **Structural decision S3 (§4) — now resolved.** |
| **Size** | **~0.5 day** + migration |
| **Reversal** | ⚠️ **Rule-3 exception — schema other features depend on.** Cheap now, not cheap after A5 reads it |

**Console read shipped 2026-07-31, `619461f`.** The agent pipeline drawer (`agentHome25.jsp`) now lists
the illustrations and conversion analyses logged against the opportunity — **date, kind and agent only**,
no `result_summary`, no premium, no county, no headcount, no figure of any kind. The numbers stay on the
tool that produced them, behind its own gate and its agent-only framing (D24).

Built as `IllustrationLogDAO.findByOpportunityId` (one query, newest first, capped at 25) plus
`/IchraOpportunityAnalyses`, a JSON feed the drawer fetches client-side. **Client-side by necessity, not
preference:** the drawer's single-opportunity detail is built from a pre-serialized `OPPS` map with no
server-side extension point, and its "Full Detail View" target (`ViewById` → `GoActivityDetail25` →
`ViewActivity25`) is the shared activity-detail page used by every activity type. Fetching from a gated
endpoint turned that constraint into the rule-2 guarantee: **an empty array renders nothing at all** — no
heading, no placeholder, no border — so an agent without ICHRA entitlement sees the drawer exactly as it
looked before the feature existed. Three checks run on every path through the endpoint and no path
returns data without all three: `IchraAccessResolver.isAvailable`, then parse, then
`OpportunityAuthz.canAccessOpportunity`; every failure answers with the same empty array, so "no such
opportunity", "not yours" and "ICHRA is off for you" are indistinguishable.

---

### Independence and parallelism

- **Item 8 (T44)** depends only on the staging credential. **Start it now, in parallel with items 1–3.**
- **Item 12 (A6)** depends on nothing at all. Drop it in whenever a half-day appears.
- **Items 1–3** are strictly sequential and total about a day and a half.
- **Item 4** is not a build item — Kevin creates the ICHRA/QSEHRA reference rows via the admin UI
  whenever he's ready to test; nothing in the sequence schedules or sizes around it.
- ⚠️ **Item 6 needs the ICHRA reference data (item 4) in place before its section can render.** The
  proposal section needs an ICHRA LOS to scope to, and **S5 forecloses the obvious shortcut** — a
  `scope='ALL'` section renders on every proposal for the PSP, which is a direct rule-2 breach, and
  "temporary" surfaces of that kind survive. **Item 6's real gate is that data existing, not a build
  item shipping.**
- **Item 11** is externally gated on an email that has not been sent. **Send it before item 9 starts.**
- ⭐ **Nothing in items 1–10 waits on an external party** (S6). The only remaining sequencing dependency
  on the demo path is item 6 waiting on Kevin having created the ICHRA reference data (item 4).

---

## 4. Structural decisions to make

**Decisions the plan needs settled and that should not be made by assumption.** Each names what it is,
what it blocks, what would settle it, and whether it can be deferred.

**Status as of 2026-07-31: all six resolved.**
✅ S1 (verified from code) · ✅ S2 · ✅ S3 · ✅ S4 · ✅ S5 · ✅ S6.

**Resolved entries stay, with the reasoning intact** (§7 rule 4). A reader needs to know a decision was
checked rather than assumed — and where a decision overrode this document's own recommendation, as S2
did, the superseded recommendation is part of the record.

### S1 — Proposal-to-application cardinality — ✅ **RESOLVED, verified 2026-07-31**

**Believed one-to-one; it is one-to-one, and enforced at the primary key.**

```java
// model/sales/application/Application.java
@Entity
public class Application {
    @Id @OneToOne @JoinColumn(name="proposal_id")
    private Proposal proposal;          // the PK *is* the proposal FK
    @OneToOne(mappedBy = "application")
    private Setup setup;                // and Application ↔ Setup is 1:1 too
}
```

`Proposal` carries a single-valued `@OneToOne(mappedBy="proposal") private Application application`.

**The chain is Proposal → Application → Setup, strictly 1:1:1.** More than one application per proposal
is not merely unusual — it is structurally impossible without a schema change.

**Blocks:** S2, decisively. **Deferrable:** no longer relevant — it is answered.

⚠️ **Two further constraints found in the same read, both load-bearing for item 6:**
`Proposal.prospect` is `nullable = false` **and** `Proposal.rate` is `nullable = false`. **An ICHRA
proposal cannot exist without a Prospect and a priced `Rate` row** — which means item 4's priced
`ServiceModule` → `RateTable` path is a hard prerequisite for the proposal attach, not a nicety.

---

### S2 — Scenarios: one proposal or several? — ✅ **RESOLVED — snapshot, 2026-07-31**

⭐ **Current resolution: snapshot, not re-derive.** One ICHRA design snapshotted onto the proposal at
creation, via `proposal_ichra_snapshot` + `proposal_ichra_snapshot_band` (**V079**, `b0e524b`,
2026-07-31, item 6). This **supersedes the re-derive-at-render resolution recorded below** (`97faad3`,
also 2026-07-31, earlier the same day), which itself superseded this document's original scenario-set
recommendation. Per §7 rule 4, both prior positions stay recorded rather than deleted.

**The finding that decided it (Phase A, 2026-07-31).** There is **no per-proposal section content
table** anywhere in AMS. Every existing `ProposalSection` is either static template HTML with merge
tokens, or derived live from the `Proposal` graph. Re-derive therefore needed net-new schema anyway — a
parameters table — and was never the cheaper path the recompute resolution below assumed it was.

**Given a table either way, snapshot wins.** It adds output columns, a band table
(`proposal_ichra_snapshot_band`) and two provenance fields (`source_env`, `rates_fetched_at`) over what
a bare parameters table would have needed, and in exchange buys a document that a later cache re-warm
cannot rewrite — a proposal already sent keeps showing what the client was shown, rather than the
numbers moving under the reader on next render.

**Shipped as V079** — `proposal_ichra_snapshot` (one row per proposal, `proposal_id` UNIQUE — still not
a scenario system) + `proposal_ichra_snapshot_band` (AGE_BAND detail rows). Commit `b0e524b`, item 6.

**Blocks:** items 6 and 9. **Settled by:** shipped code (V079). **Deferrable:** no longer relevant — it
is answered.

---

**Superseded 2026-07-31, earlier the same day — re-derive at render, resolved `97faad3`.** Kept in place
per §7 rule 4 so the prior reasoning stays visible; this was itself a revision of the original
recommendation on the table.

**Decision at the time: one design per proposal, stored as parameters, recomputed at render. Not a
scenario system.**

**What it was.** Affordability analysis naturally generates alternatives — $350 vs $450 vs a two-class
structure — and where those live decides whether the existing pipeline carries ICHRA unchanged.

**Several proposals was never viable.** S1 forecloses it: several proposals means several applications
means several setups, for one employer making one decision, polluting the pipeline with opportunities
that were never real.

**The decision went further than the original recommendation on the table.** This document had proposed
a scenario *set* below the proposal — N scenarios, one marked selected, the rest preserved — the
original recommendation, and itself now superseded. **That was rejected in favour of something
simpler: no scenario objects at all.** A design is a small set of parameters — county FIPS, plan year,
age bands, contribution amount — held against the proposal and re-evaluated against the cache every
time the section renders. Alternatives are explored **in the illustration tool**, before a proposal
exists; the proposal carries the one the agent chose.

**Why this was believed the better answer, at the time.** A scenario set is a stateful object that must
be created, selected, versioned, garbage-collected and reasoned about — and its only job is to remember
arithmetic that is cheap to redo. **Parameters plus a live cache is strictly less machinery for the same
output**, it was reasoned, and it keeps the proposal 1:1 with the decision, which is rule 1. **This
reasoning did not survive the Phase A finding** — see the current resolution above.

**What this recommendation superseded** — recorded here, not edited into those files:

- ⚠️ **`plus_quote` is not needed for this path.** `plus_tier.md`'s new-tables list describes it as
  *"Snapshot of quote inputs … Rates move; the illustration must be reproducible months later"*, and
  `plus_tier_build_plan.md` B1 schedules it as **V082**. **The reproducibility requirement was
  deliberately not being met under this resolution** — see the trade below. ⚠️ **Note what the current
  resolution above restores:** reproducibility, the exact concern `plus_quote` existed to address — by a
  different table (`proposal_ichra_snapshot`), not `plus_quote` itself.
- **D14's two render variants** (range vs age-band) become a **parameter-driven** branch rather than a
  flag on a stored snapshot. ⚠️ **Also superseded** — `proposal_ichra_snapshot.mode` is exactly a flag
  on a stored snapshot.

**The trade this resolution accepted, and why it no longer applies.** ⚠️ **A proposal sent in July and
opened in September would have shown September's numbers** under recompute-at-render. That was accepted
deliberately at the time, survivable only because the illustration is indicative market data and the
render is self-dating via `fetchedAt`. **The trade is now moot** — the snapshot resolution above means
the numbers do not move at all.

**Blocked:** items 6 and 9 — resolved under this position at the time; both instead built against the
current (snapshot) resolution above.

---

### S3 — Should an illustration or design attach to an opportunity? — ✅ **RESOLVED 2026-07-31**

**Decision: yes, nullable, opportunity-level only** — not prospect, not employer, and not anything
person-shaped. Resolved on Kevin's confirmation that a pipeline console (`AgentHome`) exists and that
illustrations and conversion analyses should stay functional against it.

**What it is.** `illustration_log` records agent, agency and parent agency and has **no opportunity,
prospect or proposal reference** (verified against V075). Today an illustration is telemetry. Attaching
it makes it evidence.

**The tension.** LA-11 keeps the design census minimal because minimalism is load-bearing — it is what
keeps the sales stage outside the BAA question. But an *opportunity* reference is an internal FK, not
personal data, and it is what A5's pipeline console reads.

**Shipped as V081** — nullable `opportunity_id` FK to `assignee(id)`, `ON DELETE SET NULL`. Commit
`a71b79d`, item 13.

**Blocks:** item 13, and A5 later. **Settled by:** shipped code (V081). **Deferrable:** no longer
relevant — it is answered.

---

### S4 — Where does the ICHRA front door live? — ✅ **RESOLVED 2026-07-31**

**Decision: top-level nav, on the `SuperDashboard` precedent.**

**What it was.** Rule 2 says PSP-admin-by-default. The precedent is `SuperDashboard`
(`navbar25.jsp:260`) — a top-level `nav-ghost` gated on an installation condition **plus** a role. The
alternatives were a section inside the Admin dropdown (safest, but reads as a tool rather than a
product) or staying in Sales (rejected — that is item 1's violation).

**Why.** **The demo's step 2 is Forrest clicking something that says ICHRA.** A dropdown entry does not
carry the message that this is a product area, and the whole thesis is that ICHRA is a product rather
than a calculator. Gated by item 2's resolver, so rule 2 holds regardless of nav prominence — **the
visibility question and the placement question are independent, and conflating them is what put the
link in the Sales dropdown in the first place.**

**Blocked:** item 3 — now unblocked.

---

### S5 — Section scoping before the LOS rows exist — ✅ **RESOLVED 2026-07-31**

**Decision: ICHRA proposal sections are LOS-scoped from the first commit. `scope='ALL'` is never
acceptable — which makes the ICHRA reference data (item 4, Kevin's admin-UI setup) a hard prerequisite
for item 6, not a scheduled build gate.**

**What it was.** `proposal_section.scope` defaults to `'ALL'`, so a section with no LOS links renders
on **every** proposal for that PSP. The tempting shortcut was to ship the section unscoped while item
4's reference rows were still being sorted out.

**Why the shortcut is closed.** An unscoped ICHRA section is visible to **every agency on every
proposal** — a direct rule-2 breach on the most client-visible surface AMS has. And "temporary"
surfaces of that shape survive: nothing about a working page generates pressure to go back and scope
it.

⚠️ **The consequence is real and should not be softened.** Item 6 needs the ICHRA reference data in
place before its section can render. **That data is now Kevin's admin-UI setup (item 4), not a
scheduled build item with its own timeline** — so item 6's actual gate is Kevin creating the
LOS/`ServiceModule`/`RateTable` rows when he is ready to test, whenever that is.

**Blocked:** item 6 — on the ICHRA reference data existing (item 4, Kevin's admin-UI setup), not on a
scheduled build item.

---

### S6 — Staging data, or wait for allow-listing? — ✅ **RESOLVED 2026-07-31**

**Decision: schedule the demo independently. Forrest is a partner, not a client — allow-listing gates
the client-facing demo, not his.**

**What it was.** Every cached row is `source_env = STAGING` and the illustration carries a *"Do not
present this to a client"* banner. The question was whether the Forrest demo waits on production
allow-listing.

**Why it does not.** **The banner draws a line between partner and client, and Forrest is on the
partner side of it.** He is being shown the machine. A system that visibly refuses to launder staging
data as production data is evidence *for* SSA, not against it — and the rates are real either way,
verified twice to the penny against the zizzl baseline.

**What allow-listing does gate** is the first time one of Forrest's agents puts a number in front of a
real prospect. That is downstream of every item in §3, and it remains a **HIGH** chase (§6) — but it is
no longer on the critical path to the demo.

⭐ **The consequence is the most useful thing this decision produces: items 1–10 now have no external
dependency at all.** Not a credential beyond the staging key already in hand, not a BAA, not counsel,
not a vendor reply. **The only thing standing between here and §1 is build time** — and the one
externally-gated item in the sequence, 11, is gated on an email nobody has sent.

**Blocked:** demo scheduling — now unblocked. No build item.

---

## 5. Deferred, and why

**Not in the sequence, by decision.** ⛔ = blocked on an external gate. ⏸ = would work, but does not
advance §1.

| Item | Why | What brings it in |
|---|---|---|
| **Enrollment lifecycle** (B5/B7) | ⛔ O12 (rep — the tightest bottleneck), O13 (BAA), O14 (deeplink model), O15, O16. **None has moved since 2026-07-29** | A rep assigned. Nothing is testable until then |
| **Verification ledger** (B3) | ⏸ Sells administration, which the thesis says utility must earn first. ⚖️ And LA-01-terminal — building it is safe, *releasing a dollar* against it is the point of no return | A sold ICHRA case |
| **Card transaction ingest** (B6) | ⛔ O10 (live carrier authorization test) and O9 (which MCCs are loaded — 6300, or 6300 + 5960). **If O10 fails this phase does not happen** | The authorization test passing |
| **The "+" catalog as a priced tier** (B1 proper) | ⏸ Item 4 delivers the *reference rows* the demo needs. The priced "+" bundle with its D14 pricing section sells administration | A sold case, and D18 decided |
| **Notice automation** (B4a/B4b) | ⏸ ⭐ **The strongest deferral on this list, and the least comfortable.** It fixes a problem that exists **today for existing clients**, both its feeds already exist and are already scheduled, and it gates on nothing. It is deferred only because it is invisible to Forrest | A gap between demo milestones. **Take it the moment one appears** |
| **SFTP / Summit Data Exchange** (P1) | ⛔ AMS has no SFTP client — net-new infrastructure. ⭐ **Administrator gate cleared 2026-09-09** — DataPath support confirmed no MOVEit administrator is needed; each TPA creates its own sub-folders under the three primary folders with its own login. The earlier **"no lead-time estimate exists anywhere"** applied to that now-cleared administrator ask | The client and delivery wiring — the folder-access ask is done |
| **A4b book radar** | ⛔ O24, which A4a (item 11) is designed to earn | A4a landing well |
| **A5 pipeline console** | ⏸ Reads item 13's opportunity link. Genuinely valuable, and not on the path to §1 | S3 resolved, plus item 13 |
| **Provider check (A2)** | ⛔ **⚠️ Owned by nobody.** O23 resolved favourably — the API takes a `providers[]` array and returns `covered` — but **it takes NPIs**, and the name-search endpoint (`GET /v1/reference/providers?query=`) is **HSOne's**, not this product's. Between *"agent types a doctor's name"* and *"API wants an NPI"* sits net-new work **nobody has sized**. The NPPES registry is public, so it is solvable | An owner and an estimate. ⚠️ **Until then A2's sizing in every document is wrong** |
| **Subsidy segmentation** | ⛔ `POST /api/v1/aptc_estimates` is documented and **never called**; the income request field is unverified on this product | One staging call to verify the shape |
| **Employee-facing anything** | ⛔ Crosses `legal_assumptions.md`'s scope line. LA-04/05/06 all trigger, and LA-09's central fact — the audience is a licensed agent — stops being true | A deliberate decision, with LA-09 in front of the person making it |
| **T47 / the Tarrant class of defect** | ⏸ Only four counties are warmed and all four returned data. **Becomes urgent the moment the county list grows** | The county-list decision (D-83) |

---

## 6. External gates and their status

**Everything with human lead time.** ⚠️ **Six items were sent to HealthSherpa on 2026-07-29 with no
recorded reply; a second round went out 2026-07-31. Four asks that gate later phases have never been
sent at all.**

| Item | Owner | Asked | Reply | Gates |
|---|---|---|---|---|
| **Production allow-listing** — production returns 403 | HealthSherpa | 2026-07-30, chased 2026-07-31; chased 2026-08-05, 2026-09-10 | ❌ none | **The first client-facing number**, not the Forrest demo (S6). Still HIGH — it is the gate between a working demo and a working product |
| **Onboarding representative** (O12) — routes credentials *and* the webhook form | HealthSherpa | 2026-07-29, chased 2026-07-31; chased 2026-08-05, 2026-09-10 | ❌ none | Everything on the enrollment rail. **The tightest bottleneck** |
| **BAA** — Geozoning, Inc. DBA HealthSherpa (O13) | HealthSherpa + counsel | 2026-07-28 flagged, 2026-07-29 sent; chased 2026-08-05, 2026-09-10 | ❌ none | Any production PHI flow. **Nothing in §3** |
| **BCBS TX policy status: when in 2026?** (O16) | HealthSherpa | 2026-07-29; chased 2026-08-05, 2026-09-10 | ❌ none | *"Largely determines a 2026 versus 2027 launch"* |
| **CHRISTUS policy status — planned at all?** | HealthSherpa | 2026-07-29; chased 2026-08-05, 2026-09-10 | ❌ none | Verification coverage in the launch market |
| **Deeplink: self-service or agent-completed?** (O14) | HealthSherpa | 2026-07-29; chased 2026-08-05, 2026-09-10 | ❌ none | Whether B5 is an employee portal or an agent workstation. **An architectural fork** |
| **Webhook auth methods** (O15) | HealthSherpa | 2026-07-29; chased 2026-08-05, 2026-09-10 | ❌ none | Whether `ApiTokenFilter` suffices |
| **MOVEit folder creation** | DataPath | ❌ **not asked** | — | P1. ⚠️ *"Requires a MOVEit administrator: a lead-time item."* **No estimate exists anywhere.** Pure lead time — ask today |
| **Encryption key exchange** (TPA/DP keys) | DataPath | ❌ **not asked** | — | P1 |
| **Counsel package** — LA-10 leads, then Group 2 (LA-01/02/03) | Counsel | ❌ **not sent** | — | ⚠️ **Nothing in §3.** LA-10 leads because it attaches to the **existing book today**, not to ICHRA later |
| **O22 book profile** + producing-agent count | SWBD | ❌ **not sent** | — | The county list (D-83), and how to size the opportunity |
| **"Send me three groups renewing next quarter"** | SWBD | ❌ **not sent** | — | **Item 11.** An easy yes. ⭐ **Send it before item 9 starts** |
| **O24 — the full group book** | SWBD | deliberately deferred | — | A4b. Earned by item 11, not asked cold |

**2026-09-10 — HealthSherpa status.** Still no reply on any of the seven rows above. Last written
HealthSherpa reply in the thread: **Julian Ferdman, 2026-07-29**. The staging key (issued 2026-07-30)
arrived through a channel not in the email thread. Kevin's sends on record: 2026-07-28 (KJ),
2026-07-29 (Julian, twice), 2026-07-30 (key request), 2026-08-05 (full list), 2026-09-10 (escalation).
⚠️ **The 2026-07-31 chase dates in the Asked column are not in Kevin's email history as reviewed
2026-09-10.** Left in place, unverified.

The 2026-09-10 escalation went To `ichra@healthsherpa.com`, Cc Julian, KJ, and **Michael Levin**.
It asked one question first, **who is SSA's onboarding contact**, since O12 routes staging deeplink
credentials and the webhook form. ⚠️ **`ichra@healthsherpa.com` bounced** (550 5.1.1, address not
found), even though HealthSherpa's own 2025-08-05 Policy Status API release names it. A forward to
**`ichra_support@healthsherpa.com`**, named in an undated agent help-center article, **also bounced**.
**Neither published ICHRA inbox is live; don't use either again.** The three individual cc's
delivered.

⭐ **Michael Levin is SVP & General Manager, ICHRA at HealthSherpa**, per his 2026-09-10 auto-reply
signature. He is the most senior HealthSherpa contact SSA has. The auto-reply said he was at ACA
Summit and slow to respond through 2026-09-11. It named no alternate contact.

**O16 reframed:** from *"when in 2026?"* to *"live for 2027-01-01 effective dates?"* With 2026
nearly over, the 2027 plan year is the question that shapes anything now.

**Docs rechecked 2026-09-10:** Supported Carriers (last updated 2026-08-24) still marks BCBS TX
Policy Status as coming in 2026, and CHRISTUS still has no Policy Status entry. The Webhooks API
page still documents authentication only as multiple supported methods, with no retry, delivery,
ordering, signature, or source-IP contract. **O15 stays open.** Detail: `docs/business/healthsherpa.md`,
2026-09-10 section.

⭐ **The two SWBD emails are the cheapest de-risking available anywhere in this plan** and neither has
been sent as of 2026-07-31. **Nobody has asked Forrest what he would want a quoting tool to do** — and
the entire agent-utility thesis descends from one sentence in one call (`ichra_strategy.md` §2).

**Chasing all of these costs an afternoon** and is the difference between waiting three weeks in August
and waiting three weeks in October.

**Detail:** `docs/ichra_strategy.md` §10 · `docs/business/healthsherpa.md` "Open items — 2026-07-29" ·
`docs/analysis/legal_assumptions.md` "What to price when counsel is engaged".

---

## 7. How this plan is maintained

**This document is expected to be edited every session.** A build plan that is not edited is not being
used.

**Rules:**

1. **Items move to done in place** — struck through, with a **date and a commit hash**. They are not
   deleted. A completed item is the record of what the sequence actually was, and the hash is how a
   later session finds the code.
2. **New items are appended and numbered onward.** ⚠️ **Never renumber.** Session close-outs, commit
   messages and prompts refer to item numbers, and renumbering silently invalidates all of them.
3. **Reordering is fine; renumbering is not.** If item 11 should now run before item 8, say so in §3's
   "Independence and parallelism" block. The numbers are identity, not order.
4. **Structural decisions (§4) move to RESOLVED with a date and the evidence** — the way S1 did. **A
   resolved decision is not deleted**, because the next reader needs to know it was checked rather than
   assumed.
5. **Deferred items (§5) move into §3 with a number when their gate clears** — and the §5 row records
   the date it moved and why.
6. **§6 rows get a reply date the day a reply lands.** ⚠️ A gate with no recorded reply is
   indistinguishable from a gate nobody chased, which is exactly the failure `ichra_strategy.md` §10
   exists to name.
7. **Session close-out records progress against item numbers**, not against prose. *"Item 5 done,
   `abc1234`; item 6 blocked on S2"* is the useful form.

**When this plan disagrees with the repo, the repo wins.** Migration numbers move, branches come and
go, and §2's corrections table exists because six separate documents were confidently wrong about
things a five-minute code read settles.

### 2026-08-01, session 6 — flow shipped after §3 completed (rule 1)

§3's thirteen items were all struck through before this session; these are **flow fixes between**
finished items, found by walking the eight surfaces as one path for the first time. The map is
`docs/analysis/ichra_flow_and_handoffs.md`.

| Shipped | Hash | What |
|---|---|---|
| **G1 / T59** | `e849dac` | Affordability (**§1 step 5**) had no entry point — two hub cards, one URL. Second card now points at `Illustration?mode=AGE_BAND&affordabilityBasis=FPL`. The URL always existed; nothing linked to it. |
| **G3** | `724cc3e` | Hub cards ordered and numbered to §1's walkthrough. Group conversion labelled "Separate analysis", not step 4 — no census carries into it. |
| **G5** | `56e7f07` | Total lives now carries AGE_BAND → RANGE. The reverse is deliberately not built: a flat total has no age to sit against, and seeding `count1` with it is wrong for the Sandoval case (3 lives, 3 ages) and wrong silently. |

⚠️ **Two corrections to this document, both in §1's walkthrough:**

1. ~~**Step 5 says "moves the contribution slider."** There is no slider — contribution is a number input
   and every change is a full form re-submit (`illustration25.jsp:152-156`).~~ **Resolved the same day,
   prompt B — the document was right and the code was behind it.** See the G9 row below.
2. **Step 6 says "prospect pre-filled."** The hand-off carries no prospect id, deliberately and with the
   reasoning stated in source (`illustration25.jsp:344-352`). The prospect step is a real click. Filed
   as **T69** — the cheap resolution is to fix this sentence, not the code. **Still open.**

**And one thing §3 completing does not cover: affordability needs two `constant` rows
(`ICHRA_AFFORDABILITY_PCT_<year>`, `FPL_ANNUAL_<year>`) that nothing in the repo seeds** (**T65**,
HIGH). Every code path for step 5 is built and every one of them fails closed to "not configured"
without those rows. That is config, and it is Kevin's.

### 2026-08-01, session 6 prompt B — step 5 made real

| Shipped | Hash | What |
|---|---|---|
| **T65 / G2** (code side) | `e90515a` | Both constants seeded in `DatabaseInitializer`, idempotently: `ICHRA_AFFORDABILITY_PCT_2026 = 0.0996`, `FPL_ANNUAL_2026 = 15960`. **New `LA-14`** records which of the two published FPL figures this is and why. ⚠️ **Does not reach production** — that seed runs on fresh installs only; the production rows remain a manual deployment item. |
| **G9** | `53a8131` | The contribution slider §1 step 5 has always described. Recomputes net cost, group total, employer outlay and each affordability verdict **client-side and live**; never recomputes the flip point (it does not move with the contribution); rewrites the proposal hand-off href as it moves so a snapshot cannot disagree with the screen. |

**§1 step 5's own words now work end to end in code** — *"at $350 Maria keeps her subsidy and comes out
ahead, at $450 she loses it"* is a drag, and the per-employee threshold updates under it. **What is not
yet true is that it works on production**, because of T65's config half. The distinction is the whole
point of the click-script in `docs/analysis/ichra_flow_and_handoffs.md` §3.

**Detail:** `CLAUDE.md` "Keeping state docs current" — this plan is a state-carrying document and
**should not be synced to project knowledge.**

---

## Related

- **Strategy — read first:** `docs/ichra_strategy.md`
- **Design detail, D1–D37, O1–O40:** `docs/analysis/plus_tier_build_plan.md` (Part 8 governs) ·
  `docs/business/plus_tier.md`
- **Compliance:** `docs/analysis/legal_assumptions.md` (LA-01–LA-12, assumptions) ·
  `docs/analysis/domain_and_compliance_rules.md` (settled rules)
- **The API:** `docs/business/healthsherpa.md` — read backwards, 2026-07-31 section first
- **The relationship:** `docs/business/swbd_premiumpath.md`
- **Service scope:** `docs/business/ichra_administration_scope.md` ·
  `docs/business/ichra_platform_capability_map.md`
- **State:** `docs/analysis/migration_tracker.md` · `docs/deployment_backlog.md` ·
  `docs/analysis/project_backlog.md`
