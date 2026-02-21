# Session History Archive

> **Purpose:** Consolidated historical record of all build sessions. For current project state, see `project_backlog.md`. For current architecture, see `application_flow.md` and `entity_reference.md`.
>
> **Last Updated:** February 21, 2026

---

## February 15–17, 2026 — Code Cleanup (5 sessions)

**Source:** `cleanup_sweep_summary.md`

Eliminated the entire `previous/` package tree and cleaned up the codebase:

- **238 files deleted** across 5 sessions (14 + 19 + 107 + 92 + 6)
- **32 cryptic data classes renamed** (e.g., `dM.java` → `EntityLookup.java`, `V.java` → `Validator.java`)
- Packages eliminated: `previous/` (all subpackages), `ams/service/`, `ams/util/`
- Static methods extracted and consolidated (e.g., into `PersonResolver`, `ChecklistDAO`, `ActivityViewHelper`)
- Full rename mapping and deletion log preserved in `cleanup_sweep_summary.md`

---

## February 18, 2026 — Email System Standardization

**Source:** `email_workflow_analysis.md`

Modernized the email system:

- Removed Microsoft Graph API from `SendEmail25`, replaced with SMTP via `EmailDAO`
- Built `StorageDAO` for Wasabi S3 file upload, pre-signed URL generation, and delete
- Built `EmailTemplate` for branded HTML email wrapper (PSP-specific colors from DB)
- `AddAttachment25` now uploads to Wasabi instead of local disk
- `ShowFileUpload` now redirects to pre-signed Wasabi URL
- Added multipart/alternative (HTML + plain text) for spam reduction

---

## February 19, 2026 — Sequence Builder Overhaul

**Source:** `sequence_overhaul_summary.md`

Replaced the old sequence builder with a modern UI:

- Created `SequenceBuilder25` servlet (consolidates `GoTicketTemplate25` + `SequenceHome`)
- Created `SequenceAction25` servlet (SAVE/CREATE/DELETE with JSON task payload)
- Created `sequenceManager25.jsp` (two-panel: filterable sequence list + drag-and-drop task builder)
- Session attributes prefixed with `sb` to avoid collision with old pages
- Task counts loaded via COUNT queries to avoid lazy-load issues
- Old pages preserved for cleanup (backlog T5): `GoTicketTemplate25`, `TaskBuilder25`, `sequenceBuilderForm.jsp`, `checklistBuilder.jsp`

---

## February 19, 2026 — AI Chatbot (Phase 1 & 2)

**Source:** `chatbot_session_summary.md`, `ai_chatbot_feature_spec.md`

Built the AI Knowledge Assistant chatbot:

- `ChatAssistant` servlet — AJAX endpoint orchestrating search + Claude API call
- `KnowledgeSearchService` — loads JSON knowledge bases at startup, keyword routing, weighted chunk ranking
- `ClaudeApiService` — calls Anthropic Messages API (Haiku 4.5)
- `TicketKnowledgeDAO` — live query for completed tickets with resolution notes
- 5 JSON knowledge bases: Summit Guide (502 chunks), Summit Videos (30), Wave Help (449), Business Continuity, Backup/Recovery
- Role-based access: standard users see Summit KBs only, admins see all 5
- Added `isResolution` boolean to Note entity for resolution flagging
- **Pending production deploy** — see chatbot deployment checklist in `project_backlog.md`

Key decisions: live ticket KB over static export, legacy cutoff date (2026-02-19), resolution flag on Note, Gson for JSON parsing, KnowledgeSearchService in application scope.

---

## February 19–20, 2026 — Sales Pipeline (Sessions 1–4)

**Source:** `sales_pipeline_reference.md`

Built the complete sales pipeline over 4 sessions:

**Session 1:** Proposal/Application entity redesign (DataKey/DataPair → ApplicationField/ApplicationFieldValue), Feature/RateDiscount/MarketingMaterial entities, ProposalBuilder + ProposalDetail servlets, `sales_pipeline_migration.sql`

**Session 2:** SendProposal (email with GUID), ViewProposal (public landing page), Proposal.sourceActivity FK, `sales_pipeline_migration_2.sql`

**Session 3:** LOS expansion (IDs 11–19), ApplicationSection entity + 20 seeded sections with LOS scoping, IrsLimit/BenefitType/BillingType entities, ApplyForProposal (dynamic form), SaveApplicationProgress (AJAX auto-save), UploadRateSheet (Wasabi), `sales_pipeline_migration_3.sql`

**Session 4:** ReviewApplications + ReviewApplication servlets (list + detail with approve/deny/more-info), automated Setup + CheckList + ToDo creation on approval, manualSetup.jsp for GenerateProp25, full end-to-end pipeline test passed.

