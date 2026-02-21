# Session — February 21, 2026 — Layout & Navigation Consolidation

**Replaces:** `next_session_layout_consolidation.md` (completed)

## Summary

Unified the navbar, layout, and CSS across all main authenticated pages. Created a single branded navbar that serves all roles (PSP User, PSP Admin, Agent, Agency Manager) and converted 10+ standalone pages to use it.

## Unified Navbar (navbar25.jsp)

Rewrote `navbar25.jsp` as the single navigation component for all authenticated pages:

- **Dark branded bar** (`#0d5681`) with full-bleed viewport width (`100vw`)
- **Left side:** PSP icon (links to role-appropriate home) + page title via `pageTitle`/`pageIcon` request attributes
- **Right side (PSP User/Admin):** Home, Log, Email, Sales dropdown (Proposal Builder, Review Applications), Admin dropdown (Service Manager, Rate Manager, Agency Manager, Resource Library, Sequence Builder), Billing, Logout
- **Right side (Agent/Agency Manager):** Pipeline, New Proposal, Logout
- **Chatbot** gated to PSP users only (`isPspUser || isPspAdmin`)
- Collapses to hamburger on mobile

## adminNav.jsp Eliminated

The standalone `adminNav.jsp` dropdown (previously embedded in admin page headers) is now absorbed into the navbar's Admin dropdown. All admin pages removed their `adminNav.jsp` import.

## CSS Consolidation

- Added shared SSA utility classes to `css-js.jsp`: `:root` variables (`--ssa`, `--ssa-alt`, `--ssa-gray`), `.btn-ssa`, `.btn-outline-ssa`, `.hdr-bar`, `.item-card`, `.empty-state`, `.edit-link`
- Admin pages no longer load their own Bootstrap CDN — they use `css-js.jsp` (Bootstrap 5.3.3)
- Removed duplicate Bootstrap JS `<script>` tags from all converted pages (was breaking dropdowns)
- Page-specific styles remain in each JSP's `<style>` block (only what's unique to that page)

## Pages Converted

| Page | Changes |
|------|---------|
| `serviceManager25.jsp` | css-js.jsp, navbar25, removed standalone Bootstrap + adminNav |
| `rateManager25.jsp` | css-js.jsp, navbar25, removed standalone Bootstrap + adminNav |
| `agencyManager25.jsp` | css-js.jsp, navbar25, removed standalone Bootstrap + adminNav, removed max-width cap |
| `library25.jsp` | css-js.jsp, navbar25, removed standalone Bootstrap + adminNav |
| `reviewApplications.jsp` | css-js.jsp, navbar25, removed standalone Bootstrap + max-width cap |
| `reviewApplication.jsp` | css-js.jsp, navbar25, removed standalone Bootstrap + max-width cap |
| `proposalDetail.jsp` | css-js.jsp, navbar25, removed standalone Bootstrap + max-width cap |
| `proposalBuilder.jsp` | Added pageTitle/pageIcon for navbar display |
| `checklistDetail25.jsp` | Role gate fix — added `isAgent \|\| isAgencyAdmin` |

## Old Navbar Elimination

Swapped 12 JSP files from old `navbar.jsp` to `navbar25.jsp` via global find-and-replace:
- proposalBuilder, emailAttachments, fileUploadPage, loginPage, loginHelp, resetPassword, login.jsp, adminAgencyHome, adminRateHome, checklistHome, sequenceHome, renewHome

## Role-Aware Navigation Fixes

- `proposalDetail.jsp` — bottom buttons: "All Applications" gated to PSP only, Home/Pipeline role-aware
- `proposalDetail.jsp` — top header: added role-aware Home/Pipeline link
- `reviewApplications.jsp` — Home button role-aware (agents → AgentHome)
- `reviewApplication.jsp` — Home button role-aware

## Technical Notes

- Navbar uses `margin-left: calc(-50vw + 50%); width: 100vw;` for full-bleed regardless of parent container padding
- Bootstrap JS must only load once per page — duplicate loads break dropdowns
- CSS variables defined in `:root` in `css-js.jsp` require hardcoded fallbacks in inline styles (navbar background uses `#0d5681` directly)

## Remaining Work (Parked)

- **T10 — Agency Manager Dashboard:** Dedicated view for Agency Managers to manage their agents and view agency prospects/status. Separate from PSP's `agencyManager25.jsp`.
- **PSP Icon/Logo from Wasabi:** Add `ICON_URL` AppConstant for PSP-specific navbar icon. Upload mechanism in PSP Admin. Currently hardcoded to `logoD.png`.
- **Page title on remaining pages:** pspHome25, agentHome25, activityDetail25 don't set pageTitle yet (fine — they're dashboards/detail pages where the title isn't needed)
