<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Service Manager</title>
    <style>
        .preview-link {
            color: #6c757d;
            font-size: 0.75rem;
            margin-left: 0.5rem;
            text-decoration: none;
        }

        .preview-link:hover {
            color: var(--ssa);
        }

        .preview-popover {
            max-width: 350px;
        }

        .preview-popover .popover-body table td {
            padding: 0.15rem 0.4rem;
            font-size: 0.8rem;
        }

        .suppressed-item {
            display: none !important;
        }

        .show-suppressed {
            display: block !important;
            opacity: 0.45;
            font-style: italic;
        }

        .list-tabs {
            border-bottom: 2px solid var(--ssa);
        }

        .list-tabs .nav-link {
            color: var(--ssa);
            font-weight: 600;
            border: none;
        }

        .list-tabs .nav-link.active {
            background: var(--ssa);
            color: white;
            border-radius: 6px 6px 0 0;
        }

        .tab-tools {
            margin-left: auto;
            display: flex;
            align-items: center;
            gap: 0.25rem;
            padding-right: 0.5rem;
        }

        .tab-tools .btn {
            color: var(--ssa);
            padding: 0.1rem 0.4rem;
        }

        .sortable-ghost {
            opacity: 0.4;
            background: #e8eef4;
        }

        .drag-handle {
            color: #adb5bd;
            cursor: grab;
            margin-right: 0.4rem;
            font-size: 0.85rem;
        }

        .drag-handle:active {
            cursor: grabbing;
        }

        .item-scroll {
            max-height: 280px;
            overflow-y: auto;
        }

        .detail-scroll {
            overflow-y: auto;
        }

        @media (min-width: 992px) {
            .detail-scroll {
                max-height: calc(100vh - 200px);
            }
        }

        .assoc-row {
            border-bottom: 1px solid #eee;
            padding: 0.4rem 0;
        }

        .assoc-row:last-child {
            border-bottom: none;
        }

        .assoc-row .btn-remove {
            border: none;
            background: none;
            color: #dc3545;
            font-size: 0.8rem;
            cursor: pointer;
            padding: 0.1rem 0.35rem;
        }

        .assoc-row .btn-remove:hover {
            color: #a71d2a;
        }

        .link-toolbar {
            display: flex;
            align-items: center;
            gap: 0.5rem;
            margin-top: 0.35rem;
        }

        .link-toolbar .btn {
            padding: 0.15rem 0.5rem;
            font-size: 0.8rem;
        }

        .link-marker {
            background: #e8f4fd;
            border-radius: 3px;
            padding: 0 2px;
        }

        .field-suppressed { display: none !important; }
        .field-sort-wrap.show-field-suppressed .field-suppressed { display: table-row !important; opacity: 0.45; font-style: italic; }


    </style>
