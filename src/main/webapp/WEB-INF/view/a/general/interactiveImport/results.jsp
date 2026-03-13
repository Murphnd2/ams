<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Interactive Import — Results</title>
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
        .step-done { background: #87a948 !important; color: white !important; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid px-4 py-3" style="max-width: 800px;">
    <h4 style="color: var(--ssa);"><i class="bi bi-clipboard-check me-1"></i>Interactive Import — Results</h4>
    <p class="text-muted mb-3">${iiSession.providerName}</p>
    <hr>

    <%-- Step Indicator (all done) --%>
    <div class="d-flex gap-2 mb-4" style="font-size: 0.78rem;">
        <span class="badge step-done"><i class="bi bi-check me-1"></i>Provider</span>
        <c:forEach var="et" items="${iiSession.availableEntityTypes}">
            <c:set var="etState" value="${iiSession.entityStates[et]}"/>
            <span class="badge step-done"><i class="bi bi-check me-1"></i>${etState.entityLabel}</span>
        </c:forEach>
        <span class="badge bg-primary">Results</span>
    </div>

    <%-- Per-entity results --%>
    <c:forEach var="et" items="${iiSession.availableEntityTypes}">
        <c:set var="etState" value="${iiSession.entityStates[et]}"/>
        <div class="result-card">
            <h6>
                <c:choose>
                    <c:when test="${etState.skipped}">
                        <i class="bi bi-skip-forward text-muted"></i> ${etState.entityLabel}
                        <span class="badge bg-light text-dark ms-2" style="font-size: 0.75rem;">Skipped</span>
                    </c:when>
                    <c:when test="${not empty etState.result}">
                        <i class="bi bi-check-circle-fill text-success"></i> ${etState.entityLabel}
                    </c:when>
                    <c:otherwise>
                        <i class="bi bi-dash-circle text-muted"></i> ${etState.entityLabel}
                        <span class="badge bg-light text-dark ms-2" style="font-size: 0.75rem;">Not processed</span>
                    </c:otherwise>
                </c:choose>
            </h6>

            <c:if test="${not empty etState.result}">
                <div class="stat-row">
                    <div class="stat"><span class="dot dot-insert"></span> <strong>${etState.result.inserted}</strong> inserted</div>
                    <div class="stat"><span class="dot dot-update"></span> <strong>${etState.result.updated}</strong> updated</div>
                    <div class="stat"><span class="dot dot-skip"></span> <strong>${etState.result.skipped}</strong> unchanged</div>
                    <div class="stat"><span class="dot dot-error"></span> <strong>${etState.result.errors}</strong> errors</div>
                </div>

                <c:if test="${etState.result.serviceItemsCreated > 0}">
                    <div style="margin-top: 6px; font-size: 0.85rem;">
                        <i class="bi bi-plus-square text-success"></i>
                        <strong>${etState.result.serviceItemsCreated}</strong> service items auto-created
                    </div>
                </c:if>

                <c:if test="${not empty etState.result.warnings}">
                    <details style="margin-top: 8px; font-size: 0.82rem;">
                        <summary class="text-muted" style="cursor: pointer;">
                            ${fn:length(etState.result.warnings)} warning(s)
                        </summary>
                        <ul class="mt-1 mb-0">
                            <c:forEach var="w" items="${etState.result.warnings}">
                                <li class="text-danger">${fn:escapeXml(w)}</li>
                            </c:forEach>
                        </ul>
                    </details>
                </c:if>
            </c:if>
        </div>
    </c:forEach>

    <%-- Grand totals --%>
    <c:set var="totalInserted" value="0"/>
    <c:set var="totalUpdated" value="0"/>
    <c:set var="totalSkipped" value="0"/>
    <c:set var="totalErrors" value="0"/>
    <c:set var="totalSI" value="0"/>
    <c:forEach var="et" items="${iiSession.availableEntityTypes}">
        <c:set var="s" value="${iiSession.entityStates[et]}"/>
        <c:if test="${not empty s.result}">
            <c:set var="totalInserted" value="${totalInserted + s.result.inserted}"/>
            <c:set var="totalUpdated" value="${totalUpdated + s.result.updated}"/>
            <c:set var="totalSkipped" value="${totalSkipped + s.result.skipped}"/>
            <c:set var="totalErrors" value="${totalErrors + s.result.errors}"/>
            <c:set var="totalSI" value="${totalSI + s.result.serviceItemsCreated}"/>
        </c:if>
    </c:forEach>

    <c:if test="${totalInserted + totalUpdated + totalSkipped + totalErrors > 0}">
        <div class="result-card" style="background: #f8f9fa; border-color: var(--ssa);">
            <h6 style="color: var(--ssa);"><i class="bi bi-bar-chart me-1"></i>Overall Totals</h6>
            <div class="stat-row">
                <div class="stat"><span class="dot dot-insert"></span> <strong>${totalInserted}</strong> inserted</div>
                <div class="stat"><span class="dot dot-update"></span> <strong>${totalUpdated}</strong> updated</div>
                <div class="stat"><span class="dot dot-skip"></span> <strong>${totalSkipped}</strong> unchanged</div>
                <div class="stat"><span class="dot dot-error"></span> <strong>${totalErrors}</strong> errors</div>
            </div>
            <c:if test="${totalSI > 0}">
                <div style="margin-top: 6px; font-size: 0.85rem;">
                    <i class="bi bi-plus-square text-success"></i>
                    <strong>${totalSI}</strong> service items auto-created
                </div>
            </c:if>
        </div>
    </c:if>

    <hr>
    <div class="d-flex gap-2">
        <form method="POST" action="InteractiveImport" class="d-inline">
            <input type="hidden" name="action" value="reset">
            <button type="submit" class="ssa-action save">
                <i class="bi bi-arrow-repeat me-1"></i>New Import
            </button>
        </form>
        <a href="ViewHome25" class="ssa-action cancel">Go to Home</a>
        <a href="SequenceBuilder25" class="ssa-action primary">Sequence Builder</a>
    </div>
</div>
</body>
</html>
