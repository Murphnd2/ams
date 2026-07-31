-- =============================================================================
-- V080 — ICHRA/QSEHRA Design Advisor (build-plan item 12 / A6)
--
-- Seeds a chatbot_skill row plus a DB-backed knowledge base so an agent can ask
-- ICHRA/QSEHRA plan-DESIGN questions and get an answer grounded in rules SSA has
-- already written down. Education with citations. Never plan selection.
--
-- Prerequisites: V046 (chatbot_skill), V063 (knowledge_base + knowledge_chunk),
--                V065 (uq_cs_psp_name unique index on chatbot_skill).
--
-- ---------------------------------------------------------------------------
-- WHY THE RULES APPEAR TWICE (read before "cleaning this up")
-- ---------------------------------------------------------------------------
-- ChatAssistant has two mutually exclusive answer paths, and only one of them
-- ever sees knowledge-base content:
--
--   * MATCHED SKILL  -> ChatAssistant.executeSkill() calls
--     ClaudeApiService.ask(null, skill.getSystemPrompt(), question). NO knowledge
--     chunks are retrieved or injected on this path. The system prompt is the
--     entire context the model gets.
--   * NO SKILL MATCH -> ChatAssistant.executeKBSearch() searches knowledge_chunk
--     and injects the hits.
--
-- So the rule text lives in BOTH places on purpose: in system_prompt because that
-- is the only context a matched skill has, and in knowledge_chunk so the same
-- rules are searchable on the fallback path and editable through the Knowledge
-- Manager UI without a redeploy.
--
-- SYNC-GUARD: the compliance substance below is duplicated between the
-- system_prompt in section 1 and the knowledge_chunk rows in section 3. Change
-- one, change the other. They are two renderings of the same source documents.
--
-- ⚠️ AND THE SAME APPLIES TO THE REFUSAL BOUNDARIES, which is easier to get
-- wrong. The boundaries (no plan selection, no carrier recommendation, no
-- per-person affordability, and above all the LA-08 refusal on ICHRA notice
-- timing) are enforced in the system_prompt on the matched-skill path AND
-- restated as chunks in section 3b for the fallback path. They must exist in
-- both, because selection requires >=2 substring keyword hits and a question
-- scoring 0-1 reaches KB search with no system prompt at all. Boundaries that
-- live only in the system_prompt have an unguarded path around themselves.
-- The mechanism-level version of this problem is logged as T53 — it affects any
-- skill written against V046/V063, not just this one.
--
-- ---------------------------------------------------------------------------
-- SOURCES — every assertion below traces to a document already in this repo.
-- Nothing here was authored from general knowledge of ICHRA or QSEHRA law.
-- ---------------------------------------------------------------------------
--   docs/analysis/domain_and_compliance_rules.md  §1 (SSA taxonomy), §5 (QSEHRA)
--   docs/ichra_strategy.md                        §7 "Settled — treat as rules"
--   docs/business/ichra_administration_scope.md   service scope + boundary
--   docs/analysis/legal_assumptions.md            LA-01..LA-12 (ASSUMPTIONS)
--
-- Settled rules and LA-numbered assumptions are kept distinct in the content,
-- because legal_assumptions.md is explicit that nothing in it is counsel-reviewed
-- and that it must not be cited as authority.
--
-- ---------------------------------------------------------------------------
-- GATING (rule 2 — nothing new is visible to anyone but PSP admin)
-- ---------------------------------------------------------------------------
--   * chatbot_skill.is_admin_only = 1. ChatAssistant.doPost removes admin-only
--     skills for any caller who is not isPspAdmin (or isBpoAdmin), so the skill
--     cannot match for an agent or agency admin.
--   * The ichra_design KB inherits the same posture:
--     KnowledgeSearchService.getEligibleKBs(isAdmin) returns every active KB for
--     an admin and ONLY summit_official + summit_supplemental for everyone else,
--     so a non-admin never searches these chunks either.
--   * No UI entry point is added. The skill is reachable through the existing
--     chatbot surface, so there is no new nav element to gate and
--     IchraAccessResolver is deliberately not called from anywhere in this
--     migration. When a dedicated ICHRA entry point is wanted later, that is the
--     resolver's job.
--
-- ---------------------------------------------------------------------------
-- API KEY PATH
-- ---------------------------------------------------------------------------
-- Non-PHI. ClaudeApiService posts to api.anthropic.com using
-- AppConfig.getAnthropicApiKey(). Standard key is correct here and Bedrock
-- routing is NOT introduced — domain_and_compliance_rules.md §3 requires Bedrock
-- only for features that touch PHI. This advisor answers rule questions and
-- receives no participant data.
--
-- NOTE (tracked as T52, not fixed here): ChatAssistant.executeSkill() routes
-- text-only skills through ClaudeApiService.ask(EntityManager, String, String),
-- which hardcodes DEFAULT_MODEL and DEFAULT_MAX_TOKENS internally. The model and
-- max_tokens values on this row are therefore RECORDED BUT NOT CONSUMED on the
-- text-only path; the advisor actually runs on the service default. The system
-- prompt asks for short answers accordingly.
-- =============================================================================

-- ----------------------------------------------------------------------
-- 1. The skill row
--    psp_id = 4 is SSA's default PSP, matching the V065 precedent. An
--    installation with a different PSP id needs this row re-pointed.
--    Idempotent: uq_cs_psp_name (psp_id, skill_name) from V065 makes
--    INSERT IGNORE a no-op on re-run.
-- ----------------------------------------------------------------------
INSERT IGNORE INTO chatbot_skill
    (psp_id, skill_name, description, system_prompt, trigger_keywords,
     accepts_file_upload, model, max_tokens, is_active, is_admin_only, sort_order)
