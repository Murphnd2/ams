<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Questionnaire Manager</title>
    <style>
        .q-item {
            padding: 10px 14px;
            border-bottom: 1px solid #eee;
            cursor: pointer;
            transition: background 0.15s;
            display: flex;
            align-items: center;
            gap: 8px;
            text-decoration: none;
            color: inherit;
        }
        .q-item:hover { background: #f0f7fa; color: inherit; }
        .q-item.active { background: #e8f4f8; border-left: 3px solid var(--ssa); }
        .q-name { font-size: 0.9rem; font-weight: 500; }
        .q-suppressed { display: none !important; }
        .q-list-scroll.show-suppressed .q-suppressed { display: flex !important; opacity: 0.45; font-style: italic; }
        .q-list-scroll.show-suppressed .q-suppressed.active { opacity: 0.7; }
        .q-panel { background: #fff; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); display: flex; flex-direction: column; height: calc(100vh - 80px); }
        .q-panel-header { background: var(--ssa); color: #fff; padding: 12px 16px; border-radius: 8px 8px 0 0; font-weight: 600; font-size: 0.95rem; flex-shrink: 0; }
        .q-list-scroll { flex: 1 1 0; min-height: 80px; overflow-y: auto; }
        .detail-scroll { overflow-y: auto; }
        @media (min-width: 992px) {
            .detail-scroll { max-height: calc(100vh - 120px); }
        }
        .filter-tabs .btn { font-size: 0.78rem; padding: 4px 8px; }
        .filter-tabs .btn.active-filter { background: var(--ssa); color: #fff; border-color: var(--ssa); }
        .suppress-toggle { font-size: 0.75rem; cursor: pointer; user-select: none; }
        .suppress-toggle:hover { color: var(--ssa); }
        .field-suppressed { display: none !important; }
        .field-table-wrap.show-field-suppressed .field-suppressed { display: table-row !important; opacity: 0.45; font-style: italic; }
        .section-header-row td { background: #f0f4f8; font-weight: 600; font-size: 0.8rem; color: #555; }
        .mode-native { background: #e0f2fe; color: #0369a1; }
        .mode-external { background: #f3e8ff; color: #7c3aed; }
        .type-chip { font-size: 0.7rem; padding: 2px 6px; border-radius: 3px; font-weight: 600; background: #e9ecef; color: #555; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<% String flash = (String) session.getAttribute("flashMessage"); if (flash != null) { session.removeAttribute("flashMessage"); %>
<div class="alert alert-success alert-dismissible fade show mt-2 mb-0 mx-3" role="alert">
    <i class="bi bi-check-circle me-1"></i><%= flash %>
    <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
</div>
<% } %>

<div class="container-fluid px-3 px-lg-4 mt-2">
    <div class="row g-3">

        <%-- ======================== LEFT PANEL ======================== --%>
        <div class="col-lg-4 col-xl-3">
            <div class="q-panel">
                <div class="q-panel-header d-flex justify-content-between align-items-center">
                    <span><i class="bi bi-ui-checks-grid me-1"></i> Questionnaire Templates</span>
                    <div class="d-flex align-items-center gap-2">
                        <span class="badge bg-light text-dark">${totalCount}</span>
                        <span class="suppress-toggle" onclick="toggleSuppressed()" title="Show/hide suppressed">
                            <i class="bi bi-eye-slash" id="suppressIcon"></i>
                        </span>
                        <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addQuestionnaireModal" title="New Questionnaire">
                            <i class="bi bi-plus-lg"></i>
                        </button>
                    </div>
                </div>

                <%-- Filter Tabs --%>
                <div class="px-2 pt-2 pb-2" style="background: #fafbfc; border-bottom: 1px solid #eee; flex-shrink: 0;">
                    <div class="btn-group w-100 filter-tabs" role="group">
                        <button type="button" class="btn btn-outline-secondary btn-sm active-filter" onclick="filterQ('all',this)">All</button>
                        <button type="button" class="btn btn-outline-secondary btn-sm" onclick="filterQ('native',this)">
                            <i class="bi bi-pencil-square"></i> ${nativeCount}
                        </button>
                        <button type="button" class="btn btn-outline-secondary btn-sm" onclick="filterQ('external',this)">
                            <i class="bi bi-box-arrow-up-right"></i> ${externalCount}
                        </button>
                    </div>
                </div>

                <%-- Search --%>
                <div class="px-2 py-2" style="border-bottom: 1px solid #eee; flex-shrink: 0;">
                    <input type="text" class="form-control form-control-sm" placeholder="Search..." id="qSearch" oninput="searchQ(this.value)">
                </div>

                <%-- Questionnaire List --%>
                <div class="q-list-scroll" id="qListScroll">
                    <c:choose>
                        <c:when test="${not empty questionnaireList}">
                            <c:forEach var="q" items="${questionnaireList}">
                                <a href="QuestionnaireManager25?qId=${q.getId()}"
                                   class="q-item ${q.isSuppressed() ? 'q-suppressed' : ''} ${selectedQuestionnaire != null && selectedQuestionnaire.getId() == q.getId() ? 'active' : ''}"
                                   data-suppressed="${q.isSuppressed()}"
                                   data-mode="${q.isExternal() ? 'external' : 'native'}"
                                   data-name="${fn:toLowerCase(q.getName())}">
                                    <div class="d-flex flex-column flex-grow-1" style="min-width:0;">
                                        <div class="d-flex align-items-center gap-1">
                                            <span class="q-name text-truncate">${q.getName()}</span>
                                            <c:if test="${q.isSuppressed()}"><i class="bi bi-eye-slash-fill text-muted" style="font-size:0.7rem;"></i></c:if>
                                        </div>
                                        <div class="d-flex align-items-center gap-1 mt-1">
                                            <span class="badge ${q.isExternal() ? 'mode-external' : 'mode-native'}" style="font-size:0.65rem;">
                                                ${q.isExternal() ? 'EXTERNAL' : 'NATIVE'}
                                            </span>
                                            <span class="type-chip">${q.getActivityType()}</span>
                                            <c:if test="${!q.isExternal() && not empty q.getFieldList()}">
                                                <span class="badge bg-secondary ms-auto" style="font-size:0.6rem;">${fn:length(q.getFieldList())} fields</span>
                                            </c:if>
                                        </div>
                                    </div>
                                </a>
                            </c:forEach>
                        </c:when>
                        <c:otherwise>
                            <div class="text-center py-4 text-muted">
                                <i class="bi bi-ui-checks-grid" style="font-size:2rem;"></i>
                                <p class="mb-0 mt-2">No questionnaire templates</p>
                            </div>
                        </c:otherwise>
                    </c:choose>
                </div>
            </div>
        </div>

        <%-- ======================== RIGHT PANEL ======================== --%>
        <div class="col-lg-8 col-xl-9">

            <c:choose>
                <c:when test="${not empty selectedQuestionnaire}">

                    <%-- Header Bar --%>
                    <h5 class="mb-2 px-3 py-2 rounded d-flex align-items-center" style="background-color: #87a948; color: white;">
                        <i class="bi bi-ui-checks-grid me-1"></i>
                        ${selectedQuestionnaire.getName()}
                        <span class="badge ${selectedQuestionnaire.isExternal() ? 'mode-external' : 'mode-native'} ms-2" style="font-size:0.7rem;">
                            ${selectedQuestionnaire.isExternal() ? 'EXTERNAL' : 'NATIVE'}
                        </span>
                        <span class="badge bg-light text-dark ms-1" style="font-size:0.7rem;">${selectedQuestionnaire.getActivityType()}</span>
                        <a href="#" class="ms-2" style="color: white;" data-bs-toggle="modal" data-bs-target="#editQuestionnaireModal" title="Edit">
                            <i class="bi bi-pencil"></i>
                        </a>
                        <form method="post" action="QuestionnaireAction25" class="ms-auto d-inline">
                            <input type="hidden" name="action" value="suppressQuestionnaire"/>
                            <input type="hidden" name="qId" value="${selectedQuestionnaire.getId()}"/>
                            <button type="submit" class="btn btn-sm btn-outline-light" title="${selectedQuestionnaire.isSuppressed() ? 'Unsuppress' : 'Suppress'}">
                                <i class="bi ${selectedQuestionnaire.isSuppressed() ? 'bi-eye' : 'bi-eye-slash'}"></i>
                                ${selectedQuestionnaire.isSuppressed() ? 'Unsuppress' : 'Suppress'}
                            </button>
                        </form>
                    </h5>

                    <div class="detail-scroll">

                        <%-- Info Card --%>
                        <div class="card mb-3">
                            <div class="hdr-bar"><i class="bi bi-info-circle me-1"></i>Details</div>
                            <div class="card-body py-2 px-3">
                                <c:if test="${not empty selectedQuestionnaire.getDescription()}">
                                    <div class="row">
                                        <div class="col-sm-3 fw-semibold">Description</div>
                                        <div class="col-sm-9">${selectedQuestionnaire.getDescription()}</div>
                                    </div>
                                </c:if>
                                <div class="row mt-1">
                                    <div class="col-sm-3 fw-semibold">Mode</div>
                                    <div class="col-sm-9">
                                        <c:choose>
                                            <c:when test="${selectedQuestionnaire.isExternal()}">
                                                <span class="badge mode-external">External Form</span>
                                            </c:when>
                                            <c:otherwise>
                                                <span class="badge mode-native">Native AMS Form</span>
                                                <small class="text-muted ms-1">${fn:length(selectedQuestionnaire.getFieldList())} fields</small>
                                            </c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                                <div class="row mt-1">
                                    <div class="col-sm-3 fw-semibold">Activity Type</div>
                                    <div class="col-sm-9"><span class="type-chip">${selectedQuestionnaire.getActivityType()}</span></div>
                                </div>
                                <div class="row mt-1">
                                    <div class="col-sm-3 fw-semibold">Status</div>
                                    <div class="col-sm-9">
                                        <c:choose>
                                            <c:when test="${selectedQuestionnaire.isSuppressed()}"><span class="badge bg-warning text-dark">Suppressed</span></c:when>
                                            <c:otherwise><span class="badge bg-success">Active</span></c:otherwise>
                                        </c:choose>
                                    </div>
                                </div>
                                <c:if test="${selectedQuestionnaire.isExternal()}">
                                    <div class="row mt-2">
                                        <div class="col-sm-3 fw-semibold">External URL</div>
                                        <div class="col-sm-9">
                                            <code style="font-size:0.8rem; word-break:break-all;">${selectedQuestionnaire.getExternalUrl()}</code>
                                        </div>
                                    </div>
                                    <div class="row mt-1">
                                        <div class="col-sm-3"></div>
                                        <div class="col-sm-9">
                                            <small class="text-muted">Merge tokens: <code>{erName}</code> <code>{activityId}</code> <code>{instanceGuid}</code></small>
                                        </div>
                                    </div>
                                </c:if>
                                <c:if test="${not empty selectedQuestionnaire.getTemplateKey()}">
                                    <div class="row mt-1">
                                        <div class="col-sm-3 fw-semibold">Template Key</div>
                                        <div class="col-sm-9"><code style="font-size:0.8rem;">${selectedQuestionnaire.getTemplateKey()}</code></div>
                                    </div>
                                </c:if>
                            </div>
                        </div>

                        <%-- Scoping Card --%>
                        <div class="card mb-3">
                            <div class="hdr-bar"><i class="bi bi-funnel me-1"></i>Scoping</div>
                            <div class="card-body py-2 px-3">
                                <form method="post" action="QuestionnaireAction25">
                                    <input type="hidden" name="action" value="updateScope"/>
                                    <input type="hidden" name="qId" value="${selectedQuestionnaire.getId()}"/>

                                    <%-- LOS Checkboxes --%>
                                    <c:if test="${not empty allLos}">
                                        <div class="mb-3">
                                            <label class="form-label fw-semibold" style="font-size:0.9rem;">Lines of Service</label>
                                            <c:forEach var="los" items="${allLos}">
                                                <div class="form-check">
                                                    <input class="form-check-input" type="checkbox" name="losIds"
                                                           value="${los.getId()}" id="qLos-${los.getId()}"
                                                           <c:forEach var="linked" items="${selectedQuestionnaire.getLosList()}">
                                                               <c:if test="${linked.getId() == los.getId()}">checked</c:if>
                                                           </c:forEach>>
                                                    <label class="form-check-label" for="qLos-${los.getId()}">${los.getDescription()}</label>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </c:if>

                                    <%-- Enhancement Checkboxes --%>
                                    <c:if test="${not empty allEnhancements}">
                                        <div class="mb-3">
                                            <label class="form-label fw-semibold" style="font-size:0.9rem;">Enhancements</label>
                                            <c:forEach var="enh" items="${allEnhancements}">
                                                <div class="form-check">
                                                    <input class="form-check-input" type="checkbox" name="enhIds"
                                                           value="${enh.getId()}" id="qEnh-${enh.getId()}"
                                                           <c:forEach var="linked" items="${selectedQuestionnaire.getEnhancementList()}">
                                                               <c:if test="${linked.getId() == enh.getId()}">checked</c:if>
                                                           </c:forEach>>
                                                    <label class="form-check-label" for="qEnh-${enh.getId()}">${enh.getDescription()}</label>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </c:if>

                                    <%-- ServiceItem Checkboxes --%>
                                    <c:if test="${not empty allServiceItems}">
                                        <div class="mb-3">
                                            <label class="form-label fw-semibold" style="font-size:0.9rem;">Service Items</label>
                                            <c:forEach var="si" items="${allServiceItems}">
                                                <div class="form-check">
                                                    <input class="form-check-input" type="checkbox" name="serviceItemIds"
                                                           value="${si.getId()}" id="qSi-${si.getId()}"
                                                           <c:forEach var="linked" items="${selectedQuestionnaire.getServiceItemList()}">
                                                               <c:if test="${linked.getId() == si.getId()}">checked</c:if>
                                                           </c:forEach>>
                                                    <label class="form-check-label" for="qSi-${si.getId()}">${si.getDescription()}</label>
                                                </div>
                                            </c:forEach>
                                        </div>
                                    </c:if>

                                    <button type="submit" class="btn btn-sm btn-outline-primary">
                                        <i class="bi bi-check-lg me-1"></i>Save Scope
                                    </button>
                                </form>
                            </div>
                        </div>

                        <%-- Fields Card (Native mode only) --%>
                        <c:if test="${!selectedQuestionnaire.isExternal()}">
                            <div class="card mb-3">
                                <div class="hdr-bar d-flex justify-content-between align-items-center">
                                    <span><i class="bi bi-input-cursor-text me-1"></i>Fields (${fn:length(selectedQuestionnaire.getFieldList())})</span>
                                    <div>
                                        <button type="button" class="btn btn-sm btn-outline-light" title="Show/hide suppressed fields" onclick="toggleFieldSuppressed()">
                                            <i class="bi bi-eye-slash" id="toggleFieldSuppIcon"></i>
                                        </button>
                                        <button class="btn btn-sm btn-outline-light" data-bs-toggle="modal" data-bs-target="#addFieldModal" title="Add field">
                                            <i class="bi bi-plus-lg"></i>
                                        </button>
                                    </div>
                                </div>
                                <div class="card-body py-2 px-3">
                                    <c:choose>
                                        <c:when test="${not empty selectedQuestionnaire.getFieldList()}">
                                            <div class="field-table-wrap" id="fieldTableWrap">
                                                <table class="table table-sm table-hover mb-0" style="font-size:0.85rem;">
                                                    <thead>
                                                    <tr>
                                                        <th style="width:20%">Section</th>
                                                        <th style="width:25%">Label</th>
                                                        <th style="width:10%">Type</th>
                                                        <th style="width:15%">Key</th>
                                                        <th style="width:5%">Req</th>
                                                        <th style="width:15%">Options</th>
                                                        <th style="width:10%" class="text-end">Actions</th>
                                                    </tr>
                                                    </thead>
                                                    <tbody>
                                                    <c:set var="lastSection" value=""/>
                                                    <c:forEach var="field" items="${selectedQuestionnaire.getFieldList()}">
                                                        <%-- Section group header --%>
                                                        <c:if test="${field.getSectionName() != null && field.getSectionName() != lastSection}">
                                                            <tr class="section-header-row ${field.isSuppressed() ? 'field-suppressed' : ''}">
                                                                <td colspan="7"><i class="bi bi-folder me-1"></i>${field.getSectionName()}</td>
                                                            </tr>
                                                            <c:set var="lastSection" value="${field.getSectionName()}"/>
                                                        </c:if>
                                                        <tr class="${field.isSuppressed() ? 'field-suppressed' : ''}">
                                                            <td>
                                                                <c:if test="${not empty field.getSectionName()}">
                                                                    <small class="text-muted">${field.getSectionName()}</small>
                                                                </c:if>
                                                            </td>
                                                            <td class="fw-semibold">${field.getLabel()}</td>
                                                            <td><span class="badge bg-light text-dark">${field.getFieldType()}</span></td>
                                                            <td><code style="font-size:0.75rem;">${field.getFieldKey()}</code></td>
                                                            <td>
                                                                <c:if test="${field.isRequired()}"><i class="bi bi-check-circle-fill text-success"></i></c:if>
                                                                <c:if test="${!field.isRequired()}"><i class="bi bi-circle text-muted" style="font-size:0.7rem;"></i></c:if>
                                                            </td>
                                                            <td>
                                                                <c:if test="${not empty field.getSelectOptions()}">
                                                                    <small class="text-muted">${fn:substring(field.getSelectOptions(), 0, 25)}${fn:length(field.getSelectOptions()) > 25 ? '...' : ''}</small>
                                                                </c:if>
                                                            </td>
                                                            <td class="text-end text-nowrap">
                                                                <a href="#" class="text-primary me-1" style="font-size:0.8rem;" title="Edit"
                                                                   onclick="openEditField(${field.getId()}, '${fn:escapeXml(field.getFieldKey())}', '${fn:escapeXml(field.getLabel())}', '${field.getFieldType()}', '${fn:escapeXml(field.getSelectOptions())}', '${fn:escapeXml(field.getHelpText())}', '${fn:escapeXml(field.getSectionName())}', ${field.isRequired()}); return false;">
                                                                    <i class="bi bi-pencil"></i>
                                                                </a>
                                                                <form method="post" action="QuestionnaireAction25" class="d-inline">
                                                                    <input type="hidden" name="action" value="suppressField"/>
                                                                    <input type="hidden" name="fieldId" value="${field.getId()}"/>
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
                                        <c:otherwise><span class="text-muted" style="font-size:0.85rem;">No fields defined yet — click + to add</span></c:otherwise>
                                    </c:choose>
                                </div>
                            </div>
                        </c:if>

                    </div>
                </c:when>

                <%-- Empty State --%>
                <c:otherwise>
                    <div class="card">
                        <div class="card-body text-center py-5" style="color:#999;">
                            <i class="bi bi-arrow-left-circle" style="font-size:3rem;"></i>
                            <p class="mb-0 fs-5 mt-3">Select a questionnaire to manage</p>
                        </div>
                    </div>
                </c:otherwise>
            </c:choose>

        </div>
    </div>
</div>

<%-- ======================== MODALS ======================== --%>

<%-- Add Questionnaire --%>
<div class="modal fade" id="addQuestionnaireModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="QuestionnaireAction25">
                <input type="hidden" name="action" value="createQuestionnaire"/>
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                    <h6 class="modal-title fw-semibold"><i class="bi bi-plus-circle me-2"></i>New Questionnaire</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Name</label>
                        <input type="text" name="name" class="form-control" required maxlength="100">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Description</label>
                        <textarea name="description" class="form-control" rows="2" maxlength="500"></textarea>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Activity Type</label>
                        <select name="activityType" class="form-select">
                            <option value="ALL">All</option>
                            <option value="SETUP">Setup</option>
                            <option value="RENEWAL">Renewal</option>
                            <option value="TICKET">Ticket</option>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Mode</label>
                        <div class="form-check">
                            <input class="form-check-input" type="radio" name="mode" id="addModeNative" value="native" checked
                                   onchange="document.getElementById('addUrlGroup').style.display='none'">
                            <label class="form-check-label" for="addModeNative">Native Form (AMS fields)</label>
                        </div>
                        <div class="form-check">
                            <input class="form-check-input" type="radio" name="mode" id="addModeExternal" value="external"
                                   onchange="document.getElementById('addUrlGroup').style.display='block'">
                            <label class="form-check-label" for="addModeExternal">External URL (Jotform, etc.)</label>
                        </div>
                    </div>
                    <div class="mb-3" id="addUrlGroup" style="display:none;">
                        <label class="form-label fw-semibold">External URL</label>
                        <input type="text" name="externalUrl" class="form-control" maxlength="500" placeholder="https://form.jotform.com/...">
                        <small class="text-muted">Tokens: <code>{erName}</code> <code>{activityId}</code> <code>{instanceGuid}</code></small>
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

<%-- Edit Questionnaire --%>
<c:if test="${not empty selectedQuestionnaire}">
<div class="modal fade" id="editQuestionnaireModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="QuestionnaireAction25">
                <input type="hidden" name="action" value="editQuestionnaire"/>
                <input type="hidden" name="qId" value="${selectedQuestionnaire.getId()}"/>
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                    <h6 class="modal-title fw-semibold"><i class="bi bi-pencil me-2"></i>Edit Questionnaire</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Name</label>
                        <input type="text" name="name" class="form-control" required maxlength="100" value="${fn:escapeXml(selectedQuestionnaire.getName())}">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Description</label>
                        <textarea name="description" class="form-control" rows="2" maxlength="500">${fn:escapeXml(selectedQuestionnaire.getDescription())}</textarea>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Activity Type</label>
                        <select name="activityType" class="form-select">
                            <option value="ALL" ${selectedQuestionnaire.getActivityType() == 'ALL' ? 'selected' : ''}>All</option>
                            <option value="SETUP" ${selectedQuestionnaire.getActivityType() == 'SETUP' ? 'selected' : ''}>Setup</option>
                            <option value="RENEWAL" ${selectedQuestionnaire.getActivityType() == 'RENEWAL' ? 'selected' : ''}>Renewal</option>
                            <option value="TICKET" ${selectedQuestionnaire.getActivityType() == 'TICKET' ? 'selected' : ''}>Ticket</option>
                        </select>
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">External URL <small class="text-muted">(leave blank for native mode)</small></label>
                        <input type="text" name="externalUrl" class="form-control" maxlength="500"
                               value="${fn:escapeXml(selectedQuestionnaire.getExternalUrl())}"
                               placeholder="https://form.jotform.com/...">
                        <small class="text-muted">Tokens: <code>{erName}</code> <code>{activityId}</code> <code>{instanceGuid}</code></small>
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

<%-- Add Field --%>
<c:if test="${!selectedQuestionnaire.isExternal()}">
<div class="modal fade" id="addFieldModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="QuestionnaireAction25">
                <input type="hidden" name="action" value="createField"/>
                <input type="hidden" name="qId" value="${selectedQuestionnaire.getId()}"/>
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                    <h6 class="modal-title fw-semibold"><i class="bi bi-plus-circle me-2"></i>New Field</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label fw-semibold">Field Key</label>
                            <input type="text" name="fieldKey" class="form-control" required maxlength="100" placeholder="e.g. employer_name">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label fw-semibold">Label</label>
                            <input type="text" name="label" class="form-control" maxlength="200" placeholder="Display label">
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label fw-semibold">Field Type</label>
                            <select name="fieldType" class="form-select" onchange="toggleAddOpts(this.value)">
                                <option value="TEXT">Text</option>
                                <option value="TEXTAREA">Textarea</option>
                                <option value="NUMBER">Number</option>
                                <option value="DATE">Date</option>
                                <option value="SELECT">Select (Dropdown)</option>
                                <option value="RADIO">Radio Buttons</option>
                                <option value="CHECKBOX">Checkbox (Multi)</option>
                                <option value="BOOLEAN">Boolean (Yes/No)</option>
                            </select>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label fw-semibold">Section Name</label>
                            <input type="text" name="sectionName" class="form-control" maxlength="100" placeholder="Group label (optional)">
                        </div>
                    </div>
                    <div class="mb-3" id="addOptsGroup" style="display:none;">
                        <label class="form-label fw-semibold">Options <small class="text-muted">(pipe-delimited)</small></label>
                        <input type="text" name="selectOptions" class="form-control" maxlength="500" placeholder="Option1|Option2|Option3">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Help Text</label>
                        <input type="text" name="helpText" class="form-control" maxlength="500">
                    </div>
                    <div class="form-check mb-3">
                        <input class="form-check-input" type="checkbox" name="isRequired" id="addFieldReq">
                        <label class="form-check-label fw-semibold" for="addFieldReq">Required</label>
                    </div>
                </div>
                <div class="modal-footer justify-content-center border-0">
                    <button type="submit" class="ssa-action save"><i class="bi bi-plus-circle me-1"></i>Add Field</button>
                    <span class="ssa-action-sep">|</span>
                    <button type="button" class="ssa-action cancel" data-bs-dismiss="modal"><i class="bi bi-x-lg me-1"></i>Cancel</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%-- Edit Field --%>
<div class="modal fade" id="editFieldModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="QuestionnaireAction25">
                <input type="hidden" name="action" value="editField"/>
                <input type="hidden" name="qId" value="${selectedQuestionnaire.getId()}"/>
                <input type="hidden" name="fieldId" id="editFieldId"/>
                <div class="modal-header py-2" style="background-color: var(--ssa); color: white;">
                    <h6 class="modal-title fw-semibold"><i class="bi bi-pencil me-2"></i>Edit Field</h6>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label fw-semibold">Field Key</label>
                            <input type="text" name="fieldKey" id="editFieldKey" class="form-control" required maxlength="100">
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label fw-semibold">Label</label>
                            <input type="text" name="label" id="editFieldLabel" class="form-control" maxlength="200">
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label fw-semibold">Field Type</label>
                            <select name="fieldType" id="editFieldType" class="form-select" onchange="toggleEditOpts(this.value)">
                                <option value="TEXT">Text</option>
                                <option value="TEXTAREA">Textarea</option>
                                <option value="NUMBER">Number</option>
                                <option value="DATE">Date</option>
                                <option value="SELECT">Select (Dropdown)</option>
                                <option value="RADIO">Radio Buttons</option>
                                <option value="CHECKBOX">Checkbox (Multi)</option>
                                <option value="BOOLEAN">Boolean (Yes/No)</option>
                            </select>
                        </div>
                        <div class="col-md-6 mb-3">
                            <label class="form-label fw-semibold">Section Name</label>
                            <input type="text" name="sectionName" id="editFieldSection" class="form-control" maxlength="100">
                        </div>
                    </div>
                    <div class="mb-3" id="editOptsGroup" style="display:none;">
                        <label class="form-label fw-semibold">Options <small class="text-muted">(pipe-delimited)</small></label>
                        <input type="text" name="selectOptions" id="editFieldOptions" class="form-control" maxlength="500">
                    </div>
                    <div class="mb-3">
                        <label class="form-label fw-semibold">Help Text</label>
                        <input type="text" name="helpText" id="editFieldHelp" class="form-control" maxlength="500">
                    </div>
                    <div class="form-check mb-3">
                        <input class="form-check-input" type="checkbox" name="isRequired" id="editFieldReq">
                        <label class="form-check-label fw-semibold" for="editFieldReq">Required</label>
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
</c:if>

<script>
    /* ── Filter Tabs ── */
    function filterQ(type, btn) {
        document.querySelectorAll('.filter-tabs .btn').forEach(b => b.classList.remove('active-filter'));
        btn.classList.add('active-filter');
        document.querySelectorAll('.q-item').forEach(el => {
            const mode = el.dataset.mode;
            el.style.display = (type === 'all' || mode === type) ? '' : 'none';
        });
    }

    /* ── Search ── */
    function searchQ(term) {
        const lower = term.toLowerCase();
        document.querySelectorAll('.q-item').forEach(el => {
            const name = el.dataset.name || '';
            el.style.display = name.includes(lower) ? '' : 'none';
        });
    }

    /* ── Suppressed Toggle ── */
    function toggleSuppressed() {
        const scroll = document.getElementById('qListScroll');
        const icon = document.getElementById('suppressIcon');
        scroll.classList.toggle('show-suppressed');
        icon.classList.toggle('bi-eye-slash');
        icon.classList.toggle('bi-eye');
    }

    /* ── Field Suppressed Toggle ── */
    function toggleFieldSuppressed() {
        const wrap = document.getElementById('fieldTableWrap');
        const icon = document.getElementById('toggleFieldSuppIcon');
        if (wrap) {
            wrap.classList.toggle('show-field-suppressed');
            icon.classList.toggle('bi-eye-slash');
            icon.classList.toggle('bi-eye');
        }
    }

    /* ── Add Field: show options input for SELECT/RADIO/CHECKBOX ── */
    function toggleAddOpts(type) {
        document.getElementById('addOptsGroup').style.display =
            ['SELECT','RADIO','CHECKBOX'].includes(type) ? 'block' : 'none';
    }

    /* ── Edit Field: show options input for SELECT/RADIO/CHECKBOX ── */
    function toggleEditOpts(type) {
        document.getElementById('editOptsGroup').style.display =
            ['SELECT','RADIO','CHECKBOX'].includes(type) ? 'block' : 'none';
    }

    /* ── Open Edit Field Modal ── */
    function openEditField(id, key, label, type, options, helpText, sectionName, required) {
        document.getElementById('editFieldId').value = id;
        document.getElementById('editFieldKey').value = key;
        document.getElementById('editFieldLabel').value = label;
        document.getElementById('editFieldType').value = type;
        document.getElementById('editFieldOptions').value = options || '';
        document.getElementById('editFieldHelp').value = helpText || '';
        document.getElementById('editFieldSection').value = sectionName || '';
        document.getElementById('editFieldReq').checked = required;
        toggleEditOpts(type);
        new bootstrap.Modal(document.getElementById('editFieldModal')).show();
    }
</script>
</body>
</html>
