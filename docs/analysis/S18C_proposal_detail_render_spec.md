# S18-C — Phase A spec: ProposalDetail section rendering + flagged-enhancement scope predicate

**Run:** S18-C (Phase A), 2026-08-05 · **Branch:** `refactor/modernize-architecture` ·
**HEAD read this run:** `36efe0e` · **Specification only — no `.java`, `.jsp`, `.sql`, or
`.properties` file was created or edited by this run.**

This document is the executable spec for a later Sonnet run. Every anchor below was verified against
the working tree this run. Where the driving prompt's own background claims did not survive
verification, that is called out inline and marked ⚠️ **CORRECTION**.

---

## 0. Verification of inherited background

The prompt supplied eight background facts and instructed that any dependency on them be verified.
Seven held exactly. One did not.

| Claim | Verdict |
|---|---|
| `proposal_section` has no `proposal_id`; membership computed at render time at the cited line ranges | **Holds.** Re-read `ViewProposal.java:254-392` in full this run. |
| `proposalEnhIds` built from `ProposalPriceLine.getModule().getEnhancement()` | **Holds** — `ViewProposal.java:275-280`, verified unique (`grep -Fc` = 1). |
| `ProposalDetail` sets exactly four attributes at `:70-73`, renders no sections | **Holds.** Full read of `ProposalDetail.java` (263 lines) this run. |
| `ProposalDetail` absent from `LoginFilter:87` allowlist | **Holds.** `grep -c "ProposalDetail" LoginFilter.java` → **0**. Allowlist line confirmed at `:87`. |
| G5 = `ViewProposal:142`; G6 = `:466-467`; neither references the demo override | **Holds.** |
| Demo override implemented twice, `ProposalBuilder:596-602` / `IllustrationServlet:723-729` | **Holds** — bodies are byte-identical. |
| `Enhancement` has no type/category/code column | **Holds.** Columns confirmed: `enhancement_id`, `description`, `short_text`, `sort_order`, `suppressed`, `psp_id`, `service_item_id`. |
| Highest migration V088 | **Holds.** |

⚠️ **CORRECTION — Task 2's named regression candidates are the wrong entity type.**
The prompt states "POP, FSA, HRA, COBRA, HSA are live and `SCOPED` on this installation" as
*enhancements*. **They are Lines of Service, not Enhancements.** Verified from the seed code:

- `DatabaseInitializer.java:482` — `createLos(em, 1L, "Flexible Spending Account", "FSA", …)`
- `DemoDataSeeder.java:182` — `createLos(em, 2L, "Flexible Spending Accounts", "FSA", …)`
- `DemoDataSeeder.java:192` — `createLos(em, 3L, "Health Reimbursement Arrangement", "HRA", …)`
- `DemoDataSeeder.java:202` — `createLos(em, 4L, "COBRA", "COBRA", …)`

The **only** seeded `Enhancement` rows in the entire codebase are:

- `DatabaseInitializer.java:485` — `createEnhancement(em, 1L, "Debit Card", "CARD", …)`
- `DemoDataSeeder.java:225` — `createEnhancement(em, 2L, "Debit Card Services", "CARDS", …)`

This materially changes Task 2's regression proof and is handled in §2.4. It does **not** change the
shape of the predicate, because the `SCOPED` filter tests LOS **and** Enhancement in the same block —
but it does mean the named witnesses for "no regression" must be split across both halves.

---

## 1. Task 1 — Section pipeline: extract or duplicate?

### 1.1 Every input the pipeline consumes between `:254` and `:392`

| # | Input | Type | Origin (file:line) |
|---|---|---|---|
| 1 | `em` | `EntityManager` | `ViewProposal.java:94` — `emf.createEntityManager()`; **closed at `:402`, before the JSP forward at `:405`** |
| 2 | `proposal` | `Proposal` | `:98-105` — JPQL with `LEFT JOIN FETCH p.losList` |
| 3 | `pricing` | `List<ProposalPriceLine>` | `:122` — `SalesDAO.getPricingWithAdjustments(em, proposal)` |
| 4 | `psp` | `PSP` | `:252` — `proposal.getProspect().getContact().getPsp()` |
| 5 | `proposalAgency` | `Agency` | `:342` — `OriginatingAgencyResolver.resolve(proposal)` |
| 6 | `ichraEntitled` | `boolean` | `:137` — `IchraAccessResolver.isAvailableForProposal(em, proposal)`; resolved **once**, deliberately (see `:131-136`) |
| 7 | `ichraSnapshot` | `ProposalIchraSnapshot` | `:139-141` — `ProposalIchraSnapshotDAO.findByProposalId(em, proposal.getId())`, only when `ichraEntitled` |
| 8 | `ichraBands` | `List<ProposalIchraSnapshotBand>` | `:145` — `findBandsBySnapshotId`, only on `MODE_AGE_BAND` |
| 9 | intake + market figures | 9 request attributes | `:154` → `resolveMarketPage(request, em, proposal, ichraEntitled)`, `:440-514` |
| 10 | `primaryColor` / `accentColor` | `String` | `:238-241` — `AppConstantDAO.getConstantValue`, with literal fallbacks `#2B5F8A` / `#7AB648` |
| 11 | `tokens` | `Map<String,String>` | `:382` — `buildTokenMap(em, proposal, psp, primaryColor, accentColor, request, ichraEntitled)`, defined `:645` |
| 12 | `request` | `HttpServletRequest` | Servlet parameter — **consumed by `buildTokenMap` for `getContextPath()` only** (`:688`) |
| 13 | `proposalLosIds` | `Set<Long>` | `:268-273` — from `proposal.getLosList()` |
| 14 | `proposalEnhIds` | `Set<Long>` | `:275-280` — from `pricing`, **not** from any per-proposal enhancement selection |

