<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Configure Import</title>
    <style>
        .file-badge { display: inline-flex; align-items: center; gap: 4px; }
        .file-badge i { color: #198754; }
        .summary-card {
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 12px 16px;
            margin-bottom: 10px;
        }
        .summary-card h6 { color: var(--ssa); margin-bottom: 6px; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid px-4 py-3" style="max-width: 800px;">
    <h4 style="color: var(--ssa);"><i class="bi bi-gear"></i> Review & Configure Import</h4>
    <p class="text-muted mb-3">Review uploaded files and configure renewal settings before importing.</p>
    <hr>

    <%-- Upload Errors --%>
    <c:if test="${not empty uploadErrors}">
        <div class="alert alert-danger">
            <strong>Upload Issues:</strong>
            <ul class="mb-0 mt-1">
                <c:forEach var="err" items="${uploadErrors}">
                    <li>${err}</li>
                </c:forEach>
            </ul>
        </div>
    </c:if>

    <%-- Upload Summary --%>
    <c:if test="${not empty uploadSummary}">
        <h6>Uploaded Files</h6>
        <c:forEach var="entry" items="${uploadSummary}">
            <div class="summary-card">
                <h6>
                    <span class="file-badge"><i class="bi bi-check-circle-fill"></i> ${entry.key}</span>
                </h6>
                <div class="d-flex gap-3" style="font-size: 0.85rem;">
                    <span><strong>File:</strong> ${entry.value.fileName}</span>
                    <span><strong>Rows:</strong> ${entry.value.rowCount}</span>
                </div>
                <div style="font-size: 0.78rem; color: #6c757d; margin-top: 4px;">
                    <strong>Columns:</strong>
                    <c:forEach var="h" items="${entry.value.headers}" varStatus="s">
                        <code>${h}</code><c:if test="${!s.last}">, </c:if>
                    </c:forEach>
                </div>
            </div>
        </c:forEach>
    </c:if>

    <%-- Import Form --%>
    <form method="POST" action="SummitImport">
        <input type="hidden" name="action" value="import">

        <%-- Renewal Months Configuration --%>
        <c:if test="${not empty sessionScope.si_planTypeFile}">
            <hr>
            <h6 style="color: var(--ssa);">Default Renewal Frequency</h6>
            <p class="text-muted" style="font-size: 0.85rem;">
                Set the default renewal frequency (in months) for imported benefits.
                This applies to all plan types unless overridden below. Default: 12 months.
            </p>
            <div class="mb-3" style="max-width: 200px;">
                <label class="form-label" style="font-size: 0.85rem;"><strong>Default Months</strong></label>
                <input type="number" class="form-control form-control-sm" name="renewalMonths_0"
                       value="12" min="1" max="60">
                <div class="form-text">Applied to types without a specific override.</div>
            </div>
        </c:if>

        <hr>
        <div class="d-flex gap-2">
            <button type="submit" class="btn btn-success">
                <i class="bi bi-play-fill"></i> Run Import
            </button>
            <a href="SummitImport" class="btn btn-outline-secondary">
                <i class="bi bi-arrow-left"></i> Back to Upload
            </a>
        </div>
    </form>
    <form method="POST" action="SummitImport" class="d-inline mt-2">
        <input type="hidden" name="action" value="reset">
        <button type="submit" class="btn btn-outline-danger btn-sm">Reset</button>
    </form>
</div>
</body>
</html>
