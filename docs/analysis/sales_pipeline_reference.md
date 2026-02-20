# Sales Pipeline — Reference Document

**Last Updated:** February 20, 2026 (Session 4)
**Replaces:** `sales_pipeline_data_model.md`, `sales_pipeline_implementation_log.md`, `sales_pipeline_session3_log.md`

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
| Application Review/Approve UI | ❌ Not started | — |
| Automated Setup creation on approval | ❌ Not started | `GenerateProp25` has template logic |
| Full pipeline test | ❌ Not done | — |

---

## Lifecycle States

### Proposal Status

| Status | Meaning | Set By |
|--------|---------|--------|
| CREATED | Proposal built, not yet sent | `ProposalBuilder` POST |
| SENT | Email sent to prospect | `SendProposal` |
| VIEWED | Prospect opened the landing page | `ViewProposal` GET |
| APPLIED | Prospect submitted application | `ApplyForProposal` POST |
| APPROVED | PSP approved the application | (not yet built) |
| DENIED | PSP denied the application | (not yet built) |
| EXPIRED | Aged out or manually closed | (not yet built) |

### Application Status

| Status | Meaning | Set By |
|--------|---------|--------|
| IN_PROGRESS | Prospect opened form, may have saved progress | `ApplyForProposal` GET / `SaveApplicationProgress` |
| SUBMITTED | Prospect clicked Submit | `ApplyForProposal` POST |
| UNDER_REVIEW | PSP is reviewing | (not yet built) |
| APPROVED | PSP approved | (not yet built) |
| DENIED | PSP denied | (not yet built) |
| MORE_INFO | PSP requested additional info | (not yet built) |

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
| GenerateProp25 | controller/activity/setup | `/GenerateProp25` | Yes | Legacy proposal+setup generator (has Setup creation logic) |

Public endpoints (`/apply/*`, `/saveApplication`, `/uploadRateSheet`, `/viewProposal/*`) are whitelisted in `LoginFilter`.

---

## JSPs

| JSP | Path | Access | Purpose |
|-----|------|--------|---------|
| proposalBuilder.jsp | `WEB-INF/view/sales/` | Internal | 3-step proposal creation form |
| proposalDetail.jsp | `WEB-INF/view/sales/` | Internal | Proposal detail with status, pricing, GUID link |
| viewProposal.jsp | `WEB-INF/view/sales/` | Public | Prospect-facing proposal with features, pricing, "Apply Now" |
| applyForProposal.jsp | `WEB-INF/view/sales/` | Public | Dynamic application form with conditional sections |
| applicationConfirmation.jsp | `WEB-INF/view/sales/` | Public | Post-submit confirmation page |

---

## Application Form Details

### Dynamic Section Assembly
Sections load based on proposal's LOS list. A JPQL query fetches sections where `scope='ALL'` or where `applicationsectionlos` overlaps with the proposal's LOSs.

### Conditional Show/Hide
19 JavaScript rules control field visibility based on other field values. When a trigger field's parent section is hidden (LOS not selected), dependent fields are also hidden. Hidden fields are cleared on hide.

### Benefit Plan Builder
The Billing section includes a JSON-based plan builder for defining benefit plans. Each plan has: planName, benefitTypeId, billingTypeId, effectiveDate, renewalDate, tiers (or flat rate), notes, and optional rate sheet upload. Plans are serialized to a hidden `bill_benefit_plans` field as JSON.

### Rate Sheet Uploads
`UploadRateSheet` servlet accepts multipart file uploads, stores them in Wasabi under `applications/{proposalId}/{uuid}/{filename}` via `StorageDAO`, and returns a JSON response with the storage key.

### Save & Restore
`SaveApplicationProgress` saves all field values via AJAX (manual button + auto-save every 60 seconds when dirty). On page reload, `ApplyForProposal` GET queries saved `ApplicationFieldValue` rows and merges them into the defaults map. JSON values use HTML entity escaping for safe attribute embedding.

### IRS Limits
`SalesDAO.getIrsLimits()` loads limits for the current plan year (falls back to most recent year). Limits display as read-only reference values in relevant sections (FSA, HSA, Transit, Adoption, QSEHRA).

---

## Key Design Decisions

1. **Module inclusion** — When an agent selects a LOS, all linked ServiceModules are automatically included via the existing `losmodules` M:N table. No separate ProposalModule entity needed.
2. **Application data model** — Original `DataKey`/`DataPair`/`ApplicationData` entities were replaced with `ApplicationField`/`ApplicationFieldValue` (two tables instead of three).
3. **Section scoping** — `ApplicationSection.scope` is either `ALL` (always shown) or `LOS` (shown only when a matching LOS is in the proposal, via `applicationsectionlos` join).
4. **PSP on Prospect** — Currently inferred through agent's PSP. Direct `psp_id` on Prospect is a future consideration.
5. **Setup creation** — `GenerateProp25` already contains the logic for creating Setup + CheckList + ToDo items from an Application. The review/approve UI will call similar logic.

---

## Database Migration Scripts

All scripts are in `docs/` at the repo root. Run in order.

| Script | Session | Description |
|--------|---------|-------------|
| `sales_pipeline_migration.sql` | 1 | New tables (feature, ratediscount, marketingmaterial, applicationfield, applicationfieldvalue), column adds to proposal + application, entity renames |
| `sales_pipeline_migration_2.sql` | 2 | `proposal.source_activity_id` nullable FK |
| `sales_pipeline_migration_3.sql` | 3 | LOS expansion (IDs 11-19), applicationsection + applicationsectionlos, ~95 applicationfield seeds, irslimit, billingtype, benefittype, constant inserts for S3 |

### Migration Status

| Environment | Script 1 | Script 2 | Script 3 |
|-------------|----------|----------|----------|
| Local (Work) | ✅ | ✅ | ✅ |
| Local (Home) | ✅ | ✅ | ✅ |
| Production | ❌ | ❌ | ❌ |

**Production note:** S3/Wasabi constants (`S3_ENDPOINT`, `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY`) in the `constant` table are INSERT IGNORE — values must be filled in manually on production after running script 3.

---

## Remaining Work

1. **Application review/approve UI** — Internal PSP view: list submitted applications, view field values grouped by section, deserialize JSON benefit plans with download links for uploaded rate sheets, approve/deny actions
2. **Automated Setup creation on approval** — When approved: create Setup activity, build checklist from ApplicationModule templates, link back to Application
3. **Full pipeline test** — Create → Send → View → Apply → Save → Submit → Review → Approve → Setup created
4. **Production migration** — Run all 3 migration scripts, fill in S3 constants, deploy code

---

## Implementation History

### Session 1 (Feb 19, 2026)
- Added status + date fields to Proposal and Application entities
- Created Feature, RateDiscount, MarketingMaterial entities
- Refactored DataKey/DataPair/ApplicationData → ApplicationField/ApplicationFieldValue
- Built ProposalBuilder servlet + JSP (3-step form)
- Built ProposalDetail servlet + JSP
- Created `sales_pipeline_migration.sql`

### Session 2 (Feb 19, 2026)
- Built SendProposal servlet (email with GUID link)
- Built ViewProposal servlet + landing page JSP (public, features + pricing + discounts)
- Added Proposal.sourceActivity FK for ticket→proposal linking
- Fixed ProposalBuilder bugs (null-safe sort, prospect loading via new `SalesDAO.getProspectsByPsp()`)
- Created `sales_pipeline_migration_2.sql`

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
- Created `sales_pipeline_migration_3.sql`
