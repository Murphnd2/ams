# Session 14 close-out — 2026-08-04

**Branch:** `refactor/modernize-architecture` (trunk, committed directly)
**Model:** Sonnet throughout, except S14-B (Opus — the prompt named the model change as the
Opus trigger, not diff size: live, non-ICHRA, customer-facing code on a workflow current clients
use today).
**HEAD at preflight:** `7fb574b4d0eb7d98eb0cd0efa06bb5527115190d` — "docs: session S14E close-out
(T74 resolved -- GroupConversion ZIP intake)". `git pull --ff-only` fetched a new tag, `v0.88.05`,
alongside "Already up to date" for the branch itself — a clean fast-forward, not a hard stop.

Sub-runs this session: **S14-A** (T138), **S14-B** (T139, plus T140/T141 filed), **S14-C**
(census layout), **S14-D** (T74 state confirmation, read-only, produced no file), **S14-E** (T74
follow-on — GroupConversion ZIP intake), **S14-F** (this close-out). Full detail lives in
`docs/session_s14a_closeout.md`, `docs/session_s14b_closeout.md`, `docs/session_s14c_closeout.md`,
and `docs/session_s14e_closeout.md` — this document summarizes for a session that has not read
those, and cites them rather than repeating their reasoning. **S14-D produced no file**; its one
durable finding is captured below under Contradictions, since otherwise it exists only in
conversation and would be lost.

⚠️ **This run was ordered to run last, after the `/GroupConversion` production walk, so this
section records an outcome rather than predicting one. The walk happened** — 2026-08-04, after
`v0.88.05`. Its steps and findings are recorded throughout this document, sourced to the prompt
that commissioned this close-out (§2a), which is the only record of the walk — it produced no
close-out of its own, being a manual operator walk rather than a build session.

---

## Shipped and released

Four code commits shipped this session, across three sub-runs (S14-A, S14-B, S14-C, S14-E — S14-D
wrote no code). **Deployment verified by tag ancestry** (`git merge-base --is-ancestor <commit>
<tag>`), not assumed:

| Commit | Item | Ancestor of `v0.88.04`? | Ancestor of `v0.88.05`? |
|---|---|---|---|
| `e7c6220` | T138 — pre-selection provenance banner (S14-A) | **YES** | **YES** |
| `240cc4f` | T139 — null-safe ToDo loop, `ViewHome25` (S14-B) | **YES** | **YES** |
| `ea6efeb` | Census input layout reshape (S14-C, no T-number) | **NO** | **YES** |
| `95a81b1` | T74 follow-on — ZIP intake on `/GroupConversion` (S14-E) | **NO** | **YES** |

**This split matters and is easy to get wrong by assumption.** `v0.88.04`'s tag commit
(`020db201f9e0e99690c963cfdd99dea4eafd37e9`) **is** S14-B's own docs close-out commit — the
release was cut at the exact moment S14-B finished, before S14-C or S14-E existed. **`v0.88.04`
carries T138 and T139 only.** `v0.88.05`'s tag commit (`7fb574b4d0eb7d98eb0cd0efa06bb5527115190d`)
is S14-E's docs close-out commit — the trunk HEAD at the end of this session's build work — and
**carries everything: T138, T139, the census reshape, and T74's GroupConversion follow-on.**

Tag dates, read from `git log -1 --format=%ai <tag>`:

| Tag | Commit | Date |
|---|---|---|
| `v0.88.03` | `44c5826` | 2026-08-04 14:20:27 -0500 |
| `v0.88.04` | `020db20` | 2026-08-04 15:19:40 -0500 |
| `v0.88.05` | `7fb574b` | 2026-08-04 16:41:56 -0500 |

The production walk (§2a below) happened **after `v0.88.05`**, so all four commits above were
walkable, and the walk's steps confirm exactly that — census reshape rendering, ZIP precedence
resolving to Hopkins, T137's label, and the results-path banners all appeared in one session on
production.

---

