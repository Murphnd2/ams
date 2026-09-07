# Session 24 close-out

Date: 2026-09-07. Branch: `refactor/modernize-architecture`. Baseline at session start: `576fe39`
(`docs: session 23 close-out`). Docs-only session — no Java, no JSP, no SQL, no migration.

---

## 1. Shipped

Two sub-runs, both documentation-only, working against real test evidence supplied in the prompt
(not derived from the repo — the repo had no prior record of any of this).

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

None. This session introduced net-new documentation of a previously undocumented integration
surface; nothing in it was checked against or found to conflict with `ichra_strategy.md`,
`domain_and_compliance_rules.md`, or `CLAUDE.md`.

---

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

---

## 8. SQL close-out audit

**No SQL was produced, run, or recommended this session.** Both sub-runs were explicitly
documentation-only with a scope fence forbidding any `.sql`/`.java`/`.jsp`/`.xml`/`.properties`
file. Neither touched a database. Highest migration version remains **V093** (unchanged from
session 23) — this session did not add, modify, or reference a migration file.
