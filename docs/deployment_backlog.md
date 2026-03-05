# Deployment Backlog

**Last Updated:** March 5, 2026
**Reference:** See `docs/deployment_strategy.md` for full context on each item.

Items are ordered by dependency (earlier items unblock later ones).

---

## Completed Items

### D-01: Create `ssa.properties` Loader ✅

**Completed:** February 23, 2026
**File:** `src/main/java/net/superiorstate/ams/AppConfig.java` (new)

Created `AppConfig` utility class that loads `/var/lib/tomcat10/conf/ssa.properties` at startup. Lookup order: (1) system property `-Dssa.config=/path/to/file` for dev machines, (2) fallback `{catalina.base}/conf/ssa.properties` for Linux VPSes. Called from `EmfListener.contextInitialized()` before EMF creation. Values accessible anywhere via `AppConfig.get(key, default)`.

---

### D-02: Fix Hardcoded Windows Path in `EmfListener` ✅

**Completed:** February 23, 2026
**File:** `src/main/java/net/superiorstate/ams/EmfListener.java`

`logStartupError()` now reads `LOG_PATH` from `AppConfig` with fallback to `catalina.base/logs`. No more hardcoded Windows path.

---

### D-03: Fix Hardcoded `SAVE_PATH` Default in `DatabaseInitializer` ✅

**Completed:** February 23, 2026
**Files changed:**
- `DatabaseInitializer.java` — Removed `SAVE_PATH` from `addPspConstants()` (no longer a DB constant)
- `AppConstantDAO.java` — Replaced two `getSavePath(em)` / `getSavePath(request)` methods with single `getSavePath()` that reads from `AppConfig`
- `AmsDataGlobal.java` — `setConstants()` now reads `SAVE_PATH` from `AppConfig` instead of DB
- Deleted dead servlets: `AddDocumentToActivity.java`, `AddFileToTask.java` (zero usages, used old local file save)

`SAVE_PATH` is now exclusively an infrastructure config value in `ssa.properties`.

---

### D-04: Remove Test Data from `DatabaseInitializer` ✅

**Completed:** February 23, 2026
**File:** `src/main/java/net/superiorstate/ams/data/service/DatabaseInitializer.java`

Removed:
- Fred Flintstone person (ID 50), Slate Rock and Gravel employer (ID -2), associated employee (ID -2), and person↔employee links
- Accelergent BPO persons (IDs 101, 102), their user accounts, and role assignments

Kept:
- User roles 101-103 (Accelergent BPO/Admin/User) — still seeded, just no users assigned

Demo benefits and ticket categories were subsequently moved to `SeedDemoData` (D-24) and removed from `DatabaseInitializer` (Session 9).

---

### D-05: Validate Deployment Key in `InitializeDataBase` ✅

**Completed:** February 23, 2026
**File:** `src/main/java/net/superiorstate/ams/controller/authentication/InitializeDataBase.java` (new)

Created the `InitializeDataBase` servlet (was previously missing entirely — form posted to a non-existent URL). Validates `deploymentKey` from form against `DEPLOYMENT_KEY` in `ssa.properties`. Rejects if key is missing, blank, or doesn't match.

---

### D-21: GitHub PAT ✅

**Completed:** February 23, 2026 (Session 1)

No expiration, read-only, scoped to Murphnd2/ams.

---

### D-23: Publish First Release ✅

**Completed:** February 23, 2026 (Session 1)

---

### D-24: Create Demo Seeder Servlet ✅

**Completed:** February 28, 2026
**Files:**
- `src/main/java/net/superiorstate/ams/controller/home/SeedDemoData.java` — Demo data seeder
- `src/main/java/net/superiorstate/ams/controller/home/ReSeedDb.java` — Factory-reset servlet
- `src/main/java/net/superiorstate/ams/controller/home/ReSeedDemoData.java` — Factory-reset + demo data
- `src/main/java/net/superiorstate/ams/data/service/DatabaseResetUtil.java` — Shared reset utility
- `src/main/java/net/superiorstate/ams/data/service/DatabaseInitializer.java` — Refactored (extracted `performInitialization(em)`)

