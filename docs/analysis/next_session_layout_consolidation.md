# Next Session — Layout & Navigation Consolidation

**Priority:** HIGH
**Goal:** Unify the look, feel, and navigation across all main pages for both PSP and Agent users

---

## Current State

### Pages with Navigation Issues

| Page | Current Navbar | Back Button | Issues |
|------|---------------|-------------|--------|
| `pspHome25.jsp` | navbar25.jsp (PSP links) | N/A | Agent baseline — OK |
| `activityDetail25.jsp` | navbar25.jsp | → ViewHome25 or AgentHome | ✅ Fixed this session |
| `checklistDetail25.jsp` | navbar25.jsp | Same as activityDetail | ⚠️ Needs same role gate fix as activityDetail25 |
| `proposalBuilder.jsp` | navbar.jsp (OLD!) | → AgentHome or ViewHome25 | ✅ Fixed, but uses old navbar |
| `proposalDetail.jsp` | navbar.jsp (OLD!) | → ViewHome25 | Needs role-aware back |
| `agentHome25.jsp` | navbar25.jsp | N/A | ✅ OK |
| `agencyManager25.jsp` | navbar25.jsp | → ViewHome25 | Needs scoping for Agency Manager |
| `rateManager25.jsp` | navbar25.jsp | → PspAdminHome | PSP only — OK |
| `reviewApplications.jsp` | Custom header | → ViewHome25 | Needs role check |
| `reviewApplication.jsp` | Custom header | → ReviewApplications | OK |
| `manualSetup.jsp` | Custom header | → ReviewApplications | OK |
| `billingHome.jsp` | Own layout | Own nav | PSP only — OK |

### Two Navbars in Use
- `navbar25.jsp` — Current, has agent links added this session
- `navbar.jsp` — Old version, used by proposalBuilder, proposalDetail, some others

---

## Proposed Work

### 1. PspAgencyHome Scoping (Agency Manager View)
- Filter agency list to only show manager's agency
- Hide rate management entirely (no assign/remove rates, no rate popovers)
- Hide pending agents for other agencies
- Hide "Create Agency" button
- Possibly add "My Agency" header instead of agency list

### 2. checklistDetail25.jsp Role Gate
- Same fix as activityDetail25.jsp — add `isAgent || isAgencyAdmin` to the `<c:when>` gate
- Currently agents would get blank page viewing standalone checklists

### 3. Navbar Consolidation
- Migrate remaining pages from `navbar.jsp` to `navbar25.jsp`
- Or create a unified approach:
  - PSP users: Home, Log, Email, Admin menu
  - Agents: Pipeline, New Proposal
  - Both: Logout
  - Brand logo links to role-appropriate home page

### 4. ProposalDetail Navigation
- Back button should go to AgentHome for agents
- "Home" button should be role-aware

### 5. Consistent Page Chrome
- All authenticated pages should use same navbar
- All pages should have consistent header/footer pattern
- Consider a shared layout fragment that handles role detection

---

## Agent-Visible Pages (Audit)

Pages an agent should be able to access:
1. `AgentHome` — Pipeline view ✅
2. `ViewActivity25` (via ViewById) — Opportunity detail ✅
3. `ProposalBuilder` — Create proposals ✅
4. `ProposalDetail` — View proposal details ⚠️ Nav needs fixing
5. `SendProposal` — Send proposal email (POST only)
6. `CreateProspect` — Create prospect (POST only)

Pages an agent should NOT access:
- `ViewHome25` — PSP dashboard
- `GoAdminHome` / `PspAdminHome` — Admin settings
- `RateManager` — Rate configuration
- `PspAgencyHome` — PSP agency management (but Agency Manager version TBD)
- `ReviewApplications` — Application review (PSP only? or should agents see their own?)
- `BillingHome` — Billing (PSP only)
- Sequence Builder, Import tools, etc.
