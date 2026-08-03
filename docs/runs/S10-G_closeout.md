# S10-G close-out — market-data tokens for plus-tier `CUSTOM` proposal sections

**Date:** 2026-08-03 · **Branch:** `refactor/modernize-architecture` · **Type:** investigate-then-build; customer-facing render path, market data, compliance boundary.
**Files:** T130 built. T131 filed (carried assumption from S10-F).

---

## 1. Preflight output, verbatim

```
$ git rev-parse --abbrev-ref HEAD
refactor/modernize-architecture

$ git log -1 --format="%h %ci %s"
091ca0f 2026-08-03 13:50:11 -0500 docs: record the S10-F close-out commit hash and stat

$ git status --short
(empty)

$ git pull --ff-only
Already up to date.

$ git merge-base --is-ancestor 091ca0f HEAD && echo T129_PRESENT
T129_PRESENT
```

All hard-stop conditions clear.

---

## 2. Q1 — the illustration's read path

`IllustrationServlet` reaches rates through **`RateCacheDAO`**, three methods, all pure JPQL:

- `RateCacheDAO.getRatesForCounty(em, planYear, countyFips)` → `List<RatingAreaRateCache>`, every age for one
  county/year, ordered by age ([RateCacheDAO.java:56-61](../../src/main/java/net/superiorstate/ams/data/dao/RateCacheDAO.java:56)),
  called at [IllustrationServlet.java:265](../../src/main/java/net/superiorstate/ams/controller/market/IllustrationServlet.java:265)
- `RateCacheDAO.getRate(em, planYear, countyFips, age, usesTobacco)` → one row or `null`
  ([:42-53](../../src/main/java/net/superiorstate/ams/data/dao/RateCacheDAO.java:42)), called at
  [IllustrationServlet.java:447](../../src/main/java/net/superiorstate/ams/controller/market/IllustrationServlet.java:447)
- `RateCacheDAO.getCountySummaries(em, planYear)` → per-county row counts and timestamps
  ([:69-84](../../src/main/java/net/superiorstate/ams/data/dao/RateCacheDAO.java:69))

**No branch of any of them can trigger a fetch.** Proof in §5.

**This run uses `getRatesForCounty`** — one query returns every age plus the counts and timestamps, so all
seven tokens resolve from a single round trip.

---

## 3. Q2 — what the cache actually contains

Read from the entity, not from the strategy document
([RatingAreaRateCache.java:17-68](../../src/main/java/net/superiorstate/ams/model/market/RatingAreaRateCache.java:17)):

| Field | Column | Used here |
|---|---|---|
| `planCount` | `plan_count` | ✅ `{{ICHRA_PLAN_COUNT}}` |
| `carrierCount` | `carrier_count` | ✅ `{{ICHRA_CARRIER_COUNT}}` |
| `lowestBronzePremium` | `lowest_bronze_premium` | ✅ the floor, all three ages |
| `fetchedAt` | `fetched_at` **NOT NULL** | ✅ `{{ICHRA_RATES_AS_OF}}` |
| `sourceEnv` | `source_env` **NOT NULL** | ✅ provenance gate |
| `age` | `age` | ✅ keying |
| `usesTobacco` | `uses_tobacco` | ✅ filtered out |
| `marketLowPremium` / `marketHighPremium` / `lcspPremium` / `benchmarkSilverPremium` / `onexLcspPremium` / `onexBenchmarkSilverPremium` | — | ❌ not exposed |

**Ages present:** the warm job writes every age from `AgeCurve.MIN_AGE` to `MAX_AGE`, i.e. **21–64 inclusive**
([RateCacheWarmService.java:334](../../src/main/java/net/superiorstate/ams/data/service/RateCacheWarmService.java:334),
[AgeCurve.java:38-39](../../src/main/java/net/superiorstate/ams/data/util/AgeCurve.java:38)). So 21/40/64 are
all guaranteed present in a warmed county.

**§1.2 satisfied — no hard-stop.** `fetched_at` is a per-row `NOT NULL` timestamp, so an as-of date is
derivable from the entry itself rather than approximated.

---

## 4. Q3 — callability from `ViewProposal`, and Q4 — cache miss

**Q3: yes, with the `EntityManager` T128 already threaded.** `RateCacheDAO` is an `abstract` class of
`static` methods taking `EntityManager` as their first argument — the same shape `ProposalIchraIntakeDAO`
already used from `buildTokenMap`. No injected dependency, no lifecycle of its own, no service to
instantiate. **Nothing needed restructuring and no second file was touched.**

