# Database Migration Tracker

Tracks database schema versions across environments.

**Last Updated:** August 1, 2026

## Environments

| Environment | Host | Schema | Notes |
|-------------|------|--------|-------|
| Local (Work) | 127.0.0.1:3306 | beta_ssa | Dev workstation |
| Local (Home) | 127.0.0.1:3306 | beta_ssa | Home workstation |
| Local (either) | 127.0.0.1:3306 | dev_ssa | Initialization testing (wiped regularly) |
| Production | superiorstate.biz | beta_ssa | Live server |
| Demo PSP | demo.superiorstate.biz | beta_ssa | Conference demo PSP (V038, seeded, release V0.37.0) |
| BPO | bpo.superiorstate.biz | beta_ssa | BPO instance (V038, initialized, release V0.37.0) |
| Master | master.superiorstate.biz | beta_ssa | Snapshot v9 (V057, stopped) |

## Current Highest Version: V092

⚠️ **Maintenance note (added 2026-07-30):** production status in the table below must be back-filled
*after a deployment actually succeeds*, not only when the migration is written. The V072/V073 rows
were correctly marked unapplied for Production the day they were authored (2026-07-17) — then never
revisited after they shipped hours later that same day. That gap, repeated silently for months, is
what let this tracker's Production column drift ~49 versions out of date before the 2026-07-30
reconciliation below. Flipping the Production cell is part of landing the deploy, not a follow-up
task. See also `CLAUDE.md`'s "Keeping state docs current" section.

## Dev Baseline

The current baseline is `docs/importscript/beta_ssa_dev_baseline_thru_V024.sql` — a structure-only dump from a V024 database.

**To reset a dev database:**
1. Reset target: `DROP DATABASE IF EXISTS dev_ssa; CREATE DATABASE dev_ssa;`
2. Import baseline: Workbench → Server → Data Import → select file → target `dev_ssa`
3. Start app against `dev_ssa` → `DatabaseInitializer` seeds data
4. For `beta_ssa`, also re-import Datapath exports after baseline import

Future migrations (V025+) are applied incrementally on top of the baseline.

## Schema Version Table

The `schema_version` table columns are: `version` (PK), `description`, `script_name`, `applied_on` (timestamp, auto-default).

The table is seeded by the baseline dump. For fresh databases created outside the baseline (e.g., `dev_ssa` after initialization), run `docs/schema_version_migration.sql` to register all versions.

## Going Forward

All new schema changes must follow these rules:
1. Create a versioned script: `V{NNN}__{description}.sql`
2. Script must self-register via `INSERT IGNORE INTO schema_version (version, description, script_name, applied_on) VALUES (..., NOW());`
3. Update this tracker with the new version
4. Update `docs/schema_version_migration.sql` with the new INSERT row

Individual migration scripts **are** stored in `docs/migrations/` (`V025__…` through the latest version). The source of truth is the V024 baseline dump (`docs/importscript/beta_ssa_dev_baseline_thru_V024.sql`) plus the incremental `V025+` scripts applied on top; `schema_version_migration.sql` registers every applied version in the `schema_version` table for databases created outside the baseline. Session summaries document what each version changed.

## Version History

_N/A = environment decommissioned / not maintained (applies to Demo PSP, BPO, Master — see Environments table above)._

