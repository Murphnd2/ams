# Phase A — Participant identity: fact-finding

> **The blocking claim is partly wrong.** AMS *does* have an AMS-generated employee key — two allocators plus
> an unused `employee.custom_id VARCHAR(45)` — and AMS *does* write named, addressed `employee` rows with no
> Summit import involved. What is genuinely absent is hire/effective/termination dates on `Employee`, and any
> roster tied to a **Prospect** (sales stage) rather than an **Employer**.

Branch `refactor/modernize-architecture` · HEAD `c5d66f6` (expected `fb8673d`; that is HEAD~2 — docs-only
commits `736f9ee`, `c5d66f6` sit on top) · tree clean at start · 2026-09-07 · read-only.

## Answers

### Q1 — `Employee`

`src/main/java/net/superiorstate/ams/model/summit/archive/Employee.java` (225 lines). **No `@Table`** — the
default name `employee` applies. DDL `docs/importscript/beta_ssa_baseline_v031.sql:1670-1692`; the table
predates the V025+ window and **no migration touches it** (`grep -rn -i "custom_id" docs/migrations/*.sql` →
no matches).

```java
Employee.java:10-12   @Id
                      @Column(name="employee_id")
                      private int id;
```
**No `@GeneratedValue`** — assigned, `int` (not `Long`). DDL `employee_id int NOT NULL`, no `AUTO_INCREMENT`.

Fields (`Employee.java:15-68`), `@Column` unless noted: `employer` (`@ManyToOne @JoinColumn(name="employer_id")`),
`firstName` `first_name`, `lastName` `last_name`, `mmKey` `mm_key` (int), `email`, `hrEmail` `hr_email`,
`address1`, `address2`, `city`, `state`, `zipCode` `zip`, `userId` `user_id`, `isActive` `is_active`, `customId`
`custom_id`, `employerList` (`@ManyToMany(mappedBy="contactList")`), `eeStatusId`, `systemStatusId`, `cobraStatusId`.
**No hire, effective, or termination date** — `grep -i "hire\|effective\|term" Employee.java` → no matches.

`custom_id`: Java `String`; DDL `custom_id varchar(45) DEFAULT NULL` (`:1685`) — nullable, **no unique constraint**
in entity or DDL (`PRIMARY KEY (employee_id)` and `KEY FK_EMPLOYEE_employer_id` are the only keys, `:1689-1691`).
**Write-only in AMS today:** `setCustomId` is called from three inbound import paths only
(`ImportCommitService.java:565`, `SummitImportService.java:477`, `UniversalImportService.java:757`);
`getCustomId()` (`Employee.java:194`) has **zero callers**.

### Q2 — Who creates `Employee` rows

`grep -rn "new Employee(" src/` — 15 hits:

| File:line | Trigger | Persists? |
|---|---|---|
| `SummitImportService.java:466` | Summit J-file import | yes, id = Summit `pid` |
| `UniversalImportService.java:745` | Universal (V048) import | yes, source id or reallocated |
| `ImportCommitService.java:553` | Interactive import commit | yes, source id or reallocated |
| `Updater.java:189` | Legacy batch import from `ImportEmployee` staging | yes |
| `Updater.java:598` | Employer-contact backfill, `contact.setId(nextId--)` | yes, **negative ids** |
| `DatabaseInitializer.java:1710` (`createEmployee`) | Bootstrap; called `:291`, `:420` with **`id = -1`** | yes |
| `DemoDataSeeder.java:926` (`seedEmployee`) | Demo seed, ids **`-200`…`-205`** (`:522-526`, `:549`) | yes |
| `CreatePspUser.java:141` / `CreatePspUser25.java:146` | **PSP staff-user creation in the UI**; `getNewId()` = `min(id) - 1` (`:158-162` / `:163-167`) | yes |
| `PersonDAO.java:330` (`assignToGenericEmployer`) | **Dead** — sole call site commented out (`PersonDAO.java:323`) | n/a |
| `EntityLookup.java:268` | Transient not-found sentinel (`setLastName("NULL")`) | no |
| `BillingAction.java:66,106,116,123` | Empty session placeholder | no |

