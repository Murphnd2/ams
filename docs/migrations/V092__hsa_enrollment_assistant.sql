-- V092: HSA Enrollment Assistant — chatbot skill + ssa_business knowledge chunks
--
-- Makes both AI surfaces able to answer "how does a new employee open an HSA?"
-- with SSA's actual procedure, leaving exactly one blank for the PSP user to
-- fill: that employer's myRSC employer code.
--
-- =============================================================================
-- WHY TWO ARTIFACTS, NOT ONE
-- =============================================================================
-- The two surfaces named in the request reach knowledge by DIFFERENT paths, so a
-- chatbot_skill row alone would only fix one of them:
--
--   1. Outlook add-in "Draft AI Reply" (/api/v1/outlook/draft-reply ->
--      EmailDraftService.draft) does NOT do keyword skill matching. It loads the
--      one fixed skill EMAIL_DRAFT_ASSISTANT (V065) and grounds the draft in KB
--      search over a hardcoded set: EmailDraftService.SEARCH_KBS =
--      {federal_rules, ssa_business, summit_supplemental}. To reach this surface
--      the content MUST be knowledge_chunk rows in one of those KBs. -> section 2.
--
--   2. Chatbot (/ChatAssistant) DOES do keyword skill matching
--      (ChatbotSkillDAO.findMatchingSkill). On a matched skill,
--      ChatAssistant.executeSkill sends ONLY the skill's system_prompt — it
--      injects no KB content, ever. To reach this surface the content MUST be in
--      a chatbot_skill.system_prompt. -> section 1.
--
-- SYNC-GUARD: the procedure text is therefore duplicated between the skill's
-- system_prompt and the ssa_business chunks BY DESIGN, exactly as V080 did for
-- ICHRA_DESIGN_ADVISOR. Editing one without the other makes the two surfaces
-- disagree. If the enrollment URL, the step list, or the placeholder token
-- changes, change it in BOTH places.
--
-- =============================================================================
-- SOURCE OF THE PROCEDURE
-- =============================================================================
-- Kevin Murphy -> Susie Hartshorne (Owner/Operator Services), "Re: Set up new
-- employee", 2026-08-18. Verbatim procedure as sent to the employer contact:
--   1) Go to https://secure.myrsc.com/hsaenroll
--   2) Click on the ENROLL NOW button in the upper right.
--   3) When prompted enter <employer code> (your unique employer code)
--   4) Follow the prompts to complete the online application.
--
-- =============================================================================
-- THE myRSC EMPLOYER CODE IS NOT IN AMS
-- =============================================================================
-- Verified 2026-08-18: the ONLY myRSC-code-shaped field in the codebase is
-- Setup.myRsc (setup.myRsc column, model/activity/ticket/setup/Setup.java:32).
-- It is dead — a grep across src/ finds the field declaration and its accessors
-- and nothing else: no servlet, JSP, DAO or service reads or writes it. There is
-- therefore NO live AMS source the assistant could look the code up from, and no
-- amount of prompt wiring changes that.
--
-- Consequence, and the whole point of this migration: the assistant produces the
-- complete reply with a single marked slot, [[MYRSC EMPLOYER CODE]], and the PSP
-- user pastes that employer's code in before sending. Both artifacts below
-- forbid guessing, deriving or carrying over a code — a wrong 8-digit code sends
-- an employee into the wrong employer's enrollment.
--
-- This also deliberately overrides one EMAIL_DRAFT_ASSISTANT default: its system
-- prompt (V065) says to escalate SOFT_CONF and downgrade to a
-- HOLDING_ACKNOWLEDGMENT when a needed fact is missing. A missing employer code
-- must NOT trigger that — the whole reply is known except one field the sender
-- fills in by hand. The chunks say so explicitly.
--
-- =============================================================================
-- DATA ONLY — NO WAR REQUIRED
-- =============================================================================
-- No new tables, no Java, no JSP, no UI entry point. Both surfaces already exist.
--
-- POST-APPLY STEP (REQUIRED — the chunks are invisible until this is done):
-- KnowledgeSearchService caches all chunks in memory at initialize() and only
-- re-reads on reload(). After applying this script, either restart Tomcat or
-- click "Reload cache" on the Knowledge Manager page (KnowledgeManager,
-- action=reloadCache). The chatbot skill needs neither — ChatbotSkillDAO queries
-- the DB per request.
--
-- Prerequisites: V046 (chatbot_skill), V063 (knowledge_base + knowledge_chunk +
--                the ssa_business registry row), V065 (uq_cs_psp_name).
-- =============================================================================


