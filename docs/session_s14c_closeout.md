# S14-C close-out — `/GroupConversion` census input layout

**Branch:** `refactor/modernize-architecture` (trunk, committed directly)
**Baseline HEAD (read from `git log` at preflight, not carried from the prompt):**
`020db201f9e0e99690c963cfdd99dea4eafd37e9` — "docs: session S14B close-out (T139 fixed; T140/T141 filed)"
**Note on preflight:** `git pull --ff-only` fetched a new tag, `v0.88.04`, alongside "Already up to
date" for the branch itself. Not a failure and not a divergent state — reported per §0, not treated
as a hard stop.

---

## Shipped

- `ea6efebe6367ff565fe248fc30456483cbee0247` — fix: mirror Illustration's age-band row shape in
  GroupConversion census input (S14-C). One file, `groupConversion25.jsp`, **+30/−20**, confined
  entirely to the census `<div class="col-12">` block (confirmed by `git diff` before commit — no
  other line in the file changed).
- Docs commit (this file) — its own hash cannot appear in its own content; read with `git log -1`
  after commit, reported in the session summary instead.

---

## What shape was copied

**Source:** `illustration25.jsp:444-483` — the age-band repeater's per-row markup, inside the
broader repeater block at `:380-501`. The row shape: a `<div class="d-flex align-items-end gap-2
mb-2">` containing one `<div>` per field, each holding a small `form-label` above a
`form-control form-control-sm` input, stacked one row per line inside a plain, non-flex container
(`#ageBandRows` in Illustration — its children simply block-stack).

**Confirmed before copying:** every `.age-band-*` class (`.age-band-row`, `.age-band-age`,
`.age-band-count`, `.age-band-income`, `.age-band-remove`) is used **only as a JavaScript selector
hook** — `grep -n "\.age-band" illustration25.jsp` returns eleven hits, all inside `<script>`
(`querySelector`/`querySelectorAll`), zero inside the page's `<style>` block or any shared
stylesheet. The entire visual shape is Bootstrap utility classes already imported via `css-js.jsp`
and already used on `groupConversion25.jsp` itself (e.g. the pre-existing `d-flex flex-wrap gap-2`
this build replaced). This is what let hard stop #3 clear without touching any shared file.

**What was deliberately not copied, and why:** the add/remove buttons, the hidden `age-band-template`
clone source, and the JS that renumbers rows and starts the list at zero. That entire apparatus
implements Illustration's "adding one replaces Eligible Employees" tier-1→tier-2 behavior, which has
no GroupConversion analog — this page has no tier-1 fallback and no eligible-employee count. Copying
it would mean GroupConversion opens with zero rows and needs clicks to reach six, which is a
functional change to "the number of census rows offered" — explicitly out of scope under §5. The
target shape from the production walk was **scannability** ("hard to scan," "reads as a grid"), not
the add/remove interaction, so mirroring the stacked-row visual shape addresses the complaint without
importing an unrelated feature.

**Deduction — extended in Income's slot.** GroupConversion's Deduction field has no Illustration
counterpart (Income does the analogous "third optional figure" job there). Deduction now occupies
the exact position Income occupies in Illustration's row — third field, immediately after Count,
same label-above-input shape. Its label text, `placeholder="Monthly"`, `min="0"`, `step="0.01"`, and
optional-blank semantics are all unchanged from before this build — only its container shape moved.

**One additional change beyond the row shape, called out explicitly:** the three inputs' inline
`style="width:Npx"` (75px/65px/100px) were removed, matching Illustration's inputs, which carry no
width styling at all and size from `form-control-sm` alone. Keeping the inline widths would have
been introducing "a CSS pattern not already used on the Illustration page" in the opposite direction
— a page-specific override the source shape doesn't have. Removing them is the more faithful mirror.

---

## Verification status

**`code-verified`.** Named explicitly, not upgraded.

**The `name`-set assertion was made against JSP source, not rendered HTML** — no authenticated
session was reachable (see below), so §4 step 5's fallback applies. Two independent checks, both
before commit:

1. `git diff` of the full file — every line outside the census block is byte-identical to baseline;
   the only changed region is the block itself.
2. `grep -n 'name="(age|count|deduction)\$\{i\}"'` against the post-edit file returns the same three
   templated names (`age${i}`, `count${i}`, `deduction${i}`) inside the same `<c:forEach begin="1"
   end="6">`, which resolve to the identical 18 concrete names as before:
   `age1..age6, count1..count6, deduction1..deduction6`. **The set is unchanged; the diff between
   before and after is empty**, confirmed by inspection rather than by capturing two renders and
   diffing them.

**What was actually executed at runtime:**

- `.\mvnw.cmd clean package` (server profile) — **BUILD SUCCESS**.
- `.\mvnw.cmd -P local clean package -DskipTests` — **BUILD SUCCESS**.
- Deployed to an isolated `CATALINA_BASE` on port 8089 per
  `docs/analysis/local_render_verification.md`; IntelliJ's run config (8082) and the system Tomcat
  (8080) untouched.
- Clean start: **zero `SEVERE`**, **zero `NullPointerException`** across the instance's logs.
- `GET /ams/GroupConversion` → `302` → `/ams/login` (confirmed on retry after one transient `HTTP
  000` during warm-up, reported rather than silently discarded).
- `GET /ams/login` → `200`, **33,776 bytes** — byte-identical to the established S7-F baseline.
- Instance stopped, scratch `CATALINA_BASE` deleted.

**What was NOT executed, and why:** the actual rendered census block. `/GroupConversion` requires an
authenticated session; **no local PSP Admin credential exists in this environment** — confirmed
again this run (no `AMS_TEST_*` environment variable set), the same wall as S13-B, S14-A, and S14-B.
**This is the third consecutive session T111 has blocked**, now across three different servlets/JSPs
(`GroupConversionServlet`, `ViewHome25`, and this one). I did not attempt to create an account —
outside this run's fence, and `local_render_verification.md` states account creation is Kevin's,
done by hand.

**Specifically not exercised:**

1. That the rendered HTML's census `name` set matches — reasoned from source (above), **not walked**.
2. That the banner element and county dropdown are present and unchanged — `git diff` confirms
   neither block's markup was touched (the diff starts after the plan-year `</c:choose>` and ends
   before the premium-fields `<div class="col-auto">`), but the actual rendered page was **not
   fetched.**
3. That the three helper-text strings ("Employer + employee combined," "Employee share is derived as
   total − employer share," "Flat, per employee, per month.") are present verbatim — same basis:
   `git diff` shows zero lines touched in that region, **not confirmed by rendering.**
4. Any visual property — spacing, alignment, whether the stacked rows actually read as less
   grid-like on screen. This recipe catches markup facts only; a claim of "looks better" would be a
   CSS-cascade/layout claim this method cannot make, and none is made here.

---

## Decisions made

1. **Copied the row shape, not the repeater apparatus.** See "What shape was copied" — closes the
   question of how literally "mirror it" should be read, in favor of matching the *visual* complaint
   (grid-like scanning) without importing add/remove/zero-start behavior this page's design doesn't
   support and §5 forbids changing.
2. **Did not reuse the literal class name `age-band-row` or its siblings.** Those classes carry zero
   CSS and exist purely as JS hooks in Illustration; since this build adds no JS, applying dead
   marker classes here would misleadingly suggest interactive behavior that doesn't exist. Used the
   underlying Bootstrap utility classes directly instead — that is the actual "shape," and it's
   already a pattern used elsewhere on this same page.
3. **Removed the inline pixel-width styling.** Not present in Illustration's inputs; kept the mirror
   faithful rather than preserving a page-specific override. See "What shape was copied" for the
   full reasoning.
4. **Deduction placed in Income's slot (third field, after Count).** The only structurally sensible
   position given Illustration's row has exactly two fixed fields (Age, Count) plus one conditional
   third (Income) — Deduction is GroupConversion's structural equivalent of that third field.
5. **Kept the outer `<div class="col-12">` and the `Census` label line untouched**, moving only the
   contents beneath it. Illustration's own outer label (`"Ages and Headcounts"`) and its optional/
   tier-2 subtitle were not copied — GroupConversion's existing label text was already accurate
   ("blank age = skip row; deduction is optional") and copying Illustration's wording verbatim would
   have described a different page's semantics.

---

## New assumptions

**S14C-1 — Bootstrap's default block-stacking is sufficient for "one row per line" with no
additional container class.** The six `.d-flex align-items-end gap-2 mb-2` rows are now direct
children of `<div class="col-12">` with no wrapping flex/grid container, relying on `<div>`'s default
`display:block` to stack them — exactly how Illustration's own `#ageBandRows` container works (a
plain `<div>`, no `d-flex`, no `flex-wrap`). **Reversal cost: trivial** — adding a wrapping `<div>`
back, if ever needed, is a one-tag change with no data implications, since `name` attributes are
untouched regardless of container structure.