**SeedDemoData** (`/SeedDemoData`) — PSP-Admin-only servlet that seeds comprehensive demo data onto a fresh initialized database. Idempotent via `DEMO_DATA_SEEDED` constant. Data created:
- 5 employers (Acme Manufacturing, Bright Horizons Childcare, Cascade Financial, Delta Regional Medical, Evergreen Landscaping) with 16 employees
- 12 benefits across FSA, HRA, HSA, COBRA, DCA, Dental, Vision
- 3 renewals (upcoming, in-progress, overdue), 2 setups, 5 tickets — all with linked checklists
- 2 BPO vendor users (Alex Rivera / BPO Admin, Priya Sharma / BPO User)
- 1 PSP staff user (Jennifer Martinez / PSP User) — all with password `demo123`

**ReSeedDb** (`/ReSeedDb`) — Factory-reset servlet. Requires PSP Admin session + deployment key from `ssa.properties`. Captures initialization values (PSP info, admin credentials, SMTP config), truncates all tables, re-initializes from saved state. Admin password hash/salt preserved across reset.

**ReSeedDemoData** (`/ReSeedDemoData`) — Extends ReSeedDb. Performs full factory reset, then seeds demo data via `SeedDemoData.seedAllDemoData()`. Same security requirements.

**Also in this session:** Fixed billing GUID 404 (`BillingQueryDAO`), redesigned `sendBillingForm.jsp` with CKEditor 5, context-aware billing link URL, branding fallback fix, Billing added to Admin navbar dropdown, `.gitignore` updated for `/out/` and `.claude/`.

---

### D-26: Vendor Management Admin Page ✅

**Completed:** March 2, 2026
**Files:**
- `src/main/java/net/superiorstate/ams/controller/user/VendorManager25.java` (new) — PSP admin page
- `src/main/webapp/WEB-INF/view/a/admin/vendorManager25.jsp` (new) — Admin UI
- `src/main/java/net/superiorstate/ams/controller/home/BpoPspClients.java` (new) — BPO admin page
- `src/main/webapp/WEB-INF/view/bpo/pspClients25.jsp` (new) — BPO admin UI

PSP-side Vendor Manager for managing BPO vendor partnerships (approve/reject requests, view API tokens, manage registrations). BPO-side PSP Clients page for managing PSP partnerships (request partnerships, view status, manage connections). Both accessible from navbar with system-type-aware visibility.

---

### D-27: Create Tomcat SSL Reference Doc ✅

**Completed:** February 23, 2026 (Session 2)
**File:** `docs/tomcat_ssl_setup.md`

Step-by-step guide covering Certbot certificate generation, Tomcat `server.xml` HTTPS connector configuration, HTTP→HTTPS redirect, privileged port binding, and auto-renewal with deploy hooks. Referenced from `docs/deployment_runbook.md` Phase 3.

---

### D-28: Save Blank Schema Dump on Master Image ✅

**Completed:** February 23, 2026 (Session 2)
**Location:** `/opt/ssa/schema/beta_ssa_blank.sql` on master VPS

Enables quick rollback: drop database, re-import blank schema, re-initialize. Referenced in deployment runbook rollback procedure.

---

### D-29: BPO Task Assignment from BPO Dashboard ✅

**Completed:** March 2, 2026
**Files:**
- `src/main/java/net/superiorstate/ams/controller/home/BpoCompleteTask.java` — Dual-mode assign (co-located + cross-system)
- `src/main/webapp/WEB-INF/view/bpo/bpoHome25.jsp` — Assign dropdown in task detail modal

BPO task assignment implemented as part of the cross-system BPO architecture. Both co-located mode (local ToDo.bpoAssignedTo) and cross-system mode (DelegatedToDo.assignedTo) supported. AJAX-powered via BpoCompleteTask servlet with `action=assign` parameter.

---

### D-32: Add BRANDING_PATH and Systemd Write Permission on Production ✅

**Completed:** February 27, 2026
**Priority:** HIGH — Required before deploying branding upload feature

**Steps performed on production:**

1. `BRANDING_PATH=/var/lib/tomcat10/branding/` was already in `ssa.properties`
2. Created directory with Tomcat ownership:
   ```bash
   sudo mkdir -p /var/lib/tomcat10/branding
   sudo chown tomcat:tomcat /var/lib/tomcat10/branding
   ```
3. Added systemd override to allow Tomcat to write to the branding directory (Tomcat 10 on Ubuntu uses `ProtectSystem=strict` which makes the filesystem read-only except for explicitly allowed paths):
   ```bash
   sudo systemctl edit tomcat10
   ```
   Added:
   ```ini
   [Service]
   ReadWritePaths=/var/lib/tomcat10/branding/
   ```
   Then:
   ```bash
   sudo systemctl daemon-reload && sudo systemctl restart tomcat10
   ```

