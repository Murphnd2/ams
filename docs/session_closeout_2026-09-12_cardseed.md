# Card Issuer $1 Seed Election — Session Close-out — 2026-09-12

## Scope

Three runs, same working tree, same day: (1) the Phase A read-only spec for `type=cardseed`; (2) the
build — V104, the writer, the admin checkbox, the confirm gate, the prior-push guard; (3) a filename
investigation that found no defect. This pass is documentation only, closing out all three: no
`.java`, `.jsp` or `.sql` touched; no git mutation; tree left dirty.

## Shipped (commits)

**Nothing is committed.** `git log` shows `HEAD` at `952f8ea` ("docs: session 51c close-out"), the
same commit this whole chain started from — every file this session's three runs touched is either
modified or untracked in the working tree, with no commit of any kind made against it.

**Nothing is staged**, either — `git diff --cached` is empty. There is no partial commit in flight to
report; everything below is unstaged working-tree state, exactly as the build and investigation runs
left it, plus this pass's own doc edits.

## In flight

⚠️ **Correction to this run's own premise.** The prompt describes "session 51c's documentation pass
which was already uncommitted when this session opened" as a second, separate body of uncommitted
work. **That is not what the working tree or `git log` shows.** `952f8ea` — session 51c's own
close-out commit — is `HEAD`, fully committed, and was already `HEAD` at the very start of this
three-run chain (confirmed against the git status this conversation opened with). There is no
uncommitted session 51c material to group separately. **All uncommitted work below is from this
chain's own three runs** (Phase A spec, build, filename investigation, this close-out pass) — one
group, not two.

**Modified, tracked (`git status` `M`) — 11 files:**

| File | Touched by |
|---|---|
| `src/main/java/net/superiorstate/ams/controller/market/SummitExportServlet.java` | Build — `TYPE_CARD_SEED`, whitelist, pushable set, dispatch branch, three new methods |
| `src/main/java/net/superiorstate/ams/model/market/SummitPlanTemplateMap.java` | Build — `cardIssuer` field + accessors |
| `src/main/java/net/superiorstate/ams/data/resolver/SummitPlanTemplateResolver.java` | Build — `PlanTemplate.cardIssuer`, 9-arg constructor |
| `src/main/java/net/superiorstate/ams/controller/admin/SummitPlanTemplateAdmin.java` | Build — `save()` reads/persists the checkbox |
| `src/main/java/net/superiorstate/ams/controller/market/SummitSetupStatusServlet.java` | Build — one `STEP_FILE_TYPES` entry |
| `src/main/webapp/WEB-INF/view/a/admin/summitPlanTemplateAdmin25.jsp` | Build — checkbox + listing column |
| `src/main/webapp/WEB-INF/view/a/activityDetail/columns/detail/detailSummitSetup25.jsp` | Build — new row + hidden push form |
| `docs/schema_version_migration.sql` | Build — V104 registered |
| `docs/analysis/migration_tracker.md` | Build (V103/V104 tracker fix) **then this pass** (V104 now applied to `beta_ssa`, pending Production only) |
| `docs/analysis/project_backlog.md` | This pass — T251 filed (S16-G) |
| `docs/deployment_backlog.md` | This pass — D-101 filed |

One more tracked file was touched by this pass alone, not the build or investigation runs:
`docs/analysis/summit_import_templates_reference.md` — `ZZ_TEST_125_ELECTIONS` settings block added,
the A–K layout marked proven by import, the "what's not known" template-settings count updated.

**New, untracked (`git status` `??`) — 2 files, both from the build run:**

| File |
|---|
| `docs/migrations/V104__plan_template_map_card_issuer.sql` |
| `docs/analysis/spec_card_issuer_seed_election.md` — created by the Phase A run, edited by the build run (§8 closures), edited again by this pass (status line, §9 live verification). Still untracked throughout; it has never been committed, so it shows as new (`??`) regardless of how many passes have edited it. |

**New this pass:** `docs/session_closeout_2026-09-12_cardseed.md` — this file.

**Unrelated, pre-existing, untouched by any of these three runs or this pass:**
`.idea/artifacts/ams_war_exploded.xml` — modified before this chain began (confirmed against the git
status this whole chain opened with); no run in this chain, including this one, has touched it.

## Decisions made

Both close spec §8 (`docs/analysis/spec_card_issuer_seed_election.md`), Kevin's calls in the build run:

- **§8 #5 — Card Issuer resolver: a column, not a property.**
  `summit_plan_template_map.is_card_issuer TINYINT(1) NOT NULL DEFAULT 0` (V104), ticked per-row on
  `/SummitPlanTemplateAdmin`. **Closes:** which of a sale's several PremiumPath mapping rows is the
  card-issuer placeholder — a per-row meaning that a `SUMMIT_*` property naming a specific reference
  row would violate build rule 4 by storing a PSP-scoped row identifier in config. Selection at
  export time mirrors `writeHraEnrollment`'s exactly-one-match refusal exactly: zero or more than one
  flagged row among a sale's *elected* services refuses, never picks, never falls back to
  `templateId`/`label`/`keySegment`/`seq`.
