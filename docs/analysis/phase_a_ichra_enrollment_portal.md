# Phase A — ICHRA/QSEHRA Employee Enrollment Portal Feasibility

**Investigation date:** 2026-07-28
**Branch at time of investigation:** `refactor/modernize-architecture`
**Highest migration at time of investigation:** V073
**Status:** Investigation complete. **No Phase B build approved.**

## Purpose

Establish what AMS can and cannot do today against a proposed employee-facing individual-plan
enrollment portal built on the HealthSherpa ONE API. See `docs/business/healthsherpa.md` for the
API evaluation and `docs/business/ichra_administration_scope.md` for the service scope.

## Proposed shape (NOT approved, NOT built)

1. Agent quotes an ICHRA or QSEHRA line of service through the existing sales portal.
2. Proposal accepted → Application → Setup activity generated.
3. Employer census imported during Setup, creating employee records in AMS.
4. Census-listed employees invited to an authenticated, employer-scoped enrollment portal.
5. Enrollment status polled per employer, driving monthly attestation and reimbursement.

## Q1 — Native `Employee` creation without a Summit ID

**Verdict: possible today, via the existing negative-ID convention.**

- `Employee.id` is `@Id @Column(name="employee_id") private int id;` — **no `@GeneratedValue`**. Every
  code path must explicitly `setId()` before persist.
- **Three independent ad hoc negative-ID allocators exist**, all `MIN(id)-1`, with no shared helper
  and no DB-level protection beyond PK uniqueness (race risk):
  - `data/service/Updater.java:558-560, 598` — `ensurePrimaryContactEmployee`
  - `controller/user/CreatePspUser25.java:163-166` and `controller/authentication/CreatePspUser.java:158-161`
    (duplicated) — `getOrCreateEmployeeForUser`, loads the entire Employee table to find the minimum
  - `data/service/DatabaseInitializer.java:1653` — `createEmployee`, called with literal `-1`
- A fourth, correct implementation — `data/util/HsaBillingHelper.java:100-103` (`getNextEeId`, filters
  `id < 0`) — has **zero call sites** anywhere in `src/main/java`.

### What breaks

1. **CDH billing is Summit-staging-dependent.** `data/service/MonthlyBiller.java:373-406`
   (`logCoverageStatusForThisMonthCDH` — the path covering HRA/MERP/ICHRA/EBHRA/QSEHRA) is driven
   entirely off `Enrollment2.importEmployee → ImportEmployee`
   (`model/summit/temp/Enrollment2.java:18-20`), a Summit-staging-only entity wiped monthly. An
   AMS-native employee gets **no CoverageStatus, no BillingGrid row, no bill** without a parallel
   insertion path.
2. **Premium Billing explicitly excludes negative IDs.** `MonthlyBiller.java:329-334`:
   `WHERE ee.employer.id = :id AND ee.id > 0`. Live in both the legacy pipeline and the Monthly
   Billing Launcher (`BillingPipelineRunner.java:229`).
3. **DATA LOSS RISK — `mergeNegativeToPositiveEmployees`** (`Updater.java:622-730`) calls
   `em.remove(...)` (line 715) on a negative-ID employee and reparents its links onto a positive-ID
   employee if it name/email-matches one at the same employer. For an ICHRA enrollee who later
   appears in a Summit import, this silently destroys a record carrying enrollment and attestation
   history. **Any native-employee design must exempt itself from this sweep or avoid the negative-ID
   convention entirely.**
4. No shared/enforced allocator — adding a fourth implementation compounds the race risk.
5. Pre-existing latent bug: `data/dao/PersonDAO.java:320` (`assignToGenericEmployer`) persists an
   `Employee` **without ever calling `setId()`**, relying on the primitive `int` default of `0`.
6. Inconsistent `gridId` formatting for negative IDs: `HsaBillingHelper.java:39-49` (N-prefixed) vs
   `MonthlyBiller.java:486` (raw, double-dash).

**Safe:** `Updater.java:386` already carves out negative IDs protectively (excludes them from
Summit-absence deactivation sweeps).