**Also applies to:** Master VPS image — baked into the snapshot so cloned PSP instances work out of the box.

---

### D-33: Update Master VPS Blank Schema and Snapshot ✅

**Completed:** February 27, 2026
**Snapshot:** `SSA-Master-Base-v5-2026-02-27`

Updated master VPS image from v4 (pre-V001 schema) to v5:
- Replaced `/opt/ssa/schema/beta_ssa_blank.sql` with V024 structure dump from production
- Verified `ssa.properties` has all current keys (SYS_HEALTH_*, BRANDING_PATH, S3, chatbot, etc.)

---

### D-34: Benefit Renewal Audit Page ✅

**Completed:** March 1, 2026
**Files:**
- `src/main/java/net/superiorstate/ams/controller/activity/renewal/BenefitAudit.java` (new)
- `src/main/webapp/WEB-INF/view/a/renew/benefitAudit25.jsp` (new)
- `src/main/java/net/superiorstate/ams/data/dao/BenefitDAO.java` (new)
- `src/main/java/net/superiorstate/ams/model/summit/BenefitOverview.java` (new)
- `docs/migrations/V028__benefit_nextrenew_renew_months.sql`
- Multiple model/DAO files updated for `nextRenewalDue` and `renewalMonths` columns

**Benefit Renewal Audit:** New page at `/BenefitAudit` (PSP Admin only) — lists all active benefits with plan year data, detected renewal dates, inline editing of `nextRenewalDue` and `renewalMonths`, employer search, flagged/no-renewal filters, "Accept All Detected" bulk action.

**Also in this session:** DatabaseInitializer seed data overhaul (ServiceItem unification alignment, onboarding checklist rewrite, simplified LOS/Enhancement/PlanType seeding), form validation on `initialize.jsp`, ManageTask25 NPE fix, Agency `tax_id` column widened to `varchar(20)`.

---

### D-35: Benefit Renewal Audit Page ✅

**Completed:** March 1, 2026
(See D-34 — same session)

---

### D-36: User Manager — User Deactivation & Role Management ✅

**Completed:** March 1, 2026
**Migration:** V029 (`is_active BOOLEAN NOT NULL DEFAULT TRUE` on user table)
**Files:**
- `docs/migrations/V029__user_is_active.sql` — Migration script
- `src/main/java/net/superiorstate/ams/model/general/User.java` — Added `isActive` field
- `src/main/java/net/superiorstate/ams/controller/user/UserManager.java` — New servlet (GET=JSON, POST=AJAX actions)
- `src/main/webapp/WEB-INF/view/a/general/userManager25.jsp` — Modal with Create User + Manage Users tabs
- `src/main/java/net/superiorstate/ams/controller/authentication/AuthenticateUser.java` — Block deactivated users at login
- `src/main/java/net/superiorstate/ams/controller/authentication/OneTimeUserLogin.java` — Block deactivated users from GUID login
- `src/main/java/net/superiorstate/ams/data/dao/RecurringChecklistDAO.java` — Filter inactive from getPspUserList
- `src/main/java/net/superiorstate/ams/data/dao/AuthDAO.java` — Filter inactive from getPspStaff
- `src/main/java/net/superiorstate/ams/data/AmsDataGlobal.java` — Added refreshUserCaches()
- `src/main/webapp/WEB-INF/view/a/general/navbar25.jsp` — Modal trigger + import
- `src/main/java/net/superiorstate/ams/controller/user/CreateUser25.java` — Removed returnTo handling

PSP Admin modal for user lifecycle management: deactivate with bulk reassignment, reactivate, add/remove agent role (home agency scoped), expand agent to PSP User. BPO users excluded. External agency agent deactivation deferred.

---

### D-38: Simplified Sales Pipeline Seeding + Demo Users

**Completed:** March 1, 2026
**File:** `src/main/java/net/superiorstate/ams/data/service/DatabaseInitializer.java`

Replaced DataPath-specific sales pipeline seeding with generic baseline:
- **ServiceItems:** 3 specific (COBRA/FSA/Debit Cards) → 2 generic ("Line of Service"/"Service Enhancement")
- **LOS:** 2 specific (COBRA Administration/CDH) → 1 generic ("Line of Service")
- **Enhancement:** 1 specific (Debit Cards) → 1 generic ("Service Enhancement")
- **ServiceModules:** 3 specific → 2 generic (one per LOS, one per Enhancement), with direct FK links
- **Enhancement↔LOS M:N:** Populated so enhancement appears when LOS is selected
- **Pricing grid:** 4 RateTable rows (LOS × 3 fee types + Enhancement × 1 setup fee) on Standard Rate
- **Agency rate assignment:** Standard Rate assigned to PSP home agency
- **ApplicationSection join tables:** All 3 baseline sections linked to both LOS and Enhancement

