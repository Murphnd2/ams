# Phase A — Proposal Builder ICHRA interjection (S10-A)

**Status:** spec, ready to execute. **Not** a report — a build prompt consumes this.
**Date:** 2026-08-03 · **Branch:** `refactor/modernize-architecture` · **HEAD at investigation:** `a517141`
**Consumes:** `los.is_plus_tier` (V086, currently read by nothing but the Service Manager admin form).

---

## 0. The one-line summary

When an ICHRA-entitled agent selects a **plus-tier** LOS in `/ProposalBuilder`, reveal an inline
ZIP + headcount panel before **Create Proposal**, and persist what was collected against the new
proposal. Unentitled agent → the markup is never emitted → no data → no plus-tier content.
Fails closed at six independent links.

---

## 1. The T9 decision — **build around it**

**T9 does not sit on this feature's path at all.** The premise behind the question was wrong in a way
that makes the decision trivial, and the correction matters more than the decision:

`GenerateProp25` ([GenerateProp25.java:39](../../src/main/java/net/superiorstate/ams/controller/activity/setup/GenerateProp25.java:39))
is **not** the Proposal Builder. It is the **Manual Setup** servlet — a PSP-staff shortcut that
manufactures Person → Prospect → Proposal → Application → Setup in one POST from a fixed
eight-checkbox form. Its hardcoded region is
[GenerateProp25.java:205-235](../../src/main/java/net/superiorstate/ams/controller/activity/setup/GenerateProp25.java:205)
(`q1..q6` → LOS ids 5,6,7,9,10,8) and
[GenerateProp25.java:245-278](../../src/main/java/net/superiorstate/ams/controller/activity/setup/GenerateProp25.java:245)
(`q1..q8` → `ServiceItem` ids 11–19). Those ids gate **which LOS get attached to the proposal and which
modules get attached to the application** — i.e. it *writes* `getLosList()` (line 229) and never *reads* it.

The Proposal Builder is a different servlet entirely:
[ProposalBuilder.java:32](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:32),
and its `createProposal` is **already fully dynamic** —
[ProposalBuilder.java:394-406](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:394)
reads `request.getParameterValues("losIds")` and attaches whatever came in. There is no id literal
anywhere in `ProposalBuilder.java`.

**Decision: build around.** The interjection asks its own question — *"does any selected LOS carry
`is_plus_tier`?"* — against the `LOS` entities the builder already loads
([ProposalBuilder.java:88-92](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:88)),
and never touches `GenerateProp25`. **Reversal cost: nil** — the two files are unrelated; nothing in this
build makes T9 harder or easier to fix later. T9 stays open, unchanged, as its own item.

⚠️ **Falsified premise, recorded:** the run brief called `GenerateProp25` "live, customer-facing, and
serves every line of service." It is **PSP-staff-only** — `isPspStaff()` 403s everyone else
([GenerateProp25.java:48-59](../../src/main/java/net/superiorstate/ams/controller/activity/setup/GenerateProp25.java:48))
— and is reached only from three internal surfaces (`manualSetup.jsp:23`,
`generateSetupForm25.jsp:3`, `reviewApplications.jsp:47`). No customer ever sees it. Phase A was
therefore cheaper than budgeted, but the caution was not wasted: it surfaced the servlet mix-up.

---

## 2. Build steps

Each step names exact paths. **New** = create; **existing** = minimal diff, reason stated.

### Step 1 — `ProposalIchraIntake` entity — **new**
`src/main/java/net/superiorstate/ams/model/sales/agency/ProposalIchraIntake.java`

Mirror the shape of the sibling
`ProposalIchraSnapshot` ([ProposalIchraSnapshot.java:33-96](../../src/main/java/net/superiorstate/ams/model/sales/agency/ProposalIchraSnapshot.java:33)):
`@Table(name="proposal_ichra_intake")`, `@OneToOne @JoinColumn(name="proposal_id", nullable=false, unique=true)`,
`@ManyToOne @JoinColumn(name="created_by")` → `Person`. Fields per §4's DDL.