## The `/GroupConversion` production walk — outcome

Walked 2026-08-04, after `v0.88.05`. Steps and observations, as recorded in the prompt that
commissioned this close-out:

1. ZIP `75482` entered, county dropdown left unselected, Compare clicked → resolved to **Hopkins
   County, TX**, dropdown then displaying it with T137's **"— test rates"** label. This is the
   precedence rule (`resolveZipPrecedence`, S14-E) confirmed on production — and the case that
   matters most, since a precedence bug here renders correctly and quotes another county's rates
   with full confidence, no visible error.
2. Census reshape (S14-C) confirmed rendering one row per line, labels intact.
3. Submission with no ages → *"Enter at least one census row."* With age 40 and no premium →
   *"Enter the current total monthly group premium."* Census parsing through the reshaped markup
   confirmed working.
4. Age 40, count 1, premium 900 / employer share 600 / contribution 500 → full comparison
   rendered: lowest bronze **$421.56**, monthly change **−$100.00**, all four disclosure banners
   present (agent-use-only, test-environment, illustration-not-a-quote, off-exchange-only).

---

## Verification status

Stated by name, per claim. `code-verified` and `runtime-verified` are different claims, and this
project has been burned before by conflating them.

- **T74 (`95a81b1`, S14-E) — `runtime-verified`.** Confirmed by walk step 1: ZIP precedence
  resolved a real ZIP to the correct county on production, with the dropdown updating to reflect
  it. Upgraded from `code-verified` (S14-E's own claim, since T111 blocked S14-E's local walk).
- **Census layout (`ea6efeb`, S14-C, no T-number) — `runtime-verified`.** Confirmed by walk step
  2 (rendering, labels intact) and step 3 (parsing through the reshaped markup, confirmed by the
  validation messages firing correctly on incomplete input). Upgraded from `code-verified`.
- **T138 (`e7c6220`, S14-A) — `runtime-verified` *in results context only*.** Walk step 4 confirms
  the test-environment banner rendered correctly during a full comparison — the underlying
  `sourceEnv`-driven banner mechanism T138 built works when exercised. **This does not fully
  close S14-A's own three unexercised assertions**: the walk's step 1 (ZIP resolving to Hopkins,
  dropdown updating) does not clearly establish that the *pre-selection-page* banner specifically
  — gated on `empty selectedCounty`, the actual new thing T138 added — was independently observed
  as its own distinct render before results appeared, as opposed to the page moving straight from
  empty form to full results in one submission. Recorded precisely rather than claimed in full;
  the pre-selection-specific scenario remains formally unconfirmed.
- **T139 (`240cc4f`, S14-B) — remains `code-verified`.** Per the prompt commissioning this
  close-out: *"T139's guard remains code-verified — it was not exercised."* The walk exercised
  `/GroupConversion` only; T139's guard sits in `ViewHome25`'s renewal-start path, which the walk
  did not touch.
- **T137 (`77fa57b`, S13-B) — one additional production data point, not recorded as a status
  change.** Walk step 1 directly observed T137's "— test rates" label rendering on the
  ZIP-resolved Hopkins option — a genuine confirmation of T137's own labeling mechanism, beyond
  session 13's own production observation (the dropdown listing four counties, walked after
  `v0.88.03`). **Not reflected as a backlog status edit** — the prompt commissioning this
  close-out named T74, the census layout, and T138 specifically as the upgrades to record, and did
  not name T137. Recorded here as an observation rather than silently omitted, without editing
  T137's row beyond that list.
- **S14-A/B/C/E's other individual claims (dropdown labels intact, banner-not-duplicated on
  results, `name`-attribute preservation, etc.)** — unaffected by this walk; still whatever each
  sub-run's own close-out states. Not restated here in full; see each close-out directly.

---

## Decisions made

Carried forward from the sub-run close-outs, with what each closed:

