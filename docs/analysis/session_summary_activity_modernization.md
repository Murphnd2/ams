# Session — February 22, 2026 — Activity List Modernization + PSP Opportunity Integration

## Summary

Modernized the ViewHome25 activity list (center column) with SSA branding to match the recently updated ToDo column, email screen, and other pages. Integrated Opportunity tracking into the activity list for PSP Admin and PSP Sales users.

## Activity Column Modernization

### activityHeader25.jsp — Complete Rewrite
- Replaced `bg-dark border-dark rounded` block with `.hdr-bar` pattern (matches ToDo column, email, library, service manager)
- Quick-view buttons (ALL/MY/REN) moved to left side of header bar
- Title centered with activity icon
- Single filter toggle button on right → collapses filter panel below
- Filter panel: light background (`#f8f9fb`), two clean rows:
  - Row 1: TYPE toggles (R/S/T/O) + ATTENTION toggles (On Us / Needs Contact)
  - Row 2: OWNER radios + SORT radios + branded Apply button (`btn-ssa`)
- Small `text-ssa fw-bold` labels replace old `pe-none` button labels

### activityList25.jsp — Complete Rewrite
- Replaced `input-group` button strips with `.act-card` flex rows (white card, hover highlight, click-to-navigate)
- Left border colored by due bucket: red (overdue), orange (warning), transparent (normal)
- Compact type badge pills: R (blue), S (purple), T (teal), O (green)
- Urgency icons in fixed-width column (2rem): `bi-stack-overflow` for waiting-on-us, `bi-telephone-fill` for needs-contact (color-coded red/orange)
- Fixed-width urgency column ensures consistent name left-alignment regardless of icon presence
- Name: truncated with ellipsis, UPPER+bold when waiting-on-us, lowercase otherwise
- Opportunity stage badge between name and due date (only for Opportunity rows)
- Due date: right-aligned `MMM dd` with color-coded urgency
- Entire card clickable via hidden submit button
- Empty state message when no matching activities

## PSP Opportunity Integration

### New Role: PSP Sales (UserRole ID 9)
- Created via migration script `opportunity_psp_migration.sql`
- Gates opportunity visibility in activity list
- PSP Admin (role 5) also has opportunity access
- Regular PSP Users see no opportunities

### Opportunity Entity Update
- Added `managedBy` field (nullable `@ManyToOne` Person, column `managed_by_id`)
- Allows PSP user to manage an agent's opportunity without changing `assigned_to`
- Agent retains ownership; PSP user gets management access

### ActivityFilter + ActivityLandingFilter
- Added `viewOpportunity` boolean to `ActivityFilter` (defaults `false`, overridden to `true` for PSP Sales/Admin on login)
- Added `includeOpportunity` boolean to `ActivityLandingFilter`

### ActivityLandingDao SQL Updates
- `open_act` CTE includes `DTYPE='Opportunity'` (excluding WON/LOST stages)
- Pulls `managed_by_id` and `opportunity_stage` columns
- New parameter #10: `incOpportunity` (LIMIT/OFFSET shifted to #11/#12)
- Opportunity filter requires `assigned_to_id = me OR managed_by_id = me` — only your own or managed opps
- Ownership filter expanded: managed opps show even in "my only" mode

### ActivityLandingRow
- Added `opportunityStage` field (String) + getter

### FilterActivities25
- Added `vOpp` parameter reading in custom filter branch
- ALL and MY presets set `viewOpportunity=true`
- REN preset sets `viewOpportunity=false`

### ViewHome25
- Reads `isPspSales` and `isPspAdmin` session attributes
- Only passes `includeOpportunity=true` to DAO when user has appropriate role AND filter is enabled

### JSP Updates
- Header: "O" toggle button (`btn-outline-success`, `bi-graph-up-arrow`) visible only for PSP Sales/Admin
- List: Green "O" type badge for opportunities, stage badge with color-coded pill (same palette as AgentHome pipeline)

## Database Changes

### Migration Script: `opportunity_psp_migration.sql` (Script #10)
```sql
INSERT IGNORE INTO userrole (role_id, description) VALUES (9, 'PSP Sales');
ALTER TABLE assignee ADD COLUMN managed_by_id BIGINT NULL;
ALTER TABLE assignee ADD CONSTRAINT fk_opp_managed_by
  FOREIGN KEY (managed_by_id) REFERENCES assignee(id);
```

### Dev Environment Fix: `link_psp_to_agency.sql`
- Links person 104 as manager of agency 14
- Adds to `agents` join table
- Grants PSP Sales role

## Files Changed

| File | Change |
|------|--------|
| `activityHeader25.jsp` | Complete rewrite — `.hdr-bar` pattern, filter panel, O toggle |
| `activityList25.jsp` | Complete rewrite — card rows, urgency icons, stage badges |
| `Opportunity.java` | Added `managedBy` field + getter/setter |
| `ActivityFilter.java` | Added `viewOpportunity` field + getter/setter + default |
| `ActivityLandingFilter.java` | Added `includeOpportunity` field |
| `ActivityLandingRow.java` | Added `opportunityStage` field + constructor param + getter |
| `ActivityLandingDao.java` | SQL updated for Opportunity support, new param #10, result column [10] |
| `FilterActivities25.java` | Added `vOpp` parameter in all filter branches |
| `ViewHome25.java` | Added role-gated `includeOpportunity` flag |
| `AmsDataLocal.java` | `intializeLocalData()` sets `viewOpportunity=true` for PSP Sales/Admin |
| `AuthDAO.java` | Added `case 9: isPspSales=true` + session attribute |

## Deployment Considerations (Noted for Future)

### D-15: Seed PSP House Agency with User Links
**Priority:** HIGH — Required for opportunity/sales features
**Status:** Noted, not yet implemented

The initializer creates an Agency but doesn't link PSP users to it. Needed:
1. Set `manager_id` on the house agency to the primary contact
2. Insert primary contact into `agents` join table
3. `CreatePspUser` should auto-add new PSP users to the house agency's `agents` table
4. Initialization user should get roles 1 (PSP User) + 5 (PSP Admin) + 9 (PSP Sales)
5. PSP users do NOT get Agent role (ID 2) — the agency link is data-only for proposal/opportunity scoping

### Two Proposal-Building Contexts
- **PSP Admin**: Sees ALL rates across all agencies. Can build proposals on behalf of any agent. Full pipeline visibility.
- **PSP Sales**: Scoped like an external agent. Sees only own prospects, only house agency rates. Manages own direct-sale opportunities.
- ProposalBuilder currently scopes rates by agency — works naturally for PSP Sales. PSP Admin "build on behalf" mode is a future enhancement.

## Backlog Items for Next Session

1. **Create Opportunity modal** for PSP users on ViewHome25 (similar to AgentHome modal but PSP-scoped)
2. **ProposalBuilder → auto-create Opportunity** when PSP builds a proposal (links proposal to opportunity)
3. **Filter logic revisit** — noted during header modernization for future cleanup
4. **D-15 implementation** — update DatabaseInitializer + CreatePspUser for house agency linking
