# S20-A — Phase A: the four ICHRA proposal sections

Date: 2026-08-06. Branch: `refactor/modernize-architecture`. Preflight HEAD: `3cb9497`.

This is a build spec, not a report. §8 is what a later build run types for item 1, with no further design
decisions. Everything above §8 is the reasoning that produced it. §9 is the close-out.

---

## 1. Read first, decide second — what the repository actually says

Every path and line number below was read this run. Where this prompt's premise did not hold, it is
marked ⚠️ and the finding is used in place of the premise.

### 1.1 `docs/analysis/S19D_ichra_payload_spec.md` — the committed schema

Read in full (599 lines). Confirmed present and current, including S19-F's per-band `affordability`
correction and its dated correction note (lines 314-329). Schema version 1, four top-level blocks:
`schemaVersion`, `provenance`, `affordability`, `ageBands`, `planLandscape`. The read contract that
matters to this run is §3's first field note (lines 263-270): **a reader must never fail on an
unrecognized `schemaVersion`; it reads only the fields its own code knows about, and treats a field
absent because the payload predates it exactly as it treats any other absent sub-block.** That contract
is what makes §4's version bump free.

### 1.2 `buildIchraPayload` as it now stands, post-S19-O

`src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:959-996`.

- `ICHRA_PAYLOAD_GSON = new GsonBuilder().serializeNulls().create()` at `:58` — S19-O's fix, so a null
  sub-block serializes as `"ageBands": null` rather than vanishing. The javadoc at `:46-49` records why:
  a plain `new Gson()` omitted nulls and defeated the stable-four-key intent.
- Always writes `schemaVersion: 1` (`:963`) and a fully-populated `provenance` (`:965-972`).
- `ageBands` from the `bands` list, or `JsonNull` (`:974-986`).
- `affordability` from `buildAffordabilityBlock` (`:1023-1070`), or `JsonNull`.
- `planLandscape` unconditionally `JsonNull` (`:993`) — T166 unbuilt.
- Two call sites, both immediately before `ProposalIchraSnapshotDAO.save`: `attachRangeSnapshot`
  (`:819-820`, passes `null` for bands) and `attachAgeBandSnapshot` (`:933-934`, passes the band list).
- The read path already exists: `ViewProposal.putIchraPayloadTokens` (`:875-954`), three tokens, one
  `try/catch`, degrades to `""`, `name`/`issuerName` withheld pending O25. It keys on `payload.has(...)`
  per block and **never reads `schemaVersion` at all** — so a version bump changes nothing there.

### 1.3 `FlaggedEnhancementResolver` — the stub, and what it needs

`src/main/java/net/superiorstate/ams/data/resolver/FlaggedEnhancementResolver.java:43-65`. Unchanged
from what S19-D read. `null` enhancement → `true` (`:45-47`); `!isSystemManaged()` → `true` (`:48-50`),
before touching `em` or `proposal` — the bit-identity guarantee; flagged → `return false` (`:56`);
any exception → `false` (`:57-64`). Class javadoc `:31-35` states the Rule-4 constraint in the file
itself: *no enhancement ID literal may ever appear in this class.*

To interrogate the payload it needs three things, and only the first exists today:

1. `em` and `proposal` in the signature — **present, unused**.
2. A way to identify *which* of the four sections a given flagged enhancement is — **does not exist.**
   `Enhancement` (`model/sales/offering/Enhancement.java:11-47`) carries `id`, `description`,
   `shortText` (`varchar(20)`), `sortOrder`, `suppressed`, `systemManaged`, `psp`, `serviceItem`,
   `losList`, `applicationSectionList`. Nothing is a stable machine-readable classifier. `shortText` is
   PSP-editable display text and must not be repurposed as a key. §6 adds a column.
3. A selection signal in the payload — **does not exist.** §4 adds it.

### 1.4 ⚠️ The load-bearing finding: the resolver is not reached for most configurations

`ViewProposal.java:284-312` is the `SCOPED` filter. Two facts change the shape of this spec:

**(a) The LOS branch short-circuits past the resolver.** `:292-299` checks `section.getLosList()`
against `proposalLosIds` and sets `matches = true` with `break` — **without calling
`FlaggedEnhancementResolver` at all.** Only the enhancement branch (`:300-307`) consults it, at `:302`:

```java
if (proposalEnhIds.contains(enh.getId()) && FlaggedEnhancementResolver.isSectionEnabled(em, proposal, enh)) {
```

⚠️ **Consequence, and it contradicts a reading of §1 of this prompt.** "Each choice is a separate
special enhancement, so each can be scoped to its own LOS page" is right about the enhancement and
wrong about the mechanism if taken to mean the section row carries an LOS association. **A section
that is both LOS-scoped and enhancement-scoped renders unconditionally** — the LOS branch matches
first and the gate never runs. The four sections must be configured `scope = 'SCOPED'` with an
**enhancement association only and an empty `proposalsectionlos`**. The LOS relationship is carried
indirectly, through `enhancement_los`, which is where it belongs. This is a hard configuration rule,
recorded in §8.6 and again in the verification walk.

**(b) `proposalEnhIds` is derived from priced lines, not from selection.** `:277-282` builds it from
`ProposalPriceLine.getModule().getEnhancement()`. Those lines come from
`SalesDAO.getPricingWithAdjustments` → `getPricing` (`data/dao/SalesDAO.java:321-353`), whose second
query is:

```java
"SELECT rt FROM RateTable rt WHERE rt.rate.id = :rateId AND rt.module.enhancement.id IN " +
        "(SELECT e.id FROM Enhancement e JOIN e.losList el WHERE el.id IN :losIds) ..."
```

So an enhancement enters `proposalEnhIds` **iff** (i) `enhancement_los` links it to a proposal LOS,
(ii) a `ServiceModule` exists with `module.enhancement` = it, **and** (iii) the proposal's chosen `Rate`
carries a `RateTable` row for that module. There is no per-proposal enhancement *selection* anywhere —
membership is implied by LOS + Rate.

⚠️ **This means the payload alone is not sufficient.** A `system_managed` enhancement is by definition
not a priced service — `serviceManager25.jsp:951` says so in the admin UI's own words: *"Marks this
enhancement as driving system-generated proposal content... Has no effect on the application."*
Requiring it to carry a `RateTable` line to become visible is backwards, and the only way to satisfy
that requirement today is to create a price line for it — putting a fabricated (probably $0) row on a
customer-facing pricing page. **Rejected.** §5 specifies a narrow membership widening instead.

⚠️ **This contradicts T165's own promise**, recorded in `project_backlog.md:225`: *"the resolver's
signature already accepts `em` and `proposal`... specifically so this build never requires a second
edit to `ViewProposal.java`."* That promise holds for the *predicate* and does not hold for
*membership*. One further line in `ViewProposal` is required. Named, not papered over.

### 1.5 The two ICHRA tables — where §1's inputs live, and where they do not

`proposal_ichra_intake` (V087 + V088; entity `model/sales/agency/ProposalIchraIntake.java:30-72`):
`intake_id`, `proposal_id` (UNIQUE), `zip`, `county_fips`, `county_name`, `state`, `headcount`,
`monthly_contribution_per_employee` (V088, **nullable**), `plan_year`, `collected_at`, `created_by`.

`proposal_ichra_snapshot` (V079 + V090; entity `ProposalIchraSnapshot.java`): `snapshot_id`,
`proposal_id` (UNIQUE), `mode`, `county_fips`, `state`, `county_name`, `plan_year`, `contribution`,
`headcount`, `group_monthly_low`, `group_monthly_high`, `group_net_total`, `employer_outlay`,
`source_env`, `rates_fetched_at`, `snapshot_at`, `created_by`, `payload_json` (V090, MEDIUMTEXT).

Mapping §1's inputs onto storage:

| §1 input | Has a home today? |
|---|---|
| ZIP | ✅ `intake.zip` |
| County (FIPS / name / state) | ✅ `intake.county_fips` / `county_name` / `state` |
| Headcount | ✅ `intake.headcount` (V087; S19-O falls back to the band-count sum) |
| Age bands (age + lives) | ✅ `proposal_ichra_snapshot_band` when a contribution exists; ✅ always in `payload_json.ageBands` |
| Employer contribution | ✅ `intake.monthly_contribution_per_employee` (V088) and `snapshot.contribution` |
| **#3 current total monthly premium** | ❌ **no home anywhere** — confirmed, as this prompt expected |
| **#3 current employer share** | ❌ **no home anywhere** — confirmed |
| #3 planned ICHRA/QSEHRA contribution | ✅ same field as the employer contribution above; §4 treats them as one input, not two |
| #4 affordability basis / annual income | ⚠️ request parameters only (`ProposalBuilder:1027`, `:1039`) — frozen into `payload_json.affordability.incomeBasis`, never persisted as columns |
| **The four selections themselves** | ❌ **no home anywhere** |

### 1.6 `ProposalIchraSnapshotBand` — the `NOT NULL` block, confirmed

`ProposalIchraSnapshotBand.java:33-37`:

```java
@Column(name = "net_per_employee", nullable = false)
private BigDecimal netPerEmployee;

@Column(name = "band_net", nullable = false)
private BigDecimal bandNet;
```

`docs/migrations/V079__proposal_ichra_snapshot.sql:72-73` matches: `net_per_employee DECIMAL(8,2) NOT
NULL`, `band_net DECIMAL(10,2) NOT NULL`. Both are net-of-contribution by definition
(`ProposalBuilder:897-898`).

The live consequence is already visible in the code. `attachAgeBandSnapshot:936-942` writes:

```java
ProposalIchraSnapshotDAO.save(em, snapshot, contribution != null ? bands : null);
```

— when no contribution was entered, **the band rows are silently not persisted at all.** The payload's
`ageBands` still carries age/lives/premium (`:929-934`), so nothing renders wrong today, but the
structured table has a hole in it exactly for the case §1 says must be supported: #1 and #3 are
available without a contribution. **In scope, fixed in §6.**

### 1.7 `proposalsectionlos` and the section extension point

`docs/migrations/V037__proposal_section_scoping.sql` shipped `proposal_section.scope VARCHAR(10) NOT
NULL DEFAULT 'ALL'` plus two join tables, `proposalsectionlos` and `proposalsectionenhancement`. Entity
`model/sales/offering/ProposalSection.java:37-50` maps both as `@ManyToMany`. A section carries
`psp`, optional `agency`, `sectionType` (TITLE/PRICING/FEATURES/CLOSING/CUSTOM), `htmlContent`,
`scope`, `sortOrder`, `active`.

Each of the four sections is therefore **an existing `CUSTOM` `ProposalSection` row**, `scope='SCOPED'`,
one `proposalsectionenhancement` row pointing at its enhancement, **zero `proposalsectionlos` rows**
(§1.4a), with `{{TOKEN}}` merge tokens in `htmlContent`. No new surface, no new table, no new render
path. `ViewProposal.replaceTokens` (`:878-905` per S19-D §1.3) substitutes; T133's residual-token
stripping guarantees an unmapped `{{...}}` never reaches the page.

### 1.8 The three "special" enhancements — concept only, no rows

Searched `docs/` and the codebase. The three appear as a **narrative decision only**:
`docs/session_closeout_2026-08-05_session18.md:63` (decision 5) and `project_backlog.md:225` (T165) —
group-vs-ICHRA comparison, contribution scenarios, affordability. **No rows exist**, and nothing seeds
any: `V089__enhancement_system_managed.sql` says in its own header *"Seeds nothing. No INSERT INTO
constant."* No `DemoDataSeeder` or `DatabaseInitializer` path creates one.

§1 of this prompt makes it **four** by promoting market illustration data from an implicit prerequisite
to its own selection. That is the right call and this spec adopts it — §1.4b shows that the three-way
model has no way to render market data alone, because every gate is per-enhancement and a prerequisite
with no enhancement of its own has no gate to pass.

Mapping: #1 → `ICHRA_MARKET`, #2 → `ICHRA_CONTRIBUTION`, #3 → `ICHRA_COMPARISON`,
#4 → `ICHRA_AFFORDABILITY`. These four string literals are the payload keys **and** the
`enhancement.system_section_key` values, byte-for-byte, deliberately (§5).

### 1.9 ⚠️ T163 is marked Planned and has already shipped

`project_backlog.md:223` says *"No admin UI exists to set `enhancement.system_managed` — settable only
by raw SQL... 📋 Planned."* It shipped in **S19-A, commit `3baf60f`** (*"feat: S19-A — system_managed
checkbox on enhancement editor"*): `ServiceManagerAction.java:207`
(`enh.setSystemManaged("on".equals(request.getParameter("systemManaged")))`, in the `editEnhancement`
case) and `serviceManager25.jsp:944-951` (the checkbox plus its help text). The backlog row was never
flipped. **This run may not edit `project_backlog.md`** — recorded in §9.5 as a stale-doc defect needing
correction. It matters to this spec because §6 extends that exact admin surface.

Note for the build run: only `editEnhancement` writes the flag. A newly *created* enhancement comes out
unflagged and must be edited once. That is fine and unchanged; §6 adds the section key to the same
`editEnhancement` handler for the same reason.

---

## 2. The model, as specified (not redesigned)

Four independently selectable sections. #1 is the base layer, not a peer: #2, #3 and #4 all need market
premiums, so all need #1's inputs.

| # | Key | Selection | Required inputs |
|---|---|---|---|
| 1 | `ICHRA_MARKET` | Market illustration data | ZIP, county, **and** either headcount ≥ 1 or ≥ 1 age band |
| 2 | `ICHRA_CONTRIBUTION` | Contribution scenarios | #1's inputs **+** employer contribution |
| 3 | `ICHRA_COMPARISON` | Comparison against their group plan | #1's inputs **+** current total monthly premium, current employer share, planned ICHRA/QSEHRA contribution |
| 4 | `ICHRA_AFFORDABILITY` | Affordability comparison | #1's inputs **+** employer contribution, affordability basis, **and ≥ 1 age band — mandatory** |

**Age bands are a fidelity upgrade for #2 and #3, never a gate.** Not mandated for either. The existing
code already models this correctly and must not be tightened: `deriveIntakeMode`
(`ProposalBuilder:653-665`) routes a band-less intake to `MODE_RANGE`, and `attachRangeSnapshot`
computes a group range from headcount × the age-21/age-64 floors (`:799-800`).

**Age bands ARE mandatory for #4.** Already true in code and requiring no change:
`buildAffordabilityBlock:1025` returns `null` when `bands` is null or empty, which is exactly `RANGE`
mode. §4's `complete` computation restates it rather than re-deriving it.

**#1's fields are required whenever any selection is made**, and #1 is additionally renderable alone.
Server-side this is one line (§8.4): `marketSelected = any of the four checked`.

---

## 3. Where the selections live

Two places, for two different reasons. Both, not either.

**`proposal_ichra_intake` — because a selection is INPUT.** V087's own header states the rule this
follows: *"Intake is INPUT: it must persist for a county with no warmed rates at all, which is the
majority of Texas counties today."* The snapshot is written only when PRODUCTION-sourced rates exist
(`ProposalBuilder:789-790`, `:878-879`), so a selection recorded only in the payload would be **lost
entirely** for the majority county — with no record of what the agent asked for and no way to diagnose
why nothing rendered. Four boolean columns, §6.

**`payload_json.sections` — because the render decision must be frozen with the document.** The
point-in-time rule (session 18 decision, closing T162) says the proposal is a snapshot with disclosed,
never recomputed, staleness. The resolver must read what was true at build time, not re-derive it from
intake rows that a later edit could change. §4.

