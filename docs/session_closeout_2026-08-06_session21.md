# Session 21 close-out — S21-A through S21-N

Date: 2026-08-06. Branch: `refactor/modernize-architecture`. HEAD at close: `1f88a0e8703fb0371d703fb3fc02438dd1b3b08d` (verify against `git log -1` — do not trust this number after the fact).

Session boundary verified this run: `9e900ed308d8c254a02fd341c7a5b53435a1c77e`'s parent is `976e4b5` (`docs: S20-Z -- session 20 close-out`), confirming `9e900ed` is this session's actual first commit, not assumed from the prompt.

---

## 1. Shipped

All 23 commits below, read fresh this run via `git log --reverse --format="%H %s" 9e900ed^..HEAD` — full hashes, not copied from any prior close-out or prompt.

**S21-A — release-prep audit (read-only, no commits).** Discovered production was already at `v0.91.05`/V091, not V090 as `migration_tracker.md` and the session's own prompt assumed. See §6.

**S21-B — correct the tracker, then T171 (`ICHRA_CONTRIBUTION`)**
- `9e900ed` — docs: S21-B -- migration_tracker V091 marked applied to production (v0.91.05)
- `b1a1916` — feat: S21-B -- T171, ICHRA proposal section 2 (ICHRA_CONTRIBUTION) scenario table
- `cbd54fc` — docs: S21-C -- T171 filed

**S21-D — RANGE-mode contribution persistence**
- `468cf7c` — fix: S21-D -- persist contribution on RANGE-mode ICHRA snapshots, matching the age-band path

**S21-E — T172 (`ICHRA_COMPARISON`)**
- `636b2fb` — feat: S21-E -- T172, freeze groupComparison in the ICHRA payload
- `140699b` — feat: S21-E -- T172, ICHRA proposal section 3 (ICHRA_COMPARISON) token
- `983b751` — docs: S21-E -- T172 filed

**S21-F — RANGE-mode contribution/net render fix**
- `6e0e6e3` — fix: S21-F -- T171, render contribution and net on RANGE-mode contribution scenarios

**S21-G — T172 unit correction (per-employee vs. group total)**
- `2620f47` — fix: S21-G -- T172, freeze planned contribution as a group total and correct employerDelta
- `44b8980` — fix: S21-G -- T172, state units in group comparison column headings
- `035537c` — fix: S21-G -- T172, state units on ICHRA current-coverage intake labels

**S21-H — merge-token helper popup, gated**
- `364987e` — feat: S21-H -- T173, list ICHRA merge tokens in the proposal token helper, gated
- `4edbcff` — docs: S21-H -- T173 filed

**S21-J — ICHRA proposal section page fixtures (corrected mid-session, four pages → three)**
- `df06fc6` — docs: S21-J -- ICHRA proposal section page fixtures *(four-page draft — superseded same session)*
- `4b1b019` — docs: S21-J -- ICHRA proposal section page fixtures *(corrected to three pages, one per live `system_section_key`)*

**S21-K — market-token defect diagnosis, two cosmetic fixes**
- `8c40f5c` — fix: S21-K -- format ICHRA_PAYLOAD_AS_OF's captured-at date for a human reader
- `e970e3b` — fix: S21-K -- remove dangling empty-tag separator from the market fixture heading

**S21-L — one constant governs authoritative rate source env**
- `903c73c` — feat: S21-L -- RateSourceEnvResolver, one constant governs authoritative rate source env
- `0416f21` — docs: S21-L -- LA-18, staging rate data accepted as authoritative pending production access
- `e197575` — docs: S21-L -- T174 filed

**S21-M — the last hardcoded source-env check**
- `6f04f2f` — fix: S21-M -- T174, route the conditional Market page source-env gate through RateSourceEnvResolver
- `b490362` — docs: S21-M -- T174 closure recorded

**S21-N — deployment item for the new constant**
- `1f88a0e` — docs: S21-N -- D88, seed ICHRA_RATE_SOURCE_ENV on existing installations

**23 commits total.** Separately, and predating every one of them: `v0.91.05` was already live in production when this session opened — see §6.

---

## 2. In flight

**None.** `git status --short` is clean at HEAD (`1f88a0e`). Every sub-run this session ended with a clean tree before handing off.

