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
- **Branch:** `feature/proposal-customization` (from `refactor/modernize-architecture`)
- **Latest migration:** V036
- **Session count:** 28
- V025-V033 applied to Demo PSP, BPO, and Master only; not yet applied to production or local dev
- V034 not yet applied anywhere (starter package template_key)
- V035-V036 not yet applied anywhere (proposal customization)

## Database Migrations
- Current highest version: **V036**
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

## Agency Creation (WIP)
- **Create Agency modal** expanded to two-column layout with Primary Contact / Agency Manager fields
- **AgencyAction.createAgency** now creates Person and sets as both `primaryContact` + `manager` on Agency
- **Status:** Form renders correctly, but backend function still not working as desired — needs debugging
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

## Full-Height Dashboard Layouts (Session 28)
- **BPO Dashboard:** `.bpo-layout` flex wrapper, `.bpo-columns` row, `.bpo-col-left`/`.bpo-col-right` flex columns — both scroll internally
- **PSP Dashboard:** `.psp-dash-body` flex wrapper, `.psp-dash-col-left`/`.psp-dash-col-right` flex columns — removed inline `max-height` from `.dash-scroll` divs
- **Both:** `@media (min-width: 992px)` only — mobile layout unchanged
- **Dropdown clipping fix:** `toDoCurrentList25.jsp` — pre-init kebab dropdowns with `popperConfig: { strategy: 'fixed' }` so menus aren't clipped by `overflow-y: auto` scroll containers

## Reference
- Full session archive: `docs/analysis/session_history_archive.md`
- Deployment backlog: `docs/deployment_backlog.md`
- Migration tracker: `docs/analysis/migration_tracker.md`
- Stashed memory source: `docs/claude_memory.md`
