-- V069: Per-agency white-label email sending (agency.email_domain, agency.email_verified)
--
-- Adds the per-agency sending-identity config consumed by EmailIdentityResolver at the
-- single EmailDAO send choke point. Together with FALLBACK_FROM (DB constant, default
-- notifications@superiorstate.net) and VERIFIED_PSP_DOMAINS (ssa.properties, default
-- superiorstate.net,superiorstate.biz), these two columns drive the four-tier sender model:
--
--   Tier 0 — sender's email domain in VERIFIED_PSP_DOMAINS -> From = sender's own address
--   Tier 1 — agency.email_domain non-blank AND email_verified=1 ->
--            From = <sender-localpart>@<email_domain>, Reply-To = sender's real address
--   Tier 2 — otherwise -> From = FALLBACK_FROM (+ sender full-name display),
--            Reply-To = sender's real address
--   System — auth/security mail -> From = noreply@superiorstate.net, no Reply-To
--
-- Storage:
--   email_domain    VARCHAR(255) NULL, UNIQUE — the agency's verified sending (sub)domain,
--                   e.g. admin.swbd.com. Stored lowercase. NULL for agencies not sending
--                   white-label. MySQL treats multiple NULLs as distinct, so non-opted-in
--                   agencies never collide on the unique index.
--   email_verified  TINYINT(1) NOT NULL DEFAULT 0 — manual PSP-admin flag (v1; no SMTP2GO
--                   API readback). MUST NOT be flipped to 1 until the domain shows Verified
--                   in SMTP2GO (SPF return-path CNAME em102001 + DKIM s102001._domainkey at
--                   the subdomain). Gates Tier 1; when 0 the agency falls back to Tier 2.
--
-- email_domain is intentionally SEPARATE from landing_host (V068): web-host presence
-- (DNS A/CNAME) is not the same as relay verification (SMTP2GO DKIM/SPF). The Agency
-- Manager UI pre-fills email_domain from landing_host for convenience but stores/verifies
-- it independently.
--
-- Prerequisites: V057 (agency.suppressed), V067 (agency.markup_enabled), V068 (agency.landing_host/landing_html).

-- ----------------------------------------------------------------------
-- 1. Add email_domain + email_verified to agency
-- ----------------------------------------------------------------------
ALTER TABLE agency ADD COLUMN email_domain VARCHAR(255) NULL;
ALTER TABLE agency ADD COLUMN email_verified TINYINT(1) NOT NULL DEFAULT 0;

-- Fast, unique lookup by sending domain. NULLs are distinct in MySQL, so agencies
-- without a white-label sending domain do not conflict.
ALTER TABLE agency ADD UNIQUE INDEX uq_agency_email_domain (email_domain);

-- ----------------------------------------------------------------------
-- 2. schema_info + schema_version self-registration
-- ----------------------------------------------------------------------
CREATE OR REPLACE VIEW schema_info AS
SELECT 'V069' AS version, '2026-07-09' AS updated;

INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V069', 'Per-agency white-label email sending (agency.email_domain unique + email_verified)', 'V069__agency_email_sending.sql', NOW());
