# S14-E close-out — T74 follow-on: ZIP intake on `/GroupConversion`

**Branch:** `refactor/modernize-architecture` (trunk, committed directly)
**Baseline HEAD (read from `git log` at preflight, not carried from the prompt):**
`2b785d5f06a3d36b6fc3d018fb2e2ba38e04d448` — "docs: session S14C close-out (GroupConversion census input layout)"

---

## Shipped

- `95a81b155db429df5b65df26f07d8e72cc9ef182` — fix: T74 follow-on — ZIP intake on
  GroupConversion (S14-E). `GroupConversionServlet.java` (+106/−2), `groupConversion25.jsp`
  (+64/−0). Only the two permitted files, confirmed by `git status`/`git diff --name-only`
  before commit.
- Docs commit (this file, plus the two T74 row edits) — its own hash cannot appear in its own
  content; read with `git log -1` after commit, reported in the session summary instead.

---

## The precedence rule implemented

*A present, non-blank `zip` parameter is always resolved and takes precedence over a stale
selection — agreement with the passed-in county (or none passed in) plus a unique resolution
proceeds on that county; disagreement with a unique resolution silently replaces the selection;
an ambiguous resolution renders the chooser and computes nothing; an empty resolution renders
no-match with no fallback to any prior selection; only a blank `zip` lets the existing
`countyFips` win outright.*

**Source:** `IllustrationServlet.java:161-220` (rule at `:178-182`, implemented `:184-220`), read
fresh this run and confirmed unchanged since `de0efe0` — the only later addition is an unrelated
`pricedCountyFips` attribute for chooser labeling at `:207`, which this build deliberately does
not import (see Decisions).

**Confirmed to match `/Illustration`'s current behavior**, not just `de0efe0`'s original commit —
read live at the line numbers above this run.

---

## Verification status

**`code-verified`.** Named explicitly, not upgraded.

**What was actually executed:**

