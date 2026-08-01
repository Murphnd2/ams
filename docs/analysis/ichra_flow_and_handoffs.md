# ICHRA flow and hand-offs — what an agent opens, and what it feeds

Traces the ICHRA surfaces as **one path**, for a **role-2 agent** of an `ichra_enabled` agency, against
`docs/swbd_ichra_build_plan.md` §1's eight-step walkthrough. Written 2026-08-01 against `a79de12`.

**Method:** read from source only. Every cell traces to a file and line read this session. Nothing is
carried over from `swbd_ichra_build_plan.md`, session close-outs, or `ichra_demo_path_role_walk.md` —
each of those was wrong on at least one row checked here. Rows that could not be settled from source
are marked `unverified`.

**Surface-set correction.** Session 5 recorded eight surfaces. **Seven are surfaces; one is not.**
`/IchraOpportunityAnalyses` is a JSON feed consumed by `agentHome25.jsp`'s pipeline drawer — its own
Javadoc says "not a page and not linked from anywhere" (`IchraOpportunityAnalyses.java:28`). No ninth
surface exists: `LandingServlet` (`/market/landing`) and `RequestQuote` (`/RequestQuote`) share the
`market` package but are the public agency-landing and quote-form pages, neither ICHRA-gated nor
ICHRA-related.

---

## §1 — Surface table

| Surface | URL | `@WebServlet` | Gate (verbatim) | User must supply | Produces | Outbound links on the result |
|---|---|---|---|---|---|---|
| ICHRA hub | `/IchraHome` | `IchraHome.java:18` | `IchraAccessResolver.isAvailable(em, request)` → else `sendRedirect("/")` (`:23-26,37`) | nothing | 5–6 cards | `Illustration`; `RateCacheAdmin` (PSP-admin only, `:85`); `Illustration?mode=AGE_BAND` ×2; `GroupConversion`; Design Advisor = `onclick="toggleChatbox()"`, no URL (`:125`) |
| Rating-area illustration | `/Illustration` (default `mode=RANGE`) | `IllustrationServlet.java:62` | `IchraAccessResolver.isAvailable(em, request)` → else `SC_FORBIDDEN` (`:80-83,413`) | county, plan year, headcount | bronze/silver/market-low at ages 21/40/64; group monthly range | **Range/Age Band** toggle (`illustration25.jsp:64-67`) — carries `countyFips`, `planYear`, `opportunityId`; **not** `headcount`. **"Use This in a Proposal"** → `ProposalBuilder?mode=RANGE&countyFips&planYear&headcount` (`:537-555`), disabled unless `sourceEnv == 'PRODUCTION'` |
| Age-band net cost | `/Illustration?mode=AGE_BAND` | same servlet, `:140-141` | same, once at `doGet`; sub-modes carry no further gate | county, plan year, up to 6 (age, count) rows, employer monthly contribution | per-band net after contribution, group net total, employer outlay | same toggle; **"Use This in a Proposal"** → `ProposalBuilder?mode=AGE_BAND&countyFips&planYear&contribution&age1..6&count1..6` (`:353-377`). Never carries affordability, income, or `opportunityId` |
| Affordability threshold | **`/Illustration?mode=AGE_BAND&affordabilityBasis=FPL\|INCOME`** — addressable, but nothing linked to it before this run (**T59**) | same servlet, `:366-407` | same | everything AGE_BAND needs **plus** a basis; `INCOME` additionally requires an income on *every* row (`:261-268`) | per-row on-exchange LCSP, flip contribution, PTC kept/lost | none of its own — it renders inside the AGE_BAND result, above that page's proposal hand-off |
| Group-to-ICHRA conversion | `/GroupConversion` | `GroupConversionServlet.java:76` | `IchraAccessResolver.isAvailable` on **both** `doGet` and `doPost` → else `SC_FORBIDDEN` (`:94-97,121-124`) | county, plan year, census rows, current total premium, current employer share, proposed contribution, optional per-row deduction | employer cost comparison, per-employee position, plain-language summary | **none, by design.** `groupConversion25.jsp:8-14` and `GroupConversionServlet.java:51-53` (D24): no print, PDF, export, email, share-link, download or proposal hand-off, "and none may be added" |
| Design advisor | no URL — `toggleChatbox()` on a widget included by `navbar25.jsp:411-413` | n/a (posts to `/ChatAssistant`) | `chatbotEnabled && ((isPspAdmin \|\| (isPspUser && chatbotAllUsers) \|\| isBpoAdmin \|\| (isBpoUser && chatbotAllBpoUsers)) \|\| ichraAvailable)` (`navbar25.jsp:411`). Hub card uses `${applicationScope.global.chatbotEnabled}` alone (`ichraHome25.jsp:122`) — equivalent *on that page only*, since the disjunct is constant-true there | a typed question | text answer with inline `Source:` citations | none |
| Rate cache admin | `/RateCacheAdmin` | `RateCacheAdmin.java:43` | `Boolean.TRUE.equals(session.getAttribute("isPspAdmin"))` → else redirect `/` (`:167-170`). **Not** `IchraAccessResolver` | plan year / county to warm | cache status, manual refresh | none |
| *(not a surface)* opportunity analyses | `/IchraOpportunityAnalyses?opportunityId=` | `IchraOpportunityAnalyses.java:50` | `isAvailable`, then `OpportunityAuthz.canAccessOpportunity`; every failure returns `[]` (`:85-103`) | an opportunity id | JSON: date, kind, agent — no figures | n/a; drawer renders read-only text, **no link to start or open an analysis** (`agentHome25.jsp:912-927`) |

