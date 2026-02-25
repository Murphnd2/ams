-- =============================================================================
-- Production SQL — February 25, 2026 (Role Cleanup + Branding Constants)
-- Run on: beta_ssa (production)
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

-- ─── 4. Verify ──────────────────────────────────────────────────────────────
-- SELECT * FROM userrole ORDER BY id;
-- SELECT * FROM constant WHERE name IN ('LOGO_NAVBAR', 'LOGO_LOGIN', 'FAVICON');
