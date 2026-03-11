<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>${empty provider ? 'Add' : 'Edit'} Provider</title>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid px-4 py-3" style="max-width: 600px;">
    <h4 style="color: var(--ssa);"><i class="bi bi-plug"></i> ${empty provider ? 'Add' : 'Edit'} Provider</h4>
    <p class="text-muted mb-3">
        <a href="ProviderSetup" style="color: var(--ssa); text-decoration: none;"><i class="bi bi-arrow-left me-1"></i>Back to Providers</a>
    </p>
    <hr>

    <form method="POST" action="ProviderSetup">
        <input type="hidden" name="action" value="saveProvider">
        <c:if test="${not empty provider}">
            <input type="hidden" name="providerId" value="${provider.id}">
        </c:if>

        <div class="mb-3">
            <label class="form-label fw-semibold">Provider Name <span class="text-danger">*</span></label>
            <input type="text" name="providerName" class="form-control" required
                   value="${not empty provider ? provider.providerName : ''}"
                   placeholder="e.g., DataPath Summit, WEX, Alegeus">
        </div>

        <div class="mb-3">
            <label class="form-label fw-semibold">Provider Code <span class="text-danger">*</span></label>
            <input type="text" name="providerCode" class="form-control" required maxlength="30"
                   value="${not empty provider ? provider.providerCode : ''}"
                   placeholder="e.g., SUMMIT, WEX, ALEGEUS" style="text-transform: uppercase;">
            <div class="form-text">Used as the source_type identifier on imported records. Must be unique.</div>
        </div>

        <div class="mb-3">
            <label class="form-label fw-semibold">Description</label>
            <textarea name="description" class="form-control" rows="3"
                      placeholder="Brief description of this platform integration...">${not empty provider ? provider.description : ''}</textarea>
        </div>

        <div class="d-flex gap-3 mt-4">
            <button type="submit" class="ssa-action save">Save Provider</button>
            <a href="ProviderSetup" class="ssa-action cancel">Cancel</a>
        </div>
    </form>

    <c:if test="${not empty provider}">
        <hr class="mt-4">
        <form method="POST" action="ProviderSetup" onsubmit="return confirm('Deactivate this provider?');">
            <input type="hidden" name="action" value="deleteProvider">
            <input type="hidden" name="providerId" value="${provider.id}">
            <button type="submit" class="ssa-action cancel" style="font-size: 0.85rem;">
                <i class="bi bi-x-circle me-1"></i>Deactivate Provider
            </button>
        </form>
    </c:if>
</div>
</body>
</html>
