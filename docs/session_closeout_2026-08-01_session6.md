# Session close-out — 2026-08-01, session 6

**Branch:** `refactor/modernize-architecture` · **Run:** ICHRA tool flow — map it, then close the
hand-off gaps · **Yardstick:** `docs/swbd_ichra_build_plan.md` §1's eight-step walkthrough, for a role-2
agent.

---

## Shipped

| Hash | What |
|---|---|
| `a79de12` | **Part 0 — tracker.** Not what the prompt expected: see "Contradictions found". |
| `e849dac` | **G1 / T59** — affordability view given its own entry point. Hub's second AGE_BAND card now points at `Illustration?mode=AGE_BAND&affordabilityBasis=FPL`. One attribute. |
| `724cc3e` | **G3** — ICHRA hub ordered and numbered to the §1 walkthrough. Group conversion labelled "Separate analysis"; Rate Cache Admin moved last, labelled `Admin`. |
| `56e7f07` | **G5** — total lives carries AGE_BAND → RANGE. Reverse direction deliberately not built. |
| `3291469` | **Part 1 docs** — new `docs/analysis/ichra_flow_and_handoffs.md`; `ichra_demo_path_role_walk.md` corrected and every row marked verified-how; T59 closed and T65–T69 filed; build plan records the three shipped fixes and two corrections to its own walkthrough. |

`./mvnw compile` clean before each of the three code commits. Pushed at `3291469`.

## In flight

Nothing. No feature branch, no uncommitted work, no partial edit.

## Decisions made

1. **FPL, not INCOME, for the affordability card's default basis.** The entered-income basis requires an
   income on *every* census row and rejects the submit otherwise (`IllustrationServlet.java:261-268`).
   A card named "Affordability Threshold" that errors on first click is worse than one that lands on
   "None". The agent can still switch on the page.
2. **G5 built in one direction only.** AGE_BAND → RANGE is exact (total lives *is* the RANGE headcount).
   RANGE → AGE_BAND was rejected: seeding `count1` with a flat total is correct only for a single-age
   group, wrong for the Sandoval demo case (3 lives, 3 ages), and wrong *silently* — worse than blank.
3. **Group conversion labelled, not numbered.** Calling it step 4 would imply a continuity the code does
   not provide: no census carries into it from steps 1–3.
4. **G7 (census sharing) specced, not built,** although it was technically inside the fence. It collides
   with two stated decisions — `GroupConversionServlet` is POST-only *because* premiums and deductions
   "have no business in a URL" (`:71-74`), and its JSP forbids adding outbound affordances (D24). That is
   Kevin's compliance call to make explicitly, not a patch to slip past a "for convenience" prohibition.

## New assumptions, and what reversing them costs

| Assumption | Reversal cost |
|---|---|
| FPL is the right default basis for the affordability card | One attribute in `ichraHome25.jsp`. Free. |
| An agent switching AGE_BAND → RANGE wants the same total lives | Delete the added EL fragment. Free. But note the **side effect**: with county *and* headcount present the Range view now computes on arrival rather than showing an empty form, and writes the usual `illustration_log` row. That is the pre-existing behaviour of any `/Illustration` GET carrying both, not new code — but it is a visible change. |
| The hub's step numbers match how an agent actually works | Delete the `hub-step` spans and the lead line. Free. Nothing reads them; no gate or href depends on them. |

## Open questions raised

- **T65 (HIGH) — is affordability configured on production?** `ICHRA_AFFORDABILITY_PCT_<year>` and
  `FPL_ANNUAL_<year>` are read by exactly one file and created by none. Every code path for demo step 5
  is built and every one fails closed to "not configured" without those two rows. Click-script step 10
  settles it. **This is the single most likely thing to break the demo, and it is config, not code.**
- **T66** — should the opportunity drawer be able to *launch* an ICHRA analysis? Today `opportunityId`
  is accepted, scope-checked, persisted and read back, with no producer anywhere.
- **T68** — may ages and counts travel in a URL between the two tools, given the POST-only decision?
- **T69** — fix the build plan's "prospect pre-filled" sentence, or build the pre-fill?

## Contradictions found

1. **Part 0's premise was already false.** The prompt said V079/V081/V083 showed `⬜` for Production and
   that session 5 "could not fix it inside its own scope fence". Session 5's *final* commit `941dc14`
   flipped all three, with the deployment-record evidence written beside them. Nothing in the table
   needed changing. What was still wrong was one sentence above it — "V079 is **not yet applied to any
   environment** — awaiting release `v0.79.00`" — which survived the reconciliation and contradicted both
   the table and the note below it. Struck, not deleted: that sentence is the mechanism of the drift the
   file keeps documenting. It was true when authored and never revisited after the deploy.
2. **Three of the prompt's seven hypotheses were wrong.** `/GroupConversion`'s missing proposal hand-off
   is D24 stated twice in source, not an oversight, and building it would have breached a written rule.
   T59's premise that no distinct URL existed was half wrong. `/IchraOpportunityAnalyses` is not a
   surface — its own Javadoc says "not a page and not linked from anywhere."
3. **The build plan's own walkthrough is wrong twice.** Step 5 promises a contribution *slider*; there is
   a number input and a full form re-submit. Step 6 promises a *pre-filled prospect*; the hand-off
   deliberately carries none and says so in source.
4. **`ichra_demo_path_role_walk.md` had drifted again** — its gate definition still described
   primary-agency-only resolution, superseded by session 5's `a5c0d8d`; its Design Advisor row quoted a
   gate expression the JSP no longer contains; its verdict predated T57/T58.

## SQL close-out audit

**No `.sql` file was created, modified or deleted. No schema changed. No migration was authored.** The
run was forbidden from producing SQL and did not. `ls docs/migrations/` is unchanged at **V083**, and
`git show --stat` for all five commits lists only `.md` and `.jsp` paths — no `.java`, no `.sql`.

⚠️ **One finding is SQL-shaped and was deliberately not acted on.** T65's two missing `constant` rows
could have been "fixed" with a migration in about four lines. That would have been a scope-fence breach,
and it would also have been wrong on the merits: applicable-percentage and FPL figures are per-plan-year
operator configuration (the same class as `RATE_CACHE_PLAN_YEARS`, D-84), not schema. It is filed as a
HIGH backlog item for Kevin, which is where it belongs.

## Next

1. **Run the 12-step click-script** in `ichra_flow_and_handoffs.md` §3 on production as a real role-2
   agent. Steps 9, 10 and 12 verify this session's work; **step 10 is the one that matters** — it settles
   T65 before the demo rather than during it.
2. **T65** — add the affordability constants if step 10 shows them missing.
3. **Kevin's remaining config, unchanged from session 5:** ICHRA reference rows (priced `ServiceModule`
   → `RateTable` + `agencyrates` assignment) and the item-10 checklist via Sequence Builder.
4. **T64** — the stale-EclipseLink-cache hypothesis is still untested, and the session-5 membership fix
   is still not established as the cure for the 2026-08-01 entitlement failure.
5. **Write-up debt** — the agency epic, all four 2026-07-31 sessions, and sessions 5 and 6 are still
   missing from `session_history_archive.md`.

**Carried forward, unclosed by this run — both standing items:**

- ⚠️ **Nobody has asked Forrest what he would want a quoting tool to do.** The entire agent-utility
  thesis still rests on one sentence in one call. This session mapped the flow *we* built against the
  demo *we* wrote; neither is evidence about what he wants.
- ⚠️ **The two SWBD emails have still never been sent** — O22 book profile, and "send me three groups
  renewing next quarter".

---

*This close-out is the last commit of session 6, by construction — the fix session 5 proposed after four
consecutive sessions whose close-out could not name its own successors.*

⚠️ **It was not.** Prompt B followed in the same session. The section below is appended; nothing above
it was rewritten. **That makes five consecutive sessions where the recorded final hash was not the real
one** — and this time the cause was not forgetting, it was that a close-out cannot know whether more
work is coming. That is a convention problem for `CLAUDE.md`, not a defect to fix here: either the
close-out stops claiming finality, or it stops being written until the session is declared over.

---

# Session 6, prompt B — make demo step 5 real

**Run:** the affordability constants and the contribution slider — the two unbuilt gaps from prompt A
that sit on the same demo beat, `swbd_ichra_build_plan.md` §1 step 5.

## Shipped

| Hash | What |
|---|---|
| `e90515a` | **T65 / G2, code side.** `DatabaseInitializer.addIchraAffordabilityConstants` seeds `ICHRA_AFFORDABILITY_PCT_2026 = 0.0996` and `FPL_ANNUAL_2026 = 15960`, idempotently. New **LA-14** in the assumptions register. |
| `53a8131` | **G9.** The contribution slider, with the live per-employee flip point. Also: the two "not configured" messages now name the missing constant. |
| `ab90d49` | Flow map G2/G9 rows, click-script step 10b, backlog T65 → partly done, build plan records both. |

`./mvnw compile` clean before each commit.

## Decisions made

1. **`0.0996`, not `9.96`.** AMS has no other percentage-valued constant, so there was no local
   convention to copy — I said so rather than inventing one. The authority is
   `AffordabilityCalculator.flipContribution`, which multiplies the value straight into monthly income
   and documents its parameter as "a decimal (e.g. 0.0883 for 8.83%)". **`9.96` would not throw.** It
   would drive every flip contribution negative, clamp to zero, and report every offer affordable at any
   contribution — LA-12's dangerous direction, silently.
2. **The 2026 FPL table ($15,960), not the 2025 one ($15,650).** The only reader is the `"FPL"` branch
   feeding the employer-side safe harbor, so the safe-harbor figure is the right one. The $15,650 figure
   belongs to the PTC computation under 26 CFR §1.36B-1(h), and **AMS computes no PTC dollar figure
   anywhere**. Recorded as LA-14 because the ambiguity is in the constant's *name*, not in the sources.
3. **The slider does not recompute the flip point.** flip = onexLCSP − pct × (income ÷ 12) does not
   depend on the contribution, so the server's figure is already final. This was the single most
   consequential design call in the run: it keeps the regulated computation in `AffordabilityCalculator`
   with no JavaScript twin to drift from it, and it means **the two constants are never needed
   client-side.**
4. **The proposal hand-off href follows the slider.** Not asked for, and necessary: without it, dragging
   to $350 and clicking "Use This in a Proposal" would have snapshotted the submitted $400 silently. The
   slider would have introduced a correctness trap that did not previously exist.
5. **The verdict strings are byte-identical to the server's.** The slider flips between two
   already-approved phrasings and introduces no new language about any employee (boundary 1). Rewriting
   shipped compliance-reviewed wording was not this run's call.
6. **The plan-year-in-the-name design was recorded, not refactored** — as instructed. A 2027
   illustration will silently find nothing and report itself unconfigured, which is the fail-closed
   direction.

