# Entity Reference — SSA Web Application

> **Last Updated:** February 21, 2026
> **Package root:** `net.superiorstate.ams.model`
> **Persistence:** EclipseLink JPA, `ssaPU`, MySQL `beta_ssa`
> **Inheritance strategy:** `SINGLE_TABLE` (rooted at `Assignee`)

## Business Context

Superior State Administrators (SSA) is a benefits administration company that uses **DataPath Inc.'s Summit** as its primary cloud-based administration system. Summit is the day-to-day platform for administering FSA, HRA, ICHRA, Transit, and COBRA services for SSA's clients.

**This web application (beta_ssa) bridges** the data SSA encounters across its operations with the source-of-truth data held in Summit for active clients. Key data flows:

- **Employers** = SSA's clients (imported from Summit)
- **Employees** = benefit participants or potential participants (imported from Summit)
- **Benefits** = the various services SSA administers: FSA, HRA, MERP, ICHRA, EBHRA, QSEHRA, COBRA, Transit, Retiree Billing, Direct Billing, Lifestyle Savings Account

### Important Design Decisions

1. **Person vs. Employee:** Summit's employee tables don't cover everyone SSA interacts with. The `Person` table was created so that every Employee can be linked to a Person, but not every Person is an Employee. A Person could be: a spouse, a new prospect, a sales agent, an employer contact, etc.

2. **Benefit ID Convention (CDH vs. Premium Billing):** Summit maintains two separate benefit tables — Consumer Driven Health (CDH) benefits and Premium Billing benefits (COBRA, Direct Bill, Retiree Billing). During import, both are brought into the single `Benefit` table. Since their IDs could overlap, **Premium Billing benefit IDs are negated** (positive → negative Long) to ensure uniqueness.

3. **Legacy Summit entities (`s`-prefix):** The entities in `model/summit/imports/` with `s` prefixes (`sEmployer`, `sEmployee`, `sBenefit`, `sBenefitYear`, `sEnrollment`) are **legacy and largely replaced** by the `Import*` entities in `model/summit/imports/order/`. However, some billing entities (`BillingLink`, `Enrollment`) still hold FK references to the `s`-prefix entities, so they can't be deleted without a migration.

4. **Sales entities are fully built out:** The `model/sales/` package contains entities for Agency, Prospect, Proposal, Application, Rate, LOS, ServiceModule, Enhancement, Feature, MarketingMaterial, ResourceCategory, and Invitation — supporting the complete sales pipeline from proposal creation through application approval and setup creation.

---

## Table of Contents

