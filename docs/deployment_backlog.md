# Deployment Backlog

**Last Updated:** February 24, 2026 (BPO Feature Session)
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
- Demo benefits (HRA, FSA, COBRA) and ticket categories — to be moved to demo seeder servlet (see D-24)

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
- Receives health check data from all PSP VPSes and displays status grid
- PSP instance registry: list of all deployed PSPs with status, domain, version, last backup
- Deployment feedback: surface errors, version drift, failed updates
- "Master Admin" concept that manages PSP installs

**Architecture decision:** Build within the AMS app (role-gated) rather than a separate website. The infrastructure and auth system already exist.

**When built:** Push migration to set `SYS_HEALTH_ENABLED=false` across all PSPs to stop email reports. Update deployment runbook Phase 7 to reference the new dashboard workflow.

---

### D-22: Investigate IONOS vCPU Core Quota

**Priority:** MEDIUM
**Status:** Not started

Determine if IONOS has a per-account limit on vCPU cores and whether additional quota needs to be requested before spinning up multiple PSP VPSes.

---

### D-24: Create Demo Seeder Servlet

**Priority:** MEDIUM
**Status:** Partially started (SeedBpoDemoData exists, needs purpose-built demo data with clear names)

Create a protected servlet (master admin only) that seeds demo data onto a fresh initialized database for demonstration purposes. Should include:

- Demo benefits (HRA, FSA, COBRA) with realistic dates
- Sample employers and employees with obvious names (e.g., "Acme Corp", "Widget Inc")
- Sample activities (tickets, renewals, setups) with sourced tasks for BPO demo
- BPO test users with role assignments

This replaces the test data previously hardcoded in `DatabaseInitializer`. Demo data should be clearly identifiable and removable.

---

### D-25: Manual Benefit Creation UI

**Priority:** MEDIUM
**Status:** Not started

Build an admin page to manually create Benefit records without requiring a Summit import. Currently benefits can only enter the system through the Summit CSV import pipeline or the demo data in the initializer.

---

### D-26: Vendor Management Admin Page

**Priority:** MEDIUM
**Status:** Not started

Build an admin page to register third-party vendor contacts (persons + users with BPO roles). Replaces the hardcoded Accelergent persons previously in `DatabaseInitializer`. Should allow PSP admins to:

- Create vendor person records
- Create vendor user accounts with appropriate roles (101/102/103)
- View/edit/deactivate existing vendors

The BPO source dropdown in task manager will populate from these registered vendors.

---

### D-29: BPO Task Assignment from BPO Dashboard

**Priority:** MEDIUM
**Status:** Not started (design completed, code ready to implement)

Add an "Assign To" dropdown inside the BPO task detail modal so BPO admins can assign unassigned delegated tasks to specific BPO users. Uses the `bpo_assigned_to_id` column on the `todo` table. The modal dropdown pre-selects the current assignee and updates via AJAX through the existing `BpoCompleteTask` servlet.

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

### D-32: Add BRANDING_PATH to ssa.properties on Production

**Priority:** HIGH — Required before deploying branding upload feature
**Status:** Not started

Add `BRANDING_PATH=/var/lib/tomcat10/branding/` to `ssa.properties` on production (and any PSP VPS instances). Create the directory with proper Tomcat ownership:
```bash
sudo mkdir -p /var/lib/tomcat10/branding
sudo chown tomcat:tomcat /var/lib/tomcat10/branding
```

The branding upload servlet (`UploadPspBranding`) now saves logos and favicons to this external directory instead of inside the webapp. The `ServeBrandingFile` servlet serves them at `/branding/*`. Without this config, branding uploads will fail with a "BRANDING_PATH not configured" error.

**Also applies to:** Master VPS image — update snapshot after adding this.

---

### D-33: Update Master VPS Blank Schema to V013

**Priority:** HIGH — Required before onboarding new PSPs
**Status:** Not started

The master VPS image (`SSA-Master-Base-v4-2026-02-23`) contains the original blank schema (pre-V001). New PSP instances cloned from this image would need to run the full V001–V013 migration chain before initialization.

**Action:** Replace `/opt/ssa/schema/beta_ssa_blank.sql` on the master VPS with the validated V013 baseline (`docs/importscript/beta_ssa_dev_baseline_thru_V013.sql`). Then take a new snapshot (`SSA-Master-Base-v5-2026-02-XX`).

This ensures new PSP clones start at V013 and only need incremental migrations going forward.

**Also update:**
- `deployment_runbook.md` — master snapshot version reference
- `deployment_strategy.md` §2.2 — master VPS image version

---

## Remaining TODOs

- Run `schema_version_migration.sql` on production database (holding until further testing)
- Update master VPS snapshot version in runbook after future image updates
- Update deployment runbook Phase 7 when PSP admin dashboard is built (D-14)
- Reserve static IPs in IONOS for each PSP deployment
