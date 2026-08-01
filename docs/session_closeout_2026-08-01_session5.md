# Session Close-out — 2026-08-01, Session 5

**Branch:** `refactor/modernize-architecture` · **Pushed through:** `2b79452`
**Scope:** getting the ICHRA Design Advisor working end to end for a role-2 agent on production —
the runtime check session 4 left open. Two code/schema fixes, three deploys, one confirmed answer.

⚠️ **This document's own commit lands one ahead of the state it describes**, the same way session 4's
did. `2b79452` is the last commit of the session's *work*; this close-out (and the backlog row it
carries) is the commit after it.

⚠️ **Session 4's close-out recorded "Pushed through: `bbc6519`" and that was not HEAD when this session
started.** `git log --oneline bbc6519..HEAD` shows three commits ahead of that record before this
session's own work begins:

| Commit | What it was |
|---|---|
| `a4cead8` | The session-4 close-out doc's own commit — expected, and predicted by that document. |
| `37b9e2f` | `Correct build plan statements superseded by session 4` — the §3 item-12 corrections session 4's own "Contradictions found" section said were coming. Recorded there in prose, but the hash was not, because it did not exist yet. |
| `a42a9ae` | `Update claude_memory.md current state for session 4` — the doc-hygiene commit per CLAUDE.md's "Keeping state docs current". Same pattern as `8e4fea4` after session 3. |

This is the third consecutive session where the close-out's "pushed through" hash was superseded within
minutes by doc commits. It is not a defect in any one document — a close-out cannot name commits that
come after it — but the pattern is now established enough to expect: **the real end of a session is
two to three commits past whatever its close-out records.** Named here rather than silently absorbed.

---

## Shipped

This session's own commits, `a5c0d8d..2b79452`, read from `git log --oneline a42a9ae..HEAD`:

| Commit | What |
|---|---|
| `a5c0d8d` | **The entitlement fix.** `IchraAccessResolver.isAvailable` now resolves entitlement against the caller's full `AgencyScope.detailAgencyIds()` membership set, not `primaryAgencyId` alone. The primary-id fast path is preserved (one `em.find`); on a miss, a single `SELECT a.id FROM Agency a WHERE a.id IN :ids AND a.ichraEnabled = true` with `setMaxResults(1)` covers the whole set, and an empty set short-circuits before the query so no empty `IN ()` is ever issued. Adds INFO logging of the resolved primary id, the membership set, the matched id and the outcome — previously a `false` was silent and indistinguishable from an unset flag. `isAvailableForNav` delegates, so the nav entry and the chat widget both inherit the fix. Fail-closed exception behaviour unchanged. Logged **T60** and **T61**. `./mvnw compile` clean. |
| `2b79452` | **V083.** `V083__ichra_design_advisor_model.sql` — `chatbot_skill.model` → `claude-sonnet-5`, `max_tokens` 2048 → 3072, keyed on `skill_name`. Registered in `migration_tracker.md` and `schema_version_migration.sql`. Also corrected the tracker: V080 and V082 marked applied on Production, with the evidence recorded rather than the cells flipped silently. |

Two commits. Every hash above was read from `git log`, not carried over from any prompt.

**Not in git, but part of the session:** three production deploys — `v0.82.00` (WAR + V079–V082),
`v0.82.01` (WAR carrying `a5c0d8d`), `v0.83.00` (WAR + V083). Releases are tagged by hand in the GitHub
web UI, so they leave no local tag; see the SQL audit below for what each applied.

## In flight

**None. The working tree was clean** at the start of this run (`git status --short` returned nothing
after `2b79452` was pushed) and carries only this document plus the `project_backlog.md` T62 row at the
time of writing.

## Decisions made

1. **Entitlement follows membership, not the primary-agency tie-break.** `AgencyScopeResolver
   .resolvePrimaryAgencyId` resolves manager-match first, else **lowest `agency_id`** — a deterministic
   rule, but one with no relationship to entitlement. A person genuinely belongs to many agencies
   (`Person ↔ Agency` is ManyToMany), and Phase 2b already gives a plain agent the full membership set
   as their authorization scope. Reading only `primaryAgencyId` was therefore narrower than the
   authorization model everywhere else in the resolver package. Fixed to match.
