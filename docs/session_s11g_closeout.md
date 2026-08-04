# Session 11-G close-out — shared market-data availability check, and the provenance-blind advisory

**Run:** S11-G (Sonnet). ICHRA-scoped, agent-facing only. No customer-facing surface changed.

**Branch:** `refactor/modernize-architecture` (trunk).

**One-line summary:** `RateCacheDAO.check()` is now the single question both the agent-facing advisory
and (in a later run) the customer-facing render gate ask. `IchraZipLookup`'s "priced" flag no longer
counts a staging-warmed county as priced — a deliberate, disclosed behavior change.

---

## Baseline

- **Hash at preflight (post-pull):** `9b72edbebabc364b5e9fcec00db2b4204c4fb726` — **matches the
  prompt's expected start (`9b72edb`) exactly.** No divergence to report.
- **Hash at end (feature commit, pushed):** `444abd43a331789f017cb8550270f1f07792c90c`
- **Hash at end (this close-out commit):** `d49b09bf5ebc31c22f3faa7180568d674799e582`, read from
  `git log` after push, never carried from the prompt and never written before the push.

Preflight passed all four gates: branch `refactor/modernize-architecture`, `git status --short` empty,
`git pull --ff-only` reported "Already up to date."

## Step 1 — discovery findings, in full

### 1. Every consumer of `pricedCountyFips` / the priced flag it produces

- **`IchraZipLookup.writeResolution:90-111`** — the only call site of `pricedCountyFips` itself
  (`:75`). Writes `"priced":true/false` per candidate into the JSON response.
- **`proposalBuilder.jsp:720-723`** (single-county-match path) — shows `#intakeUnpricedMsg` (the
  banner at `:235-237`) when `counties[0].priced === false`.
- **`proposalBuilder.jsp:646` and `:673`** (multi-county chooser) — appends `" (no rates cached yet)"`
  to an `<option>`'s label per-candidate when `county.priced === false`. No separate banner on this
  path.
- **`illustration25.jsp:1537-1543`** — the Illustration page's own county chooser, same pattern:
  appends a `"— no rates cached yet"` `<span>` per candidate. Confirmed this endpoint has a **second**
  JSP consumer beyond the one named in the run prompt (`illustration25.jsp:1565` calls
  `IchraZipLookup` directly, independent of the Proposal Builder). Both consumers are agent-facing
  pages behind `LoginFilter`; neither is customer-facing.

### 2. Every other caller of `RateCacheDAO.getCountySummaries`, and sourceEnv filtering

Three other callers, **none filtering on `sourceEnv`**, all left untouched by this run:

- `IllustrationServlet.java:151` — builds `availableCounties` (the Illustration page's own county
  dropdown, via `CountyReferenceDAO.findByFipsIn(cachedFips)`), then that servlet's **own**,
  differently-scoped private method also named `pricedCountyFips` (`IllustrationServlet.java:637-642`,
  takes `List<CountyReference>`, not `EntityManager`/`String`) builds a set from it — a name collision
  with the method this run edited, not a shared call. Confirmed by grep after the edit: this method
  does not call `IchraZipLookup`'s `pricedCountyFips` or the new `RateCacheDAO.check`.
- `GroupConversionServlet.java:440` — same `availableCounties`-for-a-dropdown pattern.
- `RateCacheAdmin.java:88` — admin table; already displays `sourceEnv` as its own column (via
  `RateCacheWarmService.currentSourceEnv()`), so it is provenance-*aware* for display, just not
  filtering `getCountySummaries` itself.

⚠️ **Flagging, not fixing:** `IllustrationServlet`'s actual county dropdown (the one that determines
which counties an agent can even attempt to illustrate) is built the same provenance-blind way this
run just corrected for the ZIP-lookup advisory. Out of scope — `IllustrationServlet.java` is not in
this run's writable set — but the same class of gap exists there. Whether it matters in practice
depends on whether `IllustrationServlet`'s own compute path (`handleRangeMode`/`handleAgeBandMode`)
independently fails closed on `sourceEnv` before showing figures — not verified this run.

### 3. `RateCacheDAO.getRatesForCounty` and `getCountySummaries`, printed in full (pre-edit)