### Step 2 — `ProposalIchraIntakeDAO` — **new**
`src/main/java/net/superiorstate/ams/data/dao/ProposalIchraIntakeDAO.java`
One `save(EntityManager, ProposalIchraIntake)`. Copy the transaction idiom from
`ProposalIchraSnapshotDAO`.

### Step 3 — `ProposalBuilder.doGet` — **existing, ~4 lines**
[ProposalBuilder.java:88-92](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:88)
**Reason:** the JSP cannot decide entitlement, and must not receive plus-tier data it isn't allowed to act on.
Diff: after the `losList` block, `boolean ichraAvailable = IchraAccessResolver.isAvailable(em, request);`
and `request.setAttribute("ichraAvailable", ichraAvailable);`. Import the resolver. Nothing else changes.

### Step 4 — `ProposalBuilder.doPost` / `createProposal` — **existing, ~8 lines + one private method**
[ProposalBuilder.java:387-391](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:387)
**Reason:** the intake must be written against the proposal that was just created, and the LOS list is
only known after line 406.
Diff: **after** the `losIds` loop (line 406), add a best-effort block byte-identical in shape to the
existing snapshot call —
```java
try {
    attachIchraIntakeIfPresent(request, em, proposal, createdBy);
} catch (Exception e) {
    System.out.println("ICHRA intake attach failed for proposal #" + proposal.getId() + ": " + e.getMessage());
}
```
plus one new private method implementing gate links 5–7 of §3. **Do not** touch
`attachIchraSnapshotIfPresent` or anything above line 406.

### Step 5 — `proposalBuilder.jsp` LOS cards — **existing, 1 attribute**
[proposalBuilder.jsp:163](../../src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp:163)
**Reason:** the client needs to know which cards are plus-tier, and only when entitled.
Diff: on the `.los-card-wrapper` div, append
`<c:if test="${ichraAvailable}"> data-plus-tier="${los.plusTier}"</c:if>`.
An unentitled render emits **no attribute at all** — not `false`, absent.

### Step 6 — `proposalBuilder.jsp` intake panel — **existing, new block inside the existing form**
Insert between STEP 3's closing `</div>` and the Submit row —
[proposalBuilder.jsp:182-191](../../src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp:182).
**Reason:** it must post with the existing single form; a second form would need a second round trip.
Wrap the whole block in `<c:if test="${ichraAvailable}">` so it is absent, not hidden, for everyone else.
Contents: ZIP text input, county `<select>` (populated by `/IchraZipLookup`), headcount number input,
and a hidden `intakeCountyName`/`intakeState` pair filled from the chosen county.

> ⚠️ **Name-collision hard requirement.** The form **already** carries hidden inputs named
> `headcount`, `countyFips`, `planYear`, `mode`, `contribution`, `age1..6`, `count1..6`
> ([proposalBuilder.jsp:61-77](../../src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp:61)) for the
> illustration hand-off. The interjection's fields **must** be named `intakeZip`, `intakeCountyFips`,
> `intakeCountyName`, `intakeState`, `intakeHeadcount`. Reusing `headcount` would produce two
> parameters of that name whenever an agent arrives from the illustration hand-off, and
> `request.getParameter` returns the first — silently mixing two different numbers.

### Step 7 — `proposalBuilder.jsp` JS — **existing, ~40 lines**
[proposalBuilder.jsp:469-506](../../src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp:469)
**Reason:** `toggleLos` and `updateSteps` are the only places that know the selection changed.
Diff: at the end of `toggleLos()` and `filterLosCards()` call a new `updateIntakePanel()` that shows the
panel iff any element in `selectedLosIds` maps to a wrapper with `data-plus-tier="true"`, and clears the
three intake inputs when it hides. Extend `updateSteps()`'s final line so `btnCreate.disabled` additionally
requires — **only when the panel is visible** — a 5-digit ZIP, a selected county, and headcount ≥ 1.
Add a `change`/`blur` handler on the ZIP field that does
`fetch('IchraZipLookup?zip=' + z + '&planYear=' + y)` and fills the county select from
`{"zip":…,"counties":[{"fips","name","state","priced"}]}`
([IchraZipLookup.java:84-110](../../src/main/java/net/superiorstate/ams/controller/market/IchraZipLookup.java:84)).

