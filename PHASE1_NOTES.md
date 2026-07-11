# Phase 1 — AgencyScopeResolver

Introduces `net.superiorstate.ams.data.resolver.AgencyScopeResolver` (+ its `AgencyScope`
value object) and retires four of the five duplicated "find my agency" resolvers
inventoried in [AGENCY_STRUCTURE_AUDIT.md](AGENCY_STRUCTURE_AUDIT.md) §2.1. No migrations,
no schema changes, no hierarchy (`parent_agency_id`) walking — that's later phases.

## What changed

**New files:**
- `src/main/java/net/superiorstate/ams/data/resolver/AgencyScope.java` — immutable
  record: `pspWide`, `pspId`, `primaryAgencyId`, `rollupAgencyIds`, `detailAgencyIds`.
  Rollup and detail are always identical in Phase 1; the split is scaffolding for a later
  phase (a General Agent seeing its downline's opportunities as rollup cards without being
  able to open their records) and is not yet consumed differently anywhere.
- `src/main/java/net/superiorstate/ams/data/resolver/AgencyScopeResolver.java` — the
  resolver. Two entry points: `resolve(EntityManager, HttpServletRequest)` (reads role
  flags + `AmsDataLocal.currentPerson` off the session, matching how every refactored
  call site already worked) and an explicit-parameter overload for callers that already
  have the flags/Person in hand. Also exposes `primaryAgencyEntity(EntityManager,
  AgencyScope)` — the "give me the full `Agency` entity, not just the id" convenience the
  task asked for; it lives on the resolver rather than on the record itself because
  fetching the entity needs an `EntityManager`, which an immutable value object shouldn't
  hold.

**Refactored (old `findAgencyForUser`/`belongsToAgency` methods deleted, replaced with resolver calls):**
- [AgentHome.java](src/main/java/net/superiorstate/ams/controller/activity/setup/AgentHome.java) — `loadData()` now calls `AgencyScopeResolver.resolve(em, request)` + `primaryAgencyEntity(...)` instead of its own `findAgencyForUser()` (deleted).
- [CreateUser25.java](src/main/java/net/superiorstate/ams/controller/user/CreateUser25.java) — the `"agencyAdmin"` creator-type branch does the same; its own `findAgencyForUser()` (deleted).
- [ProposalBuilder.java](src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalBuilder.java) — both call sites (the `isAgent||isAgencyAdmin` branch and the `isPspAdmin` branch) now go through the resolver; its own `findAgencyForUser()` (deleted).
- [OpportunityAuthz.java](src/main/java/net/superiorstate/ams/data/util/OpportunityAuthz.java) — `belongsToAgency()` kept as a private helper but reimplemented as `scope.detailAgencyIds().contains(agencyId)` instead of its own membership query.

**Design decision not explicit in the task spec, called out here:** the literal resolution
rules only assign `primaryAgencyId` for the Agency Admin and Plain Agent buckets. But
`ProposalBuilder`'s PSP-admin branch (line ~123 pre-refactor) needs to know whether *this
specific PSP staffer* also happens to personally belong to an agency (for defaulting the
prospect list) — a question that's independent of their PSP-wide authorization. So
`primaryAgencyId` is populated via the same deterministic tie-break for the **PSP-staff
bucket too**, not left null. `rollupAgencyIds`/`detailAgencyIds` for PSP staff remain empty
sets (pspWide already implies "everything in the tenant," so per-agency id sets aren't
meaningful there). This only affects the resolver's internal population of `primaryAgencyId`
for the pspWide case — it does not touch the pspWide/rollup/detail semantics as spec'd.

## Behavior-change table

