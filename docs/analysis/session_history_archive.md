# Session History Archive

> **Purpose:** Consolidated historical record of all build sessions. For current project state, see `project_backlog.md`. For current architecture, see `application_flow.md` and `entity_reference.md`.
>
> **Last Updated:** March 3, 2026

---

## February 15–17, 2026 — Code Cleanup (5 sessions)

Eliminated the entire `previous/` package tree and cleaned up the codebase:

- **238 files deleted** across 5 sessions (14 + 19 + 107 + 92 + 6)
- **32 cryptic data classes renamed** (e.g., `dM.java` → `EntityLookup.java`, `V.java` → `Validator.java`)
- Packages eliminated: `previous/` (all subpackages), `ams/service/`, `ams/util/`
- Static methods extracted and consolidated (e.g., into `PersonResolver`, `ChecklistDAO`, `ActivityViewHelper`)

---

## February 17, 2026 — Performance Session

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

Replaced the old sequence builder with a modern UI:

- Created `SequenceBuilder25` servlet (consolidates `GoTicketTemplate25` + `SequenceHome`)
- Created `SequenceAction25` servlet (SAVE/CREATE/DELETE with JSON task payload)
- Created `sequenceManager25.jsp` (two-panel: filterable sequence list + drag-and-drop task builder)
- Session attributes prefixed with `sb` to avoid collision with old pages
- Old pages preserved for cleanup (backlog T5): `GoTicketTemplate25`, `TaskBuilder25`, `sequenceBuilderForm.jsp`, `checklistBuilder.jsp`

---

## February 19, 2026 — AI Chatbot (Phase 1 & 2)

Built the AI Knowledge Assistant chatbot:

- `ChatAssistant` servlet — AJAX endpoint orchestrating search + Claude API call
- `KnowledgeSearchService` — loads JSON KBs at startup, keyword routing, weighted chunk ranking
- `ClaudeApiService` — calls Anthropic Messages API (Haiku 4.5)
- `TicketKnowledgeDAO` — live query for completed tickets with resolution notes
- 5 JSON knowledge bases: Summit Guide (502 chunks), Summit Videos (30), Wave Help (449), Business Continuity, Backup/Recovery
- Role-based access: standard users see Summit KBs only, admins see all 5
- Added `isResolution` boolean to Note entity
- V014 migration applied to all environments

Key decisions: live ticket KB over static export, legacy cutoff date (2026-02-19), resolution flag on Note, Gson for JSON, KnowledgeSearchService in application scope.

---

## February 19–20, 2026 — Sales Pipeline (Sessions 1–4)

**Living reference:** `sales_pipeline_reference.md` (kept separately)

Built the complete sales pipeline over 4 sessions:

**Session 1:** Proposal/Application entity redesign (DataKey/DataPair → ApplicationField/ApplicationFieldValue), Feature/RateDiscount/MarketingMaterial entities, ProposalBuilder + ProposalDetail servlets, `V001__sales_pipeline.sql`

**Session 2:** SendProposal (email with GUID), ViewProposal (public landing page), Proposal.sourceActivity FK, `V002__sales_pipeline_2.sql`

**Session 3:** LOS expansion (IDs 11–19), ApplicationSection entity + 20 seeded sections with LOS scoping, IrsLimit/BenefitType/BillingType entities, ApplyForProposal (dynamic form), SaveApplicationProgress (AJAX auto-save), UploadRateSheet (Wasabi), `V003__sales_pipeline_3.sql`

**Session 4:** ReviewApplications + ReviewApplication servlets (list + detail with approve/deny/more-info), automated Setup + CheckList + ToDo creation on approval, manualSetup.jsp for GenerateProp25, full end-to-end pipeline test passed.

Key decisions: Application uses Proposal as PK (not generated Long), `LEFT JOIN FETCH p.application` required to avoid lazy-load bugs, EclipseLink DISTINCT + JOIN FETCH scrambles @OrderBy (workaround: Java sort after query).

---

## February 20–21, 2026 — Service Manager + Rate Manager + Agency Manager

Built the full admin toolset for managing sales offerings:

- **Service Manager:** LOS/Enhancement/ServiceModule CRUD with drag-and-drop ordering (SortableJS), feature management with inline markdown links, application section management with LOS scoping
- **Rate Manager:** Rate/FeeType/RateTable/PriceItem CRUD, rate-LOS matrix editing, discount management
- **Agency Manager:** Agency CRUD, agent assignment, rate assignment per agency, manager FK
- **Invitation System:** `SendInvitation` → `AcceptInvite` → auto-create Person + User + Agency link. GUID-based public registration page.
- Migrations: V004 (service manager), V005 (rate manager), V006 (invitation system)

---

## February 21, 2026 — Resource Library + Opportunity System + Layout Consolidation

- **Resource Library:** `LibraryHome` servlet, `library25.jsp` with category management, Wasabi file upload/download, feature linking to proposals. V007 migration.
- **Opportunity System:** `CreateOpportunity` servlet, stage management (NEW→CONTACTED→QUALIFIED→PROPOSAL_SENT→NEGOTIATION→ON_HOLD→WON/LOST), agent pipeline view in `AgentHome`. V008 migration.
- **Activity List Redesign:** `activityList25.jsp` rewritten — compact type badge pills (R/S/T/O), urgency icons, opportunity stage badge, due date color-coding, empty state message
- **PSP Opportunity Integration:** PSP Sales role (ID 9), `managed_by_id` FK on assignee, ActivityListDAO/ActivityFilter updates. V010 migration.
- **Layout consolidation:** Unified navbar, CSS, admin pages. Create Ticket modal rebuilt. Email screen modernized.

---

## February 22, 2026 — Time Tracking

UI redesign for the timeclock system:

- Daily and weekly views with modern card-based layout
- Correction request workflow (employee submits → admin approves/denies)
- `TimeCorrectionRequest` entity and `V009__timeclock_correction.sql` migration
- Payroll export TBD (future work)

---

## February 23, 2026 — Email Screen + Checklist Listing Modernization

Two-part session modernizing the last un-modernized navbar-triggered screens:

### Email Compose Screen (`emailMaster25.jsp`)
- Card + hdr-bar wrapper with SSA button classes
- Compact inline type-badge + name replacing full-width colored banner
- Chip-style recipients + attachments (`.recipient-chip` / `.attach-chip` with ✕ remove)
- Modals modernized (SSA headers, `btn-ssa` actions)
- jQuery removed — vanilla JS `DOMContentLoaded` + `bootstrap.Modal`
- CKEditor submit fix: captures `clickedAction`, injects hidden input before submit
- **Bug fix:** Attachment upload blank screen — `getLinkName()` doesn't exist on WebLink, changed to `getPlainText()`

### Checklist Listing Column (ViewHome25 ToDo column)
- **toDoCurrentList25.jsp** complete rewrite: card-based items with left-border urgency coloring (red=overdue, orange=due today/tomorrow, blue=delinquent multi-task), inline action icons (reassign, change date), kebab menus
- **Checklist header:** `.hdr-bar` pattern with filter/create icons
- **Future checklists** integrated into same column (collapsible section)
- **Creation modals** modernized (SSA styling)

---

## February 23, 2026 — Multi-PSP Deployment (Sessions 1 & 2)

**Living references:** `deployment_strategy.md`, `deployment_runbook.md`, `deployment_backlog.md` (kept separately)

Built the complete multi-PSP deployment infrastructure over two sessions:

- **D-01 through D-06:** `AppConfig` properties loader, fixed hardcoded paths (EmfListener, SAVE_PATH), removed test data from DatabaseInitializer, created `InitializeDataBase` servlet with deployment key validation and re-init prevention
- **D-08:** Verified reserved ID ranges, bumped SEQ_GEN from 200 to 1000
- **D-09/D-10:** Created `schema_version` table, established V-numbering convention
- **D-11:** Backup script (`backup.sh`) — mysqldump → gzip → Wasabi, 7-day retention, cron 2:00 AM
- **D-12:** Update script (`update.sh`) — GitHub Releases API → SQL migrations → WAR deploy → Tomcat restart, cron 2:30 AM
- **D-13:** Health check script (`healthcheck.sh`) — system status email digest, cron 6:00 AM, kill switch via `SYS_HEALTH_ENABLED` constant
- **D-15 through D-19:** Master VPS configuration (data dir, MySQL connector, default ROOT removal, awscli, port 8080)
- **D-21:** GitHub PAT (read-only, no expiration)
- **D-23:** First release published
- **D-27:** Tomcat SSL setup guide (`tomcat_ssl_setup.md`)
- **D-28:** Blank schema dump saved on master image
- Master VPS snapshot: `SSA-Master-Base-v4-2026-02-23`

---

## February 23–24, 2026 — Activity Detail GUI Modernization

Comprehensive GUI modernization of the Activity Detail page (`activityDetail25.jsp`), completing Track A items A1–A7, A11, A12:

- **Section headers:** Checklist and History headers → `.hdr-bar` pattern
- **Detail header:** SSA blue bar with type badge pill, driver subtitle, inline action icons (owner, due date, past activities). Icons gated with `pe-none` for closed activities.
- **Primary contact:** SSA card with left blue border, pencil edit
- **Type-specific panels:** All 4 types rewritten — Ticket (expandable card), Renewal (benefit list with add/remove), Setup (data-driven module iteration replacing 8 hardcoded `c:if` blocks), Opportunity (two-card: details + proposals)
- **Add Note:** Replaced CKEditor with Quill editor, collapsible with `.hdr-bar` header, inline Reason + Status dropdowns, resizable editor (localStorage height), tab-to-save
- **Footer decomposition:** Monolithic button row → Additional Contacts card, Documents & Links card, header action icons. Footer stripped to modal imports only.
- **History body:** SSA styling, fixed duplicate date bug, empty state
- **Modal standardization:** All modals → `modal-sm`, SSA blue headers
- **Resizable three-panel layout:** Drag dividers between panels, widths persisted to localStorage, CSS media queries for responsive stacking
- **Wasabi document upload:** New `AddDocumentToActivity25` servlet. Download links use `ShowFileUpload?doc=` with pre-signed Wasabi URLs.
- **CKEditor → Quill migration:** Quill via CDN, compressed toolbar, system font stack, `ResizeObserver` persists height

New files: `AddDocumentToActivity25.java`, `detailAdditionalContacts25.jsp`, `detailDocsLinks25.jsp`
No database changes.

---

## February 24, 2026 — BPO Feature Implementation

