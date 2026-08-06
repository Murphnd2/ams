# Session 20 close-out — S20-A (Phase A spec) + S20-B (build 1)

Date: 2026-08-06. Branch: `refactor/modernize-architecture`.

---

## 1. Shipped

**S20-A (spec, Opus):** `docs/analysis/S20A_ichra_sections_spec.md`, commit `db53855`. One file
created, nothing edited, no T-number assigned, no `LA-NN` number assigned.

**S20-B (build, Sonnet):** executes S20A §8, build item 1 (`ICHRA_MARKET`, the structural
foundation for all four sections). Three commits:

- `1ea852e` — source + migration. `docs/migrations/V091__ichra_section_selection.sql` (new);
  `Enhancement.java`, `ProposalIchraIntake.java`, `ProposalIchraSnapshotBand.java`,
  `FlaggedEnhancementResolver.java`, `ViewProposal.java`, `ProposalBuilder.java`,
  `ServiceManagerAction.java`, `proposalBuilder.jsp`, `serviceManager25.jsp`.
- `299547a` — `docs/analysis/migration_tracker.md` + `docs/schema_version_migration.sql`, V091
  registered, Current Highest Version bumped.
- `56a4c2e` — `docs/analysis/project_backlog.md`: T163 corrected (shipped in S19-A, `3baf60f`,
  never flipped from 📋 Planned), T168 filed for this build.

Pushed clean, `db53855..56a4c2e`, not rejected.

**T-number assigned:** **T168** — "The four ICHRA proposal sections — build 1 (`ICHRA_MARKET`,
the structural foundation)". Highest existing was T167; confirmed by grepping every `T1XX` row
before assigning.

**Migration:** `V091__ichra_section_selection.sql`. V090 confirmed highest in
`docs/migrations/` before naming it (both at S20-A spec time and again independently at S20-B
build time). Applied to **local dev (`beta_ssa`, work) only** — verified live, see §7.

---

## 2. In flight / not built this session

- **Build 2 (`ICHRA_CONTRIBUTION`)** and **Build 3 (`ICHRA_COMPARISON`)** — no migration needed
  (V091 already carries every column both will use), a token/section-config addition each. Not
  yet filed as T-numbers.
- **Build 4 (`ICHRA_AFFORDABILITY`)** — **blocked**, pending an `LA-NN` legal_assumptions.md
  entry. S20-A §7 drafted the full entry text; it was not filed by either run. This build's form
  renders no control that could select it (§8.6 of the spec, honored: no `sectionAffordability`
  checkbox exists in `proposalBuilder.jsp`), and the resolver predicate withholds it correctly
  with no section-specific code of its own.
- **`ProposalDetail` (T164)** — still unbuilt. The `missing` array in the payload's `sections`
  block exists for its future PSP-admin preview use; nothing reads it yet.

---

## 3. Decisions made

