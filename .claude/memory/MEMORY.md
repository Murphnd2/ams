# AMS Project Memory

## Current State
- **Branch:** `refactor/modernize-architecture`
- **Latest migration:** V069
- **Session count:** 85
- **Build tool:** Maven wrapper `./mvnw compile` (no system `mvn` on PATH)
- Per-environment apply status is tracked authoritatively in `docs/analysis/migration_tracker.md`.
- Master snapshot v9 taken 2026-03-20 (V057, nginx SSL, hostname ssa-master)
- Production SSL migrated from Comodo wildcard to Let's Encrypt via nginx (2026-03-20)
- **Active feature (Session 85+):** Agent delegation per-Setup — V061 ToDo owner override shipped; next = agent-facing Setup detail view in agent portal (see Session 86 planning)

## Key Patterns
- **"25" suffix** = current/modern version of servlet or JSP
- **Ghost buttons** = `.ssa-action` (modal/form), `.ghost-action` (toolbar), `.nav-ghost` (navbar)
- **Flex page layout** = `.audit-wrap` pattern: flex column, toolbar + scrollable body, `height: calc(100vh - 64px)`
- **Sticky headers** = `position: sticky; top: 0; background: #f8f9fa; z-index: 1` on `<th>`
- **pageTitle/pageIcon** = request attributes before navbar import
- **selectOptions delimiter:** pipe-delimited (`|`), not comma
- **sanitizeHtml():** Strips `<script>`, `on*` handlers, `javascript:` protocols. Preserves `<style>`.
- **Never use bare `return;`** in servlets — always forward/redirect
- **PSP ID from session:** `local.getCurrentPerson().getPsp().getId()` (NOT `getCurrentPsp()`)

## Database & Migrations
- Migration tracker: `docs/analysis/migration_tracker.md`
- Schema version SQL: `docs/schema_version_migration.sql`
- [Migration SQL checklist](feedback_migration_checklist.md) — every migration needs 3 file updates
- **EclipseLink L2 cache eviction** required after entity mutations
- **EclipseLink nested JOIN FETCH** silently dropped — use separate queries
- **EntityManager must stay open** during JSP forward — move forward() inside try block

## Role System
- 1=PSP User, 2=Agent, 3=Client, 4=Applicant, 5=PSP Admin, 8=Agency Admin, 9=PSP Sales
- 102=BPO Admin, 103=BPO User
- Home agency = first agency matching PSP ID

## IntelliJ Relaunch Options
1. **Update Assets** — JSP/CSS/JS only. Fastest.
2. **Update Assets and Classes** — JSP + hot-swap Java method bodies.
3. **Redeploy** — New fields/methods/classes/annotations.
4. **Package and Deploy** — After POM changes.
5. **Clean, Package and Deploy** — Nuclear option.

## Key Entity Gotchas
- **ApplicationField PK** is `String fieldKey` (not Long)
- **Application PK is `proposal_id`** (not auto-generated) — needs `LEFT JOIN FETCH p.application`
- **Person has `listOfAgenciesWithThisAgent`** (ManyToMany), not `getAgency()`
- **Application.reviewedBy FK** must reference `assignee(id)` not `person(id)`
- **Person is SINGLE_TABLE in `assignee`** — any FK to a Person column must reference `assignee(id)` (V060 outlook_user_link, V061 todo.owner_id, etc.)
- **`Assignee` class lives at `net.superiorstate.ams.model.general.Assignee`** (not `model.activity.Assignee`)
- **`proposal.createdBy` is usually the PSP user**, not the outside agent — unreliable as agency signal; prefer `proposal.sourceActivity.assignedTo` or `proposal.prospect.agent`
- **JSTL fn:contains CSV gotcha:** Use comma-padded matching: `",${ids},"` then `fn:contains(csv, ",${id},")`

