# Session 86 — Agent Portal Build-out (V062)

Branch: `refactor/modernize-architecture`
Migration: **V062** (`note.agent_visible`)
Date range: 2026-04-23 → 2026-04-24

## Scope

Built the agent-facing portal pages on top of the V061 ToDo delegation
foundation from Session 85:

1. Per-note agent visibility (V062) + PSP-level default
2. `/AgentSetupList` — agents' list of Setups they're delegated into
3. Agent-flavored Setup detail (`agentSetupDetail25.jsp`) replacing the
   full PSP activity-detail view for agent-only sessions
4. AgentHome rebuilt to **Mockup B** layout (Kanban left + My Tasks
   sidebar grouped by Setup on the right)
5. `AgentCompleteToDo` endpoint so agents can mark tasks done from the
   sidebar without needing the Setup loaded into session
6. Widened `GoActivityDetail25.agentBlocked()` so the originating
   selling agent can open their Setup even without a delegated ToDo

## V062 migration — `note.agent_visible`

`docs/migrations/V062__note_agent_visibility.sql`

- `note.agent_visible TINYINT(1) NULL` — per-note override
- `idx_note_agent_visible` index
- Self-registers into `schema_version` + `schema_info` view

Resolution rule (see `NoteVisibilityResolver`):

```
agent_visible = 1    → visible to agent portal
agent_visible = 0    → hidden from agent portal
agent_visible = NULL → fall back to PSP default
                        (constant key NOTES_AGENT_VISIBLE_DEFAULT,
                         default 0 = hidden)
```

- PSP default added to `UpdatePspSettings.FEATURE_KEYS` + toggle in
  `smtpSettingsMod25.jsp` Features tab
- Cached in `AmsDataGlobal.notesAgentVisibleDefault`; exposed to JSP as
  `${applicationScope.global.notesAgentVisibleDefault}`
- Agent-authored notes are always written `agent_visible=TRUE` (see
  `AddNoteToActivity25.resolveAgentVisible()`), so an agent's own
  nudges stay visible to them regardless of PSP default

## Agent portal pages

| Page | Servlet | JSP |
|---|---|---|
| Agent pipeline + tasks | `/AgentHome` (existing) | `agentHome25.jsp` (Mockup B rebuild) |
| My setups list | `/AgentSetupList` (new) | `agentSetupList25.jsp` (new) |
| Agent Setup detail | `/ViewActivity25` branches | `agentSetupDetail25.jsp` (new) |
| Mark delegated ToDo done | `/AgentCompleteToDo` (new) | — |

### Scope rules (server-side)

- **Agent (role=2)**: sees Setups where `prospect.agent.id = me` OR any
  ToDo on the Setup's checklist has `override_ownership=1 AND owner_id=me`
- **Agency Manager (role=8)**: same but scoped to every agent in the
  agencies they manage (see `AgentSetupList.buildScopedAgentIds`)
- `GoActivityDetail25.agentBlocked()` admits the same two paths on
  single-Setup access

### Agent Setup detail layout

Two-column grid:

- **Left — Application Snapshot**: Employer basics, Key Contacts,
  Services Selected (LOS + Enhancement names, with Enhancements in
  accent-green), Application submit/review dates, and per-section
  application field responses (fed by `AgentSetupSnapshotLoader` which
  mirrors `ReviewApplication` scoping rules)
- **Right — Messages & Tasks**:
  - "My Tasks on This Setup" widget (filtered ToDoOut25 list, only
    owner=me OR `allowsNonOwner`, form posts to `CloseToDo25`)
  - Nudge composer (textarea → `AddNoteToActivity25` with hidden
    `reasonList=7` outbound + `noteStatus=3` Waiting-on-Us)
  - Filtered notes timeline (agent authors indented right with green
    accent; PSP authors flat left with blue accent)
- PSP name wired in everywhere via `${applicationScope.global.psp.fullName}`
  (tenant-branded status pills, composer label, empty-state text, etc.)

### AgentHome Mockup B

- `.agent-main-split` wraps Kanban (flex) + `.tasks-sidebar` (320px fixed)
- Sidebar: SSA-gradient header with open count, groups ToDos by Setup
  with group headers (Setup name = click-through link), round-check
  button per task (POST → `/AgentCompleteToDo`), urgency bands
  (red/past, amber/≤3 days, green/future)
- Replaces the old "Tasks Delegated to Me" full-width strip between
  the pipeline stat bar and the Kanban

## Bugs hunted during this session

### 1. JavaBean two-uppercase-letter decapitalize quirk

Field `iAmSellingAgent` on `AgentSetupRow` DTO with auto-generated
getter `isIAmSellingAgent()`. `java.beans.Introspector.decapitalize`
keeps the first letter uppercase when the first two are both
uppercase (the same rule that keeps `URL` as `URL`, not `uRL`), so
the JavaBean property name became `IAmSellingAgent`. `${row.iAmSellingAgent}`
therefore threw `PropertyNotFoundException` mid-render, aborting
the JSP output at the `iAmSellingAgent=` token — exactly where the
page source was being cut off.

