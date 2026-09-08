# Deployment Backlog

**Last Updated:** 2026-05-01
**Reference:** See `docs/deployment_strategy.md` for full context on each item.

Items are ordered by dependency (earlier items unblock later ones).

⚠️ **Maintenance note (added 2026-07-30):** migration-status claims below were reconciled
2026-07-30 against a live `schema_version` probe and `docs/analysis/migration_tracker.md`
(V001–V073 confirmed applied to production; V074/V075 not). Status must be back-filled
*after a deployment actually succeeds*, not only when an item is authored — the same
maintenance rule added to `migration_tracker.md` on the same date, for the same reason:
this is exactly how the drift accumulated.

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

### D-67: Master VPS v9 Snapshot ✅

**Completed:** March 20, 2026

Snapshot: `SSA-Master-Base-v9-2026-03-20`. Changes from v8:
- Schema upgraded V037→V057, blank schema dump updated to V057
- Nginx + python3-certbot-nginx installed (SSL now via nginx reverse proxy, not Tomcat-direct)
- Systemd ReadWritePaths added for `/var/lib/tomcat10/data/` (D-66)
- `CHATBOT_ENABLED` and `ANTHROPIC_API_KEY` removed from ssa.properties (D-58 — now self-service via UI)
- `update.sh` synced from repo (version-sort fix)
- Hostname set to `ssa-master` (D-50)
- All VPS hostnames set: `ssa-production`, `ssa-demo`, `ssa-bpo`, `ssa-master`
- SSL migrated on production from Comodo wildcard to Let's Encrypt via nginx (certbot nginx authenticator)
- `backup.sh` added to repo (`docs/scripts/backup.sh`) with LD_LIBRARY_PATH workaround

Clean state: no WAR, logs cleared, PSP_ID=UNINITIALIZED, SYSTEM_URL blank, Tomcat stopped.

---

### D-58: Self-Service Anthropic API Key Management

**Priority:** HIGH — Required before multi-PSP deployment (prevents company key leaking to other installs)
**Status:** Code complete, needs browser testing

Self-service AI key management via Settings modal. AI features start OFF on fresh installs. PSP admins enable by entering a valid Anthropic API key (validated against Anthropic API before saving). Key stored in DB `constant` table (checked first), `ssa.properties` fallback for dev. Chatbot admin-only by default; "Show chatbot to all users" toggle available. No DB migration — constant rows created dynamically. "AI Setup Guide" skill seeded on init.

**Deployment notes:**
- Remove `CHATBOT_ENABLED` from ssa.properties (no longer used)
- Remove `ANTHROPIC_API_KEY` from ssa.properties on production/deployed installs (admins enter via UI)
- Keep `ANTHROPIC_API_KEY` in dev ssa.properties for convenience (fallback still works)

---

### D-68: Phase 3a — Tomcat RemoteIpValve ✅

**Completed:** 2026-05-01  
**Applied to:** Production only — Demo, BPO, Master pending (see D-74)

Added `RemoteIpValve` to `/var/lib/tomcat10/conf/server.xml` on the production VPS, inside the `<Host>` block. Also set `requestAttributesEnabled="true"` on the existing `AccessLogValve` so the access log records real client IPs.

**What this fixed:**
- `request.getScheme()` returning `"http"` instead of `"https"` behind nginx → now returns `"https"`
- `request.getServerPort()` returning `8080` → now returns `443`
- `request.getRemoteAddr()` returning `127.0.0.1` → now returns real client IP
- `request.isSecure()` returning `false` → now returns `true`
- `NdtAccessLog.ipAddress` recording `127.0.0.1` for every audit entry → now records real client IP
- Outbound emails (proposals, agency invitations, billing notifications) containing `http://host:8080/...` links → now generate correct `https://host/...` links

**Reference:** `docs/analysis/proxy_readiness_audit.md` §4, §13 P0  
**Config copy:** `docs/infrastructure/configs/server.xml`

---

### D-69: Phase 3b — Let's Encrypt DNS-01 via Cloudflare ✅

**Completed:** 2026-05-01  
**Applied to:** Production only

Switched certbot from HTTP-01 (nginx authenticator) to DNS-01 (Cloudflare DNS API) on the production VPS.

**Changes made:**
- Installed `python3-certbot-dns-cloudflare` (apt)
- Created `/etc/letsencrypt/cloudflare.ini` (mode 600, root-owned) with a Cloudflare API token scoped to DNS:Edit on `superiorstate.biz` and `superiorstate.net`
- Re-issued both certs via `sudo certbot certonly --dns-cloudflare ...`
- Verified renewal config at `/etc/letsencrypt/renewal/superiorstate.biz.conf` and `.../superiorstate.net.conf` shows `authenticator = dns-cloudflare`

**Why:** HTTP-01 renewal is incompatible with Cloudflare proxy (orange cloud). DNS-01 is proxy-agnostic.

**Reference:** `docs/infrastructure/letsencrypt_renewal.md`, `docs/analysis/proxy_readiness_audit.md` §6

---

### D-70: Phase 3c — Cloudflare Proxy Enabled on Production ✅

**Completed:** 2026-05-01

DNS for both `superiorstate.biz` and `superiorstate.net` was migrated from GoDaddy nameservers to Cloudflare nameservers, and both domain registrations were transferred from GoDaddy to Cloudflare Registrar (at-cost pricing). Cloudflare proxy (orange cloud) was then enabled on the apex and www A records for both zones.

**Changes made:**
- Nameserver delegation: GoDaddy → `dylan.ns.cloudflare.com` / `isabel.ns.cloudflare.com`
- Domain registrar: GoDaddy → Cloudflare Registrar
- SSL/TLS mode set to **Full (strict)** on both zones
- A records for apex + www: gray cloud → orange cloud (proxied)
- Email records (MX, SPF, DKIM, DMARC, autodiscover, em102001): explicitly DNS-only
- Installed `/etc/nginx/conf.d/cloudflare-real-ip.conf` on production VPS — declares Cloudflare IP ranges as trusted, rewrites nginx `$remote_addr` from `CF-Connecting-IP` header so downstream real IPs are correct

**Reference:** `docs/infrastructure/cloudflare_setup.md`, `docs/infrastructure/production_architecture.md`  
**Config copy:** `docs/infrastructure/configs/cloudflare-real-ip.conf`

---

## Open Items

### D-71: Phase 3d — URL Generation Cleanup (Deferred)

**Priority:** LOW — Not blocking; RemoteIpValve (D-68) already produces correct URLs  
**Status:** Deferred

Five servlets build outbound URLs (sent in emails) from raw `request.getScheme()` + `request.getServerName()` + `request.getServerPort()` instead of from the `WEB_PATH` DB constant. These now work correctly because D-68's RemoteIpValve restores the real scheme/port, but they are fragile: if the valve is ever misconfigured or removed, the links break silently.

**Servlets to migrate:**

| Servlet | Link type |
|---------|-----------|
| `ProposalDetail.java` | Proposal share link in email to prospect |
| `SendProposal.java` | Proposal share link in outbound email |
| `SendInvitation.java` | Agency invitation link emailed to agents |
| `EmailBillingToEmployer.java` | Billing detail link emailed to employers |
| `125eligibility.jsp` | Return URL for NDT eligibility questionnaire |

**Target pattern:** Read `WEB_PATH` DB constant (as `HelpUserLogin` and `CreateUser25` already do), then fall back to the `SYSTEM_URL` from `AppConfig`. Do not use raw `request.getScheme()` for externally-delivered links.

**Reference:** `docs/analysis/proxy_readiness_audit.md` §2, §13 P1

---

### D-72: Phase 3e — TPA Deployment Readiness (Deferred)

**Priority:** LOW — Required before offering AMS to non-SSA TPAs  
**Status:** Deferred

Four SSA-specific hard-coded values need to be externalized before a TPA could deploy AMS under their own brand without code changes:

| Location | Hard-coded value | What to do |
|----------|-----------------|-----------|
| `CreateUser25.java:330` + `HelpUserLogin.java:89` | `"noreply@superiorstate.net"` FROM address | Add `NOREPLY_EMAIL` DB constant seeded from initialization form |
| `DatabaseInitializer.java:878` | `MASTER_REGISTRY_URL` defaults to `"https://superiorstate.net"` | Seed from `ssa.properties` `MASTER_URL` key or leave blank |
| `AmsDataGlobal.java:395` | `WEB_PATH` fallback = `"https://superiorstate.biz/"` | Change fallback to empty string or generic placeholder |
| `DocumentConstants.java` | SharePoint links + `DOC_PATH` = SSA SharePoint/docs | Convert to DB constants or configuration table |

**Reference:** `docs/analysis/proxy_readiness_audit.md` §7, §12, §13 P1

---

### D-73: Phase 3f-1 — Origin Firewall (Recommended Soon)

**Priority:** MEDIUM — Without this, the Cloudflare proxy provides IP hiding but not true access control  
**Status:** ⚠️ Appears implemented — verify & close. External probe (2026-07-15): direct port-443 to the origin **times out** from a non-Cloudflare host (`Test-NetConnection 66.179.248.171 -Port 443`), so direct access is now blocked. Confirm the VPS mechanism (UFW Cloudflare-range rules vs. tunnel) and close this item; the HSTS sub-item below may still be outstanding.

