# SSA Web Application - Migration Strategy & Implementation Plan

**Created:** Response to developer clarifications
**Tool:** IntelliJ IDEA 2022.2.2 Ultimate + GitHub integration
**Approach:** Working backwards from main screens (Home & Activity)

---

## 1. TARGET "TO-BE" PACKAGE HIERARCHY

### Current State (Mixed)
```
src/main/java/net/superiorstate/ams/
├── controller/                          (current, "25" suffix)
├── previous/
│   ├── controller/                      (legacy, still used)
│   │   ├── authentication/
│   │   ├── general/admin/
│   │   ├── activity/
│   │   ├── checklist/
│   │   └── billing/
│   ├── archive/                         (DELETE - confirmed dead)
│   └── [other packages]
└── [other packages]
```

### Proposed "TO-BE" Hierarchy (Clean)
```
src/main/java/net/superiorstate/ams/
├── controller/                          ← All servlets here
│   ├── authentication/                  ← Login, logout, password reset
│   ├── admin/                          ← Admin dashboard functions
│   ├── activity/                       ← Activity management (home, detail, CRUD)
│   │   ├── checklist/                  ← Checklist features
│   │   ├── renewal/                    ← Renewal features
│   │   └── sequence/                   ← Sequence builders
│   ├── email/                          ← Email sending features
│   ├── billing/                        ← Billing reports
│   ├── upload/                         ← File upload/import
│   └── api/                            ← (Future) REST endpoints
├── service/                            ← (Future) Business logic
│   ├── authentication/
│   ├── activity/
│   ├── email/
│   └── billing/
├── data/                               ← Data access (keep existing)
│   ├── model/
│   └── dao/
├── model/                              ← Domain models (keep existing)
├── filter/                             ← Filters (LoginFilter, CsrfFilter)
└── util/                               ← Utilities (keep existing)
```

### Key Principles
1. **Flat controller package** - no deep nesting (max 2 levels)
2. **Feature-based organization** - group by domain (activity, billing, email)
3. **No "previous" package** - everything current goes in controller
4. **No version suffixes needed** - "25" suffix becomes obsolete when everything is current
5. **Prepare for services** - eventually extract business logic from servlets

### Migration Path
```
Phase 1: previous.controller.authentication    → controller.authentication
Phase 2: previous.controller.general.admin     → controller.admin
Phase 3: previous.controller.activity.*        → controller.activity.*
Phase 4: previous.controller.checklist         → controller.activity.checklist
Phase 5: previous.controller.billing           → controller.billing
```

---

## 2. PROJECT KNOWLEDGE & FILE PRESERVATION

### What Project Currently Has
Your project knowledge system already has access to:
- All servlet files we queried
- All JSP files we examined
- Configuration files (web.xml)
- The conversation transcript

### Files to Add to Project (61% capacity consideration)

**PRIORITY 1 - Critical Documentation (Add these)**
```
/docs/analysis/
├── servlet_inventory.md           (~50KB) ← Master servlet list
├── application_flow.md            (~40KB) ← Entry points & flows
├── cleanup_recommendations.md     (~30KB) ← Action plan
└── migration_strategy.md          (~20KB) ← This file
Total: ~140KB
```

**PRIORITY 2 - Working Documents (Optional, if capacity allows)**
```
/docs/working/
├── jsp_inventory.md               (~60KB) ← JSP mapping
└── refactor_checklist.md          (~10KB) ← Per-servlet checklist
Total: ~70KB
```

### Where to Store
Create new directory structure:
```
project-root/
├── docs/
│   ├── analysis/          ← Add the 4 analysis files here
│   ├── architecture/      ← (Future) architecture decisions
│   └── migration/         ← (Future) migration logs per sprint
└── [existing structure]
```

### Recommendation
**Add the 4 Priority 1 files now** (~140KB total). These are:
1. Reference documents you'll need during refactoring
2. Small enough to not impact capacity significantly
3. GitHub-tracked so you have version history

