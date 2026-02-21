# Session History Archive

> **Purpose:** Consolidated historical record of all build sessions. For current project state, see `project_backlog.md`. For current architecture, see `application_flow.md` and `entity_reference.md`.
>
> **Last Updated:** February 21, 2026

---

## February 15–17, 2026 — Code Cleanup (5 sessions)

**Replaces:** `cleanup_sweep_summary.md` (deleted in earlier cleanup)

Eliminated the entire `previous/` package tree and cleaned up the codebase:

- **238 files deleted** across 5 sessions (14 + 19 + 107 + 92 + 6)
- **32 cryptic data classes renamed** (e.g., `dM.java` → `EntityLookup.java`, `V.java` → `Validator.java`)
- Packages eliminated: `previous/` (all subpackages), `ams/service/`, `ams/util/`
- Static methods extracted and consolidated (e.g., into `PersonResolver`, `ChecklistDAO`, `ActivityViewHelper`)

---

## February 17, 2026 — Performance Session

**Replaces:** `perf_session_summary.md`

Optimized login speed and checklist rendering:

- **EMF Reuse:** `AmsDataLocal` constructor now accepts shared `EntityManagerFactory` from servlet context instead of creating one per session. Applied to `AuthenticateUser` and `LogOut`.
- **Dead code removed:** `LegacyQueryRunner.java` and `QueryPair.java` deleted (zero usages, each created/destroyed an EMF per query).
- **Pre-computed Todo Display State:** Added 9 pre-computed display fields to `ToDoOut25` with `computeDisplayState()` and `computeAllDisplayStates()` methods. Checklist JSP simplified from ~100 lines of JSTL per row to simple property reads. `blockFuture` cascades sequentially top-to-bottom. Admin override clears pointer events only (icons still show blocked state).

---

## February 18, 2026 — Email System Standardization

**Living reference:** `email_workflow_analysis.md` (kept separately)

Modernized the email system:

- Removed Microsoft Graph API from `SendEmail25`, replaced with SMTP via `EmailDAO`
- Built `StorageDAO` for Wasabi S3 file upload, pre-signed URL generation, and delete
- Built `EmailTemplate` for branded HTML email wrapper (PSP-specific colors from DB)
- `AddAttachment25` now uploads to Wasabi instead of local disk
- `ShowFileUpload` now redirects to pre-signed Wasabi URL
- Added multipart/alternative (HTML + plain text) for spam reduction

---

## February 19, 2026 — Sequence Builder Overhaul

**Replaces:** `sequence_overhaul_summary.md` (deleted in earlier cleanup)

Replaced the old sequence builder with a modern UI:

- Created `SequenceBuilder25` servlet (consolidates `GoTicketTemplate25` + `SequenceHome`)
- Created `SequenceAction25` servlet (SAVE/CREATE/DELETE with JSON task payload)
- Created `sequenceManager25.jsp` (two-panel: filterable sequence list + drag-and-drop task builder)
- Session attributes prefixed with `sb` to avoid collision with old pages
- Old pages preserved for cleanup (backlog T5): `GoTicketTemplate25`, `TaskBuilder25`, `sequenceBuilderForm.jsp`, `checklistBuilder.jsp`

---

## February 19, 2026 — AI Chatbot (Phase 1 & 2)

**Replaces:** `chatbot_session_summary.md` (deleted in earlier cleanup)

Built the AI Knowledge Assistant chatbot:

- `ChatAssistant` servlet — AJAX endpoint orchestrating search + Claude API call
- `KnowledgeSearchService` — loads JSON KBs at startup, keyword routing, weighted chunk ranking
- `ClaudeApiService` — calls Anthropic Messages API (Haiku 4.5)
- `TicketKnowledgeDAO` — live query for completed tickets with resolution notes
- 5 JSON knowledge bases: Summit Guide (502 chunks), Summit Videos (30), Wave Help (449), Business Continuity, Backup/Recovery
- Role-based access: standard users see Summit KBs only, admins see all 5
- Added `isResolution` boolean to Note entity
- **Pending production deploy** — see chatbot deployment checklist in `project_backlog.md`

