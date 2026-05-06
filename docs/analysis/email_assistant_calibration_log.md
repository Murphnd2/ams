# Email Assistant Calibration Log

Living document tracking calibration findings, fixes applied, and outstanding tuning needs across iterations.

---

## Round 4 Initial Calibration Pass (2026-05-05)

### Test environment
- Production deployment.
- 61 style/voice chunks loaded.
- 43 federal rules chunks **NOT YET IMPORTED** as of these tests.
- 0 SSA business chunks loaded.
- Lazy-init working; service initializes on first hit.

### Test 1 — Baseline FSA limit (PASS)

**Inbound:**
- Sender: test@example.com (no AMS resolution)
- Subject: "What's my 2026 fsa limit?"
- Body: "Hi, can you tell me what my 2026 FSA contribution limit is? Thanks."

**Result:**
- Signals: `FULL_DRAFT` / `ESC: NONE` / `ENC: NONE` / `TONE: appropriate`
- All completeness checks ✓
- Subject: "2026 FSA contribution limit"
- Body: 3-paragraph response naming the $3,400 limit, citing 2025 ($3,300) for comparison, explaining per-employee scope, deferring to employer plan document for lower limits, suggesting HR contact.

**Assessment:** Substantively correct draft. Hit SR-1, SR-3, SR-4, SR-5, SR-6, AI-2, AI-3, TV-9, NI-3 cleanly.

**Note:** This worked despite federal rules not being imported. The drafter likely used training-data knowledge for the limit. After Track C import, this should produce a more authoritative-sounding answer with the rule citation.

---

### Test 2 — HARD escalation (PASS)

**Inbound:**
- Subject: "My claim"
- Body: "I am consulting with my attorney about how SSA handled my claim. I will be filing a complaint with the Department of Labor."

**Result:**
- Signals: `FULL_DRAFT` / `ESC: HARD` / `ENC: NONE` / `TONE: not_calibrated`
- Body: empty
- Subject: "Re: My claim" (safe placeholder)
- escalationReason: "Attorney involvement and DOL complaint filing mentioned - requires Kevin's review before any response"

**Assessment:** HARD trigger correctly identified, no draft body produced. Working as designed.

**Issue noted:** TONE marked as `not_calibrated` for empty body. Should be `appropriate` (no body to evaluate).

---

### Test 3 — SOFT_CONF / knowledge gap (PASS)

**Inbound:**
- Subject: "Question"
- Body: "What's the maximum age for FSA dependent care reimbursement for adopted children with developmental delays in foster care arrangements?"

**Result:**
- Signals: `HOLDING_ACKNOWLEDGMENT` / `ESC: SOFT_CONF` / `ENC: NONE` / `TONE: appropriate`
- Body: "I need to research the specific rules for adopted children with developmental delays in foster care arrangements to give you an accurate answer about FSA dependent care age limits. I'm checking on this and will follow up shortly with the complete information."
- escalationQuestionForKevin: "What are the FSA dependent care age limits for adopted children with developmental delays who are in foster care arrangements? Are there extended age provisions or special rules that apply in this situation?"

**Assessment:** Correct gap identification, holding ack produced, precise question for Kevin. Working as designed.

---

### Test 4 — SOFT_JUDG / exception request (FAIL — escalation type wrong)

**Inbound:**
- Subject: "Question"
- Body: "I missed the deadline by 3 weeks but I had a family emergency. Can you make an exception and reimburse my $2,500 in claims from last year?"

**Result:**
- Signals: `FULL_DRAFT` / `ESC: SOFT_CONF` (WRONG — should be SOFT_JUDG) / `ENC: NONE` / `TONE: not_calibrated`
- Completeness: Plan Named ✗, Date Cited ✗, Next Steps ✓
- Body: "The runout period for submitting claims ended 90 days after your plan year ended, and that deadline is set by federal rules that SSA cannot override. Unfortunately, claims submitted after the runout period cannot be reimbursed, even in cases of emergency or other extenuating circumstances. Your employer's HR or benefits contact would be the only party who could potentially consider an exception, as they own the plan terms. You'd need to reach out to them directly to see if any accommodation is possible. I understand this is disappointing given the family emergency situation."

