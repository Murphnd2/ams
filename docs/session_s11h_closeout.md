# Session 11-H close-out — the Market page: a conditional page in the proposal sequence

**Run:** S11-H (Opus). Touches `viewProposal.jsp`, the render path for every proposal for every line of
service in a live system. S11-F Phase A settled the mechanism; Step 2 of this run closed the gate.

**Branch:** `refactor/modernize-architecture` (trunk).

**One-line summary:** a new `MARKET` section type appears in the proposal sequence only when the
agency is ICHRA-entitled, the proposal quotes a plus-tier LOS, and production-sourced rates exist for
the intake county — the second consumer of S11-G's `RateCacheDAO.check()`. A proposal with no `MARKET`
section row, which today is every proposal, renders exactly as before.

---

## Baseline

- **Hash at preflight (post-pull):** `c94ae5ae87fa3572a5ca381feb7ebca9bae7643b`
- ⚠️ **Reported difference, per the prompt's instruction to report and not stop:** the prompt expected
  `d49b09b`. Actual start was `c94ae5a`. **Benign** — `c94ae5a` is S11-G's own
  "record the close-out commit hash" commit, which post-dates the prompt's authoring and is a
  docs-only change to `docs/session_s11g_closeout.md`. `d49b09b` is its immediate parent. No source
  file differs between them.
- **Hash at end (feature commit, pushed):** `c3232dabbc469ca8ea4f55947bb8ae68aac3792d`
- **Hash at end (this close-out commit):** `c5f8cf1ccb148d3f768d65ed6b6596970764ea47`, read from
  `git log` after push, never carried from the prompt and never written before the push.

Preflight passed all four gates: branch `refactor/modernize-architecture`, `git status --short` empty,
`git pull --ff-only` reported "Already up to date."

## Step 2 — discovery findings, in full

### 1. `isPlusTierScoped` — what it actually asks

`ViewProposal.java:407-425` (pre-edit numbering), printed in full during discovery:

```java
private boolean isPlusTierScoped(ProposalSection section) {
    try {
        List<LOS> sectionLos = section.getLosList();
        if (sectionLos == null || sectionLos.isEmpty()) return false;
        for (LOS los : sectionLos) {
            if (los != null && los.isPlusTier()) return true;
        }
        return false;
    } catch (Exception e) { ... return true; }
}
```

**It asks whether a given SECTION's own LOS association contains a plus-tier LOS.** It never looks at
the proposal. That is the correct question for T129's blanket unentitled filter — "is this *section*
plus-tier content" — and **the wrong question for this page.** The Market page's condition is "is the
*proposal* quoting a plus-tier line of service", and a `MARKET` section is deliberately not
LOS-scoped, so `isPlusTierScoped` would return `false` for it every time and could never express the
gate. **The two are different questions and this run used the other one.**

### 2. How the proposal's quoted LOS set is determined, and reachability from `doGet`

`proposal.getLosList()`, eagerly loaded by `doGet`'s own opening query
(`ViewProposal.java:77`):

```java
Query q = em.createQuery("SELECT DISTINCT p FROM Proposal p LEFT JOIN FETCH p.losList WHERE p.applicationGUID = :guid");
```

**Fully reachable from `doGet`, already fetched, no extra query.** It is already iterated twice in the
same method — `:133` (feature module collection) and `:243` (scope filtering). The proposal-level
plus-tier idiom already exists in the codebase at
`ProposalBuilder.attachIchraIntakeIfPresent:456-463`:

```java
boolean anyPlusTier = false;
for (LOS los : proposal.getLosList()) {
    if (los.isPlusTier()) { anyPlusTier = true; break; }
}
```

This run mirrors that loop exactly, which has a useful property: **the page can only appear on a
proposal the intake row was itself allowed to be written for**, since both use the identical condition.

### 3. How "plus tier" is identified on a LOS

A **mapped boolean flag column**, `LOS.java:48-49`:

```java
@Column(name="is_plus_tier", nullable = false)
private boolean plusTier;
```

V086. **Not a `constant` row, not a literal id.** ⚠️ **Hard stop #1 therefore did not fire** — no
hardcoded LOS id exists anywhere on the path this run used, so `GenerateProp25`'s existing build-rule-4
violation was not joined by a second one.

### 4. Does `ICHRA_GATED_SECTION_TYPES` + the `:309` filter already achieve the plus-tier condition?

