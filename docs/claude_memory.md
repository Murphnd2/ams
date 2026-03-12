# AMS Project Memory

## Build Tool
- Maven wrapper: `./mvnw compile` (no system `mvn` on PATH)
- Compile check: `./mvnw compile 2>&1 | tail -30`
- Full build: `./mvnw package -DskipTests`

## Key Patterns
- **"25" suffix** = current/modern version of servlet or JSP
- **Ghost buttons** = `.ssa-action` global class (css-js.jsp) for modal footers/form actions, `.ghost-action` for toolbar actions, `.nav-ghost` for navbar
- **Flex page layout** = `.audit-wrap` pattern: flex column, toolbar + scrollable body, `height: calc(100vh - 64px)`
- **Sticky headers** = `position: sticky; top: 0; background: #f8f9fa; z-index: 1` on `<th>`
- **pageTitle/pageIcon** = set as request attributes before navbar import for dynamic page title in navbar
- **selectOptions delimiter:** pipe-delimited (`|`), not comma — required by field rendering logic

## Current State
- **Branch:** `refactor/modernize-architecture`
- **Latest migration:** V053
- **Session count:** 64
- V025-V037 applied to Demo PSP, BPO, and Master; V038 applied to Demo and BPO; V039-V053 code-complete, not yet applied anywhere
- Not yet applied to production or local dev
- Master snapshot v8 taken 2026-03-04 (V037, fixed update.sh, fixed healthcheck.sh)

## Database Migrations
- Current highest version: **V053**
- Migration tracker: `docs/analysis/migration_tracker.md`
- Schema version SQL: `docs/schema_version_migration.sql`

## Role System
- 1=PSP User, 2=Agent, 3=Client, 4=Applicant, 5=PSP Admin, 8=Agency Admin, 9=PSP Sales
- 102=BPO Admin, 103=BPO User
- Home agency = first agency matching PSP ID (internal staff agents always here)

## Summit Import Architecture
- **Import order:** Plan Types → Employers → Employees → Benefits CDH (J4) → Benefits COBRA (J7) → Benefit Plan Years (J5)
- **J5 does NOT auto-modify nextRenewalDue** — only stores plan year data. Audit page drives corrections.
- **New benefit seeding:** J7 → `enddate + 1`; J4/J5 → `planYearEnd + 1`; J4 alone → `effectiveDate + 12mo`
- **Existing benefits on re-import:** Only update plan year dates, never overwrite nextRenewalDue

## Benefit Entity
- Surrogate PK (`benefit_id` AUTO_INCREMENT), natural key (`summit_id` + `source_type`)
- `planYearStart`, `planYearEnd` (V028), `getDetectedRenewalDate()` = planYearEnd + 1 day
- `sourceType`: 'CDH' or 'COBRA'

## User Manager
- **Modal-based** (not full page) — triggered from PSP Admin navbar dropdown, follows Settings modal pattern
- **Two tabs:** Create User (form) + Manage Users (AJAX-loaded table)
- **UserManager.java** GET returns JSON (user list), POST handles AJAX actions
- **Actions:** deactivate (with reassignment), reactivate, addAgentRole (auto home agency), removeAgentRole (home agency only), addPspUserRole
- **V029 migration:** `is_active BOOLEAN NOT NULL DEFAULT TRUE` on user table
- **Safety:** can't deactivate self, can't deactivate last PSP Admin, inactive users blocked from login
- **Deferred:** external agency agent deactivation, "turn off agency" feature
- **BPO users excluded** from manage view (not relevant for production systems)

## Service Manager (ServiceManagerAction)
- **ApplicationField PK** is `String fieldKey` (not Long) — use `request.getParameter("fieldKey")` and `em.find(ApplicationField.class, fieldKey)`
- **L2 cache eviction** required after field/section mutations: `emf.getCache().evict(ApplicationSection.class, sId)` after commit
- **Error handling:** try/catch/finally with rollback — catches exceptions, logs to stdout, rolls back active transactions

## Add Activity Modal (addActivityModal25.jsp)
- **SetupModalData** endpoint serves both Opportunity and Setup tabs via shared `aa_loadModalData(type)`
- **Agency → Prospect cascade** on Opportunity tab: `aa_onOppAgencyChange()` filters by `agencyIds`
- **Cache pattern:** `aa_setupData` cached per modal open, cleared to `null` on `hidden.bs.modal`
- **UI order (Opportunity):** Agency → Prospect toggle → Prospect/New fields → Submit

