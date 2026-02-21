# serviceManager25.jsp — Feature Management Additions

## 3 Changes Required:

---

### CHANGE 1: Insert Features card in LOS selected block

Find this line (end of Application Sections card for LOS, around line ~130-ish):
```
                    </div>
                </c:if>

                <%-- STATE: Enhancement Selected --%>
```

INSERT the Features card BEFORE `</c:if>` and BEFORE `<%-- STATE: Enhancement Selected --%>`:

```jsp
                    <%-- Features for this LOS --%>
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-check2-square me-1"></i>Features</span>
                            <c:if test="${not empty selectedModule}">
                                <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addFeatureModal" title="Add feature"><i class="bi bi-plus-lg"></i></button>
                            </c:if>
                        </div>
                        <div class="card-body py-2 px-3">
                            <c:choose>
                                <c:when test="${empty selectedModule}">
                                    <span class="text-muted" style="font-size:0.85rem;">No pricing module exists yet. Add pricing in Rate Manager first.</span>
                                </c:when>
                                <c:when test="${not empty featureList}">
                                    <c:forEach var="feature" items="${featureList}">
                                        <div class="assoc-row d-flex justify-content-between align-items-center">
                                            <div class="flex-grow-1">
                                                <i class="bi bi-check2 me-1" style="color: var(--ssa-alt);"></i>
                                                <span>${feature.getDescription()}</span>
                                                <c:if test="${not empty feature.getLibraryResource()}">
                                                    <span class="badge bg-light text-dark ms-1" style="font-size:0.7rem;">
                                                        <i class="bi bi-link-45deg"></i> ${feature.getLibraryResource().getTitle()}
                                                    </span>
                                                </c:if>
                                            </div>
                                            <div class="d-flex align-items-center gap-1">
                                                <button type="button" class="btn-remove" style="color: var(--ssa);"
                                                        onclick="openEditFeature(${feature.getId()}, '${feature.getDescription().replace("'", "\\'")}', '${not empty feature.getLibraryResource() ? feature.getLibraryResource().getId() : ''}')"
                                                        title="Edit"><i class="bi bi-pencil"></i></button>
                                                <form method="post" action="ServiceManagerAction" class="d-inline" onsubmit="return confirm('Delete this feature?');">
                                                    <input type="hidden" name="action" value="deleteFeature"/>
                                                    <input type="hidden" name="featureId" value="${feature.getId()}"/>
                                                    <input type="hidden" name="losId" value="${selectedLos.getId()}"/>
                                                    <button type="submit" class="btn-remove"><i class="bi bi-x-lg"></i></button>
                                                </form>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><span class="text-muted" style="font-size:0.85rem;">No features yet — click + to add</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
```

---

### CHANGE 2: Insert Features card in Enhancement selected block

Same card, but just before the `</c:if>` that closes `selectedEnhancement`. Find the end of the Enhancement's Application Sections card and insert the same block but with `enhId` instead of `losId`:

```jsp
                    <%-- Features for this Enhancement --%>
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-check2-square me-1"></i>Features</span>
                            <c:if test="${not empty selectedModule}">
                                <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addFeatureModal" title="Add feature"><i class="bi bi-plus-lg"></i></button>
                            </c:if>
                        </div>
                        <div class="card-body py-2 px-3">
                            <c:choose>
                                <c:when test="${empty selectedModule}">
                                    <span class="text-muted" style="font-size:0.85rem;">No pricing module exists yet. Add pricing in Rate Manager first.</span>
                                </c:when>
                                <c:when test="${not empty featureList}">
                                    <c:forEach var="feature" items="${featureList}">
                                        <div class="assoc-row d-flex justify-content-between align-items-center">
                                            <div class="flex-grow-1">
                                                <i class="bi bi-check2 me-1" style="color: var(--ssa-alt);"></i>
                                                <span>${feature.getDescription()}</span>
                                                <c:if test="${not empty feature.getLibraryResource()}">
                                                    <span class="badge bg-light text-dark ms-1" style="font-size:0.7rem;">
                                                        <i class="bi bi-link-45deg"></i> ${feature.getLibraryResource().getTitle()}
                                                    </span>
                                                </c:if>
                                            </div>
                                            <div class="d-flex align-items-center gap-1">
                                                <button type="button" class="btn-remove" style="color: var(--ssa);"
                                                        onclick="openEditFeature(${feature.getId()}, '${feature.getDescription().replace("'", "\\'")}', '${not empty feature.getLibraryResource() ? feature.getLibraryResource().getId() : ''}')"
                                                        title="Edit"><i class="bi bi-pencil"></i></button>
                                                <form method="post" action="ServiceManagerAction" class="d-inline" onsubmit="return confirm('Delete this feature?');">
                                                    <input type="hidden" name="action" value="deleteFeature"/>
                                                    <input type="hidden" name="featureId" value="${feature.getId()}"/>
                                                    <input type="hidden" name="enhId" value="${selectedEnhancement.getId()}"/>
                                                    <button type="submit" class="btn-remove"><i class="bi bi-x-lg"></i></button>
                                                </form>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><span class="text-muted" style="font-size:0.85rem;">No features yet — click + to add</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
```