### 1.2 Can the pipeline be lifted into a shared helper without changing rendered output?

**Not as a pure function, no — and the obstacles are specific, not stylistic.**

**Obstacle A — the pipeline is not a function, it is a sequence of `request.setAttribute` side
effects.** Between `:137` and `:398` the method writes **at least 16 distinct request attributes**:
`ichraSnapshot` (`:143`), `ichraBands` (`:146`), nine market attributes (`:497-507`), `proposal`,
`pricing`, `features`, `renderedFeatures`, `primaryColor`, `accentColor`, `pspName` (`:243-249`),
`proposalSections` (`:394`), `sectionHtml` (`:395`), `agencyName` (`:398`). A helper returning a
value would have to have its result re-scattered into the request by the caller, or take the request
and keep the side effects. Either is viable; neither is free.

**Obstacle B — EM lifetime is load-bearing and non-obvious.** The EM is closed at `:402`, **before**
the forward at `:405`. Three separate comments in the file (`:131-136`, `:150-153`, `:426-429`)
document that snapshot, market page, and entitlement are all resolved eagerly *because the fragments
can issue no query of their own*. Any extraction must preserve "everything resolved while the EM is
open" or lazy-load exceptions surface inside the JSP, where they render as a 500 on a
customer-facing page.

**Obstacle C — hard ordering dependency between the filter stages.** The four stages are not
commutative:
1. scope filter (`:282-309`) — must run first; it defines the candidate set.
2. entitlement filter (`:334-338`) — the comment at `:312-315` states this is **deliberately a
   separate pass after scope filtering**, because "scope answers *does this proposal include the
   service*, entitlement answers *may this audience be shown market data at all*, and conflating
   them would make one silently stand in for the other."
3. agency `TITLE`/`CLOSING` override (`:341-378`) — must run after entitlement, because it replaces
   list members and would otherwise reintroduce a section entitlement had removed.
4. token substitution (`:380-396`) — must run last; it only reaches sections still in the list.

**Obstacle D — one servlet-only dependency, and it is trivial.** `buildTokenMap` touches `request`
exactly once, for `request.getContextPath()` (`:688`), to build the Apply-Now URL. That is the only
servlet coupling in the whole token path.

**Conclusion:** extraction is *possible* — Obstacle D is negligible and A/B/C are constraints on
*how*, not blockers. But it is not free, and the value at stake is every proposal for every LOS.

### 1.3 Recommendation: **duplicate, deliberately and narrowly. Do not extract in this build.**

**Reasoning.**

*Regression risk of extraction:* `ViewProposal.doGet` is ~320 lines of interleaved query, filter, and
attribute-set with four ordering-sensitive stages (Obstacle C) and an EM-lifetime invariant
(Obstacle B) that is documented in prose but **enforced by nothing** — no test, no assertion, no
type. An extraction refactor that compiles and looks right can still silently break lazy-loading for
one section type on one LOS, and the failure surface is the public page an employer is reading. There
is no automated test suite covering this path (none was found this run), so the only verification
available is a manual walk per LOS. **The blast radius of getting extraction wrong is every line of
service; the blast radius of getting duplication wrong is the new internal page only.**

*Drift risk of duplication — real, and here is what it actually costs:* the two copies could diverge,
and the specific danger is that a future entitlement or compliance tightening lands on
`ViewProposal` and not on `ProposalDetail`, leaving the internal page more permissive than the
public one. **This risk is bounded by what `ProposalDetail` is for.** It is authenticated,
scope-checked, and shows a proposal to staff who can already see the proposal's pricing, LOS list,
and public link on that same page today. It is not an audience that entitlement filtering exists to
protect from — LA-17's concern is *market data reaching an unentitled prospect*, and there is no
prospect on this page.

*The decision:* duplicate now, extract later if and only if a third consumer appears. Two copies with
a documented pointer is a maintainable state; a speculative extraction that destabilises the public
render path to serve one new internal page is not. **This mirrors the precedent already set in this
codebase for exactly this trade-off:** `isIchraDemoOverride` is deliberately implemented twice
(`ProposalBuilder:596-602`, `IllustrationServlet:723-729`), and `IllustrationServlet:717-719`'s own
javadoc states the reason — independent re-evaluation is the safety property, not a duplication
defect.

**Constraint on the Sonnet run (mandatory):** the duplicated block in `ProposalDetail` must carry a
header comment naming `ViewProposal.java:254-392` as its origin and stating that a change to the
filter semantics there must be mirrored here. Reference this document by name. Do **not** silently
copy.

### 1.4 Anchors — not applicable

Extraction was not selected, so no lines move out of `ViewProposal.java`. **The only edit to
`ViewProposal.java` in this entire build is the single predicate insertion specified in §2.2.**
That is a deliberate outcome of the §1.3 decision and is the primary reason to prefer it.

---

## 2. Task 2 — The flagged-enhancement predicate

