# Session Close-out — 2026-07-31, Session 4

**Branch:** `refactor/modernize-architecture` · **Pushed through:** `bbc6519`
**Scope:** the ICHRA hub Design Advisor card — copy, gate, and (missing) chatbot entry point — through
T57/T58/T52, verified end to end.

⚠️ **Session-3's close-out recorded "Pushed through: `682bc8f`" but that was not HEAD when this session
started.** `git log --oneline 682bc8f..HEAD` shows two commits ahead of that record before this session's
own work begins:

| Commit | What it was |
|---|---|
| `e128fbc` | The session-3 close-out doc's own commit — expected; a close-out necessarily lands one commit after the state it describes. |
| `8e4fea4` | **Not recorded anywhere.** `Update claude_memory.md current state for the ICHRA epic` — a session-close doc-hygiene commit (per its own message: "Session-close ritual per CLAUDE.md"), same author, same day, 14 minutes after `e128fbc`. It refreshed `claude_memory.md`'s Current State block, which had drifted (latest migration read V075, now V081; latest release read v0.73.02 unverified, now v0.78.01 verified). |

This is exactly the failure this section exists to catch: `8e4fea4` was real, useful, already-pushed work
that no close-out ever listed. It is not part of this session's shipped work — it predates this session's
first commit — but it belongs in the historical record, so it is named here rather than silently absorbed
into the count below.

---

## Shipped

This session's own commits, `834daac..bbc6519`, read from `git log --oneline 8e4fea4..HEAD`:

| Commit | What |
|---|---|
| `834daac` | **T55.** Hub Design Advisor card: `Coming` badge removed, copy corrected to describe an education-with-citations tool (not "guided contribution-strategy recommendations"), boundary line added, card wired to open the chat assistant via `onclick="toggleChatbox()"`. |
| `9058a56` | Record T55 fix in `project_backlog.md`. |
| `0f631c2` | **T55a.** The card from `834daac` was unconditionally clickable but the widget wasn't unconditionally present — gated the card on the same `chatbotEnabled && (...)` expression `navbar25.jsp` uses for the widget itself, restoring `Coming`/`disabled` when it's false. |
| `a279620` | Log **T57** (skill stays `is_admin_only=1` after the hub widens) in the backlog. |
| `d83b98f` | **Demo-path role walk finding.** Fixed a real defect it caught: the hub's Rate Cache Admin card rendered "Live" for every role, but the servlet redirects any non-PSP-admin to `/` — a dead click. Card gated on `sessionScope.isPspAdmin`. |
| `668c4d5` | Add `docs/analysis/ichra_demo_path_role_walk.md` — the role-shape walk (external agency users hold neither PSP nor BPO session flags) and the surface-by-surface pass/inert/unreachable table. |
| `5517252` | Log **T58** (no external agency role satisfies the chatbot gate) and **T59** (two hub cards share one URL) in the backlog. |
| `9b95979` | **T58.** `navbar25.jsp:408`'s chatbot include widened with `|| ichraAvailable`, reusing the same page-scoped value the file already resolved for the ICHRA nav entry — no second resolver call. |
| `a0b3cf2` | **T57.** `V082__ichra_design_advisor_non_admin.sql` — `chatbot_skill.is_admin_only` 1 → 0 for `ICHRA_DESIGN_ADVISOR`, keyed on `skill_name`. Registered in `migration_tracker.md` and `schema_version_migration.sql`. |
| `cb55c95` | Matched the Design Advisor card's gate to the widened widget condition (T55a's gate had gone stale the moment T58 landed). |
| `bd3e420` | Record T52's exposure change and close T57/T58 in the backlog. |
| `14b5819` | **Citation-path verification.** Checked whether leaving the `ichra_design` KB `ADMIN_ONLY` (deliberate, in V082) actually costs a role-2 caller its citations. It does not: `ChatAssistant.executeSkill` sends only `skill.getSystemPrompt()` + the question — no KB retrieval on the matched-skill path at all. Citations are 14 literal `Source:` lines inline in V080's `system_prompt`. Corrected T53's "current exposure: none" premise, which rested in part on the admin-only flag V082 removed — the verdict survives on the KB-eligibility guard alone, but is now single-guarded. |
| `4ea160d` | **T52.** `ChatAssistant.executeSkill`'s text-only branch now uses the skill's configured `model`/`max_tokens` when both are set (non-blank model, `maxTokens > 0`); otherwise runs the historical call verbatim. Reused the existing `ClaudeApiService.ask(String, List, String, int)` overload — no service-layer change. `./mvnw compile` clean. |
| `bbc6519` | Resolve T52 in the backlog, naming which seeded skill's behaviour actually changes. |