---

### CHANGE 3: Add Feature modals + JS function

**A) Add these two modals** after the existing Edit Enhancement modal block (before `<script>`):

```jsp
<%-- Add Feature Modal --%>
<c:if test="${not empty selectedModule}">
<div class="modal fade" id="addFeatureModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="ServiceManagerAction">
        <input type="hidden" name="action" value="createFeature"/>
        <input type="hidden" name="moduleId" value="${selectedModule.getId()}"/>
        <c:if test="${not empty selectedLos}"><input type="hidden" name="losId" value="${selectedLos.getId()}"/></c:if>
        <c:if test="${not empty selectedEnhancement}"><input type="hidden" name="enhId" value="${selectedEnhancement.getId()}"/></c:if>
        <div class="modal-header">
            <h5 class="modal-title"><i class="bi bi-plus-circle me-2"></i>Add Feature</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div class="mb-3">
                <label class="form-label fw-semibold">Feature Text <span class="text-danger">*</span></label>
                <textarea name="description" class="form-control" rows="2" required maxlength="500" placeholder="e.g. Online Web Access and Claim Filing"></textarea>
            </div>
            <div class="mb-3">
                <label class="form-label fw-semibold">Linked Resource <small class="text-muted fw-normal">(optional)</small></label>
                <select name="libraryResourceId" class="form-select">
                    <option value="">-- None --</option>
                    <c:forEach var="res" items="${libraryResources}">
                        <option value="${res.getId()}">${res.getTitle()}<c:if test="${not empty res.getCategory()}"> (${res.getCategory().getName()})</c:if></option>
                    </c:forEach>
                </select>
            </div>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
            <button type="submit" class="btn btn-primary">Add Feature</button>
        </div>
    </form>
</div></div></div>
</c:if>

<%-- Edit Feature Modal --%>
<c:if test="${not empty selectedModule}">
<div class="modal fade" id="editFeatureModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="ServiceManagerAction">
        <input type="hidden" name="action" value="editFeature"/>
        <input type="hidden" name="featureId" id="editFeatureId"/>
        <c:if test="${not empty selectedLos}"><input type="hidden" name="losId" value="${selectedLos.getId()}"/></c:if>
        <c:if test="${not empty selectedEnhancement}"><input type="hidden" name="enhId" value="${selectedEnhancement.getId()}"/></c:if>
        <div class="modal-header">
            <h5 class="modal-title"><i class="bi bi-pencil me-2"></i>Edit Feature</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div class="mb-3">
                <label class="form-label fw-semibold">Feature Text <span class="text-danger">*</span></label>
                <textarea name="description" id="editFeatureDesc" class="form-control" rows="2" required maxlength="500"></textarea>
            </div>
            <div class="mb-3">
                <label class="form-label fw-semibold">Linked Resource <small class="text-muted fw-normal">(optional)</small></label>
                <select name="libraryResourceId" id="editFeatureResId" class="form-select">
                    <option value="">-- None --</option>
                    <c:forEach var="res" items="${libraryResources}">
                        <option value="${res.getId()}">${res.getTitle()}<c:if test="${not empty res.getCategory()}"> (${res.getCategory().getName()})</c:if></option>
                    </c:forEach>
                </select>
            </div>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
            <button type="submit" class="btn btn-primary">Save</button>
        </div>
    </form>
</div></div></div>
</c:if>
```

**B) Add this JS function** inside the existing `<script>` block (anywhere before the closing `</script>`):

```javascript
    function openEditFeature(id, desc, resId) {
        document.getElementById('editFeatureId').value = id;
        document.getElementById('editFeatureDesc').value = desc;
        document.getElementById('editFeatureResId').value = resId || '';
        new bootstrap.Modal(document.getElementById('editFeatureModal')).show();
    }
```
