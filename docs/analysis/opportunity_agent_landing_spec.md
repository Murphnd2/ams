# Opportunity System + Agent Landing Page — Feature Spec

**Date:** February 21, 2026
**Status:** SPEC — ready to build next session
**Approach:** Hybrid (Option C) — Opportunity as Activity subtype + separate landing page

---

## 1. Overview

Agents and Agency Managers currently land on `ViewHome25` after login, which is designed for PSP internal staff and appears mostly blank for external users. This spec defines:

1. **Opportunity** — a new Activity subtype tied to a Prospect, giving agents the full Activity toolset (notes, checklist, todos, status tracking, email) for managing sales opportunities
2. **Agent Landing Page** — a purpose-built home page for agents/agency managers that shows their pipeline, opportunities, and agency context
3. **Login routing** — detect role after authentication and redirect to the appropriate landing page

---

## 2. Opportunity Entity

### Inheritance

```
Assignee (SINGLE_TABLE root)
└── Activity
    ├── CheckList
    ├── Renewal      ← tied to Employer
    ├── Ticket       ← tied to Person (contact)
    ├── Setup        ← tied to Application/Proposal
    └── Opportunity  ← tied to Prospect (NEW)
```

### Entity Design

```java
@Entity
public class Opportunity extends Activity {

    @ManyToOne
    @JoinColumn(name = "prospect_id")
    private Prospect prospect;

    @ManyToOne
    @JoinColumn(name = "agency_id")
    private Agency agency;

    @OneToOne
    @JoinColumn(name = "checklist_id")
    private CheckList checkList;

    @Column(name = "opportunity_stage", columnDefinition = "varchar(30) DEFAULT 'NEW'")
    private String stage;

    @Column(name = "estimated_employees")
    private Integer estimatedEmployees;

    @Column(name = "estimated_value")
    private Double estimatedValue;

    @Column(name = "expected_close_date")
    private java.sql.Date expectedCloseDate;
}
```

### Opportunity Stages

| Stage | Meaning |
|-------|---------|
| NEW | Just created, initial outreach |
| CONTACTED | First contact made |
| QUALIFIED | Prospect is a real opportunity |
| PROPOSAL_SENT | Proposal created and sent |
| NEGOTIATION | Active discussion on terms |
| WON | Deal closed, becoming a client |
| LOST | Prospect declined or went elsewhere |
| ON_HOLD | Paused, revisit later |

### What Opportunity Inherits from Activity

- `id` (Long) — from Assignee
- `fullName` (String) — from Assignee, used as display name (set to prospect name)
- `dateCreated` (Timestamp)
- `dueDate` (Date)
- `isComplete` (boolean)
- `assignedTo` (Person) — the agent working the opportunity
- `loggedBy` (Person) — who created it
- `primaryContact` (Person) — the prospect contact
- `assigneeContactList` (M:N Person) — additional contacts
- `noteList` (1:M Note) — notes and emails
- `webLinkList` (M:N WebLink) — attachments

### What Opportunity Gets for Free via ViewActivity25

- Add/view notes
- Send emails
- Checklist with todos
- Attachments
- Contact management
- Activity status tracking
- Past activity history (query by prospect, like Renewal queries by employer)

---

## 3. Database Changes

```sql
-- Opportunity uses the existing assignee table (SINGLE_TABLE inheritance)
-- EclipseLink adds a DTYPE='Opportunity' discriminator automatically
-- We only need the Opportunity-specific columns on the assignee table:

ALTER TABLE assignee ADD COLUMN prospect_id BIGINT NULL;
ALTER TABLE assignee ADD COLUMN agency_id_opp BIGINT NULL;
ALTER TABLE assignee ADD COLUMN opportunity_stage VARCHAR(30) NULL DEFAULT 'NEW';
ALTER TABLE assignee ADD COLUMN estimated_employees INT NULL;
ALTER TABLE assignee ADD COLUMN estimated_value DOUBLE NULL;
ALTER TABLE assignee ADD COLUMN expected_close_date DATE NULL;

ALTER TABLE assignee ADD CONSTRAINT fk_opp_prospect
  FOREIGN KEY (prospect_id) REFERENCES prospect(prospect_id);
ALTER TABLE assignee ADD CONSTRAINT fk_opp_agency
  FOREIGN KEY (agency_id_opp) REFERENCES agency(agency_id);

-- Note: column named agency_id_opp to avoid conflict with any existing agency_id usage
```

### View Update

The `Activity25` view (used for activity listing) will need updating to include Opportunity data. We'll need to check its current definition and add DTYPE='Opportunity' support.

