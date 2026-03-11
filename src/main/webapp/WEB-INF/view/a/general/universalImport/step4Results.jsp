<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Universal Import — Results</title>
    <style>
        .result-card {
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 14px 18px;
            margin-bottom: 10px;
        }
        .result-card h6 { color: var(--ssa); margin-bottom: 8px; }
        .stat-row { display: flex; gap: 20px; font-size: 0.88rem; flex-wrap: wrap; }
        .stat-row .stat { display: flex; align-items: center; gap: 4px; }
        .stat .dot {
            width: 10px; height: 10px; border-radius: 50%; display: inline-block;
        }
        .dot-insert { background: #198754; }
        .dot-update { background: #0d6efd; }
        .dot-skip   { background: #6c757d; }
        .dot-error  { background: #dc3545; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid px-4 py-3" style="max-width: 800px;">
    <h4 style="color: var(--ssa);"><i class="bi bi-clipboard-check"></i> Import Results</h4>
    <hr>

    <%-- Step Indicator --%>
    <div class="d-flex gap-2 mb-4" style="font-size: 0.82rem;">
        <span class="badge bg-light text-dark">1. Select Provider</span>
        <span class="badge bg-light text-dark">2. Upload Files</span>
        <span class="badge bg-light text-dark">3. Review & Configure</span>
        <span class="badge bg-success">4. Results</span>
    </div>

    <c:choose>
        <c:when test="${not empty sessionScope.ui_results}">
            <c:forEach var="entry" items="${sessionScope.ui_results}">
                <div class="result-card">
                    <h6>
                        <c:choose>
                            <c:when test="${entry.key eq 'ERROR'}">
                                <i class="bi bi-exclamation-triangle-fill text-danger"></i> ${entry.key}
                            </c:when>
                            <c:otherwise>
                                <i class="bi bi-check-circle-fill text-success"></i> ${entry.key}
                            </c:otherwise>
                        </c:choose>
                    </h6>
                    <div class="stat-row">
                        <div class="stat"><span class="dot dot-insert"></span> <strong>${entry.value.inserted}</strong> inserted</div>
                        <div class="stat"><span class="dot dot-update"></span> <strong>${entry.value.updated}</strong> updated</div>
                        <div class="stat"><span class="dot dot-skip"></span> <strong>${entry.value.skipped}</strong> unchanged</div>
                        <div class="stat"><span class="dot dot-error"></span> <strong>${entry.value.errors}</strong> errors</div>
                    </div>

                    <c:if test="${entry.value.serviceItemsCreated > 0}">
                        <div style="margin-top: 6px; font-size: 0.85rem;">
                            <i class="bi bi-plus-square text-success"></i>
                            <strong>${entry.value.serviceItemsCreated}</strong> service items auto-created
                        </div>
                    </c:if>

                    <c:if test="${not empty entry.value.warnings}">
                        <details style="margin-top: 8px; font-size: 0.82rem;">
                            <summary class="text-muted" style="cursor: pointer;">
                                ${entry.value.warnings.size()} warning(s)
                            </summary>
                            <ul class="mt-1 mb-0">
                                <c:forEach var="w" items="${entry.value.warnings}">
                                    <li class="text-danger">${w}</li>
                                </c:forEach>
                            </ul>
                        </details>
                    </c:if>
                </div>
            </c:forEach>
        </c:when>
        <c:otherwise>
            <div class="alert alert-info">No import results available.</div>
        </c:otherwise>
    </c:choose>

    <hr>
    <div class="d-flex gap-2">
        <form method="POST" action="UniversalImport" class="d-inline">
            <input type="hidden" name="action" value="reset">
            <button type="submit" class="ssa-action save">
                <i class="bi bi-arrow-repeat me-1"></i>New Import
            </button>
        </form>
        <a href="ViewHome25" class="ssa-action cancel">Go to Home</a>
        <a href="SequenceBuilder25" class="ssa-action primary">Go to Sequence Builder</a>
    </div>
</div>
</body>
</html>
