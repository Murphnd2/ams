# NDT-125 Census-Based Architecture: Design Document

## Overview

This document defines the architecture for a census-based nondiscrimination testing (NDT) system within the SSA Activity Management System (AMS). Rather than asking users 89 questionnaire questions, the system collects 5-8 structural questions, accepts document uploads, uses AI to parse those documents into a normalized employee-level data structure stored as JSON in MySQL, auto-tags each employee for testing classification, identifies data gaps, and runs all applicable NDT tests computationally.

**Tech stack context:** Java 17, Jakarta EE, EclipseLink JPA, MySQL 8.0, Tomcat 10, JSP with Bootstrap 5. The system integrates with the existing `questionnaire` and `questionnaire_field` tables and the activity/checklist workflow.

---

## 1. Data Model -- Employee Census JSON Structure

### 1.1 Employee Record Schema

Each employee in the census is represented as a JSON object. The full census is an array of these objects stored in a MySQL JSON column.

```json
{
  "employee_id": "EMP-001",
  "source_row_id": 14,

  "identity": {
    "display_name": "Employee 001",
    "anonymized": true,
    "hire_date": "2019-03-15",
    "termination_date": null,
    "date_of_birth": "1985-07-22",
    "age_at_plan_year_end": 41
  },

  "employment": {
    "status": "active",
    "hours_worked_annual": 2080,
    "hours_worked_weekly_avg": 40.0,
    "months_employed_in_plan_year": 12,
    "months_of_service_total": 84,
    "job_classification": "salaried",
    "union_status": "non_union",
    "cba_covered": false,
    "entity_name": "Acme Corp",
    "is_primary_entity": true,
    "citizenship_status": "us_citizen",
    "is_nonresident_alien": false
  },

  "compensation": {
    "annual_compensation_current": 95000.00,
    "annual_compensation_prior_year": 88000.00,
    "w2_compensation": 93500.00,
    "compensation_rank_percentile": 62.5
  },

  "ownership": {
    "ownership_percentage": 0.0,
    "is_officer": false,
    "officer_title": null,
    "officer_compensation_rank": null,
    "related_to_owner": false,
    "relationship_type": null,
    "related_owner_id": null
  },

  "benefits": {
    "is_plan_eligible": true,
    "is_participant": true,
    "is_cobra": false,
    "enrolled_benefits": ["medical", "dental", "health_fsa"],
    "salary_reductions": {
      "medical_premium": 3600.00,
      "dental_premium": 480.00,
      "vision_premium": 0.00,
      "health_fsa": 2850.00,
      "dcfsa": 0.00,
      "hsa": 0.00,
      "other_pretax": 0.00
    },
    "employer_contributions": {
      "medical_premium": 14400.00,
      "dental_premium": 720.00,
      "vision_premium": 0.00,
      "health_fsa": 0.00,
      "dcfsa": 0.00,
      "hsa": 0.00,
      "other": 0.00
    },
    "total_salary_reduction": 6930.00,
    "total_employer_contribution": 15120.00
  },

  "tags": {
    "is_hci_125": false,
    "hci_125_reason": null,
    "is_hci_105h": false,
    "hci_105h_reason": null,
    "is_hce_129": false,
    "hce_129_reason": null,
    "is_key_employee_416i": false,
    "key_employee_reason": null,
    "is_excludable_125": false,
    "excludable_125_reason": null,
    "is_excludable_105h": false,
    "excludable_105h_reason": null,
    "is_excludable_129": false,
    "excludable_129_reason": null,
    "is_eligible": true,
    "is_participant": true,
    "is_cobra": false,
    "tag_confidence": 1.0,
    "tag_warnings": []
  },

  "data_sources": {
    "census_file": true,
    "payroll_file": false,
    "ownership_file": false,
    "billing_file": false,
    "enrollment_file": false,
    "manual_entry": false
  },

  "field_confidence": {
    "compensation": 0.95,
    "hours_worked": 0.90,
    "benefits_enrolled": 0.85,
    "ownership": 1.0
  }
}
```

### 1.2 Field Definitions and Constraints

| Field Path | Type | Required | Notes |
|---|---|---|---|
| `employee_id` | string | Yes | System-generated or from source file. Unique within the test run. |
| `source_row_id` | int | No | Row number in the original uploaded file, for audit traceability. |
| `identity.display_name` | string | Yes | Anonymized label (e.g., "Employee 001") or real name depending on privacy settings. |
| `identity.anonymized` | boolean | Yes | True if the name has been anonymized. |
| `identity.hire_date` | date | Yes | ISO 8601 format. Required for service-based exclusion calculations. |
| `identity.termination_date` | date | No | Null if currently employed. |
| `identity.date_of_birth` | date | Preferred | Required for age-based exclusions. If missing, `age_at_plan_year_end` can be provided directly. |
| `identity.age_at_plan_year_end` | int | Computed | Calculated from DOB and plan year end date. |
| `employment.status` | enum | Yes | One of: `active`, `terminated`, `loa`, `cobra`, `retired`. |
| `employment.hours_worked_annual` | number | Preferred | Annual hours. Required for hours-based exclusion tests. |
| `employment.hours_worked_weekly_avg` | number | Computed | `hours_worked_annual / 52`. Used for 105(h) 35-hour test. |
| `employment.months_of_service_total` | int | Computed | From hire_date to plan year end (or termination if earlier). |
| `employment.job_classification` | string | No | Free text from source file. Informational. |
| `employment.union_status` | enum | No | `union`, `non_union`, `unknown`. |
| `employment.entity_name` | string | No | For controlled group tracking. Defaults to the plan sponsor name. |
| `employment.is_nonresident_alien` | boolean | No | Defaults to false. Critical for NRA exclusions. |
| `compensation.annual_compensation_current` | number | Preferred | Current plan year compensation. |
| `compensation.annual_compensation_prior_year` | number | Preferred | Prior year. Used for HCE threshold testing under 414(q). |
| `compensation.compensation_rank_percentile` | number | Computed | Percentile rank among all employees. Used for top-25% test. |
| `ownership.ownership_percentage` | number | No | 0 if not an owner. |
| `ownership.is_officer` | boolean | No | Defaults to false. |
| `ownership.related_to_owner` | boolean | No | True if spouse/dependent/child of a >5% owner. |
| `benefits.enrolled_benefits` | string[] | Preferred | Array of benefit codes: `medical`, `dental`, `vision`, `health_fsa`, `limited_fsa`, `dcfsa`, `hsa`, `other`. |
| `benefits.salary_reductions.*` | number | Preferred | Pre-tax deduction amounts by benefit type. |
| `benefits.employer_contributions.*` | number | Preferred | Employer contribution amounts by benefit type. |
| `tags.*` | boolean/string | Computed | All tags are system-computed. See Section 5. |

### 1.3 Plan-Level Data Schema

Stored alongside the census in the `ndt_test_run` table, this captures entity-wide and plan-wide configuration.

```json
{
  "employer": {
    "legal_name": "Acme Corporation",
    "entity_type": "c_corp",
    "plan_year_start": "2026-01-01",
    "plan_year_end": "2026-12-31",
    "is_controlled_group": false,
    "controlled_group_entities": [],
    "non_covered_entities": []
  },

  "plan_design": {
    "benefits_offered": ["pop", "health_fsa", "dcfsa"],
    "other_benefits_description": null,
    "is_simple_cafeteria": false,
    "simple_cafeteria_qualifies": false,
    "waiting_period_months": 3,
    "minimum_age": null,
    "hours_requirement": 1000,
    "eligibility_varies_by_class": false,
    "eligibility_class_description": null
  },

  "cba": {
    "has_cba_employees": false,
    "cba_benefits_bargained": false,
    "cba_employees_eligible": false
  },

  "section_125": {
    "available_to_all_equally": true,
    "classification_description": null,
    "same_benefits_all": true,
    "benefits_differ_description": null,
    "safe_harbor_75pct_health": true
  },

  "section_105h": {
    "same_expenses": true,
    "same_maximum": true,
    "same_cost_sharing": true,
    "same_waiting_periods": true,
    "same_dependent_coverage": true,
    "executive_physicals": false,
    "benefit_changes_mid_year": false,
    "difference_descriptions": {}
  },

  "section_129": {
    "available_to_all_equally": true,
    "classification_description": null,
    "same_maximum": true,
    "same_terms": true,
    "employer_nonelective_contributions": false,
    "nonelective_same_terms": null,
    "spouse_dependent_participate": false
  },

  "thresholds": {
    "hce_compensation_threshold": 160000,
    "key_employee_officer_threshold": 230000,
    "key_employee_1pct_threshold": 150000,
    "plan_year": 2026
  }
}
```

### 1.4 Test Results Schema

```json
{
  "run_timestamp": "2026-03-22T14:30:00Z",
  "tests_applicable": ["125_eligibility", "125_contributions", "125_key_employee", "105h_eligibility", "105h_benefits", "129_eligibility", "129_contributions", "129_owners", "129_55pct"],
  "overall_result": "PASS",

  "results": {
    "125_eligibility": {
      "status": "PASS",
      "method": "nondiscriminatory_classification",
      "total_employees": 150,
      "excludable_employees": 22,
      "non_excludable_employees": 128,
      "hci_count": 8,
      "non_hci_count": 120,
      "hci_eligible": 8,
      "non_hci_eligible": 115,
      "hci_eligible_pct": 100.0,
      "non_hci_eligible_pct": 95.8,
      "safe_harbor_pct": 100.0,
      "unsafe_harbor_pct": 72.5,
      "passes_safe_harbor": true,
      "passes_unsafe_harbor": true,
      "notes": "Plan satisfies the nondiscriminatory classification test."
    },
    "125_contributions": {
      "status": "PASS",
      "method": "safe_harbor_75pct",
      "employer_contribution_pct_lowest_paid": 78.5,
      "threshold": 75.0,
      "notes": "Employer contributes at least 75% of the cost of the most expensive health plan for the lowest-paid eligible participant."
    },
    "125_key_employee": {
      "status": "PASS",
      "key_employee_count": 5,
      "key_employee_nontaxable_benefits": 42500.00,
      "total_nontaxable_benefits": 1250000.00,
      "key_employee_concentration_pct": 3.4,
      "threshold": 25.0,
      "notes": "Key employee nontaxable benefits are 3.4% of total, below the 25% threshold."
    }
  },

  "warnings": [
    "3 employees missing hours data -- assumed full-time (2080 hours).",
    "Nonresident alien status could not be verified for 2 employees."
  ],

  "data_quality_score": 0.92,
  "confidence_level": "high"
}
```

---

## 2. MySQL Storage Design

### 2.1 Primary Table: `ndt_test_run`