**Q4: a miss returns an empty list, not null and not a throw.** `getRatesForCounty` ends in
`.getResultList()` ([:60](../../src/main/java/net/superiorstate/ams/data/dao/RateCacheDAO.java:60)), which
returns an empty `List` when nothing matches — JPA reserves `NoResultException` for `getSingleResult`, which
this path does not use. (`getRate`, unused here, catches that and returns `null` at
[:50-52](../../src/main/java/net/superiorstate/ams/data/dao/RateCacheDAO.java:50).) The fallback is therefore
driven by `nonTobacco.isEmpty()`, which is also what makes the provenance gate's `allProduction` start
`false` for a cold county.

---

## 5. ⚠️ The no-outbound-call proof (§7.5)

Established three ways, all static:

1. **Reference search.** `HealthSherpaService` is referenced by exactly **three** files in the whole tree:
   itself, `RateCacheWarmService` (the scheduled warm job), and `RateCacheAdmin` (the admin page).
   **Neither `RateCacheDAO` nor `RatingAreaRateCache` appears in that list.**
2. **Import audit.** `RateCacheDAO`'s complete import set is `jakarta.persistence.EntityManager`,
   `jakarta.persistence.NoResultException`, `RatingAreaRateCache`, log4j's `LogManager`/`Logger`, and
   `java.time.LocalDateTime` / `java.time.format.DateTimeFormatter` / `java.util.ArrayList` / `java.util.List`
   ([RateCacheDAO.java:3-12](../../src/main/java/net/superiorstate/ams/data/dao/RateCacheDAO.java:3)).
   **No HTTP client, no URL, no socket, nothing network-capable.**
3. **Grep for HTTP primitives** (`HttpClient`, `HttpURLConnection`, `okhttp`, `RestTemplate`, `URL(`,
   `HealthSherpa`) across both `RateCacheDAO` and `RatingAreaRateCache`: **zero hits.**

The full call chain added by this run is
`buildTokenMap → putIchraMarketTokens → RateCacheDAO.getRatesForCounty → em.createQuery(...).getResultList()`
and it terminates in the database. **There is no branch, no fallback, and no lazy-warm path that can reach
the network.** The method's javadoc records this prohibition so it is not quietly reversed later.

---

## 6. Compliance decisions (§5 of the brief)

**Built, and precedented by the shipped illustration:** plan count, carrier count, the lowest available
premium at 21/40/64 stated as a floor, and an as-of date.

**Not built, and structurally impossible in this code:** no plan name, no carrier name, no ordering or
ranking, nothing marked best/recommended/default, no per-employee affordability determination. The method
reads only aggregate counts and `lowestBronzePremium`; there is no collection of plans to rank and no
per-employee input to evaluate.

**Two judgment calls, both made toward the cautious side:**

1. **`{{ICHRA_RATES_SCOPE}}` is emitted only when real figures are.** A caveat on a proposal that showed no
   figures would describe data the employer never saw. Its text names the county and the as-of date and says
   plainly that the figures are off-exchange only, *"not a quote and not a complete view of the market."*
2. **Provenance gate is `allMatch`, not `first`.** Every non-tobacco row must be `PRODUCTION`; one staging row
   suppresses the whole group. The illustration's own `setProvenanceAttributes` takes
   `distinctSourceEnvs.get(0)` ([IllustrationServlet.java:826-832](../../src/main/java/net/superiorstate/ams/controller/market/IllustrationServlet.java:826)),
   which is fine for an authenticated operator page but too permissive for a public one. **Stricter here on
   purpose**, matching how this servlet already treats `ProposalIchraSnapshot`.

**No new `LA-NN` filed, and the reason is specific.** LA-17 already names the exact content class this run
emits — *"may carry market data to an employer — premium ranges, plan and carrier counts, affordability
output"* ([legal_assumptions.md:1003-1006](../analysis/legal_assumptions.md)). This is inside an existing
assumption, not a new one. Nothing here was decided under uncertainty about authority.

### ⚠️ But one content-authoring warning, which is Kevin's not the code's

**LA-17 constraint 3 requires two structurally separate voices:** *"The agent's section carries market data.
SSA's supplemental section describes administration services only… a document that blends the two voices is
evidence against the very fact it depends on"*
([legal_assumptions.md:1034-1039](../analysis/legal_assumptions.md)).

S10-D established the tier-1 section as **SSA's supplemental voice** — administration only, no market data.
These tokens are **the agent's voice**. **Putting both into one `CUSTOM` section would collapse the
distinction LA-17 rests on.** They belong in separate sections, visually and structurally distinct.