> **A ZIP may resolve to several counties, and that is the common case, not an edge case** —
> `ZipCountyResolver`'s own javadoc records 34% of Texas ZCTAs crossing a county line
> ([ZipCountyResolver.java:15-45](../../src/main/java/net/superiorstate/ams/data/resolver/ZipCountyResolver.java:15)).
> The panel **must** present the choice and must **never** auto-pick `counties[0]`. Zero counties is
> **not an error** — render "we don't have that ZIP" and leave the county select usable.
> Label unpriced counties from the `priced` flag; do not reorder or disable them.

### Step 8 — migration — **new**, `docs/migrations/V087__proposal_ichra_intake.sql`
Body verbatim in §4. Then `docs/analysis/migration_tracker.md` and
`docs/schema_version_migration.sql` per CLAUDE.md migration discipline.

### Step 9 — `persistence-*.xml`
Register `ProposalIchraIntake` **only if** the persistence units enumerate `<class>` entries.
Check `src/main/resources/META-INF/persistence-server.xml` and `-local.xml` first — if
`ProposalIchraSnapshot` is not listed there, the units auto-discover and this step is a no-op.

---

## 3. The gate chain — six links, each fails closed

Implement in this order. **Failure behaviour at every link is identical: no intake row is written and
the proposal is created exactly as it is today.** No link ever produces an error page.

| # | Where | Check | On failure |
|---|-------|-------|-----------|
| 1 | `ProposalBuilder.doGet` (Step 3) | `IchraAccessResolver.isAvailable(em, request)` — live, per request | `ichraAvailable=false` |
| 2 | `proposalBuilder.jsp` LOS cards (Step 5) | `<c:if test="${ichraAvailable}">` around `data-plus-tier` | attribute absent; JS can never find a plus-tier card |
| 3 | `proposalBuilder.jsp` panel (Step 6) | `<c:if test="${ichraAvailable}">` around the whole block | markup absent from the DOM; no fields exist to post |
| 4 | `updateIntakePanel()` (Step 7) | any selected LOS has `data-plus-tier="true"` | panel hidden **and inputs cleared** — a stale value must not survive a deselect |
| 5 | `attachIchraIntakeIfPresent` (Step 4) | `IchraAccessResolver.isAvailable(em, request)` **again**, server-side | return; no row |
| 6 | `attachIchraIntakeIfPresent` (Step 4) | re-load the submitted `losIds` and require **at least one** `los.isPlusTier()` | return; no row |

**Links 1–4 are UX. Links 5 and 6 are the actual gate.** Everything above link 5 is client-controlled and
must be treated as an assertion by the caller, not a fact. Link 6 exists specifically because a caller can
POST `intakeZip` with a non-plus-tier `losIds` set; re-deriving plus-tier from the database is the only
answer that does not trust the form.

Validation inside link 6, all fail-closed, all `return` without a row:
`intakeZip` not exactly 5 digits · `intakeCountyFips` not exactly 5 digits · `intakeHeadcount` null or `< 1`
or `> 10000` (matches the illustration's own bound, per T85's error text) · `intakeState` not 2 chars ·
`intakeCountyName` blank.

**Entitlement uses `IchraAccessResolver.isAvailable(EntityManager, HttpServletRequest)` unchanged —
no new signature is needed.** See §5-Q4.

---

## 4. Migration body (text only — this run creates no `.sql` file)

`docs/migrations/V087__proposal_ichra_intake.sql`:

```sql
-- V087: Plus-tier intake captured in the Proposal Builder (proposal_ichra_intake)
--
-- The consumer V086 was written for. When an ICHRA-entitled agent selects an LOS carrying
-- los.is_plus_tier, the builder interjects a ZIP + county + headcount panel before Create
-- Proposal; this table records what was collected. One row per proposal, or none.
--
-- ⚠️ NOT the same thing as proposal_ichra_snapshot (V079). That table records COMPUTED RATE
-- OUTPUT and is legitimately NOT NULL on county_fips/state/county_name/plan_year/source_env,
-- because ViewProposal fails closed unless source_env='PRODUCTION'. Intake is INPUT: it must
-- persist for a county with no warmed rates at all, which is the majority of Texas counties
-- today. Widening V079's NOT NULLs to hold intake would destroy the compliance invariant
-- ViewProposal depends on. Two tables, deliberately. A proposal may carry either, both, or
-- neither, and they are written by independent best-effort paths.
--
-- Scope of the data: employer ZIP, resolved county, and eligible-employee headcount, entered
-- by an AGENT about a PROSPECT EMPLOYER. No employee-level data, no PHI, no SSN, no
-- individual identifiers of any kind. Nothing a natural person enters about themselves.
--
-- zip is stored alongside county_fips rather than discarded because a ZIP resolving to
-- several counties is the common case (34% of TX ZCTAs) -- keeping the ZIP the agent actually
-- typed makes a later wrong-county diagnosis possible. county_name/state are denormalized for
-- the same reason V079 denormalizes them: the render path must never join county_reference.
--
-- Reversal: DROP TABLE. The proposal table is untouched, so nothing on the hot core entity
-- has to be rolled back and no existing query plan changes.
--
-- Prerequisites: V079 (sibling shape reference only, not a hard dependency), V084/V085
-- (zip_county crosswalk -- without it the panel's county chooser returns empty, which is a
-- handled state, not a failure), V086 (los.is_plus_tier -- the trigger).

-- ----------------------------------------------------------------------
-- 1. proposal_ichra_intake -- one row per proposal, or none
-- ----------------------------------------------------------------------
CREATE TABLE proposal_ichra_intake (
    intake_id     BIGINT        NOT NULL AUTO_INCREMENT,
    proposal_id   BIGINT        NOT NULL,
    zip           CHAR(5)       NOT NULL,                 -- as typed by the agent, five digits
    county_fips   CHAR(5)       NOT NULL,                 -- the county the agent CHOSE, never auto-picked
    county_name   VARCHAR(100)  NOT NULL,                 -- denormalized; render never joins county_reference
    state         CHAR(2)       NOT NULL,
    headcount     SMALLINT      NOT NULL,                 -- eligible employees, 1..10000
    collected_at  DATETIME      NOT NULL,
    created_by    BIGINT        NULL,                     -- FK -> assignee(id); Person is SINGLE_TABLE under assignee
    PRIMARY KEY (intake_id),
    UNIQUE KEY uq_pii_proposal (proposal_id),
    CONSTRAINT fk_pii_proposal FOREIGN KEY (proposal_id) REFERENCES proposal(proposal_id) ON DELETE CASCADE,
    CONSTRAINT fk_pii_created_by FOREIGN KEY (created_by) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V087' AS version, '2026-08-03' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V087', 'Plus-tier ZIP/county/headcount intake captured in the Proposal Builder (proposal_ichra_intake)', 'V087__proposal_ichra_intake.sql', NOW());
```

**Alternatives weighed and rejected:**

| Option | Reversal cost | Verdict |
|---|---|---|
| Columns on `proposal` | `DROP COLUMN` × 5 on the hottest entity in the sales path; every `SELECT p FROM Proposal` widens | Rejected — `proposal` is joined everywhere; the sibling V079 already established the side-table pattern for exactly this |
| Reuse `proposal_ichra_snapshot` | Requires making five `NOT NULL` columns nullable, **including `source_env`** | **Rejected on compliance grounds.** `ViewProposal` fails closed on `source_env='PRODUCTION'`; a nullable `source_env` makes that check meaningless. Do not do this |
| A generic key/value attribute table | None exists in the model to reuse | Rejected — would be net-new anyway, and untyped |
| New 1:1 `proposal_ichra_intake` | `DROP TABLE` | ✅ **Recommended** |