```java
private static final String JPQL_GET_COUNTY_RATES =
        "SELECT r FROM RatingAreaRateCache r " +
        "WHERE r.planYear = :planYear AND r.countyFips = :countyFips " +
        "ORDER BY r.age";

public static List<RatingAreaRateCache> getRatesForCounty(EntityManager em, int planYear, String countyFips) {
    return em.createQuery(JPQL_GET_COUNTY_RATES, RatingAreaRateCache.class)
            .setParameter("planYear", planYear)
            .setParameter("countyFips", countyFips)
            .getResultList();
}
```

```java
private static final String JPQL_COUNTY_SUMMARIES =
        "SELECT r.countyFips, COUNT(r), MIN(r.fetchedAt), MAX(r.fetchedAt), MAX(r.sourceEnv) " +
        "FROM RatingAreaRateCache r WHERE r.planYear = :planYear GROUP BY r.countyFips";

public static List<CountySummary> getCountySummaries(EntityManager em, int planYear) {
    List<Object[]> rows = em.createQuery(JPQL_COUNTY_SUMMARIES, Object[].class)
            .setParameter("planYear", planYear)
            .getResultList();
    List<CountySummary> summaries = new ArrayList<>();
    for (Object[] row : rows) {
        summaries.add(new CountySummary(
                (String) row[0], ((Long) row[1]).intValue(),
                (LocalDateTime) row[2], (LocalDateTime) row[3], (String) row[4]));
    }
    return summaries;
}
```

`getCountySummaries` **does** carry `sourceEnv` per its `MAX(r.sourceEnv)` aggregate, documented at
`RateCacheDAO.java:64-67` as reliable because a county's rows are always written together in one
`replaceCountyRates` transaction — but `IchraZipLookup.pricedCountyFips` was never reading it. That is
the whole bug: the data was already in `CountySummary`, unused for this purpose.

### 4. `#intakeUnpricedMsg` wording and its toggle JS (unmodified — read-only, forbidden to edit)

`proposalBuilder.jsp:235-237`:
```html
<p class="intake-msg mt-2 mb-0 text-muted" id="intakeUnpricedMsg">
    <i class="bi bi-info-circle me-1"></i>No rate data is cached yet for the selected county -- the proposal will still be created.
</p>
```
Toggle: `proposalBuilder.jsp:720-723`, inside the `IchraZipLookup` fetch callback, single-county path
only — `if (counties[0].priced === false) { ...unpricedMsg.style.display = ''; }`. No mention of
provenance in the wording; nothing here needed or received an edit.

### ⚠️ Hard stop — did not fire

Checked explicitly per the run's condition: does `pricedCountyFips`/the priced flag drive anything
beyond the agent-facing advisory and county chooser — a write, a rate fetch, proposal creation, any
customer-facing output? **No.** Its only path is JSON → the two JSP labels/banner above. Proposal
creation (`ProposalBuilder.attachIchraIntakeIfPresent`) does not read this flag at all — it independently
validates ZIP/county/headcount and writes the intake row regardless of pricing status. The render-time
market tokens (`ViewProposal.putIchraMarketTokens`) are a completely separate code path with its own
gate, unaffected by anything in `IchraZipLookup`. **Proceeded to build.**

---

## Shipped

- `444abd43a331789f017cb8550270f1f07792c90c` — `feat: shared market-data availability check, fix provenance-blind advisory`.
  3 files changed (+78/−4): `RateCacheDAO.java`, `IchraZipLookup.java`, `project_backlog.md`. Pushed to
  `origin/refactor/modernize-architecture`.
- `d49b09bf5ebc31c22f3faa7180568d674799e582` — `docs: S11-G close-out` (this document, as a second
  commit). Pushed.

## In flight

Nothing uncommitted. `git status --short` is clean as of this writing (pre-close-out-commit).

## Decisions made

- **No new file for the result type.** `MarketDataAvailability` is a nested `public enum` inside
  `RateCacheDAO`, alongside the existing nested `CountySummary` class in the same file. Matches
  established precedent in this exact class rather than introducing a new file for a 3-constant enum
  with no behavior. Both this run's and the future `ViewProposal` call site reference it as
  `RateCacheDAO.MarketDataAvailability` — no worse than the existing cross-package reference to
  `RateCacheDAO.CountySummary` already used from `IllustrationServlet`, `GroupConversionServlet`,
  `RateCacheAdmin`, and `IchraZipLookup`.