2. **The fast path was kept rather than replaced by the set query.** The common case — a single-agency
   agent — still costs one `em.find` and no JPQL. The membership query runs only on a miss.
3. **V083 moved straight to `claude-sonnet-5` rather than to an interim 4.x model.** Mechanical, per a
   rule applied to the code as read: `ClaudeApiService`'s request builders send only `model`,
   `max_tokens`, `system` and `messages` — verified across all six body-building blocks, no
   `temperature`, `top_p`, `top_k` or `thinking`. Since no non-default sampling parameter is sent,
   Sonnet 5's restriction on them cannot bite, and there was no reason to stop at 4.6.
4. **`max_tokens` raised 2048 → 3072 with V083, not left alone.** Sonnet 5's tokenizer consumes
   materially more tokens for the same text, so V080's 2048 buys less usable output under the new
   model. This skill's whole value is answers that reach their `Source:` lines; truncation before the
   citation is the specific failure mode the budget exists to prevent.
5. **The tracker's Production cells for V080/V082 were corrected with the evidence written down**,
   not flipped quietly — following the standard the V077/V078 correction set one section above it, and
   the maintenance note added 2026-07-30 that exists precisely to stop silent drift.
6. **`ClaudeApiService` was not touched.** It needed no change; had it needed one it would have shipped
   as its own commit rather than riding along with a data migration.

## New assumptions (technical, not legal)

- ⚠️ **`a5c0d8d` is not established as the cure for the missing nav entry.** This is the most important
  thing in this document, and it is an assumption, not a finding.

  **Assumption:** the ICHRA nav entry and chat widget were hidden because EclipseLink's shared (L2)
  cache was serving a stale `Agency` row with `ichra_enabled = 0`. The flag was set by raw SQL,
  bypassing JPA entirely, so nothing evicted the cached entity; `em.find(Agency.class, id)` kept
  returning the pre-flip row until the `v0.82.01` redeploy dropped the cache with the application.

  **Basis:** the production INFO line that `a5c0d8d` added reads
  `primaryAgencyId=135533, membership=[135533], matched=135533, available=true` — a **single-agency
  agent**. The membership set has exactly one element and it is the primary id. **The fallback branch
  never fired.** The fast path matched, which is the same code path (`em.find` → `isIchraEnabled()`)
  that existed before `a5c0d8d`. So the fix cannot be what changed the answer for this user; something
  else did, and the redeploy is the obvious candidate. The L2 cache is a known, already-documented
  hazard in this codebase — it caused a live OOM incident during the Monthly Billing Launcher work and
  is called out in `claude_memory.md`.

  **Reversal cost:** none for the code — `a5c0d8d` is correct on its own terms and worth keeping
  regardless: a genuinely multi-agency agent, whose primary tie-breaks to an unentitled row, *would*
  have been denied and now is not. The cost of the assumption being wrong is diagnostic, not
  structural: it would mean the real defect is still live and will resurface the next time the flag is
  flipped on a running instance.

  **Confirm before:** treating raw-SQL entitlement as working. The test is cheap and nobody has run it —
  flip `ichra_enabled` on a *running* instance for a second agency, log in as one of its agents, and
  check whether ICHRA appears **without a restart**. Until that runs, "SQL sets the flag" is unproven
  as an operational procedure, and this session's success is not evidence for it, because the flag was
  set before a deploy every time.

  **Consequence — T61 is more important than it was logged as.** If the hypothesis holds, direct SQL is
  not merely inconvenient, it is *ineffective until restart*, and an admin checkbox writing through JPA
  would evict the cache correctly and take effect immediately. **Recommend raising T61 from MED to
  HIGH** once the confirm-before test runs and holds. A note to that effect has been added to T61's
  backlog row rather than the priority being changed on an untested hypothesis.

- **`claude-sonnet-4-20250514` was retired, not throttled or misconfigured.** Basis: `GET /v1/models`
  against the production key returned a list not containing it, and the API returned
  `404 not_found_error` naming the model string specifically. Reversal cost: one `UPDATE` — V083's
  header records the exact reversal statement. Low confidence needed; this one is about as directly
  observed as an external fact gets.

