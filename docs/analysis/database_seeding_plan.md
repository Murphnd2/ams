# SSA Database Seeding — Implementation Plan

> **Purpose:** Finalize and align all database seeding paths after the ServiceItem Unification project (V020–V024), TicketSubCategory elimination, and Session 9 DatabaseInitializer overhaul.
>
> **Context:** The old `TicketSubCategory` table has been dropped. All ticket categorization now flows through `ServiceItem` (group 3) with a `ticketCategory` FK. The seeding code needs to reflect this unified model across all initialization paths.

---

## 1. Current Seeding Architecture

There are **three seeding entry points**, each building on the previous:

| Entry Point | Servlet | When Used | What It Creates |
|-------------|---------|-----------|-----------------|
| **DatabaseInitializer** | `/InitializeDataBase` | Fresh VPS cold start | Core reference data + PSP entity + admin user |
| **SeedDemoData** | `/SeedDemoData` | Demo/conference prep | Employers, employees, benefits, activities, demo users |
| **ReSeedDemoData** | `/ReSeedDemoData` | Factory reset + demo | Truncates all → re-initializes → seeds demo data |

### Call Chain
```
InitializeDataBase.doPost()
  → DatabaseInitializer.initializeDataBase(request, em)
    → retrieveFormData(request)
    → performInitialization(em)

SeedDemoData.doGet()
  → seedAllDemoData(em, out)

ReSeedDemoData.doPost()
  → ReSeedDb.executeReset(em, out)        // capture → truncate → reinitialize
    → DatabaseResetUtil.reinitialize(em, savedState, out)
      → DatabaseInitializer.performInitialization(em)
  → SeedDemoData.seedAllDemoData(em, out)  // then layer demo data on top
```

---

## 2. Two-Tier Ticket Seeding Design

Ticket categories and starter service items are split into two tiers:

### Tier 1 — Core (All PSPs)

Seeded by `DatabaseInitializer.performInitialization()`. These are the obvious, universal categories every benefits admin PSP needs on day one. Also re-created by `ReSeedDb`.

| Cat ID | Category | Short | Starter ServiceItems |
|--------|----------|-------|----------------------|
| 11 | Claims | Claims | Claim not paid, Claim paid incorrectly |
| 12 | Access / Online | Access | Can't log in to portal, Need online access |
| 18 | Billing | Billing | Billing discrepancy, Invoice request |
| 21 | General | General | General inquiry, Other |

**Totals:** 4 categories, 8 starter ServiceItems

### Tier 2 — Extended (Demo Only)

Seeded by `SeedDemoData.seedAllDemoData()` on top of Tier 1. These are product-specific categories that vary by PSP and are better added manually via Sequence Builder for real clients. Including them in demo data showcases the system's full categorization capability.

| Cat ID | Category | Short | Starter ServiceItems |
|--------|----------|-------|----------------------|
| 13 | Debit Card | Debit | Debit card not working, Debit card replacement |
| 14 | COBRA | COBRA | COBRA enrollment, COBRA payment issue |
| 15 | HSA | HSA | HSA contribution question, HSA eligible expense question |
| 16 | Enrollment | Enroll | New hire enrollment, Open enrollment, Qualifying life event |
| 17 | Plan Services | Plans | FSA question, HRA question, Plan quote request |

**Totals:** 5 categories, 12 starter ServiceItems

---

## 3. Current State (What Needs to Change)

### DatabaseInitializer currently seeds (Session 9 state):

```java
//Create Ticket Category + Service Item (PSP defines additional categories)
TicketCategory tc = createTicketCategory(em,1L,"General","GEN");
ServiceItem siTicket = createServiceItem(em,10,"General Ticket",1,tg3,psp);
siTicket = setHasRequiredTasks(em, siTicket, true);
setTicketCategoryOnServiceItem(em, siTicket, tc);
createRequiredTaskList(em,siTicket,psp);
```

Only 1 category (General, ID 1) and 1 ServiceItem ("General Ticket", ID 10). A new PSP sees only "General Ticket" in the Create Ticket dropdown.

### ReferenceDataSeeder has (but is NOT called from DatabaseInitializer):

