# SSA Activity Management System (AMS)

## Project Overview
Benefits administration web application for Superior State Administration (SSA). Java enterprise app serving multiple user roles (PSP admins, agents, agency managers, BPO users) with activity management, sales pipelines, billing, timekeeping, and administrative tools.

## Tech Stack
- **Java 17**, Jakarta EE, EclipseLink JPA (persistence unit: `ssaPU`)
- **MySQL 8.0** (schema: `beta_ssa`)
- **Tomcat 10**, Maven WAR packaging
- **JSP** with Bootstrap 5, modern JavaScript, DM Sans font
- **Design colors:** Primary `#0d5681`, Secondary `#87a948`
- Deployed to `superiorstate.biz`
- Development in IntelliJ IDEA (PowerShell terminal)

## Architecture

```
src/main/java/net/superiorstate/ams/
├── controller/              ← All servlets (organized by feature)
│   ├── activity/            ← Activity CRUD + subpackages (contact, renewal, setup, ticket)
│   ├── authentication/      ← Login, user auth
│   ├── checklist/           ← Checklist management
│   ├── data/                ← Import/export servlets
│   ├── email/               ← Email workflow
│   ├── monthly/             ← Monthly billing
│   ├── sequence/            ← Sequence builders
│   └── user/                ← User management
├── data/
│   ├── dao/                 ← Database query classes (15 DAOs)
│   ├── resolver/            ← Entity lookups, person resolution
│   ├── service/             ← Business logic (billing, imports, sync)
│   └── util/                ← Validators, helpers, constants
│   ├── AmsDataGlobal.java   ← Application-scoped state (singleton)
│   ├── AmsDataLocal.java    ← Session-scoped state
│   └── ActivityFilter.java
├── filter/                  ← LoginFilter
└── model/                   ← All JPA entities and DTOs
    ├── activity/            ← Activity, CheckList, Renewal, Ticket
    ├── billing/             ← Billing entities
    ├── general/             ← Person, User, PSP, Address
    ├── sales/               ← Agency, Proposal, Application
    ├── summit/              ← Employee, Employer, Benefit (archive, imports, temp)
    └── (root)               ← Activity25, Constant, and other view-backed DTOs
```

## Key Patterns
- **Servlets use `@WebServlet` annotations** — no web.xml mappings
- **"25" suffix** = current/modern version of a servlet or JSP
- **`AmsDataGlobal`** = application-scoped singleton (employer lists, activity caches, constants)
- **`AmsDataLocal`** = session-scoped state (current user, current activity, filters, checklists)
- **Activity inheritance** = `Assignee` → `Activity` → `Ticket`/`Renewal`/`Setup`/`CheckList`
- **View-backed entities** (e.g., `Activity25`, `Checklist25`, `EmployeeV`, `PersonV`) = read-only, mapped to DB views
- **Import staging tables** = `summit/imports/order/` entities, populated from CSV/Excel uploads

## Database Migration Rules — MANDATORY
All schema changes MUST follow versioned migration conventions:

1. **Every schema change gets a versioned script:** `V{NNN}__{description}.sql`
2. Check `docs/analysis/migration_tracker.md` for the current highest version (currently **V024**)
3. **Scripts self-register:** Every migration must include:
   ```sql
   INSERT IGNORE INTO schema_version (version, description, script_name, applied_on)
   VALUES ('V025', 'Description here', 'V025__description.sql', NOW());
   ```
4. **Update the tracker:** Produce updated `docs/analysis/migration_tracker.md` with the new row
5. **Update schema_version_migration.sql:** Add the new version to `docs/schema_version_migration.sql`
6. **No ad-hoc DDL** — never suggest raw ALTER/CREATE for manual execution. Always wrap in a versioned script.
7. **Seed data changes** affecting `DatabaseInitializer` → note in `docs/deployment_backlog.md`

## Current Branch
Active work: `refactor/modernize-architecture`

## Production Environment
- **VPS:** IONOS Cloud, Ubuntu 24.04
- **SSH:** `kevinmurphy@superiorstate.biz`
- **MySQL on production requires:**
  ```bash
  LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu mysql --socket=/var/run/mysqld/mysqld.sock -u root -p
  ```
  (Acronis library conflict workaround)
- **Config:** `/var/lib/tomcat10/conf/ssa.properties`
- **WAR:** `/var/lib/tomcat10/webapps/ROOT.war`
- **Logs:** `/var/lib/tomcat10/logs/catalina.out`

## Developer Preferences
- **One step at a time** — don't list multiple steps; confirm completion before the next
- **Produce full downloadable files** — not partial snippets. Include full file path.
- **IntelliJ terminal is PowerShell** — use PowerShell syntax for local commands
- **No unsolicited refactoring** or style criticism — focus on getting things done
- **Check existing files first** before asking the developer to paste code
- End every response with: **Next action: ...**

## Reference Documentation
- `docs/analysis/migration_tracker.md` — DB version tracking
- `docs/analysis/session_history_archive.md` — build session history
- `docs/analysis/activity_detail_transition_plan.md` — activity detail page plan
- `docs/deployment_strategy.md` — multi-PSP deployment architecture
- `docs/deployment_runbook.md` — step-by-step deployment procedures
- `docs/deployment_backlog.md` — tracked work items
- `docs/schema_version_migration.sql` — schema_version table + all version inserts
