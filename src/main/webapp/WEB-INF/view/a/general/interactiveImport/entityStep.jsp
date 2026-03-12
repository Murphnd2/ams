<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Interactive Import — ${entityState.entityLabel}</title>
    <style>
        .ii-page { height: calc(100vh - 64px); display: flex; flex-direction: column; overflow: hidden; }
        .ii-toolbar { flex-shrink: 0; padding: 12px 20px; background: #f8f9fa; border-bottom: 1px solid #dee2e6; }
        .ii-body { flex: 1; overflow-y: auto; padding: 20px; }
        .step-badges { display: flex; gap: 6px; font-size: 0.78rem; }
        .step-badges .badge { padding: 4px 10px; }
        .step-active { background: var(--ssa) !important; }
        .step-done { background: #87a948 !important; color: white !important; }
        .summary-bar { display: flex; gap: 16px; flex-wrap: wrap; font-size: 0.88rem; }
        .summary-bar .stat { display: flex; align-items: center; gap: 5px; }
        .dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; }
        .dot-matched  { background: #198754; }
        .dot-suggested { background: #ffc107; }
        .dot-unmatched { background: #6c757d; }
        .dot-error { background: #dc3545; }
        .res-table th { font-size: 0.82rem; position: sticky; top: 0; background: #f8f9fa; z-index: 1; }
        .res-table td { font-size: 0.85rem; vertical-align: middle; }
        .status-badge { font-size: 0.75rem; padding: 2px 8px; border-radius: 10px; font-weight: 600; }
        .st-matched   { background: #d1e7dd; color: #0f5132; }
        .st-suggested { background: #fff3cd; color: #856404; }
        .st-unmatched { background: #e2e3e5; color: #41464b; }
        .st-confirmed { background: #cfe2ff; color: #084298; }
        .st-manual    { background: #cfe2ff; color: #084298; }
        .st-error     { background: #f8d7da; color: #842029; }
        .st-skipped   { background: #e2e3e5; color: #6c757d; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="ii-page">

    <%-- Toolbar --%>
    <div class="ii-toolbar">
        <div class="d-flex align-items-center justify-content-between">
            <div>
                <h5 class="mb-0" style="color: var(--ssa);">
                    <i class="bi bi-diagram-3 me-1"></i>Interactive Import — ${entityState.entityLabel}
                </h5>
                <span class="text-muted" style="font-size: 0.82rem;">${iiSession.providerName} — Step ${iiSession.stepNumber} of ${iiSession.totalSteps}</span>
            </div>
            <div>
                <form method="POST" action="InteractiveImport" class="d-inline">
                    <input type="hidden" name="action" value="reset">
                    <button type="submit" class="ssa-action cancel" style="font-size: 0.82rem;"
                            onclick="return confirm('Discard all progress and start over?');">
                        <i class="bi bi-arrow-repeat me-1"></i>Start Over
                    </button>
                </form>
            </div>
        </div>

        <%-- Step badges --%>
        <div class="step-badges mt-2">
            <c:set var="stepNum" value="1"/>
            <span class="badge ${iiSession.currentEntityStep == 'SELECT_PROVIDER' ? 'step-active' : 'step-done'}">${stepNum}. Provider</span>
            <c:forEach var="et" items="${iiSession.availableEntityTypes}">
                <c:set var="stepNum" value="${stepNum + 1}"/>
                <c:set var="etState" value="${iiSession.entityStates[et]}"/>
                <c:choose>
                    <c:when test="${et == iiSession.currentEntityStep}">
                        <span class="badge step-active">${stepNum}. ${etState.entityLabel}</span>
                    </c:when>
                    <c:when test="${fn:contains(iiSession.completedEntities, et)}">
                        <span class="badge step-done"><i class="bi bi-check me-1"></i>${stepNum}. ${etState.entityLabel}</span>
                    </c:when>
                    <c:otherwise>
                        <span class="badge bg-light text-dark">${stepNum}. ${etState.entityLabel}</span>
                    </c:otherwise>
                </c:choose>
            </c:forEach>
            <c:set var="stepNum" value="${stepNum + 1}"/>
            <span class="badge ${iiSession.currentEntityStep == 'RESULTS' ? 'step-active' : 'bg-light text-dark'}">${stepNum}. Results</span>
        </div>
    </div>

    <%-- Body --%>
    <div class="ii-body">

        <%-- File type info --%>
        <c:if test="${not empty fileType}">
            <div class="mb-3" style="font-size: 0.82rem;">
                <span class="badge bg-info text-dark">${fileType.targetEntity}</span>
                <span class="ms-2">Mode:
                    <c:choose>
                        <c:when test="${fileType.updateMode == 'CREATE_ONLY'}"><strong>Create Only</strong></c:when>
                        <c:when test="${fileType.updateMode == 'UPDATE_ONLY'}"><strong>Update Only</strong></c:when>
                        <c:otherwise><strong>Create &amp; Update</strong></c:otherwise>
                    </c:choose>
                </span>
                <span class="ms-2">Status:
                    <c:choose>
                        <c:when test="${fileType.mappingStatus == 'READY'}">
                            <span style="color: #0f5132; font-weight: 600;"><i class="bi bi-check-circle me-1"></i>READY</span>
                        </c:when>
                        <c:otherwise>
                            <span style="color: #856404; font-weight: 600;"><i class="bi bi-exclamation-circle me-1"></i>PENDING</span>
                        </c:otherwise>
                    </c:choose>
                </span>
            </div>
        </c:if>

        <%-- Upload Card (shown when no file uploaded yet) --%>
        <c:if test="${empty entityState.filePath}">
            <div class="card mb-4" style="max-width: 700px;">
                <div class="card-header" style="background: var(--ssa); color: white; font-size: 0.9rem;">
                    <i class="bi bi-file-earmark-arrow-up me-1"></i>Upload ${entityState.entityLabel} File
                </div>
                <div class="card-body">
                    <form method="POST" action="InteractiveImport" enctype="multipart/form-data">
                        <input type="hidden" name="action" value="uploadEntity">
                        <input type="hidden" name="entityType" value="${entityType}">
                        <div class="row align-items-end g-3">
                            <div class="col-md-8">
                                <label class="form-label fw-semibold">Select file</label>
                                <input type="file" name="entityFile" class="form-control" required
                                       accept=".csv,.tsv,.xlsx,.xls,.txt">
                            </div>
                            <div class="col-md-4">
                                <button type="submit" class="ssa-action save">
                                    <i class="bi bi-upload me-1"></i>Upload &amp; Analyze
                                </button>
                            </div>
                        </div>
                    </form>
                </div>
            </div>

            <div class="mt-3">
                <form method="POST" action="InteractiveImport" class="d-inline">
                    <input type="hidden" name="action" value="skipEntity">
                    <button type="submit" class="ssa-action cancel">
                        <i class="bi bi-skip-forward me-1"></i>Skip ${entityState.entityLabel}
                    </button>
                </form>
            </div>
        </c:if>

        <%-- Resolution Table (shown after file uploaded — populated in B2/B3) --%>
        <c:if test="${not empty entityState.filePath}">
            <%-- Summary bar --%>
            <div class="card mb-3">
                <div class="card-body py-2">
                    <div class="summary-bar">
                        <div class="stat"><strong>${entityState.totalRows}</strong>&nbsp;total rows</div>
                        <div class="stat"><span class="dot dot-matched"></span> <strong>${entityState.matchedCount}</strong> matched</div>
                        <div class="stat"><span class="dot dot-suggested"></span> <strong>${entityState.suggestedCount}</strong> suggested</div>
                        <div class="stat"><span class="dot dot-unmatched"></span> <strong>${entityState.unmatchedCount}</strong> unmatched</div>
                        <c:if test="${entityState.errorCount > 0}">
                            <div class="stat"><span class="dot dot-error"></span> <strong>${entityState.errorCount}</strong> errors</div>
                        </c:if>
                    </div>
                </div>
            </div>

            <%-- Resolution table placeholder (will be AJAX-driven in B3) --%>
            <div id="resolutionTable">
                <table class="table table-hover res-table">
                    <thead>
                        <tr>
                            <th style="width: 5%;">#</th>
                            <th style="width: 10%;">Ext ID</th>
                            <th style="width: 25%;">File Record</th>
                            <th style="width: 12%;">Status</th>
                            <th style="width: 25%;">AMS Match</th>
                            <th style="width: 23%;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${entityState.rows}" begin="0" end="49">
                            <tr>
                                <td>${row.rowIndex + 1}</td>
                                <td><code>${row.externalId}</code></td>
                                <td>${row.displayLabel}</td>
                                <td>
                                    <c:choose>
                                        <c:when test="${row.status == 'MATCHED'}"><span class="status-badge st-matched">Matched</span></c:when>
                                        <c:when test="${row.status == 'SUGGESTED'}"><span class="status-badge st-suggested">Suggested</span></c:when>
                                        <c:when test="${row.status == 'CONFIRMED'}"><span class="status-badge st-confirmed">Confirmed</span></c:when>
                                        <c:when test="${row.status == 'MANUAL'}"><span class="status-badge st-manual">Manual</span></c:when>
                                        <c:when test="${row.status == 'UNMATCHED'}"><span class="status-badge st-unmatched">New</span></c:when>
                                        <c:when test="${row.status == 'ERROR'}"><span class="status-badge st-error">Error</span></c:when>
                                        <c:when test="${row.status == 'SKIPPED'}"><span class="status-badge st-skipped">Skipped</span></c:when>
                                    </c:choose>
                                </td>
                                <td>
                                    <c:choose>
                                        <c:when test="${not empty row.amsDisplayLabel}">${row.amsDisplayLabel} <span class="text-muted">(ID: ${row.amsInternalId})</span></c:when>
                                        <c:when test="${row.status == 'ERROR'}"><span class="text-danger" style="font-size:0.82rem;">${row.errorMessage}</span></c:when>
                                        <c:otherwise><span class="text-muted">—</span></c:otherwise>
                                    </c:choose>
                                </td>
                                <td>
                                    <%-- Action buttons will be wired up in B3 with AJAX --%>
                                    <span class="text-muted" style="font-size: 0.8rem;">
                                        <c:if test="${row.status == 'SUGGESTED'}">
                                            <em>Confirm / Change / New</em>
                                        </c:if>
                                        <c:if test="${row.status == 'UNMATCHED'}">
                                            <em>Link / Keep as New</em>
                                        </c:if>
                                    </span>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                <c:if test="${entityState.totalRows > 50}">
                    <p class="text-muted text-center" style="font-size: 0.82rem;">
                        Showing first 50 of ${entityState.totalRows} rows. Pagination coming in next phase.
                    </p>
                </c:if>
            </div>

            <%-- Commit button --%>
            <div class="mt-3 d-flex gap-2">
                <form method="POST" action="InteractiveImport" class="d-inline">
                    <input type="hidden" name="action" value="commitEntity">
                    <button type="submit" class="ssa-action save">
                        <i class="bi bi-check-circle me-1"></i>Commit &amp; Next
                    </button>
                </form>
                <form method="POST" action="InteractiveImport" class="d-inline">
                    <input type="hidden" name="action" value="skipEntity">
                    <button type="submit" class="ssa-action cancel">
                        <i class="bi bi-skip-forward me-1"></i>Skip
                    </button>
                </form>
            </div>
        </c:if>

    </div>
</div>
</body>
</html>
