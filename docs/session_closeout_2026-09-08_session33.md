# Session 33 close-out

Date: 2026-09-08. Branch: `refactor/modernize-architecture`. Baseline at session start: `f432aa8`
(`docs: session 32 close-out`). A documentation-only session: files the off-exchange first-month premium
funding cadence into the legal assumptions register as LA-36, wires it to the two neighbouring entries it
changes the meaning of, and files the build row it implies. **No application code, no schema, no SQL, no
migration.** Ends at this commit.

⭐ **The headline: the entry that shipped is not the entry that was first written, and the difference is
the whole point.** LA-36 as drafted asserted that the cadence carve-out was *"verified against the
regulation text and the corresponding IRS bulletin"* and that the elective PTO rules were *"also verified
in the same reading."* Neither was true at that tier. The cadence was read in **agency text** — the
Federal Register preamble and IRB 2007-39 — with the operative regulation section unread; the PTO rules
rested on **practitioner commentary only**. Both claims were corrected before anything was committed, and
the register now records three distinct tiers of source inside one entry rather than one flat claim of
verification.

⚠️ **The counter-headline: the overclaim was caught by reading a run's own report, not by the run that
wrote it.** The writing run had no way to test a verification claim handed to it in a prompt — it could
only transcribe it. That is a process finding, and it is written up in §5 rather than left as an anecdote.

The session ran as three sub-runs, each a fresh Claude Code session:

| Sub-run | What | Outcome |
|---|---|---|
| **S33-A** | LA-36 filed; LA-21 and LA-22 Status cross-references; T219 row | Tree dirty, two files |
| **S33-B** | LA-36 Basis and Status replaced — verification provenance restated by tier | Tree dirty, one file |
| **S33-C** | `§4980D` spacing fix; commit S33-A + S33-B; this close-out | `01c3b2e`, this commit |

⭐ **S33-A and S33-B were both run under a no-git-mutation fence and left the tree dirty deliberately.**
Three runs' worth of edits were reviewed as one diff before anything was staged, which is what made the
S33-B correction possible at all — had S33-A committed on its own, the overclaim would have shipped.

---

## 1. Shipped

One commit, the hash read from `git log` and the file count from `git show --stat`:

| Hash | Subject | Files | Lines |
|---|---|---|---|
| `01c3b2e` | `docs: file LA-36 off-exchange first-month funding cadence; T219` | 2 | +86 / −2 |

What it carries, one line each:

- **LA-36 filed** — *First-month premium funding comes from an employee-sourced on-ramp; the month-ahead
  cadence is expressly permitted.* Seven fields, five named on-ramp options, two decisions closed inside
  the entry. Placed at the end of the register, before `## Candidates considered and not adopted`.
- **LA-21 Status cross-reference** — arrears loading demoted from *the* control preventing
  employer-payment-plan characterization to **one of five permitted on-ramps**, with the month-ahead
  cadence named as what makes the concession unnecessary where an employee-sourced on-ramp funds month one.
- **LA-22 Status cross-reference** — the excepted-benefit asymmetry made findable from the major medical
  side: excepted benefits sit outside the market reforms and carry a §106 shelter on unrecovered premium;
  individual major medical has neither, which is why the same card mechanism runs differently by benefit
  type.
- **T219 filed** — per-employee first-month funding source and front-loadable deduction schedule, MED,
  📋 Planned, written into `project_backlog.md` in the same run that raised it (standing rule S16-G).
- **`§4980D` spacing fix** — one character, in LA-36's Status field, closing up the section symbol to
  match the file's convention in all other occurrences including LA-21's own Basis. `Prop. Treas. Reg.
  § 1.125-1` in the Basis field keeps its space: that is formal citation form, and it is correct where it
  sits. Two conventions, both right in their place.

**Nothing else in either file was touched.** Verified three ways in S33-B: diff hunk ranges fell entirely
inside LA-36's line span; LA-36 with its Basis and Status blocks programmatically removed was
byte-identical before and after; and everything before the `### LA-36` heading and everything from
`## Candidates considered` onward compared identical.

---

## 2. In flight

**Nothing is uncommitted in the repo at close.** The tree is clean and both commits are pushed.

⚠️ **But two artifacts produced or reaffirmed this session live outside the repo, and neither has a home:**

1. **The two-part off-exchange funding talking points document** — an employer-facing script and an
   agent/vendor version, plus an internal notes section marked **not for distribution**. Held in project
   knowledge, **not committed anywhere**. ⚠️ **Open question of whether it belongs in `docs/`** — it is
   the customer-facing expression of exactly what LA-36 now records internally, and the two will drift
   apart the moment one is edited without the other. The internal-notes section is the reason it is not
   an obvious `docs/` commit: the repo has no established convention for a document carrying a
   do-not-distribute section inside it.
