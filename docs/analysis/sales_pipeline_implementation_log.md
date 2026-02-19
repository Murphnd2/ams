## Sales Pipeline Entity Updates — February 19, 2026

This file documents the entity changes made during the Sales Pipeline Data Model implementation sessions.
Complements `entity_reference.md` and `sales_pipeline_data_model.md`.

---

### Entities MODIFIED

#### Proposal (model/sales/agency)
**Fields added:**
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| status | String | `status` varchar(20) | Default 'CREATED'. Values: CREATED, SENT, VIEWED, APPLIED, APPROVED, DENIED, EXPIRED |
| createdBy | M:1 Person | `created_by` | Agent or PSP user who created the proposal |
| dateSent | Timestamp | `date_sent` | Nullable — when emailed to prospect |
| dateViewed | Timestamp | `date_viewed` | Nullable — first view of landing page |
| dateApplied | Timestamp | `date_applied` | Nullable — when application submitted |
| sourceActivity | M:1 Activity | `source_activity_id` | Nullable — optional link back to originating ticket (Session 2) |

#### Application (model/sales/application)
**Fields added:**
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| status | String | `status` varchar(20) | Default 'IN_PROGRESS'. Values: IN_PROGRESS, SUBMITTED, UNDER_REVIEW, APPROVED, DENIED, MORE_INFO |
| dateStarted | Timestamp | `date_started` | When prospect opened application |
| dateSubmitted | Timestamp | `date_submitted` | When prospect clicked Submit |
| dateReviewed | Timestamp | `date_reviewed` | When PSP took action |
| reviewedBy | M:1 Person | `reviewed_by` | PSP user who approved/denied |
| reviewNotes | String | `review_notes` TEXT | PSP comments |

**Relationship changed:** `applicationDataList` (List\<ApplicationData\>) → `fieldValues` (List\<ApplicationFieldValue\>)

#### TemplatePurpose (model/activity/checklist/sequences/support)
**Relationship changed:** `dataKeyList` (List\<DataKey\>) → `applicationFieldList` (List\<ApplicationField\>)

---

### Entities CREATED

#### Feature (model/sales/offering)
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| id | Long PK | `feature_id` | Auto-generated |
| serviceModule | M:1 ServiceModule | `module_id` | Which module this feature describes |
| description | String | `description` varchar(500) | Feature text |
| sortOrder | int | `sort_order` | Display ordering |
| psp | M:1 PSP | `psp_id` | Owner |

#### RateDiscount (model/sales/agency)
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| id | Long PK | `ratediscount_id` | Auto-generated |
| rate | M:1 Rate | `rate_id` | Which rate package |
| description | String | `description` varchar(200) | Discount description |
| discountAmount | double | `discount_amount` | Dollar amount off |
| priceItem | M:1 PriceItem | `price_item_id` | Which fee type the discount applies to |
| requiredLosList | M:N LOS | join: `ratediscountlos` | ALL listed LOSs must be in proposal to trigger |

#### MarketingMaterial (model/sales/offering)
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| id | Long PK | `material_id` | Auto-generated |
| title | String | `title` varchar(200) | Display name |
| description | String | `description` varchar(500) | Brief summary |
| materialType | String | `material_type` varchar(20) | PDF, VIDEO, LINK |
| url | String | `url` varchar(500) | For VIDEO and LINK types |
| storageGuid | String | `storage_guid` varchar(36) | Wasabi key for PDFs |
| audience | String | `audience` varchar(20) | PROSPECT or CLIENT |
| sortOrder | int | `sort_order` | Display ordering |
| psp | M:1 PSP | `psp_id` | Owner |
| serviceModuleList | M:N ServiceModule | join: `materialmodule` | Which modules this material is relevant to |

#### ApplicationField (model/sales/application) — renamed from DataKey
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| fieldKey | String PK | `field_key` varchar(100) | Internal key (was `key_name`) |
| label | String | `label` varchar(200) | Display label (was `easy_name`) |
| templatePurpose | M:1 TemplatePurpose | `template_purpose_id` | Scopes field to LOS context |
| fieldType | String | `field_type` varchar(20) | TEXT, DATE, NUMBER, SELECT, TEXTAREA, BOOLEAN |
| isRequired | boolean | `is_required` | Validation flag |
| sortOrder | int | `sort_order` | Display ordering |
| selectOptions | String | `select_options` varchar(500) | Pipe-delimited options for SELECT type |

#### ApplicationFieldValue (model/sales/application) — replaces DataPair + ApplicationData
| Field | Type | Column | Notes |
|-------|------|--------|-------|
| id | Long PK | `field_value_id` | Auto-generated |
| application | M:1 Application | `application_id` | Which application |
| applicationField | M:1 ApplicationField | `field_key` | Which field |
| fieldValue | String | `field_value` TEXT | Prospect's response |

---

