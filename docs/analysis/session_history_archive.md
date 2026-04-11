# Session History Archive

> **Purpose:** Consolidated historical record of all build sessions. For current project state, see `project_backlog.md`. For current architecture, see `application_flow.md` and `entity_reference.md`.
>
> **Last Updated:** April 11, 2026 (Session 77)
>
> **Note:** Sessions 1–38 (Feb 15 – Mar 5) were compressed during the Session 69 cleanup. Full details for those sessions are available in git history prior to that commit.

---

## Sessions 1–38 Summary (February 15 – March 5, 2026)

### Foundation Period (Sessions 1–5, Feb 15–17)
- **Code cleanup:** 238 files deleted, 32 cryptic classes renamed, `previous/` package tree eliminated
- **Performance:** EMF reuse (shared across sessions), pre-computed ToDo display state in `ToDoOut25`

### Core Features (Sessions 6–11, Feb 18–28)
- **Email system:** Graph API removed, SMTP via EmailDAO, Wasabi S3 storage, branded HTML templates
- **Sequence builder:** New drag-and-drop UI (`SequenceBuilder25`, `SequenceAction25`, `sequenceManager25.jsp`)
- **AI chatbot:** Knowledge assistant with 5 JSON KBs, Claude Haiku 4.5, ticket resolution search (V014)
- **Sales pipeline:** Full pipeline over 4 sessions — Proposal/Application entities, ProposalBuilder, SendProposal, ViewProposal (public GUID), ApplyForProposal, ReviewApplication with auto-Setup creation (V001–V003)
- **Admin toolset:** Service Manager (LOS/Enhancement/ServiceModule CRUD), Rate Manager, Agency Manager, Invitation System (V004–V006)
- **Resource library:** Wasabi upload/download, category management, feature linking (V007)
- **Opportunity system:** Stage management, agent pipeline, PSP Sales role (V008, V010)
- **Time tracking:** Daily/weekly views, correction request workflow (V009)
- **Summit import wizard:** Multi-step CSV import for Plan Types, Employers, Employees, Benefits (V025–V026)
- **Benefit renewal audit:** Plan year tracking, detected renewal dates, inline corrections (V028)

### GUI Modernization — Track A (Sessions 12–19, Feb 23–Mar 2)
- **Activity detail page:** Full rewrite — SSA headers, type-specific panels, Quill editor, resizable three-panel layout, Wasabi document upload
- **Checklist panel:** Restructured layout, automation integration, add task modal
- **Task manager:** Two-column rewrite with Quill email editor
- **Email screens:** New emailView25.jsp, emailHistoryList25.jsp, emailMaster25.jsp modernized
- **Navbar:** Ghost buttons, consolidated Sales dropdown, role-filtered menus
- **Track A items A1–A14 + S4 complete.** S5 (mobile polish) remaining.

### Deployment Infrastructure (Sessions 12–13, Feb 23)
- **Multi-PSP architecture:** AppConfig properties, InitializeDataBase with deployment key, schema_version tracking
- **Automation scripts:** backup.sh (mysqldump → Wasabi), update.sh (GitHub Releases → SQL → WAR), healthcheck.sh
- **Master VPS:** IONOS Cloud Ubuntu 24.04, snapshot v4
- **Living references:** `deployment_strategy.md`, `deployment_runbook.md`, `deployment_backlog.md`

### BPO System (Sessions 20–24, Mar 2–3)
- **Cross-system architecture:** Push/pull API, note sync, file attachments, vendor registry, partnership management (V027, V032, V033)
- **BPO initialization path:** D-45, co-located and federated modes
- **PSP note viewer:** BPO notes modal in checklistBasic25.jsp, AddNoteToToDo25 with cross-system callback
- **Living reference:** `bpo_feature_session_history.md`

### ServiceItem Unification (Sessions 14–18, Feb 27)
- **6-phase project:** TemplatePurpose → ServiceItem, TicketSubCategory eliminated, unified activity item → task sequence model (V020–V024)
- **Living reference:** `serviceitem_unification_design_v2.md`

### Sales & Proposal Enhancements (Sessions 25–31, Mar 3–4)
- **Starter packages:** 8 JSON templates for ApplicationSections, PackageLoader (V034)
- **Proposal customization:** Composable sections, CKEditor 5, merge tokens, section scoping (V035–V037)
- **Full-height dashboard layouts:** PSP + BPO flex layouts with internal scroll columns
- **BPO/PSP communication fixes:** V038 — vendor auto-complete, display state priority, PSP filter, sort order

### Questionnaire System (Sessions 32–33, Mar 4)
- **Phases 1–6:** 4 entities, dual-mode (native + external/Jotform), auto-attach, activity detail card, public form, webhook API (V039)
- **Living reference:** `questionnaire_system_design.md`

### Demo & Polish (Sessions 34–38, Mar 5)
- **Demo data seeder:** DemoDataSeeder.java with conference demo data, factory reset servlets (SeedDemoData, ReSeedDb, ReSeedDemoData)
- **Agent home Kanban:** Horizontal board with 6 stage columns, slide-out detail drawer, inline field saving
- **Application visibility:** Role walls, ApplicationsHome, reviewer tracking (V041)
- **Proposal settings:** Raw HTML paste for TITLE/CLOSING, feature display fix
- **Tonal zone styling:** Two-tone layout across PSP home, activity detail, admin pages
- **User manager fixes:** FormData→URLSearchParams, Friendly Names toggle, sequence collision fix

### Migrations Applied (Sessions 1–38)
V001–V024 on all environments. V025–V037 on Demo/BPO/Master. V038 on Demo/BPO. Production at V024.

---

## March 5, 2026 — Session 39: Recurring Checklist History + InitializeDataBase Fix + Agent Pipeline Enhancements

Three feature areas implemented: recurring checklist history tracking (V040), a fresh-database NPE fix, and two agent pipeline enhancements (closed opportunity lookup + application CSV export).

### Recurring Checklist History (V040)

Full history tracking for recurring checklist series — tracks each cycle's completion with details.

- **V040 migration** (`V040__delegated_todo_recurring_series.sql`) — Added `recurring_series_id` VARCHAR(36) and `recurring_cycle_number` INT to `delegated_todo` table. Index on `recurring_series_id`.
- **DelegatedToDo.java** — Added `recurringSeriesId` (UUID string) and `recurringCycleNumber` (Integer) fields with JPA annotations.
- **BpoTaskPushService.java** — Copies `recurringSeriesId` and `recurringCycleNumber` from source checklist's first task when pushing recurring instances. Increments cycle number for each new recurring cycle.
- **RecurringChecklistDAO.java** (new) — DAO with `getRecurringHistory()` query: loads past cycles by series ID, returns cycle number, checklist name, completion date, completed-by person name, and task count per cycle.
- **ViewRecurringHistory25.java** (new) — PSP-side servlet at `/ViewRecurringHistory25`, loads history for a given `seriesId` parameter, forwards to JSP.
- **BpoRecurringHistory.java** (new) — BPO-side servlet at `/BpoRecurringHistory`, same pattern as PSP side.
- **checklistHistory25.jsp** (new) — Shared JSP for both PSP and BPO, displays history table with cycle number, checklist name (linked), completion date, completed by, and task count. Uses `.audit-wrap` flex layout pattern.
- **activityDetail25.jsp** — Added "View History" link button on recurring checklists, visible when `recurringSeriesId` is present on the first delegated task.
- **bpoHome25.jsp** — Added "History" link in BPO checklist kebab menu for recurring checklists.
- **TaskReceiveApi.java** — Sets `recurringSeriesId` and `recurringCycleNumber` on delegated tasks received via BPO push API.

### InitializeDataBase NPE Fix

- **InitializeDataBase.java** — Added null guard around `createApplicationSections()` call. On fresh databases, `AmsDataGlobal.losMap` may be empty before full initialization completes, causing NPE when the initializer tries to assign sections to LOS. Guard skips section assignment if LOS map is empty (sections get assigned on next startup after LOS data exists).

### Agent Pipeline: Closed Opportunity Lookup

Collapsible section below the Kanban board showing WON/LOST opportunities with client-side filtering.

- **SalesDAO.java** — Added `getClosedOpportunitiesByAgency()` and `getClosedOpportunitiesByAgent()` static methods (for future reuse; current implementation filters from existing loaded list).
- **AgentHome.java** — Added `prop.getApplication().getStatus()` to lazy-load touch loop. Added closed opportunity extraction via stream filter from already-loaded opportunities list. Sets `closedOpportunityList`, `closedCount` request attributes.
- **agentHome25.jsp** — Added CSS for `.closed-toggle`, `.closed-panel`, `.closed-filters`, `.closed-table-wrap` with sticky headers. HTML: toggle bar with archive icon, count badge, chevron; collapsible panel with text search, WON/LOST dropdown, agent filter (admin only); table with Prospect (linked), Outcome badge (green WON/red LOST), Agent (admin only), Close Date, Proposals count, Export CSV icon. JS: `toggleClosed()` and `filterClosed()` functions. Added `hasApp` flag to OPPS JS data map.

### Agent Pipeline: Application CSV Export

New servlet for exporting application field data as CSV, integrated into both opportunity detail and agent pipeline pages.

- **ExportApplicationCsv.java** (new) — Servlet at `/ExportApplicationCsv`, accepts `proposalId` or `opportunityId` via GET. Auth via AmsDataLocal session check. Loads proposals with applications, builds master field list from ApplicationField sorted by section/field sortOrder. Streams CSV with 14 metadata columns (Prospect Name, Contact First/Last/Email/Phone, Agency Name, Agent Name, Proposal ID/Status/Created Date, Application Status/Submitted Date, Services pipe-delimited, Rate Name) plus one column per ApplicationField. Contact info falls back from prospect contact to opportunity primary contact.
- **detailOpportunity25.jsp** — Added `oppHasApp` check via JSTL forEach. Added conditional CSV export icon button (bi-filetype-csv) in proposals card header when any proposal has an application.

### Bug Fix: PSP.getId() Returns Long

- **ExportApplicationCsv.java** — Fixed `int pspId` to `long pspId` and updated `loadMasterFields()` parameter type to match. `PSP.getId()` returns `Long`, not `int`.

### Files Changed
- **New (6):** V040 migration, RecurringChecklistDAO.java, ViewRecurringHistory25.java, BpoRecurringHistory.java, checklistHistory25.jsp, ExportApplicationCsv.java
- **Modified (10):** DelegatedToDo.java, BpoTaskPushService.java, TaskReceiveApi.java, activityDetail25.jsp, bpoHome25.jsp, InitializeDataBase.java, SalesDAO.java, AgentHome.java, agentHome25.jsp, detailOpportunity25.jsp

Database changes: V040 (delegated_todo recurring series columns).

---

## March 5, 2026 — Session 40: Tonal Zone Styling + Layout Modernization

Applied three visual design prompts across multiple pages: layout/styling updates, PSP home tonal zones, and activity detail tonal zones.

### Prompt 1: Layout & Styling Updates

- **proposalBuilder.jsp** — Max-width wrapper, SSA brand colors, `.hdr-bar` header, filter pills
- **upcomingRenewals25.jsp** — Same pattern: `.hdr-bar` header, brand colors, filter pills
- **reviewApplications.jsp** — Same pattern: `.hdr-bar` header, brand colors, consistent layout

### Prompt 2: PSP Home Tonal Zones

- **pspHome25.jsp** — Three-column tonal color zones: amber timeclock (left), white activities (center), teal checklists (right). Responsive reset on mobile.

### Prompt 3: Activity Detail Tonal Zones (9 changes)

Major visual overhaul of the three-panel activity detail layout:

1. **Page background** — `body { background-color: #eef0f4; }`
2. **Breadcrumb bar** — Back navigation + activity name + overdue urgency indicator (days overdue in red). Computed via `daysUntilDue` request attribute added to `ViewActivity25.java`.
3. **Three-panel tonal zones** — Steel blue-grey left (tasks, `#e8eef5`), white center (reference data), warm cream right (communication, `#fffdf7`). Custom `.hdr-bar` overrides per panel. Responsive reset to white + SSA blue on mobile.
4. **Zone classes + stripe divs** — `.detail-panel-left`, `.detail-panel-center`, `.detail-panel-right` classes on panel elements. 3px accent stripes at panel tops.
5. **Inline status bar** — Owner, due date (with urgency coloring), and assignee displayed below the detail header in the center panel.
6. **Center column card reorder** — Header → status bar → primary contact → type-specific detail → questionnaires → docs/links → additional contacts → footer.
7. **Note compose always visible** — Removed collapse toggle from Add Note card. Quill editor always shows.
8. **BPO/sourced task badges** — `.sourced` class on BPO task rows, new `.bpo-badge` (blue) and `.bpo-done-badge` (green) replacing old Bootstrap badges.
9. **Sticky history header** — `position: sticky; top: 0` on history section header.

### Section Header Conversions (Center Column)

