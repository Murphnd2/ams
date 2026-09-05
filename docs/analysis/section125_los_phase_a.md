# Section 125 / ICHRA pilot — Phase A investigation

Read-only investigation. No application code, no SQL, no build, no database access. One file created (this one).

## Baseline

| | |
|---|---|
| Branch | `refactor/modernize-architecture` ✅ |
| Short hash | `5ac66a5` |
| Tree at start | **already dirty** — `M docs/analysis/migration_tracker.md`, `M docs/schema_version_migration.sql`, `?? docs/migrations/V092__hsa_enrollment_assistant.sql` |

Searches excluded `.claude/worktrees/**` (stale full-repo copies that duplicate every hit).

---

## Q1 — New LOS: data or code?

### 1. Where LOS is defined

`LOS` — [LOS.java:21-68](src/main/java/net/superiorstate/ams/model/sales/offering/LOS.java:21). Table `los`, PK `los_id`.
A LOS row carries only: `description`, `short_text`, `sort_order`, `suppressed`, `is_plus_tier` (V086),
FK `psp_id`, FK `service_item_id`, plus three collections (`losmodules`, proposals, enhancements).
**It carries no behavior, no type discriminator, no code hook.**

### 2. What attaches by association, not by code

| Thing | Verdict | Anchor |
|---|---|---|
| LOS-scoped **proposal** sections | ✅ association — `proposalsectionlos` join | [ProposalSection.java:40-44](src/main/java/net/superiorstate/ams/model/sales/offering/ProposalSection.java:40); scope filter [ViewProposal.java:292-318](src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:292) — a section whose `scope != 'SCOPED'` passes unconditionally, so `ALL` sections need **no** row |
| LOS-scoped **application** sections | ✅ association — `applicationsectionlos` join | [ApplicationSection.java:40-44](src/main/java/net/superiorstate/ams/model/sales/application/ApplicationSection.java:40); query [ApplyForProposal.java:508-513](src/main/java/net/superiorstate/ams/controller/activity/setup/ApplyForProposal.java:508) — `s.scope = 'ALL' OR los.id IN :losIds` |
| **Task sequences** | ✅ association, via ServiceItem — LOS→`ServiceItem`→`RequiredTaskList`→`TaskSequenceTable`→`Task` | [LOS.java:55-57](src/main/java/net/superiorstate/ams/model/sales/offering/LOS.java:55); [RequiredTaskList.java:11-13](src/main/java/net/superiorstate/ams/model/activity/checklist/sequences/RequiredTaskList.java:11); resolution [ApplicationTaskDAO.java:31-42](src/main/java/net/superiorstate/ams/data/dao/ApplicationTaskDAO.java:31). `TaskSequence` itself has **no** LOS field ([TaskSequence.java:10-28](src/main/java/net/superiorstate/ams/model/activity/checklist/sequences/TaskSequence.java:10)) |
| **Rate tables / rate visibility** | ✅ association, via ServiceModule — `RateTable`'s PK is (rate, priceItem, **module**); module→LOS | [RateTable.java:9-27](src/main/java/net/superiorstate/ams/model/sales/agency/RateTable.java:9); [ServiceModule.java:33-35](src/main/java/net/superiorstate/ams/model/sales/offering/ServiceModule.java:33). Rate *discounts* have their own `ratediscountlos` join — [RateDiscount.java:30-34](src/main/java/net/superiorstate/ams/model/sales/agency/RateDiscount.java:30) |
| **Service items / service modules** | ✅ **auto-created** by the admin UI — see item 4 | [ServiceManagerAction.java:67-91](src/main/java/net/superiorstate/ams/controller/activity/setup/ServiceManagerAction.java:67) |
| **Benefits created at setup** | ❌ **not present.** No `Benefit` row is created anywhere in the setup path. `new Benefit()` appears only in `EntityLookup`, `DatabaseInitializer`, `DemoDataSeeder` and the four import services (`SummitImportService`, `UniversalImportService`, `ImportCommitService`, `Updater`). `Setup` carries `CheckList` + `Application` + contacts and nothing else | [Setup.java:15-33](src/main/java/net/superiorstate/ams/model/activity/ticket/setup/Setup.java:15); [CreateSetup25.java:102-175](src/main/java/net/superiorstate/ams/controller/activity/setup/CreateSetup25.java:102) creates Proposal → LOS links → Application → one `ApplicationModule` per LOS's ServiceItem → CheckList → Setup → ToDos, **no Benefit** |