| Version | Description | beta_ssa (work) | beta_ssa (home) | dev_ssa | Production | Demo PSP | BPO | Master |
|---------|-------------|-----------------|-----------------|---------|------------|----------|-----|--------|
| V001–V019 | Sales pipeline through application field suppressed | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V020 | ServiceItem unification - schema + backfill | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V021 | ServiceItem linkage - LOS/Enhancement backfill | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V022 | Orphaned ticket ServiceItem backfill | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V023 | Drop ticketsubcategory table and FK | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V024 | Fix views referencing dropped ticket_category column | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V025 | Add level, los, employer_name to plantype for Summit import | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ✅ |
| V026 | Benefit table: surrogate auto-increment PK with source tracking | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ✅ |
| V027 | BPO Registration: task source refactor from Person to BpoRegistration | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ✅ |
| V028 | Benefit plan year start/end columns for renewal date correction | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ✅ |
| V029 | Add is_active column to user table for user deactivation | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ✅ |
| V030 | BPO cross-system foundation: psp_clients, delegated_todo, API columns, todo_note GUID | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ✅ |
| V031 | ToDoNote cross-system: nullable todo_id/created_by_id, author_name column | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ✅ |
| V032 | Approved vendors registry table | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ✅ |
| V033 | ToDoNote attachments: todo_note_id FK on weblink | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ✅ |
| V034 | Add template_key to applicationsection for starter packages | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ✅ |
| V035 | Feature headline column and description widening | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ✅ |
| V036 | Proposal section table for composable proposal content | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ✅ |
| V037 | Add LOS/Enhancement scoping to proposal_section | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ✅ |
| V038 | Add sort_order to delegated_todo for BPO ordering | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ⬜ |
| V039 | Questionnaire system: templates, fields, instances, values, scoping | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V040 | Add recurring_series_id to delegated_todo for BPO recurring history | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V041 | Add reviewer tracking fields to application table | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V042 | BPO pending approval workflow - status PENDING support | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V043 | Add text_value column to constant for custom landing page HTML | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V044 | Add agency scoping to proposal_section for TITLE/CLOSING overrides | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V045 | Application selected LOS and Enhancement IDs | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V046 | Chatbot skill table for extensible AI assistant capabilities | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V047 | Composite task order table for cross-sequence ordering | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V048 | Universal import system tables and seed data | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V049 | Add source_task_id to delegated_todo for required-sequence auto-approval | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V050 | BPO default assignee per PSP client | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ | ⬜ |
| V051 | Import ID mapping cross-reference table | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V052 | Import run log cross-reference tracking columns | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V053 | Interactive import enhancements: update mode, mapping status, FK flags | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V054 | Super User Dashboard — managed_installation table | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V055 | Schema info view for structural version identification | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V056 | Training video and single-use token tables | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V057 | Add suppressed flag to agency table | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V058 | Add renderer column to questionnaire | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V059 | NDT census-based testing tables | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V060 | Outlook add-in user link + weblink.note_id | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V061 | ToDo-level ownership override for agent delegation | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V062 | Per-note agent visibility override | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V063 | Knowledge Base tables (knowledge_base, knowledge_chunk, knowledge_chunk_history) + 5 KB registry rows | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V064 | Platform JSON registry seed (proposal_page_builder, automation_email_builder) | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V065 | Email Draft Assistant skill seed: unique index on chatbot_skill(psp_id,skill_name) + EMAIL_DRAFT_ASSISTANT row | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V066 | Per-proposal, per-line agent markup on pricing (proposal_price_adjustment) | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V067 | Per-agency enable flag for proposal markup (agency.markup_enabled, default OFF) | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V068 | Host-header custom agency landing pages (agency.landing_host unique + landing_html) | ⬜ | ⬜ | ⬜ | ✅ | ⬜ | ⬜ | ⬜ |
| V069 | Per-agency white-label email sending (agency.email_domain unique + email_verified) | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V070 | Self-referential agency parent link (GA -> sub-agency hierarchy): agency.parent_agency_id nullable + child index | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V071 | Per-agency public quote token (agency.quote_token unique) for RequestQuote sub-agency attribution links | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V072 | Monthly billing run tracking: billing_run + billing_run_step | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V073 | Widen billing_run.current_step to VARCHAR(255) (fixes 1406 truncation on CREATE_BILLING sub-step labels) | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V074 | Rating-area rate cache for A1 ICHRA illustration (rating_area_rate_cache table) | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V075 | Illustration log for A1 ICHRA rating illustration, no PII (illustration_log table) | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V076 | County reference data (FIPS, state, name, representative ZIP) -- Texas seed (county_reference table) | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V077 | Per-agency enable flag for ICHRA capability access (agency.ichra_enabled, default OFF) | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V078 | On-exchange LCSP and benchmark-silver columns for T44 (rating_area_rate_cache.onex_lcsp_premium/onex_benchmark_silver_premium, default NULL) | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V079 | ICHRA illustration snapshot on a proposal (proposal_ichra_snapshot + proposal_ichra_snapshot_band) | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V080 | ICHRA/QSEHRA Design Advisor: ICHRA_DESIGN_ADVISOR chatbot_skill row + ichra_design knowledge base and chunks | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V081 | Optional opportunity attribution on illustration_log (illustration_log.opportunity_id, nullable, FK to assignee(id)) | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V082 | Make ICHRA_DESIGN_ADVISOR available to non-admin callers (chatbot_skill.is_admin_only 1 → 0) | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V083 | Fix retired model on ICHRA_DESIGN_ADVISOR (chatbot_skill.model -> claude-sonnet-5, max_tokens -> 3072) | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V084 | ZIP to county crosswalk table for T74 ZIP intake (zip_county; CHAR(5) zip, composite PK, no FK to county_reference) | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V085 | Texas ZIP to county crosswalk data — 2,894 rows from the Census 2020 ZCTA-county relationship file | ⬜ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V086 | Plus-tier classification flag on line of service (`los.is_plus_tier`, NOT NULL DEFAULT 0, no backfill) — **read on three surfaces, found stale and corrected 2026-08-05 (S18-H)**: public unauthenticated `/proposal/*` (`ViewProposal.java:338`, `:452`), the authenticated Proposal Builder (`ProposalBuilder.java:459`), and written from the Service Manager admin UI (`ServiceManagerAction.java:125`) | ✅ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V087 | Plus-tier ZIP/county/headcount intake captured in the Proposal Builder (`proposal_ichra_intake`, `proposal_id` UNIQUE FK ON DELETE CASCADE, `plan_year` derived server-side not agent-asserted — S10-B HS-1) — the T125 consumer of V086. Applied to local dev (`beta_ssa`, work) 2026-08-06 (S19-M), backfilling a gap found by S19-L: `schema_version` jumped V086→V089, this migration had never run locally. Verified: `proposal_ichra_intake` table now exists, 0 rows | ✅ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V088 | Employer monthly contribution per employee on `proposal_ichra_intake` (`monthly_contribution_per_employee` DECIMAL(10,2) NULL, no backfill) — T80 half 1, an intake token exactly like V087's four. Applied to local dev (`beta_ssa`, work) 2026-08-06 (S19-M), same gap-backfill as V087 above | ✅ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V089 | System-managed classification flag on enhancement (`enhancement.system_managed`, NOT NULL DEFAULT 0, no backfill) — **read by `FlaggedEnhancementResolver.isSectionEnabled`, called from `ViewProposal.java:300` on every proposal render** — runtime-verified on production, session 18 (S18-D/S18-G): `system_managed = 1` hid the scoped section, `0` restored it | ✅ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V090 | ICHRA JSON payload column on `proposal_ichra_snapshot` (`payload_json`, `MEDIUMTEXT`, nullable, no backfill) — T165, holds the schema-versioned payload `FlaggedEnhancementResolver` will read once T164/T166 wire the predicate, plus affordability/age-band/plan-landscape data. Written by `ProposalBuilder`'s `attachRangeSnapshot`/`attachAgeBandSnapshot`; read by three new `ViewProposal` tokens (`ICHRA_AGE_BAND_TABLE`, `ICHRA_PLAN_LANDSCAPE_TABLE`, `ICHRA_PAYLOAD_AS_OF`) — no affordability token defined, by design (S19D spec §4/§9). Applied to local dev (`beta_ssa`, work) 2026-08-05 (S19-E): column confirmed `mediumtext`/nullable, `proposal_ichra_snapshot` had 0 existing rows. Applied to production via release `v0.90.02` (S19-P, 2026-08-06) — `update.sh` logged `DONE`; the `v0.90.01` publish that preceded it failed and was re-cut, content identical. See `docs/analysis/S19D_ichra_payload_spec.md` | ✅ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V091 | ICHRA section selection (S20-B, T168) — `enhancement.system_section_key` (`VARCHAR(32)`, nullable, no default — the Rule-4-compliant discriminator `FlaggedEnhancementResolver` uses to identify which of the four sections a flagged enhancement drives), four selection flags on `proposal_ichra_intake` (`section_market`/`section_contribution`/`section_comparison`/`section_affordability`, `TINYINT(1) NOT NULL DEFAULT 0`), section 3's comparison inputs (`current_total_monthly_premium`/`current_employer_monthly_share`, `DECIMAL(10,2)` nullable), and `proposal_ichra_snapshot_band.net_per_employee`/`band_net` widened off `NOT NULL` so band rows persist without an employer contribution. `FlaggedEnhancementResolver.isSectionEnabled` now reads `payload_json.sections` (schemaVersion 2) instead of unconditionally returning `false`; `ViewProposal`'s `proposalEnhIds` membership widened via `FlaggedEnhancementResolver.systemManagedIdsForProposal` (LOS-derived, no priced `RateTable` line required). See `docs/analysis/S20A_ichra_sections_spec.md`. Applied to local dev (`beta_ssa`, work) 2026-08-06 (S20-B) — verified live: `schema_version`/`schema_info` both read V091, `enhancement.system_section_key` is `varchar(32)` nullable, all four `proposal_ichra_intake.section_*` flags are `tinyint(1) NOT NULL DEFAULT 0`, both `current_*` columns are `decimal(10,2)` nullable, and `proposal_ichra_snapshot_band.net_per_employee`/`band_net` both flipped `NOT NULL` to nullable. Pre-apply gap check confirmed none of this existed beforehand. Applied to production via release `v0.91.05` (S21-B, 2026-08-06) — confirmed by a direct `schema_version` query returning V091. | ✅ | ⬜ | ⬜ | ✅ | N/A | N/A | N/A |
| V092 | HSA Enrollment Assistant (2026-08-18) — data only, no WAR. One `chatbot_skill` row (`HSA_ENROLLMENT_ASSISTANT`, psp_id=4, `is_admin_only=0`, sort_order 5, Haiku 4.5/1024) plus three `ssa_business` `knowledge_chunk` rows. Two artifacts because the two AI surfaces reach knowledge by different paths: the Outlook "Draft AI Reply" path (`OutlookDraftReplyApi` -> `EmailDraftService.draft`) does no keyword matching and grounds only in `EmailDraftService.SEARCH_KBS` = {federal_rules, ssa_business, summit_supplemental}, while `ChatAssistant.executeSkill` injects **no** KB content at all and sees only the matched skill's `system_prompt`. Content is the verbatim four-step myRSC self-enrollment procedure (secure.myrsc.com/hsaenroll -> ENROLL NOW -> employer code -> complete application), plus the rule that the per-employer myRSC code is emitted as the literal token `[[MYRSC EMPLOYER CODE]]` for the PSP user to fill from myRSC — never guessed. AMS has no live store for that code: `Setup.myRsc` exists as a column but is read/written by nothing (grep-verified 2026-08-18). Chunk 2b deliberately overrides V065's SOFT_CONF default so a missing code produces a FULL_DRAFT with a placeholder rather than a HOLDING_ACKNOWLEDGMENT. ⚠️ **Post-apply step required:** `KnowledgeSearchService` caches chunks in memory at `initialize()` — the three chunks are invisible until Tomcat restarts or Knowledge Manager's "Reload cache" (`action=reloadCache`) is clicked; the skill row needs neither. SYNC-GUARD: procedure text is duplicated between `system_prompt` and the chunks by design (V080 precedent) — edit both or the surfaces disagree. Prerequisites: V046, V063, V065. Not yet applied anywhere. | ⬜ | ⬜ | ⬜ | ⬜ | N/A | N/A | N/A |

