# S10-E close-out — T128: ICHRA intake tokens for `CUSTOM` proposal sections

**Date:** 2026-08-03 · **Branch:** `refactor/modernize-architecture` · **Type:** Implementation from a settled spec (phase b only).
**Authority:** [docs/analysis/phase_a_tier1_proposal_section.md](../analysis/phase_a_tier1_proposal_section.md) (S10-D, `b732ee5`/`ddad05d`). Read in full before any code was written.

---

## 1. Preflight output, verbatim

```
$ git rev-parse --abbrev-ref HEAD
refactor/modernize-architecture

$ git log -1 --format="%h %ci %s"
ddad05d 2026-08-03 13:21:19 -0500 docs: record the S10-D close-out commit hash

$ git status --short
(empty)

$ git pull --ff-only
Already up to date.

$ git merge-base --is-ancestor ddad05d HEAD && echo SPEC_PRESENT
SPEC_PRESENT
```

All hard-stop conditions clear.

---

## 2. Final token names, and what they were named to match

**New tokens, exactly as an HTML author would type them:**

```
{{ICHRA_COUNTY}}
{{ICHRA_COUNTY_FIPS}}
{{ICHRA_HEADCOUNT}}
{{ICHRA_PLAN_YEAR}}
```

**Existing tokens already in `buildTokenMap`**, printed per §3.1's instruction:
`PROSPECT_NAME`, `AGENT_NAME`, `AGENT_EMAIL`, `AGENCY_NAME`, `PSP_NAME`, `DATE_CREATED`, `PRIMARY_COLOR`,
`ACCENT_COLOR`, `PROPOSAL_ID`, `APPLY_BUTTON` — all `{ENTITY}_{ATTRIBUTE}`-shaped, all-caps, underscore-joined
([ViewProposal.java:450-494](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:450)).

**Convention match, not a hard-stop.** `ICHRA_` was not invented for this run — it is already the established
domain prefix throughout the codebase (`ICHRA_GATED_SECTION_TYPES`
[ViewProposal.java:54](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:54),
`IchraAccessResolver`, `ICHRA_AFFORDABILITY_PCT_2026`
[DatabaseInitializer.java:929](../../src/main/java/net/superiorstate/ams/data/service/DatabaseInitializer.java:929)),
and it reads exactly like `AGENT_`/`PSP_`/`AGENCY_` as an entity-grouping prefix. `ICHRA_PLAN_YEAR`'s
three-segment shape has precedent too — no existing token is three segments, but nothing in the convention
caps it at two, and the spec itself already proposed that exact name. No naming ambiguity was found; no
hard-stop was triggered.

⚠️ **Deviation from the S10-D spec's own table, on this run's explicit instruction.** The spec listed
`{{ICHRA_COUNTY}}`/`{{ICHRA_STATE}}`/`{{ICHRA_HEADCOUNT}}`/`{{ICHRA_PLAN_YEAR}}`. The S10-E prompt's §3.1
named a different four explicitly — *"county name, county FIPS, headcount, plan year"* — and separately
instructed that `zip` and `state` be added *"only if the existing convention makes it obvious and free; do
not add tokens speculatively."* Per the run brief's own precedence rule (*"On a decision, this prompt wins"*),
and because *which fields to expose* is a decision, not a fact about the repository, this run built the
prompt's four rather than the spec's four. Neither `zip` nor `state` was judged obvious-and-free — echoing a
county name alone reads correctly in prose without a state abbreviation, and adding a token nobody asked to
use is exactly what "do not add speculatively" forbids — so **`{{ICHRA_STATE}}` does not exist**, and
`{{ICHRA_COUNTY_FIPS}}` exists in its place. **This is flagged explicitly to Kevin in §7**, since it is the
one place this run's output differs from what the spec told him to expect.

---

## 3. `replaceTokens` unmatched-token finding

