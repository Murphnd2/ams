# AMS `docs/` Index

> **Pass 2 current-state update — 2026-07-15.** The Pass 1 tables (below) were accurate as of 2026-04-25 / migration V062 / Session 87. This section supersedes them where they disagree. When re-running the full inventory, fold this back into the tables.

## Pass 2 — what changed since Pass 1

**Highest migration:** now **V071** (Pass 1 stopped at V062). Added since:

| File | Description | Tag |
|---|---|---|
| V063__knowledge_base_tables.sql | knowledge_base / knowledge_chunk / knowledge_chunk_history + 5 KB registry rows | historical (applied) |
| V064__platform_json_registry_seed.sql | proposal_page_builder + automation_email_builder KB registry rows | historical (applied) |
| V065__email_draft_assistant_skill.sql | unique index on chatbot_skill + EMAIL_DRAFT_ASSISTANT seed | historical (applied) |
| V066__proposal_price_adjustment.sql | per-proposal, per-line agent markup on pricing | historical (applied) |
| V067__agency_markup_enabled.sql | per-agency enable flag for V066 markup (default OFF) | historical (applied) |
| V068__agency_landing_host.sql | host-header custom agency landing pages | historical (applied) |
| V069__agency_email_sending.sql | per-agency white-label email (email_domain + email_verified) | historical (applied) |
| V070__agency_parent.sql | self-referential GA→sub-agency parent link | historical (applied) |
| V071__agency_quote_token.sql | per-agency public quote token for RequestQuote attribution | historical (applied) |
| seed_ndt125_questionnaire.sql | NDT-125 questionnaire seed data | unclear (seed, not versioned) |

**New docs since Pass 1:**

