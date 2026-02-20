# AI Employee Knowledge Assistant — Feature Specification

**Created:** February 19, 2026  
**Status:** Planning  
**Priority:** Medium  
**Branch:** `feature/ai-chatbot` (when ready)  
**Dependencies:** Anthropic API key, all JSON knowledge base files finalized

---

## Overview

An AI-powered chatbox embedded in the AMS website that allows authenticated users to ask questions and receive answers sourced from indexed company knowledge bases. The system uses Claude's API with retrieval-augmented generation (RAG) — relevant chunks from pre-indexed JSON files are sent as context with each question.

---

## Knowledge Bases

All knowledge bases share a common JSON structure and are stored in `src/main/resources/knowledge/`.

| ID | File | Label | Chunks | Size | Access |
|----|------|-------|--------|------|--------|
| `summit` | `summit_guide_indexed.json` | DataPath Summit Guide | 502 | ~1 MB | All authenticated users |
| `summit_videos` | `summit_videos.json` | Summit Training Videos | 30 | Small | All authenticated users |
| `wave` | `wave_help_indexed.json` | Wave Accounting Help | 449 | ~814 KB | Admin only |
| `business_continuity` | `business_continuity_indexed.json` | Business Continuity | TBD | TBD | Admin only |
| `backup_recovery` | `backup_recovery_indexed.json` | Backup & Recovery Procedures | TBD | TBD | Admin only |

### Chunk Structure
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

`knowledge-config.json` registers all knowledge bases with metadata used by the search service for routing and access control.

```json
{
  "knowledgeBases": [
    {
      "id": "summit",
      "file": "summit_guide_indexed.json",
      "label": "DataPath Summit Guide",
      "keywords": ["summit", "hsa", "fsa", "hra", "cobra", "billing", "benefits", "enrollment", "debit card", "claims"],
      "accessRole": "user",
      "enabled": true
    },
    {
      "id": "summit_videos",
      "file": "summit_videos.json",
      "label": "Summit Training Videos",
      "keywords": ["video", "training", "how to", "watch", "tutorial"],
      "accessRole": "user",
      "enabled": true
    },
    {
      "id": "wave",
      "file": "wave_help_indexed.json",
      "label": "Wave Accounting Help",
      "keywords": ["wave", "invoice", "accounting", "payment", "receipt", "bank", "reports"],
      "accessRole": "admin",
      "enabled": true
    },
    {
      "id": "business_continuity",
      "file": "business_continuity_indexed.json",
      "label": "Business Continuity",
      "keywords": ["continuity", "disaster", "recovery", "systems", "network", "infrastructure", "vendor", "access"],
      "accessRole": "admin",
      "enabled": true
    },
    {
      "id": "backup_recovery",
      "file": "backup_recovery_indexed.json",
      "label": "Backup & Recovery Procedures",
      "keywords": ["backup", "restore", "recovery", "rclone", "wasabi", "s3", "mysql", "dump", "windows server backup"],
      "accessRole": "admin",
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
| **User** (UserRole id=1) | Summit Guide, Summit Videos | Standard PSP employees — benefits admin questions only |
| **Admin** (UserRole id=2) | All five knowledge bases | Internal admins — full access including Wave, BC, and Backup/Recovery |

The chatbot servlet checks the authenticated user's roles from `AmsDataLocal` (session) and filters which knowledge bases are searchable based on the `accessRole` field in `knowledge-config.json`.

---

## Architecture

### Request Flow

```
User types question in chatbox UI
        ↓
AJAX POST → /ChatAssistant (servlet)
        ↓
Servlet checks authentication (LoginFilter already covers this)
        ↓
Servlet determines user role → filters eligible knowledge bases
        ↓
KnowledgeSearchService.search(question, eligibleKBs)
  - Route to relevant KBs based on keyword matching against question
  - Search chunk keywords + content for relevance
  - Return top N matching chunks (e.g., top 5-8)
        ↓
Build Claude API request:
  - System prompt with instructions + source citation rules
  - Context: matched chunks with source labels
  - User message: the question
        ↓
POST to Anthropic Messages API (claude-haiku-4-5 or claude-sonnet-4-5)
        ↓
Parse response, include video links if relevant
        ↓
JSON response → chatbox UI renders answer with citations
```

### File Storage

```
src/main/resources/knowledge/
├── knowledge-config.json
├── summit_guide_indexed.json
├── summit_videos.json
├── wave_help_indexed.json
├── business_continuity_indexed.json
└── backup_recovery_indexed.json
```

### Proposed Java Components

```
controller/
└── assistant/
    └── ChatAssistant.java          ← @WebServlet, handles AJAX POST

