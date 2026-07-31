# Session Close-out — 2026-07-31, Session 2

**Branch:** `refactor/modernize-architecture` · **Pushed through:** `77eb56c`
**Scope:** Item 12 (ICHRA/QSEHRA design advisor, V080) → its guardrail-split fix → its LA-13
disclosure fix → item 10 (setup checklist content) → two small unrelated fixes → this close-out.

---

## Shipped

| Commit | What |
|---|---|
| `e25ee4e` | Item 12: ICHRA/QSEHRA design advisor. Seeds `ICHRA_DESIGN_ADVISOR` (`chatbot_skill`, `is_admin_only=1`) + a 20-chunk `ichra_design` knowledge base — **V080**. Same commit closes the prior run's doc loop: S2 revised in place (snapshot, not re-derive), items 1–9 struck through with hashes, item 4 replaced with the reference-data note, V077/V078 production status corrected, **LA-13** added, **T50–T54** logged. |
| `ae24e89` | Record item 12's own commit hash (`e25ee4e`) in the build plan. |
| `4775252` | Item 10: ICHRA setup checklist content — 19 tasks across five groups, delivered as `docs/business/ichra_setup_checklist.md` (a document for admin-UI entry, not a migration). |
| `5e431e7` | Record item 10's commit hash (`4775252`) in the build plan. |
| `5ea4dbd` | ICHRA hub: mark Age-Band Net Cost and Affordability Threshold live (both shipped in `0b4711b`/`4556ecd` but their cards still read `Coming`). T55 logged for the Design Advisor card, left `Coming`. |
| `77eb56c` | Flag the uncited ICHRA 90-day notice assertion in `ichra_administration_scope.md` Phase 2 against LA-08 — assertion kept verbatim, bracketed with its status. |

Two mid-run edits (not separate shipped commits, folded into `e25ee4e` before it landed) are worth naming because they changed what shipped: the design advisor's guardrail/content split (refusal boundaries duplicated into KB chunks) and its LA-13 disclosure fix (register identifiers and SSA-exposure characterizations stripped from agent-facing output). Both are reflected in `e25ee4e`'s content above, not as separate hashes.

## In flight

**None. The working tree is clean.** `git status --short` returns nothing after this session's three commits landed and pushed. Verified directly before writing this file, not asserted.

## Decisions made

1. **LA-13 — SSA's assumptions register is internal work product**, not disclosed to partner agencies through SSA-built tools. As first built, the design advisor's `system_prompt` would have told an SWBD agent, by default, that Texas TPA licensing (LA-10) is "unresolved and may already apply" and that SSA's substantiation approach (LA-01/02/03) is unreviewed, with LA-01 flagged irreversible once a dollar moves. All `LA-NN` identifiers and exposure characterizations were stripped from agent-facing output (the `system_prompt` and all 20 chunk `title`/`section`/`content` fields — verified by positional sweep, zero hits); the settled-versus-unfinalized distinction and every refusal boundary were retained in full, restated as "SSA has not finalized a position on this — take it to SSA directly." Reversal cost is **asymmetric**: restoring disclosure is a data edit to V080; a disclosure already made to a partner cannot be withdrawn. `knowledge_chunk.source_citation` still carries the LA references — verified below that this field never reaches the model.
2. **Refusal boundaries duplicated into KB chunks.** `ChatAssistant.executeSkill` injects no knowledge-base content on the matched-skill path — a matched skill's `system_prompt` is its entire context. Skill selection needs ≥2 distinct `trigger_keywords` matched by substring; a question scoring 0–1 falls through to KB search with none of the skill's boundaries applied. Two boundary chunks were added and the LA-07 QSEHRA-runway chunk was scope-fenced in its own body, so a notice-timing question that never names ICHRA (e.g. "how long before the plan year do I have to tell employees?") cannot surface the QSEHRA answer as though it covered ICHRA on the fallback path either.
3. **Item 10 ships as a document, not a migration.** Verified 2026-07-31 that no migration in the repo (V025–V080) has ever created a task sequence — zero inserts into or mentions of `task`/`tasksequence`/`tasksequencetable`/`requiredtasklist` across all of `docs/migrations/`. Build rule 5 names task sequences as Kevin's reference data, entered through the Sequence Builder admin UI. Content delivered fully specified in `docs/business/ichra_setup_checklist.md`.
4. **No LA-14.** The register's own test (`legal_assumptions.md`, "How to use this register") requires **both** a reading of a statute/regulation/agency guidance **and** an expensive-to-reverse decision. Item 10's one real judgment call — mark undetermined steps rather than fabricate a date — rests on no legal reading at all, and is structurally forced besides: no `Task`, `TaskSequenceTable`, `TaskSequence` or `ToDo` field can hold a due date, so the notice step is incapable of making a timing assumption in the first place.
5. **The Design Advisor hub card stays `COMING`.** Verified no dedicated servlet exists for it (`grep` for `DesignAdvisor` across `src/main/java`: zero hits) — V080 deliberately added no UI entry point, reachable only through the existing `/ChatAssistant` surface. Making the card live would mean inventing a route, which is a product decision, not a label fix; the card's copy ("Guided contribution-strategy recommendations") also doesn't match what shipped, since the advisor's hard boundaries explicitly refuse plan/strategy recommendations. Logged as **T55**, not fixed.