```sql
CREATE TABLE ndt_test_run (
    test_run_id     BIGINT AUTO_INCREMENT PRIMARY KEY,
    activity_id     BIGINT NOT NULL,
    questionnaire_instance_id BIGINT NULL,
    psp_id          INT NOT NULL,
    employer_name   VARCHAR(200) NOT NULL,
    plan_year_end   DATE NOT NULL,

    -- Status tracking
    status          VARCHAR(30) NOT NULL DEFAULT 'data_collection',
    -- Values: data_collection, parsing, gap_analysis, ready_to_test,
    --         testing, completed, error

    -- JSON data columns
    plan_data       JSON NOT NULL,
    census_data     JSON NOT NULL,
    test_results    JSON NULL,
    gap_analysis    JSON NULL,
    parse_log       JSON NULL,

    -- Metadata
    employee_count  INT DEFAULT 0,
    document_count  INT DEFAULT 0,
    data_quality_score DECIMAL(3,2) NULL,

    -- Audit
    created_by      BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    submitted_at    TIMESTAMP NULL,
    submitted_by    BIGINT NULL,

    -- Foreign keys
    CONSTRAINT fk_ndt_activity FOREIGN KEY (activity_id) REFERENCES activity(id),
    CONSTRAINT fk_ndt_psp FOREIGN KEY (psp_id) REFERENCES psp(id),
    CONSTRAINT fk_ndt_created_by FOREIGN KEY (created_by) REFERENCES person(id),

    INDEX idx_ndt_activity (activity_id),
    INDEX idx_ndt_psp_status (psp_id, status),
    INDEX idx_ndt_plan_year (plan_year_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.2 Supporting Table: `ndt_document_upload`

```sql
CREATE TABLE ndt_document_upload (
    upload_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_run_id     BIGINT NOT NULL,
    document_type   VARCHAR(30) NOT NULL,
    -- Values: census, payroll, ownership, billing, enrollment, other

    original_filename VARCHAR(255) NOT NULL,
    stored_filename   VARCHAR(255) NOT NULL,
    file_size_bytes   BIGINT NOT NULL,
    mime_type         VARCHAR(100) NOT NULL,
    row_count         INT NULL,
    column_count      INT NULL,

    -- Parsing state
    parse_status    VARCHAR(20) NOT NULL DEFAULT 'pending',
    -- Values: pending, parsing, parsed, failed, rejected
    parse_confidence DECIMAL(3,2) NULL,
    column_mapping  JSON NULL,
    parse_errors    JSON NULL,

    -- Security
    virus_scan_status VARCHAR(20) DEFAULT 'pending',
    ssn_detected      BOOLEAN DEFAULT FALSE,
    ssn_scrubbed      BOOLEAN DEFAULT FALSE,
    pii_scrub_log     JSON NULL,

    uploaded_by     BIGINT NOT NULL,
    uploaded_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ndt_doc_run FOREIGN KEY (test_run_id) REFERENCES ndt_test_run(test_run_id),
    CONSTRAINT fk_ndt_doc_uploader FOREIGN KEY (uploaded_by) REFERENCES person(id),

    INDEX idx_ndt_doc_run (test_run_id),
    INDEX idx_ndt_doc_type (test_run_id, document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.3 Audit Table: `ndt_access_log`

```sql
CREATE TABLE ndt_access_log (
    log_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_run_id     BIGINT NOT NULL,
    person_id       BIGINT NOT NULL,
    action          VARCHAR(50) NOT NULL,
    -- Values: view_census, edit_census, run_tests, view_results,
    --         download_results, upload_document, delete_document
    detail          VARCHAR(500) NULL,
    ip_address      VARCHAR(45) NULL,
    accessed_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ndt_log_run FOREIGN KEY (test_run_id) REFERENCES ndt_test_run(test_run_id),
    INDEX idx_ndt_log_run (test_run_id),
    INDEX idx_ndt_log_person (person_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2.4 JSON Blob vs. Normalized Rows -- Analysis and Recommendation

**Option A: Single JSON blob** (census_data column holds the full array)

Pros:
- Simple schema: one column, one read/write operation.
- No ORM mapping for individual employees -- the Java service class handles serialization.
- Flexible schema evolution: adding a new field to the employee record requires no ALTER TABLE.
- Atomic: the entire census is read and written as a unit, which matches the workflow (parse all at once, tag all at once, test all at once).
- Matches the existing AMS pattern of JSON-in-column for questionnaire response data.

Cons:
- Large JSON documents (1000+ employees at ~2KB each = ~2MB). MySQL JSON columns handle this without issue, but it means reading/writing the entire blob for any single-employee edit.
- No SQL-level querying of individual employees (cannot `SELECT * FROM ... WHERE employee.ownership_percentage > 5`). All filtering happens in Java.
- No referential integrity at the employee level.

**Option B: Normalized `ndt_employee` table** (one row per employee per test run)

Pros:
- SQL-level querying and indexing on employee fields.
- Smaller per-row updates.
- Relational integrity.

Cons:
- 30+ columns or more sub-tables for the nested structure.
- Schema rigidity: every new field requires ALTER TABLE and JPA entity changes.
- More complex ORM mapping (new entity, new DAO, new service layer).
- Batch operations become multi-row INSERT/UPDATE with transaction management.
- The employee data is only relevant within the context of a single test run -- it is not reused across runs. There is no cross-test-run query requirement.

**Recommendation: JSON blob (Option A).**

The census data is a transient working dataset, not a long-lived relational entity. It is created during document parsing, modified during gap-fill, consumed during test computation, and then archived with results. The entire lifecycle operates on the full dataset as a unit. There are no cross-run queries, no need to JOIN employees against other tables, and no requirement for SQL-level search. The JSON approach matches the existing AMS pattern (questionnaire responses are JSON) and avoids the schema rigidity that would slow feature evolution.

For performance with large employers (1000+ employees), the JSON document will be approximately 2-4MB. MySQL 8.0 handles JSON documents up to 1GB. The Java service will deserialize into `List<CensusEmployee>` using Jackson, operate in memory, and serialize back. This is well within normal application memory bounds.

### 2.5 Migration Script Structure

This will be a single versioned migration (V059 or the next available slot):

```sql
-- V059__ndt_census_tables.sql

CREATE TABLE ndt_test_run ( ... );
CREATE TABLE ndt_document_upload ( ... );
CREATE TABLE ndt_access_log ( ... );

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V059', 'NDT census-based testing tables', 'V059__ndt_census_tables.sql', NOW());
```

The migration tracker and `schema_version_migration.sql` will be updated per standard AMS conventions.

---

## 3. AI Document Parsing Pipeline

### 3.1 Architecture Overview

```
User uploads file
       |
       v
[NdtUploadServlet] -- validates file type, size, virus scan
       |
       v
[PII Scrubber] -- regex scan for SSNs, strip before AI processing
       |
       v
[File Parser] -- Apache POI (Excel) or OpenCSV (CSV) extracts raw rows/columns
       |
       v
[AI Column Mapper] -- sends column headers + sample rows to Anthropic API
       |                asks AI to map columns to census schema fields
       v
[User confirms mapping] -- JSP shows detected mapping, user approves
       |
       v
[AI Row Parser] -- for structured files (CSV/Excel), this is deterministic
       |            for PDFs, AI extracts tabular data
       v
[Census Merger] -- merges parsed data into existing census_data JSON
       |
       v
[Auto-Tagger] -- recomputes all employee tags based on merged data
```

### 3.2 Upload Flow (Servlet Layer)

The upload is handled by a new servlet `NdtUploadServlet` at `/ndt/upload`.

1. User selects a file and document type from the upload zone on the wizard page.
2. Servlet receives the multipart form data.
3. **Validation:**
   - File type: `.csv`, `.xlsx`, `.xls`, `.pdf` only. Reject all others.
   - File size: Maximum 10MB per file, 50MB total per test run.
   - MIME type check: verify the file content matches the declared extension.
4. **PII Scrub (pre-AI):**
   - Regex scan for SSN patterns: `\b\d{3}-\d{2}-\d{4}\b` and `\b\d{9}\b` (nine consecutive digits in a context suggesting SSN).
   - If SSNs detected: replace with `***-**-XXXX` in the working copy. Set `ssn_detected = true`, `ssn_scrubbed = true` on the upload record.
   - Log the scrub action (count of SSNs found) but never log the SSN values.
5. **Store the original file** on disk at `/var/lib/tomcat10/ndt-uploads/{test_run_id}/{upload_id}_{original_filename}`. Directory is outside the web root and not accessible via URL.
6. **Parse the file** into raw data (headers + rows) using Apache POI for Excel or OpenCSV for CSV. For PDFs, send the raw bytes to the AI for tabular extraction.
7. **Send to AI for column mapping** (see 3.3).
8. Return the mapping results to the JSP for user confirmation.

### 3.3 AI Column Mapping

**When it runs:** After the raw file is parsed into headers and sample rows.

**What is sent to the AI:**

```
System prompt:
You are a data mapping assistant for employee benefits administration.
Given the column headers and sample data rows from an uploaded file,
map each column to the standardized census schema fields.

Respond with a JSON object mapping each source column name to a target
field path, or null if the column has no mapping.

Target field paths:
- identity.display_name
- identity.hire_date
- identity.termination_date
- identity.date_of_birth
- employment.status
- employment.hours_worked_annual
- employment.job_classification
- employment.union_status
- employment.entity_name
- employment.citizenship_status
- compensation.annual_compensation_current
- compensation.annual_compensation_prior_year
- ownership.ownership_percentage
- ownership.is_officer
- ownership.officer_title
- ownership.related_to_owner
- ownership.relationship_type
- benefits.enrolled_benefits.medical
- benefits.enrolled_benefits.dental
- benefits.enrolled_benefits.vision
- benefits.enrolled_benefits.health_fsa
- benefits.enrolled_benefits.dcfsa
- benefits.enrolled_benefits.hsa
- benefits.salary_reductions.medical_premium
- benefits.salary_reductions.dental_premium
- benefits.salary_reductions.health_fsa
- benefits.salary_reductions.dcfsa
- benefits.employer_contributions.medical_premium
- benefits.employer_contributions.dental_premium
[... full list ...]

User message:
Document type: {census|payroll|ownership|billing|enrollment}

Column headers: ["Last Name", "First Name", "SSN", "DOB", "Hire Date", ...]

Sample rows (first 5, PII scrubbed):
Row 1: ["Smith", "Jane", "***-**-1234", "1985-03-15", "2019-01-10", ...]
Row 2: [...]
...
```

**Expected AI Response:**

```json
{
  "column_mapping": {
    "Last Name": {"target": "identity.display_name", "confidence": 0.95, "transform": "combine_with_first_name"},
    "First Name": {"target": "identity.display_name", "confidence": 0.95, "transform": "combine_with_last_name"},
    "SSN": {"target": null, "confidence": 1.0, "note": "SSN excluded from census"},
    "DOB": {"target": "identity.date_of_birth", "confidence": 0.98, "transform": "parse_date"},
    "Hire Date": {"target": "identity.hire_date", "confidence": 0.99, "transform": "parse_date"},
    "Annual Salary": {"target": "compensation.annual_compensation_current", "confidence": 0.92, "transform": "parse_currency"},
    "Med EE Ded": {"target": "benefits.salary_reductions.medical_premium", "confidence": 0.85, "transform": "parse_currency"}
  },
  "unmapped_columns": ["Dept Code", "Location"],
  "warnings": ["'Med EE Ded' could be monthly or annual -- sample values suggest annual."]
}
```

### 3.4 Document-Type-Specific Parsing

#### 3.4.1 Employee Census / HR Export

**Primary source.** Usually CSV or Excel.

- AI prompt emphasis: "This is an employee census or HR export. Focus on identifying employee demographics, employment dates, hours, compensation, and benefit enrollment columns."
- Transform rules: Combine first/last name fields. Parse date formats flexibly (MM/DD/YYYY, YYYY-MM-DD, etc.). Convert hourly rates to annual if hours column present. Detect enrollment indicators (Y/N, "Enrolled", plan names, tier codes).
- Expected output: One `CensusEmployee` per row.

#### 3.4.2 Payroll YTD Report

**Supplements census with compensation and deduction detail.**

- AI prompt emphasis: "This is a payroll YTD report. Focus on identifying employee identification, gross compensation, pre-tax deduction amounts by benefit type, and employer contribution amounts."
- Merge strategy: Match by employee name or employee ID to existing census records. Update `compensation` and `benefits.salary_reductions` and `benefits.employer_contributions` fields.
- Special handling: If payroll shows monthly amounts, multiply by months worked to annualize. Flag this transformation for user review.

#### 3.4.3 Insurance Billing Statement

**Confirms enrollment and employer/employee premium splits.**

- AI prompt emphasis: "This is an insurance billing statement or carrier invoice. Extract enrolled employee names, plan type, coverage tier, total premium, employee premium share, and employer premium share. Identify COBRA participants."
- Merge strategy: Match employees to census. Confirm enrollment. Update `benefits.employer_contributions` and `benefits.salary_reductions` for premium fields.
- Special value: Can compute the 75% safe harbor automatically (does employer pay at least 75% of the most expensive single coverage option?).

#### 3.4.4 Ownership / Officer Declaration

**Critical for HCI/HCE/Key Employee classification.**

- AI prompt emphasis: "This is an ownership or officer declaration. Extract each owner/officer name, ownership percentage, officer title, compensation, and any family relationships noted."
- Merge strategy: Match by name to census. Set `ownership.ownership_percentage`, `ownership.is_officer`, `ownership.officer_title`, `ownership.related_to_owner`, `ownership.relationship_type`.
- If an owner is not in the census: create a new census record and flag it. This individual may be an owner who is not an employee.

#### 3.4.5 Benefits Enrollment Report

**Detailed election-level data.**

- AI prompt emphasis: "This is a benefits enrollment report. Extract each employee, the benefits they elected, and election amounts (annual salary reduction and employer contribution by benefit type)."
- Merge strategy: Match to census. Update `benefits.enrolled_benefits`, `benefits.salary_reductions`, and `benefits.employer_contributions`.

### 3.5 Census Merge Logic

When a second (or third, fourth) document is uploaded, the parsed data must merge into the existing census.

**Matching algorithm:**

1. **Exact match on employee_id** (if both sources use the same employee ID field).
2. **Name match:** Normalize names (trim, lowercase, remove suffixes like Jr./Sr., standardize hyphenated names). Match on last name + first name. If ambiguous (e.g., two "John Smith"), flag for manual resolution.
3. **Fuzzy match:** If exact name match fails, compute Levenshtein distance. Accept matches with distance <= 2 and flag for user confirmation.
4. **No match:** Create a new employee record and flag it as "added from {document_type}."

**Field merge rules:**

| Scenario | Resolution |
|---|---|
| New field not previously populated | Accept the new value. |
| Existing field, same value | No change. |
| Existing field, different value, same source priority | Flag as conflict. Show both values to user. |
| Existing field, different value, higher-priority source | Accept higher-priority source. Log the override. |

**Source priority** (highest to lowest):
1. Manual user entry (always wins)
2. Payroll report (most authoritative for compensation)
3. Census/HR export (most authoritative for demographics)
4. Ownership declaration (most authoritative for ownership)
5. Billing statement (most authoritative for enrollment/premiums)
6. Benefits enrollment report

### 3.6 Conflict Resolution

When two sources disagree:

```json
{
  "conflicts": [
    {
      "employee_id": "EMP-042",
      "field": "compensation.annual_compensation_current",
      "value_a": {"source": "census", "value": 82000},
      "value_b": {"source": "payroll", "value": 85000},
      "auto_resolution": "payroll",
      "reason": "Payroll is the authoritative source for compensation.",
      "user_override": null
    }
  ]
}
```

Conflicts are stored in the `gap_analysis` JSON column and presented to the user on the data review page. The system auto-resolves using source priority but allows user override.

### 3.7 Confidence Scoring

Each parsed field gets a confidence score (0.0 to 1.0):

| Confidence | Meaning | Example |
|---|---|---|
| 1.0 | Deterministic extraction from structured data | "Hire Date" column clearly labeled, valid date format |
| 0.9-0.99 | High-confidence AI mapping | Column header "DOB" mapped to date_of_birth |
| 0.7-0.89 | Moderate confidence | Column header "Med EE" -- probably medical employee deduction |
| 0.5-0.69 | Low confidence -- needs user review | Ambiguous column header, unclear data format |
| < 0.5 | AI guessing -- flag prominently | Column contains mixed data, no clear mapping |

Overall data quality score for the test run = average of all field confidences weighted by importance to test calculations.

---

## 4. The User Flow (Step by Step)

### Page 1: Employer Info and Entity Type

**Title:** "About the Employer"
**URL:** `/ndt/wizard?step=1&testRunId={id}`

**Fields:**
- Employer legal name (text input, required)
- Plan year end date (date picker, required)
- Entity type (dropdown: C-Corporation, S-Corporation, Partnership, Sole Proprietorship, LLC, Tax-Exempt Organization -- required)

**Behavior:**
- On first visit, creates a new `ndt_test_run` record with status `data_collection`.
- On "Next," saves the employer section of `plan_data` JSON.

**Estimated time:** 1 minute.

### Page 2: Benefits Offered

**Title:** "Plan Benefits"
**URL:** `/ndt/wizard?step=2&testRunId={id}`

**Fields:**
- Which benefits does the cafeteria plan offer? (checkboxes, at least one required):
  - Pre-tax insurance premiums (POP)
  - Health FSA
  - Limited Purpose FSA
  - Dependent Care FSA (DCFSA)
  - HSA contributions
  - Other (with text field)

**Behavior:**
- Determines which NDT tests are applicable:
  - POP only: Section 125 eligibility and contributions tests only.
  - Health FSA or LP-FSA: adds Section 105(h) eligibility and benefits tests.
  - DCFSA: adds Section 129 eligibility, contributions, owners, and 55% tests.
  - Any non-POP benefit: adds Section 125 key employee concentration test.

**Estimated time:** 30 seconds.

### Page 3: Structural Questions

**Title:** "Plan Structure"
**URL:** `/ndt/wizard?step=3&testRunId={id}`

**Fields:**
- Is the employer part of a controlled group or affiliated service group? (Yes/No)
  - If Yes: Are any related entities excluded from the plan? (Yes/No)
    - If Yes: Names of excluded entities, employee count at excluded entities.
- Has the employer adopted a Simple Cafeteria Plan? (Yes/No/Unsure)
  - If Yes: Average 100 or fewer employees in prior 2 years? (Yes/No)
  - If Yes: Meets contribution requirement? (Yes/No)
  - If Yes: All employees with 1,000+ hours eligible? (Yes/No)
  - If all four Yes: **"Your plan qualifies for the Simple Cafeteria Plan safe harbor. All nondiscrimination tests are deemed satisfied."** Skip to attestation (Page 9).
- Are any employees covered by a collective bargaining agreement? (Yes/No)
  - If Yes: Were cafeteria benefits bargained for in good faith? (Yes/No)
  - If Yes: Can CBA employees participate in the cafeteria plan? (Yes/No)

**Estimated time:** 1-3 minutes.

### Page 4: Document Upload Zone

**Title:** "Upload Your Data"
**URL:** `/ndt/wizard?step=4&testRunId={id}`

**Layout:**
Introductory text: "Uploading employee data files dramatically reduces the number of questions we need to ask. Upload any documents you have -- the more you provide, the fewer questions remain."

Five upload zones, each with:
- Drag-and-drop area + file picker button
- Document type label and description
- Accepted formats badge (CSV, XLSX, PDF)
- Impact indicator ("Eliminates ~30 questions")
- Upload status indicator (pending, uploading, processing, complete, error)

Upload zones:
1. **Employee Census / HR Export** (HIGH priority) -- "Your employee roster with demographics, dates, and benefit enrollment."
2. **Payroll YTD Report** (HIGH priority) -- "Year-to-date compensation and pre-tax deduction amounts."
3. **Ownership & Officer List** (MEDIUM priority) -- "Owners, officers, ownership percentages, and family relationships."
4. **Insurance Billing Statement** (LOW priority) -- "Monthly carrier invoice showing enrolled employees and premiums."
5. **Benefits Enrollment Report** (LOW priority) -- "Detailed benefit elections by employee."

Bottom of page: "Skip uploads -- I'll answer all questions manually" link.

**Behavior:**
- Each file upload is AJAX-based. The user can upload multiple files without leaving the page.
- On upload, the file goes through validation, PII scrub, and AI column mapping (3-10 seconds).
- Progress bar shows status. When complete, a green checkmark and summary appears ("142 employees found, 18 columns mapped").
- User can remove and re-upload.

**Estimated time:** 2-5 minutes (or 5 seconds if skipped).

### Page 5: AI Parsing Confirmation

**Title:** "Review Parsed Data"
**URL:** `/ndt/wizard?step=5&testRunId={id}`

**Layout:**
For each uploaded file, a collapsible panel shows:
- **Column mapping table:** Source column -> Target field, with confidence indicators (green/yellow/red).
- **Sample data preview:** First 5 rows of parsed data in the target schema.
- **Unmapped columns:** Listed with dropdown to manually assign or mark "ignore."
- **Warnings:** Any parsing issues (date format ambiguity, currency parsing, etc.).
- **Conflicts:** If multiple files were uploaded, show any field-level conflicts with resolution options.

**Behavior:**
- User reviews and confirms each mapping. Can adjust mappings via dropdown.
- "Looks Good" button accepts the mapping and triggers full file parsing.
- "Re-upload" button allows replacing the file.
- After all files confirmed, the system merges all data into the census_data JSON and runs auto-tagging.

**Estimated time:** 1-3 minutes per file.

### Page 6: Gap Analysis

**Title:** "Additional Information Needed"
**URL:** `/ndt/wizard?step=6&testRunId={id}`

**Layout:**
The system analyzes the merged census data and identifies what is still missing for each applicable test. Questions are grouped by category:

- **Plan Eligibility Rules** (always shown -- these are plan design questions no file can answer):
  - Waiting period? Length?
  - Minimum age? Value?
  - Hours requirement? Value?
  - Eligibility varies by employee class? Description?

- **Missing Employee Data** (only if files did not provide):
  - Section G excludable counts (if no census)
  - Section H excludable counts (if no census and FSA offered)
  - Section I excludable counts (if no census and DCFSA offered)
  - HCI/HCE/Key Employee counts (if no ownership data)

- **Plan Design Questions** (always -- no file answers these):
  - Section 125: Plan open to all equally? Same benefits for everyone? 75% safe harbor?
  - Section 105(h): Same expenses? Same maximums? Same cost sharing? Same waiting periods? Same dependent coverage? Executive physicals? Mid-year changes?
  - Section 129: Open to all equally? Same maximum? Same terms? Employer nonelective contributions?

- **Judgment Questions** (always -- require human knowledge):
  - Any employees who are spouses/dependents of HCIs?
  - Owner spouse/dependent DCFSA participation?

Each section shows a count of remaining questions: "3 questions remaining in this section."

Questions that were answered by file uploads show as pre-filled with a green "From uploaded data" badge.

**Behavior:**
- Dynamically hides sections not applicable based on benefits offered.
- Dynamically hides questions already answered by uploads.
- Saves progress via AJAX on each field change.

**Estimated time:** 5-15 minutes depending on upload coverage (5 minutes with full uploads, 15 minutes with no uploads).

### Page 7: Employee Roster Review

**Title:** "Review Employee Classifications"
**URL:** `/ndt/wizard?step=7&testRunId={id}`

**Layout:**
A data table showing all employees with their computed tags:

| # | Name/ID | Status | Age | Service | Hours | Comp | Owner% | Officer | HCI-125 | HCI-105h | HCE-129 | Key | Excl-125 | Excl-105h | Excl-129 | Eligible | Participant |
|---|---------|--------|-----|---------|-------|------|--------|---------|---------|----------|---------|-----|----------|-----------|----------|----------|-------------|

- Each tag column shows a colored badge: green (Yes), gray (No), yellow (Warning/Low Confidence).
- Clicking a tag shows the reason (e.g., "HCI-125: ownership 12%").
- Employees with any yellow flags are sorted to the top.
- Filter controls: show all / show flagged only / show by tag.
- Edit button per row opens an inline editor for corrections.

Summary cards at the top:
- Total employees: 150
- HCIs (Sec 125): 8
- HCIs (Sec 105h): 12
- HCEs (Sec 129): 10
- Key Employees: 5
- Excludable (Sec 125): 22
- Excludable (Sec 105h): 18
- Excludable (Sec 129): 15

**Behavior:**
- User reviews computed tags and corrects any errors.
- "Override" option per tag with reason field.
- "Approve Roster" button marks the test run as `ready_to_test`.

**Estimated time:** 3-10 minutes (depends on employer size and data quality).

### Page 8: Run Tests and Results

**Title:** "Test Results"
**URL:** `/ndt/wizard?step=8&testRunId={id}`

**Layout:**
"Run All Tests" button. On click, the computation engine runs all applicable tests (1-3 seconds).

Results displayed as cards, one per test:

```
+---------------------------------------------------+
| Section 125 Eligibility Test            [PASS]     |
|---------------------------------------------------|
| Method: Nondiscriminatory Classification           |
| Non-excludable employees: 128                      |
| HCIs eligible: 8/8 (100%)                         |
| Non-HCIs eligible: 115/120 (95.8%)                |
| Safe harbor threshold: 100% -- SATISFIED           |
+---------------------------------------------------+

+---------------------------------------------------+
| Section 125 Key Employee Test           [PASS]     |
|---------------------------------------------------|
| Key employee benefits: $42,500 (3.4% of total)    |
| Threshold: 25%                                     |
| Result: 3.4% < 25% -- PASS                        |
+---------------------------------------------------+
```

Each card shows:
- Test name and pass/fail badge (green/red).
- Key metrics and percentages.
- The threshold and how the result compares.
- Expandable "Show Calculation Detail" section with full breakdown.
- Warnings (if any data assumptions were made).

Overall summary at top: "7 of 7 applicable tests PASSED" or "FAILED: 2 tests require attention."

**Behavior:**
- Failed tests show remediation guidance.
- "Download PDF Report" button generates a printable test report.
- "Re-run Tests" button available if user goes back and makes changes.

**Estimated time:** 1 minute (review results).

### Page 9: Attestation and Submit

**Title:** "Attestation"
**URL:** `/ndt/wizard?step=9&testRunId={id}`

**Fields:**
- Attestation checkbox: "I confirm that the information provided is accurate and complete to the best of my knowledge."
- Additional notes (textarea, optional).
- Digital signature: submitter name + date auto-populated.

**Behavior:**
- On submit: sets status to `completed`, records `submitted_at` and `submitted_by`.
- Locks the test run from further editing (new test run required for corrections).
- Generates final PDF report.
- Creates or updates the linked questionnaire instance with results summary.

**Estimated time:** 30 seconds.

---

## 5. Auto-Tagging Logic

### 5.1 is_hci_125 (Section 125 Highly Compensated Individual)

**IRC Section 125(e) references IRC 414(q) with modifications.**

An employee is an HCI for Section 125 purposes if ANY of the following are true:

1. **Ownership > 5%:** `ownership.ownership_percentage > 5.0`
2. **Officer:** `ownership.is_officer == true` (Note: For Section 125, all officers are HCIs regardless of compensation, unlike 414(q) which has a compensation threshold for HCE status. Section 125(e)(1)(A) specifically references the broader "officer" definition.)
3. **Highly compensated:** `compensation.annual_compensation_prior_year > thresholds.hce_compensation_threshold` (This is the 414(q) HCE threshold: $160,000 for 2026 plan year, indexed annually.)
4. **Spouse or dependent of any of the above:** `ownership.related_to_owner == true AND relationship_type IN ('spouse', 'dependent', 'child')` where the related person satisfies rule 1, 2, or 3.

**Family attribution for ownership:** Under IRC 318, an individual is treated as owning stock owned by their spouse, children, grandchildren, and parents. This means:
- If Parent owns 10%, Child is attributed 10% ownership.
- If Spouse owns 8%, the other Spouse is attributed 8% ownership.
- This is applied BEFORE checking the 5% threshold.

**Reason string construction:** `"ownership {pct}%"` or `"officer: {title}"` or `"compensation ${amt} > ${threshold}"` or `"spouse/dependent of {related_employee_id}"`.

### 5.2 is_hci_105h (Section 105(h) Highly Compensated Individual)

**IRC Section 105(h)(5) definition.**

An employee is an HCI for Section 105(h) purposes if ANY of the following are true:

1. **Ownership > 10%:** `ownership.ownership_percentage > 10.0` (Note: this is a HIGHER threshold than Section 125's 5%.)
2. **Top 5 highest-paid officers:** Among all employees where `ownership.is_officer == true`, rank by `compensation.annual_compensation_current`. The top 5 are HCIs. Store rank in `ownership.officer_compensation_rank`. If fewer than 5 officers exist, all officers qualify.
3. **Top 25% highest-compensated:** Rank ALL employees (not just officers, not just non-owners) by `compensation.annual_compensation_current`. Employees in the top 25th percentile are HCIs. `compensation.compensation_rank_percentile >= 75.0` qualifies. (The 75th percentile mark means the top 25% of earners.)

**Special rule for top-25%:** The employer can elect to use the top-paid group election (top 20% instead of top 25%), but this is less common. The system defaults to top 25%.

**Note:** Family attribution rules under IRC 318 apply to the ownership test but NOT to the officer or compensation tests.

### 5.3 is_hce_129 (Section 129 Highly Compensated Employee)

**IRC Section 129(d)(2) references IRC 414(q).**

An employee is an HCE for Section 129 (DCFSA) purposes if ANY of the following are true:

1. **Ownership > 5%:** `ownership.ownership_percentage > 5.0` at any time during the current or prior plan year.
2. **Compensation > threshold:** `compensation.annual_compensation_prior_year > thresholds.hce_compensation_threshold` ($160,000 for 2026 plan year).

**Note:** Unlike Section 125, the officer test is NOT a standalone trigger for Section 129 HCE status. Officers are only HCEs if they also meet the ownership or compensation threshold. However, the employer can elect the top-20% paid group option, which would limit the compensation test to employees in the top 20% of pay. The system defaults to the standard threshold test (all employees above the dollar threshold).

**Family attribution (IRC 318):** Same as Section 125 -- spouses, children, grandchildren, and parents are attributed ownership.

### 5.4 is_key_employee_416i (Key Employee under IRC 416(i))

An employee is a Key Employee if ANY of the following are true:

1. **Ownership > 5%:** `ownership.ownership_percentage > 5.0` at any time during the plan year.
2. **Ownership > 1% AND compensation > $150,000:** `ownership.ownership_percentage > 1.0 AND compensation.annual_compensation_current > thresholds.key_employee_1pct_threshold`. The $150,000 threshold is NOT indexed for inflation.
3. **Officer with compensation above threshold:** `ownership.is_officer == true AND compensation.annual_compensation_current > thresholds.key_employee_officer_threshold` ($230,000 for 2026, indexed annually). There is a cap on the number of officers who can be key employees: the lesser of 50 or 10% of all employees (but at least 1).

**Family attribution:** IRC 318 constructive ownership rules apply to both ownership tests.

### 5.5 is_excludable_125 (Section 125 Excludable Employee)

An employee is excludable from Section 125 testing if ANY of the following are true:

1. **Service requirement not met:** `employment.months_of_service_total < plan_design.waiting_period_months`. If the plan has no waiting period, this exclusion does not apply.
2. **Under minimum age:** `identity.age_at_plan_year_end < plan_design.minimum_age`. If the plan has no minimum age, this exclusion does not apply.
3. **Part-time/seasonal:** `employment.hours_worked_annual < 1000`. Employees working fewer than 1,000 hours during the plan year.
4. **Nonresident alien:** `employment.is_nonresident_alien == true` with no U.S.-source income.
5. **COBRA participant:** `benefits.is_cobra == true`.

**CBA employees:** If `cba.cba_benefits_bargained == true`, CBA employees are excluded from testing (they are treated as a separate testing group). If benefits were NOT bargained for, CBA employees are included in testing.

**Reason string:** Pipe-delimited list of all applicable reasons, e.g., `"under_service|part_time"`.

### 5.6 is_excludable_105h (Section 105(h) Excludable Employee)

An employee is excludable from Section 105(h) testing if ANY of the following are true:

1. **Under 3 years of service:** `employment.months_of_service_total < 36`.
2. **Under age 25:** `identity.age_at_plan_year_end < 25`.
3. **Part-time:** `employment.hours_worked_weekly_avg < 35.0` (35 hours per week, not 1,000 hours per year -- different threshold than Section 125).
4. **Nonresident alien:** `employment.is_nonresident_alien == true` with no U.S.-source income.

**Note:** These are STATUTORY exclusions, not plan-based. The employer cannot choose different thresholds. The plan can have stricter eligibility rules (e.g., waiting period < 3 years), but the statutory exclusion always applies for testing purposes.

### 5.7 is_excludable_129 (Section 129 Excludable Employee)

An employee is excludable from Section 129 testing if ANY of the following are true:

1. **Under age 21:** `identity.age_at_plan_year_end < 21`.
2. **Under 1 year of service:** `employment.months_of_service_total < 12`.
3. **Nonresident alien:** `employment.is_nonresident_alien == true` with no U.S.-source income.
4. **Compensation under $25,000:** `compensation.annual_compensation_current < 25000`. **IMPORTANT:** This exclusion applies ONLY to the 55% Average Benefits Test (Section 129(d)(8)), NOT to the eligibility test or the more-than-5% owners test.

The system stores `excludable_129_reason` with enough detail to distinguish which tests the exclusion applies to.

### 5.8 Tagging Execution Order

Tags must be computed in dependency order:

1. **Compute `compensation_rank_percentile`** for all employees (sort by compensation, assign percentile).
2. **Compute `officer_compensation_rank`** for all officers.
3. **Apply family attribution** to ownership percentages (IRC 318 constructive ownership).
4. **Compute `is_excludable_*`** tags (no dependencies on other tags).
5. **Compute `is_hci_125`**, `is_hci_105h`, `is_hce_129`, `is_key_employee_416i` (depend on ownership attribution and compensation ranking).
6. **Compute `is_eligible`** and `is_participant`** (depend on exclusion status and plan eligibility rules).
7. **Compute tag confidence** based on data source completeness.

---

## 6. Test Computation Engine

### 6.1 Section 125 Eligibility Test

**IRC Section 125(b)(1): The plan must not discriminate in favor of highly compensated individuals as to eligibility to participate.**

**Input data:**
- All non-excludable employees (`is_excludable_125 == false`)
- HCI flags (`is_hci_125`)
- Eligibility flags (`is_eligible`)
- Plan design: `section_125.available_to_all_equally`, `section_125.classification_description`

**Calculation:**

The test has two parts. The plan must satisfy EITHER:

**Part A -- Eligibility Open to All:**
If `available_to_all_equally == true`, the test is automatically satisfied. No further calculation needed.

**Part B -- Nondiscriminatory Classification Test:**
If the plan is not open to all equally, it must satisfy the nondiscriminatory classification test under Treas. Reg. 1.125-7(b):

```
HCI_eligible_pct = (HCIs who are eligible / total non-excludable HCIs) * 100
non_HCI_eligible_pct = (non-HCIs who are eligible / total non-excludable non-HCIs) * 100

Safe harbor: non_HCI_eligible_pct >= safe_harbor_percentage
```

The safe harbor percentage depends on the employer's non-HCI concentration percentage, based on the table in Treas. Reg. 1.410(b)-4(c)(4)(iv):

| NHC Concentration % | Safe Harbor % | Unsafe Harbor % |
|---|---|---|
| 0-60 | 50.00 | 40.00 |
| 61 | 49.25 | 39.25 |
| 62 | 48.50 | 38.50 |
| ... | ... | ... |
| 99 | 20.75 | 20.75 |

NHC Concentration % = (non-HCI non-excludable employees / total non-excludable employees) * 100

The plan passes if `non_HCI_eligible_pct >= safe_harbor_percentage` for the employer's NHC concentration.

If the percentage falls between safe harbor and unsafe harbor, the plan may still pass based on facts and circumstances -- the system reports this as "CONDITIONAL PASS -- requires facts and circumstances review."

**Pass/fail:**
- PASS: available to all, OR satisfies safe harbor.
- CONDITIONAL: falls between safe and unsafe harbor.
- FAIL: below unsafe harbor.

**Edge cases:**
- If there are 0 non-excludable HCIs, the test is automatically satisfied (no favored group to discriminate toward).
- If there are 0 non-excludable non-HCIs, the test is automatically satisfied (the entire workforce is HCI).
- Former employees who were eligible during the plan year are included if they were non-excludable.

### 6.2 Section 125 Contributions and Benefits Test

**IRC Section 125(b)(2): The plan must not discriminate in favor of highly compensated participants as to contributions and benefits.**

**Input data:**
- All non-excludable employees who are participants (elected at least one benefit)
- HCI/HCP flags (HCP = highly compensated participant = HCI who elected benefits)
- Salary reduction amounts and employer contribution amounts by benefit type
- Plan design: `section_125.same_benefits_all`, `section_125.safe_harbor_75pct_health`

**Calculation:**

**Safe Harbor Method (Treas. Reg. 1.125-7(c)(2)):**
If the employer contributes at least 75% of the cost of the most expensive health plan option for single coverage, AND all participants can elect the same maximum benefit, the test is deemed satisfied.

Check: `section_125.safe_harbor_75pct_health == true`.

**General Test (if safe harbor not met):**
The plan must not provide HCPs with benefits that are disproportionately greater than those available to non-HCPs. This is evaluated qualitatively:

1. Are the same benefits available to all participants? (`same_benefits_all`)
2. If not, is the classification nondiscriminatory? (Same test as eligibility.)
3. Are employer contributions the same percentage of compensation for HCPs and non-HCPs?

Quantitative check:
```
avg_benefit_hcp = total_nontaxable_benefits_hcps / hcp_count
avg_benefit_non_hcp = total_nontaxable_benefits_non_hcps / non_hcp_count
ratio = avg_benefit_hcp / avg_benefit_non_hcp
```

If `ratio > 1.0` and `same_benefits_all == false`, flag as potential failure.

**Pass/fail:**
- PASS: safe harbor met, OR same benefits available to all, OR quantitative test shows no disproportionate benefit.
- FAIL: HCPs receive disproportionately higher benefits.

### 6.3 Section 125 Key Employee Concentration Test

**IRC Section 125(b)(3): Nontaxable benefits provided to key employees must not exceed 25% of the aggregate nontaxable benefits provided to all employees.**

**Input data:**
- Key employee flags (`is_key_employee_416i`)
- Total nontaxable benefits (salary reductions + employer contributions for each participant)
- Key employees at non-covered entities (`m_key_non_covered_entities` from plan data)

**Calculation:**

```
key_employee_benefits = SUM(total_salary_reduction + total_employer_contribution)
    for all employees WHERE is_key_employee_416i == true AND is_participant == true

total_benefits = SUM(total_salary_reduction + total_employer_contribution)
    for ALL participants

concentration_pct = (key_employee_benefits / total_benefits) * 100
```

**Pass/fail:**
- PASS: `concentration_pct <= 25.0`
- FAIL: `concentration_pct > 25.0`

**Consequence of failure:** Nontaxable benefits of key employees are included in their gross income.

**Edge cases:**
- If no key employees participate, the test automatically passes (0% concentration).
- Key employees at non-covered entities in the controlled group must be included in the numerator if they participate in any cafeteria plan.
- Employer-provided benefits under the plan (not just salary reductions) count toward the numerator.

### 6.4 Section 105(h) Eligibility Test

**IRC Section 105(h)(3): A self-insured medical plan (including Health FSA) must not discriminate in favor of highly compensated individuals as to eligibility to participate.**

**Applicable only when:** Benefits offered include Health FSA or Limited Purpose FSA.

**Input data:**
- All non-excludable employees (using 105(h) exclusion rules: `is_excludable_105h`)
- HCI flags (using 105(h) definition: `is_hci_105h`)
- Benefit/eligibility flags for the self-insured plan

**Calculation:**

The plan must satisfy AT LEAST ONE of three alternative tests:

**Test 1 -- 70% Test:**
```
pct_benefiting = (employees_benefiting / total_non_excludable) * 100
PASS if pct_benefiting >= 70
```
"Benefiting" means eligible to participate in the self-insured plan (not necessarily enrolled).

**Test 2 -- 70%/80% Test:**
```
pct_eligible = (employees_eligible / total_non_excludable) * 100
pct_eligible_who_benefit = (employees_benefiting / employees_eligible) * 100
PASS if pct_eligible >= 70 AND pct_eligible_who_benefit >= 80
```

**Test 3 -- Nondiscriminatory Classification Test:**
Same classification test as Section 125 eligibility (Treas. Reg. 1.410(b)-4 safe harbor / unsafe harbor table), applied using 105(h) HCI and exclusion definitions.

```
hci_benefiting_pct = (105h_HCIs_benefiting / total_non_excludable_105h_HCIs) * 100
non_hci_benefiting_pct = (non_HCIs_benefiting / total_non_excludable_non_HCIs) * 100
PASS if non_hci_benefiting_pct >= safe_harbor_percentage (from concentration table)
```

**Pass/fail:**
- PASS: any one of the three tests is satisfied.
- FAIL: none of the three tests is satisfied.

### 6.5 Section 105(h) Benefits Test

**IRC Section 105(h)(4): Benefits provided under a self-insured plan must not discriminate in favor of highly compensated individuals.**

**Input data:**
- Plan design questions from `section_105h` in plan data.

**Calculation:**

The benefits test is primarily qualitative. The system evaluates five components:

1. **Same types of benefits:** Are the same expenses reimbursable for HCIs and non-HCIs? (`same_expenses`)
2. **Same maximum benefits:** Is the maximum reimbursement the same? (`same_maximum`)
3. **Same cost sharing:** Are employee contributions the same? (`same_cost_sharing`)
4. **Same waiting periods:** Are waiting periods the same? (`same_waiting_periods`)
5. **Same dependent coverage:** Is dependent coverage the same? (`same_dependent_coverage`)

Additional checks:
- Executive physicals or other benefits available only to HCIs? (`executive_physicals`)
- Benefits changed mid-year affecting groups differently? (`benefit_changes_mid_year`)

**Pass/fail:**
- PASS: All five components are the same for HCIs and non-HCIs, no executive-only benefits, no discriminatory mid-year changes.
- FAIL: Any component provides more favorable benefits to HCIs.

**Note:** This test is inherently qualitative. The system renders a pass/fail based on the yes/no answers, but a FAIL result should include the recommendation to consult a benefits attorney.

### 6.6 Section 129 Eligibility Test

**IRC Section 129(d)(2)-(3): The DCFSA must not discriminate in favor of employees who are highly compensated employees (HCEs).**

**Input data:**
- All non-excludable employees (using 129 exclusion rules: `is_excludable_129`)
- HCE flags (using 129 definition: `is_hce_129`)
- DCFSA eligibility flags

**Calculation:**

Same structure as Section 125 eligibility test, but using 129-specific HCE and exclusion definitions.

If `section_129.available_to_all_equally == true`, the test automatically passes.

Otherwise, apply the nondiscriminatory classification test:
```
hce_eligible_pct = (HCEs eligible for DCFSA / total non-excludable HCEs) * 100
non_hce_eligible_pct = (non-HCEs eligible for DCFSA / total non-excludable non-HCEs) * 100
PASS if non_hce_eligible_pct >= safe_harbor_percentage
```

**Pass/fail:** Same as Section 125 eligibility (PASS, CONDITIONAL, or FAIL).

### 6.7 Section 129 Benefits and Contributions Test

**IRC Section 129(d)(2): Benefits provided under the DCFSA must not discriminate in favor of HCEs.**

**Input data:**
- Plan design questions from `section_129` in plan data.

**Calculation:**

Qualitative test similar to 105(h) benefits:
- Same maximum DCFSA election for HCEs and non-HCEs? (`same_maximum`)
- Same terms and conditions? (`same_terms`)
- If employer makes nonelective contributions, are they the same for all? (`employer_nonelective_contributions`, `nonelective_same_terms`)

**Pass/fail:**
- PASS: Same maximum, same terms, and nonelective contributions (if any) are nondiscriminatory.
- FAIL: HCEs receive more favorable terms.

### 6.8 Section 129 More-Than-5% Owners Test

**IRC Section 129(d)(4): Not more than 25% of the amounts paid or incurred by the employer for dependent care assistance during the year may be provided for the class of individuals who are shareholders or owners (or their spouses or dependents) owning more than 5% of the stock or of the capital or profits interest.**

**Input data:**
- All employees where `ownership.ownership_percentage > 5.0` OR (`ownership.related_to_owner == true` AND the related owner has >5%)
- DCFSA election amounts for owners and their family members
- Total DCFSA benefits for all participants

**Calculation:**

```
owner_dcfsa = SUM(benefits.salary_reductions.dcfsa + benefits.employer_contributions.dcfsa)
    for employees WHERE ownership_percentage > 5
    PLUS amounts for spouses/dependents of >5% owners

total_dcfsa = SUM(benefits.salary_reductions.dcfsa + benefits.employer_contributions.dcfsa)
    for ALL DCFSA participants

owner_concentration_pct = (owner_dcfsa / total_dcfsa) * 100
```

**Pass/fail:**
- PASS: `owner_concentration_pct <= 25.0`
- FAIL: `owner_concentration_pct > 25.0`

**Consequence of failure:** Amounts provided to >5% owners (and their spouses/dependents) are included in their gross income.

### 6.9 Section 129 55% Average Benefits Test

**IRC Section 129(d)(8): The average benefit provided to non-HCEs under all DCFSA plans of the employer must be at least 55% of the average benefit provided to HCEs.**

**Input data:**
- HCE flags (`is_hce_129`)
- DCFSA election and benefit amounts
- Excludable employees for this test (includes employees with compensation under $25,000)

**Calculation:**

For this test specifically, employees earning under $25,000 can be excluded (in addition to the standard 129 exclusions). Apply the special exclusion:

```
test_population = non-excludable employees (129 rules)
    MINUS employees WHERE compensation.annual_compensation_current < 25000

hce_participants = employees in test_population WHERE is_hce_129 == true AND dcfsa_election > 0
non_hce_participants = employees in test_population WHERE is_hce_129 == false AND dcfsa_election > 0

avg_hce_benefit = SUM(dcfsa_benefits for HCE participants) / COUNT(hce_participants)
avg_non_hce_benefit = SUM(dcfsa_benefits for non-HCE participants) / COUNT(non_hce_participants)

ratio = (avg_non_hce_benefit / avg_hce_benefit) * 100
```

**Pass/fail:**
- PASS: `ratio >= 55.0`
- FAIL: `ratio < 55.0`

**Edge cases:**
- If no HCEs participate in the DCFSA, the test is automatically satisfied.
- If no non-HCEs participate, the test fails (the DCFSA is exclusively benefiting HCEs).
- The test uses ACTUAL benefits provided during the year, not elections. If the system only has election data (pre-year), it should use elections as a proxy and note this limitation.

---

## 7. PHI / PII Protection Requirements

### 7.1 Data Minimization

**What data does the system actually need?**

For test computations, the system needs:
- Date of birth (for age-based exclusions) -- age alone is sufficient if DOB is unavailable.
- Hire date and termination date (for service-based exclusions).
- Hours worked (for hours-based exclusions).
- Compensation amounts (for HCE/HCI thresholds and benefit ratio calculations).
- Ownership percentage and officer status.
- Benefit enrollment and election amounts.
- Union/CBA status.
- Nonresident alien status.

**What data does the system NOT need?**
- Social Security Numbers -- NEVER needed for any test computation.
- Home addresses.
- Phone numbers.
- Email addresses.
- Detailed medical claim information.
- Specific diagnoses or health conditions.

**Anonymization recommendation:**
The system should work with anonymized employee IDs by default. Employee names are accepted during document parsing (needed for cross-file matching) but can be replaced with sequential IDs (`EMP-001`, `EMP-002`, etc.) after all files are merged. The user should have the option to "Anonymize Names" which replaces all `display_name` values with IDs and sets `identity.anonymized = true`. This reduces the sensitivity of the stored data without affecting test computations.

### 7.2 SSN Handling

**Policy: Never store SSNs. Never send SSNs to the AI API.**

Implementation:
1. **Pre-upload scan:** Before any file processing, scan the raw file content for SSN patterns using regex: `\b\d{3}-\d{2}-\d{4}\b` and `\b\d{9}\b` (in numeric-only contexts).
2. **If SSNs detected:** Replace with `***-**-{last4}` in the working copy. The last 4 digits are retained only temporarily for cross-file matching (e.g., matching a census employee to a payroll record). After merge is complete, the last 4 digits are also scrubbed.
3. **Original file handling:** The original uploaded file is stored on disk temporarily for audit/re-parse purposes. It is auto-deleted after the test run is submitted or after 30 days, whichever comes first. The original file is NEVER sent to the AI API.
4. **Log the scrub:** Record `ssn_detected = true`, `ssn_scrubbed = true`, and the count of SSNs found (never log actual SSN values) in the `ndt_document_upload` record.

### 7.3 AI Processing -- Data Sent to Anthropic API

**Principle: Send the minimum data necessary for column mapping and parsing.**

**For column mapping (the primary AI use case):**
- Send: column headers, 3-5 sample rows with PII scrubbed (names replaced with "Person A", "Person B", SSNs removed, DOBs retained because they are needed for age calculation context).
- Do NOT send: full file contents, original filenames, employer name.

**For PDF parsing (if PDF upload is supported):**
- Send: the text content of the PDF with SSNs scrubbed.
- Names are needed in this context because the AI must extract tabular data where names are row identifiers.
- Compensation and benefit data must be sent because that is what the AI is extracting.

**Anthropic API data handling:**
- Anthropic's API does not train on customer data.
- API inputs are not stored by Anthropic beyond the API request lifecycle (per Anthropic's data usage policy as of 2025).
- Use the `anthropic-version` header and review Anthropic's latest data retention commitments before production deployment.

**Additional controls:**
- All API calls use HTTPS (TLS 1.3).
- API key stored in `ssa.properties` on the server, never in client-side code or version control.
- Consider using Anthropic's data processing addendum (DPA) for compliance documentation.

### 7.4 Encryption at Rest

**Recommendation: Application-level encryption for the JSON columns.**

MySQL's `AES_ENCRYPT`/`AES_DECRYPT` functions work but have limitations:
- Key management is complex (storing the encryption key in the app vs. MySQL keyring).
- Queries on encrypted data are not possible (but we do not need to query within the JSON).
- Adds complexity to every read/write operation.

**Recommended approach:**
- Use Java's `javax.crypto` (AES-256-GCM) to encrypt the `census_data` and `test_results` JSON strings before writing to the database and decrypt after reading.
- Store the encryption key in `ssa.properties` (the server configuration file, not in the codebase).
- The `plan_data` JSON does not contain PII (it is plan-level configuration) and does not require encryption.
- The `parse_log` and `gap_analysis` JSON columns may contain employee references but only by anonymized ID -- encrypt if names are used.

**Column type adjustment:** If encrypting, change `census_data JSON` to `census_data MEDIUMBLOB` (encrypted data is binary, not valid JSON). The application handles serialization/deserialization and encryption/decryption.

Alternatively, if the encryption key management burden is too high for the current phase, an acceptable intermediate step is:
- MySQL TDE (Transparent Data Encryption) at the tablespace level, which encrypts the data files on disk without application code changes.
- This protects against disk theft / backup exposure but not against database-level access by compromised credentials.

### 7.5 Encryption in Transit

- **Browser to server:** HTTPS only (already enforced via nginx reverse proxy with Let's Encrypt certificates).
- **Server to AI API:** HTTPS only (Anthropic API requires TLS).
- **Server to MySQL:** If MySQL is on the same host (localhost), the connection does not traverse the network. If on a separate host, use MySQL SSL connections (`useSSL=true` in JDBC URL).

### 7.6 Access Control

**Who can access NDT test data?**

| Role | View Census | Edit Census | Run Tests | View Results | Download Report |
|---|---|---|---|---|---|
| Questionnaire submitter (agent/user) | Yes | Yes | Yes | Yes | Yes |
| PSP Admin (role 5) | Yes | Yes | Yes | Yes | Yes |
| Agency Admin (role 8) | Their agencies only | Their agencies only | Their agencies only | Their agencies only | Their agencies only |
| BPO User (role 102/103) | Yes (read-only) | No | No | Yes | Yes |
| Other roles | No | No | No | No | No |

**Implementation:**
- The `ndt_test_run` is linked to an `activity_id`. Use the existing activity-level access control (activity ownership, agency assignment) to gate access.
- The `NdtAccessLog` table records every access event with person_id, action, and timestamp.
- JSP pages check `AmsDataLocal.getCurrentPerson()` against the activity ownership chain.

### 7.7 Data Retention

**Policy: Retain census data only as long as needed for the testing engagement.**

| Data | Retention Period | Purge Method |
|---|---|---|
| Census JSON (census_data) | 90 days after test run is completed/submitted | Scheduled job replaces with `{"purged": true, "purged_at": "..."}` |
| Test results JSON | 3 years (matches typical plan document retention requirements) | Manual purge after retention period |
| Uploaded files (on disk) | 30 days after test run completion | Scheduled job deletes files from `/var/lib/tomcat10/ndt-uploads/` |
| Plan-level data (plan_data) | Same as test results (3 years) | Purged with test results |
| Access log | 3 years | Purged with test results |

**User-initiated deletion:** The submitter or PSP admin can delete a test run at any time. This immediately purges all census data and uploaded files. Test results are retained per policy unless explicitly deleted by a PSP admin.

### 7.8 HIPAA Considerations

**Is the census data PHI?**

PHI (Protected Health Information) under HIPAA is individually identifiable health information created or received by a covered entity. The key question is whether the benefit enrollment data in the census constitutes "health information."

**Analysis:**
- **Enrollment in a group health plan** (e.g., "enrolled in medical plan, family tier") is generally NOT considered PHI by itself. It is administrative data about plan participation, not information about health status, conditions, or treatments.
- **Specific benefit elections** (e.g., "elected Health FSA for $2,850") are similarly administrative and not PHI.
- **Premium amounts** are financial/administrative data, not health information.
- **HOWEVER:** If the uploaded documents contain claims data, diagnoses, treatment records, or detailed utilization information (e.g., "cancer treatment FSA claim"), this IS PHI.

**Risk mitigation:**
1. The system is designed to NOT request or process claims data. The upload guidance explicitly states "benefit enrollment and elections" not "claims history."
2. If claims data appears in an uploaded document, the PII scrubber should flag medical terms (ICD codes, diagnosis keywords) and warn the user.
3. The system documentation should state that it is not designed to process PHI and that users should not upload documents containing claims or treatment information.

**Practical recommendation:** Even though the data may not strictly be PHI, treat it with HIPAA-like safeguards (encryption, access controls, audit logging, data retention limits) because:
- The data includes individually identifiable information (names + compensation + benefit elections).
- A conservative approach avoids regulatory risk.
- It builds trust with users who handle benefits data professionally.

### 7.9 BAA Requirements

**Do we need a Business Associate Agreement with Anthropic?**

A BAA is required when a business associate creates, receives, maintains, or transmits PHI on behalf of a covered entity.

**Analysis:**
- SSA is NOT a covered entity (it is a TPA/administrator, not a health plan or healthcare provider).
- However, SSA may be a business associate OF covered entities (the employers whose plans are being tested).
- If SSA sends data to Anthropic's API that includes individually identifiable benefit enrollment data, Anthropic could be considered a subcontractor/business associate.

**Recommendation:**
1. **Minimize what is sent to the AI.** If only column headers and anonymized sample rows are sent, there is no PHI transmitted and no BAA is needed.
2. **If full census data must be sent to the AI** (e.g., for PDF parsing with employee names and benefits), a BAA with Anthropic would be prudent.
3. **Anthropic's current position:** As of 2025, Anthropic offers a data processing addendum (DPA) but does not sign BAAs for the general API. This may change. Check current terms before deployment.
4. **Alternative:** Use a self-hosted AI model for parsing tasks that require PII. This eliminates the BAA question entirely but increases infrastructure costs.

### 7.10 Audit Trail

Every interaction with census data is logged in `ndt_access_log`:

| Action | Logged Data |
|---|---|
| `view_census` | person_id, test_run_id, IP address, timestamp |
| `edit_census` | person_id, test_run_id, fields changed (keys only, not values), IP, timestamp |
| `upload_document` | person_id, test_run_id, document_type, filename, IP, timestamp |
| `delete_document` | person_id, test_run_id, upload_id, IP, timestamp |
| `run_tests` | person_id, test_run_id, tests_run, IP, timestamp |
| `view_results` | person_id, test_run_id, IP, timestamp |
| `download_results` | person_id, test_run_id, format (PDF), IP, timestamp |
| `export_data` | person_id, test_run_id, export_type, IP, timestamp |

The log is append-only. Log entries cannot be modified or deleted except through the data retention purge process.

### 7.11 Document Upload Security

1. **File type validation:** Whitelist only `.csv`, `.xlsx`, `.xls`, `.pdf`. Reject all other extensions. Also verify MIME type matches extension (prevent `.csv` files that are actually executables).
2. **File size limits:** 10MB per file, 50MB total per test run.
3. **Virus scanning:** Integrate ClamAV (open-source) via `clamscan` command-line invocation before processing. If ClamAV is not available, use file-type validation as a minimum control.
4. **File storage:** Outside the web root (`/var/lib/tomcat10/ndt-uploads/`). Files are served through a servlet (like the existing `ServeVideo` pattern) with access control, never via direct URL.
5. **Filename sanitization:** Strip path traversal characters (`../`, `..\\`), special characters, and truncate to 200 characters. Store with a system-generated filename (`{upload_id}_{sanitized_name}`).

### 7.12 Browser-Side Protections

1. **No caching of sensitive pages:** Set HTTP headers on all NDT wizard pages:
   ```
   Cache-Control: no-store, no-cache, must-revalidate
   Pragma: no-cache
   Expires: 0
   ```
2. **Autocomplete off:** All form inputs handling employee data should have `autocomplete="off"`.
3. **Session timeout:** NDT wizard pages should use the existing AMS session timeout (30 minutes). Census data is never stored in the browser session or localStorage.
4. **CSRF protection:** All form submissions include a CSRF token (existing AMS pattern).
5. **Content Security Policy:** Restrict inline scripts and external resource loading on NDT pages.
6. **No client-side census data:** The employee roster table on Page 7 is rendered server-side (JSP). Do not send the full census JSON to the browser. If AJAX pagination is needed, send one page of employees at a time.

---

## 8. Implementation Phases

### Phase 1: Data Model and Basic Census Upload (4-6 weeks)

**Deliverables:**
- Database migration V059 creating `ndt_test_run`, `ndt_document_upload`, and `ndt_access_log` tables.
- JPA entities: `NdtTestRun`, `NdtDocumentUpload`, `NdtAccessLog`.
- `NdtTestRunDAO` with CRUD operations.
- `CensusEmployee` POJO (the Java representation of one employee JSON object).
- `NdtPlanData` POJO (the Java representation of plan-level JSON).
- `NdtUploadServlet` handling file upload, validation, and storage.
- CSV/Excel parsing using Apache POI and OpenCSV -- deterministic column mapping (no AI yet). Hardcoded mappings for common column header patterns.
- Basic wizard JSP pages (Pages 1-4 skeleton).
- PII scrubber for SSN detection and removal.

**Testing:** Upload a sample census CSV, verify it parses into the JSON structure, verify SSNs are scrubbed.

### Phase 2: AI-Powered Document Parsing (3-4 weeks)

**Deliverables:**
- `NdtAiParsingService` integrating with Anthropic Claude API.
- AI column mapping (send headers + samples, receive mapping JSON).
- Column mapping confirmation UI (Page 5).
- PDF parsing support for billing statements.
- Census merge logic (cross-file matching by name, conflict resolution).
- Confidence scoring.
- Parse logging.

**Testing:** Upload census + payroll from different sources, verify merge logic, verify conflict resolution UI.

### Phase 3: Auto-Tagging Engine (2-3 weeks)

**Deliverables:**
- `NdtTaggingService` implementing all tagging rules from Section 5.
- IRC 318 constructive ownership attribution.
- Compensation ranking (percentile calculation).
- Officer ranking.
- Exclusion tagging (all three code sections).
- Tag confidence scoring.
- Employee roster review page (Page 7) with tag display and override capability.

**Testing:** Verify tagging against known test scenarios. Build a set of 5 employer test cases with known correct tag assignments.

### Phase 4: Test Computation Engine (3-4 weeks)

**Deliverables:**
- `NdtTestEngine` implementing all 9 tests from Section 6.
- Safe harbor / unsafe harbor lookup table.
- Test results JSON generation.
- Test results display page (Page 8) with pass/fail cards and calculation details.
- Edge case handling (zero populations, no participants, etc.).

**Testing:** Verify test calculations against manually computed results for each of the 5 test employer scenarios.

### Phase 5: Gap Analysis and Targeted Questions (2-3 weeks)

**Deliverables:**
- `NdtGapAnalysisService` that examines the census data after parsing and identifies missing fields needed for each applicable test.
- Gap analysis page (Page 6) with dynamically generated questions.
- Plan design question sections (eligibility rules, 105(h) benefits, 129 terms).
- Integration with the existing `questionnaire` save/progress mechanism.
- Page 3 (structural questions) fully wired.

**Testing:** Upload a partial census (missing hours data), verify the gap analysis correctly identifies missing fields and generates appropriate questions.

### Phase 6: Results Reporting and PDF Export (2-3 weeks)

**Deliverables:**
- PDF report generation using iText or Apache PDFBox.
- Report format: cover page, test summary, detailed calculations per test, employee roster summary (anonymized), data quality notes, attestation.
- Attestation page (Page 9).
- Status management (lock test run on submission).
- Data retention scheduler (purge census data after 90 days).
- Access logging integration throughout all pages.

**Testing:** Generate PDF reports for all 5 test employer scenarios. Verify data retention purge works correctly.

### Total Estimated Timeline: 16-23 weeks (4-6 months)

---

## 9. Risk Assessment

### 9.1 AI Parsing Accuracy

**Risk:** The AI incorrectly maps columns or extracts values, leading to wrong test inputs.

**Mitigation:**
- **Mandatory user confirmation** of column mappings before data is accepted (Page 5).
- **Sample data preview** showing the first 5 parsed rows so the user can spot-check.
- **Confidence scores** with visual indicators -- low-confidence mappings are highlighted in yellow/red.
- **Override capability** at every level: column mapping, individual field, individual employee, individual tag.
- **Fallback to manual entry:** If parsing fails or confidence is too low, the system falls back to the traditional questionnaire approach for that data section.

**Residual risk:** LOW after user confirmation. The AI is a convenience layer, not an authority. The user remains responsible for data accuracy.

### 9.2 Unexpected Document Formats

**Risk:** Users upload documents in formats the system cannot parse (password-protected files, scanned images, proprietary formats, multi-sheet Excel workbooks with complex layouts).

**Mitigation:**
- **File type whitelist:** Only accept CSV, XLSX, XLS, and PDF.
- **Graceful failure:** If parsing fails, show a clear error message: "We could not parse this file. Please try exporting your data as a CSV from your HR system." Provide instructions for common HR systems (ADP, Paychex, Gusto, BambooHR).
- **Multi-sheet handling:** For Excel files with multiple sheets, show a sheet selector and let the user choose which sheet contains the relevant data.
- **Orientation detection:** Some files have employee data in columns instead of rows (transposed). The AI should detect this and transform accordingly.
- **Encoding issues:** Detect file encoding (UTF-8, ISO-8859-1, Windows-1252) and normalize to UTF-8.

**Residual risk:** MEDIUM. There will always be file formats the system cannot handle. The manual-entry fallback ensures the test can still be completed.

### 9.3 Incomplete Employee Data

**Risk:** Uploaded files are missing critical fields (no hours data, no DOB, no compensation), making it impossible to compute certain tags or tests.

**Mitigation:**
- **Gap analysis** (Phase 5) explicitly identifies missing data and asks targeted questions.
- **Reasonable defaults with warnings:** For missing hours, assume full-time (2080 hours) and flag the assumption. For missing DOB, skip age-based exclusion tests and note the limitation.
- **Partial test results:** If data is insufficient for a specific test, mark that test as "INCONCLUSIVE -- insufficient data" rather than failing it. Explain what data is needed to complete the test.
- **Data quality score:** The overall score (0.0-1.0) gives the user a clear signal about how reliable the results are.

**Residual risk:** LOW. The gap analysis system is designed to handle this gracefully.

### 9.4 Legal Liability for Incorrect Test Results

**Risk:** The system produces a PASS result, the employer relies on it, and the IRS later determines the plan was discriminatory.

**Mitigation:**
- **Attestation disclaimer:** The attestation page includes language such as: "These test results are computed based on the data provided by the user. SSA does not guarantee the accuracy of test results. Users are responsible for the completeness and accuracy of all input data. This system does not constitute legal or tax advice. Consult with qualified ERISA counsel for definitive testing determinations."
- **Data quality warnings** are included in the PDF report alongside test results.
- **Assumption documentation:** Every assumption the system makes (default values, AI parsing decisions, auto-resolutions) is logged and included in the report.
- **Professional review recommendation:** For any CONDITIONAL or borderline PASS result, the system recommends professional review.

**Residual risk:** MEDIUM. This is inherent in any automated compliance testing tool. The disclaimers and documentation reduce but do not eliminate liability. The system should be positioned as an aid to compliance professionals, not a replacement for them.

### 9.5 Performance with Large Employers

**Risk:** Employers with 1,000+ employees generate large JSON documents (2-4MB), and auto-tagging + test computation may be slow.

**Mitigation:**
- **JSON document size:** MySQL 8.0 handles JSON documents up to 1GB. A 5,000-employee census at 2KB per employee = 10MB -- well within limits.
- **Java processing:** Deserializing 5,000 employee records into a `List<CensusEmployee>` takes milliseconds. Tagging all 5,000 employees involves simple arithmetic and sorting -- sub-second execution.
- **Test computation:** All tests are aggregate calculations (sums, counts, percentages). Even with 10,000 employees, computation completes in under 1 second.
- **AI parsing:** Sending 5,000 rows to the AI is unnecessary. The AI only needs column headers + 5 sample rows for mapping. Full parsing is deterministic (CSV/Excel cell extraction).
- **Browser rendering:** The employee roster table (Page 7) should paginate at 50 rows per page to avoid rendering 5,000 table rows.

**Residual risk:** LOW. The architecture is designed for batch processing in memory, which handles large datasets efficiently.

### 9.6 Data Breach

**Risk:** A security breach exposes employee PII (names, DOBs, compensation, benefit elections).

**Mitigation:**
- Encryption at rest (Section 7.4).
- Encryption in transit (Section 7.5).
- Access control (Section 7.6).
- Data retention limits (Section 7.7).
- Audit logging (Section 7.10).
- Name anonymization option (Section 7.1).
- SSN scrubbing (Section 7.2).
- File storage outside web root (Section 7.11).

**Residual risk:** LOW with all controls implemented. The most sensitive data (SSNs) is never stored. Names can be anonymized. Census data is purged after 90 days.

### 9.7 AI API Availability

**Risk:** The Anthropic API is unavailable, slow, or returns errors during document parsing.

**Mitigation:**
- **Graceful degradation:** If the AI API is unavailable, fall back to deterministic column mapping using the hardcoded pattern-matching rules from Phase 1.
- **Timeout handling:** Set a 30-second timeout on AI API calls. If exceeded, fall back to pattern matching.
- **Retry logic:** One automatic retry with exponential backoff on transient errors (429, 500, 503).
- **Offline capability:** The deterministic parser (Phase 1) works without any AI dependency. The AI is an enhancement, not a requirement.

**Residual risk:** LOW. The system is functional without AI. AI improves accuracy and handles unusual column headers, but is not required.

---

## Appendix A: IRS Threshold Reference Table

These thresholds are indexed annually. The system should store the current year's thresholds in `plan_data.thresholds` and allow manual override for different plan years.

| Threshold | 2025 Value | 2026 Value | IRC Section |
|---|---|---|---|
| HCE compensation (414(q)) | $155,000 | $160,000 | 414(q)(1)(B) |
| Key employee officer compensation | $220,000 | $230,000 | 416(i)(1)(A)(i) |
| Key employee 1% owner compensation | $150,000 | $150,000 (not indexed) | 416(i)(1)(A)(iii) |
| Health FSA maximum salary reduction | $3,300 | $3,300 | 125(i)(2) |
| DCFSA maximum exclusion | $5,000 | $5,000 (not indexed) | 129(a)(2) |
| HSA maximum (self-only) | $4,300 | $4,300 | 223(b)(2) |
| HSA maximum (family) | $8,550 | $8,550 | 223(b)(2) |

**Note on 2026 values:** The values above are estimates based on projected indexing. Confirm IRS published figures before the 2026 plan year.

## Appendix B: Java Package Structure

```
net.superiorstate.ams.controller.activity.ndt/
    NdtWizardServlet.java          -- Wizard page controller (step routing)
    NdtUploadServlet.java          -- Document upload handler
    NdtTestRunServlet.java         -- Test execution + results API

net.superiorstate.ams.data.service/
    NdtTestRunService.java         -- Business logic orchestration
    NdtParsingService.java         -- File parsing (CSV/Excel/PDF)
    NdtAiMappingService.java       -- Anthropic API integration for column mapping
    NdtCensusMergeService.java     -- Cross-file merge logic
    NdtTaggingService.java         -- Auto-tagging engine
    NdtTestEngine.java             -- Test computation (all 9 tests)
    NdtGapAnalysisService.java     -- Missing data identification
    NdtPiiScrubber.java            -- SSN detection and removal
    NdtReportGenerator.java        -- PDF report generation

net.superiorstate.ams.data.dao/
    NdtTestRunDAO.java             -- Database operations

net.superiorstate.ams.model.activity.ndt/
    NdtTestRun.java                -- JPA entity
    NdtDocumentUpload.java         -- JPA entity
    NdtAccessLog.java              -- JPA entity
    CensusEmployee.java            -- POJO for JSON serialization
    NdtPlanData.java               -- POJO for JSON serialization
    NdtTestResults.java            -- POJO for JSON serialization
    EmployeeTags.java              -- POJO for tag data
    ColumnMapping.java             -- POJO for AI mapping response

src/main/webapp/WEB-INF/view/ndt/
    ndtWizard.jsp                  -- Main wizard container
    ndtStep1_employer.jsp          -- Employer info
    ndtStep2_benefits.jsp          -- Benefits offered
    ndtStep3_structure.jsp         -- Structural questions
    ndtStep4_upload.jsp            -- Document upload zone
    ndtStep5_mapping.jsp           -- Column mapping confirmation
    ndtStep6_gaps.jsp              -- Gap analysis / targeted questions
    ndtStep7_roster.jsp            -- Employee roster review
    ndtStep8_results.jsp           -- Test results
    ndtStep9_attestation.jsp       -- Attestation and submit
```

## Appendix C: Nondiscriminatory Classification Safe Harbor Table

This is the lookup table referenced in Sections 6.1, 6.4, and 6.6 for determining whether a classification-based eligibility test passes. Source: Treas. Reg. 1.410(b)-4(c)(4)(iv).

| Non-HCI Concentration % | Safe Harbor % | Unsafe Harbor % |
|---|---|---|
| 0-60 | 50.00 | 40.00 |
| 61 | 49.25 | 39.25 |
| 62 | 48.50 | 38.50 |
| 63 | 47.75 | 37.75 |
| 64 | 47.00 | 37.00 |
| 65 | 46.25 | 36.25 |
| 66 | 45.50 | 35.50 |
| 67 | 44.75 | 34.75 |
| 68 | 44.00 | 34.00 |
| 69 | 43.25 | 33.25 |
| 70 | 42.50 | 32.50 |
| 71 | 41.75 | 31.75 |
| 72 | 41.00 | 31.00 |
| 73 | 40.25 | 30.25 |
| 74 | 39.50 | 29.50 |
| 75 | 38.75 | 28.75 |
| 76 | 38.00 | 28.00 |
| 77 | 37.25 | 27.25 |
| 78 | 36.50 | 26.50 |
| 79 | 35.75 | 25.75 |
| 80 | 35.00 | 25.00 |
| 81 | 34.25 | 24.25 |
| 82 | 33.50 | 23.50 |
| 83 | 32.75 | 22.75 |
| 84 | 32.00 | 22.00 |
| 85 | 31.25 | 21.25 |
| 86 | 30.50 | 20.50 |
| 87 | 29.75 | 20.00 |
| 88 | 29.00 | 20.00 |
| 89 | 28.25 | 20.00 |
| 90 | 27.50 | 20.00 |
| 91 | 26.75 | 20.00 |
| 92 | 26.00 | 20.00 |
| 93 | 25.25 | 20.00 |
| 94 | 24.50 | 20.00 |
| 95 | 23.75 | 20.00 |
| 96 | 23.00 | 20.00 |
| 97 | 22.25 | 20.00 |
| 98 | 21.50 | 20.00 |
| 99 | 20.75 | 20.00 |

---

## 10. Flexible Data Input Model

NDT testing requires specific employee data fields to make classification determinations (HCI, HCE, Key Employee, excludable, etc.). In practice, the test taker (employer HR person) often does not have -- or does not want to share -- exact values for every field. They may know that "the owner earns well above the HCI threshold" without knowing the exact IRS-indexed amount, or they may be able to say "all three officers are in the top 25% of earners" without providing individual compensation figures.

The flexible input model allows the system to accept data at varying levels of precision and still make classification determinations where possible, flagging gaps only when the provided information is genuinely insufficient.

### 10a. Value Types per Field

Every employee data field that feeds into a classification determination can accept one of the following input types:

| Input Type | Description | Example |
|---|---|---|
| `EXACT` | Precise numeric or categorical value | compensation = $87,500; ownership = 8.2% |
| `ABOVE_THRESHOLD` | Value is known to exceed a specific threshold | "Compensation exceeds $160,000" (the HCE threshold) |
| `BELOW_THRESHOLD` | Value is known to be below a specific threshold | "Hours worked are under 1,000/year" |
| `PERCENTILE` | Position in a ranked distribution | "In top 25% of earners"; "Not in top 25%" |
| `BINARY_FLAG` | Direct yes/no classification answer | "Is this person a Key Employee? Yes" |
| `GROUP_DECLARATION` | Assertion applied to an entire group | "All officers earn above $160,000" |
| `RANGE` | Value falls between two bounds | "Compensation is between $100,000 and $130,000" |
| `UNKNOWN` | Value not provided and not determinable | No hours data available |

**Which fields accept which types:**

| Field | EXACT | ABOVE/BELOW_THRESHOLD | PERCENTILE | BINARY_FLAG | GROUP_DECLARATION | RANGE | UNKNOWN |
|---|---|---|---|---|---|---|---|
| `compensation.annual_compensation_current` | Y | Y (HCE threshold) | Y (top 25%) | N | Y (group comp statement) | Y | Y |
| `ownership.ownership_percentage` | Y | Y (5% threshold) | N | Y ("is >5% owner?") | N | Y | Y |
| `ownership.is_officer` | N/A (already boolean) | N | N | Y | Y ("these people are officers") | N | Y |
| `employment.hours_worked_annual` | Y | Y (1000 hrs threshold) | N | Y ("is part-time?") | Y ("all salaried are full-time") | Y | Y |
| `identity.age_at_plan_year_end` | Y | Y (21 threshold) | N | N | N | Y | Y |
| `employment.months_of_service_total` | Y | Y (12 months threshold) | N | N | Y ("all listed employees have 1+ year") | Y | Y |
| `benefits.is_plan_eligible` | N/A (boolean) | N | N | Y | Y ("all FT employees are eligible") | N | Y |
| `benefits.is_participant` | N/A (boolean) | N | N | Y | Y ("all eligible employees participate") | N | Y |
| `ownership.related_to_owner` | N/A (boolean) | N | N | Y | N | N | Y |
| `employment.union_status` | N/A (categorical) | N | N | Y | Y ("no union employees") | N | Y |
| `employment.is_nonresident_alien` | N/A (boolean) | N | N | Y | Y ("no NRAs") | N | Y |

### 10b. JSON Schema Updates

The current employee JSON schema (Section 1.1) stores flat scalar values. To support flexible inputs, each classifiable field is wrapped in a `FlexValue` object. The census JSON transitions from flat values to annotated values for fields that feed classifications.

**FlexValue schema:**

```json
{
  "value": 87500,
  "input_type": "EXACT",
  "threshold": null,
  "threshold_name": null,
  "percentile": null,
  "percentile_direction": null,
  "range_low": null,
  "range_high": null,
  "flag_value": null,
  "group_id": null,
  "source": "census_upload",
  "source_detail": null,
  "confidence": "HIGH",
  "notes": null,
  "reviewed": false,
  "reviewed_by": null,
  "reviewed_at": null
}
```

**Field definitions:**

| Field | Type | Description |
|---|---|---|
| `value` | Number/String/null | The exact value if known (input_type=EXACT). Null for all other input types. |
| `input_type` | String enum | One of: EXACT, ABOVE_THRESHOLD, BELOW_THRESHOLD, PERCENTILE, BINARY_FLAG, GROUP_DECLARATION, RANGE, UNKNOWN |
| `threshold` | Number/null | The threshold value when input_type is ABOVE_THRESHOLD or BELOW_THRESHOLD. E.g., 160000 for "above the HCE threshold." |
| `threshold_name` | String/null | Human-readable threshold label. E.g., "HCE compensation threshold (IRC 414(q))" |
| `percentile` | Integer/null | The percentile value when input_type is PERCENTILE. E.g., 25 for "top 25%." |
| `percentile_direction` | String/null | "TOP" or "BOTTOM" when input_type is PERCENTILE. E.g., "TOP" for "in top 25%." |
| `range_low` | Number/null | Lower bound when input_type is RANGE. |
| `range_high` | Number/null | Upper bound when input_type is RANGE. |
| `flag_value` | Boolean/null | The boolean answer when input_type is BINARY_FLAG. True = "yes, this classification applies." |
| `group_id` | String/null | Reference to a group declaration ID when input_type is GROUP_DECLARATION. Links to a group-level assertion. |
| `source` | String | How this value was obtained. One of: "census_upload", "payroll_upload", "questionnaire_answer", "ai_parsed", "user_entered", "group_declaration", "system_default", "reviewer_override" |
| `source_detail` | String/null | Specific source reference. E.g., "Column F of payroll_export.csv", "Question 4 of employer wizard", "Row 14 of census.xlsx" |
| `confidence` | String enum | HIGH, MEDIUM, LOW. HIGH = exact value or explicit confirmation. MEDIUM = AI-parsed or inferred. LOW = default assumption or ambiguous input. |
| `notes` | String/null | Free-text annotation by user or reviewer. |
| `reviewed` | Boolean | Has a PSP reviewer confirmed this value? |
| `reviewed_by` | Long/null | Person ID of reviewer. |
| `reviewed_at` | String/null | ISO 8601 timestamp of review. |

**Updated employee JSON example with FlexValues:**

```json
{
  "employee_id": "EMP-001",
  "source_row_id": 14,

  "identity": {
    "display_name": "Employee 001",
    "anonymized": true,
    "hire_date": "2019-03-15",
    "termination_date": null,
    "date_of_birth": "1985-07-22",
    "age_at_plan_year_end": {
      "value": 41,
      "input_type": "EXACT",
      "source": "census_upload",
      "source_detail": "Column C of census.xlsx",
      "confidence": "HIGH",
      "reviewed": false
    }
  },

  "compensation": {
    "annual_compensation_current": {
      "value": null,
      "input_type": "ABOVE_THRESHOLD",
      "threshold": 160000,
      "threshold_name": "HCE compensation threshold (IRC 414(q))",
      "source": "questionnaire_answer",
      "source_detail": "Employer stated this person earns above the HCE threshold",
      "confidence": "MEDIUM",
      "reviewed": false
    }
  },

  "ownership": {
    "ownership_percentage": {
      "value": 8.2,
      "input_type": "EXACT",
      "source": "census_upload",
      "source_detail": "Column J of census.xlsx",
      "confidence": "HIGH",
      "reviewed": true,
      "reviewed_by": 1042,
      "reviewed_at": "2026-03-15T14:22:00Z"
    },
    "is_officer": {
      "value": true,
      "input_type": "BINARY_FLAG",
      "flag_value": true,
      "source": "questionnaire_answer",
      "confidence": "HIGH",
      "reviewed": false
    }
  },

  "employment": {
    "hours_worked_annual": {
      "value": null,
      "input_type": "GROUP_DECLARATION",
      "group_id": "GRP-SALARIED-FT",
      "source": "group_declaration",
      "source_detail": "Employer declared: all salaried employees work full-time (2080+ hours)",
      "confidence": "MEDIUM",
      "reviewed": false
    }
  }
}
```

**Group Declaration entity:**

Group declarations are stored separately and referenced by `group_id`. They allow a single employer assertion to populate values across multiple employees.

```json
{
  "group_declarations": [
    {
      "group_id": "GRP-SALARIED-FT",
      "description": "All salaried employees work full-time (2080+ hours/year)",
      "field_path": "employment.hours_worked_annual",
      "assertion_type": "ABOVE_THRESHOLD",
      "threshold": 1000,
      "applies_to_filter": {
        "field": "employment.job_classification",
        "operator": "equals",
        "value": "salaried"
      },
      "employee_ids": ["EMP-001", "EMP-003", "EMP-007"],
      "source": "questionnaire_answer",
      "source_detail": "Wizard page 5, question: Are all salaried employees full-time?",
      "created_by": 2055,
      "created_at": "2026-03-10T09:15:00Z"
    },
    {
      "group_id": "GRP-OFFICERS-HCE",
      "description": "All officers earn above $160,000",
      "field_path": "compensation.annual_compensation_current",
      "assertion_type": "ABOVE_THRESHOLD",
      "threshold": 160000,
      "applies_to_filter": {
        "field": "ownership.is_officer",
        "operator": "equals",
        "value": true
      },
      "employee_ids": ["EMP-002", "EMP-005", "EMP-011"],
      "source": "questionnaire_answer",
      "source_detail": "Wizard page 5, question: Do all officers earn above the HCE threshold?",
      "created_by": 2055,
      "created_at": "2026-03-10T09:18:00Z"
    }
  ]
}
```

**Java POJO:**

```java
public class FlexValue<T> {
    private T value;
    private String inputType;       // EXACT, ABOVE_THRESHOLD, etc.
    private Double threshold;
    private String thresholdName;
    private Integer percentile;
    private String percentileDirection; // TOP, BOTTOM
    private Double rangeLow;
    private Double rangeHigh;
    private Boolean flagValue;
    private String groupId;
    private String source;
    private String sourceDetail;
    private String confidence;      // HIGH, MEDIUM, LOW
    private String notes;
    private boolean reviewed;
    private Long reviewedBy;
    private String reviewedAt;

    // Convenience methods
    public boolean isExact() { return "EXACT".equals(inputType); }
    public boolean isKnownAbove(double threshold) {
        return "EXACT".equals(inputType) && value instanceof Number && ((Number) value).doubleValue() > threshold
            || "ABOVE_THRESHOLD".equals(inputType) && this.threshold != null && this.threshold >= threshold;
    }
    public boolean isKnownBelow(double threshold) {
        return "EXACT".equals(inputType) && value instanceof Number && ((Number) value).doubleValue() < threshold
            || "BELOW_THRESHOLD".equals(inputType) && this.threshold != null && this.threshold <= threshold;
    }
    public boolean isDeterminate() { return !"UNKNOWN".equals(inputType); }
}
```

**Backward compatibility:** Fields that do NOT feed classifications (e.g., `display_name`, `hire_date`, `enrolled_benefits`) remain flat scalars. Only fields listed in the table in Section 10a are wrapped in FlexValue. The `CensusEmployee` POJO uses FlexValue for classification-relevant fields and plain types for everything else.

### 10c. Classification Logic with Flexible Inputs

The auto-tagging engine (Section 5) must handle every input type and determine whether the available information is sufficient to make a classification. For each classification, the following table specifies which input types produce a deterministic result, which are insufficient, and what confidence level applies.

**HCI -- Section 125 (ownership > 5%)**

| Input Type for `ownership_percentage` | Can Determine? | Result | Confidence |
|---|---|---|---|
| EXACT (value > 5.0) | YES | is_hci_125 = true | HIGH |
| EXACT (value <= 5.0) | YES | is_hci_125 = false (for ownership prong) | HIGH |
| ABOVE_THRESHOLD (threshold >= 5.0) | YES | is_hci_125 = true | HIGH |
| BELOW_THRESHOLD (threshold <= 5.0) | YES | is_hci_125 = false (for ownership prong) | HIGH |
| ABOVE_THRESHOLD (threshold < 5.0) | NO | Insufficient -- value is above some lower number but may or may not exceed 5% | N/A |
| BINARY_FLAG (flag_value = true for "is >5% owner") | YES | is_hci_125 = true | HIGH (direct answer) |
| BINARY_FLAG (flag_value = false) | YES | is_hci_125 = false (for ownership prong) | HIGH |
| RANGE (low > 5.0) | YES | is_hci_125 = true | MEDIUM |
| RANGE (high <= 5.0) | YES | is_hci_125 = false | MEDIUM |
| RANGE (low <= 5.0, high > 5.0) | NO | Insufficient -- straddles the threshold | N/A |
| UNKNOWN | NO | Cannot determine -- flag as gap | N/A |

**HCI -- Section 125 (compensation test: comp > indexed threshold, e.g., $160,000 for 2026)**

| Input Type for `annual_compensation_current` | Can Determine? | Result | Confidence |
|---|---|---|---|
| EXACT (value > threshold) | YES | HCI by compensation = true | HIGH |
| EXACT (value <= threshold) | YES | HCI by compensation = false | HIGH |
| ABOVE_THRESHOLD (threshold >= IRS threshold) | YES | HCI by compensation = true | HIGH |
| ABOVE_THRESHOLD (threshold < IRS threshold) | NO | Insufficient -- above some lower value but unknown if above IRS threshold | N/A |
| BELOW_THRESHOLD (threshold <= IRS threshold) | YES | HCI by compensation = false | HIGH |
| PERCENTILE ("top 25%") | PARTIAL | Suggests HCI but only if we know the IRS threshold maps to top 25% or lower -- typically insufficient without knowing the actual distribution | LOW |
| BINARY_FLAG ("is HCI? yes") | YES | HCI by compensation = true | HIGH |
| GROUP_DECLARATION ("all officers earn above $160K") | YES (for members) | HCI by compensation = true for all group members | MEDIUM |
| RANGE (low > threshold) | YES | HCI by compensation = true | MEDIUM |
| RANGE (high <= threshold) | YES | HCI by compensation = false | MEDIUM |
| UNKNOWN | NO | Flag as gap | N/A |

**Top 25% Earners -- Section 105(h) HCI**

| Input Type for `annual_compensation_current` | Can Determine? | Result | Confidence |
|---|---|---|---|
| EXACT (for all employees) | YES | System ranks all employees, computes percentiles, flags top 25% | HIGH |
| EXACT (for some) + UNKNOWN (for others) | PARTIAL | Can rank known employees but incomplete ranking; flag as gap if any UNKNOWN employees could change the cutoff | MEDIUM |
| PERCENTILE ("in top 25%") | YES | Direct answer: is_hci_105h = true | HIGH |
| PERCENTILE ("not in top 25%") | YES | Direct answer: is_hci_105h = false | HIGH |
| BINARY_FLAG ("is 105(h) HCI? yes") | YES | is_hci_105h = true | HIGH |
| GROUP_DECLARATION ("these 5 people are the top 25%") | YES (for members) | is_hci_105h = true for listed members, false for all others | MEDIUM |
| ABOVE_THRESHOLD / BELOW_THRESHOLD | NO | Threshold-based input cannot determine percentile ranking without knowing all compensations | N/A |
| UNKNOWN | NO | Flag as gap | N/A |

**Excludable by Hours (< 1,000 hours/year)**

| Input Type for `hours_worked_annual` | Can Determine? | Result | Confidence |
|---|---|---|---|
| EXACT (value < 1000) | YES | is_excludable = true (hours prong) | HIGH |
| EXACT (value >= 1000) | YES | is_excludable = false (hours prong) | HIGH |
| ABOVE_THRESHOLD (threshold >= 1000) | YES | is_excludable = false | HIGH |
| BELOW_THRESHOLD (threshold <= 1000) | YES | is_excludable = true | HIGH |
| BINARY_FLAG ("is part-time / under 1000 hours? yes") | YES | is_excludable = true | HIGH |
| GROUP_DECLARATION ("all salaried are full-time") | YES (for members) | is_excludable = false for salaried employees | MEDIUM |
| RANGE (low >= 1000) | YES | is_excludable = false | MEDIUM |
| RANGE (high < 1000) | YES | is_excludable = true | MEDIUM |
| UNKNOWN | NO | Flag as gap (default to non-excludable with warning) | N/A |

**Tagging engine decision flow:**

```
For each employee, for each classification:
  1. Resolve the FlexValue for the relevant field(s)
  2. If input_type == GROUP_DECLARATION, look up the group assertion and derive the effective input_type
  3. Check the classification-specific table above
  4. If deterministic:
     a. Set the tag value
     b. Set tag_confidence based on the input type confidence
     c. Set the tag_reason (e.g., "ownership_percentage ABOVE_THRESHOLD 5.0%")
  5. If insufficient:
     a. Set tag value to null (indeterminate)
     b. Add to gap analysis: { field, classification, input_type_provided, what_is_needed }
     c. Generate a plain-English follow-up question for the employer
```

**Compound classifications:** Some classifications require multiple fields. For example, Key Employee (IRC 416(i)) requires BOTH officer status AND compensation ranking. The tagging engine evaluates each field independently and combines:
- If ALL required fields are deterministic: classification is deterministic
- If ANY required field is insufficient: classification is insufficient -- add to gap analysis
- If fields are deterministic but conflicting (should not happen with correct logic): flag for reviewer

### 10d. Tracking Provenance

Every data point in the census must carry provenance metadata for audit defensibility. The `FlexValue` object already contains `source`, `source_detail`, `confidence`, and `reviewed` fields. This section defines the provenance tracking infrastructure beyond individual values.

**Override History:**

When a value is changed (by the employer providing updated data, by the reviewer correcting a parsed value, or by a system re-parse), the system records the change in an override log stored alongside the census data.

```json
{
  "override_log": [
    {
      "override_id": "OVR-001",
      "employee_id": "EMP-003",
      "field_path": "compensation.annual_compensation_current",
      "previous_value": {
        "value": 85000,
        "input_type": "EXACT",
        "source": "ai_parsed",
        "confidence": "MEDIUM"
      },
      "new_value": {
        "value": 95000,
        "input_type": "EXACT",
        "source": "reviewer_override",
        "confidence": "HIGH",
        "reviewed": true,
        "reviewed_by": 1042,
        "reviewed_at": "2026-03-15T14:30:00Z"
      },
      "reason": "AI parsed W-2 amount instead of total compensation. Corrected per payroll report.",
      "changed_by": 1042,
      "changed_at": "2026-03-15T14:30:00Z",
      "change_source": "reviewer_dashboard"
    },
    {
      "override_id": "OVR-002",
      "employee_id": "EMP-007",
      "field_path": "employment.hours_worked_annual",
      "previous_value": {
        "value": null,
        "input_type": "UNKNOWN"
      },
      "new_value": {
        "value": null,
        "input_type": "GROUP_DECLARATION",
        "group_id": "GRP-SALARIED-FT",
        "source": "group_declaration",
        "confidence": "MEDIUM"
      },
      "reason": "Employer confirmed all salaried employees work full-time in follow-up response #2.",
      "changed_by": 2055,
      "changed_at": "2026-03-16T11:00:00Z",
      "change_source": "followup_response"
    }
  ]
}
```

**Source Document Registry:**

Each uploaded document is registered with metadata linking it to the fields it populated.

```json
{
  "source_documents": [
    {
      "document_id": "DOC-001",
      "upload_id": 45,
      "original_filename": "2026_census_export.xlsx",
      "document_type": "census",
      "upload_timestamp": "2026-03-10T09:00:00Z",
      "uploaded_by": 2055,
      "parse_method": "ai_assisted",
      "fields_populated": [
        "identity.display_name",
        "identity.hire_date",
        "identity.date_of_birth",
        "employment.status",
        "employment.job_classification",
        "compensation.annual_compensation_current",
        "ownership.ownership_percentage"
      ],
      "row_count": 47,
      "parse_confidence": "HIGH",
      "column_mapping_confirmed": true,
      "column_mapping_confirmed_by": 2055,
      "column_mapping_confirmed_at": "2026-03-10T09:05:00Z"
    },
    {
      "document_id": "DOC-002",
      "upload_id": 46,
      "original_filename": "payroll_summary_q4.csv",
      "document_type": "payroll",
      "upload_timestamp": "2026-03-10T09:12:00Z",
      "uploaded_by": 2055,
      "parse_method": "deterministic",
      "fields_populated": [
        "compensation.w2_compensation",
        "benefits.salary_reductions.medical_premium",
        "benefits.salary_reductions.health_fsa"
      ],
      "row_count": 47,
      "parse_confidence": "HIGH",
      "column_mapping_confirmed": true
    }
  ]
}
```

**Provenance query:** For any cell in the employee roster grid, the reviewer can click to see a provenance card showing:
1. Current value and input type
2. Source document and column/row
3. Whether AI-parsed or user-entered
4. Whether reviewed, by whom, and when
5. Override history (if any prior values existed)
6. Tags affected by this value and their current status

**Java implementation:** The `override_log` and `source_documents` arrays are stored as additional JSON fields on the `ndt_test_run` entity, alongside `census_data`. The `NdtTestRun` POJO includes:

```java
@Column(name = "override_log", columnDefinition = "JSON")
private String overrideLogJson;

@Column(name = "source_documents", columnDefinition = "JSON")
private String sourceDocumentsJson;
```

Deserialized into `List<OverrideEntry>` and `List<SourceDocument>` POJOs respectively by the service layer.

---

## 11. Two-Tier Workflow (Test Taker vs. PSP Reviewer)

The NDT system serves two fundamentally different user personas who interact with the same underlying data but have completely different expertise, terminology needs, and workflow patterns.

### 11a. Test Taker Flow (External-Facing)

The test taker is the employer's HR or benefits person. They know their employees and payroll but do not know IRS testing terminology. The wizard must speak their language.

**Page 1: Welcome and Employer Information**

Content:
- Brief explanation: "We need some information about your company and employees to complete your benefits compliance testing. This usually takes 10-15 minutes, plus any time to gather documents."
- Fields: Employer name (pre-populated from activity), plan year start/end dates, entity type (C-corp, S-corp, LLC, partnership, sole proprietorship, non-profit, government), EIN (optional, for matching)
- "Save and continue" progresses to Page 2

No jargon. No mention of "NDT", "Section 125", "HCI", or any IRC references.

**Page 2: What Benefits Does Your Plan Offer?**

Plain-English benefit checklist with brief explanations:

| Checkbox | Label | Explanation |
|---|---|---|
| [ ] | Medical insurance (pre-tax premiums) | "Employees pay their share of medical premiums before taxes are taken out" |
| [ ] | Dental insurance (pre-tax premiums) | "Employees pay dental premiums before taxes" |
| [ ] | Vision insurance (pre-tax premiums) | "Employees pay vision premiums before taxes" |
| [ ] | Health FSA (Flexible Spending Account) | "Employees set aside pre-tax money for medical expenses not covered by insurance" |
| [ ] | Dependent Care FSA | "Employees set aside pre-tax money for daycare, after-school care, or elder care" |
| [ ] | HSA (Health Savings Account) | "Employees with a high-deductible health plan save pre-tax money for medical expenses" |
| [ ] | Self-insured medical plan | "Your company pays claims directly (or through a TPA) instead of buying a fully-insured policy" |
| [ ] | Other pre-tax benefits | Free text field for description |

The selections on this page determine which NDT tests are applicable (Section 125 for any cafeteria plan, Section 105(h) for self-insured, Section 129 for DCFSA). The test taker never sees this mapping.

**Page 3: Company Structure**

Simple yes/no questions with plain explanations:

1. "Does your company have any related companies, parent companies, or subsidiaries?" (controlled group question)
   - If yes: "Please list them" (name + approximate employee count)
2. "Do any of your employees belong to a union covered by a collective bargaining agreement?"
   - If yes: "Approximately how many?"
3. "Do you have any employees who are not US citizens or permanent residents working in the US?"
   - If yes: "Approximately how many?"
4. "Is your benefits plan available to all employees, or only certain groups?"
   - If certain groups: "Which groups are eligible? (e.g., full-time only, after 90 days, salaried only)"
5. "Are there any owners, partners, or shareholders who own more than 5% of the company?"
   - If yes: "How many? Can you tell us who they are, or would you prefer to include that in the documents you upload?"

**Page 4: Upload Zone**

Header: "Share Your Employee Data"

Instructions (conversational tone):
> "The most helpful thing you can share is a list of your employees with their employment details. Don't worry about getting us a perfect file -- we can work with whatever you have. Here are some examples of documents that are helpful:"
>
> - Employee census or roster from your HR system
> - Payroll summary showing compensation amounts
> - Benefits enrollment report showing who elected which benefits
> - Any report from your HR/payroll system (ADP, Paychex, Gusto, BambooHR, etc.)
>
> "The more information in the files, the fewer follow-up questions we will need to ask."

Upload interface:
- Large drag-and-drop zone with "or click to browse" link
- Accepted formats: CSV, Excel (.xlsx, .xls), PDF
- Up to 5 files, 10MB each
- Each uploaded file shows: filename, size, a green checkmark on successful upload
- "I don't have any files to upload" checkbox -- skips to Page 5 and triggers a longer question set

**Page 5: Quick Questions**

This page is dynamically generated based on what was NOT found in the uploaded documents (gap analysis runs after parsing). If uploads covered all needed data, this page shows: "We found everything we need in your uploaded files! Click Continue to review."

If gaps exist, questions are generated in plain English. Examples:

- If hours data is missing: "Do all of your employees work full-time (at least 1,000 hours per year), or do you have part-time employees?" (radio: All full-time / Some part-time / Not sure)
  - If some part-time: "Can you tell us which employees are part-time, or approximately how many?"

- If ownership data is missing and Page 3 indicated >5% owners: "You mentioned there are owners with more than 5% of the company. Can you tell us their names or employee IDs so we can match them to the employee list?"

- If compensation data is incomplete: "For the employees where we could not find compensation information, can you tell us approximately how many earn above $160,000 per year?" (This is the HCE threshold, but the test taker just sees a dollar amount.)

- If officer status is unknown: "Does your company have any officers (President, VP, Secretary, Treasurer, or similar titles)? If so, how many?"

Each question maps to one or more FlexValue updates. The answers are stored with `source: "questionnaire_answer"`.

**Page 6: Review What We Received**

Header: "Here's What We Found"

Summary cards:
- "We found **47 employees** in your uploaded files"
- "We have compensation data for **45 of 47** employees"
- "We identified **3 owners** with more than 5% ownership"
- "**2 items** need your attention" (link to items needing confirmation)

Items needing attention are displayed as simple cards:
- "We found two employees with the same name (John Smith). Are these the same person or two different people?" (radio buttons)
- "The compensation for Employee #14 looks unusually high ($850,000). Is this correct?" (Yes, that's correct / No, let me fix it)

The test taker does NOT see the full employee roster grid, tags, or classification data. They see a simplified summary.

**Page 7: Thank You**

> "All done! Your testing advisor will review the information you provided and run the compliance tests. If we need any clarification, we will send you a short follow-up with specific questions."
>
> "You can check the status of your test anytime by returning to this page."

Status indicator: Submitted / Under Review / Follow-Up Needed / Tests Complete

### 11b. PSP Reviewer Flow (Internal-Facing)

The PSP reviewer is a testing expert. They need full technical detail, data manipulation tools, and test execution controls.

**Reviewer Dashboard -- accessed via the activity detail page or a dedicated NDT menu item.**

**Panel 1: Data Overview**

Top bar showing test run status: `SUBMITTED` | `PARSING` | `IN_REVIEW` | `FOLLOW_UP_SENT` | `TESTING` | `COMPLETE` | `SUBMITTED_TO_CLIENT`

Summary metrics:
- Total employees: 47
- Data completeness: 89% (42/47 employees fully populated)
- Parse confidence: HIGH (95% of fields parsed with high confidence)
- Documents uploaded: 2 (census.xlsx, payroll_q4.csv)
- Open gaps: 5
- Tests applicable: Section 125 Eligibility, Section 125 Key Employee, Section 105(h) Eligibility, Section 105(h) Benefits, Section 129 Eligibility, Section 129 Benefits, Section 129 5% Owner, Section 129 55% Average

Document list with status:
| Document | Type | Rows | Fields Mapped | Confidence | Actions |
|---|---|---|---|---|---|
| census.xlsx | Census | 47 | 7 of 12 | HIGH | View Mapping / Re-parse / Download |
| payroll_q4.csv | Payroll | 47 | 3 of 12 | HIGH | View Mapping / Re-parse / Download |

**Panel 2: Employee Roster**

Full data grid with every employee and every field. This is the primary working surface for the reviewer.

Column groups: Identity | Employment | Compensation | Ownership | Benefits | Tags

Color coding:
- **Green cell**: HIGH confidence, reviewed or exact value
- **Yellow cell**: MEDIUM confidence, AI-parsed or group declaration
- **Red cell**: LOW confidence or UNKNOWN -- needs attention
- **Blue cell**: Value provided via BINARY_FLAG or GROUP_DECLARATION (not raw data)
- **Gray cell**: Not applicable for this employee

Cell interactions:
- **Click any cell**: Opens provenance panel showing source, confidence, override history, and edit controls
- **Edit a value**: Changes input_type to "reviewer_override", records in override_log, recalculates affected tags
- **Bulk select + action**: Select multiple employees by checkbox, then:
  - "Flag all as: [classification dropdown]" -- sets BINARY_FLAG for selected employees
  - "Set field to: [value]" -- sets EXACT value for selected employees
  - "Apply group declaration" -- creates a new GROUP_DECLARATION for the selected group
  - "Mark as reviewed" -- sets reviewed=true, reviewed_by, reviewed_at for all selected cells

Filters:
- By tag: Show only HCIs, show only excludable, show only Key Employees
- By confidence: Show only LOW/UNKNOWN values (gap-focused view)
- By data completeness: Show only employees with missing fields
- By source: Show only AI-parsed values, show only user-entered values
- By review status: Show only unreviewed values

Sorting: Any column, ascending/descending

Export: Download current view as CSV (for offline review)

**Panel 3: Gap Analysis**

System-generated list of data gaps, priority-ranked by impact on test execution.

| Priority | Gap | Affects Tests | Employees Impacted | Suggested Action |
|---|---|---|---|---|
| CRITICAL | Compensation missing for 5 employees | 125 Elig, 105(h) Elig, 129 55% Avg | EMP-012, EMP-018, EMP-023, EMP-031, EMP-044 | Ask employer for payroll data or comp ranges |
| HIGH | Hours data missing for all employees | 125 Elig (exclusion), 105(h) Elig (exclusion) | All 47 | Ask employer if all employees are full-time |
| MEDIUM | Officer status unknown | 125 Key Employee, 105(h) HCI | All 47 | Ask employer to identify officers |
| LOW | Prior year compensation missing | 125 HCI (prior year lookback) | All 47 | Ask employer or use current year only |

Each gap row expands to show:
- Why this data matters (which test, which classification, what happens if missing)
- What input types would resolve it (EXACT preferred, but ABOVE_THRESHOLD or BINARY_FLAG may suffice)
- Pre-built follow-up question in plain English, ready to send to the employer
- "Resolve manually" button to enter values directly or make assumptions

**Panel 4: Follow-Up Builder**

The reviewer composes follow-up communications to send to the test taker. The system pre-generates question templates based on the gap analysis.

Workflow:
1. Reviewer sees the gap list and selects which gaps to address in this round
2. For each selected gap, the system proposes a plain-English question (no jargon)
3. Reviewer can edit the question text, add context, or combine multiple gaps into one question
4. Reviewer clicks "Send Follow-Up" which:
   a. Creates a `FollowUpRequest` entity linked to the test run
   b. Sends an email/notification to the test taker with a link to a mini-questionnaire
   c. Sets the test run status to `FOLLOW_UP_SENT`

Pre-built question templates:

| Gap Type | Template Question |
|---|---|
| Missing compensation | "For the following employees, can you provide their approximate annual compensation (salary + bonus)? If exact amounts are not available, can you tell us if each person earns above or below $[threshold]?" |
| Missing hours | "Are all of your employees full-time (working at least 1,000 hours per year)? If some are part-time, can you identify which ones?" |
| Missing officer status | "Does your company have any officers (President, Vice President, Secretary, Treasurer, or similar titles)? If so, who holds these positions?" |
| Missing ownership | "Can you confirm which individuals (if any) own more than 5% of the company? If ownership details are sensitive, you can simply confirm whether each person's ownership is above or below 5%." |
| Ambiguous merge | "We found two employees with similar names: [Name A] and [Name B]. Are these the same person or two different people?" |

**Panel 5: Test Runner**

Available once the reviewer determines data is sufficient (either all gaps resolved, or reviewer accepts the data as-is with noted limitations).

Interface:
- List of all applicable tests with current readiness status:
  - GREEN: All required data available, ready to run
  - YELLOW: Some data gaps but test can run with assumptions (listed)
  - RED: Critical data missing, test cannot run

- "Run All Tests" button (runs all GREEN and YELLOW tests)
- Individual "Run" button per test
- Results display per test:
  - **PASS** (green banner): Test passed with calculation details expandable
  - **FAIL** (red banner): Test failed with calculation details, consequence explanation, and remediation suggestions
  - **CONDITIONAL** (yellow banner): Passed safe harbor but not unsafe harbor -- needs further analysis
  - **INCONCLUSIVE** (gray banner): Insufficient data, with list of what is needed

- Override capability: Reviewer can override any individual tag before re-running tests
  - Override requires a reason (free text)
  - Override is logged in the audit trail
  - "Re-run with override" button

- Test results are stored as JSON in `ndt_test_run.test_results` (see Section 6 for format)

**Panel 6: Report Generation**

After tests are complete:
- "Generate Report" button produces a PDF (see Phase 6 in Section 8)
- Report preview in-browser before download
- Report includes: cover page, test summary, detailed calculations, data quality notes, assumptions, attestation
- "Submit to Client" button changes status to `SUBMITTED_TO_CLIENT` and optionally sends the report via email

### 11c. Communication Loop

The workflow is iterative. The data structures below support the full lifecycle of follow-up communication between the PSP reviewer and the test taker.

**FollowUpRequest Entity:**

```java
@Entity
@Table(name = "ndt_followup_request")
public class NdtFollowUpRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "test_run_id")
    private NdtTestRun testRun;

    @Column(name = "request_number")
    private int requestNumber;          // Sequential: 1, 2, 3...

    @Column(name = "status")
    private String status;              // DRAFT, SENT, PARTIALLY_ANSWERED, COMPLETE, EXPIRED

    @Column(name = "questions_json", columnDefinition = "JSON")
    private String questionsJson;       // Array of FollowUpQuestion objects

    @ManyToOne
    @JoinColumn(name = "sent_by")
    private Person sentBy;              // PSP reviewer who sent it

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "sent_to_email")
    private String sentToEmail;         // Employer contact email

    @Column(name = "response_token")
    private String responseToken;       // Unique token for the response link (no login required)

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt; // Token valid for 14 days

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "responded_by_name")
    private String respondedByName;     // Name of the person who answered (self-reported)
}
```

**FollowUpQuestion POJO (serialized in questions_json):**

```json
{
  "questions": [
    {
      "question_id": "FQ-001",
      "question_text": "Are all of your employees full-time (working at least 1,000 hours per year)?",
      "question_type": "YES_NO_DETAIL",
      "target_field": "employment.hours_worked_annual",
      "target_classification": "is_excludable_125",
      "gap_priority": "HIGH",
      "response": null,
      "response_type": null,
      "response_timestamp": null,
      "flex_value_generated": null
    },
    {
      "question_id": "FQ-002",
      "question_text": "For the following employees, can you tell us their approximate annual compensation? If exact amounts are unavailable, please indicate whether each earns above or below $160,000.",
      "question_type": "EMPLOYEE_TABLE",
      "target_field": "compensation.annual_compensation_current",
      "target_classification": "is_hci_125",
      "gap_priority": "CRITICAL",
      "employee_ids": ["EMP-012", "EMP-018", "EMP-023"],
      "response": null,
      "response_type": null,
      "response_timestamp": null,
      "flex_value_generated": null
    }
  ]
}
```

**Question Types:**

| Type | UI Rendering | Response Format |
|---|---|---|
| `YES_NO` | Radio: Yes / No | Boolean |
| `YES_NO_DETAIL` | Radio: Yes / No + conditional text field ("If no, please explain") | Boolean + optional String |
| `NUMERIC` | Number input field | Number |
| `TEXT` | Free text input | String |
| `EMPLOYEE_TABLE` | Table listing specific employees with input fields per employee | Array of {employee_id, value} |
| `ABOVE_BELOW` | Per employee: radio "Above $X" / "Below $X" / "I'm not sure" | Array of {employee_id, input_type, threshold} |
| `MULTI_SELECT` | Checkbox list of options | Array of Strings |
| `FILE_UPLOAD` | File upload zone (for additional documents) | Upload ID reference |

**Mini-Questionnaire Renderer:**

When the test taker clicks the follow-up link (containing the `response_token`), the system renders a simplified questionnaire page containing ONLY the follow-up questions. This page:

- Does NOT require login (token-based access, like the existing video token pattern)
- Shows the employer name and a brief context message: "Your benefits advisor has a few follow-up questions about the information you submitted."
- Renders each question using the appropriate UI component for its `question_type`
- Has a single "Submit Answers" button
- On submission:
  1. Validates all required questions are answered
  2. Stores responses in the `questions_json` (updating each question's `response` field)
  3. Sets `responded_at` and `responded_by_name`
  4. Triggers the merge process: each response is converted to a FlexValue and applied to the relevant employee(s)
  5. Re-runs the auto-tagging engine on affected employees
  6. Updates the gap analysis
  7. Notifies the PSP reviewer that responses have been received
  8. Sets follow-up status to `COMPLETE` (or `PARTIALLY_ANSWERED` if some questions were skipped)

**Response-to-FlexValue Mapping:**

| Response Type | FlexValue Generated |
|---|---|
| YES_NO "Yes" to "are all employees full-time?" | GROUP_DECLARATION: all employees hours >= 1000, confidence MEDIUM |
| YES_NO "No" + detail "5 employees are part-time" | No direct FlexValue -- flags for manual identification of which 5 |
| NUMERIC value 95000 for EMP-012 compensation | EXACT: value=95000, source=followup_response, confidence HIGH |
| ABOVE_BELOW "Above $160,000" for EMP-018 | ABOVE_THRESHOLD: threshold=160000, source=followup_response, confidence HIGH |
| ABOVE_BELOW "I'm not sure" for EMP-023 | UNKNOWN: source=followup_response, notes="Employer unsure", remains in gap list |

**Lifecycle State Machine:**

```
Test Run Status Flow:

  CREATED ──> SUBMITTED ──> PARSING ──> IN_REVIEW
                                            │
                                            ├──> FOLLOW_UP_SENT ──> IN_REVIEW (responses received)
                                            │         │
                                            │         └──> FOLLOW_UP_SENT (additional follow-up)
                                            │
                                            ├──> TESTING ──> COMPLETE
                                            │                   │
                                            │                   └──> SUBMITTED_TO_CLIENT
                                            │
                                            └──> TESTING ──> IN_REVIEW (test revealed new gaps)

  Follow-Up Status Flow:

  DRAFT ──> SENT ──> PARTIALLY_ANSWERED ──> COMPLETE
                  │                              │
                  └──> EXPIRED (14 days)         └──> (responses merged into census)
