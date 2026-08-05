# S17-A — Proposal render source, AGE_BAND support, and the internal view

**Run:** S17-A, 2026-08-05 · **Branch:** `refactor/modernize-architecture` · **HEAD read this run:**
`9c9a5ced9e425ae93251023fe970b3e061fd9632` · **Read-only. No source file changed.**

Answers only. No design, no spec, no recommendation — where a finding implies work, it is described
and left there.

---

## Q1 — Snapshot or live? **Both, but never in the same section.**

**There are two distinct ICHRA section types with two distinct data sources and two distinct gates.**
The premise that one section might be "a mixture" does not hold; they are separate rows in
`proposal_section` rendering separate fragments.

| Section type | Fragment | Reads from | Gate |
|---|---|---|---|
| **`ICHRA_ILLUSTRATION`** | `proposalIchra.jsp` | **The stored snapshot, exclusively** | **G5** — `ViewProposal:142`, the snapshot's own stamped `sourceEnv` |
| **`MARKET`** | `proposalMarket.jsp` | **Live computation** from the intake + rate cache | **G6** — `ViewProposal:466`, `RateCacheDAO.check() == PRODUCTION_OK` |

**`ICHRA_ILLUSTRATION` is snapshot-only.** `viewProposal.jsp:186-192` dispatches on the type and
includes `proposalIchra.jsp` inside `<c:if test="${not empty ichraSnapshot}">`. Every EL expression in
that fragment resolves against `${ichraSnapshot}` or `${ichraBands}` — `groupMonthlyLow`/`High`
(`:64-65`), `groupNetTotal` (`:53`), `contribution` (`:57`), `headcount` (`:71`), `countyName`/`state`
(`:77`), `ratesFetchedAtDisplay` (`:80`). The attributes are set only at `ViewProposal:143-146`, from
`ProposalIchraSnapshotDAO.findByProposalId`. **No live rate lookup occurs on this path.**

**`MARKET` is live-only.** `resolveMarketPage` (`ViewProposal:440-514`) reads the **intake**
(`ProposalIchraIntakeDAO.findByProposalId`, `:461`), calls `RateCacheDAO.getRatesForCounty` (`:475`),
and computes `marketFloor21/40/64`, `marketPlanCount`, `marketCarrierCount` from the cache rows
(`:497-501`). Confirmed from the other side: every EL expression in `proposalMarket.jsp` is a
`market*` attribute (plus `${pricing}`) — **it contains no reference to `ichraSnapshot` at all.**

**The merge tokens (G7) are a third, also-live path.** `putIchraMarketTokens`
(`ViewProposal:773-847`) reads the intake and the cache directly, emitting `{{ICHRA_*}}` tokens
(`:710-713`, `:727-730`) substituted into `TITLE`/`CLOSING`/`CUSTOM` HTML. It also never reads the
snapshot.

**So the snapshot has exactly one reader in the whole render path: the `ICHRA_ILLUSTRATION` section.**

---

## Q2 — **The illustration hand-off is the only writer.** Confirmed.

Every reference to the snapshot entity or its DAO across `src/main/java/`:

- `ProposalBuilder:406` — `attachIchraSnapshotIfPresent(...)`, the sole call, inside `createProposal`
- `ProposalBuilder:558` — the method, which returns immediately unless **both** `countyFips` and
  `mode` are present as request parameters
- `ProposalBuilder:572 / :574` — dispatch to `attachAgeBandSnapshot` / `attachRangeSnapshot`
- `ProposalBuilder:655 / :737` — the two `ProposalIchraSnapshotDAO.save(...)` calls
- `ViewProposal:140 / :145` — reads only

