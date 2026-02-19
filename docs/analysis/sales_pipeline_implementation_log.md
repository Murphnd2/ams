## Sales Pipeline Entity Updates — February 19, 2026

This file documents the entity changes made during the Sales Pipeline Data Model implementation session.
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

### New Servlets / JSPs Created

#### ProposalBuilder (controller/activity/setup)
- **URL:** `/ProposalBuilder`
- **GET:** Loads agencies, rates, LOSs, prospects → forwards to `proposalBuilder.jsp`
- **POST:** Creates proposal with GUID, status=CREATED, selected LOSs
- **Known issue:** Null-safe sort on prospect names needed (line 64)

#### proposalBuilder.jsp (WEB-INF/view/sales/)
- 3-step card layout: Select Prospect → Select Rate → Select LOSs
- Step badges with visual state (pending/active/complete)
- "New Prospect" modal (posts to CreateProspect — not yet built)
- Submit disabled until all 3 steps complete

---

### Database Migration
- Full migration script saved as `docs/sales_pipeline_migration.sql`
- Local DB: fully migrated
- Production DB: NOT YET MIGRATED — run migration script before deploying code changes
- `persistence-local.xml`: requires `allowPublicKeyRetrieval=true` and `jdbc.user` property