Key decisions: live ticket KB over static export, legacy cutoff date (2026-02-19), resolution flag on Note, Gson for JSON, KnowledgeSearchService in application scope.

---

## February 19–20, 2026 — Sales Pipeline (Sessions 1–4)

**Detailed reference:** `sales_pipeline_reference.md` (kept separately)

Built the complete sales pipeline over 4 sessions:

**Session 1:** Proposal/Application entity redesign (DataKey/DataPair → ApplicationField/ApplicationFieldValue), Feature/RateDiscount/MarketingMaterial entities, ProposalBuilder + ProposalDetail servlets, `sales_pipeline_migration.sql`

**Session 2:** SendProposal (email with GUID), ViewProposal (public landing page), Proposal.sourceActivity FK, `sales_pipeline_migration_2.sql`

**Session 3:** LOS expansion (IDs 11–19), ApplicationSection entity + 20 seeded sections with LOS scoping, IrsLimit/BenefitType/BillingType entities, ApplyForProposal (dynamic form), SaveApplicationProgress (AJAX auto-save), UploadRateSheet (Wasabi), `sales_pipeline_migration_3.sql`

**Session 4:** ReviewApplications + ReviewApplication servlets (list + detail with approve/deny/more-info), automated Setup + CheckList + ToDo creation on approval, manualSetup.jsp for GenerateProp25, full end-to-end pipeline test passed.

Key decisions: Application uses Proposal as PK (not generated Long), `LEFT JOIN FETCH p.application` required to avoid lazy-load bugs, EclipseLink DISTINCT + JOIN FETCH scrambles @OrderBy (workaround: Java sort after query).

---

## February 20, 2026 — Service Manager (Session 1)

**Replaces:** `service_manager_session_summary.md` (deleted in earlier cleanup)

Built the Service Manager for configuring Lines of Service, Enhancements, and Application Sections:

- Created `Enhancement` entity with M:N to LOS
- Added `sortOrder` and `suppressed` columns to LOS
- Added nullable `los` and `enhancement` direct FKs to ServiceModule
- Created `ServiceManagerHome` (GET) + `ServiceManagerAction` (POST, 14 actions) + `ServiceManagerSort` (AJAX reorder)
- Created `serviceManager25.jsp` — tabbed Services/Enhancements with detail panel
- `service_manager_production_migration.sql` created

---

## February 20, 2026 — Rate Manager (Session 1)

**Replaces:** `rate_manager_build_log.md`

Built the Rate Manager UI for rate configuration:

- Created `PspAdminHome` servlet — Rate Manager home (rates, fee types, modules, agencies, locked rate detection)
- Created `RateTableAction` servlet — Rate CRUD (createRate, editRate, addRateTableRow, deleteRow, assignAgency, removeAgency, createPriceItem, cloneRate)
- Created `PriceItemAction` / `ServiceModuleAction` — AJAX reorder and suppress toggle
- Created `rateManager25.jsp` — 4-tab left panel (Rates, Fee Types, Modules, Agencies), right panel pricing grid
- Rate locking: rates with active proposals cannot have pricing modified (only cloned)

---

## February 20, 2026 — Agency Manager + Rate Manager Session 2

**Replaces:** `rate_manager_session2_log.md`

Built Agency Manager and enhanced Rate Manager:

- Created `PspAgencyHome` servlet + `agencyManager25.jsp` — Agency list, agent management, rate assignment
- Created `AgencyAction` servlet — Agency CRUD (createAgency, editAgency, addAgent, removeAgent, removeRate)
- **Rate Manager enhancements:** Add-pricing-row reworked to use LOS/Enhancement selection (hides ServiceModule abstraction), per-rate sort ordering (`ratetable.sort_order`), inline AJAX price editing, rate copying ("Make New From"), Grid Sort tab for per-rate drag reorder
- **Proposal rendering updates:** `SalesDAO.getPricing()` rewritten as two-query approach (LOS-linked + Enhancement-linked, merged), ViewProposal renders Enhancement feature cards, pricing headers show LOS/Enhancement names
- `rate_manager_session2_production_migration.sql` created