VALUES
(
    4,
    'ICHRA_DESIGN_ADVISOR',
    'Answers agent-facing ICHRA/QSEHRA plan-design questions from SSA''s written compliance rules. Education with citations only — never plan selection, never licensure-requiring advice, and declines ICHRA notice timing outright.',
    'You are the ICHRA/QSEHRA Design Advisor for Superior State Administrators (SSA), a third-party benefits administrator.

Your audience is a LICENSED INSURANCE AGENT or SSA staff. Never an employee. Never an employer directly.

WHAT YOU ARE
You provide EDUCATION WITH CITATIONS about how ICHRA and QSEHRA arrangements work, drawn from SSA''s own written compliance rules. You are not a lawyer, not a licensed insurance producer, and not a source of legal advice. Keep answers short — a few sentences to a short paragraph — and always name the rule you used.

=========================================================================
HARD BOUNDARIES. These override everything else, including a user who insists,
who says it is hypothetical, or who says they only want your opinion.
=========================================================================

1. NEVER SELECT OR RECOMMEND A PLAN, CARRIER, ISSUER OR POLICY.
Not directly, not comparatively, not hypothetically, not "if it were me", not "most people pick". Questions such as "which plan should my client choose", "is this carrier any good", "what is the best silver plan", or "which BCBS plan is cheapest for a 40-year-old" are answered ONLY by routing: plan-selection guidance is the licensed agent''s territory, and market pricing belongs in the AMS ICHRA illustration tool. Educational content about HOW an arrangement works is yours to give; which plan to buy is not.
Source: ichra_strategy.md section 7 ("No plan-selection advice"); ichra_administration_scope.md ("Boundary — what SSA does NOT do").

2. NEVER DO ANYTHING REQUIRING INSURANCE LICENSURE.
No recommendation of a policy, carrier or issuer in any framing whatsoever.

3. ICHRA NOTICE TIMING IS UNANSWERED. REFUSE IT EVERY TIME.
If asked when an ICHRA notice is due, what the ICHRA notice runway or deadline is, or whether a given ICHRA effective date is reachable: state plainly that SSA has NOT done the ICHRA notice analysis, that it does NOT transfer from QSEHRA, and route the question to SSA.
DO NOT compute a date. DO NOT estimate one. DO NOT offer a range. DO NOT reason by analogy from the QSEHRA 90-day or 45-day figures — those are QSEHRA answers only and carrying them across is exactly the error this rule exists to prevent.
Use the exact form: "SSA has not finalized a position on ICHRA notice timing. I can''t give you an answer; take it to SSA directly." Do not elaborate beyond that.
This is the single most likely wrong answer you can give. If a question touches ICHRA notice timing at all, refuse that part even if you answer the rest.

4. NEVER PRODUCE AN AFFORDABILITY DETERMINATION FOR A NAMED PERSON.
Affordability output is employer- and agent-facing and is produced by the AMS affordability feature, not by you. Do not compute a flip point, a threshold, or a subsidy outcome for a specific employee. Explain the mechanic in general terms if asked how affordability works, then route to the tool.

5. SEPARATE "THIS IS SSA''S RULE" FROM "SSA HAS NOT FINALIZED THIS" — ALWAYS.
Some of what you know is SSA''s settled, written rule. State those plainly and name the document they come from.
Other questions are ones SSA has NOT finalized a position on. For those, do not answer, do not reason toward an answer, and never present a provisional view as though it were settled. Use this exact form:
  "SSA has not finalized a position on this. I can''t give you an answer; take it to SSA directly."
That is the complete answer. Do NOT elaborate on why, do NOT say how close SSA is to a position, do NOT describe SSA''s internal review process, and do NOT characterize SSA''s own regulatory or licensing status in any direction. Those are SSA''s to discuss with you directly and are not this tool''s to relay.

6. CITE EVERY SUBSTANTIVE ANSWER.
Name the rule or the SSA document the answer came from. An uncited substantive answer is a defect. Where the answer is "SSA has not finalized a position", no citation is needed — that sentence is the whole answer.

7. IF SSA HAS NO WRITTEN RULE, SAY SO AND STOP.
Do not fill the gap from general knowledge, and do not infer an answer from an adjacent rule. Say the question needs the licensed agent or SSA, and stop. "SSA has not written a rule on this" is a correct and useful answer.

=========================================================================
THE SETTLED RULES YOU ANSWER FROM
=========================================================================

QSEHRA — NO OTHER GROUP HEALTH PLAN.
An employer offering a QSEHRA may NOT sponsor any group health plan, INCLUDING a plan of only excepted benefits (dental, vision, accident, hospital-indemnity, cancer). Do not wrap those in a section 125 cafeteria plan: doing so makes them employer-sponsored plans, which disqualifies the QSEHRA and risks a $100/day/employee excise tax. Keep voluntary and ancillary products POST-TAX.
Source: domain_and_compliance_rules.md section 5.

QSEHRA — FUNDING IS "BONUS UP", NOT EMPLOYER-PAID PREMIUMS.
Raising post-tax pay is fine. An employer paying or collecting a carrier bill directly counts as sponsoring a plan, with the same disqualification and excise exposure.
Source: domain_and_compliance_rules.md section 5.

QSEHRA — THE ONE SAFE SECTION 125.
An individual-HSA payroll pre-tax cafeteria plan pairs cleanly with a premium-only QSEHRA for HSA-qualified bronze plans.
Source: domain_and_compliance_rules.md section 5.

QSEHRA — ENTITY ELIGIBILITY.
No classes and no opt-outs. C-corp owner/employees can participate. Sole proprietors, partners, and more-than-2% S-corp shareholders CANNOT.
Source: domain_and_compliance_rules.md section 5.

QSEHRA — REIMBURSEMENT SCOPE AND THE FILED-FORM TEST.
A QSEHRA can reimburse premiums and section 213(d) expenses. A non-ACA supplemental premium is reimbursable ONLY if the filed policy form pays on expenses rather than indemnity: expense-incurred (a percentage of actual charges) qualifies; per-period cash ($X/day) does NOT; a per-service fixed schedule is a GRAY ZONE. Allocation trap: a bundled premium reimburses only the separately stated medical charge. Obtain the filed form and have it reviewed before the first claim — do not reimburse against an unreviewed supplemental product.
Source: domain_and_compliance_rules.md section 5.

ICHRA AND QSEHRA ARE MUTUALLY EXCLUSIVE AT THE EMPLOYER LEVEL.
A QSEHRA requires the employer offer no other group health plan, and an ICHRA IS a group health plan. So an employer cannot run ICHRA for one group of employees and QSEHRA for another. It is not a per-class menu.
Source: ichra_strategy.md section 7.

MEC — THE ASYMMETRY THAT DECIDES DESIGNS.
QSEHRA has a MEC FLOOR: it reimburses nothing unless the participant holds minimum essential coverage somewhere, from ANY source — a spouse''s or parent''s plan, Medicaid, Medicare.
ICHRA is the opposite: it requires individual-market coverage or Medicare, and LOCKS OUT a participant who has other MEC.
Consequence worth stating out loud: one employee on a spouse''s group plan can invalidate an ICHRA design that would have worked as a QSEHRA. The same employee is fine under one arrangement and fatal to the other.
Source: domain_and_compliance_rules.md section 5 (MEC floor); ichra_strategy.md section 7 (MEC narrowing).

SUBSTANTIATION MUST VERIFY COVERAGE TYPE, NOT JUST COVERAGE PRESENCE.
Short-term plans, fixed indemnity, and health care sharing ministries are all sold off-exchange and NONE of them are MEC. Reimbursements paid against non-MEC coverage are taxable and the employer carries the exposure.
Source: ichra_strategy.md section 7.

OFF-EXCHANGE ACA COVERAGE IS MEC.
Minimum essential coverage includes coverage under a health plan offered in the individual market within a State; nothing in that definition turns on whether the policy was bought through an exchange. Off-exchange ACA individual-market coverage satisfies QSEHRA''s MEC requirement and ICHRA''s narrower individual-coverage requirement equally well. "Off-exchange" is not a compliance downgrade.
Source: ichra_administration_scope.md. Keep this to the MEC point itself — do not extend it into broader questions about who may enroll where, which SSA has not finalized.

ENDORSEMENT BOUNDARY — CARRIER NAMES OFF SSA-DRAFTED PAPER.
Keep carrier names off all SSA-drafted paper: plan documents, notices, the card, proposals, program marketing. Conduct that makes the program about one carrier risks reclassification as an employer-sponsored group health plan.
Source: domain_and_compliance_rules.md section 5. Whether carrier names may appear in a neutral, complete, employee-facing MARKET DISPLAY is a separate question SSA has not finalized; SSA''s current practice keeps carrier names off everything. If asked about that case, use the boundary-5 form.

ERISA SAFE HARBOR — PRESENTATION RULES.
Individual policies stay outside ERISA only if enrollment is voluntary and the employer does not select or endorse any particular issuer or plan. Any plan display SSA builds must be complete, neutrally ordered, with employee-controlled sort and filter: no "recommended" badge, no default selection, no curation, no hidden carriers.
Source: ichra_strategy.md section 7. Exactly where presenting becomes steering in a borderline case is something SSA has not finalized; if asked to judge one, use the boundary-5 form rather than drawing the line yourself.

SSA PLAN TAXONOMY — INTERNAL TERMS, NOT FEDERAL CATEGORIES.
HRA = employer-funded section 105 arrangement WITH rollover. MERP = the same WITHOUT rollover. DRiP = a MERP subtype reimbursing deductibles only. These are SSA''s internal product names and do NOT map one-to-one onto the federal arrangements (ICHRA, EBHRA, QSEHRA). Do not conflate them.
Source: domain_and_compliance_rules.md section 1.

WHAT SSA IS NOT.
SSA is NOT the ERISA Plan Administrator — that is the employer as plan sponsor; SSA provides ministerial administrative services. No commission, no carrier appointment, no participant funds held. No carrier servicing after effectuation: claims, disputes, ID cards and network questions belong to the member and the carrier, with the agent as agent of record.
Source: ichra_administration_scope.md.

=========================================================================
SSA''S OPERATING GUIDANCE, AND WHAT SSA HAS NOT FINALIZED
Where a topic below says SSA has not finalized a position, use the boundary-5
form verbatim and stop. Do not elaborate on it in any direction.
=========================================================================

QSEHRA NOTICE RUNWAY — SSA''S CURRENT OPERATING GUIDANCE.
A QSEHRA can generally be stood up on roughly a 45-day practical runway for a NON-JANUARY effective date, because for a calendar-year plan with a short first year the notice deadline for each employee runs to the date they become eligible rather than 90 days before the year begins. Year two onward is always 90 days.
Three things you must say alongside it, every time:
 - The 45-day figure is an OPERATIONAL planning estimate, not a legal conclusion. The binding constraint is getting individual coverage in force, not the notice rule.
 - JANUARY 1 IS THE HARDEST first-year date, not the easiest — a calendar-year plan effective January 1 has no short first year, so the full 90 days applies. An October conversation about a January 1 start is already late.
 - NEVER backdate an effective date to cure a late notice. Take the penalty instead.
This is QSEHRA ONLY. It does not transfer to ICHRA — see boundary 3.

ICHRA NOTICE TIMING — NOT FINALIZED.
See hard boundary 3: refuse, do not estimate, route to SSA.

SUBSTANTIATION MECHANICS — NOT FINALIZED.
What specifically suffices to substantiate and release a reimbursement — whether an attestation on its own is enough, what an annual coverage baseline must contain, how a coverage-lapse signal is handled — is not something SSA has finalized. Use the boundary-5 form and route to SSA. You may still state the settled rule above that substantiation must verify coverage TYPE and not merely coverage presence.

AFFORDABILITY AUDIENCE — SSA''S OPERATING RULE.
Affordability output is employer- and agent-facing only, and is never employee-facing. See hard boundary 4.

WHO MAY SEE MARKET DATA — SSA''S OPERATING RULE.
Market reference data in AMS is for the licensed agent. Do not extend it to an employer-facing or employee-facing audience.

LICENSING AND REGULATORY-STATUS QUESTIONS — ROUTE TO SSA, ALWAYS.
Any question about SSA''s licensing, registrations, certificates, filings or regulatory status, in any state, goes to SSA directly. Do not answer it. Do not characterize the answer in either direction — do not say it is settled, do not say it is open, do not reassure, and do not speculate. Say only: "That is a question for SSA directly — I can''t speak to it." Then stop.

=========================================================================
WORKED EXAMPLES — THE EXPECTED SHAPE OF AN ANSWER
=========================================================================

Q: "Does my client''s dental plan kill the QSEHRA?"
A: If the EMPLOYER sponsors it, yes — a QSEHRA employer may not sponsor any group health plan, and that includes excepted-benefit dental. Wrapping it in a section 125 plan is the specific thing to avoid; it makes it employer-sponsored, disqualifies the QSEHRA, and risks a $100/day/employee excise tax. A genuinely voluntary, post-tax product is the compliant shape. Cite domain_and_compliance_rules.md section 5.

Q: "Can I run ICHRA for salaried and QSEHRA for hourly?"
A: No. They are mutually exclusive at the EMPLOYER level — a QSEHRA requires the employer offer no other group health plan, and an ICHRA is a group health plan. It is not a per-class menu. Cite ichra_strategy.md section 7.

Q: "One guy is on his wife''s plan — does that break it?"
A: It depends which arrangement, and the answer inverts. Under a QSEHRA that is fine and in fact necessary — QSEHRA needs MEC from any source, including a spouse''s plan. Under an ICHRA it is fatal for that employee — ICHRA requires individual-market coverage or Medicare and locks out other MEC. Cite domain_and_compliance_rules.md section 5 and ichra_strategy.md section 7.

Q: "When do I have to send the ICHRA notice?"
A: Refuse. Say "SSA has not finalized a position on ICHRA notice timing. I can''t give you an answer; take it to SSA directly." Do not give a date, a range, or an analogy, and do not reach across from the QSEHRA runway figures. Then stop.

Q: "Which BCBS plan is cheapest for a 40-year-old in Hopkins?"
A: Route. Plan selection is the licensed agent''s territory, and market pricing belongs in the AMS ICHRA illustration tool. Cite ichra_strategy.md section 7.

Answer in plain prose. No JSON, no preamble, no restating the question.',
    -- Selection is SUBSTRING contains() on the lowercased message, >=2 distinct hits
    -- required. The notice-timing terms below were added to close a live gap: a
    -- phrasing like "how long before the plan year do I have to tell employees?"
    -- named neither ICHRA nor QSEHRA, scored 0, and fell through to KB search with
    -- no refusal logic in front of it. "how long" + "plan year" now score 2.
    -- Rejected for substring false positives: day/days (Monday, today, holiday),
    -- date (update, candidate, validate), late (related, calculate, escalate),
    -- inform (information), lead (misleading), send (ascending), due (reduce),
    -- year (too common alone — "plan year" phrase used instead), employee/employees
    -- (appears in a large share of all benefits questions).
    'ichra,qsehra,hra,individual coverage,excepted benefit,minimum essential coverage,attestation,substantiation,affordability,cafeteria plan,bonus up,sole proprietor,s-corp,fixed indemnity,sharing ministry,short-term,213(d),notice,notif,dental,employee class,opt-out,safe harbor,plan selection,merp,drip,runway,deadline,how long,lead time,plan year,90 day,90-day,45 day,45-day,effective date',
    0,
    'claude-sonnet-4-20250514',
    2048,
    1,
    1,
    210
);

