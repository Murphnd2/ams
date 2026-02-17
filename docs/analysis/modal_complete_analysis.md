# Complete Modal Analysis - Dashboard & Activity Detail

**Last Updated:** Session - adminMenuOC.jsp + remaining modal verification
**Purpose:** Map ALL modal servlets to identify what's modern vs legacy

---

## 🎯 COMPLETE MODAL SERVLET INVENTORY

### **Dashboard Modals (from navbar25.jsp)**

| # | Modal | Servlet | Package | Status |
|---|-------|---------|---------|--------|
| 1 | createUserModal25.jsp | CreatePspUser25 | controller | ✅ MODERN |
| 2 | updatePspMod25.jsp | UpdatePsp25 | controller | ✅ MODERN |
| 3 | createBlankRenewalMod25.jsp | CreateBlankRenewal25 | controller | ✅ MODERN |
| 4 | generateSetupMod25.jsp | GenerateProp25 | controller | ✅ MODERN |
| 5 | addReminder25.jsp | CreateReminder25 | controller | ✅ MODERN |
| 6 | addChecklist25.jsp | CreateChecklist25 | controller | ✅ MODERN |
| 7 | createTicket25.jsp | CreateTicket25 | controller | ✅ MODERN |
| 8 | loginFormModal.jsp | AuthenticateUser | controller.authentication | ✅ MODERN (migrated) |
| 9 | upcomingRenewalsModal25.jsp | EmployerRenewalDetailView | controller.activity.renewal | ✅ MODERN (migrated) |
| 10 | adminMenuOC.jsp | (see admin menu breakdown below) | mixed | ⚠️ MIXED |

**Dashboard Total:** 8 direct modal servlets
- ✅ Modern: 8 — ALL MODERN

> **NOTE:** `upcomingRenewalsModal25.jsp` was previously marked "display only" — INCORRECT.
> It imports `renewalList25.jsp` which submits to `EmployerRenewalDetailView`.
> **Migrated** from `previous.controller.activity.renewal` → `controller.activity.renewal` ✅

---

### **Activity Detail Modals (from activityDetail25.jsp)**

