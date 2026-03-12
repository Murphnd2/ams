<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Interactive Import — Select Provider</title>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid px-4 py-3" style="max-width: 700px;">
    <h4 style="color: var(--ssa);"><i class="bi bi-diagram-3 me-1"></i>Interactive Import</h4>
    <p class="text-muted mb-3">Import data entity-by-entity with interactive cross-reference resolution.</p>
    <hr>

    <%-- Step Indicator --%>
    <div class="d-flex gap-2 mb-4" style="font-size: 0.82rem;">
        <span class="badge bg-primary">1. Provider</span>
        <span class="badge bg-light text-dark">2. Plan Types</span>
        <span class="badge bg-light text-dark">3. Employers</span>
        <span class="badge bg-light text-dark">4. Benefits</span>
        <span class="badge bg-light text-dark">5. Employees</span>
        <span class="badge bg-light text-dark">6. Results</span>
    </div>

    <form method="POST" action="InteractiveImport">
        <input type="hidden" name="action" value="selectProvider">

        <div class="mb-4">
            <label class="form-label fw-semibold">Import Provider</label>
            <select name="providerId" class="form-select" required style="max-width: 400px;">
                <option value="">— Select a provider —</option>
                <c:forEach var="p" items="${providers}">
                    <option value="${p.id}">${p.providerName} (${p.providerCode})</option>
                </c:forEach>
            </select>
            <div class="form-text">Choose the TPA platform to import from. Each entity type will be processed in order with interactive matching.</div>
        </div>

        <div class="d-flex gap-2">
            <button type="submit" class="ssa-action save">
                <i class="bi bi-arrow-right me-1"></i>Start Import
            </button>
            <a href="ViewHome25" class="ssa-action cancel">Cancel</a>
        </div>
    </form>

    <c:if test="${empty providers}">
        <div class="text-center text-muted py-4">
            <p style="font-size: 0.85rem;">No providers configured.
                <a href="ProviderSetup" style="color: var(--ssa);">Add one</a> first.</p>
        </div>
    </c:if>

    <hr class="mt-4">
    <div class="d-flex gap-3" style="font-size: 0.82rem;">
        <a href="ProviderSetup" style="color: var(--ssa); text-decoration: none;">
            <i class="bi bi-gear me-1"></i>Manage Providers
        </a>
        <a href="UniversalImport" style="color: var(--ssa); text-decoration: none;">
            <i class="bi bi-cloud-upload me-1"></i>Batch Import (legacy)
        </a>
    </div>
</div>
</body>
</html>
