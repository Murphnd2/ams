# Phase A — the tier-1 proposal section (S10-D)

**Status:** spec, ready to execute. **Date:** 2026-08-03 · **Branch:** `refactor/modernize-architecture` · **HEAD at investigation:** `3e233c0` (`v0.87.00`)
**Consumes:** `proposal_ichra_intake` (V087, T125) — runtime-verified in production, row 137452 against proposal 137451.

---

## 1. The Q3 gate answer — **NOT BLOCKED**

**The T116 entitlement gate does not require a snapshot.** The premise behind the question was wrong, and
the correction is what unblocks this whole item.

Read [ViewProposal.java:109](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:109):

```java
boolean ichraEntitled = IchraAccessResolver.isAvailableForProposal(em, proposal);
```

That resolves from `OriginatingAgencyResolver.resolve(proposal)` → `Agency.isIchraEnabled()` and **reads no
snapshot at all**. The snapshot enters two lines later, and only to decide whether the `ichraSnapshot`
*request attribute* gets set ([ViewProposal.java:111-120](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:111)).
The `source_env == PRODUCTION` test S10-A flagged gates **that attribute**, not the gate and not the section list.

The gate's second half ([ViewProposal.java:293-297](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:293))
strips section types in `ICHRA_GATED_SECTION_TYPES`, which is `Set.of(ProposalIchraSnapshot.SECTION_TYPE)` —
**exactly one type, `"ICHRA_ILLUSTRATION"**` ([ViewProposal.java:54](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:54)).

The snapshot dependency that *does* exist is in the JSP, and it is scoped to one branch:
[viewProposal.jsp:186-192](../../src/main/webapp/WEB-INF/view/sales/viewProposal.jsp:186) wraps **only** the
`ICHRA_ILLUSTRATION` branch in `<c:if test="${not empty ichraSnapshot}">`.

**Therefore a tier-1 section typed `CUSTOM` is untouched by every snapshot dependency in the render path**, and
renders from `sectionHtml` like any other custom page ([viewProposal.jsp:198-202](../../src/main/webapp/WEB-INF/view/sales/viewProposal.jsp:198)).
No gate change, no snapshot, no new type.

### ⚠️ The real gate question, which is the opposite of the one asked

A `CUSTOM` section is **not** in `ICHRA_GATED_SECTION_TYPES`, so `ichraEntitled` never filters it. Tier-1
therefore renders for **any** agency whose proposal carries the scoped LOS — entitled or not.

**What actually holds it closed is reference data, not code.** An agent can only put the plus-tier LOS on a
proposal if their agency's assigned rate has a `RateTable` row for it — `ProposalBuilder` gives a non-admin
only `fullAgency.getAgencyRateList()` ([ProposalBuilder.java:70-79](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:70))
and the LOS cards are filtered to that rate's modules ([ProposalBuilder.java:96-107](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:96)).
Kevin controls that by rate assignment, per agency.

**Say plainly what this is: a configuration gate, weaker than T116's code gate, and keyed on a different
flag.** `agencyrates` assignment and `agency.ichra_enabled` can disagree. That is acceptable **only because
tier-1 carries no market data** (§5) — it is not the content LA-17 exists to constrain. If tier-1 ever
acquires a rate, a carrier, a plan count or an affordability figure, this gate is insufficient and the
section must move behind `ICHRA_GATED_SECTION_TYPES`.

**Adding a type to that set is a change to the T116 gate.** It is a *strengthening* change and a one-line
`Set.of(...)` edit, but it is still the gate, so per the run brief it is **its own scoped run** — filed as
**T129**, not done inside this build.

---

## 2. Recommendation (Q5) — **(a) now, (b) as an immediate follow-on**

**Ship (a): Kevin's authored HTML as a `CUSTOM` section, `SCOPED` to the plus-tier LOS. Zero code, zero
migration, zero SQL.** Everything it needs already exists and is already live.

**Then (b), which Phase A found is far cheaper than the fork implied** — because `CUSTOM` sections already
render dynamic values through the `{{TOKEN}}` merge mechanism (§4-Q1). Making T125's row live is **not** a new
section type, a new table, or a gate change. It is ~20 lines in one method, `buildTokenMap`.

