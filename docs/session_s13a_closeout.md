# S13-A — T133: unmatched tokens must never reach a customer render

**Date:** 2026-08-04
**Branch:** `refactor/modernize-architecture` (trunk, committed directly — no branch, no tag)
**Model:** Opus

---

## Shipped

| Commit | Subject |
|---|---|
| `PLACEHOLDER_C1` | fix: strip unmatched tokens before render (T133, S13-A) |
| `PLACEHOLDER_C2` | docs: record S13-A close-out commit hash |

Hashes are filled in from `git log` after the push, per the standing convention, and are
recorded in the second commit.

**Preflight — trunk position.** The run brief carried two conflicting records of trunk
(`7655286` from a session-launch note, `def5450` as session 12's own close-out hash) and
asserted neither. Both are ancestors of HEAD and are sequential — `def5450` is the session 12
close-out, `7655286` the follow-up recording its hash. **They do not conflict.** HEAD at
preflight was `82c84965e6bb9e86506675bcea19f5f4f98138a3`, three commits ahead of `7655286`;
those three are this session's earlier email-recipient work, unrelated to T133.

---

## In flight

None. Working tree clean at close.

---

## Discovery findings

The most durable output of this run. Recorded so the next session does not re-investigate.

**Location.** `src/main/java/net/superiorstate/ams/controller/activity/setup/ViewProposal.java:872-882`
(pre-change). It is a **`private` instance method on the servlet**, not a shared utility class.

**Delimiter — quoted from source, not inferred** (line 878, pre-change):

```java
String pattern = "(?i)\\{\\{" + Pattern.quote(entry.getKey()) + "\\}\\}";
```

`{{TOKEN}}`, case-insensitive, **no whitespace admitted inside the braces**. The run brief's
recollection of `{{TOKEN}}` was correct.

**Call sites — exactly one.** `ViewProposal.java:386`:

```java
sectionHtml.put(section.getId(), replaceTokens(section.getHtmlContent(), tokens));
```

Guarded by `section.getSectionType()` being `TITLE`, `CLOSING`, or `CUSTOM` and
`getHtmlContent() != null`. Because the method is `private`, external callers are impossible
by construction — this is a proof, not a survey.

**Token map assembly.**

| Contributor | Location |
|---|---|
| `buildTokenMap` | `ViewProposal.java:641` |
| `putIchraMarketTokens` | `ViewProposal.java:769` |

No other `put*Tokens` contributor exists. 25 keys total, `put()` inline at `:645-731`
(buildTokenMap) and `:847-853` (putIchraMarketTokens).

**Used outside proposal document rendering?** **No.** One private method, one call site,
rendering `ProposalSection.htmlContent` only. `grep -rn` across `src/` for `replaceTokens`
returns the definition, that one call, and one comment — nothing else.

Two related facts established while clearing the hard stops, both worth keeping:

- **`proposalSettings.jsp:381`** renders the same `htmlContent` field **raw** into an authoring
  textarea. That is correct and must stay that way — the author edits the template with tokens
  visible. It is not a customer surface and is not affected by this change.
- **Nothing else in `src/` reads `getHtmlContent()` for render.** There is no second
  proposal-render path and **no separate PDF generator** reading sections, so the customer-facing
  PDF referenced in the T133 statement is produced from this same rendered HTML. The fix covers it.

---

## Step 1 hard stops — all four evaluated, none fired

Recorded individually because the run brief required confirmation that each was *cleared*
rather than skipped.

1. **More than one delimiter syntax / a second substitution mechanism this fix would leave
   uncovered — CLEARED.** One pattern construction, at line 878. Searched all of
   `src/main/java` for `{{`: hits are `ProposalAiBuilder` (system-prompt text *documenting*
   tokens to the model — never passed through `replaceTokens`), `ProposalSettings` (default
   template *data*), and `ViewProposal` itself. A distinct `<<TOKEN>>` mechanism exists for
   automation emails (`Automation.getHtmlContent`, `AutomationHelper`) — **different delimiter,
   different entity, different surface**; not covered by this fix and not intended to be.
   Flagged below as an open question, not treated as a blocker.
2. **A caller where a literal, un-substituted delimiter is legitimate — CLEARED EMPIRICALLY.**
   Not assumed. All 8 files in `docs/proposal_html/` contain **zero** `{{`. Of 21
   `proposal_section` rows in the local production copy, 10 contain tokens, and every distinct
   token present is a known map key: `ACCENT_COLOR`, `AGENCY_NAME`, `AGENT_EMAIL`, `AGENT_NAME`,
   `APPLY_BUTTON`, `DATE_CREATED`, `PRIMARY_COLOR`, `PROSPECT_NAME`, `PSP_NAME`. No code samples,
   escaping examples, or documentation content among them.
3. **Serves a surface outside proposal rendering — CLEARED.** `private`, one call site. See above.
4. **Delimiter not unambiguously matchable — CLEARED, conditional on implementation.** `{{NAME}}`
   is matchable *provided* the residual pattern is token-shaped. Implemented as
   `\{\{[A-Za-z0-9_]+\}\}`, **deliberately not** `\{\{[^}]*\}\}`, which would match a nested JS
   object literal such as `{{x:1}}` inside a `<script>` block pasted into a `CUSTOM` section.

---

## What was built

`ViewProposal.java` only. **+22 / −0**, read from `git diff --cached --numstat`: 2 import lines,
2 for the logger field and its blank separator, 18 for the residual-strip block (8 of which are
its explanatory comment).

1. `private static final Logger log = LoggerFactory.getLogger(ViewProposal.class);` plus the two
   `org.slf4j` imports.
2. A residual-strip block appended to `replaceTokens`, after the existing substitution loop.

The substitution loop is **byte-identical**. Every known key resolves exactly as before; the
method signature is unchanged; no caller was touched; no helper was extracted; nothing was
reformatted, reordered, or renamed.

Behavior: after all known substitutions, scan for `\{\{[A-Za-z0-9_]+\}\}`. If any are found,
emit one WARN naming all of them, then replace each with the empty string. If none are found,
**log nothing** — this path runs on every proposal render and is silent in the normal case.

---

## Verification claim

**`code-verified`.** Stated by name, and deliberately not upgraded.

**What was actually executed.** The exact `replaceTokens` body as shipped was copied verbatim
into a throwaway program outside the repo and run under JDK 17 — 12 assertions, 12 passed, exit 0:

- known token substitutes; multiple known tokens substitute; known token still case-insensitive
- unknown token stripped **and** warned; mixed known+unknown resolves the known and strips the
  unknown; several unknowns all named in a single warn
- no tokens at all → silent; `null` html → `""` and silent (pre-existing contract preserved)
- nested JS object literal `{{x:1}}` **not** matched; spaced `{{ PROSPECT_NAME }}` left alone;
  CSS `{ .x { … } }` not matched; a token mapping to `""` does not trigger a warn

**Why this is not `runtime-verified`.** No proposal was rendered by the running application. No
Tomcat walk, no browser, no PDF. `src/test` does not exist and the run brief forbade building a
harness, so the executed check is of the *method's logic*, not of the *deployed render path*.
The wiring is established by reading (one private call site, unchanged signature) — which is
exactly the class of claim this project has been burned by before.

**What a human walk must exercise to close the gap:**

1. A proposal whose section HTML contains a token that is **not** in `buildTokenMap` — confirm
   the literal `{{...}}` does not appear on the rendered public proposal page, and that
   `ams.log` carries `T133 stripped unmatched proposal token(s) before render: {{…}}`.
2. The **same page** still renders its known tokens correctly (`{{PROSPECT_NAME}}` etc.).
3. Ideally a **non-ICHRA** proposal — COBRA or FSA — since the `ICHRA_*` keys are precisely the
   ones absent from the map on other lines of service, and that is where residuals are likeliest.
4. Confirm the WARN is **absent** from `ams.log` on an ordinary proposal render with no stray
   tokens.

---

## Decisions made

1. **Option (a), strip-with-log at render time, was built.** Per the run brief, the choice was
   already made and not reopened.
2. **Option (b), authoring-time validation, declined and not to be relitigated.** The valid-token
   set is **context-dependent** — a token legitimate on an ICHRA proposal is absent from the map
   on a COBRA one — so authoring-time validation cannot be complete, would emit false warnings on
   correct content, and does nothing about rows already in the database.
3. **Residual pattern is token-shaped, not greedy.** `\{\{[A-Za-z0-9_]+\}\}` over
   `\{\{[^}]*\}\}`. The loose form is a real hazard given `CUSTOM` sections accept pasted HTML
   including `<script>`.
4. **⚠️ Scope-fence override, stated explicitly as required.** The run brief's permitted list
   ("the single file containing `replaceTokens`") and its forbidden list (`ViewProposal.java`,
   "for any reason") **directly contradict each other**, because `replaceTokens` lives in
   `ViewProposal.java`. The run stopped before writing anything and asked. Kevin ruled the
   contradiction a **defect in the prompt, not in the code**: the forbidden entry was written on
   the assumption that `replaceTokens` lived in a shared helper class. The permitted clause is
   authoritative; `ViewProposal.java` was edited, touching only the `replaceTokens` body plus the
   logger field and its import. Nothing else in that file was read for modification, reordered,
   or reformatted.
5. **⚠️ Reversal of an in-run instruction, recorded at Kevin's request.** His first ruling said
   *do not add a logger — match the file's four existing `System.out.println` sites.* He then
   reversed it: **add the SLF4J logger.** Rationale, in his words in substance: `catalina.out` is
   not where anyone would find this; `ams.log` is. `log4j2.xml` routes `net.superiorstate.ams` to
   `ams.log` with 14-day retention, so a WARN is discoverable there and a `println` is not.
6. **The four existing `System.out.println` sites (`:507`, `:555`, `:704`, `:841`) were NOT
   converted.** That is unrelated cleanup and ships alone, if at all. **A file with mixed logging
   styles is the correct outcome of this run**, not an oversight.
7. **Requirement 3 amended by Kevin, and why.** No proposal or section identifier is in scope
   inside `replaceTokens`; both exist at the call site (`:386`) but the signature was fixed by the
   brief and adding a parameter was forbidden. The WARN therefore names the unmatched token(s)
   only — sufficient to locate the offending content by grep.

---

## New assumptions

No `LA-NN` filed — this is a rendering-correctness fix, not a legal or market-claim assumption.

**Technical assumption (T-S13A-1).** *No legitimate proposal-section content contains a
token-shaped `{{WORD}}` that is meant to render literally.* Evidence: 21 `proposal_section` rows
and 8 `docs/proposal_html/` files inspected; zero counter-examples. **Reversal cost: low.** The
strip is one block in one method; deleting it restores prior behavior exactly, since the
substitution loop was not modified. Should a future proposal page legitimately need literal
braces — a page documenting the token system to an internal audience, say — that content would
be silently emptied, and the WARN in `ams.log` naming the token is the detection mechanism.

---

## Open questions raised

1. **The `<<TOKEN>>` automation-email substitution mechanism has the same class of defect and is
   untouched.** `AutomationHelper` resolves `<<#erName>>`-style tokens in `Automation.htmlContent`
   for activity emails. Whether an unmatched `<<…>>` renders literally to a recipient was **not
   investigated** — different surface, outside this run's fence. *Settled by:* a separate
   investigation run, if judged worth filing.
2. **Spaced variants `{{ FOO }}` remain unhandled.** Not stripped, because they are also not
   substituted — the substituter's pattern admits no whitespace. Behavior is **unchanged**, not
   newly broken, but a pasted page using spaced tokens would still show literal braces.
   *Settled by:* a decision on whether the substituter should tolerate whitespace at all; if it
   should, both patterns change together.
3. **Does any *deployed* environment have proposal sections carrying unmatched tokens today?**
   Checked only against the local production copy (refreshed 2026-08-02), where the answer is no.
   *Settled by:* the same query against production, or simply by watching `ams.log` for the new
   WARN after deploy — which is what it is for.

---

## Contradictions found

Flagged, not fixed.

1. **The run brief's scope fence contradicts itself.** Permitted list vs. forbidden list, as
   described in Decisions §4. Resolved by Kevin mid-run.
2. **`CLAUDE.md` states "New code uses `LoggerFactory.getLogger(...)`."** `grep` for
   `LoggerFactory.getLogger` across all of `src/main/java` returned **zero matches** before this
   run. The convention is documented but was, until this commit, unimplemented anywhere in the
   codebase. This commit is the first use. `CLAUDE.md` was not edited — outside the fence.
3. **The T133 backlog row's own location reference was stale**, citing
   `ViewProposal.java:~500-509`; the method is at `:872-902`. The S10-E note predicted exactly
   this drift. Corrected **within the T133 row only**, which is inside the fence.
4. **`docs/ichra_strategy.md` — not read, not edited.** Kevin's file. No discrepancy to report,
   because this run had no reason to open it.

---

## SQL close-out audit — mandatory section, and it is empty by design

- **SQL statements produced:** none.
- **SQL statements run:** none against any deployed environment. Three **read-only `SELECT`s**
  were run against the **local** `beta_ssa` copy during Step 1 discovery, to establish the
  blast radius empirically rather than by assumption: a count of `proposal_section` rows, a
  count of those containing `{{`, and an extraction of the distinct tokens present. **No `INSERT`,
  `UPDATE`, `DELETE`, or DDL of any kind.** These were diagnostic reads, not schema changes, and
  correctly belong to no migration.
- **SQL recommended:** none.
- **Statements requiring a versioned migration:** none.
- **Orphaned `.sql` files:** none created. `git status --untracked-files=all` reports no
  untracked `.sql` anywhere in the repository.
- **Current highest migration version — read from `docs/migrations/`, not recalled:**
  **`V088__proposal_ichra_intake_contribution.sql`**. **Unchanged by this run**, as expected.
- **Pending deployment:** nothing from this run. This run's artifact is a WAR-only change.
- **Schema described but not scripted:** none. This run describes no schema.

**This run produced no SQL, and that is the expected outcome — stated explicitly rather than
omitted.**

---

## Next

**Runtime-verify against a non-ICHRA proposal before this reaches customers.** The change is
`code-verified` only, and it edits the single method that renders every proposal document in
AMS — COBRA, FSA, HRA, and every other line of service. The executed logic check is strong on
the regex and the warn/silence behavior, but it cannot prove the method is reached as expected
in a live render, and that is precisely the gap this project's history says matters.

The cheapest sufficient walk: render one existing proposal (confirming known tokens still
appear), then add a deliberate `{{NOT_A_REAL_TOKEN}}` to a `CUSTOM` section and render again —
the token should vanish and `ams.log` should name it. That is a two-minute check and closes the
claim from `code-verified` to `runtime-verified`.

Two follow-ups worth filing but **not** done here, per the ship-unrelated-fixes-alone rule:
the `<<TOKEN>>` automation-email equivalent (open question 1), and converting this file's four
`System.out.println` sites now that it has a real logger.