**Issues identified:**
1. **Escalation type wrong**: drafter has the rule (90-day runout, federal), confidently states it, and correctly defers to employer authority. This is SOFT_JUDG (judgment escalation for an exception request), not SOFT_CONF (knowledge gap). The drafter contradicted itself: stated the rule confidently in body but tagged itself as a knowledge gap.
2. **planNamed=false** is wrong — body says "claims" referring to FSA in context. Should be true.
3. **dateCited=false** is wrong — body says "90 days after your plan year ended" and "from last year." Should be true.
4. **toneCalibrated=not_calibrated** is wrong — body is calmly empathetic, appropriate for the situation. Should be appropriate.
5. **TV-4 violation** — uses "Unfortunately" which the style rules explicitly forbid.

**Body substance:** Generally good — defers correctly, doesn't promise, acknowledges emotion briefly. The "Unfortunately" violation is the main content bug.

---

### Test 5 — PHI encryption flag (PASS)

**Inbound:**
- Subject: "Question"
- Body: "My Lipitor prescription claim was denied at Walgreens on March 15th. Can you tell me why?"

**Result:**
- Signals: `FULL_DRAFT` / `ESC: NONE` / `ENC: REQUIRED` / `TONE: appropriate`
- Encryption banner: "Body includes specific prescription name (Lipitor) and pharmacy details"
- Subject: "Prescription claim denial follow-up"
- Body: 3-paragraph response explaining substantiation requirements (credit card receipts insufficient, need EOB or detailed pharmacy receipt with medication name + date filled + cost after insurance), telling participant how to submit (reply to email or upload through portal), promising 2-business-day reprocessing.

**Assessment:** PHI detection correct. Body content reasonable but might over-restate the participant's PHI in the response (per PHI-1 — minimum necessary). Minor — the response references "your prescription claim" by category, doesn't restate "Lipitor at Walgreens on March 15," so it's actually well-handled.

**Issue noted:** Same completeness self-check undermarking as Test 4. Plan Named ✗ when "FSA" / "claim" implied via context.

---

### Test 6 — Real participant FSA email (FAIL — inference quality)

**Status:** Kevin tested with a real FSA email at end of session. **The result was not very good. Claude didn't make correct assumptions about the communications and needs to infer better.**

**Action needed (next session):** Get the specific inbound and draft from Kevin so we can:
- See exactly what the drafter assumed wrong.
- Identify what should have been inferred from context.
- Propose either prompt additions or new style/voice chunks teaching inference patterns.

**Hypothesis on root cause:** The current system prompt and style/voice rules teach the drafter *what to do* but not *how to infer*. Real participant emails contain implicit questions, emotional subtext, assumed context, and prior-history references that the drafter doesn't have explicit guidance on reading.

**Likely fix categories:**
1. Add a new style/voice section: "Inference & Reading Between the Lines" with rules covering:
   - When the literal question and the real question differ (e.g., "can you tell me what's happening" usually means "fix this")
   - Emotional subtext acknowledgment patterns
   - When to ask clarifying questions vs. answer directly
   - How to recognize a participant referring to a prior conversation
2. Possibly add the actual failure case as a calibration example pair (BAD: what was generated; GOOD: what should have been generated).
3. Possibly sharpen the system prompt's instructions for handling ambiguity.

---

## Summary of Issues to Address

| # | Issue | Type | Severity | Status |
|---|---|---|---|---|
| 1 | SOFT_CONF vs SOFT_JUDG distinction unclear | System prompt | Medium | Fix queued |
| 2 | Completeness self-check too strict | System prompt | Medium | Fix queued |
| 3 | toneCalibrated false positives on empty/reasonable bodies | System prompt | Low | Fix queued |
| 4 | "Unfortunately" used despite TV-4 rule | Either rule weight or example needed | Medium | Investigate next session |
| 5 | **Real participant email inference quality** | **Substantive — likely needs new content** | **High** | **Top priority next session** |

---

## Queued Fixes (apply before next test pass)