Converted 4 center-column cards from Bootstrap card pattern to unified `.detail-section-card` + `.detail-section-header` pattern:
- `detailPrimaryContact25.jsp`, `detailAdditionalContacts25.jsp`, `detailDocsLinks25.jsp`, `detailQuestionnaires25.jsp`

### Bug Fix: Right Panel Wrapping

- **Root cause:** Extra `</div>` in `detailAdditionalContacts25.jsp` introduced during card-to-section-card conversion. The extra close tag prematurely closed `#panelCenter`, pushing `dividerRight` and `#panelRight` outside the `#actLayout` flex container.
- **Fix:** Removed the extra `</div>` to balance 3 opens with 3 closes.

### Add Note Header Readability Fix

- **detailAddNote25.jsp** — Removed `text-white` class from header text and changed `.note-dd select` styles from white text/borders (designed for dark blue `.hdr-bar`) to dark brown text (`#92400e`) with warm-toned borders (`#e5d5b0`) to match the right panel's amber theme.

### Files Changed
- **Modified (1 Java):** ViewActivity25.java (daysUntilDue request attribute)
- **Modified (12 JSP):** activityDetail25.jsp, checklistBasic25.jsp, detailAddNote25.jsp, detailAdditionalContacts25.jsp, detailDocsLinks25.jsp, detailPrimaryContact25.jsp, detailQuestionnaires25.jsp, historyHeader.jsp, pspHome25.jsp, upcomingRenewals25.jsp, proposalBuilder.jsp, reviewApplications.jsp

No database changes.

---

## March 5, 2026 — Session 41: Application Visibility & Role Walls (V041)

Implemented role-based application visibility and review controls. PSP Users/Admins get a new Applications hub page; Agents can view applications in read-only mode. Review actions (approve/deny/more info) are gated to PSP Admins only.

### V041 Migration
- **`V041__application_reviewer_fields.sql`** — Adds `reviewed_by` (BIGINT FK → assignee), `review_notes` (TEXT), `date_reviewed` (TIMESTAMP) to application table. Uses conditional DDL via `information_schema.COLUMNS` checks for idempotency.

### Applications Home Page (New)
- **`ApplicationsHome.java`** — New servlet at `/ApplicationsHome` for PSP Users/Admins. Two JPQL queries: in-progress (status=IN_PROGRESS) and pending review (status=SUBMITTED, not yet APPROVED/DENIED). Pre-computes agency names via `Map<Long, String>` (Person → first agency from ManyToMany). Take Over action sets `Opportunity.managedBy` to current person.
- **`applicationsHome25.jsp`** — Two-section card layout with `.audit-wrap` flex pattern. In Progress table (prospect, agency, agent, date, Take Over button). Awaiting Review table (linked prospect, agency, agent, date, Review button). Empty states with inbox icons.

### Review Application Role Gates
- **`ReviewApplication.java`** — Added PSP Admin gate on doPost (403 for non-admins). Agent access check: agent-only users can only view their own prospects. Added `canReview`, `isAgentView`, `hideSetupLink` request attributes. CSV export handler for `action=exportCsv`. Added reviewer identity tracking to `more_info` case.
- **`reviewApplication.jsp`** — Read-only agent banner. CSV export button. All review forms wrapped in `<c:if test="${canReview}">`. Non-reviewer fallback with status text. Setup link gated behind `!hideSetupLink`. PSP Review info block showing reviewer name, date, status badge, and notes.

### Navbar Update
- **`navbar25.jsp`** — Added "Applications" link after "Renewals" in the PSP User/Admin nav section.

### Key Technical Decisions
- **Person ↔ Agency is ManyToMany** (not direct FK) — agency names resolved via `agent.getListOfAgenciesWithThisAgent()` in servlet, passed to JSP as a Map
- **Application.reviewedBy** FK references `assignee(id)` (not `person`) due to JPA inheritance (Person extends Assignee, no standalone person table)
- **EntityManager lifecycle** — forward() inside try block to keep EM open during JSP rendering for lazy-loaded relationships

### Files
- **New (2):** ApplicationsHome.java, applicationsHome25.jsp
- **New (1 SQL):** V041__application_reviewer_fields.sql
- **Modified (3):** ReviewApplication.java, reviewApplication.jsp, navbar25.jsp
- **Modified (docs):** migration_tracker.md, schema_version_migration.sql, deployment_backlog.md, claude_memory.md

---

## March 9, 2026 — Session 42: UserManager Fix + Friendly Names Toggle + Sequence Collision Fix

### UserManager FormData Fix
- **Root cause:** All 5 AJAX POST calls in `userManager25.jsp` used `FormData`, which sends `multipart/form-data`. The `UserManager` servlet lacks `@MultipartConfig`, so `request.getParameter()` returned null — causing "Missing action" error on every action (deactivate, reactivate, +agent, -agent, +pspUser).
- **Fix:** Replaced all 5 `FormData` usages with `URLSearchParams` (sends `application/x-www-form-urlencoded`).

### Use Friendly Names Toggle
- Added Settings toggle on Features tab to switch Admin menu labels between friendly names ("Services: What We Offer") and formal names ("Service Manager"). Defaults ON.
- **USE_FRIENDLY_NAMES** constant seeded by `DatabaseInitializer`, cached in `AmsDataGlobal.useFriendlyNames`
- **UpdatePspSettings** handles the toggle POST
- **navbar25.jsp** uses `<c:choose>` with `${applicationScope.global.useFriendlyNames}` for 8 menu items

### DemoDataSeeder Sequence Collision Fix
- **Root cause:** `DatabaseInitializer` uses explicit IDs (Person 104, CheckList 29, Ticket 99) in the shared ASSIGNEE table that bypass EclipseLink's sequence generator. When `SeedDemoData` runs after `ReSeedDb`, auto-generated IDs collide with those explicit IDs, causing `EclipseLink DescriptorException`.
- **Fix:** Added `DatabaseResetUtil.syncAssigneeSequence()` — syncs the SEQUENCE table past the max ASSIGNEE ID and resets EclipseLink's in-memory sequence cache. Called by both `SeedDemoData` and `ReSeedDemoData`.
- Added session invalidation to `SeedDemoData` so seeded data appears immediately without manual logout.

### Files
- **Modified (1 JSP):** userManager25.jsp
- **Modified (4 Java):** AmsDataGlobal.java, UpdatePspSettings.java, DatabaseInitializer.java, DatabaseResetUtil.java
- **Modified (2 JSP):** smtpSettingsMod25.jsp, navbar25.jsp
- **Modified (1 Java):** SeedDemoData.java

No database changes.

---

## March 13, 2026 — Session 43: Questionnaire Scoping Simplification

### ServiceItem-Only Scoping
Simplified questionnaire scoping from 3 independent M:N dimensions (LOS, Enhancement, ServiceItem) to **ServiceItem-only assignment**. Since LOS and Enhancement each already have a FK to ServiceItem, ServiceItem scoping subsumes both.

- **Questionnaire entity:** Removed `losList` and `enhancementList` M:N mappings (DB join tables retained, not used)
- **QuestionnaireService:** Simplified `attachMatchingQuestionnaires()` — removed LOS/Enhancement ID gathering and overlap checks, `hasScopeOverlap()` only checks ServiceItem intersection
- **QuestionnaireAction25:** `updateScope` case handles only `serviceItemIds[]` (removed LOS/Enhancement clear+re-add)
- **QuestionnaireManager25:** Removed `getActiveLos()`/`getActiveEnhancements()` methods, JOIN FETCH `si.activityCategory` for grouping
- **QuestionnaireLoader + seeds:** Stripped to COBRA Renewal native only, scoping changed from Enhancement to ServiceItem lookup by code/description

### Questionnaire Manager UI Redesign
- **Full-height layout:** Right panel uses flex column (`height: calc(100vh - 80px)`) — no page-level scroll
- **Collapsible cards:** Details and Scoping cards use Bootstrap collapse with chevron rotation
- **3-column scoping:** ServiceItem checkboxes in Setup | Renewal | Ticket columns by ActivityCategory group_id (1=Renewal, 2=Setup, 3=Ticket), sticky headers above scrollable checkbox area
- **Fields card flex-grow:** `.card-flex` class — fills remaining viewport space, card-body scrolls internally, hdr-bar always visible
- **Generic chevron rotation:** Single JS handler using `data-collapse-target` attribute on `.collapse-chevron` elements

### Files
- **Modified (5 Java):** Questionnaire.java, QuestionnaireService.java, QuestionnaireAction25.java, QuestionnaireManager25.java, QuestionnaireLoader.java
- **Modified (1 JSON):** questionnaire_seeds.json
- **Modified (1 JSP):** questionnaireManager25.jsp

No database changes (DB join tables questionnaire_los and questionnaire_enhancement retained but unused).

---

## March 16, 2026 — Session 44: BPO Deployment Fixes + Recurring Task Push

### Issue 1: EclipseLink L2 Cache Corruption on ReSeedDb → SeedDemoData
Running SeedDemoData after ReSeedDb caused `DescriptorException` — stale SINGLE_TABLE discriminator mappings in the L2 cache mapped Person fields onto Setup/Activity objects. Fixed by adding `EntityManagerFactory` parameter to `DemoDataSeeder.seedConferenceDemo()` and calling `DatabaseResetUtil.evictEntityCaches(emf)` at the top before any entity lookups. EMF threaded through all callers: SeedDemoData, ReSeedDemoData, DatabaseInitializer, InitializeDataBase.

### Issue 2: BPO Admin 403 on Admin Servlets
BPO Admin (role 102) got 403 Forbidden on `/ReSeedDb`, `/SeedDemoData`, `/SeedBpoDemoData` because auth guards only checked `isPspAdmin`. Fixed `ReSeedDb.isAdmin()` to accept both `isPspAdmin` and `isBpoAdmin`. Updated SeedDemoData and SeedBpoDemoData auth guards similarly.

### Issue 3: seedFilterPresets Duplicate Key on ReSeedDb
`seedFilterPresets` in DatabaseInitializer threw `Duplicate entry '104-1' for user_filter_preset.uq_user_slot` on BPO instance. Added idempotency guard — COUNT query before inserting, skips if presets already exist for user.

### Fix: PartnershipApproveApi Global Cache Refresh
After BPO accepts a partnership request, `AmsDataGlobal.activeBpoRegistrations` was stale — ManageTask25 vendor sourcing section wouldn't appear until Tomcat restart. Added `global.initializeGlobalData()` call after approval commit in PartnershipApproveApi.

### Fix: Recurring Checklist BPO Task Push
When a recurring checklist spawns its next cycle, sourced tasks were not pushed to the BPO vendor. `RecurringChecklistDAO.createNewRecurringChecklist()` creates new ToDos referencing the same Tasks (with `isSourced=true`), but no push happened. Added `BpoTaskPushService.pushDelegatedTasks()` at all 3 call sites: CloseActivity25, AmsDataLocal CHECK_CLOSE, AmsDataLocal CLOSE_CHECK.

### Files Modified
- `DemoDataSeeder.java` — EMF param + L2 cache eviction
- `DatabaseInitializer.java` — EMF param on seedDemoData + idempotent seedFilterPresets
- `InitializeDataBase.java` — pass EMF to seedDemoData
- `SeedDemoData.java` — pass EMF + BPO auth guard
- `ReSeedDemoData.java` — pass EMF
- `ReSeedDb.java` — BPO auth guard in isAdmin()
- `SeedBpoDemoData.java` — added auth guard
- `PartnershipApproveApi.java` — global cache refresh after approval
- `CloseActivity25.java` — BPO push on recurring spawn
- `AmsDataLocal.java` — BPO push on recurring spawn (2 cases)

---

## March 18–20, 2026 — Custom Landing Page & Request a Quote (Sessions 45–46)

Custom landing page system for PSPs, public Request a Quote form, and configurable header bar colors.

### V043 Migration
Added `text_value TEXT` nullable column to the `constant` table for storing large HTML content (landing page HTML). `Constant.java` entity updated with `textValue` field.

### Custom Landing Page
- **login.java** routing: checks `USE_CUSTOM_LANDING` flag and `CUSTOM_LANDING_HTML` content; if both exist, renders custom landing; otherwise falls back to legacy `Landing25` page
- **customLanding25.jsp** wrapper: fixed header bar (logo + Login button), `landing-body` div receives admin-authored HTML, login modal, IntersectionObserver for scroll animations, smooth scroll for anchor links
- **UpdatePspSettings.java** saves landing HTML via AJAX `action=saveLandingHtml`, sanitizes (strips `<script>`, `on*` handlers, `javascript:` protocols), stores in `CUSTOM_LANDING_HTML` constant's `textValue`
- **smtpSettingsMod25.jsp** Features tab: custom landing toggle + CodeMirror HTML editor panel for authoring content

### Request a Quote
- **RequestQuote.java** servlet at `/RequestQuote` — public (bypasses LoginFilter), creates Person + Opportunity from form data
- **requestQuote25.jsp** — standalone card-based form collecting name, contact preference, company, employees, services of interest, notes
- Notes use `<br>` (not `\n`) in `buildNoteText()` for proper display in AMS UI

