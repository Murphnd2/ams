# Session 34 close-out

Date: 2026-09-08. Branch: `refactor/modernize-architecture`. Baseline at session start: `8e7c40d`
(`docs: session 33 close-out`). A documentation-only session: files the card sub-account funding
commitment as **D41 / Part 13** of the plus-tier build plan, and corrects **D-95**'s deployment status
from a claim that was false as written to one scoped, dated and durable. **No application code, no
schema, no SQL, no migration.** Ends at this commit.

⭐ **The headline: the session's second item was a hard stop, and the hard stop was the work.** Item 2
was handed over as a wording fix — D-95 said *"Not applied anywhere"* while V096 had been applied to
`beta_ssa`, so change the over-broad word. **The file said otherwise.** Every row using "anywhere"
pairs it with an unchecked local box, and D-91 — done locally, not on production — scopes its status
to **Production by name** instead. So "anywhere" was doing exactly the work it was meant to do, and
the discrepancy was a **status claim**, not a loose word. 34a refused to treat it as wording and
stopped. Kevin ruled, 34b applied it.

⚠️ **The counter-headline: two documentation sessions in a row.** Session 33 was documentation-only and
so was this one. This project's output is Claude Code build prompts; filing and research are means, not
deliverables. §8 names it rather than leaving it to be noticed later.

The session ran as three sub-runs, each a fresh Claude Code session:

| Sub-run | What | Outcome |
|---|---|---|
| **S34-A** | D41 filed as Part 13; header bumped to Revision 10; Item 2 hard-stopped as Case B | Tree dirty, one file |
| **S34-B** | Kevin's call applied — D-95 status Production-scoped, dated, refresh-reasoning recorded | Tree dirty, two files |
| **S34-C** | Commit S34-A + S34-B; this close-out | `d2b52e9`, this commit |

⭐ **S34-A and S34-B both ran under a no-git-mutation fence and left the tree dirty deliberately** — the
same discipline session 33 used. Both halves were reviewed as one diff before staging.

---

## 1. Shipped

One commit, the hash read from `git log` and the file count from `git show --stat`:

| Hash | Subject | Files | Lines |
|---|---|---|---|
| `d2b52e9` | `docs: file D41 card sub-account funding split; correct D-95 status` | 2 | +67 / −6 |

Both files are `.md`: `docs/analysis/plus_tier_build_plan.md` and `docs/deployment_backlog.md`. This is
the only commit in the session other than this close-out's own.

---

## 2. In flight

**Nothing.** `d2b52e9` pushed cleanly to `refactor/modernize-architecture`. No branch was cut, no work
was left uncommitted, and no item was half-finished at the boundary between sub-runs. The session
carries nothing forward except the open questions in §6.

---

## 3. Decisions made

**D41 — one card, two sub-accounts, two funding rules that differ by benefit type.** Filed as **Part
13** of `docs/analysis/plus_tier_build_plan.md`. A single card carries two separately-ledgered
sub-accounts. **Excepted-benefit premium** may be employer-advanced at the start of a coverage period
and collected by salary reduction across it. **Off-exchange individual major medical** may not be
advanced at all: funded only from salary reduction already withheld, and the balance must never run
ahead of withholding.

**What it closes:** whether the card's funding behaviour is uniform across benefit types. **It is not,
and cannot be.** The card itself cannot distinguish them, so **the sub-account configuration is what
carries the distinction** — which is what makes separate ledgering a compliance control rather than
plumbing. The reasoning was already in the register (LA-20 for the shared-card structure, LA-22 for the
excepted-benefit side, LA-21 and LA-36 for the major medical side); only the row recording the
consequence for card configuration was missing.

**D-95's status treatment — a ⬜ on a migration row means *not durably applied*.** Local dev cannot
reach ✅ for a migration while the weekly refresh returns the schema to production's version: any ✅
would be true on the day it was written and silently false at the next refresh. The application is
recorded as **dated prose in the Status line instead** — `Applied to beta_ssa 2026-09-08 15:53:48` —
which stays true across refreshes and removes the dependency on whether a refresh has run since.

⭐ **This establishes the convention.** S34-B searched the whole file for any prior row describing a
local state the refresh drops and **found none** — every hit was inside D-95 itself, and V095 has no
row of its own. D-95 is the first, so its wording is now the precedent.

---

## 4. New assumptions, each with reversal cost

**A1 — D-91's local box can be ✅ and D-95's cannot, and the split is principled rather than
inconsistent.** D-91 is an `ssa.properties` entry; a **database** refresh does not touch a properties
file, so its local state is durable and ✅ is correct. D-95 is **schema**, which the refresh returns to
V094, so its local state is transient and ⬜ is correct. Two adjacent rows in the same file, same
environment, opposite marks — and both right.

⚠️ **Recorded specifically so a later tidying pass does not "correct" them into agreement.** The pair
looks like an inconsistency to anyone reading quickly, and the natural repair — flipping D-95 to ✅ to
match — reintroduces exactly the stale-checkbox failure this session removed.

