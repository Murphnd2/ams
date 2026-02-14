# activityDetail25.jsp - Complete Dependency Map

**File:** `/WEB-INF/view/a/activityDetail/activityDetail25.jsp`
**Servlet Entry Point:** ViewActivity25 (`/ViewActivity25`)
**Status:** ✅ ACTIVE - Activity detail page
**Analysis Date:** In Progress

---

## ENTRY POINTS (How Users Get Here)

### Primary Servlet Chain
**User clicks activity → GoActivityDetail25 → ViewActivity25 → activityDetail25.jsp**

1. **GoActivityDetail25** (`/GoActivityDetail25`)
   - Called from: activityList25.jsp (pspHome25.jsp)
   - Loads activity data into session
   - Forwards to: ViewActivity25 (named dispatcher)

2. **ViewActivity25** (`/ViewActivity25`)
   - Package: `controller`
   - Refreshes global delegation data if dirty
   - Forwards to: `/WEB-INF/view/a/activityDetail/activityDetail25.jsp`

### Other Entry Points
- **ViewChecklist25** (`/ViewChecklist25`) - Direct to same JSP
- **ViewById** (`/ViewById`) - Takes ID param, forwards to GoActivityDetail25

### Servlets That Forward TO ViewActivity25 (Named Dispatcher)

Based on servlet_inventory.md analysis:
1. AddNoteToActivity25 - After adding note
2. AddActivityContact25 - After adding contact
3. AddToDo25 - After adding todo
4. ViewPastActivity25 - View historical activity
5. ReturnFromPastActivity25 - Return from historical view
6. ModifyContact25 - After modifying contact
7. RemoveItemFromRenewal25 - After removing renewal item
8. ReOpenToDo25 - After reopening todo
9. SendAutoFinal25 - After automation email
10. (Many more TBD - need to analyze child components)

---

## PAGE STRUCTURE (3-Column Layout)

### Column 1: CHECKLIST Column (Left, order-2 on mobile)
**Header:**
- checklistHeader.jsp

**Conditional Components (if not complete):**
- checklistAutomation25.jsp (if not complete)

**Main Content:**
- checklistBasic25.jsp

**Footer (if not complete):**
- checklistFooter25.jsp

**Special (if CheckList type):**
- modifyRecurring25.jsp

### Column 2: DETAIL Column (Center, order-first on mobile)
**Header:**
- detailHeader25.jsp

**Primary Contact (if NOT CheckList):**
- detailPrimaryContact25.jsp

**Detail Content (Activity Type Specific):**
- detailDetail25.jsp
  - **Setup:** detailSetup25.jsp
  - **Renewal:** detailRenewal25.jsp
  - **CheckList:** (nothing)
  - **Ticket (default):** detailTicket25.jsp

**Add Note (if not complete):**
- detailAddNote25.jsp

**Footer:**
- detailFooter25.jsp

### Column 3: HISTORY Column (Right, order-last)
**Header:**
- historyHeader.jsp

**History Detail:**
- historyDetail25.jsp

---

## SHARED COMPONENTS

### Navigation
- `/WEB-INF/view/a/general/navbar25.jsp` (same as pspHome25)

### CSS/JS
- `/WEB-INF/view/css-js.jsp`
- CKEditor 36.0.1 (rich text editor)

### Auto-Save JavaScript
- **PersistChecklist25** - Auto-saves on page unload
- Tracks pending close IDs
- Uses sendBeacon API for reliable save

---

## SESSION DEPENDENCIES

### Required Session Objects
1. **`sessionScope.local`** (AmsDataLocal)
   - getCurrentActivity() → CurrentActivity object
     - getActivity() → Activity (Renewal, Setup, Ticket, or CheckList)
     - getNotes() → List<Note>
     - getToDoList() → List<ToDoOut25>
     - getPastActivities() → List<Activity>
     - getEmployees() → List<Employee>
     - getPrimaryContact() → Person
     - getAdditionalContacts() → List<Person>

2. **`sessionScope.psp`**
   - getFullName() → Page title

3. **`sessionScope.isPspUser`** - Access control
4. **`sessionScope.isPspAdmin`** - Access control

5. **Auto-save tracking:**
   - `sessionScope.local.pendingCloseIds` - Tracks unsaved checklist closes

---

## ACTIVITY TYPES & CONDITIONAL RENDERING

### Activity Type Detection
Uses: `sessionScope.local.getCurrentActivity().getActivity().getClass().getSimpleName()`

**Supported Types:**
1. **Setup** - New client setup activities
2. **Renewal** - Client renewal activities
3. **Ticket** - Support tickets
4. **CheckList** - Standalone checklists

**Type-Specific Rendering:**
- **Setup:** Shows service module selection (FSA, HRA, HSA, COBRA, etc.)
- **Renewal:** Shows renewal items and benefits
- **Ticket:** Shows ticket details
- **CheckList:** Hides primary contact, shows recurring modification

---

## IDENTIFIED SERVLETS (To Be Mapped)

### Already Documented (from servlet_inventory)
- AddNoteToActivity25
- AddActivityContact25
- AddToDo25
- CloseActivity25
- ViewPastActivity25
- ReturnFromPastActivity25
- ModifyContact25
- RemoveItemFromRenewal25
- ReOpenToDo25 ✅
- **CloseToDo25** ✅ (found in checklistBasic25)
- **ManageTask25** ✅ (found in checklistBasic25)
- **PersistChecklist25** ✅ (found in checklistBasic25 auto-save)

