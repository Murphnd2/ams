# S10-F close-out — T129: plus-tier `CUSTOM` sections brought inside the entitlement gate

**Date:** 2026-08-03 · **Branch:** `refactor/modernize-architecture` · **Type:** security-relevant change to a live, customer-facing render path.
**Closes:** T129 (severity raised MED → HIGH on the way — see §8).

---

## 1. Preflight output, verbatim

```
$ git rev-parse --abbrev-ref HEAD
refactor/modernize-architecture

$ git log -1 --format="%h %ci %s"
434f445 2026-08-03 13:30:24 -0500 docs: record the S10-E close-out commit hash

$ git status --short
(empty)

$ git pull --ff-only
Already up to date.

$ git merge-base --is-ancestor 3b04198 HEAD && echo T128_PRESENT
T128_PRESENT
```

All hard-stop conditions clear.

---

## 2. Q1 — the current section-gating mechanism

**Definition:** [ViewProposal.java:56](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:56)
```java
private static final Set<String> ICHRA_GATED_SECTION_TYPES = Set.of(ProposalIchraSnapshot.SECTION_TYPE);
```
One element, `"ICHRA_ILLUSTRATION"`.

**Read in exactly one place** (`grep` over the file returns two hits: the definition and this):
[ViewProposal.java:297](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:297),
inside the `if (!ichraEntitled)` block, as a `.filter()` on a stream that rebuilds `sections`.

**What happens to a gated section: it is OMITTED from the model, not emitted and hidden.** The filtered
`sections` list is the one that goes on to build `sectionHtml`
([:345-352](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:345)) and the one
set as the `proposalSections` request attribute
([:354-355](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:354)). The JSP
iterates only that attribute. A gated section's HTML is therefore never rendered into the response at all —
**no content reaches the browser, so there is no hidden-in-the-DOM exposure to report.** Nothing extra to fix
in this run on that count.

---

## 3. Q2 — how a section reaches an LOS, and what it costs

**Path:** `ProposalSection.losList` is a `@ManyToMany` over the V037 join table —
`@JoinTable(name = "proposalsectionlos", joinColumns = @JoinColumn(name = "section_id"), inverseJoinColumns = @JoinColumn(name = "los_id"))`
([ProposalSection.java:40-44](../../src/main/java/net/superiorstate/ams/model/sales/offering/ProposalSection.java:40)).
`is_plus_tier` is a basic mapped column on the `LOS` entity itself
([LOS.java:48-49](../../src/main/java/net/superiorstate/ams/model/sales/offering/LOS.java:48), accessor
[:82](../../src/main/java/net/superiorstate/ams/model/sales/offering/LOS.java:82)), so it arrives with the LOS
row — no further navigation.

**Cost: zero additional queries.** The collection is **already force-initialised for every section**, before any
filtering runs:

```java
// Force-init M:N collections and agency for scope/override filtering
for (ProposalSection s : sections) {
    if (s.getLosList() != null) s.getLosList().size();   // <-- :230
    ...
}
```
([ViewProposal.java:228-233](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:228))

That loop exists today for the scope pass, which already walks `section.getLosList()`
([:258-265](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:258)). By the
time the entitlement gate runs at :295, every LOS is a managed entity in the persistence context. **The new
check walks objects already in memory and issues no SQL** — it adds nothing to the per-render query count on
this live customer-facing path.

---

## 4. Q3 — the four non-ICHRA `CUSTOM` sections, and why they cannot be caught

Answered from the model and schema alone. **No SQL was run to discover which rows exist.**

The new condition fires only if a section has **at least one associated LOS with `is_plus_tier = 1`**. Two
independent model-level facts make that unreachable for a non-ICHRA `CUSTOM` section:

1. **V086 shipped the column `NOT NULL DEFAULT 0` with no backfill** —
   [V086__los_plus_tier.sql:13-14](../../docs/migrations/V086__los_plus_tier.sql) states it outright: *"No
   backfill: every existing row comes out unflagged, so nothing anywhere changes when this is applied."*
   Flagging an LOS plus-tier is a deliberate per-row admin action through the Service Manager. **Every LOS a
   pre-existing section is scoped to therefore reports `plusTier == false`.**
2. **An `ALL`-scoped section has no LOS associations at all.** `updateScope` unconditionally clears both
   collections and only re-populates them on the `SCOPED` branch
   ([ProposalSettings.java:406-426](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalSettings.java:406)):
   ```java
   section.getLosList().clear();
   section.getEnhancementList().clear();
   if ("SCOPED".equals(scope)) { /* only here are associations added */ }
   ```
   So an `ALL`-scoped `CUSTOM` section hits the `sectionLos.isEmpty()` early return and is never gated.

**Both shapes a non-ICHRA `CUSTOM` section can take are covered:** `ALL`-scoped → empty list → `false`;
`SCOPED` to ordinary LOS → every `isPlusTier()` false → `false`. Either way the section is retained and
renders byte-identically to today.

---

## 5. The change

