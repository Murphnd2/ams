# Session 20 close-out — S20-A through S20-J

Date: 2026-08-06. Branch: `refactor/modernize-architecture`. HEAD at close: `c10eedb` (verify against `git log -1` — do not trust this number after the fact).

---

## 1. Shipped

All commits below are on `refactor/modernize-architecture`, read fresh via `git log --oneline db53855~1..HEAD` this run — not copied from any prior prompt.

**S20-A — Phase A spec**
- `db53855` — docs: S20-A -- Phase A spec, the four ICHRA proposal sections

**S20-B — build 1, `ICHRA_MARKET`**
- `1ea852e` — feat: S20-B -- T168, the four ICHRA proposal sections build 1 (ICHRA_MARKET)
- `299547a` — docs: S20-B -- V091 registered in tracker and schema-version registry
- `56a4c2e` — docs: S20-B -- T168 filed, T163 corrected (shipped in S19-A, never flipped)
- `2c4a0aa` — docs: S20-B -- close-out, T168 build 1 shipped and local-dev verified

**S20-C — AI Page Builder failure diagnosis (read-only, no commits)**

**S20-D — surface Anthropic failures, fix two stale model strings**
- `8959f74` — fix: S20-D -- surface Anthropic API failures to PSP admin in the page builder
- `e16b681` — fix: S20-D -- ProposalAiBuilder on claude-sonnet-5
- `0bd6e60` — fix: S20-D -- EmailDraftService on claude-sonnet-5

**S20-E — "couldn't process it" diagnosis (read-only, no commits)**

**S20-F — response extraction by type, surface content-shape failures, distinguish truncation**
- `476fd63` — fix: S20-F -- extract Anthropic text by block type rather than array index
- `f1476a8` — fix: S20-F -- surface content-shape extraction failures to PSP admin
- `db74c11` — fix: S20-F -- distinguish truncated responses from unexpected content shapes

**S20-G — inactive proposal sections hidden by default**
- `5b6fc08` — feat: S20-G -- hide inactive proposal sections by default, with a show toggle

**S20-H — ICHRA tokens and compliance rules in the page builder's system prompt**
- `e091352` — feat: S20-H -- ICHRA merge tokens in the AI page builder system prompt
- `869050d` — feat: S20-H -- compliance and section context in the AI page builder system prompt

**S20-I — file T169**
- `d68274b` — docs: S20-I -- file the SkillManager model dropdown defect

**S20-J — fix T169**
- `a8a6d7c` — fix: S20-J -- T169, never substitute a default model on skill update
- `79ae990` — fix: S20-J -- T169, dropdown always includes the stored model value
- `c10eedb` — docs: S20-J -- mark T169 shipped

**18 commits total** across the session. Separately, and predating this session's own commits: `v0.90.02` was released to production and `update.sh` logged `DONE`, putting **V090 live in production** (recorded in `migration_tracker.md`'s V090 row, S19-P — the release that closed out session 19, immediately before this session began).

---

## 2. In flight