### 3. Every place that branches on a specific LOS identity

Sweep covered: `los.getId() ==`, `getLosId`, `LOS_ID`, `getShortText()` comparisons, `"ICHRA".equals`,
`switch` over LOS, numeric LOS literals in `.java` and `.jsp`, and `constant` rows naming a LOS.

| Location | What it does | A new LOS would… |
|---|---|---|
| `GenerateProp25.fillProposal` — [GenerateProp25.java:205-224](src/main/java/net/superiorstate/ams/controller/activity/setup/GenerateProp25.java:205) | Six fixed form questions `q1`–`q6` mapped to literal LOS ids **5, 6, 7, 9, 10, 8** | **Be excluded silently.** PSP-staff Manual Setup only ([manualSetup.jsp](src/main/webapp/WEB-INF/view/sales/manualSetup.jsp), [generateSetupForm25.jsp](src/main/webapp/WEB-INF/view/a/setup/generateSetupForm25.jsp)) — not the Proposal Builder, not the customer path |
| `GenerateProp.fillProposal` — [GenerateProp.java:143-162](src/main/java/net/superiorstate/ams/controller/activity/setup/GenerateProp.java:143) | Identical literal map; legacy twin ([generateSetupForm.jsp](src/main/webapp/WEB-INF/view/activity/setup/generateSetupForm.jsp)) | Same — excluded silently |
| LOS seeders — [DatabaseInitializer.java:482](src/main/java/net/superiorstate/ams/data/service/DatabaseInitializer.java:482), [DemoDataSeeder.java:182,192,202](src/main/java/net/superiorstate/ams/data/service/DemoDataSeeder.java:182), guard at [DatabaseInitializer.java:1490-1492](src/main/java/net/superiorstate/ams/data/service/DatabaseInitializer.java:1490) | Seed LOS at **explicit ids 1–4** on a fresh install | Not runtime branching, but it means **the LOS id space differs per installation** — the 5–10 mapping above is a Production fact, not a schema fact |
| `RequestQuote` — [RequestQuote.java:251](src/main/java/net/superiorstate/ams/controller/market/RequestQuote.java:251) | `los.getId() == losId` — matches a **submitted** id against the loaded list | Work normally; not identity branching |
| `CreateSetup25.fillToDoList` — [CreateSetup25.java:344-346](src/main/java/net/superiorstate/ams/controller/activity/setup/CreateSetup25.java:344) | If the resolved task list is empty, inserts one placeholder `Task` id **153**, marked complete | **Silently fall through** to that placeholder if the new LOS's ServiceItem has no `RequiredTaskList`. Degraded, not broken |

**No name-based, `shortText`-based, enum, or `switch`-on-LOS branch exists anywhere.** The one behavioral
flag on a LOS (`is_plus_tier`, V086) is read purely as data — [ViewProposal.java:555-565](src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:555)
states explicitly that it "reads no id literal" — and is the working precedent for adding LOS-level
behavior without id coupling.

### 4. Bottom line

**Creating a new LOS through Service Manager is a data-only act. Zero files need to change.** The
`createLos` handler ([ServiceManagerAction.java:54-111](src/main/java/net/superiorstate/ams/controller/activity/setup/ServiceManagerAction.java:54))
already does the wiring: it persists the LOS, **auto-creates** its linked `ServiceItem` (ActivityCategory 2 =
Setup, `sourceType = "MANUAL"`, `hasRequiredTasks = true`), **auto-creates** its linked `ServiceModule`, and
**auto-links every `ALL`-scoped `ApplicationSection`** for the PSP.

- **Works immediately:** appears in Proposal Builder / PSP Admin / Service Manager (`LOS.getByPsp`, suppressed
  filtered); selectable on a proposal; carried into `Application`, `ApplicationModule`, `CheckList`, `Setup`;
  picked up by every `ALL`-scoped proposal and application section; rate-table pricing works because
  [RateTableAction.java:97-110](src/main/java/net/superiorstate/ams/controller/activity/setup/RateTableAction.java:97)
  creates the module on demand if one is missing.
- **Needs an association row (admin UI, no code):** a `RequiredTaskList` under its ServiceItem — otherwise the
  Setup gets only the placeholder ToDo; any `SCOPED` proposal/application sections; `RateTable` price rows;
  `ratediscountlos` rows; enhancements via `enhancement_los`.
- **Needs code:** only if the new LOS must appear in the two legacy `GenerateProp*` Manual Setup forms.
  Nothing on the customer path.