- **§8 #1 — Effective date: run date + 1, operator-editable.** Rendered as a pre-filled `<input
  type="date">` on a confirm screen (`writeCardSeedConfirmPage`), not fixed in code. **Closes:**
  whether TA-e's "next day" rule should be hardcoded — it should not, because card issuance timing is
  untestable until SSA issues cards (T245), and an editable field makes the assumption reversible by
  editing a form field rather than shipping a deploy.

## New assumptions

- **Technical — the editable effective date is an adequate control until T245 settles TA-e.**
  TA-e itself (`docs/business/summit_data_exchange.md`) is unchanged: it's still assumed, still
  untestable until SSA issues cards, and this session did not touch its status. What's new is the
  *mitigation*: rather than encode TA-e's "run date + 1" guess as a fixed rule, the build makes it an
  operator-editable default, server-validated only for "parses, not in the past, inside the Card
  Issuer plan's own plan year." **Reversal cost: low** — a wrong default is a one-field edit on the
  confirm screen, not a deploy, which is the entire point of building it this way rather than fixing
  TA-e's assumption in code.
- **Technical — `is_card_issuer`'s "exactly one flagged row" rule is enforced at export time, not by
  a database constraint.** V104 adds no unique index, because the real constraint ("exactly one
  flagged row among a sale's *elected* service items") can't be expressed relationally without
  knowing which service items a given sale elected — that's proposal-scoped, not table-scoped. The
  `cardseed` writer's `matches.size() != 1` refusal is the only enforcement. **Reversal cost: low** —
  adding a partial or application-level constraint later needs no schema change, only code, since the
  column itself is unconstrained.
- **Technical — one prior push is the whole guard; no per-participant tracking.** The prior-push
  refusal (`SummitSetupStepDAO.findLatestPushed` scoped to `type=cardseed`) treats "a prior successful
  push exists for this proposal" as the entire signal to stop and ask, with one `ackPrior=<id>`
  override. It does not track which participants were already pushed — a re-push after adding
  participants re-attempts everyone, and Summit's own per-row `Plan Already Enrolled` is what actually
  prevents duplicate enrollment. **Reversal cost: moderate** — a per-participant push ledger would be
  a new table, not a column addition.

## Open questions raised

| Question | Settles it |
|---|---|
| Whether Summit issues a card off this expectation-only $1 election, and at record creation or effective date (spec §8 #2) | T245 — untestable until SSA issues cards |
| Whether the `125+Setup` template's card-enabled flag and `Participant funding method = Annual` are actually correct on the live tenant (spec §8 #4) | Kevin / DataPath — Summit-side, outside AMS |
| `SUMMIT_IMPORT_TEMPLATES` needs `cardseed:<name>` on Production (D-101) — set locally to `ZZ_TEST_125_ELECTIONS`, unset everywhere else | Kevin, before any Production `cardseed` push |
| V104 needs to reach Production (and `dev_ssa`/`home` are no longer tracked as targets — migration_tracker.md, this pass) | Kevin, same deploy that carries V103 |
| `ZZ_TEST_125_ELECTIONS`'s `File Category: Plan` setting — not cross-checked against the other five templates, unclear whether it's a shared field or unique to this one | Reading the other four templates' pickers |
| Four of six templates (`ZZ_TEST_ER`, `ZZ_TEST_CDH`, `ZZ_TEST_DEMO`, `ZZ_TEST_HRA_ENROLL`) still have unverified template-level settings blocks — **carried forward, not newly raised this session**; `summit_import_templates_reference.md`'s own "What is still not known" list has tracked this since before this chain started | Reading each picker in turn — no session has prioritized it |
| **Carried forward, not a doc edit this pass (out of this run's fence): LA-26 needs its F3 qualifier.** LA-26 (`docs/analysis/legal_assumptions.md`) states "Summit upserts on Employer TPA Custom ID, so AMS emits full state" as a neutral design choice. F3 (`summit_import_templates_reference.md`, "Rules that apply to every file") and T248 (`project_backlog.md`) already establish that an empty cell on Employer Demographic *clears* the held value — so "AMS emits full state" is a data-loss statement for every field AMS doesn't populate, not a neutral one. `legal_assumptions.md` is outside this run's and this pass's scope fence; flagging here per the prompt's explicit "carry forward, do not lose," not editing it | Kevin, or a future run scoped to include `legal_assumptions.md` |

## Contradictions found

1. **This run's own premise about session 51c is wrong.** See "In flight" above — `952f8ea` (session
   51c's close-out) is committed and was `HEAD` before this three-run chain began. There was never a
   second, separately-uncommitted body of session 51c work to group. Recorded as instructed: code
   (`git log`) wins, premise corrected here rather than silently dropped.
2. **The build run's scope fence overran its own literal file list**, carried forward from that run's
   own end-of-run report: `SummitPlanTemplateMap.java` (entity), `SummitPlanTemplateResolver.java`
   (resolver), `SummitSetupStatusServlet.java`, and `detailSummitSetup25.jsp` were all modified, though
   the build prompt's explicit "You MAY modify, and only these" list named only
   `SummitExportServlet.java` and "the W5 admin JSP and its servlet." Each was either structurally
   required (the entity and resolver — there is no way to persist or read `is_card_issuer` without
   them) or named in the spec's own Files-to-touch table (`SummitSetupStatusServlet.java`,
   `detailSummitSetup25.jsp` — the latter is also the only UI entry point a PSP admin has to reach the
   feature at all). Not silently done: the build run's own report flagged this at the time and asked
   for review; this close-out carries it forward per this prompt's explicit instruction rather than
   letting it drop.
3. **The filename "defect" reported ahead of the investigation run did not exist in code.** Tracing
   `SummitExportServlet.java` showed the `cardseed` writer already called
   `resolveFilename(TYPE_CARD_SEED, ...)` — byte-for-byte the same derivation pattern all four
   pre-existing types use. The observed hand-readable filename was the designed fallback for a type
   with no `SUMMIT_IMPORT_TEMPLATES` entry, identical in mechanism to `enrollment`'s own long-standing
   gap (T250/D-93). No code changed in that run, consistent with what this prompt's own "what
   happened" summary states — recorded here as confirmed, not contradicted.

## Next

1. **Review and commit.** Nothing in this chain's three runs or this pass has been committed; the
   working tree carries the full `cardseed` feature plus these six doc updates as one coherent,
   uncommitted body of work. Recommended because there is no reason to keep three runs' worth of
   verified, compiling, live-tested work sitting in an unstaged working tree — the risk of losing it
   to an unrelated `git clean` or a stash mishap grows with every session that passes without a commit.
2. **Apply V104 to Production** in the same deploy that carries V103 (both are pending on Production
   only), then add `cardseed:<name>` to Production's `SUMMIT_IMPORT_TEMPLATES` (D-101) before the
   first Production `cardseed` push.
3. **T245** — test card issuance timing against a real card-enabled enrollment once SSA can issue
   cards; this is the one thing the 2026-09-12 live verification could not settle.
4. Lower priority, not blocking: read the remaining four templates' settings blocks; give LA-26 its
   F3 qualifier (needs a run scoped to include `legal_assumptions.md`).

## SQL close-out audit

- **Produced this session (build run only): V104 only** —
  `docs/migrations/V104__plan_template_map_card_issuer.sql`, one `ALTER TABLE ... ADD COLUMN`
  guarded by `information_schema.COLUMNS` + `PREPARE`/`EXECUTE` (V103's own pattern), plus the
  `schema_info` view and `schema_version` self-registration. Registered in
  `docs/analysis/migration_tracker.md` and `docs/schema_version_migration.sql`. **This close-out pass
  produced no SQL of its own** — only prose edits to the tracker.
- **Run:** V104 against `beta_ssa` (work), by Kevin, as a precondition for the admin-screen checkbox
  and the live Summit verification to have worked at all — not run by Claude, and not independently
  confirmed by a `schema_version` probe in any of these three runs (consistent with how V103's own
  local application is recorded).
- **Recommended, not scripted:** none.
- **Known orphan, re-reported, not fixed:** `docs/migrations/seed_ndt125_questionnaire.sql` — not a
  `V`-file, registered nowhere. Unchanged by this chain.
- **Current highest version:** V104, on disk, in the tracker header, and in
  `schema_version_migration.sql` — all three agree.
- **Pending deployment:** V103 and V104, both on Production only, as of this pass's tracker
  correction. `dev_ssa` and `beta_ssa` (home) are recorded N/A for both, not pending — they are not
  environments Kevin runs either migration against.
- **Schema described but not scripted:** none.

## Compliance statement

- Files created: 1 — this close-out.
- Files modified: 5, all named in the fence — `docs/analysis/summit_import_templates_reference.md`,
  `docs/analysis/project_backlog.md`, `docs/deployment_backlog.md`,
  `docs/analysis/migration_tracker.md`, `docs/analysis/spec_card_issuer_seed_election.md` (status
  line + §9 live verification). All docs; no code, JSP, SQL or migration file touched.
- SQL produced, run, or connected to: none this pass.
- Git mutations run: none (read-only `git log`, `git status`, `git diff --stat`, `git diff --cached`).
- `.idea/artifacts/ams_war_exploded.xml`: not touched.
