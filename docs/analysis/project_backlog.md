# AMS Project Backlog

**Created:** February 19, 2026
**Last Updated:** April 23, 2026
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
| 1 | Users and Roles Enhancement | CONF | ✅ Done | `ams_to_be_vision.md` §1 | Agent + Agency Manager roles built. Invitation system complete. BPO roles built. User Manager modal (V029). SSO future (D-31). |
| 2 | Sales Portal | CONF | ✅ Done | `sales_pipeline_reference.md` | Full pipeline built and tested end-to-end. Proposal customization added (V035-V036). Raw HTML paste support for TITLE/CLOSING sections (Session 37). Application Visibility & Role Walls (V041, Session 41). AI Page Builder for custom proposal pages (Session 50). Agency-scoped TITLE/CLOSING overrides (V044, Session 51). Application service selections (V045), setup enhancement cascade, opportunity linked proposal creation (Session 52). |
| 3 | Sales / Marketing Library | CONF | ✅ Done | `session_history_archive.md` (Feb 21) | Resource Library UI, Wasabi upload/download, category management, feature linking to proposals. |
| 4 | Sequence Template Overhaul | CONF | ✅ Done | `session_history_archive.md` (Feb 19) | New drag-and-drop builder complete. Old pages preserved for cleanup. |
| 5 | Third-Party Vendor Task Outsourcing (BPO) | CONF | ✅ Done | `bpo_feature_session_history.md` | Full cross-system architecture: push/pull API, note sync, file attachments (V033), vendor registry (V032), partnership management. Co-located and federated modes both working. |
| 6 | Email System Standardization | CONF | ✅ Done | `email_workflow_analysis.md` | Wasabi attachments, branded HTML templates, SMTP sending. Graph API removed. |
| 7 | Datapath API Readiness | CONF | 💡 Backlog | `ams_to_be_vision.md` §7 | Design principle — abstract data sources from business logic. Summit import wizard (D-35) provides CSV path; API adapter is future work. |
| 8 | Deployment Readiness | CONF | ✅ Done | `deployment_strategy.md`, `deployment_runbook.md` | Multi-PSP infrastructure built. Master VPS v7 snapshot. Demo PSP + BPO live. Backup/update/health scripts deployed. |
| 9 | Mobile-Friendly Design | CONF | 💡 Backlog | `ams_to_be_vision.md` §9 | Sales portal mobile-first. Responsive improvements elsewhere. Track B item S5 remaining. |

---

## Business / Operations Tier

| # | Feature | Priority | Status | Spec Doc | Notes |
|---|---------|----------|--------|----------|-------|
| 10 | Monthly Billing Automation | HIGH | 🔨 Active | `ams_to_be_vision.md` §10 | Core billing flow restored (Session 9). GUID 404 fixed, send billing redesigned, CSV upload + monthly import rebuilt. |
| 11 | Time Tracking | MED | ✅ Done | `session_history_archive.md` (Feb 22) | UI redesigned (daily/weekly views). Correction request workflow built. Payroll export TBD. |
| 12 | Employee Onboarding Portal | MED | 💡 Backlog | `ams_to_be_vision.md` §12 | Self-service portal for new hires. |
| 13 | Invoicing System | MED | 💡 Backlog | `ams_to_be_vision.md` §13 | Replace Wave invoicing. Auto-generate from rate data. |
| 14 | Payroll-to-Accounting Automation | LOW | 💡 Backlog | `ams_to_be_vision.md` §14 | Patriot → Wave journal entries via API. |
| 15 | AI Employee Knowledge Assistant | MED | ✅ Done | `session_history_archive.md` (Feb 19) | Built and deployed. V014 migration applied to all environments. |

---

## Technical Debt / Infrastructure

