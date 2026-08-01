-- =============================================================================
-- V083 — Fix the retired model on ICHRA_DESIGN_ADVISOR
--
-- WHAT THIS DOES
-- Updates chatbot_skill.model (and max_tokens) on the ICHRA_DESIGN_ADVISOR row seeded
-- by V080. Nothing else changes: system_prompt, trigger_keywords, is_active,
-- is_admin_only (V082) and the knowledge base are all untouched.
--
-- WHY — PRODUCTION 404, 2026-08-01
-- catalina.out, 2026-08-01 10:06:
--   INFO  ChatAssistant - Executing skill 'ICHRA_DESIGN_ADVISOR' (text-only)
--         on model claude-sonnet-4-20250514 / 2048 max tokens
--   ERROR ClaudeApiService - Claude API returned status 404:
--         {"type":"not_found_error","message":"model: claude-sonnet-4-20250514"}
-- Confirmed via GET /v1/models against the production API key (2026-08-01):
-- claude-sonnet-4-20250514 is ABSENT from the account's model list. It is retired,
-- not merely slow or rate-limited. T52 (honour the skill's configured model/max_tokens)
-- is working exactly as designed — it surfaced a bad seed, it did not create one.
--
-- MODEL CHOICE — read ClaudeApiService.java before writing this migration
-- ChatAssistant.executeSkill's text-only branch (the one that hit the 404) calls the
-- four-arg ClaudeApiService.ask(String systemPrompt, List<Map<String,String>> messages,
-- String model, int maxTokens). That method's request body sets only model, max_tokens,
-- system, and messages — no temperature, top_p, top_k, or thinking block. Same shape on
-- every other ask/askWithContent/askWithStructuredMessages overload in that class.
-- Because none of temperature/top_p/top_k is sent, this row moves straight to
-- claude-sonnet-5 rather than an interim 4.x model — the request shape is already
-- compatible and there is no sampling-parameter conflict to work around.
--   * model: claude-sonnet-5
--   * max_tokens: 2048 -> 3072. Sonnet 5 uses a new tokenizer that consumes roughly 30%
--     more tokens for the same text, so V080's 2048 would buy materially less usable
--     output under the new model. This skill's entire value is answers that actually
--     reach their `Source:` citations (14 literal citations embedded in the system
--     prompt, per T53/T57) — a reply truncated before its citation is the specific
--     failure mode 2048 risks here. Sonnet tier (not Opus, not Fable) matches V080's
--     original cost/capability intent for a high-frequency education skill.
--
-- SCOPING (rule 4 — no hardcoded psp_id, no id)
-- Keyed on skill_name alone. V065's unique index is uq_cs_psp_name (psp_id, skill_name),
-- so skill_name identifies exactly one row PER PSP, not one row globally. Only psp_id=4
-- is seeded today (V080), so this updates exactly one row on every current
-- installation; on a multi-PSP installation it correctly updates each PSP's copy.
--
-- REVERSAL
-- Fully reversible with no data loss:
--   UPDATE chatbot_skill SET model = 'claude-sonnet-4-20250514', max_tokens = 2048
--    WHERE skill_name = 'ICHRA_DESIGN_ADVISOR';
-- (Not recommended — that value is the retired model this migration exists to fix.)
--
-- Prerequisites: V046 (chatbot_skill), V065 (uq_cs_psp_name), V080 (the row itself),
-- V082 (is_admin_only, unrelated column, same row).
-- Idempotent: re-running sets the same values again.
-- =============================================================================

-- ----------------------------------------------------------------------
-- 1. Point the skill at a live model with headroom for its citations
-- ----------------------------------------------------------------------
UPDATE chatbot_skill
   SET model = 'claude-sonnet-5',
       max_tokens = 3072
 WHERE skill_name = 'ICHRA_DESIGN_ADVISOR';

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V083' AS version, '2026-08-01' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V083',
        'Fix retired model on ICHRA_DESIGN_ADVISOR (chatbot_skill.model -> claude-sonnet-5, max_tokens -> 3072)',
        'V083__ichra_design_advisor_model.sql',
        NOW());
