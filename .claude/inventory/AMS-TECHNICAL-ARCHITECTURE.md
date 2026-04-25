# AMS Technical Architecture — Pass 1

A factual snapshot of the stack and structure as observed in the working tree. No interpretation of "should be." Citations point to evidence in the code.

---

## 1. Build system

- **Tool:** Maven, single module, WAR packaging (pom.xml:5-12).
- **Wrapper:** `mvnw` / `mvnw.cmd` shipped in repo with `.mvn/wrapper/maven-wrapper.properties`.
- **Java release:** `<maven.compiler.release>17</maven.compiler.release>` (pom.xml:16).
- **CI compile JDK:** Temurin **21** in GitHub Actions (.github/workflows/build.yml:18-19).
- **Profiles:** three profiles each copy a different `persistence-*.xml` into `META-INF/persistence.xml` via `maven-antrun-plugin`:
  - `server` (default, JNDI-based) — pom.xml:310-337
  - `local` (direct JDBC) — pom.xml:340-367
  - `local-dev` — pom.xml:368-394
- **Enforcer rules:** ban a checked-in `persistence.xml`, ban legacy `javax.mail`/activation jars (Angus Mail only), require upper-bound dep resolution (pom.xml:238-303).
- **WAR plugin:** `maven-war-plugin:3.3.2` with `failOnMissingWebXml=false` (pom.xml:227-236).
- **Build output:** WAR under `target/`. CI uploads `target/*.war` as artifact `ams-war` (.github/workflows/build.yml:25-29).

### Resource filtering

`build.properties` is filtered (Maven token replacement on `${project.version}`, `${maven.build.timestamp}`); other resources are not (pom.xml:200-215, src/main/resources/build.properties:1-2).

---

## 2. Runtime stack

| Layer | Choice | Evidence |
|---|---|---|
| Language | Java 17 (CI compiles on JDK 21) | pom.xml:16, .github/workflows/build.yml:18-19 |
| Servlet API | Jakarta Servlet 5.0 (`provided`) | pom.xml:36-40 |
| JSP | Jakarta JSP 3.0 (`provided`) + JSTL 2.0 | pom.xml:41-53 |
| Container | Tomcat 10 | CLAUDE.md:8; persistence-server.xml uses Tomcat-style `java:comp/env/jdbc/ssa` (src/main/resources/META-INF/persistence-server.xml:6) |
| Persistence | EclipseLink JPA 3.0.2 | pom.xml:109-113 |
| Database | MySQL 8 (driver `com.mysql:mysql-connector-j:8.4.0`) — schema `beta_ssa` | pom.xml:92-96, src/main/resources/META-INF/persistence-local.xml:9 |
| Logging | Log4j 2.20.0 + SLF4J→Log4j2 bridge | pom.xml:56-70 |
| Mail | Eclipse Angus jakarta.mail 2.0.3 | pom.xml:117-120 |
| HTML cleaner | jsoup 1.17.2 | pom.xml:148-152 |
| Excel/CSV | Apache POI 5.2.3, opencsv 5.9 | pom.xml:73-89 |
| Object storage | AWS SDK v2 (`s3`, `apache-client`), BOM 2.20.69 — used as Wasabi-compatible client | pom.xml:99-106; src/main/java/net/superiorstate/ams/data/dao/StorageDAO.java:1-30 |
| PDF text extraction | Apache PDFBox 3.0.4 (declared "for ACH reports") | pom.xml:191-196 |
| JSON | Gson 2.11.0 (declared "for Claude API") | pom.xml:184-189 |
| Microsoft auth/graph | `msal4j:1.21.0`, `microsoft-graph:5.52.0` declared in pom (no `import` of these classes was found in `src/main/java`) | pom.xml:128-138 |
| Tests | JUnit Jupiter 5.8.2 | pom.xml:154-165 |
| Front end | Bootstrap 5 (CSS shipped at `src/main/webapp/WEB-INF/css/bootstrap.css`); CKEditor 4 (`src/main/webapp/ckeditor/`) and CKEditor 5 (`src/main/webapp/resources/ckeditor5/`) both present; bootstrap-icons SVG set under `src/main/webapp/WEB-INF/css/img1/` (1,814 SVGs). | (file tree) |