**Living reference:** `bpo_feature_session_history.md` (kept separately)

Built the complete BPO (Business Process Outsourcing) delegation feature across three sessions. BPO users log in, see delegated tasks, add notes, mark complete. V011 migration (todo BPO columns, todo_note table). V012 migration (role cleanup, PSP branding constants). See `bpo_feature_session_history.md` for full details.

---

## February 24–25, 2026 — Checklist Panel Layout + Task Manager Modernization

Completed checklist panel restructuring (A8–A10) and task manager page modernization:

### Checklist Panel (Track A completion)
- **A9 — Automation integration:** Removed separate `checklistAutomation25.jsp` bar. Lightning bolt icon on first open automated task. Modal-based automation preview/send workflow. Info icon inline next to task descriptions.
- **A10 — Panel restructuring:** Header gained "+" button for add task. Body (`checklistBasic25.jsp`) removed fixed max-height — open items scroll in flex-grow area, completed section pinned below. Footer simplified to Close button + modal imports. Panel CSS changed to flex column layout.
- **Add Task Modal:** SSA blue gradient header, stacked layout, "At the top" first / "At the bottom" default, only shows open tasks in "After:" dropdown

### Task Manager Page (`taskManager25.jsp`) — Complete Rewrite
- Two-column layout: left 1/3 task settings, right 2/3 email automation
- Full viewport height via flex layout, no page scrollbar
- Left column: four SSA-bordered sections (Ordering, Employee Assignment, Vendor Sourcing, Links)
- Right column: Quill-powered automation email editor with placeholder variable pills
- SSA design patterns throughout

### Track A Status After This Session
A1–A12 complete. Remaining: A13 (closed activity banner), A14 (auto-save UX), S4 (pe-none standardization), S5 (mobile polish).

---

## February 25, 2026 — Track A Polish + Navbar Restyle + Email Screens (Session 1)

Completed Track A polish items A13, A14, S4 and restyled the navbar and email viewing screens:

- **A13 — Closed Activity Banner:** Restyled from yellow warning to muted gray archived feel. Background `#f0f0f0`, border `#ccc`, text `#6c757d`, lighter lock icon. Added "by [FirstName]" when completedBy is available.
- **A14 — Auto-Save UX Indicator:** Added amber "Unsaved" dot next to Save button in Quill note editor. Appears on `text-change` when content exists, clears when editor empty or on form submit.
- **S4 — pe-none Audit:** All interactive elements already gated for closed activities. No changes needed.
- **Navbar Restyle:** Ghost buttons replacing dropdown items, bottom radius, unauthenticated transparent bar.
- **Email View Screens:** New `emailView25.jsp`, `emailHistoryList25.jsp`, `ViewEmailHistory.java` servlet. SSA card styling. Old JSPs no longer referenced.

Track A effectively complete except S5 (mobile stacking polish).

---

## February 25, 2026 — PSP Branding + Activity List Display (Session 2)

- **PSP Branding System:** `UploadPspBranding` servlet, `ServeBrandingFile` servlet (serves from external path), `pspBranding25.jsp`. Dimension validation (navbar: max 300×80px PNG, login: max 800×400px PNG, favicon: 32×32px ICO/PNG). `BRANDING_PATH` via `ssa.properties`. Dynamic JSP references in navbar, login, favicon.
- **User Filter Presets:** 3 configurable filter slots per user. V013 migration.
- **Activity List Display:** Bootstrap icons replacing letter badges. Compact due date display.
- **Login System Fixes:** `HelpUserLogin` rewrite (branded emails, 7-day/10-min GUIDs), `OneTimeUserLogin` fix, `CreateUser25` sales-only fix.
- **LoginFilter:** Three-tier logic (static → uninitialized → auth), removed console logging.

---

## February 25, 2026 — Migration Validation & Dev Workflow (Session 3)

Validated the full V001–V013 migration chain against a fresh production dump. Found and corrected 13 bugs across 7 scripts. Produced validated combined upgrade script and V013 dev baseline. Established new dev workflow: `git pull` → import baseline → run DatabaseInitializer → import Datapath exports.

---

## February 26, 2026 — Production Upgrade Validation & Entity-Schema Alignment (Session 1)

- **Production dump import/test:** Imported fresh production dump locally, ran upgrade script, tested backward/forward compatibility across branches
- **Immutable list bug fix:** `CloseActivity25.java` `.toList()` → `.collect(Collectors.toList())` (Java 16+ immutable list caused `UnsupportedOperationException`)
- **Entity-schema mismatch discovery:** Found 8 tables where JPA entities were refactored after migration scripts were written. Fixed all in the combined upgrade script.
- **Extended upgrade script** from V013 to V016 (V014: chatbot, V015: constants-to-properties, V016: BPO registration)
- **Key learnings:** PowerShell `>` corrupts SQL dumps (UTF-16 BOM), `SET sql_log_bin = 0` required for `DEFAULT (UUID())`, entity refactoring creates silent schema drift

---

## February 26, 2026 — Email Template + SMTP Settings + Health Config (Session 2)

- **SendAutoEmail recovery:** Created redirect wrapper servlet for legacy `task.servletName` DB records
- **SMTP Settings Modal:** `UpdateSmtpSettings` servlet + `smtpSettingsMod25.jsp` — PSP Admin can configure all SMTP settings and footer text
- **Email Template redesign:** Complete rewrite of `EmailTemplate.java` — system font stack, clean white card layout, attachments after signature, Capital Case name normalization, configurable footer text via `EMAIL_FOOTER_TEXT` constant
- **V017 migration:** Moved 7 `SYS_HEALTH_*` constants from DB to `ssa.properties`, seeded `EMAIL_FOOTER_TEXT`
- **healthcheck.sh update:** Reads config from `ssa.properties` via `prop()` helper

---

## February 26, 2026 — Application Section & Field Editor (Session 3)

Added full Application Section and Field management GUI to the Service Manager:

- **Third "Sections" tab** on `serviceManager25.jsp` alongside Services and Enhancements
- Left panel: scrollable section list with drag-sort (SortableJS), suppress toggle, field count badge
- Right panel: section info card, linked Services/Enhancements (read-only), interactive Fields table with drag-sort, edit, suppress per field
- **Section CRUD:** Create/edit modals (name, description, scope ALL/LOS), suppress toggle
- **Field CRUD:** Create modal (label, auto-generated fieldKey, fieldType dropdown with 9 types, selectOptions pipe-delimited, helpText, isRequired). Edit modal (label, selectOptions, helpText, isRequired — fieldKey/fieldType locked). Per-row suppress toggle.
- **V018 migration:** `applicationsection.suppressed` column
- **V019 migration:** `applicationfield.suppressed` column

---

## February 26, 2026 — ALL-Scope Auto-Linking + Admin GUI Polish (Session 4)

### Application Section "ALL" Scope Auto-Linking
Automatic join table population so ALL-scoped sections are always linked to every active LOS and Enhancement:

- `ServiceManagerAction.java` — 4 cases updated: `createAppSection` (auto-link on ALL), `editAppSection` (additive link on scope change to ALL), `createLos` (auto-link ALL sections), `createEnhancement` (same)
- `serviceManager25.jsp` — Hide X remove button for ALL-scoped sections, ALL badge, exclude from assign dropdowns
- Bug fix: removed misplaced `<c:if>` wrapper inside Fields table

### Admin Page GUI Polish
- **Rate Manager (`rateManager25.jsp`):** Removed subtitle row, moved rate name/controls into Pricing Grid hdr-bar, added `mt-3` spacing
- **Resource Library (`library25.jsp`):** Removed subtitle row, moved resource title into Details hdr-bar, added `mt-3` spacing

No database changes — all servlet logic and JSP presentation only.

---

## February 27, 2026 — Production V019 + PSP Dashboard + Renewal Modal (Session 1)

- **V018 + V019 applied to production** (application section/field suppressed columns)
- **Migration script bug fixes:** Corrected `script` → `script_name` and `installed_on` → `applied_on` in V018/V019 self-registration INSERTs
- **Add Benefit to Renewal modal modernization:** SSA-colored header, modern form layout
- **PSP Admin Dashboard:** New `PspDashboardHome` servlet + `pspDashboard25.jsp`. Summary stat cards, filter bar (owner pills + stage pills), activity list grouped by due bucket, team workload panel. All filtering client-side in JavaScript. Dashboard link in Admin navbar dropdown.

---

## February 27, 2026 — ServiceItem Unification (Sessions 2–5)

**Living reference:** `serviceitem_unification_design_v2.md` (design doc, now complete)

Major project consolidating the four divergent "activity item → task sequence" paths into a unified ServiceItem model. Completed across four sessions:

### Phase 1: Schema (V020)
- Added columns to `templatepurpose` (renamed to ServiceItem in Java): `code`, `psp_id`, `is_suppressed`, `provider_ref`, `source_type`, `default_renewal_months`, `has_required_tasks`, `category_id`
- Added `renewal_months` to `benefit`, `service_item_id` to `los` and `enhancement`, `ticket_service_item_id` to `assignee`
- Backfilled ticket → ServiceItem links through existing TicketSubCategory → TemplatePurpose chain

### Phase 2: Java Entity Updates
- TemplatePurpose → ServiceItem, TemplateGroup → ActivityCategory class renames (tables unchanged)
- Added fields: `Benefit.renewalMonths`, `Ticket.ticketServiceItem`, `LOS.serviceItem`, `Enhancement.serviceItem`
- Updated `DatabaseInitializer`, `Importer`, `RenewalService` for new fields

### Phase 3: Backfill & Linkage (V021)
- Created 10 new group 2 ServiceItems (IDs 25-35) for expanded LOSs
- Linked all 14 LOS and 4 Enhancement records to ServiceItems
- Renamed SI 17 to "Payment Services", suppressed SI 18 and SI 20
- Wired auto-ServiceItem creation into `ServiceManagerAction` for new LOS/Enhancement

### Phase 4: TicketSubCategory Elimination
- Converted Create Ticket flow and global cache from TSC to ServiceItem
- `AmsDataGlobal.ticketSubCategories` → `ticketServiceItems`
- `CreateTicket25` writes `ticketServiceItem` directly
- `SequenceBuilder25` / `SequenceAction25` read suppression from ServiceItem
- JSP dropdowns iterate ServiceItems instead of TSCs

