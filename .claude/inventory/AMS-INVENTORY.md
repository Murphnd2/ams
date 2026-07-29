# AMS Repository Inventory — Pass 1

> **⚠️ STALE SNAPSHOT.** Generated 2026-04-25 (Pass 1). Confirmed stale 2026-07-28 — this file still
> claims the highest migration is V062; the actual highest is **V073**. Structural/package
> information remains broadly useful; **any claim about migration numbers, current branch, or which
> reference-data rows exist is unreliable.** Verify against `docs/analysis/migration_tracker.md`,
> `docs/claude_memory.md`, live `DatabaseInitializer` code, or the database itself.
>
> See also the accuracy warning at the top of `docs/analysis/entity_reference.md` — several
> "what data exists" claims in these inventory files trace to the same dead seeder
> (`ReferenceDataSeeder.java`, unreachable via the commented-out `Main.java:35`).

**Generated:** 2026-04-25
**Branch:** `inventory/pass-1-ams`
**Repo root:** `C:\Users\kevinmurphy\IdeaProjects\ams`
**Scope:** Faithful documentation of files present in the working tree on this branch. Counts and paths reflect a directory walk that excluded `target/`, `out/`, `.git/`, `.idea/`, and binary build artifacts (`*.class`, `*.jar`, `*.war`, `*.log`, `*.zip`).

---

## 1. Top-level structure

| Path | Contents |
|---|---|
| `.claude/` | Claude Code workspace assets — skills, settings, worktrees, this inventory. Listed in `.gitignore` (.gitignore:5). |
| `.git/` | Git metadata. (skipped) |
| `.github/` | GitHub Actions workflows — single `build.yml`. |
| `.gitignore` | Ignore rules. (.gitignore:1-50) |
| `.idea/` | IntelliJ IDEA project metadata. (skipped) |
| `.mvn/` | Maven wrapper config — `wrapper/maven-wrapper.properties`. |
| `CLAUDE.md` | Project instructions for Claude Code (5,798 bytes). (CLAUDE.md:1-end) |
| `README.md` | Single line: `# AMS`. (README.md:1) |
| `ams-src.7z` | 7-zip archive (9.6 MB). Untouched since 2025-09-19 in git. |
| `ams.iml` | IntelliJ module file. |
| `demo/` | Sample CSVs, brochures, demo scripts (none loaded by app code per demo/README.md). |
| `docs/` | Documentation, migrations, mockups, scripts. |
| `mvnw`, `mvnw.cmd` | Maven wrapper launchers. |
| `out/` | IntelliJ build output. (skipped) |
| `plan.md` | Single-feature plan: "J5 Benefit Year Import + Renewal Date Audit" (plan.md:1-141). |
| `pom.xml` | Maven WAR project descriptor. (pom.xml:1-396) |
| `scripts/` | Single PowerShell script for Outlook icon generation. |
| `src/` | Java + webapp + resources. |
| `src-directory.txt` | UTF-16-encoded text dump of an older `src/main/java` tree (no subpackages — flat `controller/` listing). 386 KB, single-commit. (src-directory.txt:1-15) |
| `target/` | Maven build output. (skipped) |
| `tools/` | Single jar: `tomcat-jakartee-migration.jar`. |

---

## 2. Maven structure

**Single module.** No multi-module reactor.

### `pom.xml` (pom.xml:1-396)

- `groupId`: `biz.superiorstate`
- `artifactId`: `ams`
- `version`: `1.0.0-SNAPSHOT`
- `packaging`: `war`
- `<maven.compiler.release>`: 17 (pom.xml:16)

### Dependencies (group:artifact:version)