**Fix:** renamed field + getter to `sellingAgentIsMe` (and JSP
reference) to sidestep the rule. Memory: any boolean-field getter
where the stripped name starts with two uppercase letters will
trip this — pick names starting with a single uppercase letter.

### 2. `allowNonOwner()` method typo

`ToDoOut25` defines `public boolean allowsNonOwner()` (verb with `s`,
not `allowNonOwner()` as one might expect from the field name
`allowNonOwner`). Six JSP spots (3 in `agentSetupDetail25.jsp`, 3 in
`checklistBasic25.jsp` from the agent-view filter added earlier)
called `td.allowNonOwner()` — `NoSuchMethodException` on first
iteration, aborted rendering.

**Fix:** replaced all six call sites with `allowsNonOwner()`.

### 3. `c:forEach` over null silently bailing

If `local.getCurrentActivity().getToDoList()` returned null on a
fresh `/ViewById` load before the checklist lazy-loaded, the
iteration in `agentSetupDetail25.jsp` would silently consume the
rest of the column body. Wrapped in `<c:if test="${not empty todoList}">`.

## Files touched

### New

- `docs/migrations/V062__note_agent_visibility.sql`
- `docs/mockups/agent_setup_detail_mockup.html` (design mockup for agent view)
- `docs/mockups/agent_home_delegated_tasks_mockups.html` (A/B/C mockup set; B chosen)
- `docs/analysis/session_86_notes.md` (this file)
- `src/main/java/net/superiorstate/ams/controller/activity/setup/AgentSetupList.java`
- `src/main/java/net/superiorstate/ams/controller/activity/setup/AgentCompleteToDo.java`
- `src/main/java/net/superiorstate/ams/data/resolver/NoteVisibilityResolver.java`
- `src/main/java/net/superiorstate/ams/data/resolver/AgentSetupSnapshotLoader.java`
- `src/main/java/net/superiorstate/ams/model/AgentSetupRow.java`
- `src/main/webapp/WEB-INF/view/sales/agentSetupList25.jsp`
- `src/main/webapp/WEB-INF/view/sales/agentSetupDetail25.jsp`

### Modified

- `docs/claude_memory.md` (session 86, V062)
- `docs/analysis/migration_tracker.md` (V062 row + description)
- `docs/schema_version_migration.sql` (V062 insert)
- `src/main/java/net/superiorstate/ams/controller/activity/AddNoteToActivity25.java` (agent-visible resolution)
- `src/main/java/net/superiorstate/ams/controller/activity/GoActivityDetail25.java` (widened agentBlocked)
- `src/main/java/net/superiorstate/ams/controller/activity/ViewActivity25.java` (branch to agent JSP + snapshot loader)
- `src/main/java/net/superiorstate/ams/controller/user/UpdatePspSettings.java` (NOTES_AGENT_VISIBLE_DEFAULT)
- `src/main/java/net/superiorstate/ams/data/AmsDataGlobal.java` (cached default)
- `src/main/java/net/superiorstate/ams/model/activity/note/Note.java` (agentVisible field)
- `src/main/webapp/WEB-INF/view/a/general/smtpSettingsMod25.jsp` (PSP toggle)
- `src/main/webapp/WEB-INF/view/a/activityDetail/columns/checklist/checklistBasic25.jsp` (allowsNonOwner fix + agent filter)
- `src/main/webapp/WEB-INF/view/a/activityDetail/columns/detail/detailAddNote25.jsp` (visibility override dropdown)
- `src/main/webapp/WEB-INF/view/a/activityDetail/columns/history/historyDetail25.jsp` (per-note visibility filter + badge)
- `src/main/webapp/WEB-INF/view/sales/agentHome25.jsp` (Mockup B sidebar)

## Deployment state after this session

- V062 applied to **Demo** (tested end-to-end with Miller Landscaping setup)
- V062 **not yet applied** to BPO or Master
- No production deploy

## Follow-ups queued for next session

- Apply V062 to BPO + Master (and V058-V061 catch-up if still pending)
- **Agency Manager re-delegation UI** — data model already supports it via
  V061; need a dropdown on each agency-owned ToDo allowing the manager
  to reassign among their agents
- **Cross-agency author detection** — current note indentation uses a
  simple "viewer OR selling agent" rule; should also indent notes
  authored by any fellow member of the viewer's agency
- **FEIN/address/payroll-provider surfacing** on Application Snapshot —
  these live in `ApplicationFieldValue` entries, already pulled by
  `AgentSetupSnapshotLoader`, but could be highlighted as key fields
  in the Employer section rather than only under their section cards
- Remove the stale `allowNonOwner()` pattern in any places I missed
  (grep `allowNonOwner` is clean post-fix but worth re-verifying
  before deploy to BPO)