**Bulk intake:** the Interactive Import Wizard (`controller/data/InteractiveImport.java:45`, PSP
Admin/BPO Admin only) is architecturally generic (`ImportProvider` is per-PSP configurable, not
Summit-hardcoded) and its commit step (`ImportCommitService.java:552-577`) creates real `Employee`
rows — but always with a **positive**, MAX+1-allocated ID, and it is file-upload-driven. **No
dedicated census/roster upload servlet exists.**

## Q2 — Employee-scoped login

**Verdict: `LoginFilter` needs only targeted additions. `AmsDataLocal` needs a parallel class.**

- **Live seeded roles** (`DatabaseInitializer.fillUserRoles`, lines 544-553): 1=PSP User, 2=Agent,
  3=Client, 4=Applicant, 5=PSP Admin, 8=Agency Admin, 9=PSP Super User, 102=BPO Admin, 103=BPO User.
  **Roles 6 and 7 are never seeded.** Role 101 is referenced in code but never seeded.
- **`LoginFilter.java` performs authentication only** — no role-based authorization anywhere in it.
  It reads `AmsDataLocal` off session attribute `"local"` and checks `isAuthenticated()` (lines
  78-86); unauthenticated → redirect to `/login` (line 92). Role checks happen ad hoc in ~50
  individual controllers via raw session-attribute booleans (`isPspAdmin`, `isAgent`, `isApplicant`)
  computed once at login by `data/dao/AuthDAO.java:92-125` (`assignUserRoles`). **There is no central
  authorization dispatcher.**
- Allow-list: static-resource heuristic (lines 96-111); `/api/*` (delegated to `ApiTokenFilter`);
  DB-uninitialized special case; exact-match `ALLOWED_ENDPOINTS` (lines 19-24); prefix allows (line
  87): `/proposal/`, `/apply/`, `/q/`, `/tpo`, `/outlook/`, plus exact `/saveQuestionnaire`,
  `/uploadRateSheet`, `/saveApplication`.
- **Role 4 "Applicant" is inert.** `isApplicant` is set at login (`AuthDAO.java:118`) and has **zero
  readers** anywhere in `src/main/java` or any JSP. **No `User` is ever assigned role 4.** The actual
  applicant-shaped mechanism today is the GUID-based `/apply/*` and `/q/*` prefixes, which bypass the
  User/UserRole model entirely.
- **`AmsDataLocal` is staff-shaped and leaks by default.** `intializeLocalData` (lines 89-150) loads
  `UserFilterPreset` (staff-only, 107-111), staff role flags (120-134), a compose-email scaffold
  (135-136), and — critically — for any `AppConfig.isPsp()` install, **unconditionally** copies the
  entire installation's open-activity list and renewal-employer list into every session object
  (lines 137-141: `global.getActivitiesAllOpen()`, `global.getActivitiesWithDelegation()`,
  `fillRenewalEmployers(em)`), all reachable via public getters with no scoping.
  `CurrentActivity.fillEmployeeList` (1309-1332) similarly loads **every** employee of an activity's
  employer.
- **`AmsDataGlobal`** is a single ServletContext-wide object: `activitiesAllOpen`
  (`SELECT a FROM Activity25 a`, no WHERE, lines 246-258), `employers` (all active, 263-272),
  `agencies`, `prospects`, `users` — all public getters, all unfiltered.
- **No existing role is scoped to a single `Employer`.** The nearest analogue,
  `AgencyScopeResolver`/`AgencyScope` (`data/resolver/`, merged 2026-07-15), stops at **agency**
  granularity.

### Required changes

1. A dedicated employee-session state class — **not** `AmsDataLocal` reused unmodified.
2. A new `EmployerScope`/`EmployerScopeResolver` pair modeled on `AgencyScopeResolver`'s explicit
   `canSeeX(scope, id)` gate pattern, resolving off `Person ↔ Employee ↔ Employer`.
3. A wired-up participant role — either implementing the inert role 4 or minting a new id — plus
   branches in `AuthDAO.assignUserRoles` / `AuthenticateUser` / `OneTimeUserLogin`.
4. All employer-scoped employee-facing DAO queries written fresh; no existing participant
   self-service read path exists to adapt.