**Reversal cost: low.** It is a documentation convention affecting checkbox semantics on migration rows
in one file. If a future refresh policy stops dropping local migrations, the distinction dissolves and
migration rows can carry a durable ✅ like property rows do. Nothing in code, schema or deployment
depends on it.

**A2 — the origin of D41 rests on prompt assertion, not on a repo artifact.** See §5.1. Reversal cost:
low, and it is already carried inside D41's own evidence-grade block rather than only here — if a call
record surfaces, the block is deleted and nothing else changes.

---

## 5. Contradictions found

**1. No Presidio call record exists in the repo.** The session 34 prompt cited one as D41's origin —
Kevin proposed, Daniel accepted. ⚠️ **It is not in the repository.** `Daniel` appears exactly once, in
`docs/business/swbd_premiumpath.md` line 33, identifying Daniel Cruz as Presidio's CEO/actuary — a
roster line, not a call record. Greps for the exchange's distinctive phrasing (`advance and collect`,
`regular email`, `payroll file feed`) return **nothing repo-wide**.

**D41 was filed anyway**, and that was right: the substance is fully carried by LA-20, LA-21, LA-22 and
LA-36, all of which were read in full and all of which hold. LA-22's own Status paragraph already
contained the sentence D41 formalises — *"the same card mechanism runs differently by benefit type."*
⚠️ **But the row carries an evidence-grade block recording that the origin rests on prompt assertion,
and that Daniel's acceptance is recorded nowhere in the repo.** Anything later citing D41's provenance
needs to know that.

**2. `plus_tier_build_plan.md`'s header drifted for a third time.** It read *"Revision 9 — Part 11
governs"* while **Part 12 (D40) already existed** — the exact failure its own ⚠️ 2026-08-04 correction
block was written to prevent, and which that block records having already happened to Parts 9 and 10.
⭐ **A file that documents its own drift mechanism drifted the same way again, one Part later.** Revision
10 absorbs Part 12 rather than assigning it a retrospective number, and a dated note records the miss.
The header now also states that **the precedence line, not the revision number, is the authority on
what governs** — three synchronised copies of that line are maintained in the file, and they were
correct throughout; only the prose header was wrong.

**3. D-95 was internally self-contradictory before this session.** Its ⭐ paragraph instructed
re-applying V095 **and V096** after any refresh — which presupposes both had been applied locally —
while its Status line said *"Not applied anywhere."* ⚠️ **The drift was visible inside the row itself**,
in two paragraphs eight lines apart, not only against the session 32 close-out. It survived being
written, reviewed and carried across two sessions.

---

## 6. Open questions

**1. The confirmation mechanism on D41's payroll-match side — file feed or periodic email.** Whether
confirmation of amounts actually withheld arrives as a **payroll file feed** or, in Daniel's phrasing,
a **regular email** was raised on the call and not settled. Both were live; neither was chosen.

⚠️ **Recorded as open prose inside D41 and deliberately *not* promoted to an O-number.** The
instruction was to record it as open, not to number it, and creating a numbered item nobody authorised
is its own small drift. **O44 is available** if Kevin wants it tracked in the O-series. It is a
mechanism question rather than a compliance one — the funding rule stands either way, and only the
evidence of withholding varies. **Settled by Kevin.**

**2. Does V095 warrant its own deployment row?** S34-B found `V095` appears **exactly once** in
`docs/deployment_backlog.md` — folded into D-95's ⭐ paragraph rather than carrying a row of its own,
while every other migration in the file's recent history has one. Not acted on; flagging only. It is
one row's worth of work and would make V095's state findable by searching for it.

---

## 7. Backlog and deployment items from this session

**None filed.** This session created no `T` row and no `D-NN` row. Both items were amendments to
existing artifacts — a new Part in an existing decision series, and a status correction to an existing
deployment row. **Standing rule S16-G is satisfied vacuously**: no T-number was reported, so none needed
writing.

**D-95 remains open** and is now correctly worded. Its instruction is unchanged: ship the WAR and the
migration together.

---

## 8. Next

⚠️ **Session 34 produced no code, and neither did session 33.** Two consecutive documentation sessions.
This project's deliverable is Claude Code build prompts — research and filing are means, and both
sessions were entirely means. **Naming it here rather than discovering it in three sessions' time.**
Neither session was wasted (D41 shapes the funding build, and D-95's correction removes a false claim
from the deployment record), but the next session should ship code.

⚠️ **`docs/swbd_ichra_build_plan.md` has nothing ready for a build session** — read, not assumed. Its
§3 build sequence, items 1 through 13, is **entirely `✅ Done 2026-07-31`**. Its one outstanding item is
**item 4, Kevin's admin-UI reference-data setup**, which that document explicitly classifies as *"a
configuration step rather than a precondition"* and *"not a scheduled build gate."* So the live build
front is not there — it is the Summit export track in `docs/analysis/project_backlog.md`, which is where
sessions 27 through 32 shipped.