Key decisions: LOS/Enhancement selection in add-row modal hides ServiceModule abstraction from user. Per-rate sort order column on `ratetable` (not just ServiceModule sort). Two-query approach for getPricing() because single JPQL with OR/subquery failed in EclipseLink. `cloneRate` vs `copyRate`: clone moves agencies + suppresses original (for locked rates), copy just duplicates pricing (for convenience).

---

## February 20, 2026 — Service Manager Session 2

**Replaces:** `service_manager_session2_summary.md`

Enhanced Service Manager with suppress fix and SortableJS improvements:

- Fixed suppress buttons not rendering (cached JSP issue resolved)
- Added SortableJS drag-and-drop reordering for LOS list, Enhancement list, and Application Sections
- `ServiceManagerSort` servlet handles all three sort types via AJAX

---

## February 21, 2026 — Resource Library + Feature Rendering

**Replaces:** `session_summary_feb21.md` (deleted in earlier cleanup), `serviceManager25_feature_additions.md`

Built the Resource Library and connected features to proposals:

- Created `ResourceCategory` entity for organizing library resources
- Added `category` FK to `MarketingMaterial`, widened `storageGuid` to VARCHAR(50)
- Created `LibraryHome` (GET) + `LibraryAction` (POST, multipart CRUD with Wasabi upload)
- Created `library25.jsp` — category filter pills, scrollable resource list, detail panel
- Added `libraryResource` FK to Feature entity
- Feature CRUD in ServiceManagerAction (createFeature, editFeature, deleteFeature) + drag-sort
- Feature description supports inline `[text](resourceId)` markdown-style links
- ViewProposal renders features with inline links + end-icons by file type
- Rate suppress-when-locked fix in RateTableAction
- `resource_library_production_migration.sql` created

---

## February 21, 2026 — Invitation System

**Replaces:** `invitation_system_summary.md` (deleted in earlier cleanup)

Built the complete invitation workflow from PSP to agent registration:

- Agency Manager enhancements: rate pricing popover, rate assignment state tracking, expanded edit modal
- Created `Invitation` entity (guid, email, role, 30-day expiry)
- Created `SendInvitation` servlet — creates Agency (if new) + Person + Invitation, sends email, pre-assigns rates
- Created `AcceptInvite` servlet + `acceptInvite.jsp` — validates GUID, registration form
- Existing user handling: auto-grant role if no conflicts, block if agent in different agency
- Added `manager_id` FK to Agency
- `invitation_system_migration.sql` created

---

## February 21, 2026 — Opportunity System + Agent Landing Page

**Replaces:** `opportunity_agent_landing_spec.md` (deleted in earlier cleanup), `opportunity_build_log.md` (deleted), `opportunity_spec_update.md` (deleted)

Built the Opportunity system and Agent Landing Page:

- Created `Opportunity` entity extending Activity (DTYPE='Opportunity', tied to Prospect + Agency)
- Created `CreateOpportunity` servlet — creates Opportunity + CheckList + optional new Prospect
- Created `AgentHome` servlet + `agentHome25.jsp` — pipeline view with stage grouping, detail panel, new opp modal
- Created `UpdateOpportunityStage` — AJAX stage dropdown
- Created `detailOpportunity25.jsp` — Opportunity detail in ViewActivity25
- Role-based login routing: agents → AgentHome, PSP → ViewHome25
- ProposalBuilder scoped for agents (own prospects, agency rates, auto-select single rate)
- Navigation made role-aware (back buttons, navbar links)
- Stages: NEW → CONTACTED → QUALIFIED → PROPOSAL_SENT → NEGOTIATION → WON/LOST/ON_HOLD
- Sales task seed data: TemplateGroup 5, TemplatePurpose 30, Tasks 900001–900005
- `opportunity_migration_production.sql` created

Remaining items (tracked in backlog): PspAgencyHome scoping (T10), CheckList.java backref (LOW), StdAuto.java Opportunity case (LOW), layout/appearance consolidation (T11).
