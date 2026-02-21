# Opportunity System — Build Log

**Date:** February 21, 2026
**Sessions:** 2 (spec session + build session)
**Status:** ✅ COMPLETE — deployed and tested

---

## Overview

Built the Opportunity system: a new Activity subtype for agents to track sales pipeline opportunities, plus an Agent Landing Page with pipeline view, and full integration with ViewActivity25 for detailed opportunity management.

---

## Files Created

| File | Location | Purpose |
|------|----------|---------|
| `Opportunity.java` | `model/activity/` | JPA entity extending Activity, DTYPE='Opportunity' |
| `CreateOpportunity.java` | `controller/activity/setup/` | Creates Opportunity + CheckList + optional new Prospect |
| `AgentHome.java` | `controller/activity/setup/` | Agent/Agency Manager landing page servlet |
| `UpdateOpportunityStage.java` | `controller/activity/setup/` | AJAX stage dropdown update |
| `agentHome25.jsp` | `WEB-INF/view/sales/` | Pipeline view with stage grouping, detail panel, new opp modal |
| `detailOpportunity25.jsp` | `WEB-INF/view/a/activityDetail/columns/detail/` | Opportunity detail panel in ViewActivity25 |

## Files Modified

| File | Changes |
|------|---------|
| `AuthDAO.java` | Added `case 8: isAgencyAdmin=true` + session attribute |
| `AuthenticateUser.java` | Added role-based redirect: agents → AgentHome, PSP → ViewHome25 |
| `ActivityViewHelper.java` | Added "Opportunity" case in `setActivityView()` |
| `AmsDataLocal.java` | Added "Opportunity" case in `fillPastActivities()` (inner class version) |
| `EntityLookup.java` | Added `getOpportunityById()` method |
| `GenerateProp.java` | Fixed `setActivityView` call (was passing Setup object, now passes `setup.getId()`) |
| `detailDetail25.jsp` | Added Opportunity `<c:when>` case to import `detailOpportunity25.jsp` |
| `detailHeader25.jsp` | Added Opportunity color (primary) and icon (bullseye); back button role-aware |
| `activityDetail25.jsp` | Added `isAgent \|\| isAgencyAdmin` to role gate (was PSP-only) |
| `detailFooter25.jsp` | Hidden Owner button for non-PSP users |
| `navbar25.jsp` | Added Pipeline and New Proposal links for agents |
| `ProposalBuilder.java` | Filtered prospects by agent/agency; filtered rates by agency; auto-select single rate; fixed `prospectId` pre-selection |
| `proposalBuilder.jsp` | Added auto-rate selection JS; role-aware back button |

---

## Database Changes

### Schema Migration (assignee table)

```sql
ALTER TABLE assignee ADD COLUMN prospect_id BIGINT NULL;
ALTER TABLE assignee ADD COLUMN agency_id_opp BIGINT NULL;
ALTER TABLE assignee ADD COLUMN opportunity_stage VARCHAR(30) NULL;
ALTER TABLE assignee ADD COLUMN estimated_employees INT NULL;
ALTER TABLE assignee ADD COLUMN estimated_value DOUBLE NULL;
ALTER TABLE assignee ADD COLUMN expected_close_date DATE NULL;

ALTER TABLE assignee ADD CONSTRAINT fk_opp_prospect
  FOREIGN KEY (prospect_id) REFERENCES prospect(prospect_id);
ALTER TABLE assignee ADD CONSTRAINT fk_opp_agency
  FOREIGN KEY (agency_id_opp) REFERENCES agency(agency_id);
```

**Important:** Do NOT use `DEFAULT 'NEW'` on opportunity_stage — it backfills all existing rows and causes issues with EclipseLink type resolution.

### Task Seed Data

```sql
-- TemplateGroup 5 "Sales" (verify exists, create if not)
INSERT IGNORE INTO templategroup (id, description) VALUES (5, 'Sales');

-- TemplatePurpose 30 "New Opportunity"
INSERT IGNORE INTO templatepurpose (id, description, sort_order, template_group) VALUES (30, 'New Opportunity', 100, 5);

-- Sales Tasks (900000+ range to avoid collisions)
INSERT INTO task (id, description, allow_early, allow_future) VALUES
  (900001, 'Initial contact with prospect', 1, 1),
  (900002, 'Qualify prospect needs', 1, 1),
  (900003, 'Send proposal', 1, 1),
  (900004, 'Follow up on proposal', 1, 1),
  (900005, 'Close deal', 1, 1);

-- TaskSequence (RequiredTaskList for Opportunity)
INSERT INTO tasksequence (id, description, DTYPE, purpose_id) VALUES
  (900001, 'New Opportunity Tasks', 'RequiredTaskList', 30);

-- Link tasks to sequence
INSERT INTO tasksequencetable (sequence_id, task_id, sort_order) VALUES
  (900001, 900001, 100),
  (900001, 900002, 200),
  (900001, 900003, 300),
  (900001, 900004, 400),
  (900001, 900005, 500);
```

---

## Key Architecture Decisions

### Opportunity as Activity Subtype
- Uses SINGLE_TABLE inheritance on `assignee` (DTYPE='Opportunity')
- Gets full Activity toolset for free: notes, emails, checklist, todos, attachments, contacts
- Follows same pattern as Renewal/Ticket/Setup/CheckList

### Agent Role Gate in JSPs
- `activityDetail25.jsp` was wrapped in `isPspUser || isPspAdmin` — agents got blank page
- Fixed by adding `isAgent || isAgencyAdmin` to the condition
- Owner button hidden for non-PSP users (agents shouldn't reassign activities)

### ProposalBuilder Scoping
- Agents see only their own prospects (or agency prospects for managers)
- Rates filtered to only those assigned to the agent's agency
- Single rate auto-selected (skips step 2 effectively)

### Back Button / Navigation
- `detailHeader25.jsp` back button: agents → AgentHome, PSP → ViewHome25
- `navbar25.jsp`: agents see Pipeline + New Proposal; PSP sees Home + Log + Email
- `proposalBuilder.jsp`: agents → Back to Pipeline; PSP → Back to Dashboard

### ViewById for Activity Navigation
- AgentHome links use `ViewById?id=X` which forwards to `GoActivityDetail25`
- This ensures proper activity initialization via `intializeActivity()`
- Direct `ViewActivity25` links skip initialization → blank page

---

## Opportunity Stages

| Stage | Color | Meaning |
|-------|-------|---------|
| NEW | Blue (#e3f2fd) | Just created |
| CONTACTED | Green (#e8f5e9) | First contact made |
| QUALIFIED | Orange (#fff3e0) | Real opportunity confirmed |
| PROPOSAL_SENT | Purple (#f3e5f5) | Proposal created and sent |
| NEGOTIATION | Red (#fce4ec) | Active terms discussion |
| ON_HOLD | Gray (#f5f5f5) | Paused |
| WON | Dark Green (#e8f5e9) | Deal closed |
| LOST | Dark Red (#fbe9e7) | Declined |

WON/LOST automatically mark the opportunity as complete (`isComplete = true`).

---

## Known Remaining Items

| Item | Priority | Notes |
|------|----------|-------|
| PspAgencyHome scoping | HIGH | Agency Manager sees only their agency, hide rate management |
| CheckList.java backref | LOW | Add `@OneToOne(mappedBy = "checkList") Opportunity opportunity` |
| StdAuto.java | LOW | Add Opportunity case for email recipient resolution |
| Layout/appearance consolidation | NEXT SESSION | Unify toolbar/navigation across all main pages |
