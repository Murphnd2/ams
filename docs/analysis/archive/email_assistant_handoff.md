# Email Drafting Assistant — Project Handoff & Status

**Last updated:** 2026-05-05 (end of round 4)
**Session continuity:** This document captures everything needed to continue work in a new chat session. Previous sessions are compacted; full transcripts are referenced where relevant.

---

## Executive Summary

The AMS email drafting assistant is **functional end-to-end on `/EmailDraftTest`**. A user (Deb, Delight, Kevin) can paste an inbound email, click Generate, and receive a structured draft response with the body, encryption flag, escalation flag, and completeness self-checks. All major escalation paths (HARD, SOFT_CONF, SOFT_JUDG) work. PHI detection works.

**The current open issue is calibration quality.** The drafter follows the rules it was given, but produces drafts that are too literal — it doesn't infer well from real participant emails. This is the next session's primary work.

**Round 5 (Outlook taskpane) is on hold** until calibration is good. The architectural payoff of putting the system prompt in the DB is exactly to support this iteration loop without redeploys.

---

## What's Been Built (Rounds 1-4)

### Round 1 — Architecture decisions
- **Path B chosen** over Microsoft Copilot — AMS-grounded drafter, multi-tenant ready for DataPath partnership.
- **Four knowledge domains** in MySQL: `style_voice` (always-load), `federal_rules` (search), `ssa_business` (search), `summit_supplemental` (search). Plus three platform JSONs preserved (`summit_official`, `proposal_page_builder`, `automation_email_builder`).
- **Per-database PSP architecture** — each AMS install has its own `beta_ssa` DB with PSP_ID always 4. No `psp_id` column on `knowledge_chunk` or `knowledge_base`.
- **Encryption mechanics — per-user defaults:**
  - **Deb**: encrypt-by-default; types `{SSA}` in subject to opt out.
  - **Kevin & Delight**: open-by-default; type `Secure:` in subject to opt in.
  - The drafter outputs a flag (`NONE` / `RECOMMENDED` / `REQUIRED`); the future taskpane will translate per-user posture.
- **Escalation states**: `NONE` | `SOFT_CONF` (knowledge gap, holding ack + question for Kevin) | `SOFT_JUDG` (full draft + flag for review) | `HARD` (no draft, attorney/regulator/complaint).
- **AI model**: `claude-sonnet-4-20250514` (existing proven Sonnet 4 string), editable via Skill Manager.

### Round 2 — Schema and service layer (V063)
- Created `knowledge_base`, `knowledge_chunk`, `knowledge_chunk_history` tables.
- `KnowledgeBase` / `KnowledgeChunk` / `KnowledgeChunkHistory` JPA entities + 5 enums.
- `KnowledgeBaseDAO` (read-only registry) + `KnowledgeChunkDAO` (CRUD with audit-on-write).
- `KnowledgeSearchService` extended to dual-load DB + JSON sources, with `getAlwaysLoadChunks()` and `search()` methods.
- DatabaseInitializer seeds the KB registry on fresh installs.
- 3 callers updated to pass EMF: ChatAssistant, AutomationAiBuilder, ProposalAiBuilder.

### Round 3 — Knowledge Manager UI
- `controller/assistant/KnowledgeManager.java` with full CRUD + bulk import + reload.
- `WEB-INF/view/a/assistant/knowledgeManager25.jsp` with KB strip, filters, card grid, create/edit/history modals, bulk import modal.
- `KnowledgeChunkDAO` gained `activate()` and `getHistoryByChunkId()`.
- Navbar link added under PSP Admin → Business Efficiency.
- **Bugs found and fixed:**
  - JSP EL operator bug (`!==` → `ne`).
  - Bulk import wasn't all-or-nothing (rewrote with two-pass validation + outer transaction + `createNoTx()`).
- **UX polish items logged** but not addressed:
  - Active checkbox should default to checked on create.
  - Optional confirm/note prompt on deactivate/activate.
  - "When" column in history viewer doesn't render timestamp (JS template literal issue).
  - JSON-source KBs show "0 active chunks" (DB count, technically correct but cosmetically misleading; chose Option A — leave as-is).

