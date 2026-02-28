# BPO Implementation — Build Plan

**Target:** Functional demo by late March 2026 (pre-conference presentation)
**Demo Model:** Single-instance, role-separated users. PSP outsources to "itself" as BPO.
**Cross-system API sync:** Designed for but deferred past March. Data model supports it from day one.

---

## Build Order Summary

| Step | Description | Dependencies | Status |
|------|-------------|--------------|--------|
| 1 | Data model changes (migration script) | None | ✅ Done (V011, V016) |
| 2 | Auth & role routing (BPO login flow) | Step 1 | ✅ Done |
| 3 | BPO Home servlet + landing page JSP | Step 2 | ✅ Done |
| 4 | Revised delegation logic in Task Manager | Step 1 | ✅ Done |
| 5 | BPO ToDo workflow (complete / verify / revert) | Steps 3, 4 | ✅ Done |
| 6 | ToDo notes/communication layer | Step 5 | ✅ Done |
| 7 | BPO initialization path (deployment key) | Steps 1, 2 | 📋 Planned |
| 8 | Vendor Management admin page (D-26) | Step 1 | 📋 Planned |
| 9 | Demo data & walkthrough script | All above | 📋 Planned |

---

## Step 1: Data Model Changes

**Goal:** Get the database ready for everything else.

### ToDo table additions
- `bpo_completed` (boolean, default false) — BPO user has flagged this done
- `bpo_completed_date` (date, nullable) — when BPO flagged it
- `bpo_completed_by_id` (FK to person, nullable) — which BPO user
- `todo_guid` (varchar 36, unique, not null) — UUID for cross-system sync identity
- `is_reverted` (boolean, default false) — PSP sent it back to BPO after BPO completed

### New table: `todo_note`
- `note_id` (PK, auto-increment)
- `todo_id` (FK to todo)
- `created_by_id` (FK to person)
- `created_date` (datetime)
- `note_text` (text)
- `source_type` (varchar 10) — 'PSP' or 'BPO' — visual differentiation in UI

### New table: `bpo_registration` (future-proofing for cross-system)
- `bpo_reg_id` (PK, auto-increment)
- `psp_id` (FK to psp)
- `bpo_name` (varchar 100)
- `bpo_url` (varchar 255, nullable) — for future API sync
- `is_active` (boolean, default true)
- `date_registered` (date)

For single-instance demo: one row where the PSP registers "itself" as the BPO.

### Deliverables
- Migration script `V0XX__bpo_foundation.sql` (next available version number)
- Updated `migration_tracker.md`
- Updated `schema_version_migration.sql`
- JPA entity updates: `ToDo.java`, new `ToDoNote.java`, new `BpoRegistration.java`

---

## Step 2: Auth & Role Routing

**Goal:** BPO users can log in and land on their own page.

### AuthDAO.assignUserRoles()
Add cases to the switch statement:
- `case 101: isBpo = true; break;`
- `case 102: isBpoAdmin = true; break;`
- `case 103: isBpoUser = true; break;`

Set new session attributes: `isBpo`, `isBpoAdmin`, `isBpoUser`.

### AuthenticateUser.goToPage()
Add BPO routing before the existing agent check:
```java
if (isBpo || isBpoAdmin || isBpoUser) {
    response.sendRedirect("BpoHome");
} else if (isAgent || isAgencyAdmin) {
    response.sendRedirect("AgentHome");
} else {
    response.sendRedirect("ViewHome25");
}
```

### LoginFilter
No changes expected — BPO users authenticate through the same login page.

### navbar25.jsp
Add BPO-aware conditional block (similar to agent/PSP blocks). BPO users see a simplified nav with their relevant actions only.

### Deliverables
- Modified `AuthDAO.java`
- Modified `AuthenticateUser.java`
- Modified `navbar25.jsp` (BPO nav section)

---

## Step 3: BPO Home Servlet + Landing Page

**Goal:** BPO users land on a functional dashboard after login.

### BpoHome servlet (`/BpoHome`)
- Load user's personal checklists (left column) — reuse existing checklist query scoped to logged-in user
- Load delegated ToDos where `task.isSourced = true` and `task.sourceOwner` matches BPO person (or all open BPO ToDos if unfiltered)
- Forward to `bpoHome25.jsp`

### bpoHome25.jsp layout
- **Left column:** Personal checklists (reuse existing checklist component/include)
- **Right column:** Delegated ToDo table with columns: ToDo Name | PSP Name | Due Date
  - Sortable by clicking column headers (JS sort or server-side)
  - Filter toggle: "My ToDos" vs "All Open BPO ToDos"
  - Click ToDo name → navigate to ToDo detail view

