# Sales Pipeline — Reference Document

**Last Updated:** February 20, 2026 (Session 4 — complete)
**Supersedes:** `sales_pipeline_data_model.md`, `sales_pipeline_implementation_log.md`, `sales_pipeline_session3_log.md` (no longer present in repo)

---

## Overview

The sales pipeline handles the full lifecycle from prospect identification through application approval and setup creation. The flow is: Agent creates a Proposal for a Prospect → Proposal is emailed → Prospect views proposal → Prospect fills out Application → PSP reviews and approves → Setup activity is created in AMS.

All sales entities live under `model/sales/` with sub-packages `agency/`, `application/`, and `offering/`.

---

## Pipeline Status

| Step | Status | Servlet / JSP |
|------|--------|---------------|
| Proposal Builder (agent creates proposal) | ✅ Done | `ProposalBuilder` → `proposalBuilder.jsp` |
| Proposal Detail (internal view) | ✅ Done | `ProposalDetail` → `proposalDetail.jsp` |
| Send Proposal (email with link) | ✅ Done | `SendProposal` |
| Proposal Landing Page (prospect views) | ✅ Done | `ViewProposal` → `viewProposal.jsp` |
| Application Form (prospect fills out) | ✅ Done | `ApplyForProposal` GET → `applyForProposal.jsp` |
| Save Progress (auto-save + manual) | ✅ Done | `SaveApplicationProgress` (AJAX) |
| Rate Sheet Upload | ✅ Done | `UploadRateSheet` (AJAX → Wasabi) |
| Submit Application | ✅ Done | `ApplyForProposal` POST → `applicationConfirmation.jsp` |
| "Apply Now" button on proposal page | ✅ Done | Link in `viewProposal.jsp` |
| Application Review List (internal) | ✅ Done | `ReviewApplications` → `reviewApplications.jsp` |
| Application Review Detail (internal) | ✅ Done | `ReviewApplication` → `reviewApplication.jsp` |
| Approve/Deny/More Info actions | ✅ Done | `ReviewApplication` POST |
| Automated Setup creation on approval | ✅ Done | `ReviewApplication` POST (approve action) |
| Manual Setup form | ✅ Done | `GenerateProp25` GET → `manualSetup.jsp` |
| Proposal → Review link | ✅ Done | Action buttons in `proposalDetail.jsp` |
| Full pipeline test | ✅ Done | Create → Send → View → Apply → Save → Submit → Review → Approve |
| Production DB migration | ❌ Not done | 3 scripts pending |

---

## Lifecycle States

### Proposal Status

| Status | Meaning | Set By |
|--------|---------|--------|
| CREATED | Proposal built, not yet sent | `ProposalBuilder` POST |
| SENT | Email sent to prospect | `SendProposal` |
| VIEWED | Prospect opened the landing page | `ViewProposal` GET |
| APPLIED | Prospect submitted application | `ApplyForProposal` POST |
| APPROVED | PSP approved the application | `ReviewApplication` POST |
| DENIED | PSP denied the application | `ReviewApplication` POST |
| EXPIRED | Aged out or manually closed | (not yet built) |

### Application Status

| Status | Meaning | Set By |
|--------|---------|--------|
| IN_PROGRESS | Prospect opened form, may have saved progress | `ApplyForProposal` GET / `SaveApplicationProgress` |
| SUBMITTED | Prospect clicked Submit | `ApplyForProposal` POST |
| UNDER_REVIEW | PSP is reviewing | `ReviewApplication` POST |
| APPROVED | PSP approved | `ReviewApplication` POST |
| DENIED | PSP denied | `ReviewApplication` POST |
| MORE_INFO | PSP requested additional info | `ReviewApplication` POST |

---

## Servlets

