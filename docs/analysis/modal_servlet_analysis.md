# Modal Servlet Analysis - Complete Inventory

**Purpose:** Map all modals from dashboard and activity detail pages to identify their servlets
**Status:** Complete analysis of user-facing actions

---

## DASHBOARD MODALS (pspHome25.jsp via navbar25.jsp)

### ✅ Already Modern (5 modals)
1. **addReminder25.jsp** → CreateReminder25 ✅
2. **addChecklist25.jsp** → CreateChecklist25 ✅  
3. **createTicket25.jsp** → CreateTicket25 ✅
4. **loginFormModal.jsp** → AuthenticateUser ✅ (migrated)
5. **adminMenuOC.jsp** → (offcanvas menu, no servlet)

### 🔍 Need to Check (6 modals)
6. **createUserModal25.jsp** → CreateUser25 (likely)
7. **updatePspMod25.jsp** → UpdatePsp25 (likely)
8. **createBlankRenewalMod25.jsp** → CreateBlankRenewal25 (likely)
9. **addInsertLinkModal25.jsp** → AddInsertLink25 (likely)
10. **generateSetupMod25.jsp** → GenerateSetup25 (likely)
11. **upcomingRenewalsModal25.jsp** → (display only, likely)
12. **makeRecurringModal25.jsp** → MakeRecurringFromChecklist25 ✅ (already modern)

---

## ACTIVITY DETAIL MODALS (activityDetail25.jsp)

### ✅ Already Modern (9 modals)
1. **addToDo25.jsp** → AddToDo25 ✅
2. **closeActivityModal.jsp** → CloseActivity25 ✅
3. **modContact25.jsp** → ModifyContact25 ✅
4. **addContactToActivityMod.jsp** → AddActivityContact25 ✅
5. **ownerModal25.jsp** → ChangeOwner25, ChangeDueDate25 ✅
6. **pastActivityModal25.jsp** → ViewPastActivity25 ✅
7. **addSetupItemMod.jsp** → AddSetupModule25 ✅
8. **addRenewalItemMod25.jsp** → AssignBenefitToRenewal25 ✅
9. **makeRecurringModal25.jsp** → MakeRecurringFromChecklist25 ✅

### 🔍 Need to Check (2 modals)
10. **otherContact25.jsp** → RemoveContact25 (likely ✅ modern)
11. **webLinkListModal25.jsp** → AddDocument25, AddUrl25 (likely)

---

## MODAL SERVLETS TO ANALYZE

Based on modal names, these servlets likely exist:

### **User Management:**
- CreateUser25 (or CreateUser)
- UpdatePsp25

### **Activity Creation:**
- CreateBlankRenewal25
- GenerateSetup25

### **Link/Document Management:**
- AddInsertLink25
- AddDocument25
- AddUrl25

---

## ANALYSIS STRATEGY

For each unconfirmed modal:
1. Find the modal JSP file
2. Look for <form action="ServletName">
3. Check if servlet is in controller or previous package
4. If in previous package, add to migration list
5. If doesn't exist, modal might be dead code

---

## NEXT STEPS

1. Search for CreateUser25 servlet
2. Search for UpdatePsp25 servlet
3. Search for CreateBlankRenewal25 servlet
4. Search for GenerateSetup25 servlet
5. Search for AddInsertLink25 or similar
6. Search for AddDocument25 or similar

After mapping all modals, we'll know:
- Which servlets are actively used
- Which need migration
- What's truly dead code
