# ICHRA demo-path role walk — what an external agency user actually sees

Answers one question: walking `swbd_ichra_build_plan.md` §2 steps 2–6, which surfaces render, which
render inert, and which are unreachable, **for each role shape an external agency user can hold**.
Derived from the role model in code only — no row, no login, no database was consulted. Written
2026-07-31 against `a279620`.

> ⚠️ **Re-verified 2026-08-01 (session 6, against `56e7f07`).** Every row in §2 now carries an explicit
> **`code-verified`** or **`runtime-verified`** marker — nothing in this file is implicitly true any
> more. That distinction exists because this document once marked the ICHRA nav row **pass** while that
> entry was failing in production for the intended audience, and session 5 was spent recovering from it.
> `code-verified` means the gate expression was read from source and the row follows from it;
> `runtime-verified` means someone observed it in a running installation. **No row in this file is
> `runtime-verified` by this session** — session 6 could not log in either. The click-script in
> `ichra_flow_and_handoffs.md` §3 is what converts them.
>
> Four rows were stale and are corrected below: the §2 gate definition (primary-agency-only → membership
> set), the Design Advisor card row (quoted a gate the JSP no longer contains), the Q2 open question
> (now closed), and §3's verdict (predated T57/T58). Flow-level findings that are *not* role questions —
> the affordability entry point, the unordered hub, the orphaned `opportunityId` — live in
> `docs/analysis/ichra_flow_and_handoffs.md`, not here.

## 1. Role shapes

`AuthDAO.assignUserRoles` (`AuthDAO.java:92-125`) maps `UserRole.id` → session attribute. The ten it
sets are `isAgent`, `isPspUser`, `isClient`, `isApplicant`, `isPspAdmin`, `isAgencyAdmin`,
`isPspSales`, `isBpo`, `isBpoAdmin`, `isBpoUser`.

| Shape | Roles held | `isPspAdmin` | `isPspUser` | `isBpoAdmin` | `isBpoUser` |
|---|---|---|---|---|---|
| **E1** Agency agent | 2 → `isAgent` (`AuthDAO.java:101,115`) | false | false | false | false |
| **E2** Agency admin | 8 → `isAgencyAdmin` (`AuthDAO.java:105,120`) | false | false | false | false |
| **E3** Agency admin + agent | 2 + 8 | false | false | false | false |

⚠️ **An external agency user is none of the four.** Roles 2 and 8 set `isAgent` / `isAgencyAdmin`,
which are disjoint from the PSP and BPO flags — there is no role id that grants an external agency user
any of `isPspAdmin`, `isPspUser`, `isBpoAdmin`, `isBpoUser`. This is not softenable: **no external
agency role can satisfy the chatbot gate**, under any value of `chatbotAllUsers` /
`chatbotAllBpoUsers`, because those two only widen `isPspUser` / `isBpoUser`, which an agency user
never holds.

E1/E2/E3 are identical for every gate in this walk, so the walk collapses to one column.

## 2. The walk

`ICHRA-ok` below = `IchraAccessResolver.isAvailable(em, request)` — PSP admin, **or** *any* agency in the
caller's scope has `agency.ichra_enabled`: the primary agency is a fast path, and on a miss the full
membership set `AgencyScope.detailAgencyIds()` is checked with one query (`IchraAccessResolver.java:59-91`).
E1–E3 pass it when any agency they belong to is entitled.

> **Corrected 2026-08-01.** This line previously read "the caller's **primary agency** has
> `agency.ichra_enabled` (`IchraAccessResolver.java:45-59`)" — true when written, false since session 5's
> `a5c0d8d` widened it to the membership set, and the line numbers had moved too. ⚠️ Note that the
> membership widening **is not established as the cure** for the 2026-08-01 entitlement failure; the
> production log showed a single-agency agent whose fallback never fired, and a stale EclipseLink
> `Agency` after a raw-SQL flag flip remains the untested leading alternative (**T64**).

