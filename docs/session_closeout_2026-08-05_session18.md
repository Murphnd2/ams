# Session 18 close-out — proposal render/tier audit, ProposalDetail Phase A spec, V089 built and verified on production, S17 handoff answered

**Session:** 18 (sub-runs S18-A through S18-E) · **Date:** 2026-08-05 · **Branch:**
`refactor/modernize-architecture` · **Documentation only this run (S18-E). No source file edited.**

This session answered the three questions S17-B's handoff (§10) scoped for a dedicated future
proposal session: (a) one section model or two, (b) staleness, (c) what `ProposalDetail` should
show. It also built and production-verified the first concrete step toward the answer to (a)/(c):
a flag column and a scope predicate that, together, let a future enhancement be withheld from
public proposal rendering without touching the LOS-scoping path every other line of service depends
on.

---

## 1. Shipped

Commits read from `git log 36efe0e..HEAD` this run — two, both real hashes from this run's own
`git log`, none carried from a prompt:

- **`a4d0150`** — `docs: S18-C -- Phase A spec, ProposalDetail section rendering + flagged-enhancement predicate`
- **`3968c4b`** — `feat: S18-D -- V089 enhancement.system_managed + flagged-enhancement scope predicate`

`3968c4b` is the session's only source-touching commit: the V089 migration, `Enhancement.systemManaged`,
`FlaggedEnhancementResolver` (new class), and the single `&&`-predicate appended to `ViewProposal.java`'s
`SCOPED`-section enhancement-matching loop, plus the two migration-registry files.

---

## 2. In flight

**None.** The working tree was clean at this run's own preflight (`git status --short` returned
empty) — every line of code and every doc edit from S18-C and S18-D is already committed. Outside
this repository, per the session narrative: Kevin applied V089 to production directly (not through
this session's git history — the migration file itself was already committed in `3968c4b`) and ran
two manual `UPDATE enhancement SET system_managed = ...` statements against production as test steps
for the two verification walks (§ below). Neither of those `UPDATE`s is a migration and neither is
committed anywhere — see §8.

---

## 3. Decisions made

Recorded as Kevin's design decisions this session, each mapped to what it closes.

1. **The proposal is a point-in-time document.** ICHRA plus-tier data is captured during the
   proposal-build phase and frozen, like prospect/agent/LOS/rate data already is. Staleness is
   disclosed, never recomputed. **Closes S17 handoff (b) / T162** — decided, not yet built. See the
   T162 backlog update below.
2. **One section model, not two.** A frozen payload is `ICHRA_ILLUSTRATION`'s semantics; `MARKET`'s
   live recompute contradicts it directly, so the two do not converge — `MARKET` is **retired**.
   It is a singleton PSP row and no installation currently has one created. **Closes S17 handoff
   (a) / T158** — decided, not yet built; `MARKET`'s code (constant, `resolveMarketPage`,
   `proposalMarket.jsp`) is still present as of this session.
3. **ICHRA data stored as JSON in a text field**, so the payload can evolve without migrations. It
   is a superset of today's snapshot columns — affordability needs an on-exchange LCSP figure and an
   income basis, and neither `ProposalIchraSnapshot` nor `ProposalIchraSnapshotBand` has a column for
   either. **Shape and storage location are not decided** — filed as an open question (§5) and as
   backlog row **T165**.
4. **Content is delivered through LOS/enhancement-scoped `CUSTOM` pages with tokens emitting HTML
   blocks**, not through fixed servlet-built sections. Verified feasible this session (S18-C §3.4/
   S18-A Part 3): `replaceTokens` (`ViewProposal.java:876-890`) is plain case-insensitive
   substitution with no escaping and no length constraint in the code as read.
5. **Three special enhancements drive data collection** — group-vs-ICHRA comparison, contribution
   scenarios, affordability. Toggling one on reveals and requires its fields; the fields feed the
   payload; the scoped page renders. Marked by `system_managed`, intended to be hidden from the
   application prompt. **Built this session, partially:** the `system_managed` flag and the
   proposal-*section* predicate exist (`3968c4b`); the application-*prompt* suppression does not —
   see §5's open question on this exact point.
