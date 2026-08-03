# S10-D close-out — Phase A: the tier-1 proposal section

**Date:** 2026-08-03 · **Branch:** `refactor/modernize-architecture` · **Type:** Phase A investigation, spec output
**Deliverable:** [docs/analysis/phase_a_tier1_proposal_section.md](../analysis/phase_a_tier1_proposal_section.md)

---

## 1. Preflight output, verbatim

```
$ git rev-parse --abbrev-ref HEAD
refactor/modernize-architecture

$ git log -1 --format="%h %ci %s"
3e233c0 2026-08-03 12:30:28 -0500 docs: record the S10-B close-out commit hash

$ git status --short
(empty)

$ git pull --ff-only
From https://github.com/Murphnd2/ams
 * [new tag]         v0.87.00   -> v0.87.00
Already up to date.

$ git tag --contains 1d4c284
v0.87.00
```

All four hard-stop conditions clear. ⚠️ **Note the tag arrived _with this pull_** — `v0.87.00` was not in the
local clone beforehand. This is the third-time-lucky version of the memory's standing warning: an un-fetched
local `git tag` lies. The check passed only because the pull preceded it.

---

## 2. Q1–Q5 answered

**Q1 — LOS-scoped sections, and yes, they render dynamic values.**
`ProposalSection` ([ProposalSection.java:11-56](../../src/main/java/net/superiorstate/ams/model/sales/offering/ProposalSection.java:11))
is a PSP-scoped config row with `section_type`, `html_content` (a `text` column), `scope` (`ALL`/`SCOPED`),
`sort_order`, `is_active`, and a `@ManyToMany` to `LOS` via `proposalsectionlos`
([:40-44](../../src/main/java/net/superiorstate/ams/model/sales/offering/ProposalSection.java:40)). **A section
is a config row Kevin creates**, not code — `/ProposalSettings`'s `createCustom`/`saveContent`/`setScope`
actions ([ProposalSettings.java:146-167](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalSettings.java:146),
[:120-131](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalSettings.java:120),
[:392-413](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalSettings.java:392)).
Rendered by `ViewProposal.doGet` ([:217-357](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:217))
— load active sections `ORDER BY s.sortOrder`, drop `SCOPED` sections whose linked LOS/Enhancement is not on
the proposal, then emit through `viewProposal.jsp`'s `c:choose` on `sectionType`
([:164-204](../../src/main/webapp/WEB-INF/view/sales/viewProposal.jsp:164)).
**The pivotal answer: content is stored HTML *and* it is dynamic** — `buildTokenMap`
([:446-497](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:446)) +
`replaceTokens` ([:500-509](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:500))
substitute `{{TOKEN}}` placeholders, and `CUSTOM` is one of the three types that gets this treatment
([:346-349](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:346)).
Adding a token is ~4 lines. This is what collapsed the Q5 fork.

**Q2 — "custom pages" are `CUSTOM` sections, and they are *not* agency-scoped.**
Same `proposal_section.html_content` column; the repo's own skill documents the target DOM position
(`.proposal-section.custom-section` inside a 900px `.proposal-container`,
[SKILL.md:23-30](../../.claude/skills/proposal-content-page/SKILL.md)). **It accepts exactly the shape Kevin
holds** — `sanitizeHtml` strips `<script>`, `on*` handlers and `javascript:` while **deliberately preserving
`<style>` blocks, inline styles and merge tokens**
([ProposalSettings.java:501-515](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalSettings.java:501)),
and the skill names `@import` inside `<style>` as the intended font-delivery mechanism
([SKILL.md:82](../../.claude/skills/proposal-content-page/SKILL.md)). Rendered **unescaped on the public link**
([viewProposal.jsp:198-202](../../src/main/webapp/WEB-INF/view/sales/viewProposal.jsp:198)).
❌ **The "agency-scoped" half of the premise is false:** `createCustom` never calls `setAgency`, and the
agency-override pass covers **only** `TITLE`/`CLOSING`
([ViewProposal.java:307](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:307),
[:317](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:317)).
`CUSTOM` is PSP-wide and LOS-scopable, never agency-scoped.