---

## 4. Agent Landing Page

### URL & Servlet

| Component | Value |
|-----------|-------|
| Servlet | `AgentHome` |
| URL | `/AgentHome` |
| JSP | `WEB-INF/view/sales/agentHome25.jsp` |
| Auth | Yes (role 2 or 8) |

### Layout

```
┌──────────────────────────────────────────────────────────────┐
│  [Agency Name]                          [User] [Logout]      │
├──────────────┬───────────────────────────────────────────────┤
│              │                                               │
│  MY PIPELINE │  OPPORTUNITY DETAIL                           │
│              │                                               │
│  ┌─────────┐ │  [Prospect Name]          Stage: [dropdown]   │
│  │ NEW (3) │ │                                               │
│  │ • Acme  │ │  ┌──────────┐ ┌──────────┐ ┌──────────┐     │
│  │ • Beta  │ │  │ Details  │ │ Notes    │ │ Checklist│     │
│  │ • Gamma │ │  └──────────┘ └──────────┘ └──────────┘     │
│  ├─────────┤ │                                               │
│  │QUAL (2) │ │  [Tab content area]                           │
│  │ • Delta │ │                                               │
│  │ • Echo  │ │  Details: contact info, proposals,            │
│  ├─────────┤ │           estimated value, close date         │
│  │SENT (1) │ │                                               │
│  │ • Foxt  │ │  Notes: same note system as ViewActivity25    │
│  ├─────────┤ │                                               │
│  │WON  (0) │ │  Checklist: same todo system                  │
│  │LOST (1) │ │                                               │
│  └─────────┘ │                                               │
│              │  ┌─────────────────────────────────┐          │
│  [+ New Opp] │  │ PROPOSALS FOR THIS PROSPECT     │          │
│              │  │ #101 Sent    FSA HRA    1/26    │          │
│              │  │ #98  Viewed  COBRA      12/25   │          │
│              │  └─────────────────────────────────┘          │
│              │                                               │
│  ─────────── │  [Create Proposal] [Send Email]               │
│  QUICK STATS │                                               │
│  Active: 6   │                                               │
│  Won: 12     │                                               │
│  Lost: 3     │                                               │
│  Pipeline $: │                                               │
│    $45,000   │                                               │
│              │                                               │
└──────────────┴───────────────────────────────────────────────┘
```

### Left Panel — Pipeline

- Opportunities grouped by stage (collapsible sections)
- Count badges per stage
- Click to select → loads detail in right panel
- **"+ New Opportunity"** button → modal to create (picks prospect, sets initial stage)
- Quick stats at bottom (counts, pipeline value)
- Filter by agent (Agency Manager sees all agency opportunities, Agent sees only their own)

### Right Panel — Opportunity Detail

- **Header:** Prospect name, stage dropdown (change stage inline), due date
- **Tab: Details** — Prospect contact info, agency, assigned agent, estimated employees/value, expected close date, linked proposals table
- **Tab: Notes** — Reuses the existing note/email system from ViewActivity25
- **Tab: Checklist** — Reuses the existing todo system from ViewActivity25
- **Actions:** Create Proposal (links to ProposalBuilder with prospect pre-selected), Send Email

### Agency Manager vs Agent

| Feature | Agent | Agency Manager |
|---------|-------|----------------|
| See own opportunities | ✅ | ✅ |
| See all agency opportunities | ❌ | ✅ |
| Create opportunities | ✅ | ✅ |
| Invite agents | ❌ | ✅ |
| Edit agency details | ❌ | ✅ |
| View pipeline stats | Own only | Full agency |

---

## 5. Login Routing

### Current Flow
```
AuthenticateUser → always → ViewHome25
```

### New Flow
```
AuthenticateUser
  → has role 8 (Agency Admin)?  → redirect to /AgentHome
  → has role 2 (Agent)?         → redirect to /AgentHome
  → else                        → redirect to ViewHome25 (existing)
```

### Implementation

In `AuthenticateUser`, after successful authentication and role assignment, check session flags:

```java
if (isAgencyAdmin || isAgent) {
    response.sendRedirect("AgentHome");
} else {
    // existing ViewHome25 forward
}
```

This also requires adding `case 8` to `AuthDAO.assignUserRoles()`:

```java
case 8: isAgencyAdmin = true; break;
```

And the session attribute:
```java
request.getSession().setAttribute("isAgencyAdmin", isAgencyAdmin);
```

---

## 6. ViewActivity25 Integration