---

## §2 — Gap table, ranked by demo impact

| # | Gap | Demo step | Files that would close it | In fence? | Size |
|---|---|---|---|---|---|
| **G1** | **Affordability has no entry point.** Two hub cards, one URL (**T59**). The card named for step 5 lands on AGE_BAND with the basis dropdown on "None", so the flip point does not appear until the agent finds and changes it | **5** — the demo's money moment | `ichraHome25.jsp:101` | ✅ | XS — **built** |
| **G2** | **Affordability's two constants are seeded nowhere.** `ICHRA_AFFORDABILITY_PCT_<year>` and `FPL_ANNUAL_<year>` appeared in exactly one file — the reader. No migration, no `DatabaseInitializer`, no seed script. Absent → "Affordability is not configured for plan year N" and no table | **5** | `DatabaseInitializer.addIchraAffordabilityConstants` | ⚠️ **partly closed** — see below | XS for Kevin, **still blocking on production** |

> **G2 update, 2026-08-01 (prompt B, `e90515a`).** Seeded in `DatabaseInitializer` — `ICHRA_AFFORDABILITY_PCT_2026 = 0.0996` (IRS Rev. Proc. 2025-25) and `FPL_ANNUAL_2026 = 15960` (2026 HHS guidelines, one person, 48 states + DC), idempotent, never overwriting an existing row. **This does not reach production.** `addPspConstants` runs only from `initializeDataBase`, the one-time key-gated fresh-install path in `InitializeDataBase.doPost`; nothing re-runs it against an initialized database. Every *future* installation now has them and the correct values live in code — **the production rows are still Kevin's to add by hand**, and click-script step 10 is still the thing that settles whether they are already there. The percentage is stored as a **decimal fraction** (`0.0996`, not `9.96`) because `AffordabilityCalculator.flipContribution` multiplies it straight into monthly income; AMS has no other percentage-valued constant, so the calculator's own contract is the sole authority. FPL year disambiguated in **LA-14**. The two "not configured" messages now name the missing constant.
| **G3** | **Hub is an unordered grid.** Six cards, no numbering, no sequence; the rate-cache admin card sits second for a PSP admin. Nothing says which is step one, and the two AGE_BAND cards read as different tools | **2** — "a product area, not a calculator" | `ichraHome25.jsp` | ✅ | S — **built** |
| **G4** | **`opportunityId` has zero producers.** Both `/Illustration` and `/GroupConversion` accept and scope-check it (`IllustrationServlet.java:436-450`), and `illustration25.jsp:95-97` carries it across re-submits — but **no page in the codebase ever emits an ICHRA URL containing it.** The drawer reads analyses and renders nothing when empty, so the loop has no start. Item 13's attribution is reachable only by hand-typing a URL | ~~pipeline tie-in (not a numbered step)~~ → **stage 8**, §4.1 | `agentHome25.jsp` — a "Run an ICHRA analysis" link in the drawer | ❌ non-ICHRA JSP, forbidden | S |

> **G4 — Answered, 2026-08-01 (§4.2).** `plus_tier_build_plan.md` Part 3 §A5 states the intended flow: the pipeline console is built *"from `Opportunity` + V070 hierarchy + V071 tokens + **A1's illustration log** + A4's book"*, and **D22** makes illustration access authenticated *specifically* so every run is attributed to a named agent and sub-agency. The attribution column is not decoration awaiting a use — **it is the input A5 was designed to read.** Re-ranked to §2's top three by sale-motion impact.
| **G5** | **Lives do not carry across the mode toggle.** County and plan year carry (`illustration25.jsp:64-67`); headcount does not, in either direction | **3 → 4** | `illustration25.jsp:64-67` | ✅ partly | XS — **AGE_BAND → RANGE built; RANGE → AGE_BAND deliberately not, see below** |
| **G6** | **Design advisor is always cold.** The widget posts `JSON.stringify({question})` and nothing else (`chatAssistant25.jsp:170-174`) — no URL, county, plan year, contribution or opportunity. An agent asking "is this affordable?" while looking at a result gets an answer that cannot see the result | 5 (adjacent) | `chatAssistant25.jsp` + `ChatAssistant.java` | ❌ shared, non-ICHRA | M |

> **G6 — Confirmed but downgraded, 2026-08-01 (§4.2).** A real gap; lower value than §2 implied. A6's decided scope is *rules* Q&A — S-corp eligibility, whether a dental plan disqualifies a QSEHRA, MEC narrowing, class minimums — under a *"Hard boundary. Education with citations. **Never plan selection**"*. Feeding it a live case's county, contribution and per-employee figures moves it toward per-case advice, which that boundary and D24 both constrain. Any context-passing design has to answer what it may send before it answers how.
| **G7** | **Census does not travel between the two tools.** An agent who typed a census into `/GroupConversion` retypes it on `/Illustration`, and vice versa | 4 ↔ 5 | ~~`GroupConversionServlet` (`doGet` echo) + a link~~ → **A4b's book import** | ❌ **do not build the link** — the servlet is POST-only *because* "an employer's current premium and payroll deductions have no business in a URL" (`:71-74`), and its JSP forbids adding outbound affordances (D24) | M |

