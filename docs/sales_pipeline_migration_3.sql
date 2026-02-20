-- =============================================================================
-- Sales Pipeline Session 3 — Production Migration
-- Application Form, IRS Limits, Benefit/Billing Types
-- Date: February 19-20, 2026
--
-- PREREQUISITES:
--   sales_pipeline_migration.sql   (Session 1) must be run first
--   sales_pipeline_migration_2.sql (Session 2) must be run first
--
-- Run this BEFORE deploying Session 3 code changes.
-- =============================================================================

-- =============================================================================
-- PART 1: LOS EXPANSION
-- Split HRA/MERP into 5 variants, add billing variants, add LSA + Adoption
-- =============================================================================

DELETE FROM losmodules WHERE los_id = 7;
DELETE FROM los WHERE los_id = 7;

INSERT INTO servicemodule (module_id, description, short_text, sort_order, psp_id) VALUES
(31, 'Lifestyle Spending Accounts', 'LSA', 31, 4),
(32, 'Adoption Assistance', 'ADOPT', 32, 4);

INSERT INTO los (los_id, description, short_text, psp_id) VALUES
(11, 'Health Reimbursement Arrangement', 'HRA', 4),
(12, 'Medical Expense Reimbursement Plan', 'MERP', 4),
(13, 'Individual Coverage HRA', 'ICHRA', 4),
(14, 'Excepted Benefit HRA', 'EBHRA', 4),
(15, 'Qualified Small Employer HRA', 'QSEHRA', 4),
(16, 'Retiree Billing', 'RETIREE', 4),
(17, 'Direct Billing', 'DIRECT', 4),
(18, 'Lifestyle Spending Account', 'LSA', 4),
(19, 'Adoption Assistance', 'ADOPTION', 4);

-- Fix psp_id for new LOSs (ensure ProposalBuilder sees them)
UPDATE los SET psp_id = 4 WHERE los_id IN (11,12,13,14,15,16,17,18,19);

-- Module wiring
INSERT INTO losmodules (los_id, module_id) VALUES
(11, 18), (11, 20), (11, 21),
(12, 18), (12, 20), (12, 21),
(13, 18), (13, 20), (13, 21),
(14, 18), (14, 20), (14, 21),
(15, 18), (15, 20), (15, 21),
(16, 23),
(17, 23),
(18, 31), (18, 20), (18, 21),
(19, 32);

-- =============================================================================
-- PART 2: APPLICATION SECTION MODEL
-- =============================================================================