Recorded here rather than as an `LA-NN`, following the precedent LA-17 set for its own enforcement mechanism
([:1048-1053](../analysis/legal_assumptions.md)): the register admits an entry only when it *both* rests on a
reading of authority *and* is expensive to reverse. This rests on LA-17, but reversal is a content edit —
**cheap** — so it fails the second test. It is in T130's backlog row and in §9 below.

---

## 7. The build — diff size and reason

**`ViewProposal.java` only: +137 / −2.**

| Piece | Lines | Reason |
|---|---|---|
| Imports | +5 | `RateCacheDAO`, `BigDecimal`, `NumberFormat`, `LocalDateTime`, `DateTimeFormatter`. All JDK or already-permitted-package; no new dependency. |
| `buildTokenMap` signature + call site | 2 changed | Added `boolean ichraEntitled` so §6.1's requirement — *entitlement gates the values, not just the container* — is enforceable inside the method. The value is the one already resolved once per request at [:109](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:109); **no second resolver call, and `IchraAccessResolver` is untouched.** |
| `putIchraMarketTokens` | +~105 | The whole feature, extracted to its own method rather than inlined so the cache-only prohibition, the provenance gate and the fail-closed conditions live with a javadoc that states them. |
| `formatPremium` | +~8 | One-line-per-age currency formatting; keeps the main method readable. |

The two deletions are the signature line and the call-site line, both replaced. **Everything else in the file
is untouched** — including T129's filtering, `ICHRA_GATED_SECTION_TYPES`, the snapshot block, and
`replaceTokens`.

---

## 8. Verification

**1. Build.** `.\mvnw.cmd clean package` → **BUILD SUCCESS**, 509 source files, WAR assembled. Only the
pre-existing `RecurringChecklistDAO` unchecked warning, untouched by this run.

**2. Every token, exactly as an HTML author types it.**

T128's four (unchanged by this run):
```
{{ICHRA_COUNTY}}
{{ICHRA_COUNTY_FIPS}}
{{ICHRA_HEADCOUNT}}
{{ICHRA_PLAN_YEAR}}
```
This run's seven:
```
{{ICHRA_PLAN_COUNT}}
{{ICHRA_CARRIER_COUNT}}
{{ICHRA_FLOOR_AGE_21}}
{{ICHRA_FLOOR_AGE_40}}
{{ICHRA_FLOOR_AGE_64}}
{{ICHRA_RATES_AS_OF}}
{{ICHRA_RATES_SCOPE}}
```

**3. The four cases.** "—" means empty string, never a literal `{{…}}`.

| Token | Entitled + cache hit | Entitled + cache miss | Unentitled | No intake row |
|---|---|---|---|---|
| `{{ICHRA_COUNTY}}` | `Hopkins County` | `Hopkins County` | `Hopkins County` | — |
| `{{ICHRA_COUNTY_FIPS}}` | `48223` | `48223` | `48223` | — |
| `{{ICHRA_HEADCOUNT}}` | `10` | `10` | `10` | — |
| `{{ICHRA_PLAN_YEAR}}` | `2026` | `2026` | `2026` | — |
| `{{ICHRA_PLAN_COUNT}}` | `47` | — | — | — |
| `{{ICHRA_CARRIER_COUNT}}` | `4` | — | — | — |
| `{{ICHRA_FLOOR_AGE_21}}` | `$312.44` | — | — | — |
| `{{ICHRA_FLOOR_AGE_40}}` | `$489.38` | — | — | — |
| `{{ICHRA_FLOOR_AGE_64}}` | `$1,062.29` | — | — | — |
| `{{ICHRA_RATES_AS_OF}}` | `July 31, 2026` | — | — | — |
| `{{ICHRA_RATES_SCOPE}}` | full sentence | — | — | — |

*(Illustrative values; shapes and formats are what this run established, not observed figures.)*

Code paths: **cache hit** — all four guards pass, `allProduction` true, tokens populated. **Cache miss** —
`getRatesForCounty` returns an empty list, `nonTobacco.isEmpty()`, so `allProduction` initialises `false` and
the whole block is skipped; empties hold. **Unentitled** — `ichraEntitled` false short-circuits the `if`
before any query is issued; **no rate query runs at all**. **No intake row** — `intake == null` short-circuits
the same `if`, and T128's four tokens are independently empty for the same reason.

⚠️ **Note the T128 row behaviour in column 3:** county/headcount still resolve for an unentitled agency.
That is T128's existing, deliberate behaviour and is unchanged here — those are the employer's own inputs,
not market data. **Only the market-data group is entitlement-gated.**

**4. Runtime walk: NONE PERFORMED.** No Tomcat is reachable from this container and no WAR was deployed.
**A container limitation, not evidence about any environment.**