### To Be Found (from child JSPs)
- [ ] Servlets in checklistHeader.jsp
- [ ] Servlets in checklistBasic25.jsp
- [ ] Servlets in checklistFooter25.jsp
- [ ] Servlets in detailHeader25.jsp
- [ ] Servlets in detailAddNote25.jsp
- [ ] Servlets in detailFooter25.jsp
- [ ] Servlets in historyDetail25.jsp
- [ ] Type-specific servlets (Setup, Renewal, Ticket)

---

## CHILD JSP FILES TO ANALYZE

### Priority 1 (Checklist Column)
- [ ] checklistHeader.jsp
- [x] checklistBasic25.jsp ✅ ANALYZED
  - **Primary TODO Action Servlet:** Dynamic based on todo state
  - **Form Action:** `${formServlet}` (determined by todo status)
  - **Button Parameter:** `btnToDo` with value = toDoId
  - **Servlets Used:**
    - **CloseToDo25** - Default action (complete todo)
    - **ReOpenToDo25** - If todo is already complete
  - **Task Management:**
    - **ManageTask25** - Edit/configure todo task (tools icon)
    - Form: `<form action="ManageTask25">` with `toDoId` parameter
  - **Auto-Save Form:**
    - **PersistChecklist25** - Auto-saves on page unload
    - Hidden form triggers on beforeunload/visibilitychange
  - **Session Data:**
    - `sessionScope.local.getCurrentActivity().getToDoList()` → List<ToDoOut25>
    - Admin override: `sessionScope.isPspAdmin` bypasses blocks
  - **Todo State Logic:**
    - Complete/incomplete status
    - Time blocked (can't do yet)
    - Person blocked (wrong assignee)
    - Delegated status
    - Early/future allowed flags
  - **Visual Features:**
    - GoTo links (external task links)
    - Info links (help documentation)
    - Icons change based on todo state (square, x-square, clock, person, etc.)
- [x] checklistFooter25.jsp ✅ ANALYZED
  - **Form Action:** CloseActivity25
  - **Buttons:**
    - **Add Task** → Triggers `#addToDoModal` (Bootstrap modal)
    - **Close Activity** → Triggers `#closeActivity` modal, submits to CloseActivity25
  - **Activity Type-Specific Close Buttons:**
    - Setup → "Close Setup" (secondary color)
    - Renewal → "Close Renewal" (primary color)
    - Ticket → "Close Ticket" (info color)
    - CheckList → "Close Checklist" (warning color)
  - **Close Button Logic:**
    - Disabled if any todos incomplete (`allDone="disabled"`)
    - Admin can always close (`sessionScope.isPspAdmin` bypasses)
  - **Modals Imported:**
    - closeActivityModal.jsp (confirmation modal)
    - **addToDo25.jsp** → AddToDo25 servlet
  - **AddToDo Modal Features:**
    - Form action: AddToDo25
    - Name input: `toDoName`
    - Position selector: `insertWhere` (top, bottom, or after specific todo)
    - Dynamically lists existing todos for insertion point
- [ ] checklistAutomation25.jsp
- [ ] modifyRecurring25.jsp

### Priority 2 (Detail Column)
- [ ] detailHeader25.jsp
- [ ] detailPrimaryContact25.jsp
- [ ] detailDetail25.jsp (dispatcher)
  - [ ] detailSetup25.jsp
  - [ ] detailRenewal25.jsp
  - [ ] detailTicket25.jsp
- [ ] detailAddNote25.jsp
- [ ] detailFooter25.jsp

### Priority 3 (History Column)
- [ ] historyHeader.jsp
- [ ] historyDetail25.jsp

---

## NEXT ANALYSIS STEPS

1. ✅ activityDetail25.jsp structure mapped
2. ✅ ViewActivity25 servlet analyzed
3. ✅ GoActivityDetail25 servlet analyzed
4. ✅ Activity types identified
5. ⏳ **NEXT:** Analyze checklistHeader.jsp
6. ⏳ Analyze checklistBasic25.jsp (likely has TODO actions)
7. ⏳ Analyze detail column components
8. ⏳ Analyze history column components
9. ⏳ Create complete servlet inventory for activity detail page

---

## PACKAGE ANALYSIS

**Modern Servlets (controller package):**
- ✅ ViewActivity25
- ✅ GoActivityDetail25
- ✅ ViewChecklist25
- ✅ ViewById
- ✅ AddNoteToActivity25
- ✅ AddActivityContact25
- ✅ AddToDo25
- ✅ CloseActivity25
- ✅ ViewPastActivity25
- ✅ ReturnFromPastActivity25
- ✅ ModifyContact25
- ✅ RemoveItemFromRenewal25
- ✅ ReOpenToDo25

**All servlets confirmed in modern package - excellent!**

---

## CLEANUP OPPORTUNITIES

### None Found Yet
- All servlets in modern package
- Clean structure with type-based conditional rendering
- Auto-save feature implemented properly

### Potential Improvements
- Auto-save could be extracted to separate JS file
- Type-specific detail JSPs could be consolidated if business logic allows