- **No other seeded skill is affected by the retirement.** Basis: only two migrations insert into
  `chatbot_skill` — V065 (`EMAIL_DRAFT_ASSISTANT`, `claude-sonnet-4-20250514`, 4096) and V080
  (`ICHRA_DESIGN_ADVISOR`). V065's row carries the same retired model string, **but it never reaches
  the API through the skill path**: it seeds no `trigger_keywords`, so `scoreKeywordMatch` returns 0 and
  it can never clear `findMatchingSkill`'s ≥2 threshold, and its real caller `EmailDraftService` passes
  model/maxTokens explicitly. Reversal cost: none — but ⚠️ **the reasoning is inherited from T52's
  survey, not re-verified this session**, and it does not cover skills created or edited through the
  Skill Manager admin UI after seeding, which are invisible to migration files. If anyone configured a
  skill at runtime with a 4.x-dated model string, it has the same latent 404.

## Open questions raised

- ⚠️ **The L2 cache question above** — the one thing this session's success does *not* establish. See
  the confirm-before trigger.
- **Whether any runtime-created `chatbot_skill` row carries a retired model.** Migration files cannot
  answer this; only a `SELECT skill_name, model FROM chatbot_skill` can. Not run — no database
  connection was opened by this session's tooling.
- **T60 remains open and is now better understood.** `ichraNavVisible` caches for the session's
  lifetime with no invalidation path. Note it is *not* what caused this session's symptom — logging out
  and back in did not fix it, which is what ruled the cache out and sent the investigation to the
  resolver in the first place.
- **The wider open backlog**, read from `docs/analysis/project_backlog.md` directly. Newly logged or
  still open, ICHRA-adjacent, one line each:
  - **T60** `ichraNavVisible` session cache has no invalidation — 💡 Backlog *(new this session)*
  - **T61** `agency.ichra_enabled` has no admin UI write path — 📋 Planned, **raise to HIGH pending the cache test** *(new this session)*
  - **T62** Design Advisor citations render as an imperative instruction — 💡 Backlog *(new this session)*
  - **T51** `ProposalSettings.updateScope` lets an ICHRA section revert to `scope='ALL'` — 📋 Planned, HIGH
  - **T53** unguarded path around a skill's own guardrails — 💡 Backlog, single-guarded since V082
  - **T54** dead multi-word/hyphenated `knowledge_chunk` keywords, existing KBs unsurveyed — 📋 Planned
  - **T56** banner/disclaimer triplication — 💡 Backlog
  - **T59** two hub cards share one URL — 💡 Backlog
  - Plus T39/T41/T42/T44/T47/T48/T50, all 💡 Backlog, unchanged this session.

  The backlog also carries a larger set of open items unrelated to the ICHRA epic — read
  `project_backlog.md` directly for those.

## Contradictions found

- ⚠️ **`migration_tracker.md` was wrong about V080 and V082's production state** — both carried `⬜` for
  Production while the production log proved otherwise. Corrected in `2b79452`. The evidence is
  behavioural and it is airtight in one direction: for `catalina.out` to record `ChatAssistant`
  executing `ICHRA_DESIGN_ADVISOR` **for a non-admin caller**, the row must exist (V080) *and* carry
  `is_admin_only = 0` (V082). V081 was left `⬜` — nothing in the incident establishes it either way,
  and guessing is what produced the drift in the first place.

- ⚠️ **The tracker is stale again, in the same column, as of this writing.** V079, V081 and V083 all
  still read `⬜` for Production, but per this session's deploys **V079–V083 were applied to production
  via `update.sh`**. This is the exact failure the tracker's own maintenance note (added 2026-07-30)
  exists to prevent: *"Flipping the Production cell is part of landing the deploy, not a follow-up
  task."* It is recorded here rather than fixed, because this run's scope fence permits editing only
  this document and `project_backlog.md`. **It should be the first edit of the next session.**