| Servlet | Package | URL | Auth | Purpose |
|---------|---------|-----|------|---------|
| ProposalBuilder | controller/activity/setup | `/ProposalBuilder` | Yes | Agent creates proposal (GET=form, POST=create) |
| ProposalDetail | controller/activity/setup | `/ProposalDetail` | Yes | Internal proposal detail view |
| CreateProspect | controller/activity/setup | `/CreateProspect` | Yes | Creates prospect from modal in ProposalBuilder |
| SendProposal | controller/activity/setup | `/SendProposal` | Yes | Emails proposal link to prospect |
| ViewProposal | controller/activity/setup | `/viewProposal/*` | **No** | Public proposal landing page (GUID) |
| ApplyForProposal | controller/activity/setup | `/apply/*` | **No** | Public application form (GET) + submit (POST) |
| SaveApplicationProgress | controller/activity/setup | `/saveApplication` | **No** | AJAX auto-save field values |
| UploadRateSheet | controller/activity/setup | `/uploadRateSheet` | **No** | AJAX file upload to Wasabi |
| ReviewApplications | controller/activity/setup | `/ReviewApplications` | Yes | Application review list with multi-status filtering |
| ReviewApplication | controller/activity/setup | `/ReviewApplication` | Yes | Application review detail + approve/deny/more-info actions |
| GenerateProp25 | controller/activity/setup | `/GenerateProp25` | Yes | Manual setup creation (GET=form if no params, POST=create) |

Public endpoints (`/apply/*`, `/saveApplication`, `/uploadRateSheet`, `/viewProposal/*`) are whitelisted in `LoginFilter`.

---

## JSPs

| JSP | Path | Access | Purpose |
|-----|------|--------|---------|
| proposalBuilder.jsp | `WEB-INF/view/sales/` | Internal | 3-step proposal creation form |
| proposalDetail.jsp | `WEB-INF/view/sales/` | Internal | Proposal detail with status, pricing, GUID link, review link |
| viewProposal.jsp | `WEB-INF/view/sales/` | Public | Prospect-facing proposal with features, pricing, "Apply Now" |
| applyForProposal.jsp | `WEB-INF/view/sales/` | Public | Dynamic application form with conditional sections |
| applicationConfirmation.jsp | `WEB-INF/view/sales/` | Public | Post-submit confirmation page |
| reviewApplications.jsp | `WEB-INF/view/sales/` | Internal | Application review list with multi-select status filter |
| reviewApplication.jsp | `WEB-INF/view/sales/` | Internal | Application review detail with field rendering and action panel |
| manualSetup.jsp | `WEB-INF/view/sales/` | Internal | Manual setup creation form (no application required) |

---

## Entity Reference — Current State

### Proposal (`model/sales/agency`)
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| id | Long PK | `proposal_id` | Auto-generated |
| prospect | M:1 Prospect | `prospect_id` | |
| rate | M:1 Rate | `rate_id` | |
| dateCreated | Timestamp | `date_created` | Auto, not insertable/updatable |
| isInactive | boolean | `is_inactive` | Soft delete |
| applicationGUID | String | `application_guid` varchar(36) | Public access key |
| status | String | `status` varchar(20) | Default 'CREATED' |
| createdBy | M:1 Person | `created_by` | Agent or PSP user |
| dateSent | Timestamp | `date_sent` | Nullable |
| dateViewed | Timestamp | `date_viewed` | Nullable |
| dateApplied | Timestamp | `date_applied` | Nullable |
| sourceActivity | M:1 Activity | `source_activity_id` | Optional link to originating ticket |
| losList | M:N LOS | join: `proposalitems` | Selected Lines of Service |
| application | 1:1 Application | mappedBy `proposal` | |

### Application (`model/sales/application`)
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| proposal | 1:1 Proposal (PK) | `proposal_id` | PK = FK to Proposal |
| status | String | `status` varchar(20) | Default 'IN_PROGRESS' |
| dateStarted | Timestamp | `date_started` | |
| dateSubmitted | Timestamp | `date_submitted` | |
| dateReviewed | Timestamp | `date_reviewed` | |
| reviewedBy | M:1 Person | `reviewed_by` | PSP user |
| reviewNotes | String | `review_notes` TEXT | |
| fieldValues | 1:M ApplicationFieldValue | mappedBy `application` | |
| applicationModuleList | 1:M ApplicationModule | mappedBy `application` | |
| setup | 1:1 Setup | mappedBy `application` | Created on approval |