| # | Item | Priority | Status | Notes |
|---|------|----------|--------|-------|
| T1 | Code cleanup — dead code removal | HIGH | ✅ Done | 238 files deleted, 32 renamed. `previous/` package eliminated. |
| T2 | Package reorganization | HIGH | ✅ Done | All code in clean packages. |
| T3 | Data layer rename | HIGH | ✅ Done | All cryptic names replaced. |
| T4 | Modal servlet analysis | MED | ✅ Done | All modals mapped to servlets. |
| T5 | Sequence builder old page cleanup | LOW | 📋 Planned | Delete old builder JSPs/servlets after new builder proven. |
| T6 | Database migration tracking | HIGH | ✅ Done | 53 versions tracked (V001–V053). V001-V024 on all environments, V025-V037 on Demo/BPO/Master, V038 on Demo/BPO. V039-V053 code-complete. Production at V024. |
| T7 | Docs cleanup & consolidation | MED | ✅ Done | Session 69 cleanup: removed 4 obsolete files, consolidated demo materials, compressed session archive (2397→1163 lines), replaced claude_memory.md with pointer file. |
| T8 | Empty checklist / todo list handling | LOW | 💡 Backlog | Remove task-153 dummy workaround. Audit display chain for empty todo list safety. |
| T9 | Refactor manual setup to dynamic LOS | CONF | 📋 Planned | `GenerateProp25` uses hardcoded `q1`–`q8` flags. Needs refactor to dynamic LOS from DB. |
| T10 | PspAgencyHome scoping | HIGH | 📋 Planned | Agency Manager should only see their own agency. Hide rate management for non-PSP users. |
| T11 | Layout/appearance consolidation | MED | ✅ Done | Unified navbar, CSS, admin pages. Tonal zones on PSP Home + Activity Detail (Session 40). |
| T12 | GUI modernization Track A | HIGH | ✅ Done | Activity Detail fully modernized (A1–A14, S4). Email view/history modernized. |
| T13 | GUI modernization Track B | MED | 📋 Planned | Mobile responsiveness (S5), remaining pages. |
| T14 | PSP branding system | MED | ✅ Done | Logo/favicon upload, external storage, dynamic JSP references. D-32 complete on production. |
| T15 | User filter presets | MED | ✅ Done | 3 configurable filter slots per user. V013. |
| T16 | Client-side activity list filtering | MED | 💡 Backlog | Convert ViewHome25 center column to client-side JS filtering (same pattern as PspDashboard). |
| T17 | ServiceItem Unification | HIGH | ✅ Done | TemplatePurpose → ServiceItem, TicketSubCategory eliminated. V020–V024. |
| T18 | Demo Data Seeder (D-24) | CONF | ✅ Done | `SeedDemoData` + `ReSeedDemoData` servlets. `DemoDataSeeder.java` refactored Session 36 — 11 bug fixes. |
| T19 | Summit Import Wizard (D-35) | HIGH | ✅ Done | Multi-step CSV import for Plan Types, Employers, Employees, Benefits. V025-V026. |
| T20 | BPO Cross-System Architecture | CONF | ✅ Done | Push/pull API, note sync, partnership management, vendor registry. V027-V033. |
| T21 | User Manager | HIGH | ✅ Done | Modal-based user management: deactivate/reactivate, role assignment. V029. |
| T22 | Benefit Renewal Audit | HIGH | ✅ Done | Audit page for plan year corrections, detected renewal dates. V028. |
| T23 | Starter Packages | MED | ✅ Code complete | 8 JSON package templates for ApplicationSections. Needs V034 applied + browser testing. D-53. |
| T24 | Proposal Customization | MED | ✅ Code complete | Composable section-based proposals, CKEditor 5, merge tokens. Needs V035-V036 applied + browser testing. D-54. |
| T25 | Full-Height Dashboard Layouts | MED | ✅ Done | Both PSP and BPO dashboards use flex layouts with internal scroll columns. |
| T26 | Questionnaire System | CONF | ✅ Code complete | V039 schema + entities, admin UI, auto-attach, activity detail card, native public form, manual attach, email shortcut, Jotform webhook. Phases 1–5 complete. Needs V039 applied + browser testing. D-55. |

---

## Post-Conference Tier

