# AMS Project Backlog

**Created:** February 19, 2026
**Last Updated:** March 5, 2026
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
| 2 | Sales Portal | CONF | ✅ Done | `sales_pipeline_reference.md` | Full pipeline built and tested end-to-end. Proposal customization added (V035-V036). Raw HTML paste support for TITLE/CLOSING sections (Session 37). Application Visibility & Role Walls (V041, Session 41). |
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
| T6 | Database migration tracking | HIGH | ✅ Done | 41 versions tracked (V001–V041). V001-V024 on all environments, V025-V037 on Demo/BPO/Master, V038 on Demo/BPO. V039-V041 code-complete. Production at V024. |
| T7 | Docs cleanup & consolidation | MED | ✅ Done | This audit. Session summaries consolidated. Obsolete docs flagged for deletion. |
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
| 23 | Questionnaire Completion Gating | LOW | 💡 Backlog | Phase 7: optional `todo_id` linkage on QuestionnaireInstance — blocks task completion until questionnaire is SUBMITTED/REVIEWED. |
| 24 | Shared Field Renderer Component | LOW | 💡 Backlog | Phase 8: extract reusable `fieldRenderer.jsp` from questionnaire and application form rendering. |

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
| Demo PSP | https://demo.superiorstate.biz | 192.152.28.73 | PSP | V037 | Running, seeded with demo data, release V0.37.0 |
| BPO | https://bpo.superiorstate.biz | 158.222.102.168 (DHCP) | BPO | V037 | Running, initialized, partnered with Demo PSP, release V0.37.0 |
| Master | master.superiorstate.biz | 208.94.39.77 | Master image | V037 | Snapshot v8 taken, stopped |

**Migrations pending application:**
- V025–V037: Applied to Demo/BPO/Master. Not applied to production (V024) or local dev.
- Production intentionally isolated at V024 until conference demo infrastructure is proven
