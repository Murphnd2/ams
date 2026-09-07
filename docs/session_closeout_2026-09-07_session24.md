# Session 24 close-out

Date: 2026-09-07. Branch: `refactor/modernize-architecture`. Baseline at session start: `576fe39`
(`docs: session 23 close-out`). Docs-only session — no Java, no JSP, no SQL, no migration.

---

## 1. Shipped

Three sub-runs. **S24-C** and **S24-D** were documentation-only, working against real test evidence
supplied in the prompt (not derived from the repo — the repo had no prior record of any of this).
**S24-B**, run after this close-out's first commit, was a read-only repo investigation that wrote no
file of its own; its findings are in section 9 below and are the reason sections 6 and 7 were revised.

- **S24-C** — wrote `docs/business/summit_data_exchange.md` (new, 219 lines): the first integration
  spec for DataPath Summit's file-based Data Exchange, covering transport (FTP host/port, three
  folder configs, credential posture), template mechanics (Body/Unmapped/Header/Footer Format,
  the "Optional does not mean optional" trap), the proven four-file import chain (Employer
  Demographic → Employer CDH Plan → Demographics → HRA Enrollment) with working example lines, the
  ID-ownership table (which of the four identifiers AMS owns vs. Summit owns, and the
  `Participant TPA Custom ID` global-uniqueness trap that fails one step late as `Employer ID
  Conflict`), re-import/upsert behavior, results-file correlation guidance, and the ICHRA plan
  template configuration as tested. Also appended four new entries to
  `docs/analysis/legal_assumptions.md` — **LA-25** through **LA-28**.
- **S24-D** — renamed that document's local open-question series from `O-01`–`O-10` to
  `SDX-01`–`SDX-10`, to stop it colliding with the project's pre-existing global `O-NN` registry
  (`O1`–`O52+`, tracked in `plus_tier_build_plan.md` and related docs). Added one sentence at the
  series' introduction making the distinction explicit for future readers. No other content changed.

**Both sub-runs left the working tree dirty by explicit instruction** — this close-out is the first
commit of the session.

---

## 2. In flight

None. Both sub-runs completed cleanly with no hard stops.

---

## 3. Decisions made

1. **Where this document and vendor documentation disagree, this document is correct.** Stated
   prominently in the doc's front matter — every field requirement was established by importing a
   file and reading the results file, not by reading the element picker or vendor AI ("Atlas")
   answers, both of which were wrong or silent on several points (ICHRA-as-native-plan-type being
   the clearest example).
2. **AMS emits full current state on every run, no delta tracking.** Test-verified: re-importing an
   Employer Demographic file with the same `Employer TPA Custom ID` updates in place
   (`Employer edited successfully`) rather than duplicating. Recorded as **LA-26**.
3. **Participant IDs must be derived from a globally unique AMS key, never a per-employer
   sequence.** Test-verified the hard way — a duplicated per-employer ID is accepted at Demographics
   import and only fails one file later, at HRA Enrollment, as `Employer ID Conflict`, with no way
   for Summit to say which participant it meant. Recorded as **LA-25**.
4. **The document's local open-question series is `SDX-NN`, not `O-NN`.** The project already has a
   global `O-NN` open-question registry (unhyphenated, O1–O52+) tracked in
   `docs/swbd_ichra_build_plan.md` / `docs/ichra_strategy.md` / `plus_tier_build_plan.md`. Rather
   than guess at that registry's true current maximum or touch any of those out-of-scope files, the
   ten Summit-specific open questions got their own prefixed series (S24-D).

---

## 4. New assumptions

Four, all appended to `docs/analysis/legal_assumptions.md` between LA-24 and "Candidates considered
and not adopted":

- **LA-25** — Participant identifiers must be globally unique. **Confirmed by test**, 2026-09-07.
- **LA-26** — Summit upserts on `Employer TPA Custom ID`, so AMS emits full state. **Confirmed by
  test**, 2026-09-07.
- **LA-27** — `Funding tax treatment = Pre-tax` correctly represents employer ICHRA contributions.
  ⚠️ **Assumed — thin basis** (reasoning from option names only, not verified against Summit
  behaviour). Confirm before first live ICHRA funding.
- **LA-28** — `PCOR Reportable` should be enabled on the ICHRA plan template. ⚠️ **Assumed — thin
  basis** (general PCORI treatment of HRAs, not read against primary text). Confirm before first
  live ICHRA plan is created; pairs with **SDX-10**.