---

## 3. Application architecture

Layering observed via package structure (`src/main/java/net/superiorstate/ams/`):

```
controller/    (servlets — @WebServlet annotations)
data/
  dao/         (database query classes — JPQL / Criteria queries)
  resolver/    (entity-lookup / cross-reference helpers)
  service/     (business-logic services: billing, imports, sync, AI)
  template/    (empty)
  util/        (helpers, validators, constants)
  AmsDataGlobal.java    (application-scoped state)
  AmsDataLocal.java     (session-scoped state)
  ActivityFilter.java
filter/        (1 file: ApiTokenFilter)
model/         (JPA entities + DTOs grouped by domain)
service/       (1 file: InstallationHealthScheduler)
LoginFilter.java       (top-level @WebFilter("/*"))
EmfListener.java       (top-level @WebListener)
AppConfig.java
Main.java
NewInstall.java
```

### Servlet count
196 files contain `@WebServlet` (grep across `src/main/java`).

### Entity count
138 files contain `@Entity` (grep across `src/main/java`).

### Examples by layer

- **Controllers (servlets):**
  - `controller/activity/setup/ProposalBuilder.java`, `controller/checklist/AddToDo25.java`, `controller/email/SendAuto25.java`, `controller/api/outlook/OutlookCreateTicketApi.java`.
  - 13 sub-packages by feature: `activity/`, `admin/`, `api/`, `assistant/`, `authentication/`, `checklist/`, `data/`, `email/`, `home/`, `market/`, `monthly/`, `sequence/`, `user/`.
- **DAOs (`data/dao/`, 23 files):** `ActivityDAO`, `ActivityListDAO`, `AppConstantDAO`, `AuthDAO`, `BillingQueryDAO`, `ChatbotSkillDAO`, `ChecklistDAO`, `CompositeOrderDAO`, `EmailDAO`, `NdtTestRunDAO`, `PersonDAO`, `RecurringChecklistDAO`, `RenewalQueryDAO`, `RequiredTaskDAO`, `SalesDAO`, `SequenceDAO`, `StorageDAO` (S3/Wasabi), `TaskDAO`, `TicketKnowledgeDAO`, `TicketQueryDAO`, `TimeTrackingDAO`, `ApplicationTaskDAO`, `ActivityLandingDao`.
- **Services (`data/service/`, 25 files):** `Biller`, `MonthlyBiller`, `BpoTaskPushService`, `Cleaner`, `ClaudeApiService`, `DatabaseInitializer`, `DatabaseResetUtil`, `DemoDataSeeder`, `Importer`, `ImportCommitService`, `ImportResolutionService`, `InteractiveImportSession`, `KnowledgeSearchService`, `PackageLoader`, `QuestionnaireLoader`, `QuestionnaireService`, `ReferenceDataSeeder`, `RenewalService`, `SetupPromotionService`, `SummitImportService`, `SummitProviderSeeder`, `SummitSync`, `UniversalImportService`, `Updater`, `VendorRegistryService`.
- **Resolvers (`data/resolver/`, 8 files):** `AgentSetupSnapshotLoader`, `EntityFactory`, `EntityLookup`, `ImportIdResolver`, `NoteVisibilityResolver`, `OriginatingAgencyResolver`, `PersonResolutionService`, `PersonResolver`.
- **Util (`data/util/`, 14 files):** `ActivitySessionGuard`, `ActivityViewHelper`, `ApiClient`, `AutoSafe`, `AutomationHelper`, `BillingHelper`, `DocumentConstants`, `EmailTemplate`, `HsaBillingHelper`, `HtmlHelper`, `PathUtil`, `SessionVar`, `TicketHelper`, `Validator`.
- **Top-level (`net.superiorstate.ams`):** `AppConfig`, `EmfListener`, `LoginFilter`, `Main`, `NewInstall`.

### Mapping to filesystem layering

CLAUDE.md describes the layering at CLAUDE.md:18-50; the present working tree matches that description package-for-package.

---

## 4. Persistence

### EntityManagerFactory lifecycle