| Step | Surface | File | Gate | E1–E3 (entitled agency) | Verified |
|---|---|---|---|---|---|
| 2 | ICHRA nav entry | `navbar25.jsp:209-215` | `IchraAccessResolver.isAvailableForNav` (session-cached hint) | **pass** — ⚠️ the row that was wrong once. A session that predates the entitlement flip caches `false` and keeps it for the session's life | `code-verified` |
| 2 | Hub page | `IchraHome.java:18,23-26,37` | `ICHRA-ok`, else redirect `/` | **pass** | `code-verified` |
| 2 | Card · Rating-Area Illustration | `ichraHome25.jsp:94` | → `/Illustration` (`ICHRA-ok`) | **pass** | `code-verified` |
| 2 | Card · Age-Band Net Cost | `ichraHome25.jsp:102` | → `/Illustration?mode=AGE_BAND` (`ICHRA-ok`) | **pass** | `code-verified` |
| 2 | Card · Affordability Threshold | `ichraHome25.jsp:121` | → `/Illustration?mode=AGE_BAND&affordabilityBasis=FPL` (`ICHRA-ok`) | **pass** — href corrected in session 6 (**T59**); it was byte-identical to the row above | `code-verified` |
| 2 | Card · Group-to-ICHRA Conversion | `ichraHome25.jsp:132` | → `/GroupConversion` (`ICHRA-ok`) | **pass** | `code-verified` |
| 2 | Card · Design Advisor | `ichraHome25.jsp:145-149` | `${applicationScope.global.chatbotEnabled}` — **the role disjunction is gone**; on this page `ichraAvailable` is constant-true because `IchraHome.doGet` refuses to forward otherwise, so the navbar gate reduces to `chatbotEnabled` here | **pass** | `code-verified` |
| 2 | Card · Rate Cache Admin | `ichraHome25.jsp:173-174` | → `/RateCacheAdmin`, `isPspAdmin` only (`RateCacheAdmin.java:167-170`) | **not rendered** for E1–E3 (`c:if` wrapper, 2026-07-31); moved last and labelled `Admin` in session 6 | `code-verified` |
| 3–5 | Illustration `RANGE` / `AGE_BAND` / affordability sub-mode | `IllustrationServlet.java:62,80-83,409-417` | `ICHRA-ok` once at `doGet`, else 403; sub-modes carry no further gate | **pass** (all three) | `code-verified` |
| 4 | `illustration25.jsp` | `illustration25.jsp` | none of its own — reached only via the servlet | **pass** | `code-verified` |
| 5 | `GroupConversionServlet` + `groupConversion25.jsp` | `GroupConversionServlet.java:76,94-97,121-124` | `ICHRA-ok` on both `doGet` and `doPost`, else 403 | **pass** | `code-verified` |
| 5 | Affordability output itself | `IllustrationServlet.java:366-386` | **not a role gate** — needs `constant` rows `ICHRA_AFFORDABILITY_PCT_<year>` and (FPL basis) `FPL_ANNUAL_<year>`, which **nothing in the repo seeds** | **pass on role, blocked on config** — renders "Affordability is not configured for plan year N" if absent | `code-verified`; presence on production is **unverified** |
| 6 | "Use This in a Proposal" | `illustration25.jsp:376-388`, `:548-566` | `sourceEnv == 'PRODUCTION'` — **data provenance, not role** | **pass** (role-independent) | `code-verified` |
| 6 | Landing: `ProposalBuilder` | `ProposalBuilder.java:42-79`, hidden-field echo `proposalBuilder.jsp:58-77`, snapshot attach `ProposalBuilder.java:381-388` | no role gate (T45); branches on `isAgent`/`isAgencyAdmin` → agency-scoped rates | **pass** — the hand-off survives GET → POST intact. ⚠️ It carries **no prospect id**, deliberately (`illustration25.jsp:344-352`), which contradicts build-plan §1 step 6's "prospect pre-filled" | `code-verified` |
| — | `IchraOpportunityAnalyses` + pipeline drawer | `IchraOpportunityAnalyses.java:50,85-103`; called from `agentHome25.jsp:898` | `ICHRA-ok`, then `OpportunityAuthz.canAccessOpportunity` per record | **pass, but unreachable in practice** — `opportunityId` has no producer anywhere, so no ICHRA run is ever attributed and the drawer section renders empty | `code-verified` |

## 3. Verdict

> **Superseded 2026-08-01.** The verdict below was written before T57/T58 and is kept only to show what
> was true on 2026-07-31.

~~**No — an external agency user of an `ichra_enabled` agency could not traverse steps 2 → 6 cleanly; the
first break was the hub's Rate Cache Admin card, which rendered "Live" but redirected to `/` for any
non-PSP-admin, and which this run fixed — after which the only remaining non-pass is the Design Advisor
card, which renders inert `Coming` by design.**~~

**Current verdict (2026-08-01, `code-verified`): every gate on steps 2 → 6 now passes for E1–E3.** Both
2026-07-31 breaks are closed — the Rate Cache Admin card is `isPspAdmin`-wrapped, and the Design Advisor
is reachable via T58 + `V082`.

**No role gate remains. What remains is not about roles**, and is therefore tracked in
`docs/analysis/ichra_flow_and_handoffs.md` rather than here:

- affordability needs two `constant` rows that **nothing in the repo seeds** — the likeliest break in
  the whole demo, and invisible to any role analysis;
- `opportunityId` has no producer, so the pipeline tie-in never engages;
- the design advisor receives no page context;
- build-plan §1 step 6 promises a pre-filled prospect that the hand-off deliberately does not carry.