They are written in the same request from the same values, so they cannot disagree at write time. If
they ever disagree later, **the payload wins** — it is the frozen document; the intake row is the
working input.

**Consequence, stated plainly:** on a county with no warmed production rates, the intake row records
the selections and no snapshot, therefore no payload, is written — so all four sections are withheld.
That is correct and honest: no market data means nothing to show. It is not a bug to be worked around.

---

## 4. The `sections` block — payload schema version 2

### 4.1 Shape

```json
{
  "schemaVersion": 2,
  "provenance": { "...": "unchanged from v1" },
  "sections": {
    "ICHRA_MARKET":        { "selected": true,  "complete": true,  "missing": [] },
    "ICHRA_CONTRIBUTION":  { "selected": true,  "complete": false, "missing": ["contribution"] },
    "ICHRA_COMPARISON":    { "selected": false, "complete": false, "missing": ["currentTotalMonthlyPremium", "currentEmployerMonthlyShare"] },
    "ICHRA_AFFORDABILITY": { "selected": false, "complete": false, "missing": ["ageBands", "affordabilityBasis"] }
  },
  "affordability": { "...": "unchanged from v1" },
  "ageBands":      [ "...unchanged from v1..." ],
  "planLandscape": null
}
```

`sections` is **always present and always carries all four keys** when a payload is written at all —
same floor `provenance` has. Never null, never partial. A section the agent did not select is
`{"selected": false, ...}`, not an absent key. This is what makes "not selected" and "written before
selections existed" distinguishable.

### 4.2 `selected` and `complete` are different facts, and both are needed

`selected` — the agent ticked it in the builder. `complete` — every input §2 requires for that section
was actually present at build time. An agent can select #3 and the build can still lack a figure; the
resolver must have both, and must render only on `selected && complete`. Never invent a value to
satisfy a shape: an absent figure produces `complete: false`, never a zero.

`missing` — an array of stable input keys, empty iff `complete`. It exists for the authenticated
`ProposalDetail` preview (T164) to explain *why* a section is withheld, and for post-hoc diagnosis.
**Never rendered on `/proposal/*`.** Stable key vocabulary, closed set: `zip`, `county`, `headcount`,
`ageBands`, `contribution`, `currentTotalMonthlyPremium`, `currentEmployerMonthlyShare`,
`affordabilityBasis`, `annualIncome`.

### 4.3 Completeness rules — exactly, per section

Evaluated at payload-build time in `ProposalBuilder`, from the same request values the snapshot writers
already read. No second source, no re-query.

- **`ICHRA_MARKET`.** `complete` iff a payload is being written at all. Not vacuous — it is the
  definition of the base layer: `attachIchraSnapshotIfPresent` (`:707-738`) already refuses to write
  without a resolvable county and plan year, `attachRangeSnapshot:770` refuses without `headcount >= 1`,
  and `attachAgeBandSnapshot:852` refuses without ≥ 1 valid band. Reaching `buildIchraPayload` **is**
  the proof. `missing` is `[]`.
- **`ICHRA_CONTRIBUTION`.** `complete` iff market complete **and** the resolved contribution is non-null.
  The contribution is already resolved once at `:837-840` (`ichraParam("contribution",
  "intakeContribution")`, a typed negative collapsing to null); pass that same value down rather than
  re-reading the request.
- **`ICHRA_COMPARISON`.** `complete` iff market complete **and** contribution non-null **and**
  `currentTotalMonthlyPremium` non-null and ≥ 0 **and** `currentEmployerMonthlyShare` non-null and ≥ 0.
  ⚠️ Do **not** additionally validate that the share ≤ the total. An employer who mistypes gets a
  comparison that reads oddly; an employer whose figures are legitimately unusual gets silently
  withheld content with no explanation. Record what was collected; do not adjudicate it.
- **`ICHRA_AFFORDABILITY`.** `complete` iff market complete **and** contribution non-null **and**
  `bands` non-null and non-empty **and** `affordabilityBasis` is `"FPL"` or `"INCOME"` **and** the
  resolved `annualIncome` is non-null. The last three restate `buildAffordabilityBlock:1025-1042`
  exactly — reuse that method's own outcome (`affordability != null`) rather than duplicating its
  conditions, so the two can never drift.

### 4.4 Why version 2, and what a v1 payload does

**Bump to 2.** A v1 payload's silence about selections is permanent — the frozen point-in-time rule
means it is never rewritten — and is genuinely ambiguous: it could mean "written before selections
existed" or "the agent selected nothing." The version number is the only thing that disambiguates, and
that distinction is an audit fact about a client-facing document, not merely a runtime one. S19-D §3's
read contract (§1.1) makes the bump free: no reader may fail on a version it does not recognize.

**Read behaviour for a v1 payload encountering the new reader:**

- `putIchraPayloadTokens` (`ViewProposal:875-954`) — **byte-identical.** It never reads `schemaVersion`
  and keys on `payload.has(...)` per block. A v1 payload still emits `ICHRA_AGE_BAND_TABLE`,
  `ICHRA_PLAN_LANDSCAPE_TABLE`, `ICHRA_PAYLOAD_AS_OF` exactly as today.
- `FlaggedEnhancementResolver` — `sections` is absent, so **every flagged enhancement's section is
  withheld**, identical to today's stub. Fail-closed, no behaviour change for any proposal already in
  the database.
- No reader branches on the integer `2`. The value is written and carried; the presence of `sections` is
  what the resolver keys on. That is deliberate — a reader that switched on the version number would
  have to be edited for every future bump.

**No migration of existing rows, ever.** Frozen documents stay as written. As of this run
`payload_json` is populated only by builds since V090; whatever v1 rows exist stay v1 and stay withheld.

---

## 5. The resolver predicate

### 5.1 How a flagged enhancement identifies itself — Rule 4 compliant

**A new nullable column, `enhancement.system_section_key VARCHAR(32)`**, holding one of the four
literals from §1.8. Not an ID, not a lookup into a `constant` row, not `shortText`.

This is V089's own reasoning applied a second time, verbatim from
`V089__enhancement_system_managed.sql`: *"Why a column on enhancement rather than a constant row holding
enhancement ids: the constant table has PRIMARY KEY (name) and NO psp_id, so it is global per
installation, whereas enhancement.psp_id is per-PSP. One constant cannot name the correct rows for
every PSP on a multi-PSP installation."* A column on the PSP-scoped row is PSP-scoped by construction.
Each PSP flags and keys its own four enhancements; nothing in code names a row.

**The key string is the payload key, byte-for-byte.** No prefix stripping, no case folding, no mapping
table — a transformation is a place for the two to drift. The resolver does
`payload.getAsJsonObject("sections").get(key)`, comparing trimmed values with `equals`. The admin UI
(§8.5) offers a fixed dropdown of the four, not free text, so an admin cannot mistype one into
permanent silence.

### 5.2 Membership — the second `ViewProposal` edit §1.4b forces

A new second method on the resolver, so all flagged-enhancement logic stays in one class:

```java
public static Set<Long> systemManagedIdsForProposal(EntityManager em, Proposal proposal)
```

JPQL — flat, single level, no nested `JOIN FETCH`:

```sql
SELECT e.id FROM Enhancement e JOIN e.losList el
 WHERE e.systemManaged = true AND e.suppressed = false AND el.id IN :losIds
```

Returns an empty set for a null/empty LOS list, and on any exception (fail closed, never throws).
`suppressed = false` is included deliberately: a suppressed enhancement is one the PSP has retired, and
its section must not render. The existing pricing path does not filter on `suppressed`, but this path is
new and additive, so including it withholds rather than reveals — the safe direction.

One line in `ViewProposal`, immediately after the `proposalEnhIds` loop at `:277-282`:

```java
proposalEnhIds.addAll(FlaggedEnhancementResolver.systemManagedIdsForProposal(em, proposal));
```