</head>
<body>
<div class="container-fluid">
    <c:set var="pageTitle" value="Service Manager" scope="request"/>
    <c:set var="pageIcon" value="bi-diagram-3" scope="request"/>
    <c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

    <% String flash = (String) session.getAttribute("flashMessage"); if (flash != null) { session.removeAttribute("flashMessage"); %>
    <div class="alert alert-success alert-dismissible fade show mt-2 mb-0" role="alert">
        <i class="bi bi-check-circle me-1"></i><%= flash %>
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
    <% } %>

    <div class="row g-3 mt-3">

        <%-- ======================== LEFT COLUMN ======================== --%>
        <div class="col-lg-4">
            <div class="card">
                <ul class="nav nav-tabs list-tabs" role="tablist">
                    <li class="nav-item" role="presentation">
                        <button class="nav-link ${activeTab == 'los' ? 'active' : ''}" id="losTab" data-bs-toggle="tab"
                                data-bs-target="#losPanel" type="button" role="tab">
                            <i class="bi bi-briefcase me-1"></i>Services
                        </button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link ${activeTab == 'enhancement' ? 'active' : ''}" id="enhTab"
                                data-bs-toggle="tab" data-bs-target="#enhPanel" type="button" role="tab">
                            <i class="bi bi-puzzle me-1"></i>Enhancements
                        </button>
                    </li>
                    <li class="nav-item" role="presentation">
                        <button class="nav-link ${activeTab == 'section' ? 'active' : ''}" id="secTab"
                                data-bs-toggle="tab" data-bs-target="#secPanel" type="button" role="tab">
                            <i class="bi bi-file-earmark-text me-1"></i>Sections
                        </button>
                    </li>
                    <li class="tab-tools">
                        <button type="button" class="btn btn-sm" title="Show/hide suppressed"
                                onclick="toggleSuppressed()"><i class="bi bi-eye-slash" id="toggleSuppressedIcon"></i>
                        </button>
                        <c:if test="${not empty availablePackages}">
                        <button type="button" class="btn btn-sm" id="loadPkgBtn" data-bs-toggle="modal"
                                data-bs-target="#loadPackageModal" title="Load starter package"
                                style="display:${activeTab == 'section' ? 'inline-block' : 'none'}"><i class="bi bi-box-seam"></i></button>
                        </c:if>
                        <button type="button" class="btn btn-sm" id="addBtn" data-bs-toggle="modal"
                                data-bs-target="#addLosModal" title="Add new"><i class="bi bi-plus-lg"></i></button>
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
                                           class="d-block text-decoration-none text-dark los-item ${los.isSuppressed() ? 'suppressed-item' : ''}"
                                           data-suppressed="${los.isSuppressed()}" data-id="${los.getId()}">
                                            <div class="item-card p-2 ps-3 d-flex align-items-center ${selectedLos != null && selectedLos.getId() == los.getId() ? 'active' : ''}">
                                                <i class="bi bi-grip-vertical drag-handle"></i>
                                                <span class="fw-semibold">${los.getShortText()}</span>
                                                <small class="text-muted ms-2">${los.getDescription()}</small>
                                                <c:if test="${los.isSuppressed()}"><i
                                                        class="bi bi-eye-slash-fill text-muted ms-1"
                                                        style="font-size:0.7rem;"></i></c:if>
                                            </div>
                                        </a>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <div class="empty-state py-3"><i class="bi bi-briefcase"></i>
                                        <p class="mb-0">No lines of service</p></div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <%-- Enhancements --%>
                    <div class="tab-pane fade ${activeTab == 'enhancement' ? 'show active' : ''}" id="enhPanel"
                         role="tabpanel">
                        <div class="item-scroll" id="enhScroll">
                            <c:choose>
                                <c:when test="${not empty enhancementList}">
                                    <c:forEach var="enh" items="${enhancementList}">
                                        <a href="ServiceManagerHome?enhId=${enh.getId()}&tab=enhancement"
                                           class="d-block text-decoration-none text-dark enh-item ${enh.isSuppressed() ? 'suppressed-item' : ''}"
                                           data-suppressed="${enh.isSuppressed()}" data-id="${enh.getId()}">
                                            <div class="item-card p-2 ps-3 d-flex align-items-center ${selectedEnhancement != null && selectedEnhancement.getId() == enh.getId() ? 'active' : ''}">
                                                <i class="bi bi-grip-vertical drag-handle"></i>
                                                <span class="fw-semibold">${enh.getShortText()}</span>
                                                <small class="text-muted ms-2">${enh.getDescription()}</small>
                                                <c:if test="${enh.isSuppressed()}"><i
                                                        class="bi bi-eye-slash-fill text-muted ms-1"
                                                        style="font-size:0.7rem;"></i></c:if>
                                            </div>
                                        </a>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <div class="empty-state py-3"><i class="bi bi-puzzle"></i>
                                        <p class="mb-0">No enhancements</p></div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                    <%-- App Sections --%>
                    <div class="tab-pane fade ${activeTab == 'section' ? 'show active' : ''}" id="secPanel"
                         role="tabpanel">
                        <div class="item-scroll" id="secScroll">
                            <c:choose>
                                <c:when test="${not empty appSectionList}">
                                    <c:forEach var="sec" items="${appSectionList}">
                                        <a href="ServiceManagerHome?sectionId=${sec.getId()}&tab=section"
                                           class="d-block text-decoration-none text-dark sec-item ${sec.isSuppressed() ? 'suppressed-item' : ''}"
                                           data-suppressed="${sec.isSuppressed()}" data-id="${sec.getId()}">
                                            <div class="item-card p-2 ps-3 d-flex align-items-center ${selectedSection != null && selectedSection.getId() == sec.getId() ? 'active' : ''}">
                                                <i class="bi bi-grip-vertical drag-handle"></i>
                                                <span class="fw-semibold">${sec.getName()}</span>
                                                <small class="text-muted ms-2">${sec.getScope()}</small>
                                                <c:if test="${not empty sec.getFieldList()}">
                                                    <span class="badge bg-secondary ms-auto"
                                                          style="font-size:0.65rem;">${fn:length(sec.getFieldList())}</span>
                                                </c:if>
                                                <c:if test="${sec.isSuppressed()}"><i
                                                        class="bi bi-eye-slash-fill text-muted ms-1"
                                                        style="font-size:0.7rem;"></i></c:if>
                                            </div>
                                        </a>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    <div class="empty-state py-3"><i class="bi bi-file-earmark-text"></i>
                                        <p class="mb-0">No application sections</p></div>
                                </c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                </div>
            </div>
        </div>

        <%-- ======================== RIGHT COLUMN ======================== --%>
        <div class="col-lg-8 mt-2">
            <c:if test="${not empty selectedLos}">
                <h5 class="mb-2 px-3 py-2 rounded" style="background-color: #87a948; color: white;">
                    <i class="bi bi-briefcase me-1"></i>${selectedLos.getDescription()}
                    <a href="#" class="edit-link" data-bs-toggle="modal" data-bs-target="#editLosModal" title="Edit"><i class="bi bi-pencil"></i></a>
                </h5>
            </c:if>
            <c:if test="${not empty selectedEnhancement}">
                <h5 class="mb-2 px-3 py-2 rounded" style="background-color: #87a948; color: white;">
                    <i class="bi bi-puzzle me-1"></i>${selectedEnhancement.getDescription()}
                    <a href="#" class="edit-link" data-bs-toggle="modal" data-bs-target="#editEnhModal" title="Edit"><i class="bi bi-pencil"></i></a>
                </h5>
            </c:if>
            <c:if test="${not empty selectedSection}">
                <h5 class="mb-2 px-3 py-2 rounded d-flex align-items-center" style="background-color: #87a948; color: white;">
                    <i class="bi bi-file-earmark-text me-1"></i>${selectedSection.getName()}
                    <a href="#" class="edit-link" data-bs-toggle="modal" data-bs-target="#editSectionModal" title="Edit"><i class="bi bi-pencil"></i></a>
                    <small class="text-muted ms-2" style="font-size:0.75rem;">${selectedSection.getScope()}</small>
                    <c:if test="${selectedSection.getTemplateKey() != null}">
                        <form method="post" action="ServiceManagerAction" style="display:inline; margin-left:auto;"
                              onsubmit="return confirm('Reset this section and all its fields to package defaults?');">
                            <input type="hidden" name="action" value="resetSectionToDefault"/>
                            <input type="hidden" name="sectionId" value="${selectedSection.getId()}"/>
                            <button type="submit" class="btn btn-sm btn-outline-light" title="Reset to package defaults">
                                <i class="bi bi-arrow-counterclockwise"></i> Reset to Default
                            </button>
                        </form>
                    </c:if>
                </h5>
            </c:if>
            <div class="detail-scroll">

                <%-- STATE: LOS Selected --%>
                <c:if test="${not empty selectedLos}">
                    <%-- Enhancements for this LOS --%>
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-puzzle me-1"></i>Enhancements</span>
                            <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal"
                                    data-bs-target="#assignEnhToLosModal" title="Assign enhancement"><i
                                    class="bi bi-plus-lg"></i></button>
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
                                            <form method="post" action="ServiceManagerAction" class="d-inline"
                                                  onsubmit="return confirm('Remove ${enh.getDescription()} from this line of service?');">
                                                <input type="hidden" name="action" value="removeEnhancementFromLos"/>
                                                <input type="hidden" name="losId" value="${selectedLos.getId()}"/>
                                                <input type="hidden" name="enhId" value="${enh.getId()}"/>
                                                <button type="submit" class="btn-remove"><i class="bi bi-x-lg"></i>
                                                </button>
                                            </form>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><span class="text-muted"
                                                   style="font-size:0.85rem;">None assigned</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                    <%-- Application Sections for this LOS --%>
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-file-earmark-text me-1"></i>Application Sections</span>
                            <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal"
                                    data-bs-target="#assignSectionToLosModal" title="Assign section"><i
                                    class="bi bi-plus-lg"></i></button>
                        </div>
                        <div class="card-body py-2 px-3" id="losSectionList">
                            <c:choose>
                                <c:when test="${not empty losAppSections}">
                                    <c:forEach var="section" items="${losAppSections}">
                                        <div class="assoc-row d-flex justify-content-between align-items-center los-section-item"
                                             data-id="${section.getId()}">
                                            <div class="d-flex align-items-center">
                                                <i class="bi bi-grip-vertical drag-handle"></i>
                                                <span class="fw-semibold">${section.getName()}</span>
                                                <c:if test="${section.getScope() == 'ALL'}"><span class="badge bg-primary ms-1" style="font-size:0.6rem;">ALL</span></c:if>
                                                <c:if test="${not empty section.getFieldList()}">
                                                    <a href="#" class="preview-link section-preview" tabindex="0"
                                                       data-section-id="los-${section.getId()}"><i
                                                            class="bi bi-eye"></i></a>
                                                    <div id="fields-los-${section.getId()}" class="d-none">
                                                        <table class="w-100">
                                                            <c:forEach var="f" items="${section.getFieldList()}">
                                                                <tr>
                                                                    <td>${f.getLabel()}<c:if test="${f.isRequired()}">
                                                                        <span style="color:#dc3545;font-weight:600;">*</span></c:if>
                                                                    </td>
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
                                            <c:if test="${section.getScope() != 'ALL'}">
                                                <form method="post" action="ServiceManagerAction" class="d-inline"
                                                      onsubmit="return confirm('Remove ${section.getName()} from this line of service?');">
                                                    <input type="hidden" name="action" value="removeAppSectionFromLos"/>
                                                    <input type="hidden" name="losId" value="${selectedLos.getId()}"/>
                                                    <input type="hidden" name="sectionId" value="${section.getId()}"/>
                                                    <button type="submit" class="btn-remove"><i class="bi bi-x-lg"></i>
                                                    </button>
                                                </form>
                                            </c:if>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><span class="text-muted"
                                                   style="font-size:0.85rem;">None assigned</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                    <%-- Features for this LOS --%>
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-check2-square me-1"></i>Features</span>
                            <c:if test="${not empty selectedModule}">
                                <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal"
                                        data-bs-target="#addFeatureModal" title="Add feature"><i
                                        class="bi bi-plus-lg"></i></button>
                            </c:if>
                        </div>
                        <div class="card-body py-2 px-3" id="losFeatureList">
                            <c:choose>
                                <c:when test="${empty selectedModule}">
                                    <span class="text-muted" style="font-size:0.85rem;">No pricing module exists yet. Add pricing in Rate Manager first.</span>
                                </c:when>
                                <c:when test="${not empty featureList}">
                                    <c:forEach var="feature" items="${featureList}">
                                        <div class="assoc-row d-flex justify-content-between align-items-center feature-item"
                                             data-id="${feature.getId()}">
                                            <div class="d-flex align-items-center flex-grow-1">
                                                <i class="bi bi-grip-vertical drag-handle"></i>
                                                <i class="bi bi-check2 me-1" style="color: var(--ssa-alt);"></i>
                                                <span class="feature-text">${feature.getDescription()}</span>
                                                <c:if test="${not empty feature.getLibraryResource()}">
                                                    <span class="badge bg-light text-dark ms-1"
                                                          style="font-size:0.7rem;">
                                                        <i class="bi bi-link-45deg"></i> ${feature.getLibraryResource().getTitle()}
                                                    </span>
                                                </c:if>
                                            </div>
                                            <div class="d-flex align-items-center gap-1">
                                                <button type="button" class="btn-remove" style="color: var(--ssa);"
                                                        onclick="openEditFeature(${feature.getId()}, this.closest('.feature-item').querySelector('.feature-text').textContent, '${not empty feature.getLibraryResource() ? feature.getLibraryResource().getId() : ''}')"
                                                        title="Edit"><i class="bi bi-pencil"></i></button>
                                                <form method="post" action="ServiceManagerAction" class="d-inline"
                                                      onsubmit="return confirm('Delete this feature?');">
                                                    <input type="hidden" name="action" value="deleteFeature"/>
                                                    <input type="hidden" name="featureId" value="${feature.getId()}"/>
                                                    <input type="hidden" name="losId" value="${selectedLos.getId()}"/>
                                                    <button type="submit" class="btn-remove"><i class="bi bi-x-lg"></i>
                                                    </button>
                                                </form>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><span class="text-muted" style="font-size:0.85rem;">No features yet — click + to add</span></c:otherwise>
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
                            <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal"
                                    data-bs-target="#assignLosToEnhModal" title="Assign to a line of service"><i
                                    class="bi bi-plus-lg"></i></button>
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
                                            <form method="post" action="ServiceManagerAction" class="d-inline"
                                                  onsubmit="return confirm('Remove ${los.getDescription()} from this enhancement?');">
                                                <input type="hidden" name="action" value="removeLosFromEnhancement"/>
                                                <input type="hidden" name="enhId"
                                                       value="${selectedEnhancement.getId()}"/>
                                                <input type="hidden" name="losId" value="${los.getId()}"/>
                                                <button type="submit" class="btn-remove"><i class="bi bi-x-lg"></i>
                                                </button>
                                            </form>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><span class="text-muted"
                                                   style="font-size:0.85rem;">None assigned</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                    <%-- Application Sections for this Enhancement --%>
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-file-earmark-text me-1"></i>Application Sections</span>
                            <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal"
                                    data-bs-target="#assignSectionToEnhModal" title="Assign section"><i
                                    class="bi bi-plus-lg"></i></button>
                        </div>
                        <div class="card-body py-2 px-3" id="enhSectionList">
                            <c:choose>
                                <c:when test="${not empty enhAppSections}">
                                    <c:forEach var="section" items="${enhAppSections}">
                                        <div class="assoc-row d-flex justify-content-between align-items-center enh-section-item"
                                             data-id="${section.getId()}">
                                            <div class="d-flex align-items-center">
                                                <i class="bi bi-grip-vertical drag-handle"></i>
                                                <span class="fw-semibold">${section.getName()}</span>
                                                <c:if test="${section.getScope() == 'ALL'}"><span class="badge bg-primary ms-1" style="font-size:0.6rem;">ALL</span></c:if>
                                                <c:if test="${not empty section.getFieldList()}">
                                                    <a href="#" class="preview-link section-preview" tabindex="0"
                                                       data-section-id="enh-${section.getId()}"><i
                                                            class="bi bi-eye"></i></a>
                                                    <div id="fields-enh-${section.getId()}" class="d-none">
                                                        <table class="w-100">
                                                            <c:forEach var="f" items="${section.getFieldList()}">
                                                                <tr>
                                                                    <td>${f.getLabel()}<c:if test="${f.isRequired()}">
                                                                        <span style="color:#dc3545;font-weight:600;">*</span></c:if>
                                                                    </td>
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
                                            <c:if test="${section.getScope() != 'ALL'}">
                                                <form method="post" action="ServiceManagerAction" class="d-inline"
                                                      onsubmit="return confirm('Remove ${section.getName()} from this enhancement?');">
                                                    <input type="hidden" name="action"
                                                           value="removeAppSectionFromEnhancement"/>
                                                    <input type="hidden" name="enhId"
                                                           value="${selectedEnhancement.getId()}"/>
                                                    <input type="hidden" name="sectionId" value="${section.getId()}"/>
                                                    <button type="submit" class="btn-remove"><i class="bi bi-x-lg"></i>
                                                    </button>
                                                </form>
                                            </c:if>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><span class="text-muted"
                                                   style="font-size:0.85rem;">None assigned</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                    <%-- Features for this Enhancement --%>
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-check2-square me-1"></i>Features</span>
                            <c:if test="${not empty selectedModule}">
                                <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal"
                                        data-bs-target="#addFeatureModal" title="Add feature"><i
                                        class="bi bi-plus-lg"></i></button>
                            </c:if>
                        </div>
                        <div class="card-body py-2 px-3" id="enhFeatureList">
                            <c:choose>
                                <c:when test="${empty selectedModule}">
                                    <span class="text-muted" style="font-size:0.85rem;">No pricing module exists yet. Add pricing in Rate Manager first.</span>
                                </c:when>
                                <c:when test="${not empty featureList}">
                                    <c:forEach var="feature" items="${featureList}">
                                        <div class="assoc-row d-flex justify-content-between align-items-center feature-item"
                                             data-id="${feature.getId()}">
                                            <div class="d-flex align-items-center flex-grow-1">
                                                <i class="bi bi-grip-vertical drag-handle"></i>
                                                <i class="bi bi-check2 me-1" style="color: var(--ssa-alt);"></i>
                                                <span class="feature-text">${feature.getDescription()}</span>
                                                <c:if test="${not empty feature.getLibraryResource()}">
                                                    <span class="badge bg-light text-dark ms-1"
                                                          style="font-size:0.7rem;">
                                                        <i class="bi bi-link-45deg"></i> ${feature.getLibraryResource().getTitle()}
                                                    </span>
                                                </c:if>
                                            </div>
                                            <div class="d-flex align-items-center gap-1">
                                                <button type="button" class="btn-remove" style="color: var(--ssa);"
                                                        onclick="openEditFeature(${feature.getId()}, this.closest('.feature-item').querySelector('.feature-text').textContent, '${not empty feature.getLibraryResource() ? feature.getLibraryResource().getId() : ''}')"
                                                        title="Edit"><i class="bi bi-pencil"></i></button>
                                                <form method="post" action="ServiceManagerAction" class="d-inline"
                                                      onsubmit="return confirm('Delete this feature?');">
                                                    <input type="hidden" name="action" value="deleteFeature"/>
                                                    <input type="hidden" name="featureId" value="${feature.getId()}"/>
                                                    <input type="hidden" name="enhId"
                                                           value="${selectedEnhancement.getId()}"/>
                                                    <button type="submit" class="btn-remove"><i class="bi bi-x-lg"></i>
                                                    </button>
                                                </form>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><span class="text-muted" style="font-size:0.85rem;">No features yet — click + to add</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>
                </c:if>
                <%-- STATE: Section Selected --%>
                <c:if test="${not empty selectedSection}">

                    <%-- Section Info Card --%>
                    <div class="card mb-3">
                        <div class="hdr-bar"><i class="bi bi-info-circle me-1"></i>Section Details</div>
                        <div class="card-body py-2 px-3">
                            <div class="row">
                                <div class="col-sm-3 fw-semibold">Name</div>
                                <div class="col-sm-9">${selectedSection.getName()}</div>
                            </div>
                            <div class="row mt-1">
                                <div class="col-sm-3 fw-semibold">Scope</div>
                                <div class="col-sm-9">
                                    <span class="badge bg-${selectedSection.getScope() == 'ALL' ? 'primary' : 'info'}">${selectedSection.getScope()}</span>
                                    <small class="text-muted ms-1">${selectedSection.getScope() == 'ALL' ? 'Always shown' : 'Shown when matching LOS selected'}</small>
                                </div>
                            </div>
                            <c:if test="${not empty selectedSection.getDescription()}">
                                <div class="row mt-1">
                                    <div class="col-sm-3 fw-semibold">Description</div>
                                    <div class="col-sm-9">${selectedSection.getDescription()}</div>
                                </div>
                            </c:if>
                            <div class="row mt-1">
                                <div class="col-sm-3 fw-semibold">Status</div>
                                <div class="col-sm-9">
                                    <c:choose>
                                        <c:when test="${selectedSection.isSuppressed()}"><span
                                                class="badge bg-warning text-dark">Suppressed</span></c:when>
                                        <c:otherwise><span class="badge bg-success">Active</span></c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </div>
                    </div>

                    <%-- LOS Associations --%>
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-briefcase me-1"></i>Linked Services</span>
                        </div>
                        <div class="card-body py-2 px-3">
                            <c:choose>
                                <c:when test="${not empty sectionLosItems}">
                                    <c:forEach var="los" items="${sectionLosItems}">
                                        <div class="assoc-row d-flex justify-content-between align-items-center">
                                            <div>
                                                <span class="fw-semibold">${los.getDescription()}</span>
                                                <small class="text-muted ms-2">${los.getShortText()}</small>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><span class="text-muted" style="font-size:0.85rem;">Not linked to any services (scope: ${selectedSection.getScope() == 'ALL' ? 'shows on all applications' : 'LOS-scoped but no LOS assigned'})</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                    <%-- Enhancement Associations --%>
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-puzzle me-1"></i>Linked Enhancements</span>
                        </div>
                        <div class="card-body py-2 px-3">
                            <c:choose>
                                <c:when test="${not empty sectionEnhItems}">
                                    <c:forEach var="enh" items="${sectionEnhItems}">
                                        <div class="assoc-row d-flex justify-content-between align-items-center">
                                            <div>
                                                <span class="fw-semibold">${enh.getDescription()}</span>
                                                <small class="text-muted ms-2">${enh.getShortText()}</small>
                                            </div>
                                        </div>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise><span class="text-muted" style="font-size:0.85rem;">Not linked to any enhancements</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>

                    <%-- Fields List --%>
                    <div class="card mb-3">
                        <div class="hdr-bar d-flex justify-content-between align-items-center">
                            <span><i class="bi bi-input-cursor-text me-1"></i>Fields (${fn:length(selectedSection.getFieldList())})</span>
                            <div>
                                <button type="button" class="btn btn-sm btn-outline-light" title="Show/hide suppressed fields" onclick="toggleSuppressedFields()">
                                    <i class="bi bi-eye-slash" id="toggleFieldSuppIcon"></i>
                                </button>
                                <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addFieldModal" title="Add field">
                                    <i class="bi bi-plus-lg"></i>
                                </button>
                            </div>
                        </div>
                        <div class="card-body py-2 px-3">
                            <c:choose>
                                <c:when test="${not empty selectedSection.getFieldList()}">
                                <div class="field-sort-wrap" id="fieldSortWrap">
                                    <table class="table table-sm table-hover mb-0" style="font-size:0.85rem;">
                                        <thead>
                                        <tr>
                                            <th style="width:3%"></th>
                                            <th style="width:25%">Label</th>
                                            <th style="width:12%">Type</th>
                                            <th style="width:20%">Key</th>
                                            <th style="width:8%">Req</th>
                                            <th style="width:20%">Options</th>
                                            <th style="width:12%" class="text-end">Actions</th>
                                        </tr>
                                        </thead>
                                        <tbody id="fieldSortBody">
                                        <c:forEach var="field" items="${selectedSection.getFieldList()}" varStatus="idx">
                                            <tr class="field-row ${field.isSuppressed() ? 'field-suppressed' : ''}"
                                                data-id="${field.getFieldKey()}" data-field-suppressed="${field.isSuppressed()}">
                                            <td><i class="bi bi-grip-vertical drag-handle"></i></td>
                                                <td class="fw-semibold">${field.getLabel()}</td>
                                                <td><span class="badge bg-light text-dark">${field.getFieldType()}</span></td>
                                                <td><code style="font-size:0.75rem;">${field.getFieldKey()}</code></td>
                                                <td>
                                                    <c:if test="${field.isRequired()}"><i class="bi bi-check-circle-fill text-success"></i></c:if>
                                                    <c:if test="${!field.isRequired()}"><i class="bi bi-circle text-muted" style="font-size:0.7rem;"></i></c:if>
                                                </td>
                                                <td>
                                                    <c:if test="${not empty field.getSelectOptions()}">
                                                        <small class="text-muted">${fn:substring(field.getSelectOptions(), 0, 30)}${fn:length(field.getSelectOptions()) > 30 ? '...' : ''}</small>
                                                    </c:if>
                                                </td>
                                                <td class="text-end text-nowrap">
                                                    <a href="#" class="text-primary me-1" style="font-size:0.8rem;" title="Edit"
                                                       onclick="openEditField('${field.getFieldKey()}', '${fn:escapeXml(field.getLabel())}', '${fn:escapeXml(field.getHelpText())}', ${field.isRequired()}, '${fn:escapeXml(field.getSelectOptions())}', '${field.getFieldType()}'); return false;">
                                                        <i class="bi bi-pencil"></i>
                                                    </a>
                                                    <form method="post" action="ServiceManagerAction" class="d-inline">
                                                        <input type="hidden" name="action" value="suppressAppField"/>
                                                        <input type="hidden" name="sectionId" value="${selectedSection.getId()}"/>
                                                        <input type="hidden" name="fieldKey" value="${field.getFieldKey()}"/>
                                                        <button type="submit" class="btn btn-link p-0 text-muted" style="font-size:0.8rem;" title="${field.isSuppressed() ? 'Unsuppress' : 'Suppress'}">
                                                            <i class="bi ${field.isSuppressed() ? 'bi-eye' : 'bi-eye-slash'}"></i>
                                                        </button>
                                                    </form>
                                                </td>
                                            </tr>
                                        </c:forEach>
                                        </tbody>
                                    </table>
                                </div>
                                </c:when>
                                <c:otherwise><span class="text-muted" style="font-size:0.85rem;">No fields defined yet</span></c:otherwise>
                            </c:choose>
                        </div>
                    </div>


                </c:if>

                <%-- STATE: Nothing selected --%>
                <c:if test="${empty selectedLos && empty selectedEnhancement}">
                    <div class="card">
                        <div class="card-body empty-state"><i class="bi bi-arrow-left-circle"></i>
                            <p class="mb-0 fs-5">Select a line of service or enhancement to manage its associations</p>
                        </div>
                    </div>
                </c:if>

            </div>
        </div>

    </div>