5. Standing rule: employee-facing code must never read `AmsDataGlobal`'s unfiltered collections.
   `AmsDataGlobal` is inherently ServletContext-wide and cannot be scoped down.

**Note the underlying trust assumption:** the unscoped caching works today only because every current
session type (PSP staff, agents, agency admins) is trusted at roughly PSP/agency level. **An
individual employee would be the first genuinely low-trust session in AMS.**

## Q3 — Invitation pattern and the PHI boundary

**Verdict: net-new entity required. Reuse the lifecycle pattern, not the entity.**

- `Invitation` (`model/sales/agency/Invitation.java`), `AcceptInvite`
  (`controller/activity/setup/AcceptInvite.java`).
- GUID = `UUID.randomUUID().toString()` (`SendInvitation.java:128`), 30-day expiry (line 129),
  checked on GET and POST (`AcceptInvite.java:51, 77-82`), single-use via `isUsed` (checked 49/77,
  set 179/301). Replay blocked, not silent.
- **`agency` FK is `nullable=false`** (`Invitation.java:31-33`). `role` is an unconstrained
  `varchar(20)` String, only meaningfully compared against literal `"AGENCY_MANAGER"`.
  `AcceptInvite.doPost` performs hard-coded Agency side effects (adds to `Agency.agentList`, sets
  `Agency.manager`) with no employer/employee analog. **Structurally agent-specific.**
- **The closer analogue is the `User` GUID mechanism**: `tempGuid` / `guidExpiration` / `guidUsed` /
  `allowSetPassword` (`User.java:31-41`), used for existing-account password reset and one-time login
  via `HelpUserLogin.java` → `OneTimeUserLogin.java` → `ResetLogin.java`. Two modes set in
  `HelpUserLogin.updateUserData()` (142-157): One-Time Login (`allowSetPassword=false`, 10-min
  expiry) and Password Reset (`allowSetPassword=true`, 7-day expiry). This "prove identity via GUID →
  then set password" shape matches what census-gated employee provisioning needs.

### Unauthenticated GUID/token surfaces — complete list

`AcceptInvite`, `ViewProposal` (`/proposal/*`), `ApplyForProposal` + `SaveApplicationProgress`
(`/apply/*`, `/saveApplication`), `UploadRateSheet`, `FillQuestionnaire` +
`SaveQuestionnaireProgress` (`/q/*`, `/saveQuestionnaire`), `QuestionnaireWebhookApi`,
`EmployerBillingDetail`, `ShowFileUpload`, `RequestQuote` (`?k=`), `OneTimeUserLogin`,
`HelpUserLogin`, `ResetLogin`, `CreateBpoTestUser`, `ServeVideo` (`/video*`), `LegacyTpoRedirect`
(`/tpo`), Outlook static assets (`/outlook/*` — Bearer token, different mechanism).

**None currently expose confirmed PHI.** See `docs/analysis/security_findings_2026-07-28.md` for the
structural risks among them.

**Design rule that follows:** the PHI-bearing enrollment payload must **never** be built as another
config-driven field on the `ApplicationField` / `QuestionnaireField` unauthenticated-GUID pattern.
Provisioning may use a GUID; the application itself must sit behind an authenticated session.

## Q4 — ICHRA/QSEHRA through the sales pipeline

**Verdict: this is a reference-data problem, not an architecture problem.**

- **No seeding code anywhere creates ICHRA, EBHRA, or QSEHRA `LOS` rows.** See the accuracy warning
  in `entity_reference.md`.
- Chain confirmed live: `Agency —(M:N)→ Rate —(1:M)→ RateTable —(M:1)→ ServiceModule ←(M:N)— LOS`;
  `Proposal —(M:1)→ Rate`, `—(M:N)→ LOS (losList)`; `Application`'s PK **is** `Proposal.proposal_id`
  (shared `@OneToOne @Id`); `Application → ApplicationModule` (1:M).
- ToDo generation: `ApplicationModule → ServiceItem → RequiredTaskList → TaskSequenceTable → Task`,
  confirmed in `ApplicationTaskDAO.java`, driven dynamically per-LOS in the customer-facing flow
  `ApplyForProposal.java:444-448` via `ActivityDAO.addModule()`. `SetupPromotionService.java` runs
  only *after* Setup/ToDos exist and is not part of ToDo generation.
