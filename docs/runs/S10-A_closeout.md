# S10-A close-out — Phase A: the Proposal Builder ICHRA interjection

**Date:** 2026-08-03 · **Branch:** `refactor/modernize-architecture` · **Type:** Phase A investigation, spec output
**Deliverable:** [docs/analysis/phase_a_builder_interjection.md](../analysis/phase_a_builder_interjection.md)

---

## 1. Preflight output, verbatim

```
$ git rev-parse --abbrev-ref HEAD
refactor/modernize-architecture

$ git log -1 --format="%h %ci %s"
a517141 2026-08-02 23:10:23 -0500 S9-Z: session 9 close-out

$ git status --short
(empty)

$ git pull --ff-only
Already up to date.
=== exit: 0
```

HEAD after pull: **`a517141`**. Ancestry check `git merge-base --is-ancestor 091ed99 HEAD` → **passes**;
`091ed99` is the parent of `a517141`, so HEAD is *after* the required floor. All four hard-stop conditions
clear: correct branch, clean tree, `--ff-only` succeeded, checkout not stale.

---

## 2. Answers to Q1–Q5

**Q1 — T9: build around it. Reversal cost nil.**
The question's premise was wrong, and that is the run's most useful finding. `GenerateProp25`
(`src/.../controller/activity/setup/GenerateProp25.java:39`) is **not** the Proposal Builder — it is the
PSP-staff **Manual Setup** servlet, 403-gated by `isPspStaff()` (`:48-59`), reached only from three internal
JSPs. Its hardcoded region is `:212-223` (LOS ids 5,6,7,9,10,8) and `:254-269` (ServiceItem ids 11–19), and
it *writes* `proposal.getLosList()` at `:229` rather than never touching it. The actual builder is
`ProposalBuilder.java:32`, whose `createProposal` (`:340-410`) is already fully dynamic —
`request.getParameterValues("losIds")` at `:394-406` — with no id literal anywhere in the file. The
interjection therefore never traverses the hardcoded path, and "build around" costs nothing to reverse
because there is nothing to build around. T9 stays open, unchanged, and ships alone. Its backlog row's own
wording was corrected in place this run (two falsified claims).

**Q2 — LOS selection is knowable client-side at the moment of selection; no round trip needed.**
`proposalBuilder.jsp:161-175` renders one `.los-card-wrapper` per LOS with
`onclick="toggleLos(this, ${los.getId()})"`; `toggleLos` (`:469-482`) maintains a JS `Set` and
`rebuildLosInputs()` (`:484-494`) regenerates hidden `losIds` inputs on every change. The three numbered
"steps" are **badges on a single page** (`updateSteps`, `:496-506`), not sequential pages — so there is no
existing multi-step flow to ride, but there *is* a single `<form>` (`:54`) and a single submit-enable gate,
which is a better host than a wizard. AJAX probe (`fetch` / `XHR` / `$.ajax`, not just `<form action=>`):
the page issues **none** today. The interjection's `/IchraZipLookup` call would be the first, and that
endpoint already exists and is already entitlement-gated.

**Q3 — One POST, and the additive-optional-parameter mechanism already exists.**
`POST /ProposalBuilder` with `action=createProposal` (`ProposalBuilder.java:327-331`) → `createProposal`
(`:340`). Consumed today: `prospectId`, `rateId`, `sourceActivityId`, `losIds[]`, plus the illustration
hand-off set. The smallest possible diff is **one best-effort call placed after the `losIds` loop at
`:406`**, shaped exactly like the existing `attachIchraSnapshotIfPresent` (`:387-391`, implemented at
`:422-441`) — whose own comment already states the required standard: *byte-identical no-op for every
existing caller and every existing LOS*. With the `intake*` parameters absent the new method returns before
doing anything, which covers every existing entry point, every non-plus-tier LOS, and every unentitled agent.

**Q4 — `IchraAccessResolver.isAvailable(EntityManager, HttpServletRequest)`; no new signature.**
`IchraAccessResolver.java:44-97` — session-scoped, live per request, fails closed, never throws, resolving
PSP-admin → primary agency → full `AgencyScope.detailAgencyIds()` membership. Already the method
`IchraZipLookup`, `IllustrationServlet` and `IchraHome` call, so all four agree by construction.
`isAvailableForProposal` (`:145`) is inapplicable — no `Proposal` exists at builder time, and it
deliberately carries no PSP-admin bypass because its audience is the public page. `isAvailableForNav`
(`:189`) is explicitly **not** an authorization check; its own javadoc forbids this use.