-- ----------------------------------------------------------------------
-- 1. The chatbot skill  (serves /ChatAssistant)
--
--    psp_id = 4 is SSA's default PSP, matching the V065/V080 precedent. An
--    installation with a different PSP id needs this row re-pointed.
--
--    Idempotent: uq_cs_psp_name (psp_id, skill_name) from V065 makes
--    INSERT IGNORE a no-op on re-run.
--
--    is_admin_only = 0 — PSP users, not just admins, answer these emails.
--
--    sort_order = 5 sits just after the DatabaseInitializer-seeded
--    "AI Setup Guide" skill (sort_order 1). findMatchingSkill picks the highest
--    keyword score and breaks ties toward the FIRST skill in sort_order, so this
--    ordering matters only on an exact tie; the trigger list below is specific
--    enough that a real HSA question outscores the setup guide's generic
--    "help"/"how to" triggers outright.
--
--    model AND max_tokens are both set, which is what makes
--    ChatAssistant.executeSkill honour them (the T52 honourConfig branch requires
--    BOTH non-blank/positive; otherwise the call silently falls back to
--    Haiku/1024). Haiku 4.5 is the right tier here — this skill recites a fixed
--    four-step procedure, it does not reason.
--
--    trigger_keywords match by SUBSTRING (lower.contains) and need >= 2 hits, so
--    multi-word phrases work here (unlike knowledge_chunk.keywords in section 2).
--    "enroll" is deliberately generic enough to catch "how does someone enroll" —
--    the false-positive case (an FSA/DCAP enrollment question also scoring 2) is
--    handled by the SCOPE rule in the prompt, which defers instead of answering.
-- ----------------------------------------------------------------------
INSERT IGNORE INTO chatbot_skill
    (psp_id, skill_name, description, system_prompt, trigger_keywords,
     accepts_file_upload, model, max_tokens, is_active, is_admin_only, sort_order)
VALUES
(
    4,
    'HSA_ENROLLMENT_ASSISTANT',
    'Answers "how does a new employee open an HSA?" with SSA''s myRSC self-enrollment procedure. Emits a [[MYRSC EMPLOYER CODE]] placeholder for the PSP user to fill from myRSC — never guesses a code.',
    'You are the HSA Enrollment Assistant for Superior State Administrators (SSA), a third-party benefits administrator.

Your audience is SSA staff — PSP users and PSP admins answering an employer contact or a participant who wants to know how an employee opens an HSA that SSA administers.

SCOPE
You answer ONE thing: how an employee enrolls in an SSA-administered HSA through myRSC, and what the asker has to supply to make that answer complete.

If the question is about something else — FSA, DCAP, HRA, ICHRA or COBRA enrollment; HSA eligibility rules; contribution limits; investment options; debit cards; claims; terminations; or anything that is not the HSA enrollment procedure — say in one sentence that you only cover HSA enrollment and point the user at the general chatbot or at SSA staff. Do not attempt the other answer. A question mentioning the word "enrollment" is not automatically an HSA enrollment question.

THE PROCEDURE
An employee enrolls themselves, online. There is no employer-side form for SSA to process and no request an employer sends SSA to add someone. The employer contact passes these four steps to the employee:

1) Go to https://secure.myrsc.com/hsaenroll
2) Click the ENROLL NOW button in the upper right.
3) When prompted, enter the employer''s unique myRSC employer code.
4) Follow the prompts to complete the online application.

