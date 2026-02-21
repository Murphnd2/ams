# AMS Project Backlog

**Created:** February 19, 2026
**Last Updated:** February 21, 2026
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
| 1 | Users and Roles Enhancement | CONF | 🔨 Active | `ams_to_be_vision.md` §1, `invitation_system_summary.md` | Agent + Agency Manager roles built. Invitation system complete. SSO future. |
| 2 | **Sales Portal** | **CONF** | **🔨 Active** | **`sales_pipeline_reference.md`**, **`opportunity_build_log.md`** | **Full pipeline built. Service Manager, Rate Manager, Agency Manager, Opportunity system, Agent Landing Page all complete. See status detail below.** |
| 3 | Sales / Marketing Library | CONF | ✅ Done | `session_summary_feb21.md` | Resource Library UI, Wasabi upload/download, category management, feature linking to proposals. |
| 4 | Sequence Template Overhaul | CONF | ✅ Done | `sequence_overhaul_summary.md` | New drag-and-drop builder complete. Old pages preserved for cleanup. |
| 5 | Third-Party Vendor Task Outsourcing | CONF | 💡 Backlog | `ams_to_be_vision.md` §5 | Federated model. Task entity has sourcing flags. API sync layer TBD. |
| 6 | Email System Standardization | CONF | ✅ Done | `email_workflow_analysis.md` | Wasabi attachments, branded HTML templates, SMTP sending. Graph API removed. |
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
| 15 | **AI Employee Knowledge Assistant** | **MED** | **✅ Done** | **`ai_chatbot_feature_spec.md`** | **Built. Pending production deploy (API key + DB changes).** |

---

## Technical Debt / Infrastructure

| # | Item | Priority | Status | Spec Doc | Notes |
|---|------|----------|--------|----------|-------|
| T1 | Code cleanup — dead code removal | HIGH | ✅ Done | `cleanup_sweep_summary.md` | 238 files deleted, 32 renamed. `previous/` package eliminated. |
| T2 | Package reorganization | HIGH | ✅ Done | `cleanup_sweep_summary.md` | All code in clean packages. Final structure in sweep summary. |
| T3 | Data layer rename | HIGH | ✅ Done | `cleanup_sweep_summary.md` | All cryptic names replaced. Full rename table in sweep summary. |
| T4 | Modal servlet analysis | MED | ✅ Done | — | All modals mapped to servlets. All in modern `controller` package. |
| T5 | Sequence builder old page cleanup | LOW | 📋 Planned | `sequence_overhaul_summary.md` | Delete old builder JSPs/servlets after new builder proven. |
| T6 | Database migration tracking | HIGH | 🔨 Active | `migration_tracker.md` | 7 scripts pending production. See migration tracker. |
| T7 | Docs cleanup & consolidation | MED | 🔨 Active | — | Updating stale docs, consolidating redundant session summaries. |
| T8 | Empty checklist / todo list handling | LOW | 💡 Backlog | `ams_to_be_vision.md` §17 | Remove task-153 dummy workaround. Audit display chain for empty todo list safety. See §17 for full file list. |
| T9 | Refactor manual setup to dynamic LOS | CONF | 📋 Planned | — | `GenerateProp25` uses hardcoded `q1`–`q8` flags mapped to old LOS IDs (5–10) and hardcoded TP IDs (11–19). Needs refactor: form loads LOS from DB, servlet accepts LOS ID list, `fillProposal`/`fillApplication` derive modules via `losmodules` → `ServiceModule` → `TemplatePurpose` chain. Covers all 15 LOSs (IDs 5–19). Stopgap form (`manualSetup.jsp`) works for original 8 modules. |
| T10 | PspAgencyHome scoping | HIGH | 📋 Planned | `opportunity_spec_update.md` | Agency Manager should only see their own agency in PspAgencyHome. Hide rate management for non-PSP users. |
| T11 | Layout/appearance consolidation | MED | 📋 Planned | `opportunity_build_log.md` | Unify toolbar/navigation across all main pages (ViewHome25, AgentHome, admin pages). |

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
| Production DB Migration | ❌ 7 scripts pending |

---

## AI Chatbot — Deployment Checklist

**Full spec:** `docs/analysis/ai_chatbot_feature_spec.md`

Code is complete. Production deployment requires:
- [ ] Run schema change: `ALTER TABLE note ADD COLUMN is_resolution TINYINT(1) NOT NULL DEFAULT 0;`
- [ ] Insert API key: `INSERT INTO constant (name, value, note) VALUES ('ANTHROPIC_API_KEY', '<key>', '...');`
- [ ] Update ticket categories (see `chatbot_session_summary.md` for full SQL)
- [ ] Deploy WAR with chatbot code
- [ ] Verify knowledge base JSON files are deployed to `src/main/resources/knowledge/`

---

## Reference Documents Index

| Document | Location | Purpose |
|----------|----------|---------|
| `ams_to_be_vision.md` | `docs/` | Full project vision and feature descriptions |
| `sales_pipeline_reference.md` | `docs/analysis/` | Sales pipeline entities, servlets, lifecycle states, migration scripts |
| `migration_tracker.md` | `docs/analysis/` | Which DB scripts are applied to which environments |
| `cleanup_sweep_summary.md` | `docs/analysis/` | Historical record: 238 deletions, 32 renames, final package structure |
| `entity_reference.md` | `docs/analysis/` | Full entity map (core + sales + opportunity entities) |
| `application_flow.md` | `docs/analysis/` | Entry points, navigation flows, filter chain, tech stack |
| `sequence_overhaul_summary.md` | `docs/analysis/` | Sequence builder redesign details |
| `ai_chatbot_feature_spec.md` | `docs/analysis/` | AI chatbot feature specification |
| `chatbot_session_summary.md` | `docs/analysis/` | AI chatbot implementation details + deployment SQL |
| `email_workflow_analysis.md` | `docs/analysis/` | Email system (Wasabi, SMTP, branded templates) |
| `service_manager_session_summary.md` | `docs/analysis/` | Service Manager build details |
| `session_summary_feb21.md` | `docs/analysis/` | Resource Library, Feature Management, Proposal Rendering |
| `invitation_system_summary.md` | `docs/analysis/` | Invitation system build details |
| `opportunity_build_log.md` | `docs/analysis/` | Opportunity system build details |
| `opportunity_spec_update.md` | `docs/analysis/` | Opportunity post-build corrections and remaining items |
| `servlet_inventory.md` | `docs/analysis/` | Full servlet mapping |