## Recent Sessions (60-85)
- **Session 60:** BPO auto-approval for required-sequence tasks (V049), ticket activity type fix, BPO reseed fix
- **Session 66:** Interactive Import Wizard B2-B5 complete (ImportResolutionService, ImportCommitService, AJAX resolution UI)
- **Session 67:** Checklist/BPO improvements — auto-close modal, vendor task management, L2 cache fixes
- **Session 68:** Center panel redesign — unified "Colored Tab" headers, navbar application review badge
- **Session 69:** Docs/demo cleanup session
- **Session 72:** Update script version-sort fix, activity list scroll fix
- **Session 73:** Training video system with single-use token viewing (V056)
  - TrainingVideo + VideoToken entities, ManageVideos servlet, ServeVideo servlet
  - Token-gated MP4 streaming from /var/lib/tomcat10/videos/
  - Navbar menu item gated by `isMasterSystem` flag
  - Production deployment: V054-V056 applied, ams_app granted ALL PRIVILEGES
- **Session 74:** Agency suppression feature (V057), navbar cleanup, SuperDashboard token fix
  - Agency.suppressed field + suppress/unsuppress toggle in PspAgencyHome
  - Suppression unassigns all rates (SalesDAO), hides from agency listing, new-activity modal, ProposalBuilder "all agencies"
  - Navbar: removed "Application Review" from Sales dropdown
  - DatabaseResetUtil: reset deployment_key constant on reseed (fixes SuperDashboard 401s after ReSeedDb)
  - V057 applied to Production; V039-V057 update script generated for Demo/BPO
- **Session 75:** SSL migration + Master v9 snapshot
  - Production SSL: Comodo wildcard → Let's Encrypt via nginx (certbot nginx authenticator, zero-downtime renewal)
  - SSL docs rewritten for nginx reverse proxy architecture (tomcat_ssl_setup.md, deployment_runbook.md, deployment_strategy.md)
  - Master v9: schema V037→V057, nginx+certbot-nginx installed, D-66 ReadWritePaths for /data/, chatbot keys removed from ssa.properties, update.sh synced, hostname ssa-master
  - All VPS hostnames set: ssa-production, ssa-demo, ssa-bpo, ssa-master (D-50)
  - backup.sh added to repo (docs/scripts/), D-37_Backfill.sql deleted
- **Session 76:** NDT Section 125 questionnaire framework + census-based testing data model (V058, V059); BPO dashboard tonal zone redesign, task push fixes
- **Session 77:** Outlook Web Add-in "Log to AMS" (V060 outlook_user_link) — FK fix: references assignee(id) not person(person_id)
- **Session 78:** Outlook add-in "Create Ticket" from email — ticket-categories + create-ticket API, tabbed taskpane, contact selection from recipients
- **Session 80:** Wasabi S3 upload reliability overhaul — `RequestBody.fromBytes()` over InputStream, singleton client, Apache HTTP client, 120s timeout, JSP spinner fix
- **Session 81:** Automation email editable preview — full `autoPreview25.jsp` with Quill editor, new flow (SendAuto25 → autoInputScreen25 → PrepareAutoPreview25 → autoPreview25 → SendAutoFinal25?fromPreview=true), recipient dedup fix
- **Session 82:** Outlook add-in polish + RequestQuote bot protection
- **Session 83:** NPE fix on activity detail when CheckList lookup returns null
- **Session 84:** Setup promotion cross-linking — SetupPromotionService (App link, Opp link, WON stage, Internal Note, close Opp when PSP-managed) + ActivitySessionGuard for automation multi-tab bug
- **Session 85:** Agent Delegation on Setup ToDos (V061 todo_ownership_override) — per-ToDo owner override fields (`override_ownership`, `has_owner`, `owner_id`, `allow_non_owner`), OriginatingAgencyResolver, User Assignment sub-row, AgentHome "delegated to me" panel, `GoActivityDetail25.agentBlocked()`, originating-agent header (Option C). **Next: agent-facing Setup detail view in portal**

## Detailed Topic Files
- [sessions.md](sessions.md) — Full session notes (39-60)
- [features.md](features.md) — Feature system details (proposals, questionnaires, imports, etc.)

## Demo Materials
- All demo files consolidated under `demo/` folder:
  - `demo/summit-import/` — J-series CSVs for Summit Import Wizard testing
  - `demo/interactive-import/` — Generic CSVs for Interactive Import Wizard testing
  - `demo/brochures/` — COBRA, FSA, login brochures (HTML + PDF)
- None are loaded by Java code — all for manual upload testing only

## Reference
- Full session archive: `docs/analysis/session_history_archive.md`
- Deployment backlog: `docs/deployment_backlog.md`
- Cross-workstation memory: `docs/claude_memory.md` (slim bootstrap file, travels via git)