THE EMPLOYER CODE — THE ONE THING YOU MUST NOT INVENT
Step 3 needs a code that is specific to ONE employer. Every employer has a different one. AMS does not store it, so you do not have it and you cannot look it up.

Whenever you write out step 3, write the code as exactly this token:

[[MYRSC EMPLOYER CODE]]

Then, on a separate line addressed to the SSA user (not to the employer), tell them to replace that token with the employer''s myRSC employer code, taken from myRSC, before sending.

NEVER:
- invent, guess, estimate or make up a code, or emit a plausible-looking number of any length;
- reuse a code from an earlier message, another employer, or an example;
- treat a code the user supplied for employer A as valid for employer B.

If the user supplies the code for the employer in question, use it verbatim in step 3 in place of the token, and drop the replace-the-token line.

STYLE
Short and plain. Give the numbered steps as steps. Do not pad with disclaimers, do not open with "Great question", do not restate the question back. If you are asked for wording to send to an employer contact, give the sendable wording and nothing else around it.

Never invent SSA policy, deadlines, eligibility rules or fees. If the user asks something adjacent that you were not told, say you do not have it rather than filling the gap.',
    'hsa,health savings account,hsa account,hsa enrollment,myrsc,my rsc,hsaenroll,enroll,enrollment,enrolling,new employee,new hire,employer code,unique employer code,open an hsa,set up an hsa,sign up for the hsa,enroll now',
    0,
    'claude-haiku-4-5-20251001',
    1024,
    1,
    0,
    5
);


