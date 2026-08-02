# Session 7 Close-Out — 2026-08-02

**Branch:** `refactor/modernize-architecture`
**Session commit range:** `a991a78..5f5a32e` (session 7 proper) — see the note under Shipped on why this
is not `cabbe88..5f5a32e`, which is what the originating prompt used as the range.
**This document's own commit:** not hardcoded here — a commit cannot state its own hash without becoming
a different commit. Check `git log --oneline -1` on this file's commit.

**Read this first, in order:** this file → `docs/ichra_strategy.md` (note two stale lines flagged below) →
`docs/analysis/project_backlog.md` T103–T111 → `docs/analysis/local_render_verification.md`.

**The one fact that matters most:** the illustration surface produced **seven consecutive defects that
were declared resolved from code reading and failed their first browser walk.** Four of the seven were
plain markup and would have been caught by a local HTML fetch; three needed a human eye on layout and
CSS. This session built and proved the local-fetch half of that loop (S7-F/S7-G), and it caught a real
production defect — outside ICHRA, affecting every AMS user — on its first use.

---

## 1. Shipped

### Session boundary note — read before trusting the release table

The originating prompt's table starts the session at `cabbe88`. **`git log` shows three more commits
after `cabbe88` and before session 7's first prompt** — `b6da9e4`, `0ade70e`, `787a960` — all three are
session 6's own close-out corrections (doc-only: `docs/session_closeout_2026-08-01_session6.md`,
`docs/analysis/migration_tracker.md`, `docs/claude_memory.md`). None touches application code. Session 7
proper begins at `a991a78` (S7-A). This matters for one reason: `787a960`, the last of the three, records
**v0.85.06 as "built-and-pushed but not deployed," with production still on v0.85.05** as session 6
closed. Per `CLAUDE.md`, local `git tag` is stale by design and releases are cut by hand in the GitHub
web UI, so no commit in this log can be pointed to as "the v0.85.06 release." The prompt's "deployed at
session open" is therefore correct as written — it names a deploy action Kevin took **between** sessions,
not a commit in this range — but it is worth stating plainly rather than implying `cabbe88` itself was
the release point.

### Releases, as reported and reconciled against `git log`

Every hash below was checked with `git show -s --format="%H %s"` against the actual log. All five exist
and all five commit messages match what the prompt claimed for them.

| Release | Commit (verified) | Message (verified) | Carried |
|---|---|---|---|
| `v0.85.06` | `cabbe88` | *"Session 6 prompt K2 close-out: appended"* | Prior session's work — see the boundary note above; this is a close-out doc commit, not the code commit itself. Deployed at session-7 open (between-session action, no matching commit) |
| `v0.85.07` | `a991a78` | *"S7-A: stop Bootstrap .d-flex !important from unhiding the age-band template"* | S7-A only |
| `v0.85.08` | `3efd880` | *"backlog: record T103 and prompt-L (T108)"* | Everything since `v0.85.07`: `b808175` (T107/T44/T65), `5b768ae` (T104), `1fe810b` (T103 first cut), `3e8e245` (prompt L / hub collapse, S7-C), `3efd880` itself |
| `v0.85.09` | `4abb989` | *"backlog: T103 second cut -- the S7-C fix was itself a regression"* | `d530d53` (T103 second cut, explicit landing flag, S7-D) + `4abb989` |
| `v0.85.10` | `5f5a32e` | *"backlog: T110 fixed, locally verified 2026-08-01 (S7-G)"* | `fe0c684` (T106), `53c21ed` (T105), `2f08a33` (T109 — see numbering note below), `caa1bed` (T107 residue), `26937e8` (backlog: S7-E), `40433d6` (S7-F), `3a25690` (T110 fix), `5f5a32e` |

**No migration was produced this entire session.** Verified: `git diff --stat cabbe88..5f5a32e -- '*.sql'`
returns nothing, and `docs/migrations/` topped out at **V085** at `cabbe88` and still does at `5f5a32e` —
confirmed with `git show cabbe88:docs/migrations/` and a current `ls`. Full accounting in §8.

### Full commit list, in order (`a991a78..5f5a32e`)

