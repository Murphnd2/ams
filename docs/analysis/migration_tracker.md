# Database Migration Tracker

Tracks database schema versions across environments.

**Last Updated:** March 4, 2026

## Environments

| Environment | Host | Schema | Notes |
|-------------|------|--------|-------|
| Local (Work) | 127.0.0.1:3306 | beta_ssa | Dev workstation |
| Local (Home) | 127.0.0.1:3306 | beta_ssa | Home workstation |
| Local (either) | 127.0.0.1:3306 | dev_ssa | Initialization testing (wiped regularly) |
| Production | superiorstate.biz | beta_ssa | Live server |
| Demo PSP | demo.superiorstate.biz | beta_ssa | Conference demo PSP (V038, seeded, release V0.37.0) |
| BPO | bpo.superiorstate.biz | beta_ssa | BPO instance (V038, initialized, release V0.37.0) |
| Master | master.superiorstate.biz | beta_ssa | Snapshot v8 (V037, stopped) |

## Current Highest Version: V039

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

Individual migration scripts are no longer stored in the repo. The baseline dump + `schema_version_migration.sql` are the source of truth. Session summaries document what each version changed.

## Version History

| Version | Description | beta_ssa (work) | beta_ssa (home) | dev_ssa | Production | Demo PSP | BPO | Master |
|---------|-------------|-----------------|-----------------|---------|------------|----------|-----|--------|
| V001–V019 | Sales pipeline through application field suppressed | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V020 | ServiceItem unification - schema + backfill | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V021 | ServiceItem linkage - LOS/Enhancement backfill | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V022 | Orphaned ticket ServiceItem backfill | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V023 | Drop ticketsubcategory table and FK | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V024 | Fix views referencing dropped ticket_category column | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| V025 | Add level, los, employer_name to plantype for Summit import | ⬜ | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ |
| V026 | Benefit table: surrogate auto-increment PK with source tracking | ⬜ | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ |
| V027 | BPO Registration: task source refactor from Person to BpoRegistration | ⬜ | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ |
| V028 | Benefit plan year start/end columns for renewal date correction | ⬜ | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ |
| V029 | Add is_active column to user table for user deactivation | ⬜ | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ |
| V030 | BPO cross-system foundation: psp_clients, delegated_todo, API columns, todo_note GUID | ⬜ | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ |
| V031 | ToDoNote cross-system: nullable todo_id/created_by_id, author_name column | ⬜ | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ |
| V032 | Approved vendors registry table | ⬜ | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ |
| V033 | ToDoNote attachments: todo_note_id FK on weblink | ⬜ | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ |
| V034 | Add template_key to applicationsection for starter packages | ⬜ | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ |
| V035 | Feature headline column and description widening | ⬜ | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ |
| V036 | Proposal section table for composable proposal content | ⬜ | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ |
| V037 | Add LOS/Enhancement scoping to proposal_section | ⬜ | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ✅ |
| V038 | Add sort_order to delegated_todo for BPO ordering | ⬜ | ⬜ | ⬜ | ⬜ | ✅ | ✅ | ⬜ |
| V039 | Questionnaire system: templates, fields, instances, values, scoping | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ | ⬜ |

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
- Production remains at V024 and is intentionally isolated from conference demo infrastructure.
- V034 adds nullable template_key VARCHAR(50) to applicationsection with a unique index scoped to (template_key, psp_id). Used for starter package duplicate detection. NULL values (manual/seeded sections) are unaffected by the unique constraint.
- V035 adds nullable headline VARCHAR(200) to feature table for short punchy summary text. Widens description from VARCHAR(500) to VARCHAR(2000) for paragraph content. Part of the proposal customization feature (feature sales blurb upgrade).
- V036 creates the proposal_section table for composable proposal content per PSP. Section types: TITLE, PRICING, FEATURES, CLOSING, CUSTOM. Supports HTML content with merge tokens, sort ordering, and active/inactive toggling.
- V037 adds scope column (VARCHAR(10), default 'ALL') to proposal_section and creates proposalsectionlos/proposalsectionenhancement join tables. Enables CUSTOM sections to display only when specific services are proposed.
- V038 adds sort_order INT DEFAULT 0 to delegated_todo. Stores the task's position within its source checklist, included in the BPO push payload from PSP. Enables BPO dashboard sorting by due date → activity name → sort order.
- V039 creates questionnaire system foundation: questionnaire (template with native/external dual-mode), questionnaire_field (with section_name grouping), questionnaire_instance (per-activity filling with GUID), questionnaire_field_value (answers), and 3 scoping join tables (LOS, Enhancement, ServiceItem). Requires updated WAR with 4 new entity classes and QuestionnaireLoader service.