- **`/IchraZipLookup` skipped entirely; pure server-side ZIP resolution (S14-E).** Since `de0efe0`,
  the endpoint acquired a `priced` flag (`PRODUCTION_OK`-only, `IchraZipLookup.java:141-158`) that
  contradicts `/GroupConversion`'s own T137 fail-toward-labeling design (every warmed county stays
  selectable, staging-sourced ones just marked). Reusing it would have imported that contradiction.
  Closed by hard stop #2 firing and Kevin's ruling: server-side resolution only, no blur/JS layer,
  `IchraZipLookup.java` never touched. Source: `docs/session_s14e_closeout.md`, Decisions #1.
- **`illustration_log.zip_code` mirrors Illustration's W2 fix on `/GroupConversion` (S14-E).**
  `logIllustration` previously wrote `county.getRepresentativeZip()` unconditionally — the exact
  pattern `IllustrationServlet` itself moved away from (`IllustrationServlet.java:917-930`,
  documented inline as "W2": *"anyone auditing the log would conclude an agent typed a ZIP they
  never typed"*). Closed by hard stop #5 firing, two contradictory rounds of clarification, and a
  final narrow re-ask: the column now records the agent-typed ZIP when submitted, `null` otherwise.
  Source: `docs/session_s14e_closeout.md`, Decisions #2.
- **The census reshape (S14-C) declined Illustration's add/remove repeater apparatus.** Copied the
  *row shape* (stacked, one per line, small label over small input, plain Bootstrap utility
  classes) but not the add/remove buttons, the hidden template, or the zero-start JS. That
  apparatus implements Illustration's tier-1→tier-2 behavior, which has no GroupConversion analog
  — this page has no tier-1 fallback, and copying it would mean the page opens with zero rows
  needing clicks to reach six, a functional change to "the number of census rows offered"
  explicitly out of that build's scope. Source: `docs/session_s14c_closeout.md`, "What shape was
  copied."
- **The T139 guard is silent, not logging (S14-B).** Kevin's ruling after two hard stops fired
  (see below): null on the five bypass paths means "nothing was ever loaded to change," not an
  activity-not-found signal, so there is nothing to warn about. A WARN would have fired on every
  ordinary successful renewal, blank-renewal, and opportunity creation. Source:
  `docs/session_s14b_closeout.md`, Decisions #1, Hard stops table.

---

## New assumptions

Carried forward from the sub-runs, each with its reversal cost (full detail in the cited
close-out):

| ID | Assumption | Reversal cost | Source |
|---|---|---|---|
| S14A-1 | Pre-selection banner always says "STAGING," not the county's actual recorded `source_env` | Low — fine while only two values exist | `session_s14a_closeout.md` |
| S14A-2 | `availableCounties`/`stagingCountyFips` agree since both derive from one `summaries` read | Trivial | `session_s14a_closeout.md` |
| S14B-1 | `toDoList` null unambiguously means "never populated," not "populated then cleared" | Low | `session_s14b_closeout.md` |
| S14B-2 | Nothing else in the request depends on `toDoList` being iterated | Trivial | `session_s14b_closeout.md` |
| S14C-1 | Bootstrap's default block-stacking needs no wrapping container for one-row-per-line | Trivial | `session_s14c_closeout.md` |
| S14C-2 | Removing inline pixel-width styling is safe; `form-control-sm` governs sizing alone | Trivial | `session_s14c_closeout.md` |
| S14E-1 | An unpriced chooser candidate degrades to the existing "Select a valid county" message | Low | `session_s14e_closeout.md` |
| S14E-2 | `doGet`'s ZIP handling is safe to gate strictly on `zip` presence, not `countyFips` alone | Trivial | `session_s14e_closeout.md` |

No `LA-NN` filed by any sub-run this session — each closed with an explicit statement of why (data
already computed being labeled, a null guard on an internal loop, a layout/shape mirror, an intake
mechanism already shipped elsewhere).

---

## Open questions raised

Carried forward, not re-litigated here — see each close-out for full context:

1. Should the dropdown-level banner (T138) and the per-option markers (T137) ever collapse into
   one mechanism? *Settled by:* Kevin, if the two are ever felt redundant on screen.
2. Should the five `CurrentActivity`-bypassing servlets call `intializeActivity`, or should
   `toDoList` initialize to an empty list? *Settled by:* Kevin, when T140 is scheduled.
3. Does `GenerateProp25` reach `ViewHome25:151` with a null list? Undetermined — three entry JSPs,
   not traced. *Settled by:* tracing each for a preceding `VIEW_ACTIVITY`/`VIEW_CHECKLIST` dispatch.
4. Can `CurrentActivity` itself ever be null? T141 cannot be resolved without this. *Settled by:*
   whoever takes T141.
5. Does the reshaped census block actually read as less grid-like on screen? Unanswerable by any
   markup-only method. *Settled by:* the walk that just happened — see Verification status above;
   this is now closed in substance, though no sub-run close-out formally records it as answered.
6. Should the chooser eventually gain a T137-style provenance label, once the `PRODUCTION_OK`-only
   tension with T137's fail-toward-labeling is resolved some other way? *Settled by:* whoever next
   touches either page's chooser with that question in scope.

---

## New backlog rows filed

**Filed by S14-B, cited here (not re-edited this run):**

- **T140** — Five servlets populate `CurrentActivity` partially (`setActivity()` without
  `intializeActivity()`), leaving `toDoList` unset — the actual root cause behind T139's original
  filing. MED. `grep -rn "T140" docs/` returned 0 results before assignment.
- **T141** — `ViewHome25.processData` guards `getCurrentActivity()` for null in two places and
  dereferences it unguarded in two others — an internal contradiction, no observed failure. LOW.
  `grep -rn "T141" docs/` returned 0 results before assignment.

**Filed by this run (S14-F), per §2a — both grep-confirmed unused before assignment**
(`grep -rn "T142" docs/` and `grep -rn "T143" docs/` each returned 0 results; `T141` was the prior
high-water mark):

- **T142** — GroupConversion's Count placeholder renders as data. `groupConversion25.jsp:185-186`
  shows a grey `1` via `placeholder="1"`, never a real default — observed live during the walk:
  the operator could not tell his own entered value from the placeholder on his own screen. A row
  the agent intends to keep can submit blank and drop silently. **Second occurrence of this exact
  defect class on this project** — see Contradictions below. Proposed fix: a real `value="1"`
  default, matching Illustration's own fix exactly. Restyling the placeholder is explicitly
  rejected. Described, not built.
- **T143** — GroupConversion's results render below a full-height static input form. After
  Compare, the entire form (county, ZIP, plan year, six census rows, three premium fields, Compare
  button) retains full height above the results, pushing the answer below the fold. Proposed
  direction: collapse to a compact summary line with an edit affordance once results exist —
  layout only, no servlet change. Described, not built. **Ordered after T142** — correctness
  before layout, recorded in both rows' text.

---

## Contradictions found

1. **`docs/ichra_strategy.md`'s known-stale sections remain stale, flagged not fixed — per its
   own §1 exclusion from every sub-run this session.** Re-checked this run (read-only): §4's *"What
   a user can do today: **nothing**"* (line 121) is still present, and is now **more** false than
   when session 13 flagged it — the Illustration, `/GroupConversion` (including this session's ZIP
   intake and census reshape), the Market page, and `ICHRA_ILLUSTRATION` proposal sections have all
   shipped. Not edited — Kevin's file, per every relevant scope fence this session.
2. **The T74 backlog row's stale runtime-verification caveat, and its correction — S14-D's one
   durable finding, otherwise lost.** S14-D (read-only recon, no file produced) found that the T74
   row's *"⚠️ Not runtime-verified — V084/V085 have not been applied to any database"* clause was
   itself stale — `docs/analysis/migration_tracker.md:206-219` superseded it the same day
   (2026-08-01, end of session) with a behavioral confirmation (ZIP `75482`→Hopkins, `75009`→
   Collin/Denton chooser, `90210`→correct Texas-only miss on production). **S14-D also found that a
   session recommendation carried in conversation — "T74 ZIP intake as the next sale-motion build
   item" — was wrong: T74's core scope had already shipped 2026-08-01, three days before that
   recommendation was made.** Both findings were applied by S14-E, which corrected the T74 row's
   status and its stale caveat (`docs/session_s14e_closeout.md`, "Backlog rows edited," quotes the
   exact before/after text). Recorded here because S14-D produced no file of its own — without this
   line, both findings would exist only in conversation.
3. **This session's own hard-stop count drifted across its sub-runs, and this prompt's own framing
   undercounted it.** The prompt commissioning this close-out states *"Three fired across the
   session's sub-runs — S14-B's stop #3 and S14-E's stops #2 and #5."* **Checked against
   `docs/session_s14b_closeout.md`'s own Hard stops table: two stops fired in S14-B, not one — #3
   AND #4** (*"Hard stops: two fired (#3 and #4)"*, stated explicitly in that close-out's own
   Compliance section). **The correct count for this session is four hard stops fired, not three**:
   S14-B's #3 (null is the ordinary state, not an activity-not-found signal) and #4 (the throw site
   is shared with non-renewal paths that would log at volume), plus S14-E's #2 (`/IchraZipLookup`
   has illustration-specific behavior baked in) and #5 (mirroring Illustration would change
   `illustration_log` semantics). All four are recorded as **successful outcomes** below.
