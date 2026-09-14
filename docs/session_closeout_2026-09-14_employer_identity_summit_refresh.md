# Session close-out — 2026-09-14 — Employer identity fix and scheduled Summit refresh (S61)

Branch `refactor/modernize-architecture`. Runs S61-P1 (read-only writer audit), P2 (Phase A diff
spec), P3 (build), P4 (commit/push), P5 (read-only refresh ingestion audit), P6 (build:
`SummitRefreshService`), P7 (commit/push), P9 (build: skip-if-already-imported guard), P10 (wire
`force` into `/SummitRefresh`, commit/push), P11 (this close-out). Every repo fact below was read
from the tree at close-out time, not restated from the prompts.

⚠️ **This supersedes `docs/session_closeout_2026-09-14_S61-P8_session_closeout.md` in full**, per
this run's own instruction — but that file does not exist anywhere in the tree (`git ls-files | grep
-i S61-P8` returns nothing). There is nothing to discard; recorded as a contradiction below rather
than silently ignored.

## Shipped

**Three commits**, each verified with `git show --stat`, plus this close-out's own fourth:

- **`6778881`** — "Employer identity: stop legacy I1 promotion writing Employer.altId (S61)". **3
  files changed, 10 insertions(+), 1 deletion(-)**: `docs/analysis/project_backlog.md`,
  `docs/analysis/technical_assumptions.md`, `src/main/java/net/superiorstate/ams/data/service/Updater.java`.
  `Updater.java:80`'s `employer.setAltId(row.getEmployerId())` replaced with a four-line comment;
  `SummitImportService.importEmployers` (the `/SummitImport` wizard) becomes the sole writer of
  `Employer.altId`. Files T265–T269 and TA-51.
- **`9ec635c`** — "Summit refresh: scheduled J1 employer import, off by default (S61)". **6 files
  changed, 728 insertions(+), 1 deletion(-), 2 files newly created**:
  `src/main/java/net/superiorstate/ams/controller/market/SummitRefreshServlet.java` (new),
  `src/main/java/net/superiorstate/ams/data/service/SummitRefreshService.java` (new),
  `src/main/java/net/superiorstate/ams/EmfListener.java`, `docs/deployment_backlog.md`,
  `docs/analysis/project_backlog.md`, `docs/analysis/technical_assumptions.md`. Files D-102–D-105,
  T270–T275, TA-52, TA-53, and resolves T268 in place.
- **`ae2e196`** — "Summit refresh: skip when the newest export was already imported (S61)". **3
  files changed, 117 insertions(+), 14 deletions(-)**: `docs/deployment_backlog.md`,
  `src/main/java/net/superiorstate/ams/controller/market/SummitRefreshServlet.java`,
  `src/main/java/net/superiorstate/ams/data/service/SummitRefreshService.java`. Covers two runs'
  work committed together — S61-P9's skip-if-already-imported guard and max-age default change
  (`SummitRefreshService.java`, `docs/deployment_backlog.md`), and S61-P10's `force` control wired
  into `/SummitRefresh` (`SummitRefreshServlet.java`).
- **This close-out** — one new file, `docs/session_closeout_2026-09-14_employer_identity_summit_refresh.md`.

## In flight

**Nothing.** `git status --porcelain` at close-out start was empty against HEAD `ae2e196`.

## Decisions made — and what each closed

- **The employer-identifier defect was a misbound staging column, not a fuzzy header match.**
  `Importer.java:46` binds `import1employer.Employer_ID` to J1 column `EmployerOrganizationID`,
  which duplicates `OrganizationID` in all 693 rows of a real export (`fed186fd`, 2025-10-09, a year
  before this session); `Updater.java:80` copied that into `employer.employer_id`. Silent: it opened
  a different employer's Summit page, fully rendered, never an error.
- **Option B over Option A.** The legacy path (`Updater`) stops writing `altId` rather than
  repointing `Importer.java:46`'s header override. Deciding facts, established S61-P1/P2:
  `import1employer.Employer_ID` had exactly one reader (the line removed); `Coverage.employerId` has
  no reader outside the model; every Summit-link reader already refuses cleanly on `altId == 0`; and
  repointing the override would change which files `Importer.importAllMatchingFilesInMappingOrder`
  accepts — a 20-column J1 file (the demo fixture's own shape) would start being rejected, a
  behavior change for the existing monthly-billing line of service. Filed as T266 instead of fixed.
  Reversal cost, per TA-51 (below, since it names the same line): **low** — a code revert of
  `Updater.java:80` restores the prior (wrong) behavior.
- **The wizard is now the sole writer of Summit employer identity**, and repairs an `altId` of `0`
  or a previously-wrong value on its next run against a 21-column export (`SummitImportService.java:317`,
  `if (employerId > 0 && existing.getAltId() != employerId) existing.setAltId(employerId)`). TA-51
  records this basis as **read, not runtime-verified** — see Open questions.
- **T268 resolved — accepted, not fixed.** Read verbatim from `project_backlog.md`: "Summit's
  employer `CustomID` is blank on creation unless AMS's own push supplies it; the `S{EmployerID}`
  and legacy four-digit values in the 693-row export belong to employers set up manually before the
  AMS-composed key existed. Kevin accepts the import overwriting `employer.custom_id`, with T272 (a
  mismatch audit report from an export) as the detection fallback." This closed whether `custom_id`
  is a safe join key for `/SummitLink` — it is accepted as one, by decision, not by proof.
- **J1 only for the first refresh cut.** J2/J3 reverts AMS-typed contact names (`ModifyContact25.java:90-91`
  vs `SummitImportService.java:450-451`); J4/J5/J7 carries `is_active` and renewal side-effects.
  Filed as T274, explicitly deferred rather than built partially.
- **`audit_run` as the run-record home, no schema change.** TA-53 lists the exact columns read
  (`id`, `psp_id`, `check_key`, `run_at`, `run_trigger`, `status`, `finding_count`, `summary`,
  `error`, `duration_ms`) and confirms every value this session writes fits without alteration.
  `import_run_log` was rejected because its `run_by BIGINT NOT NULL` has no person for a scheduled
  run — recorded, not worked around.
- **Refresh config split by domain.** The enable flag, `SUMMIT_REFRESH_ENABLED`, is a DB `constant`
  read via `AppConstantDAO.getConstantValue`, mirroring `AUDIT_SCHEDULER_ENABLED` exactly (same
  mechanism, same "absent means off" default). Fetch tunables are `ssa.properties`, read via
  `AppConfig.get`, mirroring `IchraUncodedParticipantsCheck`'s own keys. Two existing keys were
  **reused rather than duplicated**: `SUMMIT_SFTP_IMPORT_DIR` (the `ExportFiles` sibling) and
  `SUMMIT_AUDIT_EXPORT_MAX_BYTES` (the byte cap, default 16 MiB) — confirmed in code as
  `CFG_IMPORT_DIR`/`CFG_MAX_BYTES` constants in `SummitRefreshService.java`, not new keys.
- **`ZZ_J1_Employer` naming, as configuration not code.** D-103 records the exact expected filename
  shape, concretely: `ZZ_J1_Employer_Export_20260914081530123.CSV` — the configured prefix, then
  Summit's own `_Export_` + 17-digit `yyyyMMddHHmmssSSS` timestamp + extension, matched against the
  same regex `IchraUncodedParticipantsCheck` already uses. The prefix lives in `ssa.properties`
  rather than a constant because it is a property of the Summit-side template name, which a second
  installation's tenant can name differently, and because an `ssa.properties` edit costs a Tomcat
  restart, not a rebuild — the same rationale D-91's `SUMMIT_IMPORT_TEMPLATES` already established
  for export template names.
- **The fetch pattern was lifted, not extracted.** `SummitRefreshService`'s class Javadoc states
  plainly that its fetch logic "mirrors `IchraUncodedParticipantsCheck`" — the `ExportFiles`
  derivation, the newest-timestamped-file selector regex, the byte-cap read, the BOM strip are all
  duplicated code, not a shared helper. `IchraUncodedParticipantsCheck.java` itself was never opened
  for edit across any of the four build runs (confirmed: it appears in every run's "May NOT be
  touched" fence and in none of the four commits' file lists). No shared-surface churn for a first
  cut; the duplication is the known cost, not fixed here.
- **Freshness gates on file identity, not age — the session's second substantive design decision,
  and the reason for `ae2e196`.** Summit's most frequent export schedule is daily, so Kevin
  configured four daily schedules running the same export at 7am, 9am, 11am and 1pm. That made the
  age guard the wrong control: after 1pm the newest file is stale for roughly 18 hours, so both the
  scheduled tick and Run now would have refused a perfectly usable snapshot. Loosening the age limit
  alone would instead have re-imported the same file every tick. The tick now skips when the newest
  matching filename is the one already imported successfully, before fetching any bytes — four
  imports a day, matching the four exports. The age guard survives with a narrow purpose, per
  D-104's own text: "catch an export that has stopped running entirely."
- **The identity check deliberately does not read the shared `latestRun` field.** Every `record()`
  call overwrites `latestRun`, skips included, so comparing against it directly would have produced
  an import/skip/import/skip oscillation (a `SKIPPED` record makes the *next* tick see a non-`OK`
  `latestRun` and re-import). A dedicated field, `lastImportedFilename`, is set only on a successful
  import (`SummitRefreshService.java`, step 7 of `runOnce`) and seeded at construction from the
  persisted row only when that row is `STATUS_OK` and its `summary` parses via the
  `IMPORTED_MARKER`-prefixed shape (`parseImportedFilename`); anything doubtful — absent, wrong
  status, unparseable — falls through to "proceed with the import," never a wrong skip. Recorded
  explicitly here because it is non-obvious, and the class's own Javadoc says so in nearly these
  words, precisely so the next person to touch this code does not simplify it back into the bug.
- **Force is a PSP-admin control, never a scheduled behavior.** `/SummitRefresh` renders a second
  form, "Force re-import," alongside the unchanged "Run now" form; `doPost` reads
  `"true".equals(request.getParameter("force"))` (absent, malformed, or any other value all default
  to `false`) and calls `triggerManual(force)`. `scheduledTick()` calls
  `runAcquired(TRIGGER_SCHEDULED, false); // scheduled tick never forces` — a hardcoded literal, not
  a variable, so no code path lets a scheduled tick force.

