# Session 19-E Close-out — Build T165: the ICHRA proposal payload

Date: 2026-08-05. Branch: `refactor/modernize-architecture`. Executes
`docs/analysis/S19D_ichra_payload_spec.md` §5 (spec committed `7d41b8a`).

## 1. Shipped

| Hash | What |
|---|---|
| `abf6621` | `feat: S19-E -- ICHRA proposal payload, MEDIUMTEXT storage and three render tokens` — `ProposalIchraSnapshot.java` (field, accessors, javadoc correction), `ProposalBuilder.java` (write path), `ViewProposal.java` (read path, three tokens), `docs/migrations/V090__proposal_ichra_payload.sql` |
| *(this commit)* | `docs: S19-E -- migration registry and tracker for the ICHRA payload migration` — `docs/analysis/migration_tracker.md`, `docs/schema_version_migration.sql`, this close-out. Its own hash is unknowable from inside itself (the usual git self-reference limit); read it via `git log -1 --format=%h` after this commit lands — reported in this run's own chat output, not restated here. |

V090 is applied to local dev (`beta_ssa`, work workstation) — not to any other environment, including
production. See §8.

## 2. In flight

Nothing. Both commits above are pushed (§ compliance statement has the push output).

## 3. Decisions made

1. **`ageBands` stays `null` for `RANGE`-mode proposals** (S19-E §2, made before any code was
   written). `attachRangeSnapshot` gained no per-age loop; nothing was synthesized. Reasoning
   unchanged from the prompt: session 18 decision 6 established `RANGE` mode stores neither per-age
   premiums nor per-band lives, and inventing them would misrepresent what the mode actually
   collected. Confirmed in the code: `buildIchraPayload`'s `bands` parameter is passed `null` at
   `attachRangeSnapshot`'s call site and the method's own null/empty check serializes `ageBands` as
   JSON `null` in that branch — never an empty array standing in for "not collected."
2. **Migration version: V090** (`docs/migrations/V090__proposal_ichra_payload.sql`). Read directly
   from `ls docs/migrations/` at build time — V089 was still the highest immediately before this
   run, confirming the spec's own placeholder was still unclaimed.