```
a991a78 S7-A: stop Bootstrap .d-flex !important from unhiding the age-band template
b808175 T107/T44/T65: the ON-EXCHANGE LCSP column is genuinely on-exchange
5b768ae T104: fix singular/plural on the eligible-employee footnotes
1fe810b T103: don't error a hub-card landing that carries no age parameters
3e8e245 prompt L: collapse the hub's three illustration cards into one
3efd880 backlog: record T103 and prompt-L (T108)
d530d53 T103 second cut: an explicit landing flag, not an inferred one
4abb989 backlog: T103 second cut -- the S7-C fix was itself a regression
fe0c684 T106: relabel the outlay card as a ceiling, not a spend
53c21ed T105: put the slider's effect where the control is
2f08a33 T108: a flip point past the track max is now an edge marker, not silence
caa1bed T107 residue: name the rail and statistic in the affordability disclaimer
26937e8 backlog: record the S7-E illustration display batch
40433d6 S7-F: establish local render verification; log login-500 defect (T110/T111)
3a25690 T110: null-guard AuthDAO.validUserName against an unknown username
5f5a32e backlog: T110 fixed, locally verified 2026-08-01 (S7-G)
```

Application files touched across the whole session: **2 Java** (`IllustrationServlet.java`,
`AuthDAO.java`), **2 JSP** (`illustration25.jsp`, `ichraHome25.jsp`). No entity, no model, no schema file.

### The walks — runtime results, recorded nowhere else but here

**These are Kevin's own browser observations against deployed releases.** They are not something this
close-out re-derives from the repository — a walk verifies a running build, and the point of recording
them here is that they exist in no other file. Reported as given.

**Tier block, T1–T10, on `v0.85.06`/`v0.85.07`:** T1 pass · T2 pass · T3 pass · T4 pass · T5 pass ·
T6 pass · **T7 FAIL → fixed → pass** · T8 pass · T9 pass · T10 pass.

- **T6 pass closes K3-b** — one band no longer renders two; headcount correct at 5 lives across two
  bands after removing the first of three.
- **T9 pass closes K3-a** — pressing Illustrate twice changes nothing.
- **T65 closed by arithmetic**: flip = LCSP − $132.47 in every row, and $15,960 ÷ 12 × 0.0996 = $132.47.
  The affordability constants resolve to their LA-14 values on production.

**Hub block, on `v0.85.08`/`v0.85.09`:** H1 pass (four cards, no step framing) · H2 pass (bare form) ·
**H3 FAIL on `v0.85.08`** — the T103 first cut replaced the error banner with a *worse* empty-result
state · **H3 pass on `v0.85.09`** · H4 pass (blank `age1` still errors, so the flag keys on absence, not
blankness).

**Display batch, on `v0.85.10`:** D1 pass — slider card figures match the table exactly ($671.18 /
$2,000.00), edge marker reads *"age 55 beyond track ($1,098.33)"* · D2 pass — dragging moves both figures
live and flips the age-28 verdict at its mark.

**Login, on `v0.85.10`:** P1 pass — an unknown username returns *"Invalid username or password,"* not
500.

---

## 2. In flight

**Nothing.** Confirmed with `git status --short` at session close: clean tree. Every change this session
landed in a commit; nothing is staged, modified, or untracked.

---

## 3. Decisions made

1. **T106 — option A, chosen by Kevin.** The employer outlay is a **ceiling**, not a spend. Verified
   against `fe0c684`'s own commit message and the fuller `project_backlog.md` T106 entry:
   - **Option A (adopted):** keep the figure — `employerOutlay` is the correct maximum regardless of
     which plans employees choose — and fix the words. Relabelled **"Maximum Employer Monthly
     Commitment"**; the caption now states the reimburse-up-to-actual mechanism and points to
     Net/Employee above. The underlying value is untouched — label and caption only.
   - **Option B (rejected):** compute actual outlay at the bronze floor (~$1,875.68 in the walked
     example). Rejected because it assumes every employee buys the cheapest bronze plan, which
     understates real cost.
   - **Option C (deferred, not rejected):** show both ceiling and floor as a range. Recorded as a small
     addition on top of A, not built this session. Carried forward — see §5.