| # | Feature | Priority | Status | Notes |
|---|---------|----------|--------|-------|
| 16 | Social Media Marketing Automation | LOW | 💡 Backlog | AI-powered content for LSA, ICHRA, HSA growth. |
| 17 | BPO Questionnaire System | LOW | 💡 Backlog | Structured task responses using ApplicationSection/ApplicationField framework. |
| 18 | Microsoft 365 SSO | LOW | 💡 Backlog | D-31. Optional per-PSP, OpenID Connect. |
| 19 | Master Admin Dashboard | LOW | 💡 Backlog | D-14. Central management console for all PSP instances. |
| 20 | Summit Data Converter Web Tool | LOW | 💡 Backlog | Automate monthly CSV-to-Summit-import-format process. |
| 21 | Benefit Plan Document Generation | LOW | 💡 Backlog | 15 HTML templates across 5 lines of service. Standalone project, eventual AMS integration. |
| 22 | Automation Email Token for Questionnaires | MED | 💡 Backlog | Phase 6: `<q>` token in automation emails auto-embeds questionnaire link. Part of larger automation email design improvements. |
| 27 | AI Email Builder | MED | ✅ Done | Inline AI assistant for building automation email templates. Dedicated KB (20 chunks), multi-turn conversation, code canvas with insert/copy. Smart `<<#erName>>` resolution across all activity types with input fallback. Recipient-less activity safeguard (TO email prompt). Session 48. |
| 28 | Proposal AI Page Builder | MED | ✅ Done | Inline AI assistant for generating styled HTML proposal pages. Claude Sonnet, 24-chunk KB, dual Code/Preview canvas, Insert into Editor. Card-inset pattern with scoped CSS, merge tokens, scale factor. Session 50. |
| 23 | Questionnaire Completion Gating | LOW | 💡 Backlog | Phase 7: optional `todo_id` linkage on QuestionnaireInstance — blocks task completion until questionnaire is SUBMITTED/REVIEWED. |
| 24 | Shared Field Renderer Component | LOW | 💡 Backlog | Phase 8: extract reusable `fieldRenderer.jsp` from questionnaire and application form rendering. |
| 25 | Proposal Content Page Skill | MED | ✅ Done | Claude Code skill (`.claude/skills/proposal-content-page/`) generates styled HTML blocks for proposal custom pages. Dark navy card-inset pattern, `--s` scale factor, scoped CSS, print-ready layout. Reference examples in `docs/proposal-fsa-page*.html`. Useful for building content library and proposal inserts. |
| 29 | Agency-Scoped Proposal Sections | MED | ✅ Code complete | V044 adds agency_id FK to proposal_section. TITLE/CLOSING pages can be overridden per-agency. ProposalSettings Agency Overrides card. ViewProposal resolves agency chain. Full-height flex layout with collapsible editor. Needs V044 applied + browser testing. Session 51. |
| 26 | Legacy TPO Path → Opportunity Creation | MED | 💡 Backlog | When `/tpo/*` path is hit, auto-create an Opportunity for the agency manager. Extract the full original path sought (e.g. `/tpo/quote/12345`) so previous quote/prospect info can be looked up from the old superiorstate.net IIS site. Currently `/tpo/*` shows a static "site updated" notice page (`LegacyTpoRedirect`). |
| 35 | Summit Import Full Review | MED | 💡 Backlog | Comprehensive review of all Summit import jobs (J4 CDH, J7 COBRA, J5 plan years, J1 employers, J3 employees). J4 fixed to derive active status from termination date instead of unreliable PlanStatus column; also now syncs effectiveDate, terminationDate, hasCards on update. Apply same termination-date logic to J7 COBRA. Review all jobs for similar field-sync gaps and stale-data issues. |
| 30 | Universal Import System | HIGH | ✅ Code complete | Config-driven entity import with provider setup, field mapping, transform rules (V048). Cross-reference system (V051-V052). Provider setup rework (V053). Interactive Import Wizard B1-B5 complete. Sessions 56-66. |
| 31 | Chatbot Skill System | MED | ✅ Code complete | Extensible skill matching with ChatbotSkill entity, SkillManager admin UI, unified ChatAssistant endpoint (V046). Session 54. |
| 32 | Composite Task Ordering | MED | ✅ Code complete | Cross-sequence master ordering per activity type, drag-and-drop UI in sequenceManager25.jsp (V047). Session 55. |
| 33 | Custom Landing Page System | MED | ✅ Code complete | PSP-customizable landing page with login modal, Request a Quote form, configurable header colors (V043). Sessions 45-46. |
| 34 | Center Panel Redesign | MED | ✅ Done | Unified "Colored Tab" section headers across all activity detail panels. Navbar application review badge. Session 68. |
| 37 | White-Label Proposal → Application Flow | MED | ✅ Done | Session 88. No migration. Public proposal/application/confirmation pages suppress PSP band when selling agency present; agency name in header, charcoal neutral band, `© AgencyName` footer. Compose email pre-fill signature also uses sender's agency. `EmailTemplate.wrap()` (activity emails, quick-send) uses PSP name — separate future task if needed. |
| 36 | Agent Delegation on Setup ToDos | HIGH | 🔨 Active | V061 adds ToDo-level ownership override. PSP can delegate individual Setup ToDos to agents of the originating selling agency (resolved via Opportunity.assignedTo → Prospect.agent → Proposal.createdBy). User Assignment sub-row on `taskManager25.jsp`. Agent sees "Tasks Delegated to Me" on AgentHome with link to Setup detail (gated by `agentBlocked`). Originating agent surfaced in activity-detail header (Option C — presentational). Code-complete, not yet applied. Session 85. **Next: agent-facing Setup view (Session 86).** |

---

## Sales Portal — Current Status Detail

**Full reference:** `docs/analysis/sales_pipeline_reference.md`

All pipeline steps complete. Proposal customization (V035-V036) adds composable section-based proposals with CKEditor editing and merge tokens — code complete, pending migration application and browser testing (D-54).

---

## PSP Dashboard — Status

Built and functional. Client-side JS filtering. Placeholder cards for Agent Pipeline, BPO Vendors, Prospect Overview. Full-height flex layout with internal scroll columns (Session 28).

---

## Deployment Status

| Instance | URL | IP | Type | Schema | Status |
|----------|-----|----|------|--------|--------|
| Production PSP | https://superiorstate.biz | (production IP) | PSP | V024 | Running |
| Demo PSP | https://demo.superiorstate.biz | 192.152.28.73 | PSP | V038 | Running, seeded with demo data |
| BPO | https://bpo.superiorstate.biz | 158.222.102.168 (DHCP) | BPO | V038 | Running, initialized, partnered with Demo PSP |
| Master | master.superiorstate.biz | 208.94.39.77 | Master image | V037 | Snapshot v8 taken 2026-03-04, stopped |

**Migrations pending application:**
- V025–V037: Applied to Demo/BPO/Master.
- V038: Applied to Demo/BPO.
- V039–V053: Code-complete, not yet applied to any environment.
- Production intentionally isolated at V024 until conference demo infrastructure is proven.