- **Build-plan item 2 says "Copy V067 exactly"; only the DDL was copied.** V067's `markup_enabled`
  shipped with a checkbox in `agencyManager25.jsp` and a `setMarkupEnabled` call in
  `AgencyAction.editAgency`. V077's `ichra_enabled` shipped the column alone —
  `Agency.setIchraEnabled()` has **zero callers anywhere in the codebase**, so no UI can write it even
  in principle. Logged as **T61**. The entity maps the column, so closing the gap is a JSP + servlet
  change with no entity work and no migration.

- **A hypothesis was killed, and that is worth recording as much as the fix.** The investigation began
  from a plausible and wrong theory: that agents never render `navbar25.jsp`, so every gate session 4
  widened sat on a page the intended audience never loads. `agentHome25.jsp:255` imports it
  unconditionally (`<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>`). The observed sparse header
  was navbar25 with its role-gated entries correctly hidden — the agency-name branch at lines 115–125
  and the Sales dropdown at 186–204 both render for `isAgent`, and both sit inside the same
  `${applicationScope.isPspSystem}` wrapper (155–275) as the ICHRA block, which is what proved the ICHRA
  block was evaluated and simply returned false.

- **Build-plan item 12 needs no §7 rule-1 edit — it already complies.** Item 12's heading already reads
  `### 12 — ~~A6: design advisor~~ ✅ Done 2026-07-31, e25ee4e` — struck through, dated, hashed. Session
  4's `37b9e2f` had already corrected its Gate row. **What changed this session is evidentiary, not
  structural:** item 12 is now the first ICHRA capability *verified working end to end for a
  non-PSP user at runtime*, rather than believed complete from code reading. Nothing in §3 needs
  striking that is not already struck. If anything is added later it should be the runtime-verification
  date and this session's hashes alongside `e25ee4e` — a judgment call for whoever next edits the plan,
  **not made this run**, since the build plan is outside this run's scope fence.

## Backlog logged this session

- **T60** — `ichraNavVisible` session cache has no invalidation. Logged in `a5c0d8d`. 💡 Backlog, MED.
- **T61** — `agency.ichra_enabled` has no admin UI write path. Logged in `a5c0d8d`. 📋 Planned, MED,
  **with a recommendation to raise to HIGH** appended to its row this run, pending the L2-cache test.
- **T62** — Design Advisor citations render as an imperative instruction, not an attribution. Logged
  this run. 💡 Backlog, LOW. **Root cause verified, not guessed:** the observed
  `Cite domain_and_compliance_rules.md section 5` is reproduced *verbatim* from V080's own few-shot
  exemplar answers — the file contains four `Cite <doc>.md section <n>` strings inside its `A:` example
  answers, not merely in boundary 6's instruction. The model is imitating its examples correctly; the
  examples are written in the imperative. The mechanism works — the right document and the right section
  were named — so this is presentation polish. Not fixed this session.

**Highest backlog item is now T62.** Three numbers consumed this session (T60, T61 in `a5c0d8d`; T62
here), read from `project_backlog.md` rather than assumed.

## Next