**Yes — an `Employee` row can exist with no Summit import.** Creating a PSP user through `/CreatePspUser25`
persists one, as do `DatabaseInitializer` and `DemoDataSeeder`. AMS uses a **negative-id namespace** for its own
rows (`min(id)-1`, `nextId--`, `-1`, `-200…`), clear of Summit's positive ids. A positive-side allocator also
exists — `data/resolver/ImportIdResolver.java:87-96`, `case EMPLOYEE -> "SELECT COALESCE(MAX(employee_id), 0) + 1
FROM employee"` — called on PK collision by `UniversalImportService.java:740`, `ImportCommitService.java:546`.

### Q3 — Is there any other roster?

151 `@Entity` classes. Keyword-matching entities: `Person`, `PersonV`, `Employee`, `EmployeeV`, `Employer`,
`ImportEmployee`, `ImportEmployeeAlt`, `ImportEmployer`, `sEmployee`, `sEmployee2`, `sEmployer`, `sEmployer2`,
`Coverage`, `Enrollment`, `Enrollment2`, `ImportEnrollment`, `ImportCobPart`, `ImportCobraQb`.
`grep -ril subscriber src/main/java` → **no matches**; `enrollee` → one prose match (`RateCacheWarmService.java`).

**No migration creates any of them.** The `CREATE TABLE` names across all 70 migrations contain no `participant`,
`census`, `member`, `dependent`, `enrollee`, `roster`, `subscriber`, or `employee` table; `grep -rli` for those
words in `docs/migrations/*.sql` returns only `V059__ndt_census_tables.sql` (a JSON column, Q5),
`V076`/`V084`/`V085` (US Census ZIP reference data), `V065`/`V080`/`V092` (AI prompt prose), and
`V048`/`V069`/`V071`/`V077`/`V087`/`V093` ("dependent" in comments).

The person-tied-to-employer entities are all **Summit-sourced with assigned PKs**: `Employee` (Q1); `Employer`
(`Employer.java:9-11`, `@Id @Column(name="organization_id") private int id`, no `@GeneratedValue`); `Coverage`
(`Coverage.java:13-23`, `@Id int coverageId`, `participant_id → Employee`, `organization_id → Employer`);
`Enrollment` (`Enrollment.java:14-23`, `@Id int enrollmentId`, `participant_id → sEmployee`).

**Does any AMS-owned table hold a named person, tied to an employer, with an AMS-generated PK, not sourced from
Summit? — Partly: `assignee` does, but only as *contacts*, and never tied to an `Employer`.** `Person extends
Assignee` (`Person.java:13`), and `Assignee` carries `@Id @GeneratedValue @Column(name="id") private Long id`
(`Assignee.java:10-13`) — a genuine AMS key, single-table `assignee` (`beta_ssa_baseline_v031.sql:844-885`).
Named persons attach to a prospective employer via `Prospect.contact_id` (`Prospect.java:19-21`) and
`setupcontacts(setup_id, person_id)` (`Setup.java:27-30`; DDL `:3904-3908`). But those are one-or-a-few
decision-maker contacts, not an enumerated workforce: no hire date, no employment status, no election, no
enrollment — and their only route to an `Employer` is `Person.employee_id → Employee → Employer`
(`Person.java:38-40`), back through the Summit-keyed chain. **No AMS table enumerates an employer's workforce
under AMS-generated keys.**

### Q4 — `Person`

`model/general/Person.java`. `public class Person extends Assignee` (`:13`); `Assignee` is
`@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` (`Assignee.java:7-8`), so Person rows live in `assignee`
under `DTYPE`. **`@Id @GeneratedValue`** at `Assignee.java:10-13`.

Own fields (`Person.java:15-53`): `firstName`, `lastName`, `middleInit`, `email` (varchar 100), `phone` (varchar 12),
`title`. **No address lines of its own** — `@OneToOne @JoinColumn(name="address_id") Address address` (`:30-32`);
`Address` holds `address1/address2/city/state/zipCode` with its own `@Id @GeneratedValue` (`Address.java:12-26`).
Inherited: `fullName`, `webLinkList`, `assigneeContactList`.