Added 6 demo user accounts (all password `demo123`):
- `agency@pspdemo.com` — Agency Admin + Agent, manages Outside Agency (ID 15)
- `agent@pspdemo.com` — Sales Agent on Outside Agency
- `user@pspdemo.com` — PSP User
- `pspagent@pspdemo.com` — PSP User + Agent on home agency
- `bpoadmin@pspdemo.com` — BPO Admin
- `bpouser@pspdemo.com` — BPO User

New helper methods: `assignRateTable()`, `assignAllSectionsToLosAndEnhancement()`, `createDemoPerson()`

No migration required — initialization-only changes. Existing `ReSeedDb`/`ReSeedDemoData` inherit automatically.

---

### D-39: AJAX-Powered Setup Modal Data Loading

**Completed:** March 1, 2026
**Files:**
- `src/main/java/net/superiorstate/ams/controller/activity/setup/SetupModalData.java` (new)
- `src/main/webapp/WEB-INF/view/a/pspHome/columns/activities/addActivityModal25.jsp` (modified)
- `ServiceManagerAction.java`, `AgencyAction.java`, `SendInvitation.java`, `CreateProspect.java`, `CreateOpportunity.java` (modified)

The Setup tab in the Add Activity modal previously rendered dropdown data via server-side JSTL at page load. Cascade relationships were baked into `data-*` attributes on `<option>` elements. When admin tools modified rates/agencies/LOS, the modal HTML remained stale until full page refresh.

---

### D-40: ServiceManager Full Admin Page ✅

**Completed:** March 1, 2026
**Files:**
- `src/main/java/net/superiorstate/ams/controller/activity/setup/ServiceManagerAction.java` (major rewrite)
- `src/main/webapp/WEB-INF/view/a/admin/serviceManager25.jsp` (major rewrite)
- `src/main/java/net/superiorstate/ams/data/dao/ServiceItemDAO.java` (new)

Full admin page for managing ServiceItems, LOS, Enhancements, and their relationships. Accordion-based layout with inline create/edit/delete operations. ServiceItem CRUD, LOS↔Enhancement assignment, automatic ServiceModule creation.

---

### D-41: AJAX Opportunity Section + Agency → Prospect Cascade

**Completed:** March 1, 2026
**File:** `src/main/webapp/WEB-INF/view/a/pspHome/columns/activities/addActivityModal25.jsp`

Converted the Opportunity section of the Add Activity modal from server-rendered JSTL to AJAX-populated JavaScript, reusing the existing `SetupModalData` endpoint. No servlet changes needed.

---

### D-42: ServiceManager Enhancement Assignment + Feature Creation Fixes ✅

**Completed:** March 2, 2026
**File:** `src/main/java/net/superiorstate/ams/controller/activity/setup/ServiceManagerAction.java`

**Enhancement ↔ LOS assignment fix:** JSP forms sent `assignEnhancementToLos` / `assignLosToEnhancement` but servlet switch cases were `addEnhancementToLos` / `addLosToEnhancement`. Renamed cases to match. Added missing `contains()` guard.

**Feature creation fix:** `createLos` and `createEnhancement` auto-created ServiceItem but never created ServiceModule. Without a ServiceModule, the Add Feature UI never rendered. Added ServiceModule auto-creation to both.

**Note:** Existing LOS/Enhancement records created before this fix lack ServiceModules. A one-time backfill INSERT is needed for existing installations.

---

### D-43: Upcoming Renewals Page + MonthlyBiller Null Guard ✅

**Completed:** March 2, 2026
**Files:**
- `src/main/java/net/superiorstate/ams/controller/activity/renewal/UpcomingRenewals25.java` (new) — Standalone servlet
- `src/main/webapp/WEB-INF/view/a/renew/upcomingRenewals25.jsp` (new) — Full-page JSP
- `src/main/webapp/WEB-INF/view/a/general/navbar25.jsp` — Added Renewals nav link (PSP Users/Admins)
- `src/main/java/net/superiorstate/ams/data/service/MonthlyBiller.java` — Null guard in `logCoverageStatusForThisMonthCDH()`