</div>

<%-- ======================== MODALS ======================== --%>

<%-- Add LOS --%>
<div class="modal fade" id="addLosModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="ServiceManagerAction"><input type="hidden" name="action" value="createLos"/>
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-plus-circle me-2"></i>New Line of
                    Service</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3"><label class="form-label fw-semibold">Description</label><input type="text"
                                                                                                      name="description"
                                                                                                      class="form-control"
                                                                                                      required
                                                                                                      placeholder="e.g. Flexible Spending Accounts">
                    </div>
                    <div class="mb-3"><label class="form-label fw-semibold">Short Text</label><input type="text"
                                                                                                     name="shortText"
                                                                                                     class="form-control"
                                                                                                     required
                                                                                                     maxlength="10"
                                                                                                     placeholder="e.g. FSA">
                    </div>
                </div>
                <div class="modal-footer justify-content-center border-0">
                    <button type="submit" class="ssa-action save"><i class="bi bi-plus-circle me-1"></i>Create</button>
                    <span class="ssa-action-sep">|</span>
                    <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%-- Add Enhancement --%>
<div class="modal fade" id="addEnhModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="ServiceManagerAction"><input type="hidden" name="action"
                                                                     value="createEnhancement"/>
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-plus-circle me-2"></i>New Enhancement
                </h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3"><label class="form-label fw-semibold">Description</label><input type="text"
                                                                                                      name="description"
                                                                                                      class="form-control"
                                                                                                      required
                                                                                                      placeholder="e.g. Debit Cards">
                    </div>
                    <div class="mb-3"><label class="form-label fw-semibold">Short Text</label><input type="text"
                                                                                                     name="shortText"
                                                                                                     class="form-control"
                                                                                                     required
                                                                                                     maxlength="20"
                                                                                                     placeholder="e.g. Cards">
                    </div>
                </div>
                <div class="modal-footer justify-content-center border-0">
                    <button type="submit" class="ssa-action save"><i class="bi bi-plus-circle me-1"></i>Create</button>
                    <span class="ssa-action-sep">|</span>
                    <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%-- Edit LOS --%>
