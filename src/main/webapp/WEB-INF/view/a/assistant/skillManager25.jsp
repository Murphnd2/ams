<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Chatbot Skills</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.1/font/bootstrap-icons.css" rel="stylesheet">
    <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,100..1000;1,9..40,100..1000&display=swap" rel="stylesheet">
    <style>
        * { font-family: 'DM Sans', sans-serif; }
        .hdr-bar {
            background: #f8f9fa;
            border-bottom: 1px solid #dee2e6;
            padding: 0.65rem 1rem;
            display: flex;
            align-items: center;
            justify-content: space-between;
        }
        .hdr-bar h5 { margin: 0; font-size: 1rem; font-weight: 600; color: #0d5681; }
        .skill-card {
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 1rem;
            background: #fff;
            transition: box-shadow 0.15s;
        }
        .skill-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
        .skill-card.inactive { opacity: 0.6; }
        .btn-ssa {
            background: #0d5681;
            color: #fff;
            border: none;
            font-size: 0.82rem;
            padding: 0.35rem 0.85rem;
            border-radius: 6px;
        }
        .btn-ssa:hover { background: #094369; color: #fff; }
    </style>
</head>
<body>
<c:set var="pageTitle" value="Chatbot Skills" scope="request"/>
<c:set var="pageIcon" value="bi-robot" scope="request"/>
<c:import url="/WEB-INF/view/a/general/navbar25.jsp"/>

<div class="hdr-bar">
    <h5><i class="bi bi-robot me-2"></i>Chatbot Skills</h5>
    <button class="btn-ssa" onclick="openSkillModal()">
        <i class="bi bi-plus-lg me-1"></i>Add Skill
    </button>
</div>

<div class="container-fluid p-3">
    <c:if test="${empty skills}">
        <div class="text-center text-muted py-5">
            <i class="bi bi-robot" style="font-size:3rem; opacity:0.3;"></i>
            <p class="mt-2">No chatbot skills configured yet. Click <strong>Add Skill</strong> to create one.</p>
        </div>
    </c:if>

    <div class="row g-3">
        <c:forEach var="skill" items="${skills}">
            <div class="col-lg-6 col-xl-4">
                <div class="skill-card ${!skill.active ? 'inactive' : ''}">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <div>
                            <h6 class="mb-0 fw-bold">${skill.skillName}</h6>
                            <small class="text-muted">${skill.description}</small>
                        </div>
                        <span class="badge ${skill.active ? 'bg-success' : 'bg-secondary'}">${skill.active ? 'Active' : 'Inactive'}</span>
                    </div>
                    <div class="mb-2" style="font-size:0.78rem;">
                        <c:if test="${not empty skill.triggerKeywords}">
                            <div class="mb-1"><i class="bi bi-tag me-1 text-muted"></i><span class="text-muted">Keywords:</span> ${skill.triggerKeywords}</div>
                        </c:if>
                        <c:if test="${skill.acceptsFileUpload}">
                            <div class="mb-1"><i class="bi bi-paperclip me-1 text-muted"></i><span class="text-muted">Files:</span> ${not empty skill.acceptedMimeTypes ? skill.acceptedMimeTypes : 'any'}</div>
                        </c:if>
                        <div class="mb-1"><i class="bi bi-cpu me-1 text-muted"></i><span class="text-muted">Model:</span> ${skill.model} <span class="text-muted ms-2">Tokens:</span> ${skill.maxTokens}</div>
                        <c:if test="${skill.adminOnly}">
                            <div><i class="bi bi-shield-lock me-1 text-warning"></i><span class="text-muted">Admin only</span></div>
                        </c:if>
                    </div>
                    <div class="d-flex gap-1 mt-2">
                        <form method="post" action="SkillManager" class="d-inline">
                            <input type="hidden" name="action" value="toggle">
                            <input type="hidden" name="skillId" value="${skill.id}">
                            <button type="submit" class="btn btn-sm btn-outline-secondary" title="${skill.active ? 'Deactivate' : 'Activate'}">
                                <i class="bi ${skill.active ? 'bi-pause-circle' : 'bi-play-circle'}"></i>
                            </button>
                        </form>
                        <button class="btn btn-sm btn-outline-primary" onclick="editSkill(${skill.id})" title="Edit">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <form method="post" action="SkillManager" class="d-inline" onsubmit="return confirm('Delete this skill?')">
                            <input type="hidden" name="action" value="delete">
                            <input type="hidden" name="skillId" value="${skill.id}">
                            <button type="submit" class="btn btn-sm btn-outline-danger" title="Delete">
                                <i class="bi bi-trash"></i>
                            </button>
                        </form>
                    </div>
                </div>
            </div>

            <%-- Hidden data for edit modal population --%>
            <script>
                if (!window._skillData) window._skillData = {};
                window._skillData[${skill.id}] = {
                    skillName: "${skill.skillName}",
                    description: `<c:out value="${skill.description}" escapeXml="true"/>`,
                    systemPrompt: `<c:out value="${skill.systemPrompt}" escapeXml="true"/>`,
                    triggerKeywords: "${skill.triggerKeywords}",
                    acceptsFileUpload: ${skill.acceptsFileUpload},
                    acceptedMimeTypes: "${skill.acceptedMimeTypes}",
                    model: "${skill.model}",
                    maxTokens: ${skill.maxTokens},
                    adminOnly: ${skill.adminOnly},
                    sortOrder: ${skill.sortOrder}
                };
            </script>
        </c:forEach>
    </div>
</div>

<%-- Create/Edit Modal --%>
<div class="modal fade" id="skillModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <form method="post" action="SkillManager">
                <input type="hidden" name="action" id="smAction" value="create">
                <input type="hidden" name="skillId" id="smSkillId" value="">
                <div class="modal-header" style="background:#0d5681; color:#fff;">
                    <h5 class="modal-title" id="smTitle"><i class="bi bi-robot me-2"></i>Add Skill</h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <div class="row g-3">
                        <div class="col-md-8">
                            <label class="form-label fw-semibold">Skill Name</label>
                            <input type="text" class="form-control form-control-sm" name="skillName" id="smSkillName" required maxlength="100">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label fw-semibold">Sort Order</label>
                            <input type="number" class="form-control form-control-sm" name="sortOrder" id="smSortOrder" value="100">
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold">Description</label>
                            <input type="text" class="form-control form-control-sm" name="description" id="smDescription" maxlength="500">
                        </div>
                        <div class="col-12">
                            <label class="form-label fw-semibold">System Prompt</label>
                            <textarea class="form-control form-control-sm" name="systemPrompt" id="smSystemPrompt" rows="10" required style="font-family:monospace; font-size:0.8rem;"></textarea>
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Trigger Keywords <small class="text-muted">(comma-separated)</small></label>
                            <input type="text" class="form-control form-control-sm" name="triggerKeywords" id="smTriggerKeywords" maxlength="500"
                                   placeholder="ach,noc,wells fargo,routing">
                        </div>
                        <div class="col-md-3">
                            <label class="form-label fw-semibold">Model</label>
                            <%-- T169/S20-J — this list is a curated shortlist, not a validation set. A
                                 skill's stored model may be neither of these two (e.g. a future
                                 release), and the dropdown must still be able to represent it —
                                 setSkillModelValue() below injects a marked, non-error option for
                                 exactly that case rather than rendering blank. --%>
                            <select class="form-select form-select-sm" name="model" id="smModel">
                                <option value="claude-haiku-4-5-20251001">Haiku 4.5</option>
                                <option value="claude-sonnet-5">Sonnet 5</option>
                            </select>
                        </div>
                        <div class="col-md-3">
                            <label class="form-label fw-semibold">Max Tokens</label>
                            <input type="number" class="form-control form-control-sm" name="maxTokens" id="smMaxTokens" value="1024" min="256" max="8192">
                        </div>
                        <div class="col-md-4">
                            <div class="form-check mt-4">
                                <input type="checkbox" class="form-check-input" name="acceptsFileUpload" id="smAcceptsFile" value="true"
                                       onchange="document.getElementById('smMimeGroup').style.display = this.checked ? 'block' : 'none'">
                                <label class="form-check-label fw-semibold" for="smAcceptsFile">Accepts File Upload</label>
                            </div>
                        </div>
                        <div class="col-md-4">
                            <div class="form-check mt-4">
                                <input type="checkbox" class="form-check-input" name="adminOnly" id="smAdminOnly" value="true">
                                <label class="form-check-label fw-semibold" for="smAdminOnly">Admin Only</label>
                            </div>
                        </div>
                        <div class="col-md-8" id="smMimeGroup" style="display:none;">
                            <label class="form-label fw-semibold">Accepted MIME Types <small class="text-muted">(comma-separated)</small></label>
                            <input type="text" class="form-control form-control-sm" name="acceptedMimeTypes" id="smAcceptedMime" maxlength="200"
                                   placeholder="application/pdf">
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-sm btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-sm btn-ssa">Save Skill</button>
                </div>
            </form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/js/bootstrap.bundle.min.js"></script>
<script>
    function openSkillModal() {
        document.getElementById('smAction').value = 'create';
        document.getElementById('smSkillId').value = '';
        document.getElementById('smTitle').innerHTML = '<i class="bi bi-robot me-2"></i>Add Skill';
        document.getElementById('smSkillName').value = '';
        document.getElementById('smDescription').value = '';
        document.getElementById('smSystemPrompt').value = '';
        document.getElementById('smTriggerKeywords').value = '';
        document.getElementById('smAcceptsFile').checked = false;
        document.getElementById('smAcceptedMime').value = '';
        document.getElementById('smMimeGroup').style.display = 'none';
        setSkillModelValue('claude-haiku-4-5-20251001');
        document.getElementById('smMaxTokens').value = '1024';
        document.getElementById('smAdminOnly').checked = false;
        document.getElementById('smSortOrder').value = '100';
        new bootstrap.Modal(document.getElementById('skillModal')).show();
    }

    function editSkill(skillId) {
        const d = window._skillData[skillId];
        if (!d) return;
        document.getElementById('smAction').value = 'update';
        document.getElementById('smSkillId').value = skillId;
        document.getElementById('smTitle').innerHTML = '<i class="bi bi-robot me-2"></i>Edit Skill';
        document.getElementById('smSkillName').value = d.skillName;
        document.getElementById('smDescription').value = d.description;
        document.getElementById('smSystemPrompt').value = d.systemPrompt;
        document.getElementById('smTriggerKeywords').value = d.triggerKeywords;
        document.getElementById('smAcceptsFile').checked = d.acceptsFileUpload;
        document.getElementById('smAcceptedMime').value = d.acceptedMimeTypes;
        document.getElementById('smMimeGroup').style.display = d.acceptsFileUpload ? 'block' : 'none';
        setSkillModelValue(d.model);
        document.getElementById('smMaxTokens').value = d.maxTokens;
        document.getElementById('smAdminOnly').checked = d.adminOnly;
        document.getElementById('smSortOrder').value = d.sortOrder;
        new bootstrap.Modal(document.getElementById('skillModal')).show();
    }

    // T169/S20-J — the Model dropdown must always be able to represent whatever is actually
    // stored, even when it isn't one of the two curated options above. Without this, an
    // unrecognized value (e.g. a model released after this page was last updated) renders
    // blank — which is exactly what led an admin to pick a wrong listed option and silently
    // overwrite a working configuration. If the value has no matching <option>, inject one
    // marked "(current)" rather than styling it as an error: the stored value is very often
    // correct, just newer than this hardcoded list. Any option injected by a prior call is
    // removed first, so switching between skills never leaves a stale custom entry behind.
    function setSkillModelValue(model) {
        const select = document.getElementById('smModel');
        const existingCustom = document.getElementById('smModelCustomOption');
        if (existingCustom) existingCustom.remove();

        const hasOption = Array.from(select.options).some(o => o.value === model);
        if (!hasOption && model) {
            const opt = document.createElement('option');
            opt.id = 'smModelCustomOption';
            opt.value = model;
            opt.textContent = model + ' (current)';
            select.appendChild(opt);
        }
        select.value = model;
    }
</script>
</body>
</html>