**5. No outbound call:** §5, established three independent static ways.

---

## 9. ⚠️ Code-verified-only disclosure

Specific. The build compiled; nothing ran.

1. **No token has ever rendered.** The formats above (`$489.38`, `July 31, 2026`) are what
   `NumberFormat.getCurrencyInstance(Locale.US)` and `DateTimeFormatter.ofPattern("MMMM d, yyyy")` produce by
   contract — **read from the JDK's behaviour, not observed in output.**
2. **The provenance gate has never been exercised against a staging row.** Whether any environment currently
   holds `source_env='STAGING'` rows was not queried. If one did, this suppresses the whole group — correct,
   but unobserved.
3. **That a warmed county actually has rows at all three of 21/40/64.** Argued from `RateCacheWarmService`
   looping `AgeCurve.MIN_AGE..MAX_AGE`; no county's row set was inspected. A partial warm would blank
   individual floors while counts still resolved — the failure is graceful but unverified.
4. **That `plan_count`/`carrier_count` are non-null in practice.** Both columns are nullable; the code
   null-checks each, but no row was read to see whether the warm job populates them.
5. **The age-40 count-row rule is copied from the illustration's stated reasoning, not independently
   verified.** [IllustrationServlet.java:290-297](../../src/main/java/net/superiorstate/ams/controller/market/IllustrationServlet.java:290)
   says counts are age-specific because catastrophic plans are under-30 only. **I did not confirm that against
   HealthSherpa data** — I matched the existing decision rather than re-deriving it, which is the right move
   for consistency but means the underlying claim is inherited, not checked.
6. **The unentitled and cache-miss columns of §8.3.** Both are short-circuit arguments from reading the `if`;
   neither was executed.

**The walk that would confirm the important ones:** on a warmed Texas county with an `ichra_enabled` agency,
open the public `/proposal/{guid}` and confirm figures render with an as-of date; then set
`agency.ichra_enabled = 0`, **restart Tomcat** (T64 — EclipseLink can serve a stale `Agency`), reload, and
confirm the market-data tokens are blank **in `view-source:`** while county/headcount still resolve.

---

## 10. Registered assumption, carried from S10-F → filed as **T131**

**S10-F's safety argument rested on a premise that is now false.** It held that no existing `CUSTOM` section
could be caught by T129's discriminator, because V086 shipped `is_plus_tier` `NOT NULL DEFAULT 0` **with no
backfill**, so no LOS carried the flag.

**On 2026-08-03 the flag was set on SWBD PremiumPath Program**, which already carries at least six `CUSTOM`
sections — BenefitBridge, QSEHRA Lite, SWBD, PremiumPath, HSA Plus, Lifestyle. **Those six are now omitted
from any proposal whose originating agency lacks `agency.ichra_enabled`.** T129 is working as designed; the
premise about the data underneath it changed.

- **Accepted by Kevin, 2026-08-03**, because SWBD is not yet using the site, so no live proposal is affected.
- **Confirm-before trigger:** any agency *without* `ichra_enabled` beginning to use proposals carrying those
  sections.
- **Reversal cost:** narrow T129's discriminator — require both a plus-tier LOS association *and* a marker
  distinguishing market-data sections from ordinary ones. Cheap while only SWBD is affected; **grows with
  every section added to a plus-tier LOS.**

Filed as **T131**. Not an `LA-NN`: it rests on no reading of authority, only on a data fact that changed.

---

## 11. Backlog

Highest existing row before this run was **T129**.

| T | Action |
|---|---|
| **T130** | **Filed and closed as built.** The seven market-data tokens, the cache-only proof, the four fail-closed conditions, the reused illustration decisions, and the LA-17 constraint-3 content warning. |
| **T131** | **Filed, open, accepted risk.** §10 in full — premise, consequence, acceptance, trigger, reversal cost. |

---

## 12. Note to Kevin — every `{{ICHRA_*}}` token now available

T128's, plus this run's. All eleven degrade to blank rather than to visible `{{…}}` markup.

**Employer's own inputs — render for any agency, entitled or not:**
```
{{ICHRA_COUNTY}}
{{ICHRA_COUNTY_FIPS}}
{{ICHRA_HEADCOUNT}}
{{ICHRA_PLAN_YEAR}}
```

**Market data — blank unless the agency is ICHRA-entitled AND the county is warm AND every row is PRODUCTION-sourced:**
```
{{ICHRA_PLAN_COUNT}}
{{ICHRA_CARRIER_COUNT}}
{{ICHRA_FLOOR_AGE_21}}
{{ICHRA_FLOOR_AGE_40}}
{{ICHRA_FLOOR_AGE_64}}
{{ICHRA_RATES_AS_OF}}
{{ICHRA_RATES_SCOPE}}
```

