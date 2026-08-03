# Session 11-A close-out — T80 (half 1): employer contribution capture and monthly cost subtotal

**Run:** S11-A (Opus, per the run prompt's own instruction — this run touches `ProposalBuilder.java`,
the servlet on the proposal-creation path for every line of service in a live system, and folds Phase A
anchor discovery into itself as Step 2).

**Branch:** `refactor/modernize-architecture` (trunk).

---

## Baseline

- **Hash at preflight (post-pull):** `27b0d03e3761d7790022e1b1b6138f5fb402e383`
- **Hash at end (feature commit, pushed):** `a8471ba24ff8e0ab13df43b9e085bd6fd0fc2d66`
- **Hash at end (this close-out commit):** recorded below, read from `git log` after push, per the run
  prompt's explicit instruction never to carry a hash written before the push.

Preflight was clean on all four checks: branch was `refactor/modernize-architecture`, `git status --short`
was empty, and `git pull --ff-only` reported "Already up to date." — no fast-forward was needed.

## Session 10 close-out presence

Verified, not assumed, per Step 0a. `git log --oneline -- docs/session_closeout_2026-08-03_session10.md`
returns exactly one commit (`27b0d03 docs: session 10 close-out`), and the file exists on disk
(15,837 bytes). **Present and committed — no warning needed.**

## Anchor discovery (Step 2)

1. **V087's actual DDL** (`docs/migrations/V087__proposal_ichra_intake.sql`): table
   `proposal_ichra_intake` — `intake_id BIGINT NOT NULL AUTO_INCREMENT PK`, `proposal_id BIGINT NOT NULL`
   (UNIQUE, `FK ... REFERENCES proposal(proposal_id) ON DELETE CASCADE`), `zip CHAR(5) NOT NULL`,
   `county_fips CHAR(5) NOT NULL`, `county_name VARCHAR(100) NOT NULL`, `state CHAR(2) NOT NULL`,
   `headcount SMALLINT NOT NULL`, `plan_year SMALLINT NOT NULL`, `collected_at DATETIME NOT NULL`,
   `created_by BIGINT NULL` (FK → `assignee(id)`). Self-registration: `CREATE OR REPLACE VIEW schema_info
   AS SELECT 'V087' AS version, '2026-08-03' AS updated;` then `INSERT IGNORE INTO schema_version
   (version, description, script_name, applied_on) VALUES ('V087', ..., NOW());`. **V088 copies this
   shape exactly** — same `schema_info`/`schema_version` form, `ALTER TABLE` instead of `CREATE TABLE`.
2. **T125 intake panel location.** Servlet: `src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java`
   — **not** `controller/market/` as the run prompt guessed; the prompt anticipated this and asked me to
   locate it rather than assume. JSP: `src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp:194-235`,
   the `ichraIntakePanel` card with `intakeZip`/`intakeCountyFips`/`intakeCountyName`/`intakeState`/
   `intakeHeadcount` inputs, gated by `${ichraAvailable and not empty ichraPlanYear}` and shown/hidden by
   client JS (`updateIntakePanel()`) keyed on `data-plus-tier` LOS selection.
3. **Intake row persistence.** `ProposalBuilder.attachIchraIntakeIfPresent` (`:449-498`), called from
   `createProposal` in a best-effort try/catch (`:428-432`) after LOS attachment. Writes via JPA:
   `ProposalIchraIntakeDAO.save(em, intake)` (`src/main/java/net/superiorstate/ams/data/dao/ProposalIchraIntakeDAO.java`),
   which does `em.persist(intake)` inside an explicit transaction — not raw SQL, so no T64 flag.
4. **`buildTokenMap`.** Exactly one file: `src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:506`.
   The four existing ICHRA intake tokens (`ICHRA_COUNTY`, `ICHRA_COUNTY_FIPS`, `ICHRA_HEADCOUNT`,
   `ICHRA_PLAN_YEAR`) are populated at `:571-574`, always — one ternary per token, `ichraIntake != null
   && field != null ? field.toString() : ""`, with a comment explaining this is deliberate: an unresolved
   key renders as the literal `{{ICHRA_COUNTY}}` on a customer-facing page otherwise. **Copied this
   mechanism exactly** for the four new contribution tokens, as an all-or-nothing group rather than four
   independent ternaries, per the run prompt's explicit requirement.
5. **Currency formatting.** `ViewProposal.formatPremium` (`:703-707`): `NumberFormat.getCurrencyInstance(Locale.US).format(...)`.
   Only one currency formatter exists in the file. Added `formatCurrency(BigDecimal)` right beside it,
   calling the identical `NumberFormat.getCurrencyInstance(Locale.US).format(amount)` — not a new
   formatter, the same one factored for reuse across four call sites instead of inlined four times.