> **G7 — Answered, 2026-08-01 (§4.2). The proposed fix was the wrong one.** These are two stages, not two views of one: A3's design work runs off a **design census** (**D21** — age, ZIP, family tier, income band, a deliberately separate object), while A4a's conversion needs the employer's current group premium and its decided input is an **imported book row** (**D23**, `agency_book_group`) scanned by A4b's radar. Carrying a hand-typed census between two pages solves a data-entry annoyance in a workflow the documents do not describe. **Close as specified; the real work is A4b.**
| **G8** | **Yardstick says "prospect pre-filled"; code deliberately does not.** §1 step 6 promises ProposalBuilder with the prospect pre-filled. Both hand-off URLs carry "never a prospect id … ProposalBuilder prompts for the prospect as it always does" (`illustration25.jsp:344-352,530-536`) | **6** | `ProposalBuilder` + a prospect picker on the illustration | ❌ forbidden servlet, and it is a recorded decision, not an oversight | M |

> **G8 — Answered, 2026-08-01 (§4.2).** Prompt A could cite only a source comment; there is a **decision** behind it. A1's scope: *"No prospect PII, no employer record required."* **D7**: the full census is collected post-sale at setup, because *"quoting needs no PII."* **D21** splits the pre-sale design census from the administrative one *specifically* to keep the entire sales stage outside the BAA question. `plus_tier.md` titles the stage *"Quote (pre-application, no PII)."* **Fix the walkthrough sentence (T69); do not build the pre-fill.**
| **G9** | ~~**Step 5 says "moves the contribution slider."** There is no slider — contribution is a number input and each change is a full form re-submit~~ | **5** | `illustration25.jsp` | ✅ **built** | S–M |

> **G9 closed, 2026-08-01 (prompt B, `53a8131`).** The AGE_BAND result carries a slider that recomputes
> net cost per employee, band totals, group net, employer outlay and each employee's affordability
> verdict **client-side and live** — no POST per tick, no AJAX, no new endpoint. `/Illustration` stays
> GET-only. **It never recomputes the flip point:** flip = onexLCSP − pct × (income ÷ 12) does not depend
> on the contribution, so the server's figure is final and is read back from a `data-flip` attribute —
> the regulated computation stays in `AffordabilityCalculator` with no JavaScript twin to drift from it,
> and the two constants are consequently not needed client-side. Nothing is persisted. **The proposal
> hand-off href is rewritten as the slider moves**, closing a trap the slider would otherwise have
> introduced: dragging to $350 and clicking through would have snapshotted the submitted $400 silently.

### Answers to the seven questions — three hypotheses were wrong

1. **RANGE → AGE_BAND: county carries, lives do not.** `countyFips`, `planYear` and `opportunityId` are on the toggle URL; `headcount` is not (G5).
2. **`/GroupConversion` has no proposal hand-off — and must not get one.** The hypothesis assumed an oversight; it is D24, stated twice in source. Only the illustration (item 7) has one. **Hypothesis wrong.**
3. **No.** A census entered in one tool never reaches the other (G7), and closing it collides with the POST-only decision.
4. **Unordered card grid.** No numbering, no ordering, no "start here"; a PSP admin sees the rate-cache admin card in position two. Step one *should* be the rating-area illustration (G3).
5. **No — and worse than "only linked afterward."** It cannot be linked afterward either, from the UI: `opportunityId` has no producer anywhere (G4). Attribution works only if someone hand-edits the URL.
6. **T59 confirmed.** `ichraHome25.jsp:94` and `:101` are byte-identical hrefs. But the premise that affordability "has no URL of its own" is **half wrong**: `affordabilityBasis` is a first-class request parameter (`IllustrationServlet.java:238`) and the JSP pre-selects the dropdown from it (`:160-162`), so the URL always existed — nothing linked to it.
7. **Every conversation is cold.** Verified at the fetch call, not inferred (G6).

---

## §3 — What only Kevin can observe

`ichra_demo_path_role_walk.md` was written statically and marked the ICHRA nav row `pass` while that
entry was failing in production for the intended audience. Nothing below is asserted from code alone.

### Click-script — run on production as a real role-2 agent (Forrest's login shape, not a PSP admin)