---

## 5. Open questions raised

Ten, all local to `docs/business/summit_data_exchange.md`, numbered `SDX-01`–`SDX-10`:

1. **SDX-01** — FTPS or SFTP on port 443.
2. **SDX-02** — Do two employers with identical plan year dates share one global plan year or get
   duplicates?
3. **SDX-03** — Valid `Record Process Indicator` values.
4. **SDX-04** — Termination handling — which fields, which file.
5. **SDX-05** — Mid-year election change mechanics.
6. **SDX-06** — Is `Import Plan ID` actually globally unique, or only per-employer as tested?
7. **SDX-07** — Is load order enforced across the four-file chain?
8. **SDX-08** — Does `Funding tax treatment = Pre-tax` affect payroll/W-2 reporting? (pairs with
   LA-27)
9. **SDX-09** — Does enabling COBRA Administration for Premium Billing generate COBRA
   artifacts/notices? **Not safely testable** — flagged to route to Summit support rather than test
   directly, since the failure mode is a notice reaching a real person.
10. **SDX-10** — Does `PCOR Reportable` drive PCORI reporting data capture? (pairs with LA-28)

Also carried forward, untouched by this session: everything session 23 left open (LA-19 "covered
by" vs. "offered"; where `D-NN` decisions are registered; T166/T170/T173/T174; the unreleased
commit queue since `v0.91.05`; the three unsent SWBD emails to Forrest).

---

## 6. Contradictions found

**Revised after S24-B.** As originally written this section read "None," which was true of S24-C and
S24-D — both wrote net-new documentation of a previously undocumented surface and checked it against
nothing. S24-B, a repo investigation, found four:

1. **`CLAUDE.md` is stale on the migration version.** It states "Latest migration in tree: **V073**".
   The actual highest is **V093**. Twenty versions of drift. `docs/analysis/migration_tracker.md` is
   **correct** (its "Current Highest Version" reads V093 and rows exist through V093) — the drift is
   in `CLAUDE.md` alone, which is self-aware enough to say "always re-check `ls docs/migrations/`",
   and that instruction is what caught it. **Not corrected this session** — flagged for Kevin.
2. **The standing "Proposal → Application → Setup is strictly 1:1:1" claim is half wrong.**
   Proposal ↔ Application is genuinely 1:1 and **database-enforced**: `Application.@Id` is the
   proposal FK (a derived identifier) and the DDL declares `PRIMARY KEY (proposal_id)`. But
   Setup ↔ Application is 1:1 **in the JPA mapping only**. `Setup extends Activity extends Assignee`,
   which is `SINGLE_TABLE`, so there is no `setup` table and the join column is `assignee.proposal_id`
   — declared in the baseline DDL as a plain `KEY FK_ASSIGNEE_proposal_id`, **not a UNIQUE key**.
   Nothing prevents two Setup rows on one Application. Contrast `proposal_ichra_intake`, which does
   declare `UNIQUE KEY uq_pii_proposal (proposal_id)` — the codebase knows how to enforce this and
   did not here.