**Not** adding `CUSTOM` to `ICHRA_GATED_SECTION_TYPES` — that is the §1 trap, and it would strip the four
live non-ICHRA `CUSTOM` sections from proposals on every line of service that uses them.

**A second `.filter()` on the existing stream**
([ViewProposal.java:295-306](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:295)):

```java
if (!ichraEntitled) {
    sections = sections.stream()
            .filter(s -> !ICHRA_GATED_SECTION_TYPES.contains(s.getSectionType()))
            .filter(s -> !isPlusTierScoped(s))
            .collect(java.util.stream.Collectors.toList());
}
```

plus one private helper, `isPlusTierScoped(ProposalSection)`, placed immediately after `doGet`. It returns
`true` iff some LOS reached through `proposalsectionlos` has `isPlusTier()`; an empty/absent association is a
**determinate** `false` (this is what protects the existing sections), while **any exception is `true`** — omit
on uncertainty. It never throws, so the proposal cannot fail to render because the check failed.

**No id literal of any kind** — plus-tier is whatever `los.is_plus_tier` says, per PSP, which is exactly why
V086 was a column on `los` rather than a `constant` row naming LOS ids.

**Deliberately keyed on association, not type**, so it also covers a plus-tier-scoped `TITLE`/`CLOSING`
section should one ever be configured — the gate does not depend on the content happening to be typed `CUSTOM`.

---

## 6. Verification

**1. Build.** `.\mvnw.cmd clean package` → **BUILD SUCCESS**, 509 source files, WAR assembled. The only
compiler note is the pre-existing unchecked-operations warning in `RecurringChecklistDAO.java`, untouched by
this run.

**2. The four cases, with the code path for each cell.**

| Section | Entitled agency | Unentitled agency |
|---|---|---|
| **plus-tier `CUSTOM`** | **renders** | **OMITTED** ⬅ the security fix |
| **non-plus-tier `CUSTOM`** | **renders** | **renders** ⬅ the regression risk |

- **plus-tier + entitled → renders.** `ichraEntitled` is `true`
  ([:109](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:109)), so the
  `if (!ichraEntitled)` block at :295 **is not entered at all**. `isPlusTierScoped` is never called; the list is
  not rebuilt. Byte-identical to before this change.
- **plus-tier + unentitled → OMITTED.** Block entered; `isPlusTierScoped` walks the force-initialised
  `losList`, finds an LOS with `isPlusTier() == true`, returns `true`; `.filter(s -> !isPlusTierScoped(s))`
  drops it. The section is absent from `sections`, therefore absent from both `sectionHtml` and the
  `proposalSections` attribute ([:345-355](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:345)),
  therefore never rendered by the JSP's `c:forEach`. **This is the fix.**
- **non-plus-tier + entitled → renders.** Block not entered. Unchanged.
- **non-plus-tier + unentitled → renders.** Block entered, but `isPlusTierScoped` returns `false` — either at
  the `sectionLos == null || sectionLos.isEmpty()` early return (`ALL`-scoped, associations cleared by
  `updateScope`) or after the loop finds no `isPlusTier()` LOS (`SCOPED` to ordinary LOS, all `false` because
  V086 had no backfill). `.filter(s -> !false)` retains it. **This is the cell that must not regress, and §4
  is the argument that it cannot.**

**3. Runtime walk: NONE PERFORMED.** No Tomcat instance is reachable from this container and no WAR was
deployed anywhere. **This is a container limitation, not evidence about any environment.** See §7.

---

## 7. ⚠️ Code-verified-only disclosure

Specific, not "everything". The build compiled; nothing was executed.

1. **The unentitled-omission case — the security fix itself — has never been observed.** No code reading can
   confirm it. **The walk that would:** flag an LOS `is_plus_tier=1`; create a `CUSTOM` section `SCOPED` to it
   with recognisable marker text; build a proposal carrying that LOS from an agency with
   `agency.ichra_enabled = 0`; open the public `/proposal/{guid}` link; confirm the marker text is **absent
   from `view-source:`**, not merely invisible on screen. Then flip `ichra_enabled = 1`, **restart Tomcat**
   (T64 — EclipseLink can serve a stale `Agency` after a raw flag flip), reload, and confirm it appears. Both
   halves are needed: the first proves the gate closes, the second proves it does not over-close.
2. **That the four non-ICHRA `CUSTOM` sections still render for an unentitled agency.** §4 is a model
   argument from V086's no-backfill guarantee and `updateScope`'s clear-then-repopulate. Both were read; no
   row was inspected, per §4's own instruction not to run SQL.
3. **That `getLosList()` is genuinely initialised at the point `isPlusTierScoped` runs.** Read from the
   force-init loop at :228-233 executing before :295 in the same method with the same open `EntityManager`.
   Not observed. If it were ever wrong, the `catch` returns `true` and the section is omitted — the failure
   direction is safe, but it would silently over-gate.
4. **That no other PSP's proposals change.** Reasoned from the filter being inside `if (!ichraEntitled)` and
   from `isPlusTierScoped` returning `false` for every section with no plus-tier association. Not observed
   against a real proposal.
