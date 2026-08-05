# Phase A — T150: the step-6 demo override (global constant, shape B)

**Run:** S16-C, 2026-08-05 · **Branch:** `refactor/modernize-architecture` · **HEAD read this run:**
`540a867db68a873b56745ad124bd63c732ab7b3b` · **Analysis only — no source file changed.**

---

## 1. Verdict

**Shape B is safe to build with one named modification: the override must be scoped to the
illustration page and the snapshot *write* path, and must not touch any of the three `ViewProposal`
gates.** The reason is not a preference — it is that `/proposal/*` is unauthenticated
(`LoginFilter:87`) and the ICHRA gate on that path was *deliberately* built session-free under LA-17,
with a code comment forbidding exactly what shape B proposes: *"a PSP admin who happens to be logged
in must not cause market data to render in a document sent to a prospect"* (`ViewProposal:134-136`).
The PSP-admin half of shape B therefore **cannot be evaluated** on that path, and adding it would
reverse a settled LA-17 enforcement decision. Scoped to the illustration and write paths, shape B
works, needs no session gymnastics, and is permanently self-limiting — a snapshot written under
override carries `source_env='STAGING'` and `ViewProposal:142` refuses it forever after, including
long after the demo. **Two consequences Kevin must accept or overrule are in §5.**

**Session 15's four-gate inventory is wrong in three ways** and should not be used as the build
input: it missed three gates entirely, it missed that a fourth is broader-than-staging, and one
recorded gate (`ViewProposal:467`) is not a `sourceEnv` read at all.

---

## 2. Gate inventory — complete, every line read this run

| # | Location | Line | Class | Notes |
|---|---|---|---|---|
| **G1** | `illustration25.jsp` | **992** | staging-specific | AGE_BAND hand-off button. S15 recorded this one correctly. |
| **G2** | `illustration25.jsp` | **1404** | staging-specific | RANGE hand-off button. ⚠️ **S15 missed this.** Identical `<c:when test="${sourceEnv == 'PRODUCTION'}">`. |
| **G3** | `ProposalBuilder.attachRangeSnapshot` | **598** | staging-specific | ⚠️ **S15 missed this.** `if (!…SOURCE_ENV_PRODUCTION.equals(sourceEnv)) return;` — refuses to *write* the snapshot. |
| **G4** | `ProposalBuilder.attachAgeBandSnapshot` | **658** | staging-specific | ⚠️ **S15 missed this.** Same, per-row. |
| **G5** | `ViewProposal` (snapshot render) | **142** | staging-specific | Reads the *snapshot's own* stamped `sourceEnv`, not the live cache. Public path. |
| **G6** | `ViewProposal.resolveMarketPage` | **466-467** | ⚠️ **broader** | Not a `sourceEnv` read. Delegates to `RateCacheDAO.check(...) != PRODUCTION_OK`, which collapses `NONE_CACHED` and `STAGING_ONLY` into one refusal. **This is what S15 recorded as ":467 refuses staging data."** Public path. |
| **G7** | `ViewProposal.putIchraMarketTokens` | **796** | staging-specific | Merge-token figures. Public path. |
| **G8** | `RateCacheDAO.check` | **95** | ⚠️ **broader + shared** | The `STAGING_ONLY` determination itself. Called by `IchraZipLookup:150`, `IllustrationServlet:657`, and G6. **Loosening this would silently change the agent advisory and the county dropdown too.** |

**Display-only banners (not gates — no behaviour depends on them):** `illustration25.jsp:703, 1250,
964-966, 1384-1386`; `groupConversion25.jsp:104, 338, 455-456`; `GroupConversionServlet:486, 505`;
`rateCacheAdmin25.jsp:106, 337`. These render *"Do not present this to a client"* and source labels.
**The override must not suppress any of them** — see §5.

