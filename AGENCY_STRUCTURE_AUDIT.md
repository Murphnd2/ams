# Agency / Agent / Agency-Manager Structure Audit

Read-only audit of the current codebase on branch `refactor/modernize-architecture`, including six files with uncommitted working-tree changes (`AgencyAction.java`, `PspAgencyHome.java`, `GenerateProp25.java`, `agencyManager25.jsp`, `proposalDetail.jsp`, `reviewApplications.jsp`). Those six uncommitted changes implement an **unrelated "agency manager reassignment guard" feature** (blocks removing an agency's designated manager without promoting a replacement) plus a PSP-staff access gate on `GenerateProp25` — they do **not** touch the parent-agency/GA hierarchy, which is separate, already-committed code from V070/V071.

---

## 1. SCHEMA

### 1.1 `agency` table

Baseline: `docs/importscript/beta_ssa_dev_baseline_thru_V024.sql:627-645`

```sql
CREATE TABLE `agency` (
  `agency_id` bigint NOT NULL,
  `agency_name` varchar(200) DEFAULT NULL,
  `phone` varchar(12) DEFAULT NULL,
  `tax_id` varchar(10) DEFAULT NULL,
  `psp_id` bigint DEFAULT NULL,
  `address_id` bigint DEFAULT NULL,
  `contact_id` bigint DEFAULT NULL,
  `manager_id` bigint DEFAULT NULL,
  PRIMARY KEY (`agency_id`),
  CONSTRAINT `FK_AGENCY_address_id` FOREIGN KEY (`address_id`) REFERENCES `address` (`address_id`),
  CONSTRAINT `FK_AGENCY_contact_id` FOREIGN KEY (`contact_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `fk_agency_manager` FOREIGN KEY (`manager_id`) REFERENCES `assignee` (`id`),
  CONSTRAINT `FK_AGENCY_psp_id` FOREIGN KEY (`psp_id`) REFERENCES `assignee` (`id`)
)
```

Every subsequent `ALTER TABLE agency` (confirmed exhaustive over `docs/migrations/`):

| Migration | File | Change |
|---|---|---|
| V057 | `docs/migrations/V057__agency_suppressed.sql:5` | `+ suppressed TINYINT(1) NOT NULL DEFAULT 0` |
| V067 | `docs/migrations/V067__agency_markup_enabled.sql:23` | `+ markup_enabled TINYINT(1) NOT NULL DEFAULT 0` |
| V068 | `docs/migrations/V068__agency_landing_host.sql:30-35` | `+ landing_host VARCHAR(255)` (unique), `+ landing_html MEDIUMTEXT` |
| V069 | `docs/migrations/V069__agency_email_sending.sql:35-40` | `+ email_domain VARCHAR(255)` (unique), `+ email_verified TINYINT(1)` |
| **V070** | `docs/migrations/V070__agency_parent.sql:22-23` | `+ parent_agency_id BIGINT NULL` + plain index `ix_agency_parent` |
| **V071** | `docs/migrations/V071__agency_quote_token.sql:23-24` | `+ quote_token VARCHAR(64)` (unique) |

**Current full column list:** `agency_id` (PK), `agency_name`, `phone`, `tax_id`, `psp_id` (FK→`assignee.id`), `address_id` (FK→`address`), `contact_id` (FK→`assignee.id`), `manager_id` (FK→`assignee.id`), `suppressed`, `markup_enabled`, `landing_host` (unique), `landing_html`, `email_domain` (unique), `email_verified`, `parent_agency_id`, `quote_token` (unique).

**Does `agency` have a parent_agency_id? YES — confirmed.**
- Added V070 (`docs/migrations/V070__agency_parent.sql:22-23`), applied 2026-07-10.
- **Nullable, self-referencing, and deliberately has NO `FOREIGN KEY` constraint** — only a plain non-unique index `ix_agency_parent`. The migration's own comment states this matches the idiom of prior column-only agency migrations (V067-V069).
- **No CHECK constraint or trigger limiting nesting depth exists in SQL.** The "strict two-level guardrails" from commit `0697efe` are enforced **entirely in application code**, not the database — see §2.4 below. A raw `UPDATE agency SET parent_agency_id = ...` from a DB client could freely build a 3+ level chain or a cycle; nothing in the schema prevents it.

**JPA mapping** — `src/main/java/net/superiorstate/ams/model/sales/agency/Agency.java:72-74`:
```java
@ManyToOne @JoinColumn(name = "parent_agency_id")
private Agency parentAgency;
```
There is **no inverse `@OneToMany<Agency> children`** field on `Agency` — the entity model has no way to walk parent→children via JPA navigation; every "find my children" operation is a hand-written JPQL query (see §2).

Other Agency entity fields, for completeness: `quoteToken` (V071), `markupEnabled` (V067), `landingHost`/`landingHtml` (V068), `emailDomain`/`emailVerified` (V069), `suppressed` (V057), `manager` (`@OneToOne` `manager_id`→Person), `agentList` (`@ManyToMany` via `agents` join table — §1.4), `agencyRateList` (`@ManyToMany` via `agencyrates`), `psp` (`@ManyToOne`), `address`/`primaryContact` (`@OneToOne`).

### 1.2 GA (General Agent) concept: schema or UI-only?

**It is a real schema field (`parent_agency_id`) plus write-side validation, but there is no distinct "GA" entity, role, or type discriminator anywhere.** "GA" is purely a semantic label applied to any `Agency` row whose `parent_agency_id IS NULL` and which has at least one child — it is not a separate table, not a boolean flag, not a role. Any `agency` row can be a GA (if it has children) or a sub-agency (if `parent_agency_id` is set) or neither (top-level, no children) — the schema treats these uniformly as `Agency`.

### 1.3 `assignee` table (Person / Agent / User) — SINGLE_TABLE inheritance

`src/main/java/net/superiorstate/ams/model/general/Assignee.java:7-8`:
```java
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public abstract class Assignee { ... }
```
Discriminator column `DTYPE varchar(31)`. `Person`, `Activity` (and its subtypes `Opportunity`, `Setup`), and `CheckList` all live in **one physical table**, distinguished by `DTYPE`.

Full `assignee` column list — `docs/importscript/beta_ssa_dev_baseline_thru_V024.sql:844-922`:
```
id (PK), DTYPE, full_name, TAXID, address_id, contact_id, email, first_name, last_name,
middle_init, phone, title, psp_id, setup_id, employee_id, date_completed, date_created,
due_date, is_complete, assigned_to_id, completed_by_id, created_by_id, employer_id,
checklist_id, proposal_id, person_id, description, method_id, recurring_list_id,
email_address, myRsc, primary_contact, prospect_id, agency_id_opp, opportunity_stage,
estimated_employees, estimated_value, expected_close_date, managed_by_id,
ticket_service_item_id
```
Relevant FKs: `agency_id_opp`→`agency.agency_id`, `prospect_id`→`prospect.prospect_id`, `managed_by_id`→`assignee.id` (self-referencing — Opportunity's manager), `assigned_to_id`→`assignee.id` (self-referencing — Opportunity's assigned agent).

**Does the user/agent table carry a role/is_manager/permission column? NO — not on `assignee` or `Person` directly.** Instead:

- **`user` table** (baseline `...sql:4570-4585`) — PK/FK `person_id` (`@Id @OneToOne` to `assignee.id`), plus `email`, `password_hash`, `salt`, `user_name`, `verified_email`, `allow_set_password`, `guid_expires`, `guid_used`, `temp_guid`; `is_active` added later (`docs/migrations/V029__user_is_active.sql`).
- **Role is a separate many-to-many**: `userrole` (id, description) ↔ `userinroles(person_id, role_id)` join table ↔ `User.userRoleList` (`User.java:46-49`, `@ManyToMany @JoinTable(name="userinroles", ...)`).

**Distinct role values actually used in code** — `AuthDAO.assignUserRoles()`, `src/main/java/net/superiorstate/ams/data/dao/AuthDAO.java:92-125`:

| ID | Session flag | DB seed description (`DatabaseInitializer.java`) |
|---|---|---|
| 1 | `isPspUser` | "PSP User" |
| 2 | `isAgent` | "Agent" |
| 3 | `isClient` | "Client" |
| 4 | `isApplicant` | "Applicant" |
| 5 | `isPspAdmin` | "PSP Admin" |
| 8 | `isAgencyAdmin` | "Agency Admin" |
| 9 | `isPspSales` | **"PSP Super User"** (label/functional-name mismatch — DB description string says "Super User" but the app code treats id 9 as `isPspSales`; cosmetic drift only, the numeric ID is what's authoritative) |
| 101 | `isBpo` | (referenced as "BPO Users" in `AuthenticateUser.java:111-113`; not in the two `DatabaseInitializer` seed blocks) |
| 102 | `isBpoAdmin` | "BPO Admin" |
| 103 | `isBpoUser` | "BPO User" |

Confirms the memory's role table is accurate at the ID level; adds one previously-undocumented ID (101, generic BPO flag) and flags the id-9 description-string drift.

**There is no "is_manager" column.** "Manager" is expressed three different, inconsistent ways at the application layer (see §2.3).

### 1.4 Person ↔ Agency link

Join table `agents(person_id, agency_id)`, composite PK, no surrogate ID — baseline `...sql:672-679`.

```java
// Agency.java:67-70 (owning side)
@ManyToMany
@JoinTable(name="agents", joinColumns=@JoinColumn(name="agency_id"), inverseJoinColumns=@JoinColumn(name="person_id"))
List<Person> agentList;