`EmfListener` is `@WebListener` and is the application's startup hook (src/main/java/net/superiorstate/ams/EmfListener.java:22-32).
- On `contextInitialized`, calls `AppConfig.load()`, then creates the EMF: `Persistence.createEntityManagerFactory("ssaPU")` (EmfListener.java:40).
- Stores the EMF in `ServletContext` under attribute `emf` (EmfListener.java:41).
- Does a "DB initialized?" probe by querying `Constant` for `name='SSL_PORT'` value `'443'`; if not initialized, **skips global data load** and prints a 🔁 warning (EmfListener.java:47-59).
- If initialized, instantiates `AmsDataGlobal`, calls `initializeGlobalData(em)`, stores under attribute `global` (EmfListener.java:61-72).
- Refreshes per-context attributes `systemType`, `isBpoSystem`, `isPspSystem`, `isMasterSystem` (EmfListener.java:67-70).
- On master systems, starts `InstallationHealthScheduler` (EmfListener.java:74-78).
- On `contextDestroyed`, stops the scheduler and closes the EMF (EmfListener.java:120-132).

### Persistence units

Unit name **`ssaPU`** (used by all three profile XMLs):

- `persistence-local.xml` — `RESOURCE_LOCAL`, JDBC URL `jdbc:mysql://127.0.0.1:3306/beta_ssa?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`, user `root`, password literal `Passw0rd!` (src/main/resources/META-INF/persistence-local.xml:1-21).
- `persistence-server.xml` — `RESOURCE_LOCAL` against JNDI `<non-jta-data-source>java:comp/env/jdbc/ssa</non-jta-data-source>` (src/main/resources/META-INF/persistence-server.xml:6).
- `eclipselink.weaving=false` set in both (consistent across files).

### Inheritance strategy

Single inheritance hierarchy rooted at `Assignee` with `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` (src/main/java/net/superiorstate/ams/model/general/Assignee.java:7-9).

Subclasses observed:
- `Activity extends Assignee` (model/activity/Activity.java)
- `Person extends Assignee implements Comparable<Person>` (model/general/Person.java)
- `PSP extends Assignee` (model/general/PSP.java)
- `Recipient extends Assignee` (model/general/Recipient.java)
- `Activity` is then further extended by `CheckList`, `Opportunity`, `Renewal`, `Setup`, `Ticket` (all under `model/activity/…`).

Per `CLAUDE.md:32`: chain documented as `Assignee → Activity → Ticket/Renewal/Setup/CheckList`. The grep confirmed `Opportunity` is also a subclass of `Activity`.

### View-backed entities

Per CLAUDE.md:34, "view-backed entities (e.g., `Activity25`, `Checklist25`, `EmployeeV`, `PersonV`) = read-only, mapped to DB views." Located:
- `model/Activity25.java`, `Activity25p.java`, `Activity25u.java`, `Checklist25.java`, `Checklist25u.java` (root model package, 12 files).
- `model/general/PersonV.java`
- `model/summit/archive/EmployeeV.java`

### Auxiliary state objects

- `data/AmsDataGlobal.java` — application-scoped state singleton; loaded by `EmfListener` and stored as `ServletContext` attribute `global`.
- `data/AmsDataLocal.java` — session-scoped state. Used in session attribute `local` (LoginFilter.java:81).

---

## 5. Web layer

### Servlet declarations

- All servlets use `@WebServlet` annotations — confirmed by `failOnMissingWebXml=false` in pom and the absence of any `<servlet>` mappings in `src/main/webapp/WEB-INF/web.xml` (src/main/webapp/WEB-INF/web.xml:1-19, pom.xml:233).
- 196 servlet files (grep `@WebServlet`).

### `web.xml`

- Servlet 5.0 declaration (`https://jakarta.ee/xml/ns/jakartaee`).
- Two `context-param` entries:
  - `file-upload` — value is a hard-coded Windows path `c:\Users\kevinmurphy.SUPERIORSTATE\IdeaProjects\km_web_100\src\main\webapp\WEB-INF\view\weblink\linkfiles` (src/main/webapp/WEB-INF/web.xml:6-12).
  - `org.apache.jasper.compiler.TldScanner.jarsToSkip` (src/main/webapp/WEB-INF/web.xml:13-16).

### JSP

