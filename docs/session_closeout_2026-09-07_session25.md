# Session 25 close-out

Date: 2026-09-07. Branch: `refactor/modernize-architecture`. Baseline at session start: `2da5d66`
(`docs: client setup sequence and session 24 close-out`). Corrects the Summit export's data sources
and adds an installation prefix to the employer key. Ends at `fb8673d`.

---

## 1. Shipped

Three commits, one coherent line of work — the Summit export built in session 24 had two wrong
sources, and this session re-sourced both and hardened the employer key against a second
installation ever existing.

- **`cd5c7e0`** — Summit export stage 1: `SummitExportServlet`, generating the Employer Demographic
  and Employer CDH Plan files. `Employer TPA Custom ID` sourced from `Prospect.id`; employer address
  from `Prospect.address`; plan year from `proposal_ichra_intake.plan_year`. Registered **LA-29**
  (`Prospect.id` as the employer identity) and **LA-30** (calendar-year plan years). Session 24
  output, carried as this session's starting point.
- **`2da5d66`** — client setup sequence added to the Summit spec, session 24 close-out. Also carried
  in, not this session's work.
- **`fb8673d`** — re-source from application answers, installation prefix on the employer key.
  Registers **LA-31** and **LA-32**; narrows **LA-29** and **LA-30** status lines only (their
  assumption/basis/design-choice/risk/reversal-cost text is unchanged — the register's own
  convention is that reasoning stays visible and only status moves).

Four sub-runs produced `fb8673d`: **S25-A** (read-only investigation — where application answers
live, and how to key one without hardcoding), **S25-B** (re-sourced address and plan year to
`applicationfieldvalue`), **S25-C** (added the configured `SUMMIT_TPA_ID_PREFIX`), **S25-D**
(reviewed and committed both). All four left the working tree dirty by explicit instruction until
S25-D's commit.

---

## 2. In flight

Nothing uncommitted — `fb8673d` is pushed and the tree is clean.

But nothing has ever run. `SummitExportServlet` is **code-verified only** across all three commits —
never executed, its output never validated against Summit's importer. The four-file chain proven
live against Summit in session 24 tested files 1 and 2 with data hand-entered for the test; this
session's re-source has not been run at all, against test or real data.

**Blocked on operational prerequisites, none of them code:**

- `SUMMIT_TPA_ID_PREFIX=SSA` and `SUMMIT_ICHRA_PLAN_TEMPLATE_ID=1029` in `ssa.properties`, then a
  Tomcat restart — `AppConfig` loads once at startup, so a properties edit alone does not take
  effect.
- The `plan_year_eligibility` section (from the `s125_fsa` package) attached to the LOS being sold,
  or `plan_year_start` / `plan_year_end` are absent and the CDH Plan export correctly refuses.
- A proposal with a submitted application carrying those answers — `Application` rows are created
  lazily, only once someone opens or saves the application form, so a proposal that has never been
  applied against has nothing for the export to read.

---

## 3. Decisions made

