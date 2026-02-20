# AMS Project Backlog

**Created:** February 19, 2026  
**Last Updated:** February 19, 2026  
**Reference:** `ams_to_be_vision.md` for full project descriptions

---

## Legend

| Priority | Meaning |
|----------|---------|
| **CONF** | Must be demo-ready for Datapath Client Conference (April 20–22, 2026) |
| **HIGH** | Important for business operations, near-term |
| **MED** | Planned feature, no hard deadline |
| **LOW** | Future / nice-to-have |

| Status | Meaning |
|--------|---------|
| ✅ Done | Complete and in production |
| 🔨 Active | Currently in development |
| 📋 Planned | Scoped and ready to start |
| 💡 Backlog | Defined but not yet scoped in detail |

---

## Conference Tier (April 2026 Deadline)

| # | Feature | Priority | Status | Spec Doc | Notes |
|---|---------|----------|--------|----------|-------|
| 1 | Users and Roles Enhancement | CONF | 🔨 Active | `ams_to_be_vision.md` §1 | User/Admin/Accountant roles. SSO future. Basic auth working. |
| 2 | Sales Portal | CONF | 🔨 Active | `sales_pipeline_data_model.md`, `sales_pipeline_implementation_log.md` | Proposal flow, application, rate tables. Entity model built. ViewProposal working. |
| 3 | Sales / Marketing Library | CONF | 💡 Backlog | `ams_to_be_vision.md` §3 | MarketingMaterial entity created. UI and Wasabi storage TBD. |
| 4 | Sequence Template Overhaul | CONF | ✅ Done | `sequence_overhaul_summary.md` | New drag-and-drop builder complete. Old pages preserved for cleanup. |
| 5 | Third-Party Vendor Task Outsourcing | CONF | 💡 Backlog | `ams_to_be_vision.md` §5 | Federated model. Task entity has sourcing flags. API sync layer TBD. |
| 6 | Email System Standardization | CONF | 💡 Backlog | `ams_to_be_vision.md` §6 | Consolidate Graph + SMTP paths. SMTP as baseline for all PSPs. |
| 7 | Datapath API Readiness | CONF | 💡 Backlog | `ams_to_be_vision.md` §7 | Design principle — abstract data sources from business logic. |
| 8 | Deployment Readiness | CONF | 💡 Backlog | `ams_to_be_vision.md` §8 | VM provisioning, initialize flow audit, WAR deployment. |
| 9 | Mobile-Friendly Design | CONF | 💡 Backlog | `ams_to_be_vision.md` §9 | Sales portal mobile-first. Responsive improvements elsewhere. |

---

## Business / Operations Tier

| # | Feature | Priority | Status | Spec Doc | Notes |
|---|---------|----------|--------|----------|-------|
| 10 | Monthly Billing Automation | HIGH | 🔨 Active | `ams_to_be_vision.md` §10 | Core billing flow exists. Enhancements ongoing. |
| 11 | Time Tracking | MED | 💡 Backlog | `ams_to_be_vision.md` §11 | TimeClock25 exists. Payroll export TBD. |
| 12 | Employee Onboarding Portal | MED | 💡 Backlog | `ams_to_be_vision.md` §12 | Self-service portal for new hires. |
| 13 | Invoicing System | MED | 💡 Backlog | `ams_to_be_vision.md` §13 | Replace Wave invoicing. Auto-generate from rate data. |
| 14 | Payroll-to-Accounting Automation | LOW | 💡 Backlog | `ams_to_be_vision.md` §14 | Patriot → Wave journal entries via API. |
| 15 | **AI Employee Knowledge Assistant** | **MED** | **📋 Planned** | **`ai_chatbot_feature_spec.md`** | **See below for details.** |

---

## Technical Debt / Infrastructure

| # | Item | Priority | Status | Spec Doc | Notes |
|---|------|----------|--------|----------|-------|
| T1 | Code cleanup — dead code removal | HIGH | ✅ Done | `cleanup_sweep_summary.md` | 238 files deleted, 32 renamed. `previous/` package eliminated. |
| T2 | Package reorganization | HIGH | ✅ Done | `migration_strategy.md` | All code in clean packages. Migration complete. |
| T3 | Data layer rename | HIGH | ✅ Done | `data_layer_inventory.md` | All cryptic names replaced with descriptive names. |
| T4 | Modal servlet analysis | MED | ✅ Done | `modal_servlet_analysis.md` | All modals mapped to servlets. |
| T5 | Sequence builder old page cleanup | LOW | 📋 Planned | `sequence_overhaul_summary.md` | Delete old builder JSPs/servlets after new builder proven. |
| T6 | Database migration tracking | HIGH | 🔨 Active | `migration_tracker.md` | 3 migration scripts pending production deployment. |

---

## Post-Conference Tier

| # | Feature | Priority | Status | Spec Doc | Notes |
|---|---------|----------|--------|----------|-------|
| 16 | Social Media Marketing Automation | LOW | 💡 Backlog | `ams_to_be_vision.md` §16 | AI-powered content for LSA, ICHRA, HSA growth. |

---

## Feature Detail: AI Employee Knowledge Assistant (#15)

**Full spec:** `docs/analysis/ai_chatbot_feature_spec.md`

**Summary:** Embedded chatbox in AMS that answers employee questions from indexed knowledge bases using Claude's API (RAG approach).

**Knowledge Bases:**
- DataPath Summit Guide (502 chunks) — all users
- Summit Training Videos (30 videos) — all users
- Wave Accounting Help (449 chunks) — admin only
- Business Continuity (TBD chunks) — admin only
- Backup & Recovery Procedures (TBD chunks) — admin only

**Access Control:**
- Standard PSP users (UserRole 1) → Summit knowledge only
- Admins (UserRole 2) → All five knowledge bases

**Key Components:**
- `src/main/resources/knowledge/` — JSON KB files + config
- `ChatAssistant` servlet — AJAX endpoint, role check, orchestration
- `KnowledgeSearchService` — KB loading, chunk search, relevance ranking
- `ClaudeApiService` — Anthropic API integration
- Chatbox UI component — slide-out panel in shared layout

**Model:** Claude Haiku 4.5 (cost-efficient for internal use)

**Dependencies:**
- Anthropic API key (in progress)
- All 5 JSON knowledge base files (3 complete, 2 complete)
- HTTP client (java.net.http built-in)
- JSON parsing library (evaluate existing pom.xml)

**Phases:**
1. Foundation — KB storage, search service, API service, servlet
2. UI — chatbox component, AJAX integration, response rendering
3. Polish — relevance tuning, conversation history, feedback, error handling