- **Zero ICHRA/EBHRA/QSEHRA-specific logic exists anywhere in the live pipeline.** All grep hits are
  descriptive text, a false-positive regex match (`whichRadio`), or dead code.
- **T9 confirmed still open.** `GenerateProp25.java` hardcodes `q1`-`q8` to 6 fixed LOS ids (5-10)
  and 8 fixed ServiceItem ids (11-19) via literal `.equals("1")` checks, with no read of
  `Proposal.getLosList()`. `q3`/`wHra` is a single undifferentiated "HRA" checkbox — no HRA-variant
  differentiation exists. **Any ICHRA/QSEHRA selection through this internal Manual Setup tool would
  be silently dropped.** Scoped to `GenerateProp25` only — `ApplyForProposal.java` and
  `CreateSetup25.java` are fully dynamic.

### Where it stops

1. No live LOS row for ICHRA/EBHRA/QSEHRA (data gap; the M:N join and `Proposal.addLos()` are generic).
2. No dedicated `ServiceItem` — even the dead-code ICHRA/EBHRA PlanTypes point at the **generic HRA**
   ServiceItem (id 5). A Setup born from an ICHRA sale today would get the plain HRA checklist with
   **zero ICHRA-specific compliance steps**.
3. No RequiredTaskList/TaskSequenceTable/Task is ICHRA/EBHRA/QSEHRA-specific.
4. T9 would silently drop the selection in the Manual Setup path.
5. No QSEHRA PlanType exists at all. ICHRA/EBHRA PlanTypes exist in dead code and would need wiring —
   a small fix, whereas QSEHRA needs new code.

**Unresolved and blocking Phase B scoping:** whether any live database (production/demo/BPO/master)
already contains manually-entered ICHRA/EBHRA/QSEHRA `LOS`, `ServiceItem`, `PlanType`, or
task-sequence rows. Static analysis cannot see admin-UI-entered data. **This must be answered by
querying the databases directly.**

## Q5 — Employer-scoped landing surface

**Verdict: extension in pattern, new construct in implementation.**

- `OriginatingAgencyResolver.java` resolves an **agency** by object-graph walk:
  `proposal.sourceActivity.assignedTo` → `proposal.prospect.agent` → `proposal.createdBy` (lines
  90-108), falling back to `null`. **No host/token logic** — that lives in `AmsDataGlobal` /
  `RequestQuote`.
- `agency.quote_token` (V071): `UUID.randomUUID()` (`AgencyAction.java:458`, case `"mintQuoteToken"`),
  unique-indexed, looked up via `SalesDAO.getAgencyByQuoteToken` (`SalesDAO.java:219-230`),
  public-facing in `RequestQuote?k=<token>` URLs (`RequestQuote.java:94-99, 315-328`).
  **Attribution-only — explicitly does not drive branding** (`RequestQuote.java:287-293`).
- Landing system: V043 added one PSP-wide `constant.text_value TEXT` column. V068 added
  `agency.landing_host` / `agency.landing_html`; **non-blank `landing_html` is itself the enable
  switch**. Routing in `login.java:57-83` (`routeLogin()`): agency host match > PSP-wide custom
  landing > default login. Sanitization via `LandingSafe.clean()` (`AgencyAction.java:504`).
- **Definitive NO on Employer scoping.** All eight files touching landing/host machinery are Agency-
  or PSP-scoped; zero reference `Employer`.
- **`Employer` has no public-safe identifier of any kind** — no branding field, no slug, no token, no
  URL-safe public id. `altId` is **Summit's own employer ID**, set during import
  (`UniversalImportService.java:551, 590`), consumed by `SummitEmployerResolver` and
  `AbstractSummitEmployerRedirect` to build **internal, authenticated** deep links into Summit. It is
  never validated or documented as public-facing. The 2026-07-17 guard fix (commit `1494286`) was a
  null-safety UX fix, **not** security hardening — `altId` was never contemplated as a public
  identifier.

