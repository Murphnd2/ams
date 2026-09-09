# Session 38 Close-Out — 2026-09-09

**Type:** Documentation only. No code, no schema, no SQL, no build, no deploy.

Session 38 was scoped to register Presidio excepted-benefit findings (TDI/SERFF filings, the
approved policy forms, and the original Forrest call) as legal assumptions, file a funding-model
decision, and correct the Presidio call record. Two of the run's instructions could not be carried
out as written — one because the target file does not exist, one because the scope fence forbids
the only file that actually matches the description of "the decisions register." Both are reported
below rather than worked around.

---

## Shipped

**Nothing was committed.** The working tree is left dirty for Kevin's review, per the run's git
restrictions (no `git add`, `git commit`, or any other mutation). Files modified or created:

- `docs/analysis/legal_assumptions.md` — amended LA-22, added LA-37 and LA-38
- `docs/analysis/project_backlog.md` — added T222–T225
- `docs/session_closeout_2026-09-09_session38.md` — this file

---

## In flight (uncommitted)

- `docs/analysis/legal_assumptions.md` — LA-22's Basis/Risk/Confirm-before/Status paragraphs each
  gained a 2026-09-09 addendum; the entry's structure and pre-existing text are otherwise
  unchanged.
- `docs/analysis/legal_assumptions.md` — LA-37 added (§125 eligibility of owner-buyers; more-than-2%
  S-corp shareholders, partners, sole proprietors, and §318-attributed family cannot participate in
  a cafeteria plan).
- `docs/analysis/legal_assumptions.md` — LA-38 added (Presidio's §V.H payer restriction; post-tax
  funding is the default, pre-tax gated on §125 eligibility and a written Presidio position on
  §V.H).
- `docs/analysis/project_backlog.md` — T222 (ask Presidio/Daniel five open questions), T223 (third
  card bucket: post-tax Presidio), T224 (entity-type/owner qualifying question in the sales
  script), T225 (SSA-drafted employee communication: excepted-benefit, not MEC) added after T221.

---

## Decisions made

**Resolved by session 38b (2026-09-09).** §6a originally closed with no `D-NN` row filed, because
the only file matching "the decisions register" (`docs/analysis/plus_tier_build_plan.md`) was
forbidden by this run's own scope fence. Session 38b confirmed the fence was the defect — a
follow-up run scoped explicitly to append one row to that file, leaving D40/D41 and everything
else in it untouched — and filed **D42** there: "the card carries three sub-accounts, not two: a
Presidio post-tax bucket joins D41's pair." It closes the same open question T223 already
described in prose: the card needs three benefit types (post-tax Presidio, pre-tax Presidio,
pre-tax off-exchange ACA), not two. T223's own prose record is left as filed, per session 38b's
scope fence.

---

## New assumptions

- **LA-37** — More-than-2% S-corp shareholders, partners, sole proprietors, and §318-attributed
  family cannot participate in a cafeteria plan. **Reversal cost:** none if caught at
  qualification; per-employee W-2/941 corrections if not.
- **LA-38** — Presidio's §V.H restricts who may pay premium; pre-tax §125 salary reduction likely
  defeats the ERISA voluntary-plan safe harbor, so post-tax is the default funding path and every
  Presidio bucket needs a member-name card. **Reversal cost:** low prospectively (funding
  character, payment instrument); high for any individual whose coverage lapses from a
  non-credited payment.

---

## Open questions raised (and who settles each)

Filed as T222–T225 in `docs/analysis/project_backlog.md`. All five sub-items of T222 settle with
**Daniel** (Presidio): product generation and condition subset issued to the pilot, whether the
10/17/2025 form revision is the approved text, the SD/ACC outlines of coverage, Presidio's written
position on §V.H, whether an excepted-benefit counsel memo exists on their side, and Indiana DOI
classification. T223–T225 are SSA-internal build/process items with no external dependency.

---

## Contradictions found

**1. The scope fence names a file it also forbids.** ⭐ **RESOLVED by session 38b (2026-09-09) — the
decisions register for this family of work is `docs/analysis/plus_tier_build_plan.md`, the row is
filed there as `D42`, and the session 38 prompt's scope fence was the defect, not the repo.**
Original finding retained below for provenance. §1 of the run's instructions defines "the
decisions register" as whatever file is found by grepping `docs/` for `D-01`, `D-02`, or `## D-`.
That search returns two independent `D-NN` sequences:

- `docs/deployment_backlog.md` — `D-01` through `D-95`, all deployment/infrastructure tasks
  (seed a constant, apply a migration, snapshot a VPS). Not forbidden by the scope fence.
- `docs/analysis/plus_tier_build_plan.md` — `D29` through `D41`, business/product decisions,
  including **D40** and **D41**, which are the two prior entries in exactly this family (Summit
  plan types, and "one card, two sub-accounts, two funding rules"). **Explicitly forbidden** by
  the same run's scope fence ("You may not touch: ... `docs/analysis/plus_tier_build_plan.md`").

The three-card-bucket decision this run was asked to file is a direct continuation of D40/D41's
subject matter, not a deployment task — so the file that actually matches "the decisions register"
for this content is the forbidden one. Writing a `D-NN` row into `deployment_backlog.md` instead
would misfile a product decision into an infrastructure-task log and create a second, unrelated
`D-41`-range collision between the two files' independent numbering. Rather than guess, no `D-NN`
row was written anywhere; the decision content survives as prose inside T223. This is worth Kevin's
attention because it will recur — any future Presidio/card-funding decision hits the same wall.