**1:1:1 survival:** `Application` PK *is* `proposal_id` and `Setup` hangs off `Application`
(memory + [GenerateProp25.java:236-244](../../src/main/java/net/superiorstate/ams/controller/activity/setup/GenerateProp25.java:236)),
so an intake row keyed on `proposal_id` is reachable from any point on the
Proposal → Application → Setup path without a new join column. `ON DELETE CASCADE` matches V079.

---

## 5. Q1–Q5, condensed

**Q1 — Build around T9.** §1. `GenerateProp25` is Manual Setup, not the builder; `ProposalBuilder` has no
hardcoded ids. Reversal cost nil. T9 unchanged.

**Q2 — LOS selection is fully knowable client-side, no round trip.** Cards at
[proposalBuilder.jsp:161-175](../../src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp:161),
`onclick="toggleLos(this, ${los.getId()})"` →
[proposalBuilder.jsp:469-494](../../src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp:469)
maintains a JS `Set` and rebuilds hidden `losIds` inputs on every toggle. The three "steps" are **badges on
one page**, not pages — there is no existing multi-step flow to ride, but there is a single `<form>`
([proposalBuilder.jsp:54](../../src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp:54)) and a single
`updateSteps()` gate on the submit button
([proposalBuilder.jsp:496-506](../../src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp:496)), which
is a better host than a wizard would be. AJAX probe: the page issues **no** `fetch`/`XHR`/`$.ajax` today —
the interjection's `/IchraZipLookup` call is the first, and that endpoint already exists and is already gated.

**Q3 — Submit is one POST, and the additive-parameter pattern already exists.**
`POST /ProposalBuilder` with `action=createProposal` →
[ProposalBuilder.java:327-331](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:327)
→ `createProposal` (line 340). Parameters consumed today: `prospectId`, `rateId`, `sourceActivityId`,
`losIds[]`, plus the illustration hand-off set. **The smallest diff is one best-effort call after line 406**,
exactly mirroring `attachIchraSnapshotIfPresent`
([ProposalBuilder.java:387-391](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:387)),
whose own comment already states the standard: *byte-identical no-op for every existing caller.* With
`intake*` parameters absent — every existing entry point, every non-plus-tier LOS, every unentitled agent —
the method returns before doing anything.

**Q4 — `IchraAccessResolver.isAvailable(EntityManager, HttpServletRequest)`. No new signature.**
[IchraAccessResolver.java:44-97](../../src/main/java/net/superiorstate/ams/data/resolver/IchraAccessResolver.java:44).
Session-scoped, live, per-request, fails closed, never throws; resolves PSP-admin → primary agency →
full `AgencyScope.detailAgencyIds()` membership. It is already what `IchraZipLookup`, `IllustrationServlet`
and `IchraHome` call. `isAvailableForProposal` (line 145) is **not** applicable — no proposal exists at
builder time, and it deliberately carries no PSP-admin bypass because its audience is the public page.
`isAvailableForNav` (line 189) is **not** an authorization check and must not be used here; its own javadoc
forbids it.

**Q5 — New 1:1 `proposal_ichra_intake`, migration V087.** §4.

---

## 6. What this spec does NOT cover — name these as hard-stops in the build prompt

1. **Nothing consumes the intake row.** This spec captures data; it does not render a plus-tier proposal
   section, does not price anything, and does not change `ViewProposal`. That is the *next* piece of work.
   The build prompt must not quietly extend into it.
2. **Plan year is not collected.** `/IchraZipLookup`'s `priced` flag needs a `planYear` to be meaningful.
   The build must decide: hardcode the current year client-side, or add a fourth field. **Recommendation:**
   derive it in the JSP from the server's current year and post it as `intakePlanYear`; do not ask the agent.
   *Not resolved here — resolve it at build time and say which was chosen.*
3. **Edit-after-create is undefined.** There is no path to change an intake once the proposal exists.
   `UNIQUE KEY uq_pii_proposal` means a second write fails. The build should catch and log, not retry.
4. **No plus-tier LOS row is flagged anywhere.** See the note to Kevin. The build can be completed and
   compiled without one; it cannot be *demonstrated* without one.
