-- Sales Pipeline Session 2 — Proposal source activity link
-- Run BEFORE deploying SendProposal feature
-- Safe to run on existing data (nullable column add)

ALTER TABLE proposal ADD COLUMN source_activity_id BIGINT NULL;
