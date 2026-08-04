# S14-A close-out — T138: `/GroupConversion` pre-selection provenance banner

**Branch:** `refactor/modernize-architecture` (trunk, committed directly)
**Baseline HEAD (read from `git log` at preflight, not carried from the prompt):**
`e2aa9720d7b2c0e58a83afb576add2c4442a065a` — "docs: record session 13 close-out commit hash"

---

## Shipped

- `e7c62203a4e08f1aef93ee59193627de88e75759` — fix: render GroupConversion staging-provenance
  banner on the pre-selection GET page (T138, S14-A). `GroupConversionServlet.java` (+13/−0),
  `groupConversion25.jsp` (+13/−0). Two files, both in the permitted set, both pure additions —
  confirmed via `git diff --stat` before commit.
- Docs commit (this file + the T138 backlog row) — its own hash cannot be written into its own
  content without a third commit, which this brief's §4 step 7 does not authorize (exactly two
  commits: code, docs). Read it with `git log -1` after this file is committed; reported in the
  session's chat summary instead.

---

## Verification status

**`code-verified`.** Named explicitly, not upgraded.

**What was actually executed:**

- `.\mvnw.cmd clean package` (server profile) — `clean` failed (`target/…/xmlbeans-5.1.1.jar`
  locked by a running process, unrelated to this change); `.\mvnw.cmd package` without `clean`
  then ran clean and reported **BUILD SUCCESS**.
- `.\mvnw.cmd -P local clean package -DskipTests` (local profile) — **BUILD SUCCESS**, no lock
  issue this time (fresh `target/`).
- Deployed the local-profile WAR to an isolated `CATALINA_BASE` on port 8089, per
  `docs/analysis/local_render_verification.md`'s recipe — never touched the IntelliJ run config
  (8082) or the system Tomcat (8080).
- Server started clean: `grep -i "SEVERE\|ERROR\|Exception"` against the isolated instance's
  `catalina.<date>.log` returned **zero matches**.
- Unauthenticated `GET /ams/GroupConversion` → `302` to `/ams/login`, confirming the app deployed,
  routing reached the servlet's auth gate, and nothing in this change broke context startup or
  request dispatch.
- `GET /ams/login` → `200`, 33,776 bytes — byte-identical to S7-F's original baseline fetch,
  confirming a fully healthy deploy.