Compile / runtime:
- `jakarta.servlet:jakarta.servlet-api:5.0.0` (provided) (pom.xml:36-40)
- `jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.0.0` (provided) (pom.xml:41-46)
- `org.glassfish.web:jakarta.servlet.jsp.jstl:2.0.0` (pom.xml:49-53)
- `org.apache.logging.log4j:log4j-core:2.20.0` (pom.xml:56-60)
- `org.apache.logging.log4j:log4j-api:2.20.0` (pom.xml:61-65)
- `org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0` (pom.xml:66-70)
- `org.apache.poi:poi:5.2.3` (pom.xml:73-77)
- `org.apache.poi:poi-ooxml:5.2.3` (pom.xml:78-82)
- `com.opencsv:opencsv:5.9` (pom.xml:85-89)
- `com.mysql:mysql-connector-j:8.4.0` (pom.xml:92-96)
- `software.amazon.awssdk:s3` (BOM 2.20.69) (pom.xml:99-102)
- `software.amazon.awssdk:apache-client` (BOM 2.20.69) (pom.xml:103-106)
- `org.eclipse.persistence:org.eclipse.persistence.jpa:3.0.2` (pom.xml:109-113)
- `org.eclipse.angus:jakarta.mail:2.0.3` (pom.xml:117-120)
- `org.eclipse.angus:angus-activation:2.0.2` (pom.xml:121-125)
- `com.microsoft.azure:msal4j:1.21.0` (pom.xml:129-132)
- `com.microsoft.graph:microsoft-graph:5.52.0` (pom.xml:133-138)
- `org.jetbrains:annotations:24.1.0` (pom.xml:142-146)
- `org.jsoup:jsoup:1.17.2` (pom.xml:148-152)
- `org.jetbrains.kotlin:kotlin-stdlib:1.6.20` (pom.xml:169-172) — labeled "version alignment helpers"
- `org.jetbrains.kotlin:kotlin-stdlib-common:1.6.20` (pom.xml:174-177)
- `org.reactivestreams:reactive-streams:1.0.4` (pom.xml:179-182)
- `com.google.code.gson:gson:2.11.0` (pom.xml:185-189)
- `org.apache.pdfbox:pdfbox:3.0.4` (pom.xml:192-196)

Test:
- `org.junit.jupiter:junit-jupiter-api:5.8.2` (pom.xml:154-159)
- `org.junit.jupiter:junit-jupiter-engine:5.8.2` (pom.xml:160-165)

### Plugins
- `maven-compiler-plugin:3.11.0` — release 17 (pom.xml:218-225)
- `maven-war-plugin:3.3.2` — `failOnMissingWebXml=false` (pom.xml:227-236)
- `maven-enforcer-plugin:3.4.1` — three rules: ban a checked-in `persistence.xml`, ban legacy javax mail/activation, require upper bounds (pom.xml:238-303)
- `maven-antrun-plugin:3.1.0` (in profiles) — copies the chosen `persistence-*.xml` to `META-INF/persistence.xml` at process-resources time

### Profiles
- `server` (default; pom.xml:310-337) — copies `persistence-server.xml`
- `local` (pom.xml:340-367) — copies `persistence-local.xml`
- `local-dev` (pom.xml:368-394) — copies `persistence-local-dev.xml`

### Build output
WAR via standard Maven WAR packaging into `target/`.

---

## 3. Java packages

**Total Java files:** 444 (under `src/main/java`).
Files counted with `find src/main/java -name "*.java"`.

Single root: `net.superiorstate.ams`

Package tree, file count per leaf:

```
net.superiorstate.ams                                                  5
├── controller                                                         (—)
│   ├── activity                                                      17
│   │   ├── contact                                                    4
│   │   ├── ndt                                                        1
│   │   ├── questionnaire                                              3
│   │   ├── renewal                                                    5
│   │   ├── setup                                                     39
│   │   └── ticket                                                     1
│   ├── admin                                                          3
│   ├── api                                                           12
│   │   └── outlook                                                    6
│   ├── assistant                                                      4
│   ├── authentication                                                11
│   ├── checklist                                                     17
│   ├── data                                                          13
│   ├── email                                                         14
│   ├── home                                                          16
│   ├── market                                                         2
│   ├── monthly                                                       12
│   │   └── initial                                                    4
│   ├── sequence                                                       2
│   └── user                                                          15
├── data                                                               3
│   ├── dao                                                           23
│   ├── resolver                                                       8
│   ├── service                                                       25
│   ├── template                                                       0  (empty directory)
│   └── util                                                          14
├── filter                                                             1
├── model                                                             12  (root: Activity25, Constant, HomeData, etc.)
│   ├── activity                                                       4
│   │   ├── checklist                                                  4
│   │   │   ├── sequences                                              6
│   │   │   │   └── support                                            8
│   │   │   └── tasks                                                  6
│   │   ├── ndt                                                        3
│   │   ├── note                                                       5
│   │   ├── questionnaire                                              4
│   │   ├── renewal                                                    3
│   │   └── ticket                                                     4
│   │       └── setup                                                  1
│   ├── billing                                                        8
│   ├── general                                                       26
│   ├── imports                                                        6
│   ├── sales                                                          (—)
│   │   ├── agency                                                     9
│   │   ├── application                                                6
│   │   └── offering                                                  10
│   ├── summit                                                         (—)
│   │   ├── archive                                                    7
│   │   ├── imports                                                   14
│   │   │   └── order                                                 12
│   │   └── temp                                                       4
│   └── upload                                                         1
└── service                                                            1
```

### Top-level Java classes

`net.superiorstate.ams.*` (5 files): `AppConfig.java`, `EmfListener.java`, `LoginFilter.java`, `Main.java`, `NewInstall.java`. (src/main/java/net/superiorstate/ams)

### Servlet count

196 files contain `@WebServlet` (grep across `src/main/java`).

### Entity count

138 files contain `@Entity` (grep across `src/main/java`).

---

## 4. Webapp / web assets

Root: `src/main/webapp/`

### File counts (under `src/main/webapp`)

| Type | Count |
|---|---|
| `*.jsp` | 280 |
| `*.css` | 9 |
| `*.js` | 294 |
| `*.html` | 2 |

### Top-level files in `src/main/webapp/`

- `125eligibility.jsp`, `125eligibilitySuccess.jsp`
- `index.jsp`, `initialize.jsp`, `landing-page.jsp`, `login.jsp`
- `browserconfig.xml`
- `favicon.ico`, `logo.png`

### Top-level subdirs

| Path | Contents |
|---|---|
| `WEB-INF/` | `web.xml`, `view/` (JSP tree), `css/` |
| `ckeditor/` | 225 files — CKEditor 4 distribution + translations |
| `images/` | 8 PNG logo files (`logo1.png`, `logoA.png`, `logoC.png`, `logoD.png`, `logo_alt.png`, `logo_base.png`, `testLogoAlt.png`, `testLogoAltSmall.png`) |
| `outlook/` | 5 files — Outlook add-in: `manifest.xml`, `taskpane.html`, three icon PNGs, `README.md` |
| `resources/ckeditor5/` | 224 files — CKEditor 5 distribution (`ckeditor5.css`, `ckeditor5.js`, `.umd.js`, source maps, translations subdir) |
| `ufiles/` | 8 PDF files with UUID filenames |

### `WEB-INF/web.xml` (src/main/webapp/WEB-INF/web.xml:1-19)

Two `context-param` entries: `file-upload` (path-valued; src/main/webapp/WEB-INF/web.xml:6-12) and a Jasper `TldScanner.jarsToSkip` list (src/main/webapp/WEB-INF/web.xml:13-16). No `<servlet>` mappings; servlets use `@WebServlet` annotations (CLAUDE.md:34, src/main/java… ).

### `WEB-INF/css/`

1,826 files. Contains `bootstrap.css`, `bootstrap.bundle.min.js` (and source maps), `style.css`, `toDoListScripts.js`, plus an `img1/` subfolder with 1,820 files (1,814 SVGs + `fonts/bootstrap-icons.woff` + `bootstrap-icons.woff2` + an `index.html`).

### `WEB-INF/view/` (JSP root) — 274 JSPs

JSPs by directory (top counts):

