# Rate Manager & Agency Manager — Build Log

**Last Updated:** February 20, 2026

---

## Overview

Built admin UI for managing the pricing backbone: PriceItems (fee types), Rates (pricing tiers), RateTables (fee amounts per rate/module), Agency assignments, and Service Modules.

---

## Files Created

### Servlets (controller/activity/setup/)

| File | Purpose |
|------|---------|
| `PspAdminHome.java` | Rate Manager home servlet. Loads rates, fee types, modules, agencies. Computes locked rate IDs via proposal count. |
| `RateTableAction.java` | POST handler for Rate Manager: createRate, editRate, addRateTableRow, deleteRow, assignAgencyToRate, removeAgencyFromRate, createPriceItem, cloneRate (copy-on-write). |
| `PspAgencyHome.java` | Agency Manager home servlet. Loads agencies, agents, rates. |
| `AgencyAction.java` | POST handler for Agency Manager: createAgency, editAgency, addAgentToAgency, removeAgentFromAgency, removeRateFromAgency. |
| `PriceItemAction.java` | AJAX servlet for fee type reorder (drag-and-drop sort save) and suppress toggle. |
| `ServiceModuleAction.java` | AJAX servlet for service module reorder and suppress toggle. |

### JSPs (webapp/WEB-INF/view/sales/)

| File | Purpose |
|------|---------|
| `rateManager25.jsp` | Rate Manager UI — 2-column layout with rates, agencies, tabbed fee types/modules on left; pricing grid on right. |
| `agencyManager25.jsp` | Agency Manager UI — agency list, agent management, rate assignments. |

### DAO Additions (data/dao/SalesDAO.java)

| Method | Purpose |
|--------|---------|
| `getProposalCountByRate(em, rateId)` | Counts proposals referencing a rate — drives lock detection. |

---

## Key Architecture Decisions

### Rate Locking (Copy-on-Write)

- Rates are freely editable until a Proposal references them via `proposal.rate_id`
- Once referenced, the Rate becomes **locked** (shown with lock icon, edit controls hidden)
- "Editing" a locked Rate triggers **cloneRate**:
  1. Creates new Rate entity with same name
  2. Copies all RateTable rows to new Rate
  3. Moves agency assignments from old → new (snapshot IDs first to avoid concurrent modification)
  4. Suppresses old Rate (`isSuppressed=true`) — disappears from lists but proposals retain FK reference
  5. Redirects to the new editable Rate
- No date tagging on cloned names — proposals reference by FK, not display name
- `getRateList()` already filters `isSuppressed=false`, so suppressed rates are invisible to ProposalBuilder

### Fee Type & Module Management

- **Drag-and-drop sorting** via HTML5 drag events, AJAX save on drop
- **Suppress toggle** with confirm dialog, AJAX toggle, inline UI update
- Suppressed items hidden by default, eye-slash toggle to reveal
- Suppressed items dynamically removed from Add Pricing Row modal dropdowns (no refresh needed)
- New fee types auto-assigned sort order (max existing + 100)
- Fee Types and Service Modules share a tabbed interface with tab-aware suppress toggle and add button

### Agency Assignment

- Assign Agency modal filters to show only unassigned agencies
- Agency chips display inline with delete buttons
- Locked rates hide all agency modification controls

---

## Layout (rateManager25.jsp)

```
┌─────────────────────────────────────────────────────────────┐
│ Rate Manager          $ Rate Name ✏        [Agencies] [Home]│
├──────────────┬──────────────────────────────────────────────┤
│ [Rates]  [+] │ [Pricing Grid]                          [+] │
│  Standard    │  ┌─ Module: FSA ─────────────────────────┐  │
│  Premium     │  │ Monthly Admin Fee          $12.50     │  │
│  Custom      │  │ Setup Fee                  $250.00    │  │
│              │  └───────────────────────────────────────┘  │
├──────────────┤  ┌─ Module: COBRA ───────────────────────┐  │
│ [Agencies][+]│  │ Monthly Admin Fee          $8.00      │  │
│  ABC Corp    │  └───────────────────────────────────────┘  │
│  XYZ Inc     │                                             │
├──────────────┤                                             │
│ Fee Types|Mod│                                             │
│  Admin Fee   │                                             │
│  Setup Fee   │                                             │
│  Annual Fee  │                                             │
└──────────────┴─────────────────────────────────────────────┘
```

**Color scheme:**
- All section headers: `--ssa-primary` (#0d5681)
- Tab bar: `--ssa-primary` (#0d5681)
- Pricing grid module subheaders: `--ssa-alt` (#87a948)
- Nav buttons: `btn-ssa` (solid primary) for Home, `btn-outline-ssa` for Agencies

**Scroll constraints (desktop):**
- Rate list: max 4 rates visible (180px), then scrollbar
- Tabbed section: fills remaining screen height
- Pricing grid: fills remaining screen height

---

## Site Color Reference

| Variable | Hex | Usage |
|----------|-----|-------|
| `--ssa` / primary | `#0d5681` | `.btn-ssa`, `.bg-ssa`, `.hdr-bar`, tab bar |
| `--ssa-alt` / secondary | `#87a948` | `.btn-altSsa`, module subheaders |
| hover primary | `#06357a` | Button hover states |

---

## Entity Relationships (Sales Domain)

```
PSP ──1:M──> Rate ──1:M──> RateTable ──M:1──> PriceItem
                │                    └──M:1──> ServiceModule
                │
                └──M:N──> Agency ──M:N──> Person (agents)
                                └──1:1──> Address
                                └──1:1──> Person (primaryContact)

Proposal ──M:1──> Rate (FK preserved even when Rate suppressed)
         ──M:1──> Prospect ──M:1──> Person (agent)
         ──M:N──> LOS ──M:N──> ServiceModule
```