<c:if test="${not empty selectedLos}">
    <form id="suppressLosForm" method="post" action="ServiceManagerAction" class="d-none">
        <input type="hidden" name="action" value="suppressLos"/>
        <input type="hidden" name="losId" value="${selectedLos.getId()}"/>
    </form>
    <div class="modal fade" id="editLosModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-pencil me-2"></i>Edit Line of Service
                </h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                </div>
                <form method="post" action="ServiceManagerAction">
                    <input type="hidden" name="action" value="editLos"/><input type="hidden" name="losId"
                                                                               value="${selectedLos.getId()}"/>
                    <div class="modal-body">
                        <div class="mb-3"><label class="form-label fw-semibold">Description</label><input type="text"
                                                                                                          name="description"
                                                                                                          class="form-control"
                                                                                                          required
                                                                                                          value="${selectedLos.getDescription()}">
                        </div>
                        <div class="mb-3"><label class="form-label fw-semibold">Short Text</label><input type="text"
                                                                                                         name="shortText"
                                                                                                         class="form-control"
                                                                                                         required
                                                                                                         maxlength="10"
                                                                                                         value="${selectedLos.getShortText()}">
                        </div>
                    </div>
                    <div class="modal-footer border-0">
                        <button type="button"
                                class="btn btn-sm btn-outline-${selectedLos.isSuppressed() ? 'success' : 'warning'}"
                                onclick="document.getElementById('suppressLosForm').submit();">
                            <i class="bi bi-eye${selectedLos.isSuppressed() ? '' : '-slash'} me-1"></i>${selectedLos.isSuppressed() ? 'Unsuppress' : 'Suppress'}
                        </button>
                        <span class="flex-grow-1"></span>
                        <button type="submit" class="ssa-action save"><i class="bi bi-check-lg me-1"></i>Save</button>
                        <span class="ssa-action-sep">|</span>
                        <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<%-- Edit Enhancement --%>