### Deliverables
- New `BpoHome.java` servlet
- New `bpoHome25.jsp`
- New DAO method (or extend existing) to query sourced ToDos for BPO users

---

## Step 4: Revised Delegation Logic in Task Manager

**Goal:** Implement the new tri-state: Internal / Source but Verify / Vendor Only.

### Current state
- `isSourced` (boolean) + `allowNonOwner` (boolean) creates 3 states
- Current meaning: not sourced / sourced (anyone completes) / sourced (vendor only)

### New meaning
- **Internal** (`isSourced=false`): No change. PSP handles it.
- **Source but Verify** (`isSourced=true`, `allowNonOwner=true`): BPO completes → sets `bpo_completed=true` → unlocks for PSP to verify/close. PSP can also revert.
- **Vendor Only** (`isSourced=true`, `allowNonOwner=false`): BPO completes → ToDo is done. No PSP verification step.

The existing field semantics actually map well. The behavioral change is in how `bpo_completed` interacts with `is_complete`:
- "Source but Verify": BPO action sets `bpo_completed=true` but NOT `is_complete=true`. PSP must set `is_complete=true`.
- "Vendor Only": BPO action sets both `bpo_completed=true` AND `is_complete=true`.

### UI changes in taskManager25.jsp
- Rename button labels: "sourced" → "Source but Verify", keep "vendor only"
- Update status text descriptions to match new semantics
- BPO dropdown continues to populate from registered vendors (D-26 / `bpo_registration` table)

### Deliverables
- Modified `taskManager25.jsp` (label and status text changes)
- Modified `UpdateTask25.java` (if any logic changes needed — likely minimal since fields stay the same)
- Document the semantic change clearly

---

## Step 5: BPO ToDo Workflow

**Goal:** BPO users can complete ToDos. PSP users can verify or revert.

### BPO side — completing a ToDo
New servlet: `CompleteBpoToDo` (or extend existing completion logic)
- BPO user clicks "Complete" on a delegated ToDo
- Sets `bpo_completed = true`, `bpo_completed_date = now`, `bpo_completed_by = current person`
- If task is "Vendor Only" (`allowNonOwner=false`): also sets `is_complete=true`, `date_completed`, `completed_by`
- If task is "Source but Verify" (`allowNonOwner=true`): leaves `is_complete=false` — ToDo becomes unlocked for PSP

### PSP side — verify/close
- PSP user sees a previously-locked ToDo is now unlocked (BPO completed their part)
- Visual indicator: "BPO Completed — Awaiting Verification" state
- PSP user checks it off → normal completion flow (`is_complete=true`)

### PSP side — revert
New action: "Revert to BPO"
- Sets `bpo_completed = false`, `is_reverted = true`
- ToDo re-locks for PSP, re-appears as open on BPO dashboard
- Optional: auto-create a ToDoNote explaining the revert

### Display state logic updates
`ToDoOut25.computeAllDisplayStates()` needs new states:
- **BPO Locked:** sourced task, BPO hasn't completed yet → PSP can't check it off
- **BPO Completed / Awaiting Verify:** `bpo_completed=true`, `is_complete=false` → PSP can now check it off or revert
- **BPO Complete (final):** vendor-only task, both flags true → done

### Deliverables
- New or modified servlet for BPO ToDo completion
- New servlet/action for PSP revert
- Modified `ToDoOut25` display state computation
- Modified `toDoListFormNew.jsp` for new visual states (locked, awaiting verify, revertable)
- BPO-side ToDo detail view (similar to `activityDetail25.jsp` but scoped for BPO)

---

## Step 6: ToDo Notes / Communication Layer

**Goal:** PSP and BPO users can communicate about a specific ToDo via a note log.

### ToDoNote entity
- Maps to `todo_note` table from Step 1
- Fields: id, todo (M:1), createdBy (M:1 Person), createdDate, noteText, sourceType

### UI — BPO side
- ToDo detail view shows note history below the task info
- Visual differentiation: PSP notes vs BPO notes (different background color or icon)
- "Add Note" form at bottom

### UI — PSP side
- In the ToDo list or task manager, a delegated task shows a "Notes" link/icon
- Click opens note history for that ToDo (inline expand or modal)
- Can add notes from PSP side

### For single-instance demo
- Both sides are just different views on the same `todo_note` table
- `source_type` is set based on the role of the logged-in user