2. **The two-benefit-types-on-one-card commitment from the Presidio call** — one sub-account running
   **loose** for excepted benefits, one requiring **exact payroll match** for ACA. ⚠️ **The Presidio call
   record states this should be a decision row. It is still not filed anywhere**, and it has now been
   carried across several sessions unfiled. **Deliberately not filed in this run** — the run's scope fence
   permitted one register file and this close-out, and nothing else. See §5 and §8: the stated blocker for
   filing it does not appear to survive contact with the repo.

---

## 3. Decisions made

Two, both closed inside LA-36 rather than left as open design questions:

1. **Employer advance of off-exchange individual major medical premium, with payroll recoupment, is not
   offered, not negotiated per case, and not built.** This closes a design question raised live on the
   Presidio call and reopened repeatedly since. The entry states it flatly — the one-month gap at
   inception *"is never closed by employer advance"* — so the next person to propose it is proposing a
   change to a recorded decision rather than raising a fresh idea. **LA-36's Confirm-before field names
   exactly that event** as a trigger to revisit.
2. **Cross-year restoration of sold PTO days is closed.** It is deferred compensation under the
   prohibition on using one plan year's contributions to purchase a benefit provided in a subsequent
   year — and, independently of the tax analysis, **it restores nothing economically regardless.** Two
   separate reasons, either sufficient.

Both are recorded inside the entry rather than in a separate decision log, which is a third decision made
by omission and is picked up in §8.

---

## 4. New assumptions, each with reversal cost

| Assumption | Reversal cost |
|---|---|
| **LA-36 — a cafeteria plan may fund off-exchange individual major medical on a month-ahead cadence**, each month's reductions funding the following month's premium, including the last month of a plan year funding the first month of the next, without deferring compensation. A twelve-month first plan year therefore funds **thirteen** months of premium, and the gap is closed from employee-sourced funds. | **Low.** Funding schedule and per-employee on-ramp selection. **No schema depends on the choice** — the sub-accounts, ledgering and monthly cadence are identical across all five on-ramps. If the carve-out were read more narrowly than assumed, the fallback is option E, which is already the LA-21 default and costs one onboarding cycle. |

⭐ **The verification is deliberately tiered, and the entry says so in both its Basis and its Status.**
This is the substance of the session, not a caveat on it:

| What | Tier | Consequence |
|---|---|---|
| **The month-ahead cadence carve-out** | **Agency text** — Federal Register preamble to the 2007 proposed cafeteria plan regulations, restated in IRB 2007-39. ⚠️ **The operative regulation section itself was not read.** | One tier short of primary text, recorded that way deliberately. Load-bearing on the whole entry. |
| **The cross-year purchase prohibition** | A **proposed regulation quoted in an IRS notice** — Prop. Treas. Reg. § 1.125-1, Q&A-7 as quoted in Notice 2005-42. | Sets the boundary on the other side; forecloses stretching a prior-year reduction past the first month. |
| **The elective PTO triad** — no carryover, ordering rule, cash-out or forfeit at year end | ⚠️ **Secondary commentary only.** Consistent practitioner sources, several and independent, but **not primary text.** Agency text was read only far enough to confirm PTO is a permitted taxable benefit and that carryover is barred generally as deferral. | **Gates on-ramp option B alone.** Options A, C, D and E do not depend on it, and B is already recorded as *available, not recommended*. That containment is why the gap is tolerable. |

**No employee tax position depends on the cadence carve-out being available** — it governs only whether a
December reduction may fund January, and where it may not, coverage starts a cycle later. ⚠️ **The
material risk sits on option A rather than on the cadence:** a stipend conditioned on purchasing coverage
is an employer payment plan regardless of payroll coding, and it also enlarges the employer's effective
contribution, which threatens the deliberate unaffordability the waiver path in LA-24 depends on.

---

## 5. Contradictions found

**One, and it is a process finding rather than a conflict between documents.**

⚠️ **LA-36 as first written claimed a tier of verification that had not occurred.** The Basis field said
the cadence was *"verified against the regulation text and the corresponding IRS bulletin rather than
assumed,"* and that the elective PTO rules were *"also verified in the same reading."* Neither held. The
regulation text was not read — the preamble and the bulletin were — and the PTO rules came from
practitioner commentary, which is not the same reading and not the same tier.

