# Phase 2b — plain-agent scope fix + two residual IDOR gaps

Continues on `feat/agency-scope-resolver`. No schema, no migrations, no hierarchy
(`parent_agency_id`) walking.

## Fix 1: Plain Agent gets a real membership-based scope set

**The spec error:** Phase 1 gave the Plain Agent bucket `rollup = detail = EMPTY SET`.
That forced Phase 2's `CreateProspect`/`CreateOpportunity` agency gates to bolt on an
`|| Objects.equals(scope.primaryAgencyId(), agencyId)` carve-out just so a plain agent
could act on their own agency at all — splitting one authorization rule across two
places (the resolver's bucket rule, and an ad hoc OR at each write-path call site).

**The fix** (`AgencyScopeResolver.java`): the Plain Agent (`isAgent`, not `isAgencyAdmin`)
bucket now calls the same `resolveAgencyMembershipIds(personId)` the Agency Admin bucket
uses:

```java
if (isAgent) {
    Long primaryAgencyId = resolvePrimaryAgencyId(em, currentUser.getId());
    Set<Long> ids = resolveAgencyMembershipIds(em, currentUser.getId());
    return new AgencyScope(false, null, primaryAgencyId, ids, ids);
}
```

**Carve-outs removed** from both write paths — the gate is now plain `canSeeDetail(scope,
agencyId)`, no exceptions:
- [CreateProspect.java](src/main/java/net/superiorstate/ams/controller/activity/setup/CreateProspect.java) — `AgencyScopeResolver.canSeeDetail(scope, agencyId)`, the `|| primaryAgencyId == agencyId` clause and its `Objects` import are gone.
- [CreateOpportunity.java](src/main/java/net/superiorstate/ams/controller/activity/setup/CreateOpportunity.java) — same, in `isAuthorizedForAgency()`.

This is strictly *more* correct than the carve-out it replaces, not just equivalent: the
old OR only covered a plain agent's single tie-broken `primaryAgencyId`. A plain agent who
is a member of two agencies (Person↔Agency is many-to-many) can now legitimately act on
*either*, where the old carve-out would have 403'd the second one.

## CRITICAL — verifying no read path widened

Scope sets answer an AUTHORIZATION question ("is this agency in my world — may I write
here, does this record's owning agency count as mine"). They were never meant to answer a
READ-path row-filtering question for a Plain Agent, and after Fix 1 a Plain Agent's
`detailAgencyIds` is no longer distinguishable-by-emptiness from an Agency Admin's — so
every place that reads `scope.detailAgencyIds()`/`canSeeDetail()` for a *read* decision had
to be checked by hand to confirm it doesn't accidentally grant a Plain Agent Agency-Admin-
level read visibility. Three call sites checked, as required:

1. **AgentHome's opportunity query — unaffected, verified by absence.**
   `AgentHome.java` does not appear in the codebase-wide grep for
   `detailAgencyIds|rollupAgencyIds|canSeeDetail|canSeeRollup` at all (confirmed: 7 files
   match that grep — `ProposalDetail`, `CreateOpportunity`, `CreateProspect`,
   `AgencyScopeResolver`, `ReviewApplications`, `OpportunityAuthz`, `AgencyScope` —
   `AgentHome` is not among them). Its branch is still the untouched `isAgencyAdmin ?
   getOpportunitiesByAgency(agency.getId()) : getOpportunitiesByAgent(currentUser.getId())`
   from before this phase, and `getOpportunitiesByAgent` (`AgentHome.java:197-206`) is
   still `WHERE o.assignedTo.id = :agentId`. A Plain Agent's pipeline is exactly as
   agent_id-scoped as it was before Fix 1.

2. **ReviewApplications' Plain Agent branch — unaffected, verified by code path.**
   `ReviewApplications.java:66-72`:
   ```java
   } else if (isAgencyAdmin) {
       List<Long> agencyIds = new ArrayList<>(scope.detailAgencyIds());   // reads the set
       ...
   } else if (isAgent) {
       applications = queryApplications(em, selectedStatuses, null, currentPerson.getId());  // agentId, not agencyIds
   }
   ```
   `scope.detailAgencyIds()` is read **only** inside the `isAgencyAdmin` branch. The
   `isAgent` branch (reached only when `isAgencyAdmin` is false, since it's an `else if`
   chain) calls `queryApplications(em, statuses, null, currentPerson.getId())` — `agencyIds
   = null`, so `queryApplications` builds the `pr.agent.id = :agentId` predicate, never the
   agency-membership subquery. A Plain Agent's application list is exactly as
   agent_id-scoped as it was before Fix 1.

3. **ProposalDetail's broad visibility — explicitly re-gated to prevent widening.**
   This is the one call site that *would* have silently widened if left alone — before this
   phase, its "does the prospect's agent belong to any agency in my detail scope" loop ran
   for anyone reaching that line, and a Plain Agent's `detailAgencyIds` used to always be
   empty so the loop was a no-op for them. After Fix 1, a Plain Agent's `detailAgencyIds` is
   real, so the same loop would have started letting a plain agent open *any* colleague's
   proposal within their own agency — a genuine, unwanted widening.

   **Prevented:** `canViewProposal()` (`ProposalDetail.java`) now gates that loop on the
   `isAgencyAdmin` session flag explicitly, not on "detail set is non-empty":
   ```java
   if (prospectAgent != null && Objects.equals(prospectAgent.getId(), currentUser.getId())) {
       return true;   // self-ownership — every role, unchanged
   }
   boolean isAgencyAdmin = Boolean.TRUE.equals(request.getSession().getAttribute("isAgencyAdmin"));
   if (isAgencyAdmin && prospectAgent != null) {
       ...canSeeDetail(...) loop...   // Agency Admin only
   }
   ```
   A Plain Agent's proposal visibility is still exactly the self-ownership check — nothing
   about Fix 1 reaches them here.

**Conclusion: no read path widened for Plain Agents.** Fix 1 changes AUTHORIZATION
(`canSeeDetail` for writes) only. Every read path either never consulted the scope sets
(AgentHome), only consults them inside an `isAgencyAdmin`-gated branch that a Plain Agent's
session never enters (ReviewApplications), or was explicitly re-gated in this same phase to
keep it that way (ProposalDetail).

## Fix 2: ProposalDetail.doPost — sendToProspect and saveMarkup were unscoped

- **`sendToProspect`** had no ownership check at all — any authenticated user who could
  reach the POST could send *any* proposal's link to *its* prospect's contact email,
  regardless of which agency owned it.
- **`saveMarkup`** was role-flag-gated (`isPspAdmin || isAgent || isAgencyAdmin`) but had
  **no per-proposal ownership check** — an agent from Agency A holding any of those three
  role flags could edit markup on a proposal belonging to Agency B.

**Fix:** both actions now call the same `canViewProposal(em, request, proposal)` `doGet`
uses, immediately after the proposal is loaded and (for `saveMarkup`) after the existing
null-check — deny → 403, same idiom as everywhere else in this phase. The pre-existing
`saveMarkup`-specific checks (role-flag gate, `agencyMarkupEnabled` gate) are unchanged and
now run *in addition to*, not instead of, the ownership check.

## Fix 3: CreateProspect's unchecked agentId

Same IDOR class Phase 2 already closed in `CreateOpportunity.resolveAgent()`: a valid,
authorized `agencyId` plus an arbitrary `agentId` from *any* Person in the system was still
an IDOR (a prospect could be attributed to a person who has nothing to do with the target
agency). `CreateProspect.java`'s agent-resolution block now mirrors
`CreateOpportunity.resolveAgent()`'s pattern exactly — a submitted `agentId` is only
trusted if it's found in `agency.getAgentList()`; otherwise it's discarded and the method
falls through to the pre-existing default (current user if non-PSP-admin, else first agent
in the agency), exactly as if no `agentId` had been sent.

## Acceptance

- `mvn clean package` — **BUILD SUCCESS** (471 source files, `ams-1.0.0-SNAPSHOT.war` packaged).
- No `.sql`/migration files touched, no hierarchy walking introduced.
