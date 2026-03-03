-- ============================================================================
-- V032: Approved Vendors Registry
-- Creates the approved_vendors table for centralized BPO directory.
-- Table exists on all deployments (same WAR/schema), only populated on master.
--
-- Prerequisites: V031
-- ============================================================================

CREATE TABLE IF NOT EXISTS approved_vendors (
    vendor_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_name VARCHAR(100) NOT NULL,
    vendor_url  VARCHAR(255) NOT NULL,
    description VARCHAR(500) DEFAULT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    date_added  DATE NOT NULL,
    UNIQUE KEY uk_vendor_url (vendor_url)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V032', 'Approved vendors registry table', 'V032__approved_vendors_registry.sql', NOW());
