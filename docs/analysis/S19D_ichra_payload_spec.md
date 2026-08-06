# S19-D — Phase A: T165, the ICHRA proposal payload (shape and storage)

Date: 2026-08-05. Branch: `refactor/modernize-architecture`. This is a build spec, not a report — a later
Sonnet run executes §7 with no further design decisions. Everything above §7 is the reasoning that
produced it; everything in §7 is what that run actually types.

---

## 1. Read first, decide second — findings

### 1.1 `ProposalIchraSnapshot` / `ProposalIchraSnapshotBand`

`src/main/java/net/superiorstate/ams/model/sales/agency/ProposalIchraSnapshot.java` — `@Entity`,
table `proposal_ichra_snapshot`. Columns (all present today, lines 44-97): `snapshot_id` (PK,
generated), `proposal_id` (`@OneToOne`, `unique = true`, `nullable = false` — **one row per proposal,
not a scenario system**), `mode` (`varchar(16)`), `county_fips` (`char(5)`), `state` (`char(2)`),
`county_name` (`varchar(100)`), `plan_year`, `contribution`, `headcount`, `group_monthly_low`,
`group_monthly_high`, `group_net_total`, `employer_outlay`, `source_env` (`varchar(16)`),
`rates_fetched_at`, `snapshot_at`, `created_by`. **No text/CLOB column exists.** `ProposalIchraSnapshotBand`
(`ProposalIchraSnapshotBand.java`) is a `@ManyToOne`-to-snapshot child row: `age`, `lives`,
`floor_premium`, `net_per_employee`, `band_net`, `sort_order` — all `nullable = false`, only populated
for `mode = AGE_BAND`.

**⚠️ Load-bearing finding, not stated in this prompt.** `ProposalIchraSnapshot`'s own class javadoc
(lines 24-30) states it **"Deliberately carries no affordability figure of any kind — `/proposal/*` is
public and unauthenticated (LA-12), a materially weaker audience guarantee than the illustration page
behind `IchraAccessResolver`."** This is not incidental — it is recorded a second time, independently,
in `docs/analysis/legal_assumptions.md`'s LA-15 entry (line 944) and in
`docs/session_closeout_2026-08-01_session6.md` (line 818): *"`proposal_ichra_snapshot` (V079) is
structurally incapable of carrying [an affordability figure]."* Session 18 decision 3 (the decision
this run is asked to execute) chose to add an affordability figure to the payload anyway, reasoning
that "neither `ProposalIchraSnapshot` nor `ProposalIchraSnapshotBand` has a column for either" — true,
but the decision record does not address *why* that was true, or reopen LA-12's audience reasoning.
See §5 (Contradictions found) — this run does not resolve it, but does not paper over it either.

### 1.2 The snapshot write path

`ProposalBuilder.java:558-577` (`attachIchraSnapshotIfPresent`) is the dispatcher: reads `countyFips`,
`mode`, `planYear` from the request; returns immediately (writes nothing) if any is missing/unparseable
or the county doesn't resolve. Dispatches to `attachAgeBandSnapshot` (:659-738) or `attachRangeSnapshot`
(:605-656) by mode; any other mode value writes nothing.

Both writers are **fail-closed, all-or-nothing**: a missing headcount, a missing/negative contribution,
an unresolvable rate-cache row, or a non-`PRODUCTION` `sourceEnv` (absent the demo override) means the
method returns with **no row written at all** — never a partial snapshot.

The two guards attributed to session 18:
- **`ICHRA_DEMO_ALLOW_STAGING_PROPOSAL`**: `isIchraDemoOverride` (:596-602), reads
  `AppConfig.isIchraDemoStagingAllowed()` (the ssa.properties flag) **AND** `session.getAttribute("isPspAdmin")`
  — both required, `getSession(false)` so the check itself never creates a session.
- Confirmed exactly as session 18 described: **the snapshot is still stamped with its real `sourceEnv`**
  (`row.getSourceEnv()`, never hardcoded to `PRODUCTION`) even when the demo override admits a
  staging-sourced write — so `ViewProposal`'s public-path gate (`G5`) still refuses it permanently. The
  override only lets the write path be exercised before real rate data exists; it never produces a
  client-facing figure.

`sourceEnv` is set from whichever `RatingAreaRateCache` row supplied the figures actually used
(`row.getSourceEnv()`, lines 617/714) — not a constant, not request-supplied.

### 1.3 `replaceTokens` — the substitution mechanism

