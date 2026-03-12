<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Import Transition Manager</title>
    <style>
        .itm-page { height: calc(100vh - 64px); display: flex; flex-direction: column; overflow: hidden; }
        .itm-toolbar { padding: 12px 20px; border-bottom: 1px solid #dee2e6; background: #f8f9fa; flex-shrink: 0; }
        .itm-body { flex: 1; overflow-y: auto; padding: 16px 20px; }
        .itm-section { border: 1px solid #dee2e6; border-radius: 8px; padding: 14px 18px; margin-bottom: 14px; }
        .itm-section h6 { color: var(--ssa); margin-bottom: 10px; font-weight: 600; }
        .itm-table { width: 100%; font-size: 0.84rem; border-collapse: collapse; }
        .itm-table th { position: sticky; top: 0; background: #f8f9fa; z-index: 1;
                         padding: 6px 10px; border-bottom: 2px solid #dee2e6; text-align: left; font-weight: 600; }
        .itm-table td { padding: 5px 10px; border-bottom: 1px solid #eee; vertical-align: middle; }
        .itm-table tr:hover td { background: #f0f6ff; }
        .badge-primary-yes { background: #198754; color: #fff; }
        .badge-primary-no { background: #6c757d; color: #fff; }
        .btn-xs { font-size: 0.76rem; padding: 2px 8px; }
        .flash-bar { padding: 8px 14px; border-radius: 6px; background: #d1e7dd; color: #0f5132;
                     margin-bottom: 12px; font-size: 0.85rem; }
        .search-result-row { display: flex; align-items: center; gap: 10px; padding: 6px 10px;
                             border-bottom: 1px solid #eee; font-size: 0.84rem; }
        .search-result-row:hover { background: #f0f6ff; }
        .search-detail { color: #6c757d; font-size: 0.78rem; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="itm-page">
    <%-- ═══ TOOLBAR ═══ --%>
    <div class="itm-toolbar">
        <div class="d-flex align-items-center gap-3 flex-wrap">
            <h5 class="mb-0" style="color: var(--ssa);">
                <i class="bi bi-arrow-left-right"></i> Import Transition Manager
            </h5>
            <form method="get" action="ImportTransitionManager" class="d-flex align-items-center gap-2 ms-auto">
                <select name="providerId" class="form-select form-select-sm" style="width: 200px;" required>
                    <option value="">Select Provider</option>
                    <c:forEach var="p" items="${providers}">
                        <option value="${p.id}" ${p.id eq selectedProviderId ? 'selected' : ''}>
                            ${p.providerName} (${p.providerCode})
                        </option>
                    </c:forEach>
                </select>
                <select name="entityType" class="form-select form-select-sm" style="width: 160px;" required>
                    <option value="">Entity Type</option>
                    <option value="PLAN_TYPE" ${'PLAN_TYPE' eq selectedEntityType ? 'selected' : ''}>Plan Type</option>
                    <option value="EMPLOYER" ${'EMPLOYER' eq selectedEntityType ? 'selected' : ''}>Employer</option>
                    <option value="EMPLOYEE" ${'EMPLOYEE' eq selectedEntityType ? 'selected' : ''}>Employee</option>
                    <option value="BENEFIT" ${'BENEFIT' eq selectedEntityType ? 'selected' : ''}>Benefit</option>
                </select>
                <button type="submit" class="btn btn-sm btn-outline-primary">
                    <i class="bi bi-search"></i> Browse
                </button>
            </form>
        </div>
    </div>

    <%-- ═══ BODY ═══ --%>
    <div class="itm-body">

        <%-- Flash message --%>
        <c:if test="${not empty flash}">
            <div class="flash-bar"><i class="bi bi-check-circle me-1"></i> ${flash}</div>
        </c:if>

        <%-- ═══ MAPPINGS TABLE ═══ --%>
        <c:if test="${not empty selectedProviderId and not empty selectedEntityType}">
            <div class="itm-section">
                <h6>
                    <i class="bi bi-table"></i> Mappings
                    <span class="badge bg-secondary ms-1">${mappingCount}</span>
                </h6>

                <c:choose>
                    <c:when test="${not empty enrichedMappings}">
                        <div style="max-height: 400px; overflow-y: auto;">
                            <table class="itm-table">
                                <thead>
                                <tr>
                                    <th>#</th>
                                    <th>External ID</th>
                                    <th>Internal ID</th>
                                    <th>Entity Name</th>
                                    <th>Primary</th>
                                    <th>Notes</th>
                                    <th style="width: 120px;">Actions</th>
                                </tr>
                                </thead>
                                <tbody>
                                <c:forEach var="row" items="${enrichedMappings}">
                                    <tr>
                                        <td class="text-muted">${row.mapping.id}</td>
                                        <td><code>${row.mapping.externalId}</code></td>
                                        <td><strong>${row.mapping.internalId}</strong></td>
                                        <td>${row.entityName}</td>
                                        <td>
                                            <c:choose>
                                                <c:when test="${row.mapping.primary}">
                                                    <span class="badge badge-primary-yes">Yes</span>
                                                </c:when>
                                                <c:otherwise>
                                                    <span class="badge badge-primary-no">No</span>
                                                </c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td class="text-muted" style="font-size: 0.78rem;">${row.mapping.notes}</td>
                                        <td>
                                            <form method="post" action="ImportTransitionManager" style="display: inline;">
                                                <input type="hidden" name="providerId" value="${selectedProviderId}">
                                                <input type="hidden" name="entityType" value="${selectedEntityType}">
                                                <input type="hidden" name="mappingId" value="${row.mapping.id}">
                                                <input type="hidden" name="action" value="togglePrimary">
                                                <button type="submit" class="btn btn-outline-secondary btn-xs"
                                                        title="Toggle is_primary">
                                                    <i class="bi bi-toggle-on"></i>
                                                </button>
                                            </form>
                                            <form method="post" action="ImportTransitionManager" style="display: inline;"
                                                  onsubmit="return confirm('Remove this mapping?');">
                                                <input type="hidden" name="providerId" value="${selectedProviderId}">
                                                <input type="hidden" name="entityType" value="${selectedEntityType}">
                                                <input type="hidden" name="mappingId" value="${row.mapping.id}">
                                                <input type="hidden" name="action" value="unlink">
                                                <button type="submit" class="btn btn-outline-danger btn-xs"
                                                        title="Remove mapping">
                                                    <i class="bi bi-x-lg"></i>
                                                </button>
                                            </form>
                                        </td>
                                    </tr>
                                </c:forEach>
                                </tbody>
                            </table>
                        </div>
                    </c:when>
                    <c:otherwise>
                        <p class="text-muted mb-0" style="font-size: 0.85rem;">
                            No mappings found for this provider and entity type.
                        </p>
                    </c:otherwise>
                </c:choose>
            </div>

            <%-- ═══ SEARCH & LINK ═══ --%>
            <div class="itm-section">
                <h6><i class="bi bi-link-45deg"></i> Search & Link AMS Records</h6>
                <p class="text-muted mb-2" style="font-size: 0.82rem;">
                    Search for an existing AMS record by name or ID, then link it to an external ID from this provider.
                </p>

                <form method="get" action="ImportTransitionManager" class="d-flex align-items-center gap-2 mb-3">
                    <input type="hidden" name="providerId" value="${selectedProviderId}">
                    <input type="hidden" name="entityType" value="${selectedEntityType}">
                    <input type="text" name="q" class="form-control form-control-sm" style="width: 300px;"
                           placeholder="Search by name or ID..." value="${searchQuery}">
                    <button type="submit" class="btn btn-sm btn-outline-primary">
                        <i class="bi bi-search"></i> Search
                    </button>
                </form>

                <%-- Search results --%>
                <c:if test="${not empty searchResults}">
                    <div style="max-height: 300px; overflow-y: auto; border: 1px solid #dee2e6; border-radius: 6px;">
                        <c:forEach var="sr" items="${searchResults}">
                            <div class="search-result-row">
                                <div style="flex: 0 0 60px;"><strong>#${sr.id}</strong></div>
                                <div style="flex: 1;">
                                    ${sr.name}
                                    <div class="search-detail">${sr.detail}</div>
                                </div>
                                <div>
                                    <form method="post" action="ImportTransitionManager" class="d-flex align-items-center gap-1">
                                        <input type="hidden" name="providerId" value="${selectedProviderId}">
                                        <input type="hidden" name="entityType" value="${selectedEntityType}">
                                        <input type="hidden" name="action" value="link">
                                        <input type="hidden" name="internalId" value="${sr.id}">
                                        <input type="hidden" name="isPrimary" value="true">
                                        <input type="text" name="externalId" class="form-control form-control-sm"
                                               style="width: 100px;" placeholder="Ext ID" required>
                                        <button type="submit" class="btn btn-sm btn-outline-success" title="Link">
                                            <i class="bi bi-link"></i> Link
                                        </button>
                                    </form>
                                </div>
                            </div>
                        </c:forEach>
                    </div>
                </c:if>
                <c:if test="${not empty searchQuery and empty searchResults}">
                    <p class="text-muted" style="font-size: 0.85rem;">
                        No results for "<strong>${searchQuery}</strong>".
                    </p>
                </c:if>
            </div>

            <%-- ═══ TRANSFER PRIMARY ═══ --%>
            <div class="itm-section">
                <h6><i class="bi bi-arrow-repeat"></i> Transfer Primary</h6>
                <p class="text-muted mb-2" style="font-size: 0.82rem;">
                    Transfer is_primary status for a specific AMS record to a different provider.
                </p>
                <form method="post" action="ImportTransitionManager" class="d-flex align-items-center gap-2">
                    <input type="hidden" name="providerId" value="${selectedProviderId}">
                    <input type="hidden" name="entityType" value="${selectedEntityType}">
                    <input type="hidden" name="action" value="transferPrimary">
                    <label class="form-label mb-0" style="font-size: 0.84rem;">Internal ID:</label>
                    <input type="number" name="internalId" class="form-control form-control-sm"
                           style="width: 100px;" required>
                    <label class="form-label mb-0" style="font-size: 0.84rem;">New Primary Provider:</label>
                    <select name="newProviderId" class="form-select form-select-sm" style="width: 200px;" required>
                        <c:forEach var="p" items="${providers}">
                            <option value="${p.id}">${p.providerName}</option>
                        </c:forEach>
                    </select>
                    <button type="submit" class="btn btn-sm btn-outline-warning"
                            onclick="return confirm('Transfer primary status?');">
                        <i class="bi bi-arrow-repeat"></i> Transfer
                    </button>
                </form>
            </div>

            <%-- ═══ BULK LINK ═══ --%>
            <div class="itm-section">
                <h6><i class="bi bi-cloud-upload"></i> Bulk Link (CSV)</h6>
                <p class="text-muted mb-2" style="font-size: 0.82rem;">
                    Upload a CSV with columns: <code>external_id, internal_id</code>
                    (optional 3rd column: <code>is_primary</code> — true/false).
                </p>
                <form method="post" action="ImportTransitionManager" enctype="multipart/form-data"
                      class="d-flex align-items-center gap-2">
                    <input type="hidden" name="providerId" value="${selectedProviderId}">
                    <input type="hidden" name="entityType" value="${selectedEntityType}">
                    <input type="hidden" name="action" value="bulkLink">
                    <input type="file" name="bulkFile" class="form-control form-control-sm"
                           accept=".csv,.txt,.tsv" style="max-width: 300px;" required>
                    <button type="submit" class="btn btn-sm btn-outline-primary">
                        <i class="bi bi-upload"></i> Upload & Link
                    </button>
                </form>
            </div>
        </c:if>

        <%-- No provider selected prompt --%>
        <c:if test="${empty selectedProviderId or empty selectedEntityType}">
            <div class="text-center py-5 text-muted">
                <i class="bi bi-arrow-left-right" style="font-size: 3rem; opacity: 0.3;"></i>
                <p class="mt-3">Select a provider and entity type above to browse cross-reference mappings.</p>
                <p style="font-size: 0.85rem;">
                    Use this tool to manage the mapping between external system IDs and internal AMS IDs
                    during provider transitions.
                </p>
            </div>
        </c:if>
    </div>
</div>
</body>
</html>
