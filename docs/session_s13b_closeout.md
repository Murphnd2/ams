# S13-B — T137: per-option provenance labeling on `/GroupConversion`'s county dropdown

**Date:** 2026-08-04
**Branch:** `refactor/modernize-architecture` (trunk, committed directly — no branch, no tag)
**Model:** Opus
**HEAD at preflight:** `2329f9aace377b7bc01ce28a0ea1ef6057046461` — exactly the run brief's stated
expectation, so no discrepancy to report.

---

## Shipped

| Commit | Subject |
|---|---|
| `PLACEHOLDER_C1` | fix: label staging-sourced counties in GroupConversion dropdown (T137, S13-B) |

Filled in from `git log` after the push, and recorded in a second commit per the standing
convention.

---

## In flight

None. Working tree clean at close.

---

## Discovery findings

The run's most durable output. Recorded so the next session does not re-derive it.

### `loadAvailableCounties` — `GroupConversionServlet.java:438-449` (pre-change)

```java
private List<CountyReference> loadAvailableCounties(EntityManager em, HttpServletRequest request, int planYear) {
    List<RateCacheDAO.CountySummary> summaries = RateCacheDAO.getCountySummaries(em, planYear);
    List<String> cachedFips = summaries.stream()
            .map(RateCacheDAO.CountySummary::getCountyFips)
            .collect(Collectors.toList());

    List<CountyReference> availableCounties = CountyReferenceDAO.findByFipsIn(em, cachedFips);
    request.setAttribute("availableCounties", availableCounties);
    request.setAttribute("missingReferenceCount", cachedFips.size() - availableCounties.size());
    return availableCounties;
}
```

### Consumers — two, and they are NOT equivalent

| Site | What it does with the result |
|---|---|
| `:111` (GET) | Return value **discarded**. Only the `availableCounties` request attribute matters → dropdown only. |
| `:145` (POST) | Return value **assigned**, then used at `:176-179` to locate the submitted `countyFips`, rejecting at `:180-184` with *"Select a valid county from the list."* |

**Stated plainly, because the run brief's hard stop turns on it: it feeds validation, not only the
dropdown.** This is why the return type was left alone.

### The dropdown

`src/main/webapp/WEB-INF/view/market/groupConversion25.jsp`, option block `:111-115`. Sole
`<select id="countyFips">` on the page; `${availableCounties}` is its only `<c:forEach>` source.

### Banner verification — S12-A confirmed, with a material refinement

**The banner exists.** Servlet `setProvenanceAttributes` at `:523-539`; JSP at `:218-224`:

> **Test-environment rates.** These figures came from the `${sourceEnv}` environment, not
> production market data. Do not present this to a client.

⚠️ **But it renders on the RESULTS view only.** `setProvenanceAttributes` is called from exactly
one place — `computeConversion` at `:360` — and `doGet` (`:111`) never calls it. The JSP block sits
inside the `<c:otherwise>` at `:212`, the successful-results branch. **On the empty form, where the
agent actually chooses a county, `sourceEnv` is unset and no warning renders at all.**

The design premise ("the page already warns at page level, so a banner is not a fix") therefore
held **for the results view only**. This strengthens the case for per-option labeling rather than
weakening it — the markers now cover the one moment the banner never did. Filed as **T138**.

### Per-county provenance — already present, already discarded

No new query and no DAO change were required. `loadAvailableCounties` **already** calls
`RateCacheDAO.getCountySummaries(em, planYear)`, whose `CountySummary` exposes `getSourceEnv()`;
the pre-change code mapped straight to `getCountyFips()` and threw provenance away.

| Thing | Where |
|---|---|
| Field | `RatingAreaRateCache.sourceEnv` → column `source_env`, `nullable = false` |
| Constants | `RatingAreaRateCache.SOURCE_ENV_PRODUCTION` / `SOURCE_ENV_STAGING` |
| DAO read | `RateCacheDAO.getCountySummaries` → `CountySummary.getSourceEnv()` |
| Underlying JPQL | `SELECT r.countyFips, COUNT(r), MIN(r.fetchedAt), MAX(r.fetchedAt), MAX(r.sourceEnv) … GROUP BY r.countyFips` |