FKs out: `address_id → Address`, `psp_id → PSP` (`:34-36`), **`employee_id → Employee` `@OneToOne` (`:38-40`)**.
Owning sides elsewhere: `Agency.agentList` M:N (`:28-29`), `email_recipents` M:N (`:42-44`), `Prospect.agent`
(`Prospect.java:27-29`), `Prospect.contact_id` (`:19-21`), `setupcontacts` M:N (`:49-50`), `assignee_contacts` M:N
(`:52-53`). Scoped by **PSP** (`psp_id`), agency (M:N), user account (`User`/`AuthDAO`), prospect (contact or agent).

`grep -rn "new Person("` — 42 hits, six kinds, one line each: a contact typed onto an activity/ticket (4 sites,
e.g. `AddContactToActivity.java:95`); a decision-maker captured on a prospect/opportunity/setup/agency/quote form
(8 sites, e.g. `CreateProspect.java:83`, `CreateSetup25.java:245`); a login being provisioned (8 sites, e.g.
`CreatePspUser25.java:102`, `AuthDAO.java:163`); an email recipient (5 sites, e.g. `AddRecipient25.java:76`); an
import/resolution match (7 sites, e.g. `PersonResolver.java:62`, `ImportCommitService.java:658`); and seeding or
transient placeholders (6 sites, e.g. `DemoDataSeeder.java:870`, `LogOut.java:36`). **None of them creates a
person as an employee of an employer** — every one is a contact, an agent, or a login.

**Person → Employee is linked, one direction only** (`Person.java:38-40`; column `assignee.employee_id` at
`beta_ssa_baseline_v031.sql:859`). `Employee` has **no** back-reference to `Person`.

### Q5 — Design census

One persisted census exists, and it is an unpopulated JSON blob.

`docs/migrations/V059__ndt_census_tables.sql:10-47` creates `ndt_test_run` (`test_run_id BIGINT AUTO_INCREMENT`)
with `census_data LONGTEXT` (`:20`), `employer_name VARCHAR(200) NOT NULL` (`:14` — a **string**, not an FK to
`Employer`), `plan_year_end DATE`, `employee_count INT` (`:26`), FKs to `assignee(id)` (`:38-41`). Entity
`NdtTestRun.java:55-56`.