**No other code path persists a `ProposalIchraSnapshot`.** Those two parameters are supplied only by
the illustration hand-off link (`illustration25.jsp`'s two `<c:url>` blocks) and echoed forward as
hidden inputs by `proposalBuilder.jsp:62-78`.

**What proposal-builder-first produces instead:** a **`ProposalIchraIntake`** row, written by a
completely separate method, `attachIchraIntakeIfPresent` (`ProposalBuilder:452`), gated on
`IchraAccessResolver.isAvailable` + a plus-tier LOS + an `intakeZip` parameter (`:453-470`). That row
feeds the `MARKET` section and the merge tokens — **not** the `ICHRA_ILLUSTRATION` section.

---

## Q3 — AGE_BAND: bands persist, but the two modes store disjoint field sets

**1. Band rows have a child table.** `proposal_ichra_snapshot_band`, created in
`docs/migrations/V079__proposal_ichra_snapshot.sql`, mapped by `ProposalIchraSnapshotBand`. Its
columns are `age`, `lives`, `floorPremium`, `netPerEmployee`, `bandNet`, `sortOrder`. AGE_BAND
therefore stores **per-band detail, not only aggregates**.

**2. Field population is close to disjoint** — read from the two writers:

| Field | RANGE (`:640-653`) | AGE_BAND (`:722-735`) |
|---|---|---|
| `mode`, `countyFips`, `state`, `countyName`, `planYear` | ✅ | ✅ |
| `sourceEnv`, `ratesFetchedAt`, `snapshotAt`, `createdBy` | ✅ | ✅ |
| **`headcount`** | ✅ `:647` | ❌ **never set** |
| **`groupMonthlyLow` / `groupMonthlyHigh`** | ✅ `:648-649` | ❌ |
| **`contribution`** | ❌ | ✅ `:729` |
| **`groupNetTotal`** | ❌ | ✅ `:730` |
| **`employerOutlay`** | ❌ | ✅ `:731` |
| **band rows** | ❌ — `save(em, snapshot, null)` `:655` | ✅ `save(em, snapshot, bands)` `:737` |

⚠️ **`headcount` is never written on the AGE_BAND path, although the figure exists.** `totalLives` is
accumulated at `:713` and used at `:720` to compute `employerOutlay`, then discarded. An AGE_BAND
snapshot has a null `headcount` despite the writer knowing the total. Total lives remains derivable
by summing `lives` across band rows, so nothing is unrecoverable — but the column is empty where
RANGE's is populated. **Described here; no backlog row filed (see §4).**

**3. The hand-off does carry the census.** `illustration25.jsp`'s AGE_BAND `<c:url>` block emits
`mode=AGE_BAND`, `countyFips`, `planYear`, `contribution`, and `age{i}`/`count{i}` for every non-blank
row; `proposalBuilder.jsp:62-78` echoes all sixteen as hidden inputs; `attachAgeBandSnapshot:665-673`
reads `age1..6`/`count1..6` back. **The chain is complete and symmetric.**

**4. AGE_BAND has one gate RANGE does not:** `attachAgeBandSnapshot:661-662` returns unless
`contribution` parses to a non-negative decimal. **A missing or negative contribution silently
produces no snapshot at all**, with no equivalent on the RANGE path. Beyond that, both share the same
provenance guard and the same T150 override; G1 and G2 are otherwise identical in structure.

---

## Q4 — ⚠️ The internal view exists, is authenticated, and **renders no sections whatsoever**

**1. It is `ProposalDetail`.** `@WebServlet(name = "ProposalDetail", value = "/ProposalDetail")`
(`ProposalDetail:32`), forwarding to `/WEB-INF/view/sales/proposalDetail.jsp` (`:79`).
`ProposalBuilder:347` redirects there after creation: `sendRedirect("ProposalDetail?id=" + ...)`.

**2. It renders no sections.** `doGet` sets exactly four request attributes — `proposal`, `pricing`,
`proposalLink`, `canEditMarkup` (`:70-73`). A grep of the entire servlet for
`Section|ichraSnapshot|ichraBands|market|sourceEnv|Snapshot|Intake` returns **zero matches**.
`proposalDetail.jsp` includes no section fragment — its only imports are `css-js.jsp` and
`navbar25.jsp`. **There is no section pipeline on this page: it is header, lines of service, the
public proposal link, and the pricing summary.**

**3. It has no `sourceEnv` gate, and does not share G5** — because it renders nothing that would need
one. Provenance never enters this servlet.

**4. It is session-gated, by two independent mechanisms.** `/ProposalDetail` is **absent** from
`LoginFilter:87`'s unauthenticated allowlist (which admits `/proposal/`, `/apply/`, `/q/`, `/tpo`,
`/video`, `/outlook/` and a fixed endpoint set), so an anonymous request is redirected to login.
Beyond that, `canViewProposal` (`ProposalDetail:47`, defined `:84+`) applies a scope check: PSP staff
tenant-wide, otherwise the prospect's own agent, otherwise Agency Admin via
`AgencyScopeResolver.canSeeDetail`.