### ApplicationSection (`model/sales/application`)
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| id | Long PK | `section_id` | Auto-generated |
| name | String | `name` varchar(100) | |
| description | String | `description` varchar(500) | |
| scope | String | `scope` varchar(10) | 'ALL' or 'LOS' |
| sortOrder | int | `sort_order` | |
| psp | M:1 PSP | `psp_id` | |
| losList | M:N LOS | join: `applicationsectionlos` | Which LOSs trigger this section |
| fieldList | 1:M ApplicationField | mappedBy, ordered by sortOrder | |

20 sections seeded: Company Info, Contact, Address, Plan Year, Pay Cycle, Signing Officer, Bank, Pre-Tax, 125 Features, FSA, HRA Design, HRA Carryover, HSA Funding, Transit, Billing, Payment, Debit Cards, LSA, Adoption, COBRA-Specific.

### ApplicationField (`model/sales/application`)
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| fieldKey | String PK | `field_key` varchar(100) | Internal key (was DataKey.keyName) |
| label | String | `label` varchar(200) | Display label |
| applicationSection | M:1 ApplicationSection | `section_id` | |
| helpText | String | `help_text` varchar(500) | |
| fieldType | String | `field_type` varchar(20) | TEXT, TEXTAREA, NUMBER, DATE, SELECT, RADIO, BOOLEAN, CHECKBOX, JSON |
| isRequired | boolean | `is_required` | |
| sortOrder | int | `sort_order` | |
| selectOptions | String | `select_options` varchar(500) | Pipe-delimited for SELECT/RADIO/CHECKBOX |

~95 fields across all sections.

### ApplicationFieldValue (`model/sales/application`)
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| id | Long PK | `field_value_id` | Auto-generated |
| application | M:1 Application | `application_id` | |
| applicationField | M:1 ApplicationField | `field_key` | |
| fieldValue | String | `field_value` TEXT | |

### ApplicationModule (`model/sales/application`)
Composite PK: `(application_id, template_purpose_id)`. Links application to checklist templates for Setup creation.

### Feature (`model/sales/offering`)
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| id | Long PK | `feature_id` | Auto-generated |
| serviceModule | M:1 ServiceModule | `module_id` | |
| description | String | `description` varchar(500) | Feature text |
| sortOrder | int | `sort_order` | |
| psp | M:1 PSP | `psp_id` | |

### RateDiscount (`model/sales/agency`)
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| id | Long PK | `ratediscount_id` | Auto-generated |
| rate | M:1 Rate | `rate_id` | |
| description | String | `description` varchar(200) | |
| discountAmount | double | `discount_amount` | Dollar amount off |
| priceItem | M:1 PriceItem | `price_item_id` | Which fee type |
| requiredLosList | M:N LOS | join: `ratediscountlos` | ALL listed LOSs must be in proposal to trigger |

### MarketingMaterial (`model/sales/offering`)
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| id | Long PK | `material_id` | Auto-generated |
| title | String | `title` varchar(200) | |
| description | String | `description` varchar(500) | |
| materialType | String | `material_type` varchar(20) | PDF, VIDEO, LINK |
| url | String | `url` varchar(500) | For VIDEO/LINK |
| storageGuid | String | `storage_guid` varchar(36) | Wasabi key for PDFs |
| audience | String | `audience` varchar(20) | PROSPECT or CLIENT |
| sortOrder | int | `sort_order` | |
| psp | M:1 PSP | `psp_id` | |
| serviceModuleList | M:N ServiceModule | join: `materialmodule` | |

### BenefitType (`model/sales/offering`)
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| id | Long PK | `benefittype_id` | Auto-generated |
| name | String | `name` varchar(100) | e.g., Medical, Dental, Vision |
| defaultBillingType | M:1 BillingType | `default_billingtype_id` | |
| psp | M:1 PSP | `psp_id` | |
| sortOrder | int | `sort_order` | |

11 benefit categories seeded.

