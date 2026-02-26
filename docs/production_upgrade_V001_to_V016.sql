-- =============================================================================
-- PRODUCTION UPGRADE SCRIPT: V001 (partial) through V016
-- Date: February 26, 2026
-- Target: production beta_ssa schema at superiorstate.biz
--
-- PRODUCTION STATE AT TIME OF WRITING:
--   V001 PARTIALLY applied:
--     ✅ datakey renamed to applicationfield (with new columns)
--     ✅ proposal columns added (status, created_by, date_sent, date_viewed, date_applied)
--     ❌ application columns NOT added
--     ❌ New tables NOT created (feature, ratediscount, etc.)
--   V002-V016: None applied
--   No schema_version table exists
--
-- THIS SCRIPT:
--   1. Completes V001 (skipping already-applied parts)
--   2. Applies V002 through V016 in order
--   3. Creates schema_version table and registers all versions
--
-- CHANGES FROM PREVIOUS VERSION (production_upgrade_V001_to_V013.sql):
--   - V011 now includes todo_guid, todo.is_reverted, and task_guid columns
--     (these were added ad-hoc during BPO development, never in a migration script)
--     All three use DEFAULT (UUID()) or DEFAULT 0 for backward compatibility
--   - V014-V016 sections appended
--   - schema_version block extended through V016
--
-- HOW TO RUN:
--   mysql -u root -p --socket=/var/run/mysqld/mysqld.sock beta_ssa < production_upgrade_V001_to_V016.sql
--   (Prefix with LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu if Acronis blocks)
--
-- IMPORTANT: Take a backup BEFORE running this script!
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;
SET sql_log_bin = 0;
-- USE beta_ssa;  -- Uncomment or specify target schema before running


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
(13, 'HSA Plan Design', 'Health Savings Account eligibility requirements and contribution details.', 'LOS', 1300, 4),
(14, 'Transit/Parking Plan Design', 'Commuter benefit plan parameters and limits.', 'LOS', 1400, 4),
(15, 'Billing Plan Design', 'Retiree and direct billing plan parameters.', 'LOS', 1500, 4),
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

-- ApplicationField entity refactor: section_id replaces template_purpose_id, add help_text
-- Must be done before seeding fields so section_id can be populated in the INSERT
ALTER TABLE applicationfield ADD COLUMN section_id BIGINT NULL;
ALTER TABLE applicationfield ADD COLUMN help_text VARCHAR(500) NULL;
ALTER TABLE applicationfield ADD CONSTRAINT fk_appfield_section
    FOREIGN KEY (section_id) REFERENCES applicationsection(section_id);