| # | Do | Look at | Fails if |
|---|---|---|---|
| 1 | Log into `premiumpath.net` | the top nav | no **ICHRA** entry (`isAvailableForNav` cached false — log out, back in, re-check) |
| 2 | Click **ICHRA** | the hub renders | redirected to `/` — live `isAvailable` disagrees with the cached nav hint |
| 3 | Count the cards | 5 cards, each numbered 1–5 | a **Rate Cache Admin** card appears — a role leak; it must be invisible to a non-PSP-admin |
| 4 | Click card **1 · Rating-Area Illustration** | a **ZIP** box *and* the county dropdown, both present | the county dropdown is gone — it must stay; it is the way through when a ZIP does not resolve |
| **4a** | Type a **single-county ZIP** (`75482`, Hopkins) and **Illustrate** | goes straight to results for Hopkins, exactly as picking the county would | a chooser appears for a ZIP that touches one county, or nothing happens |
| **4b** | Type a **crossing ZIP** (`75009` — Collin **and** Denton) and **Illustrate** | *"ZIP 75009 is in more than one county"* + a list of both. ⚠️ **Nothing pre-selected, no county marked likely or recommended** | one is auto-selected, pre-checked, or highlighted as the probable answer — **that is the failure this whole design exists to prevent**; a wrong county returns wrong rates that look exactly like right ones |
| **4c** | Click one county in that list | ordinary results, and the **URL now carries `countyFips=`** so it is linkable and shareable | it stays on a `zip=` URL, or the result is not linkable |
| **4d** | Type a ZIP that is **not in the crosswalk** (an out-of-state one, e.g. `90210`) and **Illustrate** | ⚠️ *"We don't have ZIP 90210 in our county lookup"* — naming **our** coverage gap and pointing at the county selector | it says **"invalid ZIP"** or anything implying the agent mistyped. The ZIP is real; the data is ours and it is incomplete. **An agent who thinks he mistyped retypes it three times** |
| **4e** | Confirm the old path still works: open `Illustration?countyFips=48223&planYear=2026` directly | the dropdown **pre-selects Hopkins**, plus the headcount error | the dropdown shows the placeholder — that would be a regression in the contract the mode toggle, the hub cards and T59's fix all ride on. *(Settled from code 2026-08-01: it does pre-select. `submittedCountyFips` is set unconditionally and the option tag selects on it.)* |
| **4f** | ⭐ **The R1 case.** With **Hopkins already selected** from step 4a, type **`75009`** and blur | the county selection **clears**, then the two-county chooser appears | ⚠️ **Hopkins results appear.** That is R1 — a stale selection beating a typed ZIP, wrong rates indistinguishable from right ones. This is the single most important check on the page |
| **4g** | Type a ZIP and **Tab out** — do not press Enter | it resolves on blur: dropdown fills, or chooser, or no-match. **No page reload, no validation error** | nothing happens until Enter, or Enter produces *"Enter a valid number of eligible employees"* — that is R2 |
| **4h** | With a result or panel on screen, edit the ZIP | the panel and the county selection **clear immediately**, before any lookup returns | the old panel lingers (R4) |
| **4i** | Hopkins selected, **blank headcount**, press Illustrate | the headcount error **only** | *"No rate data for this county yet"* also appears — that is R3, and it is the unwarmed-county message firing for a validation failure on a county that demonstrably has rates |
| **4j** | Disable JavaScript, then repeat 4a / 4b / 4d | identical outcomes — resolution, chooser and no-match all still work | any of them stops working; the script is an enhancement, and the servlet resolves `?zip=` regardless |
| **4k** | ⭐ On the `75009` chooser, look at the two entries | **Collin and Denton each carry *"— no rates cached yet"*** unless they have been warmed. Both keep the same link, weight and order | neither is labelled — then the next step will reject a county the page just offered |
| **4l** | Click an entry labelled *"no rates cached yet"* | ⚠️ **"We don't have rates for Collin County, TX yet."** | *"Select a valid county from the list"* — that blames the agent for our coverage gap, and is the defect this run fixed |
| **4m** | Open `Illustration?countyFips=48085&planYear=2026` directly (Collin — in the crosswalk, not in the cache) | the same *"We don't have rates for Collin County, TX yet."* | the generic invalid-county message; every entry path must give the same answer |
| **4n** | Open `Illustration?countyFips=NOTAFIPS&planYear=2026` | *"Select a valid county from the list."* — correct here, because it genuinely is not a county | it claims we have no rates for it, which would be false |
| 5 | Hopkins County, 3 lives, **Illustrate** | figures + "Source: production" | any red *Test-environment rates* banner — do not demo |
| 6 | Click **Age Band** in the toolbar | county stays; **Eligible Employees carried nothing** | *expected* — G5's forward direction is not built |
| 7 | Enter the three Sandoval ages, count 1 each, contribution 400, **Illustrate** | per-band table + group net | any age reported as missing cache data |
| 8 | Click **Range** in the toolbar | headcount pre-filled **3** | blank — the G5 carry built this run did not ship |
| 9 | Back to hub, click card **3 · Affordability Threshold** | **Affordability Basis reads "FPL Safe Harbor", not "None"** | reads "None" — T59's fix did not ship |
| 10 | Re-enter county/ages/contribution, **Illustrate** | the **Affordability Threshold** block with flip contributions | **"Affordability is not configured for plan year 2026 — the constant ICHRA_AFFORDABILITY_PCT_2026 is missing or invalid"** (or the same for `FPL_ANNUAL_2026`) → **G2/T65: add that row. The message now names it. The `DatabaseInitializer` seed shipped 2026-08-01 does NOT reach an already-initialized database.** Values: `ICHRA_AFFORDABILITY_PCT_2026` = `0.0996` (decimal fraction, not `9.96`), `FPL_ANNUAL_2026` = `15960` |
| 10b | Drag the **Employer Monthly Contribution** slider | net cost, group total, outlay and each verdict all move live; no page reload; the "Use This in a Proposal" link follows the slider | nothing moves (script error — check the console), or the verdict column stays frozen while net cost moves |
| 11 | Open the chat widget, ask *"Does my client's dental plan kill the QSEHRA?"* | a cited answer, no 404 | no widget at all → the navbar gate; a 404 → a retired model string |
| 12 | Run **Illustrate** once more, then check `illustration_log` | a new row, `opportunity_id` **NULL** | *expected* — G4; NULL is the only value the UI can produce |

### `ichra_demo_path_role_walk.md` — corrections applied

Four rows were stale and are corrected in that file this run; every row is now explicitly marked
`code-verified` or `runtime-verified`. The substantive ones: its gate definition described
`IchraAccessResolver` as primary-agency-only (session 5's `a5c0d8d` widened it to the full membership
set); its Design Advisor card row quoted a gate expression the JSP no longer contains; and its §3
verdict predated both T57/T58 fixes.

### `unverified`

- **Whether `ICHRA_AFFORDABILITY_PCT_2026` / `FPL_ANNUAL_2026` exist on production.** Source proves
  nothing seeds them; only a `SELECT` on `constant` settles whether Kevin added them by hand.
  Click-script step 10 settles it.