6. **Headcount type/nullability at token-build time.** `ProposalIchraIntake.headcount` is `Integer`,
   `nullable = false` at the JPA/DB level, but `ichraIntake` itself (the whole row) can be `null` when no
   intake exists — that's the only way `getHeadcount()` reads null in `buildTokenMap`, and the existing
   `ICHRA_HEADCOUNT` token already null-guards it. Written rows are constrained to 1–10000 by
   `ProposalBuilder.attachIchraIntakeIfPresent:479` (`headcount < 1 || headcount > 10000` → no row at
   all), so a real row's headcount is never actually 0 or null — the half-1 tokens' null/zero headcount
   guard is defensive per the spec, not something a real row can currently trigger.

Nothing in Step 2 could not be determined — all six items resolved directly from source.

## Shipped

- `a8471ba24ff8e0ab13df43b9e085bd6fd0fc2d66` — `feat: employer contribution capture and cost subtotal tokens (T80, V088)`.
  8 files changed: `V088__proposal_ichra_intake_contribution.sql` (new), `ProposalIchraIntake.java`,
  `ProposalBuilder.java`, `ViewProposal.java`, `proposalBuilder.jsp`, `migration_tracker.md`,
  `schema_version_migration.sql`, `project_backlog.md`. Pushed to `origin/refactor/modernize-architecture`.

## In flight

Nothing uncommitted. `git status --short` is clean as of this writing (pre-close-out-commit).

## Decisions made

- **Touched `ProposalIchraIntake.java` (the JPA entity) even though it was not named in the scope fence.**
  The fence listed the migration, the servlet, `ViewProposal.java`, and three doc files, and separately
  told me to add the JSP to the list with justification if it turned out to be a separate file — but said
  nothing about the entity. The entity is structurally unavoidable: the migration adds a DB column, and
  neither `ProposalBuilder.java` (write) nor `ViewProposal.java` (read) can reach it without the entity
  carrying the field. Treated this the same way the prompt pre-authorized the JSP case: located it,
  touched only the one field (import, `@Column`, getter/setter), and am flagging it here explicitly
  rather than silently expanding scope.
- **JSP touched, as pre-authorized.** `proposalBuilder.jsp` is a separate file from the servlet and
  carries the T125 intake panel's markup and the panel's clear-on-hide JS. Added one `<div class="col-auto">`
  beside the headcount input, and one line to `clearIntakeFields()` so a stale contribution value doesn't
  survive an LOS deselect — the same latent-bug class T87 fixed for the other intake fields, in the same
  function, no new mechanism.
- **New request parameter is `intakeContribution`, not `contribution`.** The form already has a hidden
  `<input name="contribution">` (`proposalBuilder.jsp:66`) echoing the *illustration hand-off's* own
  contribution parameter — an entirely different feature (AGE_BAND snapshot arithmetic in
  `attachAgeBandSnapshot`, which is proposal-pricing-adjacent and explicitly forbidden territory for this
  run). Reusing `contribution` would have silently collided (`request.getParameter` returns the first
  match) and pulled a value from the wrong feature into `proposal_ichra_intake`. The panel's own doc
  comment (`:190-193`) already warns about exactly this collision class for the intake*-prefix
  convention; `intakeContribution` follows it.
- **A typed negative contribution is treated as absent, not a fail-closed rejection of the whole intake
  row.** Every other field in `attachIchraIntakeIfPresent` is required — an invalid one aborts the whole
  write. Contribution is optional, so a negative or unparseable value is set to `null` and the rest of the
  (otherwise-valid) intake row is still written. The browser control already enforces `min="0"`; the
  server-side check is defense against a direct POST, not the expected path.
- **`formatCurrency(BigDecimal)` added as a one-line wrapper around the existing formatter**, rather than
  inlining `NumberFormat.getCurrencyInstance(Locale.US).format(...)` four times. This is the same
  formatter call `formatPremium` already uses, factored once for four call sites in one block — not a new
  formatter, and a smaller diff than four identical inline calls.
- **Flipped V087's Production cell in `migration_tracker.md` from ⬜ to ✅**, per the run prompt's explicit
  instruction. Verified before flipping (not assumed): `docs/session_closeout_2026-08-03_session10.md`
  states directly, "`V087__proposal_ichra_intake.sql` ... applied to production by Kevin on 2026-08-03,
  released in `v0.87.00`." Added a dated reconciliation note following the file's own established pattern
  (see the V077/V078, V080/V082, V079–V083 precedents already in the file) rather than just silently
  changing the cell.

## New assumptions

- **LA-S11A-1: MySQL rounds/truncates a `BigDecimal` with more than 2 decimal places to the column's
  declared `DECIMAL(10,2)` scale on insert, rather than rejecting it.** The entry field
  (`intakeContribution`) has `step="0.01"` client-side but nothing server-side clamps the parsed
  `BigDecimal`'s scale before `intake.setMonthlyContributionPerEmployee(...)`. No existing code in this
  file rounds a `BigDecimal` before persisting one (`attachRangeSnapshot`/`attachAgeBandSnapshot` don't
  either), so this matches the file's existing convention rather than introducing a new one — but it is
  unverified in this container, which has no database. **Reversal cost: trivial** — a
  `.setScale(2, RoundingMode.HALF_UP)` one-liner before `setMonthlyContributionPerEmployee`, if Kevin's
  post-deploy walk finds a value stored with unexpected precision.