| Directory | JSPs |
|---|---|
| `WEB-INF/view/sales/` | 22 |
| `WEB-INF/view/a/general/` | 17 (excludes nested subdirs) |
| `WEB-INF/view/activity/renew/components/` | 16 |
| `WEB-INF/view/activity/` | 15 |
| `WEB-INF/view/a/activityDetail/columns/detail/` | 12 |
| `WEB-INF/view/a/checklistDetail/` | 11 |
| `WEB-INF/view/activity/checklist/` | 10 |
| `WEB-INF/view/activity/renew/` | 8 |
| `WEB-INF/view/checklist/sequence/` | 7 |
| `WEB-INF/view/authentication/` | 7 |
| `WEB-INF/view/a/general/universalImport/`, `summitImport/`, `interactiveImport/`, `providerSetup/`, etc. | 3-5 each |

### `WEB-INF/view/` top-level subdirs

- `a/` — 119 JSPs total. Sub-areas: `activityDetail/`, `admin/`, `assistant/`, `checklistDetail/`, `general/`, `navbar/`, `pspHome/`, `renew/`, `setup/`, `superDashboard/`, `taskManager/`, `todo/`, `z_acessory/`.
- `activity/` — `checklist/`, `note/`, `renew/`, `setup/`, `ticket/` (each with `components/` or further sub-dirs). 90+ JSPs.
- `authentication/` — login forms, password reset, custom landing, timeclock.
- `billing/` — 4 JSPs: `billingHome25.jsp`, `billingGrid25.jsp`, `erBilling25.jsp`, `sendBillingForm.jsp`.
- `bpo/` — 3 JSPs (`bpoHome25.jsp`, `bpoSettingsMod25.jsp`, `pspClients25.jsp`).
- `checklist/` — `components/`, `sequence/` (7 JSPs), `task/` (4 JSPs).
- `general/` — `admin/adminMenuOC.jsp`, `email/lists/` (2 JSPs), `utility/master/css-js.jsp`, plus `ddUserList.jsp`, `videoExpired.jsp`, `videoPlayer.jsp`.
- `market/` — `landing.jsp`, `requestQuote25.jsp`.
- `ndt/` — `ndtTestDashboard.jsp`.
- `questionnaire/` — `fillQuestionnaire.jsp`, `fillQuestionnaire_ndt_125.jsp`, `questionnaireConfirmation.jsp`.
- `sales/` — 22 JSPs: agency manager, agent home, application/proposal builders, library, rate manager, etc.
- `user/` — `outlookLinkManager.jsp`, `pspBranding25.jsp`.
- `weblink/` — empty `linkfiles/` subdir.

### Outlook add-in (`src/main/webapp/outlook/`)

- `manifest.xml` (Outlook add-in manifest)
- `taskpane.html`
- `icon-16.png`, `icon-32.png`, `icon-80.png`
- `README.md`

---

## 5. Configuration files

`*.properties`, `*.xml`, `*.yml`, `*.yaml`, `*.conf` excluding build/target/idea:

| Path | Appears to configure |
|---|---|
| `.github/workflows/build.yml` | GitHub Actions CI: pushes to `main`/`beta`/`dev` build with JDK 21 + Maven `-Pserver` (.github/workflows/build.yml:1-29) |
| `.mvn/wrapper/maven-wrapper.properties` | Maven wrapper version pin |
| `pom.xml` | See section 2 |
| `src/main/resources/build.properties` | Filtered with `${project.version}` and `${maven.build.timestamp}` (src/main/resources/build.properties:1-2) |
| `src/main/resources/META-INF/persistence-local.xml` | EclipseLink JPA, RESOURCE_LOCAL, MySQL JDBC URL `jdbc:mysql://127.0.0.1:3306/beta_ssa`, `eclipselink.weaving=false` (src/main/resources/META-INF/persistence-local.xml:1-21) |
| `src/main/resources/META-INF/persistence-local-dev.xml` | (sister persistence file for `local-dev` profile) |
| `src/main/resources/META-INF/persistence-server.xml` | EclipseLink JPA, JNDI lookup `java:comp/env/jdbc/ssa` (src/main/resources/META-INF/persistence-server.xml:1-16) |
| `src/main/webapp/WEB-INF/web.xml` | Servlet 5.0 declaration; only `context-param` entries |
| `src/main/webapp/browserconfig.xml` | Windows tile icon config |
| `src/main/webapp/outlook/manifest.xml` | Outlook web add-in manifest |