### BillingType (`model/sales/offering`)
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| id | Long PK | `billingtype_id` | Auto-generated |
| name | String | `name` varchar(100) | e.g., Tiered, Age-Rated, Flat |
| psp | M:1 PSP | `psp_id` | |
| sortOrder | int | `sort_order` | |

5 rate structure types seeded.

### IrsLimit (`model/general`)
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| limitKey | String (PK) | `limit_key` varchar(50) | e.g., FSA_HEALTH_MAX, HSA_SINGLE |
| planYear | int (PK) | `plan_year` | Composite PK with limitKey |
| amount | double | `amount` | Dollar amount |
| description | String | `description` varchar(200) | |

2025 + 2026 limits seeded (11 limit types × 2 years). Loaded via `SalesDAO.getIrsLimits()`.

---

## Pre-existing Entities (Unchanged)

These entities existed before the pipeline work and were not modified:

- **Agency** — `agency_id`, name, taxId, phone, psp, address, primaryContact, agencyRateList (M:N Rate), agentList (M:N Person)
- **Prospect** — `prospect_id`, name, contact (Person), address, agent (Person), proposalList (1:M Proposal)
- **Rate** — `rate_id`, description, isSuppressed, psp
- **RateTable** — composite PK (rate_id, price_item_id, module_id), price. Links Rate + PriceItem + ServiceModule.
- **PriceItem** — `priceitem_id`, description, psp
- **ServiceItem** — `serviceitem_id`, description, psp
- **LOS** — `los_id`, description, shortText, psp, serviceModuleList (M:N), proposalList (M:N)
- **ServiceModule** — `module_id`, description, shortText, sortOrder, psp

---

## Lines of Service

| ID | Short | Description |
|----|-------|-------------|
| 5 | POP | Premium Only Plan |
| 6 | FSA | Flexible Spending Accounts |
| 8 | COBRA | COBRA Administration |
| 9 | HSA | Health Savings Accounts |
| 10 | TRANSIT | Commuter / Transit Benefits |
| 11 | HRA | Health Reimbursement Arrangement |
| 12 | MERP | Medical Expense Reimbursement Plan |
| 13 | ICHRA | Individual Coverage HRA |
| 14 | EBHRA | Excepted Benefit HRA |
| 15 | QSEHRA | Qualified Small Employer HRA |
| 16 | RETIREE | Retiree Billing |
| 17 | DIRECT | Direct Billing |
| 18 | LSA | Lifestyle Spending Account |
| 19 | ADOPTION | Adoption Assistance |

LOS 7 (old HRA/MERP) was deleted and replaced by 11-15 in Session 3.

---

## Application Form Details

### Dynamic Section Assembly
Sections load based on proposal's LOS list. A JPQL query fetches sections where `scope='ALL'` or where `applicationsectionlos` overlaps with the proposal's LOSs. Fields within sections are sorted in Java after the query to work around an EclipseLink DISTINCT + JOIN FETCH ordering issue (the `@OrderBy` annotation is unreliable in this scenario).

### Conditional Show/Hide
19 JavaScript rules control field visibility based on other field values. When a trigger field's parent section is hidden (LOS not selected), dependent fields are also hidden. Hidden fields are cleared on hide.

### Benefit Plan Builder
The Billing section includes a JSON-based plan builder for defining benefit plans. Each plan has: planName, benefitTypeId, billingTypeId, effectiveDate, renewalDate, tiers (or flat rate), notes, and optional rate sheet upload. Plans are serialized to a hidden `bill_benefit_plans` field as JSON.

### Rate Sheet Uploads
`UploadRateSheet` servlet accepts multipart file uploads, stores them in Wasabi under `applications/{proposalId}/{uuid}/{filename}` via `StorageDAO`, and returns a JSON response with the storage key.

### Save & Restore
`SaveApplicationProgress` saves all field values via AJAX (manual button + auto-save every 60 seconds when dirty). On page reload, `ApplyForProposal` GET queries saved `ApplicationFieldValue` rows and merges them into the defaults map. JSON values use HTML entity escaping for safe attribute embedding. The proposal query must include `LEFT JOIN FETCH p.application` to avoid duplicate Application creation.

