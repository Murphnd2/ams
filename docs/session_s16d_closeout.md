# S16-D close-out — T150 build (step-6 demo override) + walk-result docs appendix

**Date:** 2026-08-05 · **Branch:** `refactor/modernize-architecture` · **Model:** Opus
**Preflight HEAD:** `a0e9d11d2633b8bd422cf12e629d4244f3bf628c`

> This close-out does not record its own commit hash. That is the convention now — the docs commit
> carries this file, and a second commit purely to record its own hash keeps the log noisy for no
> reader's benefit.

---

## 1. Shipped

| Commit | Contents |
|---|---|
| `86e41778bae3eb8e28eb8c522e57ad13f285cca3` | The build — `AppConfig`, `IllustrationServlet`, `illustration25.jsp`, `ProposalBuilder` |
| *(this commit)* | `docs/analysis/project_backlog.md` (six named rows) + this close-out |

Executed `docs/analysis/phase_a_t150_demo_override.md` §6.1–§6.4. §6.5's out-of-scope list held: no
`ViewProposal` change, no `RateCacheDAO` change, no migration, no DB constant.

**The gate as shipped:** `ICHRA_DEMO_ALLOW_STAGING_PROPOSAL=true` in `ssa.properties` **AND** a
PSP-admin session. Both, always. Absence of the property is OFF.

---

## 2. What was anchored on

**Stop #3 (anchor ambiguity) — evaluated, did not fire.** Every gate located semantically, not by
Phase A's line numbers.

**G1 — `illustration25.jsp:992`, AGE_BAND hand-off:**
```jsp
<c:when test="${sourceEnv == 'PRODUCTION'}">
    <a href="${proposalHandoffUrl}" class="ssa-action save" id="ichraProposalLink">
        <i class="bi bi-file-earmark-plus me-1"></i>Use This in a Proposal
```

**G2 — `illustration25.jsp:1404`, RANGE hand-off:** identical, minus the `id` attribute.

⚠️ **The naive pattern matches four times, not two.** `:964` and `:1384` carry the *same test
expression* on the source-label banners:
```jsp
<c:when test="${sourceEnv == 'PRODUCTION'}"> &middot; Source: production</c:when>
```
These are the display-only banners Phase A §2 lists separately and D4 protects. They are
unambiguously distinguishable — the gates have a block body, the banners have inline text and close
on the same line — and **both were left untouched.** Match count for the *gates* is exactly two, as
Phase A recorded.

**G3 — `ProposalBuilder:598`, `attachRangeSnapshot`:**
```java
// Fail closed — no PRODUCTION-sourced data backing this range, no snapshot.
if (!RatingAreaRateCache.SOURCE_ENV_PRODUCTION.equals(sourceEnv)) return;
```

**G4 — `ProposalBuilder:658`, `attachAgeBandSnapshot`** (per-row, inside the census loop):
```java
if (row == null || row.getLowestBronzePremium() == null) return;
if (!RatingAreaRateCache.SOURCE_ENV_PRODUCTION.equals(row.getSourceEnv())) return;
```
Exactly two `SOURCE_ENV_PRODUCTION` guards in the file, both with `return`. Count matches Phase A.

### Stop #1 — `ProposalBuilder` request reachability: **did not fire**

```
557: private void attachIchraSnapshotIfPresent(HttpServletRequest request, EntityManager em, Proposal proposal, Person createdBy)
579: private void attachRangeSnapshot(HttpServletRequest request, EntityManager em, Proposal proposal, Person createdBy,
                                      CountyReference county, int planYear, String countyFips)
629: private void attachAgeBandSnapshot(HttpServletRequest request, EntityManager em, Proposal proposal, Person createdBy,
                                        CountyReference county, int planYear, String countyFips)
```

**Both snapshot writers already take `HttpServletRequest request` as their first parameter.** The
override is re-evaluated in place from the request each already holds — Phase A §6.4 explicitly
offered this as the alternative to threading a value down. **No method signature in `ProposalBuilder`
changed.** That mattered: this class is on every proposal-creation path for every line of service.

---

## 3. Investigation outputs

### `AppConfig.get()` — cached at startup. **A Tomcat restart is required.**