<c:if test="${not empty selectedEnhancement}">
    <form id="suppressEnhForm" method="post" action="ServiceManagerAction" class="d-none">
        <input type="hidden" name="action" value="suppressEnhancement"/>
        <input type="hidden" name="enhId" value="${selectedEnhancement.getId()}"/>
    </form>
    <div class="modal fade" id="editEnhModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-pencil me-2"></i>Edit Enhancement</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                </div>
                <form method="post" action="ServiceManagerAction">
                    <input type="hidden" name="action" value="editEnhancement"/><input type="hidden" name="enhId"
                                                                                       value="${selectedEnhancement.getId()}"/>
                    <div class="modal-body">
                        <div class="mb-3"><label class="form-label fw-semibold">Description</label><input type="text"
                                                                                                          name="description"
                                                                                                          class="form-control"
                                                                                                          required
                                                                                                          value="${selectedEnhancement.getDescription()}">
                        </div>
                        <div class="mb-3"><label class="form-label fw-semibold">Short Text</label><input type="text"
                                                                                                         name="shortText"
                                                                                                         class="form-control"
                                                                                                         required
                                                                                                         maxlength="20"
                                                                                                         value="${selectedEnhancement.getShortText()}">
                        </div>
                    </div>
                    <div class="modal-footer border-0">
                        <button type="button"
                                class="btn btn-sm btn-outline-${selectedEnhancement.isSuppressed() ? 'success' : 'warning'}"
                                onclick="document.getElementById('suppressEnhForm').submit();">
                            <i class="bi bi-eye${selectedEnhancement.isSuppressed() ? '' : '-slash'} me-1"></i>${selectedEnhancement.isSuppressed() ? 'Unsuppress' : 'Suppress'}
                        </button>
                        <span class="flex-grow-1"></span>
                        <button type="submit" class="ssa-action save"><i class="bi bi-check-lg me-1"></i>Save</button>
                        <span class="ssa-action-sep">|</span>
                        <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<%-- Assign Enhancement to LOS --%>
<c:if test="${not empty selectedLos}">
    <div class="modal fade" id="assignEnhToLosModal" tabindex="-1">
        <div class="modal-dialog modal-sm">
            <div class="modal-content">
                <form method="post" action="ServiceManagerAction"><input type="hidden" name="action"
                                                                         value="assignEnhancementToLos"/><input
                        type="hidden" name="losId" value="${selectedLos.getId()}"/>
                    <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-puzzle me-1"></i>Assign
                        Enhancement</h6>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                    </div>
                    <div class="modal-body py-2"><select name="enhId" class="form-select form-select-sm" required>
                        <option value="">-- Select --</option>
                        <c:forEach var="enh" items="${enhancementList}">
                            <c:set var="alreadyAssigned" value="false"/>
                            <c:forEach var="assigned" items="${losEnhancements}">
                                <c:if test="${assigned.getId() == enh.getId()}"><c:set var="alreadyAssigned"
                                                                                       value="true"/></c:if>
                            </c:forEach>
                            <c:if test="${alreadyAssigned == 'false' && !enh.isSuppressed()}">
                                <option value="${enh.getId()}">${enh.getDescription()} (${enh.getShortText()})</option>
                            </c:if>
                        </c:forEach>
                    </select></div>
                    <div class="modal-footer justify-content-center border-0 py-1">
                        <button type="submit" class="btn btn-sm btn-ssa">Assign</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<%-- Assign LOS to Enhancement --%>
<c:if test="${not empty selectedEnhancement}">
    <div class="modal fade" id="assignLosToEnhModal" tabindex="-1">
        <div class="modal-dialog modal-sm">
            <div class="modal-content">
                <form method="post" action="ServiceManagerAction"><input type="hidden" name="action"
                                                                         value="assignLosToEnhancement"/><input
                        type="hidden" name="enhId" value="${selectedEnhancement.getId()}"/>
                    <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-briefcase me-1"></i>Assign to
                        Line of Service</h6>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                    </div>
                    <div class="modal-body py-2"><select name="losId" class="form-select form-select-sm" required>
                        <option value="">-- Select --</option>
                        <c:forEach var="los" items="${losList}">
                            <c:set var="alreadyAssigned" value="false"/>
                            <c:forEach var="assigned" items="${enhLosItems}">
                                <c:if test="${assigned.getId() == los.getId()}"><c:set var="alreadyAssigned"
                                                                                       value="true"/></c:if>
                            </c:forEach>
                            <c:if test="${alreadyAssigned == 'false' && !los.isSuppressed()}">
                                <option value="${los.getId()}">${los.getDescription()} (${los.getShortText()})</option>
                            </c:if>
                        </c:forEach>
                    </select></div>
                    <div class="modal-footer justify-content-center border-0 py-1">
                        <button type="submit" class="btn btn-sm btn-ssa">Assign</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<%-- Assign App Section to LOS --%>
