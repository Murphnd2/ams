# Session — February 23–24, 2026 — Activity Detail GUI Modernization

## Summary

Comprehensive GUI modernization of the Activity Detail page (`activityDetail25.jsp`), completing Track A items A1–A7, A11, A12, plus infrastructure improvements including resizable panel layout, Wasabi document upload, and Quill editor integration.

## Completed Track A Items

### A1+A2: Section Headers
- Checklist header (`checklistHeader.jsp`) → `.hdr-bar` pattern
- History header (`historyHeader.jsp`) → `.hdr-bar` pattern

### A3+S1+S2: Detail Header (`detailHeader25.jsp`)
- SSA blue `.hdr-bar` with back arrow, activity title, type badge pill
- Driver subtitle row (ticket subcategory, renewal employer, setup application, opportunity stage)
- Closed activity banner (yellow warning bar)
- Inline action icons: key (change owner), calendar (change due date), archive (past activities)
- Icons gated with `pe-none` for closed activities; archive always visible

### A4: Primary Contact (`detailPrimaryContact25.jsp`)
- Card with left SSA-blue border
- Compact label, pencil icon edit (opens `modContact25.jsp` modal)

### A5: Type-Specific Detail Panels
- **`detailTicket25.jsp`** — Card with expandable modal, conditional expand button
- **`detailRenewal25.jsp`** — Card with left blue border, shield-check icon, benefit list with remove buttons, max-height 120px scroll, expand modal
- **`detailSetup25.jsp`** — Data-driven module iteration (eliminated 8 hardcoded `c:if` blocks), SSA-blue badges, max-height scroll
- **`detailOpportunity25.jsp`** — Two-card layout: Opportunity Details (stage, prospect, agency, owner, managed by) + Proposals (ID link, status badge, LOS chips). Inline create-proposal icon.

### A6: Add Note Section (`detailAddNote25.jsp`)
- Moved from center column to top of right column (above history)
- **Replaced CKEditor with Quill** — dramatically smaller toolbar, controllable borders
- `.hdr-bar` header with inline Reason + Status dropdowns (transparent/white toggle)
- Collapsible body with chevron rotation
- Floppy disk save icon
- **Resizable editor** — CSS `resize: vertical` on Quill container, height persisted to `localStorage`
- **Tab key** exits editor to save button (Quill tab bindings cleared)
- Form wraps entire component (header + body) for dropdown inclusion

### A7: Footer Decomposition
The monolithic `detailFooter25.jsp` button row was broken into purpose-specific locations:

| Old Footer Item | New Location |
|---|---|
| Other Contacts button | `detailAdditionalContacts25.jsp` — card below primary contact |
| Docs & Links button | `detailDocsLinks25.jsp` — card below type-specific detail |
| Past Activities button | Archive icon in detail header |
| Owner button | Key icon in detail header |
| Due Date (inside owner modal) | Calendar icon in detail header (separate modal) |
| Setup/Renewal modal imports | Remain in slimmed-down footer |

**Additional Contacts card (`detailAdditionalContacts25.jsp`):**
- Always-visible header with plus-circle add button
- Empty state message when no contacts
- Compact flex rows: name, email link, x-circle remove
- Max-height 100px with overflow scroll
- `addContactToActivity25.jsp` modal import outside conditionals

**Documents & Links card (`detailDocsLinks25.jsp`):**
- Left border in `--ssa-gray`
- Upload doc and add link icons in header (gated for closed activities)
- Documents: file-download icon in SSA blue
- Links: link icon in SSA green
- Max-height 120px with conditional expand button
- Full-list modal for overflow

### A11: History Body (`historyDetail25.jsp`)
- Removed `container-fluid` wrapper and `max-height: 700px`
- Fixed duplicate date display bug
- SSA styling: reason in SSA blue, status as subtle badge pill, email notes get envelope icon
- Empty state for activities with no notes
- Right column flex layout: history fills remaining space below note editor

### A12: Modal Standardization
All modals updated to SSA pattern: `modal-sm`, SSA blue header, white text, relevant icon, `form-control-sm`, `btn-ssa`.

