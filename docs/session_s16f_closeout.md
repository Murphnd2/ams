# S16-F close-out — T150 production walk recorded, backlog reconciled, two findings filed

**Date:** 2026-08-05 · **Branch:** `refactor/modernize-architecture` · **Model:** Sonnet
**Documentation only. No source file was touched. No build ran.**

> This close-out does not record its own commit hash — the standing convention since S16-D, kept
> here for the same reason: a second commit purely to record a hash adds nothing a reader needs.

---

## 1. Shipped

One commit, two paths: `docs/analysis/project_backlog.md` and this file.

**Preflight HEAD:** `8061fc7f6eff0289cbf351694fecc91c0fa177d6` (read from `git log` this run; the
prior run, S16-D's docs commit).

---

## 2. Backlog reconciliation

**Before/after counts, `grep -c` against `docs/analysis/project_backlog.md`:**

| T-number | Before | After | Source of the row |
|---|---|---|---|
| T153 | 1 | 3 | Already existed (created by S16-D). The two new mentions are cross-references from the T154/T155 rows added this run, not edits to T153 itself |
| T154 | 0 | 2 | **Created this run**, from `docs/session_s16b_closeout.md:98` |
| T155 | 0 | 1 | **Created this run**, from `docs/session_s16b_closeout.md:99` |
| T156 | 0 | 1 | **Created this run**, from `docs/session_s16d_closeout.md:183-188` |

All three descriptions were detailed enough to write full rows without inventing content — each
close-out's "noticed and deliberately not fixed" entry already stated the defect, its cause, its
severity, and (for T154/T155) its fix shape or why it wasn't built. Nothing was guessed.

**Each new row is marked explicitly with its own provenance** — filed into the table for the first
time 2026-08-05 (S16-F), the close-out and section it came from named, the *original* filing date
and session preserved in the row's closing "Logged" line rather than overwritten with today's date.
A row's history is part of the row.

**Full T15x row inventory after this run, confirmed by listing:**
`T150 · T151 · T152 · T153 · T154 · T155 · T156 · T157 · T158` — nine rows, T150 through T158,
no gaps.

---

## 3. T150's verification upgrade

**`code-verified` → `runtime-verified`**, on a full production walk against `v0.88.07` with
`ICHRA_DEMO_ALLOW_STAGING_PROPOSAL=true` set in `ssa.properties` and Tomcat restarted.

Evidence appended to T150's row, summarized:

- **G2 exercised** — the RANGE-mode hand-off gate, which session 15's original four-gate inventory
  missed entirely (Phase A's finding, not new this run) — rendered the button **enabled** under the
  override.
- **D4 held under a real request** — all three staging banners still rendered, the footer still read
  *"Source: STAGING environment."* The override enabled the button and suppressed nothing.
- **Parameters survived the redirect and the POST** — the hand-off URL's `countyFips`, `mode`,
  `planYear`, `headcount` were confirmed present as hidden inputs inside the
  `action=createProposal` form via page-source inspection.
- **Proposal #138651 created**, no error, against a pre-existing production test prospect.
- **A `proposal_ichra_snapshot` row was written** — `snapshot_id 138652`, full field values recorded
  in the row. **This is the first successful execution of `attachRangeSnapshot` on any installation**
  — the primary reason the override was built, ahead of its demo value.
- **Two properties confirmed in practice, not only in design:** the stored `group_monthly_low`/`high`
  match the illustration to the cent (the snapshot captured the computed result, not just inputs),
  and `source_env` is honestly `STAGING` — so G5 (`ViewProposal:142`) refuses this exact row
  permanently. The self-limiting design held under a real write.

---

## 4. Two findings filed

**T-numbers assigned after Step 1's reconciliation**, per instruction, since that step could add
rows and did. `grep -c`/`grep -rn` for T157 and T158 both returned zero matches anywhere in `docs/`
before assignment — printed in the transcript above this document.

**T157 (LOW)** — `/ProposalBuilder`'s intake county chooser labels a staging-warmed county
*"(no rates cached yet)"* for a county that had just produced a full illustration in the same
session. Cause: the chooser's `priced` determination is `PRODUCTION_OK`-only. **This is the identical
`IchraZipLookup` behaviour S14-E hard-stopped on and deliberately declined to import into
`/GroupConversion`**, where T137 instead marks staging-sourced counties *"— test rates"* rather than
hiding or mislabeling them. Fix shape: apply T137's own pattern here.

**T158 (MED, dated to T136)** — the illustration hand-off and the proposal-builder intake record the
same real-world facts (county, headcount, contribution) in two textually distinct, unreconciled
namespaces (`countyFips` vs. `intakeCountyFips`, etc. — 11 occurrences confirmed by source
inspection, one hand-off, ten intake-prefixed). **No name collision** — this is not the
`countyFips`-always-wins defect class. The real problem: G5 (snapshot render) and G7 (merge tokens)
read from *different* namespaces, so the two can disagree. **Inert today** because G5 refuses the
staging-stamped snapshot outright; **live the moment production rate data lands**, which is what
dates it to T136 rather than leaving it open-ended. Recorded, not decided: Kevin's stated preference
for proposal-builder-first as the primary flow with illustration-first as secondary, and the cheap
direction (illustration-first prefilling the intake, intake as sole record) as a *direction* only.
Sub-item folded into the same row: county cannot prefill without either adding ZIP to the hand-off or
letting the intake's select accept a FIPS directly; `Eligible Employees` and contribution *can*
prefill today with nothing new built.

---

## 5. Contradictions found

**The S16-B filing gap, restated as a process finding rather than a one-off.** Three T-numbers —
T153, T154, T155 — were all reported as *"filed"* in `docs/session_s16b_closeout.md`'s "Anything
noticed and deliberately not fixed" section, and **none of the three existed in
`project_backlog.md` until S16-D (T153) and this run (T154, T155)** — a gap of one full session for
T153, and this session for the other two, which nobody had gone looking for until this run's Step 1
was specifically designed to check.

**This is not a single mistake; it is a pattern with one root cause worth naming plainly:**
*writing a sentence that says "filed as T-NNN" is not the same act as writing the row.* A close-out
is a narrative document read once and archived. `project_backlog.md` is the thing anyone — including
a future Claude Code run — actually queries. A finding that lives only in the former is a finding
that does not exist for any purpose except being re-discovered later by someone doing exactly what
this run did.

**Recommended standing correction:** filing a T-number means writing it into `project_backlog.md`
**in the same run that discovers it.** A close-out section that lists new findings should say *"filed
as row T-NNN in project_backlog.md, this run"* — never *"filed as T-NNN"* alone, which reads as
completed but describes only an intention. If a scope fence genuinely prevents writing to
`project_backlog.md` in a given run (as neither S16-B's nor S16-D's fence did, in fact, for either
row), the close-out should say so explicitly and name the row as *not yet written* rather than as
filed.

**No other contradictions found.** Phase A's gate-count claims (G1–G8) held exactly against this
walk's evidence — G2 fired as designed, and nothing observed on the walk contradicted any prior
close-out's technical claims.

---

## 6. New assumptions

**Asked directly: does anything recorded this run make a future failure mode worse, even where it
confirms a success path?**

**None that this run introduces.** This run wrote documentation only — no code changed, no behavior
changed, no gate loosened or tightened. The two new findings (T157, T158) describe **pre-existing**
conditions the walk exposed, not conditions this run created. T158 in particular is explicitly
recorded as *inert today* and only becomes live on a separate future event (production rate data
landing) that this run does not cause and does not accelerate.

The one thing worth naming as adjacent risk, though it is not a new assumption *from this run*: T150
itself (built in S16-D, not here) already carries the accepted assumption that a PSP-admin session
can now produce a staging-stamped snapshot row, bounded by G5's permanent refusal. This run's walk
**confirmed that bound holds under a real write** rather than introducing any new exposure.

---

## 7. Open questions raised

1. **T158's design direction — Kevin's call, not this run's.** Should the illustration hand-off carry
   a ZIP so county can prefill into the intake, or should the intake become the sole record with
   illustration-first prefilling into it rather than carrying a parallel parameter set? Both
   proposal-builder-first and illustration-first need to keep working, per Kevin's stated preference,
   and the cheap partial fix (headcount/contribution prefill) doesn't resolve the underlying two-
   namespace question either way.
2. **Whether T157's fix should ride along with whatever eventually resolves T136** (production
   allow-listing), since both are about the same underlying `PRODUCTION_OK`-only determination
   surfacing in a place that currently reads as "no data" rather than "test data."
3. **Whether the standing correction in §5 should itself become a written rule** somewhere more
   durable than a close-out — a CLAUDE.md line, or a note at the top of `project_backlog.md` itself,
   so the next session that files a finding sees it before writing the sentence rather than after.

---

## 8. Code-verified-only disclosure

**Everything in §3 (the T150 production walk) was operator-observed by Kevin at a browser and read
by Kevin from production MySQL. It is not code-verified, and it was not verified by this run or by
any Claude Code run.** This run's role was transcription into the backlog, not observation — no
browser was opened, no server was queried, no request was made by this session. Labeled precisely as
instructed: **operator-observed**, not downgraded to "unverified" and not claimed as this run's own
verification.

Everything in §2 and §4 (the reconciliation and the two new findings) **is** this run's own
work — reading existing close-out files and the backlog table, confirmed by the `grep` commands
whose output is quoted or summarized above. That part is code/doc-verified in the ordinary sense:
read, not run.

---

## 9. SQL close-out audit

**This run produced no SQL.** No migration, no DDL, no DML, no `INSERT INTO constant`. Nothing in
`docs/migrations/` was touched — confirmed by scope (this run's write access excluded it entirely)
and by the fact that no such file appears in this run's diff.

**One `SELECT` and one `DESCRIBE` were run during the T150 walk — by Kevin, directly against
production MySQL, read-only, as part of observing the snapshot row.** They are not part of any
migration, were not run by this Claude Code session, and are recorded here only because the walk's
result (the `snapshot_id 138652` row values in §3) came from them.

**Highest migration read from `docs/migrations/` this run: `V088__proposal_ichra_intake_contribution.sql`.**
Unchanged — confirmed identical to S16-D's own reading of the same directory.

---

## 10. Compliance statement

**Scope fence held exactly.** Write access was `docs/analysis/project_backlog.md` and
`docs/session_s16f_closeout.md` — both, and nothing else.

**`git diff --name-only` for the commit this run produces will show exactly two paths:**
`docs/analysis/project_backlog.md`, `docs/session_s16f_closeout.md`.

**No source file was touched.** No `.java`, `.jsp`, `.sql`. `docs/migrations/` was not written.
`docs/ichra_strategy.md`, `docs/swbd_ichra_build_plan.md`, `docs/analysis/snapshot_reader_audit.md`,
and every existing close-out other than this new one were read-only or untouched, per the fence.

**Hard stops:** the run brief specified one — branch wrong, tree dirty, or a non-clean pull. **It did
not fire.** Branch was `refactor/modernize-architecture`, the tree was clean at preflight, and
`git pull --ff-only` reported *"Already up to date."*

**Every hash was read from `git log` this run.** No forbidden git operation ran — no `add -A`,
`add .`, `add -u`, `stash`, `checkout`, `restore`, `reset`, `rebase`, `tag`, `branch`, or
force-push. **Commit count: one.**

---

## 11. Next

1. ⭐ **Decide T158's design direction** (§7.1) — it gates how much further proposal-builder/
   illustration integration work is worth doing before production rate data lands and the two-
   namespace disagreement becomes observable.
2. **T157** is cheap and can ride with T136 or be taken alone — apply T137's fail-toward-labeling
   pattern to the intake chooser.
3. **Consider the standing correction in §5** for somewhere more durable than a close-out, so a
   future session sees it before writing "filed as T-NNN" rather than after.
4. The T150 override remains **off everywhere except wherever the walk's `ssa.properties` edit was
   made** — confirm with Kevin whether that installation's flag should be reverted to OFF now that
   the walk is complete, or left set for further exercise.