## New assumptions (technical, not legal — none are `LA-NN` entries)

- **`ChatAssistant.executeSkill` injects no KB content.** For a text-only skill it calls `ClaudeApiService.ask(null, skill.getSystemPrompt(), question)` — verified by reading the method directly. The `system_prompt` is a matched skill's entire context.
- ⚠️ **The two keyword matchers behave oppositely.** `ChatbotSkillDAO.scoreKeywordMatch` matches skill `trigger_keywords` by **substring** `contains()` on the raw message. `KnowledgeSearchService.scoreChunk` matches `knowledge_chunk` keywords by **exact token equality** against a tokenized query (`tokenize` strips to `[a-z0-9\s]` and splits on whitespace). A chunk keyword containing a space or hyphen can therefore never match anything — five such dead keywords were found and fixed in V080's LA-07 chunk alone (`45 days`, `90 days`, `short first year`, `eligibility date`, `la-07`). Reversal cost: config/data edit per chunk, but the mechanism-wide exposure is unmeasured — see T54 below.
- **No due-date or offset field exists** on `Task`, `TaskSequenceTable`, `TaskSequence`, or `ToDo` — verified by reading all four entity classes column-by-column. This satisfies LA-08 (ICHRA notice timing undetermined) **structurally rather than by discipline**: no field can hold a computed, defaulted, or offset date, so none can leak into a compliance checklist.
- **`knowledge_chunk.source_citation` is never copied into model context.** Verified: `KnowledgeSearchService.toChunks` copies `kbId, kbLabel, title, url, content, section, category, keywords` onto the in-memory `Chunk` object — `source_citation` is not among them — and `buildContext` emits only `kbLabel | title > section` + `content`. The field reaches only the Knowledge Manager admin UI, which this session confirmed is itself gated `isPspAdmin()` on both `doGet` and `doPost` (its only two entry points) — the LA reference trail in `source_citation` is not agent-reachable by any path found.
- **`Task.description` is `varchar(200)`.** All 19 item-10 checklist task descriptions were length-checked against this limit before being written into the delivery document (longest: 185 characters).

## Open questions raised

- ⚠️ **T52 gates widening item 12's visibility.** The text-only skill path ignores `chatbot_skill.model` and `max_tokens` — `ChatAssistant.executeSkill` routes through a 3-arg `ClaudeApiService.ask` overload that hardcodes Haiku 4.5 / 1024 tokens regardless of the row's configured values. Admin-only today (no exposure); **fix before item 12's audience widens past PSP admin.**
- **Is the Knowledge Manager UI PSP-admin-only? — Verified this session: yes.** Both `KnowledgeManager.doGet` and `KnowledgeManager.doPost` (its only two entry points) redirect to `ViewHome25` unless `local.isPspAdmin()`. LA-13's decision to leave the register in `source_citation` is therefore sound on the evidence found — no non-admin path to that field exists.
- **S3 still undecided** — verified open in `swbd_ichra_build_plan.md` (header carries no ✅ RESOLVED marker). Blocks item 13, and is a rule-3 schema exception that gets more expensive after A5 reads it.
- **T54's survey has not been run.** `summit_official`/`summit_supplemental` are reachable by non-admin users today (`KnowledgeSearchService.getEligibleKBs(false)` returns exactly those two) and have not been checked for the same dead multi-word/hyphenated keyword pattern found in V080. A live feature may be degraded, unmeasured.
- **Item 4's Gate 0 probe was cancelled**, not merely deferred — build plan item 4 now reads as a pure reference-data note (Kevin creates ICHRA/QSEHRA LOS, ServiceItem, PlanType and priced ServiceModule/RateTable rows via the admin UI whenever he tests), carried forward from the prior session's edit and unchanged this session.

