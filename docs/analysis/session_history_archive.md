# Session History Archive

> **Purpose:** Consolidated historical record of all build sessions. For current project state, see `project_backlog.md`. For current architecture, see `application_flow.md` and `entity_reference.md`.
>
> **Last Updated:** February 25, 2026

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
- **Pending production deploy** — see chatbot deployment checklist in `project_backlog.md`

Key decisions: live ticket KB over static export, legacy cutoff date (2026-02-19), resolution flag on Note, Gson for JSON, KnowledgeSearchService in application scope.

**Schema changes (not yet a numbered migration):**
```sql
ALTER TABLE note ADD COLUMN is_resolution TINYINT(1) NOT NULL DEFAULT 0;
INSERT INTO constant (name, value, note) VALUES ('ANTHROPIC_API_KEY', '<key>', 'Claude API key for chatbot');
-- Ticket category updates: 9 expired, 9 updated, 7 new service-oriented categories
```

---

## February 19–20, 2026 — Sales Pipeline (Sessions 1–4)

**Living reference:** `sales_pipeline_reference.md` (kept separately)

Built the complete sales pipeline over 4 sessions:

**Session 1:** Proposal/Application entity redesign (DataKey/DataPair → ApplicationField/ApplicationFieldValue), Feature/RateDiscount/MarketingMaterial entities, ProposalBuilder + ProposalDetail servlets, `sales_pipeline_migration.sql`

**Session 2:** SendProposal (email with GUID), ViewProposal (public landing page), Proposal.sourceActivity FK, `sales_pipeline_migration_2.sql`

**Session 3:** LOS expansion (IDs 11–19), ApplicationSection entity + 20 seeded sections with LOS scoping, IrsLimit/BenefitType/BillingType entities, ApplyForProposal (dynamic form), SaveApplicationProgress (AJAX auto-save), UploadRateSheet (Wasabi), `sales_pipeline_migration_3.sql`

**Session 4:** ReviewApplications + ReviewApplication servlets (list + detail with approve/deny/more-info), automated Setup + CheckList + ToDo creation on approval, manualSetup.jsp for GenerateProp25, full end-to-end pipeline test passed.

Key decisions: Application uses Proposal as PK (not generated Long), `LEFT JOIN FETCH p.application` required to avoid lazy-load bugs, EclipseLink DISTINCT + JOIN FETCH scrambles @OrderBy (workaround: Java sort after query).

---

## February 20, 2026 — Service Manager (Session 1)

Built the Service Manager for configuring Lines of Service, Enhancements, and Application Sections:

- Created `Enhancement` entity with M:N to LOS
- Added `sortOrder` and `suppressed` columns to LOS
- Added nullable `los` and `enhancement` direct FKs to ServiceModule
- Created `ServiceManagerHome` (GET) + `ServiceManagerAction` (POST, 14 actions) + `ServiceManagerSort` (AJAX reorder)
- Created `serviceManager25.jsp` — tabbed Services/Enhancements with detail panel
- `service_manager_production_migration.sql` created

---

## February 20, 2026 — Rate Manager (Session 1)

Built the Rate Manager UI for rate configuration:

- Created `PspAdminHome` servlet — Rate Manager home (rates, fee types, modules, agencies, locked rate detection)
- Created `RateTableAction` servlet — Rate CRUD (createRate, editRate, addRateTableRow, deleteRow, assignAgency, removeAgency, createPriceItem, cloneRate)
- Created `PriceItemAction` / `ServiceModuleAction` — AJAX reorder and suppress toggle
- Created `rateManager25.jsp` — 4-tab left panel (Rates, Fee Types, Modules, Agencies), right panel pricing grid
- Rate locking: rates with active proposals cannot have pricing modified (only cloned)

---

## February 20, 2026 — Agency Manager + Rate Manager Session 2

Built Agency Manager and enhanced Rate Manager:

- Created `PspAgencyHome` servlet + `agencyManager25.jsp` — Agency list, agent management, rate assignment
- Created `AgencyAction` servlet — Agency CRUD (createAgency, editAgency, addAgent, removeAgent, removeRate)
- **Rate Manager enhancements:** Add-pricing-row reworked to use LOS/Enhancement selection (hides ServiceModule abstraction), per-rate sort ordering (`ratetable.sort_order`), inline AJAX price editing, rate copying ("Make New From"), Grid Sort tab for per-rate drag reorder
- **Proposal rendering updates:** `SalesDAO.getPricing()` rewritten as two-query approach (LOS-linked + Enhancement-linked, merged), ViewProposal renders Enhancement feature cards, pricing headers show LOS/Enhancement names
- `rate_manager_session2_production_migration.sql` created

Key decisions: LOS/Enhancement selection in add-row modal hides ServiceModule abstraction from user. Per-rate sort order column on `ratetable` (not just ServiceModule sort). Two-query approach for getPricing() because single JPQL with OR/subquery failed in EclipseLink. `cloneRate` vs `copyRate`: clone moves agencies + suppresses original (for locked rates), copy just duplicates pricing (for convenience).

