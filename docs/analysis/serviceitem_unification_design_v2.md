# ServiceItem Unification — Design Document (v2)

> **Purpose:** Consolidate the four divergent "activity item → task sequence" paths into a single, scalable model.
> **Status:** Complete — implemented V020–V024, TicketSubCategory eliminated, all environments current
> **Date:** February 27, 2026

---

## 1. Problem Statement

The system currently has four different wiring paths from activity items to their required task sequences:

| Activity | "Item" entity | Path to tasks |
|----------|--------------|---------------|
| **Renewal** | `RenewalItem` → `Benefit` → `PlanType` → `TemplatePurpose` (group 1) → `RequiredTaskList` | 3 hops through PlanType |
| **Setup** | `ApplicationModule` (composite key) → `TemplatePurpose` (group 2) → `RequiredTaskList` | Via composite join; no direct LOS/Enhancement link |
| **Ticket** | `TicketSubCategory` → `TemplatePurpose` (group 3) → `RequiredTaskList` | Via separate SubCategory table |
| **Opportunity** | Sales modules → `TemplatePurpose` (group 5) → `RequiredTaskList` | Via sales pipeline |

All four paths converge at **TemplatePurpose → RequiredTaskList → TaskSequenceTable → Task**, but the upstream wiring is different each time. This makes it hard to add new activity types, allow PSPs to define custom service items, or extend to non-DataPath providers.

---

## 2. Current State — Data Inventory

### TemplateGroup (activity type groupings)

| group_id | description | TemplatePurposes | Active RequiredTaskLists |
|----------|-------------|-----------------|------------------------|
| 1 | Renewal | 12 | 11 |
| 2 | Setup | 10 | 8 |
| 3 | Ticket | 35 | 27 |
| 4 | User | 1 | 0 |
| 5 | Sales | 1 | 1 |

### Setup-side inventory (no current link to TemplatePurpose)

| Type | Count | Suppressed | Current FK to TemplatePurpose |
|------|-------|------------|------------------------------|
| LOS | 14 | 0 | None — only FK is `psp_id → assignee` |
| Enhancement | 4 | 0 | None — only FK is `psp_id → assignee` |

Connection to task sequences currently goes: LOS → ServiceModule → ApplicationModule → TemplatePurpose. The new model creates a direct 1:1 link.

---

## 3. Design Decisions (Confirmed)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| **New table vs. evolve TemplatePurpose** | Evolve TemplatePurpose | It already serves as the pivot point; adding columns is less disruptive than a new table |
| **Table rename** | Option B — keep `templatepurpose` table, rename Java class to `ServiceItem` | Avoids FK churn; `@Table(name="templatepurpose")` handles the mapping |
| **TemplateGroup rename** | Java class becomes `ActivityCategory`; table stays `templategroup` | Same Option B approach |
| **TicketSubCategory** | Eliminate as separate concept; ticket items become ServiceItems (group 3) directly | Removes an unnecessary indirection layer |
| **LOS → ServiceItem** | 1:1 — each LOS gets a ServiceItem auto-created | Creating/suppressing a LOS naturally creates/suppresses its ServiceItem |
| **Enhancement → ServiceItem** | 1:1 — each Enhancement gets a ServiceItem auto-created | Same pattern as LOS |
| **PlanType → ServiceItem** | 1:1 — each PlanType (benefit type) gets a ServiceItem | Already exists today; just needs new columns |
| **Renewal frequency** | Default on ServiceItem, override on individual Benefit | "All HRAs default to 12-month renewal, but this specific HRA renews quarterly" |
| **RequiredTaskList creation** | Auto-created 1:1 with every ServiceItem for LOS/Enhancement/PlanType; optional for Tickets (flagged) | Setup and Renewal items always have a task list (even if empty); Ticket items only when flagged as recurring |

---

## 4. Core Principle — The 1:1 Chain

For **Setup**, **Renewal**, and **Opportunity** service items:

```
LOS / Enhancement / PlanType
  └── 1:1 → ServiceItem (enhanced TemplatePurpose)
        └── 1:1 → RequiredTaskList
              └── 1:M → TaskSequenceTable
                    └── M:1 → Task
```

For **Ticket** service items:

```
TicketCategory (grouping only)
  └── 1:M → ServiceItem (group 3, replaces TicketSubCategory)
        └── 0..1 → RequiredTaskList (only if flagged as recurring)
              └── 1:M → TaskSequenceTable
                    └── M:1 → Task
```

The upstream entity (LOS, Enhancement, PlanType) is what the business creates and manages. The ServiceItem is automatically created as its "task sequence identity." The RequiredTaskList is the task template that gets stamped onto a CheckList when an activity is created.

