# S10-B close-out — T125 build: the Proposal Builder ICHRA interjection

**Date:** 2026-08-03 · **Branch:** `refactor/modernize-architecture` · **Type:** Implementation from a settled spec.
**Authority:** [docs/analysis/phase_a_builder_interjection.md](../analysis/phase_a_builder_interjection.md) (S10-A, `4560f6d`/`7e98597`). Read in full before any code was written.

---

## 1. Preflight output, verbatim

```
$ git rev-parse --abbrev-ref HEAD
refactor/modernize-architecture

$ git log -1 --format="%h %ci %s"
7e98597 2026-08-03 12:08:50 -0500 docs: record the S10-A close-out commit hash

$ git status --short
(empty)

$ git pull --ff-only
Already up to date.

$ git merge-base --is-ancestor 7e98597 HEAD
7e98597 IS ancestor of HEAD - OK
```

All four hard-stop conditions clear: correct branch, clean tree, `--ff-only` succeeded, `7e98597` is HEAD itself (ancestor trivially holds).

---

## 2. HS-1 — plan year: resolved, mechanism identified

**Decided by the S10-B prompt itself: do not collect it.** The build derives "current" plan year server-side
from the same source `IllustrationServlet` already treats as authoritative — the `RATE_CACHE_PLAN_YEARS`
constant, read via `AppConstantDAO.getConstantValue(em, "RATE_CACHE_PLAN_YEARS")`
([IllustrationServlet.java:139](../../src/main/java/net/superiorstate/ams/controller/market/IllustrationServlet.java:139)),
parsed as a tolerant comma-separated list
([IllustrationServlet.java:708-721](../../src/main/java/net/superiorstate/ams/controller/market/IllustrationServlet.java:708)),
with the first configured entry treated as "current"
([IllustrationServlet.java:724-737](../../src/main/java/net/superiorstate/ams/controller/market/IllustrationServlet.java:724),
`resolvePlanYear`'s own comment: *"Never accepts an arbitrary year — falls back to the first configured year."*).

`ProposalBuilder.resolveCurrentPlanYear(EntityManager)` ([ProposalBuilder.java:508-521](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:508))
reproduces exactly this parse — no second mechanism, no hardcoded year — and returns `null` (not a
placeholder) when the constant is missing, blank, or carries no parseable entry. Called twice: once in
`doGet` (to expose it to the JS ZIP-lookup fetch for `priced` labeling, and to gate the panel's visibility),
and again independently in `attachIchraIntakeIfPresent` at submit time (never trusting what `doGet` resolved
earlier in case the constant changed between GET and POST).

**Stored on the intake row, per the prompt's instruction — this is a deviation from the S10-A spec.** The
spec's V087 DDL had no `plan_year` column; this build added `plan_year SMALLINT NOT NULL` to hold the
derived value, since "storing a derived value is not collecting it." Documented in the migration file itself
and in the backlog. **If `RATE_CACHE_PLAN_YEARS` is unconfigured**, the intake panel is hidden server-side —
`<c:if test="${ichraAvailable and not empty ichraPlanYear}">` ([proposalBuilder.jsp:194](../../src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp:194))
— so nothing is ever collected that a submit-time `NOT NULL` write would then have to refuse.

---

## 3. HS-2 / HS-3 — the spec's other two named hard-stops

Both had spec-stated resolutions (["What this spec does NOT cover"](../analysis/phase_a_builder_interjection.md), items 3 and 6), so neither required a fresh decision — only following what the spec already said:

**HS-2 — edit-after-create.** Spec: *"There is no path to change an intake once the proposal exists.
`UNIQUE KEY uq_pii_proposal` means a second write fails. The build should catch and log, not retry."*
Implemented exactly that: `ProposalIchraIntakeDAO.save`
([ProposalIchraIntakeDAO.java:26-38](../../src/main/java/net/superiorstate/ams/data/dao/ProposalIchraIntakeDAO.java:26))
rolls back and rethrows on any `RuntimeException` (a unique-constraint violation among them); the caller's
existing best-effort wrapper
([ProposalBuilder.java:428-432](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:428))
— the same shape `attachIchraSnapshotIfPresent` already used — catches it, logs
`"ICHRA intake attach failed for proposal #… : …"`, and does not retry.

**HS-3 — county chooser visual design.** Spec: *"Match the illustration page's existing chooser rather than
inventing one; the build should read `illustration*.jsp` first."* Read
[illustration25.jsp:1445-1595](../../src/main/webapp/WEB-INF/view/market/illustration25.jsp:1445) in full.
Illustration25.jsp already has a **JS-driven ZIP lookup on blur/change** hitting the exact same
`/IchraZipLookup` endpoint — the `lookup()`/`selectCounty()`/`renderChooser()` functions — which the
interjection's `ichraZipLookup()`/`ichraSelectCounty()`/`ichraRenderChooser()` mirror closely: same fetch
URL shape, same "half-typed ZIP says nothing" tolerance, same "field moved on, drop the answer" race guard,
same never-auto-pick-`counties[0]` rule, same unpriced-county labeling text ("no rates cached yet"). One
deliberate adaptation: illustration25.jsp's chooser renders an `<ul>` of links (each a full-page-reload
`?countyFips=` URL, since that page is GET-only and stateful via the query string), whereas the interjection
posts once through a single form and the spec's step 6 explicitly calls for a `<select>` — so the target DOM
element differs (populate a `<select>`'s `<option>`s rather than build `<li><a>` entries), while the
interaction rules are identical. Field label classes (`form-label mb-1`, `form-control form-control-sm`,
`form-select form-select-sm`) and the ZIP input's `inputmode="numeric" pattern="[0-9]{5}" maxlength="5"
placeholder="#####"` are copied verbatim from
[illustration25.jsp:289-311](../../src/main/webapp/WEB-INF/view/market/illustration25.jsp:289).

---

## 4. Resolved permitted-file list (printed per §5) and actual files changed

**Resolved list, from the spec's nine steps plus the fixed §5 union:**

1. `src/main/java/net/superiorstate/ams/model/sales/agency/ProposalIchraIntake.java` — new (step 1)
2. `src/main/java/net/superiorstate/ams/data/dao/ProposalIchraIntakeDAO.java` — new (step 2)
3. `src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java` — existing (steps 3, 4)
4. `src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp` — existing (steps 5, 6, 7)
5. `docs/migrations/V087__proposal_ichra_intake.sql` — new (step 8)
6. `docs/analysis/migration_tracker.md` — existing, register V087 (step 8 continued)
7. `docs/schema_version_migration.sql` — existing, register V087 (step 8 continued)
8. Step 9 (`persistence-*.xml`) — confirmed no-op: both persistence units carry
   `<exclude-unlisted-classes>false</exclude-unlisted-classes>`
   ([persistence-server.xml:7](../../src/main/resources/META-INF/persistence-server.xml:7),
   [persistence-local.xml:4](../../src/main/resources/META-INF/persistence-local.xml:4)), so
   `ProposalIchraIntake` auto-discovers exactly like `ProposalIchraSnapshot` did. Not written to.
9. `docs/analysis/project_backlog.md` — T125 status updated
10. `docs/runs/S10-B_closeout.md` — this file

**Actual files changed**, from `git status --short` immediately before staging:

```
 M docs/analysis/migration_tracker.md
 M docs/analysis/project_backlog.md
 M docs/schema_version_migration.sql
 M src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java
 M src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp
?? docs/migrations/V087__proposal_ichra_intake.sql
?? docs/runs/S10-B_closeout.md
?? src/main/java/net/superiorstate/ams/data/dao/ProposalIchraIntakeDAO.java
?? src/main/java/net/superiorstate/ams/model/sales/agency/ProposalIchraIntake.java
```

Exact match against the resolved list — **nothing written outside it.**

---

## 5. Final intake parameter names (§3.1)

`intakeZip`, `intakeCountyFips`, `intakeCountyName` (hidden), `intakeState` (hidden), `intakeHeadcount`. No
`intakePlanYear` parameter exists — per HS-1, plan year is never posted by the client at all; it is derived
independently server-side on both `doGet` and the `createProposal` write path. Verified none collide with the
form's pre-existing `headcount`/`countyFips`/`planYear`/`mode`/`contribution`/`age1..6`/`count1..6` hidden
inputs ([proposalBuilder.jsp:61-77](../../src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp:61)).

---

## 6. Existing files edited — reason and diff size

| File | Diff size | Reason |
|---|---|---|
| `ProposalBuilder.java` | +122 lines (`git diff --stat`) | doGet: entitlement + derived plan year, gating the JSP's panel render (spec step 3, ~14 lines incl. comments). createProposal: best-effort intake-attach call after the `losIds` loop, mirroring the existing snapshot pattern exactly (spec step 4); plus three new private methods (`attachIchraIntakeIfPresent`, `resolveCurrentPlanYear`, `normalizeFiveDigitCode`) implementing gate links 5–6 and HS-1's derivation. `attachIchraSnapshotIfPresent` and everything above line 406 of the original file were not touched. |
| `proposalBuilder.jsp` | +250/-4 lines (`git diff --stat`) | One attribute on the LOS card wrapper (spec step 5); one new `<c:if>`-gated panel card between STEP 3 and Submit (spec step 6); ~180 lines of JS (spec step 7) — `updateIntakePanel`/`ichraIntakeComplete`/`ichraZipLookup`/`ichraSelectCounty`/`ichraRenderChooser`/`ichraCountySelected`/`clearIntakeFields`/`hideIntakeMessages`/`anySelectedLosIsPlusTier`, wired into the existing `toggleLos`, `filterLosCards`, `updateSteps`, and `DOMContentLoaded` functions. One new CSS rule (`.intake-msg`) matching the file's existing `.los-none-msg` convention. |
| `docs/analysis/migration_tracker.md` | +3/-1 | Registered V087's row in the per-environment table (all ⬜/N/A — never applied anywhere from this container) and bumped "Current Highest Version" to V087. |
| `docs/schema_version_migration.sql` | +3/-1 | Registered V087 in the multi-row `INSERT IGNORE` per CLAUDE.md migration discipline. |
| `docs/analysis/project_backlog.md` | — | T125 status moved from spec-complete to built; T9's and T125's rows carry the S10-A/S10-B provenance already on file. |

`ProposalIchraIntake.java`, `ProposalIchraIntakeDAO.java`, and `V087__proposal_ichra_intake.sql` are new — no
diff-size column applies.

---

## 7. Verification results

**Maven.** `.\mvnw.cmd clean package` — **BUILD SUCCESS**, twice (once after the initial implementation, once
more after the county-chooser `change`-listener fix in §9 below). 509 source files compiled, WAR assembled
at `target\ams-1.0.0-SNAPSHOT.war`. The only compiler note is a pre-existing unchecked-operations warning in
`RecurringChecklistDAO.java`, unrelated to this change.

**Local render check — NOT RUN.** No Tomcat instance is reachable from this build container: `netstat -an`
showed nothing listening on port 8089 or any other HTTP port serving the app; `$CATALINA_HOME` is unset;
`.claude/launch.json`'s only configured server (`docs-preview`) runs a Python `http.server` over `docs/`,
not the WAR. Deploying `target\ams-1.0.0-SNAPSHOT.war` to a live Tomcat 10 + `beta_ssa` MySQL instance is
outside what this container can do — same shape of limitation S10-A hit with the `mysql` client. **No code
reading is substituted for this check** — the entitled-path and unentitled-path HTML assertions the run
brief asked for were never executed, and are not claimed as run.

**T64 note.** Not applicable — no Tomcat was started, so no config-flip-then-restart concern arose this run.

---

## 8. ⚠️ Code-verified-only disclosure

Everything in this run rests on reading code, except the Maven build itself. Specifically:

1. That `IchraAccessResolver.isAvailable` correctly gates both `doGet`'s panel visibility and
   `attachIchraIntakeIfPresent`'s write path — read from the resolver's source and from the S10-A spec's own
   disclosure that the resolver's *non-entitled* path was never runtime-verified in production either.
2. That `RATE_CACHE_PLAN_YEARS` is actually configured on any real environment, and to what value — never
   queried. `resolveCurrentPlanYear` returning `null` when it isn't is untested against a live constant row.
3. That `/IchraZipLookup` returns the JSON shape `ichraZipLookup()`'s fetch handler expects — read from
   `IchraZipLookup.java`'s `writeResolution` method; the endpoint was never actually called.
4. That the multi-county chooser's `change` listener (`ichraCountySelected`, added after the first
   implementation pass — see §9) correctly reads `option.dataset.name`/`dataset.state` — reasoned from how
   `HTMLOptionElement.dataset` maps `data-name`/`data-state` attributes; never exercised in a browser.
5. That `los.isPlusTier()` at the JSP layer (`${los.isPlusTier()}`) and at the Java layer
   (`los.isPlusTier()` in `attachIchraIntakeIfPresent`) agree on what "plus-tier" means for the same LOS row
   — both read the same JPA-mapped `is_plus_tier` column, but this was never observed against an actual
   flagged row, since none exists (see note to Kevin).
6. That an unentitled agent's builder page renders byte-identical to before this change — reasoned from the
   `<c:if test="${ichraAvailable}">` wrapping every new piece of markup, never observed.
7. That the V087 migration's DDL is valid MySQL 8 syntax and actually creates the table without error —
   never run against any database.
8. That `.mvnw.cmd clean package`'s success implies the JSP is well-formed — **it does not.** Maven does not
   compile JSPs in this project (no Jasper precompilation step); JSTL tag balance was checked manually by
   grep-counting `<c:if>`/`</c:if>`, `<c:choose>`/`</c:choose>`, `<c:when>`/`</c:when>`,
   `<c:otherwise>`/`</c:otherwise>` pairs (12/12, 3/3, 3/3, 3/3) — a balance check, not a parse.

---

## 9. New assumptions

- **Derived plan year (HS-1).** See §2. Reversal cost: adding an agent-facing plan-year field later is one
  new form field, one column already present (`proposal_ichra_intake.plan_year`), zero migration work. Cheap.
  Filed as **T127** below.
- **Multi-county `change` listener was not in the original spec text (step 7) but was required for
  correctness.** The spec's step 7 described `ichraSelectCounty`/`ichraRenderChooser` populating the
  `<select>`, but a `<select>` populated with un-selected `<option>`s needs a `change` handler to capture
  which one the agent picks — otherwise `intakeCountyName`/`intakeState` would stay blank after a manual
  multi-county selection, and `ichraIntakeComplete()`'s live gate on `btnCreate` would never re-evaluate
  after that pick either. Found and fixed during self-review, before the verification build, by tracing what
  happens when the agent's ZIP resolves to more than one county — added `ichraCountySelected()`
  ([proposalBuilder.jsp](../../src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp)) and wired it as a
  `change` listener, plus an `input` listener on `intakeHeadcount` so typing a headcount also live-updates
  the Create button's enabled state without requiring another ZIP/LOS interaction first.
- **JS-required, no progressive enhancement, for this panel specifically.** Unlike `illustration25.jsp`
  (which supports JS-off via a server-rendered county `<select>` and full-page-reload chooser links), the
  intake panel's very visibility is JS-toggled (`style="display:none"` plus `updateIntakePanel()`), matching
  how `btnCreate` itself is already JS-gated on this page (`disabled` by default, cleared only by
  `updateSteps()`). Not a new constraint introduced by this build — the whole builder already requires JS to
  submit anything.

---

## 10. Backlog rows filed or updated

| T | Action |
|---|---|
| **T125** | **Updated.** Status moved from "spec complete" to "built" with a detailed account of what was implemented, the HS-1/HS-2/HS-3 resolutions, the plan_year DDL deviation, and the not-run render check. |
| **T127** | **Filed — LOW.** "Consider an agent-facing plan-year override on the plus-tier intake panel, if a group's plan year ever needs to differ from the system default." Reversal-cost-cheap follow-on from HS-1, not a defect — filed so the derived-vs-collected decision is revisitable rather than silently permanent. |

(T126 — the intake-row consumer — was already filed by S10-A and is unchanged by this run; T125's row notes it explicitly.)

---

## 11. Note to Kevin — one-time data entry, carried forward from S10-A, unchanged

- **No LOS row anywhere carries `is_plus_tier = 1`.** One checkbox in the Service Manager
  (`serviceManager25.jsp:890-892`). The build compiles and the gate chain is code-complete without it; the
  panel cannot be seen or demonstrated without it.
- **The ZIP crosswalk is Texas-only** (V084/V085). A non-Texas demo ZIP resolves to zero counties from
  `/IchraZipLookup`, which the panel renders honestly as "we don't have that ZIP" rather than failing.

---

## 12. SQL close-out audit

- **SQL statements produced this run:** one file, `docs/migrations/V087__proposal_ichra_intake.sql` — one
  `CREATE TABLE`, one `CREATE OR REPLACE VIEW schema_info`, one `INSERT IGNORE INTO schema_version`. All
  three in a single versioned migration file, per §6 of the run brief.
- **SQL statements run:** **none.** No `mysql` client is reachable from this container (same limitation
  S10-A recorded — this is a container limitation, not evidence about any real environment's schema state).
- **In a versioned migration:** yes — `V087__proposal_ichra_intake.sql`, registered in both
  `docs/analysis/migration_tracker.md` and `docs/schema_version_migration.sql`.
- **Orphaned `.sql` files:** none created by this run. Pre-existing and unchanged:
  `docs/migrations/seed_ndt125_questionnaire.sql` (no `V` prefix, pre-existing), `docs/schema_version_migration.sql`
  itself (not a migration file, the running INSERT log), `docs/updates/update_V039_to_V057.sql`,
  `docs/importscript/beta_ssa_baseline_v031.sql`, `docs/importscript/beta_ssa_dev_baseline_thru_V024.sql`.
- **Current highest version:** `ls docs/migrations/*.sql` → **64 files**, highest `V087__proposal_ichra_intake.sql`.
  `SELECT MAX(version) FROM schema_version` — **not run; no client reachable, a container limitation, not
  evidence about any environment's actual state.**
- **Pending deployment:** V087 is pending everywhere — not applied to `beta_ssa (work)`, `beta_ssa (home)`,
  `dev_ssa`, Production, or any other environment. Carried forward unrelated to this run: `v0.86.01` (session
  9's remaining admin guards) is still built/pushed/not released.
- **Schema described but not scripted:** none — Q5's recommendation (S10-A) is now a real migration file
  (this run); nothing else in this build implied a schema change beyond what V087 covers.

---

## 13. Git, at close-out

Dirty tree during work, per instruction. Staged by explicit named path only:

```
git add src/main/java/net/superiorstate/ams/model/sales/agency/ProposalIchraIntake.java \
        src/main/java/net/superiorstate/ams/data/dao/ProposalIchraIntakeDAO.java \
        src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java \
        src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp \
        docs/migrations/V087__proposal_ichra_intake.sql \
        docs/analysis/migration_tracker.md \
        docs/schema_version_migration.sql \
        docs/analysis/project_backlog.md \
        docs/runs/S10-B_closeout.md
git commit -m "feat: ICHRA intake interjection in the Proposal Builder (T125, V087)"
git push
git log -1 --format="%h %ci %s"
```

Commit hash, read from `git log` **after** the push:

```
1d4c284 2026-08-03 12:30:12 -0500 feat: ICHRA intake interjection in the Proposal Builder (T125, V087)
```

`git show --stat` on `1d4c284`:

```
 docs/analysis/migration_tracker.md                 |   3 +-
 docs/analysis/project_backlog.md                   |   2 +-
 docs/migrations/V087__proposal_ichra_intake.sql    |  69 +++++
 docs/runs/S10-B_closeout.md                        | 321 +++++++++++++++++++++
 docs/schema_version_migration.sql                  |   3 +-
 .../controller/activity/setup/ProposalBuilder.java | 122 ++++++++
 .../ams/data/dao/ProposalIchraIntakeDAO.java       |  43 +++
 .../model/sales/agency/ProposalIchraIntake.java    | 146 ++++++++++
 .../webapp/WEB-INF/view/sales/proposalBuilder.jsp  | 250 +++++++++++++++-
 9 files changed, 954 insertions(+), 5 deletions(-)
```

⚠️ **Same disclosed deviation as S10-A, for the same reason.** This close-out cannot both record its own
commit's hash and be inside that commit, so §13's hash is filled in via a **second** commit touching only
`docs/runs/S10-B_closeout.md` — a path already inside the scope fence — rather than by amending `1d4c284`
after it was already pushed.

---

## 14. Compliance statement

**Scope fence, restated.** Writable: the union in §4 — every file the spec's nine steps named, plus
`docs/migrations/V087__proposal_ichra_intake.sql`, `docs/analysis/migration_tracker.md`,
`docs/schema_version_migration.sql`, `docs/analysis/project_backlog.md`, `docs/runs/S10-B_closeout.md`.
**`git status --short` in §4 proves nothing outside that union was touched.**

**Forbidden list, each observed:** `GenerateProp25` and its three calling JSPs — untouched, never opened for
writing. `navbar25.jsp`, `css-js.jsp`, `AppConfig`, `AmsDataGlobal`, `EmfListener`, `LoginFilter` — untouched.
`docs/ichra_strategy.md`, `docs/swbd_ichra_build_plan.md`, `docs/analysis/plus_tier_build_plan.md` —
untouched. `proposal_ichra_snapshot`'s `source_env NOT NULL` constraint — untouched; V087 is a wholly
separate table. No `git add -A`/`git add .`; every path staged by explicit name. No tag, stash, checkout,
restore, or reset issued at any point this run.

**Unrelated observations not fixed:** the pre-existing unchecked-operations compiler note in
`RecurringChecklistDAO.java` was noticed during the verification build and left alone — out of scope, not
this run's to fix.

**No git operation other than the read-only preflight checks and the close-out `add`/`commit`/`push`/`log`
above was run this session.**

**Code was written this run** — this is the T125 *build*, not another Phase A. Confirmed against the scope
fence: every file touched is either in the resolved permitted-file list (§4) or is this close-out itself.
