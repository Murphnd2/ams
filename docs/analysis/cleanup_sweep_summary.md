# Cleanup Sweep Summary — All Sessions

**Branch:** `refactor/modernize-architecture`  
**Build Verified:** ✅ after every batch of deletions  
**Last Updated:** February 17, 2026

---

## Grand Total: 226 Files Deleted

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

## Session 4: Controller Sweep Completion (86 files)

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
| Root-level (`CreateTpa`, `UploadFileServlet`, `ViewFileUpload`) | 3 |
| `psp/admin/` + `agency/` + `agency/helper/` + `rates/` | 35 |
| `previous/filter/CsrfFilter.java` | 1 |

---

## Cumulative Progress

| Session | Files Deleted |
|---------|--------------|
| GoAdminHome ecosystem | 14 |
| Email workflow cleanup | 19 |
| Major sweep | 107 |
| Controller sweep completion | 86 |
| **Grand Total** | **226 files** |

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
| ViewSelectedActivity.java | Static utility methods used by active callers (servlet doGet/doPost path is dead — forwards to deleted GoAdminHome) |
| ViewSelectedChecklist.java | 27 usages — core utility class (`getToDoListByChecklistId`) |

### Notes on Active Files
- Most are kept for **static utility methods only**, not as servlets
- `ViewSelectedActivity` is a candidate for future refactor: extract static methods into a utility class
- `ShowFileUpload` may overlap with `ShowUploadPage` — revisit later
- All subpackages under `previous/controller/` have been flattened — these 10 files now sit directly in `previous/controller/`

---

## Packages Fully Eliminated

- `previous/archive/` — entire package deleted
- `previous/controller/xtra/` — entire package deleted
- `previous/controller/general/admin/q/` — all but StdAuto.java deleted
- `previous/controller/general/admin/` — all but ViewEmail, ViewSelectedActivity, ViewSelectedChecklist deleted
- `previous/controller/activity/checklist/sequence/` — all but AddRecurringSequence deleted
- `previous/controller/activity/checklist/task/` — all but AddFileToTask deleted
- `previous/controller/activity/renewal/` — entire package deleted
- `previous/controller/activity/setup/` — all but GenerateProp deleted
- `previous/controller/activity/ticket/` — all but CreateTicket deleted
- `previous/controller/billing/` — entire package deleted
- `previous/controller/checklist/` — entire package deleted
- `previous/controller/psp/admin/` — entire tree deleted (root + agency + agency/helper + rates)
- `previous/controller/summit/` — entire package deleted
- `previous/filter/` — CsrfFilter deleted (package now empty)

---

## What Remains (Not Dead Code — Needs Refactoring)

### `previous/data/` (~30 DAO files)
- Heavily referenced by both modern and legacy code
- Refactor candidate: consolidate, reorganize, reduce file count
- Not a deletion target — these are active

### `previous/model/` (~70 JPA entity files)
- Active JPA entities used throughout the application
- Refactor candidate: review for unused entities after controller cleanup
- Not a deletion target — these are active

---

## Security Items

| Item | Status |
|------|--------|
| CsrfFilter not registered | DELETED — was never wired up, dead code |
| AuthenticateUser failure handling | Still needs `displayLoginFailure()` implementation |
| InitializeDataBase accessibility | DELETED — servlet removed |
| LoginFilter stale URL whitelist entries | `/EmployerBillingDetail` and `/InitializeDataBase` entries are now dead strings — harmless, clean up when convenient |

---

## Dead String References (Harmless, Clean Up When Convenient)

These are `getServletContext().getNamedDispatcher()` or URL whitelist strings that reference deleted servlets. They cause no compile or runtime errors — the forward paths are simply never reached.

- `LoginFilter` line 21: references `/InitializeDataBase` and `/EmployerBillingDetail`
- `AddRecurringSequence.goToPage()`: references `RecurringSequenceBuilder` (deleted)
- `CreateTicket @WebServlet annotation`: string `CreateTicket2` (deleted)
- `AddFileToTask.goToPage()`: references `TaskDetailView` (deleted)
- `ViewSelectedActivity.goToPage()`: references `GoAdminHome` (deleted)