`ViewProposal.java:878-905`. **Correction to this prompt's premise:** tokens are **not** `${...}`-shaped.
The actual syntax is `{{TOKEN_NAME}}`, case-insensitive
(`"(?i)\\{\\{" + Pattern.quote(key) + "\\}\\}"`, line 883), substituted via
`Matcher.quoteReplacement` (so a `$` or `\` inside a token's *value* is not re-interpreted — the one
escaping behaviour present). There is **no fixed list of token names anywhere** — the token set is
whatever a given render call happens to `tokens.put(...)` into the `Map<String, String>` passed in;
each name is a string literal at its own call site (`buildTokenMap`, :647-736, and helper methods like
`putIchraMarketTokens`, :774-859). Convention observed across every existing entry: `ICHRA_` prefix,
upper-snake-case (`ICHRA_PLAN_COUNT`, `ICHRA_FLOOR_AGE_21`, `ICHRA_CONTRIBUTION_MONTHLY`, etc.).

Line number: session 18 cited `:876-890`; the method is actually at `:878-905` — two lines of drift,
not a contradiction, likely from intervening edits. Confirmed correct in substance: **T133's residual-token
stripping is real** (:886-903) — any `{{WORD}}`-shaped string left unmatched after substitution is
logged (`log.warn`) and stripped to `""` before render, so an unmapped token can never reach the
customer-facing HTML as a literal `{{...}}` string, and a lookup failure inside a token-builder
(`putIchraMarketTokens`'s own `try/catch`, :844-848) degrades to empty-string tokens rather than
propagating an exception.

### 1.4 `FlaggedEnhancementResolver`

`src/main/java/net/superiorstate/ams/data/resolver/FlaggedEnhancementResolver.java` — confirmed
unchanged from what S19-A/S19-B read: `isSectionEnabled(EntityManager em, Proposal proposal, Enhancement
enhancement)` returns `true` immediately for `enhancement == null` or `!enhancement.isSystemManaged()`
(the bit-identity guarantee); for a flagged enhancement it currently always executes the stub —
`return false;` (line 56) — withholding the section unconditionally. Fails closed on any exception.

**What a build run changes:** per the row filed for this exact class
(`docs/analysis/project_backlog.md` T165), *"the resolver's signature already accepts `em` and
`proposal`, unused by the stub today, specifically so this build never requires a second edit to
`ViewProposal.java` — implementing this row only changes the resolver's step-3 body."* Concretely: the
`return false;` at line 56 becomes a lookup — `ProposalIchraSnapshotDAO.findByProposalId(em,
proposal.getId())`, `null` → still `false` (no payload written, stays withheld, same fail-closed
posture as today); non-null → deserialize `getPayloadJson()` and decide.

**What this run does not decide, and flags rather than invents:** the *predicate* — which payload
sub-block must be present for a *given* flagged enhancement's section to enable — cannot key on
`enhancement.getId()` (Rule 4: no hardcoded reference-row IDs; `enhancement` is PSP-scoped, so an ID
literal would be wrong on a second installation). Nothing in the schema this run specifies (§3) resolves
that mapping. It is T164/T166 territory, named here as a **blocked** sub-question in §7.

### 1.5 The affordability inputs (V078)

`RatingAreaRateCache.java:51-56/161-173` — `onexLcspPremium`, `onexBenchmarkSilverPremium`: both
`BigDecimal`, `@Column` with no `nullable = false`, i.e. **nullable**, matching
`docs/migrations/V078__rate_cache_onex_lcsp.sql` exactly: `DECIMAL(8,2) NULL`, no default, no backfill.
Written today by `RateCacheWarmService.java:387-388`
(`row.setOnexLcspPremium(scaleOrNull(...))` / `setOnexBenchmarkSilverPremium(scaleOrNull(...))`) — the
warm job's on-exchange derivation, added after V078. Read by
`AffordabilityCalculator.flipContribution(onexLcspPremium, applicablePct, annualIncome)` (pure function,
no I/O), which returns the contribution level at or above which the offer becomes affordable (clamped
to zero).

### 1.6 `Proposal`'s own text-bearing columns

`src/main/java/net/superiorstate/ams/model/sales/agency/Proposal.java` — full field list read (lines
14-62): `id`, `prospect`, `rate`, `dateCreated`, `isInactive`, `applicationGUID` (`varchar(36)`),
`status` (`varchar(20)`), `createdBy`, `dateSent`, `dateViewed`, `dateApplied`, `losList`
(`@ManyToMany`), `application` (`@OneToOne`, mapped by `Application`), `sourceActivity`. **No text/CLOB
column, no unused nullable column of any kind.** Confirms §3 Option B would require a genuinely new
column on a table every line of service already reads via `losList`/`application`/`sourceActivity` —
not a rider on existing slack.

### 1.7 `PlanSummary` (page 7 / T166 inputs)

`HealthSherpaService.java:306-346` — static nested class, fields: `hiosId`, `name`, `metalLevel`,
`grossPremium` (`Double`), `premium` (`Double`), `issuerName`, `hsaEligible` (`Boolean`), `ichraOnly`
(`Boolean`). Parsed from `hios_id`, `name`, `metal_level`, `gross_premium`, `premium`, `issuer.name`
(nested, preferred) / `issuer_name` (legacy fallback), `hsa_eligible`, `ichra_only`. Confirmed already
on the wire per `/api/v1/quotes` — matches T166's backlog description exactly.

`PER_PAGE = 100`, `MAX_PAGES = 20` (lines 47/52) — confirmed, gives a **2,000-plan cap**, treated as a
failure if reached (line 118). Reference county (Hopkins TX, FIPS 48223) returned 65 plans per V078's
migration header. §3 sizes against the cap, not the reference.

---

## 2. Storage decision — Option A

**New `MEDIUMTEXT` column, `payload_json`, on `proposal_ichra_snapshot`.**

**Why not B (column on `Proposal`).** §1.6 confirms `Proposal` has no slack text column and is read by
every line of service through `losList`/`application`/`sourceActivity` joins — widening it for a
feature specific to ICHRA-flagged proposals is exactly the shared-surface cost this run was told to
weigh, and there is no offsetting benefit: nothing about the payload needs to be visible without also
joining to the ICHRA snapshot (the mode/county/planYear/sourceEnv columns the payload's own provenance
depends on, per §3, all already live there).

**Why not C (new table).** `proposal_ichra_snapshot` is already a `UNIQUE`-on-`proposal_id`,
1:1, write-once-at-build entity — the exact shape the payload needs. A new table would duplicate that
cardinality constraint, need its own DAO and its own lifecycle, for data that lives and dies with the
same row the snapshot already represents. Rejected on the same "don't build a new abstraction the
existing one already models" grounds this codebase's own conventions apply elsewhere (`ProposalDetail`
duplicating rather than extracting the section pipeline, session 18 decision 9, is the precedent for
*not* over-abstracting here, read the other direction — but the underlying discipline, avoid new
machinery a rider modification already covers, is the same one).

**Why A is more than "the smaller of three tables."** It rides the existing entity's existing
invariants for free: `@OneToOne` uniqueness already enforces one payload per proposal; the existing
fail-closed write discipline (§1.2) already governs when a row is written at all; `ProposalIchraSnapshotDAO.save`
(existing, unmodified) already wraps the write in one transaction. Adding a column costs nothing the
entity doesn't already pay for.

**Sizing arithmetic.** Worst case is `MAX_PAGES × PER_PAGE = 2,000` plans (§1.7), not the 65-plan
reference county. Estimating one `PlanSummary` JSON object generously — `hiosId` (~15 chars), `name`
(~60 chars for a verbose plan name), `metalLevel` (~10), `grossPremium`/`premium` (~8 each),
`issuerName` (~40), `hsaEligible`/`ichraOnly` (~5 each), plus JSON punctuation/keys — comes to roughly
250-300 bytes per plan object. `2,000 × 300 bytes ≈ 600 KB` for the plan-landscape block alone;
age-band and affordability blocks add at most a few KB. MySQL `TEXT`'s 65,535-byte (~64 KB) ceiling is
**under the worst case by roughly 9×** — the exact trap this run was told not to walk into by sizing
against the reference county. `MEDIUMTEXT` (16,777,215 bytes, ~16 MB) clears the 600 KB worst case by
more than 25×, with ample headroom for the smaller blocks and for `name`/`issuerName` values longer than
estimated. `LONGTEXT` is not warranted — nothing in this shape approaches megabytes.

**Migration DDL** (text only — this run creates no `.sql` file):

```sql
-- V090: ICHRA payload JSON column on proposal_ichra_snapshot (proposal_ichra_snapshot.payload_json)
--
-- T165. Carries the JSON payload FlaggedEnhancementResolver needs to render a system_managed
-- enhancement's scoped section, plus the affordability, age-band, and plan-landscape data neither
-- proposal_ichra_snapshot's nor proposal_ichra_snapshot_band's existing columns carry.
-- See docs/analysis/S19D_ichra_payload_spec.md.
--
-- NULLABLE, NO DEFAULT, NO BACKFILL -- every row written before this column existed reads NULL;
-- FlaggedEnhancementResolver's null-check (unchanged fail-closed behaviour) treats that identically
-- to "no ICHRA hand-off at all". Every snapshot row written by the two existing fail-closed writers
-- (attachRangeSnapshot / attachAgeBandSnapshot) after this ships always populates at least
-- {"schemaVersion":1,"provenance":{...}} -- provenance is derivable from inputs the writers already
-- require to reach ProposalIchraSnapshotDAO.save() at all, so an empty-but-non-null payload is the
-- floor, not NULL.
--
-- Sized at MEDIUMTEXT against the 2,000-plan worst case (PER_PAGE=100 x MAX_PAGES=20,
-- HealthSherpaService.java:47/52), not the 65-plan reference county -- see spec sec 2 for the
-- arithmetic. TEXT's 64KB ceiling is undersized for that worst case by roughly 9x.
--
-- Prerequisites: V079 (proposal_ichra_snapshot).

ALTER TABLE proposal_ichra_snapshot
    ADD COLUMN payload_json MEDIUMTEXT NULL;

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V090' AS version, '<build date>' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V090', 'ICHRA JSON payload column on proposal_ichra_snapshot (payload_json, MEDIUMTEXT, nullable, no backfill)', 'V090__proposal_ichra_payload.sql', NOW());
```

`<build date>` and the version number `V090` are provisional — **the build run must re-read
`docs/migrations/` for the actual next-available version at build time**, per every prior spec's own
discipline; do not assume V090 is still unclaimed by the time this is built.

---

## 3. Payload schema (version 1)

```json
{
  "schemaVersion": 1,
  "provenance": {
    "sourceEnv": "PRODUCTION",
    "capturedAt": "2026-08-05T14:32:00",
    "ratesFetchedAt": "2026-08-04T09:00:00",
    "planYear": 2026,
    "countyFips": "48223",
    "countyName": "Hopkins"
  },
  "affordability": {
    "onExchangeLcspPremium": 705.37,
    "incomeBasis": {
      "type": "ENTERED",
      "annualIncome": 34000.00
    },
    "applicablePercentage": 0.0883,
    "subsidyPreservingCeiling": 450.00
  },
  "ageBands": [
    { "age": 21, "lives": 4, "premium": 382.93 },
    { "age": 40, "lives": 6, "premium": 489.38 }
  ],
  "planLandscape": {
    "capturedAt": "2026-08-05T14:32:00",
    "plans": [
      {
        "hiosId": "12345TX1234567",
        "name": "Silver PPO 2000",
        "metalLevel": "Silver",
        "grossPremium": 705.37,
        "premium": 689.12,
        "issuerName": "Example Health Plan",
        "hsaEligible": false,
        "ichraOnly": true
      }
    ]
  }
}
```

**Field notes:**

- **`schemaVersion`** (integer, required, starts at `1`). Read behaviour on an unrecognized version:
  a reader must never fail on an unknown value. Read only the fields the reader's own code knows about;
  fields the current build doesn't recognize (a future, higher version) are simply never accessed, not
  an error. A version *lower* than the reader expects (a schema that gained fields after this payload
  was written) means the newer fields are absent — read as `null`/missing, same as any other absent
  sub-block (§4). No payload this schema produces is ever rewritten to a newer version; the frozen
  point-in-time document (constraint from this run's own framing) means an old payload stays exactly
  as written.
- **`provenance`** is self-contained on purpose, duplicating `sourceEnv`/`planYear`/`countyFips`/
  `countyName`/`ratesFetchedAt` that already exist as real columns on the same `proposal_ichra_snapshot`
  row (§1.1). This is deliberate, not an oversight: per this run's own instruction, provenance must let
  "a staleness disclosure can be rendered from the payload alone" — i.e. without a second query against
  the snapshot row's own columns, and robust to the payload someday moving storage location.
  `capturedAt` is the payload's own write timestamp (mirrors `ProposalIchraSnapshot.snapshotAt`'s
  existing convention — same format, same source: `LocalDateTime.now()` at write time).
- **`affordability`** — see §5 (Contradictions found) for the audience tension this block sits inside.
  `onExchangeLcspPremium` is the raw input (`RatingAreaRateCache.onexLcspPremium`, §1.5) — the
  affordability *threshold's* underlying figure. `subsidyPreservingCeiling` is
  `AffordabilityCalculator.flipContribution`'s **output** — LA-15's "ceiling," a different number
  computed from the threshold, income, and `applicablePercentage`, **never derived by relabeling the
  threshold figure at render time.** `applicablePercentage` is carried for audit/transparency (so a
  later correction to the IRS-indexed constant, per LA-14's own recorded risk, doesn't leave a payload
  that can't be explained) — informational, not intended for direct display. `incomeBasis.type` is
  `"ENTERED"` (employer-supplied household income) or `"FPL_SAFE_HARBOR"` (the configured federal
  poverty line constant used as the reference income) — mirrors `AffordabilityCalculator.flipContribution`'s
  own `annualIncome` parameter doc: *"entered household income, or the configured FPL for the safe-harbor
  basis."* Whichever basis was used at write time is recorded, never re-derived later.
- **`ageBands`** — one entry per age actually present in the proposal-build request (mirrors
  `attachAgeBandSnapshot`'s existing `age1..age6` parameter loop, §1.2), **premium only** — the raw
  floor premium at that age, not a contribution-net figure. Net-of-contribution math for a "contribution
  scenario" enhancement is computed at render time from `premium` and the snapshot's own existing
  `contribution` column, not pre-baked into the payload — keeps the payload an input/base-output
  surface rather than pre-computing every hypothetical scenario a future interactive UI might want,
  matching the render-everything-store-nothing discipline LA-16 already established for sandbox-style
  employer interaction elsewhere in this feature.
- **`planLandscape.plans`** carries every `PlanSummary` field as parsed today (§1.7) — **including**
  `name` and `issuerName`. This run does **not** decide whether those two fields may ever reach a
  rendered page (O25, explicitly out of scope per this run's own instructions) — they are carried in
  storage because withholding them from the *payload* would make a future O25-permissive decision
  require a second HealthSherpa call and a schema version bump for no reason. The **display gate is a
  read-path decision, not a storage-path one** — see §4.

**Explicitly excluded, standing compliance boundary:** no SSN, no name, no date of birth, no any other
employee-identifying field. `ageBands` carries `age` and `lives` (a count), never a roster. This mirrors
`ProposalIchraSnapshotBand`'s own existing shape, which already made the same choice.

---

## 4. Write path and read path

### Write

New private method on `ProposalBuilder`, called from `attachIchraSnapshotIfPresent` (§1.2) after either
`attachRangeSnapshot` or `attachAgeBandSnapshot` builds (but before `ProposalIchraSnapshotDAO.save`
persists) the `ProposalIchraSnapshot` instance — see §5 for the exact call-site edit.

**Partial-payload policy — a decision this run makes, stated explicitly.** The *existing* snapshot row
is all-or-nothing (§1.2): if its required inputs are missing, no row is written at all. The **JSON
payload inside that row is not held to the same all-or-nothing standard, deliberately.** Each of
`affordability`, `ageBands`, and `planLandscape` is independently `null` (omitted, not an empty
placeholder) when its own required inputs are absent, while the other sub-blocks still populate if
their own inputs are present. **Reasoning:** session 18 decision 5 made the three special enhancements
independently toggleable — an employer proposal can reveal "group comparison" without "affordability."
Forcing the whole payload closed because one optional sub-block's inputs are missing would silently
break every *other* toggled-on enhancement on the same proposal. `provenance` is the one sub-block
that is never partial: it is derivable entirely from inputs the *existing* snapshot writers already
require to reach `save()` at all (mode, county, plan year, source env), so if a snapshot row is written
at all, `provenance` is always fully populated.

**Render-side consequence of a missing/absent sub-block:** every read-path token or HTML-block builder
(§4.2) treats an absent sub-block as "nothing to show," never as an error — same discipline
`putIchraMarketTokens`'s own `try/catch` (§1.3) already established.

### Read

**Tokens this spec defines**, all in the existing `ICHRA_` upper-snake-case convention, all built inside
`ViewProposal.java`'s existing token-building pass (alongside `putIchraMarketTokens`, §1.3), all
degrading to `""` — never a 500, never a literal `{{TOKEN}}` — when the payload is absent, unparseable,
or the relevant sub-block is `null`:

| Token | Source | Emits |
|---|---|---|
| `ICHRA_AGE_BAND_TABLE` | `payload.ageBands` | A server-built HTML `<table>` block (age / lives / premium columns), or `""` if `ageBands` is absent or empty. |
| `ICHRA_PLAN_LANDSCAPE_TABLE` | `payload.planLandscape.plans` | A server-built HTML `<table>` block (metal level, HSA-eligible, ICHRA-only, premium — **`name`/`issuerName` withheld from the built HTML pending O25**, even though both are present in storage per §3), or `""` if the block is absent or empty. |
| `ICHRA_PAYLOAD_AS_OF` | `payload.provenance.capturedAt` | A staleness-disclosure line, formatted like `ICHRA_RATES_SCOPE` already is (§1.3) — a plain sentence naming the capture date, never a bare timestamp. `""` if the payload itself is absent (no snapshot row, or the row predates this column and reads `NULL`). |

**Affordability tokens are deliberately not defined by this spec.** See §5 — creating a public,
unauthenticated-page token that emits `onExchangeLcspPremium` or `subsidyPreservingCeiling` walks
directly into the LA-12/LA-15 tension this run's own research surfaced, and this run is not the place
to resolve it (out of scope, same footing as O25). The `affordability` block is written to storage
(§4, Write) so it exists the moment a compliance decision permits displaying it, but **no token reads
it in this spec.** `ProposalDetail` (session 18 decision 8's designated authenticated-only surface,
T164, not built by this run) may read the block directly off the deserialized payload for PSP-admin
preview without going through the public token mechanism at all — that surface carries a materially
different audience guarantee than `/proposal/*`, the same distinction `ProposalIchraSnapshot`'s own
javadoc already draws (§1.1).

**A missing payload must never 500 a public proposal page.** All three tokens above are built inside
the same `try { ... } catch (Exception e) { log/degrade }` shape `putIchraMarketTokens` already uses
(§1.3) — a `null` `ProposalIchraSnapshotDAO.findByProposalId` result, a Gson parse failure, or a
malformed `schemaVersion` all degrade to `""` for every token this spec defines, identically.

**Out of scope, named rather than answered:** whether `HealthSherpaService.quoteSingleApplicant`
(page 7's live call) executes synchronously inside the proposal-create request is session 18's own
open question 2, unresolved, and is T166's to answer — this spec's `planLandscape.plans` array is
simply "populated when T166 populates it, empty otherwise," and every token above already treats an
empty `plans` array the same as an absent one.

---

## 5. Build-ready section

A later Sonnet run executes this with no further design decisions except the two named **BLOCKED**
items below.

**Files to create:**
1. `docs/migrations/V0NN__proposal_ichra_payload.sql` — **re-read `docs/migrations/` at build time for
   the real next version number; do not assume V090.** DDL text is in §2 above, verbatim except the
   version number/date/filename.

**Files to edit:**
1. `src/main/java/net/superiorstate/ams/model/sales/agency/ProposalIchraSnapshot.java` — add, following
   the exact style of every other column on this class (e.g. `sourceEnv`, lines 86-87/205-211):
   ```java
   @Column(name = "payload_json", columnDefinition = "MEDIUMTEXT")
   private String payloadJson;

   public String getPayloadJson() { return payloadJson; }
   public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
   ```
   Plain `@Column(columnDefinition = "MEDIUMTEXT")`, no `@Lob` — confirmed the established convention
   for every MEDIUMTEXT/TEXT/LONGTEXT column in this codebase (`Agency.landingHtml`,
   `NdtTestRun`'s four LONGTEXT columns, `ApplicationSection.htmlContent`, etc. — none use `@Lob`).
   **Also update the class javadoc** (lines 24-30) — it currently states this entity "deliberately
   carries no affordability figure of any kind." That sentence becomes false the moment this column
   ships with a populated `affordability` block; leaving it as-is would misdocument the entity for the
   next reader. Do not delete the audience-guarantee reasoning (`/proposal/*` is public/unauthenticated,
   LA-12) — that reasoning is *why* §4's read path withholds affordability tokens; update the sentence
   to say the payload *carries* the figure in storage but the public read path does not expose it,
   citing this spec.
2. `src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java` — add
   `import com.google.gson.*;` (Gson already a project dependency, used identically in
   `HealthSherpaService.java`, §1.7 — no new dependency). Add a private method,
   `buildIchraPayload(...)`, called from `attachRangeSnapshot` and `attachAgeBandSnapshot` (§1.2, lines
   640-655 and 722-737) immediately before each method's own `ProposalIchraSnapshotDAO.save(...)` call
   — populate `snapshot.setPayloadJson(buildIchraPayload(...))` there. The method serializes §3's shape:
   `provenance` always; `ageBands` when age/lives/premium inputs are present (mirrors the existing
   `age1..age6` loop already in `attachAgeBandSnapshot`, §1.2 — `attachRangeSnapshot` has no per-age
   loop today and would need one added, or `ageBands` stays `null` for `RANGE`-mode proposals — **this
   run does not decide which; name it as the build run's own call, guided by whether `RANGE` mode is
   expected to populate age-banded data at all**, which nothing read this run answers); `affordability`
   only when `onexLcspPremium`/income/applicable-percentage inputs are all present (a new
   `AffordabilityCalculator.flipContribution` call site — none exists in `ProposalBuilder.java` today);
   `planLandscape` only when T166's plan-fetch call (not yet built) supplies plans.
3. `src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java` — add
   `import com.google.gson.*;` if not already present (confirmed absent, §1 imports list). Add a new
   private method, `putIchraPayloadTokens(EntityManager em, Map<String, String> tokens, Proposal
   proposal)`, following `putIchraMarketTokens`'s exact shape (§1.3: one `try/catch`, degrade to `""`
   inside the catch, never propagate). Reads `ProposalIchraSnapshotDAO.findByProposalId`, deserializes
   `getPayloadJson()` if non-null, builds the three tokens from §4 (Read). Call it from the same place
   `putIchraMarketTokens` is called today.

**Verification steps Kevin walks** (this run proves none of these — see §7):
1. Build a proposal through the illustration hand-off (RANGE or AGE_BAND mode) with a complete set of
   inputs, on a PSP-admin session with real production-sourced rate cache data (or the T150 demo
   override). Confirm `payload_json` is non-null and contains valid JSON matching §3's shape, by direct
   query against the local dev database (same `mysql.exe`/`MYSQL_PWD` procedure S19-C used).
2. Confirm a proposal built with an incomplete affordability input (e.g. no income entered) still
   writes `ageBands`/`planLandscape` if those inputs were present, and `affordability: null` — not a
   missing row.
3. Confirm the public `/proposal/*` page renders `ICHRA_AGE_BAND_TABLE`/`ICHRA_PLAN_LANDSCAPE_TABLE`/
   `ICHRA_PAYLOAD_AS_OF` correctly when a `CUSTOM` page's HTML contains those tokens, and confirm **no**
   affordability figure appears anywhere on that public page regardless of what the underlying row's
   `payload_json` contains.
4. Confirm a proposal with no snapshot row at all (no ICHRA hand-off) renders those same three tokens
   as empty strings, not literal `{{...}}` text and not a 500.

**BLOCKED — named, not papered over:**
1. **The `FlaggedEnhancementResolver` predicate** (§1.4). This spec gives the resolver something to
   read (`payload_json`, once §5's build ships) but not a Rule-4-compliant way to decide *which*
   `system_managed` enhancement's section a given payload's content satisfies. T164/T166's problem to
   solve, not this run's.
2. **Public-page exposure of the `affordability` block.** §4 (Read) deliberately defines no token for
   it. What would settle this: an explicit compliance decision — recommend it be recorded as a new
   `LA-NN` entry (next available number, per `legal_assumptions.md`'s own numbering — **do not assume
   it is LA-18**; that file's own index notes LA-14/15/16 were never even added to its summary table,
   so re-read the file at decision time) — stating whether `onExchangeLcspPremium`/
   `subsidyPreservingCeiling` may ever reach an unauthenticated `/proposal/*` page, and if so, under
   what framing constraint (LA-15's threshold-vs-ceiling wording discipline would bind directly).

---

## 6. Decisions made (this run)

1. **Storage: Option A**, new `payload_json` `MEDIUMTEXT` column on `proposal_ichra_snapshot`. Closes
   half of T165 (the "which text field, on which entity" half of session 18's open question).
2. **`MEDIUMTEXT`, sized against the 2,000-plan cap, not the 65-plan reference county.** Closes the
   sizing half of the same open question.
3. **Payload sub-blocks are independently nullable; the row itself stays all-or-nothing.** A decision
   this spec makes that the prompt did not dictate — reasoned from session 18 decision 5's toggle
   independence. Reversal cost: low — a future build could tighten this to fully-atomic without a
   migration (it's a serialization-logic change, not a schema one), but *loosening* an atomic write
   after employers have already seen partial proposals would be the harder direction.
4. **No public token for the `affordability` block.** The single highest-stakes call this run makes,
   and the reason §0's "expensive to get wrong" framing is taken literally here. Reversal cost:
   low in one direction (adding a token later, once a compliance decision permits it, is additive) and
   **not low in the other** — if a future build had instead wired the token now and it shipped, LA-15's
   own recorded reversal-cost language applies verbatim: *"a ceiling once presented to an employer as a
   target has been acted on."*
5. **Class javadoc on `ProposalIchraSnapshot` must be corrected as part of this build**, not left to
   drift — flagged explicitly in §5's file-edit list rather than assumed.

## 7. New assumptions, and reversal cost

| Assumption | Reversal cost |
|---|---|
| `RANGE`-mode proposals do not need `ageBands` populated (only `AGE_BAND` mode has the per-age loop `attachAgeBandSnapshot` already runs) | Low if wrong — adding an equivalent per-age loop to `attachRangeSnapshot` is additive, no schema change, no migration |
| Gson (already a dependency, already used in `HealthSherpaService`) is an acceptable serializer for this payload too, rather than introducing Jackson or hand-rolled JSON | Low — nothing else in this spec depends on which JSON library is chosen; swapping libraries is a code change confined to two methods |
| `applicablePercentage` is worth carrying for audit even though no read-path token this spec defines displays it | Low — an unused stored field costs nothing; removing it later (if genuinely never useful) is a no-op schema-wise since it lives inside the JSON text, not a column |
| A future O25-permissive decision on carrier names will read `planLandscape.plans[].name`/`.issuerName` from storage rather than requiring a second HealthSherpa call | Low if wrong in the sense that nothing breaks — but if O25 resolves restrictively, these two fields sit in storage unused indefinitely, which is the accepted cost of not re-fetching later |

## 8. Open questions raised, and who settles them

1. **Whether `attachRangeSnapshot` gains an age-band-style loop, or `RANGE`-mode proposals simply never
   populate `ageBands`.** Named in §5 as the build run's own call. Kevin, or whoever builds T165, at
   build time.
2. **The `FlaggedEnhancementResolver` predicate.** T164/T166's question, not this spec's — see §5
   BLOCKED item 1.
3. **Public exposure of the affordability block.** Kevin/counsel, via a new `LA-NN` entry — see §5
   BLOCKED item 2. This is the one this spec treats as load-bearing enough to block a whole token
   category on, not merely note.
4. **Carried forward, unchanged, from session 18 §5 item 2:** whether the HealthSherpa call for page 7
   runs synchronously inside the proposal-create request. T166's question.
5. **Carried forward, unchanged, from session 18 §5 item 5:** whether a post-V078 re-warm has actually
   populated `onexLcspPremium`/`onexBenchmarkSilverPremium` on any real cache row anywhere. This spec's
   `affordability` block is only as good as that column being populated — unverified by this run (no
   database was queried).
6. **O25 (carrier names).** Still not this run's or any single future run's to decide alone — named
   again here only because §3/§4 both touch it directly (storing `name`/`issuerName` while gating their
   display).

## 9. Contradictions found

1. **This prompt's premise about token syntax was wrong.** §2 item 3 asked whether tokens are
   `${...}`-shaped or matched from a fixed list. Neither is correct: tokens are `{{TOKEN_NAME}}`,
   case-insensitive, and the "list" is simply every `tokens.put(...)` call site scattered through
   `ViewProposal.java` — there is no central registry. Corrected in §1.3.
2. **Session 18's decision 3 does not address the audience tension its own execution walks into.**
   `ProposalIchraSnapshot`'s javadoc and two independent close-out records (§1.1) establish, as a
   *deliberate* design property, that nothing proposal-adjacent carries an affordability figure, because
   `/proposal/*` is public/unauthenticated (LA-12) — a materially different audience than the
   employer/agent-facing guarantee LA-12's own design choice requires. Decision 3 overrides this by
   adding the column, but the decision record only justifies *that* neither entity has the column, not
   *why* it's safe to add one now. This spec does not resolve the tension (not this run's job, per the
   same reasoning O25 gets) — it names it, in §5's BLOCKED list and §6 decision 4, and designs the read
   path (no public token) so the storage decision doesn't silently become a display decision by default.
3. **Minor, not load-bearing:** session 18 cited `replaceTokens` at `ViewProposal.java:876-890`; it is
   actually at `:878-905`. Noted in §1.3, not re-filed as its own backlog row — too small to be worth
   the bookkeeping, but stated rather than silently corrected, per this project's own stated preference
   for grepping load-bearing claims rather than restating them (`verify_before_asserting` working
   preference, this repository's memory).

No other contradictions found. Every other fact this prompt asserted (the two-guard shape, `sourceEnv`
staying honest under the demo override, `PlanSummary`'s field list, `PER_PAGE`/`MAX_PAGES`, the 65-plan
reference county, the resolver's stub behaviour) was verified against the repository and held.

## 10. Recommended next step

Build §5 as written, in order: migration → entity field + javadoc correction → `ProposalBuilder` write
path → `ViewProposal` read path (three tokens only, no affordability token). Leave both BLOCKED items
exactly as named rather than guessing at either — the `FlaggedEnhancementResolver` predicate blocks
T164/T166 specifically, not this build; the affordability-token question should go to Kevin as a
direct question (*"does an `LA-NN` entry need to be written before this figure can ever appear on a
public proposal page?"*) before anyone reaches for the token map to add one, because by the time a
token exists and ships, LA-15's own reversal-cost language stops being hypothetical.

## 11. SQL close-out audit

- **Every SQL statement this run produced:** the DDL text in §2, plus the illustrative
  `V090__proposal_ichra_payload.sql` header/footer shown there. **Recommended, in a spec, not written
  to a versioned migration file** — no `.sql` file was created by this run, confirmed by this run's own
  scope fence (§6 of the prompt: "You may create exactly one file"). The version number `V090` is a
  placeholder for the build run to confirm, not a claim this run reserves it.
- **Current highest migration version, read directly:** **V089**
  (`docs/migrations/V089__enhancement_system_managed.sql`, confirmed via `ls docs/migrations/` this
  run — unchanged from S19-C).
- **No `.sql` file was created.** Confirmed by this run's own file list: the only file written is this
  spec.
- **Orphaned `.sql` files:** none observed; none created.
- **Schema described but not scripted:** the entirety of §2's DDL and §3's JSON shape — by design, per
  this run's own scope fence. This is the intended output of a Phase A run, not an oversight.

## 12. Compliance statement

**Preflight output, verbatim:**
```
$ git rev-parse --abbrev-ref HEAD
refactor/modernize-architecture

$ git log -1 --oneline
9c49b5a docs: S19-C -- V089 applied to local dev, tracker updated

$ git status --short
(no output — clean)

$ git pull --ff-only
Already up to date.
```

**Code-verified vs. runtime-verified.** This run executed no code and touched no database — everything
above is code-verified at best (read directly from the repository, with file paths and line numbers
cited throughout §1) or reasoned from those reads (§2-§4's design decisions). Nothing in this spec has
been runtime-verified: no migration has run, no payload has been written, no token has rendered. §5's
"Verification steps Kevin walks" section exists specifically because this run cannot perform any of
them itself.

**Scope fence compliance:** no `.java`, `.jsp`, or `.sql` file was edited or created. No file under
`docs/migrations/` was touched. `migration_tracker.md`, `project_backlog.md`, `ichra_strategy.md`,
`swbd_ichra_build_plan.md`, and `legal_assumptions.md` were read (for §1.1/§1.5/§9's LA-15/LA-12
citations) but not edited. No T-number was assigned — items needing one are named in §5/§8 as needing
filing, not filed. No `LA-NN` number was assigned — §5/§9 describe what the entry would need to say and
recommend it be written, without claiming a number.