---

## 3. HANDLING "PREVIOUS" PACKAGE SAFELY

### Important Clarification Acknowledged
✓ "previous" ≠ unused
✓ Folder location doesn't determine usage
✓ "25" = current/near-current
✓ Must verify references before deletion

### Safe Removal Process (IntelliJ-Optimized)

**Step 1: Verify No References (Use IntelliJ)**
```
Right-click on class → Find Usages (Alt+F7)
Check:
- [ ] Java files
- [ ] JSP files  
- [ ] XML files
- [ ] JavaScript files
```

**Step 2: Mark as Deprecated First**
```java
@Deprecated
@WebServlet(name = "OldServlet", value = "/OldServlet")
public class OldServlet extends HttpServlet {
    // Keep for 1 sprint before deleting
}
```

**Step 3: Use IntelliJ Refactoring**
```
1. Verify new version works
2. Add @Deprecated to old version
3. Commit to branch
4. Monitor for 1 sprint
5. If no issues → Safe Delete (Alt+Delete in IntelliJ)
```

### Files We're 100% Sure Are Dead
Only these have ZERO references:
```
previous/archive/                    ← Entire package
  - sendAutomationFinal.java
  - emailActionsNew.java
  - doCheckListAction.java
  - goCheckListDetail.java
  - createSimpleChecklist.java
  
previous/controller/HomeServlet.java ← Empty implementation

JSPs:
  - toDoIsCompleteMain.jsp           ← Empty template
  - ttt.jsp                          ← Empty template
```

**These can be deleted immediately** (but still via branch/PR process)

---

## 4. TOP-DOWN APPROACH: Home & Activity Screens

### Starting from Main Windows

#### ViewHome25 (Dashboard) - Dependency Chain
```
Entry Point: /ViewHome25
↓
Servlet: ViewHome25.java (controller package) ✓ Already current
↓
JSP: [Need to identify which JSP]
↓
Components imported by JSP
↓
Servlets called from JSP forms/links
```

#### ViewActivity25 (Activity Detail) - Dependency Chain
```
Entry Point: /ViewActivity25
↓
Servlet: ViewActivity25.java (controller package) ✓ Already current
↓
JSP: /WEB-INF/view/a/activityDetail/activityDetail25.jsp ✓ Found
↓
Components (15+ JSP imports):
  - Checklist column components
  - Detail column components  
  - History column components
↓
Form Actions (servlets called):
  - AddNoteToActivity25 ✓ Already current
  - AddActivityContact25 ✓ Already current
  - AddToDo25 ✓ Already current
  - CloseActivity25 ✓ Already current
  - SendEmail25 ✓ Already current
  - [More to map]
```

### Top-Down Analysis Process

**Phase A: Map ViewHome25 Screen**
1. Find which JSP ViewHome25 forwards to
2. Map all servlets called from that JSP
3. Verify all are current (in controller, have "25")
4. Identify any in "previous" package
5. Create migration plan for those

**Phase B: Map ViewActivity25 Screen**
1. ✓ Already know JSP: activityDetail25.jsp
2. Map all form actions in JSP
3. Map all links/buttons in JSP
4. Verify servlet versions
5. Create migration plan

**Phase C: Work Outward**
1. Map servlets called FROM home/activity screens
2. Map servlets those call (2nd level)
3. Continue until all paths traced
4. Anything not in trace = candidate for review/removal

---

## 5. GIT BRANCH STRATEGY

### Recommended Branch Name
```
refactor/modernize-architecture
```

**Alternatives:**
- `refactor/cleanup-legacy-code`
- `feature/package-reorganization`
- `migration/consolidate-packages`

### Why This Name?
- "refactor" = code improvement without changing functionality
- "modernize-architecture" = clearly states the goal
- Not tied to specific ticket/issue numbers
- Descriptive for team members

### Branch Strategy