**Q2 hard-stop did not fire.** No gate sits on a path shared with a non-ICHRA line of service. G8 is
shared across three surfaces, but all three are ICHRA-scoped (`IchraZipLookup`, `IllustrationServlet`,
`ViewProposal`'s plus-tier-gated market page). It is nonetheless **out of bounds for this build** —
see §4.

---

## 3. Q3 — What happens to a non-ICHRA proposal today

**Answer: `sourceEnv` is never evaluated on a non-ICHRA proposal at any gate, because every gate sits
behind an earlier ICHRA-specific guard that returns first.** The override cannot change that answer
because it does not touch those guards.

| Gate | Earlier guard that returns first | Value of `sourceEnv` on a normal proposal |
|---|---|---|
| G5 `:142` | `ichraSnapshot != null` — and the snapshot is `null` unless `isAvailableForProposal` passed **and** a `proposal_ichra_snapshot` row exists (`:139-141`) | **Never read.** No row exists for a non-ICHRA proposal. |
| G6 `:466` | `!ichraEntitled → return` (`:444`), then `!anyPlusTier → return` (`:457`), then `intake == null → return` (`:462`) | **Never read.** |
| G7 `:796` | `ichraEntitled && intake != null && countyFips != null && planYear != null` (`:780-781`) | **Never read.** |
| G1/G2 | On `/Illustration`, which `IllustrationServlet.isAuthorized` gates on `IchraAccessResolver.isAvailable` | **Unreachable.** A non-ICHRA proposal never renders this page. |
| G3/G4 | `attachIchraSnapshotIfPresent` returns unless `countyFips` **and** `mode` are both request parameters — sent only by the item-7 hand-off link | **Never read.** |

`ViewProposal`'s own Javadoc states the property independently: *"Byte-identical for every proposal
with no MARKET section row, which today is all of them"* (`:437-438`).

---

## 4. Q4 — The unauthenticated employer-facing URL ⚠️

**This is the finding that shapes the build.**

**Is the proposal reachable without login? Yes.** `LoginFilter:87`:

```java
boolean allowedPath = ALLOWED_ENDPOINTS.contains(path) || path.startsWith("/proposal/") || …
```

`ViewProposal` is `@WebServlet("/proposal/*")`, keyed on `applicationGUID`. Anyone with the link — the
employer, anyone they forward it to — renders the page with no session.

**Does the override's effect follow it there? Under shape B as written, yes — and worse, the
PSP-admin half would silently evaporate.** There is no session on that path, so
`session.getAttribute("isPspAdmin")` is meaningless. Shape B's two conditions would degrade to one:
a single global flag that, when ON, puts staging figures on every entitled agency's public proposal
links. That is not the shape Kevin chose.

**Does LA-17 bear on this? Directly, and it is already enforced in code.** `ViewProposal:131-137`:

```java
// LA-17 / T116 — resolved ONCE per request … Deliberately NOT the
// session-based isAvailable(): this page is public and unauthenticated, and a PSP admin who
// happens to be logged in must not cause market data to render in a document sent to a
// prospect.
boolean ichraEntitled = IchraAccessResolver.isAvailableForProposal(em, proposal);
```

`isAvailableForProposal` (`IchraAccessResolver:145-166`) takes no `HttpServletRequest`, reads no
session, and resolves entitlement from the proposal's own originating agency. LA-17's enforcement
section (`legal_assumptions.md:1041-1057`) makes the availability resolver *"the enforcement point for
a legal boundary, not only a visibility preference,"* and constraint 1 (`:1026-1029`) rests the whole
entry on every send being an agent action. **A PSP-admin session influencing a prospect-facing render
is precisely the coupling LA-17's enforcement was built to prevent.**

**Consequence for the build:** the override stops at the write path. G5/G6/G7 stay exactly as they
are. This is not merely safe — it is *self-limiting*: G5 reads the snapshot's own stamped
`source_env`, so a demo-created snapshot is permanently identifiable and permanently refused on the
public page, with no cleanup step and no risk of a stale demo artifact leaking later.

---

## 5. Decisions surfaced for Kevin

**D1 — What the demo actually shows. This is a real limitation, not a technicality.**
With the override scoped to G1–G4, the demo gets: the button enabled, the proposal created, and a
snapshot row written stamped `STAGING`. **The proposal itself renders with no ICHRA section**, because
G5 refuses the staging-stamped snapshot. So step 6 demonstrates *the hand-off works*, not *the
finished document*. If the demo needs the rendered proposal, that requires loosening G5 — a public
unauthenticated path — and then a **visible demo-data marker on the artifact becomes mandatory, not
optional**, along with a decision on whether LA-17 constraint 1 survives it. **Session 15 left the
marker question to you; this Phase A does not decide it.** My reading: take the limitation, keep G5
closed.

**D2 — Constant name.** Spec below uses `ICHRA_DEMO_ALLOW_STAGING_PROPOSAL`. It names the surface,
the nature, the effect and the scope, and it is impossible to mistake for a production toggle.
Shorter alternative if you prefer: `ICHRA_DEMO_STAGING_HANDOFF`.

**D3 — `ssa.properties` rather than a DB constant.** Spec below reads the flag from `ssa.properties`
via `AppConfig`, not from the `constant` table. Rationale: a DB constant travels with a database
restore — a demo flag set on one installation could arrive on another via a dump. A properties entry
is per-installation by construction and cannot be replicated accidentally. It also needs no
`AmsDataGlobal` change. **This departs from `RATE_CACHE_WARM_ENABLED`'s DB-constant precedent
deliberately**; say so if you want the DB shape instead.

**D4 — The banners stay.** The override enables the *button*; it must not suppress
`illustration25.jsp:703/1250`'s *"Do not present this to a client"* warnings. The agent should see
both at once during the demo. Confirm you agree.

---

## 6. Build spec

### 6.1 `AppConfig.java` — append only

Append after the HealthSherpa Base URL block (`~:226`). **Change nothing existing.** No new cached
field, no `AmsDataGlobal` change — this reads `ssa.properties` directly through the existing `get()`.

```java
// --- ICHRA demo override (ssa.properties only, no default — fails closed) ---

/**
 * True only when ICHRA_DEMO_ALLOW_STAGING_PROPOSAL is explicitly "true" in
 * ssa.properties. Absent, blank, or any other value means OFF — mirroring
 * HEALTHSHERPA_BASE_URL's fail-closed contract: absence is a decision, never a default.
 * Deliberately NOT a DB constant: a demo flag must not travel with a database restore.
 */
public static boolean isIchraDemoStagingAllowed() {
    return "true".equalsIgnoreCase(get("ICHRA_DEMO_ALLOW_STAGING_PROPOSAL"));
}
```

**Must not change:** any existing getter, any cached field, `AmsDataGlobal`, `EmfListener`.

### 6.2 `IllustrationServlet.java` — surface the flag plus the role

G1/G2 are JSP `<c:when>` tests with no `EntityManager` and no direct role access (Q6: **there is no
`isPspAdmin` anywhere in `illustration25.jsp`, `IllustrationServlet`, or `ViewProposal` — zero
grep hits**). The servlet must resolve both halves and set one attribute. Set it on **every** path
that forwards to `illustration25.jsp`, next to the existing `sourceEnv` attribute plumbing
(`setProvenanceAttributes`, `~:829-851`):

```java
request.setAttribute("ichraDemoOverride",
        AppConfig.isIchraDemoStagingAllowed()
                && Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin")));
```

The `isPspAdmin` session attribute is the established mechanism (`RateCacheAdmin.isAuthorized`,
`IchraAccessResolver.isAvailable` both read it this way). **Both conditions required; `&&`, never
`||`.**

**Must not change:** `isAuthorized()`, the `IchraAccessResolver` call, `setProvenanceAttributes`'s
existing `sourceEnv`/`fetchedAt` attributes, or the county-dropdown logic at `:657`.

### 6.3 `illustration25.jsp` — G1 (`:992`) and G2 (`:1404`)

Both change identically, and **only** the `<c:when>` test:

```jsp
<c:when test="${sourceEnv == 'PRODUCTION' or ichraDemoOverride}">
```

**Must not change:** the `<c:otherwise>` disabled-button branch, the `quiet-note` text, the
`<c:url>` parameter blocks, and — critically — the staging banners at `:703` and `:1250`.

### 6.4 `ProposalBuilder.java` — G3 (`:598`) and G4 (`:658`)

The write path has no session-independent problem (it is `doPost` on an authenticated servlet), but
it does need the same two conditions. Resolve once in `attachIchraSnapshotIfPresent` and pass down,
or re-evaluate at each site:

```java
boolean demoOverride = AppConfig.isIchraDemoStagingAllowed()
        && Boolean.TRUE.equals(request.getSession().getAttribute("isPspAdmin"));
```

G3 (`:598`): `if (!demoOverride && !RatingAreaRateCache.SOURCE_ENV_PRODUCTION.equals(sourceEnv)) return;`
G4 (`:658`): same guard on the per-row check.

⚠️ **`snapshot.setSourceEnv(sourceEnv)` at `:620` and `:693` must keep stamping the *actual* value
(`STAGING`).** Do not stamp `PRODUCTION` under override. The honest stamp is what makes G5's refusal
permanent and the artifact self-identifying.

**Must not change:** the `countyFips`/`mode` presence guard, the tobacco filter, the arithmetic, the
`try/catch` that keeps a snapshot failure from breaking proposal creation.

### 6.5 Explicitly out of scope — do not touch

`ViewProposal:142`, `:466-467`, `:796`; `RateCacheDAO.check` (`:81-106`) and its enum;
`IchraAccessResolver.isAvailableForProposal`; `IchraZipLookup`; `GroupConversionServlet`; every
banner listed in §2. **No migration. No `INSERT INTO constant`. No `DatabaseInitializer` entry.**

---

## 7. What this Phase A could not establish

1. **Whether the demo is acceptable without the rendered proposal section (D1).** That is a product
   judgment, not a code fact.
2. **What `ssa.properties` currently contains on the demo installation.** Not readable from this
   workstation; the file is at `-Dssa.config=C:\ssa\ssa.properties` locally and
   `/var/lib/tomcat10/conf/ssa.properties` on the servers. The build does not depend on it, but the
   *demo* does — someone must add the line where the demo runs.
3. **Whether any non-`sourceEnv` gate refuses staging by a different mechanism.** I grepped
   `sourceEnv`/`source_env`/`SOURCE_ENV` exhaustively across `src/`, and followed `RateCacheDAO.check`
   to its three callers. A gate keyed on something else entirely (a date, a county allow-list) would
   not appear in that search.
4. **Runtime behaviour of any of this.** Nothing was executed. Every finding is static reading.

---

## 8. SQL close-out audit

**No SQL of any kind is produced or recommended by this Phase A or by the build it specifies.** No
migration, no DDL, no DML, no `INSERT INTO constant`, no `DatabaseInitializer` seed — the constant is
an `ssa.properties` entry set by hand per installation, and its absence is the OFF state.

**Highest migration read from `docs/migrations/` this run: `V088__proposal_ichra_intake_contribution.sql`.**
Unchanged by this run.

---

## 9. Compliance statement

- **Paths written this run: exactly one** — `docs/analysis/phase_a_t150_demo_override.md` (this file).
- **No source file was modified.** No `.java`, `.jsp`, `.sql`, no `docs/migrations/`, no
  `ichra_strategy.md`, no `project_backlog.md`, no `swbd_ichra_build_plan.md`.
- **Hard stops fired: none.** Preflight passed on all four conditions — branch
  `refactor/modernize-architecture`, clean tree, `git pull --ff-only` returned *"Already up to date."*
  The Q2 hard-stop (a gate shared with a non-ICHRA line of service) was evaluated and **did not
  fire**.
- **Every hash was read from `git log` this run.** HEAD: `540a867db68a873b56745ad124bd63c732ab7b3b`.
- **No forbidden git operation ran.** No `add -A`/`add .`/`add -u`, no `stash`, `checkout`, `restore`,
  `reset`, `rebase`, `tag`, `branch`, no force-push.