## New assumptions, and what reversing them costs

| Assumption | Reversal cost |
|---|---|
| **LA-14** — `FPL_ANNUAL_2026` holds the employer safe-harbor figure | ⭐ **Trivial.** A one-row `UPDATE`, or two lines in `DatabaseInitializer`. **Nothing derived from it is persisted** — no affordability figure reaches `illustration_log` or `proposal_ichra_snapshot`, which is structurally incapable of carrying one. There is no back-catalogue to restate. |
| 9.96% is the correct 2026 applicable percentage | Same — one row. Cited to Rev. Proc. 2025-25; this is among the better-sourced entries in the register. |
| A slider max of ~110% of the highest premium on the page gives enough range | Delete one line. Cosmetic. |
| Step size of $5 is fine for the demo's $350-vs-$450 story | One attribute. |

## Open questions

- **Are the two rows on production?** Still unanswered, and the seed shipped this run **does not settle
  it** — `addPspConstants` runs from `initializeDataBase` only, the one-time key-gated fresh-install
  path. Click-script step 10 remains the only thing that answers it.
- **Plan year 2027** needs a new constant pair *and* a new reader. Recorded in LA-14's confirm-before,
  not scheduled.
- **Does the slider behave on a real page?** Verified by compile and by JSTL tag-balance check only.
  `mvnw compile` does not compile JSPs, and this session could not log in — so the slider is
  `code-verified`, never `runtime-verified`. Click-script step 10b is what settles it.

## Contradictions found