<c:if test="${not empty selectedLos}">
    <div class="modal fade" id="assignSectionToLosModal" tabindex="-1">
        <div class="modal-dialog modal-sm">
            <div class="modal-content">
                <form method="post" action="ServiceManagerAction"><input type="hidden" name="action"
                                                                         value="assignAppSectionToLos"/><input
                        type="hidden" name="losId" value="${selectedLos.getId()}"/>
                    <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-file-earmark-text me-1"></i>Assign
                        Section</h6>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                    </div>
                    <div class="modal-body py-2"><select name="sectionId" class="form-select form-select-sm" required>
                        <option value="">-- Select --</option>
                        <c:forEach var="section" items="${appSectionList}">
                            <c:set var="alreadyAssigned" value="false"/>
                            <c:forEach var="assigned" items="${losAppSections}">
                                <c:if test="${assigned.getId() == section.getId()}"><c:set var="alreadyAssigned"
                                                                                           value="true"/></c:if>
                            </c:forEach>
                            <c:if test="${alreadyAssigned == 'false' && section.getScope() != 'ALL'}">
                                <option value="${section.getId()}">${section.getName()}</option>
                            </c:if>
                        </c:forEach>
                    </select></div>
                    <div class="modal-footer justify-content-center border-0 py-1">
                        <button type="submit" class="btn btn-sm btn-ssa">Assign</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<%-- Assign App Section to Enhancement --%>
<c:if test="${not empty selectedEnhancement}">
    <div class="modal fade" id="assignSectionToEnhModal" tabindex="-1">
        <div class="modal-dialog modal-sm">
            <div class="modal-content">
                <form method="post" action="ServiceManagerAction"><input type="hidden" name="action"
                                                                         value="assignAppSectionToEnhancement"/><input
                        type="hidden" name="enhId" value="${selectedEnhancement.getId()}"/>
                    <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-file-earmark-text me-1"></i>Assign
                        Section</h6>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                    </div>
                    <div class="modal-body py-2"><select name="sectionId" class="form-select form-select-sm" required>
                        <option value="">-- Select --</option>
                        <c:forEach var="section" items="${appSectionList}">
                            <c:set var="alreadyAssigned" value="false"/>
                            <c:forEach var="assigned" items="${enhAppSections}">
                                <c:if test="${assigned.getId() == section.getId()}"><c:set var="alreadyAssigned"
                                                                                           value="true"/></c:if>
                            </c:forEach>
                            <c:if test="${alreadyAssigned == 'false' && section.getScope() != 'ALL'}">
                                <option value="${section.getId()}">${section.getName()}</option>
                            </c:if>
                        </c:forEach>
                    </select></div>
                    <div class="modal-footer justify-content-center border-0 py-1">
                        <button type="submit" class="btn btn-sm btn-ssa">Assign</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<%-- Add Feature --%>
<c:if test="${not empty selectedModule}">
    <div class="modal fade" id="addFeatureModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <form method="post" action="ServiceManagerAction">
                    <input type="hidden" name="action" value="createFeature"/>
                    <input type="hidden" name="moduleId" value="${selectedModule.getId()}"/>
                    <c:if test="${not empty selectedLos}"><input type="hidden" name="losId"
                                                                 value="${selectedLos.getId()}"/></c:if>
                    <c:if test="${not empty selectedEnhancement}"><input type="hidden" name="enhId"
                                                                         value="${selectedEnhancement.getId()}"/></c:if>
                    <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-plus-circle me-2"></i>Add Feature
                    </h6>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label fw-semibold">Feature Text <span
                                    class="text-danger">*</span></label>
                            <textarea name="description" id="addFeatureDesc" class="form-control" rows="2" required
                                      maxlength="500" placeholder="e.g. Online Web Access and Claim Filing"></textarea>
                            <div class="link-toolbar">
                                <button type="button" class="btn btn-outline-secondary"
                                        onclick="insertLink('addFeatureDesc','addFeatureLinkRes')"
                                        title="Wrap selected text as a resource link"><i
                                        class="bi bi-link-45deg me-1"></i>Link Selection
                                </button>
                                <select id="addFeatureLinkRes" class="form-select form-select-sm"
                                        style="max-width:250px;">
                                    <option value="">-- Pick resource --</option>
                                    <c:forEach var="res" items="${libraryResources}">
                                        <option value="${res.getId()}">${res.getTitle()}</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-semibold">Linked Resource <small class="text-muted fw-normal">(icon
                                at end)</small></label>
                            <select name="libraryResourceId" class="form-select">
                                <option value="">-- None --</option>
                                <c:forEach var="res" items="${libraryResources}">
                                    <option value="${res.getId()}">${res.getTitle()}<c:if
                                            test="${not empty res.getCategory()}">
                                        (${res.getCategory().getName()})</c:if></option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <div class="modal-footer justify-content-center border-0">
                        <button type="submit" class="ssa-action save"><i class="bi bi-plus-circle me-1"></i>Add Feature</button>
                        <span class="ssa-action-sep">|</span>
                        <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<%-- Edit Feature --%>
<c:if test="${not empty selectedModule}">
    <div class="modal fade" id="editFeatureModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <form method="post" action="ServiceManagerAction">
                    <input type="hidden" name="action" value="editFeature"/>
                    <input type="hidden" name="featureId" id="editFeatureId"/>
                    <c:if test="${not empty selectedLos}"><input type="hidden" name="losId"
                                                                 value="${selectedLos.getId()}"/></c:if>
                    <c:if test="${not empty selectedEnhancement}"><input type="hidden" name="enhId"
                                                                         value="${selectedEnhancement.getId()}"/></c:if>
                    <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-pencil me-2"></i>Edit Feature</h6>
                        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label fw-semibold">Feature Text <span
                                    class="text-danger">*</span></label>
                            <textarea name="description" id="editFeatureDesc" class="form-control" rows="2" required
                                      maxlength="500"></textarea>
                            <div class="link-toolbar">
                                <button type="button" class="btn btn-outline-secondary"
                                        onclick="insertLink('editFeatureDesc','editFeatureLinkRes')"
                                        title="Wrap selected text as a resource link"><i
                                        class="bi bi-link-45deg me-1"></i>Link Selection
                                </button>
                                <select id="editFeatureLinkRes" class="form-select form-select-sm"
                                        style="max-width:250px;">
                                    <option value="">-- Pick resource --</option>
                                    <c:forEach var="res" items="${libraryResources}">
                                        <option value="${res.getId()}">${res.getTitle()}</option>
                                    </c:forEach>
                                </select>
                            </div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-semibold">Linked Resource <small class="text-muted fw-normal">(icon
                                at end)</small></label>
                            <select name="libraryResourceId" id="editFeatureResId" class="form-select">
                                <option value="">-- None --</option>
                                <c:forEach var="res" items="${libraryResources}">
                                    <option value="${res.getId()}">${res.getTitle()}<c:if
                                            test="${not empty res.getCategory()}">
                                        (${res.getCategory().getName()})</c:if></option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                    <div class="modal-footer justify-content-center border-0">
                        <button type="submit" class="ssa-action save"><i class="bi bi-check-lg me-1"></i>Save</button>
                        <span class="ssa-action-sep">|</span>
                        <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<%-- Add App Section --%>
<div class="modal fade" id="addSectionModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="ServiceManagerAction"><input type="hidden" name="action"
                                                                     value="createAppSection"/>
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-plus-circle me-2"></i>New Application
                    Section</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3"><label class="form-label fw-semibold">Name</label><input type="text" name="name"
                                                                                               class="form-control"
                                                                                               required maxlength="100"
                                                                                               placeholder="e.g. Dependent Care FSA">
                    </div>
                    <div class="mb-3"><label class="form-label fw-semibold">Description</label><input type="text"
                                                                                                      name="description"
                                                                                                      class="form-control"
                                                                                                      maxlength="500"
                                                                                                      placeholder="Optional description">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Scope</label>
                        <select name="scope" class="form-select" required>
                            <option value="ALL">ALL — Always shown on every application</option>
                            <option value="LOS">LOS — Only when a matching service is selected</option>
                        </select>
                    </div>
                </div>
                <div class="modal-footer justify-content-center border-0">
                    <button type="submit" class="ssa-action save"><i class="bi bi-plus-circle me-1"></i>Create</button>
                    <span class="ssa-action-sep">|</span>
                    <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%-- Load Starter Package --%>
