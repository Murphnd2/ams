# Sales Pipeline Implementation Log — Session 3

**Date:** February 19-20, 2026
**Focus:** Application Form — Data Model, JSP, Polish, Billing Redesign, Save Progress

---

## Overview

Built the full prospect-facing application form. The form dynamically assembles sections based on which Lines of Service are included in the proposal. Includes conditional show/hide logic, IRS limit integration, a JSON-based benefit plan builder with file uploads, and auto-save.

---

## LOS Expansion

Split the old HRA/MERP (LOS 7) into 5 specific variants. Added billing and specialty LOSs.

| ID | Short | Description |
|----|-------|-------------|
| 11 | HRA | Health Reimbursement Arrangement |
| 12 | MERP | Medical Expense Reimbursement Plan |
| 13 | ICHRA | Individual Coverage HRA |
| 14 | EBHRA | Excepted Benefit HRA |
| 15 | QSEHRA | Qualified Small Employer HRA |
| 16 | RETIREE | Retiree Billing |
| 17 | DIRECT | Direct Billing |
| 18 | LSA | Lifestyle Spending Account |
| 19 | ADOPTION | Adoption Assistance |

---

## Application Section Architecture

**New tables:** `applicationsection`, `applicationsectionlos`

**Design:** Each section has a `scope` (ALL or LOS). ALL sections show for every proposal. LOS sections only show when the proposal includes a matching LOS (via `applicationsectionlos` join).

**20 sections total:** Company Info, Contact, Address, Plan Year, Pay Cycle, Signing Officer, Bank, Pre-Tax, 125 Features, FSA, HRA Design, HRA Carryover, HSA Funding, Transit, Billing, Payment, Debit Cards, LSA, Adoption, COBRA-Specific.

**~95 application fields** across all sections, with field types: TEXT, TEXTAREA, NUMBER, DATE, SELECT, RADIO, BOOLEAN, CHECKBOX, JSON.

---

## Entities Created

| Entity | Package | Purpose |
|--------|---------|---------|
| `ApplicationSection` | model/sales/application | Section with scope, sort order, LOS join |
| `IrsLimit` | model/general | Composite PK (limit_key, plan_year), dollar amounts |
| `IrsLimitId` | model/general | @IdClass for IrsLimit |
| `BillingType` | model/sales/offering | Rate structures (Tiered, Age-Rated, Flat, etc.) |
| `BenefitType` | model/sales/offering | Benefit categories with default billing type |

---

## Servlets Created

| Servlet | URL | Purpose |
|---------|-----|---------|
| `ApplyForProposal` | `/apply/*` | Public — loads proposal by GUID, assembles sections, renders form |
| `UploadRateSheet` | `/uploadRateSheet` | Public — AJAX file upload to Wasabi, returns JSON |
| `SaveApplicationProgress` | `/saveApplication` | Public — AJAX save of all field values |

**LoginFilter updates:** Added `/uploadRateSheet` and `/saveApplication` to allowed paths.

---

## JSP: applyForProposal.jsp

### Features
- Dynamic section assembly from servlet data
- Progress bar (counts filled visible fields)
- Conditional show/hide (19 rules including nested cascades)
- 2-column checkbox layout
- Radio label styling (fw-normal)
- IRS limit integration (dynamic help text, placeholders, dropdown options)
- JSON benefit plan builder with add/remove, tier templates, file upload
- Save Progress button + auto-save every 60 seconds
- Restore saved values on return visit
- beforeunload warning for unsaved changes
- Pre-populated defaults from prospect/contact data

### Conditional Rules
- Company structure "Other" → show describe field
- Eligibility entry/service "Other" → show describe fields
- Pay cycle 2 fields → show only if "have 2nd" = Yes
- Flex credit amount → show if flex credits checked
- FSA health limit/rollover → show if health FSA OR limited FSA checked (multi-trigger)
- FSA dep care limit/rollover → show if dep care checked
- Payment method → show only if facilitate = Yes
- Card acknowledge → gates all card setup fields
- Card copay details → show only if respective copay = Yes
- HRA flat/tier amounts → show based on benefit structure selection
- HRA carryover %/cap → show if carryover = Yes
- HRA spenddown details → show if spenddown = Yes
- HSA bank details → show only if funding via EFT

---

## IRS Limits

**Table:** `irslimit` with composite PK (limit_key, plan_year)

**Strategy:** Servlet loads current year, falls back to most recent if not found.