### Phase 5: Code Cleanup
- Removed all TSC creation from `DatabaseInitializer`, `ReferenceDataSeeder`, `EntityLookup`
- Removed `SessionVar.ticketReasonList`, `TicketQueryDAO.getTicketSubCats()`
- Deleted `createTicketForm.jsp`, `createTicketFormNew.jsp`, `ddTicketTypes.jsp`
- `TicketKnowledgeDAO` JPQL updated from `ticketSubCategory` to `ticketServiceItem`

### Phase 6: Production Upgrade + Table Drop (V022, V023, V024)
- **V022:** Discovered 730 orphaned tickets (TSC records with NULL `temp_purpose_id`). Created 6 catch-all ServiceItems (IDs 36-41) and backfilled all orphaned tickets.
- **V023:** Dropped `ticketsubcategory` table and FK column from `assignee`. Deleted `TicketSubCategory.java`, removed `ticketSubCategory` field from `Ticket.java`, simplified all JSP ternary fallbacks.
- **V024:** Fixed `a_base_01` through `a_base_05` view chain that still referenced the dropped `ticket_category` column.
- Final grep for `TicketSubCategory` across all `.java` and `.jsp`: zero results.

**Remaining optional items:** JSP file renames (cosmetic: `ddTemplatePurposes.jsp` → `ddServiceItems.jsp`), future Benefit → ServiceItem direct FK shortcut.

---

## February 27, 2026 — Docs Cleanup & Consolidation (Session 6)

- Deleted 4 obsolete importscript files (V013 and V017 baselines, V013 and V016 upgrade scripts)
- Exported new `beta_ssa_dev_baseline_thru_V024.sql` baseline
- Consolidated 10 individual session summary files into this archive
- Updated `migration_tracker.md` (V024, all environments ✅)
- Updated `schema_version_migration.sql` (V024 entry added)

---

## February 28, 2026 — Billing Fixes & Demo Seeder (Session 7)

### Billing Fixes
- **GUID 404 fix:** `BillingQueryDAO.getEmployerByBillingGuid()` was doing an indirect lookup through `sEmployer.organizationId` → `EntityLookup.getEmployerById()` (wrong PK space). Changed to use direct `employer` FK on `BillingLink`. Also fixed `getBillingMonthByBillingGuid()` double-call.
- **Send Billing redesign:** `sendBillingForm.jsp` fully rewritten with CKEditor 5 rich text editor, chip-based recipient toggles, collapsible extra contacts, improved default message text. `SendEmployerBillingDetail.getMessage()` updated for single `emailBody` param with legacy fallback.
- **Context-aware billing link:** `EmailBillingToEmployer` now derives base URL from `request` instead of `AppConstantDAO.getWebPath()` — works correctly on localhost and production.

### Branding Fallback Fix
- `AmsDataGlobal.setConstants()` — added `brandingFileExists()` helper that checks if `/branding/*` paths actually exist on disk before using them. Prevents broken images when switching between environments with shared database.

### Navbar Update
- Added `BillingAction` to PSP Admin dropdown in `navbar25.jsp`.

### Demo Seeder (D-24)
- **New file:** `SeedDemoData.java` — PSP-Admin-only servlet at `/SeedDemoData`
- Idempotent via `DEMO_DATA_SEEDED` database constant
- Creates: 5 employers (Acme Manufacturing, Bright Horizons, Cascade Financial, Delta Medical, Evergreen Landscaping), 16 employees, 12 benefits (FSA/HRA/HSA/COBRA/DCA/Dental/Vision), 3 renewals, 2 setups, 5 tickets — all with linked checklists
- Creates demo user accounts: Jennifer Martinez (PSP User), Alex Rivera (BPO Admin), Priya Sharma (BPO User) — all password `demo123`
- Negative IDs for employers/employees/benefits to avoid collision with real data

### Factory Reset Servlets (D-24 continued)
- **New file:** `DatabaseResetUtil.java` — Static utility with `SavedState` inner class, captures PSP/Person/User/Address/Agency/Constants values, clears all tables (TRUNCATE with FK checks disabled), re-initializes via `DatabaseInitializer.performInitialization(em)`, restores admin password hash/salt
- **New file:** `ReSeedDb.java` — Factory-reset servlet at `/ReSeedDb`. GET renders confirmation page with deployment key input. POST validates key against `AppConfig.get("DEPLOYMENT_KEY")`, then runs capture → clear → reinitialize → reload globals. PSP Admin session required.
- **New file:** `ReSeedDemoData.java` — Extends `ReSeedDb`, overrides `executeReset()` to also call `SeedDemoData.seedAllDemoData(em, out)` after the base reset. Shows demo credentials table on success.
- **Modified:** `DatabaseInitializer.java` — Extracted `performInitialization(em)` from `initializeDataBase(request, em)` so reset servlets can call init logic without an HttpServletRequest
- **Modified:** `SeedDemoData.java` — Extracted `seedAllDemoData(EntityManager em, PrintWriter out)` public method so `ReSeedDemoData` can invoke it

### Git / Infrastructure
- `.gitignore` updated: added `/out/` (IntelliJ artifact output) and `.claude/` (Claude Code metadata)
- Removed stale `.claude/worktrees/` entries from git tracking

---

## February 28, 2026 — Factory Reset Bug Fixes (Session 8)

### ReSeedDb / ReSeedDemoData Fixes
- **SMTP Settings NPE:** `CloseActivity25.processActivityClosure()` threw NPE when browser URL pointed to `/CloseActivity25` (via `RequestDispatcher.forward()`) and SMTP settings form posted back. Added null guard for `local.getCurrentActivity()`. Also changed `UpdateSmtpSettings` to always redirect to `ViewHome25` instead of using unreliable referer.
- **User PK type error:** `DatabaseResetUtil` used `em.find(User.class, person)` but User's `@Id @OneToOne Person` maps to Long PK. Changed both occurrences to `em.find(User.class, person.getId())`.
- **FK constraint on BillingGroup:** After TRUNCATE, EclipseLink's L2 shared cache still had stale BillingGroup entities, causing `DatabaseInitializer.createBillingGroup()` to skip INSERTs. PlanType then failed on FK. Added L2 cache eviction after truncation.
- **EclipseLink identity map corruption:** Reusing the same EntityManager after native SQL TRUNCATE confused EclipseLink's internal identity maps. Changed `executeReset()` to close the old EM and return a fresh one.
- **Three-EM pattern for ReSeedDemoData:** Init EM had managed entities (Person 104, CheckList 29, Ticket 99) whose cross-references confused EclipseLink. `ReSeedDemoData.executeReset()` now closes the init EM and creates a third fresh EM for demo seeding.
- **EclipseLink `evictAll()` descriptor corruption (critical):** `emf.getCache().evictAll()` in EclipseLink 3.0.2 corrupts internal descriptor metadata — Person entity was mapped to ASSIGNEE table (`RelationalDescriptor(Person --> [DatabaseTable(ASSIGNEE)])`). Replaced with per-class eviction via JPA Metamodel API: `DatabaseResetUtil.evictEntityCaches(emf)` iterates `emf.getMetamodel().getEntities()` and evicts each class individually.

---

## February 28, 2026 — Summit Import + DatabaseInitializer Overhaul (Session 9)

### V025 + V026 Migrations
- **V025:** Added `level`, `los`, `employer_name` columns to `plantype` table for Summit import metadata.
- **V026:** Benefit table surrogate PK — added `summit_id`/`source_type` columns, renumbered negative PKs to positive, converted `benefit_id` to AUTO_INCREMENT, added unique index on `(source_type, summit_id)`. Decouples internal PKs from Summit's `EmployerPlan_ID`.

### Summit Data Import Wizard (D-35)
- **New file:** `SummitImportWizard.java` — Multi-step wizard servlet for importing Summit CSV exports (Plan Types, Employers, Employees, Benefits)
- **New file:** `SummitImportService.java` — Business logic for parsing and importing each Summit export file
- **Modified:** `EntityLookup.java` — Added `getBenefitBySummitKey(em, sourceType, summitId)` for source-discriminated lookups
- **Modified:** `Benefit.java` — Added `@GeneratedValue(IDENTITY)`, `summitId`, `sourceType` fields
- **Modified:** `Updater.java`, `SummitSync.java` — Updated JPQL joins from `b.id` to `b.summitId`, removed negation hack
- **New JSPs:** `step1Upload.jsp` and supporting wizard step pages

### DatabaseInitializer Seed Data Overhaul
Systematic cleanup of all seed data to reflect a production-ready fresh deployment:

- **ActivityCategory:** Added "Opportunity" (ID 4), renamed "User" to "Opportunity" in ReferenceDataSeeder
- **ServiceItem unification alignment:** Restructured all ServiceItem seeding:
  - Setup: 3 items — COBRA, Flexible Spending Accounts (FSA), Debit Cards — linked 1:1 to LOS/Enhancement entities
  - Renewals: Auto-created via `createPlanTypeWithRenewal()` — each PlanType gets a 1:1 Renewal ServiceItem
  - Tickets: 1 category (General) + 1 service item with `hasRequiredTasks=true`
  - Opportunities: Nothing seeded (PSP creates as needed)
- **PlanType expansion:** 16 plan types (DCA, FSA, HRA, HSA, COBRA, Transit, Parking, Adoption, Tuition, Lifestyle, Medical, Dental, Vision, Group Life, STD, LTD)
- **LOS simplified:** 2 entries (COBRA, CDH) linked to Setup ServiceItems
- **Enhancement added:** 1 entry (Debit Cards) linked to Setup ServiceItem
- **ServiceModules simplified:** 3 modules matching LOS + Enhancement
- **BillingGroup simplified:** Single "Standard" billing group (removed 3 unused groups)
- **Demo benefits removed:** Benefits come from Summit import; `SeedDemoData` handles demo data
- **Monthly import tasks removed:** 7 orphaned tasks with unreferenced shortcodes
- **Initialization checklist rewritten:** "Summit Data Transfer" onboarding checklist with 4 steps:
  1. Setup Exports in Summit (doc link placeholder)
  2. Download Plan Types from Summit (doc link placeholder)
  3. Import Summit Exports into AMS (links to `/SummitImport`)
  4. Modify Benefit Renewal Frequencies

### SeedDemoData Enhancement
- Added `closeInitializationChecklist()` — marks the initialization checklist as complete with yesterday's date when demo data is seeded, so it doesn't appear in the task list.