```java
private static final Properties props = new Properties();
private static boolean loaded = false;

public static synchronized boolean load() {
    if (loaded) return !props.isEmpty();   // ← reads the file at most once, ever
    ...
    props.load(in);
}

public static String get(String key) {
    return props.getProperty(key);          // ← in-memory only, never touches disk
}
```

**Answer for the demo, stated plainly: adding `ICHRA_DEMO_ALLOW_STAGING_PROPOSAL=true` to
`ssa.properties` does nothing until Tomcat is restarted.** `load()` short-circuits on `loaded` and
`get()` only ever reads the in-memory `Properties`. There is no reload path and none was added.

### Every path forwarding to `illustration25.jsp` — thirteen, all covered

All thirteen `forward()` calls live in `IllustrationServlet`; no other servlet forwards to this JSP
(`GroupConversionServlet`'s match is a comment).

| Method | Forward lines |
|---|---|
| `doGet` (82–252) | 144, 208, 217, 223, 237 |
| `handleRangeMode` (253–318) | 261, 309 |
| `handleAgeBandMode` (319–522) | 353, 359, 367, 402, 423, 507 |

**`ichraDemoOverride` is set once in `doGet` immediately after `opportunityId`, before the first
forward at `:144`.** Both mode handlers are invoked from below that point, so a single assignment
dominates all thirteen — verified by method-boundary mapping, not assumed.

⚠️ **This departs from Phase A §6.2's suggested placement, and the departure is the point.** Phase A
said to put it *"next to the existing `sourceEnv` attribute plumbing (`setProvenanceAttributes`)."*
**`setProvenanceAttributes` is not called on every path** — it runs only when results exist — so that
placement would have missed the early returns, including the no-plan-years forward at `:144`. The
`opportunityId` line was used instead because its own comment states exactly the required property:
*"Resolved once here so every forward path below (including the early returns) carries it."* A missed
path fails closed (button stays disabled), which is safe but would have looked like a data problem
rather than a plumbing one.

---

## 4. Verification status

**`code-verified`.** Named per claim:

| Claim | Level |
|---|---|
| Compiles and packages | **Verified** — `.\mvnw.cmd clean package` → `BUILD SUCCESS`, 02:33, WAR written |
| Two JSP gates changed, banners untouched | **Code-verified** — read before and after; both banner blocks byte-identical |
| No `ProposalBuilder` signature changed | **Code-verified** — signatures printed above |
| `source_env` stamp stays honest | **Code-verified** — `setSourceEnv` at `:650`/`:732` receives the variable; the only assignments are `row.getSourceEnv()` at `:617`/`:714`. No literal anywhere |
| Attribute reaches all 13 forwards | **Code-verified** — method-boundary mapping |
| The button actually enables under the flag | **NOT verified** — requires a running instance with the property set |
| A snapshot row actually writes | **NOT verified** — see §10 |

**Port 8089: no local Tomcat reachable — a no-server condition, not T111.** Three independent
signals agreed: `Test-NetConnection` returned `TcpTestSucceeded: False`, `netstat -ano` showed no
`:8089` entry, and `Get-Process java` found no process at all. **This is the third consecutive run
to find this** (S16-A, S16-B, S16-D). Per instruction, Tomcat was not started and no run
configuration was touched. **This is explicitly not T111** — see §5 and the T111 row correction.

---

## 5. Decisions made

1. **Attribute placement moved from `setProvenanceAttributes` to the `opportunityId` line** (§3).
   Phase A's suggestion did not satisfy Phase A's own requirement.
2. **Re-evaluate the override in place rather than thread it down** — the option Phase A offered
   that avoids touching a signature in live shared code.
3. **Hoisted the override resolution out of `attachAgeBandSnapshot`'s per-row loop.** The answer
   cannot vary between rows; per-row cost stays identical to before.
4. **Left the `row == null || getLowestBronzePremium() == null` check untouched** in G4. That is a
   *data-completeness* check, not a provenance check — a missing row means there is no figure to
   record at all, and the override has no business loosening it. Only the provenance line changed.
5. **Created the T153 backlog row rather than editing a severity that had no row** — see §8.

---

## 6. New assumptions

**Asking the required question first: did this change make any failure mode worse, even where it
made the success path better?**

**Yes — one, and it is worth stating plainly rather than filing under "none."**

**A1 — The override widens what a compromised or careless PSP-admin session can produce.**
Before this change, *no* session could cause a staging-sourced snapshot row to exist; the guard was
absolute. Now a PSP admin on an installation with the property set can create proposal rows carrying
staging figures. **What bounds it:** the row is stamped `STAGING` honestly, so `ViewProposal:142`
refuses it on the public path permanently — the blast radius is database rows and the internal
proposal view, never an employer-facing figure. **Reversal cost:** delete the two `!demoOverride &&`
conjuncts in `ProposalBuilder` and the two `or ichraDemoOverride` disjuncts in the JSP — four edits,
no schema, no data migration. Rows already written stay refused by G5 regardless.

**A2 — The two-condition gate is duplicated, not centralised.** `IllustrationServlet` and
`ProposalBuilder` each carry their own `isIchraDemoOverride`. They can drift. **Deliberate:** Phase A
established that the obvious shared home, `RateCacheDAO.check()`, is also consumed by
`IchraZipLookup`'s advisory and `IllustrationServlet`'s county dropdown, so centralising there would
have changed two surfaces nobody asked to change. **Reversal cost:** extract to a small policy class
when a third caller appears; two call sites do not yet justify one. Filed as **T156**.

---

## 7. Open questions

1. **Does the snapshot write actually succeed end to end?** This is the run's real payload — those
   writers have never executed successfully on any installation — and it remains unexercised. It
   needs a live instance with the property set and a PSP-admin session.
2. **Should the override log when it fires?** Nothing currently records that a snapshot was written
   under override. The `source_env` stamp makes it *discoverable* after the fact, but no log line
   says *"this row exists because the demo flag was on."* Not built; not obviously needed while the
   flag is off everywhere.
3. **Does `ProposalBuilder` need the override at all if the button is the only sender?** The two
   guards are defence in depth against a hand-crafted POST. Kept deliberately, but it does mean two
   places must agree.

---

## 8. Contradictions found

⚠️ **1. T153 has no row in `project_backlog.md`.** The run prompt instructed *"T153 | MED → LOW"*,
which presupposes an existing row. `grep -c "T153" docs/analysis/project_backlog.md` returned **0** —
T153 was filed in `docs/session_s16b_closeout.md` and **never written into the backlog table.**
**Resolution: the row was created at LOW**, carrying the browser-blocked reasoning, and states
explicitly that it was filed into the table for the first time this run at its corrected severity.
Doing nothing would have silently lost the finding; stopping the run over a missing row would have
been disproportionate. **Flagged here because "change the severity" and "create the row" are not the
same act, and Kevin asked for the former.**

**2. Phase A §6.2's suggested placement does not satisfy Phase A's own requirement.** Detailed in
§3. Resolved by using the `opportunityId` precedent instead.

**3. Phase A §6.2 and §6.4 both write `request.getSession()`.** The run prompt's Stop #2 corrected
this to `getSession(false)` with a null check — a feature check must not create a session. **Applied
at both sites.** Recording it because the Phase A text still carries the creating form and a future
reader following it verbatim would reintroduce the problem.

**4. Phase A's line numbers held exactly** — 992, 1404, 598, 658 all still correct. No drift, despite
the caution. Worth recording as a case where the anchors *did* survive.

---

## 9. Noticed and deliberately not fixed

`grep -rn "T15[0-9]" docs/` run before assigning. Matches found up to **T155**
(`docs/session_s16b_closeout.md:76`). High-water mark confirmed; **next available was T156.**

- **T156 (new, LOW)** — the two-condition demo-override predicate is duplicated in
  `IllustrationServlet.isIchraDemoOverride` and `ProposalBuilder.isIchraDemoOverride`. Identical
  today, free to drift. Not centralised because the natural shared home (`RateCacheDAO.check()`) is
  consumed by two unrelated surfaces — see A2. Extract when a third caller appears.

**Not assigned a number, but noticed:** `IllustrationServlet` now has thirteen forward points to one
JSP. That is a lot of exits for one method to hold, and it is why the attribute-placement question
in §3 was delicate at all. Not a defect; a structural observation that would matter to anyone
refactoring `doGet`.

---

## 10. Code-verified-only disclosure

**Asserting only what actually happened this run.**

- The build **compiled and packaged**. That was observed.
- **No page was rendered.** No browser, no HTTP request, no running server — port 8089 had no
  listener and the machine had no `java` process.
- **The override has never been observed to fire.** `ICHRA_DEMO_ALLOW_STAGING_PROPOSAL` is set in no
  properties file anywhere, by design; the code path taken when it is `true` has been read, not run.
- ⭐ **The snapshot write path is still unexercised.** `attachRangeSnapshot` and
  `attachAgeBandSnapshot` have never successfully written a row on any installation — that is why
  this was built — and **this run did not change that.** It made the write *possible*; it did not
  demonstrate it. Anyone reading this as "the snapshot write now works" is reading it wrong.
- The production-walk results recorded in §11's backlog rows (T142, T143, T138) were **performed by
  Kevin against `v0.88.06`** and supplied to this run. They were transcribed, not observed here.

---

## 11. SQL close-out audit

**No SQL of any kind was produced, recommended, or executed this run.** No migration, no DDL, no
DML, no `INSERT INTO constant`, no `DatabaseInitializer` seed. The demo flag is an `ssa.properties`
entry; its absence is the OFF state.

**Highest migration read from `docs/migrations/` this run: `V088__proposal_ichra_intake_contribution.sql`.**
`git status --short docs/migrations/` returned empty — **migrations unchanged.**

---

## 12. Compliance statement

**Paths written — six, all inside the fence:**
`src/main/java/net/superiorstate/ams/AppConfig.java` ·
`src/main/java/net/superiorstate/ams/controller/market/IllustrationServlet.java` ·
`src/main/webapp/WEB-INF/view/market/illustration25.jsp` ·
`src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java` ·
`docs/analysis/project_backlog.md` · `docs/session_s16d_closeout.md`

`git diff --name-only` for the code commit returned exactly the four source paths; for the docs
commit, exactly the two docs paths.

**Nothing forbidden was written.** No `ViewProposal`, no `RateCacheDAO`, no `IchraAccessResolver`,
no `IchraZipLookup`, no `GroupConversionServlet`, no `groupConversion25.jsp`, no `AmsDataGlobal`, no
`EmfListener`, no `LoginFilter`, no `navbar25.jsp`, no `css-js.jsp`, no `.sql`, no
`docs/migrations/`, no `DatabaseInitializer`, no `docs/ichra_strategy.md`, no
`docs/swbd_ichra_build_plan.md`, and no banner listed in Phase A §2.

**Hard stops — all six evaluated, none fired:**

| Stop | Outcome |
|---|---|
| **#1** request reachability | **Did not fire.** Both writers already take `request`; no signature changed |
| **#2** session creation | **Did not fire — actively applied.** `getSession(false)` + null check at both sites, correcting Phase A |
| **#3** anchor ambiguity | **Did not fire.** Gate counts matched (2 and 2); the two extra JSP matches are banners, distinguished and untouched |
| **#4** honest stamp | **Did not fire.** `setSourceEnv` receives the real variable; no literal `PRODUCTION` anywhere |
| **#5** `AppConfig` shape | **Did not fire.** `get(String)` is exactly as assumed; appended only |
| **#6** build failure | **Did not fire.** `BUILD SUCCESS` |

**Preflight** passed on all four conditions plus the ancestry check
(`git merge-base --is-ancestor a0e9d11… HEAD` → exit 0).

**Every hash was read from `git log` this run.** No forbidden git operation ran — no `add -A`,
`add .`, `add -u`, `stash`, `checkout`, `restore`, `reset`, `rebase`, `tag`, `branch`, or
force-push. **Commit count: two.**

---

## 13. Next

1. ⭐ **Exercise the snapshot write.** Set `ICHRA_DEMO_ALLOW_STAGING_PROPOSAL=true` in
   `ssa.properties` on the demo installation, **restart Tomcat** (§3), and walk
   `/Illustration` → *Use This in a Proposal* → create. Confirm a `proposal_ichra_snapshot` row
   appears with `source_env = 'STAGING'`. **That is the first successful execution of that path on
   any installation** and is the thing most worth watching.
2. **Confirm the proposal renders without the ICHRA section** — the accepted D1 limitation, and the
   check that proves G5 still holds.
3. **Release `v0.88.07`** — Java + JSP only, `ROOT.war`, no `.sql`. Tag created by Kevin in the web
   UI, not from local git.
4. **T153** remains the cheapest open defect on `/GroupConversion`, now correctly filed and
   downgraded to LOW.