---

## 3. Decisions made, and what each closed

1. **Staging rate data is authoritative until production HealthSherpa access exists; entitlement, not data provenance, is the control (S21-L).** Closes the multi-run inconsistency where `RateCacheDAO.check()`, `putIchraMarketTokens`, and `ProposalBuilder`'s snapshot-attach checks each hardcoded `PRODUCTION` independently and could disagree about the same cache row. One resolver, one constant (`ICHRA_RATE_SOURCE_ENV`, `STAGING` today) now answers for all of them.
2. **Three ICHRA proposal-section pages, not four (S21-J, corrected mid-run).** Closes a wrong initial assumption that plan-landscape and age-band content deserved a page of their own — tokens are gated by `system_section_key`, and only three keys (`ICHRA_MARKET`, `ICHRA_CONTRIBUTION`, `ICHRA_COMPARISON`) can ever render a section; `ICHRA_AFFORDABILITY` is a fourth literal that is permanently inert (§5).
3. **Section 2's (T171) RANGE-mode contribution is now frozen and rendered (S21-D, S21-F).** Closes the gap where S21-D's write-side fix (persisting contribution on RANGE snapshots) shipped without S21-F's corresponding read-side consumer, so persisting a value nothing read changed nothing visible for one full sub-run.
4. **Section 3's (T172) comparison arithmetic corrected from per-employee-vs-group-total confusion to a consistent group total (S21-G).** Closes a real, customer-facing arithmetic defect: the shipped delta had subtracted a per-employee contribution figure from group-total premiums, producing a materially wrong (and flattering) number.
5. **The merge-token helper popup now lists ICHRA tokens, gated by the same resolver every other ICHRA surface asks (S21-H).** Closes the specific defect class that produced the `{{PROPOSAL_DATE}}` incident: an author working from an incomplete list of what the code actually resolves.

---

## 4. New assumptions, and reversal cost

**LA-18 — Staging rate data is treated as fully authoritative pending production HealthSherpa access** (`docs/analysis/legal_assumptions.md`, filed S21-L). Kevin's explicit, on-the-record decision: entitled ICHRA users — including on the public, unauthenticated proposal page — see staging-sourced plan counts, carrier counts, and premium floors presented as plain fact, with no distinguishing marker. The control is entitlement (`IchraAccessResolver`/`agency.ichra_enabled`), not a data-provenance check; nobody outside PSP admin is granted that entitlement while `ICHRA_RATE_SOURCE_ENV` reads `STAGING`.

**Reversal cost — split, deliberately not softened.** Flipping the constant to `PRODUCTION` once real data is live is a one-row change, no code impact. **Not reversible in the other direction:** any proposal already sent while the constant read `STAGING` cannot be un-sent or silently corrected — its figures are frozen into `payload_json` at build time, and an employer who has already read them has already read them.

**Confirm-before trigger:** granting `agency.ichra_enabled` to any agency other than through PSP-admin access, while `ICHRA_RATE_SOURCE_ENV` still reads `STAGING`. Per the deployment note in §6, **this trigger has already fired once, deliberately** — ICHRA entitlement was granted to agency 14 on local dev this session, as reported to this session, to make the market-token investigation possible at all. Not verified by direct query this run (out of scope fence — no database access), recorded as reported.

---

## 5. Open questions and carried-forward items