The production VPS public IP (66.179.248.171) previously accepted inbound connections on port 443 from any source, letting an attacker who discovered the origin IP bypass Cloudflare. The 2026-07-15 probe indicates that path is now closed.

**Recommended implementation:** UFW rules restricting port 443 to Cloudflare's published IP ranges, plus an automated update mechanism to keep the rules current.

```bash
# Example: deny all :443, then allow Cloudflare ranges
sudo ufw default deny incoming
sudo ufw allow from 103.21.244.0/22 to any port 443
sudo ufw allow from 173.245.48.0/20 to any port 443
# ... (all ranges from docs/infrastructure/configs/cloudflare-real-ip.conf)
```

**Auto-update consideration:** Cloudflare rarely changes IP ranges but does occasionally. A cron script that fetches `https://www.cloudflare.com/ips-v4` and `https://www.cloudflare.com/ips-v6` and diffs against the UFW ruleset would prevent silent access loss if ranges change.

**Escape hatch:** SSH (port 22) must remain open from the operator's IP or a bastion. The `ufw allow ssh` rule must be in place before restricting port 443.

**Also bundle:** HSTS header (`Strict-Transport-Security: max-age=31536000; includeSubDomains`) in nginx site config.

---

### D-74: Phase 3f-2 — Demo/BPO/Master VPS Standardization

**Priority:** MEDIUM — Master image is the template; misalignment affects all future clones  
**Status:** Not started

The three non-production VPSes use the pre-Phase-3 architecture. Decisions needed before the next master snapshot:

| VPS | Current SSL | Current proxy | RemoteIpValve? |
|-----|------------|--------------|----------------|
| `demo.superiorstate.biz` | Let's Encrypt, nginx plugin (HTTP-01) | None (direct) | No |
| `bpo.superiorstate.biz` | TODO(verify) — may be Tomcat-direct or nginx | None | No |
| `master.superiorstate.biz` | Snapshot v9 has nginx+certbot-nginx installed | None (stopped) | No |

**Recommended action:**
1. Decide whether demo/BPO should be put behind Cloudflare proxy (probably yes for consistency)
2. Apply D-68 (RemoteIpValve) to master image so all future clones inherit it
3. Switch demo and BPO to DNS-01 certbot (same Cloudflare API token works for all subdomains if token is scoped correctly)
4. Take master snapshot v10 after changes

**Note:** Demo and BPO use subdomains (`demo.superiorstate.biz`, `bpo.superiorstate.biz`). The Cloudflare API token for certbot DNS-01 is already scoped to `superiorstate.biz` zone — it covers all subdomains.

---

### D-75: Phase 3f-3 — SameSite Cookie Configuration

**Priority:** LOW — CSRF risk is low in current threat model; no user-reported issues  
**Status:** Not started

AMS `web.xml` has no `<session-config>` element. Tomcat 10 defaults apply: session cookie is `HttpOnly=true`, `Secure` is now set (via RemoteIpValve, D-68), but `SameSite` is not configured — behavior depends on browser default (which varies across browsers and versions).

**Recommended change to `web.xml`:**

```xml
<session-config>
  <cookie-config>
    <http-only>true</http-only>
    <secure>true</secure>
  </cookie-config>
</session-config>
```

Note: `SameSite` attribute is not directly configurable in servlet `web.xml` as of Servlet 5.0 (Jakarta EE 9). It requires either a Tomcat-specific `sameSiteCookies` attribute on the `Context` element in `context.xml`, or a filter that rewrites the `Set-Cookie` header.

**Tomcat context.xml approach:**
```xml
<Context sameSiteCookies="strict">
```

This is a low-risk application code change but is bundled here as a deployment/config concern since it touches both `web.xml` and `context.xml`.

---

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

### D-50: Set Unique Hostnames on VPS Boxes ✅

**Completed:** March 20, 2026

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
**Status:** Code complete — V040 applied to production (verified 2026-07-30 via `schema_version` probe) — needs browser testing

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

### D-80: Apply V041 + Application Visibility & Role Walls

**Priority:** MEDIUM
**Status:** Code complete — V041 applied to production (verified 2026-07-30 via `schema_version` probe) — needs browser testing

**Prerequisite:** V041 migration (`reviewed_by`, `review_notes`, `date_reviewed` on application table)

Application visibility and role-based review controls. PSP Users/Admins get a new Applications hub page showing all in-flight and submitted applications across the PSP. PSP Admins can take over unmanaged opportunities and perform review actions (approve/deny/request more info). Agents can view applications in read-only mode with CSV export. Review actions are hard-gated to PSP Admin role (403 on POST for non-admins).

**Files:**
- `V041__application_reviewer_fields.sql` — migration (conditional DDL)
- `ApplicationsHome.java` — new hub servlet
- `applicationsHome25.jsp` — new hub page
- `ReviewApplication.java` — role gates, agent access check, CSV export
- `reviewApplication.jsp` — conditional UI, read-only banner, reviewer info
- `navbar25.jsp` — Applications nav link

---

### D-60: Proposal AI Page Builder

**Priority:** MEDIUM
**Status:** Code complete — no migration needed, deploy WAR

Inline AI assistant for building styled HTML proposal custom pages. Adds "Build with AI" button to TITLE/CLOSING/CUSTOM section editors in Proposal Settings. Uses Claude Sonnet with 24-chunk knowledge base covering the card-inset pattern, scoped CSS, merge tokens, and layout patterns. Multi-turn conversation with code canvas (dual Code/Preview tabs, Insert into Editor, Copy). Requires CLAUDE_API_KEY in ssa.properties (already configured on all environments from D-48+).

**Prerequisites:** V035+V036 applied (D-54), CLAUDE_API_KEY in ssa.properties

**Files:**
- `ProposalAiBuilder.java` (new servlet)
- `proposal-page-builder.json` (new KB)
- `knowledge-config.json` (updated)
- `ClaudeApiService.java` (new overload, timeout increase)
- `ProposalSettings.java` (redirect fix)
- `proposalSettings.jsp` (AI panel, bug fixes)
- `.claude/skills/proposal-content-page/SKILL.md` (dev-only skill)

---

### D-61: Apply V046 + Chatbot Skill System

**Priority:** MEDIUM — New feature, no dependencies on existing data
**Status:** Code complete — V046 applied to production (verified 2026-07-30 via `schema_version` probe) — needs skill creation via UI

Deploy steps:
1. Apply `docs/migrations/V046__chatbot_skill_table.sql` to target databases
2. Deploy WAR
3. Navigate to Skill Manager (Business Efficiency → Chatbot Skills)
4. Create ACH Report Analyzer skill with system prompt from session notes

**Files (new):**
- `V046__chatbot_skill_table.sql`, `ChatbotSkill.java`, `ChatbotSkillDAO.java`
- `SkillManager.java`, `skillManager25.jsp`

**Files (modified):**
- `ClaudeApiService.java`, `ChatAssistant.java`, `chatAssistant25.jsp`, `navbar25.jsp`

---

### D-62: Apply V047 + Composite Task Ordering

**Priority:** MEDIUM — New feature, no dependencies on existing data
**Status:** Code complete — V047 applied to production (verified 2026-07-30 via `schema_version` probe) — needs browser testing

**Prerequisite:** V047 migration (`composite_task_order` table)

Cross-sequence composite task ordering for multi-LOS/Enhancement setups. PSP admins define master task ordering across all sequences of a given activity type (Setup, Renewal, Ticket) via the Sequence Manager's new "Composite Order" view. When a Setup activity is created with multiple LOS/Enhancements, tasks are populated in composite order instead of arbitrary per-sequence order. Reusable tasks shared across sequences are deduplicated. Falls back to existing behavior when no composite order is defined.

**Files (new):**
- `V047__composite_task_order.sql`, `CompositeTaskOrder.java`, `CompositeTaskView.java`, `CompositeOrderDAO.java`

**Files (modified):**
- `ApplicationTaskDAO.java`, `AddSetupModule25.java`, `SequenceBuilder25.java`, `SequenceAction25.java`, `sequenceManager25.jsp`

---

### D-63: Apply V048 + Universal Import System

**Priority:** MEDIUM — New feature, no dependencies on existing data
**Status:** Code complete — V048 applied to production (verified 2026-07-30 via `schema_version` probe) — needs browser testing

**Prerequisite:** V048 migration (universal import system tables + seed data)

Provider-agnostic data import system for non-Summit TPA platforms. Configuration-driven column mappings allow any TPA platform (WEX, Alegeus, Employee Navigator, etc.) to be connected. 4-step import wizard (Select Provider → Upload Files → Review & Configure → Results). Import history tracking via ImportRunLog. Summit uses dedicated redirect to existing SummitImportWizard.

**Deploy steps:**
1. Apply `docs/migrations/V048__universal_import_system.sql` to target databases
2. Deploy WAR
3. Navigate to Universal Import — "DataPath (Summit)" redirects to existing Summit wizard; other providers follow universal flow
4. Optionally configure new providers via Import Providers admin page
5. Test full import flow via Universal Import wizard