Every hash above was read from `git log`, not carried over from any prompt.

## In flight

**None. The working tree is clean.** `git status --short` returned nothing at the start of this run and
again immediately before this file was written.

## Decisions made

1. **Q1 answered yes** — the design advisor is agent-facing, not PSP-admin-only. Item 12's stated
   agent-visible outcome (*"Does my client's dental plan kill the QSEHRA?"* — answered with citations,
   in seconds) is not achievable by an audience that cannot reach the tool. Closed **T57** and **T58** as
   build items rather than won't-fix.
2. **`navbar25.jsp`'s widening kept `chatbotEnabled` as a common factor, deviating from the prompt's
   authored spec** (`(existing) || (ICHRA expression)`, no shared factor). The literal form would render
   the chat widget for ICHRA-entitled users on an installation with no Anthropic API key configured —
   `chatbotEnabled` derives from `AppConfig.hasAnthropicApiKey()` — producing a live-looking widget where
   every question fails. **The delivered form (`chatbotEnabled && (roles || ichraAvailable)`) is the
   correct one; the authored spec was wrong on this point.** Both forms are strictly widening over the
   pre-T58 condition, so the deviation cost nothing else.
3. **The `ichra_design` knowledge base stays `ADMIN_ONLY`.** Verified, not assumed: it costs a non-admin
   caller nothing, because `ChatAssistant.executeSkill` consults no KB on the matched-skill path — see
   `14b5819` above.
4. **T52 fixed defensively.** A skill that configures nothing (`model` null/blank or `maxTokens <= 0`)
   is byte-identical to its pre-fix behaviour; only a skill that configured both gets them honoured.

## New assumptions (technical, not legal)

- **Citations on the matched-skill path are model output, not structurally produced.** Nothing in code
  extracts or validates a citation; the mechanism is boundary 6 of the `system_prompt`
  ("CITE EVERY SUBSTANTIVE ANSWER") plus 14 literal `Source:` lines written inline. Reversal cost: none —
  but it means citation quality and presence are a **prompt property**, not a code guarantee, and nothing
  would catch a drift in either.
- **`ChatbotSkill.maxTokens` is a primitive `int`**, not `Integer` — "unset" can only ever present as
  `0`, never `null`. T52's defensive guard (`skillMaxTokens > 0`) depends on this being the only way a
  skill can fail to configure a token budget. Reversal cost: low, but the guard would need rewriting if
  the column type ever changed.

## Open questions raised

- ⚠️ **The one thing static analysis could not close.** No one has yet asked the design advisor the
  dental/QSEHRA question from a role-2 agent login on an installation with V082 applied and the new WAR
  deployed, and confirmed the reply names a source document. Every check this session ran was static
  (code reading) or ran as PSP admin; this is the first real runtime observation of the shipped feature.
- **T53 is now single-guarded.** Its "current exposure: none" verdict rested on two independent guards
  (`is_admin_only` and KB non-admin-eligibility); V082 removed the first. The verdict still holds on the
  second alone, but flipping `ichra_design` to non-admin visibility — the obvious-looking move if someone
  later wants "better citations" — is now the one change that would open T53's trap for real. Recorded in
  `project_backlog.md`'s T53 row and in the role-walk doc.
- **T56** — banner/disclaimer triplication across `illustration25.jsp`/`groupConversion25.jsp`. Still
  open, still unfixed, still explicitly out of scope of every run this session (ships alone).