5. **Multi-plus-tier selection is treated as one interjection.** Two plus-tier LOS selected → one panel,
   one intake row. If any future plus-tier line needs its own ZIP, this shape is wrong. Nothing today does.
6. **The county chooser's visual design is unspecified.** Match the illustration page's existing chooser
   rather than inventing one; the build should read `illustration*.jsp` first.
7. **Untested against a session with multiple agencies.** Link 5 inherits whatever `isAvailable` does for a
   multi-membership agent, including the unresolved T64 question about EclipseLink serving a stale `Agency`.

---

## 7. Claims verified and claims falsified

| # | Claim (from the run brief / project knowledge) | Verdict | Evidence |
|---|---|---|---|
| 1 | `los.is_plus_tier` shipped in V086 and **nothing reads it** | ✅ **confirmed** (with nuance) | `docs/migrations/V086__los_plus_tier.sql`; the only readers are `ServiceManagerAction.java:125` (write) and `serviceManager25.jsp:890-892` (admin display). No behaviour reads it |
| 2 | `GenerateProp25` hardcodes LOS ids 5–10 | ✅ **confirmed** | [GenerateProp25.java:212-223](../../src/main/java/net/superiorstate/ams/controller/activity/setup/GenerateProp25.java:212) — ids 5,6,7,9,10,8. Also `ServiceItem` 11,12,13,16,15,14,17,19 at lines 254-269 |
| 3 | `GenerateProp25` never reads `Proposal.getLosList()` | ❌ **falsified as stated** | It *writes* it — `proposal.getLosList().add(los)` at [GenerateProp25.java:229](../../src/main/java/net/superiorstate/ams/controller/activity/setup/GenerateProp25.java:229). The true defect is that it never reads an agent's *selection*; the backlog T9 row's phrasing carried this over |
| 4 | `GenerateProp25` is **live and customer-facing** | ❌ **falsified** | `isPspStaff()` 403 at [GenerateProp25.java:48-59](../../src/main/java/net/superiorstate/ams/controller/activity/setup/GenerateProp25.java:48); callers are `manualSetup.jsp:23`, `generateSetupForm25.jsp:3`, `reviewApplications.jsp:47` — all internal |
| 5 | `GenerateProp25` is the Proposal Builder / on the interjection's path | ❌ **falsified** | Separate servlets: `/GenerateProp25` vs `/ProposalBuilder` ([ProposalBuilder.java:32](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:32)). Different JSPs, different POST handlers |
| 6 | T116 added `isAvailableForProposal`, and there is no proposal at builder time | ✅ **confirmed** | [IchraAccessResolver.java:145](../../src/main/java/net/superiorstate/ams/data/resolver/IchraAccessResolver.java:145); `createProposal` constructs the `Proposal` at [ProposalBuilder.java:357](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:357) |
| 7 | The builder is a new front door / needs net-new plumbing for extra params | ❌ **falsified** | The additive optional-parameter path already exists end to end — hidden inputs at [proposalBuilder.jsp:61-77](../../src/main/webapp/WEB-INF/view/sales/proposalBuilder.jsp:61) → `attachIchraSnapshotIfPresent` at [ProposalBuilder.java:422](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:422) |
| 8 | ZIP → county resolution needs building | ❌ **falsified** | `/IchraZipLookup` exists, returns JSON with a `priced` flag, and is already gated by `isAvailable` ([IchraZipLookup.java:47-82](../../src/main/java/net/superiorstate/ams/controller/market/IchraZipLookup.java:47)) |
| 9 | ZIP+headcount could just reuse `proposal_ichra_snapshot` | ❌ **falsified** | Five `NOT NULL` columns incl. `source_env`, and `ProposalBuilder` refuses to write unless PRODUCTION rate rows exist ([ProposalBuilder.java:462-470](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:462)). Intake must survive an unpriced county |
| 10 | V086 is the latest migration | ✅ **confirmed** | `ls docs/migrations/` → 63 versioned files, highest `V086__los_plus_tier.sql` |
| 11 | Highest backlog T-number is T124 | ✅ **confirmed** | `docs/analysis/project_backlog.md` — T125/T126 are free |
