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