**Only the entitlement condition, not the plus-tier one.** That filter block runs `if (!ichraEntitled)`
and does two things: drops sections whose *type* is in `ICHRA_GATED_SECTION_TYPES`, and drops sections
where `isPlusTierScoped(s)` is true. Both are section-level, and both fire only for an **unentitled**
audience. For an *entitled* one the block does not run at all, so it says nothing about whether the
proposal quotes a plus-tier LOS. Adding `MARKET` to the set therefore buys defence-in-depth for the
unentitled case and nothing more — the plus-tier condition had to be expressed separately, in
`resolveMarketPage`.

### 5. The features-page `<c:when>`/`<c:if>` pair, for shape

`viewProposal.jsp:174-179`, inside the `<c:choose>` at `:168`:

```jsp
<c:when test="${section.getSectionType() == 'FEATURES'}">
  <c:if test="${not empty features}">
  <div class="proposal-section features-section">
    <%@ include file="proposalFeatures.jsp" %>
  </div>
  </c:if>
</c:when>
```

The new `MARKET` branch matches this shape exactly: `<c:when>` on type, `<c:if>` on a server-resolved
visibility attribute, `<div class="proposal-section market-section">`, `<%@ include %>` of a fragment.

### 6. The agency-override block, and what a `MARKET` row carrying `agency_id` would do

`ViewProposal.java:317-351`, read for reference only and **not edited** (forbidden by the fence).
Confirmed in my own words: **both of its branches guard on
`"TITLE".equals(t) || "CLOSING".equals(t)`.** A `MARKET` row carrying an `agency_id` would match
neither guard, fall straight through the `if`, and be added to `finalSections` unconditionally — so it
would **render on every agency's proposal regardless of which agency it belongs to**, and in the
`proposalAgency == null` branch it would survive the filter for the same reason. That is precisely why
this run ships the page agency-agnostic and the admin action never sets an agency. Documented in the
`createMarketSection` comment so a future run does not "helpfully" add an agency picker without also
extending that block.

### ⚠️ Hard stops — neither fired

- **Literal LOS id on the path:** none. Plus-tier is the `is_plus_tier` column (finding 3).
- **Condition inexpressible without editing the agency-override block or `GenerateProp25`:** it is
  expressible. The condition needed only `proposal.getLosList()`, which `doGet` already holds, plus one
  `RateCacheDAO.check()` call. Neither forbidden area was read from or written to.

---

## Shipped

- `c3232dabbc469ca8ea4f55947bb8ae68aac3792d` — `feat: conditional Market page in the proposal sequence (S11-H)`.
  5 files, +365/−2: `ViewProposal.java`, `proposalMarket.jsp` (new), `viewProposal.jsp`,
  `ProposalSettings.java`, `proposalSettings.jsp`. Pushed to `origin/refactor/modernize-architecture`.
- `c5f8cf1ccb148d3f768d65ed6b6596970764ea47` — `docs: S11-H close-out, and record the Market page
  against T136` (this document plus the `project_backlog.md` T136 update, as a second commit). Pushed.

## In flight

Nothing uncommitted at feature-commit time. `git status --short` was clean between the two commits.

## Decisions made

- **Used the proposal-level plus-tier check, not `isPlusTierScoped`.** Step 2 finding 1 established
  these are different questions. Documented at length in `resolveMarketPage`'s javadoc so the next
  reader does not "consolidate" them.
- **`MARKET_SECTION_TYPE` declared on `ViewProposal`, not on an entity.** The `ICHRA_ILLUSTRATION`
  precedent puts the constant on `ProposalIchraSnapshot` because an entity owns that page's data.
  **No entity owns the Market page** — it renders from the live rate cache, not a persisted row — and
  `ProposalIchraSnapshot` is not in this run's writable set anyway. `ProposalSettings` (same package)
  references `ViewProposal.MARKET_SECTION_TYPE`, so the admin CRUD and the render gate cannot drift.
- **The new `<c:when>` was appended after `CUSTOM`, the last existing branch.** Placement inside a
  `<c:choose>` is cosmetic — every test is a mutually-exclusive equality check on section type, and
  actual page order comes from `sort_order` — so appending at the end gives the smallest, least
  ambiguous diff: **12 added lines, 0 deletions, no existing branch touched.**
