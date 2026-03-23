# NDT Section 125 Cafeteria Plan -- Minimal Questionnaire Redesign

## Executive Summary

The current questionnaire has **91 fields** across **19 sections** (A through X). Many of these fields ask for numeric counts and dollar totals that can be computed from employee-level data files. By accepting structured file uploads, the questionnaire can be reduced to as few as **12-15 questions** in the best case (all uploads provided) or **25-30 questions** in the no-upload case (compared to 91 today), because many current fields are conditional and only appear based on benefits offered.

---

## 1. Data Points Required for Each Test

### 1.1 Section 125 Eligibility Test

| # | Data Point | Classification | Source |
|---|-----------|---------------|--------|
| 1 | Total employees (all related entities, plan year) | EXTRACTABLE | Census file: count rows with active status during plan year |
| 2 | CBA-covered employee count | EXTRACTABLE | Census file: union_status or CBA flag column |
| 3 | Whether CBA benefits were bargained for | JUDGMENT | Requires knowledge of CBA negotiations |
| 4 | Whether CBA employees can participate in cafeteria plan | PLAN DESIGN | Plan document / employer knowledge |
| 5 | Employees not meeting service requirement | EXTRACTABLE | Census: hire_date vs. plan year start, compared against plan waiting period |
| 6 | Employees under minimum age | EXTRACTABLE | Census: DOB vs. plan year start |
| 7 | Part-time/seasonal employees (<1,000 hours) | EXTRACTABLE | Census or payroll: hours_worked column |
| 8 | Nonresident aliens with no US-source income | EXTRACTABLE (partial) | Census: citizenship/visa status column (if present); often requires JUDGMENT |
| 9 | COBRA participants | EXTRACTABLE | Census or carrier file: COBRA flag |
| 10 | HCI count: >5% owners | EXTRACTABLE | Ownership list file |
| 11 | HCI count: officers (non-owner) | EXTRACTABLE | Ownership list file or census: officer flag |
| 12 | HCI count: high earners (>$160K for 2026 PY) | EXTRACTABLE | Payroll/census: prior-year compensation |
| 13 | HCI count: spouse/dependent employees of HCIs | JUDGMENT | Requires family relationship knowledge not in standard files |
| 14 | HCIs at non-covered related entities | EXTRACTABLE (partial) | Related entity list + ownership list; may require JUDGMENT |
| 15 | Whether plan is open to all eligible employees equally | PLAN DESIGN | Plan document knowledge |
| 16 | Classification description (if not open equally) | PLAN DESIGN | Plan document knowledge |
| 17 | Total non-excludable employees | EXTRACTABLE | Computed: total minus excludables |
| 18 | Non-HCIs eligible to participate | EXTRACTABLE | Census: enrollment eligibility flag or computed from plan rules |

### 1.2 Section 125 Contributions & Benefits Test

| # | Data Point | Classification | Source |
|---|-----------|---------------|--------|
| 1 | Whether same benefits offered to everyone equally | PLAN DESIGN | Plan document |
| 2 | Description of benefit differences by group | PLAN DESIGN | Plan document |
| 3 | Whether employer meets 75% health contribution safe harbor | PLAN DESIGN / EXTRACTABLE | Carrier invoice: employer vs. employee premium split |
| 4 | HCIs who elected benefits (HCPs) | EXTRACTABLE | Census + enrollment: cross-reference HCI list with elections |
| 5 | Non-HCIs who elected benefits | EXTRACTABLE | Census + enrollment: non-HCI employees with elections |
| 6 | Total salary reductions by HCPs | EXTRACTABLE | Payroll: pre-tax deductions for HCI employees |
| 7 | Total employer contributions for HCPs | EXTRACTABLE | Payroll or carrier file: employer contributions for HCI employees |
| 8 | Total salary reductions by non-HCIs | EXTRACTABLE | Payroll: pre-tax deductions for non-HCI employees |
| 9 | Total employer contributions for non-HCIs | EXTRACTABLE | Payroll or carrier file: employer contributions for non-HCI employees |

### 1.3 Section 125 Key Employee Concentration Test

| # | Data Point | Classification | Source |
|---|-----------|---------------|--------|
| 1 | Key employees: >5% owners | EXTRACTABLE | Ownership list |
| 2 | Key employees: 1-5% owners earning >$150K | EXTRACTABLE | Ownership list + payroll |
| 3 | Key employees: officers above comp threshold ($230K for 2026 PY) | EXTRACTABLE | Census officer flag + payroll compensation |
| 4 | Key employees at non-covered entities | EXTRACTABLE (partial) | Related entity list + ownership list |
| 5 | Key employees who elected benefits | EXTRACTABLE | Cross-reference key employee list with enrollment |
| 6 | Total salary reductions by key employees | EXTRACTABLE | Payroll: pre-tax deductions for key employees |
| 7 | Total employer contributions for key employees | EXTRACTABLE | Payroll/carrier: employer contributions for key employees |

### 1.4 Section 105(h) Eligibility Test (Health FSA / LP-FSA)

| # | Data Point | Classification | Source |
|---|-----------|---------------|--------|
| 1 | Employees with <3 years of service | EXTRACTABLE | Census: hire_date vs. plan year start |
| 2 | Employees under age 25 | EXTRACTABLE | Census: DOB vs. plan year start |
| 3 | Employees working <35 hours/week | EXTRACTABLE | Census or payroll: hours worked or scheduled hours |
| 4 | Nonresident aliens with no US-source income | EXTRACTABLE (partial) | Census: citizenship column |
| 5 | 105(h) HCIs: >10% owners | EXTRACTABLE | Ownership list |
| 6 | 105(h) HCIs: top-5 paid officers (non-owner) | EXTRACTABLE | Ownership list + payroll: rank by compensation |
| 7 | 105(h) HCIs: top-25% earners (non-owner/officer) | EXTRACTABLE | Payroll: rank all by compensation, top quartile |
| 8 | 105(h) HCIs at non-covered entities | EXTRACTABLE (partial) | Related entity list |
| 9 | Non-excludable employees benefiting (elected FSA) | EXTRACTABLE | Census/enrollment: FSA election flag |
| 10 | Total non-excludable employees (105(h) rules) | EXTRACTABLE | Computed from census |
| 11 | Non-excludable employees eligible to benefit | EXTRACTABLE | Census: eligibility flag or computed |
| 12 | Eligibility classification description | PLAN DESIGN | Plan document |
| 13 | 105(h) HCIs who benefit | EXTRACTABLE | Cross-reference HCI list with FSA elections |
| 14 | Non-excludable 105(h) HCIs | EXTRACTABLE | Computed |
| 15 | Non-HCIs who benefit | EXTRACTABLE | Computed |
| 16 | Total non-excludable non-HCIs | EXTRACTABLE | Computed |

### 1.5 Section 105(h) Benefits Test

| # | Data Point | Classification | Source |
|---|-----------|---------------|--------|
| 1 | Same expenses reimbursable for everyone? | PLAN DESIGN | Plan document |
| 2 | Same maximum reimbursement for everyone? | PLAN DESIGN | Plan document |
| 3 | Same employee contributions for everyone? | PLAN DESIGN | Plan document |
| 4 | Same waiting periods for everyone? | PLAN DESIGN | Plan document |
| 5 | Same dependent coverage for everyone? | PLAN DESIGN | Plan document |
| 6 | Executive physicals offered selectively? | PLAN DESIGN | Plan document |
| 7 | Benefits changed mid-year affecting groups differently? | JUDGMENT | Employer knowledge of mid-year changes |
| 8 | Descriptions of any differences (up to 6 conditional text fields) | PLAN DESIGN | Plan document |

