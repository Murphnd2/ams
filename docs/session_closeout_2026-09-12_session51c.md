# Session 51c Close-out — 2026-09-12

## Scope

The build half of session 51: W1 (a Summit behaviour test), W2–W5 (code + one migration), then this
documentation pass recording what was proven, correcting what the repo still got wrong, and adding a
PremiumPath structure doc. This pass touched docs only; no `.java`, `.jsp`, `.sql` or config; no git
mutation — tree left dirty.

## Shipped (with hashes) — all pushed to `origin/refactor/modernize-architecture`

| | Commit | What | Verified how |
|---|---|---|---|
| W1 | — (no code) | Summit behaviour test: CDH plan effective `10/01/2025` vs plan year 1/1/2026–12/31/2026 | Stored **verbatim, uncoerced** (Edit Benefit Plan screen) |
| W2 | `9963fd9` | Employer Demographic contact columns L–N, gated by `SUMMIT_EMPLOYER_OPTIONAL_ELEMENTS` (`SummitEmployerElementResolver`) | Export diff 11 → 14 fields, first 11 byte-identical; imported; employer `158E141452` shows Title/Name/Email |
| W3 | `4d43dba` | V103 — `seq`, `effective_date_rule`, `offset_months`, `plan_year_offset_years`; unique key → (psp, service item, seq) | Applied to `beta_ssa`; `information_schema.STATISTICS` shows only `uq_summit_plan_template_map_psp_service_seq` (3 cols) |
| W4 | `5982c26` | File 2 fan-out — one CDH row per active mapping row, per-row E/G/H via `SummitPlanDateRuleResolver` | Single-row export byte-identical; six-row fan-out imported, six plans created on the right dates |
| W5 | `a889263` | Admin: several rows per service item, four fields exposed, V1–V3 validations | Five PremiumPath rows entered through the UI |

Doc commits earlier the same day: `df1b3a9` (51), `f7a18bc` (51b). This pass's docs are uncommitted.

## In flight

- V103 is applied to local `beta_ssa` only — **pending on dev_ssa and production**. W4/W5 code is
  on trunk and will run against a pre-V103 schema until it is applied (the entity maps four columns
  that would not exist).
- The HRA Enrollment emitter (`writeHraEnrollment`) still targets the 5-column template-1030 layout;
  the tenant's `ZZ_TEST_HRA_ENROLL` is 8 columns with Tier ID. Not touched this session; blocked on the
  funding-method design (spec §4).

## Decisions made

- ICHRA/QSEHRA are **Contribution Schedule** funded; Single Fund is not the standard (recorded 51).
- Column H stays **answer-derived and shifted** by `plan_year_offset_years`, not recomputed (F8) — the
  byte-identical bar for a short first plan year decided it.
- `seq`, not `sequence`, as the column name (MariaDB reserved word).
- V2/V3 admin uniqueness checks are PSP-wide, inactive rows included, case-insensitive, and fire on
  edit only when the offending field changes — no data migration, no auto-fix.

## New assumptions with reversal cost

- None new this pass. TA-e (card issuance on the effective date; next-day rule) stands from 51,
  reversal cost low, still untestable until SSA issues cards (T245).

## Findings recorded (F7–F12)

| # | Finding | Recorded at |
|---|---|---|
| F7 | V103's first index swap was drop-then-add; `psp_id`'s FK blocked the drop and both keys survived. **General rule: ADD the replacement key before DROPping the original on an FK column.** | `migration_tracker.md` "Going Forward" rule 5 (general), V103 row, V103 script header + Reversal block |
| F8 | Column H comes from the `plan_year_end` answer, shifted — never from `planYearBegin` | `summit_import_templates_reference.md` §2 W4 bullet; `summit_data_exchange.md` §2; PremiumPath doc |
| F9 | The date rules as built (plan-year shift; `PLAN_YEAR_START` ignores `offset_months`; `MOST_RECENT_PAST_MONTHDAY` strictly past; Feb 29 → Feb 28; `today` once per export; out-of-year E intended) | reference §2; data_exchange §2; spec §7; PremiumPath doc |
| F10 | Verified six-plan fan-out output for `158E141452`, D 2027-01-01 | spec §7 (new table under the benefits table); PremiumPath doc |
| F11 | Three admin validations, PSP-wide, inactive included, edit-only-when-changed | PremiumPath doc "Entering the rows"; W5 commit; reference §2 |
| F12 | W2 baseline hazard — legacy `contact_name`-only baseline makes `CONTACT_NAME` empty, and empty clears | reference §1 (own paragraph); data_exchange §1 |