```

**Email notification templates:**

1. **Follow-Up Request Email** (to test taker):
   - Subject: "Follow-up Questions for [Employer Name] Benefits Testing"
   - Body: Brief context, count of questions, link with token, expiration notice (14 days)
   - No technical jargon

2. **Response Received Notification** (to PSP reviewer):
   - Subject: "Follow-up Responses Received -- [Employer Name]"
   - Body: Summary of answers received, count of gaps resolved, count remaining, link to reviewer dashboard

3. **Reminder Email** (to test taker, sent at day 7 if no response):
   - Subject: "Reminder: Follow-up Questions for [Employer Name] Benefits Testing"
   - Body: Same as original with "This is a reminder" prefix

**Database table for follow-up requests:**

```sql
CREATE TABLE ndt_followup_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_run_id BIGINT NOT NULL,
    request_number INT NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    questions_json JSON NOT NULL,
    sent_by BIGINT,
    sent_at DATETIME,
    sent_to_email VARCHAR(255),
    response_token VARCHAR(64),
    token_expires_at DATETIME,
    responded_at DATETIME,
    responded_by_name VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_followup_test_run FOREIGN KEY (test_run_id) REFERENCES ndt_test_run(id),
    CONSTRAINT fk_followup_sent_by FOREIGN KEY (sent_by) REFERENCES person(id),
    UNIQUE KEY uk_response_token (response_token),
    INDEX idx_test_run (test_run_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 11d. Access Control

The access control model extends the existing AMS role system (Section 7.6) with NDT-specific permissions.

**Permission Matrix:**

| Action | Test Taker (via token) | Test Taker (logged in, role 2/3) | PSP Reviewer (role 5) | PSP Admin (role 5) | BPO User (role 102/103) | System |
|---|---|---|---|---|---|---|
| Submit initial data (wizard) | N/A | YES (own activities) | YES | YES | NO | N/A |
| Answer follow-up questions | YES (via token) | YES (own activities) | NO (sends questions, does not answer) | NO | NO | N/A |
| View own submission status | YES (via token, limited) | YES | N/A | N/A | N/A | N/A |
| View census data | NO | NO | YES (assigned activities) | YES (all) | YES (read-only) | YES |
| Edit census data | NO | NO | YES (assigned activities) | YES (all) | NO | YES (auto-tag) |
| Override tags/values | NO | NO | YES | YES | NO | NO |
| Run tests | NO | NO | YES | YES | NO | NO |
| View test results | NO | YES (own, after submission) | YES | YES | YES (read-only) | N/A |
| Send follow-up questions | NO | NO | YES | YES | NO | NO |
| Generate reports | NO | NO | YES | YES | YES | NO |
| Download reports | NO | YES (own) | YES | YES | YES | NO |
| View audit log | NO | NO | NO | YES | NO | N/A |
| Manage reviewers | NO | NO | NO | YES | NO | N/A |
| Delete test run | NO | NO | NO | YES | NO | N/A |
| Auto-parse documents | N/A | N/A | N/A | N/A | N/A | YES |
| Auto-tag employees | N/A | N/A | N/A | N/A | N/A | YES |
| Identify gaps | N/A | N/A | N/A | N/A | N/A | YES |
| Send reminder emails | N/A | N/A | N/A | N/A | N/A | YES (scheduled) |

**Token-based access:**

Follow-up response links use single-use-style tokens (similar to the existing `VideoToken` pattern from Session 73). The token:
- Is a 64-character random string generated by `SecureRandom`
- Expires after 14 days
- Is valid for multiple accesses (the test taker may return to the form)
- Is invalidated when the follow-up request status moves to `COMPLETE` or `EXPIRED`
- Does NOT grant access to any other part of the system
- Is scoped to the specific follow-up request (cannot be used to view census data, test results, or other follow-ups)

**Implementation pattern:**

```java
// In NdtFollowUpServlet (mapped to /ndt/followup)
String token = request.getParameter("token");
NdtFollowUpRequest followUp = ndtDao.findByResponseToken(token);

if (followUp == null) {
    request.setAttribute("error", "This link is not valid.");
    forward(request, response, "/WEB-INF/view/ndt/followupExpired.jsp");
    return;
}
if (followUp.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
    followUp.setStatus("EXPIRED");
    ndtDao.update(followUp);
    request.setAttribute("error", "This link has expired. Please contact your benefits advisor.");
    forward(request, response, "/WEB-INF/view/ndt/followupExpired.jsp");
    return;
}
if ("COMPLETE".equals(followUp.getStatus())) {
    request.setAttribute("message", "You have already submitted your answers. Thank you!");
    forward(request, response, "/WEB-INF/view/ndt/followupComplete.jsp");
    return;
}

// Render the mini-questionnaire
request.setAttribute("followUp", followUp);
request.setAttribute("questions", parseQuestions(followUp.getQuestionsJson()));
forward(request, response, "/WEB-INF/view/ndt/followupForm.jsp");
```

**Activity-level access gating:**

For logged-in users, NDT data access is gated through the activity ownership chain:
1. The `ndt_test_run` is linked to an `activity_id`
2. The activity has an assigned agent (person) and agency
3. PSP reviewers (role 5) can access test runs for activities assigned to their PSP
4. Agency admins (role 8) can access test runs for activities within their agency
5. The test taker (role 2 or 3) can access only their own activity's test run
6. BPO users (role 102/103) have read-only access to all test runs within their PSP scope

This reuses the existing `AmsDataLocal.getCurrentPerson()` and activity ownership validation patterns already established throughout AMS.

---

## 12. Iterative Upload Guidance & Data Completeness Engine

The upload phase is not a one-shot step. It is an iterative loop: each upload triggers re-analysis, updates a visual scorecard, and generates smarter suggestions about what to upload next. The system actively coaches the test taker toward data completeness by showing what is still needed, naming specific documents in plain English, and quantifying the impact of each potential upload.

---

### 12a. Data Completeness Scorecard

After each upload (and after the initial structural questions on Pages 1-3 are answered), the system displays a scorecard showing completion status across seven data categories. These categories map directly to the fields the test computation engine (Section 6) requires.

#### Category Definitions

| # | Category | Required For | Primary Data Sources |
|---|----------|-------------|---------------------|
| 1 | **Employee Roster** | All tests -- the foundation | Census/HR export |
| 2 | **Compensation Data** | HCI/HCE classification, contributions tests | Payroll YTD report, W-2 summary, census with comp column |
| 3 | **Benefits Enrollment** | Section 125 contributions test, Section 105(h) benefits test | Carrier list bill, enrollment report, benefits census |
| 4 | **Ownership & Officers** | HCI/HCE/Key Employee classification | Ownership declaration, K-1 schedules, org chart |
| 5 | **Plan Design & Eligibility Rules** | Eligibility tests, excludable employee determination | User answers only (no upload helps here) |
| 6 | **Hours & Service Data** | Excludable employee determination (part-time, service requirements) | Payroll, time tracking export, census with hours column |
| 7 | **Controlled Group / Related Entities** | Controlled group aggregation | User answers + entity list upload |

#### Scoring Logic

Each category is scored as a percentage based on the ratio of populated fields to required fields, weighted by employee coverage.

```java
public class CategoryScore {
    String categoryName;          // e.g., "Employee Roster"
    int categoryIndex;            // 1-7
    double completionPercent;     // 0.0 - 100.0
    int employeesCovered;         // employees with data in this category
    int employeesTotal;           // total employees in census
    String status;                // "complete", "partial", "missing"
    List<String> missingFields;   // plain-English descriptions of what's missing
    String primarySource;         // what document type would fill this
}
```

**Per-category field requirements and scoring weights:**

**Category 1 -- Employee Roster (weight: 1.0 per field):**
- `identity.display_name` or anonymized ID (required)
- `identity.hire_date` (required)
- `employment.employment_status` (required)
- `identity.termination_date` (required if terminated)
- `identity.date_of_birth` OR `identity.age_at_plan_year_end` (required for 105(h) age exclusion)
- Score = (employees with all required fields / total employees) * 100

**Category 2 -- Compensation Data (weight: 1.0 per field):**
- `compensation.annual_compensation_current` (required)
- `compensation.annual_compensation_prior_year` (required for 414(q) HCE threshold)
- `compensation.pre_tax_deductions.total` (needed for contributions test)
- `compensation.employer_contributions.total` (needed for contributions test)
- Score = (employees with at least current comp / total employees) * 100
- Bonus: prior-year comp and deduction breakdowns raise score above 75%

**Category 3 -- Benefits Enrollment (weight: 1.0 per field):**
- `benefits.is_eligible` (required)
- `benefits.is_enrolled` (required)
- `benefits.plan_tier` (required if enrolled)
- `benefits.employee_premium` and `benefits.employer_premium` (needed for contributions test)
- For each applicable benefit type (health FSA, DCFSA, HSA): election amount
- Score = (employees with enrollment status known / total employees) * 100

**Category 4 -- Ownership & Officers (weight: 1.0 per field):**
- For each owner: `ownership.ownership_percentage` (required)
- For each officer: `ownership.is_officer` + `ownership.officer_title` (required)
- `ownership.related_to_owner` + `ownership.relationship_type` (required for family attribution)
- Score = 100% if ownership declaration provided and all owners/officers identified
- Score = 0% if no ownership data at all
- Special: if entity type is Government or Tax-Exempt, category auto-scores 100% with note "Not applicable for this entity type"

**Category 5 -- Plan Design & Eligibility Rules (weight: 1.0 per field):**
- `plan_design.waiting_period_months` (required)
- `plan_design.minimum_age` (required)
- `plan_design.hours_threshold` (required)
- `section_125.available_to_all_equally` (required)
- `section_125.same_benefits_all` (required)
- Scored entirely from user answers on Pages 2-3 and gap-fill questions
- No upload can improve this score

**Category 6 -- Hours & Service Data (weight: 1.0 per field):**
- `employment.hours_worked_annual` (required for Section 125 part-time exclusion: < 1,000 hours)
- `employment.hours_worked_weekly_avg` (required for Section 105(h) part-time exclusion: < 35 hours/week)
- `employment.months_of_service_total` (required for service-based exclusions)
- Score = (employees with hours data / total employees) * 100
- Note: `months_of_service_total` can be derived from `hire_date` if available, so only hours are the true gap

**Category 7 -- Controlled Group / Related Entities (weight: 1.0 per field):**
- Only scored if user answered "Yes" to controlled group question on Page 3
- Entity names, employee counts per entity, participating status
- Score = 100% if user answered "No" to controlled group (not applicable)
- Score = 0% if user answered "Yes" but provided no entity details

#### Visual Display

The scorecard renders as a card-based layout inside the wizard page. Each category is a horizontal row:

```html
<div class="scorecard-category" data-status="${category.status}">
    <div class="category-header">
        <span class="category-icon">${statusIcon}</span>
        <span class="category-name">${category.categoryName}</span>
        <span class="category-coverage">${category.employeesCovered} of ${category.employeesTotal} employees</span>
    </div>
    <div class="progress-bar-container">
        <div class="progress-bar ${statusClass}" style="width: ${category.completionPercent}%"></div>
        <span class="progress-label">${category.completionPercent}%</span>
    </div>
    <div class="category-gaps" th:if="${!category.missingFields.empty}">
        <span class="gap-label">Still needed:</span>
        <span class="gap-detail">${gapDescription}</span>
    </div>
</div>
```

**Status thresholds:**
- Green (complete): 90-100% -- enough data to run tests reliably
- Yellow (partial): 50-89% -- tests can run but results may be incomplete
- Red (missing): 0-49% -- critical data missing, tests cannot produce reliable results

**Overall readiness indicator:**
- "Ready to test" -- all categories green or yellow, no red categories in test-blocking fields
- "Almost there" -- one or two yellow categories, no red
- "More data needed" -- any red category that blocks a test the plan requires

---

### 12b. Smart Upload Suggestions

After each gap analysis cycle, the system selects the single most impactful next upload and presents it prominently. The suggestion engine ranks potential uploads by a composite score.

#### Priority Scoring Formula

```
suggestion_score = (gaps_filled * gap_criticality_weight) / effort_estimate
```

Where:
- `gaps_filled` = number of scorecard fields this document type would populate
- `gap_criticality_weight` = sum of criticality weights for each gap (test-blocking gaps = 3.0, test-quality gaps = 2.0, nice-to-have gaps = 1.0)
- `effort_estimate` = estimated difficulty for the test taker to locate this document (1 = easy, 2 = moderate, 3 = hard)

#### Suggestion Data Model

```java
public class UploadSuggestion {
    String documentType;           // internal key, e.g., "employee_census"
    String plainEnglishName;       // "Your employee roster or HR export"
    String description;            // what the document contains
    String impact;                 // "This would complete roster data for all 47 employees"
    String whereToFind;            // "Usually exported from your HR system (ADP, Paychex, Gusto)"
    String exampleFormats;         // "Excel spreadsheet or CSV file"
    List<String> categoriesFilled; // which scorecard categories this improves
    double suggestionScore;        // computed priority score
    boolean alreadyProvided;       // true if a document of this type was already uploaded
}
```

#### Full Suggestion Catalog

**1. Employee Census / HR Export**

| Field | Value |
|-------|-------|
| Internal key | `employee_census` |
| Plain-English name | "Your employee roster -- the list of everyone who works for you" |
| Description | "A spreadsheet from your HR or payroll system listing all employees with their names, hire dates, job titles, and employment status. Some versions include compensation and hours too." |
| What it fills | Roster (names, dates, status), possibly compensation and hours if columns are present |
| Categories improved | Employee Roster (primary), Compensation Data (if comp column present), Hours & Service Data (if hours column present) |
| Impact template | "This would establish the roster for all employees and is the foundation for every test" |
| Where to find | "Export from your HR system -- ADP (Reports > Custom Reports > Employee Census), Paychex (Reports > Employee > Employee Listing), Gusto (People > Reports > Employee Details), or ask your HR administrator for a current employee listing" |
| Example formats | "Excel (.xlsx) or CSV file. Any format works -- we use AI to map your columns automatically" |
| Effort estimate | 1 (easy -- standard HR export) |
| When to suggest | Always first if no census has been uploaded. Highest priority document. |

**2. Insurance Carrier List Bill / Enrollment Report**

| Field | Value |
|-------|-------|
| Internal key | `carrier_list_bill` |
| Plain-English name | "Your health insurance company's monthly billing statement" |
| Description | "The monthly invoice from your insurance carrier that lists every enrolled employee by name, shows their coverage tier (single, family, etc.), and breaks down the premium between what the company pays and what the employee pays." |
| What it fills | Benefits enrollment status, plan tier, premium split (employer vs. employee) |
| Categories improved | Benefits Enrollment (primary) |
| Impact template | "This would complete benefits enrollment data for all ${totalEmployees} employees, showing who has coverage and how much each person pays" |
| Where to find | "Ask your insurance broker or contact your carrier directly. It is the monthly statement that shows every enrolled employee -- sometimes called a 'list bill,' 'enrollment invoice,' or 'group billing statement.' It usually arrives as a PDF or Excel file each month." |
| Example formats | "PDF (most common), Excel, or CSV. The AI parser handles all three." |
| Effort estimate | 2 (moderate -- may need to ask broker) |
| When to suggest | After census is uploaded but Benefits Enrollment category is below 50%. Only suggest if plan offers health/dental/vision benefits. |

**3. Payroll YTD Report / W-2 Summary**

| Field | Value |
|-------|-------|
| Internal key | `payroll_ytd` |
| Plain-English name | "A year-end payroll summary showing each employee's total earnings and deductions" |
| Description | "A report from your payroll provider that shows each employee's gross compensation, pre-tax deductions broken down by type (medical, dental, FSA, HSA, 401k), and employer contributions. This is the most accurate source for compensation data." |
| What it fills | Exact compensation (current year and possibly prior year), pre-tax deduction amounts by type, employer contribution amounts |
| Categories improved | Compensation Data (primary), Hours & Service Data (if hours column present) |
| Impact template | "This would provide exact compensation and deduction data for all ${totalEmployees} employees, replacing any estimates with actual payroll figures" |
| Where to find | "From your payroll provider: ADP (Reports > Tax Reports > Year-End Summary), Paychex (Reports > Tax > Annual Earnings Summary), Gusto (Reports > Payroll > Year-End Report). For the current plan year, use the most recent YTD report. A W-2 summary report works too if the plan year matches the calendar year." |
| Example formats | "Excel, CSV, or PDF. Excel/CSV is preferred for faster parsing." |
| Effort estimate | 1 (easy -- standard payroll report) |
| When to suggest | After census is uploaded but Compensation Data category is below 75%, OR when compensation data exists but is missing prior-year figures or deduction breakdowns. |

**4. FSA/HSA Election Report**

| Field | Value |
|-------|-------|
| Internal key | `fsa_hsa_elections` |
| Plain-English name | "The annual enrollment report from your FSA or HSA administrator" |
| Description | "A report showing which employees elected a Health FSA, Dependent Care FSA (DCFSA), Limited Purpose FSA, or HSA contribution, and how much each person elected for the plan year." |
| What it fills | FSA/HSA election amounts by type per employee, participant status for FSA/HSA benefits |
| Categories improved | Benefits Enrollment (for FSA/HSA-specific data) |
| Impact template | "This would show which employees elected FSA/HSA benefits and their election amounts -- needed for the Section 125 contributions test and Section 129 DCFSA tests" |
| Where to find | "From your FSA/HSA administrator: WageWorks, HealthEquity, Alegeus, DataPath, or your TPA. Look for an 'enrollment report' or 'election summary' for the current plan year. Your benefits broker may also have this." |
| Example formats | "Excel or CSV, sometimes PDF" |
| Effort estimate | 2 (moderate -- separate system from payroll) |
| When to suggest | Only when plan offers Health FSA, DCFSA, or HSA (checked on Page 2) AND election amounts are missing from census and payroll data. Do not suggest if FSA/HSA is not part of the plan. |

**5. Ownership Declaration / K-1 Schedule**

| Field | Value |
|-------|-------|
| Internal key | `ownership_declaration` |
| Plain-English name | "A list of business owners with their ownership percentages" |
| Description | "A document identifying who owns the business and what percentage each person owns. This is critical because owners above certain thresholds (5% for Section 125, 10% for Section 105(h)) are automatically classified as highly compensated. Family members of owners are also affected." |
| What it fills | Ownership percentages, family relationships between owners and employees |
| Categories improved | Ownership & Officers (primary) |
| Impact template | "This would identify all owners and their percentages, which determines HCI classification for ${hciCount} potential highly compensated individuals" |
| Where to find | "Your CPA or tax preparer has this. For partnerships and S-Corps, Schedule K-1 forms from your tax return list each owner's percentage. For LLCs, the Operating Agreement has ownership splits. For C-Corps, the stock ledger or shareholder agreement. If you are the sole owner, just tell us -- no upload needed." |
| Example formats | "PDF (K-1 forms), Excel, or just a simple list in any format" |
| Effort estimate | 2 (moderate -- may need to ask CPA) |
| When to suggest | When entity type is NOT Government or Tax-Exempt, AND ownership data has not been provided, AND the plan requires HCI/HCE classification (which is always, unless Simple Cafeteria Plan safe harbor applies). |

**6. Officer List**

| Field | Value |
|-------|-------|
| Internal key | `officer_list` |
| Plain-English name | "A list of your company's officers -- president, VP, secretary, treasurer" |
| Description | "A list identifying which employees hold officer positions. For Section 125, ALL officers are automatically classified as highly compensated. For Section 105(h), the top 5 highest-paid officers are classified as HCI." |
| What it fills | Officer identification, officer titles |
| Categories improved | Ownership & Officers |
| Impact template | "This would identify all officers, who are automatically classified as highly compensated for Section 125 testing" |
| Where to find | "From your corporate minutes, bylaws, or articles of incorporation. Your attorney or corporate secretary maintains this. For small businesses, the owner often knows this off the top of their head." |
| Example formats | "Any format -- PDF, Word document, or just a list" |
| Effort estimate | 1 (easy for small companies, moderate for larger ones) |
| When to suggest | When officer data is missing AND the plan requires Section 125 or Section 105(h) testing. Often combined with the ownership suggestion into a single prompt: "Who are the owners and officers?" |

**7. Controlled Group Entity List**

| Field | Value |
|-------|-------|
| Internal key | `controlled_group_entities` |
| Plain-English name | "A list of all related companies in your controlled group" |
| Description | "If your business is part of a group of related companies (parent-subsidiary, brother-sister, or affiliated service group), we need to know which entities exist, how many employees each has, and which ones participate in the cafeteria plan." |
| What it fills | Related entity names, employee counts per entity, plan participation status |
| Categories improved | Controlled Group / Related Entities |
| Impact template | "This would complete the controlled group analysis, which affects how employees are counted for all nondiscrimination tests" |
| Where to find | "Your CPA or tax attorney should have this -- it is typically documented in your tax returns or corporate structure documents. Look for any entity that shares common ownership of 80% or more." |
| Example formats | "Any format -- a simple list with entity names, EINs, and employee counts is sufficient" |
| Effort estimate | 3 (hard -- may require CPA consultation) |
| When to suggest | Only when user answered "Yes" to controlled group question on Page 3 AND entity details have not been provided. |

**8. Union / CBA Documentation**

| Field | Value |
|-------|-------|
| Internal key | `cba_documentation` |
| Plain-English name | "Documentation about your union employees and their benefits" |
| Description | "If some employees are covered by a collective bargaining agreement (CBA), we need to know which employees are covered and whether cafeteria plan benefits were part of the bargaining. CBA employees whose benefits were bargained in good faith are excluded from nondiscrimination testing." |
| What it fills | Which employees are CBA-covered, whether benefits were bargained |
| Categories improved | Employee Roster (CBA flags) |
| Impact template | "This would identify which of your ${cbaCount} union employees can be excluded from testing, potentially simplifying the analysis" |
| Where to find | "Your HR department or labor relations team has the collective bargaining agreement. The key section is the one covering employee benefits -- specifically whether health insurance and FSA benefits are included." |
| Example formats | "PDF of the CBA, or a simple list of CBA-covered employees" |
| Effort estimate | 2 (moderate -- HR department has this) |
| When to suggest | Only when user answered "Yes" to CBA question on Page 3 AND the details about which employees are covered or whether benefits were bargained are still missing. |

---

### 12c. The Upload Loop UX

The upload experience is a state machine with four states. The wizard's Page 4 (Document Upload Zone, defined in Section 4) evolves dynamically based on what data has been collected.

#### State Machine

```
STATE 1: NO UPLOADS YET
  Trigger: User arrives at Page 4 with no documents uploaded and no census data.

  Display:
  ┌─────────────────────────────────────────────────────────────┐
  │  Upload Your Data                                           │
  │                                                             │
  │  To get started, upload your employee roster or HR report.  │
  │  This is usually the most helpful first step.               │
  │                                                             │
  │  ┌─────────────────────────────────────────────────────┐    │
  │  │  [drag-and-drop zone]                               │    │
  │  │  Employee Census / HR Export                         │    │
  │  │  "Your employee roster with demographics and dates"  │    │
  │  │  Accepts: CSV, XLSX, PDF                            │    │
  │  │  ★ MOST HELPFUL FIRST STEP                          │    │
  │  └─────────────────────────────────────────────────────┘    │
  │                                                             │
  │  Other documents you can upload:                            │
  │  • Payroll YTD Report                                       │
  │  • Insurance Billing Statement                              │
  │  • Ownership & Officer List                                 │
  │  • Benefits Enrollment Report                               │
  │  • FSA/HSA Election Report                                  │
  │  (expand each for description and "where to find" tips)     │
  │                                                             │
  │  ─────────────────────────────────────────────────────────  │
  │  Don't have any documents? No problem.                      │
  │  [Continue with questions only →]                           │
  └─────────────────────────────────────────────────────────────┘

  Behavior:
  - The census upload zone is prominent and expanded by default.
  - Other document types are listed as collapsible accordion items.
  - "Continue with questions only" link skips to gap-fill questions (Page 6+).


STATE 2: AFTER FIRST UPLOAD PARSED
  Trigger: First document has been uploaded and AI parsing is complete.

  Display:
  ┌─────────────────────────────────────────────────────────────┐
  │  Great! We found 47 employees and extracted hire dates,     │
  │  employment status, and compensation data.                  │
  │                                                             │
  │  ── Data Completeness Scorecard ──────────────────────────  │
  │  Employee Roster      ████████████████████░░  89%  47/47    │
  │  Compensation Data    ████████████████░░░░░░  72%  34/47    │
  │  Benefits Enrollment  ░░░░░░░░░░░░░░░░░░░░░░   0%   0/47   │
  │  Ownership & Officers ░░░░░░░░░░░░░░░░░░░░░░   0%   0/47   │
  │  Plan Design          ████████████████████░░  85%  (answers)│
  │  Hours & Service      ████████████░░░░░░░░░░  55%  26/47   │
  │  Controlled Group     ████████████████████████ 100% (N/A)   │
  │                                                             │
  │  ── What Would Help Most Next ────────────────────────────  │
  │  ┌─────────────────────────────────────────────────────┐    │
  │  │  📋 Your health insurance company's monthly          │
  │  │     billing statement                                │    │
  │  │                                                      │    │
  │  │  This shows us who has health coverage and how much  │    │
  │  │  each person pays. It would complete benefits        │    │
  │  │  enrollment data for all 47 employees.               │    │
  │  │                                                      │    │
  │  │  Where to find it: Ask your insurance broker or      │    │
  │  │  carrier. It's the monthly statement listing every   │    │
  │  │  enrolled employee.                                  │    │
  │  │                                                      │    │
  │  │  [Upload this document]                              │    │
  │  └─────────────────────────────────────────────────────┘    │
  │                                                             │
  │  [Upload a different document ▼]                            │
  │  [That's all I have — continue with questions →]            │
  └─────────────────────────────────────────────────────────────┘

  Behavior:
  - Scorecard shows live completion percentages with color coding.
  - The single best next-upload suggestion is displayed prominently.
  - "Upload a different document" expands the full document type list.
  - "That's all I have" transitions to gap-fill questions.


STATE 3: AFTER SECOND+ UPLOAD PARSED
  Trigger: Second or subsequent document parsed.

  Display:
  ┌─────────────────────────────────────────────────────────────┐
  │  ✓ Updated! Benefits enrollment went from 0% → 89%.        │
  │    We now know coverage status for 42 of 47 employees.     │
  │                                                             │
  │  ── Data Completeness Scorecard ──────────────────────────  │
  │  Employee Roster      ████████████████████░░  89%  47/47    │
  │  Compensation Data    ████████████████░░░░░░  72%  34/47    │
  │  Benefits Enrollment  ██████████████████░░░░  89%  42/47    │
  │  Ownership & Officers ░░░░░░░░░░░░░░░░░░░░░░   0%   0/47   │
  │  Plan Design          ████████████████████░░  85%  (answers)│
  │  Hours & Service      ████████████░░░░░░░░░░  55%  26/47   │
  │  Controlled Group     ████████████████████████ 100% (N/A)   │
  │                                                             │
  │  Looking good! Just a few gaps remaining.                   │
  │                                                             │
  │  ── Next Suggestion ──────────────────────────────────────  │
  │  Your company's ownership list (owners and percentages).    │
  │  This determines who is classified as "highly compensated"  │
  │  for testing purposes.                                      │
  │                                                             │
  │  [Upload this document]                                     │
  │  [Upload a different document ▼]                            │
  │  [That's all I have — continue with questions →]            │
  └─────────────────────────────────────────────────────────────┘

  Behavior:
  - Shows delta since last upload ("went from X% to Y%") in a success banner.
  - Updated scorecard with new percentages.
  - Next suggestion recalculated based on remaining gaps.
  - If all categories are green: "All data categories are well covered! You can proceed to testing or upload additional documents for even better accuracy."


STATE 4: USER SAYS "THAT'S ALL I HAVE"
  Trigger: User clicks "That's all I have -- continue with questions."

  Display:
  ┌─────────────────────────────────────────────────────────────┐
  │  Based on what you provided, we just need a few more        │
  │  pieces of information.                                     │
  │                                                             │
  │  Remaining questions: 12                                    │
  │  (Without your uploads, this would have been 47 questions)  │
  │                                                             │
  │  [Continue to questions →]                                  │
  └─────────────────────────────────────────────────────────────┘

  Behavior:
  - Transitions to gap-fill questions (generated dynamically based on what's still missing).
  - Shows how many questions were eliminated by uploads (motivational feedback).
  - Gap-fill questions are organized by category, highest-criticality gaps first.
  - Questions that can be suppressed by the "We Already Know" intelligence (Section 12e) are removed.
```

#### State Transition Logic

```java
public UploadLoopState determineState(NdtTestRun testRun) {
    CensusData census = testRun.getCensusData();
    List<UploadedDocument> uploads = testRun.getUploadedDocuments();

    if (uploads.isEmpty() && (census == null || census.getEmployees().isEmpty())) {
        return UploadLoopState.NO_UPLOADS;
    }

    // After any upload, check if user has clicked "that's all I have"
    if (testRun.getUploadPhaseComplete()) {
        return UploadLoopState.DONE_UPLOADING;
    }

    if (uploads.size() == 1) {
        return UploadLoopState.AFTER_FIRST_UPLOAD;
    }

    return UploadLoopState.AFTER_SUBSEQUENT_UPLOAD;
}
```

---

### 12d. Gap Analysis Engine

After each upload, the system executes a six-step gap analysis pipeline. This pipeline runs server-side and feeds the scorecard and suggestion engine.

#### Pipeline Steps

```
Step 1: Parse uploaded document via AI (Section 3 pipeline)
    ↓
Step 2: Merge new data into existing census (match by name/ID)
    ↓
Step 3: Re-run auto-tagging with updated data (Section 5 logic)
    ↓
Step 4: Identify remaining gaps by category
    ↓
Step 5: Score each gap by criticality
    ↓
Step 6: Generate updated scorecard + select next upload suggestion
```

#### Step 2: Merge Logic

When a new document is parsed, its data must be merged into the existing census array. The merge process:

1. **Match employees** between the new document and existing census:
   - Primary match: exact name match (case-insensitive, trimmed)
   - Secondary match: employee ID / SSN-last-4 match
   - Fuzzy match: Levenshtein distance <= 2 on last name + first initial match
   - Unmatched rows from the new document become new employee records if they appear to be real employees (not totals rows, not headers)

2. **Merge fields** for matched employees:
   - If the existing field is null/empty, populate it from the new source
   - If the existing field has a value and the new source has a different value, flag as a merge conflict (handled in Section 12f)
   - Track source attribution: `{ "field": "annual_compensation_current", "value": 85000, "source": "payroll_ytd", "uploaded_at": "2026-03-23T14:30:00" }`

3. **Re-index the census** after merge:
   - Update `employee_count` in the test run
   - Recalculate all derived fields (age at plan year end, months of service)
   - Clear and re-run auto-tagging (Step 3)

```java
public class CensusMergeResult {
    int employeesMatched;         // existing employees updated with new data
    int employeesAdded;           // new employees found in this document
    int fieldsPopulated;          // previously-empty fields now filled
    int conflictsDetected;        // fields with conflicting values from different sources
    List<MergeConflict> conflicts;
    Map<String, String> fieldSourceMap;  // which source provided each field
}
```

#### Step 4: Gap Identification

For each applicable test (determined by Page 2 answers), identify the minimum required data and what is missing.

**Test-to-data dependency map:**

| Test | Required Data (test cannot run without this) | Quality Data (improves accuracy) |
|------|---------------------------------------------|----------------------------------|
| Section 125 Eligibility | Roster, HCI classification, eligibility status | Plan design confirmation |
| Section 125 Contributions | Roster, HCI classification, enrollment status, salary reduction amounts | Employer contribution amounts, benefit-by-benefit breakdown |
| Section 125 Key Employee | Roster, Key Employee classification, benefit amounts | Benefit-by-benefit breakdown |
| Section 105(h) Eligibility | Roster, HCI classification (105h-specific), eligibility status | Age, service years for statutory exclusions |
| Section 105(h) Benefits | Roster, HCI classification, employer-paid benefit amounts | Plan tier, specific benefit values |
| Section 129 Eligibility | Roster, HCE classification (129-specific), DCFSA eligibility | Age, service for exclusions |
| Section 129 Contributions | Roster, HCE classification, DCFSA election amounts | Employer DCFSA contributions |
| Section 129 55% Avg Benefits | Roster, all employee DCFSA amounts including zero-election | Compensation for $25K exclusion |
| Section 129 >5% Owners | Roster, ownership percentages, DCFSA amounts for owners | Family attribution data |

**Gap criticality levels:**

```java
public enum GapCriticality {
    BLOCKING,    // Test cannot run at all without this data (weight: 3.0)
    DEGRADED,    // Test can run but results are unreliable (weight: 2.0)
    NICE_TO_HAVE // Test runs fine, but additional data would improve confidence (weight: 1.0)
}
```

**Criticality assignment rules:**

- BLOCKING: Roster missing entirely, OR HCI classification impossible (no comp + no ownership data), OR enrollment status unknown for > 50% of employees when running a benefits test
- DEGRADED: Comp data available for < 80% of employees, OR hours data missing when part-time exclusion would apply, OR prior-year comp missing (forced to use current-year as proxy)
- NICE_TO_HAVE: Deduction breakdown available as total but not by type, OR exact premium split unknown but total premium known

#### Step 5: Gap Scoring

Each gap receives a composite criticality score used to prioritize both gap-fill questions and upload suggestions.

```java
public class GapScore {
    String fieldPath;              // e.g., "compensation.annual_compensation_current"
    String categoryName;           // e.g., "Compensation Data"
    GapCriticality criticality;    // BLOCKING, DEGRADED, NICE_TO_HAVE
    int employeesAffected;         // how many employees are missing this field
    double percentAffected;        // employeesAffected / totalEmployees
    List<String> testsImpacted;    // which tests need this data
    String documentThatWouldFill;  // suggestion catalog key, e.g., "payroll_ytd"
}
```

#### Handling Partial Data

The engine must handle partial data gracefully:

- **Compensation known for 40 of 47 employees:** Test CAN run. The 7 employees with unknown comp are flagged in the test output. If any of the 7 turn out to be HCIs, the test result could change. The system reports: "Test result is PASS, but 7 employees have unknown compensation. If any of these employees earn above $160,000, they would be classified as HCI and the result could change."
- **Benefits enrollment known for 30 of 47:** Test CAN run with a warning. Unknown employees are assumed non-enrolled (conservative assumption for eligibility test, liberal assumption for contributions test). The system flags: "15 employees have unknown enrollment status. Results assume they are not enrolled."
- **Ownership data completely missing:** HCI classification falls back to compensation-only. Officers cannot be identified. The system warns: "Ownership data is missing. HCI classification is based on compensation only. If any employee owns more than 5% of the business, the test results could change."

**"Complete enough" threshold per test:**
- A test is "runnable" when at least 80% of employees have the BLOCKING-level data fields populated.
- A test is "reliable" when at least 95% of employees have both BLOCKING and DEGRADED-level fields populated.
- Below 80%, the test is marked "INSUFFICIENT DATA" and cannot produce a result.

---

### 12e. "We Already Know" Intelligence

The system applies deduction rules to suppress unnecessary upload suggestions and gap-fill questions when the answer can be inferred from data already collected. This reduces the burden on the test taker and makes the system feel intelligent.

#### Deduction Rules

Each rule is evaluated after every data update (upload parse or question answer). Rules are implemented as stateless functions that examine the current census and plan data.

```java
public interface DeductionRule {
    String ruleId();
    String description();
    boolean applies(NdtTestRun testRun);
    List<String> suppressedQuestions();    // question keys to skip
    List<String> suppressedSuggestions(); // upload suggestion keys to skip
}
```

**Rule catalog:**

**Rule D-01: No Excludable Employees by Service**
- Condition: ALL employees have `hire_date` more than 36 months before plan year end (for 105h) or more than `waiting_period_months` before plan year end (for 125)
- Effect: Suppress questions about waiting period exclusion details, suppress "How long is your plan's waiting period?" if not already answered (the answer does not matter because no one would be excluded regardless)
- Rationale: If everyone has been there long enough, the waiting period exclusion produces an empty set

**Rule D-02: All Employees Above HCE Threshold**
- Condition: ALL employees have `annual_compensation_current` OR `annual_compensation_prior_year` above the HCE threshold ($160,000 for 2026)
- Effect: Suppress detailed compensation classification questions -- everyone is HCI by compensation
- Rationale: The HCI/non-HCI split is trivially "all HCI," and the eligibility test is automatically satisfied (0 non-HCIs to discriminate against)

**Rule D-03: Very Small Employer (5 or Fewer Employees)**
- Condition: Total census employee count <= 5
- Effect: Suppress questions about statistical classifications, suppress hours-tracking upload suggestion (easier to ask directly for 5 people), simplify ownership questions to "Which of these 5 people are owners?"
- Rationale: Percentage-based tests with 5 employees have limited permutations; direct questioning is faster than document uploads

**Rule D-04: Government or Tax-Exempt Entity**
- Condition: `entity_type` is "Government" or "Tax-Exempt Organization"
- Effect: Suppress ALL ownership questions and ownership upload suggestions. Auto-set Ownership & Officers category to 100% with note "Not applicable." Suppress K-1 suggestion, ownership declaration suggestion. Still ask about officers (government entities have officers).
- Rationale: Government and tax-exempt entities have no private ownership; the 5%/10% ownership tests do not apply

**Rule D-05: No Controlled Group**
- Condition: User answered "No" to controlled group question on Page 3
- Effect: Suppress controlled group entity list upload suggestion. Auto-set Controlled Group category to 100%. Suppress all follow-up questions about related entities.
- Rationale: No controlled group means no aggregation needed

**Rule D-06: Simple Cafeteria Plan Safe Harbor**
- Condition: User answered "Yes" to all four Simple Cafeteria Plan questions on Page 3
- Effect: Suppress ALL upload suggestions and gap-fill questions. Skip directly to attestation. Display: "Your plan qualifies for the Simple Cafeteria Plan safe harbor. All nondiscrimination tests are deemed satisfied."
- Rationale: Simple Cafeteria Plan exempts from all NDT testing

**Rule D-07: No CBA Employees**
- Condition: User answered "No" to CBA question on Page 3
- Effect: Suppress CBA documentation upload suggestion. Suppress all CBA-related gap-fill questions. Auto-set all employees' `cba.is_cba_member` to false.
- Rationale: No CBA employees means no CBA exclusion analysis needed

**Rule D-08: Sole Proprietor**
- Condition: `entity_type` is "Sole Proprietorship" AND employee count is small (< 20)
- Effect: Suppress ownership upload suggestion. Auto-prompt: "As a sole proprietorship, you are the 100% owner. Please confirm: [Your name] is the sole owner. (Yes/No)" If confirmed, auto-populate ownership at 100% for that person.
- Rationale: Sole proprietorships have exactly one owner by definition

**Rule D-09: FSA/HSA Not Offered**
- Condition: User did NOT check Health FSA, DCFSA, or HSA on Page 2
- Effect: Suppress FSA/HSA election report upload suggestion. Suppress Section 129 tests entirely. Suppress FSA/HSA-related gap-fill questions.
- Rationale: If these benefits are not offered, related data is irrelevant

**Rule D-10: Hours Derivable from Employment Status**
- Condition: ALL employees have `employment_status` = "Full-Time" AND employer confirms all full-time employees work 40+ hours/week
- Effect: Suppress hours tracking upload suggestion. Auto-populate `hours_worked_annual` as 2080 and `hours_worked_weekly_avg` as 40.0 for all full-time employees. Suppress questions about individual employee hours.
- Rationale: If everyone is full-time at 40+ hours, no one can be excluded as part-time

**Rule D-11: Prior-Year Comp Not Needed**
- Condition: Census shows the plan is in its first year of existence (all hire dates are within the current plan year, or user confirms first plan year)
- Effect: Suppress prior-year compensation questions and payroll suggestions that specifically target prior-year data. Use current-year compensation for the 414(q) HCE threshold test.
- Rationale: First-year plans use current-year compensation; there is no prior year

**Rule D-12: Single Health Plan Option**
- Condition: User confirms only one health plan option is offered (no choice of plans)
- Effect: Suppress questions about multiple plan tiers and plan-by-plan benefits testing. Simplify the Section 105(h) benefits test to a single-plan analysis.
- Rationale: With one plan, the "same benefits available to all" test is simpler

#### Deduction Rule Execution

Rules are evaluated in order D-01 through D-12 after every data change. The results are cached in the test run's JSON metadata:

```json
{
  "deduction_rules_applied": [
    { "rule_id": "D-04", "applied_at": "2026-03-23T14:35:00", "effect": "Suppressed ownership questions and uploads" },
    { "rule_id": "D-07", "applied_at": "2026-03-23T14:35:00", "effect": "Suppressed CBA documentation" }
  ],
  "suppressed_questions": ["ownership_percentage", "k1_upload", "cba_details", "cba_benefits_bargained"],
  "suppressed_suggestions": ["ownership_declaration", "cba_documentation"]
}
```

---

### 12f. Merge Conflict Resolution

When overlapping data arrives from multiple uploads, the same employee field may have different values from different sources. The system must detect, store, and resolve these conflicts.

#### Conflict Detection

A merge conflict is created whenever:
- The same employee (matched by name or ID) has a value for the same field from two or more sources
- The values differ beyond a tolerance threshold

**Tolerance thresholds by field type:**
- Compensation fields: values differ by more than 2% OR more than $500 (whichever is smaller). Small rounding differences from different payroll periods are not conflicts.
- Date fields: values differ by more than 1 day.
- Boolean/enum fields: any difference is a conflict.
- String fields (names, titles): case-insensitive comparison after trimming. Abbreviation matching is NOT applied -- "VP" vs. "Vice President" IS a conflict for resolution.

#### Conflict Data Model

```java
public class MergeConflict {
    String conflictId;            // UUID
    String employeeId;            // which employee
    String employeeDisplayName;   // for UI display
    String fieldPath;             // e.g., "compensation.annual_compensation_current"
    String fieldDisplayName;      // "Annual Compensation (Current Year)"

    // Value A
    Object valueA;                // e.g., 85000
    String sourceA;               // e.g., "payroll_ytd"
    String sourceADisplayName;    // "Payroll YTD Report (uploaded Mar 23)"
    LocalDateTime sourceAUploadedAt;

    // Value B
    Object valueB;                // e.g., 82000
    String sourceB;               // e.g., "employee_census"
    String sourceBDisplayName;    // "Employee Census (uploaded Mar 23)"
    LocalDateTime sourceBUploadedAt;

    // Resolution
    String resolvedBy;            // "auto" or person ID of reviewer
    String resolvedValue;         // which value was chosen
    String resolvedSource;        // which source was used
    String resolutionReason;      // "Source priority: payroll > census for compensation"
    LocalDateTime resolvedAt;
    ConflictStatus status;        // PENDING, AUTO_RESOLVED, MANUALLY_RESOLVED
}

public enum ConflictStatus {
    PENDING,          // Awaiting resolution
    AUTO_RESOLVED,    // System applied source priority hierarchy
    MANUALLY_RESOLVED // Reviewer picked a value
}
```

#### Source Priority Hierarchy

The system auto-resolves conflicts using a source authority hierarchy. Each field type has a ranked list of authoritative sources.

| Field Category | Source Priority (most authoritative first) |
|---------------|-------------------------------------------|
| **Compensation** (annual comp, gross pay) | 1. `payroll_ytd` 2. `w2_summary` 3. `employee_census` 4. `carrier_list_bill` |
| **Pre-tax deductions** (FSA, HSA, medical premiums) | 1. `payroll_ytd` 2. `fsa_hsa_elections` 3. `carrier_list_bill` 4. `employee_census` |
| **Benefits enrollment** (enrolled Y/N, plan tier) | 1. `carrier_list_bill` 2. `fsa_hsa_elections` 3. `employee_census` 4. `payroll_ytd` |
| **Premium amounts** (employee/employer split) | 1. `carrier_list_bill` 2. `payroll_ytd` 3. `employee_census` |
| **Hire date / Termination date** | 1. `employee_census` 2. `payroll_ytd` 3. `carrier_list_bill` |
| **Hours worked** | 1. `payroll_ytd` 2. `employee_census` 3. `time_tracking_export` |
| **Ownership percentages** | 1. `ownership_declaration` 2. `k1_schedule` 3. `employee_census` |
| **Officer status** | 1. `officer_list` 2. `ownership_declaration` 3. `employee_census` |

When a conflict is auto-resolved:
1. The higher-priority source's value is used as the active value
2. The lower-priority source's value is stored as an alternative
3. The conflict status is set to `AUTO_RESOLVED`
4. The resolution reason is recorded: "Source priority: payroll_ytd ranks higher than employee_census for compensation fields"

#### Conflict Resolution UI (PSP Reviewer View)

Conflicts are surfaced only to the PSP reviewer (not the test taker). The reviewer sees a conflicts panel in their test run review dashboard.

```
┌─────────────────────────────────────────────────────────────────┐
│  Data Conflicts (3 auto-resolved, 1 needs review)              │
│                                                                 │
│  ── Needs Review ────────────────────────────────────────────── │
│  Employee: Jane Smith                                           │
│  Field: Annual Compensation                                     │
│  Payroll YTD Report says: $85,000                               │
│  Employee Census says: $82,000                                  │
│  Difference: $3,000 (3.5%)                                      │
│  [Use Payroll Value] [Use Census Value] [Enter Custom Value]    │
│                                                                 │
│  ── Auto-Resolved (click to expand) ─────────────────────────── │
│  ▸ John Doe — Hire Date: Census (2019-03-15) vs Payroll         │
│    (2019-03-14) → Used Census value (primary source for dates)  │
│  ▸ Bob Lee — Premium Amount: List Bill ($450) vs Census ($445)  │
│    → Used List Bill value (primary source for premiums)         │
│  ▸ Amy Chen — Hours: Payroll (2,080) vs Census (2,040)          │
│    → Used Payroll value (primary source for hours)              │
└─────────────────────────────────────────────────────────────────┘
```

**UI behaviors:**
- Auto-resolved conflicts are collapsed by default (reviewer can expand to audit)
- Pending conflicts are expanded and highlighted with a yellow warning bar
- Each pending conflict shows both values side-by-side with the source name and upload timestamp
- Three resolution buttons: use value A, use value B, or enter a custom value
- After all conflicts are resolved, the scorecard is recalculated and tests can proceed
- Conflicts do NOT block test execution -- the system uses auto-resolved values. But pending conflicts generate a warning on the test results: "1 data conflict is unresolved and may affect results."

#### Conflict Storage in JSON

Conflicts are stored in the test run's `census_metadata` JSON field:

```json
{
  "merge_conflicts": [
    {
      "conflict_id": "uuid-1",
      "employee_id": "EMP-042",
      "field_path": "compensation.annual_compensation_current",
      "value_a": 85000,
      "source_a": "payroll_ytd",
      "value_b": 82000,
      "source_b": "employee_census",
      "status": "AUTO_RESOLVED",
      "resolved_value": 85000,
      "resolved_source": "payroll_ytd",
      "resolution_reason": "Source priority: payroll ranks above census for compensation",
      "resolved_at": "2026-03-23T14:40:00"
    }
  ],
  "source_tracking": {
    "EMP-042": {
      "compensation.annual_compensation_current": {
        "active_value": 85000,
        "active_source": "payroll_ytd",
        "alternative_values": [
          { "value": 82000, "source": "employee_census" }
        ]
      }
    }
  }
}
```

---

## 13. PSP Pre-Seeding & Prior Year Rollover

The PSP (testing firm) often has access to employer data before the test taker starts -- enrollment files from carriers they manage, payroll feeds, prior year test data. The system must support pre-loading this data so the test taker sees a head start when they open the wizard.

### 13a. PSP Pre-Seeding Flow

Before the test taker receives the questionnaire link, the PSP reviewer can:

1. Create the test run (linked to an NDT activity)
2. Upload documents they already have (carrier enrollment files, payroll exports, prior year data)
3. System parses and populates the census
4. PSP reviewer can also manually enter known structural data (entity type, controlled group status, benefits offered, plan eligibility rules)
5. PSP reviewer can answer plan design questions on behalf of the employer (since PSPs often know the plan document better than the employer does)
6. When ready, PSP sends the questionnaire link to the test taker

#### Test Run Lifecycle

The test run progresses through a defined set of statuses:

```
DRAFT  -->  SENT  -->  IN_PROGRESS  -->  REVIEW  -->  TESTING  -->  COMPLETE
```

| Status | Description | Who is active | Transitions to |
|--------|-------------|---------------|----------------|
| `DRAFT` | PSP reviewer is pre-seeding data. Test taker has no access yet. | PSP reviewer only | `SENT` (when PSP clicks "Send to Test Taker") |
| `SENT` | Link has been delivered to the test taker but they haven't opened it yet. | Neither (waiting) | `IN_PROGRESS` (when test taker opens the wizard) |
| `IN_PROGRESS` | Test taker is actively filling gaps, uploading documents, confirming data. | Test taker | `REVIEW` (when test taker clicks "Submit for Review") |
| `REVIEW` | PSP reviewer is validating completeness, resolving conflicts, checking data quality. | PSP reviewer | `TESTING` (when reviewer approves) or `IN_PROGRESS` (if sent back to test taker) |
| `TESTING` | NDT tests are running computationally against the finalized census. | System (automated) | `COMPLETE` (when all tests finish) |
| `COMPLETE` | Results are available. Test run is read-only. | Both (read-only) | None (terminal state) |

Implementation in `NdtTestRun` entity:

```java
@Column(name = "status")
@Enumerated(EnumType.STRING)
private TestRunStatus status = TestRunStatus.DRAFT;

public enum TestRunStatus {
    DRAFT,        // PSP pre-seeding
    SENT,         // Link delivered, awaiting test taker
    IN_PROGRESS,  // Test taker working
    REVIEW,       // PSP reviewing
    TESTING,      // Running NDT computations
    COMPLETE      // Results available
}
```

Status transition rules enforced in `NdtTestRunService`:

```java
private static final Map<TestRunStatus, Set<TestRunStatus>> ALLOWED_TRANSITIONS = Map.of(
    TestRunStatus.DRAFT,       Set.of(TestRunStatus.SENT),
    TestRunStatus.SENT,        Set.of(TestRunStatus.IN_PROGRESS),
    TestRunStatus.IN_PROGRESS, Set.of(TestRunStatus.REVIEW),
    TestRunStatus.REVIEW,      Set.of(TestRunStatus.TESTING, TestRunStatus.IN_PROGRESS),
    TestRunStatus.TESTING,     Set.of(TestRunStatus.COMPLETE),
    TestRunStatus.COMPLETE,    Set.of()  // terminal
);

public void transitionStatus(NdtTestRun run, TestRunStatus newStatus, Person actor) {
    TestRunStatus current = run.getStatus();
    if (!ALLOWED_TRANSITIONS.getOrDefault(current, Set.of()).contains(newStatus)) {
        throw new IllegalStateException(
            "Cannot transition from " + current + " to " + newStatus);
    }
    run.setStatus(newStatus);
    run.setStatusChangedAt(LocalDateTime.now());
    run.setStatusChangedBy(actor.getId());
    auditLog(run, current, newStatus, actor);
}
```

#### PSP Reviewer UI During DRAFT Mode

The reviewer dashboard (Section 11b) operates in DRAFT mode with these additions:

1. **Pre-Seed Toolbar** -- displayed at the top of the reviewer dashboard when status is `DRAFT`:
   - "Upload Documents" button (same pipeline as test taker uploads, but tagged as PSP source)
   - "Enter Data Manually" button (opens a structured form for plan design questions)
   - "Import Prior Year" button (triggers the rollover flow from Section 13d)
   - "Apply Template" button (loads a pre-fill template from Section 13c.6)
   - Effort estimator display (Section 13e) showing projected test taker burden

2. **Data Entry Panel** -- PSP can directly answer plan design questions:
   - Renders the same questionnaire fields the test taker would see
   - Answers are saved with `source: "psp_manual_entry"` in the census metadata
   - Fields the PSP fills are shown as "pre-filled" (not "confirmed") since the test taker hasn't attested yet

3. **Send to Test Taker Action** -- prominent button that:
   - Validates minimum viable pre-seed is met (entity type + benefits offered + plan year)
   - Generates or retrieves the test taker access link
   - Transitions status from `DRAFT` to `SENT`
   - Sends notification email to the test taker contact
   - Records the send event in the audit trail

```java
public void sendToTestTaker(NdtTestRun run, Person pspReviewer) {
    // Validate minimum pre-seed
    PreSeedCompleteness completeness = calculateCompleteness(run);
    if (!completeness.meetsMinimumViable()) {
        throw new ValidationException(
            "Minimum pre-seed not met. Required: entity type, benefits offered, plan year. " +
            "Missing: " + completeness.getMissingMinimumFields());
    }

    // Generate access token if not already created
    if (run.getTestTakerAccessToken() == null) {
        run.setTestTakerAccessToken(UUID.randomUUID().toString());
    }

    // Transition status
    transitionStatus(run, TestRunStatus.SENT, pspReviewer);

    // Send email notification
    String link = buildTestTakerLink(run);
    emailService.sendTestTakerInvitation(
        run.getTestTakerContact(),
        run.getEmployer().getName(),
        pspReviewer.getPsp().getName(),
        link,
        completeness.getEstimatedTestTakerMinutes()
    );

    auditLog(run, "SENT_TO_TEST_TAKER", pspReviewer,
        Map.of("completeness_at_send", completeness.getPercentage()));
}
```

#### Source Tagging for Pre-Seeded Data

Every data point in the census tracks its origin. Pre-seeded data uses distinct source identifiers:

```json
{
  "employee_id": "EMP-001",
  "compensation": {
    "annual_compensation_current": {
      "value": 95000.00,
      "source": "psp_upload",
      "source_detail": "carrier_enrollment_2026.xlsx",
      "entered_by": "psp_reviewer",
      "entered_by_person_id": 4521,
      "entered_at": "2026-03-20T10:15:00",
      "confirmed_by_test_taker": false,
      "confirmed_at": null
    }
  }
}
```

Source values and their meanings:

| Source Value | Description |
|---|---|
| `psp_upload` | Document uploaded by PSP reviewer, parsed by system |
| `psp_manual_entry` | PSP reviewer typed the value directly into a form field |
| `prior_year_rollover` | Copied from a previous year's test run |
| `template_prefill` | Applied from a reusable PSP template |
| `carrier_feed` | Automated carrier integration (future) |
| `payroll_feed` | Automated payroll integration (future) |
| `test_taker_upload` | Document uploaded by the test taker |
| `test_taker_manual` | Test taker typed the value directly |
| `test_taker_confirmed` | Test taker confirmed a pre-seeded value without changing it |

The `confirmed_by_test_taker` flag is critical: it tracks whether the test taker has attested to a pre-seeded value. During the REVIEW phase, the PSP reviewer can see which pre-seeded values the test taker confirmed, changed, or ignored.

### 13b. Head Start Acknowledgment (Test Taker Experience)

When the test taker opens the wizard after PSP pre-seeding, they should see a modified experience that acknowledges the work already done and focuses their attention on what remains.

#### Modified Page 1: Head Start Welcome

Instead of the standard Page 1 from Section 10, the wizard detects pre-seeded data and renders a head start variant:

```
+-----------------------------------------------------------------------+
|  NDT Data Collection Wizard                                           |
|                                                                       |
|  Welcome, [Test Taker Name]!                                          |
|                                                                       |
|  +---------------------------------------------------------------+   |
|  |  HEAD START                                                    |   |
|  |                                                                |   |
|  |  Your testing advisor at [PSP Name] has already provided       |   |
|  |  some of your employee data. Here's where things stand:        |   |
|  |                                                                |   |
|  |  +-------------------+  +-------------------+                  |   |
|  |  | ALREADY PROVIDED  |  | STILL NEEDED      |                 |   |
|  |  |                   |  |                    |                 |   |
|  |  | [=======   ] 58%  |  | [===       ] 42%  |                 |   |
|  |  |                   |  |                    |                 |   |
|  |  | * Entity type     |  | * Employee hours   |                |   |
|  |  | * Benefits list   |  | * Ownership info   |                |   |
|  |  | * 142 of 200      |  | * 58 employees     |                |   |
|  |  |   employees       |  |   missing data     |                |   |
|  |  | * Plan eligibility |  | * Compensation     |                |   |
|  |  |   rules           |  |   verification     |                |   |
|  |  +-------------------+  +-------------------+                  |   |
|  +---------------------------------------------------------------+   |
|                                                                       |
|  Estimated time to complete: ~15 minutes                              |
|                                                                       |
|  +---------------------+    +---------------------------+             |
|  | Review Pre-Loaded   |    | Start Filling Gaps  -->   |             |
|  | Data (optional)     |    | (recommended)             |             |
|  +---------------------+    +---------------------------+             |
+-----------------------------------------------------------------------+
```

The head start page is driven by the `PreSeedSummary` computed at page load:

```java
public class PreSeedSummary {
    private String pspName;
    private int totalFieldsRequired;
    private int fieldsPreSeeded;
    private int totalEmployees;
    private int employeesFullyPopulated;
    private int employeesPartiallyPopulated;
    private int employeesMissing;
    private List<String> categoriesProvided;    // e.g., "Entity type", "Benefits list"
    private List<String> categoriesStillNeeded; // e.g., "Ownership info", "Hours worked"
    private int estimatedMinutesRemaining;
    private int estimatedUploadsNeeded;
    private boolean hasPreSeededData;           // false = skip head start, show normal Page 1

    public double getCompletenessPercentage() {
        return totalFieldsRequired > 0
            ? (double) fieldsPreSeeded / totalFieldsRequired * 100.0
            : 0.0;
    }
}
```

#### Modified Scorecard: "Provided by Advisor" vs. "Still Needed"

The data completeness scorecard (Section 12) is extended with source-aware rendering:

| Scorecard Item | Standard Mode | Head Start Mode |
|---|---|---|
| Filled field | Green checkmark | Green checkmark with "Provided by [PSP Name]" badge |
| Missing field | Red X | Red X with "Needed from you" badge |
| Confirmed field | N/A | Blue checkmark with "You confirmed" badge |
| Changed field | N/A | Yellow pencil with "You updated" badge |
| Disputed field | N/A | Orange flag with "Flagged for review" badge |

Each scorecard row in head start mode shows:

```html
<tr class="scorecard-row" data-source="${field.source}">
    <td>${field.displayName}</td>
    <td>
        <c:choose>
            <c:when test="${field.source == 'psp_upload' || field.source == 'psp_manual_entry'}">
                <span class="badge bg-info">Provided by ${pspName}</span>
                <span class="text-muted">${field.value}</span>
                <button class="btn btn-sm btn-outline-warning flag-btn"
                        data-field="${field.path}">Flag as incorrect</button>
            </c:when>
            <c:when test="${field.source == 'test_taker_confirmed'}">
                <span class="badge bg-primary">You confirmed</span>
                <span>${field.value}</span>
            </c:when>
            <c:when test="${empty field.value}">
                <span class="badge bg-danger">Needed from you</span>
            </c:when>
        </c:choose>
    </td>
</tr>
```

#### "Review Pre-Loaded Data" Optional Step

When the test taker clicks "Review Pre-Loaded Data" on the head start page, they see a read-only summary grouped by source:

1. **Documents uploaded by advisor** -- list of files the PSP uploaded, with a summary of what data each provided (e.g., "Carrier enrollment file provided benefits elections for 142 employees")
2. **Data entered by advisor** -- structural fields the PSP manually entered (entity type, plan design answers, eligibility rules)
3. **Data from prior year** -- if applicable, what was rolled forward with a "verify still current" prompt

For each section, the test taker can:
- **Confirm** ("This looks correct") -- marks the data as `test_taker_confirmed`
- **Flag** ("Something looks wrong") -- creates a dispute record for the PSP reviewer to address

```java
public class DataDispute {
    private String disputeId;       // UUID
    private long testRunId;
    private String employeeId;      // null for structural disputes
    private String fieldPath;       // e.g., "employment.hours_worked_annual"
    private Object preSeededValue;  // what the PSP provided
    private String testTakerComment; // free text: "I think this should be 1500 hours"
    private Object suggestedValue;  // optional: what the test taker thinks it should be
    private String status;          // OPEN, RESOLVED_ACCEPT_ORIGINAL, RESOLVED_ACCEPT_CHANGE
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private Long resolvedByPersonId;
}
```

Disputes are stored in the test run's `census_metadata` JSON under a `disputes` array. The PSP reviewer sees them during the REVIEW phase.

#### Reduced Upload Suggestions

The upload suggestion engine (Section 12) is modified in head start mode to only suggest uploads for gaps the PSP couldn't fill:

```java
public List<UploadSuggestion> getUploadSuggestions(NdtTestRun run) {
    List<UploadSuggestion> allSuggestions = calculateAllGaps(run);

    if (!run.hasPreSeededData()) {
        return allSuggestions; // normal mode: suggest everything
    }

    // Head start mode: filter out suggestions for data categories
    // that already have sufficient coverage from pre-seeding
    return allSuggestions.stream()
        .filter(s -> !isAdequatelyCoveredByPreSeed(run, s.getDataCategory()))
        .map(s -> {
            // Adjust the suggestion text for head start context
            s.setDescription("Your advisor provided " + s.getPreSeededCoverage() +
                "% of " + s.getDataCategory() + " data. " +
                "Upload this to fill the remaining gaps.");
            return s;
        })
        .collect(Collectors.toList());
}
```

If pre-seeding covered everything, the test taker sees no upload prompts at all -- just confirmation questions and a "Submit for Review" button.

#### Handling Disagreements

When the test taker flags pre-loaded data as incorrect:

1. The field is marked as `disputed` in the census metadata
2. The test taker can optionally provide their believed-correct value and a comment
3. The dispute appears in the PSP reviewer's dashboard during the REVIEW phase
4. The PSP reviewer resolves each dispute by:
   - Accepting the test taker's correction (updates the census value, source becomes `test_taker_manual`)
   - Keeping the original value with a justification note
   - Sending the item back to the test taker for more information

Dispute resolution flow:

```
Test Taker flags field  -->  Dispute created (status: OPEN)
                              |
                              v
PSP Reviewer sees dispute in REVIEW phase
                              |
              +---------------+---------------+
              |               |               |
              v               v               v
     Accept change     Keep original    Send back for
     (update value)    (add note)       more info
              |               |               |
              v               v               v
     RESOLVED_ACCEPT    RESOLVED_KEEP    IN_PROGRESS
     _CHANGE            _ORIGINAL        (re-open wizard)
```

### 13c. Pre-Seed Sources

The system supports six categories of pre-seed data, each with its own ingestion pathway.

#### 13c.1 Manual PSP Upload

PSP reviewer uploads documents through the reviewer dashboard. Uses the same parsing pipeline as test taker uploads (Section 10) but with PSP-specific metadata:

```java
public class PreSeedUpload {
    private long testRunId;
    private long uploadedByPersonId; // PSP reviewer
    private String fileName;
    private String fileType;         // carrier_enrollment, payroll_export, census_file, other
    private LocalDateTime uploadedAt;
    private String parseStatus;      // PENDING, PARSED, FAILED
    private int recordsParsed;
    private int recordsMerged;       // how many matched existing employees
    private int recordsNew;          // how many were new additions
    private int conflictsDetected;   // how many fields conflicted with existing data
}
```

The upload pipeline recognizes the PSP context and tags all extracted data with `source: "psp_upload"` and `source_detail: "<filename>"`. Merge conflicts with existing data are handled by the conflict resolution engine from Section 12, with the PSP reviewer resolving conflicts instead of the test taker.

#### 13c.2 Carrier Integration Feed (Future)

Hook interface for automated enrollment data from insurance carriers:

```java
public interface CarrierFeedProvider {

    /**
     * Identifier for this carrier integration (e.g., "anthem_blue_cross", "united_healthcare").
     */
    String getCarrierId();

    /**
     * Fetch enrollment data for a given employer and plan year.
     * Returns parsed employee benefit election records.
     */
    List<CarrierEnrollmentRecord> fetchEnrollment(
        String employerIdentifier,
        int planYear,
        CarrierCredentials credentials
    );

    /**
     * Map carrier-specific field names to census schema field paths.
     */
    Map<String, String> getFieldMapping();
}

public class CarrierEnrollmentRecord {
    private String carrierMemberId;
    private String firstName;
    private String lastName;
    private String ssn;                    // for matching, never stored
    private String dateOfBirth;
    private String benefitType;            // medical, dental, vision, etc.
    private String planName;
    private String coverageTier;           // ee_only, ee_spouse, ee_children, family
    private BigDecimal employeeContribution;
    private BigDecimal employerContribution;
    private String effectiveDate;
    private String terminationDate;
}
```

When a carrier feed is available, the PSP reviewer dashboard shows an "Import from [Carrier Name]" button that triggers the fetch and merge.

#### 13c.3 Payroll Integration Feed (Future)

Hook interface for automated payroll data:

```java
public interface PayrollFeedProvider {

    String getPayrollProviderId();

    /**
     * Fetch YTD payroll data for a given employer and date range.
     */
    List<PayrollRecord> fetchPayrollData(
        String employerIdentifier,
        LocalDate periodStart,
        LocalDate periodEnd,
        PayrollCredentials credentials
    );

    Map<String, String> getFieldMapping();
}

public class PayrollRecord {
    private String payrollEmployeeId;
    private String firstName;
    private String lastName;
    private String ssn;                     // for matching, never stored
    private BigDecimal ytdGrossCompensation;
    private BigDecimal ytdW2Compensation;
    private BigDecimal ytdHoursWorked;
    private String payFrequency;            // weekly, biweekly, semi_monthly, monthly
    private String employmentStatus;        // active, terminated, leave
    private String hireDate;
    private String terminationDate;
    private BigDecimal ytd401kDeferral;
    private BigDecimal ytdEmployerMatch;
    private BigDecimal ytdFsaContribution;
    private BigDecimal ytdDcfsaContribution;
}
```

#### 13c.4 Prior Year Rollover

Detailed in Section 13d below. The pre-seed source identifier is `prior_year_rollover` with `prior_test_run_id` in the metadata.

#### 13c.5 Manual Data Entry

PSP reviewer enters values through structured forms in the reviewer dashboard. The forms render the same questionnaire fields the test taker would see, but in "advisor mode":

- Labels say "Enter on behalf of employer" instead of "Tell us about your plan"
- Help text is tailored for PSP professionals (assumes familiarity with plan documents)
- Sections are organized by data category rather than by the guided wizard flow
- All entries are tagged with `source: "psp_manual_entry"` and `entered_by_person_id`

```java
/**
 * Servlet for PSP manual data entry during DRAFT phase.
 * Renders questionnaire fields in advisor mode and saves with PSP source tags.
 */
@WebServlet("/ndt/preseed/manual-entry")
public class PreSeedManualEntry extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        NdtTestRun run = getTestRun(req);
        validateStatus(run, TestRunStatus.DRAFT);
        validateIsPspReviewer(req, run);

        // Load questionnaire fields grouped by category for advisor view
        List<FieldCategory> categories = questionnaireService.getFieldsByCategory(
            run.getQuestionnaireId(), RenderMode.ADVISOR);

        // Pre-fill with any existing values (from uploads or prior entry)
        Map<String, Object> existingValues = censusService.getStructuralValues(run);

        req.setAttribute("categories", categories);
        req.setAttribute("existingValues", existingValues);
        req.setAttribute("testRun", run);
        req.getRequestDispatcher("/WEB-INF/view/ndt/preseedManualEntry.jsp")
           .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        NdtTestRun run = getTestRun(req);
        validateStatus(run, TestRunStatus.DRAFT);
        Person reviewer = getCurrentPerson(req);

        Map<String, String> fieldValues = extractFieldValues(req);

        for (Map.Entry<String, String> entry : fieldValues.entrySet()) {
            censusService.setStructuralValue(
                run,
                entry.getKey(),   // field path
                entry.getValue(), // value
                "psp_manual_entry",
                reviewer.getId()
            );
        }

        resp.sendRedirect("/ndt/reviewer/dashboard?testRunId=" + run.getId());
    }
}
```

#### 13c.6 Template Pre-Fill

PSPs can create reusable templates for common plan configurations. A template captures a set of plan design answers that can be applied to new test runs as a starting point.

Data model:

```java
@Entity
@Table(name = "ndt_preseed_template")
public class PreSeedTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "psp_id", nullable = false)
    private Long pspId;

    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;

    @Column(name = "description", length = 1000)
    private String description;

    /** JSON object mapping field paths to pre-fill values */
    @Column(name = "field_values", columnDefinition = "JSON")
    private String fieldValuesJson;

    @Column(name = "created_by_person_id")
    private Long createdByPersonId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "usage_count")
    private int usageCount;

    @Column(name = "is_active")
    private boolean active = true;
}
```

Template field values JSON structure:

```json
{
  "structural": {
    "entity_type": "c_corp",
    "is_controlled_group": false,
    "plan_year_start": "01-01",
    "plan_year_end": "12-31"
  },
  "benefits_offered": [
    { "type": "health_fsa", "is_offered": true },
    { "type": "dcfsa", "is_offered": true },
    { "type": "medical", "is_offered": true },
    { "type": "dental", "is_offered": false },
    { "type": "vision", "is_offered": false }
  ],
  "eligibility": {
    "waiting_period_days": 90,
    "minimum_age": null,
    "minimum_hours_per_week": 30,
    "exclude_union": true,
    "exclude_nonresident_alien": true
  },
  "plan_design": {
    "health_fsa_limit": 3050,
    "dcfsa_limit": 5000,
    "employer_match_formula": null,
    "safe_harbor_type": "none"
  }
}
```

When a PSP reviewer applies a template:

```java
public void applyTemplate(NdtTestRun run, PreSeedTemplate template, Person reviewer) {
    Map<String, Object> fieldValues = parseJson(template.getFieldValuesJson());

    for (Map.Entry<String, Object> entry : flattenFields(fieldValues).entrySet()) {
        censusService.setStructuralValue(
            run,
            entry.getKey(),
            entry.getValue(),
            "template_prefill",
            reviewer.getId()
        );
    }

    // Track template usage
    template.setUsageCount(template.getUsageCount() + 1);
    em.merge(template);

    auditLog(run, "TEMPLATE_APPLIED", reviewer,
        Map.of("template_id", template.getId(),
               "template_name", template.getTemplateName(),
               "fields_applied", fieldValues.size()));
}
```

#### Pre-Seed Source Data Model Additions

Database table for tracking all pre-seed operations:

```sql
CREATE TABLE ndt_preseed_audit (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    test_run_id     BIGINT NOT NULL,
    source_type     VARCHAR(30) NOT NULL,       -- psp_upload, psp_manual_entry, prior_year_rollover, template_prefill, carrier_feed, payroll_feed
    source_detail   VARCHAR(500),               -- filename, template name, prior run ID, etc.
    person_id       BIGINT NOT NULL,            -- who performed the action
    action          VARCHAR(50) NOT NULL,        -- UPLOAD, PARSE, MANUAL_ENTRY, ROLLOVER, TEMPLATE_APPLY
    records_affected INT DEFAULT 0,
    fields_affected  INT DEFAULT 0,
    conflicts_created INT DEFAULT 0,
    metadata_json   JSON,                       -- additional context (field paths, conflict details)
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_preseed_audit_run (test_run_id),
    INDEX idx_preseed_audit_person (person_id),
    CONSTRAINT fk_preseed_audit_run FOREIGN KEY (test_run_id) REFERENCES ndt_test_run(id),
    CONSTRAINT fk_preseed_audit_person FOREIGN KEY (person_id) REFERENCES person(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

JPA entity:

```java
@Entity
@Table(name = "ndt_preseed_audit")
public class PreSeedAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_run_id", nullable = false)
    private Long testRunId;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType;

    @Column(name = "source_detail", length = 500)
    private String sourceDetail;

    @Column(name = "person_id", nullable = false)
    private Long personId;

    @Column(name = "action", nullable = false, length = 50)
    private String action;

    @Column(name = "records_affected")
    private int recordsAffected;

    @Column(name = "fields_affected")
    private int fieldsAffected;

    @Column(name = "conflicts_created")
    private int conflictsCreated;

    @Column(name = "metadata_json", columnDefinition = "JSON")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
```

Integration hook interface for future carrier/payroll feeds:

```java
/**
 * Common interface for all external data feed providers.
 * Implementations handle carrier-specific or payroll-specific logic.
 */
public interface ExternalFeedProvider {

    /** Unique identifier for this provider (e.g., "anthem", "adp", "paychex"). */
    String getProviderId();

    /** Human-readable name shown in the UI. */
    String getDisplayName();

    /** Whether this provider is currently configured and available. */
    boolean isAvailable(Long pspId);

    /** Fetch data and return normalized records ready for census merge. */
    List<NormalizedFeedRecord> fetchData(FeedRequest request);

    /** Field mapping from provider schema to census schema. */
    Map<String, String> getFieldMapping();
}

public class FeedRequest {
    private Long testRunId;
    private String employerIdentifier;
    private int planYear;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Map<String, String> credentials;
}

public class NormalizedFeedRecord {
    private String sourceEmployeeId;
    private String matchKey;              // SSN hash or name+DOB for matching
    private Map<String, Object> fields;   // field_path -> value
    private String sourceProvider;
    private LocalDateTime fetchedAt;
}
```

### 13d. Prior Year Rollover

When an employer had an NDT test run in the previous year, the system offers to roll forward data that is likely still valid, reducing both PSP and test taker effort.

#### Identifying Prior Year Tests

The system matches prior year tests using a multi-factor approach:

```java
public class PriorYearMatcher {

    /**
     * Find the most recent completed test run for the same employer.
     * Matching priority:
     * 1. Same employer_id (direct link)
     * 2. Same employer name + same PSP (fuzzy match fallback)
     * 3. Same activity chain (if activities are linked year-over-year)
     */
    public Optional<NdtTestRun> findPriorYear(NdtTestRun currentRun) {
        Long employerId = currentRun.getEmployerId();
        int currentPlanYear = currentRun.getPlanYear();
        Long pspId = currentRun.getPspId();

        // Strategy 1: Direct employer_id match
        Optional<NdtTestRun> match = testRunDao.findByEmployerAndPlanYear(
            employerId, currentPlanYear - 1, TestRunStatus.COMPLETE);
        if (match.isPresent()) return match;

        // Strategy 2: Name + PSP match (handles employer_id changes)
        String employerName = currentRun.getEmployer().getName();
        match = testRunDao.findByEmployerNameAndPspAndPlanYear(
            employerName, pspId, currentPlanYear - 1, TestRunStatus.COMPLETE);
        if (match.isPresent()) return match;

        // Strategy 3: Activity chain (NDT activities linked to same employer across years)
        Long activityId = currentRun.getActivityId();
        if (activityId != null) {
            match = testRunDao.findByLinkedActivityPriorYear(
                activityId, currentPlanYear - 1);
        }

        return match;
    }
}
```

#### What Gets Copied vs. Flagged

| Data Category | Rollover Behavior | Rationale |
|---|---|---|
| Entity type | Copy, flag "verify still current" | Rarely changes but corporate restructuring happens |
| Controlled group status | Copy, flag "verify still current" | M&A activity can change this |
| Related entity list | Copy, flag "verify still current" | Entities may be added or removed |
| Benefits offered | Copy, flag "verify still current" | Plan lineup usually stable year to year |
| Plan eligibility rules | Copy, flag "verify still current" | Eligibility rules are relatively stable |
| Ownership structure | Copy percentages, flag "verify and update" | Ownership percentages commonly shift |
| Officer designations | Copy, flag "verify still current" | Officers may change |
| Plan design values (FSA limits, etc.) | Copy, flag "IRS limits may have changed" | IRS adjusts limits annually |
| Employee list | Copy names and IDs only, flag all as "verify employment status" | New hires, terms, status changes |
| Compensation amounts | Do NOT copy | Changes every year |
| Benefits elections | Do NOT copy | Re-enrollment happens annually |
| Hours worked | Do NOT copy | Changes every year |
| Deferral amounts | Do NOT copy | Employee elections change annually |

#### PSP Reviewer Rollover UI

When the PSP reviewer clicks "Import Prior Year" during DRAFT mode, they see:

```
+-----------------------------------------------------------------------+
|  Prior Year Data Rollover                                             |
|                                                                       |
|  We found a completed test for [Employer Name] from plan year 2025.   |
|  Test completed: March 15, 2025 | 187 employees                      |
|                                                                       |
|  Select what to roll forward:                                         |
|                                                                       |
|  [x] Entity structure                                                 |
|      Entity type: C Corporation                                       |
|      Controlled group: No                                             |
|      --> Will be flagged "verify still current"                       |
|                                                                       |
|  [x] Benefits offered                                                 |
|      Health FSA, DCFSA, Medical, Dental                               |
|      --> Will be flagged "verify still current"                       |
|                                                                       |
|  [x] Plan eligibility rules                                          |
|      90-day waiting period, 30 hrs/wk minimum                         |
|      --> Will be flagged "verify still current"                       |
|                                                                       |
|  [x] Ownership structure                                              |
|      3 owners identified last year                                    |
|      --> Percentages flagged "verify and update"                      |
|                                                                       |
|  [x] Officer designations                                             |
|      5 officers identified last year                                  |
|      --> Will be flagged "verify still current"                       |
|                                                                       |
|  [x] Plan design values                                               |
|      FSA limit: $3,050 | DCFSA limit: $5,000                         |
|      --> Flagged "IRS limits may have changed for 2026"               |
|                                                                       |
|  [x] Employee roster (names and IDs only)                             |
|      187 employees from last year                                     |
|      --> All flagged "verify employment status"                       |
|      --> Compensation, elections, hours NOT copied                    |
|                                                                       |
|  [ ] Do NOT roll forward anything (start fresh)                       |
|                                                                       |
|  +-----------------------------------+                                |
|  | Apply Selected Rollover Items      |                               |
|  +-----------------------------------+                                |
+-----------------------------------------------------------------------+
```

#### Rollover Execution

```java
public class PriorYearRolloverService {

    public RolloverResult executeRollover(
            NdtTestRun currentRun,
            NdtTestRun priorRun,
            RolloverSelections selections,
            Person reviewer) {

        RolloverResult result = new RolloverResult();

        if (selections.isIncludeEntityStructure()) {
            copyStructuralFields(priorRun, currentRun, ENTITY_STRUCTURE_FIELDS, result);
        }

        if (selections.isIncludeBenefitsOffered()) {
            copyStructuralFields(priorRun, currentRun, BENEFITS_OFFERED_FIELDS, result);
        }

        if (selections.isIncludeEligibilityRules()) {
            copyStructuralFields(priorRun, currentRun, ELIGIBILITY_RULE_FIELDS, result);
        }

        if (selections.isIncludeOwnership()) {
            copyOwnershipWithVerifyFlag(priorRun, currentRun, result);
        }

        if (selections.isIncludeOfficers()) {
            copyOfficerDesignations(priorRun, currentRun, result);
        }

        if (selections.isIncludePlanDesign()) {
            copyPlanDesignWithLimitWarnings(priorRun, currentRun, result);
        }

        if (selections.isIncludeEmployeeRoster()) {
            copyEmployeeRosterNamesOnly(priorRun, currentRun, result);
        }

        // Tag all copied data
        tagRolledForwardData(currentRun, priorRun.getId());

        // Create audit record
        auditLog(currentRun, "PRIOR_YEAR_ROLLOVER", reviewer,
            Map.of(
                "prior_run_id", priorRun.getId(),
                "prior_plan_year", priorRun.getPlanYear(),
                "fields_copied", result.getFieldsCopied(),
                "employees_copied", result.getEmployeesCopied(),
                "flags_created", result.getFlagsCreated()
            ));

        return result;
    }

    private void copyStructuralFields(
            NdtTestRun source, NdtTestRun target,
            List<String> fieldPaths, RolloverResult result) {
        for (String fieldPath : fieldPaths) {
            Object value = censusService.getStructuralValue(source, fieldPath);
            if (value != null) {
                censusService.setStructuralValue(
                    target, fieldPath, value,
                    "prior_year_rollover",
                    null  // system action, no specific person
                );
                censusService.addVerificationFlag(target, fieldPath,
                    "Rolled forward from " + source.getPlanYear() +
                    " test. Please verify this is still current.");
                result.incrementFieldsCopied();
                result.incrementFlagsCreated();
            }
        }
    }

    private void copyEmployeeRosterNamesOnly(
            NdtTestRun source, NdtTestRun target, RolloverResult result) {
        List<CensusEmployee> priorEmployees = censusService.getEmployees(source);

        for (CensusEmployee emp : priorEmployees) {
            CensusEmployee skeleton = new CensusEmployee();
            skeleton.setEmployeeId(emp.getEmployeeId());
            skeleton.setDisplayName(emp.getDisplayName());
            skeleton.setHireDate(emp.getHireDate());
            // Do NOT copy: compensation, benefits, hours, deferrals
            skeleton.setSource("prior_year_rollover");
            skeleton.setPriorTestRunId(source.getId());
            skeleton.setVerificationFlag(
                "Rolled forward from " + source.getPlanYear() +
                ". Verify employment status and update all data fields.");

            censusService.addEmployee(target, skeleton);
            result.incrementEmployeesCopied();
            result.incrementFlagsCreated();
        }
    }

    private void copyPlanDesignWithLimitWarnings(
            NdtTestRun source, NdtTestRun target, RolloverResult result) {
        // IRS limit fields that change annually
        Set<String> irsLimitFields = Set.of(
            "plan_design.health_fsa_limit",
            "plan_design.dcfsa_limit",
            "plan_design.401k_elective_deferral_limit",
            "plan_design.401k_catch_up_limit",
            "plan_design.annual_compensation_limit",
            "plan_design.hce_threshold"
        );

        for (String fieldPath : PLAN_DESIGN_FIELDS) {
            Object value = censusService.getStructuralValue(source, fieldPath);
            if (value != null) {
                censusService.setStructuralValue(
                    target, fieldPath, value,
                    "prior_year_rollover", null);

                String flagMessage = irsLimitFields.contains(fieldPath)
                    ? "Rolled forward from " + source.getPlanYear() +
                      ". IRS limits may have changed for " + target.getPlanYear() +
                      ". Please verify the current limit."
                    : "Rolled forward from " + source.getPlanYear() +
                      ". Verify still current.";

                censusService.addVerificationFlag(target, fieldPath, flagMessage);
                result.incrementFieldsCopied();
                result.incrementFlagsCreated();
            }
        }
    }
}

public class RolloverResult {
    private int fieldsCopied;
    private int employeesCopied;
    private int flagsCreated;
    private List<String> warnings;  // e.g., "Prior year had 0 employees -- nothing to roll forward"
    // getters, incrementers
}
```

#### Rolled-Forward Data Tags

All rolled-forward data carries consistent metadata in the census JSON:

```json
{
  "source": "prior_year_rollover",
  "prior_test_run_id": 1042,
  "prior_plan_year": 2025,
  "rolled_forward_at": "2026-03-20T09:30:00",
  "verification_status": "pending",
  "verification_flag": "Rolled forward from 2025 test. Please verify this is still current."
}
```

#### "Changes Since Last Year" Review Page

When a test taker opens the wizard for a run that includes rolled-forward data, they see a dedicated review step after the head start page:

```
+-----------------------------------------------------------------------+
|  Changes Since Last Year                                              |
|                                                                       |
|  Your advisor rolled forward some data from your 2025 NDT test.       |
|  Please confirm what's still current and update what's changed.       |
|                                                                       |
|  PLAN STRUCTURE                                                       |
|  +---------------------------------------------------------------+   |
|  | Entity Type: C Corporation                                     |   |
|  | Last year: C Corporation                                       |   |
|  |  ( ) Still the same  ( ) Changed  _______________              |   |
|  +---------------------------------------------------------------+   |
|  | Controlled Group: No                                           |   |
|  | Last year: No                                                  |   |
|  |  ( ) Still the same  ( ) Changed  _______________              |   |
|  +---------------------------------------------------------------+   |
|                                                                       |
|  PLAN DESIGN                                                          |
|  +---------------------------------------------------------------+   |
|  | Health FSA Limit: $3,050                                       |   |
|  | Last year: $3,050                                              |   |
|  | Note: IRS limits may have changed for 2026.                    |   |
|  |  ( ) Still the same  ( ) Changed  _______________              |   |
|  +---------------------------------------------------------------+   |
|                                                                       |
|  ELIGIBILITY                                                          |
|  +---------------------------------------------------------------+   |
|  | Waiting Period: 90 days                                        |   |
|  | Last year: 90 days                                             |   |
|  |  ( ) Still the same  ( ) Changed  _______________              |   |
|  +---------------------------------------------------------------+   |
|                                                                       |
|  EMPLOYEE ROSTER                                                      |
|  +---------------------------------------------------------------+   |
|  | 187 employees rolled forward from last year                    |   |
|  | Please indicate:                                               |   |
|  |  - New hires since last test: [___] (approximate count)        |   |
|  |  - Terminations since last test: [___] (approximate count)     |   |
|  |  Or upload a current employee roster to reconcile.             |   |
|  |  [Upload Current Roster]                                       |   |
|  +---------------------------------------------------------------+   |
|                                                                       |
|  [Save and Continue -->]                                              |
+-----------------------------------------------------------------------+
```

When the test taker selects "Still the same" for a field, it is marked as `test_taker_confirmed` with the rolled-forward value preserved. When they select "Changed" and provide a new value, the census is updated with `source: "test_taker_manual"` and the prior year value is retained in the `alternative_values` array for audit purposes.

### 13e. Pre-Seed Completeness Thresholds

The system provides the PSP reviewer with guidance on when enough pre-seeding has been done, expressed as tiers with estimated test taker effort.

#### Completeness Tiers

| Tier | Label | Requirements | Est. Test Taker Effort |
|---|---|---|---|
| 0 | No Pre-Seed | Nothing pre-seeded | 60-90 min, 3-5 uploads |
| 1 | Minimum Viable | Entity type + benefits offered + plan year | 45-60 min, 3-4 uploads |
| 2 | Good | Tier 1 + carrier enrollment file | 15-30 min, 1-2 uploads |
| 3 | Great | Tier 2 + payroll data + ownership info | 5-15 min, 0-1 uploads |
| 4 | Complete | All data provided | 5 min, 0 uploads (confirm only) |

#### Completeness Calculation

```java
public class PreSeedCompleteness {

    private int tier;
    private String tierLabel;
    private double overallPercentage;
    private int estimatedMinutesRemaining;
    private int estimatedUploadsNeeded;
    private List<String> missingMinimumFields;
    private List<CompletionSuggestion> suggestions;

    /**
     * Data categories and their weights in the completeness calculation.
     * Weights reflect how much test taker effort each category saves.
     */
    private static final Map<String, Double> CATEGORY_WEIGHTS = Map.of(
        "entity_structure",    0.05,   // quick questions, low weight
        "benefits_offered",    0.05,   // quick questions, low weight
        "plan_year",           0.02,   // single field
        "eligibility_rules",   0.08,   // moderate complexity
        "plan_design",         0.10,   // moderate complexity
        "ownership",           0.10,   // often requires research
        "officer_designations", 0.05,  // moderate
        "employee_roster",     0.15,   // significant data volume
        "compensation_data",   0.15,   // requires payroll data
        "benefits_elections",  0.15,   // requires carrier data
        "hours_worked",        0.10    // requires payroll/timecard data
    );

    public static PreSeedCompleteness calculate(NdtTestRun run) {
        PreSeedCompleteness result = new PreSeedCompleteness();
        double weightedScore = 0.0;

        for (Map.Entry<String, Double> entry : CATEGORY_WEIGHTS.entrySet()) {
            String category = entry.getKey();
            double weight = entry.getValue();
            double categoryCompleteness = calculateCategoryCompleteness(run, category);
            weightedScore += weight * categoryCompleteness;
        }

        result.overallPercentage = weightedScore * 100.0;
        result.tier = determineTier(run);
        result.tierLabel = TIER_LABELS.get(result.tier);
        result.estimatedMinutesRemaining = estimateMinutes(result.tier, weightedScore);
        result.estimatedUploadsNeeded = estimateUploads(run);
        result.missingMinimumFields = findMissingMinimum(run);
        result.suggestions = generateSuggestions(run, result.tier);

        return result;
    }

    public boolean meetsMinimumViable() {
        return missingMinimumFields.isEmpty();
    }

    private static int determineTier(NdtTestRun run) {
        boolean hasEntityType = hasValue(run, "structural.entity_type");
        boolean hasBenefits = hasValue(run, "structural.benefits_offered");
        boolean hasPlanYear = hasValue(run, "structural.plan_year_start");
        boolean hasCarrierData = hasSourceData(run, "psp_upload", "carrier")
                              || hasSourceData(run, "carrier_feed", null);
        boolean hasPayrollData = hasSourceData(run, "psp_upload", "payroll")
                              || hasSourceData(run, "payroll_feed", null);
        boolean hasOwnership = hasValue(run, "structural.ownership");
        boolean hasAllStructural = hasAllStructuralFields(run);
        boolean hasAllCensusData = hasCompleteCensus(run);

        if (hasAllStructural && hasAllCensusData) return 4; // Complete
        if (hasEntityType && hasBenefits && hasPlanYear
            && hasCarrierData && hasPayrollData && hasOwnership) return 3; // Great
        if (hasEntityType && hasBenefits && hasPlanYear
            && hasCarrierData) return 2; // Good
        if (hasEntityType && hasBenefits && hasPlanYear) return 1; // Minimum Viable
        return 0; // No Pre-Seed
    }

    private static int estimateMinutes(int tier, double weightedScore) {
        return switch (tier) {
            case 0 -> 75;  // 60-90 average
            case 1 -> 50;  // 45-60 average
            case 2 -> 20;  // 15-30 average
            case 3 -> 10;  // 5-15 average
            case 4 -> 5;   // confirmation only
            default -> 75;
        };
    }

    private static int estimateUploads(NdtTestRun run) {
        int uploads = 0;
        if (!hasSourceData(run, "psp_upload", "carrier")
            && !hasSourceData(run, "carrier_feed", null)) uploads++; // needs carrier data
        if (!hasSourceData(run, "psp_upload", "payroll")
            && !hasSourceData(run, "payroll_feed", null)) uploads++; // needs payroll data
        if (!hasSourceData(run, "psp_upload", "census")
            && !hasCompleteCensus(run)) uploads++;                   // needs census
        if (!hasValue(run, "structural.ownership")) uploads++;       // may need ownership doc
        return uploads;
    }
}
```

#### Completion Suggestions

The system generates actionable suggestions to help the PSP reach the next tier:

```java
public class CompletionSuggestion {
    private String action;              // e.g., "Upload carrier enrollment file"
    private String impact;              // e.g., "Will fill benefits elections for employees"
    private int minutesSaved;           // estimated minutes saved for test taker
    private int tierAfterCompletion;    // what tier this would reach
    private String priority;            // HIGH, MEDIUM, LOW
}
```

Example suggestions at each tier:

**Tier 0 (No Pre-Seed) suggestions:**
- "Enter entity type, benefits offered, and plan year to reach Minimum Viable pre-seed. Saves ~15 minutes for the test taker." (HIGH)
- "Upload a carrier enrollment file to fill benefits elections for all employees. Saves ~25 minutes." (HIGH)

**Tier 1 (Minimum Viable) suggestions:**
- "Upload the carrier enrollment file from [Carrier Name]. This will fill benefits elections and reduce test taker effort from ~50 min to ~20 min." (HIGH)
- "Enter ownership information if available. Ownership data is often the hardest for test takers to find." (MEDIUM)

**Tier 2 (Good) suggestions:**
- "Upload payroll data (YTD compensation, hours worked). Combined with the carrier data already provided, this will reduce test taker effort to ~10 min." (HIGH)
- "Enter ownership structure and officer designations to reach Great pre-seed level." (MEDIUM)

**Tier 3 (Great) suggestions:**
- "Enter any remaining plan design values (FSA limits, match formula). The test taker will only need to confirm and verify." (LOW)
- "Review the employee roster for completeness. Any additions since last enrollment period?" (MEDIUM)

#### Effort Estimator Display

The PSP reviewer dashboard shows the effort estimator as a persistent widget:

```
+---------------------------------------------------------------+
|  TEST TAKER EFFORT ESTIMATOR                                  |
|                                                                |
|  Current pre-seed level: GOOD (Tier 2)                         |
|  Overall completeness: 58%                                     |
|                                                                |
|  If you send now:                                              |
|    Time: ~20 min  |  Uploads: 1-2  |  Questions: 12           |
|                                                                |
|  To reduce effort further:                                     |
|  +----------------------------------------------------------+ |
|  | [!] Upload payroll data                          -15 min  | |
|  |     Reduces to: ~10 min, 0-1 uploads, 5 questions        | |
|  +----------------------------------------------------------+ |
|  | [ ] Add ownership info                           -3 min   | |
|  |     Reduces to: ~17 min, 1-2 uploads, 9 questions        | |
|  +----------------------------------------------------------+ |
|                                                                |
|  +----------------------+                                      |
|  | Send to Test Taker   |   (minimum viable pre-seed met)     |
|  +----------------------+                                      |
+---------------------------------------------------------------+
```

The estimator updates in real time as the PSP adds data, providing immediate feedback on the impact of each pre-seeding action. The "Send to Test Taker" button is enabled once minimum viable pre-seed is met (Tier 1 or above) and disabled with a tooltip explaining what's missing when at Tier 0.