### Bug Fixes
- **ManageTask25 cancel button NPE:** `taskManager25.jsp` called `getCurrentActivity().getActivity()` without null-checking `getCurrentActivity()`. Added null guard.
- **Tax ID data truncation:** `Agency.taxId` was `varchar(10)`, too short for formatted inputs like `12-34567890` (11 chars). Widened to `varchar(20)`.
- **Initialization form validation:** Added `maxlength` attributes to all `initialize.jsp` form fields. Added server-side `validateFormFields()` in `InitializeDataBase.java`. Wrapped initialization in try/catch to show errors on form instead of stack traces.

### Files Changed
- `DatabaseInitializer.java` — Major restructure of seed data
- `ReferenceDataSeeder.java` — "User" → "Opportunity" category rename
- `SeedDemoData.java` — Close initialization checklist on demo seed
- `InitializeDataBase.java` — Form validation + error handling
- `Agency.java` — `tax_id` column widened to `varchar(20)`
- `taskManager25.jsp` — Null guard on cancel button
- `initialize.jsp` — `maxlength` attributes on all fields

---

## February 28, 2026 — V027 BPO Refactor + PSP Settings + Opportunity Enhancements + Sales Fixes (Session 10)

### V027 Migration: BPO Registration Task Source Refactor
- **New file:** `docs/migrations/V027__bpo_registration_task_source.sql`
- Added `is_approved`, `is_requested`, `is_accepted` status columns to `bpo_registration`
- Changed task vendor sourcing from Person FK (`source_owner`) to BpoRegistration FK (`bpo_registration_id`)
- Updated entities: `Task.java`, `ToDoOut.java`, `ToDoOut25.java`, `BpoRegistration.java`
- Updated DAOs: `TaskDAO.java`, `ActivityListDAO.java`, `ActivityLandingDao.java`
- Updated servlets: `UpdateTask25.java`, `AddToDo25.java`
- Updated JSPs: `taskManager25.jsp`, `bpoHome25.jsp`

### PSP Settings Modal + Use Timeclock Toggle
- **Renamed:** `UpdateSmtpSettings.java` → `UpdatePspSettings.java` — handles both SMTP and feature settings
- **Redesigned:** `smtpSettingsMod25.jsp` — tabbed modal with Email Settings and Features tabs
- **New constant:** `USE_TIMECLOCK` in `DatabaseInitializer` — toggles timeclock vs quick ticket card
- **New file:** `quickTicket25.jsp` — inline ticket creation card replacing timeclock when disabled
- **Modified:** `navbar25.jsp` — "Email Settings" → "Settings" with `#pspSettingsMod` target, conditional "Time Corrections" link
- **Modified:** `pspHome25.jsp` — conditional rendering of timeclock vs quick ticket column
- **Modified:** `AmsDataGlobal.java` — `useTimeclock` flag loaded from constants

### Add Activity Button on Home Page
- **New file:** `addActivityModal25.jsp` — modal with Renewal/Opportunity type toggle
  - Renewal: employer select → posts to `CreateBlankRenewal25`
  - Opportunity: existing/new prospect toggle, agency select → posts to `CreateOpportunity` with `returnTo=home`
- **Modified:** `activityHeader25.jsp` — added "+" button in toolbar
- **Modified:** `CreateOpportunity.java` — returns Opportunity from `createOpportunity()`, supports `returnTo=home` parameter
- **Modified:** `AmsDataGlobal.java` — added `prospects` list cache, `opportunityManagers` list (roles 5+9)
- **Modified:** `AmsDataLocal.java` — added overloaded `getActivity25u(em, Opportunity)` method

### Opportunity Detail Card Enhancements
- **Stage dropdown:** Replaced static badge with editable `<select>` for authorized users (owner, agent, PSP admin)
- **Two-column layout:** Row A (Prospect | Stage), Row B (Agent | Agency), Row C (Managed By) — responsive, single-column on mobile
- **Managed By editing:** PSP admins get editable dropdown populated from `opportunityManagers` (roles 5+9 only); other users see static text
- **Modified:** `UpdateOpportunityStage.java` — AJAX support (`ajax=true` returns JSON), `managedById` parameter handling
- **Modified:** `detailOpportunity25.jsp` — two-column layout, AJAX functions for stage and managedBy updates
- **Modified:** `AmsDataGlobal.java` — `loadOpportunityManagers()` method combining PSP Admin (5) and PSP Sales (9) roles

### Sales Pipeline Fixes
- **JPQL field name mismatch:** `SalesDAO.getLosFull()` and `getModuleFull()` referenced `sm.serviceItemList` but the JPA field on `ServiceModule` is `moduleDetailList` (getter is `getServiceItemList()`). Fixed both queries.
- **Proposal Detail page cleanup:**
  - Removed duplicate header rows (two "Proposal #..." blocks)
  - Page title now shows `Proposal #123` in navbar
  - Added `proposal-content` CSS wrapper: `width: fit-content; min-width: 700px; max-width: 100%; margin: 0 auto` on desktop (992px+) — content-driven width that centers and shrinks on wide monitors
  - Added `mt-2` gap between navbar and content
  - Added `size="70"` on GUID link input to prevent text truncation under `fit-content`

### Bug Fixes
- **NPE in UpdateTask25:** Fixed null pointer when `getSourceOwner()` was null
- **HTML corruption on BPO dashboard:** Fixed malformed JSP in `bpoHome25.jsp`
- **Quick ticket card margin:** `.tc-panel` had conflicting margin-top; fixed by zeroing inline and using `mt-2` on parent column div
- **Opportunity detail horizontal scrollbar:** Changed Bootstrap `gx-3` to `g-0` on row elements to eliminate negative-margin overflow
- **SeedDemoData todo creation:** Fixed demo data seeder creating todos for seeded activities

---

## February 28, 2026 — V028 Benefit Plan Year + Renewal Audit (Session 11)

### Problem: Short Plan Years Breaking Renewal Dates
Summit import calculates `nextRenewalDue` as `effectiveDate + renewalMonths`, but benefits with short plan years (e.g., FSA starting 6/1 but renewing 1/1 annually) get wrong renewal dates. The real renewal anchor is `planYearEnd + 1 day`.

### V028 Migration
- **New file:** `docs/migrations/V028__benefit_plan_year_columns.sql`
- Added `plan_year_start DATE NULL` and `plan_year_end DATE NULL` to `benefit` table

### Benefit.java Entity Changes
- Added `planYearStart`, `planYearEnd` fields with JPA `@Column` mapping
- Added `getDetectedRenewalDate()` helper: returns `planYearEnd + 1 day` as LocalDate

### J7 (COBRA) Import Fix — Use `enddate` as Renewal Anchor
- **Modified:** `SummitImportService.importBenefitsCobra()`
- Pre-computes latest plan year end/start across J7 multi-rows per benefit
- **New benefits:** Use `enddate + 1` as renewal anchor (instead of `effectiveDate`)
- **Existing benefits:** Only update `planYearStart`/`planYearEnd` (never overwrite `nextRenewalDue`)

### J5 (CDH Benefit Plan Years) Import — New Method
- **New method:** `SummitImportService.importBenefitYears()`
- Parses J5 CSV, groups by `EmployerPlan_ID`, finds latest plan year per benefit
- Stores `planYearStart`/`planYearEnd` on CDH benefits (matched via `summitId` + `sourceType='CDH'`)
- **First-time seeding only:** Seeds `nextRenewalDue = planYearEnd + 1` only when `planYearEnd` was previously null
- **Short plan year detection:** Compares end date month/day across plan year rows; flags warnings when inconsistent

### Summit Import Wizard — J5 Integration
- **Modified:** `SummitImportWizard.java` — added J5 session attribute, upload handling, validation (J5 requires J4)
- **Import order:** Plan Types → Employers → Employees → Benefits CDH → Benefits COBRA → Benefit Plan Years (J5)
- **Modified:** `step1Upload.jsp` — added 7th upload card for "Benefit Plan Years (J5)"
- `step2Configure.jsp` and `step4Results.jsp` handle J5 dynamically (no changes needed)

### Benefit Renewal Audit Page
- **New servlet:** `BenefitAudit25.java` at `/BenefitAudit` — PSP Admin only
- **New JSP:** `benefitAudit25.jsp` — full-height flex layout with sticky headers
- **Columns:** Employer, Plan Name, Type, Source (CDH/COBRA), Effective Date, Plan Year End, Detected Renewal, Next Renewal Due (editable), Renewal Months (editable), Status
- **Features:**
  - Employer name search with live typing match (scrolls to first alphabetic match)
  - "Flagged Only" filter — shows benefits with inconsistent year-to-year end dates
  - "No Renewal" filter — shows benefits with null `nextRenewalDue`
  - Inline save per row (date + months)
  - "Accept All Detected" bulk action — sets `nextRenewalDue` from plan year data for all benefits
  - Ghost-style action buttons in toolbar
- **Modified:** `navbar25.jsp` — added "Benefit Audit" link in admin dropdown

### Key Design Decisions
- **J5 does NOT auto-modify `nextRenewalDue`** — only stores plan year data. The audit page drives renewal corrections.
- **J7 re-import does NOT modify `nextRenewalDue`** on existing benefits — only updates plan year dates.
- **New benefit seeding rules:** J7 new COBRA → `enddate + 1`; J4/J5 new CDH with J5 → `planYearEnd + 1`; J4 without J5 → `effectiveDate + 12 months`
- **Flagging = short plan year detection** — only triggers when end dates change year-to-year across multiple plan year rows (not effective vs renewal mismatch)

---

## March 1, 2026 — Session 12/13: User Manager + Days Since Contact

Two sessions covering configurable activity alert settings and the new User Manager feature.

### Days Since Contact — Configurable Setting (Session 12)
- **Modified:** `smtpSettingsMod25.jsp` — Added "Days Until Contact Alert" number input in Features tab
- **Modified:** `UpdatePspSettings.java` — Reads/writes `DAYS_SINCE_WARNING` constant
- **Modified:** `AmsDataGlobal.java` — Added `daysSinceWarning` field, loaded from constants
- **Modified:** Activity view JSPs — Use configurable threshold instead of hardcoded 7 days

### V029 Migration — User Deactivation
- **New file:** `docs/migrations/V029__user_is_active.sql`
- Adds `is_active BOOLEAN NOT NULL DEFAULT TRUE` to user table
- All existing users default to active

### User Entity — isActive Field
- **Modified:** `User.java` — Added `isActive` boolean field, getter/setter