## Open questions and who settles them

| Question | Settles it |
|---|---|
| Apply V103 to dev_ssa and production; verify production template column counts against production config (T247 closed on dev only) | Kevin — before the first production file 2 push |
| File 1 re-send semantics given empty-clears (T248) | Kevin (design) |
| How AMS learns a plan's employer funding method / emits Tier ID on HRA Enrollment; which HRA Enrollment template the tenant will hold | Kevin (design) — the next build |
| Card issuance trigger (TA-e, T245) | Test when issuance is live |
| `SummitResponseService` `fields[0]` classification, wrong for participant-keyed shapes | Code fix, not scheduled |
| Remove dead `findByPspAndServiceItem` (T249); `enrollment:` entry in `SUMMIT_IMPORT_TEMPLATES` (T250) | Small follow-ups |

## Contradictions found

1. W4's own spec said columns E, G **and H** all came from `planYearBegin`; the code showed H comes
   from the `plan_year_end` answer. Built to the code (F8); the prompt premise is corrected in the docs.
2. Spec §7's benefits table shows the generic plan effective 10/1/26; the verified run on 2026-09-12
   produced 2025-10-01. Both are right — the rule is run-date dependent — recorded under the table.
3. V103 as first written claimed to swap the unique key; the database kept both. Corrected before
   push (F7).
4. Earlier in the day: two template layouts documented from prose, both wrong (51b).

⚠️ **The pattern, named again.** Two template layouts were documented from an adjacent document and
both were wrong. W4's column-H premise came from a doc rather than the code and was wrong. V103's
index ordering was wrong until the database rejected it. **Read the picker, read the code, run the
thing — do not document from an adjacent document.** Every section of the reference now says which
of those three it came from.

## SQL close-out audit

- **Produced this session: V103 only** — `docs/migrations/V103__plan_template_map_sequence_and_date_rules.sql`,
  committed in `4d43dba` (index order corrected in place before commit), registered in
  `migration_tracker.md` and `docs/schema_version_migration.sql`. Applied to `beta_ssa`; **pending on
  dev_ssa and production**. This doc pass produced no SQL.
- **Run:** V103 on `beta_ssa` by Kevin, twice (first pass left both unique keys; corrected script's
  re-run dropped the old one) — not by Claude.
- **Recommended, not scripted:** none. T249/T250 are code/config, not schema.
- **Highest version:** V103 on disk = tracker header = `schema_version_migration.sql`.
- **Pending deployment:** V103 (dev_ssa, production).
- **Known orphan, re-reported, not fixed:** `docs/migrations/seed_ndt125_questionnaire.sql` — not a
  `V`-file, registered nowhere.
- **Schema described but not scripted:** none.

## Next

1. Commit this pass (five docs + PremiumPath doc + this file); push.
2. Apply V103 to dev_ssa and production; run the T247 production check (picker counts vs
   `SUMMIT_CDH_OPTIONAL_ELEMENTS` / `SUMMIT_EMPLOYER_OPTIONAL_ELEMENTS`).
3. Decide the HRA Enrollment layout and funding-method source — the next emitter build.
4. T249, T250, and the `SummitResponseService` classification fix as small follow-ups.

## Compliance statement

- Files created: 2 — `docs/analysis/premiumpath_summit_plan_structure.md`, this close-out.
- Files modified: 5 — `docs/analysis/summit_import_templates_reference.md`,
  `docs/business/summit_data_exchange.md`, `docs/analysis/summit_import_spec.md`,
  `docs/analysis/project_backlog.md`, `docs/analysis/migration_tracker.md`. All docs.
- SQL produced: none this pass. Java/JSP/config touched: none.
- Git mutations run: none (read-only `git log`, `git status`).