---

## Step 1 hard stops — one fired

| # | Condition | Result |
|---|---|---|
| 1 | Banner absent, contradicting S12-A | **Cleared.** Present on the results path; confirmed in both servlet and JSP source before building, at Kevin's explicit request. Not absent from both, so the premise holds. |
| 2 | Feeds validation or submission | **⛔ FIRED** — `:176-184`. Reported before writing anything; see Decisions §1. |
| 3 | Provenance needs a file outside the permitted set | **Cleared.** Already fetched inside the permitted method. `RateCacheDAO` untouched. |
| 4 | Mixed provenance undeterminable without a new query | **Cleared, and verified rather than assumed** — see Decisions §3. |

---

## What was built

| File | Diff |
|---|---|
| `GroupConversionServlet.java` | **+14 / −0** — one `java.util.Set` import, a 7-line comment, a 5-line set construction, and one `setAttribute` |
| `groupConversion25.jsp` | **+1 / −1** — the option block only |

Counts read from `git diff --numstat`, not estimated. Nothing was reformatted, reordered, renamed,
or refactored; no neighbouring code was touched; no config flag was added.

A county whose rates are not production-sourced renders as `County, ST — test rates`. A
production-sourced county gets nothing appended.

---

## Verification claim

**`code-verified`.** Named explicitly, and not upgraded.

**What was actually executed:**

- `.\mvnw.cmd -P local clean package -DskipTests` → `BUILD SUCCESS`.
- The mixed-provenance rule was **executed against MySQL**, not reasoned about:
  `SELECT MAX(v) FROM (SELECT 'PRODUCTION' UNION ALL SELECT 'STAGING')` returns **`STAGING`**,
  confirming a mixed county collapses to a non-`PRODUCTION` value and is therefore labeled.
- Local cache state was read to establish what a walk could show: **4 counties warmed**
  (48029, 48113, 48201, 48223), all plan year 2026, 44 rows each, **all `STAGING`**, and
  **zero counties with mixed `source_env`**.

**What was NOT executed:** the page was not loaded. `/GroupConversion` requires an authenticated
session and no local test credential exists (T111 remains open), so no render was performed.

⚠️ **The mixed state and the production state are not merely unexercised — they are currently
unexercisable.** Every warmed county is staging-sourced and none is mixed, so even a full human
walk today **cannot** demonstrate an unlabeled production county or a mixed-provenance county.
A walk can only show that all four counties appear and all four carry the marker. Saying the
feature is "verified" on that basis would overstate it: the interesting half of the behavior —
the *absence* of a marker — has no data to produce it until production HealthSherpa access lands
(**T136**).

**What a human walk would need to cover:**

1. `/GroupConversion` loads and the dropdown lists **all four** warmed counties — none missing.
   This is the regression guard that matters most, since T137 exists precisely because filtering
   would have emptied the list.
2. All four carry `— test rates`.
3. Submitting one still completes a comparison — proving the untouched validation path at
   `:176-184` is genuinely untouched.
4. **Deferred until T136:** with at least one production-sourced county cached, that county
   renders **without** the marker while staging ones keep it.

---

## Decisions made

1. **⚠️ Proceeded despite hard stop #2, on Kevin's ruling.** The stop fired — `loadAvailableCounties`
   feeds validation. Kevin ruled it not a true conflict with Step 2's constraint 6: **the stop was a
   proxy for the risk constraint 6 states precisely** — a reshaped return breaking a non-dropdown
   consumer. With `List<CountyReference>` untouched and provenance riding alongside in a separate
   request attribute, that risk does not exist, so proceeding is consistent with the stop's intent.
   **His correction to the brief, recorded verbatim in substance:** the stop condition was
   imprecisely worded and should have read *"stop if the return shape must change AND there are
   consumers beyond the dropdown."*