5. **`.\mvnw.cmd clean package` confirms compilation, not behaviour.** No JSP was touched this run, so the
   render path itself is unexercised.

---

## 8. Backlog

Highest existing row was **T129**; no new T-number was needed.

| T | Action |
|---|---|
| **T129** | **CLOSED**, and **severity raised MED → HIGH** on the way. S10-D's tolerance condition was *"only while tier-1 content stays market-data-free"*; the next build removes that condition by putting HealthSherpa-derived premium floors, plan counts and carrier counts into the same section. Detail cell records the trap avoided, the discriminator chosen, the zero-query finding, the two model-level reasons existing sections cannot be caught, and the code-verified-only status. |

No new rows filed — nothing out-of-scope was noticed that needed one.

---

## 9. Note to Kevin

**Yes — this needs its own release, and it must go out *before* the market-data section is configured**, not
alongside it: the gate has to exist before there is anything behind it worth gating. It bundles cleanly with
the two already-unreleased items on trunk (T128's `{{ICHRA_*}}` tokens, `3b04198`; session 9's six admin
guards, `820d027`), so one release picks up all three.

---

## 10. SQL close-out audit

**This run produced, ran, and recommends no SQL.** Explicitly: no `.sql` file created, edited or deleted; no
DDL; no DML; **no read query either** — Q3 was answered from the entity model and the migration script rather
than by inspecting rows, exactly as §4.Q3 required. No migration was created and none is needed: the change
reads `los.is_plus_tier`, which V086 already shipped.

**Current highest version, `ls docs/migrations/*.sql`: 64 files, highest `V087__proposal_ichra_intake.sql`** —
unchanged by this run, matching the expectation. Applied to production 2026-08-03 per Kevin's report; **this
run did not and could not verify that** — no `mysql` client is reachable from this container, a container
limitation and not evidence about any environment's state.

---

## 11. Commit

Read from `git log -1` **after** the push:

```
b3915d4 2026-08-03 13:49:47 -0500 fix: gate plus-tier CUSTOM proposal sections behind ICHRA entitlement (T129)
```

⚠️ **Same disclosed deviation as S10-A/B/D/E:** a close-out cannot contain its own commit's hash and also be
inside that commit, so §11 and §13 are filled in by a **second** commit touching only
`docs/runs/S10-F_closeout.md` — a path already inside the fence — rather than by amending a pushed commit.

---

## 12. Compliance statement

**Scope fence, restated.** Writable: `ViewProposal.java`, `docs/analysis/project_backlog.md`,
`docs/runs/S10-F_closeout.md`. **Nothing outside it was written** — `git status --short` before staging showed
exactly those three and nothing else (§13).

**`IchraAccessResolver` was called, not modified.** This run reads `ichraEntitled`, the boolean already
resolved once per request at
[:109](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:109) by the
pre-existing `IchraAccessResolver.isAvailableForProposal(em, proposal)` call. **That call site is unchanged and
`IchraAccessResolver.java` was not opened for writing** — no new overload, no signature change, no behaviour
change.

**Non-plus-tier `CUSTOM` sections are unaffected**, and the code path that proves it: `isPlusTierScoped`
returns `false` at either the `sectionLos == null || sectionLos.isEmpty()` early return or after the loop
finds no `isPlusTier()` LOS; `.filter(s -> !isPlusTierScoped(s))` therefore retains the section, and the rest
of the pipeline is untouched. The two model-level guarantees behind that are V086's `NOT NULL DEFAULT 0` with
**no backfill** ([V086:13-14](../../docs/migrations/V086__los_plus_tier.sql)) and `updateScope`'s
clear-then-repopulate-only-if-SCOPED
([ProposalSettings.java:406-426](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalSettings.java:406)).

**`proposal_ichra_snapshot`, `source_env`, and the `ICHRA_ILLUSTRATION` branch's existing logic are
unchanged** — the first `.filter()` on `ICHRA_GATED_SECTION_TYPES` is byte-identical, and `Set.of(...)` at :56
was **not** modified. The diff is **53 insertions, 0 deletions** — a pure addition; nothing existing was
altered or removed.

**No migration was created and no SQL was run** (§10).

**Git operations this session:** the five read-only preflight commands, `status`/`diff --stat` while working,
and the close-out `add`/`commit`/`push`/`log`. No `git add -A`, no tag, no stash, checkout, restore or reset.

---

## 13. `git show --stat`

On `b3915d4`, after the push — **proving no file outside the fence was touched**:

```
 docs/analysis/project_backlog.md                   |   2 +-
 docs/runs/S10-F_closeout.md                        | 282 +++++++++++++++++++++
 .../controller/activity/setup/ViewProposal.java    |  53 ++++
 3 files changed, 336 insertions(+), 1 deletion(-)
```

Three files, all named in §3's permitted list. `ViewProposal.java` is **+53 / −0** — a pure addition, so
nothing pre-existing in the render path was altered or removed. The single deletion in the changeset is the
one rewritten table row in `project_backlog.md`.