**Files (new):**
- `V048__universal_import_system.sql` — migration (5 tables + seed data)
- `ImportProvider.java`, `ImportFileType.java`, `ImportFieldMapping.java`, `ImportPlanTypeMapping.java`, `ImportRunLog.java` — 5 JPA entities in `model/imports/`
- `UniversalImportService.java` — import engine in `data/service/`
- `ProviderSetup.java` — provider CRUD servlet in `controller/data/`
- `UniversalImport.java` — 4-step wizard servlet in `controller/data/`
- `ImportHistory.java` — history viewer servlet in `controller/data/`
- `SummitProviderSeeder.java` — Summit seeder in `data/service/` (unused — Summit uses dedicated redirect)
- 5 ProviderSetup JSPs in `view/a/general/providerSetup/` (providerList, providerEdit, fileTypeEdit, fieldMappingEdit, planTypeMappingEdit)
- 4 UniversalImport JSPs in `view/a/general/universalImport/` (step1Provider, step2Upload, step3Configure, step4Results)
- `importHistory.jsp` — import history page

**Files (modified):**
- `navbar25.jsp` — Data Import section with 3 links (Import Providers, Universal Import, Import History)

---

### D-64: Apply V051-V053 + Import Cross-Reference System + Phase A Provider Rework

**Priority:** HIGH — Required for interactive import wizard
**Status:** Code complete — V051, V052, V053 all applied to production (verified 2026-07-30 via `schema_version` probe) — needs browser testing

**Prerequisite:** D-63 (V048 Universal Import System) must be applied first

Three migrations enabling multi-provider cross-reference resolution, provider setup rework, and interactive import support:
- **V051:** `import_id_mapping` table for cross-reference PK resolution across providers
- **V052:** xref tracking columns (`xref_resolved`, `pk_allocated`, `mappings_recorded`) on `import_run_log`
- **V053:** `update_mode` + `mapping_status` on `import_file_type`; `is_fk` + `fk_entity_type` on `import_field_mapping`

**Deploy steps:**
1. Apply `docs/migrations/V051__import_id_mapping.sql`
2. Apply `docs/migrations/V052__import_run_log_xref_tracking.sql`
3. Apply `docs/migrations/V053__interactive_import_enhancements.sql`
4. Deploy WAR
5. Test Import Transition Manager (browse/link/unlink xref mappings)
6. Test ProviderSetup rework (sample file upload, PK/FK flagging, status badges)
7. Test UPDATE_ONLY mode (create file type, verify FK not required)

**Files (new):**
- `V051__import_id_mapping.sql`, `V052__import_run_log_xref_tracking.sql`, `V053__interactive_import_enhancements.sql`
- `ImportIdMapping.java` — xref entity in `model/imports/`
- `ImportIdResolver.java` — resolution service in `data/resolver/`
- `ImportTransitionManager.java` — xref admin servlet in `controller/data/`
- `importTransition.jsp` — xref admin UI

**Files (modified):**
- `SummitImportService.java`, `SummitImportWizard.java` — xref recording integration
- `UniversalImportService.java` — resolver+fallback integration
- `ImportRunLog.java`, `ImportFieldMapping.java`, `ImportFileType.java` — new fields
- `ProviderSetup.java` — rewritten (autoDetect, saveMappingsBulk, validateAndUpdateStatus)
- `fieldMappingEdit.jsp` — full rewrite (sample-file-driven table)
- `fileTypeList.jsp` — Status badge, Mode column, UPDATE_ONLY option

---

### D-65: Phase B Interactive Import Wizard

**Priority:** HIGH — Core import workflow enhancement
**Status:** B1 shell complete, B2-B5 pending development

**Prerequisite:** D-64 (V051-V053) must be applied

Entity-by-entity import wizard with interactive cross-reference resolution. Users upload one entity type at a time, see matched/suggested/unmatched rows, resolve cross-references interactively, then commit. Entity order: PlanType → Employer → Benefit → Employee.

**B1 (complete):** Wizard shell — select provider, step through entity types (skip all), results page
**B2 (pending):** Upload + auto-resolution engine (ImportResolutionService, fuzzy matching)
**B3 (pending):** AJAX resolution interactions (InteractiveImportApi, confirm/search/link)
**B4 (pending):** Commit logic + entity progression
**B5 (pending):** Polish, UPDATE_ONLY enforcement, error handling

**Files (new — B1):**
- `InteractiveImportSession.java` — session POJO in `data/service/`
- `InteractiveImport.java` — wizard servlet in `controller/data/`
- `selectProvider.jsp`, `entityStep.jsp`, `results.jsp` — 3 JSPs in `view/a/general/interactiveImport/`

**Files (modified — B1):**
- `step1Provider.jsp` — added Interactive Import link

---

### D-59: BPO Deployment Fixes + Recurring Task Push

**Priority:** HIGH — Fixes critical bugs discovered during demo/BPO VPS testing
**Status:** Code complete — no migration needed, deploy WAR to all instances

Five fixes bundled together (no schema changes):
1. **EclipseLink L2 cache corruption:** `DemoDataSeeder.seedConferenceDemo()` evicts L2 cache before entity lookups to prevent stale SINGLE_TABLE discriminator mappings after ReSeedDb
2. **BPO Admin 403 on admin servlets:** `ReSeedDb.isAdmin()`, `SeedDemoData`, `SeedBpoDemoData` now accept both `isPspAdmin` and `isBpoAdmin`
3. **seedFilterPresets duplicate key:** Idempotency guard in `DatabaseInitializer.seedFilterPresets()` — skips if presets already exist
4. **PartnershipApproveApi cache refresh:** Refreshes `AmsDataGlobal` after BPO approval so vendor sourcing appears in ManageTask25 without Tomcat restart
5. **Recurring checklist BPO push:** `BpoTaskPushService.pushDelegatedTasks()` called after `createNewRecurringChecklist()` at all 3 call sites

**Files:**
- `DemoDataSeeder.java`, `DatabaseInitializer.java`, `InitializeDataBase.java`
- `SeedDemoData.java`, `ReSeedDemoData.java`, `ReSeedDb.java`, `SeedBpoDemoData.java`
- `PartnershipApproveApi.java`
- `CloseActivity25.java`, `AmsDataLocal.java`

---

### D-66: Systemd ReadWritePaths for Tomcat Data Directory ✅

**Completed:** March 20, 2026 (master image v9)
**Applied to:** Demo PSP ✅, Master ✅ — BPO and Production still need applying

Tomcat 10 on Ubuntu 24.04 ships with `ProtectSystem=strict` in the systemd unit, which makes the filesystem read-only except for explicitly allowed paths. The existing branding override (`/var/lib/tomcat10/webapps/ROOT/branding/`) only covers branding files. Summit Import (and any future file-upload feature) writes to `SAVE_PATH/summit_import/` which is blocked.

**Fix (per VPS):**
```bash
sudo systemctl edit tomcat10
```
Add to the override file:
```ini
[Service]
ReadWritePaths=/var/lib/tomcat10/data/
```
Then:
```bash
sudo systemctl daemon-reload
sudo systemctl restart tomcat10
```

**Note:** The existing branding `ReadWritePaths` line should be consolidated into the same override block. Both lines can coexist:
```ini
[Service]
ReadWritePaths=/var/lib/tomcat10/webapps/ROOT/branding/
ReadWritePaths=/var/lib/tomcat10/data/
```

---

### D-81: Apply V060 + Outlook Web Add-in ("Log to AMS")

**Priority:** MEDIUM — New feature, no urgency, but adds value for PSP users handling client email
**Status:** Code complete — V060 applied to production (verified 2026-07-30 via `schema_version` probe); add-in not yet deployed/registered

New Outlook Web Add-in that adds a "Log to AMS" button to the reading pane. When clicked, a taskpane opens, auto-authenticates via the user's Microsoft 365 email, and lets them pick an open AMS activity and log the email as a Note. Any email attachments are uploaded to Wasabi and linked to the note via WebLink records.

**Migration V060:**
- Creates `outlook_user_link` table (person_id, m365_email unique, api_token unique, is_active)
- Adds nullable `weblink.note_id` FK column + index (matches existing `email_id` / `todo_note_id` pattern)

**New API endpoints (bypass ApiTokenFilter, use per-user tokens):**
- `POST /api/v1/outlook/authenticate` — exchange M365 email for api_token
- `GET  /api/v1/outlook/activities?q={term}` — PSP-scoped open-activity search (Activity25 view)
- `POST /api/v1/outlook/log-email` — multipart upload, creates Note + WebLink rows, uploads files to Wasabi

**New admin page:**
- `/OutlookLinkManager` (PSP Admin role 5) — link AMS users to M365 emails, regenerate tokens, unlink
- Token = 2× UUID.randomUUID concatenated, dashes stripped = 64 hex chars

**Static add-in files at `/outlook/`:**
- `manifest.xml` — Office Add-in MailApp manifest (Mailbox 1.5+)
- `taskpane.html` — single-page UI (Office.js + Bootstrap, SSA brand colors)
- `icon-16.png` / `icon-32.png` / `icon-80.png` — navy tile with white "A", green accent stripe
- `scripts/generate-outlook-icons.ps1` — reproducible icon generator

**Auth model:** The add-in does NOT use session cookies. On first open, it POSTs the user's M365 email to `/authenticate`, receives a token, stores it in `localStorage`, and sends it as `Authorization: Bearer {token}` on every subsequent call. PSP Admin must link each user via `/OutlookLinkManager` before they can use the add-in.