### 1.6 Section 129 Eligibility Test (Dependent Care FSA)

| # | Data Point | Classification | Source |
|---|-----------|---------------|--------|
| 1 | Employees under age 21 | EXTRACTABLE | Census: DOB |
| 2 | Employees with <1 year of service | EXTRACTABLE | Census: hire_date |
| 3 | Nonresident aliens | EXTRACTABLE (partial) | Census: citizenship column |
| 4 | Employees earning <$25,000 | EXTRACTABLE | Payroll: compensation |
| 5 | 129 HCEs: >5% owners | EXTRACTABLE | Ownership list |
| 6 | 129 HCEs: high earners (>$160K for 2026 PY) | EXTRACTABLE | Payroll: prior-year compensation |
| 7 | 129 HCEs at non-covered entities | EXTRACTABLE (partial) | Related entity list |
| 8 | Whether DCFSA open to all equally | PLAN DESIGN | Plan document |
| 9 | Total non-excludable (129 rules) | EXTRACTABLE | Computed |
| 10 | Non-HCEs eligible for DCFSA | EXTRACTABLE | Census + plan rules |

### 1.7 Section 129 Benefits & Contributions Test

| # | Data Point | Classification | Source |
|---|-----------|---------------|--------|
| 1 | Same DCFSA maximum for everyone? | PLAN DESIGN | Plan document |
| 2 | Same DCFSA terms for everyone? | PLAN DESIGN | Plan document |
| 3 | Does employer contribute directly? | PLAN DESIGN | Plan document |
| 4 | Are employer contributions the same for everyone? | PLAN DESIGN | Plan document |
| 5 | Descriptions of differences (conditional text fields) | PLAN DESIGN | Plan document |

### 1.8 Section 129 More-Than-5% Owners Test

| # | Data Point | Classification | Source |
|---|-----------|---------------|--------|
| 1 | 5%+ owners who elected DCFSA | EXTRACTABLE | Ownership list + enrollment |
| 2 | Total DCFSA elections by 5%+ owners | EXTRACTABLE | Payroll: DCFSA deductions for owners |
| 3 | Owner spouse/dependent DCFSA elections exist? | JUDGMENT | Family relationship knowledge |
| 4 | Total DCFSA elections by owner spouses/dependents | EXTRACTABLE (if identified) | Payroll |
| 5 | Total DCFSA elections for all participants | EXTRACTABLE | Payroll: sum of all DCFSA deductions |

### 1.9 Section 129 55% Average Benefits Test

| # | Data Point | Classification | Source |
|---|-----------|---------------|--------|
| 1 | HCEs who elected DCFSA | EXTRACTABLE | Cross-reference HCE list with enrollment |
| 2 | Total DCFSA elections by HCEs | EXTRACTABLE | Payroll: DCFSA deductions for HCEs |
| 3 | Non-HCEs who elected DCFSA | EXTRACTABLE | Payroll/enrollment |
| 4 | Total DCFSA elections by non-HCEs | EXTRACTABLE | Payroll: DCFSA deductions for non-HCEs |

### 1.10 Simple Cafeteria Plan Safe Harbor

| # | Data Point | Classification | Source |
|---|-----------|---------------|--------|
| 1 | Has employer adopted Simple Cafeteria Plan? | PLAN DESIGN | Plan document |
| 2 | Average 100 or fewer employees? | EXTRACTABLE | Census: count employees in prior two years |
| 3 | Meets contribution requirement? | PLAN DESIGN | Plan document |
| 4 | All employees with 1,000+ hours eligible? | PLAN DESIGN | Plan document |

### Classification Summary

| Category | Count | Percentage |
|----------|-------|------------|
| EXTRACTABLE (fully from files) | ~55 | ~60% |
| EXTRACTABLE (partial -- needs confirmation) | ~8 | ~9% |
| PLAN DESIGN (requires plan document knowledge) | ~22 | ~24% |
| JUDGMENT (requires human knowledge) | ~6 | ~7% |

---

## 2. Standard File Uploads That Could Replace Questions

### 2.1 Employee Census with Benefits Enrollment

**What it is:** A combined employee roster showing demographic data, employment status, and benefit elections. This is the single most impactful file -- it can eliminate 40-50 manually entered data points.

**Common formats:** CSV, XLSX, PDF (less useful -- requires OCR)

**Data points it contains:**
- Employee name, SSN/ID, DOB, hire date, termination date
- Employment status (active, terminated, LOA, COBRA)
- Hours worked (annual or weekly)
- Employee class (salaried, hourly, executive, seasonal, etc.)
- Compensation (annual salary or YTD earnings)
- Union/CBA status
- Citizenship/visa status (sometimes)
- Benefits elected: medical tier, dental, vision, Health FSA election, DCFSA election, HSA election
- Pre-tax premium amounts
- Officer/owner flags (sometimes)

**Questions it eliminates:**
- C: c_total_employees, c_cba_employee_count
- G: g_under_service_requirement, g_under_minimum_age, g_part_time_seasonal, g_cobra
- H: h_under_3_years_service, h_under_age_25, h_under_35_hours
- I: i_under_age_21, i_under_1_year_service
- J: j_high_comp_non_owner_officer (if compensation included)
- K: k_top25pct_non_owner_officer (if compensation included)
- L: l_high_comp_non_owners (if compensation included)
- N: n_total_non_excludable, n_non_hci_eligible
- O: o_hcp_count, o_non_hci_participant_count
- Q: all numeric fields (q_employees_benefiting, q_total_non_excludable, etc.)
- S: s_total_non_excludable, s_non_hce_eligible
- V: v_hce_participant_count, v_non_hce_participant_count

**Example column headers to detect:**
```
Employee Name, Last Name, First Name, SSN, Date of Birth, DOB, Birth Date,
Hire Date, Original Hire Date, Termination Date, Term Date,
Status, Employment Status, Active/Terminated,
Hours, Annual Hours, Hours Worked, Weekly Hours, Avg Weekly Hours,
Salary, Annual Salary, Compensation, Annual Comp, YTD Gross,
Class, Employee Class, Job Category, Classification, EE Class,
Union, CBA, Bargaining Unit,
Medical, Medical Plan, Medical Tier, Medical Election,
Dental, Vision,
Health FSA, HFSA, FSA Medical, FSA Election, Health FSA Annual,
DCFSA, DCAP, Dependent Care, Dependent Care FSA, DCFSA Annual,
HSA, HSA Contribution, HSA Annual,
COBRA, COBRA Status,
Citizenship, Visa Status, NRA
```

### 2.2 Payroll YTD Report

**What it is:** Year-to-date payroll summary showing compensation and deduction breakdowns per employee.

**Common formats:** CSV, XLSX, PDF

**Data points it contains:**
- Employee identifier (name, SSN, employee ID)
- Gross compensation
- Pre-tax deductions by type (medical premium, dental premium, vision premium, Health FSA, DCFSA, HSA)
- Employer contributions by type (medical, dental, vision, HSA, FSA)
- Hours worked YTD

**Questions it eliminates (beyond census):**
- O: o_hcp_employee_reductions, o_hcp_employer_contributions, o_non_hci_employee_reductions, o_non_hci_employer_contributions
- P: p_key_employee_reductions, p_key_employer_contributions
- U: u_owner_election_total, u_spouse_dependent_election_total, u_total_dcap_elections
- V: v_hce_election_total, v_non_hce_election_total
- I: i_under_25k_compensation

