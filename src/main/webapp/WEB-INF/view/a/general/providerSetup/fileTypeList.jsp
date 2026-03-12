<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>File Types — ${provider.providerName}</title>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid px-4 py-3" style="max-width: 1050px;">
    <h4 style="color: var(--ssa);"><i class="bi bi-file-earmark-ruled"></i> File Types — ${provider.providerName}</h4>
    <p class="text-muted mb-3">
        <a href="ProviderSetup" style="color: var(--ssa); text-decoration: none;"><i class="bi bi-arrow-left me-1"></i>Back to Providers</a>
    </p>
    <hr>

    <c:if test="${not empty fileTypes}">
        <table class="table table-hover align-middle mb-4">
            <thead>
                <tr style="font-size: 0.85rem;">
                    <th>#</th>
                    <th>File Label</th>
                    <th>Target Entity</th>
                    <th>Format</th>
                    <th>Mode</th>
                    <th>Status</th>
                    <th>Required</th>
                    <th class="text-end">Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="ft" items="${fileTypes}">
                    <tr>
                        <td>${ft.sortOrder}</td>
                        <td>
                            <strong>${ft.fileLabel}</strong>
                            <c:if test="${not empty ft.description}">
                                <br><small class="text-muted">${ft.description}</small>
                            </c:if>
                        </td>
                        <td><span class="badge bg-info text-dark">${ft.targetEntity}</span></td>
                        <td>${ft.fileFormat}</td>
                        <td style="font-size: 0.82rem;">
                            <c:choose>
                                <c:when test="${ft.updateMode == 'CREATE_ONLY'}">Create Only</c:when>
                                <c:when test="${ft.updateMode == 'UPDATE_ONLY'}">Update Only</c:when>
                                <c:otherwise>Create &amp; Update</c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${ft.mappingStatus == 'READY'}">
                                    <span style="font-size:0.78rem; padding:2px 8px; border-radius:10px; background:#d1e7dd; color:#0f5132; font-weight:600;">
                                        <i class="bi bi-check-circle me-1"></i>READY
                                    </span>
                                </c:when>
                                <c:otherwise>
                                    <span style="font-size:0.78rem; padding:2px 8px; border-radius:10px; background:#fff3cd; color:#856404; font-weight:600;">
                                        <i class="bi bi-exclamation-circle me-1"></i>PENDING
                                    </span>
                                </c:otherwise>
                            </c:choose>
                        </td>
                        <td>
                            <c:if test="${ft.required}"><i class="bi bi-check-circle text-success"></i></c:if>
                            <c:if test="${!ft.required}"><i class="bi bi-dash text-muted"></i></c:if>
                        </td>
                        <td class="text-end">
                            <a href="ProviderSetup?action=mappings&fileTypeId=${ft.id}" class="ssa-action primary" style="font-size:0.85rem;">Mappings</a>
                            <form method="POST" action="ProviderSetup" class="d-inline"
                                  onsubmit="return confirm('Delete this file type and its mappings?');">
                                <input type="hidden" name="action" value="deleteFileType">
                                <input type="hidden" name="fileTypeId" value="${ft.id}">
                                <button type="submit" class="ssa-action cancel" style="font-size:0.85rem;">Delete</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:if>

    <c:if test="${empty fileTypes}">
        <div class="text-center text-muted py-4 mb-4">
            <i class="bi bi-file-earmark-plus" style="font-size: 1.5rem;"></i>
            <p class="mt-2">No file types defined yet. Add the files this provider exports.</p>
        </div>
    </c:if>

    <%-- Add/Edit File Type Form --%>
    <div class="card">
        <div class="card-header" style="background: var(--ssa); color: white; font-size: 0.9rem;">
            <i class="bi bi-plus-circle me-1"></i>Add File Type
        </div>
        <div class="card-body">
            <form method="POST" action="ProviderSetup">
                <input type="hidden" name="action" value="saveFileType">
                <input type="hidden" name="providerId" value="${provider.id}">

                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="form-label fw-semibold">File Label <span class="text-danger">*</span></label>
                        <input type="text" name="fileLabel" class="form-control" required
                               placeholder="e.g., Employer Listing (J1)">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label fw-semibold">Target Entity <span class="text-danger">*</span></label>
                        <select name="targetEntity" class="form-select" required>
                            <option value="PLAN_TYPE">PLAN_TYPE</option>
                            <option value="EMPLOYER">EMPLOYER</option>
                            <option value="EMPLOYEE">EMPLOYEE</option>
                            <option value="BENEFIT">BENEFIT</option>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label fw-semibold">Format</label>
                        <select name="fileFormat" class="form-select">
                            <option value="CSV">CSV</option>
                            <option value="EXCEL">Excel</option>
                            <option value="TSV">TSV</option>
                        </select>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label fw-semibold">Update Mode</label>
                        <select name="updateMode" class="form-select">
                            <option value="CREATE_AND_UPDATE">Create New &amp; Update Existing</option>
                            <option value="CREATE_ONLY">Create New Only</option>
                            <option value="UPDATE_ONLY">Update Existing Only</option>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <label class="form-label fw-semibold">Sort Order</label>
                        <input type="number" name="sortOrder" class="form-control" value="0">
                    </div>
                    <div class="col-md-3 d-flex align-items-end">
                        <div class="form-check">
                            <input type="checkbox" name="isRequired" class="form-check-input" checked>
                            <label class="form-check-label">Required</label>
                        </div>
                    </div>
                    <div class="col-12">
                        <label class="form-label fw-semibold">Description</label>
                        <input type="text" name="description" class="form-control"
                               placeholder="Help text shown in wizard (optional)">
                    </div>
                </div>

                <div class="mt-3">
                    <button type="submit" class="ssa-action save">Add File Type</button>
                </div>
            </form>
        </div>
    </div>
</div>
</body>
</html>
