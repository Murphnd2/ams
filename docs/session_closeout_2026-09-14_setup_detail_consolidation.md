# Session close-out — 2026-09-14 — Setup detail view consolidation (S59)

Branch `refactor/modernize-architecture`. Runs S59-P1 (Phase A, read-only), P2, P3 (hard-stop), P3rev,
P4, P5 (hard-stop), P5rev, P6 (hard-stop), P6rev, P7, P8, P9, P10, P11 (this close-out). Every repo
fact below was read from the tree at close-out time, not restated from the prompts.

## Shipped

Five commits, all on `refactor/modernize-architecture`, all pushed. **The build prompt that led into
this close-out named four; there are five** — `2efbe80` (S59-P6rev) and `e6079e2` (S59-P7) are two
separate commits, not one, confirmed by `git show --stat` on each.

- **`a9f027e`** — S59-P2 (Employer/Plans section titles become the Summit hyperlink when available,
  `SummitEmployerLinkServlet` `mode` param), S59-P3rev (`CardIssuerAvailabilityService`/`Servlet`, new),
  S59-P4 (per-section help toggles, `.collapse`), S59-P5rev (step-number badges removed; Contribution
  schedules and the card-issuer row promoted to top-level peers).
- **`2efbe80`** — S59-P6rev: Request census / Census / Demographics merged into one state-driven Census
  row (`CensusLifecycleService`/`Servlet`, new).
- **`e6079e2`** — S59-P7: the Census row's per-state control lists replaced with one fixed control
  sequence, envelope restored to every state (it had silently dropped from three).
- **`c18e753`** — S59-P8: `125 PI Elections`/`HRA Enrollment` merged onto one Enrollment line behind
  file-type choosers; Enrollment Matrix/Copy matrix link/Open agent view relocated in from
  `detailSetup25.jsp`; census row's redundant preview button removed.
- **`0489a0a`** — S59-P9 (the three relocated matrix controls become icon-only with `aria-label`; the
  card-issuer row moves above Enrollment) + S59-P10 (card-issuer row retitled `Card Issuance` with a
  peer title and separator; `Open agent view`'s wording no longer claims read-only).

**New files (four):** `data/service/CardIssuerAvailabilityService.java`,
`controller/market/CardIssuerAvailabilityServlet.java`, `data/service/CensusLifecycleService.java`,
`controller/market/CensusLifecycleServlet.java` — all verified present at those paths.

## In flight

**None.** Working tree is clean at close-out; every commit above is pushed to
`origin/refactor/modernize-architecture`.

## Decisions made

- **Step numbers removed rather than computed.** S59-P5's original plan (a computed counter) was
  superseded before it ran — Kevin's call: the badges were load-bearing for nothing, since order is
  carried by document position. S59-P5rev deleted them outright.
- **PSP admin only, for now.** The three relocated matrix controls narrow from
  `isPspAdmin or isPspUser` to admin-only on arrival into the panel (S59-P8). Opening the whole panel
  to PSP user is deferred, filed as **T252**.
- **Revoke stays on the compose page (`censusRequest25.jsp`), not a one-click row control.** S59-P6
  correctly hard-stopped on finding no relocatable Revoke control existed in the merged-row scope;
  Kevin's decision (S59-P6rev) was that the compose page's context — what was sent, when it expires —
  is the right confirm surface, not a bare button.
- **The file-type chooser does not pre-filter.** Which of `125 PI Elections`/`HRA Enrollment` actually
  applies stays inside `SummitExportServlet`, unmodified and not duplicated; the chooser offers both
  unconditionally and relies on the exporter's existing refusals. Filed as **T253**.
- **`Contribution schedules` stays in the panel.** Kevin's idea to move it into a plan-setup checklist
  task instead is carried as an idea, not a spec — filed as **T257**, not built.
- **The census row's preview button was removed as redundant** — `/CensusUpload` already lands on a
  page showing the roster, so a separate disabled preview duplicated what that click already gives.
  Scoped to the census row only; the placeholder stays on Employer, Plans, the card-issuer row, and the
  Enrollment line (S59-P8).

## New assumptions

Eighteen `TA-27`–`TA-44` entries filed in `docs/analysis/technical_assumptions.md` this run (see that
file for the full text — assumption, basis, design choice, risk if wrong, reversal cost, per entry):
`TA-27`, `TA-28` (S59-P2); `TA-29`, `TA-30`, `TA-31` (S59-P3rev); `TA-32` (S59-P4); `TA-33`, `TA-34`,
`TA-35` (S59-P6rev); `TA-36` (S59-P6rev/P7, corrected to eight states); `TA-37`, `TA-38` (S59-P7);
`TA-39`, `TA-40`, `TA-41` (S59-P8); `TA-42` (S59-P9); `TA-43` (S59-P10); `TA-44` (S59-P5rev, reconciled
this run — its original hierarchy-by-indentation claim is now stale since S59-P8 removed the last
indented rows; the panel is flat).

## Open questions raised

- **The `REVOKED`-request-with-a-stray-`PENDING`-submission precedence case was chosen, not settled.**
  `CensusLifecycleService`'s precedence puts `AWAITING_REVIEW` ahead of `REVOKED`, so that specific
  combination resolves to "awaiting review" rather than "revoked" — a reasonable default, not a
  validated one; no real data was checked to confirm this combination is even reachable in practice
  (see `TA-33`).
- **Which three lines constitute "the census lines"** was ambiguous at Phase A (S59-P1) — only two rows
  were ever titled with "census" in them; S59-P6rev/P7 resolved this by merging all three
  (Request census / Census / Demographics) into one row, but the naming ambiguity in the original
  five-item plan was never explicitly reconciled on the record until now.
- **Whether Employer's third muted line** (the S59-P2 `mode=status` identity-confirmation line,
  distinct from the description and the `/SummitSetupStatus` include) belongs inside its help toggle
  was a judgment call made in S59-P4, not a settled instruction — flagged there, still open.