When an agent clicks into an Opportunity from the agent landing page, `ViewActivity25` needs to handle DTYPE="Opportunity". The existing switch in `ActivityViewHelper.setActivityView()` needs a new case:

```java
case "Opportunity":
    request.getSession().setAttribute("adminView", 4); // new view mode
    Opportunity opp = EntityLookup.getOpportunityById(em, id);
    request.getSession().setAttribute("currentOpportunity", opp);
    request.getSession().setAttribute("currentRenewal", new Renewal());
    request.getSession().setAttribute("currentSetup", new Setup());
    request.getSession().setAttribute("currentTicket", new Ticket());
    // Load prospect's proposals for the detail panel
    if (opp.getProspect() != null) {
        List<Proposal> proposals = SalesDAO.getProposalListFull(em, opp.getProspect());
        request.setAttribute("opportunityProposals", proposals);
    }
    break;
```

The `viewActivity25.jsp` already uses `adminView` to show/hide type-specific sections. Adding `adminView==4` sections for Opportunity-specific content (prospect info, stage, proposals) follows the existing pattern.

---

## 7. CheckList Pattern

Every Activity subtype follows the same checklist creation pattern (see `CreateTicket25`, `CreateBlankRenewal25`):

1. Create the Activity (Opportunity)
2. Create a CheckList linked to it
3. Populate todos from a RequiredTaskList (via TemplatePurpose → TaskSequenceTable → Task)

For Opportunities, we need:
- A new **TemplateGroup** for sales (or reuse group 3/tickets if appropriate)
- A **TemplatePurpose** for "New Opportunity" (or multiple for different opportunity types)
- A **RequiredTaskList** with default sales tasks (e.g., "Initial contact", "Qualify needs", "Send proposal", "Follow up", "Close")

These can be created via the existing Sequence Builder UI — no code changes needed for the task template system.

---

## 8. Past Activities for Opportunities

The `fillPastActivities()` method in `AmsDataLocal` needs a new case:

```java
case "Opportunity":
    Opportunity opp = (Opportunity) a;
    q = em.createQuery("SELECT o FROM Opportunity o WHERE o.prospect.id = :pId AND o.id <> :oId ORDER BY o.id DESC");
    q.setParameter("pId", opp.getProspect().getId());
    q.setParameter("oId", opp.getId());
    break;
```

This shows previous opportunities for the same prospect — same pattern as Renewal showing previous renewals for the same employer.

---

## 9. Build Order (Next Session)

| Step | Task | Type |
|------|------|------|
| 1 | Add `case 8` to `AuthDAO.assignUserRoles()` | Java edit |
| 2 | Add login routing in `AuthenticateUser` | Java edit |
| 3 | Run schema migration (assignee columns for Opportunity) | SQL |
| 4 | Create `Opportunity.java` entity | New file |
| 5 | Create `CreateOpportunity.java` servlet | New file |
| 6 | Add "Opportunity" case to `ActivityViewHelper` | Java edit |
| 7 | Add "Opportunity" case to `fillPastActivities` | Java edit |
| 8 | Create `AgentHome.java` servlet | New file |
| 9 | Create `agentHome25.jsp` | New file |
| 10 | Add Opportunity sections to `viewActivity25.jsp` | JSP edit |
| 11 | Seed TemplateGroup/TemplatePurpose/Tasks for sales | SQL |
| 12 | Test full flow: login → agent home → create opportunity → view detail → notes → checklist |

---

## 10. Files to Create

| File | Location | Purpose |
|------|----------|---------|
| `Opportunity.java` | `model/activity/` | JPA entity extending Activity |
| `CreateOpportunity.java` | `controller/activity/setup/` | Creates Opportunity + CheckList |
| `AgentHome.java` | `controller/activity/setup/` | Agent/Agency Manager landing page servlet |
| `agentHome25.jsp` | `WEB-INF/view/sales/` | Agent landing page UI |

## Files to Modify

| File | Changes |
|------|---------|
| `AuthDAO.java` | Add `case 8: isAgencyAdmin=true` + session attribute |
| `AuthenticateUser.java` | Add role-based redirect after login |
| `ActivityViewHelper.java` | Add "Opportunity" case in `setActivityView()` |
| `AmsDataLocal.java` | Add "Opportunity" case in `fillPastActivities()` |
| `viewActivity25.jsp` | Add `adminView==4` sections for Opportunity detail |
| `CheckList.java` | Add `@OneToOne(mappedBy = "checkList") Opportunity opportunity` |
| `LoginFilter.java` | No change needed (AgentHome requires auth) |
| `StdAuto.java` | Add "Opportunity" case for email recipient resolution |
