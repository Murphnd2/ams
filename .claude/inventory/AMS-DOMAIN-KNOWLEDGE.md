# AMS Domain Knowledge — Pass 1

> **⚠️ STALE SNAPSHOT.** Generated 2026-04-25 (Pass 1). Confirmed stale 2026-07-28 — this file still
> claims the highest migration is V062; the actual highest is **V073**. Structural/package
> information remains broadly useful; **any claim about migration numbers, current branch, or which
> reference-data rows exist is unreliable.** Verify against `docs/analysis/migration_tracker.md`,
> `docs/claude_memory.md`, live `DatabaseInitializer` code, or the database itself.
>
> See also the accuracy warning at the top of `docs/analysis/entity_reference.md` — several
> "what data exists" claims in these inventory files trace to the same dead seeder
> (`ReferenceDataSeeder.java`, unreachable via the commented-out `Main.java:35`).

This document captures the **business domain** of AMS as it can be reconstructed from source code, JPA entities, controller subpackages, configuration, and `docs/`. Citations refer to files actually present in the working tree. Where a fact comes from a doc rather than code, the doc is named explicitly. Where the doc and the code disagree, the contradiction is logged in `docs/analysis/archive/AMS-OPEN-QUESTIONS.md` rather than reconciled here.

---

## 1. System purpose

AMS ("Activity Management System") is a Java/Jakarta EE web application owned and developed by **Superior State Administration (SSA)**. The repository's project metadata and CLAUDE files describe it as a **benefits-administration companion platform** intended to be used alongside a third-party platform called **Datapath / Summit** by entities called **PSPs (Plan Service Providers)** (CLAUDE.md:3-12; docs/ams_to_be_vision.md:1-40).

Concrete signals in the source tree:

- The Maven coordinates declare `net.superiorstate:ams:1.0-SNAPSHOT`, packaging `war`, with a final WAR name of `ROOT.war` (pom.xml; the same artifact name is referenced by deployment notes in CLAUDE.md:55-58).
- The persistence unit is `ssaPU` against MySQL schema `beta_ssa` (`src/main/resources/META-INF/persistence-local.xml`; `pom.xml` profiles).
- A "Summit" import/sync subsystem sits beside an internal "AMS" model — see `data/service/SummitSync.java`, `data/service/SummitImportService.java`, `data/service/SummitProviderSeeder.java`, and the `model/summit/` package containing dozens of entities mirroring Summit-style tables (sEmployee, sEmployer, sBenefit, sCoveragePB, sEnrollment, HsaEe, HsaEr, etc.).
- The deployment target referenced in CLAUDE.md is `superiorstate.biz` on IONOS Cloud Tomcat 10 (CLAUDE.md:55-66).

---

## 2. Top-level domain entities

The domain model lives under `src/main/java/net/superiorstate/ams/model/`. The structure already reveals the major problem domains:

```
model/
├── activity/          ← Activity hierarchy (CRM-like work units)
├── billing/           ← Monthly billing
├── general/           ← Person, User, PSP, Address, etc.
├── imports/           ← Generic import-mapping tables
├── sales/             ← Agency / Application / Offering catalog
├── summit/            ← Summit (Datapath) imported and archived data
└── upload/            ← Upload preview DTO
```

### 2.1 Person hierarchy (single-table inheritance)

`model/general/Assignee.java` is the abstract root of a `SINGLE_TABLE` hierarchy. From the file listing, concrete subclasses include `Person`, `PSP`, `Recipient` (under `general/`) and `Activity` and its subclasses (under `activity/`). MEMORY.md:53-55 records:

> "Person is SINGLE_TABLE in `assignee` — any FK to a Person column must reference `assignee(id)`"

Concrete Person types are differentiated by **role IDs** (see `general/UserRole.java`). MEMORY.md:33-36 enumerates the role IDs used in production:

```
1=PSP User, 2=Agent, 3=Client, 4=Applicant,
5=PSP Admin, 8=Agency Admin, 9=PSP Super User,
102=BPO Admin, 103=BPO User
```