⭐ **The consequence for the option Q4 was asked to test: it does not exist.** The hoped-for move —
open only the internal view's section gate, leave `/proposal/*` untouched — has nothing to open.
`ProposalDetail` does not render sections at all, so there is no separate gate there; making the
internal page show ICHRA content would mean **building section rendering into it**, not relaxing a
condition. **Reported, not designed.**

---

## Q5 — What each tier makes computable

Grounded in what the two writers actually store (Q3), not on what would be arithmetically possible.

**Cost comparison (current group cost vs ICHRA).** From **RANGE**: the ICHRA side is available as a
group premium *range* only — `groupMonthlyLow`/`High`, being age-21 and age-64 floors × headcount
(`:637-638`). It is a spread across a hypothetical age mix, **not** this group's cost, because RANGE
stores no ages. From **AGE_BAND**: a real per-group figure — `groupNetTotal` plus per-band
`floorPremium`/`netPerEmployee`/`bandNet`. **Anything comparing a specific employer's actual cost
needs AGE_BAND.** RANGE supports a market-range framing only.

**Contribution scenarios (employer cost at several allowance levels).** From **RANGE**: not
supported — `contribution` is never written, and with no per-age premiums stored there is nothing to
re-net against a different allowance. From **AGE_BAND**: supported for the *stored* contribution
(`contribution`, `employerOutlay`, `groupNetTotal`). ⚠️ **Recomputing at a different allowance from
stored data alone is possible but only from the band rows** — `floorPremium` per age and `lives` per
band are exactly the inputs needed; `netPerEmployee` and `bandNet` are already netted at the stored
contribution and would have to be recomputed, not reused.

**Affordability threshold. Not computable from stored data in either mode.** `computeAffordability`
(`IllustrationServlet:541-597`) needs three inputs, and the snapshot stores none of them:
`getOnexLcspPremium()` off a cache row (`:574`), the `ICHRA_AFFORDABILITY_PCT_{year}` constant
(`:552`), and either `FPL_ANNUAL_{year}` or the row's entered income (`:584`). Confirmed against the
entities: **neither `ProposalIchraSnapshot` nor `ProposalIchraSnapshotBand` has an on-exchange-LCSP
column or an income column.** Any proposal-side affordability figure would require a live cache read
plus the two constants — the same shape as the `MARKET` section, not the snapshot.

**Is the illustration's affordability/slider genuinely AGE_BAND-only? Partly UI, partly data.**
`computeAffordability` is called from exactly one place — `:511`, inside `handleAgeBandMode`. Its
actual requirement is **an age**, not a band structure: it looks up `cacheByAge.get(row.getAge())`
per row to get that age's on-exchange LCSP. RANGE collects no age from the user, so as written the
computation has nothing to key on — **that is a genuine data constraint on RANGE input**. But nothing
in the arithmetic requires *bands specifically*: the same call would work for a single representative
age, and RANGE already resolves ages 21/40/64 from the cache for its own display. **So: the
computation requires an age; offering it only on the age-banded form is a UI choice on top of that.**

⚠️ **Context, as instructed — not a finding of this audit:** `computeAffordability` reads
`getOnexLcspPremium()`, the V078 on-exchange column, which is the *correct* field (T44's defect was
using the off-exchange one). That column is **null on any cache row warmed before V078**, so
affordability renders its "on-exchange rates not cached; re-warm required" branch (`:575-578`) until
a post-V078 re-warm. Do not read this section as saying the affordability figure is correct today.

---

## 2. The consequence for proposal-builder-first