- **Whether the nav entry renders for Forrest's session today.** `isAvailableForNav` caches per
  session; a session predating the entitlement flip caches `false`. Step 1 settles it.

---

## §4 — The sale motion

Added 2026-08-01 (prompt C). §1–§3 are surface-first. This section reads the **business and plus-tier
documents**, which are agent-first, against them. Deciding documents: `plus_tier_build_plan.md`
(29 Jul, Rev 6, **Part 8 governs**), `plus_tier.md` (29 Jul), `ichra_administration_scope.md` (28 Jul),
`ichra_platform_capability_map.md` (29 Jul), `swbd_premiumpath.md` (30 Jul), `healthsherpa.md` (31 Jul).

### §4.1 — The motion

> ⚠️ **Two sources, different standing.** The stage table below was derived from **dated business
> documents**. Everything marked **[K 8/1]** comes from **Kevin's direct account of how agents work**,
> given conversationally on 2026-08-01 — a *better* source about agent behaviour than any internal
> document, and a *weaker* one about what the code does. Nothing marked [K 8/1] is a document citation.

#### The shape, per Kevin — this supersedes the eight-stage ordering below **[K 8/1]**

> *"An agent aware of a prospect goes into AMS and generates a proposal for an ICHRA/QSEHRA solution."*

One intake, three levels of input fidelity, one proposal out:

| Tier | Input | Typical case | Today |
|---|---|---|---|
| **1** | ZIP + headcount | New prospect, nothing in hand | ⚠️ county dropdown, not ZIP (**T74**) |
| **2** | ZIP(s) + age-banded headcounts | Prospect who has shared something | ✅ AGE_BAND — single county only |
| **3** | Actual census | Existing client | ⚠️ 6 hand-typed rows; `/GroupConversion` is **tier 3 only** (**C4**), since it needs a current group premium that exists only on an established client |

**The proposal is the centre of the product, not a document at the end of it [K 8/1].** It carries
employer-facing evaluation tools — current group rates where they exist, expected or known increase —
and SSA is visible in it. Two uses, needing different things:

- **Employer-driven** — sent cold, worked alone. *"There are just some employers who want to mess
  around on their own and this would be perfect for."* Must stand up with no agent present.
- **Agent-driven** — he is in the room on a phone or laptop working examples, discussing scenarios
  outside the proposal. The proposal that follows carries an application link and is **close to a sold
  case**. This is why **T78 (mobile) is a tier-3 requirement, not a nice-to-have.**

**Triggers — context, not structure [K 8/1].** An existing book turns on a **renewal increase**; a flat
renewal is left alone. A **prospect** can be approached any time, because ICHRA can beat even a flat
renewal. A group with **no coverage, or resolved to drop it**, is the cleanest case — and the one where
the doctor objection barely lands.

#### The stage table — **superseded as an ordering**, retained for what it maps

Stage 1 and stages 6–8 survive intact; stages 2–5 are re-read through the tiers above.

| # | Stage | Who acts | Surface | Produces → consumed by |
|---|---|---|---|---|
| 1 | **Entity / eligibility screen** — sole props, partners, >2% S-corp shareholders cannot participate; any other group health plan disqualifies a QSEHRA | Agent | ⛔ **none** | eligibility result, which *"filters the LOS menu"* (`plus_tier.md` §Quote) → stage 2 |
| 2 | ~~**Subsidy segmentation**~~ — **cannot happen here (C1) [K 8/1]**. PTC eligibility turns on **household income**, and the agent has ages, not wages, until well after first contact. Real, but late-stage | Agent | ⛔ none | — |
| 3 | **Market illustration** — county → premium range | Agent | ✅ `/Illustration` | range → stage 4 |
| 4 | **Design** — age-band net cost, affordability threshold per employee, **class optimization** | Agent | ✅ AGE_BAND + affordability · ⛔ **no class optimization** | a priced design **and a design fee** (A3) → stage 6 |
| 5 | **"Will I lose my doctor?"** — ~~per-employee NPI provider check~~ → **carrier network breadth, group vs individual, with known discrepancies named (C5) [K 8/1]** | Agent | ⛔ **none** — O23 **resolved favorably** (Part 8, 30 Jul), so unblocked, not gated | the objection that kills cases, answered |
| 6 | **Proposal → application → setup** | Agent, then employer | ✅ hand-off → `ProposalBuilder`; ⛔ ICHRA task sequence not yet entered | signed setup |
| 7 | **Renewal defense** — import the group book, radar surfaces renewals 90–120 days out, auto-runs the conversion | SSA scheduler, output to agent (**D24**) | ⚠️ `/GroupConversion` is A4a's *analysis*; **A4b's radar and `agency_book_group` do not exist** | a queue of pre-analysed opportunities |
| 8 | **Pipeline console** (A5) | Agent / GA | ⚠️ partial — drawer + `/IchraOpportunityAnalyses`, fed by nothing (**G4**) | network visibility |

`not decided`: where stage 1's screen lives (no document places it on a surface), and whether stage 5 is
agent- or employee-driven — `ichra_platform_capability_map.md` Layer 3 calls that out as the open
question that *"determines whether this is an employee portal or an agent workstation."*

### §4.2 — What this settles in §2

**§2 has 5 open rows** — G2, G4, G6, G7, G8. (G1, G3, G9 built; G5 built one way, the other direction a
recorded decision.)