1. **`ICHRA_PLAN_LANDSCAPE_TABLE` can never render, and the market fixture carries a section built around it anyway.** `ProposalBuilder.buildIchraPayload` sets `planLandscape` to `JsonNull` unconditionally (confirmed again this run, `:1036`) — T166, the HealthSherpa plan-fetch that would populate it, was never built. `docs/analysis/ichra_section_pages/ICHRA_MARKET.html`'s "Off-Exchange Plan Landscape" section sits above a token that is permanently empty. **Open for Kevin:** remove that section from the fixture, or leave it and mark it explicitly pending T166.
2. **`ICHRA_AFFORDABILITY` is a selectable-looking admin option that can never do anything.** It exists as a literal in `serviceManager25.jsp`'s `system_section_key` dropdown and in `buildSectionsBlock`'s payload output, but `selected` is hardcoded `false` server-side — no control anywhere lets an agent select it, so it can never gate a render. An admin who picks it from the dropdown gets a working-looking configuration that silently does nothing. **Open:** grey it out, remove it from the dropdown until build 4 is unblocked, or document the trap — Kevin's call.
3. **Market figures are not frozen at build time, and this session's own work did not change that.** `putIchraMarketTokens` (`ViewProposal.java`) is a live, render-time query against `rating_area_rate_cache` — not a read of the frozen `payload_json`. A sent proposal's plan count, carrier count, and premium floors can change after the client has already received the document, which sits uneasily next to this session's own point-in-time design for every other ICHRA figure (T162). **Recommended, not built:** freeze the market figures into the payload the same way age-band premiums already are.
4. **`.claude/skills/proposal-content-page/SKILL.md`'s token table is stale, in a narrower way than this close-out's own generating prompt described it.** Verified fresh this run: the file does **not** list `{{PROPOSAL_DATE}}`, `{{SECONDARY_COLOR}}`, or `{{CURRENT_DATE}}` as valid — it carries an explicit "do not use any of these" warning table naming exactly those three (dated 2026-08-03, predating this session), each with the correct token to use instead. It does **not** omit `{{ACCENT_COLOR}}` — present under "Brand colors." It does **not** omit "every ICHRA token" — 15 ICHRA tokens are documented. **What is actually stale:** its own claim that "these 25 are the complete set" — it is missing exactly the 5 tokens that postdate its last update: `ICHRA_AGE_BAND_TABLE`, `ICHRA_PLAN_LANDSCAPE_TABLE`, `ICHRA_PAYLOAD_AS_OF` (T165/V090, predates this session) and this session's own `ICHRA_CONTRIBUTION_SCENARIO_TABLE`/`ICHRA_GROUP_COMPARISON_TABLE` (T171/T172). **Open:** update the token table and its "25 is complete" claim. See §6 for how this bears on the `{{PROPOSAL_DATE}}` incident specifically.
5. **`ProposalAiBuilder.java`'s system prompt is stale in the same direction** — its own `MERGE TOKENS`/`ICHRA TOKENS` blocks list the pre-T171/T172 token set and have never been updated for either. Separate surface, separate fix, not touched this session (explicitly out of scope in every run that found it).
6. **The merge-token helper popup (T173, shipped this session) is itself incomplete in one respect.** `buildTokenMap` also resolves `PRIMARY_COLOR`/`ACCENT_COLOR`; neither is in the popup, though both are documented in `ProposalAiBuilder.java`'s system prompt. Flagged, not fixed, when T173 shipped.
7. **`docs/swbd_ichra_build_plan.md` is confirmed session-6-era and carries no trace of any T-number from T163 through T174** (re-verified this run: zero matches for that range, header still dated 2026-07-31 against migration V076, 15 versions behind current). Kevin's call on whether it gets updated, archived, or retired — flagged repeatedly across the session, touched by none of it.

---

## 6. Contradictions found