**Deployment steps:**
1. Apply `docs/migrations/V060__outlook_user_link.sql`
2. Deploy WAR
3. Navigate to `/OutlookLinkManager` (as PSP Admin) and link at least one user's M365 email
4. In Outlook Web → More apps → Get Add-ins → My add-ins → Add from URL → `https://superiorstate.biz/outlook/manifest.xml`
5. Open any email → click "Log to AMS" in the ribbon → verify taskpane auto-authenticates and picker works
6. Test logging an email with attachments → verify Note appears on the activity with WebLink rows (check the activity's note list; attachments are tied to the note via `weblink.note_id`)

**Files:**
- `docs/migrations/V060__outlook_user_link.sql`
- `src/main/java/net/superiorstate/ams/model/general/OutlookUserLink.java`
- `src/main/java/net/superiorstate/ams/controller/api/outlook/` (4 files)
- `src/main/java/net/superiorstate/ams/controller/user/OutlookLinkManager.java`
- `src/main/webapp/WEB-INF/view/user/outlookLinkManager.jsp`
- `src/main/webapp/outlook/` (manifest, taskpane, README, 3 PNGs)
- `scripts/generate-outlook-icons.ps1`
- Modified: `Note.java`, `WebLink.java`, `LoginFilter.java`, `ApiTokenFilter.java`

**Future enhancements (not in scope):**
- Bulk user linking via CSV
- Auto-detect activity from email subject line keywords
- Log sent emails (not just received)
- Org-wide deployment via Microsoft 365 Admin Center (Integrated Apps)

**Applies to:** Demo PSP ✅, BPO ⬜, Production ⬜, Master image ⬜

---

### D-76: Set `PSP_HOSTS` in `ssa.properties` per environment (V068 host-header agency landing) ⬜

**Depends on:** V068 (`agency.landing_host` / `agency.landing_html`) deployed.

The V068 host-header custom agency landing feature classifies each incoming request's `Host` header as either a **PSP host** (default landing/login, unchanged) or a **non-PSP host** (candidate for an agency-branded landing). The PSP-host allow-list is read from `ssa.properties` via `AppConfig.get("PSP_HOSTS", "superiorstate.net,superiorstate.biz")` — a comma-separated, **exact-match** (never wildcard) list, so agency front doors on subdomains like `swbd.superiorstate.net` are correctly treated as non-PSP.

**Action:** on each environment's `ssa.properties` (production / demo / bpo / master), set `PSP_HOSTS` to that installation's own PSP hostname(s). If the key is absent the code falls back to the default `superiorstate.net,superiorstate.biz`, which is correct for the primary production PSP but should be reviewed per host.

- `PSP_HOSTS=superiorstate.net,superiorstate.biz` (production default)
- Demo/BPO/Master: set to the respective PSP host(s) as appropriate so their default hosts are not mistaken for agency vanity hosts.

**Separate (parallel) infra track — DNS/TLS for vanity hosts (not code):**
- `*.superiorstate.net` subdomains (e.g. `swbd.superiorstate.net`) — feasible now via Cloudflare DNS + wildcard/DNS-01 cert; nginx must forward `Host` unmodified.
- Customer-owned domains (e.g. `admin.swbd.com`) — need Cloudflare for SaaS / custom hostnames (per-host certs). Separate epic; the application code is identical for both (it only reads `getServerName()`).

**Applies to:** Production ⬜, Demo PSP ⬜, BPO ⬜, Master image ⬜

---

### D-77: Seed `SUMMIT_TPA_GUID` constant on already-initialized environments

**Priority:** MEDIUM — Required for the `SummitEditEmployer` redirect to produce a working link
**Status:** Done — SUMMIT_TPA_GUID inserted on production (beta_ssa) 2026-07-17.

`AmsDataGlobal.getSummitTpaGuid()` reads the `SUMMIT_TPA_GUID` constant. `getConstantValue()` catches `NoResultException` and returns `""` rather than throwing, so `AmsDataGlobal`'s hardcoded fallback is unreachable for the missing-row case — `getSummitTpaGuid()` currently resolves to `""` in production. The `SummitEditEmployer` redirect needs a real `tpaGuid` value in its outbound URL.

**Action** — run on production (`beta_ssa`) as a write-capable account (`ams_app`):
```sql
INSERT INTO constant (name, value)
VALUES ('SUMMIT_TPA_GUID', '22bec8d2-cd1c-4795-aebe-fa3f60522b6d')
ON DUPLICATE KEY UPDATE value = VALUES(value);
```

Also apply to demo / bpo / master only if the Summit EditEmployer redirect is used on that environment.

**Verify:**
```sql
SELECT value FROM constant WHERE name='SUMMIT_TPA_GUID';
```
Should return the GUID.

---

### D-78: Seed `HEALTHSHERPA_API_KEY` constant on environments running rate-cache warming

**Priority:** MEDIUM — Blocks the A1 rating-illustration rate-cache warm job until issued
**Status:** Not started — **key issued 2026-07-30 and in hand; this item is actionable.** The
previous "key not yet issued" blocker is resolved.

**Depends on:** Nothing external for staging. A key issued 2026-07-30 authenticates against
`https://api.ichra-staging.healthsherpa.com`. **Production returns 403 pending allow-listing** —
follow-up open with KJ Sherman / Julian Ferdman — so a production seeding of this constant must pair
with **D-79** pointing at staging until allow-listing lands. As of T43, seeding this key without D-79
set doesn't reach HealthSherpa at all: `HealthSherpaService` fails closed locally on the missing base
URL, logging and skipping rather than calling out to any endpoint.

Insert a `constant` row named `HEALTHSHERPA_API_KEY` with the issued key value, on each environment that runs the rate-cache warm job:

```sql
INSERT INTO constant (name, value) VALUES ('HEALTHSHERPA_API_KEY', '<issued-key>')
ON DUPLICATE KEY UPDATE value = VALUES(value);
```

**Note:** the value is not yet issued — this item is blocked until HealthSherpa credentials for the ICHRA Partner API arrive. `AppConfig.getHealthSherpaApiKey()` falls back to `ssa.properties` (`HEALTHSHERPA_API_KEY`) when the DB row is absent or empty, matching the existing `ANTHROPIC_API_KEY` pattern.

**Applies to:** Production ⬜, Demo PSP ⬜, BPO ⬜, Master image ⬜ — whichever environment(s) end up running the scheduled warm job (not yet decided).

**Verified 2026-07-31:** not applied to **any** environment. A `SELECT name FROM constant` against
local dev returned zero rows for `HEALTHSHERPA_API_KEY` and `HEALTHSHERPA_BASE_URL`, and neither
appears in `ssa.properties`. Consequence: `AppConfig.getHealthSherpaApiKey()` has always returned
null in the running application, so the path `AppConfig` → `AmsDataGlobal` startup population →
`HealthSherpaService` is code-complete and **entirely unexercised** — every HealthSherpa verification
recorded to date was made out-of-band with a manually supplied key, never through AMS. Applying this
item is a prerequisite for any HealthSherpa call from AMS, including the D-82 rate-cache warm job.
See also **T43** — with no base URL configured, `getHealthSherpaBaseUrl()` now returns null and
`HealthSherpaService` refuses the call rather than falling through to the **production** endpoint.

---

### D-79: Seed `HEALTHSHERPA_BASE_URL` constant

**Priority:** HIGH — **required on every environment that runs the warm job, production included.**
As of T43, `AppConfig.getHealthSherpaBaseUrl()` has no default. Absent or blank means HealthSherpa is
not configured on that installation: `HealthSherpaService` refuses the call and logs an error, and
`RateCacheWarmService`'s warm job logs and skips rather than guessing an environment. There is no
value inherited from anywhere else — the operator chooses staging or production deliberately, per
installation, by setting this constant.

**Status:** Not started

Each of the four installations (Production, Demo, Master, BPO) has its own database and its own
`ssa.properties`, so each needs this value set independently — nothing here is shared or inherited
across environments.

Insert a `constant` row named `HEALTHSHERPA_BASE_URL` to point an environment at HealthSherpa's staging or production API:

```sql
INSERT INTO constant (name, value) VALUES ('HEALTHSHERPA_BASE_URL', 'https://api.ichra-staging.healthsherpa.com')
ON DUPLICATE KEY UPDATE value = VALUES(value);
```

- Staging value: `https://api.ichra-staging.healthsherpa.com`.
- Production value: `https://api.ichra.healthsherpa.com` — pending allow-listing (see D-78), currently
  403s regardless of whether it's configured.

The value can also be set on a running installation without a redeploy — either this `constant` row
directly, or the `HEALTHSHERPA_BASE_URL` property in `ssa.properties`. The `SystemConstantsApi` PUT
path refreshes `AppConfig`'s cached value immediately, no restart required.

**Note:** `AppConfig.getHealthSherpaBaseUrl()` falls back to `ssa.properties` (`HEALTHSHERPA_BASE_URL`), then returns null, when the DB row is absent or empty. There is no hardcoded default of any kind.

**Applies to:** Production ⬜, Demo PSP ⬜, BPO ⬜, Master image ⬜ — every environment that runs the rate-cache warm job now requires this constant; none may rely on an inherited or default value.

**Verified 2026-07-31:** not applied to **any** environment. A `SELECT name FROM constant` against
local dev returned zero rows for `HEALTHSHERPA_API_KEY` and `HEALTHSHERPA_BASE_URL`, and neither
appears in `ssa.properties`. Consequence: `AppConfig.getHealthSherpaApiKey()` has always returned
null in the running application, so the path `AppConfig` → `AmsDataGlobal` startup population →
`HealthSherpaService` is code-complete and **entirely unexercised** — every HealthSherpa verification
recorded to date was made out-of-band with a manually supplied key, never through AMS. Applying this
item is a prerequisite for any HealthSherpa call from AMS, including the D-82 rate-cache warm job.
See also **T43** — with no base URL configured, `getHealthSherpaBaseUrl()` now returns null and the
warm job fails closed rather than targeting **production**.

---

### D-82: Seed `RATE_CACHE_WARM_ENABLED` constant per instance

**Priority:** MEDIUM — Controls whether the A1 rate-cache warm job runs on this instance at all
**Status:** Not started

Insert a `constant` row named `RATE_CACHE_WARM_ENABLED` (value `true` or `false`) on every instance. Without a per-instance flag, all four installations (Production, Demo, Master, BPO) would each register the scheduled warm job and independently hammer the HealthSherpa API on a key whose rate limits are unknown.

```sql
INSERT INTO constant (name, value) VALUES ('RATE_CACHE_WARM_ENABLED', 'true')
ON DUPLICATE KEY UPDATE value = VALUES(value);
```

- **Production:** `true`.
- **Demo:** optional — set `true` only if the demo environment needs live rate-cache data.
- **Master:** `false`.
- **BPO:** `false`.

**Note:** `EmfListener` reads this via `AppConstantDAO.getConstantValue(em, "RATE_CACHE_WARM_ENABLED")` at startup, case-insensitive `"true"` check — matching the `IS_MASTER` constant pattern. Absent or any other value means the job does not register at all (no executor thread, no scheduling). Deliberately **not** gated on `AppConfig.isMaster()` — this job needs to run on production, not master, mirroring the `billingExecutor` precedent.

**Applies to:** Production ⬜, Demo PSP ⬜, BPO ⬜, Master image ⬜.

---

### D-83: Seed `RATE_CACHE_COUNTIES` constant

**Priority:** MEDIUM — Required before the warm job has anything to warm
**Status:** Not started — no counties configured yet

Insert a `constant` row named `RATE_CACHE_COUNTIES` listing the counties the warm job populates, so a new market needs no redeploy — just a constant update.

```sql
INSERT INTO constant (name, value) VALUES ('RATE_CACHE_COUNTIES', '75482:48223:TX')
ON DUPLICATE KEY UPDATE value = VALUES(value);
```

⚠️ **Format is `zip:fips:state` triples, comma-separated — not the `fips:state` pairs originally specified in the A1 Phase B-1b prompt.** `HealthSherpaService.quoteSingleApplicant` requires a ZIP code as a real request field; the county list as originally specified had no ZIP. Rather than guess a representative ZIP per county in code (a real correctness risk — the wrong ZIP could silently return the wrong rating area), the format carries one, supplied here by whoever configures the constant. Malformed entries (wrong part count, blank segment) are skipped and logged by `RateCacheWarmService`, not fatal to the run.

**Update 2026-07-30 (V076) — bare-FIPS form now also accepted.** `RATE_CACHE_COUNTIES` entries now accept two formats, both supported indefinitely with no cutover required: the legacy `zip:fips:state` triple above, and a bare `county_fips` (e.g. `48223`) resolved through the new `county_reference` table (V076) via `CountyReferenceDAO.findByFips`, which supplies the zip and state from that row. `RateCacheWarmService.readConfiguredCounties` dispatches on entry shape — 3 colon-separated parts is the legacy triple, 1 part is the bare-FIPS form, anything else is malformed and skipped as before. Bare FIPS is preferred for new entries going forward since it keeps county identity in one place (`county_reference`) instead of duplicating a hand-picked ZIP into every constant entry, but the triple form is not deprecated — it still works unchanged and needs no migration.

> ⭐ **Reframed 2026-08-04 — "which counties" is largely not a business decision, and never was a cost one.**
>
> This item has been carried as blocked on an unmade business call about which markets SSA quotes.
> The cost model says otherwise. Verified against `RateCacheWarmService.warmCounty`:
>
> - **A county-year costs two API calls** — one off-exchange quote at age 21, one on-exchange at 21
>   (T44/V078) — plus one canary per plan year *in total*, not per county. It yields **44 rows**, and
>   every row above age 21 is `AgeCurve` arithmetic, not a fetch.
> - **Storage is irrelevant at any plausible scale.** All 254 Texas counties × 2 plan years is
>   **22,352 rows**; every US county × 2 years is ~276,600. The row is ~15 small numeric columns.
> - **The only real cost is calls, and it is a function of cadence, not county count.** The full state
>   × 2 plan years is ~1,016 calls per cycle. At today's 24-hour cadence that is ~370,000 calls a
>   year — which is what makes a long county list look expensive. **ACA rates are annual filings**, so
>   at a plan-year cadence the same full-state warm is ~1,016 calls **once** — less than three days of
>   the current four-county schedule. See **T152**.
>
> **Recommendation: seed all 254 Texas counties and stop treating the list as a gating decision.**
> Pair it with **T76** (warm-on-miss) as the safety net — which also handles the multi-installation
> case, where each TPA's counties differ and cannot be known centrally.
>
> ⚠️ **What *does* remain a real decision** is **which states**, not which counties: the ZIP crosswalk
> is Texas-only (V084/V085, 2,894 rows) and `AgeCurve` implements the **federal default curve only**,
> which Texas uses and several states do not. Expanding beyond Texas needs both a crosswalk migration
> and a state dimension on the curve. See **T148**.
>
> ⚠️ **Warming more counties does not make them demoable** — every warmed row is stamped
> `source_env = STAGING` until production allow-listing lands (**T136**), and the provenance gates
> still fire. Volume and provenance are independent problems.

**Applies to:** Production ⬜ — the county *list* is no longer the blocker (see above); seeding it is. State scope beyond Texas remains a genuine decision.

---

### D-84: Seed `RATE_CACHE_PLAN_YEARS` constant

**Priority:** HIGH — Without this, the cache silently serves the wrong plan year during open enrollment
**Status:** Not started

Insert a `constant` row named `RATE_CACHE_PLAN_YEARS` listing which plan years the warm job populates, comma-separated:

```sql
INSERT INTO constant (name, value) VALUES ('RATE_CACHE_PLAN_YEARS', '2026')
ON DUPLICATE KEY UPDATE value = VALUES(value);
```

**Why this exists.** `RateCacheWarmService` originally derived its plan year from `Year.now()` — wrong during the period it matters most. Open enrollment for plan year 2027 begins November 1, 2026. From that date, an agent illustrating a group with a January 1, 2027 effective date needs 2027 rates, but `Year.now()` returns 2026 until January 1. The cache would have served the wrong plan year while appearing perfectly healthy — no error, no warning, just wrong numbers during the busiest quoting window of the year. `plan_year` is already part of `rating_area_rate_cache`'s unique key, so the cache can hold multiple years simultaneously — the fix is configuration, not schema.

**During open enrollment (November 1 onward), configure both the current and upcoming plan year**, e.g. `2026,2027`, so illustrations for either effective date resolve correctly.

> ⚠️ **BLOCKED PREREQUISITE — added 2026-08-04. This item is NOT config-only for plan year 2027.**
>
> **`AgeCurve` currently contains a curve for plan year 2026 and no other** (`AgeCurve.java:90-92`,
> `CURVES_BY_PLAN_YEAR` is a hardcoded static map). Adding `2027` to this constant therefore
> **produces nothing**: `hasCurveFor(2027)` returns false, `RateCacheWarmService` logs the ERROR
> described below and skips the year, and no 2027 county is ever warmed.
>
> **So the November step is a code change plus a deployment, not a constant update.** Sequence it
> accordingly — the curve has to ship *before* the constant is seeded, or the seeding is a no-op that
> looks done. Tracked as **T148** in `docs/analysis/project_backlog.md`.
>
> **Verify the 2026 curve at the same time.** `AgeCurve`'s own Javadoc records that **only five of its
> 44 factors are empirically confirmed** (ages 21, 25, 40, 45, 64); the remaining 39 are transcribed
> from the CMS federal default and have never been checked against a published CMS source. Every
> premium the illustration displays at any age other than 21 is derived from that table by
> `AgeCurve.scale()`, so a wrong interior factor yields a confidently wrong figure with no symptom.
>
> **Also note the curve is state-scoped.** Texas uses the federal default; several states publish
> their own. A state dimension is required before any non-federal-default state is warmed.
>
> ⚠️ **Two related plan-year items, so they are not discovered separately in November:**
> **T127** — with two plan years live simultaneously, `IllustrationServlet.resolvePlanYear` falls
> through to `configuredPlanYears.get(0)`, so **sort order in this constant silently decides the plan
> year** for every illustration and every plus-tier proposal intake. Reordering does not fix it; it
> moves the silent error to the other population. **D-86** — `ICHRA_AFFORDABILITY_PCT_2027` will need
> its own IRS-verified value; it is per plan year and does not carry forward.

**A year with no `AgeCurve` entry is skipped with a logged error, never guessed or silently substituted with a different year's curve.** `RateCacheWarmService` reads this constant fresh at the start of every run (not once at startup), so a change takes effect on the next scheduled tick without a restart. If the constant is absent, empty, or wholly unparseable, the run is skipped entirely and logged as an error rather than falling back to any default year.

**Applies to:** Production ⬜, Demo PSP ⬜, BPO ⬜, Master image ⬜ — same environments as D-82/D-83.

---

### D-85: `catalina.out` on production never rotates

**Priority:** LOW — no current operational impact; it grows slowly relative to available disk
**Status:** Not started

`/var/log/tomcat10/catalina.out` on production is 97,150,379 bytes and holds entries from 2025-09-10 onward — it has never been rotated. Every other log in that directory rotates daily and compresses: `catalina.YYYY-MM-DD.log.gz`, `localhost.*.log.gz`, `localhost_access_log.*.txt.gz`, `ams-*.log.gz`.

**The tell is ownership.** `catalina.out` is owned by `syslog:adm`; every other file in the directory is owned by `tomcat:adm`. That means `catalina.out` is being written through a systemd/syslog redirect rather than by Tomcat's own juli handler — which is why juli's rotation doesn't cover it, and why no logrotate rule appears to exist for it either.

**Impact:**
- Unbounded growth on the production filesystem
- Slow to search exactly when it matters — during an incident
- Conversely, it's the only log with the full eleven-month history, which is what made the 2026-07-31 production log review (T40 above) possible at all — any rotation policy should preserve a long retention window, not just cap the file size

**Likely fix** (not yet applied — needs verification first): a logrotate rule for `catalina.out` using `copytruncate`, since the process writing it holds the file open and a plain rename-based rotation would just keep writing to the unlinked inode.

**This was only checked on production.** Master, Demo, and BPO run the same Tomcat setup and should be checked before any fix is applied — this item covers all four installations, not production alone.

**Applies to:** Production ⬜, Demo PSP ⬜, BPO ⬜, Master image ⬜ — unconfirmed on the latter three; production is the only one inspected so far.

---

### D-86: Seed `ICHRA_AFFORDABILITY_PCT_{year}` constant

**Priority:** HIGH — without this, the item-9 affordability feature produces no output at all for that plan year (fails closed by design; this is not a bug to route around)
**Status:** Not started

Insert a `constant` row named `ICHRA_AFFORDABILITY_PCT_{year}` (e.g. `ICHRA_AFFORDABILITY_PCT_2026`) holding the IRS-indexed applicable percentage for ICHRA/employer-coverage affordability for that plan year, as a decimal:

```sql
INSERT INTO constant (name, value) VALUES ('ICHRA_AFFORDABILITY_PCT_{year}', '<verify-against-IRS-revenue-procedure-before-entry>')
ON DUPLICATE KEY UPDATE value = VALUES(value);
```

⚠️ **No value is suggested here on purpose.** The applicable percentage is indexed annually by IRS revenue procedure and must be looked up and verified against the current revenue procedure for the target plan year before this row is entered — never carried forward from a prior year, never guessed. Entering a wrong or stale value here does not fail loudly; it silently changes every affordability threshold the feature computes. Verify, then enter, then spot-check one worked example against a known IRS reference figure before relying on the feature.

**Why this exists.** `IllustrationServlet`'s affordability computation (build item 9) reads this constant per plan year and fails closed — absent, empty, or unparseable means no affordability output is rendered at all for that plan year, not a default and not a prior year's value. The AGE_BAND illustration's net-cost table works with or without this constant; only the affordability section depends on it.

**Applies to:** Production ⬜, Demo PSP ⬜, BPO ⬜, Master image ⬜ — same environments as D-83/D-84, per plan year in use.

---

### D-87: Seed `FPL_ANNUAL_{year}` constant

**Priority:** MEDIUM — gates only the FPL safe-harbor basis; entered-income affordability analysis still works without it
**Status:** Not started

Insert a `constant` row named `FPL_ANNUAL_{year}` (e.g. `FPL_ANNUAL_2026`) holding the federal poverty line, annual, mainland 48 states, for a household of one, for that plan year:

```sql
INSERT INTO constant (name, value) VALUES ('FPL_ANNUAL_{year}', '<verify-against-HHS-poverty-guidelines-before-entry>')
ON DUPLICATE KEY UPDATE value = VALUES(value);
```

⚠️ **No value is suggested here on purpose.** The federal poverty line is published annually by HHS and must be looked up and verified against the current poverty guidelines for the target plan year before this row is entered — never carried forward from a prior year, never guessed.

**Why this exists.** The affordability feature's FPL safe-harbor basis (build item 9) needs no per-employee income data — it uses this single figure for the whole group instead. If this constant is absent, empty, or unparseable, the FPL safe-harbor basis is simply unavailable and the page says so; the entered-income basis is unaffected and keeps working.

**Applies to:** Production ⬜, Demo PSP ⬜, BPO ⬜, Master image ⬜ — same environments as D-86, per plan year in use.

---

### D-88: Seed `ICHRA_RATE_SOURCE_ENV` constant on already-initialized installations

**Priority:** MEDIUM — the resolver's fail-closed default happens to match the intended value today, so nothing visibly breaks; the row is easy to forget precisely because of that
**Status:** Not started

S21-L added `RateSourceEnvResolver` — the single source of truth for "which `RatingAreaRateCache.sourceEnv` is authoritative for this installation" — backed by a `constant` row seeded through `DatabaseInitializer.createConstant(em, "ICHRA_RATE_SOURCE_ENV", "STAGING")` (`data/service/DatabaseInitializer.java`, called from `initializeDataBase()`). **That path only runs on a brand-new installation.** Kevin's dev database and production have both already initialized, so neither will ever execute that seed — this is the `D-NN` counterpart the project's standing rule requires for an existing database, exactly as D-77/D-78/D-79/D-82/D-83/D-84/D-86/D-87 are for their own constants. Both paths stay in place; neither replaces the other.

```sql
INSERT INTO constant (name, value) VALUES ('ICHRA_RATE_SOURCE_ENV', 'STAGING')
ON DUPLICATE KEY UPDATE value = VALUES(value);
```

Matches `createConstant`'s own row shape exactly: only `name` and `value` are set; `note` and `text_value` are left at their column defaults (`NULL`), the same as every row that method creates. Safe to run twice and safe on a database that already has the row — `ON DUPLICATE KEY UPDATE` on `name` (the table's primary key) makes this idempotent either way.