Read, not changed. [ViewProposal.java:499-509](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:499)
(pre-existing method, only line numbers shifted by this run's insertion above it):

```java
private String replaceTokens(String html, Map<String, String> tokens) {
    if (html == null) return "";
    String result = html;
    for (Map.Entry<String, String> entry : tokens.entrySet()) {
        String pattern = "(?i)\\{\\{" + Pattern.quote(entry.getKey()) + "\\}\\}";
        result = result.replaceAll(pattern, Matcher.quoteReplacement(entry.getValue()));
    }
    return result;
}
```

**Finding: it iterates the map's own keys and substitutes each in turn. A token with no map entry is never
matched by any iteration, so it is left as the literal `{{TOKEN_NAME}}` string in the output.** There is no
fallback branch, no default, no removal pass for unmatched `{{...}}` syntax. Confirms the spec's §3.2 premise
exactly.

**Per §3.2's instruction, `replaceTokens` itself was not touched** — changing its unmatched-key behavior
globally would alter every existing token (`PROSPECT_NAME`, `AGENT_NAME`, …) for every existing line of
service, which is explicitly out of scope. Instead, the fix is entirely on the write side: all four
`ICHRA_*` keys are `put()` into the map **unconditionally**, in every code path, including when no intake row
exists — so `replaceTokens` always finds a match and the unmatched-literal branch is never reached for these
keys, without changing its behavior for anyone else's tokens.

---

## 4. Diff — file, size, reason

**`src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java` only.** 24 insertions, 2
deletions (`git diff --stat`).

**Reason, in three pieces:**
1. **Imports** (+2 lines): `ProposalIchraIntakeDAO` and `ProposalIchraIntake` — the two the scope fence
   explicitly anticipated and no others.
2. **Method signature + one call site** (2 lines changed): `buildTokenMap` gained a leading `EntityManager em`
   parameter; its single caller ([:340](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:340))
   was updated to pass the already-open `em` local. The spec offered two equally-acceptable approaches for
   getting an `EntityManager` into the method; this is the smaller of the two (one signature change plus one
   call-site change, versus resolving the intake earlier in `doGet` and threading an extra parameter through).
3. **Token block** (+20 lines): a best-effort `ProposalIchraIntakeDAO.findByProposalId` lookup wrapped in
   `try`/`catch` (falls through to `null` on any failure, matching `ProposalBuilder.attachIchraIntakeIfPresent`'s
   sibling pattern for the same table), then four unconditional `tokens.put(...)` calls with the
   `intake != null && getter() != null` ternary already used by every other conditional token in this method
   (compare `AGENT_NAME`'s `agent != null` branch immediately above).

**Nothing else in the file was touched.** `ICHRA_GATED_SECTION_TYPES`, `isAvailableForProposal`, the
`ichraSnapshot`/`ichraBands` block, and the section-filtering passes are all byte-identical to before this run.

---

## 5. `git show --stat`, proving the fence held

```
$ git status --short
 M src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java
```

Only the one permitted file changed in `src/`. (Backlog and close-out staged separately at commit time — see §9.)

---

## 6. ⚠️ Code-verified-only disclosure

Specific, not "everything":

1. **That `ProposalIchraIntakeDAO.findByProposalId`'s internal `getSingleResult()` cannot throw
   `NonUniqueResultException` here** — it relies on `proposal_ichra_intake.uq_pii_proposal` (V087) actually
   holding as a real database constraint. The `try`/`catch` added in this run covers that possibility
   defensively, but the constraint's presence on any live schema was never queried.
2. **That the four tokens render correctly inside Kevin's actual HTML** — never exercised, because the HTML
   does not exist in the repository yet (spec §7 item 1, unchanged).
3. **That a proposal with no `CUSTOM` section, or no ICHRA-scoped `CUSTOM` section, is unaffected** — reasoned
   from the fact that `buildTokenMap` is only ever called from inside `if (!sections.isEmpty())`
   ([ViewProposal.java:338](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:338))
   and that adding map entries cannot change any *other* key's resolution; never observed against a real
   proposal without the new tokens in its HTML.
4. **That the `EntityManager` passed to `buildTokenMap` is still open and valid at the point it's used** —
   true by inspection (the call site is inside the same `try` block as `em`'s acquisition, well before the
   `finally` at [:359](../../src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:359)
   closes it), never confirmed by running the servlet.
5. **`.\mvnw.cmd clean package` succeeding confirms compilation, not JSP or runtime behavior** — no JSP was
   touched by this run, so this caveat is narrower than it was in S10-B, but it is still true that Maven does
   not exercise `viewProposal.jsp`'s rendering of these tokens.

---

## 7. New assumptions, with reversal cost

- **`{{ICHRA_STATE}}` was deliberately not built**, per §2's deviation. Reversal cost: one more `tokens.put()`
  line and one more getter call on the existing `ichraIntake` object already in scope — trivially cheap,
  should the state abbreviation turn out to be needed in the prose.
- **A DAO lookup failure degrades to empty-string tokens rather than surfacing anywhere.** Reversal cost:
  none needed — this is the explicitly required fail-closed behavior (spec §3.3), not a judgment call this
  run made independently.
- **The `System.out.println` logging idiom (not an SLF4J/Log4j logger) was used for the defensive catch**,
  matching the sibling `ProposalBuilder.attachIchraIntakeIfPresent` pattern for the same table, rather than
  introducing a new logger to a file that has none today. Reversal cost: trivial — one import and one field,
  if this file is later migrated to structured logging per CLAUDE.md's "migrated organically when files are
  touched" guidance.

No `LA-NN` was filed or touched — this run resolves values, it does not gate, and the spec already established
that no new legal assumption is needed for tier-1 content (S10-D §5).

---

## 8. Backlog

Highest existing row before this run was **T129** (S10-D). No new T-number was needed — this run is T128's
own phase (b), so **T128 was updated in place**, not re-filed. Its status moved from "spec complete" to
"phase (b) built"; its detail cell now records the exact token deviation from §2 and points here.

---

## 9. Note to Kevin — the exact tokens to paste, copyable

Paste these into your HTML wherever the employer's own numbers belong. Each degrades to plain, correct-reading
text if a proposal has no intake row (an older proposal, or one created before this shipped) — never to
visible `{{...}}` markup:

```
{{ICHRA_COUNTY}}
{{ICHRA_COUNTY_FIPS}}
{{ICHRA_HEADCOUNT}}
{{ICHRA_PLAN_YEAR}}
```

- `{{ICHRA_COUNTY}}` → e.g. `Hopkins County`, or the phrase-neutral empty string if no intake exists — write
  sentences that still read correctly with it blank, e.g. *"for employees in {{ICHRA_COUNTY}}"* rather than
  *"we found options in {{ICHRA_COUNTY}}"* (the second implies a market claim this section may not make).
- `{{ICHRA_COUNTY_FIPS}}` → the raw 5-digit FIPS code, e.g. `48223`. Built because it was explicitly requested
  this run; it has no obvious prose use — probably skip it unless you have a specific reason to print it.
- `{{ICHRA_HEADCOUNT}}` → e.g. `10`, the eligible-employee count as of when the proposal was created. **Not
  live** — if the proposal is sent months later, this number does not update (spec §7 item 5, unchanged by
  this run).
- `{{ICHRA_PLAN_YEAR}}` → e.g. `2026`, derived server-side, never asked of the agent (T125/T127).

⚠️ **`{{ICHRA_STATE}}` does not exist.** If your draft needs the state abbreviation next to the county name,
say so and it is a one-line addition — see §7.

⚠️ **Still true, carried forward: the ZIP crosswalk is Texas-only.** A non-Texas employer produces no intake
row at all, so every token above falls back to empty for them — the section still renders, just without the
county/headcount specifics.

---

## 10. SQL close-out audit

**This run produced, ran, and recommends no SQL.** No `.sql` file was created, edited, or deleted. No SQL
statement of any kind — DDL or DML — was written or executed. Current highest version, `ls docs/migrations/*.sql`:
**64 files, highest `V087__proposal_ichra_intake.sql`** — unchanged by this run, matching the run brief's
expectation. `SELECT MAX(version) FROM schema_version` was not run — no `mysql` client reachable from this
container, a container limitation and not evidence about any environment's actual state (per S10-A/B/D
precedent). This run introduces no new column, table, or migration dependency of any kind — it reads an
existing table (`proposal_ichra_intake`, V087) through an existing DAO method that was not modified.

---

## 11. Commit

Read from `git log -1` **after** the push:

```
COMMIT_HASH_RECORDED_BELOW
```