New standalone page at `/UpcomingRenewals` replacing the existing modal-based renewal picker. Groups employer renewals by month (OVERDUE first, then chronological). Employer cards expand inline to show available benefits with pre-checked urgency flags. "Start Renewal" delegates to existing `AddRenewal25` servlet. Uses `RenewalQueryDAO.getEmployerRenewals()` with a bulk `MIN(nextRenewalDue)` query for accurate month grouping.

MonthlyBiller fix: Added `if (b == null || ee == null) continue;` null guard in `logCoverageStatusForThisMonthCDH()` matching the existing pattern in `logCoverageStatusForThisMonthPB()`.

No database changes.

---

### D-44: BPO Cross-System Architecture ✅

**Completed:** March 2, 2026
**Migrations:** V030 (`bpo_cross_system_foundation`), V031 (`todo_note_cross_system_nullable`)
**Files (new):**
- `src/main/java/net/superiorstate/ams/filter/ApiTokenFilter.java` — Bearer token auth on `/api/*`
- `src/main/java/net/superiorstate/ams/controller/api/PartnershipRequestApi.java` — BPO→PSP partnership requests
- `src/main/java/net/superiorstate/ams/controller/api/PartnershipApproveApi.java` — PSP approves/rejects partnerships
- `src/main/java/net/superiorstate/ams/controller/api/TaskReceiveApi.java` — BPO receives tasks from PSP
- `src/main/java/net/superiorstate/ams/controller/api/TaskUpdateApi.java` — BPO receives REVERT/RECALL/UPDATE
- `src/main/java/net/superiorstate/ams/controller/api/TaskNotesApi.java` — Cross-system note exchange
- `src/main/java/net/superiorstate/ams/controller/api/TaskCompletedCallbackApi.java` — PSP receives completion callbacks
- `src/main/java/net/superiorstate/ams/controller/api/NoteAddedCallbackApi.java` — PSP receives note callbacks
- `src/main/java/net/superiorstate/ams/data/service/BpoTaskPushService.java` — PSP pushes sourced tasks to BPO vendors
- `src/main/java/net/superiorstate/ams/data/util/ApiClient.java` — HTTP utility for cross-system calls
- `src/main/java/net/superiorstate/ams/model/activity/checklist/tasks/DelegatedToDo.java` — BPO-side task entity
- `src/main/java/net/superiorstate/ams/model/general/PspClient.java` — BPO-side PSP partnership entity

**Files (modified):**
- `AppConfig.java` — `isPsp()`, `isBpo()`, `getSystemType()` from `ssa.properties`
- `EmfListener.java` — Added DelegatedToDo, PspClient to managed entities
- `AmsDataGlobal.java` — Conditional loading by system type
- `BpoRegistration.java` — API tokens, partner_url, `isAvailable()` helper
- `ToDoNote.java` — Nullable toDo/createdBy, authorName, `getDisplayAuthor()`
- `BpoHome.java` — Dual-mode: cross-system DelegatedToDo vs co-located ToDo
- `bpoHome25.jsp` — Dual rendering with JS cross-system awareness
- `BpoCompleteTask.java` — Dual-mode complete/note/assign with PSP callbacks
- `BpoGetNotes.java` — todoGuid query support, null-safe author display
- `AddRenewal25.java`, `CreateChecklist25.java` — BpoTaskPushService push hooks
- `navbar25.jsp` — System-type-aware admin links

Full cross-system BPO architecture enabling PSP and BPO deployments to exchange tasks, notes, and completion status via authenticated REST APIs.

---

### D-45: Master VPS Image Fix — v7 Snapshot ✅

**Completed:** March 2, 2026
**Snapshot:** `SSA-Master-Base-v7-2026-03-02`

Fixed critical MySQL configuration issues discovered during BPO VPS standup. Changes applied to master VPS (208.94.39.77 / `master.superiorstate.biz`):

1. **`lower_case_table_names = 1`** — MySQL 8 on Linux defaults to case-sensitive table names. EclipseLink generates uppercase queries (`SELECT ... FROM ACTIVITYSTATUS`), which fail on case-sensitive MySQL. Required reinitializing the MySQL data directory (`--initialize-insecure`).
2. **`ams_app` MySQL user recreated** — After data directory reinit, all users were wiped. Recreated `ams_app` with password matching `context.xml`, using interactive MySQL shell (not `-e` flag) to avoid bash escaping special characters.
3. **V031 schema + schema_version reimported** — Schema dump from `dev_ssa` (structure-only) plus `schema_version_migration.sql` for 31 version tracking records.
4. **`SYSTEM_URL=` placeholder verified** — Added by WS2, needed by VendorManager for partnership requests. Blank on master, set during provisioning.
5. **Clean state** — Temp files removed, no WAR, logs cleared, Tomcat stopped before snapshot.