**How it was caught matters more than that it happened.** It was caught by **reading the writing run's own
report**, which flagged the claim as supplied-not-tested, and not by the run that wrote the entry.
⚠️ **The writing run had no way to catch it.** A verification claim arrived in a prompt as a bare
assertion; the run's job was to transcribe it into the register; nothing in that task could test whether
the assertion was true, and a register entry does not carry a marker distinguishing *"the author verified
this"* from *"the author was told this was verified."*

**The correction, generalised:** where a prompt asserts that something has been verified, **the tier and
the source belong in the prompt text** so the entry can carry them. "Verified" alone is not a fact a
writing run can act on responsibly — it is a claim the run will faithfully launder into the permanent
record. LA-36 now carries three tiers explicitly, which is what a future reader needs and what a bare
"verified" would have denied them.

⚠️ **This is the same failure shape the register's own closing instruction warns about** — *"verify
against primary text before any of it is quoted to counsel or into a plan document"* — arriving from the
direction nobody was watching: not a reader over-trusting the register, but the register being written to
assert more than its author had.

**Also noted, non-blocking:** `docs/deployment_backlog.md` D-95 reads **"Not applied anywhere"** for V096,
while the session 32 close-out records V096 applied to `beta_ssa` at 2026-09-08 15:53:48. These are
probably compatible — D-95 is a *deployment* row and `beta_ssa` is the local dev database, not a deployed
environment — but the phrase "anywhere" does not say so. **Not corrected in this run**, which was fenced
to one register file and this close-out.

---

## 6. Open questions

1. ⚠️ **The elective PTO triad is unverified against primary text** — no carryover between plan years, the
   ordering rule requiring nonelective PTO first, and cash-out or forfeiture of unused elective PTO at
   year end. **Gates on-ramp option B only**; options A, C, D and E do not depend on it. **Settled by:**
   reading the operative regulation, before any PTO on-ramp goes live in a real plan. Not urgent, because
   B is recorded as available rather than recommended — but it must not be reached by accident.
2. ⚠️ **The precise conditions under which increased taxable compensation escapes employer-payment-plan
   characterization.** **Load-bearing on on-ramp option A**, which is the recommended path wherever a
   stipend already exists — so this is the more consequential of the two unverified items despite being
   the less visible. **Settled by:** primary text.
3. **The §4980D minimum-penalty and cap figures**, used in internal reasoning and **deliberately kept out
   of every employer-facing artifact.** **Settled by:** primary text. Lowest stakes of the three: nothing
   is quoted to anyone.
4. ⚠️ **LA-19's "covered by" versus "offered" question remains OPEN** — whether the amended cafeteria plan
   regulation conditions the §125 permission on the employee being *covered by* an ICHRA or merely
   *offered* one. Its own Status still reads **"the most load-bearing unverified item in the structure,"**
   and that is unchanged by this session. **Restated here because LA-36 sits directly on top of it:** the
   entire month-one funding question only arises for employees whose individual major medical premium is
   pre-taxable at all, which is what LA-19 governs.
5. **Where decision rows live in this repo** — stated as the blocker on filing the card sub-account
   commitment. ⚠️ **The repo appears to answer this already.** See §8: the premise did not survive a look
   at the files.

---

## 7. Backlog and deployment items from this session

One backlog row, written in the same run that raised it (standing rule S16-G). No deployment rows — this
session shipped nothing deployable.

| Item | Priority | State |
|---|---|---|
| **T219** — per-employee first-month funding source field and front-loadable deduction schedule | MED | 📋 Planned — **specified and unblocked** |

T219 records that card funding must capture, per employee, which month-one on-ramp applies — bonus or
stipend, PTO sale, double first deduction, employee direct pay, or delayed effective date — and must
support per-pay-period deduction amounts that **vary rather than levelling** across the plan year.
⭐ **It is a funding-source field plus a front-loadable schedule, not a new mechanism**: the sub-accounts,
ledgering and monthly cadence are unchanged across all five on-ramps, which is exactly why LA-36's
reversal cost is low.

Carried unchanged from session 32 and not touched here: **D-95** (apply V096; ship the WAR and the
migration together), **T213** (transport undecided, SDX-01 still unasked), **T210**, **T214**, **T215**,
**T217**, **T218**.

---

## 8. Next

**Recommended: file the card sub-account commitment now — the stated blocker does not hold.**

The premise carried into this session was that filing it is blocked because *"where decision rows live has
not been established."* ⚠️ **The repo says otherwise, and it took one grep to see it.** A numbered product
decision series exists, is current, and lives in a document that is precisely about this subject:

- **`docs/analysis/plus_tier_build_plan.md` carries the D-series and it runs to D40** — the canonical
  plus-tier plan, described in the backlog's own row #43 as *"Canonical plan … decisions D1–D37, open
  items O1–O38."* The series has moved past that description on its own.
- **`docs/business/plus_tier.md`** holds the earlier product decisions D1–D17.
- **`docs/deployment_backlog.md`** holds the **D-NN** deployment series, currently to D-95 — a different
  series with a different prefix, which is probably what made the question feel unsettled.

So the commitment — **one sub-account running loose for excepted benefits, one requiring exact payroll
match for ACA** — has an obvious home as the next D-number in the plus-tier build plan. ⭐ **And the
register already carries its reasoning**: LA-20 establishes the two-sub-accounts-one-purse structure, and
the LA-21/LA-22 asymmetry this session made explicit *is* the justification for the two sub-accounts
behaving differently. The decision row would point at them rather than re-argue them, which makes it a
short row, not a research task.

**Why this first, over the alternatives:** it is a design commitment **made on a call and carried unfiled
across several sessions**, which is the precise failure mode standing rule S16-G exists to prevent — and
it **shapes the funding build**, so T219 will be specified against it whether or not it has been written
down. Filing it costs one short row. Leaving it costs another session of it existing only in a call
record.

**Then, in this order:**

1. **T219 — the funding source field and front-loadable schedule.** Now specified and unblocked. Do it
   after the decision row, so the "loose versus exact payroll match" distinction is recorded before code
   is written against it, rather than inferred from the code afterwards.
2. **Decide whether the talking points document belongs in `docs/`.** ⚠️ **The real question is not
   whether to commit it but what to do with its do-not-distribute section**, and that is a convention
   decision the repo has not had to make before. A defensible split: the employer and agent/vendor scripts
   into `docs/business/`, the internal notes folded into LA-36 or the new decision row where they are
   already governed. Cheap, and it stops the customer-facing copy from drifting away from what the
   register says.
3. **Read primary text on open question 2** — the conditions under which increased taxable compensation
   escapes employer-payment-plan characterization. It is load-bearing on on-ramp option A, which is the
   recommended path wherever a stipend exists, so it is the unverified item most likely to be reached in
   practice.

⚠️ **Not recommended as next, but not to be lost: the DataPath email.** Session 32 recommended it, it
remains the longest pole in the Summit track, and it still costs one email. It is a different track from
this session's work — nothing here advanced or blocked it — and it should not disappear because a funding
session intervened.

---

## 9. SQL close-out audit

**Session 33 produced no SQL.** Every item below verified against the repo, not from memory:

- **No SQL was produced, run, or recommended** in any of the three sub-runs. All three carried an explicit
  no-SQL fence and all three honoured it.
- **No `.sql` file was created**, and **none was orphaned.** `git show --name-only 01c3b2e` lists exactly
  two files, both `.md`: `docs/analysis/legal_assumptions.md` and `docs/analysis/project_backlog.md`.
- **No migration was authored, applied, or modified.** `docs/migrations/` is untouched; nothing was added
  to `docs/analysis/migration_tracker.md` or `docs/schema_version_migration.sql`.
- **No schema change is implied by anything filed this session.** LA-36 states it in its own Reversal cost
  field — *"No schema depends on the choice; the sub-accounts, ledgering and monthly cadence are identical
  across all five options"* — and T219 restates it: a funding-source field and a variable schedule land in
  a subsystem that does not yet exist, whose schema is not being designed here.
- **Current highest migration version: V096** (`docs/migrations/V096__summit_file_export.sql`), read by
  listing `docs/migrations/`. **73 migration scripts in tree.** Unchanged by this session.

### Pending deployment

⚠️ **Two migrations are unapplied to production, both carried from earlier sessions and neither advanced
here:**

| Version | State |
|---|---|
| **V095** — `summit_plan_template_map` | ⚠️ On `beta_ssa` only. **Not on production.** |
| **V096** — `summit_file_export` | ⚠️ On `beta_ssa` (applied 2026-09-08 15:53:48). **Not on production** — D-95 open. |

**Production remains at V094** (release `v0.94.00`). ⚠️ **The weekly refresh of `beta_ssa` from production
drops both V095 and V096 and every row in their tables** — re-apply both, in order, after any refresh.
D-95's instruction stands: **ship the WAR and the migration together.** A WAR without `summit_file_export`
downloads exports correctly and writes one ERROR line per export, forever.

**Nothing from session 33 is pending deployment.** This session shipped documentation only.