### Invisible Sections Fix
Landing page `.ss-fade` elements (services, differentiator, CTA sections) invisible because `sanitizeHtml()` stripped the IntersectionObserver `<script>`. Fixed by moving the observer into the wrapper JSP where it's not subject to sanitization.

### Configurable Header Colors
- **LANDING_HEADER_COLOR** and **LANDING_HEADER_TEXT_COLOR** constants (defaults `#0d5681` / `#ffffff`)
- Loaded in `AmsDataGlobal.setConstants()`, used in `customLanding25.jsp` and `requestQuote25.jsp` via EL expressions
- **Settings UI:** two `<input type="color">` pickers with hex text input sync in Features tab
- No migration needed — constants upserted on first save via `upsertConstant()`

### Marketing Prompt Doc
Created `docs/custom-landing-page-prompt.md` — comprehensive guide for external Claude marketing project on how to author HTML content for the custom landing page (wrapper constraints, RequestQuote integration, design guidelines, animation system).

### Files Created
- `docs/custom-landing-page-prompt.md` — marketing prompt for landing page HTML authoring
- `docs/sample-landing-content.html` — reference landing page HTML
- `docs/migrations/V043__custom_landing_page.sql` — text_value column on constant table
- `src/main/java/net/superiorstate/ams/controller/market/RequestQuote.java` — public quote form servlet
- `src/main/webapp/WEB-INF/view/authentication/customLanding25.jsp` — landing page wrapper
- `src/main/webapp/WEB-INF/view/market/requestQuote25.jsp` — quote form JSP
- `src/main/webapp/images/logo_alt.png`, `logo_base.png` — logo variants

### Files Modified
- `login.java` — custom landing routing logic
- `LoginFilter.java` — whitelist `/RequestQuote`
- `AuthenticateUser.java` — redirect to custom landing after login error
- `LogOut.java` — redirect to custom landing on logout
- `Constant.java` — `textValue` field + getter/setter
- `AmsDataGlobal.java` — landing HTML cache, header color fields/getters
- `DatabaseInitializer.java` — seed `USE_CUSTOM_LANDING` constant
- `Opportunity.java` — field addition
- `UpdatePspSettings.java` — color constants GET/POST, landing HTML AJAX save, HTML sanitizer
- `smtpSettingsMod25.jsp` — color pickers, landing HTML editor
- `requestQuote25.jsp` — dynamic header colors
- `customLanding25.jsp` — dynamic header colors, IntersectionObserver
- `TaskReceiveApi.java`, `BpoRecurringHistory.java` — unrelated modifications

## March 22, 2026 — Agent Role Scoping & User Creation Fixes (Session 47)

Bug fixes for agent-only user access controls and user creation name formatting.

### ReviewApplications Agent Scoping
- **ReviewApplications.java** query now adds `AND pr.agent.id = :agentId` when the logged-in user is agent-only (not PSP User/Admin), restricting the application list to their own prospects. Previously showed all PSP applications regardless of role.

### User Manager Reassignment Filtering
- **UserManager.java** GET now includes `agencyIds` array per user in JSON response, queried from the agency-agent join table
- **userManager25.jsp** `umBuildTargetDropdown()` now filters reassignment candidates by role:
  - Deactivating a PSP User/Admin → only shows other PSP User (1) or PSP Admin (5) users
  - Deactivating an agent-only user → only shows other agents assigned to the same agency
- Previously showed all active users regardless of role or agency

### Create User Name Normalization
- **CreateUser25.java** `createPerson()` now normalizes names to capital case (e.g. "KEVIN" → "Kevin") instead of forcing all-uppercase via `toUpperCase()`
- Added `capitalCase()` helper method: trims, lowercases, then capitalizes first letter

### Files Modified
- `ReviewApplications.java` — agent-scoped JPQL query
- `UserManager.java` — agencyIds in JSON, import consolidation
- `userManager25.jsp` — role-aware reassignment dropdown
- `CreateUser25.java` — capital case name normalization

## March 24, 2026 — AI Email Builder & Smart Automation Tags (Session 48)

Added an AI-powered "Build with AI" panel for automation email templates, plus smart fallback behavior for `<<#erName>>` and recipient-less activities.

### AI Email Builder (Phase 2)
- **automation-email-builder.json** — new 20-chunk knowledge base covering the full automation tag language, examples, best practices, and processing flow
- **knowledge-config.json** — registered the new `automation_email_builder` KB (admin-only)
- **AutomationAiBuilder.java** — new servlet at `/AutomationAiBuilder`, PSP Admin only, specialized system prompt with full tag reference, multi-turn conversation via session history (`aiBuilderHistory`, max 10 turns), searches only `automation_email_builder` KB
- **ClaudeApiService.java** — added multi-turn `ask(systemPrompt, List<Map<String,String>> messages)` overload
- **KnowledgeSearchService.java** — clarifying comment on admin-only KB list
- **taskManager25.jsp** — redesigned right column with 3-zone layout: toolbar (Build with AI / Tags / Preview), editor textarea, collapsible AI builder panel with chat UI. Code blocks render as dark canvas with "Insert into Editor" and "Copy" buttons
- **UpdateTask25.java** — clears `aiBuilderHistory` session attribute on save/cancel

### Smart `<<#erName>>` Resolution
- **AutomationHelper.resolveErName()** — new method resolving employer/prospect name across all activity types: Renewal → employer, Ticket → contact→employee→employer, Setup → application→proposal→prospect, Opportunity → prospect, CheckList → delegates to parent. Returns null on any failure.
- **SendAuto25.java** — resolves `<<#erName>>` BEFORE input extraction. If name resolves, bakes it into the template. If null, swaps to `<ii>Employer Name</ii>` which becomes a manual input field.

### Recipient-Less Activity Safeguard
- **SendAuto25.java** — checks for valid primary contact email before input extraction. If no valid recipient exists (standalone checklist/personal task), injects `<ii><to></ii>` as first input field. New "TO" type in `getLabelType()`.
- **autoInputScreen25.jsp** — TO inputs render with `type="email"` and placeholder for browser validation
- **SendAutoFinal25.java** — extracts TO email separately (not embedded in body), validates via `Validator.isValidEmail()`, finds or creates Person record, adds as first recipient

### Files Created
- `AutomationAiBuilder.java` — AI builder servlet
- `automation-email-builder.json` — AI builder knowledge base

### Files Modified
- `SendAuto25.java` — smart tag resolution, recipient check, TO input injection
- `SendAutoFinal25.java` — TO email extraction, validation, recipient creation
- `AutomationHelper.java` — `resolveErName()` method
- `ClaudeApiService.java` — multi-turn ask() overload
- `KnowledgeSearchService.java` — comment
- `knowledge-config.json` — new KB entry
- `taskManager25.jsp` — 3-zone layout, AI builder UI
- `autoInputScreen25.jsp` — email type input for TO
- `UpdateTask25.java` — session cleanup

## March 10, 2026 — SendAutoFinal25 White Screen Fix (Session 49)

Fixed a white screen bug in `SendAutoFinal25` that occurred when sending automation emails from standalone checklists (personal tasks) with no primary contact.

### Root Cause
Three bare `return;` statements in SendAutoFinal25 produced an empty HTTP response (white screen) when any step in the email creation/send/update pipeline failed:
1. Email entity persist catch block — `catch (Exception) { return; }` (no logging, no rollback)
2. Email send failure — `if (!emailSent) return;`
3. Activity lookup failure — `if (a1 == null) return;`

### Fix
- Restructured error handling so the method **always** cleans up and forwards to `ViewActivity25`, regardless of which step fails
- Email creation catch block now logs the exception (`exception.printStackTrace()`), rolls back the active transaction, and lets `email` stay null so downstream send/update are skipped
- Email send failure now logs and skips the activity update but continues to cleanup/redirect
- Activity update block wrapped in its own try/catch with logging and rollback
- Cache cleanup and `forward(request, response)` to ViewActivity25 are unconditional at the end

### Files Modified
- `SendAutoFinal25.java` — eliminated all silent `return;` paths, added error logging and transaction rollback

## March 10, 2026 — Proposal AI Page Builder + Bug Fixes (Session 50)

Built an inline AI assistant for generating styled HTML content blocks for proposal custom pages. Follows the `AutomationAiBuilder` pattern (Session 48) — multi-turn conversation, knowledge base context injection, code canvas with insert/copy.

### Proposal AI Page Builder
- **ProposalAiBuilder.java** — new servlet at `/ProposalAiBuilder`, PSP Admin only, uses Claude Sonnet (`claude-sonnet-4-20250514`, 4096 max tokens). Specialized system prompt covering the card-inset pattern, scoped CSS, merge tokens, wrapper div requirement, Bootstrap collision avoidance. Searches only `proposal_page_builder` KB.
- **proposal-page-builder.json** — new 24-chunk knowledge base covering concepts (rendering context, merge tokens), structural patterns (skeleton, card-inset, CSS scoping, scale factor, responsive rules), style adaptation (colors, fonts, source material), layout patterns (two-column, card grid, stat rings, callout), section type guidance (title/closing/custom), examples (dark navy, light/white, brand-adaptive), and practical guidance (mistakes, workflow, font sizing).
- **knowledge-config.json** — registered `proposal_page_builder` KB
- **ClaudeApiService.java** — added `ask(systemPrompt, messages, model, maxTokens)` overload for explicit model/token control. Increased timeout from 30s to 60s.
- **proposalSettings.jsp** — added `.ps-ai-*` CSS classes, "Build with AI" button on TITLE/CLOSING/CUSTOM section toolbars, inline AI chat panel with messages area and input field, JavaScript functions: `openProposalAiBuilder`, `sendProposalAiQuestion`, `formatProposalAiResponse` (code canvas with dual Code/Preview tabs, Insert into Editor, Copy), `insertIntoSectionEditor`, `copyCanvasRaw`