---

### D-48: Apply V033 Migration (BPO Note Attachments) ✅

**Completed:** March 4, 2026

Applied to Demo, BPO, and Master via release V0.37.0 (demo/BPO already had V033 from v0.33.0 release; master applied manually).

---

### D-49: Update Healthcheck Script + Config on Master VPS Image ✅

**Completed:** March 4, 2026

Fixed healthcheck.sh deployed to master from repo. `ams_app` password verified matching across MySQL user, `context.xml`, and `ssa.properties`. Bundled into v8 snapshot.

---

### D-55: Fix update.sh Duplicate Insert Bug ✅

**Completed:** March 4, 2026

The `update.sh` script's post-migration `INSERT INTO schema_version` conflicted with the self-registering `INSERT IGNORE` inside each migration SQL file, causing `ERROR 1062 (Duplicate entry)` and aborting the update. Fixed by changing to `INSERT IGNORE INTO schema_version` on all three VPS boxes (demo, BPO, master). Must also be applied to the repo copy of the script and baked into the next master snapshot.

---

### D-56: Master VPS v8 Snapshot ✅

**Completed:** March 4, 2026

Snapshot: `SSA-Master-Base-v8-2026-03-04`. Changes from v7: schema upgraded V031→V037, `update.sh` INSERT IGNORE fix, fixed `healthcheck.sh` from repo, `ams_app` MySQL password realigned, blank schema dump updated to V037. Clean state: no WAR, logs cleared, PSP_ID=UNINITIALIZED, SYSTEM_URL blank, Tomcat stopped.

---

## Open Items

### D-07: Externalize Database Connection

**Priority:** MEDIUM — Currently working via JNDI in context.xml
**Status:** Deferred

The database connection is currently configured via Tomcat JNDI datasource in `context.xml`, which is outside the WAR. This works for multi-PSP deployment. Moving it to `ssa.properties` is a future nice-to-have but not blocking.

---

### D-14: Master Admin Dashboard & PSP Instance Management

**Priority:** LOW — Phase 2 (after health check emails are operational)
**Status:** Not started

Build a dashboard view in the AMS application, accessible only to "Master Admin" role. This serves as the central management console for all PSP instances.

**Features:**
- Receives health check data from all PSP VMs and displays status grid
- PSP instance registry: list of all deployed PSPs with status, domain, version, last backup
- Deployment feedback: surface errors, version drift, failed updates
- "Master Admin" concept that manages PSP installs

**Architecture decision:** Build within the AMS app (role-gated) rather than a separate website. The infrastructure and auth system already exist.

**When built:** Push migration to set `SYS_HEALTH_ENABLED=false` across all PSPs to stop email reports. Update deployment runbook Phase 7 to reference the new dashboard workflow.

---

### D-22: Investigate IONOS DCD Resource Limits

**Priority:** MEDIUM
**Status:** In progress — IP block reservation error encountered

Investigate IONOS Cloud DCD account-level limits on:
1. **IP block reservations** — Received "error occurred while reserving ip block" when attempting to reserve a second static IP block via DCD IP Manager (Menu → Network Services → IP Management). Root cause unknown. First block reservation (for production) succeeded; second (for BPO VPS) failed.
2. **vCPU core quota** — Determine if there's a per-account limit on total vCPU cores across all VMs in the VDC.

**Workaround (current):** BPO VPS at `bpo.superiorstate.biz` (158.222.102.168) is running on a DHCP-assigned IP, which is stable unless the VM is deallocated. Acceptable for demo/test; reserve a static IP for production BPO if the IP block issue is resolved.

**Next steps:** Contact IONOS Cloud support to clarify account limits and request quota increase if needed. Reference DCD contract and SSA-PSP VDC (US-Las Vegas).

---

### D-25: Manual Benefit Creation UI

**Priority:** MEDIUM
**Status:** Not started

Build an admin page to manually create Benefit records without requiring a Summit import. Currently benefits can only enter the system through the Summit CSV import pipeline or the demo data in the initializer.

---

### D-30: BPO Automation Email — Contact-less Checklist Support

**Priority:** MEDIUM
**Status:** Not started (design discussion completed)

