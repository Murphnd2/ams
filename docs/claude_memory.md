# AMS Claude Memory (Cross-Workstation)

> This file travels via git to bootstrap Claude sessions on either workstation.
> The auto-memory system (`~/.claude/projects/.../memory/MEMORY.md`) holds per-machine session state.
> For full project architecture, see `CLAUDE.md` in the project root.

## Current State
- **Branch:** `refactor/modernize-architecture`
- **Latest migration:** V062
- **Session count:** 87
- **Build tool:** Maven wrapper `./mvnw compile` (no system `mvn` on PATH)
- V025-V037 applied to Demo/BPO/Master; V038 applied to Demo/BPO; V039-V062 code-complete, V060+V062 applied to Demo (testing agent portal)
- Master snapshot v9 taken 2026-03-20 (V057)
- **Active feature (Session 87):** Agent portal polish on top of Session 86's V062 foundation. Shipped: agent Setup detail completion persistence fix (`AgentCompleteToDo`/`AgentReopenToDo`), Completed collapsible, filter narrowed to owned tasks only, Kanban widescreen fit (CONTACTED column removed + column width shrunk), CONTACTED removed from all selectable stage dropdowns, Add Note redesign (tri-state agent-visibility pill in header + inline footer Reason/Status/Save).

## Key Patterns
- **"25" suffix** = current/modern version of servlet or JSP
- **Ghost buttons** = `.ssa-action` (modal/form), `.ghost-action` (toolbar), `.nav-ghost` (navbar)
- **Flex page layout** = `.audit-wrap` pattern: flex column, toolbar + scrollable body, `height: calc(100vh - 64px)`
- **sanitizeHtml():** Strips `<script>`, `on*` handlers, `javascript:` protocols. Preserves `<style>`.
- **Never use bare `return;`** in servlets — always forward/redirect
- **PSP ID from session:** `local.getCurrentPerson().getPsp().getId()` (NOT `getCurrentPsp()`)
- **selectOptions delimiter:** pipe-delimited (`|`), not comma

## Key Entity Gotchas
- **ApplicationField PK** is `String fieldKey` (not Long)
- **Application PK is `proposal_id`** (not auto-generated) — needs `LEFT JOIN FETCH p.application`
- **Person has `listOfAgenciesWithThisAgent`** (ManyToMany), not `getAgency()`
- **Application.reviewedBy FK** must reference `assignee(id)` not `person(id)`
- **Person is SINGLE_TABLE in `assignee`** — any FK to a Person column must reference `assignee(id)` (applies to V060 outlook_user_link, V061 todo.owner_id, etc.)
- **`Assignee` class is at `net.superiorstate.ams.model.general.Assignee`** (not `model.activity.Assignee`)
- **`proposal.createdBy` is typically the PSP user** who built the proposal (not the outside agent) — unreliable as a signal for the originating agency; prefer `proposal.sourceActivity.assignedTo` or `proposal.prospect.agent`
- **JSTL fn:contains CSV gotcha:** Use comma-padded matching: `",${ids},"` then `fn:contains(csv, ",${id},")`
- **EclipseLink L2 cache eviction** required after entity mutations
- **EclipseLink nested JOIN FETCH** silently dropped — use separate queries
- **EntityManager must stay open** during JSP forward — move forward() inside try block

## Recent Sessions
- **Session 70:** Sequence Manager enhancements — copy-from-existing modal, inline rename, unsaved changes warning, wider left panel, fixed-width badges, filter scoping fix
- **Session 78:** Outlook add-in "Create Ticket" feature — new API endpoints (ticket-categories, create-ticket), tabbed taskpane UI, contact selection from email recipients
- **Session 80:** Wasabi S3 upload reliability overhaul — `RequestBody.fromBytes()` instead of InputStream, singleton client, Apache HTTP client, 120s timeout, JSP spinner fix
- **Session 81:** Automation email editable preview — full `autoPreview25.jsp` with Quill editor, new session flow (SendAuto25 → autoInputScreen25 → PrepareAutoPreview25 → autoPreview25 → SendAutoFinal25?fromPreview=true), recipient dedup bug fixed
- **Session 82:** Outlook add-in polish + RequestQuote bot protection
- **Session 83:** Fixed NPE on activity detail when CheckList lookup returns null
- **Session 84:** Setup promotion cross-linking (SetupPromotionService: App link, Opp link, WON stage, Internal Note, close Opp when PSP-managed) + ActivitySessionGuard for automation multi-tab bug
- **Session 85:** Agent Delegation on Setup ToDos (V061) — ToDo-level ownership override, OriginatingAgencyResolver, User Assignment sub-row, AgentHome delegated-to-me panel, GoActivityDetail25.agentBlocked(), originating-agent header item (Option C).
- **Session 86:** Agent Portal build-out (V062 `note.agent_visible`) — per-note agent visibility + PSP-level default, `/AgentSetupList` scoped servlet/JSP (AgentSetupRow DTO + AgentSetupSnapshotLoader for application field snapshot), agent-flavored Setup detail (`agentSetupDetail25.jsp` with two-column Application Snapshot + Messages/Tasks + nudge composer + agent/PSP-indented notes timeline), AgentHome rebuilt to Mockup B (Kanban left + My Tasks sidebar grouped by Setup with urgency bands), AgentCompleteToDo endpoint (ownership-gated), widened `agentBlocked()` to admit originating selling agent, NoteVisibilityResolver helper, PSP name swapped in everywhere `${applicationScope.global.psp.fullName}`. **Two JSP bugs hunted down:** `iAmSellingAgent` EL property invisibility (JavaBean two-uppercase-letter decapitalize quirk — renamed to `sellingAgentIsMe`); `ToDoOut25.allowNonOwner()` typo (actual method is `allowsNonOwner()`, fixed in both agent and PSP JSPs). Detail in `docs/analysis/session_86_notes.md`.
- **Session 87:** Agent portal polish + Add Note redesign. Agent Setup detail: Done button now posts to `AgentCompleteToDo` (not `CloseToDo25` — that only queues for a PSP-home round-trip agents never make, so completions silently didn't persist); new `AgentReopenToDo` mirrors the complete servlet for undo; Completed collapsible section added; filter narrowed to `isMyTask()` only (shared `allowsNonOwner` tasks no longer leak in). AgentHome Kanban: CONTACTED column removed from `boardStages`, column width shrunk (min 210 / flex 220 / max 240) — now fits widescreen without horizontal scroll. CONTACTED removed as a selectable stage from `agentHome25.jsp` new-opp modal, `detailOpportunity25.jsp` inline editor, and `AgentHome.STAGE_ORDER` (CSS color class and JS label/color maps retained for legacy data). **Add Note redesign** (`detailAddNote25.jsp` full rewrite): tri-state agent-visibility pill in the header bar (`tabindex="-1"`, cycles Default → Visible → Hidden on click via hidden `<input name="agentVisible">`); Reason + Status selects moved to a new `.note-footer-bar` below the editor with `min-width: 170px` / `140px`; tab order now Quill → Reason → Status → Save (Quill's Tab binding targets `select[name="reasonList"]` first, falls back to submit).

## Reference Docs
| Topic | Location |
|-------|----------|
| Detailed session history | `docs/analysis/session_history_archive.md` |
| Migration tracking | `docs/analysis/migration_tracker.md` |
| Schema version SQL | `docs/schema_version_migration.sql` |
| Project backlog | `docs/analysis/project_backlog.md` |
| Deployment backlog | `docs/deployment_backlog.md` |
| Entity data model | `docs/analysis/entity_reference.md` |
| Servlet/endpoint map | `docs/analysis/application_flow.md` |