**Option A: Single Long-Running Branch (Recommended)**
```
main
  ↓
  refactor/modernize-architecture  ← Work here for entire migration
    ↓
    [Periodic merges back to main after each phase]
```

**Pros:**
- All related changes in one branch
- Easy to track overall progress
- Can merge incrementally after each phase

**Cons:**
- Long-running branch can diverge from main
- Need to merge main into branch frequently

**Option B: Multiple Feature Branches**
```
main
  ↓
  ├─ refactor/delete-archive-package
  ├─ refactor/migrate-authentication
  ├─ refactor/migrate-admin
  └─ refactor/migrate-activity
```

**Pros:**
- Smaller, focused PRs
- Less chance of conflicts
- Each can be reviewed independently

**Cons:**
- More branch management
- Dependencies between branches

### Recommended Approach: Hybrid
```
main
  ↓
  refactor/modernize-architecture (long-running)
    ↓
    ├─ phase-1-quick-wins (sub-branch)
    ├─ phase-2-auth-migration (sub-branch)
    └─ phase-3-admin-migration (sub-branch)
```

### Git Workflow
```bash
# Initial setup
git checkout -b refactor/modernize-architecture

# For each phase
git checkout -b phase-1-quick-wins refactor/modernize-architecture
[Make changes]
git commit -am "Delete archive package"
git push origin phase-1-quick-wins
[Create PR to refactor/modernize-architecture]
[Merge after review]

# Periodically merge back to main
git checkout main
git merge refactor/modernize-architecture
git push origin main
```

---

## COMPREHENSIVE IMPLEMENTATION PLAN

### Sprint 0: Setup (2-3 hours)
```
[ ] Create branch: refactor/modernize-architecture
[ ] Add documentation to /docs/analysis/
[ ] Commit documentation
[ ] Create GitHub project board with tasks
[ ] Set up IntelliJ run configurations for testing
```

### Sprint 1: Quick Wins + Home Screen Analysis (1 week)

**Day 1: Dead Code Removal**
```
Sub-branch: phase-1-quick-wins

Tasks:
[ ] Use IntelliJ "Find Usages" on each archive file
[ ] Verify 0 references
[ ] Safe Delete archive package (Alt+Delete)
[ ] Delete empty JSPs (ttt.jsp, toDoIsCompleteMain.jsp)
[ ] Delete HomeServlet.java
[ ] Run full build (Ctrl+F9)
[ ] Test application startup
[ ] Commit: "Remove confirmed dead code"
[ ] Test major workflows
```

**Day 2: Security Fixes**
```
[ ] Add @WebFilter("/*") to CsrfFilter
[ ] Test CSRF protection
[ ] Implement AuthenticateUser.displayLoginFailure()
[ ] Test login failure flow
[ ] Commit: "Fix critical security issues"
```

**Day 3-4: Map ViewHome25**
```
[ ] Find JSP that ViewHome25 forwards to
[ ] Document all servlets/links in that JSP
[ ] Use IntelliJ Navigate → Declaration to trace calls
[ ] Create viewHome25_dependencies.md
[ ] Identify any servlets in "previous" package
```

**Day 5: Test & Merge**
```
[ ] Full regression test
[ ] Create PR: phase-1-quick-wins → refactor/modernize-architecture
[ ] Code review
[ ] Merge
[ ] Tag: v1.0-sprint1-complete
```

### Sprint 2: Activity Screen + First Migration (1 week)

**Day 1-2: Map ViewActivity25 Dependencies**
```
Sub-branch: phase-2-activity-analysis

[ ] Document all servlets called from activityDetail25.jsp
[ ] Check each servlet version (current vs previous)
[ ] Use IntelliJ Structure view (Alt+7) to see JSP structure
[ ] Create activityDetail25_dependencies.md
[ ] Identify migration candidates
```