4. **T111's "consecutive session" count drifted internally across the session's own close-outs,
   and this prompt's "fourth consecutive session" framing is precise only under one reading.**
   Individual close-outs' running tallies disagree with each other: S14-A said "two consecutive"
   (S13-B, S14-A); S14-B said "third consecutive" (S13-B, S14-A, S14-B); S14-C also said **"third
   consecutive"** while listing S13-B, S14-A, *and* S14-B as the prior wall — an internal
   off-by-one, since that makes S14-C the fourth, not the third; S14-E said "fourth consecutive"
   while listing S13-B, S14-A, S14-B, and S14-E — **omitting S14-C from its own count entirely**.
   **Counting precisely: five sessions attempted a local authenticated walk and were blocked by
   T111 this workstream — S13-B, S14-A, S14-B, S14-C, S14-E** (S14-D did not attempt one; it was
   read-only recon). **This prompt's "fourth consecutive session" is exact only if counted within
   session 14's own five sub-runs alone (S14-A, B, C, E — four of five sub-runs attempted a local
   walk and were blocked; S14-D didn't attempt one), and undercounts by one if S13-B is included in
   the tally, as several of the individual close-outs themselves did.** Recorded as a pattern, per
   this prompt's own instruction, not a single incident: a plausible-sounding running count, copied
   forward from close-out to close-out without being recounted from the primary sources each time,
   drifted quietly across five documents. The same shape of error S14-B itself flagged ("a
   plausible summary asserted a step ahead of the evidence") recurred here at the level of a
   running tally rather than a single claim.
5. **`docs/swbd_ichra_build_plan.md` item 5 did not disagree with the Illustration's shipped
   state**, checked by S14-C specifically because the prompt commissioning that build raised the
   possibility. No discrepancy found — recorded in `docs/session_s14c_closeout.md`, Contradictions
   #2, cited here rather than re-derived.

---

## Next

**Recommend T142 before T143** — correctness before layout, as both rows' own text states. T142 is
a data-integrity defect (a value can silently fail to submit); T143 is a readability improvement.
Both are `/GroupConversion` JSP-only work and would ship as separate build runs.

**T136 (production HealthSherpa access) remains the single highest-leverage item outside this
session's scope**, per session 13's own "Next" section, carried forward unchanged — it unblocks
verification for T130 and the conditional Market page in addition to anything ICHRA-adjacent, with
no code change required on any of them once it lands.

**T111 is worth Kevin's time, but precisely for what it gates — see below, not as a blanket
blocker.** It is the reason T140 (a session-state defect) cannot be locally iterated on quickly. It
is not the reason anything shipped this session is `code-verified` rather than `runtime-verified`
today — the production walk closed that gap for three of this session's four items directly.

---

## ⚠️ T111 — what it gates, and what it does not

**Four of session 14's five sub-runs that attempted local verification were blocked by it**
(S14-A, S14-B, S14-C, S14-E; S14-D was read-only and never attempted a walk) — see Contradictions
#4 above for the precise count across the whole workstream (five, including S13-B).

**What T111 gates: local, automated, authenticated iteration** — the ability to build, deploy to
an isolated Tomcat, log in, and check a rendered page *without a human at a browser*, inside a
single Claude Code session. This is exactly what **T140** needs: a session-state defect
(`CurrentActivity` populated partially by five different servlets) that requires walking multiple
request sequences quickly to characterize, which a human production walk can do but far more
slowly and at the cost of Kevin's own time per iteration.

**What T111 does NOT gate: production verification by a human operator.** It never has. **T138 and
Illustration's ZIP intake were both runtime-verified against production while T111 was open** —
Illustration's ZIP intake on 2026-08-01 (`docs/analysis/migration_tracker.md:206-219`, ZIP
`75482`/`75009`/`90210`), and now T74, the census layout, and T138-in-results-context in this very
session's walk, all while T111 has sat open the entire time. **The framing "code-verified until
T111 is resolved" was used loosely across several of this session's close-outs and is not
accurate** — it describes what *automated local iteration* can achieve, not what *verification in
general* requires. This walk is the direct proof: it upgraded three items to `runtime-verified`
without T111 being touched at all.