### 2.1 `ViewProposal.java:270-315` verbatim

```java
                Set<Long> proposalEnhIds = new HashSet<>();
                for (ProposalPriceLine line : pricing) {
                    if (line.getModule() != null && line.getModule().getEnhancement() != null) {
                        proposalEnhIds.add(line.getModule().getEnhancement().getId());
                    }
                }

                List<ProposalSection> filteredSections = new ArrayList<>();
                for (ProposalSection section : sections) {
                    if (!"SCOPED".equals(section.getScope())) {
                        filteredSections.add(section);
                        continue;
                    }
                    // SCOPED — check if any linked LOS or Enhancement matches
                    boolean matches = false;
                    if (section.getLosList() != null) {
                        for (LOS los : section.getLosList()) {
                            if (proposalLosIds.contains(los.getId())) {
                                matches = true;
                                break;
                            }
                        }
                    }
                    if (!matches && section.getEnhancementList() != null) {
                        for (Enhancement enh : section.getEnhancementList()) {
                            if (proposalEnhIds.contains(enh.getId())) {
                                matches = true;
                                break;
                            }
                        }
                    }
                    if (matches) {
                        filteredSections.add(section);
                    }
                }
                sections = filteredSections;

                // LA-17 / T116 — entitlement gate, half two. A SEPARATE pass after scope filtering,
                // deliberately not folded into it: scope answers "does this proposal include the
                // service", entitlement answers "may this audience be shown market data at all", and
                // conflating them would make one silently stand in for the other.
                //
                // ⚠️ Half one already suppresses all visible output on its own TODAY, because
                // viewProposal.jsp wraps the entire ICHRA <div> in <c:if test="${not empty
                // ichraSnapshot}"> — verified by runtime walk 2026-08-02, not assumed. This pass is
                // therefore defence in depth, and deliberately so: it does not depend on that JSP
```

### 2.2 The exact edit

**Location:** inside the enhancement-matching loop only — the block at `:293-305` above. **The LOS
loop at `:290-297` is not touched.**

**Current inner condition:**
```java
if (proposalEnhIds.contains(enh.getId())) {
    matches = true;
    break;
}
```

**Specified replacement:**
```java
if (proposalEnhIds.contains(enh.getId())
        && FlaggedEnhancementResolver.isSectionEnabled(em, proposal, enh)) {
    matches = true;
    break;
}
```

**Why this position and no other.** The predicate is `&&`-appended *after* the existing membership
test, so it is evaluated only for an enhancement that would already have matched. An enhancement
that does not price on this proposal short-circuits before the resolver is consulted — same work,
same result, same call count as today for every non-matching row.

**The resolver — name, package, signature, and required stub behaviour.**

- **Class:** `FlaggedEnhancementResolver`
- **Package:** `net.superiorstate.ams.data.resolver`
  (verified this run — package exists, 13 classes, no name collision)
- **Signature:**
  ```java
  public static boolean isSectionEnabled(EntityManager em, Proposal proposal, Enhancement enhancement)
  ```
- **Contract, in this order:**
  1. If `enhancement == null` → return `true`. (Cannot be flagged; behave as today.)
  2. If `!enhancement.isSystemManaged()` → return **`true`, unconditionally and immediately.**
     ⚠️ **This is the bit-identity guarantee. An unflagged enhancement must never reach any
     further logic in this method.**
  3. Flagged path — resolve the per-proposal signal. **The signal does not exist yet.** Until the
     ICHRA JSON payload build lands, this branch returns **`false`** (section withheld).
  4. Any exception → return **`false`** (fail closed, matching `isPlusTierScoped`'s stated
     uncertainty discipline at `ViewProposal:538-544`). Never throws; log and return.

**Do not invent the payload shape.** Step 3's body is a stub in this build. The Sonnet run implements
steps 1, 2, and 4 fully, and step 3 as a single `return false;` with a `TODO` naming this document
and the fact that the payload is a later build.

**Constraint (mandatory): never hardcode an enhancement ID.** The flag column added by V089 is the
**only** discriminator. No `if (enh.getId() == 2L)`, no ID list in a constant row, no ID literal
anywhere in `FlaggedEnhancementResolver` or in the `ViewProposal` edit. This mirrors the reasoning
already recorded in `V086__los_plus_tier.sql:24-28`: the `constant` table has `PRIMARY KEY (name)`
and **no `psp_id`**, so it is global per installation, whereas `enhancement.psp_id` is per-PSP — one
constant cannot name the right rows on a multi-PSP installation, and AMS is multi-PSP by design.

### 2.3 Why `em` and `proposal` are in the signature despite being unused by the stub

They are the inputs the flagged path will need once the payload lands, and adding a parameter later
means touching `ViewProposal.java` a second time. **Touching that file twice is the thing this spec
is trying hardest to avoid.** `em` is in scope and open at the call site (closed at `:402`);
`proposal` is in scope from `:105`. The Sonnet run passes both and the stub ignores both.

### 2.4 How an unflagged enhancement is proven unaffected

