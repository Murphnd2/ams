# Summit Data Import & Sync — Design Document

**Created:** February 28, 2026
**Status:** Draft — pending review
**Backlog Item:** D-35 (new)

---

## 1. Purpose

Build a robust data import/sync process that brings Summit data into AMS. This serves two needs:

1. **Initialization** — First run on a freshly deployed PSP instance, populating employers, employees, benefits, and plan types from Summit exports
2. **Ongoing Sync** — Subsequent runs that update existing records, add new ones, and flag terminated items. Summit remains the source of truth for entity data.

The design is **vendor-agnostic** at the CSV layer — while Summit/DataPath is the primary source, the column mapping approach allows adaptation to other benefit administration platforms.

---

## 2. Data Scope

### What We Import

| Data Type | Summit Export | Report Type | Target Table | Purpose |
|-----------|-------------|-------------|-------------|---------|
| **Plan Types** | Pre-defined export (Excel) | — | `plantype` | Reference data — categorizes benefits (FSA, HRA, COBRA, etc.) |
| **Employers** | J1_Employer (CSV) | Employer Listing | `employer` | Client companies the PSP manages |
| **Employees (Contact)** | J2_Employee (CSV) | Participant Listing Simple | `employee` | Names, email, addresses, custom IDs |
| **Employees (Status)** | J3_Employee_Alt (CSV) | Participant Listing Report with Division Option | `employee` | Status IDs, lifecycle dates (effective/term/hire) |
| **Benefits** | J4_Benefits (CSV) | Employer Benefit Plans | `benefit` | Plans each employer offers — drives renewal pipeline |

**Why two employee files?** Summit's pre-built exports are not configurable — all columns are sent for a given report type. Neither employee export contains all the fields needed for sync:
- **J2 (Simple)** has contact info (email, address, city, state, zip, custom ID) but no status or dates
- **J3 (Division)** has status IDs and lifecycle dates but no contact info

The sync process joins both on `Participant_ID` to build the complete employee record. Banking/SSN/reimbursement fields in J3 are ignored (billing-only).

### What We Do NOT Import (billing-specific)

- Enrollments (I6) — monthly billing
- Benefit Years (I5) — billing cycles
- COBRA QB/terms/coverage (I7, I8, I9, IA, IB) — COBRA billing
- HSA accounts — billing
- J3 billing-only columns: SSN, BankName, RoutingNo, AccountType, AccountNumber, UserBank_ID, ReimbursementMethod, ReimbursementMethod_ID, No_of_participants, DivisionName, ByDivision

---

## 3. Summit Export Specifications

### 3a. Plan Types (Excel)

**Source:** Summit → Processing → Data Exchange → Exports (pre-defined "Plan Type" report)

| Column | Type | Example | Maps To |
|--------|------|---------|---------|
| Plan Type ID | Integer | 1, 1001, 1008 | `plantype.PlanType_ID` |
| Plan Type Code | String | FSA, HRA, DRiP | `plantype.Code` |
| Plan Type Name | String | Medical Flexible Spending Account | `plantype.PlanTypeName` |
| Created Date | String | 09/09/2013 | (informational) |
| Modified Date | String | (often blank) | (informational) |
| Level | String | System Default / TPA Custom / Employer Custom | NEW — store for reference |
| LOS | String | CDH / COBRA / "CDH, COBRA" | NEW — Line of Service |
| Employer Name | String | (only for Employer Custom) | NEW — for employer-custom types |

**Level breakdown:**
- **System Default** (IDs 1–16) — exists for all Summit clients
- **TPA Custom** (IDs 1001+) — unique to this PSP
- **Employer Custom** — unique to a specific employer

### 3b. Employers (CSV)

**Source:** Summit → Data Exchange → Employer Listing (J1)

