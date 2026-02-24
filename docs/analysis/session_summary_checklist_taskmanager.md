# Session — February 24–25, 2026 — Checklist Panel Layout + Task Manager Modernization

## Summary

Completed checklist panel restructuring (A10 footer, layout, add task modal) and began task manager page modernization (`taskManager25.jsp`). Spans Track A completion items and the beginning of the Manage Task screen overhaul.

## Completed Items

### A9 — Checklist Automation Integration (carried from prior session)
- Removed separate `checklistAutomation25.jsp` bar from `activityDetail25.jsp`
- Lightning bolt icon on first open automated task in `checklistBasic25.jsp`
- Modal-based automation preview/send workflow
- Info icon inline next to task descriptions

### A10 — Checklist Panel Restructuring
- **Header** (`checklistHeader.jsp`): Added "+" icon button to open `addToDoModal`
- **Body** (`checklistBasic25.jsp`): Removed fixed `max-height:575px`. Open items scroll in flex-grow area. Completed section pinned below scroll (always visible without scrolling).
- **Footer** (`checklistFooter25.jsp`): Simplified to just Close button (`btn-outline-secondary`) + modal imports. Add Task moved to header.
- **Panel CSS** (`activityDetail25.jsp`): `#panelLeft` changed from `overflow: hidden auto` to `display: flex; flex-direction: column; overflow: hidden` so body fills remaining height.
- **Automation import removed** from `activityDetail25.jsp`

### Add Task Modal Modernization
- `addToDo25.jsp`: SSA blue gradient header, stacked label+input layout, `btn-ssa` submit
- "At the top" option first, "At the bottom" last (selected by default)
- Only shows open (non-complete) tasks in "After:" dropdown
- Dropdown font sized to 0.85rem

### Task Manager Page Modernization (`taskManager25.jsp`)
Complete rewrite with SSA design patterns:

**Layout:**
- Two-column: left 1/3 (`col-lg-4`) task settings, right 2/3 (`col-lg-8`) email automation
- Full viewport height via flex layout (`tm-form` constrained to `calc(100vh - 110px)`)
- No page scrollbar — left card scrolls internally, right textarea fills available space

**Left Column — Task Settings card with four SSA-bordered sections:**
1. **Ordering** — compact toggle rows (Any Time / Only When At Top, Independent / Blocks Below)
2. **Employee Assignment** — three-way toggle (Anyone / Assigned / Exclusive) with conditional dropdown
3. **Vendor Sourcing** — three-way toggle (Internal / Sourced / Vendor Only) with conditional dropdown
4. **Links** — GoTo and Info each with fixed-width (120px) toggle + URL input that shows/hides

**Right Column — Email Automation:**
- Display statement input
- Monospace textarea that flex-grows to fill available height
- Tag Reference button opens overlay

**Tag Reference Overlay:**
- Positioned via JS (`getBoundingClientRect`) to exactly cover the left card
- Matches left card's rounded corners and position
- SSA border, shadow, table with sticky header
- Sits below the "Manage Task" header bar

**Footer:**
- Save/Cancel ghost buttons inside left card at bottom (`mt-auto`)
- Underline accent style, no background or side borders

**Form field names preserved exactly** — no changes to `UpdateTask25.java` needed.

## Bug Fixes

### Null-safe EL expressions in `taskManager25.jsp`
- `toDo.getTask().getGoToLink().getLinkPath()` → wrapped in `<c:if test='${...!=null}'>`
- `toDo.getTask().getInfoLink().getLinkPath()` → wrapped in `<c:if test='${...!=null}'>`
- `toDo.getTask().getAutomation().getHtmlContent()` → wrapped in `<c:if test='${...!=null}'>`

### Method name fix
- `applicationScope.global.getPspUsers()` → `applicationScope.global.getUsers()` (getPspUsers doesn't exist on AmsDataGlobal)

### BPO dropdown selection
- Changed from `toDo.getTask().getOwner().getId()` to `toDo.getTask().getSourceOwner().getId()` for source owner matching

## Java Fixes Still Pending (from A9 session)

In `AmsDataLocal.java`:
- **TD_ADD case**: Add `getCurrentActivity().reSortToDoList();` after inserting new item
- **AUTO_CLOSE case**: Replace manual `remove(t)` + `add(t)` with `getCurrentActivity().reSortToDoList();`

## Files Modified

| File | Location | Changes |
|---|---|---|
| `checklistHeader.jsp` | `activityDetail/columns/checklist/` | Added "+" Add Task button |
| `checklistBasic25.jsp` | `activityDetail/columns/checklist/` | Flex layout, pinned completed section |
| `checklistFooter25.jsp` | `activityDetail/columns/checklist/` | Simplified to Close button only |
| `activityDetail25.jsp` | `activityDetail/` | panelLeft flex column, removed automation import |
| `addToDo25.jsp` | `checklistDetail/` | SSA modal modernization |
| `taskManager25.jsp` | `taskManager/` | Complete rewrite |

## No Database Changes

No SQL migrations this session — all changes are JSP/UI and CSS.

## Track A Status

| Item | Description | Status |
|---|---|---|
| A1+A2 | Section Headers | ✅ |
| A3+S1+S2 | Detail Header | ✅ |
| A4 | Primary Contact | ✅ |
| A5 | Type-Specific Panels | ✅ |
| A6 | Add Note | ✅ |
| A7 | Footer Decomposition | ✅ |
| A8 | Checklist Body | ✅ |
| A9 | Checklist Automation | ✅ |
| A10 | Checklist Footer/Layout | ✅ |
| A11 | History Body | ✅ |
| A12 | Modal Standardization | ✅ |
| A13 | Closed activity banner | Remaining |
| A14 | Auto-save UX | Remaining |
| S4 | Standardize pe-none gating | Remaining |
| S5 | Mobile stacking polish | Remaining |