### Entities DELETED
| Class | Package | Replaced By |
|-------|---------|-------------|
| DataKey | model/sales/application | ApplicationField |
| DataPair | model/sales/application | ApplicationFieldValue |
| ApplicationData | model/sales/application | Eliminated (absorbed into ApplicationFieldValue) |
| ApplicationDataID | model/sales/application | Eliminated |

---

## Session 1 — February 19, 2026

### New Servlets / JSPs Created

#### ProposalBuilder (controller/activity/setup)
- **URL:** `/ProposalBuilder`
- **GET:** Loads agencies, rates, LOSs, prospects → forwards to `proposalBuilder.jsp`
- **POST:** Creates proposal with GUID, status=CREATED, selected LOSs → redirects to `ProposalDetail`

#### proposalBuilder.jsp (WEB-INF/view/sales/)
- 3-step card layout: Select Prospect → Select Rate → Select LOSs
- Step badges with visual state (pending/active/complete)
- "New Prospect" modal (posts to CreateProspect)
- Submit disabled until all 3 steps complete
- Auto-selects prospect when redirected from CreateProspect
- Triggers `updateSteps()` on page load when prospect is pre-selected

### Database Migration
- Full migration script saved as `docs/sales_pipeline_migration.sql`
- Local DB: fully migrated
- Production DB: NOT YET MIGRATED — run migration script before deploying code changes
- `persistence-local.xml`: requires `allowPublicKeyRetrieval=true` and `jdbc.user` property

---

## Session 2 — February 19, 2026

### Bugs Fixed
- **ProposalBuilder.java** — Null-safe sort had typo: `b.getName() != null ? a.getName()` → fixed to `b.getName()`
- **Prospect loading** — Replaced complex nested fetch join loop (`getAgencyProspects` per agency) with new `SalesDAO.getProspectsByPsp()` method. Old approach failed to pick up newly created prospects due to EclipseLink join/cache behavior.

### New Servlets Created

#### CreateProspect (controller/activity/setup)
- **URL:** `/CreateProspect`
- **POST:** Creates Person (contact) + Prospect, links to agency's first agent, redirects to ProposalBuilder with `?selectedProspect=` param for auto-selection
- Follows existing `GenerateProp` pattern for Person/Prospect creation

#### ProposalDetail (controller/activity/setup)
- **URL:** `/ProposalDetail?id=`
- **GET:** Loads proposal with LOSs (fetch join), pricing via `SalesDAO.getPricing()`, forwards to `proposalDetail.jsp`

#### SendProposal (controller/activity/setup)
- **URL:** `/SendProposal`
- **GET:** Pre-populates send form with prospect contact, default email body with signature, proposal link
- **POST:** Sends branded email via `EmailDAO` + `EmailTemplate.wrapBodyOnly()`, updates proposal status to SENT, sets dateSent, optionally logs email to source activity
- Features: editable To/CC fields, "Copy me" checkbox, CKEditor body editor, signature visible and editable in body

### New JSPs Created

#### proposalDetail.jsp (WEB-INF/view/sales/)
- Proposal header with status badge
- Prospect info with contact details
- Rate package and creation date/creator
- Copyable GUID link for prospect sharing
- LOS badges
- Pricing summary table grouped by ServiceModule
- Action buttons: Send Proposal (links to SendProposal), Back to Builder

#### sendProposal.jsp (WEB-INF/view/sales/)
- Proposal summary bar (LOSs, link)
- Recipients card: To (pre-filled), CC (comma-separated), Copy me checkbox
- Message card: Subject (pre-filled), Body with CKEditor (pre-filled with template + signature)
- Send button with loading state
- CKEditor 4.22.1 standard toolbar

### ProposalBuilder Enhancements
- `doPost` now returns `Proposal` from `createProposal()` and redirects to `ProposalDetail?id=`
- `doGet` reads `selectedProspect` param and passes to JSP for auto-selection
- JSP auto-selects prospect and triggers `updateSteps()` on page load
- Prospect dropdown uses new `SalesDAO.getProspectsByPsp()` — simpler, always current

### EmailTemplate Enhancement
- Added `wrapBodyOnly(String body, String pspName, EntityManager em)` — branded header + footer wrapper without signature block. Used by SendProposal so signature is part of editable body (no duplication).

### New DAO Methods
- `SalesDAO.getProspectsByPsp(em, pspId)` — direct query for all prospects by PSP via agent relationship

### Entity Changes
- `Proposal.sourceActivity` (M:1 Activity, nullable) — optional link to originating ticket for PSP workflow

### Database Changes (Session 2)
- `ALTER TABLE proposal ADD COLUMN source_activity_id BIGINT NULL;`
- Migration script: `docs/sales_pipeline_migration_2.sql`

### What's Next
1. Proposal landing page — public-facing, GUID-accessed, shows features + pricing (what the prospect sees)
2. Application form — prospect fills out after viewing proposal
3. Application review/approve UI — PSP reviews, approves/denies
4. Automated Setup creation on approval
5. Future enhancements: multi-view ProposalBuilder (admin vs agent vs PSP), duplicate email check on CreateProspect, fullName on Person
