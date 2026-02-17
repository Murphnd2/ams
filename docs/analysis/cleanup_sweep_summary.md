# Cleanup Sweep Summary — Full Session (February 17, 2026)

**Branch:** `refactor/modernize-architecture`  
**Build Verified:** ✅ `mvn clean package` — SUCCESS (verified after each batch)

---

## Phase 1: Archive Package Sweep (20 files)
**Entire `previous.archive` package deleted.**

| Subpackage | Files |
|------------|-------|
| Root (5) | changeDueDate, changeOwner, createTicketNew, removeContactFromActivity, sendAutomation |
| activityDetail/ (1) | goActivityDetail |
| activityDetail/actions/ (8) | addContactToActivity, addNoteToItem, addToDoToList, closeActivity, closeToDoOut, manageTask, modContactForm, reOpenToDoOut |
| checklistDetail/ (4) | createReminder, createSimpleChecklist, doCheckListAction, goCheckListDetail |
| pspHome/ (2) | createTicket3, goPspHome |

## Phase 2: Empty/Placeholder Files (4 files)
- HomeServlet.java, toDoIsCompleteMain.jsp, ttt.jsp, detailPastLabel25.jsp

## Phase 3: Duplicate Servlet Consolidation (3 files)
- TaskBuilder.java → TaskBuilder25
- ClearGrid.java → ClearGrid25
- MakeRecurringFromChecklist.java → MakeRecurringFromChecklist25

## Phase 4: Orphaned Legacy JSPs (11 files)
- Parent JSPs: activityDetail.jsp, checklistDetail.jsp (zero usages)
- Column JSPs (9): checklistAutomation, checklistBasic, checklistFooter, detailHeader, detailPrimaryContact, detailDetail, detailAddNote, detailFooter, historyDetail

## Phase 5: previous/controller/xtra/ (4 files)
- FixEmployeeList, FixRenewal, GarbageIt, RefreshPersonNames

## Phase 6: TestServlet (1 file)
- TestServlet.java

## Phase 7: general/admin Show* Filters (10 files)
- ShowAllActivities, ShowFollowUps, ShowFsaRenewals, ShowHraRenewals, ShowOnUsActivities, ShowPopRenewals, ShowRenewalActivities, ShowSetupActivities, ShowSortedByDate, ShowTicketActivities

## Phase 8: general/admin Orphaned Servlets (20 files)
- UpdateRenewalContact, ViewSelectedRenewal, ChangeActivityOwner, ChangeActivityDueDate, CreateInsertLink, CreateNewEmployer, DeleteSingleItemRenewal, PunchClock, XferDpi, RefreshTicketEmployees
- AddNoteToActivity, AddRenewalContact, AddSetupContact, AddToDoToChecklist, AssignBenefitToRenewal, CloseSingleItemChecklist, CloseToDo, ReOpenToDo, RemoveItemFromRenewal, RemoveRenewalContact, RemoveSetupContact
- Also removed dead `getFormServlet()` method from ToDo.java
- ViewEmailHistory

## Phase 9: general/admin/q/ Package (25 files)
- AddDocTask, AddHasQbTask, AddHsaQuick, AddHsaToPop, AddTaskToChecklist, CheckIfHsaIsRight, ConfirmAddHSA, CreateTicketTemplate, DelTask, ProcessAutomationContent
- Send105Reminder, Send125Initial, Send125Reminder, SendAutoEmail, SendCobraReminder, SendEligibilityTestInitial, SendFsaReminder, SendHSAWelcomeSetup, SendInvoiceFollowUp, SendKeyman, SendKeymanReminder, SendOeReminder, SendRenewalWelcome, SendRenewalWelcome2, SendTestCompleted, SendWelcomeFSA, WelcomeNewPop

## Phase 10: previous/controller/activity/ (8 files)
- DownloadActivityDoc, ModifyContactForm, RefreshCurrentActivity, RemoveContactFromOther, ViewPastActivity
- AddReminder, AddSimpleChecklist, ChangeChecklistDueDate

---

## Files Confirmed ACTIVE (Do Not Delete)

| File | Why Active |
|------|-----------|
| ViewEmail.java | Referenced by multiple JSPs (historyDetail25, emailList, etc.) |
| ViewSelectedActivity.java | 131 usages — core utility with static helpers |
| ViewSelectedChecklist.java | 27 usages — core utility (getToDoListByChecklistId) |
| StdAuto.java | Referenced by SendEmployerBillingDetail |
| AddContactToActivity.java | Static method used by billing servlets |
| SendQuote1.java | Kept — needs further text search verification |

## Session Totals

| Category | Files Deleted |
|----------|--------------|
| Archive package | 20 |
| Empty/placeholder | 4 |
| Duplicate servlets | 3 |
| Orphaned legacy JSPs | 11 |
| xtra/ package | 4 |
| TestServlet | 1 |
| Show* filters | 10 |
| general/admin orphans | 21 |
| general/admin/q/ | 25 |
| activity/ orphans | 8 |
| **This Session Total** | **107 files** |

## Cumulative Cleanup Progress (All Sessions)

| Session | Files Deleted |
|---------|--------------|
| GoAdminHome ecosystem | 14 |
| Email workflow cleanup | 19 |
| This session | 107 |
| **Grand Total** | **140 files** |

---

## What Remains in previous/controller/ (To Sweep Next)

### general/admin/ — DONE (4 active files remain)
- ViewEmail, ViewSelectedActivity, ViewSelectedChecklist, q/StdAuto

### activity/ — Partially Done
- AddContactToActivity.java (ACTIVE — billing dependency)
- activity/checklist/sequence/ (11 files — NOT YET CHECKED)
- activity/checklist/task/ (3 files — NOT YET CHECKED)
- activity/renewal/ (6 files — NOT YET CHECKED)
- activity/setup/ (4 files — NOT YET CHECKED)
- activity/ticket/ (3 files — NOT YET CHECKED)

### Other Unchecked Subpackages
- billing/ (13 files)
- checklist/ (10 files)
- psp/admin/ + psp/admin/agency/ + psp/admin/rates/ (~25 files)
- summit/ (2 files: InitializeDataBase, RefreshData)
- Root: CreateTpa, ShowFileUpload, UploadFileServlet, ViewFileUpload

### Also Unchecked
- previous/data/ (~30 files — DAOs, likely still referenced)
- previous/model/ (~70 files — JPA entities, likely still referenced)
- previous/filter/ (1 file: CsrfFilter)
