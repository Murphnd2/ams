# AMS Project Backlog

**Created:** February 19, 2026
**Last Updated:** February 25, 2026
**Reference:** `docs/ams_to_be_vision.md` for full project descriptions

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
| ✅ Done | Complete and in production (or ready for production) |
| 🔨 Active | Currently in development |
| 📋 Planned | Scoped and ready to start |
| 💡 Backlog | Defined but not yet scoped in detail |

---

## Conference Tier (April 2026 Deadline)

| # | Feature | Priority | Status | Spec Doc | Notes |
|---|---------|----------|--------|----------|-------|
| 1 | Users and Roles Enhancement | CONF | 🔨 Active | `ams_to_be_vision.md` §1 | Agent + Agency Manager roles built. Invitation system complete. SSO future. |
| 2 | **Sales Portal** | **CONF** | **🔨 Active** | **`sales_pipeline_reference.md`** | **Full pipeline built. Service Manager, Rate Manager, Agency Manager, Opportunity system, Agent Landing Page all complete. See status detail below.** |
| 3 | Sales / Marketing Library | CONF | ✅ Done | `session_history_archive.md` (Feb 21) | Resource Library UI, Wasabi upload/download, category management, feature linking to proposals. |
| 4 | Sequence Template Overhaul | CONF | ✅ Done | `session_history_archive.md` (Feb 19) | New drag-and-drop builder complete. Old pages preserved for cleanup. |
| 5 | Third-Party Vendor Task Outsourcing | CONF | 💡 Backlog | `ams_to_be_vision.md` §5 | Federated model. Task entity has sourcing flags. API sync layer TBD. |
| 6 | Email System Standardization | CONF | ✅ Done | `email_workflow_analysis.md` | Wasabi attachments, branded HTML templates, SMTP sending. Graph API removed. |
| 7 | Datapath API Readiness | CONF | 💡 Backlog | `ams_to_be_vision.md` §7 | Design principle — abstract data sources from business logic. |
| 8 | Deployment Readiness | CONF | ✅ Done | `deployment_strategy.md`, `deployment_runbook.md` | Multi-PSP infrastructure built. Master VPS image ready. Backup/update/health scripts deployed. |
| 9 | Mobile-Friendly Design | CONF | 💡 Backlog | `ams_to_be_vision.md` §9 | Sales portal mobile-first. Responsive improvements elsewhere. |

---

## Business / Operations Tier

| # | Feature | Priority | Status | Spec Doc | Notes |
|---|---------|----------|--------|----------|-------|
| 10 | Monthly Billing Automation | HIGH | 🔨 Active | `ams_to_be_vision.md` §10 | Core billing flow exists. Enhancements ongoing. |
| 11 | Time Tracking | MED | 🔨 Active | `session_history_archive.md` (Feb 22) | UI redesigned (daily/weekly views). Correction request workflow built (submit/review/approve). Payroll export TBD. |
| 12 | Employee Onboarding Portal | MED | 💡 Backlog | `ams_to_be_vision.md` §12 | Self-service portal for new hires. |
| 13 | Invoicing System | MED | 💡 Backlog | `ams_to_be_vision.md` §13 | Replace Wave invoicing. Auto-generate from rate data. |
| 14 | Payroll-to-Accounting Automation | LOW | 💡 Backlog | `ams_to_be_vision.md` §14 | Patriot → Wave journal entries via API. |
| 15 | **AI Employee Knowledge Assistant** | **MED** | **✅ Done** | **`session_history_archive.md` (Feb 19)** | **Built. Pending production deploy (API key + DB changes).** |

---

## Technical Debt / Infrastructure

| # | Item | Priority | Status | Spec Doc | Notes |
|---|------|----------|--------|----------|-------|
| T1 | Code cleanup — dead code removal | HIGH | ✅ Done | `session_history_archive.md` (Feb 15–17) | 238 files deleted, 32 renamed. `previous/` package eliminated. |
| T2 | Package reorganization | HIGH | ✅ Done | `session_history_archive.md` (Feb 15–17) | All code in clean packages. |
| T3 | Data layer rename | HIGH | ✅ Done | `session_history_archive.md` (Feb 15–17) | All cryptic names replaced. |
| T4 | Modal servlet analysis | MED | ✅ Done | — | All modals mapped to servlets. All in modern `controller` package. |
| T5 | Sequence builder old page cleanup | LOW | 📋 Planned | `session_history_archive.md` (Feb 19) | Delete old builder JSPs/servlets after new builder proven. |
| T6 | Database migration tracking | HIGH | 🔨 Active | `migration_tracker.md` | 10 scripts tracked. Most pending production. See migration tracker. |
| T7 | Docs cleanup & consolidation | MED | ✅ Done | — | Session summaries consolidated into `session_history_archive.md`. Stale refs cleaned. |
| T8 | Empty checklist / todo list handling | LOW | 💡 Backlog | `ams_to_be_vision.md` §17 | Remove task-153 dummy workaround. Audit display chain for empty todo list safety. |
| T9 | Refactor manual setup to dynamic LOS | CONF | 📋 Planned | — | `GenerateProp25` uses hardcoded `q1`–`q8` flags mapped to old LOS IDs (5–10) and hardcoded TP IDs (11–19). Needs refactor to dynamic LOS from DB. Stopgap form (`manualSetup.jsp`) works for original 8 modules. |
| T10 | PspAgencyHome scoping | HIGH | 📋 Planned | `session_history_archive.md` (Feb 21) | Agency Manager should only see their own agency in PspAgencyHome. Hide rate management for non-PSP users. |
| T11 | Layout/appearance consolidation | MED | ✅ Done | `session_history_archive.md` (Feb 21) | Unified navbar, CSS, admin pages. Create Ticket modal rebuilt. Email screen modernized. |

