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
        .row-btn { font-size: 0.72rem; padding: 1px 7px; border-radius: 4px; border: 1px solid #ccc;
                   background: white; cursor: pointer; white-space: nowrap; }
        .row-btn:hover { background: #e9ecef; }
        .row-btn.confirm { border-color: #198754; color: #0f5132; }
        .row-btn.confirm:hover { background: #d1e7dd; }
        .row-btn.link { border-color: #0d6efd; color: #084298; }
        .row-btn.link:hover { background: #cfe2ff; }
        .row-btn.skip-btn { border-color: #6c757d; color: #6c757d; }
        .row-btn.skip-btn:hover { background: #e2e3e5; }
        .search-results { max-height: 300px; overflow-y: auto; }
        .search-result-item { padding: 8px 12px; cursor: pointer; border-bottom: 1px solid #eee; font-size: 0.85rem; }
        .search-result-item:hover { background: #e8f0fe; }
        .search-result-item .detail { font-size: 0.78rem; color: #6c757d; }
        .candidate-list { font-size: 0.78rem; margin-top: 4px; }
        .candidate-item { padding: 2px 0; cursor: pointer; color: #0d6efd; }
        .candidate-item:hover { text-decoration: underline; }
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
                <c:if test="${not empty entityState.fileName}">
                    <span class="ms-3"><i class="bi bi-file-earmark me-1"></i>${entityState.fileName}</span>
                </c:if>
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

        <%-- Resolution Table (shown after file uploaded) --%>
        <c:if test="${not empty entityState.filePath}">
            <%-- Summary bar --%>
            <div class="card mb-3">
                <div class="card-body py-2">
                    <div class="summary-bar">
                        <div class="stat"><strong>${entityState.totalRows}</strong>&nbsp;total rows</div>
                        <div class="stat"><span class="dot dot-matched"></span> <strong id="cnt-matched">${entityState.matchedCount}</strong> matched</div>
                        <div class="stat"><span class="dot dot-suggested"></span> <strong id="cnt-suggested">${entityState.suggestedCount}</strong> suggested</div>
                        <div class="stat"><span class="dot dot-unmatched"></span> <strong id="cnt-unmatched">${entityState.unmatchedCount}</strong> unmatched</div>
                        <div class="stat" id="cnt-error-wrap" style="${entityState.errorCount == 0 ? 'display:none' : ''}">
                            <span class="dot dot-error"></span> <strong id="cnt-error">${entityState.errorCount}</strong> errors
                        </div>
                    </div>
                </div>
            </div>

            <%-- Resolution table --%>
            <div id="resolutionTable">
                <table class="table table-hover res-table">
                    <thead>
                        <tr>
                            <th style="width: 5%;">#</th>
                            <th style="width: 10%;">Ext ID</th>
                            <th style="width: 25%;">File Record</th>
                            <th style="width: 10%;">Status</th>
                            <th style="width: 25%;">AMS Match</th>
                            <th style="width: 25%;">Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        <c:forEach var="row" items="${entityState.rows}" begin="0" end="49">
                            <tr id="row-${row.rowIndex}" data-row-index="${row.rowIndex}" data-status="${row.status}">
                                <td>${row.rowIndex + 1}</td>
                                <td><code>${fn:escapeXml(row.externalId)}</code></td>
                                <td>${fn:escapeXml(row.displayLabel)}</td>
                                <td class="td-status">
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
                                <td class="td-ams">
                                    <c:choose>
                                        <c:when test="${not empty row.amsDisplayLabel}">
                                            <span class="ams-label">${fn:escapeXml(row.amsDisplayLabel)}</span>
                                            <span class="text-muted">(ID: ${row.amsInternalId})</span>
                                        </c:when>
                                        <c:when test="${row.status == 'ERROR'}"><span class="text-danger" style="font-size:0.82rem;">${fn:escapeXml(row.errorMessage)}</span></c:when>
                                        <c:otherwise><span class="text-muted">&mdash;</span></c:otherwise>
                                    </c:choose>
                                    <%-- Candidate list for SUGGESTED rows --%>
                                    <c:if test="${row.status == 'SUGGESTED' && not empty row.candidates && fn:length(row.candidates) > 1}">
                                        <div class="candidate-list">
                                            <span class="text-muted">Other:</span>
                                            <c:forEach var="cand" items="${row.candidates}" begin="1" end="3">
                                                <span class="candidate-item" onclick="ii_manualLink(${row.rowIndex}, ${cand.internalId}, '${fn:escapeXml(cand.displayLabel)}')"
                                                      title="${fn:escapeXml(cand.detail)}">${fn:escapeXml(cand.displayLabel)}</span>
                                            </c:forEach>
                                        </div>
                                    </c:if>
                                </td>
                                <td class="td-actions">
                                    <c:choose>
                                        <c:when test="${row.status == 'SUGGESTED'}">
                                            <button class="row-btn confirm" onclick="ii_resolve(${row.rowIndex},'confirm')" title="Accept this match">
                                                <i class="bi bi-check"></i> Confirm
                                            </button>
                                            <button class="row-btn" onclick="ii_resolve(${row.rowIndex},'new')" title="Create as new record">
                                                <i class="bi bi-plus"></i> New
                                            </button>
                                            <button class="row-btn link" onclick="ii_openSearch(${row.rowIndex})" title="Search for a different match">
                                                <i class="bi bi-search"></i>
                                            </button>
                                            <button class="row-btn skip-btn" onclick="ii_resolve(${row.rowIndex},'skip')" title="Skip this row">
                                                <i class="bi bi-x"></i>
                                            </button>
                                        </c:when>
                                        <c:when test="${row.status == 'UNMATCHED'}">
                                            <button class="row-btn link" onclick="ii_openSearch(${row.rowIndex})" title="Search and link to existing record">
                                                <i class="bi bi-search"></i> Link
                                            </button>
                                            <button class="row-btn skip-btn" onclick="ii_resolve(${row.rowIndex},'skip')" title="Skip this row">
                                                <i class="bi bi-x"></i> Skip
                                            </button>
                                        </c:when>
                                        <c:when test="${row.status == 'CONFIRMED' || row.status == 'MANUAL'}">
                                            <button class="row-btn" onclick="ii_resolve(${row.rowIndex},'new')" title="Undo — create as new instead">
                                                <i class="bi bi-arrow-counterclockwise"></i> Undo
                                            </button>
                                        </c:when>
                                        <c:when test="${row.status == 'SKIPPED'}">
                                            <button class="row-btn" onclick="ii_resolve(${row.rowIndex},'new')" title="Restore this row">
                                                <i class="bi bi-arrow-counterclockwise"></i> Restore
                                            </button>
                                        </c:when>
                                        <c:when test="${row.status == 'MATCHED'}">
                                            <span class="text-muted" style="font-size:0.78rem;"><i class="bi bi-lock me-1"></i>Auto-matched</span>
                                        </c:when>
                                        <c:when test="${row.status == 'ERROR'}">
                                            <button class="row-btn link" onclick="ii_openSearch(${row.rowIndex})" title="Search and link manually">
                                                <i class="bi bi-search"></i> Link
                                            </button>
                                            <button class="row-btn skip-btn" onclick="ii_resolve(${row.rowIndex},'skip')" title="Skip this row">
                                                <i class="bi bi-x"></i> Skip
                                            </button>
                                        </c:when>
                                    </c:choose>
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
                <c:if test="${entityState.totalRows > 50}">
                    <p class="text-muted text-center" style="font-size: 0.82rem;">
                        Showing first 50 of ${entityState.totalRows} rows. Remaining rows will be imported with current resolution.
                    </p>
                </c:if>
            </div>

            <%-- Action bar --%>
            <div class="mt-3 d-flex gap-2 align-items-center flex-wrap">
                <form method="POST" action="InteractiveImport" class="d-inline" id="commitForm"
                      onsubmit="return ii_confirmCommit();">
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
                <form method="POST" action="InteractiveImport" class="d-inline">
                    <input type="hidden" name="action" value="resetUpload">
                    <input type="hidden" name="entityType" value="${entityType}">
                    <button type="submit" class="ssa-action cancel" style="font-size: 0.82rem;"
                            title="Clear this file and upload a different one">
                        <i class="bi bi-arrow-repeat me-1"></i>Re-upload
                    </button>
                </form>
            </div>
        </c:if>

    </div>
</div>

<%-- Search Modal --%>
<div class="modal fade" id="searchModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header py-2" style="background: var(--ssa); color: white;">
                <h6 class="modal-title"><i class="bi bi-search me-1"></i>Search AMS Records</h6>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body">
                <input type="hidden" id="search-row-index" value="">
                <div class="mb-3">
                    <input type="text" id="search-query" class="form-control" placeholder="Type to search..."
                           autocomplete="off">
                </div>
                <div id="search-results" class="search-results">
                    <p class="text-muted text-center py-3" style="font-size:0.85rem;">Type at least 2 characters to search</p>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    // ═══════════════════════════════════════════════════════════════
    //  Interactive Import — Resolution Actions (B3)
    // ═══════════════════════════════════════════════════════════════

    let _searchModal;
    let _searchTimer = null;

    document.addEventListener('DOMContentLoaded', function() {
        _searchModal = new bootstrap.Modal(document.getElementById('searchModal'));

        // Live search with debounce
        document.getElementById('search-query').addEventListener('input', function() {
            clearTimeout(_searchTimer);
            const q = this.value.trim();
            if (q.length < 2) {
                document.getElementById('search-results').innerHTML =
                    '<p class="text-muted text-center py-3" style="font-size:0.85rem;">Type at least 2 characters to search</p>';
                return;
            }
            _searchTimer = setTimeout(() => ii_doSearch(q), 300);
        });
    });

    /** Resolve a row: confirm, new, skip */
    function ii_resolve(rowIndex, resolution) {
        const params = new URLSearchParams();
        params.set('action', 'resolveRow');
        params.set('rowIndex', rowIndex);
        params.set('resolution', resolution);

        fetch('InteractiveImport', { method: 'POST', body: params })
            .then(r => r.json())
            .then(data => {
                if (data.error) { console.error(data.error); return; }
                ii_updateRow(data);
                ii_updateCounts(data);
            })
            .catch(e => console.error('Resolve error:', e));
    }

    /** Manual link to a specific AMS record */
    function ii_manualLink(rowIndex, internalId, label) {
        const params = new URLSearchParams();
        params.set('action', 'resolveRow');
        params.set('rowIndex', rowIndex);
        params.set('resolution', 'manual');
        params.set('internalId', internalId);
        params.set('label', label);

        fetch('InteractiveImport', { method: 'POST', body: params })
            .then(r => r.json())
            .then(data => {
                if (data.error) { console.error(data.error); return; }
                ii_updateRow(data);
                ii_updateCounts(data);
                _searchModal.hide();
            })
            .catch(e => console.error('Manual link error:', e));
    }

    /** Open search modal for a row */
    function ii_openSearch(rowIndex) {
        document.getElementById('search-row-index').value = rowIndex;
        document.getElementById('search-query').value = '';
        document.getElementById('search-results').innerHTML =
            '<p class="text-muted text-center py-3" style="font-size:0.85rem;">Type at least 2 characters to search</p>';
        _searchModal.show();
        setTimeout(() => document.getElementById('search-query').focus(), 300);
    }

    /** Execute AMS search */
    function ii_doSearch(query) {
        const resultsDiv = document.getElementById('search-results');
        resultsDiv.innerHTML = '<p class="text-muted text-center py-2"><i class="bi bi-hourglass-split me-1"></i>Searching...</p>';

        fetch('InteractiveImport?step=ajax&action=searchAms&query=' + encodeURIComponent(query))
            .then(r => r.json())
            .then(items => {
                if (!items.length) {
                    resultsDiv.innerHTML = '<p class="text-muted text-center py-3" style="font-size:0.85rem;">No matches found</p>';
                    return;
                }
                const rowIndex = document.getElementById('search-row-index').value;
                let html = '';
                items.forEach(item => {
                    html += '<div class="search-result-item" onclick="ii_manualLink(' + rowIndex + ',' + item.id + ',\'' + ii_esc(item.label) + '\')">';
                    html += '<div><strong>' + ii_esc(item.label) + '</strong> <span class="text-muted">(ID: ' + item.id + ')</span></div>';
                    html += '<div class="detail">' + ii_esc(item.detail) + '</div>';
                    html += '</div>';
                });
                resultsDiv.innerHTML = html;
            })
            .catch(e => {
                resultsDiv.innerHTML = '<p class="text-danger text-center py-3">Search failed</p>';
                console.error('Search error:', e);
            });
    }

    /** Update a table row after resolution */
    function ii_updateRow(data) {
        const tr = document.getElementById('row-' + data.rowIndex);
        if (!tr) return;
        tr.dataset.status = data.status;

        // Update status cell
        const statusBadge = ii_statusBadge(data.status);
        tr.querySelector('.td-status').innerHTML = statusBadge;

        // Update AMS match cell
        let amsHtml;
        if (data.amsDisplayLabel && data.amsInternalId) {
            amsHtml = '<span class="ams-label">' + ii_esc(data.amsDisplayLabel) + '</span> '
                    + '<span class="text-muted">(ID: ' + data.amsInternalId + ')</span>';
        } else {
            amsHtml = '<span class="text-muted">&mdash;</span>';
        }
        tr.querySelector('.td-ams').innerHTML = amsHtml;

        // Update actions cell
        tr.querySelector('.td-actions').innerHTML = ii_actionButtons(data.rowIndex, data.status);
    }

    /** Update summary counts */
    function ii_updateCounts(data) {
        document.getElementById('cnt-matched').textContent = data.matchedCount;
        document.getElementById('cnt-suggested').textContent = data.suggestedCount;
        document.getElementById('cnt-unmatched').textContent = data.unmatchedCount;
        document.getElementById('cnt-error').textContent = data.errorCount;
        document.getElementById('cnt-error-wrap').style.display = data.errorCount > 0 ? '' : 'none';
    }

    /** Generate status badge HTML */
    function ii_statusBadge(status) {
        const map = {
            'MATCHED':   '<span class="status-badge st-matched">Matched</span>',
            'SUGGESTED': '<span class="status-badge st-suggested">Suggested</span>',
            'CONFIRMED': '<span class="status-badge st-confirmed">Confirmed</span>',
            'MANUAL':    '<span class="status-badge st-manual">Manual</span>',
            'UNMATCHED': '<span class="status-badge st-unmatched">New</span>',
            'ERROR':     '<span class="status-badge st-error">Error</span>',
            'SKIPPED':   '<span class="status-badge st-skipped">Skipped</span>'
        };
        return map[status] || status;
    }

    /** Generate action buttons for a given status */
    function ii_actionButtons(rowIndex, status) {
        switch (status) {
            case 'SUGGESTED':
                return '<button class="row-btn confirm" onclick="ii_resolve(' + rowIndex + ',\'confirm\')"><i class="bi bi-check"></i> Confirm</button> '
                     + '<button class="row-btn" onclick="ii_resolve(' + rowIndex + ',\'new\')"><i class="bi bi-plus"></i> New</button> '
                     + '<button class="row-btn link" onclick="ii_openSearch(' + rowIndex + ')"><i class="bi bi-search"></i></button> '
                     + '<button class="row-btn skip-btn" onclick="ii_resolve(' + rowIndex + ',\'skip\')"><i class="bi bi-x"></i></button>';
            case 'UNMATCHED':
                return '<button class="row-btn link" onclick="ii_openSearch(' + rowIndex + ')"><i class="bi bi-search"></i> Link</button> '
                     + '<button class="row-btn skip-btn" onclick="ii_resolve(' + rowIndex + ',\'skip\')"><i class="bi bi-x"></i> Skip</button>';
            case 'CONFIRMED':
            case 'MANUAL':
                return '<button class="row-btn" onclick="ii_resolve(' + rowIndex + ',\'new\')"><i class="bi bi-arrow-counterclockwise"></i> Undo</button>';
            case 'SKIPPED':
                return '<button class="row-btn" onclick="ii_resolve(' + rowIndex + ',\'new\')"><i class="bi bi-arrow-counterclockwise"></i> Restore</button>';
            case 'MATCHED':
                return '<span class="text-muted" style="font-size:0.78rem;"><i class="bi bi-lock me-1"></i>Auto-matched</span>';
            case 'ERROR':
                return '<button class="row-btn link" onclick="ii_openSearch(' + rowIndex + ')"><i class="bi bi-search"></i> Link</button> '
                     + '<button class="row-btn skip-btn" onclick="ii_resolve(' + rowIndex + ',\'skip\')"><i class="bi bi-x"></i> Skip</button>';
            default:
                return '';
        }
    }

    /** Confirm before commit — warn about unresolved suggestions */
    function ii_confirmCommit() {
        const matched = parseInt(document.getElementById('cnt-matched').textContent) || 0;
        const suggested = parseInt(document.getElementById('cnt-suggested').textContent) || 0;
        const unmatched = parseInt(document.getElementById('cnt-unmatched').textContent) || 0;
        const errors = parseInt(document.getElementById('cnt-error').textContent) || 0;

        let msg = 'Commit ' + (matched + unmatched) + ' rows?\n\n';
        msg += '  \u2713 ' + matched + ' matched (update existing)\n';
        msg += '  + ' + unmatched + ' new (create records)\n';

        if (suggested > 0) {
            msg += '\n\u26a0 ' + suggested + ' suggested rows are still unresolved!\n';
            msg += 'They will be treated as NEW records.\n';
        }
        if (errors > 0) {
            msg += '\n\u2716 ' + errors + ' error rows will be skipped.\n';
        }

        return confirm(msg);
    }

    /** Escape for safe HTML insertion */
    function ii_esc(s) {
        if (!s) return '';
        const d = document.createElement('div');
        d.textContent = s;
        return d.innerHTML.replace(/'/g, '&#39;');
    }
</script>
</body>
</html>