| Path | Description | Tag |
|---|---|---|
| `docs/analysis/host_agency_landing_phase1_plan.md` | Host-agency landing (V068) phase-1 plan | historical (shipped) |
| `docs/analysis/host_agency_landing_phase2A_confirmation.md` | Phase-2A confirmation note | historical (one-off) |
| `docs/analysis/email_identity_current_state.md` | Email sender-identity current-state map (pre-V069) | current |
| `docs/analysis/email_whitelabel_phaseA_confirmation.md` | White-label email (V069) phase-A confirmation | historical (one-off) |
| `docs/analysis/email_assistant_handoff.md` | Email Draft Assistant session handoff prompt | historical (one-off) |
| `docs/analysis/email_assistant_calibration_log.md` | Email-assistant calibration log | current (living log) |
| `docs/analysis/proxy_readiness_audit.md` | Reverse-proxy / host-routing readiness audit | current |
| `docs/analysis/proxy_readiness_audit_prompt.md` | The prompt that generated the audit | historical (one-off) |
| `docs/analysis/qsehra_attestation_claims_engine.md` | QSEHRA monthly-attestation claims-engine design (backlog #38) | current (design, not started) |
| `docs/infrastructure/production_architecture.md` | Production host/proxy architecture | current |
| `docs/infrastructure/letsencrypt_renewal.md` | Let's Encrypt renewal procedure | current |
| `docs/infrastructure/cloudflare_setup.md` | Cloudflare setup for vanity/customer hosts | current |

**Re-tagged since Pass 1** (resolved in `AMS-OPEN-QUESTIONS.md`): `questionnaire_system_design.md` → **implemented (V039)**; `serviceitem_unification_design_v2.md` → **implemented (V020)**; `summit_import_design.md` → **shipped** (Summit + Universal converge, not one replacing the other); `sales_pipeline_reference.md` "Replaces" → reworded "Supersedes".

**Archive candidates** (one-off/session-scoped — Phase 2 of the 2026-07 cleanup will move these under `docs/analysis/archive/`): `session_86_notes.md`, `bpo_feature_session_history.md`, `outlook-addin-handoff.md`, `email_assistant_handoff.md`, `host_agency_landing_phase2A_confirmation.md`, `email_whitelabel_phaseA_confirmation.md`, `proxy_readiness_audit_prompt.md`, `dead_jsp_cleanup_summary.md`, `dead_jsp_investigation.md`.

---

## `docs/` top-level (Pass 1)

| Path | Description (from file head) | Last commit | Touches | Tag |
|---|---|---|---|---|
| `docs/ams_to_be_vision.md` | "Activity Management System — To-Be Vision Document"; created Feb 2026, target milestone "Datapath Client Conference, April 20–22, 2026"; product/domain strategy and roadmap (docs/ams_to_be_vision.md:1-7). | 2026-02-20 | 3 | current |
| `docs/claude_memory.md` | "Cross-Workstation" Claude bootstrap file; describes itself as a slim travel-via-git companion to per-machine `~/.claude/.../MEMORY.md`; lists current branch, latest migration, session count (docs/claude_memory.md:1-7). | 2026-04-24 | 37 | current |
| `docs/custom-landing-page-prompt.md` | Authoring-guide prompt for generating HTML content for AMS custom landing pages; describes wrapper page constraints and CSS scoping (docs/custom-landing-page-prompt.md:1-7). | 2026-03-06 | 1 (single-commit) | current |
| `docs/deployment_backlog.md` | "Deployment Backlog" — items ordered by dependency; tracks D-01… backlog work for multi-PSP deployments (docs/deployment_backlog.md:1-7). | 2026-04-11 | 45 | current |
| `docs/deployment_runbook.md` | "PSP Deployment Runbook" — step-by-step procedures for deploying new PSP installations; references deployment_strategy.md and tomcat_ssl_setup.md (docs/deployment_runbook.md:1-5). | 2026-03-20 | 7 | current |
| `docs/deployment_strategy.md` | "SSA/AMS Deployment Strategy" — multi-PSP deployment architecture; status "Active — BPO deployed, Demo PSP next" (docs/deployment_strategy.md:1-7). | 2026-03-20 | 7 | current |
| `docs/outlook-addin-handoff.md` | "Outlook Add-in — Session 78 Handoff Prompt"; copy-paste prompt for continuing the add-in build in another Claude Code session (docs/outlook-addin-handoff.md:1-7). | 2026-04-12 | 1 (single-commit) | unclear (it is a one-shot session handoff prompt; current state of the Outlook add-in code is up-to-date but this prompt describes a specific point-in-time hand-off from Session 78). |
| `docs/preview-pages.html` | Standalone HTML harness ("Proposal Page Preview"): scaffolding to render proposal page HTML blocks at print sizes (docs/preview-pages.html:1-9). | 2026-03-06 | 1 (single-commit) | current |
| `docs/sample-landing-content.html` | Sample landing-page HTML using SSA brand fonts/colors (`--teal #005F73`, `--olive #7A9B3C`, `--navy #003049`) — content authoring sample for `custom-landing-page-prompt.md` (docs/sample-landing-content.html:1-9). | 2026-03-06 | 1 (single-commit) | current |
| `docs/schema_version_migration.sql` | DDL for the `schema_version` table + INSERT statements for every applied version. Per CLAUDE.md, every migration script self-registers into this table (CLAUDE.md:55-65). | 2026-04-24 | 48 | current |
| `docs/serviceitem_unification_design_v2.md` | Design v2 to consolidate four divergent activity-item→task-sequence paths into one model. Status: "Draft v2 — incorporating developer feedback", Feb 2026 (docs/serviceitem_unification_design_v2.md:1-7). | 2026-02-27 | 2 | unclear (tagged "Draft v2"; subsequent analysis docs reference unification complete in V020-V024 baseline — see AMS-OPEN-QUESTIONS.md). |
| `docs/tomcat_ssl_setup.md` | "SSL Setup with Nginx + Let's Encrypt"; referenced by deployment_runbook.md Phase 3; describes the production cert architecture (docs/tomcat_ssl_setup.md:1-9). | 2026-03-20 | 4 | current |

---

## `docs/analysis/`

| Path | Description | Last commit | Touches | Tag |
|---|---|---|---|---|
| `docs/business/datapath.md` | "DataPath Partnership Strategy — Complete Context"; partnership/business strategy with DataPath. **Moved 2026-07-15** from `docs/analysis/CONTEXT_DataPath_Partnership_Strategy.md` into the new `docs/business/` opportunity register (see `docs/business/README.md`). | 2026-04-23 | 2 | current |
| `docs/analysis/activity_detail_transition_plan.md` | "Activity Detail Page — Transition Plan"; status "Track A complete — A1–A14 + S4 done. S5 (mobile polish) remaining" (docs/analysis/activity_detail_transition_plan.md:1-5). | 2026-02-23 | 5 | current (in-progress per status field). |
| `docs/analysis/application_flow.md` | "SSA Web Application — Entry Point & Flow Analysis"; mapping of 100+ servlets and how requests flow (docs/analysis/application_flow.md:1-6). | 2026-02-21 | 2 | current (servlet count grew since — see AMS-OPEN-QUESTIONS.md). |
| `docs/analysis/bpo_cross_system_brainstorm.md` | "PSP ↔ BPO Cross-System Communication — Brainstorm & Process Plan"; explicitly self-labels "Brainstorm — not a build plan yet" (docs/analysis/bpo_cross_system_brainstorm.md:1-7). | 2026-03-01 | 1 (single-commit) | unclear (brainstorm; some elements clearly built — V030 BPO foundation — but the doc is not a "current state" reference). |
| `docs/analysis/bpo_feature_session_history.md` | "BPO Feature Implementation — February 24, 2026" — session-by-session log of building BPO delegation across three sessions (docs/analysis/bpo_feature_session_history.md:1-5). | 2026-02-24 | 1 (single-commit) | historical |
| `docs/analysis/database_seeding_plan.md` | Plan to align all DB seeding paths after the ServiceItem Unification project (V020–V024) (docs/analysis/database_seeding_plan.md:1-6). | 2026-03-01 | 1 (single-commit) | unclear (a plan, not a current-state description). |
| `docs/analysis/dead_jsp_cleanup_summary.md` | "Dead JSP Cleanup Summary"; result report from `cleanup/dead-jsp-removal` branch, 2026-03-03 (docs/analysis/dead_jsp_cleanup_summary.md:1-5). | 2026-03-03 | 1 (single-commit) | historical |
| `docs/analysis/dead_jsp_investigation.md` | Companion investigation report; "Forward reachability from servlet dispatch roots + recursive c:import/include tracing" (docs/analysis/dead_jsp_investigation.md:1-5). | 2026-03-03 | 1 (single-commit) | historical |
| `docs/analysis/email_workflow_analysis.md` | "Email Workflow Analysis"; status "Email system fully modernized" — SMTP + Wasabi storage + branded templates (docs/analysis/email_workflow_analysis.md:1-5). | 2026-02-18 | 2 | current |
| `docs/analysis/entity_reference.md` | "Entity Reference"; package root `net.superiorstate.ams.model`, EclipseLink + `ssaPU`, MySQL `beta_ssa`, `SINGLE_TABLE` rooted at `Assignee` (docs/analysis/entity_reference.md:1-7). | 2026-02-27 | 4 | current |
| `docs/analysis/migration_tracker.md` | DB migration tracker across environments; "Last Updated: April 23, 2026" (docs/analysis/migration_tracker.md:1-5). | 2026-04-24 | 60 | current |
| `docs/analysis/project_backlog.md` | "AMS Project Backlog"; references `docs/ams_to_be_vision.md` for full project descriptions (docs/analysis/project_backlog.md:1-5). | 2026-04-23 | 28 | current |
| `docs/analysis/questionnaire_system_design.md` | "Questionnaire System Design"; date March 3, 2026, status "Design — Not Started" (docs/analysis/questionnaire_system_design.md:1-5). | 2026-03-04 | 1 (single-commit) | unclear (status says "Not Started" but V039 questionnaire_system has shipped — see AMS-OPEN-QUESTIONS.md). |
| `docs/analysis/sales_pipeline_reference.md` | "Sales Pipeline — Reference Document"; explicitly says it **replaces** `sales_pipeline_data_model.md`, `sales_pipeline_implementation_log.md`, `sales_pipeline_session3_log.md` (docs/analysis/sales_pipeline_reference.md:1-7). | 2026-02-27 | 5 | current (those replaced docs are not present in the tree — see AMS-OPEN-QUESTIONS.md). |
| `docs/analysis/session_86_notes.md` | "Session 86 — Agent Portal Build-out (V062)"; per-session notes (docs/analysis/session_86_notes.md:1-5). | 2026-04-24 | 1 (single-commit) | historical |
| `docs/analysis/session_history_archive.md` | "Session History Archive"; "Consolidated historical record of all build sessions"; last updated April 24, 2026 (Session 87) (docs/analysis/session_history_archive.md:1-5). | 2026-04-24 | 73 | historical (by self-description) |
| `docs/analysis/summit_import_design.md` | "Summit Data Import & Sync — Design Document"; status "Draft — pending review", Feb 2026 (docs/analysis/summit_import_design.md:1-5). | 2026-02-28 | 2 | unclear (a design doc with "Draft" status; the system has clearly shipped — `SummitImportService.java`, `SummitImportWizard.java`, V048/V051-V053 migrations — but the design doc has not been updated). |

---

## `docs/importscript/`

| Path | Description | Last commit | Touches | Tag |
|---|---|---|---|---|
| `docs/importscript/beta_ssa_baseline_v031.sql` | SQL baseline dump up through V031 (file head shows it's a multi-MB SQL dump). | 2026-03-03 | 1 (single-commit) | historical (baseline at V031; current is V062). |
| `docs/importscript/beta_ssa_dev_baseline_thru_V024.sql` | SQL baseline dump up through V024. | 2026-02-27 | 1 (single-commit) | historical (older baseline). |
| `docs/importscript/ndt125_census_architecture.md` | "NDT-125 Census-Based Architecture: Design Document"; defines census-based nondiscrimination-testing architecture (docs/importscript/ndt125_census_architecture.md:1-7). | 2026-03-23 | 1 (single-commit) | unclear (large design doc 4,879 lines; V059 ndt_census_tables shipped, but doc itself isn't dated as "current state"). |
| `docs/importscript/ndt125_deduction_analysis.md` | NDT-125 cafeteria-plan-test field reduction analysis (89 fields → minimal set) (docs/importscript/ndt125_deduction_analysis.md:1-9). | 2026-03-23 | 1 (single-commit) | unclear |
| `docs/importscript/ndt125_minimal_questionnaire_design.md` | NDT Section 125 minimal-questionnaire redesign — 91 fields → 12-30 questions (docs/importscript/ndt125_minimal_questionnaire_design.md:1-7). | 2026-03-23 | 1 (single-commit) | unclear |

(The three NDT-125 markdowns under `importscript/` are unusually placed alongside SQL baselines — see AMS-OPEN-QUESTIONS.md.)

---

## `docs/migrations/` — V025 through V062 + seed

All 38 versioned files are single-commit, named `V{NNN}__{description}.sql`. Each file appears (per CLAUDE.md:55-65) to register itself in the `schema_version` table on apply. Highest committed version: **V062** (`V062__note_agent_visibility.sql`, 2026-04-24).

| File | Last commit | Tag |
|---|---|---|
| V025__plantype_import_columns.sql | 2026-02-28 | historical (applied) |
| V026__benefit_surrogate_pk.sql | 2026-02-28 | historical (applied) |
| V027__bpo_registration_task_source.sql | 2026-02-28 | historical (applied) |
| V028__benefit_plan_year_columns.sql | 2026-02-28 | historical (applied) |
| V029__user_is_active.sql | 2026-03-01 | historical (applied) |
| V030__bpo_cross_system_foundation.sql | 2026-03-02 | historical (applied) |
| V031__todo_note_cross_system_nullable.sql | 2026-03-02 | historical (applied) |
| V032__approved_vendors_registry.sql | 2026-03-04 | historical (applied) |
| V033__todo_note_attachments.sql | 2026-03-04 | historical (applied) |
| V034__starter_package_template_key.sql | 2026-03-04 | historical (applied) |
| V035__feature_headline_description.sql | 2026-03-03 | historical (applied) |
| V036__proposal_section_table.sql | 2026-03-03 | historical (applied) |
| V037__proposal_section_scoping.sql | 2026-03-03 | historical (applied) |
| V038__delegated_todo_sort_order.sql | 2026-03-04 | historical (applied) |
| V039__questionnaire_system.sql | 2026-03-04 | historical (applied) |
| V040__delegated_todo_recurring_series.sql | 2026-03-05 | historical (applied) |
| V041__application_reviewer_fields.sql | 2026-03-05 | historical (applied) |
| V042__delegated_todo_pending_status.sql | 2026-03-06 | historical (applied) |
| V043__custom_landing_page.sql | 2026-03-06 | historical (applied) |
| V044__proposal_section_agency_scoping.sql | 2026-03-10 | historical (applied) |
| V045__application_selected_services.sql | 2026-03-10 | historical (applied) |
| V046__chatbot_skill_table.sql | 2026-03-11 | historical (applied) |
| V047__composite_task_order.sql | 2026-03-11 | historical (applied) |
| V048__universal_import_system.sql | 2026-03-11 | historical (applied) |
| V049__delegated_todo_source_task_id.sql | 2026-03-11 | historical (applied) |
| V050__bpo_default_assignee.sql | 2026-03-12 | historical (applied) |
| V051__import_id_mapping.sql | 2026-03-12 | historical (applied) |
| V052__import_run_log_xref_tracking.sql | 2026-03-12 | historical (applied) |
| V053__interactive_import_enhancements.sql | 2026-03-12 | historical (applied) |
| V054__managed_installation.sql | 2026-03-15 | historical (applied) |
| V055__schema_info_view.sql | 2026-03-15 | historical (applied) |
| V056__training_video_tokens.sql | 2026-03-16 | historical (applied) |
| V057__agency_suppressed.sql | 2026-03-19 | historical (applied) |
| V058__questionnaire_renderer.sql | 2026-03-23 | historical (applied somewhere; per MEMORY.md not yet applied to all envs) |
| V059__ndt_census_tables.sql | 2026-03-23 | historical (per MEMORY.md "code-complete, not yet applied anywhere") |
| V060__outlook_user_link.sql | 2026-04-11 | current (per MEMORY.md "applied to local dev + production only") |
| V061__todo_ownership_override.sql | 2026-04-23 | current (per MEMORY.md "code-complete, not yet applied anywhere") |
| V062__note_agent_visibility.sql | 2026-04-24 | current |
| seed_ndt125_questionnaire.sql | 2026-03-23 | unclear (seed data, not a versioned migration) |

(MEMORY.md is the only available reference for "applied where" across environments; see AMS-OPEN-QUESTIONS.md for the inconsistency between V058-V062 in MEMORY vs. CLAUDE.md naming "V044 current".)

---

## `docs/mockups/`

| Path | Description | Last commit | Touches | Tag |
|---|---|---|---|---|
| `docs/mockups/addnote_redesign.html` | "Add Note — Redesign Options" mockup. | 2026-04-24 | 1 (single-commit) | current |
| `docs/mockups/agent_delegation_paths.html` | "Agent Delegation — UI Paths Mockup (v2)" — visualization of delegation flow paths. | 2026-04-23 | 1 (single-commit) | current |
| `docs/mockups/agent_home_delegated_tasks_mockups.html` | "AgentHome — Delegated Tasks, 3 Approaches" mockup. | 2026-04-24 | 1 (single-commit) | current |
| `docs/mockups/agent_setup_detail_mockup.html` | "Agent Setup Detail — Mockup" — supports the next session per MEMORY.md. | 2026-04-24 | 1 (single-commit) | current |
| `docs/mockups/bpo-detail-panel-mockup.html` | "BPO Detail Panel — Two-Zone Split Mockup v2". | 2026-03-19 | 1 (single-commit) | unclear (mockup; may have been implemented in Session 76's BPO redesign per MEMORY.md). |
| `docs/mockups/bpo-home-redesign.html` | "BPO Dashboard — Redesign Mockup". | 2026-03-11 | 1 (single-commit) | unclear (same as above). |
| `docs/mockups/center-panel-alternatives.html` | "Center Panel — Before vs After (Colored Tab)" mockup. | 2026-03-13 | 1 (single-commit) | unclear (Session 68 in MEMORY.md says "Center panel redesign" shipped — this mockup likely represents the design phase). |

---

## `docs/proposal_html/`

8 sample proposal HTML files (single-commit each). All committed 2026-03-06.

| Path | Notes |
|---|---|
| `docs/proposal_html/proposal-cobra-services.html` | Sample proposal page — COBRA services. |
| `docs/proposal_html/proposal-hra-decision-guide.html` | Sample proposal page — HRA decision guide. |
| `docs/proposal_html/proposal-hra-plan-types.html` | Sample proposal page — HRA plan types. |
| `docs/proposal_html/proposal-hsatoday-solution-page3.html` | Sample proposal page — HSA Today solution page 3. |
| `docs/proposal_html/proposal-lsa-basics-page1.html` | Sample proposal page — LSA basics page 1. |
| `docs/proposal_html/proposal-lsa-coverage-and-benefits-page2.html` | Sample proposal page — LSA coverage and benefits page 2. |
| `docs/proposal_html/proposal_fsa_one.html` | Sample proposal page — FSA, page 1. |
| `docs/proposal_html/proposal_fsa_two.html` | Sample proposal page — FSA, page 2. |

Tag: `unclear` for all — they appear to be source samples for the `.claude/skills/proposal-content-page/` skill but are not referenced by any application-side code in this inventory.

---

## `docs/scripts/`

| Path | Description | Last commit | Touches | Tag |
|---|---|---|---|---|
| `docs/scripts/backup.sh` | Bash — "SSA Database Backup Script" (docs/scripts/backup.sh:1-3). | 2026-03-20 | 1 (single-commit) | current |
| `docs/scripts/healthcheck.sh` | Bash — "SSA Health Check Script" (docs/scripts/healthcheck.sh:1-3). | 2026-03-03 | 2 | current |
| `docs/scripts/update.sh` | Bash — "SSA Update Script" (docs/scripts/update.sh:1-3). | 2026-03-16 | 3 | current |

---

## `docs/updates/`

| Path | Description | Last commit | Touches | Tag |
|---|---|---|---|---|
| `docs/updates/update_V039_to_V057.sql` | Concatenated upgrade script bundling V039 through V057. Per MEMORY.md it was generated for Demo/BPO. | 2026-03-19 | 1 (single-commit) | historical (a one-off bundle from Session 74). |

---

## Summary tags

| Tag | Count |
|---|---|
| current | ~22 |
| historical | ~45 (most migration files + session archive + cleanup reports) |
| unclear | ~14 |
| superseded | 0 explicit (`sales_pipeline_reference.md` says it replaces three other docs that are not present in the tree) |

See `AMS-OPEN-QUESTIONS.md` for unresolved questions and contradictions surfaced while reviewing these files.