### Login Security — Block Deactivated Users
- **Modified:** `AuthenticateUser.java` — `validatedLogin()` rejects inactive users after credential validation
- **Modified:** `OneTimeUserLogin.java` — GUID-based login rejects deactivated accounts
- **Modified:** `AuthenticateUser.getUsersByRole()` — Filters inactive users from role lookups

### Global Cache Filtering
- **Modified:** `RecurringChecklistDAO.getPspUserList()` — Added `u.isActive()` check
- **Modified:** `AuthDAO.getPspStaff()` — Added `u.isActive()` check
- **Modified:** `AmsDataGlobal.java` — Added `refreshUserCaches(EntityManager em)` method to reload all user-related caches (users, opportunityManagers, bpoUsers) after activation/role changes

### UserManager Servlet
- **New file:** `UserManager.java` at `/UserManager`
- **GET:** Returns JSON user list (personId, name, email, roles, active, homeAgent) for the Manage Users tab. Also routes `action=getCounts` for reassignment count lookups. Filters out BPO-only users (roles 102/103).
- **POST:** AJAX JSON handler for 6 actions:
  - `getCounts` — returns open activities, managed opportunities, delegated todos, owned tasks for a person
  - `deactivate` — validates (not self, not last admin), bulk reassigns activities/opportunities/todos/tasks, clears agency manager, sets `isActive=false`, refreshes caches
  - `reactivate` — sets `isActive=true`, refreshes caches
  - `addAgentRole` — adds role 2, adds person to PSP home agency's agentList (auto-resolved, no agency selection)
  - `removeAgentRole` — reassigns open opportunities, removes role 2 (and role 8 if present), removes from home agency
  - `addPspUserRole` — adds role 1 to expand agent-only to PSP User

### User Manager Modal
- **New file:** `userManager25.jsp` — Modal component (not full page), following Settings modal (`smtpSettingsMod25.jsp`) pattern
- **Tab 1: Create User** — Server-rendered form with role checkboxes, sales capability toggle, agency selection. All IDs prefixed `um` to avoid conflicts with `createUserModal25.jsp`.
- **Tab 2: Manage Users** — AJAX-loaded table built via JavaScript when modal opens. Columns: Name, Email, Roles (badges), Status (Active/Inactive), Actions.
- **Action buttons per user:** Deactivate (opens stacked sub-modal with counts and reassignment dropdown), +Agent (confirm dialog, auto home agency), -Agent (stacked sub-modal with opportunity reassignment, home agency agents only), +PSP User (confirm dialog, home agency agents only), Reactivate (for inactive users)
- **Sub-modals:** Deactivate confirmation and Remove Agent confirmation stack on top of the main modal with z-index fix for Bootstrap 5
- **Reassignment dropdowns:** Built dynamically from AJAX user data, excluding the target user

### Navbar Integration
- **Modified:** `navbar25.jsp` — PSP Admin dropdown: changed "Create User" page link to "User Manager" modal trigger button (`data-bs-target="#userManagerModal"`). Added `<c:import>` for `userManager25.jsp` in PSP Admin modal imports block.
- Agency Admin "Add Agent" and BPO Admin "Create User" buttons unchanged — still use `createUserModal25.jsp`

### CreateUser25 Cleanup
- **Modified:** `CreateUser25.java` — Removed `returnTo=UserManager` redirect handling (no longer needed since UserManager GET returns JSON, not a page)

### Key Design Decisions
- **Modal, not full page** — User Manager is a modal (like Settings) to keep the admin workflow lightweight and accessible from any page
- **BPO users excluded** from the manage view — production systems won't have both PSP and BPO users (BPO users are demo data only)
- **addAgentRole auto-assigns to PSP home agency** — no agency selection needed; internal staff agents always belong to the home agency
- **removeAgentRole scoped to home agency agents only** — external agency agent deactivation requires prospect reassignment and "turn off agency" logic (deferred)
- **Deferred to future plan:** External agency agent deactivation, "turn off agency" feature when last agent in external agency is deactivated

---

## March 1, 2026 — Navbar Menu Consolidation (Session 14)

Consolidated navbar menu items to eliminate duplication for multi-role users (especially PSP Admin + Agent + Agency Admin).

### Logo Link Priority Fix
- **Modified:** `navbar25.jsp` logo `<c:choose>` block — added `isPspUser || isPspAdmin → ViewHome25` check before `isAgent || isAgencyAdmin → AgentHome`
- Previously, a PSP Admin who was also an agent was incorrectly sent to AgentHome when clicking the logo

### Sales Dropdown Consolidation
- **Moved Sales dropdown** outside the PSP User/Admin `<c:if>` block into its own standalone section
- **New visibility condition:** `isAgent || isAgencyAdmin || isPspAdmin` (was only visible to PSP Users/Admins)
- **Role-filtered contents:**
  - Pipeline (AgentHome) + New Proposal (ProposalBuilder) — `isAgent` only
  - Application Review — `isPspAdmin` only (with conditional divider when agent items present)
  - Add Agent (createUserModal) — `isAgencyAdmin` only (with divider)

### Standalone Agent Buttons Removed
- **Deleted** the entire `AGENT / AGENCY MANAGER LINKS` section (Pipeline, New Proposal, Add Agent as top-level navbar buttons)
- All items now consolidated inside the Sales dropdown

### Resulting Navbar Per Role
- **PSP Admin + Agent + Agency Admin:** Home, Log, Email, Sales (5 items), Admin, Logout
- **PSP User + Agent:** Home, Log, Email, Sales (Pipeline + New Proposal), Logout
- **PSP User only:** Home, Log, Email, Logout (no Sales dropdown)
- **Agent only:** Sales (Pipeline + New Proposal), Logout
- **Agent + Agency Admin:** Sales (Pipeline + New Proposal + Add Agent), Logout
- **BPO:** Unchanged

### Files Changed
- `src/main/webapp/WEB-INF/view/a/general/navbar25.jsp` — Only file modified

No database changes.

## March 1, 2026 — PSP Role Separation: PSP Priority Over Agent (Session 15)

Fixed login redirect chain, navbar logo, and activity filter behavior so PSP roles take priority over Agent roles for dual-role users. Previously Agent/AgencyAdmin won over PSP in routing.

### Priority Rule (applied everywhere)
1. BPO roles → BpoHome (unchanged)
2. PSP roles (isPspUser || isPspAdmin) → ViewHome25 — **PSP wins** even if user also has Agent
3. Agent-only (isAgent || isAgencyAdmin but NOT PSP) → AgentHome

### Login Redirect Reorder
- **AuthenticateUser.java** `goToPage()` — reordered BPO → PSP → Agent-only; switched unsafe `(boolean)` casts to `Boolean.TRUE.equals()`
- **OneTimeUserLogin.java** `authenticateAndRedirect()` — added `isPspUser`/`isPspAdmin` session reads; reordered redirect chain

### Activity Filter: Opportunity Chip for Agents
- **activityHeader25.jsp** — added `sessionScope.isAgent` to Opportunity chip visibility gate (was `isPspSales || isPspAdmin` only)
- **FilterActivities25.java** — added `isAgent` to opportunity preservation in preset path

### Agent-Only Default Filters
- **AmsDataLocal.java** `intializeLocalData()` — agent-only users (no PSP roles) default to Ticket + Opportunity only; Renewal and Setup hidden

### Other Servlet Redirects
- **CreateUser25.java** `goToHome()` — reordered to BPO → PSP → Agent-only
- **Reviewed (no change needed):** ChecklistAction25, CloseActivity25, CreateChecklist25, CreateReminder25, MakeRecurringFromChecklist25, ModifyRecurringTask25, UpdateTask25 — all already forward to ViewHome25 for non-BPO users

### Files Changed
- `AuthenticateUser.java`, `OneTimeUserLogin.java`, `CreateUser25.java`, `FilterActivities25.java`, `AmsDataLocal.java`, `activityHeader25.jsp`

No database changes.

## March 1, 2026 — AJAX Opportunity Section + Agency → Prospect Cascade (Session 16)

Converted the Opportunity section of the Add Activity modal from JSTL to AJAX, adding agency → prospect cascading. Reuses the existing `SetupModalData` endpoint — no servlet changes needed.

### Changes
- **Agency dropdown:** Replaced JSTL `<c:choose>`/`<c:forEach>` with empty `<select>` populated by JS from `SetupModalData`
- **Prospect dropdown:** Replaced JSTL `<c:forEach>` with empty `<select>` filtered by selected agency's `agencyIds`
- **UI reorder:** Agency moved above prospect toggle (agency drives cascade)
- **Shared data loader:** `aa_loadSetupData()` → `aa_loadModalData(type)` — single fetch serves both Opportunity and Setup tabs with caching
- **New functions:** `aa_rebuildOppOptions()` (agency population + auto-select), `aa_onOppAgencyChange()` (prospect filtering)
- **Cache reset:** `aa_setupData = null` on modal close for fresh data on next open

### Files Changed
- `src/main/webapp/WEB-INF/view/a/pspHome/columns/activities/addActivityModal25.jsp` — Only file modified

No database changes.

## March 1, 2026 — Global Ghost Buttons + Checklist Detail Fix (Session 17)

- **Global `.ssa-action` ghost buttons:** Applied across 30+ modals in 15 files for modal footers/form actions
- **Fixed broken `goCheckListDetail` cancel URL**

No database changes.

---

## March 2, 2026 — Client-Side Activity Filtering + ServiceManager Fixes (Session 18)

### Client-Side Activity Filtering
Converted the home page activity filter from server-side round-trips to instant client-side filtering:

- **ViewHome25.java** — Loads ALL open activities in one SQL query (ownership=0, all types, no attention filter). Removed `isActionableForPerson` post-filter. Added `mePersonId`, `daysSinceWarning`, `canSeeOpportunities` request attributes for JS.
- **FilterActivities25.java** — Added AJAX support: returns 204 No Content for `X-Requested-With: XMLHttpRequest` requests instead of forwarding to ViewHome25.
- **activityHeader25.jsp** — `afSubmit()` now calls `filterAndRender()` (client-side) + debounced `syncFilterToServer()` (background AJAX). Presets serialized to JS array. Preset links changed from `href` navigation to `onclick` with `applyPreset()`. Added `updateRowCount()`, `buildSummary()` functions.
- **activityList25.jsp** — `activityRows` serialized to `ALL_ACTIVITIES` JS array. JSTL `<c:forEach>` replaced with `filterAndRender()` that produces identical card HTML. Row click uses dynamic form POST to `GoActivityDetail25`. Initial render on page load uses current UI control state.