### JSON resources (`src/main/resources/`)

`knowledge/` (8 files):
- `automation-email-builder.json`
- `backup_recovery_indexed.json`
- `business_continuity_indexed.json`
- `knowledge-config.json`
- `proposal-page-builder.json`
- `summit_guide_indexed.json`
- `summit_videos.json`
- `wave_help_indexed.json`

`packages/` (8 files):
- `billing_payments.json`, `cobra.json`, `general.json`, `hra.json`, `hsa.json`, `package-index.json`, `s125_fsa.json`, `transit_parking.json`

`questionnaire/`:
- `questionnaire_seeds.json`

---

## 6. Scripts

| Path | Appears to do |
|---|---|
| `demo/scripts/domain-guard.sh` | Demo helper (bash). |
| `demo/scripts/demo-zero-to-renewals.md` | Markdown demo walkthrough (script-adjacent doc). |
| `docs/scripts/backup.sh` | Production backup script (bash). |
| `docs/scripts/healthcheck.sh` | Health-check script. |
| `docs/scripts/update.sh` | Production update/deploy script. |
| `scripts/generate-outlook-icons.ps1` | PowerShell — generates Outlook add-in PNG icons. |

No `*.bat`, `*.py`, or other scripts found.

---

## 7. Database artifacts

### Baselines / dumps (`docs/importscript/`)

- `beta_ssa_baseline_v031.sql` (single-commit, 2026-03-03)
- `beta_ssa_dev_baseline_thru_V024.sql` (single-commit, 2026-02-27)

### Versioned migrations (`docs/migrations/`)

V025 through V062 (38 numbered files), plus `seed_ndt125_questionnaire.sql`:

```
V025__plantype_import_columns.sql
V026__benefit_surrogate_pk.sql
V027__bpo_registration_task_source.sql
V028__benefit_plan_year_columns.sql
V029__user_is_active.sql
V030__bpo_cross_system_foundation.sql
V031__todo_note_cross_system_nullable.sql
V032__approved_vendors_registry.sql
V033__todo_note_attachments.sql
V034__starter_package_template_key.sql
V035__feature_headline_description.sql
V036__proposal_section_table.sql
V037__proposal_section_scoping.sql
V038__delegated_todo_sort_order.sql
V039__questionnaire_system.sql
V040__delegated_todo_recurring_series.sql
V041__application_reviewer_fields.sql
V042__delegated_todo_pending_status.sql
V043__custom_landing_page.sql
V044__proposal_section_agency_scoping.sql
V045__application_selected_services.sql
V046__chatbot_skill_table.sql
V047__composite_task_order.sql
V048__universal_import_system.sql
V049__delegated_todo_source_task_id.sql
V050__bpo_default_assignee.sql
V051__import_id_mapping.sql
V052__import_run_log_xref_tracking.sql
V053__interactive_import_enhancements.sql
V054__managed_installation.sql
V055__schema_info_view.sql
V056__training_video_tokens.sql
V057__agency_suppressed.sql
V058__questionnaire_renderer.sql
V059__ndt_census_tables.sql
V060__outlook_user_link.sql
V061__todo_ownership_override.sql
V062__note_agent_visibility.sql
seed_ndt125_questionnaire.sql
```

### Update bundles (`docs/updates/`)

- `update_V039_to_V057.sql`

### Top-level

- `docs/schema_version_migration.sql` — `schema_version` table DDL + version inserts (48 commits — actively touched).

---

## 8. `.claude/` contents

| Path | Type | Notes |
|---|---|---|
| `.claude/inventory/` | directory | This pass output. |
| `.claude/launch.json` | JSON | Single launch config: docs preview Python http.server on port 8123 (.claude/launch.json:1-12). |
| `.claude/settings.local.json` | JSON | Permissions allow-list (very large — 56,876 bytes). |
| `.claude/skills/proposal-content-page/SKILL.md` | Markdown | 275 lines, proposal-builder HTML block generator. |
| `.claude/worktrees/vigilant-jennings/` | directory | Empty (only `.` and `..` present at survey time). |