## Agency Creation
- **Create Agency modal** two-column layout with Agency Info (name, phone, taxId) + Primary Contact (first, last, email)
- **Single phone field** — agency phone used for both Agency.phone and Person.phone
- **AgencyAction.createAgency** creates Person as `primaryContact` + `manager`, creates User with temp password, assigns Agency Admin (8) + Agent (2) roles, adds to agency agentList
- **Files:** `AgencyAction.java` (createAgency case), `agencyManager25.jsp` (#addAgencyModal)

## Approved Vendors Registry (V032)
- **ApprovedVendor** entity in `model/general/`, auto-discovered (no EmfListener change needed)
- **VendorRegistryApi** = public GET endpoint at `/api/v1/registry/vendors`, bypasses ApiTokenFilter
- **VendorRegistryService** = fetches from master via `ApiClient.getJson()`, returns `null` on failure
- **VendorManager25** = doGet fetches registry, filters already-connected, passes to JSP
- **MASTER_REGISTRY_URL** constant seeded in PSP init, defaults to `https://superiorstate.biz`
- D-46: seed constant on existing PSPs, D-47: populate approved_vendors on master

## BPO Note Attachments (V033)
- **ToDoNote ↔ WebLink** relationship: `@OneToMany(mappedBy="toDoNote")` on ToDoNote, `@ManyToOne @JoinColumn(name="todo_note_id")` on WebLink
- **Upload pattern:** `BpoCompleteTask.uploadNoteAttachment()` — Wasabi via StorageDAO, WebLink with linkType=1 (file)
- **Cross-system attachments:** linkType=2 (external URL) — BPO pre-signs 7-day URLs, PSP stores as external links
- **Display:** `renderNotes()` in bpoHome25.jsp renders attachment pills with paperclip icon

## PSP Note Viewer (Session 24)
- **BpoGetNotes.java** serves both PSP and BPO sides — no BPO-only guard, handles linkType 1+2
- **checklistBasic25.jsp** — note indicator (chat icon + count) on sourced tasks, DOMContentLoaded AJAX loads counts
- **BPO Notes Modal** in checklistBasic25.jsp — Bootstrap modal with `pspRenderNotes()`, BPO/PSP source badges, attachment pills
- **AddNoteToToDo25.java** — PSP-side note+attachment servlet, fires cross-system callback to BPO's TaskNotesApi POST
- **Kebab menu** on sourced tasks includes "Notes" option opening the modal
- **ToDoOut25.isWhoBlocked** — now includes `&& !bpoCompleted` so BPO-completed tasks show as normal completed (not locked)

## Starter Packages (V034, Sessions 25–26)
- **PackageLoader.java** in `data/service/` — reads JSON from `src/main/resources/packages/`
- **8 packages:** general, pretax_s125, fsa, hra, hsa, transit_parking, billing_payments, specialty
- **Duplicate detection:** `template_key` column on applicationsection, unique per (template_key, psp_id)
- **ApplicationField.fieldKey** is String PK — skip individual fields if key already exists
- **em.flush()** after section persist ensures section_id assigned before field FK references
- **Scope ALL auto-assign:** new ALL-scoped sections auto-linked to all active LOS and Enhancements on load
- **getAvailablePackagesForPsp(em, pspId):** hides fully-loaded packages from dropdown
- **resetSectionToDefault(em, section):** restores section+fields to JSON defaults, suppresses manually-added fields
- **ServiceManagerAction** cases: `loadStarterPackage`, `resetSectionToDefault`
- **serviceManager25.jsp** — Load Package button hidden when all loaded, Reset to Default button on package sections
- **selectOptions delimiter:** pipe-delimited (`|`), not comma — required by field rendering logic
- **D-53** in deployment_backlog — code complete, needs V034 migration applied + browser testing

## Proposal Customization (Session 27)
- **V035:** Feature headline VARCHAR(200) + description widened to VARCHAR(2000)
- **V036:** proposal_section table for composable proposal sections per PSP
- **ProposalSection** entity in `model/sales/offering/`
- **ProposalSettings.java** servlet: CRUD for sections, CKEditor 5 Classic + SortableJS on JSP
- **Section types:** TITLE, PRICING, FEATURES, CLOSING, CUSTOM (string-based, no enum)
- **Merge tokens:** `{{TOKEN_NAME}}` replaced at render time in ViewProposal.java
- Auto-initializes default sections on first access (TITLE, FEATURES, PRICING, CLOSING)
- **HTML sanitization:** regex-based in ProposalSettings.sanitizeHtml() (strips script/on*/javascript:)
- **viewProposal.jsp:** section-based rendering when proposalSections exist, falls back to legacy layout
- **Extracted includes:** proposalFeatures.jsp, proposalPricing.jsp
- **D-54** in deployment_backlog — code complete, needs V035+V036 applied + browser testing

## Proposal Feature Display Fix (Session 29)
- **Root cause:** Feature loading used RateTable module IDs, but Features attach to the direct-FK module (`servicemodule.los_id` / `servicemodule.enhancement_id`) — different IDs
- **ViewProposal.java fix:** Resolves direct-FK modules per LOS/Enhancement instead of deriving from pricing; JOIN FETCH `sm.los` + `sm.enhancement` for lazy-load safety after `em.close()`
- **proposalFeatures.jsp fix:** LOS matching changed from M:N `los.getServiceModuleList()` to direct FK `feature.getServiceModule().getLos().getId()`; Enhancement matching uses `feature.getServiceModule().getEnhancement().getId()`

## Proposal Section Scoping (Session 29, V037)
- **V037:** scope column on proposal_section + proposalsectionlos/proposalsectionenhancement join tables
- **ProposalSection entity:** scope (ALL/SCOPED), losList (M:N → LOS), enhancementList (M:N → Enhancement)
- **ProposalSettings servlet:** `updateScope` POST case, loads allLos/allEnhancements for checkboxes
- **proposalSettings.jsp:** "Scoped" badge on left panel, Display Scope card with radio + checkboxes on CUSTOM sections
- **ViewProposal.java:** scope filtering — SCOPED sections only render when proposal's LOS/Enhancements overlap
- **Pattern:** mirrors ApplicationSection scope pattern (same column, same join table naming convention)

## Full-Height Dashboard Layouts (Session 28)
- **BPO Dashboard:** `.bpo-layout` flex wrapper, `.bpo-columns` row, `.bpo-col-left`/`.bpo-col-right` flex columns — both scroll internally
- **PSP Dashboard:** `.psp-dash-body` flex wrapper, `.psp-dash-col-left`/`.psp-dash-col-right` flex columns — removed inline `max-height` from `.dash-scroll` divs
- **Both:** `@media (min-width: 992px)` only — mobile layout unchanged
- **Dropdown clipping fix:** `toDoCurrentList25.jsp` — pre-init kebab dropdowns with `popperConfig: { strategy: 'fixed' }` so menus aren't clipped by `overflow-y: auto` scroll containers

## BPO/PSP Communication Fixes (Session 31, V038)
- **Issue 5 (Vendor-only auto-complete):** TaskCompletedCallbackApi detects `isSourced && !allowNonOwner`, auto-closes vendor-only tasks, differentiates note text
- **Issue 6 (Display state priority):** ToDoOut25.computeDisplayState() rewritten — completed → position-blocked → sourced(BPO waiting) → sourced(BPO done/verify) → owner-blocked → delegated → default
- **Issue 1 (Session refresh):** UpdateTask25 calls `computeAllDisplayStates()` after save to update checklist icons
- **Issue 8 (Mutual exclusion):** Employee assignment and vendor sourcing are mutually exclusive in ManageTask25 (JS toggle + server-side enforcement — sourcing wins)
- **Enhancement 3 (PSP filter):** BPO dashboard PSP filter dropdown with sessionStorage persistence, filters both active and completed sections
- **Enhancement 4 (Sort order):** DelegatedToDo.sortOrder field (V038), pushed via BpoTaskPushService, BpoHome queries sort by dueDate → activityName → sortOrder
- **Vendor-only reopen protection:** checklistBasic25.jsp completed section shows X icon (no reopen form) for `isSourced && !allowNonOwner` tasks

## Questionnaire System (V039, Phases 1–6 — Sessions 32–33, Scoping Simplification — Session 43)
- **4 entities** in `model/activity/questionnaire/`: Questionnaire, QuestionnaireField, QuestionnaireInstance, QuestionnaireFieldValue
- **B2.1 entity conventions:** `fetch=LAZY` on all `@ManyToOne`, `cascade=ALL, orphanRemoval=true` on parent→child `@OneToMany`
- **Convenience methods:** `Questionnaire.isNative()`, `Questionnaire.isScopedToServices()`, `QuestionnaireInstance.isExternal()`
- **Dual-mode:** `external_url IS NULL` = native (AMS fields), `IS NOT NULL` = external (Jotform pointer)
- **QuestionnaireField** uses BIGINT auto PK (not String PK like ApplicationField), `field_key` is regular column with unique index per questionnaire
- **section_name** column on questionnaire_field for UI grouping (no section entity)
- **QuestionnaireLoader.java** in `data/service/` — reads `questionnaire_seeds.json`, pattern follows PackageLoader
- **1 seed questionnaire:** COBRA Renewal (native) — scoped to COBRA ServiceItem by code/description lookup
- **Scoping:** ServiceItem-only via `questionnaire_serviceitem` join table (LOS/Enhancement M:N mappings removed from entity, DB tables retained)
- **Auto-attach:** `QuestionnaireService.attachMatchingQuestionnaires()` checks `activity_type` match + ServiceItem overlap only
- **DatabaseInitializer** hook after assignAllSectionsToLos, before createInitializationChecklist
- **Merge tokens in external URLs:** `{erName}`, `{activityId}`, `{instanceGuid}` — resolved by `Questionnaire.resolveExternalUrl()`
- **Design doc:** `docs/analysis/questionnaire_system_design.md` — 7 phases total
- **QuestionnaireService.java** (`data/service/`) — static utility (abstract class, follows RenewalService pattern):
  - `attachMatchingQuestionnaires()` — auto-attach on activity creation, activityType + ServiceItem overlap
  - `getInstancesForActivity()` — load instances for activity detail card
  - `getInstanceByGuid()` — GUID-based lookup for public form
  - `getFieldsForQuestionnaire()` — direct field query (avoids EclipseLink nested JOIN FETCH)
  - `getFieldValueMap()` — saved values as Map<String, String>
  - `submitInstance()` — marks SUBMITTED, creates Note with "Waiting on Us" status + "Quick Action" reason
- **QuestionnaireManager25.java** (`controller/activity/setup/`) — standalone admin page at `/QuestionnaireManager25`
  - Full-height flex layout: left panel (questionnaire list) + right panel (details/scoping/fields)
  - Right panel: `.right-panel` flex column, `.cards-wrap` holds 3 cards, Fields card is `.card-flex` (flex-grow with scrollable body)
  - Details and Scoping cards are collapsible (Bootstrap collapse with chevron rotation)
  - Scoping card: 3-column layout (Setup | Renewal | Ticket) by ActivityCategory group_id, sticky column headers above scrollable checkboxes
- **QuestionnaireAction25.java** (`controller/activity/setup/`) — CRUD handler for questionnaires+fields, `updateScope` handles ServiceItem-only
- **FillQuestionnaire.java** (`controller/activity/questionnaire/`) — public form at `/q/{guid}`, GET renders form, POST submits
- **SaveQuestionnaireProgress.java** — AJAX auto-save at `/saveQuestionnaire`
- **QuestionnaireInstanceAction.java** — PSP-side actions: review, reopen, detach, markComplete, attach, emailQuestionnaire
- **detailQuestionnaires25.jsp** — activity detail card with status badges, copy link, envelope email, kebab actions, "+" attach dropdown
- **Email questionnaire:** envelope icon per instance → creates in-memory WebLink (linkType=2), sets subject "Questionnaire: <name>", preserves existing recipients from GoActivityDetail25, forwards to CreateEmail25
- **Manual attach:** `getAvailableQuestionnaires()` in QuestionnaireService, `availableQuestionnaires` field in CurrentActivity
- **QuestionnaireWebhookApi.java** (`controller/api/`) — public endpoint at `/api/v1/questionnaire/webhook`, bypasses ApiTokenFilter
  - Parses Jotform `rawRequest` JSON, finds GUID via hidden `ref` field, calls `submitInstance()`
  - Extracts submitter name/email from common Jotform field patterns
  - Idempotent — ignores already SUBMITTED/REVIEWED instances
- **Jotform hidden ref field** added to all 10 SSA forms — `{ref}` default value, backward compatible (forms work without GUID)
- **Webhook URL config pending** — must be set manually per form: Jotform Settings → Integrations → Webhooks → `https://superiorstate.biz/api/v1/questionnaire/webhook`
- **EclipseLink nested JOIN FETCH gotcha:** `LEFT JOIN FETCH q.fieldList` inside `JOIN FETCH qi.questionnaire q` is silently dropped — always load fields via separate direct query
- **Phases 1–6 complete.** Next: Phase 7 (completion gating)

## Demo Data Seeder (Session 36)
- **DemoDataSeeder.java** (`data/service/`) — conference demo data for `demo.superiorstate.biz`
- **SeedDemoData** → `DemoDataSeeder.seedConferenceDemo(em, emf)`, **ReSeedDemoData** extends ReSeedDb + re-seeds demo data
- **L2 cache eviction** required at top of `seedConferenceDemo()` — prevents SINGLE_TABLE discriminator corruption after ReSeedDb
- **seedFilterPresets** has idempotency guard (COUNT before INSERT) — prevents duplicate key on re-init
- **seedAgencyAndProspects** creates Agency 15 (AccelVantage Benefits), agency manager (Sarah Mitchell), sales agent (James Rivera / agent@pspdemo.com), assigns Demo Rate, creates 3 prospects
- **ProposalSettings.initializeDefaults()** auto-creates all 4 section types — DemoDataSeeder does NOT seed proposal sections
- **EclipseLink L2 cache eviction** after `DatabaseInitializer.createApplicationSections()` — fixes fields not appearing in Service Manager / Application views
- **EclipseLink sequence reset** in `DatabaseResetUtil.evictEntityCaches()` — `ServerSession.getSequencingControl().resetSequencing()` fixes duplicate PK errors after DB reset
- **AmsDataLocal null guard** in `intializeActivity()` — prevents NPE when activity not found (e.g., after DB reset with stale caches)

## Proposal Builder Rate Filtering (Session 36)
- **ProposalBuilder.doGet()** queries `SalesDAO.getProspectAgencyData(em)` and `SalesDAO.getAgencyRateMap(em)` directly (not global cache) — ensures newly-created prospects are included
- **Client-side filtering:** `filterRatesByProspect()` JS function resolves prospect → agency IDs → allowed rate IDs, shows/hides rate cards
- **Auto-expand:** if pre-selected prospect not in default list, server switches to expanded "all" list and sets `autoExpand=true`
- **Maps:** `prospectAgencyMapJson` (prospectId→"agencyId,agencyId"), `agencyRateMapJson` (agencyId→[rateIds])

## Application Field Types
- **EMAIL fieldType** must be handled in `applyForProposal.jsp` — renders as `<input type="email">` with placeholder
- **reviewApplication.jsp** uses `<c:otherwise>` catch-all for unrecognized field types — EMAIL displays fine there

## Application Entity PK
- **Application PK is `proposal_id`** (not auto-generated) — `@Id @OneToOne @JoinColumn(name="proposal_id")`
- **ApplyForProposal.loadProposal()** must include `LEFT JOIN FETCH p.application` to avoid creating duplicate Applications when one already exists

## Review Application Headers (Session 38)
- **hdr-bar pattern** for review pages: `background: var(--ssa); color: #fff` single subheader replacing duplicate header blocks
- **reviewApplication.jsp** — prospect name, proposal #, status badge, Back to List + Pipeline/Home buttons
- **reviewApplications.jsp** — title, description, Manual Setup/New Proposal/Pipeline buttons

## Opportunity Visibility on PSP Home (Session 38)
- **ActivityLandingRow.managedById** — new Long field exposed through DAO → Row DTO → JSP/JS
- **ActivityLandingDao** — `b.managed_by_id` added to outer SELECT (index [11])
- **Type filter** — changed from `assigned_to_id = p.me OR managed_by_id = p.me` to `managed_by_id IS NOT NULL` (shows all PSP-managed opportunities)
- **Ownership filter** — `effectiveOwner` logic: Opportunities use `managedById` when not null, all other types use `assignedToId`

## Agent Home Kanban Redesign (Session 38)
- **agentHome25.jsp** — full rewrite: horizontal Kanban board (6 active stage columns), stat strip, slide-out detail drawer (380px)
- **Kanban cards** — company name, agent (agency admins only), value, employee count
- **Drawer sections** — Details, Pipeline Data (inline-editable), Stage dropdown, Proposals (stacked rows, newest first, + New on top)
- **OPPS data map** — JS object with all opportunity fields including nested proposals via JSTL forEach
- **Inline field saving** — individual field AJAX saves via `UpdateOpportunityStage` with spinner → ✓ Saved → fade pattern
- **CreateOpportunity.java** — reads `estimatedEmployees`, `estimatedValue`, `expectedCloseDate` before persist
- **UpdateOpportunityStage.java** — added handlers for all 3 pipeline fields with blank-to-null clearing
- **detailOpportunity25.jsp** — unconditional rows with `<c:choose>` showing `—` when null (was conditional `<c:if>`)
- **EclipseLink lazy loading fix** — `LEFT JOIN FETCH o.prospect p LEFT JOIN FETCH p.proposalList` + explicit `.size()` force-initialization loop for proposals and their LOS lists
- **Proposal display** — stacked `.drawer-prop-row` (not inline pills), `.drawer-new-prop` dashed button on top, descending sort by ID

## Recurring Checklist History (V040, Session 39)
- **V040 migration:** `recurring_series_id` VARCHAR(36) + `recurring_cycle_number` INT on `delegated_todo`
- **DelegatedToDo.java:** new `recurringSeriesId`, `recurringCycleNumber` fields
- **BpoTaskPushService.java:** copies series ID + increments cycle number on recurring push
- **RecurringChecklistDAO.java** (new): `getRecurringHistory()` — past cycles by series ID
- **ViewRecurringHistory25.java** (PSP), **BpoRecurringHistory.java** (BPO): history servlets
- **checklistHistory25.jsp** (shared): history table with `.audit-wrap` layout
- **activityDetail25.jsp** + **bpoHome25.jsp**: "View History" links for recurring checklists
- **D-57** in deployment_backlog — code complete, needs V040 applied + browser testing

## Agent Pipeline: Closed Lookup + CSV Export (Session 39)
- **Closed Opportunity Lookup:** collapsible section below Kanban board with WON/LOST table, client-side text/outcome/agent filters
- **ExportApplicationCsv.java** (new): servlet at `/ExportApplicationCsv`, accepts `proposalId` or `opportunityId`, streams CSV with 14 metadata columns + dynamic ApplicationField columns
- **Contact info fallback:** prospect.getContact() → opportunity.getPrimaryContact()
- **Application entity PK note:** `Application.getId()` returns the proposal_id (Long), not auto-generated
- **PSP.getId() returns Long** — use `long` not `int` when storing

## Application Visibility & Role Walls (V041, Session 41)
- **V041 migration:** reviewed_by (BIGINT FK → assignee), review_notes (TEXT), date_reviewed (TIMESTAMP) on application table. Conditional DDL for idempotency.
- **ApplicationsHome.java** (new): `/ApplicationsHome` servlet for PSP Users/Admins. Two queries (in-progress + pending review). Agency names pre-computed as `Map<Long, String>` because Person ↔ Agency is ManyToMany (no direct FK). Take Over sets `Opportunity.managedBy`.
- **applicationsHome25.jsp** (new): `.audit-wrap` flex layout, two card tables with empty states
- **ReviewApplication.java** mods: PSP Admin gate on doPost (403), agent access check, `canReview`/`isAgentView`/`hideSetupLink` attrs, CSV export, reviewer tracking on `more_info`
- **reviewApplication.jsp** mods: read-only agent banner, gated review forms, reviewer info block, CSV export button
- **navbar25.jsp**: Applications link after Renewals (PSP Users/Admins only)
- **Key gotcha:** Person has `listOfAgenciesWithThisAgent` (ManyToMany), not `getAgency()` — can't JOIN FETCH directly in JPQL
- **Key gotcha:** Application.reviewedBy FK must reference `assignee(id)` not `person(id)` (JPA inheritance, no person table)
- **Key gotcha:** EntityManager must stay open during JSP forward — move forward() inside try block before em.close()
- **D-58** in deployment_backlog — code complete, needs V041 applied + browser testing

## Session 42 (2026-03-09)
- UserManager FormData→URLSearchParams fix (5 POST calls)
- Use Friendly Names toggle (Settings Features tab, all 8 menu items conditional)
- DemoDataSeeder sequence collision fix (DatabaseResetUtil.syncAssigneeSequence)

## Session 44 (2026-03-16) — BPO Deployment Fixes + Recurring Task Push
- EclipseLink L2 cache corruption fix: DemoDataSeeder.seedConferenceDemo() now takes EMF, evicts L2 cache first
- BPO Admin 403 fix: ReSeedDb.isAdmin(), SeedDemoData, SeedBpoDemoData accept isBpoAdmin
- seedFilterPresets idempotency guard (COUNT before INSERT)
- PartnershipApproveApi: refreshes AmsDataGlobal after BPO approval (fixes ManageTask25 vendor sourcing visibility)
- Recurring checklist BPO push: pushDelegatedTasks() at all 3 createNewRecurringChecklist call sites (CloseActivity25, AmsDataLocal x2)
- PspClient.autoAcceptTasks: toggle exists but not wired — all tasks land as ACTIVE (future project)
- D-59 in deployment_backlog — code complete, no migration needed

## Custom Landing Page System (V043, Sessions 45–46)
- **V043 migration:** `text_value TEXT` nullable column on `constant` table for storing large HTML content
- **Constant.java:** new `textValue` field (separate from `value` VARCHAR)
- **login.java:** routing logic — if `USE_CUSTOM_LANDING=true` and HTML content exists, forward to `customLanding25.jsp`; otherwise forward to legacy `Landing25`
- **customLanding25.jsp:** wrapper page with fixed header, login modal, IntersectionObserver for scroll animations
- **Custom landing HTML stored in** `CUSTOM_LANDING_HTML` constant's `textValue` column, saved/loaded via `UpdatePspSettings`
- **sanitizeHtml():** Strips `<script>`, `on*` handlers, `javascript:` protocols. Preserves `<style>` blocks intentionally.
- **Wrapper IntersectionObserver:** Elements with `.ss-fade` or `.fade-in` get `.ss-visible`/`.visible` added on scroll into view. Smooth scroll for anchor links.
- **CSS-only animations:** Hero uses `@keyframes` (no JS needed). `<script>` blocks in content are stripped on save — use CSS animations + wrapper observer only.
- **RequestQuote.java:** Public servlet at `/RequestQuote`, collects prospect info (name, contact, company, employees, services, notes), creates Opportunity
- **requestQuote25.jsp:** Card-based form with dynamic header colors from AmsDataGlobal
- **Configurable header colors:** `LANDING_HEADER_COLOR` (default `#0d5681`) and `LANDING_HEADER_TEXT_COLOR` (default `#ffffff`) — stored as constants, loaded in AmsDataGlobal, used in customLanding25.jsp and requestQuote25.jsp
- **Settings UI:** Two `<input type="color">` pickers with hex text sync in smtpSettingsMod25.jsp Features tab
- **Marketing prompt:** `docs/custom-landing-page-prompt.md` — external guide for Claude marketing project on writing landing page HTML
- **D-60** in deployment_backlog — code complete, needs V043 applied + browser testing

## Session 48 (2026-03-24) — AI Email Builder & Smart Automation Tags
- **AI Email Builder:** `AutomationAiBuilder.java` servlet, `automation-email-builder.json` KB (20 chunks), multi-turn ClaudeApiService, 3-zone taskManager25.jsp layout
- **Smart `<<#erName>>`:** `AutomationHelper.resolveErName()` covers all activity types; SendAuto25 resolves before input extraction, falls back to `<ii>Employer Name</ii>` input
- **Recipient-less safeguard:** SendAuto25 injects `<ii><to></ii>` when no valid primary contact; autoInputScreen25.jsp `type="email"`; SendAutoFinal25 extracts TO, validates, creates/finds Person

## Session 49 (2026-03-10) — SendAutoFinal25 White Screen Fix
- **Bug:** Bare `return;` statements in SendAutoFinal25 caused white screen when email create/send/update failed (no logging, no redirect)
- **Fix:** Restructured so method always forwards to ViewActivity25; email creation catch logs + rolls back; email send failure logs + skips update; activity update wrapped in try/catch; cache cleanup is unconditional
- **Pattern note:** Never use bare `return;` in servlet action methods — always redirect/forward so user gets a response

## Session 47 (2026-03-06) — Legacy TPO Redirect + Hardcoded URL Fixes
- **LegacyTpoRedirect.java:** Catches `/tpo` and `/tpo/*` URLs from old superiorstate.net IIS site, forwards to legacyTpo.jsp
- **legacyTpo.jsp:** Friendly "Our Site Has Been Updated" notice page, public (no auth), uses PSP branding
- **LoginFilter.java:** Added `path.startsWith("/tpo")` to allowedPath bypass
- **Hardcoded URL audit:** Replaced `superiorstate.biz` in SendProposal.java, ProposalDetail.java, proposalDetail.jsp with `AmsDataGlobal.getWebPath()`
- **PSP entity has no phone/email** — removed invalid EL references from legacyTpo.jsp
- **DNS/SSL migration:** superiorstate.net now points to same Linux box, Nginx + certbot configured
- **Backlog #26:** Legacy TPO Path → Opportunity Creation (extract full path for old quote/prospect lookup)

## Session 51 (2026-03-10) — Agency-Scoped Proposal Sections & Full-Height Layout
- **V044 migration:** `agency_id` nullable FK on `proposal_section`, unique index `(psp_id, agency_id, section_type)`
- **ProposalSection.java:** nullable `Agency` field with `@ManyToOne @JoinColumn`
- **ProposalSettings.java:** `agencyList` in GET; `createAgencySection` clones default content; `deleteAgencySection`; relaxed `toggleActive` for agency-scoped
- **ViewProposal.java:** Agency resolution chain: Proposal → Prospect → Agent join table → Agency; TITLE/CLOSING override lookup
- **proposalSettings.jsp full-height layout:** `.ps-page` flex wrapper (`calc(100vh - 64px)`), `.ps-left` sticky header + scrollable list, `.ps-right` flex column, `.ps-editor-body` collapsible via chevron toggle, `.ps-bottom-card` (Agency Overrides / Display Scope) fills remaining height with own scrollbar
- **Merge Tokens modal:** `#mergeTokensModal` — moved from inline block to Bootstrap modal, triggered by `{}` header button
- **Preview iframe fix:** `PREVIEW_MIN_HEIGHT` (440px) + `setTimeout` defer prevents tiny preview box
- **Bug fixes:** proposal URL, features visibility, ModuleDetail @Table
- **Git sync:** Pulled `948aadb` from other workstation (AI Builder), resolved `proposalFeatures.jsp` conflict

## Session 52 (2026-03-10) — Application Service Selections, Setup Enhancement Cascade, Opportunity Proposal Creation
- **V045 migration:** `selected_los_ids` + `selected_enhancement_ids` VARCHAR(500) on `application` table
- **Application.java:** comma-separated ID storage, helper methods (`getSelectedLosIdList()`, `hasServiceSelections()`)
- **ApplyForProposal.java:** saves LOS/Enhancement selections to Application; restores checkboxes on reload; duplicate key fix (LEFT JOIN FETCH p.application)
- **SaveApplicationProgress.java:** AJAX auto-save includes service selections
- **AmsDataGlobal.enhLosMap:** Map<Long,List<Long>> built from enhancement_los join table via JPQL, cached for Setup modal
- **SetupModalData.java:** added enhLosMap JSON — maps enhancement ID → parent LOS IDs
- **addActivityModal25.jsp Setup tab:** `aa_filterEnhancements()` shows enhancements when parent LOS checked (pure enhLosMap, no rateExtraMap dependency)
- **addActivityModal25.jsp Opportunity tab:** Rate dropdown + LOS checkboxes + `aa_onOppRateChange()` + `aa_prepareOppSubmit()` + validation requiring rate+LOS
- **CreateOpportunity.java:** creates linked Proposal with selected rate + LOS after Opportunity+CheckList; sets sourceActivity=opp
- **ProposalBuilder.java:** Rate→LOS map built from RateTable module with null checks
- **proposalDetail.jsp:** displays proposal LOS items
- **reviewApplication.jsp:** shows saved service selections with green check badges

## Session 53 (2026-03-11) — Application Enhancement Selection Bug Fix
- **Bug:** `fn:contains()` substring matching on comma-separated enhancement IDs caused false positives in `applyForProposal.jsp`
- **Example:** `fn:contains("15,3", "5")` → true because "5" is substring of "15"
- **Impact:** Selecting single-LOS enhancements phantom-selected multi-LOS enhancements; Modify panel pre-checked wrong boxes on every page reload, making them impossible to remove
- **Fix:** Comma-padded exact matching — `<c:set var="enhIdsCsv" value=",${selectedEnhancementIds},"/>` then `fn:contains(enhIdsCsv, ",${enh.getId()},")` — 6 occurrences fixed across service selection panel, summary bar, and modify panel
- **JSP-only change** — no Java or DB changes needed

## Session 54 (2026-03-11) — Extensible Chatbot Skill System
- **V046:** `chatbot_skill` table (13 columns, FK to assignee, composite index on psp_id+is_active)
- **ChatbotSkill.java** entity + **ChatbotSkillDAO.java** with scoring-based skill matching
- **ClaudeApiService.java:** 3 new overloads for structured content blocks (document + text)
- **ChatAssistant.java:** rewritten with `@MultipartConfig`, unified JSON + multipart endpoint, skill matching + fallback to KB search
- **SkillManager.java** + **skillManager25.jsp:** CRUD admin UI for chatbot skills, card grid with modal
- **AchFileUpload.java** deleted — replaced by unified ChatAssistant endpoint
- D-61 in deployment_backlog

## Session 55 (2026-03-11) — Composite Task Ordering
- **V047:** `composite_task_order` table — cross-sequence task ordering per activity type per PSP
- **CompositeTaskOrder.java** entity, **CompositeTaskView.java** DTO in `model/activity/checklist/sequences/support/`
- **CompositeOrderDAO.java** — CRUD + `getAllTasksForCategory()` (dedup across RequiredTaskLists) + `getCompositeOrderMap()`
- **ApplicationTaskDAO.java** — `applyCompositeOrder()` with fallback to legacy first-occurrence dedup
- **AddSetupModule25.java** — composite-aware module addition (composite sort_order for new ToDos)
- **SequenceBuilder25.java** — `handleCompositeRequest()` for `?composite={groupId}` mode
- **SequenceAction25.java** — `SAVE_COMPOSITE` action with manual JSON parsing
- **sequenceManager25.jsp** — composite UI: button bar, drag-and-drop task list, source sequence pills, save/cancel
- **PSP ID from session:** `local.getCurrentPerson().getPsp().getId()` (NOT `getCurrentPsp()`)
- D-62 in deployment_backlog — code complete, needs V047 applied + browser testing

## Universal Import System (V048, Sessions 56-58)
- **5 tables:** import_provider, import_file_type, import_field_mapping, import_plan_type_mapping, import_run_log
- **UniversalImport.java** — 4-step batch wizard: select provider → upload files → review/configure → results
- **UniversalImportService.java** — config-driven entity import with field mapping, transform rules, upsert patterns
- **ProviderSetup.java** — Admin CRUD for providers, file types, field mappings
- **SummitImportWizard.java** — Looks up SUMMIT provider, records import_id_mapping entries
- **4-entity import:** PLAN_TYPE → EMPLOYER → EMPLOYEE → BENEFIT (config-driven column mapping)
- **EMF pattern:** `getServletContext().getAttribute("emf")` — NOT `@PersistenceUnit`
- D-63 in deployment_backlog — code complete, needs V048 applied + browser testing

## Import Cross-Reference System (V051-V052, Session 63)
- **`import_id_mapping` table:** cross-reference for multi-provider PK resolution
- **Entity:** `model/imports/ImportIdMapping.java`, **Resolver:** `data/resolver/ImportIdResolver.java`
- Maps `(provider_id, entity_type, external_id)` → `internal_id` with `is_primary` flag
- **Phase 1:** V051 migration, ImportIdMapping entity, ImportIdResolver (resolve, record, allocate, resolvePlanType cascade)
- **Phase 2:** SummitImportService integration — accepts optional ImportProvider, records mappings
- **Phase 3:** UniversalImportService integration — resolver+fallback for all em.find() calls, PK conflict → allocateInternalId(), FK resolution via xref
- **Phase 4:** ImportTransitionManager servlet + importTransition.jsp — browse/search/link/unlink/togglePrimary/transferPrimary/bulkCSV
- **Phase 5:** V052 adds xref_resolved/pk_allocated/mappings_recorded to import_run_log; integrity checker methods on ImportIdResolver
- All 5 phases code-complete, compiles clean

## Phase A: Provider Setup Rework (V053, Session 64)
- **V053 migration:** `update_mode`, `mapping_status` on import_file_type; `is_fk`, `fk_entity_type` on import_field_mapping
- **ProviderSetup.java** rewrite:
  - `autoDetect()` — persistent sample file, unmapped rows display
  - `saveMappingsBulk()` — all-at-once table save with PK radio, FK checkbox+type, AMS field dropdowns
  - `validateAndUpdateStatus()` — PK/FK readiness checking, sets READY/PENDING status
- **fieldMappingEdit.jsp** — full rewrite: sample-file-driven table with PK radio, FK checkbox+type, AMS field dropdowns, bulk save, client-side validation
- **fileTypeList.jsp** — Status badge (PENDING/READY), Mode column (Create Only/Create & Update/Update Only), updateMode in add form
- **UPDATE_ONLY mode patch:** Third update mode. `validateAndUpdateStatus` skips FK requirements for UPDATE_ONLY (supplemental data refresh — only PK needed, no FK creation)
- Phase A code-complete, compiles clean

## Phase B1: Interactive Import Wizard Shell (Session 64)
- **InteractiveImportSession.java** (`data/service/`) — Session POJO with inner classes:
  - `EntityImportState`: entityType, fileTypeId, updateMode, filePath, fileName, totalRows, rows, counts, resolutionComplete, skipped, ImportResult
  - `ImportRow`: rowIndex, externalId, canonicalValues, displayLabel, status (MATCHED/SUGGESTED/UNMATCHED/CONFIRMED/MANUAL/SKIPPED/ERROR), amsInternalId, amsDisplayLabel, matchMethod, matchConfidence, errorMessage, candidates
  - `MatchCandidate`: internalId, displayLabel, matchMethod, confidence, detail
  - `ENTITY_ORDER = List.of("PLAN_TYPE", "EMPLOYER", "BENEFIT", "EMPLOYEE")`
  - `getAvailableEntityTypes()`, `advanceToNextEntity()`, `getStepNumber()`, `getTotalSteps()`
- **InteractiveImport.java** (`controller/data/`) — Wizard servlet at `/InteractiveImport`
  - `@WebServlet`, `@MultipartConfig`, session key `ii_session`
  - GET routing: step=1 (selectProvider.jsp), step=entity (entityStep.jsp), step=results (results.jsp)
  - POST actions: selectProvider (init session), skipEntity (mark skipped, advance), reset (clear session)
- **3 JSPs** in `interactiveImport/` folder:
  - `selectProvider.jsp` — Provider dropdown, dynamic step indicator
  - `entityStep.jsp` — Full-height flex layout, upload card + resolution table placeholder + step badges
  - `results.jsp` — Per-entity result cards with insert/update/skip/error colored dots
- **step1Provider.jsp** — Added Interactive Import link
- **Entity order:** PLAN_TYPE → EMPLOYER → BENEFIT → EMPLOYEE (benefit before employee)
- B1 code-complete, compiles clean. **B2 (Upload + Auto-Resolution) pending.**

## Reference
- Last session (64): Phase B1 Interactive Import wizard shell
- Phase B2-B5 pending (resolution engine, AJAX interactions, commit logic, polish)
- Full session archive: `docs/analysis/session_history_archive.md`
- Deployment backlog: `docs/deployment_backlog.md`
- Migration tracker: `docs/analysis/migration_tracker.md`
- Stashed memory source: `docs/claude_memory.md`