-- Seed application fields (~95 fields across sections)
-- NOTE: section_id maps field to ApplicationSection (replaced old template_purpose_id)
INSERT INTO applicationfield (field_key, label, section_id, field_type, is_required, sort_order, select_options) VALUES
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
('plan_year_short',      'Short Plan Year?',                             4, 'RADIO', 0, 300, 'Yes,No'),
('plan_year_short_start','Short Plan Year Start Date',                   4, 'DATE', 0, 400, NULL),
('pay_frequency',        'Pay Frequency',                                5, 'SELECT', 1, 100, 'Weekly,Bi-Weekly,Semi-Monthly,Monthly'),
('pay_day',              'Pay Day',                                      5, 'TEXT', 0, 200, NULL),
('first_pay_date',       'First Pay Date of Plan Year',                  5, 'DATE', 0, 300, NULL),
('signer_name',          'Signing Officer Name',                         6, 'TEXT', 1, 100, NULL),
('signer_title',         'Title',                                        6, 'TEXT', 1, 200, NULL),
('signer_email',         'Email Address',                                6, 'EMAIL', 0, 300, NULL),
('signer_phone',         'Phone Number',                                 6, 'TEXT', 0, 400, NULL),
('bank_name',            'Bank Name',                                    7, 'TEXT', 0, 100, NULL),
('bank_routing',         'Routing Number',                               7, 'TEXT', 0, 200, NULL),
('bank_account',         'Account Number',                               7, 'TEXT', 0, 300, NULL),
('bank_account_type',    'Account Type',                                 7, 'SELECT', 0, 400, 'Checking,Savings'),
('pretax_medical',       'Medical Premium Pre-Tax',                      8, 'RADIO', 0, 100, 'Yes,No'),
('pretax_dental',        'Dental Premium Pre-Tax',                       8, 'RADIO', 0, 200, 'Yes,No'),
('pretax_vision',        'Vision Premium Pre-Tax',                       8, 'RADIO', 0, 300, 'Yes,No'),
('pretax_supplemental',  'Supplemental Insurance Pre-Tax',               8, 'RADIO', 0, 400, 'Yes,No'),
('pretax_group_life',    'Group Life Pre-Tax',                           8, 'RADIO', 0, 500, 'Yes,No'),
('s125_new_or_existing', 'New or Existing Section 125 Plan?',            9, 'RADIO', 0, 100, 'New Plan,Existing Plan'),
('s125_prior_tpa',       'Prior TPA (if existing)',                      9, 'TEXT', 0, 200, NULL),
('s125_effective_date',  'Section 125 Effective Date',                   9, 'DATE', 0, 300, NULL),
('s125_grace_period',    'Grace Period',                                 9, 'SELECT', 0, 400, 'None,2.5 Months,$610 Rollover'),
('fsa_medical_limit',    'Medical FSA Annual Limit',                    10, 'TEXT', 0, 100, NULL),
('fsa_dependent_limit',  'Dependent Care FSA Annual Limit',             10, 'TEXT', 0, 200, NULL),
('fsa_employer_contrib',  'Employer Contribution to FSA?',              10, 'RADIO', 0, 300, 'Yes,No'),
('fsa_employer_amount',  'Employer FSA Contribution Amount',            10, 'TEXT', 0, 400, NULL),
('fsa_runout_period',    'FSA Runout Period',                           10, 'SELECT', 0, 500, '90 Days,6 Months,12 Months'),
('fsa_min_election',     'Minimum Annual Election',                     10, 'TEXT', 0, 600, NULL),
('hra_plan_type',        'HRA Plan Type',                               11, 'SELECT', 0, 100, 'Integrated,Stand-Alone,Post-Deductible'),
('hra_eligible_expenses','Eligible Expense Categories',                 11, 'TEXTAREA', 0, 200, NULL),
('hra_annual_allowance', 'Annual Allowance per Employee',               11, 'TEXT', 0, 300, NULL),
('hra_family_allowance', 'Annual Allowance per Family',                 11, 'TEXT', 0, 400, NULL),
('hra_proration',        'Prorate New Hire Allowance?',                 11, 'RADIO', 0, 500, 'Yes,No'),
('hra_carryover',        'Carryover Unused Funds?',                     12, 'RADIO', 0, 100, 'Yes,No'),
('hra_carryover_limit',  'Carryover Limit',                             12, 'TEXT', 0, 200, NULL),
('hra_carryover_cap',    'Lifetime Carryover Cap',                      12, 'TEXT', 0, 300, NULL),
('hra_runout_period',    'Runout Period After Plan Year End',            12, 'SELECT', 0, 400, '90 Days,6 Months,12 Months'),
('hra_term_runout',      'Runout Period After Termination',             12, 'SELECT', 0, 500, 'None,30 Days,60 Days,90 Days'),
('hsa_employer_contrib', 'Employer HSA Contribution?',                  13, 'RADIO', 0, 100, 'Yes,No'),
('hsa_ee_amount',        'Employer Contribution (Employee)',            13, 'TEXT', 0, 200, NULL),
('hsa_fam_amount',       'Employer Contribution (Family)',              13, 'TEXT', 0, 300, NULL),
('hsa_custodian',        'HSA Custodian/Bank',                          13, 'TEXT', 0, 400, NULL),
('hsa_catch_up',         'Allow Catch-Up Contributions (55+)?',        13, 'RADIO', 0, 500, 'Yes,No'),
('transit_monthly_limit','Monthly Transit Limit',                       14, 'TEXT', 0, 100, NULL),
('parking_monthly_limit','Monthly Parking Limit',                       14, 'TEXT', 0, 200, NULL),
('transit_employer_sub', 'Employer Subsidy?',                           14, 'RADIO', 0, 300, 'Yes,No'),
('transit_sub_amount',   'Employer Subsidy Amount',                     14, 'TEXT', 0, 400, NULL),
('billing_type',         'Billing Type',                                15, 'SELECT', 0, 100, 'Self-Bill,List Bill'),
('billing_frequency',    'Billing Frequency',                           15, 'SELECT', 0, 200, 'Monthly,Quarterly'),
('billing_due_day',      'Payment Due Day of Month',                    15, 'TEXT', 0, 300, NULL),
('billing_contact_name', 'Billing Contact Name',                        15, 'TEXT', 0, 400, NULL),
('billing_contact_email','Billing Contact Email',                       15, 'EMAIL', 0, 500, NULL),
('payment_method',       'Reimbursement Payment Method',                16, 'SELECT', 0, 100, 'Direct Deposit,Check,Payroll'),
('payment_schedule',     'Payment Schedule',                            16, 'SELECT', 0, 200, 'Weekly,Bi-Weekly,Semi-Monthly,Monthly'),
('payment_min_amount',   'Minimum Reimbursement Amount',                16, 'TEXT', 0, 300, NULL),
('debit_card_needed',    'Debit Cards Needed?',                         17, 'RADIO', 0, 100, 'Yes,No'),
('debit_card_accounts',  'Accounts Linked to Card',                     17, 'CHECKBOX', 0, 200, 'FSA,HRA,HSA,Transit'),
('debit_card_dependents','Issue Dependent Cards?',                      17, 'RADIO', 0, 300, 'Yes,No'),
('debit_card_count',     'Number of Cards Needed',                      17, 'NUMBER', 0, 400, NULL),
('lsa_eligible_expenses','Eligible Expense Categories',                 18, 'TEXTAREA', 0, 100, NULL),
('lsa_annual_allowance', 'Annual Allowance per Employee',               18, 'TEXT', 0, 200, NULL),
('lsa_proration',        'Prorate New Hire Allowance?',                 18, 'RADIO', 0, 300, 'Yes,No'),
('lsa_carryover',        'Carryover Unused Funds?',                     18, 'RADIO', 0, 400, 'Yes,No'),
('adopt_annual_limit',   'Annual Benefit Limit',                        19, 'TEXT', 0, 100, NULL),
('adopt_eligible_expenses','Eligible Expenses',                         19, 'TEXTAREA', 0, 200, NULL),
('adopt_reimbursement',  'Reimbursement Method',                        19, 'SELECT', 0, 300, 'Direct Deposit,Check,Payroll'),
('cobra_prior_admin',    'Current COBRA Administrator',                 20, 'TEXT', 0, 100, NULL),
('cobra_active_count',   'Number of Active COBRA Participants',         20, 'NUMBER', 0, 200, NULL),
('cobra_state_reqs',     'State-Specific COBRA Requirements',           20, 'TEXTAREA', 0, 300, NULL),
('cobra_term_notice',    'Termination Notice Process',                  20, 'SELECT', 0, 400, 'Employer Notifies,TPA Notifies,Both');