2. **Local render verification becomes part of the loop (S7-F).** Claude Code can build, deploy to an
   isolated local Tomcat on port 8089, authenticate, fetch a rendered page, and assert on markup —
   **before** Kevin cuts a release. Proven end to end in S7-F, then used for real in S7-G on its first
   opportunity. One manual prerequisite remains open — see §5.

3. **T108 numbering collision — record the convention, not just the fix.** `T108` was already assigned
   (`3efd880`, the prompt-L hub restructure) when a separate prompt's commit (`2f08a33`) also self-labels
   itself `T108` in its own commit message. The collision was caught and the backlog entry for the
   second item was written as **T109**, with an explicit note inside it flagging the discrepancy between
   the commit message and the assigned number (verified: `project_backlog.md` line for T109 reads
   *"⚠️ Numbered T109, not the T108 this item shipped under in its originating prompt..."*). **Root
   cause, as reported:** the claude.ai side assigned T103–T109 without reading the backlog file.
   **Convention going forward: Claude Code assigns backlog numbers**, since it is the side that can
   actually read `project_backlog.md` before writing to it.

---

## 4. New assumptions

**None written this session.** Checked directly: `git log --oneline a991a78..5f5a32e --
docs/analysis/legal_assumptions.md` returns nothing — the file was not touched in session 7's range.
Its last commit is `e90515a` (T65, prior session), and its most recent content addition (LA-01 through
LA-12/13) predates this session entirely. **This confirms the prompt's premise: S7-B's Branch 1 never
fired**, so no `LA-NN` entry was warranted or written.

No technical (non-`LA`) assumptions of the reversal-cost-tracked kind were introduced either — this
session's work was defect fixes and one relabel, none of which rests on an unconfirmed legal or
compliance reading.

---

## 5. Open questions raised

**Carried forward, not settled this session:**

- **Kevin's two one-time prerequisites for authenticated local verification** (notes, not numbered work):
  - A local **PSP Admin (role 5, `is_active=1`)** test account, credentials stored in
    `C:\ssa\ssa.properties` — outside the repo. `IchraAccessResolver.isAvailable` short-circuits on
    `isPspAdmin`, so no `agency.ichra_enabled` flip is needed.
  - A decision on the **Hopkins fixture** — S7-F recommends a hand-built local fixture for
    `rating_area_rate_cache` over configuring a live HealthSherpa key on the dev workstation, since
    render-verification doesn't need real rates, only a populated result state. Not built; Kevin's call.

- **The three SWBD asks remain unsent**, and two of them gate top-ranked builds per `ichra_strategy.md`
  §2 and §10: **O22** (book profile — counties, group-size distribution, carriers, renewal-date
  distribution, producing-agent count) and **O24** (will SWBD share its group book). `/GroupConversion`
  has been built, corrected twice, and never fed real data because the ask was never made. **This is the
  oldest unresolved item in the project**, and nothing in this session touched it — carried forward
  prominently, as instructed, not buried under the release table.

- **T106 option C** — whether the employer eventually wants ceiling and floor shown as a range. Deferred,
  not rejected; a small addition on top of the shipped option A.

---

## 6. Contradictions found

Every item the originating prompt asked to be checked was checked directly against the repository.

- **`CLAUDE.md` is wrong about the WAR name.** Line 10 claims *"final WAR named `ROOT.war`."*
  `pom.xml` has no `finalName` element (`grep -n finalName pom.xml` returns nothing) — the artifact is
  always `ams-1.0.0-SNAPSHOT.war`; the release-time rename to `ROOT.war` is a deploy step, not a build
  output. `MEMORY.md` already had this right.

- **`ichra_strategy.md` is stale in two places, confirmed by line number.** §3 row 3 (line 84) still
  reads *"Cached LCSP is off-exchange only (T44); ICHRA affordability needs the on-exchange LCSP... 2
  calls to fix T44."* §9 step 4 (line 267) still lists *"T44 — on-exchange LCSP"* as a future step gated
  on credential + counsel. **T44 shipped 2026-07-31 in `ba023bd` (`ICHRA item 8 (T44): on-exchange LCSP
  and benchmark silver`) with migration V078** — confirmed `ba023bd` is an ancestor of HEAD and predates
  this session. That file is Kevin's and explicitly out of this run's scope; flagged, not edited.