| Row | Verdict | Deciding document |
|---|---|---|
| **G2** — production constants | **Untouched** | nothing read bears on whether the rows exist |
| **G4** — `opportunityId` has no producer | **Answered** | Part 3 §A5 builds the pipeline console *"from `Opportunity` + V070 + V071 + **A1's illustration log** + A4's book"*, and **D22** makes access authenticated precisely to get per-agent attribution. G4 is not a nice-to-have; it is A5's data source |
| **G6** — advisor is cold | **Confirmed, and downgraded** | A6's decided scope is *rules* Q&A — *"Hard boundary. Education with citations. Never plan selection."* Feeding it live case figures pushes it toward per-case advice, which that boundary and D24 constrain. Real gap, lower value than §2 implied |
| **G7** — census does not travel between the tools | **Answered** | wrong fix. A3 works from a **design census** (**D21**, a separate object); A4a needs the current group premium and its decided input is an **imported book row** (**D23** `agency_book_group`), not a hand-carried census. The answer is A4b, not a link between two pages |
| **G8** — prospect not pre-filled | **Answered** | A1's scope is *"No prospect PII, no employer record required"*; **D7** collects the full census post-sale; `plus_tier.md` titles the stage *"Quote (pre-application, no PII)"*. The walkthrough sentence is what is wrong — as T69 already proposed, now backed by a decision rather than a code comment |

**Six stages carry no surface at all** and were invisible to a surface-first map: stage 1 (**N1**
eligibility screen), stage 2 (**N2** subsidy segmentation), stage 4's class optimization (**N4**),
stage 5 (**N5** provider check), stage 7's radar (**N6**), and **N3** — A1's scope is *"ZIP → county +
FIPS"* but the page offers only a county dropdown; `healthsherpa.md` (31 Jul) closes the API route
(*"AMS must carry its own county reference data"*) while `county_reference.representative_zip` already
carries it locally, unwired.

~~**Re-ranked by sale-motion impact.**~~ ⚠️ **Wrong at the top; superseded below.** It read: 1. N1+N2
intake front end · 2. N5 provider check · 3. G4 attribution. **C1 removes N2 from first place** —
subsidy segmentation needs household income, which the agent does not have at first contact. Kept for
provenance.

#### Corrected ranking, 2026-08-01 (prompt D), six corrections applied **[K 8/1]**

1. **T74 — ZIP intake.** Tier 1 *is* the motion's front door, and today it asks for a county. **Data
   layer shipped 2026-08-01** (V084/V085 + resolver); **the servlet and JSP remain.** See §5. Prompt C
   sized this as wiring; it was a data build, and the crosswalk had to be created.
2. **T81 — the interactive employer proposal.** The proposal is the centre of the product, not its
   output. A **phase, not an item**; ships as a **sandbox first** (Part 5 decision) — render everything,
   store nothing.
3. **T80 — cost certainty.** *"The through-line of the whole pitch, and it is unbuilt."* Raised
   independently at two separate points in the walkthrough. ⚠️ **Mechanism comparison, not a forecast.**

**Moved down.** **T71** (subsidy segmentation) falls from first to **late-stage** — it needs wages
(**C1**). **T70** (entity screen) stays real and first-in-sequence, but is a small gate, not a phase.
**T75** (class optimization) defers — 50+ groups are not the prospect set — **but partly returns for a
different reason (C3): geographic rating area is a permitted ICHRA class**, so a 14-person group across
two rating areas structurally needs two classes. Both halves hold; they are not in tension. **T73**
shrinks to network breadth (**C5**) and gets cheaper — aggregate, cacheable, identical for every
employee in a county, **touching no employee data**, so it clears build rule 3 outright.

**Dissolved (C6):** the leave-behind gap. The proposal *is* the leave-behind; nothing separate to build.

**G2 still drops off this ranking without becoming less urgent** — it blocks the *demo*, not the motion.

#### Part 1 — four claims tested against source, 2026-08-01

Asserted conversationally from names and memory; each was checked against the schema and the code.

| # | Claim | Verdict |
|---|---|---|
| 1 | `county_reference` is county → **one** ZIP, so it cannot serve reverse lookup | ✅ **correct.** V076: PK `county_fips`, `representative_zip CHAR(5) NOT NULL` — one row per county, 254 for TX. A reverse lookup would match only those 254 ZIPs. ⚠️ **But §4.2's own earlier line calling it *"exactly that data, unwired to the UI"* was wrong**, and so was T74's original *"small: a ZIP box that resolves through `CountyReferenceDAO`"* |
| 2 | Some ZIP → county crosswalk exists | ❌ **none, anywhere.** `CountyReferenceDAO` exposes `findByFips`, `listByState`, `findByFipsIn` — all county-keyed. No resource file, no seeded table, and no API route (`healthsherpa.md` 31 Jul: *"the ZIP→FIPS route via this API is closed"*). **A build item, not a wiring item** |
| 3 | The rate cache is keyed by rating area | ❌ **keyed by county FIPS.** `uq_rarc_year_county_age_tobacco (plan_year, county_fips, age, uses_tobacco)` — and **there is no `rating_area` column at all**, despite the table being named `rating_area_rate_cache`. So **"dedupe by rating area before pricing" cannot be done against this cache**; dedupe is by county FIPS — correct, but coarser (two counties sharing a rating area still hold separate identical rows) |
| 4 | The ICHRA JSPs are fixed-width desktop markup | ❌ **overstated.** All three carry `<meta name="viewport" content="width=device-width, initial-scale=1">` and load Bootstrap 5.3.3. The real problems are narrower: **five-column result tables with no `overflow-x` wrapper and no `.table-responsive` anywhere in the ICHRA path**, ~10 fixed-px input widths (65–190px), and a `height: calc(100vh - 64px)` shell that fights mobile browser chrome. **Viable, not designed for it** — which sizes T78 down |