6. **Age banding is a data requirement of those enhancements, not a separate toggle.** Group
   comparison and contribution scenarios both need per-age premiums and per-band lives; `RANGE` mode
   stores neither (confirmed S17-A Q3).
7. **Page 7 (plan landscape) is mandatory**, sourced from a live HealthSherpa call at proposal
   creation and serialized into the payload. No plan cache is needed — see the reversal in §6, and
   backlog row **T166**.
8. **`ProposalDetail` gains section rendering.** **Reopens and answers S17 handoff (c) with yes.** It
   is the only authenticated surface where a PSP admin can preview staging-sourced figures; `G5`
   stays untouched on the public path. Specced in full (`docs/analysis/S18C_proposal_detail_render_spec.md`
   §3), not built — build order steps 5-6, backlog row **T164**.
9. **Duplicate the section pipeline into `ProposalDetail`; do not extract it.** Per S18-C §1.3 —
   extraction's blast radius is every line of service (four ordering-sensitive filter stages, an
   EM-lifetime invariant enforced only by a comment, no test coverage); duplication's blast radius is
   the one new internal page. Mirrors the precedent already set by `isIchraDemoOverride`'s deliberate
   triple implementation.

---

## 4. New assumptions

Each with its reversal cost, as required.

- **Tokens can carry large HTML blocks.** Code-verified (`replaceTokens`'s regex has no size limit
  and Java's `String.replaceAll` imposes none in practice) but **never runtime-verified with an
  actual large block** — the only tokens exercised in production today are short strings (names,
  dates, dollar figures) and short-to-medium hand-authored `CUSTOM` HTML. **Reversal cost: low if
  wrong** — if a large payload-driven block turns out to hit a real limit (request size, JSP buffer,
  browser rendering), the token mechanism itself doesn't change; only how much content one token is
  asked to carry would need to shrink or split.
- **The HealthSherpa plan list's completeness and pagination are unknown at scale.**
  `HealthSherpaService` pages at `PER_PAGE=100`, `MAX_PAGES=20` (2,000-plan safety cap, treated as a
  failure if hit) — verified in code, never exercised against a market with more than the reference
  county's 65 plans. **Reversal cost: low to moderate** — the pagination loop already exists and
  works for any count under the cap; a market that actually approached 2,000 plans would need the cap
  raised or the "hit the cap = failure" policy reconsidered, not a new fetch mechanism.
- **Duplicated-pipeline drift risk is accepted and bounded to an authenticated internal page.**
  Per decision 9 — the risk is real (a future entitlement tightening could land on `ViewProposal`
  and not `ProposalDetail`) but is judged acceptable because `ProposalDetail`'s audience already sees
  the proposal's pricing and LOS list unfiltered on that same page today; it is not the audience
  entitlement filtering exists to protect against. **Reversal cost: moderate** — extracting later,
  if a third consumer of the pipeline appears, means confronting the same EM-lifetime and
  ordering-dependency obstacles S18-C §1.2 catalogued, just with two call sites depending on the
  result instead of one.
- **`system_managed` is intended to also suppress the *application* prompt, not only proposal
  rendering** (decision 5), but only the proposal-rendering half is built. **Reversal cost: low** —
  nothing currently reads `system_managed` from `ApplyForProposal.java`, so there is no existing
  behavior to undo; wiring it in is additive. Flagged as an open question, not an assumption
  resolved, in §5.
- **`MARKET`'s retirement (decision 2) is safe to treat as low-risk** because no installation has a
  `MARKET` `proposal_section` row today (per S17-A §3.1's unresolved question, still unresolved by a
  database read this session — see §5). **Reversal cost if the premise is wrong: unknown** — if a
  `MARKET` row does exist somewhere, "retire" becomes "migrate away from a live row," a different and
  more expensive operation than deleting unused code.
- **Three claims this session were asserted from reading and disproved by a grep or a walk**: that
  agency-scoped `CUSTOM` HTML pages exist (they do not — `setAgency()` is gated to `TITLE`/`CLOSING`);
  that POP/FSA/HRA/COBRA/HSA were `SCOPED` enhancements (they are Lines of Service); and that nothing
  reads `los.is_plus_tier` (three surfaces do). All three originated as confident restatements rather
  than verifications. **Standing implication:** where a claim is load-bearing and a grep is cheap,
  grep.

---

## 5. Open questions raised

1. **Payload shape and storage location.** Decision 3 settles *that* it's JSON in a text field; it
   does not settle *which* text field, on which entity, with what internal schema. Filed as **T165**.
2. **Is a live HealthSherpa API call inside the proposal-create request path acceptable** for
   latency and failure handling? Page 7 (decision 7) requires a call at proposal creation, not at a
   scheduled warm time — unlike every existing HealthSherpa call site (`RateCacheWarmService`, a
   background job; `IllustrationServlet`, an explicit agent-initiated action with its own loading
   state). A proposal-create request blocking on an external API call, with `MAX_ATTEMPTS=3` and
   backoff already built into `HealthSherpaService`, could add multiple seconds to a synchronous
   servlet response, or fail the whole creation if HealthSherpa is unreachable. Not decided this
   session; unresolved in **T166**.
3. **Should the three special enhancements also suppress `ApplicationSection` rows scoped to
   them?** Per S18-B Part 4: `ReviewApplication.java:116-136` gates which `ApplicationSection` rows
   render on `application.getSelectedEnhancementIdList()`, and an enhancement absent from that list —
   whether because the applicant declined it or because it was never offered — produces identical
   downstream state. **So the answer is: they will**, automatically, the moment `system_managed`
   enhancements are also filtered out of `ApplyForProposal.java`'s enumeration query
   (`:93-102`), with no extra code needed to make it happen. **Whether that is intended is not
   decided.** It could be exactly right — these three enhancements may exist purely to drive
   proposal-side ICHRA content and have no application-side setup work of their own — or it could be
   a defect if any of the three is later given `ApplicationSection` content that should still appear
   during setup regardless of proposal-page visibility. Recorded here rather than assumed either way.
4. **O25 (carrier names).** Page 7's plan landscape (decision 7, T166) would display `issuerName`
   and plan `name` per `PlanSummary` — both already parsed off the wire per S18-B Part 1 — but
   `proposalMarket.jsp`'s own header comment states the current compliance discipline is "no plan or
   carrier is named, ranked or recommended" (LA-17/LA-04/LA-05). Whatever O25 resolves to gates
   whether page 7 can name a carrier at all; not this session's question to answer, only to flag as
   the dependency it is.
5. **T44 on-exchange LCSP plus the post-V078 re-warm.** `onex_lcsp_premium`/
   `onex_benchmark_silver_premium` (V078) are the *correct* affordability inputs, but per S17-A Q5,
   they are null on any cache row warmed before V078, and whether a post-V078 re-warm has actually run
   anywhere remains unconfirmed by a database read (S17-A §3, item 2, still open). The payload's
   affordability figure (decision 3) is only as good as this column being populated.
6. **Does any `proposal_section` row of type `MARKET` or `ICHRA_ILLUSTRATION` actually exist on any
   installation?** Carried forward, still unresolved by a database read, from S17-A §3 item 1 and
   S18-C §8. Bears directly on decision 2's "no installation currently has one" claim, which rests on
   S11-H's own build-time comment, not an independent count.

---

## 6. Contradictions found

1. **⚠️ The project brief's "agency-scoped custom HTML pages" claim is wrong.** `setAgency()` is
   called exactly once in the codebase (`ProposalSettings.java:294`), hard-gated to `TITLE`/`CLOSING`.
   `CUSTOM` is always PSP-wide. Kevin's workaround: agency-specific LOS rows placed only in that
   agency's rate table, giving branded pages through LOS scoping instead — functional, but costs LOS
   proliferation. Filed as **T167** so the wrong claim stops propagating into future planning.
2. **⚠️ `claude.ai` asserted POP/FSA/HRA/COBRA/HSA were live `SCOPED` enhancements.** They are Lines
   of Service. Caught by S18-C §0 before the build, not after. Had it stood uncaught, S18-D's
   regression walk would have exercised only the untouched LOS branch (`ViewProposal.java:291-298`)
   and proved nothing about the edited enhancement branch — the walk that actually ran used the
   Debit Card Services enhancement instead, the only seeded `Enhancement` row available as a witness.
3. **⚠️ Reversal: page 7 was assessed as requiring a plan-level cache plus a new warm path. S18-B
   disproved this.** `HealthSherpaService.PlanSummary` already parses `hios_id`, `name`,
   `metal_level`, `issuer`, `gross_premium`, `hsa_eligible`, `ichra_only` per plan;
   `RateCacheWarmService` discards `name`, `hsaEligible`, `ichraOnly` after computing its aggregates.
   The data is on the wire today — decision 7 and backlog row T166 are built on this reversal.
4. **The staging demo assumption did not hold.** `ICHRA_DEMO_ALLOW_STAGING_PROPOSAL` + `isPspAdmin`
   gates only the illustration hand-off button and the two `ProposalBuilder` snapshot-write guards;
   the snapshot keeps its real `sourceEnv`, so `G5` refuses it permanently regardless. And sections
   render on exactly one surface in the whole application — the unauthenticated `/proposal/*` —
   confirmed by grepping every include/forward of `proposalIchra.jsp`/`proposalMarket.jsp`/
   `viewProposal.jsp` this session (S18-B Part 2). **This is what makes decision 8 necessary rather
   than optional**: without an authenticated rendering surface, there was and is no way for a PSP
   admin to preview staging-sourced ICHRA content at all.
5. **Prompt fence too narrow: S18-D was told "append registration row only" for
   `migration_tracker.md`**, which excluded the "Current Highest Version" header — a line that
   necessarily goes stale the moment a new version is registered below it. Kevin bumped it manually
   after S18-D's close-out flagged the gap. **Prompt-precision note**, not a defect in S18-D's
   execution: the fence did exactly what it was written to do, and what it was written to do left a
   one-line, easily-missed follow-up. Worth a narrower fence next time only if the header update is
   meant to travel with the same run as the row it describes.
6. **⚠️ V086's tracker description was false.** `los.is_plus_tier` was documented as
   `⚠️ **nothing reads it yet**`, but the column is read on three surfaces: public unauthenticated
   `/proposal/*` (`ViewProposal.java:338`, the T129 entitlement filter via `isPlusTierScoped`, and
   `:452`, S11-H's `resolveMarketPage` plus-tier check), the authenticated Proposal Builder
   (`ProposalBuilder.java:459`, `attachIchraIntakeIfPresent`'s `anyPlusTier` gate), and the
   authenticated Service Manager, which writes it from a checkbox (`ServiceManagerAction.java:125`).
   **Why this mattered enough to fix rather than leave:** the row told a future session the column was
   inert, when in fact it gates what an employer sees on a public proposal link. Discovered by S18-H's
   Task 3 grep, which ran only because a prior run's assumption (S18-G's, that the claim was "still
   accurate") was checked rather than inherited. Corrected in the tracker by S18-I.
7. **The migration tracker's Production column was wrong on two of its four most recent rows.**
   V086 and V088 both read unapplied while both were in fact applied on production. Settled by a
   direct `schema_version` query Kevin ran against production on 2026-08-05 —
   `SELECT version FROM schema_version WHERE version IN ('V086','V087','V088','V089')` returned all
   four — the same class of evidence that confirmed V087 (session 10), and the class V086 and V088
   had never received. **Practice this suggests:** a direct `schema_version` read at each deploy is
   cheap; a future session building on a column the tracker says isn't there — when it actually is —
   is not.

---

## 7. Next

**Recommended: build T163 (the admin UI toggle) before anything else in this thread.**

Reasoning: every other row this session filed (T164, T165, T166) is either a multi-step spec-then-
build (ProposalDetail rendering, the payload) or blocked on an external dependency (T136, production
HealthSherpa access, still open per prior sessions). T163 is neither. It is the same shape as the
`suppressed` toggle already sitting one line away in `ServiceManagerAction.java:215`, requires no
design decision this session left open, and it retires the operational hazard this session's own
walk exposed: T64's newly-added third observation confirms that setting `system_managed` by hand
requires a Tomcat restart to take effect, which is exactly the kind of runbook trap T64 already warns
about for every other hand-edited config column. Shipping the checkbox removes the need for anyone
to touch this column by SQL ever again — the walk that proved the resolver works should be the last
time it's exercised that way.

After that, the payload shape (T165) is the actual gating item for decisions 3, 5, 6, and 7 all at
once — it is worth its own Phase A spec before any of ProposalDetail (T164), the special enhancements'
field-reveal UI, or page 7 (T166) can be built for real, since all four write to or read from
whatever T165 decides.

**T163 has a working precedent twice over, found this session while verifying V086's description.**
`ServiceManagerAction.java:125` + `serviceManager25.jsp:890-892` is checkbox → boolean → entity write
for `plusTier` on the LOS editor, and `ServiceManagerAction.java:215` is the same shape for
`suppressed` on the enhancement editor — the same table T163 must edit. Neither needs to be invented;
T163 is a third instance of a pattern already built twice in the same servlet.

---

## 8. SQL close-out audit

**Every SQL statement produced, run, or recommended this session:**

1. **V089 DDL** (`docs/migrations/V089__enhancement_system_managed.sql`, committed `3968c4b`,
   S18-D) — one `ALTER TABLE enhancement ADD COLUMN system_managed TINYINT(1) NOT NULL DEFAULT 0;`,
   one `CREATE OR REPLACE VIEW schema_info`, one `INSERT IGNORE INTO schema_version`. **In a
   versioned migration — yes.** Written by S18-D, **applied to production** per this session's
   narrative (V089 applied cleanly; all 18 `enhancement` rows defaulted to `0`; no other behaviour
   changed) and **verified** by the two production walks below. This run (S18-E) did not write, run,
   or modify any SQL — documentation only, confirmed by this run's own scope fence and by the fact
   that no `.sql` file appears in this run's edits.
2. **Two manual `UPDATE enhancement SET system_managed = 1 / 0 WHERE enhancement_id = 1` statements**,
   run directly against production by Kevin as the two verification-walk test steps (§ below,
   Runtime verification). **These are not migrations, are not in any versioned script, are not
   committed anywhere, and must not become migrations** — they were a deliberate, temporary flip to
   prove the resolver's flagged branch is actually consulted (walk 2) and then reverted (also walk 2),
   not a durable state change. Recorded here explicitly per instruction, so this fact lives somewhere
   citable rather than only in conversation.

**Orphaned `.sql` files — checked this run, none found inside the tracked repository.** A filesystem
sweep (`find . -name "*.sql"`, excluding `target/`) surfaced SQL files in three places outside
`docs/migrations/`/`docs/importscript/`: `release/` (six files, `V066`-`V071`, all duplicates of
already-registered migrations) and `.claude/worktrees/*/docs/migrations/` (stale mirrors through
`V065` inside two apparently-abandoned isolation worktrees). **Both are git-ignored and untracked**
(`release/` per `.gitignore:69-70`, explicitly documented as "Local release-staging folder... never
committed"; `.claude/worktrees/` per `.gitignore:16`) — neither is an orphaned migration requiring
action, both are pre-existing local/session artifacts unrelated to this session's work. `git ls-files`
confirms neither path is tracked.

**Current highest migration version: V089** (`docs/migrations/V089__enhancement_system_managed.sql`),
unchanged from S18-D — this run created no new migration.

**What is pending deployment:** nothing from this session — V089 is already applied to production,
per the narrative, and this run built nothing further. **What is *not* pending deployment but *is*
pending documentation, and could not be fixed this run:** `docs/analysis/migration_tracker.md`'s
Production column for the V089 row still reads unapplied (⬜), because that file is outside this
run's scope fence (permitted only for "new rows and status updates" in `project_backlog.md`, not
`migration_tracker.md`). **Flagged explicitly rather than silently left** — this is precisely the
kind of drift `CLAUDE.md`'s migration-tracker maintenance note warns against, and a future run (or
Kevin directly) needs to flip that cell now that production has actually received V089.

**Schema described but not scripted:** the ICHRA JSON payload (decision 3, T165) is a described data
shape with no column, no table, and no migration — deliberately, per decision 3's own "shape and
storage location are not decided."

---

## 9. Compliance statement

**Preflight output:**
```
$ git rev-parse --abbrev-ref HEAD
refactor/modernize-architecture

$ git log -1 --oneline
3968c4b feat: S18-D -- V089 enhancement.system_managed + flagged-enhancement scope predicate

$ git status --short
(empty)

$ git pull --ff-only
Already up to date.
```
**Hard stop: did not fire.** Branch correct; pull reported "Already up to date."; tree was already
clean, not merely non-blocking-dirty.

**T-number discipline — high-water mark and pre-assignment `grep -c` output, as required:**
```
$ grep -rn "T1[5-9][0-9]" docs/ | grep -oE "T1[5-9][0-9]" | sort -u -V
T150 T151 T152 T153 T154 T155 T156 T157 T158 T159 T160 T161 T162
```
High-water mark: **T162**. Candidates checked before assignment:
```
$ for n in T163 T164 T165 T166 T167; do printf "%s: " "$n"; grep -rc "$n" docs/ | grep -v ":0" | wc -l; done
T163: 0
T164: 0
T165: 0
T166: 0
T167: 0
```
All five confirmed unused before assignment. Re-verified after writing, each appears exactly once:
```
$ for n in T163 T164 T165 T166 T167; do printf "%s: " "$n"; grep -c "| $n |" docs/analysis/project_backlog.md; done
T163: 1  T164: 1  T165: 1  T166: 1  T167: 1
```

**Paths written this run — exactly two:**
1. `docs/session_closeout_2026-08-05_session18.md` (this file, new)
2. `docs/analysis/project_backlog.md` (five new rows T163-T167; status/notes updates to T158, T162,
   T64)

**No `.java`, `.jsp`, `.sql`, or `.properties` file was touched this run.** No file under
`docs/migrations/` was touched. `docs/analysis/S18C_proposal_detail_render_spec.md`,
`docs/ichra_strategy.md`, `docs/swbd_ichra_build_plan.md`, and every prior close-out were read-only
or not read at all (only the S18-C spec and the S17-B close-out §10 were actually read, per the
required reading list).

**No git mutation ran.** No `add`, `commit`, `stash`, `checkout`, `restore`, `reset`, `rebase`,
`tag`, `branch`, push, or force-push. The only git commands executed were the four read-only
preflight commands and the `git log 36efe0e..HEAD` reads used to source §1's hashes.

**Code-verified vs. runtime-verified disclosure:**
- **Code-verified, by this session's own runs (S18-A/B/C/D):** the rate-cache shape and
  `PlanSummary` field parsing (S18-B Part 1), the T150 override's exact gates (S18-B Part 2), the
  `Enhancement` entity and pricing mechanism (S18-B Part 3), the application-prompt enumeration
  (S18-B Part 4), the entire S18-C spec, and S18-D's build (compiled, `BUILD SUCCESS`, diff verified
  as exactly the six permitted paths).
- **Runtime-verified, by Kevin, on production, reported in this session's narrative and not
  independently re-verified by this run:** both walks (witness proposal
  `78778d5f-d749-47b6-99a5-20a765107fa4`, walk 1 — no regression across all six sections including
  the enhancement-scoped one; walk 2 — flag consulted, section disappears/reappears with a restart
  each way), V089's clean production application, and the third `T64` observation (raw `UPDATE`
  invisible until restart). **This run (S18-E) performed no runtime verification of its own** — it
  is documentation-only by its own scope fence, and every runtime claim in this close-out traces to
  the session narrative supplied for this run, not to a database query or a page render this run
  performed. No SQL was run, no server was reached, no page was fetched by this run.
- **Not verified by anything, this session or prior:** whether any `proposal_section` row of type
  `MARKET` or `ICHRA_ILLUSTRATION` exists on any installation (§5 item 6); whether the post-V078
  re-warm has run anywhere (§5 item 5).