- **The wider open backlog**, read from `docs/analysis/project_backlog.md` directly rather than carried
  forward. ICHRA/chatbot-epic-adjacent items still open, one line each:
  - **T39** `HealthSherpaService` needs multi-applicant support — 💡 Backlog
  - **T41** `fields` param to trim quote payload — 💡 Backlog
  - **T42** `include_non_enrollable_offex` semantics unverified — 💡 Backlog
  - **T44** LCSP/benchmark silver computed off-exchange-only — 💡 Backlog
  - **T47** representative-ZIP vs. HealthSherpa for low-containment counties — 💡 Backlog
  - **T48** `source_env` is advisory only, not a partition — 💡 Backlog
  - **T50** Tarrant County's representative ZIP is DFW Airport — 💡 Backlog
  - **T51** `ProposalSettings.updateScope` lets an ICHRA section revert to `scope='ALL'` — 📋 Planned, HIGH
  - **T53** unguarded path around a skill's own guardrails — 💡 Backlog (see above)
  - **T54** dead multi-word/hyphenated `knowledge_chunk` keywords, existing KBs unsurveyed — 📋 Planned
  - **T56** banner/disclaimer triplication — 💡 Backlog (see above)
  - **T59** two hub cards share one URL — 💡 Backlog

  The backlog also carries a larger set of open items unrelated to the ICHRA epic (legacy import/staging,
  security hardening, GUI modernization, and others — T5 through T38-range plus T45/T46/T49) — not
  enumerated here as out of this close-out's scope; read `project_backlog.md` directly for those.
- **V082's blast radius**, recorded as an accepted, one-`UPDATE`-reversible deviation from build rule 2:
  PSP users (`CHATBOT_ALL_USERS`) and BPO users (`CHATBOT_ALL_BPO_USERS`) can now newly match
  `ICHRA_DESIGN_ADVISOR` where those toggles are on, because `is_admin_only` is a property of the skill,
  not of ICHRA entitlement. **Zero in practice wherever both toggles are off** — verify their current
  setting before treating this as inert on any given installation.

## Contradictions found

- ⚠️ **The stale HEAD in the session-3 handoff** — see the top of this document. `682bc8f` was not the
  actual last-pushed commit by the time session 4 began; `8e4fea4` landed after and was never recorded.
- **T53's "Current exposure: none" premise, corrected in-flight (`14b5819`).** It rested on two guards;
  V082 (this session, `a0b3cf2`) removed one. The correction is recorded directly in T53's backlog row
  rather than left implicit.
- **`docs/swbd_ichra_build_plan.md` item 12's Gate row and one sentence in its mechanism-findings
  paragraph were factually wrong** after this session's work and are corrected below (§3 of the run this
  document follows) — item 12 no longer requires `is_admin_only = 1`, a UI entry point now exists, and
  T52 is fixed rather than deliberately deferred.

## Backlog logged this session

- **T57** — logged `a279620`, closed `bd3e420` (via `V082`, `a0b3cf2`).
- **T58** — logged `5517252`, closed `bd3e420` (via `9b95979`).
- **T59** — logged `5517252`, still open.
- **T52** — pre-existing (logged prior session), closed this session (`bbc6519`, via `4ea160d`).
- Highest backlog item is still **T59** — no new item number was consumed this session beyond what the
  prior session's items already reserved.

## Next

**Recommended next step: run the one runtime check nobody has run.** Deploy the WAR carrying this
session's commits together with **V082** applied, log in as a role-2 agent on an `ichra_enabled` agency,
ask the canonical dental/QSEHRA question, and confirm the reply names a source document. This is the only
gap static analysis in this session could not close, and it is cheap — one login, one question.

**The three pieces of this session's work are a single deploy unit, not three independent ones:**
- **V082 without the WAR** grants no entry point — the skill would be reachable in principle but no
  session ever gets past `navbar25.jsp`'s pre-T58 gate to try.
- **The WAR without V082** grants entry to a skill still marked `is_admin_only = 1` — an agent reaches
  the chat assistant and finds the ICHRA skill silently stripped from its own matching pool.