### ServiceManager Enhancement ↔ LOS Assignment Fix
- **Bug:** JSP forms sent `assignEnhancementToLos` / `assignLosToEnhancement` but servlet cases were named `addEnhancementToLos` / `addLosToEnhancement` — action name mismatch caused silent no-ops
- **Fix:** Renamed both servlet cases to match JSP (`assign*` prefix, consistent with section assignment naming)
- **Also:** Added missing `contains()` guard to `assignLosToEnhancement`

### ServiceModule Auto-Creation for Features
- **Bug:** `createLos` and `createEnhancement` auto-created ServiceItem but never created ServiceModule. Without a ServiceModule, `findModuleByLos`/`findModuleByEnhancement` returned null, so the Add Feature button/modal never rendered.
- **Fix:** Added auto-creation of ServiceModule in both `createLos` (linked via `setLos`) and `createEnhancement` (linked via `setEnhancement`)

### Files Changed
- `ViewHome25.java`, `FilterActivities25.java` — Client-side filter infrastructure
- `activityHeader25.jsp`, `activityList25.jsp` — JS filtering + rendering
- `ServiceManagerAction.java` — Enhancement assignment fix + ServiceModule auto-creation

No database changes.

---

## March 2, 2026 — Upcoming Renewals Page + MonthlyBiller Fix (Session 19)

### Upcoming Renewals Full Page
Replaced the modal-based renewal employer picker with a standalone page at `/UpcomingRenewals`:

- **UpcomingRenewals25.java** (new) — Servlet with GET (load grouped renewals) and POST (selectEmployer expands card inline, startRenewal delegates to AddRenewal25). Groups `RenewalEmployer` DTOs by month using bulk `MIN(nextRenewalDue)` query. OVERDUE bucket first, then chronological `TreeMap<YearMonth>`.
- **upcomingRenewals25.jsp** (new) — Full standalone page. Month group headers (red=overdue, blue gradient=future) with employer count badges. Collapsed employer cards with color-coded left border by stage. Expanded state shows benefit checkboxes with urgent items (due within 30 days) pre-checked in red. Auto-scrolls to expanded employer.
- **navbar25.jsp** — Added "Renewals" link with `bi-calendar-check` icon after Email, visible to PSP Users and PSP Admins.

### MonthlyBiller Null Guard
- **MonthlyBiller.java** — Added `if (b == null || ee == null) continue;` in `logCoverageStatusForThisMonthCDH()` after the EntityLookup calls, matching the existing pattern in `logCoverageStatusForThisMonthPB()`.

### Files Changed
- `UpcomingRenewals25.java` (new), `upcomingRenewals25.jsp` (new) — Renewals page
- `navbar25.jsp` — Renewals nav link
- `MonthlyBiller.java` — Null guard

No database changes.

---

## March 2, 2026 — BPO Cross-System Architecture (Session 20)

### WS1: Role Detection + Foundation (V030)
- **AppConfig.java** — Added `isPsp()`, `isBpo()`, `getSystemType()` methods reading `system.type` from `ssa.properties`
- **EmfListener.java** — Added `DelegatedToDo.class` and `PspClient.class` to managed entity list
- **AmsDataGlobal.java** — Conditional loading: BPO skips employer/benefit/renewal data; PSP skips BPO client lists
- **BpoRegistration.java** — Added API token (outbound/inbound), partner_url, date columns, and `isAvailable()` helper
- **DelegatedToDo.java** (new) — BPO-side entity for tasks delegated from PSP. Fields: todoGuid, pspClient, taskName, dueDate, status, assignedTo, completed flags
- **PspClient.java** (new) — BPO-side entity tracking PSP partnerships. Fields: pspName, pspUrl, API tokens, status (PENDING/APPROVED/REJECTED/DISCONNECTED)
- **V030 migration** — Creates psp_clients and delegated_todo tables, adds API columns to bpo_registration, adds todo_guid to todo_note
- **navbar25.jsp** — System-type-aware links: Vendor Manager (PSP), PSP Clients (BPO)

### WS2: Partnership Flow
- **ApiTokenFilter.java** (new) — Filter on `/api/*` validating `Authorization: Bearer {token}` against BpoRegistration (PSP) or PspClient (BPO) inbound tokens
- **PartnershipRequestApi.java** (new) — PSP receives BPO partnership requests (POST /api/v1/partnership/request)
- **PartnershipApproveApi.java** (new) — PSP approves/rejects partnerships (POST /api/v1/partnership/approve), generates API tokens
- **VendorManager25.java** (new) — PSP admin page for managing BPO vendors
- **BpoPspClients.java** (new) — BPO admin page for managing PSP client partnerships
- **ApiClient.java** (new) — HTTP utility for cross-system JSON calls with Bearer auth
- **vendorManager25.jsp** (new), **pspClients25.jsp** (new) — Admin UIs

### WS3: Task Delegation API
- **TaskReceiveApi.java** (new) — BPO endpoint receiving tasks from PSP (POST /api/v1/tasks), creates DelegatedToDo records, deduplicates by todoGuid
- **TaskUpdateApi.java** (new) — BPO endpoint for REVERT/RECALL/UPDATE commands (POST /api/v1/tasks/update)
- **TaskNotesApi.java** (new) — BPO serves/receives notes by todoGuid (GET/POST /api/v1/tasks/notes)
- **V031 migration** — Makes todo_note.todo_id and created_by_id nullable, adds author_name column for cross-system display

### WS4: PSP Integration Hooks
- **BpoTaskPushService.java** (new) — PSP-side service pushes sourced tasks to BPO vendors after checklist creation. Groups by BpoRegistration, builds activity context, POSTs to each vendor. All failures non-fatal.
- **TaskCompletedCallbackApi.java** (new) — PSP receives task completion callbacks (POST /api/v1/callback/task-completed), marks local ToDo.bpoCompleted=true
- **NoteAddedCallbackApi.java** (new) — PSP receives note callbacks (POST /api/v1/callback/note-added), creates cross-system ToDoNote
- **AddRenewal25.java** — Added BpoTaskPushService.pushDelegatedTasks() hook after renewal checklist creation
- **CreateChecklist25.java** — Added BpoTaskPushService.pushDelegatedTasks() hook after checklist creation

### WS5: BPO Task Management
- **BpoHome.java** — Dual-mode loading: cross-system queries DelegatedToDo, co-located queries local ToDo
- **bpoHome25.jsp** — Dual rendering with `<c:choose>` on `${crossSystemMode}`, JS tracks `currentCrossSystem`/`currentTodoGuid` for AJAX
- **BpoCompleteTask.java** — Dual-mode dispatch on `crossSystem` param. Cross-system modes operate on DelegatedToDo with PSP callbacks (non-fatal, after commit). Co-located modes unchanged.
- **BpoGetNotes.java** — Added todoGuid query support. Uses `getDisplayAuthor()` for null-safe author rendering
- **ToDoNote.java** — Made toDo/createdBy nullable, added authorName field, `getDisplayAuthor()` method
- **ApiClient.java** — Added `postJsonObject()` overload for complex payloads, refactored to shared `postJsonString()`

### Files Changed
- **New (18):** V030 migration, V031 migration, DelegatedToDo.java, PspClient.java, ApiTokenFilter.java, PartnershipRequestApi.java, PartnershipApproveApi.java, VendorManager25.java, BpoPspClients.java, ApiClient.java, vendorManager25.jsp, pspClients25.jsp, TaskReceiveApi.java, TaskUpdateApi.java, TaskNotesApi.java, TaskCompletedCallbackApi.java, NoteAddedCallbackApi.java, BpoTaskPushService.java
- **Modified (12):** AppConfig.java, EmfListener.java, AmsDataGlobal.java, BpoRegistration.java, navbar25.jsp, BpoHome.java, bpoHome25.jsp, BpoCompleteTask.java, BpoGetNotes.java, ToDoNote.java, AddRenewal25.java, CreateChecklist25.java

Database changes: V030 (cross-system foundation), V031 (nullable notes columns + author_name).

---

## March 2, 2026 — Session 21: BPO Initialization Path (D-45)

Added BPO deployment initialization support. When `initialize.jsp` submits a deployment key, the prefix determines system type:

- **PSP-key** → existing `DatabaseInitializer.initializeDataBase()` (unchanged behavior)
- **BPO-key** → new `DatabaseInitializer.initializeBpoDataBase()` (lightweight init)
- **PSP-key-DEMO** → PSP init + demo data seeder (existing behavior, preserved)

### Key Changes

- **InitializeDataBase.java** — New key parsing: `{TYPE}-{KEY}` or `{TYPE}-{KEY}-{DEMOTAG}` format. Validates PSP/BPO prefix, branches to correct initializer. Refreshes servlet context system type attributes post-init.
- **DatabaseInitializer.java** — New `initializeBpoDataBase()` method seeds shared foundation (sequence, statuses, roles, contact methods, days of week, link types, activity categories, reasons created, task frequencies, sentinel tasks, ticket category + service item, time entry, user with BPO Admin role, filter presets) while skipping PSP-specific structures (plan types, billing groups, LOS/Enhancement, service modules, application sections, demo users, Summit onboarding checklist). Added `addBpoConstants()` (SYSTEM_TYPE=BPO + shared SMTP/web/branding). Added `createBpoWelcomeChecklist()` (3-step BPO onboarding). Added `SYSTEM_TYPE=PSP` to existing `addPspConstants()`.
- **AppConfig.java** — Added `cachedSystemType` volatile field with `setSystemType()`. `getSystemType()` checks cache first, then falls back to `ssa.properties`, then defaults to "PSP".
- **AmsDataGlobal.java** — After `setConstants(em)`, reads `SYSTEM_TYPE` from DB and calls `AppConfig.setSystemType()` to cache the authoritative value.
- **EmfListener.java** — Refreshes `systemType`/`isBpoSystem`/`isPspSystem` servlet context attributes after `initializeGlobalData()` completes (DB constant overrides ssa.properties fallback).
- **initialize.jsp** — Added hint text below deployment key field: "Format: PSP-yourkey or BPO-yourkey"

### Files Changed
- **Modified (6):** AppConfig.java, DatabaseInitializer.java, InitializeDataBase.java, AmsDataGlobal.java, EmfListener.java, initialize.jsp

No database migration required — initialization-only changes.

---

## March 3, 2026 — Approved Vendors Registry (Session 22)

Added centralized BPO vendor directory on the master installation. PSP deployments query the registry to discover available BPOs and connect via the existing partnership handshake.