Building an employer-scoped landing page requires new migrations, new `AmsDataGlobal` cache logic, a
new `login.java` resolution branch, and a newly-minted public-safe Employer identifier. **Sized like
the original V068 effort, not a config addition.**

## Cross-cutting

- **Q1's billing gap and Q4's checklist gap share one root cause:** AMS's monthly billing and
  checklist generation both assume everything traces back to an `Enrollment2`/`ImportEmployee`
  Summit-staging row. An ICHRA/QSEHRA-native employee breaks that assumption in two independent
  subsystems.
- The negative-ID Employee convention and the `AgencyScopeResolver` work are both deliberate,
  documented patterns worth imitating stylistically (explicit resolver classes with documented
  fallback order).

## Open questions for Kevin (from the investigation)

1. Should ICHRA/QSEHRA-native employees be billed through the standard Summit-driven monthly
   pipeline, or is a separate reimbursement rail intended? (Backlog #38 notes no reimbursement
   payment rail exists in AMS today.) This determines whether Q1's billing gap is in scope.
2. Implement the inert `UserRole` id 4 ("Applicant"), or mint a fresh role id?
3. What identifier carries an employer-scoped public landing page — slug, token, or host (V071 vs
   V068 precedent)?
4. Is there appetite to remediate the existing PHI-adjacent surfaces in `ApplyForProposal` /
   `FillQuestionnaire` independent of this feature? (See security findings doc.)

## Items flagged but not fully verified

- `EmployerBillingDetail?uid=` and `ShowFileUpload?doc=` — identified as risks, target entities' full
  field lists not read. Need a follow-up pass.
- Whether live databases contain manually-entered ICHRA/QSEHRA reference data (above).

---

## 2026-07-29 — PREMISE CHANGED: findings still valid, stakes reduced

This investigation was scoped against an architecture in which **AMS collects the enrollment
application, including SSN/ITIN, immigration status, incarceration status, and disability
attestations.**

On 2026-07-29 it was established that the correct target product — the HealthSherpa **ICHRA Partner
API** (`docs.ichra.healthsherpa.com`), not HSOne — uses an **Application Deeplink** model:
`POST /ichra/off_ex` returns HTTP 302 with a `Location` header, AMS redirects the browser, and **the
applicant completes the application on HealthSherpa.** See `docs/business/healthsherpa.md`.

**Every factual finding in this report remains accurate.** What changes is the stakes:

| Question | Effect |
|---|---|
| **Q1** — native `Employee` creation | **Unchanged.** Census intake still requires AMS-native employee records. The `mergeNegativeToPositiveEmployees` data-loss risk (`Updater.java:622-730`) still applies. |
| **Q2** — employee-scoped login | **Lower stakes, still needed.** An employee session would carry shopping and status, not PHI. The `AmsDataLocal` installation-wide leak (lines 137-141) is still disqualifying for reuse. |
| **Q3** — PHI boundary | **Substantially reduced.** The hardest problem — an authenticated PHI-bearing form — largely dissolves. **The security findings stand entirely on their own merits** and are unaffected. |
| **Q4** — sales pipeline | **Unchanged.** Still a reference-data gap. |
| **Q5** — employer-scoped landing | **Unchanged.** |

**Two things that would reverse this:**

1. **Choosing EnrollConnect over the Deeplink.** EnrollConnect requires AMS to collect and submit
   everything, including carrier-specific attestation text (returned via
   `include=enrollment_requirements`) and electronic signature consent. **That reinstates the full Q3
   problem.**
2. **The deeplink turning out to be agent-driven rather than employee-self-service.** The Use Cases
   documentation describes redirecting *agents* to complete each employee's enrollment. If an agent
   completes each application, Q2's employee role may not be needed at all — the build becomes an
   agent workstation instead. **Unresolved as of this writing.**

**New requirement not covered by this investigation:** inbound webhook ingestion. The ICHRA Partner API
**pushes** Submission Confirmation and Policy Status events to a public authenticated HTTPS endpoint —
it is not a polling model. The existing `/api/*` prefix with `ApiTokenFilter` is the natural home, but
**no Phase A work has been done on it.**