Key decisions: Application uses Proposal as PK (not generated Long), `LEFT JOIN FETCH p.application` required to avoid lazy-load bugs, EclipseLink DISTINCT + JOIN FETCH scrambles @OrderBy (workaround: Java sort after query).

---

## February 20, 2026 — Service Manager

**Source:** `service_manager_session_summary.md`

Built the Service Manager for configuring Lines of Service, Enhancements, and Application Sections:

- Created `Enhancement` entity with M:N to LOS
- Added `sortOrder` and `suppressed` columns to LOS
- Added nullable `los` and `enhancement` direct FKs to ServiceModule
- Created `ServiceManagerHome` (GET) + `ServiceManagerAction` (POST, 14 actions) + `ServiceManagerSort` (AJAX reorder)
- Created `serviceManager25.jsp` — tabbed Services/Enhancements with detail panel showing associations + application sections
- `service_manager_production_migration.sql` created

Known issue at end of session: Suppress buttons not rendering in edit modals (possibly cached JSP).

---

## February 20, 2026 — Rate Manager + Agency Manager

**Source:** `servlet_inventory_update.md`

Built admin UIs for rate and agency management:

- `PspAdminHome` — Rate Manager home (rates, fee types, modules, agencies, locked rate detection)
- `RateTableAction` — Rate CRUD (createRate, editRate, addRateTableRow, deleteRow, assignAgency, removeAgency, createPriceItem, cloneRate)
- `PspAgencyHome` — Agency Manager home (agencies, agents, rates for selected agency)
- `AgencyAction` — Agency CRUD (createAgency, editAgency, addAgent, removeAgent, removeRate)
- `PriceItemAction` / `ServiceModuleAction` — AJAX reorder and suppress toggle
- JSPs: `rateManager25.jsp`, `agencyManager25.jsp`

---

## February 21, 2026 — Resource Library + Feature Rendering

**Source:** `session_summary_feb21.md`

Built the Resource Library and connected features to proposals:

- Created `ResourceCategory` entity for organizing library resources
- Added `category` FK to `MarketingMaterial`, widened `storageGuid` to VARCHAR(50)
- Created `LibraryHome` (GET) + `LibraryAction` (POST, multipart-enabled CRUD with Wasabi upload)
- Created `library25.jsp` — category filter pills, scrollable resource list, detail panel
- Added `libraryResource` FK to Feature entity
- Feature CRUD in ServiceManagerAction (createFeature, editFeature, deleteFeature) + drag-sort
- Feature description supports inline `[text](resourceId)` markdown-style links
- ViewProposal renders features with inline links + end-icons by file type
- Rate suppress-when-locked fix in RateTableAction
- `resource_library_production_migration.sql` created

---

## February 21, 2026 — Invitation System

**Source:** `invitation_system_summary.md`

Built the complete invitation workflow from PSP to agent registration:

- Agency Manager enhancements: rate pricing popover, rate assignment state tracking, expanded edit modal
- Created `Invitation` entity (guid, email, role, 30-day expiry)
- Created `SendInvitation` servlet — creates Agency (if new) + Person + Invitation, sends email, pre-assigns rates
- Created `AcceptInvite` servlet + `acceptInvite.jsp` — validates GUID, registration form (Agency Manager: name/taxId/address/password, Agent: name/password)
- Existing user handling: auto-grant role if no conflicts, block if agent in different agency
- Added `manager_id` FK to Agency
- `invitation_system_migration.sql` created

---

## February 21, 2026 — Opportunity System + Agent Landing Page

**Source:** `opportunity_build_log.md`, `opportunity_spec_update.md`

Built the Opportunity system and Agent Landing Page:

- Created `Opportunity` entity extending Activity (DTYPE='Opportunity', tied to Prospect + Agency)
- Created `CreateOpportunity` servlet — creates Opportunity + CheckList + optional new Prospect
- Created `AgentHome` servlet + `agentHome25.jsp` — pipeline view with stage grouping, detail panel, new opp modal, quick stats
- Created `UpdateOpportunityStage` — AJAX stage dropdown
- Created `detailOpportunity25.jsp` — Opportunity detail in ViewActivity25
- Added role-based login routing: agents → AgentHome, PSP → ViewHome25
- ProposalBuilder scoped for agents (own prospects, agency rates, auto-select single rate)
- Navigation made role-aware (back buttons, navbar links)
- Stages: NEW → CONTACTED → QUALIFIED → PROPOSAL_SENT → NEGOTIATION → WON/LOST/ON_HOLD
- Sales task seed data: TemplateGroup 5, TemplatePurpose 30, Tasks 900001–900005
- `opportunity_migration_production.sql` created

Remaining items (tracked in backlog): PspAgencyHome scoping (T10), CheckList.java backref (LOW), StdAuto.java Opportunity case (LOW), layout/appearance consolidation (T11).