**Bit-identical when no enhancement is flagged**, which is every row in existence until a PSP admin
flags one — the same guarantee V089 gave. The set gains nothing, the filter behaves exactly as today.

⚠️ The LOS short-circuit at `:292-299` is **not** changed. The four sections must carry no
`proposalsectionlos` rows (§1.4a, §8.6). Changing the short-circuit would alter the render of every
LOS-scoped section on every line of service; a configuration rule is the cheaper and narrower fix.

### 5.3 The predicate

Replaces `return false;` at `FlaggedEnhancementResolver.java:56`. Everything above it is unchanged.

```java
String key = enhancement.getSystemSectionKey();
if (key == null || key.isBlank()) return false;           // flagged but unclassified

ProposalIchraSnapshot snapshot = ProposalIchraSnapshotDAO.findByProposalId(em, proposal.getId());
if (snapshot == null || snapshot.getPayloadJson() == null) return false;

JsonObject payload = new Gson().fromJson(snapshot.getPayloadJson(), JsonObject.class);
if (payload == null || !payload.has("sections") || !payload.get("sections").isJsonObject()) return false;

JsonObject sections = payload.getAsJsonObject("sections");
String k = key.trim();
if (!sections.has(k) || !sections.get(k).isJsonObject()) return false;

JsonObject sec = sections.getAsJsonObject(k);
return sec.has("selected") && !sec.get("selected").isJsonNull() && sec.get("selected").getAsBoolean()
    && sec.has("complete") && !sec.get("complete").isJsonNull() && sec.get("complete").getAsBoolean();
```

`new Gson()` (read-only; `serializeNulls` is a write-side concern). Every branch stays inside the
existing `try`, so any parse failure lands in the existing `catch` and returns `false`.

### 5.4 Behaviour, exhaustively

| Situation | Result | Why |
|---|---|---|
| Enhancement unflagged (`system_managed = 0`) | **`true`**, before any query | Bit-identity guarantee, `:48-50`, untouched |
| Selected **and** complete | **`true`** — section renders | The only true case |
| Selected but **incomplete** | **`false`** — withheld | A section whose data is missing must never render a hole |
| **Not selected** | **`false`** — withheld | The agent did not ask for it |
| **No payload at all** (no snapshot row, or `payload_json` NULL) | **`false`** — withheld | Identical to today's stub. **Never a 500**: the whole body is inside the existing try/catch, and `ViewProposal` calls this from a plain boolean condition |
| **v1 payload** (no `sections`) | **`false`** — withheld | §4.4; fail-closed, no behaviour change for existing rows |
| Flagged but `system_section_key` NULL | **`false`** — withheld | Half-configured is not configured |
| Unparseable JSON, malformed `sections`, any exception | **`false`** — withheld, `log.warn` | Existing `catch` at `:57-64`, unchanged |

**A missing payload must never 500 a public proposal page** — and does not. The method never throws
(existing catch-all), returns a plain `boolean` consumed by an `&&` at `:302`, and the DAO call is the
same `findByProposalId` `ViewProposal:142` and `:880` already make on the same request.

**Cost.** The resolver is called once per (SCOPED section × its enhancements) pair, so a flagged
enhancement on two sections issues two `findByProposalId` calls and two parses. With at most four
flagged enhancements per PSP and the same proposal id every time, EclipseLink's L2 cache serves the
repeat lookups. Correctness before caching; do not add a per-request memo in build 1.

---

## 6. The migration — DDL as text, no `.sql` file this run

`docs/migrations/` read directly this run. Highest version present: **V090**
(`V090__proposal_ichra_payload.sql`). Next available is therefore **V091** — the build run must
re-read the directory and confirm, per every prior spec's own discipline.

Filename: `docs/migrations/V091__ichra_section_selection.sql`

```sql
-- V091: The four ICHRA proposal sections -- selection, the section discriminator, #3's
-- employer-supplied comparison inputs, and the band NOT NULL correction.
--
-- S20-A. Three unrelated-looking changes in one script because they are one feature: the
-- agent's selection of which ICHRA content a proposal carries. See
-- docs/analysis/S20A_ichra_sections_spec.md.
--
-- ----------------------------------------------------------------------
-- 1. enhancement.system_section_key -- the Rule-4-compliant discriminator
-- ----------------------------------------------------------------------
-- V089 gave FlaggedEnhancementResolver a flag (system_managed) but no way to tell WHICH
-- of the four sections a flagged enhancement is. This column is that answer, and it is a
-- column on enhancement for exactly the reason V089's own header gives for system_managed:
-- the constant table has PRIMARY KEY (name) and no psp_id, so it is global per
-- installation, whereas enhancement.psp_id is per-PSP. One constant cannot name the
-- correct rows for every PSP. A hardcoded enhancement id in Java would not survive a
-- second installation at all.
--
-- NULLABLE, no default, no backfill, no CHECK constraint and no enum: every existing row
-- reads NULL, and the resolver treats a flagged-but-unkeyed enhancement as withheld
-- (half-configured is not configured). The admissible values are the four literals below;
-- they are enforced by the admin UI's fixed dropdown, not by the column, matching this
-- schema's established practice (proposal_section.scope and section_type are both bare
-- VARCHARs with no CHECK).
--
--   ICHRA_MARKET         -- market illustration data (the base layer)
--   ICHRA_CONTRIBUTION   -- contribution scenarios
--   ICHRA_COMPARISON     -- comparison against their current group plan
--   ICHRA_AFFORDABILITY  -- affordability comparison
--
-- Seeds nothing. No INSERT INTO constant. No enhancement row is created or flagged by this
-- script -- a PSP admin flags and keys its own four in the Service Manager.
--
-- No index: mirrors V089's own reasoning -- read per already-loaded entity, never queried on.
-- (systemManagedIdsForProposal filters on system_managed, not on this column.)

ALTER TABLE enhancement
    ADD COLUMN system_section_key VARCHAR(32) NULL AFTER system_managed;

-- ----------------------------------------------------------------------
-- 2. proposal_ichra_intake -- the four selections
-- ----------------------------------------------------------------------
-- INPUT, so it belongs here rather than only in payload_json, for the reason V087's own
-- header states: intake must persist for a county with no warmed rates at all, which is
-- the majority of Texas counties today. proposal_ichra_snapshot is written only when
-- PRODUCTION-sourced rates exist, so a selection recorded only in the payload is lost
-- entirely for that county -- with no record of what the agent asked for.
--
-- Four named booleans rather than one CSV column: matches los.is_plus_tier (V086),
-- agency.markup_enabled (V067) and enhancement.system_managed (V089), and avoids the
-- substring-matching trap V045's comma-separated selected_enhancement_ids created on
-- application (where "5" matches inside "15" without comma-padding).
--
-- TINYINT(1) NOT NULL DEFAULT 0, no backfill: every existing intake row comes out with all
-- four unselected, which is the truth -- those proposals were built before any of this
-- existed and carry no ICHRA section.
--
-- section_market is stored as submitted AND as derived: the servlet sets it true whenever
-- any of the other three is true (spec sec 2 -- #1 is the base layer, not a peer), so this
-- column always reflects what was actually required, never only what was ticked.

ALTER TABLE proposal_ichra_intake
    ADD COLUMN section_market        TINYINT(1) NOT NULL DEFAULT 0 AFTER monthly_contribution_per_employee,
    ADD COLUMN section_contribution  TINYINT(1) NOT NULL DEFAULT 0 AFTER section_market,
    ADD COLUMN section_comparison    TINYINT(1) NOT NULL DEFAULT 0 AFTER section_contribution,
    ADD COLUMN section_affordability TINYINT(1) NOT NULL DEFAULT 0 AFTER section_comparison;

-- ----------------------------------------------------------------------
-- 3. proposal_ichra_intake -- #3's employer-supplied comparison inputs
-- ----------------------------------------------------------------------
-- The employer's CURRENT group plan cost, as the agent reports it. Neither figure has a
-- home anywhere in the schema today (verified against every column of both ICHRA tables,
-- spec sec 1.5). Both are employer-reported inputs, exactly like
-- monthly_contribution_per_employee (V088) -- not computed, not market-derived, and
-- carrying no relationship to rating_area_rate_cache or proposal_ichra_snapshot.
--
-- The third input section 3 needs -- the planned ICHRA/QSEHRA contribution -- is
-- monthly_contribution_per_employee (V088), already here. One field, not two: the figure
-- the employer plans to contribute is the same figure section 2's scenarios use, and
-- storing it twice is how two copies disagree.
--
-- Nullable, no backfill: both are unanswered until the agent selects section 3, and an
-- unanswered value produces sections.ICHRA_COMPARISON.complete = false -- an omitted
-- section, never an error and never a zero standing in for an unknown.
--
-- DECIMAL(10,2) matches monthly_contribution_per_employee (V088). These are whole-group
-- monthly figures rather than per-employee ones, so the wider scale matters: 99,999,999.99
-- covers any group AMS will quote.
--
-- No CHECK that the employer share <= the total. An employer whose figures are unusual
-- would get content silently withheld with no explanation; record what was collected, do
-- not adjudicate it.

ALTER TABLE proposal_ichra_intake
    ADD COLUMN current_total_monthly_premium  DECIMAL(10,2) NULL AFTER section_affordability,
    ADD COLUMN current_employer_monthly_share DECIMAL(10,2) NULL AFTER current_total_monthly_premium;

-- ----------------------------------------------------------------------
-- 4. proposal_ichra_snapshot_band -- the NOT NULL correction
-- ----------------------------------------------------------------------
-- V079 made net_per_employee and band_net NOT NULL. Both are net-of-contribution figures
-- by definition (ProposalBuilder:897-898), so without an employer contribution there is
-- nothing honest to put in either -- and section 1 and section 3 are both available with no
-- contribution at all.
--
-- The live consequence today: attachAgeBandSnapshot:942 writes
--   ProposalIchraSnapshotDAO.save(em, snapshot, contribution != null ? bands : null)
-- so when no contribution was entered the band rows are SILENTLY NOT PERSISTED. Nothing
-- renders wrong (payload_json.ageBands still carries age/lives/premium), but the
-- structured table has a hole in it for exactly the case this feature must support.
--
-- Widening to NULL is strictly permissive: every existing row has a value and keeps it, no
-- backfill is possible or needed, and no reader anywhere assumes non-null (grep: the only
-- consumers are the writer above and ProposalIchraSnapshotDAO's own flat find).
--
-- Reversal: re-tightening to NOT NULL after contribution-less band rows exist would require
-- deleting them or inventing values, so this is one-directional in practice. Accepted --
-- the alternative is a permanent hole in the structured data.

ALTER TABLE proposal_ichra_snapshot_band
    MODIFY COLUMN net_per_employee DECIMAL(8,2)  NULL,
    MODIFY COLUMN band_net         DECIMAL(10,2) NULL;

-- ----------------------------------------------------------------------
-- 5. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V091' AS version, '<build date>' AS updated;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V091', 'ICHRA section selection: enhancement.system_section_key, four selection flags and section 3 comparison inputs on proposal_ichra_intake, band net NOT NULL widened', 'V091__ichra_section_selection.sql', NOW());
```

