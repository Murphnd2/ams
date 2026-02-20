# Servlet Inventory Update — February 20, 2026

**Add these entries to `docs/analysis/servlet_inventory.md`:**

---

## controller/activity/setup/ — New Servlets

| Servlet | URL | Method | Purpose |
|---------|-----|--------|---------|
| `PspAdminHome` | `/PspAdminHome` | GET | Rate Manager home. Loads rates, fee types, modules, agencies, locked rate detection. |
| `RateTableAction` | `/RateTableAction` | POST | Rate Manager actions: createRate, editRate, addRateTableRow, deleteRow, assignAgencyToRate, removeAgencyFromRate, createPriceItem, cloneRate. |
| `PspAgencyHome` | `/PspAgencyHome` | GET | Agency Manager home. Loads agencies, agents, rates for selected agency. |
| `AgencyAction` | `/AgencyAction` | POST | Agency Manager actions: createAgency, editAgency, addAgentToAgency, removeAgentFromAgency, removeRateFromAgency. |
| `PriceItemAction` | `/PriceItemAction` | POST (AJAX) | Fee type reorder and suppress toggle. Returns JSON. |
| `ServiceModuleAction` | `/ServiceModuleAction` | POST (AJAX) | Service module reorder and suppress toggle. Returns JSON. |

## JSP Additions

| JSP | Location | Servlet |
|-----|----------|---------|
| `rateManager25.jsp` | `/WEB-INF/view/sales/rateManager25.jsp` | PspAdminHome |
| `agencyManager25.jsp` | `/WEB-INF/view/sales/agencyManager25.jsp` | PspAgencyHome |