### IRS Limits
`SalesDAO.getIrsLimits()` loads limits for the current plan year (falls back to most recent year). Limits display as read-only reference values in relevant sections (FSA, HSA, Transit, Adoption, QSEHRA).

---

## Application Review Details

### Review List (`ReviewApplications`)
- Defaults to showing only SUBMITTED applications
- Multi-select status filter: click individual status buttons to toggle them on/off
- Clicking ALL selects all statuses; clicking any individual status deselects ALL
- If all 5 individual statuses are selected, treated as ALL
- Deselecting last status falls back to SUBMITTED
- Servlet accepts `?status=SUBMITTED,UNDER_REVIEW` (comma-separated) or `?status=ALL`
- Flash messages via `msg` and `err` query params from redirect after actions

### Review Detail (`ReviewApplication`)
- GET loads application with all field values joined to ApplicationField and ApplicationSection
- Builds `valueMap` (fieldKey → value) for JSP rendering
- Loads sections scoped to proposal's LOS list (same query pattern as `ApplyForProposal`)
- Extracts `storageKey` values from `bill_benefit_plans` JSON and generates pre-signed Wasabi download URLs (1-hour expiry) via `StorageDAO.getDownloadUrl()`
- Two-column layout: application data left, sticky action panel right
- Field rendering by type: BOOLEAN → Yes/No icons, CHECKBOX → pipe-split badges, TEXTAREA → pre-wrapped, JSON (benefit plans) → JavaScript-rendered cards with tier tables and download buttons

### Review Actions (POST)
| Action | Application Status | Proposal Status | Additional |
|--------|-------------------|-----------------|------------|
| `under_review` | UNDER_REVIEW | (unchanged) | Redirects back to detail |
| `approve` | APPROVED | APPROVED | Creates Setup + CheckList + ToDos, updates activity cache |
| `deny` | DENIED | DENIED | Sets reviewedBy, dateReviewed, reviewNotes |
| `more_info` | MORE_INFO | (unchanged) | Sets reviewNotes |

### Setup Creation on Approval
Mirrors `GenerateProp25.createSetup()` / `createChecklist()` / `fillToDoList()`:

1. Create CheckList with dummy task 153 (pre-completed, sortOrder 0)
2. Create Setup linked to Application, Prospect contact, current user
3. Link CheckList back to Setup
4. Fill ToDo list from `ApplicationTaskDAO.getTasksRequiredForApplication()` (walks `ApplicationModule` → `TemplatePurpose` → `RequiredTaskList`)
5. If no tasks found, inserts task 153 as fallback (see backlog item T8 for planned cleanup)
6. Update `AmsDataGlobal` / `AmsDataLocal` activity caches for immediate display

### Manual Setup
- `GenerateProp25` GET with no params forwards to `manualSetup.jsp` form
- Form collects company name, contact, email, and `q1`–`q8` module flags (legacy LOS mapping)
- POST creates full Prospect → Proposal → Application → Setup chain
- **Known limitation:** `q1`–`q8` only covers original 6 LOSs + 2 modules. Expanded LOSs (IDs 11–19) not yet supported. See backlog item T9.

---

## Key Design Decisions

1. **Module inclusion** — When an agent selects a LOS, all linked ServiceModules are automatically included via the existing `losmodules` M:N table. No separate ProposalModule entity needed.
2. **Application data model** — Original `DataKey`/`DataPair`/`ApplicationData` entities were replaced with `ApplicationField`/`ApplicationFieldValue` (two tables instead of three).
3. **Section scoping** — `ApplicationSection.scope` is either `ALL` (always shown) or `LOS` (shown only when a matching LOS is in the proposal, via `applicationsectionlos` join).
4. **PSP on Prospect** — Currently inferred through agent's PSP. Direct `psp_id` on Prospect is a future consideration.
5. **Setup creation** — `ReviewApplication` creates Setup on approval using the same pattern as `GenerateProp25`. Manual setup (no application) is still supported via `GenerateProp25` directly.
6. **Application PK** — `Application` uses `@Id @OneToOne Proposal` as its PK (not a generated Long). Queries that access `proposal.getApplication()` must use `LEFT JOIN FETCH p.application` to avoid lazy-load or duplicate-insert issues.
7. **Field sort workaround** — EclipseLink's `DISTINCT` + `JOIN FETCH` scrambles `@OrderBy` annotations. Fields are re-sorted in Java after the query.