3. **`Benefit` cannot be the source of a benefit-create export**, contrary to the natural assumption
   (and to S24-B's own prompt, which stated "the benefits created at setup … are the natural source").
   No `Benefit` is created at setup: all nine `new Benefit(` call sites are import services, seeders,
   or updaters — none in a setup, application, or proposal controller. The entity lives in
   `model/summit/**archive**/` and its second field is `@Column(name="summit_id", nullable = false)`,
   so a row cannot exist before Summit has assigned an ID. It is downstream of the export, not its
   source. Using it would be circular.
4. **`docs/business/summit_data_exchange.md`'s suggested participant key does not exist as described.**
   It recommends deriving the participant ID from "the AMS employee record's own primary key";
   `Employee.@Id` is an **assigned** `int` holding Summit's employee ID, not `@GeneratedValue`. Filed
   as **T177**. The doc's requirement is right; only its suggested source is unavailable.


## 7. Next

**Recommended:** resolve **SDX-01** (FTPS vs SFTP) before any automation work on this integration —
it gates client library selection and is the cheapest of the ten to close. **LA-27 and LA-28** are
the two thin-basis assumptions in this session's output and should be confirmed (ideally with
Summit support or counsel, per the doc's own guidance) before the first live ICHRA plan/funding
event, not discovered after.

**Also outstanding, carried forward unchanged from session 23:** LA-19 (open, load-bearing); where
`D-NN` decisions live; T166 (HealthSherpa plan fetch); T170; T173's two flagged discrepancies;
T174; the unreleased commit queue (every commit since `v0.91.05`, now including V092/V093, still
unshipped to production); the three unsent SWBD emails to Forrest.

**Added by S24-B.** The emitter cannot be specified until **T177** (no AMS-owned globally unique
participant identifier) and **T178** (no per-participant annual election amount) are settled — both
are net-new capture, not mapping work, and both were filed to `project_backlog.md` this session. Two
further items need no decision, only recording: `Prospect.id` is the leading candidate for
`Employer TPA Custom ID`, and the employer address block must be sourced from `Prospect.address`
because the `employer` table has none. **`CLAUDE.md`'s "Latest migration in tree: V073" is wrong
(actual: V093) and is not corrected here** — it is Kevin's file to change.

---

## 8. SQL close-out audit

**No SQL was produced, run, or recommended this session.** Both sub-runs were explicitly
documentation-only with a scope fence forbidding any `.sql`/`.java`/`.jsp`/`.xml`/`.properties`
file. Neither touched a database. Highest migration version remains **V093** (unchanged from
session 23) — this session did not add, modify, or reference a migration file.

**S24-B addendum.** S24-B likewise produced, ran, and recommended **no SQL**. It read migration and
baseline `.sql` files as source material only, authored none, and deliberately did not suggest a
query for Kevin to run even for the one question a query would settle (whether ICHRA/Ins125/FSA/HSA
rows exist in `plantype`), per its own prompt's instruction. It wrote zero files and ran no git
mutation; the backlog rows and this section were written afterward, on Kevin's "close session".

---

## 9. Sub-run S24-B — Summit export surface and the Setup/Benefit model (read-only)

Baseline `66eb81e`, tree clean at start. Four questions, all answered from source.

### Q1 — Does an export-to-Summit surface already exist? **No.** Confidence: high.

Nothing in AMS writes a structured data file intended for Summit or any external system. Searches
run: `sftp|SFTP|FTPS|ftp1|dpath.com|jsch|commons-net` (**zero hits in `src/main/java`,
`src/main/webapp`, `pom.xml`, or any config** — docs, one demo CSV, one mockup, and two chatbot
knowledge JSONs describing an unrelated *Paycom* pipeline); all file-writer constructors (**four live
files**: `SummitImportWizard`, `Importer`, `BillingHelper`, `EmfListener` — all inbound staging or
diagnostics); `Content-Disposition|text/csv`; all scheduler idioms (**two schedulers**,
`InstallationHealthScheduler` and `RateCacheWarmService`, **neither produces a file**; no cron, no
Quartz); and every `AppConfig.get` key in the codebase (`ANTHROPIC_API_KEY`, `BRANDING_PATH`,
`DEPLOYMENT_KEY`, `LOG_PATH`, `PSP_HOSTS`, `SAVE_PATH`, `SYSTEM_URL`, `VERIFIED_PSP_DOMAINS`,
`VIDEO_PATH` — **none names an outbound host, folder, or drop path**; `SAVE_PATH` is inbound staging,
three call sites).

Nearest existing thing: `ExportApplicationCsv` (`@WebServlet("/ExportApplicationCsv")`) — a browser
download of application intake answers, streamed to `response.getWriter()`, **never touching the
filesystem**, with headers hardcoded in code and a variable tail driven off PSP-scoped
`ApplicationField` rows. Not a Summit file; shares no columns with one.

**The Summit emitter is a new mechanism, not an extension.** There is no risk of building a parallel
export beside a working one. (Note: `project_backlog.md` Post-Conference Tier item 20, "Summit Data
Converter Web Tool," describes such a tool — it is LOW-priority backlog, unbuilt.)

The run also captured the **inbound** layouts, which were not previously written down anywhere — the
verbatim header rows of `demo/summit-import/J1,J2,J3,J4,J5,J7`. Two corroborate the spec directly:
**J2 carries `ParticipantCustomID`** and **J4 carries `ImportPlanID`**. AMS already ingests both
AMS-owned identifiers the export must write.

### Q2 — Setup representation and triggers

`Setup extends Activity extends Assignee` (`SINGLE_TABLE`) — **no `setup` table**; rows live in
`assignee` under `DTYPE`. Own fields: `checkList`, `application` (1:1 via
`@JoinColumn(name="proposal_id")`), `primaryContactSetup`, `contactList`, `myRsc`. The 1:1:1 claim is
addressed in section 6 item 2.

**Completion** is the inherited `Activity` triple — `is_complete`, `date_completed`,
`completed_by_id`. No Setup-specific status enum.

**Hooks: creation yes, completion no.** `SetupPromotionService.promoteAfterSetupCreation(...)` is an
explicit post-create hook ("run whenever a Setup activity is born from an Application"), called from
`ReviewApplication` and `CreateSetup25` — **the working precedent for the shape a completion hook
should take**. Closing goes through the generic `CloseActivity25`, whose `updateInMemoryStructures()`
branches only on `CheckList` vs. everything else: **no Setup branch, no extension point.** The nearest
dispatch surface, `AmsDataLocal.respondToActivityUpdate(em, "CLOSE_ACTIVITY", activity)`, is a
cache-maintenance switch, not a domain-event bus.

**Admin UI:** Setups are ordinary activities — `ViewById` -> `ViewActivity25`, diverting to
`agentSetupDetail25.jsp` for agents. No dedicated PSP Setup admin page.

### Q3 — Benefit model vs. the export target

`Benefit`'s full field list was read and recorded (18 persisted fields). Its disqualification as an
export source is section 6 item 3. Structurally it *could* hold five distinct plan rows on one
employer (five rows sharing `employer_id`, distinct `plan_type_id`); whether the ICHRA-family
`plantype` rows actually exist on any installation **cannot be determined from the repo** — that
table is import-populated and no tracked seeder enumerates it.

Against the concrete target, per field:

- **Employer name** — `Prospect.name`. **Employer address/city/state/zip** — `Prospect.address`
  -> `Address`. **Not from `Employer`**: the `employer` table has **no address columns at all**
  (verified against baseline DDL), and neither does `ImportEmployer`. Given the spec's warning that
  the address block is a hard requirement despite being labelled Optional, **address must come from
  the Prospect side.**
- **Participant first/last name, address block** — available from `Employee` or `Person`.
- **Participant effective date** — not on `Employee`; only on inbound staging entities.
- **Plan type discriminator, name, description, plan year begin/end** — partially derivable
  (`LOS`/`Enhancement` selections, `proposal_ichra_intake.plan_year` as a SMALLINT), but no date pair
  is stored on the proposal side and no LOS-to-PlanType mapping exists.
- **Participant annual election** — **does not exist.** Filed as **T178**.

**Type resolution pattern to follow** (project rule 4, no hardcoded reference IDs): `plantype` has
**no `psp_id`** — it is global-per-installation. The existing non-hardcoded precedent is
`import_plan_type_mapping` (V048): `source_plan_code` -> `target_plan_type_id`, with `provider_id NULL`
meaning system default. A later build should follow that, not invent one.

**The two identifier questions:**

1. **Globally unique participant ID** — see **T177**. Partially solved for already-imported employees
   (`Employee.custom_id` already round-trips it); unsolved for new ones.
2. **Immutable employer ID** — `Employer.organization_id` and `er_key` are both Summit-owned and so
   circular for the create case; `taxId` is explicitly disqualified by the spec as mutable.
   **`Prospect.id`** (`@Id @GeneratedValue`, `prospect_id BIGINT`) is **the only identifier found that
   is AMS-owned, auto-generated, immutable, and exists before Summit does** — per-installation unique,
   which is exactly the spec's stated scope for `Employer TPA Custom ID`. Recorded as the leading
   candidate; not a decision.

### Q4 — Where a field mapping could live as data

Four surfaces, reported without design:

| Surface | Read at runtime | Scope | Survives a production DB restore? |
|---|---|---|---|
| `constant` table | Per-call JPQL, no caching. `name varchar(50)` PK, `value varchar(1000)`, `note`, `text_value TEXT` | **Global — no `psp_id` column** | No — in-schema, a production restore overwrites it |
| `ssa.properties` | Loaded once at startup into a static `Properties`, read-only after | Per-installation, global | Yes — on disk, outside the schema; needs a restart to change |
| DB-first-with-properties-fallback | `AppConfig.setAnthropicApiKey`/`setSystemType` pattern: `constant` row cached into a `volatile` static at boot, falling back to `ssa.properties` | Global | Partial |
| Agency-scoped columns | Ordinary entity fields on `agency` | Per-agency | No |

**Agency configuration is not a generic key/value surface** — every setting is a purpose-built
column added by its own migration (`markup_enabled`, `landing_host`, `email_domain`, `quote_token`,
`ichra_enabled`, and so on). There is **no settings/config/preference table anywhere in the schema**
(grep-verified across `docs/migrations/` and `docs/importscript/`).

**The one existing surface actually shaped like a field mapping** is the Universal Import config family
(V048) — `import_provider` (**`psp_id NOT NULL`**), `import_file_type` (carries `file_format`,
`sort_order`), `import_field_mapping` (`source_column` -> `canonical_field`, `is_required`, `is_key`,
`transform_rule`), `import_plan_type_mapping` (nullable `provider_id` = system default), and
`import_run_log` (per-entity counters). Genuinely PSP-scoped, ordered, format-aware, with a
system-default tier and a run log. Inbound-only today. In-schema, so it does not survive a production
restore. **Reported as an available surface only — no design proposed.**

### What S24-B could not determine

Whether ICHRA/Ins125/FSA/HSA/LFSA rows exist in `plantype` on any installation, and with what `Code`
values — a live-data read, deliberately not proposed as SQL. Whether any live `assignee` row set
actually contains two Setups on one `proposal_id` — the schema permits it, which is the finding.
Whether plan-year begin/end dates are recoverable by convention from `plan_year`'s SMALLINT. Where a
per-participant election would come from (T178 — net-new capture, not a mapping problem).

---

## 10. Second half — S24-E (export stage 1) and S24-F (setup sequence design)

Two more sub-runs after section 9's S24-B investigation closed. **S24-E** built the first working
piece of the emitter. **S24-F** (this section) designed the full client setup sequence as
documentation and closed the session. Baseline for both: `2330e1a`.

### 10.1 Shipped

- `66eb81e` — docs: session 24 close-out — Summit Data Exchange integration spec (S24-C's
  `docs/business/summit_data_exchange.md` + LA-25–28; S24-D's `O-NN` → `SDX-NN` renumbering).
- `2330e1a` — docs: session 24 close-out expansion and backlog updates (S24-B's findings folded into
  sections 6–9 above, plus the T177/T178 backlog rows).

### 10.2 In flight

**S24-E, uncommitted** (working tree dirty by explicit instruction throughout):

- `src/main/java/net/superiorstate/ams/controller/market/SummitExportServlet.java` (new) —
  PSP-admin-only, URL-only browser-download servlet generating the Employer Demographic and Employer
  CDH Plan files. Stage 1 of the emitter: employer and plan only, no participants or enrollment.
- `docs/analysis/legal_assumptions.md` — **LA-29** (`Prospect.id` as the Summit employer key) and
  **LA-30** (calendar-year plan years) appended.

**Code-verified only — never run, never deployed, output never validated against Summit.** Blocked on:
`SUMMIT_ICHRA_PLAN_TEMPLATE_ID` being set in `ssa.properties` on this installation (no tracked config
file exists to hold it — see open question 4 below), and a proposal existing with both an ICHRA intake
row carrying a `plan_year` and a `Prospect` with a complete address (`address1`/`city`/`state`/`zip`
all non-blank).

### 10.3 Decisions made

1. The four-file chain — Employer Demographic → Employer CDH Plan → Demographics → HRA Enrollment —
   proven by live test against Summit.
2. `Prospect.id` is the employer identity Summit keys on (**LA-29**).
3. ICHRA is premium-reimbursement-only — settled earlier in the session.
4. The emitter emits full current state on every run, never deltas (**LA-26**), so there is no
   sent-state store or create-vs-update branch anywhere in the design.
5. Stage 1 of the emitter is scoped to employer and plan only — participants and enrollment need an
   employee roster AMS does not have, and are explicitly deferred.
6. The client setup sequence splits into a deterministic core (files 1–4, fires as one batch at
   implementation) and a partner-dependent tail (files 5–8, event-driven on third-party data arrival,
   some of which may never arrive and get entered by hand instead).
7. Renewal is explicitly out of scope for the setup sequence just designed — new-client setup only.

### 10.4 New assumptions

All six from this session, LA-25 through LA-30, one line each with reversal cost:

- **LA-25** — Participant identifiers must be globally unique. Reversal cost: cheap before any
  participant is created in Summit, rising sharply after (re-keying live records).
- **LA-26** — Summit upserts on `Employer TPA Custom ID`; AMS emits full state, not deltas. Reversal
  cost: moderate — would require adding a sent-state store.
- **LA-27** — `Funding tax treatment = Pre-tax` correctly represents employer ICHRA contributions.
  Reversal cost: cheap as a template setting, rising once contributions are actually processed.
- **LA-28** — `PCOR Reportable` should be enabled on the ICHRA plan template. Reversal cost: cheap now,
  expensive to reconstruct retroactively once a filing is due.
- **LA-29** — `Prospect.id` is the employer identity Summit keys on. Reversal cost: cheap before the
  first file is imported, rising sharply after — it is an upsert key.
- **LA-30** — ICHRA plan years are calendar years. Reversal cost: moderate — requires capturing
  explicit begin/end dates on the intake, a schema change.

### 10.5 Open questions raised

`SDX-01`–`SDX-10` carried forward by reference from `docs/business/summit_data_exchange.md` (section 5
above lists all ten). Four more raised this half:

1. ⚠️ **The participant identity gap, which blocks setup files 4 onward.** AMS has no AMS-generated
   employee key: `Employee.@Id` is an *assigned* int holding Summit's ID, and `Employee.custom_id`
   only has a value for employees already imported from Summit. AMS also has no pre-Summit employee
   roster carrying names, addresses, and effective dates. This is both a schema decision and a new
   collection point for data about real people — the one category the project's build rules say to
   settle before building. Settled by: a design decision in the next session.
2. What is the PB file type and field set for the ICHRA notice plan? Settled by: a test import, the
   same way the CDH chain was proven.
3. Should the V048 import configuration family be extended to drive outbound mapping? Settled by: a
   design decision.
4. Where should installation config keys be documented, given no `ssa.properties` or sample is tracked
   in the repo at all? `HEALTHSHERPA_API_KEY`, `ICHRA_DEMO_ALLOW_STAGING_PROPOSAL`, and
   `SUMMIT_ICHRA_PLAN_TEMPLATE_ID` exist only in Javadoc and scattered analysis docs. Recommended: a
   key list in `docs/deployment_runbook.md`, names and purposes only, never values. Settled by: a
   small separate run.

### 10.6 Contradictions found

Five, recorded plainly because four were believed and stated as fact before being corrected:

1. `Benefit` is **not** created at setup and cannot source the export — it is an inbound mirror in
   `model/summit/**archive**/` with `summit_id NOT NULL`. The build plan's "benefits created during
   setup flow into renewals" needs correcting. (Section 6 item 3 above.)
2. Setup ↔ Application is **not** DB-enforced 1:1. Proposal ↔ Application is (`PRIMARY KEY
   (proposal_id)`), but `assignee.proposal_id` carries a plain index, not a unique key — while
   `proposal_ichra_intake` declares one, so the pattern exists in the codebase and was not used here.
   (Section 6 item 2 above.)
3. Vendor AI ("Atlas") stated ICHRA had no native Summit plan type and "likely uses HRA." **Wrong** —
   ICHRA is a native plan type, as are EBHRA, LFSA, HSA, Ins125, and Ins125_w_HSA.
4. `Participant TPA Custom ID` must be **globally unique**, not per-employer. The Demographics import
   accepts a duplicate and reports success; the failure surfaces one file later as
   `Employer ID Conflict`. This was concluded wrongly mid-session from the successful create, and
   corrected only by the later enrollment failure. (**LA-25**.)
5. **`CLAUDE.md` says the latest migration is V073 and `MEMORY.md` said V089. Both are stale — the
   tree is at V093.** `CLAUDE.md`'s drift was already caught in section 6 item 1 above; `MEMORY.md`'s
   own V089 drift was caught and self-corrected during this session's memory read. Not fixed in either
   tracked file by this run — worth a separate one.

### 10.7 Next

The participant identity decision (open question 1 above), because it blocks setup files 4 through 8.
Then a runtime walk of `SummitExportServlet`, which has never executed.