## Contradictions found

- `ichra_administration_scope.md` Phase 2 asserted the 90-day/coverage-begins ICHRA notice rule flatly while LA-08 disowns that exact assertion as uncited — **fixed this session** (`77eb56c`).
- The Design Advisor hub card's copy promises contribution-strategy recommendations, which the advisor is built to refuse — **T55, not fixed**.
- Hub cards read `COMING` for two shipped features (Age-Band Net Cost, Affordability Threshold) — **fixed this session** (`5ea4dbd`).
- The project-knowledge handoff reported the highest backlog item as T48; verified at this session's start it was already **T49** — a doc external to this repo, stale before this session began.

## Backlog logged (T50–T55, read from `project_backlog.md`)

- **T50** — Tarrant County 48439's representative ZIP is unusable (DFW Airport)
- **T51** — `ProposalSettings.updateScope` lets an ICHRA section be reverted to `scope='ALL'`
- **T52** — Matched chatbot skills silently ignore their configured `model` and `max_tokens`
- **T53** — Chatbot skills have an unguarded path around their own guardrails
- **T54** — Multi-word and hyphenated `knowledge_chunk` keywords are silently dead — existing KBs unsurveyed
- **T55** — ICHRA hub "Design Advisor" card fronts a page that does not exist, and its copy does not match what shipped

## Next

- **Item 11** — gated on the email to Forrest asking for three to five renewing groups. **Still not sent** — verified in `swbd_ichra_build_plan.md` §6, still `❌ not sent`.
- **Item 13** — needs S3 decided.
- **T52** — before item 12 widens beyond PSP admin.
- **T54 survey** — read-only (`SELECT kb_id, chunk_id, title, keywords FROM knowledge_chunk WHERE keywords REGEXP '[ -]'`), may be fixing a live degradation.
- ⭐ **Kevin's, unchanged and still unchased:** production allow-listing (still `❌ none` per §6 — blocks two shipped features from client-facing use); D-86 and D-87 (confirmed present in `docs/deployment_backlog.md`); the ICHRA/QSEHRA LOS rows and the ICHRA Illustration section scoped to them (item 4); entering the item 10 checklist through the Sequence Builder UI (`docs/business/ichra_setup_checklist.md`); **the two SWBD emails, neither ever sent.**
- ⚠️ **Release `v0.80.00`** — `ROOT.war` plus **both** `V079__proposal_ichra_snapshot.sql` and `V080__ichra_design_advisor_skill.sql`. **V079 was never released**, so this is not `v0.79.00`. Local `git tag` shows nothing past `v0.71.06` and is stale by design (per `CLAUDE.md`) — not used here as evidence of anything; the actual current release must be read from the GitHub Releases page, which this session did not check.

## SQL close-out audit

**V080 is the only SQL produced this session**, committed in `e25ee4e` as `docs/migrations/V080__ichra_design_advisor_skill.sql`. Structure: 1 `INSERT IGNORE` (skill row), 1 `INSERT IGNORE` (KB registry row), 20 guarded `INSERT ... SELECT ... WHERE NOT EXISTS` (chunks), 1 `CREATE OR REPLACE VIEW schema_info`, 1 `INSERT IGNORE INTO schema_version`. No `INSERT INTO constant`.

Item 10 (checklist content) and the small-items sweep (hub cards, Phase 2 flag) produced **no SQL at all** — stated explicitly, not by omission.

Verified this session:
- `migration_tracker.md` — `## Current Highest Version: V080`; both `V079` and `V080` rows present, Production column `⬜` for both (unapplied everywhere).
- `docs/schema_version_migration.sql` — both `V079` and `V080` registered, tail entry correctly terminated with `;`.
- `docs/migrations/` — V079 and V080 both present as files; the only non-versioned file present is `seed_ndt125_questionnaire.sql`, a pre-existing orphan already tracked as **T38** from a prior session, not new this session.
- **Nothing was executed against any database** — no database client was invoked at any point this session; every SQL statement exists only as file content.

---

**Related:** `docs/swbd_ichra_build_plan.md` (live sequencing) · `docs/analysis/legal_assumptions.md` (LA-01–LA-13) · `docs/analysis/project_backlog.md` (T1–T55) · `docs/analysis/migration_tracker.md` (V001–V080) · `docs/business/ichra_setup_checklist.md` (item 10 content)