---

## Carried forward, unchanged

⚠️ **Everything in this section is repeated from session 13's own close-out, which itself flagged
narrative-versus-repo status precisely — this run has no new information about any of it and did
not re-derive any of it independently.** Session 13's own record is cited as the authoritative
last-known state; nothing below is asserted as newly confirmed by this session's work, which was
confined to ICHRA/GroupConversion/`ViewHome25` build items and this close-out.

- **No reply from Forrest, per session 13's narrative** (`docs/session_closeout_2026-08-04_session13.md`,
  "Carried forward"). `docs/analysis/forrest_call_checklist.md`, referenced there as "remains
  prepared," was confirmed **not to exist** anywhere in the repository by that same session — not
  re-checked this run.
- **`docs/ichra_strategy.md` remains stale**, per session 13's flagged discrepancies — reconfirmed
  this run for §4 specifically (see Contradictions #1 above), not re-checked in full for every
  section session 13 named.
- **The HealthSherpa blocker table** (production allow-listing, onboarding representative,
  staging Basic Auth, BAA with Geozoning, webhook auth methods, BCBS TX/CHRISTUS policy-status
  timing) is unchanged, per session 13's own citation of `docs/session_closeout_2026-08-03_session12.md:186-191`
  — not independently re-verified this run.
- **`Proposal.dateCreated` renders blank** — session 13's own inference (a matched-but-null
  problem, not a missing-token one) stands; `SHOW CREATE TABLE proposal` on production remains the
  settling check, not run this session (outside scope — this session touched no production
  database).
