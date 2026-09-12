# Session 51 Close-out — 2026-09-12

## Scope

Three runs, all documentation: (1) a read-only audit separating what the repo documents, implements
and proves about the `125 PI Elections` path; (2) `docs/analysis/hra_enrollment_test_plan.md`, the
hand-upload checklist Kevin worked from; (3) this reconciliation run — a new working reference for
all six Summit import templates, targeted corrections to five docs, two register entries, two backlog
rows, and this close-out. No `.java`, `.jsp`, `.sql`, `pom.xml` or config file was touched. No git
mutation ran (no `add`/`commit`/`stash`/`checkout`/`restore`/`reset`, no tags) — the working tree
is left dirty for Kevin.

## Shipped (with hashes)

Nothing committed this session. HEAD is still `8a539bd` (S50, "Import Plan ID must be strictly
alphanumeric"). All session-51 work is uncommitted in the working tree:

- **New:** `docs/analysis/summit_import_templates_reference.md` (306 lines, written verbatim from
  Kevin's 2026-09-12 picker reads and test results) · `docs/analysis/hra_enrollment_test_plan.md`
  (252 lines, run 2) · this file.
- **Edited:** `docs/analysis/summit_import_spec.md`, `docs/analysis/summit_import_contracts.md`,
  `docs/business/summit_data_exchange.md`, `docs/analysis/project_backlog.md`,
  `docs/analysis/legal_assumptions.md` (cross-reference only).

## In flight

- Kevin's hand-upload sequence against `ZZSDX27A`: Tests 1–3 of the HRA Enrollment plan ran
  (Single Fund by import, ICHRA pre-plan-year date, Contribution Schedule with tier and blank
  schedules). Test 2 (card timing) is **untestable until SSA issues cards** — reclassified, TA-e /
  T245. Tests 4–6 parked as T246.
- No emitter work is in flight. `125 PI Elections` still has no emitter; `writeHraEnrollment`
  still targets the five-column template-1030 layout, not `ZZ_TEST_HRA_ENROLL`'s eight.

## Decisions made

- **ICHRA and QSEHRA will be Contribution Schedule funded with monthly stipends in almost all
  cases. Single Fund is not the ICHRA standard.** Recorded in `summit_import_spec.md` §6 and §9 #11,
  `summit_import_contracts.md` (recommendation struck), `summit_data_exchange.md` (after TA-e).
  Consequences: AMS does emit `Tier ID`; how AMS learns a plan's employer funding method stays a
  live design question. Do not relitigate.
- **§9 #13 closed without a new LA entry** — LA-36 already covers the month-ahead cadence; spec §7
  concerns and LA-36 now cross-reference each other.

## New assumptions with reversal cost

- **TA-e — card issuance effective dating** (`summit_data_exchange.md`, after TA-d; the highest
  existing entry was TA-d — TA-a/b/c do not appear anywhere in the repo). Card issued on the
  effective date, provided coverage is prospective; AMS emits a calculated next-day effective date
  for card-seed enrollments. **Reversal cost: low** — one date calculation in one emitter. Status
  Assumed; not implied by evidence (funds are live at record creation — `-15`). Sub-questions
  (CST vs UTC clock, batch cut-off, weekends/holidays, one day of lead) recorded, non-blocking.

## Open questions and who settles them

| Question | Settles it |
|---|---|
| Card production trigger (§9 #1) | Test, when issuance is live — T245 |
| Multi-tier Single Fund tier resolution (§9 #3), Pro-Rate on effective date (§9 #8), `Default Value` on a blank field (§9 #5) | Test — parked, T246 (need plan config that does not exist; lower value after the funding decision) |
| `Plan Status` code set on Elections (§9 #4) | DataPath — no candidate codes recorded anywhere |
| What `Import for Process Approval` does when checked (§9 #6, residual) | Test / DataPath |
| The separate `Enrollment` file type (§9 #7) | DataPath |
| `Undo Last Change` scope (§9 #10) | Test |
| Effective-date policy for elections (§9 #12) | **Kevin** |
| Whether LA-36's *Assumed* status suffices before plan documents are drafted (§9 #13 residual) | Counsel, on LA-36's own "Confirm before" terms |
| How AMS learns a plan's employer funding method (spec §4) — now unavoidable, since Tier ID is emitted | **Kevin** (design) |
| Whether F/G schedule columns are required on HRA Enrollment where the employer has no default schedule | Test |
| Whether the amount column funds anything on a non-tiered employer plan (the template-1030 layout the shipped emitter uses) | Test |

## Contradictions found

Found in run 1 and corrected in run 3 unless noted:

1. `125 PI Elections` "never imported / field set unproven" — `summit_data_exchange.md` §`125 PI
   Elections`, SDX-12, SDX-13, files 6/7 notes; backlog T195/T229/T232 — vs spec §2 and the
   contracts record (proven 2026-09-11/12). **Corrected** (C3, C4): marked superseded, SDX-12/13
   resolved, backlog rows updated. No emitter claimed.
2. Spec §7 / §9 #13 asked for a new `LA-NN` while LA-36 already existed. **Corrected** (C5).
3. Results-line shape documented as one shape for all types (spec §1) vs observed three- and
   four-field shapes; `SummitResponseService.check` classifies on `fields[0]`. **Doc corrected**
   (C10); **code not changed** — implication recorded in spec §1.
4. HRA Enrollment amount column "carries the benefit amount" (`summit_data_exchange.md` §4) vs
   `-15` showing Annual Election $0.00 / Employer Funding $6,000 from the tier. **Corrected** (C1),
   scoped to tiered employer plans; the non-tiered case stays untested.
5. "Empty roster emits a zero-row file" (`summit_data_exchange.md`) vs the T209 refusal in code.
   **Corrected** (C2).
6. Import Plan ID written `{employerTpaCustomId}-{keySegment}` in prose while the code concatenates
   with no separator. **Corrected** at the three live-claim sites in `summit_data_exchange.md`
   and T185 in the backlog (C6); statements that the hyphenated form is *unusable* or *superseded*
   were left as they are correct.
7. Spec §7 step 3's ICHRA leg at 12/1/2026 — "untested, `Plan Not Found` plausible" — vs proven to
   fail. **Corrected** (C7), with the note that a wrong Import Plan ID returns the same string.
8. **New, found while editing:** spec §3 (session 50) recorded `Employer Contribution Amount` as
   "offered but unmapped" on `125 PI Contributions`; Kevin's 2026-09-12 picker read found no such
   element. Both are picker observations a day apart. Spec §3 marked superseded by the newer read
   (C9); if the element reappears, the template was changed between reads.
9. **New, found while editing:** the shipped `writeHraEnrollment` (five columns, amount, no Tier
   ID, no Filler) and the eight-column `ZZ_TEST_HRA_ENROLL` layout are both "import-proven" —
   against different templates on different dates. Recorded in the reference §6 and
   `summit_data_exchange.md` §4; **not reconciled** — which template the tenant will hold for
   production is a decision, not a doc fix.
10. **Not corrected, out of scope:** three session close-outs (S30, S31, S50) carry the hyphenated
    Import Plan ID prose. Historical records; left alone.

## SQL close-out audit

- **SQL statements produced, run or recommended this session: none.** No `.sql` file created,
  edited or proposed; no DDL, DML or schema described in prose that would need scripting. The
  session-51 findings are Summit-side facts and doc corrections; the only schema-shaped items
  raised (a per-participant election amount, a plan-family marker, a funding-method column) are
  recorded as *gaps* in the run-1 audit and T195, not designed and not scripted.
- **Highest migration on disk:** `V102__employer_custom_id.sql`. **Tracker header:** V102
  (`docs/analysis/migration_tracker.md:19`). **Match: yes.** `docs/schema_version_migration.sql`
  also ends at V102.
- **Pending deployment:** none — the tracker records V097–V102 applied everywhere as of session 50.
- **Orphaned `.sql` file, reported not fixed:** `docs/migrations/seed_ndt125_questionnaire.sql` is
  not a `V`-file and is registered in neither `migration_tracker.md` nor
  `schema_version_migration.sql`. Untouched this session.
- **Schema described but not scripted:** none newly. (The three gaps above are prerequisites for an
  Elections emitter and are deliberately undesigned.)

## Next

1. Kevin commits the session-51 docs (nothing else is dirty except `.idea/`).
2. Decide the design question the funding decision forces: how a `summit_plan_template_map` row (or
   its operator) tells the HRA Enrollment emitter the plan's employer funding method and tier —
   the shipped emitter emits neither Tier ID nor Filler and targets a layout the tenant's
   `ZZ_TEST_HRA_ENROLL` template does not have.
3. Decide §9 #12 (effective-date policy for elections) — the last Kevin-owned item gating a
   `125 PI Elections` emitter, alongside the data gaps in T195.
4. `SummitResponseService.check`: fix the first-field classification before relying on it for any
   participant-keyed template (Demographics, Elections, HRA Enrollment). Code change; not this run.

## Compliance statement

- Files created: 2 — `docs/analysis/summit_import_templates_reference.md`, this close-out.
- Files modified: 5 — `docs/analysis/summit_import_spec.md`, `docs/analysis/summit_import_contracts.md`,
  `docs/business/summit_data_exchange.md`, `docs/analysis/project_backlog.md`,
  `docs/analysis/legal_assumptions.md` (one cross-reference paragraph appended to LA-36; substance
  untouched). All docs.
- SQL produced: none. Java/JSP/config touched: none.
- Git mutations run: none. Read-only: `git status --short`, `git log --oneline`.
