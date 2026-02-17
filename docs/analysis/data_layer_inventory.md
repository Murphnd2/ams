# previous/data/ — Inventory & Refactor Plan

**Branch:** `refactor/modernize-architecture`  
**Last Updated:** February 17, 2026  
**Status:** Inventory COMPLETE — all files verified, ready for rename/refactor phase

---

## Summary

37 active files across 6 subpackages + root. All are `abstract` utility classes with static methods. Most have cryptic 1-4 character names from original development.

**6 dead files already deleted:** ReadInboundEmailService, dbBilling, dbEe, dbNote, dS1, summit/bill

**Goal:** Rename to descriptive names, reorganize into logical groups, consolidate duplicates.

---

## File Inventory

### Root Level (9 files)

| File | Usages | Purpose | Proposed Name |
|------|--------|---------|---------------|
| `V.java` | 92 | Validation & normalization (email, URL, phone, names, employer names) | `Validator.java` |
| `Q.java` | 50 | Constants (doc paths, form URLs, thresholds) + email templates + renewal business logic | **SPLIT** (see below) |
| `eV.java` | 59 | Person/Employee resolution from text input (ID, email, name) | `PersonResolver.java` — consolidate with XP |
| `XP.java` | 27 | Person resolution from text (employee codes, email, names) | Consolidate into `PersonResolver.java` |
| `SessionVar.java` | Many | Session state POJO (current activity, person, user, todos, filters) + activity list filtering | `SessionState.java` or keep |
| `QueryPair.java` | Several | Simple key-value pair for named queries (used by dGen) | Keep or inline |
| `Starter.java` | Several | Database initialization logic | `DatabaseInitializer.java` |
| `StarterData.java` | Several | Reference data seeding (statuses, categories, frequencies, LOS, modules) | `ReferenceDataSeeder.java` |
| `Summit.java` | Several | Summit platform integration (employer/employee data sync) | `SummitSync.java` |

### activity/ (3 files)

| File | Usages | Purpose | Proposed Name |
|------|--------|---------|---------------|
| `vA.java` | Several | Activity view session setup (set session attrs for activity detail page) | `ActivityViewHelper.java` |
| `dActivity.java` | Several | Activity DB queries (employee lists, primary contact, person creation from employee) | `ActivityDAO.java` |
| `aList.java` | Several | Activity listing, filtering, sorting (by owner, type, priority, date) + checklist listing | `ActivityListDAO.java` |

### checklist/ (4 files)

| File | Usages | Purpose | Proposed Name |
|------|--------|---------|---------------|
| `dbCheck.java` | Several | Checklist DB queries (used by AmsDataGlobal) | `ChecklistDAO.java` |
| `dbRec.java` | Several | Recurring checklist queries (used by AmsDataGlobal, AmsDataLocal) | `RecurringChecklistDAO.java` |
| `dbReq.java` | Several | Required task list queries (used by dC for building task lists) | `RequiredTaskDAO.java` |
| `dTask.java` | Several | Task-related queries | `TaskDAO.java` |

### misc/ (11 files — was 15, 4 deleted)

| File | Usages | Purpose | Proposed Name |
|------|--------|---------|---------------|
| `dbA.java` | Many | Constant value lookups from DB (save path, web path, false close date) | `AppConstantDAO.java` |
| `dbAuth.java` | Several | Authentication queries (user/password lookups, used by StarterData) | `AuthDAO.java` |
| `dbEmail.java` | Many | Email queries + email validation (`isValidEmail`, person-by-email lookups) | `EmailDAO.java` |
| `dbRenew.java` | Many | Renewal queries (employees assigned, contacts not assigned, benefits not in renewal) | `RenewalQueryDAO.java` |
| `dbS.java` | Several | Application module & required task list queries (used by GenerateProp25) | `ApplicationTaskDAO.java` |
| `dbTicket.java` | Many | Ticket queries (employee list formatting) | `TicketQueryDAO.java` |
| `dbTime.java` | Several | Time tracking queries (punch history, used by TimeClock25, SessionVar) | `TimeTrackingDAO.java` |
| `ddC.java` | Several | Task sequence/template queries + DoW creation (used by AmsDataGlobal, StarterData) | `SequenceDAO.java` |
| `dG.java` | 69 | Sales/proposal utilities — Agency, LOS, ServiceModule lookups, JotForm params, pricing | `SalesDAO.java` |
| `dGen.java` | Several | Generic named query executor (creates own EMF — legacy pattern, used by dM) | `LegacyQueryRunner.java` |
| `dP.java` | 29 | Person/Employee resolution + duplicate person merging (used by AmsDataLocal, StdAuto) | `PersonDAO.java` |
| `dPSP.java` | 5 | PSP staff/agent/admin lists + employer list (used by SessionVar) | `PspDAO.java` |

### model/ (2 files + 2 subpackages with 1 file each = 4 files)