The ManageTask25 automation feature uses the activity's primary contact for email To:/Cc: fields. Checklists have no primary contacts, so automation is currently hidden for BPO users. Design needed: in the absence of valid email addresses, show a prompt for manual email entry (validated, semicolon-separated or add-one-at-a-time). This benefits all standalone checklists, not just BPO.

---

### D-31: Microsoft 365 SSO (Optional Per-PSP)

**Priority:** LOW — Phase 2 (after multi-PSP foundation is stable)
**Status:** Not started

Add optional "Sign in with Microsoft" button on login page using Microsoft Entra ID (Azure AD) with OpenID Connect / OAuth 2.0. Existing username/password auth remains as the default and fallback.

**Design considerations:**
- Per-PSP opt-in: each PSP decides whether to enable SSO via a `SSO_ENABLED` database constant
- Azure AD app registration: one multi-tenant app or per-PSP registrations (TBD)
- OAuth callback URLs must be domain-specific (each PSP has its own domain)
- User matching: SSO email matched to existing AMS user record — no auto-provisioning initially
- Fallback required for users without M365 accounts (agents, clients, external contacts)
- Session setup must follow same `AmsDataLocal` initialization path as normal login

**Implementation approach (when ready):**
- Add `microsoft-identity-web` or manual OAuth 2.0 authorization code flow
- New servlet: `MicrosoftLoginCallback` to handle the OAuth redirect
- Login page: conditional "Sign in with Microsoft" button when `SSO_ENABLED=true`
- Map authenticated email → `User.email` → full `loadSessionData25()` flow

---

### D-46: Seed MASTER_REGISTRY_URL Constant on Existing PSPs

**Priority:** MEDIUM — Required after deploying approved vendors WAR
**Status:** Not started

After deploying the approved vendors WAR, insert the constant on existing PSP deployments:
```sql
INSERT IGNORE INTO constant (constant_name, constant_value) VALUES ('MASTER_REGISTRY_URL', 'https://superiorstate.biz');
```
New deployments get this automatically via DatabaseInitializer.

---

### D-47: Populate Approved Vendors on Master/Production

**Priority:** MEDIUM — Required after deploying V032 WAR to master
**Status:** Not started

After deploying V032 WAR to master and production, insert the BPO VPS as an approved vendor:
```sql
INSERT INTO approved_vendors (vendor_name, vendor_url, description, is_active, date_added)
VALUES ('Accelergent BPO Services', 'https://bpo.superiorstate.biz', 'DataPath subsidiary — claims processing, data entry, compliance support', TRUE, CURDATE());
```
Additional vendors can be added via direct SQL on the master installation. A future admin UI for managing the registry is a backlog item.

---

### D-50: Set Unique Hostnames on VPS Boxes

**Priority:** LOW
**Status:** Not started

All cloned VPS boxes have hostname `ubuntu` (inherited from master image). Health report emails all show `Hostname: ubuntu` making them hard to distinguish.

**Action:** Set unique hostnames on each VPS:
- Demo PSP: `sudo hostnamectl set-hostname demo-psp`
- BPO: `sudo hostnamectl set-hostname bpo`
- Production: `sudo hostnamectl set-hostname production`

**Master image:** Leave as `ubuntu` or set to `uninitialized` — set during provisioning per Section 8.1 of deployment_strategy.md.

---

### D-51: Configure BPO VPS Backup Cron + Wasabi

**Priority:** MEDIUM
**Status:** Not started

The BPO VPS (bpo.superiorstate.biz) has no backup cron configured and no Wasabi uploads. Demo PSP is backing up correctly.

**Action:**
1. Verify `/opt/ssa/scripts/backup.sh` exists and is executable on BPO
2. Verify Wasabi credentials in `ssa.properties` on BPO
3. Add backup cron: `0 2 * * * /opt/ssa/scripts/backup.sh`
4. Test: run backup script manually, confirm local file + Wasabi upload

---

### D-52: Reduce Healthcheck Error Log Noise

**Priority:** LOW
**Status:** Not started

Health reports show 250-300+ errors/day, mostly bots and scanners. Real errors get drowned out.

**Common noise patterns:**
- `userRoleList is null` NPE — unauthenticated requests hitting protected pages
- `Error parsing HTTP request header` / `Invalid character found` — bot garbage requests
- `Connection reset by peer` / `Failed to send ping` — dropped HTTP/2 connections

**Possible approaches:**
- Add null guard for `userRoleList` in the code path that iterates it
- Filter healthcheck error grep to exclude known noise patterns
- Or both

---

### D-53: Starter Packages for Application Sections

