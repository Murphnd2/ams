# Session close-out — 2026-09-13 — tier and schedule standards (S57)

## Shipped

One commit, verified via `git log`:

- **`d78cf44`** — "Coverage tier picker, payroll_frequency Summit seed, and alias-based recurrence
  filtering (V109-V111)". Per `git show d78cf44 --stat`, this commit carries `docs/analysis/migration_tracker.md`,
  `docs/analysis/technical_assumptions.md`, `docs/claude_memory.md`, `docs/schema_version_migration.sql`,
  the three new migrations (V109, V110, V111), the `CoverageTier`/`CoverageTierDAO`/`PaycycleFrequencyAlias`/
  `PaycycleFrequencyAliasDAO` new classes, and the `PayrollFrequency`/`PayrollFrequencyDAO`/`PayrollFrequencyAdmin`/
  `EnrollmentMatrixServlet` + their JSPs edits from that phase of the session (S57-P1 through P5). Pushed to
  `origin/refactor/modernize-architecture` (confirmed by `git log` showing it as the branch tip and the branch
  reading "up to date with origin").

## In flight

Per `git status` and `git diff --stat`, four files carry uncommitted changes — the S57-P6 schedule-suggestion
filter (TA-15/TA-17), not yet committed:

- `docs/analysis/technical_assumptions.md` — 1 insertion (TA-17 appended).
- `src/main/java/net/superiorstate/ams/controller/market/EnrollmentMatrixServlet.java` — 154 lines
  changed (three new methods — `filterSuggestedSchedules`, `buildSuggestedPayrollFrequencies`,
  `resolveSuggestedPayrollFrequency` — plus a copied `parseStrictIsoDate` helper, a generalised
  `resolveApplicationAnswer` in place of the single-purpose `resolveApplicationPaycycleFrequency`,
  two new field-key constants, and the `doGet` wiring that resolves three application answers and
  sets two new request attributes).
- `src/main/resources/packages/s125_fsa.json` — 1 line changed (the `paycycle_frequency` field's
  `selectOptions`, corrected to the six confirmed options).
- `src/main/webapp/WEB-INF/view/market/enrollmentMatrix25.jsp` — 32 lines changed (two `<optgroup>`s
  around the payroll-frequency `<select>`, plus a caption naming an unambiguous suggestion).

None of this is committed. It compiles (`./mvnw -q compile` ran clean at the end of that run) but has
not been exercised against a live setup — see "What is verified and what is not" below.

## What this session decided

**Tier standard.** Four Summit Tier IDs — `EE/Only`, `EE/SP`, `EE/CN`, `EE/FAM` — read off the Summit UI
and seeded into a new `coverage_tier` table (V109, 4 rows, confirmed by counting the `INSERT IGNORE`
values in the migration file). These are DataPath's "Tier Structure 3," the set Summit stamps into a
benefit plan's Coverage Levels/Tiers grid from its own dropdown — so the Tier ID string is never typed
by hand on the Summit side either. Per `docs/analysis/technical_assumptions.md` TA-14 (quoted below),
all HRA plans carry all four tiers even when the contribution does not vary by tier, because a flat
plan is simply configured with the same dollar amount on each of the four rows rather than using a
different tier count. `coverage_tier` has no consumer yet other than the enrollment matrix's tier
`<select>` (S57-P2, `EnrollmentMatrixServlet.buildCoverageTierOptions`), which writes the selected
`summit_tier_id` string into `enrollment_matrix_entry.tier_name` — the same column the free-text input
wrote before it, so no schema or exporter change was needed for the picker itself.