**Q5 — New 1:1 side table `proposal_ichra_intake`, migration V087.**
Candidates actually found in the model: columns on `proposal` (`Proposal.java:16-60`); reuse of
`proposal_ichra_snapshot` (`V079__proposal_ichra_snapshot.sql:39-61`, entity at
`ProposalIchraSnapshot.java:33-96`); no generic key/value attribute table exists to reuse. **Reuse is
rejected on compliance grounds** — V079 is `NOT NULL` on `county_fips`, `state`, `county_name`, `plan_year`
and **`source_env`**, and `ProposalBuilder:462-470/521-523` refuses to write unless PRODUCTION rate rows
exist; intake is *input* and must persist for a county with no warmed rates, which is most Texas counties
today. Making `source_env` nullable would void the `PRODUCTION` fail-closed check `ViewProposal` depends on.
Columns on `proposal` are rejected because it is the hottest entity on the sales path. The side table
mirrors the sibling pattern V079 already set, reverses by `DROP TABLE`, and survives Proposal → Application
→ Setup because `Application`'s PK *is* `proposal_id`. Migration body is written as **text in the spec §4**;
no `.sql` file was created. Data scope: employer ZIP, chosen county, eligible-employee headcount, entered by
an agent about a prospect employer — **no employee-level data, no PHI, no SSN**, nothing a natural person
enters about themselves.

---

## 3. Files written

Three, all documentation:

| Path | New/existing |
|---|---|
| `docs/analysis/phase_a_builder_interjection.md` | new — the spec |
| `docs/analysis/project_backlog.md` | existing — T9 corrected in place; T125, T126 filed |
| `docs/runs/S10-A_closeout.md` | new — this file (§6 of the run brief) |

`git status --short` at close-out, before staging:

```
 M docs/analysis/project_backlog.md
?? docs/analysis/phase_a_builder_interjection.md
?? docs/runs/
```

**Nothing under `src/` and nothing under `docs/migrations/` appears — proven by the above, not asserted.**

---

## 4. ⚠️ Code-verified-only disclosure

**Every conclusion in this run rests on reading code. Nothing was executed, deployed, or observed at
runtime.** No compile was run, no server was started, no database was queried. The list, in full, so the
build prompt knows exactly which of its foundations have never run:

1. That `GenerateProp25` 403s a non-PSP-staff caller — read from the guard, never exercised.
2. That the interjection's panel can be revealed with no round trip — inferred from `toggleLos` maintaining
   client state; never executed in a browser.
3. That `attachIchraSnapshotIfPresent` is a true no-op when its parameters are absent — read from the early
   `return` at `:425-427`, and from its comment's claim, which is itself a code-verified claim.
4. That `IchraAccessResolver.isAvailable` fails closed on every path — read from the try/catch and the
   `return false` branches. The **admin** path of the S9-F/S9-G guards was runtime-verified in production;
   this resolver's **non-entitled** path was not, and cannot be from production alone.
5. That `/IchraZipLookup` returns the JSON shape the panel would parse — read from `writeResolution`
   (`:89-110`); never called.
6. That `proposal_ichra_snapshot`'s `NOT NULL` columns block reuse — read from V079's DDL; the table was
   never inspected in a live schema.
7. That V086 is applied nowhere / anywhere — **not determined at all.** See §5.
8. That `LOS.isPlusTier()` has no behavioural reader — established by grep over `src/`, which is strong for
   Java but would miss a reader constructed by string in a JSP EL expression.
9. That the proposed `V087` DDL executes cleanly against the live schema — **never run.**
10. The parameter-name collision on `headcount` — read from `proposalBuilder.jsp:61-77` against
    `ProposalBuilder`'s `getParameter` calls. The collision is real in the source; the *symptom* was never
    reproduced.

---

## 5. SQL close-out audit

- **SQL statements produced by this run: none as a file.** One migration body (`V087__proposal_ichra_intake.sql`)
  is written **as text inside** `docs/analysis/phase_a_builder_interjection.md` §4, deliberately, per the run
  brief. It is a recommendation, not a script.
- **SQL run: none.** No read query, no write query.
- **In a versioned migration:** N/A — nothing was scripted.
- **Orphaned `.sql` files created by this run: none.** Pre-existing unversioned `.sql` files, unchanged and
  not touched: `docs/migrations/seed_ndt125_questionnaire.sql` (in the migrations directory but carrying no
  `V` version — pre-existing, noted not fixed), `docs/schema_version_migration.sql`,
  `docs/updates/update_V039_to_V057.sql`, `docs/importscript/beta_ssa_baseline_v031.sql`,
  `docs/importscript/beta_ssa_dev_baseline_thru_V024.sql`.
- **Current highest version — `ls docs/migrations/`:** **V086** (`V086__los_plus_tier.sql`); 63 versioned
  scripts total.