### §4.3 — The agent's job, and where the tools do not fit it

**The built set covers the middle of one stage of an eight-stage motion.** Stages 3 and 4 are real,
good, and finished. Stages 1, 2, 5 and 7 have nothing, and 1 and 2 come *first* — the tools begin at
"pick a county" and the agent's job begins two decisions earlier.

Two tools sit on steps the agent does not perform in the motion as decided:

- **`/GroupConversion` as a hand-keyed form.** A4a's decided input is a book of groups auto-scanned by
  A4b's radar (**D23**), with output pushed to the agent. Typing one group's census, current premium
  and employer share is the *demo* shape. Worse, A4a's own gate — 🟣 *"a few renewing groups"* from
  Forrest — was never asked for, so the tool has been built and never fed.
- **`/RateCacheAdmin` on the agent's hub.** PSP operator plumbing; correctly `isPspAdmin`-wrapped, but
  it is on the hub because the hub is surface-organised rather than motion-organised.

**And one tool answers a question the agent was never recorded asking.** See the close-out's Part 4.

---

## §5 — Next build: ZIP intake

> ✅ **T74 is complete for `/Illustration` as of 2026-08-01.** Part 1 shipped the data layer — V084
> (table), V085 (2,894 Texas rows), `ZipCounty`, `ZipCountyDAO`, `ZipCountyResolver`. Part 2 shipped the
> UI — `IllustrationServlet` (`5b70586`) and `illustration25.jsp` (`a41a481`). **`/GroupConversion` is
> deliberately still on the county dropdown**, per this section's own "only after the illustration path
> is proven". Runtime verification: §3 click-script steps 4a–4d.

~~⚠️ **There is no ZIP → county crosswalk in this repo, this schema, or the HealthSherpa API.**~~ There
was not; **there is now.** `county_reference` remains county → *one* representative ZIP (V076, PK
`county_fips`) and still cannot be reversed — the crosswalk is a separate table.

**The data — as built.** `zip_county` (V084): `zip CHAR(5)`, `county_fips CHAR(5)`, `land_area_ratio
DECIMAL(7,6) NULL`, composite PK `(zip, county_fips)`, **no FK** (`county_reference` is Texas-only, so
an FK would reject valid rows on the first out-of-state expansion). V085 seeds **Texas only**, per this
section's own scope line.

| Fact | Value |
|---|---|
| Source | US Census Bureau **2020 ZCTA-to-county relationship file**, fetched from `www2.census.gov` 2026-08-01. Public domain (17 U.S.C. 105). Same file V076 uses |
| Rows / ZCTAs / counties | **2,894 / 1,992 / 254** — an exact 254-of-254 set match with `county_reference`, zero either way |
| **ZIPs spanning >1 county** | **686 — 34% of Texas ZCTAs.** ⚠️ Disambiguation is the **common path**, not an edge case |
| File size | 89 KB — attachable to a GitHub release by hand |

⚠️ **The count in this section's earlier draft — "~2,600 ZCTAs" — was my estimate and was wrong. It is
1,992.**

⚠️ **ZCTA is not ZIP, and this reaches users.** ZCTAs approximate USPS ZIPs and omit those with no
residential delivery area — PO-box-only and single-building ZIPs especially. **A valid USPS ZIP an agent
types may simply not be here.** That is a *miss*, and the UI must say *"we don't have that ZIP, choose a
county"* — **never "invalid ZIP"**. HUD's crosswalk is the better source (real USPS delivery data, and a
residential *address* ratio rather than a land-area one); it was tried first on 2026-08-01 and could not
be obtained — `huduser.gov` file paths return HTTP 202 with a zero-byte body, and its API returns 401
without a registered token. Swapping to HUD later replaces V085's rows and changes nothing else.

⚠️ **`land_area_ratio` is a share of land, not of people.** It orders the candidates a crossing ZIP
offers. **It must never auto-select, and no figure shown to a user may derive from it.**

**What the agent types.** A 5-digit ZIP, replacing the county dropdown as the primary input. The
dropdown stays as a fallback and for the counties a ZIP cannot reach.

**Resolution.** ZIP → one or more `county_fips` → the rate cache, which Part 1 confirmed is keyed
`(plan_year, county_fips, age, uses_tobacco)` — **not by rating area, and there is no rating-area column
at all.** So the whole path is county-FIPS-keyed end to end.

- **One county** → resolve silently, show the resolved county name so the agent can see what it chose.
- **Crossing ZIP** → **the agent picks. It must never silently resolve.** Present the candidate counties
  and stop. This is the single rule most likely to be shortcut, and a wrong county is a wrong premium
  presented as fact.
- **Unknown ZIP** → say so and offer the county dropdown. Never guess a neighbour.

**Cache miss** (**T76**) — a resolved county with no warmed rates should **warm on demand** rather than
report "no data". Matters beyond SWBD: every TPA warms from its own book. ⚠️ Blocked in practice — no
AMS installation has ever authenticated to the HealthSherpa API (`healthsherpa.md`, 31 Jul). **Ship the
miss path as an honest message first; the warm trigger lands when a key does.**

**Multiple locations** (**T77**) — a design holds a **set** of ZIPs, not one. Dedupe **by resolved
`county_fips`** before pricing, since that is the cache key. ⚠️ Rating-area dedupe is *not* available
and must not be claimed: two counties in one rating area price identically but hold separate rows.