- 280 JSP files. View root: `src/main/webapp/WEB-INF/view/`.
- Authoring conventions per `CLAUDE.md:24` — files named with a "25" suffix mark the current/modern version of a servlet or JSP.
- A `WEB-INF/view/general/utility/master/css-js.jsp` and a `WEB-INF/view/css-js.jsp` (JSP-include partials) are present alongside per-feature JSPs.

### Filters

Two `@WebFilter` files:
- `LoginFilter.java` (top-level) — `@WebFilter("/*")` (LoginFilter.java:16-17).
  - Hard-coded allow-list of unauth endpoints (login, ResetLogin, AcceptInvite, RequestQuote, etc.) (LoginFilter.java:19-24).
  - Allows `/api/*`, `/proposal/*`, `/apply/*`, `/q/*`, `/saveQuestionnaire`, `/tpo*`, `/uploadRateSheet`, `/saveApplication`, `/video*`, `/outlook/*` (LoginFilter.java:56-58, 87).
  - Static-resource heuristic: matches by extension and known prefixes (LoginFilter.java:96-111).
  - Session attribute `local` of type `AmsDataLocal` exposes `isAuthenticated()` (LoginFilter.java:80-84).
  - Session attribute `uninitialized` toggles the "redirect to `/initialize.jsp` if DB not ready" behaviour (LoginFilter.java:62-76).
- `filter/ApiTokenFilter.java` — `@WebFilter(urlPatterns = "/api/*")` (ApiTokenFilter.java:13).
  - Skips auth for `partnership/request`, `partnership/approve`, `registry/*`, `questionnaire/webhook`, `system/register` (ApiTokenFilter.java:24-47), and Outlook add-in endpoints which use per-user tokens validated via `OutlookApiHelper` (ApiTokenFilter.java:49-50).

### Session handling

- `EmfListener` is also `HttpSessionListener` and `HttpSessionAttributeListener` (EmfListener.java:23).
- `sessionCreated` populates `thisMonth`, `lastMonth`, `twoMonth` date attributes and re-checks the DB-initialized signal (EmfListener.java:135-165).

---

## 6. Security

### Authentication

- Login servlet: `controller/authentication/AuthenticateUser.java`. Reads `userPassword` request param and calls `AuthDAO.validateLogin(em, userName, password)`.
- Password hashing: `AuthDAO.generatePasswordHash` uses `MessageDigest.getInstance("SHA-512")` with a per-user salt loaded from `User.salt`. Hash is stored in `User.passwordHash`. (`src/main/java/net/superiorstate/ams/data/dao/AuthDAO.java`)
  - Single-pass SHA-512 (no PBKDF2/bcrypt/argon2 visible).
- Session-based auth: an `AmsDataLocal` instance is placed in `HttpSession` and exposes `isAuthenticated()` (LoginFilter.java:80-84).
- One-time guid login flow: `OneTimeUserLogin`, `HelpUserLogin` (controller/authentication/).
- API token auth: per-PSP tokens validated by `ApiTokenFilter`; per-user tokens for Outlook taskpane validated via `OutlookApiHelper`.

### Roles

`UserRole` is an entity with `Integer roleId`, `String description`, and a `ManyToMany` back-reference to `User` (src/main/java/net/superiorstate/ams/model/general/UserRole.java:10-22). Per CLAUDE memory `MEMORY.md`:
> 1=PSP User, 2=Agent, 3=Client, 4=Applicant, 5=PSP Admin, 8=Agency Admin, 9=PSP Sales, 102=BPO Admin, 103=BPO User
(That mapping is in user-supplied memory; the codebase itself uses numeric `roleId` values throughout — see CLAUDE.md context block.)

### TLS / SSL

- The application reads `Constant` row `name='SSL_PORT'` (expected value `'443'`) as a "DB initialized" signal (EmfListener.java:50-58).
- The deployment-side TLS is documented as nginx + Let's Encrypt at `docs/tomcat_ssl_setup.md` and is not handled by Java code.

---

## 7. Logging