**Structural proof (strongest, and it is the reason for step 2's ordering).** Step 2 returns `true`
before touching `em`, `proposal`, the payload, or any query. For `system_managed = 0` — which,
per §3.1, is **every row in existence the moment V089 applies** — the composite condition
`proposalEnhIds.contains(...) && isSectionEnabled(...)` reduces to `proposalEnhIds.contains(...)`,
which is today's condition exactly. Not "equivalent"; identical.

**Witness-based proof.** ⚠️ Per the §0 correction, the prompt's named witnesses (POP/FSA/HRA/COBRA/
HSA) are **LOS**, not Enhancements, and therefore exercise the **untouched** LOS branch. That makes
them excellent *negative* controls but useless as *positive* ones. The walk needs both:

| Witness | Branch exercised | What must be true after the change |
|---|---|---|
| A `SCOPED` section linked to the **FSA / HRA / COBRA LOS** (`DemoDataSeeder.java:182/192/202`) | LOS loop, `:290-297` — **not edited** | Renders exactly as before. Any change here means the wrong loop was edited. |
| A `SCOPED` section linked to the **"Debit Card Services" enhancement** (`DemoDataSeeder.java:225`, `system_managed = 0`) | Enhancement loop, `:298-305` — **edited**, step 2 path | Renders exactly as before. **This is the load-bearing witness** — it is the only seeded row that proves the edited branch is transparent for unflagged rows. |
| A `SCOPED` section linked to an enhancement **manually set `system_managed = 1`** | Enhancement loop, step 3 path | Section **disappears** (stub returns `false`). Proves the flag is actually consulted. |
| A section with `scope = 'ALL'` | Neither — `continue` at `:284-286` | Unchanged. |

**What the walk looks for:** open the public `/proposal/{guid}` for a proposal carrying each witness
and compare the rendered section list against a pre-change capture of the same URL. The comparison is
section *presence and order*, not pixels. **The middle two rows are the whole test** — the first and
last are controls confirming the harness itself is sound.

---

## 3. Task 3 — `ProposalDetail` section rendering

### 3.1 Attributes to set, in order

`ProposalDetail.doGet` currently sets four attributes at `:70-73` and closes the EM at `:76`. The
new work is inserted **after the `canViewProposal` check at `:47-50` and before the `em.close()` at
`:76`** — i.e. inside the existing `try`, so the EM-open invariant (Obstacle B) holds identically to
`ViewProposal`.

Required order — this mirrors `ViewProposal.doGet` and the ordering is load-bearing per §1.2
Obstacle C:

1. `ichraEntitled` — `IchraAccessResolver.isAvailableForProposal(em, proposal)`. *Resolve once.*
2. `ichraSnapshot` / `ichraBands` — **provenance-split, see §3.3.** This is the only step whose
   condition differs from `ViewProposal`.
3. Market attributes — call a local copy of `resolveMarketPage`. ⚠️ **G6 is not relaxed** (§3.5).
4. `primaryColor` / `accentColor` — `AppConstantDAO`, same literal fallbacks (`#2B5F8A`/`#7AB648`).
5. `features` / `renderedFeatures` / `pspName` — only if the `FEATURES` section is to render.
6. Section load → scope filter → **flagged predicate (§2)** → entitlement filter → agency override
   → token substitution, producing `proposalSections` + `sectionHtml`.
7. `agencyName`.
8. `stagingPreview` (new, boolean) — see §3.4.

Attributes already set at `:70-73` (`proposal`, `pricing`, `proposalLink`, `canEditMarkup`) stay
exactly as they are. `pricing` is already loaded at `:52` and is the input to `proposalEnhIds` — no
second query needed.

### 3.2 Can `viewProposal.jsp`'s fragments be reused as-is? **No — a CSS-scope variant is required.**

This is a concrete, verified blocker, not a style preference.

`proposalIchra.jsp` and `proposalMarket.jsp` both open with `<div class="los-card">` →
`<div class="los-card-header">` → `<div class="los-card-body">` (`proposalIchra.jsp:14-18`,
`proposalMarket.jsp:23-27`). Those three classes are defined **only inside `viewProposal.jsp`'s own
`<style>` block, at `:54`, `:61`, `:68`** — verified by grepping all of `src/main/webapp/` for the
class definitions. They are **not** in `css-js.jsp`, which is what `proposalDetail.jsp` imports
(`proposalDetail.jsp:7`).

⚠️ **Worse than merely unstyled — there is a live collision.** `proposalBuilder.jsp:9` defines
`.los-card` with an unrelated meaning: `cursor: pointer; transition: all 0.2s ease; border: 2px
solid #dee2e6` — a *clickable selection card*. Two different meanings for one class name already
exist in this codebase. Adding a third consumer that inherits neither definition is how that becomes
a rendering bug someone debugs later.