1. **`migration_tracker.md` was stale at the moment this session opened, and the session's own opening prompt inherited that staleness.** S21-A's release-prep audit was framed around "production is at V090, this release ships V091" — both the tracker's V091 row and the session's own premise. A `git fetch --tags` plus a direct GitHub Releases check showed **`v0.91.05` was already published and live, carrying V091 and every one of session 20's commits**, before session 21's first commit was ever written. S21-B's first commit corrected the tracker the same session it was caught. **A document asserted a repo fact that was already false, and the falseness propagated into a release-prep task's own premise before anyone checked the actual repository state.**
2. **The market-token empty-render defect had four candidate explanations across this session, in sequence, and the fourth was the real one — found in one log line, not by code reading.** In order: a cold/unwarmed rate cache (ruled out — S21-K found the age-band table rendering real premiums, which requires warm data); a partially-warmed county (S21-K's leading hypothesis, code-plausible, not confirmed); a staging-only snapshot admitted through the T150 demo override (S21-K's alternate hypothesis, equally plausible, not confirmed); **and, per this close-out's own instruction, the actual cause: `IchraAccessResolver` returning `available=false` because the proposal's agency lacked ICHRA entitlement.** `putIchraMarketTokens`'s own gate requires `ichraEntitled` as its first condition (`ViewProposal.java`) — if that is false, every market token is empty regardless of anything about the rate cache's provenance. **This means S21-L/S21-M's `RateSourceEnvResolver` work — real, independently justified by Kevin's LA-18 decision, and correctly built — was not the fix for the specific empty-token symptom that motivated investigating source-env handling in the first place.** The unification was worth doing on its own terms; it was not what actually unblocked the two observed proposals. Recorded plainly, per instruction, rather than left to imply the resolver work closed the incident it was prompted by.
3. **The suspected `DATE_CREATED` EclipseLink cache defect (raised S21-G, elaborated S21-H) was disproved by a direct walk, not by further code reading.** S21-H's own code analysis was internally consistent (an `insertable=false, updatable=false` column, no `em.refresh()` after persist, EclipseLink's default cache-everything policy) and proposed a plausible mechanism. Per this close-out's own instruction: a walk of two real proposals — one persisted before the last application restart, one after — showed **both rendering their creation dates correctly.** There is no date-rendering defect. **This session's own reasoned hypothesis about *why* a defect existed was wrong about there being a live defect to explain at all.**
4. **This close-out's own generating instructions mischaracterized `.claude/skills/proposal-content-page/SKILL.md`.** Addressed in full in §5 item 4 — recorded here because it is the same class of error as items 1–3 above: a claim about repository state, made without the direct check this document exists to insist on, corrected by reading the file rather than repeating the claim.

---

## 7. The market-token investigation — cause and correction, recorded in full

**What was observed:** `ICHRA_MARKET` fixture tokens (`ICHRA_PLAN_COUNT`, `ICHRA_CARRIER_COUNT`, the three `ICHRA_FLOOR_AGE_*`, `ICHRA_RATES_AS_OF`, `ICHRA_RATES_SCOPE`) rendered empty on real A1 Door Company proposals, while `ICHRA_AGE_BAND_TABLE` — on the same proposal, same county — rendered real premiums.

**What this session tried, across S21-K/S21-L/S21-M:** established that the two token groups ask different-strength questions of the rate cache at different moments (age-band premiums frozen per-specific-age at build time; market tokens live-queried across the whole county at render time), built `RateSourceEnvResolver` to unify every hardcoded `PRODUCTION` comparison behind one constant, and routed all four remaining hardcoded sites through it, including the one (`ViewProposal.java:145`) an earlier fence had missed.

**What actually explains the observation, per this close-out's own instruction:** `putIchraMarketTokens`'s gate begins `if (ichraEntitled && intake != null && ...)`. `ichraEntitled` comes from `IchraAccessResolver.isAvailableForProposal(em, proposal)`, resolved once in `ViewProposal`'s main flow and logged unconditionally: `log.info("[ICHRA] Proposal access resolved: proposalId={}, agencyId={}, available={}", ...)`. **That single line, read directly, would have shown `available=false`** — the proposal's originating agency was not `ichra_enabled` — which alone is sufficient to explain every empty market token, independent of anything about rate-cache provenance, warmth, or which source env is authoritative.