2. **Label wording: `— test rates`, matching the existing banner's vocabulary.** The banner already
   says "**Test-environment rates.**" Introducing a second term ("staging", "non-production") for
   one concept on one page was rejected. Rendered via `&mdash;`, matching the file's existing
   `&middot;` entity style rather than a raw character.
3. **Mixed-provenance rule: fail toward labeling, and verified.** The filter is
   `!SOURCE_ENV_PRODUCTION.equals(getSourceEnv())`, so **null, unrecognised, and mixed all label.**
   Mixed resolves through `MAX(sourceEnv)`; that `MAX('PRODUCTION','STAGING') = 'STAGING'` was
   **executed against MySQL rather than assumed**, because the whole rule rests on it.
4. **Never filter.** Every warmed county remains selectable. This is the entire reason T137 was
   separated from S11-G, and it is why the dropdown does not empty today.
5. **The banner was left alone.** It is correct on the results view and stays correct.
6. **Scope-fence extension, stated explicitly.** The brief permitted only the T137 and T133 rows in
   `project_backlog.md`. Kevin additionally authorised **one new row (T138)** for the GET-path
   banner gap. `T138` was confirmed unused — zero occurrences anywhere in the backlog or in
   `docs/` — before assignment. No other row was added; a `T139` reference briefly written into the
   T137 row was removed rather than left dangling, since no second row was authorised.
7. **Option 3 declined on instruction.** Fixing the GET-path banner gap was explicitly rejected as
   scope creep: a second code path, and a change to what an agent sees before running any
   comparison. Filed, not built.

---

## New assumptions

**T-S13B-1 — `source_env` has exactly two values, and `STAGING` sorts above `PRODUCTION`.**
The mixed-county guarantee depends on `MAX(r.sourceEnv)` returning the non-production value.
Verified for today's two constants. **Reversal cost: low but silent.** If a third value sorting
below `PRODUCTION` were introduced (`DEV`, `ARCHIVE`, `BACKFILL`…), a mixed county containing it
plus `PRODUCTION` would aggregate to `PRODUCTION` and render **unlabeled** — the exact failure the
rule exists to prevent, with no error and no log line. The fix would be a per-county provenance
read that does not aggregate, which is a `RateCacheDAO` change and therefore outside this run's
fence. Raised as an open question below.

**T-S13B-2 — a marker inside `<option>` text is an acceptable presentation.** Option text cannot
carry markup, so the marker is plain text appended to the label. **Reversal cost: trivial** — one
JSP line. If it proves unreadable in a narrow select, `<optgroup>` separation is the alternative.

No `LA-NN` filed: this is display labeling of data provenance, not a market or legal claim. It
makes an existing caveat *more* visible, which is inside LA-17's direction of travel, not a new
assumption about what may be shown.

---

## Open questions raised

1. **Does the `MAX(sourceEnv)` aggregate remain safe if a third `source_env` value is introduced?**
   Today it does; the guarantee is incidental to string ordering, not designed. *Settled by:*
   whoever adds a third value — the safe change is a non-aggregating per-county provenance read in
   `RateCacheDAO`, which no current caller needs.
2. **Should the banner and the option markers agree about when they speak?** After T137 the page
   warns at selection time (markers) and again at results time (banner), but never at selection
   time via the banner. **T138** captures the gap; whether the answer is "call
   `setProvenanceAttributes` on GET" or "the markers are sufficient and the banner is
   results-appropriate" is a design decision, not a defect fix. *Settled by:* Kevin, when T138 is
   scheduled.
3. **What provenance should a *pre-selection* page even claim?** On GET no county is chosen, so
   there is no single `sourceEnv` to report — only a set of per-county ones. This is the substantive
   reason T138 is not a one-line fix. *Settled by:* the T138 design pass.

---

## Contradictions found

Flagged, not fixed.

1. **The run brief's hard stop #2 and Step 2 constraint 6 addressed the same hazard with opposite
   instructions.** Resolved by Kevin mid-run; his rewording is recorded in Decisions §1. This is the
   second consecutive run to stop on a defect in the brief rather than in the code.