**Production column reconciled 2026-07-30** against a live, read-only `schema_version` probe run
directly against the production database — that probe is the source of truth for the corrections
above, not developer memory or release notes. The probe returned 73 versions recorded (V001 through
V073), each with an `applied_on` timestamp; V072 applied 2026-07-17 12:54:26, V073 applied 2026-07-17
14:11:18. Every V0NN row's Production cell above is now ✅. (`beta_ssa (work)`, `beta_ssa (home)`,
`dev_ssa`, and the Demo/BPO/Master columns were **not** re-probed in this pass and are unchanged —
they still reflect whatever was last recorded for them.)

**Production V074–V076 deployment (2026-07-31):** release `v0.76.00` deployed to production at 11:37;
`update.sh` applied V074, V075, and V076 in order, then swapped the WAR and logged `DONE`. Verified
directly against the production database afterward: `county_reference` row count = 254 (spot-checked
`48223` → `Hopkins County`, representative ZIP `75437`); `schema_version` contains V074, V075, V076
with 2026-07-31 timestamps; `schema_info` reports V076. Production cells for V074–V076 above reflect
this. `beta_ssa (work)`, `beta_ssa (home)`, and `dev_ssa` are unchanged — still unapplied.

**Production V077/V078 status — verified empirically, 2026-07-31, re-confirmation pending.** These two
rows are corrected from unapplied to applied on Production, but **not from a deployment log entry** the
way V074–V076 above are — no `update.sh` run or WAR-swap record was consulted for this correction. The
evidence is behavioral: `Agency.ichraEnabled` maps and production serves ICHRA-gated pages (requires
V077), and a verification `SELECT` against the production database returned populated
`onex_lcsp_premium` values on `rating_area_rate_cache` (requires V078). ⚠️ **This session could not
reach the production database directly to re-run that `SELECT` itself** — the correction above rests on
the empirical evidence already on record for 2026-07-31, not on a fresh query run from this workstation.
Kevin can re-confirm directly with:

```
LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql --socket=/var/run/mysqld/mysqld.sock -u <user> -p beta_ssa \
  -e "SELECT version, description, applied_on FROM schema_version WHERE version IN ('V077','V078') \
      UNION ALL \
      SELECT 'V078-data', CONCAT('onex_lcsp_premium populated rows: ', COUNT(*)), NULL \
      FROM rating_area_rate_cache WHERE onex_lcsp_premium IS NOT NULL;"
```

~~V079 is **not yet applied to any environment** — committed in `b0e524b`, awaiting release
`v0.79.00`.~~ **Superseded 2026-08-01** — V079 shipped to Production via `update.sh` during release
`v0.82.00`, not `v0.79.00`. See the "Production V079–V083 reconciled" section below, which is the
current statement. Struck rather than deleted because this sentence is why the V079 Production cell
read ⬜ for a day after the deploy: it was written at authoring time and never revisited.

**Production V080/V082 status — corrected 2026-08-01, evidence from a live incident, not from a
deployment log entry.** Both rows were carried as unapplied ("Current Highest Version: V082" section
above listed Production ⬜ for both) since this tracker was last hand-updated, which was stale. The
correction is forced by direct production evidence, not inferred: `catalina.out` on 2026-08-01 10:06
recorded `ChatAssistant` executing skill `ICHRA_DESIGN_ADVISOR` (text-only) for a non-admin caller,
which then hit a 404 from the Claude API because the row's configured `model` value
(`claude-sonnet-4-20250514`) was retired — see V083 below, filed to fix that model string. For that
log line to exist at all: (1) the `ICHRA_DESIGN_ADVISOR` row itself must exist and be matched by name,
which requires **V080**; and (2) it must have been reachable and selected for a **non-admin** agent,
which requires `is_admin_only = 0`, i.e. **V082**. Both are therefore applied on Production regardless
of what this file previously recorded. Production cells for V080 and V082 above are corrected to ✅.
This session could not connect to the production database to confirm via `schema_version` directly —
the correction rests on the behavioral proof above, the same evidentiary standard used for the
V077/V078 correction one section up. V081 has no comparable evidence either way and is left ⬜.

