# NDT-125 Cafeteria Plan Test: Calculated Fields & Sequencing Deductions

## 7. Calculated Fields & Sequencing Deductions

This section analyzes every field in the NDT-125 questionnaire seed (`q_ndt_125_cafeteria_plan_test`) to identify fields that can be auto-calculated, gate questions that eliminate downstream fields, and the resulting minimum question set.

**Total fields in current form: 89**

---

### 7a. Fields That Can Be Calculated From Other Answers

#### Summation Fields (always calculable)

| Calculated Field | Formula | Display Treatment |
|---|---|---|
| `j_total_hci` "Total HCIs (sum of all above)" | `j_owners_5pct + j_officers_non_owners + j_high_comp_non_owner_officer + j_spouse_dependent_count` (where `j_spouse_dependent_count` = 0 if `j_spouse_dependent_exists` = No) | **Read-only, show for verification.** User needs to confirm the system's sum matches their expectation since deduplication is manual. |
| `k_total_105h_hci` "Total Section 105(h) HCIs" | `k_owners_10pct + k_top5_officers_non_owners + k_top25pct_non_owner_officer` | **Read-only, show for verification.** Same deduplication concern. |
| `m_total_key_employees` "Total key employees" | `m_owners_5pct + m_owners_1pct_150k + m_officers_above_threshold` | **Read-only, show for verification.** |
| `l_total_hce` "Total Section 129 HCEs" | `l_owners_5pct + l_high_comp_non_owners` | **Read-only, show for verification.** |

#### Derivable "Non-Covered Entity" Fields (conditional on Section B)

When `b_controlled_group` = "No" OR `b_non_covered_entities` = "No", the following all default to **0** and need not be asked:

| Field | Default Value | Condition for Default |
|---|---|---|
| `j_hci_non_covered_entities` | 0 | No controlled group or no non-covered entities |
| `k_hci_non_covered_entities` | 0 | Same |
| `m_key_non_covered_entities` | 0 | Same |
| `l_hce_non_covered_entities` | 0 | Same |

These 4 fields can be **hidden entirely** when the condition applies, or shown as read-only "0" for confirmation.

#### Derivable Population Fields

| Calculated Field | Formula | Display Treatment |
|---|---|---|
| `n_total_non_excludable` | `c_total_employees + b_non_covered_entity_employee_count - (g_under_service_requirement + g_under_minimum_age + g_part_time_seasonal + g_nonresident_alien + g_cobra)` | **Read-only, show for verification.** Complex enough that user should confirm. |
| `q_total_non_excludable` | `c_total_employees + b_non_covered_entity_employee_count - (h_under_3_years_service + h_under_age_25 + h_under_35_hours + h_nonresident_alien)` | **Read-only, show for verification.** Different exclusion thresholds from Section 125. |
| `q_non_hci_non_excludable` | `q_total_non_excludable - q_105h_hci_non_excludable` | **Read-only, show for verification.** |
| `s_total_non_excludable` | `c_total_employees + b_non_covered_entity_employee_count - (i_under_age_21 + i_under_1_year_service + i_nonresident_alien)` | **Read-only, show for verification.** |

#### Fields That Are Always 0 When a Parent Is "No"

| Field | Is 0 When | Reason |
|---|---|---|
| `j_spouse_dependent_count` | `j_spouse_dependent_exists` = "No" | No spouse/dependent HCIs exist |
| `b_non_covered_entity_employee_count` | `b_controlled_group` = "No" or `b_non_covered_entities` = "No" | No non-covered entities |
| `c_cba_employee_count` | `c_cba_covered` = "No" | No union employees |
| `u_spouse_dependent_election_total` | `u_spouse_dependent_participate` = "No" | No owner spouse/dependent DCFSA elections |

#### Summary of Calculable Fields