**Persisted, not request-scoped. Designed identified:** `docs/importscript/ndt125_census_architecture.md:134-160`
specifies `employee_id`, `identity.display_name` ("Anonymized label … **or real name** depending on privacy
settings"), `identity.anonymized`, `identity.hire_date` (**required**), `identity.termination_date`,
`identity.date_of_birth`, `employment.status/hours/months_of_service/job_classification/union_status/entity_name`,
`compensation.*`, `ownership.*`, `benefits.enrolled_benefits` + `salary_reductions`/`employer_contributions`.
`ndt_document_upload` carries `ssn_detected`/`ssn_scrubbed`/`pii_scrub_log` (`V059:71-73`).

**But nothing populates it.** `grep -rn "setCensusData\|getCensusData" src/main/java` → three hits: the two
accessors (`NdtTestRun.java:128-129`) and exactly one write, the literal `testRun.setCensusData("[]")` at
`NdtTestRunServlet.java:178`. No parser, no reader, no DAO query. A schema column plus a written spec, not a
working roster — and a JSON blob, so nothing can join or FK to a person inside it.

### Q6 — Can the application answer store hold a roster?

**Physically yes; behaviourally no — strictly one value per field key per application.**

DDL `docs/importscript/beta_ssa_baseline_v031.sql:752-762` (identical at `beta_ssa_dev_baseline_thru_V024.sql:752`;
**no migration alters it**) — the key definitions this answer rests on:
```sql
`field_value_id` bigint NOT NULL AUTO_INCREMENT,   `application_id` bigint NOT NULL,
`field_key` varchar(100) ... NOT NULL,             `field_value` text,
PRIMARY KEY (`field_value_id`),  KEY `application_id` (`application_id`),  KEY `field_key` (`field_key`),
```
Entity `ApplicationFieldValue.java:5-22`: `@Id @GeneratedValue @Column(name="field_value_id") Long id`,
`@ManyToOne application`, `@ManyToOne applicationField`, `field_value TEXT`.

The PK is a **surrogate auto-increment**; `(application_id, field_key)` carries **only non-unique `KEY`s, no UNIQUE
constraint**; and there is **no group, index, sequence, repeat, or row-number column of any kind**. So the schema
would not reject N rows sharing a field key. Every code path collapses them anyway: the writer
`SaveApplicationProgress.java:101-121` queries by (`app`, `field`) then `existing.get(0).setFieldValue(...)`,
updating the first row and ignoring the rest — its empty-value branch `em.remove(existing.get(0))` (`:120`) deletes
only the first, and the same `get(0)` upsert appears at `ApplyForProposal.java:407-420`; readers build a
`Map<String,String>` keyed on `fieldKey` (`AgentSetupSnapshotLoader.java:56-59`,
`SummitExportServlet.loadApplicationAnswers:165-178`), so duplicates silently collapse, last row wins.

**A census cannot ride this surface without new schema. It needs its own table.**

### Q7 — `SummitExportServlet` as it stands

`controller/market/SummitExportServlet.java` (351 lines), `@WebServlet(name="SummitExportServlet",
value="/SummitExport")` (`:38`). Its own header already records the blocker: *"enrollment are out of scope; they
need an employee roster AMS does not have"* (`:31`).

Key build and config read (`:149-156`):
```java
private static String resolveEmployerTpaCustomId(Prospect prospect) {
    String rawPrefix = AppConfig.get("SUMMIT_TPA_ID_PREFIX");
    if (rawPrefix == null) return null;
    String prefix = rawPrefix.trim();
    if (prefix.isEmpty()) return null;
    if (prefix.contains("|") || prefix.chars().anyMatch(Character::isWhitespace)) return null;
    return prefix + "-" + prospect.getId();
}
```
Refusal paths, all "refuse rather than guess": no application answers → 400 (`:107-112`); null prefix → 500 with
`log.error` (`:120-129`); any of the four address answers blank → 400 (`:203-210`); unparseable
`plan_year_start`/`plan_year_end` → 400 (`:239-253`); missing `SUMMIT_ICHRA_PLAN_TEMPLATE_ID` → 500 (`:257-264`).

Structure a file-4 generator would extend, in five lines:
1. `doGet` guards (PSP-admin `:65`, `:92`), loads `Proposal → Prospect`, loads the answer map once, builds
   `employerTpaCustomId` once, dispatches on a `type` parameter (`:131-135`).
2. One `private void write<FileName>(response, prospect, answers, employerTpaCustomId)` per file.
3. Each pulls its inputs, refuses on any blank, then `String.join("|", …)`.
4. All share one sink, `writeFile(response, filename, line)` (`:299`) — which takes **a single `String line`**.
5. So file 4 needs a new dispatch branch, a new writer, **and a multi-row sink**: `writeFile` emits one record.

### Q8 — The V048 import configuration family

All five exist, created together in `docs/migrations/V048__universal_import_system.sql`:

| Table | Lines | Columns |
|---|---|---|
| `import_provider` | `:9-19` | `provider_id` AI PK, `provider_name`, `provider_code` UNIQUE, `description`, `is_active`, `psp_id`, `created_on` |
| `import_file_type` | `:21-32` | `file_type_id` AI PK, `provider_id`, `file_label`, **`target_entity VARCHAR(20) NOT NULL`**, `file_format` (dflt `CSV`), `sort_order`, `is_required`, `description` |
| `import_field_mapping` | `:34-45` | `mapping_id` AI PK, `file_type_id`, `source_column`, `canonical_field`, `is_required`, `is_key`, `transform_rule` |
| `import_plan_type_mapping` | `:47-57` | `mapping_id` AI PK, `provider_id`, `source_plan_code`, `source_plan_name`, `target_plan_type_id`, `is_system_default` |
| `import_run_log` | `:59-82` | `run_id` AI PK, `provider_id`, `run_by`, `started_on`, `completed_on`, `status`, `{plan_types,employers,employees,benefits}_{inserted,updated,skipped}`, `service_items_created`, `warnings`, `errors` |

Entities in `model/imports/` (one per table, plus `ImportIdMapping` from V051). Twelve Java readers/writers, all
import-side: `ProviderSetup`, `UniversalImport`, `InteractiveImport`, `SummitImportWizard`,
`ImportTransitionManager`, `UniversalImportService`, `ImportCommitService`, `ImportResolutionService`,
`SummitImportService`, `SummitProviderSeeder`, `DemoDataSeeder`, `ImportIdResolver`. `target_entity` values in
code today: **`PLAN_TYPE`, `EMPLOYER`, `EMPLOYEE`, `BENEFIT`** — `UniversalImportService.java:337-340` and
`V051__import_id_mapping.sql:12`.

**Inbound-only.** `grep -n "Import" SummitExportServlet.java` → no matches; every consumer above is an import
wizard, import service, or seeder. `import_field_mapping` maps *source column → canonical field*, a direction with
no inverse consumer in the tree.

### Q9 — Migration state

`ls docs/migrations/` — 70 files, `V025__plantype_import_columns.sql` … `V093__proposal_ichra_intake_section125.sql`,
plus one unversioned `seed_ndt125_questionnaire.sql`. **Highest present: `V093`. Next free: `V094`.**
`docs/analysis/migration_tracker.md:19` reads `## Current Highest Version: V093`. **They agree.**
(`CLAUDE.md` still claims V073 — not touched by this run.)

### Q10 — `Setup` and `assignee`

`assignee` (`beta_ssa_baseline_v031.sql:844-885`, `PRIMARY KEY (id)` at `:885`) is the single physical table behind
`@Inheritance(SINGLE_TABLE)` on `Assignee` (`Assignee.java:7-8`) — not a domain concept but the union of every
subclass's columns under a `DTYPE` discriminator (`:846`). Person contributes `first_name`…`psp_id`/`address_id`/
**`employee_id`** (`:859`); Activity the assignment/completion columns; Renewal **`employer_id`** (`:867`);
Opportunity **`prospect_id`** (`:877`); Setup `checklist_id`/`proposal_id`/`person_id`/`myRsc` (`:868-870`, `:875`).
`V059:37` states the convention: *"all reference assignee table due to single-table inheritance."*

`Setup extends Activity extends Assignee` (`Setup.java:12`, `Activity.java:14`) and holds `@OneToOne checklist_id →
CheckList`, **`@OneToOne @JoinColumn(name="proposal_id") private Application application`** (`Setup.java:18-20`) —
one Setup ↔ at most one Application, and `Application.@Id` *is* its `Proposal` (`Application.java:16-19`), so
Setup→Application→Proposal is 1:1:1 and Proposal→Prospect is N:1 — plus `@OneToOne person_id → Person
primaryContactSetup` (`:22-24`) and `@ManyToMany setupcontacts → List<Person> contactList` (`:27-30`). **No
per-employee data hangs off a Setup today:** its only person edges are those two contact links, it has no
`Employer` FK, and no collection of anything employee-shaped.

### Q11 — Who is the employer of record after setup?

`Prospect` (`model/sales/agency/Prospect.java:9-31`) has `@Id @GeneratedValue @Column(name="prospect_id") private
Long id`, `name varchar(200)`, `@OneToOne contact_id → Person`, `@OneToOne address_id → Address`, `@ManyToOne
agent_id → Person`, `@OneToMany(mappedBy="prospect")` proposals. **It persists unchanged after a Setup is created**
— Setup creation builds `Proposal`/`Application` around an existing prospect (`CreateSetup25.java:81`
`resolveProspect`, `:92-93` `proposal.setProspect(prospect)`) and never rewrites it;
`grep -rn "em.remove(.*[Pp]rospect\|DELETE FROM prospect"` → **no matches**. It is the only employer-shaped entity
in the tree with an AMS-generated PK.

Downstream the employer of record is **`Employer`** (`model/summit/archive/Employer.java:7-11`) — `@Id
@Column(name="organization_id") private int id`, assigned `int`, **no `@GeneratedValue`**, Summit-sourced, and the
FK target of `employee.employer_id` (`beta_ssa_baseline_v031.sql:1691`). Reached by `Renewal.employer`
(`Renewal.java:12-13`), `ActivityOut` (`:39-40`), `BillingGrid`/`BillingLink`, `Coverage.organization_id`, and the
HSA chain — all post-import surfaces. There is no `Client` or `Group` entity; `PspClient` is a BPO installation
concept, not an employer.

**Nothing links a `Prospect` to an `Employer`.** `grep -n "Employer" Prospect.java Setup.java` → no matches, and no
`setEmployer(` call anywhere takes a Prospect-derived value. The two identities never join: a roster row created at
sales stage can FK only to `Prospect(prospect_id)`; one created post-import can FK only to
`Employer(organization_id)`; no code reconciles them.

## Claims tested

| # | Sentence from "Why this run exists" | Verdict | Evidence |
|---|---|---|---|
| 1 | "AMS has no AMS-generated employee key" | **WRONG** | Two allocators: `ImportIdResolver.java:87-96` (`MAX(employee_id)+1`, called `UniversalImportService.java:740`, `ImportCommitService.java:546`) and `CreatePspUser25.java:163-167` (`min(id)-1`, negative namespace; same convention at `Updater.java:598`, `DatabaseInitializer.java:291,420`, `DemoDataSeeder.java:522-526`). Plus an unused `employee.custom_id varchar(45)` (`beta_ssa_baseline_v031.sql:1685`) — written by three import paths, `getCustomId()` never called. |
| 2a | "…no pre-Summit employee roster carrying names, addresses…" | **WRONG** on names + addresses | `employee` carries `first_name`, `last_name`, `address1`, `address2`, `city`, `state`, `zip` (`Employee.java:19-47`), and AMS writes rows with all of them and **no Summit import**: `CreatePspUser25.java:146-155` (a UI action), `DatabaseInitializer.java:1710-1725`, `DemoDataSeeder.java:926-936`. |
| 2b | "…and effective dates" | **CONFIRMED** | No hire, effective, or termination date on `Employee` (`Employee.java:10-68`; `grep -i "hire\|effective\|term"` → no matches) nor in its DDL (`beta_ssa_baseline_v031.sql:1670-1692`). The only place such dates are *specified* is the NDT census JSON (`ndt125_census_architecture.md:139-141`), which nothing populates (`NdtTestRunServlet.java:178` writes `"[]"`, the sole write). |
| 2c | "…roster" — an enumerated workforce keyed to a sales-stage employer | **CONFIRMED** | No table enumerates a workforce under AMS-generated keys tied to a `Prospect`. `Person`/`assignee` holds named persons with `@GeneratedValue` PKs (`Assignee.java:10-13`) but only as contacts (`Prospect.contact_id`, `setupcontacts`), with no employment, election, or enrollment attributes, and no `Employer` edge except back through `Person.employee_id → Employee` (`Person.java:38-40`). |
| 3a | "`Employee.@Id` is an assigned int" | **CONFIRMED** | `Employee.java:10-12` — `@Id @Column(name="employee_id") private int id`, no `@GeneratedValue`; DDL `employee_id int NOT NULL`, no `AUTO_INCREMENT` (`beta_ssa_baseline_v031.sql:1671`). |
| 3b | "…holding Summit's ID" | **PARTLY WRONG** | True only for import-created rows (`SummitImportService.java:466-467`). AMS-created rows hold AMS-allocated negative ids, and both import services **reallocate** on collision, warning "Employee ID X conflicts, allocated Y" (`UniversalImportService.java:739-745`, `ImportCommitService.java:545-551`). The column is a mixed namespace, not Summit's id. |

## What this run could not establish

- **Whether `custom_id` is populated in practice, and with what.** Write-only in AMS and sourced from the import
  file, so its real content is a property of Summit's export, not this tree. A Summit test export, or a Kevin
  decision to define it, would answer it. (Row contents are out of scope by project rule.)
- **What Summit's file-4 (Demographics) requires per participant, and whether `Participant TPA Custom ID` must be
  stable across plan years.** Not in the tree — needs the Summit spec or a test import, as session 24 did for the
  employer files.
- **Whether the negative-id namespace is intentional policy or accident.** Four independent sites use it with no
  shared constant and no comment. Kevin's call whether a roster extends it, uses
  `ImportIdResolver.allocateInternalId`, or uses `custom_id` / a new column.
- **Whether a roster belongs to a `Prospect` or an `Employer`.** Q11 shows the two identities never join in code.
  Which one a participant row FKs to is a design decision, not a fact recoverable from the tree.
- **Whether the NDT census was abandoned or merely unfinished.** Schema and a detailed spec exist; no parser does.
  Nothing in the tree dates or explains the stop.