**Contribution schedule standard.** Summit's Contribution Schedules follow a `PP-` naming convention;
this session mirrored them into `payroll_frequency` (V110). Counted directly from the migration's
`INSERT IGNORE INTO payroll_frequency` statement: **14 rows**. One row, `PP-MO-1ST` ("Monthly, posts
on the 1st — plan-level, not selectable"), is seeded `enrollment_approved = 0` — the only row with
that value among the 14 — because it is the employer stipend / monthly-premium posting schedule
configured on the benefit plan itself, not a payroll-cycle choice, and must never appear in the matrix
dropdown. Five further rows are seeded `preferred = 0` (discouraged, but still enrollment-approved):
`PP-BW-THU-A-26`, `PP-BW-FRI-A-26`, `PP-WK-THU-52`, `PP-WK-FRI-52` (all labelled "every check (not
recommended)", 26/26/52/52 deductions respectively) — these are the "every check" variants of
schedules that also exist in a preferred, coalesced form (`PP-BW-THU-A-24`, `PP-BW-FRI-A-24`,
`PP-WK-THU-48`, `PP-WK-FRI-48`).

## Migrations

Counted directly from each file, not restated from a prior report:

- **V109** (`docs/migrations/V109__coverage_tier_reference.sql`) — creates `coverage_tier`, seeds
  **4 rows** (counted: `EE_ONLY`, `EE_SPOUSE`, `EE_CHILDREN`, `EE_FAMILY`). No consumer at the time it
  was authored; the tier `<select>` built on it landed in the same commit.
- **V110** (`docs/migrations/V110__payroll_frequency_seed_and_filter_metadata.sql`) — adds six nullable/
  defaulted filter-metadata columns to `payroll_frequency` (`recurrence`, `semimonthly_variant`,
  `pay_dow`, `anchor_date`, `deduction_count`, `preferred`), each behind its own guarded
  `information_schema` + `PREPARE`/`EXECUTE` check; seeds `payroll_frequency` with **14 rows** (counted)
  and a new table `paycycle_frequency_alias` with **5 rows** (counted — the file's own comment records
  that this was reduced from an original 7 once the application field's real option list was confirmed).
- **V111** (`docs/migrations/V111__drop_payroll_frequency_application_value.sql`) — one guarded
  `ALTER TABLE payroll_frequency DROP COLUMN application_value` (confirmed by reading the file's SQL
  directly), superseding that column with `paycycle_frequency_alias`.

**Current highest version: V111** (`ls docs/migrations/` sorted by version, confirmed). `docs/analysis/migration_tracker.md`'s "Current Highest Version" header line also reads V111 — internally consistent
with the migration files on disk.

**Dev and production versions: not independently verified by me — recorded per Kevin's report.** I
have no database client available and no way to open a connection from this environment (no `mysql`
binary found, and the local JDBC URL is an environment property not present in the tracked repo). Per
Kevin, dev is now at **V111** — he applied V105 through V111 and exercised the enrollment matrix in
the running app (see "What is verified and what is not" below for exactly what he checked).
`docs/analysis/migration_tracker.md` states Production is at V104 and that V105 through V111 are all
unapplied there; I have not verified Production myself either. The earlier 15-vs-14 `payroll_frequency`
row-count question is resolved — see "Corrections made this session" — both figures are correct: 14
from V110's seed, 15 in dev because of one pre-existing hand-created row, `BIWEEKLY24`.

## New assumptions

Read verbatim from `docs/analysis/technical_assumptions.md`. That register did not exist before this
session (created during S57-P2 Phase A investigation); TA-9 through TA-13 in it are copied verbatim
from `docs/session_closeout_2026-09-13_two_file_export.md`, where they were first recorded in an
earlier session the same day. TA-14 through TA-17 are new this session:

> **TA-14 — SSA's standard HRA tier set is DataPath's "Tier Structure 3":** `EE/Only`, `EE/SP`, `EE/CN`, `EE/FAM`, seeded in V109. Adopted because Summit stamps these into the benefit plan's Coverage Levels/Tiers grid from the Tier Group dropdown, so neither side types the string. All HRA plans carry all four tiers even when the contribution does not vary by tier — a flat ICHRA is configured with the same amount on each row. The Tier ID strings are read off the Summit UI, not off an import results file, so the match is unvalidated. `summit_tier_id` carries no unique constraint; two active rows sharing a value would collapse to one option in the picker. **Reversal cost: low** — four seeded rows and one `<select>`'s option list.
>
> **TA-15 — The matrix schedule dropdown will be filtered from two application answers**, `paycycle_frequency` (free-text dropdown) and `paycycle_first_paydate` (date), widened by `paycycle_other_have`. Day-of-week is treated as the strong signal: a Thursday employer hides all Friday schedules and vice versa. Bi-weekly hides all weekly; weekly keeps same-day bi-weekly. Bi-weekly parity is computed as `daysBetween(first_paydate, anchor_date) mod 14` — `0` is cycle A, `7` is cycle B. Semi-monthly resolves entirely from the answer text, with no date arithmetic. `paycycle_other_have = Yes` drops the cadence and parity rules and keeps only the day rule. An unrecognised or missing answer results in **no filtering**, never partial filtering. **Not yet implemented and not yet validated against real application data.** **Reversal cost: low** — the filter is one query predicate; the metadata columns are additive.
>
> **TA-16 — `payroll_frequency.application_value` (V107) is retired in favour of `paycycle_frequency_alias` (V110).** The column mapped a `paycycle_frequency` answer to a schedule at per-schedule grain and was read by one preselection path that returned the first match by sort order — wrong for three of four bi-weekly employers, since one answer legitimately matches Thursday/Friday × cycle A/B. The alias table maps answer text to a recurrence token at per-answer grain, and V110's `pay_dow` / `anchor_date` / `semimonthly_variant` columns carry the discrimination the column could not express. Payroll preselection is intentionally absent until the TA-15 filter ships. **Reversal cost: low** — one guarded `ADD COLUMN` and roughly 25 lines restored across five files; no data to restore.
>
> **TA-17 — The TA-15 suggestion is surfaced, never preselected.** Participant panels are hidden `<div>`s in a single form and hidden inputs submit, so a `selected` suggestion would persist on the next Save for every unlocked participant the operator never opened. The suggested `<optgroup>` renders first and a caption names the single preferred survivor; the operator still clicks. A date that fails strict ISO parsing, an unmatched alias, or a blank frequency answer all yield an empty suggested map and a flat list — never a partial filter. Bi-weekly parity uses `Math.floorMod`, since a first paydate earlier than the anchor is the normal case. **Reversal cost: low** — adding `selected` in the JSP is one word; removing the feature is deleting two optgroups and one method.

Note TA-15 itself (written before the suggestion was built) still says "will be filtered" and "not yet
implemented" — that text was not updated when TA-17 landed the actual implementation later the same
session. Both entries are left as written, per the register's own "appended, never edited in place"
rule; a reader needs both TA-15 and TA-17 to get the current picture.

## Corrections made this session

- **`payroll_frequency.application_value` (V107) retired via V111**, in favour of
  `paycycle_frequency_alias` (V110). What was wrong: `application_value` held one string per schedule
  row, matched by equality, and its one reader (`PayrollFrequencyDAO.findByApplicationValue`, since
  deleted) returned only the first match in sort order — so an answer like `Bi-Weekly`, which
  legitimately matches four seeded schedules (Thursday/Friday × cycle A/B), silently preselected the
  lowest-sort-order one regardless of which day or cycle the employer actually ran. The alias table
  maps answer text to a recurrence token instead of to one specific schedule row, and the new
  `pay_dow`/`anchor_date`/`semimonthly_variant` columns on `payroll_frequency` itself carry the
  discrimination a single string column could not.
- **The `paycycle_frequency_alias` seed was reduced from 7 rows to 5** once the application field's
  actual Select Options were confirmed. The original 7-row seed included two extra rows spelling
  "Semi-Monthy" (missing the "l") as a hedge against a possible misspelling in the configured dropdown
  text; once the real option list was confirmed as `Weekly|Bi-Weekly|Semi-Monthly 1st/15th|
  Semi-Monthly 15th/Last|Monthly|Other`, those two hedge rows were dead data and were removed. `Other`
  intentionally carries no alias row.
- **`s125_fsa.json`'s `paycycle_frequency` field's `selectOptions` corrected** this session (in the
  in-flight, uncommitted work) from `Weekly|Bi-Weekly|Semi-Monthly|Monthly` to
  `Weekly|Bi-Weekly|Semi-Monthly 1st/15th|Semi-Monthly 15th/Last|Monthly|Other` — confirmed by
  `git diff` showing exactly that one-line change. Before this correction, a plain `Semi-Monthly`
  answer matched no alias row (the alias table's two semi-monthly rows both carry a `1st/15th` or
  `15th/Last` suffix), so the schedule-suggestion filter would have silently degraded to "no
  suggestion" for every semi-monthly employer with no error surfaced anywhere. I found the
  JSON-to-`ApplicationField` sync logic in
  `src/main/java/net/superiorstate/ams/data/service/PackageLoader.java`
  (`resetSectionToDefault`, line 221), which does overwrite an existing field's `selectOptions` from
  the JSON on each call, confirming the mechanism V107's own migration comment described — **the
  migration comment (`docs/migrations/V107__payroll_frequency.sql:6`) names this method
  `restoreDefaults`; that name does not exist anywhere in the codebase (confirmed by a `src/`-wide
  grep). The real method is `PackageLoader.resetSectionToDefault`. `restoreDefaults` is a misnomer
  that originated in this session's own prior analysis and then propagated into later prompts and
  this document — not a name that ever existed in code.** I did not trace whether or when
  `resetSectionToDefault` is actually invoked against the `s125_fsa` package in a running system, so
  the practical reseed risk that motivated the `s125_fsa.json` fix is stated above but unconfirmed —
  only the overwrite mechanism itself, once triggered, is confirmed to behave as described.
- **Stale V110 comments and tracker figures corrected** (S57-P5, same commit as the rest of this
  session's shipped work): the V110 migration's own header comment previously said its
  `paycycle_frequency_alias` seed had been "cross-checked" against an authoring prompt's table with
  "two of seven" keys disagreeing — stale once the alias seed was cut to 5 rows and the disagreeing
  rows removed. `docs/analysis/migration_tracker.md`'s V110 entries (both the top summary and the
  full-table row) repeated the same "7 seeded rows"/"two of seven" language and were corrected to
  match the 5-row reality.
- **The 15-vs-14 `payroll_frequency` row-count discrepancy is resolved — per Kevin, not from the
  repo.** V110 seeds 14 rows, confirmed by counting the migration's `INSERT IGNORE` statement (see
  "Migrations" above). Kevin reports dev's `PayrollFrequencyAdmin` grid shows 15 because it also
  contains `BIWEEKLY24`, a row he created by hand before V110 existed. `BIWEEKLY24` is the value
  stored on saved matrices in dev and cannot be deleted without orphaning them; Kevin has unchecked
  its Enrollment Approved flag, which drops it from the dropdown for new selections while the
  `(inactive)` passthrough (`EnrollmentMatrixServlet.buildPayrollFrequencyOptions`) keeps it visible
  and selected on any row that already references it. I have not verified any of this against the
  database myself — it is Kevin's report of dev's current state, recorded here because the next
  session needs it, not because the repo proves it.
- **The TA-15 preselect decision was overturned to surface-only.** The original design intended an
  unambiguous suggestion to be preselected (`selected` on the option). This was overturned because
  the enrollment matrix's participant panels are hidden `<div>`s inside one `<form>`, and a hidden
  `<select>`'s value still submits with the form — so a `selected` suggestion would silently persist
  onto every unlocked participant's row the next time anyone clicked Save, including rows the
  operator never opened. The shipped (uncommitted) mechanism instead renders the suggested schedules
  in a `<optgroup>` first and names the single preferred survivor in a caption below the `<select>`;
  the operator still has to click it.

## Open questions and deferred items

- **D-93** — confirmed via `docs/deployment_backlog.md` line 1585 onward: `SUMMIT_IMPORT_TEMPLATES`
  needs a fourth entry (`enrollment:<templateName>`) so the HRA Enrollment download resolves a
  Summit-import-bindable filename instead of its legacy descriptive one. Status per that file:
  "Not started anywhere," including on Kevin's local dev.
- **TA-12** — `MONTHLY_PREMIUM` annualising (× 12) into column G of the 125 PI Elections file —
  unverified against any real Summit import, per the assumption's own text.
- **Effective date on the HRA/125 enrollment files is still sourced from the `plan_year_start`
  application answer, not `SummitPlanDateRuleResolver` (V103).** Confirmed by reading
  `SummitExportServlet.java` line 1738-1739 directly: "`Effective Date` is the `plan_year_start`
  answer for both, as file 2 emits it." `SummitPlanDateRuleResolver` exists and is called elsewhere in
  the same file (file 2's CDH plan emission, lines 1001/2031) but not for this column.
- **`migration_tracker.md` is missing its V107 table row** — confirmed by grep: the versioned table
  jumps from a `| V106 |` row straight to `| V108 |`; V107 appears only in the file's narrative
  "Current Highest Version" summary section above the table, never in the table itself. Pre-existing
  drift from a prior session; it ships separately, not touched this session.
- **`coverage_tier` has no admin CRUD screen.** Confirmed: no `CoverageTierAdmin` class or JSP exists
  anywhere in the repo (the only hit for that name is a mention inside `migration_tracker.md`'s prose).
  `payroll_frequency` has one (`PayrollFrequencyAdmin.java` + `payrollFrequencyAdmin25.jsp`).
- **Two Summit schedules are not yet seeded: `PP-BW-THU-B-26` and `PP-BW-FRI-B-26`.** Confirmed by
  grep against V110's seed — no row of either name exists. The B-cycle "every check" (26-deduction)
  variants exist for neither Thursday nor Friday; only the A-cycle 26-deduction variants
  (`PP-BW-THU-A-26`, `PP-BW-FRI-A-26`) are seeded. A bi-weekly cycle-B employer who deducts every
  check rather than the coalesced 24 currently has no matching schedule row at all.
- **The repo-wide grep for JSP variables shadowing an EL implicit object has not been run.** One
  instance (`header`) was found and fixed in an earlier session the same day (commit `70c0e0e`,
  confirmed in `git log`), but that was a single fix, not a systematic search across every JSP for
  every implicit-object name (`param`, `paramValues`, `cookie`, `pageContext`, `pageScope`,
  `requestScope`, `sessionScope`, `applicationScope`, `headerValues`, `initParam`). I ran a narrow,
  three-name check (`header`/`param`/`cookie`) against `src/main/webapp` this session and found no
  further hits, but that is not the repo-wide audit this item calls for.
- **`BIWEEKLY24` cleanup, per Kevin's report (not independently verified).** A hand-created
  `payroll_frequency` row predating V110, now Enrollment-Approved-unchecked but still referenced by
  saved matrices in dev. Every saved matrix referencing it needs its schedule re-picked to a `PP-`
  name before that matrix can export correctly — `BIWEEKLY24` is not a Summit Contribution Schedule
  name and would fail or misroute on import. The `(inactive)` marker on the payroll `<select>` is how
  those rows are found (any cell showing `BIWEEKLY24 (inactive)` needs attention). Once no saved row
  references it, the row itself can be removed.
- **`buildSuggestedPayrollFrequencies` and the preselect resolution each run the day/recurrence/
  parity filter independently** — confirmed by reading the uncommitted `EnrollmentMatrixServlet.java`:
  `doGet` calls `filterSuggestedSchedules` a second time (inside the `resolveSuggestedPayrollFrequency`
  call) rather than reusing the list already computed for the map. Two reads of a small reference
  table (14 rows) per request. Recorded as a known inefficiency, not treated as a defect, and not
  fixed this session per this run's own no-fix instruction.

## What is verified and what is not

- **Runtime-verified in dev, by Kevin — his in-app check, not something this repo or I confirmed.**
  Per Kevin: dev is at V111 (he applied V105 through V111 himself) and he exercised the enrollment
  matrix's two stored-value paths in the running app:
  - The payroll `<select>` renders `BIWEEKLY24 (inactive)` and keeps it selected, after V111 deleted
    the old preselection path — the `(inactive)` passthrough survived that retirement.
  - The ICHRA tier cell renders the blank `— choose —` option, selected, with the four tier options
    behind it, on a cell with no stored tier.
  - Save with nothing changed leaves every value unchanged after a reopen — both the inactive payroll
    value and the blank tier.
  - A tier set, saved, and reopened comes back preselected.

  That is the full round-trip on both stored-value paths (payroll frequency and coverage tier). I did
  not observe any of this myself and have no database or application access this session — it is
  recorded here as Kevin's report, attributed to him, not presented as something the repo proves.
- **Still not runtime-verified, and kept clearly separate from the above:** the TA-15 schedule
  suggestion filter (`filterSuggestedSchedules`/`buildSuggestedPayrollFrequencies` — compile-only;
  needs a setup whose application answered all three `paycycle_*` fields, which Kevin's round-trip
  check above did not exercise), V111's effect on the `PayrollFrequencyAdmin` page itself (the
  `application_value` column and form field were removed from that page's code, but nobody has
  reported opening that specific page since), and anything at all against a real Summit import.
- **Compile-verified only:** all Java changes across the session — the V109/V110/V111 entity and DAO
  additions, the `PayrollFrequency`/`PayrollFrequencyDAO`/`PayrollFrequencyAdmin` edits, the
  `EnrollmentMatrixServlet` tier-picker and schedule-suggestion additions — compiled cleanly under
  `./mvnw compile` each time it was run this session. JSP changes (`enrollmentMatrix25.jsp`,
  `payrollFrequencyAdmin25.jsp`) were not compiled by that step; JSPs compile at Tomcat deploy time,
  which did not happen from this session directly (Kevin's own dev deploy, above, is the only compile
  of the JSPs on record).
- **Not verified at all, by anyone, as far as this close-out has evidence of:** the TA-15 schedule
  filter against real application data; V111's `PayrollFrequencyAdmin` page with the column removed;
  and, as stated below, anything against a real Summit import.

⚠️ **Nothing in this session has been tested against a real Summit import.** The four Tier ID strings
and the fourteen Contribution Schedule names were read off the Summit UI (per TA-14's and V110's own
migration comments) and are not confirmed by an import results file — the same caveat TA-11 already
carries for the two-file export's column contracts generally.

## SQL close-out audit

- **Every SQL statement produced this session** is inside the three versioned migrations already
  listed above: V109 (`CREATE TABLE` + 4-row seed + `schema_info`/`schema_version` blocks), V110 (six
  guarded `ALTER TABLE` blocks + 14-row seed + `CREATE TABLE` + 5-row seed + `schema_info`/
  `schema_version` blocks), V111 (one guarded `ALTER TABLE ... DROP COLUMN` + `schema_info`/
  `schema_version` blocks). No SQL was written outside a migration file.
- **No SQL was run.** No database connection was made and no statement was executed by Claude Code at
  any point in this session.
- **No orphaned `.sql` files** were found beyond the three migrations named above — `ls docs/migrations/`
  shows no unversioned or stray `.sql` file introduced this session.
- **Current highest version: V111.**
- **Pending deployment:** per `docs/analysis/migration_tracker.md`'s own (unverified-by-me-this-session)
  record, V105 through V111 are all unapplied on every environment, with Production at V104.
- **Schema described but not scripted:** none identified this session — every column and table this
  session discusses (the six `payroll_frequency` filter-metadata columns, `coverage_tier`,
  `paycycle_frequency_alias`) has a corresponding migration statement; nothing was designed in prose
  without a matching script.

## Next

The recommended next step is **closing D-93** (adding the `enrollment:<templateName>` entry to
`SUMMIT_IMPORT_TEMPLATES`) so a real HRA Enrollment file can actually be pushed to Summit's import
queue — right now the file downloads with a legacy filename no Summit template will bind to, which is
the one thing standing between this session's schema work and an actual import result. A real import
is the single event that would settle TA-12 (whether the annualised `MONTHLY_PREMIUM` math lands
correctly in Summit), TA-14 (whether the four Tier ID strings match character-for-character), and the
fourteen `PP-` schedule names simultaneously — all three are currently "read off the Summit UI,
unconfirmed," and one clean import turns all three from assumption to fact at once.

## Compliance statement

1. Only this file, `docs/session_closeout_2026-09-13_tier_and_schedule_standards.md`, was created.
   No other file was modified, created, or deleted.
2. No git mutation command was run — only `git log`, `git status`, `git diff --stat`, and read-only
   `grep`/`Read` calls. No `mvnw`, no database connection, no SQL execution.
3. Facts I could not verify from the repo, and what I wrote instead:
   - **Whether dev is actually at V111, whether the `payroll_frequency` admin screen there shows 15
     rows, and whether the tier-picker/payroll-select round trip actually behaves as described in the
     running app.** I have no database or application access this session. These are now recorded as
     Kevin's own in-app report (the "Runtime-verified in dev, by Kevin" subsection and the
     `BIWEEKLY24` correction above), explicitly attributed to him rather than presented as something
     I or the repo confirmed. Both the 14-row (V110 seed) and 15-row (dev grid) figures are recorded
     as correct, with `BIWEEKLY24` as the reconciling fact — not as an unresolved mismatch, since
     Kevin's explanation accounts for the difference.
   - **Whether `PackageLoader.resetSectionToDefault` is ever actually invoked against the `s125_fsa`
     package in a running system**, and therefore whether an uncorrected `paycycle_frequency` option
     list would really have been overwritten on a real reseed. I verified the method exists and what
     it does to `selectOptions` when called, but did not trace its caller(s) or confirm this package
     is reachable from them — that trace remains unfollowed, so the practical reseed risk is stated
     but unconfirmed.
   - **Runtime behavior of the TA-15 schedule filter, `PayrollFrequencyAdmin` under V111, and anything
     against a real Summit import** — none of these has been exercised against a live system that I
     have evidence of, and they remain explicitly separate from the tier-picker/payroll-select round
     trip Kevin did report checking. I wrote "not verified at all" for these rather than inferring
     success from the code compiling or from an unrelated check.
   - I no longer treat `PackageLoader.restoreDefaults`'s status as an open question: it is confirmed
     not to exist in the codebase (a `src/`-wide grep found zero occurrences), the real method is
     `PackageLoader.resetSectionToDefault`, and the wrong name is confirmed to have originated in this
     session's own prior analysis before propagating into later prompts and this document. Two
     occurrences of the wrong name remain uncorrected outside this close-out's scope this run —
     `docs/migrations/V107__payroll_frequency.sql:6` (a migration file, out of scope to edit) and
     `docs/session_closeout_2026-09-12_payroll_frequency.md:17` (a different, already-closed session's
     close-out, not named in this run's scope fence) — both reported here rather than corrected.
