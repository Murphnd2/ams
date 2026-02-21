# Rate Manager Session 2 — Build Log
**Date:** February 20, 2026

## Overview

Reworked the Rate Manager add-pricing workflow to use LOS/Enhancement selection instead of raw ServiceModule selection. Added per-rate sort ordering, inline price editing, rate copying, and updated proposal pages to use the new sort order and display Enhancement pricing/features.

---

## Database Changes (APPLIED to beta_ssa)

### Migration: `rate_manager_sort_migration.sql`
1. **`ratetable.sort_order`** — `INT NOT NULL DEFAULT 0` — per-rate ordering of pricing groups
2. **Backfill** — copied `servicemodule.sort_order` into each ratetable row as initial value

---

## Files Modified

### Servlets

| File | Changes |
|------|---------|
| `PspAdminHome.java` | Loads LOS list, Enhancement list (non-suppressed, sorted). Builds `usedFeesJson` (module→used fee type IDs) and `entityModuleMapJson` (los/enh→moduleId) for smart filtering in add-row modal. Builds `rateModuleList` (distinct modules in selected rate's pricing, for Grid Sort tab). |
| `RateTableAction.java` | `addRateTableRow` reworked: accepts `losId` or `enhId`, finds-or-creates ServiceModule, auto-assigns per-rate `sortOrder`. New actions: `copyRate` (duplicate rate with pricing, no suppress), `updatePrice` (inline AJAX price edit), `reorderModules` (per-rate Grid Sort drag save). `cloneRate` updated to copy `sortOrder`. |
| `ViewProposal.java` | Feature loading now uses module IDs extracted from pricing list (includes enhancement modules) instead of `getDistinctListOfServiceModulesForThisProposal()`. |

### DAO

| File | Changes |
|------|---------|
| `SalesDAO.java` | `getRateTableList()` ORDER BY changed from `rt.module.sortOrder` to `rt.sortOrder` (per-rate sort). `getPricing()` rewritten as two-query approach: query 1 gets LOS-linked module rows, query 2 gets Enhancement-linked module rows via `enhancement_los` join. Results merged, deduped, sorted by `rt.sortOrder`. |

### Entities

| File | Changes |
|------|---------|
| `RateTable.java` | Added `sortOrder` field (int, mapped to `sort_order` column). |

### JSPs

| File | Changes |
|------|---------|
| `rateManager25.jsp` | **Add Pricing Row modal**: LOS/Enhancement dropdown with optgroups replaces ServiceModule dropdown. Fee Type dropdown smart-filtered (excludes already-used fee types for selected LOS/Enhancement in this rate). **Pricing grid headers**: show LOS or Enhancement name instead of raw module description, group by module ID. **Modules tab → "Grid Sort" tab**: renamed, shows only modules in selected rate, drag reorder saves per-rate sort via `RateTableAction.reorderModules`. Suppress toggles removed from Grid Sort. **"Make New From" button**: copies rate with new name (available locked or not), client-side name uniqueness validation. **Inline price editing**: click price to edit, Enter/blur saves via AJAX, Escape cancels. |
| `viewProposal.jsp` | **Enhancement feature cards**: after LOS cards, renders feature cards for enhancement modules (accent-colored header, puzzle icon). Only shows if features exist for that enhancement module. De-duplicates by enhancement ID. **Pricing headers**: show LOS/Enhancement name instead of module description, group by module ID. |
| `proposalDetail.jsp` | **Pricing headers**: show LOS/Enhancement name instead of module description, group by module ID. |

---

## Key Architecture Decisions

### Add Pricing Row — LOS/Enhancement Selection
- User selects a LOS or Enhancement (not a ServiceModule) from an optgroup dropdown
- System finds existing ServiceModule with matching `los_id` or `enhancement_id` FK, or auto-creates one
- Fee Type dropdown smart-filters to show only fee types not yet assigned to that LOS/Enhancement in the current rate
- This hides the ServiceModule abstraction from the user entirely

### Per-Rate Sort Order
- `ratetable.sort_order` column allows each rate to order its pricing groups independently
- New rows inherit sort order from existing rows for the same module, or `max + 100` for new modules
- Grid Sort tab (left column) shows only modules in the selected rate, drag-to-reorder saves per-rate
- `cloneRate` and `copyRate` both preserve sort order

### Enhancement Pricing in Proposals
- `SalesDAO.getPricing()` uses two separate queries (LOS-linked + Enhancement-linked) merged together
- Single JPQL query with OR/subquery failed in EclipseLink; two-query approach is reliable
- Enhancement modules are found via: `ServiceModule.enhancement_id` → `enhancement_los` → proposal's LOS IDs
- Features loaded using module IDs from pricing list, not from old `getDistinctListOfServiceModulesForThisProposal()`

### Rate Copying vs Cloning
- **cloneRate** (existing): for locked rates only. Creates copy, moves agency assignments, suppresses original. Preserves proposal FK integrity.
- **copyRate** (new): available for any rate. Creates copy with new name, copies pricing and sort order. Does NOT copy agency assignments or suppress original. For speed in building similar rates.

---

## Entity Relationships (Updated)

```
PSP ──1:M──> Rate ──1:M──> RateTable ──M:1──> PriceItem
                │              │          └──M:1──> ServiceModule ──M:1──> LOS
                │              │                                   └──M:1──> Enhancement
                │              └── sort_order (per-rate ordering)
                │
                └──M:N──> Agency

Enhancement ──M:N──> LOS (via enhancement_los)
            ──M:N──> ApplicationSection (via applicationsectionenhancement)

Feature ──M:1──> ServiceModule (module_id FK)
```

Proposal pricing flow:
1. Proposal selects Rate + LOSs
2. `getPricing()` finds RateTable rows where module links to selected LOSs (direct) OR to Enhancements associated with selected LOSs
3. Results ordered by `ratetable.sort_order` (per-rate custom order)
