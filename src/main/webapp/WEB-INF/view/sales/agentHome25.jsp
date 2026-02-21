<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<html>
<head>
    <c:import url="/WEB-INF/view/css-js.jsp"></c:import>
    <title>Agent Home</title>
    <style>
        .pipeline-stage { margin-bottom: 0.25rem; }
        .pipeline-header {
            cursor: pointer;
            padding: 0.4rem 0.6rem;
            border-radius: 6px;
            font-size: 0.8rem;
            font-weight: 600;
            display: flex;
            justify-content: space-between;
            align-items: center;
            user-select: none;
        }
        .pipeline-header:hover { filter: brightness(0.95); }
        .pipeline-header .badge { font-size: 0.7rem; }
        .opp-item {
            padding: 0.35rem 0.6rem 0.35rem 1.2rem;
            font-size: 0.78rem;
            cursor: pointer;
            border-left: 3px solid transparent;
            transition: background-color 0.15s;
        }
        .opp-item:hover { background-color: #f0f0f0; }
        .opp-item.active { background-color: #e8f0fe; border-left-color: #4a7dbd; font-weight: 600; }
        .opp-item .opp-agent { font-size: 0.65rem; color: #888; }
        .stage-NEW .pipeline-header { background-color: #e3f2fd; color: #1565c0; }
        .stage-CONTACTED .pipeline-header { background-color: #e8f5e9; color: #2e7d32; }
        .stage-QUALIFIED .pipeline-header { background-color: #fff3e0; color: #e65100; }
        .stage-PROPOSAL_SENT .pipeline-header { background-color: #f3e5f5; color: #6a1b9a; }
        .stage-NEGOTIATION .pipeline-header { background-color: #fce4ec; color: #b71c1c; }
        .stage-ON_HOLD .pipeline-header { background-color: #f5f5f5; color: #616161; }
        .stage-WON .pipeline-header { background-color: #e8f5e9; color: #1b5e20; }
        .stage-LOST .pipeline-header { background-color: #fbe9e7; color: #bf360c; }
        .stat-card { text-align: center; padding: 0.5rem; }
        .stat-card .stat-value { font-size: 1.3rem; font-weight: 700; }
        .stat-card .stat-label { font-size: 0.7rem; color: #888; text-transform: uppercase; }
        .detail-placeholder {
            display: flex; flex-direction: column; align-items: center; justify-content: center;
            height: 300px; color: #aaa;
        }
        .detail-placeholder i { font-size: 3rem; margin-bottom: 0.5rem; }
        .stage-select { font-size: 0.85rem; padding: 0.2rem 0.4rem; border-radius: 4px; }
    </style>
</head>
<body>
<div class="container-fluid">
    <%-- Navbar --%>
    <nav class="navbar navbar-light bg-white border-bottom mb-3 px-2">
        <div class="d-flex align-items-center">
            <img src="${pageContext.request.contextPath}/images/logo1.png" alt="Logo" style="height: 32px;" class="me-3">
            <c:if test="${not empty agency}">
                <span class="fw-bold">${fn:escapeXml(agency.getName())}</span>
            </c:if>
        </div>
        <div class="d-flex align-items-center gap-2">
            <span class="text-muted" style="font-size: 0.85rem;">
                <i class="bi bi-person-circle me-1"></i>${sessionScope.local.getCurrentPerson().getFirstName()} ${sessionScope.local.getCurrentPerson().getLastName()}
            </span>
            <c:if test="${sessionScope.isAgencyAdmin}">
                <a href="PspAgencyHome?agencyId=${agency.getId()}" class="btn btn-outline-secondary btn-sm" title="Agency Manager View">
                    <i class="bi bi-gear"></i>
                </a>
            </c:if>
            <a href="LogOut" class="btn btn-outline-danger btn-sm"><i class="bi bi-box-arrow-right"></i></a>
        </div>
    </nav>

    <c:choose>
        <c:when test="${empty agency}">
            <div class="alert alert-warning mt-4">
                <i class="bi bi-exclamation-triangle me-2"></i>Your account is not associated with an agency. Please contact your administrator.
            </div>
        </c:when>
        <c:otherwise>
            <div class="row">
                <%-- ===================== LEFT COLUMN: Pipeline ===================== --%>
                <div class="col-lg-3">
                    <%-- New Opportunity Button --%>
                    <button class="btn btn-primary btn-sm w-100 mb-2" data-bs-toggle="modal" data-bs-target="#newOppModal">
                        <i class="bi bi-plus-circle me-1"></i>New Opportunity
                    </button>

                    <%-- Pipeline Stages --%>
                    <div id="pipelineList">
                        <c:forEach var="stage" items="${stageOrder}">
                            <c:set var="stageOpps" value="${pipelineMap[stage]}" />
                            <c:set var="stageCount" value="${fn:length(stageOpps)}" />
                            <div class="pipeline-stage stage-${stage}">
                                <div class="pipeline-header" onclick="toggleStage('${stage}')">
                                    <span><i class="bi bi-chevron-down me-1 stage-chevron" id="chev-${stage}"></i>${fn:replace(stage, '_', ' ')}</span>
                                    <span class="badge bg-dark bg-opacity-25">${stageCount}</span>
                                </div>
                                <div class="stage-items" id="items-${stage}" style="${stageCount > 0 ? '' : 'display:none;'}">
                                    <c:forEach var="opp" items="${stageOpps}">
                                        <div class="opp-item" data-opp-id="${opp.getId()}" onclick="selectOpp(this, ${opp.getId()})">
                                            <div class="text-truncate">${fn:escapeXml(opp.getFullName())}</div>
                                            <c:if test="${sessionScope.isAgencyAdmin && opp.getAssignedTo() != null}">
                                                <div class="opp-agent">${opp.getAssignedTo().getFirstName()} ${opp.getAssignedTo().getLastName()}</div>
                                            </c:if>
                                        </div>
                                    </c:forEach>
                                    <c:if test="${stageCount == 0}">
                                        <div class="text-muted text-center py-1" style="font-size: 0.7rem;">—</div>
                                    </c:if>
                                </div>
                            </div>
                        </c:forEach>
                    </div>

                    <%-- Quick Stats --%>
                    <hr class="my-2">
                    <div class="row g-1 mb-2">
                        <div class="col-4">
                            <div class="stat-card">
                                <div class="stat-value text-primary">${activeCount}</div>
                                <div class="stat-label">Active</div>
                            </div>
                        </div>
                        <div class="col-4">
                            <div class="stat-card">
                                <div class="stat-value text-success">${wonCount}</div>
                                <div class="stat-label">Won</div>
                            </div>
                        </div>
                        <div class="col-4">
                            <div class="stat-card">
                                <div class="stat-value text-danger">${lostCount}</div>
                                <div class="stat-label">Lost</div>
                            </div>
                        </div>
                    </div>
                    <div class="text-center mb-3" style="font-size: 0.8rem;">
                        <span class="text-muted">Pipeline Value:</span>
                        <strong class="text-primary">$<fmt:formatNumber value="${pipelineValue}" pattern="#,##0" /></strong>
                    </div>
                </div>

                <%-- ===================== RIGHT COLUMN: Detail ===================== --%>
                <div class="col-lg-9">
                    <%-- Placeholder (shown when no opportunity selected) --%>
                    <div id="detailPlaceholder" class="detail-placeholder">
                        <i class="bi bi-kanban"></i>
                        <div>Select an opportunity from the pipeline</div>
                        <div style="font-size: 0.8rem;" class="text-muted mt-1">or create a new one to get started</div>
                    </div>

                    <%-- Detail Panel (hidden until selection) --%>
                    <div id="detailPanel" style="display: none;">
                        <div class="card">
                            <div class="card-header bg-white d-flex justify-content-between align-items-center py-2">
                                <div>
                                    <h5 class="mb-0 fw-bold" id="detailName"></h5>
                                    <small class="text-muted" id="detailAgent"></small>
                                </div>
                                <div class="d-flex align-items-center gap-2">
                                    <select class="form-select form-select-sm stage-select" id="detailStage" onchange="updateStage()">
                                        <c:forEach var="stage" items="${stageOrder}">
                                            <option value="${stage}">${fn:replace(stage, '_', ' ')}</option>
                                        </c:forEach>
                                    </select>
                                    <a id="btnViewActivity" class="btn btn-outline-primary btn-sm" href="#" title="Open full activity view">
                                        <i class="bi bi-box-arrow-up-right"></i>
                                    </a>
                                </div>
                            </div>
                            <div class="card-body">
                                <%-- Detail Info --%>
                                <div class="row mb-3">
                                    <div class="col-md-6">
                                        <table class="table table-sm table-borderless mb-0" style="font-size: 0.82rem;">
                                            <tr><td class="text-muted" style="width:120px;">Contact</td><td id="detailContact">—</td></tr>
                                            <tr><td class="text-muted">Email</td><td id="detailEmail">—</td></tr>
                                            <tr><td class="text-muted">Due Date</td><td id="detailDueDate">—</td></tr>
                                        </table>
                                    </div>
                                    <div class="col-md-6">
                                        <table class="table table-sm table-borderless mb-0" style="font-size: 0.82rem;">
                                            <tr><td class="text-muted" style="width:120px;">Est. Employees</td><td id="detailEEs">—</td></tr>
                                            <tr><td class="text-muted">Est. Value</td><td id="detailValue">—</td></tr>
                                            <tr><td class="text-muted">Close Date</td><td id="detailCloseDate">—</td></tr>
                                        </table>
                                    </div>
                                </div>

                                <%-- Action Buttons --%>
                                <div class="d-flex gap-2 mb-3">
                                    <a id="btnCreateProposal" class="btn btn-sm btn-outline-primary" href="#">
                                        <i class="bi bi-file-earmark-plus me-1"></i>Create Proposal
                                    </a>
                                </div>

                                <%-- Proposals Table --%>
                                <div id="proposalsSection" style="display:none;">
                                    <h6 class="fw-bold" style="font-size:0.85rem;"><i class="bi bi-file-earmark-text me-1"></i>Proposals</h6>
                                    <div id="proposalsTableContainer"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%-- ===================== NEW OPPORTUNITY MODAL ===================== --%>
<div class="modal fade" id="newOppModal" tabindex="-1">
    <div class="modal-dialog">
        <div class="modal-content">
            <form method="post" action="CreateOpportunity" id="newOppForm">
                <input type="hidden" name="agencyId" value="${agency != null ? agency.getId() : ''}" />
                <input type="hidden" name="prospectMode" id="prospectMode" value="new" />
                <div class="modal-header">
                    <h5 class="modal-title"><i class="bi bi-plus-circle me-2"></i>New Opportunity</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <%-- Prospect Mode Toggle --%>
                    <div class="btn-group w-100 mb-3" role="group">
                        <input type="radio" class="btn-check" name="prospectToggle" id="togNew" autocomplete="off" checked onclick="showProspectMode('new')">
                        <label class="btn btn-outline-primary" for="togNew"><i class="bi bi-plus-circle me-1"></i>New Prospect</label>
                        <input type="radio" class="btn-check" name="prospectToggle" id="togExisting" autocomplete="off" onclick="showProspectMode('existing')">
                        <label class="btn btn-outline-primary" for="togExisting"><i class="bi bi-search me-1"></i>Existing Prospect</label>
                    </div>

                    <%-- NEW PROSPECT FIELDS --%>
                    <div id="newProspectFields">
                        <div class="mb-2">
                            <label class="form-label fw-semibold mb-1">Company Name <span class="text-danger">*</span></label>
                            <input type="text" name="companyName" class="form-control form-control-sm" id="companyName" placeholder="e.g. Acme Corp">
                        </div>
                        <div class="row mb-2">
                            <div class="col-6">
                                <label class="form-label fw-semibold mb-1">Contact First Name</label>
                                <input type="text" name="contactFirst" class="form-control form-control-sm" placeholder="First">
                            </div>
                            <div class="col-6">
                                <label class="form-label fw-semibold mb-1">Contact Last Name</label>
                                <input type="text" name="contactLast" class="form-control form-control-sm" placeholder="Last">
                            </div>
                        </div>
                        <div class="mb-2">
                            <label class="form-label fw-semibold mb-1">Contact Email</label>
                            <input type="email" name="contactEmail" class="form-control form-control-sm" placeholder="email@example.com">
                        </div>
                    </div>

                    <%-- EXISTING PROSPECT DROPDOWN --%>
                    <div id="existingProspectFields" style="display: none;">
                        <div class="mb-2">
                            <label class="form-label fw-semibold mb-1">Select Prospect</label>
                            <select name="prospectId" class="form-select form-select-sm" id="prospectSelect">
                                <option value="">-- Select Prospect --</option>
                                <c:forEach var="p" items="${prospects}">
                                    <option value="${p.getId()}">${fn:escapeXml(p.getName())}</option>
                                </c:forEach>
                            </select>
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary"><i class="bi bi-plus-circle me-1"></i>Create</button>
                </div>
            </form>
        </div>
    </div>
</div>

<%-- ===================== OPPORTUNITY DATA (JSON for JS) ===================== --%>
<script>
    const opportunities = {
        <c:forEach var="opp" items="${opportunities}" varStatus="s">
        ${opp.getId()}: {
            id: ${opp.getId()},
            name: "${fn:escapeXml(opp.getFullName())}",
            stage: "${opp.getStage()}",
            agent: "${opp.getAssignedTo() != null ? fn:escapeXml(opp.getAssignedTo().getFirstName()) : ''}${opp.getAssignedTo() != null ? ' ' : ''}${opp.getAssignedTo() != null ? fn:escapeXml(opp.getAssignedTo().getLastName()) : ''}",
            contact: "${opp.getPrimaryContact() != null ? fn:escapeXml(opp.getPrimaryContact().getFirstName()) : ''}${opp.getPrimaryContact() != null ? ' ' : ''}${opp.getPrimaryContact() != null ? fn:escapeXml(opp.getPrimaryContact().getLastName()) : '—'}",
            email: "${opp.getPrimaryContact() != null && opp.getPrimaryContact().getEmail() != null ? opp.getPrimaryContact().getEmail() : '—'}",
            dueDate: "${opp.getDueDate() != null ? opp.getDueDate() : '—'}",
            estimatedEEs: "${opp.getEstimatedEmployees() != null ? opp.getEstimatedEmployees() : '—'}",
            estimatedValue: "${opp.getEstimatedValue() != null ? opp.getEstimatedValue() : ''}",
            closeDate: "${opp.getExpectedCloseDate() != null ? opp.getExpectedCloseDate() : '—'}",
            prospectId: "${opp.getProspect() != null ? opp.getProspect().getId() : ''}"
        }${!s.last ? ',' : ''}
        </c:forEach>
    };

    let selectedOppId = null;

    function toggleStage(stage) {
        const items = document.getElementById('items-' + stage);
        const chev = document.getElementById('chev-' + stage);
        if (items.style.display === 'none') {
            items.style.display = '';
            chev.className = 'bi bi-chevron-down me-1 stage-chevron';
        } else {
            items.style.display = 'none';
            chev.className = 'bi bi-chevron-right me-1 stage-chevron';
        }
    }

    function selectOpp(el, oppId) {
        document.querySelectorAll('.opp-item.active').forEach(e => e.classList.remove('active'));
        el.classList.add('active');
        selectedOppId = oppId;

        const opp = opportunities[oppId];
        if (!opp) return;

        document.getElementById('detailPlaceholder').style.display = 'none';
        document.getElementById('detailPanel').style.display = '';

        document.getElementById('detailName').textContent = opp.name;
        document.getElementById('detailAgent').textContent = opp.agent;
        document.getElementById('detailStage').value = opp.stage;
        document.getElementById('detailContact').textContent = opp.contact;
        document.getElementById('detailEmail').textContent = opp.email;
        document.getElementById('detailDueDate').textContent = opp.dueDate;
        document.getElementById('detailEEs').textContent = opp.estimatedEEs;
        document.getElementById('detailValue').textContent = opp.estimatedValue ? '$' + Number(opp.estimatedValue).toLocaleString() : '—';
        document.getElementById('detailCloseDate').textContent = opp.closeDate;

        document.getElementById('btnViewActivity').href = 'ViewById?id=' + oppId;

        if (opp.prospectId) {
            document.getElementById('btnCreateProposal').href = 'ProposalBuilder?prospectId=' + opp.prospectId;
        }
    }

    function updateStage() {
        if (!selectedOppId) return;
        const newStage = document.getElementById('detailStage').value;
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = 'UpdateOpportunityStage';
        form.innerHTML = '<input type="hidden" name="oppId" value="' + selectedOppId + '">' +
                          '<input type="hidden" name="stage" value="' + newStage + '">';
        document.body.appendChild(form);
        form.submit();
    }

    // ===== New Opportunity Modal: prospect mode toggle =====
    function showProspectMode(mode) {
        document.getElementById('prospectMode').value = mode;
        if (mode === 'new') {
            document.getElementById('newProspectFields').style.display = '';
            document.getElementById('existingProspectFields').style.display = 'none';
            document.getElementById('companyName').required = true;
            document.getElementById('prospectSelect').required = false;
        } else {
            document.getElementById('newProspectFields').style.display = 'none';
            document.getElementById('existingProspectFields').style.display = '';
            document.getElementById('companyName').required = false;
            document.getElementById('prospectSelect').required = true;
        }
    }
    // Initialize on load
    showProspectMode('new');
</script>
</body>
</html>