**Example column headers to detect:**
```
Employee, EE Name, Employee ID, SSN,
YTD Gross, Gross Pay, Total Compensation,
Medical EE, Medical ER, Dental EE, Dental ER, Vision EE, Vision ER,
FSA Medical, Health FSA, HFSA Deduction,
DCFSA, DCAP, Dependent Care Deduction,
HSA EE, HSA ER, HSA Deduction,
Total Pre-Tax, Sec 125 Deductions,
ER Medical, ER Dental, ER Vision, Employer HSA,
Hours YTD, Total Hours
```

### 2.3 Monthly Insurance Billing / Carrier Invoice

**What it is:** Monthly premium billing from the insurance carrier showing enrolled employees, plan tiers, and premium splits.

**Common formats:** PDF (most common), XLSX, CSV

**Data points it contains:**
- Enrolled employees by plan
- Plan tier (EE only, EE+Spouse, EE+Children, Family)
- Total premium per tier
- Employee contribution per tier
- Employer contribution per tier
- COBRA participants

**Questions it eliminates:**
- O: o_safe_harbor_health (can compute: does employer pay >= 75% of most expensive option?)
- G: g_cobra (COBRA participants listed separately)
- Enrollment confirmation for who is participating

**Example column headers to detect:**
```
Subscriber, Member Name, Employee,
Plan, Plan Name, Coverage Level, Tier,
Total Premium, Employee Premium, Employer Premium,
EE Only, EE+SP, EE+CH, Family,
COBRA, Coverage Type
```

### 2.4 Ownership / Officer List

**What it is:** A list of owners, officers, and their ownership percentages and compensation.

**Common formats:** CSV, XLSX, manually entered table

**Data points it contains:**
- Owner/officer name (linkable to census by name or SSN)
- Ownership percentage
- Ownership type (direct, indirect, family attribution)
- Officer title
- Compensation (current and prior year)
- Family relationships (spouse of, child of)

**Questions it eliminates:**
- J: j_owners_5pct, j_officers_non_owners, j_spouse_dependent_count, j_total_hci
- K: k_owners_10pct, k_top5_officers_non_owners, k_total_105h_hci
- L: l_owners_5pct, l_total_hce
- M: m_owners_5pct, m_owners_1pct_150k, m_officers_above_threshold, m_total_key_employees

**Example column headers to detect:**
```
Name, Owner Name, Officer Name,
Ownership %, Percent Owned, Ownership Percentage,
Title, Officer Title, Position,
Compensation, Annual Comp, Prior Year Comp,
Type, Direct/Indirect, Attribution,
Related To, Spouse Of, Family Relationship
```

### 2.5 Related Entity List

**What it is:** A list of companies in the controlled group or affiliated service group.

**Common formats:** CSV, XLSX, manually entered table

**Data points it contains:**
- Entity name
- Entity type (C-Corp, S-Corp, LLC, etc.)
- Ownership relationship to plan sponsor
- Employee count
- Whether employees can participate in the cafeteria plan
- HCI/Key Employee/HCE counts at each entity

**Questions it eliminates:**
- B: b_controlled_group, b_non_covered_entities, b_non_covered_entity_names, b_non_covered_entity_employee_count
- J: j_hci_non_covered_entities
- K: k_hci_non_covered_entities
- L: l_hce_non_covered_entities
- M: m_key_non_covered_entities

**Example column headers to detect:**
```
Entity Name, Company Name,
Entity Type, Legal Structure,
Relationship, Ownership %,
Employee Count, Total Employees, Headcount,
Plan Eligible, Covered, Participates in Plan
```

### 2.6 Plan Document Summary / SPD

**What it is:** The Summary Plan Description or a plan design summary. Typically a PDF document that cannot be parsed programmatically in a reliable way.

**Common formats:** PDF (almost always), occasionally Word

**Data points it contains:**
- Eligibility rules (waiting period, age, hours)
- Benefits offered
- Contribution formulas
- Maximum benefit levels
- Employee classes and their rules

**Why it is NOT a good upload target for automation:**
PDF plan documents are unstructured prose. They vary wildly in format, structure, and terminology across law firms and TPA platforms. Reliable automated extraction would require natural language understanding well beyond column-header matching. These data points are better captured as direct questions.

**Recommendation:** Do NOT attempt to parse SPDs. Keep plan design questions as manual-entry fields. However, consider offering an optional SPD upload for reference/audit purposes.

---

## 3. Minimal Question Set

### Phase 1: Setup (Always Required) -- 6 questions

These questions cannot be answered by any file and must always be asked:

| # | Field Key | Question | Type | Why Always Required |
|---|-----------|----------|------|-------------------|
| 1 | a_employer_legal_name | Employer legal name | TEXT | Identity -- no file provides this in the right context |
| 2 | a_plan_year_end_date | Plan year end date | DATE | Defines the testing period for all calculations |
| 3 | a_entity_type | Entity type | SELECT | Determines which ownership rules apply |
| 4 | d_benefits_offered | Benefits offered under the plan | CHECKBOX | Gates which tests apply (controls entire flow) |
| 5 | d_benefits_other_describe | Other benefits (if selected) | TEXT | Conditional on "Other" |
| 6 | f_simple_cafeteria | Has employer adopted Simple Cafeteria Plan? | RADIO | If Yes + qualifies, skips ALL testing |

### Phase 2: File Uploads (Optional But Recommended) -- 0-5 uploads

Present upload zones for these file types. Each is optional. The system detects columns and shows the user what it found before proceeding.

| # | Upload | Priority | What It Replaces |
|---|--------|----------|-----------------|
| 1 | Employee Census with Benefits Enrollment | HIGH | ~30-35 numeric fields across Sections C, G, H, I, N, Q, S |
| 2 | Payroll YTD Report | HIGH | ~10-12 dollar-amount fields across Sections O, P, U, V |
| 3 | Ownership / Officer List | MEDIUM | ~15 fields across Sections J, K, L, M |
| 4 | Related Entity List | LOW | ~5 fields in Section B + non-covered entity counts |
| 5 | Carrier Invoice | LOW | Confirms enrollment, helps with 75% safe harbor calculation |

### Phase 3: Gap-Fill Questions (Only If Corresponding Upload Missing)

These questions ONLY appear if the file that would have answered them was not uploaded or could not be parsed.

**If NO Census uploaded (adds ~20 questions):**
- c_total_employees
- c_cba_covered, c_cba_employee_count
- g_under_service_requirement, g_under_minimum_age, g_part_time_seasonal, g_nonresident_alien, g_cobra
- h_under_3_years_service, h_under_age_25, h_under_35_hours, h_nonresident_alien (if FSA offered)
- i_under_age_21, i_under_1_year_service, i_nonresident_alien, i_under_25k_compensation (if DCFSA offered)
- n_total_non_excludable, n_non_hci_eligible
- q_employees_benefiting, q_total_non_excludable, q_eligible_to_benefit, q_105h_hci_benefiting, q_105h_hci_non_excludable, q_non_hci_benefiting, q_non_hci_non_excludable (if FSA offered)
- s_total_non_excludable, s_non_hce_eligible (if DCFSA offered)

**If NO Payroll uploaded (adds ~10 questions):**
- o_hcp_employee_reductions, o_hcp_employer_contributions, o_non_hci_employee_reductions, o_non_hci_employer_contributions
- p_key_employee_reductions, p_key_employer_contributions
- u_owner_election_total, u_spouse_dependent_election_total, u_total_dcap_elections (if DCFSA offered)
- v_hce_election_total, v_non_hce_election_total (if DCFSA offered)

