# Sales Pipeline — Data Model Design

**Created:** February 19, 2026
**Project:** AMS Sales Portal (#2 from ams_to_be_vision.md)
**Status:** Design — pre-implementation

---

## Design Decisions from Q&A

1. Pricing is at the **ServiceModule** level (not LOS directly). Each LOS maps to one or more ServiceModules, and each ServiceModule gets its own fee line items within a rate package. This matches the existing `RateTable` structure.
2. Multi-plan discounts are defined per rate package. Admin specifies which LOSs must all be selected to trigger the discount, the dollar amount, and which fee type it applies to.
3. Prospects are **not locked** to a single agent — any agent or PSP inside-sales user can create proposals for any prospect.
4. Application data uses **system-defined templates** per LOS (standard fields for all + LOS-specific sections), with room for custom fields later.
5. Application access is **GUID-based** — anyone with the link can view/edit. No prospect login required.

---

## Entity Inventory

### Legend

| Symbol | Meaning |
|--------|---------|
| ✅ EXISTS | Entity exists, no changes needed |
| 🔧 MODIFY | Entity exists, needs field additions or relationship changes |
| 🆕 NEW | Entity does not exist, must be created |

---

## Layer 1 — PSP Admin: Catalog & Rate Configuration

These entities define what a PSP sells and how it's priced.

### LOS ✅ EXISTS
Line of Service — a primary product (FSA, HRA, COBRA, etc.)

| Field | Type | Notes |
|-------|------|-------|
| id | Long PK | `los_id`, auto-generated |
| description | varchar(100) | Full name |
| shortText | varchar(10) | Code (FSA, HRA, etc.) |
| psp | M:1 PSP | Owner |
| serviceModuleList | M:N ServiceModule | Join table `losmodules` |

**No changes needed.** Current entity is correct.

---

### ServiceModule ✅ EXISTS
Module / enhancement — optional add-ons linked to eligible LOSs (e.g., Debit Cards, Payment Services). Also used as the pricing anchor in RateTable.

| Field | Type | Notes |
|-------|------|-------|
| id | Long PK | Auto-generated |
| description | varchar | Full name |
| shortText | varchar | Code |
| sortOrder | int | Display ordering |
| psp | M:1 PSP | Owner |
| listOfLosWithThisModule | M:N LOS | Inverse of LOS.serviceModuleList |

**No changes needed.** Each LOS has a "self" ServiceModule (e.g., LOS "FSA" → ServiceModule "FSA") plus optional enhancement modules (e.g., ServiceModule "Cards"). Pricing is always at the ServiceModule level.

---

### PriceItem ✅ EXISTS
Fee type — reusable catalog of how a fee is applied (Setup Fee, Annual Fee, PPPM, etc.)

| Field | Type | Notes |
|-------|------|-------|
| id | Long PK | |
| description | varchar | Display name |
| sortOrder | int | Controls display order on proposals |
| psp | M:1 PSP | Owner |

**No changes needed.**

---

### Rate ✅ EXISTS
Rate package — a named pricing tier assigned to agencies.

| Field | Type | Notes |
|-------|------|-------|
| id | Long PK | |
| description | varchar | Package name (e.g., "Standard Rate") |
| isSuppressed | boolean | Soft-delete / hide |
| psp | M:1 PSP | Owner |
| listOfAgenciesWithThisRate | M:N Agency | Join table `agencyrates` |
| rateTable | 1:M RateTable | All pricing rows |

**No changes needed.**

---

### RateTable ✅ EXISTS
Individual price point — one fee amount for one ServiceModule/PriceItem combo within a Rate package.

| Field | Type | Notes |
|-------|------|-------|
| rateTableID | Composite PK | (rate_id, price_item_id, module_id) |
| rate | M:1 Rate | Which rate package |
| priceItem | M:1 PriceItem | Which fee type |
| module | M:1 ServiceModule | Which module/LOS-module |
| price | double | Dollar amount |

**No changes needed.** This is the core pricing engine. Admin creates rows here to define what's available and at what price within each rate package.

---

### RateDiscount 🆕 NEW
Multi-plan discount rule within a rate package.

| Field | Type | Notes |
|-------|------|-------|
| id | Long PK | Auto-generated |
| rate | M:1 Rate | Which rate package this discount belongs to |
| description | varchar(200) | Display text (e.g., "FSA + HRA Bundle Discount") |
| discountAmount | double | Dollar amount off |
| priceItem | M:1 PriceItem | Which fee type the discount applies to (annual, setup, etc.) |
| requiredLosList | M:N LOS | Join table `ratediscountlos` — ALL listed LOSs must be in the proposal to trigger |

**New entity.** The system evaluates all discounts for the proposal's rate package, checks if the proposal's selected LOSs include all required LOSs for each discount, and applies qualifying discounts to the displayed pricing.

**Table:** `ratediscount`
**Join table:** `ratediscountlos` (ratediscount_id, los_id)

---

### Feature 🆕 NEW
Descriptive feature/capability tied to a ServiceModule. Used to populate proposal content with what the PSP provides for each service.

| Field | Type | Notes |
|-------|------|-------|
| id | Long PK | Auto-generated |
| serviceModule | M:1 ServiceModule | Which module this feature describes |
| description | varchar(500) | Feature text (e.g., "24/7 online participant portal") |
| sortOrder | int | Display ordering within the module |
| psp | M:1 PSP | Owner |

**New entity.** Multiple features per ServiceModule. Displayed on proposal pages under each LOS/module section.

**Table:** `feature`

---

### MarketingMaterial 🆕 NEW
Standalone library item (brochure, video, web link) managed independently and linked to ServiceModules for display on proposals and other contexts.

| Field | Type | Notes |
|-------|------|-------|
| id | Long PK | Auto-generated |
| title | varchar(200) | Display name (e.g., "FSA Participant Guide") |
| description | varchar(500) | Brief summary of the material |
| materialType | varchar(20) | PDF, VIDEO, LINK |
| url | varchar(500) | For VIDEO and LINK types — direct URL |
| storageGuid | varchar(36) | For PDF type — Wasabi object key |
| audience | varchar(20) | PROSPECT (sales-facing) or CLIENT (educational/how-to) |
| sortOrder | int | Display ordering |
| psp | M:1 PSP | Owner |
| serviceModuleList | M:N ServiceModule | Join table `materialmodule` — which modules this material is relevant to |

**New entity.** Materials are created and managed in a separate library UI (future project). The linkage to ServiceModules means that when a proposal includes a LOS, all materials tagged to that LOS's modules can be shown to the prospect alongside features and pricing.

**Table:** `marketingmaterial`
**Join table:** `materialmodule` (material_id, module_id)

---

## Layer 2 — Agency & Agent Configuration

### Agency ✅ EXISTS

| Field | Type | Notes |
|-------|------|-------|
| id | Long PK | |
| name | varchar(200) | |
| taxId, phone | varchar | |
| psp | M:1 PSP | Owner |
| address | 1:1 Address | |
| primaryContact | 1:1 Person | |
| agencyRateList | M:N Rate | Join table `agencyrates` |
| agentList | M:N Person | Join table `agents` |

**No changes needed.** Agents are Persons assigned to an Agency. Agency has assigned Rate packages.

---

## Layer 3 — Sales Flow (Prospect → Proposal)

### Prospect 🔧 MODIFY

| Field | Type | Notes |
|-------|------|-------|
| id | Long PK | |
| name | varchar(200) | Company/prospect name |
| contact | 1:1 Person | Primary contact |
| address | 1:1 Address | |
| agent | M:1 Person | **EXISTING — reconsider: creator, not exclusive owner** |
| proposalList | 1:M Proposal | |

**Change needed:** The `agent` field currently implies ownership. Since prospects are not agent-locked, this field should represent "created by" rather than "belongs to." No schema change required — just a semantic clarification. Any agent/PSP user can create proposals for any prospect they can see.

> **Future consideration:** Add `psp` (M:1 PSP) to Prospect if not already present, to scope visibility. Currently Prospect doesn't have a direct PSP link — it's inferred through the agent's PSP. May want to add this explicitly.

---

### Proposal 🔧 MODIFY

**Current fields:**

| Field | Type | Notes |
|-------|------|-------|
| id | Long PK | |
| dateCreated | Timestamp | Auto-set |
| isInactive | boolean | Soft delete |
| applicationGUID | varchar(36) | GUID for public access |
| prospect | M:1 Prospect | |
| rate | M:1 Rate | Selected rate package |
| losList | M:N LOS | Join table `proposalitems` |
| application | 1:1 Application | Mapped by Application.proposal |

**Fields to ADD:**

| Field | Type | Notes |
|-------|------|-------|
| status | varchar(20) | Lifecycle state (see below) |
| createdBy | M:1 Person | The agent or PSP user who created this proposal |
| dateSent | Timestamp | Nullable — when emailed to prospect |
| dateViewed | Timestamp | Nullable — first view of proposal landing page |
| dateApplied | Timestamp | Nullable — when application was submitted |

**Proposal status values:**

| Status | Meaning |
|--------|---------|
| CREATED | Proposal built, not yet sent |
| SENT | Email sent to prospect with proposal link |
| VIEWED | Prospect has opened the proposal landing page |
| APPLIED | Prospect has submitted an application |
| APPROVED | PSP has approved the application |
| DENIED | PSP has denied the application |
| EXPIRED | Proposal aged out or manually closed |

---

### Module Inclusion Rule — RESOLVED
When an agent selects a LOS, **all linked modules are automatically included**. No separate ProposalModule entity needed. The system derives included modules from the existing LOS → ServiceModule (M:N `losmodules`) relationship at display/pricing time.

---

## Layer 4 — Application (Prospect-Facing)

### Application 🔧 MODIFY

**Current fields:**

| Field | Type | Notes |
|-------|------|-------|
| proposal | 1:1 Proposal (PK) | Proposal this application belongs to |
| applicationModuleList | 1:M ApplicationModule | Which modules the prospect selected |
| fieldValues | 1:M ApplicationFieldValue | Prospect's form responses (was applicationDataList) |
| setup | 1:1 Setup | Created upon approval |

**Fields to ADD:**

| Field | Type | Notes |
|-------|------|-------|
| status | varchar(20) | IN_PROGRESS, SUBMITTED, UNDER_REVIEW, APPROVED, DENIED, MORE_INFO |
| dateStarted | Timestamp | When prospect first opened the application |
| dateSubmitted | Timestamp | When prospect clicked Submit |
| dateReviewed | Timestamp | When PSP took action |
| reviewedBy | M:1 Person | PSP user who approved/denied |
| reviewNotes | text | PSP comments on approval/denial |

---

### ApplicationModule ✅ EXISTS
Tracks which modules the prospect chose to apply for (may be a subset of what was proposed).

| Field | Type | Notes |
|-------|------|-------|
| Composite PK | (application_id, template_purpose_id) | |
| application | M:1 Application | |
| templatePurpose | M:1 TemplatePurpose | Links to checklist template system |

**No changes needed** — but review whether `templatePurpose` is still the right link or if this should point to ServiceModule instead. Current structure ties application modules to checklist templates, which is useful for the approval→Setup creation flow.

---

### ApplicationData ✅ EXISTS (verify structure)
Stores the prospect's form field responses.

> **Need to verify:** What fields does `ApplicationData` currently have? If it's a simple key-value store (field_name, field_value, application_id), it may work for our predefined template approach. If it has a different structure, we may need to adjust.

---

### ApplicationField 🔧 RENAME + MODIFY (was `DataKey`)
Defines a form field on the application. Scoped to a TemplatePurpose (which maps to a LOS/module context) or general (null TemplatePurpose = appears on all applications).

**Current fields (from DataKey):**

| Field | Type | Notes |
|-------|------|-------|
| keyName | String PK | Internal key — rename column to `field_key` |
| easyName | varchar | Display label |
| templatePurpose | M:1 TemplatePurpose | Scopes field to a LOS/module context (null = general/all) |

**Fields to ADD:**

| Field | Type | Notes |
|-------|------|-------|
| fieldType | varchar(20) | TEXT, DATE, NUMBER, SELECT, TEXTAREA, BOOLEAN |
| isRequired | boolean | Validation flag |
| sortOrder | int | Display ordering within section |
| selectOptions | varchar(500) | Nullable — pipe-delimited options for SELECT type |

**Rename:** `DataKey` → `ApplicationField`, table `datakey` → `applicationfield`

---

### ApplicationFieldValue 🔧 RENAME + MODIFY (was `DataPair` + `ApplicationData` combined)
Stores a prospect's answer for a specific field on a specific application. Replaces both `DataPair` and `ApplicationData` — flattened from three tables to two.

| Field | Type | Notes |
|-------|------|-------|
| id | Long PK | Auto-generated |
| application | M:1 Application | Which application this answer belongs to |
| applicationField | M:1 ApplicationField | Which field this answers |
| fieldValue | varchar/text | The prospect's response |

**Rename:** `DataPair` + `ApplicationData` → single `ApplicationFieldValue`, table `applicationfieldvalue`
**Eliminate:** `ApplicationData` join entity and `ApplicationDataID` embeddable — no longer needed.

**Application relationship update:** `Application.applicationDataList` becomes `Application.fieldValues` (1:M ApplicationFieldValue)

---

## Layer 5 — Approval → Setup Handoff

This layer already partially exists via the `Setup` entity's relationship to `Application`. The enhancement is:

1. When PSP approves an application (`Application.status` → `APPROVED`, `Proposal.status` → `APPROVED`):
2. System automatically creates a `Setup` activity
3. Setup pulls its checklist from templates matching the `ApplicationModule` list (existing TemplatePurpose linkage)
4. Setup is linked back to the Application (existing `Setup.application` relationship)

**No new entities needed** for this layer — it's workflow logic built on existing relationships.

---

## Relationship Diagram (New/Modified Only)

```
Rate ──1:M──> RateTable (existing)
Rate ──1:M──> RateDiscount (NEW)
RateDiscount ──M:N──> LOS (NEW join: ratediscountlos)
RateDiscount ──M:1──> PriceItem (NEW - which fee type)

ServiceModule ──1:M──> Feature (NEW)
ServiceModule ──M:N──> MarketingMaterial (NEW join: materialmodule)

Proposal ──M:1──> Person (NEW field: createdBy)
Proposal  + status, dateSent, dateViewed, dateApplied (NEW fields)
Proposal ──M:N──> LOS (existing — modules auto-derived from LOS→ServiceModule)

Application + status, dateStarted, dateSubmitted, dateReviewed, reviewedBy, reviewNotes (NEW fields)
Application ──1:M──> ApplicationFieldValue (REFACTORED - was DataPair+ApplicationData)

ApplicationField (RENAMED from DataKey + new metadata fields)
ApplicationFieldValue ──M:1──> ApplicationField

MarketingMaterial (NEW - standalone library, linked to modules)
```

---

## New Database Tables Summary

| Table | Purpose |
|-------|---------|
| `ratediscount` | Multi-plan discount rules per rate package |
| `ratediscountlos` | Join: which LOSs are required for a discount |
| `feature` | Descriptive features per ServiceModule |
| `marketingmaterial` | Standalone library of brochures, videos, links |
| `materialmodule` | Join: links materials to ServiceModules |
| `applicationfield` | Form field definitions per section/LOS (renamed from `datakey`) |
| `applicationfieldvalue` | Prospect responses per application (replaces `datapair` + `applicationdata`) |

**Tables to DROP (after migration):**

| Table | Replaced By |
|-------|-------------|
| `datakey` | `applicationfield` |
| `datapair` | `applicationfieldvalue` |
| `applicationdata` | Eliminated — relationship absorbed into `applicationfieldvalue` |

**Entities to DELETE:**

| Class | Replaced By |
|-------|-------------|
| `DataKey` | `ApplicationField` |
| `DataPair` | `ApplicationFieldValue` |
| `ApplicationData` | Eliminated |
| `ApplicationDataID` | Eliminated |

---

## Resolved Design Decisions

1. **Module inclusion** — All modules linked to a selected LOS are automatically included. No ProposalModule entity needed.
2. **Marketing materials** — Standalone `MarketingMaterial` entity linked to ServiceModules via M:N `materialmodule` join table. Wasabi storage for PDFs, URLs for videos/links.
3. **Application data refactor** — RESOLVED: `DataKey` → `ApplicationField` (add fieldType, isRequired, sortOrder, selectOptions). `DataPair` + `ApplicationData` → single `ApplicationFieldValue` (application, field, value). Delete `DataPair`, `ApplicationData`, `ApplicationDataID`.
4. **PSP on Prospect** — LOW PRIORITY: Consider adding direct `psp_id` to prospect table later for cleaner scoping.

---

## Recommended Build Order

| Step | What | Why First |
|------|------|-----------|
| 1 | Add `status` + date fields to Proposal and Application entities | Foundation for everything — no UI works without lifecycle states |
| 2 | Create `Feature` entity + admin CRUD | Low complexity, high demo value — proposals look richer |
| 3 | Create `RateDiscount` entity + admin CRUD | Completes the rate package configuration story |
| 4 | Build Proposal Builder JSP | The core agent-facing UI — select rate, LOSs, create proposal |
| 5 | Rebuild proposal landing page | Public-facing, GUID-accessed, shows features + pricing + discounts |
| 6 | Build Application form | Prospect-facing, GUID-accessed, predefined fields filtered by LOS |
| 7 | Build Application review/approve UI | PSP admin reviews, approves/denies, triggers Setup creation |
| 8 | Automated Setup creation on approval | Closes the loop from sale to operational activity |