---

## 5. Proposed Schema Changes

### 5a. Enhanced `templatepurpose` (ServiceItem)

New columns added to existing table:

```sql
ALTER TABLE templatepurpose
  ADD COLUMN psp_id BIGINT NULL,
  ADD COLUMN is_suppressed TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN provider_ref VARCHAR(100) NULL,
  ADD COLUMN code VARCHAR(20) NULL,
  ADD COLUMN source_type VARCHAR(20) NULL,
  ADD COLUMN default_renewal_months INT NULL,
  ADD COLUMN has_required_tasks TINYINT(1) NOT NULL DEFAULT 1;

ALTER TABLE templatepurpose
  ADD CONSTRAINT FK_TEMPLATEPURPOSE_psp_id
  FOREIGN KEY (psp_id) REFERENCES assignee(id);
```

| Column | Purpose |
|--------|---------|
| `psp_id` | PSP ownership for multi-tenant scoping |
| `is_suppressed` | Soft-hide from UI without deleting |
| `provider_ref` | External system reference (DataPath plan type ID, vendor code, etc.) |
| `code` | Short identifier for display (e.g., "FSA", "HRA", "COBRA") |
| `source_type` | Origin: `DATAPATH`, `MANUAL`, `IMPORT` |
| `default_renewal_months` | Default renewal cycle for this service type (NULL = not renewable) |
| `has_required_tasks` | Whether this ServiceItem should have a RequiredTaskList (default true; false for one-off tickets) |

### 5b. New FK columns on `los` and `enhancement`

```sql
ALTER TABLE los
  ADD COLUMN service_item_id INT NULL,
  ADD CONSTRAINT FK_LOS_service_item_id
  FOREIGN KEY (service_item_id) REFERENCES templatepurpose(purpose_id);

ALTER TABLE enhancement
  ADD COLUMN service_item_id INT NULL,
  ADD CONSTRAINT FK_ENHANCEMENT_service_item_id
  FOREIGN KEY (service_item_id) REFERENCES templatepurpose(purpose_id);
```

### 5c. Renewal frequency on `benefit`

```sql
ALTER TABLE benefit
  ADD COLUMN renewal_months INT NOT NULL DEFAULT 12;
```

Populated from ServiceItem's `default_renewal_months` at benefit creation time; editable per-benefit afterward. `RenewalService.updateNextRenewalDateInBenefitTable()` changes from `plusYears(1)` to `plusMonths(benefit.getRenewalMonths())`.

### 5d. TicketSubCategory migration

The `ticketsubcategory` table's role is absorbed by ServiceItem (group 3). Migration:

- Each existing TicketSubCategory already has a TemplatePurpose — that TemplatePurpose *is* the ServiceItem
- `ticketsubcategory.is_active` maps to `ServiceItem.is_suppressed` (inverted)
- `ticketsubcategory.category_id` → add `category_id` to `templatepurpose` for ticket grouping:

```sql
ALTER TABLE templatepurpose
  ADD COLUMN category_id BIGINT NULL,
  ADD CONSTRAINT FK_TEMPLATEPURPOSE_category_id
  FOREIGN KEY (category_id) REFERENCES ticketcategory(category_id);
```

After migration, `ticketsubcategory` can be dropped (or kept as a view for backward compatibility during transition).

---

## 6. Data Backfill Plan

### Phase 1: Populate new columns on existing TemplatePurposes

```sql
-- PSP ownership (SSA = 4)
UPDATE templatepurpose SET psp_id = 4 WHERE psp_id IS NULL;

-- Source type by group
UPDATE templatepurpose SET source_type = 'DATAPATH' WHERE group_id = 1;
UPDATE templatepurpose SET source_type = 'MANUAL'   WHERE group_id IN (2, 3, 5);

-- Default renewal months for renewal group
UPDATE templatepurpose SET default_renewal_months = 12 WHERE group_id = 1;

-- Ticket items: migrate category_id and suppression from ticketsubcategory
UPDATE templatepurpose tp
  INNER JOIN ticketsubcategory tsc ON tsc.temp_purpose_id = tp.purpose_id
  SET tp.category_id = tsc.category_id,
      tp.is_suppressed = CASE WHEN tsc.is_active = 1 THEN 0 ELSE 1 END
  WHERE tp.group_id = 3;

-- Ticket items without required tasks (no RequiredTaskList exists)
UPDATE templatepurpose tp
  SET tp.has_required_tasks = 0
  WHERE tp.group_id = 3
    AND NOT EXISTS (
      SELECT 1 FROM tasksequence ts
      WHERE ts.purpose_id = tp.purpose_id
        AND ts.DTYPE = 'RequiredTaskList'
        AND ts.is_inactive = 0
    );
```

