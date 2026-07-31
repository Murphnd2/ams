# ICHRA demo-path role walk — what an external agency user actually sees

Answers one question: walking `swbd_ichra_build_plan.md` §2 steps 2–6, which surfaces render, which
render inert, and which are unreachable, **for each role shape an external agency user can hold**.
Derived from the role model in code only — no row, no login, no database was consulted. Written
2026-07-31 against `a279620`.

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

`ICHRA-ok` below = `IchraAccessResolver.isAvailable(em, request)` — PSP admin, **or** the caller's
primary agency has `agency.ichra_enabled` (`IchraAccessResolver.java:45-59`). E1–E3 pass it when their
agency is entitled.

| Step | Surface | File | Gate | E1–E3 (entitled agency) |
|---|---|---|---|---|
| 2 | ICHRA nav entry | `navbar25.jsp:209-215` | `IchraAccessResolver.isAvailableForNav` (session-cached hint) | **pass** |
| 2 | Hub page | `IchraHome.java:22-30,37` | `ICHRA-ok`, else redirect `/` | **pass** |
| 2 | Card · Rating-Area Illustration | `ichraHome25.jsp` | → `/Illustration` (`ICHRA-ok`) | **pass** |
| 2 | Card · Rate Cache Admin | `ichraHome25.jsp` | → `/RateCacheAdmin`, `isPspAdmin` only (`RateCacheAdmin.java:168-169`) | **was unreachable — fixed this run** |
| 2 | Card · Age-Band Net Cost | `ichraHome25.jsp` | → `/Illustration?mode=AGE_BAND` (`ICHRA-ok`) | **pass** |
| 2 | Card · Affordability Threshold | `ichraHome25.jsp` | → `/Illustration?mode=AGE_BAND` (`ICHRA-ok`) | **pass** |
| 2 | Card · Group-to-ICHRA Conversion | `ichraHome25.jsp` | → `/GroupConversion` (`ICHRA-ok`) | **pass** |
| 2 | Card · Design Advisor | `ichraHome25.jsp` | `chatbotEnabled && (isPspAdmin \|\| (isPspUser && chatbotAllUsers) \|\| isBpoAdmin \|\| (isBpoUser && chatbotAllBpoUsers))` | **inert** — renders `Coming`, by design (T55a) |
| 3–5 | Illustration `RANGE` / `AGE_BAND` / affordability sub-mode | `IllustrationServlet.java:79-83,413` | `ICHRA-ok` once at `doGet`, else 403; sub-modes carry no further gate | **pass** (all three) |
| 4 | `illustration25.jsp` | `illustration25.jsp` | none of its own — reached only via the servlet | **pass** |
| 5 | `GroupConversionServlet` + `groupConversion25.jsp` | `GroupConversionServlet.java:93-95,120-122,387` | `ICHRA-ok` on both `doGet` and `doPost`, else 403 | **pass** |
| 6 | "Use This in a Proposal" | `illustration25.jsp:365-377`, `:537-553` | `sourceEnv == 'PRODUCTION'` — **data provenance, not role** | **pass** (role-independent) |
| 6 | Landing: `ProposalBuilder` | `ProposalBuilder.java:42-79` | no role gate (T45); branches on `isAgent`/`isAgencyAdmin` → agency-scoped rates | **pass** |
| — | `IchraOpportunityAnalyses` + pipeline drawer | `IchraOpportunityAnalyses.java:85-100`; called from `agentHome25.jsp:898` | `ICHRA-ok`, then `OpportunityAuthz.canAccessOpportunity` per record | **pass** |

## 3. Verdict

**No — an external agency user of an `ichra_enabled` agency could not traverse steps 2 → 6 cleanly; the
first break was the hub's Rate Cache Admin card, which rendered "Live" but redirected to `/` for any
non-PSP-admin, and which this run fixed — after which the only remaining non-pass is the Design Advisor
card, which renders inert `Coming` by design.**

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
- **Q2.** The hub's *Age-Band Net Cost* and *Affordability Threshold* cards both point at
  `Illustration?mode=AGE_BAND` — same URL, two cards. Role-independent and outside this walk's scope,
  but worth confirming it is intended.