**If this is not applied:** `RateSourceEnvResolver.authoritativeSourceEnv` finds no row, logs `log.error("[RATE-SOURCE-ENV] Constant '...' missing or unrecognized ...")` on every single call, and falls to its coded fail-closed default — which is also `STAGING`. **The resulting behavior is therefore identical to having applied this row, by coincidence, not by correctness.** That is exactly what makes the missing row easy to overlook: nothing renders wrong, nothing looks broken, and the only sign anything is missing is a log line that will scroll past unnoticed unless someone is watching for it. Apply the row anyway — a resolver silently running on its error path in steady state is not an acceptable resting state, even when today's error-path answer and today's correct answer happen to agree.

**The flip, when production rate access is enabled:**

```sql
UPDATE constant SET value = 'PRODUCTION' WHERE name = 'ICHRA_RATE_SOURCE_ENV';
```

One row, no code change, no deploy, no restart — `RateSourceEnvResolver` reads uncached, per call. **Cross-reference: `legal_assumptions.md` LA-18.** Its confirm-before trigger is granting `agency.ichra_enabled` to any agency, other than through PSP-admin access, while this constant still reads `STAGING`. The flip above and LA-18's trigger are two sides of the same decision — do not flip this row without also having confirmed LA-18's condition, and do not grant entitlement while assuming this row already reads `PRODUCTION` without checking it directly.