**S14C-2 — removing the inline width styles is safe because `form-control-sm` alone governs sizing
elsewhere on this page and in Illustration.** Verified by reading Illustration's equivalent inputs
(no width style anywhere in `:444-483`) and by the fact that Bootstrap's `form-control-sm` already
sets a sane default width via its own CSS, not via any inline override. **Reversal cost: trivial** —
re-adding three `style="width:Npx"` attributes, if a rendered walk ever shows a layout problem,
touches only the affected input(s).

No `LA-NN` filed: this is a display/layout change to an already-existing input surface, not a new
market, legal, or data-source claim.

---

## Open questions raised

1. **Does the stacked-row shape actually read as less grid-like on screen, and does it fit
   comfortably in the page's existing width without introducing new scroll or wrap behavior?** This
   recipe cannot answer that — it is exactly the CSS-cascade/layout boundary
   `local_render_verification.md` documents. *Settled by:* a human browser walk, once T111 unblocks
   an authenticated session, or sooner via a direct browser visit that doesn't require this session's
   isolated-Tomcat/curl-only approach.
2. **T111 has now blocked three consecutive sessions across three different pages.** Same open
   question S14-B raised, restated because it is the limiting factor again. *Settled by:* Kevin, per
   the existing T111 backlog row.
3. **Should `GenerateProp25`'s reach-the-null-toDoList question (T140, still open) get resolved
   before or independently of a general "get local auth working" push?** Not raised by this build
   specifically, but worth noting: every recent ICHRA/GroupConversion session has hit the identical
   wall, which argues for prioritizing T111 over any single feature session's local-verification gap.

---

## Contradictions found

1. **None found in this prompt's factual claims.** Every citation checked against the live files:
   `illustration25.jsp:444-483` for the row shape (the prompt did not cite specific line numbers for
   this, correctly leaving it to be found — "Read the Illustration's age-band input markup" — and it
   was found intact); `GroupConversionServlet.java:150-157` and `:190-221` for the by-name parsing
   (also not pre-cited by the prompt, and confirmed as described).