**Q3 — the gate collision does not exist; tier-1 is not blocked.**
`ichraEntitled` is resolved from the originating agency's `ichra_enabled` alone and **reads no snapshot**
([ViewProposal.java:109](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:109)).
The `source_env == PRODUCTION` test gates only whether the `ichraSnapshot` *request attribute* is set
([:114-120](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:114)), and the
JSP's `<c:if test="${not empty ichraSnapshot}">` wraps **only** the `ICHRA_ILLUSTRATION` branch
([viewProposal.jsp:186-192](../../src/main/webapp/WEB-INF/view/sales/viewProposal.jsp:186)). Gate half two
strips exactly one type, `Set.of(ProposalIchraSnapshot.SECTION_TYPE)`
([ViewProposal.java:54](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:54)).
A `CUSTOM`-typed tier-1 section passes untouched — **no gate change, no snapshot, no weakening of T116 or
LA-17.** ⚠️ **The inverse is the real finding:** `CUSTOM` is *also* not covered by the gate, so tier-1 renders
for unentitled agencies too, held closed only by rate-assignment reference data
([ProposalBuilder.java:70-79](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:70)).
Tolerable only while the content is market-data-free; filed as **T129**, its own scoped run.

**Q4 — tier-1 needs no new legal assumption.** LA-17 constraint 3
([legal_assumptions.md:1034-1039](../analysis/legal_assumptions.md)) already partitions the document into the
agent's market-data section and *"SSA's supplemental section [which] describes administration services only."*
**Tier-1 is that supplemental section**, so LA-17's thin carve-out is not load-bearing for it and LA-09 never
restricted it. ✅ **May echo the employer's own county and headcount** — geography and the employer's own
census, entered by the agent about the employer, not the result of any market query. ✅ May explain
ICHRA/QSEHRA generically and state the SSA administration fees already on the same document's pricing
summary. ❌ No rates, carriers, plan names or counts, premium ranges, affordability, ranking, curation,
recommendation, default, or enrollment path. **No `LA-NN` filed** — no call had to be made under uncertainty.
⚠️ The residual risk is phrasing, not data: the section can breach the boundary **without rendering a figure**
(`"we found coverage options in {{ICHRA_COUNTY}}"` asserts a market fact); prose review is a required step.

**Q5 — (a) now, (b) immediately after; they are not alternatives.**
(a) is **zero code**: Kevin creates a `CUSTOM` section scoped to the plus-tier LOS and pastes his HTML —
nothing in `src/` is touched, no migration. (b) is **~20 lines in one method** because of Q1's token finding —
four tokens from `ProposalIchraIntakeDAO.findByProposalId` (the DAO method already exists,
[ProposalIchraIntakeDAO.java:14-23](../../src/main/java/net/superiorstate/ams/data/dao/ProposalIchraIntakeDAO.java:14)),
no new section type, no entity, no JSP change, no gate change, no migration. Reversal: delete the section row,
or delete the token entries. **(b) is (a) plus tokens in the same row**, so Kevin's HTML can carry the
placeholders from day one — which is exactly why the spec makes shipping (b) before the content reaches a real
employer a hard requirement rather than a nicety.

---

## 3. Files written

| Path | New/existing |
|---|---|
| `docs/analysis/phase_a_tier1_proposal_section.md` | new — the spec |
| `docs/analysis/project_backlog.md` | existing — T126 annotated; T127, T128, T129 filed |
| `docs/runs/S10-D_closeout.md` | new — this file |

`docs/analysis/legal_assumptions.md` was **read but not written** — Q4 forced no new `LA-NN`.

`git status --short` immediately before staging:

```
 M docs/analysis/project_backlog.md
?? docs/analysis/phase_a_tier1_proposal_section.md
?? docs/runs/S10-D_closeout.md
```

**Nothing under `src/` and nothing under `docs/migrations/` appears — proven by the above, not asserted.**

---

## 4. ⚠️ Code-verified-only disclosure

**Everything in this run is read, not run.** No compile, no server, no browser, no database. What a runtime
walk would still have to confirm, specifically:

1. **That a `CUSTOM` section scoped to a plus-tier LOS actually appears on a real proposal.** The scope-pass
   logic was read, never exercised for this case.
2. **That a page-sized block with embedded `<style>` and an `@import` renders correctly inside the 900px
   `.proposal-container`** — and that `page-break-before` behaves in print. The skill asserts the container
   geometry; nobody has printed one from this position. This is spec §7 item 3.