**Applies to:** Kevin's local dev database (`beta_ssa`, work) ⬜ — needed now, for local ICHRA testing to stop hitting the resolver's error-log path on every render. Production ⬜ — needed before this run's own LA-18 confirm-before trigger can ever be satisfied. Demo PSP, BPO, Master: not currently running ICHRA-dependent code; apply only if/when one of them does.

---

### D-89: `ssa.properties` needs `SUMMIT_TPA_ID_PREFIX` and `SUMMIT_ICHRA_PLAN_TEMPLATE_ID`, plus a Tomcat restart

**Priority:** MEDIUM — nothing is broken today because nothing has run; this is the entire remaining gap between three code-verified emitters and three runtime-verified ones
**Status:** Not started. ⚠️ **`SUMMIT_TPA_ID_PREFIX` is undecided and must be settled before this is applied — see below.**

`SummitExportServlet` (`/SummitExport`, PSP-admin only, linked from the Setup detail screen since S27-C) emits Summit files 1, 2 and 4. **It has never executed.** It reads both properties at request time via `AppConfig.get(...)` and **refuses to emit** — with a plain-text error naming the missing key — when either is absent or malformed. These are `ssa.properties` entries, not `constant` rows, so **there is no SQL for this item** and **Tomcat must be restarted to pick them up**.

**`SUMMIT_ICHRA_PLAN_TEMPLATE_ID` = `1030`.** That is the `ICHRA+` plan template created 2026-09-07 (see `docs/business/summit_data_exchange.md`, "Summit objects created for the ICHRA+ bundle"). ⚠️ **Known limitation, recorded there and repeated here:** the `ICHRA` plan type now carries **two** active templates — `ICHRA` at **1009** and `ICHRA+` at **1030** — and AMS carries exactly one such property, so **every ICHRA sale AMS emits will point at 1030**, facilitated or not. 1009's status is unexamined. This is a single-template configuration behaving as designed, not a defect.