-- IRS Limits table
-- NOTE: Entity uses composite PK (limit_key, plan_year), not auto-increment
CREATE TABLE irslimit (
    limit_key VARCHAR(50) NOT NULL,
    plan_year INT NOT NULL,
    amount DOUBLE NOT NULL,
    description VARCHAR(200) NOT NULL,
    PRIMARY KEY (limit_key, plan_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed 2025 IRS limits
INSERT INTO irslimit (plan_year, limit_key, description, amount) VALUES
(2025, 'FSA_MEDICAL',     'Health FSA contribution limit',          3300.00),
(2025, 'FSA_DEPENDENT',   'Dependent care FSA limit (single/MFJ)', 5000.00),
(2025, 'FSA_ROLLOVER',    'FSA rollover maximum',                    660.00),
(2025, 'HSA_SELF',        'HSA self-only contribution limit',       4300.00),
(2025, 'HSA_FAMILY',      'HSA family contribution limit',          8550.00),
(2025, 'HSA_CATCHUP',     'HSA catch-up contribution (55+)',        1000.00),
(2025, 'TRANSIT',         'Transit/vanpool monthly limit',           325.00),
(2025, 'PARKING',         'Qualified parking monthly limit',         325.00),
(2025, 'QSEHRA_SELF',     'QSEHRA self-only reimbursement',        6350.00),
(2025, 'QSEHRA_FAMILY',   'QSEHRA family reimbursement',          12800.00),
(2025, 'EBHRA',           'Excepted Benefit HRA limit',             2150.00),
(2025, 'ADOPTION',        'Adoption assistance exclusion',         17280.00);

-- BenefitType and BillingType tables
CREATE TABLE benefittype (
    benefittype_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    default_billingtype_id BIGINT NULL,
    psp_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    FOREIGN KEY (default_billingtype_id) REFERENCES billingtype(billingtype_id),
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE billingtype (
    billingtype_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    psp_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed benefit types
INSERT INTO benefittype (benefittype_id, name, psp_id, sort_order) VALUES
(1, 'Medical',       4, 100),
(2, 'Dental',        4, 200),
(3, 'Vision',        4, 300),
(4, 'Life',          4, 400),
(5, 'Supplemental',  4, 500);

-- Seed billing types
INSERT INTO billingtype (billingtype_id, name, psp_id, sort_order) VALUES
(1, 'Self-Bill',     4, 100),
(2, 'List Bill',     4, 200),
(3, 'Carrier Bill',  4, 300),
(4, 'Direct Bill',   4, 400),
(5, 'Payroll',       4, 500);

-- S3/Wasabi constants (placeholders — fill in after running)
INSERT IGNORE INTO constant (name, value) VALUES ('S3_ENDPOINT', 'FILL_ME_IN');
INSERT IGNORE INTO constant (name, value) VALUES ('S3_BUCKET', 'FILL_ME_IN');
INSERT IGNORE INTO constant (name, value) VALUES ('S3_ACCESS_KEY', 'FILL_ME_IN');
INSERT IGNORE INTO constant (name, value) VALUES ('S3_SECRET_KEY', 'FILL_ME_IN');


-- =============================================================================
-- V004: Service Manager
-- =============================================================================
-- NOTE: Original V004 had FK bug (REFERENCES psp(psp_id)) and wrong INT type.
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
    name VARCHAR(50) NOT NULL,
    icon_class VARCHAR(50) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    psp_id BIGINT NOT NULL,
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE marketingmaterial ADD COLUMN category_id BIGINT NULL;
ALTER TABLE marketingmaterial ADD CONSTRAINT fk_mm_category
    FOREIGN KEY (category_id) REFERENCES resourcecategory(category_id);

ALTER TABLE marketingmaterial MODIFY COLUMN storage_guid VARCHAR(50);

ALTER TABLE feature ADD COLUMN library_resource_id BIGINT NULL;
ALTER TABLE feature ADD CONSTRAINT fk_feature_library_resource
    FOREIGN KEY (library_resource_id) REFERENCES marketingmaterial(material_id);


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
INSERT IGNORE INTO templategroup (group_id, description) VALUES (5, 'Sales');

-- New Opportunity template purpose
INSERT IGNORE INTO templatepurpose (purpose_id, description, sort_order, group_id)
  VALUES (30, 'New Opportunity', 100, 5);

-- Sales tasks (900000+ range)
INSERT INTO task (task_id, description, allow_early, allow_future) VALUES
  (900001, 'Initial contact with prospect', 1, 1),
  (900002, 'Qualify prospect needs', 1, 1),
  (900003, 'Send proposal', 1, 1),
  (900004, 'Follow up on proposal', 1, 1),
  (900005, 'Close deal', 1, 1);

INSERT INTO tasksequence (sequence_id, description, DTYPE, purpose_id)
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
    out_log_id BIGINT NULL,
    original_date DATE NOT NULL,
    original_in_time TIME NOT NULL,
    original_out_time TIME NULL,
    requested_in_time TIME NULL,
    requested_out_time TIME NULL,
    request_note VARCHAR(500) NULL,
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
-- NOTE: This section now includes todo_guid, todo.is_reverted, and task_guid
-- columns that were added ad-hoc during BPO development but missed from the
-- original V011 script. All use server-generated defaults for backward
-- compatibility (old code that doesn't know about these columns can still INSERT).

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

-- BPO sync identity GUID for todo (cross-system sync)
-- DEFAULT (UUID()) ensures backward compatibility: old code can INSERT without providing this value
ALTER TABLE todo
    ADD COLUMN todo_guid VARCHAR(36) NOT NULL DEFAULT (UUID()),
    ADD UNIQUE INDEX uq_todo_guid (todo_guid);

-- BPO revert flag: PSP sent task back to BPO
ALTER TABLE todo
    ADD COLUMN is_reverted TINYINT(1) NOT NULL DEFAULT 0;

-- BPO sync identity GUID for task (cross-system sync)
ALTER TABLE task
    ADD COLUMN task_guid VARCHAR(36) NOT NULL DEFAULT (UUID()),
    ADD UNIQUE INDEX uq_task_guid (task_guid);

CREATE TABLE todo_note (
    note_id BIGINT NOT NULL AUTO_INCREMENT,
    todo_id BIGINT NOT NULL,
    created_by_id BIGINT NOT NULL,
    created_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note_text TEXT NOT NULL,
    source_type VARCHAR(10) NOT NULL DEFAULT 'PSP',
    PRIMARY KEY (note_id),
    CONSTRAINT FK_TODONOTE_todo FOREIGN KEY (todo_id) REFERENCES todo(todo_id),
    CONSTRAINT FK_TODONOTE_created_by FOREIGN KEY (created_by_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_todonote_todo ON todo_note(todo_id);
CREATE INDEX idx_todonote_created_date ON todo_note(created_date);

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
-- NOTE: Entity was refactored after original V013 script was written.
-- Column names updated to match current UserFilterPreset.java entity.

CREATE TABLE user_filter_preset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    slot_number INT NOT NULL,
    label VARCHAR(16) NOT NULL,
    view_renewal TINYINT(1) NOT NULL DEFAULT 1,
    view_setup TINYINT(1) NOT NULL DEFAULT 1,
    view_ticket TINYINT(1) NOT NULL DEFAULT 1,
    view_opportunity TINYINT(1) NOT NULL DEFAULT 0,
    ownership_filter INT NOT NULL DEFAULT 0,
    attention_filter INT NOT NULL DEFAULT 0,
    sort_alphabetically TINYINT(1) NOT NULL DEFAULT 1,
    CONSTRAINT fk_preset_user FOREIGN KEY (user_id) REFERENCES `user`(person_id),
    CONSTRAINT uq_user_slot UNIQUE (user_id, slot_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Seed default presets for existing users (3 slots each)
INSERT INTO user_filter_preset (user_id, slot_number, label,
    view_renewal, view_setup, view_ticket, view_opportunity,
    ownership_filter, attention_filter, sort_alphabetically)
SELECT u.person_id, 1, 'All Activities', 1, 1, 1, 0, 0, 0, 1
FROM user u;

INSERT INTO user_filter_preset (user_id, slot_number, label,
    view_renewal, view_setup, view_ticket, view_opportunity,
    ownership_filter, attention_filter, sort_alphabetically)
SELECT u.person_id, 2, 'My Urgent', 1, 1, 1, 0, 1, 1, 0
FROM user u;

INSERT INTO user_filter_preset (user_id, slot_number, label,
    view_renewal, view_setup, view_ticket, view_opportunity,
    ownership_filter, attention_filter, sort_alphabetically)
SELECT u.person_id, 3, 'Renewals', 1, 0, 0, 0, 0, 0, 0
FROM user u;


-- =============================================================================
-- V014: Chatbot Deployment
-- =============================================================================

-- Note resolution flag for AI chatbot
ALTER TABLE note ADD COLUMN is_resolution TINYINT(1) NOT NULL DEFAULT 0;

-- Anthropic API key constant (placeholder — fill in real value)
INSERT IGNORE INTO constant (name, value, note)
VALUES ('ANTHROPIC_API_KEY', 'FILL_ME_IN', 'Claude API key for AI chatbot assistant');

-- Deactivate old ticket categories (preserved via FK, hidden from dropdown)
UPDATE ticketcategory SET active = 0;

-- New service-oriented ticket categories
INSERT IGNORE INTO ticketcategory (category_id, DESCRIPTION, short_text, active) VALUES
(11, 'Claims',            'Claims',  1),
(12, 'Access / Online',   'Access',  1),
(13, 'Debit Card',        'Debit',   1),
(14, 'COBRA',             'COBRA',   1),
(15, 'HSA',               'HSA',     1),
(16, 'Enrollment',        'Enroll',  1),
(17, 'Plan Services',     'Plans',   1),
(18, 'Billing',           'Billing', 1),
(21, 'General',           'General', 1);

-- Starter subcategories
INSERT IGNORE INTO ticketsubcategory (subcategory_id, DESCRIPTION, category_id, is_active) VALUES
(101, 'Claim not paid',               11, 1),
(102, 'Claim paid incorrectly',       11, 1),
(103, 'Can''t log in to portal',      12, 1),
(104, 'Need online access',           12, 1),
(105, 'Debit card not working',       13, 1),
(106, 'Debit card replacement',       13, 1),
(107, 'COBRA enrollment',             14, 1),
(108, 'COBRA payment issue',          14, 1),
(109, 'HSA contribution question',    15, 1),
(110, 'HSA eligible expense question', 15, 1),
(111, 'New hire enrollment',          16, 1),
(112, 'Open enrollment',              16, 1),
(113, 'Qualifying life event',        16, 1),
(114, 'FSA question',                 17, 1),
(115, 'HRA question',                 17, 1),
(116, 'Plan quote request',           17, 1),
(117, 'Billing discrepancy',          18, 1),
(118, 'Invoice request',              18, 1),
(119, 'General inquiry',              21, 1),
(120, 'Other',                        21, 1);


-- =============================================================================
-- V015: Constants to Properties
-- =============================================================================
-- IMPORTANT: Deploy code (StorageDAO, ClaudeApiService using AppConfig) BEFORE
-- running this section. The code must read from ssa.properties instead of DB.

DELETE FROM constant WHERE name IN (
    'S3_ENDPOINT',
    'S3_BUCKET',
    'S3_ACCESS_KEY',
    'S3_SECRET_KEY',
    'ANTHROPIC_API_KEY',
    'SAVE_PATH'
);


-- =============================================================================
-- V016: BPO Registration & Assignment Tables
-- =============================================================================
-- CREATE TABLE IF NOT EXISTS — safe on all environments

CREATE TABLE IF NOT EXISTS bpo_registration (
    bpo_reg_id BIGINT NOT NULL AUTO_INCREMENT,
    psp_id BIGINT NOT NULL,
    bpo_name VARCHAR(100) NOT NULL,
    bpo_url VARCHAR(255) DEFAULT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    date_registered DATE NOT NULL,
    PRIMARY KEY (bpo_reg_id),
    KEY FK_BPOREG_psp (psp_id),
    CONSTRAINT FK_BPOREG_psp FOREIGN KEY (psp_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS bpo_psp_assignment (
    assignment_id BIGINT NOT NULL AUTO_INCREMENT,
    bpo_user_id BIGINT NOT NULL,
    psp_id BIGINT NOT NULL,
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    date_assigned DATE NOT NULL,
    PRIMARY KEY (assignment_id),
    UNIQUE KEY uq_bpo_psp_user (bpo_user_id, psp_id),
    KEY FK_BPOPSP_psp (psp_id),
    CONSTRAINT FK_BPOPSP_psp FOREIGN KEY (psp_id) REFERENCES assignee(id),
    CONSTRAINT FK_BPOPSP_user FOREIGN KEY (bpo_user_id) REFERENCES assignee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


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
('V001', 'Sales pipeline - tables, columns, entity renames', 'V001__sales_pipeline.sql'),
('V002', 'Sales pipeline 2 - proposal source_activity_id', 'V002__sales_pipeline_2.sql'),
('V003', 'Sales pipeline 3 - LOS expansion, app sections, IRS limits', 'V003__sales_pipeline_3.sql'),
('V004', 'Service manager - enhancement, join tables, SM FKs, LOS columns', 'V004__service_manager.sql'),
('V005', 'Rate manager - ratetable sort_order', 'V005__rate_manager.sql'),
('V006', 'Invitation system - invitation table, agency manager_id', 'V006__invitation_system.sql'),
('V007', 'Resource library - category, material FK, feature FK', 'V007__resource_library.sql'),
('V008', 'Opportunity system - assignee columns, sales tasks', 'V008__opportunity_system.sql'),
('V009', 'Timeclock correction - request table', 'V009__timeclock_correction.sql'),
('V010', 'PSP opportunity integration - sales role, managed_by', 'V010__psp_opportunity_integration.sql'),
('V011', 'BPO delegation - todo BPO columns, todo_guid, is_reverted, task_guid, todo_note, BPO roles', 'V011__bpo_delegation_feature.sql'),
('V012', 'Role cleanup and PSP branding constants', 'V012__role_cleanup_psp_branding_constants.sql'),
('V013', 'User filter presets - 3 slots per user (refactored columns)', 'V013__user_filter_presets.sql'),
('V014', 'Chatbot deployment - note.is_resolution, API key, ticket categories', 'V014__chatbot_deployment.sql'),
('V015', 'Move S3 and API key constants to ssa.properties, delete dead SAVE_PATH', 'V015__constants_to_properties.sql'),
('V016', 'BPO registration and PSP assignment tables', 'V016__bpo_registration_tables.sql');


-- =============================================================================
-- RESTORE SETTINGS
-- =============================================================================

SET SQL_SAFE_UPDATES = 1;
SET FOREIGN_KEY_CHECKS = 1;


-- =============================================================================
-- POST-RUN REMINDERS
-- =============================================================================

-- 1. V015 deletes S3 constants from DB — ensure ssa.properties has:
--      S3_ENDPOINT=https://s3.us-east-1.wasabisys.com
--      S3_BUCKET=ams-file-storage
--      S3_ACCESS_KEY=<your-access-key>
--      S3_SECRET_KEY=<your-secret-key>
--      ANTHROPIC_API_KEY=<your-api-key>   (SSA installs only)
--
-- 2. If running V014 without V015 deployed yet, fill in DB constants first:
--    UPDATE constant SET value = 'your-endpoint' WHERE name = 'S3_ENDPOINT';
--    UPDATE constant SET value = 'your-bucket'   WHERE name = 'S3_BUCKET';
--    UPDATE constant SET value = 'your-key'      WHERE name = 'S3_ACCESS_KEY';
--    UPDATE constant SET value = 'your-secret'   WHERE name = 'S3_SECRET_KEY';
--
-- 3. Verify with:
--    SELECT * FROM schema_version ORDER BY version;
--    SELECT COUNT(*) FROM enhancement;
--    SELECT COUNT(*) FROM applicationsection;
--    SELECT COUNT(*) FROM applicationfield;
--    SELECT COUNT(*) FROM irslimit;
--    DESCRIBE assignee;  -- should show prospect_id, agency_id_opp, opportunity_stage, managed_by_id
--    DESCRIBE todo;      -- should show bpo_* columns, todo_guid, is_reverted
--    DESCRIBE task;      -- should show task_guid
--    DESCRIBE note;      -- should show is_resolution
--    DESCRIBE application; -- should show status, date_started, etc.
--    SELECT * FROM bpo_registration;
--    SELECT * FROM bpo_psp_assignment;