---

## February 20, 2026 — Service Manager Session 2

Enhanced Service Manager with suppress fix and SortableJS improvements:

- Fixed suppress buttons not rendering (cached JSP issue resolved)
- Added SortableJS drag-and-drop reordering for LOS list, Enhancement list, and Application Sections
- `ServiceManagerSort` servlet handles all three sort types via AJAX

---

## February 21, 2026 — Resource Library + Feature Rendering

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

Remaining items (tracked in backlog): PspAgencyHome scoping (T10), layout/appearance consolidation (T11).

---

## February 21, 2026 — Layout & Navigation Consolidation

Unified the navbar, layout, and CSS across all main authenticated pages:

- Rewrote `navbar25.jsp` as single navigation component for all roles (PSP User, PSP Admin, Agent, Agency Manager)
- Dark branded bar (`#0d5681`) with role-aware menu items, collapses to hamburger on mobile
- Eliminated standalone `adminNav.jsp` dropdown — absorbed into navbar's Admin dropdown
- All admin pages (Service Manager, Rate Manager, Agency Manager, Library, Sequence Builder) converted to use unified navbar
- Agent pages (AgentHome) converted to use unified navbar
- Consistent `pageTitle`/`pageIcon` request attributes across all pages
- Chatbot gated to PSP users only

---

## February 21, 2026 — Create Ticket Form Overhaul

Rebuilt the Create Ticket modal from a basic datalist form into a modern typeahead system:

- **Person typeahead:** Replaced `<datalist>` with custom JS dropdown — filters on name/employer/email, shows styled badge on selection, comma-tolerant search (`murphy, k` works), hidden `employeeId` field for direct lookup
- **Person resolution chain in servlet:** employeeId → direct lookup; else freeform text → email path (employee by email → person by email → create from email) → name path (employee by name → person by name → create from name). Handles single-word names, email-to-name parsing.
- **Grouped reason dropdown:** Client-side JS regroups flat `<option>` list into `<optgroup>` by TicketCategory. No backend change.
- **Removed dead UI:** Contact method dropdown (hardcoded, never read), empty `getName()` function
- **Servlet stale state fix:** All instance variables nulled at top of each request (servlets are singletons)
- **Immutable list fix:** `AmsDataLocal.respondToActivityUpdate()` ADD_TICKET case — switched from direct `.add()` to mutable copy pattern (pre-existing bug)
- **Sequence suppress toggle:** Added "Hide from Dropdown" / "Restore to Dropdown" button in Sequence Manager for ticket sequences. Toggles `TicketSubCategory.isActive`, refreshes global cache.
- No database changes required.

---

## February 22, 2026 — Timeclock Redesign + Correction Workflow

Redesigned the ViewHome25 timeclock column and built a time correction request workflow:

- **UI Redesign:** Replaced flat date/in/out rows with two-tab layout: Today (day selector, stretch timeline with progress bars, per-stretch durations, pulsing active dot) and Week (horizontal bar chart per day with 8h marker, overtime coloring, avg/day + remaining stats)
- **DaySummary DTO** (`model/general/`) — aggregates stretches per day with computed totals, overtime flag, progress percentage
- **TimeStretch enhanced** with `inLogId`/`outLogId`, fixed `getMinutesWorked()` bug (uses `Duration.between()` instead of broken `compareTo`), added `getMinutesFormatted()` and `isComplete()`
- **TimeCorrectionRequest entity** (`model/general/`) — PENDING/APPROVED/DENIED workflow. References TimeLog records via FK. Snapshots original values. Nullable requested times.
- **SubmitTimeCorrection servlet** (`controller/user/`) — employee modal form submission
- **timeCorrectionModal.jsp** — Bootstrap modal with time input validation (in < out, no overlap with adjacent stretches), boundary hints
- **ReviewTimeCorrections servlet + JSP** (`controller/user/`) — admin review page with filter tabs, approve/deny with comment, auto-updates TimeLog on approve
- **Correction status badges** on stretch timeline (Pending=orange, Approved=green, Denied=red) via `correctionMap` loaded in ViewHome25
- **Navbar:** Added "Time Corrections" link to Admin dropdown
- **Bug fixes:** TimeClock25 forward→redirect, AuthenticateUser time init on login + forward→redirect, clock state correct on relogin
- **Migration:** `timeclock_correction_migration.sql` (#9) — `time_correction_request` table with FKs to assignee and timelog

---

## February 22, 2026 — Activity List Modernization + PSP Opportunity Integration

Modernized the ViewHome25 activity list (center column) with SSA branding and integrated Opportunity tracking for PSP users:

### Activity Column Modernization
- **activityHeader25.jsp:** Replaced old styled block with `.hdr-bar` pattern. Quick-view buttons (ALL/MY/REN) on left, filter toggle on right. Filter panel: TYPE toggles (R/S/T/O), ATTENTION toggles (On Us / Needs Contact), OWNER radios, SORT radios, branded Apply button
- **activityList25.jsp:** Replaced `input-group` strips with `.act-card` flex rows. Left border colored by due bucket (red=overdue, orange=warning). Compact type badge pills (R/S/T/O). Urgency icons (waiting-on-us, needs-contact). Opportunity stage badge. Due date with color-coded urgency. Empty state message.

### PSP Opportunity Integration
- **New Role: PSP Sales** (UserRole ID 9) — gates opportunity visibility for PSP users
- **managed_by_id** FK on assignee — tracks which PSP user manages an opportunity
- **ActivityListDAO updates:** `loadActivities()` includes Opportunities for PSP Sales/Admin users. Opportunities managed by current user flagged as "mine". Quick-view ALL/MY includes managed opportunities.
- **ActivityFilter updates:** New `filterOpportunity` toggle, Opportunity stage badge in list rows
- **Migration:** `V010__psp_opportunity_integration.sql` — PSP Sales role seed + managed_by_id column

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
## February 25, 2026 — Track A Polish + Navbar Restyle + Email Screens

Completed Track A polish items A13, A14, S4 and restyled the navbar and email viewing screens.

### Track A Polish (A13, A14, S4)
- **A13 — Closed Activity Banner:** Restyled from yellow warning to muted gray archived feel. Background `#f0f0f0`, border `#ccc`, text `#6c757d`, lighter lock icon. Added "by [FirstName]" when completedBy is available.
- **A14 — Auto-Save UX Indicator:** Added amber "Unsaved" dot next to Save button in Quill note editor. Appears on `text-change` when content exists, clears when editor empty or on form submit (page reload). Purely visual — no server-side auto-save.
- **S4 — pe-none Audit:** Full audit of all activity detail panels confirmed all interactive elements are already gated for closed activities. Mix of `pe-none` class (`isPast`/`penone` variables) and `c:if isComplete()==false` (hidden entirely). No changes needed.

### Navbar Restyle (`navbar25.jsp`)
- **Ghost-style nav links** replacing `btn btn-sm btn-outline-light` — new `.nav-ghost` class with no borders, subtle hover highlight (`rgba(255,255,255,0.13)`), modern clean feel
- **Taller padding** (`0.35rem` → `0.55rem`)
- **Bottom radius** (`border-radius: 0 0 8px 8px`) — connects visually with rounded `.hdr-bar` headers below
- **Dropped full-bleed hack** — removed `margin-left: calc(-50vw + 50%); width: 100vw;` so navbar respects container padding
- **Admin warm tone** — `.nav-ghost-warn` (amber tint) distinguishes admin from regular nav
- **Logout dimmed** — `.nav-ghost-logout` at lower opacity
- **Thin dividers** — `.nav-divider` (1px vertical line) between groups on desktop
- **Dropdown menus** — smaller font (0.82rem) consistent with nav items
- **Unauthenticated state** — transparent background (no dark bar) for login/initialize pages, SSA-blue ghost button
- Hamburger border removed for cleaner mobile toggle

### Email Screens Modernization
- **ViewEmail servlet** — Rewritten with fetch-join query (recipients + weblinks in one JPQL), force-init of lazy fields while EM open, request attributes instead of session pollution, input validation, forwards to new JSP
- **emailView25.jsp** (new) — SSA card with `hdr-bar` header showing subject + timestamp. Clean metadata rows (From, To with chip-style recipients, Files with chip-style download links). Email body in padded area. No jQuery, no old sub-JSPs.
- **ViewEmailHistory servlet** (new/replace) — Uses `EmailDAO.getEmailsToRecipient()`, force-inits lazy fields, request attributes (`emailHistoryList`, `emailHistoryAddress`, `emailHistoryCount`)
- **emailHistoryList25.jsp** (new) — SSA card with `hdr-bar` showing email address + count. Compact column headers (Date/From/Subject). Clickable rows open `ViewEmail?id=` in new tab. Hover highlight, truncating text, scroll at 600px.

Both email screens use `navbar25.jsp` with page titles. Old JSPs (`emailView.jsp`, `emailList.jsp`) and old sub-JSPs (`toWhoList2.jsp`, `attachmentList2.jsp`) no longer referenced by the new servlets.

### Track A Final Status
A1–A14 and S4 complete. Remaining: S3 (questionnaire placeholder — Track B dependency), S5 (mobile stacking polish).

New files: `emailView25.jsp`, `emailHistoryList25.jsp`, `ViewEmailHistory.java`
Modified files: `ViewEmail.java`, `navbar25.jsp`, `detailHeader25.jsp`, `detailAddNote25.jsp`
No database changes.