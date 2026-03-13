# AMS Claude Memory (Cross-Workstation)

> This file travels via git to bootstrap Claude sessions on either workstation.
> The auto-memory system (`~/.claude/projects/.../memory/MEMORY.md`) holds per-machine session state.
> For full project architecture, see `CLAUDE.md` in the project root.

## Current State
- **Branch:** `refactor/modernize-architecture`
- **Latest migration:** V053
- **Session count:** 69
- **Build tool:** Maven wrapper `./mvnw compile` (no system `mvn` on PATH)
- V025-V037 applied to Demo/BPO/Master; V038 applied to Demo/BPO; V039-V053 code-complete, not yet applied
- Master snapshot v8 taken 2026-03-04 (V037)

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
- **JSTL fn:contains CSV gotcha:** Use comma-padded matching: `",${ids},"` then `fn:contains(csv, ",${id},")`
- **EclipseLink L2 cache eviction** required after entity mutations
- **EclipseLink nested JOIN FETCH** silently dropped — use separate queries
- **EntityManager must stay open** during JSP forward — move forward() inside try block

## Recent Sessions
- **Session 66:** Interactive Import Wizard B2-B5 complete
- **Session 67:** Checklist/BPO improvements — auto-close modal, vendor task management, L2 cache fixes
- **Session 68:** Center panel redesign — unified "Colored Tab" headers, navbar application review badge
- **Session 69:** Docs/demo cleanup — removed obsolete files, consolidated demo/, compressed session archive

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
