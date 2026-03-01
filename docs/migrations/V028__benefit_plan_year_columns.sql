-- V028: Add plan year tracking columns to benefit table
-- These columns store the latest plan year start/end from Summit exports
-- (J5 for CDH benefits, J7 enddate for COBRA benefits).
-- Used to derive the actual renewal date: plan_year_end + 1 day.

ALTER TABLE benefit ADD COLUMN plan_year_start DATE NULL;
ALTER TABLE benefit ADD COLUMN plan_year_end DATE NULL;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V028', 'Benefit plan year start/end columns for renewal date correction', 'V028__benefit_plan_year_columns.sql', NOW());