- Log4j 2 (core + api + slf4j2-impl bridge) declared in pom (pom.xml:56-70).
- No `log4j2.xml` or `log4j2.properties` was found inside the working tree (`find src -name 'log4j*'` empty). Logging config is presumably supplied by the container or by classpath defaults.
- Class-level logger usage observed in `data/dao/StorageDAO.java` (`org.apache.logging.log4j.LogManager.getLogger`).
- `EmfListener` writes startup errors to `{LOG_PATH}/emf_error.log` if EMF creation fails — uses `AppConfig.get("LOG_PATH", System.getProperty("catalina.base", ".") + "/logs")` (EmfListener.java:94-110).
- A great deal of `System.out.println(…)` is used for runtime diagnostics throughout the app (e.g., EmfListener.java:59, 71-72, AppConfig.java:50-52).

---

## 8. Configuration & secrets

- `AppConfig.java` loads `ssa.properties` at startup with this lookup order (AppConfig.java:13-17):
  1. System property `ssa.config` (e.g., `-Dssa.config=/path`)
  2. Fallback: `${catalina.base}/conf/ssa.properties`
- Properties read include `PSP_ID`, `LOG_PATH`, `S3_ENDPOINT`, `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`, `SYSTEM_TYPE` (system-type detection: PSP/BPO/Master).
- Production location per CLAUDE.md:91: `/var/lib/tomcat10/conf/ssa.properties`.
- Hard-coded JDBC password `Passw0rd!` is committed in `persistence-local.xml` (src/main/resources/META-INF/persistence-local.xml:11) — applies only to the local Maven profile.

---

## 9. Deployment hints (visible in repo)

- **Container target:** Tomcat 10 (CLAUDE.md:8). Production WAR path per CLAUDE.md:92: `/var/lib/tomcat10/webapps/ROOT.war`.
- **Production OS:** Ubuntu 24.04 on IONOS Cloud (CLAUDE.md:88).
- **Database:** MySQL on the same host. Production MySQL invocation requires an `LD_LIBRARY_PATH` workaround (`/usr/lib/x86_64-linux-gnu`) due to an Acronis library conflict (CLAUDE.md:84-89).
- **TLS:** Production fronted by nginx with Let's Encrypt (docs/tomcat_ssl_setup.md:1-9).
- **Operational scripts:** `docs/scripts/backup.sh`, `docs/scripts/healthcheck.sh`, `docs/scripts/update.sh`.
- **Outlook icon utility:** `scripts/generate-outlook-icons.ps1`.
- **CI:** `.github/workflows/build.yml` builds the WAR on push to `main`/`beta`/`dev` (.github/workflows/build.yml:1-29). CI does not deploy.

---

## 10. JSON resource bundles

`src/main/resources/` carries JSON used at runtime:
- `knowledge/` — eight files including `knowledge-config.json`, `automation-email-builder.json`, `proposal-page-builder.json`, plus indexed knowledge files (`backup_recovery_indexed.json`, `business_continuity_indexed.json`, `summit_guide_indexed.json`, `wave_help_indexed.json`) and `summit_videos.json`. Loaded by `KnowledgeSearchService`.
- `packages/` — eight files: `package-index.json`, `general.json`, plus per-product packages (`billing_payments.json`, `cobra.json`, `hra.json`, `hsa.json`, `s125_fsa.json`, `transit_parking.json`). Loaded by `PackageLoader`.
- `questionnaire/` — `questionnaire_seeds.json`. Loaded by `QuestionnaireLoader`.

---

## 11. Notable utilities

- `tools/tomcat-jakartee-migration.jar` — third-party Tomcat Jakarta-EE namespace migration tool (one-off jar, not invoked by build).
- `ams-src.7z` — 9.6 MB compressed snapshot of source under repo root. Single commit, untouched since 2025-09-19.
- `src-directory.txt` — UTF-16-encoded file listing of an older flat `controller/` layout (src-directory.txt:1-15).

---

## 12. Notes on items declared but with no source-side import found

These dependencies are declared in `pom.xml` but no `import` of their packages was found in `src/main/java` during this pass:

- `com.microsoft.azure:msal4j:1.21.0`
- `com.microsoft.graph:microsoft-graph:5.52.0`

(See `AMS-OPEN-QUESTIONS.md` — they may be reflectively loaded, removed but not deleted from pom, or this pass missed an import.)