`.claude/` is listed in `.gitignore` (.gitignore:5), so most files in this folder are not tracked by git.

---

## 9. `docs/` contents

### Top-level files

| Path | Bytes |
|---|---|
| `docs/ams_to_be_vision.md` | 27,342 |
| `docs/claude_memory.md` | 7,228 |
| `docs/custom-landing-page-prompt.md` | 11,030 |
| `docs/deployment_backlog.md` | 50,380 |
| `docs/deployment_runbook.md` | 14,668 |
| `docs/deployment_strategy.md` | 19,763 |
| `docs/outlook-addin-handoff.md` | 6,757 |
| `docs/preview-pages.html` | 1,884 |
| `docs/sample-landing-content.html` | 22,712 |
| `docs/schema_version_migration.sql` | 7,588 |
| `docs/serviceitem_unification_design_v2.md` | 17,661 |
| `docs/tomcat_ssl_setup.md` | 6,112 |

### Subdirectories

- `docs/analysis/` — 17 markdown files (architecture, transition plans, dead-JSP investigations, BPO brainstorm, NDT design, sales pipeline reference, session history archive, etc.). See AMS-DOCS-INDEX.md for per-file summaries.
- `docs/importscript/` — 2 SQL baseline dumps + 3 NDT-125 design markdowns.
- `docs/migrations/` — 38 versioned `V025…V062` SQL files + `seed_ndt125_questionnaire.sql`.
- `docs/mockups/` — 7 HTML mockups (BPO panels, agent home, agent setup detail, add-note redesign, agent delegation paths, center-panel alternatives).
- `docs/proposal_html/` — 8 sample proposal HTML pages (FSA, COBRA, HRA, HSA, LSA themes).
- `docs/scripts/` — `backup.sh`, `healthcheck.sh`, `update.sh`.
- `docs/updates/` — `update_V039_to_V057.sql`.

---

## 10. Other top-level files

| Path | Notes |
|---|---|
| `ams-src.7z` | 9.6 MB compressed archive (single commit, untouched since 2025-09-19). |
| `ams.iml` | IntelliJ module descriptor. |
| `mvnw`, `mvnw.cmd` | Maven wrapper launchers. |
| `tools/tomcat-jakartee-migration.jar` | Standalone migration utility jar. |
| `src-directory.txt` | UTF-16 text dump of an older flat `controller/` listing — does not match the current package layout. (src-directory.txt:1-15) |
| `plan.md` | Single-feature plan ("J5 Benefit Year Import + Renewal Date Audit"); single commit on 2026-02-28. (plan.md:1-141) |

---

## 11. Demo materials (`demo/`)

Per `demo/README.md`: "None of these files are loaded by application code — they are for manual upload testing only."

| Path | Contents |
|---|---|
| `demo/README.md` | Index of demo materials |
| `demo/beneliance/` | `ben_small.png`, `beneliance_landing.html` |
| `demo/brochures/` | COBRA, FSA, login brochures (HTML + PDF) |
| `demo/interactive-import/suite/` | `benefits.csv`, `employees.csv`, `employers.csv`, `plan_types_suite.csv` |
| `demo/interactive-import/wex/` | `benefits.csv`, `employees.csv`, `employers.csv`, `plan_types_wex.csv` |
| `demo/scripts/` | `demo-zero-to-renewals.md`, `domain-guard.sh` |
| `demo/summit-import/` | J1/J2/J3/J4/J5/J7 CSVs + `Summit - DataPath.xlsx` |

---

## 12. CI / GitHub

`.github/workflows/build.yml` (.github/workflows/build.yml:1-29):
- Triggers: push to `main`, `beta`, `dev`; manual `workflow_dispatch`.
- JDK: temurin **21** (.github/workflows/build.yml:18-19).
- Build command: `mvn -B -Pserver -DskipTests=false clean package`.
- Uploads `target/*.war` artifact named `ams-war`.

(Note: pom declares `<maven.compiler.release>17</maven.compiler.release>` while CI uses JDK 21 — see AMS-OPEN-QUESTIONS.md.)