## 4. Punch list

| # | Break | Smallest fix | Owner | Fixed this run |
|---|---|---|---|---|
| 1 | Hub Rate Cache Admin card rendered for every role; servlet is `isPspAdmin`-only → dead click | Wrap the card in `<c:if test="${sessionScope.isPspAdmin}">` | **ICHRA-owned** (`ichraHome25.jsp`) | ✅ yes |
| 2 | Design Advisor card inert for every external agency user — the demo's advisor is not reachable by Forrest | Widening requires editing the `navbar25.jsp:408` chatbot gate (or `AmsDataGlobal`) to admit `isAgent`/`isAgencyAdmin`, **plus** V080's `is_admin_only=1` (T57). Two changes, one shared + one schema | **shared + schema** | ❌ no — out of scope by §4 row 2 |
| 3 | Even if #2 were widened, `ChatAssistant.doPost` strips admin-only skills for non-admin callers, so an agency user would reach a chat assistant with no ICHRA skill loaded | Already logged | shared + schema | ❌ no — **T57** |

Punch-list item 1 was safe because it *narrows* a card to match a gate that already exists in the
servlet it points at. **No gate was widened anywhere in this run.**

**Items 2 and 3 were fixed 2026-07-31** — T58 (`navbar25.jsp` gate gained `|| ichraAvailable`) and T57
(`V082`, `is_admin_only` 1 → 0). Both shipped together; neither works alone.

### Post-fix verification — does the advisor still cite anything for a role-2 caller?

Checked 2026-07-31 because V082 deliberately left the `ichra_design` knowledge base `ADMIN_ONLY`, and
item 12 promises answers *with citations*. **Verified: yes, and retrieval is genuinely not on the path.**

| Question | Answer | Evidence |
|---|---|---|
| What does a matched skill send to the model? | The skill's `system_prompt` and the raw question — nothing else | `ChatAssistant.java:194` — `ClaudeApiService.ask(null, skill.getSystemPrompt(), question)` |
| Are KB chunks injected on that path? | No. The two paths are mutually exclusive `if`/`else` | `ChatAssistant.java:139-147` |
| Is KB eligibility filtered by the caller or the skill? | The **caller** — `getEligibleKBs(local.isPspAdmin())` | `ChatAssistant.java:207-208`; `KnowledgeSearchService.java:331-339` |
| Where do citations come from? | **Nothing in code produces them structurally on the skill path.** They are model output, driven by boundary 6 ("CITE EVERY SUBSTANTIVE ANSWER") plus **14 literal `Source:` lines inline in the `system_prompt`** | `V080` lines 105-251 |
| Does the canonical demo question match? | Yes — "Does my client's dental plan kill the QSEHRA?" hits `qsehra` + `dental` = 2, and the threshold is `>= 2` | `ChatbotSkillDAO.java:91` |

**Conclusion: no fix needed.** The citation strings travel on the skill row, not in the KB, so an
`ADMIN_ONLY` KB costs a non-admin caller nothing on the matched path. A question scoring 0–1 keywords
still falls through to KB search and retrieves **no** ICHRA content for a non-admin — the fail-safe
direction, and the reason the KB was correctly left alone.

## 5. Open questions

- **Q1 — ANSWERED YES, 2026-07-31.** Should an external agency user see the Design Advisor at all?
  Item 12 specifies an agent-visible outcome, so an advisor only SSA staff can reach is not the
  feature that was specified. Implemented as T58 + T57/`V082`. The disclosure concern that made this a
  question — the content is SSA's internal rule set, including "SSA has not finalized" language — is
  handled by the skill's own boundaries rather than by hiding it.
- **Q2 — ANSWERED NO, 2026-08-01 (closed).** The hub's *Age-Band Net Cost* and *Affordability Threshold*
  cards both pointed at `Illustration?mode=AGE_BAND` — same URL, two cards. Not intended: it left the
  affordability view, which is build-plan §1 **step 5**, with no entry point at all. Filed as **T59**,
  fixed in session 6 (`e849dac`) by pointing the second card at
  `Illustration?mode=AGE_BAND&affordabilityBasis=FPL`. T59 had assumed there was no URL to point it at;
  in fact `affordabilityBasis` has been a request parameter since item 9 and the dropdown already
  pre-selects from it — the URL existed and nothing linked to it.
- **Q3 — new, 2026-08-01, open.** Is affordability configured on production? It needs `constant` rows
  `ICHRA_AFFORDABILITY_PCT_<year>` and `FPL_ANNUAL_<year>`, and **no migration, seed script or
  `DatabaseInitializer` path creates either.** Not a role question, which is exactly why this document
  would never have caught it. Settled by step 10 of the click-script in `ichra_flow_and_handoffs.md` §3.
