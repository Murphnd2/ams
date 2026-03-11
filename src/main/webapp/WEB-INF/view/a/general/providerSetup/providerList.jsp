<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Import Providers</title>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid px-4 py-3" style="max-width: 1100px;">
    <div class="d-flex align-items-center justify-content-between mb-3">
        <h4 class="mb-0" style="color: var(--ssa);"><i class="bi bi-plug"></i> Import Providers</h4>
        <a href="ProviderSetup?action=edit" class="ssa-action primary">
            <i class="bi bi-plus-circle me-1"></i>Add Provider
        </a>
    </div>
    <p class="text-muted mb-3">Configure TPA platform connections. Each provider defines the file types and column mappings needed to import data into AMS.</p>
    <hr>

    <c:if test="${empty providers}">
        <div class="text-center text-muted py-5">
            <i class="bi bi-plug" style="font-size: 2rem;"></i>
            <p class="mt-2">No providers configured yet.</p>
            <a href="ProviderSetup?action=edit" class="btn btn-sm btn-outline-ssa">Add Your First Provider</a>
        </div>
    </c:if>

    <c:if test="${not empty providers}">
        <table class="table table-hover align-middle">
            <thead>
                <tr style="font-size: 0.85rem;">
                    <th>Provider</th>
                    <th>Code</th>
                    <th>Status</th>
                    <th class="text-end">Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="p" items="${providers}">
                    <tr>
                        <td>
                            <strong>${p.providerName}</strong>
                            <c:if test="${not empty p.description}">
                                <br><small class="text-muted">${p.description}</small>
                            </c:if>
                        </td>
                        <td><code>${p.providerCode}</code></td>
                        <td>
                            <c:choose>
                                <c:when test="${p.active}"><span class="badge bg-success">Active</span></c:when>
                                <c:otherwise><span class="badge bg-secondary">Inactive</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="text-end" style="white-space: nowrap;">
                            <a href="ProviderSetup?action=edit&id=${p.id}" class="ssa-action primary" style="font-size:0.85rem;">Edit</a>
                            <a href="ProviderSetup?action=files&id=${p.id}" class="ssa-action primary" style="font-size:0.85rem;">File Types</a>
                            <a href="ProviderSetup?action=planTypes&id=${p.id}" class="ssa-action primary" style="font-size:0.85rem;">Plan Types</a>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:if>
</div>
</body>
</html>