**Deliberately NOT in this migration:**

- **No column for `affordabilityBasis` / `annualIncome`.** Section #4 is blocked on an unwritten `LA-NN`
  entry (§7) whose outcome could change what may be stored at all. Adding a column now for a section
  that cannot ship is speculative. Build 4 gets its own migration.
- **No column for the payload's `sections` block.** It rides `payload_json` (V090, MEDIUMTEXT) — the
  whole point of that column being JSON is that the payload evolves without a migration.
- **No `INSERT INTO constant`, no seed rows of any kind.**

**One migration covers all three builds.** Builds 2 and 3 (§7) need no further schema change; #3's
columns land here even though build 3 is the one that writes them, because splitting a two-column
`ALTER` across two versions buys nothing and costs a second production apply.

---

## 7. Build sequence

Four builds. Each needs its own T-number; **this run assigns none** (§9.5 lists what needs filing).

### Build 1 — `ICHRA_MARKET`, the base layer ⭐ first, and the only one renderable alone

Everything structural is here: the migration, the discriminator, the `sections` block, the resolver
predicate, the membership widening, the form. Builds 2 and 3 are content and inputs on top of it.

**Create:** `docs/migrations/V091__ichra_section_selection.sql` (§6, verbatim except version/date).

**Edit:**

1. `src/main/java/net/superiorstate/ams/model/sales/offering/Enhancement.java` — after `systemManaged`
   (`:29-30`), matching that field's style exactly:
   ```java
   @Column(name = "system_section_key", columnDefinition = "varchar(32)")
   private String systemSectionKey;
   ```
   plus getter/setter alongside `isSystemManaged`/`setSystemManaged` (`:66-67`).
2. `src/main/java/net/superiorstate/ams/model/sales/agency/ProposalIchraIntake.java` — six fields
   (four `Boolean`/`boolean` selections, two `BigDecimal`), following
   `monthlyContributionPerEmployee`'s style (`:61-62`), plus getters/setters. Update the class javadoc
   (`:9-27`) — it says *"Nothing reads this table yet — T126"*, which is already false
   (`ViewProposal:463`, `:708`) and becomes more false here.
3. `src/main/java/net/superiorstate/ams/model/sales/agency/ProposalIchraSnapshotBand.java` — drop
   `nullable = false` from `netPerEmployee` and `bandNet` (`:33-37`). Add a javadoc note on both saying
   why: net-of-contribution by definition, absent when no contribution was entered.
4. `src/main/java/net/superiorstate/ams/data/resolver/FlaggedEnhancementResolver.java` — replace the
   stub `return false;` (`:56`) with §5.3's predicate; add `systemManagedIdsForProposal` (§5.2).
   Rewrite the *"The flagged path is a stub"* javadoc paragraph (`:22-25`) — it will be false. Keep the
   *"No enhancement ID literal"* paragraph (`:31-35`) verbatim and add that `system_section_key` is the
   discriminator, for the same reason.
5. `src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java` — **one line**
   after the `proposalEnhIds` loop (`:277-282`), per §5.2. Nothing else in this file changes; in
   particular `putIchraPayloadTokens` (`:875-954`) is untouched and stays version-agnostic.
6. `src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java`:
   - `attachIchraIntakeIfPresent` (`:511-581`) — read the four selection checkboxes, derive
     `sectionMarket = market || contribution || comparison || affordability` (§8.4), read #3's two
     optional decimals with the same "negative collapses to unanswered" rule V088's field already uses
     (`:558-561`), set all six on the intake. **No new fail-closed condition** — none of these may
     block an intake write.
   - `buildIchraPayload` (`:959-996`) — `schemaVersion` 2; add the `sections` block per §4; add a
     private `buildSectionsBlock(...)` computing §4.3's rules. Signature grows the values it needs
     (contribution, the two comparison figures, and `buildAffordabilityBlock`'s own result so §4.3's
     last rule reuses it rather than restating it).
   - Both call sites (`:819-820`, `:933-934`) pass the new values.
   - `attachAgeBandSnapshot:942` — change to `ProposalIchraSnapshotDAO.save(em, snapshot, bands)`
     unconditionally, now that the columns permit it. Update the `:936-941` comment, which documents
     the old constraint.
7. `src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp` — §8.6's checkbox block and §8.7's
   show/hide and mandatory rules.
8. `src/main/java/net/superiorstate/ams/controller/activity/setup/ServiceManagerAction.java` — in the
   `editEnhancement` case (`:201-210`), next to `setSystemManaged` (`:207`):
   `enh.setSystemSectionKey(...)` from a request parameter, trimmed, blank → null.