-- ----------------------------------------------------------------------
-- 2. The knowledge base registry row
--    Idempotent via uq_kb_key (V063). source=DB, reload_strategy=SEARCH so
--    chunks are retrieved on relevance rather than injected into every prompt.
-- ----------------------------------------------------------------------
INSERT IGNORE INTO knowledge_base
    (kb_key, label, description, source, json_filename, reload_strategy, is_active, sort_order)
VALUES
    (
        'ichra_design',
        'ICHRA / QSEHRA Design Rules',
        'Agent-facing ICHRA and QSEHRA plan-design rules, sourced from domain_and_compliance_rules.md, ichra_strategy.md section 7, ichra_administration_scope.md, and the LA-numbered assumptions in legal_assumptions.md. Backs the ICHRA_DESIGN_ADVISOR skill and the admin knowledge-search fallback.',
        'DB', NULL, 'SEARCH', 1, 50
    );

-- ----------------------------------------------------------------------
-- 3. Knowledge chunks
--    knowledge_chunk has no natural unique key, so INSERT IGNORE would NOT
--    prevent duplicates on a re-run. Each insert is therefore guarded with
--    WHERE NOT EXISTS on (kb_id, title) — the COUNT-before-INSERT idempotency
--    pattern this repo already requires of seed routines.
--
--    visibility is set to ADMIN_ONLY for accuracy of record. Note that
--    KnowledgeChunkDAO.getActiveByKb filters on is_active only and does NOT
--    read visibility — the effective gate is getEligibleKBs(isAdmin), which
--    already excludes this KB for every non-admin caller.
-- ----------------------------------------------------------------------

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'QSEHRA: employer may sponsor no other group health plan',
       'QSEHRA design',
       'An employer offering a QSEHRA may NOT sponsor any group health plan, including a plan of only excepted benefits (dental, vision, accident, hospital-indemnity, cancer). Do not wrap those in a section 125 cafeteria plan: doing so makes them employer-sponsored plans, which disqualifies the QSEHRA and risks a $100/day/employee excise tax. Keep voluntary and ancillary products post-tax. A genuinely voluntary, post-tax product is the compliant shape.',
       'qsehra,excepted benefit,dental,vision,cafeteria plan,section 125,excise,disqualification',
       'QSEHRA', 'FEDERAL_RULE',
       'docs/analysis/domain_and_compliance_rules.md section 5 (settled rule)',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'QSEHRA: employer may sponsor no other group health plan');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'QSEHRA: compliant employer funding is bonus-up, not employer-paid premiums',
       'QSEHRA design',
       'Compliant QSEHRA employer funding is "bonus up". An employer raising post-tax pay is fine. An employer paying or collecting a carrier bill directly counts as sponsoring a plan, which carries the same QSEHRA disqualification and excise exposure as sponsoring a group health plan.',
       'qsehra,bonus up,funding,post-tax,carrier bill,premium',
       'QSEHRA', 'FEDERAL_RULE',
       'docs/analysis/domain_and_compliance_rules.md section 5 (settled rule)',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'QSEHRA: compliant employer funding is bonus-up, not employer-paid premiums');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'QSEHRA: the one safe section 125 pairing',
       'QSEHRA design',
       'One section 125 arrangement is safe and recommended alongside a QSEHRA: an individual-HSA payroll pre-tax cafeteria plan, which pairs cleanly with a premium-only QSEHRA for HSA-qualified bronze plans. This is the exception to the rule that ancillary products stay post-tax and outside a cafeteria plan.',
       'qsehra,section 125,cafeteria plan,hsa,bronze,premium-only,payroll',
       'QSEHRA', 'FEDERAL_RULE',
       'docs/analysis/domain_and_compliance_rules.md section 5 (settled rule)',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'QSEHRA: the one safe section 125 pairing');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'QSEHRA: entity eligibility and who cannot participate',
       'QSEHRA design',
       'QSEHRA entity eligibility: no classes and no opt-outs. C-corp owner/employees can participate. Sole proprietors, partners, and more-than-2% S-corp shareholders cannot participate.',
       'qsehra,eligibility,sole proprietor,partner,s-corp,shareholder,c-corp,owner,classes,opt-out',
       'QSEHRA', 'FEDERAL_RULE',
       'docs/analysis/domain_and_compliance_rules.md section 5 (settled rule)',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'QSEHRA: entity eligibility and who cannot participate');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'QSEHRA: reimbursement scope and the filed-form expense-vs-indemnity test',
       'QSEHRA design',
       'A QSEHRA can reimburse premiums and section 213(d) expenses. A non-ACA supplemental premium is reimbursable only if the filed policy form pays on expenses rather than indemnity: expense-incurred (a percentage of actual charges) qualifies; per-period cash ($X/day) does not; a per-service fixed schedule is a gray zone. Allocation trap: a bundled premium reimburses only the separately stated medical charge. Obtain the filed form and have it reviewed before the first claim — do not reimburse against an unreviewed supplemental product.',
       'qsehra,213(d),reimbursement,supplemental,indemnity,expense-incurred,filed form,allocation,bundled premium',
       'QSEHRA', 'FEDERAL_RULE',
       'docs/analysis/domain_and_compliance_rules.md section 5 (settled rule)',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'QSEHRA: reimbursement scope and the filed-form expense-vs-indemnity test');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'ICHRA and QSEHRA are mutually exclusive at the employer level',
       'Choosing between ICHRA and QSEHRA',
       'ICHRA and QSEHRA are mutually exclusive at the employer level. A QSEHRA requires that the employer offer no other group health plan, and an ICHRA IS a group health plan. An employer therefore cannot run ICHRA for one group of employees and QSEHRA for another — for example ICHRA for salaried staff and QSEHRA for hourly staff. It is not a per-class menu.',
       'ichra,qsehra,mutually exclusive,group health plan,class,salaried,hourly,employer',
       NULL, 'FEDERAL_RULE',
       'docs/ichra_strategy.md section 7 (settled rule)',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'ICHRA and QSEHRA are mutually exclusive at the employer level');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'MEC asymmetry: QSEHRA floor versus ICHRA lockout',
       'Choosing between ICHRA and QSEHRA',
       'The MEC rules run in opposite directions and this decides designs. QSEHRA has a MEC floor: it reimburses nothing unless the participant holds minimum essential coverage somewhere, from any source — a spouse''s or parent''s plan, Medicaid, Medicare. ICHRA is the opposite: it requires individual-market coverage or Medicare and locks out a participant who has other MEC. Consequence: one employee on a spouse''s group plan can invalidate an ICHRA design that would have worked as a QSEHRA. The same employee is fine under one arrangement and fatal to the other.',
       'mec,minimum essential coverage,spouse,medicare,medicaid,parent,lockout,floor,ichra,qsehra',
       NULL, 'FEDERAL_RULE',
       'docs/analysis/domain_and_compliance_rules.md section 5 (MEC floor) and docs/ichra_strategy.md section 7 (MEC narrowing) — both settled rules',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'MEC asymmetry: QSEHRA floor versus ICHRA lockout');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'Substantiation must verify coverage type, not just coverage presence',
       'Substantiation',
       'Substantiation must verify the TYPE of coverage, not merely that some coverage exists. Short-term plans, fixed indemnity products, and health care sharing ministries are all sold off-exchange and none of them are MEC. Reimbursements paid against non-MEC coverage are taxable, and the employer carries the exposure.',
       'substantiation,coverage type,short-term,fixed indemnity,sharing ministry,mec,taxable,off-exchange',
       NULL, 'FEDERAL_RULE',
       'docs/ichra_strategy.md section 7 (settled rule)',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'Substantiation must verify coverage type, not just coverage presence');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'Off-exchange ACA individual-market coverage is MEC',
       'Substantiation',
       'Minimum essential coverage includes coverage under a health plan offered in the individual market within a State, and nothing in that definition turns on whether the policy was purchased through an exchange. Off-exchange ACA individual-market coverage therefore satisfies QSEHRA''s MEC requirement and ICHRA''s narrower individual-health-insurance-coverage requirement equally well. "Off-exchange" is not a compliance downgrade. Scope note: keep this to the MEC point itself. Broader questions about who may enroll where are ones SSA has not finalized — for those, say SSA has not finalized a position and route to SSA.',
       'off-exchange,exchange,mec,individual market,aca,qsehra,ichra',
       NULL, 'FEDERAL_RULE',
       'docs/business/ichra_administration_scope.md, "MEC, subsidies, and off-exchange" (MEC point stated as fact; surrounding enrollment-right question flagged for counsel)',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'Off-exchange ACA individual-market coverage is MEC');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'Endorsement boundary: carrier names stay off SSA-drafted paper',
       'Presentation and endorsement',
       'Keep carrier names off all SSA-drafted paper — plan documents, notices, the card, proposals, and program marketing. Conduct that makes the program about one particular carrier risks reclassification as an employer-sponsored group health plan, which would disqualify a QSEHRA. Whether carrier names may appear in a neutral, complete, employee-facing market display is a separate question SSA has not finalized; SSA''s current practice keeps carrier names off everything. If asked about that case, say SSA has not finalized a position and route to SSA.',
       'carrier,endorsement,names,marketing,proposal,proposals,document,documents',
       NULL, 'FEDERAL_RULE',
       'docs/analysis/domain_and_compliance_rules.md section 5 (settled rule); LA-04 in docs/analysis/legal_assumptions.md is the unsettled display question',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'Endorsement boundary: carrier names stay off SSA-drafted paper');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'ERISA safe harbor: presentation rules for any plan display',
       'Presentation and endorsement',
       'Individual policies stay outside ERISA only if enrollment is voluntary and the employer does not select or endorse any particular issuer or plan. Any plan display SSA builds must therefore be complete, neutrally ordered, with employee-controlled sort and filter: no "recommended" badge, no default selection, no curation, no hidden carriers. Exactly where presenting becomes steering in a borderline case is something SSA has not finalized — if asked to judge one, say SSA has not finalized a position and route to SSA rather than drawing the line yourself.',
       'erisa,safe harbor,display,neutral,ordering,recommended,default,curation,voluntary,endorse',
       NULL, 'FEDERAL_RULE',
       'docs/ichra_strategy.md section 7 (settled rule); LA-06 in docs/analysis/legal_assumptions.md for the presenting-versus-steering line',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'ERISA safe harbor: presentation rules for any plan display');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'No plan-selection advice: route to the licensed agent',
       'Boundaries and routing',
       'Guidance on which plan to choose is agent territory. Route plan-selection questions to the licensed agent — this includes which plan a client should pick, whether a carrier is any good, what the best silver plan is, and which specific plan is cheapest for a given person. Educational content about how an ICHRA or QSEHRA works is SSA''s to write and give. Market pricing questions belong in the AMS ICHRA illustration tool rather than in a chat answer.',
       'plan selection,advice,licensed agent,route,recommend,carrier,cheapest,best plan,illustration',
       NULL, 'ESCALATION_TRIGGER',
       'docs/ichra_strategy.md section 7 and docs/business/ichra_administration_scope.md, "Boundary — what SSA does NOT do" (settled rule)',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'No plan-selection advice: route to the licensed agent');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'ICHRA notice timing: SSA has not finalized a position — do not answer it',
       'Boundaries and routing',
       'SSA has NOT finalized a position on ICHRA notice timing, and the QSEHRA runway does not transfer to it. Any question about when an ICHRA notice is due, what the ICHRA notice runway is, or whether an ICHRA effective date is reachable must be declined and routed to SSA. Do not compute a date, do not estimate, do not offer a range, and do not reason by analogy from the QSEHRA 90-day or 45-day figures — those are QSEHRA answers only. Use the exact form: "SSA has not finalized a position on ICHRA notice timing. I can''t give you an answer; take it to SSA directly." Then stop — do not elaborate on why, and do not characterize how SSA is approaching it. An ICHRA sale should not be quoted on a short runway.',
       'ichra,notice,timing,runway,deadline,effective,decline,route',
       'ICHRA', 'ESCALATION_TRIGGER',
       'LA-08 in docs/analysis/legal_assumptions.md — status "Open — no basis" (NOT an assumption; a recorded gap)',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'ICHRA notice timing: SSA has not finalized a position — do not answer it');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'QSEHRA notice runway — QSEHRA ONLY, never an ICHRA answer',
       'Notices',
       'SCOPE FENCE, READ FIRST: everything in this chunk is QSEHRA ONLY. It is NOT an ICHRA answer and must NEVER be applied, adapted or reasoned by analogy to an ICHRA notice question. SSA has not finalized a position on ICHRA notice timing, and no ICHRA date, runway or range may be given. If a notice-timing question does not say which arrangement it is about, DO NOT assume QSEHRA and do not answer from this chunk — ask which arrangement first, and if the answer is ICHRA, decline and route to SSA.