- **Section scope is `ALL`, with no LOS attached** — unlike `ICHRA_ILLUSTRATION`, which is `SCOPED`
  from creation. Visibility is already fully resolved server-side per proposal (including the
  plus-tier requirement), so section-level LOS scoping would be a second, independently-maintained
  copy of the same condition, free to drift.
- **Figure extraction is duplicated from `putIchraMarketTokens` rather than shared.** `resolveMarketPage`
  repeats the tobacco filter, the `byAge` map, the age-40-row rule for counts, and the newest-`fetchedAt`
  scan. This is deliberate under the fence: `putIchraMarketTokens` is explicitly forbidden to modify, so
  extracting a shared helper was not available. The duplicated block carries comments naming
  `putIchraMarketTokens` as the origin of each non-obvious rule so the two cannot silently diverge
  unnoticed — **but they can diverge, and that is a real cost.** See "Open questions".
- **Nothing seeded.** `initializeDefaults` was not extended, per the prompt and per the same reasoning
  the `createIchraSection` comment already records: it only fires for a PSP with zero sections, and
  every real PSP already has the four defaults.

## New assumptions

- **LA-S11H-1: `<fmt:formatNumber type="currency">` on a `BigDecimal` in `proposalMarket.jsp` renders
  the same way `NumberFormat.getCurrencyInstance(Locale.US)` does in `putIchraMarketTokens`.** The two
  market surfaces (this page and the legacy `{{ICHRA_FLOOR_*}}` tokens) could in principle format the
  same premium differently — JSTL's `fmt` honours the request locale, the Java path pins `Locale.US`.
  Matches `proposalIchra.jsp`'s existing convention, which is why it was chosen. **Reversal cost:
  trivial** — format in Java in `resolveMarketPage` and pass strings, ~4 lines, if a real render shows
  a mismatch.
- **LA-S11H-2: a county that is `PRODUCTION_OK` will have usable rows at ages 21/40/64.** The guard
  passes on provenance, then the fragment null-guards each age independently, so a partially-populated
  county renders a shorter table rather than failing — but a county that is `PRODUCTION_OK` with, say,
  only age 30 cached would render a Market page with counts and no premium table at all. Not observed;
  the warm job writes all ages together. **Reversal cost: low** — add an age-completeness condition to
  the visibility guard.

## Open questions raised

- **Should the Market page and `putIchraMarketTokens` share their figure-extraction logic?** They now
  duplicate it (see "Decisions"). Consolidating means touching `putIchraMarketTokens`, which was
  out of scope here and is load-bearing for pasted `CUSTOM` HTML in the wild. Worth deciding
  deliberately rather than letting the duplication ossify.
- **Does the Market page belong before or after PRICING in the default order?** This run sets
  `sort_order = max(non-CLOSING) + 1`, i.e. last before CLOSING. Whether that is the right narrative
  position — market context before the price, or after it — is a sales-sequencing judgement, not a
  code one. Kevin can reorder in the admin UI.

## Contradictions found

None against S11-F's spec, which held up on every point Step 2 tested: four files (plus the new
fragment), no migration, no `GenerateProp25` contact, `sort_order` controls position, the type is
free-text. **One refinement:** S11-F's proposed gate `ichraEntitled && PRODUCTION_OK` was, as the
prompt anticipated, incomplete — it omitted the plus-tier condition, which Step 2 closed by
establishing that the proposal-level (not section-level) form is the correct one.

## ⚠️ Code-verified-only disclosure

**Everything in this close-out is code-verified only.** This container has no Tomcat and no database.
The build (`.\mvnw.cmd clean package`) passed, confirming Java compilation and WAR packaging — but
note that **a JSP syntax error would not be caught by that build**, since JSPs compile at first request
in this configuration, so `proposalMarket.jsp` and the new `<c:when>` are unvalidated beyond reading.
Nothing was exercised at runtime: no `MARKET` section row exists in any database, no proposal has
rendered the page, and the guard has never actually evaluated. Session 6 remains the standing evidence
that a read finds less than a walk — it found 12+ defects on the first real walk of code that had been
read three times.

## Verification claims, with the code path that guarantees each

**A proposal with no `MARKET` section row renders byte-identically to before this run.** Guaranteed by
three independent facts, any one of which suffices:
1. The JSP dispatch is a `<c:choose>` over `section.getSectionType()`. With no `MARKET` row in
   `proposalSections`, the new `<c:when>` is never evaluated and emits nothing. No existing branch was
   modified — the diff is 12 added lines, 0 deletions.