**A proposal created through the proposal-builder-first flow contains no `ICHRA_ILLUSTRATION` section,
because that flow writes no snapshot** — only the illustration hand-off supplies the `countyFips` and
`mode` parameters that path requires (Q2). What it does produce is a `ProposalIchraIntake` row, which
feeds the **`MARKET`** section and the `{{ICHRA_*}}` merge tokens, both computed live against the rate
cache.

So the two flows produce **different sections, not different fidelities of the same section**: the
hand-off produces a frozen snapshot of what the agent saw; the builder-first flow produces a live
market page recomputed at every render. **Kevin's stated primary flow is the one that cannot produce
the snapshot-backed section at all.**

---

## 3. What could not be established

1. **Whether any `proposal_section` row of type `ICHRA_ILLUSTRATION` or `MARKET` actually exists on
   production.** Both require an admin-created row to render at all; this run read code, not the
   database. `ViewProposal:437-438`'s own comment says MARKET is "byte-identical for every proposal
   with no MARKET section row, which today is all of them," but that comment dates from its own build
   and was not re-verified here.
2. **Whether `onex_lcsp_premium` is populated on any current cache row.** That needs a database read.
3. **Runtime behaviour of the AGE_BAND write path.** Still never executed on any installation — Q3 is
   a reading of the code, not an observation of a row.
4. **Whether the `MARKET` section and the `ICHRA_ILLUSTRATION` section can both appear on one
   proposal.** Nothing read this run prevents it; the interaction was not traced.

---

## 4. Warrants a backlog row — **none filed this run**

The scope fence forbids writing to `docs/analysis/project_backlog.md`, so **no row was filed for
either of the following.** Both are described here so they are not lost.

- **`headcount` is never written on the AGE_BAND snapshot path** (Q3.2). `totalLives` is computed at
  `ProposalBuilder:713` and discarded after being used for `employerOutlay` at `:720`. Low severity —
  the value is recoverable by summing band `lives` — but it leaves a populated-on-one-path,
  null-on-the-other column with no stated reason, and `proposalIchra.jsp:71` renders
  `${ichraSnapshot.headcount}` in the RANGE branch only.
- **A missing or negative `contribution` silently suppresses the entire AGE_BAND snapshot**
  (`ProposalBuilder:661-662`, Q3.4). The agent gets a created proposal with no ICHRA section and no
  indication why. RANGE has no equivalent failure mode.

---

## 5. Code-verified-only disclosure

**Everything in this document is code-verified only.** Every claim rests on reading source files and
migrations in the working tree at `9c9a5ce`. **Nothing was executed, no page was rendered, no
database was queried, no server was started.** In particular: the AGE_BAND write path has still never
run on any installation, and this audit does not change that — it establishes what the code would do,
not what it has done.

---

## 6. SQL close-out audit

**This run produced no SQL.** No migration, no DDL, no DML, no `INSERT INTO constant`. `docs/migrations/`
was read only.

**Highest migration read from `docs/migrations/` this run:**
`V088__proposal_ichra_intake_contribution.sql`. **`git status --short docs/migrations/` returned
empty — migrations unchanged.**

---

## 7. Compliance statement

- **Paths written — exactly one:** `docs/analysis/proposal_render_and_tier_audit.md` (this file).
- **No source file was touched.** No `.java`, no `.jsp`, no `.sql`. `docs/migrations/`,
  `docs/analysis/project_backlog.md`, `docs/ichra_strategy.md`, `docs/swbd_ichra_build_plan.md`, and
  every existing close-out and analysis document were read-only.
- **Hard stops: none fired.** Branch was `refactor/modernize-architecture`, the tree was clean at
  preflight, and `git pull --ff-only` reported *"Already up to date."*
- **Every hash came from `git log` this run.** HEAD: `9c9a5ced9e425ae93251023fe970b3e061fd9632`.
- **No forbidden git operation ran** — no `add -A`/`add .`/`add -u`, no `stash`, `checkout`,
  `restore`, `reset`, `rebase`, `tag`, `branch`, no force-push.
- **Commit count: one.** This document's own hash is not recorded within it.
