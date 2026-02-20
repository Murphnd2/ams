# AI Employee Knowledge Assistant — Feature Specification

**Created:** February 19, 2026
**Updated:** February 19, 2026
**Status:** Phase 1 & 2 Complete — Ready for Testing
**Priority:** Medium
**Dependencies:** Anthropic API key (✅ configured), all JSON knowledge base files (✅ complete)

---

## Overview

An AI-powered chatbox embedded in the AMS website that allows authenticated users to ask questions and receive answers sourced from indexed company knowledge bases and resolved ticket history. The system uses Claude's API with retrieval-augmented generation (RAG) — relevant chunks from pre-indexed JSON files plus live database queries are sent as context with each question.

---

## Knowledge Bases

### Static Knowledge Bases (JSON)

All knowledge bases share a common JSON structure and are stored in `src/main/resources/knowledge/`.

| ID | File | Label | Chunks | Size | Access |
|----|------|-------|--------|------|--------|
| `summit` | `summit_guide_indexed.json` | DataPath Summit Guide | 502 | ~1 MB | All authenticated users |
| `summit_videos` | `summit_videos.json` | Summit Training Videos | 30 | Small | All authenticated users |
| `wave` | `wave_help_indexed.json` | Wave Accounting Help | 449 | ~814 KB | Admin only |
| `business_continuity` | `business_continuity_indexed.json` | Business Continuity | TBD | TBD | Admin only |
| `backup_recovery` | `backup_recovery_indexed.json` | Backup & Recovery Procedures | TBD | TBD | Admin only |

### Live Knowledge Base (Database)

**Resolved Tickets** — Completed tickets with resolution-flagged notes are queried live from the database. This provides institutional knowledge of how past issues were resolved.

- **Source:** `Ticket` + `Note` tables (where `isComplete=true` AND `note.isResolution=true`)
- **Legacy cutoff:** Only tickets created on or after 2026-02-19 are included
- **Search:** LIKE matching against ticket description, subcategory, category, and resolution note text
- **Access:** All authenticated users
- **Format:** Results formatted inline with the same context pattern as JSON KB chunks

### Chunk Structure (JSON KBs)
```json
{
  "source": "Name of knowledge base",
  "total_pages": 100,
  "total_chunks": 502,
  "chunks": [
    {
      "title": "Section title",
      "url": "Source URL if applicable",
      "content": "Text content (under 2000 chars per chunk)",
      "section": "Sub-section",
      "category": "Topic category",
      "keywords": ["search", "terms"]
    }
  ]
}
```

### Configuration Registry

`knowledge-config.json` registers all static knowledge bases with metadata used by the search service for routing and access control.

```json
{
  "knowledgeBases": [
    {
      "id": "summit",
      "file": "summit_guide_indexed.json",
      "label": "DataPath Summit Guide",
      "keywords": ["summit", "hsa", "fsa", "hra", "cobra", "billing", "benefits", "enrollment", "debit card", "claims"],
      "enabled": true
    },
    {
      "id": "summit_videos",
      "file": "summit_videos.json",
      "label": "Summit Training Videos",
      "keywords": ["video", "training", "how to", "watch", "tutorial"],
      "enabled": true
    },
    {
      "id": "wave",
      "file": "wave_help_indexed.json",
      "label": "Wave Accounting Help",
      "keywords": ["wave", "invoice", "accounting", "payment", "receipt", "bank", "reports"],
      "enabled": true
    },
    {
      "id": "business_continuity",
      "file": "business_continuity_indexed.json",
      "label": "Business Continuity",
      "keywords": ["continuity", "disaster", "recovery", "systems", "network", "infrastructure", "vendor", "access"],
      "enabled": true
    },
    {
      "id": "backup_recovery",
      "file": "backup_recovery_indexed.json",
      "label": "Backup & Recovery Procedures",
      "keywords": ["backup", "restore", "recovery", "rclone", "wasabi", "s3", "mysql", "dump", "windows server backup"],
      "enabled": true
    }
  ]
}
```

---

## Access Control

Access is role-based, using the existing `User` / `UserRole` system:

| Role | Knowledge Bases Available | Notes |
|------|--------------------------|-------|
| **PSP User** (UserRole id=1) | Summit Guide, Summit Videos, Resolved Tickets | Standard employees — benefits admin questions only |
| **PSP Admin** (UserRole id=5) | All five JSON KBs + Resolved Tickets | Full access including Wave, BC, and Backup/Recovery |

The chatbot servlet checks `AmsDataLocal.isPspAdmin()` from the session and filters which knowledge bases are searchable.

---

## Architecture

### Request Flow