### Key Changes

- **V032 Migration** — `approved_vendors` table (vendor_id, vendor_name, vendor_url, description, is_active, date_added) with unique constraint on vendor_url. Table exists on all deployments, only populated on master.
- **ApprovedVendor.java** (new) — JPA entity for `approved_vendors` table. Auto-discovered via `exclude-unlisted-classes=false` in persistence.xml.
- **ApiClient.java** — Added `redirectClient` (follows 301/302, 5s timeout) and `getJson()` method for non-authenticated GET requests. Returns `ApiResponse` with statusCode=-1 on failure.
- **VendorRegistryApi.java** (new) — `@WebServlet("/api/v1/registry/vendors")`, public GET endpoint returning JSON array of active vendors. Returns 405 for non-GET methods.
- **ApiTokenFilter.java** — Added bypass for `/api/v1/registry/` path prefix (public endpoint, no auth required).
- **DatabaseInitializer.java** — Added `MASTER_REGISTRY_URL` constant to `addPspConstants()` (PSP only, defaults to `https://superiorstate.biz`).
- **VendorRegistryService.java** (new) — Static utility that fetches approved vendors from the master registry via `ApiClient.getJson()`. Returns `null` on failure (for `registryAvailable` flag), empty list on success with no results. Guarded by `AppConfig.isPsp()`.
- **VendorManager25.java** — `doGet()` now calls `VendorRegistryService.fetchApprovedVendors()`, builds set of already-registered URLs, filters out connected vendors, and passes `registryVendors` + `registryAvailable` to JSP.
- **vendorManager25.jsp** — Replaced manual "Request New Connection" form with registry-driven "Available Vendors" card. Connect button submits to existing `requestConnection` POST handler. Falls back to manual entry form when registry is unreachable. Shows "All available vendors are already connected" when registry returns empty after filtering.

### Deployment Backlog
- **D-46:** Seed `MASTER_REGISTRY_URL` constant on existing PSP deployments
- **D-47:** Populate `approved_vendors` table on master/production with Accelergent BPO Services

### Files Changed
- **New (4):** V032 migration, ApprovedVendor.java, VendorRegistryApi.java, VendorRegistryService.java
- **Modified (7):** migration_tracker.md, schema_version_migration.sql, deployment_backlog.md, ApiClient.java, ApiTokenFilter.java, DatabaseInitializer.java, VendorManager25.java, vendorManager25.jsp

---

## March 3, 2026 — BPO Note Attachments (Session 23)

Added file attachment support to BPO task notes. BPO users can attach files when adding notes via the BPO dashboard modal. Files upload to Wasabi S3 and are served via pre-signed URLs. Cross-system mode includes attachment metadata in API callbacks so PSP deployments can display BPO-side attachments.

### Key Changes

- **V033 Migration** — `todo_note_id BIGINT NULL` FK column on `weblink` table with index. Follows the `email_id` FK pattern for email attachments.
- **ToDoNote.java** — Added `@OneToMany(mappedBy = "toDoNote") List<WebLink> webLinkList` relationship.
- **WebLink.java** — Added `@ManyToOne @JoinColumn(name = "todo_note_id") ToDoNote toDoNote` inverse side.
- **BpoCompleteTask.java** — Added `@MultipartConfig` annotation. New `uploadNoteAttachment()` helper method handles file upload to Wasabi via `StorageDAO` and persists `WebLink` with `toDoNote` FK. Both `addNote()` and `addNoteCrossSystem()` call the helper after persisting the note. Updated `callbackNoteAdded()` to include attachment metadata (7-day pre-signed URLs) using `ApiClient.postJsonObject()` for nested payloads.
- **bpoHome25.jsp** — Replaced URL-encoded AJAX with `FormData` multipart submission. Added file input with label/clear controls. `renderNotes()` now renders attachment pills (paperclip icon, SSA-colored badges) linking to pre-signed Wasabi URLs.
- **BpoGetNotes.java** — Queries use `LEFT JOIN FETCH n.webLinkList`. JSON response includes `attachments` array with `name` and `url` fields. Handles both linkType 1 (local Wasabi file, 1-hour pre-signed URL) and linkType 2 (external URL from cross-system).
- **TaskNotesApi.java** — GET response includes attachments with 7-day pre-signed URLs for cross-system access. POST handler accepts optional `attachments` array and persists as `WebLink` with linkType=2 (external URL).
- **NoteAddedCallbackApi.java** — Accepts optional `attachments` array in callback payload. Persists as `WebLink` with linkType=2, attached to the local `ToDoNote`.
- **WS7 (PSP-side note display)** — Deferred. PSP checklist views don't currently render BPO notes; attachment infrastructure is in place for when they do.

### Files Changed
- **New (1):** V033 migration script
- **Modified (9):** ToDoNote.java, WebLink.java, BpoCompleteTask.java, bpoHome25.jsp, BpoGetNotes.java, TaskNotesApi.java, NoteAddedCallbackApi.java, migration_tracker.md, schema_version_migration.sql

### Follow-up Fix: L2 Cache Eviction

After persisting a WebLink attachment in `uploadNoteAttachment()`, EclipseLink's L2 shared cache retained the ToDoNote with an empty `webLinkList`. Subsequent `LEFT JOIN FETCH` queries in `BpoGetNotes` returned the stale cached entity. Added `em.getEntityManagerFactory().getCache().evict(ToDoNote.class, note.getId())` after the WebLink commit — established EclipseLink pattern used elsewhere in the codebase.

---

## March 3, 2026 — PSP Note Viewer + Vendor Only Display Polish (Session 24)

Enabled PSP users to view and add notes on sourced/delegated tasks directly from the checklist panel. Also refined the display state for Vendor Only tasks so BPO-completed items show as normal completed rather than locked.

### WS1: BpoGetNotes PSP Compatibility (Verified)
- **BpoGetNotes.java** — Already works on both PSP and BPO deployments. No `AppConfig.isBpo()` guard. Accepts both `todoId` and `todoGuid` parameters. Handles linkType 1 (local Wasabi pre-signed URL) and linkType 2 (external URL from cross-system). No changes needed.

### WS2: Note Indicator on Sourced Tasks
- **checklistBasic25.jsp** — Added `bpo-note-indicator` span to each sourced task row (after BPO Done badge). Hidden by default; DOMContentLoaded AJAX fetches notes via `BpoGetNotes?todoId=` for each sourced task and reveals indicator with count when notes exist. Clickable — opens notes modal.

### WS3: Notes Modal + AddNoteToToDo25 Servlet
- **checklistBasic25.jsp** — Added Bootstrap modal (`#bpoNotesModal`) with:
  - Note list viewer (`pspRenderNotes()`) rendering BPO/PSP source badges, author, date, text, and attachment pills
  - Text input with "Add" button and file attachment support (same pattern as BPO dashboard)
  - `openBpoNotesModal(todoId)` — loads notes via AJAX, shows modal
  - `pspAddNoteAjax()` — FormData POST to AddNoteToToDo25, refreshes note list after success
  - File label/clear helper functions (`pspUpdateFileLabel`, `pspClearFileInput`)
- **"Notes" kebab menu item** — Added to sourced task dropdown menus as alternative modal trigger
- **AddNoteToToDo25.java** (new) — `@WebServlet("/AddNoteToToDo25")`, `@MultipartConfig`. PSP-side servlet for note+attachment submission:
  1. Persists ToDoNote with sourceType="PSP", todoGuid from the local ToDo
  2. Optional file upload to Wasabi via StorageDAO, WebLink with linkType=1
  3. L2 cache eviction after WebLink persist
  4. Cross-system callback to BPO's TaskNotesApi POST endpoint (non-fatal) when task is sourced, includes attachment metadata with 7-day pre-signed URL

### WS4: Vendor Only Display State Refinement
- **ToDoOut25.java** — Single-line change in `computeDisplayState()`:
  - Before: `this.isWhoBlocked = !this.isMyTask && !allowNonOwner && (hasOwner || isSourced);`
  - After: `this.isWhoBlocked = !this.isMyTask && !allowNonOwner && (hasOwner || isSourced) && !bpoCompleted;`
  - Effect: BPO-completed Vendor Only tasks show as normal strikethrough (not locked). Source but Verify tasks where BPO is done show "BPO Done - Verify" badge and are actionable.

### Files Changed
- **New (1):** AddNoteToToDo25.java
- **Modified (2):** checklistBasic25.jsp, ToDoOut25.java

No database changes.

---

## March 3, 2026 — Starter Packages + Healthcheck Fixes (Session 25)

Added a "Load Starter Package" feature to the Service Manager, allowing PSP admins to load pre-configured sets of ApplicationSections and fields from bundled JSON templates. Also verified and committed the rewritten healthcheck script and added D-49 through D-52 deployment backlog items from a separate healthcheck troubleshooting session.

### WS1: V034 Migration + Entity Update
- **V034__starter_package_template_key.sql** — Adds nullable `template_key VARCHAR(50)` to `applicationsection` with unique index scoped to `(template_key, psp_id)`. NULL values (manual/seeded sections) unaffected by unique constraint.
- **ApplicationSection.java** — Added `templateKey` field with getter/setter.