2. **⚠️ S13-A's own "contradictions" section overstated a finding, and this run corrects it.**
   S13-A reported that `CLAUDE.md`'s "new code uses `LoggerFactory.getLogger(...)`" was unimplemented
   anywhere, having grepped for `LoggerFactory.getLogger` and found zero matches. That is literally
   true but **materially misleading**: the codebase has a strong logging convention — **Log4j2's
   `LogManager.getLogger`, used in 31 files**, including `GroupConversionServlet` itself (`:23-24`)
   and `RateCacheDAO`. The correct statement is that AMS logs via the **Log4j2 API directly**, not
   via SLF4J. **Consequence: S13-A's SLF4J logger introduced a third style** (`System.out`, Log4j2,
   SLF4J) into a codebase that had two. **No behavioral impact** — `log4j-slf4j2-impl` bridges SLF4J
   to Log4j2, so the T133 WARN reaches `ams.log` correctly, as the runtime walk confirmed. But the
   stylistic choice was made on incomplete evidence and Kevin was told the file had no convention to
   match when the codebase did. `ViewProposal.java` is outside this run's fence and was **not**
   changed; flagged for a future decision on whether to align it to `LogManager`.
3. **T137's own row cited `loadAvailableCounties` at `:437-446`.** It is at `:438-449`. Minor drift
   of the same kind S13-A found in T133's row; corrected only inside the T137 row, which is in fence.
4. **`docs/ichra_strategy.md` — not read, not edited.** Kevin's file. No discrepancy to report; this
   run had no reason to open it.

---

## SQL close-out audit — mandatory section, empty by design

- **SQL statements produced:** none.
- **SQL statements run:** none against any deployed environment. Four **read-only `SELECT`s** were
  run against the **local** `beta_ssa` copy: per-county cache summary with distinct `source_env`; a
  mixed-provenance county count; the `MAX('PRODUCTION','STAGING')` aggregate check; and an
  `information_schema` existence check. **No `INSERT`, `UPDATE`, `DELETE`, or DDL of any kind.**
  Diagnostic reads, correctly belonging to no migration.
- **SQL recommended:** none.
- **Statements requiring a versioned migration:** none.
- **Orphaned `.sql` files:** none created. `git status --untracked-files=all` reports no untracked
  `.sql` anywhere in the repository, and no `.sql` was modified.
- **Current highest migration version — read from `docs/migrations/`, not recalled:**
  **`V088__proposal_ichra_intake_contribution.sql`**. **Unchanged by this run.**
- **Pending deployment:** nothing from this run beyond the WAR itself. Note that T133 (S13-A) is
  also not yet in a published release.
- **Schema described but not scripted:** none.

⚠️ **Local environment note, recorded and deliberately not acted on.** The local database is behind
the repository: `SELECT MAX(version) FROM schema_version` returns **V086**, and
`beta_ssa.proposal_ichra_intake` **does not exist locally** (confirmed via `information_schema` —
0 rows), so **V087 and V088 are unapplied locally.** This surfaces as a caught-and-logged
EclipseLink error on proposal views. **This is local environment state, not a code defect, and
applying migrations is Kevin's** — no action taken. It is also a live illustration of the weekly
production→local refresh documented in `docs/analysis/local_render_verification.md`: the refresh
restores production's schema, so locally-applied migrations do not survive it.

**This run produced no SQL, which is the expected outcome — stated explicitly rather than omitted.**

---

## Next

**Ship T133 and T137 together in the next release, then walk `/GroupConversion` once.** Neither is
in a published release yet — `v0.88.02` predates both — so a single release covers them.

The walk worth doing is short and its value is asymmetric: confirming **all four counties still
appear** is the real regression guard, because T137 exists precisely because the obvious fix
(filtering) would have emptied the dropdown entirely. The marker itself is cosmetic by comparison.

**Do not wait on the mixed/production cases.** They cannot be exercised until **T136** lands
production HealthSherpa access, and blocking a correct, low-risk display change on data that does
not exist would repeat the shape of mistake that left T48 stale.