| | (a) static custom page | (b) dynamic tokens |
|---|---|---|
| Build size | **0 lines of code** | ~20 lines, one file, one method |
| Migration | none | none |
| Reversal | delete the section row | delete the token entries |
| In front of Forrest | **as soon as Kevin pastes the HTML** | one release later |
| Consumes T125's row | ❌ no | ✅ yes |

They are not alternatives — (b) is (a) plus tokens in the same section row. **Sequence them; do not choose.**
Kevin's HTML can carry `{{ICHRA_COUNTY}}` placeholders from day one: until (b) ships they render as literal
text, which is why step 4 makes shipping (b) before the content goes to a real employer a hard requirement.

---

## 3. Build steps

### Phase (a) — configuration only, no code

**Step 1 — Kevin creates the section.** `/ProposalSettings` → "Add Custom Page" → paste the authored HTML →
set scope `SCOPED` and tick the plus-tier LOS. Backed by the `createCustom`
([ProposalSettings.java:146-167](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalSettings.java:146)),
`saveContent` ([ProposalSettings.java:120-131](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalSettings.java:120))
and `setScope` ([ProposalSettings.java:392-413](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalSettings.java:392))
actions, all of which already accept `CUSTOM`. **No file is touched by this step.**

⚠️ Sort order matters: `createCustom` places the new section immediately before `CLOSING`
([ProposalSettings.java:139-163](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalSettings.java:139)).
If tier-1 should sit elsewhere, reorder it in the admin UI after creating it.

### Phase (b) — the dynamic follow-on

**Step 2 — `ViewProposal.java`, `buildTokenMap` — existing, ~20 lines.**
[ViewProposal.java:446-497](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:446).
**Reason:** it is the single place proposal-scoped values become section-renderable, and tier-1 needs the
employer's own county and headcount in prose. **Minimal diff:** load the intake row once via a new
`ProposalIchraIntakeDAO.findByProposalId(em, proposal.getId())` call (the DAO method already exists,
[ProposalIchraIntakeDAO.java:14-23](../../src/main/java/net/superiorstate/ams/data/dao/ProposalIchraIntakeDAO.java:14)),
then put four tokens. Add the `EntityManager` to the method signature, or resolve the intake in `doGet` and
pass it — either is acceptable; the signature is private and has one caller
([ViewProposal.java:340](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:340)).

Tokens, and their **absent-row fallbacks — which are mandatory, not optional**:

| Token | With an intake row | No intake row |
|---|---|---|
| `{{ICHRA_COUNTY}}` | `Hopkins County` | `your county` |
| `{{ICHRA_STATE}}` | `TX` | `` (empty) |
| `{{ICHRA_HEADCOUNT}}` | `10` | `your` |
| `{{ICHRA_PLAN_YEAR}}` | `2026` | `` (empty) |

⚠️ **A token with no fallback is a defect, not a blank.** `replaceTokens`
([ViewProposal.java:500-509](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:500))
substitutes every key in the map unconditionally; a missing key is left as the literal string `{{ICHRA_COUNTY}}`
**on a customer-facing page**, and an empty-string value produces `"employees in ."`. The fallbacks above are
chosen so the sentence reads correctly either way — draft the prose against the *fallback* column first.

**Step 3 — nothing else.** No new entity, no DAO, no JSP change, no section type, no migration, no
`persistence.xml` edit. The DAO, the entity, the render branch and the merge engine all already exist.

**Step 4 — content review before the section reaches a real employer.** §5. Not a code step; do not skip it.

---

## 4. The gate chain — fails closed at every link