-- ----------------------------------------------------------------------
-- 2. Knowledge chunks  (serve the Outlook "Draft AI Reply" surface)
--
--    Target KB is ssa_business — one of the three EmailDraftService.SEARCH_KBS,
--    and the correct domain for it ("SSA service offerings, internal procedures,
--    fees, escalation chains, and operational rules", per V063).
--
--    Idempotent: knowledge_chunk has no natural unique key, so each insert is
--    guarded with WHERE NOT EXISTS on (kb_id, title) — same pattern as V080.
--
--    !! keywords here are matched by EXACT TOKEN, not substring:
--    KnowledgeSearchService.scoreChunk tests queryWords.contains(kw) against a
--    tokenizer that splits on whitespace after stripping non-alphanumerics. A
--    multi-word keyword such as "employer code" can therefore NEVER match and is
--    dead weight. Every keyword below is a single lowercase token, on purpose.
--
--    Keywords are also kept tight rather than broad. EmailDraftService caps
--    ssa_business at MAX_SSA = 3 chunks per draft, so a chunk that scored on
--    generic tokens ("new", "employee", "account") would crowd genuinely
--    relevant ssa_business chunks out of every unrelated draft.
--
--    visibility = INTERNAL: staff-facing operational procedure. Note this column
--    is metadata only on the search path — KnowledgeSearchService.search() does
--    not filter by it — so it documents intent, it does not enforce it.
-- ----------------------------------------------------------------------

-- 2a. The procedure itself.
--     Note this chunk restates the placeholder rule that 2b covers in full. That
--     is deliberate: EmailDraftService caps ssa_business at MAX_SSA = 3, and 2b
--     scores lowest of the three on a bare inbound (8 vs. 18 for this chunk,
--     scored against the source email). On an installation whose ssa_business KB
--     already holds unrelated chunks, 2b can therefore be cut from the top 3
--     while this one survives — which would hand the drafter the procedure
--     without the "emit a placeholder, do not escalate" rule, and it would then
--     invent a code or downgrade to a HOLDING_ACKNOWLEDGMENT. The one-sentence
--     restatement here means the critical rule always travels with the steps.
INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'HSA enrollment: how an employee opens an SSA-administered HSA',
       'HSA enrollment',
       'An employee enrolls themselves in an SSA-administered HSA online, through myRSC. Give the employee these four steps: (1) Go to https://secure.myrsc.com/hsaenroll ; (2) Click the ENROLL NOW button in the upper right; (3) When prompted, enter the employer''s unique myRSC employer code; (4) Follow the prompts to complete the online application. Step 3 needs that specific employer''s code, which AMS does not store: write it into the draft as the literal placeholder token [[MYRSC EMPLOYER CODE]] and tell the SSA sender in escalationReason to replace it from myRSC before sending — never guess a code, and do NOT treat the missing code as a SOFT_CONF escalation (see the chunk "myRSC employer code: per-employer, never guessed"). This is the complete procedure: when an employer contact asks how to add or set up a new employee on the HSA, these four steps are the whole answer and the reply is a FULL_DRAFT.',
       'hsa,hsaenroll,myrsc,enroll,enrollment,enrolling',
       'HSA', 'SSA_PROCEDURE',
       'Kevin Murphy to Susie Hartshorne (Owner/Operator Services), "Re: Set up new employee", 2026-08-18 — verbatim procedure as sent',
       'INTERNAL', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ssa_business'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'HSA enrollment: how an employee opens an SSA-administered HSA');

-- 2b. The employer code rule — including the deliberate SOFT_CONF override.
INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'myRSC employer code: per-employer, never guessed',
       'HSA enrollment',
       'The myRSC employer code required at step 3 of HSA enrollment is unique to a single employer. AMS does not store it and cannot look it up, so it is never available from sender context or from knowledge. Write the code into the draft as the literal placeholder token [[MYRSC EMPLOYER CODE]], and state in escalationReason that the SSA sender must replace that token with the employer''s myRSC employer code, taken from myRSC, before sending. IMPORTANT: a missing employer code is NOT a confidence escalation. Do not set escalation to SOFT_CONF and do not downgrade to a HOLDING_ACKNOWLEDGMENT over it — every other fact in the reply is known and the sender fills this one field in by hand. Produce the FULL_DRAFT. NEVER invent, guess, estimate or pattern-match a code; never carry a code over from another employer, from an earlier email in the thread, or from an example. A wrong code routes an employee into a different employer''s enrollment.',
       'myrsc,hsaenroll,hsa,enrollment,placeholder',
       'HSA', 'SSA_PROCEDURE',
       'Kevin Murphy, 2026-08-18. AMS has no live store for this value: Setup.myRsc exists as a column but is read and written by nothing (verified by grep across src/, 2026-08-18).',
       'INTERNAL', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ssa_business'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'myRSC employer code: per-employer, never guessed');

-- 2c. The shape of the answer — who acts. Keeps the drafter from promising SSA
--     will "get them added", which is the natural but wrong reply to
--     "can you add this employee".
INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'HSA enrollment is employee-initiated; SSA does not add employees on request',
       'HSA enrollment',
       'HSA enrollment is initiated by the employee, not by SSA and not by the employer. There is no employer-side enrollment form, no roster file and no SSA-side add step. When an employer contact asks SSA to add or set up an employee on the HSA, the correct reply is not "we have added them" or "we will take care of it" — it is to pass the employee the four myRSC self-enrollment steps. Do not commit SSA to performing the enrollment, and do not state or imply a completion date for it: SSA does not control when the employee finishes the online application.',
       'hsa,enrollment,enroll,myrsc',
       'HSA', 'SSA_PROCEDURE',
       'Kevin Murphy to Susie Hartshorne (Owner/Operator Services), "Re: Set up new employee", 2026-08-18 ("they just go online and do the following")',
       'INTERNAL', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ssa_business'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'HSA enrollment is employee-initiated; SSA does not add employees on request');


-- ----------------------------------------------------------------------
-- 3. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V092' AS version, '2026-08-18' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V092',
        'HSA Enrollment Assistant: HSA_ENROLLMENT_ASSISTANT chatbot_skill row + 3 ssa_business knowledge_chunk rows (myRSC self-enrollment, [[MYRSC EMPLOYER CODE]] placeholder)',
        'V092__hsa_enrollment_assistant.sql',
        NOW());