## New assumptions

Three entries filed in `docs/analysis/technical_assumptions.md` this session — read verbatim, not
restated:

- **TA-51** — `SummitImportService.importEmployers` is the sole writer of `Employer.altId` and
  repairs rows left at `0` or previously wrong on its next run. **Basis:** `SummitImportService.java:317`,
  quoted in the entry. **Status: read, not runtime-verified.** **Reversal cost: low** — a code revert
  of `Updater.java:80` restores prior behavior. This is what the plan to repair rows 1400/1401 by
  re-import rests on — nobody has run the wizard against a real, current J1 export and confirmed it
  actually corrects a pre-existing wrong value.
- **TA-52** — the J1 employer export does not currently land in Summit's SFTP `ExportFiles`
  directory; no observation of one exists in the tree (two Summit-written files observed there, 2023
  and 2025, neither a J1; one audit participant export). `SummitRefreshService` is therefore inert
  until Kevin configures that export Summit-side, named `ZZ_J1_Employer`. **Risk if wrong:** none to
  data — a tick with no matching file is a recorded no-op. **Reversal cost: none.**
- **TA-53** — `audit_run` is an acceptable home for refresh run records without a schema change.
  **Basis:** the exact column list (above) and the exact values this service writes to each,
  verified to fit. Records two accepted side-effects: an `ERROR` refresh row lights the audit
  navbar badge (`AuditService.getErrorCount()`), and `import_run_log` was rejected for its `NOT NULL`
  `run_by`. **Reversal cost: low** — a migration adding a purpose-built table, with
  `SummitRefreshService.record` as the only caller to repoint.

