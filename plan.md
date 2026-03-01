# Plan: J5 Benefit Year Import + Renewal Date Audit

## Problem
During Summit imports, `nextRenewalDue` is calculated as `effectiveDate + renewalMonths`, rolled forward. This assumes the effective date represents the annual renewal anchor. But for new clients with **short plan years** (e.g., FSA starts 6/1, but is a calendar-year plan), the first plan year is 6/1–12/31 and the true renewal date is 1/1 — not the 6/1 effective date.

The J5 export ("Benefit Plan Years") contains the actual plan year start/end dates per benefit. By importing this data and examining the most recent plan year's end date + 1 day, we can detect and correct inaccurate renewal dates.

## Current State
- J5 data is **not imported** by the wizard (only J1, J2, J3, J4, J7)
- `ImportBenefitYear` entity exists but is only used by the legacy billing `Importer`
- `ImportBenefitYear.EmployerPlan_ID` FK → `ImportBenefitCdh` (staging table, not always populated)
- `Benefit` entity has `effectiveDate`, `nextRenewalDue`, `renewalMonths` — no plan year fields
- No benefit list/audit view exists in the app

## Design

### 1. V028 Migration — Add plan year columns to `benefit`

```sql
ALTER TABLE benefit ADD COLUMN plan_year_start DATE NULL;
ALTER TABLE benefit ADD COLUMN plan_year_end DATE NULL;
```

Simple and direct. We store the **latest** plan year dates on the benefit itself. No new tables needed — we only need the most recent plan year per benefit for the audit, and this avoids the FK complications with the legacy staging tables.

### 2. Benefit.java — Add plan year fields

Add two nullable `java.sql.Date` fields:
- `planYearStart` → column `plan_year_start`
- `planYearEnd` → column `plan_year_end`

Add helper method:
- `getDetectedRenewalDate()` → if `planYearEnd` is set, returns `planYearEnd + 1 day` as `LocalDate`
- `hasRenewalMismatch()` → compares detected renewal date's month/day vs. `effectiveDate` month/day

### 3. SummitImportService — Add `importBenefitYears()` method

**Input:** J5 CSV file
**J5 CSV columns** (from existing `Importer` mapping):
`EmployerPlanDetailForPlanYear_ID`, `ContributionSchedule`, `ContributionScheduleTemplate_ID`, `Employer`, `Organization_ID`, `PlanDescription`, `PlanName`, `PlanStatus`, `PlanYear`, `PlanYear_ID`, `EmployerPlan_ID`

**Logic:**
1. Parse all J5 rows into a `Map<Integer, List<PlanYearRow>>` keyed by `EmployerPlan_ID`
2. For each `EmployerPlan_ID`, find the row with the **latest plan year end date**
3. Look up the `Benefit` via `summitId = EmployerPlan_ID` AND `sourceType = 'CDH'`
4. If benefit found:
   - Set `benefit.planYearStart` and `benefit.planYearEnd` from the latest plan year
   - Compute `detectedRenewalDate = planYearEnd + 1 day`
   - Compare `detectedRenewalDate` month/day vs `benefit.effectiveDate` month/day
   - If different: recalculate `nextRenewalDue` using the detected renewal anchor (instead of effective date), rolled forward to the next future occurrence. Count as "adjusted."
   - If same: no change needed. Count as "matched."
5. If benefit not found: count as "skipped" with warning

**Returns:** `ImportResult` with inserted=0, updated=adjusted count, skipped=unmatched, errors

### 4. SummitImportWizard — Add J5 file upload

**Step 1 (Upload):**
- Add 7th file upload card: "Benefit Plan Years (J5)" — CSV
- New session attribute: `SI_BENEFIT_YEAR_FILE`
- Validation: J5 requires that J4 (CDH benefits) was also uploaded (plan years reference CDH benefits)

**Step 2 (Configure):**
- Show J5 file summary (filename, row count, headers) like other files
- No special configuration needed for J5