// Person.java:28-29 (inverse side)
@ManyToMany(mappedBy = "agentList")
List<Agency> listOfAgenciesWithThisAgent;
```

The schema is genuinely many-to-many — a `Person` can belong to more than one `Agency` — but nearly every business-logic query in the codebase assumes a *single* agency per person and takes only the first match (see §2.1).

`Agency.manager` (`Agency.java:58-60`, `@OneToOne manager_id`→`Person`) is a **separate** single-manager pointer, independent of `agentList` membership.

### 1.5 Sales-pipeline tables

| Concept | Table | Notes |
|---|---|---|
| **Prospect** | `prospect` (standalone) | `docs/importscript/...sql:3129-3142`: `prospect_id` (PK), `name`, `agent_id`→`assignee.id`, `address_id`→`address`, `contact_id`→`assignee.id`. Entity `model/sales/agency/Prospect.java`. |
| **Opportunity** ("stage") | *no standalone table* — a `DTYPE`-discriminated row in `assignee` (extends `Activity`) | Columns: `prospect_id`, `agency_id_opp` (FK→`agency`), `checklist_id`, `opportunity_stage` (free-text `varchar(30) DEFAULT 'NEW'`, **no enum/lookup table** — any string is legal), `estimated_employees`, `estimated_value`, `expected_close_date`, `managed_by_id` (self-FK→`assignee`), `assigned_to_id` (self-FK→`assignee`). Entity `model/activity/Opportunity.java`. |
| **Proposal** | `proposal` (standalone) | `docs/importscript/...sql:3084-3102`: `proposal_id` (PK), `application_guid`, `prospect_id`→`prospect`, `rate_id`→`rate`, `status`, `created_by`, `date_created/sent/viewed/applied`, `source_activity_id`. Join table `proposalitems(los_id, proposal_id)`. Entity `model/sales/agency/Proposal.java`. |
| **Application** | `application` (standalone, 1:1 extension of Proposal) | `docs/importscript/...sql:689-699`: PK = `proposal_id` (also FK→`proposal.proposal_id`), `status`, `date_started/submitted/reviewed`, `reviewed_by`→`assignee` (added V041), `review_notes` (V041), `selected_los_ids`/`selected_enhancement_ids` (V045, comma-separated). Entity `model/sales/application/Application.java`. |
| **Setup** | *no standalone table* — a `DTYPE`-discriminated row in `assignee` (extends `Activity`) | Columns: `checklist_id`→`assignee` (CheckList, itself a `DTYPE` row), `proposal_id`→`application.proposal_id`, `person_id`→`assignee` (primary contact), `myRsc`. Join table `setupcontacts(setup_id, person_id)`. Entity `model/activity/ticket/setup/Setup.java`. |
| **Task/ToDo** | `todo` (standalone) | `docs/importscript/...sql:4409-4435`: `todo_id` (PK), `checklist_id`→`assignee`, `task_id`→`task`, `completed_by_id`→`assignee`, `sort_order`, `is_complete`, BPO fields (`bpo_completed*`, `bpo_assigned_to_id`), `todo_guid` (unique), `is_reverted`. Ownership-override fields `override_ownership`/`has_owner`/`owner_id`/`allow_non_owner` added by V061. Notes in a sibling `todo_note` table. |
| **DelegatedToDo (BPO cross-tenant)** | separate entity/table, `model/activity/checklist/tasks/DelegatedToDo.java` | Fields include `pspClient`, `assignedTo`, `status`, `dueDate`, `sortOrder`, `recurringSeriesId`, `sourceTaskId` — used exclusively by the BPO console (`BpoHome.java`), **not** by the agent-facing "My Setups"/"My Tasks" screens, which instead use `ToDo.overrideOwnership`. These are two distinct delegation mechanisms; do not conflate them. |

No self-referencing FK exists on `prospect`, `proposal`, or `application`. The only self-referencing FKs in the whole pipeline are: `agency.parent_agency_id`→`agency.agency_id` (new, V070, no hard constraint), `assignee.managed_by_id`→`assignee.id`, `assignee.assigned_to_id`→`assignee.id` (both pre-existing, Opportunity fields).

---

## 2. AUTHORIZATION

### 2.1 "Which agency am I" resolution — no single mechanism; at least 4 independent implementations

There is **no** JWT/token-embedded agency claim, and **no** single `AmsDataLocal` method that all code calls to resolve "my agency." Instead:

- **At login only, once, cached for the session's lifetime:**
  - `AuthenticateUser.loadSessionData25()` (`src/main/java/net/superiorstate/ams/controller/authentication/AuthenticateUser.java:91-109`) loads the `Person` and stores it directly as `session.setAttribute("currentPerson", p)` (line 93) — a **detached JPA entity**, never re-fetched unless a servlet explicitly re-queries.
  - `AuthDAO.assignUserRoles()` (`AuthDAO.java:92-125`) sets 10 boolean session attributes (`isAgent`, `isAgencyAdmin`, `isPspAdmin`, etc. — one per role). **No `agencyId`/`Agency` object is cached at login.** These flags are never re-verified mid-session (no filter revalidates roles on subsequent requests — see 2.3).
  - `AmsDataLocal.intializeLocalData()` (`src/main/java/net/superiorstate/ams/data/AmsDataLocal.java:89-150`) calls `OriginatingAgencyResolver.resolve(p)` once and caches only the **agency name string** (`currentAgencyName`, line 78) — used for navbar branding only, not for authorization.

- **Per-request, re-queried fresh by individual servlets — with 4 independently-written, inconsistent copies of "find my agency":**
  1. `AgentHome.findAgencyForUser()` (`src/main/java/net/superiorstate/ams/controller/activity/setup/AgentHome.java:155-172`): tries `Agency.manager.id = :userId` first, else falls back to `JOIN a.agentList ag WHERE ag.id = :userId` (`getResultList().get(0)`).
  2. `CreateUser25.findAgencyForUser()` (`src/main/java/net/superiorstate/ams/controller/user/CreateUser25.java:278-293`) — near-identical duplicate of #1.
  3. `ProposalBuilder.findAgencyForUser()` (`src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java:394-408`) — a **behaviorally different** reimplementation: uses `.getSingleResult()` instead of `getResultList().get(0)` on the membership query, so a person belonging to 2+ agencies throws `NonUniqueResultException`, silently caught and returns `null` — diverges from #1/#2's "take the first."
  4. `OpportunityAuthz.belongsToAgency()` (`src/main/java/net/superiorstate/ams/data/util/OpportunityAuthz.java:91-102`) — a `COUNT(...)` boolean predicate: `a.id = :agencyId AND (a.manager.id = :personId OR ag.id = :personId)`.

- **Underlying model allows multi-agency membership** (`Person.listOfAgenciesWithThisAgent`, §1.4), but every resolver above treats "my agency" as singular and takes only the first match — `OriginatingAgencyResolver.agencyOf()` (`src/main/java/net/superiorstate/ams/data/resolver/OriginatingAgencyResolver.java:126-131`) is explicit about this in its own javadoc ("first agency membership").

### 2.2 Every place a query is agency/agent scoped

| File | Method (lines) | Predicate |
|---|---|---|
| `data/dao/SalesDAO.java` | `getAgencyProspects` (180-200) | `a.id = :agency_id`, walks `agentList` in Java |
| `data/dao/SalesDAO.java` | `getProspectsByPsp` (202-207) | `p.agent.psp.id = :psp_id` (PSP-wide, no agency) |
| `data/dao/SalesDAO.java` | `getAgencyAgents` (232-239) | `a.id = :agency_id` |
| `data/dao/SalesDAO.java` | `getAgentProspects` (247-251) | `p.agent.id = :agent_id` |
| `data/dao/SalesDAO.java` | `getProposalsByAgency` (295-304) | `p.prospect.agent.id IN (SELECT ag.id FROM Agency a JOIN a.agentList ag WHERE a.id = :agencyId)` — **single agency only, no parent/child walk** |
| `data/dao/SalesDAO.java` | `getAgencyManagerMap` (487-497) | derives "manager" from role-8 membership in `agentList` — **a third, different notion of "manager" vs. `agency.manager_id`** |
| `data/dao/SalesDAO.java` | `countChildAgencies` (121-125) | `a.parentAgency.id = :id` — used only for the write-guard, see §2.4 |
| `data/dao/SalesDAO.java` | `getClosedOpportunitiesByAgency` / `ByAgent` (539-557) | `o.agency.id = :agencyId` / `o.assignedTo.id = :agentId` |
| `controller/.../AgentHome.java` | `getOpportunitiesByAgency` (174-186) | `o.agency.id = :agencyId` |
| `controller/.../AgentHome.java` | `getOpportunitiesByAgent` (188-200) | `o.assignedTo.id = :agentId` |
| `controller/.../AgentHome.java` | `getProspectsByAgency` (240-250) | `p.agent.id IN (agency's agentList)` |
| `controller/.../AgentSetupList.java` | `buildScopedAgentIds` (91-104) | `JOIN a.agentList ag WHERE a.manager.id = :mgrId` — trusts `agency.manager_id` specifically |
| `controller/.../AgentSetupList.java` | `loadScopedSetups` (114-135) | `app.proposal.prospect.agent.id IN :ids OR EXISTS(ToDo owner IN :ids)` |
| `controller/.../ReviewApplications.java` | doGet (57-70) | `pr.agent.id = :agentId` **only when `agentOnly`** (pure Agent role, no PSP flags); otherwise **no agency/agent predicate at all** |
| `controller/.../ProposalDetail.java` | doGet (36-68) | **no scoping at all** — any authenticated user can view any proposal by ID (IDOR) |
| `controller/.../ApplicationsHome.java` | doGet (39-68) | `pr.contact.psp.id = :pspId` only — PSP-tenant-wide, gated to PSP staff |
| `controller/.../CreateProspect.java` | doPost (32-54) | `agencyId` taken directly from an unchecked request parameter — no verification it belongs to the caller |
| `controller/.../CreateOpportunity.java` | `resolveAgent`/create (67-102, 238-264) | accepts arbitrary `agencyId`/`agentId` request params with **no server-side check** tying them to the caller's own scope |
| `data/util/OpportunityAuthz.java` | `canAccessOpportunity` (34-72) | centralized predicate: PSP-staff OR self-assigned/managed OR (`isAgencyAdmin` AND agency-membership) |

**Key inconsistencies:**
1. `Opportunity` has a direct FK (`agency_id_opp`) for single-hop scoping; `Proposal`/`Prospect` have none and require a 2-3 hop membership-walk subquery — two different modeling strategies for "which agency owns this."
2. "Agency Admin gets whole-agency visibility" is implemented three different ways with three different eligibility rules: role-flag-only (`AgentHome`), strictly `agency.manager_id` (`AgentSetupList`), and "any agent in the list, when combined with the role flag" (`OpportunityAuthz`).
3. `ProposalDetail.java` and (for non-`agentOnly` sessions) `ReviewApplications.java` have **no** agency/agent scoping — real IDOR-shaped gaps, not specific to the GA work but relevant background for anyone adding hierarchy-aware scoping later.
4. `CreateProspect.java` and `CreateOpportunity.java` trust client-supplied `agencyId`/`agentId` unconditionally.

### 2.3 Existing role checks — no centralized JSP tag; two enforcement styles

- **No custom JSP tag library exists** (`WEB-INF/tags/` has no `.tld` for role checks) — all gating is either inline EL (`${sessionScope.isAgencyAdmin}`, 15 occurrences in `navbar25.jsp` alone) or a servlet-level `if (Boolean.TRUE.equals(session.getAttribute("isX")))` idiom, which the audit found in **49 controller files, 158 occurrences**.
- **One real centralized authorization module exists, but only for Opportunities**: `OpportunityAuthz.java` (`canAccessOpportunity`, lines 34-72; `canReassignManager`, lines 79-81). It is correctly wired into `UpdateOpportunityStage.java` and `GoActivityDetail25.java`, but its own javadoc's claim of also covering `AddNoteToActivity25.java` and `SendEmail25.java` is **stale** — grep confirms zero references to `OpportunityAuthz` in either file. Those two endpoints are ungated by this mechanism today.
- **"Manager" vs. "Agent" (role 8 vs. role 2) is already distinguished, but three incompatible ways:**
  1. Role-flag-only: `isAgencyAdmin` session boolean (any role-8 holder), consumed by `AgentHome`, `AgentSetupList`, `ProposalBuilder`, `OpportunityAuthz`, navbar.
  2. Entity-based: `Agency.manager` FK — the field the (uncommitted) manager-reassignment-guard feature and `AgentSetupList.buildScopedAgentIds` both trust.
  3. Derived map: `SalesDAO.getAgencyManagerMap()` defines "manager" as any role-8 holder present in `agentList` — can name a *different* person than #2 for the same agency.
- **No session/role re-validation filter** — `LoginFilter.java` (`@WebFilter("/*")`) only checks `AmsDataLocal.isAuthenticated()`; it does zero role or agency checking, leaving all authorization to the destination servlet. Role/agency session state set at login is never refreshed until re-login.

### 2.4 GA / parent-agency authorization

**Confirmed: parent-agency logic exists only as write-side CRUD validation and UI-selector population. There is no query-level authorization anywhere that expands a GA (parent) user's visibility to its sub-agencies' data.**

What exists (all pre-existing/committed, not part of the current uncommitted diff):
- `AgencyAction.java` `editAgency` (lines 167-203) enforces three rules on `Agency.parentAgency` writes:
  - **R1** (line 186): no self-parent
  - **R2** (line 199): the chosen parent must itself be top-level (`parent.getParentAgency() == null`) — this is the actual mechanism behind "strict two-level"; it prevents 3+-level chains by construction, not by a depth counter
  - **R3** (line 190): an agency that already has children cannot itself be assigned a parent (via `SalesDAO.countChildAgencies`)
- `AgencyAction.java` `updateRates` (lines 280-293): a sub-agency's selectable rates are constrained to its parent GA's rate set — a restriction, not an access grant.
- `PspAgencyHome.java` (lines 70-113): builds the `eligibleParents` dropdown (top-level agencies only) and `selectedAgencyHasChildren` flag for the Agency Manager UI's parent selector; also derives `quoteLinkBase` by falling back agency→parent→PSP host (branding only).

What is **absent**:
- No DAO method returns "children of agency X" as a list (only `countChildAgencies`, a count, used solely for the R3 write-guard) — nothing to build an `agencyId IN (...)` expanded scope from.
- **None** of the agency/agent-scoped queries inventoried in §2.2 (`getProposalsByAgency`, `getAgencyProspects`, `AgentHome.getOpportunitiesByAgency`/`getProspectsByAgency`, `AgentSetupList.buildScopedAgentIds`, `OpportunityAuthz.belongsToAgency`) join through `parentAgency` in either direction. Grepping the whole codebase for `parentAgency`/`parent_agency_id` turns up exactly 8 files: the JSP UI, `AgencyAction.java`, `PspAgencyHome.java`, `SalesDAO.countChildAgencies`, the `Agency` model, and migration/doc files — no DAO authorization use.
- **No new role or session flag exists for "this Agency Admin's agency is a GA."** `AuthDAO.assignUserRoles()` still only recognizes the fixed 1/2/3/4/5/8/9/101/102/103 set.
- `GenerateProp25.java` (recently touched, uncommitted) has zero GA-awareness — its only change is an unrelated PSP-staff gate.

**Bottom line:** if a GA manager today opens Agency Manager (`PspAgencyHome`) or the Agent Pipeline (`AgentHome`) for their own GA agency, they see only that single `agency_id`'s own direct data — proposals, prospects, opportunities sold under sub-agencies are invisible unless the viewer separately switches to each sub-agency (`PspAgencyHome` is PSP-Admin-only and does support agency switching via its selector, but that's a manual agency-by-agency switch, not an aggregated GA rollup; `AgentHome`/pipeline has no agency switcher at all).