**Recommended next step: the reference data, not a code item.** ⚠️ **All thirteen items in
`docs/swbd_ichra_build_plan.md` §3 are struck through and done** — verified by reading the §3 headings
directly, not from the plan's prose. Items 1, 2 and 3 in particular are all `✅ Done 2026-07-31,
0b4711b`. There is no §3 build item left to recommend.

Mapping §1's eight demo steps against what exists:

| Step | Needs | State |
|---|---|---|
| 1 Login, SWBD branding | — | ✅ built (V068–V071) |
| 2 Click **ICHRA** in top nav | Item 3 | ✅ done — **and confirmed working for a role-2 agent this session** |
| 3 Hopkins County, 3 lives | — | ✅ built |
| 4 Three actual ages | Item 5 | ✅ done |
| 5 Contribution slider, flip point | Items 8 + 9 | ✅ done |
| 6 **Use this in a proposal** | Items 6 + 7 | ✅ done |
| 7 Send; public link, markup invisible | — | ✅ built (V066/V067) |
| 8 Apply → Setup with ICHRA task sequence | Items 4 + 10 | ⚠️ **the only gap** |

**Step 8 is the whole remaining path, and neither half of it is code.** Item 4 was deleted from the
sequence on 2026-07-31 as explicitly not a build item — *"Kevin creates the ICHRA/QSEHRA `LOS`,
`ServiceItem`, `PlanType` and priced `ServiceModule` → `RateTable` rows through the admin UI when he is
ready to test."* Item 10 is marked **"✅ Content done"** — deliberately different wording from the other
twelve — with its deliverable sitting ready at `docs/business/ichra_setup_checklist.md` (verified
present) and its own row stating no migration exists or should: *"no migration in the repo (V025–V080)
has ever created a task sequence."* Sequences come from the Sequence Builder admin UI.

So the fastest path to §1's demo is **two admin-UI configuration sessions, in this order**: create the
ICHRA reference rows (item 4), then type the checklist from `ichra_setup_checklist.md` into the Sequence
Builder against the `ServiceItem` that creates (item 10). Item 10 depends on item 4's rows existing, so
the order is forced.

**Two cheap things to do first, both diagnostic rather than constructive:**
1. **Flip `ichra_enabled` on a running instance and check without restarting** — the confirm-before
   trigger for this session's central assumption. One `UPDATE`, one login. It decides whether T61 is a
   convenience item or a correctness one, and that decision changes the priority of real work.
2. **Correct the tracker's V079/V081/V083 Production cells**, which this run could not touch. One edit,
   and it stops the exact drift the file's own maintenance note was written to prevent.

---

## SQL close-out audit

**`V083__ichra_design_advisor_model.sql` is the only SQL produced this session.** Verified by reading
the file directly, quoted in full (statements only; the file's header comment block is omitted here):

```sql
UPDATE chatbot_skill
   SET model = 'claude-sonnet-5',
       max_tokens = 3072
 WHERE skill_name = 'ICHRA_DESIGN_ADVISOR';

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V083' AS version, '2026-08-01' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V083',
        'Fix retired model on ICHRA_DESIGN_ADVISOR (chatbot_skill.model -> claude-sonnet-5, max_tokens -> 3072)',
        'V083__ichra_design_advisor_model.sql',
        NOW());
```

Three statements. The `UPDATE` is keyed on `skill_name` alone (rule 4 — no hardcoded `psp_id`, no id);
V065's unique index is `(psp_id, skill_name)`, so it updates one row per PSP — one row on every current
installation, since only `psp_id=4` is seeded.

**In a versioned migration:** yes — all three, in `V083__ichra_design_advisor_model.sql`. The session's
only other commit, `a5c0d8d`, touched one `.java` file and one `.md` file and produced no SQL. The JPQL
string it added (`SELECT a.id FROM Agency a WHERE a.id IN :ids AND a.ichraEnabled = true`) is
application code compiled into the WAR, not schema, and correctly needs no migration.

**Orphaned `.sql` files.** `docs/migrations/` holds **60 files** — 59 versioned (V025 through V083,
unbroken) plus the one known pre-existing orphan, **`seed_ndt125_questionnaire.sql`** (T38, logged
2026-07-29). Nothing new joined it this session.

**Current highest version: V083**, read from `docs/analysis/migration_tracker.md`
(`## Current Highest Version: V083`) and cross-checked against the directory listing — they agree. V083
is registered in `docs/schema_version_migration.sql`, tail entry correctly terminated with `;`.

**Pending deployment — and the tracker disagrees with reality here.** Per the tracker's own cells,
V079, V081 and V083 read `⬜` for Production while V080 and V082 read `✅`. **Per this session's actual
deploys, V079 through V083 were all applied to production via `update.sh`** across releases `v0.82.00`
(V079–V082) and `v0.83.00` (V083). The tracker is stale on three rows; see "Contradictions found" and
"Next". Every version through V078 shows `✅` and is not in question. The `beta_ssa (work)`,
`beta_ssa (home)` and `dev_ssa` columns show `⬜` for V079–V083 and were not touched this session.

**Schema described but not scripted: none.** This session named no table or column without a migration
behind it. V083 changes only values in existing `chatbot_skill` columns (`model`, `max_tokens`, both
from V046); it creates nothing.

**What was and was not executed against a database — stated explicitly, because it differs from every
prior session in this epic:**

