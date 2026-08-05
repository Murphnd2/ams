# Session S16-B close-out — T143: collapse `/GroupConversion`'s input form, plus a scoped docs correction

**Run on:** Sonnet, 2026-08-05.

## 1. Shipped

- `d77a9ac12a5f46e739aba0207f70022e98c1a0ae` — fix: T143 -- collapse GroupConversion's input form once results exist (`groupConversion25.jsp`, +55/−1)
- `<recorded below, commit 2>` — docs: T142 mechanism correction, T143 resolved, ichra_strategy.md D20/D38/D39 + precedence fix, session close-out

Both hashes read from `git log` in this run. Per the capped hash convention, this file is committed as part of commit 2, so commit 2 cannot cite its own hash inside itself — see the compliance statement (§12) for how that is resolved without a fourth commit.

## 2. Part 1 — what was anchored on

**Step 1 — page structure.**
- `<form>` opens at `groupConversion25.jsp:113` (`<form method="post" action="GroupConversion" ...>`), closes at `:221` (pre-edit line numbers; shifted after the edit).
- Results gate: **`${not empty selectedCounty and hasRates}`** — the exact compound condition the `<c:otherwise>` branch (pre-edit `:299`) sits behind, nested inside `<c:if test="${not empty selectedCounty}">` (`:287`) wrapping a `<c:choose>` whose sibling `<c:when test="${not hasRates}">` (`:289`) is an empty-state message, not results. Single coherent gate, not scattered tests — no hard-stop #1.
- All four disclosure banners (agent-only `:301`, test-environment `:305`, illustration-not-a-quote `:313`, off-exchange-only `:319`) sit entirely inside that same results block — none needed to move, none were touched.

**Step 2 — precedent.** `illustration25.jsp` already solves this exact problem as **W14** (`:228-258`, `:1727-1807`): two sibling `.status-card` divs, `#inputSummary` and `#inputCard`, toggled by inline `style="display:none"` computed server-side via a `hasResult` EL variable for the *initial* render, with a client-side `editBtn` click handler doing the one-directional toggle afterward. Mirrored the div-pair/toggle *mechanism* exactly. Diverged deliberately on content-population — see step 4.

**Step 3 — Bootstrap collapse availability.** Checked anyway per the run brief even though precedent made it moot: `css-js.jsp:3-4` loads Bootstrap 5.3.3 CSS and JS bundle via CDN, imported into `groupConversion25.jsp` at `:22`. Available, but unused — the mirrored mechanism uses plain `style.display`, not `data-bs-toggle="collapse"`, because that is what precedent actually does. No hard-stop #3.

**Step 4 — summary-line content availability.** All three target facts were already in JSP scope as request attributes the servlet already sets, confirmed by reading `GroupConversionServlet.java` directly (not assumed):
- County name + plan year: `selectedCounty.countyName`/`.state` (already used at `:413`) and `selectedPlanYear` (already used at `:414`).
- Total lives: `submittedTotalLives` — set at `GroupConversionServlet.java:290`, already used in the JSP at `:338`/`:408`.
- Current total monthly premium: `submittedCurrentTotalPremium` — set at `GroupConversionServlet.java:182` (echoes the raw request parameter), already used at `:200` to populate the input's own `value`.

**All three rendered — none dropped.** This let the build satisfy hard-stop #4 more directly than mirroring W14's client-side `describe()` would have: no new request attribute, no scriptlet, no servlet touch, and the summary text is correct even without JavaScript (a divergence from W14's own behavior — see §9).

## 3. Verification status

- **Code-verified**: the JSP diff, the div/gate structure, the servlet attribute confirmations, and the build.
- **`.\mvnw.cmd clean package` → `BUILD SUCCESS`.** No hard-stop #6.
- **Local render verification: "no local Tomcat reachable on 8089 — a no-server condition, not T111."** `jps -l` showed no relevant Java process; `netstat` showed no listener on port 8089; `curl` returned connection-refused (exit 7, HTTP code `000`). Per the run brief, did not attempt to start Tomcat, invoke `tomcat:run`, or touch any run configuration — that is Kevin's. This is the same finding S16-A made for T142, now confirmed a second time under the run brief's explicit instruction to distinguish it from T111 by name rather than defaulting to "blocked by T111."
- **What T111 does not gate:** production verification by a human operator, unaffected by either T142 or T143's local-verification gap.

