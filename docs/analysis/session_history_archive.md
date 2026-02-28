# Session History Archive

> **Purpose:** Consolidated historical record of all build sessions. For current project state, see `project_backlog.md`. For current architecture, see `application_flow.md` and `entity_reference.md`.
>
> **Last Updated:** February 28, 2026

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

### Git / Infrastructure
- `.gitignore` updated: added `/out/` (IntelliJ artifact output) and `.claude/` (Claude Code metadata)
- Removed stale `.claude/worktrees/` entries from git tracking