<div class="modal fade" id="loadPackageModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="ServiceManagerAction">
                <input type="hidden" name="action" value="loadStarterPackage"/>
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                    <h6 class="modal-title fw-semibold"><i class="bi bi-box-seam me-2"></i>Load Starter Package</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                </div>
                <div class="modal-body">
                    <p class="text-muted mb-3" style="font-size:0.85rem;">Select a pre-configured package to load its application sections and fields. Sections that already exist will be skipped.</p>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Package</label>
                        <select name="packageId" class="form-select" required>
                            <option value="">-- Select a package --</option>
                            <c:forEach var="pkg" items="${availablePackages}">
                                <option value="${pkg.id()}">${pkg.name()} - ${pkg.description()}</option>
                            </c:forEach>
                        </select>
                    </div>
                </div>
                <div class="modal-footer justify-content-center border-0">
                    <button type="submit" class="ssa-action save"><i class="bi bi-box-seam me-1"></i>Load Package</button>
                    <span class="ssa-action-sep">|</span>
                    <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%-- Edit App Section --%>
<c:if test="${not empty selectedSection}">
    <form id="suppressSectionForm" method="post" action="ServiceManagerAction" class="d-none">
        <input type="hidden" name="action" value="suppressAppSection"/>
        <input type="hidden" name="sectionId" value="${selectedSection.getId()}"/>
    </form>
    <div class="modal fade" id="editSectionModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-pencil me-2"></i>Edit Application
                    Section</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button>
                </div>
                <form method="post" action="ServiceManagerAction">
                    <input type="hidden" name="action" value="editAppSection"/><input type="hidden" name="sectionId"
                                                                                      value="${selectedSection.getId()}"/>
                    <div class="modal-body">
                        <div class="mb-3"><label class="form-label fw-semibold">Name</label><input type="text"
                                                                                                   name="name"
                                                                                                   class="form-control"
                                                                                                   required
                                                                                                   maxlength="100"
                                                                                                   value="${selectedSection.getName()}">
                        </div>
                        <div class="mb-3"><label class="form-label fw-semibold">Description</label><input type="text"
                                                                                                          name="description"
                                                                                                          class="form-control"
                                                                                                          maxlength="500"
                                                                                                          value="${selectedSection.getDescription()}">
                        </div>
                        <div class="mb-3">
                            <label class="form-label fw-semibold">Scope</label>
                            <select name="scope" class="form-select" required>
                                <option value="ALL" ${selectedSection.getScope() == 'ALL' ? 'selected' : ''}>ALL —
                                    Always shown on every application
                                </option>
                                <option value="LOS" ${selectedSection.getScope() == 'LOS' ? 'selected' : ''}>LOS — Only
                                    when a matching service is selected
                                </option>
                            </select>
                        </div>
                    </div>
                    <div class="modal-footer border-0">
                        <button type="button"
                                class="btn btn-sm btn-outline-${selectedSection.isSuppressed() ? 'success' : 'warning'}"
                                onclick="document.getElementById('suppressSectionForm').submit();">
                            <i class="bi bi-eye${selectedSection.isSuppressed() ? '' : '-slash'} me-1"></i>${selectedSection.isSuppressed() ? 'Unsuppress' : 'Suppress'}
                        </button>
                        <span class="flex-grow-1"></span>
                        <button type="submit" class="ssa-action save"><i class="bi bi-check-lg me-1"></i>Save</button>
                        <span class="ssa-action-sep">|</span>
                        <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</c:if>

<%-- Add Field Modal --%>
<c:if test="${not empty selectedSection}">
    <div class="modal fade" id="addFieldModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
        <form method="post" action="ServiceManagerAction"><input type="hidden" name="action" value="createAppField"/><input type="hidden" name="sectionId" value="${selectedSection.getId()}"/>
            <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-plus-circle me-2"></i>New Field</h6><button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button></div>
            <div class="modal-body">
                <div class="mb-3"><label class="form-label fw-semibold">Label <small class="text-muted fw-normal">(displayed to user)</small></label><input type="text" name="label" class="form-control" required maxlength="200" placeholder="e.g. Company Legal Name"></div>
                <div class="mb-3"><label class="form-label fw-semibold">Field Key <small class="text-muted fw-normal">(internal, unique, cannot change later)</small></label><input type="text" name="fieldKey" id="newFieldKey" class="form-control" required maxlength="100" placeholder="e.g. company_legal_name" pattern="[a-z0-9_]+" title="Lowercase letters, numbers, underscores only"></div>
                <div class="mb-3">
                    <label class="form-label fw-semibold">Field Type</label>
                    <select name="fieldType" class="form-select" required>
                        <option value="TEXT">TEXT — Single line</option>
                        <option value="TEXTAREA">TEXTAREA — Multi-line</option>
                        <option value="NUMBER">NUMBER</option>
                        <option value="DATE">DATE</option>
                        <option value="SELECT">SELECT — Dropdown</option>
                        <option value="RADIO">RADIO — Radio buttons</option>
                        <option value="BOOLEAN">BOOLEAN — Yes/No</option>
                        <option value="CHECKBOX">CHECKBOX — Multi-select checkboxes</option>
                        <option value="JSON">JSON — Complex data (file uploads, etc.)</option>
                    </select>
                </div>
                <div class="mb-3"><label class="form-label fw-semibold">Select Options <small class="text-muted fw-normal">(pipe-delimited, for SELECT/RADIO/CHECKBOX)</small></label><input type="text" name="selectOptions" class="form-control" maxlength="500" placeholder="e.g. Option A|Option B|Option C"></div>
                <div class="mb-3"><label class="form-label fw-semibold">Help Text</label><input type="text" name="helpText" class="form-control" maxlength="500" placeholder="Optional tooltip or guidance"></div>
                <div class="form-check mb-3"><input class="form-check-input" type="checkbox" name="isRequired" id="newFieldReq"><label class="form-check-label fw-semibold" for="newFieldReq">Required</label></div>
            </div>
            <div class="modal-footer justify-content-center border-0"><button type="submit" class="ssa-action save"><i class="bi bi-plus-circle me-1"></i>Create</button><span class="ssa-action-sep">|</span><button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button></div>
        </form>
    </div></div></div>

    <%-- Edit Field Modal --%>
    <div class="modal fade" id="editFieldModal" tabindex="-1"><div class="modal-dialog"><div class="modal-content">
        <form method="post" action="ServiceManagerAction"><input type="hidden" name="action" value="editAppField"/><input type="hidden" name="sectionId" value="${selectedSection.getId()}"/><input type="hidden" name="fieldKey" id="editFieldKey"/>
            <div class="modal-header py-2" style="background-color: var(--ssa); color: white;"><h6 class="modal-title fw-semibold"><i class="bi bi-pencil me-2"></i>Edit Field</h6><button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="close"></button></div>
            <div class="modal-body">
                <div class="mb-3">
                    <label class="form-label fw-semibold">Field Key</label>
                    <input type="text" class="form-control" id="editFieldKeyDisplay" disabled>
                </div>
                <div class="mb-3">
                    <label class="form-label fw-semibold">Field Type</label>
                    <input type="text" class="form-control" id="editFieldTypeDisplay" disabled>
                    <small class="text-muted">Type cannot be changed. Suppress this field and create a new one if a different type is needed.</small>
                </div>
                <div class="mb-3"><label class="form-label fw-semibold">Label</label><input type="text" name="label" id="editFieldLabel" class="form-control" required maxlength="200"></div>
                <div class="mb-3"><label class="form-label fw-semibold">Select Options <small class="text-muted fw-normal">(pipe-delimited)</small></label><input type="text" name="selectOptions" id="editFieldOptions" class="form-control" maxlength="500"></div>
                <div class="mb-3"><label class="form-label fw-semibold">Help Text</label><input type="text" name="helpText" id="editFieldHelp" class="form-control" maxlength="500"></div>
                <div class="form-check mb-3"><input class="form-check-input" type="checkbox" name="isRequired" id="editFieldReq"><label class="form-check-label fw-semibold" for="editFieldReq">Required</label></div>
            </div>
            <div class="modal-footer justify-content-center border-0"><button type="submit" class="ssa-action save"><i class="bi bi-check-lg me-1"></i>Save</button><span class="ssa-action-sep">|</span><button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button></div>
        </form>
    </div></div></div>
