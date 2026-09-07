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
- **Latest migration:** **V094** (`employer_participant` — the AMS-owned participant roster, session 26). Updated 2026-09-07 (session 26 close); the line read V093 before that, and V085 before session 25 corrected it. **V092, V093 and V094 are unapplied in Production, Demo, BPO and Master.** V092 and V093 were applied to local `beta_ssa` in session 26 (S26-E) and **both applied cleanly with no errors** — useful information for the production deployment. ⚠️ **`schema_info` reflects apply order, not the highest version applied**: locally V093 ran after V094 and won the `CREATE OR REPLACE VIEW`, so the view understates the schema. Production applies in version order via `update.sh`, so the exposure is to out-of-order or partial applies only. Always re-check `ls docs/migrations/` rather than trusting this line — it has drifted before and will again. ⚠️ **V084 and V085** (`zip_county` crosswalk, T74 ZIP intake) **are applied on Production**, shipped with release `v0.85.00` — confirmed **behaviourally**, not from a deployment log: a 2026-08-01 runtime walk showed ZIP `75482`→Hopkins, `75009`→Collin/Denton chooser, `90210`→correct miss, none of which is reachable against an empty table. V079 onward remains unapplied on every local schema.
- **Latest release:** superseded the 2026-07-31/08-01 entries here entirely; see `docs/session_closeout_2026-08-01_session6.md` §1 for the full `v0.85.00`–`v0.85.06` ladder. ⚠️ **`v0.85.06` is built and pushed (commit `cabbe88`) but NOT DEPLOYED.** Production runs **`v0.85.05`**, which carries a live defect: the AGE_BAND repeater force-rendered a duplicate blank first row under specific conditions, silently doubling the submitted headcount with nothing on screen explaining it (**K3-b**, fixed in `v0.85.06`). `v0.85.06` is a WAR-only release — no migrations attached, since V084/V085 already shipped with `v0.85.00`. Release tags are typed in the GitHub web UI, never pushed from local git — a local `git tag` listing is stale by design; `git fetch --tags` first or read the Releases page.
- **ICHRA/QSEHRA admin stream active — and now BUILT.** Origin: SWBD (Forrest) quoting ICHRA through zizzl, which gated
  carriers and charged a ~$660/mo admin minimum — unbundle logic gives the admin to SSA. Target rail is
  the **HealthSherpa ICHRA Partner API** (`docs.ichra.healthsherpa.com`) — **not** HSOne, and **not**
  EDE; it is an **off-exchange** rail, free to use, purpose-built for ICHRA administrators.
  ⚠️ **"Evaluation only — no build approved" is superseded (2026-07-31): the entire 13-item build
  sequence in `docs/swbd_ichra_build_plan.md` §3 shipped that day**, across three sessions. The
  *enrollment* rail remains unbuilt and still blocked on HealthSherpa (rep, BAA, allow-listing). Its **Policy Status webhook** would unblock #38
  (attestation), but availability is **carrier-gated**: live for Ambetter/Cigna/Molina/Oscar/UHC
  (metro TX covered), **not** for BCBS TX (2026) or CHRISTUS (unlisted) — so rural TX has no automated
  coverage verification today. See `docs/business/healthsherpa.md` for the full evaluation, carrier
  matrix, and open questions.