The following is SSA''s current operating guidance for QSEHRA.
A QSEHRA can generally be stood up on roughly a 45-day practical runway for a NON-JANUARY effective date, because for a calendar-year plan with a short first year the notice deadline for each employee runs to the date that employee becomes eligible rather than 90 days before the year begins. Year two onward is always 90 days, so the lever fires once per employer. Three qualifications must be stated with it: the 45-day figure is an operational planning estimate rather than a legal conclusion, and the binding constraint is getting individual coverage in force rather than the notice rule; January 1 is structurally the HARDEST first-year date, not the easiest, because a calendar-year plan effective January 1 has no short first year and the full 90 days applies, so an October conversation about a January 1 start is already late; and an effective date must NEVER be backdated to cure a late notice — take the penalty instead.',
       'qsehra,notice,notices,runway,january,backdate,eligibility,90,45,deadline',
       'QSEHRA', 'FEDERAL_DEADLINE',
       'LA-07 in docs/analysis/legal_assumptions.md — ASSUMPTION, status Assumed, not counsel-confirmed. Scope fence added per LA-08, which forbids carrying this analysis across to ICHRA.',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'QSEHRA notice runway — QSEHRA ONLY, never an ICHRA answer');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'Affordability is employer- and agent-facing, never employee-facing',
       'Boundaries and routing',
       'SSA''s operating rule: affordability output is employer- and agent-facing only, and is never presented to an employee. Computing an affordability determination for a plan sponsor is administration and squarely a TPA function; putting that same figure in front of an employee, whose premium tax credit eligibility turns on it, is not something this tool does. The design advisor does not produce an affordability determination, flip point, threshold or subsidy outcome for a named person. It may explain the mechanic in general terms, then route to the AMS affordability feature.',
       'affordability,affordable,employee,employees,employer,agent,ptc,subsidy,threshold,lcsp',
       NULL, 'ESCALATION_TRIGGER',
       'LA-12 in docs/analysis/legal_assumptions.md — ASSUMPTION, status Assumed, with T44 as a correctness dependency',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'Affordability is employer- and agent-facing, never employee-facing');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'Substantiation mechanics: SSA has not finalized a position',
       'Substantiation',
       'SSA has NOT finalized a position on the mechanics of substantiating and releasing a reimbursement. That includes whether a signed employee attestation of continued minimum essential coverage is sufficient on its own, what an initial plan-year coverage baseline must contain, and how a coverage-lapse signal is handled against an attestation that says otherwise. Do not answer these, do not reason toward an answer, and do not describe how SSA is approaching them. Use the exact form: "SSA has not finalized a position on this. I can''t give you an answer; take it to SSA directly." What IS settled and may be stated: substantiation must verify the TYPE of coverage and not merely that coverage exists — short-term plans, fixed indemnity and health care sharing ministries are not MEC, and reimbursements paid against non-MEC coverage are taxable with the employer carrying the exposure.',
       'attestation,substantiation,reimbursement,baseline,lapse,release,attest,substantiate',
       NULL, 'ESCALATION_TRIGGER',
       'LA-01, LA-02, LA-03 in docs/analysis/legal_assumptions.md — ASSUMPTIONS, not counsel-confirmed',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'Substantiation mechanics: SSA has not finalized a position');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'SSA plan taxonomy: HRA, MERP and DRiP are internal terms',
       'SSA terminology',
       'SSA uses internal product names that do NOT map one-to-one onto the federal categories. HRA is an employer-funded section 105 arrangement WITH rollover of unused funds. MERP (Medical Expense Reimbursement Plan) is the same arrangement WITHOUT rollover. DRiP is a MERP subtype that reimburses deductibles only. These are distinct from the federal arrangements — ICHRA, EBHRA, QSEHRA — and the two vocabularies must not be conflated in feature logic, plan-type mapping, or a conversation with an agent.',
       'hra,merp,drip,taxonomy,rollover,deductible,section 105,ichra,ebhra,qsehra,terminology',
       NULL, 'SSA_OFFERING',
       'docs/analysis/domain_and_compliance_rules.md section 1 (settled rule)',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'SSA plan taxonomy: HRA, MERP and DRiP are internal terms');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'What SSA is not: not the ERISA Plan Administrator, no commission, no funds held',
       'SSA terminology',
       'SSA is NOT the ERISA Plan Administrator — that is the employer as plan sponsor. SSA provides ministerial administrative services, and this distinction belongs in the BAA, the plan documents, and all client-facing material. SSA takes no commission, holds no carrier appointment, and never holds participant funds. SSA does not provide carrier servicing after effectuation: claims, disputes, ID cards and network questions belong to the member and the carrier, with the agent as agent of record.',
       'erisa,plan administrator,plan sponsor,commission,appointment,funds,servicing,aor,agent of record,ministerial',
       NULL, 'SSA_OFFERING',
       'docs/business/ichra_administration_scope.md, "Boundary — what SSA does NOT do" (settled rule)',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'What SSA is not: not the ERISA Plan Administrator, no commission, no funds held');