```
User types question in chatbox UI
        ↓
AJAX POST → /ChatAssistant (servlet)
        ↓
Servlet checks authentication (AmsDataLocal from session)
        ↓
Servlet determines user role → filters eligible knowledge bases
        ↓
KnowledgeSearchService.search(question, eligibleKBs)
  - Route to relevant KBs based on keyword matching
  - Search chunk keywords + content for relevance
  - Return top 8 matching chunks
        ↓
TicketKnowledgeDAO.searchResolvedTickets(em, searchTerms, 5)
  - Query completed tickets with resolution notes
  - Filter by legacy cutoff date (2026-02-19)
  - Return top 5 formatted ticket results
        ↓
Combine KB context + ticket context
        ↓
Build Claude API request:
  - System prompt with instructions + citation rules
  - Context: matched chunks + resolved tickets
  - User message: the question
        ↓
POST to Anthropic Messages API (claude-haiku-4-5)
        ↓
JSON response → chatbox UI renders answer
```

### File Layout

```
src/main/java/net/superiorstate/ams/
├── controller/assistant/
│   └── ChatAssistant.java              ← @WebServlet("/ChatAssistant"), AJAX endpoint
├── data/service/
│   ├── ClaudeApiService.java           ← Anthropic API HTTP calls
│   └── KnowledgeSearchService.java     ← JSON KB loading, chunk search, ranking
├── data/dao/
│   └── TicketKnowledgeDAO.java         ← Live query for resolved tickets

src/main/resources/knowledge/
├── knowledge-config.json
├── summit_guide_indexed.json
├── summit_videos.json
├── wave_help_indexed.json
├── business_continuity_indexed.json
└── backup_recovery_indexed.json

src/main/webapp/WEB-INF/view/a/general/
└── chatAssistant25.jsp                 ← Slide-out chatbox UI component
```

### UI Component

A floating button (bottom-right corner) that opens a slide-out chat panel. Included via `<c:import>` in `navbar25.jsp` and available on all authenticated pages. Only renders when `sessionScope.local.isAuthenticated() == true`.

Features:
- Chat bubble UI with user/assistant message styling
- Loading indicator ("Thinking...") during API call
- Markdown-style response formatting (links, bold, line breaks)
- Input disabled while waiting for response
- Color scheme matches AMS brand (`#2B5F8A` primary)

---

## Search Strategy

### KB Routing

1. Tokenize the question into lowercase words (stop words removed)
2. Score each eligible KB by counting keyword matches from `knowledge-config.json`
3. Search all KBs that score above threshold (or default to all eligible if no strong match)

### Chunk Ranking (Weighted Scoring)

| Source | Weight |
|--------|--------|
| Chunk `keywords` array match | 5x |
| Chunk `title` match | 3x |
| Chunk `section` match | 2x |
| Chunk `content` match | 1x |

Returns top 8 chunks sorted by score.

### Resolved Ticket Search

Separate from chunk ranking — queries the database directly:
- Tokenizes question, keeps words > 2 chars
- LIKE matches against: subcategory description, category description, ticket description, resolution note detail
- Returns top 5 most recently resolved matches
- Strips CKEditor HTML from note text
- Only includes tickets created after 2026-02-19 (legacy cutoff)

### Video Enrichment

`summit_videos.json` chunks include Wistia embed URLs in the `url` field. When a video chunk matches, its URL is included in the context so Claude can reference it in the answer.

---

## Claude API Integration

### Model

`claude-haiku-4-5-20251001` — cost-efficient for internal use.

### API Key Storage

Stored in the `constant` table: `ANTHROPIC_API_KEY`. Retrieved via `AppConstantDAO.getConstantValue(em, "ANTHROPIC_API_KEY")`.

### System Prompt

```
You are an AI assistant for employees of a benefits administration company.
Answer questions using ONLY the provided context from our knowledge bases.
If the context doesn't contain enough information to answer, say so clearly.
When citing information, mention the source document name.
If a training video link is included in the context, include it in your response.
Keep answers concise and practical.
Do not make up information that isn't in the provided context.
```

### Technical Details

- HTTP client: `java.net.http.HttpClient` (built-in Java 17)
- JSON library: Gson 2.11.0 (added to pom.xml)
- API version header: `2023-06-01`
- Max tokens: 1024
- Timeout: 30 seconds
- Error handling: User-friendly messages, full errors logged via Log4j2

---

## Database Changes

### New Column: `note.is_resolution`

```sql
ALTER TABLE note ADD COLUMN is_resolution TINYINT(1) NOT NULL DEFAULT 0;
```

Boolean flag on the `Note` entity. When set to `true`, marks this note as the resolution note for its parent ticket. Used by `TicketKnowledgeDAO` to find resolved ticket knowledge.

