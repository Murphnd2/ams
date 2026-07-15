# Phase 2 — closing the IDOR gaps

Closes the query-scoping/authorization gaps inventoried in
[AGENCY_STRUCTURE_AUDIT.md](AGENCY_STRUCTURE_AUDIT.md) §2.2 (Key inconsistencies #3, #4)
using `AgencyScopeResolver` (`net.superiorstate.ams.data.resolver`, introduced in Phase 1,
corrected in Phase 1b — see [PHASE1_NOTES.md](PHASE1_NOTES.md)). No schema, no migrations,
no hierarchy (`parent_agency_id`) walking.

**The rule enforced throughout:** every access decision routes through
`AgencyScopeResolver.canSeeDetail(scope, agencyId)` — never a raw read of
`scope.detailAgencyIds()`. PSP staff have `pspWide = true` with **empty** scope sets by
design; a raw `.contains(id)` check would incorrectly deny PSP staff access to their own
tenant. `canSeeDetail()` is the only primitive used for "is this agency in my authorized
set" anywhere in this phase.

**Deny behavior:** `response.sendError(HttpServletResponse.SC_FORBIDDEN); return;` — the
exact idiom already committed in `GenerateProp25` (commit `8a4c8d2`). No second pattern
introduced.

## A design decision that shows up in three of the five targets

`canSeeDetail()` alone is not sufficient for the write paths (`CreateProspect`,
`CreateOpportunity`) and would break the single most common demo-visible workflow if used
alone: **a Plain Agent creating a prospect/opportunity for their own agency.** A Plain
Agent's `detailAgencyIds` set is **intentionally empty** by design (Phase 1's rule: agents
scope by `agent_id`, not `agency_id`) — so `canSeeDetail(scope, myOwnAgencyId)` always
returns `false` for a Plain Agent, even for their own agency. The legitimate UI path
(`agentHome25.jsp`'s "New Opportunity" modal) submits a hidden `agencyId` field set to the
agent's own `AgencyScopeResolver.primaryAgencyId` — so without an explicit carve-out, every
plain agent's own "create a new opportunity" submission would 403.

**Fix applied consistently:** wherever a write path checks a submitted `agencyId`, the gate
is `canSeeDetail(scope, agencyId) || Objects.equals(scope.primaryAgencyId(), agencyId)`.
This closes the actual IDOR (an arbitrary *foreign* agency is rejected) while leaving the
legitimate self-service path (an agent's own agency) completely unchanged. This mirrors the
pattern the task itself specified for `ProposalDetail` (an explicit
`prospect.agent.id == currentPerson` carve-out alongside the agency-set check) — applied by
analogy to the two write endpoints where the same gap would otherwise exist. Flagged here
prominently since it's a judgment call beyond the task's literal "reject unless
canSeeDetail(...)" wording, made specifically to avoid breaking the plain-agent golden path
two days before a demo.

## Five-target table

| # | File | Before | After | Who loses access they legitimately had? |
|---|---|---|---|---|
| **1** | [ProposalDetail.java](src/main/java/net/superiorstate/ams/controller/activity/setup/ProposalDetail.java) `doGet` | **No scoping at all.** Any authenticated session — Agent, Client, Applicant, BPO user, anyone — could `GET /ProposalDetail?id=<any id>` and view any PSP's any proposal by guessing/incrementing the id. | New `canViewProposal()` gate: PSP staff (`pspWide`) always pass. Plain Agent passes only if `prospect.agent.id == currentPerson.id` (their own proposal). Agency Admin passes if the prospect's agent belongs to any agency in their `detailAgencyIds` (resolved via the same 3-hop `prospect.agent → agency.agentList` relationship `SalesDAO.getProposalsByAgency` already used in the other direction). Everyone else: 403. | **Nobody who had a legitimate reason to view a proposal.** A Plain Agent still opens every proposal for their own prospects, unchanged. An Agency Admin still opens every proposal sold by anyone in their agency, unchanged (and, post-1b, now correctly covers every agency they're a member of, not just their tie-broken primary — see PHASE1_NOTES.md "Phase 1b"). What's lost is exactly the IDOR: a Client/Applicant/BPO user, or an agent from an unrelated agency, can no longer view someone else's proposal by URL-guessing. `doPost`'s `sendToProspect` and `saveMarkup` actions were **not** touched (out of the explicit `doGet`-only scope for this target) — `sendToProspect` remains completely unscoped and `saveMarkup` remains role-flag-gated only, with no per-proposal ownership check. Flagging this as a residual gap for a later phase, same as `AGENCY_STRUCTURE_AUDIT.md`'s own "state explicitly when something doesn't exist" convention. |
| **2** | [ReviewApplications.java](src/main/java/net/superiorstate/ams/controller/activity/setup/ReviewApplications.java) `doGet` | `agentOnly = isAgent && !isPspAdmin && !isPspUser` (note: **ignores `isPspSales`**). If `agentOnly`, filter to `pr.agent.id = :agentId` (self only). **Otherwise — including every Agency Admin session — no agency/agent predicate at all**: full, unscoped visibility across every applicant in the PSP tenant. | Resolver-driven: `pspWide` → unchanged (full tenant, same as before). Agency Admin → **new** carve-out: applications whose proposal's prospect's agent belongs to any agency in `detailAgencyIds` (previously Agency Admins fell through to the fully-unscoped branch — see next column). Plain Agent → self only, unchanged. Anyone else → empty list (previously: also fully unscoped, since `agentOnly` requires `isAgent`). | **Intentional narrowing for Agency Admins — flagged explicitly, as instructed.** Before: an Agency Admin saw every application PSP-tenant-wide (they weren't in the `agentOnly` bucket, so no predicate applied at all — arguably itself an accidental IDOR, per the audit). After: an Agency Admin sees only applications belonging to their own agency/agencies. This is the biggest access change in this phase and needs a deliberate look before merging if any demo path relies on an Agency Admin seeing another agency's applications here — that was never an intended behavior, but it was the *actual* behavior until now. Two secondary fixes as a side effect of routing through the resolver instead of the old ad hoc boolean: (a) a session with `isPspSales` and `isAgent` both true previously miscomputed `agentOnly = true` (self-only) despite being PSP staff — now correctly resolves to `pspWide` (full visibility), a widening for that specific combination; (b) anyone in neither the Agent nor Agency Admin nor PSP-staff bucket (Client/Applicant/BPO) previously saw the fully-unscoped list — now correctly sees an empty list. Both are correctness fixes, not intentional feature changes, but are real behavior differences worth naming. |
| **3** | [CreateProspect.java](src/main/java/net/superiorstate/ams/controller/activity/setup/CreateProspect.java) `doPost` | `agencyId` parsed straight off an unchecked request param — any authenticated user could POST a prospect attributed to any agency in the system. | Gate: `canSeeDetail(scope, agencyId) || primaryAgencyId == agencyId` (see the design-decision section above), 403 otherwise. | **Nobody loses their normal workflow.** A Plain Agent still creates prospects for their own agency (via the `primaryAgencyId` carve-out) exactly as before. An Agency Admin/PSP staff still creates prospects for any agency in their scope, exactly as before. What's closed: submitting a foreign `agencyId` belonging to an agency the caller has no relationship to. **Residual, explicitly out-of-scope-for-this-phase gap:** this file also accepts an unchecked `agentId` request param (line ~38) and does not verify it belongs to the target agency — the exact same class of IDOR Target 4 explicitly closes for `CreateOpportunity`. The task scoped Target 3 to the `agencyId` check only; this `agentId` gap is **not fixed here** and is called out for a future phase, per the audit's own convention of naming what wasn't touched rather than silently leaving it out of the record. |
| **4** | [CreateOpportunity.java](src/main/java/net/superiorstate/ams/controller/activity/setup/CreateOpportunity.java) `resolveAgent`/`createOpportunity` | Accepted arbitrary `agencyId` **and** `agentId` request params with zero server-side check tying either to the caller's permissions — a caller could attribute a brand-new opportunity to any agency and any person system-wide. | Two independent fixes: (a) new `isAuthorizedForAgency()` gate in `doPost`, same `canSeeDetail(...) || primaryAgencyId == agencyId` rule as Target 3, checked before `createOpportunity()` ever runs. (b) `resolveAgent()` now verifies a submitted `agentId` is actually a member of the target agency's `agentList` before trusting it — an invalid/foreign `agentId` is silently discarded and falls through to the pre-existing fallback chain (current-user-if-member → first agent in agency → current user), exactly as if no `agentId` had been submitted at all. | **Nobody loses their normal workflow.** Plain Agent: still creates opportunities for their own agency via the `primaryAgencyId` carve-out; the agent-facing modal (`agentHome25.jsp`) never sends an `agentId` field at all, so the membership check never rejects anything for that path — `resolveAgent` falls through to "current user is a member" exactly as before. PSP Admin: the `addActivityModal25.jsp` agency+agent dropdowns are populated from that agency's own roster via JS, so a legitimate submission's `agentId` is always a real member — unaffected. What's closed: (1) attributing a new opportunity to a foreign agency, (2) attributing a legitimately-scoped opportunity to an unrelated person as the "agent of record" via a hand-crafted `agentId`. |
| **5** | [AgentHome.java](src/main/java/net/superiorstate/ams/controller/activity/setup/AgentHome.java) `doGet`/`doPost` | **No role gate at all** — any authenticated session, including Client/Applicant/BPO users, could load `/AgentHome`. Post-Phase-1b this just rendered an empty pipeline for those sessions (`AgencyScopeResolver`'s "anyone else" bucket → `primaryAgencyId = null`) rather than exposing data, but the page itself was still reachable. | New `isAuthorized()` gate, checked before `loadData()` runs: requires `isAgent \|\| isAgencyAdmin \|\|` PSP staff (`isPspAdmin`/`isPspUser`/`isPspSales`). Deny → 403, matching the `GenerateProp25` idiom exactly. | **Nobody who had a working pipeline loses it.** Agent and Agency Admin sessions pass the gate exactly as before and see exactly the same data (this target only adds a gate — `loadData()`'s query logic is untouched). What's closed: a Client/Applicant/BPO session can no longer even load the page (previously got an empty-but-200 response; now gets 403). As a side effect, this also **structurally closes** the theoretical "stale `agentList`/`manager_id` row on a role-revoked person" edge case flagged as PHASE1_NOTES.md behavior change #1 — a session now needs one of the five gating flags to ever reach `AgencyScopeResolver.resolve()` here at all, so the resolver's "anyone else" bucket is no longer reachable through this endpoint. |

## Net effect for the two demo-visible personas

- **Plain Agent, normal workflow (view own proposals, create own prospects/opportunities,
  see own pipeline):** **unchanged** in every target. The one Plain-Agent-relevant risk
  (Target 3/4's `canSeeDetail` gate potentially blocking their own agency) was identified
  and closed with the `primaryAgencyId` carve-out before it could reach the demo.
- **Agency Admin, normal workflow (view own agency's proposals/pipeline, create for own
  agency, manage own agents):** unchanged in Targets 1, 3, 4, 5. **Target 2 is the one
  genuine narrowing** — Agency Admins go from seeing every application PSP-tenant-wide to
  seeing only their own agency's, which is a real, intentional access change worth a
  deliberate look (flagged above) even though it's closing what the audit already
  identified as unintended over-exposure, not revoking an intentionally-granted permission.

## Acceptance

- `mvn clean package` — **BUILD SUCCESS** (471 source files, `ams-1.0.0-SNAPSHOT.war` packaged).
- No `.sql`/migration files touched.
- No hierarchy (`parent_agency_id`) walking introduced.