---

## Post-Conference Tier

| # | Feature | Priority | Status | Spec Doc | Notes |
|---|---------|----------|--------|----------|-------|
| 16 | Social Media Marketing Automation | LOW | 💡 Backlog | `ams_to_be_vision.md` §16 | AI-powered content for LSA, ICHRA, HSA growth. |

---

## Sales Portal — Current Status Detail

**Full reference:** `docs/analysis/sales_pipeline_reference.md`

| Pipeline Step | Status |
|---------------|--------|
| Service Manager (LOS, Enhancements, App Sections, Features) | ✅ |
| Rate Manager (Rates, Fee Types, Rate Tables) | ✅ |
| Agency Manager (Agencies, Agents, Rate Assignment) | ✅ |
| Invitation System (PSP → Agent registration flow) | ✅ |
| Resource Library (Upload, categorize, link to features) | ✅ |
| Proposal Builder | ✅ |
| Proposal Detail (internal) | ✅ |
| Proposal Feature Rendering (inline links, resource icons) | ✅ |
| Send Proposal (email) | ✅ |
| Proposal Landing Page (public) | ✅ |
| Application Form (dynamic sections, conditional logic, IRS limits) | ✅ |
| Save/Restore Progress | ✅ |
| Rate Sheet Upload (Wasabi) | ✅ |
| Submit Application | ✅ |
| Application Review/Approve UI | ✅ |
| Automated Setup Creation | ✅ |
| Opportunity System (agent pipeline tracking) | ✅ |
| Agent Landing Page (pipeline view, stage management) | ✅ |
| Agent-scoped ProposalBuilder | ✅ |
| Full Pipeline Test | ❌ Next |
| Production DB Migration | ❌ 10 scripts pending |

---

## AI Chatbot — Deployment Checklist

Code is complete. Production deployment requires:
- [ ] Run schema change: `ALTER TABLE note ADD COLUMN is_resolution TINYINT(1) NOT NULL DEFAULT 0;`
- [ ] Insert API key: `INSERT INTO constant (name, value, note) VALUES ('ANTHROPIC_API_KEY', '<key>', '...');`
- [ ] Update ticket categories (see `session_history_archive.md` Feb 19 chatbot entry for context)
- [ ] Deploy WAR with chatbot code
- [ ] Verify knowledge base JSON files are deployed to `src/main/resources/knowledge/`

---

## Reference Documents Index

| Document | Location | Purpose |
|----------|----------|---------|
| `ams_to_be_vision.md` | `docs/` | Full project vision and feature descriptions |
| `deployment_strategy.md` | `docs/` | Multi-PSP architecture, VPS, config, releases |
| `deployment_runbook.md` | `docs/` | Step-by-step PSP provisioning checklist |
| `deployment_backlog.md` | `docs/` | Deployment work items (D-numbers) |
| `tomcat_ssl_setup.md` | `docs/` | SSL/HTTPS setup guide |
| `schema_version_migration.sql` | `docs/` | Schema version table + retroactive inserts |
| `sales_pipeline_reference.md` | `docs/analysis/` | Sales pipeline entities, servlets, lifecycle states |
| `migration_tracker.md` | `docs/analysis/` | Which DB scripts are applied to which environments |
| `entity_reference.md` | `docs/analysis/` | Full entity map (core + sales + opportunity entities) |
| `application_flow.md` | `docs/analysis/` | Entry points, navigation flows, filter chain, tech stack |
| `email_workflow_analysis.md` | `docs/analysis/` | Email system (Wasabi, SMTP, branded templates) |
| `servlet_inventory.md` | `docs/analysis/` | Full servlet mapping |
| `session_history_archive.md` | `docs/analysis/` | Consolidated record of all build sessions |
| `activity_detail_transition_plan.md` | `docs/analysis/` | Activity Detail Track A/B status and execution order |