3. **That `sanitizeHtml` leaves Kevin's specific blocks intact.** The regexes were read; his HTML has never
   been passed through them. A `<style>` block containing the substring `javascript:` in a comment, or an
   attribute matching the event-handler pattern, would be silently altered on save.
4. **That the four proposed tokens survive `replaceTokens`' case-insensitive regex** with real values —
   `Matcher.quoteReplacement` is used, so a county name with `$` is safe, but this is read, not tested.
5. **That an unentitled agency genuinely cannot reach the plus-tier LOS in the builder.** The rate→LOS
   filtering was read ([ProposalBuilder.java:96-107](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:96));
   the negative case has never been walked. **This is the whole of T129's fail-closed argument**, so it is the
   single most important unverified claim in the run.
6. **That `OriginatingAgencyResolver.resolve(proposal)` returns the agency one would expect** for a
   multi-membership agent — the unordered `list.get(0)` nondeterminism T116's javadoc already records is
   unchanged and unexercised here.
7. **That no other PSP's existing proposals change.** The argument is that this run adds one config row and
   (later) four map entries, neither of which alters any existing branch — read, not observed.

---

## 5. New assumptions, with reversal cost

- **Tier-1 may render for an unentitled agency, because it carries no market data.** Reversal: add the
  section type to `ICHRA_GATED_SECTION_TYPES` — one line — but that is the T116 gate, so it ships alone
  (**T129**). Cheap to reverse, deliberately not reversed here.
- **Reference-data gating (rate assignment) is sufficient protection for market-data-free content.**
  Reversal: same as above. ⚠️ **Basis is thin in one specific way** — it assumes `agencyrates` assignment and
  `agency.ichra_enabled` will be kept consistent by hand, and nothing enforces that.
- **Token fallbacks make a missing intake row a prose degradation rather than a defect.** Reversal: change
  four strings. Assumes the prose is drafted against the fallback wording, which is a content-authoring
  discipline, not a code guarantee.

**No `LA-NN` was filed.** Q4's boundary was clear enough that no call had to be made under uncertainty; the
reasoning is recorded in the spec §5 rather than promoted to the register.

---

## 6. Backlog rows filed or closed

Highest existing row was **T126** — see the correction below.

| T | Action |
|---|---|
| **T126** | **Annotated.** Superseded in practice by T128. One of its own premises falsified: its stated dependency on T122 does not hold for tier-1, because tier-1 carries no market data. Its "nothing reads it" claim re-confirmed true. |
| **T127** | **Filed — LOW.** ⚠️ **This is a correction.** `docs/runs/S10-B_closeout.md` §10 states S10-B filed T127. **It did not** — no such row existed and the highest row in the backlog was T126. The row now exists, carrying the plan-year-override content S10-B described, plus an explicit note that the close-out asserted a filing it never performed. **S10-B's §10 is left as written rather than edited** — it is evidence of the error, and silently backfilling it would erase exactly the signal worth keeping. |
| **T128** | **Filed — HIGH.** The tier-1 build, two-phase, pointing at the spec. Carries the token-fallback and prose-review hard requirements and the Q3 finding. |
| **T129** | **Filed — MED.** Whether the T116 gate should cover `CUSTOM` sections carrying plus-tier content. Extracted so it cannot become a quiet edit inside T128. Decide alongside T122. |

---

## 7. Note to Kevin — not work items

- **The tier-1 content itself does not exist in the repository.** Phase (a) is you pasting your `.icm1`/`.icm2`
  blocks into a new Custom Page; no code ships for it.
- **Put `{{ICHRA_COUNTY}}` / `{{ICHRA_HEADCOUNT}}` placeholders in that HTML now** — they render as literal
  text until T128 phase (b) ships, so the section should not go to a real employer until it does.
- **`createCustom` places a new section immediately before `CLOSING`** — reorder it in the admin UI if tier-1
  belongs elsewhere in the document.
- **The ZIP crosswalk is still Texas-only** (V084/V085) — a non-TX employer ZIP produces no intake row, so the
  tokens fall back to their generic wording and the section still reads correctly.