### Phase 2: Create ServiceItems for existing LOS and Enhancement

For each LOS and Enhancement, create a ServiceItem (group 2 = Setup) and link it:

```sql
-- This would be done programmatically in Java (DatabaseInitializer or a one-time migration servlet)
-- For each LOS: create TemplatePurpose with group_id=2, description=LOS.description, psp_id=LOS.psp_id
-- Then: UPDATE los SET service_item_id = <new purpose_id> WHERE los_id = <los_id>
-- Then: create RequiredTaskList linked to the new TemplatePurpose

-- Same for each Enhancement
```

Note: Some LOS items may already have TemplatePurposes (the existing 10 in group 2). The migration needs to match existing ones by name/description before creating duplicates.

### Phase 3: Backfill benefit.renewal_months

```sql
UPDATE benefit SET renewal_months = 12;  -- All existing benefits are annual
```

---

## 7. Java Refactoring Plan

### Entity renames (class only, table names unchanged)

| Current class | New class | Table (unchanged) |
|---------------|-----------|-------------------|
| `TemplatePurpose` | `ServiceItem` | `templatepurpose` |
| `TemplateGroup` | `ActivityCategory` | `templategroup` |

### New fields on ServiceItem

```java
@Entity
@Table(name = "templatepurpose")
public class ServiceItem {

    @Id @GeneratedValue
    @Column(name = "purpose_id")
    private int id;

    @Column(name = "description", columnDefinition = "varchar(200)")
    private String description;

    @Column(name = "code", columnDefinition = "varchar(20)")
    private String code;                    // NEW

    @Column(name = "sort_order")
    private int sortOrder;

    @ManyToOne
    @JoinColumn(name = "group_id")
    private ActivityCategory activityCategory;  // RENAMED from templateGroup

    @ManyToOne
    @JoinColumn(name = "psp_id")
    private PSP psp;                        // NEW

    @Column(name = "is_suppressed")
    private boolean suppressed;              // NEW

    @Column(name = "provider_ref", columnDefinition = "varchar(100)")
    private String providerRef;              // NEW

    @Column(name = "source_type", columnDefinition = "varchar(20)")
    private String sourceType;               // NEW

    @Column(name = "default_renewal_months")
    private Integer defaultRenewalMonths;    // NEW

    @Column(name = "has_required_tasks")
    private boolean hasRequiredTasks = true;  // NEW

    @ManyToOne
    @JoinColumn(name = "category_id")
    private TicketCategory ticketCategory;    // NEW (nullable, only for group 3)

    @OneToMany(mappedBy = "serviceItem")     // RENAMED from templatePurpose
    private List<ApplicationModule> applicationModuleList;
}
```

### Files requiring updates (TemplatePurpose → ServiceItem references)

| File | Change |
|------|--------|
| `RequiredTaskList.java` | `templatePurpose` field → `serviceItem` |
| `PlanType.java` | `templatePurpose` field → `serviceItem` |
| `TicketSubCategory.java` | Eventually removed; during transition, keep FK |
| `ApplicationModule.java` | `templatePurpose` reference → `serviceItem` |
| `LOS.java` | Add `serviceItem` field (new 1:1 FK) |
| `Enhancement.java` | Add `serviceItem` field (new 1:1 FK) |
| `Benefit.java` | Add `renewalMonths` field |
| `SequenceBuilder25.java` | All TemplatePurpose references → ServiceItem |
| `SequenceAction25.java` | TemplatePurpose creation logic → ServiceItem |
| `TaskBuilder25.java` | TemplatePurpose references → ServiceItem |
| `RenewalService.java` | `plusYears(1)` → `plusMonths(benefit.getRenewalMonths())` |
| `Importer.java` | TemplatePurpose creation → ServiceItem with new columns populated |
| `DatabaseInitializer.java` | `createTemplatePurpose()` → `createServiceItem()` |
| `ReferenceDataSeeder.java` | TemplateGroup/TemplatePurpose references updated |
| `EntityLookup.java` | `getTemplatePurposeById()` → `getServiceItemById()` |
| `AmsDataGlobal.java` | Any cached TemplatePurpose lists |
| `ServiceManagerHome.java` | If referencing TemplatePurpose for module management |

### JSP files to grep

```bash
grep -rl "templatePurpose\|TemplatePurpose\|templateGroup\|TemplateGroup" src/main/webapp/
```

---

## 8. TicketSubCategory Elimination — Transition Plan

### Current state
- `TicketSubCategory` has: `id`, `description`, `isActive`, `ticketCategory`, `templatePurpose`
- Used in ticket creation dropdowns and sequence builder

