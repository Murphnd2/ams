# Session Summary — February 21, 2026
**Topics:** Resource Library, Feature Management in Service Manager, Proposal Feature Rendering, Rate Suppress Fix

---

## 1. Resource Library — COMPLETED ✅

### New Entity: ResourceCategory
- `model/sales/offering/ResourceCategory.java` — JPA entity for organizing library resources
- Table: `resourcecategory` (category_id PK, name, sort_order, psp_id FK)

### Modified Entity: MarketingMaterial
- Added `category` ManyToOne FK → ResourceCategory (`category_id`)
- Widened `storage_guid` from VARCHAR(36) → VARCHAR(50) to accommodate UUID.extension pattern

### New Servlets
- `controller/activity/setup/LibraryHome.java` — GET servlet for Resource Library page. Loads categories and materials (with category eager-fetched). Handles category filter via `?categoryId=X`.
- `controller/activity/setup/LibraryAction.java` — POST servlet for Resource Library CRUD. Actions: `createCategory`, `editCategory`, `deleteCategory`, `createResource`, `editResource`, `deleteResource`. Multipart-enabled. Validates file extensions (PDF, XLSX, DOCX, CSV). Uploads to Wasabi via StorageDAO with Content-Disposition header.

### Modified Servlet: ShowFileUpload
- Added fallback to `MarketingMaterial.storageGuid` when `WebLink.linkPath` returns null
- Enables library resource downloads via existing `ShowFileUpload?doc=GUID` pattern

### New JSP
- `webapp/WEB-INF/view/sales/library25.jsp` — Resource Library UI. 2-column layout matching serviceManager25.jsp pattern. Category filter pills, scrollable resource list, detail panel, modals for add/edit/delete resources and manage categories.

### Modified JSP: adminNav.jsp
- Added Resource Library menu item (bi-collection icon, `LibraryHome` URL)

### Database Changes (APPLIED to beta_ssa)
```sql
CREATE TABLE resourcecategory (
    category_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    psp_id BIGINT NOT NULL,
    FOREIGN KEY (psp_id) REFERENCES assignee(id)
);

ALTER TABLE marketingmaterial ADD COLUMN category_id BIGINT NULL;
ALTER TABLE marketingmaterial ADD CONSTRAINT fk_mm_category 
    FOREIGN KEY (category_id) REFERENCES resourcecategory(category_id);

ALTER TABLE marketingmaterial MODIFY COLUMN storage_guid VARCHAR(50);
```

---

## 2. Feature Management in Service Manager — COMPLETED ✅

### Modified Entity: Feature
- Added `libraryResource` ManyToOne FK → MarketingMaterial (`material_id`)
- Added getter/setter for `libraryResource`

### Modified Servlet: ServiceManagerHome
- Loads `libraryResources` list (all MarketingMaterial for PSP) for feature→resource dropdown
- When LOS selected: finds ServiceModule via `findModuleByLos()`, loads features via `getFeaturesForModule()` with `LEFT JOIN FETCH f.libraryResource`
- When Enhancement selected: same via `findModuleByEnhancement()`
- Sets `selectedModule`, `featureList`, `libraryResources` as request attributes

### Modified Servlet: ServiceManagerAction
- Added 3 new actions: `createFeature`, `editFeature`, `deleteFeature`
- `createFeature`: takes moduleId, description, optional libraryResourceId
- `editFeature`: updates description and libraryResource (can clear by sending empty)
- `deleteFeature`: removes Feature entity

### Modified Servlet: ServiceManagerSort
- Added `case "feature"` to update `Feature.sortOrder` for drag-and-drop reorder

### Modified JSP: serviceManager25.jsp
- Features card added to both LOS and Enhancement selected blocks (after Application Sections)
- Features are drag-sortable (SortableJS with `feature-item` class, `losFeatureList`/`enhFeatureList` container IDs)
- Each feature row: drag handle, green check icon, description text, optional linked resource badge, edit pencil, delete X
- Add Feature modal: textarea with inline link toolbar (select text → pick resource → "Link Selection" wraps as `[text](resourceId)`), plus separate "Linked Resource (icon at end)" dropdown
- Edit Feature modal: same layout, pre-populated via `openEditFeature()` JS function
- `insertLink()` JS function: wraps textarea selection as `[text](resourceId)` markdown-style markers

### Database Changes (APPLIED to beta_ssa)
```sql
ALTER TABLE feature ADD COLUMN material_id BIGINT NULL;
ALTER TABLE feature ADD CONSTRAINT fk_feature_material 
    FOREIGN KEY (material_id) REFERENCES marketingmaterial(material_id);
```

---

## 3. Proposal Feature Rendering — COMPLETED ✅

### Modified Servlet: ViewProposal
- Eagerly fetches `libraryResource` with features: `LEFT JOIN FETCH f.libraryResource`
- Collects all referenced resource IDs from inline `[text](id)` markers AND `libraryResource` FKs
- Builds `resourceUrlMap` (id → download URL) and `resourceTypeMap` (id → DOCUMENT/VIDEO/LINK)
- `renderFeatureHtml()`: converts `[text](id)` markers to `<a>` tags, appends end-icon based on file type (PDF, spreadsheet, Word, video, external link icons)
- Sets `renderedFeatures` map (featureId → rendered HTML) as request attribute

### Modified JSP: viewProposal.jsp
- In both LOS and Enhancement feature blocks, changed `${feature.getDescription()}` → `${renderedFeatures[feature.getId()]}` to render pre-built HTML with links and icons

---

## 4. Rate Suppress When Locked — COMPLETED ✅

### Modified Servlet: RateTableAction
- Added `suppressRate` action: toggles `isSuppressed` flag regardless of lock status. Clears selection after suppress.

### Modified JSP: rateManager25.jsp
- Added eye-slash suppress button inline next to rate name (always visible, not gated by `!isLocked`)
- Confirm dialog explains it won't affect existing proposals

---

## Files Changed This Session

### New Files
| File | Type |
|------|------|
| `model/sales/offering/ResourceCategory.java` | Entity |
| `controller/activity/setup/LibraryHome.java` | Servlet |
| `controller/activity/setup/LibraryAction.java` | Servlet |
| `webapp/WEB-INF/view/sales/library25.jsp` | JSP |

### Modified Files
| File | Change |
|------|--------|
| `model/sales/offering/Feature.java` | Added `libraryResource` ManyToOne FK |
| `model/sales/offering/MarketingMaterial.java` | Added `category` ManyToOne FK |
| `controller/activity/setup/ServiceManagerHome.java` | Feature + library resource loading |
| `controller/activity/setup/ServiceManagerAction.java` | Feature CRUD actions |
| `controller/activity/setup/ServiceManagerSort.java` | Feature sort type |
| `controller/activity/setup/ViewProposal.java` | Feature HTML rendering with inline links |
| `controller/activity/setup/ShowFileUpload.java` | MarketingMaterial fallback |
| `controller/activity/setup/RateTableAction.java` | `suppressRate` action |
| `webapp/WEB-INF/view/sales/serviceManager25.jsp` | Features card, modals, drag sort, inline link UI |
| `webapp/WEB-INF/view/sales/viewProposal.jsp` | Rendered feature HTML |
| `webapp/WEB-INF/view/sales/rateManager25.jsp` | Suppress button for locked rates |
| `webapp/WEB-INF/view/sales/adminNav.jsp` | Resource Library menu item |