- `{{ICHRA_FLOOR_AGE_*}}` are the **lowest available** monthly premium at that age — a floor, formatted
  `$489.38`. Write them as *"starting at"*, never as *"the price"*.
- `{{ICHRA_RATES_SCOPE}}` is a complete pre-written sentence naming the county and date and stating the
  figures are off-exchange only and not a quote. Drop it in near the figures; it self-suppresses when there
  are none.
- ⚠️ **Put market data in a *different* `CUSTOM` section from the tier-1 administration content.** LA-17
  constraint 3 requires the agent's voice and SSA's to be structurally separate in the document; one section
  carrying both erodes the distinction the whole assumption rests on. §6.
- ⚠️ **A cold county renders every market token blank.** Texas counties are warmed per the rate-cache admin
  page; a county nobody has warmed shows nothing rather than zeros.
- **`{{ICHRA_STATE}}` still does not exist** (carried from S10-E) — one line to add if the prose needs it.

---

## 13. SQL close-out audit

**This run produced, ran, and recommends no SQL.** No `.sql` file created, edited or deleted; no DDL, no DML,
no migration. The feature reads `rating_area_rate_cache` (V074/V078) through an existing DAO method that was
**not modified**, and `proposal_ichra_intake` (V087) through the accessor T128 already added.

**Current highest version, `ls docs/migrations/*.sql`: 64 files, highest `V087__proposal_ichra_intake.sql`** —
unchanged by this run. Applied to production 2026-08-03 per Kevin's report; **this run did not verify that** —
no `mysql` client is reachable from this container, a container limitation and not evidence about any
environment's state.

---

## 14. Commit

Read from `git log -1` **after** the push:

```
0a1a783 2026-08-03 14:05:59 -0500 feat: market-data merge tokens for plus-tier proposal sections
```

`git show --stat` on `0a1a783` — **proving no file outside the fence was touched**:

```
 docs/analysis/project_backlog.md                   |   2 +
 docs/runs/S10-G_closeout.md                        | 388 +++++++++++++++++++++
 .../controller/activity/setup/ViewProposal.java    | 139 +++++++-
 3 files changed, 527 insertions(+), 2 deletions(-)
```

Three files, all named in §3's permitted list. The only file under `src/` is `ViewProposal.java`; the two
deletions in the changeset are its replaced signature and call-site lines.

⚠️ **Same disclosed deviation as S10-A/B/D/E/F:** a close-out cannot carry its own commit's hash and be
inside that commit, so §14 is filled in by a **second** commit touching only `docs/runs/S10-G_closeout.md`,
already inside the fence, rather than by amending a pushed commit.

---

## 15. Compliance statement

**Scope fence, restated.** Writable: `ViewProposal.java`, `docs/analysis/project_backlog.md`,
`docs/analysis/legal_assumptions.md` *(only if §5 forced a new `LA-NN` — **it did not, and the file was not
written**)*, and `docs/runs/S10-G_closeout.md`. **Nothing outside it was written**, proven by the
`git status --short` immediately before staging showing exactly three paths.

**⚠️ No code path reachable from proposal render can call HealthSherpa.** Established three independent
static ways in §5: `HealthSherpaService` is referenced by only three files, none of them on this path;
`RateCacheDAO` imports no network-capable type; and a grep for every HTTP primitive across the DAO and the
entity returns zero hits. The added chain terminates in `em.createQuery(...).getResultList()`. **No lazy
warm, no fallback fetch, no retry-with-fetch.**

**`IchraAccessResolver` was called, not modified** — this run consumes the `ichraEntitled` boolean already
resolved at [:109](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:109),
passing it as a parameter. No second resolver call was added and `IchraAccessResolver.java` was never opened
for writing.

**T129's filtering and `ICHRA_GATED_SECTION_TYPES` are unchanged** — byte-identical; this run adds a second,
independent gate on the *values*, deliberately duplicating protection rather than relying on the section
filter.

**`proposal_ichra_snapshot`, `source_env` handling, and the `ICHRA_ILLUSTRATION` branch are unchanged.**

**No migration, entity, or JSP was created or changed.** The only file touched under `src/` is
`ViewProposal.java`.

**Git operations this session:** the five read-only preflight commands, `status`/`diff --stat` while working,
and the close-out `add`/`commit`/`push`/`log`. No `git add -A`, no tag, no stash, checkout, restore or reset.