### For future cross-system
- Notes live on the BPO database
- PSP accesses them via API using the `todo_guid`
- Data model already supports this with the GUID field

### Deliverables
- New `ToDoNote.java` entity
- New `ToDoNoteDAO.java` (add/list notes for a ToDo)
- New servlet: `AddToDoNote` / `ViewToDoNotes`
- UI components for note display + entry on both BPO and PSP views

---

## Step 7: BPO Initialization Path

**Goal:** A new deployment can be initialized as a BPO provider via deployment key prefix.

### DatabaseInitializer changes
- Check if deployment key starts with "BPO"
- If BPO: seed BPO-specific roles, skip PSP-specific seed data (employers, benefits, etc.)
- Seed a minimal BPO admin user instead of PSP admin
- Skip sales-related seed data (LOS, rates, agencies)

### What BPO init includes
- Person + User for BPO admin
- UserRoles 101, 102, 103
- Basic constants
- PSP table entry with BPO flag (may need a new column: `is_bpo` on PSP table, or use a constant)

### What BPO init skips
- Template groups/purposes (those belong to PSPs)
- Ticket categories/subcategories
- LOS, enhancements, fee types
- The initialization checklist (replace with BPO-specific welcome checklist)

### Deliverables
- Modified `DatabaseInitializer.java` (BPO init branch)
- Migration script for any new columns needed (e.g., `psp.is_bpo`)
- Document in deployment backlog

---

## Step 8: Vendor Management Admin Page (D-26)

**Goal:** PSP admins can register BPO vendors instead of hardcoding them.

### Admin page
- List existing BPO registrations (from `bpo_registration` table)
- Add new: name, URL (optional for now)
- Edit/deactivate existing
- For single-instance demo: register the local installation as a BPO

### Task Manager integration
- BPO source dropdown in `taskManager25.jsp` populates from `bpo_registration` + associated BPO persons
- Replaces the current hardcoded person-based dropdown

### Deliverables
- New servlet: `VendorManager` or similar
- New JSP: `vendorManager25.jsp`
- Modified task manager dropdown source
- Close out D-26 in deployment backlog

---

## Step 9: Demo Data & Walkthrough Script

**Goal:** Set up a compelling demo scenario for the conference.

### Demo scenario
1. PSP Admin configures tasks with "Source but Verify" and "Vendor Only" delegation
2. PSP Admin registers local BPO in vendor management
3. Activity is created with a checklist containing delegated tasks
4. Log in as BPO User → see delegated ToDos on BPO dashboard
5. BPO User completes a "Source but Verify" task → adds a note
6. Log in as PSP User → see the task is now unlocked, review BPO note
7. PSP User verifies and closes the task
8. (Bonus) PSP User reverts a different task back to BPO with a note

### Deliverables
- Demo seed data script or servlet (builds on D-24 concept)
- Written walkthrough/script for the presentation
- Screenshots or recording if needed

---

## Timeline Estimate

| Week | Steps | Focus |
|------|-------|-------|
| Week 1 (Feb 24 – Mar 2) | 1, 2 | Data model + auth routing |
| Week 2 (Mar 3 – 9) | 3, 4 | BPO landing page + delegation logic |
| Week 3 (Mar 10 – 16) | 5 | ToDo workflow (the core feature) |
| Week 4 (Mar 17 – 23) | 6, 7, 8 | Notes, init path, vendor mgmt |
| Week 5 (Mar 24 – 28) | 9 | Demo prep, polish, walkthrough |

---

## Design Decisions for Discussion

1. **`bpo_registration` vs reusing `psp` table:** The concept doc implies BPOs might be tracked in the PSP table with a flag. A separate `bpo_registration` table is cleaner for the PSP→BPO relationship and avoids overloading PSP. Thoughts?

2. **ToDo GUID generation:** Auto-generate UUID on ToDo creation (`@PrePersist` hook), or only for sourced ToDos? Recommend: all ToDos get one — it's cheap and simplifies future sync.

3. **Revert history:** Should reverts be tracked (how many times, who, when) or is a single `is_reverted` boolean sufficient? For demo, boolean is fine. For production, a revert count or history table might be valuable.

4. **BPO sees which PSP?** In single-instance mode, there's only one PSP. But the `bpo_registration` table and the ToDo list's "PSP Name" column are designed for multi-PSP from day one.

5. **Navbar for BPO:** Minimal nav (Home, Logout, maybe Settings) or should we plan for future expansion (reports, admin)?