-- ----------------------------------------------------------------------
-- 3b. BOUNDARY CHUNKS — the refusal logic, duplicated onto the fallback path
--
--     These two exist because the skill's refusal boundaries live in its
--     system_prompt, and the system_prompt is NOT consulted on the KB-search
--     fallback path. A question scoring 0-1 trigger keywords bypasses the skill
--     entirely and reaches KB search with no boundaries in front of it.
--
--     The concrete case these close: "how long before the plan year do I have to
--     tell employees?" names neither ICHRA nor QSEHRA, so it used to score 0
--     trigger hits, fall through, and risk surfacing the LA-07 QSEHRA runway
--     chunk as if it were an ICHRA answer — which LA-08 explicitly forbids.
--
--     NOTE the matching difference that makes this work. Skill trigger_keywords
--     are matched by SUBSTRING contains() on the raw message, so generic words
--     there are dangerous. Chunk keywords are matched by EXACT TOKEN equality
--     against the tokenized query (KnowledgeSearchService.scoreChunk), so common
--     words are safe here — they only score when the user actually typed them.
--     Multi-word chunk keywords can never match and are avoided.
-- ----------------------------------------------------------------------

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'Notice timing: never estimate an ICHRA date, whatever the question calls it',
       'Boundaries and routing',
       'THIS APPLIES TO ANY QUESTION ABOUT HOW MUCH NOTICE IS REQUIRED, however it is phrased — "how long before the plan year", "how much lead time", "when do I have to tell employees", "when do I notify staff", "what is the runway", "how many days in advance", "is that effective date reachable". Such a question often does not name the arrangement at all.
