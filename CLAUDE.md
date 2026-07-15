# SSA Activity Management System (AMS)

## What AMS is

AMS is a Java/Jakarta EE web application owned by Superior State Administration (SSA). It is a benefits-administration companion platform intended to be used alongside the third-party Datapath/Summit benefits platform by entities called PSPs (Plan Service Providers). It supports activity management, sales pipeline (LOS → Module → Rate → Agency → Prospect → Proposal → Application → Setup), monthly billing, time tracking, NDT compliance, questionnaires, an Outlook web add-in, and a federated BPO task-outsourcing model. The codebase has 201 `@WebServlet`s and 142 `@Entity` classes; deployment target is `superiorstate.biz` on IONOS Cloud Tomcat 10. See `.claude/inventory/AMS-DOMAIN-KNOWLEDGE.md` for the full domain map.

## Stack and runtime

- **Java:** `pom.xml` sets `<maven.compiler.release>17</maven.compiler.release>`; CI (`.github/workflows/build.yml`) also runs `actions/setup-java@v4` with `java-version: '17'`. (The earlier 17-vs-21 divergence was reconciled — resolved Open Question #1.)
- **Build:** Maven, WAR packaging, final WAR named `ROOT.war`. Profiles:
  - `server` (default) — JNDI datasource `java:comp/env/jdbc/ssa`, `persistence-server.xml`
  - `local` — direct JDBC to `127.0.0.1:3306/beta_ssa`, `persistence-local.xml`
  - `local-dev` — variant for local dev
  - The active profile drives `maven-antrun` to copy the matching `persistence-*.xml` into place.
- **Maven wrapper only** — there is no system `mvn` on the dev workstation. Use `./mvnw` (PowerShell: `.\mvnw.cmd`).
- **Servlet container:** Tomcat 10 (Jakarta EE namespace; `jakarta.servlet` 5.0).
- **Persistence:** MySQL 8 schema `beta_ssa`, EclipseLink JPA 3.0.2, persistence unit `ssaPU`.
- **Logging:** Log4j 2.20.0 + SLF4J→Log4j bridge, configured at `src/main/resources/log4j2.xml` (console `catalina.out` + rolling `${catalina.base}/logs/ams.log`, 14-day retention; `net.superiorstate.ams` → DEBUG, noisy libs → WARN). New code uses `LoggerFactory.getLogger(...)`; legacy `System.out.println` calls remain and are migrated organically when files are touched. (Resolved Open Question #20.)
- **Other notable libs:** Apache POI 5.2.3, opencsv 5.9, PDFBox 3.0.4, Eclipse Angus mail 2.0.3, AWS SDK v2 (Wasabi/S3), jsoup 1.17.2, Anthropic Claude API client (custom). (MSAL4j and Microsoft Graph were **removed** from `pom.xml` — Outlook integration is a Web Add-in calling AMS REST endpoints, not a server-side Graph SDK; resolved Open Question #3.)

## Repository layout

```
ams/
├── src/main/java/net/superiorstate/ams/
│   ├── EmfListener.java, LoginFilter.java   (package-root web infra)
│   ├── controller/   (organized by feature: activity, admin, api, assistant,
│   │                   authentication, checklist, data, email, home, market,
│   │                   monthly, sequence, user)
│   ├── data/         (dao/, resolver/, service/, util/, plus AmsDataGlobal
│   │                  and AmsDataLocal at the package root)
│   ├── filter/       (ApiTokenFilter)
│   └── model/        (activity/, billing/, general/, imports/, sales/,
│                       summit/, upload/ — all JPA entities and DTOs)
├── src/main/resources/META-INF/
│   ├── persistence-local.xml   (RESOURCE_LOCAL, dev creds — see warnings)
│   └── persistence-server.xml  (JTA, JNDI lookup)
├── src/main/webapp/            (JSPs, static assets, CKEditor 5 via CDN,
│                                outlook/, WEB-INF/web.xml, WEB-INF/tags/)
├── docs/                       (design docs and references — see AMS-DOCS-INDEX.md)
├── docs/migrations/            (V025-V0XX schema migrations)
├── docs/analysis/              (migration_tracker.md, project_backlog.md, etc.)
├── demo/                       (CSV/HTML test data — not loaded by Java)
├── .claude/                    (Claude Code assets — see "Where deeper context lives")
├── .github/workflows/          (build.yml — CI)
└── pom.xml
```

## Current development context

- **Trunk:** `refactor/modernize-architecture` — the working mainline **and the GitHub default branch**. Most work is committed **directly** here. (`main` is retired: fully merged, ~345 commits behind, kept only for history.)
- **Feature branches are situational** — spun off only when a safe fallback point is needed (e.g., risky work tested on production before a demo), then folded back to trunk. **No feature branch is currently in flight** — the agency access-scope / IDOR-hardening work (`AgencyScopeResolver`) merged to trunk 2026-07-15 (`e0a62d1`) and its branch was deleted. Always verify live branches with `git branch -a`.
- **Latest migration in tree:** **V071** (`docs/migrations/V071__agency_quote_token.sql`). Always re-check `ls docs/migrations/` directly — versions move quickly. The authoritative tracker is `docs/analysis/migration_tracker.md`.
- **Per-installation migration state** (Production / Master / Demo / BPO) is tracked in `MEMORY.md`, not here.

## How to run things locally

- **Compile:** `./mvnw compile` (PowerShell: `.\mvnw.cmd compile`)
- **Build WAR:** `./mvnw -P local clean package` for local dev, or `./mvnw -P server clean package` for deployment artifacts.
- **Local DB:** MySQL on `127.0.0.1:3306`, schema `beta_ssa`. Credentials live in `src/main/resources/META-INF/persistence-local.xml` (literal credentials in a tracked file — Open Question #15).
- **Deploy locally:** typically run via IntelliJ Tomcat run-config; relaunch options documented in `MEMORY.md` ("IntelliJ Relaunch Options").

### Migration discipline

Schema changes follow versioned migration conventions:

1. New file: `docs/migrations/V{NNN}__{description}.sql`
2. Each script self-registers in `schema_version` (see existing scripts for the `INSERT IGNORE` pattern)
3. Update `docs/analysis/migration_tracker.md`
4. Update `docs/schema_version_migration.sql`

No ad-hoc DDL — every schema change must be a versioned script. See an existing script under `docs/migrations/` as a template.
- `docs/importscript/` — baseline DDL snapshots (`beta_ssa_dev_baseline_thru_V024.sql`, `beta_ssa_baseline_v031.sql`); used to reset a dev database to a known version.

## Production environment (summary)

- **VPS:** IONOS Cloud, Ubuntu 24.04, four hosts: `ssa-production`, `ssa-demo`, `ssa-bpo`, `ssa-master`.
- **SSL:** Let's Encrypt via nginx reverse proxy (migrated from Comodo wildcard 2026-03).
- **Tomcat config:** `/var/lib/tomcat10/conf/ssa.properties`, WAR at `/var/lib/tomcat10/webapps/ROOT.war`, logs at `/var/lib/tomcat10/logs/catalina.out`.
- **MySQL on production** requires `LD_LIBRARY_PATH=/usr/lib/x86_64-linux-gnu` workaround (Acronis library conflict).
- Full procedures: `docs/deployment_runbook.md`, `docs/deployment_strategy.md`.

## Where deeper context lives

- **`MEMORY.md`** — operational gotchas (EclipseLink L2 cache, nested JOIN FETCH, EM-open-during-forward), role IDs, key entity quirks, recent session log.
- **`.claude/inventory/`** — the full audit pass:
  - `AMS-INVENTORY.md` — factual file/package map
  - `AMS-DOMAIN-KNOWLEDGE.md` — business-domain map
  - `AMS-TECHNICAL-ARCHITECTURE.md` — stack and architecture
  - `AMS-DOCS-INDEX.md` — every `docs/` file tagged current/historical/superseded
  - `AMS-OPEN-QUESTIONS.md` — 30 unresolved items (referenced below as "Open Question #N")
  - `AMS-CLAUDE-ASSETS.md` — `.claude/` contents
- **`docs/analysis/migration_tracker.md`** — authoritative migration version log
- **`docs/analysis/session_history_archive.md`** — long-form session history
- **`docs/analysis/project_backlog.md`** — feature priorities
- **`docs/deployment_backlog.md`** — deployment-side work items (D-NN)
- **`docs/ams_to_be_vision.md`** — sales-portal pipeline narrative
- **`.claude/skills/proposal-content-page/SKILL.md`** — repo-local skill for generating proposal HTML blocks

## Developer preferences

- **One step at a time** — confirm completion before the next.
- **Produce full downloadable files** — not partial snippets. Include full file path.
- **IntelliJ terminal is PowerShell** — use PowerShell syntax for local commands.
- **No unsolicited refactoring or style criticism.**
- **Check existing files first** before asking the developer to paste code.
- End every response with: **Next action: ...**

## Known unknowns

Numbers reference `.claude/inventory/AMS-OPEN-QUESTIONS.md` — all 30 Pass-1 items are now **resolved** there; consult it for the settled answers rather than re-investigating.

Two things genuinely keep moving — always re-check live state:

1. **Migration versions move fast** — `ls docs/migrations/` is the source of truth, not this file or `MEMORY.md`. Latest as of this writing is V071; expect drift. (Open Question #2)
2. **Branch claims drift** — verify any "current feature branch" against `git branch -a` before relying on it. Working line is `refactor/modernize-architecture` → feature branches; `main` is far behind. (Open Question #19)

Settled since Pass 1 (don't re-litigate): Java is **17** across pom + CI (#1); `log4j2.xml` **exists** and is configured (#5/#20); MSAL/Graph were **removed** from pom — Outlook is a Web Add-in (#3/#7); `/tpo` is **intentional** legacy-URL support (#4/#17); Summit and Universal import subsystems **converge** rather than one replacing the other (#6/#18).

## What NOT to assume

This file is a **snapshot**. Migration versions advance, branches come and go, dependencies get added and removed, and `MEMORY.md` is updated more often than this file. For anything time-sensitive:

- **Migration version** → `ls docs/migrations/`
- **Active branches** → `git branch -a`
- **Current PSP/BPO/Master/Demo schema state** → `MEMORY.md`
- **Recent work / sessions** → `MEMORY.md` "Recent Sessions" or `docs/analysis/session_history_archive.md`
- **Build commands actually used** → check `pom.xml` profiles and `.github/workflows/build.yml`

When this file disagrees with the inventory, `MEMORY.md`, or live git/filesystem state, trust the live source over this file.

## Cross-cutting knowledge

Shared preferences, lessons, and conventions are in `.claude/toolkit/` (git submodule from kevin-claude-toolkit). Read those files for context on working preferences, build hygiene, EclipseLink gotchas, secret rotation procedures, and git workflow conventions.