`loadDefaultTicketCategories(em)` — seeds all 9 categories + all 20 starter items in one block. Not split into tiers.

### SeedDemoData currently:

Creates demo tickets but does not seed any additional categories or starter service items.

---

## 4. Implementation Plan

### Step 1: Split `ReferenceDataSeeder` into Two Methods

**File:** `src/main/java/net/superiorstate/ams/data/service/ReferenceDataSeeder.java`

Replace the existing `loadDefaultTicketCategories(em)` with two methods:

```java
/**
 * Tier 1 — Core ticket categories and starter ServiceItems for all PSPs.
 * Called from DatabaseInitializer.performInitialization().
 * Universal categories every benefits admin needs on day one.
 */
public static void loadCoreTicketCategories(EntityManager em, PSP psp) {

    // ── Core Categories ──
    createTicketCategory(em, 11L, "Claims",          "Claims");
    createTicketCategory(em, 12L, "Access / Online", "Access");
    createTicketCategory(em, 18L, "Billing",         "Billing");
    createTicketCategory(em, 21L, "General",         "General");

    // ── Core Starter ServiceItems (group 3, no sequence) ──
    ActivityCategory ticketGroup = EntityLookup.getTemplateGroupById(em, 3);
    createStarterServiceItem(em, ticketGroup, 11L, "Claim not paid", psp);
    createStarterServiceItem(em, ticketGroup, 11L, "Claim paid incorrectly", psp);
    createStarterServiceItem(em, ticketGroup, 12L, "Can't log in to portal", psp);
    createStarterServiceItem(em, ticketGroup, 12L, "Need online access", psp);
    createStarterServiceItem(em, ticketGroup, 18L, "Billing discrepancy", psp);
    createStarterServiceItem(em, ticketGroup, 18L, "Invoice request", psp);
    createStarterServiceItem(em, ticketGroup, 21L, "General inquiry", psp);
    createStarterServiceItem(em, ticketGroup, 21L, "Other", psp);
}

/**
 * Tier 2 — Extended ticket categories and starter ServiceItems for demo/conference data.
 * Called from SeedDemoData.seedAllDemoData().
 * Product-specific categories that real PSPs add manually via Sequence Builder.
 * Must be called AFTER loadCoreTicketCategories().
 */
public static void loadExtendedTicketCategories(EntityManager em, PSP psp) {

    // ── Extended Categories ──
    createTicketCategory(em, 13L, "Debit Card",      "Debit");
    createTicketCategory(em, 14L, "COBRA",           "COBRA");
    createTicketCategory(em, 15L, "HSA",             "HSA");
    createTicketCategory(em, 16L, "Enrollment",      "Enroll");
    createTicketCategory(em, 17L, "Plan Services",   "Plans");

    // ── Extended Starter ServiceItems ──
    ActivityCategory ticketGroup = EntityLookup.getTemplateGroupById(em, 3);
    createStarterServiceItem(em, ticketGroup, 13L, "Debit card not working", psp);
    createStarterServiceItem(em, ticketGroup, 13L, "Debit card replacement", psp);
    createStarterServiceItem(em, ticketGroup, 14L, "COBRA enrollment", psp);
    createStarterServiceItem(em, ticketGroup, 14L, "COBRA payment issue", psp);
    createStarterServiceItem(em, ticketGroup, 15L, "HSA contribution question", psp);
    createStarterServiceItem(em, ticketGroup, 15L, "HSA eligible expense question", psp);
    createStarterServiceItem(em, ticketGroup, 16L, "New hire enrollment", psp);
    createStarterServiceItem(em, ticketGroup, 16L, "Open enrollment", psp);
    createStarterServiceItem(em, ticketGroup, 16L, "Qualifying life event", psp);
    createStarterServiceItem(em, ticketGroup, 17L, "FSA question", psp);
    createStarterServiceItem(em, ticketGroup, 17L, "HRA question", psp);
    createStarterServiceItem(em, ticketGroup, 17L, "Plan quote request", psp);
}
```

Also update `createStarterServiceItem` to accept and set PSP:

```java
private static void createStarterServiceItem(EntityManager em, ActivityCategory ticketGroup,
                                              Long catId, String description, PSP psp) {
    TicketCategory tc = EntityLookup.getTicketCategoryById(em, catId);
    if (tc == null) return;
    em.getTransaction().begin();
    ServiceItem si = new ServiceItem();
    si.setDescription(description);
    si.setActivityCategory(ticketGroup);
    si.setTicketCategory(tc);
    si.setPsp(psp);
    si.setSourceType("SYSTEM");
    si.setSortOrder(100);
    em.persist(si);
    em.getTransaction().commit();
}
```

**Remove** the old `loadDefaultTicketCategories(EntityManager em)` method entirely to prevent confusion.

### Step 2: Wire Tier 1 into `DatabaseInitializer.performInitialization()`

**File:** `src/main/java/net/superiorstate/ams/data/service/DatabaseInitializer.java`

**Replace** the existing single-category ticket seeding block:

```java
//Create Ticket Category + Service Item (PSP defines additional categories)
TicketCategory tc = createTicketCategory(em,1L,"General","GEN");
ServiceItem siTicket = createServiceItem(em,10,"General Ticket",1,tg3,psp);
siTicket = setHasRequiredTasks(em, siTicket, true);
setTicketCategoryOnServiceItem(em, siTicket, tc);
createRequiredTaskList(em,siTicket,psp);
```

**With:**

```java
//Create Core Ticket Categories + Starter ServiceItems
// Seeds 4 universal categories (Claims, Access, Billing, General) and 8 starter
// dropdown items. PSP adds product-specific categories via Sequence Builder.
ReferenceDataSeeder.loadCoreTicketCategories(em, psp);

// Create the "General Ticket" ServiceItem with a RequiredTaskList.
// This is the default service item used by the initialization note (Ticket 99).
TicketCategory tcGeneral = EntityLookup.getTicketCategoryById(em, 21L);
ServiceItem siTicket = createServiceItem(em, 10, "General Ticket", 1, tg3, psp);
siTicket = setHasRequiredTasks(em, siTicket, true);
setTicketCategoryOnServiceItem(em, siTicket, tcGeneral);
createRequiredTaskList(em, siTicket, psp);
```

**Key changes:**
- Old `createTicketCategory(em, 1L, "General", "GEN")` removed — General is now seeded as ID 21 by `loadCoreTicketCategories`
- `siTicket` links to category ID 21 (matches production) instead of old ID 1
- 8 starter ServiceItems immediately available in Create Ticket dropdown

### Step 3: Wire Tier 2 into `SeedDemoData.seedAllDemoData()`

**File:** `src/main/java/net/superiorstate/ams/controller/home/SeedDemoData.java`

Add the Tier 2 call near the **top** of `seedAllDemoData()`, before creating demo tickets, so the extended categories and service items are available for demo ticket assignments:

```java
public void seedAllDemoData(EntityManager em, PrintWriter out) {
    PSP psp = EntityLookup.getPspById(em, 4L);
    Person adminPerson = EntityLookup.getPersonById(em, 104L);

    if (psp == null || adminPerson == null) {
        throw new IllegalStateException("Database not initialized — PSP or admin person not found.");
    }

    log(out, "<strong>Starting demo data seed...</strong>");

    // ═══════════════════════════════════════════
    //  EXTENDED TICKET CATEGORIES (Tier 2)
    // ═══════════════════════════════════════════
    log(out, "<h5 class='mt-3' style='color:#0d5681;'>Extended Ticket Categories</h5>");
    ReferenceDataSeeder.loadExtendedTicketCategories(em, psp);
    log(out, "Seeded 5 extended categories + 12 starter service items");

    // ... rest of existing seedAllDemoData (employers, employees, benefits, etc.)
```

### Step 4: Verify `SeedDemoData` Ticket Creation

**File:** `src/main/java/net/superiorstate/ams/controller/home/SeedDemoData.java`

Review the demo ticket creation section. Demo tickets that reference product-specific categories (COBRA, HSA, etc.) should use the Tier 2 category IDs now seeded by Step 3. Verify:

- No hardcoded references to old category ID 1
- Demo tickets that use categories 13–17 are created **after** the `loadExtendedTicketCategories` call
- Any `EntityLookup.getTicketCategoryById(em, id)` calls in the demo ticket creation use IDs from the 11–21 range

### Step 5: Verify ReSeedDb / ReSeedDemoData Compatibility

**No code changes needed.** The existing flow handles this correctly:

- **ReSeedDb** → truncates → calls `DatabaseInitializer.performInitialization()` → gets Tier 1 only ✅
- **ReSeedDemoData** → truncates → calls `performInitialization()` → then calls `SeedDemoData.seedAllDemoData()` → gets Tier 1 + Tier 2 ✅

### Step 6: Verify `AmsDataGlobal` Cache Loading

**File:** `src/main/java/net/superiorstate/ams/data/AmsDataGlobal.java`

On startup, `initializeGlobalData(em)` loads:
- `ticketCategories` — all rows from `TicketCategory` table
- `ticketServiceItems` — from `TicketQueryDAO.getActiveTicketServiceItems(em)`

These queries load whatever exists in the tables with no ID filtering. No changes needed — after a fresh init, globals will have 4 categories + 9 service items (8 starters + 1 "General Ticket"); after demo seed, they'll have 9 categories + 21 service items.

Note: `SeedDemoData` already calls `global.initializeGlobalData(em)` at the end to refresh caches after seeding.

---

## 5. ID Allocation Summary (Post-Implementation)

### TicketCategory IDs

| ID | Category | Tier | Seeded By |
|----|----------|------|-----------|
| 11 | Claims | Core | DatabaseInitializer → ReferenceDataSeeder |
| 12 | Access / Online | Core | DatabaseInitializer → ReferenceDataSeeder |
| 13 | Debit Card | Extended | SeedDemoData → ReferenceDataSeeder |
| 14 | COBRA | Extended | SeedDemoData → ReferenceDataSeeder |
| 15 | HSA | Extended | SeedDemoData → ReferenceDataSeeder |
| 16 | Enrollment | Extended | SeedDemoData → ReferenceDataSeeder |
| 17 | Plan Services | Extended | SeedDemoData → ReferenceDataSeeder |
| 18 | Billing | Core | DatabaseInitializer → ReferenceDataSeeder |
| 21 | General | Core | DatabaseInitializer → ReferenceDataSeeder |

All IDs match production. Old ID 1 ("General"/"GEN") is no longer seeded.

### ServiceItem IDs (ticket group 3)

| ID | Description | Source |
|----|-------------|--------|
| 10 | General Ticket (with RequiredTaskList) | DatabaseInitializer (hardcoded ID) |
| auto | 8 core starters | ReferenceDataSeeder.loadCoreTicketCategories (auto-increment) |
| auto | 12 extended starters | ReferenceDataSeeder.loadExtendedTicketCategories (auto-increment) |

---

## 6. What Each Init Path Produces

| Path | Categories | Starter SIs | "General Ticket" w/ RTL | Demo Data |
|------|-----------|-------------|------------------------|-----------|
| Fresh Init | 4 (Core) | 8 | ✅ | ❌ |
| ReSeedDb | 4 (Core) | 8 | ✅ | ❌ |
| SeedDemoData | 9 (Core + Extended) | 20 | ✅ | ✅ |
| ReSeedDemoData | 9 (Core + Extended) | 20 | ✅ | ✅ |

---

## 7. Testing Checklist

### Fresh Initialize (dev_ssa)
1. Drop and recreate empty `dev_ssa` schema from baseline dump
2. Start Tomcat, navigate to `/initialize.jsp`, fill form, submit
3. **Verify:**
   - [ ] 4 TicketCategory rows exist (IDs 11, 12, 18, 21)
   - [ ] 9 ServiceItems in group 3 exist (1 "General Ticket" + 8 core starters)
   - [ ] "General Ticket" (ID 10) has `hasRequiredTasks = true` and a linked RequiredTaskList
   - [ ] All 8 core starters have `sourceType = 'SYSTEM'`, `psp_id` populated, no RequiredTaskList
   - [ ] Ticket 99 (initialization note) has `ticketServiceItem` pointing to ID 10
   - [ ] Create Ticket dropdown shows 8 starter items grouped under 4 categories
   - [ ] Categories 13–17 do NOT exist
   - [ ] Sequence Builder shows "General Ticket" sequence under Ticket tab