CREATE TABLE IF NOT EXISTS applicationsection (
    section_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    scope VARCHAR(10) NOT NULL DEFAULT 'ALL',
    sort_order INT NOT NULL DEFAULT 0,
    psp_id BIGINT NOT NULL,
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS applicationsectionlos (
    section_id BIGINT NOT NULL,
    los_id BIGINT NOT NULL,
    PRIMARY KEY (section_id, los_id),
    FOREIGN KEY (section_id) REFERENCES applicationsection(section_id),
    FOREIGN KEY (los_id) REFERENCES los(los_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Modify applicationfield: drop templatePurpose FK, add section FK + help_text
ALTER TABLE applicationfield
    DROP FOREIGN KEY FK_DATAKEY_template_purpose_id;
ALTER TABLE applicationfield
    DROP COLUMN template_purpose_id;
ALTER TABLE applicationfield
    ADD COLUMN section_id BIGINT,
    ADD COLUMN help_text VARCHAR(500),
    ADD CONSTRAINT fk_appfield_section FOREIGN KEY (section_id) REFERENCES applicationsection(section_id);

-- =============================================================================
-- PART 3: SEED SECTIONS (1-20)
-- =============================================================================

INSERT INTO applicationsection (section_id, name, description, scope, sort_order, psp_id) VALUES
(1,  'Company Information', 'Basic company details for plan document preparation.', 'ALL', 100, 4),
(2,  'Primary Contact', 'Main point of contact for plan setup and administration.', 'ALL', 200, 4),
(3,  'Company Address', NULL, 'ALL', 300, 4),
(4,  'Plan Year & Eligibility', 'Plan effective dates and employee eligibility requirements.', 'ALL', 400, 4),
(5,  'Pay Cycle & Deductions', 'Payroll deduction schedule and cycle information.', 'LOS', 500, 4),
(6,  'Signing Officer', 'Officer authorized to sign plan documents.', 'ALL', 600, 4),
(7,  'Bank Account (EFT)', 'Company bank account for electronic fund transfers.', 'ALL', 700, 4),
(8,  'Premium Pre-Tax Selections', 'Which employee premiums will be pre-taxed under the Section 125 plan.', 'LOS', 800, 4),
(9,  'Section 125 Plan Features', 'Additional features available under your Section 125 plan.', 'LOS', 900, 4),
(10, 'FSA Options', 'Flexible Spending Account types, limits, and rollover rules.', 'LOS', 1000, 4),
(11, 'HRA Plan Design', 'Benefit structure, eligible expenses, and availability schedule.', 'LOS', 1100, 4),
(12, 'HRA Carryover & Spenddown', 'Year-end carryover rules and post-eligibility spenddown access.', 'LOS', 1200, 4),
(13, 'HSA Funding', 'How the employer will fund employee Health Savings Accounts.', 'LOS', 1300, 4),
(14, 'Transit / Commuter Options', 'Parking and transit benefit selections and election rules.', 'LOS', 1400, 4),
(15, 'Billing Administration', 'Benefit plans and rate structures for billing arrangements.', 'LOS', 1500, 4),
(16, 'Payment Services', 'Reimbursement payment method preferences.', 'LOS', 1600, 4),
(17, 'Debit Card Services', 'Debit card setup for participant benefit access.', 'LOS', 1700, 4),
(18, 'LSA Plan Design', 'Lifestyle Spending Account eligible expenses and limits.', 'LOS', 1800, 4),
(19, 'Adoption Assistance', 'Adoption assistance benefit design and limits.', 'LOS', 1900, 4),
(20, 'COBRA-Specific Requirements', 'Termination rules, state requirements, and current COBRA activity.', 'LOS', 1550, 4);

-- Section-to-LOS scoping
INSERT INTO applicationsectionlos (section_id, los_id) VALUES
(5, 6), (5, 10), (5, 12),
(8, 5), (8, 6),
(9, 5), (9, 6),
(10, 6),
(11, 11), (11, 12), (11, 13), (11, 14), (11, 15),
(12, 11), (12, 13), (12, 14), (12, 15),
(13, 9),
(14, 10),
(15, 8), (15, 16), (15, 17),
(16, 6), (16, 11), (16, 12), (16, 13), (16, 14), (16, 15), (16, 18), (16, 19),
(17, 6), (17, 11), (17, 12), (17, 13), (17, 14), (17, 15), (17, 18),
(18, 18),
(19, 19),
(20, 8);

-- =============================================================================
-- PART 4: SEED APPLICATION FIELDS (all sections, final sort orders)
-- =============================================================================

-- Clear any existing fields from Session 1 migration
DELETE FROM applicationfield;

-- Section 1: Company Information
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('company_name',            'Company Name',              1, 'TEXT',   1, 100, NULL, NULL),
('company_tax_id',          'Tax ID (EIN)',              1, 'TEXT',   1, 200, NULL, 'Federal Employer Identification Number'),
('company_structure',       'Company Structure',         1, 'SELECT', 1, 300, 'C-Corporation|S-Corporation|LLC|Partnership|Sole Proprietorship|Non-Profit|Government|Other', NULL),
('company_structure_other', 'If Other, Describe',        1, 'TEXT',   0, 400, NULL, NULL),
('total_employees',         'Total Number of Employees', 1, 'NUMBER', 0, 500, NULL, 'Approximate headcount across all locations');

-- Section 2: Primary Contact
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('contact_first_name', 'First Name',       2, 'TEXT', 1, 100, NULL, NULL),
('contact_last_name',  'Last Name',        2, 'TEXT', 1, 200, NULL, NULL),
('contact_email',      'Email',            2, 'TEXT', 1, 300, NULL, NULL),
('contact_phone',      'Phone Number',     2, 'TEXT', 1, 400, NULL, NULL),
('contact_title',      'Title / Position', 2, 'TEXT', 0, 500, NULL, NULL),
('contact_fax',        'Fax Number',       2, 'TEXT', 0, 600, NULL, NULL);

-- Section 3: Company Address
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('address_street1', 'Street Address',  3, 'TEXT', 1, 100, NULL, NULL),
('address_street2', 'Address Line 2',  3, 'TEXT', 0, 200, NULL, 'Suite, floor, etc.'),
('address_city',    'City',            3, 'TEXT', 1, 300, NULL, NULL),
('address_state',   'State',           3, 'TEXT', 1, 400, NULL, NULL),
('address_zip',     'Zip Code',        3, 'TEXT', 1, 500, NULL, NULL);

-- Section 4: Plan Year & Eligibility
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('plan_start_date',         'Plan Start / Effective Date', 4, 'DATE',   1, 100, NULL, NULL),
('plan_renewal_date',       'Plan Renewal Date',           4, 'DATE',   1, 200, NULL, NULL),
('elig_min_age',            'Minimum Age of an Eligible Employee', 4, 'NUMBER', 0, 300, NULL, 'Leave blank if no minimum'),
('elig_min_hours',          'Minimum Hours per Week',      4, 'NUMBER', 0, 400, NULL, 'Leave blank if no minimum'),
('elig_seasonal_months',    'Minimum Months per Year for Seasonal Employee Eligibility', 4, 'NUMBER', 0, 500, NULL, 'Leave blank if not applicable'),
('elig_exclude_union',      'Exclude Union Employees',     4, 'RADIO',  1, 600, 'Yes|No', NULL),
('elig_entry_frequency',    'How Frequently May Eligible Employees Enter the Plan', 4, 'SELECT', 1, 700, 'Immediately upon eligibility|Monthly|Quarterly|Semi-Annually|Annually (open enrollment only)|Other', NULL),
('elig_entry_freq_other',   'If Other, Describe',          4, 'TEXT',   0, 750, NULL, NULL),
('elig_service_time',       'Service Time Required to Become Benefit Eligible', 4, 'SELECT', 1, 800, 'Immediately upon hire|30 days|60 days|90 days|First of month after hire|First of month after 30 days|First of month after 60 days|First of month after 90 days|Other', NULL),
('elig_service_time_other', 'If Other, Describe',          4, 'TEXT',   0, 850, NULL, NULL);

-- Section 5: Pay Cycle & Deductions
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('pay_period_1',          'Pay Period 1 Frequency',                5, 'SELECT', 1, 100, 'Weekly|Bi-Weekly|Semi-Monthly|Monthly', NULL),
('pay_first_deduction_1', 'Date of First Pay Deduction',           5, 'DATE',   1, 200, NULL, NULL),
('pay_has_second_cycle',  'Do You Have a 2nd Pay Cycle',           5, 'RADIO',  1, 300, 'Yes|No', NULL),
('pay_period_2',          'Pay Period 2 Frequency',                5, 'SELECT', 0, 400, 'Weekly|Bi-Weekly|Semi-Monthly|Monthly', NULL),
('pay_first_deduction_2', 'Date of First Pay Deduction (Cycle 2)', 5, 'DATE',   0, 500, NULL, NULL);

-- Section 6: Signing Officer
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('officer_name',  'Name of Officer Who Will Sign Plan Documents', 6, 'TEXT', 1, 100, NULL, NULL),
('officer_title', 'Title of Officer',                             6, 'TEXT', 1, 200, NULL, NULL);

-- Section 7: Bank Account
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('bank_name',           'Bank Name',        7, 'TEXT',  1, 100, NULL, NULL),
('bank_account',        'Account Number',   7, 'TEXT',  1, 200, NULL, NULL),
('bank_routing',        'Routing Number',   7, 'TEXT',  1, 300, NULL, NULL),
('bank_account_type',   'Account Type',     7, 'RADIO', 1, 400, 'Checking|Savings', NULL),
('bank_address',        'Bank Address',     7, 'TEXT',  0, 500, NULL, NULL),
('bank_starting_check', 'Starting Check #', 7, 'TEXT',  0, 600, NULL, 'If applicable');

-- Section 8: Premium Pre-Tax Selections
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('pretax_major_medical', 'Major Medical',              8, 'BOOLEAN', 0, 100, NULL, NULL),
('pretax_dental_vision', 'Group Dental / Vision',      8, 'BOOLEAN', 0, 200, NULL, NULL),
('pretax_group_life',    'Group Term Life',             8, 'BOOLEAN', 0, 300, NULL, NULL),
('pretax_disability',    'Group Disability (STD/LTD)',  8, 'BOOLEAN', 0, 400, NULL, NULL),
('pretax_other',         'Other Premiums to Pre-Tax',   8, 'TEXT',    0, 500, NULL, 'Describe any additional premiums');

-- Section 9: Section 125 Plan Features
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('s125_hsa_pretax',         'Include HSA Pre-Tax Contributions', 9, 'BOOLEAN', 0, 100, NULL, 'Expanded POP — adds HSA salary reduction to the 125 plan.'),
('s125_flex_credits',       'Flexible Credits',                  9, 'BOOLEAN', 0, 200, NULL, 'Employer-funded credits employees can apply toward benefits'),
('s125_flex_credit_amount', 'Flexible Credit Amount',            9, 'TEXT',    0, 300, NULL, 'Describe the credit structure if applicable'),
('s125_cash_in_lieu',       'Cash in Lieu of Benefits',          9, 'BOOLEAN', 0, 400, NULL, 'Allow employees to receive taxable cash if they waive coverage'),
('s125_vacation_buysell',   'Vacation Buy / Sell Option',        9, 'BOOLEAN', 0, 500, NULL, NULL);

-- Section 10: FSA Options (help_text NULL — dynamic IRS limits in JSP)
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('fsa_health',          'Health FSA',                                    10, 'BOOLEAN', 0, 100, NULL, NULL),
('fsa_limited_purpose', 'Limited Purpose FSA (HSA Compatible)',          10, 'BOOLEAN', 0, 200, NULL, 'Dental and vision expenses only — for employees with an HSA'),
('fsa_health_limit',    'Health / Limited Purpose FSA Annual Limit',     10, 'NUMBER',  0, 300, NULL, NULL),
('fsa_health_rollover', 'Health / Limited Purpose FSA Year-End Rule',    10, 'SELECT',  0, 400, 'No Carryover or Grace Period|Carryover|2-1/2 Month Grace Period', NULL),
('fsa_dependent_care',  'Dependent Care FSA',                            10, 'BOOLEAN', 0, 500, NULL, NULL),
('fsa_depcare_limit',   'Dependent Care FSA Annual Limit',               10, 'NUMBER',  0, 600, NULL, NULL),
('fsa_depcare_rollover','Dependent Care FSA Year-End Rule',              10, 'SELECT',  0, 700, 'No Spend Down or Grace Period|2-1/2 Month Grace Period|Spend Down after Termination|Grace Period and Spend Down', NULL),
('fsa_pra',             'Premium Reimbursement Account (PRA)',           10, 'BOOLEAN', 0, 800, NULL, 'Pre-tax reimbursement for individual non-major-medical premiums');

-- Section 11: HRA Plan Design
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('hra_eligible_expenses',    'Reimbursable Expenses',                          11, 'CHECKBOX', 1, 100, 'Deductible|General Medical Expenses|Coinsurance|General Dental Expenses|Co-Pays|General Vision Expenses|Individual Insurance Premiums|Prescription / Rx Expenses', NULL),
('hra_eligible_other',       'Other Reimbursable Expenses',                    11, 'TEXT',     0, 150, NULL, 'Describe any additional eligible expenses'),
('hra_benefit_structure',    'Benefit Amount Structure',                        11, 'RADIO',   1, 200, 'Flat rate regardless of coverage level|Varies by coverage level (Single, Family, etc.)', NULL),
('hra_flat_amount',          'Flat Rate Benefit Amount (per Plan Year)',        11, 'NUMBER',  0, 300, NULL, 'Enter the annual dollar amount per participant'),
('hra_amount_by_tier',       'Benefit Amounts by Coverage Tier',               11, 'TEXTAREA',0, 400, NULL, 'List each tier and amount (e.g., Single: $1,000, Family: $2,500)'),
('hra_benefit_availability', 'How the Annual Benefit Becomes Available',        11, 'RADIO',   1, 450, 'Entirely available on day one|Equal monthly increments|Equal quarterly increments|Equal semi-annual increments', NULL),
('hra_annual_usage_cap',     'Annual Usage Cap',                                11, 'NUMBER',  0, 500, NULL, 'Maximum reimbursement per year. Leave blank for no cap.');

-- Section 12: HRA Carryover & Spenddown
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('hra_has_carryover',    'Allow Year-End Carryover',              12, 'RADIO',    1, 100, 'Yes|No', 'Unspent HRA balance carries forward to future plan years'),
('hra_carryover_pct',    'Carryover Percentage',                  12, 'NUMBER',   0, 200, NULL, 'Percentage of remaining balance that carries over (e.g., 100)'),
('hra_carryover_cap',    'Carryover Cap',                         12, 'NUMBER',   0, 300, NULL, 'Maximum dollar amount that can carry over. Leave blank for no cap.'),
('hra_has_spenddown',    'Allow Post-Eligibility Spenddown',      12, 'RADIO',    1, 400, 'Yes|No', 'Allow terminated/ineligible participants access to remaining balance'),
('hra_spenddown_events', 'Events Triggering Spenddown Access',    12, 'CHECKBOX', 0, 500, 'Termination (Non-Retirement)|Retirement|Death of Employee|Disability of Employee|Full Time to Part Time|USERRA Leave', NULL),
('hra_spenddown_months', 'Number of Months Allowed for Spenddown',12, 'NUMBER',   0, 600, NULL, NULL),
('hra_spenddown_pct',    'Spenddown Percentage of Accumulated Balance', 12, 'NUMBER', 0, 700, NULL, NULL);

-- Section 13: HSA Funding
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('hsa_funding_method',    'How Will You Fund Employee HSAs',  13, 'SELECT', 1, 100, 'We are not funding them|We will mail in a check as needed|We wish to fund electronically via EFT/ACH|We will wire the funds (carries a fee)', NULL),
('hsa_bank_name',         'Bank Name (for EFT)',              13, 'TEXT',   0, 200, NULL, NULL),
('hsa_bank_account',      'Account Number',                   13, 'TEXT',   0, 300, NULL, NULL),
('hsa_bank_routing',      'Routing Number',                   13, 'TEXT',   0, 400, NULL, NULL),
('hsa_bank_account_type', 'Account Type',                     13, 'RADIO',  0, 500, 'Checking|Savings', NULL);

-- Section 14: Transit / Commuter
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('transit_parking_reimb',      'Parking Reimbursement',          14, 'BOOLEAN', 0, 100, NULL, NULL),
('transit_transit_reimb',      'Transit Reimbursement',          14, 'BOOLEAN', 0, 200, NULL, NULL),
('transit_parking_conversion', 'Parking Conversion (Pre-Tax)',   14, 'BOOLEAN', 0, 300, NULL, 'Employee pre-tax salary reduction for parking'),
('transit_transit_conversion', 'Transit Conversion (Pre-Tax)',   14, 'BOOLEAN', 0, 400, NULL, 'Employee pre-tax salary reduction for transit'),
('transit_election_changes',   'How Often Can Employees Change Election Amounts', 14, 'SELECT', 1, 500, 'Monthly|Quarterly|Semi-Annually|Annually', NULL);

-- Section 15: Billing Administration (JSON plan builder only)
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('bill_benefit_plans', 'Benefit Plans', 15, 'JSON', 1, 50, NULL, 'Add each benefit plan that will be billed under this arrangement.');

-- Section 16: Payment Services
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('pay_facilitate_payments', 'Do You Want Us to Facilitate Reimbursement Payments on Your Behalf', 16, 'RADIO', 1, 100, 'Yes|No', 'We can mail checks and/or initiate direct deposits to participants'),
('pay_method_preference',  'Preferred Reimbursement Method', 16, 'SELECT', 0, 200, 'Check|Direct Deposit|Either (participant choice)', NULL);

-- Section 17: Debit Card Services
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('card_acknowledge_substantiation', 'I understand the substantiation requirements for benefit debit cards', 17, 'BOOLEAN', 1, 100, NULL, 'Unsubstantiated transactions will cause a temporary card block after 30 days. Checking this will display the remaining card setup fields.'),
('card_company_name',       'Company Name to Appear on Cards', 17, 'TEXT',     0, 200, NULL, NULL),
('card_contact_name',       'Debit Card Contact Name',         17, 'TEXT',     0, 300, NULL, 'Person we should contact for card-related questions'),
('card_contact_email',      'Debit Card Contact Email',        17, 'TEXT',     0, 400, NULL, 'For multiple contacts, separate email addresses with a semicolon (;)'),
('card_has_copay_medical',  'Do You Offer Medical Plans With Co-Pays', 17, 'RADIO', 0, 500, 'Yes|No', NULL),
('card_med_copay_details',  'Medical Co-Pay Details',          17, 'TEXTAREA', 0, 600, NULL, 'List plan names and their co-pay amounts'),
('card_has_copay_dental',   'Do You Offer Dental Plans With Co-Pays', 17, 'RADIO', 0, 700, 'Yes|No', NULL),
('card_dental_copay_details','Dental Co-Pay Details',          17, 'TEXTAREA', 0, 800, NULL, NULL),
('card_has_copay_vision',   'Do You Offer Vision Plans With Co-Pays', 17, 'RADIO', 0, 900, 'Yes|No', NULL),
('card_vision_copay_details','Vision Co-Pay Details',          17, 'TEXTAREA', 0, 1000, NULL, NULL);

-- Section 18: LSA Plan Design
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('lsa_eligible_expenses', 'Categories of Eligible Expenses',    18, 'CHECKBOX', 1, 100, 'Fitness / Gym Memberships|Wellness Programs|Financial Planning|Student Loan Repayment|Professional Development|Childcare / Eldercare|Home Office Equipment|Transportation|Other', NULL),
('lsa_eligible_other',    'Other Eligible Expenses',            18, 'TEXT',     0, 200, NULL, 'Describe any additional categories'),
('lsa_annual_allowance',  'Annual LSA Allowance per Employee',  18, 'NUMBER',  1, 300, NULL, 'Dollar amount available per plan year'),
('lsa_benefit_structure',  'Benefit Amount Structure',           18, 'RADIO',   1, 400, 'Same amount for all employees|Varies by class or tier', NULL),
('lsa_tier_details',       'Tier / Class Details',               18, 'TEXTAREA',0, 500, NULL, 'If varies — describe each tier and amount'),
('lsa_carryover',          'Allow Unused Balance to Carry Over', 18, 'RADIO',   1, 600, 'Yes|No', NULL);

-- Section 19: Adoption Assistance
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('adopt_max_benefit',       'Maximum Benefit per Adoption',                19, 'NUMBER',   1, 100, NULL, NULL),
('adopt_eligible_expenses', 'Eligible Expenses',                           19, 'CHECKBOX', 1, 200, 'Legal fees|Court costs|Agency fees|Travel expenses|Home study fees|Other', NULL),
('adopt_eligible_other',    'Other Eligible Expenses',                     19, 'TEXT',     0, 300, NULL, NULL),
('adopt_domestic_only',     'Limit to Domestic Adoptions Only',            19, 'RADIO',   1, 400, 'Yes — domestic only|No — domestic and international', NULL),
('adopt_waiting_period',    'Waiting Period Before Benefit Eligible',      19, 'SELECT',  0, 500, 'None — immediately eligible|Same as plan eligibility|Other', NULL),
('adopt_notes',             'Additional Notes',                            19, 'TEXTAREA',0, 600, NULL, NULL);

-- Section 20: COBRA-Specific Requirements
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options, help_text) VALUES
('bill_coverage_end',       'When Does Coverage End After Termination',       20, 'SELECT',   1, 100, 'End of the Month|Date of Termination|15th of the Month|15th or End of Month', NULL),
('bill_state_flags',        'State-Specific Requirements (Check All That Apply)', 20, 'CHECKBOX', 0, 200, 'Employees or locations in California|Health plans written in Illinois|Union administers COBRA for union employees|Sponsor an HMO health plan|None of the above', NULL),
('bill_current_activity',   'Current Billing Activity (Check All That Apply)',    20, 'CHECKBOX', 0, 300, 'One or more active billed participants|Participants notified but within election window|Participants with qualifying event but not yet notified|None of the above', NULL),
('bill_send_initial_notice','Should We Mail an Initial Notice to Your Existing Employees', 20, 'RADIO', 1, 400, 'Yes|No', NULL);

-- =============================================================================
-- PART 5: IRS LIMITS TABLE
-- =============================================================================

CREATE TABLE IF NOT EXISTS irslimit (
    limit_key VARCHAR(50) NOT NULL,
    plan_year INT NOT NULL,
    amount DOUBLE NOT NULL,
    description VARCHAR(200),
    PRIMARY KEY (limit_key, plan_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2025 limits
INSERT INTO irslimit (limit_key, plan_year, amount, description) VALUES
('FSA_HEALTH_MAX',  2025, 3300,  'Health FSA Maximum Annual Election'),
('FSA_DEPCARE_MAX', 2025, 5000,  'Dependent Care FSA Maximum Annual Election'),
('FSA_CARRYOVER',   2025, 660,   'Health FSA Maximum Carryover Amount'),
('HSA_SINGLE',      2025, 4300,  'HSA Annual Contribution Limit — Self-Only'),
('HSA_FAMILY',      2025, 8550,  'HSA Annual Contribution Limit — Family'),
('HSA_CATCHUP',     2025, 1000,  'HSA Catch-Up Contribution (age 55+)'),
('TRANSIT_MONTHLY', 2025, 325,   'Qualified Transit/Parking Monthly Limit'),
('ADOPTION_MAX',    2025, 17280, 'Adoption Assistance Maximum Exclusion'),
('QSEHRA_SINGLE',   2025, 6150,  'QSEHRA Maximum Annual — Self-Only'),
('QSEHRA_FAMILY',   2025, 12450, 'QSEHRA Maximum Annual — Family'),
('EBHRA_MAX',       2025, 2150,  'Excepted Benefit HRA Maximum Annual');

-- 2026 limits (published)
INSERT INTO irslimit (limit_key, plan_year, amount, description) VALUES
('FSA_HEALTH_MAX',  2026, 3400,  'Health FSA Maximum Annual Election'),
('FSA_DEPCARE_MAX', 2026, 7500,  'Dependent Care FSA Maximum Annual Election (increased by OBBBA)'),
('FSA_CARRYOVER',   2026, 680,   'Health FSA Maximum Carryover Amount'),
('HSA_SINGLE',      2026, 4400,  'HSA Annual Contribution Limit — Self-Only'),
('HSA_FAMILY',      2026, 8750,  'HSA Annual Contribution Limit — Family'),
('HSA_CATCHUP',     2026, 1000,  'HSA Catch-Up Contribution (age 55+)'),
('TRANSIT_MONTHLY', 2026, 340,   'Qualified Transit/Parking Monthly Limit'),
('ADOPTION_MAX',    2026, 17670, 'Adoption Assistance Maximum Exclusion'),
('QSEHRA_SINGLE',   2026, 6450,  'QSEHRA Maximum Annual — Self-Only'),
('QSEHRA_FAMILY',   2026, 13100, 'QSEHRA Maximum Annual — Family'),
('EBHRA_MAX',       2026, 2200,  'Excepted Benefit HRA Maximum Annual');

-- =============================================================================
-- PART 6: BENEFIT TYPE & BILLING TYPE TABLES
-- =============================================================================

CREATE TABLE IF NOT EXISTS billingtype (
    billingtype_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    psp_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO billingtype (billingtype_id, name, psp_id, sort_order) VALUES
(1, 'Tiered Rates',          4, 1),
(2, 'Age-Rated',             4, 2),
(3, 'Flat Rate',             4, 3),
(4, 'Age and Gender Rated',  4, 4),
(5, 'Individually Rated',    4, 5);

CREATE TABLE IF NOT EXISTS benefittype (
    benefittype_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    default_billingtype_id BIGINT,
    psp_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    FOREIGN KEY (default_billingtype_id) REFERENCES billingtype(billingtype_id),
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO benefittype (benefittype_id, name, default_billingtype_id, psp_id, sort_order) VALUES
(1,  'Medical',                 1, 4, 1),
(2,  'Dental',                  1, 4, 2),
(3,  'Vision',                  1, 4, 3),
(4,  'Life Insurance',          2, 4, 4),
(5,  'Short-Term Disability',   2, 4, 5),
(6,  'Long-Term Disability',    2, 4, 6),
(7,  'EAP',                     3, 4, 7),
(8,  'HRA',                     3, 4, 8),
(9,  'FSA',                     3, 4, 9),
(10, 'HSA',                     3, 4, 10),
(11, 'Other',                   3, 4, 11);

-- =============================================================================
-- PART 7: S3/WASABI CONSTANTS (values must be filled in manually)
-- Only run if constants don't already exist on production
-- =============================================================================

INSERT IGNORE INTO constant (name, value, note) VALUES
('S3_ENDPOINT',   '', 'Wasabi S3 endpoint URL — fill in before use'),
('S3_BUCKET',     '', 'Wasabi bucket name — fill in before use'),
('S3_ACCESS_KEY', '', 'Wasabi access key — fill in before use'),
('S3_SECRET_KEY', '', 'Wasabi secret key — fill in before use');

-- =============================================================================
-- END OF SESSION 3 MIGRATION
-- =============================================================================