**Import execution order** (after existing 5 imports):
1. Plan Types (Excel)
2. Employers (J1)
3. Employees (J2 + J3)
4. Benefits CDH (J4)
5. Benefits COBRA (J7)
6. **Benefit Plan Years (J5)** ← new, runs after benefits exist

**Step 4 (Results):**
- Show J5 results row: "Benefit Plan Years" — matched / adjusted / skipped / errors
- Adjusted count tells the user how many renewal dates were corrected
- Expandable warnings show which benefits had no match

### 5. Benefit Renewal Audit View (new admin page)

**Servlet:** `BenefitAudit25.java` at `@WebServlet("/BenefitAudit")`
**JSP:** `benefitAudit25.jsp`
**Access:** PSP Admin only (same gate as Summit Import)

**List view columns:**

| Column | Source | Notes |
|--------|--------|-------|
| Employer | `benefit.employer.employerName` | Sortable |
| Plan Name | `benefit.planName` | |
| Plan Type | `benefit.planType.planTypeName` | |
| Effective Date | `benefit.effectiveDate` | Original from J4 |
| Plan Year | `benefit.planYearStart` – `benefit.planYearEnd` | From J5 import (may be null) |
| Detected Renewal | Computed: `planYearEnd + 1 day` | Shown only when plan year data exists |
| Next Renewal Due | `benefit.nextRenewalDue` | **Editable** (date picker) |
| Renewal Months | `benefit.renewalMonths` | **Editable** (dropdown: 12, 24, 36) |
| Status | Mismatch badge | Red badge if effective date M/D ≠ detected renewal M/D |

**Features:**
- Toggle: "Show mismatches only" — filters to benefits where plan year data exists and detected renewal differs from effective date
- AJAX save: clicking save on a row POSTs updated `nextRenewalDue` and `renewalMonths` to the servlet
- Bulk action: "Accept all detected dates" — updates all mismatched benefits to use the detected renewal date

**Navigation:** Accessible from the admin/settings area (same place as Summit Import link)

## Data Flow Example

**Scenario:** FSA benefit, effective date 6/1/2025, calendar-year plan

```
J4 import:
  EmployerPlan_ID=500, EffectiveDate=06/01/2025
  → Benefit created: effectiveDate=6/1/2025, nextRenewalDue=6/1/2026 (12-month default)

J5 import:
  EmployerPlan_ID=500, PlanYear="06/01/2025-12/31/2025" (short first year)
  EmployerPlan_ID=500, PlanYear="01/01/2026-12/31/2026" (full calendar year)

  → Latest plan year end = 12/31/2026
  → Detected renewal = 1/1 (12/31 + 1 day)
  → Effective date month/day = 6/1 ≠ 1/1 → MISMATCH
  → Recalculate: nextRenewalDue = 1/1/2027 (next future 1/1)
  → Benefit updated: planYearStart=1/1/2026, planYearEnd=12/31/2026, nextRenewalDue=1/1/2027
```

Audit view shows: effective=6/1, detected renewal=1/1, nextRenewalDue=1/1/2027 with green checkmark (already corrected).

## Build Order (one step at a time)

1. **V028 migration script** — `ALTER TABLE benefit ADD COLUMN plan_year_start / plan_year_end`
2. **Benefit.java** — Add `planYearStart`, `planYearEnd` fields + helper methods
3. **SummitImportService.importBenefitYears()** — J5 CSV parsing and renewal correction logic
4. **SummitImportWizard** — Add J5 session attribute, upload handling, import call
5. **step1Upload.jsp** — Add J5 file upload card
6. **step2Configure.jsp** — Show J5 summary
7. **step4Results.jsp** — Show J5 import results
8. **BenefitAudit25.java** — Servlet for the audit/edit view
9. **benefitAudit25.jsp** — Audit list view with inline editing
10. **Navigation link** — Add Benefit Audit to admin menu
11. **Compile + test**
