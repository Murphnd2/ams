# AMS Claude Memory

> This is the single authoritative current-state document for AMS — branch, latest migration, active
> epic, recent sessions, key gotchas. AMS is now developed on a single workstation; the earlier
> two-file, cross-workstation arrangement (this file plus a second `.claude/memory/MEMORY.md`) no
> longer applies and has been retired (2026-07-30) — `.claude/memory/MEMORY.md` is stale (V069,
> Session 85) and should not be read for current state.
> For full project architecture, see `CLAUDE.md` in the project root.

## Current State
- **Integration branch:** `refactor/modernize-architecture` — feature branches are cut from / merged back to it, so it trails the in-flight feature by only a few commits. `main` is ~345 commits stale and is **not** the working line.
- **In-flight branch:** none — the agency-scope-resolver work merged to trunk 2026-07-15 (`e0a62d1`); branch deleted.
- **Latest migration:** V075 (always re-check `ls docs/migrations/` — this line lags)
- **Latest release:** v0.73.02 (reconciled 2026-07-30; release-note detail not verified in this pass — local `git tag` is stale by design since releases are cut in the GitHub web UI, see CLAUDE.md's Releases section — check the GitHub Releases page for what v0.73.02 actually shipped). Prior entry here (v0.71.08, 2026-07-15) is superseded.
- **ICHRA/QSEHRA admin stream active.** Origin: SWBD (Forrest) quoting ICHRA through zizzl, which gated
  carriers and charged a ~$660/mo admin minimum — unbundle logic gives the admin to SSA. Target rail is
  the **HealthSherpa ICHRA Partner API** (`docs.ichra.healthsherpa.com`) — **not** HSOne, and **not**
  EDE; it is an **off-exchange** rail, free to use, purpose-built for ICHRA administrators.
  **Evaluation only — no build approved.** Its **Policy Status webhook** would unblock #38
  (attestation), but availability is **carrier-gated**: live for Ambetter/Cigna/Molina/Oscar/UHC
  (metro TX covered), **not** for BCBS TX (2026) or CHRISTUS (unlisted) — so rural TX has no automated
  coverage verification today. See `docs/business/healthsherpa.md` for the full evaluation, carrier
  matrix, and open questions.
- **Session count:** 88 numbered sessions logged in `session_history_archive.md`, plus one dated (unnumbered) entry — **July 15, 2026: Agent Pipeline sidebar proposals + create-proposal hand-off (v0.71.08)**. The agency/white-label epic (V068-V071, `AgencyScopeResolver`) below still spans several sessions that were never written up — that catch-up entry is still outstanding.
- **Build tool:** Maven wrapper `./mvnw compile` (no system `mvn` on PATH)
- Per-environment apply status is tracked authoritatively in `docs/analysis/migration_tracker.md`.
- Master snapshot v9 taken 2026-03-20 (V057)
- **Active epic (post-Session-88):** Agency / white-label / multi-agency hierarchy + access-scope hardening. Shipped in order: **V068** host-header agency landing pages → **V069** per-agency white-label email sending (`EmailIdentityResolver`, 4-tier sender identity) → white-label proposal/application wrapper + RequestQuote host-awareness → **V070** GA→sub-agency parent link (strict two-level hierarchy) → **V071** per-agency public quote tokens for RequestQuote attribution → agency manager-reassignment guards + PSP-staff gate on manual setup. **Landed (merged to trunk 2026-07-15, `e0a62d1`):** introduced `AgencyScopeResolver` (retired 4 duplicated agency resolvers), decoupled scope sets from the `primaryAgencyId` singleton, closed residual IDOR gaps via `canSeeDetail()` (Phases 1–2b), and routed `ViewProposal` agency/agent resolution through `OriginatingAgencyResolver`.
- ICHRA+/QSEHRA+ tier — requirements settled and data model designed 2026-07-29; not built. Canonical doc: `docs/business/plus_tier.md`. Decision highlights: single bundled PEPM (no per-proposal add-on election, because verification method varies per participant); no per-employee pricing; billing unchanged (headcount → Wave, both enrolled and eligible counts); participant verification ledger isolated from the billing pipeline (whose only correction mechanism is whole-month wipe-and-recreate); participant correlation via HMAC-SHA256 of normalized SSN with the key in `ssa.properties`, storing hash + last four only; PremiumPath Card MCC-restricted to 6300/5960 at issuance.
- Summit feeds confirmed — card transaction export (`MCC`, `MerchantName`, `UserID`; `UserID` is the participant's ID even on dependent cards; `MerchantName` truncates at 16 chars) and mailing/coverage-event export (`EventTypeID`, `EventName`, `Mailed`; no participant ID, joined on SSN hash). Both schedulable and employer-filterable. Summit export columns can't be modified; a custom export was priced and declined.
- Tracker discrepancy to resolve — `migration_tracker.md` shows V072/V073 unapplied to every environment, but the Monthly Billing Launcher is live in production and depends on `billing_run` / `billing_run_step`. Either the tracker is stale or the pending `v0.73.PP` release is not actually WAR-only. Resolve before cutting it.

> **Active workstream (2026-07-29):** ICHRA/QSEHRA administration on the HealthSherpa **ICHRA Partner
> API** (`docs.ichra.healthsherpa.com` — **not** HSOne). Evaluation only; **no build approved.**
> See `docs/business/healthsherpa.md` (API evaluation, carrier matrix, open questions),
> `docs/business/ichra_administration_scope.md` (service scope and MEC/subsidy segmentation),
> `docs/business/ichra_platform_capability_map.md` (offering model), and
> `docs/analysis/phase_a_ichra_enrollment_portal.md` (AMS feasibility).
>
> **Also outstanding and unrelated to ICHRA:** `docs/analysis/security_findings_2026-07-28.md` —
> FINDING 1 (`/CreateBpoTestUser`, unauthenticated account creation with a hard-coded password printed
> in the response) should be remediated ahead of feature work. Tracked as **T29**.

**ICHRA+ / QSEHRA+ planning — Rev 5 (2026-07-30)**
- `docs/analysis/plus_tier_build_plan.md` is **canonical** — decisions D1–D37, open items O1–O38,
  phased build list, prioritised question list. Seven parts; **precedence runs Part 7 > Part 6 >
  Part 5 > Part 4 > Parts 1–3** (earlier parts retained for provenance, superseded where they
  conflict).
- **Participant architecture settled:** census intake at AMS → generated Summit import file → Summit
  creates participants → J2/J3 back into AMS. **No negative-ID employees for "+" participants**, so
  the `mergeNegativeToPositiveEmployees` data-loss path (T33) is off the route.
- **Ongoing new hires are entered Summit-first.** Only the setup census is AMS-first.
- **Notice mechanism:** a **notional COBRA-type benefit** whose status change fires Summit's
  tracked-letter vehicle. It also **flags eligibility counts**, and is **not billable** by DataPath
  while participants remain active employees — cost is notice mailing only.
- **All eligibles are loaded**, not only enrollees, which separates the eligible and enrolled counts
  at source.
- **Sequencing criterion:** earliest evidence that SSA makes SWBD's network win more cases — not
  earliest live administration.
- **Known risk:** the census/participant phase changes `SummitImportService` / `SummitImportWizard`,
  live production import code where T36's divergent status mapping lives. **Needs its own Phase A.**
- `docs/analysis/summit_notice_automation_discovery.md` — Summit test protocol, **not yet run**.
- **No SQL was produced.** Highest migration remains **V075**, pending re-verification against
  `ls docs/migrations/`.

## Key Patterns
- **"25" suffix** = current/modern version of servlet or JSP. For model-layer adapter classes at `model/` root, three variants exist:
  - **`25` (no suffix):** `@Entity` mapped to a DB view — read-only JPA projection, no setters. E.g. `Activity25` → `a25_activity_list_open`.
  - **`25p`:** `@Entity` on a separate DB view for **participating** activities — those where the current user has an assigned task/dependency. Table name confirms: `a25_activity_list_participating`. Stored in `AmsDataLocal.activitiesWithDependencies`.
  - **`25u`:** No JPA. Mutable copy constructed from the base `25` entity; adds computed fields and view helpers (`isDelegated`, `getDateHtml()`, `Comparable`, `RecurringTaskList`). These are the in-memory working objects held in session/controllers/JSPs. Origin of "u" not preserved — best evidence: **u**nmapped (no JPA) or **u**tility (adds view computation).
  - New view adapters should use descriptive names rather than single-letter suffixes. (Resolves open question #22.)
- **Ghost buttons and page layout — only one of these is global.** `.ssa-action` (modal footers, form
  actions) is defined in `css-js.jsp` and is genuinely global. `.nav-ghost` is defined once inside
  `navbar25.jsp`, so it comes free with the navbar import. **`.ghost-action` and `.audit-wrap` are
  neither** — they are per-JSP inline `<style>` declarations, redeclared in every page that uses them,
  and they drift: `.audit-wrap`'s height is `calc(100vh - 64px)` in `rateCacheAdmin25.jsp` but
  `calc(100vh - 56px)` in `emailDraftTest25.jsp`. Copy from a sibling page; do not assume a global
  exists. Canonical `.ghost-action`: `benefitAudit25.jsp`. Canonical `.audit-wrap`:
  `rateCacheAdmin25.jsp`. (Verified 2026-07-31 — zero hits across all three CSS files.)
- **`AgencyScopeResolver` — the public entry point is `resolve`, not `resolvePrimaryAgencyId`.**
  `resolvePrimaryAgencyId` is **private** and cannot be called from a servlet. Use
  `AgencyScopeResolver.resolve(em, request).primaryAgencyId()` — it reads `AmsDataLocal` off the
  session and the five role flags itself. An explicit-inputs overload
  `resolve(EntityManager, Person, boolean×5)` exists for callers already holding them.
  `primaryAgencyId` can be **null** (a Person belonging to no agency), and for PSP staff it resolves
  but does not imply authorization. (Verified 2026-07-31.)
- **sanitizeHtml():** Strips `<script>`, `on*` handlers, `javascript:` protocols. Preserves `<style>`.
- **Never use bare `return;`** in servlets — always forward/redirect
- **PSP ID from session:** `local.getCurrentPerson().getPsp().getId()` (NOT `getCurrentPsp()`)
- **selectOptions delimiter:** pipe-delimited (`|`), not comma
- **EclipseLink nested `JOIN FETCH` (2-level) drops the deep collection.** A query like
  `Opportunity → prospect → proposalList` silently fails to populate the 2nd-level collection. Load the
  deep collection via a **separate flat query off its own root** (pattern in `SalesDAO`,
  `QuestionnaireService`, `FillQuestionnaire`). Confirmed + fixed in `AgentHome` (2026-07-15).
- **User identity can split across three fields.** Login authenticates on `user.user_name`
  (`AuthDAO.validateLogin`); `User.email` and `Person.email` can diverge (Agency Manager shows
  `Person.email`; User Manager shows `User.email`). Always confirm the actual `user_name` before telling
  someone to run a password reset. (Cost debugging time on the SWBD/Forrest login, 2026-07-15.)

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

## Logging

- Config: `src/main/resources/log4j2.xml` (Log4j 2.20.0 + `log4j-slf4j2-impl` SLF4J bridge)
- Output: console (`catalina.out`) AND `${catalina.base}/logs/ams.log` (daily rotation, 14-day retention)
- Levels: `net.superiorstate.ams` → DEBUG; noisy libs (EclipseLink, Apache HTTP, Tomcat) → WARN; root → INFO
- New code pattern: `private static final Logger log = LoggerFactory.getLogger(MyClass.class);` (import `org.slf4j.*`)
- Existing `System.out.println` calls are NOT being bulk-migrated — clean up organically when files are touched
- Resolves open question #20

## Auth

`LoginFilter` (`net.superiorstate.ams.LoginFilter`) is a `@WebFilter("/*")`. The following paths bypass session auth:

- `/api/` — handled separately (lines 55-58); authenticated via `ApiTokenFilter`, not session
- `/proposal/` — public proposal viewer (links sent to clients)
- `/apply/` — public application / intake forms; `/saveApplication` (submit endpoint) also exempt
- `/q/` — public questionnaires; `/saveQuestionnaire` (submit endpoint) also exempt
- `/tpo` — **intentional legacy support**: outdated URL from a prior business website; landing page informs visitors the link is outdated; kept for backward compat with old marketing materials
- `/uploadRateSheet` — rate sheet upload by external parties
- `/video` — public video content
- `/outlook/` — Outlook Web Add-in taskpane endpoint

Static resources (`/images/`, `/css/`, `/js/`, `/fonts/`, etc.) are exempted before the auth check via `isStaticResource()`. Resolves open question #17.

## Recent Sessions
- **Session 70:** Sequence Manager enhancements — copy-from-existing modal, inline rename, unsaved changes warning, wider left panel, fixed-width badges, filter scoping fix
- **Session 78:** Outlook add-in "Create Ticket" feature — new API endpoints (ticket-categories, create-ticket), tabbed taskpane UI, contact selection from email recipients
- **Session 80:** Wasabi S3 upload reliability overhaul — `RequestBody.fromBytes()` instead of InputStream, singleton client, Apache HTTP client, 120s timeout, JSP spinner fix
- **Session 81:** Automation email editable preview — full `autoPreview25.jsp` with Quill editor, new session flow (SendAuto25 → autoInputScreen25 → PrepareAutoPreview25 → autoPreview25 → SendAutoFinal25?fromPreview=true), recipient dedup bug fixed
- **Session 82:** Outlook add-in polish + RequestQuote bot protection
- **Session 83:** Fixed NPE on activity detail when CheckList lookup returns null
- **Session 84:** Setup promotion cross-linking (SetupPromotionService: App link, Opp link, WON stage, Internal Note, close Opp when PSP-managed) + ActivitySessionGuard for automation multi-tab bug
- **Session 85:** Agent Delegation on Setup ToDos (V061) — ToDo-level ownership override, OriginatingAgencyResolver, User Assignment sub-row, AgentHome delegated-to-me panel, GoActivityDetail25.agentBlocked(), originating-agent header item (Option C).
- **Session 86:** Agent Portal build-out (V062 `note.agent_visible`) — per-note agent visibility + PSP-level default, `/AgentSetupList` scoped servlet/JSP (AgentSetupRow DTO + AgentSetupSnapshotLoader for application field snapshot), agent-flavored Setup detail (`agentSetupDetail25.jsp` with two-column Application Snapshot + Messages/Tasks + nudge composer + agent/PSP-indented notes timeline), AgentHome rebuilt to Mockup B (Kanban left + My Tasks sidebar grouped by Setup with urgency bands), AgentCompleteToDo endpoint (ownership-gated), widened `agentBlocked()` to admit originating selling agent, NoteVisibilityResolver helper, PSP name swapped in everywhere `${applicationScope.global.psp.fullName}`. **Two JSP bugs hunted down:** `iAmSellingAgent` EL property invisibility (JavaBean two-uppercase-letter decapitalize quirk — renamed to `sellingAgentIsMe`); `ToDoOut25.allowNonOwner()` typo (actual method is `allowsNonOwner()`, fixed in both agent and PSP JSPs). Detail in `docs/analysis/archive/session_86_notes.md`.
- **Session 87:** Agent portal polish + Add Note redesign. Agent Setup detail: Done button now posts to `AgentCompleteToDo` (not `CloseToDo25` — that only queues for a PSP-home round-trip agents never make, so completions silently didn't persist); new `AgentReopenToDo` mirrors the complete servlet for undo; Completed collapsible section added; filter narrowed to `isMyTask()` only (shared `allowsNonOwner` tasks no longer leak in). AgentHome Kanban: CONTACTED column removed from `boardStages`, column width shrunk (min 210 / flex 220 / max 240) — now fits widescreen without horizontal scroll. CONTACTED removed as a selectable stage from `agentHome25.jsp` new-opp modal, `detailOpportunity25.jsp` inline editor, and `AgentHome.STAGE_ORDER` (CSS color class and JS label/color maps retained for legacy data). **Add Note redesign** (`detailAddNote25.jsp` full rewrite): tri-state agent-visibility pill in the header bar (`tabindex="-1"`, cycles Default → Visible → Hidden on click via hidden `<input name="agentVisible">`); Reason + Status selects moved to a new `.note-footer-bar` below the editor with `min-width: 170px` / `140px`; tab order now Quill → Reason → Status → Save (Quill's Tab binding targets `select[name="reasonList"]` first, falls back to submit).
- **Session 88 (July 10):** White-Label Proposal → Application Flow (no migration). Public proposal/application/confirmation pages suppress the PSP band when a selling agency is present — agency name in header, charcoal neutral band, `© AgencyName` footer; compose-email pre-fill signature uses the sender's agency. `EmailTemplate.wrap()` (activity emails, quick-send) still uses PSP name — separate future task.
- **Post-Session-88 (agency epic, not yet numbered in the archive):** V068 host-header agency landing pages (`login.routeLogin` host dispatch + `LandingSafe` Jsoup sanitizer) → V069 per-agency white-label email (`EmailIdentityResolver` 4-tier; removed the unconditional `mail.smtp.from` override so SMTP2GO VERP owns the return-path) → white-label wrapper login + RequestQuote host-awareness → V070 GA→sub-agency parent link (strict two-level guardrails, rate-assignment constraint) → V071 per-agency quote tokens → agency manager-reassignment guard + PSP-staff gate on manual setup → **`feat/agency-scope-resolver`**: `AgencyScopeResolver` consolidation (retired 4 duplicated resolvers) + IDOR closure via `canSeeDetail()`. **These sessions still need writing into `session_history_archive.md`.**
- **2026-07-31 — rate cache correctness + HealthSherpa doc reconciliation.** Two defects found in
  Phase B-1b code, both caught pre-deployment. (1) HealthSherpa's `POST /api/v1/quotes` defaults to
  `per_page: 20` and `meta` carries no total, so every cached aggregate was computed on 20 of 65
  plans — no Gold at all, 5 of 27 silver, and **LCSP is the ICHRA affordability threshold**.
  (2) Catastrophic plans are restricted to under-30, so the single age-21 call's plan set is invalid
  for ages 30+; the age curve governs premiums but says nothing about which plans exist. Both fixed
  in `eba17ad`; `healthsherpa.md` reconciled in `5d08677`. Also established: **no ZIP→FIPS endpoint
  exists on the ICHRA Partner API** (the docs point to a reference *page*, not an endpoint), so AMS
  must carry its own county reference data; and **D-78/D-79 have never been applied to any
  environment**, so `AppConfig.getHealthSherpaApiKey()` has always returned null and no AMS
  installation has ever authenticated to HealthSherpa. `getHealthSherpaBaseUrl()` defaults to
  **production** when unset — open decision whether to default to staging or refuse to call.

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

---

## Session 2026-07-28 — ICHRA / HealthSherpa (docs only, no code)

**No code, schema, or SQL changes. Highest migration remains V073.**

**Business:**
- Off-exchange enrollment access **requested** via HealthSherpa developer portal.
- **Marketplace account deliberately not linked** — linking binds a single agent profile and would
  pre-answer the open AOR question in the wrong direction. See `docs/business/healthsherpa.md`.
- Contact established: **KJ Sherman**, HealthSherpa Technical Product Team. Five written questions
  sent; call offer deferred pending written answers. Questions logged in `healthsherpa.md`.
- **Blocking answer:** whether `GET /v1/enrollments` surfaces lapse/termination/grace states or only
  current status. Determines whether monthly attestation is automatable — and therefore whether the
  economics work against zizzl.

**Investigation:**
- Phase A complete → `docs/analysis/phase_a_ichra_enrollment_portal.md`. **No Phase B approved.**
- Security findings → `docs/analysis/security_findings_2026-07-28.md`. **FINDING 1
  (`/CreateBpoTestUser`, unauthenticated account creation with hard-coded password) is unrelated to
  ICHRA and should be remediated ahead of feature work.**
- ICHRA service scope → `docs/business/ichra_administration_scope.md`.

**Documentation accuracy — important for future sessions:**
- `docs/analysis/entity_reference.md` contains data claims derived from **dead code**
  (`ReferenceDataSeeder.java`; entry point `Main.java:35` commented out). **ICHRA/EBHRA/QSEHRA LOS
  rows are not created by any live seeder**, and the live `DatabaseInitializer` LOS block is itself
  commented out (476-510). An accuracy warning has been added to that file. **Read it before relying
  on any "what data exists" claim.**
- `.claude/inventory/*` files are stale Pass-1 snapshots (2026-04-25, claim V062). Warnings added.
- `UserRole` 6 and 7 do not exist in seeding. Role 4 "Applicant" is seeded but **inert** — zero
  readers, never assigned to any user.
- There is **no `TemplatePurpose` Java class** — the entity is `ServiceItem`,
  `@Table(name="templatepurpose")`.

**Unresolved and blocking Phase B scoping:** whether any live database contains manually-entered
ICHRA/EBHRA/QSEHRA `LOS`, `ServiceItem`, `PlanType`, or task-sequence rows. **Requires a direct
database query — static analysis cannot answer it.**

---

## Session 2026-07-29 — HealthSherpa product correction (docs only, no code)

**No code, schema, or SQL changes. Highest migration remains V073.**

**The headline: the ICHRA Partner API (`docs.ichra.healthsherpa.com`) is a separate, more capable
product from HSOne.** Confirmed by HealthSherpa's product owner for ICHRA/off-exchange. This
**reinstates** a framing `docs/business/healthsherpa.md` had previously retracted — **the retraction
was wrong.** Full correction appended to that file.

**Resolved favorably:**
- **AOR is per-application by NPN**, not fixed per key — the go/no-go question. `_agent_id` is
  *required* on the deeplink (HSOne rejected it). Maps cleanly onto SWBD's downline, and means SSA is
  structurally not competing with an agency's agents for the policy.
- **Webhooks exist** — Submission Confirmation and Policy Status. The prior note said unconfirmed.
- **`paid_through_date` and `grace_period_start_date`** are in the payload — the attestation primitive.
- **Staging environments exist.** **Free to use**, no contract or pricing gate.
- **QSEHRA is supported** on the off-exchange rail.

**Resolved unfavorably:**
- **Policy status is carrier-gated off-exchange.** BCBS TX ☑️ 2026, **CHRISTUS blank**, HCSC payment
  webhook *In Progress*. **Rural Texas has no automated coverage verification today.** Metro Texas
  does — Ambetter, Cigna, Molina, Oscar, and UHC all have it live.

**Architecture change:** enrollment is **deeplink-out** — HealthSherpa collects the PHI, AMS sends
prefill demographics only. **This substantially reduces the Phase A PHI problem.** The alternative path
(EnrollConnect) reinstates it. Noted on `docs/analysis/phase_a_ichra_enrollment_portal.md`.

**Unresolved, blocking design:** whether the deeplink supports **employee self-service** or assumes an
**agent** completes it. Determines whether AMS builds an employee portal or an agent workstation.

**On-exchange is worth pursuing** — status there is granted at **state level** (FFM states plus
Georgia; Texas is FFM), not carrier by carrier, making it currently the *more* mature rail for coverage
automation in Texas. Blocked on ⚖️ licensure (a counsel question — FFM web-broker rules; SSA holds no
licensure or appointments) and on a Marketplace agent-account link SSA cannot satisfy. **Julian said
"entirely off-exchange" but the docs describe on-exchange enrollment for FFM carriers — ask, do not
infer.**

**New docs this session:** `docs/business/ichra_platform_capability_map.md` (five-layer offering model
for GAs and agents; **Layers 1 and 5 — sales/modeling tools and the GA console with its alert engine —
are the defensible ones**). MEC/subsidy segmentation appended to
`docs/business/ichra_administration_scope.md` — **off-exchange coverage is MEC, and subsidies only
matter to the subsidy-eligible, so PremiumPath splits by population rather than failing outright.**

**Contacts:** Julian Ferdman (product, ICHRA/off-ex), KJ Sherman (technical product), Michael Levin
(role unknown, CC'd without introduction). **No onboarding rep assigned — this blocks staging
credentials and the webhook configuration form.**

**Still unaddressed by anyone: the BAA.** Counterparty Geozoning, Inc. DBA HealthSherpa.

**Useful for future Claude Code work:** the ICHRA docs expose `GET <page>.md?ask=<question>` for
dynamic querying, a full index at `/llms.txt`, and an MCP integration at
`/getting-started/ai-agents-and-mcp`.
