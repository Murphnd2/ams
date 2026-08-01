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
| **G4** | **`opportunityId` has zero producers.** Both `/Illustration` and `/GroupConversion` accept and scope-check it (`IllustrationServlet.java:436-450`), and `illustration25.jsp:95-97` carries it across re-submits — but **no page in the codebase ever emits an ICHRA URL containing it.** The drawer reads analyses and renders nothing when empty, so the loop has no start. Item 13's attribution is reachable only by hand-typing a URL | pipeline tie-in (not a numbered step) | `agentHome25.jsp` — a "Run an ICHRA analysis" link in the drawer | ❌ non-ICHRA JSP, forbidden | S |
| **G5** | **Lives do not carry across the mode toggle.** County and plan year carry (`illustration25.jsp:64-67`); headcount does not, in either direction | **3 → 4** | `illustration25.jsp:64-67` | ✅ partly | XS — **AGE_BAND → RANGE built; RANGE → AGE_BAND deliberately not, see below** |
| **G6** | **Design advisor is always cold.** The widget posts `JSON.stringify({question})` and nothing else (`chatAssistant25.jsp:170-174`) — no URL, county, plan year, contribution or opportunity. An agent asking "is this affordable?" while looking at a result gets an answer that cannot see the result | 5 (adjacent) | `chatAssistant25.jsp` + `ChatAssistant.java` | ❌ shared, non-ICHRA | M |
| **G7** | **Census does not travel between the two tools.** An agent who typed a census into `/GroupConversion` retypes it on `/Illustration`, and vice versa | 4 ↔ 5 | `GroupConversionServlet` (`doGet` echo) + a link | ❌ **do not build** — the servlet is POST-only *because* "an employer's current premium and payroll deductions have no business in a URL" (`:71-74`), and its JSP forbids adding outbound affordances (D24). Needs Kevin's compliance call, not a patch | M |
| **G8** | **Yardstick says "prospect pre-filled"; code deliberately does not.** §1 step 6 promises ProposalBuilder with the prospect pre-filled. Both hand-off URLs carry "never a prospect id … ProposalBuilder prompts for the prospect as it always does" (`illustration25.jsp:344-352,530-536`) | **6** | `ProposalBuilder` + a prospect picker on the illustration | ❌ forbidden servlet, and it is a recorded decision, not an oversight | M |
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
