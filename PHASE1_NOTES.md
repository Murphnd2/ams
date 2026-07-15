# Phase 1 — AgencyScopeResolver

Introduces `net.superiorstate.ams.data.resolver.AgencyScopeResolver` (+ its `AgencyScope`
value object) and retires four of the five duplicated "find my agency" resolvers
inventoried in [AGENCY_STRUCTURE_AUDIT.md](AGENCY_STRUCTURE_AUDIT.md) §2.1. No migrations,
no schema changes, no hierarchy (`parent_agency_id`) walking — that's later phases.

This document covers both the initial Phase 1 cut and the **Phase 1b correction** (see
below) that fixed a spec error in how the initial cut derived the Agency Admin bucket's
scope sets. The behavior-change table reflects the corrected, current state.

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
- [OpportunityAuthz.java](src/main/java/net/superiorstate/ams/data/util/OpportunityAuthz.java) — `belongsToAgency()` kept as a private helper but reimplemented as `AgencyScopeResolver.canSeeDetail(scope, agencyId)` instead of its own membership query (see Phase 1b — this was `scope.detailAgencyIds().contains(agencyId)` in the initial cut, which was the bug).

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

## Phase 1b — scope-set correction

**The bug:** the initial Phase 1 cut derived the Agency Admin bucket's `detailAgencyIds`/
`rollupAgencyIds` from the singleton `primaryAgencyId` (`ids = primaryAgencyId != null ?
Set.of(primaryAgencyId) : Set.of()`). `primaryAgencyId` and detail/rollup answer two
different questions and must not be conflated:

- **`primaryAgencyId`** = "which agency do I *display*/default to" — singular, tie-broken
  (manager match, else lowest agency_id membership). Used for branding, the New-Opportunity
  modal's hidden `agencyId` field, prospect-list defaults. **Unchanged by Phase 1b, in every
  bucket including PSP staff.**
- **`detailAgencyIds`/`rollupAgencyIds`** = "which agencies may I *see*" — a **set**.
  Person↔Agency is genuinely many-to-many (the `agents` join table), so an Agency Admin can
  legitimately manage one agency while also being a plain member of another. Deriving this
  set from the singular tie-break silently dropped every membership except the one that won
  the tie-break — exactly the narrowing `PHASE1_NOTES.md`'s original OpportunityAuthz row
  flagged.

**The fix (`AgencyScopeResolver.java`):**
- Added `resolveAgencyMembershipIds(EntityManager, Long)` — `SELECT DISTINCT a.id FROM
  Agency a LEFT JOIN a.agentList ag WHERE a.manager.id = :pid OR ag.id = :pid`. This is the
  set-valued form of `OpportunityAuthz`'s original predicate, not a derivation of
  `primaryAgencyId`.
- The Agency Admin bucket now sets `rollup = detail = resolveAgencyMembershipIds(...)`,
  independent of `primaryAgencyId` (which is still computed alongside it via the unchanged
  tie-break).
- Plain Agent and PSP-staff buckets are unchanged (`rollup`/`detail` stay empty sets in both;
  `primaryAgencyId` still resolved in both, as it always was).
