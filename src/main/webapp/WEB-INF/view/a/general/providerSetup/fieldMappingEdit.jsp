<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Column Mappings — ${fileType.fileLabel}</title>
    <style>
        .status-badge { font-size: 0.8rem; padding: 3px 10px; border-radius: 12px; font-weight: 600; }
        .status-pending { background: #fff3cd; color: #856404; }
        .status-ready   { background: #d1e7dd; color: #0f5132; }
        .mapping-table th { font-size: 0.82rem; position: sticky; top: 0; background: #f8f9fa; z-index: 1; }
        .mapping-table td { font-size: 0.85rem; vertical-align: middle; }
        .mapping-table select.form-select { font-size: 0.82rem; padding: 4px 8px; }
        .mapping-table .form-check-input { width: 1.1em; height: 1.1em; }
        .fk-type-select { font-size: 0.8rem; padding: 2px 6px; width: 120px; display: none; }
        .transform-input { font-size: 0.8rem; padding: 3px 6px; }
        .validation-msg { font-size: 0.85rem; }
        #transformSection .transform-row { display: none; }
        #transformSection.expanded .transform-row { display: table-row; }
    </style>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid px-4 py-3" style="max-width: 1100px;">

    <%-- Header --%>
    <div class="d-flex align-items-center justify-content-between mb-2">
        <div>
            <h4 class="mb-0" style="color: var(--ssa);">
                <i class="bi bi-arrows-expand me-1"></i>Column Mappings — ${fileType.fileLabel}
            </h4>
            <p class="text-muted mb-0 mt-1" style="font-size: 0.85rem;">
                <a href="ProviderSetup?action=files&id=${provider.id}" style="color: var(--ssa); text-decoration: none;">
                    <i class="bi bi-arrow-left me-1"></i>Back to File Types — ${provider.providerName}
                </a>
            </p>
        </div>
        <div class="text-end">
            <span class="badge bg-info text-dark me-2" style="font-size: 0.82rem;">Target: ${fileType.targetEntity}</span>
            <c:choose>
                <c:when test="${fileType.mappingStatus == 'READY'}">
                    <span class="status-badge status-ready"><i class="bi bi-check-circle me-1"></i>READY</span>
                </c:when>
                <c:otherwise>
                    <span class="status-badge status-pending"><i class="bi bi-exclamation-circle me-1"></i>PENDING</span>
                </c:otherwise>
            </c:choose>
        </div>
    </div>
    <hr class="mt-1">

    <%-- Sample File Upload Card --%>
    <div class="card mb-4">
        <div class="card-header d-flex align-items-center" style="background: var(--ssa); color: white; font-size: 0.9rem;">
            <i class="bi bi-file-earmark-arrow-up me-1"></i>Sample File
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
                        <div class="form-text">
                            <i class="bi bi-exclamation-triangle text-warning me-1"></i>
                            Uploading a new sample file will <strong>replace all existing mappings</strong> and reset status to PENDING.
                        </div>
                    </div>
                    <div class="col-md-4">
                        <button type="submit" class="ssa-action save"
                                onclick="return confirm('This will replace all existing mappings with auto-detected ones from the new sample file. Continue?');">
                            <i class="bi bi-file-earmark-arrow-up me-1"></i>Upload & Auto-Detect
                        </button>
                    </div>
                </div>
            </form>
            <c:if test="${not empty sampleHeaders}">
                <div class="mt-2" style="font-size: 0.82rem;">
                    <i class="bi bi-check-circle text-success me-1"></i>
                    Sample file loaded: <strong>${fn:length(sampleHeaders)} columns</strong> detected
                </div>
            </c:if>
        </div>
    </div>

    <%-- Column Mapping Table (only shown when mappings exist) --%>
    <c:if test="${not empty mappings}">
        <form id="bulkForm" method="POST" action="ProviderSetup">
            <input type="hidden" name="action" value="saveMappingsBulk">
            <input type="hidden" name="fileTypeId" value="${fileType.id}">

            <h6 class="text-muted mb-2">
                <i class="bi bi-table me-1"></i>Column Mapping Table
                <span class="ms-2 text-secondary" style="font-size: 0.8rem;">(${fn:length(mappings)} columns)</span>
            </h6>

            <div class="table-responsive" style="max-height: 60vh; overflow-y: auto;">
                <table class="table table-hover mapping-table mb-0">
                    <thead>
                        <tr>
                            <th style="width: 22%;">Source Column</th>
                            <th style="width: 5%; text-align: center;">PK</th>
                            <c:if test="${fileType.targetEntity == 'BENEFIT' || fileType.targetEntity == 'EMPLOYEE'}">
                                <th style="width: 5%; text-align: center;">FK</th>
                                <th style="width: 10%;">FK Type</th>
                            </c:if>
                            <th style="width: 22%;">AMS Field</th>
                            <th style="width: 5%; text-align: center;">Req</th>
                            <th style="width: 22%;" id="transformHeader">
                                <a href="javascript:void(0)" onclick="toggleTransforms()" style="color: var(--ssa); text-decoration: none;">
                                    <i class="bi bi-chevron-right me-1" id="transformChevron"></i>Transform Rules
                                </a>
                            </th>
                        </tr>
                    </thead>
                    <tbody id="transformSection">
                        <c:forEach var="m" items="${mappings}" varStatus="idx">
                            <tr>
                                <%-- Source Column (readonly) --%>
                                <td>
                                    <code>${m.sourceColumn}</code>
                                    <input type="hidden" name="sourceColumn" value="${m.sourceColumn}">
                                </td>

                                <%-- PK radio --%>
                                <td class="text-center">
                                    <input type="radio" name="pkColumnIndex" value="${idx.index}"
                                           class="form-check-input" ${m.key ? 'checked' : ''}>
                                </td>

                                <%-- FK checkbox + type (only for BENEFIT/EMPLOYEE) --%>
                                <c:if test="${fileType.targetEntity == 'BENEFIT' || fileType.targetEntity == 'EMPLOYEE'}">
                                    <td class="text-center">
                                        <input type="checkbox" class="form-check-input fk-check"
                                               data-row="${idx.index}" ${m.fk ? 'checked' : ''}
                                               onchange="toggleFkType(this, ${idx.index})">
                                        <input type="hidden" name="isFkFlag" id="isFk_${idx.index}" value="${m.fk ? 'true' : 'false'}">
                                    </td>
                                    <td>
                                        <select name="fkEntityType" id="fkType_${idx.index}"
                                                class="form-select fk-type-select" ${m.fk ? 'style="display:inline-block;"' : ''}>
                                            <option value="">—</option>
                                            <option value="EMPLOYER" ${m.fkEntityType == 'EMPLOYER' ? 'selected' : ''}>EMPLOYER</option>
                                            <c:if test="${fileType.targetEntity == 'BENEFIT'}">
                                                <option value="PLAN_TYPE" ${m.fkEntityType == 'PLAN_TYPE' ? 'selected' : ''}>PLAN_TYPE</option>
                                            </c:if>
                                        </select>
                                    </td>
                                </c:if>
                                <c:if test="${fileType.targetEntity != 'BENEFIT' && fileType.targetEntity != 'EMPLOYEE'}">
                                    <%-- Hidden FK fields for non-FK target entities --%>
                                    <input type="hidden" name="isFkFlag" value="false">
                                    <input type="hidden" name="fkEntityType" value="">
                                </c:if>

                                <%-- AMS Field dropdown --%>
                                <td>
                                    <select name="canonicalField" class="form-select">
                                        <option value="">— None —</option>
                                        <c:if test="${fileType.targetEntity == 'PLAN_TYPE'}">
                                            <option value="plan_type_id" ${m.canonicalField == 'plan_type_id' ? 'selected' : ''}>plan_type_id</option>
                                            <option value="code" ${m.canonicalField == 'code' ? 'selected' : ''}>code</option>
                                            <option value="name" ${m.canonicalField == 'name' ? 'selected' : ''}>name</option>
                                            <option value="level" ${m.canonicalField == 'level' ? 'selected' : ''}>level</option>
                                            <option value="line_of_service" ${m.canonicalField == 'line_of_service' ? 'selected' : ''}>line_of_service</option>
                                        </c:if>
                                        <c:if test="${fileType.targetEntity == 'EMPLOYER'}">
                                            <option value="employer_id" ${m.canonicalField == 'employer_id' ? 'selected' : ''}>employer_id</option>
                                            <option value="employer_name" ${m.canonicalField == 'employer_name' ? 'selected' : ''}>employer_name</option>
                                            <option value="contact_name" ${m.canonicalField == 'contact_name' ? 'selected' : ''}>contact_name</option>
                                            <option value="email" ${m.canonicalField == 'email' ? 'selected' : ''}>email</option>
                                            <option value="phone" ${m.canonicalField == 'phone' ? 'selected' : ''}>phone</option>
                                            <option value="alt_id" ${m.canonicalField == 'alt_id' ? 'selected' : ''}>alt_id</option>
                                            <option value="er_key" ${m.canonicalField == 'er_key' ? 'selected' : ''}>er_key</option>
                                            <option value="status" ${m.canonicalField == 'status' ? 'selected' : ''}>status</option>
                                        </c:if>
                                        <c:if test="${fileType.targetEntity == 'EMPLOYEE'}">
                                            <option value="employee_id" ${m.canonicalField == 'employee_id' ? 'selected' : ''}>employee_id</option>
                                            <option value="employer_id" ${m.canonicalField == 'employer_id' ? 'selected' : ''}>employer_id</option>
                                            <option value="first_name" ${m.canonicalField == 'first_name' ? 'selected' : ''}>first_name</option>
                                            <option value="last_name" ${m.canonicalField == 'last_name' ? 'selected' : ''}>last_name</option>
                                            <option value="email" ${m.canonicalField == 'email' ? 'selected' : ''}>email</option>
                                            <option value="hr_email" ${m.canonicalField == 'hr_email' ? 'selected' : ''}>hr_email</option>
                                            <option value="address1" ${m.canonicalField == 'address1' ? 'selected' : ''}>address1</option>
                                            <option value="address2" ${m.canonicalField == 'address2' ? 'selected' : ''}>address2</option>
                                            <option value="city" ${m.canonicalField == 'city' ? 'selected' : ''}>city</option>
                                            <option value="state" ${m.canonicalField == 'state' ? 'selected' : ''}>state</option>
                                            <option value="zip" ${m.canonicalField == 'zip' ? 'selected' : ''}>zip</option>
                                            <option value="custom_id" ${m.canonicalField == 'custom_id' ? 'selected' : ''}>custom_id</option>
                                            <option value="user_id" ${m.canonicalField == 'user_id' ? 'selected' : ''}>user_id</option>
                                            <option value="status" ${m.canonicalField == 'status' ? 'selected' : ''}>status</option>
                                        </c:if>
                                        <c:if test="${fileType.targetEntity == 'BENEFIT'}">
                                            <option value="benefit_id" ${m.canonicalField == 'benefit_id' ? 'selected' : ''}>benefit_id</option>
                                            <option value="employer_id" ${m.canonicalField == 'employer_id' ? 'selected' : ''}>employer_id</option>
                                            <option value="plan_type_id" ${m.canonicalField == 'plan_type_id' ? 'selected' : ''}>plan_type_id</option>
                                            <option value="plan_name" ${m.canonicalField == 'plan_name' ? 'selected' : ''}>plan_name</option>
                                            <option value="plan_description" ${m.canonicalField == 'plan_description' ? 'selected' : ''}>plan_description</option>
                                            <option value="effective_date" ${m.canonicalField == 'effective_date' ? 'selected' : ''}>effective_date</option>
                                            <option value="termination_date" ${m.canonicalField == 'termination_date' ? 'selected' : ''}>termination_date</option>
                                            <option value="status" ${m.canonicalField == 'status' ? 'selected' : ''}>status</option>
                                        </c:if>
                                    </select>
                                </td>

                                <%-- Required checkbox --%>
                                <td class="text-center">
                                    <input type="checkbox" class="form-check-input req-check"
                                           data-row="${idx.index}" ${m.required ? 'checked' : ''}
                                           onchange="document.getElementById('reqFlag_${idx.index}').value = this.checked ? 'true' : 'false'">
                                    <input type="hidden" name="isRequiredFlag" id="reqFlag_${idx.index}" value="${m.required ? 'true' : 'false'}">
                                </td>

                                <%-- Transform rule (collapsible) --%>
                                <td class="transform-row">
                                    <input type="text" name="transformRule" class="form-control transform-input"
                                           value="${m.transformRule}" placeholder="e.g., UPPERCASE, DATE:M/d/yyyy">
                                </td>
                            </tr>
                        </c:forEach>
                    </tbody>
                </table>
            </div>

            <%-- Validation Messages --%>
            <div id="validationMessages" class="mt-3">
                <%-- Populated by JavaScript --%>
            </div>

            <%-- Save Button --%>
            <div class="mt-3 d-flex gap-2 align-items-center">
                <button type="submit" class="ssa-action save" onclick="return prepareBulkSave();">
                    <i class="bi bi-check-circle me-1"></i>Save All Mappings
                </button>
                <a href="ProviderSetup?action=files&id=${provider.id}" class="ssa-action cancel">
                    <i class="bi bi-x-circle me-1"></i>Cancel
                </a>
            </div>
        </form>
    </c:if>

    <%-- Empty State --%>
    <c:if test="${empty mappings}">
        <div class="text-center text-muted py-5 mb-4">
            <i class="bi bi-file-earmark-arrow-up" style="font-size: 2rem; color: var(--ssa);"></i>
            <p class="mt-2 mb-0">No column mappings defined yet.</p>
            <p style="font-size: 0.85rem;">Upload a sample file above to auto-detect column mappings.</p>
        </div>
    </c:if>

</div>

<script>
    /* ========== Transform Rules Toggle ========== */
    let transformsExpanded = false;
    function toggleTransforms() {
        const section = document.getElementById('transformSection');
        const chevron = document.getElementById('transformChevron');
        transformsExpanded = !transformsExpanded;
        if (transformsExpanded) {
            section.classList.add('expanded');
            chevron.className = 'bi bi-chevron-down me-1';
        } else {
            section.classList.remove('expanded');
            chevron.className = 'bi bi-chevron-right me-1';
        }
    }

    /* ========== FK Checkbox Toggle ========== */
    function toggleFkType(checkbox, rowIdx) {
        const hidden = document.getElementById('isFk_' + rowIdx);
        const select = document.getElementById('fkType_' + rowIdx);
        if (checkbox.checked) {
            hidden.value = 'true';
            select.style.display = 'inline-block';
        } else {
            hidden.value = 'false';
            select.style.display = 'none';
            select.value = '';
        }
        updateValidation();
    }

    /* ========== Validation Preview ========== */
    function updateValidation() {
        const msgs = [];
        const targetEntity = '${fileType.targetEntity}';
        const updateMode = '${fileType.updateMode}';

        // Check PK
        const pkRadios = document.querySelectorAll('input[name="pkColumnIndex"]');
        let hasPk = false;
        pkRadios.forEach(r => { if (r.checked) hasPk = true; });
        if (!hasPk) {
            msgs.push('<i class="bi bi-exclamation-triangle text-warning me-1"></i>A PK (primary key) column must be selected.');
        }

        // Check FK requirements — not required for UPDATE_ONLY mode (supplemental data refresh)
        if (updateMode !== 'UPDATE_ONLY' && (targetEntity === 'BENEFIT' || targetEntity === 'EMPLOYEE')) {
            let hasEmployerFk = false;
            let hasPlanTypeFk = false;
            document.querySelectorAll('.fk-check').forEach(function(cb) {
                if (cb.checked) {
                    const rowIdx = cb.getAttribute('data-row');
                    const fkType = document.getElementById('fkType_' + rowIdx).value;
                    if (fkType === 'EMPLOYER') hasEmployerFk = true;
                    if (fkType === 'PLAN_TYPE') hasPlanTypeFk = true;
                }
            });
            if (!hasEmployerFk) {
                msgs.push('<i class="bi bi-exclamation-triangle text-warning me-1"></i>An Employer FK column must be flagged for ' + targetEntity + ' imports.');
            }
            if (targetEntity === 'BENEFIT' && !hasPlanTypeFk) {
                msgs.push('<i class="bi bi-exclamation-triangle text-warning me-1"></i>A Plan Type FK column must be flagged for BENEFIT imports.');
            }
        }

        const container = document.getElementById('validationMessages');
        if (msgs.length > 0) {
            container.innerHTML = '<div class="alert alert-warning py-2 mb-0">' +
                msgs.map(m => '<div class="validation-msg">' + m + '</div>').join('') +
                '</div>';
        } else {
            container.innerHTML = '<div class="alert alert-success py-2 mb-0"><i class="bi bi-check-circle me-1"></i>All validation criteria met — status will be <strong>READY</strong> after save.</div>';
        }
    }

    /* ========== Prepare Bulk Save ========== */
    function prepareBulkSave() {
        // Sync all FK hidden fields for non-FK target entities (already set as hidden inputs)
        // For FK targets, the hidden isFkFlag values are synced via toggleFkType
        return true;
    }

    /* ========== Init ========== */
    document.addEventListener('DOMContentLoaded', function() {
        // Wire PK radio change to update validation
        document.querySelectorAll('input[name="pkColumnIndex"]').forEach(r => {
            r.addEventListener('change', updateValidation);
        });
        // Wire FK selects to update validation
        document.querySelectorAll('.fk-type-select').forEach(s => {
            s.addEventListener('change', updateValidation);
        });
        // Run initial validation
        updateValidation();
    });
</script>
</body>
</html>