9. `src/main/webapp/WEB-INF/view/sales/serviceManager25.jsp` — a `<select name="systemSectionKey">`
   under the System Managed checkbox (`:944-951`), five options: a blank *"— none —"* plus the four
   literals from §1.8. **A fixed dropdown, never a text input** — a mistyped key is permanent silence.

**Verification walk (Kevin, against a PSP-admin session):**

1. Apply V091 to local dev. Confirm `schema_version` carries `V091` and that
   `SHOW COLUMNS FROM proposal_ichra_snapshot_band` reports `net_per_employee` and `band_net` as `YES`
   under `Null`.
2. Service Manager → edit an enhancement → tick **System Managed**, choose **ICHRA_MARKET**, save.
   Reopen; confirm both persisted.
3. Create a `CUSTOM` `ProposalSection`, `scope = SCOPED`, associated to **that enhancement only**.
   ⚠️ **Add no LOS association** — §1.4a: a LOS association makes the section render unconditionally
   and the gate never runs. Put `{{ICHRA_AGE_BAND_TABLE}}` and `{{ICHRA_PAYLOAD_AS_OF}}` in its HTML.
   Confirm `SELECT * FROM proposalsectionlos WHERE section_id = <id>` returns **zero rows**.
4. Ensure `enhancement_los` links the enhancement to the plus-tier LOS. (No `RateTable` row is needed —
   that is what §5.2's membership widening buys.)
5. Build a proposal through the plus-tier intake with ZIP/county/headcount and **market ticked only**.
   Query `payload_json`: `schemaVersion` is `2`, `sections` carries all four keys,
   `ICHRA_MARKET.selected` and `.complete` are both true, the other three `selected: false`.
6. View the public `/proposal/*` page. **The section renders.** This is the first time a
   `system_managed` enhancement's section has ever been visible — the whole point of the build.
7. Untick market (and everything else) on a second proposal; confirm `selected: false` and that the
   section is **absent** from the public page — not empty, absent.
8. On a proposal with no snapshot row at all, confirm the page renders with no `{{...}}` literal, no
   section, and **no 500**.
9. Regression, the one that matters: view an existing non-ICHRA proposal on another line of service
   carrying LOS-scoped `CUSTOM` sections. Confirm byte-identical render — §5.2's `addAll` contributes
   an empty set when nothing is flagged.
10. Age bands with **no contribution**: confirm rows now appear in `proposal_ichra_snapshot_band` with
    `net_per_employee` and `band_net` NULL. Before V091 this wrote zero band rows.

### Build 2 — `ICHRA_CONTRIBUTION`, contribution scenarios

**No migration.** Every input already exists (`intake.monthly_contribution_per_employee`,
`snapshot.contribution`), build 1 wrote the `sections` gate, and V090's `payload_json` carries what a
scenario needs.

- `ProposalBuilder.java` — nothing new to collect. Build 1's §4.3 rule already computes
  `ICHRA_CONTRIBUTION.complete`.
- `ViewProposal.java` — one new token, `ICHRA_CONTRIBUTION_SCENARIO_TABLE`, built inside
  `putIchraPayloadTokens` (`:875-954`) in that method's existing shape: read `payload.ageBands` and
  the snapshot's own `contribution`; emit per-band premium / contribution / net when bands exist, and a
  single group-level low–high net row from `group_monthly_low`/`group_monthly_high` when they do not
  (RANGE mode — §2: bands are a fidelity upgrade, not a gate). **Computed from the frozen payload, never
  re-fetched from `rating_area_rate_cache`** — T162's point-in-time rule. `""` on anything absent.
  ⚠️ **No ranking, no recommendation, no default scenario.** A table of what the entered contribution
  produces, not a menu of options — the standing no-shopping-layer boundary.
- Configuration: a second enhancement keyed `ICHRA_CONTRIBUTION`, a second `CUSTOM` section, again
  **enhancement-scoped only**.

**Verification:** build a proposal with market + contribution ticked and a contribution entered →
section renders, figures match `snapshot.contribution` × the payload's bands. Tick contribution and
leave the amount blank → `complete: false` and the section is **absent** while the market section still
renders. That last check is the one that proves per-section independence.

### Build 3 — `ICHRA_COMPARISON`, comparison against their group plan

**No migration** — V091 already added `current_total_monthly_premium` and
`current_employer_monthly_share`.

- `proposalBuilder.jsp` — two fields, hidden unless #3 is ticked, required when it is (§8.7).
- `ProposalBuilder.java` — build 1 already persists both to intake. Add a `groupComparison` sub-block to
  the payload freezing the three figures (current total, current employer share, planned contribution)
  and the derived employer delta, so the rendered comparison is point-in-time and never recomputed.
  **No `schemaVersion` bump** — an absent `groupComparison` is unambiguous, because
  `sections.ICHRA_COMPARISON` already says whether it was selected. That is the principled line: bump
  when an absent block's *meaning* is ambiguous (§4.4), not merely when a block is added.
- `ViewProposal.java` — one token, `ICHRA_GROUP_COMPARISON_TABLE`, same shape and same degradation.
- Configuration: a third enhancement/section pair.

**Verification:** enter a current premium and employer share → section renders with all three figures
and the delta. Blank one of them → `complete: false`, `missing` names the blank one, section absent,
the other two sections unaffected. Enter a share **larger** than the total → the section still renders
(§4.3: record, do not adjudicate).

### Build 4 — `ICHRA_AFFORDABILITY` — ⛔ **BLOCKED**

Not buildable, and not for a technical reason.

`ProposalIchraSnapshot`'s own class javadoc states the entity *"deliberately carries no affordability
figure of any kind — `/proposal/*` is public and unauthenticated (LA-12), a materially weaker audience
guarantee than the illustration page behind `IchraAccessResolver`"* (S19-D §1.1, recorded independently
in `legal_assumptions.md` LA-15 and `session_closeout_2026-08-01_session6.md`). S19-D then deliberately
defined **no token** for the payload's `affordability` block for that reason, and named it BLOCKED
item 2.

**An LOS-scoped proposal section renders on exactly that public page.** Section #4 is therefore the
direct collision S19-D declined to walk into. Building the payload half is already done
(`buildAffordabilityBlock:1023-1070`, per-band since S19-F); what cannot ship is anything that renders
it.

**Proposed `LA-NN` entry — written here, needs filing, NOT filed by this run.** The register's highest
entry today is **LA-17** (`legal_assumptions.md:998`), so this reads as LA-18 — but that file's own
index gap warning (`:118`: LA-14/15/16 exist in the register and were never added to its summary table)
means the number must be re-confirmed against the register at filing time, not assumed.

> **LA-NN — An employer-facing affordability comparison may appear on the public proposal page.**
>
> **The claim.** The subsidy-preserving contribution ceiling and the on-exchange LCSP premium
> underlying it may be rendered on an unauthenticated `/proposal/*` page, framed as a figure computed
> for the employer about its own offer, when the proposal reaches that page through an agent-composed,
> agent-sent, agency-branded link (LA-17's channel).
>
> **What it is in tension with.** LA-12 — affordability is computed for the employer, never presented
> to an employee as a determination. The public proposal page has no audience control: the employer
> may forward the link to an employee, and nothing in AMS prevents it. LA-15 — a subsidy-preserving
> ceiling is a different object from an affordability threshold, and *"a ceiling once presented to an
> employer as a target has been acted on."* LA-16 — data arriving through an unauthenticated proposal
> link carries a weaker guarantee than anything behind `IchraAccessResolver`.
>
> **Reversal cost. ⭐ Not low, one direction only.** Adding the section later, once permitted, is
> additive and cheap. Removing it after employers have acted on a ceiling is not a display edit — the
> figure has already been used to set a contribution. This asymmetry is why the section is blocked
> rather than shipped-and-reviewed.
>
> **What would settle it.** An explicit decision that either (a) the figure may appear, under a stated
> framing constraint that LA-15's threshold-vs-ceiling wording discipline would bind directly, or
> (b) it may not, in which case section #4 renders only on `ProposalDetail` (T164's authenticated
> PSP-admin surface) and the public section is retired from this model permanently.
>
> **Status:** Assumed — **unresolved. Blocks build 4.**