- **LA-S11A-2: a negative-contribution POST is rare enough that "treat as absent" is an acceptable
  fail-open for an optional field.** The alternative (reject the whole intake row) was available and
  would have been more conservative. **Reversal cost: one `if` block** — change the guard to `return;`
  instead of `monthlyContribution = null;`.

## Open questions raised

- **Does the actual MySQL column round or truncate an out-of-scale insert?** (LA-S11A-1 above.) Settled
  by Kevin's post-deploy walk step 3 (enter a value like `250.999` and check what's stored), not by
  anything available in this container.

## Contradictions found

None against the build plan or existing docs. One internal accuracy gap in `ProposalBuilder.java`'s own
javadoc (the `attachIchraIntakeIfPresent` comment claimed *every* field fails closed, which stopped being
true the moment `intakeContribution` was added) — corrected in the same commit, not left to drift.

## ⚠️ Code-verified-only disclosure

**Everything in this close-out is code-verified only.** This container has no Tomcat and no database
connection. The build (`.\mvnw.cmd clean package`) passed, which confirms the code compiles and packages,
but nothing here has been exercised at runtime: no proposal was actually created with a contribution
value, no token was actually observed resolving in rendered HTML, and the V088 migration has not been run
against any database, including local. Session 10 is the standing evidence that a read finds less than a
walk — six code-verified claims across sessions 9–10 turned out to need correction after a runtime walk,
and T84 (a silent-wrong-rates defect) was found only by one.

## SQL close-out audit

- **SQL produced this run:** `docs/migrations/V088__proposal_ichra_intake_contribution.sql` — one
  `ALTER TABLE proposal_ichra_intake ADD COLUMN monthly_contribution_per_employee DECIMAL(10,2) NULL AFTER
  headcount;`, one `CREATE OR REPLACE VIEW schema_info AS SELECT 'V088' ...;`, one `INSERT IGNORE INTO
  schema_version (...) VALUES ('V088', ...);`. All three statements are in this one versioned migration
  file. No SQL was run against any database — no database is reachable from this container.
- **Orphaned `.sql` files:** none introduced. `docs/migrations/` still contains exactly one non-versioned
  file (`seed_ndt125_questionnaire.sql`), pre-existing and untouched.
- **Current highest migration version:** `V088` (`ls docs/migrations/*.sql | sort | tail -3` shows
  `V087__proposal_ichra_intake.sql`, `V088__proposal_ichra_intake_contribution.sql`,
  `seed_ndt125_questionnaire.sql`).
- **Pending deployment:** V088, everywhere, including Production. See "What Kevin does after this run"
  below.
- **Schema described but not scripted:** none. Every column referenced in the Java/JSP changes
  (`monthly_contribution_per_employee`) is in the V088 script.

## Compliance statement

Restating the scope fence: writable set was the V088 migration, `ProposalBuilder.java`,
`ViewProposal.java` (the `buildTokenMap` file), `migration_tracker.md`, `schema_version_migration.sql`,
`project_backlog.md`, and this close-out — plus `proposalBuilder.jsp` (pre-authorized, JSP-separate-from-
servlet case) and `ProposalIchraIntake.java` (not pre-authorized by name, added per the "Decisions made"
justification above, and disclosed here rather than silently). **`git status --short` before this commit
showed exactly those files and no others.**

Forbidden list — confirmed untouched: `replaceTokens` (not opened for edit), any read of proposal
pricing/fee/billing data (none — half 1 is intake arithmetic only, no `RateTable`/`Rate`/fee reference
anywhere in the diff), `putIchraMarketTokens`/`rating_area_rate_cache`/`source_env`/the provenance gate
(not touched — the new tokens are populated in the same unconditional block as the four existing intake
tokens, entirely separate from `putIchraMarketTokens`'s call), `IchraAccessResolver.isAvailable` /
`ICHRA_GATED_SECTION_TYPES` / `is_plus_tier` (not touched), `navbar25.jsp`/`css-js.jsp`/`AppConfig`/
`AmsDataGlobal`/`EmfListener`/`LoginFilter` (not touched), `docs/ichra_strategy.md` (not read), no
`INSERT INTO constant` in the migration (confirmed — the migration has no `constant` table reference at
all).

No forbidden git operation was run. Staging was by explicit named path (`git add <path> <path> ...`,
listed individually in the commit above) — no `git add -A`, no `git add .`. No `git stash`, `checkout`,
`restore`, `reset`, or local tag was created or run.

## What Kevin does after this run

1. Apply V088 to production via `update.sh` (applies migrations before swapping the WAR).
2. Cut the release: GitHub web UI → Draft a new release → **type** the tag `v0.88.00` → attach `ROOT.war`
   (renamed from `ams-1.0.0-SNAPSHOT.war`) plus `V088__proposal_ichra_intake_contribution.sql` → Publish.
3. Walk it: create a proposal with a contribution entered, and one with it blank, and confirm the cost
   block renders in the first and is absent — not blank-braced — in the second. While there, worth
   spot-checking LA-S11A-1 (enter a value with 3+ decimal places and confirm what's actually stored).
