-- V037: Add LOS/Enhancement scoping to proposal_section
-- Allows CUSTOM sections to appear only when specific services are proposed.

-- Add scope column to proposal_section
ALTER TABLE proposal_section ADD COLUMN scope VARCHAR(10) NOT NULL DEFAULT 'ALL' AFTER html_content;

-- LOS scoping join table (mirrors applicationsectionlos)
CREATE TABLE proposalsectionlos (
    section_id  BIGINT NOT NULL,
    los_id      BIGINT NOT NULL,
    PRIMARY KEY (section_id, los_id),
    CONSTRAINT fk_psl_section FOREIGN KEY (section_id) REFERENCES proposal_section(section_id) ON DELETE CASCADE,
    CONSTRAINT fk_psl_los FOREIGN KEY (los_id) REFERENCES los(los_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Enhancement scoping join table (mirrors applicationsectionenhancement)
CREATE TABLE proposalsectionenhancement (
    section_id      BIGINT NOT NULL,
    enhancement_id  BIGINT NOT NULL,
    PRIMARY KEY (section_id, enhancement_id),
    CONSTRAINT fk_pse_section FOREIGN KEY (section_id) REFERENCES proposal_section(section_id) ON DELETE CASCADE,
    CONSTRAINT fk_pse_enhancement FOREIGN KEY (enhancement_id) REFERENCES enhancement(enhancement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Self-register
INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
VALUES ('V037', 'Add LOS/Enhancement scoping to proposal_section', 'V037__proposal_section_scoping.sql', NOW());