| Call site | Verdict | Detail |
|---|---|---|
| **AgentHome.findAgencyForUser()** → `AgencyScopeResolver` | **(b) Behavior change — narrow edge case, no known live trigger** | `AgentHome.doGet`/`doPost` have **no role gate at all** (confirmed — any authenticated session can hit `/AgentHome`). The old `findAgencyForUser` ran its manager-then-membership tie-break for *any* Person regardless of role. The new resolver's "anyone else" bucket (session has none of `isPspAdmin`/`isPspUser`/`isPspSales`/`isAgencyAdmin`/`isAgent` set) returns `primaryAgencyId = null` unconditionally, even if that person is still literally listed in some `agency.agentList` or as an `agency.manager_id`. Trigger: a person whose sales role was later revoked (so no gating flag is set at login) but who was never removed from the agency's `agents` join table or as `manager_id` — they'd now see an empty pipeline instead of their stale agency's data. Checked every `addAgent()`/`setManager()` call site in the codebase (`ReferenceDataSeeder`, `UserManager`, `CreateUser25`, `AcceptInvite`, `AgencyAction`, `GenerateProp`/`GenerateProp25`) — all of them only ever add Agent/Agency-Admin-role people, so there's no code path today that creates this stale state. Flagging it because the divergence is real, not because it's reachable today. |
| **CreateUser25.findAgencyForUser()** → `AgencyScopeResolver` | **(a) Exactly equivalent** | The only call site is gated by `if (!isAgencyAdmin) return "Only Agency Admins can create agents.";` immediately above, so the resolver always executes in the Agency Admin bucket, which runs the identical tie-break the old method ran. |
| **ProposalBuilder.findAgencyForUser()** → `AgencyScopeResolver` (both call sites) | **(a) Exactly equivalent** | Call site 1 is gated `if (isAgent \|\| isAgencyAdmin)` → Agency Admin or Plain Agent bucket, same tie-break. Call site 2 runs unconditionally inside `if (isPspAdmin)` → PSP-staff bucket, which (per the design decision above) still runs the same tie-break for `primaryAgencyId`. A session with more than one flag true (e.g. `isPspAdmin && isAgent`) doesn't change the outcome either — every bucket that can fire here computes `primaryAgencyId` via the identical query, so which bucket "wins" is irrelevant to the value returned. |
| **OpportunityAuthz.belongsToAgency()** → `AgencyScopeResolver` | **(b) Behavior change — real, not just theoretical** | Old: `a.manager.id = :personId OR ag.id = :personId` for the *specific target* `opp.getAgency().getId()` — i.e. "is this person a manager or **any** member of *that* agency." New: `scope.detailAgencyIds().contains(agencyId)`, and for the Agency Admin bucket `detailAgencyIds` is the **singleton** `{primaryAgencyId}` (the one tie-broken agency), not every agency the person is a member of. **Trigger:** an Agency Admin who manages Agency A (so `primaryAgencyId` tie-breaks to A via the manager-match rule) but is also a plain member of Agency B loses access to Agency B's opportunities through this check — previously granted, now denied. This is the most consequential divergence in this phase; it's a real narrowing of access, not a widening, so it fails closed rather than open, but it's still a change worth a deliberate call before merging. Confirmed live consumers: `UpdateOpportunityStage.java` and `GoActivityDetail25.java` both call `OpportunityAuthz.canAccessOpportunity(...)`, which is what invokes this. |

## OriginatingAgencyResolver.agencyOf() — inventoried, not refactored

`OriginatingAgencyResolver.agencyOf(Person)` (`OriginatingAgencyResolver.java:126-131`)
serves a **genuinely different purpose** and should **not** fold into
`AgencyScopeResolver`:

- It answers "which agency **originated this specific sale**" (a Proposal/Setup), via a
  three-step waterfall (source Opportunity's `assignedTo` → prospect's `agent` →
  proposal's `createdBy`) — a question about a *sale record*, not about "what can the
  current session user see." `AgencyScopeResolver` has no concept of a Proposal/Setup at
  all.
- It operates on an **arbitrary `Person`** passed in by the caller (frequently *not* the
  current session user — e.g. resolving the outside agent who sold a proposal that a PSP
  staffer is now viewing), and is called from contexts with no `HttpServletRequest`/session
  at all (email-identity resolution for outbound mail). `AgencyScopeResolver.resolve()`
  fundamentally needs a session (or explicit role flags) to pick a bucket; there's no
  session to read in those callers.
- Its inner `agencyOf(Person)` step (`list.get(0)` on `person.getListOfAgenciesWithThisAgent()`)
  is doing the same underlying job as `AgencyScopeResolver`'s private tie-break, but with a
  **weaker** rule — first element of an already-loaded in-memory list (JPA collection
  order, not guaranteed meaningful) rather than "manager match, else lowest agency_id."
  Swapping it to reuse the resolver's tie-break would require a DB query where today it's
  a zero-cost in-memory read, and would silently change `OriginatingAgencyResolver`'s
  output for any multi-agency person — out of scope for a "do not refactor" inventory
  item, and arguably its own follow-up phase if the codebase wants exactly one
  person→agency tie-break rule instead of two.

**Recommendation:** leave `OriginatingAgencyResolver` as-is. If a future phase wants a
single canonical tie-break rule everywhere, extract `AgencyScopeResolver`'s
`resolvePrimaryAgencyId(EntityManager, Long)` into a small shared utility that both
resolvers call — but that's a deliberate, separately-reviewed behavior change for
`OriginatingAgencyResolver`'s callers (markup gating, agent-delegation eligibility,
email-identity resolution), not a Phase 1 concern.

## Acceptance

- `mvn clean package` — **BUILD SUCCESS** (471 source files, `ams-1.0.0-SNAPSHOT.war` packaged).
- No `.sql`/migration files touched.
- No JSP files touched — all four refactors are Java-only method-body swaps; JSP-visible
  attributes (`agency`, `userAgency`, `agentList`, etc.) keep the same names and same
  entity type.
