<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Import History</title>
    <style>
        .stat-cell { font-size: 0.82rem; white-space: nowrap; }
        .stat-cell .inserted { color: #198754; font-weight: 600; }
        .stat-cell .updated  { color: #0d6efd; font-weight: 600; }
        .stat-cell .skipped  { color: #6c757d; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid px-4 py-3" style="max-width: 1100px;">
    <div class="d-flex align-items-center justify-content-between mb-3">
        <h4 class="mb-0" style="color: var(--ssa);"><i class="bi bi-clock-history"></i> Import History</h4>
        <a href="UniversalImport" class="ssa-action primary">
            <i class="bi bi-cloud-upload me-1"></i>New Import
        </a>
    </div>
    <p class="text-muted mb-3">Recent data import runs from the Universal Import system.</p>
    <hr>

    <c:choose>
        <c:when test="${not empty runs}">
            <table class="table table-hover align-middle" style="font-size: 0.88rem;">
                <thead>
                    <tr style="font-size: 0.8rem;">
                        <th>#</th>
                        <th>Provider</th>
                        <th>Status</th>
                        <th>Started</th>
                        <th>Plan Types</th>
                        <th>Employers</th>
                        <th>Employees</th>
                        <th>Benefits</th>
                        <th>SIs</th>
                        <th>Notes</th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="r" items="${runs}">
                        <tr>
                            <td>${r.id}</td>
                            <td><strong>${r.provider.providerName}</strong></td>
                            <td>
                                <c:choose>
                                    <c:when test="${r.status == 'COMPLETED'}"><span class="badge bg-success">Completed</span></c:when>
                                    <c:when test="${r.status == 'RUNNING'}"><span class="badge bg-warning text-dark">Running</span></c:when>
                                    <c:when test="${r.status == 'FAILED'}"><span class="badge bg-danger">Failed</span></c:when>
                                    <c:otherwise><span class="badge bg-secondary">${r.status}</span></c:otherwise>
                                </c:choose>
                            </td>
                            <td style="font-size: 0.82rem;">
                                <c:if test="${r.startedOn != null}">
                                    <fmt:formatDate value="${r.startedOn}" pattern="MMM d, yyyy h:mm a"/>
                                </c:if>
                            </td>
                            <td class="stat-cell">
                                <span class="inserted">${r.planTypesInserted}</span>/<span class="updated">${r.planTypesUpdated}</span>/<span class="skipped">${r.planTypesSkipped}</span>
                            </td>
                            <td class="stat-cell">
                                <span class="inserted">${r.employersInserted}</span>/<span class="updated">${r.employersUpdated}</span>/<span class="skipped">${r.employersSkipped}</span>
                            </td>
                            <td class="stat-cell">
                                <span class="inserted">${r.employeesInserted}</span>/<span class="updated">${r.employeesUpdated}</span>/<span class="skipped">${r.employeesSkipped}</span>
                            </td>
                            <td class="stat-cell">
                                <span class="inserted">${r.benefitsInserted}</span>/<span class="updated">${r.benefitsUpdated}</span>/<span class="skipped">${r.benefitsSkipped}</span>
                            </td>
                            <td class="stat-cell">
                                <c:if test="${r.serviceItemsCreated > 0}">
                                    <span class="inserted">${r.serviceItemsCreated}</span>
                                </c:if>
                                <c:if test="${r.serviceItemsCreated == 0}">—</c:if>
                            </td>
                            <td style="font-size: 0.8rem;">
                                <c:if test="${not empty r.warnings}">
                                    <details>
                                        <summary class="text-warning" style="cursor: pointer;">warnings</summary>
                                        <pre style="font-size: 0.75rem; white-space: pre-wrap; max-width: 300px;">${r.warnings}</pre>
                                    </details>
                                </c:if>
                                <c:if test="${not empty r.errors}">
                                    <details>
                                        <summary class="text-danger" style="cursor: pointer;">errors</summary>
                                        <pre style="font-size: 0.75rem; white-space: pre-wrap; max-width: 300px;">${r.errors}</pre>
                                    </details>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
            <p class="text-muted" style="font-size: 0.78rem;">
                Columns show <span style="color:#198754; font-weight:600;">inserted</span> /
                <span style="color:#0d6efd; font-weight:600;">updated</span> /
                <span style="color:#6c757d;">unchanged</span> counts.
                SIs = Service Items auto-created.
            </p>
        </c:when>
        <c:otherwise>
            <div class="text-center text-muted py-5">
                <i class="bi bi-clock-history" style="font-size: 2rem;"></i>
                <p class="mt-2">No import history yet.</p>
                <a href="UniversalImport" class="ssa-action primary">Run Your First Import</a>
            </div>
        </c:otherwise>
    </c:choose>
</div>
</body>
</html>
