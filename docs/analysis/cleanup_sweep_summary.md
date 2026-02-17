# Cleanup Sweep Summary — All Sessions

**Branch:** `refactor/modernize-architecture`  
**Build Verified:** ✅ after every batch of deletions  
**Last Updated:** February 17, 2026

---

## Grand Total: 232 Files Deleted

---

## Session 1: GoAdminHome Ecosystem (14 files)

- GoAdminHome.java + adminHome.jsp (core hub)
- 12 supporting servlets that forwarded to GoAdminHome

## Session 2: Email Workflow Cleanup (19 files)

- GoEmailHome, EmailActions, ResetEmailView, EscapeEmail, AddEmail, emailActionsNew
- emailHome.jsp + 9 supporting JSPs
- UpdateAutomation, sendAutomationFinal, updateTaskInfo

## Session 3: Major Sweep (107 files)

| Category | Files Deleted |
|----------|--------------|
| Archive package (`previous.archive`) | 20 |
| Empty/placeholder files | 4 |
| Duplicate servlets | 3 |
| Orphaned legacy JSPs | 11 |
| `previous/controller/xtra/` | 4 |
| TestServlet | 1 |
| `general/admin/` Show* filters | 10 |
| `general/admin/` orphaned servlets | 21 |
| `general/admin/q/` package | 25 |
| `previous/controller/activity/` partial | 8 |

## Session 4: Controller Sweep Completion + Data Layer Cleanup (92 files)

| Category | Files Deleted |
|----------|--------------|
| `activity/checklist/sequence/` | 10 |
| `activity/checklist/task/` | 2 |
| `activity/renewal/` | 6 |
| `activity/setup/` | 2 |
| `activity/ticket/` | 2 |
| `billing/` | 13 |
| `checklist/` | 10 |
| `summit/` | 2 |
| Root-level controllers | 3 |
| `psp/admin/` + all subpackages | 35 |
| `previous/filter/CsrfFilter.java` | 1 |
| `previous/data/` dead files | 6 |

### Data Layer Files Deleted (6)
- `ReadInboundEmailService.java` — experimental IMAP reader, never wired up
- `dbBilling.java` — orphaned with billing controller deletion
- `dbEe.java` — zero usages
- `dbNote.java` — zero usages
- `dS1.java` — billing link queries, orphaned
- `summit/bill.java` — zero usages

---

## Cumulative Progress

| Session | Files Deleted |
|---------|--------------|
| GoAdminHome ecosystem | 14 |
| Email workflow cleanup | 19 |
| Major sweep | 107 |
| Controller sweep + data cleanup | 92 |
| **Grand Total** | **232 files** |

---

## Files Confirmed ACTIVE (Do Not Delete)

### `previous/controller/` (10 files remaining)

| File | Why Active |
|------|-----------|
| AddContactToActivity.java | Static method used by billing servlets |
| AddFileToTask.java | Static `getExtensionByStringHandling()` used by AddDocumentToActivity & AddAttachment25 |
| AddRecurringSequence.java | Static `getDaysChecked()` used by MakeRecurringFromChecklist25 |
| CreateTicket.java | Static `createToDoList()` used by ViewSelectedActivity |
| GenerateProp.java | External URL call from old website transfer process |
| ShowFileUpload.java | URL-based file serving during billing/import; also referenced by WebLink model & attachmentList2.jsp |
| StdAuto.java | Referenced by SendEmployerBillingDetail |
| ViewEmail.java | Referenced by multiple JSPs (historyDetail25, emailList, etc.) |
| ViewSelectedActivity.java | Static utility methods used by active callers (servlet doGet/doPost path is dead) |
| ViewSelectedChecklist.java | 27 usages — core utility class |

### `previous/data/` (37 files remaining — all active, need refactoring)

See `data_layer_inventory.md` for complete inventory with proposed renames.

---

## Packages Fully Eliminated

- `previous/archive/` — entire package
- `previous/controller/xtra/`
- `previous/controller/general/admin/q/` — all but StdAuto
- `previous/controller/general/admin/` — all but ViewEmail, ViewSelectedActivity, ViewSelectedChecklist
- `previous/controller/activity/checklist/sequence/` — all but AddRecurringSequence
- `previous/controller/activity/checklist/task/` — all but AddFileToTask
- `previous/controller/activity/renewal/` — entire package
- `previous/controller/activity/setup/` — all but GenerateProp
- `previous/controller/activity/ticket/` — all but CreateTicket
- `previous/controller/billing/` — entire package
- `previous/controller/checklist/` — entire package
- `previous/controller/psp/admin/` — entire tree
- `previous/controller/summit/` — entire package
- `previous/filter/` — CsrfFilter deleted

**Note:** All remaining `previous/controller/` files have been flattened — no more subpackages.

---

## Security Items

| Item | Status |
|------|--------|
| CsrfFilter not registered | DELETED — was never wired up |
| AuthenticateUser failure handling | Still needs `displayLoginFailure()` implementation |
| InitializeDataBase accessibility | DELETED — servlet removed |
| LoginFilter stale URL whitelist | `/EmployerBillingDetail` and `/InitializeDataBase` entries are dead strings — harmless |

---

## Dead String References (Harmless)

These are `getServletContext().getNamedDispatcher()` or URL whitelist strings referencing deleted servlets. No compile or runtime errors.

- `LoginFilter` line 21: `/InitializeDataBase`, `/EmployerBillingDetail`
- `AddRecurringSequence.goToPage()`: `RecurringSequenceBuilder`
- `CreateTicket @WebServlet`: `CreateTicket2`
- `AddFileToTask.goToPage()`: `TaskDetailView`
- `ViewSelectedActivity.goToPage()`: `GoAdminHome`

---

## What Remains (Next Phase: Refactor)

### `previous/data/` — 37 files, all active
- Rename cryptic names to descriptive names
- Reorganize package structure
- Consolidate duplicates (eV + XP → PersonResolver)
- Split Q.java into focused classes
- See `data_layer_inventory.md` for full plan

### `previous/model/` — ~70 JPA entity files
- Review after data layer refactor
- Likely mostly active
