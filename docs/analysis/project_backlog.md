# AMS Project Backlog

**Created:** February 19, 2026
**Last Updated:** February 27, 2026
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
| 1 | Users and Roles Enhancement | CONF | ✅ Done | `ams_to_be_vision.md` §1 | Agent + Agency Manager roles built. Invitation system complete. BPO roles built. SSO future (D-31). |
| 2 | **Sales Portal** | **CONF** | **✅ Done** | **`sales_pipeline_reference.md`** | **Full pipeline built and tested end-to-end. See status detail below.** |
| 3 | Sales / Marketing Library | CONF | ✅ Done | `session_history_archive.md` (Feb 21) | Resource Library UI, Wasabi upload/download, category management, feature linking to proposals. |
| 4 | Sequence Template Overhaul | CONF | ✅ Done | `session_history_archive.md` (Feb 19) | New drag-and-drop builder complete. Old pages preserved for cleanup. |
| 5 | Third-Party Vendor Task Outsourcing (BPO) | CONF | ✅ Done | `bpo_feature_session_history.md` | BPO delegation, dashboard, task completion, notes. Roles 101-103. API sync layer TBD for cross-site. |
| 6 | Email System Standardization | CONF | ✅ Done | `email_workflow_analysis.md` | Wasabi attachments, branded HTML templates, SMTP sending. Graph API removed. |
| 7 | Datapath API Readiness | CONF | 💡 Backlog | `ams_to_be_vision.md` §7 | Design principle — abstract data sources from business logic. |
| 8 | Deployment Readiness | CONF | ✅ Done | `deployment_strategy.md`, `deployment_runbook.md` | Multi-PSP infrastructure built. Master VPS image ready. Backup/update/health scripts deployed. |
| 9 | Mobile-Friendly Design | CONF | 💡 Backlog | `ams_to_be_vision.md` §9 | Sales portal mobile-first. Responsive improvements elsewhere. Track B. |

---

## Business / Operations Tier

| # | Feature | Priority | Status | Spec Doc | Notes |
|---|---------|----------|--------|----------|-------|
| 10 | Monthly Billing Automation | HIGH | 🔨 Active | `ams_to_be_vision.md` §10 | Core billing flow exists. Enhancements ongoing. |
| 11 | Time Tracking | MED | ✅ Done | `session_history_archive.md` (Feb 22) | UI redesigned (daily/weekly views). Correction request workflow built. Payroll export TBD. |
| 12 | Employee Onboarding Portal | MED | 💡 Backlog | `ams_to_be_vision.md` §12 | Self-service portal for new hires. |
| 13 | Invoicing System | MED | 💡 Backlog | `ams_to_be_vision.md` §13 | Replace Wave invoicing. Auto-generate from rate data. |
| 14 | Payroll-to-Accounting Automation | LOW | 💡 Backlog | `ams_to_be_vision.md` §14 | Patriot → Wave journal entries via API. |
| 15 | **AI Employee Knowledge Assistant** | **MED** | **✅ Done** | **`session_history_archive.md` (Feb 19)** | **Built. Pending production deploy (needs V014 migration — see below).** |

---

## Technical Debt / Infrastructure

| # | Item | Priority | Status | Spec Doc | Notes |
|---|------|----------|--------|----------|-------|
| T1 | Code cleanup — dead code removal | HIGH | ✅ Done | `session_history_archive.md` (Feb 15–17) | 238 files deleted, 32 renamed. `previous/` package eliminated. |
| T2 | Package reorganization | HIGH | ✅ Done | `session_history_archive.md` (Feb 15–17) | All code in clean packages. |
| T3 | Data layer rename | HIGH | ✅ Done | `session_history_archive.md` (Feb 15–17) | All cryptic names replaced. |
| T4 | Modal servlet analysis | MED | ✅ Done | — | All modals mapped to servlets. All in modern `controller` package. |
| T5 | Sequence builder old page cleanup | LOW | 📋 Planned | `session_history_archive.md` (Feb 19) | Delete old builder JSPs/servlets after new builder proven. |
| T6 | Database migration tracking | HIGH | ✅ Done | `migration_tracker.md` | 19 versions tracked (V001–V019). All applied to production. |
| T7 | Docs cleanup & consolidation | MED | 🔨 Active | — | Session summaries consolidated. Migration docs corrected Feb 25. Ongoing. |
| T8 | Empty checklist / todo list handling | LOW | 💡 Backlog | `ams_to_be_vision.md` §17 | Remove task-153 dummy workaround. Audit display chain for empty todo list safety. |
| T9 | Refactor manual setup to dynamic LOS | CONF | 📋 Planned | — | `GenerateProp25` uses hardcoded `q1`–`q8` flags mapped to old LOS IDs (5–10) and hardcoded TP IDs (11–19). Needs refactor to dynamic LOS from DB. Stopgap form (`manualSetup.jsp`) works for original 8 modules. |
| T10 | PspAgencyHome scoping | HIGH | 📋 Planned | `session_history_archive.md` (Feb 21) | Agency Manager should only see their own agency in PspAgencyHome. Hide rate management for non-PSP users. |
| T11 | Layout/appearance consolidation | MED | ✅ Done | `session_history_archive.md` (Feb 21) | Unified navbar, CSS, admin pages. Create Ticket modal rebuilt. Email screen modernized. |
| T12 | GUI modernization Track A | HIGH | ✅ Done | `activity_detail_transition_plan.md` | Activity Detail page fully modernized (A1–A14, S4). Email view/history modernized. |
| T13 | GUI modernization Track B | MED | 📋 Planned | `activity_detail_transition_plan.md` | Mobile responsiveness, remaining pages. |
| T14 | PSP branding system | MED | ✅ Done | `session_history_2026-02-25.md` | Logo/favicon upload, external storage, dynamic JSP references. D-32 (BRANDING_PATH) still needed on production. |
| T15 | User filter presets | MED | ✅ Done | `session_summary_2026-02-25_s2.md` | 3 configurable filter slots per user. V013 migration. |
| T16 | Client-side activity list filtering | MED | 💡 Backlog | — | Convert ViewHome25 center column from server-side form submit filtering to client-side JS filtering (same pattern as PspDashboard). Hydrate all activities into JS array on page load, filter/sort instantly in browser. Eliminates server round-trip on every filter change. |

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
| Full Pipeline Test | ✅ |
| Production DB Migration | ✅ All environments at V019 |

---

## AI Chatbot — Deployment Checklist

Code is complete. V014 migration applied to all environments. Production deploy ready.

---

## PSP Dashboard — New (Feb 27, 2026)

Built and functional. See `session_summary_2026-02-27.md` for full details.
- Servlet: `PspDashboardHome.java`
- JSP: `pspDashboard25.jsp`
- Client-side JS filtering (instant, no server round-trips)
- Placeholder cards for Agent Pipeline, BPO Vendors, Prospect Overview
- Accessible via Admin dropdown in navbar