1. **Membership widening approved and shipped, per §2A of this run's prompt.** `ViewProposal`'s
   `proposalEnhIds` set gains a second source:
   `FlaggedEnhancementResolver.systemManagedIdsForProposal(em, proposal)`, added right after the
   existing `RateTable`-derived loop. **This contradicts T165's own stated promise**
   (`project_backlog.md` T165 row: *"the resolver's signature already accepts `em` and
   `proposal`... specifically so this build never requires a second edit to
   `ViewProposal.java`"*) — that promise held for the predicate (`isSectionEnabled`) and did not
   hold for membership. One line was required. A `system_managed` enhancement by design carries
   no priced `RateTable` line (`serviceManager25.jsp`'s own help text: *"Has no effect on the
   application"*), so it never entered `proposalEnhIds` via the existing loop and its section was
   structurally unreachable regardless of what the predicate answered — S20-A §1.4b found this,
   S20-B built the fix.
2. **Affordability's display posture for build 4, per §2B.** Not built this session by
   instruction. When build 4 ships, it will render the contribution ceiling and its inputs and
   state no verdict — no "affordable"/"not affordable" label on a public page — preserving LA-12's
   intent. The `LA-NN` entry is filed against a real design at that time, not now.
3. **T163 correction is a status-only fix.** No code changed; `ServiceManagerAction.java:207` and
   `serviceManager25.jsp:944-951` already existed from S19-A (`3baf60f`) before this session
   started. Confirmed by reading both files directly this run, not merely trusting S20-A's §1.9
   finding.
4. **RANGE mode now parses `contribution`/`currentTotalPremium`/`currentEmployerShare`** in
   `attachRangeSnapshot` — previously it read only `headcount`. This was necessary for the
   `sections` block's completeness computation to be correct for RANGE-mode proposals (spec §2:
   *"Age bands are a fidelity upgrade for #2 and #3, never a gate"* — RANGE mode must be able to
   satisfy #2/#3 completeness without bands). **No structured column was added for RANGE mode's
   `contribution`** (`snapshot.setContribution(...)` is still AGE_BAND-only) — this was a
   deliberate scope-minimization call this run made, not dictated verbatim by either prompt: the
   payload's `sections` block only needs the raw request value, not a second write path into an
   existing column whose semantics (net-of-contribution structured figures) don't apply to RANGE
   mode's own columns. Named here as a design call, not papered over.
5. **A shared `resolveSectionSelections`/`IchraSectionSelections` record** (Java 17 record, one
   new private helper in `ProposalBuilder`) derives the four booleans in exactly one place, used
   by both the intake write (`attachIchraIntakeIfPresent`) and the payload's `sections` block
   (`buildSectionsBlock`), so the two can never independently drift on what "selected" means. Not
   explicitly specified by either prompt; added because the alternative was duplicating the same
   four-line derivation twice in one class.
6. **JS required-ness is not native HTML `required`.** Every existing field in this intake panel
   (`intakeZip`, `intakeHeadcount`, etc.) is gated through `ichraIntakeComplete()` and
   `btnCreate.disabled`, not the `required` HTML attribute — this build's new fields
   (`intakeCurrentTotalPremium`, `intakeCurrentEmployerShare`) follow the same convention rather
   than introducing native `required` on fields that are sometimes `display:none` (a known
   browser-validation trap: some browsers refuse submission on a hidden required field with no
   visible error).

---

## 4. New assumptions, and reversal cost

| Assumption | Reversal cost |
|---|---|
| RANGE mode's `contribution`/comparison figures are parsed for payload completeness only, never written to a structured `ProposalIchraSnapshot` column | Low — adding `snapshot.setContribution(...)` to `attachRangeSnapshot` later is additive, no schema change |
| `resolveSectionSelections` / `IchraSectionSelections` record is the single source of truth for the four booleans | Low — a private refactor confined to `ProposalBuilder`, no external contract |
| The `sections` block's `ICHRA_AFFORDABILITY.missing` array is best-effort (bands/contribution/basis/income checked without duplicating `buildAffordabilityBlock`'s exact FPL-constant lookup) since the section is never selectable this build | Low — section 4 is unreachable regardless (no checkbox exists to select it), so imprecision here has no render-path consequence until build 4, which will need its own review anyway |
| No native HTML `required` attribute on any new form field; all required-ness enforced by `ichraIntakeComplete()`/JS | Low — matches every existing field in this panel; adding native `required` later is additive but would need the hidden-field trap checked first |
| Admin dropdown's `ICHRA_AFFORDABILITY` option is selectable now even though build 4 is blocked | Low — an admin classifying an enhancement as `ICHRA_AFFORDABILITY` today has no effect (no section ever gets configured for it, and `secAffordability` is server-hardcoded `false`); harmless to leave available rather than special-casing the dropdown |

---

## 5. Open questions, and who settles them

1. **The `LA-NN` affordability entry** (S20-A §7 draft). Kevin/counsel. Blocks build 4 alone.
2. ⚠️ **Carried forward from S20-A §9.3 item 2, still unresolved, still the weakest link:** the
   "four sections carry no `proposalsectionlos` rows" rule is enforced by convention and by the
   verification walk documented in the spec — **not by code.** A future admin who associates a
   LOS with one of these `CUSTOM` sections silently disables the resolver gate for that section
   (§1.4a of the spec: the LOS branch in `ViewProposal`'s `SCOPED` filter matches first and never
   calls the resolver), with no error anywhere. This build did not add a guard against it —
   §9.3 item 2 of the spec named this as optional-but-strictly-better; it was not in this build's
   scope fence (§5 of this run's prompt lists exactly which files may be edited, and the
   `ProposalSettings`/section-admin surface that would need the guard is not among them).
3. **Whether `ProposalDetail` (T164) renders the `missing` array for a PSP admin.** Not this
   build's or this spec's call.
4. **Whether any real `rating_area_rate_cache` row has a populated `onexLcspPremium` anywhere.**
   Carried forward unverified from S19-D §8 item 5 and S20-A §9.3 item 4. No database query this
   run touched that column.
5. **Builds 2/3's own T-numbers** — not filed. Recommend filing before either is built, per the
   S16-G standing rule (a finding/build lives in `project_backlog.md` or it does not exist).

---

## 6. Contradictions found

1. **T165's stated promise did not hold — confirmed and fixed, not merely inherited from the
   spec.** Section 3 (§1.4b of `docs/analysis/S20A_ichra_sections_spec.md`) named this; this
   build independently re-confirmed it by reading `SalesDAO.getPricing`'s two queries directly
   before writing the fix, rather than trusting the spec's citation alone.
2. **`project_backlog.md` T163 was stale, exactly as the spec's §1.9 found**, and this build
   verified the claim itself before writing the correction: `ServiceManagerAction.java:207` and
   `serviceManager25.jsp:944-951` both exist and match T163's own "Fix shape, if taken" almost
   verbatim (the same action pattern, the same JSP row, one boolean control — this build's own
   discriminator dropdown sits directly beside it).
3. **No contradiction found in the spec's build-ready section (§8) itself.** Every file path,
   line-number anchor (re-verified semantically per this run's own §6 instruction, since S20-A's
   line numbers were from `db53855` and had already drifted after each successive edit in this
   same session), and method signature named in §8 matched what this run found on the ground,
   with two narrow additions this run made and disclosed above (§3 items 4-6) rather than left
   silent.

No other contradictions found. Every anchor named in the spec — `ViewProposal.java:277-282`
(before this run's edits), `FlaggedEnhancementResolver.java:56`, `Enhancement.java:29-30`,
`ProposalIchraSnapshotBand.java:33-37`, `ServiceManagerAction.java:201-210`,
`serviceManager25.jsp:944-951` — was located, printed, and confirmed unique before editing, per
this run's own anchor-verification instruction (§6).

---

## 7. SQL close-out audit

- **Every SQL statement produced:** `docs/migrations/V091__ichra_section_selection.sql`, full
  text matching `docs/analysis/S20A_ichra_sections_spec.md` §6 verbatim except the version
  confirmation and the build date (`2026-08-06`, substituted for the spec's `<build date>`
  placeholder).
- **In a versioned migration:** yes — `docs/migrations/V091__ichra_section_selection.sql`,
  registered in both `docs/analysis/migration_tracker.md` and `docs/schema_version_migration.sql`
  in this same session.
- **Orphaned `.sql` files:** none. V091 is registered in both places, nothing left dangling.
- **Current highest migration version, read from `docs/migrations/`:** confirmed **V090** before
  naming V091 (both before writing the file and again, independently, in the pre-apply gap check
  below), now **V091** after this session.
- **Applied and verified — local dev (`beta_ssa`, work workstation) only:**
  - Pre-apply gap check (`SELECT version FROM schema_version WHERE version = 'V091'`,
    `SHOW COLUMNS ... LIKE 'system_section_key'`, `SHOW COLUMNS ... LIKE 'section_%'`,
    `SHOW COLUMNS ... WHERE Field IN ('net_per_employee','band_net')`) confirmed nothing existed
    yet, and confirmed the band columns were still `NOT NULL` (`Null = NO`) beforehand.
  - Applied via `mysql.exe < V091__ichra_section_selection.sql` (full path,
    `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe` — `mysql.exe` alone is not on PATH on
    this workstation, confirmed by `where mysql.exe` failing before locating the binary), exit 0.
  - Post-apply verification: `schema_version` and `schema_info` both read `V091`;
    `enhancement.system_section_key` is `varchar(32)` nullable; all four
    `proposal_ichra_intake.section_*` columns are `tinyint(1) NOT NULL DEFAULT 0`; both
    `current_*` columns are `decimal(10,2)` nullable; `proposal_ichra_snapshot_band.net_per_employee`/
    `band_net` both flipped from `Null = NO` to `Null = YES`. Existing row counts unaffected
    (18 `enhancement` rows, 2 `proposal_ichra_intake` rows, both unchanged pre/post).
  - `MYSQL_PWD` was extracted inline from `C:\ssa\ssa.properties` (`local.db.password`) for each
    invocation and unset immediately after — no password printed anywhere in this transcript,
    same procedure S19-C/S19-E established.
- **What is pending deployment:** **production has not received V091.** Production remains at
  **V090** as of this session's close. Local dev (`beta_ssa`, work) is the only environment at
  V091.

---

## 8. Code-verified vs. runtime-verified disclosure

**This session compiled and applied a migration; it did not exercise the application at
runtime.** What was actually done:

- **Code-verified:** every file edit, anchored against the live working tree before editing
  (§6 of this run's prompt — anchors printed, confirmed unique, hard-stop-on-absent was the
  standing rule though never triggered).
- **Build-verified:** `.\mvnw.cmd package` — `BUILD SUCCESS`, 510 source files compiled, WAR
  packaged. This proves the Java compiles and the JSPs are syntactically well-formed enough to
  bundle (Tomcat compiles JSPs at deploy time, not Maven at build time — a JSP script-block syntax
  error would not necessarily surface here).
- **Database-verified (local dev only):** the migration's DDL was applied and every resulting
  column/constraint independently queried and confirmed, per §7 above.
- **NOT runtime-verified, by anyone, this session:** no Tomcat deployment, no HTTP request, no
  browser page load, no proposal ever built through this code path, no enhancement ever flagged
  through the Service Manager UI, no `/proposal/*` page ever rendered a `system_managed` section.
  **The ten-step verification walk in `docs/analysis/S20A_ichra_sections_spec.md` §7 build 1
  (referenced from spec §8.8) is Kevin's** — this session performed none of its steps. In
  particular, step 6 (*"the section renders — this is the first time a `system_managed`
  enhancement's section has ever been visible"*) is the actual proof this build works, and it has
  not happened yet.

---

## 9. Recommended next step

Walk `docs/analysis/S20A_ichra_sections_spec.md` §7 build 1's ten verification steps against
local dev, in order. Step 3 requires manually creating a `CUSTOM` `ProposalSection` with
`scope = SCOPED`, associated to the flagged enhancement **only** — zero `proposalsectionlos`
rows, per §1.4a of the spec and the open weakness recorded in §5 item 2 above. Step 9 (the
regression check against an existing non-ICHRA proposal with LOS-scoped `CUSTOM` sections) is the
one that must not move.

Before building 2 or 3, file their T-numbers. Before building 4, get an answer on the `LA-NN`
question — the draft text is sitting in `docs/analysis/S20A_ichra_sections_spec.md` §7, unfiled.