- Added `AgencyScopeResolver.canSeeDetail(scope, agencyId)` / `canSeeRollup(scope,
  agencyId)` — both `pspWide() || <set>.contains(agencyId)`. PSP staff have `pspWide=true`
  with **empty** scope sets by design (§ pspWide already implies "everything in the
  tenant"); a call site that read the raw set directly (`scope.detailAgencyIds().contains(id)`)
  would incorrectly deny PSP staff access to their own tenant. Javadoc on both methods says
  call sites MUST use the gate methods, never the raw sets. `OpportunityAuthz.belongsToAgency()`
  now calls `canSeeDetail()` instead of reading the set directly.

**Equivalence check, as required by the task:** `OpportunityAuthz`'s pre-Phase-1 predicate
was `a.manager.id = :personId OR ag.id = :personId` (via `LEFT JOIN a.agentList ag`), scoped
to `a.id = :agencyId`, returned as a `COUNT(a) > 0` boolean — "is the *specific target*
agency one where I'm manager or a member." `resolveAgencyMembershipIds` runs the identical
`LEFT JOIN a.agentList ag WHERE a.manager.id = :pid OR ag.id = :pid` predicate *without* the
`a.id = :agencyId` filter, returning the full set of agencies satisfying it; `canSeeDetail`
then checks `.contains(agencyId)`. These are logically the same truth table for any given
target agency — the old query filtered-then-counted, the new one computes-the-set-then-
membership-tests — same join, same OR, same rows. `belongsToAgency()`'s own gating call site
(`OpportunityAuthz.canAccessOpportunity`, line ~68) still checks `flag(request,
"isAgencyAdmin")` before calling it, unchanged. Net: **`OpportunityAuthz.belongsToAgency()`
is now exactly equivalent to its pre-Phase-1 predicate.** (`canSeeDetail`'s `pspWide()` OR-branch
is technically unreachable at this specific call site, since `canAccessOpportunity` already
returns `true` for PSP staff at an earlier check, lines 53-55 — routing through `canSeeDetail`
anyway per the task's explicit directive, so the gate is used correctly if that earlier
early-return is ever removed or reordered.)

## Behavior-change table (current, post-1b)

| Call site | Verdict | Detail |
|---|---|---|
| **AgentHome.findAgencyForUser()** → `AgencyScopeResolver` | **(b) Behavior change — narrow edge case, no known live trigger. Unaffected by Phase 1b — still uses `primaryAgencyId` only, per instruction not to switch it to the rollup set.** | `AgentHome.doGet`/`doPost` have **no role gate at all** (confirmed — any authenticated session can hit `/AgentHome`). The old `findAgencyForUser` ran its manager-then-membership tie-break for *any* Person regardless of role. The new resolver's "anyone else" bucket (session has none of `isPspAdmin`/`isPspUser`/`isPspSales`/`isAgencyAdmin`/`isAgent` set) returns `primaryAgencyId = null` unconditionally, even if that person is still literally listed in some `agency.agentList` or as an `agency.manager_id`. Trigger: a person whose sales role was later revoked (so no gating flag is set at login) but who was never removed from the agency's `agents` join table or as `manager_id` — they'd now see an empty pipeline instead of their stale agency's data. Checked every `addAgent()`/`setManager()` call site in the codebase (`ReferenceDataSeeder`, `UserManager`, `CreateUser25`, `AcceptInvite`, `AgencyAction`, `GenerateProp`/`GenerateProp25`) — all of them only ever add Agent/Agency-Admin-role people, so there's no code path today that creates this stale state. Flagging it because the divergence is real, not because it's reachable today. |
| **CreateUser25.findAgencyForUser()** → `AgencyScopeResolver` | **(a) Exactly equivalent** | The only call site is gated by `if (!isAgencyAdmin) return "Only Agency Admins can create agents.";` immediately above, so the resolver always executes in the Agency Admin bucket, which runs the identical tie-break the old method ran (`primaryAgencyId`, untouched by Phase 1b). |
| **ProposalBuilder.findAgencyForUser()** → `AgencyScopeResolver` (both call sites) | **(a) Exactly equivalent** | Call site 1 is gated `if (isAgent \|\| isAgencyAdmin)` → Agency Admin or Plain Agent bucket, same tie-break. Call site 2 runs unconditionally inside `if (isPspAdmin)` → PSP-staff bucket, which still runs the same tie-break for `primaryAgencyId` (unchanged by Phase 1b — the fix touched only the Agency Admin bucket's scope *sets*, not `primaryAgencyId` in any bucket). A session with more than one flag true (e.g. `isPspAdmin && isAgent`) doesn't change the outcome either — every bucket that can fire here computes `primaryAgencyId` via the identical query. |
| **OpportunityAuthz.belongsToAgency()** → `AgencyScopeResolver` | **(a) Exactly equivalent (corrected in Phase 1b — see the equivalence check above)** | Pre-1b this row read "(b) real behavior change" — the initial cut narrowed access to only the tie-broken `primaryAgencyId`, dropping any other agency the admin was a plain member of. Phase 1b's `resolveAgencyMembershipIds()` restores the full multi-agency membership set, so `canSeeDetail()` now matches the pre-Phase-1 `a.manager.id = :personId OR ag.id = :personId` predicate exactly, for every target agency. Confirmed live consumers: `UpdateOpportunityStage.java` and `GoActivityDetail25.java` both call `OpportunityAuthz.canAccessOpportunity(...)`, which is what invokes this. |

**Net status versus pre-Phase-1 code:** after Phase 1b, exactly **one** behavior change
remains anywhere in this refactor — the `AgentHome` edge case above, which requires a
session with none of the five gating role flags set while the underlying person still has a
stale `agents`/`manager_id` row from before their role was revoked. No code path in the
codebase currently creates that stale state, so there is no known live trigger. Every other
call site (`CreateUser25`, both `ProposalBuilder` sites, `OpportunityAuthz.belongsToAgency()`)
is now exactly equivalent to its pre-Phase-1 behavior.

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