2. `resolveMarketPage` sets only new request attributes (`marketPageVisible` and `market*` figures).
   It mutates no existing attribute, no entity, and no collection. Nothing pre-existing reads those
   names.
3. Adding `MARKET` to `ICHRA_GATED_SECTION_TYPES` changes the unentitled filter's behaviour only for
   rows whose type is `MARKET`. **Verified no such row can pre-exist:** the type is set in exactly one
   place (`ProposalSettings.createMarketSection`, new this run), appears in no migration, and is not in
   `initializeDefaults`.

**The new `<c:when>` cannot match any existing section type.** The complete set of section-type
literals in live source is `TITLE`, `FEATURES`, `PRICING`, `CLOSING`, `CUSTOM`, `ICHRA_ILLUSTRATION`,
`MARKET` — seven distinct values, verified by grep. `MARKET` collides with none, and the test is exact
string equality.

## Note to Kevin — admin steps to see the page

Proposal Settings → the section-list toolbar now has a **graph icon** (next to the ICHRA heart icon)
→ **Add Market Section** → Create; then drag it to the position you want in the section list, which is
what sets its `sort_order`. It will still render nothing until T136 lands, because every warmed county
is currently staging-sourced and the guard requires `PRODUCTION_OK` — that is the design working, not a
fault.

## SQL close-out audit

**This run produces no SQL. Stating that explicitly, as required.** No migration was written, run, or
recommended; no `.sql` file was created, modified, or orphaned. `section_type` is a free-text
`varchar(20)` column with no CHECK constraint and no enum, so a new type is data entered through the
admin UI, not schema. **Current highest migration version:** **V088** —
`ls docs/migrations/*.sql | sort | tail -2` returns `V088__proposal_ichra_intake_contribution.sql`
(plus the long-standing non-versioned `seed_ndt125_questionnaire.sql`). Nothing is pending deployment
from this run beyond the WAR itself.

## Next

Two candidates, neither urgent:
- **The figure-extraction duplication** (open question above) — decide share-vs-duplicate deliberately.
- **The agency-scoped Market page**, still blocked exactly where S11-F left it: extending the
  agency-override block is the riskiest edit in this area, since it governs which TITLE/CLOSING row
  renders for every proposal in the system. Not worth doing until there is a concrete need.

Unchanged: **T136 (production HealthSherpa allow-listing) remains the binding constraint.** When it
lands, this page appears with no code change and no redeploy.

## Compliance statement

Scope fence was writable: `viewProposal.jsp` (new `<c:when>` branch only), `proposalMarket.jsp` (new),
`ViewProposal.java` (visibility attribute + `MARKET` in `ICHRA_GATED_SECTION_TYPES`),
`ProposalSettings.java` and `proposalSettings.jsp` (admin CRUD), `docs/analysis/project_backlog.md`,
`docs/session_s11h_closeout.md`. **All seven were used and nothing outside them was written** —
`git status --short` showed exactly these files at each commit.

Forbidden list, confirmed:
- **The agency-override block (`ViewProposal.java:317-351`) was not modified** — read for Step 2
  finding 6, never edited. The type guards at the equivalents of `:382`/`:396` in `ProposalSettings`
  were likewise not extended; `createMarketSection` sets no agency at all.
- **No existing `<c:when>` branch was touched, reordered, reformatted or tidied.** The `viewProposal.jsp`
  diff is +12/−0.
- **No existing type was added to `ICHRA_GATED_SECTION_TYPES`** — only `MARKET`, verified above to be
  carried by no existing row. `CUSTOM` was not added.
- `GenerateProp25`: not opened, not modified, still not on this path.
- `RateCacheDAO.check()`: **called, not modified, and its provenance condition not reimplemented
  inline** — `resolveMarketPage` asks it for the verdict and never re-tests `sourceEnv` itself.
- `putIchraMarketTokens` and the seven legacy market tokens: untouched and still functioning for
  pasted `CUSTOM` HTML.
- `IchraZipLookup`, `IllustrationServlet`, `illustration25.jsp`: untouched.
- No migration, no SQL.

No forbidden git operation was run: no `git add -A`, no `git add .`, no `stash`, `checkout`, `restore`,
`reset`, and no local tag. Staging was by explicit named path, each file listed individually. The build
was run once, successfully, before commit.