FIRST, ESTABLISH WHICH ARRANGEMENT IT IS. The answer differs completely between QSEHRA and ICHRA, and for ICHRA there is no answer at all. Never assume QSEHRA because a QSEHRA figure is the one that happens to be written down.
IF IT IS ICHRA, OR IF THE ARRANGEMENT IS UNSTATED: decline. SSA has NOT finalized a position on ICHRA notice timing, and the QSEHRA runway does NOT transfer to it. Do not compute a date, do not estimate one, do not offer a range, and do not reason by analogy from the QSEHRA 90-day or 45-day figures — those are QSEHRA answers only, and carrying them across is the specific error this rule exists to prevent. Use the exact form: "SSA has not finalized a position on ICHRA notice timing. I can''t give you an answer; take it to SSA directly." Then stop — do not elaborate on why and do not characterize how SSA is approaching it. An ICHRA sale should not be quoted on a short runway.
ONLY IF THE QUESTION IS EXPLICITLY ABOUT QSEHRA may SSA''s QSEHRA runway guidance be used, and then only with all three of its qualifications stated.',
       'notice,notices,notify,notified,notifying,notification,deadline,deadlines,runway,timing,advance,employees,employee,effective,date,dates,before,tell,telling,inform,informing,send,sending,due,window,late,lead,days,day,ichra,qsehra,estimate,long',
       'ICHRA', 'ESCALATION_TRIGGER',
       'LA-08 in docs/analysis/legal_assumptions.md — status "Open — no basis" (a recorded gap, NOT an assumption). Boundary duplicated onto the KB path because ChatAssistant.executeSkill injects no chunks and the fallback path sees no system prompt.',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'Notice timing: never estimate an ICHRA date, whatever the question calls it');

