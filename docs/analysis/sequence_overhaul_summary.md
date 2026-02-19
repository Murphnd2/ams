# Sequence Template Overhaul — Session Summary

**Date:** February 19, 2026
**Feature:** #4 — Recurring Tasks / Sequence Template Overhaul
**Status:** ✅ FULLY FUNCTIONAL — Save, Create, Delete all working

---

## What Was Built

### New Files Created

1. **`src/main/java/net/superiorstate/ams/controller/sequence/SequenceBuilder25.java`**
   - Entry-point servlet at `/SequenceBuilder25`
   - Replaces both `GoTicketTemplate25` and legacy `SequenceHome`
   - Supports `?load=ID` to pre-select a sequence in the builder
   - Eagerly loads task counts via COUNT queries (avoids lazy-load issues)
   - Session attributes prefixed with `sb` to avoid collision with old page

2. **`src/main/java/net/superiorstate/ams/controller/sequence/SequenceAction25.java`**
   - Action servlet at `/SequenceAction25`
   - Handles SAVE (reorder/add/remove tasks), CREATE (new sequence), DELETE (soft-delete)
   - Parses JSON task array from client-side drag-and-drop builder
   - Creates new Task entities for custom tasks, links existing reusable tasks
   - Ticket creation: builds TicketSubCategory → TemplatePurpose → RequiredTaskList chain
   - Renewal/Setup creation: links to unassigned TemplatePurpose
   - Always redirects back to SequenceBuilder25

3. **`src/main/webapp/WEB-INF/view/a/general/sequenceBuilder/sequenceManager25.jsp`**
   - Two-panel layout: left = sequence list, right = task builder
   - Left panel: filterable by type (Ticket/Renewal/Setup), searchable, clickable
   - Right panel: drag-and-drop reordering, add new/existing tasks, reusable flag toggle
   - Save serializes task order + flags to JSON, POSTs to SequenceAction25

### Files Modified

1. **`src/main/webapp/WEB-INF/view/general/admin/adminMenuOC.jsp`**
   - Changed "Manage Task Templates" href: `GoTicketTemplate25` → `SequenceBuilder25`

### Bug Fixed

1. **`src/main/java/net/superiorstate/ams/model/ReqTaskListTix.java`** (line ~69)
   - `getSingleResult()` → `getResultList().get(0)` to handle duplicate TicketSubCategory rows

---

## Architecture Notes

- **Session attribute prefix `sb`** — all new attributes use `sb` prefix so old pages still work during transition
- **`taskCountMap`** (request attribute) — `Map<Long, Integer>` built via COUNT queries to avoid lazy-loading `taskSequenceTableList` after EntityManager closes
- **Task.java getter names** — non-standard: `hasOwner()`, `hasGoTo()`, `hasInfo()`, `isSourced()` (not `isHasOwner()` etc.)
- **Task property flags** (owner, link, outsource) deferred to ManageTask25 — only reusable flag is togglable in the builder since the others require additional data (person, weblink)
- **JSON task payload** format from `prepareSubmit()`:
  ```json
  [
    {"order": 0, "taskId": 123, "desc": "Verify caller identity", "reusable": true},
    {"order": 1, "taskId": -1, "desc": "New custom task", "reusable": false}
  ]
  ```
  Where `taskId=-1` means create a new Task entity.

---

## Old Pages Preserved (Phase 3 Cleanup Candidates)

These are untouched and can be deleted once the new builder is proven in production:

| File | Notes |
|------|-------|
| `controller/activity/ticket/GoTicketTemplate25.java` | Old entry servlet |
| `controller/checklist/TaskBuilder25.java` | Old action servlet (btnTb routing) |
| `sequenceBuilderForm.jsp` | Old UI (radio buttons + session state) |
| `checklistBuilder.jsp` | Duplicate of old UI |
| `SequenceHome` servlet + `sequenceHome.jsp` | Legacy entry point (previous package) |

---

## Key Entity Relationships (Reference)

```
TemplateGroup (1=Renewal, 2=Setup, 3=Ticket)
  └── TemplatePurpose (Health FSA, COBRA, HSA, etc.)
        └── RequiredTaskList extends TaskSequence
              └── TaskSequenceTable (join: task + sortOrder)
                    └── Task

For Tickets only:
  TicketCategory → TicketSubCategory → TemplatePurpose
  ReqTaskListTix (DTO wrapping RequiredTaskList + TicketSubCategory for display)
```

---

## Development Workflow Note

Discovered during this session: no need to clean/package/restart Tomcat for most changes.
- **Java changes:** `Ctrl+F9` (incremental build) → `F5` in browser
- **JSP changes:** Just save → `F5` in browser
- **Tomcat Run Config:** Set "On Update action" and "On frame deactivation" to "Update classes and resources"
- **Full clean/package only needed for:** pom.xml changes, persistence.xml changes, structural changes
