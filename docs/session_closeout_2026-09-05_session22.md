# Session 22 close-out

Date: 2026-09-05. Branch: `refactor/modernize-architecture`. HEAD at close: `72c118295d696bf794b57a6ce890b616213aa06a` (verify against `git log -1` — do not trust this number after the fact).

Session boundary verified this run: `73c9c81dcd8b9150ca060e8b82d7333a844babd8`'s parent is `5ac66a56e9007f72afd5432ad9e043c71a0b10f8` (`docs: S21-P -- bring proposal-content-page token tables current with T171/T172`), confirming `73c9c81` is this session's actual first commit.

---

## 1. Shipped

Both hashes below read fresh this run via `git log`, not copied from the generating prompt.

- **`73c9c81`** — `docs: file T175/T176 and Phase A findings on the §125 LOS surface` — `docs/analysis/project_backlog.md` (+2 lines, T175/T176), `docs/analysis/section125_los_phase_a.md` (new file, 195 lines).
- **`72c1182`** — `docs: register LA entries for the §125 / micro-ICHRA card structure` — `docs/analysis/legal_assumptions.md` (+202 lines, LA-19 through LA-24).

Both are documentation. **No application code, no schema, no behavior change shipped this session.**

---

## 2. In flight

`docs/migrations/V092__hsa_enrollment_assistant.sql` (untracked), plus `docs/analysis/migration_tracker.md` and `docs/schema_version_migration.sql` (both modified) — the V092 cluster. Uncommitted because their contents have not been reviewed and Kevin could not confirm what V092 contains. They were deliberately excluded from both of this session's commits by explicit scope fence, and verified byte-for-byte unchanged after each commit this session. **This is the first thing to resolve next session** — any new migration would be V093, and sequences behind it.

---

## 3. Decisions made

Recorded here without `D-NN` numbers — see the note at the end of this section.

1. **AMS's role in the prospective §125 / micro-ICHRA structure is the proposal, not the administration.** Closed by the Phase A finding that `Setup` creates no `Benefit`, that no per-employee allowance or waiver state exists anywhere in the model, and that `Prospect`/`Setup` have no relational link to `summit.archive.Employer`. Per-employee administration is Summit's, and out of scope for AMS analysis. This retired two build items scoped earlier in the session before either was specced.
2. **Build the structure generically, not a specific carrier's deal.** The proposal section models a micro-ICHRA plus §125 plus taxable stipend, with alternative-coverage cost agent-entered. No carrier name, no product name, no fetched third-party rate. Closes the question of whether to model one partner's product directly. Rationale: preserves the no-steering boundary, extends the existing "nothing partner-specific in code" principle, and keeps the section saleable in states where any given carrier isn't licensed.
3. **Arrears loading, not annual true-up, is the control on §125 sub-account funding.** Recorded in full at LA-21.
4. **A new LOS is created by Kevin in Service Manager when testing requires it.** Not a work item and never a dependency — Phase A confirmed `ServiceManagerAction.createLos` auto-creates the `ServiceItem` and `ServiceModule` and links every `ALL`-scoped `ApplicationSection`, so a new LOS needs zero code.

**Note.** These four are not filed as `D-NN` rows. The decision register's location is currently ambiguous — `D-NN` appears both as strategy decisions in `ichra_strategy.md` (D4, D38) and as deployment-backlog items in `CLAUDE.md`'s migration rules — and `ichra_strategy.md` is separately known to be stale (§6, item 1). Assigning numbers into a register whose location and health are both uncertain would compound the problem. **Resolving where D-numbers live is itself a next-session item.**

---

## 4. New assumptions

LA-19 through LA-24, filed this session in `docs/analysis/legal_assumptions.md`.

- **LA-19 — §125 pre-tax treatment of individual premiums requires ICHRA coverage and off-exchange purchase.** Reversal cost: low before any employee election is collected; high afterward (per-employee, per-pay-period W-2/941 corrections). ⚠️ **OPEN and load-bearing:** whether the amended cafeteria plan regulation conditions pre-tax treatment of individual premiums on the employee being *covered by* an ICHRA or merely *offered* one. It decides whether the waive-for-subsidy population keeps pre-tax treatment, and it must be settled before any plan document issues.
- **LA-20 — ICHRA and §125 funds may share one card with separate sub-accounts.** Reversal cost: low — card configuration and funding schedule.
- **LA-21 — Arrears loading is the control preventing employer-payment-plan characterization.** Reversal cost: low — funding schedule.
- **LA-22 — Employer advance of excepted-benefit premiums is not an employer payment plan.** Reversal cost: low — funding schedule, but retroactive if the excepted-benefit characterization is ever challenged.
- **LA-23 — An ICHRA may fund on-exchange premiums by member-name card; residual is post-tax only.** Reversal cost: low.
- **LA-24 — The structure is designed for non-ALEs and inverts above 50 FTEs.** Reversal cost: none if caught at qualification; the engagement itself if not.