**`SUMMIT_TPA_ID_PREFIX` is undecided.** DataPath assigned SSA **TPA ID 158**, and the leading candidate is to build the prefix from it. The reasoning is **not** routing — the FTP credentials already handle that. It is that a TPA-derived prefix **closes a uniqueness scope that cannot be tested**: the participant-key global-uniqueness finding was established with two employers **under one TPA**, so whether Summit's participant namespace is **per-TPA or instance-wide is unproven**. A TPA-derived prefix is correct under either answer. It is also **naturally distinct per installation**, which is the property `resolveEmployerTpaCustomId` exists to guarantee.

⚠️ **Effectively irreversible once real records land.** The prefix is the **leading segment of both upsert keys** — `{PREFIX}-{prospectId}` for the employer and `{PREFIX}-P-{participantId}` for the participant. **Changing an upsert key orphans every Summit record keyed on the old value**, leaving the old rows stranded and creating duplicates under the new key. Decide it once, before the first real import — not after.

**The value must not be blank and must not contain a pipe or any whitespace** — `resolveEmployerTpaCustomId` validates exactly that and refuses to emit otherwise, rather than falling back to a bare id.

**Also required before an emitted file will import, and not an `ssa.properties` matter:** the **Summit-side Demographics template must be mapped to the eleven elements in the order recorded** in `docs/business/summit_data_exchange.md` ("An optional field must never be the last column"). **The tested template carried nine.** A file AMS emits against a nine-column template will not import correctly. No code change is involved — this is Summit-side template configuration.

**Third prerequisite, also outside `ssa.properties`:** `plan_year_eligibility` must be attached to the LOS being sold, or file 2 refuses — it sources plan year from the `plan_year_start`/`plan_year_end` application answers that section carries.

**Applies to:** Kevin's local dev database / local Tomcat ⬜ — needed for the first runtime walk of any of the three files. Production ⬜ — needed before any real employer is exported. Demo PSP, BPO, Master: not running the Summit export; apply only if/when one of them does.

---

### D-90: `ssa.properties` needs `SUMMIT_PLAN_TEMPLATES`, plus a Tomcat restart

**Priority:** MEDIUM — nothing is broken today; without it file 2 emits one ICHRA row regardless of what the employer actually bought
**Status:** Not started. **Values are installation-specific and must be discovered per installation — see below.**

`SummitExportServlet` file 2 (Employer CDH Plan) emits **one row per plan the employer elected**, driven by a new `ssa.properties` key read at request time through `SummitPlanTemplateResolver.configured()` (shipped `a17f32d`, session 28). This is an `ssa.properties` entry, not a `constant` row, so **there is no SQL for this item** and **Tomcat must be restarted to pick it up** — `AppConfig` loads the file once at startup.

**Format.** Comma-separated entries, each three or four colon-separated fields:

```
SUMMIT_PLAN_TEMPLATES=<serviceItemId>:<templateId>:<keySegment>[:<label>],...
```

- `serviceItemId` — the AMS `ServiceItem` primary key of the elected service. **Installation-specific.**
- `templateId` — the Summit-assigned Plan Template ID (D-89 records `1030` for `ICHRA+`, and `1031`/`1032` for the two `Ins125+` templates).
- `keySegment` — the segment placed inside `Import Plan ID`. No pipe, no whitespace.
- `label` — optional, human-facing, used in `Plan Name` and `Plan Description`. Defaults to `keySegment`.

⚠️ **Entry order is emit order.** The rows come out of the file in the order they appear in the property, so the property is where row ordering is decided.

**How to discover this installation's `ServiceItem` ids — no SQL required.** Generate Summit file 2 for any proposal with the key **unset or deliberately mismatched**. The resulting 400 page lists every service elected on that application as `id = description`, which is exactly the input this key needs. On Kevin's local dev, Red Creek Solutions returned `12=FSA, 17=Payment Services, 19=Debit Cards, 123054=HFSA, 123057=DCAP, 123060=PRA`. **Production's ids will differ** — this is the reason the key is config rather than code (Rule 4), and the reason no `ServiceItem` id appears in any source file.

⚠️ **Absence is a supported state, not a failure.** With the key unset, file 2 falls back to the legacy single-ICHRA row built from `SUMMIT_ICHRA_PLAN_TEMPLATE_ID` — byte-identical to what production emitted at `v0.94.00`, and runtime-verified as such in session 28. **Production is currently on that path and it is correct as far as it goes**; it simply emits one ICHRA row whatever else was sold. Nothing degrades by deploying `a17f32d` without setting this key.

**Parsing is tolerant.** A malformed entry is skipped with a `WARN` naming it and the rest still parse, so **a typo costs one plan row, not the export**. Check `catalina.out` for `[SUMMIT-EXPORT] SUMMIT_PLAN_TEMPLATES entry '...' skipped:` after a restart. A duplicate `serviceItemId` keeps the first entry and warns on the later one.

**Depends on D-89** for the template ids themselves, and on `plan_year_eligibility` being attached to the LOS being sold — file 2 refuses without the `plan_year_start`/`plan_year_end` answers that section carries, before it ever reaches this key.

⚠️ **Reversible, unlike D-89's prefix.** Changing this key changes `Import Plan ID`'s middle segment, which is an upsert key — but **nothing has been imported into Summit yet**, so the cost is zero today and non-zero the moment a file lands. See T185, which is the same clock.

**Applies to:** Kevin's local dev database / local Tomcat ⬜ — set with placeholder template ids during the session 28 walk; needs real values. Production ⬜ — needed before an employer holding anything other than an ICHRA is exported. Demo PSP, BPO, Master: not running the Summit export; apply only if/when one of them does.

---

### D-91: `ssa.properties` needs `SUMMIT_IMPORT_TEMPLATES`, plus a Tomcat restart

**Priority:** MEDIUM — nothing is broken today; without it the three export downloads keep their legacy descriptive filenames, which **no Summit import template will bind to**
**Status:** Not started on Production. **Set and runtime-verified on Kevin's local dev with test templates — production names are not yet decided.**

Summit binds a retrieved file to an import template **by filename prefix**: the uploaded file's name must begin with the template's own name. Until the emitted filenames carry that prefix, no file AMS produces can be imported at all. Template names live in each TPA's own Summit tenant, so this is an `ssa.properties` entry, not a `constant` row — **there is no SQL for this item** and **Tomcat must be restarted to pick it up**, since `AppConfig` loads the file once at startup. Read at request time through `SummitImportTemplateResolver.templateNameFor()` (session 29).

**Format.** Comma-separated entries, each two colon-separated fields:

```
SUMMIT_IMPORT_TEMPLATES=<type>:<templateName>,...
```

- `type` — the export servlet's own `type` request parameter, so the vocabulary is fixed and not invented here: `employer` (file 1, Employer Demographic), `cdhplan` (file 2, Employer CDH Plan), `demographics` (file 4, Demographics). Matched case-insensitively.
- `templateName` — the **exact** Summit import template name for that installation's tenant. Used verbatim as the filename prefix. Letters, digits, `_`, `-` and `.` only; a name carrying a space, quote or path separator is rejected at parse time with a `WARN`, because the value travels inside a `Content-Disposition` header.

The emitted filename becomes `{templateName}_{yyyyMMddHHmmss}.txt`. The timestamp keeps successive downloads of the same file distinct.

⚠️ **No configured template name may be a prefix of another.** Summit matches on prefix, so `SSA_ER` and `SSA_ER_FIX` would route one file to the wrong template **silently**. `SummitImportTemplateResolver` logs a `WARN` naming both on collision but **serves anyway** — refusing would turn a vendor-side naming choice into an AMS outage. Check `catalina.out` for `[SUMMIT-EXPORT] SUMMIT_IMPORT_TEMPLATES prefix collision:` after a restart.

⚠️ **Absence is a supported state, not a failure.** With the key unset, all three files download under the legacy descriptive names (`employer-demographic-…`, `employer-cdh-plan-…`, `demographics-…`) — byte-identical to what production emits today. **This is safe to deploy before it is configured**; nothing degrades, the files simply cannot be imported into Summit until the key is set, which is already true today.

**Parsing is tolerant.** A malformed entry is skipped with a `WARN` naming it and the rest still parse, so a typo costs one filename, not the export. A duplicate `type` keeps the first entry and warns on the later one.

⚠️ **A misspelled `type` is silent** — `employeer:X` parses fine and simply never matches, so that one file quietly keeps its legacy name. The resolver deliberately does not whitelist the vocabulary (the servlet owns its request contract). The symptom is visible where it matters: the downloaded file does not carry the expected prefix. **Check all three filenames after setting this key, not just one.**

**Currently set locally to the proven test templates** `ZZ_TEST_ER` / `ZZ_TEST_CDH` / `ZZ_TEST_DEMO`. Runtime-verified 2026-09-08 — `ZZ_TEST_ER_20260908091934.txt`, `ZZ_TEST_CDH_20260908091939.txt`, `ZZ_TEST_DEMO_20260908091948.txt`. **Production names are not yet decided** and must come from the production Summit tenant, not from these test values.

**Related to D-90** but independent of it: D-90 decides what rows go *inside* file 2, this decides what the file is *called*. Neither blocks the other.

**Applies to:** Kevin's local dev database / local Tomcat ✅ — set to the `ZZ_TEST_*` templates and runtime-verified 2026-09-08. Production ⬜ — needed before any file is uploaded to Summit; requires the production tenant's real template names first. Demo PSP, BPO, Master: not running the Summit export; apply only if/when one of them does.

---