1. **The prompt said "the initial computation stays a POST" and attributed a POST-only decision to this
   servlet. That is wrong, and I did not follow it.** `IllustrationServlet` is **GET-only by design** —
   *"an illustration is a query, not a state change, and GET makes results linkable"*
   (`IllustrationServlet.java:57-60`), and its form is `method="get"`. The POST-only decision belongs to
   `GroupConversionServlet`, which is a different servlet with a different reason (*"an employer's
   current premium and payroll deductions have no business in a URL"*). Converting `/Illustration` to
   POST would have destroyed the linkability that the mode toggle, the hub cards and prompt A's own T59
   fix all depend on. The slider is client-side, so the distinction cost nothing — but the instruction
   was followed in spirit (no request per tick) and not in letter.
2. **The build plan's step-5 "slider" correction from prompt A is withdrawn.** Prompt A recorded it as a
   document error. It was not: the document described the intended feature and the code was behind it.
   Corrected in place.
3. **Prompt A's own G2 row said "operator config only".** Prompt B's fence directed a `DatabaseInitializer`
   seed, which is neither a contradiction nor a full fix — the seed is correct and does not reach an
   existing installation. Both statements are now in the G2 row.

## SQL close-out audit

**No `.sql` file was created, modified or deleted. No schema changed. No migration was authored.** In
particular **no `INSERT INTO constant` exists in any migration** — the two constants went through
`DatabaseInitializer`, which is the prescribed route. `ls docs/migrations/` is unchanged at **V083**.
`git show --stat` for the three commits lists one `.java`, one `.jsp` and four `.md` paths.

## Next

1. **Click-script steps 10 and 10b**, on production, as a role-2 agent. Step 10 settles T65; step 10b is
   the first runtime verification the slider has ever had.
2. **Add the two `constant` rows to production by hand** if step 10 shows them missing —
   `ICHRA_AFFORDABILITY_PCT_2026` = `0.0996`, `FPL_ANNUAL_2026` = `15960`. Not a migration.
3. **T69** — decide whether to fix the build plan's "prospect pre-filled" sentence or build the pre-fill.
4. Unchanged from prompt A: **T66** (`opportunityId` has no producer), **T64** (stale-cache hypothesis
   untested), the ICHRA reference rows and item-10 checklist, and the write-up debt.

**Carried forward, still unclosed:**

- ⚠️ **Nobody has asked Forrest what he would want a quoting tool to do.** This run built the interaction
  §1 step 5 describes — a document SSA wrote about a demo SSA designed. Still not evidence about him.
- ⚠️ **The two SWBD emails have still never been sent** — O22 book profile, and *"send me three groups
  renewing next quarter"*.

---

# Session 6, prompt C — the sale motion

**Run:** read `docs/business/` and `plus_tier_build_plan.md` against the flow map. *"It's how it fits
into the sale process that is of question."* No code, no schema.

## Contradictions found

**Leading with the one the prompt asked for: a document answers something the build plan carries as
open — and it has been answerable since 30 July.** `swbd_ichra_build_plan.md` §5 and Part 3 §A2 both
carry the provider check ("will I lose my doctor?") as gated on **O23**, with the note *"if O23 fails,
this phase does not happen."* **Part 8 of `plus_tier_build_plan.md`, dated 30 July, resolves O23
favorably** — `healthsherpa.md`'s 2026-07-30 section confirms the quote request accepts a `providers`
array of NPIs and each returned plan carries `covered` plus `covered_addresses`. A2 is unblocked, has
been for two days, and the plan calls it *"the highest-emotion demo in the plan"* and the objection that
kills cases. Filed **T73**. Flagged, not resolved — `swbd_ichra_build_plan.md` is outside this fence.

**1. Document vs observed code — code wins.**

| Document | Claim | Code |
|---|---|---|
| `plus_tier.md` §"Requirements by stage → Quote" | quote inputs include **entity type** and **group-plan status**; first output is an **eligibility result** that *"filters the LOS menu"* | `IllustrationServlet.java:122-283` collects county, plan year, ages, counts, contribution, basis. Neither input exists; no eligibility output exists anywhere (**T70**) |
| `plus_tier_build_plan.md` Part 3 §A1; **D6** | *"ZIP → county + FIPS"*; ZIP a mandatory quote input | county dropdown only. No ZIP field in `illustration25.jsp` or `groupConversion25.jsp`; the sole `zip` reference is `IllustrationServlet.java:651`, derived *after* county selection (**T74**) |
| `ichra_platform_capability_map.md` Layer 1 | four sales-and-modeling tools | three exist. **Class optimization** has no surface — no match anywhere under `controller/market` (**T75**) |
| `ichra_administration_scope.md` (28 Jul) | *"intake includes a routing step"* — subsidy segmentation decides which product to sell | no surface; the tools begin after that decision (**T71**) |

**2. Document vs `ichra_strategy.md` / `swbd_ichra_build_plan.md` — flagged, not resolved (outside fence).**
The A2/O23 item above. Also: `swbd_ichra_build_plan.md` §1 step 6's *"prospect pre-filled"* against
A1's *"No prospect PII, no employer record required"*, **D7** and **D21** — already **T69**, now backed
by a decision rather than a source comment.

**3. Document vs document — later governs, both named.** `healthsherpa.md`'s 2026-07-28 warning
(*"the shopping UI is a price list with phone calls behind it"*) is disclaimed by **its own 2026-07-30
section** as describing HSOne, not the ICHRA Partner API. Later date governs; that is also what
unblocks A2. Separately, `plus_tier.md` **D13** claims card MCCs 6300 **and** 5960 while
`swbd_premiumpath.md` records only 6300 loaded — D13 flags the risk itself; **excluded from this run's
scope** as money mechanics.

**4. Overridden in this prompt.** Nothing substantive. Its instruction *"do not assume nine open gaps —
count what is actually there"* was correct: **five** are open (G2, G4, G6, G7, G8); G1, G3, G9 are
built and G5 is built one way with the other a recorded decision. Its LA-15 numbering was also right,
but **no LA-15 was written** — the one compliance-relevant tension found (**O25**, carrier names on
displays) is already **LA-04**, which resolves it in the conservative direction the code already takes.

## Part 4 — has Forrest actually said any of this?

**Partly, and not about this.** The doc set is **not** one sentence restated at length — there is a real
record of things Forrest said, mostly from the **2026-07-28** call: that zizzl gated his carriers
(turned off Christus, refused to quote his own), that it charged a **~$660/mo minimum** on a 3-life
group, the unbundle logic (*"if he's doing the carrier legwork anyway, better to administer
elsewhere"*), and that **he** raised HealthSherpa and is *"focused on integration."* The build plan is
careful about this, marking the small-case economics as *"**his own framing**, not one you are
inventing."* Earlier calls (7/13, 7/14) record his ask for fees, pricing and a revenue share.

**But every one of those is about administration economics and integration. Not one is about what a
quoting tool should do.** No document records Forrest describing a quoting workflow, naming a step he
performs, or saying what he would want on screen. The whole of Track A — six phases, and every ICHRA
surface built in sessions 1–6 — is inference from *"he was quoting through zizzl and hated the gating
and the price."*

**And the two artifacts that would settle it were both requested and never chased.** `swbd_premiumpath.md`
carries *"map the end-to-end workflow (**Forrest to provide a flowchart**)"* as an open item. **O22**
(book profile, incl. producing-agent count) and **O24** (will he share his book) are both marked
*"Settled by: Forrest"*, and O24 is described as *"**the partnership test as much as a technical
gate.**"* None has been sent. So: **more than one sentence, but nothing about the thing that was
built** — and the flowchart he offered to draw would have been the sale motion, from him, for free.

## What §4 concluded

**§2 had 5 open rows** → **3 Answered** (G4, G7, G8), **1 Confirmed-but-downgraded** (G6), **1
Untouched** (G2), **0 Moot**. Two of the three answers say the fix prompt A proposed was the wrong one.

**Re-ranked by sale-motion impact, and the order changed completely** — §2 ranked by demo step, which
put config and polish on top:

1. **T70 + T71 — the intake front end** (stages 1–2 of eight). Every built tool assumes their output.
2. **T73 — the provider check.** Unblocked 30 July, unbuilt.
3. **T66 / G4 — illustration → opportunity attribution.** It is A5's designed input, not a loose end.

**G2 drops off the ranking without becoming less urgent** — it blocks the *demo*, not the motion.

**The sharpest finding:** the built set covers **the middle of one stage of an eight-stage motion**, and
two tools sit on steps the agent does not perform — `/GroupConversion` as a hand-keyed form (A4a's
decided input is a scanned book, **D23**), and `/RateCacheAdmin` on the agent's hub. Worse for the
first: A4a's own gate was *"a few renewing groups"* from Forrest, so **the tool was built and never
fed.**

## SQL close-out audit

**This run was forbidden from producing SQL and produced none.** No `.sql` file created, modified or
deleted; no schema change; no migration. **No `.java` or `.jsp` was touched either** — this run shipped
no code. `git show --stat e95c4b3` lists two `.md` paths. Migrations unchanged at **V083**.

## Next

1. **Send the two SWBD emails.** They are now the top of this list rather than a footnote: O22 gates
   which markets to warm and whether A4 is worth building; the three-renewing-groups ask is the input
   `/GroupConversion` has been waiting for since it shipped. **Add a third:** ask Forrest for the
   flowchart he already offered.
2. **Click-script steps 10 / 10b** (from prompt B) — still the only runtime verification.
3. **T70/T71** — the intake front end, if the next build prompt follows §4.2's ranking.
4. **T73** — A2, blocked in practice on the same thing everything HealthSherpa-facing is: **no AMS
   installation has ever authenticated to that API** (`healthsherpa.md`, 31 Jul; `HEALTHSHERPA_API_KEY`
   absent from both `constant` and `ssa.properties` on local dev, D-78/D-79 unapplied everywhere).
   ⚠️ Worth confirming production is not in that state before the demo — click-script step 4 covers it.
5. **O38** — confirm division-scoped ICHRA classes in the Summit UI before designing anything for T75.

---

# Session 6, prompt D — the agent-interaction walkthrough

**Source:** Kevin's step-by-step account of a real agent-and-client interaction, given conversationally
on **2026-08-01**. Marked **[K 8/1]** throughout the documents — a *better* source about agent behaviour
than any internal document, a *weaker* one about what the code does. **No code shipped.**

## Part 1 — four claims tested, two wrong, one of them mine

**Leading with this because it is the most useful thing in the run.**

| # | Claim | Verdict |
|---|---|---|
| 1 | `county_reference` is county → **one** ZIP and cannot be reversed | ✅ **The conversational claim was right.** V076: PK `county_fips`, one `representative_zip` per county, 254 TX rows. |
| — | …but **prompt C's version was wrong, and it was mine.** §4.2 called it *"exactly that data, unwired to the UI"* and T74 said the fix was *"small: a ZIP box that resolves through `CountyReferenceDAO`"* | ❌ **Both false.** Reversing it matches 254 ZIPs out of ~2,600 Texas ZCTAs. I sized a data build as wiring — corrected in place in both files |
| 2 | Some ZIP → county crosswalk exists | ❌ **None.** Not in `CountyReferenceDAO` (`findByFips` / `listByState` / `findByFipsIn`, all county-keyed), not a resource file, not a seeded table, not the API (`healthsherpa.md` 31 Jul: the route is closed) |
| 3 | The rate cache is keyed by rating area | ❌ **County FIPS.** `uq_rarc_year_county_age_tobacco (plan_year, county_fips, age, uses_tobacco)` — **no `rating_area` column exists**, despite the table being called `rating_area_rate_cache`. **"Dedupe by rating area before pricing" cannot be done against this cache** |
| 4 | The ICHRA JSPs are fixed-width desktop markup | ❌ **Overstated.** Viewport meta on all three, Bootstrap 5.3.3 loaded. Real defects are narrow: **no `overflow-x` wrapper or `.table-responsive` anywhere in the ICHRA path** (the five-column AGE_BAND and affordability tables are what overflow), ~10 fixed-px inputs, and the `calc(100vh - 64px)` shell. **T78 sizes down to three CSS fixes** |

**Two of these changed the run's output.** #2 turned the top-ranked build from wiring into a data build
with a migration — which is exactly the kind of thing that must not be discovered mid-build. #3 means
T77 must dedupe by county FIPS and **must not claim rating-area dedupe** in code or UI copy.

## Shipped

| Hash | What |
|---|---|
| `0bdfe74` | §4.1 rewritten as three fidelity tiers; six corrections applied to §4 and the §4.2 ranking; **§5 ZIP intake spec**; T74/T73/T71/T75 corrected; **T76–T83** added; **LA-15**, **LA-16**. |

## Decisions made

1. **T81 ships as a sandbox first** (recorded 2026-08-01). The employer corrects estimates, enters
   current group rates and an expected increase, figures update live — **and nothing is written.** No
   row, no PII, no authentication. Their corrections returning to the agent is better and is a
   **separate, later decision**. Rationale: build rule 3's worked example — render everything, store
   nothing, ship the useful part while the collection question stays open. Precedent already in the
   tree: prompt B's contribution slider (`53a8131`).
2. **§4.1's eight-stage ordering is superseded, not deleted.** Stage 1 and stages 6–8 survive; 2–5 are
   re-read through the tiers. The document-derived rows keep their provenance.
3. **T82 logged at Kevin's own confidence — *"probably"* — not as settled**, because it proposes
   changing shipped, working code rather than filling a gap.
4. **LA-15 records a tension and resolves nothing.** No wording changed anywhere in the illustration.

## New assumptions, and reversal cost

| Assumption | Reversal cost |
|---|---|
| **LA-15** — a subsidy-preserving *ceiling* is a different object from an affordability *threshold* | ⭐ Display edit both ways; nothing derived is persisted (`proposal_ichra_snapshot` is structurally incapable of carrying an affordability figure). Asymmetry is reputational: a ceiling presented as a target has been acted on |
| **LA-16** — employer-entered data on an unauthenticated proposal link | ⭐ **Zero while the sandbox holds — nothing to reverse, because nothing is stored.** Inverts sharply the moment a write path exists. **The cheap moment to decide is before the first row, which is now** |
| ZIP data ships Texas-first, matching V076 | A later migration of the same shape. Free |
| County-FIPS dedupe is the right granularity for T77 | Coarser than rating-area dedupe and cannot be improved without a rating-area column — a schema change, not a config one |

## Open questions

- **T83** — does HealthSherpa provide enrollment support during the enrollment window, and who holds
  the employee's hand? Kevin: *"not sure about this at all."* Gated by **O12 / O13 / O14**, none moved
  since 7/29. **Ask Forrest and ask HealthSherpa. Do not design around either answer** — an assumed
  answer here silently sets the scope of the whole enrollment phase.
- **T82** — what *should* the RANGE headline anchor on, if not the bronze floor? Settle the metric
  before touching the label.
- **T76** — how is on-miss warming bounded against a mistyped ZIP triggering an unbounded warm?

## Contradictions found

1. **My own prompt-C text vs the schema.** §4.2 and T74 both described `county_reference` as already
   carrying the ZIP crosswalk. It does not. Corrected in both files rather than quietly overwritten —
   the wrong sizing is the interesting part.
2. **Table name vs table.** `rating_area_rate_cache` has no rating-area column. Not a defect — the name
   describes the domain concept, the key describes the data — but it invited exactly the wrong
   assumption about dedupe, twice.
3. **§4.2's own ranking vs C1.** Prompt C put subsidy segmentation first, from documents that describe
   it as stage 2. Kevin's account says the agent has ages, not wages, at first contact. **The documents
   are not wrong about the capability; they are wrong about when it can run.**

## SQL close-out audit

**This run was forbidden from producing SQL and produced none.** No `.sql` file created, modified or
deleted; no schema change; no migration. **No `.java` or `.jsp` touched either.** `git show --stat
0bdfe74` lists three `.md` paths. Migrations unchanged at **V083**.

⚠️ **§5 specifies a migration it does not author** — the `zip → county_fips` table. That is deliberate
and stated in the spec: the next prompt writes it, under the normal migration discipline.

## Next

1. **Build T74 from §5.** It is the next build, and its first line is the one that changes the estimate:
   the crosswalk does not exist.
2. **Send the SWBD emails** — now three: O22 book profile, three renewing groups (the input
   `/GroupConversion` has waited for since it shipped), and the flowchart Forrest offered to draw. Add
   **T83**'s enrollment-support question to the same message.
3. **Click-script steps 10 / 10b** — still the only runtime verification of prompts A and B.
4. **O38** Summit UI check before anything in T75(b) or T77.

**Carried forward, still unclosed:**

- ⚠️ **Nobody has asked Forrest what he would want a quoting tool to do.** ⭐ **This run narrows the gap
  without closing it** — it is Kevin's account of how agents work, which is the closest thing to ground
  truth the project has, and it corrected six things. But Kevin is not Forrest, and the flowchart
  Forrest offered to draw is still not drawn.
- ⚠️ **The two SWBD emails have still never been sent** — O22 book profile, and *"send me three groups
  renewing next quarter"*.

---

# Session 6, prompt E — T74 part 1: the ZIP → county crosswalk

Data layer only. **No `.jsp` touched, no servlet touched.** Built from §5, which governs.

## Provenance — the fact this whole item rests on

| | |
|---|---|
| **Source URL** | `https://www2.census.gov/geo/docs/maps-data/data/rel2020/zcta520/tab20_zcta520_county20_natl.txt` |
| **Fetched** | 2026-08-01 — HTTP 200, 6,821,287 bytes, 47,864 lines |
| **Upstream primary source** | US Census Bureau, **2020 Census ZCTA-to-county relationship file** (2020 vintage) |
| **Licence** | Work of the US Government — **public domain, 17 U.S.C. 105.** No redistribution restriction |
| **Why this one** | It is the *primary* source, not a mirror, and it is **the same file V076 already uses** — so `zip_county` and `county_reference` are cut from one vintage and cannot disagree about which counties exist. Verified: **exact 254-of-254 set match, zero either way** |

**⭐ No crosswalk row was authored from model knowledge. Not one ZIP, not one FIPS code.** Every row is a
scripted transform of the file above, via `docs/scripts/generate_zip_county.ps1`, which is deterministic
— same source in, same file out. That was the run's hard stop and it was not approached.

**HUD was preferred and was tried first.** The prompt is right that a HUD-derived crosswalk is better:
built from real USPS delivery data, and it carries a **residential (address-count)** ratio rather than a
land-area one. It could not be obtained — `huduser.gov` file paths return **HTTP 202 with a zero-byte
body**, and `hudapi/public/usps` returns **401** without a registered access token, which I did not
create. Recorded in the V085 header and in the generator so the next person does not repeat the attempt
blind. Swapping to HUD later replaces V085's rows; V084's table is unchanged by it.

**⚠️ The prompt said `census.gov` was unreachable. It is reachable** — HTTP 200 on the first request.
That changed the run for the better: the primary source rather than a GitHub mirror whose licence and
vintage I would have had to establish second-hand.

## Counts — verified before commit, not assumed

| Measure | Value |
|---|---|
| Crosswalk rows (TX) | **2,894** — independently recounted from the source; generator and `awk` agree |
| Distinct ZCTAs | **1,992** |
| Distinct counties | **254** — all of Texas, exact match with `county_reference` |
| **ZCTAs spanning >1 county** | **686 — 34.4%** |
| `land_area_ratio` NULL | 0 · max exactly `1.000000`, none above |
| Width check | every `zip` and `county_fips` exactly 5 characters — regex over the generated file, not assumed |
| **V085 file size** | **89 KB** — no release-upload concern |

**34% is the number to carry forward.** A third of Texas ZIPs resolve to more than one county, so
disambiguation is the *common* path. Anything that auto-selects is wrong one time in three.

## Shipped

| Hash | What |
|---|---|
| `4f242af` | **V084** — `zip_county` table |
| `2689d67` | **V085** — 2,894 Texas rows + `generate_zip_county.ps1` |
| `3f7d92e` | Tracker + `schema_version_migration.sql` registration; highest version → **V085** |
| `d7cbc4f` | `ZipCounty`, `ZipCountyDAO`, `ZipCountyResolver` |
| `e6dee49` | §5 and T74/T77 updated |

`./mvnw compile` clean. **Nothing calls the resolver yet — intended, per the run's own framing.**

## Decisions made

1. **`zip CHAR(5)`, never an integer.** An integer column silently destroys every leading-zero ZIP.
   Texas starts at 7 so it would not have bitten today — it would have bitten on the first expansion
   migration, in production, quietly.
2. **No FK to `county_reference`**, because that table is **Texas-only** (V076, 254 rows — verified, not
   assumed). An FK would reject valid crosswalk rows for any state whose counties are not yet seeded.
3. **The ratio column is `land_area_ratio`, not `res_ratio`.** HUD's ratio counts addresses; this counts
   dirt. For a ZIP with a town on one side of a county line and ranchland on the other they disagree —
   so it orders choices and never makes one.
4. **`getUnique()` returns null on an ambiguous resolution** rather than the first candidate. A caller
   wanting "the" county for a ZIP that has three must confront that.
5. **No secondary index.** The composite PK is already the index for the only lookup performed.
6. **Texas only**, per §5 — not the prompt's "seed nationally if manageable". §5 governs; see below.

## New assumptions, and reversal cost

| Assumption | Reversal cost |
|---|---|
| A ZCTA-derived crosswalk is good enough to ship while HUD is unobtainable | **Low** — replace V085's rows via a new data migration. V084's table and all three Java classes are unchanged by the swap. The cost is borne meanwhile by users whose ZIP misses |
| Land-area ordering is an acceptable proxy for "show the likely county first" | **Free** — one `ORDER BY`, and nothing derives a displayed figure from it |
| Dropping crosswalk rows whose county is absent from `county_reference` is right | **Free** — one `if`. Today it can only fire on data inconsistency, since the two sets match exactly |

## Contradictions found

1. **The prompt vs reality on network access.** It stated `census.gov` was not reachable. It is. I used
   the primary source instead of a mirror — **better provenance than the prompt's own fallback plan.**
2. **The prompt vs §5 on scope.** The prompt says *"seed nationally if the file is manageable"*; §5 says
   *"Out of scope: multi-state ZIP data (Texas first, same as V076)."* **§5 governs, so Texas.** It also
   matches the business — SWBD is a Texas GA and Presidio is Texas-only.
3. **The prompt vs §5 on source.** §5 names the Census ZCTA file; the prompt prefers HUD for a real
   correctness reason (ZCTA ≠ ZIP) that §5 never addressed. I tried HUD first — the prompt's concern was
   sound — and fell back to §5's named source when HUD proved unobtainable, recording the limitation
   loudly in three places. **Not a conflict resolved against §5; §5 was silent on the point.**
4. **My own §5 text vs the source.** §5 estimated *"~2,600 ZCTAs"* for Texas. The real figure is
   **1,992**. Corrected in place.
5. **T77 as written was unbuildable.** It said to dedupe multi-location designs *by rating area*.
   `rating_area_rate_cache` **has no `rating_area` column** — dedupe is by `county_fips`. Corrected.

## SQL close-out audit — in full, because this run produced SQL

**Two versioned migrations, both new, both additive. No other SQL exists anywhere in this run.**

| File | Statements |
|---|---|
| `docs/migrations/V084__zip_county_crosswalk.sql` | `CREATE TABLE zip_county` · `CREATE OR REPLACE VIEW schema_info` · `INSERT IGNORE INTO schema_version` |
| `docs/migrations/V085__zip_county_crosswalk_tx.sql` | one `INSERT IGNORE INTO zip_county` carrying 2,894 value rows · `CREATE OR REPLACE VIEW schema_info` · `INSERT IGNORE INTO schema_version` |

- **Orphaned `.sql` files: none.** Both are versioned, sequential and registered.
- **No existing table altered.** `county_reference` and `rating_area_rate_cache` were read, never written.
- **No `INSERT INTO constant`** in either file, or anywhere in this run.
- **Idempotent:** `INSERT IGNORE` throughout; re-running either is a no-op.
- **Registered** in `docs/analysis/migration_tracker.md` and `docs/schema_version_migration.sql`.
- **Current highest version: V085** (was V083).
- **Pending deployment: both.** ⬜ in **every** column including local — neither has been run against any
  database. That is their true state, not a stale cell. Applying them changes no behaviour, because
  nothing reads `zip_county` yet.
- **Described but not scripted:** national expansion beyond Texas — deliberately, as a future `V0NN`
  data migration produced by `generate_zip_county.ps1 -State XX`.

## Next

1. **T74 part 2** — `IllustrationServlet` + `illustration25.jsp`. The resolver contract is in §5. The
   rule most likely to be shortcut: **never take element zero on an ambiguous ZIP**, and 34% of Texas
   ZIPs are ambiguous.
2. **Apply V084/V085 locally** and re-check the counts against a live `SELECT` before any UI trusts them.
3. **T76** warm-on-miss remains blocked on the same thing — no installation has ever authenticated to
   the HealthSherpa API.
4. Unchanged: click-script steps 10/10b, the three SWBD emails, T83's enrollment-support question.

**Carried forward, still unclosed:** nobody has asked Forrest what he would want a quoting tool to do,
and the two SWBD emails have still never been sent.

> ⚠️ **Superseded in part by prompt G.** The precedence rule this run shipped — *"the servlet only
> consults `?zip=` when `countyFips` is null or blank, so the existing parameter always wins"* — was
> **wrong**, and produced a silently-wrong-rates defect. See the prompt G section.

---

# Session 6, prompt F — T74 part 2: ZIP intake on the illustration

## What I anchored on

Both files are shipped, and both were edited **earlier in this same session** — the servlet by prompt B
(the affordability messages), the JSP by prompts A, B and D (the G5 toggle carry, the slider, the
verdict cells). So anchors were printed and uniqueness checked before any edit.

| File | Anchor | Line | Occurrences |
|---|---|---|---|
| `IllustrationServlet.java` | `String countyFips = request.getParameter("countyFips");` + its blank-check and `submittedCountyFips` set | **122–127** | **1** |
| `IllustrationServlet.java` | `"Select a valid county from the list."` (the unwarmed-county branch) | **169** | **1** |
| `illustration25.jsp` | the County `<div class="col-auto">` wrapping `<select id="countyFips" name="countyFips">` | **109–119** | **1** |

No anchor was ambiguous, so nothing was stopped or approximated.

## Shipped

| Hash | What |
|---|---|
| `5b70586` | `IllustrationServlet` — `?zip=` resolution, three outcomes |
| `a41a481` | `illustration25.jsp` — ZIP box, crossing-ZIP chooser, no-match message |
| `54505d2` | §5 updated, click-script steps 4a–4e, T74 closed for `/Illustration` |

`./mvnw compile` clean before each commit.

## Decisions made

1. **`?zip=` is consulted only when `countyFips` is absent.** The existing contract always wins, so the
   mode toggle, hub cards, T59's affordability card and any bookmarked URL are untouched.
2. **A unique ZIP falls through to the ordinary county path** rather than getting its own branch —
   the two paths share one code path and therefore cannot diverge.
3. **The chooser is a status card, not an alert.** 34% of Texas ZIPs land there; it is a normal step.
   Plain equal-weight links, nothing pre-selected, nothing badged likely.
4. **The unwarmed-county message differs on the ZIP path only.** Telling an agent who typed a ZIP to
   "select a valid county from the list" describes neither what they did nor what went wrong, and would
   collapse two states §5 requires kept apart. **The outcome is unchanged** — still an error, still no
   rates, still T76's. The county path keeps its original string byte-for-byte.
5. **Mobile deliberately not attempted** — the chooser is a plain list. That is T78.

## The exact no-match copy

> **We don't have ZIP 90210 in our county lookup**
> ZIP coverage is incomplete — the lookup is built from Census tabulation areas, which omit some valid
> ZIPs, and currently covers Texas only. This is a gap in our data, not a problem with the ZIP.
> **Select the county above instead** — everything else works the same.

The word "invalid" appears nowhere. An agent who believes he mistyped will retype it three times.

## New assumptions, and reversal cost

| Assumption | Reversal cost |
|---|---|
| An agent seeing a two-county chooser will read it as a normal step rather than a failure | **Free** — wording and styling only. Settled by click-script 4b |
| Naming the resolved county on the unwarmed-county error helps more than the generic string | **Free** — one `if`. The county path is unaffected either way |
| Land-area ordering reads as stability, not as a recommendation | **Free**, and the riskier direction is watched: if it ever reads as steering, drop the ordering entirely and sort by county name |

## Contradictions found

1. **The prompt says `?county=`; the actual parameter is `?countyFips=`.** Shorthand, not a conflict —
   I used the real name, which is what every existing link carries.
2. **Nothing in §5 was contradicted.** §5 governed on the point that mattered most — it says the county
   dropdown *"stays as a fallback"*, and this run kept it. §5 also scopes `/GroupConversion` to *"only
   after the illustration path is proven"*, so it was left alone despite the temptation to do both.
3. **A compile error worth recording:** assigning `countyFips` inside the ZIP branch made it no longer
   effectively final, breaking the county-matching lambda below. Fixed with a final copy. Caught by
   `./mvnw compile`, which is why it runs before each commit rather than at the end.

## SQL close-out audit

**This run was forbidden from producing SQL and produced none.** No `.sql` file created, modified or
deleted. No schema change, no migration. **V084/V085 are the schema for this feature and were finished
in prompt E** — nothing here revisits them. `ls docs/migrations/` unchanged at **V085**. `git show
--stat` across the three commits lists one `.java`, one `.jsp` and two `.md` paths.

## ⚠️ Not runtime-verified

**V084 and V085 have not been applied to any database, including local**, so `zip_county` does not exist
where this code runs. Per build rule 5 I coded against the model rather than the rows, and every example
ZIP in the click-script was verified against the **shipped V085 file** instead: `75482` → Hopkins alone
(ratio 1.000000), `75009` → Collin 0.93 / Denton 0.07, `90210` absent. **This is `code-verified` only.**
Click-script steps 4a–4e are what make it real.

## Next

1. **Apply V084/V085**, then run click-script **4a–4e**. Step **4b** is the one that matters — if a
   county is ever auto-selected on a crossing ZIP, the feature is worse than the dropdown it replaced.
2. **`/GroupConversion` ZIP intake** — the same treatment, once the illustration path is proven.
3. **T76** warm-on-miss still blocked: no installation has authenticated to the HealthSherpa API.
4. Unchanged: click-script 10/10b, the three SWBD emails, T83's enrollment-support question.

**Carried forward, still unclosed:** nobody has asked Forrest what he would want a quoting tool to do,
and the two SWBD emails have still never been sent.

---

# Session 6, prompt G — ZIP intake repair

Runtime walk on production, 2026-08-01, role-2 agent. The good path works; the ZIP path was broken in
five ways.

## ⭐ R1 first — a shipped instruction produced silently wrong rates

**An agent had Hopkins selected from a previous run, typed ZIP `75009` — Collin/Denton, not Hopkins —
and got Hopkins results. No warning, no mismatch notice.** Wrong county, wrong rates, **indistinguishable
from right ones**, in front of a client. It is the exact failure the crossing-ZIP chooser was built to
prevent, arriving through a door nobody had modelled.

**The cause was an instruction I wrote, not a coding slip.** Prompt F specified:

> *"the servlet only consults `?zip=` when `countyFips` is null or blank, so the existing parameter
> always wins."*

That was written to protect the `?countyFips=` URL contract, and it does protect it. But **"always wins"
also means a stale dropdown selection beats a freshly typed ZIP.** The sentence was precise, defensible,
and wrong — and it was reviewed and shipped as written.

**Three consecutive runs verified this feature and all three missed it.** Prompt E was code-verified,
prompt F was code-verified, prompt F's own compliance statement said so plainly. **One runtime walk found
it in about a minute.** That is the most useful thing in this session's record: `code-verified` is not a
weaker form of `runtime-verified`, it is a different claim, and for a defect that lives in the
*interaction between two inputs* it is close to worthless. The click-script existed precisely to catch
this class of thing and had not been run.

**The corrected rule** — a present ZIP is always resolved, and a county the ZIP contradicts is never
computed from. Not with a warning. Not at all.

| ZIP | County | Behaviour |
|---|---|---|
| blank | set | **County wins** — the `?countyFips=` contract, preserved |
| set, **agrees** | set | Proceed on that county |
| set, **contradicts**, resolves to one | set | The ZIP replaces the selection |
| set, **contradicts**, resolves to several | set | Chooser. **Nothing computed** |
| set, resolves to nothing | set | No-match. **No fallback to the stale county** |
| set | blank | As built |

## What I anchored on

Both files had been edited three times this session. Every anchor was printed with line numbers and
checked for single occurrence **before** any edit; none was ambiguous, so nothing was stopped.

| File | Anchor | Line | Occurrences |
|---|---|---|---|
| `IllustrationServlet.java` | `String countyFips = request.getParameter("countyFips");` | **123** | 1 |
| `illustration25.jsp` | `${not empty selectedCounty and mode == 'AGE_BAND'}` | **282** | 1 |
| `illustration25.jsp` | `${not empty selectedCounty and mode != 'AGE_BAND'}` | **653** | 1 |
| `illustration25.jsp` | `id="zip" name="zip"` | **121** | 1 |
| `illustration25.jsp` | `${not empty zipCandidates}` | **229** | 1 |
| `illustration25.jsp` | `c:if test="${zipNoMatch}"` | **263** | 1 |

## R1–R5, and how each resolved

| # | Finding | Status |
|---|---|---|
| **R1** → **T84** | Stale county silently overrode a typed ZIP | ✅ `de0efe0` — precedence rewritten; `Resolution.containsCounty()` added |
| **R2** → **T85** | ZIP resolved only on Enter, and Enter submitted the form | ✅ `de0efe0` + `6543db7` — new `/IchraZipLookup` endpoint, resolve on blur |
| **R3** → **T86** | *"No rate data for this county yet"* on a pure validation failure | ✅ `6543db7` — both result panels now require `empty inputError` |
| **R4** → **T87** | A stale panel survived the next interaction | ✅ `6543db7` — editing the ZIP clears panels and selection immediately |
| **R5** → **T88** | `?countyFips=` direct-URL behaviour unconfirmed | ✅ **Investigated, no defect, no code changed** |

**R3's cause is worth stating precisely**, because it is the same class as R1: the mode handler sets
`inputError` and **returns before `hasRates` is set**, so the JSP's `not hasRates` branch fired and
printed the unwarmed-county message for a county that returns a full table one screenshot later. **That
collapsed the two states prompt F required kept apart** — in the direction nobody was watching, since
every prior check had been aimed at the ZIP side of that pair.

**R5's answer, settled from code rather than by asking:** `?countyFips=` **does** pre-select the
dropdown. `IllustrationServlet` sets `submittedCountyFips` unconditionally once a county is present,
before any mode handler runs, and the JSP's option tag selects on it. The note *"requires county
selection"* meant only that a headcount is also needed — expected, not a regression. Click-script step
4e now asserts the pre-selection so a future regression is caught.

## Shipped

| Hash | What |
|---|---|
| `de0efe0` | Precedence fix, `containsCounty()`, new `IchraZipLookup` JSON endpoint |
| `6543db7` | Blur lookup, R3 panel guards, R4 clearing, corrected ZIP-field comment |
| `2b2ca7a` | §5 rule replaced, click-script 4f–4j, T84–T88 |

`./mvnw compile` clean before each commit.

## Decisions made

1. **A contradicted county is never computed from** — not with a warning banner, which was the tempting
   cheaper option. A warning on a page an agent is presenting from is a warning nobody reads.
2. **The chooser and no-match panels are now rendered always and hidden**, so the blur path toggles the
   *same* markup instead of carrying a second copy of the wording in JavaScript. **The copy has one
   source and cannot drift.** It is unchanged, and "invalid" still appears nowhere a user can see it —
   the one occurrence in the file is inside a JSP comment telling future edits not to use it.
3. **The endpoint resolves and nothing else** — no rates, no cache read, no warming. T76 stays untouched.
4. **Server-side precedence is enforced independently of the script.** JavaScript may be off, and `?zip=`
   can arrive in a URL; the servlet does not trust the page.
5. **R5 was settled by reading the code, not by asking Kevin** — as instructed, and it took less time
   than writing the question would have.

## New assumptions, and reversal cost

| Assumption | Reversal cost |
|---|---|
| Clearing the county selection the moment the ZIP changes is right, even mid-typing | **Free** — two lines. The risk is an agent who edits a ZIP and loses a deliberate county pick; the alternative risk is R1, which is far worse |
| A single-county ZIP should silently fill the dropdown rather than announce itself | **Free** — the resolved-county note is one element; make it louder if agents miss it |
| Ordering candidates by land-area share reads as stability, not ranking | **Free** — drop the ordering and sort by name if it ever reads as steering |

## Contradictions found

1. **Prompt F's precedence instruction vs. correct behaviour** — the R1 defect. Mine, shipped, and
   corrected here. The old rule is struck through in §5 rather than deleted, so it stays visible beside
   what replaced it.
2. **Prompt F's own comment in the JSP** still asserted the "county always wins" rule at the ZIP field.
   Corrected — a stale comment restating the exact false premise that caused the bug is how it comes back.
3. **Nothing in prompt G was overridden.** Its instruction to settle R5 from code rather than asking was
   right, and its read of R3 as "the two states collapsing in the unwatched direction" was exactly what
   the code showed.

## SQL close-out audit

**This run was forbidden from producing SQL and produced none.** No `.sql` file created, modified or
deleted. No schema change, no migration. V084/V085 are unchanged and remain correct. `ls
docs/migrations/` unchanged at **V085**. The three commits list two `.java` (plus one new `.java`), one
`.jsp` and two `.md` paths — no `.sql`.

## What happens with JavaScript disabled

**Everything still works.** The ZIP field posts as `?zip=` on submit and `IllustrationServlet` resolves
it server-side with the same precedence rule, producing the same three outcomes — unique, chooser,
no-match — all server-rendered. The blur lookup is an enhancement that removes a submit, not a
dependency. **R2's original symptom does return without the script** (a ZIP typed with an empty headcount
submits and reports the headcount error), which is why the server-side rule had to be correct on its own
rather than relying on the field to have pre-resolved. Click-script step 4j covers this.

## ⚠️ Still not runtime-verified

V084/V085 have **still** not been applied to any database, so `zip_county` does not exist where this code
runs. Everything above is `code-verified` — **the same claim that missed R1 three times.** Click-script
steps 4a–4j are what make it real, and **4f is the one that matters**: Hopkins selected, type `75009`,
and Hopkins results appearing means the defect is back.

## Next

1. **Apply V084/V085, then run click-script 4a–4j.** Step **4f** first.
2. **Rebuild the WAR** — the production artifact built earlier today predates all of R1–R4 and carries
   the defect.
3. **`/GroupConversion` ZIP intake** — unchanged in scope, and now with a precedence rule that is known
   correct rather than assumed.
4. **T76** warm-on-miss still blocked: no installation has authenticated to the HealthSherpa API.
5. Unchanged: click-script 10/10b, the three SWBD emails, T83's enrollment-support question.

**Carried forward, still unclosed:** nobody has asked Forrest what he would want a quoting tool to do,
and the two SWBD emails have still never been sent.

---

# Session 6, prompt H — the coverage mismatch

## Part 1, answer 1 — what populates the county dropdown

**The inference was right.** `IllustrationServlet.java:113-118`:

```java
List<RateCacheDAO.CountySummary> summaries = RateCacheDAO.getCountySummaries(em, planYear);
List<String> cachedFips = summaries.stream().map(...::getCountyFips).collect(...);
List<CountyReference> availableCounties = CountyReferenceDAO.findByFipsIn(em, cachedFips);
```

`RateCacheDAO.JPQL_COUNTY_SUMMARIES` is `SELECT r.countyFips, COUNT(r), … FROM RatingAreaRateCache r
WHERE r.planYear = :planYear GROUP BY r.countyFips`. So the dropdown is **distinct counties present in
`rating_area_rate_cache` for the selected plan year**, intersected with `county_reference` — *not*
`county_reference`'s 254.

**On the count: source proves the mechanism, not the number.** The dropdown holds however many counties
have been warmed, which is a property of production data, not of the code. **Four** is the walk's
observation and I could not verify it from source — V084/V085 are still unapplied locally, and the
production cache is not readable from here. The mismatch does not depend on the exact figure.

## Part 1, answer 2 — `?countyFips=` at runtime, and the `<Hopkins>` artefact

**Two different questions, two different right answers, and until this run they shared one wrong message.**

| URL | Before this run | After |
|---|---|---|
| `?countyFips=48223` (Hopkins, **in the cache**) | Works — dropdown pre-selects Hopkins, headcount error if blank. **No "select a valid county"** | Unchanged |
| `?countyFips=48085` (Collin, **in the crosswalk, not the cache**) | ⚠️ *"Select a valid county from the list."* — **wrong; blames the agent** | *"We don't have rates for Collin County, TX yet."* |
| `?countyFips=<Hopkins>` (a pasted placeholder) | *"Select a valid county from the list."* — **correct** | Unchanged |

**The walk's result was a literal-paste artefact**, as the prompt suspected: `<Hopkins>` is not a FIPS,
`availableCounties` cannot contain it, and the message was right for that input. **Prompt G's code-read
of `?countyFips=48223` was correct** — R5/T88 stands.

⚠️ **But the artefact was standing in front of a real defect.** The same message was also firing for
`48085`, where it is false. A wrong diagnosis pointed at the right file.

## The finding

**The crosswalk knows 254 Texas counties. The illustration prices only the warmed ones.** So ZIP
resolution could hand an agent a real county the tool has never been able to price, then reject it as
invalid. Observed: `75009` rendered its chooser correctly — Collin and Denton, equal weight, nothing
pre-selected — and **clicking either did nothing useful.**

**ZIP intake did not create this gap. It exposed it.** Before ZIP the agent picked from four counties and
never saw the boundary.

## Shipped

| Hash | What |
|---|---|
| `8285b83` | `describeUnavailableCounty` — the single seam; `IchraZipLookup` reports `priced` per candidate |
| `0e2e014` | Chooser labels an unpriceable candidate, both renderers |
| `707ede6` | §5's fourth state, click-script 4k–4n, T89, T76's attachment point |

`./mvnw compile` clean before each commit.

## ⭐ The pattern — three defects, three code-reads, three runtime disproofs

**This is the session's most useful finding, and it is now a pattern rather than an incident.**

| # | Declared from code | Disproved by the walk |
|---|---|---|
| **R1 / T84** | *"`countyFips` always wins"* — precise, defensible, protecting a real contract | A stale selection beat a typed ZIP → **wrong county's rates, indistinguishable from right ones** |
| **R5 / T88** | *"`?countyFips=` pre-selects; no defect"* | Correct **for the case read** — and the untested neighbouring case (`48085`) was broken |
| **T89** | *"the chooser is correct; nothing pre-selected"* — true, and it passed review twice | Both offered counties were unpickable |

Every one was **correct about the code it examined** and wrong about the system. The common shape: the
defect lived in the **interaction between two things** — a stale input and a fresh one, a crosswalk and a
cache — and reading either alone showed nothing wrong.

**`code-verified` is not a weaker `runtime-verified`. It is a different claim.** It says "this code does
what I think it does". It does not say "the feature works". For anything spanning two data sources or two
inputs, the second claim needs the walk, and no amount of the first substitutes.

**Proposed line for `CLAUDE.md`'s "Keeping state docs current" ritual — proposed, not applied:**

> **A feature spanning two data sources or two inputs is not verified until it has been walked.** Mark
> such work `code-verified` and keep it out of a release note until a runtime walk clears it — three
> defects in the ICHRA ZIP path (T84, T88, T89) were each declared resolved from code reading and each
> disproved by the first walk that ran.

## Decisions made

1. **One unwarmed message for every entry path**, not a fourth message. It replaces the ZIP-specific
   variant rather than joining it.
2. **The label is descriptive, never evaluative.** *"— no rates cached yet"* is a fact about our data.
   The unpriced entry keeps its link, its weight and its land-area position: **not demoted, not greyed
   out, not disabled**. Steering counties is steering.
3. **No warm stub, button or TODO** at T76's seam — a disabled control implying a capability that does
   not exist is worse than its absence.
4. **The dropdown's four counties were left alone**, as instructed. Whether it should list all 254 with
   most marked unavailable is **T89's open half** and is downstream of T76: with warm-on-miss it is
   reasonable, without it it is 250 dead options.
5. **A missing `pricedCountyFips` shows the caveat rather than throwing** — both in the JSP's `empty`
   guard and the endpoint's empty-set default. Cautious direction on both sides.

## New assumptions, and reversal cost

| Assumption | Reversal cost |
|---|---|
| An agent would rather see an unpriceable county labelled than hidden | **Free** — one `c:if` and one JS branch. Hiding it would be worse: the employer sits in that county whether we can price it or not |
| Land-area ordering should not change to put priced counties first | **Free** to reverse, but doing so **would be steering** — the label carries the information without ranking |
| `— no rates cached yet` reads as our gap, not the county's problem | **Free** — wording only |

## Contradictions found

1. **Prompt G's R5 conclusion was right and incomplete.** `?countyFips=48223` does pre-select; the
   code-read was sound. It just answered a narrower question than the symptom implied, and the
   neighbouring case was broken.
2. **§5 listed three states; there were four.** "Not a county at all" had been folded into the unwarmed
   case, which is exactly the collapse §5 warns against — committed by the section that warns about it.
3. **Nothing in prompt H was overridden.** Its two suspicions — that the dropdown is cache-derived and
   that `<Hopkins>` was a paste artefact — were both correct.

## SQL close-out audit

**This run was forbidden from producing SQL and produced none.** No `.sql` file created, modified or
deleted; no migration; no schema change. **The rate cache and its warm job were not touched in any way.**
`ls docs/migrations/` unchanged at **V085**. The four commits list two `.java`, one `.jsp` and two `.md`
paths.

## Next

1. **Apply V084/V085, then run click-script 4a–4n.** **4l and 4m** are this run's regression tests;
   **4f** is still R1's.
2. **Rebuild the WAR** — the artefact built earlier today predates R1–R4 *and* this run.
3. **T76** — the seam is ready. Note before building: warming from staging stamps `source_env = STAGING`,
   so a warmed county **works but is not demoable**, and it helps only inside Texas.
4. **T89's open half** — the dropdown's scope, after T76.
5. Unchanged: click-script 10/10b, the three SWBD emails, T83.

**Carried forward, still unclosed:** nobody has asked Forrest what he would want a quoting tool to do,
and the two SWBD emails have still never been sent.

---

# Session 6, prompt I — the illustration surface after runtime review

## ⭐ W10 first — the finding that was not a defect

> *"the slider was actually hard for me to even see it as a tool, I looked right past it to the data. I
> didn't get the impression that the page was anything other than a static result."*

**The slider works perfectly.** Every verdict flips at the right figure; the walk confirmed $420 and
$573. **And the person who watched it get built looked past it.** That control is build-plan §1 step 5 —
*"at $419 Maria keeps her subsidy, at $420 she doesn't"* — and it is the one thing this page has that a
competitor's quote engine does not. A control nobody notices is a control nobody drags, and a demo where
nobody drags it is a demo of a calculator.

**The cause was visible in the markup once the symptom named it:** the slider used `.status-card` —
byte-identical to the read-only result panels above and below it. **It looked like output because it was
dressed as output.**

**What I built**

| | Why |
|---|---|
| Own tint + left accent edge | Stops it being one of the result cards. The single highest-value change, because it fixes the actual cause |
| Filled track behind the handle | Reads as a control rather than a rule |
| 22px handle, grab cursor, focus/hover ring | Reads as draggable at a glance; also the touch target |
| ⭐ **Flip-point ticks on the track**, labelled *"age N"* | **Makes the money moment visible before anything is dragged** — the difference between a control an agent notices and one they do not |

**What I rejected:** moving it. It already sits directly above the table it changes; moving it below
would bury it further. The *"drag to see the effect; nothing is saved"* label and the *"Showing $X; the
figures were calculated at $Y. Reset"* line are both kept unchanged, as instructed.

**Why it cannot read as a recommendation.** This is the most tempting place in the product to cross the
no-steering line, so the constraint drove the design rather than being checked afterwards:

- **Every tick is identical** — same colour, same width, same label form. Nothing distinguishes one flip
  point from another, because nothing about them differs except the number.
- **No region is shaded.** No green below a threshold, no red above, no highlighted band. That was the
  obvious "helpful" move and it is exactly the one that would have said *aim here*.
- **The track fill is a position indicator**, in the existing SSA blue — it shows where the handle is,
  not that the covered region is good.
- **The legend states a fact:** *"Marks on the track show where each age band's affordability verdict
  changes."* Not where to aim.
- **No auto-animation, no auto-play, no "try me" tooltip.** The page must not appear to do arithmetic by
  itself in front of a client.
- Ticks read `data-flip` from rows the server already computed — **no threshold is recalculated
  client-side** — and are `aria-hidden`, since the affordability table already exposes the same figures
  properly.

⚠️ **Known imprecision, stated rather than hidden:** ticks are positioned as a percentage of track width,
so near the extremes they sit a few pixels off the handle (thumb width is not accounted for). A visual
cue; the exact figures are in the table.

## ⭐ The session's real lesson

**This was the first session in which a full runtime walk was performed.** It followed **three
consecutive runs that reported clean from code reading** — prompts E, F and H each said `code-verified`
in their own compliance statements, and each was correct about the code it examined.

**The walk produced twelve findings.** And the most important one **was not a defect at all**: the
slider does exactly what it was built to do, and that turned out not to matter, because nobody saw it.

No amount of code reading finds that. There is no wrong line to read.

## W2 — what the source actually showed

**The inference was right, and I verified it before changing anything.**
`IllustrationServlet.java:775` read:

```java
logRow.setZipCode(county.getRepresentativeZip());
```

Unconditional. So a run where the agent typed nothing and picked Hopkins from the dropdown logged
`zip_code = '75437'` — **Hopkins's representative ZIP from `county_reference` (V076)**, not anything a
person entered. The column read like user input and was not: anyone auditing the log would conclude an
agent typed a ZIP they never typed.

**Now:** the ZIP is recorded only when the agent actually supplied one, NULL otherwise. `county_fips`
already carries the county, so the derived value added no information and actively misled. **Strictly a
reduction in what is stored** — the only direction this column may ever move. No migration; the column
is unchanged and nullable.

## Shipped

| Hash | What |
|---|---|
| `7bc5794` | W1, W2, W3, W4, W5, W6, W11 |
| `ced1679` | **W10**, committed separately as instructed |
| `80ef35a` | Click-script 4o–4r and 10a′/10a″/10c; T90–T94 |

`./mvnw compile` clean before each commit.

## Decisions made

1. **W4 removed the confirmation but not the information.** Deleting the under-field note fixed the
   redundancy and the alignment shift — but it was also the only signal for a ZIP resolving to **one**
   county with no rates, which the dropdown *cannot* show, because the dropdown is built from counties
   that have rates. That case moved to its own panel outside the form row, reusing the servlet's exact
   wording rather than inventing a second phrasing.
2. **W6 states a constraint instead of prescribing a move.** *"Select another county from the list"* is
   wrong advice when every county a ZIP resolved to is uncached — following it means running a
   neighbouring county's rates for a client.
3. **W11 fixed in CSS, not markup.** The disabled button appears in both mode branches; a page-scoped
   rule covers both without editing either, and reverts by deleting the rule.
4. **W1 wrapped rather than enumerated.** One `#illustrationResults` wrapper hidden in the existing
   `invalidate()`, instead of hunting individual panels — fewer places for the next result panel to be
   forgotten.

## New assumptions, and reversal cost

| Assumption | Reversal cost |
|---|---|
| Ticks make the slider noticeable without reading as guidance | **Free** — delete `renderTicks` and the `.contrib-tick` rules. ⚠️ The asymmetry matters: if they *do* read as guidance, that is a compliance problem, not a cosmetic one, so this is the one to check first at 10a″ |
| Hiding results on ZIP change is never unwanted | **Free.** The alternative — leaving them — is W1, which is worse |
| `#####` reads as a format hint, not a value | **Free** — one attribute |
| Silence on a successful ZIP resolve is clearer than confirming it | **Free.** The dropdown is the confirmation; if agents miss it, restore a note *outside* the form row |

## Regression fence — how I checked it still holds

The walk's passing behaviours were the fence, and none of this run touches their logic:

- **Affordability, flip-point and AGE_BAND calculation** — untouched. The ticks *read* `data-flip`;
  nothing recomputes a threshold, and `AffordabilityCalculator` was not opened.
- **Slider verdict flipping** — the `render()` maths is byte-identical; only the track fill line was
  added.
- **ZIP resolution in all three states, both coverage-mismatch paths, `?countyFips=`** — the precedence
  block and `describeUnavailableCounty` were not touched except W6's trailing sentence.
- **Mode carry both directions** — the toolbar links were not touched.
- **The three banners and the `90210` copy** — verified present and unchanged by grep after each commit
  (staging ×2, off-exchange ×2, no-match ×1).
- **Structure** — JSTL tag balance and `<div>` balance checked after every edit (20/20 `c:if`, 65/65
  divs at the end), plus `./mvnw compile`.

⚠️ **This is code-verification again**, which is exactly the claim this session learned to distrust. The
fence is only genuinely intact once the walk re-runs — steps 4o–4r and 10a′–10c are new and untested.

## Contradictions found

1. **W4 as specified would have lost information.** *"Removing it fixes both"* is true of the redundancy
   and the alignment, and it would also have removed the only signal for the single-unpriced-county case.
   Removed as instructed; the information relocated.
2. **Nothing else in prompt I was overridden.** W2's inference held, and the eight findings were all
   reproducible from source.

## SQL close-out audit

**This run was forbidden from producing SQL and produced none.** No `.sql` file created, modified or
deleted; no migration; no schema change. ⚠️ **W2 changes what is written to an existing column and
required no migration** — the column was already nullable, and the change only ever writes less. `ls
docs/migrations/` unchanged at **V085**. The four commits list one `.java`, one `.jsp` and two `.md`.

## Judged not worth changing

- **The Design Advisor's missing LIVE badge** — `ichraHome25.jsp`, out of fence, ships alone.
- **The two identical `<button ... disabled>` blocks** — left as-is; CSS covers both, and editing
  duplicate markup twice invites drift.
- **The unpriced panel's footnote duplicating the servlet's W6 sentence.** They are two renderings of one
  state (before the click and after), so the duplication is deliberate — but it *is* a drift risk, and if
  a third copy ever appears the wording should move to one place.
- **Tick pixel precision near the track extremes** — a visual cue, with exact figures in the table.

## Next

1. **Re-walk the click-script**, now 4a–4r and 10a′–10c. **10a′ is the one that matters** — look at the
   page for five seconds and see whether the slider announces itself. If it still does not, the fix
   failed regardless of how good the ticks look.
2. **Rebuild the WAR.** The artefact built earlier today predates R1–R4, H and all of this.
3. **T76** — seam ready at `describeUnavailableCounty`.
4. Unchanged: T89's dropdown-scope question (after T76), the three SWBD emails, T83.

**Carried forward, still unclosed:** nobody has asked Forrest what he would want a quoting tool to do,
and the two SWBD emails have still never been sent.

---

# Session 6, prompt J — the illustration on a phone, across a desk

## ⭐ What the repeater did to the query parameters: nothing

**That was the contract most at risk, so it is the first thing to state.** Rows are still
`age1..ageN` / `count1..countN` / `income1..incomeN`, **contiguous from 1**. Nothing was renamed, no
parameter changed shape, and no gap can appear.

The mechanism is `renumber()`, called after **every** add and remove: it rewrites `id`, `name` and the
label's `for` across all remaining rows so the set is always 1..N. Verified after the change — the JSP
still emits `name="age${i}"`, `name="count${i}"`, `name="income${i}"` and the proposal hand-off loop is
untouched at `end="6"`.

This mattered because `/Illustration` is GET-only and its **URLs carry state**: the mode toggle, the hub
cards, T59's affordability card, the proposal hand-off and every link verified this session ride on
those names. A repeater that renamed to `age[]` or left `age1, age3` after a removal would have broken
all of it silently.

## ⚠️ What I did not do, and why

**The six-row cap stays.** W7 asked for no arbitrary markup cap, with any limit server-side and
generous. The limit *is* server-side — `IllustrationServlet.AGE_BAND_ROWS`, now **published to the JSP**
rather than duplicated in it — but **it cannot be raised from the ICHRA side alone.**

`proposalBuilder.jsp` echoes **exactly six** `age`/`count` hidden-field pairs into the proposal POST, and
that file is outside this run's fence. Raising `AGE_BAND_ROWS` to a generous number without raising it
there would **silently drop rows 7+ from every proposal snapshot** — a wrong figure on a client-facing
document, produced by a change that looks purely additive at this end.

Per the run's own instruction — *"if a change would put any fenced behaviour at risk, do not make it, log
it instead"* — logged as **T96**, with the constraint now written at both ends so the next person meets
it before the bug rather than after. Six bands covers Sandoval (three) and every case seen so far.

## Shipped

| Hash | What |
|---|---|
| `fe35261` | **W13** — ZIP carries across the mode toggle |
| `609e56d` | **W7 + W8** — repeater; income only on the INCOME basis |
| `7e78f6a` | **W14** — input block collapses after a result |
| `682dcef` | **T78** — the three named mobile defects |
| `b12f875` | **W9** — two tap-triggered popovers |
| `031355b` | Click-script L1–L10 + mobile M1–M7; T95, T96 |

`./mvnw compile` clean before each commit; separate commits per concern as instructed.

## W8 — which bases consume income, from source

| Basis | Reads per-row income? | Evidence |
|---|---|---|
| **None** | No — affordability is not computed at all | `IllustrationServlet:435` returns unless FPL or INCOME |
| **FPL Safe Harbor** | **No** — uses the configured `FPL_ANNUAL_<year>` constant | `:452-453`, and `:475` `referenceIncome = "FPL".equals(basis) ? fplAnnual : row.getIncome()` |
| **Entered Income** | **Yes**, and requires it on every row | `:306` `incomeBasis`, `:327` parses only then |

So the source matched the prompt's expectation. **Visibility is driven off the basis selector**, not a
bare toggle — an agent choosing FPL sees the income fields disappear, which teaches the relationship
instead of hiding a field. Values stay in the DOM when hidden, so switching basis and back loses
nothing, and a hidden input submitting is harmless because the servlet only reads income on INCOME.

## The popover text, verbatim

**Employer Monthly Contribution:**

> The amount the employer puts toward each employee's individual premium every month. It lowers what the
> employee pays and raises the employer's total outlay. It is also what the affordability threshold is
> measured against.

**Affordability Basis:**

> Which income figure the affordability threshold is calculated from. FPL Safe Harbor uses the federal
> poverty guideline, so no employee income is needed. Entered Income uses an income you type for each age
> band. None hides the affordability section entirely.

**Neither suggests a value.** No typical figure, no starting point, no range, no "most employers". The
basis text names what each option *calculates from* — a fact about the mechanism, not a preference
between them. Grepped for `typical|recommend|suggest|start|usually|most employers|should`: no hits.
Trigger is click/focus, because a hover-only tooltip does not exist on a phone.

## Decisions made

1. **The collapse is presentation only.** The form is hidden, never emptied or detached, so Edit
   re-shows exactly what was submitted. The summary is built from the **live form controls**, not server
   attributes, so it cannot disagree with what the form holds after the ZIP field or repeater changed
   something client-side.
2. **The banners were left completely alone.** They are the obvious next place to find vertical space
   and the instruction was explicit. Verified present in both mode branches after every commit.
3. **Ticks degrade, the handle does not.** On a narrow track the "age N" labels drop and the marks stay —
   the marks are what make the flip points visible before anything is dragged, and the exact figures are
   in the table below either way.
4. **`100vh` kept at the desktop breakpoint.** The toolbar-plus-scroll-body layout is deliberate on a
   wide screen; only the mobile case reverts to normal page scrolling.
5. **The remove control hides on a lone row** rather than being disabled — removing the only row would
   leave an unsubmittable form, and a silently dead button is worse than no button.

## Anchor ambiguity, and how it was resolved

Two anchors were **not** single-occurrence, so neither was edited blind:

- `<c:forEach begin="1" end="6" var="i">` — **twice** (the form repeater and the proposal hand-off).
  Distinguished by surrounding context; **only the form one was touched**, and the hand-off loop is
  verified still at `end="6"`.
- `<table class="results-table">` — **three times**, and all three needed the identical wrapper, so
  `replace_all` was correct rather than a shortcut. Closing tags were handled in two edits because their
  indentation differs. Verified: 3 tables, 3 `.table-responsive` wrappers, `<div>` balance 70/70.

## New assumptions, and reversal cost

| Assumption | Reversal cost |
|---|---|
| Collapsing after a result is always wanted | **Free** — delete the summary card and the `hasResult` set. ⚠️ If an agent wants to tweak one field repeatedly, Edit is one extra click each time; L3 is where that shows up |
| A one-row repeater is clearer than five visible rows | **Free.** The risk is an agent not noticing "+ Add age band" — L4 checks it |
| Marks without labels still communicate on a phone | **Free** — one media query. The figures are in the table regardless |
| Six bands is enough | **Free to raise, but only with `proposalBuilder.jsp`** — see T96 |

## Fenced behaviours: what I could and could not verify

**Could verify (code/structure only):** parameter names unchanged and hand-off loop intact (grep);
banners present in both branches (grep, after every commit); JSTL and `<div>` balance after every edit;
`./mvnw compile` clean; no fixed pixel widths remaining; three tables wrapped; no steering words in
popover copy.

**Could not verify — all of it behavioural:** `?countyFips=` pre-select · the three ZIP states ·
county-ZIP precedence and the contradicted-county discard · results clearing on ZIP change · typed-vs-
derived ZIP logging · ZIP surviving the chooser click · unavailable-county wording · disabled proposal
link · mode carry · **the slider, its ticks and verdict flipping** · `illustration_log` writing.

⚠️ **None of the logic behind those was touched** — this run changed layout, visibility and markup
structure. But that is exactly the claim this session learned to distrust three times over, and it is a
weaker claim here than usual: **this is the largest visual change the file has taken**, and the slider
and its ticks now live inside a card that starts hidden. **The fence is not verified until L1–L10 and
M1–M7 run.**

## Contradictions found

1. **W7's "no arbitrary cap" could not be fully honoured** — the real cap lives in a file outside the
   fence. Logged as T96 rather than risked.
2. **W4's earlier removal interacts with W14.** The unpriced-county panel added in prompt I sits outside
   the form card, so it survives the collapse correctly — worth noting because had it stayed inside the
   form, collapsing would have hidden a message the agent needs.
3. **Nothing else in prompt J was overridden.** W8's source expectation held exactly.

## SQL close-out audit

**This run was forbidden from producing SQL and produced none.** No `.sql` file created, modified or
deleted; no migration; no schema change. `ls docs/migrations/` unchanged at **V085**. The six commits
list one `.java`, one `.jsp` and two `.md` paths.

## Next

1. **Walk L1–L10 and M1–M7**, and re-check the full fence. **L5 first** — the URL after removing a middle
   band — because that is the contract this run put most at risk. **M1 second**: the staging banner
   visible without scrolling on a phone.
2. **Rebuild the WAR.** The artefact built earlier today predates prompts G, H, I and J.
3. **T96** — decide whether to raise the band cap, which needs `proposalBuilder.jsp` in scope.
4. Unchanged: **T76** (seam ready), T89's dropdown-scope question, the three SWBD emails, T83.

**Carried forward, still unclosed:** nobody has asked Forrest what he would want a quoting tool to do,
and the two SWBD emails have still never been sent.

---

# Session 6, prompt K — one analysis surface

## How `mode` is derived, and how the old URLs still resolve

**That is the contract most at risk, so it goes first.** `mode` was **not** removed, renamed or
deprecated. `IllustrationServlet`:

```java
if (MODE_AGE_BAND.equals(modeParam))      mode = MODE_AGE_BAND;   // honoured verbatim
else if (MODE_RANGE.equals(modeParam))    mode = MODE_RANGE;      // honoured verbatim
else                                      mode = hasAnyAgeBand(request) ? AGE_BAND : RANGE;
```

**An explicit parameter always wins and is never second-guessed.** Derivation happens only when no
`mode` is present at all — which is exactly the new form, because the hidden `mode` field was removed
from it. That removal is what makes adding the first age band a *transition* rather than a resubmit
locked to the tier the page opened in.

| URL | Before | After |
|---|---|---|
| `?mode=RANGE&countyFips=…&headcount=3` | range | **identical** |
| `?mode=AGE_BAND&countyFips=…` | age-band form, one starter row | **identical** — `mode=AGE_BAND` still seeds row 1, so the hub card works even with JavaScript off, where "+ Add age band" cannot help |
| `?mode=AGE_BAND&affordabilityBasis=FPL` (T59's card) | age-band + affordability preselected | **identical** |
| proposal hand-off (`mode=AGE_BAND`, `age1..6`) | — | **identical**, and the hand-off loop is untouched at `end="6"` |
| form submit (no `mode`) | n/a | derived from whether any `ageN` is non-blank |

`hasAnyAgeBand` is bounded by the same `AGE_BAND_ROWS` the parse loop uses, so the two cannot disagree
about how many rows exist.

## What changed, and why it is one thing rather than three

Kevin: *"steps 1, 2 and 3 are really 3 versions of the same thing — the only difference is level of
detail available."* **The code already agreed** — all three hub cards were `/Illustration` with a
different `mode`. The toggle presented two tools where there was one, and the form now grows instead of
switching.

⭐ **This dissolved T59.** *"Affordability has no URL of its own"* was logged as a defect. It was never
one: affordability is a **section that appears when a basis is chosen**, not a step with an address. The
defect existed only inside the three-step framing, and **the framing was the error**. The
`&affordabilityBasis=FPL` deep-link shipped in `e849dac` remains correct and useful — it just was not
the resolution of a defect. Re-closed on that reasoning.

**W13-R is fixed by construction.** The toggle dropped the ZIP, and after prompt J dropped the county
too. There is now no toggle to lose state across — a structural fix rather than another parameter
appended to a link.

## Shipped

| Hash | What |
|---|---|
| `2445edb` | The progressive form + **W15** |
| `8ef8c65` | **M2** full-width slider track, **M4** edge-to-edge cards |
| `d0d0d64` | §4 decision block, T59 dissolved, click-script K1–K9 + M8–M10, T97/T98 |

`./mvnw compile` clean before each commit.

## Anchors

All single-occurrence, verified before editing. The one ambiguity from prompt J
(`<c:forEach begin="1" end="6" var="i">`, twice) did not recur — the form's loop is now
`end="${ageBandMaxRows}"` and the hand-off's is still literal `end="6"`, so they are textually distinct.

| Change | Anchor | Line |
|---|---|---|
| Toggle removal | `<div class="ms-auto d-flex gap-1">` | 184 |
| Hidden mode field | `<input type="hidden" name="mode"` | 239 |
| Form branch merge | `<c:when test="${mode == 'AGE_BAND'}">` | 303 |
| Headcount | `id="headcount" name="headcount"` | 410 |
| Count placeholder | `placeholder="1"` | 347 |
| Income placeholder | `placeholder="Annual"` | 352 |
| Slider row (M2) | `.contrib-slider-wrap { position` | 98 |
| Mode derivation | `MODE_AGE_BAND.equals(request.getParameter` | 94 |

## W15 — every placeholder after this run

| Field | Hint | Could it be mistaken for a value? |
|---|---|---|
| ZIP | `#####` | No — not a ZIP |
| Income | `$/yr` | No — a unit, not an amount |
| **Count** | **none — a real default of `1`** | **No, and this is the important one** |
| Eligible Employees, Contribution, Age | none | — |

**Count needed more than a placeholder.** It showed a grey `1` that read as an entry (*"placeholder was
showing a 1, I thought it was an entry"*, twice), **and** a blank field silently computed as 1 — the
walk's URL carried `count1=` empty while the result assumed one life. **Entered and assumed were
indistinguishable.** It now carries a real, black, submitted `1`, so the value in the box is the value
that counts. The servlet's blank-defaults-to-1 parse is untouched and still covers a hand-edited URL.

## Decisions made

1. **Contribution and basis appear with the first band.** They mean nothing without a band to apply them
   to, and showing dead inputs at tier 1 is the same confusion in a new place. Hidden, never removed.
2. **Eligible Employees hides when a band exists** rather than sitting alongside it. Two competing
   headcounts on one form is exactly the ambiguity W15 is about.
3. **The add affordance is accented, not muted.** If an agent cannot see that more detail is available,
   the tiering is invisible and this run achieved nothing — the same failure as the slider nobody
   noticed. The note beside it says what it *does*, not what to *use*: no steering toward a fidelity.
4. **A hidden template row** backs the zero-band case, since the add button previously cloned the last
   row and there is now no last row. Different class, unnamed inputs — it can neither be counted nor
   submitted.
5. **`mode=AGE_BAND` still seeds a starter row.** Without it the hub's card would land on a form with no
   band and no way to add one when JavaScript is off.

## New assumptions, and reversal cost

| Assumption | Reversal cost |
|---|---|
| Deriving `mode` from the presence of age bands matches intent | **Free** — restore the hidden field. ⚠️ The risk is a stale bookmark with `ageN` but no `mode` now resolving to AGE_BAND where it once gave a range. No such URL is emitted anywhere; K6 checks the explicit case |
| Zero bands is the right default opening state | **Free** — one EL condition. If agents expect a row, `mode=AGE_BAND` already gives one |
| A real `1` in Count beats a placeholder | **Free**, and the asymmetry favours it: a wrong-but-visible default is correctable, a right-but-invisible one is not |
| Hiding Eligible Employees at tier 2 is clearer than disabling it | **Free** — one line in `syncTier()` |

## Fenced behaviours: verified and not

**Verified (structure and greps only):** every query parameter name unchanged (`age${i}`, `count${i}`,
`income${i}`, `headcount`, `contribution`, `affordabilityBasis`, `countyFips`, `zip`, `planYear`); the
proposal hand-off loop still `end="6"`; all three banners present in both branches; JSTL and `<div>`
balance (73/73) after every edit; one `<form>`; `./mvnw compile` clean; all placeholders reviewed.

**Not verified — all behavioural, and this is the largest structural change the form has taken:**
`?countyFips=` pre-select · the three ZIP states · county-ZIP precedence · results clearing · ZIP
logging · ZIP surviving the chooser click · unavailable-county wording · disabled proposal link · the
collapsed summary and Edit · repeater renumbering · income visibility · both popovers · **the slider,
its ticks and verdict flipping** · `illustration_log` writing · mobile.

⚠️ **Three of those now sit inside markup this run restructured** — the repeater, the tier fields and the
collapsed summary all read the same DOM the new `syncTier()` manipulates. **The fence is not verified
until K1–K9, L1–L10 and M1–M10 run.**

## Judged too risky, logged instead

- **T97 — the chat bubble covering the affordability verdict column on mobile.** The most consequential
  cell on the page, obscured. **The fix belongs to the shared widget**, so it cannot be done from inside
  the ICHRA fence — and it must not be worked around locally, because page-specific padding on one
  surface would drift the moment the widget moves.
- **T98 — Agent Pipeline on mobile.** Outside ICHRA entirely; recorded, not investigated.
- **T96 — the six-row cap** stays, unchanged and untouched, for the reason logged in prompt J:
  `proposalBuilder.jsp` echoes exactly six pairs and sits outside the fence.
- **The hub cards** were not touched. Prompt L.

## SQL close-out audit

**This run was forbidden from producing SQL and produced none.** No `.sql` file created, modified or
deleted; no migration; no schema change. `ls docs/migrations/` unchanged at **V085**. The four commits
list one `.java`, one `.jsp` and two `.md` paths.

## Next

1. **Walk K1–K9 first**, then L and M. **K4** (the tier transition, with ZIP and county surviving) and
   **K5/K6** (old `mode=` URLs landing unchanged) are the two that matter — they are the contract.
2. **Prompt L** — consolidate the hub's cards now that the surface behind them is one thing.
3. **Rebuild the WAR.** The artefact from earlier today predates prompts G through K.
4. Unchanged: **T76** (seam ready), T89, T96, the three SWBD emails, T83.

**Carried forward, still unclosed:** nobody has asked Forrest what he would want a quoting tool to do,
and the two SWBD emails have still never been sent.