**The strongest candidate is blocked on Kevin: T196** — the first live execution of the HRA Enrollment
emitter. It is HIGH, and it gates the whole enrollment chain by its own terms: *"nothing else in this
chain should be built on it until it has run once."* ⚠️ **Blocked three ways, all Kevin's**: a runtime
execution, deployment item **D-93** (`SUMMIT_IMPORT_TEMPLATES` needs an `enrollment:` entry, plus a
Tomcat restart), and a Summit-side HRA Enrollment template that must exist before the file can bind.
No Claude Code session can unblock any of the three.

**Recommended build, and it is not blocked: T209 — the zero-row guard on `writeDemographics` and
`writeHraEnrollment`.** HIGH, code-verified, and specified down to the shape: *"Do it as its own run,
with the same shape as file 2's guard — refuse, name the reason, no file."* ⭐ **The pattern is already
in the tree and already proven** — `1139fc6` shipped exactly that guard for file 2 after a 0-byte file
downloaded on proposal 140956. This run copies a proven fix into the two writers that were left out
because they sat outside S31-J's fence.

**Why it is the right one:** it needs **no schema, no config key, no runtime execution, no browser walk
and no admin-UI data entry** — nothing on Kevin's side at all. And the defect it closes is a silent one:
a zero-row Demographics file creates no participants and a zero-row enrollment file enrols nobody, both
upload cleanly, and the operator gets no signal whatsoever. The likely trigger is ordinary — generating
file 4 before the census is uploaded.

⚠️ **One caution to carry into that run, from T209's own row:** it means editing two **import-proven**
writers, which is a trade S30-A declined once already. Fence it tightly to the guard, change nothing
else in either writer, and it stays a safe run.

**Not recommended as next, but not to be lost: the three SWBD emails to Forrest.** Still unsent, still
the longest-standing open item on the project, still Kevin's alone. A build session does not touch them
and they should not disappear because one intervened.

⚠️ **T219 was session 33's recommended follow-on and is deliberately *not* recommended here.** Its
precondition is now met — D41 is filed, so the loose-versus-exact-payroll-match distinction is on record
before code is written against it. But T219 is a funding-source field **plus** a variable deduction
schedule landing in a **card-funding subsystem that does not yet exist**, as session 33's own SQL audit
noted. That is a subsystem design, a migration and a parser change, not a session's build. It should be
scoped deliberately rather than picked up because it was next on a list.

---

## 9. SQL close-out audit

**Session 34 produced no SQL.** Every item below verified against the repo as it now stands, not
asserted from memory:

- **No SQL was produced, run, or recommended** in any of the three sub-runs. S34-A and S34-B each
  carried an explicit no-SQL fence; both honoured it, and this run carries the same fence.
- **No `.sql` file was created, modified, or orphaned.** `git show --name-only d2b52e9` lists exactly
  two files, both `.md`: `docs/analysis/plus_tier_build_plan.md` and `docs/deployment_backlog.md`.
- **No migration was authored, applied, or modified.** `docs/migrations/` is untouched; nothing was
  added to `docs/analysis/migration_tracker.md` or `docs/schema_version_migration.sql`.
- **No schema change is implied by anything filed this session.** D41 says so in its own Reversal cost
  field — *"card configuration and funding schedule. No schema depends on which way it runs"* — and
  D-95's correction is wording on an existing row for an already-authored migration.
- **Current highest migration version: V096** (`docs/migrations/V096__summit_file_export.sql`), read by
  listing `docs/migrations/`. **73 `.sql` files in tree — 72 versioned migrations plus the
  non-versioned `seed_ndt125_questionnaire.sql`.** Unchanged by this session.
- **No schema described in docs but not scripted** was introduced. T219's funding-source field and
  variable schedule remain a backlog row against a subsystem that does not exist — planned, not
  described as built.

### Pending deployment

⚠️ **Two migrations are unapplied to production, both carried from earlier sessions and neither advanced
here. This is the live item.**

| Version | State |
|---|---|
| **V095** — `summit_plan_template_map` | ⚠️ On `beta_ssa` only. **Not on production.** No deployment row of its own — see §6.2. |
| **V096** — `summit_file_export` | ⚠️ Applied to `beta_ssa` 2026-09-08 15:53:48. **Not on production** — **D-95 open**, and now correctly worded. |

**Production remains at V094** (release `v0.94.00`). ⚠️ **The weekly refresh of `beta_ssa` from
production drops both V095 and V096 and every row in their tables** — re-apply both, in order, after any
refresh. D-95's instruction stands: **ship the WAR and the migration together.** A WAR without
`summit_file_export` downloads exports correctly and writes one ERROR line per export, forever.

⭐ **What changed here is the record, not the state.** D-95 no longer claims V096 is unapplied
everywhere; it names Production as the outstanding environment and records the local application with
its timestamp. The deployment position is exactly what it was at session start.

**Nothing from session 34 is pending deployment.** This session shipped documentation only.