---

## Database Migration Scripts

All sales pipeline scripts (V001–V003) are tracked in the centralized `docs/analysis/migration_tracker.md`. All environments are at V024. The dev baseline dump is `docs/importscript/beta_ssa_dev_baseline_thru_V024.sql`.

---

## Remaining Work

1. **Refactor manual setup to dynamic LOS** — `GenerateProp25` uses hardcoded `q1`–`q8` flags mapped to old LOS IDs. Needs refactor to accept dynamic LOS list from DB. See backlog T9. (CONF priority)
2. **Empty checklist handling** — Remove task-153 dummy workaround. See backlog T8. (LOW priority)

---

## Implementation History

### Session 1 (Feb 19, 2026)
- Added status + date fields to Proposal and Application entities
- Created Feature, RateDiscount, MarketingMaterial entities
- Refactored DataKey/DataPair/ApplicationData → ApplicationField/ApplicationFieldValue
- Built ProposalBuilder servlet + JSP (3-step form)
- Built ProposalDetail servlet + JSP
- Created `V001__sales_pipeline.sql`

### Session 2 (Feb 19, 2026)
- Built SendProposal servlet (email with GUID link)
- Built ViewProposal servlet + landing page JSP (public, features + pricing + discounts)
- Added Proposal.sourceActivity FK for ticket→proposal linking
- Fixed ProposalBuilder bugs (null-safe sort, prospect loading via new `SalesDAO.getProspectsByPsp()`)
- Created `V002__sales_pipeline_2.sql`

### Session 3 (Feb 19-20, 2026)
- Expanded LOS: deleted LOS 7 (HRA/MERP), added IDs 11-19 (HRA, MERP, ICHRA, EBHRA, QSEHRA, RETIREE, DIRECT, LSA, ADOPTION)
- Created ApplicationSection entity + 20 seeded sections with LOS scoping
- Created IrsLimit entity with 2025+2026 limits
- Created BenefitType (11 categories) + BillingType (5 rate structures) entities
- Built ApplyForProposal servlet (dynamic section assembly, conditional show/hide, JSON plan builder, file uploads)
- Built SaveApplicationProgress servlet (AJAX auto-save + restore)
- Built UploadRateSheet servlet (Wasabi storage)
- Built applicationConfirmation.jsp
- Completed ApplyForProposal POST handler (submit, set SUBMITTED/APPLIED statuses)
- Created `V003__sales_pipeline_3.sql`

### Session 4 (Feb 20, 2026)
- Built ReviewApplications servlet + JSP (list view with multi-select status filtering, defaults to SUBMITTED)
- Built ReviewApplication servlet + JSP (detail view with field rendering, JSON benefit plan display, Wasabi download links)
- Implemented approve/deny/more-info/under-review actions with review notes
- Automated Setup + CheckList + ToDo creation on approval (mirrors GenerateProp25 pattern)
- Built manualSetup.jsp standalone form for GenerateProp25 (param guard on GET)
- Added "Review Application" / "View Approved/Denied Application" buttons to proposalDetail.jsp
- Added `LEFT JOIN FETCH p.application` to ProposalDetail and SaveApplicationProgress queries (fixed lazy-load and duplicate-insert bugs)
- Fixed field sort order in ApplyForProposal (EclipseLink DISTINCT + JOIN FETCH workaround — explicit Java sort after query)
- Added Home navigation to review list and detail pages
- Updated adminMenuOC.jsp with "Create Proposal" and "Review Applications" links
- Full end-to-end pipeline test passed: Create → Send → View → Apply → Save → Submit → Review → Approve → Setup created
- Added backlog items: T8 (empty checklist handling), T9 (refactor manual setup to dynamic LOS)