- `.\mvnw.cmd clean package` — one compile error (`countyFips` no longer effectively final after
  the ZIP branch could reassign it, breaking the existing `selectedCounty` lookup's lambda) —
  fixed with the identical pattern `IllustrationServlet.java:228-230` already used for the same
  shape (`final String resolvedFips = countyFips;`), then **BUILD SUCCESS**.
- `.\mvnw.cmd -P local clean package -DskipTests` — **BUILD SUCCESS**.
- Deployed the local-profile WAR to an isolated `CATALINA_BASE` on port 8089 per
  `docs/analysis/local_render_verification.md`; IntelliJ's run config (8082) and the system
  Tomcat (8080) untouched.
- Clean start: **zero `SEVERE`**, **zero `NullPointerException`**.
- `GET /ams/GroupConversion` → `302` → `/ams/login`; `GET /ams/GroupConversion?zip=75009`
  (a zip-carrying URL exercising the new code path's routing, though not its logic — see below)
  → also `302` → `/ams/login`, no server error introduced.
- `GET /ams/login` → `200`, **33,776 bytes** — byte-identical to the established baseline.
- Instance stopped, scratch `CATALINA_BASE` deleted.

**What was NOT executed, and why:** the actual authenticated render and the actual ZIP
resolution logic. **T111 (no local PSP Admin credential) has now blocked a fourth consecutive
session** — confirmed again this run (no `AMS_TEST_*` environment variable set). Both requests
above redirect at `LoginFilter`, before `doGet`/`doPost` ever run, so neither exercises the new
`resolveZipPrecedence` method, the ZIP box, the chooser, the no-match panel, or the
`logIllustration` change.

**§5 step 7's three assertions, and which form each took:**

1. **Census block `name` attributes unchanged from `ea6efeb`** — asserted against **source**, not
   rendered HTML. `git diff ea6efebe6367ff565fe248fc30456483cbee0247 -- groupConversion25.jsp`
   filtered to `name="(age|count|deduction)\$\{i\}"` and the `CENSUS_ROWS`/`c:forEach begin="1"
   end="6"` lines returns **empty** — zero lines touching the census block. The full diff against
   `ea6efeb` is **64 insertions, 0 deletions**, purely additive.
2. **Banner block and county dropdown structurally untouched** — asserted against **source**.
   `grep` confirms both the T138 pre-selection banner ("Test-environment rates") and the
   results-path banner are present at their (shifted-by-insertion, but content-identical) line
   numbers, and T137's `stagingCountyFips`/"— test rates" marker is present unchanged. Neither
   block appears in the diff except as line-number drift from insertions elsewhere on the page.
3. **Three helper-text strings present verbatim** — asserted against **source**. All three
   ("Employer + employee combined.", "Employee share is derived as total − employer share.",
   "Flat, per employee, per month.") confirmed present via grep, untouched by the diff.

None of the three could be asserted against rendered HTML this run — the fallback JSP-source form
was used for all three, exactly as `local_render_verification.md`'s own boundary describes: this
recipe catches markup facts from a fetch; without a fetch, source inspection is the next-best
available evidence, weaker than a rendered assertion but not absent.

---

## Decisions made

Two hard stops fired before any code was written; both were referred back for a ruling rather
than resolved unilaterally, given the explicit "I will decide it" framing on stop #5 and the
production-facing risk both carried.

1. **Hard stop #2 fired — `/IchraZipLookup` reuse declined, confirmed twice.** Since `de0efe0`,
   an S11-G change added a `pricedCountyFips()` method (`IchraZipLookup.java:141-158`) requiring a
   `planYear` parameter and computing a `priced` boolean per candidate via
   `RateCacheDAO.check() == PRODUCTION_OK` — counting staging-sourced rates identically to no data
   at all. This is baked into the JSON response shape itself (`:102`), not just the doc comment,
   and it directly contradicts this page's own shipped T137 design (fail-toward-labeling: every
   warmed county stays selectable, staging-sourced ones just marked). **Decision: skip JS reuse
   entirely — no blur-lookup layer, pure server-side resolution**, confirmed identically in both
   rounds of clarification. `IchraZipLookup.java` was never touched; its `priced` computation and
   its doc comment (still scoped to "the illustration page's ZIP field") are both accurate as-is
   and needed no widening, since §3.3's conditional ("if this build reuses the endpoint") never
   triggered.
2. **Hard stop #5 fired — mirroring `/Illustration` faithfully meant changing what
   `illustration_log.zip_code` records.** `logRow.setZipCode(county.getRepresentativeZip())` was
   the exact pattern `IllustrationServlet` itself used to have, and moved away from — documented
   inline as "W2" (`IllustrationServlet.java:917-930`): *"This previously wrote
   `county.getRepresentativeZip()` unconditionally... anyone auditing the log would conclude an
   agent typed a ZIP they never typed... Strictly a reduction in what is stored."* **Decision,
   confirmed after two contradictory rounds and a final narrow re-ask: mirror W2 exactly** — the
   column now records the agent-typed ZIP when one was submitted, `null` otherwise. Reasoning
   recorded here since it wasn't restated with the final short-form answer: choosing "mirror" over
   "leave untouched" also resolves what would otherwise have been a **new** cross-surface
   contradiction (two logging tables sharing the same column-naming convention but disagreeing on
   what it means) — mirroring keeps both surfaces on the same convention going forward, rather than
   introducing a fresh divergence between them. No new backlog row was filed proposing a dedicated
   typed-ZIP column; that proposal was raised attached to the "leave untouched" option in an earlier
   round and its premise (Illustration and GroupConversion disagreeing) did not materialize once
   "mirror" was chosen.
3. **doGet/doPost: one shared private method, not two copies.** `resolveZipPrecedence` is called
   identically from both entry points — `/GroupConversion` has two (a chooser-link/bookmarked-URL
   GET, and the Compare-button POST) where the GET-only `/Illustration` has one. Sharing was
   achievable without any awkwardness (the method's contract — return the resolved `countyFips`, or
   `null` meaning "already forwarded, caller must return" — fits both callers cleanly), so per the
   instruction's own conditional, this was done rather than printing two near-duplicate blocks.
4. **`doGet` only engages the ZIP block when `zip` is present**, not merely when `countyFips` is.
   `doGet` never read `countyFips` at all before this build; making it start doing so on a bare
   `?countyFips=X` (no `zip`) would be new, unasked-for GET behavior beyond ZIP intake. The chooser's
   own links always carry both parameters together (mirroring Illustration's identical pattern), so
   this restriction costs nothing the chooser needs.
5. **The chooser omits Illustration's per-candidate "no rates cached yet" caveat.** That caveat is
   driven by the same `pricedCountyFips`/`PRODUCTION_OK`-only concept declined in decision #1. A
   chooser candidate this page cannot actually price is instead caught by the page's existing
   "Select a valid county from the list" validation, the same as any other unavailable county —
   consistent with GroupConversion having no "priced vs. not" vocabulary today at all.
6. **`submittedCountyFips` updates to the ZIP-resolved county**, matching `IllustrationServlet`'s
   own pattern (`:226`, set *after* the precedence block, from the possibly-reassigned local
   variable) — so a silent override is reflected in the dropdown's `selected` marking, not left
   showing the stale pre-override value.

---

## New assumptions

**S14E-1 — a chooser candidate not present in `availableCounties` degrades to the existing "Select
a valid county from the list" message, not a dedicated caveat.** Verified by reading the existing
`doPost` validation (`selectedCounty == null` → that exact message) and confirming it runs
unconditionally after the ZIP precedence block resolves a `countyFips`, regardless of source.
**Reversal cost: low** — a dedicated caveat, if ever wanted, would need the same
`pricedCountyFips`-style machinery this build deliberately declined, and is a design decision for
whoever revisits decision #1 above.

**S14E-2 — GET's ZIP handling is safe to gate strictly on `zip` presence.** Verified: every
generated chooser link in the new markup carries both `zip` and `countyFips` together (mirroring
Illustration's `<c:param>` pattern exactly), so no legitimate use of the chooser is blocked by this
gate. **Reversal cost: trivial** — removing the `zipParam != null` guard in `doGet` is a one-line
change if `?countyFips=` alone is ever wanted to pre-select on GET.

No `LA-NN` filed: this build adds an intake mechanism already shipped and production-walked
elsewhere in the codebase; it makes no new market or legal claim.

---

## Open questions raised

1. **`illustration_log.zip_code` now holds mixed semantics across its own history, on both
   surfaces.** On `/Illustration`: representative ZIP in rows written before W2, agent-typed ZIP
   (or null) after, nothing in the row itself distinguishing the two eras. **On `/GroupConversion`,
   this build introduces the identical split**: every `CONVERSION`-mode row written before
   `95a81b1` carries `county.getRepresentativeZip()`; every row after carries the agent-typed ZIP or
   `null`. Recorded here per explicit instruction — **not fixed, not filed as a defect.** Whoever
   queries this column for the D-83 county-list decision needs to know pre-fix rows are not agent
   input, on either surface. *Settled by:* whoever undertakes that query, informed by this note.
2. **T111 has now blocked four consecutive sessions** (S13-B, S14-A, S14-B, S14-D's read-only run
   aside, and now S14-E), across four different servlets/JSPs. *Settled by:* Kevin, per the existing
   T111 backlog row — restated because it is the limiting factor on verification claims yet again.
3. **Should the chooser eventually gain a T137-style provenance/priced label**, once decision #1's
   underlying tension (Illustration's PRODUCTION_OK-only "priced" vs. GroupConversion's
   fail-toward-labeling) is resolved some other way? Not addressed here — explicitly declined for
   this build. *Settled by:* whoever next touches either page's chooser with that question in scope.

---

## Backlog rows edited

Both edits are confined to the T74 row (`docs/analysis/project_backlog.md`, one line in the
source file), confirmed by `git diff --stat` showing exactly `1 file changed, 1 insertion(+), 1
deletion(-)` for that file. No new T-number created, no row renumbered.

**Status field — before:**
> `✅ Done for `/Illustration``

**Status field — after:**
> `✅ **RESOLVED 2026-08-04 (S14-E)** — `95a81b155db429df5b65df26f07d8e72cc9ef182`. Code-verified.`

**Stale-caveat clause — before:**
> `⚠️ **Not runtime-verified** — V084/V085 have not been applied to any database, so this is `code-verified` only; §3 click-script steps 4a–4e settle it. Original note follows.`

**Stale-caveat clause — after:**
> `⚠️ **Correction (S14-E, 2026-08-04): this caveat was already stale when written.** `docs/analysis/migration_tracker.md:206-219` superseded it the same day (2026-08-01, end of session) with a behavioral confirmation: V084/V085 shipped in release `v0.85.00` and are applied on Production — ZIP `75482`→Hopkins, `75009`→Collin/Denton chooser, `90210`→correct Texas-only miss, none of which is reachable against an empty `zip_county` table. `docs/claude_memory.md:13` independently corroborates the same facts. The row was never updated after that correction landed, until now. Original note follows.`

The rest of the row (Part 1/Part 2 history, the original corrections chronicled within it) was
left untouched — only these two spans were edited, per the scope fence's explicit "two edits only."

---

## Contradictions found

1. **None in this prompt's own factual claims.** Every citation checked against live files this
   run: `de0efe0` exists and matches the prompt's characterization exactly; `IllustrationServlet`'s
   current precedence matches `de0efe0` (only a later, unrelated `pricedCountyFips` addition);
   `ZipCountyResolver` is confirmed generic with zero Illustration coupling.
2. **The prompt's own framing anticipated `/IchraZipLookup` might have drifted, and it had.** Hard
   stop #2 fired exactly as the prompt's own §4.2 wording described (a response shape assuming the
   illustration page) — not a contradiction of the prompt, but confirmation that its caution was
   warranted.
3. **A genuine surprise, not anticipated by the prompt: mirroring `/Illustration` "verbatim" as
   §1 instructed would have meant adopting W2's logging-semantics change too**, which the prompt's
   own hard stop #5 then had to carve out as a separate decision. The instruction to "copy, not
   design" and the instruction to treat the logging change as a distinct, reserved decision are in
   slight tension — resolved by treating §1's "copy" as governing the UI/precedence behavior and
   §4.5's carve-out as governing data-meaning specifically, which is how this build proceeded.
4. **No disagreement found with `ichra_flow_and_handoffs.md` §5.** It explicitly frames
   `/GroupConversion` as deliberately deferred, gated on the illustration path being proven — this
   build is exactly that gate being satisfied, consistent with the doc's own framing, not a
   correction of it.
5. **Two rounds of my own clarifying questions produced contradictory or off-target answers before
   a resolvable one landed** — recorded not as a defect in the prompt, but as a fact about this
   run's process: the first round's two answers both addressed the same question (zip-logging) and
   disagreed with each other; the second round's two answers both addressed a different single
   question (endpoint reuse, resolved) without restating zip-logging; a third, narrowly-scoped
   re-ask finally produced one clean answer. No code was written until that third round resolved.

---

## Next

**Ship this alongside `ea6efeb` (S14-C, also unreleased) and the earlier T133/T137/T138/T139
items in the next release, then walk `/GroupConversion` once, covering both sessions' additions
together.** This build's diff against S14-C's census reshape is empty by construction, so there is
no interaction risk between them — but neither has been walked, and a single release should carry
both rather than staggering ICHRA/GroupConversion changes across multiple unverified releases.

**T111 remains the single highest-leverage next step for this whole workstream.** Four consecutive
sessions have now produced `code-verified`-only builds on this page family specifically because of
it — resolving it would let the next session upgrade several still-open verification claims at
once, not just its own.

---

## Code-verified-only disclosure

Every claim about the *runtime behavior* of the new ZIP intake rests on reading code, not on a
runtime walk:

- That `resolveZipPrecedence`'s five branches actually produce the described outcomes when a real
  ZIP is submitted (agree/unique/ambiguous/no-match/blank) — reasoned from the code, which mirrors
  `IllustrationServlet`'s own logic line-for-line, but **not exercised**.
- That the chooser panel and no-match panel render correctly, with the right links and copy —
  **not exercised**; markup was read and compared to Illustration's, not rendered.
- That `submittedCountyFips` correctly reflects a ZIP-resolved override in the dropdown's
  `selected` marking — **not exercised**.
- That `logIllustration` now writes the agent-typed ZIP (or null) correctly — **not exercised**;
  the code path was read and matched against `IllustrationServlet`'s W2 fix, not run.
- All three §5-step-7 assertions (census `name` set, banner/dropdown structural integrity, helper
  text) — made against **source**, explicitly not against rendered HTML, per the fallback this
  prompt itself anticipated.
- Both New assumptions (S14E-1, S14E-2).

What **was** runtime-verified: both Maven profiles build (after fixing one real compile error);
the WAR deploys and starts with zero `SEVERE` and zero `NullPointerException`; `/GroupConversion`
and `/GroupConversion?zip=75009` both correctly 302 to `/login` with no server error introduced by
this change; the login page renders at its established byte count. **None of that reaches this
build's actual new logic**, which sits entirely behind an authentication this environment cannot
currently provide.

---

## SQL close-out audit

- **SQL statements produced:** none.
- **SQL statements run:** none. No `SELECT` or any other statement was run against local
  `beta_ssa` this session — every question this run raised was answered from source and from
  already-tracked documentation.
- **Orphaned `.sql` files:** zero. `git diff --name-only 2b785d5f06a3d36b6fc3d018fb2e2ba38e04d448..HEAD`
  returns only `GroupConversionServlet.java` and `groupConversion25.jsp` — no `.sql` path. Run this
  session, not recalled.
- **Current highest migration version:** `V088__proposal_ichra_intake_contribution.sql`, read from
  `ls docs/migrations/` this run. **Unchanged by this build** — no migration created or needed; the
  crosswalk (V084/V085) was already applied on Production before this session began.
- **Pending deployment:** this build's WAR, alongside `ea6efeb` (S14-C, also unreleased) and the
  earlier unreleased T133/T137/T138/T139 items.
- **Schema described but not scripted:** none.

**This build produced no SQL, which is the expected outcome — stated explicitly rather than omitted.**

---

## Compliance statement

- **Scope fence restated:** permitted were `GroupConversionServlet.java`, `groupConversion25.jsp`,
  `IchraZipLookup.java`'s doc comment only (conditionally, if reused — it was not, so this
  condition never triggered and the file was never touched), the T74 backlog row (two edits only),
  and this close-out. **Confirmed nothing outside that set was written**: `git status --short`
  after all edits shows exactly the two code files, the backlog file, and this new close-out.
- **`IllustrationServlet.java`, `illustration25.jsp`, and `ZipCountyResolver.java` were read and
  NOT modified.** Confirmed: none appear in `git status`, in the code commit's file list, or in
  `git diff --name-only 2b785d5f06a3d36b6fc3d018fb2e2ba38e04d448..HEAD`.
- **`IchraZipLookup.java` was left entirely untouched** — not comment-only, not touched at all,
  since the reuse decision was "skip JS reuse entirely." Confirmed absent from every diff listed
  above.
- **Every hash was read from `git log`/`git rev-parse` this run** — the code commit
  (`95a81b155db429df5b65df26f07d8e72cc9ef182`) and the baseline
  (`2b785d5f06a3d36b6fc3d018fb2e2ba38e04d448`). The prompt deliberately stated no baseline hash;
  none was carried from it.
- **Claims sourced from the repo versus this prompt's narrative:** every line number, precedence
  claim, and file-existence fact above was independently re-verified against live files this run —
  `de0efe0` read in full before anything else, per §2's explicit instruction; the current
  `/Illustration` precedence re-read and compared against it line-by-line; `IchraZipLookup.java`
  read in full and compared against its state at `de0efe0`.
- **No forbidden git operation ran.** Only `git log`, `git show`, `git status`, `git diff`,
  `git pull --ff-only`, `git rev-parse`, `git add <named path>`, `git commit`, `git push`. Staging
  was by explicit named path, one `git add` per file.
- **Two hard stops fired (#2 and #5).** Both reported before anything was written; both resolved
  by explicit ruling, across three rounds of clarification given two rounds of contradictory or
  off-target answers — recorded in full above rather than smoothed over.