**Day 3-5: Migrate High-Priority Servlet**
```
[ ] Pick first servlet to migrate (likely authentication)
[ ] Use IntelliJ Refactor → Move (F6)
[ ] Update package declaration
[ ] Find Usages to update all references
[ ] Update JSP references
[ ] Test thoroughly
[ ] Commit per servlet migrated
```

### Sprint 3-N: Continue Top-Down Migration

**Each Sprint:**
1. Pick next screen/feature from home/activity
2. Map dependencies
3. Migrate 3-5 servlets
4. Test thoroughly
5. Merge to main branch

---

## INTELLIJ-SPECIFIC TIPS

### Useful Shortcuts for This Work
```
Alt+F7        - Find Usages (essential!)
Ctrl+Alt+F7   - Show Usages popup
F6            - Move/Refactor
Shift+F6      - Rename
Alt+Delete    - Safe Delete
Ctrl+F9       - Build project
Alt+7         - Structure view (see JSP includes)
Ctrl+Shift+F  - Find in path (search entire codebase)
Ctrl+H        - Type hierarchy
```

### IntelliJ Refactoring Workflow
```
1. Right-click class → Refactor → Move (F6)
2. Type new package path
3. IntelliJ will:
   - Update package declaration
   - Update imports everywhere
   - Update @WebServlet annotation if needed
   - Show preview of changes
4. Review changes
5. Click "Do Refactor"
6. Run build
7. Fix any errors IntelliJ couldn't auto-fix
```

### Testing in IntelliJ
```
1. Use Run Configurations for Tomcat
2. Set breakpoints in servlets
3. Debug mode (Shift+F9)
4. Step through code
5. Verify data flow
```

---

## DELIVERABLES BY PHASE

### Phase 1 (Sprint 1)
- [ ] Clean codebase (archive deleted)
- [ ] Security issues fixed
- [ ] viewHome25_dependencies.md
- [ ] Git tag: v1.0-sprint1-complete

### Phase 2 (Sprint 2)
- [ ] activityDetail25_dependencies.md
- [ ] First servlet migration complete
- [ ] Migration process documented
- [ ] Git tag: v1.0-sprint2-complete

### Phase 3+ (Sprints 3-N)
- [ ] All servlets migrated to controller package
- [ ] "previous" package deleted
- [ ] Updated architecture documentation
- [ ] Git tag: v2.0-migration-complete

---

## SUCCESS CRITERIA

### After Sprint 1
- ✓ No dead code in repository
- ✓ Security issues resolved
- ✓ Build still works
- ✓ All tests pass
- ✓ Home screen dependencies documented

### After Complete Migration
- ✓ All servlets in controller package
- ✓ No "previous" package
- ✓ No "25" suffixes needed
- ✓ Clear, flat package structure
- ✓ Full test coverage maintained
- ✓ Documentation up to date

---

## RISK MITIGATION

### Using IntelliJ to Reduce Risk
1. **Find Usages** - ensures no missed references
2. **Refactor → Move** - auto-updates imports
3. **Safe Delete** - warns if references exist
4. **Git integration** - easy rollback if needed
5. **Local History** - IntelliJ keeps local file history

### Safety Checklist Per Change
```
[ ] Run Find Usages before moving/deleting
[ ] Use IntelliJ refactoring tools (not manual edits)
[ ] Commit after each logical change
[ ] Run build after each change
[ ] Test affected workflows
[ ] Keep branch up to date with main
```

---

## NEXT IMMEDIATE STEPS

1. **Create branch:**
   ```bash
   git checkout -b refactor/modernize-architecture
   ```

2. **Add documentation files** to `/docs/analysis/`

3. **Start Sprint 1, Day 1:**
   - Open IntelliJ
   - Navigate to `previous/archive/` package
   - Right-click first file → Find Usages
   - Verify 0 usages
   - Safe Delete (Alt+Delete)
   - Repeat for all archive files
   - Commit: "Remove archive package"

4. **Come back for next steps** after each day/task

---

**Ready to begin?** The first task is creating the branch and adding documentation files.