(Role 9's DB-seeded label is "PSP Super User", not "PSP Sales" — `DatabaseInitializer.java`; the session flag it maps to is still `isPspSales`, a cosmetic label/functional-name mismatch, not a numeric-ID error.)

`UserRole.java:11-21` is a thin entity (id+description, mapped on `userRoleList` ManyToMany on `User`); the role IDs themselves are seed-data driven.

### 2.2 Activity hierarchy

`Activity` (under `model/activity/`) is itself a subclass of `Assignee` and is in turn extended by:

- `model/activity/checklist/CheckList.java` (and `RecurringItems.java`, plus `sequences/` and `tasks/` subpackages)
- `model/activity/Opportunity.java` (sales opportunities, distinct from `Proposal`)
- `model/activity/renewal/Renewal.java` (and `RenewalEmployer.java`, `RenewalItem.java`)
- `model/activity/ticket/Ticket.java` and `model/activity/ticket/setup/Setup.java`

CLAUDE.md:30-34 paraphrases this as: "`Assignee` → `Activity` → `Ticket`/`Renewal`/`Setup`/`CheckList`". The actual file tree adds `Opportunity` to that list.

Supporting entities under `activity/`:

- `note/Note.java`, `note/Email.java`, `note/EmailComparator.java`, `note/ActivityStatus.java`, `note/ReasonCreated.java` — activity comms / status enums
- `ndt/NdtTestRun.java`, `ndt/NdtAccessLog.java`, `ndt/NdtDocumentUpload.java` — Section 125 nondiscrimination testing artifacts (NDT)
- `questionnaire/Questionnaire.java`, `QuestionnaireField.java`, `QuestionnaireFieldValue.java`, `QuestionnaireInstance.java` — generic questionnaire framework
- `ticket/TicketCategory.java`, `ContactMethod.java`, `tEmployee.java` — ticket support data

### 2.3 Sales subsystem

`model/sales/` is split into `agency/`, `application/`, and `offering/`. The names map to a sales-portal pipeline that is described narratively in `docs/ams_to_be_vision.md`. Entities present:

- **Agency / Agent / Prospect:** `agency/Agency.java`, `agency/Invitation.java`, `agency/Prospect.java`
- **Proposal builder:** `agency/Proposal.java`, `agency/PriceItem.java`, `agency/Rate.java`, `agency/RateDiscount.java`, `agency/RateTable.java`, `agency/RateTableID.java`
- **Application (signed proposal → onboarding):** `application/Application.java`, `application/ApplicationField.java`, `application/ApplicationFieldValue.java`, `application/ApplicationModule.java`, `application/ApplicationModuleID.java`, `application/ApplicationSection.java`
- **Offering catalog:** `offering/LOS.java` (Line of Service), `offering/ServiceModule.java`, `offering/ModuleDetail.java`, `offering/Feature.java`, `offering/Enhancement.java`, `offering/BenefitType.java`, `offering/BillingType.java`, `offering/MarketingMaterial.java`, `offering/ResourceCategory.java`, `offering/ProposalSection.java`

### 2.4 Billing

`model/billing/`:

- `BillingMonth.java`, `BillingItem.java`, `BillingGroup.java`, `BillingGrid.java`, `BillingLink.java`, `BillingSummary.java`
- Variance reporting: `EmployeeVariance.java`, `EmployerVariance.java`

The corresponding service layer is `data/service/Biller.java` and `data/service/MonthlyBiller.java`; see also `controller/monthly/`.

### 2.5 Summit (Datapath) integration

`model/summit/` is the largest subdomain by entity count and is split into:

- `summit/imports/` — staging tables populated from CSV/Excel uploads (DpiEr, HsaAccount, HsaEe, HsaEr, sEmployee/sEmployee2, sEmployer/sEmployer2, sBenefit, sBenefitTierPB, sBenefitYear, sCoveragePB, sCobraQb, sEnrollment) plus an `order/` subpackage referenced from CLAUDE.md.
- `summit/temp/` and `summit/archive/` — directories present (contents not enumerated in this pass).

Service-side: `data/service/SummitSync.java`, `SummitImportService.java`, `SummitProviderSeeder.java`.

### 2.6 General supporting entities

`model/general/`:

- Identity / org: `Person.java`, `User.java`, `UserRole.java`, `PSP.java`, `Address.java`, `PspClient.java`
- Communications: `Recipient.java`, `WebLink.java`, `LinkType.java`, `OutlookUserLink.java`, `ChatbotSkill.java`, `Automation.java`
- Time tracking: `TimeLog.java`, `TimeStretch.java`, `TimeCorrectionRequest.java`, `DaySummary.java`
- Vendor / partnership: `ApprovedVendor.java`, `BpoRegistration.java`, `ManagedInstallation.java`
- Compliance: `IrsLimit.java`
- Training video: `TrainingVideo.java`, `VideoToken.java`
- User prefs: `UserFilterPreset.java`, `SequenceTracker.java`

### 2.7 Generic imports framework

`model/imports/` contains a domain-agnostic mapping layer used by the Interactive Import Wizard:

- `ImportProvider.java`, `ImportFileType.java`, `ImportFieldMapping.java`, `ImportPlanTypeMapping.java`, `ImportIdMapping.java`, `ImportRunLog.java`

Service: `data/service/ImportResolutionService.java`, `ImportCommitService.java`, `InteractiveImportSession.java`, `UniversalImportService.java`.

### 2.8 View-backed read-only entities

Several entities map onto database views and act as read-only DTOs (the "25" suffix denotes the modern variant). At the `model/` root:

- `Activity25.java`, `Activity25p.java`, `Activity25u.java` — modern activity-list views (likely PSP / user variants)
- `Checklist25.java`, `Checklist25u.java`
- `ActivityLandingFilter.java`, `ActivityLandingRow.java`, `AgentSetupRow.java`, `HomeData.java`, `ToDoOut25.java`, `ReqTaskListTix.java`
- `Constant.java` — KV store backing app constants probed by `EmfListener`

`PersonV.java` (under `general/`) is similarly a view-backed read-only entity (CLAUDE.md:35-36 references it as a "view-backed entity" pattern).

---

## 3. User roles and access tiers

The runtime distinguishes three deployment **system types** at the installation level (CLAUDE.md and MEMORY.md), and within an installation distinguishes user roles by numeric IDs:

| Role ID | Name | Notes |
|---|---|---|
| 1 | PSP User | Generic internal user |
| 2 | Agent | External sales agent — gated portal `controller/.../AgentHome` referenced in MEMORY.md |
| 3 | Client | Employer-side user |
| 4 | Applicant | Prospect/applicant |
| 5 | PSP Admin | Internal admin |
| 8 | Agency Admin | External agency manager |
| 9 | PSP Super User | Internal sales; session flag is `isPspSales` despite the DB label (label/functional-name mismatch, cosmetic) |
| 102 | BPO Admin | Business-process-outsourcing admin (federated vendor) |
| 103 | BPO User | BPO worker |

Source: `MEMORY.md:33-36` (the project's persistent memory file). The numeric IDs are not enumerated in code but are seeded by `data/service/ReferenceDataSeeder.java` / `DemoDataSeeder.java` (file present, contents not read in this pass).

Authentication implementation:

- Login flow lives in `controller/authentication/` (file listing not fully enumerated).
- `LoginFilter.java` (`@WebFilter("/*")`) inspects the `local` session attribute (`AmsDataLocal`) and short-circuits requests to a hardcoded list of public paths — `/api/`, `/proposal/`, `/apply/`, `/q/`, `/tpo`, `/video`, `/outlook/` (LoginFilter.java in this pass; cited in AMS-TECHNICAL-ARCHITECTURE.md).
- Password storage is SHA-512 with a per-user salt (`data/dao/AuthDAO.java`; cited in AMS-TECHNICAL-ARCHITECTURE.md).
- A separate `ApiTokenFilter` covers `/api/*` for machine-to-machine endpoints.

---

## 4. External integrations

Reconstructed from `pom.xml`, `data/service/`, `data/dao/`, `controller/api/`, and `controller/api/outlook/`:

| Integration | Direction | Evidence |
|---|---|---|
| **MySQL 8 (`beta_ssa`)** | Primary store | `mysql-connector-j` 8.4.0; `persistence-local.xml`; CLAUDE.md:8 |
| **EclipseLink JPA 3.0.2** | ORM | `pom.xml` |
| **Wasabi / S3 (AWS SDK v2)** | Object storage uploads | `aws-sdk-bom` 2.20.69; `data/dao/StorageDAO.java` (referenced in MEMORY.md Session 80) |
| **Apache POI 5.2.3** | Excel import/export | `pom.xml` |
| **opencsv 5.9** | CSV import | `pom.xml` |
| **PDFBox 3.0.4** | PDF generation/parsing | `pom.xml` |
| **Eclipse Angus mail 2.0.3** | SMTP | `pom.xml`; `controller/email/` |
| **MSAL4j + Microsoft Graph** | Declared but unused | `pom.xml` declares `msal4j` 1.21.0 and `microsoft-graph` 5.52.0; no imports found in `src/main/java` (see docs/analysis/archive/AMS-OPEN-QUESTIONS.md) |
| **Outlook Web Add-in** | Inbound REST | `controller/api/outlook/` (NoteAddedCallbackApi, etc. — see API table below). MEMORY.md Sessions 77-78 describe the add-in. |
| **Datapath / Summit** | CSV imports + sync | `model/summit/imports/`, `data/service/SummitSync.java`, `data/service/SummitImportService.java` |
| **Anthropic Claude API** | AI assistant | `data/service/ClaudeApiService.java`; `controller/assistant/` |
| **BPO vendor partners** | Outbound/inbound REST (federated tasks) | `controller/api/PartnershipRequestApi.java`, `PartnershipApproveApi.java`, `TaskReceiveApi.java`, `TaskUpdateApi.java`, `TaskCompletedCallbackApi.java`, `TaskNotesApi.java`, `NoteAddedCallbackApi.java`, `VendorRegistryApi.java`, `SystemRegisterApi.java`, `data/service/VendorRegistryService.java`, `data/service/BpoTaskPushService.java` |
| **Questionnaire webhooks** | Inbound | `controller/api/QuestionnaireWebhookApi.java`; `data/service/QuestionnaireService.java` |
| **System telemetry** | Outbound (master collects from installations) | `controller/api/SystemHealthApi.java`, `SystemConstantsApi.java`; `model/general/ManagedInstallation.java`; `EmfListener.startInstallationHealthScheduler()` for master only |
| **jsoup 1.17.2** | HTML sanitization (sanitizeHtml) | `pom.xml`; SKILL.md describes the `sanitizeHtml()` server method |
| **Log4j 2.20.0 + SLF4J bridge** | Logging | `pom.xml`; runtime logs via `catalina.out` |

---

## 5. API endpoint surface (machine-to-machine)

Endpoints under `controller/api/` (deduced from `@WebServlet` URL patterns, exposed via `/api/*`). All `/api/*` paths bypass `LoginFilter` and are gated by `ApiTokenFilter` instead.

Top-level (non-Outlook):

- `NoteAddedCallbackApi`, `TaskCompletedCallbackApi`, `TaskNotesApi`, `TaskReceiveApi`, `TaskUpdateApi` — task federation callbacks
- `PartnershipApproveApi`, `PartnershipRequestApi` — vendor partnership lifecycle
- `QuestionnaireWebhookApi` — questionnaire submissions
- `SystemConstantsApi`, `SystemHealthApi`, `SystemRegisterApi` — installation telemetry & registration
- `VendorRegistryApi` — registry queries

Outlook subtree (`controller/api/outlook/`): files not enumerated individually in this pass, but MEMORY.md Sessions 77-78 describe two flows:

- **"Log to AMS"** — write an email back to an AMS activity as a note
- **"Create Ticket"** — create a ticket entity from an email; tabbed taskpane; contact selection from recipients

The Outlook add-in client is presumably hosted under `webapp/outlook/` (LoginFilter allows `/outlook/`).

---

## 6. End-user surface (servlet controller subpackages)

`controller/` is organized by feature, not by technical layer (CLAUDE.md:14-23, with the live tree extending it):

```
controller/
├── activity/         ← Activity CRUD (+ contact, renewal, setup, ticket)
├── admin/            ← Admin tooling
├── api/              ← M2M API + Outlook integration
├── assistant/        ← AI/Claude assistant UI
├── authentication/   ← Login, password, etc.
├── checklist/        ← Checklist management
├── data/             ← Import/export servlets
├── email/            ← Email composition workflow
├── home/             ← Home dashboards (likely per role)
├── market/           ← Marketing/library (Sales & Marketing Library — per ams_to_be_vision.md)
├── monthly/          ← Monthly billing UI
├── sequence/         ← Sequence builders (recurring task templates)
└── user/             ← User management
```

CLAUDE.md:14-23 lists this structure but omits `admin`, `assistant`, `home`, and `market` — those appear in the live tree only. (See docs/analysis/archive/AMS-OPEN-QUESTIONS.md.)

---

## 7. Workflows (reconstructed from package + service names)

The code suggests the following primary workflows. Each entry names the controller(s) and service(s) where the bulk of the implementation lives. **No attempt is made to verify that any workflow is currently complete or fully wired** — this is a structural map, not a behavioral audit.

### 7.1 Activity lifecycle

- Create / list / detail under `controller/activity/` (and subpackages `contact/`, `renewal/`, `setup/`, `ticket/`).
- Status / notes / emails: `controller/email/` + `model/activity/note/`.
- Modern detail page is the "25" variant (e.g., `Activity25` view-backed entity, `GoActivityDetail25` referenced in MEMORY.md Session 85).
- Setup-specific: `model/activity/ticket/setup/Setup.java` + `data/service/SetupPromotionService.java` (Session 84).

### 7.2 Sales pipeline

`docs/ams_to_be_vision.md:30-180` describes a pipeline `LOS → Module → Rate → Rate Table → Fee Type → Agency → Sales Agent → Prospect → Proposal → Application → Setup`. The matching code:

- **Catalog:** `model/sales/offering/`
- **Pricing:** `model/sales/agency/Rate.java`, `RateTable.java`, `RateDiscount.java`, `PriceItem.java`
- **Acquisition:** `model/sales/agency/Prospect.java`, `Invitation.java`
- **Quotation:** `model/sales/agency/Proposal.java` + proposal builder JSPs (CLAUDE.md, MEMORY.md `proposal-content-page` skill)
- **Onboarding:** `model/sales/application/` (Application as signed proposal → setup)
- **Promotion to active:** `data/service/SetupPromotionService.java`

### 7.3 Recurring tasks / sequences

- Templates: `model/activity/checklist/sequences/` (subpackage; not enumerated in this pass)
- Tracker: `model/general/SequenceTracker.java`
- Controllers: `controller/sequence/` (sequence builders), `controller/checklist/`

### 7.4 Imports

Two import subsystems coexist:

- **Summit imports** (provider-specific, CSV/Excel from Datapath): `data/service/SummitImportService.java`, `data/service/SummitSync.java`, staging tables in `model/summit/imports/`.
- **Interactive / Universal imports** (generic, mappable): `data/service/InteractiveImportSession.java`, `ImportResolutionService.java`, `ImportCommitService.java`, `UniversalImportService.java`, mapping entities in `model/imports/`.

CLAUDE.md and MEMORY.md (Session 66) describe the Interactive Import Wizard as recently completed (B2-B5).

### 7.5 Monthly billing

- Service: `data/service/MonthlyBiller.java`, `data/service/Biller.java`
- Entities: `model/billing/`
- UI: `controller/monthly/`

### 7.6 Compliance: NDT (Section 125 nondiscrimination testing)

- Entities: `model/activity/ndt/NdtTestRun.java`, `NdtAccessLog.java`, `NdtDocumentUpload.java`
- Backed by V058 (NDT) + V059 (census-based testing data model) — MEMORY.md Session 76

### 7.7 Questionnaires

- Entities: `model/activity/questionnaire/`
- Service: `data/service/QuestionnaireService.java`, `QuestionnaireLoader.java`
- Inbound: `controller/api/QuestionnaireWebhookApi.java`
- See V039 and follow-ups (MEMORY.md)

### 7.8 Time tracking

- Entities: `model/general/TimeLog.java`, `TimeStretch.java`, `DaySummary.java`, `TimeCorrectionRequest.java`
- (Controllers not enumerated by name in this pass — likely in `controller/user/` or similar)

### 7.9 Training videos

- Entities: `model/general/TrainingVideo.java`, `VideoToken.java`
- Token-gated streaming via `ServeVideo` servlet; admin via `ManageVideos` (MEMORY.md Session 73)
- Master-system-only feature gated by `isMasterSystem`

### 7.10 Federated BPO / vendor task outsourcing

- Service: `data/service/BpoTaskPushService.java`, `VendorRegistryService.java`
- API surface: `PartnershipRequestApi`, `PartnershipApproveApi`, `TaskReceiveApi`, `TaskUpdateApi`, `TaskCompletedCallbackApi`, `TaskNotesApi`, `NoteAddedCallbackApi`, `VendorRegistryApi`, `SystemRegisterApi`
- Entity hooks: `model/general/ApprovedVendor.java`, `BpoRegistration.java`, `ManagedInstallation.java`

### 7.11 Outlook add-in

- Server endpoints under `controller/api/outlook/`
- Manifest / client likely under `webapp/outlook/`
- See MEMORY.md Sessions 77-78, 82

### 7.12 AI assistant

- Service: `data/service/ClaudeApiService.java`, `KnowledgeSearchService.java`
- Entity: `model/general/ChatbotSkill.java`
- Controller subpackage: `controller/assistant/`
- Doc: `docs/ai_assistant_design.md` (referenced in `docs/analysis/archive/AMS-DOCS-INDEX.md`)

---

## 8. Deployment models — three system types

The codebase distinguishes **three flavors** of installation. The flag-set is driven by `ssa.properties` and surfaced through `AppConfig` and `AmsDataGlobal`:

| Type | Purpose (per MEMORY.md / docs) | Notable behaviors |
|---|---|---|
| **PSP installation** (default) | Production tenant for a specific PSP (e.g., `superiorstate.biz`) | Standard end-user UI |
| **BPO installation** | Federated worker installation that receives tasks from PSPs | Different home dashboard; auto-approval of required-sequence tasks (MEMORY.md Session 60) |
| **Master installation** (`ssa-master`) | Central registry / health monitor for all installations; hosts `TrainingVideo` library | `EmfListener` starts `InstallationHealthScheduler` only when `isMasterSystem` |

CLAUDE.md and MEMORY.md describe four live VPS hosts (`ssa-production`, `ssa-demo`, `ssa-bpo`, `ssa-master`) on IONOS Cloud (MEMORY.md Session 75).

---

## 9. Master / Demo / Production / BPO migration state

MEMORY.md tracks per-installation schema state:

- V025-V057 applied to **Production** and **Master**
- V038 applied to **Demo** and **BPO**
- V058-V061 code-complete, not yet applied anywhere
- V060 applied to local dev + production only
- Latest migration in tree (per `AMS-INVENTORY.md`): **V062**

CLAUDE.md:42 records the "current highest version" as **V044**, which conflicts with the actual contents of `docs/migrations/`. Logged in `docs/analysis/archive/AMS-OPEN-QUESTIONS.md`.

---

## 10. Domain glossary (terms used in the code)

| Term | Meaning (per code/docs in this tree) |
|---|---|
| **PSP** | Plan Service Provider — the SSA-equivalent organization running an AMS instance (`model/general/PSP.java`) |
| **BPO** | Business Process Outsourcing — a vendor org that performs delegated work for one or more PSPs (`model/general/BpoRegistration.java`) |
| **Agency** | Outside sales/broker organization that sources prospects (`model/sales/agency/Agency.java`) |
| **LOS** | Line of Service — top of the offering catalog (`model/sales/offering/LOS.java`) |
| **Module / ServiceModule** | A purchasable line item under an LOS (`model/sales/offering/ServiceModule.java`) |
| **Rate / Rate Table** | Pricing applied to a Module for a given Agency (`Rate.java`, `RateTable.java`) |
| **Prospect** | Potential client, sourced by an Agency or PSP Super User (`Prospect.java`) |
| **Proposal** | Quotation issued to a Prospect (`Proposal.java`) |
| **Application** | Signed Proposal in onboarding state, parent of section/module/field tree (`Application.java`) |
| **Setup** | Activity subclass — onboarding work created from an accepted Application (`Setup.java`) |
| **Activity** | Any unit of trackable work; abstract over Ticket/Renewal/Setup/CheckList/Opportunity |
| **CheckList** | Recurring multi-task activity (`CheckList.java`) |
| **Ticket** | Service-desk-style activity (`Ticket.java`) |
| **Renewal** | Annual employer-renewal activity (`Renewal.java`) |
| **Opportunity** | Sales-pipeline activity distinct from Proposal (`Opportunity.java`) |
| **Sequence** | A recurring task template (`SequenceTracker.java`, `controller/sequence/`) |
| **Assignee** | Inheritance root for any entity that can own/be assigned work (Person, PSP, Activity, Recipient) |
| **NDT** | Nondiscrimination Testing — IRS Section 125 compliance reports (`model/activity/ndt/`) |
| **Summit** | The Datapath benefits platform AMS imports from / mirrors |
| **Master system** | Central registry installation that monitors all PSP/BPO installations |
| **TPO** | Third-Party Outsourcing — referenced in `LoginFilter` allow-list (`/tpo`) |
| **"25" suffix** | Marker for the modern variant of a servlet/JSP/entity (e.g., `Activity25.java`) |
| **Ghost button** | UI convention `.ssa-action`/`.ghost-action`/`.nav-ghost` (MEMORY.md "Key Patterns") |

---

## 11. Deprecated / legacy items observed

- `model/Activity25p.java` and `Activity25u.java` — sit alongside `Activity25.java` with no immediately-obvious purpose distinction; one of these may be a deprecated variant. (Logged.)
- `model/Checklist25u.java` next to `Checklist25.java` — same pattern.
- `model/summit/imports/sEmployee.java` and `sEmployee2.java`; same for `sEmployer.java` and `sEmployer2.java` — apparent versioning by suffix.
- `webapp/ckeditor/` and `webapp/ckeditor5/` (per `AMS-INVENTORY.md`) — two CKEditor distributions coexist.
- `model/general/ChatbotSkill.java` plus `data/service/ClaudeApiService.java` and `KnowledgeSearchService.java` — unclear whether the chatbot subsystem is actively used or in-progress (no doc consulted in this pass).
- `model/general/Automation.java` — referenced by MEMORY.md Session 81 ("Automation email editable preview") so likely current.
- `data/template/` package was reported as empty in earlier inventory work (logged in OPEN-QUESTIONS).

---

## 12. What this document does NOT cover

By the rules of this pass:

- No attempt is made to map JSP files to the servlets that forward to them.
- No SQL/DDL is read — the schema is described only via JPA entity files.
- No JavaScript / front-end behavior is described beyond what `webapp/` directory naming reveals.
- No assertion is made about which workflows are *complete* vs *in-progress* — that requires running the application.
- No external system (Datapath, Wasabi, IONOS, MS Graph, Anthropic API) is contacted; integrations are described from declared dependencies and code references only.
