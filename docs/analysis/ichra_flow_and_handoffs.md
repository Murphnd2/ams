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
| 4 | Click card **1 · Rating-Area Illustration** | county dropdown | empty, or "Rate cache is not configured" — `RATE_CACHE_PLAN_YEARS` or the warm job |
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

### §4.1 — The motion, in order

| # | Stage | Who acts | Surface | Produces → consumed by |
|---|---|---|---|---|
| 1 | **Entity / eligibility screen** — sole props, partners, >2% S-corp shareholders cannot participate; any other group health plan disqualifies a QSEHRA | Agent | ⛔ **none** | eligibility result, which *"filters the LOS menu"* (`plus_tier.md` §Quote) → stage 2 |
| 2 | **Subsidy segmentation** — split the census into PTC-eligible (→ QSEHRA-lite) and not (→ ICHRA). *"Answers which product to sell before anyone commits"* | Agent | ⛔ **none** | the product decision → stage 3 |
| 3 | **Market illustration** — county → premium range | Agent | ✅ `/Illustration` | range → stage 4 |
| 4 | **Design** — age-band net cost, affordability threshold per employee, **class optimization** | Agent | ✅ AGE_BAND + affordability · ⛔ **no class optimization** | a priced design **and a design fee** (A3) → stage 6 |
| 5 | **"Will I lose my doctor?"** — provider check (A2) | Agent or employee | ⛔ **none** — O23 **resolved favorably** (Part 8, 30 Jul), so this is unblocked, not gated | the objection that kills cases, answered |
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

**Re-ranked by sale-motion impact — the order changed completely.** §2 ranked by demo step, which put
config and polish on top. By motion:

1. **N1 + N2 — the intake front end.** Stage 1 and 2 of an eight-stage motion, and *every* built tool
   assumes their output already exists. An agent cannot know whether they are running an ICHRA or a
   QSEHRA case, and entity type can void the design before any number matters.
2. **N5 — the provider check.** Unblocked by Part 8 on 30 Jul and still unbuilt. The capability map
   calls it the objection that kills cases; the plan calls it *"the highest-emotion demo."*
3. **G4 — illustration → opportunity attribution.** Promoted out of "pipeline tie-in, not a numbered
   step": it is the input A5 was designed to read, and A5 is the pitch line *"here's your whole
   network's pipeline."*

**G2 drops off this ranking without becoming less urgent** — it blocks the *demo*, not the motion, and
it is an operational task, not a build.

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
