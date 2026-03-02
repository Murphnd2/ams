-- D-37: Backfill PSP Home Agency Config for beta_ssa

-- 1. Add Agent (2) and Agency Admin (8) roles to Kevin (person 104)
INSERT IGNORE INTO userinroles (person_id, role_id) VALUES (104, 2);
INSERT IGNORE INTO userinroles (person_id, role_id) VALUES (104, 8);

-- 2. Set manager_id on home agency
UPDATE agency SET manager_id = 104 WHERE agency_id = 14;

-- 3. Add person 104 to agency 14's agent list
INSERT IGNORE INTO agents (agency_id, person_id) VALUES (14, 104);

-- 4. Insert PSP_HOME_AGENCY_ID constant
INSERT IGNORE INTO constant (name, value)
VALUES ('PSP_HOME_AGENCY_ID', '14');