**If NO Ownership List uploaded (adds ~12 questions):**
- j_owners_5pct, j_officers_non_owners, j_high_comp_non_owner_officer, j_total_hci
- k_owners_10pct, k_top5_officers_non_owners, k_top25pct_non_owner_officer, k_total_105h_hci (if FSA offered)
- l_owners_5pct, l_high_comp_non_owners, l_total_hce (if DCFSA offered)
- m_owners_5pct, m_owners_1pct_150k, m_officers_above_threshold, m_total_key_employees

**If NO Related Entity List uploaded (adds ~4 questions):**
- b_controlled_group, b_non_covered_entities, b_non_covered_entity_names, b_non_covered_entity_employee_count

### Phase 4: Plan Design & Judgment Questions (Always Required -- No File Can Answer These)

These questions must always be asked because they require knowledge of the plan document or human judgment:

**Controlled Group / CBA (always):**
- b_controlled_group (unless Related Entity file uploaded)
- c_cba_covered (unless census has union column)
- c_cba_good_faith (JUDGMENT -- always ask if CBA employees exist)
- c_cba_eligible (PLAN DESIGN -- always ask if CBA employees exist)

**Plan Eligibility Rules (always -- from plan document):**
- e_waiting_period, e_waiting_period_length
- e_minimum_age, e_minimum_age_value
- e_hours_requirement, e_hours_requirement_value
- e_eligibility_varies, e_eligibility_varies_describe

**Simple Cafeteria Plan (conditional on f_simple_cafeteria = Yes):**
- f_100_or_fewer, f_contribution_requirement, f_eligibility_requirement

**Section 125 Eligibility (always):**
- n_available_to_all, n_classification_description (if not open equally)

**Section 125 Contributions & Benefits (if not POP-only):**
- o_same_benefits_all, o_benefits_differ_describe (if not same)
- o_safe_harbor_health

**HCI Spouse/Dependent (JUDGMENT -- always):**
- j_spouse_dependent_exists, j_spouse_dependent_count

**Section 105(h) Benefits Test (if FSA offered -- all PLAN DESIGN):**
- r_same_expenses, r_expenses_differ_describe
- r_same_maximum, r_maximum_differ_describe
- r_same_cost_sharing, r_cost_sharing_differ_describe
- r_same_waiting_periods, r_waiting_differ_describe
- r_same_dependent_coverage, r_dependent_differ_describe
- r_executive_physicals
- r_benefit_changes, r_benefit_changes_describe

**Section 105(h) Eligibility (if FSA offered):**
- q_classification_description

**Section 129 Tests (if DCFSA offered):**
- s_available_to_all, s_classification_description
- t_same_maximum, t_maximum_differ_describe
- t_same_terms, t_terms_differ_describe
- t_nonelective_contributions, t_nonelective_same_terms, t_nonelective_describe
- u_spouse_dependent_participate (JUDGMENT)

**Attestation (always):**
- x_notes, x_attestation

### Phase 5: Review & Confirm (Calculated Results Shown for Verification)

After parsing uploads, display a confirmation screen showing:

1. **Workforce Summary:** "We found X total employees, Y active during the plan year, Z terminated."
2. **Excludable Employee Counts:** Show the computed counts for each exclusion category with the rules applied.
3. **HCI/HCE/Key Employee Identification:** "Based on the ownership list and compensation data, we identified X HCIs, Y Key Employees, Z HCEs."
4. **Election Totals:** "Total pre-tax salary reductions: $X for HCIs, $Y for non-HCIs."
5. **Test Population Sizes:** Show the non-excludable populations for each test section.

Each computed value has a "Confirm" checkbox and an "Override" option to manually correct.

---

## 4. Proposed Flow (Page by Page)

### Page 1: Entity & Plan Setup
**Title:** "About the Employer and Plan"
**Contents:**
- Employer legal name (text)
- Plan year end date (date picker)
- Entity type (dropdown)
- Benefits offered under the plan (checkboxes)
- Other benefits description (conditional on "Other")

**Conditions:** Always shown.
**Estimated time:** 1-2 minutes.

### Page 2: Simple Cafeteria Plan Check
**Title:** "Simple Cafeteria Plan Safe Harbor"
**Contents:**
- Has employer adopted Simple Cafeteria Plan? (radio: Yes/No/Unsure)
- If Yes: 100 or fewer employees? Contribution requirement met? All 1,000+ hour employees eligible?
- If all Yes: Display "Your plan qualifies for the Simple Cafeteria Plan safe harbor. No further testing is required." and skip to attestation.

**Conditions:** Always shown.
**Estimated time:** 30 seconds (if No/Unsure, moves on). If Yes and qualifies, questionnaire ends here.

### Page 3: File Uploads
**Title:** "Upload Your Data Files"
**Contents:**
- Introductory text: "Uploading files dramatically reduces the number of questions. We can accept any of the following:"
- Upload zone 1: Employee Census / Benefits Enrollment (CSV or Excel) -- "Recommended -- eliminates ~30 questions"
- Upload zone 2: Payroll YTD Report (CSV or Excel) -- "Recommended -- eliminates ~10 questions"
- Upload zone 3: Ownership & Officer List (CSV or Excel) -- "Eliminates ~12 questions"
- Upload zone 4: Related Entity List (CSV or Excel) -- "Eliminates ~4 questions"
- Upload zone 5: Carrier Invoice (PDF, CSV, or Excel) -- "Helps verify enrollment data"
- "Skip uploads" link at bottom

**Conditions:** Always shown (unless Simple Cafeteria Plan safe harbor was met).
**Estimated time:** 2-5 minutes (finding and uploading files). Or 5 seconds if skipped.

### Page 4: Column Mapping Confirmation (per upload)
**Title:** "Confirm Column Mapping"
**Contents:**
- For each uploaded file, show a table of detected columns mapped to data fields
- Auto-mapped columns shown with green checkmarks
- Unrecognized columns shown with dropdown to manually map
- Preview of first 5 rows of parsed data
- "Looks good" / "Re-upload" buttons

**Conditions:** Shown only if files were uploaded. One sub-page per file.
**Estimated time:** 1-2 minutes per file.

### Page 5: Parsed Data Confirmation
**Title:** "Verify Computed Values"
**Contents:**
- Summary cards showing all computed values grouped by test:
  - Workforce: total employees, CBA count, excludable counts
  - HCI/Key Employee/HCE identification results
  - Election and contribution totals
- Each value shows the computed number with a "Confirm" toggle (default: confirmed)
- "Override" link next to each value opens an inline edit field
- Discrepancy warnings (e.g., "Total HCIs seems high -- please verify")

**Conditions:** Shown only if files were uploaded and parsed.
**Estimated time:** 2-3 minutes (review and confirm).

### Page 6: Controlled Group & Related Entities
**Title:** "Related Companies"
**Contents:**
- Is the employer part of a controlled group? (radio)
- If Yes and no Related Entity file uploaded: entity names, employee counts at non-covered entities
- If Related Entity file was uploaded: show parsed entities for confirmation

**Conditions:** Always shown (unless Simple Cafeteria Plan safe harbor).
**Estimated time:** 1-2 minutes.

### Page 7: Workforce & CBA
**Title:** "Workforce and Union Status"
**Contents:**
- If census NOT uploaded: total employees, CBA-covered employee count
- If census uploaded: show computed values for confirmation
- CBA good-faith bargaining question (always if CBA employees exist)
- CBA eligibility for cafeteria plan (always if CBA employees exist)

**Conditions:** Always shown. Content adapts based on uploads.
**Estimated time:** 1-2 minutes.