### Transition
1. **Phase 1:** Add `category_id` to `templatepurpose`. Backfill from `ticketsubcategory`. Both tables coexist.
2. **Phase 2:** Update all code to read ticket items from ServiceItem (group 3) instead of TicketSubCategory. The `category_id` on ServiceItem replaces `ticketsubcategory.category_id`. The `is_suppressed` flag replaces `is_active` (inverted).
3. **Phase 3:** Update `Ticket` entity — if it has a direct FK to `ticketsubcategory`, migrate to FK to ServiceItem instead.
4. **Phase 4:** Drop `ticketsubcategory` table (or keep as empty shell if FKs are hard to migrate immediately).

### What references TicketSubCategory today?

Need to verify:
```sql
SELECT TABLE_NAME, COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'beta_ssa'
  AND REFERENCED_TABLE_NAME = 'ticketsubcategory';
```

---

## 9. Renewal Evolution — Expanded Vision

### Current renewal chain
```
DataPath Import → PlanType → Benefit → RenewalItem → Renewal
                     ↓
              TemplatePurpose (group 1) → RequiredTaskList → Tasks
```

### To-be renewal chain
```
[Any source] → PlanType → Benefit (with renewal_months) → RenewalItem → Renewal
                  ↓
           ServiceItem (group 1, with default_renewal_months) → RequiredTaskList → Tasks
```

### New capabilities

1. **Manual PlanType creation** — PSP admin can create a PlanType + ServiceItem for a service not in DataPath
2. **Configurable renewal frequency** — ServiceItem sets the default (e.g., 12 months); individual Benefits can override (e.g., quarterly for a specific employer)
3. **Multi-vendor import** — New import configurations can specify: vendor name (`source_type`), benefit type mappings, default renewal frequency. The Importer creates PlanTypes + ServiceItems tagged with the vendor's `provider_ref`.
4. **Non-DataPath benefits** — A benefit administered through a second SaaS platform gets its own PlanType with `source_type = 'VENDOR_X'` and `provider_ref` storing that system's ID

### RenewalService change

```java
// BEFORE
b.setNextRenewalDue(Date.valueOf(b.getNextRenewalDue().toLocalDate().plusYears(1)));

// AFTER
int months = b.getRenewalMonths();  // defaults to 12 if not set
b.setNextRenewalDue(Date.valueOf(b.getNextRenewalDue().toLocalDate().plusMonths(months)));
```

---

## 10. What Does NOT Change

- **RequiredTaskList** — stays as-is, still links 1:1 to ServiceItem
- **TaskSequence / TaskSequenceTable / Task** — untouched
- **RenewalItem / Benefit** — Benefit gains `renewal_months`; otherwise unchanged
- **ApplicationModule** — still uses composite key; just references ServiceItem instead of TemplatePurpose
- **RecurringTaskList / HowToList** — separate TaskSequence subtypes, unaffected
- **CheckList / ToDo** — downstream of task sequences, unaffected

---

## 11. Implementation Order

| Step | Scope | Risk |
|------|-------|------|
| 1 | Migration script: add columns to `templatepurpose`, `los`, `enhancement`, `benefit` | Low — all additive, nullable or defaulted |
| 2 | Backfill script: populate psp_id, source_type, category_id, renewal_months | Low — data-only |
| 3 | Create ServiceItems for existing LOS/Enhancement (match existing TemplatePurposes by name) | Medium — need dedup logic |
| 4 | Java rename: `TemplatePurpose` → `ServiceItem`, `TemplateGroup` → `ActivityCategory` | Medium — many file touches, IDE refactor helps |
| 5 | Update `RenewalService` to use `benefit.getRenewalMonths()` | Low — isolated change |
| 6 | Update `Importer` to populate new ServiceItem columns | Low — additive |
| 7 | Update Sequence Builder UI to work with ServiceItem | Medium — UI changes |
| 8 | Migrate TicketSubCategory references to ServiceItem | Medium — need to verify all FKs |
| 9 | Drop TicketSubCategory table | Low — after all references removed |

---

## 12. Open Item

**PlanType bidirectional relationship (Question 3 from v1):** Currently `PlanType` → `ServiceItem` is `@ManyToOne`. Adding a `ServiceItem.planType` back-reference would allow the ServiceItem to know "I represent this specific DataPath plan type." This is useful for UI display and import dedup logic, but adds a bidirectional JPA relationship to maintain. **Recommend deferring** — can be added later when a specific use case demands it. The `provider_ref` field on ServiceItem can store the PlanType ID as a string reference in the meantime.