- **The dark-on-dark heading cause remains unexplained** — the symptom is fixed, the print variant
  is explained, the original browser-render cause is not. Do not record "Bootstrap did it" as
  settled, per session 13's own explicit caution.
- **LA-17 still prohibits a session-based PSP-admin gate on the public proposal path**, by name, in
  writing. Not re-proposed this session; not relevant to any of this session's ICHRA/agent-facing
  work.

---

## `illustration_log.zip_code` — mixed semantics, recorded not fixed

**Now applies on both surfaces sharing the column.** On `/Illustration`: rows written before the
W2 fix (`IllustrationServlet.java:917-930`) carry the county's representative ZIP; rows after carry
the agent-typed ZIP or `null`, with nothing in the row itself distinguishing the two eras. **This
session's S14-E build introduces the identical split on `/GroupConversion`**: every `CONVERSION`-
mode row written before `95a81b1` carries `county.getRepresentativeZip()`; every row after carries
the agent-typed ZIP or `null`. **Not fixed. Not filed as a defect.** Recorded per S14-E's own
explicit instruction (`docs/session_s14e_closeout.md`, Open questions #1) so that whoever queries
this column for the **D-83** county-list decision knows pre-fix rows on either surface are not
agent input.

---

## SQL close-out audit

**Session 14 produced no SQL** — across S14-A, S14-B, S14-C, S14-D, S14-E, and this close-out.
Stated explicitly rather than the section being omitted.

- **SQL statements produced:** none, in any sub-run.
- **SQL statements run:** none against any environment, in any sub-run. S14-A, S14-B, S14-C, and
  S14-E each state explicitly in their own SQL audits that no `SELECT` or any other statement was
  run — every question each sub-run raised was answered from source, `grep`, `git`, and build/log
  output. S14-D (read-only recon) likewise ran no SQL. This close-out (S14-F) ran none either.
- **Orphaned `.sql` files:** none. `git diff --name-only e2aa9720d7b2c0e58a83afb576add2c4442a065a..HEAD`
  — spanning all of this session's code and doc commits, from session 13's own close-out hash
  through the end of S14-E — contains **zero `.sql` paths**, confirmed by running the command this
  run. The full file list touched this session: `docs/analysis/project_backlog.md`, four sub-run
  close-outs (`session_s14a/b/c/e_closeout.md`), `ViewHome25.java`, `GroupConversionServlet.java`,
  `groupConversion25.jsp` — eight paths, none of them `.sql`.
- **Current highest migration version — read from `docs/migrations/` this run, not recalled:**
  **`V088__proposal_ichra_intake_contribution.sql`.** Unchanged by any part of session 14. The
  crosswalk (V084/V085, T74's data layer) was already applied on Production before this session
  began — see the T74 row's corrected caveat, above.
- **Pending deployment:** as of this close-out, everything through `95a81b1` (T74/S14-E) is
  already released in `v0.88.05` — nothing from S14-A through S14-E is unreleased. This close-out
  and its accompanying backlog edits are documentation only and carry no deployable artifact.
- **Schema described but not scripted:** one — **a dedicated column for the agent-typed ZIP**,
  raised during S14-E's clarification rounds as an alternative to overloading `zip_code`'s existing
  meaning. **Explicitly described and deliberately not built**, once the "mirror W2" decision was
  made instead (see Decisions, above, and `docs/session_s14e_closeout.md`, Decisions #2) — the
  premise for a dedicated column (Illustration and GroupConversion disagreeing on what the column
  means) did not materialize once both surfaces were aligned to the same convention.

---

## Compliance statement

**Scope fence, restated:** permitted to write only this close-out
(`docs/session_closeout_2026-08-04_session14.md`, new) and status-field updates in
`docs/analysis/project_backlog.md` for rows this session closed or whose verification level
changed, plus the two new rows specified in §2a (T142, T143). **No source file, no JSP, no `.sql`
file, and no existing close-out (`session_s14a_closeout.md` through `session_s14e_closeout.md`)
was opened for editing — each was read only.** `docs/ichra_strategy.md` and
`docs/swbd_ichra_build_plan.md` were read (the former to reconfirm staleness, the latter not
reopened at all this run — S14-C's own reading stands) and **neither was edited.** Confirmed
nothing outside this set was written — see the git diff in the commit below.

**Every hash cited in this document was read from `git log` during this run**, via
`git log -1 --format=%H`, `git log --oneline <range>`, and `git merge-base --is-ancestor` — none
carried from the prompt's narrative, none a placeholder. This close-out's own commit hash is
recorded in a follow-up commit per the standing convention, read from `git log` after the push,
not written in advance.

**Per-claim table, repo-derived versus narrative-derived, for everything load-bearing:**

| Claim | Source |
|---|---|
| All four commit hashes and their tag ancestry | **Repo** — `git log`, `git merge-base --is-ancestor`, this run |
| `v0.88.04`/`v0.88.05` composition and tag dates | **Repo** — this run |
| The production walk's four steps and their outcomes | **This prompt's §2a narrative** — no independent record of the walk exists in the repo; not re-derivable |
| T74/census-layout/T138-in-results verification upgrades | **This prompt's explicit instruction**, applied to backlog edits this run |
| T137's additional "test rates" observation | **This prompt's §2a narrative**, cross-referenced against the T137 row's own prior (session 13) production observation, which **is** repo-derived |
| Every sub-run's Decisions/Assumptions/Verification claim cited above | **Repo** — each sub-run's own close-out, itself a mix of repo-derived code facts and, where stated, its own narrative flags |
| S14-D's two findings (stale caveat, wrong recommendation) | **This prompt's §2 narrative**, cross-checked this run against `docs/analysis/migration_tracker.md:206-219` (repo) and against `docs/session_s14e_closeout.md`'s own "Backlog rows edited" section (repo, which applied S14-D's finding) |
| The hard-stop-count correction (four, not three) | **Repo** — `docs/session_s14b_closeout.md`'s own Hard stops table, re-read this run |
| The T111 consecutive-session-count correction (five, not three or four, depending on scope) | **Repo** — each sub-run close-out's own stated count, cross-tabulated this run |
| `docs/ichra_strategy.md` §4 still reading "nothing" | **Repo** — re-read this run |
| Carried-forward section (Forrest, HealthSherpa blockers, `dateCreated`, dark-on-dark heading, LA-17) | **Narrative, via session 13's own close-out** — not independently re-derived this run, explicitly flagged as such above |

**No forbidden git operation was run**: no `git add -A`, no `git add .`, no `stash`, `checkout`,
`restore`, `reset`, and no local tag. Staging was by explicit named path.

**Whether any hard stop fired in this run: no.** S14-F is documentation-only with no hard-stop
list of its own beyond the preflight gate, which passed cleanly (correct branch, clean tree, clean
fast-forward pull). **Four hard stops fired across this session's sub-runs — corrected from this
prompt's stated three, per Contradictions #3 above — and all four are recorded here as successful
outcomes, each catching something real before it shipped:**

| Sub-run | Stop | What it caught |
|---|---|---|
| S14-B | #3 — null is the ordinary state, not an activity-not-found signal | Caught a filing whose stated mechanism (`EntityLookup.getActivityById` failing to find a match) was wrong — the real cause was five servlets bypassing `intializeActivity` entirely, filed as T140 |
| S14-B | #4 — the throw site is shared with non-renewal paths that would log at volume | Caught what would have been a WARN firing on every ordinary successful renewal, blank-renewal, and opportunity creation, had the original guard-and-warn instruction been followed literally |
| S14-E | #2 — `/IchraZipLookup` has illustration-specific behavior baked in | Caught a response-shape assumption (`priced`, `PRODUCTION_OK`-only) that would have imported a direct contradiction with `/GroupConversion`'s own T137 design, had the endpoint been reused as originally contemplated |
| S14-E | #5 — mirroring Illustration would change `illustration_log` semantics | Caught a production-logging semantics change that would otherwise have shipped unexamined — recording an agent-typed ZIP instead of a derived one is a data-meaning change to a table other code and future queries depend on, reserved for Kevin's explicit ruling rather than decided unilaterally |

---

**This close-out's own commit hash**, read from `git log` after push, recorded per the standing
convention: `a3e4eb292a643543a7ffac50553403f84bbd43f3` — `docs: session 14 close-out (T74/T138
upgraded runtime-verified; T142/T143 filed)`.