### Proposal AI Builder Bug Fixes
- **Model 404 fix:** Changed from non-existent `claude-sonnet-4-5-20250514` to valid `claude-sonnet-4-20250514`
- **Form submission on Enter:** AI input field inside `<form>` triggered form POST on Enter. Fixed with `event.preventDefault()` in onkeydown handler.
- **Form submission on Insert/Copy buttons:** Dynamically generated `<button>` elements defaulted to `type="submit"` inside form. Fixed by adding `type="button"` to all generated buttons.
- **`<br>` code corruption:** `\n` → `<br>` replacement in `formatProposalAiResponse` corrupted HTML code stored in hidden elements. Fixed with placeholder pattern — code blocks replaced with `@@CANVAS_BLOCK_N@@` tokens before `<br>` conversion, then restored afterward. Raw code stored via `textContent` on a `<div>` (set in setTimeout), immune to innerHTML manipulation.
- **Missing CSS wrapper div:** AI generated CSS scoped under `.about1` but HTML lacked the `<div class="about1">` wrapper, so zero CSS selectors matched. Fixed system prompt with explicit correct/wrong examples and HTML structure template. Updated KB "Common Mistakes" chunk (wrapper = #1 mistake) and "CSS Scoping Rules" chunk.
- **Bootstrap `.card` collision:** AI used `.card` class name which collides with Bootstrap 5's `.card { background-color: #fff }`. Updated system prompt rule #7 and KB to ban Bootstrap class names even when scoped.

### ProposalSettings Focus Preservation
- **ProposalSettings.java** — POST redirect now includes `?sectionId=X` to preserve which section was active after save. `createCustom` redirects to new section ID, `deleteCustom` clears to default.

### Proposal Viewer Rendering Fixes
- **ViewProposal.java** — fixed feature loading to use direct-FK modules per LOS/Enhancement; JOIN FETCH for lazy-load safety
- **proposalFeatures.jsp** — LOS matching via direct FK
- **proposalPricing.jsp** — rendering updates
- **ApplyForProposal.java** — `LEFT JOIN FETCH p.application` for lazy-load safety
- **ReviewApplication.java** — application review updates

### Skill File
- `.claude/skills/proposal-content-page/SKILL.md` — installed Claude Code skill definition for generating proposal page HTML blocks

### Files Created
- `ProposalAiBuilder.java` — AI builder servlet
- `proposal-page-builder.json` — 24-chunk knowledge base
- `.claude/skills/proposal-content-page/SKILL.md` — Claude Code skill

### Files Modified
- `ProposalSettings.java` — POST redirect with sectionId, focus preservation
- `ClaudeApiService.java` — model/maxTokens overload, timeout increase
- `knowledge-config.json` — new KB entry
- `proposalSettings.jsp` — AI builder panel, CSS, JS functions, bug fixes
- `ViewProposal.java` — feature loading fix
- `proposalFeatures.jsp` — LOS matching fix
- `proposalPricing.jsp` — rendering updates
- `ApplyForProposal.java` — JOIN FETCH fix
- `ReviewApplication.java` — review updates

---

## March 10, 2026 — Session 51: Agency-Scoped Proposal Sections, Proposal Settings Full-Height Layout

### Agency-Scoped TITLE/CLOSING Overrides (V044)
- **V044 migration** — `agency_id` nullable FK on `proposal_section`, unique index on `(psp_id, agency_id, section_type)`
- **ProposalSection.java** — added nullable `Agency` field with `@ManyToOne @JoinColumn`
- **ProposalSettings.java** — `doGet` passes `agencyList` to JSP; POST actions: `createAgencySection` (clones default content), `deleteAgencySection`; relaxed `toggleActive` to allow agency-scoped TITLE/CLOSING
- **ViewProposal.java** — agency resolution chain (Proposal → Prospect → Agent → Agency), TITLE/CLOSING override lookup by agency + section type
- **proposalSettings.jsp** — Agency Overrides card on default TITLE/CLOSING sections: lists existing overrides with delete buttons, create dropdown filtered to agencies without overrides

### Proposal Settings Full-Height Flex Layout (No Page Scroll)
- **`.ps-page`** outer wrapper — `height: calc(100vh - 64px)`, flex column, no overflow
- **Left column (`.ps-left`)** — sticky header, card-body scrolls independently
- **Right column (`.ps-right`)** — flex column; editor panel fills space
- **Collapsible editor body** — chevron toggle on section header bar collapses/expands the editor area with CSS transition (max-height, opacity, padding)
- **Bottom cards fill remaining height** — Agency Overrides (TITLE/CLOSING) and Display Scope (CUSTOM) wrapped in `.ps-bottom-card` with `flex: 1` and scrollable card-body; expand when editor is collapsed
- **Merge Tokens modal** — moved inline token-ref block into `#mergeTokensModal` Bootstrap modal, triggered by `{}` button on section header bar
- **Preview iframe fix** — `PREVIEW_MIN_HEIGHT` constant (440px) prevents tiny preview box; `setTimeout` defer for content layout before auto-sizing

### Bug Fixes
- **Proposal URL malformed** — `ProposalDetail.java`, `SendProposal.java`, `proposalDetail.jsp` URL construction fixed
- **Features visibility** — `proposalFeatures.jsp` skip-card guard for LOS/Enhancement sections with no features; `viewProposal.jsp` guard
- **ModuleDetail.java** — added missing `@Table` annotation

### Git Sync
- Pulled commit `948aadb` (Proposal AI Builder, Session 50) from other workstation
- Stash + pull + pop strategy; resolved one conflict in `proposalFeatures.jsp` (variable naming)
- `ProposalSettings.java` and `proposalSettings.jsp` auto-merged cleanly (AI builder + agency overrides coexist)

### Files Created
- `docs/migrations/V044__proposal_section_agency_scoping.sql`

### Files Modified
- `ProposalSection.java` — agency FK field
- `ProposalSettings.java` — agency list, createAgencySection, deleteAgencySection
- `ViewProposal.java` — agency resolution, TITLE/CLOSING override logic
- `proposalSettings.jsp` — full-height flex layout, collapsible editor, merge tokens modal, agency overrides UI, preview min-height fix
- `ProposalDetail.java`, `SendProposal.java`, `proposalDetail.jsp` — URL fix
- `proposalFeatures.jsp` — features visibility guard
- `viewProposal.jsp` — features visibility guard
- `ModuleDetail.java` — @Table annotation

---

## March 10, 2026 — Session 52: Application Service Selections, Setup Enhancement Cascade, Opportunity Proposal Creation

### V045 Migration — Application Selected Services
- **V045__application_selected_services.sql** — `selected_los_ids VARCHAR(500)` and `selected_enhancement_ids VARCHAR(500)` on `application` table
- **Application.java** — new fields, comma-separated ID storage, helper methods (`getSelectedLosIdList()`, `getSelectedEnhancementIdList()`, `hasServiceSelections()`)

### Application Form Service Selection Persistence
- **ApplyForProposal.java** — stores checked LOS/Enhancement IDs as comma-separated strings on Application entity during form submission; pre-selects checkboxes on reload from saved values; fix for duplicate key error on Application INSERT (LEFT JOIN FETCH p.application in loadProposal)
- **SaveApplicationProgress.java** — saves LOS/Enhancement selections during AJAX auto-save progress
- **applyForProposal.jsp** — service selection checkboxes restore state from saved `selectedLosIds`/`selectedEnhancementIds`; enhancement opt-in display; fix for selections resetting on page reload
- **ReviewApplication.java** — displays saved service selections in review view
- **reviewApplication.jsp** — shows selected services with green check badges

### Setup Modal Enhancement Cascade (enhLosMap)
- **AmsDataGlobal.java** — new `enhLosMap` (Map<Long, List<Long>>) built from `enhancement_los` join table via JPQL; cached alongside other setup maps; refreshed in `refreshSalesData()`
- **SetupModalData.java** — added `enhLosMap` JSON block to endpoint response, mapping enhancement ID → parent LOS IDs
- **addActivityModal25.jsp (Setup tab)** — new `aa_filterEnhancements()` function: shows enhancement switches when at least one parent LOS is checked; hides and unchecks when no parent LOS selected; driven purely by enhLosMap (removed rateExtraMap dependency)

### Opportunity Tab — Rate/LOS Selection + Linked Proposal Creation
- **addActivityModal25.jsp (Opportunity tab)** — added Rate dropdown (`aa_oppRateId`), LOS checkbox switches (`.aa-opp-los-item`), hidden `losIds` input; cascade: agency → rate filtering via `dataset.rates`, rate → LOS filtering via `rateLosMap`; `aa_onOppRateChange()`, `aa_prepareOppSubmit()`, updated `aa_validateOpp()` (requires rate + LOS)
- **CreateOpportunity.java** — after Opportunity + CheckList creation, creates linked Proposal with selected rate and LOS items; sets `sourceActivity` to the opportunity; uses ProposalBuilder's bidirectional M:N pattern for LOS linking

### ProposalBuilder LOS Filtering Fix
- **ProposalBuilder.java** — Rate→LOS availability map now built from `RateTable.getModule().getLos()` with proper null checks; ensures only LOSs with fee line items in the rate appear as selectable
- **proposalDetail.jsp** — displays selected LOS items on proposal detail page

### Files Created
- `docs/migrations/V045__application_selected_services.sql`

### Files Modified
- `Application.java` — selected LOS/Enhancement ID fields + helpers
- `ApplyForProposal.java` — service selection save/restore, duplicate key fix
- `SaveApplicationProgress.java` — LOS/Enhancement auto-save
- `applyForProposal.jsp` — service selection UI with state restore
- `ReviewApplication.java` — service selection display
- `reviewApplication.jsp` — selected services badges
- `AmsDataGlobal.java` — enhLosMap cache
- `SetupModalData.java` — enhLosMap JSON
- `addActivityModal25.jsp` — Setup enhancement cascade, Opportunity Rate/LOS/Proposal
- `CreateOpportunity.java` — linked Proposal creation
- `ProposalBuilder.java` — Rate→LOS map fix
- `proposalDetail.jsp` — LOS display
- `migration_tracker.md` — V045 row
- `schema_version_migration.sql` — V045 insert

### Database Changes
- **V045:** `selected_los_ids VARCHAR(500)` and `selected_enhancement_ids VARCHAR(500)` on `application`

---

## March 11, 2026 — Session 54: Extensible Chatbot Skill System

### V046 Migration — Chatbot Skill Table
- **V046__chatbot_skill_table.sql** — `chatbot_skill` table with FK to `assignee(id)`, index on `(psp_id, is_active)`, self-registers in `schema_version`
- Fields: skill_name, description, system_prompt (TEXT), trigger_keywords, accepts_file_upload, accepted_mime_types, model, max_tokens, is_active, admin_only, sort_order

### ChatbotSkill Entity + DAO
- **ChatbotSkill.java** — JPA entity in `model/general/` with helper methods: `acceptsMimeType()` (checks comma-separated MIME list), `getTriggerKeywordSet()` (parses keywords to lowercase Set)
- **ChatbotSkillDAO.java** — static methods: `getActiveSkills()`, `getAllSkills()`, `findMatchingSkill()` with scoring (file MIME match = 10 base + keyword bonus, text-only requires >= 2 keyword hits)

### ClaudeApiService Structured Content Support
- **ClaudeApiService.java** — three new overloads for document content blocks:
  - `askWithContent(systemPrompt, JsonArray contentBlocks, model, maxTokens)` — sends content as JsonArray (document + text blocks)
  - `askWithContent(systemPrompt, contentBlocks)` — convenience with defaults
  - `askWithStructuredMessages(systemPrompt, messages, model, maxTokens)` — multi-turn with mixed content types

### ChatAssistant Unified Endpoint
- **ChatAssistant.java** — rewritten with `@MultipartConfig`, accepts both JSON and multipart/form-data
- Skill matching: loads active skills for PSP, filters admin-only for non-admins, matches via `ChatbotSkillDAO.findMatchingSkill()`
- `executeSkill()` — file-accepting skills: base64-encode file → document content block → `askWithContent()`; text-only skills: `ask()` with skill system prompt
- `executeKBSearch()` — extracted existing KB/ticket search as fallback when no skill matches

### Skill Manager Admin UI
- **SkillManager.java** — CRUD servlet at `/SkillManager`, PSP Admin only, PRG pattern for create/update/delete/toggle
- **skillManager25.jsp** — card grid with all skills, create/edit modal with all fields, toggle active/inactive, delete with confirmation
- `window._skillData[skillId]` pattern for safe server-to-JS data transfer

### Chat Assistant JSP Updates
- **chatAssistant25.jsp** — unified endpoint (`ChatAssistant` instead of `AchFileUpload`), broadened file accept (`.pdf,.xlsx,.csv,.txt`), generic file icon, `chatFile` part name
- **navbar25.jsp** — added "Chatbot Skills" link under Business Efficiency

### Files Created
- `docs/migrations/V046__chatbot_skill_table.sql`
- `src/main/java/net/superiorstate/ams/model/general/ChatbotSkill.java`
- `src/main/java/net/superiorstate/ams/data/dao/ChatbotSkillDAO.java`
- `src/main/java/net/superiorstate/ams/controller/assistant/SkillManager.java`
- `src/main/webapp/WEB-INF/view/a/assistant/skillManager25.jsp`

### Files Modified
- `ClaudeApiService.java` — structured content block overloads
- `ChatAssistant.java` — unified multipart/JSON endpoint with skill matching
- `chatAssistant25.jsp` — broadened file support, unified endpoint
- `navbar25.jsp` — Chatbot Skills nav link
- `migration_tracker.md` — V046 row
- `schema_version_migration.sql` — V046 insert

### Files Deleted
- `AchFileUpload.java` — replaced by unified ChatAssistant endpoint

### Database Changes
- **V046:** `chatbot_skill` table with 13 columns, FK to assignee, composite index on (psp_id, is_active)

---

## March 11, 2026 — Session 55: Composite Task Ordering

### V047 Migration — Composite Task Order Table
- **V047__composite_task_order.sql** — `composite_task_order` table with FKs to `assignee`, `templategroup`, `task`, unique index on `(psp_id, group_id, task_id)`, self-registers in `schema_version`
- Fields: id (AUTO_INCREMENT PK), psp_id, group_id (1=Renewal, 2=Setup, 3=Ticket), task_id, sort_order
- Purpose: Defines master task ordering across all sequences within a given activity type

### CompositeTaskOrder Entity + CompositeTaskView DTO
- **CompositeTaskOrder.java** — JPA entity in `model/activity/checklist/sequences/support/`, ManyToOne FKs to PSP, ActivityCategory, Task
- **CompositeTaskView.java** — DTO with Task, compositeSortOrder (-1 if unordered), List<String> sequenceNames, boolean reusable, `isOrdered()` helper

### CompositeOrderDAO
- **CompositeOrderDAO.java** — abstract DAO in `data/dao/`, static methods following project conventions
- `getCompositeOrder()` — ordered list by sort_order
- `hasCompositeOrder()` — boolean existence check
- `getAllTasksForCategory()` — iterates RequiredTaskLists for a group_id, deduplicates tasks by ID, strips type prefixes from sequence names, forces eager access of Task boolean fields, merges with existing composite positions, sorts ordered-first then unordered
- `saveCompositeOrder()` — delete-and-rebuild pattern within transaction
- `getCompositeOrderMap()` — returns Map<Long, Integer> (taskId → sortOrder) for population logic

### ApplicationTaskDAO — Composite Ordering with Fallback
- `getTasksRequiredForApplication()` now collects unique tasks via legacy dedup, then checks for composite ordering and applies if available
- Extracted `getTasksWithLegacyOrder()` private method (original dedup logic)
- Added `applyCompositeOrder()` private method: loads composite map, reassigns sort values (unordered tasks get max+10), sorts
- Zero behavior change when no composite order defined

### AddSetupModule25 — Composite-Aware Module Addition
- `addMissingTasksFromServiceItem()` loads composite map from DAO
- New ToDos use composite sort order when available, falls back to per-sequence sort_order

### SequenceBuilder25 — Composite Data Loading
- New `handleCompositeRequest()` method: parses `?composite={groupId}` param, resolves PSP ID from session, loads composite task list via DAO
- Sets request attributes: `compositeGroupId`, `compositeGroupName`, `compositeTaskList`, `hasExistingComposite`, `compositeTaskCount`
- `loadAllSequences()` extended: checks composite existence for each activity type, sets `hasCompositeRenewal/Setup/Ticket` request attributes

### SequenceAction25 — SAVE_COMPOSITE Action
- New `SAVE_COMPOSITE` case in switch statement
- `handleSaveComposite()` method: gets PSP ID from session, parses composite order JSON (`[{"taskId":123,"order":0},...]`), calls `CompositeOrderDAO.saveCompositeOrder()`
- `parseCompositeEntries()` method: manual JSON parsing following project conventions (no external libraries)
- Redirect to `SequenceBuilder25?composite={groupId}` after save

### sequenceManager25.jsp — Composite UI
- CSS: `.composite-btn`, `.seq-source-pill`, `.task-row.unordered` styles
- Composite order button bar: hidden by default, shown when type filter active, layers icon, indicator when composite exists
- Composite view panel: header with group name/task count/saved indicator, SAVE_COMPOSITE form, drag-and-drop task rows with source sequence pills, unordered task styling, save/cancel bar
- JavaScript: `updateCompositeBar()`, `hasCompositeMap`, `groupIdMap`, drag-and-drop events, `renumberComposite()`, `prepareCompositeSubmit()`, auto-activate filter tab in composite mode

### Files Created
- `docs/migrations/V047__composite_task_order.sql`
- `src/main/java/net/superiorstate/ams/model/activity/checklist/sequences/support/CompositeTaskOrder.java`
- `src/main/java/net/superiorstate/ams/model/activity/checklist/sequences/support/CompositeTaskView.java`
- `src/main/java/net/superiorstate/ams/data/dao/CompositeOrderDAO.java`

### Files Modified
- `ApplicationTaskDAO.java` — composite ordering with fallback in population logic
- `AddSetupModule25.java` — composite-aware module addition
- `SequenceBuilder25.java` — composite data loading + handleCompositeRequest()
- `SequenceAction25.java` — SAVE_COMPOSITE action handler
- `sequenceManager25.jsp` — composite UI with drag-and-drop
- `migration_tracker.md` — V047 row
- `schema_version_migration.sql` — V047 insert
- `deployment_backlog.md` — D-62 item

### Database Changes
- **V047:** `composite_task_order` table with 5 columns, 3 FKs, unique composite index on (psp_id, group_id, task_id)

---

## March 11, 2026 — Sessions 56–57: Universal Import System

Built a provider-agnostic data import system replacing the hardcoded Summit import pipeline. Configuration-driven column mappings allow any TPA platform (DataPath Summit, WEX, Alegeus, Employee Navigator, etc.) to be connected to AMS. Implemented across 6 phases in 2 sessions.

### V048 Migration — Universal Import Tables
- **V048__universal_import_system.sql** — 5 tables: `import_provider` (TPA registry), `import_file_type` (file definitions per provider), `import_field_mapping` (column→canonical mappings), `import_plan_type_mapping` (provider plan codes → AMS plan types), `import_run_log` (execution history)
- Seeds 17 universal plan type codes (FSA, HRA, HSA, DCA, COBRA, LPFSA, MRA, PBA, PKG, TRN, DNS, VSN, LIF, STD, LTD, MED, GEN)
- Self-registers in `schema_version`

### Phase 1: JPA Entities (5 new)
- **ImportProvider.java** — TPA platform registry with providerCode, providerName, description, active flag
- **ImportFileType.java** — File definitions per provider: fileLabel, fileKey, targetEntity (PLAN_TYPE/EMPLOYER/EMPLOYEE/BENEFIT), fileFormat (CSV/TSV/EXCEL), required flag, sortOrder
- **ImportFieldMapping.java** — Column-to-canonical-field mappings: sourceColumn (CSV header), canonicalField (AMS-standard name), transformRule (UPPERCASE/LOWERCASE/DATE:pattern/MAP:k=v/BOOLEAN/INT)
- **ImportPlanTypeMapping.java** — Provider plan codes → AMS plan types, nullable provider_id for universal defaults
- **ImportRunLog.java** — Execution history with per-entity insert/update/skip/error counters, serviceItemsCreated, warnings/errors TEXT, status (RUNNING/COMPLETED/FAILED)

### Phase 2: UniversalImportService
- **UniversalImportService.java** — Core import engine in `data/service/`, 4 main methods:
  - `importPlanTypes()` — Upsert plan types with canonical field mapping and transform rules
  - `importEmployers()` — Upsert employers by external organization ID
  - `importEmployees()` — Upsert employees by participant ID + organization ID, creates Person records
  - `importBenefits()` — Upsert benefits with CDH/COBRA source type detection, auto-creates ServiceItems
- Universal file parsing: CSV, TSV, Excel support via Apache POI
- Transform rules: UPPERCASE, LOWERCASE, DATE:pattern, MAP:k=v, BOOLEAN:truthy, INT
- Import order: Plan Types → Employers → Employees → Benefits
- Returns ImportResult per entity with insert/update/skip/error counts and warnings

### Phase 3: ProviderSetup Servlet + 5 JSPs
- **ProviderSetup.java** — CRUD servlet for provider registry, file types, column mappings, plan type mappings
- **providerList.jsp** — Provider listing with Active/Inactive badges, Quick Setup: DataPath Summit button
- **providerEdit.jsp** — Provider create/edit form (name, code, description, active toggle)
- **fileTypeEdit.jsp** — File type CRUD with format/entity/required configuration
- **fieldMappingEdit.jsp** — Column mapping editor with auto-detect upload, canonical field dropdowns by entity type, transform rule support
- **planTypeMappingEdit.jsp** — Universal defaults display + provider-specific plan type mapping CRUD

### Phase 4: UniversalImport Wizard Servlet + 4 JSPs
- **UniversalImport.java** — 4-step wizard with session state management, file upload handling, import execution
  - Session keys: UI_PROVIDER_ID, UI_TEMP_DIR, UI_UPLOADED_FILES, UI_UPLOAD_SUMMARY, UI_RESULTS
  - Creates ImportRunLog per execution, groups files by target entity, ordered execution, AmsDataGlobal reload
- **step1Provider.jsp** — Provider dropdown with step indicator badges
- **step2Upload.jsp** — Dynamic file upload cards from provider's file types
- **step3Configure.jsp** — Upload summary with headers/row counts, renewal months config
- **step4Results.jsp** — Per-entity result cards with insert/update/skip/error dot indicators

### Phase 5: DataPath Summit Seed Configuration (superseded by Session 58)
- **SummitProviderSeeder.java** — Idempotent seeder creating pre-configured "DataPath Summit" provider (now unused — Summit uses dedicated redirect)
  - 7 file types: Plan Types (EXCEL), Employer J1 (CSV), Participant Contact J2 (CSV), Participant Status J3 (CSV), Benefits CDH J4 (CSV), Benefit Plan Years J5 (CSV), Benefits COBRA J7 (CSV)
  - All column mappings matching existing SummitImportService field names

### Phase 6: Import History View
- **ImportHistory.java** — Simple servlet querying ImportRunLog by PSP, max 100 results
- **importHistory.jsp** — Table with status badges, per-entity stats, service items created, expandable warnings/errors
- **navbar25.jsp** — "Data Import" section with 3 links: Import Providers, Universal Import, Import History

### Files Created
- `docs/migrations/V048__universal_import_system.sql`
- `src/main/java/net/superiorstate/ams/model/imports/ImportProvider.java`
- `src/main/java/net/superiorstate/ams/model/imports/ImportFileType.java`
- `src/main/java/net/superiorstate/ams/model/imports/ImportFieldMapping.java`
- `src/main/java/net/superiorstate/ams/model/imports/ImportPlanTypeMapping.java`
- `src/main/java/net/superiorstate/ams/model/imports/ImportRunLog.java`
- `src/main/java/net/superiorstate/ams/data/service/UniversalImportService.java`
- `src/main/java/net/superiorstate/ams/data/service/SummitProviderSeeder.java`
- `src/main/java/net/superiorstate/ams/controller/data/ProviderSetup.java`
- `src/main/java/net/superiorstate/ams/controller/data/UniversalImport.java`
- `src/main/java/net/superiorstate/ams/controller/data/ImportHistory.java`
- `src/main/webapp/WEB-INF/view/a/general/providerSetup/providerList.jsp`
- `src/main/webapp/WEB-INF/view/a/general/providerSetup/providerEdit.jsp`
- `src/main/webapp/WEB-INF/view/a/general/providerSetup/fileTypeEdit.jsp`
- `src/main/webapp/WEB-INF/view/a/general/providerSetup/fieldMappingEdit.jsp`
- `src/main/webapp/WEB-INF/view/a/general/providerSetup/planTypeMappingEdit.jsp`
- `src/main/webapp/WEB-INF/view/a/general/universalImport/step1Provider.jsp`
- `src/main/webapp/WEB-INF/view/a/general/universalImport/step2Upload.jsp`
- `src/main/webapp/WEB-INF/view/a/general/universalImport/step3Configure.jsp`
- `src/main/webapp/WEB-INF/view/a/general/universalImport/step4Results.jsp`
- `src/main/webapp/WEB-INF/view/a/general/universalImport/importHistory.jsp`

### Files Modified
- `navbar25.jsp` — Data Import section (Import Providers, Universal Import, Import History)

### Database Changes
- **V048:** 5 tables (`import_provider`, `import_file_type`, `import_field_mapping`, `import_plan_type_mapping`, `import_run_log`) + 17 universal plan type seed rows

---

## March 11, 2026 — Session 58: Universal Import Runtime Fixes + Summit Redirect

Tested Universal Import system in browser and resolved runtime issues. Made architectural decision: Summit import keeps its dedicated wizard; Universal Import wizard lists Summit as a redirect option.

### Runtime Fixes
- **EMF null NPE** on all 3 new servlets (ProviderSetup, UniversalImport, ImportHistory): replaced `@PersistenceUnit` annotation with `getServletContext().getAttribute("emf")` pattern matching project convention
- **providerList.jsp layout**: widened container from 900px to 1100px, added `white-space: nowrap` on Actions td
- **SummitProviderSeeder**: added missing J5 (Benefit Plan Years) file type with 2 column mappings, bumped J7 to sort order 7

### Summit Redirect Architecture
- **Decision:** Summit's 7-file import with custom J5 plan-year enrichment logic is too specialized for generic column mapping. Summit uses its dedicated `SummitImportWizard` servlet; Universal Import handles all other providers.
- **step1Provider.jsp** — "DataPath (Summit)" hardcoded as first dropdown option (value `SUMMIT_REDIRECT`), form always renders regardless of configured providers
- **UniversalImport.java** — `handleSelectProvider()` detects `SUMMIT_REDIRECT` and redirects to `SummitImport`
- **ProviderSetup.java** — removed `seedSummit` POST action and `SummitProviderSeeder` import
- **providerList.jsp** — removed "Quick Setup: DataPath Summit" button from empty state

### Files Modified
- `ProviderSetup.java`, `UniversalImport.java`, `ImportHistory.java` — EMF injection fix
- `step1Provider.jsp` — Summit redirect option + always-render form
- `providerList.jsp` — removed Summit seed button, widened layout
- `SummitProviderSeeder.java` — added J5 file type (file retained but unused)

---

## Session 59 — Self-Service Anthropic API Key Management (March 11, 2026)

### Overview
Decoupled the company Anthropic API key from `ssa.properties` so each PSP install can manage its own AI activation. AI features start OFF on fresh installs; PSP admins enable them by entering a valid Anthropic API key through the Settings UI. Added admin-only chatbot visibility toggle.

### Key Decisions
- **Hybrid key resolution:** DB `constant` table checked first, `ssa.properties` fallback for dev convenience
- **No separate toggle:** `chatbotEnabled` derived from whether a valid API key resolves (no `CHATBOT_ENABLED` property)
- **Admin-only default:** Chatbot visible only to PSP Admins unless "Show chatbot to all users" is enabled
- **No DB migration needed:** Constant rows created dynamically on first save via `upsertConstant()`

### Changes

**AppConfig.java** — Added `cachedAnthropicApiKey` volatile field, `setAnthropicApiKey()`, `getAnthropicApiKey()` (DB-first, ssa.properties fallback, null for blank/FILL_ME_IN), `hasAnthropicApiKey()`

**AmsDataGlobal.java** — Replaced `CHATBOT_ENABLED` property check with DB-first key resolution via `AppConfig`. Added `chatbotAllUsers` boolean (from `CHATBOT_ALL_USERS` constant, defaults false). `chatbotEnabled` now derived from `AppConfig.hasAnthropicApiKey()`

**ClaudeApiService.java** — Replaced all 4 hardcoded key lookups with `AppConfig.getAnthropicApiKey()`. Added `validateApiKey(String)` — minimal API call (haiku, 1 token) returning null if valid, error message if not

**UpdatePspSettings.java** — GET: added `AI_KEY_SOURCE`, `AI_KEY_HINT`, `CHATBOT_ALL_USERS` to JSON. POST: added `saveApiKey` (validates → saves to DB → reloads cache), `removeApiKey` (sets empty string to override ssa.properties), `CHATBOT_ALL_USERS` toggle save

**smtpSettingsMod25.jsp** — AI Assistant section in Features tab: status badge, password input with visibility toggle, Validate & Save button, Remove Key button (conditional), "Show chatbot to all users" switch

**navbar25.jsp** — Chatbot conditional updated: admins always see it when enabled; standard users only when `chatbotAllUsers` is on

**DatabaseInitializer.java** — Seeds "AI Setup Guide" chatbot skill on init with idempotency guard. Explains AI key setup, skill manager, knowledge base system

### Files Modified
- `AppConfig.java` — cached API key with DB-first resolution
- `AmsDataGlobal.java` — chatbotEnabled from key presence, chatbotAllUsers flag
- `ClaudeApiService.java` — centralized key lookup, validateApiKey()
- `UpdatePspSettings.java` — AI key AJAX save/remove, chatbotAllUsers toggle
- `smtpSettingsMod25.jsp` — AI Assistant settings UI
- `navbar25.jsp` — admin-gated chatbot visibility
- `DatabaseInitializer.java` — AI Setup Guide skill seed

---

## Session 60 — BPO Auto-Approval for Required Sequences + Reseed Fixes (March 11, 2026)

### Overview
Extended BPO task auto-approval to cover required-sequence tasks (tied to tickets, setups, renewals via ServiceItem). Once a BPO approves a delegated task from a RequiredTaskList, future occurrences of the same source task from the same PSP are auto-accepted. Also fixed BPO reseed crash and ticket activity type resolution.

### V049 Migration
- **New file:** `docs/migrations/V049__delegated_todo_source_task_id.sql`
- Added `source_task_id VARCHAR(20)` to `delegated_todo` table
- Composite index on `(source_task_id, psp_client_id)` for auto-approval lookups

### BPO Auto-Approval — Required Sequence Tasks
- **DelegatedToDo.java** — Added `sourceTaskId` field (maps to `source_task_id` column)
- **BpoTaskPushService.java** — Sends `sourceTaskId` (PSP's `task.getId()`) in push payload alongside existing `recurringSeriesId`
- **TaskReceiveApi.java** — Three-tier auto-approval: (1) global auto-accept toggle, (2) recurring series match, (3) source task ID match. Any previously-approved task with the same sourceTaskId from the same PSP auto-accepts

### Ticket Activity Type Fix
- **CreateTicket25.java** — `pushDelegatedTasks()` was called inside `createCheckListForTicket()` before `ticket.setCheckList(checkList)` was committed, so `checklist.getTicket()` returned null → activity type defaulted to "CHECKLIST". Fixed by:
  1. Removed push from `createCheckListForTicket()`
  2. Added explicit bidirectional link: `freshChecklist.setTicket(t)` + persist (mirrors Setup pattern)
  3. Moved push to `createTicketObject()` after bidirectional FK committed

### BPO Reseed Fix
- **DatabaseResetUtil.java** — `reinitialize()` unconditionally called PSP's `performInitialization()`, which created constants like `SUMMIT_PATH` with null values on BPO systems. Fixed by:
  1. Added `systemType` to `SavedState`, captured from `SYSTEM_TYPE` constant
  2. Branched `reinitialize()`: calls `performBpoInitialization(em)` for BPO, `performInitialization(em)` for PSP
- **DatabaseInitializer.java** — Extracted `performBpoInitialization(em)` from `initializeBpoDataBase(request, em)` so reseed can call BPO init path without HttpServletRequest. Fixed missing `setParameter("id", id)` in `createDayOfWeek()` catch block.

### Files Modified
- `DelegatedToDo.java` — sourceTaskId field + getter/setter
- `BpoTaskPushService.java` — sends sourceTaskId in task push payload
- `TaskReceiveApi.java` — three-tier auto-approval logic (auto-accept, recurring, source task)
- `CreateTicket25.java` — bidirectional ticket-checklist link, moved push after commit
- `DatabaseResetUtil.java` — systemType capture, PSP/BPO branching in reinitialize()
- `DatabaseInitializer.java` — extracted performBpoInitialization(), fixed DoW parameter bug

---

## Session 61 — March 11, 2026 — BPO Assignment & Filtering (V050)

### BPO Dashboard Column Spacing
- Added `gap: 0.75rem` and `padding-left: 0.75rem` to `.bpo-columns` flex container
- Removed `border-right` from `.bpo-col-left`, added proper `border` and `border-radius: 6px` to the left card

### My Tasks Filter Fix
- Bug: "My Tasks" query included `OR d.assignedTo IS NULL`, showing unassigned tasks
- Fix: Removed NULL condition from both cross-system (`getMyDelegatedToDos`) and co-located (`getMyBpoToDos`) queries

### Unassigned Filter View
- Added new viewMode `"unassigned"` with dedicated queries for both cross-system and co-located modes
- Added "Unassigned" button to toolbar between "My Tasks" and "All Open"

### Auto-Assignment for Auto-Accepted Tasks (V050)
- When tasks are auto-accepted (global toggle, recurring series, or required-sequence), the system now attempts assignment:
  1. Find most recent prior instance with same recurringSeriesId or sourceTaskId that had an active assignee
  2. Fall back to PspClient.defaultAssignee if set and active
  3. Leave unassigned if neither found
- Added `resolveAutoAssignee()`, `findPriorAssignee()`, `isActiveBpoUser()` methods to TaskReceiveApi

### Default Assignee per PSP Client
- V050 migration: `default_assignee_id` FK column on `psp_clients`
- `PspClient.java`: `defaultAssignee` ManyToOne field
- `pspClients25.jsp`: dropdown to select default assignee per PSP client (populated from BPO users)
- `BpoPspClients.java`: `setDefaultAssignee` action handler

### Files Modified
- `bpoHome25.jsp` — column gap CSS, unassigned filter button
- `BpoHome.java` — fixed My Tasks queries, added unassigned queries and viewMode
- `TaskReceiveApi.java` — auto-assignment logic after auto-approval
- `PspClient.java` — defaultAssignee field
- `pspClients25.jsp` — default assignee dropdown UI
- `BpoPspClients.java` — setDefaultAssignee handler
- `V050__bpo_default_assignee.sql` — migration script
- `migration_tracker.md` — V050 row
- `schema_version_migration.sql` — V050 insert

---

## Session 62 — March 12, 2026 — UI Polish, Starter Packages Rebuild, Universal Import Demo Data

### ViewHome25 Scrollbar Fix
- `.home-zones-row` changed from `min-height: calc(100vh - 48px)` to `height: calc(100vh - 62px)` with `overflow: hidden`
- Added `min-height: 0` to `.home-zones-row > [class*="col-"]` for proper flex shrinking
- Added `overflow-y: auto` to `.home-col-left` for internal scrolling
- Updated responsive media query from `min-height: auto` to `height: auto; overflow: visible`
- Removed inline `height: calc(100vh - 70px)` from center and right columns

### ActivityDetail25 Scrollbar Fix
- `#actLayout` height changed from `calc(100vh - 70px)` to `calc(100vh - 80px)` to account for navbar + breadcrumb bar

### Starter Packages Rebuilt from Production
Deleted all existing package JSON files and rebuilt from non-suppressed `applicationsection` and `applicationfield` data in production database. Regrouped per business requirements:
- **Deleted:** `pretax_s125.json`, `fsa.json`, `specialty.json`
- **New:** `s125_fsa.json` (combined 125/FSA, 7 sections), `cobra.json` (billing plan design + COBRA requirements)
- **Rebuilt:** `general.json` (4 sections), `hra.json` (standard HRA only, excludes EBHRA/ICHRA, 7 sections), `hsa.json` (1 section), `transit_parking.json` (1 section), `billing_payments.json` (2 sections, billing plan design moved to cobra)
- **package-index.json** updated: 7 packages (was 8)
- All `selectOptions` normalized from comma-delimited to pipe-delimited format

### Universal Import Demo Data
- **DemoDataSeeder.java** — Added section 3R: `seedUniversalImportProviders()` creates two import providers (Wex, DPI Suite) each with 4 file types (PLAN_TYPE, EMPLOYER, EMPLOYEE, BENEFIT) and full field mappings including DATE transform rules. Idempotent via provider_code + psp_id check.
- **DatabaseInitializer.java** — Modified to pass EMF parameter for DemoDataSeeder calls
- **4 CSV seed files** in `src/main/resources/demo-import/`:
  - `plan_types.csv` — 10 plan types (IDs 100–109)
  - `employers.csv` — 11 employers (10 active + 1 inactive, IDs 1001–1011)
  - `employees.csv` — 25 employees across all employers, UP Michigan addresses (IDs 2001–2025)
  - `benefits.csv` — 26 benefits with mixed effective dates (IDs 3001–3026)
- **Bug fix:** Initial CSV files used string-prefixed IDs (PT-100, ER-1001, etc.) which failed `parseIntSafe()` validation in UniversalImportService. Changed to plain integer IDs.

### Files Changed
- **Modified (7):** DatabaseInitializer.java, DemoDataSeeder.java, pspHome25.jsp, activityDetail25.jsp, package-index.json, general.json, hra.json, hsa.json, billing_payments.json, transit_parking.json
- **New (6):** s125_fsa.json, cobra.json, plan_types.csv, employers.csv, employees.csv, benefits.csv
- **Deleted (3):** pretax_s125.json, fsa.json, specialty.json

No database changes.

---

## Session 63 — March 12, 2026 — Import Cross-Reference System (V051-V052)

Built a multi-provider cross-reference resolution system for the import pipeline. The `import_id_mapping` table maps external IDs from any provider to internal AMS IDs, enabling multiple TPA platforms to import overlapping data without PK conflicts. Completed in 5 phases.

### V051 Migration — Import ID Mapping Table

- **New file:** `docs/migrations/V051__import_id_mapping.sql`
- Creates `import_id_mapping` table: provider_id (FK), entity_type, external_id, internal_id, is_primary, created_at, updated_at
- Unique index on (provider_id, entity_type, external_id)
- Index on (entity_type, internal_id) for reverse lookups

### V052 Migration — XRef Tracking on Run Log

- **New file:** `docs/migrations/V052__import_run_log_xref_tracking.sql`
- Adds `xref_resolved`, `pk_allocated`, `mappings_recorded` INT columns to `import_run_log`

### Phase 1: Entity + Resolver

- **ImportIdMapping.java** (`model/imports/`) — JPA entity for cross-reference mappings
- **ImportIdResolver.java** (`data/resolver/`) — Static utility methods:
  - `resolveEntity()` — provider xref → direct match → null chain
  - `resolvePlanType()` — cascading: xref → plan_type_mapping → code → exact name → fuzzy name
  - `recordMapping()` — upsert xref after successful import
  - `allocateInternalId()` — find next available ID for PK conflicts
  - Integrity checker methods for orphan/conflict detection

### Phase 2: SummitImportService Integration

- **SummitImportService.java** — Accepts optional ImportProvider parameter, records mappings after each entity import
- **SummitImportWizard.java** — Looks up SUMMIT provider by code, passes to service

### Phase 3: UniversalImportService Integration

- **UniversalImportService.java** — Resolver+fallback for all `em.find()` calls:
  - PK conflict → `allocateInternalId()` assigns next available
  - FK resolution via xref chain before import
  - Records all successful mappings

### Phase 4: Import Transition Manager

- **ImportTransitionManager.java** (`controller/data/`) — Admin servlet at `/ImportTransitionManager`
  - Browse all mappings with pagination, search, entity type filter
  - Link: manually create xref mapping
  - Unlink: remove xref mapping
  - Toggle Primary: change which mapping is the primary for an entity
  - Transfer Primary: reassign primary from one provider to another
  - Bulk CSV: upload CSV of external_id → internal_id mappings
- **importTransition.jsp** — Full admin UI with search bar, filter tabs, mapping table, action modals

### Phase 5: Run Log XRef Tracking

- V052 migration adds tracking columns
- ImportRunLog entity updated with new fields
- UniversalImportService records xref stats per run

### Files Changed
- **New (6):** V051 migration, V052 migration, ImportIdMapping.java, ImportIdResolver.java, ImportTransitionManager.java, importTransition.jsp
- **Modified (4):** SummitImportService.java, SummitImportWizard.java, UniversalImportService.java, ImportRunLog.java

Database changes: V051 (import_id_mapping table), V052 (import_run_log xref columns).

---

## Session 64 — March 12, 2026 — Provider Setup Rework (V053) + Interactive Import Shell (B1)

Major rework of the provider setup workflow with sample-file-driven column mapping, PK/FK flagging, and status tracking. Also built the shell for the Interactive Import wizard (Phase B1) and added UPDATE_ONLY as a third import update mode.

### V053 Migration — Interactive Import Enhancements

- **New file:** `docs/migrations/V053__interactive_import_enhancements.sql`
- Adds `update_mode` VARCHAR(30) DEFAULT 'CREATE_AND_UPDATE' and `mapping_status` VARCHAR(20) DEFAULT 'PENDING' to `import_file_type`
- Adds `is_fk` BOOLEAN DEFAULT FALSE and `fk_entity_type` VARCHAR(50) to `import_field_mapping`

### Phase A: Provider Setup Rework

Complete rewrite of the field mapping workflow. Mappings are now driven by a sample file rather than manual configuration.

- **ProviderSetup.java** — Three major new methods:
  - `autoDetect()` — Reads uploaded sample file, parses headers, matches to existing field mappings, displays unmapped rows
  - `saveMappingsBulk()` — Saves all field mappings at once from table form (PK radio, FK checkbox+type, AMS field dropdown)
  - `validateAndUpdateStatus()` — Checks PK/FK readiness, sets file type to READY or PENDING
- **fieldMappingEdit.jsp** — Full rewrite: sample-file-driven table with PK radio buttons, FK checkboxes with entity type dropdowns, AMS field dropdowns, bulk save button, client-side validation
- **fileTypeList.jsp** — Added Status badge (PENDING/READY with color coding), Mode column (Create Only/Create & Update/Update Only), UPDATE_ONLY option in add-file-type form
- **ImportFieldMapping.java** — Added `isFk` boolean and `fkEntityType` String fields
- **ImportFileType.java** — Added `updateMode` and `mappingStatus` String fields

### UPDATE_ONLY Mode Patch

Third update mode for supplemental data refresh files. When a file type is UPDATE_ONLY, only PK mapping is required (no FK needed) — allows files that refresh extraneous data on already-imported records without requiring foreign key resolution.

- **ProviderSetup.java** — `validateAndUpdateStatus()` skips FK requirements when `updateMode == "UPDATE_ONLY"`
- **fileTypeList.jsp** — Added `UPDATE_ONLY` display and form option
- **fieldMappingEdit.jsp** — Client-side validation skips FK messages when UPDATE_ONLY

### Phase B1: Interactive Import Wizard Shell

Entity-by-entity import wizard with cross-reference resolution. Navigable skeleton — select provider, step through entity types, see results page.

- **InteractiveImportSession.java** (`data/service/`) — Session POJO with typed state:
  - Fields: providerId, providerName, currentEntityStep, entityStates map, completedEntities list
  - `ENTITY_ORDER = List.of("PLAN_TYPE", "EMPLOYER", "BENEFIT", "EMPLOYEE")`
  - Inner classes: EntityImportState, ImportRow, MatchCandidate
  - Helper methods: getAvailableEntityTypes(), advanceToNextEntity(), getStepNumber(), getTotalSteps()
- **InteractiveImport.java** (`controller/data/`) — Wizard servlet at `/InteractiveImport`:
  - GET: step=1 (selectProvider), step=entity (entityStep), step=results (results)
  - POST: selectProvider (init session), skipEntity (mark skipped, advance), reset (clear session)
  - Security: PSP Admin (role 5) or BPO Admin (role 102) only
- **selectProvider.jsp** — Provider dropdown, dynamic step indicator, Start Import button
- **entityStep.jsp** — Full-height flex layout, upload card, resolution table with summary bar and status badges, Commit & Next / Skip buttons
- **results.jsp** — Per-entity result cards with colored dots (insert/update/skip/error), warnings collapsible
- **step1Provider.jsp** — Added link to Interactive Import

### Files Changed
- **New (7):** V053 migration, InteractiveImportSession.java, InteractiveImport.java, selectProvider.jsp, entityStep.jsp, results.jsp
- **Modified (6):** ProviderSetup.java, fieldMappingEdit.jsp, fileTypeList.jsp, step1Provider.jsp, ImportFieldMapping.java, ImportFileType.java

Database changes: V053 (update_mode + mapping_status on file_type, is_fk + fk_entity_type on field_mapping).

---

## March 13, 2026 — Session 66: Interactive Import Wizard B2-B5 Complete

Full implementation of the Interactive Import Wizard — upload, resolution, commit, and results flow.

### B2: Upload & Auto-Resolution
- **InteractiveImport.java** — `handleUploadEntity()`: saves uploaded CSV to servlet context temp dir, parses via UniversalImportService, runs ImportResolutionService to auto-categorize rows
- **ImportResolutionService.java** (new) — Auto-categorizes import rows as MATCHED (exact ID/name hit), SUGGESTED (fuzzy match), or UNMATCHED (no match found); queries by entity type with PSP scoping

### B3: AJAX Row Resolution
- **entityStep.jsp** — Full resolution UI: status badges (Matched/Suggested/Unmatched/New/Skipped), confirm/manual-match/create-new/skip actions per row, search modal for manual entity matching, summary bar with live counts
- **InteractiveImport.java** — AJAX endpoints: resolveRow, searchEntities, bulkConfirmMatched

### B4: Import Commit with Person Association
- **ImportCommitService.java** — Per-entity upsert with cross-reference recording, PK allocation, FK resolution
- **Employee commit** — After insert/update, associates employees with Person objects via email/name matching; only matches unlinked persons (`p.employee IS NULL`); creates new Person + Assignee if no match found

### B5: Polish & Fixes
- Commit confirmation dialog, SUGGESTED→UNMATCHED pre-commit validation, re-upload support
- Results page with per-entity cards showing insert/update/skip/error counts
- Temp file permission fix: use `jakarta.servlet.context.tempdir` instead of `java.io.tmpdir`
- **PlanType dropdown fix** in planTypeMappingEdit.jsp: `${pt.id}` → `${pt.planTypeId}` (PlanType entity uses `getPlanTypeId()` not `getId()`)

### Admin Menu Update
- **navbar25.jsp** — Admin Data Import link now points to `/InteractiveImport` (new wizard) instead of old SummitImport

### Provider Setup Improvements
- **providerList.jsp** — DataPath Summit provider shown as preconfigured/system with edit restrictions
- **DatabaseInitializer.java** — Auto-creates DataPath Summit provider on startup (not demo-seed dependent)
- **DemoDataSeeder.java** — Seeds "DataPath DPI Suite" provider with full field mappings for Plan Type, Employer, Benefit, Employee file types

### Files Changed
- **New (2):** ImportCommitService.java, ImportResolutionService.java
- **Modified (10):** InteractiveImport.java, ProviderSetup.java, DatabaseInitializer.java, DemoDataSeeder.java, benefits.csv, entityStep.jsp, results.jsp, selectProvider.jsp, navbar25.jsp, planTypeMappingEdit.jsp, providerList.jsp

---

## March 13, 2026 — Session 67: Checklist/BPO Improvements & Attachment Fixes

### Auto-Close Modal on Last ToDo Completion
- **CloseToDo25.java** — After completing a todo, checks if all todos are now complete; if so, sets `autoShowCloseModal` request attribute
- **checklistFooter25.jsp** — JavaScript auto-shows the `#closeActivity` Bootstrap modal when flag is set
- Works for both activity-linked checklists (Setup, Renewal, Ticket) and standalone person-assigned checklists

### Vendor Task Management for PSP Admins
- **taskManager25.jsp** — Added "Revoke Vendor" button (PSP admin only, sourced tasks) with confirm modal; leverages existing UpdateTask25 Sourced→Internal flow
- **checklistBasic25.jsp** — Added "Send Back" kebab menu item for BPO-completed-but-not-PSP-verified tasks with confirm modal
- **SendBackToDo25.java** (new) — Clears `bpoCompleted`, sets `isReverted=true`, persists, creates audit note, calls `BpoTaskPushService.revertTaskCompletion()` to notify BPO
- **BpoTaskPushService.java** — Added `revertTaskCompletion()` method sending REVERT action to BPO's `/api/v1/tasks/update`

### BPO Note Attachment Visibility Fix (L2 Cache)
- **NoteAddedCallbackApi.java** — Added `em.getEntityManagerFactory().getCache().evict(ToDoNote.class, note.getId())` after commit; fixes EclipseLink L2 cache staleness where `LEFT JOIN FETCH n.webLinkList` returned empty collection for cross-system note attachments
- **TaskNotesApi.java** — Same L2 cache eviction fix on BPO side receiving PSP note callbacks

### Recurring Checklist Past Runs Fixes
- **bpoHome25.jsp** — Fixed `loadPastRunNotes()` using wrong JSON field names (`n.sourceType`→`n.source`, `n.authorName`→`n.author`, `n.createdDate`→`n.date`, `n.noteText`→`n.text`); added attachment rendering
- **ViewRecurringHistory25.java** — Changed notes query to `LEFT JOIN FETCH n.webLinkList`; added attachment serialization (linkType 1=Wasabi pre-signed URL, linkType 2=external URL)
- **checklistHistory25.jsp** — Added attachment rendering (paperclip links) in past completion notes

### Files Changed
- **New (1):** SendBackToDo25.java
- **Modified (10):** NoteAddedCallbackApi.java, TaskNotesApi.java, CloseToDo25.java, ViewRecurringHistory25.java, BpoTaskPushService.java, checklistBasic25.jsp, checklistFooter25.jsp, checklistHistory25.jsp, taskManager25.jsp, bpoHome25.jsp

---

## March 13, 2026 — Session 68: Center Panel Redesign & Application Badge

### Unified Center Panel Section Headers ("Colored Tab" Pattern)
Replaced two inconsistent section header patterns in the activity detail center panel with a single unified "Colored Tab" design:
- **Before:** Mix of flat gray bar headers (`detail-section-header` with `#f7f9fc` bg) for Primary Contact/Questionnaires, and Bootstrap `card border-start border-3` left-colored-border cards for Setup/Renewal/Ticket/Opportunity
- **After:** All sections use a consistent pattern: left 3px blue accent bar + `#f0f5fa` tinted background header, darker `#1e3a5f` title text, with content in a `detail-section-body` div below
- New CSS classes: `.detail-section-header` (restyled), `.detail-section-body` (new)
- **activityDetail25.jsp** — Updated CSS definitions for center section styles
- **detailPrimaryContact25.jsp** — Swapped content div to `detail-section-body`
- **detailQuestionnaires25.jsp** — Swapped content div to `detail-section-body`
- **detailSetup25.jsp** — Converted from `card border-start` to `detail-section-card`/`detail-section-header`/`detail-section-body`
- **detailRenewal25.jsp** — Same conversion
- **detailTicket25.jsp** — Same conversion
- **detailOpportunity25.jsp** — Same conversion (both "Opportunity Details" and "Proposals" cards)

### Navbar "Awaiting Review" Application Badge
Added a red badge counter to the Applications nav link showing count of submitted applications awaiting review:
- **AmsDataLocal.java** — Added `awaitingReviewCount` field with lazy-loading getter; count query mirrors ApplicationsHome logic (`status='SUBMITTED'`, proposal not approved/denied/inactive); `invalidateAwaitingReviewCount()` resets on next access
- **navbar25.jsp** — Added `<span class="badge rounded-pill bg-danger">` after Applications text, only rendered when count > 0
- **ReviewApplication.java** — Calls `invalidateAwaitingReviewCount()` after any approve/deny/more_info/under_review action
- **CreateSetup25.java** — Calls `invalidateAwaitingReviewCount()` after direct setup creation (which approves the application)

### Files Changed
- **Modified (10):** activityDetail25.jsp, detailPrimaryContact25.jsp, detailQuestionnaires25.jsp, detailSetup25.jsp, detailRenewal25.jsp, detailTicket25.jsp, detailOpportunity25.jsp, AmsDataLocal.java, ReviewApplication.java, CreateSetup25.java, navbar25.jsp

---

## March 16, 2026 — Session 74: Import Plan Cross-Population & User Cache Fix

### Benefit Import: Plan Name / Plan Description Cross-Population
Added a rule to both import engines so that plan_name and plan_description fill each other's gaps:
- If the import defines `plan_name` but not `plan_description`, `plan_description` is populated with `plan_name` (and vice versa)
- On UPDATE: only fills the null/empty field — never overwrites an existing value
- On INSERT: cross-populates the import variables before entity creation
- **ImportCommitService.java** — Added cross-population logic for interactive import wizard
- **UniversalImportService.java** — Added same logic for legacy Summit import engine

### User Creation: PSP User List Cache Refresh
Fixed bug where newly created PSP users would not appear in ownership/delegation dropdowns until Tomcat restart:
- `CreateUser25.refreshGlobalCaches()` had an empty block for PSP roles — the BPO path correctly called `global.setBpoUsers()`, but the PSP path was a no-op
- Now calls `global.refreshUserCaches(em)` which reloads PSP user list, opportunity managers, and BPO users

### Files Changed
- **Modified (3):** CreateUser25.java, ImportCommitService.java, UniversalImportService.java

---

## March 17, 2026 — Session 75: Admin Checklist Override, User Creation Presets, Import Cross-Population

### PSP Admin Checklist Override
Admins were blocked from interacting with time-blocked checklist items (dimmed rows, non-functional "Open" and "Complete" in kebab menu). Fixed by removing `td-blocked` CSS class for PSP admins:
- **checklistBasic25.jsp** — `td-blocked` class now conditionally excluded when `sessionScope.isPspAdmin` is true, so blocked rows render at full opacity and all interactions work
- Kebab "Complete" option now available to admins on blocked items (previously gated by `btnIcon == 'square'` which excluded blocked items)
- Automation lightning bolt icon also now visible to admins on blocked items
- Complete action hardcoded to `CloseToDo25` for admin override (blocked items had no `formServlet` set)

### User Creation: Default Filter Presets
New PSP users created via User Manager had no activity view filter presets, and the ViewHome25 page only supports editing (not creating) presets:
- **DatabaseInitializer.seedFilterPresets()** — Changed from `private` to `public` so it can be called from user creation flow
- **CreateUser25.java** — Added step 6 to call `DatabaseInitializer.seedFilterPresets()` for newly created users, seeding the 3 default filter slots (My Actionable, My Pipeline, All Open)

### Benefit Import: Plan Name / Plan Description Cross-Population
(Committed in Session 74) Added rule to both import engines so plan_name and plan_description fill each other when one is null/empty.

### Files Changed
- **Modified (3):** checklistBasic25.jsp, CreateUser25.java, DatabaseInitializer.java

---

## March 19, 2026 — Session 76: BPO Dashboard Redesign & Bug Fixes

### BPO Dashboard — Tonal Zone Layout (ViewHome25 pattern)
Major visual overhaul of `bpoHome25.jsp` to match PSP ViewHome25's tonal zone layout:

- **Three-column tonal zones** — Sage green left (checklists, `#f9faf4`), white center (delegated tasks), cool slate right (detail panel, `#f4f6f9`). Matches PSP's amber/white/teal pattern.
- **Edge-to-edge columns** — Negative margins (`-0.75rem`) on layout container to cancel Bootstrap `.container-fluid` padding, matching how PSP's `.row` class works.
- **Zone stripes** — 3px colored accent bars at top of each column (sage green left/center, slate blue right), column-scoped via `.bpo-col-left .zone-stripe` pattern.
- **No card borders or rounded corners** — Killed global `.hdr-bar { border-radius: 6px 6px 0 0 }` and Bootstrap `.card` border-radius with page-level `!important` overrides. Columns separated by thin border lines, not card edges.
- **Left column wider** — `flex: 0 0 25%` matching PSP's `col-xl-3` proportion.
- **6px spacer** — Edge-to-edge gray bar between navbar and columns, matching PSP's inline-style spacer.
- **`body { background-color: #eef0f4 }`** — Gray page background matching PSP.
- **Responsive mobile reset** — Tonal backgrounds reset to white, zone stripes hidden, right panel overlays as full-width fixed drawer.

### BPO Dashboard — Two-Zone Split Detail Panel
Replaced the old right panel with a compact two-zone layout based on an approved HTML mockup:

- **Top zone (context card)** — CSS Grid fields (PSP, Type, Activity, Due, Assigned To), close button, action links, completed banner. Compact layout replaces old vertical card stack.
- **Bottom zone (tabbed)** — Notes tab (default, with note stream + input bar) and Past Runs tab (alternate, with expandable inline notes). Custom CSS tabs, not Bootstrap.
- **AJAX Mark Complete** — Converted from form-submit to AJAX POST. On success, removes task row from tree view, cleans up empty activity/PSP parent groups, closes panel.
- **Enhanced due date display** — Relative text ("6 days overdue", "Due today", "Due in 3 days") alongside the date.

### BPO Task Delegation Bug Fixes
- **AddRenewal25.java** — Re-fetch checklist after sequence creation so `toDoList` includes newly-created required-sequence ToDos before BPO push. Previously pushed stale detached checklist with empty toDoList.
- **AssignBenefitToRenewal25.java** — Same fix: re-fetch checklist via `EntityLookup.getCheckListById()` before BPO push.
- **BpoCompleteTask.java** — Added AJAX support: returns HTTP 200 for `XMLHttpRequest` callers, falls back to redirect for form submissions.

### SendBackToDo25 — Push Note to BPO
When a PSP admin sends a task back to the BPO vendor, the rejection note is now pushed to the BPO's `TaskNotesApi` so the vendor can see the reason:
- New `pushNoteToBpo()` method posts `todoGuid`, `noteText`, `authorName` to the partner URL's `/api/v1/tasks/notes` endpoint using `ApiClient.postJsonObject()`. Non-fatal on failure.

### Minor Fix
- **checklistBasic25.jsp** — Replaced Unicode checkmark emoji `✓` with HTML entity `&#10003;` for consistent encoding.

### Files Created
- `docs/mockups/bpo-detail-panel-mockup.html` — Standalone browser-viewable design mockup for the two-zone detail panel

### Files Modified
- **Java (4):** AddRenewal25.java, AssignBenefitToRenewal25.java, SendBackToDo25.java, BpoCompleteTask.java
- **JSP (2):** bpoHome25.jsp, checklistBasic25.jsp

No database changes.

---

## April 11, 2026 — Session 77: Outlook Web Add-in "Log to AMS" (V060)

End-to-end build of an Outlook Web Add-in that lets users log an inbound email as a Note on an open AMS activity, with attachments uploaded to Wasabi and linked via WebLink records. Also built the supporting admin page and V060 migration.

### V060 Migration

- **New file:** `docs/migrations/V060__outlook_user_link.sql`
- Creates `outlook_user_link` table: `(id, person_id, m365_email UNIQUE, api_token UNIQUE, is_active, created_date)`
- Adds nullable `weblink.note_id BIGINT` FK + index `idx_weblink_note` (same pattern as existing `email_id` / `todo_note_id`)
- Updates `schema_info` view to V060
- Self-registers in `schema_version`
- **Numbering note:** Originally planned as V058, bumped to V060 because V058 (questionnaire_renderer) and V059 (ndt_census_tables) were already taken.

### Authentication Model

The add-in does NOT use session cookies or the existing `ApiTokenFilter`. Each linked user has a per-user `api_token` (64 hex chars = two concatenated UUIDs, dashes stripped) stored in `outlook_user_link`. The taskpane:

1. On first open, reads `Office.context.mailbox.userProfile.emailAddress`
2. POSTs it to `/api/v1/outlook/authenticate`
3. Stores the returned token in `localStorage`
4. Sends `Authorization: Bearer {token}` on every subsequent request

This avoids cookie/session issues with Office.js iframes entirely. If no link row exists for a given M365 email, the add-in shows "Account not linked — contact your administrator."

### API Endpoints (`/controller/api/outlook/`)

- **`OutlookApiHelper`** — Shared token validator (`validateOutlookToken`) + JSON helpers (`sendJson`, `sendJsonError`, `escapeJson`). Avoids pulling in a JSON library for the single-field request bodies.
- **`OutlookAuthApi`** (`POST /api/v1/outlook/authenticate`) — Exchanges M365 email for the stored api_token. Returns `{token, userName, personId}` or 404 "not linked". Uses a minimal inline JSON body parser.
- **`OutlookActivitiesApi`** (`GET /api/v1/outlook/activities?q=...`) — PSP-scoped search against `Activity25` (view `a25_activity_list_open`). Filters on `LOWER(a25.name) LIKE ?` and `activity.loggedBy.psp.id = ?`. Returns up to 20 matches as `[{activityId, label, employerName, activityType}]`. Label format: `{employerName} — {activityType} (Due: {date})`.
- **`OutlookLogEmailApi`** (`POST /api/v1/outlook/log-email`) — Multipart upload (20 MB/file, 50 MB total). Creates a `Note` with `reason_id=4` ("Received Email", confirmed in DatabaseInitializer seed), user-chosen `status_id` (default 3 "Waiting on Us"), `createdBy = token-linked person`. For each attachment: UUID filename, Wasabi upload via `StorageDAO.uploadFile`, `WebLink` row with `note_id` FK. PSP cross-tenant check via `activity.loggedBy.psp.id` comparison → 403 on mismatch.

### Filter Updates

- **`ApiTokenFilter.java`** — Added bypass for `/api/v1/outlook/` paths (follows existing partnership / registry / webhook / system-register pattern).
- **`LoginFilter.java`** — Added `/outlook/` to `allowedPath` check so the static add-in files (manifest.xml, taskpane.html, PNGs) are reachable without auth. Note: `/api/*` was already whitelisted.
- **Package discovery:** `LoginFilter` lives at `net.superiorstate.ams.LoginFilter` (root package), not `net.superiorstate.ams.filter.LoginFilter`.

### Admin Page (`/OutlookLinkManager`)

- **`OutlookLinkManager.java`** (`controller/user/`) — PSP Admin (role 5) CRUD servlet. Actions: `link` (create with fresh token), `unlink` (soft-delete via `isActive=false`), `relink` (regenerate token, reactivates if inactive).
- **`outlookLinkManager.jsp`** (`WEB-INF/view/user/`) — Full-page admin UI with `.audit-wrap` flex layout (toolbar + scrollable body, `calc(100vh - 64px)`), sticky-header table, SSA brand badges, Bootstrap modal for "Link New User" with user dropdown + email field. Confirm prompts on destructive actions.
- PSP users dropdown scoped to `psp.id = 4` and `u.isActive = true`.

### Static Add-in Files (`/src/main/webapp/outlook/`)

- **`manifest.xml`** — Office Add-in MailApp manifest, Mailbox API 1.5+, `MessageReadCommandSurface` button that opens the taskpane, `ReadWriteItem` permission. Uses VersionOverrides for ribbon button configuration.
- **`taskpane.html`** — Single self-contained page (Office.js + Bootstrap 5 CDN). Auto-auth flow on load → email preview card (from/subject/date + attachment checkboxes) → typeahead activity picker with 300 ms debounce → status radio (Waiting on Us default) → multipart submission. Base64 attachment content converted to `Blob` via `atob` helper. SSA brand colors `#0d5681` navy / `#87a948` green.
- **`README.md`** — Icon specs + sideload instructions + reference to the generator script.

### Icon Generation

- **`scripts/generate-outlook-icons.ps1`** — Reproducible PowerShell icon generator using `System.Drawing` (no ImageMagick dependency). Generates three navy-tile PNGs with a white bold "A" glyph and a green accent stripe at the bottom (32/80 only; 16px is a flat square for clarity at small size).
- **`icon-16.png`** (269 B), **`icon-32.png`** (541 B), **`icon-80.png`** (1.0 KB) — Brand-matched tiles, rounded corners on 32/80.
- PowerShell gotcha: `New-Object` with float/int arguments must use `-ArgumentList` with explicit `[float]` casts or the `RectangleF` overload resolution fails.

### Entity Changes

- **`OutlookUserLink.java`** (`model/general/`) — New JPA entity. IDENTITY-generated id, ManyToOne Person, unique m365_email + api_token, isActive flag.
- **`Note.java`** — Added `@OneToMany(mappedBy="note") List<WebLink> webLinkList` + getter/setter.
- **`WebLink.java`** — Added `@ManyToOne @JoinColumn(name="note_id") Note note` + getter/setter.

### Critical Findings from Investigation

- **ActivityStatus seed is:** `1=Waiting on Them`, `2=No Change`, `3=Waiting on Us`. Prior MEMORY.md had these swapped — corrected by reading `DatabaseInitializer.java:276-278` and `AddNoteToActivity25.handleAnyActivityStatusUpdates`.
- **ReasonCreated id=4 ("Received Email")** already seeded — no new reason needed.
- **Activity25 view-backed DTO** has no service_item column — labels use employer name + DTYPE + due date instead.
- **JSP directory** is `WEB-INF/view/` (singular), not `views/`.

### Files Created (13)
- `docs/migrations/V060__outlook_user_link.sql`
- `model/general/OutlookUserLink.java`
- `controller/api/outlook/OutlookApiHelper.java`
- `controller/api/outlook/OutlookAuthApi.java`
- `controller/api/outlook/OutlookActivitiesApi.java`
- `controller/api/outlook/OutlookLogEmailApi.java`
- `controller/user/OutlookLinkManager.java`
- `WEB-INF/view/user/outlookLinkManager.jsp`
- `webapp/outlook/manifest.xml`
- `webapp/outlook/taskpane.html`
- `webapp/outlook/README.md`
- `webapp/outlook/{icon-16,icon-32,icon-80}.png`
- `scripts/generate-outlook-icons.ps1`

### Files Modified (6)
- `Note.java`, `WebLink.java` — bidirectional note ↔ weblink relationship
- `LoginFilter.java` — `/outlook/` static path allow
- `ApiTokenFilter.java` — `/api/v1/outlook/` bypass
- `docs/analysis/migration_tracker.md` — V060 row
- `docs/schema_version_migration.sql` — V060 INSERT

### Deployment
See D-67 in `docs/deployment_backlog.md`. Not yet applied to any environment.