**The lesson stated plainly, as instructed:** several rounds of code reading produced two live, well-reasoned, code-consistent hypotheses about cache provenance. One log line settled the actual question in a way neither hypothesis, however well-argued, could have — because both were reasoning about the wrong gate. The `RateSourceEnvResolver` unification remains shipped and correct on its own terms (Kevin's LA-18 decision needed it regardless of this specific incident), but it should not be credited with resolving the incident that prompted it.

---

## 8. Deployment state

- **`D-88`** (`docs/deployment_backlog.md`, filed S21-N) seeds `ICHRA_RATE_SOURCE_ENV = 'STAGING'` for any installation that predates S21-L's `DatabaseInitializer` seed — every existing installation, since none of them re-run that path. **Applied to local dev only, per this session's own record — not production.**
- **Production still needs, in order:** (1) the `D-88` insert, so `RateSourceEnvResolver` stops falling to its logged error-path default on every call; (2) a release carrying every commit after `v0.91.05` — all 23 commits listed in §1, none of which are in production yet.
- **ICHRA entitlement was granted to agency 14 on local dev this session** — reported, not independently queried (no database access this run) — which is LA-18's confirm-before trigger firing, deliberately, to make the market-token investigation possible at all. Not a production action.

---

## 9. Next

**Recommended: freeze the ICHRA_MARKET figures into the payload, the same way age-band premiums already are — before the next release, not after.**

Reasoning: every other ICHRA figure this session touched or shipped (age bands, T171's scenarios, T172's comparison) is a frozen, point-in-time read of `payload_json`, matching this project's own T162 design rule. `putIchraMarketTokens` is the one remaining live query, and it is live on the public, unauthenticated proposal page — the same page LA-18 just accepted the risk of showing staging data on. A sent proposal's plan/carrier counts and premium floors can silently change (or silently go from populated to empty, or the reverse) after the employer has already opened the link, for reasons having nothing to do with anything the recipient did. This is a small, well-scoped piece of work (mirror `attachAgeBandSnapshot`'s existing freeze pattern) and it closes the one structural inconsistency this session's own market-token investigation surfaced but did not fix.

**Also outstanding, lower urgency, independent of each other:** the three stale-token surfaces named in §5 (`ProposalAiBuilder.java`'s system prompt, `SKILL.md`'s token table, the merge-token popup's missing `PRIMARY_COLOR`/`ACCENT_COLOR`); the `ICHRA_PLAN_LANDSCAPE_TABLE`/`ICHRA_AFFORDABILITY` fixture and dropdown decisions in §5; and the production release itself (§8). **The three SWBD emails to Forrest remain unsent** — the longest-standing open item in this project, predating this session, gating the top-ranked build items in `docs/business/swbd_premiumpath.md`'s own sequencing — and nothing in this session changed that or depended on it. It is named here because it is real, outstanding, and unrelated to everything else in this list, not because this session has new information about it.

---

## 10. SQL close-out audit

- **No SQL was run or executed by any run this session.** No database was touched by any S21 sub-run — confirmed across every individual close-out's own compliance statement.
- **SQL was produced as text, twice, never as a file:** S21-N's `D-88` entry (`docs/deployment_backlog.md`) — an idempotent `INSERT INTO constant (name, value) VALUES ('ICHRA_RATE_SOURCE_ENV', 'STAGING') ON DUPLICATE KEY UPDATE value = VALUES(value);` seed, plus the corresponding `UPDATE constant SET value = 'PRODUCTION' WHERE name = 'ICHRA_RATE_SOURCE_ENV';` flip statement for when production access is enabled. Both are Kevin's to run; neither was executed by this session.
- **No `.sql` file was created or modified this session.** No migration gained an `INSERT INTO constant` — the rule held throughout.
- **Orphaned `.sql` files:** none created, none found.
- **Current highest migration version:** **V091**, unchanged all session (re-confirmed this run via `ls docs/migrations/`).
- **Pending deployment, schema-wise: nothing.** V091 is already live in production (confirmed by direct `schema_version` query, per `migration_tracker.md`'s own row, corrected S21-B this session — see §6). What is pending is code, not schema: all 23 commits in §1, plus the `D-88` constant seed (data, not schema).
- **Schema described but not scripted:** none this session.

---

## 11. Code-verified vs. runtime-verified

**Code-verified by every individual run this session, consistently:** the entire `RateSourceEnvResolver` build-out (S21-L/M), the T171/T172 corrections (S21-D/F/G), the merge-token popup gating (S21-H), the three ICHRA fixture pages (S21-J), the `D-88` entry's column-by-column verification against the `constant` table's real DDL (S21-N).

**Reported to this session as runtime-verified (browser walks on local dev, by Kevin, between sub-runs — not performed or independently re-confirmed by any Claude Code turn this session):**
- T169's dropdown-preservation fix, both the update and create paths.
- T171's contribution scenarios, both AGE_BAND and RANGE fidelity modes, including the corrected group-total contribution and net columns.
- T172's group comparison, including the corrected per-employee-to-group-total arithmetic.
- The `ICHRA_MARKET` page rendering with live market figures (once entitlement was granted — see §7).
- The merge-token popup rendering, gated, without breaking Proposal Settings.
- The `DATE_CREATED` walk that closed §6 item 3 (no cache defect).

**Not runtime-verified, and named as open work rather than claimed:** the market-figure freeze recommended in §9; the three stale-token-surface fixes in §5; production deployment of anything in §1 or §8.