- **V091 and T168 (build 1, `ICHRA_MARKET`) are on local dev only.** Verified live there — `schema_version`/`schema_info` both read V091, every new column confirmed by type and nullability against a pre-apply gap check (`migration_tracker.md`'s V091 row). **Production is at V090.**
- **S20-D, S20-F, S20-G, S20-H, and S20-J are committed but not released.** Production does not have: the AI builder's Anthropic-failure surfacing or the two model-string fixes (S20-D); the type-based text extraction, the content-shape failure surfacing, or the truncation distinction (S20-F); the inactive-section hide/show toggle (S20-G); the ICHRA merge tokens and compliance rules in the builder's system prompt (S20-H); the T169 dropdown fix (S20-J).
- This project releases from a single trunk at a chosen point (`CLAUDE.md`'s deployment model — a GitHub Release tag typed against `refactor/modernize-architecture` HEAD, not auto-deployed per commit). There is no mechanism to ship a subset of this session's commits independently of the rest; the next release will carry all of it together, including V091/T168.

---

## 3. Decisions made

1. **The four ICHRA proposal sections model** (`ICHRA_MARKET`, `ICHRA_CONTRIBUTION`, `ICHRA_COMPARISON`, `ICHRA_AFFORDABILITY`). Age bands are a fidelity upgrade for sections 2 and 3, never a gate — but mandatory for section 4, because affordability tests against the lowest-cost silver plan for the employee's own age, and a headcount-only input has no age to test against.
2. **Section 1 (`ICHRA_MARKET`) is the base layer, not a peer.** Sections 2, 3, and 4 all need its market data; none of them can render without it.
3. **Membership widening in `ViewProposal` over fabricating a $0 price line for a `system_managed` enhancement.** `FlaggedEnhancementResolver.systemManagedIdsForProposal` widens `proposalEnhIds` directly from the proposal's LOS list rather than requiring a priced `RateTable` row. **This contradicts T165's own stated promise** ("this build never requires a second edit to `ViewProposal.java`") — the promise held for the predicate (`isSectionEnabled`), not for membership. Recorded as a real gap the S20-A spec found, not papered over.
4. **Section 4's display posture, decided but not built:** render the contribution ceiling and its inputs, state no verdict — no "affordable"/"not affordable" framing on a public page. The `LA-NN` legal_assumptions.md entry that would formally settle public exposure of the affordability figure is filed **when section 4 is actually built, against a real design** — its draft text exists in the S20-A spec, but no entry was filed this session, deliberately.
5. **Text extraction by block type, not array index** (S20-F commit 1) — `extractText` scans every `content` block for `type == "text"` rather than assuming index 0, concatenating multiple text blocks if present.
6. **Truncation surfaced, not silent** (S20-F commit 3) — a `max_tokens` stop now returns the extracted text as normal, with a visible notice appended, rather than either discarding usable content or looking identical to any other failure.
7. **`ProposalAiBuilder.MAX_TOKENS` left at 4096, deliberately** — not raised this session. With truncation now visible instead of silent, the actual truncation rate becomes observable for the first time; raising the ceiling without that data would be a guess. See T170 below — the one live case observed since this decision was made does not yet resolve cleanly to "truncation," so the deferral holds.

---

## 4. New assumptions, and reversal cost

- **SortableJS's DOM-order submission (`querySelectorAll('.section-card')`) safely tolerates `display:none` rows** (S20-G). Verified against the actual server-side reorder handler (`ProposalSettings.java:206-214`, `sortOrder = i + 1` by array index) rather than assumed — confirmed low-risk because it was checked, not because it was likely.
- **The git-history reconstruction technique used to split single-file, multi-commit diffs** (S20-D commit 1/2 split, S20-F's three-commit split) **produces byte-identical results to a single-pass edit.** Verified each time via matching git blob hashes before committing, not merely assumed. Reversal cost: none realized — every check passed.
- **Production's release-per-tag deployment model means no commit from this session has reached production without an explicit release.** Consistent with `CLAUDE.md`'s documented deployment model and with `migration_tracker.md`'s own V090/V091 Production-column state. Reversal cost if wrong: low to detect (a direct production check would show it immediately) but this session made no production check of its own — see §5 below.

If a claim isn't listed above, it wasn't newly assumed this session — every other technical decision in §3 was checked against source, not assumed.

---

## 5. Open questions and carried-forward items

- **The ` ```html ` truncation in the AI page builder — filed as T170 this run.** Reproducible: the response stops at the opening code fence, and no `⚠️ This response was cut off...` notice (added S20-F) appeared. Two code-plausible, unconfirmed explanations recorded in T170's row: either it wasn't actually a `max_tokens` stop, or the notice was appended but swallowed by `formatProposalAiResponse`'s regex (which requires a *closed* code fence to recognize a canvas block at all — an unclosed one passes the entire raw, unescaped response straight into `innerHTML`, where a truncated open tag like `<style>` could hide anything concatenated after it). Deferred by Kevin. Settling it needs one live reproduction with the raw Anthropic body logged — which `extractText`'s current logging does not capture on a *successful* extraction (S20-E's finding: only the structural-failure path logs the raw body).
- **S20-J is not yet walked.** The T169 fix (never substitute a default model on update; dropdown always represents the stored value) is code-verified only — traced through both the client and server paths logically, but no browser session has exercised it.
- **The `proposalsectionlos` weakness** (S20-A spec §9.3 item 2, carried forward unresolved): the rule that the four ICHRA sections must carry zero `proposalsectionlos` rows is enforced by convention and by the verification walk, not by code. An admin who associates a LOS with one of these sections silently disables `FlaggedEnhancementResolver`'s gate for it, with no error anywhere.
- **`EMAIL_DRAFT_ASSISTANT`'s actual runtime model lived in `chatbot_skill`, not in code** (found during S20-D). The general lesson, now demonstrated twice in one session (once here, once in T169's production incident): database configuration outranks a code fallback whenever it's non-blank, and it rots exactly as invisibly as a hardcoded string does — arguably worse, since a code string is at least visible to whoever reads the file.
- **Sections 2 (`ICHRA_CONTRIBUTION`) and 3 (`ICHRA_COMPARISON`) are unblocked** per the S20-A spec §8 — no migration, no new architecture, a token and a section-config pair each. **Section 4 (`ICHRA_AFFORDABILITY`) stays blocked** on the undrafted-but-specified `LA-NN` entry.
- **Three SWBD emails to Forrest remain unsent** — the longest-standing open item in this project's history, and unrelated to anything this session touched technically. Nothing built this session reaches Forrest without them.

---

## 6. Contradictions found

Recording each premise a prompt this session asserted that the repository disproved — per this project's own standing practice, these are the most useful part of this record, not an embarrassment to bury.

1. **`suppressed` was not a checkbox precedent.** Corrected in session 19; noted here because the same *shape* of assumption — "a sibling boolean flag already has an admin control, so this one probably does too" — recurred as a pattern worth naming, not because it recurred as the identical claim.
2. **No eye icon existed in the Proposal Sections panel header** (S20-G). A prompt assumed one did and asked to extend it; an exhaustive search of the panel header found only three "add section" buttons. Built a new control instead, stated plainly that there was nothing to extend.
3. **`max_tokens` truncation was not the mechanism for the "couldn't process it" extraction failure** (S20-E). A truncated response still returns a `text` block at index 0 — just a shorter one — which passes the old index-based extraction check and renders as broken-but-processed HTML, not as the "couldn't process it" fallback string. The two failure classes were conflated in the prompt's framing and separated in the diagnosis.
4. **The token count was 28 total / 18 `ICHRA_*`, not 26/15** (S20-H). Traced to an arithmetic error in a prior session's own close-out *summary* (10 + 15 + 3 = 28, not 26) — the enumerated list in that same close-out was already correct and matched this session's fresh, independently-derived count exactly.
5. **`ICHRA_PAYLOAD_AS_OF` emits a plain disclosure sentence, not an HTML block**, unlike its two payload-token siblings (`ICHRA_AGE_BAND_TABLE`, `ICHRA_PLAN_LANDSCAPE_TABLE`), which do emit `<table>` blocks. A prompt's framing that "all three emit HTML blocks" was corrected before it was written into the system prompt as guidance the model would have followed literally.
6. **The `ProposalIchraSnapshot` zero-rows mystery, from earlier verification work this session, ultimately resolved to "no proposal had actually been created"** — not the staging provenance guard, and not a database mismatch, both of which were considered and discarded as the explanation before the real one was found. Recorded here as a reminder that the first two plausible explanations for a zero-rows result were both wrong.

---

## 7. Production incident — T169, recorded in full

`SkillManager`'s Edit Skill modal's Model dropdown (`skillManager25.jsp:165-166`, before this session's fix) hardcoded exactly two options: `claude-haiku-4-5-20251001` and `claude-sonnet-4-5-20250514`. The second **is not a valid model ID** — it pairs a 4.5 minor version with Sonnet 4's release date; Sonnet 4.5's real snapshot is `claude-sonnet-4-5-20250929`.

`ICHRA_DESIGN_ADVISOR` was stored as `claude-sonnet-5` — verified working end to end — which matched neither dropdown option, so the Edit Skill modal rendered its Model field blank. An admin selected the only sonnet-class option available to clear the blank, and the save overwrote the working configuration with the invalid string.

**Compounding it:** `SkillManager.populateFromRequest`'s ternary substituted `claude-haiku-4-5-20251001` whenever the `model` request parameter was absent — and an unselected `<select>` submits no entry for that field at all. So **any edit to any skill whose stored model fell outside the two hardcoded options silently downgraded it to Haiku**, including an edit that never touched the Model field. Three production skills — `NDT Testing Specialist`, `EMAIL_DRAFT_ASSISTANT`, `ICHRA_DESIGN_ADVISOR` — were left on the invalid string. The fourth, `ACH Report Analyzer`, was untouched only because its value came from `DatabaseInitializer`'s seed, never from an edit through this dropdown.

**Repair, by Kevin, outside this session's own commits:**
```sql
UPDATE chatbot_skill SET model = 'claude-sonnet-5' WHERE psp_id = 4 AND model = 'claude-sonnet-4-5-20250514'
```
3 rows affected, plus a Tomcat restart (`chatbot_skill` is JPA-mapped; a raw `UPDATE` against a running instance does not take effect until restart — T64's third logged observation of this same pattern). Kevin then verified the Design Advisor answers correctly on `claude-sonnet-5`.

**T169, filed and fixed this session (S20-I, S20-J), addresses both faults:** the save path now never invents a model on update (leaves the stored value untouched and logs a WARN naming the skill when the parameter is absent), and the dropdown now always represents whatever is actually stored — injecting a marked, non-error `(current)` option for any value not on the curated list, rather than rendering blank.

**The fix is not in production.** Every skill whose model isn't `claude-haiku-4-5-20251001` remains vulnerable to the same silent downgrade on production until this session's commits are released.

---

## 8. Next

1. **Walk S20-J (T169) and T168's own verification list before anything else.** Both are code-verified only. T169 in particular protects live production data from a defect that has already fired once — confirming the fix actually holds in a browser, not just in traced code, should happen before it's treated as closed.
2. **Once both check out, cut a release.** Everything from S20-B through S20-J ships together as this project's release model requires — there is no partial-release path. That release would put V091/T168, the AI builder hardening (S20-D/F/H), the inactive-section toggle (S20-G), and the T169 fix (S20-J) into production in one move, closing most of this session's own "in flight" list at once.
3. **Chase T170 (the truncation symptom) with an actual reproduction and raw-body logging**, since it's a live, reproducible defect in the exact feature this session spent the most effort hardening, and the two candidate explanations recorded this session point at genuinely different fixes.
4. **The three SWBD emails remain Kevin's alone**, unrelated to anything technical here, but the oldest item on this project's own record — worth naming every time this document format asks "what's next," since nothing else in this project's roadmap reaches Forrest without them.

---

## 9. SQL close-out audit

- **Every SQL statement produced, run, or recommended this session:**
  - `docs/migrations/V091__ichra_section_selection.sql` (S20-B) — **created, in a versioned migration.** Registered in both `docs/analysis/migration_tracker.md` and `docs/schema_version_migration.sql` in the same session. Applied to local dev (`beta_ssa`, work) and verified live — every new column checked by type and nullability against a pre-apply gap check.
  - The manual production `UPDATE chatbot_skill SET model = 'claude-sonnet-5' WHERE psp_id = 4 AND model = 'claude-sonnet-4-5-20250514'` (3 rows) — **explicitly not in a migration.** This was deliberate reference-data repair by Kevin, outside this session's own commits and outside any run's fence — never a schema change, and no run this session was authorized to touch `chatbot_skill` rows (S20-J's own fence explicitly forbade it: "the data is already repaired; this run protects it").
  - No other SQL was produced, run, or recommended this session.
- **Orphaned `.sql` files:** none. V091 is the only `.sql` file created this session and it is fully registered.
- **Current highest migration version, read from `docs/migrations/` this run:** **V091** (`V091__ichra_section_selection.sql`).
- **Dev's current version: V091**, applied and verified on local dev (`beta_ssa`, work) only.
- **Production's current version: V090**, via release `v0.90.02` (`update.sh` logged `DONE`) — a release that predates this session's own work. V091 is pending the next release.

---

## 10. Code-verified vs. runtime-verified

**Runtime-verified this session** (walked in a browser or against a live database, not merely compiled):
- **T168, both directions** — a `system_managed` section renders when its enhancement is selected and its data is present, and is correctly withheld when either condition fails.
- **Existing (pre-session) proposals unaffected**, confirmed on both dev and production.
- **Service Manager and the "System managed" checkbox**, confirmed on production.
- **The AI builder generating HTML** through a real conversation.
- **The inactive-section toggle**, including that drag-and-drop reorder continues to work correctly with sections hidden.
- **The builder using ICHRA tokens**, and **declining a plan-ranking request with a compliant alternative** rather than violating the no-steering-layer rule.
- **The Design Advisor answering correctly on `claude-sonnet-5`**, post-repair.

**Not runtime-verified — code-verified only:**
- **S20-J (T169).** Both the save-path fix and the dropdown fix are traced through their logic in this record and in S20-J's own report, not exercised in a browser.
- **T170's truncation symptom** — observed once by Kevin, not yet reproduced under logging that would settle its cause.
- **V091's full column set beyond what the pre-apply/post-apply gap check confirmed** — the gap check verified type, nullability, and default per column; it did not exercise the application code paths that read or write those columns end to end (that's T168's own verification list, separately tracked as runtime-verified above only for the render behavior, not for every write path).

No code was edited this run beyond the T170 backlog row. No `.sql` file was touched. No production access. No Anthropic API call. No credential was printed or written, at any point in this record or in the session it summarizes.