### Page 8: Plan Eligibility Rules
**Title:** "Eligibility Conditions"
**Contents:**
- Waiting period? (radio) + length (dropdown, conditional)
- Minimum age? (radio) + value (number, conditional)
- Hours requirement? (radio) + value (dropdown, conditional)
- Eligibility varies by class? (radio) + description (textarea, conditional)

**Conditions:** Always shown. These are always PLAN DESIGN questions.
**Estimated time:** 2-3 minutes.

### Page 9: Gap-Fill -- Excludable Employees
**Title:** "Excludable Employee Counts"
**Contents:**
- Only questions for data NOT provided by uploads
- Section 125 excludables (if no census): under service, under age, part-time, NRA, COBRA
- Section 105(h) excludables (if no census AND FSA offered): under 3 years, under 25, under 35 hrs
- Section 129 excludables (if no census AND DCFSA offered): under 21, under 1 year, under $25K

**Conditions:** Shown only if census was NOT uploaded (or parsing missed these fields).
**Estimated time:** 3-5 minutes if shown; skipped entirely with census upload.

### Page 10: Gap-Fill -- HCI / Key Employee / HCE Identification
**Title:** "Highly Compensated & Key Employee Counts"
**Contents:**
- Only questions for data NOT provided by ownership list or census
- Section 125 HCIs: owners, officers, high earners, spouse/dependents
- Section 105(h) HCIs (if FSA): 10% owners, top-5 officers, top-25% earners
- Section 129 HCEs (if DCFSA): 5% owners, high earners
- Key Employees: 5% owners, 1-5% owners >$150K, officers >$230K
- Non-covered entity counts for each category

**Conditions:** Shown only if ownership list was NOT uploaded.
**Estimated time:** 5-8 minutes if shown; reduced to 1 question (spouse/dependent) with ownership upload.

### Page 11: Section 125 Eligibility Test
**Title:** "Section 125 Eligibility"
**Contents:**
- Plan open to all eligible employees equally? (radio)
- If No: classification description (textarea)
- If no census: non-excludable count, non-HCI eligible count
- If census uploaded: show computed values for confirmation

**Conditions:** Always shown.
**Estimated time:** 1-2 minutes.

### Page 12: Section 125 Contributions & Benefits Test
**Title:** "Section 125 Contributions & Benefits"
**Contents:**
- Same benefits offered to everyone equally? (radio)
- If No: describe differences
- 75% safe harbor met? (radio)
- If no payroll: HCP count, non-HCI count, all salary reduction and employer contribution dollar amounts
- If payroll uploaded: show computed values for confirmation

**Conditions:** Shown only if benefits include more than just POP.
**Estimated time:** 2-5 minutes depending on uploads.

### Page 13: Section 125 Key Employee Concentration Test
**Title:** "Key Employee Concentration"
**Contents:**
- If no payroll: key employee benefit election totals
- If payroll uploaded: show computed values for confirmation

**Conditions:** Shown only if benefits include more than just POP.
**Estimated time:** 1-3 minutes depending on uploads.

### Page 14: Section 105(h) Eligibility Test
**Title:** "Health FSA Eligibility (Section 105(h))"
**Contents:**
- Eligibility classification description
- If no census: all numeric population counts
- If census uploaded: show computed values for confirmation

**Conditions:** Shown only if Health FSA or LP-FSA is offered.
**Estimated time:** 2-4 minutes depending on uploads.

### Page 15: Section 105(h) Benefits Test
**Title:** "Health FSA Benefits Design (Section 105(h))"
**Contents:**
- Same expenses? Same maximum? Same cost-sharing? Same waiting periods? Same dependent coverage?
- Executive physicals? Benefits changed mid-year?
- Conditional description fields for each "No" answer

**Conditions:** Shown only if Health FSA or LP-FSA is offered.
**Estimated time:** 3-5 minutes. All PLAN DESIGN questions -- uploads cannot help here.

### Page 16: Section 129 Eligibility Test
**Title:** "Dependent Care FSA Eligibility (Section 129)"
**Contents:**
- DCFSA open to all equally? (radio)
- If No: classification description
- If no census: non-excludable count, non-HCE eligible count
- If census uploaded: show computed values for confirmation

**Conditions:** Shown only if DCFSA is offered.
**Estimated time:** 1-2 minutes.

### Page 17: Section 129 Benefits & Contributions Test
**Title:** "Dependent Care FSA Benefits & Contributions"
**Contents:**
- Same maximum? Same terms?
- Employer contributes directly? If yes: same for everyone?
- Conditional description fields

**Conditions:** Shown only if DCFSA is offered.
**Estimated time:** 2-3 minutes. All PLAN DESIGN questions.

### Page 18: Section 129 Owners & Average Benefits Tests
**Title:** "Dependent Care -- Owner Concentration & Average Benefits"
**Contents:**
- If no payroll: owner election totals, spouse/dependent elections, total elections, HCE/non-HCE election splits
- If payroll uploaded: show computed values for confirmation
- Spouse/dependent participation question (always -- JUDGMENT)

**Conditions:** Shown only if DCFSA is offered.
**Estimated time:** 2-4 minutes depending on uploads.

### Page 19: Notes & Attestation
**Title:** "Final Review"
**Contents:**
- Additional notes (textarea, optional)
- Summary of all test results (pass/fail/needs-review)
- Attestation checkbox

**Conditions:** Always shown.
**Estimated time:** 1-2 minutes.

### Total Flow Summary

| Scenario | Pages Shown | Estimated Time |
|----------|------------|---------------|
| Simple Cafeteria Plan qualifies | 2 pages | 2-3 minutes |
| POP-only plan, all uploads | ~10 pages | 10-15 minutes |
| POP-only plan, no uploads | ~12 pages | 20-30 minutes |
| Full plan (FSA + DCFSA), all uploads | ~16 pages | 15-25 minutes |
| Full plan (FSA + DCFSA), no uploads | ~18 pages | 35-50 minutes |

---

## 5. File Parsing Skills Needed

### 5.1 Census / Enrollment Parser

**Input formats:** CSV, XLSX (first sheet or named sheet)

**Column detection strategy:**
1. Read header row (row 1, or row 2 if row 1 looks like a title).
2. Normalize headers: lowercase, strip whitespace/punctuation, collapse synonyms.
3. Match against a synonym dictionary:
   - DOB synonyms: "date of birth", "dob", "birth date", "birthdate", "birth_date"
   - Hire date synonyms: "hire date", "original hire date", "hire_date", "date hired", "start date"
   - Compensation synonyms: "salary", "annual salary", "compensation", "annual comp", "ytd gross", "annual pay"
   - Hours synonyms: "hours", "annual hours", "hours worked", "ytd hours", "weekly hours"
   - FSA synonyms: "health fsa", "hfsa", "fsa medical", "medical fsa", "health fsa election", "fsa annual"
   - DCFSA synonyms: "dcfsa", "dcap", "dependent care", "dependent care fsa", "dc fsa"
   - Status synonyms: "status", "employment status", "active/terminated", "ee status"
   - Class synonyms: "class", "employee class", "job category", "classification", "ee class", "employee type"
4. For unmatched columns, present them to the user in the Column Mapping Confirmation page.