- **Session count:** 88 numbered sessions logged in `session_history_archive.md`, plus dated (unnumbered) entries — **July 15, 2026** (Agent Pipeline sidebar proposals + create-proposal hand-off, v0.71.08), **July 31, 2026 × 4** (the ICHRA sequence; sessions 2, 3 and 4 have close-out documents in `docs/`), and **August 1, 2026 × 2** (session 5 — ICHRA made to actually work for an agent, close-out `docs/session_closeout_2026-08-01_session5.md`; session 6 — ZIP intake + the illustration UX overhaul, close-out `docs/session_closeout_2026-08-01_session6.md`). ⚠️ **Write-up debts outstanding:** the agency/white-label epic (V068–V071, `AgencyScopeResolver`), the four 2026-07-31 ICHRA sessions, and now both 2026-08-01 sessions are all still missing from `session_history_archive.md`.
- **Build tool:** Maven wrapper `./mvnw compile` (no system `mvn` on PATH)
- Per-environment apply status is tracked authoritatively in `docs/analysis/migration_tracker.md`.
- Master snapshot v9 taken 2026-03-20 (V057)
- ⭐ **Active epic (2026-07-31): ICHRA.** The 13-item sequence in `docs/swbd_ichra_build_plan.md` §3 is **complete** and all six structural decisions (S1–S6) are resolved. Shipped: gated front door + `IchraAccessResolver` + AGE_BAND mode (`0b4711b`) → on-exchange LCSP (`ba023bd`) → affordability threshold (`4556ecd`) → proposal hand-off + LOS-scoped section, V079 (`e5b2009`/`b0e524b`) → design advisor, V080 (`e25ee4e`) → setup checklist content (`4775252`) → group-to-ICHRA conversion (`7db188d`) → opportunity attribution, V081 (`a71b79d`) + the drawer console read (`619461f`). **Session 4 (same day) closed the design advisor's remaining gap** — it shipped in V080 as PSP-admin-only with no UI entry point at all; session 4 gave it one (hub card, T55/T55a), opened the chatbot entry point to ICHRA-entitled agency users (T58, `navbar25.jsp`), opened the skill itself to non-admin callers (T57, **V082**), verified the citation path survives that (matched skills consult no KB — citations are inline `Source:` lines in the `system_prompt`, so leaving the `ichra_design` KB `ADMIN_ONLY` costs nothing), and fixed T52 (matched skills now honour their configured `model`/`max_tokens`, so the advisor runs Sonnet/2048 as V080 intended rather than the hardcoded Haiku/1024 default). **What remains is Kevin's, not a build queue:** the next release (now needs V079–**V082**, not just through V081), the ICHRA/QSEHRA LOS reference rows, the item-10 checklist through the Sequence Builder, D-86/D-87, production allow-listing, the two unsent SWBD emails, and **the one runtime check nobody has run** — ask the design advisor the dental/QSEHRA question from a role-2 agent login post-deploy and confirm the reply cites a source. Close-outs: `docs/session_closeout_2026-07-31_session{2,3,4}.md`. ✅ **That runtime check ran and passed 2026-08-01 (session 5)** — a role-2 agent got a substantively correct answer citing `domain_and_compliance_rules.md` section 5, the first ICHRA capability verified end to end for a non-PSP user. It took three fixes to get there: entitlement resolution (`a5c0d8d`), a retired model (`2b79452`/V083), and an admin write path for the entitlement flag (`0852c2f`/T61). **What remains before the §1 demo is Kevin's and is configuration, not code:** the ICHRA/QSEHRA reference rows (priced `ServiceModule` → `RateTable` with an `agencyrates` assignment), the item-10 checklist through the Sequence Builder, and the two still-unsent SWBD emails (O22, "three groups renewing next quarter"). See `docs/session_closeout_2026-08-01_session5.md`.
- ⭐ **Session 6 (2026-08-01, 12 prompts): ZIP intake shipped, then the illustration UX rebuilt around a runtime walk that found what code review could not.** V084/V085 add a Texas ZIP→county crosswalk (Census 2020 ZCTA data, 2,894 rows, `ZipCountyResolver` — three first-class outcomes: unique/ambiguous/no-match, never auto-selects on a crossing ZIP). The three "steps" on the ICHRA hub (range / age-band / affordability) were dissolved as a framing error, not a defect — Kevin: *"steps 1, 2 and 3 are really 3 versions of the same thing."* `/Illustration` is now **one progressive form**: `mode=` stays a fully supported URL parameter (hub cards unaffected) but is derived from input, not chosen by a toggle. ⚠️ **A full runtime walk (the session's first) found 12+ defects that three prior code-verified passes had missed**, including one (**K3-b**) that silently doubled a submitted headcount by force-rendering a duplicate blank age-band row — root-caused to one clause reading the wrong signal (`mode` instead of the `modeExplicit` flag added earlier the same session, then never wired up). **Deployment gap: `v0.85.06` (the K3 fix) is built and pushed but NOT deployed as of session close — production runs `v0.85.05`, which has the bug.** V084/V085 shipped with `v0.85.00` and are confirmed live on Production by runtime evidence (a `git log`-only session could not otherwise have known this — see the close-out's §8a on container-vs-system state). Full ladder, verification-state tables (runtime-verified vs code-verified-only vs never-tested), and the two process findings (`code-verified` ≠ `runtime-verified`; five prompt-level imprecisions each produced a real defect) are in `docs/session_closeout_2026-08-01_session6.md` §1–§8 — read that file's top section before touching this surface again, not the appendices below it. **Prompt L (hub card consolidation) was scoped and not run** — `ichraHome25.jsp` still shows three cards for a surface that no longer has three steps.
- **Prior epic (post-Session-88, complete):** Agency / white-label / multi-agency hierarchy + access-scope hardening. Shipped in order: **V068** host-header agency landing pages → **V069** per-agency white-label email sending (`EmailIdentityResolver`, 4-tier sender identity) → white-label proposal/application wrapper + RequestQuote host-awareness → **V070** GA→sub-agency parent link (strict two-level hierarchy) → **V071** per-agency public quote tokens for RequestQuote attribution → agency manager-reassignment guards + PSP-staff gate on manual setup. **Landed (merged to trunk 2026-07-15, `e0a62d1`):** introduced `AgencyScopeResolver` (retired 4 duplicated agency resolvers), decoupled scope sets from the `primaryAgencyId` singleton, closed residual IDOR gaps via `canSeeDetail()` (Phases 1–2b), and routed `ViewProposal` agency/agent resolution through `OriginatingAgencyResolver`.
- ICHRA+/QSEHRA+ tier — requirements settled and data model designed 2026-07-29; not built. Canonical doc: `docs/business/plus_tier.md`. Decision highlights: single bundled PEPM (no per-proposal add-on election, because verification method varies per participant); no per-employee pricing; billing unchanged (headcount → Wave, both enrolled and eligible counts); participant verification ledger isolated from the billing pipeline (whose only correction mechanism is whole-month wipe-and-recreate); participant correlation via HMAC-SHA256 of normalized SSN with the key in `ssa.properties`, storing hash + last four only; PremiumPath Card MCC-restricted to 6300/5960 at issuance.
- Summit feeds confirmed — card transaction export (`MCC`, `MerchantName`, `UserID`; `UserID` is the participant's ID even on dependent cards; `MerchantName` truncates at 16 chars) and mailing/coverage-event export (`EventTypeID`, `EventName`, `Mailed`; no participant ID, joined on SSN hash). Both schedulable and employer-filterable. Summit export columns can't be modified; a custom export was priced and declined.
- ~~Tracker discrepancy to resolve — V072/V073 shown unapplied while the Monthly Billing Launcher runs in production.~~ **Resolved** — `migration_tracker.md` now shows both `✅` on Production (verified 2026-07-31). The tracker had been stale, not the release.

> **Active workstream (2026-07-29):** ICHRA/QSEHRA administration on the HealthSherpa **ICHRA Partner
> API** (`docs.ichra.healthsherpa.com` — **not** HSOne). ⚠️ **"Evaluation only; no build approved" was
> true when written on 2026-07-29 and is superseded as of 2026-07-31** — the 13-item agent-facing
> sequence shipped (see the ICHRA epic bullet above). The *enrollment* rail is still unbuilt and still
> externally blocked.
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
- **Session 26 (2026-09-07, S26-A–H, migration V094):** Built the **AMS-owned participant roster**
  that sessions 24–25 were blocked on — `employer_participant` (**V094**) plus a census upload at
  the Setup screen. Two read-only Phase A runs came first and **falsified three claims carried
  forward from sessions 24–25**: AMS *does* have employee-key allocators (a negative-id namespace
  at `min(id)-1`, and `ImportIdResolver`'s `MAX(employee_id)+1`); `employee.custom_id` is
  **write-only** (`getCustomId` has zero callers) and does not round-trip Summit's
  ParticipantCustomID; and `Employee.@Id` is a *mixed* namespace, not simply Summit's id. The
  roster is therefore a **new table, not `employee`** — an id a later import can reallocate cannot
  back a Summit upsert key. It **FKs to `prospect`, not `Employer`**: file 4 fires in the same
  batch as file 1, before the Summit-sourced `Employer` row exists, and S26-A established that
  nothing in the codebase links a `Prospect` to an `Employer` at all. `CensusParseService` does
  CSV + XLSX with header-synonym matching (column order irrelevant, unrecognised columns dropped
  silently), all-or-nothing on any row error, a 5,000-row cap, and XLSX numeric-cell leading-zero
  ZIP recovery. `CensusUploadServlet` is PSP-admin + ICHRA gated, resolves `proposalId → Proposal →
  Prospect`, and **parses from the `Part` stream without ever writing the file to disk** — names and
  home addresses. **SSN, DOB and compensation are excluded at the parser, not just the schema**
  (**LA-35**), so the values never enter the process. Registered **LA-33** (participant key derived
  at emit time, never stored), **LA-34** (replacement refuses rather than merges), **LA-35**.
  **Runtime-verified against a real 38-employee employer file** — the parser end to end, both gates,
  the `proposalId` resolution, `insertAll`, `deleteByProspectId`, and the mapping report. Two
  browser walks found what code review had not: S26-F added a "what was read from the file" mapping
  report (a row count alone cannot distinguish "it parsed" from "it read the columns I think it
  read"), and S26-G fixed a dead end where a loaded roster suppressed the upload form entirely,
  making the replacement refusal unreachable — the refusal now fires **after** parsing, so a
  rejected replacement still reports what the submitted file contained before the operator destroys
  a good roster. ⚠️ **Next is the multi-row sink**: `SummitExportServlet.writeFile` takes a single
  `String`, so **file 2 (Employer CDH Plan) is already wrong on committed code**, not merely
  unbuilt. Commits: `1cfb402` (the build), `e2fc648` (Phase A findings), `3c1c770` (close-out).
  Full detail: `docs/session_closeout_2026-09-07_session26.md`.
- **Session 25 (2026-09-07, S25-A–E, no migration):** Corrected the two wrong data sources in
  session 24's just-shipped `SummitExportServlet` (`cd5c7e0`) before its first real use. Employer
  address and plan year had been read from `Prospect.address` and
  `proposal_ichra_intake.plan_year` — both wrong. **`Prospect.address` never holds the employer's
  address**: unset at five of seven `new Prospect()` creation sites, and agency-derived (via the
  contact `Person`, set from the selling `Agency`) at the other two — the servlet as committed
  would have hard-failed for most proposals and silently emitted the selling agency's address for
  the rest. Re-sourced both fields to `applicationfieldvalue` (`address_street1/city/state/zip`,
  `plan_year_start`/`plan_year_end`), following the established literal-`fieldKey` house pattern
  (`ApplyForProposal`, `ReviewApplication`). Also added a configured installation prefix to
  `Employer TPA Custom ID` (`SUMMIT_TPA_ID_PREFIX`, e.g. `SSA-42`) — a bare `Prospect.id` is only
  unique within one AMS database, and the platform's multi-installation ambition means two
  installations could someday feed the same Summit TPA account. `DEPLOYMENT_KEY` was considered and
  rejected as the prefix source — it's a credential gating destructive operations
  (`InitializeDataBase`, `ReSeedDb`, `SystemRegisterApi`), never a display value. Every missing
  input now refuses by name rather than emitting a partial file; no silent fallbacks anywhere.
  Registered **LA-31** (application answers as the export source) and **LA-32** (installation
  prefix as configuration); narrowed **LA-29**/**LA-30** status lines only. **Code-verified only —
  still never run, output never validated against Summit.** Blocked on three operational
  prerequisites, none of them code: `SUMMIT_TPA_ID_PREFIX`/`SUMMIT_ICHRA_PLAN_TEMPLATE_ID` in
  `ssa.properties` + a Tomcat restart, the `plan_year_eligibility` section (`s125_fsa` package)
  attached to the LOS being sold, and a proposal with a submitted application. Commits: `fb8673d`
  (the fix), `736f9ee` (close-out). Full detail: `docs/session_closeout_2026-09-07_session25.md`.
- **Session 24 (2026-09-07, S24-C/D, docs only, no code, no migration):** First-ever integration
  spec for DataPath Summit's file-based Data Exchange, `docs/business/summit_data_exchange.md` —
  transport, template mechanics, the proven four-file import chain (Employer Demographic → Employer
  CDH Plan → Demographics → HRA Enrollment), and the ID-ownership table. **Every field requirement
  in it is test-verified** (files imported, results read), not inferred from vendor docs, which were
  wrong or silent on several points including ICHRA being a native Summit plan type. Two
  test-verified findings matter most: Summit **upserts** on `Employer TPA Custom ID` (so AMS can
  safely emit full current state with no delta tracking — **LA-26**), and `Participant TPA Custom
  ID` must be **globally unique across every employer**, not per-employer — a duplicate is accepted
  at Demographics import and only fails one file later at HRA Enrollment as `Employer ID Conflict`
  (**LA-25**). Two additional assumptions (**LA-27** pre-tax funding treatment, **LA-28** PCOR
  Reportable) are flagged thin-basis, unverified against actual Summit behavior. The doc's ten local
  open questions are numbered **SDX-01**–**SDX-10**, deliberately distinct from the project's
  existing global `O-NN` registry. Full detail: `docs/session_closeout_2026-09-07_session24.md`.
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
- **2026-07-31 (sessions 2 and 3) — the ICHRA build sequence, items 1–13, shipped in one day.**
  Full detail lives in the two close-out documents (`docs/session_closeout_2026-07-31_session2.md`,
  `…_session3.md`); only what a future session needs up front is repeated here.
  **Session 2** (`e25ee4e`…`77eb56c`): design advisor (V080) + setup checklist content; **LA-13** added
  (SSA's assumptions register is internal work product — LA identifiers stripped from agent-facing
  output); **T50–T55** logged.
  **Session 3** (`7db188d`…`682bc8f`): group-to-ICHRA conversion analysis; **V081** opportunity
  attribution on `illustration_log`; the opportunity-drawer console read; **T56** logged.
  ⚠️ **Three things a future session should not re-derive.** (1) **Item 9 is not a separate servlet** —
  it is an `affordabilityBasis` sub-mode inside `IllustrationServlet`'s `AGE_BAND` path; several docs
  read it as a page. (2) **`Opportunity` has no table** — `Opportunity extends Activity extends
  Assignee`, `SINGLE_TABLE`, so any FK targets **`assignee(id)`** (V050 and V060 got this wrong before).
  (3) **No delete path for an `Opportunity`/`Activity`/`Assignee` row exists anywhere in AMS**, verified
  by grep — an assumption about *absence*, so one new admin servlet falsifies it silently.
  ⚠️ **A retraction worth remembering as method, not trivia:** V081's `ON DELETE SET NULL` was justified
  in its own header as fixing a live foreign-key failure. The verification grep found no delete path
  exists at all. The decision was right for other reasons and stands; the claim was written into the
  migration header *and* the build plan before anyone checked. Corrected in `cf15ff8` / `682bc8f`.
- **2026-07-31 (session 4) — the design advisor's remaining gap, closed.** Full detail in
  `docs/session_closeout_2026-07-31_session4.md`. V080 (session 2) shipped `ICHRA_DESIGN_ADVISOR`
  `is_admin_only=1` with **no UI entry point at all** — deliberately, since none existed yet. Session 4
  built one: `834daac` (T55, hub card copy/target corrected) → `0f631c2` (T55a, card gated on chatbot
  availability) → `668c4d5` (demo-path role walk — established that an external agency user holds
  neither `isPspAdmin`/`isPspUser` nor `isBpoAdmin`/`isBpoUser`, so **no** agency role could ever have
  reached the chat widget; caught and fixed a real defect along the way, the hub's Rate Cache Admin card
  rendering "Live" for every role while its servlet 403s anyone but PSP admin) → `9b95979` (T58, widened
  `navbar25.jsp:408`'s chatbot gate with `|| ichraAvailable`, reusing the page's own already-resolved
  value — no second resolver call) → `a0b3cf2` (T57, **V082** — `chatbot_skill.is_admin_only` 1→0 for
  `ICHRA_DESIGN_ADVISOR`, keyed on `skill_name` per rule 4) → `14b5819` (verified the `ichra_design` KB
  staying `ADMIN_ONLY` costs nothing: `ChatAssistant.executeSkill` sends only `skill.getSystemPrompt()`
  plus the question, no KB retrieval on the matched-skill path at all — citations are 14 literal
  `Source:` lines written inline into V080's `system_prompt`) → `4ea160d` (T52, `executeSkill`'s
  text-only branch now honours a skill's configured `model`/`max_tokens` when both are set, defensively
  — a skill configuring neither is byte-identical to before; only `ICHRA_DESIGN_ADVISOR` changes
  behaviour, `EMAIL_DRAFT_ASSISTANT` cannot keyword-match at all since V065 never set
  `trigger_keywords`). ⚠️ **Three things worth not re-deriving.** (1) **`navbar25.jsp`'s widened
  condition deliberately kept `chatbotEnabled` as a common factor** rather than the literal
  `(existing) || (ICHRA expression)` form, because the literal form would render the widget on an
  installation with no Anthropic API key — a live-looking button where every question fails. (2) **T53
  is now single-guarded**, not doubly — `is_admin_only` no longer blocks a non-admin's fallback KB
  search from the ICHRA skill's own guardrail gap; the KB's own non-admin-ineligibility is now the only
  thing preventing it, so flipping `ichra_design` to non-admin visibility later would reopen T53 for
  real. (3) **V082's blast radius is real, not hypothetical:** `is_admin_only` is a property of the
  skill, not of ICHRA entitlement, so PSP users (`CHATBOT_ALL_USERS`) and BPO users
  (`CHATBOT_ALL_BPO_USERS`) can now also match `ICHRA_DESIGN_ADVISOR` wherever those toggles are on —
  zero in practice only where both are off. **Untested:** whether the advisor's answer actually names a
  source document for a real role-2 login post-deploy — static analysis established the code path but
  nobody has asked it the question for real.
- **2026-08-01 (session 5) — ICHRA made to actually work for an agent.** Full detail in
  `docs/session_closeout_2026-08-01_session5.md`. Deploying v0.82.00 (WAR + V079–V082) produced the
  symptom session 4 could not have predicted: a role-2 agent on an `ichra_enabled` agency saw **no ICHRA
  nav entry and no chat widget**. Three fixes, in order. (1) **Entitlement resolution** (`a5c0d8d`) —
  `IchraAccessResolver` consulted only `primaryAgencyId`, which `AgencyScopeResolver` tie-breaks on
  manager-match else *lowest `agency_id`*, a rule with no relationship to entitlement; it now tests the
  agent's full `detailAgencyIds()` membership set, with the single-agency fast path preserved and INFO
  logging added because a `false` was previously silent. (2) **A retired model** (`2b79452`, **V083**) —
  the advisor 404'd on `claude-sonnet-4-20250514`, absent from `/v1/models`; moved to `claude-sonnet-5`
  with `max_tokens` 2048→3072 (new tokenizer consumes more for the same text, and this skill's value is
  answers that reach their `Source:` lines). ⚠️ **T52 did not cause this — it surfaced it**; before T52
  the configured model was never read. (3) **T61** (`0852c2f`) — `agency.ichra_enabled` had *no admin UI
  write path at all*; `setIchraEnabled()` had zero callers, so entitlement was hand-written SQL. Build
  plan item 2 said "copy V067 exactly" and only the DDL had been copied. Mirrored `markup_enabled`'s
  checkbox in `agencyManager25.jsp` + `AgencyAction`; no migration. ✅ **The runtime check then passed** —
  correct answer citing `domain_and_compliance_rules.md` §5. ⚠️ **Two things worth carrying forward.**
  `a5c0d8d` is **not established as the cure** — the production INFO line shows a single-agency agent
  whose fallback never fired, so the redeploy (and EclipseLink's shared cache dropping a stale `Agency`
  written by raw SQL) is the leading alternative; entitling via the new checkbox on a *running* instance
  then worked with no restart, which is **consistent with that hypothesis, not a disproof of it** — the
  UI writes through JPA, the very path that keeps the cache correct. The general question is **T64**.
  And T62's citation wording (`Cite <doc>.md section N`) is **not instruction leakage** — V080's own
  few-shot exemplar answers contain that literal string four times; the model imitated them faithfully.
  Backlog: T60/T61/T62/T63/T64 logged, T61 closed.
- **2026-08-04 — automation email dropped a primary contact that the screen displayed (no migration).**
  A renewal showed `CLINT ROSENBERG / clint@northernmetals.com` on the activity-detail page, but
  `SendAuto25` treated it as having no recipient and injected a "To (Email Address)" prompt.
  **Root cause: a `Person`'s address can live on its own `assignee.email` column *or* on a linked
  `Employee`, and the two layers disagreed about which to read.** `detailPrimaryContact25.jsp` checks
  the Employee first; every server-side path read only `Person.getEmail()`, a plain field getter.
  `Employee.getEmail()` compounds it by preferring `hr_email` over `email`. Fixed by adding
  **`Person.getEffectiveEmail()`** (`employee.hr_email` → `employee.email` → `person.email`, Employee-first
  so what is sent matches what is shown) and routing six call sites through it: `SendAuto25`,
  `AutomationHelper.getRecipientList` (**including its dedupe keys** — otherwise a contact that is both
  primary and additional double-adds), `SendAutoFinal25`, both `EmailDAO` overloads, plus
  `autoPreview25.jsp` and `detailAdditionalContacts25.jsp`, which would otherwise have rendered a
  recipient chip with an empty `<>`. ✅ **Runtime-verified by Kevin** on a locally-created renewal — the
  contact resolved into the SendAuto email as intended. ⚠️ **Four things worth not re-deriving.**
  (1) **It is not a renewal bug**, despite surfacing there — production counts were **29 Tickets, 8
  Renewals, 0 Setups**; the `primary_contact` column is on `Activity`, so every type is exposed.
  Renewals merely *create* such contacts, via `fillPrimaryContacts()` → `employer.contactList[0]` →
  `PersonDAO.getPersonByEmployee()`. (2) **`AddActivityContact25.getValidEmail()` already implemented the
  identical Employee-first rule** as a private helper — the precedence chosen here is house convention,
  not a new invention; it simply had never been applied to the automation path. (3) **`ModifyContact25` /
  `ModContact25` were deliberately left alone** — they pre-fill an *editable* field, so resolving there
  would write the employee's address onto the person row on save. (4) **The phantom-Person bug, fixed
  in a follow-up commit the same day — and it was the larger half.** `PersonDAO.getPersonByEmployee()`
  returned an empty unsaved `Person` (not `null`) when no match existed. All **three** callers are
  written as `if (p == null)` / `if (p != null)`, so a non-null placeholder defeated every one:
  `AmsDataLocal.fillPrimaryContacts()` and `SessionVar` installed the placeholder as the activity's
  primary contact (renders blank, can never receive mail), and — the consequential one —
  **`EmailDAO.getPersonByEmail()`'s `if (p == null) AuthDAO.createPersonFromEmployee(...)` branch was
  unreachable dead code.** So a CC'd address belonging to an Employee with no Person row resolved to the
  placeholder, got added to the recipient list by `AutomationHelper.processLists`, and was then
  **silently dropped** by `EmailDAO`'s `isValidEmail` filter — no error, no delivery. **7,892 employees**
  were in that state. Fixed by returning `null` (matching `getPersonByEmployee1()`, the correct twin
  that had zero callers). ✅ **Runtime-verified by Kevin against the reported case.**
  **Release split:** `v0.88.02` (= `809e0f2`) carries only the `getEffectiveEmail()` fix; the
  phantom/dropped-recipient fix ships in the next release. ⚠️ **The one-line fix alone would have been a regression**: it activates
  `createPersonFromEmployee()`, never executed before, whose `e.getState().substring(0,2)` NPEs on a
  null state — **807** of those employees have one. Hardened in the same commit, along with silencing a
  `printStackTrace()` on the now-routine "no Person yet" probe. Blast radius on the display half was
  nil: **0 open renewals** would have produced a phantom primary contact. Note 5 employees carry
  duplicate Person rows, which `AuthDAO.getPersonFromEmployee()`'s `getSingleResult()` would throw
  `NonUniqueResultException` on — unreachable from the create path (duplicates imply a Person exists),
  but a live trap for any future caller.
  **Also this session:** discovered that local `beta_ssa` is refreshed from production **weekly by a
  scheduled task** (`Weekly-Refresh-beta_ssa`, Sundays 03:05, `C:\Scripts\Pull-BetaSsa.ps1`) — undocumented
  until now, and the source of two wrong conclusions before it was found. Documented in
  `docs/analysis/local_render_verification.md`; the script itself was repaired (it lives on the
  workstation, outside this repo).

## Session 2026-08-04 (b) — HealthSherpa research + doc reconciliation (docs only, no code, no migration)

**O2 closed** — the enrollment/status API surface re-verified against the ICHRA Partner API's public
documentation. Open since 2026-07-29 at an estimated hour; it took about that, **needed no
credential, no representative and no BAA**, and falsified more recorded claims than any other single
action in this workstream. 13 of 25 doc pages read.

**Six corrections to `docs/business/healthsherpa.md`**, the headline being that **UnitedHealthcare
has been API-enrollable in Texas since 2026-06-09** — seven weeks *before* this repo recorded it as
"the known non-API-enrollable carrier." **That claim was wrong when written, not superseded**, which
is a reason to distrust the 2026-07-28 HSOne-era findings harder than "wrong shape." Also: the
deeplink path is `/public/ichra/off_ex`; `tpa_slug` is accepted, not rejected; the deeplink accepts
`ssn`, so PHI minimisation is a **design choice**, not a property of the rail; `pending_effectuation`
belongs to Submission Confirmation, not Policy Status; headless enrollment is **carrier-dependent**,
not impossible (ACH can be set server-side).

⭐ **`plan_hios_id` is required on every enrollment route**, including Deeplinks V2 (*"no
shopping/browse experience is supported"*). **There is no hand-off that moves the ERISA
neutral-presentation burden to HealthSherpa** — it sits with SSA, which makes **O20** load-bearing
rather than theoretical.

⭐ **The market data is not proprietary.** ACA rates are carrier filings; HealthSherpa, zizzl and
Ideon all read the same ones — which is why three vendors agree to the cent. Differentiation is
catalog completeness and who controls it (exactly where zizzl failed Forrest). The **rate-data** half
of the HealthSherpa dependency is therefore low-risk; the **enrollment rail** is the real bet.

⭐ **Silver loading almost certainly explains V078's 44% on-vs-off LCSP gap** — off-exchange-only
silver *mirror* plans without the CSR load, i.e. different plans, not different prices for one plan.
Makes T44/V078 **systematic infrastructure, not belt-and-braces**. Untested; the probe is two staging
calls (**T151**).

**Cache economics, settled:** a county-year is **2 API calls → 44 rows**; storage is irrelevant at
any scale (22,352 rows for all Texas × 2 plan years). **The only cost is calls, and it is a function
of cadence** — the job refreshes daily, but ACA rates are annual filings. D-83's "which counties"
was never a cost question (**T152**, **D-83** reframed).

⚠️ **Two dated items with a November deadline:** **T148** — `AgeCurve` has **no 2027 curve**, so
D-84's plan-year seeding is a **no-op** without a code change (and only 5 of the 2026 curve's 44
factors were ever verified). **T127** raised LOW→MED — once two plan years are live, **sort order in
a constant silently decides the plan year**.

⚠️ **The demo breaks at step 6**, and has since item 7 shipped: the *"Use This in a Proposal"*
control is `disabled` on staging rates, so `swbd_ichra_build_plan.md` §1's *"no external gate"* claim
was wrong the day it was written. Its gate is **redundant** — `ViewProposal` refuses staging data at
three further points. Kevin reopened the provenance decision deliberately and asked for a
time-boxed, agency-scoped admin override: **T150** (requirement + design sketch, not specced).

**Filed:** T144–T152. **Updated:** `healthsherpa.md` (two new dated sections, +540 lines),
`business/README.md` (all four §15-flagged stale items fixed), `ichra_strategy.md` (O2 closed, §4
staleness banner, §10 register), `ichra_platform_capability_map.md`, `ichra_administration_scope.md`,
`project_backlog.md`, `deployment_backlog.md` (D-83, D-84), `swbd_ichra_build_plan.md`.

⚠️ **Not updated, deliberately:** `docs/analysis/plus_tier_build_plan.md` — canonical for build
mechanics with O1–O40, not read this session, and editing it blind is this project's recurring
failure mode. **O2's findings almost certainly move several O-items there; it needs its own pass.**

⚠️ **Caveat on everything above:** findings came from fetched-and-summarised documentation pages, not
from parsing the OpenAPI YAML each API reference page has linked since 2026-07-09. **Nothing is safe
to code against without that check** — filed as **T145**, and the reason is the three prior misses
(`fip_code`, `/api`, `/public`).

---

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