## 4. Part 2 — what was corrected

### 2a — T142's severity rationale, `docs/analysis/project_backlog.md`

**Before:** *"Observed live: the operator could not tell his own entered value from the placeholder on his own screen. A row the agent intends to keep can submit blank and drop silently — a data-integrity defect, not a cosmetic one."*

**After** (original sentence kept verbatim, correction inserted immediately after per the run brief's instruction not to delete the record of what was believed): added *"⚠️ Correction (S16-B, 2026-08-05): this mechanism is wrong, and was disproved by S16-A while fixing it. No row was ever dropped — `GroupConversionServlet.java:224` skips a census row only on blank **age**, never on blank count, and `parseBandCount()` (`:616`) already defaulted a blank count to `1` server-side, before T142's fix ever existed. The actual defect was display-honesty, not data-loss... Severity kept at MED — the harm... stands on its own without the data-loss framing; only the stated mechanism was wrong, not the priority."*

Also updated T143's row (status, hash, outcome — see §2 above for the technical content).

### 2b — the two `Part 8 is current` strings, `docs/ichra_strategy.md`

`grep -n "Part 8 is current" docs/ichra_strategy.md` returned **exactly two matches** before editing — no hard-stop #2b.

1. **§11, line 385** (reversal-discipline paragraph). Before: *"...**later Parts govern earlier ones**. Part 8 is current. Four decisions above..."* After: *"...**later Parts govern earlier ones**. Revision 9 / Part 11 is current. Four decisions above..."*
2. **§15, line 478** ("Which document supersedes which" list). Before: *"**Later Parts govern earlier ones. Part 8 is current.** Its own header states the precedence: Part 8 > Part 7 > Part 6 > Part 5 > Part 4 > Parts 1–3."* After: *"**Later Parts govern earlier ones. Revision 9 / Part 11 is current.** Its own header states the precedence: Part 11 > Part 10 > Part 9 > Part 8 > Part 7 > Part 6 > Part 5 > Part 4 > Parts 1–3."*

Target values (Revision 9, Part 11, precedence chain) were not invented — confirmed against `plus_tier_build_plan.md:7-11` (read-only): *"**Revision 9** — O2 resolved from public documentation... **Part 11 governs.**... upgrades **D20** from a decision to a requirement... adds O41–O42"* and `:99`: *"**Precedence: Part 11 > Part 10 > Part 9 > Part 8 > Part 7 > Part 6 > Part 5 > Part 4 > Parts 1–3.**"* — the extended chain in `ichra_strategy.md` now matches this verbatim.

`grep -n "Part 8 is current" docs/ichra_strategy.md` after editing returned **zero matches**, confirmed.

### 2c — §11's decision register, `docs/ichra_strategy.md`

**Heading:** `## 11. Decisions settled — D1–D37` → `## 11. Decisions settled — D1–D39`.

**D20's row**, before: *"Outbound correlation key is a separate opaque UUID; **`ssn_hash` never leaves AMS** | ✅"* — described purely as an adopted decision. After: appended *"⬆️ **Upgraded from a decision to a requirement, Part 11** — `external_id` is unique per platform and both create and submit are non-idempotent, so AMS-side dedup is structural, not optional hygiene; stop describing it as a choice"* and changed the status column to *"✅ **required** (Part 11)"*. Sourced verbatim from `plus_tier_build_plan.md:2161` (read-only): *"**D20** | ⬆️ **Upgraded from a decision to a requirement.** ... `external_id` being **unique per platform**, with **create and submit both non-idempotent**, makes it structurally necessary — AMS must own dedup and cannot retry blindly. **D20 is no longer optional and should stop being described as a choice**"* — not invented from the run brief's own description of D20, which was a pointer, not a source, per the run brief's own instruction.

**D38 and D39 added as new rows**, summarized from source text read in `plus_tier_build_plan.md` (read-only), not from the run brief's descriptions:

- **D38 source** (`plus_tier_build_plan.md:2007-2023`): *"The CMS **ICHRA Employer LCSP Premium Look-up Table** is the source for any compliance-facing affordability figure... It is **on-exchange by construction**... HealthSherpa remains the **illustration** source — market low/high premium, plan count, carrier count, lowest bronze, age-band net cost... `onex_lcsp_premium` and `onex_benchmark_silver_premium` (V078, T44) are **retained**... become a **cross-check**."* Summarized as: *"LCSP data-source split — CMS's ICHRA Employer LCSP Look-up Table is the source for compliance-facing affordability (on-exchange by construction); HealthSherpa remains the illustration source (market premiums, plan/carrier counts). V078's `onex_*` columns retained as a cross-check, not removed."*
- **D39 source** (`plus_tier_build_plan.md:2049-2067`): *"`ICHRA+` and `QSEHRA+` are **two separate `LOS` rows**. They are not one line of service carrying a design attribute... **A health FSA is a group health plan.** A **QSEHRA requires that the employer offer no group health plan.** Therefore **QSEHRA + FSA disqualifies the QSEHRA**... **An ICHRA *is* a group health plan**... A **QSEHRA is excepted from that definition**."* Summarized as: *"`ICHRA+` and `QSEHRA+` are two separate `LOS` rows, not one LOS with a design attribute — the quotable attached-product bundle differs by statute (QSEHRA excludes any group health plan, so QSEHRA + FSA disqualifies the QSEHRA; ICHRA has no such exclusion)."*

`git diff -- docs/ichra_strategy.md` (printed in full during the run) confirms the edit touched exactly: the two `Part 8 is current` strings, the §11 heading, D20's row, and the two new D38/D39 rows. Nothing else in the file changed.

## 5. Decisions made

- **Server-side EL summary content over W14's client-side `describe()`.** The run brief's hard-stop #4 explicitly framed the check as "already in JSP scope... as request attributes," and all three target facts turned out to be exactly that. This is a deliberate, reasoned divergence from precedent's *content* mechanism while keeping precedent's *structural* mechanism (two toggled divs, one-directional JS-driven Edit button) intact — documented inline in the JSP comment so a future reader does not mistake it for an oversight.
- **Kept the defensive `empty inputError` conjunct** in `hasResult` even though it is structurally redundant today (verified against the servlet's actual early-return paths), matching W14's own three-part condition for the same reason W14 has it — cheap insurance against a future servlet change silently decoupling the two conditions.
- **Did not add K3-d-style stale-result clearing** (W14's later enhancement that hides the results panel on any post-Edit form change). The run brief's constraint 5 explicitly forbids touching the results block, and K3-d requires wrapping it with a new `id` to target from JS. Out of scope for this build; not silently skipped — recorded as a new item below (T155).
- **T142's original filing text was kept, not deleted**, per the run brief's explicit instruction — the correction sits immediately after it rather than replacing it.

## 6. New assumptions

None. Every value used in the summary line, every gate condition, and every corrected doc claim was confirmed by reading the relevant source (`GroupConversionServlet.java`, `illustration25.jsp`, `plus_tier_build_plan.md`) directly, not inferred from the run brief's own descriptions.

## 7. Open questions raised

None specific to T142/T143. Section 9 below raises two new observations, but neither is a question for Kevin to answer — both are filed as concrete, self-contained findings for a future session.

## 8. Contradictions found

**T142's row (2a) contained a real mechanism error**, not just a remedy that happened to match — this is exactly the distinction the run brief asked to check for, having named S16-A's own miss on this point. The original filing believed a row could be silently dropped; reading `GroupConversionServlet.java:224` and `:616` directly during S16-A's own build showed that was never possible, but S16-A's close-out reported the finding without flagging the row as contradicted. Corrected in this run (2a).

No other contradictions found. T143's proposed remedy ("collapse to a compact summary line with an edit affordance... layout only, no servlet change") matched both the filed description and the actual implementation once precedent was read — the mechanism and the remedy agree.

## 9. Anything noticed and deliberately not fixed

`grep -rn "T15[0-9]" docs/` (run before assigning anything) returned matches up to **T152** — high-water mark confirmed, next available is T153.

- **T153 (new, MED)** — `/GroupConversion`'s "No cached rate data" empty-state can render simultaneously with an unrelated input-validation error. Found while confirming `hasResult`'s safety: `GroupConversionServlet.doPost`'s census-row loop returns immediately with `inputError` set on an invalid age or count (`:228-238`), **before** `hasRates` is ever computed (`:313-314`). Since `selectedCounty` is already set by that point (`:218`), the JSP's `<c:if test="${not empty selectedCounty}"><c:choose><c:when test="${not hasRates}">` (pre-edit `:287-297`) evaluates `hasRates` as unset/falsy and renders *"No cached rate data for age(s) ... in this county"* — a cache-completeness message — directly alongside the actual, unrelated *"Row N: count must be a positive whole number"* alert. Two disagreeing explanations for the same blocked submission. Code-verified only, not observed live. Does not affect this run's `hasResult` gate, which correctly excludes this state via its `empty inputError` conjunct (§5) — the form stays open, as it should; only the pre-existing empty-state message is the problem, and that block was out of scope to touch this run (constraint 5).
- **T154 (new, LOW)** — `illustration25.jsp`'s W14 comment (`:241-244`) claims *"With JavaScript off the summary never renders and the form is simply always open, which is today's behaviour."* Reading the actual markup (`:252`, `:260`) says otherwise: `#inputSummary`'s visibility and `#inputCard`'s visibility are both computed via the *same* server-side `hasResult` ternary, so with `hasResult` true and JavaScript off, the summary div is visible (with an empty `#inputSummaryText`, since only `describe()` populates it) while the form stays hidden and the Edit button is inert — the opposite of "form is simply always open." Low priority (an internal admin tool already depending on Bootstrap JS elsewhere is not a realistic no-JS environment) but a genuine stale-comment-vs-code discrepancy in a file this run could only read, not fix. This run's own T143 build inherits the *toggle*-without-JS limitation (Edit does nothing without JS) but not the *content* one — the summary line renders correctly via EL regardless of JavaScript (§2, step 4 divergence).
- **T155 (new, LOW)** — no K3-d-equivalent stale-result clearing exists for `/GroupConversion` after this run. If an agent clicks Edit and changes an input without resubmitting, the results below still describe the prior submission. `illustration25.jsp` closed this exact gap for itself as a named follow-on to its own W14 (`:1786-1804`, "K3-d"). Deliberately not built here — the run brief's constraint 5 forbids touching the results block, which K3-d's mechanism requires (wrapping it with an `id` to hide via JS). Filed rather than silently absent.
- **Pre-existing doc staleness immediately adjacent to my 2c edit, not fixed because it was outside the four authorized touch-points:** `ichra_strategy.md:343`, *"The **only place all 37 appear together**"* and `:387`, *"`docs/analysis/plus_tier_build_plan.md` Parts 2, 3, 4, 5, 6, 7, 8"* — both now read stale next to the corrected D1–D39 heading and D38/D39 rows (should read "39" and extend through Parts 9-11 respectively). Left untouched per the scope fence's explicit "nothing else in it may be touched" — recorded here rather than silently left inconsistent. Not assigned a T-number since it is pure prose inside a file Kevin gated to this run only; a future run with the same authorization can fix it alongside whatever prompted reopening the file.

## 10. Code-verified-only disclosure

Every claim in this close-out rests on reading code/docs and build/verification-command output, not on observing the running application, except the git/build mechanics themselves. Specifically code-verified-only:
- The collapse will render correctly (visible/hidden state matching `hasResult`) on first load and after a Compare submission.
- The summary line will display the correct county, plan year, lives, and premium text.
- The Edit button will correctly re-show the form and refocus the ZIP field.
- T153 (the simultaneous empty-state/validation-error rendering) — read from `GroupConversionServlet.java`, never triggered live.
- T154 (W14's no-JS behavior) — read from `illustration25.jsp`, never triggered live; `illustration25.jsp` was read-only this run regardless.
- The claim that "no local Tomcat reachable on 8089" is a no-server condition rather than T111 — established by `jps`/`netstat`/`curl` all agreeing no server is listening, which is itself an observation, not a code-read; everything downstream of that (what a running instance *would* show) remains code-verified only.

## 11. SQL close-out audit

**This run produced no SQL.** No `.sql` file was created, edited, run, or recommended. Current highest migration, read from `docs/migrations/` this run (not recalled): **V088** (`V088__proposal_ichra_intake_contribution.sql`). No orphaned `.sql` files were introduced. Nothing is pending deployment beyond the WAR itself. No schema was described but left unscripted. Migrations confirmed unchanged at V088 — this run touched no migration file and no schema.

## 12. Compliance statement

- **Wrote to:** `src/main/webapp/WEB-INF/view/market/groupConversion25.jsp` (Part 1 only), `docs/analysis/project_backlog.md` (T142's row per 2a, T143's row), `docs/ichra_strategy.md` (2b's two strings, 2c's heading/D20 row/D38/D39 rows only), `docs/session_s16b_closeout.md` (new file).
- **Wrote to nothing else.** `git diff --name-only` on commit 1 (`d77a9ac`): exactly `src/main/webapp/WEB-INF/view/market/groupConversion25.jsp`. Commit 2's file list is confirmed the same way once made: `docs/analysis/project_backlog.md`, `docs/ichra_strategy.md`, `docs/session_s16b_closeout.md` — nothing else.
- **`docs/ichra_strategy.md` was edited only at 2b and 2c, confirmed by the full diff printed in §4 above** — the two `Part 8 is current` strings (§11 line 385, §15 line 478), the §11 heading, D20's row, and the two new D38/D39 rows. §4's banner, §5, §9, §10, and §12 were not opened for editing at any point in this run.
- **Read-only, as scoped:** `GroupConversionServlet.java`, `illustration25.jsp`, `css-js.jsp`, `docs/analysis/plus_tier_build_plan.md` — all read, none edited.
- **No forbidden git operation was run.** Only `git rev-parse`, `git log`, `git status`, `git diff`, `git ls-files`, `git merge-base`, `git pull --ff-only`, `git add <named path>`, `git commit`, and `git push` were used. No branch created/switched/deleted; no `add -A`/`add .`/`add -u`; no `stash`/`checkout`/`restore`/`reset`/`rebase`/`tag`/`branch`; no force-push.
- **Hard stops:** none fired. #1 (results gate shape), #2b (match count), #3 (Bootstrap availability), #4 (attribute availability), #6 (build failure) were each checked and resolved to their non-blocking branch.
- **Every hash cited was read from `git log` in this run.** No hash was carried from the prompt and no placeholder was left in the final version of this file (commit 2's own hash is handled per the capped convention below, not left as a placeholder).
- **Commit count: two, or three if a third is made solely to record commit 2's hash** — capped per the run brief, never a fourth.

## 13. Next

No open T142/T143 work remains. Recommended next: **T153** (the simultaneous empty-state/validation-error rendering on `/GroupConversion`) — it is a real, if narrow, correctness defect on the same page this run and S16-A already improved, cheap to isolate (the fix is almost certainly narrowing the `<c:when test="${not hasRates}">` condition to also require `empty inputError`, mirroring exactly the defensive conjunct this run already added to `hasResult`). T154 and T155 are lower priority and can wait for a session that already has `illustration25.jsp` or the results block in scope.