**Output data points:**
```json
{
  "total_employees": 150,
  "active_during_plan_year": 142,
  "terminated_during_plan_year": 8,
  "cobra_participants": 3,
  "cba_covered": 12,
  "excludable_125": {
    "under_service_req": 5,
    "under_minimum_age": 2,
    "part_time_seasonal": 18,
    "nonresident_alien": 0,
    "cobra": 3
  },
  "excludable_105h": {
    "under_3_years": 22,
    "under_age_25": 8,
    "under_35_hours": 20,
    "nonresident_alien": 0
  },
  "excludable_129": {
    "under_age_21": 3,
    "under_1_year": 10,
    "nonresident_alien": 0,
    "under_25k": 15
  },
  "elections": {
    "medical_enrolled": 120,
    "health_fsa_enrolled": 45,
    "dcfsa_enrolled": 22,
    "hsa_enrolled": 30
  },
  "compensation_ranked": [ /* ordered list for top-25% calc */ ]
}
```

**Edge cases:**
- Multiple sheets in Excel (ask user which sheet or scan all for header patterns)
- Merged header rows (look for data starting at row 3+)
- Date format ambiguity (MM/DD/YYYY vs. DD/MM/YYYY -- default to US format, flag if ambiguous)
- Hours as weekly vs. annual (detect from magnitude: <60 = weekly, >200 = annual)
- Compensation as hourly rate vs. annual (detect from magnitude: <200 = hourly rate, needs hours to annualize)
- Status values vary wildly ("A", "Active", "T", "Terminated", "LOA", "COBRA", "1", "0")
- Missing columns: flag what could not be extracted, generate gap-fill questions for those fields only

**Confidence scoring:**
- HIGH (auto-accept): Column header matches synonym exactly, data type validates (dates are dates, numbers are numbers), row count > 10
- MEDIUM (show confirmation): Column header is a partial match, or data has some parse errors (<5%)
- LOW (ask user): Column header unrecognized, data type mismatches > 5%, or fewer than 5 data rows

### 5.2 Payroll YTD Parser

**Input formats:** CSV, XLSX

**Column detection strategy:**
Same synonym-matching approach as census. Key difference: payroll files have dollar amounts in most columns and often have multiple deduction columns.

Detection heuristic for deduction type:
1. Look for columns with "$" or numeric values in the 100-10,000 range
2. Match column header against benefit-type synonyms
3. Distinguish EE (employee) vs. ER (employer) deductions by header keywords: "ee", "employee", "er", "employer", "company"

**Output data points:**
```json
{
  "employee_deductions": {
    "medical_premium_total": 245000,
    "dental_premium_total": 32000,
    "vision_premium_total": 12000,
    "health_fsa_total": 112500,
    "dcfsa_total": 55000,
    "hsa_total": 75000
  },
  "employer_contributions": {
    "medical_total": 890000,
    "dental_total": 48000,
    "vision_total": 18000,
    "hsa_total": 37500
  },
  "by_hci_status": {
    "hci_salary_reductions": 45000,
    "hci_employer_contributions": 62000,
    "non_hci_salary_reductions": 432000,
    "non_hci_employer_contributions": 931500
  },
  "by_key_employee_status": { /* similar split */ },
  "by_hce_status": { /* similar split for Section 129 */ }
}
```

Note: The "by_hci_status" split requires cross-referencing with the ownership/officer list or census. If no ownership file is uploaded, the payroll parser cannot split by HCI status, and those questions must still be asked manually.

**Edge cases:**
- YTD vs. single-period payroll (detect by checking if values seem like annual or per-period amounts)
- Negative amounts (adjustments, corrections) -- include in totals
- Multiple deduction columns for the same benefit type (e.g., "Medical Pre-Tax" and "Medical Catchup")
- Employer contributions may not be on the same report (may need carrier invoice instead)

**Confidence scoring:**
- HIGH: Dollar amounts sum correctly, employee count matches census (if both uploaded)
- MEDIUM: Some employees have zero or missing values; totals look reasonable but no cross-reference
- LOW: Amounts look like per-period not YTD, or significant mismatches

### 5.3 Ownership / Officer Parser

**Input formats:** CSV, XLSX

**Column detection strategy:**
Look for columns with names, percentages, titles, and compensation. This is typically a small file (5-20 rows).

**Output data points:**
```json
{
  "owners": [
    {
      "name": "John Smith",
      "ownership_pct": 51.0,
      "is_officer": true,
      "officer_title": "President",
      "compensation_current": 250000,
      "compensation_prior": 240000,
      "family_relationships": ["Jane Smith (spouse)"]
    }
  ],
  "computed": {
    "owners_5pct": 2,
    "owners_10pct": 1,
    "owners_1to5_over_150k": 1,
    "officers_total": 4,
    "officers_above_230k": 2,
    "total_125_hci": 8,
    "total_105h_hci": 5,
    "total_129_hce": 4,
    "total_key_employees": 5
  }
}
```

**Edge cases:**
- Family attribution: if the file includes a "Related To" column, apply attribution rules automatically. If not, ask the user.
- Indirect ownership: may not be calculable from a flat file. Flag and ask.
- Multiple entities: ownership at different entities in the controlled group. Need to map to entity names.

**Confidence scoring:**
- HIGH: Clean data with percentages, names match census, clear officer flags
- MEDIUM: Percentages present but no family relationship data
- LOW: Missing compensation data, no clear officer identification

### 5.4 Related Entity Parser

**Input formats:** CSV, XLSX

**Column detection strategy:**
Simple file -- typically 2-10 rows with entity name, type, relationship, and employee count.

**Output data points:**
```json
{
  "entities": [
    {
      "name": "Smith Holdings LLC",
      "type": "LLC / Partnership",
      "relationship": "Parent",
      "employee_count": 25,
      "plan_eligible": false
    }
  ],
  "computed": {
    "is_controlled_group": true,
    "non_covered_entities": ["Smith Holdings LLC"],
    "non_covered_employee_count": 25
  }
}
```

**Edge cases:**
- Minimal: this is usually a very simple file
- May need to ask which entities' employees are eligible for the plan

**Confidence scoring:**
- HIGH: All columns present and populated
- MEDIUM: Missing employee counts (can ask as gap-fill)
- LOW: Only entity names, no other data

### 5.5 Carrier Invoice Parser

**Input formats:** PDF (most common), CSV, XLSX

**Column detection strategy for CSV/XLSX:** Standard synonym matching.
**For PDF:** Use tabular PDF extraction (e.g., tabula-style line detection). Look for premium tables with tier/rate structure.

**Output data points:**
```json
{
  "plans": [
    {
      "plan_name": "BlueCross PPO",
      "tiers": {
        "ee_only": { "total": 650, "employee": 100, "employer": 550 },
        "ee_spouse": { "total": 1400, "employee": 350, "employer": 1050 },
        "ee_children": { "total": 1200, "employee": 250, "employer": 950 },
        "family": { "total": 1800, "employee": 500, "employer": 1300 }
      },
      "employer_pct_of_most_expensive": 72.2
    }
  ],
  "enrolled_employees": 120,
  "cobra_participants": 3,
  "safe_harbor_75pct_met": false
}
```

**Edge cases:**
- PDF parsing is inherently unreliable; always present results for user confirmation
- Multiple carriers/plans on different invoices
- Composite rates vs. age-banded rates
- Self-funded plans may not have a traditional invoice

**Confidence scoring:**
- HIGH: CSV/XLSX with clear premium columns
- MEDIUM: PDF parsed successfully but some values uncertain
- LOW: PDF parsing failed or data looks incomplete

---

## 6. Question Count Comparison

### Current Questionnaire Field Inventory