## Open questions raised

- **Rows 1400 and 1401 are still wrong in the database.** The correct values exist in the export.
  This is Kevin's SQL and Kevin's decision to record, not a work item and not a migration — no
  migration was written or proposed for this at any point in the session. Unblocked by T268's
  resolution (the custom_id-as-join-key question no longer stands in the way of deciding how to
  repair these rows), but not resolved.
- **Three things must happen before the refresh does anything:**
  1. `SUMMIT_REFRESH_ENABLED` does not exist as a `constant` row anywhere (verified: no migration or
     seed inserts it; D-102's own status line reads "Not set anywhere"), so the scheduled tick is
     off. D-102 through D-105 are the deployment items recording this. Run now works regardless of
     this flag — confirmed in code (`SummitRefreshServlet.gate()` never checks it).
  2. The Summit-side export template must be named `ZZ_J1_Employer` and must include the `EmployerID`
     column (TA-52). No J1 has been observed landing in the export directory as of this close-out.
  3. **D-104 — the clock/timezone interaction is materially reduced, not resolved.** Quoting the
     backlog row directly: "Worst case: an 18-hour-old file (the overnight gap) plus 6 hours of skew
     computes to ~24 hours — inside the 26-hour default, but by only a ~2-hour margin. A late morning
     run, or any additional delay, can still push a genuinely fresh file over the guard on a
     UTC-clocked server." Confirm the server clock before enabling the scheduler.
- **The audit navbar badge — answered this run, not left open.** Traced `navbar25.jsp`: the badge
  block (`applicationScope.auditService.actionCount`/`errorCount`, lines ~271–279) sits nested inside
  the single `<c:if test="${sessionScope.isPspAdmin}">` opened at line 218 and closed at line 287 —
  confirmed by matching indentation and counting every `<c:if>`/`</c:if>` between them (one nested,
  balanced pair for `isMasterSystem` is the only other tag in that span). **The badge is PSP-admin
  only**, the same population as `/SummitRefresh` itself, so a refresh `ERROR` row lighting it is not
  a rule-2 exposure — nobody sees it who doesn't already know the capability exists. TA-53 already
  names this as an accepted side-effect; this run traced the JSP to confirm the population claim
  rather than assume it.
- **Nothing in the refresh path has been runtime-tested.** No tick has run, no file has been
  fetched, no import has been driven by the service, `SUMMIT_REFRESH_ENABLED` was never set. Every
  claim about `SummitRefreshService`'s behavior in this close-out and in TA-51/52/53 is read from
  code, not observed.

## Contradictions found

1. **This close-out's own supersede target does not exist.** `S61-P8_session_closeout.md` is named
   nowhere in `git ls-files`, `git log`, or the working tree. Nothing was discarded because nothing
   was there to discard.
2. **S61-P1's prompt asserted the wrong mechanism, and the run itself killed it with evidence.** It
   carried "a loose header match hits `EmployerOrganizationID` before `EmployerID`" as the cheapest
   explanation. Dead for the wizard (`SummitImportService` matches header keys exactly, no
   normalization); the real cause was an explicit override at `Importer.java:46`. The hypothesis was
   labelled as one and the run disproved it rather than building on it — the process worked, but the
   framing pointed away from the answer.
3. **CRLF anchoring — the transferable lesson.** S61-P3's verbatim `FIND` block for `Updater.java:80`
   was given without specifying line endings. The tree is CRLF (confirmed again this run: `file` on
   every touched `.java` file reports "with CRLF line terminators"); a naive LF-based match reported
   0 hits on a line that verifiably existed. The run verified the encoding before editing rather than
   widening the match or guessing. Same defect class as S60's line-number drift (T188) and S59's own
   prior instance of it.
4. **S61-P6's prompt got three tree facts wrong, all caught before any edit was made.**
   `docs/deployment_backlog.md` ran to **D-101**, not D-93, at that point (confirmed again now:
   `grep -o "^### D-[0-9]*"` sorted numerically tops out at D-105 as of this close-out, after this
   session's own D-102–D-105). `EmfListener.java` lives at
   `src/main/java/net/superiorstate/ams/EmfListener.java`, not under a `data/listener/` package that
   does not exist. `project_backlog.md`'s own priority legend (`:11-16`) uses `MED`, not `MEDIUM` —
   confirmed again this run by reading the same legend block.
5. **S61-P5's prompt understated the overwrite surface.** It framed `custom_id` as "the one overwrite
   we knew about." The wizard also unconditionally or conditionally overwrites
   `employer.employer_name/email/phone/contact_name` and
   `employee.first_name/last_name/email/address*/is_active` on every run — read again from
   `SummitImportService.java` this close-out. Only `employee.first_name/last_name` (and `email` under
   the Universal/`UniversalImportService` path specifically) are dual-written by an AMS user surface
   (`ModifyContact25.java:90-93`); everything else the wizard overwrites has no AMS-side writer to
   collide with.
6. **S61-P9's prompt named a data source that would have reintroduced the bug if followed literally.**
   It pointed at the `audit_run` row's `summary` as the source for "the last successfully imported
   filename" via "whatever mechanism the file already uses to read that row" — i.e., the existing
   `latestRun` field — without noting that *every* `record()` call overwrites that field, including a
   `SKIPPED` one. Read literally, the skip check would have compared against `latestRun` and
   oscillated import/skip/import/skip forever, since a skip immediately un-sets the condition that
   caused it. The build caught this and introduced `lastImportedFilename`, a field `record()` never
   touches, seeded from the persisted row only via a marker-prefixed parse. A prompt specifying a
   data source without checking what else writes it.
7. **A UTF-8 double-encoding incident during S61-P6, self-corrected within the same run.** A
   byte-level `perl` pass double-encoded `EmfListener.java`'s existing non-ASCII characters (an em
   dash, `✅`, `⚠️`, `🔁`), producing 12 spurious deletions alongside the intended 23 insertions.
   Restored via `git show HEAD:… > file` (a plain file overwrite, not `checkout`/`restore`, so no
   forbidden git command was used), re-normalized to CRLF to match the working copy's convention,
   verified `git status` clean for that file, then re-applied the four edits with byte-only escapes.
   Final diff, confirmed at the time and re-confirmed via `git show --stat 9ec635c` this close-out:
   23 insertions, 0 deletions in that file. Every subsequent run in this session (S61-P9, S61-P10)
   was warned of this exact failure mode and checked its own diff's deletion lines individually
   before proceeding — confirmed in both runs' own reports.
8. **`CLAUDE.md` still names V073 as the latest migration** (`CLAUDE.md:53`, `:132`, re-read this
   close-out); `docs/migrations/` holds through **V113** (`ls docs/migrations | sort -V | tail -1`).
   Pre-existing, unrelated to S61, and `CLAUDE.md` names itself a snapshot instructing the reader to
   check `ls docs/migrations/` directly — expected drift, already recorded by S60's own close-out,
   confirmed unchanged.
9. **The prompt's own "Next" section claim is wrong against the tracker, and is corrected rather than
   restated below.** "V112 and V113 applied to dev; production at V104 with V105–V113 pending" does
   not match `docs/analysis/migration_tracker.md`. V112 and V113's own rows mark every environment
   column `⬜`/`N/A` — no column shows applied — and each row's own narrative text states "Pending
   everywhere — no environment has taken this one," for both. V104's own row marks its
   **Production column `⬜`** (not applied), which is itself in tension with narrative text elsewhere
   in the same document (V108's row: "Production is at V104") — a pre-existing internal
   inconsistency in the tracker, not introduced by this session and not resolved here. The accurate
   statement, read from the tracker as it stands: **nothing past V104 is recorded as applied
   anywhere**, and even V104 itself is marked unapplied to Production on its own row.

## Noted, not fixed

- The "Force re-import" control has no confirmation dialog, unlike some comparable controls
  elsewhere in the codebase (e.g. the Enrollment line's `onclick="return confirm(...)"` choosers). A
  stray click re-imports the same snapshot; cost is one redundant idempotent import, and the overlap
  guard still prevents concurrency. Recorded as acceptable in S61-P10's own report.
- `TicketHelper.resolveContactEmail` and `PersonDAO.refreshPersonDataFromEmployeeData` have no
  callers — filed as T275, not removed.
- `import_run_log.run_by` remains `NOT NULL` with no accommodation for a scheduled (personless) run.
- T252 (opening the Summit setup panel to PSP user) still lists only the original five fragment
  servlets plus the outer gate as needing to change together — its own row text has **not** been
  updated to name `/SummitRefresh` as a sixth. Recorded here rather than silently assumed done; see
  Next.
- Whatever else the four build/audit runs flagged in passing and filed as their own `T-`/`D-` rows —
  read those backlogs directly rather than this list.

## Next

Recommended next step and why, plus other unblocked candidates — none gating each other. Read the
backlogs for current state rather than trusting this list:

- **The larger goal: export-based push verification.** Four pushes exist — employer, plans, debit
  card issuance, enrollments. The exports themselves need looking at first: what each contains, what
  identifier correlates a row back to an AMS record, whether a given push is confirmable from an
  export at all. Both identifiers this depends on are now settled (`Employer.altId`'s sole writer,
  and `custom_id`'s accepted-overwrite status), which was the blocker; `SummitRefreshService` is the
  first piece of machinery in the tree that reads an export on a schedule, and the same fetch shape
  could extend to the other three.
- **Arming and runtime-testing the refresh once the export exists** — the cheapest way to convert a
  code-read feature (TA-51, TA-52, TA-53, and every claim in this close-out about the refresh path)
  into a verified one. Design the test so the wrong answer is reachable: a fresh session, an
  uncorrected row (1400 or 1401, still wrong as of this close-out), a file the service has not
  already imported.
- **Repairing rows 1400/1401** — unblocked by T268, Kevin's SQL, not a migration.
- **T265** — the synthetic 21-column J1 fixture, still HIGH and open. ⚠️ Kevin's real export carries
  693 real employers' contact details and must never enter the repo.
- **`benefit-plans` and `pay-schedules`** — still in the Summit page catalog, still unexercised
  through `/SummitLink` (TA-49's unverified half, unchanged since S60's own close-out).
- **T261/T262** — two small display edits, confirmed still open (`💡 Backlog`), ship alone.
- **D-93** — the `SUMMIT_IMPORT_TEMPLATES` `enrollment:` entry, confirmed still unset anywhere; still
  blocks any real HRA Enrollment push.
- **T252** — opening the Summit setup panel to PSP user; the outer gate plus the fragment servlets,
  which should now include `/SummitRefresh` as a sixth (see Noted, not fixed) — this session's own
  new PSP-admin-only servlet was not added to that row.
- **V112 and V113** — per the tracker, confirmed **pending everywhere**, not applied to dev or any
  other environment (see Contradictions #9) — do not restate "applied to dev" going forward.
  Production's own position per the tracker's per-row markers is less settled than prior narrative
  text asserts; verify directly before relying on either claim.
- **Provenance columns on `enrollment_matrix_entry`** (TA-25) — not recoverable later, unrelated to
  S61, still open.
- **The exporter refuses on none of the three conditions the agent page flags** (TA-24/TA-26) —
  delegating `SummitExportServlet` to `MatrixCompletenessService` is the recorded fix, still
  unbuilt.
- **Nothing has been tested as a real agent** — still the largest unblocked item in the project, per
  prior close-outs, unchanged by this session.

## SQL close-out audit

**This session produced no SQL, no migration, and no schema change whatsoever.** Verified, not
asserted:

- `git show --stat` on all three commits (`6778881`, `9ec635c`, `ae2e196`) shows no `.sql` file in
  any of the three file lists (reproduced above).
- **No `constant` row was inserted.** D-102's `SUMMIT_REFRESH_ENABLED`, and every other new
  `ssa.properties` key from D-103–D-105, are deployment items describing configuration Kevin sets
  outside the repo — none is a migration, none is a seed `INSERT`, and no database was queried by
  any run this session.
- **Current highest migration in the tree: V113** (`ls docs/migrations | sort -V | tail -1` →
  `V113__enrollment_matrix_participant_agent_note.sql`), matching
  `docs/analysis/migration_tracker.md`'s own "Current Highest Version: V113" header. Unchanged by
  this session — no file under `docs/migrations/` was created, modified, or referenced by any of the
  three commits.
- **What is pending deployment, per the tracker, read directly rather than restated:** V105 through
  V113 all carry "not applied to any database" or "pending everywhere" language in their own row
  text, with no environment column marked applied for any of them. V104's own row marks its
  Production column unapplied as well — see Contradictions #9 for the tension this creates against
  other rows' narrative claims. This session neither resolves nor depends on that state; S61's work
  needed no migration, applied or pending.
- **Orphaned `.sql` files outside `docs/migrations/`** (repo-tracked, `git ls-files | grep '\.sql$'`):
  `docs/importscript/beta_ssa_baseline_v031.sql`, `docs/importscript/beta_ssa_dev_baseline_thru_V024.sql`
  (baseline DDL snapshots, self-described as such), `docs/schema_version_migration.sql`, and
  `docs/updates/update_V039_to_V057.sql` (noted, not investigated further, by S60's own close-out;
  unchanged this session). `release/V095__summit_plan_template_map.sql` and
  `release/V096__summit_file_export.sql` exist but are gitignored (`.gitignore:70`) — local
  deployment-staging copies, not a repo concern, unchanged this session.
- **Schema described but not scripted:** none found for S61's own work — D-102 through D-105 are
  explicit that they are `constant`-row and `ssa.properties` items, not schema, and TA-53 confirms
  `audit_run`'s existing columns hold every value this session writes without alteration.

**No SQL was produced, executed, or proposed by this session.**

## Addendum — first runtime-verified refresh (post-close-out)

A fifth commit landed after this close-out was written and pushed: **`b6f1864`** — "Summit refresh:
accept variable-width export timestamps (S61)". Verified via `git show --stat`: **2 files changed,
79 insertions(+), 22 deletions(-)**: `src/main/java/net/superiorstate/ams/data/service/SummitRefreshService.java`,
`docs/analysis/technical_assumptions.md`. Files TA-54.

⚠️ **This addendum supersedes one specific claim from "Open questions raised" above: "Nothing in the
refresh path has been runtime-tested."** That statement is no longer true. It is left in the body
unedited — the addendum's job is to say what has changed, not to make the original read as if it had
already known this.

**What happened, in sequence:**

1. Kevin created the Summit export template `ZZ_J1_Employer` and ran `/SummitRefresh` → Run now.
   Result: `STATUS_OK`, 867 ms, summary "No-op — no export matching prefix 'ZZ_J1_Employer' in
   /DataExchange/Superior State Administrators Inc/ExportFiles." **The file was in fact present.**
   The refusal's own wording was misleading: it said nothing had matched the prefix, when the prefix
   had matched fine — the timestamp filter was what rejected the file. A code-read could not have
   surfaced this; the first real tick did, in one click.
2. Cause, found by comparing the SFTP listing against the selector: the export's timestamp was 16
   digits (`ZZ_J1_Employer_Export_2026091410080821.CSV`) where the participant audit export's is 17,
   and the pattern in place at the time required exactly 17.
3. `b6f1864` widened the pattern to 14–17 digits, dropped ordering and the age check to second
   precision, and split the no-op message into distinct causes (no prefix match vs. prefix matched
   but unparseable vs. stale) so this specific failure shape is now named rather than folded into a
   generic "no-op." TA-54 filed.
4. Redeployed, Run now again. **It imported.** `STATUS_OK`, 8336 ms, summary: "Imported
   ZZ_J1_Employer_Export_2026091410080821.CSV (2026-09-14T10:08:08) — **5 inserted, 641 updated, 47
   unchanged, 0 errors**." `5 + 641 + 47 = 693` — exactly the row count of the export Kevin pulled by
   hand. The whole file was consumed; nothing was silently dropped.

**What these counts do and do not prove.** They prove the file was found, fetched, parsed, and fully
consumed with zero errors — the fetch/select/read/import pipeline works end to end against a real
export, for the first time. They do **not** prove much about *change*: `641 updated` is not evidence
that 641 employers actually changed — `SummitImportService.importEmployers` re-`merge`s any row whose
email/phone/contact is blank on every run regardless of whether anything differs (established S61-P5,
restated on the `/SummitRefresh` status page itself). `5 inserted` does mean something concrete: five
employers were new to AMS and did not exist before this import.

**Two consequences of this import that have not been verified, and are recorded here as unconfirmed
rather than assumed:**

- **Rows 1400 and 1401** should now hold the correct `employer_id`, since `importEmployers`'s update
  path overwrites `altId` from the file's `EmployerID` column under an `employerId > 0` guard
  (`SummitImportService.java:317`). If so, the pending repair this close-out's "Open questions" and
  "Next" sections both flag closes with no SQL at all. **Unconfirmed — no one has looked at those
  rows**, and this run did not query the database to check them, per its own scope fence.
- **Org 1402's `custom_id`** should now hold whatever the file carried (`S1387`, per Kevin's earlier
  manual pull) in place of the AMS-composed `158E136748`. That is T268 exactly as accepted — but the
  practical effect, not previously observed, is that `/SummitLink` will stop finding that employer by
  its composed key until a setup push writes the composed key back over it. **Unconfirmed.**
- The cheap way to test both without SQL: click a `/SummitLink` page for a legacy-created employer
  (to check 1400/1401's `altId`), and for org 1402 (to check whether `/SummitLink` still resolves it
  or now reports "not yet in Summit employer data").

**TA-51 remains read, not runtime-verified — do not upgrade it.** The import ran successfully, but
whether it actually wrote the *correct* `altId` into the previously-wrong rows (1400/1401 specifically)
is exactly the thing nobody has checked yet. A successful import proves the mechanism runs; it does
not by itself prove the mechanism repaired those two rows.

**TA-52 is confirmed in part, not in full.** The export now exists, lands in the export directory
under the `ZZ_J1_Employer` prefix, and the service found and imported it — the "inert until
configured" condition is discharged for the **manual** trigger. The **scheduled** path remains
completely unexercised: `SUMMIT_REFRESH_ENABLED` still does not exist as a `constant` row (unchanged
by this addendum, not queried, not inserted), so no scheduled tick has ever fired. Every observation
above came from clicking "Run now" twice. Keep that distinction sharp — TA-52's own entry has been
updated in place to say exactly this, not more.

**Revised Next**, replacing nothing above but narrowing what's actually still open:

- **The scheduled tick is still completely unexercised.** Everything observed so far is the manual
  trigger. Arming `SUMMIT_REFRESH_ENABLED` and watching a scheduled tick fire is still undone.
- **The two unconfirmed consequences above** — rows 1400/1401's `altId`, and org 1402's `custom_id` —
  are the cheapest possible next check: two `/SummitLink` clicks, no SQL.
- **The identity-skip guard (`ae2e196`) has never been observed.** Every tick so far has been a fresh
  import against a file not previously imported. A second Run now against the same
  `ZZ_J1_Employer_Export_2026091410080821.CSV` — unless a newer export has since landed — should now
  report `STATUS_SKIPPED`, "Skipped — … was already imported." One click confirms the whole `ae2e196`
  design that has, until now, only been reasoned about from code.