**Production V079–V083 reconciled 2026-08-01, at session close.** All five rows now read ✅ for
Production. **V079, V081 and V083 were the stale ones** — V080 and V082 had already been corrected in
`2b79452` from the 404-incident evidence. The remaining three are corrected from the deployment record
rather than from behaviour: `update.sh` applied **V079–V082** during the `v0.82.00` release and **V083**
during the `v0.83.00` release, both on 2026-08-01. V083 additionally carries direct confirmation — a
read-only `SELECT` against `chatbot_skill` after that deploy returned `model = claude-sonnet-5`,
`max_tokens = 3072`, `is_admin_only = 0`, and the Design Advisor then answered a live agent question
that had been returning `404 not_found_error` before it.

⚠️ **This correction is late, and the lateness is the point.** These cells were flagged as stale twice
in `docs/session_closeout_2026-08-01_session5.md` — once in "Contradictions found" and again in the
revised Next block — and shipped un-fixed both times, in a session that had *already* corrected two
other rows in this same column. That is precisely the drift the 2026-07-30 maintenance note at the top
of this file exists to prevent: **flipping the Production cell is part of landing the deploy, not a
follow-up task.** Noticing the gap and deferring it is the same outcome as not noticing.

`beta_ssa (work)`, `beta_ssa (home)` and `dev_ssa` remain ⬜ for V079–V083 and were not re-probed.

~~**V084/V085 authored 2026-08-01 — unapplied everywhere, and that is their true state, not a stale
cell.** Both are ⬜ in every column because neither has been run against any database, including local.~~
**Superseded 2026-08-01, same day, end of session.** That statement was written from inside the Claude
Code container, which has no database connection at all — "never run against any database" was true of
the container and false of the system, the same shape of error as R1/R5/the coverage mismatch earlier
this session: correct about what was examined, wrong about the whole. **Both migrations shipped in
release `v0.85.00` and are applied on Production**, confirmed behaviourally by a role-2 agent's runtime
walk the same session: ZIP `75482` resolved to Hopkins County TX and populated the dropdown; ZIP `75009`
rendered the two-county chooser (Collin/Denton, land-area ordered); ZIP `90210` correctly missed with the
Texas-only coverage message. None of that is possible against an empty `zip_county` table. Production
cells corrected to ✅ above. `beta_ssa (work)`, `beta_ssa (home)` and `dev_ssa` remain ⬜ and
were not probed — this correction rests on production behavioural evidence only, per the same standard
the V077/V078 and V080/V082 corrections above used. **V085 is 89 KB** — comfortably attachable to a
GitHub release by hand, unlike a national build of the same table would be.

**Production V087 status — flipped 2026-08-03, S11-A.** Carried unapplied since the "Current Highest
Version: V087" section was written at S10-B authoring time and never revisited — exactly the drift
pattern the 2026-07-30 maintenance note at the top of this file exists to prevent. Corrected on
direct evidence from `docs/session_closeout_2026-08-03_session10.md`: "`V087__proposal_ichra_intake.sql`
... applied to production by Kevin on 2026-08-03, released in `v0.87.00`." Production cell for V087
above is now ✅. `beta_ssa (work)`, `beta_ssa (home)`, and `dev_ssa` are unchanged — still unapplied,
not re-probed. V088 is applied on production, confirmed 2026-08-05 (session 18) by a direct `schema_version`
query: `SELECT version FROM schema_version WHERE version IN ('V086','V087','V088','V089') ORDER BY version;` returned all four.

## Notes