| File | Usages | Purpose | Proposed Name |
|------|--------|---------|---------------|
| `dH.java` | Several | HTML anchor tag wrapper utility (`wrapInAnchorTag`) | `HtmlHelper.java` |
| `dL.java` | Several | Link/document constants (video URLs, SharePoint guides, enrollment packets) + insertable link list | `DocumentConstants.java` |
| `creates/dC.java` | Several | Entity creation factory (checklists, reminders, tasks, ToDos, weblinks) | `EntityFactory.java` |
| `getByIds/dM.java` | Very high | Entity lookups by ID (~40+ getXxxById methods — the core repository) | `EntityLookup.java` |

### renewal/ (1 file)

| File | Usages | Purpose | Proposed Name |
|------|--------|---------|---------------|
| `dR.java` | 48 | Renewal CRUD — create renewals, add/remove benefits, manage checklists, close ToDos | `RenewalService.java` |

### summit/ (1 file — was 2, bill.java deleted)

| File | Usages | Purpose | Proposed Name |
|------|--------|---------|---------------|
| `dH.java` | 7 | HSA billing grid population + HSA employer lookups (used by Updater) | `HsaBillingHelper.java` |

---

## Proposed Q.java Split

`Q.java` is doing too many things. Suggested split:

| Content | Move To |
|---------|--------|
| Document path constants (`DOC_PATH`, `DOC_SRA`, etc.) | Merge into `DocumentConstants.java` (model/dL.java renamed) |
| Jotform URLs (`FORM_COBRA_TAKEOVER`, etc.) | Merge into `DocumentConstants.java` or separate `FormLinks.java` |
| Compensation thresholds (`KEYMAN_OFFICER_COMP`, etc.) | `ComplianceConstants.java` |
| Summit transition HTML text | `EmailTemplates.java` |
| Welcome/renewal email body text | `EmailTemplates.java` |
| Renewal business logic (`addNextTask`, `followUp`, `isPop`, `hasFsa`) | `RenewalEmailService.java` |
| WebLink/form insert helpers | `LinkHelper.java` |

---

## Consolidation Candidates

### eV.java + XP.java → `PersonResolver.java`
Both resolve text input to a Person entity. Different approaches but same goal. Consolidate into one class with a clean public API.

### vA.java + ViewSelectedActivity static methods
Both set session attributes for activity detail views. Consolidate into one `ActivityViewHelper`.

### Q.java constants + dL.java constants
Both hold document/link constants. Merge into `DocumentConstants.java`.

### model/dH.java (HtmlHelper) — possible merge into Validator or standalone
Tiny utility class, could stay standalone or merge.

---

## Rename Priority Order

**Phase 1: Simple renames (no logic changes, just Shift+F6 in IntelliJ)**
1. `V.java` → `Validator.java` — 92 usages, pure utility, cleanest rename
2. `dM.java` → `EntityLookup.java` — core repository
3. `dC.java` → `EntityFactory.java` — creation methods
4. `dR.java` → `RenewalService.java` — renewal CRUD
5. `dbA.java` → `AppConstantDAO.java` — config lookups
6. `dG.java` → `SalesDAO.java` — sales/proposal
7. `dP.java` → `PersonDAO.java` — person resolution
8. `dbTime.java` → `TimeTrackingDAO.java`
9. `dbRenew.java` → `RenewalQueryDAO.java`
10. `dbTicket.java` → `TicketQueryDAO.java`
11. `dbEmail.java` → `EmailDAO.java`
12. `dbCheck.java` → `ChecklistDAO.java`
13. `dbRec.java` → `RecurringChecklistDAO.java`
14. `dbReq.java` → `RequiredTaskDAO.java`
15. `dTask.java` → `TaskDAO.java`
16. `dbS.java` → `ApplicationTaskDAO.java`
17. `dbAuth.java` → `AuthDAO.java`
18. `ddC.java` → `SequenceDAO.java`
19. `dGen.java` → `LegacyQueryRunner.java`
20. `dPSP.java` → `PspDAO.java`
21. `dActivity.java` → `ActivityDAO.java`
22. `aList.java` → `ActivityListDAO.java`
23. `vA.java` → `ActivityViewHelper.java`
24. `eV.java` → `PersonResolver.java`
25. `model/dH.java` → `HtmlHelper.java`
26. `model/dL.java` → `DocumentConstants.java`
27. `summit/dH.java` → `HsaBillingHelper.java`
28. `Summit.java` → `SummitSync.java`
29. `Starter.java` → `DatabaseInitializer.java`
30. `StarterData.java` → `ReferenceDataSeeder.java`

**Phase 2: Consolidations (logic merges)**
- eV + XP → single PersonResolver
- Q.java split into focused classes
- vA + ViewSelectedActivity static methods → ActivityViewHelper
- dL + Q constants → DocumentConstants

**Phase 3: Package reorganization**
- Move renamed files out of `previous/data/` into logical `data/` structure
- Eliminate `previous/data/` package entirely

**Build and verify after every rename.**