data/
└── service/
    ├── KnowledgeSearchService.java ← Loads KBs, searches chunks, ranks results
    └── ClaudeApiService.java       ← Builds and sends requests to Anthropic API
```

### UI Component

A chatbox panel embedded in the main AMS layout (likely in `navbar25.jsp` or a shared include). Opens as a slide-out or modal. Available on all authenticated pages.

---

## Search Strategy

### KB Routing

When a question comes in, the system determines which knowledge base(s) to search:

1. Tokenize the question into lowercase words
2. Score each eligible KB by counting keyword matches from `knowledge-config.json`
3. Search all KBs that score above a threshold (or default to all eligible if no strong match)

### Chunk Ranking

Within selected KBs, rank chunks by relevance:

1. Exact keyword matches in `keywords` array (highest weight)
2. Keyword matches in `title` and `section` fields
3. Keyword matches in `content` (lowest weight)
4. Return top 5-8 chunks sorted by score

### Video Enrichment

After selecting context chunks, also check `summit_videos.json` for title matches against the question. If a training video is relevant, append its Wistia embed URL to the response context so Claude can include it in the answer.

---

## Claude API Integration

### Model Selection

Use `claude-haiku-4-5` for cost efficiency. Fall back to `claude-sonnet-4-5` if response quality needs improvement for certain KB types.

### API Key Storage

Store the Anthropic API key as a database constant in the existing `AppConstant` table (same pattern as SMTP credentials). Retrieve via `AppConstantDAO.getConstantValue(em, "ANTHROPIC_API_KEY")`.

### System Prompt (Draft)

```
You are an AI assistant for employees of a benefits administration company. 
Answer questions using ONLY the provided context from our knowledge bases. 
If the context doesn't contain enough information to answer, say so clearly.

When citing information, mention the source document name.
If a training video is relevant, include the video link in your response.
Keep answers concise and practical.
Do not make up information that isn't in the provided context.
```

### Request Format

Standard Anthropic Messages API POST to `https://api.anthropic.com/v1/messages` with:
- `model`: `claude-haiku-4-5-20251001` (or configured model string)
- `max_tokens`: 1024
- `system`: System prompt above
- `messages`: Single user message containing the question + context chunks

### Cost Considerations

Haiku pricing is significantly lower than Sonnet. With ~2000 chars per chunk and 5-8 chunks per request, each query sends roughly 10-16K characters of context (~3-5K tokens input). At Haiku rates this should be very economical for internal employee use.

---

## Implementation Phases

### Phase 1: Foundation
- Create `src/main/resources/knowledge/` directory
- Add all JSON KB files and `knowledge-config.json`
- Build `KnowledgeSearchService` — load KBs at startup, implement search
- Build `ClaudeApiService` — API key retrieval, request/response handling
- Build `ChatAssistant` servlet — role check, search, API call, response

### Phase 2: UI
- Build chatbox component (slide-out panel or modal)
- Embed in shared layout (all authenticated pages)
- AJAX integration with ChatAssistant servlet
- Render responses with source citations and video links

### Phase 3: Polish
- Tune search relevance (keyword weighting, chunk count)
- Tune system prompt for answer quality
- Add conversation history (multi-turn within session)
- Add "was this helpful?" feedback mechanism
- Loading indicator, error handling, rate limiting

---

## Future Considerations

- **Knowledge base updates:** Re-crawl sources and regenerate JSON files as documentation changes. No code changes needed — just replace the JSON files and restart.
- **Additional KBs:** Add new JSON files and register in `knowledge-config.json`. No code changes needed.
- **Client-facing version:** Eventually, a simplified version could be exposed to client contacts (the future "Client Contact" role) with access limited to Summit-related KBs only.
- **Ticket integration:** If the chatbot can't answer a question, offer to create a support ticket from the conversation. Ties into the existing Ticket activity type.
- **Embeddings upgrade:** If keyword search proves insufficient, upgrade to vector embeddings for semantic search. Would require an embedding model and a vector similarity library.

---

## Dependencies

| Dependency | Status | Notes |
|------------|--------|-------|
| Anthropic API key | In progress | Being set up |
| `summit_guide_indexed.json` | Complete | 502 chunks |
| `summit_videos.json` | Complete | 30 video mappings |
| `wave_help_indexed.json` | Complete | 449 chunks |
| `business_continuity_indexed.json` | Complete | Finalized |
| `backup_recovery_indexed.json` | Complete | Finalized |
| HTTP client library | Evaluate | `java.net.http.HttpClient` (built-in Java 11+) or add dependency |
| JSON parsing | Evaluate | Jakarta JSON-P, Gson, or Jackson (check what's already in pom.xml) |
