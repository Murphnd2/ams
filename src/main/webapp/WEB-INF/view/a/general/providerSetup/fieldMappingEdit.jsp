<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Column Mappings — ${fileType.fileLabel}</title>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid px-4 py-3" style="max-width: 1000px;">
    <h4 style="color: var(--ssa);"><i class="bi bi-arrows-expand"></i> Column Mappings — ${fileType.fileLabel}</h4>
    <p class="text-muted mb-1">
        <a href="ProviderSetup?action=files&id=${provider.id}" style="color: var(--ssa); text-decoration: none;">
            <i class="bi bi-arrow-left me-1"></i>Back to File Types — ${provider.providerName}
        </a>
    </p>
    <p class="text-muted mb-3" style="font-size: 0.85rem;">
        Target Entity: <span class="badge bg-info text-dark">${fileType.targetEntity}</span>
        &nbsp; Format: ${fileType.fileFormat}
    </p>
    <hr>

    <%-- Auto-Detect from Sample File --%>
    <div class="card mb-4">
        <div class="card-header d-flex align-items-center" style="background: var(--ssa); color: white; font-size: 0.9rem;">
            <i class="bi bi-magic me-1"></i>Auto-Detect from Sample File
        </div>
        <div class="card-body">
            <form method="POST" action="ProviderSetup" enctype="multipart/form-data">
                <input type="hidden" name="action" value="autoDetect">
                <input type="hidden" name="fileTypeId" value="${fileType.id}">
                <div class="row align-items-end g-3">
                    <div class="col-md-8">
                        <label class="form-label fw-semibold">Upload a sample ${fileType.fileFormat} file</label>
                        <input type="file" name="sampleFile" class="form-control"
                               accept="${fileType.fileFormat == 'CSV' ? '.csv' : fileType.fileFormat == 'TSV' ? '.tsv' : '.xlsx,.xls'}" required>
                        <div class="form-text">Headers will be read and matched to canonical fields automatically.</div>
                    </div>
                    <div class="col-md-4">
                        <button type="submit" class="ssa-action save" onclick="return confirm('This will replace all existing mappings with auto-detected ones. Continue?');">
                            <i class="bi bi-magic me-1"></i>Auto-Detect Mappings
                        </button>
                    </div>
                </div>
            </form>
        </div>
    </div>

    <%-- Current Mappings Table --%>
    <c:if test="${not empty mappings}">
        <h6 class="text-muted mb-2"><i class="bi bi-list-columns-reverse me-1"></i>Current Mappings (${mappings.size()})</h6>
        <table class="table table-hover align-middle mb-4">
            <thead>
                <tr style="font-size: 0.85rem;">
                    <th>Source Column</th>
                    <th>→</th>
                    <th>Canonical Field</th>
                    <th>Required</th>
                    <th>Key</th>
                    <th>Transform</th>
                    <th class="text-end">Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="m" items="${mappings}">
                    <tr>
                        <td><code>${m.sourceColumn}</code></td>
                        <td><i class="bi bi-arrow-right text-muted"></i></td>
                        <td><strong>${m.canonicalField}</strong></td>
                        <td>
                            <c:if test="${m.required}"><i class="bi bi-check-circle text-success"></i></c:if>
                            <c:if test="${!m.required}"><i class="bi bi-dash text-muted"></i></c:if>
                        </td>
                        <td>
                            <c:if test="${m.key}"><i class="bi bi-key text-warning"></i></c:if>
                            <c:if test="${!m.key}"><i class="bi bi-dash text-muted"></i></c:if>
                        </td>
                        <td>
                            <c:if test="${not empty m.transformRule}"><code style="font-size: 0.8rem;">${m.transformRule}</code></c:if>
                            <c:if test="${empty m.transformRule}"><span class="text-muted">—</span></c:if>
                        </td>
                        <td class="text-end">
                            <button class="ssa-action primary" style="font-size:0.85rem;"
                                    onclick="editMapping('${m.id}','${m.sourceColumn}','${m.canonicalField}','${m.required}','${m.key}','${m.transformRule}')">
                                Edit
                            </button>
                            <form method="POST" action="ProviderSetup" class="d-inline"
                                  onsubmit="return confirm('Delete this mapping?');">
                                <input type="hidden" name="action" value="deleteMapping">
                                <input type="hidden" name="mappingId" value="${m.id}">
                                <input type="hidden" name="fileTypeId" value="${fileType.id}">
                                <button type="submit" class="ssa-action cancel" style="font-size:0.85rem;">Delete</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:if>

    <c:if test="${empty mappings}">
        <div class="text-center text-muted py-4 mb-4">
            <i class="bi bi-arrows-expand" style="font-size: 1.5rem;"></i>
            <p class="mt-2">No column mappings defined yet. Upload a sample file to auto-detect, or add mappings manually below.</p>
        </div>
    </c:if>

    <%-- Add/Edit Mapping Form --%>
    <div class="card">
        <div class="card-header" style="background: var(--ssa); color: white; font-size: 0.9rem;">
            <i class="bi bi-plus-circle me-1"></i><span id="formTitle">Add Mapping</span>
        </div>
        <div class="card-body">
            <form id="mappingForm" method="POST" action="ProviderSetup">
                <input type="hidden" name="action" value="saveMapping">
                <input type="hidden" name="fileTypeId" value="${fileType.id}">
                <input type="hidden" id="mappingId" name="mappingId" value="">

                <div class="row g-3">
                    <div class="col-md-5">
                        <label class="form-label fw-semibold">Source Column <span class="text-danger">*</span></label>
                        <input type="text" id="sourceColumn" name="sourceColumn" class="form-control" required
                               placeholder="e.g., Organization ID, First Name">
                        <div class="form-text">Exact column header name from the provider's file.</div>
                    </div>
                    <div class="col-md-5">
                        <label class="form-label fw-semibold">Canonical Field <span class="text-danger">*</span></label>
                        <select id="canonicalField" name="canonicalField" class="form-select" required>
                            <option value="">— Select —</option>
                            <c:if test="${fileType.targetEntity == 'PLAN_TYPE'}">
                                <option value="plan_type_id">plan_type_id (PK)</option>
                                <option value="code">code</option>
                                <option value="name">name</option>
                                <option value="level">level</option>
                                <option value="line_of_service">line_of_service</option>
                            </c:if>
                            <c:if test="${fileType.targetEntity == 'EMPLOYER'}">
                                <option value="employer_id">employer_id (PK)</option>
                                <option value="employer_name">employer_name</option>
                                <option value="contact_name">contact_name</option>
                                <option value="email">email</option>
                                <option value="phone">phone</option>
                                <option value="alt_id">alt_id</option>
                                <option value="er_key">er_key</option>
                                <option value="status">status</option>
                            </c:if>
                            <c:if test="${fileType.targetEntity == 'EMPLOYEE'}">
                                <option value="employee_id">employee_id (PK)</option>
                                <option value="employer_id">employer_id (FK)</option>
                                <option value="first_name">first_name</option>
                                <option value="last_name">last_name</option>
                                <option value="email">email</option>
                                <option value="hr_email">hr_email</option>
                                <option value="address1">address1</option>
                                <option value="address2">address2</option>
                                <option value="city">city</option>
                                <option value="state">state</option>
                                <option value="zip">zip</option>
                                <option value="custom_id">custom_id</option>
                                <option value="user_id">user_id</option>
                                <option value="status">status</option>
                            </c:if>
                            <c:if test="${fileType.targetEntity == 'BENEFIT'}">
                                <option value="benefit_id">benefit_id (PK)</option>
                                <option value="employer_id">employer_id (FK)</option>
                                <option value="plan_type_id">plan_type_id (FK)</option>
                                <option value="plan_name">plan_name</option>
                                <option value="plan_description">plan_description</option>
                                <option value="effective_date">effective_date</option>
                                <option value="termination_date">termination_date</option>
                                <option value="status">status</option>
                            </c:if>
                        </select>
                    </div>
                    <div class="col-md-2 d-flex flex-column gap-2 justify-content-end">
                        <div class="form-check">
                            <input type="checkbox" id="isRequired" name="isRequired" class="form-check-input">
                            <label class="form-check-label" for="isRequired">Required</label>
                        </div>
                        <div class="form-check">
                            <input type="checkbox" id="isKey" name="isKey" class="form-check-input">
                            <label class="form-check-label" for="isKey">Key</label>
                        </div>
                    </div>
                    <div class="col-12">
                        <label class="form-label fw-semibold">Transform Rule</label>
                        <input type="text" id="transformRule" name="transformRule" class="form-control"
                               placeholder="e.g., UPPERCASE, DATE:M/d/yyyy, MAP:Active=true|Inactive=false, BOOLEAN:Active|Yes">
                        <div class="form-text">
                            Options: <code>UPPERCASE</code>, <code>LOWERCASE</code>, <code>INT</code>,
                            <code>DATE:pattern</code>, <code>MAP:key=val|key=val</code>, <code>BOOLEAN:truthy1|truthy2</code>
                        </div>
                    </div>
                </div>

                <div class="mt-3 d-flex gap-2">
                    <button type="submit" class="ssa-action save" id="saveBtn">Add Mapping</button>
                    <button type="button" class="ssa-action cancel" id="cancelBtn" style="display:none;" onclick="resetForm()">Cancel Edit</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    function editMapping(id, sourceCol, canonicalField, required, isKey, transform) {
        document.getElementById('mappingId').value = id;
        document.getElementById('sourceColumn').value = sourceCol;
        document.getElementById('canonicalField').value = canonicalField;
        document.getElementById('isRequired').checked = (required === 'true');
        document.getElementById('isKey').checked = (isKey === 'true');
        document.getElementById('transformRule').value = (transform === 'null' ? '' : transform);
        document.getElementById('formTitle').textContent = 'Edit Mapping';
        document.getElementById('saveBtn').textContent = 'Update Mapping';
        document.getElementById('cancelBtn').style.display = '';
        document.getElementById('sourceColumn').focus();
    }

    function resetForm() {
        document.getElementById('mappingId').value = '';
        document.getElementById('sourceColumn').value = '';
        document.getElementById('canonicalField').value = '';
        document.getElementById('isRequired').checked = false;
        document.getElementById('isKey').checked = false;
        document.getElementById('transformRule').value = '';
        document.getElementById('formTitle').textContent = 'Add Mapping';
        document.getElementById('saveBtn').textContent = 'Add Mapping';
        document.getElementById('cancelBtn').style.display = 'none';
    }
</script>
</body>
</html>