### Round 4 — EmailAssistantDraft servlet (V065)
- **Migration V065** seeded `EMAIL_DRAFT_ASSISTANT` chatbot_skill row with system prompt embedded. Added unique index `(psp_id, skill_name)` for idempotency.
- **`ChatbotSkillDAO.getBySkillName(em, name, pspId)`** added.
- **`KnowledgeSearchService.Chunk.getKbId()`** getter added (needed by retrieval split helper).
- **`controller/assistant/EmailAssistantDraft.java`** — POST /EmailAssistantDraft. Flow: PSP Admin auth → resolve sender via `PersonDAO.getPersonByEmail` → `person.getEmployee().getEmployer()` chain (null-guarded) → `getAlwaysLoadChunks({"style_voice"})` + search across `{federal_rules, ssa_business, summit_supplemental}` → `applyRetrievalSplit(6, 3, 2)` → load system prompt from DB → assemble structured prompt → call Claude → parse JSON with retry → return structured response.
- **`controller/assistant/EmailDraftTest.java`** + **`emailDraftTest25.jsp`** — internal test page with form, results panel, color-coded badges (escalation / encryption / tone / completeness), "Use for Refine" button.
- **Bugs found and fixed:**
  - JSP include path bug (Claude Code guessed wrong path; corrected by reading existing JSPs).
  - **Lazy-init issue**: `KnowledgeSearchService` is initialized on-demand by ChatAssistant on first hit after Tomcat boot, not at boot via a listener. EmailAssistantDraft's preflight check returned `service_unavailable` when the service was uninitialized. Fixed to match ChatAssistant's lazy-init pattern.

### Content authored
- **Track A complete** — `style_voice_seed.json` (61 chunks). Imported. Sections: Structure (8), Tone & Voice (11), Length & Depth (6), Accuracy (5), Exclusions (5), Special Situations (5), PHI & Privacy (4), Calibration Examples (17 — 7 BAD, 10 GOOD, mostly verbatim from real Deb threads).
- **Track C v1 complete** — `federal_rules_fsa_seed.json` (43 chunks: 30 FSA + 13 DCAP). Includes 2025 + 2026 limits. Captures OBBBA changes (DCAP $5,000→$7,500). Status: **PENDING IMPORT** — file generated, not yet pasted into Bulk Import targeting Federal Benefits Rules KB.
- **Track B parked** — SSA business knowledge; to be authored collaboratively with Kevin's input (escalation chains, service-scope clarifications, internal procedures).

---

## Current State (verified working)

| Capability | Status |
|---|---|
| V063, V064, V065 migrations applied to production | ✓ |
| 7 KBs registered (4 DB-source + 3 JSON-source) | ✓ |
| Knowledge Manager UI fully functional (create/edit/deactivate/activate/delete/bulk import/history) | ✓ |
| Bulk import all-or-nothing rollback | ✓ |
| Style/voice content imported (61 chunks) | ✓ |
| Federal rules content imported (43 chunks) | ⚠ **PENDING — Kevin needs to import `federal_rules_fsa_seed.json`** |
| EmailAssistantDraft servlet functional | ✓ |
| EmailDraftTest UI functional | ✓ |
| HARD escalation correctly fires on attorney/DOL mentions | ✓ |
| SOFT_CONF correctly fires on knowledge gaps | ✓ |
| PHI detection (REQUIRED encryption) on prescription/pharmacy/date | ✓ |
| AMS sender resolution (graceful degradation when unknown) | ✓ |
| Lazy-init handles fresh boots without ChatAssistant warm | ✓ |

---

## Calibration Issues (open — next session priority)

Three issues found in metadata calibration during round 4 testing:

### Issue 1 — SOFT_CONF vs. SOFT_JUDG confusion
The exception-request test ("I missed the deadline by 3 weeks but had a family emergency") was tagged `SOFT_CONF` instead of `SOFT_JUDG`. The drafter knew the rule (90-day runout, no exceptions per IRS), confidently stated it, and correctly deferred to employer authority — but tagged itself as a knowledge gap. SOFT_CONF means "I lack information"; SOFT_JUDG means "I have information and produced a complete draft, but this case warrants Kevin's review."

**Fix queued (not yet applied):** Add to system prompt's ESCALATION DECISION section:
> Critical distinction: SOFT_CONF means you LACK information (rule not in knowledge, calculation unclear). SOFT_JUDG means you HAVE the information and produced a complete draft, but the inbound matches a category that warrants review before sending. If you have the rule and applied it confidently, the escalation is SOFT_JUDG, not SOFT_CONF. Discretionary asks ("can you make an exception"), retroactive corrections, and amount-based exception requests are SOFT_JUDG even when you state the rule clearly.

### Issue 2 — Completeness self-check too strict
The drafter is undermarking its own work. Bodies that clearly state "FSA," "90 days," "$2,500 from last year" are getting `planNamed=false` and `dateCited=false`. The self-check is being too literal — wants explicit phrases like "your FSA" rather than recognizing context.

