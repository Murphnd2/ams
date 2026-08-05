# Session 17 close-out — proposal render audit, three findings filed, T158 reframed, handoff scoped

**Date:** 2026-08-05 · **Branch:** `refactor/modernize-architecture` · **Model:** Opus (S17-A) / Sonnet (S17-B)
**S17-A was a read-only audit. S17-B (this run) is documentation only. No source file was touched
by either run. No build ran, no `.war` was produced, no migration was written.**

> This close-out does not record its own commit hash — the standing convention since S16-D, kept
> here for the same reason: a second commit purely to record a hash adds nothing a reader needs.

---

## 1. Shipped

Two commits this session, both docs-only. Hashes read from `git log` this run, not carried from
either prompt.

| Commit | Run | Paths touched |
|---|---|---|
| `69d8e37509635467dd1610a29ccd6957f33aa7df` | S17-A | `docs/analysis/proposal_render_and_tier_audit.md` (new) |
| *(this run's commit — hash not recorded here, see the standing convention above)* | S17-B | `docs/analysis/project_backlog.md` (three new rows + T158 rewrite), `docs/session_closeout_2026-08-05_session17.md` (new) |

**No code shipped this session.** Stated explicitly per the run brief: zero `.java`, `.jsp`, or
`.sql` files were touched by either S17-A or S17-B. Preflight HEAD for this run was `69d8e37`
(S17-A's own commit, confirmed clean, confirmed already up to date with remote before this run
began).

---

## 2. What the session established

S17-A's five answered questions, one line each — cited from `docs/analysis/proposal_render_and_tier_audit.md`, not restated:

1. **Q1 (snapshot or live?):** Both, but never in the same section — `ICHRA_ILLUSTRATION` is
   snapshot-only (reads `ProposalIchraSnapshot`, gate G5), `MARKET` is live-only (reads intake +
   rate cache every render, gate G6). §1 of the audit.
2. **Q2 (is the illustration hand-off the only writer?):** Confirmed — `attachRangeSnapshot`/
   `attachAgeBandSnapshot` are the sole writers of `ProposalIchraSnapshot`; `attachIchraIntakeIfPresent`
   is a separate writer of a separate table. §2.
3. **Q3 (does AGE_BAND work end-to-end?):** Bands persist correctly, but the two modes store
   disjoint field sets — RANGE writes `headcount`, AGE_BAND does not (this run's T160). §3.
4. **Q4 (does an internal gated view exist?):** `ProposalDetail` exists, is authenticated, and
   **renders no sections whatsoever** — exactly four attributes set (`proposal`, `pricing`,
   `proposalLink`, `canEditMarkup`), zero grep matches for section/snapshot/market/sourceEnv/intake
   anywhere in the servlet or its JSP. §4.
5. **Q5 (what does each tier make computable?):** A fidelity ladder from county-only through
   affordability-basis, with each rung's added computability named against what data it needs. §5.

---

## 3. ⚠️ Contradictions found

**The important one is a `claude.ai` claim, not a repo claim, and it shaped the whole investigation's framing.**

At some point in this engagement's history, a `claude.ai` conversation asserted that
proposal-builder-first "produces a proposal with no ICHRA figures at all" — and that claim appeared
both in conversation and in a wireframe handed to Kevin. **S17-A disproves it directly:** the
proposal-builder-first flow produces a proposal with a **different** ICHRA section — `MARKET`, not
`ICHRA_ILLUSTRATION` — not *no* section. `attachIchraIntakeIfPresent` writes a `ProposalIchraIntake`
row whenever the flow's own gates are satisfied (entitled, plus-tier LOS, `intakeZip` present), and
`resolveMarketPage` renders from it live. The confusion is understandable — `MARKET` is currently
invisible in practice on this installation, because every warmed county is staging-sourced and G6
requires `PRODUCTION_OK` — but invisible-today and does-not-exist-in-the-design are different
claims, and the wrong one was the one that circulated.

**Recorded because an unrecorded correction to a confident wrong claim is how the wrong claim
survives** — the next session that reads a stale wireframe or an old conversation thread without
this note would reasonably re-derive the same wrong premise S17-A was commissioned to check.

**Also recorded here:** Q4's hoped-for option — an internal-only gate that could be opened to give
an agent a safe, authenticated preview of ICHRA figures ahead of a staging-data client demo — **does
not exist**. `ProposalDetail` has no section-rendering capability of any kind to gate. This closes
off what would have been the cleanest path to a safe staging demo without touching the public,
unauthenticated proposal page's LA-17 posture.

**No other contradictions found.** S17-A's remaining four answers held against this run's own
independent re-reading of `ViewProposal.java` and `ProposalBuilder.java` (done in the course of
confirming F3 — see §6 below); nothing else in the audit disagreed with the code as read this run.

---

## 4. Decisions made

- **Filed F1, F2, and F3 as backlog rows T160, T161, T162** (`docs/analysis/project_backlog.md`) —
  see §6 for detail. Per the standing rule established in S16-G: filing a T-number means writing the
  row in the same run that discovers or confirms it, not describing it in a close-out to be filed
  later.
- **Reframed T158** — appended the correction (kept the original text, per the convention S16-B used
  on T142) rather than rewriting or deleting it. The correction: T158 is not one section fed two
  ways, it is two structurally distinct section types (`ICHRA_ILLUSTRATION` vs `MARKET`), and which
  one an employer sees depends on the flow the agent used. The namespace divergence T158 originally
  described is a symptom of that two-flow, two-table design, not the underlying problem.
- **Explicitly decided NOT to pursue a staging-data proposal demo beyond what T150 already ships.**
  Both section types are blocked by the identical staging condition (G5 refuses a staging-stamped
  snapshot; G6 requires `PRODUCTION_OK`), and only **T136** (production HealthSherpa access) unblocks
  either — there is no code-level workaround left to build that doesn't re-open the session-10
  decision T150 already relitigated once, deliberately, under narrow conditions. **Agent-facing
  surfaces already demo today with honest banners** (T137/T138/T150's own walk) — that is the
  standing answer for "can Forrest see current numbers," and this session adds nothing to it.

---

## 5. New assumptions

**Asked directly: does anything this session records make a failure mode worse, even where it
confirms a working path?**

**None.** Both S17-A and S17-B were read-only/docs-only — no code changed, no gate loosened or
tightened, no behavior changed for any user. T160, T161, and T162 describe **pre-existing**
conditions the audit exposed, not conditions this session created:

- T160 (missing `headcount` on AGE_BAND snapshots) has existed since the AGE_BAND snapshot writer
  was built (S16-D) and has never run on any installation.
- T161 (silent contribution-guard drop) has the same origin and the same never-run status.
- T162 (staleness divergence between the two section types) is inherent to `MARKET`'s live-recompute
  design, which predates this session by several sessions (S11-H) and was a deliberate design choice
  at the time, not a defect introduced now.

Reversal cost for each: none of the three findings closes off any future direction — all three fix
shapes (set `headcount`, surface a warning, decide MARKET's staleness policy) remain equally
available whenever a future session picks them up.

---

## 6. Open questions raised

1. **T158's reframed decision — Kevin's, for a dedicated future session.** Converge
   `ICHRA_ILLUSTRATION` and `MARKET` into one section type, or keep them structurally distinct with
   an explicit, documented flow-to-section rule? Not specced or decided in this run.
2. **T162 / F3's design question — also Kevin's.** Should `MARKET` snapshot at proposal-creation
   time (matching `ICHRA_ILLUSTRATION`'s point-in-time semantics), carry a visible "may change since
   you last viewed this" disclosure alongside its existing as-of date, or stay live by deliberate
   design? Recorded as a design question, not only a defect, because "stay live" has a real argument
   in its favor (a corrected cache row should propagate to an already-sent proposal, not perpetuate
   a stale error).
3. **The affordability-input problem, unresolved since T44/V078** — `computeAffordability` correctly
   reads `getOnexLcspPremium()` (on-exchange), never the off-exchange figure that understates true
   LCSP by roughly 44%, but the broader question of when off-exchange vs. on-exchange pricing should
   drive which displayed figure across the illustration surfaces remains open and is unchanged by
   this session.

None of these three is scoped, sized, or sequenced here — that is explicitly Step 4's job, not this
section's.

---

## 7. SQL close-out audit

**No SQL was produced or run this session, by either S17-A or S17-B.** No migration, no DDL, no DML,
no `INSERT INTO constant`. `docs/migrations/` was read-only in both runs' scope fences and was not
written.

**Highest migration read from `docs/migrations/` this run: `V088__proposal_ichra_intake_contribution.sql`.**
Confirmed by listing the directory this run (`ls docs/migrations/`) — unchanged from every prior
session's reading back through S14.

---

## 8. Compliance statement

**Scope fence held exactly, for both runs.** S17-A's write access was
`docs/analysis/proposal_render_and_tier_audit.md` only. S17-B's (this run's) write access was
`docs/analysis/project_backlog.md` and `docs/session_closeout_2026-08-05_session17.md` — both, and
nothing else.

**`git diff --name-only` for this run's commit shows exactly two paths:**
`docs/analysis/project_backlog.md`, `docs/session_closeout_2026-08-05_session17.md`.

**No source file was touched by either run.** No `.java`, `.jsp`, `.sql` anywhere in the diff for
either commit. `docs/migrations/`, `docs/ichra_strategy.md`, `docs/swbd_ichra_build_plan.md`,
`docs/analysis/proposal_render_and_tier_audit.md`, and every existing close-out were read-only or
untouched by this run, per its fence.

**T-numbers assigned:** T160, T161, T162. Each confirmed unused before assignment —
`grep -c "T160" docs/analysis/project_backlog.md`, `grep -c "T161" ...`, and `grep -c "T162" ...`
each returned `0` prior to this run's edit; `grep -rn "T1[5-9][0-9]" docs/` was run first to confirm
T159 was the standing high-water mark. All four greps were run this run, output shown in the
transcript above this document.

**Hard stops:** the run brief specified one — branch wrong, tree dirty, or a non-clean pull. **It did
not fire.** Branch was `refactor/modernize-architecture`, the tree was clean at preflight, and
`git pull --ff-only` reported *"Already up to date."*

**Every hash in this document was read from `git log` this run.** No forbidden git operation ran —
no `add -A`, `add .`, `add -u`, `stash`, `checkout`, `restore`, `reset`, `rebase`, `tag`, `branch`, or
force-push. **No SQL was produced or run.** **Commit count: one** (this run's; S17-A's commit,
`69d8e37`, was made in the prior run and is cited, not repeated).

---

## 9. Next

See §10 below (Step 4's handoff) for the scoped question list for a dedicated future proposal
session. Outside that:

1. **T157** remains cheap and available to ride with T136 or be taken alone (apply T137's
   fail-toward-labeling pattern to the intake county chooser).
2. **T159** (the `/GroupConversion` headline mislabel) remains a small, isolated JSP-only fix,
   unrelated to this session's ICHRA findings.
3. **T160/T161** are both small, isolated fixes in `ProposalBuilder.attachAgeBandSnapshot` and can
   ship independently of the larger T158/T162 design decisions — neither requires resolving the
   converge-vs-distinct question first.

---

## 10. Handoff — scope for a dedicated future proposal session (questions only, not a plan)

This section is a question list for whoever picks up proposal detail/content/staleness as its own
session. **Nothing below is specced, sized, or sequenced** — that is explicitly this future session's
first job, not this close-out's.

**(a) One section or two?** Converge `ICHRA_ILLUSTRATION` and `MARKET` into a single section type, or
keep them structurally distinct with an explicit, documented flow-to-section rule (T158)? What would
"converge" even mean given they write to different tables today (`proposal_ichra_snapshot` vs.
`proposal_ichra_intake`) with different completeness guarantees?

**(b) Staleness (T162/F3).** Should `MARKET` snapshot at proposal-creation time, carry a visible
as-of / "may have changed" disclosure, or stay live by deliberate design? Does the answer differ for
a proposal already sent vs. one still being edited?

**(c) What should `ProposalDetail` show?** It currently renders header/LOS/link/pricing only, with no
section-rendering capability at all (S17-A Q4). Is an agent-facing preview — "see this proposal
exactly as the employer will" — a wanted feature, or is the public link itself sufficient for that
purpose? This is an open product question, not a given requirement.

**(d) Which optional sections are worth building, and at what fidelity tier is each computable?**
S17-A's Q5 fidelity ladder (§5 of the audit) is the direct input here — county-only, +age-bands,
+contribution, +affordability-basis each unlock different computable content.

**(e) Custom content pages.** Static, agency-scoped HTML already exists as a proposal extension point
requiring no data at all. How much of a proposal's ICHRA story should live there (hand-authored,
agency-branded) versus in computed sections (`ICHRA_ILLUSTRATION`/`MARKET`)? Is there a middle
ground — a computed section that's editable per-agency?

**Context, not a work item:** Kevin holds a nine-section ICHRA proposal wireframe (cover,
how-ICHRA-works, market section, administration/pricing spine, plus optional cost-comparison /
contribution-scenarios / plan-landscape / affordability / next-steps sections, each annotated with
its gate) that is **not in this repository** — it lives in Kevin's own project knowledge. A future
session that wants it should **ask for it directly rather than attempt to reconstruct it** from
close-out prose; no close-out, including this one, has ever transcribed its contents.