- **`CLAUDE.md`'s Open Question #15 no longer holds.** It claims *"literal credentials in a tracked
  file"* for `persistence-local.xml`. Confirmed current content: the file carries only placeholders —
  `${local.db.url}`, `${local.db.user}`, `${local.db.password}` — resolved at build time from
  `C:\ssa\ssa.properties`, which lives outside the repository. No literal credential is tracked.

- **Local `schema_info` was a stub view returning literal `1`,** not a version string, until S7-F applied
  V074–V085 and it self-healed (every migration from V055 on redefines the view). **`SELECT MAX(version)
  FROM schema_version` is the reliable local check**, not the view — recorded in
  `local_render_verification.md`.

- **`local_render_verification.md` needs three corrections, reported in S7-G and confirmed still
  unapplied.** `git log --oneline -- docs/analysis/local_render_verification.md` shows a single commit
  (`40433d6`, its creation) — nothing since. The three: a missing redeploy step for "the WAR changed,
  reuse the base"; the `transaction-type` check documented as distinguishing `-P local` from `-P server`
  is a **false signal** (both profiles' `persistence-*.xml` declare `RESOURCE_LOCAL` as the EclipseLink
  attribute — only `<non-jta-data-source>` value differs, and S7-G caught this mid-run when it briefly
  mis-read a correctly-built server-profile WAR as suspect); and an undocumented but now-established
  secret-safe pattern for fetching a real local username for comparison probes. **Recorded as pending
  work here; not applied in this run**, per the scope fence.

