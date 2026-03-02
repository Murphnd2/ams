<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Resource Library</title>
    <style>
        .item-scroll { max-height: 500px; overflow-y: auto; }
        .detail-scroll { overflow-y: auto; }
        @media (min-width: 992px) { .detail-scroll { max-height: calc(100vh - 200px); } }
        .cat-pill { display: inline-block; padding: 0.25rem 0.65rem; border-radius: 20px; font-size: 0.8rem; cursor: pointer; text-decoration: none; border: 1px solid #dee2e6; color: #495057; margin: 0.15rem; transition: all 0.15s; }
        .cat-pill:hover { border-color: #0d5681; color: #0d5681; }
        .cat-pill.active { background: #0d5681; color: white; border-color: #0d5681; }
        .type-badge { font-size: 0.7rem; padding: 0.15rem 0.4rem; border-radius: 3px; font-weight: 600; text-transform: uppercase; }
        .type-badge.document { background: #fde8e8; color: #c0392b; }
        .type-badge.video { background: #e8f4fd; color: #2980b9; }
        .type-badge.link { background: #e8fde8; color: #27ae60; }
        .ext-badge { font-size: 0.65rem; padding: 0.1rem 0.3rem; border-radius: 3px; font-weight: 700; text-transform: uppercase; background: #f0f0f0; color: #555; margin-left: 0.25rem; }
        .field-label { font-weight: 600; font-size: 0.85rem; color: #495057; }
        .resource-meta { font-size: 0.8rem; color: #6c757d; }
    </style>
</head>
<body>
<div class="container-fluid">
    <c:set var="pageTitle" value="Resource Library" scope="request"/>
    <c:set var="pageIcon" value="bi-collection" scope="request"/>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <%-- ======================== ERROR ALERT ======================== --%>
    <c:if test="${not empty param.error}">
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            <i class="bi bi-exclamation-triangle me-1"></i>${param.error}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    </c:if>

    <div class="row g-3 mt-3">

        <%-- ======================== LEFT COLUMN ======================== --%>
        <div class="col-lg-4">
            <div class="card">
                <div class="hdr-bar d-flex justify-content-between align-items-center">
                    <span><i class="bi bi-collection me-1"></i>Resources</span>
                    <div>
                        <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#manageCategoriesModal" title="Manage categories"><i class="bi bi-tags"></i></button>
                        <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addResourceModal" title="Add resource"><i class="bi bi-plus-lg"></i></button>
                    </div>
                </div>

                <%-- Category filter pills --%>
                <div class="px-3 py-2" style="border-bottom: 1px solid #dee2e6;">
                    <a href="LibraryHome" class="cat-pill ${empty activeCatId ? 'active' : ''}">All</a>
                    <c:forEach var="cat" items="${categoryList}">
                        <a href="LibraryHome?catId=${cat.getId()}" class="cat-pill ${activeCatId == cat.getId() ? 'active' : ''}">
                            <i class="${cat.getIconClass()} me-1"></i>${cat.getName()}
                        </a>
                    </c:forEach>
                </div>

                <%-- Resource list --%>
                <div class="item-scroll" id="resourceScroll">
                    <c:choose>
                        <c:when test="${not empty resourceList}">
                            <c:forEach var="res" items="${resourceList}">
                                <%-- Derive file extension from storageGuid for display --%>
                                <c:set var="resExt" value=""/>
                                <c:if test="${not empty res.getStorageGuid() && res.getStorageGuid().contains('.')}">
                                    <c:set var="resExt" value="${fn:substringAfter(res.getStorageGuid(), '.')}"/>
                                </c:if>
                                <a href="LibraryHome?resId=${res.getId()}${not empty activeCatId ? '&catId='.concat(activeCatId) : ''}"
                                   class="d-block text-decoration-none text-dark" data-id="${res.getId()}">
                                    <div class="item-card p-2 ps-3 ${selectedResource != null && selectedResource.getId() == res.getId() ? 'active' : ''}">
                                        <div class="d-flex align-items-center">
                                            <c:choose>
                                                <c:when test="${res.getMaterialType() == 'VIDEO'}">
                                                    <i class="bi bi-camera-video me-2" style="color: #2980b9; font-size: 1.1rem;"></i>
                                                </c:when>
                                                <c:when test="${res.getMaterialType() == 'LINK'}">
                                                    <i class="bi bi-box-arrow-up-right me-2" style="color: #27ae60; font-size: 1.1rem;"></i>
                                                </c:when>
                                                <c:when test="${not empty res.getCategory()}">
                                                    <i class="${res.getCategory().getIconClass()} me-2" style="color: var(--ssa); font-size: 1.1rem;"></i>
                                                </c:when>
                                                <c:otherwise>
                                                    <i class="bi bi-file-earmark me-2" style="color: #adb5bd; font-size: 1.1rem;"></i>
                                                </c:otherwise>
                                            </c:choose>
                                            <div class="flex-grow-1 min-width-0">
                                                <div class="fw-semibold text-truncate">${res.getTitle()}</div>
                                                <div class="resource-meta d-flex align-items-center gap-2">
                                                    <span class="type-badge ${fn:toLowerCase(res.getMaterialType())}">${res.getMaterialType()}</span>
                                                    <c:if test="${not empty resExt}"><span class="ext-badge">${resExt}</span></c:if>
                                                    <c:if test="${not empty res.getCategory()}">
                                                        <span>${res.getCategory().getName()}</span>
                                                    </c:if>
                                                </div>
                                            </div>
                                        </div>
                                    </div>
                                </a>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <div class="empty-state py-4">
                                <i class="bi bi-collection"></i>
                                <p class="mb-0 mt-2">No resources yet</p>
                                <small class="text-muted">Click + to add your first resource</small>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <%-- ======================== RIGHT COLUMN ======================== --%>
        <div class="col-lg-8">
            <div class="detail-scroll">

                <c:choose>
                    <c:when test="${not empty selectedResource}">
                        <%-- Derive file extension for detail display --%>
                        <c:set var="selExt" value=""/>
                        <c:if test="${not empty selectedResource.getStorageGuid() && selectedResource.getStorageGuid().contains('.')}">
                            <c:set var="selExt" value="${fn:toUpperCase(fn:substringAfter(selectedResource.getStorageGuid(), '.'))}"/>
                        </c:if>

                        <%-- Resource Detail Card --%>
                        <div class="card mb-3">
                            <div class="hdr-bar d-flex justify-content-between align-items-center">
                                <span>
                                    <c:if test="${not empty selectedResource.getCategory()}"><i class="${selectedResource.getCategory().getIconClass()} me-1"></i></c:if>
                                    <c:if test="${empty selectedResource.getCategory()}"><i class="bi bi-info-circle me-1"></i></c:if>
                                    ${selectedResource.getTitle()}
                                </span>
                                <div>
                                    <a href="#" class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#editResourceModal"><i class="bi bi-pencil me-1"></i>Edit</a>
                                    <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#deleteResourceModal"><i class="bi bi-trash me-1"></i>Delete</button>
                                </div>
                            </div>
                            <div class="card-body">
                                <div class="row mb-3">
                                    <div class="col-md-6">
                                        <div class="field-label">Title</div>
                                        <div>${selectedResource.getTitle()}</div>
                                    </div>
                                    <div class="col-md-3">
                                        <div class="field-label">Type</div>
                                        <span class="type-badge ${fn:toLowerCase(selectedResource.getMaterialType())}">${selectedResource.getMaterialType()}</span>
                                        <c:if test="${not empty selExt}"><span class="ext-badge">${selExt}</span></c:if>
                                    </div>
                                    <div class="col-md-3">
                                        <div class="field-label">Audience</div>
                                        <div>${not empty selectedResource.getAudience() ? selectedResource.getAudience() : 'Any'}</div>
                                    </div>
                                </div>
                                <div class="row mb-3">
                                    <div class="col-md-6">
                                        <div class="field-label">Category</div>
                                        <c:choose>
                                            <c:when test="${not empty selectedResource.getCategory()}">
                                                <i class="${selectedResource.getCategory().getIconClass()} me-1"></i>${selectedResource.getCategory().getName()}
                                            </c:when>
                                            <c:otherwise><span class="text-muted">Uncategorized</span></c:otherwise>
                                        </c:choose>
                                    </div>
                                    <div class="col-md-6">
                                        <div class="field-label">Description</div>
                                        <div>${not empty selectedResource.getDescription() ? selectedResource.getDescription() : '<span class="text-muted">No description</span>'}</div>
                                    </div>
                                </div>

                                <%-- Access / Preview --%>
                                <div class="row">
                                    <div class="col-12">
                                        <div class="field-label mb-1">Resource Link</div>
                                        <c:choose>
                                            <c:when test="${selectedResource.getMaterialType() == 'DOCUMENT' && not empty selectedResource.getStorageGuid()}">
                                                <a href="ShowFileUpload?doc=${selectedResource.getStorageGuid()}" target="_blank" class="btn btn-sm btn-outline-ssa">
                                                    <i class="bi bi-download me-1"></i>Download ${selExt}
                                                </a>
                                            </c:when>
                                            <c:when test="${selectedResource.getMaterialType() == 'VIDEO' && not empty selectedResource.getUrl()}">
                                                <a href="${selectedResource.getUrl()}" target="_blank" class="btn btn-sm btn-outline-ssa">
                                                    <i class="bi bi-camera-video me-1"></i>Watch Video
                                                </a>
                                            </c:when>
                                            <c:when test="${selectedResource.getMaterialType() == 'LINK' && not empty selectedResource.getUrl()}">
                                                <a href="${selectedResource.getUrl()}" target="_blank" class="btn btn-sm btn-outline-ssa">
                                                    <i class="bi bi-box-arrow-up-right me-1"></i>Open Link
                                                </a>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="text-muted">No link configured</span>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                            </div>
                        </div>

                    </c:when>
                    <c:otherwise>
                        <div class="card">
                            <div class="card-body">
                                <div class="empty-state py-5">
                                    <i class="bi bi-collection" style="font-size: 3rem;"></i>
                                    <h5 class="mt-3" style="color: #6c757d;">Resource Library</h5>
                                    <p class="text-muted mb-0">Select a resource from the list to view details,<br>or click <strong>+</strong> to add a new one.</p>
                                </div>
                            </div>
                        </div>
                    </c:otherwise>
                </c:choose>

            </div>
        </div>

    </div>
</div>

<%-- ======================== MODALS ======================== --%>

<%-- Add Resource Modal --%>
<div class="modal fade" id="addResourceModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="LibraryAction" enctype="multipart/form-data">
        <input type="hidden" name="action" value="createResource"/>
        <c:if test="${not empty activeCatId}"><input type="hidden" name="catId" value="${activeCatId}"/></c:if>
        <div class="modal-header">
            <h5 class="modal-title"><i class="bi bi-plus-circle me-2"></i>Add Resource</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div class="mb-3">
                <label class="form-label fw-semibold">Title <span class="text-danger">*</span></label>
                <input type="text" name="title" class="form-control" required maxlength="200" placeholder="e.g. FSA Compliance Guide">
            </div>
            <div class="mb-3">
                <label class="form-label fw-semibold">Description</label>
                <textarea name="description" class="form-control" rows="2" maxlength="500" placeholder="Brief description of this resource"></textarea>
            </div>
            <div class="row mb-3">
                <div class="col-md-4">
                    <label class="form-label fw-semibold">Type <span class="text-danger">*</span></label>
                    <select name="materialType" class="form-select" required id="addResType" onchange="toggleAddFields()">
                        <option value="DOCUMENT">Document</option>
                        <option value="VIDEO">Video</option>
                        <option value="LINK">Link</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label fw-semibold">Category</label>
                    <select name="categoryId" class="form-select">
                        <option value="">-- None --</option>
                        <c:forEach var="cat" items="${categoryList}">
                            <option value="${cat.getId()}">${cat.getName()}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label fw-semibold">Audience</label>
                    <select name="audience" class="form-select">
                        <option value="">-- Any --</option>
                        <option value="PROSPECT">Prospect</option>
                        <option value="CLIENT">Client</option>
                    </select>
                </div>
            </div>
            <div class="mb-3" id="addFileGroup">
                <label class="form-label fw-semibold">Upload File</label>
                <input type="file" name="fileUpload" class="form-control" accept=".pdf,.xlsx,.docx,.csv">
                <small class="text-muted">Accepted: .pdf, .xlsx, .docx, .csv</small>
            </div>
            <div class="mb-3 d-none" id="addUrlGroup">
                <label class="form-label fw-semibold">URL</label>
                <input type="url" name="url" class="form-control" placeholder="https://...">
            </div>
        </div>
        <div class="modal-footer justify-content-center border-0">
            <button type="submit" class="ssa-action save"><i class="bi bi-plus-circle me-1"></i>Add Resource</button>
            <span class="ssa-action-sep">|</span>
            <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
        </div>
    </form>
</div></div></div>

<%-- Edit Resource Modal --%>
<c:if test="${not empty selectedResource}">
<div class="modal fade" id="editResourceModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="LibraryAction" enctype="multipart/form-data">
        <input type="hidden" name="action" value="editResource"/>
        <input type="hidden" name="resId" value="${selectedResource.getId()}"/>
        <c:if test="${not empty activeCatId}"><input type="hidden" name="catId" value="${activeCatId}"/></c:if>
        <div class="modal-header">
            <h5 class="modal-title"><i class="bi bi-pencil me-2"></i>Edit Resource</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <div class="mb-3">
                <label class="form-label fw-semibold">Title <span class="text-danger">*</span></label>
                <input type="text" name="title" class="form-control" required maxlength="200" value="${selectedResource.getTitle()}">
            </div>
            <div class="mb-3">
                <label class="form-label fw-semibold">Description</label>
                <textarea name="description" class="form-control" rows="2" maxlength="500">${selectedResource.getDescription()}</textarea>
            </div>
            <div class="row mb-3">
                <div class="col-md-4">
                    <label class="form-label fw-semibold">Type <span class="text-danger">*</span></label>
                    <select name="materialType" class="form-select" required id="editResType" onchange="toggleEditFields()">
                        <option value="DOCUMENT" ${selectedResource.getMaterialType() == 'DOCUMENT' ? 'selected' : ''}>Document</option>
                        <option value="VIDEO" ${selectedResource.getMaterialType() == 'VIDEO' ? 'selected' : ''}>Video</option>
                        <option value="LINK" ${selectedResource.getMaterialType() == 'LINK' ? 'selected' : ''}>Link</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label fw-semibold">Category</label>
                    <select name="categoryId" class="form-select">
                        <option value="">-- None --</option>
                        <c:forEach var="cat" items="${categoryList}">
                            <option value="${cat.getId()}" ${not empty selectedResource.getCategory() && selectedResource.getCategory().getId() == cat.getId() ? 'selected' : ''}>${cat.getName()}</option>
                        </c:forEach>
                    </select>
                </div>
                <div class="col-md-4">
                    <label class="form-label fw-semibold">Audience</label>
                    <select name="audience" class="form-select">
                        <option value="">-- Any --</option>
                        <option value="PROSPECT" ${selectedResource.getAudience() == 'PROSPECT' ? 'selected' : ''}>Prospect</option>
                        <option value="CLIENT" ${selectedResource.getAudience() == 'CLIENT' ? 'selected' : ''}>Client</option>
                    </select>
                </div>
            </div>
            <div class="mb-3" id="editFileGroup">
                <label class="form-label fw-semibold">Replace File</label>
                <input type="file" name="fileUpload" class="form-control" accept=".pdf,.xlsx,.docx,.csv">
                <c:if test="${not empty selectedResource.getStorageGuid()}">
                    <small class="text-muted">Current: ${fn:toUpperCase(fn:substringAfter(selectedResource.getStorageGuid(), '.'))} file &mdash; upload new to replace</small>
                </c:if>
                <c:if test="${empty selectedResource.getStorageGuid()}">
                    <small class="text-muted">Accepted: .pdf, .xlsx, .docx, .csv</small>
                </c:if>
            </div>
            <div class="mb-3" id="editUrlGroup">
                <label class="form-label fw-semibold">URL</label>
                <input type="url" name="url" class="form-control" placeholder="https://..." value="${selectedResource.getUrl()}">
            </div>
        </div>
        <div class="modal-footer justify-content-center border-0">
            <button type="submit" class="ssa-action save"><i class="bi bi-check-lg me-1"></i>Save Changes</button>
            <span class="ssa-action-sep">|</span>
            <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
        </div>
    </form>
</div></div></div>

<%-- Delete Resource Confirmation --%>
<div class="modal fade" id="deleteResourceModal" tabindex="-1"><div class="modal-dialog modal-sm"><div class="modal-content">
    <form method="post" action="LibraryAction">
        <input type="hidden" name="action" value="deleteResource"/>
        <input type="hidden" name="resId" value="${selectedResource.getId()}"/>
        <c:if test="${not empty activeCatId}"><input type="hidden" name="catId" value="${activeCatId}"/></c:if>
        <div class="modal-header">
            <h5 class="modal-title text-danger"><i class="bi bi-exclamation-triangle me-2"></i>Delete</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body">
            <p>Delete <strong>${selectedResource.getTitle()}</strong>?</p>
            <c:if test="${not empty selectedResource.getStorageGuid()}">
                <p class="text-muted small">The uploaded file will also be removed from storage.</p>
            </c:if>
        </div>
        <div class="modal-footer justify-content-center border-0">
            <button type="submit" class="ssa-action danger"><i class="bi bi-trash me-1"></i>Delete</button>
            <span class="ssa-action-sep">|</span>
            <button type="button" class="ssa-action cancel" data-bs-dismiss="modal">Cancel</button>
        </div>
    </form>
</div></div></div>
</c:if>

<%-- Manage Categories Modal --%>
<div class="modal fade" id="manageCategoriesModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <div class="modal-header">
        <h5 class="modal-title"><i class="bi bi-tags me-2"></i>Manage Categories</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
    </div>
    <div class="modal-body">
        <table class="table table-sm table-hover mb-3">
            <thead><tr><th>Icon</th><th>Name</th><th></th></tr></thead>
            <tbody>
                <c:forEach var="cat" items="${categoryList}">
                    <tr>
                        <td><i class="${cat.getIconClass()}" style="font-size:1.1rem;color:var(--ssa);"></i></td>
                        <td>${cat.getName()}</td>
                        <td class="text-end">
                            <button type="button" class="btn btn-sm btn-outline-secondary py-0 px-1"
                                    onclick="openEditCategory(${cat.getId()}, '${cat.getName()}', '${cat.getIconClass()}')" title="Edit">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <form method="post" action="LibraryAction" class="d-inline" onsubmit="return confirm('Delete category ${cat.getName()}? Resources will become uncategorized.');">
                                <input type="hidden" name="action" value="deleteCategory"/>
                                <input type="hidden" name="catId" value="${cat.getId()}"/>
                                <button type="submit" class="btn btn-sm btn-outline-danger py-0 px-1"><i class="bi bi-trash"></i></button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty categoryList}">
                    <tr><td colspan="3" class="text-muted text-center">No categories yet</td></tr>
                </c:if>
            </tbody>
        </table>
        <hr>
        <h6>Add Category</h6>
        <form method="post" action="LibraryAction" class="row g-2 align-items-end">
            <input type="hidden" name="action" value="createCategory"/>
            <div class="col-5">
                <label class="form-label small mb-0">Name</label>
                <input type="text" name="name" class="form-control form-control-sm" required maxlength="50" placeholder="e.g. Webinar">
            </div>
            <div class="col-5">
                <label class="form-label small mb-0">Icon Class</label>
                <input type="text" name="iconClass" class="form-control form-control-sm" maxlength="50" placeholder="e.g. bi-play-circle" value="bi-folder">
            </div>
            <div class="col-2">
                <button type="submit" class="btn btn-sm btn-primary w-100">Add</button>
            </div>
        </form>
    </div>
    <div class="modal-footer justify-content-center border-0">
        <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Close</button>
    </div>
</div></div></div>

<%-- Edit Category Modal --%>
<div class="modal fade" id="editCategoryModal" tabindex="-1"><div class="modal-dialog modal-sm"><div class="modal-content">
    <form method="post" action="LibraryAction">
        <input type="hidden" name="action" value="editCategory"/>
        <input type="hidden" name="catId" id="editCatId"/>
        <div class="modal-header py-2">
            <h6 class="modal-title"><i class="bi bi-pencil me-1"></i>Edit Category</h6>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body py-2">
            <div class="mb-2">
                <label class="form-label small mb-0">Name</label>
                <input type="text" name="name" id="editCatName" class="form-control form-control-sm" required maxlength="50">
            </div>
            <div class="mb-2">
                <label class="form-label small mb-0">Icon Class</label>
                <input type="text" name="iconClass" id="editCatIcon" class="form-control form-control-sm" maxlength="50">
            </div>
        </div>
        <div class="modal-footer py-1">
            <button type="submit" class="btn btn-primary btn-sm">Save</button>
        </div>
    </form>
</div></div></div>

<script>
    function toggleAddFields() {
        const type = document.getElementById('addResType').value;
        document.getElementById('addFileGroup').classList.toggle('d-none', type !== 'DOCUMENT');
        document.getElementById('addUrlGroup').classList.toggle('d-none', type === 'DOCUMENT');
    }

    function toggleEditFields() {
        const type = document.getElementById('editResType').value;
        document.getElementById('editFileGroup').classList.toggle('d-none', type !== 'DOCUMENT');
        document.getElementById('editUrlGroup').classList.toggle('d-none', type === 'DOCUMENT');
    }

    <c:if test="${not empty selectedResource}">
    (function() { toggleEditFields(); })();
    </c:if>

    function openEditCategory(id, name, iconClass) {
        document.getElementById('editCatId').value = id;
        document.getElementById('editCatName').value = name;
        document.getElementById('editCatIcon').value = iconClass;
        new bootstrap.Modal(document.getElementById('editCategoryModal')).show();
    }

    const resScroll = document.getElementById('resourceScroll');
    if (resScroll) {
        const saved = sessionStorage.getItem('libResScroll');
        if (saved) resScroll.scrollTop = parseInt(saved);
        resScroll.addEventListener('scroll', () => sessionStorage.setItem('libResScroll', resScroll.scrollTop));
    }
</script>
</body>
</html>