**Files.** ~~`IllustrationServlet` (parse + resolve), a new ICHRA-only `ZipCountyDAO`, the new entity,
`illustration25.jsp`~~ — split across two runs:

| | Status |
|---|---|
| `docs/migrations/V084__zip_county_crosswalk.sql`, `V085__zip_county_crosswalk_tx.sql` | ✅ shipped `4f242af`, `2689d67` |
| `docs/scripts/generate_zip_county.ps1` — deterministic regeneration, national expansion is `-State XX` | ✅ shipped `2689d67` |
| `model/market/ZipCounty.java`, `data/dao/ZipCountyDAO.java`, `data/resolver/ZipCountyResolver.java` | ✅ shipped `d7cbc4f` |
| `IllustrationServlet` (parse + resolve) | ✅ shipped `5b70586` |
| `illustration25.jsp` (ZIP box, chooser, no-match message) | ✅ shipped `a41a481` |
| `GroupConversionServlet` + its JSP | ⬜ **only after** the illustration path is proven |

**The contract, and how the UI honours it.** `ZipCountyResolver.resolve(em, zip)` returns a `Resolution`
with three first-class outcomes and **no fourth**:

| Outcome | What shipped |
|---|---|
| `isUnique()` | `countyFips` is assigned and the request **falls through to the ordinary county path**. Nothing downstream knows a ZIP was involved, so the two paths cannot diverge |
| `isAmbiguous()` | Candidates go to the JSP, which renders a **plain list of equal-weight links** to ordinary `?countyFips=` URLs. **Nothing pre-selected, nothing badged likely** — `getUnique()` returns null here by design, and the land-area ordering is stability only, never a recommendation |
| `isEmpty()` | A distinct `zipNoMatch` attribute — **not** `inputError`. Copy names our gap, not the agent's typo |

⚠️ ~~**`?zip=` is consulted only when `countyFips` is absent**, so the existing contract always wins.~~
**That rule shipped and was wrong — it caused R1.** A production walk on 2026-08-01 found a stale
Hopkins selection silently overriding a freshly typed `75009` (Collin/Denton), returning Hopkins rates
with no warning. **Corrected `de0efe0`.** The rule now:

| ZIP | County | Behaviour |
|---|---|---|
| blank | set | **County wins** — this is what preserves the `?countyFips=` contract |
| set, **agrees** with the selection | set | Proceed on that county |
| set, **contradicts** it, resolves to one | set | The ZIP replaces the selection |
| set, **contradicts** it, resolves to several | set | Chooser. **Nothing computed** |
| set, resolves to nothing | set | No-match. **No fallback to the stale county** |
| set | blank | As built |

**Never compute from a county the ZIP contradicts** — not with a warning, not with a note.

**Resolution without submit (R2).** `/IchraZipLookup` (`de0efe0`) is a gated GET/JSON endpoint so the
field resolves on **blur**, not only on Enter — Enter submits, so typing a ZIP with an empty headcount
used to be answered with *"Enter a valid number of eligible employees."* **Server-side precedence is
enforced independently**, because JavaScript may be off and `?zip=` can arrive in a URL.

⚠️ **Four states, kept separate. Do not collapse them:**

1. **ZIP not in the crosswalk** → *"We don't have ZIP N in our county lookup."* Coverage gap.
2. **County real but unwarmed** → *"We don't have rates for X, TX yet."* **T76**'s to fix.
3. **Not a county at all** (typo, truncated FIPS, pasted placeholder) → *"Select a valid county from the
   list."* Correct for that input, unchanged.
4. **No ZIP and no county** → the bare form, exactly as before.

### ⚠️ The coverage mismatch — the crosswalk knows 254 counties, the illustration prices four

V085 gave the crosswalk **all 254 Texas counties**. The county dropdown is built from
`rating_area_rate_cache` — **the counties that have been warmed**, four on production
(`IllustrationServlet.java:113-118` → `RateCacheDAO.getCountySummaries`, grouped by `county_fips` for
the plan year, intersected with `county_reference`). So ZIP resolution could hand an agent a real county
the tool has never been able to price, and the page answered *"Select a valid county from the list"* —
**blaming the agent for a gap that is ours.**

**ZIP intake did not create this. It exposed it.** Before ZIP, the agent picked from four counties and
never saw the boundary.

Fixed 2026-08-01 (`8285b83`, `0e2e014`): `describeUnavailableCounty` distinguishes states 2 and 3, and
the chooser labels an unpriceable candidate **before** the click — *"— no rates cached yet"*, descriptive
only, with the entry keeping its link, weight and land-area position.

⭐ **T76 attaches at `IllustrationServlet.describeUnavailableCounty`, the unwarmed branch** — the one
place that knows "a real county, no rates". No stub, button or TODO was left there: a disabled control
implying a capability that does not exist is worse than its absence.

**Left undecided, deliberately (T89):** whether the dropdown should list all 254 with most marked
unavailable. That is downstream of warm-on-miss and is a real question, not an oversight.

The resolver never throws, never reads the rate cache, and never warms anything (**T76**).

**Constraints.** Gated by `IchraAccessResolver.isAvailable`, no new ungated path · no hardcoded LOS /
ServiceItem / PlanType / ServiceModule / RateTable literal · **nothing persisted about any person** —
a ZIP typed by an agent is an input, not a record · no behaviour change for any non-ICHRA line of
service · the `source_env = STAGING` banner untouched and still above the results.

**Out of scope.** Multi-state ZIP data (Texas first, same as V076) · the entity screen (**T70**) ·
estimated-census state (**T79**) · mobile (**T78**) · anything that writes a row.