### D-92: `ssa.properties` may set `SUMMIT_BRANCH_CODE` — optional; the Summit-side template mapping is not

**Priority:** LOW for the AMS side (defaults cleanly), **HIGH for the Summit side** — without the template mapping, file 4 does not import at all
**Status:** No AMS action required. ⚠️ **The Summit-side template change is required and is not an AMS deployment step.**

`ssa.properties` **may** set `SUMMIT_BRANCH_CODE`; it **defaults to `AMS` when unset**, so no action is required to deploy. It fills the mandatory final column (L) of the Demographics import template. Read at request time through `AppConfig`, so a change needs a Tomcat restart — but since the default is a real value, an installation that never sets it is fully functional.

⚠️ **The Summit-side `ZZ_TEST_DEMO` template must map `Branch Code` as the final mandatory element**, after `E-mail Address` and `Mailing Address Line 2`, or the file will not import. That is Summit configuration, not an AMS deployment step, and **it must exist on any tenant AMS exports to**. The same applies to whatever the production tenant's Demographics template is named.

**Why the column exists at all.** A trailing empty optional field breaks Summit's parse, and `Mailing Address Line 2` sits at K and is blank on most rosters. `Branch Code` is mandatory, always populated, has no downstream consumer in SSA's usage and is part of no identity — so its reversal cost is a template edit, not orphaned records. ⚠️ **Nothing may be appended after it**: a new optional field goes before it and `Branch Code` stays last.

⭐ **Import-proven 2026-09-08** in the A–L order with `AMS` in column L: five of six rows created, including all three rows with an empty column K. The sixth failed on `Mailing Address Line 1` exceeding 50 characters (T193), not on order or on the sentinel.

**Applies to:** Kevin's local dev database / local Tomcat ⬜ — no AMS change needed; the `ZZ_TEST_DEMO` template already carries the mapping as of the 2026-09-08 hand-built import. Production ⬜ — the production Demographics template must map `Branch Code` last before any file 4 is uploaded. Demo PSP, BPO, Master: not running the Summit export; apply only if/when one of them does.

### D-93: `SUMMIT_IMPORT_TEMPLATES` needs a fourth entry, `enrollment:<templateName>`

**Priority:** MEDIUM — nothing is broken today; without it the HRA Enrollment download keeps its legacy descriptive filename (`hra-enrollment-…`), which **no Summit import template will bind to**
**Status:** Not started anywhere. **No new config key** — this is a fourth entry inside the existing `SUMMIT_IMPORT_TEMPLATES` key from [D-91](#d-91-ssaproperties-needs-summit_import_templates-plus-a-tomcat-restart), and everything D-91 says about format, prefix collisions, tolerant parsing and the Tomcat restart applies unchanged.

S30-A added `type=enrollment` to `SummitExportServlet` — the proven chain's fourth file, HRA Enrollment. Filename resolution runs through the same `SummitImportTemplateResolver.templateNameFor()` call the other three use, and the resolver deliberately does not whitelist the discriminator vocabulary, so **no resolver code change was needed and none was made**. The only deployment action is the extra entry:

```
SUMMIT_IMPORT_TEMPLATES=employer:<erName>,cdhplan:<cdhName>,demographics:<demoName>,enrollment:<enrollName>
```

⚠️ **A Summit-side HRA Enrollment import template must exist on the tenant** with the five columns in this order — `Employer TPA Custom ID`, `Participant TPA Custom ID`, `Import Plan ID`, `Effective Date`, `Participant Annual Election Amount` — delimited `|`, dates `YYYYMMDD`, no header, no footer, no body record indicator, Extraneous Data No. That template is Summit configuration, not an AMS deployment step. The layout was import-proven by a hand-built file on 2026-09-08; **the emitter that produces it has never been run** (T196).

⚠️ **Absence is a supported state.** With no `enrollment` entry the file still downloads, under `hra-enrollment-{employer}-{prospectId}-{yyyyMMdd}.txt`. Nothing degrades; the file simply cannot be bound to a Summit template until the entry is set — which is exactly the state the other three files were in before D-91.

⚠️ **Check the new filename against the prefix-collision rule** in D-91 before setting it. Summit binds on filename prefix, so `enrollName` must not be a prefix of, nor prefixed by, any of the other three. `SummitImportTemplateResolver` logs a `WARN` on collision and still serves — grep `catalina.out` for `[SUMMIT-EXPORT] SUMMIT_IMPORT_TEMPLATES prefix collision:` after the restart.

**Applies to:** Kevin's local dev database / local Tomcat ⬜ — needed before the first live execution of the emitter (T196). Production ⬜ — needed before any HRA Enrollment file is uploaded from Production. Demo / BPO / Master — N/A, no Summit tenant.

### D-94: `ssa.properties` may set the Summit CDH optional-element keys — all optional, all default safely

**Priority:** LOW for the AMS side (every key defaults, and unset means today's behaviour exactly), **HIGH for the Summit side** — the optional elements must be mapped on the `Employer CDH Plan` template before any file carrying them will import
**Status:** Not started anywhere. **No new schema and no admin screen** — S31-H deliberately made every one of these a property, so nothing here needs a migration.

S31-H let file 2 emit the `Employer CDH Plan` template's **optional** elements after its eight mandatory columns. Which elements exist, and in what order, is a property of the Summit-side template, so AMS conforms to config rather than compiling an order in. **Eight new keys, none required:**

⚠️ **There is deliberately no grace-period day-count key.** S31-H had one (`SUMMIT_CDH_GRACE_DAYS`, default `75`) and **S31-I removed it**: Treas. Reg. §1.125-1(e) caps a grace period at the fifteenth day of the third calendar month after the plan year ends, which varies with the year-end month and which 75 days overshoots by a day for a 31 December year end. The grace period is emitted as a computed **date**, so there is nothing to configure. Do not add the key back.

| Key | Default when unset | What it does |
|---|---|---|
| `SUMMIT_CDH_OPTIONAL_ELEMENTS` | *(empty — emit nothing extra)* | Ordered, comma-separated element list; **config order is emit order** |
| `SUMMIT_CDH_GRACE_FIELDS` | *(empty — no plan has grace)* | `keySegment:applicationFieldKey` pairs, e.g. `FSA:hfsa_roll_or_grace,DCAP:dcap_grace` |
| `SUMMIT_CDH_RUNOUT_DAYS` | `90` | Run-out days after plan year end |
| `SUMMIT_CDH_TERM_RUNOUT_DAYS` | `90` | Run-out days after termination |
| `SUMMIT_CDH_TERM_RUNOUT_TYPE` | `1` | Summit's "days after termination" type |
| `SUMMIT_CDH_BOOL_TRUE` | `true` | How a Boolean true is written |
| `SUMMIT_CDH_BOOL_FALSE` | `false` | How a Boolean false is written |

⭐ **Unset means today's behaviour, exactly.** With `SUMMIT_CDH_OPTIONAL_ELEMENTS` absent no extra column is appended and file 2's bytes are identical to what it emitted before S31-H. **This is safe to deploy before it is configured** — nothing degrades, and an installation that takes the commit without editing `ssa.properties` sees no change whatsoever. Read at request time through `AppConfig`, so **Tomcat must be restarted** to pick any of them up.

Worked example — **the element names are the vendor's**, read out of the installation's own Summit template and deliberately not written into source:

```
SUMMIT_CDH_OPTIONAL_ELEMENTS=RUNOUT_ENABLED,RUNOUT_BY_DATE,RUNOUT_DAYS,TERM_RUNOUT_TYPE,TERM_RUNOUT_DAYS,GRACE_ENABLED,GRACE_BY_DATE,GRACE_DAYS
SUMMIT_CDH_GRACE_FIELDS=FSA:hfsa_roll_or_grace,DCAP:dcap_grace
```

⚠️ **`ZZ_TEST_CDH` currently maps no optional elements at all.** Setting `SUMMIT_CDH_OPTIONAL_ELEMENTS` before the Summit-side template maps them produces a file that **will not import**. The Summit-side change is required, is not an AMS deployment step, and must come first.

⚠️ **The order must match the template's element order exactly.** Summit binds optional elements **positionally**. A wrong order does not fail — it loads each value into the neighbouring field and imports cleanly.

⚠️ **An unrecognised token refuses the export**, unlike every sibling resolver's tolerant skip. That is deliberate: a skipped token would shift every element after it. Check `catalina.out` for `[SUMMIT-EXPORT] SUMMIT_CDH_OPTIONAL_ELEMENTS contains an unrecognised element token` after a restart.

⚠️ **A malformed `SUMMIT_CDH_GRACE_FIELDS` entry is skipped with a `WARN`, and a skipped entry is indistinguishable at emit time from a key segment that was never listed** — that plan quietly takes the empty-grace path rather than refusing. Grep `catalina.out` for `SUMMIT_CDH_GRACE_FIELDS entry` after changing this key rather than trusting it parsed.

⚠️ **Boolean representation is unproven** — nothing has been imported to establish whether Summit wants `true`/`false`, `1`/`0` or `Y`/`N`, which is why both sides are config. One import settles it and the fix is then editing these two keys, not a build.

**Applies to:** Kevin's local dev / local Tomcat ⬜ — needed only when testing the optional block. Production ⬜ — needed only once the production `Employer CDH Plan` template maps optional elements; **leaving every key unset is a correct and complete configuration** until then. Demo / BPO / Master — N/A, no Summit tenant.
