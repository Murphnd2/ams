# BPO Feature Implementation — February 24, 2026

## Overview

Built the complete BPO (Business Process Outsourcing) delegation feature across three sessions. This enables PSPs to outsource specific checklist tasks to third-party vendors (like Accelergent) who log in to their own dashboard, view delegated tasks, add notes, and mark them complete.

## Architecture

**Roles:** Three BPO roles added to UserRole table:
- 101 (Accelergent BPO) — parent/legacy role
- 102 (Accelergent BPO Admin) — admin-level BPO user
- 103 (Accelergent BPO User) — standard BPO user

**Data Model Changes (V011):**
- `todo` table: Added `bpo_completed` (boolean), `bpo_completed_date` (date), `bpo_completed_by_id` (FK→person), `bpo_assigned_to_id` (FK→person)
- New `todo_note` table: `note_id`, `todo_id` (FK), `created_by_id` (FK), `note_text`, `source_type` (PSP/BPO), `created_at`

**Entity Changes:**
- `ToDo.java`: Added bpoCompleted, bpoCompletedDate, bpoCompletedBy (Person), bpoAssignedTo (Person) fields with JPA annotations
- `ToDoNote.java` (new): JPA entity for task-level notes with source type tracking
- `ToDoOut25.java`: Added bpoCompleted, bpoAssignedTo, bpoCompletedBy fields mapped from view

**Authentication:**
- `AuthDAO.assignUserRoles()`: Added isBpo, isBpoAdmin, isBpoUser session attributes (roles 101, 102, 103)
- `AuthenticateUser.authenticateUser()`: BPO roles redirect to `BpoHome` instead of `ViewHome25`
- `AmsDataGlobal.initializeGlobalData()`: Loads BPO users from all three roles (101, 102, 103) into `bpoUsers` list
- `LoginFilter`: No changes needed — BPO users authenticate normally

## New Files Created

### Servlets (controller/home/)
| File | URL | Purpose |
|------|-----|---------|
| `BpoHome.java` | `/BpoHome` | Dashboard servlet — loads delegated tasks via JPQL, forwards to bpoHome25.jsp |
| `BpoCompleteTask.java` | `/BpoCompleteTask` | Handles mark-complete (POST redirect) and add-note (AJAX 200) actions |
| `BpoGetNotes.java` | `/BpoGetNotes` | AJAX endpoint — returns JSON array of notes for a given todoId |
| `BpoCompletedTasks.java` | `/BpoCompletedTasks` | AJAX endpoint — returns completed BPO tasks filtered by day range |
| `SeedBpoDemoData.java` | `/SeedBpoDemoData` | One-time demo seeder — marks existing tasks as sourced for BPO |
| `CreateBpoTestUser.java` | `/CreateBpoTestUser` | One-time test user creator (bpoadmin@test.com / bpouser@test.com) |

### JSPs
| File | Purpose |
|------|---------|
| `WEB-INF/view/bpo/bpoHome25.jsp` | BPO dashboard with two-column layout, task detail modal, completed section |

### Entities (model/)
| File | Purpose |
|------|---------|
| `model/activity/checklist/tasks/ToDoNote.java` | JPA entity for task-level notes |

## Modified Files

### Role Gates & Navigation
| File | Change |
|------|--------|
| `activityDetail25.jsp` | Added BPO roles to role gate; conditional center panel hide for CheckList type; `checklist-mode` body class for CSS |
| `navbar25.jsp` | BPO role-aware navigation (BpoHome link instead of ViewHome25) |
| `CloseActivity25.java` | BPO redirect in `goToPage()` |
| `MakeRecurringFromChecklist25.java` | BPO redirect in `goToPage()` |
| `ModifyRecurringTask25.java` | BPO redirect in `goToPage()` + hidden inputs for startDate/sequenceName |
| `UpdateTask25.java` | BPO redirect in `goToPage()` |
| `ChecklistAction25.java` | Conditional BPO redirect (only when path=ViewHome25, allows ViewChecklist25 forward) |
| `CreateReminder25.java` | BPO redirect in `goToPage()` |
| `CreateChecklist25.java` | BPO redirect in `goToPage()` |

### Dropdowns & UI Components
| File | Change |
|------|--------|
| `ddUserList25.jsp` | Role-aware: shows `global.getBpoUsers()` for BPO, `global.getUsers()` for PSP |
| `modifyRecurring25.jsp` | Role-aware user forEach + SSA restyle with ghost buttons + method name fix (getTaskFrequencies/getTaskFrequency) |
| `MakeRecurringForm25.jsp` | SSA restyle with labels, two-column layout, btn-ssa submit |
| `makeRecurringModal25.jsp` | SSA branded modal header |
| `taskManager25.jsp` | BPO: employee dropdown uses BPO users, vendor sourcing hidden, automation column hidden, left column full-width, cancel routes to BpoHome |
| `AmsDataGlobal.java` | BPO users loaded from all three roles (101+102+103) with dedup and sort |

## BPO Dashboard Features

### Left Column: My Checklists
- Reuses existing `toDoCurrentList25.jsp` — BPO users can create personal checklists and reminders
- SSA-alt (green) header branding to distinguish from PSP blue

### Right Column: Delegated Tasks
- Table layout with sortable columns (Task Name, PSP, Due Date, Status)
- Due date color coding (red=overdue, orange=today, gray=future)
- Status badges (Unassigned=yellow, Assigned=green)
- My Tasks / All Open toggle via GET param
- Activity type display in subtitle (Renewal, Setup, Ticket, CheckList)

### Task Detail Modal
- PSP name, activity name, due date display
- GoTo link (opens task website) and Info link (instructions) — conditional display
- Notes section with AJAX loading and inline add (source type badges: BPO=blue, PSP=yellow)
- Mark Complete button (POST redirect, auto-adds completion note)

### Completed Tasks Section
- Collapsed by default at bottom of delegated tasks card
- Quick filter buttons: 1 Day / 3 Day / 1 Week
- AJAX-loaded list showing task name (strikethrough), activity + PSP, completed by

## Checklist Detail Enhancements (All Roles)

### Center Column Hidden for CheckList Type
- CheckList activities have no type-specific content in center panel
- Conditional rendering skips center panel + right divider
- CSS override expands right panel: `body.checklist-mode #panelRight { flex: 1 1 auto }`
- Drag script null guard for missing divider

## Pending BPO Items

1. **Task Assignment (D-29):** Assign unassigned tasks to BPO users from modal — designed but not implemented
2. **Automation Contacts (D-30):** Checklists lack primary contacts for email automation — hidden for BPO, needs design for contact-less automation
3. **Demo Data (D-24):** SeedBpoDemoData exists but needs purpose-built data with clear names for live demos
4. **BPO Initialization (Step 7):** Registration flow for PSPs to connect a BPO vendor
5. **Vendor Management (D-26):** Admin page for managing BPO contacts
6. **Agent/Agency Expansion:** User dropdowns designed to eventually support agency-scoped filtering (deferred)
