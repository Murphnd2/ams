# Invitation System — Session Summary

**Date:** February 21, 2026
**Sessions:** Agency Manager enhancements + Invitation system build

---

## What Was Built

### Agency Manager Enhancements

1. **Rate Pricing Popover** — Hover over a rate name in the Rate Assignments card to see a pricing grid (fee types × service modules). Uses Bootstrap 5 popover with `sanitize: false` to allow table HTML.

2. **Rate Assignment State Tracking** — Checkboxes toggle row opacity (checked=1.0, unchecked=0.4). Save button starts disabled/grey and becomes enabled/blue only when state differs from the last save.

3. **Agency Edit Modal Expansion** — Two-column modal with Agency Info + Primary Contact (left) and Address (right). All fields persist via `AgencyAction`.

4. **Invite Modal** — Two-column modal for sending invitations. Left: New/Existing agency toggle, role selection (Agency Manager or Agent). Right: Contact info, rate pre-assignment checkboxes. Always rendered (not gated by selected agency).

### Invitation System (Full Flow)

Complete invitation workflow from PSP → Email → Registration → Login:

| Step | Component | Description |
|------|-----------|-------------|
| 1. Send | `SendInvitation` servlet | Creates Agency (if new), Person, Invitation record. Sends email via `EmailDAO.sendEmail()`. Pre-assigns rates if selected. |
| 2. Email | HTML email | Contains invite link: `/AcceptInvite?guid=xxx`. 30-day expiration. |
| 3. Register | `AcceptInvite` servlet + `acceptInvite.jsp` | Validates GUID. Agency Manager form: name, tax ID, address, password. Agent form: name, optional address, password. |
| 4. Login | Existing `AuthenticateUser` | New user logs in with email + password. Role-based access via `UserRole`. |

### Existing User Handling

When an invitation is accepted by someone who already has a User account:

| Scenario | Behavior |
|----------|----------|
| No agent/agency roles | Auto-grant role, add to agency, redirect to login |
| Agent in THIS agency, invite is Agency Manager | Upgrade role, set as manager, redirect to login |
| Agent in a DIFFERENT agency | Block with error message, require manual intervention |

---

## Database Changes

### New Table: `invitation`

| Column | Type | Notes |
|--------|------|-------|
| invitation_id | BIGINT PK | Auto-increment |
| guid | VARCHAR(36) UNIQUE | UUID invite token |
| email | VARCHAR(200) | Invitee email |
| first_name | VARCHAR(100) | |
| last_name | VARCHAR(100) | |
| agency_id | BIGINT FK | → agency |
| role | VARCHAR(20) | `AGENCY_MANAGER` or `AGENT` |
| invited_by | BIGINT FK | → assignee (PSP user who sent) |
| person_id | BIGINT FK | → assignee (Person created for invitee) |
| date_created | TIMESTAMP | Auto |
| date_expires | TIMESTAMP | 30 days from creation |
| date_accepted | TIMESTAMP | Set on registration |
| is_used | BOOLEAN | Set true on registration |

### Modified Table: `agency`

| Column | Type | Notes |
|--------|------|-------|
| manager_id | BIGINT FK (new) | → assignee (Person who is agency manager) |

### Migration Script

`docs/invitation_system_migration.sql` — Run on production before deploying invitation code.

---

## New Files

| File | Location | Purpose |
|------|----------|---------|
| `Invitation.java` | `model/sales/agency/` | JPA entity for invitation table |
| `SendInvitation.java` | `controller/activity/setup/` | POST servlet — creates invitation + sends email |
| `AcceptInvite.java` | `controller/activity/setup/` | GET: shows registration form. POST: creates User + roles |
| `acceptInvite.jsp` | `WEB-INF/view/sales/` | Public registration page (Agency Manager full form / Agent basic form) |

## Modified Files

| File | Changes |
|------|---------|
| `Agency.java` | Added `manager` field (Person, `@OneToOne`, `manager_id` FK) + getter/setter |
| `Invitation.java` | Added `person` field (Person, `@ManyToOne`, `person_id` FK) + getter/setter |
| `agencyManager25.jsp` | Invite button + modal, rate popover, rate state tracking, icon fixes |
| `LoginFilter.java` | Added `/AcceptInvite` to `ALLOWED_ENDPOINTS` |
| `PspAgencyHome.java` | Added `rateTableMap` loading for rate popovers |

---

## UserRole Reference (Standard Seed)

| ID | Description | Notes |
|----|-------------|-------|
| 1 | PSP User | Internal staff |
| 2 | Agent | Sales agent (assigned to agency) |
| 3 | Client | Employer client contact |
| 4 | Applicant | Created during proposal application |
| 5 | PSP Admin | Admin-level internal staff |
| 6 | Pending Agent | Pre-assignment holding role |
| 7 | Anonymous | Public/unauthenticated |
| 8 | Agency Admin | Agency Manager role |
| 9 | PSP Super User | Super admin |
| 101 | Accelergent BPO | Partner role |
| 102 | Accelergent Admin | Partner admin |
| 103 | Accelergent User | Partner user |

**Note:** Roles 1–9 should be seeded for every new PSP installation. Roles 101+ are partner-specific.

---

## AuthDAO.assignUserRoles Gap

The `AuthDAO.assignUserRoles()` switch statement currently handles roles 1–5 only. It does NOT set session flags for:
- Role 2 (Agent) — `isAgent` IS handled
- Role 8 (Agency Admin) — **NOT handled** — needs `isAgencyAdmin` session flag

**TODO:** Add `case 8: isAgencyAdmin=true;break;` and corresponding `request.getSession().setAttribute("isAgencyAdmin", isAgencyAdmin);` to enable role-based navigation for Agency Managers.

---

## Password Hashing Compatibility

`AcceptInvite` uses `AuthDAO.generateSalt()` and `AuthDAO.generatePasswordHash()` to ensure password hashes match the format used by `AuthenticateUser` login validation.

**Known quirk:** `AuthDAO.generateSalt()` calls `byte[].toString()` which produces a Java object reference string (e.g., `[B@1a2b3c`), not actual salt bytes. This is technically a bug but is consistent across all user creation paths, so login works correctly. Do not "fix" this without migrating all existing password hashes.

---

## Remaining Work

| Item | Priority | Notes |
|------|----------|-------|
| Manager badge on agent list | HIGH | Show badge when agent is agency manager |
| Role-based navigation | HIGH | Agency Manager/Agent see appropriate content after login |
| `AuthDAO.assignUserRoles` case 8 | HIGH | Enable `isAgencyAdmin` session flag |
| Pending invitations display | MED | Show on Agency Manager page |
| Resend invitation | MED | Regenerate GUID + extend expiry |
| Invitation expiry cleanup | LOW | Scheduled task or lazy cleanup |
| UserRole seed documentation | LOW | Ensure new PSP installs get all standard roles |