- **`SELECT MAX(version) FROM schema_version` locally: NOT RUN — and this is a container limitation, not a
  fact about any environment.** There is no `mysql` client on PATH in this Claude Code container and no
  database connection from it. ⚠️ **Do not read this as "V086 is unapplied."** Session 6 made exactly that
  inference and it was wrong; the authoritative per-environment state is
  `docs/analysis/migration_tracker.md`, and only Kevin can probe a live `schema_version`.
- **Pending deployment:** nothing from this run. Carried forward from session 9 and unchanged here —
  `v0.86.01` (six admin guards, `820d027`) is built and pushed but not released.
- **Schema described but not scripted:** `proposal_ichra_intake` / **V087**. This is intentional and is the
  single most important handoff in the run — Q5 was to be *decided*, not built. **The build run creates the
  `.sql` file**, then updates `docs/analysis/migration_tracker.md` and `docs/schema_version_migration.sql`
  per CLAUDE.md migration discipline.

---

## 6. Backlog rows filed or updated

T-numbers assigned by reading `docs/analysis/project_backlog.md` directly — highest existing was **T124**.

| T | Action |
|---|---|
| **T9** | **Updated in place.** Two of its own claims falsified with file/line evidence: it *writes* `getLosList()` rather than never reading it, and it is PSP-staff-only Manual Setup, not a customer-facing builder. Recorded that T9 is **not on the interjection's path** and that S10-A decided build-around at nil reversal cost. Status now reads "de-scoped from the plus-tier build; still open on its own." |
| **T125** | **Filed — HIGH.** Build the interjection. Spec-complete, points at `phase_a_builder_interjection.md`. Carries the two hard requirements (the `headcount` name collision; never auto-pick `counties[0]`), the note that only gate links 5–6 are real authorization, and the three named hard-stops. |
| **T126** | **Filed — MED.** The renderer that consumes `proposal_ichra_intake`, filed explicitly so T125's build prompt cannot grow into it. Depends on T122 being decided first. |

---

## 7. Note to Kevin — one-time data entry, not work items

- **No LOS row anywhere carries `is_plus_tier = 1`.** The Service Manager checkbox exists
  (`serviceManager25.jsp:890-892` → `ServiceManagerAction.java:125`), so this is one click on one LOS. T125
  can be built and compiled without it; it cannot be **demonstrated** without it.
- **The demo agent's agency needs `ichra_enabled`** (the T61 checkbox) — same requirement as every other
  ICHRA surface, presumably already true for the SWBD demo agency, but the interjection is invisible without it.
- **The demo employer's ZIP should resolve to a county with warmed rates**, or the panel will honestly label
  the county unpriced. `/IchraZipLookup` reports this per-county via its `priced` flag *before* the click, so
  it degrades gracefully — but a demo reads better on a priced county.
- **The ZIP crosswalk is Texas-only** (V085). A non-Texas demo ZIP returns zero counties, which the panel
  renders as "we don't have that ZIP" — correct behaviour, poor demo.

---

## 8. Compliance statement

**Scope fence, restated.** Readable: anything in the repository. Writable: exactly
`docs/analysis/phase_a_builder_interjection.md`, `docs/analysis/project_backlog.md`, and — per §6 of the run
brief, which explicitly extends the §2 fence — `docs/runs/S10-A_closeout.md`. **Nothing outside that set was
written.** Creating `docs/runs/` was required to place the third file; it contained nothing prior.

**Prohibitions, each observed:** no file under `src/` modified; no file under `docs/migrations/` created,
modified or deleted; no SQL of any kind run, writing or reading; `docs/ichra_strategy.md`,
`docs/swbd_ichra_build_plan.md` and `docs/analysis/plus_tier_build_plan.md` untouched and unopened for
writing; no `git add -A` or `git add .` — every path staged explicitly by name; no tag, stash, checkout,
restore or reset.

**Files changed** — from `git show --stat` on the close-out commit, recorded below after the push.

**Git operations run this session:** the four read-only preflight commands (`rev-parse`, `log`, `status`,
`pull --ff-only`), plus `merge-base --is-ancestor` and `log --oneline` as read-only ancestry checks, and the
close-out `add` / `commit` / `push` / `log`. No other git operation of any kind.

**No code was written this run.** Every Java and JSP fragment appearing in the spec is a *description of a
diff to be made by a later run*, inside a Markdown document. No `.java`, `.jsp`, `.xml`, or `.sql` file was
created or edited.

---

## 9. Commit

Recorded from `git log -1` **after** the push, never carried from the prompt:

```
COMMIT_HASH_RECORDED_BELOW
```