| # | Modal | Servlet | Package | Status |
|---|-------|---------|---------|--------|
| 11 | addToDo25.jsp | AddToDo25 | controller.checklist | ✅ MODERN |
| 12 | closeActivityModal.jsp | CloseActivity25 | controller.activity | ✅ MODERN |
| 13 | modContact25.jsp | ModifyContact25 | controller.activity.contact | ✅ MODERN |
| 14 | addContactToActivityMod.jsp | AddActivityContact25 | controller.activity.contact | ✅ MODERN |
| 15 | ownerModal25.jsp | ChangeOwner25, ChangeDueDate25 | controller.activity | ✅ MODERN |
| 16 | pastActivityModal25.jsp | ViewPastActivity25 | controller.activity | ✅ MODERN |
| 17 | addSetupItemMod.jsp | AddSetupModule25 | controller.activity.setup | ✅ MODERN |
| 18 | addRenewalItemMod25.jsp | AssignBenefitToRenewal25 | controller.activity.renewal | ✅ MODERN |
| 19 | makeRecurringModal25.jsp | MakeRecurringFromChecklist25 | controller.checklist | ✅ MODERN |
| 20 | webLinkListModal25.jsp | (hub — launches #21 and #22 modals) | N/A | ✅ DISPLAY HUB |
| 21 | addDocumentToActivityMod.jsp | AddDocumentToActivity | controller.activity | ✅ MODERN (was already migrated) |
| 22 | addUrlToActivityMod.jsp | AddUrlToActivity | controller.activity | ✅ MODERN (was already migrated) |
| 23 | otherContact25.jsp | RemoveContact25, AddActivityContact25 | controller.activity.contact | ✅ MODERN CONFIRMED |

> **CONFIRMED from prior analysis:** `otherContact25.jsp` → `contactManager25.jsp` →
> `RemoveContact25` at `controller.activity.contact` ✅. [add] button chains to
> `addContactToActivity25.jsp` → `AddActivityContact25` ✅. Both fully modern.

**Activity Detail Total:** 11 modal servlets
- ✅ Modern: 11 — ALL MODERN

---

### **Admin Offcanvas Menu (adminMenuOC.jsp) — Full Breakdown**

| Button | Action Type | Servlet | Package | Status |
|--------|-------------|---------|---------|--------|
| Create A Setup | Modal → #createSetupForm | GenerateProp25 | controller.activity.setup | ✅ MODERN |
| Create Empty Renewal | Modal → #createBlankRenewal | CreateBlankRenewal25 | controller.activity.renewal | ✅ MODERN |
| Upcoming Renewals | Modal → #addRenewalModal | EmployerRenewalDetailView | controller.activity.renewal | ✅ MODERN (migrated) |
| Manage Task Templates | Direct href | GoTicketTemplate25 | controller.activity.ticket | ✅ MODERN |
| Create User | Modal → #createUserModal | CreatePspUser25 | controller.user | ✅ MODERN |
| View Billing Page | Direct href | ResetBillingView | controller | ✅ MODERN |
| Refresh ToDo Automation *(superUser only)* | Direct href | RefreshToDoAutomation | previous.controller.general.admin.q | ⚠️ LEGACY |
| Clear Import Tables *(superUser only)* | Direct href | ClearImport (class: ClearImportTables) | previous.controller.summit | ⚠️ LEGACY — **empty body** |
| Clear Monthly Billing *(superUser only)* | Direct href | ClearMonthlyBilling | previous.controller.summit.twtw | ⚠️ LEGACY (modern: ClearBilling25 exists) |
| Update Tables *(superUser only)* | Direct href | UpdateTables | previous.controller.summit.twtw | ⚠️ LEGACY — **empty body** |
| Create Monthly Billing *(superUser only)* | Direct href | CreateMonthlyBilling | previous.controller.summit.twtw | ⚠️ LEGACY |
| ~~Create Insert Link~~ | ~~Modal~~ | ~~AddInsertLink25~~ | — | 🚫 COMMENTED OUT |

**Notes on monthly process servlets:**
- `ClearImport` (`ClearImportTables.java`) — `showSummitStuff()` body is empty; forwards to `/index.jsp`
- `UpdateTables` — `createHsaEmployers()` body is empty; forwards to `/index.jsp`
- `ClearMonthlyBilling` — functional; modern replacement `ClearBilling25` exists at `controller.monthly`
- All monthly process servlets forward to `/index.jsp` (bypasses normal app flow)
- These 5 are seeded as automation task names in `Starter.java` (task IDs 10–14)

---

## 📊 UPDATED SUMMARY STATISTICS

### All Modal Servlets (unique, excluding display-only containers)

| Servlet | Package | Status |
|---------|---------|--------|
| CreatePspUser25 | controller.user | ✅ |
| UpdatePsp25 | controller.user | ✅ |
| CreateBlankRenewal25 | controller.activity.renewal | ✅ |
| GenerateProp25 | controller.activity.setup | ✅ |
| CreateReminder25 | controller.checklist | ✅ |
| CreateChecklist25 | controller.checklist | ✅ |
| CreateTicket25 | controller.activity.ticket | ✅ |
| AuthenticateUser | controller.authentication | ✅ |
| GoTicketTemplate25 | controller.activity.ticket | ✅ |
| ResetBillingView | controller | ✅ |
| AddToDo25 | controller.checklist | ✅ |
| CloseActivity25 | controller.activity | ✅ |
| ModifyContact25 | controller.activity.contact | ✅ |
| AddActivityContact25 | controller.activity.contact | ✅ |
| ChangeOwner25 | controller.activity | ✅ |
| ChangeDueDate25 | controller.activity | ✅ |
| ViewPastActivity25 | controller.activity | ✅ |
| AddSetupModule25 | controller.activity.setup | ✅ |
| AssignBenefitToRenewal25 | controller.activity.renewal | ✅ |
| MakeRecurringFromChecklist25 | controller.checklist | ✅ |
| RemoveContact25 | controller.activity.contact | ✅ |
| EmployerRenewalDetailView | controller.activity.renewal | ✅ (migrated) |
| AddDocumentToActivity | controller.activity | ✅ (was already migrated) |
| AddUrlToActivity | controller.activity | ✅ (was already migrated) |
| RefreshToDoAutomation | previous.controller.general.admin.q | ⚠️ |
| ClearImport | previous.controller.summit | ⚠️ stub |
| ClearMonthlyBilling | previous.controller.summit.twtw | ⚠️ |
| UpdateTables | previous.controller.summit.twtw | ⚠️ stub |
| CreateMonthlyBilling | previous.controller.summit.twtw | ⚠️ |

**Total unique modal servlets: 29**
- ✅ Modern (controller): 24 (83%)
- ⚠️ Legacy (previous.controller): 5 (17%)

---

## 🎯 LEGACY SERVLETS TO MIGRATE (by priority)

### ✅ Priority 1 — COMPLETE
1. ~~**AddDocumentToActivity**~~ — already at `controller.activity` ✅
2. ~~**AddUrlToActivity**~~ — already at `controller.activity` ✅
3. ~~**EmployerRenewalDetailView**~~ — migrated to `controller.activity.renewal` ✅

### Priority 2 — Admin (infrequent, superUser only)
4. **RefreshToDoAutomation** — `previous.controller.general.admin.q` → `controller.admin`
5. **CreateMonthlyBilling** — `previous.controller.summit.twtw` → `controller.monthly`
6. **ClearMonthlyBilling** — replace with existing `ClearBilling25` at `controller.monthly`

### Priority 3 — Stubs (empty bodies, low urgency)
7. **ClearImport** (`ClearImportTables`) — empty; migrate or delete
8. **UpdateTables** — empty; migrate or delete

---

## 📈 PROGRESS TRACKER

| Area | Total Servlets | Modern | Legacy | % Modern |
|------|---------------|--------|--------|----------|
| Dashboard modals | 8 | 8 | 0 | 100% ✅ |
| Activity detail modals | 11 | 11 | 0 | 100% ✅ |
| Admin menu (direct) | 10 | 7 | 3 | 70% |
| **All modal servlets** | **29** | **24** | **5** | **83%** |

---

## ✅ WHAT'S CONFIRMED THIS SESSION

- `otherContact25.jsp` → `RemoveContact25` ✅ MODERN CONFIRMED (was "likely")
- `webLinkListModal25.jsp` → display hub only ✅ CONFIRMED (no own servlet)
- `upcomingRenewalsModal25.jsp` → NOT display-only ⚠️ CORRECTED — calls `EmployerRenewalDetailView`
- `adminMenuOC.jsp` → fully mapped, 5 superUser-only monthly process servlets identified
- `GoTicketTemplate25` ✅ MODERN CONFIRMED at `controller.activity.ticket`
- Duplicate noted: `GoTicketTemplate` (legacy) still exists at `previous.controller.general.admin.q`
- `ClearBilling25` modern replacement already exists at `controller.monthly` for `ClearMonthlyBilling`

---

**Analysis Status:** ✅ COMPLETE — All modals on pspHome25.jsp and activityDetail25.jsp fully mapped
**Priority 1 Migration:** ✅ COMPLETE — EmployerRenewalDetailView migrated, AddDocumentToActivity + AddUrlToActivity confirmed already modern
**Remaining legacy (admin-only):** 5 servlets in Priority 2 & 3