3. **The affordability block's single-figure resolution — a decision this run had to make that
   neither the spec nor this prompt settled.** While building `buildAffordabilityBlock`, reading
   `IllustrationServlet.computeAffordability` (`:541-597`) surfaced that this codebase's *only*
   existing affordability computation is **per age band** — each `AgeBandRow` gets its own
   `onexLcspPremium`, its own reference income, its own flip contribution. The spec's committed
   schema (§3, already shipped in `7d41b8a`) defines `affordability` as **one top-level block**, not
   an array — a shape mismatch this run did not have authority to redesign (§2 of this prompt: "No
   other design decision is open"). Resolved by evaluating affordability at **age 40** as the single
   representative age — reusing, not inventing, the exact convention `ViewProposal.putIchraMarketTokens`
   already established for the identical problem ("one figure needed for a whole group": `byAge.get(40)`
   fallback, T130, confirmed during S19-D's own research). The constant lookups
   (`ICHRA_AFFORDABILITY_PCT_<planYear>`, `FPL_ANNUAL_<planYear>` via `AppConstantDAO`) and the
   `AffordabilityCalculator.flipContribution` call mirror `IllustrationServlet`'s existing code
   exactly — same constant names, same fail-closed behaviour on a missing constant. New request
   parameters this introduces: `affordabilityBasis` (`"FPL"`/`"INCOME"` — same vocabulary
   `IllustrationServlet` already uses for its own `affordabilityBasis` parameter, for consistency)
   and `annualIncome` (singular — the payload's single-block shape has no per-band equivalent of
   `IllustrationServlet`'s `income1..6`). **No JSP currently submits either parameter**, so this
   entire block resolves to `null` in every proposal built today — code-complete and dormant until a
   future build adds the intake fields, exactly matching how `planLandscape` is dormant until T166.
4. **`planLandscape` is unconditionally `null`.** No HealthSherpa call exists anywhere in this run.
   Matches this prompt's explicit "not build T166's plan fetch" instruction and the spec's own
   framing of `planLandscape` as populated only once T166 supplies plans.
5. **The `ProposalIchraSnapshot` class javadoc was corrected**, per the spec's explicit instruction —
   the old "deliberately carries no affordability figure of any kind" sentence is gone; the new
   text keeps the audience-guarantee reasoning (`/proposal/*` public/unauthenticated, LA-12) but
   states plainly that the figure is now carried in storage while the public token set still
   withholds it.

## 4. New assumptions, and reversal cost

| Assumption | Reversal cost |
|---|---|
| Affordability is evaluated at age 40 as the group's single representative age, reusing `ViewProposal`'s own existing convention rather than a per-band array | Low — this is a serialization-logic choice inside `buildAffordabilityBlock`, not a schema commitment; changing the representative age, or moving to a per-band array, is a code change with no migration, though it would be a schema-version bump (`schemaVersion: 2`) since the JSON shape itself would change |
| `affordabilityBasis`/`annualIncome` are the right request-parameter names for a future JSP to submit, chosen to mirror `IllustrationServlet`'s existing `affordabilityBasis` vocabulary | Low — nothing reads these parameters today outside this run's own new code; renaming them costs nothing since no form yet submits them |
| A raw HTML `<table>` (no CSS classes beyond `ichra-age-band-table`/`ichra-plan-landscape-table`) is an acceptable default rendering for the two new HTML-block tokens, with styling left to whatever CSS a `CUSTOM` page or the PSP's own stylesheet supplies | Low — display-only; changing the table markup later is a code change confined to `putIchraPayloadTokens`, no data migration |
| `RateCacheDAO.getRate(em, planYear, countyFips, 40, false)` (a fresh cache lookup, not reusing `age21Row`/`age64Row`/the per-band loop's `row`) is an acceptable extra query, gated so it only runs when `affordabilityBasis` is actually present | Low — today `affordabilityBasis` is never present, so this query never executes; if it later runs on every proposal build it is one indexed lookup, not a loop |

## 5. Open questions — both BLOCKED items carried forward, unresolved

1. **The `FlaggedEnhancementResolver` predicate** (spec §5 BLOCKED item 1). Untouched by this run,
   per instruction. `payload_json` now exists for it to read, once T164/T166 decide how.
2. **Public-page exposure of the `affordability` block** (spec §5 BLOCKED item 2). No token was
   defined, registered, or wired for it — confirmed by inspection of `putIchraPayloadTokens`: it
   builds exactly three tokens (`ICHRA_AGE_BAND_TABLE`, `ICHRA_PLAN_LANDSCAPE_TABLE`,
   `ICHRA_PAYLOAD_AS_OF`) and nothing in the method ever reads `payload.get("affordability")` at
   all. No `LA-NN` entry was filed — per this prompt's own instruction not to file one, this is
   named here as still needed, not created.

## 6. Contradictions found

1. **The spec's single-block `affordability` schema does not match this codebase's only existing
   affordability computation, which is per age band.** Recorded in full in §3 decision 3 above —
   `IllustrationServlet.computeAffordability` computes one flip-contribution *per age*, not one for
   a whole group. The spec (`7d41b8a`, already committed and authoritative per this run's own §0)
   commits to a single top-level block regardless, so this run implemented that shape using the
   age-40 representative-age convention rather than redesigning the schema — a build-time
   resolution of an ambiguity the spec left open, not a deviation from the spec's actual JSON shape.
   **Flagging this explicitly because it's the kind of thing a later reader building on the payload
   (T164, a per-band UI) needs to know**: the payload's `affordability` block is a single
   group-level estimate at age 40, not a per-employee or per-band figure, and any future UI that
   implies otherwise would be overstating the block's precision.
2. No other contradiction found. Every anchor cited in the spec (§1's line-number citations for
   `attachRangeSnapshot`/`attachAgeBandSnapshot`, `putIchraMarketTokens`'s call site, the
   `ProposalIchraSnapshot` column list, the `Proposal`/`PlanSummary` field lists) matched the
   repository exactly at the point this run read it, each confirmed unambiguous before editing
   (§5 of this prompt's own anchor discipline).

## 7. Next

Kevin reviews and, if the age-40/single-block affordability resolution (§3 decision 3) doesn't sit
right, that's the one design choice in this run genuinely worth a second look — everything else is
mechanical execution of the already-committed spec. After that: the two BLOCKED items are real
follow-on work (T164/T166 for the resolver predicate; an `LA-NN` entry, Kevin's or counsel's call,
before any affordability token is ever added) — neither is this run's or this close-out's to resolve
further. Recommend Kevin walk §5's four verification steps from the spec next, since nothing in this
run proved any of them (see §9).

## 8. SQL close-out audit

- **Every SQL statement produced:** `docs/migrations/V090__proposal_ichra_payload.sql` (created,
  full text matches spec §2's DDL verbatim except the version/date, which were confirmed live rather
  than assumed) — **is** a versioned migration, registered in both `docs/analysis/migration_tracker.md`
  and `docs/schema_version_migration.sql` in this same run.
- **Run this session, against local dev only:** the two gap-check queries (`SELECT version FROM
  schema_version WHERE version = 'V090'`; `SHOW COLUMNS FROM proposal_ichra_snapshot LIKE
  'payload_json'` — both zero rows, confirmed before applying), the migration file itself (applied
  via `mysql.exe < V090__proposal_ichra_payload.sql`, exit 0), and the post-apply verification
  (`SELECT version ...` → `V090`; `SHOW COLUMNS ...` → `payload_json | mediumtext | YES | | NULL |`;
  `SELECT COUNT(*) FROM proposal_ichra_snapshot` → `0`). No password was printed — `MYSQL_PWD` was
  extracted inline from `C:\ssa\ssa.properties` and unset immediately after each invocation, same
  procedure S19-C established.
- **Orphaned `.sql` files:** none. `V090__proposal_ichra_payload.sql` is registered in both the
  tracker and the schema-version registry script in this same run — nothing left dangling.
- **Current highest migration version, read from `docs/migrations/`:** **V090**
  (`docs/migrations/V090__proposal_ichra_payload.sql`, this run's own file — confirmed by directory
  listing immediately before naming it, and unchanged since).
- **What is pending deployment:** **production has not received this migration.** V090 is applied to
  local dev (`beta_ssa`, work workstation) only — the tracker's V090 row reads `beta_ssa
  (work)=✅, beta_ssa (home)=⬜, dev_ssa=⬜, Production=⬜, Demo/BPO/Master=N/A`. No deploy, no
  release, no tag was touched by this run.
- **Schema described but not scripted:** none remaining from T165's storage half — the column now
  exists, is registered, and is applied locally. The *predicate* half (what
  `FlaggedEnhancementResolver` does with the column) remains unscripted by design — that is
  BLOCKED item 1, T164/T166's problem, not a gap in this run's own SQL.

## 9. Compliance statement

**Preflight output, verbatim:**
```
$ git rev-parse --abbrev-ref HEAD
refactor/modernize-architecture

$ git log -1 --oneline
7d41b8a docs: S19-D -- Phase A spec, ICHRA proposal payload shape and storage

$ git status --short
(no output — clean)

$ git pull --ff-only
Already up to date.
```

**Anchor discipline (§5 of this prompt):** every edit target was located semantically and printed
before editing, per this run's own transcript — `ProposalIchraSnapshot.java`'s javadoc block, its
`createdBy` field/accessor pair, `ProposalBuilder.java`'s two `save(...)` call sites and their
surrounding ~15 lines, `ViewProposal.java`'s `putIchraMarketTokens` call site and its method body's
end. Every anchor matched exactly once; none required a fallback.

**Scope fence compliance:** exactly the five files §4 permitted were edited, plus the one migration
file §4 permitted created, plus this close-out. `git diff --stat` after the build showed no file
outside that list. `FlaggedEnhancementResolver.java`, `Enhancement.java`, `AffordabilityCalculator.java`
(called, not modified — confirmed by `git diff` showing zero changes to that file), and every other
named-off-limits file were not touched. No T-number was assigned. No `LA-NN` number was assigned. No
literal LOS/ServiceItem/PlanType/ServiceModule/RateTable ID appears anywhere in this build's diff
(confirmed by reading the diff — the only IDs referenced are `proposal.getId()` and
`enhancement.getId()`-style runtime accessors, never a literal). No employee-identifying field was
added — `ageBands` carries `age`/`lives`/`premium` only, matching `ProposalIchraSnapshotBand`'s own
existing shape exactly.

**Code-verified vs. runtime-verified.** Code-verified this run: the project compiles (`BUILD SUCCESS`),
the migration applies cleanly to local dev and the resulting column/registration match what was
written, and the diff is confined to the intended files. **Runtime-verified: none of the spec's four
verification steps.** No proposal has been built through the illustration hand-off with this code
running in a live servlet; no `payload_json` value produced by `attachRangeSnapshot`/
`attachAgeBandSnapshot` has been inspected; no `/proposal/*` page has rendered
`ICHRA_AGE_BAND_TABLE`/`ICHRA_PLAN_LANDSCAPE_TABLE`/`ICHRA_PAYLOAD_AS_OF`; no confirmation that the
public page shows zero affordability content regardless of payload contents. Compiling is not
rendering — all four steps are Kevin's walk, unperformed by this run, exactly as the prompt said
they would be.