### WS2: JSON Package Files
- **src/main/resources/packages/** — 9 files total:
  - `package-index.json` — registry of all packages with id, name, description, file reference
  - 8 package files: `general.json`, `pretax_s125.json`, `fsa.json`, `hra.json`, `hsa.json`, `transit_parking.json`, `billing_payments.json`, `specialty.json`
- Each package defines sections with `templateKey`, name, description, scope, sortOrder, and nested fields with fieldKey, label, fieldType, required, sortOrder, helpText, selectOptions.

### WS3: PackageLoader Service
- **PackageLoader.java** (new, `data/service/`) — Static utility class:
  - `getAvailablePackages()` — reads `package-index.json`, returns `List<PackageSummary>` (record: id, name, description)
  - `loadPackage(em, packageId, psp)` — reads package JSON, persists sections+fields in single transaction, returns `PackageLoadResult` (record: sectionsLoaded, sectionsSkipped, skippedKeys)
  - Duplicate detection: JPQL count query on `templateKey + psp.id`; existing fields skipped by `em.find(ApplicationField.class, fieldKey)`

### WS4: Servlet Endpoints
- **ServiceManagerHome.java** — Added `PackageLoader.getAvailablePackages()` to request attributes for the modal dropdown.
- **ServiceManagerAction.java** — New `loadStarterPackage` case: calls `PackageLoader.loadPackage()`, sets session `flashMessage` with result summary, redirects to `?tab=section`.

### WS5: JSP Modal UI
- **serviceManager25.jsp** — Three changes:
  1. Flash message display (alert-success, auto-dismiss) after navbar import, reads/clears `flashMessage` session attribute
  2. "Load Package" button (bi-box-seam icon) in tab-tools area, visible only when Sections tab is active (JS toggles on tab switch)
  3. `#loadPackageModal` — Bootstrap modal with package select dropdown populated from `availablePackages`, form POSTs to `ServiceManagerAction` with `action=loadStarterPackage`

### Healthcheck Script Verification + Backlog
- **docs/scripts/healthcheck.sh** — Verified final version contains all fixes: `set -uo pipefail`, `get_prop()`, `run_mysql()` with LD_LIBRARY_PATH, `SELECT COUNT(*) FROM constant` init check, SMTP from `SYS_HEALTH_*` keys, no hardcoded fallbacks, no `db_constant()`.
- **D-49:** Update healthcheck + config on master VPS image
- **D-50:** Set unique hostnames on VPS boxes
- **D-51:** Configure BPO backup cron + Wasabi
- **D-52:** Reduce healthcheck error log noise
- **D-53:** Starter packages (renumbered from original D-49)

### Files Changed
- **New (11):** PackageLoader.java, V034 migration script, package-index.json, 8 package JSON files
- **Modified (9):** ApplicationSection.java, ServiceManagerHome.java, ServiceManagerAction.java, serviceManager25.jsp, healthcheck.sh, deployment_backlog.md, migration_tracker.md, schema_version_migration.sql, claude_memory.md

### Database Changes
- **V034:** `ALTER TABLE applicationsection ADD COLUMN template_key VARCHAR(50) NULL` + unique index `uq_section_template_psp (template_key, psp_id)`

---

## March 3, 2026 — Starter Package Fixes + Enhancements (Session 26)

Fixed package loading (fields not persisting, ALL-scope not linking) and added PSP-filtered dropdown, reset-to-default, and L2 cache eviction.

### Bug Fixes
- **EclipseLink merge interference:** `em.merge(section)` for ALL-scope linking within the same transaction as field creation caused fields to silently not persist. Restructured `loadPackage()` into two passes — Pass 1 creates sections+fields and commits, Pass 2 links ALL-scope sections to LOS/Enhancements in a separate transaction (matching `createAppSection` pattern).
- **L2 cache stale data:** After package load, `ServiceManagerHome` queries returned cached sections without fields. Added `emf.getCache().evict(ApplicationSection.class)` after `loadStarterPackage` in `ServiceManagerAction`.
- **Field suppressed flag:** Ensured `field.setSuppressed(false)` set explicitly on all new fields during load.

### Enhancements
- **`getAvailablePackagesForPsp(em, pspId)`** — Filters dropdown to hide fully-loaded packages. Counts existing templateKeys per PSP vs package sections; hides package when all sections already exist.
- **`resetSectionToDefault(em, section)`** — Restores package-loaded section and fields to JSON template defaults. Existing fields: properties restored, un-suppressed. Missing fields: created. Extra manually-added fields: suppressed. Section name/description/scope/sortOrder restored.
- **`resetSectionToDefault` servlet action** — New case in `ServiceManagerAction`, flash message with field count, L2 cache eviction, redirect to section tab.
- **JSP changes:** Load Package button wrapped in `<c:if test="${not empty availablePackages}">` (hidden when all loaded). "Reset to Default" button in section detail header for package-loaded sections only (`templateKey != null`), with confirm dialog.
- **Suppress-as-delete audit:** Confirmed no `em.remove()` calls exist for ApplicationField or ApplicationSection — only suppress toggle. Safe for reset-to-default pattern.

### Files Changed
- **Modified (3):** PackageLoader.java, ServiceManagerAction.java, serviceManager25.jsp

No database changes (uses V034 from Session 25).

---

## March 3, 2026 — Proposal Customization (Session 27)

Added composable proposal layout with PSP admin editor and feature sales blurb upgrade.

### Phase 1: Feature Sales Blurb Upgrade
- **V035:** Added `headline VARCHAR(200)` to feature table, widened `description` from VARCHAR(500) to VARCHAR(2000). Headline shows as bold text above the description in proposal rendering.
- **Feature.java:** New `headline` field with getter/setter, `description` column definition updated.
- **Service Manager UI:** Add/Edit Feature modals updated — new Headline input, Description textarea widened to 4 rows with 2000 char limit, labels clarified with helper text.
- **ServiceManagerAction:** `createFeature` and `editFeature` cases read/persist headline. Edit case clears headline to null when blank.
- **viewProposal.jsp:** Two-tier feature rendering — bold headline above muted description text. Falls back gracefully when no headline set.

### Phase 2: Proposal Section Architecture
- **V036:** Created `proposal_section` table (section_id, psp_id FK→assignee, section_type, title, html_content TEXT, sort_order, is_active, timestamps). FK targets `assignee(id)` due to single-table inheritance (PSP extends Assignee).
- **ProposalSection.java:** New JPA entity in `model/sales/offering/`.
- **ProposalSettings.java:** New servlet with full CRUD — `saveContent` (CKEditor HTML with sanitization), `createCustom` (inserts before CLOSING), `deleteCustom`, `reorder` (AJAX SortableJS), `toggleActive`, `renameSection`. Auto-initializes 4 default sections (TITLE, FEATURES, PRICING, CLOSING) on first access.
- **proposalSettings.jsp:** Two-panel admin page — left panel: drag-drop section list with type icons and badges; right panel: CKEditor 5 Classic (v36.0.1) with HTML source toggle, merge token reference panel. Modal for adding custom pages.
- **HTML sanitization:** Regex-based `sanitizeHtml()` strips `<script>`, `on*` event handlers, and `javascript:` protocols.
- **Merge tokens:** 10 tokens (`{{PROSPECT_NAME}}`, `{{AGENT_NAME}}`, `{{AGENT_EMAIL}}`, `{{AGENCY_NAME}}`, `{{PSP_NAME}}`, `{{DATE_CREATED}}`, `{{PRIMARY_COLOR}}`, `{{ACCENT_COLOR}}`, `{{APPLY_BUTTON}}`, `{{PROPOSAL_ID}}`) replaced at render time in ViewProposal.java.
- **ViewProposal.java:** Loads active ProposalSections for the proposal's PSP, builds token map from proposal data, replaces tokens in HTML content, passes `proposalSections` list and `sectionHtml` map to JSP.
- **viewProposal.jsp:** Section-based rendering loop when `proposalSections` exist — dispatches TITLE/CLOSING/CUSTOM to rendered HTML, FEATURES to `proposalFeatures.jsp` include, PRICING to `proposalPricing.jsp` include. Falls back to legacy hardcoded layout when no sections configured.
- **Extracted includes:** `proposalFeatures.jsp` (LOS cards + Enhancement cards), `proposalPricing.jsp` (rate table) — shared between section-based and legacy rendering paths.

### Phase 3: Navigation
- **navbar25.jsp:** "Proposal Settings" link added to Admin dropdown (between Resource Library and Sequence Builder divider), `bi-sliders` icon.

### Backlog Items
- **D-54:** Apply V035+V036 migrations and browser-test Proposal Settings page

### Files Changed
- **New (5):** V035 migration, V036 migration, ProposalSection.java, ProposalSettings.java, proposalSettings.jsp, proposalFeatures.jsp, proposalPricing.jsp
- **Modified (8):** Feature.java, ServiceManagerAction.java, ViewProposal.java, viewProposal.jsp, serviceManager25.jsp, navbar25.jsp, migration_tracker.md, schema_version_migration.sql

### Database Changes
- **V035:** `ALTER TABLE feature ADD COLUMN headline VARCHAR(200)`, `ALTER TABLE feature MODIFY COLUMN description VARCHAR(2000)`
- **V036:** `CREATE TABLE proposal_section` (FK to assignee table)

---

## March 3, 2026 — Full-Height Dashboard Layouts + Dropdown Fix (Session 28)

Made the BPO Dashboard and PSP Dashboard fill the full viewport height on desktop (≥992px) instead of scrolling the entire page. Also fixed kebab dropdown menus getting clipped inside scrollable containers.

### BPO Dashboard (`bpoHome25.jsp`)
- **CSS media query** `@media (min-width: 992px)` — Added flex layout classes: `.bpo-layout` (flex column, `height: calc(100vh - 70px)`, overflow hidden), `.bpo-columns` (flex child fills remaining height), `.bpo-col-left` / `.bpo-col-right` (flex columns with cards stretching to fill and card-body scrolling internally).
- **HTML structure** — Wrapped content in `<div class="bpo-layout">`, added `bpo-columns` to the row, `bpo-col-left` to the left column (My Checklists), `bpo-col-right` to the right column (Delegated Tasks).
- Both columns now scroll internally; page body no longer scrolls on desktop.

### PSP Dashboard (`pspDashboard25.jsp`)
- **CSS media query** `@media (min-width: 992px)` — Added flex layout classes: `.psp-dash-body` (flex column, `height: calc(100vh - 70px)`, overflow hidden), `.psp-dash-col-left` / `.psp-dash-col-right` (flex columns). Left column's card and `.dash-scroll` fill available height. Right column's first card (Team Workload) flexes to fill.
- **HTML structure** — Wrapped header + stat cards + filter bar + two-column row in `<div class="psp-dash-body">`, added `psp-dash-col-left` to `col-lg-8`, `psp-dash-col-right` to `col-lg-4`.
- **Removed inline max-heights** from all four `.dash-scroll` divs (600px, 280px, 200px, 200px) — flex layout now controls sizing.

### Kebab Dropdown Clipping Fix (`toDoCurrentList25.jsp`)
- **Problem:** With the new `overflow-y: auto` on scrollable card bodies, Bootstrap dropdown menus from kebab buttons on checklist items rendered inside the scroll container and were clipped. A tiny scrollbar appeared instead of the menu being visible.
- **Fix:** Pre-initialized all kebab dropdowns inside `.todo-current` with `new bootstrap.Dropdown(el, { popperConfig: { strategy: 'fixed' } })`. The `fixed` strategy tells Popper.js to position the menu relative to the viewport rather than the overflow ancestor, preventing clipping.

### Files Changed
- **Modified (3):** bpoHome25.jsp, pspDashboard25.jsp, toDoCurrentList25.jsp

No database changes.
