<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>NDT Census Testing</title>
    <style>
        :root {
            --psp-primary: ${not empty primaryColor ? primaryColor : '#0d5681'};
            --psp-accent: ${not empty accentColor ? accentColor : '#87a948'};
        }
        .audit-wrap {
            display: flex; flex-direction: column;
            height: calc(100vh - 64px);
            overflow: hidden;
        }
        .audit-toolbar {
            display: flex; align-items: center; justify-content: space-between;
            padding: 10px 16px 8px; flex-shrink: 0;
        }
        .audit-scroll {
            flex: 1; overflow-y: auto; padding: 0 16px 16px;
        }
        .ghost-action {
            background: none; border: none; color: var(--psp-primary);
            font-size: 0.82rem; padding: 4px 10px; cursor: pointer;
            border-radius: 4px; text-decoration: none;
            display: inline-flex; align-items: center; gap: 4px;
            line-height: 1.4;
        }
        .ghost-action:hover { background: rgba(13,86,129,0.08); color: var(--psp-primary); }

        /* Header card */
        .ndt-header-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 8px;
            padding: 1rem 1.25rem; margin-bottom: 1rem;
        }
        .ndt-header-card .stat-label {
            font-size: 0.75rem; color: #6c757d; text-transform: uppercase;
            letter-spacing: 0.03em; margin-bottom: 2px;
        }
        .ndt-header-card .stat-value {
            font-size: 0.92rem; font-weight: 500; color: #212529;
        }

        /* Creation card */
        .ndt-create-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 8px;
            padding: 2rem; max-width: 500px; margin: 2rem auto;
        }
        .ndt-create-card h5 { color: var(--psp-primary); margin-bottom: 1.25rem; }

        /* Upload zone */
        .upload-zone-card {
            background: #fff; border: 1px solid #dee2e6; border-radius: 8px;
            padding: 1rem 1.25rem; margin-bottom: 1rem;
        }
        .drop-zone {
            border: 2px dashed #adb5bd; border-radius: 8px;
            padding: 2rem; text-align: center; cursor: pointer;
            transition: border-color 0.2s, background 0.2s;
            color: #6c757d;
        }
        .drop-zone:hover, .drop-zone.drag-over {
            border-color: var(--psp-primary);
            background: rgba(13,86,129,0.04);
        }
        .drop-zone i { font-size: 2rem; display: block; margin-bottom: 0.5rem; color: #adb5bd; }
        .upload-feedback {
            font-size: 0.82rem; margin-top: 0.5rem; min-height: 1.4em;
        }

        /* Documents table */
        .doc-table th {
            font-size: 0.78rem; white-space: nowrap;
            position: sticky; top: 0; background: #f8f9fa; z-index: 1;
        }
        .doc-table td { font-size: 0.84rem; vertical-align: middle; }
        .btn-delete-upload {
            background: none; border: none; color: #dc3545;
            font-size: 0.85rem; cursor: pointer; padding: 2px 6px;
            border-radius: 4px;
        }
        .btn-delete-upload:hover { background: rgba(220,53,69,0.08); }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"></c:import>

<div class="audit-wrap">
    <%-- Toolbar --%>
    <div class="audit-toolbar">
        <div class="d-flex align-items-center gap-2">
            <i class="bi bi-shield-check" style="font-size: 1.1rem; color: var(--psp-primary);"></i>
            <span style="font-size: 0.95rem; font-weight: 600; color: var(--psp-primary);">NDT Census Testing</span>
        </div>
        <div class="d-flex align-items-center gap-1">
            <c:if test="${not empty testRun}">
                <a href="${pageContext.request.contextPath}/ViewActivity25" class="ghost-action">
                    <i class="bi bi-arrow-left"></i> Back to Activity
                </a>
            </c:if>
        </div>
    </div>

    <c:if test="${not empty error}">
        <div class="alert alert-danger mx-3 mb-2">${error}</div>
    </c:if>

    <%-- Scrollable body --%>
    <div class="audit-scroll">

        <c:choose>
            <%-- STATE 1: No test run exists --%>
            <c:when test="${empty testRun}">
                <div class="ndt-create-card">
                    <h5><i class="bi bi-plus-circle me-2"></i>Start NDT Census Test</h5>
                    <form method="POST" action="${pageContext.request.contextPath}/NdtTestRun">
                        <input type="hidden" name="action" value="create">
                        <input type="hidden" name="activityId" value="${activity.id}">

                        <div class="mb-3">
                            <label class="form-label" style="font-size: 0.82rem; color: #6c757d;">Employer</label>
                            <input type="text" class="form-control" value="${fn:escapeXml(employerName)}" readonly
                                   style="background: #f8f9fa; font-size: 0.9rem;">
                        </div>

                        <div class="mb-4">
                            <label class="form-label" style="font-size: 0.82rem; color: #6c757d;">Plan Year End Date</label>
                            <input type="date" name="planYearEnd" class="form-control" required
                                   style="font-size: 0.9rem;">
                        </div>

                        <button type="submit" class="btn w-100"
                                style="background: var(--psp-primary); color: #fff; font-size: 0.9rem; padding: 0.5rem;">
                            <i class="bi bi-shield-check me-1"></i> Start NDT Test
                        </button>
                    </form>
                </div>
            </c:when>

            <%-- STATE 2: Test run exists --%>
            <c:otherwise>
                <%-- Header card --%>
                <div class="ndt-header-card">
                    <div class="row g-3">
                        <div class="col-md-3">
                            <div class="stat-label">Employer</div>
                            <div class="stat-value">${fn:escapeXml(employerName)}</div>
                        </div>
                        <div class="col-md-2">
                            <div class="stat-label">Plan Year End</div>
                            <div class="stat-value">
                                <c:if test="${testRun.planYearEnd != null}"><fmt:formatDate value="${testRun.planYearEnd}" pattern="MM/dd/yyyy"/></c:if>
                                <c:if test="${testRun.planYearEnd == null}">—</c:if>
                            </div>
                        </div>
                        <div class="col-md-2">
                            <div class="stat-label">Status</div>
                            <div class="stat-value">
                                <span class="badge ${testRun.statusBadgeClass}" style="font-size: 0.75rem;">
                                    ${fn:escapeXml(testRun.statusLabel)}
                                </span>
                            </div>
                        </div>
                        <div class="col-md-2">
                            <div class="stat-label">Created</div>
                            <div class="stat-value">
                                <c:if test="${testRun.createdAt != null}"><fmt:formatDate value="${testRun.createdAt}" pattern="MM/dd/yyyy"/></c:if>
                                <c:if test="${testRun.createdAt == null}">—</c:if>
                            </div>
                        </div>
                        <div class="col-md-1">
                            <div class="stat-label">Employees</div>
                            <div class="stat-value">${testRun.employeeCount}</div>
                        </div>
                        <div class="col-md-2">
                            <div class="stat-label">Documents</div>
                            <div class="stat-value">${fn:length(documents)}</div>
                        </div>
                    </div>
                </div>

                <%-- Upload zone card --%>
                <div class="upload-zone-card">
                    <div class="d-flex align-items-center justify-content-between mb-3">
                        <h6 class="mb-0" style="color: var(--psp-primary); font-size: 0.9rem;">
                            <i class="bi bi-cloud-upload me-1"></i> Upload Documents
                        </h6>
                    </div>

                    <div class="row g-3 align-items-end mb-3">
                        <div class="col-md-4">
                            <label class="form-label" style="font-size: 0.78rem; color: #6c757d;">Document Type</label>
                            <select id="docType" class="form-select form-select-sm" style="font-size: 0.85rem;">
                                <option value="Employee Census">Employee Census</option>
                                <option value="Payroll Report">Payroll Report</option>
                                <option value="Ownership Declaration">Ownership Declaration</option>
                                <option value="Insurance Billing">Insurance Billing</option>
                                <option value="Benefits Enrollment">Benefits Enrollment</option>
                                <option value="Other">Other</option>
                            </select>
                        </div>
                    </div>

                    <div class="drop-zone" id="dropZone">
                        <i class="bi bi-cloud-arrow-up"></i>
                        <div style="font-size: 0.9rem;">Drop files here or click to upload</div>
                        <div style="font-size: 0.75rem; color: #adb5bd; margin-top: 4px;">
                            CSV, Excel, or PDF files accepted
                        </div>
                    </div>
                    <input type="file" id="fileInput" style="display: none;" multiple
                           accept=".csv,.xls,.xlsx,.pdf,.txt">
                    <div id="uploadFeedback" class="upload-feedback"></div>
                </div>

                <%-- Uploaded documents table --%>
                <c:if test="${not empty documents}">
                    <div style="background: #fff; border: 1px solid #dee2e6; border-radius: 8px; padding: 1rem 1.25rem;">
                        <h6 class="mb-3" style="color: var(--psp-primary); font-size: 0.9rem;">
                            <i class="bi bi-file-earmark-text me-1"></i> Uploaded Documents
                        </h6>
                        <table class="table table-sm table-hover doc-table mb-0">
                            <thead>
                                <tr>
                                    <th>Type</th>
                                    <th>Filename</th>
                                    <th>Size</th>
                                    <th>Uploaded</th>
                                    <th>Parse Status</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody>
                                <c:forEach var="doc" items="${documents}">
                                    <tr>
                                        <td>
                                            <span class="badge bg-secondary" style="font-size: 0.7rem;">
                                                ${fn:escapeXml(doc.documentType)}
                                            </span>
                                        </td>
                                        <td>${fn:escapeXml(doc.originalFilename)}</td>
                                        <td style="white-space: nowrap;">
                                            <c:choose>
                                                <c:when test="${doc.fileSize ge 1048576}">
                                                    <fmt:formatNumber value="${doc.fileSize / 1048576}" maxFractionDigits="1"/> MB
                                                </c:when>
                                                <c:when test="${doc.fileSize ge 1024}">
                                                    <fmt:formatNumber value="${doc.fileSize / 1024}" maxFractionDigits="0"/> KB
                                                </c:when>
                                                <c:otherwise>${doc.fileSize} B</c:otherwise>
                                            </c:choose>
                                        </td>
                                        <td>
                                            <fmt:formatDate value="${doc.uploadDate}" pattern="MM/dd/yyyy h:mm a"/>
                                        </td>
                                        <td>
                                            <span class="badge ${doc.parseStatusBadgeClass}" style="font-size: 0.7rem;">
                                                ${fn:escapeXml(doc.parseStatusLabel)}
                                            </span>
                                        </td>
                                        <td>
                                            <form method="POST"
                                                  action="${pageContext.request.contextPath}/NdtTestRun"
                                                  style="display: inline; margin: 0;">
                                                <input type="hidden" name="action" value="deleteUpload">
                                                <input type="hidden" name="uploadId" value="${doc.id}">
                                                <input type="hidden" name="testRunId" value="${testRun.id}">
                                                <button type="submit" class="btn-delete-upload"
                                                        title="Delete document"
                                                        onclick="return confirm('Delete this document? This cannot be undone.');">
                                                    <i class="bi bi-trash3"></i>
                                                </button>
                                            </form>
                                        </td>
                                    </tr>
                                </c:forEach>
                            </tbody>
                        </table>
                    </div>
                </c:if>
            </c:otherwise>
        </c:choose>
    </div>
</div>

<c:if test="${not empty testRun}">
<script>
(function() {
    var dropZone = document.getElementById('dropZone');
    var fileInput = document.getElementById('fileInput');
    var feedback = document.getElementById('uploadFeedback');

    // Click to upload
    dropZone.addEventListener('click', function() {
        fileInput.click();
    });

    // Drag events
    dropZone.addEventListener('dragover', function(e) {
        e.preventDefault();
        dropZone.classList.add('drag-over');
    });
    dropZone.addEventListener('dragleave', function(e) {
        e.preventDefault();
        dropZone.classList.remove('drag-over');
    });
    dropZone.addEventListener('drop', function(e) {
        e.preventDefault();
        dropZone.classList.remove('drag-over');
        if (e.dataTransfer.files.length > 0) {
            uploadFiles(e.dataTransfer.files);
        }
    });

    // File input change
    fileInput.addEventListener('change', function() {
        if (fileInput.files.length > 0) {
            uploadFiles(fileInput.files);
        }
    });

    function uploadFiles(files) {
        var docType = document.getElementById('docType').value;
        var formData = new FormData();
        formData.append('action', 'upload');
        formData.append('testRunId', '${testRun.id}');
        formData.append('documentType', docType);
        for (var i = 0; i < files.length; i++) {
            formData.append('file', files[i]);
        }

        feedback.innerHTML = '<span class="text-primary"><i class="bi bi-arrow-repeat spin"></i> Uploading...</span>';

        fetch('${pageContext.request.contextPath}/NdtTestRun', {
            method: 'POST',
            body: formData
        })
        .then(function(response) {
            if (!response.ok) throw new Error('Upload failed');
            return response.text();
        })
        .then(function() {
            feedback.innerHTML = '<span class="text-success"><i class="bi bi-check-circle"></i> Upload successful</span>';
            fileInput.value = '';
            setTimeout(function() { location.reload(); }, 800);
        })
        .catch(function(err) {
            feedback.innerHTML = '<span class="text-danger"><i class="bi bi-exclamation-circle"></i> ' + err.message + '</span>';
        });
    }
})();
</script>
<style>
    @keyframes spin { to { transform: rotate(360deg); } }
    .spin { display: inline-block; animation: spin 1s linear infinite; }
</style>
</c:if>

</body>
</html>