</c:if>


<script src="https://cdn.jsdelivr.net/npm/sortablejs@1.15.0/Sortable.min.js"></script>
<script>
    // ── Tab-aware add button ──────────────────────────────────────────
    const addBtn = document.getElementById('addBtn');
    const loadPkgBtn = document.getElementById('loadPkgBtn');
    const tabModalMap = { losTab: '#addLosModal', enhTab: '#addEnhModal', secTab: '#addSectionModal' };
    Object.entries(tabModalMap).forEach(([tabId, modalTarget]) => {
        const tabEl = document.getElementById(tabId);
        if (tabEl) {
            tabEl.addEventListener('shown.bs.tab', () => {
                addBtn.setAttribute('data-bs-target', modalTarget);
                if (loadPkgBtn) loadPkgBtn.style.display = (tabId === 'secTab') ? 'inline-block' : 'none';
            });
        }
    });
    // Set initial state based on active tab
    const activeTabEl = document.querySelector('.list-tabs .nav-link.active');
    if (activeTabEl && tabModalMap[activeTabEl.id]) {
        addBtn.setAttribute('data-bs-target', tabModalMap[activeTabEl.id]);
    }


    // ── Suppress toggle ──────────────────────────────────────────────
    let showSuppressed = sessionStorage.getItem('smShowSuppressed') === 'true';

    // ── Field suppress toggle ─────────────────────────────────────────
    let showSuppressedFields = false;

    function toggleSuppressedFields() {
        showSuppressedFields = !showSuppressedFields;
        const icon = document.getElementById('toggleFieldSuppIcon');
        if (icon) icon.className = showSuppressedFields ? 'bi bi-eye' : 'bi bi-eye-slash';
        const wrap = document.getElementById('fieldSortWrap');
        if (wrap) {
            wrap.classList.toggle('show-field-suppressed', showSuppressedFields);
        }
    }

    function applySuppressedState() {
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

    function toggleSuppressed() {
        showSuppressed = !showSuppressed;
        sessionStorage.setItem('smShowSuppressed', showSuppressed);
        applySuppressedState();
    }

    applySuppressedState();

    // ── Scroll state preservation ─────────────────────────────────────
    const losScroll = document.getElementById('losScroll');
    const enhScroll = document.getElementById('enhScroll');
    const secScroll = document.getElementById('secScroll');
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
    if (secScroll) {
        const savedSec = sessionStorage.getItem('smSecScroll');
        if (savedSec) secScroll.scrollTop = parseInt(savedSec);
        secScroll.addEventListener('scroll', () => sessionStorage.setItem('smSecScroll', secScroll.scrollTop));
    }


    // ── Application Section preview popovers ──────────────────────────
    document.querySelectorAll('.section-preview').forEach(link => {
        const sectionId = link.dataset.sectionId;
        const fieldsDiv = document.getElementById('fields-' + sectionId);
        if (fieldsDiv) {
            new bootstrap.Popover(link, {
                trigger: 'focus',
                placement: 'left',
                html: true,
                sanitize: false,
                customClass: 'preview-popover',
                title: link.closest('.assoc-row').querySelector('.fw-semibold').textContent,
                content: fieldsDiv.innerHTML
            });
        }
    });

    // ── Edit Feature modal population ─────────────────────────────────
    function openEditFeature(id, desc, resId) {
        document.getElementById('editFeatureId').value = id;
        document.getElementById('editFeatureDesc').value = desc;
        document.getElementById('editFeatureResId').value = resId || '';
        new bootstrap.Modal(document.getElementById('editFeatureModal')).show();
    }

    // ── Edit Field modal population ───────────────────────────────────
    function openEditField(key, label, helpText, isRequired, selectOptions, fieldType) {
        document.getElementById('editFieldKey').value = key;
        document.getElementById('editFieldKeyDisplay').value = key;
        document.getElementById('editFieldTypeDisplay').value = fieldType;
        document.getElementById('editFieldLabel').value = label;
        document.getElementById('editFieldHelp').value = helpText || '';
        document.getElementById('editFieldOptions').value = selectOptions || '';
        document.getElementById('editFieldReq').checked = isRequired;
        new bootstrap.Modal(document.getElementById('editFieldModal')).show();
    }

    // ── Auto-generate field key from label ────────────────────────────
    const addFieldModal = document.getElementById('addFieldModal');
    if (addFieldModal) {
        const labelInput = addFieldModal.querySelector('input[name="label"]');
        const keyInput = document.getElementById('newFieldKey');
        if (labelInput && keyInput) {
            labelInput.addEventListener('input', function() {
                // Only auto-fill if user hasn't manually edited the key
                if (!keyInput.dataset.manual) {
                    keyInput.value = this.value.toLowerCase()
                        .replace(/[^a-z0-9\s]/g, '')
                        .replace(/\s+/g, '_')
                        .substring(0, 100);
                }
            });
            keyInput.addEventListener('input', function() {
                keyInput.dataset.manual = 'true';
            });
            // Reset manual flag when modal opens
            addFieldModal.addEventListener('show.bs.modal', function() {
                delete keyInput.dataset.manual;
            });
        }
    }

    // ── Inline link insertion ─────────────────────────────────────────
    // Select text in textarea → pick resource → click "Link Selection"
    // Wraps selection as [selected text](resourceId)
    function insertLink(textareaId, selectId) {
        const ta = document.getElementById(textareaId);
        const sel = document.getElementById(selectId);
        const resId = sel.value;
        if (!resId) {
            alert('Pick a resource first.');
            return;
        }

        const start = ta.selectionStart;
        const end = ta.selectionEnd;
        if (start === end) {
            alert('Select some text in the feature field first.');
            return;
        }

        const text = ta.value;
        const selected = text.substring(start, end);
        const linked = '[' + selected + '](' + resId + ')';
        ta.value = text.substring(0, start) + linked + text.substring(end);
        ta.focus();
        ta.selectionStart = start;
        ta.selectionEnd = start + linked.length;
        sel.value = '';
    }

    // ── Drag-and-drop sorting (SortableJS) ────────────────────────────
    function initSortable(containerId, itemClass, type) {
        const el = document.getElementById(containerId);
        if (!el || el.querySelectorAll('.' + itemClass).length === 0) return;
        new Sortable(el, {
            handle: '.drag-handle',
            animation: 150,
            draggable: '.' + itemClass,
            ghostClass: 'sortable-ghost',
            filter: 'form, button, a.preview-link',
            preventOnFilter: false,
            onEnd: function () {
                const ids = Array.from(el.querySelectorAll('.' + itemClass)).map(item => item.dataset.id);
                const params = new URLSearchParams();
                params.append('type', type);
                ids.forEach(id => params.append('ids[]', id));
                fetch('ServiceManagerSort', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                    body: params.toString()
                });
            }
        });
    }

    initSortable('losScroll', 'los-item', 'los');
    initSortable('enhScroll', 'enh-item', 'enhancement');
    initSortable('losSectionList', 'los-section-item', 'appSection');
    initSortable('enhSectionList', 'enh-section-item', 'appSection');
    initSortable('losFeatureList', 'feature-item', 'feature');
    initSortable('enhFeatureList', 'feature-item', 'feature');
    initSortable('secScroll', 'sec-item', 'appSection');
    initSortable('fieldSortBody', 'field-row', 'appField');


</script>
</body>
</html>