**2026 Published Limits:**
- Health FSA: $3,400
- Dep Care FSA: $7,500 (OBBBA legislative increase)
- FSA Carryover: $680
- HSA Single: $4,400 / Family: $8,750 / Catch-up: $1,000
- Transit/Parking: $340/month
- Adoption: $17,670
- QSEHRA Single: $6,450 / Family: $13,100
- EBHRA: $2,200

---

## Billing Administration Redesign

**Problem:** Old approach used individual fields per plan (bill_med_plan_name_1, etc.) — not scalable.

**Solution:** Single `bill_benefit_plans` JSON field. Interactive plan builder in JSP.

### Plan Builder UI
- Add/remove plan cards with accent border
- Benefit type dropdown (pre-selects default billing type)
- Billing type override dropdown
- Tier template presets (Single/Family, 3-tier, 4-tier, Custom)
- Flat rate input for flat-rate plans
- Notes textarea for age-rated/other plans
- Optional file upload (PDF, XLSX, XLS, CSV) to Wasabi
- Upload button and delete button inline in plan header

### JSON Structure
```json
[{
  "planName": "Blue Cross PPO",
  "benefitTypeId": 1,
  "benefitTypeName": "Medical",
  "billingTypeId": 1,
  "billingTypeName": "Tiered Rates",
  "effectiveDate": "2026-01-01",
  "renewalDate": "2027-01-01",
  "tiers": [{"name": "Single", "amount": "650"}, {"name": "Family", "amount": "1800"}],
  "storageKey": "uuid.pdf",
  "fileName": "rate_sheet.pdf"
}]
```

---

## Save Progress

**Endpoint:** `/saveApplication` (POST, multipart)

**Flow:**
1. Creates Application entity (status IN_PROGRESS) if not exists
2. Saves all field values as ApplicationFieldValue rows
3. Returns JSON with timestamp

**Auto-save:** Every 60 seconds if dirty (any input/change event).

**Restore:** On page load, servlet queries saved ApplicationFieldValues and merges into defaults map. All field types (text, select, radio, checkbox, date, number, textarea, JSON) restore correctly.

**HTML escaping:** JSON values stored in hidden field use `bill_benefit_plans_escaped` key with HTML entity encoding to prevent attribute breakage.

---

## DAO Updates

### SalesDAO
- `getIrsLimits(EntityManager em)` — loads current year IRS limits, falls back to most recent

---

## Files Created/Modified

### New Files
| File | Type |
|------|------|
| `controller/activity/setup/ApplyForProposal.java` | Servlet |
| `controller/activity/setup/UploadRateSheet.java` | Servlet |
| `controller/activity/setup/SaveApplicationProgress.java` | Servlet |
| `model/sales/application/ApplicationSection.java` | Entity |
| `model/general/IrsLimit.java` | Entity |
| `model/general/IrsLimitId.java` | IdClass |
| `model/sales/offering/BillingType.java` | Entity |
| `model/sales/offering/BenefitType.java` | Entity |
| `WEB-INF/view/sales/applyForProposal.jsp` | JSP |

### Modified Files
| File | Change |
|------|--------|
| `data/dao/SalesDAO.java` | Added getIrsLimits() |
| `LoginFilter.java` | Added /uploadRateSheet, /saveApplication to allowed paths |
| `model/sales/application/ApplicationField.java` | Changed FK from TemplatePurpose to ApplicationSection, added helpText |

---

## Database Changes

**Migration script:** `docs/sales_pipeline_migration_3.sql`

### New Tables
- `applicationsection` — 20 sections seeded
- `applicationsectionlos` — section-to-LOS scoping
- `irslimit` — 2025 + 2026 IRS limits (11 limit types × 2 years)
- `billingtype` — 5 rate structure types
- `benefittype` — 11 benefit categories with default billing types

### Modified Tables
- `applicationfield` — dropped template_purpose_id FK, added section_id FK + help_text
- `constant` — S3 constants needed (INSERT IGNORE, values must be filled in)

---

## What's Next (Session 4)

1. **Application submit handler** — POST endpoint that sets status to SUBMITTED, dateSubmitted
2. **Application review/approve UI** — PSP-side list + detail view, approve/deny actions
3. **Automated Setup creation** — on approval, create Setup + Checklist from application data
4. **Wire "Apply Now" button** on proposal landing page to `/apply/{guid}`
5. **Full pipeline test** — Create → Send → View → Apply → Save → Submit → Review → Approve