2. **`docs/swbd_ichra_build_plan.md` item 5 does not disagree with the Illustration's shipped state.**
   The prompt raised the possibility explicitly ("this project's documents have been wrong about what
   shipped before"). Checked: item 5 (`:246-261`) is marked "✅ Done 2026-07-31, `0b4711b`," describes
   "a second illustration mode taking age-band counts and rendering per-band net cost," and the
   actual `illustration25.jsp` age-band repeater exists exactly as described, fully wired with
   parsing, rendering, and the W7/W8/W9/W15/K3-b history documented inline in the JSP's own comments.
   **No discrepancy this time** — worth recording as a data point given the prompt's own caution.
3. **The prompt's phrase "one JSP, ICHRA-only, no servlet change, no schema" held exactly.** The final
   diff touches only `groupConversion25.jsp`, confirmed by `git status`/`git diff --name-only` before
   commit.

---

## Next

**A human browser walk of `/GroupConversion`, once reachable.** This build's only unverified claim
of substance is whether the reshaped census block actually reads better — a judgment this recipe
cannot make. It is a small, low-risk thing to check alongside whatever unblocks T111, and should be
bundled with the still-outstanding walk S13-B recommended for T133/T137 together, since all of it
sits on the same unreleased WAR.

---

## Code-verified-only disclosure

Every claim about the *rendered* page rests on reading source (the JSP itself, and its diff against
baseline), not on a runtime walk:

- The `name`-set assertion (Verification status, item 1) — made against JSP source per §4 step 5's
  explicit fallback, not against captured HTML.
- The banner and county dropdown being present and unchanged (item 2).
- The three helper-text strings being present verbatim (item 3).
- Any claim about visual scannability or how the reshaped rows actually appear (item 4) — explicitly
  **not claimed** anywhere in this close-out, since this method cannot support it.
- Both New assumptions (S14C-1, S14C-2).

What **was** runtime-verified: both Maven profiles build; the WAR deploys and starts with zero
`SEVERE` and zero `NullPointerException`; `/GroupConversion` correctly 302s to `/login` with no
server error introduced by this change; the login page renders at its established byte count. **None
of that reaches the census block's actual markup**, which sits behind an authentication this
environment cannot currently provide.

---

## SQL close-out audit

- **SQL statements produced:** none.
- **SQL statements run:** none. No `SELECT` or any other statement was run against local `beta_ssa`
  this session.
- **Orphaned `.sql` files:** zero. `git diff --name-only 020db201f9e0e99690c963cfdd99dea4eafd37e9..HEAD`
  returns only `src/main/webapp/WEB-INF/view/market/groupConversion25.jsp` — no `.sql` path. Run this
  session, not recalled.
- **Current highest migration version:** `V088__proposal_ichra_intake_contribution.sql`, read from
  `ls docs/migrations/` this run. **Unchanged by this build** — no migration created or needed.
- **Pending deployment:** this fix's WAR, alongside the still-unreleased T133/T137/T138/T139 from
  prior sessions this week.
- **Schema described but not scripted:** none.

**This build produced no SQL, which is the expected outcome — stated explicitly rather than omitted.**

---

## Compliance statement

- **Scope fence restated:** permitted were `groupConversion25.jsp`'s census input block and this
  close-out file. **Nothing outside that set was written** — `git status --short` after all edits
  shows exactly `src/main/webapp/WEB-INF/view/market/groupConversion25.jsp` plus (after this file is
  saved) `docs/session_s14c_closeout.md`.
- **`GroupConversionServlet.java` and both Illustration files were read and NOT modified.** Confirmed:
  none of `GroupConversionServlet.java`, `IllustrationServlet.java`, or `illustration25.jsp` appear
  in `git status`, in the code commit's file list, or in `git diff --name-only <baseline>..HEAD`.
- **Every hash was read from `git log`/`git rev-parse` this run** — the code commit
  (`ea6efebe6367ff565fe248fc30456483cbee0247`) and the baseline
  (`020db201f9e0e99690c963cfdd99dea4eafd37e9`). The prompt deliberately stated no baseline hash; none
  was carried from it.
- **Claims sourced from the repo versus this prompt's narrative:** every line number, class-name
  claim, and CSS/JS-ownership fact above was independently verified against live files this run
  (`grep`, `git diff`, direct reads) rather than taken on the prompt's word. Nothing in the prompt's
  factual claims failed that check this time — see Contradictions, above.
- **No forbidden git operation ran.** Only `git log`, `git status`, `git diff`, `git pull --ff-only`,
  `git rev-parse`, `git add <named path>`, `git commit`. Staging was by explicit named path, one
  `git add` per file (only one code file this run).
- **No hard stop fired.** All four were evaluated in §4 step 1 / the plan statement and cleared
  explicitly: #1 (by-name parsing, confirmed), #2 (Illustration's age-band surface exists, confirmed),
  #3 (no shared CSS/JS needed, confirmed by the `.age-band-*` grep), #4 (census block self-contained,
  confirmed by diff boundaries).