### New Constant: `ANTHROPIC_API_KEY`

```sql
INSERT INTO constant (name, value, note) VALUES ('ANTHROPIC_API_KEY', '<key>', 'Claude API key for chatbot assistant');
```

### Ticket Category Overhaul

Expired 9 old intent-based categories (active=0), updated 9 kept categories with better descriptions, added 7 new service-oriented categories. See `chatbot_session_summary.md` for full SQL.

---

## Maven Dependency Added

```xml
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
    <version>2.11.0</version>
</dependency>
```

---

## Implementation Status

### Phase 1: Foundation ✅ Complete
- [x] JSON KB files in `src/main/resources/knowledge/`
- [x] `knowledge-config.json` registry
- [x] `KnowledgeSearchService.java` — loads KBs at startup, keyword search, weighted ranking
- [x] `ClaudeApiService.java` — Anthropic API calls via java.net.http
- [x] `ChatAssistant.java` — servlet with auth check, role filtering, search + API orchestration
- [x] `TicketKnowledgeDAO.java` — live resolved ticket queries
- [x] `note.is_resolution` column added
- [x] Anthropic API key stored in `constant` table
- [x] Gson dependency added to pom.xml

### Phase 2: UI ✅ Complete
- [x] `chatAssistant25.jsp` — slide-out panel with AJAX integration
- [x] Included in `navbar25.jsp` (available on all authenticated pages)
- [x] Response rendering with markdown formatting
- [x] Loading indicator and error handling

### Phase 3: Polish — Not Started
- [ ] End-to-end testing (build, deploy, test with real questions)
- [ ] Tune search relevance (keyword weighting, chunk count)
- [ ] Tune system prompt for answer quality
- [ ] Add conversation history (multi-turn within session)
- [ ] Add "was this helpful?" feedback mechanism
- [ ] UI for flagging a note as resolution when closing a ticket
- [ ] Rate limiting consideration

---

## Ticket Category Reference (Current)

### Active Categories (16)

| ID | Short | Description |
|----|-------|-------------|
| 11 | Claims | Claims Processing & Reimbursement |
| 12 | Access | Online Access & Portal Support |
| 13 | Forms | Forms & Documentation |
| 14 | Plans | Plan Design & Configuration |
| 15 | Learn | Training & Education |
| 16 | Enroll | Enrollment & Eligibility Changes |
| 17 | Sales | Sales Inquiries & Quotes |
| 21 | General | Uncategorized / General |
| 22 | Process | Internal Process & Workflow |
| 23 | Debit Card | Debit Card Issues & Activation |
| 24 | COBRA | COBRA Administration & Qualifying Events |
| 25 | Billing | Billing, Invoicing & Payments |
| 26 | Rules | Plan Rules, Compliance & Contribution Limits |
| 27 | Updates | Account Updates & Information Changes |
| 28 | HSA | HSA Contributions, Distributions & Investments |
| 29 | Employer | Employer Administration & File Feeds |

### Expired Categories (9)

| ID | Short | Description | Reason |
|----|-------|-------------|--------|
| 1 | HOW | How Do I Do Something? | Vague intent — replaced by specific categories |
| 2 | WHY | Why Did This Happen? | Same |
| 3 | GET | Did SSA Receive Something? | Same |
| 4 | LAW | What Does The Law Allow For? | Replaced by Rules (26) |
| 5 | OTHER | What Else Can SSA Do For Us? | Replaced by General (21) |
| 6 | NEED | I Need Something! | Vague intent |
| 18 | HELP | Help Me Do Something | Overlaps HOW |
| 19 | QUEST | I Have a Question | Too vague |
| 20 | SALES | I'm Interested in Something | Overlaps Sales (17) |

Historical tickets linked to expired categories are preserved. Expired categories do not appear in ticket creation or sequence management dropdowns (filtered by `active=true` in existing queries).

---

## Future Considerations

- **Resolution note UI:** Add a checkbox or button in the ticket close flow to flag the resolution note. Currently `is_resolution` must be set manually.
- **Knowledge base updates:** Re-crawl sources and regenerate JSON files. No code changes needed — replace files and restart.
- **Additional KBs:** Add new JSON files and register in `knowledge-config.json`. No code changes needed.
- **Client-facing version:** Simplified version for future Client Contact role, limited to Summit KBs.
- **Ticket escalation:** If chatbot can't answer, offer to create a support ticket.
- **Embeddings upgrade:** If keyword search proves insufficient, upgrade to vector embeddings for semantic search.
- **CreateTicket25 auto-categorization:** The keyword-based auto-categorizer in `CreateTicket25` references old category IDs (11, 12, 16, 17, 21). Should be reviewed to ensure it maps to valid active categories.