**HARD STOP not triggered** — the threshold was "code changes in more than two files"; the actual count is zero.

---

## Q2 — Waiver state

**Not present.** Searched `.java`, `.jsp`, `docs/migrations/*.sql` and the baseline DDL for `waiv*`,
`declin*`, `opt.out`, `opted_out`, `elect_no`, `no_coverage`. The only hits are unrelated prose (an
EclipseLink cache comment at `BillingPipelineRunner.java:91`; ICHRA-notice "decline and route" prompt text in
`V080__ichra_design_advisor_skill.sql`). **No employee, participant, benefit, enrollment, application or
market entity carries a decline / waive / opt-out state.**

**Nearest existing concept, and the natural attachment point:**
`Enrollment` — [Enrollment.java:13-47](src/main/java/net/superiorstate/ams/model/summit/temp/Enrollment.java:13),
table `enrollment` ([baseline DDL lines 1801-1828](docs/importscript/beta_ssa_baseline_v031.sql:1801)). It is
the only per-employee-per-plan-per-plan-year row in the model: `participant_id` + `benefit_id` +
`benefit_year_id`, with `py_start` / `py_end` and `term_date`. **Cardinality is exactly right — per employee,
per plan, per plan year.**

Two structural caveats:

1. **Every FK on `enrollment` points at Summit *import staging* tables** — `semployee`, `sbenefit`,
   `sbenefityear`, `semployer` — not at the AMS-native `employee` / `benefit` / `employer`. It is populated
   by import, not originated by AMS.
2. `Employee` ([Employee.java:8-64](src/main/java/net/superiorstate/ams/model/summit/archive/Employee.java:8))
   carries `ee_status_id`, `system_status_id`, `cobra_status_id` — Summit-derived status codes,
   employer-scoped, **not plan-year scoped and not a waiver**. It has no direct link to `Benefit`.

The one §125-adjacent per-employee structure AMS *originates* is NDT:
`NdtTestRun.censusData` ([NdtTestRun.java:55-56](src/main/java/net/superiorstate/ams/model/activity/ndt/NdtTestRun.java:55))
— a `LONGTEXT` JSON blob scoped to one test run (activity + `plan_year_end`), not relational rows.

---

## Q3 — Per-employee allowance

1. **No entity represents a benefit as created at setup, because setup creates none.** See Q1 item 2.
   `Benefit` ([Benefit.java:13-70](src/main/java/net/superiorstate/ams/model/summit/archive/Benefit.java:13))
   is **employer-scoped, not employee-scoped**: FK `employer_id`, FK `plan_type_id`, with `effective_date`,
   `termination_date`, `plan_year_start`, `plan_year_end`, `next_renewal_due`, `renewal_months`. It carries
   effective dates and a plan-year boundary — but **no amount field of any kind**, per-employee or otherwise.
   Rows come only from the import services and the seeders.
2. **Cardinality: group-level.** One `Benefit` per employer per plan; the per-employee fan-out is
   `enrollment`. Nothing derives a per-employee amount from it. A repo-wide search for `allowance`,
   `monthly_amount`, `monthlyAmount`, `employerContribution` returns **zero hits** in `src/main/java`. The
   nearest per-employee money field is `ElectionAmount`, typed **`String`**, on Summit import staging rows
   ([sEnrollment.java:49-50](src/main/java/net/superiorstate/ams/model/summit/imports/sEnrollment.java:49),
   [ImportEnrollment.java:53](src/main/java/net/superiorstate/ams/model/summit/imports/order/ImportEnrollment.java:53))
   — import passthrough, not an AMS-modelled allowance.
3. **A monthly-per-employee allowance exists today in exactly one place, and it is not per-employee:**
   `ProposalIchraIntake.monthlyContributionPerEmployee` —
   [ProposalIchraIntake.java:62-63](src/main/java/net/superiorstate/ams/model/sales/agency/ProposalIchraIntake.java:62),
   column `proposal_ichra_intake.monthly_contribution_per_employee decimal(10,2) NULL` (V088). It is a
   **single uniform scalar per proposal** (`proposal_id UNIQUE`), sales-side, agent-entered, nullable, and it
   never crosses into setup or administration.

**Attachment point if a per-employee attribute were added later:** `Enrollment` (table `enrollment`) — the
same row as Q2's waiver, and the only place where employee, plan and plan year already meet. There is a hard
seam to cross first: `Prospect` ([Prospect.java:13-29](src/main/java/net/superiorstate/ams/model/sales/agency/Prospect.java:13))
and `Setup` have **no relational link of any kind** to `summit.archive.Employer`. The sales pipeline and the
administration side do not touch.

