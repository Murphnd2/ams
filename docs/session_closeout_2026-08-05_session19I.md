# Session 19-I Close-out — Age bands in the plus-tier intake block

Date: 2026-08-06 (session 19 continues; filename keeps the session's 2026-08-05 series per the
prompt). Branch: `refactor/modernize-architecture`.

## 1. Shipped

| Hash | What |
|---|---|
| *(this run's commit)* | `feat: S19-I -- age bands in the plus-tier intake block, standalone and from hand-off` — `ProposalBuilder.java` (band emit, two-namespace input resolution, derived mode), `proposalBuilder.jsp` (band repeater UI, prefill, gating). Hash read via `git log -1 --format=%h` in this run and reported in its chat output; a commit cannot contain its own hash. |

No migration. No `.sql`. Nothing deployed by this run.

## 2. Standing design rule (§0's principle, recorded as asked)

**The plus-tier intake block is a first-class intake surface, not a fallback.** Every illustration
fidelity level must be reachable directly in it, and anything the illustration already collected must
arrive prefilled. Both directions must hold:

- **Standalone** — an agent lands on `ProposalBuilder` with no ICHRA parameters and can enter ZIP,
  county, eligible employees, age bands, and contribution from scratch.
- **Handed off** — an agent who did the work in the illustration finds it prefilled and re-types
  nothing.

This is a standing rule for this surface, not a one-run goal. Anything added to the illustration's
intake in future is incomplete until it also exists here.

## 3. Decisions made

1. **§3 item 5 finding — mode was chosen from the URL, and that had to change.**
   `attachIchraSnapshotIfPresent` (`ProposalBuilder.java`) read `mode`, `countyFips` and `planYear`
   from **un-prefixed request parameters only** — the illustration hand-off's namespace, echoed
   through hidden fields at `proposalBuilder.jsp:62-78` (`value="${param.mode}"` etc.). A standalone
   plus-tier intake carries no `mode`, so it wrote an intake row and **never a snapshot** —
   which is precisely why `payload_json.ageBands` could not be non-null on any path.
2. **What changed about mode selection.** `mode` is now taken from the URL when present, and
   otherwise **derived** from what the intake block submitted (`deriveIntakeMode`): any
   `intakeAge{i}` present → `AGE_BAND`; else an `intakeHeadcount` present → `RANGE`; else null (no
   snapshot, exactly as today). This mirrors the illustration's own rule that mode is derived from
   whether a band exists rather than asserted separately (`illustration25.jsp` K3-b note). **A
   hand-off's declared mode is never overridden** — the URL wins whenever it carries one.
3. **Two-namespace input resolution (`ichraParam`).** Every ICHRA input the snapshot writers read —
   `countyFips`, `headcount`, `contribution`, `age{i}`, `count{i}` — now resolves hand-off namespace
   first, `intake*` namespace as fallback. Hand-off wins on every field it supplies, so **an
   untouched hand-off behaves exactly as it did before this run**. `planYear` falls back to
   `resolveCurrentPlanYear(em)`, the same derived source and fail-closed posture
   `attachIchraIntakeIfPresent` already uses.
4. **Contribution stays REQUIRED for an AGE_BAND snapshot — deliberately unchanged.** See §6
   contradiction 1: this is the one thing standing between "bands entered" and "`ageBands`
   non-null", and it was left alone on purpose. Defaulting a missing contribution to zero would
   record *"the employer contributes nothing"* as though the agent had asserted it, making entered
   and assumed indistinguishable — the exact failure the illustration's own W15 note exists to
   prevent. Reported rather than patched.
5. **Eligible Employees when bands exist** — mirrors the illustration exactly: the input is
   **hidden, never removed** (`#intakeHeadcountField`), so nothing is lost switching back, and the
   servlet ignores `headcount` in AGE_BAND regardless. Added beyond the illustration: a read-only
   **derived total** (`#intakeDerivedHeadcountField`, "N from bands") shown in its place, so the
   figure does not simply vanish from the panel. That is a display addition, not a third behaviour —
   the input's hidden-not-removed semantics are unchanged.
6. **Band field names are `intakeAge{i}`/`intakeCount{i}`** — `intake*`-prefixed like every other
   field in this panel, because the form already posts un-prefixed `age{i}`/`count{i}` hidden fields
   for the hand-off and reusing those names would silently collide (`getParameter` returns the
   first). This is the panel's own documented convention, not a new one.
7. **Cap is 6, enforced server-side** (`ProposalBuilder.ICHRA_AGE_BAND_ROWS`), matching
   `IllustrationServlet.AGE_BAND_ROWS` and the six hidden pairs in this form. The illustration's own
   comment warns that raising one without the others silently drops rows 7+ from every proposal
   snapshot; the new constant carries that warning forward in its javadoc.

## 4. How the non-plus-tier path was confirmed untouched

The highest-risk part of the run, checked three ways rather than asserted:

1. **All new server-side emit code is inside the existing `if (ichraAvailable)` block** in `doGet` —
   confirmed by reading the diff; zero lines added outside it. An unentitled session receives no
   `handoffBandsJson`, no `ichraAgeBandMaxRows`, and the JSP's `<c:if>` emits no panel at all.
2. **All new JSP markup is inside the existing `<c:if test="${ichraAvailable and not empty
   ichraPlanYear}">` panel block**, and all new JS is inside the T125 block whose every function
   begins with a null-element guard. The new functions follow that same discipline
   (`ichraBandRowList()` returns `[]` when `#intakeBandRows` is absent; every other new function
   no-ops off a null check).
3. **The submit path cannot see band data for a non-plus-tier proposal.** Two cases: (a) panel never
   rendered → no `intakeAge{i}` parameters exist at all → `deriveIntakeMode` returns null → no
   snapshot, as today; (b) panel rendered but the agent deselected the plus-tier LOS →
   `clearIntakeFields()` runs, and it now **removes every band row** (`ichraRemoveAllBandRows()`,
   added this run precisely because a hidden input still submits) → again no `intakeAge{i}` →
   null → no snapshot. A proposal supplying only a headcount still produces a RANGE snapshot
   exactly as before.

**Pre-existing behaviour deliberately not changed, but worth recording:** `attachIchraSnapshotIfPresent`
has never had a plus-tier gate of its own — it is driven purely by parameters. So an agent who
arrives from a hand-off, then deselects the plus-tier LOS, still gets a snapshot written from the
hand-off's own hidden fields. That was true before this run and is unchanged by it; noted rather
than silently altered.

## 5. New assumptions, and reversal cost

| Assumption | Reversal cost |
|---|---|
| A standalone intake (no illustration) should produce a snapshot at all — previously it produced only an intake row | Low, and it is the run's stated point. Reverting means restoring the URL-only `mode` read; no schema, no data migration. Note it is an additive behaviour change: plus-tier proposals built by hand now write a `proposal_ichra_snapshot` row where they previously wrote none |
| Deriving mode from "any band present, else headcount" is the right rule for a standalone intake | Low — one method (`deriveIntakeMode`), no persistence. It mirrors the illustration's own derivation, so divergence would be visible as an inconsistency between two surfaces rather than as silent wrongness |
| A read-only derived-total display is the right treatment when bands replace Eligible Employees | Low — display only; removing it leaves the illustration's exact hidden-input behaviour underneath, untouched |
| Six bands remains the right cap for this surface | Low but **coupled**: the constant must move in lockstep with `IllustrationServlet.AGE_BAND_ROWS` and the hidden-field pairs. The javadoc on `ICHRA_AGE_BAND_ROWS` states this so the coupling is discoverable at the point of change |

## 6. Contradictions found

1. **⚠️ The AGE_BAND hand-off URL in this prompt's §1 writes no snapshot at all — bands or not — and
   this run does not fix that.** `attachAgeBandSnapshot` returns immediately when contribution is
   null (`ProposalBuilder.java`, the `contribution == null || contribution.signum() < 0` guard,
   unchanged by this run and predating it). §1's URL carries `contribution=` **empty**. So the
   walked hand-off produces no `proposal_ichra_snapshot` row, and therefore no `payload_json`, for a
   reason that has nothing to do with bands. **Bands now reach the writer correctly; a contribution
   must also be present for the snapshot to be written.** In the intake block that means the agent
   fills the (currently optional-labelled) "Monthly employer contribution per employee" field.
   Flagged rather than patched — see §3 decision 4 for why defaulting it to zero would be worse than
   the gap. **This is the highest-value follow-up from this run** and needs a decision, not a
   workaround: either the illustration should carry a real contribution into the AGE_BAND hand-off,
   or the intake block should require one whenever bands are present, or the snapshot writer should
   tolerate its absence. Not this run's call.
2. **The prompt's §1 statement that "`ProposalBuilder` reads none of them" is accurate for `doGet`
   but not for `doPost`.** The un-prefixed `age1..6`/`count1..6` *are* echoed as hidden fields
   (`proposalBuilder.jsp:67-78`) and *are* read by `attachAgeBandSnapshot` at submit time. What was
   missing was any way to (a) see or edit them, and (b) produce them without a hand-off — which is
   what this run adds. The bands were not being "dropped entirely" on the POST path; they were
   invisible on the GET path and unreachable standalone.
3. **§1's "no `headcount` parameter exists on the AGE_BAND path" — confirmed correct**, and it is
   correct *by design*: the illustration hides Eligible Employees the moment a band exists, because
   the counts are the headcount. This run reproduces that rather than adding a headcount to the
   AGE_BAND hand-off.
4. Everything else this prompt asserted was verified and held: the 6-pair cap (`AGE_BAND_ROWS = 6`),
   the S19-G/S19-H prefill working for ZIP and County, and `ProposalIchraSnapshotBand`'s field set.

## 7. Open questions

1. **The contribution requirement** (§6 contradiction 1) — needs Kevin's decision. Until it is
   resolved, `payload_json.ageBands` stays null on any path where contribution is absent, regardless
   of this run's work.
2. **Whether `attachIchraSnapshotIfPresent` should gain a plus-tier gate** (§4's closing note).
   Pre-existing, unchanged, and arguably fine — recorded so it is a decision rather than an
   oversight.
3. **Carried forward unchanged:** S19-D/E/F's two BLOCKED items — the `FlaggedEnhancementResolver`
   predicate, and public exposure of the payload's `affordability` block. Neither is touched here.

## 8. SQL close-out audit

- **SQL produced, run, or recommended this run: none.** Stated explicitly, as asked. No `.sql` file
  was created or edited; nothing under `docs/migrations/` was touched; no database was queried or
  modified by this run.
- **Current highest migration version, read from `docs/migrations/`: V090**
  (`V090__proposal_ichra_payload.sql`).
- `ProposalIchraSnapshotBand` already existed with every field this run needs (`age`, `lives`,
  `floorPremium`, `netPerEmployee`, `bandNet`, `sortOrder`) — no schema was needed, which is why
  none was written.
- **Nothing pending deployment** from this run beyond the code itself; production has not received
  V090 either (unchanged from S19-E's audit).

## 9. Code-verified vs. runtime-verified

**Code-verified:** the project compiles (`mvnw package` → `BUILD SUCCESS`, run without `clean` per
the prompt's live-Tomcat constraint); the diff is confined to the two files in scope; the
band-emit path concatenates only parsed ints, so no request text can reach the page; the
non-plus-tier analysis in §4 is a direct reading of the guards, not an assumption.

**Runtime-verified: nothing.** No walk was performed by this run. Specifically unproven until Kevin
walks it: that the repeater renders and renumbers correctly in a browser; that the hand-off prefill
populates bands; that Eligible Employees hides and the derived total appears; that `btnCreate`
enables on a band-only intake; that a standalone band entry actually produces an AGE_BAND snapshot
with `ProposalIchraSnapshotBand` rows; and that `payload_json.ageBands` serializes non-null (which,
per §6, also requires a contribution). Compiling is not rendering, and code-verified is not
runtime-verified.

---

## S19-J — Contribution becomes optional for the ICHRA snapshot write

Date: 2026-08-06. Hash: `a949fbf` — `fix: S19-J -- contribution is optional for the ICHRA snapshot
write`. Edited: `ProposalBuilder.java` only.

### Decision and its four reasons (§0, recorded as directed)

Relaxed `attachAgeBandSnapshot`'s contribution guard. Contribution is now optional; never
fabricated.

1. The illustration already treats contribution as an optional fidelity step — its own card says
   adding one unlocks net cost and the flip-point slider, capability rather than a gate. The
   snapshot refusing to record ZIP, county, and bands without one contradicted that tiering.
2. The snapshot's job changed under the guard: it was written for cost math, but since S19-E it is
   the carrier for `payload_json` — provenance, `ageBands`, `planLandscape` — none of which need a
   contribution.
3. Corrects a prior finding — see below.
4. Defaulting to zero stays forbidden. Recording "the employer contributes nothing" as though the
   agent stated it is a fabricated employer statement in a frozen document. Null is stored null and
   disclosed as absent (or, per §6 below, not yet disclosed at all — an open item).

### §2 findings

1. **The range path did not carry the same guard — the prompt's premise here did not hold.**
   `attachRangeSnapshot` never reads `contribution` at all; RANGE mode never collects one. Only
   `attachAgeBandSnapshot` had the guard. Verified by reading both methods in full before editing
   either.
2. **`contribution` column nullability — nullable, confirmed both ways.** Entity:
   `ProposalIchraSnapshot.java`, `@Column(name = "contribution")` with no `nullable = false`. Live
   local schema: `SHOW COLUMNS FROM proposal_ichra_snapshot LIKE 'contribution%'` →
   `contribution | decimal(8,2) | YES | | NULL`. No hard stop; no migration needed for this column.
3. **A second NOT NULL finding the prompt did not anticipate, and the reason this run's shape isn't
   a one-line guard removal.** `ProposalIchraSnapshotBand.netPerEmployee` and `.bandNet` are both
   `nullable = false` — and both are *computed from* contribution
   (`floorPremium.subtract(contribution)`). `ProposalIchraSnapshotBand.java` is out of this run's
   fence and no migration was permitted, so neither could be relaxed at the schema level. Resolved
   by **not persisting band rows when contribution is null** — the same "pass `null` bands to
   `ProposalIchraSnapshotDAO.save`" pattern the RANGE path already uses — rather than writing a
   fabricated net figure into a column whose entire purpose is a net-of-contribution breakdown.
   `groupNetTotal`/`employerOutlay` on the snapshot itself (both nullable) are left `null`, not
   `BigDecimal.ZERO`, for the same reason.
4. **The payload serializer needs no change and received none.** `buildIchraPayload`'s `ageBands`
   array (S19-F's schema) reads only `age`/`lives`/`floorPremium` off each band object — never
   `netPerEmployee`/`bandNet`. The in-memory `ProposalIchraSnapshotBand` objects built during the
   loop always carry those three fields regardless of contribution, so the payload's `ageBands`
   populates whether or not a contribution was entered. Confirmed by reading the serializer; not
   touched, as the fence required.

### How each consumer now behaves on null

- **`ProposalIchraSnapshotBand` table** — zero rows written for a contribution-less AGE_BAND
  snapshot (see finding 3). Not "null net figures stored" — no band rows at all, honestly reflecting
  that no net computation happened.
- **`ProposalIchraSnapshot.groupNetTotal` / `.employerOutlay`** — stored `null`.
- **`payload_json.ageBands`** — populates normally (age/lives/premium), independent of contribution.
- **`proposalIchra.jsp`** (read, not edited — out of this run's fence): `<c:forEach var="band"
  items="${ichraBands}">` over zero rows renders an empty `<tbody>`, not an error. `<fmt:formatNumber
  value="${ichraSnapshot.groupNetTotal}">` / `.contribution` on a null value is standard,
  spec-defined JSTL behaviour — no output is written, no exception thrown. Net effect: the page
  currently shows "Group Monthly Net Cost: " and "...contribution ( per employee)." with blank
  currency slots rather than a number — not broken, not a fabricated zero, but not a clear
  disclosure either. **Not runtime-verified** — reasoned from the JSTL spec and this file's own
  read, not observed in a browser.

### Correction to S19-G's 0-rows attribution

**The prompt's proposed explanation does not hold for the walk it names, and this run traced the
real one instead of repeating the guess.** S19-G's spot-check walk was explicitly `mode=RANGE`
(`ProposalBuilder?mode=RANGE&countyFips=48223&...&headcount=3`). RANGE never gates on contribution
(finding 1), so a missing contribution could not have caused that walk's zero rows. Queried the live
local rate cache instead: `SELECT age, source_env, lowest_bronze_premium FROM
rating_area_rate_cache WHERE plan_year=2026 AND county_fips='48223' AND age IN (21,64)` returned both
rows with `source_env = STAGING`. `attachRangeSnapshot` fails closed on exactly this — it refuses to
write from anything but `PRODUCTION`-sourced rates unless the T150 demo override (ssa.properties
flag AND PSP-admin session) was active for that request. **The actual, verified cause of S19-G's
zero rows is locally-cached staging-only rate data for that county, not a missing contribution.**

### Non-plus-tier path confirmed unchanged

`attachIchraSnapshotIfPresent` still requires `countyFips` and a resolvable `mode` (explicit or
derived) before doing anything at all; a non-plus-tier submission carries neither — no `age{i}`,
`intakeAge{i}`, `countyFips`, or `mode` parameters exist on such a submission at the HTTP layer, so
this method returns at its first guard exactly as before S19-J. This run changed nothing about *when*
`attachAgeBandSnapshot` runs, only what it does once already inside it with a valid band set — so
the non-plus-tier path is unreachable-by-construction here, the same conclusion S19-I already
reached and reconfirmed by re-reading rather than re-asserted.

### Open question — not built, per the fence

**Does the rendered proposal need an explicit "no contribution entered" disclosure?** Per §6 above,
`proposalIchra.jsp` currently renders blank currency text rather than a stated absence when
`groupNetTotal`/`contribution` are null — a real gap, but display-only and not a fabrication. The
RANGE branch of the same file already has a precedent for this exact situation
(`<c:when test="${not empty ichraSnapshot.groupMonthlyLow and not empty
ichraSnapshot.groupMonthlyHigh}">` ... `<c:otherwise>&mdash;</c:otherwise>`) that a future run could
mirror for the AGE_BAND branch. Not built here — `ViewProposal.java`/`proposalIchra.jsp` are both
outside this run's fence, and the prompt's own §3 marks any such UI text as out of scope. Recorded as
its own item, not assumed away.

### SQL close-out audit

No SQL produced, run, or recommended by this run. No `.sql` file created or edited; nothing under
`docs/migrations/` touched. The only database interaction was two read-only `SELECT`s (§2 finding 2,
and the correction's rate-cache query) — no `INSERT`/`UPDATE`/`ALTER` of any kind. Current highest
migration version, read from `docs/migrations/`: **V090**
(`V090__proposal_ichra_payload.sql`) — unchanged by this run.

### Code-verified vs. runtime-verified

**Code-verified:** compiles (`mvnw package`, no `clean`, per the live-Tomcat constraint) →
`BUILD SUCCESS`; diff confined to `ProposalBuilder.java`; the nullability findings (§2 items 2-3) are
read directly from the entity mappings and the live local schema, not assumed; the payload
serializer's independence from `netPerEmployee`/`bandNet` is confirmed by reading it, not inferred.

**Runtime-verified: nothing.** Not walked: that a bands-only, no-contribution submission actually
writes a snapshot row with zero band rows and a non-null `payload_json.ageBands`; that a
contribution-bearing submission is bit-identical to its pre-S19-J behaviour; that `proposalIchra.jsp`
renders the blank-currency case as reasoned above rather than throwing. Compiling is not rendering,
and a query against a live schema is not the same as observing the write path execute.

---

## S19-N — T165 milestone: `payload_json` verified carrying real content, end to end

Date: 2026-08-06. Read-only spot-check, no code changes. Kevin created proposal **#137451** on dev
(A1 Door Company, SWBD Pricing, SWBD PremiumPath Program, ZIP 75482, Hopkins County TX, bands 40×3
and 55×2, contribution $200) — the first proposal built after local dev's V087/V088 gap was closed
(S19-M) and this session's staging-source uncertainty (S19-K/S19-L) was resolved.

**Result: T165's core question is answered affirmatively.** Snapshot row, band rows, and payload all
present and mutually consistent: `mode = AGE_BAND`, band rows 40×3/55×2 with correct
`net_per_employee`/`band_net` arithmetic against the $200 contribution, `payload_json.ageBands`
matching those bands exactly (`age`/`lives`/`premium`), `provenance` populated
(`sourceEnv: STAGING`, `capturedAt`, `ratesFetchedAt`, `countyFips`, `countyName`), `schemaVersion: 1`.
`source_env = STAGING` on the write confirms the T150 demo override was active for that session —
the earlier "staging rates block every write" theory from S19-K was correct in principle but this
proposal shows the override does let a real write through when it's on.

Two defects surfaced during that verification, reported rather than patched at the time. Fixed in
this run (S19-O), below.

## S19-O — Two defects from the 137451 verification

Date: 2026-08-06. Hashes: `47c4e19` (defect 1), `7df82ea` (defect 2). Edited: `ProposalBuilder.java`
only, both commits.

### Defect 1 — Gson silently dropped `affordability`/`planLandscape`

`buildIchraPayload` added both as `JsonNull.INSTANCE` but serialized with a plain `new Gson()`,
which omits null-valued object members by default — so both keys vanished from the persisted JSON
instead of appearing as explicit `null`. Confirmed directly against proposal 137451's own
`payload_json` (S19-N): neither key was present at all.

**Why this mattered:** the S19-D spec's whole point in specifying a stable four-key schema with
explicit nulls was so a reader could tell "this proposal has no affordability data" apart from "this
payload predates the affordability block." As shipped, both cases looked identical — no key at all.

**Checked before changing the serializer:** (1) row count — `SELECT COUNT(*), SUM(payload_json IS
NOT NULL) FROM proposal_ichra_snapshot` → `1, 1`. Exactly one non-null payload exists anywhere on
this database (proposal 137451's), and it is not touched by this fix — the fence required leaving it
as-is, and the fix only changes future serialization. No conversion was needed or performed; the
claim that none was needed is now verified against a real count, not assumed. (2) The payload's one
reader, `ViewProposal.putIchraPayloadTokens` — every check there is `payload.has(key) &&
payload.get(key).isJsonObject()/.isJsonArray()`. A `JsonNull` value fails the type check exactly like
an absent key does, so a present-but-null member and an absent member already behaved identically to
that reader. Nothing depended on the omission.

**Fix:** a shared `private static final Gson ICHRA_PAYLOAD_GSON = new GsonBuilder().serializeNulls()
.create()` field replaces the inline `new Gson()` at the serializer's one call site.
`schemaVersion` stays `1`. No migration. What goes *into* the payload is unchanged — only how it
serializes.

### Defect 2 — a band-only proposal wrote no `proposal_ichra_intake` row

`attachIchraIntakeIfPresent` unconditionally required a valid `intakeHeadcount` (1–10000) and
returned early otherwise. S19-I's band-repeater UI hides the real `#intakeHeadcount` input the moment
a band exists, showing only a read-only derived total — so that input submits blank, and the intake
write never happened for any band-only proposal, including 137451 itself (confirmed by S19-N: no
intake row despite a completed proposal).

**This was an S19-I scope oversight, not a defect in what S19-I built.** S19-I made the *snapshot*
write path (`attachAgeBandSnapshot`, via `deriveIntakeMode`/`ichraParam`) fully band-aware. The
separate *intake* write path — a different table, a different entity, a different purpose (T125's
own ZIP/county/headcount collection, distinct from the snapshot's computed rate output; see V087's
own migration header on why the two tables are deliberately separate) — was never updated to match.

**`proposal_ichra_intake`'s actual columns** (read via `DESCRIBE` before deciding anything):
`intake_id`, `proposal_id`, `zip`, `county_fips`, `county_name`, `state`, `headcount` (`smallint NOT
NULL`), `monthly_contribution_per_employee` (nullable, V088), `plan_year`, `collected_at`,
`created_by`. **No band-detail columns exist — only a single `headcount` integer.** Per the prompt's
own branching this made the answer unambiguous, not a judgment call: the derived sum of entered band
counts is what `headcount` gets. No hard stop was needed.

**Fix:** when `intakeHeadcount` parses to `null`, fall back to `sumIntakeBandCounts(request)` — a new
helper that sums `intakeCount1..N` for every row where count ≥ 1. **Deliberately mirrors
`proposalBuilder.jsp`'s own `ichraBandTotalLives()` field-for-field** — same parameter names, same
"count ≥ 1" rule, no age-validity check in either function. This was a specific design choice, not an
oversight: the JS function sums every present count with no age check at all (so even a
freshly-added row with a still-blank age already counts toward the live "N from bands" display), and
matching that exactly — rather than filtering by age validity the way `attachAgeBandSnapshot`'s own
band loop does — is what makes UI and server structurally unable to disagree. They are not two
implementations that happen to agree; they are the same summation rule expressed twice over the same
field names, so there is nothing for a future edit to one side to silently desynchronize from the
other except by construction.

**Headcount-only path confirmed unchanged:** the new fallback only triggers when
`parseIntOrNull(request.getParameter("intakeHeadcount"))` is `null` — a headcount-only submission
supplies that parameter directly and the fallback is never reached; behaviour for that path is
bit-identical to before this run.

**Non-plus-tier path confirmed unchanged, both defects:** defect 1 only changes how an
already-decided payload string is serialized — it cannot affect whether `buildIchraPayload` is
called at all, and that call only happens from inside the AGE_BAND/RANGE snapshot writers, themselves
reachable only when `countyFips`/`mode` are present (never true for a non-plus-tier submission, per
S19-I's own established reasoning, reconfirmed here rather than re-asserted). Defect 2's new code
sits entirely *after* `attachIchraIntakeIfPresent`'s existing `anyPlusTier` early-return gate
(unchanged, still the first substantive check) — a non-plus-tier submission returns there before
reaching any of this run's new code. Confirmed by reading the diff: both changes are nested strictly
inside already-gated code, nothing moved earlier.

### SQL close-out audit

No SQL produced, run, or recommended by this run. No `.sql` file created or edited; nothing under
`docs/migrations/` touched; no `INSERT`/`UPDATE`/`DELETE`/`ALTER`/`CREATE` against any database — the
only database interaction was the read-only row-count check and the read-only `DESCRIBE`. Proposal
137451's existing `payload_json` was not modified, per the fence. Current highest migration version,
read from `docs/migrations/`: **V090** (`V090__proposal_ichra_payload.sql`), unchanged by this run.
Dev is now contiguous through V090 (S19-M); production remains pending V090 only (per the tracker,
V087–V089 already applied there, V090 not yet — unchanged by this run, not re-checked here).

### Code-verified vs. runtime-verified

**Code-verified:** compiles (`mvnw package`, no `clean`) → `BUILD SUCCESS` for both commits in
sequence; diff confined to `ProposalBuilder.java` across both; the Gson-omission mechanism and the
reader's null-tolerance were confirmed by reading the actual code paths, not inferred; the
`proposal_ichra_intake` column list was read directly via `DESCRIBE`, not assumed; the UI/server
total-matching claim is a direct comparison of the two functions' source, not a behavioural
assumption.

**Runtime-verified: nothing.** Not walked: that a new band-only proposal now produces both a
snapshot row *and* an intake row; that the intake row's `headcount` matches what the UI displayed;
that a fresh payload now genuinely serializes `"affordability":null,"planLandscape":null` rather than
omitting them; that a headcount-only (no bands) proposal's intake write is unaffected. Compiling is
not rendering, and reading the two summation functions side by side is not the same as observing
them agree on a live submission.
