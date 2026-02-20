# AI Chatbot Implementation Session — February 19, 2026

**Branch:** main (or `feature/ai-chatbot` if branched)
**Status:** Phase 1 & 2 complete, ready for first build and test

---

## Summary

Built the full AI Knowledge Assistant chatbot feature from scratch in one session. The system uses Claude's API (Haiku 4.5) with RAG — searching pre-indexed JSON knowledge bases plus live resolved ticket history to provide context-aware answers to employee questions.

---

## Files Created (6)

| File | Location | Purpose |
|------|----------|---------|
| `ClaudeApiService.java` | `data/service/` | Calls Anthropic Messages API, retrieves API key from DB |
| `KnowledgeSearchService.java` | `data/service/` | Loads JSON KBs at startup, keyword routing, weighted chunk ranking |
| `TicketKnowledgeDAO.java` | `data/dao/` | Live query for completed tickets with resolution notes |
| `ChatAssistant.java` | `controller/assistant/` | `@WebServlet("/ChatAssistant")` — AJAX endpoint, orchestrates search + API |
| `chatAssistant25.jsp` | `WEB-INF/view/a/general/` | Slide-out chatbox UI with AJAX integration |
| `ai_chatbot_feature_spec.md` | `docs/analysis/` | Updated feature spec (replaces previous planning version) |

## Files Modified (4)

| File | Change |
|------|--------|
| `pom.xml` | Added Gson 2.11.0 dependency |
| `Note.java` | Added `isResolution` boolean field + getter/setter |
| `navbar25.jsp` | Added `<c:import>` for `chatAssistant25.jsp` at end of file |
| `ChatAssistant.java` | Wired in `TicketKnowledgeDAO` for live ticket context |

---

## Database Changes

### Schema Changes

```sql
-- Add resolution flag to notes
ALTER TABLE note ADD COLUMN is_resolution TINYINT(1) NOT NULL DEFAULT 0;
```

### Data Changes

```sql
-- Store Anthropic API key
INSERT INTO constant (name, value, note)
VALUES ('ANTHROPIC_API_KEY', '<key>', 'Claude API key for chatbot assistant');

-- Expire old intent-based ticket categories
UPDATE ticketcategory SET active = 0 WHERE category_id IN (1,2,3,4,5,6,18,19,20);

-- Update kept categories with better descriptions
UPDATE ticketcategory SET short_text = 'Claims', description = 'Claims Processing & Reimbursement' WHERE category_id = 11;
UPDATE ticketcategory SET short_text = 'Access', description = 'Online Access & Portal Support' WHERE category_id = 12;
UPDATE ticketcategory SET short_text = 'Forms', description = 'Forms & Documentation' WHERE category_id = 13;
UPDATE ticketcategory SET short_text = 'Plans', description = 'Plan Design & Configuration' WHERE category_id = 14;
UPDATE ticketcategory SET short_text = 'Learn', description = 'Training & Education' WHERE category_id = 15;
UPDATE ticketcategory SET short_text = 'Enroll', description = 'Enrollment & Eligibility Changes' WHERE category_id = 16;
UPDATE ticketcategory SET short_text = 'Sales', description = 'Sales Inquiries & Quotes' WHERE category_id = 17;
UPDATE ticketcategory SET short_text = 'General', description = 'Uncategorized / General' WHERE category_id = 21;
UPDATE ticketcategory SET short_text = 'Process', description = 'Internal Process & Workflow' WHERE category_id = 22;

-- Add new service-oriented categories
INSERT INTO ticketcategory (category_id, short_text, description, active) VALUES (23, 'Debit Card', 'Debit Card Issues & Activation', 1);
INSERT INTO ticketcategory (category_id, short_text, description, active) VALUES (24, 'COBRA', 'COBRA Administration & Qualifying Events', 1);
INSERT INTO ticketcategory (category_id, short_text, description, active) VALUES (25, 'Billing', 'Billing, Invoicing & Payments', 1);
INSERT INTO ticketcategory (category_id, short_text, description, active) VALUES (26, 'Rules', 'Plan Rules, Compliance & Contribution Limits', 1);
INSERT INTO ticketcategory (category_id, short_text, description, active) VALUES (27, 'Updates', 'Account Updates & Information Changes', 1);
INSERT INTO ticketcategory (category_id, short_text, description, active) VALUES (28, 'HSA', 'HSA Contributions, Distributions & Investments', 1);
INSERT INTO ticketcategory (category_id, short_text, description, active) VALUES (29, 'Employer', 'Employer Administration & File Feeds', 1);
```

---

## Production Deployment SQL

Run this script on the production database when deploying this feature:

```sql
-- 1. Schema: Add resolution flag to notes
ALTER TABLE note ADD COLUMN is_resolution TINYINT(1) NOT NULL DEFAULT 0;

-- 2. Data: Add Anthropic API key (replace <PRODUCTION_KEY> with actual key)
INSERT INTO constant (name, value, note)
VALUES ('ANTHROPIC_API_KEY', '<PRODUCTION_KEY>', 'Claude API key for chatbot assistant');

-- 3. Data: Expire old ticket categories
UPDATE ticketcategory SET active = 0 WHERE category_id IN (1,2,3,4,5,6,18,19,20);

-- 4. Data: Update kept category descriptions
UPDATE ticketcategory SET short_text = 'Claims', description = 'Claims Processing & Reimbursement' WHERE category_id = 11;
UPDATE ticketcategory SET short_text = 'Access', description = 'Online Access & Portal Support' WHERE category_id = 12;
UPDATE ticketcategory SET short_text = 'Forms', description = 'Forms & Documentation' WHERE category_id = 13;
UPDATE ticketcategory SET short_text = 'Plans', description = 'Plan Design & Configuration' WHERE category_id = 14;
UPDATE ticketcategory SET short_text = 'Learn', description = 'Training & Education' WHERE category_id = 15;
UPDATE ticketcategory SET short_text = 'Enroll', description = 'Enrollment & Eligibility Changes' WHERE category_id = 16;
UPDATE ticketcategory SET short_text = 'Sales', description = 'Sales Inquiries & Quotes' WHERE category_id = 17;
UPDATE ticketcategory SET short_text = 'General', description = 'Uncategorized / General' WHERE category_id = 21;
UPDATE ticketcategory SET short_text = 'Process', description = 'Internal Process & Workflow' WHERE category_id = 22;

-- 5. Data: Add new ticket categories
INSERT INTO ticketcategory (category_id, short_text, description, active) VALUES (23, 'Debit Card', 'Debit Card Issues & Activation', 1);
INSERT INTO ticketcategory (category_id, short_text, description, active) VALUES (24, 'COBRA', 'COBRA Administration & Qualifying Events', 1);
INSERT INTO ticketcategory (category_id, short_text, description, active) VALUES (25, 'Billing', 'Billing, Invoicing & Payments', 1);
INSERT INTO ticketcategory (category_id, short_text, description, active) VALUES (26, 'Rules', 'Plan Rules, Compliance & Contribution Limits', 1);
INSERT INTO ticketcategory (category_id, short_text, description, active) VALUES (27, 'Updates', 'Account Updates & Information Changes', 1);
INSERT INTO ticketcategory (category_id, short_text, description, active) VALUES (28, 'HSA', 'HSA Contributions, Distributions & Investments', 1);
INSERT INTO ticketcategory (category_id, short_text, description, active) VALUES (29, 'Employer', 'Employer Administration & File Feeds', 1);
```

---

## Key Design Decisions

1. **Live ticket KB over static export** — Resolved tickets are queried from the database in real-time rather than exported to JSON. Always current, minimal DB load (single query per chat question, filtered and limited).

2. **Legacy cutoff date** — Tickets created before 2026-02-19 are excluded from the chatbot's ticket knowledge. Only new tickets with proper categorization and resolution notes are included.

3. **Resolution flag on Note** — Added `is_resolution` boolean to the `Note` entity. Only notes explicitly flagged as resolutions are included in chatbot context. Prevents the chatbot from using intermediate status notes as answers.

4. **Ticket category overhaul** — Replaced vague intent-based categories (HOW, WHY, NEED) with service-oriented categories (Claims, Debit Card, COBRA, HSA, etc.). Expired categories are soft-disabled (`active=0`), preserving all historical data.

5. **KnowledgeSearchService in application scope** — Initialized once on first request, stored in `ServletContext`. JSON KBs are loaded into memory and shared across all requests. Thread-safe (read-only after init).

6. **Gson for JSON** — Added as explicit dependency rather than relying on transitive dependency from Microsoft Graph. Lightweight, well-suited for the simple JSON structures involved.

---

## Next Steps (Phase 3)

1. **Build and test** — Maven build, deploy to Tomcat, verify chatbox appears and sends/receives
2. **Resolution note UI** — Add checkbox/button in ticket close flow to flag the resolution note
3. **Tune search relevance** — Adjust keyword weights and chunk counts based on real usage
4. **Tune system prompt** — Refine based on answer quality observations
5. **Conversation history** — Multi-turn context within a session
6. **Review CreateTicket25 auto-categorizer** — Currently references category IDs (11, 12, 16, 17, 21) which are still valid, but should be reviewed against the new category set