- **One more, not in the prompt's list — found while auditing `migration_tracker.md` for §8.**
  The tracker's Production column (confirmed by header: `Version | Description | beta_ssa (work) |
  beta_ssa (home) | dev_ssa | Production | Demo PSP | BPO | Master`) correctly shows **✅ for V074–V085**.
  But the **"beta_ssa (work)" and "beta_ssa (home)" columns still show ⬜ for V074–V085**, even though
  S7-F applied all twelve to the local workstation database on 2026-08-01 — the first time any local
  database had received them. Not fixed here (`migration_tracker.md` is outside this run's edit scope,
  and the tracker's own convention ties local-column updates to a different cadence than production
  ones), but flagged for whoever next touches that file.

- **File-location note on this document itself.** Every prior session close-out lives directly under
  `docs/` (`docs/session_closeout_2026-07-31_session2.md` through
  `docs/session_closeout_2026-08-01_session6.md`) — five for five. This session's prompt explicitly
  directed `docs/analysis/session_closeout_2026-08-02_session7.md`. Followed as instructed; flagged in
  case the directory change was not intentional, since it breaks the established pattern for anyone
  globbing `docs/session_closeout_*`.

---

## 7. Next

**Build sequence item 6 — illustration → LOS-scoped proposal section.** The ⭐ step where the
illustration stops being a calculator and becomes part of a sellable document.

**Why this and not something else:** the loop this session proved (S7-F/S7-G) has now paid for itself
once on a defect outside ICHRA entirely. The illustration surface itself has been walked clean through
seven rounds of defects — tier block, hub block, display batch, and now login. The next highest-value
step on the build sequence is turning that verified illustration into proposal content, which is exactly
where the sales pipeline converts agent utility into a signed deal.

**Phase A is required before any build**, because this is the first ICHRA work in a while that touches
live, non-ICHRA, customer-facing code:
- `ViewProposal` is live customer-facing code — every existing PSP's proposals render through it today.
- `ProposalSection.sectionType` is a free `VARCHAR(20)` dispatched by a JSP `<c:choose>` **with no
  default branch** — an unmatched type renders silently, not with an error. Any new section type has to
  be threaded through that dispatch correctly the first time, because a mistake here is invisible until
  someone notices a blank spot on a real proposal.

**One more thing to establish before item 6 is specified, not after:** a **"Use This in a Proposal"**
link already renders on the illustration result today — confirmed present in `illustration25.jsp`
(`<i class="bi bi-file-earmark-plus me-1"></i>Use This in a Proposal`, wired to `ProposalBuilder` via
`<c:url value="ProposalBuilder">`). What that link currently does, and how far short it falls of a real
LOS-scoped section, is the actual starting point for item 6 — not a blank-page design.

**Model recommendation:** Phase A on **Opus**, not a Sonnet build. The reasoning above — "first ICHRA
work that touches live non-ICHRA code," "no default branch," "silent on an unmatched type" — is exactly
the shape of judgment call this project's own convention reserves for the more careful model, and Phase
A's job is entirely judgment: map what exists, find where a new section type would break something
quietly, and specify before anyone writes a line of proposal-facing code.

---

## 8. The SQL close-out audit

**No new SQL was authored anywhere this session.** Verified two ways:

1. `git diff --stat cabbe88..5f5a32e -- '*.sql'` — **empty.** No `.sql` file appears in any commit across
   the full session range, including the three session-6 close-out commits before `a991a78`.
2. `docs/migrations/` — highest version at `cabbe88` (`git show cabbe88:docs/migrations/`) and at
   `5f5a32e` (current `ls`) is identically **V085**. No file added, removed, or renamed in that
   directory this session.

**What ran was execution, not authorship — S7-F only.** Twelve **pre-existing, already-versioned**
migrations (V074 through V085, all committed in prior sessions) were run for the first time against the
**local** development database, which had drifted to V073. Backed up first (`mysqldump`,
`--single-transaction`, to a path outside the repo, verified non-empty and cleanly terminated), then
applied in order; all twelve succeeded with no destructive statement in any of them (verb census:
`CREATE TABLE` ×5, `ALTER TABLE` ×3 — all `ADD COLUMN` — `INSERT IGNORE`/`INSERT INTO`,
`CREATE OR REPLACE VIEW`; the only `DROP`/`DELETE` strings anywhere in the twelve files are inside
rollback **comments**). Alongside that, S7-F and S7-G together ran a large number of **read-only**
queries locally — `SELECT`, `SHOW TABLES`, `SHOW COLUMNS`, `information_schema` lookups — none of which
wrote anything and none of which is a candidate for a migration.

**Current highest version:** **V085**, confirmed identically in `docs/migrations/`,
`docs/analysis/migration_tracker.md`'s "Current Highest Version" line, and (after S7-F's local run) the
local database's own `schema_version` table.

**Pending deployment:** nothing new from this session — no migration was written, so nothing is queued.
Per `migration_tracker.md`'s own reconciliation notes (dated 2026-08-01, predating and unrelated to this
session's work), **V074 through V085 are all recorded ✅ on Production** — V074–V076 from direct
deployment-log evidence, V077/V078/V080/V082 from behavioral evidence (a live incident trace), and
V079/V081/V083 from a deployment-record reconciliation. V084/V085 are the two most recently added rows
and read ✅ Production in the same table. **The one gap this session's own audit surfaced** (see §6): the
tracker's local-workstation columns for V074–V085 do not yet reflect that S7-F just applied them there —
a doc gap, not a deployment gap.

**Schema described but not scripted:** none. No prompt this session proposed a schema change of any
kind — the two Java files and two JSPs touched this session are entirely presentation and control-flow
fixes (a null guard, a landing-state flag, a label change, slider-card figure duplication, an edge-marker
render path). Confirmed by file-type census: `git diff --stat cabbe88..5f5a32e --name-only`, grouped by
extension, returns `2 java · 2 jsp · 5 md` — zero `.sql`, zero entity/model files.

**Orphaned `.sql` file, pre-existing and unchanged:** `docs/migrations/seed_ndt125_questionnaire.sql`
sits outside the numbered sequence, already tracked as **T38** (`📋 Planned`, unrelated to this session).
Confirmed still present and still the only non-`V*` file in `docs/migrations/`; not touched.

---

## Related

- `docs/analysis/project_backlog.md` — T103–T111 (this session's numbered items)
- `docs/analysis/local_render_verification.md` — the recipe S7-F wrote and S7-G exercised; three
  corrections pending, see §6
- `docs/analysis/migration_tracker.md` — authoritative migration state; one doc gap noted in §6
- `docs/ichra_strategy.md` — two stale lines noted in §6, not corrected here (Kevin's file)
- `docs/session_closeout_2026-08-01_session6.md` — prior session's close-out; the three commits between
  `cabbe88` and `a991a78` belong to it, not to this session