| Section | Section Name | Fields | Conditional? |
|---------|-------------|--------|-------------|
| A | Entity & Plan Context | 3 | No |
| B | Controlled Group | 4 | Fields 3-4 conditional on b_controlled_group=Yes |
| C | Workforce Overview | 5 | Fields 3-5 conditional on c_cba_covered=Yes |
| D | Benefits Offered | 2 | Field 2 conditional on "Other" |
| F | Simple Cafeteria Plan | 4 | Fields 2-4 conditional on f_simple_cafeteria=Yes |
| E | Plan Eligibility Conditions | 8 | Several fields conditional on Yes answers |
| G | Excludable Employees (125) | 5 | No |
| J | HCI Classification (125) | 7 | Fields 5-6 conditional on j_spouse_dependent_exists=Yes |
| N | Section 125 Eligibility | 4 | Fields 2-4 conditional on n_available_to_all=No |
| O | Section 125 Contributions & Benefits | 9 | Conditional on non-POP benefits; field 2 conditional |
| M | Key Employee Classification | 5 | Conditional on non-POP benefits |
| P | Key Employee Concentration | 3 | Conditional on non-POP benefits |
| H | Excludable Employees (105(h)) | 4 | Conditional on FSA offered |
| K | HCI Classification (105(h)) | 5 | Conditional on FSA offered |
| Q | Section 105(h) Eligibility | 8 | Conditional on FSA offered |
| R | Section 105(h) Benefits | 12 | Conditional on FSA offered; many sub-conditionals |
| I | Excludable Employees (129) | 4 | Conditional on DCFSA offered |
| L | HCE Classification (129) | 4 | Conditional on DCFSA offered |
| S | Section 129 Eligibility | 4 | Conditional on DCFSA offered |
| T | Section 129 Benefits & Contributions | 7 | Conditional on DCFSA offered; many sub-conditionals |
| U | Section 129 Owners Test | 5 | Conditional on DCFSA offered |
| V | Section 129 Average Benefits | 4 | Conditional on DCFSA offered |
| X | Notes & Attestation | 2 | No |
| **TOTAL** | | **112 fields** | |

Note: The raw field count is 112 but many are conditional (shown only when a parent question is answered a certain way). In a typical full-plan scenario with all benefits offered, a user encounters roughly **80-91 fields** depending on their answers. In a POP-only scenario, roughly **45-55 fields**.

### Comparison Table

| Scenario | Questions User Must Answer | Pages | Est. Time |
|----------|--------------------------|-------|-----------|
| **Current approach (POP only)** | ~45-55 | 19 sections | 30-45 min |
| **Current approach (all benefits)** | ~80-91 | 19 sections | 45-75 min |
| **Redesigned, no uploads (POP only)** | ~28 | ~10 pages | 15-20 min |
| **Redesigned, no uploads (all benefits)** | ~55 | ~16 pages | 30-45 min |
| **Redesigned + census upload (POP only)** | ~15 | ~8 pages | 8-12 min |
| **Redesigned + census upload (all benefits)** | ~35 | ~14 pages | 20-30 min |
| **Redesigned + census + payroll (POP only)** | ~12 | ~7 pages | 6-10 min |
| **Redesigned + census + payroll (all benefits)** | ~25 | ~13 pages | 15-22 min |
| **Redesigned + all uploads (POP only)** | ~10 | ~6 pages | 5-8 min |
| **Redesigned + all uploads (all benefits)** | ~18 | ~10 pages | 10-15 min |
| **Simple Cafeteria Plan qualifies** | ~8 | 2 pages | 2-3 min |

### Key Reductions Explained

**Why "no uploads" redesign still reduces questions from ~91 to ~55:**
1. Better conditional logic: current form shows all 19 sections. Redesigned flow skips entire pages when benefits are not offered.
2. Computed/sum fields eliminated: fields like j_total_hci, k_total_105h_hci, l_total_hce, m_total_key_employees are auto-calculated sums that the user should never type manually.
3. Duplicate nonresident-alien counts across Sections G, H, I consolidated: ask once, apply to each test.
4. Simple Cafeteria Plan short-circuit: if qualified, entire questionnaire collapses to 8 questions.

**Why census upload has the biggest impact (~20-30 questions eliminated):**
The census file provides DOB, hire date, hours, compensation, enrollment status, and class for every employee. This lets the system compute all excludable-employee counts, all population sizes, and most participation counts automatically.

**Why payroll upload has the second biggest impact (~10 questions eliminated):**
Dollar-amount questions (salary reductions, employer contributions, election totals) are the most error-prone fields in the current questionnaire. Users frequently confuse employee vs. employer amounts, or include the wrong deduction categories. Parsing payroll eliminates these errors entirely.

**Irreducible minimum (~10-18 questions depending on benefits):**
Even with all files uploaded, these questions can never be eliminated:
- Entity identity (name, type, plan year) -- 3 questions
- Benefits offered -- 1 question
- Simple cafeteria plan check -- 1 question
- Plan eligibility rules (waiting period, age, hours, class variation) -- 4-8 questions
- Plan design fairness (same benefits for all? same maximums?) -- 4-12 questions depending on benefits
- HCI spouse/dependent identification -- 1 question (JUDGMENT)
- Attestation -- 1 question

---

## Appendix A: Field-to-Upload Mapping Reference

This table shows every field in the current questionnaire and whether it can be replaced by a file upload.

