# Session Summary — March 2–3, 2026 (Late Evening)

## Demo PSP Standup for Datapath Client Conference

### Overview

Stood up the demo PSP instance at `demo.superiorstate.biz` for the Datapath Client Conference (April 20–22, 2026). Cloned from master snapshot `SSA-Master-Base-v7-2026-03-02`, configured, deployed, and connected to the existing BPO instance at `bpo.superiorstate.biz`. Discovered and fixed several cross-system issues during validation.

---

### Phase 1–2: Provisioning & DNS
- Cloned `SSA-Master-Base-v7-2026-03-02` in IONOS DCD (US-Las Vegas)
- VM named `SSA-Demo-PSP`
- **Static IP reserved successfully:** `192.152.28.73`
- DNS A record created: `demo.superiorstate.biz` → `192.152.28.73`
- DNS verified via `nslookup`

### Phase 3: VM Configuration
- `ssa.properties` configured: `PSP_ID=DEMO_PSP`, `SYSTEM_URL=https://demo.superiorstate.biz`, plus Wasabi, GitHub PAT, and deployment key
- Data directory verified: `/var/lib/tomcat10/data/` (tomcat:tomcat ownership)
- Branding directory verified: `/var/lib/tomcat10/branding/` (tomcat:tomcat ownership)

### Phase 4: WAR Deployment
- Update script ran successfully (silent — no terminal output, noted for fix)
- ROOT.war deployed (90MB), Tomcat started
- Startup confirmed: `✅ ssa.properties loaded (PSP_ID=DEMO_PSP)`, `🔁 Skipping global data load – database not initialized`

### Phase 5: SSL
- Certbot issued cert for `demo.superiorstate.biz` (expires June 1, 2026)
- **Issue 1:** APR/OpenSSL native library conflict — `cannot create new ssl` errors
  - **Fix:** Commented out `AprLifecycleListener` in `server.xml` to force pure Java NIO SSL
- **Issue 2:** SSL still failing — `PEMFile` permission denied on `privkey.pem`
  - **Fix:** `chmod 755` on `/etc/letsencrypt/live` and `/etc/letsencrypt/archive` directories, `chmod 644` on `privkey1.pem`
  - **Note:** Same fix was needed on BPO setup — should be added to deployment runbook
- HTTPS working after fixes

### Phase 6: Initialization
- Navigated to `/initialize.jsp`, filled form, PSP initialized
- Admin login confirmed

### Phase 7: Demo Data
- `/SeedDemoData` executed — employers, employees, benefits, activities, demo users seeded

### Phase 8: PSP ↔ BPO Partnership

**Issue 3:** Runbook had the flow backwards — the **PSP initiates** the partnership request to the BPO, not the other way around.

**Issue 4:** Seed data created a fake `bpo_registration` row pointing to `https://accelvantage.com` with no API tokens. Had to delete it and create a fresh registration via the UI.

**Issue 5:** Partnership request returned HTTP 302 — `LoginFilter` was intercepting `/api/v1/partnership/request` on the BPO and redirecting to `/login`.
- **Fix:** Added `/api/` bypass to `LoginFilter.java`:
  ```java
  if (path.startsWith("/api/")) {
      chain.doFilter(req, res);
      return;
  }
  ```
- Rebuilt WAR, deployed to **both** Demo PSP and BPO instances
- Partnership request succeeded after fix
- BPO admin approved partnership — both sides show APPROVED with tokens exchanged

### Phase 9: Automated Systems
- Backup script ran (silent but successful — 1 file in Wasabi)
- Health check ran — report generated:
  - Tomcat/MySQL running
  - "DB Initialized: Not initialized" (display-only issue)
  - Errors listed were all from the earlier SSL permission issue (aged out)
  - WAR version: v0.31.0

---

### Cross-System Validation — Issues Found

#### Vendor Sourcing Hidden in ManageTask25
- `AmsDataGlobal.getActiveBpoRegistrations()` caches at startup
- New BPO registration added after startup wasn't visible
- **Fix:** Restart Tomcat to reload global cache
- **Future:** Consider cache refresh on registration changes

#### BPO Task Push Not Firing for Tickets
- `BpoTaskPushService.pushDelegatedTasks()` only hooked into `AddRenewal25` and `CreateChecklist25`
- `CreateTicket25.createCheckListForTicket()` does not call the push service
- Same gap exists in `ReviewApplication`, `GenerateProp`, `CreateSetup25`
- **Workaround:** Tested via renewal path (HRA task) — push worked
- **Fix needed:** Add push hooks to all creation paths (see Claude Code prompt)

#### BPO Completed Tasks Not Showing
- `BpoCompletedTasks.java` queries local `ToDo` table only
- Cross-system mode stores completed tasks in `DelegatedToDo` table
- Completed tasks section on BPO dashboard shows empty
- **Fix needed:** Add cross-system query branch (see Claude Code prompt)

#### Cross-System Callback — Actually Working
- Initial concern that PSP wasn't receiving completion callbacks
- Investigation confirmed: `bpo_completed = 1` on PSP ToDo records
- Callbacks are working — both "Vendor Only" (auto-complete) and "Source but Verify" (awaiting PSP verification) behaving correctly
- The display on PSP side may need visual update to reflect BPO completion state

#### BPO User Auth Errors
- `userRoleList` NPE on BPO login attempts — demo users seeded by PSP's `SeedDemoData` don't exist in BPO database
- BPO admin account works (created during BPO initialization)

---

### Code Changes This Session

| File | Change | Deployed To |
|------|--------|-------------|
| `LoginFilter.java` | Added `/api/` path bypass for cross-system API calls | Demo PSP + BPO |

**No database migrations produced.** All changes were infrastructure provisioning and one Java code fix.

---

### Current Environment Status

| Environment | URL | IP | Type | Schema | WAR | Status |
|-------------|-----|----|------|--------|-----|--------|
| Production | https://superiorstate.biz | (prod IP) | PSP | V024 | Pre-BPO | Running (untouched) |
| **Demo PSP** | **https://demo.superiorstate.biz** | **192.152.28.73** | **PSP** | **V031** | **v0.31.0** | **Running, demo data, BPO connected** |
| BPO | https://bpo.superiorstate.biz | 158.222.102.168 | BPO | V031 | v0.31.0 | Running, connected to demo |
| Master | master.superiorstate.biz | 208.94.39.77 | Master | V031 | None | Snapshot v7, stopped |

---

### Pending Items (Claude Code Prompt Created)

1. **Push on ToDo creation** — all creation paths (ticket, setup, manual add)
2. **Push on task update** — when sourced task metadata changes
3. **Push on vendor assignment/removal** — sourced ↔ internal transitions
4. **BpoCompletedTasks** — add cross-system `DelegatedToDo` query
5. **PSP checklist display** — visual indicator for BPO completion state
6. **Logging** — add observability to all cross-system API calls
7. **Update script verbosity** — echo output on manual runs

### Runbook Updates Needed

- Add SSL cert permission fix (`chmod 755` on letsencrypt dirs, `chmod 644` on privkey)
- Correct partnership flow direction (PSP → BPO, not BPO → PSP)
- Note that `AprLifecycleListener` must be disabled in `server.xml`
- Note Tomcat restart needed after first partnership approval (global cache)
