# Session Close-out — 2026-07-31, Session 3

**Branch:** `refactor/modernize-architecture` · **Pushed through:** `682bc8f`
**Scope:** Item 11 (A4a group-to-ICHRA conversion analysis) → item 13 (opportunity attribution, V081) →
a retraction of item 13's own justification → the console read that made item 13 worth having → this
close-out.

**With items 1–13 struck through, the ICHRA build sequence in `swbd_ichra_build_plan.md` §3 is
complete.** Item 4 was never a build item (Kevin's reference-data entry via the admin UI) and item 10 is
`Content done` pending Sequence Builder entry. What remains is Kevin's list, not a build queue.

---

## Shipped

| Commit | What |
|---|---|
| `7db188d` | **Item 11: group-to-ICHRA conversion analysis (A4a).** New `GroupConversionServlet` (`/GroupConversion`, POST-computed) + `groupConversion25.jsp`, mirroring item 9's AGE_BAND shape — same county/plan-year selection, same repeating (age, count) census rows, same `RateCacheDAO.getRate`/`lowest_bronze_premium` read, same `IchraAccessResolver` gate. Adds current group premium + a flat proposed contribution; outputs employer-level delta and a per-employee net-position table. Hub card converted from `Coming` to live. |
| `5f98c53` | Record item 11's hash in the build plan; correct its **Depends on** row (the Forrest email gates a compelling *demo*, not the build) and its **Attaches at** row (records that item 9 is not a separate servlet). |
| `a71b79d` | **Item 13: optional opportunity attribution — V081.** Nullable `illustration_log.opportunity_id`, FK to `assignee(id)`, `ON DELETE SET NULL`. Both ICHRA servlets capture and scope-validate an optional `opportunityId` via `OpportunityAuthz` and persist null on any failure. No picker, no new surface. |
| `ae904a3` | Record item 13's hash; **resolve S3** (all six structural decisions now settled); note the console read as deferred for want of an extension point. |
| `cf15ff8` | **V081 header correction** — the `ON DELETE SET NULL` justification asserted a live failure that no code path produces. Comment only; no SQL changed. See Contradictions. |
| `619461f` | **The console read.** `IllustrationLogDAO.findByOpportunityId` (one query, newest first, capped at 25) + `/IchraOpportunityAnalyses`, a JSON feed the agent pipeline drawer fetches. Renders date, kind and agent — nothing else. |
| `682bc8f` | Record the console read in the build plan; replace the "not built" paragraph; propagate `cf15ff8`'s correction into item 13's Schema row, where the same overstated claim had been repeated. |

Every hash above was read from `git log`, not carried over. The list matches the seven this run was
given, in the order given.

## In flight

**None. The working tree is clean.** `git status --short` returns nothing — verified directly
immediately before writing this file, and again after it was committed.

## Decisions made

1. **S3 resolved — yes, nullable, opportunity-level only.** On Kevin's confirmation that a pipeline
   console exists (`AgentHome` / "Agent Pipeline") and should stay functional against these. Closes item
   13 and, with it, the last open structural decision — §4 now reads *all six resolved*. Not prospect,
   not employer, nothing person-shaped: **LA-11 holds untouched**, because an opportunity id is a
   pipeline record and adds no field to the design census.
2. **`ON DELETE SET NULL` on `fk_illustration_log_opportunity`.** The decision is unchanged and correct.
   Its stated justification was not, and was retracted the same session — recorded under Contradictions
   rather than buried here, because the failure was a documentation failure and belongs where it is
   visible.
3. **Item 11 logs a `CONVERSION` row to `illustration_log`.** `mode` is `VARCHAR(16)` with no `ENUM` and
   no `CHECK` (V075:45), so a new value cost no schema. Kept deliberately as telemetry: whether agents
   run these tools at all is the *precondition* of the falsification test in `ichra_strategy.md` §1 —
   see the note under Contradictions, because the test as written is narrower than "do they use it".
4. **Item 11 carries no proposal hand-off and no export path**, unlike item 9's page, which has a "Use
   this in a proposal" button. **D24: the output goes to the agent, never to the employer.** A
   conversion analysis names an employer's current premium and its employees' payroll deductions; the
   hand-off was deliberately not carried over rather than accidentally omitted.
5. **The console read is a JSON endpoint the drawer fetches, not a widening of `OPPS`.** The
   pre-serialized map at `agentHome25.jsp:738-768` is rendered server-side for every agent on every page
   load; putting ICHRA data there would ship it to agents outside the gate and grow a shared surface's
   diff. Fetching from a gated endpoint made the rule-2 guarantee structural instead of conditional —
   **an empty array renders nothing at all**, so an agent without ICHRA sees the drawer exactly as
   before. The JSP diff is 69 insertions, 0 deletions.
6. **`result_summary` is deliberately not exposed on the drawer.** It can carry premium figures
   (`"bronze floor 421.57-1368.03"`, `"employer 4200.00 -> 6020.00"`), and a shared sales surface is the
   wrong place to settle who may see those. v1 returns date, kind and agent — no premium, no county, no
   headcount, no plan year, no figure of any kind. Widening it is a decision, not a convenience.

## New assumptions (technical, not legal — **none is an `LA-NN` entry**; the register still ends at LA-13)

- **Nothing in AMS read `illustration_log`** from V075 until `619461f` this session. Three writers
  existed before any reader did — `IllustrationServlet`, `GroupConversionServlet`, and the entity
  itself; `RateCacheAdmin`'s two hits are javadoc stating its diagnostic does *not* write there.
  Reversal cost: none incurred — but it means every column's meaning has been asserted by its writers
  and never checked by a reader, and the first reader arrived this session.
- **No delete path for an `Opportunity`, `Activity` or `Assignee` row exists anywhere in AMS**, verified
  by grep over `src/main/java` on 2026-07-31 — every `em.remove` call site targets some other entity.
  This is the fact that made the original `ON DELETE` justification wrong. Reversal cost: none; but the
  assumption is *fragile by nature* — it is a statement about absence, and one new admin servlet
  falsifies it silently.
- **`IllustrationLogDAO` is explicitly not an authorization boundary.** Its javadoc says so and says
  every caller must check `OpportunityAuthz.canAccessOpportunity` first, as `IchraOpportunityAnalyses`
  does. It is now **the one reusable read path into the table, and A5 will be the next caller** — so
  this is an assumption with a known future consumer, not a hypothetical. Reversal cost: low now (one
  caller); rises with each caller that forgets.
- **Item 9 is not a separate servlet.** It is an `affordabilityBasis` sub-mode inside
  `IllustrationServlet`'s `AGE_BAND` path, rendered in `illustration25.jsp:263-321` — verified against
  `4556ecd`, whose entire diff touches four files and creates no servlet. Several documents read it as a
  page. Corrected in item 11's **Attaches at** row (`5f98c53`); other documents not swept.
- **`Opportunity` has no table of its own.** `Opportunity extends Activity extends Assignee`, and
  `Assignee` is `@Inheritance(SINGLE_TABLE)` with `@Id @Column(name="id") Long` — so every Opportunity
  row lives in `assignee` and any FK targets **`assignee(id)`**. V081's header records that this has
  bitten prior migrations and **names V050 and V060** specifically (V081:20). Reversal cost: catastrophic
  if got wrong — a FK pointed at a table that does not exist fails at apply time, which is the benign
  direction; one pointed at the wrong table would not.

## Open questions raised

- **T52** — matched chatbot skills ignore configured `model`/`max_tokens`. Unchanged this session and
  **still gates widening item 12 past PSP admin.**
- **T54** — the dead-keyword survey **has still not been run**. `summit_official`/`summit_supplemental`
  are non-admin-reachable today, so this may be a live degradation, unmeasured.
- **T55** — the Design Advisor hub card. Still open, still unbuilt this session; the card still reads
  `Coming` and its copy still promises what the advisor refuses.
- **T56** — banner/disclaimer duplication, **now three copies** (`illustration25.jsp`'s two mode
  branches plus `groupConversion25.jsp`). Logged this session at `7db188d`, deliberately unfixed —
  the fix touches item 9's shipped JSP and belongs in its own commit.
- **Whether `result_summary` should ever surface on the drawer, and to whom.** Decision 6 defers it; it
  does not answer it. The question is not "can we" but "which audience", and D24 governs one side of it.
- **Notice automation (B4a/B4b).** §5 calls it *"the strongest deferral on this list, and the least
  comfortable"* — it fixes a problem that exists **today for existing clients**, both feeds already
  exist and are already scheduled, and it gates on nothing. Its stated trigger is *"a gap between demo
  milestones — take it the moment one appears."* **With items 1–13 done, that gap is now open.**
  Awaiting Kevin's yes.
- **A5's gate is now met.** §5 lists the pipeline console's entry condition as *"S3 resolved, plus item
  13"* — both landed this session, and `619461f` is a first, narrow instance of exactly the read A5
  generalises. Not started; noted because the blocker text is now stale.

## Contradictions found

- ⚠️ **A claim this project asserted and then had to retract — the session's own doc-hygiene failure.**
  `ON DELETE SET NULL` was argued as fixing a live failure: *"an agent who creates an opportunity, runs
  an illustration against it, and then deletes the opportunity hits a foreign-key error."* The
  verification grep run in the same session found **no delete path for an Opportunity, Activity or
  Assignee row exists in AMS at all**, so nothing would fail under `RESTRICT` today. The decision stands
  and is unchanged — `SET NULL` is free, semantically right for a nullable attribution column, and
  correct if a delete path ever appears — but the reasoning was overstated and had been **written into
  V081's header as fact**, which is precisely how this project's docs go wrong. Corrected in `cf15ff8`;
  the same claim was then found **repeated in the build plan's item-13 Schema row** and corrected in
  `682bc8f`. Two places, one unverified sentence, caught only because the grep was run at all.
- ⚠️ **The release baseline was wrong everywhere, and is now established.** `swbd_ichra_build_plan.md`
  §2 carries an open row — *"Release v0.76.02 — ⚠️ Unconfirmed in-repo… confirm against the GitHub
  Releases page"* — and session 2 declined to check, recording only that local tags showed nothing past
  `v0.71.06`. **Resolved this session:** `git fetch --tags` (the method `CLAUDE.md` prescribes) shows the
  actual latest release is **`v0.78.01`**, pointing at `4556ecd` (item 9), with **16 commits since**.
  Three consequences: (a) `ichra_strategy.md:6`'s baseline line — *HEAD `935c31e`, migration V076,
  release v0.76.00* — is stale on all three counts; (b) `migration_tracker.md:160`'s *"V079 … awaiting
  release `v0.79.00`"* is stale, since V079/V080/V081 will now ship together; (c) the tracker's
  V077/V078 Production correction explicitly noted it rested on *behavioral* evidence with **no
  deployment-log entry consulted** — the existence of `v0.77.00`, `v0.78.00` and `v0.78.01` is
  independent corroboration that releases were cut for exactly those versions. ⚠️ **Corroboration, not
  proof**: a release existing is not evidence that `update.sh` applied its SQL. The tracker's
  re-confirmation query still deserves running.
- **`swbd_ichra_build_plan.md` §6 still instructs "Send it before item 9 starts"** on the
  *"three groups renewing next quarter"* row. Item 9 shipped last session (`4556ecd`) and item 11
  shipped this one (`7db188d`), both without the email. The instruction is doubly stale. Item 11's own
  **Depends on** row was corrected in `5f98c53`; **§6's row was not touched** — this run's fence
  permitted only the close-out. Recorded, not fixed.
- **The falsification test is narrower than the telemetry answers.** Decision 3 keeps the `CONVERSION`
  log row on the grounds that tool usage is the falsification test. Read directly,
  `ichra_strategy.md` §1's bullet is *"Agents use the tools and it changes nothing about who wins the
  case — utility that does not convert is a cost centre."* The test is about **conversion**, and
  `illustration_log` measures **usage only** — it records no outcome and no won/lost signal. Usage is a
  necessary precondition of the test, not the test. Stated precisely here rather than repeated loosely.

## Backlog logged this session

- **T56** — Test-environment banner, generic disclaimers and `meta-line` block triplicated across
  `illustration25.jsp` and `groupConversion25.jsp`. Logged at `7db188d`, unfixed. Highest backlog item
  is now **T56**, read from `project_backlog.md`.

## Next

**Recommended next step: notice automation (B4a/B4b).** It is the only item on the deferral list whose
own stated trigger has just fired — the build sequence is complete, so the "gap between demo milestones"
it waits for is now open. It gates on nothing external, both feeds already exist and are already
scheduled, and unlike everything else queued it fixes a problem **existing clients have today** rather
than one Forrest might have later. Its only demerit is invisibility to the demo, which is a weaker
objection now that the demo path is built.

⭐ **Kevin's list — not build items. Current state read from the repo this session, not copied forward:**

- ⭐ **Release `v0.81.00`** — `ROOT.war` plus **three** SQL files: `V079__proposal_ichra_snapshot.sql`,
  `V080__ichra_design_advisor_skill.sql`, `V081__illustration_log_opportunity.sql`. All three are `⬜`
  on Production in the tracker; no other version is unapplied. **Confirmed this session that the current
  release is `v0.78.01`** (via `git fetch --tags`), so V079 and V080 were indeed never released and this
  is not `v0.79.00` or `v0.80.00`. Per `CLAUDE.md` the tag is **typed in the GitHub Releases web UI** —
  never pushed from local git.
- **The ICHRA/QSEHRA LOS rows** and the ICHRA Illustration section scoped to them (item 4, admin UI).
  Still the hard prerequisite for item 6's section rendering — S5 forecloses the `scope='ALL'` shortcut.
- **The item 10 checklist through the Sequence Builder UI** — 19 tasks, fully specified in
  `docs/business/ichra_setup_checklist.md`. Item 10 remains `Content done`, not done, until this happens.
- **D-86 and D-87** — both `Status: Not started`, confirmed in `docs/deployment_backlog.md`. D-86 is
  HIGH: without `ICHRA_AFFORDABILITY_PCT_{year}` the item-9 affordability feature produces **no output
  at all** for that plan year. It fails closed by design and that is not a bug to route around.
- ⭐ **Production allow-listing** — still `❌ none` per §6, asked 2026-07-30 and chased 2026-07-31.
  Still blocking shipped features from client-facing use.
- ⭐ **The two SWBD emails — checked this session, and neither has been sent.** §6 still records the
  O22 book profile as `❌ not sent` and *"send me three groups renewing next quarter"* as `❌ not sent`.
  The plan calls them *"the cheapest de-risking available anywhere"*, and **item 11 now exists
  specifically to make the second one worth answering.** Nobody has yet asked Forrest what he would want
  a quoting tool to do.

## SQL close-out audit

**V081 is the only SQL produced this session**, committed in `a71b79d` as
`docs/migrations/V081__illustration_log_opportunity.sql`. Verified by reading the file: **three
statements**, and no others.

1. `ALTER TABLE illustration_log ADD COLUMN opportunity_id BIGINT NULL, ADD CONSTRAINT
   fk_illustration_log_opportunity FOREIGN KEY (opportunity_id) REFERENCES assignee(id) ON DELETE SET
   NULL;`
2. `CREATE OR REPLACE VIEW schema_info AS SELECT 'V081' …`
3. `INSERT IGNORE INTO schema_version (version, description, script_name, applied_on) VALUES ('V081', …)`

No index (InnoDB creates one implicitly for the FK). No backfill. **No `INSERT INTO constant`** —
constant rows are `D-NN` deployment-backlog items.

`cf15ff8` edited this file's **header comment only**. Confirmed by filtering the diff to non-comment
lines: every changed line begins with `--`, and the filtered diff is empty. **No SQL was changed.**

Item 11 (`7db188d`) and the console read (`619461f`) produced **no SQL at all** — stated explicitly, not
by omission. The console read adds a JPQL `SELECT` inside `IllustrationLogDAO`, which is application
code, not schema.

**In a versioned migration:** all three statements, yes — `V081__illustration_log_opportunity.sql`.

**Orphaned `.sql` files.** Listed `docs/migrations/` directly: **58 files** — 57 versioned (V025 through
V081, unbroken) plus **exactly one** that does not match `V{NNN}__*.sql`:
`seed_ndt125_questionnaire.sql`, the known pre-existing orphan tracked as
**T38** (`project_backlog.md:99`, logged 2026-07-29). **It is still there, and nothing new joined it
this session.**

**Current highest version: V081**, read from the directory (`V081__illustration_log_opportunity.sql` is
the highest-numbered file present). Cross-checked against `migration_tracker.md:19` — `## Current
Highest Version: V081`. **They agree.** V081 is also registered in `docs/schema_version_migration.sql`
(line 104) with the tail entry correctly terminated by `;`.

**Pending deployment.** Per the tracker's Production column, exactly three versions are unapplied on
production: **V079, V080 and V081** — all `⬜`. Every version through V078 shows `✅`. The same three are
also unapplied on `beta_ssa (work)`, `beta_ssa (home)` and `dev_ssa`.

**Schema described but not scripted: none.** This session's only new document is this close-out, and it
names no table or column without a migration behind it. `illustration_log.opportunity_id` is the one new
column and V081 creates it. ⚠️ Worth naming as a **pre-existing** case rather than a new one: **D-86 and
D-87 contain executable `INSERT` snippets for `constant` rows** (`ICHRA_AFFORDABILITY_PCT_{year}`,
`FPL_ANNUAL_{year}`) that are deliberately *not* migrations — by design, per the rule that constant rows
are deployment-backlog items. Both remain `Not started`, so that SQL is recommended and unexecuted. It
was written in a prior session, not this one.

**Nothing was executed against any database.** No database client was invoked at any point this
session; no connection was opened; every SQL statement named above exists only as file content. The only
commands run were `git` (read-only except `add`/`commit`/`push` on the paths named in each run) and
`./mvnw compile`.

---

**Related:** `docs/swbd_ichra_build_plan.md` (items 1–13 now complete; §4 all six resolved) ·
`docs/analysis/legal_assumptions.md` (LA-01–LA-13, unchanged this session) ·
`docs/analysis/project_backlog.md` (T1–T56) · `docs/analysis/migration_tracker.md` (V001–V081) ·
`docs/session_closeout_2026-07-31_session2.md` (the prior session, `a7db5a6`)