**Fix queued:**
> Be generous with self-marks: planNamed=true if the plan/account type is clearly identified anywhere in the body (including via context like "your prescription claim" implying FSA). dateCited=true if any date or plan year is mentioned. nextStepsStated=true if the participant knows what to do next. Mark false only when the relevant element is genuinely absent and would have been useful.
>
> For HOLDING_ACKNOWLEDGMENT or HARD-escalation drafts, mark all completeness fields as true (the standard doesn't apply when no substantive draft was produced).

### Issue 3 — toneCalibrated false positives
Empty HARD-escalation bodies and reasonably-toned bodies are getting marked `not_calibrated`.

**Fix queued:**
> toneCalibrated values: "appropriate" is the default for any reasonable draft. "too_warm" or "too_curt" only when the tone clearly mismatches the inbound register. "not_calibrated" only when the tone is genuinely unprofessional, contradictory, or unfit for the audience. Empty bodies (HARD escalation) should be marked "appropriate" because no tone was generated to evaluate.

### **Issue 4 (CRITICAL) — Real participant emails produce poor drafts**
Kevin tested with a real FSA-related inbound. The result was not good: "claude didn't make correct assumptions about the communications and needs to infer better."

This is the highest-priority calibration item and is **substantively different from issues 1-3**. Issues 1-3 are metadata labels. Issue 4 is the **draft quality on real inputs**.

The current prompt teaches the drafter rules to follow but doesn't teach it **how to infer**. Real participant emails contain:
- An implicit question buried under a literal one ("can you tell me what's happening" often means "fix this for me")
- Emotional subtext that needs acknowledgment before substance
- Context the participant assumes Deb has (their plan year, their employer, what was discussed last week)
- A history of prior interactions the drafter should recognize but currently can't see

**Diagnosis is preliminary** — we don't yet have the specific email Kevin tested with. Next session should ask Kevin to paste the inbound + the bad draft so we can see exactly where inference broke down. Likely fixes will involve adding inference rules to the style/voice KB (a new section, "Inference & Reading Between the Lines") or sharpening the system prompt's instructions for handling ambiguity.

**Next session plan: see "Immediate Next Steps" below.**

---

## Immediate Next Steps for Next Session

### 1. Get Kevin's failed test case
Ask Kevin to paste:
- The exact inbound email he ran (anonymized if needed).
- The draft the assistant produced.
- His specific feedback on what the assistant got wrong (which assumptions were missed, what should have been inferred).

This is the single most important input. Don't proceed with prompt tuning until this is concrete.

### 2. Verify federal rules import
Before any further testing, confirm `federal_rules_fsa_seed.json` was imported. If not, that's the first action — pasting 43 chunks into Bulk Import targeting Federal Benefits Rules KB. Without this, the drafter is operating without federal rule grounding.

Verify via the KB strip: Federal Benefits Rules card should show 43 active chunks (not 0).

### 3. Apply the four calibration fixes
Edit the system prompt via Skill Manager → EMAIL_DRAFT_ASSISTANT → Edit. Apply Issues 1, 2, 3 above. Issue 4 will need a more substantive fix once we see the failure case.

### 4. Calibration testing methodology
Once the four fixes are applied, run a more rigorous test pass. Suggested test cases:
- A real Deb-style FSA denial question.
- A real claim-status question.
- An election change request (qualifying event).
- A leave-of-absence question.
- A frustrated-participant email (emotional subtext).
- A multi-issue inbound (3 distinct questions).
- An inbound from a real participant in the AMS DB (verify AMS context resolution).
- A first-time card use question (operational, not regulatory).

For each, capture the draft and Kevin's read of how well it matches what Deb would actually send. Iterate the prompt where drafts miss.

### 5. After calibration is good — round 5
Round 5 = Outlook taskpane. Pre-design notes from round 1:
- Read mode + compose mode entry points.
- `displayReplyForm({htmlBody})` for read mode.
- `setAsync()` for compose mode.
- Hosted on AMS Tomcat.
- Reuses existing add-in's auth.

Don't start round 5 until drafts feel right on `/EmailDraftTest`.

---

## Architectural Notes for Future Reference

### Lazy-init pattern
`KnowledgeSearchService` is initialized lazily by the first servlet that hits it (currently ChatAssistant or EmailAssistantDraft). Both servlets handle the null-attribute case by initializing on-demand. This is fragile — any future AI servlet must replicate this pattern. **Long-term cleanup**: add a `ServletContextListener` that initializes the service at boot. Logged but not blocking.

### Per-employer service scope
Kevin's COBRA correction surfaced this: "what SSA does" depends on which services the employer contracted for. SSA might administer FSA only, FSA + COBRA, FSA + COBRA + discrimination testing, etc. The corrected COBRA chunk handles this conditionally in prose, but the future drafter should ideally pull contracted services from AMS data per-employer (likely from the `Offering` or contract structure) and inject into the prompt context. **Round 4.5 or later candidate.**

### Annual update workflow
Federal rules content is time-bound. Each year when IRS publishes new limits via Rev. Proc., new chunks must be authored (or existing ones updated) for the new plan year. Currently manual. Could be automated (script that scrapes Rev. Procs. and proposes chunk updates) as future infrastructure work.

### JSP compile-time blind spot
JSPs aren't compiled at Maven build time — Tomcat compiles them at first request. Two bugs in this project surfaced from this (EL operator, missing include path). Workaround: explicitly instruct Claude Code prompts to copy include directives from existing reference JSPs rather than guess.

---

## Key Files Reference

### Source
- `src/main/java/net/superiorstate/ams/controller/assistant/EmailAssistantDraft.java` — main servlet
- `src/main/java/net/superiorstate/ams/controller/assistant/EmailDraftTest.java` — test page servlet
- `src/main/webapp/WEB-INF/view/a/assistant/emailDraftTest25.jsp` — test page UI
- `src/main/java/net/superiorstate/ams/controller/assistant/KnowledgeManager.java` — content management
- `src/main/webapp/WEB-INF/view/a/assistant/knowledgeManager25.jsp` — content management UI
- `src/main/java/net/superiorstate/ams/data/service/KnowledgeSearchService.java` — KB load/search
- `src/main/java/net/superiorstate/ams/data/dao/KnowledgeChunkDAO.java` — chunk CRUD
- `src/main/java/net/superiorstate/ams/data/dao/KnowledgeBaseDAO.java` — KB registry reads
- `src/main/java/net/superiorstate/ams/data/dao/ChatbotSkillDAO.java` — skill lookup (`getBySkillName`)
- `src/main/java/net/superiorstate/ams/model/general/KnowledgeBase.java`, `KnowledgeChunk.java`, `KnowledgeChunkHistory.java` — JPA entities

### Migrations
- `docs/migrations/V063__knowledge_base_tables.sql` — core schema
- `docs/migrations/V064__platform_json_registry_seed.sql` — platform JSON KB registry rows
- `docs/migrations/V065__email_draft_assistant_skill.sql` — chatbot_skill seed + unique index

### Content seeds
- `src/main/resources/knowledge/style_voice_seed.json` — Track A (imported)
- `src/main/resources/knowledge/federal_rules_fsa_seed.json` — Track C v1 (PENDING IMPORT)
- `src/main/resources/knowledge/summit_official.json` — Summit user guide (511 chunks, JSON-source)
- `src/main/resources/knowledge/proposal_page_builder.json` — JSON-source
- `src/main/resources/knowledge/automation_email_builder.json` — JSON-source

### Analysis & tracking
- `docs/analysis/migration_tracker.md` — V069 is current highest
- `docs/schema_version_migration.sql` — retroactive insert block
- `docs/analysis/email_assistant_handoff.md` — this file
- `docs/analysis/email_assistant_calibration_log.md` — calibration findings (next session updates)

### Production endpoints
- `https://superiorstate.biz/EmailDraftTest` — internal test page (PSP Admin only)
- `https://superiorstate.biz/EmailAssistantDraft` — POST endpoint (PSP Admin only, JSON in/out)
- `https://superiorstate.biz/KnowledgeManager` — content authoring UI (PSP Admin only)
- `https://superiorstate.biz/SkillManager` — system prompt editor (PSP Admin only)

### Production access
- SSH: `ssh kevinmurphy@ssh.superiorstate.biz` (NOT `superiorstate.biz` — Cloudflare orange cloud breaks SSH on apex)
- MySQL: `sudo LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql --socket=/var/run/mysqld/mysqld.sock -u root -p`
- Schema: `USE beta_ssa;`
- Catalina log: `/var/lib/tomcat10/logs/catalina.out`

---

## How to Resume Work in a New Session

When opening a new session:

1. Reference this doc: `docs/analysis/email_assistant_handoff.md`.
2. Reference the calibration log: `docs/analysis/email_assistant_calibration_log.md`.
3. Confirm with Kevin: has Track C federal rules been imported yet? (verify via Knowledge Manager → Federal Benefits Rules card showing 43 chunks)
4. Ask Kevin for the failed real-FSA-email test case from the prior session.
5. Apply the three queued metadata calibration fixes via Skill Manager.
6. Diagnose the inference issue from the failure case and propose substantive prompt or KB content additions.
7. Re-test on `/EmailDraftTest`, iterate.
8. When drafts feel right on canonical cases, design and ship round 5 (Outlook taskpane).

---

## Working Pattern (Established Through Rounds 1-4)

- Claude.ai (chat sessions) authors prompts and content.
- Claude Code (in IntelliJ) executes codebase work.
- Kevin handles SSH and ops verification.
- Each Claude Code prompt has Phase 1 hard-stop investigation reporting findings before proceeding.
- All schema changes use versioned migration scripts (V0XX format) with self-registration.
- Working branch: `refactor/modernize-architecture` in `Murphnd2/ams`.
- Push to GitHub triggers automatic SQL apply + WAR build/deploy + Tomcat restart.
- Content edits via Skill Manager / Knowledge Manager require no redeploy.

---

## Known Open Items (logged, not blocking)

1. UX polish on Knowledge Manager (active default, deactivate confirmation prompt, history timestamp render).
2. ServletContextListener for KnowledgeSearchService boot init (replaces lazy-init).
3. Per-employer service scope injection into drafter prompt (round 4.5 or later).
4. Annual federal rules update workflow (automation candidate).
5. Track B SSA business knowledge authoring (collaborative session needed).
6. Track C continuation: HSA, COBRA, transit, HRA/ICHRA chunks.
7. Round 6 "Request Kevin's Input" workflow for SOFT_CONF / SOFT_JUDG escalations.