- ✅ **Executed against production, by Kevin, outside this tooling:** V079, V080, V081 and V082 via
  `update.sh` during the `v0.82.00` deploy; V083 via `update.sh` during the `v0.83.00` deploy. These are
  real applied migrations, which is why the tracker's V080/V082 correction was warranted and why its
  V079/V081/V083 cells are now wrong.
- ✅ **Executed against production, read-only:** one verification `SELECT` against `chatbot_skill` after
  the `v0.83.00` deploy, confirming the row reads `model = claude-sonnet-5`, `max_tokens = 3072`,
  `is_admin_only = 0`. Read-only; it mutated nothing.
- ✅ **Also run against the live API, not a database:** `GET /v1/models` with the production key, which
  is what established that `claude-sonnet-4-20250514` is retired rather than misconfigured.
- ❌ **Not executed by any Claude Code run in this session.** No database client was invoked and no
  connection was opened by the tooling in any of this session's runs. Every SQL statement written in
  those runs — V083's three, and the illustrative `UPDATE agency SET ichra_enabled = 1` printed in the
  first investigation's report — existed only as file content or as report text. Commands run by the
  tooling were `git` (read-only except `add`/`commit`/`push` on explicitly named paths),
  `./mvnw compile`, `./mvnw -P server clean package`, and read-only file inspection.

---

**Related:** `docs/swbd_ichra_build_plan.md` (§3 now entirely complete; item 12 verified at runtime this
session) · `docs/analysis/project_backlog.md` (T60/T61/T62 new, highest item T62) ·
`docs/analysis/migration_tracker.md` (V001–V083; V080/V082 corrected `2b79452`, V079/V081/V083 stale) ·
`docs/business/ichra_setup_checklist.md` (item 10's deliverable, awaiting entry into the Sequence
Builder) · `docs/session_closeout_2026-07-31_session4.md` (the prior session, recorded through
`bbc6519`, actual final commit `a42a9ae`)

---

## Addendum — 2026-08-01, the confirm-before test ran

**This section is appended after the document above; nothing above it has been edited.**

**The test.** After deploying `v0.83.01` (`0852c2f`, the T61 checkbox), a second agency was entitled for
ICHRA through the new UI on the **running** production instance — no restart, no redeploy. An agent of
that agency then logged in **fresh** and ICHRA was present: nav entry and chat widget both rendered.

**The result, stated precisely — this is not a disproof.** The UI writes through JPA
(`em.find` → setter → transaction commit), which is exactly the path that keeps EclipseLink's shared
cache correct. So a clean result here is **fully consistent with** the stale-cache assumption above
being true — it does not test the raw-SQL path at all, because the new UI exists precisely so nobody
has to use that path anymore. Do not read this as "the cache hypothesis was wrong." It was never tested
by this run, and it remains untested.

**What this actually establishes, narrower than a clean resolution:**

- **The UI entitlement path works without a restart.** T61 is closed in practice, not merely in code —
  a real agency was entitled on a live instance and a real agent saw the result without any deploy step
  intervening.
- **The stale-cache hypothesis is moot for ICHRA entitlement specifically**, because the only code path
  that could ever hit the trap — a raw SQL `UPDATE` against `agency.ichra_enabled` — is no longer the
  supported way to set the flag. Nobody needs to use it, so whether it was ever actually broken stops
  mattering for this feature.
- **The general question is untouched and still open:** whether a raw SQL `UPDATE` against *any*
  JPA-mapped AMS config table goes unseen by a running instance until a restart. This session's test
  cannot speak to it either way — it tested the JPA path, not the SQL path, on purpose, since that is
  the whole point of having built the UI. Logged as its own backlog item (T64) below, since it applies
  far beyond ICHRA and to every operational runbook in this codebase that says "run this UPDATE."

**T60 was also observed, not just reasoned about.** The confirm test required a **fresh login** — the
agent who was entitled had not been logged in before the flag was set, so `ichraNavVisible` had never
been cached for that session. This is consistent with T60's known effect (an already-logged-in agent
stays stale until re-login) but does not test it either; a same-session before/after check was not run.