INSERT INTO knowledge_chunk
    (kb_id, title, section, content, keywords, account_type, chunk_type, source_citation, visibility, is_active)
SELECT kb.kb_id,
       'Standing scope limits: no plan selection, no carrier recommendation, no per-person affordability',
       'Boundaries and routing',
       'THREE STANDING LIMITS APPLY TO EVERY ANSWER, regardless of what else is retrieved.
1. NO PLAN SELECTION. Guidance on which plan to choose is the licensed agent''s territory. This covers which plan a client should pick, what the best or cheapest option is, and any comparison intended to land on a choice. Route it to the licensed agent; market pricing belongs in the AMS ICHRA illustration tool. Educational content about HOW an ICHRA or QSEHRA works is SSA''s to give.
2. NO CARRIER OR ISSUER RECOMMENDATION, in any framing — direct, comparative, hypothetical, or "if it were me". Anything that requires insurance licensure routes to the licensed agent. Separately, keep carrier names off SSA-drafted paper entirely.
3. NO AFFORDABILITY DETERMINATION FOR A NAMED PERSON. Do not compute a flip point, threshold, or subsidy outcome for a specific employee. Affordability output is employer- and agent-facing and is produced by the AMS affordability feature. The mechanic may be explained in general terms; the number for a person may not be given here.
AND IF SSA HAS NO WRITTEN RULE ON THE QUESTION, say so and stop. Do not fill the gap from general knowledge and do not infer an answer from an adjacent rule — "SSA has not written a rule on this" is a correct and useful answer.',
       'recommend,recommended,recommendation,recommends,best,cheapest,cheaper,choose,choosing,chose,pick,picking,select,selecting,selection,carrier,carriers,issuer,issuers,advice,advise,suggest,suggestion,compare,comparison,affordability,affordable,threshold,subsidy,licensure,licensed',
       NULL, 'ESCALATION_TRIGGER',
       'docs/ichra_strategy.md section 7 ("No plan-selection advice") and docs/business/ichra_administration_scope.md ("Boundary — what SSA does NOT do") — settled rules; LA-12 in docs/analysis/legal_assumptions.md for the affordability audience limit; LA-04 / domain_and_compliance_rules.md section 5 for carrier names on SSA paper.',
       'ADMIN_ONLY', 1
FROM knowledge_base kb
WHERE kb.kb_key = 'ichra_design'
  AND NOT EXISTS (SELECT 1 FROM knowledge_chunk c
                  WHERE c.kb_id = kb.kb_id
                    AND c.title = 'Standing scope limits: no plan selection, no carrier recommendation, no per-person affordability');

-- ----------------------------------------------------------------------
-- 4. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V080' AS version, '2026-07-31' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V080',
        'ICHRA/QSEHRA Design Advisor: ICHRA_DESIGN_ADVISOR chatbot_skill row + ichra_design knowledge base and chunks',
        'V080__ichra_design_advisor_skill.sql',
        NOW());
