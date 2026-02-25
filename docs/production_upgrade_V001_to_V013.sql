-- =============================================================================
-- PRODUCTION UPGRADE SCRIPT: V001 (partial) through V013
-- Date: February 25, 2026
-- Target: production beta_ssa schema at superiorstate.biz
--
-- PRODUCTION STATE AT TIME OF WRITING:
--   V001 PARTIALLY applied:
--     ✅ datakey renamed to applicationfield (with new columns)
--     ✅ proposal columns added (status, created_by, date_sent, date_viewed, date_applied)
--     ❌ application columns NOT added
--     ❌ New tables NOT created (feature, ratediscount, etc.)
--   V002-V013: None applied
--   No schema_version table exists
--
-- THIS SCRIPT:
--   1. Completes V001 (skipping already-applied parts)
--   2. Applies V002 through V013 in order
--   3. Creates schema_version table and registers all versions
--
-- HOW TO RUN:
--   mysql -u root -p --socket=/var/run/mysqld/mysqld.sock beta_ssa < production_upgrade_V001_to_V013.sql
--   (Prefix with LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu if Acronis blocks)
--
-- IMPORTANT: Take a backup BEFORE running this script!
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- =============================================================================
-- V001: COMPLETE THE PARTIAL APPLICATION (Sales Pipeline)
-- =============================================================================

-- V001 Step 1: datakey rename — ALREADY DONE, SKIP
-- V001 Step 2: proposal columns — ALREADY DONE, SKIP

-- V001 Step 3: application columns — NOT YET APPLIED
ALTER TABLE application
  ADD COLUMN status VARCHAR(20) DEFAULT 'IN_PROGRESS',
  ADD COLUMN date_started TIMESTAMP NULL,
  ADD COLUMN date_submitted TIMESTAMP NULL,
  ADD COLUMN date_reviewed TIMESTAMP NULL,
  ADD COLUMN reviewed_by BIGINT,
  ADD COLUMN review_notes TEXT;

