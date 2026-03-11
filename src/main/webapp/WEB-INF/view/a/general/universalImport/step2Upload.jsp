<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Universal Import — Upload Files</title>
    <style>
        .upload-card {
            border: 2px dashed #dee2e6;
            border-radius: 8px;
            padding: 16px 20px;
            margin-bottom: 12px;
            transition: border-color 0.2s;
        }
        .upload-card:hover { border-color: var(--ssa); }
        .upload-card .badge { font-size: 0.7rem; }
        .upload-label { font-weight: 600; color: var(--ssa); margin-bottom: 4px; }
        .upload-hint { font-size: 0.82rem; color: #6c757d; margin-bottom: 8px; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid px-4 py-3" style="max-width: 800px;">
    <h4 style="color: var(--ssa);"><i class="bi bi-cloud-upload"></i> Universal Import — ${provider.providerName}</h4>
    <p class="text-muted mb-3">Upload the data files exported from ${provider.providerName}.</p>
    <hr>

    <%-- Step Indicator --%>
    <div class="d-flex gap-2 mb-4" style="font-size: 0.82rem;">
        <span class="badge bg-light text-dark">1. Select Provider</span>
        <span class="badge bg-primary">2. Upload Files</span>
        <span class="badge bg-light text-dark">3. Review & Configure</span>
        <span class="badge bg-light text-dark">4. Results</span>
    </div>

    <c:choose>
        <c:when test="${not empty fileTypes}">
            <form method="POST" action="UniversalImport" enctype="multipart/form-data">
                <input type="hidden" name="action" value="upload">

                <c:forEach var="ft" items="${fileTypes}">
                    <div class="upload-card">
                        <div class="upload-label">
                            <i class="bi bi-file-earmark-arrow-up"></i> ${ft.fileLabel}
                            <span class="badge bg-secondary">${ft.fileFormat}</span>
                            <span class="badge bg-info text-dark">${ft.targetEntity}</span>
                            <c:if test="${ft.required}">
                                <span class="badge bg-danger">Required</span>
                            </c:if>
                        </div>
                        <c:if test="${not empty ft.description}">
                            <div class="upload-hint">${ft.description}</div>
                        </c:if>
                        <input type="file" class="form-control form-control-sm" name="file_${ft.id}"
                               accept="${ft.fileFormat == 'CSV' ? '.csv,.txt' : ft.fileFormat == 'TSV' ? '.tsv,.txt' : '.xlsx,.xls'}"
                               style="max-width: 450px;"
                               ${ft.required ? 'required' : ''}>
                    </div>
                </c:forEach>

                <hr>
                <div class="d-flex gap-2">
                    <button type="submit" class="ssa-action save">
                        <i class="bi bi-arrow-right me-1"></i>Next: Review & Configure
                    </button>
                    <a href="UniversalImport" class="ssa-action cancel">
                        <i class="bi bi-arrow-left me-1"></i>Back
                    </a>
                </div>
            </form>
        </c:when>
        <c:otherwise>
            <div class="text-center text-muted py-4">
                <i class="bi bi-file-earmark-plus" style="font-size: 1.5rem;"></i>
                <p class="mt-2">No file types defined for this provider.</p>
                <a href="ProviderSetup?action=files&id=${provider.id}" class="ssa-action primary">
                    <i class="bi bi-plus-circle me-1"></i>Configure File Types
                </a>
            </div>
        </c:otherwise>
    </c:choose>
</div>
</body>
</html>