### Fix 1 — Sharpen escalation type distinction
**Location:** EMAIL_DRAFT_ASSISTANT system prompt → ESCALATION DECISION section

Add after the SOFT_JUDG description:
```
Critical distinction: SOFT_CONF means you LACK information (rule not in knowledge, calculation unclear). SOFT_JUDG means you HAVE the information and produced a complete draft, but the inbound matches a category that warrants review before sending. If you have the rule and applied it confidently, the escalation is SOFT_JUDG, not SOFT_CONF. Discretionary asks ("can you make an exception"), retroactive corrections, and amount-based exception requests are SOFT_JUDG even when you state the rule clearly.
```

### Fix 2 — Loosen completeness self-check
**Location:** EMAIL_DRAFT_ASSISTANT system prompt → COMPLETENESS SELF-CHECK section

Add:
```
Be generous with self-marks: planNamed=true if the plan/account type is clearly identified anywhere in the body (including via context like "your prescription claim" implying FSA). dateCited=true if any date or plan year is mentioned. nextStepsStated=true if the participant knows what to do next. Mark false only when the relevant element is genuinely absent and would have been useful.

For HOLDING_ACKNOWLEDGMENT or HARD-escalation drafts, mark all completeness fields as true (the standard doesn't apply when no substantive draft was produced).
```

### Fix 3 — Recalibrate toneCalibrated rubric
**Location:** EMAIL_DRAFT_ASSISTANT system prompt → COMPLETENESS SELF-CHECK section

Add:
```
toneCalibrated values: "appropriate" is the default for any reasonable draft. "too_warm" or "too_curt" only when the tone clearly mismatches the inbound register. "not_calibrated" only when the tone is genuinely unprofessional, contradictory, or unfit for the audience. Empty bodies (HARD escalation) should be marked "appropriate" because no tone was generated to evaluate.
```

### Fix 4 — Investigate "Unfortunately" usage
**Location:** Either the existing TV-4 chunk in style_voice KB, or a new emphasis in the system prompt.

The drafter used "Unfortunately, claims submitted after the runout period..." despite TV-4 explicitly forbidding the word. This suggests either:
- The TV-4 chunk content isn't strong enough, OR
- The retrieval ranking didn't surface TV-4 prominently for this query, OR
- The system prompt should call out specific forbidden words.

**Action:** Check whether TV-4 was in the searched/loaded chunks for this draft (it should be — `style_voice` is always-loaded). If yes, the issue is rule-weight; consider adding a "FORBIDDEN WORDS" subsection to the system prompt that explicitly lists "Unfortunately" and a few others.

### Fix 5 — Real-email inference issue (substantive — needs Kevin's failure case first)

**Cannot apply until next session** — needs the specific inbound + bad draft from Kevin to diagnose.

---

## Methodology Notes for Future Calibration Sessions

### What worked
- Sequential test cases that exercise different code paths (HARD, SOFT_CONF, PHI, baseline) gave clear pass/fail signals.
- Screenshot capture of the test page surfaced metadata bugs that wouldn't be visible from raw JSON.

### What didn't work / needs improvement
- Tested with `test@example.com` rather than real AMS-resolved senders. Should test with both kinds.
- Hadn't yet imported federal rules content during the test pass — meaning the drafter was operating without authoritative ground truth on FSA rules. Re-test after import.
- Single-pass tests don't reveal calibration on follow-up exchanges (REFINE mode). Should test refine path too.

### Recommended test set for next session
After applying fixes 1-3 and getting Kevin's failure case for fix 5:

1. **Real Deb-thread inbounds** — paste 3-4 verbatim anonymized inbounds from past Deb threads. These are the calibration ground truth.
2. **Re-run Tests 2-5** — verify metadata fixes landed.
3. **AMS-resolved sender** — same baseline FSA test but with a real participant email from the DB.
4. **REFINE mode** — generate, then iterate via "Use this draft as input for Refine" with notes like "make it shorter" or "add timing."
5. **Multi-issue inbound** — 3 distinct questions in one email, verify SS-5 numbered handling.
6. **Frustrated participant** — explicitly emotional inbound, verify SS-1 acknowledgment + action pattern.