---

## 5. Open questions raised

1. **LA-19's "covered by" vs. "offered."** Settled by reading the amended cafeteria plan regulation against primary text. Not settleable from the repo.
2. **What V092 contains and whether it is complete.** Settled by reading the file and its two associated doc edits.
3. **T175 — Texas silver load breadth.** Largely answered already: the V078 migration header records a live Hopkins TX probe at age 40, off-exchange $489.38 against on-exchange $705.37 — a spread far too large to be plan-mix noise, and indicating an on-exchange-only load. Left open at LOW because it is one county, one age, one plan year, and the two figures are each their own channel's lowest-cost silver rather than the same plan. Settled by the same comparison across more warmed Texas rating areas.
4. **Where `D-NN` decisions are registered.** Settled by a decision from Kevin.

---

## 6. Contradictions found

1. **D4 in `ichra_strategy.md` is stale.** It records that enhancement visibility is controlled by rate-table pricing and not a flag; V089's `enhancement.system_managed` plus `FlaggedEnhancementResolver` is exactly that flag, and `ViewProposal` reads it. Found during Phase A, recorded, not fixed.
2. **`docs/migrations/seed_ndt125_questionnaire.sql` has no `V{NNN}__` prefix**, so it self-registers no `schema_version` row and the migration tracker cannot see it.
3. **Documentation path drift.** The backlog is at `docs/analysis/project_backlog.md`, not `docs/project_backlog.md`. A prompt authored outside the repo asserted the latter and hard-stopped correctly. Worth recording because the hard stop worked and the wrong path was cheap; the same error without a fence would have created a second backlog file.
4. **Checked `swbd_ichra_build_plan.md` and `ichra_strategy.md` for any statement that AMS administers per-employee ICHRA benefits, or that a `Benefit` is created at setup — neither found.** Targeted search (`administ`, `per-employee.*(ICHRA|benefit)`, `Benefit is created`, `creates? a Benefit`, `Benefit row`) across both files turned up only: `ichra_strategy.md`'s repeated "agent utility is the wedge, administration revenue is what it earns" framing (administration as a future business line, not a claim that AMS performs it today), and its line-40 note that "Summit provides no import template for creating Premium Billing benefit plans, so every group needs at least two benefits hand-created" — which describes manual work inside **Summit**, a separate system, not a claim that AMS's own `Setup` flow creates a `Benefit` row. **No contradiction found against the Phase A findings in either file.**

---

## 7. Next

**Recommended: review and commit V092 and its two doc edits**, because the tree cannot be clean and no new migration can be numbered until it is.

**Then the §125 structure proposal section** — a new LOS-scoped section rendering the micro-ICHRA plus §125 plus taxable stipend structure with the employer's before/after cost position. Follows the T171/T172 pattern shipped in session 21, so Phase A is skippable: re-deriving anchors that recent would cost more than the build it enables. Needs two additional nullable scalars beside `ProposalIchraIntake.monthlyContributionPerEmployee`, which is a small additive migration on a sales-side table and would be V093 — hence the V092 dependency.

**Also carried forward and unchanged from prior sessions:** T166 (HealthSherpa plan fetch, which `ICHRA_PLAN_LANDSCAPE_TABLE` is empty without), T170, T173's two flagged discrepancies, T174's untouched `ViewProposal.java:144`, and the unreleased commit queue.

---

## 8. SQL close-out audit

**No SQL was produced by any of this session's runs.** Not the Phase A investigation, not the T175/T176 backlog filing, not the LA-19–24 registration, and not this close-out. Say so in those words: no SQL statement was produced, run, or recommended this session.

- **In a versioned migration:** not applicable — nothing was produced.
- **Orphaned `.sql` files:** `docs/migrations/seed_ndt125_questionnaire.sql`, named explicitly (also §6, item 2) — no `V{NNN}__` prefix, invisible to the tracker. Noticed, not created, this session.
- **Current highest migration version in the repo:** **V092** (`docs/migrations/V092__hsa_enrollment_assistant.sql`) — **still untracked and unreviewed**, unchanged in state across this entire session (§2).
- **Pending deployment:** nothing schema-related shipped this session to deploy. V092 itself is pending review, not deployment, until its content is confirmed.
- **Schema described but not scripted:** none this session — LA-19 through LA-24 and T175/T176 are compliance and investigation records, not schema proposals.
