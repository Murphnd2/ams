<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"/>
    <title>Plan Type Mappings — ${provider.providerName}</title>
</head>
<body>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>
<div class="container-fluid px-4 py-3" style="max-width: 1000px;">
    <h4 style="color: var(--ssa);"><i class="bi bi-diagram-2"></i> Plan Type Mappings — ${provider.providerName}</h4>
    <p class="text-muted mb-3">
        <a href="ProviderSetup" style="color: var(--ssa); text-decoration: none;"><i class="bi bi-arrow-left me-1"></i>Back to Providers</a>
    </p>
    <p class="text-muted mb-3" style="font-size: 0.85rem;">
        Map this provider's plan type codes to AMS plan types. Unmapped codes will create new plan types automatically during import.
    </p>
    <hr>

    <%-- System Default Mappings --%>
    <c:if test="${not empty systemDefaults}">
        <h6 class="text-muted mb-2"><i class="bi bi-shield-check me-1"></i>Universal Defaults</h6>
        <p class="text-muted mb-2" style="font-size: 0.85rem;">
            Pre-configured mappings that apply to all providers. Provider-specific mappings below override these.
        </p>
        <table class="table table-sm table-hover align-middle mb-4" style="font-size: 0.9rem;">
            <thead>
                <tr style="font-size: 0.8rem;">
                    <th>Source Plan Code</th>
                    <th>Source Plan Name</th>
                    <th>→ AMS Plan Type</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="sd" items="${systemDefaults}">
                    <tr class="table-light">
                        <td><code>${sd.sourcePlanCode}</code></td>
                        <td>${not empty sd.sourcePlanName ? sd.sourcePlanName : '—'}</td>
                        <td>
                            <c:choose>
                                <c:when test="${sd.targetPlanType != null}">${sd.targetPlanType.planTypeName}</c:when>
                                <c:otherwise><span class="text-muted fst-italic">Auto-create</span></c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:if>

    <%-- Provider-Specific Mappings --%>
    <h6 class="text-muted mb-2"><i class="bi bi-plug me-1"></i>Provider-Specific Mappings</h6>

    <c:if test="${not empty ptMappings}">
        <table class="table table-hover align-middle mb-4">
            <thead>
                <tr style="font-size: 0.85rem;">
                    <th>Source Plan Code</th>
                    <th>Source Plan Name</th>
                    <th>→ AMS Plan Type</th>
                    <th class="text-end">Actions</th>
                </tr>
            </thead>
            <tbody>
                <c:forEach var="ptm" items="${ptMappings}">
                    <tr>
                        <td><code>${ptm.sourcePlanCode}</code></td>
                        <td>${not empty ptm.sourcePlanName ? ptm.sourcePlanName : '—'}</td>
                        <td>
                            <c:choose>
                                <c:when test="${ptm.targetPlanType != null}">
                                    <strong>${ptm.targetPlanType.planTypeName}</strong>
                                    <small class="text-muted ms-1">(ID: ${ptm.targetPlanType.planTypeId})</small>
                                </c:when>
                                <c:otherwise><span class="text-muted fst-italic">Auto-create on import</span></c:otherwise>
                            </c:choose>
                        </td>
                        <td class="text-end">
                            <button class="ssa-action primary" style="font-size:0.85rem;"
                                    onclick="editPtm('${ptm.id}','${ptm.sourcePlanCode}','${ptm.sourcePlanName}','${ptm.targetPlanType != null ? ptm.targetPlanType.planTypeId : ""}')">
                                Edit
                            </button>
                            <form method="POST" action="ProviderSetup" class="d-inline"
                                  onsubmit="return confirm('Delete this plan type mapping?');">
                                <input type="hidden" name="action" value="deletePlanTypeMapping">
                                <input type="hidden" name="ptmId" value="${ptm.id}">
                                <input type="hidden" name="providerId" value="${provider.id}">
                                <button type="submit" class="ssa-action cancel" style="font-size:0.85rem;">Delete</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
            </tbody>
        </table>
    </c:if>

    <c:if test="${empty ptMappings}">
        <div class="text-center text-muted py-4 mb-4">
            <i class="bi bi-diagram-2" style="font-size: 1.5rem;"></i>
            <p class="mt-2">No provider-specific plan type mappings yet. Universal defaults will be used during import.</p>
        </div>
    </c:if>

    <%-- Add/Edit Plan Type Mapping Form --%>
    <div class="card">
        <div class="card-header" style="background: var(--ssa); color: white; font-size: 0.9rem;">
            <i class="bi bi-plus-circle me-1"></i><span id="ptmFormTitle">Add Plan Type Mapping</span>
        </div>
        <div class="card-body">
            <form id="ptmForm" method="POST" action="ProviderSetup">
                <input type="hidden" name="action" value="savePlanTypeMapping">
                <input type="hidden" name="providerId" value="${provider.id}">
                <input type="hidden" id="ptmId" name="ptmId" value="">

                <div class="row g-3">
                    <div class="col-md-4">
                        <label class="form-label fw-semibold">Source Plan Code <span class="text-danger">*</span></label>
                        <input type="text" id="sourcePlanCode" name="sourcePlanCode" class="form-control" required
                               placeholder="e.g., FSA, HRA, DENTAL" style="text-transform: uppercase;">
                        <div class="form-text">The plan type code as it appears in the provider's data.</div>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label fw-semibold">Source Plan Name</label>
                        <input type="text" id="sourcePlanName" name="sourcePlanName" class="form-control"
                               placeholder="e.g., Flexible Spending Account">
                        <div class="form-text">Display name from the provider (optional).</div>
                    </div>
                    <div class="col-md-4">
                        <label class="form-label fw-semibold">AMS Plan Type</label>
                        <select id="targetPlanTypeId" name="targetPlanTypeId" class="form-select">
                            <option value="">— Auto-create on import —</option>
                            <c:forEach var="pt" items="${allPlanTypes}">
                                <option value="${pt.planTypeId}">${pt.planTypeName} (${pt.code})</option>
                            </c:forEach>
                        </select>
                        <div class="form-text">Leave blank to auto-create a new plan type during import.</div>
                    </div>
                </div>

                <div class="mt-3 d-flex gap-2">
                    <button type="submit" class="ssa-action save" id="ptmSaveBtn">Add Mapping</button>
                    <button type="button" class="ssa-action cancel" id="ptmCancelBtn" style="display:none;" onclick="resetPtmForm()">Cancel Edit</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script>
    function editPtm(id, code, name, targetPtId) {
        document.getElementById('ptmId').value = id;
        document.getElementById('sourcePlanCode').value = code;
        document.getElementById('sourcePlanName').value = (name === 'null' ? '' : name);
        document.getElementById('targetPlanTypeId').value = targetPtId;
        document.getElementById('ptmFormTitle').textContent = 'Edit Plan Type Mapping';
        document.getElementById('ptmSaveBtn').textContent = 'Update Mapping';
        document.getElementById('ptmCancelBtn').style.display = '';
        document.getElementById('sourcePlanCode').focus();
    }

    function resetPtmForm() {
        document.getElementById('ptmId').value = '';
        document.getElementById('sourcePlanCode').value = '';
        document.getElementById('sourcePlanName').value = '';
        document.getElementById('targetPlanTypeId').value = '';
        document.getElementById('ptmFormTitle').textContent = 'Add Plan Type Mapping';
        document.getElementById('ptmSaveBtn').textContent = 'Add Mapping';
        document.getElementById('ptmCancelBtn').style.display = 'none';
    }
</script>
</body>
</html>