| CSV Column | Maps To | Notes |
|-----------|---------|-------|
| OrganizationID | `employer.organization_id` (PK) | Summit's primary employer ID |
| Employer_ID | `employer.employer_id` | Alternate ID |
| EmployerName | `employer.employer_name` | |
| CustomID | `employer.er_key` | DPI Suite ER Key |
| Status | — | **Filter:** skip inactive |
| OrganizationStatusID | — | **Filter:** numeric status code |
| PrimaryContact | `employer.contact_name` | |
| Phone / PhoneNumber | `employer.phone` | |
| Email | `employer.email` | |
| TaxID | — | Available but not currently mapped to employer table |
| SetUpComplete | — | Informational |
| ImplementationLead | — | Informational |

**Active filter logic:** Import only rows where Status indicates active (exact values TBD — need to confirm Summit's status codes with Kevin).

### 3c. Employees — Two Files Required

Both employee exports share `Participant_ID` as the join key. The sync process merges them into a single `employee` record.

#### J2 — Participant Listing Simple (contact info)

**Source:** Summit → Data Exchange → Participant Listing Simple

| CSV Column | Maps To | Notes |
|-----------|---------|-------|
| Participant_ID | `employee.employee_id` (PK) | Summit's participant ID |
| Organization_ID | `employee.employer_id` (FK) | Links to employer |
| FirstName | `employee.first_name` | |
| LastName | `employee.last_name` | |
| Email | `employee.email` | **J2-only** |
| Address1 | `employee.address1` | **J2-only** |
| Address2 | `employee.address2` | **J2-only** |
| City | `employee.city` | **J2-only** |
| State | `employee.state` | **J2-only** |
| ZipCode | `employee.zip` | **J2-only** |
| ParticipantCustomID | `employee.custom_id` | **J2-only** — employer's custom ID |
| User_ID | `employee.user_id` | Portal user ID |
| UserStatus | — | Text status (use J3 numeric IDs instead) |
| Employer_ID | — | Alternate employer ID (skip) |
| EmployerName | — | Denormalized (skip) |
| EmployerCustomID | — | Informational |
| EmployerOrganizationID | — | Duplicate of Organization_ID |
| SetupCompletionDate | — | Informational |
| IsRegisterdToPortal | — | Informational |
| FailedLoginCount | — | Informational |
| LastLoginDate | — | Informational |

#### J3 — Participant Listing Report with Division Option (status + dates)

**Source:** Summit → Data Exchange → Participant Listing Report with Division Option

| CSV Column | Maps To | Notes |
|-----------|---------|-------|
| Participant_ID | (join key) | Matches J2 PK |
| Organization_ID | (FK validation) | Should match J2 |
| Participant First name | — | Name available in J2 (prefer J2 for consistency) |
| Participant Last Name | — | Name available in J2 |
| ParticipantStatusId | `employee.ee_status_id` | **J3-only** — employment status code |
| userStatusID | `employee.system_status_id` | **J3-only** — system active/inactive |
| EmploymentStatusID | — | Alternate status ID |
| EmploymentStatus | — | Text label for EmploymentStatusID |
| EffectiveDate | (new column TBD) | **J3-only** — employee effective date |
| TerminationDate | (new column TBD) | **J3-only** — employee termination date |
| HireDate | (new column TBD) | **J3-only** — hire date |
| ParticpantStatusDescription | — | Text label for ParticipantStatusId |
| User Status Description | — | Text label for userStatusID |
| DOB | — | Available but not currently mapped |
| ERName | — | Denormalized employer name (skip) |
| EmployerOrganizationID | — | Duplicate of Organization_ID |
| ParticipantName | — | Concatenated name (skip — use J2 first/last) |
| UserId | — | Duplicate of J2.User_ID |
| CreatedDate | — | Informational |
| SSN | — | **Billing-only** (ignored) |
| ReimbursementMethod | — | **Billing-only** (ignored) |
| ReimbursementMethod_ID | — | **Billing-only** (ignored) |
| BankName | — | **Billing-only** (ignored) |
| RoutingNo | — | **Billing-only** (ignored) |
| AccountType | — | **Billing-only** (ignored) |
| AccountNumber | — | **Billing-only** (ignored) |
| UserBank_ID | — | **Billing-only** (ignored) |
| No: of participants | — | **Billing-only** (ignored) |
| DivisionName | — | **Billing-only** (ignored) |
| ByDivision | — | **Billing-only** (ignored) |

**Merge logic:** J2 is the primary source (contact info + names). J3 supplements with status IDs and dates. Records are matched on `Participant_ID`. A participant present in J3 but missing from J2 still gets created (using J3's name fields as fallback).

**Active filter logic:** Use J3's `userStatusID` to determine active/inactive. Skip employees whose Organization_ID doesn't match an imported (active) employer.

### 3d. Benefits/Plans (CSV)

**Source:** Summit → Data Exchange → Employer Benefit Plans (J4). May also need J7 (PB Employer Benefit Detail Report) for COBRA plans.

| CSV Column | Maps To | Notes |
|-----------|---------|-------|
| EmployerPlan_ID | `benefit.benefit_id` (PK) | Summit's plan ID |
| OrganizationID | `benefit.employer_id` (FK) | Links to employer |
| PlanTypeID | `benefit.plan_type_id` (FK) | Links to plantype |
| PlanName | `benefit.plan_name` | |
| PlanDescription | `benefit.plan_description` | |
| EffectiveDate | `benefit.effective_date` | Format: M/D/YYYY |
| TerminationDate | `benefit.termination_date` | Format: M/D/YYYY |
| PlanStatus | `benefit.active` | **Filter:** active = not terminated |
| CardEnabled | `benefit.hasCards` | "True" / "False" |
| Employer_ID | — | Alternate employer ID |
| EmployerName | — | Denormalized (skip) |
| PlanType | — | Text name (use PlanTypeID FK instead) |
| ImportPlanID | — | Informational |
| LinkedtoDefaultPlan | — | Informational |

**Renewal fields set during import:**
- `benefit.renewal_months` — from Step 2 configuration (per PlanType, default 12)
- `benefit.next_renewal_due` — calculated: `effectiveDate + renewalMonths` (or today + renewalMonths if effectiveDate is in the past)
- `benefit.last_renewed` — null on first import

---

## 4. Schema Changes

### 4a. PlanType table — add Level and LOS columns

The current `plantype` table only has: `PlanType_ID`, `Code`, `PlanTypeName`, `billing_group_id`, `purpose_id`.

**Add columns:**
- `level` VARCHAR(20) — "System Default", "TPA Custom", "Employer Custom"
- `los` VARCHAR(20) — "CDH", "COBRA", "CDH, COBRA"
- `employer_name` VARCHAR(255) — populated only for Employer Custom level

**Migration:** V025

### 4b. Import tracking constant

Add `SUMMIT_LAST_IMPORT` constant to track when the last import ran (ISO datetime string). Helps PSP admin know if data is stale.

---

## 5. GUI Design — Multi-Step Wizard

### Servlet: `SummitImportWizard` (`/SummitImport`)

**Security:** PSP Admin session required. Accessible from Admin navbar dropdown.

**Session attributes:** Prefixed with `si` (summit import).

### Step 1: UPLOAD

```
┌─────────────────────────────────────────────────────────┐
│  Import Summit Data                                      │
│  ─────────────────                                       │
│                                                          │
│  Upload your Summit export files. Plan Types must be     │
│  imported first (it's the reference data for benefits).  │
│                                                          │
│  ┌─ Plan Types (Excel) ─────────────────────────────┐   │
│  │  [Choose File]  summit_plantypes.xlsx    ✅ 27 rows │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌─ Employers (CSV) ────────────────────────────────┐   │
│  │  [Choose File]  I1_Employer_20260228.csv ✅ 139 rows│  │
│  └──────────────────────────────────────────────────┘   │
│  ┌─ Employees — Contact (CSV) ─────────────────────┐   │
│  │  [Choose File]  J2_Employee_20260228.csv ✅ 4045 rows│ │
│  │  Participant Listing Simple                        │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌─ Employees — Status (CSV) ─────────────────────┐    │
│  │  [Choose File]  J3_Employee_20260228.csv ✅ 4045 rows│ │
│  │  Participant Listing Report w/ Division Option     │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌─ Benefits (CSV) ─────────────────────────────────┐   │
│  │  [Choose File]  I4_Benefits_20260228.csv ✅ 312 rows│  │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│  Preview: validates headers, shows row counts,           │
│  flags missing/extra columns                             │
│                                                          │
│  [Cancel]                          [Next: Configure →]   │
└─────────────────────────────────────────────────────────┘
```

**Processing on upload:**
- Parse headers from each file
- Validate against expected column set (warn on missing required, ignore extra)
- Count rows
- Store parsed data in session (or temp table) for Step 2 preview

### Step 2: CONFIGURE

```
┌─────────────────────────────────────────────────────────┐
│  Configure Renewal Frequency                             │
│  ───────────────────────────                             │
│                                                          │
│  Set how often renewals are due for each plan type.      │
│  This applies to all benefits of that type.              │
│                                                          │
│  Plan Type        Code    LOS     Level     Months       │
│  ──────────       ────    ───     ─────     ──────       │
│  Dependent Care   DCA     CDH     System    [12]         │
│  Medical FSA      FSA     CDH,CO  System    [12]         │
│  Health Reimb     HRA     CDH,CO  System    [12]         │
│  Health Savings   HSA     CDH,CO  System    [12]         │
│  ...                                                     │
│  PRA              PRA     CDH     TPA       [12]         │
│  ICHRA            ICHRA   CDH     TPA       [12]         │
│  COPAY            COPAY   CDH     Employer  [12]         │
│                                                          │
│  Import Summary:                                         │
│  • 27 plan types (16 system, 10 TPA, 1 employer)        │
│  • 127 active employers (12 inactive → skip)             │
│  • 3,841 active employees (204 inactive → skip)          │
│  • 298 active benefits (14 terminated → skip)            │
│  • 18 new Renewal ServiceItems will be created           │
│                                                          │
│  [← Back]                            [Import Now →]      │
└─────────────────────────────────────────────────────────┘
```

**Pre-processing for this step:**
- Parse plan type file → display table with editable renewalMonths
- Scan employers/employees/benefits for active flags → compute skip counts
- Identify which PlanTypes will need new ServiceItems (ones not already in DB)

### Step 3: IMPORT (Processing)

```
┌─────────────────────────────────────────────────────────┐
│  Importing Summit Data...                                │
│  ────────────────────────                                │
│                                                          │
│  ✅ Plan Types .................. 27 imported             │
│  ✅ Renewal ServiceItems ........ 18 created             │
│  ✅ Employers ................... 127 imported (12 skip)  │
│  ⏳ Employees ................... 2,841 of 3,841...       │
│  ○ Benefits                                              │
│  ○ Reload application state                              │
│                                                          │
│  [progress bar ████████████░░░░░░░░░░░░░░░ 62%]         │
└─────────────────────────────────────────────────────────┘
```

**Processing order (dependencies):**
1. Plan Types → `plantype` table (upsert by PlanType_ID)
2. ServiceItems → `templatepurpose` table (create Renewal ServiceItems for new PlanTypes)
3. Employers → `employer` table (upsert by organization_id, active filter)
4. Employees → `employee` table (upsert by employee_id, active filter, employer FK validation)
5. Benefits → `benefit` table (upsert by benefit_id, active filter, set renewalMonths)
6. Reload AmsDataGlobal (refresh employer lists, plan types, service items)

### Step 4: RESULTS

```
┌─────────────────────────────────────────────────────────┐
│  Import Complete                                         │
│  ───────────────                                         │
│                                                          │
│  ┌─ Summary ───────────────────────────────────────┐    │
│  │  Plan Types:  27 imported (16 new, 11 updated)  │    │
│  │  ServiceItems: 18 created (Renewal sequences)   │    │
│  │  Employers:   127 imported (12 skipped)          │    │
│  │  Employees:   3,841 imported (204 skipped)       │    │
│  │  Benefits:    298 imported (14 skipped)           │    │
│  │  Duration:    8.3 seconds                        │    │
│  └─────────────────────────────────────────────────┘    │
│                                                          │
│  ⚠ 3 warnings:                                          │
│  • Employee 45021: employer 9912 not found (skipped)     │
│  • Benefit 8801: PlanTypeID 9999 not found (skipped)     │
│  • Employer 1234: name changed "Acme Inc" → "Acme LLC"   │
│                                                          │
│  [Go to Home]    [Go to Sequence Builder]                │
└─────────────────────────────────────────────────────────┘
```

---

## 6. Upsert Logic

Each entity uses its Summit ID as the natural key. On import:

| Scenario | Action |
|----------|--------|
| ID not in DB | INSERT new record |
| ID exists, data changed | UPDATE fields from CSV |
| ID exists, data unchanged | SKIP (no-op) |
| ID exists in DB but not in CSV | Leave as-is (don't delete — data may be intentionally absent from this export) |
| Record in CSV marked inactive/terminated | UPDATE: set `active=false` / `termination_date` |

**Counts tracked per entity:** inserted, updated, skipped (unchanged), skipped (filtered), errors.

---

## 7. ServiceItem Auto-Creation

When benefits are imported, for each distinct PlanType found:

1. Check if a Renewal-type ServiceItem already exists for that PlanType
   - Query: `SELECT si FROM ServiceItem si WHERE si.activityCategory.id = 1 AND si.providerRef = :planTypeId`
2. If not found, create:
   ```
   ServiceItem:
     description = PlanType.planTypeName
     code = PlanType.code
     activityCategory = 1 (Renewal)
     psp = current PSP (ID 4)
     sourceType = "SUMMIT"
     providerRef = PlanType.planTypeId (as string)
     defaultRenewalMonths = configured value from Step 2
     suppressed = false
     hasRequiredTasks = false (no task sequence yet)
   ```
3. Link PlanType.purpose_id → new ServiceItem.id

This means after import, the Sequence Builder already shows Renewal entries for each plan type, ready for the PSP admin to build task sequences.

---

## 8. Files to Create / Modify

### New Files

| File | Purpose |
|------|---------|
| `controller/data/SummitImportWizard.java` | Multi-step wizard servlet (GET/POST, step routing) |
| `data/service/SummitImportService.java` | Core import logic (parse, validate, upsert, auto-create ServiceItems) |
| `webapp/WEB-INF/view/a/general/summitImport/step1Upload.jsp` | Upload form |
| `webapp/WEB-INF/view/a/general/summitImport/step2Configure.jsp` | Renewal frequency config |
| `webapp/WEB-INF/view/a/general/summitImport/step3Progress.jsp` | Progress display |
| `webapp/WEB-INF/view/a/general/summitImport/step4Results.jsp` | Results summary |
| `V025__plantype_import_columns.sql` | Add `level`, `los`, `employer_name` to plantype |

### Modified Files

| File | Change |
|------|--------|
| `navbar25.jsp` | Add "Import Summit Data" to Admin dropdown |
| `AmsDataGlobal.java` | Refresh method after import (employers, plan types, service items) |
| `PlanType.java` | Add `level`, `los`, `employerName` fields |

### Existing Files to Reuse

| File | Reuse |
|------|-------|
| `Importer.java` | Reference for CSV parsing patterns (`fallbackCsvInsert`, `extractHeaders`, `convertFileToUTF8`) |
| `EntityLookup.java` | Existing lookup methods for PlanType, Employer, Employee, Benefit, ServiceItem |
| `DatabaseInitializer.java` | Reference for ServiceItem creation pattern |

---

## 9. Implementation Phases

### Phase 1: Foundation (this session or next)
- V025 migration (plantype columns)
- PlanType entity update
- `SummitImportService.java` — Plan Type import + ServiceItem auto-creation
- `SummitImportWizard.java` — Step 1 (upload) + Step 2 (configure) for Plan Types only
- Test with the attached `Summit - DataPath.xlsx`

### Phase 2: Core Entities
- Employer import (upsert logic, active filter)
- Employee import (upsert logic, active filter, FK validation)
- Benefit import (upsert logic, renewalMonths, date parsing)
- Full wizard flow (all 4 steps)

### Phase 3: Polish & Integration
- Progress bar (AJAX polling or server-sent events)
- Warning/error detail display
- Navbar integration
- `SUMMIT_LAST_IMPORT` tracking constant
- Update DatabaseInitializer checklist to reference new wizard

### Phase 4: Future — API Adapter
- When Summit API access is available, add an API adapter layer
- Same `SummitImportService` methods, different data source (HTTP instead of CSV)
- Same GUI but with "Import from API" option alongside "Upload Files"

---

## 10. Verification

### Initialization (first run)
1. Deploy fresh instance, run DatabaseInitializer
2. Navigate to `/SummitImport`
3. Upload all 4 files → verify row counts and header validation
4. Configure renewal months → verify plan type table displays correctly
5. Run import → verify all tables populated correctly
6. Check Sequence Builder → verify Renewal ServiceItems created
7. Verify AmsDataGlobal refreshed (employer list, plan types in dropdowns)

### Sync (subsequent run)
1. Modify a few records in Summit (rename an employer, terminate a benefit, add a new employee)
2. Re-export and re-upload
3. Run import → verify updates applied, new records inserted, terminated records flagged
4. Verify no duplicates created

### Edge Cases
- Upload with wrong file format → clear error message
- CSV with missing required columns → warning, partial import
- Employee referencing non-existent employer → skip with warning
- Benefit referencing non-existent PlanType → skip with warning
- Re-import same data → all records show as "unchanged" (0 inserts, 0 updates)

---

## 11. Open Items

| # | Item | Status | Notes |
|---|------|--------|-------|
| 1 | ~~Two employee exports — investigate whether both are needed.~~ | ✅ Resolved | **Both required.** J2 (Participant Listing Simple) has contact info; J3 (Participant Listing Report w/ Division Option) has status IDs and dates. Summit exports are not configurable — all columns are sent. Banking/SSN fields in J3 are billing-only and ignored by sync. |
| 2 | **Employee table schema — add lifecycle date columns.** J3 provides EffectiveDate, TerminationDate, HireDate which have no corresponding columns on the `employee` table today. Decide whether to add these (would require V025 or V026 migration). | Open | Useful for filtering active/terminated and for onboarding context. |
| 3 | **Summit export naming convention.** Rename I1/I2/I3/I4 references throughout codebase to J1/J2/J3/J4 to match Summit report type naming. Or keep internal names separate. | Open | Low priority — cosmetic. |

### Summit Export Report Types (reference)

| Internal Name | Summit Report Type | Description |
|---|---|---|
| J1 (I1_Employer) | Employer Listing | All employer organizations |
| J2 (I2_Employee) | Participant Listing Simple | Employee contact info |
| J3 (I3_Employee_Alt) | Participant Listing Report with Division Option | Employee status + dates + banking |
| J4 (I4_Benefits) | Employer Benefit Plans | CDH benefit plans |
| J7 (I7_Cobra_Benefits) | PB Employer Benefit Detail Report | COBRA/PB benefit detail |