---

## Q4 — Carrier entity

**No.** There is no carrier table and no carrier entity. Carrier exists only as:

- a free-text `String` column named `Carrier` on three Summit **import staging** entities —
  [sBenefitTierPB.java:54](src/main/java/net/superiorstate/ams/model/summit/imports/sBenefitTierPB.java:54),
  [ImportBenefitTier.java:54](src/main/java/net/superiorstate/ams/model/summit/imports/order/ImportBenefitTier.java:54),
  [ImportCobPart.java:71](src/main/java/net/superiorstate/ams/model/summit/imports/order/ImportCobPart.java:71);
- an aggregate `Integer carrierCount` on
  [RatingAreaRateCache.java:58-59](src/main/java/net/superiorstate/ams/model/market/RatingAreaRateCache.java:58)
  — a count, never an identity.

The `model/market` package holds only `CountyReference`, `IllustrationLog`, `RatingAreaRateCache`,
`ZipCounty`. No `CREATE TABLE` matching carrier exists in `docs/migrations/` or the baseline DDL.

---

## Contradictions

1. **`docs/ichra_strategy.md:350` — decision D4, "Enhancement visibility controlled by rate-table pricing,
   not a flag ✅"** is stale. V089 shipped `enhancement.system_managed` plus `FlaggedEnhancementResolver`, and
   [ViewProposal.java:284-289](src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:284)
   widens `proposalEnhIds` from that flag precisely *because* a system-managed enhancement drives no pricing.
   Visibility is now flag-controlled for that class of enhancement. D4 carries no amendment marker.
2. **`CLAUDE.md` — "Latest migration in tree: **V073**"** vs. actual **V092** (`ls docs/migrations/`).
   CLAUDE.md self-flags this as expected drift, so it is a known-stale statement rather than a defect.
3. **No contradiction with `docs/swbd_ichra_build_plan.md:68-69`.** Its claim that `ApplyForProposal` and
   `CreateSetup25` are "fully per-LOS driven", so "the customer-facing sales-to-setup path carries a new LOS
   with no work", is **confirmed** by this run. The `GenerateProp*` id literals sit outside that path, and the
   doc's wording already scopes the claim to the customer-facing path.

---

## What I could not determine

| Open | What would settle it |
|---|---|
| Whether the Production `los` table actually holds ids 5–10 with the POP / FSA / HRA / HSA / Transit / COBRA meanings `GenerateProp25` assumes, or whether that mapping has drifted | `SELECT los_id, short_text, description FROM los` on Production. Not run — no DB access this run. All id claims above are read from source; the seeders write **1–4**, so the 5–10 mapping is **inferred** from the two `fillProposal` methods, not observed |
| Whether `NdtTestRun.censusData`'s JSON schema contains any participation / eligibility field that could stand in for a waiver | The writer is `NdtTestRunServlet`; the shape is declared in no entity. Reading that servlet's parse path would settle it — not pursued, since a JSON blob is not a candidate attachment point either way |
| Whether `enrollment` rows are ever written outside the import path | Grepping the write path for `new Enrollment(`. Not run; the FK topology (every FK to an `s*` staging table) is strong evidence for the structural claim, but "import-only" is **inferred** |

---

## SQL close-out audit

- **Every SQL statement produced, run, or recommended in this run: none.** No SQL was produced, no SQL was
  run, no SQL was recommended, and no database connection was made.
- **In a versioned migration:** not applicable — nothing was produced.
- **Orphaned `.sql` files noticed:**
  - `docs/migrations/seed_ndt125_questionnaire.sql` — sits in the versioned-migration directory with **no
    `V{NNN}__` prefix**, so it registers no `schema_version` row and is invisible to the tracker.
  - `docs/updates/update_V039_to_V057.sql` — a rollup outside `docs/migrations/`.
  - `release/V066__…` through `release/V071__…` — six duplicate copies of migrations that also exist under
    `docs/migrations/`.
  - `.claude/worktrees/**` holds full duplicate copies of `docs/migrations/` and `docs/importscript/`.
- **Current highest migration version in the repo:** **V092**
  (`docs/migrations/V092__hsa_enrollment_assistant.sql` — untracked in git as of this run's baseline).
- **Anything described but not scripted:** nothing. This run designed no schema change and names no field.