- **`check()` re-queries `getRatesForCounty` itself** rather than accepting a `CountySummary` or a
  pre-fetched row list. Deliberate: the second consumer (`ViewProposal`, a later run) will call it with
  only a `planYear`/`countyFips` pair from an intake row, no `CountySummary` on hand. Keeping `check()`
  self-contained is what makes it callable identically from both places, which is the entire design
  requirement S11-F's spec set.
- **Exception path returns `NONE_CACHED`, not `STAGING_ONLY`.** The prompt asked for "the most
  restrictive state" without naming which of the two non-`PRODUCTION_OK` states that is. Reasoned that
  `NONE_CACHED` is the more conservative claim on failure — "we don't know / found nothing" — versus
  `STAGING_ONLY`, which asserts something more specific (rows exist, and they're not production) that a
  failed lookup has no basis to claim. Also matches the existing fail-closed idiom elsewhere in this
  codebase (`pricedCountyFips`'s prior empty-set-on-exception, `putIchraMarketTokens`'s
  empty-strings-on-exception) — "nothing available" is always the fallback shape, never a specific
  negative claim.
- **`getCountySummaries` itself was left untouched**, and `pricedCountyFips` still calls it first to
  enumerate candidate counties before calling `check()` per county. This keeps the diff minimal — one
  extra `getRatesForCounty` query per already-cached county (bounded by however many counties have any
  rows at all for the plan year, not all 254) — rather than restructuring the method's shape.

## New assumptions

- **LA-S11G-1: the extra per-county query in `pricedCountyFips` (one `getRatesForCounty` call per
  county already returned by `getCountySummaries`) is acceptable latency for an interactive ZIP-lookup
  endpoint.** Unverified under load — this container cannot run the app. **Reversal cost: low** — if it
  proves slow, `check()` could instead be given an overload accepting a pre-fetched row list so
  `pricedCountyFips` fetches once per plan year and partitions locally, without changing `check()`'s
  public contract for the `ViewProposal` caller (which needs the fresh per-county query regardless,
  since it only ever asks about one county per request).

## Open questions raised

- **Does `IllustrationServlet`'s own county dropdown and compute path independently fail closed on
  `sourceEnv`?** (Step 1, finding 2, flagged above.) Not settled by this run — `IllustrationServlet.java`
  was not in scope. Worth a targeted look before treating the Illustration page as unaffected by the
  same gap this run fixed for the Proposal Builder's advisory.

## Contradictions found

None against the run prompt or existing docs. `getCountySummaries`'s own javadoc
(`RateCacheDAO.java:64-67`, "a county's rows are always written together... MAX(sourceEnv) collapsing to
a single value per county is the expected case, not a fallback") is consistent with — and in a sense
already predicted — the correctness of using per-row `sourceEnv` checks rather than distrusting the
aggregate; `check()` doesn't rely on that invariant anyway, since it re-derives from the raw rows.

## ⚠️ Behavior-change disclosure (§3c)

**This intentionally flips counties that today report priced to unpriced.** Confirmed directly against
the one concrete data point on record: `docs/analysis/project_backlog.md`'s T130 row states Hopkins
County (`48223`, PY2026) is "fully warmed — 44 rows, every age 21–64 — but every row carries
`source_env='STAGING'`." Under the old logic that county reported `priced: true`. Under the new logic it
reports `priced: false`, and the `#intakeUnpricedMsg` caveat now fires for it.

**Since T136 (production HealthSherpa access) remains unresolved, every currently-warmed county is
staging-sourced** — so in practice, **the advisory now fires for every county an agent tries**, where
before this run it fired for none. This is stated plainly here and recorded against T136 in
`project_backlog.md` (new sub-bullet on the existing T136 row, not a new backlog item, since it's a
direct consequence of T136's unresolved status rather than an independent finding) — not buried in a
commit message. It is correct: it now matches what `ViewProposal`'s render-time gate actually produces
for the same county, which was the entire point.

## Side-by-side: the production condition, existing vs. new

**Existing** (`ViewProposal.putIchraMarketTokens`, unmodified by this run):
```java
List<RatingAreaRateCache> rows = RateCacheDAO.getRatesForCounty(em, intake.getPlanYear(), intake.getCountyFips());
List<RatingAreaRateCache> nonTobacco = new ArrayList<>();
for (RatingAreaRateCache r : rows) {
    if (!r.isUsesTobacco()) nonTobacco.add(r);
}
boolean allProduction = !nonTobacco.isEmpty();
for (RatingAreaRateCache r : nonTobacco) {
    if (!RatingAreaRateCache.SOURCE_ENV_PRODUCTION.equals(r.getSourceEnv())) {
        allProduction = false;
        break;
    }
}
```

**New** (`RateCacheDAO.check`):
```java
List<RatingAreaRateCache> rows = getRatesForCounty(em, planYear, countyFips);
List<RatingAreaRateCache> nonTobacco = new ArrayList<>();
for (RatingAreaRateCache r : rows) {
    if (!r.isUsesTobacco()) nonTobacco.add(r);
}
if (nonTobacco.isEmpty()) {
    return MarketDataAvailability.NONE_CACHED;
}
for (RatingAreaRateCache r : nonTobacco) {
    if (!RatingAreaRateCache.SOURCE_ENV_PRODUCTION.equals(r.getSourceEnv())) {
        return MarketDataAvailability.STAGING_ONLY;
    }
}
return MarketDataAvailability.PRODUCTION_OK;
```

Identical row-fetch, identical tobacco filter, identical per-row `sourceEnv` check with the identical
break-on-first-mismatch short-circuit. The only difference is that the old code's single boolean
`allProduction` (true only when non-empty **and** every row is `PRODUCTION`) is now split into three
named states: `allProduction == true` ⇔ `PRODUCTION_OK`; `allProduction == false` because the list was
empty ⇔ `NONE_CACHED`; `allProduction == false` because some row wasn't `PRODUCTION` ⇔ `STAGING_ONLY`.
**A strict refinement of the existing condition, not a new one.** `ViewProposal.putIchraMarketTokens`
itself was not touched — this comparison is offered as proof the new method reproduces its logic
exactly, for whenever a later run wires it in as the second call site.

## ⚠️ Code-verified-only disclosure

Everything above is code-verified only. The build (`.\mvnw.cmd clean package`) passed, confirming
compilation and packaging, but nothing was exercised at runtime: no ZIP was actually looked up, no JSON
response was actually inspected, and the claim that every currently-warmed county is staging-sourced
rests on the backlog's prior recorded finding (Kevin's own production query, per T130's row), not on a
fresh query from this container — this container has no database connection at all.

## Next

The Market page (S11-F Phase A's second half) can now be built as its own run with no new gating logic
— only a call site: `RateCacheDAO.check(em, intake.getPlanYear(), intake.getCountyFips()) ==
MarketDataAvailability.PRODUCTION_OK` combined with the existing `ichraEntitled` flag, per S11-F's spec.
Worth a look, separately: whether `IllustrationServlet`'s own county dropdown carries the same
provenance-blind gap this run fixed for the Proposal Builder's advisory (open question above).

## SQL close-out audit

**This run produces no SQL.** No migration was written, run, or recommended. No `.sql` file was
created, modified, or orphaned. **Current highest migration version:** **V088** —
`ls docs/migrations/*.sql | sort | tail -2` returns `V088__proposal_ichra_intake_contribution.sql`
(plus the long-standing non-versioned `seed_ndt125_questionnaire.sql`).

## Compliance statement

Scope fence was writable: `RateCacheDAO.java`, `IchraZipLookup.java`, `docs/analysis/project_backlog.md`,
`docs/session_s11g_closeout.md`. **All four were used; no fifth file was needed** — the result type went
into `RateCacheDAO.java` as a nested enum rather than a new file, per "Decisions made" above.

Forbidden list, confirmed: **`proposalBuilder.jsp` was not touched** — read three times for discovery,
never edited; the advisory element and its wording are byte-identical to before this run. **No UI text
anywhere says "staging"** — the word appears only in Java javadoc comments (`RateCacheDAO.java`,
`IchraZipLookup.java`), never in the JSON response (`writeResolution` is unmodified) or in any
JSP-rendered string. `ViewProposal.java`, `putIchraMarketTokens`, and every proposal-render path: not
touched. `IchraAccessResolver`, the entitlement gate, `ICHRA_GATED_SECTION_TYPES`: not touched.
`proposal_section`, section types, the agency-override block: not touched. No rate-fetch or cache-write
path was touched — `check()` and the rewired `pricedCountyFips` only read `sourceEnv`, never write it.
No migration, no SQL.

No forbidden git operation was run: no `git add -A`, no `git add .`, no `stash`, `checkout`, `restore`,
`reset`, and no local tag. Staging was by explicit named path, each file listed individually. Build was
run once, successfully, before commit.