| Category | Count |
|---|---|
| Summation totals (read-only verification) | 4 |
| Non-covered entity defaults (hidden when N/A) | 4 |
| Population derivations (read-only verification) | 4 |
| Zero-defaults from parent "No" | 4 |
| **Total calculable/derivable fields** | **16** |

---

### 7b. Sequencing Deductions (One Answer Eliminates Multiple Questions)

#### Gate 1: `b_controlled_group` = "No" (No related companies)

**Knockout chain:**
- `b_non_covered_entities` -- skipped (default: No)
- `b_non_covered_entity_names` -- skipped (default: empty)
- `b_non_covered_entity_employee_count` -- skipped (default: 0)
- `j_hci_non_covered_entities` -- skipped (default: 0)
- `k_hci_non_covered_entities` -- skipped (default: 0)
- `m_key_non_covered_entities` -- skipped (default: 0)
- `l_hce_non_covered_entities` -- skipped (default: 0)

**Net questions saved: 7**

#### Gate 1b: `b_controlled_group` = "Yes" BUT `b_non_covered_entities` = "No"

**Knockout chain:**
- `b_non_covered_entity_names` -- skipped (default: empty)
- `b_non_covered_entity_employee_count` -- skipped (default: 0)
- `j_hci_non_covered_entities` -- skipped (default: 0)
- `k_hci_non_covered_entities` -- skipped (default: 0)
- `m_key_non_covered_entities` -- skipped (default: 0)
- `l_hce_non_covered_entities` -- skipped (default: 0)

**Net questions saved: 6**

#### Gate 2: `c_cba_covered` = "No" (No union employees)

**Knockout chain:**
- `c_cba_good_faith` -- skipped (default: No)
- `c_cba_employee_count` -- skipped (default: 0)
- `c_cba_eligible` -- skipped (default: No)

**Net questions saved: 3**

#### Gate 2b: `c_cba_covered` = "Yes" BUT `c_cba_good_faith` = "No"

CBA employees cannot be excluded from testing if benefits were not bargained for, but the remaining questions (`c_cba_employee_count`, `c_cba_eligible`) are still relevant for reporting purposes.

**Net questions saved: 0** (all sub-questions still needed)

#### Gate 3: `f_simple_cafeteria` = "Yes" AND `f_100_or_fewer` = "Yes" AND `f_contribution_requirement` = "Yes" AND `f_eligibility_requirement` = "Yes" (Simple Cafeteria Plan Safe Harbor Met)

**Knockout chain:** ALL remaining sections (E through X) are skipped. The employer passes all tests automatically.

Sections eliminated: E (8 fields), G (5), J (7), N (4), O (8), M (5), P (3), H (4), K (5), Q (8), R (12), I (4), L (4), S (4), T (7), U (5), V (4), X (2)

**Net questions saved: 93** (but this is the entire rest of the form -- only Section A (3), B (4), C (5), D (2), F (4) = 18 questions remain)

Note: If any of the four F conditions is "No", the safe harbor fails and the full form is required.

#### Gate 4: `e_waiting_period` = "No" (No waiting period)

**Knockout chain:**
- `e_waiting_period_length` -- skipped
- `g_under_service_requirement` implicitly = 0 (but still shown for confirmation)

**Net questions saved: 1**

#### Gate 5: `e_minimum_age` = "No" (No minimum age)

**Knockout chain:**
- `e_minimum_age_value` -- skipped
- `g_under_minimum_age` implicitly = 0 (but still shown for confirmation)

**Net questions saved: 1**

#### Gate 6: `e_hours_requirement` = "No" (No hours requirement)

**Knockout chain:**
- `e_hours_requirement_value` -- skipped

**Net questions saved: 1**

#### Gate 7: `e_eligibility_varies` = "No" (Same rules for all)

**Knockout chain:**
- `e_eligibility_varies_describe` -- skipped

**Net questions saved: 1**

#### Gate 8: `d_benefits_offered` does NOT include "Health FSA" or "Limited Purpose FSA" (No self-insured plans)