| # | Where | Check | On failure |
|---|-------|-------|-----------|
| 1 | Reference data (Kevin) | The plus-tier LOS has a `RateTable` row only in rates assigned to intended agencies | agent cannot select the LOS → no scoped section |
| 2 | `ProposalBuilder` doGet | non-admin sees only `fullAgency.getAgencyRateList()` ([:70-79](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:70)) | LOS card absent |
| 3 | `ViewProposal` scope pass | `SCOPED` section renders only if a linked LOS is on the proposal ([:249-276](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:249)) | section dropped from the list |
| 4 | `ProposalSection.active` | admin toggle ([:234-240](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalSettings.java:234)) | not loaded — the query filters `s.active = true` ([ViewProposal.java:221](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:221)) |
| 5 | Token fallbacks (phase b) | no intake row → neutral wording, never a literal `{{TOKEN}}` | prose degrades to generic; page still correct |

**Link 3 is the operative one.** Links 1–2 are configuration; link 5 is correctness, not access. **There is
no code-level entitlement link in this chain** — that is the §1 finding, stated again here so a build run
cannot mistake this chain for T116-equivalent protection.

---

## 5. Content boundary (Q4)

**Tier-1 needs no new legal assumption, because it is not the content LA-17 governs.**

LA-17 constraint 3 ([legal_assumptions.md:1034-1039](legal_assumptions.md)) already partitions the document:
*"The agent's section carries market data. SSA's supplemental section describes administration services only —
no plans, no carriers, no selection guidance. Keep them visually and structurally distinct."*

**Tier-1 *is* that supplemental section.** It carries no market data, so LA-17's carve-out — the thin,
"Assumed" one resting on structural reasoning and an industry-pattern observation — **is not load-bearing for
it at all.** Tier-1 sits inside the zone LA-09 never restricted. **No `LA-NN` is filed by this run.**

**MAY contain:**
- ✅ **The employer's own inputs echoed back — county name, headcount.** These came from the agent about the
  employer; they are geography and the employer's own census, not the result of any market query. Nothing in
  LA-09 or LA-17 reaches them.
- ✅ Generic explanation of how ICHRA/QSEHRA works — statute-level mechanics, no product.
- ✅ SSA administration fees already on the proposal's own pricing summary (`PRICING` section, same document).
- ✅ An invitation to a consultation with the agent.

**MUST NOT contain:** rates or premium figures of any kind · carrier names · plan names or counts ·
premium ranges · affordability figures or determinations · any ranking, curation, recommendation, or default ·
any enrollment path · agent markup as a distinguishable line (markup is invisible to the employer, V066/V067).

⚠️ **The one drift risk, and it is a phrasing risk rather than a data risk.** Echoing the county is safe;
*characterising* it is not. `"employees in {{ICHRA_COUNTY}}"` is fine. `"we found coverage options in
{{ICHRA_COUNTY}}"` asserts a market fact and crosses into LA-09 territory with no data on the page to
support it. **Review the prose for implied market claims, not just for numbers** — the section can breach
the boundary without rendering a single figure.

---

## 6. Migration body

**None. This spec requires no schema change of any kind** — not for (a), not for (b). `proposal_ichra_intake`
(V087) and `proposal_section` (V036/V037/V044) already hold everything. No `.sql` file was created and none
is recommended.

---

## 7. What this spec does NOT cover — build-time hard-stops

1. **The content itself is Kevin's and does not exist in the repository.** This spec establishes that the
   mechanism accepts it, not that it is written. A build run must not author employer-facing prose.
2. **Whether tier-1 should be code-gated on `agency.ichra_enabled`.** §1 says it currently is not, and why
   that is tolerable while the content stays market-data-free. **This is Kevin's call, not a build run's** —
   filed as **T129**.
3. **Print/page-break behaviour inside `.proposal-container` (max-width 900px) is unverified.** Kevin's
   blocks are print-sized with their own `page-break-before`. The skill documents the container
   ([SKILL.md:25-27](../../.claude/skills/proposal-content-page/SKILL.md)) but no one has run a real
   print preview of a page-sized block in this position.
4. **Multiple plus-tier LOS on one proposal → one intake row but potentially several scoped sections.** The
   scope pass matches *any* linked LOS ([ViewProposal.java:256-263](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:256)),
   so two scoped sections both render. Not wrong, but unconsidered.