### Factory Reset (ReSeedDb)
1. Run `/ReSeedDb` with deployment key
2. **Verify:** Same results as fresh initialize above — only Tier 1 data

### Demo Seed (SeedDemoData on top of fresh init)
1. After fresh init, run `/SeedDemoData`
2. **Verify:**
   - [ ] 9 TicketCategory rows exist (IDs 11–18, 21)
   - [ ] 21 ServiceItems in group 3 exist (1 "General Ticket" + 8 core + 12 extended starters)
   - [ ] Create Ticket dropdown shows all 20 starter items grouped under 9 categories
   - [ ] Demo tickets exist with valid `ticketServiceItem` references
   - [ ] Demo users can log in and create tickets

### Factory Reset + Demo (ReSeedDemoData)
1. Run `/ReSeedDemoData` with deployment key
2. **Verify:** Same results as Demo Seed above

### Production Comparison
1. Compare `SELECT * FROM ticketcategory` between seeded dev_ssa and production
2. **Verify:** Production has all 9 categories (IDs 11–21) — matches demo-seeded state

---

## 8. Files to Modify

| File | Change |
|------|--------|
| `src/main/java/net/superiorstate/ams/data/service/ReferenceDataSeeder.java` | Replace `loadDefaultTicketCategories(em)` with `loadCoreTicketCategories(em, psp)` and `loadExtendedTicketCategories(em, psp)`; add `PSP psp` param to `createStarterServiceItem`; remove old method |
| `src/main/java/net/superiorstate/ams/data/service/DatabaseInitializer.java` | Replace single-category ticket seed with `ReferenceDataSeeder.loadCoreTicketCategories(em, psp)` call; link "General Ticket" SI to category ID 21 instead of old ID 1; remove `createTicketCategory(em, 1L, ...)` |
| `src/main/java/net/superiorstate/ams/controller/home/SeedDemoData.java` | Add `ReferenceDataSeeder.loadExtendedTicketCategories(em, psp)` call near top of `seedAllDemoData()`; verify demo tickets use IDs 11–21 |

**No migration script needed** — this is purely Java seed-data logic, not a schema change. The `ticketcategory` and `templatepurpose` table structures are unchanged.

---

## 9. Risks and Considerations

1. **ID 10 conflict:** The "General Ticket" ServiceItem uses hardcoded ID 10 via `createServiceItem(em, 10, ...)`. If auto-increment for `templatepurpose` has advanced past 10 in any environment, this could conflict. The existing `createServiceItem` method checks for existing before creating, but verify.

2. **TicketCategory ID 1 in existing dev databases:** Any dev databases initialized before Session 9 may have tickets referencing `ticketcategory.category_id = 1`. A `ReSeedDb` operation cleans this up (truncates everything).

3. **Ordering dependency:** `loadCoreTicketCategories` must be called **after** `createTemplateGroup(em, 3, "Ticket")` because it does `EntityLookup.getTemplateGroupById(em, 3)`. The current placement in `performInitialization` satisfies this — the ActivityCategory creation block runs before the ticket seeding block.

4. **SeedDemoData idempotency:** `loadExtendedTicketCategories` does not check for existing categories before creating. The `DEMO_DATA_SEEDED` constant guard in `SeedDemoData.doGet()` prevents double-runs. If the guard is bypassed, duplicate categories would be created. The `createTicketCategory` method uses manual IDs and `em.persist()` which would throw a PK violation — this is acceptable as a natural guard.

5. **Real PSP customization path:** After Tier 1 init, a real PSP adds product-specific categories through Sequence Builder → "New Category" flow in `SequenceAction25`. Those categories get auto-increment IDs (not the hardcoded 13–17 range). This is fine — the hardcoded IDs are only used in demo seeding.
