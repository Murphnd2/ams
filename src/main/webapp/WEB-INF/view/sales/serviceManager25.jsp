<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <title>Service Manager</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
    <style>
        :root { --ssa: #0d5681; --ssa-alt: #87a948; }
        body { background: #f8f9fa; }
        .btn-ssa { background: var(--ssa); border-color: var(--ssa); color: white; }
        .btn-ssa:hover { background: #06357a; color: white; }
        .btn-outline-ssa { background: white; border-color: var(--ssa); color: var(--ssa); }
        .btn-outline-ssa:hover { background: var(--ssa); color: white; }
        .hdr-bar { background-color: var(--ssa); color: white; padding: 0.5rem 0.75rem; font-weight: 600; font-size: 1rem; border-radius: 6px 6px 0 0; }
        .hdr-bar .btn-outline-light { padding: 0.15rem 0.5rem; }
        .item-card { cursor: pointer; transition: all 0.15s; }
        .item-card:hover { background-color: #f0f4f8; }
        .item-card.active { border-left: 4px solid var(--ssa); background-color: #e8eef4; }
        .item-scroll { max-height: 280px; overflow-y: auto; }
        .detail-scroll { overflow-y: auto; }
        @media (min-width: 992px) { .detail-scroll { max-height: calc(100vh - 200px); } }
        .assoc-row { border-bottom: 1px solid #eee; padding: 0.4rem 0; }
        .assoc-row:last-child { border-bottom: none; }
        .assoc-row .btn-remove { border: none; background: none; color: #dc3545; font-size: 0.8rem; cursor: pointer; padding: 0.1rem 0.35rem; }
        .assoc-row .btn-remove:hover { color: #a71d2a; }
        .empty-state { text-align: center; color: #6c757d; padding: 2rem 1rem; }
        .empty-state i { font-size: 2rem; margin-bottom: 0.5rem; display: block; }
        .nav-tabs.list-tabs { background-color: var(--ssa); border-bottom: none; border-radius: 6px 6px 0 0; }
        .nav-tabs.list-tabs .nav-link { color: rgba(255,255,255,0.7); font-weight: 600; font-size: 0.95rem; border: none; padding: 0.5rem 0.75rem; border-radius: 0; }
        .nav-tabs.list-tabs .nav-link:first-child { border-radius: 6px 0 0 0; }
        .nav-tabs.list-tabs .nav-link:hover { color: white; }
        .nav-tabs.list-tabs .nav-link.active { color: var(--ssa); background: white; }
        .nav-tabs.list-tabs .tab-tools { display: flex; align-items: center; gap: 4px; margin-left: auto; padding-right: 0.75rem; }
        .nav-tabs.list-tabs .tab-tools .btn { color: rgba(255,255,255,0.8); border-color: rgba(255,255,255,0.5); padding: 0.15rem 0.5rem; font-size: 1rem; }
        .nav-tabs.list-tabs .tab-tools .btn:hover { color: white; border-color: white; }
        .edit-link { color: inherit; font-size: 0.75rem; opacity: 0.5; margin-left: 0.3rem; }
        .edit-link:hover { opacity: 1; }
        .suppressed-item { display: none; }
        .show-suppressed { display: block !important; opacity: 0.45; }
        .preview-link { color: var(--ssa); font-size: 0.75rem; cursor: pointer; margin-left: 0.4rem; opacity: 0.6; }
        .preview-link:hover { opacity: 1; }
        .preview-popover { max-width: 380px; }
        .preview-popover .popover-body { max-height: 300px; overflow-y: auto; font-size: 0.82rem; padding: 0.5rem 0.75rem; }
    </style>
</head>
<body>
<div class="container-fluid py-3" style="max-width: 1400px;">

    <%-- ======================== TOP HEADER BAR ======================== --%>
    <div class="row align-items-center mb-3">
        <div class="col-lg-4">
            <h4 class="mb-0" style="font-size: 1.5rem; font-weight: 500;">
                <i class="bi bi-diagram-3 me-2"></i>Service Manager
            </h4>
        </div>
        <div class="col-lg-5">
            <c:if test="${not empty selectedLos}">
                <h4 class="mb-0" style="font-size: 1.5rem; font-weight: 500;">
                    <i class="bi bi-briefcase me-1"></i>${selectedLos.getDescription()}
                    <a href="#" class="edit-link" data-bs-toggle="modal" data-bs-target="#editLosModal" title="Edit"><i class="bi bi-pencil"></i></a>
                </h4>
            </c:if>
            <c:if test="${not empty selectedEnhancement}">
                <h4 class="mb-0" style="font-size: 1.5rem; font-weight: 500;">
                    <i class="bi bi-puzzle me-1"></i>${selectedEnhancement.getDescription()}
                    <a href="#" class="edit-link" data-bs-toggle="modal" data-bs-target="#editEnhModal" title="Edit"><i class="bi bi-pencil"></i></a>
                </h4>
            </c:if>
        </div>
        <div class="col-lg-3 text-end">
            <div class="d-flex gap-2 justify-content-end align-items-center">
                <a href="PspAdminHome" class="btn btn-outline-ssa"><i class="bi bi-cash-coin me-1"></i>Rates</a>
                <a href="ViewHome25" class="btn btn-ssa"><i class="bi bi-house me-1"></i>Home</a>
            </div>
        </div>
    </div>

    <div class="row g-3">

        <%-- ======================== LEFT COLUMN ======================== --%>
        <div class="col-lg-4">
            <div class="card">
                <ul class="nav nav-tabs list-tabs" role="tablist">
                    <li class="nav-item" role="presentation">
                        <button class="nav-link ${activeTab == 'los' ? 'active' : ''}" id="losTab" data-bs-toggle="tab" data-bs-target="#losPanel" type="button" role="tab">
                            <i class="bi bi-briefcase me-1"></i>Services
                        </button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link ${activeTab == 'enhancement' ? 'active' : ''}" id="enhTab" data-bs-toggle="tab" data-bs-target="#enhPanel" type="button" role="tab">
                            <i class="bi bi-puzzle me-1"></i>Enhancements
                        </button>
                    </li>
                    <li class="tab-tools">
                        <button type="button" class="btn btn-sm" title="Show/hide suppressed" onclick="toggleSuppressed()"><i class="bi bi-eye-slash" id="toggleSuppressedIcon"></i></button>
                        <button type="button" class="btn btn-sm" id="addBtn" data-bs-toggle="modal" data-bs-target="#addLosModal" title="Add new"><i class="bi bi-plus-lg"></i></button>
                    </li>
                </ul>
                <div class="tab-content">
                    <%-- Lines of Service --%>
                    <div class="tab-pane fade ${activeTab == 'los' ? 'show active' : ''}" id="losPanel" role="tabpanel">
                        <div class="item-scroll" id="losScroll">
                            <c:choose>
                                <c:when test="${not empty losList}">
                                    <c:forEach var="los" items="${losList}">
                                        <a href="ServiceManagerHome?losId=${los.getId()}"
                                           class="d-block text-decoration-none text-dark ${los.isSuppressed() ? 'suppressed-item' : ''}"
                                           data-suppressed="${los.isSuppressed()}">
                                            <div class="item-card p-2 ps-3 ${selectedLos != null && selectedLos.getId() == los.getId() ? 'active' : ''}">
                                                <span class="fw-semibold">${los.getShortText()}</span>
                                                <small class="text-muted ms-2">${los.getDescription()}</small>
                                                <c:if test="${los.isSuppressed()}"><i class="bi bi-eye-slash-fill text-muted ms-1" style="font-size:0.7rem;"></i></c:if>
                                            </div>
                                        </a>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><div class="empty-state py-3"><i class="bi bi-briefcase"></i><p class="mb-0">No lines of service</p></div></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <%-- Enhancements --%>
                    <div class="tab-pane fade ${activeTab == 'enhancement' ? 'show active' : ''}" id="enhPanel" role="tabpanel">
                        <div class="item-scroll" id="enhScroll">
                            <c:choose>
                                <c:when test="${not empty enhancementList}">
                                    <c:forEach var="enh" items="${enhancementList}">
                                        <a href="ServiceManagerHome?enhId=${enh.getId()}&tab=enhancement"
                                           class="d-block text-decoration-none text-dark ${enh.isSuppressed() ? 'suppressed-item' : ''}"
                                           data-suppressed="${enh.isSuppressed()}">
                                            <div class="item-card p-2 ps-3 ${selectedEnhancement != null && selectedEnhancement.getId() == enh.getId() ? 'active' : ''}">
                                                <span class="fw-semibold">${enh.getShortText()}</span>
                                                <small class="text-muted ms-2">${enh.getDescription()}</small>
                                                <c:if test="${enh.isSuppressed()}"><i class="bi bi-eye-slash-fill text-muted ms-1" style="font-size:0.7rem;"></i></c:if>
                                            </div>
                                        </a>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><div class="empty-state py-3"><i class="bi bi-puzzle"></i><p class="mb-0">No enhancements</p></div></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <%-- ======================== RIGHT COLUMN ======================== --%>
        <div class="col-lg-8">
            <div class="detail-scroll">

                <%-- STATE: LOS Selected --%>
                <c:if test="${not empty selectedLos}">
                    <%-- Enhancements for this LOS --%>
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-puzzle me-1"></i>Enhancements</span>
                            <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#assignEnhToLosModal" title="Assign enhancement"><i class="bi bi-plus-lg"></i></button>
                        </div>
                        <div class="card-body py-2 px-3">
                            <c:choose>
                                <c:when test="${not empty losEnhancements}">
                                    <c:forEach var="enh" items="${losEnhancements}">
                                        <div class="assoc-row d-flex justify-content-between align-items-center">
                                            <div>
                                                <span class="fw-semibold">${enh.getDescription()}</span>
                                                <small class="text-muted ms-2 d-none d-md-inline">${enh.getShortText()}</small>
                                            </div>
                                            <form method="post" action="ServiceManagerAction" class="d-inline" onsubmit="return confirm('Remove ${enh.getDescription()} from this line of service?');">
                                                <input type="hidden" name="action" value="removeEnhancementFromLos"/>
                                                <input type="hidden" name="losId" value="${selectedLos.getId()}"/>
                                                <input type="hidden" name="enhId" value="${enh.getId()}"/>
                                                <button type="submit" class="btn-remove"><i class="bi bi-x-lg"></i></button>
                                            </form>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><span class="text-muted" style="font-size:0.85rem;">None assigned</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                    <%-- Application Sections for this LOS --%>
                    <div class="card">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-file-earmark-text me-1"></i>Application Sections</span>
                            <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#assignSectionToLosModal" title="Assign section"><i class="bi bi-plus-lg"></i></button>
                        </div>
                        <div class="card-body py-2 px-3">
                            <c:choose>
                                <c:when test="${not empty losAppSections}">
                                    <c:forEach var="section" items="${losAppSections}">
                                        <div class="assoc-row d-flex justify-content-between align-items-center">
                                            <div class="d-flex align-items-center">
                                                <span class="fw-semibold">${section.getName()}</span>
                                                <%-- Preview icon — field data embedded in hidden div, read by JS --%>
                                                <c:if test="${not empty section.getFieldList()}">
                                                    <a href="#" class="preview-link section-preview" tabindex="0"
                                                       data-section-id="los-${section.getId()}"><i class="bi bi-eye"></i></a>
                                                    <div id="fields-los-${section.getId()}" class="d-none">
                                                        <table class="w-100">
                                                            <c:forEach var="f" items="${section.getFieldList()}">
                                                                <tr>
                                                                    <td>${f.getLabel()}<c:if test="${f.isRequired()}"> <span style="color:#dc3545;font-weight:600;">*</span></c:if></td>
                                                                    <td style="color:#6c757d;font-size:0.75rem;white-space:nowrap;">${fn:toLowerCase(f.getFieldType())}</td>
                                                                </tr>
                                                            </c:forEach>
                                                        </table>
                                                    </div>
                                                </c:if>
                                                <c:if test="${not empty section.getDescription()}">
                                                    <small class="text-muted ms-2 d-none d-lg-inline">${section.getDescription()}</small>
                                                </c:if>
                                            </div>
                                            <form method="post" action="ServiceManagerAction" class="d-inline" onsubmit="return confirm('Remove ${section.getName()} from this line of service?');">
                                                <input type="hidden" name="action" value="removeAppSectionFromLos"/>
                                                <input type="hidden" name="losId" value="${selectedLos.getId()}"/>
                                                <input type="hidden" name="sectionId" value="${section.getId()}"/>
                                                <button type="submit" class="btn-remove"><i class="bi bi-x-lg"></i></button>
                                            </form>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><span class="text-muted" style="font-size:0.85rem;">None assigned</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </c:if>

                <%-- STATE: Enhancement Selected --%>
                <c:if test="${not empty selectedEnhancement}">
                    <%-- Lines of Service for this Enhancement --%>
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-briefcase me-1"></i>Available For</span>
                            <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#assignLosToEnhModal" title="Assign to a line of service"><i class="bi bi-plus-lg"></i></button>
                        </div>
                        <div class="card-body py-2 px-3">
                            <c:choose>
                                <c:when test="${not empty enhLosItems}">
                                    <c:forEach var="los" items="${enhLosItems}">
                                        <div class="assoc-row d-flex justify-content-between align-items-center">
                                            <div>
                                                <span class="fw-semibold">${los.getDescription()}</span>
                                                <small class="text-muted ms-2 d-none d-md-inline">${los.getShortText()}</small>
                                            </div>
                                            <form method="post" action="ServiceManagerAction" class="d-inline" onsubmit="return confirm('Remove ${los.getDescription()} from this enhancement?');">
                                                <input type="hidden" name="action" value="removeLosFromEnhancement"/>
                                                <input type="hidden" name="enhId" value="${selectedEnhancement.getId()}"/>
                                                <input type="hidden" name="losId" value="${los.getId()}"/>
                                                <button type="submit" class="btn-remove"><i class="bi bi-x-lg"></i></button>
                                            </form>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><span class="text-muted" style="font-size:0.85rem;">None assigned</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                    <%-- Application Sections for this Enhancement --%>
                    <div class="card">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-file-earmark-text me-1"></i>Application Sections</span>
                            <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#assignSectionToEnhModal" title="Assign section"><i class="bi bi-plus-lg"></i></button>
                        </div>
                        <div class="card-body py-2 px-3">
                            <c:choose>
                                <c:when test="${not empty enhAppSections}">
                                    <c:forEach var="section" items="${enhAppSections}">
                                        <div class="assoc-row d-flex justify-content-between align-items-center">
                                            <div class="d-flex align-items-center">
                                                <span class="fw-semibold">${section.getName()}</span>
                                                <c:if test="${not empty section.getFieldList()}">
                                                    <a href="#" class="preview-link section-preview" tabindex="0"
                                                       data-section-id="enh-${section.getId()}"><i class="bi bi-eye"></i></a>
                                                    <div id="fields-enh-${section.getId()}" class="d-none">
                                                        <table class="w-100">
                                                            <c:forEach var="f" items="${section.getFieldList()}">
                                                                <tr>
                                                                    <td>${f.getLabel()}<c:if test="${f.isRequired()}"> <span style="color:#dc3545;font-weight:600;">*</span></c:if></td>
                                                                    <td style="color:#6c757d;font-size:0.75rem;white-space:nowrap;">${fn:toLowerCase(f.getFieldType())}</td>
                                                                </tr>
                                                            </c:forEach>
                                                        </table>
                                                    </div>
                                                </c:if>
                                                <c:if test="${not empty section.getDescription()}">
                                                    <small class="text-muted ms-2 d-none d-lg-inline">${section.getDescription()}</small>
                                                </c:if>
                                            </div>
                                            <form method="post" action="ServiceManagerAction" class="d-inline" onsubmit="return confirm('Remove ${section.getName()} from this enhancement?');">
                                                <input type="hidden" name="action" value="removeAppSectionFromEnhancement"/>
                                                <input type="hidden" name="enhId" value="${selectedEnhancement.getId()}"/>
                                                <input type="hidden" name="sectionId" value="${section.getId()}"/>
                                                <button type="submit" class="btn-remove"><i class="bi bi-x-lg"></i></button>
                                            </form>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><span class="text-muted" style="font-size:0.85rem;">None assigned</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </c:if>

                <%-- STATE: Nothing selected --%>
                <c:if test="${empty selectedLos && empty selectedEnhancement}">
                    <div class="card"><div class="card-body empty-state"><i class="bi bi-arrow-left-circle"></i><p class="mb-0 fs-5">Select a line of service or enhancement to manage its associations</p></div></div>
                </c:if>

            </div>
        </div>

    </div>
</div>

<%-- ======================== MODALS ======================== --%>

<%-- Add LOS --%>
<div class="modal fade" id="addLosModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="ServiceManagerAction"><input type="hidden" name="action" value="createLos"/>
        <div class="modal-header"><h5 class="modal-title"><i class="bi bi-plus-circle me-2"></i>New Line of Service</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body">
            <div class="mb-3"><label class="form-label fw-semibold">Description</label><input type="text" name="description" class="form-control" required placeholder="e.g. Flexible Spending Accounts"></div>
            <div class="mb-3"><label class="form-label fw-semibold">Short Text</label><input type="text" name="shortText" class="form-control" required maxlength="10" placeholder="e.g. FSA"></div>
        </div>
        <div class="modal-footer"><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button><button type="submit" class="btn btn-primary">Create</button></div>
    </form>
</div></div></div>

<%-- Add Enhancement --%>
<div class="modal fade" id="addEnhModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <form method="post" action="ServiceManagerAction"><input type="hidden" name="action" value="createEnhancement"/>
        <div class="modal-header"><h5 class="modal-title"><i class="bi bi-plus-circle me-2"></i>New Enhancement</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body">
            <div class="mb-3"><label class="form-label fw-semibold">Description</label><input type="text" name="description" class="form-control" required placeholder="e.g. Debit Card Services"></div>
            <div class="mb-3"><label class="form-label fw-semibold">Short Text</label><input type="text" name="shortText" class="form-control" required maxlength="20" placeholder="e.g. Cards"></div>
        </div>
        <div class="modal-footer"><button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button><button type="submit" class="btn btn-primary">Create</button></div>
    </form>
</div></div></div>

<%-- Edit LOS --%>
<c:if test="${not empty selectedLos}">
<div class="modal fade" id="editLosModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <div class="modal-header"><h5 class="modal-title"><i class="bi bi-pencil me-2"></i>Edit Line of Service</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
    <form method="post" action="ServiceManagerAction">
        <input type="hidden" name="action" value="editLos"/><input type="hidden" name="losId" value="${selectedLos.getId()}"/>
        <div class="modal-body">
            <div class="mb-3"><label class="form-label fw-semibold">Description</label><input type="text" name="description" class="form-control" required value="${selectedLos.getDescription()}"></div>
            <div class="mb-3"><label class="form-label fw-semibold">Short Text</label><input type="text" name="shortText" class="form-control" required maxlength="10" value="${selectedLos.getShortText()}"></div>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-sm btn-outline-${selectedLos.isSuppressed() ? 'success' : 'warning'}"
                    onclick="document.getElementById('suppressLosForm').submit();">
                <i class="bi bi-eye${selectedLos.isSuppressed() ? '' : '-slash'} me-1"></i>${selectedLos.isSuppressed() ? 'Unsuppress' : 'Suppress'}
            </button>
            <span class="flex-grow-1"></span>
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
            <button type="submit" class="btn btn-primary">Save</button>
        </div>
    </form>
</div></div></div>
</c:if>

<%-- Edit Enhancement --%>
<c:if test="${not empty selectedEnhancement}">
<div class="modal fade" id="editEnhModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
    <div class="modal-header"><h5 class="modal-title"><i class="bi bi-pencil me-2"></i>Edit Enhancement</h5><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
    <form method="post" action="ServiceManagerAction">
        <input type="hidden" name="action" value="editEnhancement"/><input type="hidden" name="enhId" value="${selectedEnhancement.getId()}"/>
        <div class="modal-body">
            <div class="mb-3"><label class="form-label fw-semibold">Description</label><input type="text" name="description" class="form-control" required value="${selectedEnhancement.getDescription()}"></div>
            <div class="mb-3"><label class="form-label fw-semibold">Short Text</label><input type="text" name="shortText" class="form-control" required maxlength="20" value="${selectedEnhancement.getShortText()}"></div>
        </div>
        <div class="modal-footer">
            <button type="button" class="btn btn-sm btn-outline-${selectedEnhancement.isSuppressed() ? 'success' : 'warning'}"
                    onclick="document.getElementById('suppressEnhForm').submit();">
                <i class="bi bi-eye${selectedEnhancement.isSuppressed() ? '' : '-slash'} me-1"></i>${selectedEnhancement.isSuppressed() ? 'Unsuppress' : 'Suppress'}
            </button>
            <span class="flex-grow-1"></span>
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
            <button type="submit" class="btn btn-primary">Save</button>
        </div>
    </form>
</div></div></div>
</c:if>

<%-- Hidden suppress forms (OUTSIDE modals to avoid nested form issues) --%>
<c:if test="${not empty selectedLos}">
    <form id="suppressLosForm" method="post" action="ServiceManagerAction" class="d-none">
        <input type="hidden" name="action" value="suppressLos"/>
        <input type="hidden" name="losId" value="${selectedLos.getId()}"/>
    </form>
</c:if>
<c:if test="${not empty selectedEnhancement}">
    <form id="suppressEnhForm" method="post" action="ServiceManagerAction" class="d-none">
        <input type="hidden" name="action" value="suppressEnhancement"/>
        <input type="hidden" name="enhId" value="${selectedEnhancement.getId()}"/>
    </form>
</c:if>

<%-- Assign Enhancement to LOS --%>
<c:if test="${not empty selectedLos}">
<div class="modal fade" id="assignEnhToLosModal" tabindex="-1"><div class="modal-dialog modal-sm"><div class="modal-content">
    <form method="post" action="ServiceManagerAction"><input type="hidden" name="action" value="assignEnhancementToLos"/><input type="hidden" name="losId" value="${selectedLos.getId()}"/>
        <div class="modal-header py-2"><h6 class="modal-title"><i class="bi bi-puzzle me-1"></i>Assign Enhancement</h6><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body py-2"><select name="enhId" class="form-select form-select-sm" required><option value="">-- Select --</option>
            <c:forEach var="enh" items="${enhancementList}">
                <c:set var="alreadyAssigned" value="false"/>
                <c:forEach var="assigned" items="${losEnhancements}">
                    <c:if test="${assigned.getId() == enh.getId()}"><c:set var="alreadyAssigned" value="true"/></c:if>
                </c:forEach>
                <c:if test="${alreadyAssigned == 'false' && !enh.isSuppressed()}">
                    <option value="${enh.getId()}">${enh.getDescription()}</option>
                </c:if>
            </c:forEach>
        </select></div>
        <div class="modal-footer py-1"><button type="submit" class="btn btn-primary btn-sm">Assign</button></div>
    </form>
</div></div></div>
</c:if>

<%-- Assign App Section to LOS --%>
<c:if test="${not empty selectedLos}">
<div class="modal fade" id="assignSectionToLosModal" tabindex="-1"><div class="modal-dialog modal-sm"><div class="modal-content">
    <form method="post" action="ServiceManagerAction"><input type="hidden" name="action" value="assignAppSectionToLos"/><input type="hidden" name="losId" value="${selectedLos.getId()}"/>
        <div class="modal-header py-2"><h6 class="modal-title"><i class="bi bi-file-earmark-text me-1"></i>Assign Section</h6><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body py-2"><select name="sectionId" class="form-select form-select-sm" required><option value="">-- Select --</option>
            <c:forEach var="section" items="${appSectionList}">
                <c:set var="alreadyAssigned" value="false"/>
                <c:forEach var="assigned" items="${losAppSections}">
                    <c:if test="${assigned.getId() == section.getId()}"><c:set var="alreadyAssigned" value="true"/></c:if>
                </c:forEach>
                <c:if test="${alreadyAssigned == 'false'}">
                    <option value="${section.getId()}">${section.getName()}</option>
                </c:if>
            </c:forEach>
        </select></div>
        <div class="modal-footer py-1"><button type="submit" class="btn btn-primary btn-sm">Assign</button></div>
    </form>
</div></div></div>
</c:if>

<%-- Assign LOS to Enhancement --%>
<c:if test="${not empty selectedEnhancement}">
<div class="modal fade" id="assignLosToEnhModal" tabindex="-1"><div class="modal-dialog modal-sm"><div class="modal-content">
    <form method="post" action="ServiceManagerAction"><input type="hidden" name="action" value="assignLosToEnhancement"/><input type="hidden" name="enhId" value="${selectedEnhancement.getId()}"/>
        <div class="modal-header py-2"><h6 class="modal-title"><i class="bi bi-briefcase me-1"></i>Assign to Line of Service</h6><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body py-2"><select name="losId" class="form-select form-select-sm" required><option value="">-- Select --</option>
            <c:forEach var="los" items="${losList}">
                <c:set var="alreadyAssigned" value="false"/>
                <c:forEach var="assigned" items="${enhLosItems}">
                    <c:if test="${assigned.getId() == los.getId()}"><c:set var="alreadyAssigned" value="true"/></c:if>
                </c:forEach>
                <c:if test="${alreadyAssigned == 'false' && !los.isSuppressed()}">
                    <option value="${los.getId()}">${los.getDescription()} (${los.getShortText()})</option>
                </c:if>
            </c:forEach>
        </select></div>
        <div class="modal-footer py-1"><button type="submit" class="btn btn-primary btn-sm">Assign</button></div>
    </form>
</div></div></div>
</c:if>

<%-- Assign App Section to Enhancement --%>
<c:if test="${not empty selectedEnhancement}">
<div class="modal fade" id="assignSectionToEnhModal" tabindex="-1"><div class="modal-dialog modal-sm"><div class="modal-content">
    <form method="post" action="ServiceManagerAction"><input type="hidden" name="action" value="assignAppSectionToEnhancement"/><input type="hidden" name="enhId" value="${selectedEnhancement.getId()}"/>
        <div class="modal-header py-2"><h6 class="modal-title"><i class="bi bi-file-earmark-text me-1"></i>Assign Section</h6><button type="button" class="btn-close" data-bs-dismiss="modal"></button></div>
        <div class="modal-body py-2"><select name="sectionId" class="form-select form-select-sm" required><option value="">-- Select --</option>
            <c:forEach var="section" items="${appSectionList}">
                <c:set var="alreadyAssigned" value="false"/>
                <c:forEach var="assigned" items="${enhAppSections}">
                    <c:if test="${assigned.getId() == section.getId()}"><c:set var="alreadyAssigned" value="true"/></c:if>
                </c:forEach>
                <c:if test="${alreadyAssigned == 'false'}">
                    <option value="${section.getId()}">${section.getName()}</option>
                </c:if>
            </c:forEach>
        </select></div>
        <div class="modal-footer py-1"><button type="submit" class="btn btn-primary btn-sm">Assign</button></div>
    </form>
</div></div></div>
</c:if>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
    // ── Tab-aware add button ──────────────────────────────────────────
    const addBtn = document.getElementById('addBtn');
    document.getElementById('losTab').addEventListener('shown.bs.tab', () => {
        addBtn.setAttribute('data-bs-target', '#addLosModal');
    });
    document.getElementById('enhTab').addEventListener('shown.bs.tab', () => {
        addBtn.setAttribute('data-bs-target', '#addEnhModal');
    });

    // ── Suppress toggle ──────────────────────────────────────────────
    let showSuppressed = false;
    function toggleSuppressed() {
        showSuppressed = !showSuppressed;
        document.getElementById('toggleSuppressedIcon').className = showSuppressed ? 'bi bi-eye' : 'bi bi-eye-slash';
        document.querySelectorAll('[data-suppressed="true"]').forEach(item => {
            if (showSuppressed) {
                item.classList.remove('suppressed-item');
                item.classList.add('show-suppressed');
            } else {
                item.classList.add('suppressed-item');
                item.classList.remove('show-suppressed');
            }
        });
    }

    // ── Scroll state preservation ─────────────────────────────────────
    const losScroll = document.getElementById('losScroll');
    const enhScroll = document.getElementById('enhScroll');
    if (losScroll) {
        const savedLos = sessionStorage.getItem('smLosScroll');
        if (savedLos) losScroll.scrollTop = parseInt(savedLos);
        losScroll.addEventListener('scroll', () => sessionStorage.setItem('smLosScroll', losScroll.scrollTop));
    }
    if (enhScroll) {
        const savedEnh = sessionStorage.getItem('smEnhScroll');
        if (savedEnh) enhScroll.scrollTop = parseInt(savedEnh);
        enhScroll.addEventListener('scroll', () => sessionStorage.setItem('smEnhScroll', enhScroll.scrollTop));
    }

    // ── Application Section preview popovers (built from hidden divs) ──
    document.querySelectorAll('.section-preview').forEach(link => {
        const sectionId = link.dataset.sectionId;
        const fieldsDiv = document.getElementById('fields-' + sectionId);
        if (fieldsDiv) {
            new bootstrap.Popover(link, {
                trigger: 'focus',
                placement: 'left',
                html: true,
                customClass: 'preview-popover',
                title: link.closest('.assoc-row').querySelector('.fw-semibold').textContent,
                content: fieldsDiv.innerHTML
            });
        }
    });
</script>
</body>
</html>