5. **`{{ICHRA_HEADCOUNT}}` is the headcount at proposal-creation time and never updates.** No edit path
   exists (T125's `uq_pii_proposal`). A proposal sent months later shows a stale figure with no indication.
6. **No runtime walk of any of this.** §8 of the close-out lists exactly what remains unconfirmed.

---

## 8. Claims verified and falsified

| # | Claim | Verdict | Evidence |
|---|---|---|---|
| 1 | T116's gate requires a snapshot to render any ICHRA content | ❌ **falsified** | `isAvailableForProposal` reads only the originating agency's flag ([ViewProposal.java:109](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:109)); the snapshot is fetched separately and only sets a request attribute ([:111-120](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:111)) |
| 2 | `ViewProposal` depends on `proposal_ichra_snapshot.source_env` as a fail-closed check | ✅ **confirmed, but narrower than stated** | [ViewProposal.java:114](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:114) — it gates the *attribute*, hence only the `ICHRA_ILLUSTRATION` JSP branch ([viewProposal.jsp:187](../../src/main/webapp/WEB-INF/view/sales/viewProposal.jsp:187)). It gates nothing else |
| 3 | Sections can only render static stored content | ❌ **falsified — the pivotal finding** | `{{TOKEN}}` merge: `buildTokenMap` ([:446-497](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:446)) + `replaceTokens` ([:500-509](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:500)), applied to `CUSTOM` ([:346-349](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:346)) |
| 4 | `proposalsectionlos` (V037) associates a section with an LOS | ✅ **confirmed** | `@ManyToMany @JoinTable(name="proposalsectionlos")` ([ProposalSection.java:40-44](../../src/main/java/net/superiorstate/ams/model/sales/offering/ProposalSection.java:40)); scope pass at [ViewProposal.java:249-276](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:249) |
| 5 | A section is a config row Kevin creates, not code | ✅ **confirmed** | `ProposalSettings` `createCustom`/`saveContent`/`setScope` actions; `html_content` is a `text` column ([ProposalSection.java:34-35](../../src/main/java/net/superiorstate/ams/model/sales/offering/ProposalSection.java:34)) |
| 6 | **Agency-scoped** custom HTML pages exist | ❌ **falsified** | `createCustom` never calls `setAgency` ([ProposalSettings.java:146-152](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalSettings.java:146)), and the agency-override pass handles **only** `TITLE`/`CLOSING` ([ViewProposal.java:307](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:307), [:317](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:317)). `CUSTOM` is PSP-wide, LOS-scopable, never agency-scoped |
| 7 | The mechanism accepts raw HTML with embedded `<style>` and a Google Fonts `@import` | ✅ **confirmed** | `sanitizeHtml` strips `<script>`, `on*`, `javascript:` and **preserves `<style>`** by design ([ProposalSettings.java:501-515](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalSettings.java:501)); the repo skill documents `@import` inside `<style>` as the intended delivery ([SKILL.md:46,82](../../.claude/skills/proposal-content-page/SKILL.md)) |
| 8 | Custom pages render on the public proposal link | ✅ **confirmed** | `/proposal/*` → `viewProposal.jsp`, `CUSTOM` branch emits `${sectionHtml[...]}` unescaped ([viewProposal.jsp:198-202](../../src/main/webapp/WEB-INF/view/sales/viewProposal.jsp:198)) |
| 9 | A new standalone ICHRA proposal page is needed | ❌ **falsified** | Both existing mechanisms are usable; standing rule 1's bar is not met |
| 10 | Tier-1 content requires an LA-17-style market-data assumption | ❌ **falsified** | LA-17 constraint 3 already permits an SSA administration-only supplemental section ([legal_assumptions.md:1034-1039](legal_assumptions.md)) |
| 11 | Highest backlog T-number is T127 (per S10-B's close-out) | ❌ **falsified** | Highest actual row is **T126**. **S10-B's close-out claims it filed T127; no such row exists.** Corrected by this run |
| 12 | Latest migration is V087 | ✅ **confirmed** | `ls docs/migrations/` — 64 versioned files, highest `V087__proposal_ichra_intake.sql` |