**Priority:** MEDIUM
**Status:** Migrations applied to Demo/BPO/Master — pending browser testing

**Prerequisite:** V034 migration (`template_key` column on `applicationsection`)

New "Load Starter Package" feature on the Service Manager page. PSP admins can load pre-configured sets of ApplicationSections with fields from bundled JSON templates. 8 packages available: General Employer Setup, Pre-Tax/S125, FSA, HRA, HSA, Transit/Parking, Billing & Payments, Specialty Benefits. Duplicate detection via `template_key` column — re-loading a package skips already-loaded sections.

**Files:**
- `V034__starter_package_template_key.sql` — migration
- `ApplicationSection.java` — `templateKey` field
- `src/main/resources/packages/` — 9 JSON files (index + 8 packages)
- `PackageLoader.java` — service class
- `ServiceManagerHome.java` — passes available packages to JSP
- `ServiceManagerAction.java` — `loadStarterPackage` action
- `serviceManager25.jsp` — modal + flash message

---

### D-54: Proposal Customization — Apply V035+V036 and Browser Test

**Priority:** MEDIUM
**Status:** Migrations applied to Demo/BPO/Master — pending browser testing

**Prerequisites:** V035 + V036 migrations applied to target environment

Proposal customization feature: composable section-based proposal layout with PSP admin editor. Feature sales blurb upgrade (headline + expanded description), CKEditor 5 for TITLE/CLOSING/CUSTOM page editing, drag-drop section reorder, merge token replacement, HTML sanitization. Auto-initializes default sections on first access.

**Files:**
- V035 migration (feature headline + description widening)
- V036 migration (proposal_section table, FK→assignee)
- ProposalSection.java entity
- ProposalSettings.java servlet + proposalSettings.jsp
- Feature.java, ServiceManagerAction.java, ViewProposal.java updates
- viewProposal.jsp section-based rendering + proposalFeatures.jsp + proposalPricing.jsp includes
- navbar25.jsp (Proposal Settings link)

---

### D-37: Backfill PSP Home Agency Config

**Priority:** HIGH
**Status:** Not started

On existing installations, run one-time SQL to:
1. Add Agent (2) and Agency Admin (8) roles to the PSP admin user
2. Set `manager_id` on the home agency to the PSP admin's person ID
3. Insert `PSP_HOME_AGENCY_ID` constant with the home agency's ID

This aligns existing databases with the updated DatabaseInitializer behavior.

---

### D-57: Apply V040 + Recurring Checklist History

**Priority:** MEDIUM
**Status:** Code complete — needs V040 applied + browser testing

**Prerequisite:** V040 migration (`recurring_series_id` + `recurring_cycle_number` on `delegated_todo`)

Recurring checklist history tracking: each recurring cycle gets a UUID series ID and incrementing cycle number. PSP and BPO both get "View History" links showing past cycles with completion dates, completed-by names, and task counts. CSV export for application data also included (no migration needed).

**Files:**
- `V040__delegated_todo_recurring_series.sql` — migration
- `DelegatedToDo.java` — new fields
- `BpoTaskPushService.java` — series ID propagation
- `RecurringChecklistDAO.java` — history query
- `ViewRecurringHistory25.java`, `BpoRecurringHistory.java` — servlets
- `checklistHistory25.jsp` — shared view
- `ExportApplicationCsv.java` — CSV export servlet
- `AgentHome.java`, `agentHome25.jsp` — closed opportunity lookup + export integration
- `detailOpportunity25.jsp` — CSV export button

---

### D-58: Apply V041 + Application Visibility & Role Walls

**Priority:** MEDIUM
**Status:** Code complete — needs V041 applied + browser testing

**Prerequisite:** V041 migration (`reviewed_by`, `review_notes`, `date_reviewed` on application table)

Application visibility and role-based review controls. PSP Users/Admins get a new Applications hub page showing all in-flight and submitted applications across the PSP. PSP Admins can take over unmanaged opportunities and perform review actions (approve/deny/request more info). Agents can view applications in read-only mode with CSV export. Review actions are hard-gated to PSP Admin role (403 on POST for non-admins).

**Files:**
- `V041__application_reviewer_fields.sql` — migration (conditional DDL)
- `ApplicationsHome.java` — new hub servlet
- `applicationsHome25.jsp` — new hub page
- `ReviewApplication.java` — role gates, agent access check, CSV export
- `reviewApplication.jsp` — conditional UI, read-only banner, reviewer info
- `navbar25.jsp` — Applications nav link