## Contradictions found

Several, on the record:

- **Phase A (S59-P1) placed the panel's sections in `detailSetup25.jsp`; they are in
  `detailSummitSetup25.jsp`.** `detailSetup25.jsp` only ever held the `<c:import>` that pulls the panel
  in, plus (until S59-P8) three relocated controls below it.
- **`demographicsSettled` is in `CensusIntakeService.java:283`, not `SummitExportServlet:283`.** Same
  line number, wrong file — the citation that reached S59-P6/P6rev's prompts was stale.
- **`CLAUDE.md` says the latest migration is V073** (`:53`, `:132`); `docs/migrations/` holds through
  **V113**. `CLAUDE.md` names itself a snapshot and tells the reader to check `ls docs/migrations/`
  directly, so this is expected drift, not a defect — noted per this run's instruction to record it.
- **`project_backlog.md`'s T188 cites stale line references and a `max-height: 120px`** that is now
  `320px` (`detailSetup25.jsp:31`, not `:28`) — confirmed by reading the current file; T188 itself is
  unmodified by this run.
- **Revoke and Renew were specified in S59-P6's prompt as existing row-level controls; they exist only
  on the compose page (`censusRequest25.jsp`), reached via the single envelope link.** S59-P6 caught
  this and correctly hard-stopped rather than inventing controls or silently dropping the requirement.
- **Three prompt defects were caught by hard stops before anything was written:**
  1. **S59-P3** — the prompt asked `CardIssuerAvailabilityService` to distinguish
     `SummitPlanTemplateResolver`'s table-vs-property source, which the resolver's own javadoc
     declares callers "cannot tell... and must not learn." Stopped; S59-P3rev redesigned the guard
     around a row-level question instead.
  2. **S59-P6** — Revoke/Renew specified as existing controls, above.
  3. **S59-P9** — its own baseline instruction ("Baseline: the commit holding S59-P8") was
     self-contradictory with reality: S59-P8 was still uncommitted, so no such commit existed.
     Stopped and reported rather than guessing which state to treat as authoritative; S59-P8 was
     committed on request before S59-P9 ran.

  All three cost one round trip each, not a bad commit.

## SQL close-out audit

**This session produced no SQL, no migration, and no schema change whatsoever.** Every one of the five
commits above is JSP/Java only (four new servlet/service pairs, JSP restructuring); nothing in this
session touched `docs/migrations/`, `docs/schema_version_migration.sql`, or any database.

**Migration position**, verified against `docs/analysis/migration_tracker.md` and `ls docs/migrations/`
at close-out:

- **Highest migration in the tree: V113** (`docs/migrations/V113__enrollment_matrix_participant_agent_note.sql`),
  confirmed by both the tracker's own "Current Highest Version: V113" header and a direct directory
  listing.
- **V112 and V113 still need applying to dev** — both explicitly marked "not applied to any database"
  in the tracker (S58-P3/P7, 2026-09-13); this session did not change that.
- **Production is at V104**, with **V105–V113 pending** — per the tracker's own explicit statement on
  V108's entry ("Production is at V104: V105, V106, V107 and V108 are all pending deployment
  together"), extended through V113 since nothing after V104 has an applied marker anywhere in the
  tracker.
- **Next release**, following this project's `v0.{highest migration}.{patch}` naming convention (e.g.
  `v0.96.00` for V096, `v0.94.00` for V094) would be **`v0.113.PP`** — this is the convention applied,
  not a statement found verbatim anywhere in the docs; no explicit "next release version" line exists
  in `claude_memory.md` or `deployment_strategy.md` as of this close-out.

## Verification notes (facts checked against the tree, not copied from the build prompts)

- **Commit hashes:** all five confirmed via `git log --oneline` and `git show --stat` — see "Shipped"
  above for the one correction (five commits, not four; `2efbe80`/`e6079e2` are separate).
- **Four new file paths:** confirmed present via directory listing.
- **Current row order:** confirmed by reading `detailSummitSetup25.jsp` — Employer · Plans (CDH) ·
  Contribution schedules · Census · Card Issuance · Enrollment.
- **`summitHelp-*` id count:** six (`employer`, `cdhplan`, `contribsched`, `census`, `cardseed`,
  `enrollment`), each appearing exactly once, confirmed by grep.
- **`padding-left: 2rem` count:** zero — confirmed; both former indented sub-rows (`125 PI Elections`,
  `HRA Enrollment`) were consumed into the Enrollment line by S59-P8.
- **Highest migration:** V113, confirmed above.
- **Every line number cited in the contradictions list above:** re-checked against the current tree at
  close-out time (`detailSetup25.jsp:31` for the `max-height` value, `CensusIntakeService.java:283`
  for `demographicsSettled`, `CLAUDE.md:53`/`:132` for the V073 claim).

## Next

**Recommended next step: item 5's remaining scope (the `125 PI Elections`/`HRA Enrollment` consolidation
is done; T253 — the exporter delegating to a shared applicability service — is the natural follow-on
if the chooser's unfiltered offering proves confusing in practice), or T252 (opening the panel to PSP
user), whichever Kevin prioritizes.** Neither is blocked — both are scoped, filed, and ready to pick up
without further Phase A work, since this close-out's verification pass confirmed the current tree state
they'd build against. **Blocked, not recommended next:** anything touching `SummitExportServlet`
directly remains gated on Kevin's own review of that file, per this session's repeated fencing of it.