**Knockout chain:** Entire sections H, K, Q, R are skipped.
- Section H: 4 fields
- Section K: 5 fields
- Section Q: 8 fields
- Section R: 12 fields

**Net questions saved: 29**

#### Gate 9: `d_benefits_offered` does NOT include "Dependent Care FSA" (No DCFSA)

**Knockout chain:** Entire sections I, L, S, T, U, V are skipped.
- Section I: 4 fields
- Section L: 4 fields
- Section S: 4 fields
- Section T: 7 fields
- Section U: 5 fields
- Section V: 4 fields

**Net questions saved: 28**

#### Gate 10: `d_benefits_offered` = ONLY "Pre-tax insurance premiums (POP)" (POP-only plan)

**Knockout chain:** Sections M, P, O (contributions test), H, K, Q, R, I, L, S, T, U, V all skipped.
- POP-only plans skip the concentration test and all sub-plan tests.

**Net questions saved: 57** (Sections M+P+O+H+K+Q+R+I+L+S+T+U+V)

#### Gate 11: `n_available_to_all` = "Yes" (Plan open to all equally -- Section 125 Eligibility)

**Knockout chain:**
- `n_classification_description` -- skipped
- `n_non_hci_eligible` -- skipped (equals total non-excludable non-HCIs)

**Net questions saved: 2**

#### Gate 12: `o_same_benefits_all` = "Yes" (Same benefits for everyone -- Section 125 C&B)

**Knockout chain:**
- `o_benefits_differ_describe` -- skipped

**Net questions saved: 1**

#### Gate 13: `j_spouse_dependent_exists` = "No"

**Knockout chain:**
- `j_spouse_dependent_count` -- skipped (default: 0)

**Net questions saved: 1**

#### Gate 14: Section R sub-gates (each "Yes" or "N/A" skips the describe field)

| Gate | Answer | Field Skipped |
|---|---|---|
| `r_same_expenses` = "Yes" | | `r_expenses_differ_describe` |
| `r_same_maximum` = "Yes" | | `r_maximum_differ_describe` |
| `r_same_cost_sharing` = "Yes" or "N/A" | | `r_cost_sharing_differ_describe` |
| `r_same_waiting_periods` = "Yes" | | `r_waiting_differ_describe` |
| `r_same_dependent_coverage` = "Yes" or "N/A" | | `r_dependent_differ_describe` |
| `r_benefit_changes` = "No" | | `r_benefit_changes_describe` |

**Net questions saved: up to 6** (likely all 6, since most compliant plans answer Yes)

#### Gate 15: Section T sub-gates

| Gate | Answer | Field Skipped |
|---|---|---|
| `t_same_maximum` = "Yes" | | `t_maximum_differ_describe` |
| `t_same_terms` = "Yes" | | `t_terms_differ_describe` |
| `t_nonelective_contributions` = "No" | | `t_nonelective_same_terms`, `t_nonelective_describe` |

**Net questions saved: up to 4**

#### Gate 16: Section S sub-gates

| Gate | Answer | Field Skipped |
|---|---|---|
| `s_available_to_all` = "Yes" | | `s_classification_description`, `s_non_hce_eligible` |

**Net questions saved: up to 2**

#### Gate 17: `u_spouse_dependent_participate` = "No"

**Knockout chain:**
- `u_spouse_dependent_election_total` -- skipped (default: 0)

**Net questions saved: 1**

#### Gate 18: `d_benefits_other_describe` (conditional on "Other" checkbox)

If "Other" is not selected in `d_benefits_offered`:
- `d_benefits_other_describe` -- skipped

**Net questions saved: 1**

---

### 7c. Question Reduction Summary Table

Baseline count: **89 fields** across all sections.