---

## 3. THE PIPELINE SCREEN

**JSP:** `src/main/webapp/WEB-INF/view/sales/agentHome25.jsp` — kanban columns at lines 301-339, stage tokens `NEW,QUALIFIED,PROPOSAL_SENT,NEGOTIATION,ON_HOLD` (line 301).

**Controller:** `src/main/java/net/superiorstate/ams/controller/activity/setup/AgentHome.java` (`@WebServlet("/AgentHome")`, line 23), forwards to the JSP above (line 47); `pageTitle = "Agent Pipeline"` (line 48).

It is the designated landing/home page for **both** Agents and Agency Admins (`navbar25.jsp:113-122`, logo link routes to `AgentHome` when `isAgent || isAgencyAdmin`).

### Agent vs. manager rendering — genuine data-scope branch, not just a label

`AgentHome.loadData()` lines 74-80:
```java
if (isAgencyAdmin) {
    opportunities = getOpportunitiesByAgency(em, agency.getId());   // line 179: o.agency.id = :agencyId
} else {
    opportunities = getOpportunitiesByAgent(em, currentUser.getId()); // line 193: o.assignedTo.id = :agentId
}
```
The same branch repeats for the "New Opportunity" prospect picker (lines 130-136) and the manager-only agent-filter dropdown (lines 139-146, `SalesDAO.getAgencyAgents`). JSP-side, admin-only rendering differences include the agent name on kanban cards (`agentHome25.jsp:314`) and the "All Agents" filter in the closed-opportunities panel (lines 462-469).