**Builds 1, 2 and 3 are not blocked by this.** None of them renders an affordability figure, and §5.3's
predicate withholds `ICHRA_AFFORDABILITY` correctly with no code of its own — its enhancement simply
never has a section configured. Build 1's form must therefore **not render the #4 checkbox at all**
(§8.6) — a visible-but-disabled control invites the question every time an agent sees it.

---

## 8. Build-ready — item 1, executable with no further design decisions

### 8.1 Migration
`docs/migrations/V091__ichra_section_selection.sql`, §6 verbatim. Substitute the real build date for
`<build date>`; re-read `docs/migrations/` and confirm V091 is still unclaimed.

### 8.2 Payload additions
`schemaVersion` → `2`. New top-level `sections` object, §4.1's shape, always present with all four keys
whenever a payload is written. Completeness per §4.3. Serialized by the existing
`ICHRA_PAYLOAD_GSON` (`ProposalBuilder:58`) — `serializeNulls` is already correct for this block, and
`missing` is an empty array rather than null when complete.

### 8.3 Resolver
§5.3's predicate replacing `FlaggedEnhancementResolver.java:56`; §5.2's
`systemManagedIdsForProposal` added to the same class; §5.2's single `addAll` line in `ViewProposal`
after `:282`. Behaviour table §5.4 is the acceptance criteria.

### 8.4 Server-side selection derivation — authoritative
```java
boolean secContribution  = "on".equals(request.getParameter("sectionContribution"));
boolean secComparison    = "on".equals(request.getParameter("sectionComparison"));
boolean secAffordability = false; // build 4 blocked -- never read from the request
boolean secMarket        = "on".equals(request.getParameter("sectionMarket"))
                        || secContribution || secComparison || secAffordability;
```
The JS in §8.7 mirrors this for the agent's benefit; **the server never trusts it.** #1 is the base
layer, so selecting any other section selects #1 whether or not its box was ticked.

### 8.5 Admin surface
`ServiceManagerAction.editEnhancement` (`:201-210`) writes `systemSectionKey`, trimmed, blank → null.
`serviceManager25.jsp` (`:944-951`) gains a fixed five-option `<select>`. Help text: *"Which
system-generated section this enhancement drives. Only meaningful when System Managed is ticked."*

### 8.6 Form — the checkbox block
Inside the existing `#ichraIntakePanel` (`proposalBuilder.jsp:194-300`), which is already gated on
`ichraAvailable and not empty ichraPlanYear` and shown by JS only for a plus-tier LOS — so no new
visibility gate is needed. Place it **above** the ZIP/county row, since it determines what that row
requires.

- `name="sectionMarket"` — "Market illustration data"
- `name="sectionContribution"` — "Contribution scenarios"
- `name="sectionComparison"` — "Comparison against their current group plan"
- **No `sectionAffordability` control** — build 4 is blocked (§7).

Two `intake*`-prefixed fields for #3, in a `<div id="intakeComparisonFields" style="display:none;">`:
`name="intakeCurrentTotalPremium"` and `name="intakeCurrentEmployerShare"`, both
`type="number" min="0" step="0.01"`. ⚠️ The `intake*` prefix is mandatory — the form already posts
un-prefixed hand-off fields and `request.getParameter` returns the first
(`proposalBuilder.jsp:190-193`).

⚠️ **Configuration rule, restated because it is the one that silently breaks the whole feature:** each
of the four sections is a `CUSTOM` `ProposalSection` with `scope='SCOPED'`, **one
`proposalsectionenhancement` row and zero `proposalsectionlos` rows.** A LOS association makes
`ViewProposal:292-299` match first and the gate never runs.

### 8.7 Show/hide and mandatory-field rules
Extend the existing JS, reusing its own helpers (`ichraIntakeComplete()` at `~:822`,
`ichraHasValidBand()` at `~:749`, `ichraBandTotalLives()` at `~:738`) — do not add a second convention.

- **Rule A.** Ticking #2 or #3 ticks #1 and marks it read-only, with a note: *"Market illustration data
  is included automatically — the other sections are built from it."* Cosmetic only; §8.4 is
  authoritative.
- **Rule B.** #1 selected → ZIP, county, and (headcount ≥ 1 **or** ≥ 1 valid age band) required.
  Already exactly what `ichraIntakeComplete()` enforces; extend it to return `true` early when **no**
  section is selected, so an agent who wants none is not blocked by the panel.
- **Rule C.** #2 selected → `intakeContribution` required (it is `placeholder="Optional"` today,
  `:239-240`; swap the placeholder and mark it required only while #2 is ticked).