-- V001 Step 4: New tables
CREATE TABLE feature (
    feature_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(500) NOT NULL,
    sort_order INT,
    module_id BIGINT NOT NULL,
    psp_id BIGINT NOT NULL,
    FOREIGN KEY (module_id) REFERENCES servicemodule(module_id),
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ratediscount (
    ratediscount_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(200) NOT NULL,
    discount_amount DOUBLE NOT NULL,
    rate_id BIGINT NOT NULL,
    price_item_id BIGINT NOT NULL,
    FOREIGN KEY (rate_id) REFERENCES rate(rate_id),
    FOREIGN KEY (price_item_id) REFERENCES priceitem(price_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE ratediscountlos (
    ratediscount_id BIGINT NOT NULL,
    los_id BIGINT NOT NULL,
    PRIMARY KEY (ratediscount_id, los_id),
    FOREIGN KEY (ratediscount_id) REFERENCES ratediscount(ratediscount_id),
    FOREIGN KEY (los_id) REFERENCES los(los_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE marketingmaterial (
    material_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    material_type VARCHAR(20) NOT NULL,
    url VARCHAR(500),
    storage_guid VARCHAR(36),
    audience VARCHAR(20),
    sort_order INT,
    psp_id BIGINT NOT NULL,
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE materialmodule (
    material_id BIGINT NOT NULL,
    module_id BIGINT NOT NULL,
    PRIMARY KEY (material_id, module_id),
    FOREIGN KEY (material_id) REFERENCES marketingmaterial(material_id),
    FOREIGN KEY (module_id) REFERENCES servicemodule(module_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE applicationfieldvalue (
    field_value_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    field_key VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
    field_value TEXT,
    FOREIGN KEY (application_id) REFERENCES application(proposal_id),
    FOREIGN KEY (field_key) REFERENCES applicationfield(field_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


-- =============================================================================
-- V002: Proposal source_activity_id
-- =============================================================================

ALTER TABLE proposal ADD COLUMN source_activity_id BIGINT NULL;


-- =============================================================================
-- V003: LOS Expansion, Application Sections, IRS Limits, Benefit/Billing Types
-- =============================================================================

-- Delete old HRA/MERP LOS (ID 7) — clean up before expansion
DELETE FROM losmodules WHERE los_id = 7;
DELETE FROM los WHERE los_id = 7;

-- New service modules
INSERT INTO servicemodule (module_id, description, short_text, sort_order, psp_id) VALUES
(31, 'Lifestyle Spending Accounts', 'LSA', 31, 4),
(32, 'Adoption Assistance', 'ADOPT', 32, 4);

-- Expanded LOS entries
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

-- Application Section model
CREATE TABLE applicationsection (
    section_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    scope VARCHAR(10) NOT NULL DEFAULT 'ALL',
    sort_order INT NOT NULL DEFAULT 0,
    psp_id BIGINT NOT NULL,
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE applicationsectionlos (
    section_id BIGINT NOT NULL,
    los_id BIGINT NOT NULL,
    PRIMARY KEY (section_id, los_id),
    FOREIGN KEY (section_id) REFERENCES applicationsection(section_id),
    FOREIGN KEY (los_id) REFERENCES los(los_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed application sections (20 sections)
INSERT INTO applicationsection (section_id, name, description, scope, sort_order, psp_id) VALUES
(1, 'Company Information', 'Legal business name, EIN, and basic company details.', 'ALL', 100, 4),
(2, 'Primary Contact', 'Main point of contact for benefits administration.', 'ALL', 200, 4),
(3, 'Mailing Address', 'Company mailing address for correspondence.', 'ALL', 300, 4),
(4, 'Plan Year', 'Benefit plan year start and end dates.', 'ALL', 400, 4),
(5, 'Pay Cycle', 'Payroll frequency and pay dates.', 'ALL', 500, 4),
(6, 'Signing Officer', 'Authorized signer for plan documents.', 'ALL', 600, 4),
(7, 'Bank Information', 'Banking details for employer contributions and reimbursements.', 'ALL', 700, 4),
(8, 'Pre-Tax Elections', 'Section 125 pre-tax election preferences.', 'LOS', 800, 4),
(9, 'Section 125 Plan Features', 'Cafeteria plan design options and features.', 'LOS', 900, 4),
(10, 'FSA Plan Design', 'Flexible Spending Account plan parameters and limits.', 'LOS', 1000, 4),
(11, 'HRA Plan Design', 'Health Reimbursement Arrangement plan structure and eligible expenses.', 'LOS', 1100, 4),
(12, 'HRA Carryover & Runout', 'Carryover rules, runout periods, and rollover options for HRA plans.', 'LOS', 1200, 4),
(13, 'HSA Funding', 'Health Savings Account employer contribution details.', 'LOS', 1300, 4),
(14, 'Transit & Parking', 'Qualified transportation and parking benefit details.', 'LOS', 1400, 4),
(15, 'Billing Administration', 'Benefit plans and rate structures for billing arrangements.', 'LOS', 1500, 4),
(16, 'Payment Services', 'Reimbursement payment method preferences.', 'LOS', 1600, 4),
(17, 'Debit Card Services', 'Debit card setup for participant benefit access.', 'LOS', 1700, 4),
(18, 'LSA Plan Design', 'Lifestyle Spending Account eligible expenses and limits.', 'LOS', 1800, 4),
(19, 'Adoption Assistance', 'Adoption assistance benefit design and limits.', 'LOS', 1900, 4),
(20, 'COBRA-Specific Requirements', 'Termination rules, state requirements, and current COBRA activity.', 'LOS', 2000, 4);

-- Section ↔ LOS associations
INSERT INTO applicationsectionlos (section_id, los_id) VALUES
(8, 5), (8, 6),
(9, 5), (9, 6),
(10, 6),
(11, 11), (11, 12), (11, 13), (11, 14), (11, 15),
(12, 11), (12, 12), (12, 13), (12, 14), (12, 15),
(13, 9),
(14, 10),
(15, 16), (15, 17),
(16, 6), (16, 9), (16, 11), (16, 12), (16, 13), (16, 18),
(17, 6), (17, 9), (17, 11), (17, 12), (17, 13), (17, 18),
(18, 18),
(19, 19),
(20, 8);

-- Seed application fields (~95 fields across sections)
INSERT INTO applicationfield (field_key, label, template_purpose_id, field_type, is_required, sort_order, select_options) VALUES
('company_legal_name',   'Legal Company Name',                            1, 'TEXT', 1, 100, NULL),
('company_dba',          'DBA (Doing Business As)',                       1, 'TEXT', 0, 200, NULL),
('company_ein',          'Employer Identification Number (EIN)',          1, 'TEXT', 1, 300, NULL),
('company_employees',    'Number of Employees',                          1, 'NUMBER', 1, 400, NULL),
('company_eligible',     'Number of Eligible Employees',                 1, 'NUMBER', 0, 500, NULL),
('company_phone',        'Company Phone Number',                         1, 'TEXT', 1, 600, NULL),
('company_website',      'Company Website',                              1, 'TEXT', 0, 700, NULL),
('contact_name',         'Contact Name',                                 2, 'TEXT', 1, 100, NULL),
('contact_title',        'Title',                                        2, 'TEXT', 0, 200, NULL),
('contact_email',        'Email Address',                                2, 'EMAIL', 1, 300, NULL),
('contact_phone',        'Phone Number',                                 2, 'TEXT', 1, 400, NULL),
('contact_fax',          'Fax Number',                                   2, 'TEXT', 0, 500, NULL),
('address_street1',      'Street Address Line 1',                        3, 'TEXT', 1, 100, NULL),
('address_street2',      'Street Address Line 2',                        3, 'TEXT', 0, 200, NULL),
('address_city',         'City',                                         3, 'TEXT', 1, 300, NULL),
('address_state',        'State',                                        3, 'TEXT', 1, 400, NULL),
('address_zip',          'ZIP Code',                                     3, 'TEXT', 1, 500, NULL),
('plan_year_start',      'Plan Year Start Date',                         4, 'DATE', 1, 100, NULL),
('plan_year_end',        'Plan Year End Date',                           4, 'DATE', 1, 200, NULL),
('plan_year_short',      'Short Plan Year?',                             4, 'RADIO', 0, 300, 'Yes|No'),
('plan_year_short_start','Short Plan Year Start Date',                   4, 'DATE', 0, 400, NULL),
('pay_frequency',        'Payroll Frequency',                            5, 'SELECT', 1, 100, 'Weekly|Bi-Weekly|Semi-Monthly|Monthly'),
('pay_periods',          'Number of Pay Periods Per Year',               5, 'NUMBER', 1, 200, NULL),
('pay_first_date',       'First Payroll Deduction Date',                 5, 'DATE', 0, 300, NULL),
('signer_name',          'Authorized Signer Name',                       6, 'TEXT', 1, 100, NULL),
('signer_title',         'Authorized Signer Title',                      6, 'TEXT', 1, 200, NULL),
('bank_name',            'Bank Name',                                    7, 'TEXT', 0, 100, NULL),
('bank_routing',         'Routing Number',                               7, 'TEXT', 0, 200, NULL),
('bank_account',         'Account Number',                               7, 'TEXT', 0, 300, NULL),
('bank_account_type',    'Account Type',                                 7, 'SELECT', 0, 400, 'Checking|Savings'),
('pretax_125_exists',    'Does the Employer Currently Have a Section 125 Plan?', 8, 'RADIO', 1, 100, 'Yes|No'),
('pretax_125_provider',  'Current Section 125 Plan Provider',            8, 'TEXT', 0, 200, NULL),
('pretax_effective_date', 'Pre-Tax Effective Date',                      8, 'DATE', 0, 300, NULL),
('sec125_allow_midyear', 'Allow Mid-Year Election Changes (Beyond Qualifying Events)?', 9, 'RADIO', 0, 100, 'Yes|No'),
('sec125_carryover',     'Offer Health FSA Carryover?',                  9, 'RADIO', 0, 200, 'Yes|No'),
('sec125_grace_period',  'Offer Grace Period Instead of Carryover?',     9, 'RADIO', 0, 300, 'Yes|No|N/A'),
('sec125_runout',        'Post-Plan Year Runout Period (Days)',          9, 'NUMBER', 0, 400, NULL),
('fsa_health_max',       'Health FSA Maximum Annual Election',           10, 'NUMBER', 0, 100, NULL),
('fsa_health_min',       'Health FSA Minimum Annual Election',           10, 'NUMBER', 0, 200, NULL),
('fsa_depcare_max',      'Dependent Care FSA Maximum Annual Election',   10, 'NUMBER', 0, 300, NULL),
('fsa_depcare_min',      'Dependent Care FSA Minimum Annual Election',   10, 'NUMBER', 0, 400, NULL),
('fsa_er_contribution',  'Employer FSA Seed/Match Contribution',         10, 'TEXT', 0, 500, NULL),
('fsa_terminate_rule',   'Terminated Employee FSA Run-out Rule',         10, 'SELECT', 0, 600, 'Claims through termination date only|30-day runout from termination|End of plan year runout|COBRA continuation available'),
('hra_eligible_expenses','HRA Eligible Expense Categories',              11, 'CHECKBOX', 0, 100, 'Medical|Dental|Vision|Prescription|All 213(d) Expenses'),
('hra_annual_max',       'HRA Maximum Annual Benefit',                   11, 'NUMBER', 0, 200, NULL),
('hra_annual_min',       'HRA Minimum Annual Benefit',                   11, 'NUMBER', 0, 300, NULL),
('hra_er_funding',       'Employer Funding Amount',                      11, 'NUMBER', 0, 400, NULL),
('hra_funding_schedule', 'Employer Funding Schedule',                    11, 'SELECT', 0, 500, 'Lump sum at plan start|Quarterly|Monthly|Per pay period'),
('hra_deductible_req',   'Require Deductible Met Before HRA Pays?',     11, 'RADIO', 0, 600, 'Yes|No'),
('hra_carryover_allowed','Allow Carryover of Unused HRA Balance?',       12, 'RADIO', 0, 100, 'Yes|No'),
('hra_carryover_max',    'Maximum Carryover Amount',                     12, 'NUMBER', 0, 200, NULL),
('hra_carryover_type',   'Carryover Type',                               12, 'SELECT', 0, 300, 'Dollar amount|Percentage of unused|Unlimited'),
('hra_runout_days',      'Post-Termination Runout Period (Days)',         12, 'NUMBER', 0, 400, NULL),
('hra_term_forfeit',     'Forfeit Unused Balance on Termination?',       12, 'RADIO', 0, 500, 'Yes|No'),
('hsa_er_contribution',  'Employer HSA Contribution Amount',             13, 'NUMBER', 0, 100, NULL),
('hsa_er_frequency',     'Employer Contribution Frequency',              13, 'SELECT', 0, 200, 'Lump sum|Monthly|Per pay period|Quarterly'),
('hsa_er_match',         'Employer Match (if applicable)',               13, 'TEXT', 0, 300, NULL),
('hsa_custodian',        'Current/Preferred HSA Custodian',              13, 'TEXT', 0, 400, NULL),
('hsa_investment_threshold','Investment Threshold Amount',               13, 'NUMBER', 0, 500, NULL),
('transit_monthly_max',  'Monthly Transit Election Maximum',             14, 'NUMBER', 0, 100, NULL),
('parking_monthly_max',  'Monthly Parking Election Maximum',             14, 'NUMBER', 0, 200, NULL),
('transit_pretax',       'Offer Pre-Tax Transit Benefits?',              14, 'RADIO', 0, 300, 'Yes|No'),
('transit_er_subsidy',   'Employer Transit Subsidy (if any)',            14, 'TEXT', 0, 400, NULL),
('bill_plan_types',      'Benefit Plans to be Billed (Check All That Apply)', 15, 'CHECKBOX', 0, 100, 'Medical|Dental|Vision|Life|Disability|Voluntary Benefits|Other'),
('bill_rate_structure',  'Rate Structure',                               15, 'SELECT', 0, 200, 'Composite|Age-Banded|Tiered (EE/ES/EC/EF)|Other'),
('bill_payment_method',  'Employer Payment Method',                      15, 'SELECT', 0, 300, 'ACH|Check|Wire Transfer'),
('bill_frequency',       'Billing Frequency',                            15, 'SELECT', 0, 400, 'Monthly|Quarterly|Semi-Annually|Annually'),
('pay_method_preference','Preferred Reimbursement Payment Method',       16, 'SELECT', 0, 100, 'Direct Deposit (ACH)|Check|Payroll Integration'),
('pay_direct_deposit',   'Offer Participant Direct Deposit?',            16, 'RADIO', 0, 200, 'Yes|No'),
('pay_check_address',    'Reimbursement Check Mailing',                  16, 'SELECT', 0, 300, 'To participant home address|To employer for distribution'),
('card_requested',       'Request Debit Cards for Participants?',        17, 'RADIO', 1, 100, 'Yes|No'),
('card_eligible_plans',  'Card-Eligible Plans',                          17, 'CHECKBOX', 0, 200, 'Health FSA|Dependent Care FSA|HRA|HSA|Transit/Parking'),
('card_dependent_cards', 'Issue Dependent Cards?',                       17, 'RADIO', 0, 300, 'Yes|No'),
('card_auto_issue',      'Auto-Issue Cards to New Enrollees?',           17, 'RADIO', 0, 400, 'Yes|No'),
('lsa_eligible_expenses','LSA Eligible Expense Categories',              18, 'CHECKBOX', 0, 100, 'Fitness & Gym|Wellness Programs|Mental Health|Financial Planning|Education|Childcare|Pet Care|Home Office|Other'),
('lsa_annual_max',       'LSA Maximum Annual Benefit',                   18, 'NUMBER', 0, 200, NULL),
('lsa_er_funding',       'Employer Funding Amount',                      18, 'NUMBER', 0, 300, NULL),
('lsa_funding_schedule', 'Funding Schedule',                             18, 'SELECT', 0, 400, 'Lump sum|Monthly|Quarterly|Per pay period'),
('lsa_carryover',        'Allow Carryover of Unused LSA Balance?',       18, 'RADIO', 0, 500, 'Yes|No'),
('adoption_max_benefit', 'Maximum Adoption Benefit Per Event',           19, 'NUMBER', 0, 100, NULL),
('adoption_eligible',    'Eligible Adoption Types',                      19, 'CHECKBOX', 0, 200, 'Domestic|International|Foster-to-Adopt|All'),
('adoption_expenses',    'Reimbursable Expense Categories',              19, 'CHECKBOX', 0, 300, 'Legal fees|Court costs|Agency fees|Travel|Home study|Other'),
('adoption_waiting_period','Waiting Period Before Benefit Available',    19, 'TEXT', 0, 400, NULL),
('cobra_admin_current',  'Current COBRA Administrator (if different)',   20, 'TEXT', 0, 100, NULL),
('cobra_state_reqs',     'State-Specific COBRA Requirements (Check All That Apply)', 20, 'CHECKBOX', 0, 200, 'Employees or locations in California|Health plans written in Illinois|Union administers COBRA for union employees|Sponsor an HMO health plan|None of the above'),
('bill_current_activity','Current Billing Activity (Check All That Apply)', 20, 'CHECKBOX', 0, 300, 'One or more active billed participants|Participants notified but within election window|Participants with qualifying event but not yet notified|None of the above'),
('bill_send_initial_notice','Should We Mail an Initial Notice to Your Existing Employees', 20, 'RADIO', 1, 400, 'Yes|No');

-- IRS Limits table
CREATE TABLE irslimit (
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

-- 2026 limits
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

-- Billing Type table
CREATE TABLE billingtype (
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
(5, 'Individual Rated',      4, 5);

-- Benefit Type table
CREATE TABLE benefittype (
    benefittype_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    psp_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO benefittype (benefittype_id, name, psp_id, sort_order) VALUES
(1, 'Medical',              4, 1),
(2, 'Dental',               4, 2),
(3, 'Vision',               4, 3),
(4, 'Life Insurance',       4, 4),
(5, 'Short-Term Disability', 4, 5),
(6, 'Long-Term Disability',  4, 6),
(7, 'Accident',             4, 7),
(8, 'Critical Illness',     4, 8),
(9, 'Hospital Indemnity',   4, 9),
(10, 'Legal',               4, 10),
(11, 'Identity Theft',      4, 11);

-- S3/Wasabi constants (INSERT IGNORE — fill in real values after running)
INSERT IGNORE INTO constant (name, value, note) VALUES
('S3_ENDPOINT',   'FILL_ME_IN', 'Wasabi S3 endpoint'),
('S3_BUCKET',     'FILL_ME_IN', 'Wasabi bucket name'),
('S3_ACCESS_KEY', 'FILL_ME_IN', 'Wasabi access key'),
('S3_SECRET_KEY', 'FILL_ME_IN', 'Wasabi secret key');


-- =============================================================================
-- V004: Service Manager (Enhancement, join tables, ServiceModule FKs, LOS columns)
-- =============================================================================

-- NOTE: Original script referenced psp(psp_id) which doesn't exist.
-- Corrected to reference assignee(id) with BIGINT type.
CREATE TABLE enhancement (
    enhancement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    description VARCHAR(200) NOT NULL,
    short_text VARCHAR(20) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    suppressed TINYINT(1) NOT NULL DEFAULT 0,
    psp_id BIGINT NOT NULL,
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE enhancement_los (
    enhancement_id BIGINT NOT NULL,
    los_id BIGINT NOT NULL,
    PRIMARY KEY (enhancement_id, los_id),
    FOREIGN KEY (enhancement_id) REFERENCES enhancement(enhancement_id),
    FOREIGN KEY (los_id) REFERENCES los(los_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE applicationsectionenhancement (
    section_id BIGINT NOT NULL,
    enhancement_id BIGINT NOT NULL,
    PRIMARY KEY (section_id, enhancement_id),
    FOREIGN KEY (section_id) REFERENCES applicationsection(section_id),
    FOREIGN KEY (enhancement_id) REFERENCES enhancement(enhancement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ServiceModule FKs
ALTER TABLE servicemodule ADD COLUMN los_id BIGINT NULL;
ALTER TABLE servicemodule ADD COLUMN enhancement_id BIGINT NULL;
ALTER TABLE servicemodule ADD CONSTRAINT fk_sm_los FOREIGN KEY (los_id) REFERENCES los(los_id);
ALTER TABLE servicemodule ADD CONSTRAINT fk_sm_enhancement FOREIGN KEY (enhancement_id) REFERENCES enhancement(enhancement_id);

-- LOS new columns
ALTER TABLE los ADD COLUMN sort_order INT NOT NULL DEFAULT 0;
ALTER TABLE los ADD COLUMN suppressed TINYINT(1) NOT NULL DEFAULT 0;

-- Seed enhancements
INSERT INTO enhancement (enhancement_id, description, short_text, sort_order, suppressed, psp_id) VALUES
    (1, 'Debit Card Services', 'Cards', 100, 0, 4),
    (2, 'Payment Services', 'Payment', 200, 0, 4),
    (3, 'Document Services', 'Docs', 300, 0, 4),
    (4, 'Multi-Plan Discounts', 'Discounts', 400, 0, 4);

-- Enhancement ↔ LOS associations
INSERT INTO enhancement_los (enhancement_id, los_id) VALUES
    (1,6),(1,11),(1,9),(1,8),(1,10),(1,12),(1,13),(1,18),
    (2,6),(2,11),(2,9),(2,8),(2,10),(2,12),(2,13),(2,18),
    (3,6),(3,11),(3,9),(3,8),(3,12),
    (4,6),(4,11),(4,9),(4,8),(4,10);

-- Backfill ServiceModule FKs
UPDATE servicemodule SET los_id = 5 WHERE short_text = 'POP' AND los_id IS NULL;
UPDATE servicemodule SET los_id = 6 WHERE short_text = 'FSA' AND los_id IS NULL;
UPDATE servicemodule SET los_id = 11 WHERE short_text = 'HRA' AND los_id IS NULL;
UPDATE servicemodule SET los_id = 9 WHERE short_text = 'HSA' AND los_id IS NULL;
UPDATE servicemodule SET los_id = 8 WHERE short_text = 'COBRA' AND los_id IS NULL;
UPDATE servicemodule SET los_id = 10 WHERE short_text = 'Transit' AND los_id IS NULL;

UPDATE servicemodule SET enhancement_id = 1 WHERE short_text = 'Cards' AND enhancement_id IS NULL;
UPDATE servicemodule SET enhancement_id = 2 WHERE short_text = 'Payment' AND enhancement_id IS NULL;
UPDATE servicemodule SET enhancement_id = 3 WHERE short_text = 'Docs' AND enhancement_id IS NULL;
UPDATE servicemodule SET enhancement_id = 4 WHERE short_text = 'Discounts' AND enhancement_id IS NULL;

-- ApplicationSection ↔ Enhancement links
INSERT INTO applicationsectionenhancement (section_id, enhancement_id) VALUES (16, 2), (17, 1);

-- LOS sort_order backfill
UPDATE los SET sort_order = 100 WHERE los_id = 5;
UPDATE los SET sort_order = 200 WHERE los_id = 6;
UPDATE los SET sort_order = 300 WHERE los_id = 11;
UPDATE los SET sort_order = 400 WHERE los_id = 12;
UPDATE los SET sort_order = 500 WHERE los_id = 13;
UPDATE los SET sort_order = 600 WHERE los_id = 14;
UPDATE los SET sort_order = 700 WHERE los_id = 15;
UPDATE los SET sort_order = 800 WHERE los_id = 9;
UPDATE los SET sort_order = 900 WHERE los_id = 8;
UPDATE los SET sort_order = 1000 WHERE los_id = 10;
UPDATE los SET sort_order = 1100 WHERE los_id = 16;
UPDATE los SET sort_order = 1200 WHERE los_id = 17;
UPDATE los SET sort_order = 1300 WHERE los_id = 18;
UPDATE los SET sort_order = 1400 WHERE los_id = 19;


-- =============================================================================
-- V005: Rate Manager — ratetable sort_order
-- =============================================================================

ALTER TABLE ratetable ADD COLUMN sort_order INT NOT NULL DEFAULT 0;

UPDATE ratetable rt
    INNER JOIN servicemodule sm ON rt.module_id = sm.module_id
SET rt.sort_order = sm.sort_order;


-- =============================================================================
-- V006: Invitation System
-- =============================================================================

ALTER TABLE agency ADD COLUMN manager_id BIGINT NULL;
ALTER TABLE agency ADD CONSTRAINT fk_agency_manager
  FOREIGN KEY (manager_id) REFERENCES assignee(id);

CREATE TABLE invitation (
  invitation_id BIGINT AUTO_INCREMENT PRIMARY KEY,
  guid VARCHAR(36) NOT NULL UNIQUE,
  email VARCHAR(200) NOT NULL,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  agency_id BIGINT NOT NULL,
  role VARCHAR(20) NOT NULL,
  invited_by BIGINT NOT NULL,
  person_id BIGINT NULL,
  date_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  date_expires TIMESTAMP NOT NULL,
  date_accepted TIMESTAMP NULL,
  is_used BOOLEAN DEFAULT FALSE,
  CONSTRAINT fk_invitation_agency FOREIGN KEY (agency_id) REFERENCES agency(agency_id),
  CONSTRAINT fk_invitation_invited_by FOREIGN KEY (invited_by) REFERENCES assignee(id),
  CONSTRAINT fk_invitation_person FOREIGN KEY (person_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- NOTE: Original script used column name "id" but production uses "role_id". Fixed here.
INSERT IGNORE INTO userrole (role_id, description) VALUES
  (1, 'PSP User'),
  (2, 'Agent'),
  (3, 'Client'),
  (4, 'Applicant'),
  (5, 'PSP Admin'),
  (8, 'Agency Admin'),
  (9, 'PSP Super User');


-- =============================================================================
-- V007: Resource Library
-- =============================================================================

CREATE TABLE resourcecategory (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    psp_id BIGINT NOT NULL,
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE marketingmaterial ADD COLUMN category_id BIGINT NULL;
ALTER TABLE marketingmaterial ADD CONSTRAINT fk_mm_category
    FOREIGN KEY (category_id) REFERENCES resourcecategory(category_id);

ALTER TABLE marketingmaterial MODIFY COLUMN storage_guid VARCHAR(50);

ALTER TABLE feature ADD COLUMN material_id BIGINT NULL;
ALTER TABLE feature ADD CONSTRAINT fk_feature_material
    FOREIGN KEY (material_id) REFERENCES marketingmaterial(material_id);


-- =============================================================================
-- V008: Opportunity System
-- =============================================================================

ALTER TABLE assignee ADD COLUMN prospect_id BIGINT NULL;
ALTER TABLE assignee ADD COLUMN agency_id_opp BIGINT NULL;
ALTER TABLE assignee ADD COLUMN opportunity_stage VARCHAR(30) NULL;
ALTER TABLE assignee ADD COLUMN estimated_employees INT NULL;
ALTER TABLE assignee ADD COLUMN estimated_value DOUBLE NULL;
ALTER TABLE assignee ADD COLUMN expected_close_date DATE NULL;

ALTER TABLE assignee ADD CONSTRAINT fk_opp_prospect
  FOREIGN KEY (prospect_id) REFERENCES prospect(prospect_id);
ALTER TABLE assignee ADD CONSTRAINT fk_opp_agency
  FOREIGN KEY (agency_id_opp) REFERENCES agency(agency_id);

-- Sales template group
INSERT IGNORE INTO templategroup (id, description) VALUES (5, 'Sales');

-- New Opportunity template purpose
INSERT IGNORE INTO templatepurpose (id, description, sort_order, template_group)
  VALUES (30, 'New Opportunity', 100, 5);

-- Sales tasks (900000+ range)
INSERT INTO task (id, description, allow_early, allow_future) VALUES
  (900001, 'Initial contact with prospect', 1, 1),
  (900002, 'Qualify prospect needs', 1, 1),
  (900003, 'Send proposal', 1, 1),
  (900004, 'Follow up on proposal', 1, 1),
  (900005, 'Close deal', 1, 1);

INSERT INTO tasksequence (id, description, DTYPE, purpose_id)
  VALUES (900001, 'New Opportunity Tasks', 'RequiredTaskList', 30);

INSERT INTO tasksequencetable (sequence_id, task_id, sort_order) VALUES
  (900001, 900001, 100),
  (900001, 900002, 200),
  (900001, 900003, 300),
  (900001, 900004, 400),
  (900001, 900005, 500);


-- =============================================================================
-- V009: Timeclock Correction
-- =============================================================================

CREATE TABLE time_correction_request (
    request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    requestor_id BIGINT NOT NULL,
    in_log_id BIGINT NOT NULL,
    out_log_id BIGINT NOT NULL,
    original_date DATE NOT NULL,
    original_in_time TIME NOT NULL,
    original_out_time TIME NOT NULL,
    requested_in_time TIME NULL,
    requested_out_time TIME NULL,
    correction_note VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    date_requested TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewer_id BIGINT NULL,
    review_comment VARCHAR(500) NULL,
    date_reviewed TIMESTAMP NULL,
    FOREIGN KEY (requestor_id) REFERENCES assignee(id),
    FOREIGN KEY (reviewer_id) REFERENCES assignee(id),
    FOREIGN KEY (in_log_id) REFERENCES timelog(log_id),
    FOREIGN KEY (out_log_id) REFERENCES timelog(log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_tcr_status ON time_correction_request (status);
CREATE INDEX idx_tcr_requestor ON time_correction_request (requestor_id);
CREATE INDEX idx_tcr_date ON time_correction_request (original_date);


-- =============================================================================
-- V010: PSP Opportunity Integration
-- =============================================================================

INSERT IGNORE INTO userrole (role_id, description) VALUES (9, 'PSP Sales');

ALTER TABLE assignee ADD COLUMN managed_by_id BIGINT NULL;
ALTER TABLE assignee ADD CONSTRAINT fk_opp_managed_by
  FOREIGN KEY (managed_by_id) REFERENCES assignee(id);


-- =============================================================================
-- V011: BPO Delegation Feature
-- =============================================================================

ALTER TABLE todo
    ADD COLUMN bpo_completed TINYINT(1) NOT NULL DEFAULT 0
        AFTER is_complete,
    ADD COLUMN bpo_completed_date DATE DEFAULT NULL
        AFTER bpo_completed,
    ADD COLUMN bpo_completed_by_id BIGINT DEFAULT NULL
        AFTER bpo_completed_date,
    ADD COLUMN bpo_assigned_to_id BIGINT DEFAULT NULL
        AFTER bpo_completed_by_id;

ALTER TABLE todo
    ADD CONSTRAINT fk_todo_bpo_completed_by
        FOREIGN KEY (bpo_completed_by_id) REFERENCES assignee(id),
    ADD CONSTRAINT fk_todo_bpo_assigned_to
        FOREIGN KEY (bpo_assigned_to_id) REFERENCES assignee(id);

CREATE TABLE todo_note (
    note_id BIGINT NOT NULL AUTO_INCREMENT,
    todo_id BIGINT NOT NULL,
    created_by_id BIGINT NOT NULL,
    note_text TEXT NOT NULL,
    source_type VARCHAR(20) DEFAULT 'PSP',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (note_id),
    CONSTRAINT fk_todonote_todo FOREIGN KEY (todo_id) REFERENCES todo(todo_id),
    CONSTRAINT fk_todonote_person FOREIGN KEY (created_by_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_todonote_todo ON todo_note(todo_id);
CREATE INDEX idx_todonote_created ON todo_note(created_at);

-- NOTE: Original script used column name "id" but production uses "role_id". Fixed here.
INSERT IGNORE INTO userrole (role_id, description) VALUES (101, 'Accelergent BPO');
INSERT IGNORE INTO userrole (role_id, description) VALUES (102, 'Accelergent BPO Admin');
INSERT IGNORE INTO userrole (role_id, description) VALUES (103, 'Accelergent BPO User');


-- =============================================================================
-- V012: Role Cleanup + PSP Branding Constants
-- =============================================================================

DELETE FROM userrole WHERE role_id IN (6, 7, 10);

UPDATE userrole SET description = 'BPO Admin' WHERE role_id = 102;
UPDATE userrole SET description = 'BPO User' WHERE role_id = 103;

INSERT IGNORE INTO constant (name, value) VALUES ('LOGO_NAVBAR', '/images/logoA.png');
INSERT IGNORE INTO constant (name, value) VALUES ('LOGO_LOGIN', '/images/logoD.png');
INSERT IGNORE INTO constant (name, value) VALUES ('FAVICON', '/favicon.ico');


-- =============================================================================
-- V013: User Filter Presets
-- =============================================================================

CREATE TABLE user_filter_preset (
    preset_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    slot_number INT NOT NULL,
    preset_name VARCHAR(50) NOT NULL,
    filter_type_renewal TINYINT(1) NOT NULL DEFAULT 1,
    filter_type_setup TINYINT(1) NOT NULL DEFAULT 1,
    filter_type_ticket TINYINT(1) NOT NULL DEFAULT 1,
    filter_type_opportunity TINYINT(1) NOT NULL DEFAULT 0,
    filter_attention_onus TINYINT(1) NOT NULL DEFAULT 0,
    filter_attention_contact TINYINT(1) NOT NULL DEFAULT 0,
    filter_owner VARCHAR(10) NOT NULL DEFAULT 'all',
    filter_sort VARCHAR(20) NOT NULL DEFAULT 'name',
    CONSTRAINT fk_preset_user FOREIGN KEY (user_id) REFERENCES user(user_id),
    CONSTRAINT uq_user_slot UNIQUE (user_id, slot_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed default presets for existing users (3 slots each)
INSERT INTO user_filter_preset (user_id, slot_number, preset_name,
    filter_type_renewal, filter_type_setup, filter_type_ticket, filter_type_opportunity,
    filter_attention_onus, filter_attention_contact, filter_owner, filter_sort)
SELECT u.user_id, 1, 'All Activities', 1, 1, 1, 0, 0, 0, 'all', 'name'
FROM user u;

INSERT INTO user_filter_preset (user_id, slot_number, preset_name,
    filter_type_renewal, filter_type_setup, filter_type_ticket, filter_type_opportunity,
    filter_attention_onus, filter_attention_contact, filter_owner, filter_sort)
SELECT u.user_id, 2, 'My Urgent', 1, 1, 1, 0, 1, 0, 'mine', 'due';

INSERT INTO user_filter_preset (user_id, slot_number, preset_name,
    filter_type_renewal, filter_type_setup, filter_type_ticket, filter_type_opportunity,
    filter_attention_onus, filter_attention_contact, filter_owner, filter_sort)
SELECT u.user_id, 3, 'Renewals', 1, 0, 0, 0, 0, 0, 'all', 'due';


-- =============================================================================
-- SCHEMA VERSION TABLE + REGISTRATION
-- =============================================================================

CREATE TABLE IF NOT EXISTS schema_version (
    version VARCHAR(10) NOT NULL,
    description VARCHAR(200),
    script_name VARCHAR(200),
    applied_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT IGNORE INTO schema_version (version, description, script_name) VALUES
('V001', 'Sales pipeline - tables, columns, entity renames', 'sales_pipeline_migration.sql'),
('V002', 'Sales pipeline 2 - proposal source_activity_id', 'sales_pipeline_migration_2.sql'),
('V003', 'Sales pipeline 3 - LOS expansion, app sections, IRS limits', 'sales_pipeline_migration_3.sql'),
('V004', 'Service manager - enhancement, join tables, SM FKs, LOS columns', 'service_manager_production_migration.sql'),
('V005', 'Rate manager - ratetable sort_order', 'rate_manager_session2_production_migration.sql'),
('V006', 'Invitation system - invitation table, agency manager_id', 'invitation_system_migration.sql'),
('V007', 'Resource library - category, material FK, feature FK', 'resource_library_production_migration.sql'),
('V008', 'Opportunity system - assignee columns, sales tasks', 'opportunity_migration_production.sql'),
('V009', 'Timeclock correction - request table', 'timeclock_correction_migration.sql'),
('V010', 'PSP opportunity integration - sales role, managed_by', 'V010__psp_opportunity_integration.sql'),
('V011', 'BPO delegation - todo BPO columns, todo_note, BPO roles', 'V011__bpo_delegation_feature.sql'),
('V012', 'Role cleanup and PSP branding constants', 'V012__role_cleanup_psp_branding_constants.sql'),
('V013', 'User filter presets - 3 slots per user', 'V013__user_filter_presets.sql');


-- =============================================================================
-- RESTORE SETTINGS
-- =============================================================================

SET SQL_SAFE_UPDATES = 1;
SET FOREIGN_KEY_CHECKS = 1;


-- =============================================================================
-- POST-RUN REMINDERS
-- =============================================================================

-- 1. Fill in S3/Wasabi constants:
--    UPDATE constant SET value = 'your-endpoint' WHERE name = 'S3_ENDPOINT';
--    UPDATE constant SET value = 'your-bucket'   WHERE name = 'S3_BUCKET';
--    UPDATE constant SET value = 'your-key'      WHERE name = 'S3_ACCESS_KEY';
--    UPDATE constant SET value = 'your-secret'   WHERE name = 'S3_SECRET_KEY';
--
-- 2. Verify with:
--    SELECT * FROM schema_version ORDER BY version;
--    SELECT COUNT(*) FROM enhancement;
--    SELECT COUNT(*) FROM applicationsection;
--    SELECT COUNT(*) FROM applicationfield;
--    SELECT COUNT(*) FROM irslimit;
--    DESCRIBE assignee;  -- should show prospect_id, agency_id_opp, opportunity_stage, managed_by_id
--    DESCRIBE todo;      -- should show bpo_* columns
--    DESCRIBE application; -- should show status, date_started, etc.
