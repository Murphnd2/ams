-- =============================================================================
-- V082 — Make the ICHRA/QSEHRA Design Advisor available to non-admin callers (T57)
--
-- WHAT THIS DOES
-- Flips chatbot_skill.is_admin_only from 1 to 0 on the ICHRA_DESIGN_ADVISOR row
-- seeded by V080. Nothing else changes: system_prompt, trigger_keywords, is_active
-- and the knowledge base are all untouched.
--
-- WHY
-- Build-plan item 12 specifies an AGENT-visible outcome — "Does my client's dental
-- plan kill the QSEHRA?", answered with citations, in seconds. V080 deliberately
-- seeded the row admin-only because no UI entry point existed yet and nothing was
-- meant to become visible on that migration alone. Both halves of that gate are now
-- being opened together:
--   * T58 (WAR, same release): navbar25.jsp's chatbot include is widened to admit
--     any caller for whom the ICHRA capability is already available — PSP admin, or
--     an agency with agency.ichra_enabled (V077), resolved by IchraAccessResolver.
--   * T57 (this migration): ChatAssistant.doPost line 132-133 removes admin-only
--     skills for any caller who is not isPspAdmin/isBpoAdmin, so an entitled agent
--     would otherwise reach the assistant with this skill stripped out.
-- Neither is useful without the other. Ship them together.
--
-- ⚠️ BLAST RADIUS — STATED PLAINLY, NOT ELIDED
-- is_admin_only is a property of the SKILL, not of the caller's entitlement. It has
-- no notion of ICHRA. Clearing it therefore admits EVERY non-admin caller who can
-- already reach the chat assistant — which, after T58, means:
--   * ICHRA-entitled agency agents (role 2) and agency admins (role 8) — intended;
--   * PSP users (role 1) where CHATBOT_ALL_USERS is on — NOT previously able to
--     match this skill, and gaining it here as a side effect;
--   * BPO users (role 103) where CHATBOT_ALL_BPO_USERS is on — likewise.
-- If either toggle is on for an installation, those users can now match the ICHRA
-- design advisor. That is a real widening beyond the ICHRA audience and is accepted
-- knowingly: the content is SSA's own written compliance rules, the skill refuses
-- plan selection and licensure-requiring advice by construction, and it declines
-- ICHRA notice timing outright (LA-08). Narrowing it to ICHRA-entitled callers only
-- would require a per-skill entitlement concept that chatbot_skill does not have.
--
-- KNOWLEDGE-BASE ASYMMETRY (deliberate, fail-safe, not an oversight)
-- The ichra_design knowledge_base and its chunks stay ADMIN_ONLY. KnowledgeSearchService
-- .getEligibleKBs(false) returns only summit_official/summit_supplemental, so a non-admin
-- reaches the skill but NOT the chunks. This is the safe direction and is why it is left
-- alone: a matched skill needs no chunks (ChatAssistant.executeSkill passes the
-- system_prompt as the model's entire context), while a question that scores too few
-- trigger keywords falls through to KB search and simply finds no ICHRA content —
-- rather than finding ICHRA content with none of the skill's refusal boundaries loaded,
-- which is the T53 trap. Do not "fix" this asymmetry without re-reading T53.
--
-- QUALITY CAVEAT NOW USER-VISIBLE (T52)
-- ChatAssistant.executeSkill routes text-only skills through the 3-arg
-- ClaudeApiService.ask, which hardcodes DEFAULT_MODEL and DEFAULT_MAX_TOKENS. The
-- model ('claude-sonnet-4-20250514') and max_tokens (2048) recorded on this row are
-- NOT consumed. As of this migration that defect stops being PSP-admin-only in effect:
-- agents now reach an advisor running on a model and token budget nobody chose, on a
-- citation-bearing, compliance-adjacent surface. Tracked as T52; not fixed here
-- because its fix changes the effective model for every seeded keyword-matched skill.
--
-- SCOPING (rule 4 — no hardcoded psp_id, no id)
-- Keyed on skill_name alone. Note that V065's unique index is uq_cs_psp_name
-- (psp_id, skill_name), so skill_name identifies exactly one row PER PSP, not one row
-- globally. Only psp_id=4 is seeded today (V080), so this updates exactly one row on
-- every current installation; on a multi-PSP installation it would correctly update
-- each PSP's copy, which is the intended portable behaviour.
--
-- REVERSAL
-- Fully reversible with no data loss:
--   UPDATE chatbot_skill SET is_admin_only = 1 WHERE skill_name = 'ICHRA_DESIGN_ADVISOR';
-- Reversing this alone leaves T58's navbar widening in place, which is harmless — an
-- entitled agent would reach the assistant with the ICHRA skill stripped, i.e. exactly
-- the pre-V082 behaviour.
--
-- Prerequisites: V046 (chatbot_skill), V065 (uq_cs_psp_name), V080 (the row itself).
-- Idempotent: re-running sets 0 where it is already 0.
-- =============================================================================

-- ----------------------------------------------------------------------
-- 1. Clear the admin-only flag
-- ----------------------------------------------------------------------
UPDATE chatbot_skill
   SET is_admin_only = 0
 WHERE skill_name = 'ICHRA_DESIGN_ADVISOR';

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V082' AS version, '2026-07-31' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V082',
        'Make ICHRA_DESIGN_ADVISOR available to non-admin callers (chatbot_skill.is_admin_only 1 -> 0)',
        'V082__ichra_design_advisor_non_admin.sql',
        NOW());