1. **Employer address and plan year source from application answers, not `Prospect` or
   `proposal_ichra_intake`.** `Prospect.address` is unset at five of seven `new Prospect()` creation
   sites (`CreateOpportunity`, `CreateProspect`, `CreateSetup25`, `RequestQuote`, the demo seeder is
   the sixth, address-set) and agency-derived at the other two (`GenerateProp`, `GenerateProp25`,
   both assigning the contact `Person`'s address, itself set from the selling `Agency`). It never
   held the employer's own address. The applicant asserts the real address and plan year during
   application; those answers persist in `applicationfieldvalue`.
2. **Plan year reads `plan_year_start` / `plan_year_end` as real dates**, not a bare year integer, so
   a non-calendar plan year is now expressible for this file. LA-30's calendar-year assumption is no
   longer load-bearing for the CDH Plan export specifically.
3. **`Employer TPA Custom ID` = `{SUMMIT_TPA_ID_PREFIX}-{prospectId}`**, prefix `SSA` on this
   installation, read from config at request time and never hardcoded. Uniqueness rises from
   per-installation to cross-installation — the platform's stated multi-installation ambition means
   two installations could someday feed the same Summit TPA account, and a bare `Prospect.id` would
   let both claim the same number for different employers.
4. ⚠️ **The employer key stays opaque — no encoded meaning.** Agency, agent, rate plan, and initial
   LOS were all considered as key components and rejected: the key is Summit's upsert key, every one
   of those attributes is mutable, and a change orphans the Summit record and creates a duplicate.
   Record the pattern this establishes — **opaque immutable key, rich mutable attributes.**
   Attribution belongs in `Employer Name`, the five User Defined Field sets, or `Division Custom ID`
   — never in the key.
5. **Every missing input refuses with a named field key rather than emitting a partial file.** No
   silent fallbacks anywhere in the export — not `Prospect`→`Agency` for address, not a bare id for
   the employer key, not `proposal_ichra_intake` for plan year.
6. **`DEPLOYMENT_KEY` is a credential, not an installation identifier**, so a dedicated
   `SUMMIT_TPA_ID_PREFIX` was added instead of reusing it. All three of its call sites
   (`SystemRegisterApi`, `InitializeDataBase`, `ReSeedDb`) gate destructive or security-sensitive
   operations by comparing a submitted value against it and rejecting on mismatch — never displaying
   it. A credential must never appear in an exported file; the cost of a dedicated key is one config
   line.

---

## 4. New assumptions

**LA-31** and **LA-32**, appended to `docs/analysis/legal_assumptions.md` after LA-30. LA-29 and
LA-30 had **status lines narrowed only** — their assumption, basis, design choice, risk, and reversal
cost are unchanged.

- **LA-31** — Employer identity details for the Summit export come from application answers, not
  `Prospect` or `proposal_ichra_intake`. Confirmed by code inspection, 2026-09-07. Reversal cost:
  cheap — a source repoint confined to `SummitExportServlet`, no schema.
- **LA-32** — The Summit TPA prefix is configuration, not a code literal. Assumed, 2026-09-07.
  Reversal cost: cheap before the first real import, effectively irreversible after — changing an
  upsert key orphans every record keyed on the old value.
- **LA-29**, narrowed — the employer key is now emitted as a configured prefix plus `Prospect.id`,
  raising uniqueness from per-installation to cross-installation.
- **LA-30**, narrowed — no longer load-bearing for the Employer CDH Plan export specifically, since
  `plan_year_start`/`plan_year_end` supply real dates; still applies anywhere a bare year integer is
  the only available source.

---

## 5. Open questions raised

1. ⚠️ **Participant identity — still the blocker for setup files 4 through 8.** AMS has no
   AMS-generated employee key (`Employee.@Id` is an assigned int holding Summit's ID) and no
   pre-Summit employee roster carrying names, addresses, and effective dates. This is both a schema
   decision and a new collection point for data about real people — the one category the project's
   build rules say to settle before building. Carried forward from session 24 (T177/T178), unchanged
   this session.
2. **GA attribution via User Defined Fields.** `Agency` carries a self-referential
   `Parent (General Agency)` field, set at establishment, already driving a hierarchy that controls
   rates and branding. SWBD is currently top-level with no parent. Emitting GA attribution is
   therefore a parent-chain walk, not new schema — but whether SWBD should have a parent is a business
   decision, not a build one.
3. **Should `plan_year_eligibility` be attached to the ICHRA LOS, or should ICHRA-specific plan year
   fields be defined instead?** It currently ships in the `s125_fsa` package, which is why the export
   refuses on any installation that has not attached that section to the LOS being sold.
4. **Where should installation config keys be documented?** No `ssa.properties` or sample is tracked
   in the repo, so `HEALTHSHERPA_API_KEY`, `ICHRA_DEMO_ALLOW_STAGING_PROPOSAL`,
   `SUMMIT_ICHRA_PLAN_TEMPLATE_ID`, and now `SUMMIT_TPA_ID_PREFIX` exist only in Javadoc and
   scattered docs. Recommended: a key list in `docs/deployment_runbook.md`, names and purposes only,
   never values. Settled by: a small separate run.
5. **The PB-side ICHRA notice plan file type and field set remain completely unproven** — everything
   tested in session 24 was CDH.

`SDX-01`–`SDX-10` carried forward by reference from `docs/business/summit_data_exchange.md`,
untouched this session.

---

## 6. Contradictions found

1. **`Prospect.address` never holds the employer's address** — unset at five of seven
   `new Prospect()` sites, and `GenerateProp`/`GenerateProp25` assign the contact's address, where
   the contact was created from the `Agency`. The servlet as committed in `cd5c7e0` would have
   hard-failed for most proposals (the `firstBlank` pre-flight check) and silently emitted the
   selling agency's address for the rest. Session 24's close-out (section 9, Q3) had already
   recorded "`Prospect.id` is the leading candidate for `Employer TPA Custom ID`, and the employer
   address block must be sourced from `Prospect.address`" as a finding with no decision — this
   session shows that finding's second half needed correcting once the actual creation-site behavior
   was traced, not just the schema shape.
2. **`ApplicationField` has no numeric ID** — `field_key` is the varchar primary key, the admin UI
   declares it immutable (`serviceManager25.jsp`: *"internal, unique, cannot change later"*), and
   `label` is the editable field. Reading by literal `fieldKey` is the established house pattern
   (`ApplyForProposal:243-247`, `ReviewApplication`, `bill_benefit_plans` throughout the application
   surfaces). The concern — going in — that PSP-scoped IDs would differ per installation and break a
   literal-key read was inverted: there is no PSP-scoped numeric ID to differ from.
3. **`docs/business/summit_data_exchange.md` already said the plan year comes from the application**
   ("AMS already knows the plan year from the application") while the code committed in `cd5c7e0`
   read `proposal_ichra_intake`. Spec and implementation disagreed at merge time; the spec was right.
4. **A proposal is not guaranteed to have an application.** `Application.@Id` is the proposal FK, so
   an Application implies a Proposal, but not the reverse — `Application` rows are created lazily,
   only when someone opens or saves the application form. The servlet now refuses by name
   ("Proposal N has no submitted application…") rather than emit blanks.
5. **Still unfixed from session 24: `CLAUDE.md` states the latest migration is V073 and `MEMORY.md`
   stated V089. The tree is at V093.** Not touched this session — worth a separate small run.
6. **Still outstanding: S24-A**, filing eleven legal assumptions from earlier sessions, was tabled
   mid-session 24 and remains unfiled.

---

## 7. Next

**Participant identity** (open question 1), because it blocks setup files 4 through 8 and needs a
full session — a schema decision plus a new collection point for data about real people, the
category the project's own build rules say to settle before building.

**Cheaper but gated:** a runtime walk of `SummitExportServlet` against a real proposal — gated on
Kevin's three operational prerequisites in section 2, none of which are code.

---

## 8. SQL close-out audit

**No SQL was produced, run, or recommended this session**, across S25-A through S25-D or this
close-out. No migration was created — every field the export now reads (`address_street1`,
`address_city`, `address_state`, `address_zip`, `plan_year_start`, `plan_year_end`) already existed
in the tree's application-field definition sources before this session began. Highest migration
version in the tree: **V093**, unchanged from session 24.

---

## Close-out of this run (S25-E)

**Baseline hash:** `2da5d66` at session start (per this run's own baseline check); `fb8673d` at HEAD
when this run began, confirmed by `git log --oneline -4`.

**Close-out file path created:** `docs/session_closeout_2026-09-07_session25.md` (this file).

**Anchor disclosure.** This run created one new file and modified nothing existing — no anchoring
was required or performed. `git status --porcelain` was empty before this file was written.

**Verification class.** Code-verified only, carried in from S25-A through S25-D's own close-outs and
this session's commit history — nothing in this run itself exercised code. The document was written
and read back for structure against `docs/session_closeout_2026-09-07_session24.md`; nothing was run.

**SQL close-out audit.** No SQL was produced, run, or recommended by this run, and no migration was
created. Highest migration version observed: **V093**.

**Scope fence compliance.** No git mutation command was run — only `rev-parse`, `log`, `status`, and
directory listing. No existing file was modified — `legal_assumptions.md`, `summit_data_exchange.md`,
the session 24 close-out, `CLAUDE.md`, and `MEMORY.md` are all untouched. No `.sql` file was created
or modified. **Working tree left dirty** by this run's own new, uncommitted file.
