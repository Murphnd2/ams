# Opportunity System — Spec Update (Post-Build)

**Original Spec:** `docs/analysis/opportunity_agent_landing_spec.md`
**Status:** ✅ COMPLETE — All 12 build steps done + bug fixes + agent scoping

---

## Changes from Original Spec

### Entity Design — Minor Adjustments
- `opportunity_stage` column has NO default value (original spec had `DEFAULT 'NEW'`). Default is set in Java entity only. Backfilling all rows caused EclipseLink issues.
- `agency_id_opp` column name confirmed (avoids conflict with any existing `agency_id` usage)

### CreateOpportunity — Enhanced
- Added "New Prospect" mode: agents can create a new prospect inline (company name, contact first/last, email) instead of requiring an existing prospect
- Modal has radio toggle between "New Prospect" and "Existing Prospect" modes

### ViewActivity25 Integration — Different Path
- Spec suggested using `ActivityViewHelper.setActivityView()` with `adminView==4`
- Actual implementation: agents use `ViewById` → `GoActivityDetail25` → `intializeActivity()` which loads via `AmsDataLocal.CurrentActivity`
- `detailOpportunity25.jsp` reads from `sessionScope.local.getCurrentActivity().getActivity()` directly (not `sessionScope.currentOpportunity`)
- Required fixing the JSP role gate: `activityDetail25.jsp` was wrapped in `isPspUser || isPspAdmin` — added `isAgent || isAgencyAdmin`

### ProposalBuilder — Agent Scoping (Added)
- Not in original spec, but required for agent workflow
- Prospect dropdown filtered by agent (own prospects) or agency (manager's view)
- Rate selection filtered to agency's assigned rates only
- Single rate auto-selected if agency has exactly one
- `prospectId` parameter pre-selects prospect in dropdown
- Back button role-aware (Pipeline vs Dashboard)

### Navigation — Agent-Aware (Added)
- `detailHeader25.jsp` back button: agents → AgentHome
- `navbar25.jsp`: Pipeline + New Proposal links for agents
- Owner button hidden for non-PSP users in `detailFooter25.jsp`

---

## Remaining Items from Spec

| Spec Item | Status | Notes |
|-----------|--------|-------|
| Opportunity entity | ✅ Done | |
| CreateOpportunity servlet | ✅ Done | Enhanced with new prospect mode |
| AgentHome servlet + JSP | ✅ Done | |
| AuthDAO case 8 | ✅ Done | |
| Login routing | ✅ Done | |
| ActivityViewHelper | ✅ Done | |
| fillPastActivities | ✅ Done | |
| ViewActivity25 integration | ✅ Done | Via GoActivityDetail25 path |
| Task seed data | ✅ Done | Group 5, Purpose 30, Tasks 900001-900005 |
| CheckList.java backref | ❌ Not done | Low priority — add `@OneToOne(mappedBy="checkList") Opportunity` |
| StdAuto.java Opportunity case | ❌ Not done | Low priority — email recipient resolution |
| PspAgencyHome scoping | ❌ Not done | HIGH — Agency Manager should only see own agency |