- **Rule D.** #3 selected → `#intakeComparisonFields` shown, both fields required, **and**
  `intakeContribution` required (the planned contribution is #3's third input).
- **Rule E.** #4 — not rendered. When it ships it additionally requires ≥ 1 age band; headcount alone
  is never sufficient, because ICHRA affordability tests against the LCSP *for the employee's age*.
- **Hidden fields never block.** `#intakeComparisonFields` is hidden, not removed, following the
  panel's own established convention (`#intakeHeadcountField`, `:219-227`) — and the readiness gate
  must skip required checks on anything currently hidden, or `btnCreate` is permanently disabled.

### 8.8 Verification walk
§7 build 1, steps 1-10, in order. Step 6 is the acceptance criterion (a `system_managed` section
renders for the first time); step 9 is the regression that must not move.

### 8.9 Nothing in item 1 is blocked
Every decision build 1 needs is made above. The only blocked item in this spec is build 4, and it
blocks nothing in builds 1-3.

---

## 9. Close-out

### 9.1 Decisions, and what each closes

1. **The `sections` block, schemaVersion 2, four always-present keys, `selected`/`complete`/`missing`.**
   Closes the payload half of "what does the resolver read."
2. **`enhancement.system_section_key VARCHAR(32)`, values identical to the payload keys.** Closes the
   Rule-4 discriminator question S19-D named BLOCKED item 1 and T165 could not answer. Applies V089's
   own recorded reasoning (a PSP-scoped column, not a global constant, and never an id literal).
3. **Selections stored in BOTH `proposal_ichra_intake` and the payload.** Closes the "does the payload
   suffice" question. It does not: the snapshot is not written for a county with no warmed production
   rates, which is most of Texas.
4. **Membership widened via `systemManagedIdsForProposal`, not via a $0 `RateTable` line.** Closes a
   question nobody had asked, because nobody had traced `proposalEnhIds` to `SalesDAO.getPricing`.
5. **#1 promoted to a full selection, derived server-side from the other three.** Closes how a market
   section renders alone under a per-enhancement gate.
6. **The band `NOT NULL` widening ships in build 1, not later.** Closes §1.6's silent data hole.
7. **Build 4 blocked pending an `LA-NN` entry; its text drafted, not filed.** Closes nothing — it keeps
   S19-D's BLOCKED item 2 blocked, deliberately, rather than letting a build sequence quietly resolve a
   compliance question by shipping.

### 9.2 New assumptions, and reversal cost

| Assumption | Reversal cost |
|---|---|
| A PSP admin will configure four enhancement + section pairs by hand; nothing is seeded | **Low.** A seeder is additive. Seeding rows for a PSP-scoped, PSP-worded classification is what V089 explicitly declined to do |
| `system_section_key` as a free VARCHAR with the admin dropdown as the only enforcement | **Low.** A CHECK constraint or lookup table is additive. Matches `proposal_section.scope`/`section_type`, both bare VARCHARs |
| Payload keys identical to column values, no mapping layer | **Low**, and reversing it would *add* a drift surface. The reason not to map |
| The four sections carry no `proposalsectionlos` rows — enforced by convention and by the verification walk, not by code | ⚠️ **Not low, and the weakest link in this spec.** A future admin who adds a LOS association silently disables the gate for that section, with no error. Named in §9.4 as needing a guard |
| `suppressed = false` in the membership query, where the existing pricing path has no such filter | **Low.** Additive filter, withholds rather than reveals |
| Sections 2 and 3 need no migration beyond V091 | **Low.** Both are additive if wrong |
| The resolver's per-call `findByProposalId` + parse is cheap enough (≤ 4 flagged enhancements, L2 cache, same proposal id) | **Low.** A per-request memo is a local change confined to one class |

### 9.3 Open questions, and who settles them

1. **The `LA-NN` affordability entry.** Kevin/counsel. Blocks build 4 alone. Draft text in §7.
2. **Should a section with a `proposalsectionlos` row be refused at configuration time?** §9.4. Kevin,
   or whoever builds item 1 — it could be a one-line validation in the section admin UI, or left as a
   documented rule. This spec assumes documented; a guard would be strictly better.
3. **Does `ProposalDetail` (T164) render `missing` for a PSP admin?** §4.2 stores it for that purpose.
   T164's call, not this spec's.
4. **Carried forward from S19-D §8 item 5, still unverified:** whether any real `rating_area_rate_cache`
   row anywhere has a populated `onexLcspPremium`. Build 4's data depends on it; no database was queried
   this run.
5. **O25 (carrier names).** Untouched. `putIchraPayloadTokens:918` still withholds `name`/`issuerName`.

### 9.4 Contradictions found — including what this prompt asserts that the repository disproves

1. ⚠️ **"Each choice is a separate special enhancement, so each can be scoped to its own LOS page"
   (§1 of this prompt) is right about the enhancement and wrong about the mechanism.**
   `ViewProposal:292-299` matches the LOS branch first and never calls `FlaggedEnhancementResolver`. A
   section carrying both a LOS and an enhancement association renders unconditionally. The four
   sections must be enhancement-scoped only. §1.4a, and the reason §8.6 states it as a hard rule.
2. ⚠️ **The payload alone cannot enable a section.** `proposalEnhIds` (`ViewProposal:277-282`) is
   derived from priced `RateTable` rows via `SalesDAO.getPricing:336-341`, so a `system_managed`
   enhancement — which by the admin UI's own words has no pricing role — never enters the set and its
   section is unreachable regardless of what the resolver would answer. §1.4b.
3. ⚠️ **T165's stated promise does not hold.** `project_backlog.md:225`: *"this build never requires a
   second edit to `ViewProposal.java`."* True for the predicate, false for membership — one line is
   required (§5.2). Stated rather than worked around.
4. ⚠️ **`project_backlog.md:223` (T163) is stale.** It says no admin UI exists to set
   `enhancement.system_managed`; `ServiceManagerAction.java:207` and `serviceManager25.jsp:944-951`
   set it, shipped in S19-A (`3baf60f`). This run may not edit the backlog — **needs correction.** §9.5.
5. ⚠️ **`ProposalIchraIntake`'s class javadoc is stale.** `:19`: *"Nothing reads this table yet — T126."*
   `ViewProposal:463` and `:708` both read it. Corrected as part of build 1 (§7, edit 2).
6. **`proposal_ichra_snapshot_band` band rows are silently not written when no contribution was
   entered** (`ProposalBuilder:942`). Not a contradiction of this prompt — this prompt predicted the
   `NOT NULL` problem correctly (§3 item 5) — but the live consequence is worse than "blocks band
   persistence": it is already happening on every contribution-less AGE_BAND proposal today. §1.6.

Everything else this prompt asserted held: `serializeNulls()` is present and post-S19-O
(`ProposalBuilder:58`); the resolver is an unconditional stub (`:56`); #3's two figures have no home
anywhere (verified column by column against both tables); `netPerEmployee`/`bandNet` are `NOT NULL` and
contribution-derived; `proposalsectionlos` shipped in V037; the three special enhancements are a concept
with no rows.

### 9.5 Needs a T-number — described, not filed (this run assigns none)

1. Build 1 — the four-section foundation: V091, the discriminator, the `sections` block, the resolver
   predicate, the membership widening, the form. §7/§8.
2. Build 2 — `ICHRA_CONTRIBUTION` scenarios. §7.
3. Build 3 — `ICHRA_COMPARISON`. §7.
4. Build 4 — `ICHRA_AFFORDABILITY`. **Blocked** on the `LA-NN` entry drafted in §7.
5. **Correct T163's status** in `project_backlog.md` from 📋 Planned to resolved, citing `3baf60f`
   (S19-A). §9.4 item 4. A doc-defect row, same class as T167.
6. **A guard against LOS-associating a system-managed section** — §9.3 item 2. Optional but strictly
   better than a documented convention.

### 9.6 SQL close-out audit

- **§6's DDL is recommended, in a spec, not in a versioned migration.** Stated exactly that way. No
  `.sql` file was created by this run.
- **Current highest migration version, read directly from `docs/migrations/` this run: V090**
  (`V090__proposal_ichra_payload.sql`). V091 is the next available and is a **proposal**, not a
  reservation — the build run re-reads the directory.
- **Dev and production are both at V090.** No environment moved this run.
- **Orphaned `.sql` files:** none observed, none created.
- **Schema described but not scripted:** the entirety of §6. By design — that is the output of a
  Phase A run.
- **No `INSERT INTO constant`** anywhere in §6. Self-registration is `INSERT IGNORE INTO
  schema_version` plus the `CREATE OR REPLACE VIEW schema_info` block, both present.

### 9.7 Code-verified vs. runtime-verified

**This run ran nothing.** No code was compiled, no migration applied, no database queried, no page
loaded. Everything in §1 is **code-verified** — read directly from the working tree at `3cb9497`, with
file paths and line numbers cited throughout — and everything in §2-§8 is **reasoned from those reads**,
which is weaker still. Nothing here is runtime-verified. §7's verification walks exist precisely because
this run cannot perform any of them.

### 9.8 Scope-fence compliance

One file created: `docs/analysis/S20A_ichra_sections_spec.md`. **Nothing edited.** No `.java`, no
`.jsp`, no `.sql`; nothing under `docs/migrations/`; not `migration_tracker.md`, not
`project_backlog.md`, not `legal_assumptions.md`, not `S19D_ichra_payload_spec.md` — all read, none
written. **No T-number assigned** (§9.5 describes six items needing one). **No `LA-NN` number
assigned** — §7 drafts the entry's text and explicitly declines to claim a number, per that register's
own index-gap warning.

**Standing compliance boundaries observed throughout:** no shopping or steering layer — nothing in this
spec ranks, curates, recommends, or defaults a plan (§7 build 2 states it explicitly for the one section
that could have drifted there); affordability is computed for the employer and is the one section
**blocked** rather than shipped; off-exchange-only means any plan display stays definitionally
incomplete and `planLandscape` remains T166's, unread by this spec; no enrollment path originates in
AMS; the design census stays minimal — `sections` and the new intake columns carry selections and
employer-level dollar figures only, no employee identifier of any kind, no SSN, no roster.

### 9.9 Recommended next step

File a T-number for build 1 and execute §8. It is the whole structural spine — after it, builds 2 and
3 are a token, a section row, and (for #3) two form fields each, with no migration and no new
architecture. Do **not** start with #2 or #3: neither can render without #1's gate, its payload block,
and its membership widening.

Before typing any of it, put §7's `LA-NN` draft in front of Kevin as a direct question — *"does an
`LA-NN` entry need to be written before an affordability figure can appear on a public proposal
page?"* — so build 4's status is settled by a decision rather than by build 1 shipping and the question
going quiet.