- **Whoever can select the plus-tier LOS can show a tier-1 section**, entitled or not — governed by which
  rates you assign to which agency, not by `ichra_enabled`. That is T129's question.

---

## 8. SQL close-out audit

- **SQL produced this run: NONE.** Explicitly: no `CREATE`, no `ALTER`, no `INSERT`, no migration body in the
  spec, and no `.sql` file created. The spec's §6 states outright that neither phase of T128 requires a schema
  change — `proposal_ichra_intake` (V087) and `proposal_section` (V036/V037/V044) already hold everything.
- **SQL run: none**, read or write.
- **In a versioned migration:** N/A — nothing was scripted.
- **Orphaned `.sql` files created: none.** Pre-existing and untouched:
  `docs/migrations/seed_ndt125_questionnaire.sql` (unversioned, pre-existing), `docs/schema_version_migration.sql`,
  `docs/updates/update_V039_to_V057.sql`, `docs/importscript/beta_ssa_baseline_v031.sql`,
  `docs/importscript/beta_ssa_dev_baseline_thru_V024.sql`.
- **Current highest version — `ls docs/migrations/`:** **V087** (`V087__proposal_ichra_intake.sql`), 64
  versioned scripts. Matches the run brief's expectation.
- **`SELECT MAX(version) FROM schema_version`: not run** — no `mysql` client in this container. Per the run
  brief, V087 is applied to production as of 2026-08-03; **that is Kevin's report, not something this run
  verified**, and the container's inability to check is a container limitation, not evidence about any
  environment.
- **Pending deployment from this run: nothing.** Phase (a) of T128 is a config row, not a deploy.
- **Schema described but not scripted: none.**

---

## 9. Commit

Read from `git log -1` **after** the push:

```
b732ee5 2026-08-03 13:21:05 -0500 docs: Phase A spec for the tier-1 proposal section (S10-D)
```

`git show --stat` on `b732ee5`:

```
 docs/analysis/phase_a_tier1_proposal_section.md | 191 ++++++++++++++++++++
 docs/analysis/project_backlog.md                |   6 +-
 docs/runs/S10-D_closeout.md                     | 290 ++++++++++++++++++++++++
 3 files changed, 486 insertions(+), 1 deletion(-)
```

⚠️ **Same disclosed deviation as S10-A and S10-B, for the same structural reason.** A close-out cannot both
record its own commit's hash and be inside that commit, so this section is filled in by a **second** commit
touching only `docs/runs/S10-D_closeout.md` — a path already inside the scope fence. Amending `b732ee5` was
rejected: it is already pushed, and project convention prefers a new commit over an amend.

---

## 10. Compliance statement

**Scope fence, restated.** Readable: anything. Writable: exactly
`docs/analysis/phase_a_tier1_proposal_section.md`, `docs/analysis/project_backlog.md`,
`docs/analysis/legal_assumptions.md` (only if Q4 forced a new `LA-NN` — **it did not, and the file was not
written**), and `docs/runs/S10-D_closeout.md`. **Nothing outside that set was written**, proven by the
`git status --short` in §3.

**Forbidden list, each observed:** no write under `src/` — none; no write under `docs/migrations/` — none;
no SQL of any kind, read or write — none run; `docs/ichra_strategy.md`, `docs/swbd_ichra_build_plan.md`,
`docs/analysis/plus_tier_build_plan.md` — untouched and never opened for writing; `GenerateProp25` and its
calling JSPs — untouched, not read this run; **the T116 gate and `proposal_ichra_snapshot` — not modified**
(both were *read* extensively, which is what produced §2-Q3; `ICHRA_GATED_SECTION_TYPES` is unchanged, and the
one-line addition the spec discusses is deferred to T129 rather than made); no `git add -A`/`git add .` —
every path staged by explicit name; no tag, stash, checkout, restore, or reset.

**No code was written this run.** Every Java, JSP and token fragment in the spec is a *description of a diff a
later run will make*, inside a Markdown document. No `.java`, `.jsp`, `.xml` or `.sql` file was created or
edited.

**Git operations run this session:** the five read-only preflight commands (`rev-parse`, `log`, `status`,
`pull --ff-only`, `tag --contains`), plus `status` before staging, and the close-out `add`/`commit`/`push`/`log`.
No other git operation of any kind.