- **Either without T52's fix** leaves the advisor answering on Haiku/1024 rather than the Sonnet/2048
  V080 configured — the citation-truncation risk this session's urgency argument was built on.

All three ship together or the advisor does not work as specified.

---

## SQL close-out audit

**`V082__ichra_design_advisor_non_admin.sql` is the only SQL produced this session.** Verified by reading
the file directly (`docs/migrations/V082__ichra_design_advisor_non_admin.sql`), quoted in full:

```sql
UPDATE chatbot_skill
   SET is_admin_only = 0
 WHERE skill_name = 'ICHRA_DESIGN_ADVISOR';

CREATE OR REPLACE VIEW schema_info AS
SELECT 'V082' AS version, '2026-07-31' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V082',
        'Make ICHRA_DESIGN_ADVISOR available to non-admin callers (chatbot_skill.is_admin_only 1 -> 0)',
        'V082__ichra_design_advisor_non_admin.sql',
        NOW());
```

Three statements. The `UPDATE` is keyed on `skill_name` alone (rule 4 — no hardcoded `psp_id`, no id);
its own header notes V065's unique index is `(psp_id, skill_name)`, so this updates one row per PSP —
one row on every current installation, since only `psp_id=4` is seeded (V080).

**In a versioned migration:** yes — all three statements, `V082__ichra_design_advisor_non_admin.sql`.
No other commit this session touched a `.sql` file: `834daac`, `9058a56`, `0f631c2`, `a279620`,
`d83b98f`, `668c4d5`, `5517252`, `9b95979`, `cb55c95`, `bd3e420`, `14b5819`, `4ea160d`, `bbc6519` are all
JSP, Java, or Markdown.

**Orphaned `.sql` files.** `docs/migrations/` directly: **59 files** — 58 versioned (V025 through V082,
unbroken) plus the one known pre-existing orphan, **`seed_ndt125_questionnaire.sql`** (T38, logged
2026-07-29). Still there; nothing new joined it this session. Also checked and excluded: a local
`release/` folder at the repo root containing `ROOT.war` and six old migration copies (V066-V071, dated
July 9-10) — this is explicitly gitignored release-staging debris (`.gitignore:69-70`), not tracked by
git, and pre-dates this session by weeks. Not a repo orphan.

**Current highest version: V082**, read from `docs/analysis/migration_tracker.md:19`
(`## Current Highest Version: V082`) and cross-checked against the directory listing — they agree. V082
is also registered in `docs/schema_version_migration.sql`, tail entry correctly terminated with `;`.

**Pending deployment.** Per the tracker's per-environment columns (Production / beta_ssa (work) /
beta_ssa (home) / dev_ssa), **V079 through V082 are all `⬜`** on every environment — four versions
unapplied everywhere. Every version through V078 shows `✅`. **V082 must ship with V079-V081**, not as a
standalone release, since all four are unapplied and this session's WAR changes assume V082 is present.

**Schema described but not scripted: none.** This session's new/changed documents (this close-out and
the two `swbd_ichra_build_plan.md` corrections) name no table or column without a migration behind them.
`chatbot_skill.is_admin_only` already existed (V046); V082 only changes a value in an existing column.

**Nothing was executed against any database.** No database client was invoked at any point this session;
no connection was opened; every SQL statement named above exists only as file content. Commands run were
`git` (read-only except `add`/`commit`/`push` on the paths named in each run), `./mvnw compile`, and
read-only file inspection (`grep`, `ls`, `find`).

---

**Related:** `docs/swbd_ichra_build_plan.md` (item 12 corrected this session) ·
`docs/analysis/ichra_demo_path_role_walk.md` (new this session, `668c4d5`) ·
`docs/analysis/project_backlog.md` (T52/T55/T55a/T57/T58 closed, T59 open, highest item T59) ·
`docs/analysis/migration_tracker.md` (V001-V082) ·
`docs/session_closeout_2026-07-31_session3.md` (the prior session, recorded through `682bc8f`, actual
final commit `8e4fea4`)