**Specified approach:** add a scoped style block to `proposalDetail.jsp` that redefines
`.los-card`, `.los-card-header`, `.los-card-body` **inside a wrapper class** — e.g.
`.proposal-preview .los-card { … }` — copying the three rule bodies verbatim from
`viewProposal.jsp:54-70`, and wrap the included fragments in `<div class="proposal-preview">`. This
leaves both fragments unmodified (they remain `viewProposal.jsp`'s), avoids the
`proposalBuilder.jsp` collision by scoping, and keeps the preview visually faithful to what the
employer sees.

**Do not modify `proposalIchra.jsp` or `proposalMarket.jsp`.** They are public-page fragments with
documented compliance discipline in their header comments (`proposalIchra.jsp:3-12`,
`proposalMarket.jsp:3-21`). Editing them to suit an internal page puts a compliance-reviewed public
artifact at risk to serve a preview.

**Static-include note:** both are pulled in with `<%@ include file="…" %>` (translation-time
textual inclusion, `viewProposal.jsp:189` and `:211`), not `<jsp:include>`. A second static include
from `proposalDetail.jsp` is legal and independent — it does not disturb `viewProposal.jsp`'s
translation unit.

### 3.3 The provenance split, precisely

**Public path (`ViewProposal:142`, G5) — unchanged, permanently:**
```java
if (ichraSnapshot != null && RatingAreaRateCache.SOURCE_ENV_PRODUCTION.equals(ichraSnapshot.getSourceEnv()))
```

**Internal path (`ProposalDetail`, new):**
```java
boolean stagingPreview = isIchraDemoOverride(request);   // properties flag AND isPspAdmin
if (ichraSnapshot != null
        && (RatingAreaRateCache.SOURCE_ENV_PRODUCTION.equals(ichraSnapshot.getSourceEnv())
            || stagingPreview)) {
    // set ichraSnapshot / ichraBands
}
```

The two conditions of `isIchraDemoOverride` are exactly those at `ProposalBuilder:596-602`:
`AppConfig.isIchraDemoStagingAllowed()` (reads `ICHRA_DEMO_ALLOW_STAGING_PROPOSAL` from
`ssa.properties`, `AppConfig.java:250-252`) **AND** `session.getAttribute("isPspAdmin")` via
`getSession(false)`.

**Extract the duplicated method, or add a third copy? → Add a third copy. Deliberately.**

*Justification.* The method already exists twice by design, and `IllustrationServlet:715-719`'s
javadoc states why: "`ProposalBuilder` re-evaluates the same two conditions independently before
writing a snapshot, so enabling the button cannot by itself produce a row." **Independent
re-evaluation is the safety property.** A shared static would make all three sites fail open together
if it were ever weakened — the current shape means a mistake in one is caught by the others. Both
existing copies use `getSession(false)` specifically so a feature check never creates a session; the
third copy must do the same. The method is 6 lines with no branching beyond the two conditions;
duplication cost is near zero and the coupling cost of centralising a security predicate is not.

⚠️ **The third copy must be byte-identical to `ProposalBuilder:596-602`.** If a future run centralises
these, it centralises all three at once — not two.

### 3.4 The banner

**Exact markup to reuse — `illustration25.jsp:703-709`, verbatim:**
```jsp
<c:if test="${not empty sourceEnv and sourceEnv != 'PRODUCTION'}">
    <div class="disclaimer" style="background:#f8d7da; border-color:#f5c2c7; color:#842029;">
        <i class="bi bi-exclamation-triangle-fill me-1"></i>
        <strong>Test-environment rates.</strong> These figures came from the
        <c:out value="${sourceEnv}"/> environment, not production market data. Do not present this to a client.
    </div>
</c:if>
```

⚠️ **`.disclaimer` is defined only in `illustration25.jsp:54-57` and `groupConversion25.jsp:50`** —
verified this run. It is not global. Copy the rule body into `proposalDetail.jsp`'s style block
alongside the §3.2 additions:
```css
.disclaimer {
    background: #fff3cd; border: 1px solid #ffe69c; border-radius: 6px;
    padding: 0.75rem 1rem; margin-bottom: 0.9rem; font-size: 0.85rem; color: #664d03;
}
```
(The inline `style=` on the banner overrides the yellow base to red — copying the base rule is still
required for padding, radius, and font-size.)

**Binding:** `ProposalDetail` must set a `sourceEnv` request attribute from
`ichraSnapshot.getSourceEnv()` when a snapshot is bound, so the `<c:if>` above works unmodified. The
banner renders whenever `sourceEnv != 'PRODUCTION'` — i.e. **it is driven by the data's own stamp,
not by the override flag.** A staging snapshot always carries its banner; there is no path that
renders a staging figure without it.

### 3.5 Isolation proof — structural, and it holds

**Claim: nothing in this change can cause `ViewProposal`'s public path to render a `STAGING`
snapshot.** Four independent legs, each verified this run:

1. **Different servlets, no shared mutable state.** `ProposalDetail` (`@WebServlet("/ProposalDetail")`,
   `:32`) and `ViewProposal` (`@WebServlet("/proposal/*")`, `:48`) are separate classes. The new code
   lives entirely in `ProposalDetail` plus one `&&`-clause in `ViewProposal`'s **enhancement**
   loop (§2.2) — which touches section *membership*, never provenance.
2. **G5 is not edited.** `ViewProposal:142` is unchanged by this build. Its condition is a bare
   `SOURCE_ENV_PRODUCTION.equals(...)` with no disjunct and no override term.
3. **The override is unreachable from `ViewProposal`.** `grep` for `isIchraDemoOverride` and
   `ICHRA_DEMO_ALLOW_STAGING_PROPOSAL` in `ViewProposal.java` → **zero matches**, and this build adds
   none. The third copy of the method (§3.3) is a `private` member of `ProposalDetail`.
4. **The override cannot be satisfied on the public path even if it were reachable.** It requires
   `session.getAttribute("isPspAdmin")`, and `/proposal/*` is on `LoginFilter`'s unauthenticated
   allowlist (`:87`) — the audience is a prospect with no session. `getSession(false)` returns null,
   the method returns `false`. **This leg holds independently of legs 1-3.**

**Verdict: structurally proven. This is not a hard stop.** The proof does not rest on reviewer
discipline or on a comment — legs 2 and 4 are each independently sufficient.

### 3.6 Visibility split — constraint

Two different gates, and they must not be conflated:

- **Section content** (which sections appear) — governed by LOS/enhancement scope + entitlement,
  identical to the public path. Visible to **anyone who passes `canViewProposal`**
  (`ProposalDetail:100-135`): PSP staff tenant-wide, the prospect's own agent, or an Agency Admin via
  `AgencyScopeResolver.canSeeDetail`.
- **Staging-sourced figures** — **PSP-admin only**, gated by the two-condition override.

**Constraint:** an Agency Admin or Plain Agent who passes `canViewProposal` sees the section list and
all `PRODUCTION`-sourced content, and **never** sees a `STAGING`-sourced snapshot, regardless of the
properties flag. The `isPspAdmin` half of the override is what enforces this and must not be relaxed
to "any authenticated user."

---

## 4. Task 4 — Migration spec: `V089__enhancement_system_managed.sql`

**Modelled on `V086__los_plus_tier.sql`** (read in full this run) — same shape, same
`TINYINT(1) NOT NULL DEFAULT 0` pattern, same two-part footer.

```sql
-- V089: System-managed classification flag on an enhancement (enhancement.system_managed)
--
-- Marks an enhancement whose proposal-section visibility is decided per-proposal by
-- FlaggedEnhancementResolver rather than by scope membership alone. See
-- docs/analysis/S18C_proposal_detail_render_spec.md §2.
--
-- NOT NULL DEFAULT 0 — mirrors los.is_plus_tier (V086), agency.markup_enabled (V067),
-- agency.ichra_enabled (V077) and enhancement.suppressed itself. No backfill: every
-- existing row comes out UNFLAGGED, so nothing anywhere changes when this is applied.
-- FlaggedEnhancementResolver returns true immediately for an unflagged enhancement, so
-- the ViewProposal scope filter reduces to its current condition exactly.
--
-- Naming: system_managed rather than is_system_managed or system_managed_enabled. This
-- is a classification of what an enhancement IS (cf. V086's reasoning), but the
-- enhancement table's existing boolean is bare `suppressed`, not `is_suppressed` — this
-- column matches its own table's established form rather than another table's.
--
-- Why a column on enhancement rather than a constant row holding enhancement ids: the
-- constant table has PRIMARY KEY (name) and NO psp_id, so it is global per installation,
-- whereas enhancement.psp_id is per-PSP. One constant cannot name the correct rows for
-- every PSP on a multi-PSP installation. Same reasoning recorded in V086 lines 24-28.
--
-- Seeds nothing. No INSERT INTO constant. No index: neither enhancement.suppressed nor
-- los.is_plus_tier carries one, and this column is read per already-loaded entity, never
-- queried on.
--
-- Prerequisites: none.

-- ----------------------------------------------------------------------
-- 1. Add system_managed to enhancement
-- ----------------------------------------------------------------------
ALTER TABLE enhancement
    ADD COLUMN system_managed TINYINT(1) NOT NULL DEFAULT 0;

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V089' AS version, '<DATE OF APPLICATION>' AS updated;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V089', 'System-managed classification flag on enhancement (enhancement.system_managed, default OFF)', 'V089__enhancement_system_managed.sql', NOW());
```

**Default guarantee (stated explicitly, as required):** `NOT NULL DEFAULT 0` means **every existing
`enhancement` row is unflagged on apply, and no current behaviour changes.** Combined with
`FlaggedEnhancementResolver` step 2 (§2.2), applying V089 alone is a no-op at the render layer.

**Entity change accompanying the migration:** add to
`model/sales/offering/Enhancement.java` —
```java
@Column(name = "system_managed", nullable = false)
private boolean systemManaged;
```
plus `isSystemManaged()` / `setSystemManaged(boolean)`, matching the existing `suppressed` field's
shape at `Enhancement.java:26-27` / `:60-61`.

**No `INSERT INTO constant`.** None is implied by this migration. If the later payload build needs
one, it is recorded as a **D-NN deployment note**, never scripted here — matching
`V086__los_plus_tier.sql:30-31`'s stated rule.

**Registration (both required, per CLAUDE.md's migration discipline):**
1. `docs/analysis/migration_tracker.md` — add the V089 row, and flip the per-environment status
   **only once that environment has actually received it**.
2. `docs/schema_version_migration.sql` — add the corresponding entry.

Both files verified to exist this run.

---

## 5. Task 5 — Risk list and build order

### 5.1 Every way this build could break an existing line of service

| # | Risk | Anchor that would cause it | Severity |
|---|---|---|---|
| 1 | Predicate appended to the **LOS** loop instead of the enhancement loop → every LOS-scoped section (FSA, HRA, COBRA — the live ones) evaluated against a resolver that knows nothing about LOS | `ViewProposal.java:290-297` (must stay untouched) vs `:298-305` (the target) | **Critical** — breaks every LOS |
| 2 | Resolver's unflagged short-circuit placed after any other logic → an exception in that logic makes unflagged enhancements fail closed and vanish | `FlaggedEnhancementResolver` step 2 ordering (§2.2) | **Critical** |
| 3 | Predicate placed *before* `proposalEnhIds.contains(...)` instead of `&&`-appended after → resolver consulted for enhancements that do not price on this proposal | `ViewProposal.java:299` | High |
| 4 | V089 written `NOT NULL` without `DEFAULT 0` → apply fails on a non-empty table, or rows land ambiguous | V089 §4 DDL | High |
| 5 | Duplicated pipeline in `ProposalDetail` resolves anything after `em.close()` → lazy-load exception inside the JSP | `ProposalDetail.java:76` — all new work must precede it | High |
| 6 | `.los-card` copied into `proposalDetail.jsp` **unscoped** → collides with `proposalBuilder.jsp:9`'s clickable-card meaning if the two ever share a page or a future shared stylesheet | §3.2 — wrapper class is mandatory | Medium |
| 7 | Entitlement filter omitted from the `ProposalDetail` copy → internal page shows ICHRA sections for an unentitled agency | `ViewProposal.java:334-338` must be mirrored | Medium (internal audience, but a compliance-shaped gap) |
| 8 | Third `isIchraDemoOverride` copy drifts from `ProposalBuilder:596-602` (e.g. `getSession(true)`) → creates sessions on a feature check | §3.3 | Medium |
| 9 | `sourceEnv` attribute bound but banner markup omitted → a staging figure renders unlabelled | §3.4 | **Critical if reached** — this is the one that could put a test figure in front of a client |

### 5.2 Build order — smallest reversible step first

| Step | Work | Independently shippable? | Reversal |
|---|---|---|---|
| **1** | V089 migration + `Enhancement.systemManaged` field/getters. Nothing reads it. | **Yes** — pure no-op, exactly V086's precedent | `DROP COLUMN` + revert field |
| **2** | `FlaggedEnhancementResolver` with steps 1/2/4 real, step 3 stubbed `return false`. Nothing calls it. | **Yes** — dead code, zero render impact | Delete the class |
| **3** | The one-line `&&` in `ViewProposal.java:299`. **This is the only public-path edit in the build.** | **Yes**, and it should ship alone so the §2.4 walk attributes any regression to exactly one line | Revert one line |
| **4** | Admin UI toggle for `system_managed` (Service Manager, alongside the existing `suppressed` toggle at `ServiceManagerAction.java:215`) | Yes | Revert |
| **5** | `ProposalDetail` section rendering — duplicated pipeline, PRODUCTION-only, no staging path yet | Yes | Revert servlet + JSP |
| **6** | Staging preview: third `isIchraDemoOverride` copy, `sourceEnv` binding, banner | Yes | Revert |

**Ship steps 1-3 as one release and stop.** Walk the §2.4 witnesses before starting step 5. Steps 5-6
touch no file that renders a public proposal and can proceed on their own cadence afterwards.

### 5.3 What cannot be verified without a runtime walk

1. **That the §2.4 witnesses render identically before and after step 3.** The bit-identity argument
   is structural and strong, but "structurally identical" and "renders identically" are different
   claims, and only the second one protects a customer. **Walk:** capture the public
   `/proposal/{guid}` section list for a proposal carrying an FSA/HRA/COBRA LOS-scoped section and
   one carrying the Debit Card enhancement-scoped section, before and after step 3; diff section
   presence and order.
2. **That a flagged enhancement actually disappears.** Requires setting `system_managed = 1` on a row
   and re-walking. Cannot be checked from code.
3. **That `ProposalDetail`'s duplicated pipeline produces the same section list as `ViewProposal`
   for the same proposal.** **Walk:** open `/ProposalDetail?id=N` and `/proposal/{guid}` for the same
   proposal side by side and compare section lists. This is the primary acceptance test for step 5
   and there is no static substitute for it.
4. **That the fragments render correctly under the scoped CSS (§3.2).** Visual, unavoidably.
5. **That the staging banner appears on every staging-sourced figure.** Requires an installation with
   `ICHRA_DEMO_ALLOW_STAGING_PROPOSAL=true`, a PSP-admin session, and a staging-sourced snapshot.
   ⚠️ Per prior sessions, the AGE_BAND write path has **never executed on any installation** — a
   staging AGE_BAND snapshot may have to be created before this can be walked at all.

⚠️ **T111 note:** the absence of a local PSP Admin test credential gates *local automated* iteration
of items 2, 3, and 5. It has never blocked a production walk, and multiple prior items were
runtime-verified against production while it sat open. Do not treat it as a ceiling on verification.

---

## Compliance statement

**1. Preflight output, and hard-stop status**
```
$ git rev-parse --abbrev-ref HEAD
refactor/modernize-architecture

$ git log -1 --oneline
36efe0e docs: S17-B -- session 17 close-out, T160/T161/T162 filed, T158 reframed

$ git status --short
(empty)

$ git pull --ff-only
Already up to date.
```
**Hard stop: did not fire.** Branch correct; pull reported "Already up to date."; tree clean at
preflight.

**2. Complete list of files read this run**
- `src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java` — `:100-420` in full as required, plus `:405-644` and `:644-693`
- `src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalDetail.java` — full (263 lines)
- `src/main/webapp/WEB-INF/view/sales/proposalDetail.jsp` — full (294 lines)
- `src/main/webapp/WEB-INF/view/sales/viewProposal.jsp` — full (277 lines)
- `src/main/webapp/WEB-INF/view/sales/proposalIchra.jsp` — full
- `src/main/webapp/WEB-INF/view/sales/proposalMarket.jsp` — full
- `src/main/webapp/WEB-INF/view/market/illustration25.jsp` — `:54-65`, `:700-714`, plus grep sweep for staging/banner markup
- `src/main/java/net/superiorstate/ams/model/sales/offering/Enhancement.java` (read S18-B this session; field list re-cited, not re-read)
- `docs/migrations/V086__los_plus_tier.sql` — full (the named convention model)
- `src/main/java/net/superiorstate/ams/LoginFilter.java` — `:80-95`, plus `grep -c "ProposalDetail"` → 0
- `src/main/java/net/superiorstate/ams/data/resolver/IchraAccessResolver.java` — signatures only
- `src/main/java/net/superiorstate/ams/data/dao/ProposalIchraSnapshotDAO.java`, `ProposalIchraIntakeDAO.java` — signatures only
- `src/main/java/net/superiorstate/ams/data/service/DatabaseInitializer.java`, `DemoDataSeeder.java` — LOS/Enhancement seed sites only (the §0 correction)
- Directory listings: `src/main/java/net/superiorstate/ams/data/resolver/`, and existence checks on `docs/analysis/migration_tracker.md`, `docs/schema_version_migration.sql`
- Greps: `.los-card`/`.disclaimer` definition sweep across all of `src/main/webapp/`; anchor-uniqueness `grep -Fc` on six `ViewProposal.java` anchors

**3. Paths written — exactly one:** `docs/analysis/S18C_proposal_detail_render_spec.md` (this file,
new). **No `.java`, `.jsp`, `.sql`, or `.properties` file was created or edited this run.** The V089
DDL and every Java signature in this document are specification text inside a Markdown file, not
source.

**4. `git status --short` at close, verbatim**
```
?? docs/analysis/S18C_proposal_detail_render_spec.md
```
One new untracked file, nothing else changed. **The tree is left dirty deliberately — Kevin reviews
and commits.**

**5. Git mutation confirmation.** No `add`, `commit`, `stash`, `checkout`, `restore`, `reset`,
`rebase`, `tag`, `branch`, `merge`, or push ran this run. The only git commands executed were the
four read-only preflight commands.

**6. SQL audit.** Exactly one SQL artifact was **produced as specification text**: the
`V089__enhancement_system_managed.sql` body in §4, comprising one `ALTER TABLE … ADD COLUMN`, one
`CREATE OR REPLACE VIEW schema_info`, and one `INSERT IGNORE INTO schema_version`. **None of it was
run. No `.sql` file was created. The database was not queried.** No `INSERT INTO constant` was
produced or recommended. `docs/migrations/` is **unchanged** — the directory was read only, and the
`git status` above shows no modification under it. **Current highest migration version: V088**
(`V088__proposal_ichra_intake_contribution.sql`). V089 does not exist on disk and must be created by
the later Sonnet run.

**7. Code-verified-only disclosure.** Nothing in this document is runtime-verified. **No page was
rendered, no server was started, no build was run, no database was queried.**

| Section | Basis |
|---|---|
| §0 background verification | **Source-read** (`ViewProposal`, `ProposalDetail`, `LoginFilter`, `Enhancement`, `DatabaseInitializer`, `DemoDataSeeder`) + filesystem listing for the migration version |
| §1 pipeline inputs, obstacles, recommendation | **Source-read.** The recommendation itself is an engineering judgement over source-read facts, not a verified property |
| §2 predicate, anchors, `grep -Fc` uniqueness | **Source-read.** Anchor uniqueness is a mechanical grep result, verified this run |
| §2.4 regression witnesses | **Source-read** of seed code. ⚠️ Which rows exist on any *live* installation is **not** verified — the seeders show what is created at init, not what a production database currently holds |
| §3 attributes, CSS blocker, provenance split, banner | **Source-read** (servlet, four JSPs, `AppConfig`) |
| §3.5 isolation proof | **Source-read**, four independent structural legs. Not runtime-confirmed, and §5.3 item 1 exists precisely because structural proof is not a render guarantee |
| §4 migration spec | **Migration-read** (`V086` as the convention model) + source-read for the entity field shape. The DDL is authored here, never executed |
| §5 risks, build order, walk items | **Source-read**, with the walk items explicitly named as the things source-reading *cannot* settle |

**8. Unanswered**
- `UNANSWERED: Do any proposal_section rows with scope='SCOPED' and a non-empty enhancementList actually exist on Production, Demo, Master or BPO? — §2.4's witness table assumes the Debit Card enhancement is reachable as a scoped section; only a database read of proposal_section + proposalsectionenhancement would confirm a witness exists to walk. If none does, one must be created before step 3 can be regression-tested at all.`
- `UNANSWERED: Does any live installation currently render an ICHRA_ILLUSTRATION or MARKET section on a real proposal? — carried forward unresolved from S17-A §3.1. It bounds how much of §5.3 item 3's side-by-side comparison is actually exercisable today; a database read would settle it.`
- `UNANSWERED: Is there an existing admin UI surface for toggling enhancement-level booleans that step 4 should extend rather than add to? — ServiceManagerAction.java:215 toggles enhancement.suppressed, but whether its JSP has room for a second per-enhancement toggle was not read this run; reading the Service Manager JSP would settle it.`