- V020 requires corresponding Java code changes (TemplatePurpose → ServiceItem, TemplateGroup → ActivityCategory class/field renames). The code branch with renames must be deployed alongside the schema change.
- V021 creates new group 2 ServiceItems (IDs 25-35), renames SI 17 to "Payment Services", suppresses SI 18 and SI 20, and links all LOS and Enhancement records to their ServiceItems.
- V022 creates 6 catch-all ServiceItems (IDs 36-41) for ticket categories that had no ServiceItem, and backfills 730 orphaned tickets.
- V023 drops the ticketsubcategory table and its FK column (ticket_category) from assignee. Requires updated WAR with TicketSubCategory.java deleted.
- V024 rebuilds the a_base_01 through a_base_05 view chain to remove references to the dropped ticket_category column.
- dev_ssa can be reset from the V024 baseline at any time.
- V025 adds three nullable columns to the plantype table for Summit import metadata. No data migration needed — columns are populated by the new Summit Import wizard.
- V026 adds summit_id and source_type columns to benefit, renumbers negative PKs to positive, converts benefit_id to AUTO_INCREMENT, and adds a unique index on (source_type, summit_id). Requires updated WAR with Benefit.java entity changes and all summit-key lookups.
- V027 adds is_approved, is_requested, is_accepted status columns to bpo_registration. Changes task vendor sourcing from Person FK (source_owner) to BpoRegistration FK (bpo_registration_id). Requires updated WAR with Task.java, ToDoOut25.java, UpdateTask25.java, and all DAO/view entity changes.
- V028 adds plan_year_start and plan_year_end nullable DATE columns to benefit table. Used by J5 (CDH) and J7 (COBRA) imports to store plan year boundaries for renewal date correction.
- V029 adds is_active BOOLEAN NOT NULL DEFAULT TRUE to user table. Enables user deactivation in the new User Manager page. All existing users default to active.
- V030 adds BPO cross-system foundation: adds API token (outbound/inbound), partner_url, and date columns to bpo_registration; creates psp_clients table (BPO side, tracks PSP clients with status PENDING/APPROVED/REJECTED/DISCONNECTED); creates delegated_todo table (BPO side, local copy of tasks from PSPs); adds todo_guid column to todo_note for cross-system note sync.
- V031 makes todo_note.todo_id and todo_note.created_by_id nullable for cross-system notes where the originating entity doesn't exist locally. Adds author_name column for display when created_by is null.
- V032 creates the approved_vendors table for a centralized BPO vendor directory. Table exists on all deployments (same WAR/schema), only populated on master.
- V033 adds todo_note_id FK column to the weblink table, enabling file attachments on BPO task notes. Follows the same pattern as email_id FK for email attachments.
- Demo PSP, BPO, and Master environments upgraded to V037 via release V0.37.0 on March 4, 2026. Master snapshot v8 (`SSA-Master-Base-v8-2026-03-04`).
- V051 creates the import_id_mapping cross-reference table for multi-provider ID resolution. Maps (provider_id, entity_type, external_id) → internal_id with is_primary flag for transition scenarios. Includes back-fill from existing archive data (Summit provider assumed). Requires updated WAR with ImportIdMapping entity, ImportIdResolver class, and updated SummitImportService/UniversalImportService.
- V052 adds xref_resolved, pk_allocated, and mappings_recorded INT columns to import_run_log for tracking cross-reference resolution metrics during import runs. Requires updated WAR with ImportRunLog entity fields.
- V053 adds update_mode and mapping_status columns to import_file_type; adds is_fk and fk_entity_type columns to import_field_mapping. Part of Phase A (Provider Setup Rework) — enables sample-file-driven column mapping with PK/FK flagging and readiness status tracking. Requires updated WAR with ImportFileType/ImportFieldMapping entity fields and reworked ProviderSetup servlet.
- V034 adds nullable template_key VARCHAR(50) to applicationsection with a unique index scoped to (template_key, psp_id). Used for starter package duplicate detection. NULL values (manual/seeded sections) are unaffected by the unique constraint.
- V035 adds nullable headline VARCHAR(200) to feature table for short punchy summary text. Widens description from VARCHAR(500) to VARCHAR(2000) for paragraph content. Part of the proposal customization feature (feature sales blurb upgrade).
- V036 creates the proposal_section table for composable proposal content per PSP. Section types: TITLE, PRICING, FEATURES, CLOSING, CUSTOM. Supports HTML content with merge tokens, sort ordering, and active/inactive toggling.
- V037 adds scope column (VARCHAR(10), default 'ALL') to proposal_section and creates proposalsectionlos/proposalsectionenhancement join tables. Enables CUSTOM sections to display only when specific services are proposed.
- V038 adds sort_order INT DEFAULT 0 to delegated_todo. Stores the task's position within its source checklist, included in the BPO push payload from PSP. Enables BPO dashboard sorting by due date → activity name → sort order.
- V039 creates questionnaire system foundation: questionnaire (template with native/external dual-mode), questionnaire_field (with section_name grouping), questionnaire_instance (per-activity filling with GUID), questionnaire_field_value (answers), and 3 scoping join tables (LOS, Enhancement, ServiceItem). Requires updated WAR with 4 new entity classes and QuestionnaireLoader service.
- V040 adds recurring_series_id VARCHAR(36) and recurring_cycle_number INT to delegated_todo. Enables recurring checklist history tracking across BPO push cycles.
- V041 adds reviewed_by (BIGINT FK → assignee), review_notes (TEXT), and date_reviewed (TIMESTAMP) to the application table. Uses conditional DDL (information_schema checks) since columns may already exist on some environments. Requires updated WAR with Application Visibility & Role Walls feature.
- V042 documents the introduction of the PENDING status value for delegated_todo. No DDL changes — status VARCHAR(30) already exists. When autoAcceptTasks is OFF for a PspClient, incoming tasks arrive as PENDING and require BPO Admin approval before becoming ACTIVE.
- V043 adds nullable text_value TEXT column to the constant table. Used for storing large text content (custom landing page HTML). The existing value VARCHAR column is unchanged. Requires updated WAR with Constant entity textValue field, AmsDataGlobal custom landing cache, login.java routing, UpdatePspSettings AJAX save, and new customLanding25.jsp.
- V044 adds nullable agency_id BIGINT FK to proposal_section (references agency). Enables agency-scoped TITLE/CLOSING overrides — when an agency-scoped section exists for a proposal's agency, it renders instead of the default. Includes unique index on (psp_id, agency_id, section_type). Requires updated WAR with ProposalSection entity agency field, ProposalSettings create/delete/toggle changes, ViewProposal agency resolution logic, and proposalSettings.jsp Agency Overrides UI.
- V045 adds selected_los_ids VARCHAR(500) and selected_enhancement_ids VARCHAR(500) to the application table. Stores which LOS and Enhancement services the applicant selected during proposal application. Comma-separated ID format for simple storage without join tables. Requires updated WAR with Application entity fields, ApplyForProposal save/restore logic, and ReviewApplication display.
- V046 creates chatbot_skill table for per-PSP, admin-configurable AI chatbot skills. Each skill defines a specialized system prompt, trigger keywords, file acceptance rules (MIME types), model preference, max_tokens, admin-only toggle, and sort order. FK to assignee (PSP). Requires updated WAR with ChatbotSkill entity, ChatbotSkillDAO, refactored ChatAssistant (unified multipart/JSON endpoint with skill matching), SkillManager CRUD servlet, and skillManager25.jsp admin page.
- V049 adds source_task_id VARCHAR(20) to delegated_todo with composite index on (source_task_id, psp_client_id). Enables BPO auto-approval of required-sequence tasks (SETUP/RENEWAL): once the BPO approves a task the first time, future occurrences of the same source task from the same PSP are auto-accepted without manual approval. Requires updated WAR with DelegatedToDo sourceTaskId field, BpoTaskPushService sending sourceTaskId in payload, and TaskReceiveApi auto-approval logic for SETUP/RENEWAL activity types.
- V048 creates the universal import system: import_provider (TPA platform registry), import_file_type (file definitions per provider), import_field_mapping (column-to-canonical-field mappings), import_plan_type_mapping (provider plan codes → AMS plan types, nullable provider_id for universal defaults), and import_run_log (execution history). Seeds 17 universal plan type codes (FSA, HRA, HSA, DCA, COBRA, etc.). Requires updated WAR with 5 new entities in model/imports/, UniversalImportService, ProviderSetup servlet, and UniversalImport wizard servlet.
- V050 FK fix: original script referenced `person(id)` which doesn't exist as a standalone table (Person extends Assignee via JPA inheritance). Corrected to `assignee(id)`. On all environments the column was added but the FK failed silently — manually applied corrected FK on production, demo, and BPO (March 12, 2026).
- V061 adds four ownership-override columns to the todo table: override_ownership TINYINT(1), has_owner TINYINT(1), owner_id BIGINT (FK → assignee), and allow_non_owner TINYINT(1). Mirrors Task's ownership shape at the ToDo instance level. When override_ownership=0 the ToDo inherits from Task; when 1 the ToDo's own fields are authoritative. Index on owner_id supports the delegated_to_me EXISTS subquery in ActivityLandingDao. Enables per-Setup delegation to originating-agency agents without affecting the Task template. Requires updated WAR with ToDo entity fields, ToDoOut25 resolver swap, UpdateTask25 override branch, ActivityLandingDao SQL update, OriginatingAgencyResolver, taskManager25.jsp override sub-row, and AgentHome delegated-to-me panel.
- V062 adds nullable agent_visible TINYINT(1) column to the note table and indexes it. NULL = fall back to PSP default (constant key NOTES_AGENT_VISIBLE_DEFAULT, default 0 = hidden); 0 = hidden from agent portal; 1 = visible. Used by the agent-portal Setup detail view to filter internal PSP chatter from agent-visible status updates. Agent-authored nudge notes are always written with agent_visible=1 and surface on Waiting-On-Us via existing ActivityStatus id=3. Requires updated WAR with Note.agentVisible field, NOTES_AGENT_VISIBLE_DEFAULT added to UpdatePspSettings FEATURE_KEYS and PSP settings UI, AddNoteToActivity25 reading the toggle, NoteVisibilityResolver helper, GoActivityDetail25.agentBlocked() widened to selling-agent / agency-manager scope, AgentSetupList servlet + agentSetupList25.jsp, and agent-flavored filtering on the Setup detail JSP.
- V063 creates the knowledge_base, knowledge_chunk, and knowledge_chunk_history tables for AMS's AI knowledge system. Schema-only; no chunk content is seeded — content for DB-backed KBs arrives in subsequent migrations or via the future admin UI. Five KB registry rows are seeded: style_voice (ALWAYS_LOAD, communication style rules), federal_rules (Section 125/FSA/HSA/COBRA/DCAP/ICHRA/HRA/transit), ssa_business (offerings, procedures, fees, escalation chains), summit_supplemental (SSA tribal Summit gotchas), and summit_official (JSON-backed pointer to the existing summit_guide_indexed.json classpath file). The registry is now the single enumeration point for all knowledge bases, DB and JSON alike. No Java entities, DAOs, or UI in this round — schema only.
- V064 seeds two platform-JSON registry rows in `knowledge_base` (proposal_page_builder, automation_email_builder) that were missed by V063 because they were intended to be seeded by `DatabaseInitializer.seedKnowledgeBaseRegistry()`, which only runs on fresh PSP installs. Idempotent via `INSERT IGNORE`.
- V065 adds a unique index on `chatbot_skill(psp_id, skill_name)` and seeds the `EMAIL_DRAFT_ASSISTANT` skill row for the default PSP (psp_id=4). Powers the Outlook add-in Draft AI Reply feature (EmailDraftService).
- V066 creates `proposal_price_adjustment` (id, proposal_id, module_id, price_item_id, markup_amount DECIMAL(10,2) DEFAULT 0, created_by, date_created) with a unique key on (proposal_id, module_id, price_item_id) and a CHECK constraint enforcing markup_amount >= 0 (upward-only markup). FKs: proposal_id -> proposal(proposal_id), module_id -> servicemodule(module_id), price_item_id -> priceitem(price_item_id), created_by -> assignee(id). Lets PSP admins and selling agents add a flat-dollar markup to any proposal pricing line (setup, annual, PEPM, enhancements, and $0 "Included" lines); sell price = RateTable.price + markup, computed at read time, never stored on RateTable itself (which is shared across proposals/agencies). Requires updated WAR with ProposalPriceAdjustment entity, ProposalPriceLine DTO, SalesDAO.getPricingWithAdjustments()/saveProposalPriceAdjustment(), ProposalDetail `saveMarkup` action, and the base\|markup\|sell breakdown UI in proposalDetail.jsp (internal, full breakdown) vs. proposalPricing.jsp (public GUID page, sell price only — base and markup are never emitted in that page's HTML).
- V067 adds `markup_enabled TINYINT(1) NOT NULL DEFAULT 0` to `agency` — OFF for every existing and future agency, no backfill. Gates the V066 markup feature per-agency: hides the editable markup UI and rejects the `ProposalDetail` `saveMarkup` action server-side when the proposal's originating agency isn't enabled (or no agency resolves — the PSP-direct case is also OFF, no admin exception). Does NOT touch `SalesDAO.getPricingWithAdjustments()` or the sell-price math — markups already saved under V066 keep computing into sellPrice even if their agency is later disabled; only the ability to add/change a markup is gated. Originating agency resolved via the new `OriginatingAgencyResolver.resolve(Proposal)` overload (sourceActivity -> prospect.agent -> createdBy, same order as the V061 Setup-based resolver). Requires updated WAR with Agency.markupEnabled field, OriginatingAgencyResolver Proposal overload, ProposalDetail combined gate (both doGet and the saveMarkup doPost branch), AgencyAction editAgency read/write + a new explicit isPspAdmin guard on AgencyAction.doPost, and an "Enable agent markup" checkbox in agencyManager25.jsp's Edit Agency modal.
- V068 adds `landing_host VARCHAR(255) NULL` (UNIQUE index `uq_agency_landing_host`) and `landing_html MEDIUMTEXT NULL` to `agency`, extending the V043 PSP-wide custom-landing feature to a per-agency branded front door. When AMS is reached over a non-PSP host (anything other than the exact hosts in `ssa.properties` `PSP_HOSTS`, default `superiorstate.net,superiorstate.biz`), `login.routeLogin` normalizes `request.getServerName()`, resolves an agency by that host (case-insensitive), and — only if its `landing_html` is non-blank — forwards to `customLanding25.jsp` with a `whiteLabel` request attribute (suppressing the PSP header chrome) plus `Content-Security-Policy: frame-ancestors 'none'` and `X-Content-Type-Options: nosniff`. Every miss (PSP host, unmatched host, matched-but-blank) falls through to the existing PSP landing/login flow, unchanged. The white-label switch is the PRESENCE of non-blank `landing_html` — no separate enable flag. `AmsDataGlobal` caches a `host -> agency_id` map built from the already-cached agency list (skips suppressed agencies per V057, keys on normalized-lowercase host, non-blank only), rebuilt in `initializeGlobalData` and `refreshSalesData` — so the existing `AgencyAction.refreshSalesData` call after every edit invalidates it for free. Landing HTML is sanitized on save by the new parser-based `net.superiorstate.ams.data.util.LandingSafe` (Jsoup safelist preserving `<style>`/font `<link>`/layout tags/`class`/`id`/inline `style`/relative links, blocking `<script>/<iframe>/<object>/<embed>/<form>`/`on*`/dangerous URL+CSS vectors); the same sanitizer also replaces the weaker regex in `UpdatePspSettings.saveLandingHtml` for the PSP landing. Requires updated WAR with Agency.landingHost/landingHtml fields, AmsDataGlobal host map + `isPspHost`/`getAgencyIdForHost`/`PSP_HOSTS` reader, login.java host dispatch, AgencyAction `saveAgencyLandingHtml` AJAX action + `landing_host` read/validate (format check, lowercase+trim, duplicate + PSP-host rejection) in editAgency, LandingSafe util, customLanding25.jsp chrome gate, and the host field + landing-HTML panel in agencyManager25.jsp's Edit Agency modal. Prerequisites: V057 (agency.suppressed), V067 (agency.markup_enabled). Ops prerequisite (separate track): set `PSP_HOSTS` in ssa.properties per environment; DNS/TLS for vanity hosts (`*.superiorstate.net` feasible now via Cloudflare; customer domains need Cloudflare-for-SaaS).
- V069 adds `email_domain VARCHAR(255) NULL` (UNIQUE index `uq_agency_email_domain`) and `email_verified TINYINT(1) NOT NULL DEFAULT 0` to `agency`, adding per-agency white-label email sending on top of the V068 landing feature. Fixes a live deliverability gap: because `SMTP_FROM` is blank in prod, `EmailDAO` previously set `From = Reply-To = envelope-from = the sender's own address`, so non-PSP agents already sent `From:` an unverified domain (DMARC-fail/spam). A new `EmailIdentityResolver` resolves a four-tier sender identity at the single `EmailDAO` choke point: **Tier 0** (sender domain in `ssa.properties` `VERIFIED_PSP_DOMAINS`, default `superiorstate.net,superiorstate.biz`) → From = sender's own address + full-name display; **Tier 1** (agency `email_domain` non-blank AND `email_verified=1`) → From = `<sender-localpart>@<email_domain>` + full-name display, Reply-To = sender's real address; **Tier 2** (any other human sender) → From = `FALLBACK_FROM` (DB constant, default `notifications@superiorstate.net`) + sender full-name display, Reply-To = sender's real address; **System** (new-user, password/help) → From = `noreply@superiorstate.net`, no Reply-To. Invariant: every human tier sets Reply-To = the sender's real inbox. Tier-1 grafts the sender's real localpart onto `email_domain`; a malformed localpart drops to Tier 2 rather than emit a broken From. `email_verified` is a manual PSP-admin flag (v1, no SMTP2GO API readback) and MUST NOT be flipped until SMTP2GO shows the domain Verified (SPF return-path CNAME `em102001` + DKIM `s102001._domainkey` at the subdomain). Also **removes** the unconditional `mail.smtp.from` envelope override in `EmailDAO` so SMTP2GO's per-domain VERP owns the return-path (verified-domain SPF alignment); `EmailIdentity` keeps a nullable `envelopeFrom` hook (null in all tiers for v1). Requires updated WAR with Agency.emailDomain/emailVerified fields, `EmailIdentity` value object, `EmailIdentityResolver`, `EmailDAO` identity-aware overload + display-name From + envelope change, `OriginatingAgencyResolver.resolve(Person)` (promoted from private `agencyOf`), call-site wiring across the 8 send paths (system paths use `systemIdentity()`), deletion of the dead `AmsDataLocal.sendEmail`, an `EmailTemplate` signature-source fix, and Sending Domain + Verified fields in agencyManager25.jsp's Edit Agency modal. Prerequisites: V057, V067, V068. Ops prerequisite (separate track): set `VERIFIED_PSP_DOMAINS` in ssa.properties; verify each agency `email_domain` in SMTP2GO before flipping `email_verified`.
- V072 creates billing_run (one row per Monthly Billing Launcher run: status, current_step, mode FULL/BILLING_ONLY, plan_type_supplied, renewals_refreshed flag, error_text, launched_by FK→assignee) and billing_run_step (one row per pipeline step: WIPE/IMPORT/PROMOTE/CLEAR_BILLING/CREATE_BILLING with status, timings, detail). Schema only — no data migration; rows are written at runtime by the launcher's background worker. Requires an updated WAR with BillingRun/BillingRunStep entities (added in a later increment). SSA production only.
- V073 widens billing_run.current_step VARCHAR(40)→VARCHAR(255). V072 sized it too short for the worker's "CREATE_BILLING: <label> (n/14)" progress labels, which threw MySQL 1406 and failed a run mid-CREATE_BILLING. Schema-only; no code change (the app already writes the labels). Recover an affected run via a BILLING_ONLY re-run.
- V074 creates rating_area_rate_cache (plan_year, county_fips, state, age, uses_tobacco, market_low/high_premium, lcsp_premium, benchmark_silver_premium, lowest_bronze_premium, carrier_count, plan_count, fetched_at, source_env) — per-county-per-age premium cache for the A1 ICHRA rating illustration, warmed by a scheduled background job (one HealthSherpa API call per county per plan year via the statutory age-rating curve, not per age). uses_tobacco is present but unused pending open item O19. Schema only — no data migration. Requires updated WAR with RatingAreaRateCache entity, RateCacheDAO, AgeCurve, RateCacheWarmService, and RateCacheAdmin (added in this increment; the illustration servlet/page itself is a separate, not-yet-approved phase).
- V075 creates illustration_log (created_at, agent_person_id FK→assignee, agency_id/parent_agency_id FK→agency, zip_code, county_fips, state, plan_year, eligible_headcount, mode, cache_hit, result_summary) — audit/analytics log for A1 illustration runs. Holds no PII; result_summary is a short computed descriptor, never a name or employer identifier. parent_agency_id is deliberately denormalized (no GA hierarchy-walk helper exists in the codebase). Schema only — no data migration.
- V076 creates county_reference (county_fips CHAR(5) PK, state, county_name, representative_zip) — national county identity data for the B-2 agent-facing ICHRA illustration, seeded for Texas only (254 rows) via `docs/scripts/generate_county_reference.py` against Census Gazetteer + 2020 ZCTA-relationship source files. Not PSP-scoped — identical on every installation, matching the V074/V075 precedent. Supersedes the `zip:fips:state` triple format of `RATE_CACHE_COUNTIES` (D-83) by letting `RateCacheWarmService` resolve a bare county_fips through `CountyReferenceDAO`; the triple form remains fully supported. Schema only — no Java seed mirror (see CLAUDE.md's DatabaseInitializer note). Requires updated WAR with CountyReference entity and CountyReferenceDAO.
- V077 adds `ichra_enabled TINYINT(1) NOT NULL DEFAULT 0` to `agency` — OFF for every existing and future agency, no backfill, mirroring the V067 `markup_enabled` precedent. Backs the new `IchraAccessResolver.isAvailable(EntityManager, HttpServletRequest)` (session PSP-admin → available; otherwise resolve the caller's primary agency via `AgencyScopeResolver` and read this flag → available if set; anything else, or any exception, fails closed to not-available). Gates both the `/Illustration` servlet guard and the new `/IchraHome` hub servlet guard with the same resolver call, and the top-level ICHRA nav entry in `navbar25.jsp`. No agency is entitled by this migration — Kevin flips it per agency at deployment time. Requires updated WAR with Agency.ichraEnabled field, IchraAccessResolver, IchraHome servlet + ichraHome25.jsp hub, and the navbar/IllustrationServlet gate changes.
- V078 adds `onex_lcsp_premium DECIMAL(8,2) NULL` and `onex_benchmark_silver_premium DECIMAL(8,2) NULL` to `rating_area_rate_cache` — same type/precision as the existing `lcsp_premium`/`benchmark_silver_premium` (V074), nullable, no default, no backfill. T44: a live staging probe against Hopkins TX (48223, PY2026, 2026-07-31) confirmed the existing off-exchange-derived `lcsp_premium` understates the true on-exchange LCSP by ~44% at age 40 ($489.38 vs. $705.37) — the dangerous direction for affordability (LA-12). `lcsp_premium`/`benchmark_silver_premium` are unchanged in name, meaning and data; the illustration keeps displaying them as off-exchange figures. `RateCacheWarmService` fetches one additional on-exchange base quote per county-year (age 21, mirroring the existing off-exchange base-call-plus-`AgeCurve`-scaling strategy — the probe confirmed the federal age curve holds identically on both rails, so this does not become a per-age call), non-fatal on failure (columns stay null for that county-year). Schema only — populated by the next warm run after this migration and the corresponding WAR ship; not yet read by anything (item 9 is the first reader). Requires updated WAR with RatingAreaRateCache.onexLcspPremium/onexBenchmarkSilverPremium fields and RateCacheWarmService's on-exchange derivation.
- V079 creates `proposal_ichra_snapshot` (one row per proposal, `proposal_id` FK UNIQUE ON DELETE CASCADE, `mode`, `county_fips`/`state`/`county_name` denormalized, `plan_year`, `contribution`/`headcount` inputs, `group_monthly_low`/`group_monthly_high`/`group_net_total`/`employer_outlay` outputs, `source_env` NOT NULL, `rates_fetched_at`, `snapshot_at`, `created_by` FK→assignee) and `proposal_ichra_snapshot_band` (AGE_BAND rows only, FK ON DELETE CASCADE to the parent, `age`/`lives`/`floor_premium`/`net_per_employee`/`band_net`/`sort_order`). S2 overturned from re-derive to snapshot on the Phase A finding (2026-07-31) that no per-proposal section content table exists anywhere — every existing `ProposalSection` is static template HTML or reads the `Proposal` graph directly, so re-derive needed a new table too and was never the cheaper path. No affordability column of any kind — the schema is structurally incapable of carrying one, since `/proposal/*` is public and unauthenticated (LA-12). DECIMAL precision copied exactly from `rating_area_rate_cache` (V074). Not a scenario system — `proposal_id UNIQUE` enforces exactly one design per proposal; N scenarios later costs dropping that index and adding `is_selected`, not built now. Schema only — populated by `ProposalBuilder.createProposal` when ICHRA hand-off params are present; read by `ViewProposal`/`proposalIchra.jsp` via the new `ICHRA_ILLUSTRATION` `ProposalSection` type, created through `ProposalSettings`'s admin UI once an ICHRA LOS exists. Requires updated WAR with `ProposalIchraSnapshot`/`ProposalIchraSnapshotBand` entities, `ProposalIchraSnapshotDAO`, and the `ProposalBuilder`/`ProposalSettings`/`ViewProposal` wiring.
- V080 seeds the ICHRA/QSEHRA Design Advisor (build-plan item 12 / A6): one `chatbot_skill` row (`ICHRA_DESIGN_ADVISOR`, psp_id=4, `is_admin_only=1`) plus an `ichra_design` `knowledge_base` registry row and 18 `knowledge_chunk` rows. **Data only — no new tables, no Java, no JSP, no UI entry point**, so no WAR change is required; the skill is reachable through the existing `/ChatAssistant` chatbot surface. Gating is rule-2 by construction: `ChatAssistant.doPost` strips admin-only skills for any caller who is not `isPspAdmin`/`isBpoAdmin`, and `KnowledgeSearchService.getEligibleKBs(false)` returns only `summit_official`/`summit_supplemental`, so a non-admin reaches neither the skill nor the chunks. `IchraAccessResolver` is deliberately **not** called — there is no new UI element to gate. Content traces to `domain_and_compliance_rules.md` §1/§5, `ichra_strategy.md` §7, `ichra_administration_scope.md`, and the LA-numbered assumptions in `legal_assumptions.md`, with settled rules and assumptions kept distinct; the skill declines ICHRA notice timing outright per LA-08. Idempotent: `INSERT IGNORE` on the skill (via V065's `uq_cs_psp_name`) and the KB row (via V063's `uq_kb_key`); the chunks have no natural unique key so each is guarded with `WHERE NOT EXISTS` on (kb_id, title). ⚠️ The rule text is duplicated between `system_prompt` and the chunks **by design** — `ChatAssistant.executeSkill` injects no KB content, so a matched skill's system prompt is its entire context, while the chunks serve the no-match fallback path and the Knowledge Manager edit UI. A `SYNC-GUARD` comment in the script records this. Prerequisites: V046, V063, V065.
- V092 seeds the HSA Enrollment Assistant. Data only — no new tables, no Java, no JSP, no UI entry point, so no WAR change is required; both consuming surfaces already exist. One `chatbot_skill` row (`HSA_ENROLLMENT_ASSISTANT`, psp_id=4) serves `/ChatAssistant`, and three `knowledge_chunk` rows in the `ssa_business` KB serve the Outlook add-in's "Draft AI Reply" (`EmailDraftService`). **Both are required** — a skill row alone reaches only the chatbot, because `EmailDraftService` never does keyword skill matching (it loads the fixed `EMAIL_DRAFT_ASSISTANT` row from V065 and grounds in `SEARCH_KBS`), and chunks alone reach only the drafter, because `ChatAssistant.executeSkill` sends the matched skill's `system_prompt` as its entire context and injects no KB content. The procedure is Kevin's own verbatim reply to Susie Hartshorne (Owner/Operator Services), "Re: Set up new employee", 2026-08-18. The one field neither surface can supply is the per-employer myRSC employer code: `Setup.myRsc` is the only such column in the codebase and nothing reads or writes it (grep-verified 2026-08-18), so both artifacts emit the literal token `[[MYRSC EMPLOYER CODE]]` and instruct the PSP user to replace it from myRSC before sending, with an explicit prohibition on guessing, deriving, or carrying a code over from another employer. Chunk 2b also states that a missing employer code is **not** a confidence escalation — without that, V065's system prompt would downgrade the reply to a HOLDING_ACKNOWLEDGMENT over one hand-filled field. Idempotent: `INSERT IGNORE` on the skill (via V065's `uq_cs_psp_name`); the chunks have no natural unique key so each is guarded with `WHERE NOT EXISTS` on (kb_id, title), the V080 pattern. ⚠️ Two traps recorded in the script's comments: `knowledge_chunk.keywords` are matched by **exact token** (`queryWords.contains` against a whitespace tokenizer), so a multi-word keyword can never match and every keyword seeded here is a single lowercase token; and the chunks stay invisible after apply until `KnowledgeSearchService.reload()` runs — restart Tomcat or click "Reload cache" in Knowledge Manager. Prerequisites: V046, V063, V065.