- Isolated Tomcat instance stopped and its scratch `CATALINA_BASE` deleted (outside the repo, in
  `$HOME`, per the recipe's own disposability note).

**What was NOT executed, and why:** the actual authenticated render. `/GroupConversion` requires
an `IchraAccessResolver`-satisfying session, and **no local PSP Admin test credential exists in
this environment** — confirmed two ways: no `AMS_TEST_*` environment variable is set, and S13-B's
own close-out (2026-08-04, same day) independently reached the identical wall and left **T111 open**
for exactly this reason. Per §4 step 6's explicit instruction, I did not attempt to create an
account — that is a database write outside this run's permitted fence (§1), and the local render
recipe itself states account creation is "Kevin does this once, by hand."

**Consequently, none of §4 step 6's three assertions were runtime-exercised:**

1. Banner present iff the dropdown set is non-empty and contains a staging-sourced county — **not
   exercised**. Reasoned from code: `loadAvailableCounties` sets `sourceEnv` to
   `RatingAreaRateCache.SOURCE_ENV_STAGING` exactly when `availableCounties` contains at least one
   fips in `stagingCountyFips`, and to `null` otherwise (including the empty-dropdown case, since
   `Stream.anyMatch` on an empty/non-matching set is `false`); the JSP guard
   `not empty sourceEnv and sourceEnv != 'PRODUCTION'` then governs rendering. S13-B separately
   confirmed (2026-08-04, SQL-verified) that all 4 locally-warmed counties are staging-sourced, so
   even an authenticated walk today could only exercise the "banner shows" branch, never "banner
   absent because production-sourced" or "banner absent because dropdown empty" — those remain
   unexercisable locally regardless of the credential gap, pending **T136**.
2. Dropdown still lists its counties with T137's per-option labels intact — **not exercised**.
   This build did not touch `availableCounties`' construction, `CountyReferenceDAO.findByFipsIn`,
   or the `stagingCountyFips` attribute T137 already sets; only a new attribute was added
   alongside. Reasoned safe by inspection, not walked.
3. Results path unchanged, no banner duplication — **not exercised** at runtime. Reasoned from
   code: the new JSP block is gated on `empty selectedCounty`, which is only ever empty on GET or
   on a POST that returns before setting `selectedCounty` (validation-error paths); once
   `selectedCounty` is set (line 186), this new block's guard is false by construction, and the
   existing results-path banner (`:218-224` in the JSP, unedited) is unaffected. `git diff` confirms
   zero lines changed inside that existing block.

---

## Decisions made

1. **Did not touch `setProvenanceAttributes`.** It has exactly one caller (`computeConversion`,
   line 361 — confirmed by `grep -n`), so hard stop #1 did not fire either way, but reshaping it
   was unnecessary: the dropdown-set provenance question is answerable entirely from data
   `loadAvailableCounties` already fetches (`RateCacheDAO.getCountySummaries`), via the
   `stagingCountyFips` set T137 already builds. Not touching it is the smaller diff and leaves the
   results path's rendering logic provably untouched (§4 step 3's "do not change what the results
   path renders").
2. **Computed the new attribute inside `loadAvailableCounties`, not as a separate GET-only step.**
   Both `doGet` and `doPost` call `loadAvailableCounties` before anything else meaningful happens.
   On `doPost`, if the run reaches `computeConversion`, `setProvenanceAttributes` (unchanged) runs
   afterward and overwrites the same `sourceEnv` request attribute with its own selected-county
   value — the exact value it already produced before this change. No new conditional was needed
   to prevent conflict; the existing call order already provides it.
3. **Banner wording: reused verbatim, no rewrite.** Existing text (JSP `:218-224`, unedited):
   > **Test-environment rates.** These figures came from the `${sourceEnv}` environment, not
   > production market data. Do not present this to a client.

   New pre-selection text (JSP, new block): byte-for-byte identical. The existing text never names
   or interpolates a county — it interpolates `${sourceEnv}` (an environment name, "STAGING"),
   which reads sensibly whether or not a specific county's results are on screen. §4 step 4's
   county-naming trigger (hard stop #4) therefore did not fire, and per that same step's instruction
   ("If its text is already neutral with respect to any particular county, reuse it verbatim"), no
   wording was changed. The "do not present this to a client" force is unweakened — the same
   sentence, unedited.
4. **Fail-toward-warning implemented as: any staging fips in the dropdown flips the attribute to
   the literal constant `RatingAreaRateCache.SOURCE_ENV_STAGING`, not the county's actual recorded
   value.** See New assumptions #1 below — this is a real, if currently invisible, difference from
   the results-path banner's behavior.
5. **JSP placement: above the selection form, gated on `empty selectedCounty`.** Chosen so the
   warning is visible before an agent picks a county (matching §2's "fixed" definition) and so it
   also naturally reappears on every POST validation-error re-render (missing county, invalid
   county, blank census) — states that are, in substance, still "pre-selection."

---

## New assumptions

**S14A-1 — the pre-selection banner always says "STAGING," never the county's actual recorded
`source_env` value.** Unlike the results-path banner (which reads the true value off the loaded
`RatingAreaRateCache` rows), the new attribute is a boolean-derived literal:
`RatingAreaRateCache.SOURCE_ENV_STAGING` when any flagged county is present, `null` otherwise. If a
county's `source_env` were some third, unrecognized string (not `PRODUCTION`, not `STAGING`), it
would still correctly trigger the banner (fail-toward-warning preserved, matching `stagingCountyFips`'s
own null/unrecognized-still-marked behavior) — but the banner would say "the STAGING environment,"
which could be literally false for that value. **Reversal cost: low.** Today every recorded value is
either `PRODUCTION` or `STAGING` (T137's S13-B verified this is the only pair in use), so the two
values coincide in every reachable case. A fix, if a third value is ever introduced, would carry the
actual distinct value(s) through instead of a single constant — a small, localized change in
`loadAvailableCounties`, not a DAO change.

**S14A-2 — `availableCounties.stream().map(getCountyFips).anyMatch(stagingCountyFips::contains)` is
an accurate proxy for "the dropdown the agent is about to see contains a staging county."** This
assumes `stagingCountyFips` (built from all cached fips) and `availableCounties` (built by resolving
cached fips through `CountyReferenceDAO`) are evaluated against the same `summaries` read within one
`loadAvailableCounties` call, so they cannot disagree mid-computation. **Reversal cost: trivial** —
both collections are local to the same method invocation.

No `LA-NN` filed — this labels existing data, matching T137's own no-`LA-NN` determination for the
same reason (display of provenance already computed, not a new market/legal claim).

---

## Open questions raised

1. **T111 (no local PSP Admin test credential) is still the blocker on ever runtime-verifying this
   page**, and now blocks two consecutive T138-adjacent builds (S13-B, S14-A) plus the standing
   T111 backlog row itself. *Settled by:* Kevin, per the existing backlog row — not attempted here,
   since creating the account is a database write outside this run's fence.
2. **S14A-1's literal-STAGING assumption is only correct while exactly two `source_env` values
   exist.** S13-B raised the identical structural concern for the `MAX(sourceEnv)` aggregate
   (T-S13B-1); this build inherits the same fragility in a different spot. *Settled by:* whoever
   introduces a third `source_env` value — see New assumptions #1 for the fix shape.
3. **Should the dropdown-level banner and the per-option `— test rates` markers (T137) ever be
   collapsed into one mechanism, now that both exist?** Not addressed here — out of scope per §5
   ("does not filter counties," and no instruction to unify the two T137/T138 mechanisms was given).
   *Settled by:* Kevin, if the two are ever felt to be redundant on screen.

---

## Contradictions found

1. **§4 step 1's line-number citations for `setProvenanceAttributes` did not match the current
   file.** The prompt states "cited by S13-B at servlet `:523-539`"; as read this run, the method is
   at **`:537-553`** (a 14-line drift, consistent with S13-B's own diff to `GroupConversionServlet.java`
   being `+14/−0`, landed after S13-B's close-out was written). The single call site is at **`:361`**
   (S13-B's own citation of `:360` for the same call is off by one for the same reason). The JSP
   citation (`:218-224`) **did** match exactly — that file was untouched between S13-B and this run.
2. **§4 step 1 also asked to read "the method that populates the county dropdown (S13-B cites
   `loadAvailableCounties` at `:176-184`)."** This citation does not point at `loadAvailableCounties`
   in either the current file or, on inspection of S13-B's own close-out text, in S13-B's file either.
   `:176-184` is the **consumer** of its return value — the `doPost` block that validates a submitted
   `countyFips` against the already-loaded `availableCounties` list (`"Select a valid county from the
   list."`). S13-B's own close-out text (`docs/session_s13b_closeout.md`, Decisions §1) uses the same
   `:176-184` citation for exactly that consumer role, not for the method's own location. This
   prompt appears to have conflated the two while transcribing S13-B's citation. The method itself
   was read in full at its actual location, **`:440-463`** before this run's edit (S13-B's own
   close-out separately flagged its own drift on this same method, from `:437-446` to `:438-449`,
   so this line has moved at least three times across three sessions — worth not re-citing a specific
   line number for this method in a future prompt at all, and instead always re-resolving it by name).
3. No other discrepancy found between this prompt's factual claims and the repository's current
   state. `docs/ichra_strategy.md` was not read or edited, per §1's explicit exclusion.

---

## Next

**Ship this alongside T137 in the next release, then walk `/GroupConversion` once — but only after
T111 is resolved.** T137 is already unreleased (`v0.88.02` predates it, per S13-B); this build adds
to the same unreleased surface, so there is no reason to release it separately. The walk, once a
credential exists, should specifically assert the three items listed under "What was NOT executed"
above — they are the entire reason this build is `code-verified` rather than `runtime-verified`, and
they are cheap to check once the one blocking prerequisite (T111) is closed.

## Code-verified-only disclosure

Every substantive claim in this close-out about the *running behavior* of the pre-selection banner —
whether it appears, whether the dropdown still renders correctly, and whether the results page stays
free of a duplicate banner — rests on reading code, not on a runtime walk. Specifically:

- The banner's presence/absence logic (Verification status #1)
- The dropdown's continued correctness under T137's labels (Verification status #2)
- The absence of banner duplication on the results path (Verification status #3)
- Both New assumptions entries (S14A-1, S14A-2)

What **was** runtime-verified: the build compiles and packages under both Maven profiles; the WAR
deploys and starts with zero `SEVERE` log entries; the unauthenticated request path reaches the
servlet's auth gate and correctly redirects, with no server error introduced by this change; the
login page itself renders correctly. None of that reaches the two edited files' actual new logic,
which is gated behind authentication this environment cannot currently provide.

---

## SQL close-out audit

- **SQL statements produced:** none.
- **SQL statements run:** none. No `SELECT` or any other statement was run against `beta_ssa` this
  session — this build needed no data question answered, only code and log inspection.
- **Orphaned `.sql` files:** zero. `git diff --name-only e2aa9720d7b2c0e58a83afb576add2c4442a065a..HEAD`
  returns exactly `src/main/java/net/superiorstate/ams/controller/market/GroupConversionServlet.java`
  and `src/main/webapp/WEB-INF/view/market/groupConversion25.jsp` — no `.sql` path, confirmed by
  running the command this session, not recalled.
- **Current highest migration version:** `V088__proposal_ichra_intake_contribution.sql`, read from
  `ls docs/migrations/` this run. **Unchanged by this build**, as expected — no migration was created
  or needed.
- **Pending deployment:** this build's WAR, alongside T137 (also unreleased) — see Next, above.
- **Schema described but not scripted:** none.

**This build produced no SQL, which is the expected outcome — stated explicitly rather than omitted.**

---

## Compliance statement

- **Scope fence restated:** permitted to modify were exactly `GroupConversionServlet.java`, the one
  JSP it forwards to (`groupConversion25.jsp` — resolved from the `VIEW` constant, confirmed as the
  sole forward target), `docs/analysis/project_backlog.md`'s existing T138 row, and this close-out
  file. **Confirmed nothing outside that set was written**: `git status --short` after all edits
  shows only these four paths touched, no others.
- **Every hash in this document was read from `git log`/`git rev-parse` in this run** — the code
  commit hash (`e7c62203a4e08f1aef93ee59193627de88e75759`) and the baseline HEAD
  (`e2aa9720d7b2c0e58a83afb576add2c4442a065a`) were both read live, never carried from the prompt
  (the prompt deliberately stated no baseline hash, per §0).
- **Claims sourced from the repo** (grep output, `git log`, `git diff`, build/log output, S13-B's
  actual close-out text) **versus this prompt's narrative:** every line-number citation and every
  "what was already established" claim above was independently re-verified against the live
  repository this run, not taken on the prompt's word — see Contradictions, above, for the two
  places the prompt's citations did not hold up.
- **No forbidden git operation ran.** Only `git log`, `git status`, `git diff`, `git pull --ff-only`,
  `git rev-parse`, `git add <named path>`, and `git commit` were used. Staging was by explicit named
  path, one `git add` per file, confirmed via the transcript of this run.
- **No hard stop fired.** All four were evaluated in Steps and cleared explicitly above.