1. [Inheritance Hierarchy](#inheritance-hierarchy)
2. [model/general — Core Identity](#modelgeneral--core-identity)
3. [model/activity — Activity System](#modelactivity--activity-system)
4. [model/activity/note — Notes & Email](#modelactivitynote--notes--email)
5. [model/activity/renewal — Renewals](#modelactivityrenewal--renewals)
6. [model/activity/ticket — Tickets & Setup](#modelactivityticket--tickets--setup)
7. [model/activity — Opportunity](#modelactivity--opportunity)
8. [model/activity/checklist — Checklists & Tasks](#modelactivitychecklist--checklists--tasks)
9. [model/activity/checklist/sequences — Task Sequences](#modelactivitychecklistsequences--task-sequences)
10. [model/activity/checklist/sequences/support — Sequence Support](#modelactivitychecklistsequencessupport--sequence-support)
11. [model/billing — Monthly Billing](#modelbilling--monthly-billing)
12. [model/sales/agency — Agencies, Prospects & Invitations](#modelsalesagency--agencies-prospects--invitations)
13. [model/sales/offering — Service Catalog](#modelsalesoffering--service-catalog)
14. [model/sales/application — Applications](#modelsalesapplication--applications)
15. [model/summit/archive — Employer/Employee Archive](#modelsummitarchive--employeremployee-archive)
16. [model/summit/imports — Summit Import (Live)](#modelsummitimports--summit-import-live)
17. [model/summit/imports/order — Import Staging Tables](#modelsummitimportsorder--import-staging-tables)
18. [model/summit/temp — Temp/Transactional](#modelsummittemp--temptransactional)
19. [model (root) — View-Backed DTOs & Constants](#model-root--view-backed-dtos--constants)
20. [Non-Entity Support Classes](#non-entity-support-classes)
21. [Join Tables (No Entity Class)](#join-tables-no-entity-class)
22. [Entity Relationship Diagram](#entity-relationship-diagram)

---

## Inheritance Hierarchy

The entire activity/identity system shares a single table via `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` rooted at `Assignee`:

```
Assignee (abstract, @Entity, SINGLE_TABLE)
├── Person
├── PSP
├── Recipient
└── Activity
    ├── CheckList
    ├── Renewal
    ├── Ticket
    ├── Setup
    └── Opportunity

TaskSequence (abstract, @Entity, SINGLE_TABLE)
├── RequiredTaskList
├── RecurringTaskList
└── HowToList
```

All `Assignee` subtypes share the same database table with a discriminator column. `TaskSequence` subtypes similarly share a single table.

---

## model/general — Core Identity

### Assignee
| | |
|---|---|
| **Class** | `Assignee` (abstract) |
| **Table** | `assignee` (single-table inheritance root) |
| **ID** | `id` (Long, `@GeneratedValue`) |
| **Key Fields** | `fullName` (varchar) |
| **Relationships** | `webLinkList` → M:N WebLink (join: `assignee_links`), `assigneeContactList` → M:N Person (join: `assignee_contacts`) |
| **Notes** | Root of all identity/activity entities. Uses `SINGLE_TABLE` inheritance. |

### Person
| | |
|---|---|
| **Class** | `Person extends Assignee` |
| **Key Fields** | `firstName`, `lastName`, `middleInit`, `email`, `phone`, `title` |
| **Relationships** | `address` → 1:1 Address, `psp` → M:1 PSP, `employee` → 1:1 Employee, `emailList` → M:N Email (join: `email_recipents`), `listOfAgenciesWithThisAgent` → M:N Agency (mapped by `agentList`), `prospectList` → 1:M Prospect, `setupList` → M:N Setup (mapped by `contactList`), `listOfAssigneesWithThePerson` → M:N Assignee (mapped by `assigneeContactList`) |
| **Notes** | Universal contact record. Covers everyone SSA interacts with — employees, spouses, prospects, sales agents, employer contacts. Every Employee is a Person but not every Person is an Employee. |

### PSP
| | |
|---|---|
| **Class** | `PSP extends Assignee` |
| **Key Fields** | `taxId` (varchar 10) |
| **Relationships** | `address` → 1:1 Address, `contact` → 1:1 Person, `employer` → 1:1 Employer |
| **Named Queries** | `PSP.getById` |

### Recipient
| | |
|---|---|
| **Class** | `Recipient extends Assignee` |
| **Key Fields** | `emailAddress` |
| **Notes** | Used for non-Person email recipients. |

### User
| | |
|---|---|
| **Class** | `User` (standalone entity) |
| **Table** | `user` |
| **ID** | `person` (Person, `@OneToOne`, column `person_id`) — composite PK using Person |
| **Key Fields** | `userName` (unique), `email` (unique), `emailVerified`, `passwordHash`, `salt`, `tempGuid`, `guidExpiration`, `guidUsed`, `allowSetPassword` |
| **Relationships** | `person` → 1:1 Person, `userRoleList` → M:N UserRole (join: `userinroles`) |

### UserRole
| | |
|---|---|
| **Class** | `UserRole` |
| **Key Fields** | `id`, `description` |
| **Values** | 1=PSP User, 2=Agent, 3=Client, 4=Applicant, 5=PSP Admin, 6=Pending Agent, 7=Anonymous, 8=Agency Admin, 9=PSP Super User |

### Address
| | |
|---|---|
| **Class** | `Address` |
| **Key Fields** | `id`, `address1`, `address2`, `city`, `state`, `zipCode` |

### WebLink
| | |
|---|---|
| **Class** | `WebLink` |
| **Table** | `weblink` |
| **ID** | `id` (Long, column `link_id`) |
| **Key Fields** | `plainText` (description, varchar 200), `linkPath` (varchar 2000), `active` |
| **Relationships** | `linkType` → M:1 LinkType, `email` → M:1 Email, `listOfTasksWithThisWebLink` → M:N Task (mapped by `webLinkList`), `listOfAssigneesWithThisWebLink` → M:N Assignee (mapped by `webLinkList`) |

### LinkType
| | |
|---|---|
| **Class** | `LinkType` |
| **Key Fields** | `id`, `typeName` |
| **Notes** | Type 1 = file upload, Type 2 = external URL |

### TimeStretch
| | |
|---|---|
| **Class** | `TimeStretch` |
| **Notes** | Referenced in `SessionVar` for time tracking history. |

---

## model/activity — Activity System

### Activity
| | |
|---|---|
| **Class** | `Activity extends Assignee` |
| **Key Fields** | `dateCreated` (Timestamp, auto), `dueDate`, `isComplete`, `dateCompleted` |
| **Relationships** | `loggedBy` → M:1 Person (`created_by_id`), `assignedTo` → M:1 Assignee (`assigned_to_id`), `completedBy` → M:1 Person (`completed_by_id`), `primaryContact` → M:1 Person (`primary_contact`), `noteList` → 1:M Note |
| **Notes** | Base class for all activity types. Inherits `id`, `fullName`, `webLinkList`, `assigneeContactList` from Assignee. |

### ActivityOut
| | |
|---|---|
| **Class** | `ActivityOut` |
| **Notes** | DTO (non-entity) wrapping Activity for display purposes. |

### ActivityShell
| | |
|---|---|
| **Class** | `ActivityShell` |
| **Notes** | Lightweight DTO for activity list views. |

---

## model/activity/note — Notes & Email

### Note
| | |
|---|---|
| **Class** | `Note` |
| **ID** | `id` (Long) |
| **Key Fields** | `detail` (text), `dateGenerated`, `isResolution` (boolean, default false) |
| **Relationships** | `activity` → M:1 Activity, `createdBy` → M:1 Person, `reasonCreated` → M:1 ReasonCreated, `status` → M:1 ActivityStatus |
| **Notes** | `isResolution` flag used by AI chatbot to identify resolution notes for ticket knowledge base. |

### Email
| | |
|---|---|
| **Class** | `Email extends Note` |
| **Key Fields** | `subject` |
| **Relationships** | `recipientList` → M:N Person (mapped by `emailList`), `webLinkList` → 1:M WebLink |

### ReasonCreated
| | |
|---|---|
| **Class** | `ReasonCreated` |
| **Key Fields** | `id`, `description`, `isOutbound` |
| **Notes** | Lookup: Received Call, Received Email, Made Call, Left Voicemail, Sent Email, Quick Action, etc. |

### ActivityStatus
| | |
|---|---|
| **Class** | `ActivityStatus` |
| **Key Fields** | `id`, `description` |
| **Notes** | Lookup: 1=Waiting on Them, 2=No Change, 3=Waiting on Us |

---

## model/activity/renewal — Renewals

### Renewal
| | |
|---|---|
| **Class** | `Renewal extends Activity` |
| **Relationships** | `employer` → M:1 Employer (`employer_id`), `renewalItemList` → 1:M RenewalItem, `checkList` → 1:1 CheckList (`checklist_id`) |

### RenewalItem
| | |
|---|---|
| **Class** | `RenewalItem` |
| **Key Fields** | `id` |
| **Relationships** | `renewal` → M:1 Renewal, `benefit` → M:1 Benefit |

### RenewalEmployer
| | |
|---|---|
| **Class** | `RenewalEmployer` |
| **Notes** | DTO used for employer renewal list views. |

---

## model/activity/ticket — Tickets & Setup

### Ticket
| | |
|---|---|
| **Class** | `Ticket extends Activity` |
| **Key Fields** | `description` |
| **Relationships** | `contact` → M:1 Person (`person_id`), `ticketSubCategory` → M:1 TicketSubCategory (`ticket_category`), `contactMethod` → M:1 ContactMethod (`method_id`), `checkList` → 1:1 CheckList (`checklist_id`) |

### Setup (model/activity/ticket/setup/)
| | |
|---|---|
| **Class** | `Setup extends Activity` |
| **Relationships** | `application` → 1:1 Application, `checkList` → 1:1 CheckList, `contactList` → M:N Person, `primaryContactSetup` → M:1 Person |
| **Notes** | Represents new client setup activities. Created on application approval or manually via GenerateProp25. |

### TicketCategory
| | |
|---|---|
| **Class** | `TicketCategory` |
| **Key Fields** | `id` (Long), `description`, `shortCode` |
| **Notes** | Service-oriented categories: Claims, Debit Card, COBRA, HSA, etc. (Updated Feb 2026 from intent-based HOW/WHY/NEED categories.) |

### ContactMethod
| | |
|---|---|
| **Class** | `ContactMethod` |
| **Key Fields** | `id`, `description` |

### tEmployee
| | |
|---|---|
| **Class** | `tEmployee` |
| **Notes** | Ticket-specific employee view/wrapper used in `AmsDataGlobal`. |

---

## model/activity — Opportunity

### Opportunity
| | |
|---|---|
| **Class** | `Opportunity extends Activity` |
| **DTYPE** | `Opportunity` |
| **Key Fields** | `stage` (varchar 30, column `opportunity_stage`), `estimatedEmployees` (int), `estimatedValue` (double), `expectedCloseDate` (Date) |
| **Relationships** | `prospect` → M:1 Prospect (`prospect_id`), `agency` → M:1 Agency (`agency_id_opp`), `checkList` → 1:1 CheckList (`checklist_id`) |
| **Stages** | NEW, CONTACTED, QUALIFIED, PROPOSAL_SENT, NEGOTIATION, ON_HOLD, WON, LOST |
| **Notes** | Sales pipeline tracking for agents. WON/LOST auto-set `isComplete=true`. Uses same `assignee` table columns via SINGLE_TABLE inheritance. Column `agency_id_opp` avoids conflict with any existing `agency_id` usage. No DEFAULT on `opportunity_stage` column — default set in Java only. |

---

## model/activity/checklist — Checklists & Tasks

### CheckList
| | |
|---|---|
| **Class** | `CheckList extends Activity` |
| **Relationships** | `toDoList` → 1:M ToDo, `recurringTaskList` → M:1 RecurringTaskList (`recurring_list_id`), `setup` → 1:1 Setup (mapped by `checkList`), `renewal` → 1:1 Renewal (mapped by `checkList`), `ticket` → 1:1 Ticket (mapped by `checkList`) |
| **Notes** | Every Activity type (Ticket, Renewal, Setup, Opportunity) gets an associated CheckList. |

### CheckListOut
| | |
|---|---|
| **Class** | `CheckListOut` |
| **Notes** | DTO for checklist display. |

### CheckListShell
| | |
|---|---|
| **Class** | `CheckListShell` |
| **Notes** | Lightweight DTO for checklist list views. |

### RecurringItems
| | |
|---|---|
| **Class** | `RecurringItems` |
| **ID** | `id` (Long, column `recurring_id`) |
| **Key Fields** | `isInactive`, `daysInAdvance`, `dateOne`, `dateTwo`, `dateStart` |
| **Relationships** | `assignee` → M:1 Assignee, `taskSequence` → M:1 TaskSequence (`sequence_id`), `taskFrequency` → M:1 TaskFrequency (`frequency_id`), `doWList` → M:N DoW (join: `recurring_days`) |

### model/activity/checklist/tasks/

#### Task
| | |
|---|---|
| **Class** | `Task` |
| **Key Fields** | `id` (Long), `description`, `isReUsable`, `isSourced`, `allowNonOwner` |
| **Relationships** | `webLinkList` → M:N WebLink, `sourceOwner` → M:1 Person |
| **Notes** | Reusable task definitions. Task 153 = default auto-complete task (dummy workaround — see backlog T8). Sourcing flags (`isSourced`, `sourceOwner`, `allowNonOwner`) support third-party vendor outsourcing. |

#### ToDo
| | |
|---|---|
| **Class** | `ToDo` |
| **Key Fields** | `id`, `sortOrder`, `isComplete` |
| **Relationships** | `checkList` → M:1 CheckList, `task` → M:1 Task |

#### ToDoOut / ToDoOut25
| | |
|---|---|
| **Class** | `ToDoOut` / `ToDoOut25` |
| **Notes** | Display DTO for todo items with state logic (blocked, delegated, etc.) |

#### SortedTask
| | |
|---|---|
| **Class** | `SortedTask` |
| **Key Fields** | `task` (Task), `sortOrder` (int) |
| **Notes** | Non-entity helper for ordering tasks during checklist creation. |

---

## model/activity/checklist/sequences — Task Sequences

### TaskSequence
| | |
|---|---|
| **Class** | `TaskSequence` (abstract, `@Entity`, SINGLE_TABLE inheritance) |
| **ID** | `id` (Long) |
| **Key Fields** | `description` |
| **Relationships** | `taskSequenceTableList` → 1:M TaskSequenceTable |
| **Notes** | Base for all task sequence types. |

### RequiredTaskList
| | |
|---|---|
| **Class** | `RequiredTaskList extends TaskSequence` |
| **Relationships** | `serviceItem` → 1:1 TemplatePurpose (`purpose_id`) |
| **Notes** | Links a TemplatePurpose to a set of required tasks. Used for Setup, Ticket, and Opportunity checklists. |

### RecurringTaskList
| | |
|---|---|
| **Class** | `RecurringTaskList extends TaskSequence` |
| **Relationships** | `assignee` → M:1 Person (`user_id`), `taskFrequency` → M:1 TaskFrequency, `checkLists` → 1:M CheckList (mapped by `recurringTaskList`), `doWList` → M:N DoW (join: `days_of_week`) |
| **Key Fields** | `daysInAdvance`, `dateStart` |

### HowToList
| | |
|---|---|
| **Class** | `HowToList extends TaskSequence` |
| **Key Fields** | `uniqueId` |

---

## model/activity/checklist/sequences/support — Sequence Support

### TaskSequenceTable
| | |
|---|---|
| **Class** | `TaskSequenceTable` |
| **Relationships** | `taskSequence` → M:1 TaskSequence, `task` → M:1 Task |
| **Key Fields** | `sortOrder` |
| **Notes** | Join entity linking TaskSequence to Task with ordering. |

### TemplatePurpose
| | |
|---|---|
| **Class** | `TemplatePurpose` |
| **Key Fields** | `id` (int), `description`, `sortOrder` |
| **Relationships** | `activityCategory` → M:1 TemplateGroup |
| **Notes** | Defines specific service purposes (Health FSA, HRA, COBRA, HSA, Transit, POP, New Opportunity, etc.) tied to both billing groups and task templates. |

### TemplateGroup
| | |
|---|---|
| **Class** | `TemplateGroup` |
| **Key Fields** | `id` (int), `description` |
| **Notes** | Groups: 1=Renewal, 2=Setup, 3=Ticket, 5=Sales |

### TaskFrequency
| | |
|---|---|
| **Class** | `TaskFrequency` |
| **Key Fields** | `id` (int), `description` |
| **Notes** | Lookup: Daily, Weekly, Bi-Weekly, Monthly, various "first/last X of the month" patterns. |

### DoW (Day of Week)
| | |
|---|---|
| **Class** | `DoW` |
| **Key Fields** | `id`, `weekdayId`, `name` |
| **Relationships** | `recurringTaskListList` → M:N RecurringTaskList (mapped by `doWList`) |

---

## model/billing — Monthly Billing

### BillingMonth
| | |
|---|---|
| **Class** | `BillingMonth` |
| **Key Fields** | `monthId` (int), `fullDate` (Date) |
| **Notes** | Each month gets a billing cycle record. |

### BillingGroup
| | |
|---|---|
| **Class** | `BillingGroup` |
| **Key Fields** | `id` (int), `description` |
| **Notes** | Groups: FSA, HRA, COBRA, HSA, Transit, POP, MERP, LSA, Other, DUAL |

### BillingGrid
| | |
|---|---|
| **Class** | `BillingGrid` |
| **ID** | `gridId` (String, composite key: `date-eeId`) |
| **Key Fields** | `currentStatus`, boolean flags: `flexSpend`, `healthReimb`, `dualPlan`, `cobra`, `transit`, `hsa`, `lsa`, `retiree`, `direct` |
| **Relationships** | `employer` → M:1 Employer, `employee` → M:1 Employee, `billingMonth` → M:1 BillingMonth |

### BillingItem
| | |
|---|---|
| **Class** | `BillingItem` |
| **ID** | `billingId` (String) |
| **Key Fields** | `employerId`, `participantId`, `benefitGroupId`, `employerName`, `participantName`, `benefitGroup`, `haveCards` |
| **Relationships** | `billingMonth` → M:1 BillingMonth |

### BillingLink
| | |
|---|---|
| **Class** | `BillingLink` |
| **ID** | `billingId` (String) |
| **Key Fields** | `uniqueId` |
| **Relationships** | `billingMonth` → M:1 BillingMonth, `sEmployer` → M:1 sEmployer (`organization_id`), `employer` → M:1 Employer |
| **Notes** | Links Summit organizations to local Employer records for billing. |

### BillingSummary ⚡ VIEW-BACKED
| | |
|---|---|
| **Class** | `BillingSummary` |
| **Table** | `billing_summary` (DB view) |
| **Annotations** | `@ReadOnly` (EclipseLink) |
| **ID** | `uuid` (String) |
| **Key Fields** | totals for: cobra, direct, dualPlan, fsa, hra, lsa, hsa, retiree, transit |
| **Relationships** | `employer` → M:1 Employer, `billingMonth` → M:1 BillingMonth |
| **Notes** | READ-ONLY. Do not persist. |

### EmployerVariance
| | |
|---|---|
| **Class** | `EmployerVariance` |
| **Notes** | Non-entity DTO for billing variance comparison between months. |

---

## model/sales/agency — Agencies, Prospects & Invitations

### Agency
| | |
|---|---|
| **Class** | `Agency` |
| **ID** | `id` (Long, column `agency_id`) |
| **Key Fields** | `name`, `taxId`, `phone`, `email` |
| **Relationships** | `psp` → M:1 PSP, `agentList` → M:N Person (join: `agents`), `rateList` → M:N Rate (join: `agencyrates`), `address` → 1:1 Address, `manager` → M:1 Person (`manager_id`) |
| **Notes** | `manager` FK added for invitation system — identifies the Agency Manager. |

### Prospect
| | |
|---|---|
| **Class** | `Prospect` |
| **ID** | `id` (Long, column `prospect_id`) |
| **Key Fields** | `name` |
| **Relationships** | `agent` → M:1 Person, `proposalList` → 1:M Proposal, `contact` → 1:1 Person (the primary contact for this prospect) |

### Proposal
| | |
|---|---|
| **Class** | `Proposal` |
| **ID** | `id` (Long, column `proposal_id`) |
| **Key Fields** | `applicationGUID` (varchar 36, unique), `status` (CREATED/SENT/VIEWED/APPLIED/APPROVED/DENIED/EXPIRED), `dateSent`, `dateViewed`, `dateApplied` |
| **Relationships** | `prospect` → M:1 Prospect, `rate` → M:1 Rate, `losList` → M:N LOS (join: `proposalitems`), `application` → 1:1 Application (mapped by `proposal`), `createdBy` → M:1 Person, `sourceActivity` → M:1 Activity (`source_activity_id`) |

### Invitation
| | |
|---|---|
| **Class** | `Invitation` |
| **Table** | `invitation` |
| **ID** | `id` (Long, column `invitation_id`) |
| **Key Fields** | `guid` (varchar 36, unique), `email`, `firstName`, `lastName`, `role` (AGENCY_MANAGER or AGENT), `dateCreated`, `dateExpires`, `dateAccepted`, `isUsed` |
| **Relationships** | `agency` → M:1 Agency, `invitedBy` → M:1 Person (PSP user who sent), `person` → M:1 Person (Person record created for invitee) |
| **Notes** | 30-day expiration. GUID used in registration URL (`/AcceptInvite?guid=xxx`). |

---

## model/sales/offering — Service Catalog

### LOS (Line of Service)
| | |
|---|---|
| **Class** | `LOS` |
| **ID** | `id` (Long, column `los_id`) |
| **Key Fields** | `shortText`, `longText`, `sortOrder`, `suppressed` (boolean) |
| **Relationships** | `psp` → M:1 PSP, `moduleList` → M:N ServiceModule (join: `losmodules`), `enhancementList` → M:N Enhancement (inverse, mapped by `losList`) |
| **Values** | IDs 5–19: FSA, HRA, Transit, HSA, COBRA, POP, HRA(MERP), ICHRA, EBHRA, QSEHRA, Retiree, Direct Bill, LSA, Adoption Assistance |
| **Notes** | Implements `Comparable` by sortOrder. `suppressed` hides from UI without deleting. |

### Enhancement
| | |
|---|---|
| **Class** | `Enhancement` |
| **Table** | `enhancement` |
| **ID** | `id` (Long, column `enhancement_id`) |
| **Key Fields** | `name`, `description`, `sortOrder`, `suppressed` (boolean) |
| **Relationships** | `psp` → M:1 PSP, `losList` → M:N LOS (join: `enhancement_los`, owning side) |
| **Values** | Cards (Debit Cards), Payment (Payment Services), Docs (Document Services), Discounts (Volume Discounts) |
| **Notes** | Add-on services linked to eligible LOSs. Implements `Comparable` by sortOrder. |

### ServiceModule
| | |
|---|---|
| **Class** | `ServiceModule` |
| **ID** | `id` (Long, column `module_id`) |
| **Key Fields** | `shortText`, `longText`, `sortOrder`, `suppressed` |
| **Relationships** | `psp` → M:1 PSP, `losList` → M:N LOS (mapped by `moduleList`), `los` → M:1 LOS (nullable, direct FK `los_id`), `enhancement` → M:1 Enhancement (nullable, direct FK `enhancement_id`) |
| **Notes** | `los` and `enhancement` nullable FKs added for Service Manager — allows direct lookup of the module for a specific LOS or Enhancement without traversing the M:N join table. |

### Rate
| | |
|---|---|
| **Class** | `Rate` |
| **ID** | `id` (Long, column `rate_id`) |
| **Key Fields** | `description` |
| **Relationships** | `psp` → M:1 PSP, `rateTableList` → 1:M RateTable, `agencyList` → M:N Agency (mapped by `rateList`) |

### RateTable
| | |
|---|---|
| **Class** | `RateTable` |
| **ID** | `RateTableID` (Embeddable composite: `rate_id` + `los_id`) |
| **Relationships** | `rate` → M:1 Rate, `los` → M:1 LOS, `feeTypeList` → 1:M FeeType |

### FeeType
| | |
|---|---|
| **Class** | `FeeType` |
| **ID** | `id` (Long, column `fee_type_id`) |
| **Key Fields** | `description`, `sortOrder`, `suppressed` |
| **Relationships** | `psp` → M:1 PSP |

### PriceItem
| | |
|---|---|
| **Class** | `PriceItem` |
| **Relationships** | `rateTable` → M:1 RateTable, `feeType` → M:1 FeeType |
| **Key Fields** | `price` (double) |

### RateDiscount
| | |
|---|---|
| **Class** | `RateDiscount` |
| **Key Fields** | `description`, `discountAmount` |
| **Relationships** | `rate` → M:1 Rate, `priceItem` → M:1 PriceItem, `losList` → M:N LOS (join: `ratediscountlos`) |

### Feature
| | |
|---|---|
| **Class** | `Feature` |
| **ID** | `id` (Long, column `feature_id`) |
| **Key Fields** | `description` (varchar 500), `sortOrder` |
| **Relationships** | `module` → M:1 ServiceModule, `psp` → M:1 PSP, `libraryResource` → M:1 MarketingMaterial (`material_id`, nullable) |
| **Notes** | Description text supports inline markdown-style links `[text](resourceId)` which are rendered as HTML links in proposals. `libraryResource` is an optional icon link displayed at end of text. |

### MarketingMaterial
| | |
|---|---|
| **Class** | `MarketingMaterial` |
| **Table** | `marketingmaterial` |
| **ID** | `id` (Long, column `material_id`) |
| **Key Fields** | `title`, `description`, `materialType` (DOCUMENT/VIDEO/LINK), `url`, `storageGuid` (varchar 50, UUID.extension), `audience`, `sortOrder` |
| **Relationships** | `psp` → M:1 PSP, `category` → M:1 ResourceCategory (`category_id`, nullable) |
| **Notes** | Files stored in Wasabi S3 (`ams-file-storage/{psp-slug}/UUID.ext`). storageGuid widened from VARCHAR(36) to VARCHAR(50) to accommodate extension. |

### ResourceCategory
| | |
|---|---|
| **Class** | `ResourceCategory` |
| **Table** | `resourcecategory` |
| **ID** | `id` (Long, column `category_id`) |
| **Key Fields** | `name`, `sortOrder` |
| **Relationships** | `psp` → M:1 PSP |
| **Notes** | Organizes MarketingMaterial entries in the Resource Library UI. |

### IrsLimit
| | |
|---|---|
| **Class** | `IrsLimit` |
| **Table** | `irslimit` |
| **Key Fields** | `year`, `limitType`, `amount` |
| **Notes** | IRS contribution limits by year (FSA, DCAP, HSA, Transit, etc.). Used in application form for dynamic limit display. |

### BenefitType
| | |
|---|---|
| **Class** | `BenefitType` |
| **Table** | `benefittype` |
| **Key Fields** | `id`, `name`, `description` |
| **Notes** | 11 benefit categories used in application plan design section. |

### BillingType
| | |
|---|---|
| **Class** | `BillingType` |
| **Table** | `billingtype` |
| **Key Fields** | `id`, `name`, `description` |
| **Notes** | 5 rate structures (PEPM, Flat, Tiered, Per Claim, Percentage). |

---

## model/sales/application — Applications

### Application
| | |
|---|---|
| **Class** | `Application` |
| **ID** | `proposal` (Proposal, `@OneToOne @Id`, column `proposal_id`) — uses Proposal as PK |
| **Key Fields** | `status` (IN_PROGRESS/SUBMITTED/UNDER_REVIEW/APPROVED/DENIED/MORE_INFO), `dateStarted`, `dateSubmitted`, `dateReviewed`, `reviewNotes` |
| **Relationships** | `proposal` → 1:1 Proposal (PK), `reviewedBy` → M:1 Person, `fieldValues` → 1:M ApplicationFieldValue, `applicationModuleList` → 1:M ApplicationModule, `setup` → 1:1 Setup (mapped by `application`) |

### ApplicationSection
| | |
|---|---|
| **Class** | `ApplicationSection` |
| **ID** | `id` (Long, column `section_id`) |
| **Key Fields** | `name`, `description`, `scope` (ALL or LOS), `sortOrder` |
| **Relationships** | `psp` → M:1 PSP, `losList` → M:N LOS (join: `applicationsectionlos`), `fieldList` → 1:M ApplicationField (ordered by sortOrder), `enhancementList` → M:N Enhancement (join: `applicationsectionenhancement`) |
| **Notes** | 20 seeded sections: Company Info, Contact, Address, Plan Year, Pay Cycle, Signing Officer, Bank, Pre-Tax, 125 Features, FSA, HRA Design, HRA Carryover, HSA Funding, Transit, Billing, Payment, Debit Cards, LSA, Adoption, COBRA-Specific. |

### ApplicationField
| | |
|---|---|
| **Class** | `ApplicationField` |
| **ID** | `fieldKey` (String PK, column `field_key`, varchar 100) |
| **Key Fields** | `label`, `helpText`, `fieldType` (TEXT/TEXTAREA/NUMBER/DATE/SELECT/RADIO/BOOLEAN/CHECKBOX/JSON), `isRequired`, `sortOrder`, `selectOptions` (pipe-delimited for SELECT/RADIO/CHECKBOX) |
| **Relationships** | `applicationSection` → M:1 ApplicationSection |
| **Notes** | ~95 fields across all sections. |

### ApplicationFieldValue
| | |
|---|---|
| **Class** | `ApplicationFieldValue` |
| **ID** | `id` (Long, column `field_value_id`) |
| **Relationships** | `application` → M:1 Application, `applicationField` → M:1 ApplicationField |
| **Key Fields** | `fieldValue` (TEXT) |

### ApplicationModule
| | |
|---|---|
| **Class** | `ApplicationModule` |
| **ID** | `ApplicationModuleID` (Embeddable composite: `application_id` + `template_purpose_id`) |
| **Notes** | Links application to checklist templates for Setup creation. |

---

## model/summit/archive — Employer/Employee Archive

These are the **primary local data tables**. They are populated and kept in sync by the import pipeline (`Importer` → staging tables → `Updater` → archive).

### Employer
| | |
|---|---|
| **Class** | `Employer` |
| **ID** | `id` (int, column `employer_id`) — **not auto-generated** (matches Summit's Organization ID) |
| **Key Fields** | `employerName`, `email`, `contactName`, `phone`, `erKey`, `altId`, `isActive`, `isBillable`, boolean flags: `hasPop`, `hasPb`, `hasCdh` |
| **Relationships** | `contactList` → M:N Employee |
| **Notes** | Each Employer = one of SSA's clients. Synced from Summit via ImportEmployer staging table. |

### Employee
| | |
|---|---|
| **Class** | `Employee` |
| **ID** | `id` (int, column `employee_id`) — **not auto-generated** |
| **Key Fields** | `firstName`, `lastName`, `mmKey`, `email`, `hrEmail`, address fields, `userId`, `isActive`, `customId`, `eeStatusId`, `systemStatusId`, `cobraStatusId` |
| **Relationships** | `employer` → M:1 Employer (`employer_id`), `employerList` → M:N Employer (mapped by `contactList`) |
| **Notes** | Represents a benefit participant or potential participant from Summit. Negative IDs = HSA-only employees (not in Summit). Every Employee can be linked to a Person, but not every Person is an Employee. |

### Benefit
| | |
|---|---|
| **Class** | `Benefit` |
| **ID** | `id` (int, column `benefit_id`) |
| **Key Fields** | `planName`, `planDescription`, `effectiveDate`, `terminationDate`, `isActive`, `hasCards`, `lastRenewed`, `nextRenewalDue`, `pbBenId` |
| **Relationships** | `employer` → M:1 Employer, `planType` → M:1 PlanType, `renewalItemList` → 1:M RenewalItem |
| **Notes** | Unified benefit table combining Summit's CDH and Premium Billing exports. **Positive IDs** = CDH benefits. **Negative IDs** = Premium Billing benefits — negated during import to prevent ID collision. |

### PlanType
| | |
|---|---|
| **Class** | `PlanType` |
| **ID** | `planTypeId` (int, column `PlanType_ID`) |
| **Key Fields** | `code`, `planTypeName` |
| **Relationships** | `billingGroup` → M:1 BillingGroup, `serviceItem` → M:1 TemplatePurpose (`purpose_id`) |
| **Notes** | Maps Summit's plan type codes to billing groups and template purposes. CDH types: DCA, FSA, HRA, HSA, LFSA, MERP, PRA, ICHRA, EBHRA, DRiP, LSA. Premium Billing types: PRK, TRN, Dental, EAP, Life, Medical, Pharmacy, Vision, NEFSA. |

### CoverageStatus
| | |
|---|---|
| **Class** | `CoverageStatus` |
| **ID** | `id` (String, column `coverage_status_id`) |
| **Key Fields** | `monthFor`, `isActive`, `hasCards` |
| **Relationships** | `employer` → M:1 Employer, `employee` → M:1 Employee, `benefit` → M:1 Benefit, `billingGroup` → M:1 BillingGroup |

---

## model/summit/imports — Summit Import (Live) ⚠️ LEGACY

These `s`-prefix entities were the **original** mappings to Summit's live data. They have been **largely replaced** by the `Import*` staging entities in `model/summit/imports/order/`. However, they cannot be deleted yet because the following entities still hold FK references to them:

- `BillingLink` → references `sEmployer`
- `Enrollment` (in `model/summit/temp/`) → references `sEmployee`, `sEmployer`, `sBenefit`, `sBenefitYear`
- `sEnrollment` → references `sEmployee`, `sBenefitYear`

**Future cleanup:** These dependencies should be migrated to use `Import*` entities or the archive tables, after which the `s`-prefix entities can be removed.

### sEmployer
| | |
|---|---|
| **Class** | `sEmployer` |
| **ID** | `organizationId` (int) |

### sEmployee
| | |
|---|---|
| **Class** | `sEmployee` |
| **ID** | `id` (int, column `Participant_ID`) |
| **Relationships** | `sEmployer` → M:1 sEmployer (`Organization_ID`) |

### sBenefit
| | |
|---|---|
| **Class** | `sBenefit` |
| **ID** | `benefitId` (int, column `EmployerPlan_ID`) |
| **Relationships** | `sEmployer` → M:1 sEmployer (`OrganizationID`), `PlanType` → M:1 PlanType (`PlanTypeID`) |

### sBenefitYear
| | |
|---|---|
| **Class** | `sBenefitYear` |
| **Notes** | Summit benefit year (plan year) data. |

### HsaAccount
| | |
|---|---|
| **Class** | `HsaAccount` |
| **Key Fields** | `hsaId` (int), `firstName`, `lastName`, `employer` (String name), `active` |

---

## model/summit/imports/order — Import Staging Tables

Nine staging tables used by the monthly import pipeline. CSVs from Summit are imported into these tables, then the `Updater` service syncs them into the archive tables.

| Entity | Table | Purpose |
|--------|-------|---------|
| `ImportEmployer` | `import1employer` | Employer/organization data |
| `ImportEmployee` | `import2employee` | Employee/participant data |
| `ImportBenefit` | `import3benefit` | CDH benefit plans |
| `ImportBenefitYear` | `import4benefityear` | Benefit plan years |
| `ImportEnrollment` | `import5enrollment` | CDH enrollment records |
| `ImportPremiumBenefit` | `import6premiumbenefit` | Premium billing benefits (COBRA, Direct, Retiree) |
| `ImportPremiumEnrollment` | `import7premiumenrollment` | Premium billing enrollments |
| `ImportCobraQualify` | `import8cobraqualify` | COBRA qualifying events |
| `ImportCobraParticipant` | `import9cobrapart` | COBRA participant details |

---

## model/summit/temp — Temp/Transactional

### Enrollment
| | |
|---|---|
| **Class** | `Enrollment` |
| **Notes** | Transactional enrollment data referencing `sEmployee`, `sEmployer`, `sBenefit`, `sBenefitYear`. |

### HsaEr / HsaEe
| | |
|---|---|
| **Class** | `HsaEr`, `HsaEe` |
| **Notes** | HSA employer and employee entities used in HSA billing. `HsaEr` links to `Employer` via `organization_id`. `HsaEe` links to `HsaEr` and optionally to `Employee`. |

---

## model (root) — View-Backed DTOs & Constants

### Activity25 / Activity25p / Activity25u ⚡ VIEW-BACKED
| | |
|---|---|
| **Notes** | View-backed read-only activity DTOs. `Activity25` is the primary display view. `Activity25p` and `Activity25u` are variant views. |

### Checklist25 ⚡ VIEW-BACKED
| | |
|---|---|
| **Notes** | View-backed read-only DTO for checklist display. |

### EmployeeV ⚡ VIEW-BACKED
| | |
|---|---|
| **Notes** | View-backed read-only employee view. |

### PersonV ⚡ VIEW-BACKED
| | |
|---|---|
| **Notes** | View-backed read-only person view. |

> **⚡ VIEW-BACKED entities are READ-ONLY.** They map to MySQL views, not tables. Never call `persist()` or `merge()` on them.

### Constant
| | |
|---|---|
| **Class** | `Constant` |
| **Key Fields** | `name` (PK), `value`, `note` |
| **Notes** | Application-wide configuration stored in DB. Includes SMTP settings, S3/Wasabi credentials, email colors, API keys. Queried via `AppConstantDAO.getConstantValue()`. |

---

## Non-Entity Support Classes

These are DTOs or helper classes that appear in the model layer but are **not** JPA entities:

| Class | Package | Purpose |
|-------|---------|---------|
| `ActivityOut` | `model/activity/` | Activity display wrapper |
| `ActivityShell` | `model/activity/` | Lightweight activity list item |
| `CheckListOut` | `model/activity/checklist/` | Checklist display wrapper |
| `CheckListShell` | `model/activity/checklist/` | Lightweight checklist list item |
| `ToDoOut` / `ToDoOut25` | `model/activity/checklist/tasks/` | ToDo display with state logic |
| `SortedTask` | `model/activity/checklist/tasks/` | Task + sort order pair |
| `GenSeq` | `model/activity/checklist/sequences/` | Sequence builder session DTO |
| `ReqTaskListTix` | `model/` | DTO wrapping RequiredTaskList + TicketSubCategory for display |
| `RenewalEmployer` | `model/activity/renewal/` | Employer renewal list item |
| `EmployerVariance` | `model/billing/` | Billing variance comparison |
| `RateTableID` | `model/sales/agency/` | `@Embeddable` composite key |
| `ApplicationModuleID` | `model/sales/application/` | `@Embeddable` composite key |

---

## Join Tables (No Entity Class)

These M:N relationships are managed via `@JoinTable` — no separate entity class exists:

| Join Table | Owning Entity | Target Entity | Columns |
|------------|--------------|---------------|---------|
| `assignee_links` | Assignee | WebLink | `assignee_id`, `link_id` |
| `assignee_contacts` | Assignee | Person | `assignee_id`, `person_id` |
| `email_recipents` | Person | Email | `recipient_id`, `email_id` |
| `userinroles` | User | UserRole | `person_id`, `role_id` |
| `agents` | Agency | Person | `agency_id`, `person_id` |
| `agencyrates` | Agency | Rate | `agency_id`, `rate_id` |
| `proposalitems` | Proposal | LOS | `proposal_id`, `los_id` |
| `losmodules` | LOS | ServiceModule | `los_id`, `module_id` |
| `enhancement_los` | Enhancement | LOS | `enhancement_id`, `los_id` |
| `applicationsectionlos` | ApplicationSection | LOS | `section_id`, `los_id` |
| `applicationsectionenhancement` | ApplicationSection | Enhancement | `section_id`, `enhancement_id` |
| `ratediscountlos` | RateDiscount | LOS | `ratediscount_id`, `los_id` |
| `days_of_week` | RecurringTaskList | DoW | `item_id`, `dow_id` |
| `recurring_days` | RecurringItems | DoW | `item_id`, `dow_id` |

---

## Quick Lookup Index

| "I need to find..." | Look at... |
|---------------------|-----------|
| What table does Person map to? | `assignee` (single-table inheritance) |
| Relationship between Ticket and CheckList? | Ticket has `@OneToOne checkList` (column `checklist_id`) |
| What are the Activity subtypes? | CheckList, Renewal, Ticket, Setup, Opportunity — all extend Activity which extends Assignee |
| Which entities are read-only views? | Activity25, Activity25p, Activity25u, Checklist25, EmployeeV, PersonV, BillingSummary |
| Where do Summit CSV imports go? | `model/summit/imports/order/` — tables `import1employer` through `import9cobrapart` |
| Why are some Benefit IDs negative? | Premium Billing benefits (COBRA, Direct Bill, Retiree) have IDs negated to avoid collision with CDH benefit IDs |
| What links Summit to local data? | `Updater` service syncs Import staging tables → archive tables (Employer, Employee, Benefit) |
| What's the task template chain? | TemplateGroup → TemplatePurpose → RequiredTaskList → TaskSequenceTable → Task |
| How does billing work? | BillingMonth → BillingGrid (per employee per month) with boolean flags per service type |
| What's the sales pipeline chain? | Agency → Rate → Proposal → Application → Setup (with LOS/Enhancement/Feature/MarketingMaterial for content) |
| What's the difference between Person and Employee? | Every Employee is a Person, but Person also covers spouses, prospects, agents, contacts |
| What are the `s`-prefix entities? | Legacy Summit mappings — largely replaced by `Import*` entities, but still referenced by some billing entities |
| What is Summit? | DataPath Inc.'s cloud-based benefits administration platform — SSA's primary system |
| How do Opportunities work? | Opportunity extends Activity, tied to Prospect + Agency. Stages: NEW→CONTACTED→QUALIFIED→PROPOSAL_SENT→NEGOTIATION→WON/LOST |
| How do invitations work? | Invitation entity with GUID link, 30-day expiry. PSP sends → agent registers → User created |