| Modal | Changes |
|---|---|
| `ownerModal25.jsp` | **Split into two:** Change Owner modal + Change Due Date modal (separate triggers) |
| `pastActivityModal25.jsp` | Compact flex rows with status badges, open/closed indicators |
| `addContactToActivity25.jsp` | Email field + optional employee select with divider |
| `modContact25.jsp` | Stacked label/input layout, SSA labels |
| `addDocumentToActivityMod.jsp` | File upload + optional display name, posts to new Wasabi servlet |
| `addUrlToActivityMod.jsp` | URL + optional display name |

## Infrastructure Changes

### Resizable Three-Panel Layout
- **Single layout** replaces duplicate mobile/desktop blocks — eliminates duplicate modal ID conflicts
- CSS media queries handle responsive behavior:
  - `≥1200px`: flex row, drag dividers visible, panels side-by-side
  - `768–1199px`: flex-wrap, center on top, left/right split 50%
  - `<768px`: full-width stacking
- **Drag dividers** between panels: 3px visible bar inside 9px hit zone, SSA blue on hover, grip marks at midpoint
- **Panel widths persisted** to `localStorage` (`actDetailPanels` key)
- `min-width` constraints prevent panel collapse

### Wasabi Document Upload (`AddDocumentToActivity25.java`)
**New servlet** — replaces deleted `AddDocumentToActivity.java` (was using local disk storage).
- Modeled on `AddAttachment25` pattern
- Uploads via `StorageDAO.uploadFile()` to Wasabi S3
- Creates `WebLink` (linkType 1) and attaches to Activity's `webLinkList`
- `@MultipartConfig` with 10MB file limit

### Download Link Fix
- Changed from dead `DownloadActivityDoc?fileId=` to working `ShowFileUpload?doc=`
- `ShowFileUpload` generates pre-signed Wasabi URL (1-hour expiry)

### CKEditor → Quill Migration
- Removed CKEditor CDN from page `<head>`
- Quill loaded via CDN (`quill@2.0.3`)
- Toolbar: bold, italic, ordered list, bullet list, link
- Custom CSS: compressed toolbar (22px buttons), 0.8rem font, system font stack
- `ResizeObserver` persists editor height to `localStorage`

## Files Created

| File | Location |
|---|---|
| `AddDocumentToActivity25.java` | `controller/activity/` |
| `detailAdditionalContacts25.jsp` | `activityDetail/columns/detail/` |
| `detailDocsLinks25.jsp` | `activityDetail/columns/detail/` |

## Files Modified/Rewritten

| File | Changes |
|---|---|
| `activityDetail25.jsp` | Single-layout with CSS media queries, resizable panels, panel divider CSS+JS |
| `detailHeader25.jsp` | Owner/date/archive icons added |
| `detailAddNote25.jsp` | Complete rewrite — Quill, collapsible, resizable |
| `detailRenewal25.jsp` | Card-based with scroll/expand |
| `detailSetup25.jsp` | Data-driven module iteration |
| `detailOpportunity25.jsp` | Two-card layout |
| `detailFooter25.jsp` | Stripped to modal imports only |
| `historyDetail25.jsp` | SSA styling, fixed duplicate date bug |
| `ownerModal25.jsp` | Split into owner + due date modals |
| `pastActivityModal25.jsp` | SSA compact layout |
| `addContactToActivity25.jsp` | SSA modal |
| `modContact25.jsp` | SSA modal |
| `addDocumentToActivityMod.jsp` | SSA modal, posts to new Wasabi servlet |
| `addUrlToActivityMod.jsp` | SSA modal |

## No Database Changes

No SQL migrations this session — all changes are JSP/UI, CSS, and one new servlet.

## Remaining Track A Items

| Item | Description | Notes |
|---|---|---|
| A8 | Checklist body (ToDo list) | Biggest single piece — card-based rows with left-border urgency, kebab menus |
| A9 | Checklist automation section | Review and modernize |
| A10 | Checklist footer | SSA button styling |
| A13 | Closed activity banner | Restyle yellow completed banner |
| A14 | Auto-save UX | Visual indicator for pending/saved state |
| S4 | Standardize `pe-none` gating | Currently inconsistent across panels |
| S5 | Mobile stacking polish | Verify responsive layout at all breakpoints |
