-- =============================================================================
-- V012 — Role Cleanup + PSP Branding Constants
-- Date: February 25, 2026
-- Target: beta_ssa schema (local and production)
--
-- Prerequisite: V011 (BPO roles 101-103 must exist before renaming 102/103)
--
-- Changes:
--   1. Delete unused UserRoles (6, 7, 10) — never checked in code
--   2. Rename BPO roles to remove Accelergent branding
--   3. Seed PSP branding constants (logo/favicon paths)
--
-- Note: DatabaseInitializer was also updated this session to:
--   - Remove creation of roles 6, 7, 10
--   - Seed LOGO_NAVBAR, LOGO_LOGIN, FAVICON via addPspConstants()
--   - Update ReferenceDataSeeder.fillUserRoles() and AmsDataGlobal exclude list
-- =============================================================================

-- ─── 1. Delete dead UserRoles ────────────────────────────────────────────────
-- Roles 6 (Pending Agent), 7 (Anonymous), 10 (Other) were never checked in code
DELETE FROM userrole WHERE id IN (6, 7, 10);

-- ─── 2. Rename BPO roles (remove Accelergent branding) ──────────────────────
UPDATE userrole SET description = 'BPO Admin' WHERE id = 102;
UPDATE userrole SET description = 'BPO User' WHERE id = 103;

-- ─── 3. Seed logo and favicon constants ──────────────────────────────────────
INSERT IGNORE INTO constant (name, value) VALUES ('LOGO_NAVBAR', '/images/logoA.png');
INSERT IGNORE INTO constant (name, value) VALUES ('LOGO_LOGIN', '/images/logoD.png');
INSERT IGNORE INTO constant (name, value) VALUES ('FAVICON', '/favicon.ico');

-- ─── Self-register in schema_version ─────────────────────────────────────────
INSERT IGNORE INTO schema_version (version, description, script_name)
VALUES ('V012', 'Role cleanup and PSP branding constants', 'V012__role_cleanup_psp_branding_constants.sql');

-- ─── VERIFICATION ────────────────────────────────────────────────────────────
-- SELECT * FROM userrole ORDER BY id;
-- SELECT * FROM constant WHERE name IN ('LOGO_NAVBAR', 'LOGO_LOGIN', 'FAVICON');
-- SELECT * FROM schema_version WHERE version = 'V012';

-- ─── ROLLBACK (if needed) ────────────────────────────────────────────────────
-- INSERT INTO userrole (id, description) VALUES (6, 'Pending Agent'), (7, 'Anonymous'), (10, 'Other');
-- UPDATE userrole SET description = 'Accelergent BPO Admin' WHERE id = 102;
-- UPDATE userrole SET description = 'Accelergent BPO User' WHERE id = 103;
-- DELETE FROM constant WHERE name IN ('LOGO_NAVBAR', 'LOGO_LOGIN', 'FAVICON');
-- DELETE FROM schema_version WHERE version = 'V012';