**2. No file exists whose first heading is a Presidio call record.** Searched: every file matching
`*presidio*` or `*call_record*`/`*call-record*` by filename (none found); every file containing
"Presidio" as text (`docs/business/swbd_premiumpath.md`, `docs/business/README.md`,
`docs/session_closeout_2026-09-08_session31.md`, `docs/analysis/project_backlog.md`,
`docs/analysis/plus_tier_build_plan.md`, `docs/migrations/V076__county_reference.sql`,
`docs/ichra_strategy.md`, session close-outs 33/34); and every file containing "Daniel Cruz," "call
record," or "call notes" (the same handful, re-surfaced). The closest candidate,
`docs/business/swbd_premiumpath.md`, opens with `# SWBD / PremiumPath` and reads as the engagement's
general reference doc — it is not, by its own first heading, a Presidio call record. Per the run's
explicit instruction, no such file was created. §6b (correcting the call record's "possibly a
transcription garble" note, and adding the SERFF table / form numbers / H21→H23I.003 finding) was
not performed anywhere.

**3. No filed document currently describes the card as having only two benefit types or frames
Presidio as §125-first**, as far as this session's searches went — `legal_assumptions.md`'s LA-20
already frames the ICHRA/§125 stack as two sub-accounts *for the ACA bucket specifically*, not as a
global two-bucket ceiling, so it does not contradict a third Presidio bucket; it simply predates
Presidio entirely. No correction was needed or made to LA-20.

---

## Next

**Kevin needs to resolve the two hard-stops before this content can be finished as specified:**
point him at either (a) telling this agent to write the `D-NN` row into
`docs/analysis/plus_tier_build_plan.md` despite the fence (i.e., the fence was overly broad for
this run), or (b) naming a different file as the actual decisions register, or (c) accepting T223's
prose-only record as sufficient. Separately, either point to the actual Presidio call record file
(if one exists under a name this session's searches missed) or confirm none exists yet, in which
case §6b's corrections belong in `docs/business/swbd_premiumpath.md` once that file is back in
scope. After either is resolved, T222's five questions to Daniel are the next real-world action —
they gate LA-22, LA-38, and T223 all at once.

---

## SQL close-out audit

**No SQL was produced, run, or recommended in this session.** No file under `docs/migrations/` was
created, modified, or read for any purpose beyond the version check below. Current highest
migration version, read from `docs/analysis/migration_tracker.md` (read-only): **V096**
(`V096__summit_file_export.sql`), consistent with `ls docs/migrations/`. Production is recorded
there as current at V096 as of 2026-09-09 (session 37) — this session made no change to that
tracker or to any other migration-status document, and confirms no orphaned `.sql` files were
created by this run.

---

## Compliance statement

1. **Files created or modified, by full path:**
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\docs\analysis\legal_assumptions.md`
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\docs\analysis\project_backlog.md`
   - `C:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\ams\docs\session_closeout_2026-09-09_session38.md`
2. **No git mutation commands were executed** — no `git add`, `git commit`, `git stash`, `git
   checkout`, `git restore`, `git tag`, or `git reset`. The preflight ran only `git
   rev-parse`, `git log -1`, `git status --short`, and `git pull --ff-only` (fast-forward, already
   up to date, no divergence).
3. **No file outside the §1 scope fence was touched.** The only files edited are the two explicitly
   named in the fence and this run's own close-out; the two files the fence located by search or
   forbade by name (the Presidio call record, `plus_tier_build_plan.md`) were read/searched-for
   only, never written.
4. **Numbers read and assigned:**
   - Highest pre-existing `LA-NN`: **LA-36**. Assigned: **LA-37**, **LA-38**.
   - Highest pre-existing `T-NNN` (in `project_backlog.md`): **T221**. Assigned: **T222, T223,
     T224, T225**.
   - Highest pre-existing `D-NN`: **D-95** in `docs/deployment_backlog.md`; **D41** in
     `docs/analysis/plus_tier_build_plan.md` (two independent sequences — see Contradiction 1). **No
     `D-NN` was assigned** — see Decisions made, above.
5. **Hard-stops / not-found anchors:**
   - §6a — no `D-NN` row filed; the only matching "decisions register" is forbidden by the scope
     fence (Contradiction 1).
   - §6b — no file found whose first heading is a Presidio call record; none created (Contradiction
     2). The call-record correction and the SERFF-table/form-number/H21→H23I.003 addition from §6b
     were therefore not performed anywhere.
6. **Places requiring interpretation rather than literal instruction-following:**
   - §1's location instructions for "the decisions register" produced a file the scope fence
     separately forbids; treated as a hard-stop on that sub-task rather than as authorization to
     either write to the forbidden file or to redirect the write to the differently-scoped
     `deployment_backlog.md`.
   - The prompt's own title, "Session 37," collided with an already-existing
     `docs/session_closeout_2026-09-09_session37.md` committed earlier the same day (`3687115`).
     This close-out is filed as **Session 38** instead, matching the existing directory's
     sequential numbering rather than the prompt's stated session number.
   - LA-22 and LA-37/LA-38's cross-references were built from the entries actually found (LA-24 for
     non-ALE, LA-21 for arrears loading) rather than assumed sight-unseen, per the run's own "read
     the real number" instruction.