| Optimization Type | Questions in Current Form | Questions Eliminated | Net Questions |
|---|---|---|---|
| **Baseline (no optimization)** | 89 | 0 | 89 |
| **Sequencing deductions only** (typical scenario: no controlled group, no CBA, no safe harbor, has Health FSA + DCFSA, all benefits same) | 89 | 27 | 62 |
| **Sequencing deductions only** (POP-only plan, no controlled group, no CBA) | 89 | 67 | 22 |
| **Calculated fields only** (auto-compute totals and populations) | 89 | 8 | 81 |
| **Calculated fields as read-only** (show but don't require manual entry) | 89 | 8 manual entries removed, 8 read-only shown | 81 manual + 8 auto |
| **File uploads: census** (replaces Sections G, H, I excludable counts, plus c_total_employees) | 89 | 14 | 75 |
| **File uploads: census + payroll** (also replaces HCI/HCE/Key Employee classification counts in J, K, L, M, plus contribution dollar amounts in O, P, V, U) | 89 | 42 | 47 |
| **All optimizations combined** (typical full-benefit plan: census+payroll upload, sequencing, calculated fields) | 89 | 58 | 31 |
| **Best case** (POP-only, no controlled group, no CBA, census upload) | 89 | 71 | 18 |
| **Best case with safe harbor** (Simple Cafeteria Plan passes) | 89 | 71 | 18 |

**Scenario detail for "All optimizations combined" (typical full-benefit plan):**
- Start: 89
- Sequencing saves: ~27 (no controlled group, no CBA, no safe harbor, Yes/N/A on all R gates, Yes on all T gates, no Other benefit)
- Census+payroll upload saves: ~42 (all numeric classification and dollar amount fields)
- Calculated field saves: ~8 (totals and population derivations auto-computed, some overlap with uploads)
- Overlap adjustment: ~19 (many uploaded fields are also the ones that would be sequenced away or calculated)
- Net: approximately **31 manual questions**

---

### 7d. Revised Minimal Question Set

Taking ALL three optimization types into account (file uploads, auto-calculations, and sequencing deductions), here is the absolute minimum question list.

---

#### ALWAYS ASK (no way to avoid these) -- 19 questions

These must be answered regardless of uploads, deductions, or calculations.

| # | Field Key | Label | Section | Reason |
|---|---|---|---|---|
| 1 | `a_employer_legal_name` | Employer legal name | A | Identity |
| 2 | `a_plan_year_end_date` | Plan year end date | A | Identity |
| 3 | `a_entity_type` | Entity type | A | Determines testing applicability |
| 4 | `b_controlled_group` | Is the employer part of a related group? | B | Gate question |
| 5 | `c_total_employees` | Total employees | C | Baseline for all tests (unless census uploaded) |
| 6 | `c_cba_covered` | Are any employees union members? | C | Gate question |
| 7 | `d_benefits_offered` | Which benefits does the cafeteria plan offer? | D | Determines which sections apply |
| 8 | `f_simple_cafeteria` | Has the employer adopted a Simple Cafeteria Plan? | F | Gate question (potential full bypass) |
| 9 | `e_waiting_period` | Does the plan have a waiting period? | E | Gate + eligibility context |
| 10 | `e_minimum_age` | Does the plan require a minimum age? | E | Gate + eligibility context |
| 11 | `e_hours_requirement` | Does the plan require minimum hours? | E | Gate + eligibility context |
| 12 | `e_eligibility_varies` | Do eligibility rules differ by class? | E | Gate question |
| 13 | `n_available_to_all` | Is the plan open to all equally? | N | Gate for eligibility test |
| 14 | `o_same_benefits_all` | Are the same benefits offered to everyone? | O | Gate for C&B test |
| 15 | `o_safe_harbor_health` | Does the employer meet the 75% safe harbor? | O | Determines if C&B test auto-passes |
| 16 | `j_spouse_dependent_exists` | Are any employees spouses/dependents of HCIs? | J | Gate question |
| 17 | `x_attestation` | I confirm this information is accurate | X | Required for submission |
| 18 | `x_notes` | Additional notes | X | Optional but always shown |
| 19 | `d_benefits_other_describe` | If Other, describe | D | Conditional on "Other" checkbox but always shown if applicable |

Note: Questions 5 (`c_total_employees`) moves to "ASK ONLY IF NO UPLOAD" when census is provided, but it is listed here because the census upload is optional. If census is uploaded, it becomes confirmatory (read-only).

---

#### ASK IF APPLICABLE (gated by earlier answers) -- 24 questions

These appear only when a prior answer triggers them.

| # | Field Key | Label | Gate Condition |
|---|---|---|---|
| 1 | `b_non_covered_entities` | Do related companies exclude employees? | `b_controlled_group` = Yes |
| 2 | `b_non_covered_entity_names` | Names of non-covered companies | `b_non_covered_entities` = Yes |
| 3 | `b_non_covered_entity_employee_count` | Employee count at non-covered companies | `b_non_covered_entities` = Yes |
| 4 | `c_cba_good_faith` | Were cafeteria benefits bargained for? | `c_cba_covered` = Yes |
| 5 | `c_cba_employee_count` | Number of CBA employees | `c_cba_covered` = Yes |
| 6 | `c_cba_eligible` | Can CBA employees participate? | `c_cba_covered` = Yes |
| 7 | `f_100_or_fewer` | Did employer average 100 or fewer? | `f_simple_cafeteria` = Yes |
| 8 | `f_contribution_requirement` | Does employer meet contribution req? | `f_simple_cafeteria` = Yes |
| 9 | `f_eligibility_requirement` | Are all 1,000+ hour employees eligible? | `f_simple_cafeteria` = Yes |
| 10 | `e_waiting_period_length` | How long is the waiting period? | `e_waiting_period` = Yes |
| 11 | `e_minimum_age_value` | What is the minimum age? | `e_minimum_age` = Yes |
| 12 | `e_hours_requirement_value` | What is the hours requirement? | `e_hours_requirement` = Yes |
| 13 | `e_eligibility_varies_describe` | Describe class eligibility rules | `e_eligibility_varies` = Yes |
| 14 | `n_classification_description` | Describe eligibility differences | `n_available_to_all` = No |
| 15 | `n_non_hci_eligible` | Non-HCIs eligible to participate | `n_available_to_all` = No |
| 16 | `o_benefits_differ_describe` | Describe benefit differences | `o_same_benefits_all` = No |
| 17 | `j_spouse_dependent_count` | Additional HCI spouse/dependent count | `j_spouse_dependent_exists` = Yes |
| 18 | `r_expenses_differ_describe` | Describe expense differences | `r_same_expenses` = No |
| 19 | `r_maximum_differ_describe` | Describe maximum differences | `r_same_maximum` = No |
| 20 | `r_cost_sharing_differ_describe` | Describe cost sharing differences | `r_same_cost_sharing` = No |
| 21 | `r_waiting_differ_describe` | Describe waiting period differences | `r_same_waiting_periods` = No |
| 22 | `r_dependent_differ_describe` | Describe dependent coverage differences | `r_same_dependent_coverage` = No |
| 23 | `r_benefit_changes_describe` | Describe mid-year changes | `r_benefit_changes` = Yes |
| 24 | `u_spouse_dependent_election_total` | DCFSA elections by owner spouses/dependents | `u_spouse_dependent_participate` = Yes |

Plus the following are gated by `d_benefits_offered` selections (section-level gates):

| Gate | Sections Shown | Additional "ASK IF APPLICABLE" Questions |
|---|---|---|
| Health FSA or Limited Purpose FSA selected | H, K, Q, R | All Yes/No gate questions in R (6 questions: `r_same_expenses`, `r_same_maximum`, `r_same_cost_sharing`, `r_same_waiting_periods`, `r_same_dependent_coverage`, `r_executive_physicals`, `r_benefit_changes`) + Q eligibility questions |
| Dependent Care FSA selected | I, L, S, T, U, V | All gate questions in S, T, U (e.g., `s_available_to_all`, `t_same_maximum`, `t_same_terms`, `t_nonelective_contributions`, `u_spouse_dependent_participate`) |
| NOT POP-only | M, P, O (full) | Key employee and concentration fields |

---

#### ASK ONLY IF NO UPLOAD (file upload would provide this) -- 30 questions

These are numeric data-entry fields that a census file, payroll file, or election file could populate automatically.

**Census file replaces:**

| # | Field Key | Label | Section |
|---|---|---|---|
| 1 | `c_total_employees` | Total employees | C |
| 2 | `c_cba_employee_count` | Number of CBA employees | C |
| 3 | `g_under_service_requirement` | Employees not meeting service req | G |
| 4 | `g_under_minimum_age` | Employees under minimum age | G |
| 5 | `g_part_time_seasonal` | Part-time/seasonal employees | G |
| 6 | `g_nonresident_alien` | Nonresident aliens | G |
| 7 | `g_cobra` | COBRA participants | G |
| 8 | `h_under_3_years_service` | Employees < 3 years service | H |
| 9 | `h_under_age_25` | Employees under age 25 | H |
| 10 | `h_under_35_hours` | Employees < 35 hours/week | H |
| 11 | `h_nonresident_alien` | Nonresident aliens (105h) | H |
| 12 | `i_under_age_21` | Employees under age 21 | I |
| 13 | `i_under_1_year_service` | Employees < 1 year service | I |
| 14 | `i_nonresident_alien` | Nonresident aliens (129) | I |
| 15 | `i_under_25k_compensation` | Employees earning < $25K | I |
| 16 | `b_non_covered_entity_employee_count` | Non-covered entity employees | B |

**Payroll + ownership data replaces:**

| # | Field Key | Label | Section |
|---|---|---|---|
| 17 | `j_owners_5pct` | 5%+ owners | J |
| 18 | `j_officers_non_owners` | Officers (non-owners) | J |
| 19 | `j_high_comp_non_owner_officer` | High earners (non-owner/officer) | J |
| 20 | `k_owners_10pct` | 10%+ owners | K |
| 21 | `k_top5_officers_non_owners` | Top-5 paid officers | K |
| 22 | `k_top25pct_non_owner_officer` | Top-25% earners | K |
| 23 | `m_owners_5pct` | 5%+ owners (key employee) | M |
| 24 | `m_owners_1pct_150k` | 1-5% owners earning > $150K | M |
| 25 | `m_officers_above_threshold` | Officers above comp threshold | M |
| 26 | `l_owners_5pct` | 5%+ owners (Sec 129) | L |
| 27 | `l_high_comp_non_owners` | High earners (Sec 129) | L |

**Election/enrollment data replaces:**

| # | Field Key | Label | Section |
|---|---|---|---|
| 28 | `o_hcp_count` | HCIs who elected benefits | O |
| 29 | `o_non_hci_participant_count` | Non-HCIs who elected benefits | O |
| 30 | `o_hcp_employee_reductions` | Total salary reductions by HCPs | O |

(And similarly for `o_hcp_employer_contributions`, `o_non_hci_employee_reductions`, `o_non_hci_employer_contributions`, `p_key_participant_count`, `p_key_employee_reductions`, `p_key_employer_contributions`, `q_employees_benefiting`, `q_eligible_to_benefit`, `q_105h_hci_benefiting`, `q_non_hci_benefiting`, `v_hce_participant_count`, `v_hce_election_total`, `v_non_hce_participant_count`, `v_non_hce_election_total`, `u_owner_participant_count`, `u_owner_election_total`, `u_total_dcap_elections` -- an additional 18 fields.)

**Total replaceable by file uploads: 48 fields** (16 census + 11 payroll/ownership + 21 election/enrollment)

---

#### NEVER ASK (always calculated or derived) -- 8 questions

These should never require manual entry. The system computes them automatically.

| # | Field Key | Label | Formula | Notes |
|---|---|---|---|---|
| 1 | `j_total_hci` | Total HCIs | Sum of j_owners_5pct + j_officers_non_owners + j_high_comp_non_owner_officer + j_spouse_dependent_count | Show as read-only for verification |
| 2 | `k_total_105h_hci` | Total 105(h) HCIs | Sum of k_owners_10pct + k_top5_officers_non_owners + k_top25pct_non_owner_officer | Show as read-only for verification |
| 3 | `m_total_key_employees` | Total key employees | Sum of m_owners_5pct + m_owners_1pct_150k + m_officers_above_threshold | Show as read-only for verification |
| 4 | `l_total_hce` | Total Sec 129 HCEs | Sum of l_owners_5pct + l_high_comp_non_owners | Show as read-only for verification |
| 5 | `n_total_non_excludable` | Total non-excludable (Sec 125) | c_total_employees + b_non_covered_entity_employee_count - sum(Section G) | Show as read-only for verification |
| 6 | `q_total_non_excludable` | Total non-excludable (Sec 105h) | c_total_employees + b_non_covered_entity_employee_count - sum(Section H) | Show as read-only for verification |
| 7 | `q_non_hci_non_excludable` | Non-excludable non-HCIs (105h) | q_total_non_excludable - q_105h_hci_non_excludable | Show as read-only for verification |
| 8 | `s_total_non_excludable` | Total non-excludable (Sec 129) | c_total_employees + b_non_covered_entity_employee_count - sum(Section I) | Show as read-only for verification |

---

#### SHOW FOR CONFIRMATION (calculated, displayed read-only for user to verify) -- 8 questions

These are the same 8 fields listed under "NEVER ASK" above. They should be displayed as **read-only computed values** with an option for the user to override if the calculation does not match their records (e.g., due to deduplication edge cases).

| # | Field Key | Computed Value | Override Allowed? |
|---|---|---|---|
| 1 | `j_total_hci` | Auto-summed | Yes (manual deduplication may differ) |
| 2 | `k_total_105h_hci` | Auto-summed | Yes |
| 3 | `m_total_key_employees` | Auto-summed | Yes |
| 4 | `l_total_hce` | Auto-summed | Yes |
| 5 | `n_total_non_excludable` | Auto-computed | Yes (CBA exclusions are nuanced) |
| 6 | `q_total_non_excludable` | Auto-computed | Yes |
| 7 | `q_non_hci_non_excludable` | Auto-computed | Yes |
| 8 | `s_total_non_excludable` | Auto-computed | Yes |

**UI treatment:** Display as a pre-filled, highlighted field (e.g., light blue background) with the formula shown in a tooltip. Include an "Override" link that converts it to an editable field if the user disagrees with the computed value.

---

#### Combined Minimum Question Counts by Scenario

| Scenario | ALWAYS ASK | ASK IF APPLICABLE | MANUAL DATA ENTRY | CONFIRMATION | Total User Actions |
|---|---|---|---|---|---|
| **Worst case** (full benefits, controlled group, CBA, all classifications differ, no uploads) | 19 | 24 | 30 | 8 | 81 |
| **Typical** (full benefits, no controlled group, no CBA, all benefits same, no uploads) | 17 | 6 | 22 | 6 | 51 |
| **Typical + census upload** | 17 | 6 | 6 | 6 | 35 |
| **Typical + census + payroll + elections upload** | 17 | 6 | 0 | 6 | 29 |
| **POP-only, no controlled group, no CBA** | 14 | 3 | 0 | 0 | 17 |
| **Simple Cafeteria Plan safe harbor passes** | 12 | 4 | 0 | 0 | 16 |
| **Best possible case** (safe harbor, no controlled group, no CBA) | 10 | 2 | 0 | 0 | 12 |

The realistic target for a "typical" employer (full benefits, compliant plan, census upload provided) is approximately **35 user interactions** -- down from 89 fields, a **61% reduction**.