So: managers see every `Opportunity` whose `agency_id_opp` matches their single resolved agency; agents see only opportunities where `assigned_to_id` is themselves. **This is real code-path divergence, not merely a UI label** — but it is still single-agency scoping in both branches; `agency` here is always the one `Agency` returned by `findAgencyForUser()` (lines 155-172), never a set of agencies.

### Relationship to the separate "Agency Manager" console

`agencyManager25.jsp` / `PspAgencyHome.java` is a **fully independent PSP-back-office screen**, not a variant render of the pipeline:
- Reached only from the PSP-Admin "Admin" dropdown (`navbar25.jsp:207,223`) — never linked from the Agency Admin's own navbar section, never linked from `agentHome25.jsp`.
- Grep confirms zero cross-references between `agentHome25.jsp`/`AgentHome.java` and `agencyManager25.jsp`/`PspAgencyHome.java` in either direction.
- Functionally it's a master-detail agency-roster admin page (create/select agency, assign rates, manage agent roster, set GA parent, view a flat prospect summary) — not a kanban board.

### GA-hierarchy awareness in the pipeline screen: NONE

`AgentHome.java` never references `parentAgency` anywhere (confirmed by grep). `findAgencyForUser()` always resolves exactly one `Agency`; `getOpportunitiesByAgency`/`getProspectsByAgency` both filter by a single literal `agency_id` with no subquery over children, no union with a parent GA, and no agency-switcher UI exists on `agentHome25.jsp` (the only dropdown present is the closed-opportunities "All Agents" filter, itself scoped to the single resolved agency's own roster). A GA manager's pipeline shows only their own GA's directly-attributed opportunities — sub-agency activity is invisible on this screen, full stop. This capability does not exist today.

---

## 4. ADJACENT SCREENS

| Screen | JSP | Controller | Predicate | Hardcoded or parameterized? |
|---|---|---|---|---|
| **My Setups** | `sales/agentSetupList25.jsp` | `AgentSetupList.java` | `buildScopedAgentIds()` (91-104): self only for Agent; **agency-wide** (`Agency.manager.id = :mgrId` walk) for Agency Admin. Row query `loadScopedSetups()` (114-135) filters `app.proposal.prospect.agent.id IN :ids OR delegated ToDo owner IN :ids`. | **Already parameterized by scope** — the one screen in the app that does agency-wide expansion correctly for admins. Uses `ToDo.overrideOwnership`, not the separate `DelegatedToDo` entity (that's BPO-only). |
| **My Tasks** (agent-facing) | `agentHome25.jsp` sidebar (342-360) | `AgentHome.getDelegatedToDos()` (208-238) | `td.owner.id = :agentId`, always `currentUser.getId()` | **Hardcoded to self**, even for Agency Admins — no `isAgencyAdmin` branch exists here at all, unlike the same JSP's opportunity list. Inconsistent with "My Setups" in the very same page. |
| **My Tasks** (BPO-facing, separate screen) | `bpo/bpoHome25.jsp` | `BpoHome.java`, `viewMode` param | `mine` (self, hardcoded) / `all` (unscoped) / `unassigned` (`assignedTo IS NULL`) | Parameterized via `viewMode`, but each mode's underlying query is itself simple (not agency-aware). Uses `DelegatedToDo`, a different entity from the agent-facing "My Tasks." |
| **Applications** | `sales/reviewApplications.jsp` | `ReviewApplications.java` | `agentOnly = isAgent && !isPspAdmin && !isPspUser`; if true, `pr.agent.id = :agentId` (self only); if false, **no agency/agent predicate at all** (status filter only) | **Hardcoded self-only for plain Agents; unscoped for everyone else.** No `isAgencyAdmin` carve-out exists — an Agency Admin whose only true flag is `isAgent` falls into the same self-only bucket as a plain Agent, with no agency-wide fallback. This is the least-parameterized of the four screens. |
| **Applications** (PSP-wide sibling) | `sales/applicationsHome25.jsp` | `ApplicationsHome.java` | `pr.contact.psp.id = :pspId`, PSP-staff only (gated `isPspUser \|\| isPspAdmin`) | Parameterized by PSP tenant, not by agency/agent — different scope model, worth noting but not the primary "Applications" screen agents/managers use. |
| **New Opportunity** (agent-facing modal) | `agentHome25.jsp` modal (646-726) | `CreateOpportunity.java` | Hidden `agencyId` field = `agency.getId()` from `findAgencyForUser()` (line 650); **no `agentId` field at all** | **Hardcoded** — agency is the current user's own single agency (no selector), agent defaults via `resolveAgent()`'s membership fallback (effectively self). |
| **New Opportunity** (PSP-admin modal) | `pspHome/columns/activities/addActivityModal25.jsp` | `CreateOpportunity.java` (same backend) | Explicit `<select name="agencyId">` (63-71) + conditional `<select name="agentId">` (100-108), both cascading via JS | **Parameterized** — full agency + agent selection. |

**Cross-cutting note:** `CreateOpportunity.resolveAgent()`/`createOpportunity()` (`CreateOpportunity.java:67-102, 238-264`) already accepts arbitrary `agencyId`/`agentId` request parameters with **no server-side check** tying them to the caller's permissions — the backend is scope-agnostic by construction; only the *agent-facing* JSP entry point is what currently constrains it to self/own-agency (by simply not rendering selector fields). This means enabling cross-agency opportunity creation for a broader role set is a front-end (form fields) change plus a **new** authorization check in the servlet — the servlet itself imposes none today.

---

## Gaps for recursive hierarchy

What would have to change to support real `agency -> agency -> agency (-> ...)` nesting and GA-aggregated visibility, based on everything above:

1. **Schema: depth is currently capped at 2 levels by construction (R2 in `AgencyAction.editAgency`, not by any DB constraint).** To go beyond two levels, R2 (`parent.getParentAgency() == null`) would need to be relaxed or replaced with an actual cycle/depth check (e.g. a recursive CTE or an application-side ancestor walk with a max-depth guard), since MySQL 8 supports `WITH RECURSIVE` but nothing in this codebase currently uses it. Because `parent_agency_id` has no FK constraint today, a real hierarchy also has no DB-level protection against cycles — that would need to become either a proper `FOREIGN KEY` (still allows cycles) plus an app-level cycle check, or a nested-set/closure-table structure if deep queries need to be fast.

2. **No "get all descendants/ancestors of agency X" primitive exists.** `SalesDAO.countChildAgencies` only counts direct children for the write-guard. Every scoping query in §2.2 and §4 would need a genuinely new DAO method — most naturally a recursive JPQL/native query or a closure table — to expand a single `agencyId` into "this agency and all its descendants" before it can be reused as an `IN (...)` scope.

3. **All 10+ agency/agent-scoping query sites are single-level and would each need updating**, not just one central place: `SalesDAO.getAgencyProspects/getAgencyAgents/getProposalsByAgency`, `AgentHome.getOpportunitiesByAgency/getProspectsByAgency/findAgencyForUser`, `AgentSetupList.buildScopedAgentIds`, `OpportunityAuthz.belongsToAgency`, `ReviewApplications` (would need to gain agency scoping at all first), `CreateOpportunity`/`CreateProspect` (would need real authorization added, not just parameterization). Given the existing 3-4-way inconsistency in how "manager" and "my agency" are each already resolved, hierarchy work should probably consolidate these into one shared resolver/DAO method rather than propagate a 5th variant.

4. **No role/session concept of "I manage a GA, expand my scope to children" exists.** `AuthDAO.assignUserRoles()` and the session boolean set are flat; nothing marks a session as "agency-admin-of-a-parent-with-children" vs. "agency-admin-of-a-leaf." This would need either a computed session/request attribute (e.g. `myAgencyAndDescendantIds`, computed once per request or cached similarly to `currentAgencyName`) or a query-time subquery, consistently applied everywhere in item 3.

5. **The Agent Pipeline screen (`AgentHome.java`/`agentHome25.jsp`) has zero awareness of `parentAgency`** and would need the most direct changes to satisfy "GA sees sub-agency pipeline" — both the opportunity query and the prospect/agent-filter dropdowns are single-`agency_id` today with no UI affordance for switching or aggregating across a hierarchy (contrast with `PspAgencyHome`, which at least has an agency **switcher**, though still not an aggregating rollup).

6. **`Agency` entity has no inverse `children` collection** — recursive hierarchy work would likely want to add a `@OneToMany(mappedBy = "parentAgency") List<Agency> children` for convenience, though for performance a raw recursive query/closure table is probably still needed for any "all descendants" scoping use rather than walking JPA associations node-by-node.

7. **Pre-existing IDOR-shaped gaps should be closed before or alongside hierarchy work**, since expanding scope to a whole subtree makes any existing scoping hole proportionally worse: `ProposalDetail.java` (no agency check on read at all), `ReviewApplications.java` (unscoped for any non-`agentOnly` session), `CreateProspect.java`/`CreateOpportunity.java` (trust client-supplied `agencyId`/`agentId` unconditionally). A hierarchy-aware scope would still need a genuine ownership check at these points to be meaningful.
