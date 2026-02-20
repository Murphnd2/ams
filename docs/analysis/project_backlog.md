# AMS Project Backlog

**Created:** February 19, 2026
**Last Updated:** February 20, 2026
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
| 1 | Users and Roles Enhancement | CONF | 🔨 Active | `ams_to_be_vision.md` §1 | User/Admin/Accountant roles. SSO future. Basic auth working. |
| 2 | **Sales Portal** | **CONF** | **🔨 Active** | **`sales_pipeline_reference.md`** | **Proposal flow complete through submit. Review/approve UI next. See reference doc for full status.** |
| 3 | Sales / Marketing Library | CONF | 💡 Backlog | `ams_to_be_vision.md` §3 | MarketingMaterial entity created. UI and Wasabi storage TBD. |
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
| 15 | **AI Employee Knowledge Assistant** | **MED** | **✅ Done** | **`chatbot_session_summary.md`**, **`ai_chatbot_feature_spec.md`** | **Built. Pending production deploy (API key + DB changes).** |

---

## Technical Debt / Infrastructure

| # | Item | Priority | Status | Spec Doc | Notes |
|---|------|----------|--------|----------|-------|
| T1 | Code cleanup — dead code removal | HIGH | ✅ Done | `cleanup_sweep_summary.md` | 238 files deleted, 32 renamed. `previous/` package eliminated. |
| T2 | Package reorganization | HIGH | ✅ Done | `cleanup_sweep_summary.md` | All code in clean packages. Final structure in sweep summary. |
| T3 | Data layer rename | HIGH | ✅ Done | `cleanup_sweep_summary.md` | All cryptic names replaced. Full rename table in sweep summary. |
| T4 | Modal servlet analysis | MED | ✅ Done | — | All modals mapped to servlets. All in modern `controller` package. |
| T5 | Sequence builder old page cleanup | LOW | 📋 Planned | `sequence_overhaul_summary.md` | Delete old builder JSPs/servlets after new builder proven. |
| T6 | Database migration tracking | HIGH | 🔨 Active | `migration_tracker.md` | 3 sales pipeline scripts + chatbot script pending production. |
| T7 | Docs cleanup | MED | 🔨 Active | — | Consolidating stale analysis docs. In progress this session. |

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
| Proposal Builder | ✅ |
| Proposal Detail (internal) | ✅ |
| Send Proposal (email) | ✅ |
| Proposal Landing Page (public) | ✅ |
| Application Form (dynamic sections, conditional logic, IRS limits) | ✅ |
| Save/Restore Progress | ✅ |
| Rate Sheet Upload (Wasabi) | ✅ |
| Submit Application | ✅ |
| Application Review/Approve UI | ❌ Next |
| Automated Setup Creation | ❌ |
| Production DB Migration | ❌ 3 scripts pending |

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
| `sales_pipeline_reference.md` | `docs/analysis/` | Sales pipeline entities, servlets, status, migration scripts |
| `migration_tracker.md` | `docs/analysis/` | Which DB scripts are applied to which environments |
| `cleanup_sweep_summary.md` | `docs/analysis/` | Historical record: 238 deletions, 32 renames, final package structure |
| `entity_reference.md` | `docs/analysis/` | Full entity map (core entities; sales entities now in pipeline reference) |
| `application_flow.md` | `docs/analysis/` | Entry points, navigation flows, filter chain, tech stack |
| `sequence_overhaul_summary.md` | `docs/analysis/` | Sequence builder redesign details |
| `chatbot_session_summary.md` | `docs/analysis/` | AI chatbot implementation details + deployment SQL |
| `ai_chatbot_feature_spec.md` | `docs/analysis/` | AI chatbot feature specification |
| `email_workflow_analysis.md` | `docs/analysis/` | Email system redesign (Wasabi, templates, SMTP) |