| Field Key | Current Section | Can Be Replaced By | Replacement Upload |
|-----------|----------------|--------------------|--------------------|
| a_employer_legal_name | A | NO | -- |
| a_plan_year_end_date | A | NO | -- |
| a_entity_type | A | NO | -- |
| b_controlled_group | B | YES | Related Entity List |
| b_non_covered_entities | B | YES | Related Entity List |
| b_non_covered_entity_names | B | YES | Related Entity List |
| b_non_covered_entity_employee_count | B | YES | Related Entity List |
| c_total_employees | C | YES | Census |
| c_cba_covered | C | YES (partial) | Census (if union column exists) |
| c_cba_good_faith | C | NO (JUDGMENT) | -- |
| c_cba_employee_count | C | YES | Census |
| c_cba_eligible | C | NO (PLAN DESIGN) | -- |
| d_benefits_offered | D | NO | -- |
| d_benefits_other_describe | D | NO | -- |
| f_simple_cafeteria | F | NO (PLAN DESIGN) | -- |
| f_100_or_fewer | F | YES | Census (count over 2 prior years) |
| f_contribution_requirement | F | NO (PLAN DESIGN) | -- |
| f_eligibility_requirement | F | NO (PLAN DESIGN) | -- |
| e_waiting_period | E | NO (PLAN DESIGN) | -- |
| e_waiting_period_length | E | NO (PLAN DESIGN) | -- |
| e_minimum_age | E | NO (PLAN DESIGN) | -- |
| e_minimum_age_value | E | NO (PLAN DESIGN) | -- |
| e_hours_requirement | E | NO (PLAN DESIGN) | -- |
| e_hours_requirement_value | E | NO (PLAN DESIGN) | -- |
| e_eligibility_varies | E | NO (PLAN DESIGN) | -- |
| e_eligibility_varies_describe | E | NO (PLAN DESIGN) | -- |
| g_under_service_requirement | G | YES | Census (hire date vs. plan rules) |
| g_under_minimum_age | G | YES | Census (DOB vs. plan rules) |
| g_part_time_seasonal | G | YES | Census (hours) |
| g_nonresident_alien | G | YES (partial) | Census (citizenship column) |
| g_cobra | G | YES | Census or Carrier Invoice |
| j_owners_5pct | J | YES | Ownership List |
| j_officers_non_owners | J | YES | Ownership List |
| j_high_comp_non_owner_officer | J | YES | Ownership List + Payroll |
| j_spouse_dependent_exists | J | NO (JUDGMENT) | -- |
| j_spouse_dependent_count | J | YES (partial) | Ownership List (if family column) |
| j_total_hci | J | COMPUTED | Sum of above |
| j_hci_non_covered_entities | J | YES (partial) | Ownership + Related Entity |
| n_available_to_all | N | NO (PLAN DESIGN) | -- |
| n_classification_description | N | NO (PLAN DESIGN) | -- |
| n_total_non_excludable | N | YES | Census (computed) |
| n_non_hci_eligible | N | YES | Census + Ownership |
| o_same_benefits_all | O | NO (PLAN DESIGN) | -- |
| o_benefits_differ_describe | O | NO (PLAN DESIGN) | -- |
| o_safe_harbor_health | O | YES (partial) | Carrier Invoice |
| o_hcp_count | O | YES | Census + Ownership |
| o_non_hci_participant_count | O | YES | Census + Ownership |
| o_hcp_employee_reductions | O | YES | Payroll + Ownership |
| o_hcp_employer_contributions | O | YES | Payroll + Ownership |
| o_non_hci_employee_reductions | O | YES | Payroll + Ownership |
| o_non_hci_employer_contributions | O | YES | Payroll + Ownership |
| m_owners_5pct | M | YES | Ownership List |
| m_owners_1pct_150k | M | YES | Ownership List + Payroll |
| m_officers_above_threshold | M | YES | Ownership List + Payroll |
| m_total_key_employees | M | COMPUTED | Sum of above |
| m_key_non_covered_entities | M | YES (partial) | Ownership + Related Entity |
| p_key_participant_count | P | YES | Ownership + Census |
| p_key_employee_reductions | P | YES | Ownership + Payroll |
| p_key_employer_contributions | P | YES | Ownership + Payroll |
| h_under_3_years_service | H | YES | Census |
| h_under_age_25 | H | YES | Census |
| h_under_35_hours | H | YES | Census |
| h_nonresident_alien | H | YES (partial) | Census |
| k_owners_10pct | K | YES | Ownership List |
| k_top5_officers_non_owners | K | YES | Ownership List + Payroll |
| k_top25pct_non_owner_officer | K | YES | Payroll (ranked) |
| k_total_105h_hci | K | COMPUTED | Sum of above |
| k_hci_non_covered_entities | K | YES (partial) | Ownership + Related Entity |
| q_employees_benefiting | Q | YES | Census (FSA election) |
| q_total_non_excludable | Q | YES | Census (computed) |
| q_eligible_to_benefit | Q | YES | Census |
| q_classification_description | Q | NO (PLAN DESIGN) | -- |
| q_105h_hci_benefiting | Q | YES | Ownership + Census |
| q_105h_hci_non_excludable | Q | YES | Ownership + Census |
| q_non_hci_benefiting | Q | YES | Census + Ownership |
| q_non_hci_non_excludable | Q | YES | Census + Ownership |
| r_same_expenses | R | NO (PLAN DESIGN) | -- |
| r_expenses_differ_describe | R | NO (PLAN DESIGN) | -- |
| r_same_maximum | R | NO (PLAN DESIGN) | -- |
| r_maximum_differ_describe | R | NO (PLAN DESIGN) | -- |
| r_same_cost_sharing | R | NO (PLAN DESIGN) | -- |
| r_cost_sharing_differ_describe | R | NO (PLAN DESIGN) | -- |
| r_same_waiting_periods | R | NO (PLAN DESIGN) | -- |
| r_waiting_differ_describe | R | NO (PLAN DESIGN) | -- |
| r_same_dependent_coverage | R | NO (PLAN DESIGN) | -- |
| r_dependent_differ_describe | R | NO (PLAN DESIGN) | -- |
| r_executive_physicals | R | NO (PLAN DESIGN) | -- |
| r_benefit_changes | R | NO (JUDGMENT) | -- |
| r_benefit_changes_describe | R | NO (JUDGMENT) | -- |
| i_under_age_21 | I | YES | Census |
| i_under_1_year_service | I | YES | Census |
| i_nonresident_alien | I | YES (partial) | Census |
| i_under_25k_compensation | I | YES | Census or Payroll |
| l_owners_5pct | L | YES | Ownership List |
| l_high_comp_non_owners | L | YES | Ownership + Payroll |
| l_total_hce | L | COMPUTED | Sum of above |
| l_hce_non_covered_entities | L | YES (partial) | Ownership + Related Entity |
| s_available_to_all | S | NO (PLAN DESIGN) | -- |
| s_classification_description | S | NO (PLAN DESIGN) | -- |
| s_total_non_excludable | S | YES | Census (computed) |
| s_non_hce_eligible | S | YES | Census + Ownership |
| t_same_maximum | T | NO (PLAN DESIGN) | -- |
| t_maximum_differ_describe | T | NO (PLAN DESIGN) | -- |
| t_same_terms | T | NO (PLAN DESIGN) | -- |
| t_terms_differ_describe | T | NO (PLAN DESIGN) | -- |
| t_nonelective_contributions | T | NO (PLAN DESIGN) | -- |
| t_nonelective_same_terms | T | NO (PLAN DESIGN) | -- |
| t_nonelective_describe | T | NO (PLAN DESIGN) | -- |
| u_owner_participant_count | U | YES | Ownership + Census |
| u_owner_election_total | U | YES | Ownership + Payroll |
| u_spouse_dependent_participate | U | NO (JUDGMENT) | -- |
| u_spouse_dependent_election_total | U | YES (if identified) | Payroll |
| u_total_dcap_elections | U | YES | Payroll |
| v_hce_participant_count | V | YES | Ownership + Census |
| v_hce_election_total | V | YES | Ownership + Payroll |
| v_non_hce_participant_count | V | YES | Census + Ownership |
| v_non_hce_election_total | V | YES | Payroll + Ownership |
| x_notes | X | NO | -- |
| x_attestation | X | NO | -- |

### Summary Counts

| Replacement Category | Field Count |
|---------------------|-------------|
| Cannot be replaced (PLAN DESIGN + JUDGMENT + identity) | 40 |
| Replaced by Census | 28 |
| Replaced by Payroll | 12 |
| Replaced by Ownership List | 14 |
| Replaced by Related Entity List | 5 |
| Replaced by Carrier Invoice | 2 |
| Auto-computed (sum fields) | 5 |
| Replaced by Census + Ownership cross-reference | 6 |
| **Total** | **112** |

---

## Appendix B: Implementation Priority

### Phase 1 (Highest Impact -- Build First)
1. **Census parser** -- eliminates the most questions and is the most standardized file format
2. **Conditional page logic** -- skip entire sections based on benefits offered
3. **Auto-computed sum fields** -- never ask users to add up their own numbers

### Phase 2 (High Impact)
4. **Payroll parser** -- eliminates error-prone dollar-amount fields
5. **Ownership list parser** -- small file, high value, eliminates complex HCI/HCE/Key Employee sections
6. **Parsed data confirmation UI** -- critical for user trust

### Phase 3 (Medium Impact)
7. **Related entity parser** -- small file, simple parsing
8. **Carrier invoice parser (CSV/XLSX only)** -- helps with 75% safe harbor check
9. **Cross-file correlation** -- linking census employees to ownership list and payroll

### Phase 4 (Lower Priority)
10. **Carrier invoice PDF parser** -- complex, unreliable, low incremental value
11. **SPD upload for reference** -- no parsing, just storage for audit trail
12. **Template downloads** -- provide blank CSV templates for each upload type so employers know what format to use
